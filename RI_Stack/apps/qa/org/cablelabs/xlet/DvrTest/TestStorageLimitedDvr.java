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
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.net.OcapLocator;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

public class TestStorageLimitedDvr extends DvrTest
{
    public TestStorageLimitedDvr(Vector locators)
    {
        super(locators);
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new ChangeMinTSBSpace());
        return tests;
    }


    private class ChangeMinTSBSpace extends TestCase
    {
        ChangeMinTSBSpace()
        {
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("ChangeMinTSPSpace: run test");
            m_failed = TEST_PASSED;

            MediaStorageVolume msv = null;
            StorageProxy[] proxies = StorageManager.getInstance().getStorageProxies();

            LogicalStorageVolume lsv[] = proxies[0].getVolumes();

            for (int i =0; i < lsv.length; i++)
            {
                if (lsv[i] instanceof MediaStorageVolume)
                {
                    msv = (MediaStorageVolume)lsv[i];
                }
            }

            OcapRecordingManager recMgr = (OcapRecordingManager)OcapRecordingManager.getInstance();
            long minTSBDuration = recMgr.getSmallestTimeShiftDuration();
            long minTSBSize = msv.getMinimumTSBSize();
            long freeSpace = msv.getFreeSpace();

            DVRTestRunnerXlet.log("initial minimum TSB Duration ="+minTSBDuration);
            DVRTestRunnerXlet.log("initial minimum TSB Size ="+minTSBSize);
            DVRTestRunnerXlet.log("initial free space is ="+freeSpace);


            long[] newSizes = {5000000, freeSpace, minTSBDuration, freeSpace+minTSBDuration, minTSBDuration, freeSpace+minTSBDuration+1, minTSBDuration, 0};
            for (int i = 0; i < newSizes.length; i++)
            {
                try
                {
                    msv.setMinimumTSBSize(newSizes[i]);
                    if (newSizes[i] > freeSpace + minTSBSize)
                    {
                        m_failedReason = "TestStorageLimitedDvr FAIL: IllegalArgumentException failed to be thrown when setting the minimum TSB size to be " +newSizes[i] +" which is > free space (" +freeSpace +") + current TSB size (" +minTSBSize +")";
                        DVRTestRunnerXlet.log(m_failedReason);
                        m_failed = TEST_FAILED;
                    }
                }
                catch (IllegalArgumentException e)
                {
                    if (newSizes[i] <= freeSpace + minTSBSize)
                    {
                        m_failedReason = "TestStorageLimitedDvr FAIL: caught IllegalArgumentException even though the minimum TSB size requested is " +newSizes[i] +" which is <= free space (" +freeSpace +") + current TSB size (" +minTSBSize +")";
                        DVRTestRunnerXlet.log(m_failedReason);
                        m_failed = TEST_FAILED;
                    }
                }

                long newMinTSBDuration = recMgr.getSmallestTimeShiftDuration();
                long newMinTSBSize = msv.getMinimumTSBSize();
                long newFreeSpace = msv.getFreeSpace();
             
                if (newMinTSBDuration != minTSBDuration)
                {
                    m_failedReason = "TestStorageLimitedDvr FAIL: setting the minimum TSBSize via setMinimumTSBSize() should have no affect on the smallestTimeShiftDuration; it should still = "+minTSBDuration +" instead of the new value of "+newMinTSBDuration;
                    DVRTestRunnerXlet.log(m_failedReason);
                    m_failed = TEST_FAILED;
                }
                if (newFreeSpace != freeSpace)
                {
                    m_failedReason = "TestStorageLimitedDvr FAIL: setting the minimum TSBSize via setMinimumTSBSize() should have no affect on the amount of free space; it should still = "+freeSpace +" instead of the new value of "+newFreeSpace;
                    DVRTestRunnerXlet.log(m_failedReason);
                    m_failed = TEST_FAILED;
                }
                if (newMinTSBSize != newSizes[i] &&
                    newSizes[i] <= freeSpace + minTSBSize)
                {
                    m_failedReason = "TestStorageLimitedDvr FAIL: setting the minimum TSBSize via setMinimumTSBSize() failed; new value should be "+newSizes[i] +" instead of the value of "+newMinTSBSize;
                    DVRTestRunnerXlet.log(m_failedReason);
                    m_failed = TEST_FAILED;
                }
                if (newMinTSBSize != minTSBSize &&
                    newSizes[i] > freeSpace + minTSBSize)
                {
                    m_failedReason = "TestStorageLimitedDvr FAIL: value of minimum TSB size should not have changed from " +minTSBSize +" since the new value requested of " +newSizes[i] +" is > free space (" +freeSpace +") + current TSB size (" +minTSBSize +"); instead it is set to "+newMinTSBSize;
                    DVRTestRunnerXlet.log(m_failedReason);
                    m_failed = TEST_FAILED;
                }

                DVRTestRunnerXlet.log("TestStorageLimitedDvr: after setting minimum TSB size to " +newSizes[i] +":\n      new minimum TSB Duration = " +newMinTSBDuration +"\n      new minimum TSB Size = "+newMinTSBSize +"\n      new free space = "+newFreeSpace);

                minTSBSize = newMinTSBSize;
            }


            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getName() + " completed: FAILED");
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }

        public String getName()
        {
            return "ChangeMinTSBSpace";
        }
    }

}
