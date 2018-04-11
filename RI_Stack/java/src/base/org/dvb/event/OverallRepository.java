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

package org.dvb.event;

import java.awt.event.KeyEvent;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

/**
 * This class defines a repository which initially contains all the user events
 * which can be delivered to an application. This includes all keycodes for
 * which KEY_PRESSED and KEY_RELEASED events can be generated and all keychars
 * for which KEY_TYPED events can be generated. Note that the set of keycodes
 * and keychars which can be generated is dependent on the input devices of the
 * MHP terminal. For example, this pre-defined repository could be used by an
 * application, which requires a pin code from the user, in order to prevent
 * another applications from receiving events.
 * 
 * @see UserEvent
 * @see org.havi.ui.event.HKeyCapabilities
 */
public class OverallRepository extends UserEventRepository
{
    /**
     * The constructor for the repository. The name of the constructed instance
     * (as returned by getName()) is implementation dependent.
     */
    public OverallRepository()
    {
        this("OverallRepository");
    }

    /**
     * The constructor for the repository with a name.
     * 
     * @param name
     *            the name to use for the repository
     */
    public OverallRepository(String name)
    {
        super(name);

        // Add appropriate events
        if (availableKeys != null) for (int i = 0; i < availableKeys.length; ++i)
            addKey(availableKeys[i]);
        if (availableChars != null) for (int i = 0; i < availableChars.length; ++i)
            addKeyChar(availableChars[i]);
    }

    /**
     * Adds the specified keyChar to the repository. KeyChars are added such
     * that when retrieved using the <code>getUserEvent</code> method a
     * <code>UserEvent</code> with a <code>type</code> of <code>KEY_TYPED</code>
     * and a <code>code</code> of <code>VK_UNDEFINED</code> will be returned
     * 
     * @param keychar
     *            character representation for event to be added
     */
    private void addKeyChar(char keychar)
    {
        addUserEvent(new UserEvent("", UserEvent.UEF_KEY_EVENT, keychar, 0L));
    }

    /**
     * Available KEY_PRESSED and KEY_RELEASED virtual keycodes.
     */
    private static final int availableKeys[];

    /**
     * Available KEY_TYPED character representations.
     */
    private static final char availableChars[];

    static
    {
        // AvailableKeys/AvailableChars should be initialized here...
        // ...preferably using a native method or some sort of toolkit method.

        // For now we'll just kludge it... with mandatory ordinary key codes
        // plus watchtv- and vod-oriented keys
        availableKeys = new int[] { KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4,
                KeyEvent.VK_5, KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_UP,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT,
                KeyEvent.VK_ENTER,
                KeyEvent.VK_PAGE_UP,
                KeyEvent.VK_PAGE_DOWN,

                // Other HRcEvents
                HRcEvent.VK_POWER, HRcEvent.VK_REWIND, HRcEvent.VK_STOP, HRcEvent.VK_PLAY, HRcEvent.VK_RECORD,
                HRcEvent.VK_FAST_FWD, HRcEvent.VK_CHANNEL_UP, HRcEvent.VK_CHANNEL_DOWN, HRcEvent.VK_RECALL_FAVORITE_0,
                HRcEvent.VK_VOLUME_UP, HRcEvent.VK_VOLUME_DOWN, HRcEvent.VK_MUTE, HRcEvent.VK_INFO, HRcEvent.VK_GUIDE,
                HRcEvent.VK_COLORED_KEY_0, HRcEvent.VK_COLORED_KEY_1, HRcEvent.VK_COLORED_KEY_2,
                HRcEvent.VK_COLORED_KEY_3,

                // Other OCRcEvents
                OCRcEvent.VK_RF_BYPASS, OCRcEvent.VK_EXIT, OCRcEvent.VK_MENU, OCRcEvent.VK_LAST, OCRcEvent.VK_PREV_DAY,
                OCRcEvent.VK_NEXT_DAY, OCRcEvent.VK_SETTINGS, };
        availableChars = new char[] { '\n', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', };
    }
}
