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

package org.cablelabs.impl.manager.pod;


import javax.tv.service.navigation.ServiceDetails;

import org.cablelabs.impl.davic.resources.ResourceOfferListener;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.session.MPEException;
import org.davic.resources.ResourceStatusListener;
import org.ocap.hardware.pod.POD;
import org.ocap.hardware.pod.PODApplication;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests PodImpl
 */
public class PodImplTest extends TestCase
{
    /**
     * Tests instantiation of a PodImpl()
     */
    public void testConstructor()
    {
        // test constructor
        PODManager pman = new TestPodManagerImpl();
        assertNotNull("Constructor failed to get an instance of POD", new PodImpl(pman));

        // end of test
        return;
    }

    /**
     * Tests PodImpl.isReady()
     */
    public void testIsReady()
    {
        TestPodManagerImpl pman = new TestPodManagerImpl();
        PodImpl pod = new PodImpl(pman);

        // test is-not-ready
        pman.isReady = false;
        assertFalse("POD failed to indicate a not-ready state", pod.isReady());

        // test is-ready
        pman.isReady = true;
        assertTrue("POD failed to indicate a ready state", pod.isReady());

        // end of test
        return;
    }

    /**
     * Tests PodImpl.getApplications()
     */
    public void testGetApplications()
    {
        TestPodManagerImpl pman = new TestPodManagerImpl();
        PodImpl pod = new PodImpl(pman);
        PODApplication[] returnedApps;

        // test for 0 apps when POD isn't ready
        pman.podApps = new byte[] { 0x00 };
        pman.isReady = false;
        try
        {
            returnedApps = pod.getApplications();
            fail("getApplications() unexpectedly succeeded when POD wasn't ready");
        }
        catch (IllegalStateException e1)
        {
            // successfully threw IllegalStateException when POD wasn't ready
        }
        catch (Exception e2)
        {
            fail("getApplications() unexpectedly threw another exception when POD wasn't ready: " + e2.getMessage());
        }

        // test for 0 apps when POD is ready
        pman.podApps = new byte[] { 0x00 };
        pman.isReady = true;
        try
        {
            returnedApps = pod.getApplications();
            assertEquals("POD failed to return 0-length application list", returnedApps.length, 0);
        }
        catch (IllegalStateException e1)
        {
            fail("getApplications() unexpectedly threw IllegalStateException: " + e1.getMessage());
        }
        catch (Exception e2)
        {
            fail("getApplications() unexpectedly threw another exception: " + e2.getMessage());
        }

        // TODO: determine how to format the byte array as a POD would
        // pman.appInfo = 10;
        // pman.podApps = new byte[??];
        // returnedApps = pod.getApplications();
        // assertEquals("POD failed to return expected application list",
        // returnedApps.length,10);

        // end of test
        return;
    }

    /**
     * Tests PodImpl.getHostParam()
     */
    public void testGetHostParam()
    {
        TestPodManagerImpl pman = new TestPodManagerImpl();

        int featureID = 0x01;
        byte[] hostParams = { 0x01, 0x02, 0x03, 0x04 };

        pman.hostFeatures = new int[] { featureID };
        pman.featureID = featureID;
        pman.hostParams = hostParams;

        // test getting Host Params when POD is ready

        pman.isReady = true;
        PodImpl pod1 = new PodImpl(pman);

        byte[] returnedParams1 = pod1.getHostParam(featureID);
        assertTrue("POD failed to return expected Feature Param array when POD is ready", areByteArraysEquivalent(
                returnedParams1, hostParams));

        // test getting Host Params when POD is not ready

        pman.isReady = false;
        PodImpl pod2 = new PodImpl(pman);

        byte[] returnedParams2 = pod2.getHostParam(featureID);
        assertTrue("POD failed to return expected Feature Param array when POD is not ready", areByteArraysEquivalent(
                returnedParams2, hostParams));

        // end of test
        return;
    }

