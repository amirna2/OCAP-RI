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
import java.util.ArrayList;

import org.cablelabs.impl.snmp.OID;
import org.cablelabs.impl.snmp.OIDAmbiguityException;

import org.cablelabs.impl.manager.snmp.MIB;
import org.cablelabs.impl.manager.snmp.MIBRouterImpl;
import org.cablelabs.impl.manager.snmp.MIBValueAccess;
import org.cablelabs.impl.manager.snmp.MIBValueMap;
import org.cablelabs.impl.manager.snmp.OIDDelegationInfo;
import org.cablelabs.impl.manager.snmp.OIDDelegationListener;
import org.cablelabs.impl.manager.snmp.SNMPManagerImpl;

import org.cablelabs.impl.ocap.diagnostics.MIBDefinitionExt;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MIBRouterImplTest extends TestCase
{
    public static final String c123 = "1.2.3";

    public static final String c1232 = "1.2.3.2";

    public static final String c12310 = "1.2.3.1.0";

    public static final String c12300 = "1.2.3.0.0";

    public static final String c12410 = "1.2.4.1.0";

    public static final String c12400 = "1.2.4.0.0";

    public static OIDDelegationListener sharedListener = null;

    public MIBRouterImplTest(String name)
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

        suite.addTest(new MIBRouterImplTest("testRegisterOids"));
        suite.addTest(new MIBRouterImplTest("testGetDelegatedValues"));
        suite.addTest(new MIBRouterImplTest("testUnregisterOids"));
        suite.addTest(new MIBRouterImplTest("testAdd"));
        suite.addTest(new MIBRouterImplTest("testGetMIBValues")); // depends on
                                                                  // testAdd
        suite.addTest(new MIBRouterImplTest("testRemove")); // should be done
                                                            // just before
                                                            // testCompleted
        suite.addTest(new MIBRouterImplTest("testCompleted")); // restore
                                                               // everything,
                                                               // must be last
                                                               // test in suite

        return suite;
    }

    private OIDDelegationListener getSharedListener()
    {
        if (sharedListener == null)
        {
            //sharedListener = new DummyListener();
        }
        return sharedListener;
    }

    public void testGetDelegatedValues()
    {
        MIBRouterImpl router = getRouter();

        MIBDefinitionExt[] data = null;
        try
        {
            data = router.queryMIBRouter(c12300);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(false);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
        assertNotNull(data);
        assertTrue(data.length == 1);

        try
        {
            System.out.println("queryMIBRouter(c123)");
            data = router.queryMIBRouter(c123);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(false);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
        assertNotNull(data);
        assertTrue(data.length == 5); // three registered plus two from
                                      // properties MIB

    }

    public void testUnregisterOids()
    {
        MIBRouterImpl router = getRouter();

        MIBDefinitionExt[] data = null;
        try
        {
            data = router.queryMIBRouter(c12300);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(false);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }

        assertTrue(data.length == 1);

        router.unregisterOidDelegate(c12300);

        try
        {
            data = router.queryMIBRouter(c12300);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(false);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }
        assertTrue(data.length == 0);

    }

    public void testRegisterOids()
    {
        MIBRouterImpl router = getRouter();

        IllegalArgumentException iae = null;
        OIDAmbiguityException oae = null;

        try
        {
            router.registerOidDelegate(c12300, OIDDelegationInfo.DELEGATE_TYPE_LEAF, getSharedListener(), new OID(
                    c12300));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = null;
        }
        assertNull(iae);
        assertNull(oae);
        iae = null;
        oae = null;

        try
        {
            router.registerOidDelegate(c12310, OIDDelegationInfo.DELEGATE_TYPE_LEAF, getSharedListener(), new OID(
                    c12310));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = null;
        }
        assertNull(iae);
        assertNull(oae);
        iae = null;
        oae = null;

        try
        {
            router.registerOidDelegate(c1232, OIDDelegationInfo.DELEGATE_TYPE_TABLE, getSharedListener(),
                    new OID(c1232));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = e;
        }
        assertNull(iae);
        assertNull(oae);
        iae = null;
        oae = null;

        try
        {
            router.registerOidDelegate(c12400, OIDDelegationInfo.DELEGATE_TYPE_LEAF, getSharedListener(), new OID(
                    c12400));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = null;
        }
        assertNull(iae);
        assertNull(oae);
        iae = null;
        oae = null;

        try
        {
            router.registerOidDelegate(c12410, OIDDelegationInfo.DELEGATE_TYPE_LEAF, getSharedListener(), new OID(
                    c12410));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = null;
        }
        assertNull(iae);
        assertNull(oae);
        iae = null;
        oae = null;

        /*
         * test for failure
         * *****************************************************
         * *****************
         */
        try
        {
            router.registerOidDelegate(c12310, OIDDelegationInfo.DELEGATE_TYPE_LEAF, getSharedListener(), new OID(
                    c12310));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = e;
        }
        assertNull(iae);
        assertNotNull(oae); // should not allow the same registration twice
        iae = null;
        oae = null;

        /*
         * test for failure
         * *****************************************************
         * *****************
         */
        try
        {
            router.registerOidDelegate("1.2.3", OIDDelegationInfo.DELEGATE_TYPE_TREE, getSharedListener(), new OID(
                    "1.2.3"));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = e;
        }
        assertNull(iae);
        assertNotNull(oae); // should not allow a parent registration
        iae = null;
        oae = null;

        /*
         * test for failure
         * *****************************************************
         * *****************
         */
        try
        {
            router.registerOidDelegate("1.2.3.2.1", OIDDelegationInfo.DELEGATE_TYPE_TABLE, getSharedListener(),
                    new OID("1.2.3.2.1"));
        }
        catch (IllegalArgumentException e)
        {
            iae = e;
        }
        catch (OIDAmbiguityException e)
        {
            oae = e;
        }
        assertNull(iae);
        assertNotNull(oae); // should not allow a child registration
        iae = null;
        oae = null;

    }

    public void testAdd()
    {
        MIBRouterImpl router = getRouter();
        MIB mib = PropertiesMIBImpl.getInstance();

        if (router.isMIBAdded(mib)) return;

        addMib(router, mib);
    }

    private void addMib(MIBRouterImpl router, MIB mib)
    {
        try
        {
            router.addMIB(mib);
        }
        catch (IllegalArgumentException e)
        {
            assertFalse("got IllegalArgumentException", true);
        }
        catch (IllegalStateException e)
        {
            assertFalse("got IllegalStateException", true);
        }
        catch (OIDAmbiguityException e)
        {
            assertFalse("got OIDAmbiguityException", true);
        }
    }

    public void testRemove()
    {
        MIBRouterImpl router = getRouter();
        MIB mib = PropertiesMIBImpl.getInstance();

        if (!router.isMIBAdded(mib))
        {
            addMib(router, mib);
        }

        assertTrue(router.isMIBAdded(mib));

        router.removeMIB(mib);

        assertFalse(router.isMIBAdded(mib));

        String[] mibOids = mib.getOIDs();
        for (int i = 0; i < mibOids.length; i++)
        {
            String oid = mibOids[i];
            MIBDefinitionExt[] map = null;
            try
            {
                map = router.queryMIBRouter(oid);
            }
            catch (IllegalArgumentException e)
            {
                assertTrue(false);
            }
            catch (IOException e)
            {
                assertTrue(false);
            }
            assertTrue(map.length == 0);
        }
    }

    public void testGetMIBValues()
    {
        MIBRouterImpl router = getRouter();

        try
        {
            MIBDefinitionExt[] values = router.queryMIBRouter(PropertiesMIBImplTest.cOid1);
            //PropertiesMIBImplTest.testDefaultValueArray(PropertiesMIBImplTest.cOid1, values);

            values = router.queryMIBRouter(PropertiesMIBImplTest.cOid2);
            //PropertiesMIBImplTest.testDefaultValueArray(PropertiesMIBImplTest.cOid2, values);

            values = router.queryMIBRouter("100.100.100.100.0");
            assertNotNull(values);
            assertEquals(0, values.length);
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(false);
        }
        catch (IOException e)
        {
            assertTrue(false);
        }

    }

    public void testCompleted()
    {
        PropertiesMIBImpl.resetToDefaultProperties();
    }

    /*private class DummyListener implements OIDDelegationListener
    {

        public ArrayList notifyOidValueRequest(OIDDelegationInfo info)
        {
            // TODO, TODO_SNMP, remove println
            System.out.println("notifyOidValueRequest: oid = " + info.oid);

            ArrayList list = new ArrayList();

            MIBValueMap map = new MIBValueMap();
            map.oid = info.oid;
            map.type = MIBValueAccess.SNMP_TYPE_UNKNOWN;
            map.value = ASN1Helper.getDataFromString(info.oid);
            map.writable = MIBValueMap.VALUE_WRITABLE_UNKNOWN;

            list.add(map);

            return list;
        }

    }*/

    MIBRouterImpl getRouter()
    {
        SNMPManagerImpl mgr = (SNMPManagerImpl) SNMPManagerImpl.getInstance();
        return (MIBRouterImpl) mgr.getMibRouter();
    }

}
