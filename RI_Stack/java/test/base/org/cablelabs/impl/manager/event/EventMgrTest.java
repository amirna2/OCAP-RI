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

/*
 * Created on Nov 8, 2005
 */
package org.cablelabs.impl.manager.event;

import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.focus.FocusManagerImpl;
import org.cablelabs.impl.manager.FocusManagerTest;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerTest;
import org.cablelabs.test.TestUtils;

import java.awt.Component;
import java.awt.event.KeyEvent;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.event.UserEventTest;
import org.dvb.event.EventManagerTest.Focus;

/**
 * Tests the EventMgr implementation of FocusManager.
 * 
 * @author Aaron Kamienski
 */
public class EventMgrTest extends TestCase
{
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(EventMgr.class);
    }

    /**
     * Tests that KeyEvent.CHAR_UNDEFINED is translated as necessary by
     * dispatch.
     */
    public void testdispatch_CHAR_UNDEFINED_translation() throws Exception
    {
        boolean xlate = UserEventTest.CHAR_UNDEFINED != KeyEvent.CHAR_UNDEFINED;
        if (!xlate) return; // Nothing to test here

        EventMgr em = (EventMgr) EventMgr.getInstance();
        try
        {
            FocusManager fm = (FocusManager) FocusManagerImpl.getInstance();
            Focus focus = new Focus();

            // Request that app be given focus
            // And verify that it can
            fm.requestActivate(focus, true);
            try
            {
                int vk = KeyEvent.VK_Z;

                assertTrue("Internal error - could not activate FocusContext", focus.waitForActivated(3000));

                Component COMPONENT = new Component()
                { /* empty */
                };
                KeyEvent key = new KeyEvent(COMPONENT, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, vk,
                        KeyEvent.CHAR_UNDEFINED);

                // Verify that it is notified of event
                em.dispatch(key);
                focus.syncForEvents(1, 2000L);
                assertEquals("Expected listener to be called", 1, focus.events.size());

                KeyEvent ke = (KeyEvent) focus.events.elementAt(0);
                if (ke != key)
                {
                    // Test for equivalence if not same
                    assertEquals("Expected specified key ID to be dispatched", key.getID(), ke.getID());
                    assertEquals("Expected specified key event to be dispatched", key.getKeyCode(), ke.getKeyCode());
                }
                // Verify that KeyEvent.CHAR_UNDEFINED was translated
                assertEquals("Expected CHAR_UNDEFINED to be translated", UserEventTest.CHAR_UNDEFINED, ke.getKeyChar());
            }
            finally
            {
                fm.notifyDeactivated(focus);
            }
        }
        finally
        {
            em.destroy();
        }
    }

    public EventMgrTest(String name)
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(EventMgrTest.class);

        ImplFactory factory = new ManagerTest.ManagerFactory()
        {
            public Object createImplObject()
            {
                return EventMgr.getInstance();
            }

            public void destroyImplObject(Object obj)
            {
                ((Manager) obj).destroy();
            }
        };
        InterfaceTestSuite fmgr = FocusManagerTest.isuite();
        fmgr.addFactory(factory);
        suite.addTest(fmgr);

        return suite;
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

}
