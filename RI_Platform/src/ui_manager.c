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


// Include system header files.
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

// Include RI Platform header files.
#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>
#include <ri_ui_manager.h>
#include <ri_display.h>
#include <ri_log.h>
#include <gstreamer/gstdisplay.h>
#include <glib.h>

#include "platform.h"
#include "display.h"
#include "frontpanel.h"
#include "backpanel.h"
#include "ui_window_common.h"

#define RILOG_CATEGORY uiManagerCat
log4c_category_t* uiManagerCat = NULL;
log4c_category_t* stackCat = NULL;
log4c_category_t* riUserCat = NULL;

static ri_ui_manager_t* ui_manager_instance = NULL;

// UI Manager private data
struct ri_ui_manager_data_s
{
    //  one callback for each environment is accepted at this point.
    void (*key_event_callback)(ri_event_type, ri_event_code);
    void (*key_event_callback_mfg)(ri_event_type, ri_event_code);
};

static void log_msg(ri_ui_manager_t* object, ri_log_level level,
        const char *module, const char* format, va_list args)
{
    log4c_category_t* cat = NULL;
    if ((module != NULL) && (strcmp(module, "JVM") == 0))
    {
        cat = riUserCat;
    }
    else
    {
        cat = stackCat;
    }

    switch (level)
    {
    case RI_LOG_LEVEL_FATAL:
        RILOGV_FATAL_C(cat, format, args);
        break;
    case RI_LOG_LEVEL_ERROR:
        RILOGV_ERROR_C(cat, format, args);
        break;
    case RI_LOG_LEVEL_WARN:
        RILOGV_WARN_C(cat, format, args);
        break;
    case RI_LOG_LEVEL_INFO:
        RILOGV_INFO_C(cat, format, args);
        break;
    case RI_LOG_LEVEL_DEBUG:
        RILOGV_DEBUG_C(cat, format, args);
        break;
    case RI_LOG_LEVEL_TRACE:
        RILOGV_TRACE_C(cat, format, args);
        break;
    default:
        RILOGV_DEBUG_C(cat, format, args);
        break;
    }
}

static void register_key_event_cb(ri_ui_manager_t* object, void(*cb)(
        ri_event_type type, ri_event_code code))
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    if (object->data->key_event_callback != NULL)
    {
        RILOG_WARN("%s -- Callback already registered - removing old callback",
                __FUNCTION__);
    }
    object->data->key_event_callback = cb;
    window_register_key_event_callback(cb);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

static ri_error register_key_event_cb_mfg(ri_ui_manager_t* object, void(*cb)(
        ri_event_type type, ri_event_code code))
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    if (object->data->key_event_callback_mfg != NULL)
    {
        RILOG_WARN("%s -- Callback already registered - removing old callback",
                __FUNCTION__);
    }

    object->data->key_event_callback_mfg = cb;
    window_register_key_event_callback_mfg(cb);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return RI_ERROR_NONE;
}

