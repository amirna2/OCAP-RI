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

package org.cablelabs.impl.manager;

import org.cablelabs.impl.manager.FocusManager.DispatchFilter;
import org.cablelabs.impl.manager.FocusManager.FocusContext;

import java.awt.AWTEvent;
import java.awt.Component;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * Tests the FocusManager implementation.
 */
public class FocusManagerTest extends ManagerTest
{
    /**
     * Tests notifyDeactivate() for an unknown context.
     */
    public void testNotifyDeactivate_newContext() throws Exception
    {
        // Request focus
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // Deactivate
        synchronized (fc)
        {
            focusMgr.notifyDeactivated(fc);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
        }
    }

    /**
     * Tests notifyDeactivate() for an activable, but not focused FocusContext.
     */
    public void testNotifyDeactivate_notOwner() throws Exception
    {
        // focus should be granted
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc2 = new FC();
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
        }

        // Deactivate the first owner
        synchronized (fc)
        {
            focusMgr.notifyDeactivated(fc);

            assertFalse("Activation should not have changed", fc.waitActivated(true, 2000L));
            assertTrue("Expected activation not to change", fc2.isActivated());
        }
    }

    /**
     * Tests requestActivate() called for a new FocusContext.
     */
    public void testRequestActivate_newContext_focus() throws Exception
    {
        // focus should be granted
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc2 = new FC();
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc3 = new FC();
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, true);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Focus withdrawal
        synchronized (fc3)
        {
            focusMgr.notifyDeactivated(fc3);

            assertFalse("Expected notifyDeactivated() to be called", fc3.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Focus withdrawal
        synchronized (fc2)
        {
            focusMgr.notifyDeactivated(fc2);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc3.isActivated());
        }
    }

    /**
     * Tests requestActivate() called for a new FocusContext.
     */
    public void testRequestActivate_newContext() throws Exception
    {
        // focus should be granted
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc2 = new FC();
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
        }

        // Focus should *not* be stolen
        // Should simply be added to end of list
        FC fc3 = new FC();
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, false);

            // There should be no change of focus
            assertTrue("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 1000L));
        }

        // Focus withdrawal
        synchronized (fc2)
        {
            focusMgr.notifyDeactivated(fc2);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc3.isActivated());
        }

        // Focus withdrawal
        synchronized (fc)
        {
            focusMgr.notifyDeactivated(fc);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc2.isActivated());
        }
    }

    /**
     * Tests requestActivate() called for an activable, but not focused
     * FocusContext.
     */
    public void testRequestActivate_notOwner_focus() throws Exception
    {
        // focus should be granted
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc2 = new FC();
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc3 = new FC();
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, true);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Have already activable focus request activation
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc3.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Focus withdrawal
        synchronized (fc2)
        {
            focusMgr.notifyDeactivated(fc2);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Focus withdrawal
        synchronized (fc3)
        {
            focusMgr.notifyDeactivated(fc3);

            assertFalse("Expected notifyDeactivated() to be called", fc3.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc2.isActivated());
        }
    }

    /**
     * Tests requestActivate() called for the currently focused FocusContext.
     */
    public void testRequestActivate_currentOwner_focus() throws Exception
    {
        // focus should be granted
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc2 = new FC();
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc3 = new FC();
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, true);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Have already focused focus request activation
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, true);

            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc2.isActivated());
            assertFalse("Should still be deactivated", fc.isActivated());
        }
    }

    /**
     * Tests requestActivate() called for the currently focused FocusContext.
     */
    public void testRequestActivate_currentOwner() throws Exception
    {
        // focus should be granted
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc2 = new FC();
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc3 = new FC();
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, true);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Have already focused focus request activation
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, false);

            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc2.isActivated());
            assertFalse("Should still be deactivated", fc.isActivated());
        }
    }

    /**
     * Tests requestActivate() called for an activable, but not focused
     * FocusContext.
     */
    public void testRequestActivate_notOwner() throws Exception
    {
        // focus should be granted
        FC fc = new FC();
        synchronized (fc)
        {
            focusMgr.requestActivate(fc, true);

            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc2 = new FC();
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, true);

            assertFalse("Expected notifyDeactivated() to be called", fc.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
        }

        // focus should be stolen
        FC fc3 = new FC();
        synchronized (fc3)
        {
            focusMgr.requestActivate(fc3, true);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc3.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Have already activable focus request, but not focus
        synchronized (fc2)
        {
            focusMgr.requestActivate(fc2, false);

            assertFalse("Did not expect notifyActivated() to be called", fc2.waitActivated(true, 5000L));
            assertTrue("Should still be activated", fc3.isActivated());
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Focus withdrawal
        synchronized (fc3)
        {
            focusMgr.notifyDeactivated(fc3);

            assertFalse("Expected notifyDeactivated() to be called", fc3.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc2.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc.isActivated());
        }

        // Focus withdrawal
        synchronized (fc2)
        {
            focusMgr.notifyDeactivated(fc2);

            assertFalse("Expected notifyDeactivated() to be called", fc2.waitActivated(false, 5000L));
            assertTrue("Expected notifyActivated() to be called", fc.waitActivated(true, 5000L));
            assertFalse("Should still be deactivated", fc3.isActivated());
        }
    }

    /**
     * Simple test implementation of <code>FocusContext</code>.
     * 
     * @author aaronk
     */
    class FC implements FocusContext
    {
        public Vector events = new Vector();

        public boolean activated = false;

        public void dispatchEvent(AWTEvent e, DispatchFilter filter, boolean interestFilter)
        {
            synchronized (events)
            {
                events.addElement(e);
                events.notify();
            }
        }

        public int getPriority()
        {
            return PRIORITY_NORMAL;
        }

        public AWTEvent getNextEvent(long ms) throws InterruptedException
        {
            AWTEvent e = null;
            synchronized (events)
            {
                if (events.size() == 0) wait(ms);
                if (events.size() > 0)
                {
                    e = (AWTEvent) events.elementAt(0);
                    events.removeElementAt(0);
                }
            }
            return e;
        }

        public synchronized void notifyActivated()
        {
            activated = true;
            notify();
        }

        public synchronized void notifyDeactivated()
        {
            activated = false;
            notify();
        }

        public synchronized boolean waitActivated(boolean waitActivated, long ms) throws InterruptedException
        {
            if (activated != waitActivated) wait(ms);
            return activated;
        }

        public boolean isActivated()
        {
            return activated;
        }

        public void clearFocus()
        {
        }

        public Component getFocusOwner()
        {
            return null;
        }
    }

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(FocusManagerTest.class);
        suite.setName(FocusManager.class.getName());
        return suite;
    }

    public FocusManagerTest(String name, ImplFactory f)
    {
        super(name, FocusManager.class, f);
    }

    protected FocusManager createFocusManager()
    {
        return (FocusManager) createManager();
    }

    private FocusManager focusMgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        focusMgr = (FocusManager) mgr;
    }

    protected void tearDown() throws Exception
    {
        focusMgr = null;
        super.tearDown();
    }
}
