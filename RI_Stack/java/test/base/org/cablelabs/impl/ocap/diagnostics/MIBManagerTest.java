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

/**
 * @author Alan Cohn
 */
package org.cablelabs.impl.ocap.diagnostics;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.diagnostics.MIBListener;
import org.ocap.diagnostics.MIBManager;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;
import org.ocap.diagnostics.MIBObject;

import org.cablelabs.impl.manager.snmp.OIDDelegationInfo;
import org.cablelabs.impl.manager.snmp.OIDDelegationListener;
import org.cablelabs.impl.manager.snmp.MIBValueMap;

import java.util.ArrayList;

public class MIBManagerTest extends TestCase implements MIBListener
{
    private MIBManager mibm = null;

    private final static String oid1234 = "1.2.3.4";

    private final static String oid123410 = "1.2.3.4.1.0";

    private final static String oid123420 = "1.2.3.4.2.0";

    private final static String oid1235 = "1.2.3.5"; // testSNMPOid

    private final static String oid1235110 = "1.2.3.5.1.1.0"; // testSetMIBObject

    private final static String oid1238 = "1.2.3.8";

    private final static String oid1237 = "1.2.3.7";

    private final static String oid12371 = "1.2.3.7.1";

    private final static String oid12372 = "1.2.3.7.2";

    private final static String oidBad = "1.2.3.4.a.0";

    private final static String scfsr = "SNMP_CHECK_FOR_SET_REQUEST";

    private final static String ssr = "SNMP_SET_REQUEST";

    private final static String sgr = "SNMP_GET_REQUEST";

    private final static String sgnr = "SNMP_GET_NEXT_REQUEST";

    private static byte[] ba123410 = { 0, 0, 0, 1 };

    private static byte[] ba123420 = { 0, 0, 0, 2 };

    private static byte[] baNew = { 1, 2, 3, 4 };

    private static byte[] ba12371 = { 0, 0, 7, 1 };

    private static byte[] ba12372 = { 0, 0, 7, 2 };

