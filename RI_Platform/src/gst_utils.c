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

#include <gmodule.h>
#include <gst/gst.h>
#include <stdlib.h>

#include <ri_log.h>
#include <ri_config.h>

#include <platform.h>

// Category for all GStreamer element logging
log4c_category_t* gstCategory = NULL;

// Category for this file
#define RILOG_CATEGORY gstutilsCategory
log4c_category_t* gstutilsCategory = NULL;

void rilog_debug_function(GstDebugCategory* category, GstDebugLevel level,
        const gchar* file, const char* function, gint line, GObject* object,
        GstDebugMessage* message, gpointer data)
{
    int log4c_priority = LOG4C_PRIORITY_UNKNOWN;

    // Don't log if category is not enabled
    if (level > gst_debug_category_get_threshold(category))
    {
        return;
    }

    switch (level)
    {
    case GST_LEVEL_NONE:
        break;
    case GST_LEVEL_ERROR:
        log4c_priority = LOG4C_PRIORITY_ERROR;
        break;
    case GST_LEVEL_WARNING:
        log4c_priority = LOG4C_PRIORITY_WARN;
        break;
    case GST_LEVEL_INFO:
        log4c_priority = LOG4C_PRIORITY_INFO;
        break;
    case GST_LEVEL_DEBUG:
        log4c_priority = LOG4C_PRIORITY_DEBUG;
        break;
    case GST_LEVEL_LOG:
        log4c_priority = LOG4C_PRIORITY_TRACE;
        break;
    default:
        break;
    }

    if (log4c_priority != LOG4C_PRIORITY_UNKNOWN)
    {
        log4c_category_log(gstCategory, log4c_priority, "%s %s:%d %s\n", file,
                function, line, gst_debug_message_get(message));
    }
}

void gst_init_library()
{
    GError* err = NULL;
    GOptionContext* ctx = g_option_context_new("GStreamer Initialization");
    int numArgs = ricfg_getMaxMultiValues("RIPlatform");
    char** args = g_try_malloc(sizeof(char*) * numArgs);
    char *path = NULL;
    int i;

    // Get the RILOG category for GStreamer -- and set its priority to
    // the most verbose setting.  The GStreamer command line args in the platform
    // config file will select the level of logging to output
    gstCategory = log4c_category_get("RI.GStreamer");
    (void) log4c_category_set_priority(gstCategory, LOG4C_PRIORITY_TRACE);

    // Get the RILOG category for this file
    gstutilsCategory = log4c_category_get("RI.GST_Utils");

    if (NULL == args)
    {
        RILOG_ERROR("%s could not alloc args!?\n", __func__);
        return;
    }

    // Get GStreamer command line options from config file
    ricfg_getMultiValue("RIPlatform", "RI.Platform.gstargs", &args[1], &numArgs);
    numArgs++; // To account for mystery app name at arg 0
    args[0] = "ri";

    RILOG_INFO("%s() -- GStreamer cmd line args are:\n", __FUNCTION__);

    for (i = 1; i < numArgs; i++)
    {
        RILOG_INFO("\t%s\n", args[i]);
    }

    // Remove the default logging function and register our own
    //
    // FIXME -- Right now the GStreamer library is patched to not add
    // its default log function at init-time.  This is because of the
    // inability to pass function pointers between DLLs
    gst_debug_remove_log_function(gst_debug_log_default);
    gst_debug_add_log_function(rilog_debug_function, NULL);

    // initialize GStreamer from command line options
    g_option_context_add_group(ctx, gst_init_get_option_group());
    if (!g_option_context_parse(ctx, &numArgs, &args, &err))
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        g_free(err);
        err = NULL;
    }

    gst_debug_set_active(TRUE);
    gst_debug_set_colored(FALSE);

    RILOG_INFO("Loading GStreamer plugins...\n");
    err = NULL;
    path = g_module_build_path(NULL, "gstudp");

    if (NULL == gst_plugin_load_file(path, &err)) // udpsrc
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }
#ifdef EXTERNAL_CT_ENGINE
    g_free(path);
    path = g_module_build_path(NULL, "gsttcp");

    if (NULL == gst_plugin_load_file(path, &err)) // tcpsrc
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }
#endif
    g_free(path);
    path = g_module_build_path(NULL, "gstrtp");

    if (NULL == gst_plugin_load_file(path, &err)) // rtpmp2tdepay
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }

    g_free(path);
    path = g_module_build_path(NULL, "gstcoreelements");

    if (NULL == gst_plugin_load_file(path, &err)) // filesink
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }

    g_free(path);
    path = g_module_build_path(NULL, "gstcablelabs");

    if (NULL == gst_plugin_load_file(path, &err)) // CableLabs plugin
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }

    g_free(path);
    path = g_module_build_path(NULL, "gstapp");

    if (NULL == gst_plugin_load_file(path, &err)) // GstApp plugins from the Bad plugin suite
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }

    // Temporary plugin to allow testing of video until actual components are in place
    g_free(path);
    path = g_module_build_path(NULL, "gstvideotestsrc");

    if (NULL == gst_plugin_load_file(path, &err)) // video src
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }

    g_free(path);
    path = g_module_build_path(NULL, "gsttestvideosrc");

    if (NULL == gst_plugin_load_file(path, &err)) // video src
    {
        RILOG_FATAL(-2, "Error initializing GStreamer! -- %s\n", err->message);
    }

    if (NULL != err)
    {
        RILOG_WARN("warning initializing GStreamer: %s\n", err->message);
        g_free(err);
        err = NULL;
    }

    g_free(path);
    g_free(args);
}

