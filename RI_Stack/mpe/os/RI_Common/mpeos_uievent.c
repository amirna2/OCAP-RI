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

/* Header Files */
#include "mpeos_uievent.h"
#include "mpeos_dbg.h"

#include <glib.h>
#include <ri_ui_manager.h>
#include <ri_test_interface.h>
#include <string.h>

static void platform_key_event_cb(ri_event_type type, ri_event_code code);

// GLib does not support mutex locks with timeout, so we will need
// a conditional variable.
static GMutex *event_queue_mutex = NULL;
static GCond *event_queue_cond = NULL;
static GQueue *event_queue = NULL;
static ri_ui_manager_t *ri_ui_manager = NULL;

static int32_t ri_mpe_event_type_translation[RI_EVENT_TYPE_LAST] =
{ OCAP_KEY_PRESSED, /* RI_EVENT_TYPE_PRESSED  */
OCAP_KEY_RELEASED /* RI_EVENT_TYPE_RELEASED */
};

static int32_t ri_mpe_event_code_translation[RI_OCRC_LAST] =
{ OCAP_VK_ENTER, /* RI_VK_ENTER         */
OCAP_VK_BACK_SPACE, /* RI_VK_BACK_SPACE    */
OCAP_VK_TAB, /* RI_VK_TAB           */
OCAP_VK_UP, /* RI_VK_UP            */
OCAP_VK_DOWN, /* RI_VK_DOWN          */
OCAP_VK_LEFT, /* RI_VK_LEFT          */
OCAP_VK_RIGHT, /* RI_VK_RIGHT         */
OCAP_VK_HOME, /* RI_VK_HOME          */
OCAP_VK_END, /* RI_VK_END           */
OCAP_VK_PAGE_DOWN, /* RI_VK_PAGE_DOWN     */
OCAP_VK_PAGE_UP, /* RI_VK_PAGE_UP       */
OCAP_VK_COLORED_KEY_0, /* RI_VK_COLORED_KEY_0 */
OCAP_VK_COLORED_KEY_1, /* RI_VK_COLORED_KEY_1 */
OCAP_VK_COLORED_KEY_2, /* RI_VK_COLORED_KEY_2 */
OCAP_VK_COLORED_KEY_3, /* RI_VK_COLORED_KEY_3 */
OCAP_VK_GUIDE, /* RI_VK_GUIDE         */
OCAP_VK_MENU, /* RI_VK_MENU          */
OCAP_VK_INFO, /* RI_VK_INFO          */
OCAP_VK_0, /* RI_VK_0             */
OCAP_VK_1, /* RI_VK_1             */
OCAP_VK_2, /* RI_VK_2             */
OCAP_VK_3, /* RI_VK_3             */
OCAP_VK_4, /* RI_VK_4             */
OCAP_VK_5, /* RI_VK_5             */
OCAP_VK_6, /* RI_VK_6             */
OCAP_VK_7, /* RI_VK_7             */
OCAP_VK_8, /* RI_VK_8             */
OCAP_VK_9, /* RI_VK_9             */
OCAP_VK_A, /* RI_VK_A             */
OCAP_VK_B, /* RI_VK_B             */
OCAP_VK_C, /* RI_VK_C             */
OCAP_VK_D, /* RI_VK_D             */
OCAP_VK_E, /* RI_VK_E             */
OCAP_VK_F, /* RI_VK_F             */
OCAP_VK_G, /* RI_VK_G             */
OCAP_VK_H, /* RI_VK_H             */
OCAP_VK_I, /* RI_VK_I             */
OCAP_VK_J, /* RI_VK_J             */
OCAP_VK_K, /* RI_VK_K             */
OCAP_VK_L, /* RI_VK_L             */
OCAP_VK_M, /* RI_VK_M             */
OCAP_VK_N, /* RI_VK_N             */
OCAP_VK_O, /* RI_VK_O             */
OCAP_VK_P, /* RI_VK_P             */
OCAP_VK_Q, /* RI_VK_Q             */
OCAP_VK_R, /* RI_VK_R             */
OCAP_VK_S, /* RI_VK_S             */
OCAP_VK_T, /* RI_VK_T             */
OCAP_VK_U, /* RI_VK_U             */
OCAP_VK_V, /* RI_VK_V             */
OCAP_VK_W, /* RI_VK_W             */
OCAP_VK_X, /* RI_VK_X             */
OCAP_VK_Y, /* RI_VK_Y             */
OCAP_VK_Z, /* RI_VK_Z             */
OCAP_VK_VOLUME_UP, /* RI_VK_VOLUME_UP     */
OCAP_VK_VOLUME_DOWN, /* RI_VK_VOLUME_DOWN   */
OCAP_VK_MUTE, /* RI_VK_MUTE          */
OCAP_VK_PLAY, /* RI_VK_PLAY          */
OCAP_VK_PAUSE, /* RI_VK_PAUSE         */
OCAP_VK_STOP, /* RI_VK_STOP          */
OCAP_VK_REWIND, /* RI_VK_REWIND        */
OCAP_VK_RECORD, /* RI_VK_RECORD        */
OCAP_VK_FAST_FWD, /* RI_VK_FAST_FWD      */
OCAP_VK_SETTINGS, /* RI_VK_SETTINGS      */
OCAP_VK_EXIT, /* RI_VK_EXIT          */
OCAP_VK_CHANNEL_UP, /* RI_VK_CHANNEL_UP    */
OCAP_VK_CHANNEL_DOWN, /* RI_VK_CHANNEL_DOWN  */
OCAP_VK_ON_DEMAND, /* RI_VK_ON_DEMAND     */
OCAP_VK_RF_BYPASS, /* RI_VK_RF_BYPASS     */
OCAP_VK_POWER, /* RI_VK_POWER         */
OCAP_VK_LAST, /* RI_VK_LAST          */
OCAP_VK_NEXT_FAVORITE_CHANNEL, /* RI_VK_NEXT_FAVORITE_CHANNEL */
OCAP_VK_LIVE, /* RI_VK_LIVE */
OCAP_VK_LIST /* RI_VK_LIST */
};

