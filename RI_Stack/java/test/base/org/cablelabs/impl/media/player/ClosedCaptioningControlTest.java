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
package org.cablelabs.impl.media.player;

import javax.media.Player;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.ocap.media.ClosedCaptioningControl;
import org.ocap.media.ClosedCaptioningEvent;
import org.ocap.media.ClosedCaptioningListener;

import org.cablelabs.impl.media.JMFBaseInterfaceTest;

/**
 * ClosedCaptioningControlTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class ClosedCaptioningControlTest extends JMFBaseInterfaceTest
{

    private ClosedCaptioningControl control;

    private CannedClosedCaptionListener listener;

    private Player player;

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(ClosedCaptioningControlTest.class, factory);
    }

    public ClosedCaptioningControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public ClosedCaptioningControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        player = (Player) createImplObject();
        control = (ClosedCaptioningControl) player.getControl("org.ocap.media.ClosedCaptioningControl");
        listener = new CannedClosedCaptionListener();
        control.addClosedCaptioningListener(listener);
    }

    public void tearDown() throws Exception
    {
        player.close();
        control = null;
        player = null;
        listener = null;

        super.tearDown();
    }

    // Test section
    public void testSetStateCCTurnOn()
    {
        setStateValidateEvent(ClosedCaptioningControl.CC_TURN_ON, ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_ON);
    }

    public void testSetStateCCTurnOff()
    {
        //
        // first turn the control on
        //
        setStateValidateEvent(ClosedCaptioningControl.CC_TURN_ON, ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_ON);
        setStateValidateEvent(ClosedCaptioningControl.CC_TURN_OFF, ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_OFF);
    }

    public void testSetStateCCTurnOnMute()
    {
        setStateValidateEvent(ClosedCaptioningControl.CC_TURN_ON_MUTE,
                ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_ON_MUTE);
    }

    private void setStateValidateEvent(int state, int eventId)
    {
        listener.reset();
        control.setClosedCaptioningState(state);
        listener.waitForEvents(1);
        assertTrue(listener.events.size() == 1);

        ClosedCaptioningEvent evt = (ClosedCaptioningEvent) listener.events.get(0);
        assertTrue(evt.getSource().equals(control));
        assertTrue(evt.getEventID() == eventId);
        assertTrue(control.getClosedCaptioningState() == state);
    }

    public void testSettingToCurrentStateDoesNotGenerateEvent()
    {

        setStateValidateEvent(ClosedCaptioningControl.CC_TURN_ON, ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_ON);
        control.setClosedCaptioningState(ClosedCaptioningControl.CC_TURN_ON);
        listener.reset();
        listener.waitForEvents(1);
        assertTrue("Calling setState with current state generated an event", listener.events.size() == 0);
    }

    public void testSetGetClosedCaptioningServiceNumber()
    {
        int supported[] = control.getSupportedClosedCaptioningServiceNumber();
        assertTrue(supported.length >= 2);
        control.setClosedCaptioningServiceNumber(supported[0], supported[1]);
        listener.waitForEvents(1);
        assertTrue(listener.events.size() == 1);
        ClosedCaptioningEvent evt = (ClosedCaptioningEvent) listener.events.get(0);
        assertTrue(evt.getEventID() == ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_SELECT_NEW_SERVICE);
        int[] serviceNumbers = control.getClosedCaptioningServiceNumber();
        assertTrue(serviceNumbers[0] == supported[0]);
        assertTrue(serviceNumbers[1] == supported[1]);
    }

    public void testSetClosedCaptioningServiceNumberInvalidAnalog()
    {
        int supported[] = control.getSupportedClosedCaptioningServiceNumber();
        assertTrue(supported.length >= 1);
        try
        {
            control.setClosedCaptioningServiceNumber(Integer.MIN_VALUE, supported[0]);
            fail("No exception on invalid argument");
        }
        catch (IllegalArgumentException exc)
        {
            // expected case
        }
    }

    public void testSetClosedCaptioningServiceNumberInvalidDigital()
    {
        int supported[] = control.getSupportedClosedCaptioningServiceNumber();
        assertTrue(supported.length >= 1);
        try
        {
            control.setClosedCaptioningServiceNumber(supported[0], Integer.MIN_VALUE);
            fail("No exception on invalid argument");
        }
        catch (IllegalArgumentException exc)
        {
            // expected case
        }
    }

    public void testRemoveListener()
    {
        listener.reset();
        control.removeClosedCaptioningListener(listener);
        control.setClosedCaptioningState(ClosedCaptioningControl.CC_TURN_ON);
        listener.waitForEvents(1);
        assertTrue(listener.events.size() == 0);

        assertTrue(control.getClosedCaptioningState() == ClosedCaptioningControl.CC_TURN_ON);
    }

    public void testRemoveListenerMultipleTimes()
    {
        control.removeClosedCaptioningListener(listener);
        control.removeClosedCaptioningListener(listener);
        //
        // no assertion, just make sure an exception isn't thrown
        // 
    }

    private static class CannedClosedCaptionListener extends CannedControllerListener implements
            ClosedCaptioningListener
    {
        public CannedClosedCaptionListener()
        {
            super(1);
        }

        public void ccStatusChanged(ClosedCaptioningEvent event)
        {
            synchronized (events)
            {
                events.add(event);
                events.notifyAll();
            }
        }
    }

}
