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

import java.util.SortedMap;

import org.cablelabs.impl.snmp.OID;
import org.cablelabs.impl.snmp.OIDMap;
import org.cablelabs.impl.snmp.OIDAmbiguityException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class OIDMapTest extends TestCase
{
    OID oid012 = new OID("0.1.2");

    OID oid02 = new OID("0.2");

    OID oid022 = new OID("0.2.2");

    OID oid123 = new OID("1.2.3");

    OID oid1234 = new OID("1.2.3.4");

    OID oid12341 = new OID("1.2.3.4.1");

    OID oid12342 = new OID("1.2.3.4.2");

    OID oid123421 = new OID("1.2.3.4.2.1");

    OID oid1235 = new OID("1.2.3.5");

    OID oid124 = new OID("1.2.4");

    OID oid234 = new OID("2.3.4");

    OID oid235 = new OID("2.3.5");

    OID oid2351 = new OID("2.3.5.1");

    public OIDMapTest(String name)
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

        suite.addTest(new OIDMapTest("testOverlappedAccess"));
        suite.addTest(new OIDMapTest("testGetAncestors"));
        suite.addTest(new OIDMapTest("testGetChildern"));
        suite.addTest(new OIDMapTest("testHasParent"));
        suite.addTest(new OIDMapTest("testHasChildern"));
        suite.addTest(new OIDMapTest("testAmbiguityPrevention"));

        return suite;
    }

    public void testOverlappedAccess()
    {
        OIDMap map = createAmbiguousMap();

        OID temp = (OID) map.get(oid234);
        assertEquals(oid234.getString(), temp.getString());

        temp = (OID) map.get(oid12341);
        assertEquals(oid12341.getString(), temp.getString());

        temp = (OID) map.get(oid1234);
        assertEquals(oid1234.getString(), temp.getString());
    }

    public void testGetAncestors()
    {
        OIDMap map = createAmbiguousMap();

        SortedMap subTree = map.getAncestors(new OID("1.2.3.4")); // oid1234

        int size = subTree.size();
        assertEquals(2, size);
        assertTrue(subTree.get(oid123).equals(oid123));
        assertTrue(subTree.get(oid124).equals(oid124));

        subTree = map.getAncestors(oid123);
        size = subTree.size();
        assertEquals(0, size);

        subTree = map.getAncestors(oid012);
        size = subTree.size();
        assertEquals(2, size);
        assertTrue(subTree.get(oid02).equals(oid02));
        assertTrue(subTree.get(oid022).equals(oid022));
    }

    public void testGetChildern()
    {
        OIDMap map = createAmbiguousMap();

        SortedMap subTree = map.getChildren(new OID("1.2.3.4")); // oid1234

        int size = subTree.size();
        assertEquals(3, size);

        assertTrue(subTree.get(oid12341).equals(oid12341));
        assertTrue(subTree.get(oid12342).equals(oid12342));
        assertTrue(subTree.get(oid123421).equals(oid123421));

        subTree = map.getChildren(new OID("2.3.5.1"));
        size = subTree.size();
        assertEquals(0, size);
    }

    public void testHasChildern()
    {
        OIDMap map = createAmbiguousMap();

        boolean descendents = map.hasChildren(oid1234);
        assertTrue(descendents);

        descendents = map.hasChildren(oid235);
        assertTrue(descendents);

        descendents = map.hasChildren(oid012);
        assertFalse(descendents);

        descendents = map.hasChildren(oid2351);
        assertFalse(descendents);

    }

    public void testHasParent()
    {
        OIDMap map = createAmbiguousMap();

        boolean parent = map.hasParent(oid1234);
        assertTrue(parent);

        parent = map.hasParent(oid012);
        assertFalse(parent);

        parent = map.hasParent(oid234);
        assertFalse(parent);
    }

    public void testAmbiguityPrevention()
    {

        OIDMap map = new OIDMap(true); // prevent ambiguities

        IllegalArgumentException iae = null;
        OIDAmbiguityException oae = null;

        try
        {
            map.put(oid12341, oid12341);
        }
        catch (IllegalArgumentException e1)
        {
            iae = e1;
        }
        catch (OIDAmbiguityException e1)
        {
            oae = e1;
        }
        assertNull(iae);
        assertNull(oae);
        iae = null;
        oae = null;

        try
        {
            map.put(oid123, oid123);
        }
        catch (IllegalArgumentException e1)
        {
            iae = e1;
        }
        catch (OIDAmbiguityException e1)
        {
            oae = e1;
        }
        assertNull(iae);
        assertNotNull(oae);
        iae = null;
        oae = null;

        /********************/
        try
        {
            map.put(oid235, oid235);
        }
        catch (IllegalArgumentException e1)
        {
            iae = e1;
        }
        catch (OIDAmbiguityException e1)
        {
            oae = e1;
        }
        assertNull(iae);
        assertNull(oae);
        iae = null;
        oae = null;

        try
        {
            map.put(oid2351, oid2351);
        }
        catch (IllegalArgumentException e1)
        {
            iae = e1;
        }
        catch (OIDAmbiguityException e1)
        {
            oae = e1;
        }
        assertNull(iae);
        assertNotNull(oae);
        iae = null;
        oae = null;

    }

    private OIDMap createAmbiguousMap()
    {
        OIDMap map = new OIDMap(false); // don't prevent ambiguities

        try
        {
            map.put(oid012, oid012);
            map.put(oid02, oid02);
            map.put(oid022, oid022);
            map.put(oid12341, oid12341);
            map.put(oid123, oid123);
            map.put(oid1234, oid1234);
            map.put(oid12342, oid12342);
            map.put(oid123421, oid123421);
            map.put(oid1235, oid1235);
            map.put(oid124, oid124);
            map.put(oid234, oid234);
            map.put(oid235, oid235);
            map.put(oid2351, oid2351);

        }
        catch (IllegalArgumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (OIDAmbiguityException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return map;
    }

}
