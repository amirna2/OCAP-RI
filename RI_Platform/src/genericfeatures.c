// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END


#include <ri_log.h>
#include <ri_config.h>

#include <glib.h>
#include <libxml/xmlreader.h>

#include "genericfeatures.h"
#include "sectionutils.h"

// Logging category
log4c_category_t* gf_RILogCategory = NULL;

// Use UAL category for logs in this file
#define RILOG_CATEGORY gf_RILogCategory

#define MAX_VALUE_LEN 1024
#define GFDB_PREFIX  "GF:/"
char *GF_MMI_DIALOG[] =
{
    "invalid",
    "app_info_0",
    "mmi",
    "mmi16lines",
    "mmi_ASCII_1",
    "mmi_ASCII_2",
    "mmi_ASCII_3",
    "mmi_ASCII_4",
    "mmi_ASCII_5",
    "mmi_ASCII_6",
    "mmi_ASCII_7",
    "mmi_ASCII_8",
    "mmi_ASCII_9",
};

typedef struct
{
    char* value;
    char* flags;
} ht_value;

struct Gf
{
    char* databaseFile;
    char* backupFile;
    GHashTable* indexNamePairs;
    GHashTable* nameValuePairs;
    GQueue* changedEventedVars;
} gf;

extern ri_bool OpenMmiUrl(uint8_t displayType, char* url);

// Internal function prototypes
static ri_bool isXmlDocValid(const xmlDocPtr doc, const char* schema_file);
static void addDatabaseEntry(xmlTextReaderPtr reader,
        const char* attrValueName, GHashTable* in_table, GHashTable *nv_table);
static ri_bool addEntriesFromSection(xmlDocPtr doc, const char* nodeName,
        const char* attrValueName, GHashTable *in_table, GHashTable *nv_table);
static ri_bool loadDatabase(const char* from_file, const char* schema_file,
        GHashTable* in_table, GHashTable* nv_table);
static ri_bool saveDatabase(GHashTable *in_table, GHashTable* nv_table,
        const char* to_file);
static void destroyHashTableValue(gpointer value);