GstElement* gst_load_element(const char* name, const char* alias)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    GstElement* element = NULL;
    element = gst_element_factory_make(name, alias);
    if (element == NULL)
    {
        RILOG_FATAL(-3, "Unable to instantiate %s element.\n", name);
    }
    else
    {
        RILOG_DEBUG("%s element has been successfully instantiated.\n", name);
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return element;
}

/**
 * Utility debugging/development function prints bins and elements in the specified bin.
 *
 * This function exists purely as a development aid - it has no functionality
 * that is useful for the operation of the system.
 *
 * It is assumed that the specified bin can be itself comprised of sub-bins of
 * elements.  This function will recurse through all of the bins and elements,
 * printing out the names of the sub-bins and elements contained therein,
 * along with their states.
 * Note that this is a recursive function that will be called for each bin
 * encountered.
 *
 * Usage notes:
 * 	Just call this function, specifying some message to help identify the dump
 * 	of bin elements, a 'root' bin (which can be a pipeline), and '0' for the
 * 	level.
 *
 * @param header message.
 * @param bin the bin to print information about.
 * @param level this should be set to 0.
 */
void print_bin_elements(char* message, GstBin* bin, int level)
{
    char* pszBinName;
    char* pszPadName;
    GstElement* pElement;
    GstPad* pPad;
    char indent[256];
    int i;
    gint refCount = -1;

    for (i = 0; i <= level; i++)
    {
        indent[i] = '\t';
    }
    indent[i] = '\0';

    // get the name of the bin
    pszBinName = gst_element_get_name(bin);

    // get the ref count of the bin
    refCount = GST_OBJECT_REFCOUNT_VALUE(bin);

    // print information about the current element
    if (0 == level)
    {
        RILOG_DEBUG("%s: %s state = %s, ref count = %d\n", message, pszBinName,
                gst_element_state_get_name(GST_STATE((GstElement*) bin)),
                refCount);
    }
    else
    {
        // The minus 1 on the refCount is due to the fact that this method
        //	has recursed into itself, and therefore, the element has had
        //	its reference incremented as a result of the
        //	gst_bin_iterate_elements() call.
        RILOG_DEBUG("%s %s state = %s, ref count = %d\n", indent, pszBinName,
                gst_element_state_get_name(GST_STATE((GstElement*) bin)),
                refCount - 1);
    }

    // walk the pads and print out info...	
    GstIterator* pItrSinkPads =
            gst_element_iterate_sink_pads((GstElement*) bin);
    while (GST_ITERATOR_OK
            == gst_iterator_next(pItrSinkPads, (gpointer*) &pPad))
    {
        // ...print out the child elements
        // GORP: fill in

        pszPadName = gst_pad_get_name(pPad);
        RILOG_INFO("%s   ## sinkpad = %s, blocked = %s, blocking = %s\n",
                indent, pszPadName, ((gst_pad_is_blocked(pPad) == 0) ? "NO"
                        : "YES"), ((gst_pad_is_blocking(pPad) == 0) ? "NO"
                        : "YES"));
        g_free(pszPadName);

        // the iterate function increments the refcount on the returned
        //	objects, so unref here
        gst_object_unref(pPad);
    }

    GstIterator* pItrSrcPads = gst_element_iterate_src_pads((GstElement*) bin);
    while (GST_ITERATOR_OK == gst_iterator_next(pItrSrcPads, (gpointer*) &pPad))
    {
        // ...print out the child elements
        // GORP: fill in

        pszPadName = gst_pad_get_name(pPad);
        RILOG_INFO("%s   ## srcpad = %s, blocked = %s, blocking = %s\n",
                indent, pszPadName, ((gst_pad_is_blocked(pPad) == 0) ? "NO"
                        : "YES"), ((gst_pad_is_blocking(pPad) == 0) ? "NO"
                        : "YES"));
        g_free(pszPadName);

        // the iterate function increments the refcount on the returned
        //	objects, so unref here
        gst_object_unref(pPad);
    }

    // if we are processing a bin...
    if (TRUE == GST_IS_BIN(bin))
    {
        // ...then we need to recurse into it so
        // for each element...
        GstIterator* pItr1 = gst_bin_iterate_elements(bin);
        while (GST_ITERATOR_OK == gst_iterator_next(pItr1,
                (gpointer*) &pElement))
        {
            // ...print out the child elements
            print_bin_elements(NULL, (GstBin*) pElement, level + 1);

            // the iterate function increments the refcount on the returned
            //	objects, so unref here
            gst_object_unref(pElement);
        }

        // freeing the iterator should decrement the refcount on the bin itself
        gst_iterator_free(pItr1);
    }

    g_free(pszBinName);
}

