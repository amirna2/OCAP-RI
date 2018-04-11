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


package org.ocap.system.event;

import junit.framework.TestCase;

public class SysEventMgrImplTest extends TestCase
{
    SysEventMgrImpl sysEventMgrImpl;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(SysEventMgrImplTest.class);
        System.exit(0);
    }
    
    protected void setUp() throws Exception
    {
        super.setUp();
        sysEventMgrImpl = new SysEventMgrImpl();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testSetEventListenerUnknownType()
    {
        try
        {
            SystemEventListener listener = new CannedSystemEventListener();
            sysEventMgrImpl.setEventListener(-1, listener);
            fail("Setting listener with a bad type code should cause an " + 
                    "exception");
            
        }
        catch(Exception exc)
        {
            // expected outcome
        }
    }
    
    public void testUnsetEventListenerNotSet()
    {
        sysEventMgrImpl.unsetEventListener(SystemEventManager.ERROR_EVENT_LISTENER);
        // just doing a sanity check that this doesn't cause an exception
    }
    
    public void testUnsetEventListenerUnknownType()
    {
        sysEventMgrImpl.unsetEventListener(-1);
        // just doing a sanity check that this doesn't cause an exception
    }
    
    public void testNotifyEvent()
    {
        CannedSystemEventListener listener = new CannedSystemEventListener();
        sysEventMgrImpl.setEventListener(SystemEventManager.REBOOT_EVENT_LISTENER,
                listener);
        //
        // first, verify that we are getting events
        //
        SystemEvent event = new RebootEvent(RebootEvent.REBOOT_BY_IMPLEMENTATION,
                "message");
        sysEventMgrImpl.log(event);
        listener.waitForEvents(1);
        assertTrue("Listener did not receive event", listener.events.size() == 1);
        assertTrue(event.equals(listener.getEvent(0)));
    }
    
    public void testNotifyEventWithTimeout()
    {
        CannedSystemEventListener listener = new CannedSystemEventListener();
        sysEventMgrImpl.setEventListener(SystemEventManager.REBOOT_EVENT_LISTENER,
                listener);
        //
        // first, verify that we are getting events
        //
        SystemEvent event = new RebootEvent(RebootEvent.REBOOT_BY_IMPLEMENTATION,
                "message");
        sysEventMgrImpl.log(event, 1000l);
        listener.waitForEvents(1);
        assertTrue("Listener did not receive event", listener.events.size() == 1);
        assertTrue(event.equals(listener.getEvent(0)));
    }
    
    public void testDontReceiveEventNotRegisteredFor()
    {
        CannedSystemEventListener listener = new CannedSystemEventListener();
        sysEventMgrImpl.setEventListener(SystemEventManager.REBOOT_EVENT_LISTENER,
                listener);
        //
        // first, verify that we are getting events
        //
        SystemEvent event = 
            new ErrorEvent(ErrorEvent.APP_CAT_GENERAL_ERROR, "message");
        sysEventMgrImpl.log(event);
        listener.waitForEvents(1);
        assertTrue("Listener received an event in error", 
                   listener.events.size() == 0);
    }
    
    public void testUnsetEventListener()
    {
        CannedSystemEventListener listener = new CannedSystemEventListener();
        sysEventMgrImpl.setEventListener(SystemEventManager.REBOOT_EVENT_LISTENER,
                listener);
        //
        // first, verify that we are getting events
        //
        SystemEvent event = new RebootEvent(RebootEvent.REBOOT_BY_IMPLEMENTATION,
                "message");
        sysEventMgrImpl.log(event);
        listener.waitForEvents(1);
        assertTrue("Listener did not receive event", listener.events.size() == 1);
        
        //
        // next, unregister the listener and verify that no more events are
        // received
        //
        listener.reset();
        sysEventMgrImpl.unsetEventListener(SystemEventManager.REBOOT_EVENT_LISTENER);
        sysEventMgrImpl.log(event);
        listener.waitForEvents(1);
        assertTrue("Listener received an event after being unset", 
                listener.events.size() == 0);
     }
    
    //
    // set a listener, then set a second listener for the same event type,
    // verify that the first listener doesn't receive any event
    //
    public void testSetSecondListener()
    {
        CannedSystemEventListener listener1 = new CannedSystemEventListener();
        CannedSystemEventListener listener2 = new CannedSystemEventListener();
        sysEventMgrImpl.setEventListener(SystemEventManager.REBOOT_EVENT_LISTENER,
                listener2);
        //
        // first, verify that we are getting events
        //
        SystemEvent event = new RebootEvent(RebootEvent.REBOOT_BY_IMPLEMENTATION,
                "message");
        sysEventMgrImpl.log(event);
        listener1.waitForEvents(1);
        listener2.waitForEvents(1);
        assertTrue("incorrect listener received event", 
                listener1.events.size() == 0);
        assertTrue("correct listener did not receive event", 
                listener2.events.size() == 1);
     }

    public void testSetListenerMultipleTypes()
    {
        CannedSystemEventListener listener = new CannedSystemEventListener();
        sysEventMgrImpl.setEventListener(SystemEventManager.REBOOT_EVENT_LISTENER,
                listener);
        sysEventMgrImpl.setEventListener(SystemEventManager.ERROR_EVENT_LISTENER,
                listener);
        
        SystemEvent event1 = new RebootEvent(RebootEvent.REBOOT_BY_IMPLEMENTATION,
                "message");
        SystemEvent event2 = 
            new ErrorEvent(ErrorEvent.APP_CAT_GENERAL_ERROR, "message");
        sysEventMgrImpl.log(event1);
        sysEventMgrImpl.log(event2);
        
        listener.waitForEvents(2);
        assertTrue("Listener did not receive both events",
                listener.events.size() == 2);
       
    }
    
    public void testNotifyEventApplicationDefined()
    {
        SystemEvent event = new MySystemEvent(1);
        try
        {
            sysEventMgrImpl.log(event);
            fail("Application allowed a user-defined system event");
        }
        catch(IllegalArgumentException exc)
        {
            //expected outcome
        }
    }
    private static class MySystemEvent extends SystemEvent
    {
        public MySystemEvent(int i)
        {
            super(i);
        }
    }
}