ri_bool GfInit()
{
    ri_bool retval = FALSE;
    char *path = NULL;

    // Create our logging category
    gf_RILogCategory = log4c_category_get("RI.GF");
    RILOG_DEBUG("%s -- Entry\n", __FUNCTION__);

    memset((void*) &gf, 0, sizeof(gf));

    path = ricfg_getValue("RIPlatform", "RI.Headend.resources.directory");
    if (path == NULL)
    {
        RILOG_ERROR("%s -- could not read resource path from config?!\n",
                __FUNCTION__);
        retval = FALSE;
    }
    else
    {
        char scFile[MAX_VALUE_LEN];
        char dbFile[MAX_VALUE_LEN];
        char bkFile[MAX_VALUE_LEN];

        // index-name HT values are used as keys in the name-value HT
        // destroyValue function is NOT supplied for the index-name HT
        // and destroyKey function IS supplied for the name-value HT
        gf.indexNamePairs = g_hash_table_new_full(g_int_hash, g_int_equal,
                g_free, NULL);
        gf.nameValuePairs = g_hash_table_new_full(g_str_hash, g_str_equal,
                g_free, destroyHashTableValue);
        gf.changedEventedVars = g_queue_new();

        RILOG_DEBUG("%s -- Using resource path %s\n", __FUNCTION__, path);
        snprintf(scFile, MAX_VALUE_LEN, "%s/%s", path, GF_XMLSCHEMA_FILE);
        snprintf(dbFile, MAX_VALUE_LEN, "%s/%s", path, GF_DATABASE_FILE);
        snprintf(bkFile, MAX_VALUE_LEN, "%s.bak", dbFile);
        gf.databaseFile = g_strdup(dbFile);
        gf.backupFile = g_strdup(bkFile);
        RILOG_DEBUG("%s -- Attempting to open GF database file %s\n",
                __FUNCTION__, gf.databaseFile);

        retval = loadDatabase(gf.databaseFile, scFile, gf.indexNamePairs,
                gf.nameValuePairs);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return retval;
}

void GfExit()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (saveDatabase(gf.indexNamePairs, gf.nameValuePairs,
            gf.databaseFile) == FALSE)
    {
        RILOG_WARN("%s -- Unable to save database file %s\n", __FUNCTION__,
                gf.databaseFile);
    }

    g_free(gf.databaseFile);
    gf.databaseFile = NULL;
    g_free(gf.backupFile);
    gf.backupFile = NULL;
    g_hash_table_unref(gf.indexNamePairs);
    gf.indexNamePairs = NULL;
    g_hash_table_unref(gf.nameValuePairs);
    gf.nameValuePairs = NULL;
    while (g_queue_is_empty(gf.changedEventedVars) == FALSE)
    {
        g_free(g_queue_pop_head(gf.changedEventedVars));
    }
    g_queue_free(gf.changedEventedVars);
    gf.changedEventedVars = NULL;

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_bool SetValue(int dialogRequest, const char *inname, const char *invalue)
{
    ri_bool retVal = FALSE;
    const ht_value *value = NULL;
    char *name = NULL;
    RILOG_TRACE("%s -- Entry,(%d, %p, %p)\n", __FUNCTION__,
                dialogRequest, inname, invalue);

    if (NULL == inname || NULL == invalue)
    {
        RILOG_ERROR("%s -- invalid args!\n", __FUNCTION__);
    }
    else if (dialogRequest != 0)
    {
        RILOG_INFO("%s -- \"DialogRequest\" (%d) for MMI...\n",
                __FUNCTION__, dialogRequest);
        retVal = OpenMmiUrl(0, GF_MMI_DIALOG[dialogRequest]);
    }
    else if (strstr(inname, GFDB_PREFIX) != inname)
    {
        RILOG_WARN("%s -- \"GF:/\" prefix is required for variable names\n",
                __FUNCTION__);
    }
    else
    {
        name = (char*) (inname + strlen(GFDB_PREFIX));
        RILOG_INFO("%s: name = %s\n", __FUNCTION__, name);
        value = (const ht_value*) g_hash_table_lookup(
                                                gf.nameValuePairs, name);
        if (value == NULL)
        {
            guint *idx = g_try_malloc(sizeof(guint));

            if (NULL == idx)
            {
                RILOG_FATAL(-1,
                            "line %d of %s, %s memory allocation failure\n",
                            __LINE__, __FILE__, __func__);
            }

            char *key = g_strdup(name);
            ht_value *val = g_try_malloc(sizeof(ht_value));

            if (NULL == val)
            {
                RILOG_FATAL(-1,
                            "line %d of %s, %s memory allocation failure\n",
                            __LINE__, __FILE__, __func__);
            }

            *idx = g_hash_table_size(gf.indexNamePairs);
            val->flags = g_strdup("WGVPR");
            val->value = g_strdup(invalue);

            if (saveDatabase(gf.indexNamePairs,
                    gf.nameValuePairs, gf.backupFile) == FALSE)
            {
                RILOG_WARN("%s -- Problems saving old GF database to %s\n",
                        __FUNCTION__, gf.backupFile);
            }

            g_hash_table_insert(gf.indexNamePairs, idx, key);
            g_hash_table_insert(gf.nameValuePairs, key, val);

            if (saveDatabase(gf.indexNamePairs,
                    gf.nameValuePairs, gf.databaseFile) == FALSE)
            {
                RILOG_WARN("%s -- Problems saving modified GF database to %s\n",
                        __FUNCTION__, gf.databaseFile);
            }

            RILOG_DEBUG("%s -- Successfully created new GF entry \"%s\" "
                        "with value \"%s\" and flags \"%s\"\n",
                        __FUNCTION__, key, val->value, val->flags);

            retVal = TRUE;
        }
        else if (strchr(value->flags, 'W') == NULL)
        {
            RILOG_WARN("%s --  value is read-only!\n", __FUNCTION__);
        }
        else
        {
            char *new_key = g_strdup(name);
            ht_value *new_val = g_try_malloc(sizeof(ht_value));

            if (NULL == new_val)
            {
                RILOG_FATAL(-1,
                            "line %d of %s, %s memory allocation failure\n",
                            __LINE__, __FILE__, __func__);
            }

            new_val->value = g_strdup(invalue);
            new_val->flags = g_strdup(value->flags);

            if (saveDatabase(gf.indexNamePairs,
                    gf.nameValuePairs, gf.backupFile) == FALSE)
            {
                RILOG_WARN("%s -- Problems saving old GF database to %s\n",
                        __FUNCTION__, gf.backupFile);
            }

            // DON'T use g_hash_table_replace as it frees the _OLD_ key.
            // We don't want to free the old key because it is a value in
            // the index-name hash table.
            g_hash_table_insert(gf.nameValuePairs, new_key, new_val);

            if (saveDatabase(gf.indexNamePairs,
                    gf.nameValuePairs, gf.databaseFile) == FALSE)
            {
                RILOG_WARN("%s -- Problems saving modified GF database to %s\n",
                        __FUNCTION__, gf.databaseFile);
            }

            if (strchr(new_val->flags, 'E') != NULL)
            {
                g_queue_push_tail(gf.changedEventedVars, g_strdup(
                        name));
            }

            retVal = TRUE;
        }
    }

    RILOG_DEBUG("%s -- Exit: \"%s\"\n", __FUNCTION__, boolStr(retVal));
    return retVal;
}

char* GetValue(const char *inname)
{
    char *name = NULL;
    char outval[MAX_VALUE_LEN] = {0};

    RILOG_TRACE("%s -- Entry,(%s)\n", __FUNCTION__, inname);

    if (strstr(inname, GFDB_PREFIX) != inname)
    {
        RILOG_WARN("%s -- \"GF:/\" prefix is required for variable names\n",
                __FUNCTION__);
    }
    else
    {
        char *p = NULL;
        name = (char*) (inname + strlen(GFDB_PREFIX));

        if (NULL != (p = strstr(name, ".html")))
        {
            *p = 0;    // NULL terminate the GF tag before the file ext
        }

        if (NULL != (p = strstr(name, "v2")))
        {
            *p = 0;    // NULL terminate the GF tag before the version
        }

        const ht_value* value = (const ht_value*) g_hash_table_lookup(
                gf.nameValuePairs, name);
        if (value == NULL)
        {
            RILOG_WARN("%s -- (%s) not found\n", __FUNCTION__, name);
        }
        else if (strchr(value->flags, 'M') != NULL)
        {
            RILOG_WARN("%s -- (%s) invalid type M\n", __FUNCTION__, name);
        }
        else if (strstr(name, "Resource") != NULL)
        {
            char *path, file[FILENAME_MAX];
            FILE *fp = NULL;
            int bytesRead = 0;

            // load HTML file named in 'value'
            snprintf(outval, MAX_VALUE_LEN, "%s", value->value);

            if (NULL != (path = ricfg_getValue("RIPlatform",
                    "RI.Headend.resources.directory")))
            {
                if (strlen(path) + strlen(value->value) < FILENAME_MAX)
                {
                    sprintf(file, "%s/%s", path, value->value);
                    RILOG_INFO("%s -- opening = %s\n", __FUNCTION__, file);

                   if (NULL != (fp = fopen(file, "r")))
                   {
                        if (0 <= (bytesRead = fread(outval, 1,
                                                    MAX_VALUE_LEN - 1, fp)))
                        {
                            outval[bytesRead] = 0;
                        }
                   }
                }
            }
        }
        else
        {
            snprintf(outval, MAX_VALUE_LEN, "%s", value->value);
        }
    }

    RILOG_DEBUG("%s -- Exit, returning: \"%s\"\n", __FUNCTION__, outval);
    return g_strdup(outval);
}

char* GetValueNameByIndex(uint32_t index)
{
    RILOG_TRACE("%s -- Entry,(%d)\n", __FUNCTION__, index);
    char* name = g_hash_table_lookup(gf.indexNamePairs, &index);

    RILOG_DEBUG("%s -- Exit, returning: \"%s\"\n", __FUNCTION__, name);
    return name;
}

char* GetValueUpdateName(void)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    char* varName = g_queue_pop_head(gf.changedEventedVars);

    RILOG_DEBUG("%s -- Exit, returning: \"%s\"\n", __FUNCTION__, varName);
    return varName;
}

