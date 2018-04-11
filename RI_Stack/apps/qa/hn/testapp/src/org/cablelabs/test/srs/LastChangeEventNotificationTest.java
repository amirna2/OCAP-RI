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

package org.cablelabs.test.srs;

import java.util.Enumeration;

import org.cablelabs.test.Test;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetModuleEvent;
import org.ocap.hn.NetModuleEventListener;
import org.ocap.hn.recording.NetRecordingRequestManager;
import org.ocap.hn.recording.NetRecordingSpec;
import org.ocap.hn.recording.RecordingNetModule;

/**
 * LastChangeEventNotificationTest - This test obtains a reference to the
 * <code>NetRecordingRequestManager</code> and then registers itself as an
 * interested listener. This will allow the test to receive the
 * <code>NetModuleEvent</code> that corresponds to a <code>LastChange</code>
 * event. Then the tests makes a call to
 * {@link org.ocap.hn.recording.NetRecordingRequestManager#requestSchedule(NetRecordingSpec, NetActionHandler)}
 * which will cause
 * {@link org.cablelabs.impl.ocap.hn.upnp.srs.ScheduledRecordingService#createRecordSchedule(NetRecordingEntryLocal)}
 * to be called. The <code>createRecordSchedule</code> method will cause a
 * <code>LastChange</code> event to be generated. The test is successful if it
 * receives notification of a STATE_CHANGE <code>NetModuleEvent</code> for the
 * <code>NetRecordingRequestManager</code>.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 */
public class LastChangeEventNotificationTest extends Test
{
    private NetRecordingRequestManager netRecordingRequestManager = null;

    private MyNetActionHandler netActionHandler = null;

    private String failMessage = "";

    public int clean()
    {
        netRecordingRequestManager = null;

        netActionHandler = null;

        return Test.TEST_STATUS_PASS;
    }

    public int execute()
    {

        this.testLogger.setPrefix("LastChangeEventNotificationTest: ");
        this.testLogger.log("Executing Test: " + this.getName());

        if (executeTest())
        {
            this.testLogger.log("test PASSED!");

            return Test.TEST_STATUS_PASS;
        }
        else
        {
            this.testLogger.log("test FAILED! - reason: " + failMessage);

            return Test.TEST_STATUS_FAIL;
        }
    }

    public int prepare()
    {
        netActionHandler = new MyNetActionHandler();

        return Test.TEST_STATUS_PASS;
    }

    public String getName()
    {
        return "srs.LastChangeEventNotificationTest";
    }

    public String getDescription()
    {
        return "This is the SRS LastChangeEventNotificationTest";
    }

