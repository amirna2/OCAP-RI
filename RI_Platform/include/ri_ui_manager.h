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

#ifndef _RI_UI_MANAGER_H_
#define _RI_UI_MANAGER_H_

#include <stdarg.h>
#include <ri_types.h>
#include <ri_frontpanel.h>
#include <ri_backpanel.h>

typedef struct ri_ui_manager_s ri_ui_manager_t;
typedef struct ri_ui_manager_data_s ri_ui_manager_data_t;

typedef enum ri_event_type_enum
{
    RI_EVENT_TYPE_PRESSED = 0, RI_EVENT_TYPE_RELEASED, RI_EVENT_TYPE_LAST
} ri_event_type;

typedef enum ri_event_code_enum
{
    RI_VK_ENTER = 0,
    RI_VK_BACK_SPACE,
    RI_VK_TAB,
    RI_VK_UP,
    RI_VK_DOWN,
    RI_VK_LEFT,
    RI_VK_RIGHT,
    RI_VK_HOME,
    RI_VK_END,
    RI_VK_PAGE_DOWN,
    RI_VK_PAGE_UP,
    RI_VK_COLORED_KEY_0,
    RI_VK_COLORED_KEY_1,
    RI_VK_COLORED_KEY_2,
    RI_VK_COLORED_KEY_3,
    RI_VK_GUIDE,
    RI_VK_MENU,
    RI_VK_INFO,
    RI_VK_0,
    RI_VK_1,
    RI_VK_2,
    RI_VK_3,
    RI_VK_4,
    RI_VK_5,
    RI_VK_6,
    RI_VK_7,
    RI_VK_8,
    RI_VK_9,
    RI_VK_A,
    RI_VK_B,
    RI_VK_C,
    RI_VK_D,
    RI_VK_E,
    RI_VK_F,
    RI_VK_G,
    RI_VK_H,
    RI_VK_I,
    RI_VK_J,
    RI_VK_K,
    RI_VK_L,
    RI_VK_M,
    RI_VK_N,
    RI_VK_O,
    RI_VK_P,
    RI_VK_Q,
    RI_VK_R,
    RI_VK_S,
    RI_VK_T,
    RI_VK_U,
    RI_VK_V,
    RI_VK_W,
    RI_VK_X,
    RI_VK_Y,
    RI_VK_Z,
    RI_VK_VOLUME_UP,
    RI_VK_VOLUME_DOWN,
    RI_VK_MUTE,
    RI_VK_PLAY,
    RI_VK_PAUSE,
    RI_VK_STOP,
    RI_VK_REWIND,
    RI_VK_RECORD,
    RI_VK_FAST_FWD,
    RI_VK_SETTINGS,
    RI_VK_EXIT,
    RI_VK_CHANNEL_UP,
    RI_VK_CHANNEL_DOWN,
    RI_VK_ON_DEMAND,
    RI_VK_RF_BYPASS,
    RI_VK_POWER,
    RI_VK_LAST,
    RI_VK_NEXT_FAVORITE_CHANNEL,
    RI_VK_LIVE,
    RI_VK_LIST,
    RI_OCRC_LAST
} ri_event_code;

typedef enum ri_log_level_enum
{
    RI_LOG_LEVEL_FATAL,
    RI_LOG_LEVEL_ERROR,
    RI_LOG_LEVEL_WARN,
    RI_LOG_LEVEL_INFO,
    RI_LOG_LEVEL_DEBUG,
    RI_LOG_LEVEL_TRACE
} ri_log_level;

/**
 * This structure represents a User Interface manager.
 */
struct ri_ui_manager_s
{
    /**
     * Log a message from a client to the RI Platform.
     */
    void (*log_msg)(ri_ui_manager_t* object, ri_log_level level,
            const char* module, const char* format, va_list args);

    /**
     * Reset the platform.
     */
    void (*platform_reset)(void);

    /**
     * Register key event callback.
     */
    void (*register_key_event_cb)(ri_ui_manager_t* object, void(*cb)(
            ri_event_type type, ri_event_code code));

    ri_error (*register_key_event_cb_mfg)(ri_ui_manager_t* object, void(*cb)(
            ri_event_type type, ri_event_code code));

    // Private UI Manager data
    ri_ui_manager_data_t* data;
};

/**
 * Returns the singleton instance of the user interface manager for the RI platform
 *
 * @return the UI manager singleton instance
 */
RI_MODULE_EXPORT ri_ui_manager_t* ri_get_ui_manager(void);

/**
 * Returns the singleton instance of the front panel for the RI platform.
 *
 * @return the Front Panel singleton instance
 */
RI_MODULE_EXPORT ri_frontpanel_t* ri_get_frontpanel(void);

/**
 * Returns the singleton instance of the back panel for the RI platform.
 *
 * @return the Back Panel singleton instance
 */
RI_MODULE_EXPORT ri_backpanel_t* ri_get_backpanel(void);

/**
 * Processes key input.
 * <p>
 * A key event is generated and passed through to the RI Platform,
 * as if the event had been generated from a remote.
 * </p>
 *
 * @param value The key value to handle. It should be a valid enumeration
 * of type <code>ri_event_code</code>.
 */
RI_MODULE_EXPORT void ri_process_key(ri_event_code value);

/**
 * Processes key pressed input.
 * <p>
 * A key pressed event is generated and passed through to the RI Platform,
 * as if the event had been generated from a remote.
 * </p>
 *
 * @param value The key value to handle. It should be a valid enumeration
 * of type <code>ri_event_code</code>.
 */
RI_MODULE_EXPORT void ri_process_key_pressed(ri_event_code value);

/**
 * Processes key released input.
 * <p>
 * A key released event is generated and passed through to the RI Platform,
 * as if the event had been generated from a remote.
 * </p>
 *
 * @param value The key value to handle. It should be a valid enumeration
 * of type <code>ri_event_code</code>.
 */
RI_MODULE_EXPORT void ri_process_key_released(ri_event_code value);

/**
 * A blocking call to process a log entry, possibly resulting the the log entry
 * becoming accessible via the {ocStbHostSystemLogging} table.
 * The entry will not be accessible if table processing is paused or if the
 * group or level threshold criteria of this event do not meet the values
 * specified by the {ocStbHostSystemLoggingResetControl,
 * ocStbHostSystemLoggingLevel, or ocStbHostSystemLoggingGroup} controls.
 *
 * @param oid is a string representation of OID associated with the log table.
 * @param timeStamp is a string representation of the millisecond timestamp
 *                  recorded at the time the event occurred.
 * @param message is the pre-formatted log message that will be truncated at
 *                table entry to 256 bytes (inclusive of a null character).
 *
 * @return TRUE if the log was successfully processed
 * @return FALSE if the log could not be added due to memory limitations,
 *               invalid params (bad OID, null message, etc).
 */
RI_MODULE_EXPORT ri_bool ri_snmpAddLogEntry(char *oid, char *timeStamp, char *message);

#endif /* _RI_UI_MANAGER_H_ */