static ri_bool isXmlDocValid(const xmlDocPtr doc, const char* schema_file)
{
    ri_bool result = FALSE;
    xmlSchemaParserCtxtPtr parser = NULL;

    parser = xmlSchemaNewParserCtxt(schema_file);
    if (parser == NULL)
    {
        RILOG_ERROR("%s -- error reading GF schema file %s?!\n",
                __FUNCTION__, schema_file);
        result = FALSE;
    }
    else
    {
        xmlSchemaPtr schema = NULL;
        RILOG_DEBUG("%s -- successfully read GF schema file %s\n",
                __FUNCTION__, schema_file);
        schema = xmlSchemaParse(parser);
        if (schema == NULL)
        {
            RILOG_ERROR("%s -- error parsing GF schema file %s?!\n",
                    __FUNCTION__, schema_file);
            xmlSchemaFreeParserCtxt(parser);
            result = FALSE;
        }
        else
        {
            xmlSchemaValidCtxtPtr validator = NULL;
            RILOG_DEBUG("%s -- successfully parsed GF schema file %s\n",
                    __FUNCTION__, schema_file);
            validator = xmlSchemaNewValidCtxt(schema);
            if (validator == NULL)
            {
                RILOG_ERROR(
                        "%s -- error creating GF schema validation context\n",
                        __FUNCTION__);
                xmlSchemaFree(schema);
                xmlSchemaFreeParserCtxt(parser);
                result = FALSE;
            }
            else
            {
                int valid = 0;
                RILOG_DEBUG(
                        "%s -- successfully created GF schema validation context\n",
                        __FUNCTION__);
                valid = xmlSchemaValidateDoc(validator, doc);
                if (valid < 0)
                {
                    RILOG_ERROR(
                            "%s -- could not validate GF database against schema file %s",
                            __FUNCTION__, schema_file);
                    result = FALSE;
                }
                else if (valid > 0)
                {
                    RILOG_ERROR(
                            "%s -- could not validate GF database because of internal error\n",
                            __FUNCTION__);
                    result = FALSE;
                }
                else // valid == 0
                {
                    RILOG_DEBUG(
                            "%s -- successfully validated GF database against schema %s\n",
                            __FUNCTION__, schema_file);
                    result = TRUE;
                }
                xmlSchemaFreeValidCtxt(validator);
                xmlSchemaFreeParserCtxt(parser);
                xmlSchemaFree(schema);
            }
        }
    }
    return result;
}

