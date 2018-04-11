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

package org.ocap.dvr;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.cablelabs.test.TestUtils;

import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.storage.MediaStorageVolume;

/**
 * Tests org.ocap.dvr.OcapRecordingProperties
 * 
 * @author Arlis Dodson
 */
public class OcapRecordingPropertiesTest extends TestCase
{
    private byte m_bitRate;

    private long m_expPer;

    private byte m_priFlag;

    private String m_org;

    private ExtendedFileAccessPermissions m_fap;

    private MediaStorageVolume m_msv;

    private int m_retPri;

    /**
     * Test well-formed constructor calls ...
     */
    public void testCtorOkay()
    {

        try
        {
            m_bitRate = OcapRecordingProperties.HIGH_BIT_RATE;
            OcapRecordingProperties orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap,
                    m_org, m_msv);
            assertNotNull("Failed to construct OcapRecordingProperties Object", orp);
            m_bitRate = OcapRecordingProperties.MEDIUM_BIT_RATE;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            assertNotNull("Failed to construct OcapRecordingProperties Object", orp);
            m_bitRate = OcapRecordingProperties.LOW_BIT_RATE;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            assertNotNull("Failed to construct OcapRecordingProperties Object", orp);
            m_priFlag = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            assertNotNull("Failed to construct OcapRecordingProperties Object", orp);
            m_priFlag = OcapRecordingProperties.RECORD_WITH_CONFLICTS;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            assertNotNull("Failed to construct OcapRecordingProperties Object", orp);
            m_priFlag = OcapRecordingProperties.TEST_RECORDING;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            assertNotNull("Failed to construct OcapRecordingProperties Object", orp);
        }
        catch (Exception e)
        {
            assertNotNull("Failed to construct OcapRecordingProperties Object");
            fail("OcapRecordingProperties ctor threw " + e.getMessage());
        }
    }

    /**
     * Test ill-formed constructor calls ...
     */
    public void testCtorBad()
    {
        try
        {
            m_bitRate = 0;
            OcapRecordingProperties orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap,
                    m_org, m_msv);
            fail("OcapRecordingProperties ctor failed to throw IllegalArgumentException");
            m_bitRate = Byte.MAX_VALUE;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            fail("OcapRecordingProperties ctor failed to throw IllegalArgumentException");
            m_bitRate = OcapRecordingProperties.LOW_BIT_RATE;
            m_priFlag = 0;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            fail("OcapRecordingProperties ctor failed to throw IllegalArgumentException");
            m_priFlag = Byte.MAX_VALUE;
            orp = new OcapRecordingProperties(m_bitRate, m_expPer, m_retPri, m_priFlag, m_fap, m_org, m_msv);
            fail("OcapRecordingProperties ctor failed to throw IllegalArgumentException");
        }
        catch (Exception e)
        {
            assertTrue("OcapRecordingProperties ctor threw Exception other than IllegalArgumentException",
                    e instanceof IllegalArgumentException);
        }
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
        TestSuite suite = new TestSuite(OcapRecordingPropertiesTest.class);
        return suite;
    }

    public OcapRecordingPropertiesTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        m_bitRate = OcapRecordingProperties.HIGH_BIT_RATE;
        m_expPer = 300;
        m_fap = new ExtendedFileAccessPermissions(true, true, true, true, true, true, null, null);
        m_priFlag = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;
        m_org = "AcmeVideo";
        m_retPri = OcapRecordingProperties.DELETE_AT_EXPIRATION;
        // TODO - obtain ref to MediaStorageVolume
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
}
