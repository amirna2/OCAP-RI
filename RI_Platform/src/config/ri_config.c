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

#include "ri_config.h"

#include <glib.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>

#define STRBUFFER_SZ 1024

#define MAX_CONFIG_FILE_LINE 512

#define MAX_CONFIG_SETS 32
#define DEFAULT_MAX_MULTIVALUE 32

// Linked list node that holds configuration values
typedef struct _ConfigNode
{
    char* name;
    char* value;
    struct _ConfigNode* next;

} ConfigNode;

// Represents the configurations values from a particular config file
typedef struct _ConfigSet
{
    char* configID;
    int maxMultiValues;
    ConfigNode* configList;

} ConfigSet;

static int g_configSetsInited = 0;
static ConfigSet g_configSets[MAX_CONFIG_SETS];

static void initConfigSets()
{
    int i;
    for (i = 0; i < MAX_CONFIG_SETS; i++)
    {
        g_configSets[i].configID = NULL;
        g_configSets[i].maxMultiValues = DEFAULT_MAX_MULTIVALUE;
        g_configSets[i].configList = NULL;
    }
    g_configSetsInited = 1;
}

/**
 * Trim all whitespace from the leading and trailing edges of the
 * the given input string
 *
 * @param instr the string to trim
 * @param outstr a pointer to the location when the trimmed string
 *        will be written
 */
static void trim(char *instr, char* outstr)
{
    char *ptr = instr;
    char *endptr = instr + strlen(instr) - 1;
    int length;

    /* Advance pointer to first non-whitespace char */
    while (isspace(*ptr))
        ptr++;

    /* Move end pointer toward the front to first non-whitespace char */
    while (isspace(*endptr))
        endptr--;

    length = endptr + 1 - ptr;
    strncpy(outstr, ptr, length);
    outstr[length] = '\0';
}

/**
 * Substitute all environment variable references in the given string
 * with the value of the environment variable.  Environment variable
 * references are in the format:
 *
 *                   $(VAR_NAME)
 *
 * @param inString the string to parse
 * @return a newly g_try_malloc'd string that has all environment variable
 *         references replaced by the variable's value
 */
static char* expandEnvVars(char* inString)
{
    int stringLen = strlen(inString);
    char* temp = inString;
    char *varBegin, *varEnd;
    const char* envVar;
    const int MAX_VAR_LEN = 1024;
    char newName[MAX_VAR_LEN + 1];
    int newNameLen = 0;
    char c;

    newName[0] = '\0';

    // Parse any environment variables
    while ((varBegin = strchr(temp, '$')) != NULL)
    {
        // Search for opening and closing parens
        if (((varBegin - inString + 1) >= stringLen)
                || (*(varBegin + 1) != '('))
            goto parse_error;

        // Append characters up to this point to the new name
        strncat(newName, temp, varBegin - temp);
        newNameLen += varBegin - temp;
        if (newNameLen > MAX_VAR_LEN)
            goto length_error;

        varBegin += 2; // start of env var name

        if ((varEnd = strchr(varBegin, ')')) == NULL)
            goto parse_error;

        c = *varEnd;
        *varEnd = '\0';
        envVar = getenv(varBegin);
        *varEnd = c;
        if (envVar == NULL)
            goto parse_error;

        // Append env var value to the new name
        strncat(newName, envVar, strlen(envVar));
        newNameLen += strlen(envVar);
        if (newNameLen > MAX_VAR_LEN)
            goto length_error;

        temp = varEnd + 1;
    }

    // Append remaining characters
    strncat(newName, temp, (inString + stringLen) - temp);
    newNameLen += (inString + stringLen) - temp;
    if (newNameLen > MAX_VAR_LEN)
        goto length_error;

    return g_strdup(newName);

    parse_error: printf("parseEnvVars -- Illegal env var name or format! %s\n",
            inString);
    return NULL;

    length_error: printf("parseEnvVars -- Env var value is too long! %s\n",
            inString);
    return NULL;
}

/**
 * Returns the configuration set associated with the given ID.
 * Returns NULL if the ID is not found in our list
 */
static ConfigSet* getConfigSet(const char* config_id)
{
    int i;

    for (i = 0; i < MAX_CONFIG_SETS; i++)
    {
        ConfigSet* configSet = &g_configSets[i];

        if (configSet->configID != NULL && strcmp(configSet->configID,
                config_id) == 0)
            return configSet;
    }

    // Not found
    return NULL;
}

/**
 * Returns a pointer to the first available configuration set
 */
static ConfigSet* getAvailableConfigSet()
{
    int i;

    for (i = 0; i < MAX_CONFIG_SETS; i++)
    {
        if (g_configSets[i].configID == NULL)
            return &g_configSets[i];
    }

    // None available
    return NULL;
}