static void addDatabaseEntry(xmlTextReaderPtr reader,
        const char* attrValueName, GHashTable* in_table, GHashTable* nv_table)
{
    int parentDepth = xmlTextReaderDepth(reader);
    char* parentNodeName = g_strdup(
            (const char*) xmlTextReaderConstName(reader));
    int readerStatus = xmlTextReaderRead(reader);
    if (readerStatus == 1)
    {
        int entryDepth = xmlTextReaderDepth(reader);
        int expectedDepth = parentDepth + 1;
        if (entryDepth != expectedDepth)
        {
            RILOG_WARN("%s -- Did not find any entries in the %s section.\n",
                    __FUNCTION__, parentNodeName);
        }
        else
        {
            while (entryDepth == expectedDepth && readerStatus == 1)
            {
                const char* nodeName = (const char*) xmlTextReaderConstName(
                        reader);
                if (strcmp(nodeName, "Entry") != 0)
                {
                    // We don't care about any "#text" nodes...
                    if (strcmp(nodeName, "#text") != 0)
                    {
                        RILOG_WARN(
                                "%s -- Unrecognized XML node %s in the %s section - skipping.\n",
                                __FUNCTION__, nodeName, parentNodeName);
                    }
                }
                else
                {
                    xmlChar* name = xmlTextReaderGetAttribute(reader,
                            (const xmlChar*) "name");
                    xmlChar* flags = xmlTextReaderGetAttribute(reader,
                            (const xmlChar*) "flags");
                    xmlChar* value = xmlTextReaderGetAttribute(reader,
                            (const xmlChar*) attrValueName);

                    if (name == NULL || flags == NULL || value == NULL)
                    {
                        RILOG_WARN(
                                "%s -- Invalid entry in the %s section: (name=%s,flags=%s,%s=%s) - ignoring.\n",
                                __FUNCTION__, parentNodeName,
                                ((name == NULL) ? ("") : ((char*) name)),
                                ((flags == NULL) ? ("") : ((char*) flags)),
                                attrValueName, ((value == NULL) ? ("")
                                        : ((char*) value)));
                    }
                    else
                    {
                        guint *idx = g_try_malloc(sizeof(guint));

                        if (NULL == idx)
                        {
                            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                                        __LINE__, __FILE__, __func__);
                        }

                        char *key = g_strdup((char*) name);
                        ht_value *val = g_try_malloc(sizeof(ht_value));

                        if (NULL == val)
                        {
                            RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                                        __LINE__, __FILE__, __func__);
                        }


                        *idx = g_hash_table_size(in_table);
                        val->value = g_strdup((char*) value);
                        val->flags = g_strdup((char*) flags);

                        RILOG_DEBUG(
                                "%s -- Parsed GF entry in the %s section: name \"%s\", flags \"%s\", %s \"%s\"\n",
                                __FUNCTION__, parentNodeName, key, val->flags,
                                attrValueName, val->value);

                        g_hash_table_insert(in_table, idx, key);
                        g_hash_table_insert(nv_table, key, val);
                    }
                    xmlFree(name);
                    xmlFree(flags);
                    xmlFree(value);
                }
                readerStatus = xmlTextReaderRead(reader);
                entryDepth = xmlTextReaderDepth(reader);
            }
        }
    }
    g_free(parentNodeName);
}

