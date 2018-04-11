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

import junit.framework.*;
import java.awt.event.KeyEvent;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

/**
 * Tests OverallRepository implementation
 */
public class OverallRepositoryTest extends UserEventRepositoryTest
{
    /**
     * Tests the default constructor.
     */
    public void testDefaultConstructor()
    {
        OverallRepository uer = new OverallRepository();
        assertNotNull("Expected implementation-specific name to be non-null", uer.getName());
        assertNotNull("OverallRepository should not be null", uer.getUserEvent());
        assertTrue("OverallRepository should initially be non-empty", 0 != uer.getUserEvent().length);
    }

    /**
     * Tests the OverallRepository(String) constructor.
     */
    public void testConstructor()
    {
        OverallRepository uer = new OverallRepository("xyz");
        assertEquals("Expected name given in constructor", "xyz", uer.getName());
        assertNotNull("OverallRepository should not be null", uer.getUserEvent());
        assertTrue("OverallRepository should initially be non-empty", 0 != uer.getUserEvent().length);
    }

    /**
     * Tests getUserEvent(). Expects at least a given set of events to be
     * available.
     */
    public void testGetUserEvent()
    {
        // Ensure that OverallRepository AT LEAST has the OCAP Ordinary
        // Mandatory Keycodes
        UserEvent[] events = overallRepository.getUserEvent();

        for (int i = 0; i < basicKeys.length; ++i)
        {
            int code = basicKeys[i];
            boolean found = false;
            for (int j = 0; !found && j < events.length; ++j)
            {
                if (null != events[j] && code == events[j].getCode() && KeyEvent.KEY_PRESSED == events[j].getType())
                {
                    found = true;
                    events[j] = null;
                }
            }
            assertTrue("Expected to find type=KEY_PRESSED,code=" + code + " in repository", found);
        }

        // Clear repository of all events
        clearRepository(overallRepository);

        super.testGetUserEvent();
    }

    private static void clearRepository(UserEventRepository uer)
    {
        UserEvent[] events = uer.getUserEvent();

        for (int i = 0; i < events.length; ++i)
            uer.removeUserEvent(events[i]);
    }

    /**
     * The set of keycodes that we ill expect to at least be present as
     * KEY_PRESSED events. There can be more than this.
     */
    private static final int[] basicKeys = {
            // Mandatory Orgdinary Keycodes
            KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6,
            KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9, KeyEvent.VK_ENTER,
            KeyEvent.VK_UP,
            KeyEvent.VK_DOWN,
            KeyEvent.VK_LEFT,
            KeyEvent.VK_RIGHT,
            // Removed per I09
            // HRcEvent.VK_COLORED_KEY_0, HRcEvent.VK_COLORED_KEY_1,
            // HRcEvent.VK_COLORED_KEY_2, HRcEvent.VK_COLORED_KEY_3,
            // Added per I09
            KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN,
            // Common HRcEvents
            HRcEvent.VK_CHANNEL_UP, HRcEvent.VK_CHANNEL_DOWN, HRcEvent.VK_VOLUME_UP, HRcEvent.VK_VOLUME_DOWN,
            HRcEvent.VK_MUTE, HRcEvent.VK_GUIDE,
            // Common OCRcEvents
            OCRcEvent.VK_EXIT, OCRcEvent.VK_MENU };

    protected OverallRepository overallRepository;

    protected OverallRepository createOverallRepository()
    {
        OverallRepository or = new OverallRepository(getClass().getName());

        // Add previously present keys to keyset
        UserEvent[] events = or.getUserEvent();
        for (int i = 0; i < events.length; ++i)
            keyset.addUserEvent(events[i]);

        return or;
    }

    protected UserEventRepository createUserEventRepository()
    {
        overallRepository = createOverallRepository();
        return overallRepository;
    }

    /* ===== Boilerplate ===== */
    public OverallRepositoryTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(OverallRepositoryTest.class);
        return suite;
    }
}