// For RI test interface (telnet interface)
#define MPEOS_MENUS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| b | TSB Buffering\r\n" \
    "|---+-----------------------\r\n" \
    "| d | Display\r\n" \
    "|---+-----------------------\r\n" \
    "| h | HN\r\n" \
    "|---+-----------------------\r\n" \
    "| p | POD\r\n" \
    "|---+-----------------------\r\n" \
    "| n | Tuning\r\n" \
    "|---+-----------------------\r\n" \
    "| s | Storage\r\n" \
    "|---+-----------------------\r\n" \
    "| t | 3DTV Tests\r\n"

// For RI test interface (telnet interface) 3DTV Tests
#define MPEOS_3DTV_MENU \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| b | Broadcast Playback\r\n" \
    "|---+-----------------------\r\n" \
    "| h | HN Playback\r\n" \
    "|---+-----------------------\r\n" \
    "| r | Recording Playback\r\n" \
    "|---+-----------------------\r\n" \
    "| t | TSB Playback\r\n"

static int mpeosMenuInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    switch (rxBuf[0])
    {
        case 'b':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("TSB Buffering")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s TSB Buffering sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 'd':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("MPEOS Display")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s MPEOS Display sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 'h':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("HN")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s HN sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 'p':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("POD")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s POD sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 's':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("Storage")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s Storage sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 't':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("3DTV Tests")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s 3DTV Tests sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 'n':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("Tuning")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s Tuner Tests sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }
    } // END switch (rxBuf[0])

    return 0;
} // END mpeosMenuInputHandler()

static MenuItem MpeosMenuItem =
{ true, "m", "MPEOS", MPEOS_MENUS, mpeosMenuInputHandler };

static int test3DTVMenuInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_POD, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    switch (rxBuf[0])
    {
        case 'b':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("Media")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s Broadcast Playback sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 'h':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("HN 3DTV Test")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s HN 3DTV sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 'r':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("Recording Playback")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s Recording Playback sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }

        case 't':
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("TSB Playback")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_UI,
                      "%s TSB Playback sub-menu failed?\n", __FUNCTION__);
            *retCode = MENU_FAILURE;
            break;
        }
        else
        {
            return 1;
        }
    } // END switch (rxBuf[0])

    return 0;
} // END test3DTVMenuInputHandler()


static MenuItem Test3DTVMenuItem =
{ false, "m", "3DTV Tests", MPEOS_3DTV_MENU, test3DTVMenuInputHandler };

/**
 * <i>mpeos_initUIEvents</i> initializes a queue, and registers it for reception
 * of user input events. This queue will be utilized by the mpeos_gfxWaitNextEvent set
 * of functions
 */
void mpeos_initUIEvents(void)
{
    event_queue_mutex = g_mutex_new();
    event_queue_cond = g_cond_new();
    event_queue = g_queue_new();
    ri_ui_manager = ri_get_ui_manager();

    ri_ui_manager->register_key_event_cb(ri_ui_manager, platform_key_event_cb);
 
    // Register the base MPEOS Telnet Interface Menu
    ri_test_RegisterMenu(&MpeosMenuItem);

    // Register the 3DTV Tests Telnet Interface Menu
    ri_test_RegisterMenu(&Test3DTVMenuItem);
}

