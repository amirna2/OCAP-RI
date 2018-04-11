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

package org.ocap.hardware.device;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.ocap.hardware.Host;
import org.ocap.hardware.VideoOutputPort;

import org.cablelabs.impl.manager.host.DeviceSettingsHostManagerImpl;
import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsHostImpl;
import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsVideoOutputPortImpl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class VideoOutputPortTest extends TestCase
{
	boolean configurationCBCalled = false;
    boolean connectionStatusCBCalled = false;
    boolean enabledStatusCBCalled = false;
    Thread testCaseThread = null;
    
    public VideoOutputPortTest(String name)
    {
        super(name);
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
        TestSuite suite = new TestSuite();
        suite.addTest(new VideoOutputPortTest("testGetVideoPorts"));
        suite.addTest(new VideoOutputPortTest("testDisableAllPorts"));
        suite.addTest(new VideoOutputPortTest("testEnableAllPorts"));
        suite.addTest(new VideoOutputPortTest("testEnableOnePort"));
        suite.addTest(new VideoOutputPortTest("testVOPListenerInterface"));
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /*
     * Start of tests
     */
    public void testGetVideoPorts()
    {
        Enumeration e = getVideoPorts();

        while (e.hasMoreElements())
        {
            VideoOutputPort videoOutputPort = (VideoOutputPort) e.nextElement();
            assertNotNull(videoOutputPort);
        }
    }

    public void testDisableAllPorts()
    {
        disableAllPorts();
        Enumeration e = getVideoPorts();
        while (e.hasMoreElements())
        {
            VideoOutputPort videoOutputPort = (VideoOutputPort) e.nextElement();
            assertFalse("Port should be disabled", videoOutputPort.status());
        }
    }

    public void testEnableAllPorts()
    {
        enableAllPorts();
        Enumeration e = getVideoPorts();
        while (e.hasMoreElements())
        {
            VideoOutputPort videoOutputPort = (VideoOutputPort) e.nextElement();
            assertTrue("Port should be enabled", videoOutputPort.status());
        }
    }

    public void testEnableOnePort()
    {
        disableAllPorts();
        Enumeration e = getVideoPorts();
        Set sdPorts = new HashSet();
        Set hdPorts = new HashSet();
        VideoOutputPort aVideoPort;
        
        //separate sd and hd ports...
        while(e.hasMoreElements())
        {
        	aVideoPort = (VideoOutputPort)e.nextElement();
        	switch(aVideoPort.getType())
        	{
    			case VideoOutputPort.AV_OUTPUT_PORT_TYPE_RF:
        		case VideoOutputPort.AV_OUTPUT_PORT_TYPE_BB:
        		case VideoOutputPort.AV_OUTPUT_PORT_TYPE_SVIDEO:
        			sdPorts.add(aVideoPort);
        			break;
        		case VideoOutputPort.AV_OUTPUT_PORT_TYPE_COMPONENT_VIDEO:
        		case VideoOutputPort.AV_OUTPUT_PORT_TYPE_DVI:
        		case VideoOutputPort.AV_OUTPUT_PORT_TYPE_HDMI:
        		case VideoOutputPort.AV_OUTPUT_PORT_TYPE_INTERNAL:
    			case VideoOutputPort.AV_OUTPUT_PORT_TYPE_1394:
    				hdPorts.add(aVideoPort);
    				break;
        	}
        }
        // Enable an sd port
        aVideoPort = (VideoOutputPort)sdPorts.iterator().next();
        aVideoPort.enable();
        //check the sd ports, and the hd ports...
        
        Iterator i = sdPorts.iterator();
        while(i.hasNext())
        {
        	VideoOutputPort anotherVideoPort = (VideoOutputPort)i.next();
        	assertTrue("All SD ports are enabled together.", anotherVideoPort.status());
        }
        
        i =  hdPorts.iterator();
        while(i.hasNext())
        {
        	VideoOutputPort anotherVideoPort = (VideoOutputPort)i.next();
        	assertFalse("HD ports should not be affected.", anotherVideoPort.status());
        }

        aVideoPort = (VideoOutputPort)sdPorts.iterator().next();
        aVideoPort.disable();
        
        i = sdPorts.iterator();
        while(i.hasNext())
        {
        	VideoOutputPort anotherVideoPort = (VideoOutputPort)i.next();
        	assertFalse("All SD ports are disabled together.", anotherVideoPort.status());
        }
        
        i =  hdPorts.iterator();
        while(i.hasNext())
        {
        	VideoOutputPort anotherVideoPort = (VideoOutputPort)i.next();
        	assertFalse("HD ports should not be affected.", anotherVideoPort.status());
        }
        
        aVideoPort = (VideoOutputPort)hdPorts.iterator().next();
        aVideoPort.enable();
        
        i = sdPorts.iterator();
        while(i.hasNext())
        {
        	VideoOutputPort anotherVideoPort = (VideoOutputPort)i.next();
        	assertFalse("SD ports should not be affected.", anotherVideoPort.status());
        }
        
        i =  hdPorts.iterator();
        while(i.hasNext())
        {
        	VideoOutputPort anotherVideoPort = (VideoOutputPort)i.next();
        	if(anotherVideoPort == aVideoPort)
        	{
        		assertTrue("A single HD port should be enabled.", anotherVideoPort.status());
        	}
        	else
        	{
        		assertFalse("All other HD ports should not be affected.", anotherVideoPort.status());
        	}
        }
    }
    
    public void testVOPListenerInterface()
    {
    	this.testCaseThread = Thread.currentThread();
        Object vop = this.getVideoPorts().nextElement();
        if (vop != null && vop instanceof VideoOutputSettings)
        {
            VideoOutputSettings vops = (VideoOutputSettings) vop;
            VideoOutputConfiguration current = vops.getOutputConfiguration();
            VideoOutputPortListener listener;
            vops.addListener(listener = new VideoOutputPortListener()
            {
                public void configurationChanged(VideoOutputPort source, VideoOutputConfiguration oldConfig,
                        VideoOutputConfiguration newConfig)
                {
                	configurationCBCalled = true;
                	testCaseThread.interrupt();
                }

                public void connectionStatusChanged(VideoOutputPort source, boolean status)
                {
                	connectionStatusCBCalled = true;
                	testCaseThread.interrupt();
                }

                public void enabledStatusChanged(VideoOutputPort source, boolean status)
                {
                	enabledStatusCBCalled = true;
                	testCaseThread.interrupt();
                }
            });

            try
            {
                assertFalse("configurationCBCalled is false before the call.", configurationCBCalled);
            	vops.setOutputConfiguration(current);
                try
                {
                    Thread.sleep(300);
                }
                catch (InterruptedException ie)
                {
                }
                assertTrue("configurationCBCalled is true after the call.", configurationCBCalled);
            }
            catch (java.lang.SecurityException se)
            {
                fail("Security exception");
            }
            catch (org.ocap.hardware.device.FeatureNotSupportedException fnse)
            {
                fail("FeatureNotSupported exception");
            }
            catch (java.lang.IllegalArgumentException iae)
            {
                fail("Illegal Argument Exception");
            }
            
            assertFalse("enabledStatusCBCalled is false before the call.", enabledStatusCBCalled);
            ((VideoOutputPort)vop).enable();
            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException ie)
            {
            }
            assertTrue("enabledStatusCBCalled is true after the call.", enabledStatusCBCalled);
            
            assertFalse("connectionStatusCBCalled is false before the call.", connectionStatusCBCalled);
            ((DeviceSettingsVideoOutputPortImpl)vop).asyncEvent(DeviceSettingsHostManagerImpl.MPE_DISP_EVENT_CONNECTED, ((DeviceSettingsVideoOutputPortImpl)vop).getHandle(), 0);
            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException ie)
            {
            }
            assertTrue("connectionStatusCBCalled is true after the call.", connectionStatusCBCalled);
            
            // remove listener
            vops.removeListener(listener);
        }
    }


    private Enumeration getVideoPorts()
    {
        DeviceSettingsHostImpl host = getHost();
        Enumeration e = host.getVideoOutputPorts();
        assertTrue(e.hasMoreElements());
        return e;
    }

    private void disableAllPorts()
    {
        Enumeration e = getVideoPorts();
        while (e.hasMoreElements())
        {
            ((VideoOutputPort) e.nextElement()).disable();
        }
    }

    private void enableAllPorts()
    {
        Enumeration e = getVideoPorts();
        while (e.hasMoreElements())
        {
            ((VideoOutputPort) e.nextElement()).enable();
        }
    }

    DeviceSettingsHostImpl getHost()
    {
        return (DeviceSettingsHostImpl) Host.getInstance();
    }
}