static ri_bool addEntriesFromSection(xmlDocPtr doc, const char* nodeName,
        const char* attrValueName, GHashTable* in_table, GHashTable* nv_table)
{
    xmlTextReaderPtr reader = xmlReaderWalker(doc);
    int xmlHasNode = xmlTextReaderRead(reader);
    ri_bool foundNode = FALSE;

    while (xmlHasNode == 1)
    {
        const char* name = (const char*) xmlTextReaderConstName(reader);
        if (strcmp(name, nodeName) == 0)
        {
            if (foundNode == TRUE)
            {
                RILOG_WARN("%s -- Multiple %s sections detected!\n",
                        __FUNCTION__, nodeName);
            }
            else
            {
                foundNode = TRUE;
            }
            addDatabaseEntry(reader, attrValueName, in_table, nv_table);
        }
        xmlHasNode = xmlTextReaderRead(reader);
    }

    if (xmlHasNode == -1)
    {
        RILOG_WARN(
                "%s -- XML reader terminated prematurely because of error!\n",
                __FUNCTION__);
    }

    if (xmlTextReaderClose(reader) != 0)
    {
        RILOG_WARN("%s -- error while closing XML reader!\n", __FUNCTION__);
    }
    else
    {
        xmlFreeTextReader(reader);
    }

    return foundNode;
}

static ri_bool loadDatabase(const char* from_file, const char* schema_file,
        GHashTable* in_table, GHashTable *nv_table)
{
    ri_bool result = FALSE;
    xmlDocPtr doc = NULL;

    doc = xmlParseFile(from_file);
    if (doc == NULL)
    {
        RILOG_ERROR("%s -- error reading GF database from %s\n",
                __FUNCTION__, from_file);
        result = FALSE;
    }
    else
    {
        RILOG_DEBUG("%s -- successfully read GF database from %s\n",
                __FUNCTION__, from_file);
        if (isXmlDocValid(doc, schema_file) == FALSE)
        {
            RILOG_WARN(
                    "%s -- could not validate GF database %s against schema %s\n",
                    __FUNCTION__, from_file, schema_file);
        }
        else
        {
            RILOG_INFO(
                    "%s -- successfully validated GF databsse %s against schema %s\n",
                    __FUNCTION__, from_file, schema_file);
        }

        if (addEntriesFromSection(doc, "Variables", "value", in_table, nv_table)
                == FALSE)
        {
            RILOG_WARN("%s -- section Variables was not found!\n", __FUNCTION__);
        }
        if (addEntriesFromSection(doc, "Methods", "args", in_table, nv_table)
                == FALSE)
        {
            RILOG_WARN("%s -- section Methods was not found!\n", __FUNCTION__);
        }
        if (addEntriesFromSection(doc, "Resources", "state", in_table, nv_table)
                == FALSE)
        {
            RILOG_WARN("%s -- section Resources was not found!\n", __FUNCTION__);
        }
        xmlFreeDoc(doc);
        result = TRUE;
    }
    return result;
}