/**
 * The <i>mpeos_gfxWaitNextEvent</i> waits for the system to generate a user input event
 * for a specified length of time. If an event is received in the specified time,
 * the caller's mpe_GfxEvent structure will be filled with the event data.
 *
 * @param event    the caller's event structure to be filled
 * @param timeout  the length of time to wait, in milliseconds
 *
 * @return         MPE_SUCCESS if an event is received, otherwise MPE_ETIMEOUT
 */
mpe_Error mpeos_gfxWaitNextEvent(mpe_GfxEvent *event, uint32_t timeout)
{
    mpe_Error retval = MPE_SUCCESS;
    GTimeVal abs_time =
    { 0, 0 };
    gboolean cond_signalled = TRUE;
    mpe_GfxEvent *queued_event = NULL;

    g_get_current_time(&abs_time);
    g_time_val_add(&abs_time, timeout * 1000); // timeout is in msec, function expects usec

    g_mutex_lock(event_queue_mutex);
    while (g_queue_is_empty(event_queue) && cond_signalled == TRUE)
    {
        cond_signalled = g_cond_timed_wait(event_queue_cond, event_queue_mutex,
                &abs_time);
    }

    if (cond_signalled)
    {
        queued_event = (mpe_GfxEvent*) g_queue_pop_head(event_queue);
        g_assert(queued_event != NULL);
        retval = MPE_SUCCESS;
    }
    else
    {
        retval = MPE_ETIMEOUT;
    }
    g_mutex_unlock(event_queue_mutex);

    if (queued_event != NULL)
    {
        event->eventId = queued_event->eventId;
        event->eventCode = queued_event->eventCode;
        g_free(queued_event);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UI,
                "mpeos_gfxWaitNextEvent: returning type: %d, code: %d\n",
                event->eventId, event->eventCode);

        // Toggle the power mode if the power key was selected.
        if ((event->eventId == OCAP_KEY_PRESSED) && (event->eventCode
                == OCAP_VK_POWER))
            togglePowerMode();
    }
    else
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UI,
                "mpeos_gfxWaitNextEvent: timeout of %dms occurred\n", timeout);
    }

    return retval;
}

/**
 * The <i>mpeos_gfxGeneratePlatformKeyEvent</i> causes the system to generate an event as if it
 * was received via the system's native facility.
 *
 * @param eventId   Event type: e.g., OCAP_KEY_PRESSED, OCAP_KEY_RELEASED
 * @param eventCode VK key code: e.g., VK_ENTER, VK_EXIT
 *
 * @return         MPE_SUCCESS if an event is generated successfully
 *                 or an appropriate error code if the event could not be generated.
 */
mpe_Error mpeos_gfxGeneratePlatformKeyEvent(int32_t eventId, int32_t eventCode)
{
    mpe_Error retval = MPE_SUCCESS;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UI,
            "mpeos_gfxGeneratePlatformKeyEvent: received type: %d, code: %d\n",
                eventId, eventCode);

    mpe_GfxEvent* event = NULL;

    event = g_malloc(sizeof(mpe_GfxEvent));
    event->eventId = eventId;
    event->eventCode = eventCode;
    event->eventChar = (int32_t)NULL;
    event->rsvd[0] = (int32_t) NULL;

    // Send this event to the queue just as if it had been received from platform
    g_mutex_lock(event_queue_mutex);
    g_queue_push_tail(event_queue, (gpointer)event);
    g_cond_signal(event_queue_cond);
    g_mutex_unlock(event_queue_mutex);

    return retval;
}

/**
 * Platform key event callback - registered in mpeos_initUIEvents().
 **/
void platform_key_event_cb(ri_event_type type, ri_event_code code)
{
    mpe_GfxEvent* event = NULL;

    event = g_malloc(sizeof(mpe_GfxEvent));
    event->eventId = ri_mpe_event_type_translation[type];
    event->eventCode = ri_mpe_event_code_translation[code];
    event->eventChar = (int32_t) NULL;
    event->rsvd[0] = (int32_t) NULL;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_UI,
            "platform_key_event_cb: received type: %d, code: %d\n", type, code);

    g_mutex_lock(event_queue_mutex);
    g_queue_push_tail(event_queue, (gpointer) event);
    g_cond_signal(event_queue_cond);
    g_mutex_unlock(event_queue_mutex);
}
