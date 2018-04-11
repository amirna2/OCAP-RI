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

/*
 * @author Alan Cohn
 */

package org.cablelabs.impl.ocap.manager.eas.message;

import org.cablelabs.impl.ocap.manager.eas.message.EASLocationCode;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EASLocationCodeTest extends TestCase
{
    private int STATE = 1;

    private int COUNTY = 2;

    private int COUNTY_SUB = 3;

    private EASLocationCode base = new EASLocationCode(STATE, COUNTY, COUNTY_SUB);

    private EASLocationCode baseMax = new EASLocationCode(EASLocationCode.MAX_STATES, EASLocationCode.MAX_COUNTIES,
            EASLocationCode.MAX_SUBDIVISIONS);

    /*
     * Test EASLocationCode constructor and equals method for simple parameters.
     */
    public void testEASLocationCodeEqual()
    {
        byte[] codes = packLocationCode(STATE, COUNTY, COUNTY_SUB);
        EASLocationCode test = new EASLocationCode(codes);
        assertTrue("testEASLocationCodeEqual not equal", test.equals(base));
    }

    /*
     * Test EASLocationCode constructor and equals method for MAX parameters.
     */
    public void testEASLocationCodeMaxEqual()
    {
        byte[] codes = packLocationCode(EASLocationCode.MAX_STATES, EASLocationCode.MAX_COUNTIES,
                EASLocationCode.MAX_SUBDIVISIONS);
        EASLocationCode test = new EASLocationCode(codes);
        assertTrue("testEASLocationCodeMaxEqual not equal", test.equals(baseMax));

        assertTrue("testEASLocationCodeMaxEqual hash code not same", test.hashCode() == baseMax.hashCode());
    }

    /*
     * Test EASLocationCode constructor and equals method for ALL parameters.
     */
    public void testEASLocationCodeUnspecified()
    {
        byte[] codes = packLocationCode(EASLocationCode.ALL_STATES, EASLocationCode.ALL_COUNTIES,
                EASLocationCode.ALL_SUBDIVISIONS);
        EASLocationCode test = new EASLocationCode(codes);
        assertTrue("testEASLocationCodeUnspecified not equal", test.equals(EASLocationCode.UNSPECIFIED));
    }

    /*
     * Test EASLocationCode constructor with null parameter
     */
    public void testEASLocationCodeArrayNull()
    {
        try
        {
            EASLocationCode test = new EASLocationCode(null);
            assertTrue("testEASLocationCodeArrayNull no exception", false);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    /*
     * Test EASLocationCode constructor with to large parameter
     */
    public void testEASLocationCodeArrayBig()
    {
        byte[] tooBig = { 0, 0, 0, 0 };
        try
        {
            EASLocationCode test = new EASLocationCode(tooBig);
            assertTrue("testEASLocationCodeArrayBig no exception", false);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    /*
     * Test EASLocationCode constructor with too small parameter
     */

    public void testEASLocationCodeArraySmall()
    {
        byte[] tooSmall = { 0, 0 };
        try
        {
            EASLocationCode test = new EASLocationCode(tooSmall);
            assertTrue("testEASLocationCodeArraySmall no exception", false);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(true);
        }
    }

    /*
     * For the 3 byte array: byte [0] all 8 bits state byte [1] left most 4 bits
     * subdivision + right most 2 bits county 2 MSB byte [2] all 8 bits county
     * LSB
     */
    private byte[] packLocationCode(int state, int county, int sub)
    {
        byte[] bArray = { (byte) state, (byte) ((sub << 4) | (county >> 8)), (byte) (county & 0xff) };
        return bArray;

    }

    /*
     * Class initialization from TestCase
     */
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
        TestSuite suite = new TestSuite(EASLocationCodeTest.class);
        return suite;
    }

}
