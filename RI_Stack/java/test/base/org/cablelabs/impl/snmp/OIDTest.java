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

package org.cablelabs.impl.snmp;

import org.cablelabs.impl.snmp.OID;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OIDTest extends TestCase
{
    private OID o1234 = new OID("1.2.3.4");

    private OID o12 = new OID("1.2");

    private OID o12340 = new OID("1.2.3.4.0");

    private OID o12340_e = new OID("1.2.3.4.0");

    private OID o12341 = new OID("1.2.3.4.1");

    private OID o134 = new OID("1.3.4");

    private OID o1234_l = new OID("1.2.3.4", true);

    private OID o1234567 = new OID("1.2.3.4.5.6.7");

    private OID o1234567_l = new OID("1.2.3.4.5.6.7", true);

    private OID o12345 = new OID("1.2.3.4.5");

    private OID o12346 = new OID("1.2.3.4.6");

    private OID o12340_l = new OID("1.2.3.4.0", true);

    private OID o12341_l = new OID("1.2.3.4.1", true);

    private OID o12345_29_8 = new OID("1.2.3.4.5.29.8");

    private OID o12345000 = new OID("1.2.3.4.5.0.0.0");

    private OID o12340000 = new OID("1.2.3.4.0.0.0.0");

    private OID o12340000_l = new OID("1.2.3.4.0.0.0.0", true);

    private OID o123400135 = new OID("1.2.3.4.0.0.1.3.5");

    private OID o123405_l = new OID("1.2.3.4.0.5", true);

    private OID o123400_l = new OID("1.2.3.4.0.0", true);

    private OID o12340003_l = new OID("1.2.3.4.0.0.0.3", true);

    public OIDTest(String name)
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

        suite.addTest(new OIDTest("testRelationships"));
        suite.addTest(new OIDTest("testCompareTo"));

        return suite;
    }

    /*
     * Start of tests
     */
    public void testCompareTo()
    {

        assertEquals("should be equal", 0, o12340.compareTo(o12340_e));
        assertEquals("should be equal", 0, o12340.compareTo(o12340));
        assertEquals("should be equal", 0, o12340_e.compareTo(o12340_e));
        assertEquals("should be a less", -1, o12340_e.compareTo(o12341));
        assertEquals("should be a less", -1, o12340.compareTo(o1234));
        assertEquals("should be a less", -1, o12340.compareTo(o12));
        assertEquals("should be a more", 1, o1234.compareTo(o12340));
        assertEquals("should be a more", 1, o12.compareTo(o12340));

        assertEquals("should be less", -1, o1234.compareTo(o134));
    }

    public void testRelationships()
    {
        assertEquals("should be equal", OID.cEqual, o12340.getRelationship(o12340_e));
        assertEquals("should be equal", OID.cEqual, o12340.getRelationship(o12340));
        assertEquals("should be equal", OID.cEqual, o12340_e.getRelationship(o12340));
        assertEquals("should be a lesser sib", OID.cLesserSibling, o12340.getRelationship(o12341));
        assertEquals("should be a greater sib", OID.cGreaterSibling, o12341.getRelationship(o12340));
        assertEquals("should be a child", OID.cChild, o12340.getRelationship(o1234));
        assertEquals("should be a child", OID.cChild, o12340.getRelationship(o12));
        assertEquals("should be a parent", OID.cParent, o1234.getRelationship(o12340));
        assertEquals("should be a parent", OID.cParent, o12.getRelationship(o12340));
        assertEquals("should be lesser branch", OID.cLesserBranch, o1234.getRelationship(o134));
        assertEquals("should be greater branch", OID.cGreaterBranch, o134.getRelationship(o1234));

        // 1.2.3.4(0.0.0)
        // 1.2.3.4
        assertEquals(OID.cChild, o1234_l.getRelationship(o1234));

        // 1.2.3.4
        // 1.2.3.4(0.0.0)
        assertEquals(OID.cParent, o1234.getRelationship(o1234_l));

        // 1.2.3.4(0.0.0)
        // 1.2.3.4.5.6.7
        assertEquals(OID.cLesserBranch, o1234_l.getRelationship(o1234567));

        // 1.2.3.4.5.6.7(0.0.0.0.0)
        // 1.2.3.4
        assertEquals(OID.cChild, o1234567_l.getRelationship(o1234));

        // 1.2.3.4.5.6.7
        // 1.2.3.4(0.0.0)
        assertEquals(OID.cGreaterBranch, o1234567.getRelationship(o1234_l));

        assertEquals(OID.cChild, o12340_l.getRelationship(o12340));
        assertEquals(OID.cParent, o12340.getRelationship(o12340_l));

        assertEquals(OID.cLesserSibling, o12345.getRelationship(o12346));
        assertEquals(OID.cGreaterSibling, o12346.getRelationship(o12345));

        // 1.2.3.4.0
        // 1.2.3.4.0.0.0.0.0
        assertEquals(OID.cParent, o12340.getRelationship(o12340_l));

        // 1.2.3.4.0(0.0.0.0)
        // 1.2.3.4.0
        assertEquals(OID.cChild, o12340_l.getRelationship(o12340));

        // * 1.2.3.4.0 (l) : 1.2.3.4.0 (l) == cEqual
        assertEquals(OID.cEqual, o12340_l.getRelationship(o12340_l));

        // 1.2.3.4.1
        // 1.2.3.4.0
        assertEquals(OID.cGreaterSibling, o12341.getRelationship(o12340));

        // 1.2.3.4.1(0.0.0.0)
        // 1.2.3.4.0
        assertEquals(OID.cGreaterBranch, o12341_l.getRelationship(o12340));

        // 1.2.3.4.1(0.0.0.0)
        // 1.2.3.4.0(0.0.0.0)
        assertEquals(OID.cGreaterBranch, o12341_l.getRelationship(o12340_l));

        // 1.2.3.4.0
        // 1.2.3.4.1(0.0.0.0)
        assertEquals(OID.cLesserBranch, o12340.getRelationship(o12341_l));

        // 1.2.3.4.0(0.0.0.0
        // 1.2.3.4.1
        assertEquals(OID.cLesserBranch, o12340_l.getRelationship(o12341));

        // 1.2.3.4.0.0.0.0
        // 1.2.3.4.1.0.0.0
        assertEquals(OID.cLesserBranch, o12340_l.getRelationship(o12341_l));

        // private OID o12340000_l = new OID("1.2.3.4.0.0.0.0", true);
        // private OID o123400135 = new OID("1.2.3.4.0.0.1.3.5");
        // private OID o123405_l = new OID("1.2.3.4.0.5", true);
        // private OID o123400_l = new OID("1.2.3.4.0.0", true);
        // private OID o12340003_l = new OID("1.2.3.4.0.0.0.3", true);

        // 1.2.3.4.0.0.0.0(0.0.0.0)
        // 1.2.3.4.0(0.0.0.0.0.0.0)
        assertEquals(OID.cEqual, o12340000_l.getRelationship(o12340_l));

        // 1.2.3.4(0.0.0.0.0.0.0.0)
        // 1.2.3.4.0.0.1.3.5
        assertEquals(OID.cLesserBranch, o1234_l.getRelationship(o123400135));

        // 1.2.3.4.0.5(0.0.0.0.0.0)
        // 1.2.3.4.0.0.0.3(0.0.0.0)
        assertEquals(OID.cGreaterBranch, o123405_l.getRelationship(o12340003_l));

        assertEquals(OID.cParent, o12345.getRelationship(o12345000));
        assertEquals(OID.cChild, o12345_29_8.getRelationship(o12345));
        assertEquals(OID.cParent, o12340000.getRelationship(o12340_l));
        assertEquals(OID.cParent, o12340.getRelationship(o12340000_l));
        // 1.2.3.4.0(.0.0.0.0.0)
        // 1.2.3.4.0.0.1.3.5
        assertEquals(OID.cLesserBranch, o12340_l.getRelationship(o123400135));

        // 1.2.3.4.0.5(0.0.0.0)
        // 1.2.3.4.0
        assertEquals(OID.cChild, o123405_l.getRelationship(o12340));

        /*
         * branchCompareDepth branchCompareDepth | | V V 1.2.3.4.0.5 (l) :
         * 1.2.3.4.0 (l) == cGreaterBranch == 1.2.3.4.0.5(0.0.0.0....)
         * 1.2.3.4.0|0(0.0.0.0....)
         * 
         * 1.2.3.4.0.0 (l) : 1.2.3.4.0 (l) == cEqual == 1.2.3.4.0.0(0.0.0.0....)
         * 1.2.3.4.0|0(0.0.0.0....)
         * 
         * 1.2.3.4.0 (l) : 1.2.3.4.0.0.0.3 (l) == cLesserBranch ==
         * 1.2.3.4|0.0.0(0.0.0.0.0....) 1.2.3.4.0.0.3(0.0.0.0.0....)
         * 
         * 1.2.3.4.0.0.0.3 (l) : 1.2.3.4.0 (l) == cGreaterBranch ==
         * 1.2.3.4.0.0.0.3(0.0.0.0.0....) 1.2.3.4.0|0.0.0(0.0.0.0.0....)
         */
        assertEquals(OID.cGreaterBranch, o123405_l.getRelationship(o12340_l));
        assertEquals(OID.cEqual, o123400_l.getRelationship(o12340_l));
        assertEquals(OID.cLesserBranch, o12340_l.getRelationship(o12340003_l));
        assertEquals(OID.cGreaterBranch, o12340003_l.getRelationship(o12340_l));

    }

    // public void testIsChild()
    // {
    // assertTrue(OID.isChildOf(o12340, o1234));
    // assertFalse(OID.isChildOf(o1234, o12340));
    // assertFalse(OID.isChildOf(o1234, o1234));
    // assertFalse(OID.isChildOf(o12340, o12340));
    // assertTrue(OID.isChildOf(o12340, o12));
    // assertFalse(OID.isChildOf(o1234, otherBranch));
    // }
}