static ri_bool saveDatabase(GHashTable *in_table, GHashTable *nv_table,
        const char *to_file)
{
    xmlDocPtr doc = NULL;
    xmlNodePtr root = NULL;
    xmlNodePtr variables = NULL;
    xmlNodePtr methods = NULL;
    xmlNodePtr resources = NULL;
    guint index = 0;
    gpointer key = NULL;
    int xmlSaveResult = -1;

    doc = xmlNewDoc(BAD_CAST "1.0");

    root = xmlNewNode(NULL, BAD_CAST "GfDatabase");
    (void)xmlNewProp(root, BAD_CAST "xsi:schemaLocation", BAD_CAST "http://www.cablelabs.com/ri/he4ri-20080701 GfDatabase.xsd");
    (void)xmlNewProp(root, BAD_CAST "xmlns", BAD_CAST "http://www.cablelabs.com/ri/he4ri-20080701");
    (void)xmlNewProp(root, BAD_CAST "xmlns:xsi", BAD_CAST "http://www.w3.org/2001/XMLSchema-instance");
    (void) xmlDocSetRootElement(doc, root);

    variables = xmlNewChild(root, NULL, BAD_CAST "Variables", NULL);
    methods = xmlNewChild(root, NULL, BAD_CAST "Methods", NULL);
    resources = xmlNewChild(root, NULL, BAD_CAST "Resources", NULL);

    while ((key = g_hash_table_lookup(in_table, &index)) != NULL)
    {
        char *name = (char*) key;
        ht_value *value = g_hash_table_lookup(nv_table, name);
        xmlNodePtr node = xmlNewNode(NULL, BAD_CAST "Entry");

        if ((value == NULL) || (value->flags == NULL) || (value->value == NULL))
        {
            RILOG_ERROR("%s -- hash lookup failure?!\n", __FUNCTION__);
            xmlFreeDoc(doc);
            return FALSE;
        }

        (void)xmlNewProp(node, BAD_CAST "name", BAD_CAST name);
        (void)xmlNewProp(node, BAD_CAST "flags", BAD_CAST value->flags);
        if (strchr(value->flags, 'V') != NULL)
        {
            (void)xmlNewProp(node, BAD_CAST "value", BAD_CAST value->value);
            (void) xmlAddChild(variables, node);
        }
        else if (strchr(value->flags, 'M') != NULL)
        {
            (void)xmlNewProp(node, BAD_CAST "args", BAD_CAST value->value);
            (void) xmlAddChild(methods, node);
        }
        else // everything else is a resource
        {
            (void)xmlNewProp(node, BAD_CAST "state", BAD_CAST value->value);
            (void) xmlAddChild(resources, node);
        }
        index++;
    }

    xmlSaveResult = xmlSaveFormatFileEnc(to_file, doc, "UTF-8", 1);
    xmlFreeDoc(doc);
    // There is a bug in libxml2 library occuring on Win32 systems
    // that causes an internal CriticalSection structure to be freed
    // when a call to xmlCleanupParser() is made. The CS structure is
    // contained in the threads.c file, name cleanup_helpers_cs.
    // Any subsequent API call following the xmlCleanupParser() call
    // will crash the library on Windows.
    // To prevent this, we are going to execute the xmlCleanupParser()
    // call on non-Win32 systems. This effectively solves OCORI-1913.
#ifndef WIN32
    xmlCleanupParser();
#endif
    return ((xmlSaveResult > 0) ? (TRUE) : (FALSE));
}

static void destroyHashTableValue(gpointer value)
{
    ht_value* hashtable_value = (ht_value*) value;
    g_free(hashtable_value->value);
    g_free(hashtable_value->flags);
    g_free(hashtable_value);
}

