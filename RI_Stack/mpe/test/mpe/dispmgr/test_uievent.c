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

#include "test_disp.h"
#include "mpe_uievents.h"

void test_gfxWaitNextEvent(CuTest *tc);
void test_gfxWaitNextEvent_poll(CuTest *tc);
void eventName(mpe_GfxEvent * uievent);
CuSuite* getTestSuite_gfxEvent(void);

/**
 * Tests the gfxWaitNextEvent() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxWaitNextEvent(CuTest *tc)
{
    /*USER_OK("After hitting OK, please send a remote event."
     " If this doesn't occur within 5sec, the test will fail.",
     "test_gfxWaitNextEvent"); */

    /* Should tell user what to push */
    bool run = true;

    while (run)
    {
        mpe_GfxEvent keyevent =
        { -1, -1, };
        mpe_Error ec;

        threadSleep(3000, 0);
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "Hit any key within 60 seconds\n");
        ec = gfxWaitNextEvent(&keyevent, 60000);
        if (ec == MPE_SUCCESS)
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "Keypress received");
        else if (ec = MPE_ETIMEOUT)
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "Timeout occured");
        CuAssert(tc, "gfxWaitNextEvent failed", ec == MPE_SUCCESS);
        eventName(&keyevent);
        CuAssert(tc, "gfxWaitNextEvent returned a bad value", keyevent.eventId
                != -1);
        if (keyevent.eventCode == OCAP_VK_STOP)
        {
            run = false;
            TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST,
                    "STOP pressed, exiting out of test\n");
        }
    }

    /* Check for key up/down */
#if 0 /* TODO */
    //CuFail(tc, "Unfinished test");
#endif

}

/**
 * Tests the gfxWaitNextEvent() call.
 *
 * @param tc pointer to test case structure
 */
void test_gfxWaitNextEvent_poll(CuTest *tc)
{
#if 0
    /* USER_OK("If test hangs after hitting OK, then there was a failure.", "test_gfxWaitNextEvent_poll"); */

    mpe_GfxEvent event =
    {   -1, -1,};
    time_t t = time(NULL);

    mpe_Error ec = gfxWaitNextEvent(&event, 0);
    /* Check that it returned RIGHT away! */
    CuAssert(tc, "gfxWaitNextEvent(0) should return success",
            ec == MPE_SUCCESS);
    CuAssert(tc, "gfxWaitNextEvent(0) should return right away",
            t+1 >= time(NULL)); // allow for time rollover to next second
#endif

}

/**
 * Helper function that correlates the name of the key to 
 * the event generated
 */
