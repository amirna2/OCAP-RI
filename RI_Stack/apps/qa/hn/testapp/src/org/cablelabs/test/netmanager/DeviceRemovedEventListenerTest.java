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

package org.cablelabs.test.netmanager;

import org.cablelabs.test.Test;
import org.ocap.hn.DeviceEvent;
import org.ocap.hn.DeviceEventListener;
import org.ocap.hn.NetManager;

/**
 * DeviceRemovedEventListenerTest - This test creates a
 * <code>TestDeviceEventListener</code> object which is a
 * <code>DeviceEventListener</code>. This listener is passed into the
 * {@link org.ocap.hn.NetManager#addDeviceEventListener(DeviceEventListener)}
 * method. Then the test waits to receive a DEVICE_REMOVED event. The test
 * passes if the listener is notified of this event otherwise it fails. The test
 * will also fail if it is unable to obtain a handle to the
 * <code>NetManager</code>. The test will time out after two minutes if it does
 * not receive a DEVICE_REMOVED event.
 * 
 * Note: In order to run this test, another UPnP device will need to be started
 * up on the network before the test begins executing (i.e. launch the simulator
 * from a second machine). Once the test has started executing, the recently
 * started device will need to be shut down. A log message will appear in the
 * trace window indicating that the new device should be removed from the
 * network.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 */
public class DeviceRemovedEventListenerTest extends Test
{
    private String failMessage = "";

    public int clean()
    {
        return Test.TEST_STATUS_PASS;
    }

    public int execute()
    {

        this.testLogger.setPrefix("DeviceRemovedEventListenerTest: ");
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
        return Test.TEST_STATUS_PASS;
    }

    public String getName()
    {
        return "netmanager.DeviceRemovedEventListenerTest";
    }

    public String getDescription()
    {
        return "This is the NetManager DeviceRemovedEventListenerTest";
    }

    private boolean executeTest()
    {
        this.testLogger.log("Obtaining an instance of the NetManager");
        NetManager netManagerInstance = NetManager.getInstance();

        if (netManagerInstance == null)
        {
            failMessage = "unable to get an instance of NetManager";
            return false;
        }

        this.testLogger.log("Creating TestDeviceEventListener");
        TestDeviceEventListener deviceEventListener = new TestDeviceEventListener();

        int initialNumberOfDevices = netManagerInstance.getDeviceList(null).size();

        int retryCount = 8;

        netManagerInstance.addDeviceEventListener(deviceEventListener);

        while (retryCount > 0)
        {
            this.testLogger.log("Time to remove test device from network ...");

            try
            {
                Thread.sleep(15000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            // Indicates that the number of Devices on the network has
            // changed which means that Device events have been generated.
            if (netManagerInstance.getDeviceList(null).size() != initialNumberOfDevices)
            {
                break;
            }

            retryCount--;
        }

        if (retryCount <= 0)
        {
            failMessage = "device was never removed from the network";
            return false;
        }
        else if (deviceEventListener.isNotified())
        {
            return true;
        }
        else
        {
            failMessage = "Listener did not recieve DEVICE_REMOVED event";
            return false;
        }
    }

    /**
     * TestDeviceEventListener - inner class that is a DeviceEventListener. This
     * class enables the test to create multiple DeviceEventListeners.
     */
    class TestDeviceEventListener implements DeviceEventListener
    {
        private boolean isNotified;

        // default constructor
        public TestDeviceEventListener()
        {
            testLogger.log("Creating TestDeviceEventListener: " + this.toString());
            isNotified = false;
        }

        public void notify(DeviceEvent event)
        {
            testLogger.log("DeviceEventListener.notify() called on " + this.toString());
            testLogger.log("DeviceEvent - " + event.getType());

            if (event.getType() == DeviceEvent.DEVICE_REMOVED)
            {
                isNotified = true;
            }
        }

        /**
         * isNotified - indicates if the listener has been notified.
         */
        public boolean isNotified()
        {
            return isNotified;
        }

    }
}
