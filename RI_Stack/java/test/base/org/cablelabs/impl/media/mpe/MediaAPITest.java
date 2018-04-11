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

package org.cablelabs.impl.media.mpe;

import java.awt.Dimension;

import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.CannedHScreen;
import org.cablelabs.impl.media.CannedVideoDevice;
import org.cablelabs.impl.media.mpe.MediaAPI.Event;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoDevice;
import org.ocap.media.ClosedCaptioningControl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * @author Joshua Keplinger
 * 
 */
public class MediaAPITest extends TestCase
{
    protected CannedVideoDevice vd;

    protected MediaAPI mapi;

    /**
     * 
     */
    public MediaAPITest()
    {
        super("MediaAPITest");
    }

    public MediaAPITest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            TestRunner.run(suite());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(MediaAPITest.class);
        suite.setName("MediaAPITest");
        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();

        mapi = new CannedMediaAPIImpl();
        vd = (CannedVideoDevice) CannedHScreen.getInstance().getDefaultHVideoDevice();
    }

    public void tearDown() throws Exception
    {
        ((CannedMediaAPIImpl) mapi).destroy();

        mapi = null;
        vd = null;

        super.tearDown();
    }

    /*
     * Testing section
     */
    public void testDecodeBroadcast() throws Exception
    {
        EDListener listener = new EDListener()
        {
            public void asyncEvent(int eventCode, int eventData1, int eventData2)
            {
            }
        };
        int[] pids = new int[] { 1, 2 };
        short[] types = new short[] { 128, 127 };
        byte cci = 0;
        MediaDecodeParams params = new MediaDecodeParams(listener, vd.getHandle(), 1, (short)0, pids[0], pids, types, false, false, new float[]{0.0F}, cci);

        // This is just to make sure it doesn't arbitrarily throw an Exception
        int session = mapi.decodeBroadcast(params);
        assertEquals(session, params.getVideoHandle());
        mapi.stopBroadcastDecode(session, false);

        // Now for exception testing
        // Test each of the objects passed in
        try
        {
            params = new MediaDecodeParams(null, vd.getHandle(), 1, (short)0, pids[0], pids, types, false, false, new float[]{0.0F}, cci);
            session = mapi.decodeBroadcast(params);
            fail("Exception should be thrown with null EDListener");
        }
        catch (IllegalArgumentException ex)
        {
        }
        try
        {
            params = new MediaDecodeParams(listener, vd.getHandle(), 1, (short)0, 42, null, types, false, false, new float[]{0.0F}, cci);
            session = mapi.decodeBroadcast(params);
            fail("Exception should be thrown with null pids array");
        }
        catch (IllegalArgumentException ex)
        {
        }
        try
        {
            params = new MediaDecodeParams(listener, vd.getHandle(), 1, (short)0, pids[0], pids, null, false, false, new float[]{0.0F}, cci);
            session = mapi.decodeBroadcast(params);
            fail("Exception should be thrown with null types array");
        }
        catch (IllegalArgumentException ex)
        {
        }

        // Now we'll make the two arrays different sizes and cause an Exception
        pids = new int[] { 7 };
        try
        {
            params = new MediaDecodeParams(listener, vd.getHandle(), 1, (short)0, pids[0], pids, types, false, false, new float[]{0.0F}, cci);
            session = mapi.decodeBroadcast(params);
            fail("Exception should be thrown with different sized arrays");
        }
        catch (IllegalArgumentException ex)
        {
        }

        // Finally, we'll pass in a bad decoder handle to cause an error from
        // JNI code and an exception to be thrown
        pids = new int[] { 1, 2 };
        try
        {
            params = new MediaDecodeParams(listener, -1, 1, (short)0, pids[0], pids, types, false, false, new float[]{0.0F}, cci);
            session = mapi.decodeBroadcast(params);
            fail("Exception should be thrown with bad decoder handle");
        }
        catch (MPEMediaError err)
        {
        }
    }

    public void testSetAndGetBounds()
    {
        ScalingBounds bounds = mapi.getBounds(vd.getHandle());
        assertNotNull("Returned bounds is null", bounds);

        bounds.dst = new HScreenRectangle(0, 0, 0.5f, 0.5f);
        assertTrue(mapi.setBounds(vd.getHandle(), bounds));

        ScalingBounds newBounds = mapi.getBounds(vd.getHandle());
        assertNotNull("Returned size is null", newBounds);
        assertEquals("New size does not match expected size", bounds, newBounds);

        // Bad video device for setBounds()
        assertFalse("Expected false with invalid VD", mapi.setBounds(7, bounds));

        // Bad video device for getBounds()
        try
        {
            mapi.getBounds(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testCheckBounds()
    {
        ScalingBounds size = mapi.getBounds(vd.getHandle());
        size.dst = new HScreenRectangle(0.25f, 0.20f, 0.75f, 0.80f);

        ScalingBounds retValue = mapi.checkBounds(vd.getHandle(), size);
        assertEquals("Returned width does not match expected result", 0.75f, retValue.dst.width, 0.001f);
        assertEquals("Returned height does not match expected result", 0.8f, retValue.dst.height, 0.001f);

        // Bad ScalingBounds
        try
        {
            mapi.checkBounds(vd.getHandle(), null);
            fail("Expected IllegalArgumentException with null SB");
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Bad video device for getBounds()
        try
        {
            mapi.checkBounds(7, size);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testSwapDecoders()
    {
        // Call method to make sure it doesn't throw arbitrary Exception
        HVideoDevice[] vds = CannedHScreen.getInstance().getHVideoDevices();
        mapi.swapDecoders(((CannedVideoDevice) vds[0]).getHandle(), ((CannedVideoDevice) vds[1]).getHandle(), true);

        // Now we'll pass in an invalid decoder to cause a failure
        try
        {
            mapi.swapDecoders(-1, 2, true);
            fail("Exception should be thrown with null EDListener");
        }
        catch (MPEMediaError err)
        {
        }
    }

    public void testBlock() throws Exception
    {
        // Call method with valid parameteras and make sure it doesn't throw
        // exception.
        mapi.blockPresentation(vd.getHandle(), true);
        mapi.blockPresentation(vd.getHandle(), false);
        // Try an invalid session handle to cause a failure
        try
        {
            mapi.blockPresentation(-1, false);
            fail("Exception should be thrown with bad session handle");
        }
        catch (MPEMediaError err)
        {/* expected */
        }
    }

    public void testFreeze() throws Exception
    {
        // Call method to make sure it doesn't throw arbitrary Exception
        mapi.freeze(vd.getHandle());

        // Now we'll pass in an invalid decoder to cause a failure
        try
        {
            mapi.freeze(-1);
            fail("Exception should be thrown with bad decoder handle");
        }
        catch (MPEMediaError err)
        {/* expected */
        }
    }

    public void testResume() throws Exception
    {
        // Call method to make sure it doesn't throw arbitrary Exception
        mapi.resume(vd.getHandle());

        // Now we'll pass in an invalid decoder to cause a failure
        try
        {
            mapi.resume(-1);
            fail("Exception should be thrown with bad decoder handle");
        }
        catch (MPEMediaError err)
        {/* expected */
        }
    }

    public void testSupportScaling()
    {
        // Call method to make sure it doesn't throw arbitrary Exception
        assertTrue("Result should be true", mapi.supportsComponentVideo(vd.getHandle()));

        // Now we'll pass in an invalid decoder to cause a failure
        try
        {
            mapi.supportsComponentVideo(-1);
            fail("Exception should be thrown with null EDListener");
        }
        catch (MPEMediaError err)
        {
        }
    }

    public void testDripFeedStart()
    {
        EDListener listener = new EDListener()
        {

            public void asyncEvent(int eventCode, int eventData1, int eventData2)
            {

            }

        };
        MediaDripFeedParams params = new MediaDripFeedParams(listener, 1);
        // Successful call
        int handle = mapi.dripFeedStart(params);
        assertEquals("Handle does not match", params.videoHandle, handle);

        // Now a bad VD
        try
        {
            params = new MediaDripFeedParams(listener, 7);
            mapi.dripFeedStart(params);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testDripFeedRenderFrame()
    {
        byte[] data = new byte[4];
        // Successful call
        mapi.dripFeedRenderFrame(1, data);

        // Now a bad dripfeed handle
        try
        {
            mapi.dripFeedRenderFrame(7, data);
            fail("Expected MPEMediaError with bad dripfeed handle");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testDripFeedStop()
    {
        // Successful call
        mapi.dripFeedStop(1);

        // Now a bad dripfeed handle
        try
        {
            mapi.dripFeedStop(7);
            fail("Expected MPEMediaError with bad dripfeed handle");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testGetInputVideoSize()
    {
        // Successful call
        Dimension size = mapi.getVideoInputSize(1);
        assertEquals("Height is incorrect", 480, size.height);
        assertEquals("Width is incorrect", 640, size.width);

        // Bad video device
        try
        {
            mapi.getVideoInputSize(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testSupportsComponentVideo()
    {
        // Successful call
        assertTrue("VD should support component video", mapi.supportsComponentVideo(1));

        // Invalid video device
        try
        {
            mapi.supportsComponentVideo(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testSetAndGetCCState()
    {
        // Successfull calls
        assertEquals("CCState is incorrect", ClosedCaptioningControl.CC_TURN_OFF, mapi.getCCState());
        mapi.setCCState(ClosedCaptioningControl.CC_TURN_ON);
        assertEquals("CCState is incorrect", ClosedCaptioningControl.CC_TURN_ON, mapi.getCCState());

        // Set bad ccstate
        try
        {
            mapi.setCCState(19);
            fail("Expected MPEMediaError with bad ccstate");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testSetCCServiceNumbers()
    {
        // Successful call
        mapi.setCCServiceNumbers(ClosedCaptioningControl.CC_ANALOG_SERVICE_T1, 200);

        // Bad analog service number
        try
        {
            mapi.setCCServiceNumbers(1, 100);
            fail("Expected MPEMediaError with bad analog service number");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testGetSupportedCCServiceNumbers()
    {
        int[] svcNums = mapi.getCCSupportedServiceNumbers();
        assertEquals("Array length is incorrect", 2, svcNums.length);
        assertEquals("Analog value is incorrect", ClosedCaptioningControl.CC_ANALOG_SERVICE_CC1, svcNums[0]);
        assertEquals("Digital value is incorrect", 100, svcNums[1]);
    }

    public void testGetAspectRatio()
    {
        // Successful call
        assertEquals("AR is incorrect", 2, mapi.getAspectRatio(1));

        // Bad video device
        try
        {
            mapi.getAspectRatio(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testGetActiveFormatDefinition()
    {
        // Successful call
        assertEquals("AFD is incorrect", 15, mapi.getActiveFormatDefinition(1));

        // Bad video device
        try
        {
            mapi.getActiveFormatDefinition(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testIsPlatformDFC()
    {
        // Successful call
        assertTrue("VD should be platform DFC", mapi.isPlatformDFC(1));

        // Bad video device
        try
        {
            mapi.isPlatformDFC(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testGetDFC()
    {
        // Successful call
        assertEquals("DFC is incorrect", 2, mapi.getDFC(1));

        // Bad video device
        try
        {
            mapi.getDFC(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testCheckDFC()
    {
        // Successful call
        assertTrue("DFC should be valid", mapi.checkDFC(1, 2));

        // Bad video device
        assertFalse("DFC should be invalid", mapi.checkDFC(29, 2));
    }

    public void testSetDFC()
    {
        // Successful call
        mapi.setDFC(1, 4);

        // Bad video device
        try
        {
            mapi.setDFC(7, 2);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testSupportsClipping()
    {
        // Successful call
        assertTrue("VD should support clipping", mapi.supportsClipping(1));

        // Bad video device
        try
        {
            mapi.supportsClipping(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testGetPositioningCapability()
    {
        // Successful call
        assertEquals("Wrong positioning capability", 0, mapi.getPositioningCapability(1));

        // Bad video device
        try
        {
            mapi.getPositioningCapability(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testGetScalingCaps()
    {
        // Successful call
        ScalingCaps caps = mapi.getScalingCaps(1);
        assertNotNull("ScalingCaps cannot be null", caps);

        // Bad video device
        try
        {
            mapi.getScalingCaps(7);
            fail("Expected MPEMediaError with bad video device");
        }
        catch (MPEMediaError expected)
        {
        }
    }

    public void testEventToString()
    {
        CannedMediaAPIImpl cmapi = (CannedMediaAPIImpl) mapi;
        assertEquals("MEDIA_CONTENT_PRESENTING", cmapi.toString(Event.CONTENT_PRESENTING));
        assertEquals("ED_QUEUE_TERMINATED", cmapi.toString(Event.QUEUE_TERMINATED));
        assertEquals("MEDIA_STILL_FRAME_DECODED", cmapi.toString(Event.STILL_FRAME_DECODED));
        assertEquals("MEDIA_FAILURE_UNKNOWN", cmapi.toString(Event.FAILURE_UNKNOWN));
        assertEquals("STREAM_RETURNED", cmapi.toString(Event.STREAM_RETURNED));
        assertEquals("STREAM_NO_DATA", cmapi.toString(Event.STREAM_NO_DATA));
        assertEquals("STREAM_CA_DIALOG_PAYMENT", cmapi.toString(Event.STREAM_CA_DIALOG_PAYMENT));
        assertEquals("STREAM_CA_DIALOG_TECHNICAL", cmapi.toString(Event.STREAM_CA_DIALOG_TECHNICAL));
        assertEquals("STREAM_CA_DENIED_ENTITLEMENT", cmapi.toString(Event.STREAM_CA_DENIED_ENTITLEMENT));
        assertEquals("STREAM_CA_DENIED_TECHNICAL", cmapi.toString(Event.STREAM_CA_DENIED_TECHNICAL));
        assertEquals("ACTIVE_FORMAT_CHANGED", cmapi.toString(Event.ACTIVE_FORMAT_CHANGED));
        assertEquals("ASPECT_RATIO_CHANGED", cmapi.toString(Event.ASPECT_RATIO_CHANGED));
        assertEquals("DFC_CHANGED", cmapi.toString(Event.DFC_CHANGED));
        assertEquals("UNKNOWN(1000)", cmapi.toString(1000));
    }

}
