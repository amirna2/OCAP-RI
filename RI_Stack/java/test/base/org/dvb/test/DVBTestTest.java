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

package org.dvb.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.cablelabs.test.TestUtils;

import java.io.IOException;

/**
 * Tests org.dvb.test.DVBTest
 * 
 * @author Aaron Kamienski
 * @author Brent Thompson
 * @author Arlis Dodson
 */
public class DVBTestTest extends TestCase
{
    private final String m_id = "TestCaseID";

    private final String m_msg = "Canned message";

    private final int m_num = 1313;

    /**
     * Tests public fields.
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(DVBTest.class);
        TestUtils.testNoAddedFields(DVBTest.class, fieldNames);
        TestUtils.testFieldValues(DVBTest.class, fieldNames, fieldValues);
    }

    /**
     * Tests that log(String,String) throws IOException
     */
    public void testLog1() throws Exception
    {
        try
        {
            DVBTest.log(m_id, m_msg);
            fail("No Exception thrown by DVBTest.log(String,String)");
        }
        catch (Exception e)
        {
            assertTrue("Wrong Exception thrown: " + e.getMessage(), e instanceof IOException);
        }
    }

    /**
     * Tests that log(String,int) throws IOException
     */
    public void testLog2() throws Exception
    {
        try
        {
            DVBTest.log(m_id, m_num);
            fail("No Exception thrown by DVBTest.log(String,int)");
        }
        catch (Exception e)
        {
            assertTrue("Wrong Exception thrown: " + e.getMessage(), e instanceof IOException);
        }
    }

    /**
     * Tests that prompt(String,int,String) throws IOException
     */
    public void testPrompt() throws Exception
    {
        try
        {
            DVBTest.prompt(m_id, m_num, m_msg);
            fail("No Exception thrown by DVBTest.prompt(String,int,String)");
        }
        catch (Exception e)
        {
            assertTrue("Wrong Exception thrown: " + e.getMessage(), e instanceof IOException);
        }
    }

    /**
     * Tests that terminate(String,int) throws IOException
     */
    public void testTerminate() throws Exception
    {
        int[] termConds = { DVBTest.PASS, DVBTest.FAIL, DVBTest.OPTION_UNSUPPORTED, DVBTest.HUMAN_INTERVENTION,
                DVBTest.UNRESOLVED, DVBTest.UNTESTED, -999 };
        for (int i = 0; i < termConds.length; ++i)
        {

            int cond = termConds[i];
            try
            {
                DVBTest.terminate(m_id, cond);
                fail("No exception thrown by DVBTest.terminate(String," + cond + ")");
            }
            catch (Exception e)
            {
                assertTrue("Wrong Exception thrown: " + e.getMessage(), e instanceof IOException);
            }
        }
    }

    /**
     * Names of public static fields.
     */
    private static final String[] fieldNames = { "PASS", "FAIL", "OPTION_UNSUPPORTED", "HUMAN_INTERVENTION",
            "UNRESOLVED", "UNTESTED" };

    /**
     * Expected values of public static fields.
     */
    private static final int[] fieldValues = { 0, -1, -2, -3, -4, -5 };

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
        TestSuite suite = new TestSuite(DVBTestTest.class);
        return suite;
    }

    public DVBTestTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

}
