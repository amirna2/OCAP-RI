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
package org.cablelabs.xlet.SoundContention;

import java.awt.event.KeyEvent;

import org.ocap.ui.event.OCRcEvent;

/**
 * Key code helper functions.
 */
public class KeyCodeHelper
{

    /**
     * Used to convert a key code to a human readable string.
     * 
     * @param keyCode
     * @return key name.
     */
    public static String getKeyName(int keyCode)
    {
        // TODO Auto-generated method stub
        switch (keyCode)
        {
            case KeyEvent.VK_0:
                return "0";
            case KeyEvent.VK_1:
                return "1";
            case KeyEvent.VK_2:
                return "2";
            case KeyEvent.VK_3:
                return "3";
            case KeyEvent.VK_4:
                return "4";
            case KeyEvent.VK_5:
                return "5";
            case KeyEvent.VK_6:
                return "6";
            case KeyEvent.VK_7:
                return "7";
            case KeyEvent.VK_8:
                return "8";
            case KeyEvent.VK_9:
                return "9";
            case KeyEvent.VK_LEFT:
                return "Left Arrow";
            case KeyEvent.VK_RIGHT:
                return "Right Arrow";
            case KeyEvent.VK_UP:
                return "Up";
            case KeyEvent.VK_DOWN:
                return "Down";
            case KeyEvent.VK_ENTER:
                return "Enter";
            case OCRcEvent.VK_CHANNEL_DOWN:
                return "Channel Down";
            case OCRcEvent.VK_CHANNEL_UP:
                return "Channel Up";
            case OCRcEvent.VK_VOLUME_DOWN:
                return "Volume Down";
            case OCRcEvent.VK_VOLUME_UP:
                return "Volume Up";
            case OCRcEvent.VK_GUIDE:
                return "Guide";
            case OCRcEvent.VK_MENU:
                return "Menu";
            case OCRcEvent.VK_MUTE:
                return "Mute";
            case OCRcEvent.VK_INFO:
                return "Info";
            case OCRcEvent.VK_POWER:
                return "Power";
            case OCRcEvent.VK_PAGE_DOWN:
                return "Page Down";
            case OCRcEvent.VK_PAGE_UP:
                return "Page Up";
            case OCRcEvent.VK_FADER_FRONT:
                return "Fader Front";
            case OCRcEvent.VK_FADER_REAR:
                return "Fader Rear";
            case OCRcEvent.VK_APPS:
                return "Apps";
            case OCRcEvent.VK_ACCEPT:
                return "Accept";
            case OCRcEvent.VK_BACK:
                return "back";
            case OCRcEvent.VK_FORWARD:
                return "Forward";
            case OCRcEvent.VK_PAUSE:
                return "Pause";
            case OCRcEvent.VK_STOP:
                return "Stop";
            case OCRcEvent.VK_RECORD:
                return "Record";
            case OCRcEvent.VK_FAST_FWD:
                return "Fast Forward";
            case OCRcEvent.VK_REWIND:
                return "Rewind";
            case OCRcEvent.VK_PLAY:
                return "Play";
            case OCRcEvent.VK_LIST:
                return "List";
            case OCRcEvent.VK_LIVE:
                return "Live";
            case OCRcEvent.VK_LAST:
                return "Last";
            case OCRcEvent.VK_SETTINGS:
                return "Settings";
            case OCRcEvent.VK_CLEAR:
                return "Clear";
            case OCRcEvent.VK_CLEAR_FAVORITE_0:
                return "Clear Favorite 0";
            case OCRcEvent.VK_CLEAR_FAVORITE_1:
                return "Clear Favorite 1";
            case OCRcEvent.VK_CLEAR_FAVORITE_2:
                return "Clear Favorite 2";
            case OCRcEvent.VK_CLEAR_FAVORITE_3:
                return "Clear Favorite 3";
            case OCRcEvent.VK_COLORED_KEY_0:
                return "Colored Key 0";
            case OCRcEvent.VK_COLORED_KEY_1:
                return "Colored Key 1";
            case OCRcEvent.VK_COLORED_KEY_2:
                return "Colored Key 2";
            case OCRcEvent.VK_COLORED_KEY_3:
                return "Colored Key 3";
            case OCRcEvent.VK_COLORED_KEY_4:
                return "Colored Key 4";
            case OCRcEvent.VK_COLORED_KEY_5:
                return "Colored Key 5";
            case OCRcEvent.VK_EXIT:
                return "Exit";
            case OCRcEvent.VK_NEXT_FAVORITE_CHANNEL:
                return "Next Favorite Channel";
            case OCRcEvent.VK_STORE_FAVORITE_0:
                return "Store Favorite 0";
            case OCRcEvent.VK_STORE_FAVORITE_1:
                return "Store Favorite 1";
            case OCRcEvent.VK_STORE_FAVORITE_2:
                return "Store Favorite 2";
            case OCRcEvent.VK_STORE_FAVORITE_3:
                return "Store Favorite 3";
            case OCRcEvent.VK_VIDEO_MODE_NEXT:
                return "Video Mode Next";
            case OCRcEvent.VK_WINK:
                return "Wink";
            case 433:
                return "FAV";
            default:
                return "" + keyCode;
        }
    }

}