RIConfigError ricfg_parseConfigFile(const char* config_id, const char* filename)
{
    FILE* fp;
    char lineBuffer[512];
    char* maxMultiValues;
    ConfigSet* configSet;

    // Initialize our configuration sets
    if (!g_configSetsInited)
        initConfigSets();

    // Return an error if this ID is already taken
    if (getConfigSet(config_id) != NULL)
    {
        printf(
                "*** Config File ERROR!  Configuration ID already in use! -- %s ***\n",
                config_id);
        return RICONFIG_ERROR_ID_IN_USE;
    }

    // Return an error if there are no more config sets available
    if ((configSet = getAvailableConfigSet()) == NULL)
    {
        printf("*** Config File ERROR!  No available configuration sets! ***\n");
        return RICONFIG_ERROR_NOT_AVAILABLE;
    }

    // Load and parse our config file
    if ((fp = fopen(filename, "r")) == NULL)
    {
        printf(
                "*** Config File WARNING!  Could not open configuration file! ***\n");
        printf("*** config_id = %s, Configuration file: %s\n", config_id,
                filename);
        return 1;
    }

    configSet->configID = g_strdup(config_id);

    /* Read each line of the file */
    while (fgets(lineBuffer, MAX_CONFIG_FILE_LINE, fp) != NULL)
    {
        char name[MAX_CONFIG_FILE_LINE];
        char value[MAX_CONFIG_FILE_LINE];
        char trimname[MAX_CONFIG_FILE_LINE];
        char trimvalue[MAX_CONFIG_FILE_LINE];
        ConfigNode *node;
        char *equals;
        int length;

        /* Ignore comment lines */
        if (lineBuffer[0] == '#')
            continue;

        /* Ignore lines that do not have an '=' char */
        if ((equals = strchr(lineBuffer, '=')) == NULL)
            continue;

        /* Read the property and store in the cache */
        length = equals - lineBuffer;
        strncpy(name, lineBuffer, length);
        name[length] = '\0'; /* have to null-term */

        length = lineBuffer + strlen(lineBuffer) - equals + 1;
        strncpy(value, equals + 1, length);
        value[length] = '\0';

        /* Trim all whitespace from name and value strings */
        trim(name, trimname);
        trim(value, trimvalue);

        node = (ConfigNode*) g_try_malloc(sizeof(ConfigNode));
        if (node == NULL)
            return RICONFIG_MEMORY_ALLOCATION;
        node->name = g_strdup(trimname);
        if (node->name == NULL)
        {
            g_free(node);
            return RICONFIG_MEMORY_ALLOCATION;
        }

        // Parse env vars from value
        if ((node->value = expandEnvVars(trimvalue)) != NULL)
        {
            /* Insert at the front of the list */
            node->next = configSet->configList;
            configSet->configList = node;
        }
        else
        {
            printf(
                    "*** Config File WARNING!  Could not parse env var from config value! -- %s ***\n",
                    trimvalue);
            g_free(node->name);
            g_free(node);
        }
    }

    // Get the max values for multi-value configurations
    if ((maxMultiValues = ricfg_getValue(config_id, "Config.maxMultiValues"))
            != NULL)
    {
        if ((configSet->maxMultiValues = atoi(maxMultiValues)) == 0)
            configSet->maxMultiValues = DEFAULT_MAX_MULTIVALUE;
    }

    // Close the file
    fclose(fp);

    return 0;
}

char* ricfg_getValue(const char* config_id, const char* configName)
{
    ConfigSet* configSet;
    ConfigNode* walker;

    // Initialize our configuration sets
    if (!g_configSetsInited)
        initConfigSets();

    // Find the desired config set
    if ((configSet = getConfigSet(config_id)) == NULL)
        return NULL;

    walker = configSet->configList;
    while (walker != NULL)
    {
        if (strcmp(walker->name, configName) == 0)
            return walker->value;
        walker = walker->next;
    }

    return NULL;
}

int ricfg_getBoolValue(const char* config_id, const char* configName)
{
    char* cfgVal = ricfg_getValue(config_id, configName);

    return (NULL == cfgVal)? 0 : (0 == strcasecmp(cfgVal, "true"));
}

void ricfg_getMultiValue(const char* config_id, char* configNameRoot,
        char** configValues, int* numValues)
{
    ConfigSet* configSet;
    char configName[STRBUFFER_SZ + 1];
    int i;

    // Initialize our configuration sets
    if (!g_configSetsInited)
        initConfigSets();

    // Find the desired config set
    if ((configSet = getConfigSet(config_id)) == NULL)
    {
        *numValues = 0;
        return;
    }

    for (i = 0; i < *numValues && i < configSet->maxMultiValues; i++)
    {
        (void) snprintf(configName, STRBUFFER_SZ, "%s.%d", configNameRoot, i);
        if ((configValues[i] = ricfg_getValue(config_id, configName)) == NULL)
            break;
    }
    *numValues = i;
}

int ricfg_getMaxMultiValues(const char* config_id)
{
    ConfigSet* configSet;

    // Initialize our configuration sets
    if (!g_configSetsInited)
        initConfigSets();

    // Find the desired config set
    if ((configSet = getConfigSet(config_id)) == NULL)
        return 0;

    return configSet->maxMultiValues;
}

void ricfg_freeConfigList(const char* config_id)
{
    ConfigSet* configSet;
    ConfigNode* walker;

    // Initialize our configuration sets
    if (!g_configSetsInited)
        initConfigSets();

    // Find the desired config set
    if ((configSet = getConfigSet(config_id)) == NULL)
        return;

    walker = configSet->configList;
    while (walker != NULL)
    {
        ConfigNode* nodeToDelete = walker;

        // Free the name/value strings
        g_free(walker->name);
        g_free(walker->value);

        walker = walker->next;

        // Free the node itself
        g_free(nodeToDelete);
    }

    g_free(configSet->configID);
    configSet->configID = NULL;
    configSet->maxMultiValues = DEFAULT_MAX_MULTIVALUE;
}