    public MIBManagerTest(String name)
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
        suite.addTest(new MIBManagerTest("testInstanceMIBManager"));
        suite.addTest(new MIBManagerTest("testRegisterOid"));
        suite.addTest(new MIBManagerTest("testUnRegisterOid"));
        suite.addTest(new MIBManagerTest("testExceptions"));
        suite.addTest(new MIBManagerTest("testSetMIBObject"));
        suite.addTest(new MIBManagerTest("testqueryMIBrow"));
        suite.addTest(new MIBManagerTest("testSNMPOid"));
        suite.addTest(new MIBManagerTest("testNotifyOidValueRequest"));
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /*************************************************************
     * Actual tests start here
     ************************************************************/
    public void testInstanceMIBManager()
    {
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
                assertNotNull("MIBManager getInstance is null", mibm);
            }
            catch (SecurityException e)
            {
                fail("MIBManager getInstance throws SecurityException");
            }
        }
    }

    public void testRegisterOid()
    {
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
            }
            catch (SecurityException e)
            {
                fail("Security Exception");
            }
        }

        assertNotNull("MIBManager getInstance is null", mibm);
        if (mibm == null) return;

        // register mib 1.2.3.4.1.0
        try
        {
            mibm.registerOID(oid123410, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            fail("RegisterOID IllegalArgumentException " + e);
            return;
        }

        // register mib 1.2.3.4.2.0
        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            fail("RegisterOID IllegalArgumentException " + e);
            return;
        }

        MIBDefinition[] mibd;

        // query for parent mib 1.2.3.4
        mibd = mibm.queryMibs(oid1234);
        // check for 2 mib found
        assertNotNull("MIBManager queryMibs 1.2.3.4 returned null", mibm);
        if (mibd == null) return;

        assertTrue("MIBManager queryMibs 1.2.3.4 returned array length expected 2 = " + mibd.length, mibd.length == 2);
        if (mibd.length != 2) return;

        // query for mib 1.2.3.4.1.0
        mibd = mibm.queryMibs(oid123410);

        // check for 1 mib found
        assertNotNull("MIBManager queryMibs 1.2.3.4.1.0 returned null", mibm);
        if (mibd == null) return;

        assertTrue("queryMibs returned array length not 1 = " + mibd.length, mibd.length == 1);
        if (mibd.length != 1) return;

        assertTrue("queryMibs 1.2.3.4.1.0 data type not integer (2) = " + mibd[0].getDataType(),
                mibd[0].getDataType() == MIBDefinition.SNMP_TYPE_INTEGER);

        MIBObject mo = mibd[0].getMIBObject();
        assertNotNull("MIBManager queryMibs 1.2.3.4.1.0 MIBObject is null", mibm);
        if (mo == null) return;

        assertTrue("MIBObject oid not " + oid123410 + "is " + mo.getOID(), mo.getOID().equals(oid123410));

        byte[] ba = mo.getData();
        assertNotNull("MIBObject data is null", ba);
        if (ba == null) return;

        assertTrue("MIBObject data length is not " + ba123410.length + " is " + ba.length, ba.length == ba123410.length);

        for (int x = 0; x < ba.length; x++)
            assertTrue("MIBObject data entry value not as expected", ba[x] == ba123410[x]);

        try
        {
            mibm.unregisterOID(oid123420);
        }
        catch (IllegalArgumentException e)
        {
        };// do nothing

        try
        {
            mibm.unregisterOID(oid123410);
        }
        catch (IllegalArgumentException e)
        {
        };// do nothing
    }

    public void testUnRegisterOid()
    {
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
            }
            catch (SecurityException e)
            {
                assertTrue("Security Exception", false);
            }
        }

        assertNotNull("MIBManager getInstance is null", mibm);
        if (mibm == null) return;

        // unregister OIDs in case they are registered, ignore any errors.
        try
        {
            mibm.unregisterOID(oid123410);
        }
        catch (IllegalArgumentException e)
        {
        };// do nothing

        try
        {
            mibm.unregisterOID(oid123420);
        }
        catch (IllegalArgumentException e)
        {
        };// do nothing

        // register mib 1.2.3.4.1.0
        try
        {
            mibm.registerOID(oid123410, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            fail("RegisterOID IllegalArgumentException " + e);
            return;
        }

        // register mib 1.2.3.4.2.0
        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            fail("RegisterOID IllegalArgumentException " + e);
            return;
        }

        MIBDefinition[] mibdef;
        // unregister 1.2.3.4.2.0
        try
        {
            mibm.unregisterOID(oid123420);
        }
        catch (IllegalArgumentException e)
        {
            fail("RegisterOID IllegalArgumentException " + e);
        };// do nothing

        // query for mib 1.2.3.4.1.0
        mibdef = mibm.queryMibs(oid123410);
        assertNotNull("queryMibs 1.2.3.4.1.0 did not return not-null", mibdef);
        if (mibdef != null)
            assertTrue("queryMibs 1.2.3.4.1.0 length should be 1 is " + mibdef.length, mibdef.length == 1);

        // query for mib 1.2.3.4.2.0
        mibdef = mibm.queryMibs(oid123420);
        assertNotNull("queryMibs 1.2.3.4.2.0 did not return not-null", mibdef);
        if (mibdef != null)
            assertTrue("queryMibs 1.2.3.4.2.0 length should be 0 is " + mibdef.length, mibdef.length == 0);

        // unregister 1.2.3.4.1.0
        try
        {
            mibm.unregisterOID(oid123410);
        }
        catch (IllegalArgumentException e)
        {
            fail("RegisterOID IllegalArgumentException " + e);
        };// do nothing

        // query for mib 1.2.3.4.1.0
        mibdef = mibm.queryMibs(oid123410);
        assertNotNull("queryMibs 1.2.3.4.1.0 did not return not-null", mibdef);
        if (mibdef != null)
            assertTrue("queryMibs 1.2.3.4.1.0 length should be 0 is " + mibdef.length, mibdef.length == 0);

    }

    public void testExceptions()
    {
        boolean eTest;
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
            }
            catch (SecurityException e)
            {
                fail("Security Exception");
            }
        }

        assertNotNull("MIBManager getInstance is null", mibm);
        if (mibm == null) return;

        // bad oid
        eTest = false;
        try
        {
            mibm.registerOID(oidBad, // bad oid
                    MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertTrue("Bad MIB OID value not caught", eTest);

        // bad access value
        eTest = false;
        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE + 100, // bad
                                                                               // value
                    true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertTrue("Bad MIB Access value not caught", eTest);

        // bad leaf
        eTest = false;
        try
        {
            mibm.registerOID(oid1234, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                                                                             // bad
                                                                             // value
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertTrue("Bad MIB Leaf value not caught", eTest);

        // bad type
        eTest = false;
        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER + 0x100, // bad value
                    this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertTrue("Bad MIB Definition value not caught", eTest);

        // bad call back for notifySNMPRequest
        eTest = false;
        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, null); // notifySNMPRequest
                                                            // bad value
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertTrue("Bad MIB notifySNMPRequest value not caught", eTest);

        // register good OID then try to register it again
        eTest = false;
        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertFalse("Good MIB exception caught", eTest);

        // try to register same OID
        eTest = false;
        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertTrue("Register same OID exception not caught", eTest);

        // try to unregister same OID twice
        eTest = false;
        try
        {
            mibm.unregisterOID(oid123420);
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            mibm.unregisterOID(oid123420);
        }
        catch (IllegalArgumentException e)
        {
            eTest = true;
        }

        assertTrue("UnRegister unknown OID exception not caught", eTest);
    }

    /*
     * set new value for registered OID 123410 and unregistered OID 1235110 The
     * oid1235 values are defined in file propertiesMib.props
     * 1.2.3.5.0.0=2:true:\u0003\u0005\u0000\u0000
     * 1.2.3.5.1.0=2:true:\u0003\u0005\u0001\u0000
     * 1.2.3.5.1.1.0=2:true:\u0003\u0005\u0001\u0001
     * 1.2.3.5.1.2.0=2:true:\u0003\u0005\u0001\u0002
     * 1.2.3.5.2.0=2:true:\u0003\u0005\u0002\u0000
     * 1.2.3.5.3.0=2:true:\u0003\u0005\u0003\u0000
     */
    public void testSetMIBObject()
    {
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
            }
            catch (SecurityException e)
            {
                fail("Security Exception");
            }
        }

        assertNotNull("MIBManager getInstance is null", mibm);
        if (mibm == null) return;

        try
        {
            mibm.registerOID(oid123410, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
        }

        MIBObject mibToSet = new MIBObject(oid123410, baNew);

        // call MIBManager
        mibm.setMIBObject(mibToSet);

        assertTrue("testSetMIBObject lengths not same " + ba123410.length + " vs " + baNew.length,
                ba123410.length == baNew.length);

        if (ba123410.length == baNew.length)
            for (int x = 0; x < ba123410.length; x++)
            {
                assertTrue("testSetMIBObject entry not same [" + x + "] " + ba123410[x] + " " + baNew[x],
                        ba123410[x] == baNew[x]);
            }

        mibm.unregisterOID(oid123410);

        // attempt to set an unregistered OID that's defined in
        // propertiesMib.props
        mibToSet = new MIBObject(oid1235110, baNew);

        // call MIBManager
        mibm.setMIBObject(mibToSet);

        // retrieve the OID with new value
        MIBDefinition[] mibd = mibm.queryMibs(oid1235110);

        assertTrue("Query oid " + oid1235110 + "not length 1 is " + mibd.length, mibd.length == 1);
        if (mibd.length != 1) return;

        byte[] bdata = mibd[0].getMIBObject().getData();

        assertTrue("testSetMIBObject lengths not same " + bdata.length + " vs " + baNew.length,
                bdata.length == baNew.length);

        if (bdata.length == baNew.length)
            for (int x = 0; x < bdata.length; x++)
            {
                assertTrue("testSetMIBObject entry not same [" + x + "] " + bdata[x] + " " + baNew[x],
                        bdata[x] == baNew[x]);
            }
    }

    // test that a register oid of 1.2.3.7 will return oids 1.2.3.7.1 and
    // 1.2.3.7.2 for queryMibs()
    public void testqueryMIBrow()
    {
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
                assertNotNull("MIBManager getInstance is null", mibm);
            }
            catch (SecurityException e)
            {
                fail("MIBManager getInstance throws SecurityException");
            }
        }

        if (mibm == null) return;

        try
        {
            mibm.registerOID(oid1237, MIBManager.MIB_ACCESS_READWRITE, false, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
        }

        MIBDefinition[] mibd = mibm.queryMibs(oid1237);

        assertTrue("testqueryMIBrow length not 2 is " + mibd.length, mibd.length == 2);
        if (mibd.length != 2)
        {
            mibm.unregisterOID(oid1237);
            return;
        }

        assertTrue("testqueryMIBrow first string not " + oid12371 + " is " + mibd[0].getMIBObject().getOID(),
                mibd[0].getMIBObject().getOID().equalsIgnoreCase(oid12371));

        assertTrue("testqueryMIBrow second string not " + oid12372 + " is " + mibd[1].getMIBObject().getOID(),
                mibd[1].getMIBObject().getOID().equalsIgnoreCase(oid12372));

        mibm.unregisterOID(oid1237);
    }

    /*
     * The oid1235 and oid1238 values are defined in file propertiesMib.props
     * 1.2.3.5.0.0=2:true:\u0003\u0005\u0000\u0000
     * 1.2.3.5.1.0=2:true:\u0003\u0005\u0001\u0000
     * 1.2.3.5.1.1.0=2:true:\u0003\u0005\u0001\u0001
     * 1.2.3.5.1.2.0=2:true:\u0003\u0005\u0001\u0002
     * 1.2.3.5.2.0=2:true:\u0003\u0005\u0002\u0000
     * 1.2.3.5.3.0=2:true:\u0003\u0005\u0003\u0000 1.2.3.8.1.0=4:false:This is a
     * string. 1.2.3.8.2.0=4:false:This is another string.
     */
    public void testSNMPOid()
    {
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
                assertNotNull("MIBManager getInstance is null", mibm);
            }
            catch (SecurityException e)
            {
                fail("MIBManager getInstance throws SecurityException");
            }
        }

        if (mibm == null) return;

        MIBDefinition[] mibdef;
        // query for mib oid1235
        mibdef = mibm.queryMibs(oid1235);
        assertNotNull("queryMibs " + oid1235 + " did not return not-null", mibdef);
        if (mibdef != null)
        {
            int expectedLength = 6;
            assertTrue("queryMibs " + oid1235 + " length should be " + expectedLength + " is " + mibdef.length,
                    mibdef.length == expectedLength);
            for (int x = 0; x < mibdef.length; x++)
            {
                assertTrue("Data type not " + MIBDefinition.SNMP_TYPE_INTEGER + " is " + mibdef[x].getDataType(),
                        mibdef[x].getDataType() == MIBDefinition.SNMP_TYPE_INTEGER);

                MIBObject mobj = mibdef[x].getMIBObject();
                byte[] bData = mobj.getData();
                assertTrue("MIBObject data not 4 bytes is " + bData.length, bData.length == 4);
            }
        }

        // query for strings with oid1238
        mibdef = mibm.queryMibs(oid1238);
        assertNotNull("queryMibs " + oid1238 + " did not return not-null", mibdef);
        if (mibdef != null)
        {
            int expectedLength = 2;
            assertTrue("queryMibs " + oid1238 + " length should be " + expectedLength + " is " + mibdef.length,
                    mibdef.length == expectedLength);

            System.out.println("TestSNMPOid returned strings:");
            for (int x = 0; x < mibdef.length; x++)
            {
                assertTrue("Data type not " + MIBDefinition.SNMP_TYPE_OCTETSTRING + " is " + mibdef[x].getDataType(),
                        mibdef[x].getDataType() == MIBDefinition.SNMP_TYPE_OCTETSTRING);

                // display the strings
                MIBObject mobj = mibdef[x].getMIBObject();
                byte[] bData = mobj.getData();

                for (int y = 0; y < bData.length; y++)
                    System.out.print((char) bData[y]);
                System.out.println();
            }
            System.out.println("TestSNMPOid end of strings.");
        }
    }

    public void testNotifyOidValueRequest()
    {
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();
                assertNotNull("MIBManager getInstance is null", mibm);
            }
            catch (SecurityException e)
            {
                fail("MIBManager getInstance throws SecurityException");
            }
        }

        if (mibm == null) return;

        try
        {
            mibm.registerOID(oid123420, MIBManager.MIB_ACCESS_READWRITE, true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, this); // notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
        } // ignore

        // access internal method
        MIBManagerImpl mibmi = (MIBManagerImpl) mibm;

        MIBListenerData mibldata = mibmi.getRegisteredOids(oid123420);

        // OIDDelegationInfo(String oid, int delegateType, int reqType, byte[]
        // setValue, OIDDelegationListener listener, Object userObj)
        OIDDelegationInfo info = new OIDDelegationInfo(oid123420, OIDDelegationInfo.DELEGATE_TYPE_TABLE,
                SNMPRequest.SNMP_GET_REQUEST, new byte[0], null, mibldata);

        // call method test
        ArrayList alist = null; //mibmi.notifyOidValueRequest(info);

        assertNotNull("testNotifyOidValueRequest byte array is null", alist);
        if (alist == null) return;

        assertTrue("testNotifyOidValueRequest return array size is not 1", alist.size() == 1);
        if (alist.size() != 1) return;

        // convert to an array of MIBValueMap
        MIBValueMap[] ba = new MIBValueMap[alist.size()];
        alist.toArray(ba);

        assertTrue("testNotifyOidValueRequest byte array length not " + ba123420.length,
                ba[0].value.length == ba123420.length);
        if (ba[0].value.length != ba123420.length) return;

        for (int x = 0; x < ba123420.length; x++)
        {
            assertTrue("returned array not equal byte [" + x + "] " + ba[0].value[x] + " SB " + ba123420[x],
                    ba[0].value[x] == ba123420[x]);
        }
        mibm.unregisterOID(oid123420);
    }

    /*
     * Callback method for registerOID (non-Javadoc)
     * 
     * @see
     * org.ocap.diagnostics.MIBListener#notifySNMPRequest(org.ocap.diagnostics
     * .SNMPRequest)
     */
    public SNMPResponse notifySNMPRequest(SNMPRequest request)
    {
        int reqtype = request.getRequestType();
        MIBObject moIn = request.getMIBObject();
        String oid = moIn.getOID();
        byte[] barray = moIn.getData();
        MIBObject moOut = null;
        String rtstr;

        switch (reqtype)
        {
            case SNMPRequest.SNMP_GET_REQUEST:
                rtstr = sgr;
                break;
            case SNMPRequest.SNMP_GET_NEXT_REQUEST:
                rtstr = sgnr;
                break;
            case SNMPRequest.SNMP_CHECK_FOR_SET_REQUEST:
                rtstr = scfsr;
                break;
            case SNMPRequest.SNMP_SET_REQUEST:
                rtstr = ssr;
                break;
            default:
                rtstr = "Unknown Request Type";
                break;
        }
        System.out.println("In MIBManagerTest notifySNMPRequest for oid " + oid + " request type " + rtstr);

        if (0 == oid.indexOf(oid1237))
        {
            if (oid.endsWith(oid1237))
            {
                moOut = new MIBObject(oid12371, ba12371);
                SNMPResponse sr = new SNMPResponse(SNMPResponse.SNMP_REQUEST_SUCCESS, moOut);
                return sr;
            }
            else if (oid.endsWith(oid12371))
            {
                moOut = new MIBObject(oid12372, ba12372);
                SNMPResponse sr = new SNMPResponse(SNMPResponse.SNMP_REQUEST_SUCCESS, moOut);
                return sr;
            }
            else if (oid.endsWith(oid12372))
            {
                moOut = new MIBObject(oid12372, ba12372);
                SNMPResponse sr = new SNMPResponse(SNMPResponse.SNMP_REQUEST_NO_SUCH_NAME, moOut);
                return sr;
            }
            else
            {

            }
        }

        if (reqtype == SNMPRequest.SNMP_GET_REQUEST || reqtype == SNMPRequest.SNMP_GET_NEXT_REQUEST)
        {
            if (oid.equals(oid123410) || oid.equals(oid1234))
            {
                moOut = new MIBObject(oid, ba123410);
            }
            else
            {
                moOut = new MIBObject(oid, ba123420);
            }

        }
        else if (reqtype == SNMPRequest.SNMP_SET_REQUEST)
        {
            System.out.println("In MIBManagerTest notifySNMPRequest SNMP_SET_REQUEST");
            System.out.println("Data length = " + barray.length);
            for (int x = 0; x < barray.length; x++)
                System.out.println("  Data [" + x + "] = " + barray[x]);

            if (oid.equals(oid123410))
            {
                System.arraycopy(barray, 0, ba123410, 0, ba123410.length);
                moOut = new MIBObject(oid, ba123410);
            }
            else
            {
                System.arraycopy(barray, 0, ba123420, 0, ba123420.length);
                moOut = new MIBObject(oid, ba123420);
            }
        }
        else
        { // SNMP_CHECK_FOR_SET_REQUEST - return what the new value would be if
          // changed.
            if (oid.equals(oid123410))
            {
                moOut = new MIBObject(oid, barray);
            }
            else
            {
                moOut = new MIBObject(oid, barray);
            }
        }

        SNMPResponse sr = new SNMPResponse(SNMPResponse.SNMP_REQUEST_SUCCESS, moOut);
        return sr;
    }
}