    private boolean executeTest()
    {
        // obtain a handle to the local SRS
        if (!getNetRecordingRequestManager())
        {
            this.testLogger.logError("Could not find local NetRecordingRequestManager or ContentServerNetModule.");
            return false;
        }

        this.testLogger.log("Creating TestNetManagerEventListener");

        TestNetModuleEventListener testNetModuleEventListener = new TestNetModuleEventListener(
                "TestNetModuleEventListener");

        this.testLogger.log("Adding " + testNetModuleEventListener.getName()
                + " to NetRecordingRequestManager as a listener for NetModule events");

        netRecordingRequestManager.addNetModuleEventListener(testNetModuleEventListener);

        NetRecordingSpec spec = new NetRecordingSpec();

        NetActionRequest netActionRequest = this.netRecordingRequestManager.requestSchedule(spec, this.netActionHandler);

        this.testLogger.log("Action Status: " + netActionRequest.getActionStatus());

        try
        {
            Thread.sleep(10000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        if (!testNetModuleEventListener.isNotifiedOfNetModuleEvent())
        {
            failMessage = testNetModuleEventListener.getName() + " did NOT recieve NetModuleEvent";
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * getNetRecordingRequestManager - this method gets the
     * <code>NetRecordingRequestManager</code>.
     * 
     * @return a boolean value indicating if the
     *         <code>NetRecordingRequestManager</code> was successfully
     *         discovered. If the method returns TRUE then the
     *         <code>NetRecordingRequestManager</code> was found, otherwise it
     *         failed.
     */
    private boolean getNetRecordingRequestManager()
    {
        NetList netList = NetManager.getInstance().getNetModuleList(null);

        /**
         * Make a second attempt to obtain a list of all the NetModules in the
         * event that none were discovered during the first attempt.
         */
        if (netList.size() <= 0)
        {
            try
            {
                Thread.sleep(30000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            netList = NetManager.getInstance().getNetModuleList(null);
        }

        if (netList.size() == 0)
        {
            this.failMessage = "NetManager returned 0 size NetModuleList";
            return false;
        }

        this.testLogger.log("NetManager returned NetModuleList size = " + String.valueOf(netList.size()));

        Enumeration enumerator = netList.getElements();

        while (enumerator.hasMoreElements())
        {
            Object netListObject = enumerator.nextElement();

            if (netListObject instanceof NetRecordingRequestManager)
            {
                netRecordingRequestManager = (NetRecordingRequestManager) netListObject;

                if (netRecordingRequestManager.isLocal() == false)
                {
                    netRecordingRequestManager = null;
                    this.failMessage = "found a non-local netRecordingRequestManager";
                    return false;
                }

                this.testLogger.log("found a NetRecordingRequestManager: isLocal - "
                        + String.valueOf(((NetRecordingRequestManager) netListObject).isLocal()) + " Device - "
                        + ((NetRecordingRequestManager) netListObject).getDevice().getName());
            }
            else if (netListObject instanceof RecordingNetModule)
            {
                this.testLogger.log("found a RecordingNetModule: isLocal - "
                        + String.valueOf(((RecordingNetModule) netListObject).isLocal()) + " Device - "
                        + ((RecordingNetModule) netListObject).getDevice().getName());
            }
            else if (netListObject instanceof NetModule)
            {
                this.testLogger.log("found a NetModule: isLocal - "
                        + String.valueOf(((NetModule) netListObject).isLocal()) + " Device - "
                        + ((NetModule) netListObject).getDevice().getName());
            }
            else
            {
                this.failMessage = "found unknown type, isLocal = "
                        + String.valueOf(((NetModule) netListObject).isLocal());
                return false;
            }
        }

        if (netRecordingRequestManager == null)
        {
            this.failMessage = "could not find NetRecordingRequestManager";

            return false;
        }

        return true;
    }

    /**
     * TestNetManagerEventListener - inner class that is a
     * <code>NetModuleEventListener</code>. This class enables the test to
     * create multiple listener instances.
     * 
     * @author Ben Foran (Flashlight Engineering and Consulting)
     */
    class TestNetModuleEventListener implements NetModuleEventListener
    {
        private String listenerName;

        private boolean isNotifiedOfNetModuleEvent;

        TestNetModuleEventListener(String listenerName)
        {
            this.listenerName = listenerName;

            isNotifiedOfNetModuleEvent = false;

            testLogger.log("Creating TestNetManagerEventListener: " + listenerName);
        }

        /**
         * notify - <code>NetModuleEventListener</code> callback.
         */
        public void notify(NetModuleEvent event)
        {
            testLogger.log("TestNetManagerEventListener.notify() for NetModuleEvent called on " + listenerName);
            testLogger.log("NetModuleEvent - " + event.getType());

            isNotifiedOfNetModuleEvent = true;
        }

        /**
         * getName - returns the name assigned to this listener.
         * 
         * @return name of the listener.
         */
        String getName()
        {
            return listenerName;
        }

        /**
         * isNotifiedOfNetModuleEvent - indicates if the listener has been
         * notified of a <code>NetModuleEvent</code>.
         * 
         * return - true is this <code>NetModuleEventListener</code> has been
         * notified of a <code>NetModuleEvent</code>, otherwise false.
         */
        boolean isNotifiedOfNetModuleEvent()
        {
            return isNotifiedOfNetModuleEvent;
        }

        /**
         * resetNotifications - reset <code>DeviceEvent</code> and
         * <code>NetModuleEvent</code> notification flags.
         */
        void resetNotifications()
        {
            isNotifiedOfNetModuleEvent = false;
        }
    }

    /**
     * MyNetActionHandler - inner class to manage asynchronous calls that
     * require a <code>NetActionHandler</code>.
     * 
     * @author Dan Woodard (Flashlight Engineering and Consulting)
     */
    private class MyNetActionHandler implements NetActionHandler
    {
        // responses from received NetActionEvent
        private NetActionEvent netActionEvent = null;

        // signals and flags for received NetActionHandler NetActionEvent
        private boolean receivedNetActionEvent = false;

        // use for signaling completion of async calls
        private Object signal = new Object();

        // timeout to wait for NetActionHandler NetActionEvents
        private static final long TIMEOUT = 5000;// 5 seconds

        private String failReason = "";

        /**
         * notify - <code>NetActionHandler</code> callback.
         */
        public void notify(NetActionEvent netActionEvent)
        {
            testLogger.log("MyNetActionHandler: got notify to MyNetActionHandler");

            this.netActionEvent = netActionEvent;

            this.receivedNetActionEvent = true;

            try
            {
                synchronized (this.signal)
                {
                    this.signal.notifyAll();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        /**
         * waitRequestResponse - waits for async response from a
         * NetActionRequest method. If successful, the ActionEvent will be in
         * this.netActionEvent.
         * 
         * @return true if the response was received, otherwise return false.
         */
        public boolean waitRequestResponse()
        {
            testLogger.log("MyNetActionHandler: wait for notify to MyNetActionHandler");

            if (!this.receivedNetActionEvent)
            {
                try
                {
                    synchronized (this.signal)
                    {
                        this.signal.wait(TIMEOUT);
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

            if (!this.receivedNetActionEvent)
            {
                failReason = "MyNetActionHandler: Failed to get NetActionEvent within " + MyNetActionHandler.TIMEOUT
                        + " milliseconds";
                return false;
            }

            // reset for next request
            this.receivedNetActionEvent = false;

            testLogger.log("MyNetActionHandler: got NetActionEvent to MyNetActionHandler");

            return true;
        }

        /**
         * getNetActionEvent - gets the <code>NetActionEvent</code> received by
         * this <code>NetActionHandler</code>.
         * 
         * @return a <code>NetActionEvent</code>, this object will be null if it
         *         was not received, otherwise non-null if it is a valid event.
         */
        public NetActionEvent getNetActionEvent()
        {
            return this.netActionEvent;
        }

        /**
         * getFailReason - returns the reason for the failure.
         * 
         * @return the reason for the failure.
         */
        public String getFailReason()
        {
            return this.failReason;
        }
    }
}