void create_ui_manager()
{
    uiManagerCat = log4c_category_get("RI.UIManager");
    stackCat = log4c_category_get("RI.Stack");
    riUserCat = log4c_category_get("RI.Stack.StdOut");

    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);

    ui_manager_instance = g_try_malloc0(sizeof(ri_ui_manager_t));

    if (NULL == ui_manager_instance)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    ui_manager_instance->log_msg = log_msg;
    ui_manager_instance->platform_reset = platform_reset;
    ui_manager_instance->register_key_event_cb = register_key_event_cb;
    ui_manager_instance->register_key_event_cb_mfg = register_key_event_cb_mfg;

    ui_manager_instance->data = g_try_malloc0(sizeof(ri_ui_manager_data_t));

    if (NULL == ui_manager_instance->data)
    {
        RILOG_FATAL(-1, "line %d of %s, %s memory allocation failure!\n",
                    __LINE__, __FILE__, __func__);
    }

    // Get the display associated with the GStreamer pipeline.
    ri_pipeline_manager_t *pipeline_manager = ri_get_pipeline_manager();
    if (NULL == pipeline_manager)
    {
        RILOG_ERROR("%s -- Pipeline manager was NULL\n", __FUNCTION__);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    ri_display_t* display = pipeline_manager->get_display(pipeline_manager);
    if (NULL == display)
    {
        RILOG_ERROR("%s -- Display returned by pipeline manager was NULL\n",
                __FUNCTION__);

        RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
        return;
    }

    /*
     uint32_t num_pipelines;
     ri_pipeline_t **pipelines = (ri_pipeline_t **)pipeline_manager->get_live_pipelines(pipeline_manager, &num_pipelines);
     ri_pipeline_t *pipeline = pipelines[0];
     ri_display_t* display;
     if (NULL != pipeline)
     {
     RILOG_WARN("about to call get_display\n", __FUNCTION__);
     display = pipeline->get_display(pipeline);
     if (NULL == display)
     {
     RILOG_WARN("display returned by pipeline was NULL", __FUNCTION__);
     return;
     }
     }
     else
     {
     RILOG_WARN("pipeline was NULL", __FUNCTION__);
     return;
     }
     */

    // Initialize the window manager.
    UIInfo *context = display_get_context(display);

    // Initiate window creation if configured to be supplied externally
    if (context->pWindowInfo->is_supplied)
    {
        uint32_t window_id = window_init(context);
        //uint32_t window_id = window_init(NULL);
        if (window_id != 0)
        {
            // Using an external Window handle, so initialize the display.
            display_set_window_id(display, window_id);
        }
    }

    // Register key events.
    ui_manager_instance->data->key_event_callback = NULL;
    window_register_key_event_callback(
            ui_manager_instance->data->key_event_callback);

    //added to allow window to "fake" video output port connect/disconnect events.
    window_register_display_event_callback(&toggleDisplayConnectedDisconnected);

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

ri_ui_manager_t* ri_get_ui_manager(void)
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    if (ui_manager_instance == NULL)
    {
        create_ui_manager();
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
    return ui_manager_instance;
}

ri_frontpanel_t* ri_get_frontpanel(void)
{
    // May be NULL if no front panel exists.
    return get_frontpanel();
}

ri_backpanel_t* ri_get_backpanel(void)
{
    // May be NULL if no back panel exists.
    return get_backpanel();
}

void destroy_ui_manager()
{
    RILOG_TRACE("%s -- Entry\n", __FUNCTION__);
    if (NULL != ui_manager_instance)
    {
        if (NULL != ui_manager_instance->data)
        {
            g_free(ui_manager_instance->data);
        }

        // Until stack shutdown is properly implemented
        // the following code must not be executed as
        // it is causing JVM crashes, if any logging
        // is output after the platform has shut down.
        //g_free(ui_manager_instance);
    }
    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void ri_process_key_pressed(ri_event_code value)
{
    RILOG_TRACE("%s -- Entry;\n", __FUNCTION__);

    // Process the key input and forward to RI Platform.
    ri_ui_manager_t* uiManager = ri_get_ui_manager();
    if (uiManager != NULL)
    {
        void (*cb)(ri_event_type, ri_event_code);
        cb = uiManager->data->key_event_callback;
        if (cb != NULL)
        {
            RILOG_DEBUG("######### Processing Key Pressed: %d ##########\n",
                    value);
            (*cb)(RI_EVENT_TYPE_PRESSED, value);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void ri_process_key_released(ri_event_code value)
{
    RILOG_TRACE("%s -- Entry;\n", __FUNCTION__);

    // Process the key input and forward to RI Platform.
    ri_ui_manager_t* uiManager = ri_get_ui_manager();
    if (uiManager != NULL)
    {
        void (*cb)(ri_event_type, ri_event_code);
        cb = uiManager->data->key_event_callback;
        if (cb != NULL)
        {
            RILOG_DEBUG("######### Processing Key Released: %d ##########\n",
                    value);
            (*cb)(RI_EVENT_TYPE_RELEASED, value);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

void ri_process_key(ri_event_code value)
{
    RILOG_TRACE("%s -- Entry;\n", __FUNCTION__);

    // Process the key input and forward to RI Platform.
    ri_ui_manager_t* uiManager = ri_get_ui_manager();
    if (uiManager != NULL)
    {
        void (*cb)(ri_event_type, ri_event_code);
        cb = uiManager->data->key_event_callback;
        if (cb != NULL)
        {
            RILOG_DEBUG("######### Processing Key: %d ##########\n", value);
            (*cb)(RI_EVENT_TYPE_PRESSED, value);
            (*cb)(RI_EVENT_TYPE_RELEASED, value);
        }
    }

    RILOG_TRACE("%s -- Exit\n", __FUNCTION__);
}

extern ri_bool SnmpAddLogEntry(char *oid, char *timeStamp, char *message);

ri_bool ri_snmpAddLogEntry(char *oid, char *timeStamp, char *message)
{
    return SnmpAddLogEntry(oid, timeStamp, message);
}

