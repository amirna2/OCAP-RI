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

import java.io.IOException;

import org.cablelabs.impl.snmp.PropertiesMIBImpl;

import org.cablelabs.impl.manager.snmp.MIBValueAccess;
import org.cablelabs.impl.manager.snmp.MIBValueMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PropertiesMIBImplTest extends TestCase
{
    public static final String cOid1 = "1.2.3.4.15";

    public static final byte[] cOid1Def = { 1, 2, 3, 4, 15 };

    public static final String cOid2 = "1.2.3.4.16";

    public static final byte[] cOid2Def = { 1, 2, 3, 4, 16 };

    public PropertiesMIBImplTest(String name)
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

        suite.addTest(new PropertiesMIBImplTest("testLoad"));
        suite.addTest(new PropertiesMIBImplTest("testSet"));
        suite.addTest(new PropertiesMIBImplTest("testGetOids"));
        suite.addTest(new PropertiesMIBImplTest("testCompleted")); // must be
                                                                   // run last
        return suite;
    }

    public void testCompleted() throws Exception
    {
        PropertiesMIBImpl.resetToDefaultProperties();
    }

    public void testLoad()
    {
        PropertiesMIBImpl mib = (PropertiesMIBImpl) PropertiesMIBImpl.getInstance();

        MIBValueMap[] mapArray = mib.getPropertiesMIBValues(cOid1);
        testDefaultValueArray(cOid1, mapArray);

        mapArray = mib.getPropertiesMIBValues(cOid2);
        testDefaultValueArray(cOid2, mapArray);

        mapArray = mib.getPropertiesMIBValues("1.2.3.4.299");
        assertEquals(0, mapArray.length);
    }

    public static void testDefaultValueArray(String oid, MIBValueMap[] mapArray)
    {
        assertEquals(1, mapArray.length);
        assertTrue(mapArray[0].oid.equals(oid));
        assertTrue(mapArray[0].writable == MIBValueMap.VALUE_WRITABLE_TRUE);
        assertEquals(MIBValueAccess.SNMP_TYPE_INTEGER, mapArray[0].type);
    }

    public void testSet()
    {
        PropertiesMIBImpl mib = (PropertiesMIBImpl) PropertiesMIBImpl.getInstance();

        byte[] newValue = new byte[1];
        newValue[0] = 0;

        //IOException ioe = null;
        IllegalArgumentException iae = null;
        try
        {
            mib.setMIBValue(cOid1, newValue);
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        //catch (IOException e)
        //{
        //    ioe = e;
        //}
        assertNull(iae);
        //assertNull(ioe);

        iae = null;
        //ioe = null;

        MIBValueMap[] mapArray = mib.getPropertiesMIBValues(cOid1);
        assertEquals(1, mapArray.length);
        assertTrue(mapArray[0].oid.equals(cOid1));
        assertTrue(mapArray[0].writable == MIBValueMap.VALUE_WRITABLE_TRUE);
        assertEquals(MIBValueAccess.SNMP_TYPE_INTEGER, mapArray[0].type);
        assertEquals(0, mapArray[0].value[0]);
    }

    public void testGetOids()
    {
        PropertiesMIBImpl mib = (PropertiesMIBImpl) PropertiesMIBImpl.getInstance();

        String[] oids = mib.getOIDs();

        assertTrue(oids.length >= 2);
    }

}