void eventName(mpe_GfxEvent * uievent)
{
    switch (uievent->eventCode)
    {
    case OCAP_KEY_FIRST:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_KEY_FIRST \n");
        break;
    case OCAP_KEY_LAST:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_KEY_LAST \n");
        break;
    case OCAP_KEY_PRESSED:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_KEY_PRESSED \n");
        break;
    case OCAP_VK_ENTER:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ENTER \n");
        break;
    case OCAP_VK_BACK_SPACE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_BACK_SPACE \n");
        break;
    case OCAP_VK_TAB:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_TAB \n");
        break;
    case OCAP_VK_CANCEL:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CANCEL \n");
        break;
    case OCAP_VK_CLEAR:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CLEAR \n");
        break;
    case OCAP_VK_SHIFT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SHIFT \n");
        break;
    case OCAP_VK_CONTROL:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CONTROL \n");
        break;
    case OCAP_VK_ALT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ALT \n");
        break;
    case OCAP_VK_PAUSE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PAUSE \n");
        break;
    case OCAP_VK_CAPS_LOCK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CAPS_LOCK \n");
        break;
    case OCAP_VK_ESCAPE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ESCAPE \n");
        break;
    case OCAP_VK_SPACE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SPACE\n");
        break;
    case OCAP_VK_PAGE_UP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PAGE_UP\n");
        break;
    case OCAP_VK_PAGE_DOWN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PAGE_DOWN \n");
        break;
    case OCAP_VK_END:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_END \n");
        break;
    case OCAP_VK_HOME:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_HOME \n");
        break;
    case OCAP_VK_LEFT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_LEFT \n");
        break;
    case OCAP_VK_UP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_UP \n");
        break;
    case OCAP_VK_RIGHT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RIGHT \n");
        break;
    case OCAP_VK_DOWN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_DOWN \n");
        break;
    case OCAP_VK_COMMA:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_COMMA \n");
        break;
    case OCAP_VK_PERIOD:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PERIOD \n");
        break;
    case OCAP_VK_SLASH:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SLASH \n");
        break;
    case OCAP_VK_0:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_0 \n");
        break;
    case OCAP_VK_1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_1 \n");
        break;
    case OCAP_VK_2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_2 \n");
        break;
    case OCAP_VK_3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_3 \n");
        break;
    case OCAP_VK_4:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_4 \n");
        break;
    case OCAP_VK_5:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_5 \n");
        break;
    case OCAP_VK_6:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_6 \n");
        break;
    case OCAP_VK_7:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_7 \n");
        break;
    case OCAP_VK_8:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_8 \n");
        break;
    case OCAP_VK_9:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_9 \n");
        break;
    case OCAP_VK_SEMICOLON:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SEMICOLON \n");
        break;
    case OCAP_VK_EQUALS:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_EQUALS \n");
        break;
    case OCAP_VK_A:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_A \n");
        break;
    case OCAP_VK_B:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_B \n");
        break;
    case OCAP_VK_C:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_C \n");
        break;
    case OCAP_VK_D:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_D \n");
        break;
    case OCAP_VK_E:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_E \n");
        break;
    case OCAP_VK_F:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F \n");
        break;
    case OCAP_VK_G:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_G \n");
        break;
    case OCAP_VK_H:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_H \n");
        break;
    case OCAP_VK_I:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_I \n");
        break;
    case OCAP_VK_J:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_J \n");
        break;
    case OCAP_VK_K:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_K \n");
        break;
    case OCAP_VK_L:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_L \n");
        break;
    case OCAP_VK_M:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_M \n");
        break;
    case OCAP_VK_N:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_N \n");
        break;
    case OCAP_VK_O:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_O \n");
        break;
    case OCAP_VK_P:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_P \n");
        break;
    case OCAP_VK_Q:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_Q \n");
        break;
    case OCAP_VK_R:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_R \n");
        break;
    case OCAP_VK_S:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_S \n");
        break;
    case OCAP_VK_T:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_T \n");
        break;
    case OCAP_VK_U:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_U \n");
        break;
    case OCAP_VK_V:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_V \n");
        break;
    case OCAP_VK_W:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_W \n");
        break;
    case OCAP_VK_X:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_X \n");
        break;
    case OCAP_VK_Y:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_Y \n");
        break;
    case OCAP_VK_Z:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_Z \n");
        break;
    case OCAP_VK_OPEN_BRACKET:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_OPEN_BRACKET \n");
        break;
    case OCAP_VK_BACK_SLASH:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SLASH \n");
        break;
    case OCAP_VK_CLOSE_BRACKET:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CLOSE_BRACKET \n");
        break;
    case OCAP_VK_NUMPAD0:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD0 \n");
        break;
    case OCAP_VK_NUMPAD1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD1 \n");
        break;
    case OCAP_VK_NUMPAD2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD2 \n");
        break;
    case OCAP_VK_NUMPAD3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD3 \n");
        break;
    case OCAP_VK_NUMPAD4:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD4 \n");
        break;
    case OCAP_VK_NUMPAD5:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD5 \n");
        break;
    case OCAP_VK_NUMPAD6:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD6 \n");
        break;
    case OCAP_VK_NUMPAD7:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD7 \n");
        break;
    case OCAP_VK_NUMPAD8:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD8 \n");
        break;
    case OCAP_VK_NUMPAD9:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMPAD9 \n");
        break;
    case OCAP_VK_MULTIPLY:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_MULTIPLY \n");
        break;
    case OCAP_VK_ADD:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ADD \n");
        break;
    case OCAP_VK_SEPARATER:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SEPARATER \n");
        break;
    case OCAP_VK_SUBTRACT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SUBTRACT \n");
        break;
    case OCAP_VK_DECIMAL:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_DECIMAL \n");
        break;
    case OCAP_VK_DIVIDE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_DIVIDE \n");
        break;
    case OCAP_VK_F1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F1 \n");
        break;
    case OCAP_VK_F2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F2 \n");
        break;
    case OCAP_VK_F3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F3 \n");
        break;
    case OCAP_VK_F4:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F4 \n");
        break;
    case OCAP_VK_F5:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F5 \n");
        break;
    case OCAP_VK_F6:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F6 \n");
        break;
    case OCAP_VK_F7:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F7 \n");
        break;
    case OCAP_VK_F8:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F8 \n");
        break;
    case OCAP_VK_F9:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F9 \n");
        break;
    case OCAP_VK_F10:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F10 \n");
        break;
    case OCAP_VK_F11:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F11 \n");
        break;
    case OCAP_VK_F12:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_F12 \n");
        break;
    case OCAP_VK_DELETE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_DELETE \n");
        break;
    case OCAP_VK_NUM_LOCK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUM_LOCK \n");
        break;
    case OCAP_VK_SCROLL_LOCK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SCROLL_LOCK \n");
        break;
    case OCAP_VK_ASTERISK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ASTERISK \n");
        break;
    case OCAP_VK_PRINTSCREEN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PRINTSCREEN \n");
        break;
    case OCAP_VK_INSERT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_INSERT \n");
        break;
    case OCAP_VK_HELP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_HELP \n");
        break;
    case OCAP_VK_META:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_META \n");
        break;
    case OCAP_VK_BACK_QUOTE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_BACK_QUOTE \n");
        break;
    case OCAP_VK_QUOTE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_QUOTE \n");
        break;
    case OCAP_VK_FINAL:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_FINAL \n");
        break;
    case OCAP_VK_CONVERT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CONVERT \n");
        break;
    case OCAP_VK_NONCONVERT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NONCONVERT \n");
        break;
    case OCAP_VK_ACCEPT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ACCEPT \n");
        break;
    case OCAP_VK_MODECHANGE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_MODECHANGE \n");
        break;
    case OCAP_VK_KANA:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_KANA \n");
        break;
    case OCAP_VK_KANJI:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_KANJI \n");
        break;
    case OCAP_VK_UNDEFINED:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_UNDEFINED \n");
        break;
    case OCAP_VK_COLORED_KEY_0:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_COLORED_KEY_0 \n");
        break;
    case OCAP_VK_COLORED_KEY_1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_COLORED_KEY_1 \n");
        break;
    case OCAP_VK_COLORED_KEY_2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_COLORED_KEY_2 \n");
        break;
    case OCAP_VK_COLORED_KEY_3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_COLORED_KEY_3 \n");
        break;
    case OCAP_VK_COLORED_KEY_4:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_COLORED_KEY_4 \n");
        break;
    case OCAP_VK_COLORED_KEY_5:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_COLORED_KEY_5 \n");
        break;
    case OCAP_VK_POWER:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_POWER \n");
        break;
    case OCAP_VK_DIMMER:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_DIMMER \n");
        break;
    case OCAP_VK_WINK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_WINK \n");
        break;
    case OCAP_VK_REWIND:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_REWIND \n");
        break;
    case OCAP_VK_STOP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_STOP \n");
        break;
    case OCAP_VK_EJECT_TOGGLE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_EJECT \n");
        break;
    case OCAP_VK_PLAY:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PLAY \n");
        break;
    case OCAP_VK_RECORD:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RECORD \n");
        break;
    case OCAP_VK_FAST_FWD:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_FAST_FWD \n");
        break;
    case OCAP_VK_PLAY_SPEED_UP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PLAY_SPEED_UP \n");
        break;
    case OCAP_VK_PLAY_SPEED_DOWN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PLAY_SPEED_DOWN \n");
        break;
    case OCAP_VK_PLAY_SPEED_RESET:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PLAY_SPEED_RESET \n");
        break;
    case OCAP_VK_RECORD_SPEED_NEXT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RECORD_SPEED_NEXT \n");
        break;
    case OCAP_VK_GO_TO_START:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_GO_TO_START \n");
        break;
    case OCAP_VK_GO_TO_END:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_GO_TO_END \n");
        break;
    case OCAP_VK_TRACK_PREV:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_TRACK_PREV \n");
        break;
    case OCAP_VK_TRACK_NEXT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_TRACK_NEXT \n");
        break;
    case OCAP_VK_RANDOM_TOGGLE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RANDOM_TOGGLE \n");
        break;
    case OCAP_VK_CHANNEL_UP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CHANNEL_UP \n");
        break;
    case OCAP_VK_CHANNEL_DOWN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CHANNEL_DOWN \n");
        break;
    case OCAP_VK_STORE_FAVORITE_0:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_STORE_FAVORITE_0 \n");
        break;
    case OCAP_VK_STORE_FAVORITE_1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_STORE_FAVORITE_1 \n");
        break;
    case OCAP_VK_STORE_FAVORITE_2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_STORE_FAVORITE_2 \n");
        break;
    case OCAP_VK_STORE_FAVORITE_3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_STORE_FAVORITE_3 \n");
        break;
    case OCAP_VK_RECALL_FAVORITE_0:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RECALL_FAVORITE_0 \n");
        break;
    case OCAP_VK_RECALL_FAVORITE_1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RECALL_FAVORITE_1 \n");
        break;
    case OCAP_VK_RECALL_FAVORITE_2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RECALL_FAVORITE_2 \n");
        break;
    case OCAP_VK_RECALL_FAVORITE_3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RECALL_FAVORITE_3 \n");
        break;
    case OCAP_VK_CLEAR_FAVORITE_0:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CLEAR_FAVORITE_0 \n");
        break;
    case OCAP_VK_CLEAR_FAVORITE_1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CLEAR_FAVORITE_1 \n");
        break;
    case OCAP_VK_CLEAR_FAVORITE_2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CLEAR_FAVORITE_2 \n");
        break;
    case OCAP_VK_CLEAR_FAVORITE_3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CLEAR_FAVORITE_3 \n");
        break;
    case OCAP_VK_SCAN_CHANNELS_TOGGLE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_CHANNELS_TOGGLE \n");
        break;
    case OCAP_VK_PINP_TOGGLE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PINP_TOGGLE \n");
        break;
    case OCAP_VK_SPLIT_SCREEN_TOGGLE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SPLIT_SCREEN \n");
        break;
    case OCAP_VK_DISPLAY_SWAP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_DISPLAY_SWAP \n");
        break;
    case OCAP_VK_SCREEN_MODE_NEXT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SCREEN_MODE_NEXT \n");
        break;
    case OCAP_VK_VIDEO_MODE_NEXT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_VIDEO_MODE_NEXT \n");
        break;
    case OCAP_VK_VOLUME_UP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_VOLUME_UP \n");
        break;
    case OCAP_VK_VOLUME_DOWN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_VOLUME_DOWN \n");
        break;
    case OCAP_VK_MUTE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_MUTE \n");
        break;
    case OCAP_VK_SURROUND_MODE_NEXT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SURROUND_MODE_NEXT \n");
        break;
    case OCAP_VK_BALANCE_RIGHT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_BALANCE_RIGHT \n");
        break;
    case OCAP_VK_BALANCE_LEFT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_BALANCE_LEFT \n");
        break;
    case OCAP_VK_FADER_FRONT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_FADER_FRONT \n");
        break;
    case OCAP_VK_FADER_REAR:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_FADER_REAR \n");
        break;
    case OCAP_VK_BASS_BOOST_UP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_BASS_BOOST_UP \n");
        break;
    case OCAP_VK_BASS_BOOST_DOWN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_BASS_BOOST_DOWN \n");
        break;
    case OCAP_VK_INFO:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_INFO \n");
        break;
    case OCAP_VK_GUIDE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_GUIDE \n");
        break;
    case OCAP_VK_TELETEXT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_TELETEXT \n");
        break;
    case OCAP_VK_SUBTITLE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SUBTITILE \n");
        break;
    case OCAP_VK_NUMBER_SIGN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NUMBER_SIGN \n");
        break;
    case OCAP_OCRC_FIRST:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_OCRC_FIRST \n");
        break;
    case OCAP_VK_EXIT:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_EXIT \n");
        break;
    case OCAP_VK_MENU:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_MENU \n");
        break;
    case OCAP_VK_NEXT_DAY:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NEXT_DAY \n");
        break;
    case OCAP_VK_PREV_DAY:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PREV_DAY \n");
        break;
    case OCAP_VK_APPS:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_APPS \n");
        break;
    case OCAP_VK_LINK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_lINK \n");
        break;
    case OCAP_VK_LAST:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_LAST \n");
        break;
    case OCAP_VK_BACK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_BACK \n");
        break;
    case OCAP_VK_FORWARD:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_FORWARD \n");
        break;
    case OCAP_VK_ZOOM:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ZOOM \n");
        break;
    case OCAP_VK_SETTINGS:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SETTINGS \n");
        break;
    case OCAP_VK_NEXT_FAVORITE_CHANNEL:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_NEXT_FAVORITE_CHANNEL \n");
        break;
    case OCAP_VK_RESERVE_1:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RESERVE_1 \n");
        break;
    case OCAP_VK_RESERVE_2:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RESERVE_2 \n");
        break;
    case OCAP_VK_RESERVE_3:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RESERVE_3 \n");
        break;
    case OCAP_VK_RESERVE_4:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RESERVE_4 \n");
        break;
    case OCAP_VK_RESERVE_5:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RESERVE_5 \n");
        break;
    case OCAP_VK_RESERVE_6:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_RESERVE_6 \n");
        break;
    case OCAP_VK_LOCK:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_LOCK \n");
        break;
    case OCAP_VK_SKIP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_SKIP \n");
        break;
    case OCAP_VK_LIST:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_LIST \n");
        break;
    case OCAP_VK_LIVE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_LIVE \n");
        break;
    case OCAP_VK_ON_DEMAND:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_ON_DEMAND \n");
        break;
    case OCAP_VK_PINP_MOVE:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PINP_MOVE \n");
        break;
    case OCAP_VK_PINP_UP:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PINP_UP \n");
        break;
    case OCAP_VK_PINP_DOWN:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_PINP_DOWN \n");
        break;
    case OCAP_VK_INSTANT_REPLAY:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "OCAP_VK_INSTANT_REPLAY \n");
        break;
    default:
        TRACE(MPE_LOG_DEBUG, MPE_MOD_TEST, "Key not found \n");
        break;
    }
}

/**
 * Create and return the test suite for the mpe_gfx APIs.
 * @return a pointer to the new test suite.
 */
CuSuite* getTestSuite_gfxEvent(void)
{
    CuSuite* suite = CuSuiteNew();

    SUITE_STOP_TEST(suite, test_gfxWaitNextEvent);
    SUITE_STOP_TEST(suite, test_gfxWaitNextEvent_poll);

    return suite;
}