    /**
     * Tests PodImpl.updateHostParam()
     */
    public void testUpdateHostParam()
    {
        TestPodManagerImpl pman = new TestPodManagerImpl();

        int goodFeatureID = 0x01;
        int badFeatureID = 0x11;
        byte[] hostParams = { 0x11, 0x12, 0x13, 0x14, 0x15 };

        pman.isReady = true;
        pman.hostFeatures = new int[] { goodFeatureID };
        pman.featureID = goodFeatureID;
        pman.hostParams = hostParams;

        PodImpl pod = new PodImpl(pman);

        // test update on unknown Host Parm when POD is ready
        assertFalse("POD succeeded to update unknown Feature Param when POD is ready", pod.updateHostParam(
                badFeatureID, hostParams));

        // test update on known Host Param when POD is ready
        pman.hostParams = null;
        assertTrue("POD failed to update Host Params when POD is ready", pod.updateHostParam(goodFeatureID, hostParams));
        assertTrue("POD failed to properly update Host Params", areByteArraysEquivalent(pman.hostParams, hostParams));

        // test update on unknown Host Parm when POD is not ready
        pman.isReady = false;
        assertFalse("POD succeeded to update unknown Feature Param when POD is not ready", pod.updateHostParam(
                badFeatureID, hostParams));

        // test update on known Host Param when POD is not ready
        pman.hostParams = null;
        assertFalse("POD updated Host Params when POD is not ready", pod.updateHostParam(goodFeatureID, hostParams));
        assertTrue("POD improperly updated Host Params when POD is not ready", areByteArraysEquivalent(pman.hostParams,
                null));

        // end of test
        return;
    }

    private boolean areByteArraysEquivalent(byte[] array1, byte[] array2)
    {
        if ((array1 == null) && (array2 == null)) return true;

        if (array1.length != array2.length) return false;

        for (int i = 0; i < array1.length; i++)
            if (array1[i] != array2[i]) return false;

        return true;
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
        TestSuite suite = new TestSuite(PodImplTest.class);
        return suite;
    }

    static
    {
        System.loadLibrary("mpe");
    }

}

/**
 * Test PodManager Implementation w/ public fields to control what this
 * PODManager does
 */
class TestPodManagerImpl implements PODManager
{
    // private Object eventListener = null;

    boolean isReady = false;

    boolean isPresent = false;

    int infoId = 0;

    int appInfo = 0;

    byte[] podApps = null;

    int[] hostFeatures = null;

    int featureID = 0;

    byte[] hostParams = null;

    public TestPodManagerImpl()
    {
        return;
    }

    public synchronized static Manager getInstance()
    {
        return new TestPodManagerImpl();
    }

    public synchronized void destroy()
    {
        return;
    }

    public void finalize()
    {
        destroy();
        return;
    }

    public POD getPOD()
    {
        return null;
    }

    public int getPODInfo(int infoId, int podAppInfo)
    {
        if (this.infoId == infoId)
        {
            return this.appInfo;
        }
        else
        {
            return 0;
        }
    }

    public byte[] getPODApplications(int podAppInfo)
    {
        if (!isPODReady()) throw new IllegalStateException("CableCard device not ready");

        return this.podApps;
    }

    public boolean isPODReady()
    {
        return this.isReady;
    }

    public boolean isPODPresent()
    {
        return isPresent;
    }

    public int[] getPODHostFeatures()
    {
        return this.hostFeatures;
    }

    public byte[] getPODHostParam(int featureID)
    {
        if (this.featureID == featureID)
        {
            return this.hostParams;
        }
        else
        {
            return new byte[0];
        }
    }

    public boolean updatePODHostParam(int featureID, byte[] value)
    {
        if (this.featureID == featureID)
        {
            this.hostParams = value;
            return true;
        }
        else
        {
            return false;
        }
    }

    public void initPOD(Object eventListener)
    {
        // this.eventListener = eventListener;
        return;
    }

    public void addPODListener(PODListener listener)
    {
    }

    public void removePODListener(PODListener listener)
    {
    }

    public CASession startDecrypt(CADecryptParams params) throws MPEException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void stopDecrypt(CASession session) throws MPEException
    {
        // TODO Auto-generated method stub

    }

    public void addResourceOfferListener(ResourceOfferListener listener, int priority)
    {
        // TODO Auto-generated method stub

    }

    public void removeResourceOfferListener(ResourceOfferListener listener)
    {
        // TODO Auto-generated method stub

    }

    public void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        // TODO Auto-generated method stub

    }

    public void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        // TODO Auto-generated method stub

    }

    public int getManufacturerID()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getVersion()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public byte[] getMACAddress()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public byte[] getSerialNumber()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public PODApplication[] getPODApplications()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void processApplicationInfoCnfAPDU(byte[] apdu_data,
            int resourceVersion)
    {
        // TODO Auto-generated method stub
        
    }

    public void removeCCIForService(Object key)
    {
        // TODO Auto-generated method stub
        
    }

    public void setCCIForService(Object key, ServiceDetails serviceDetails, byte cci)
    {
        // TODO Auto-generated method stub
        
    }

    public CopyControlInfo getCCIForService(ServiceDetails serviceDetails)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
