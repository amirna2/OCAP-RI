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

package org.cablelabs.xlet.snmp;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.ocap.diagnostics.MIBListener;
import org.ocap.diagnostics.MIBManager;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;
import org.ocap.diagnostics.MIBObject;

import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.InetAddress;

/**
 * SNMP Tests This Xlet tests a number of SNMP related method calls and can be used for testing SNMP features from an application.
 *
 */
public class SNMPXlet implements Xlet, MIBListener
{
    private XletContext ctx;

    private boolean started;

    private MIBManager mibm = null;

    private static byte[] defaultMibData = { 0, 0, 0, 0 };

    private final static String OID_FOR_REGISTERING    = "1.2.3.4.1.0"; // Made up oid for registering

    private final static String OID_NO_SUCH_OBJECT   = "1.3.6.1.2.1.25.1.2.0"; // NO SUCH OBJECT leaf
    private final static String OID_NO_SUCH_BRANCH   = "1.3.6.1.2.1.25.1.2"; // NO SUCH OBJECT branch
    private final static String OID_NON_EXISTANT   = "9.9.9.9.9.1"; // Made up oid that does NOT exist

    private static final String OID_STACK_AND_PLATFORM_BASE_OID = "1.3.6.1.4.1.4491.2.3.1.1";
    private static final String OID_STACK_TOTAL_MEMORY_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.8.1.0";
    
    // Platform OID for system descriptor, this is sure to be available from a daemon Net-SNMP     
    private final static String OID_PLATFORM_SysDescr = "1.3.6.1.2.1.1.1.0";     

    private static byte[] test1MibData = { 0x02, 0x04, 0x01, 0x02, 0x03, 0x04 }; //T = SNMP_TYPE_INTEGER 02, L= 04, V= 0x01020304 (16909060)
    private static int TEST3_ExpectedNumOids = 13; //Least number of OIDs expected when querying OID_STACK_AND_PLATFORM_BASE_OID

    private static final int ASN1_POS_TYPE = 0;   // An ASN.1 TLV position of the Type byte
    private static final int ASN1_POS_LENGTH = 1; // An ASN.1 TLV position of the Length byte
    
    private static final int FULL_ASN1_LENGTH_INTEGER = 6; // Length of an ASN.1 representation of integer with type and length
    private static final int PARTIAL_ASN1_LENGTH_INTEGER = 4; // Length of an ASN.1 representation of integer without type or length. 

    private static final int FULL_ASN1_LENGTH_OID_PLATFORM_SysDescr = 256+4; //Expected length of String with Type length
    private static final int PARTIAL_ASN1_LENGTH_OID_PLATFORM_SysDescr = 256; //Expected length of string without type length
    
    private final static String SNMP_CHECK_FOR_SET_REQUEST = "SNMP_CHECK_FOR_SET_REQUEST";
    private final static String SNMP_SET_REQUEST = "SNMP_SET_REQUEST";
    private final static String SNMP_GET_REQUEST = "SNMP_GET_REQUEST";
    private final static String SNMP_GET_NEXT_REQUEST = "SNMP_GET_NEXT_REQUEST";
    private final static String SNMP_UNKNOWN_REQUEST = "Unknown Request Type";


    /**
     * Implements {@link Xlet#initXlet}.
     */
    public void initXlet(javax.tv.xlet.XletContext xc)
    {
        this.ctx = xc;
    }

    /**
     * Implements {@link Xlet#startXlet}. Will display the gui.
     */
    public void startXlet()
    {
        if (!started)
        {
            initialize();
            started = true;
        }
    }

    /**
     * Implements {@link Xlet#pauseXlet}. Will hide the gui.
     */
    public void pauseXlet()
    {
    }

    /**
     * Implements {@link Xlet#destroyXlet}. Will hide the gui and cleanup.
     */
    public void destroyXlet(boolean forced) throws XletStateChangeException
    {
        if (!forced) throw new XletStateChangeException("Don't want to go away");

    }

    /**
     * Initializes the GUI.
     */
    private void initialize()
    {

        // initialize the MIB Manager and test
        if (mibm == null)
        {
            try
            {
                mibm = MIBManager.getInstance();

                // Perform the tests

                doTest1();
                doTest2();
                doTest3();
                doTest4();
                doTest5();
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Use existing interface to register a simple leaf MIB, then Get a value from it.
     */
    private void doTest1()
    {
        String testResult = "";
        
        MIBDefinition[] mibd;
        testResult = "FAIL"; //default condition
        boolean throughLoop;
        System.out.println("SNMPXlet:doTest1: START_TEST:Register a MIB and get a value from it");
        try
        {
            // Register the OID
            mibm.registerOID(OID_FOR_REGISTERING,
                             MIBManager.MIB_ACCESS_READWRITE,
                             true, //leaf
                             MIBDefinition.SNMP_TYPE_INTEGER,
                             this);

            // Do a "Get" on this OID leaf
            mibd = mibm.queryMibs(OID_FOR_REGISTERING);

            if ( (mibd != null) &&
                 (mibd[0].getDataType() == MIBDefinition.SNMP_TYPE_INTEGER))
            {
                MIBObject mo = mibd[0].getMIBObject();
                if (mo == null)
                {
                    System.out.println("SNMPXlet:doTest1:FAIL mo is null");
                }
                else if (!mo.getOID().equals(OID_FOR_REGISTERING))
                {
                    System.out.println("SNMPXlet:doTest1:FAIL mo.getOID=" + mo.getOID());
                }
                else
                {
                    byte[] ba = mo.getData();
                    if (ba.length != test1MibData.length)
                    {
                        System.out.println("SNMPXlet:doTest1:FAIL Wrong length of data");
                    }
                    else
                    {
                        throughLoop = true;
                        for (int x = 0; x < ba.length; x++)
                        {
                            if (ba[x] != test1MibData[x])
                            {
                                System.out.println("SNMPXlet:doTest1:FAIL data wrong in byte array ");
                                throughLoop = false;
                                break;
                            }
                        }
                        if (throughLoop == true)
                        {
                            testResult = "PASS"; 
                        }
                    }
                }

            }
            else
            {
                System.out.println("SNMPXlet:doTest1: Unexpected MibDefinition returned, null or wrong type");
            }

            // Unregister the OID

            try
            {
                mibm.unregisterOID(OID_FOR_REGISTERING);
            }
            catch (IllegalArgumentException e)
            {
                System.out.println("SNMPXlet:doTest1: unregisterOID caused IllegalArgumentException");
            }

        }
        catch (IllegalArgumentException e)
        {
            System.out.println("SNMPXlet:doTest1:FAIL IllegalArgumentException");
        }
        System.out.println("SNMPXlet:doTest1:END_TEST result = "+testResult);

    }

    /**
     * Ask for a non-existing OID and check that the MIBDefinition[] is of length zero
     */
    private void doTest2()
    {
        System.out.println("SNMPXlet:doTest2:START_TEST: Ask for non-existent oid");
        
        System.out.println("SNMPXlet:doTest2a:START_TEST: Ask for no such object leaf");
        doSubTest2(OID_NO_SUCH_OBJECT);
        
        System.out.println("SNMPXlet:doTest2b:START_TEST: Ask for no such object branch ");
        doSubTest2(OID_NO_SUCH_BRANCH);
        
        System.out.println("SNMPXlet:doTest2c:START_TEST: Ask for non existant OID ");
        doSubTest2(OID_NON_EXISTANT);
    }
    
    private void doSubTest2(String oid)
    {
        MIBDefinition[] mibd;
        String testResult = "";

        System.out.println("SNMPXlet:doSubTest2:queryMibs(" + oid +")");
        mibd = mibm.queryMibs(oid);
        if ((mibd != null) && (mibd.length == 0))
        {
            testResult = "PASS";
        }
        else
        {
            testResult = "FAIL";
        }

        System.out.println("SNMPXlet:doSubTest2:END_TEST result = " + testResult);
        
    }


    /**
     * Test getting OIDs from a root OID 'root'-enough to have data in both Stack and Platform
     */
    private void doTest3()
    {
        String testResult = "";
        
        MIBDefinition[] mibd;
        int x;

        System.out.println("SNMPXlet:doTest3:START_TEST: App OIDs related to CR019");

        System.out.println("SNMPXlet:doTest3:queryMibs(" + OID_STACK_AND_PLATFORM_BASE_OID+ ")");
        mibd = mibm.queryMibs(OID_STACK_AND_PLATFORM_BASE_OID);
        for (x=0; x < mibd.length; x++)
        {
            System.out.println("SNMPXlet:doTest3:MIBDef[" + x + "] getMIBObject().getOID(): " + mibd[x].getMIBObject().getOID());
        }
        if (mibd.length >= TEST3_ExpectedNumOids)
        {
            testResult = "PASS"; 
        }
        else
        {
            testResult = "FAIL"; 
        }
            
        System.out.println("SNMPXlet:doTest3:END_TEST result = " + testResult);
    }


    private static String getHexString(byte[] b)
    {
      String result = "";
      for (int i=0; i < b.length; i++) 
      {
        result +=  ("0x" + Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 ) + " ");
      }
      return result;
    }
    private static String getCharString(byte[] b)
    {
      String result = "";
      for (int i=0; i < b.length; i++) 
      {
        result +=  "  " + (char)b[i] + "  ";
      }
      return result;
    }
    

    /**
     * CR019 Test
     * Read a Stack MIB, check that the data is in Partial ASN.1 format
     */
    private void doTest4()
    {
        String testResult = "";
        
        MIBDefinition[] mibd;

        System.out.println("SNMPXlet:doTest4:START_TEST: Stack OID data ASN1 check (CR019)");

        System.out.println("SNMPXlet:doTest4:queryMibs((Integer Stack_Total_Memory_OID)" + OID_STACK_TOTAL_MEMORY_OID+ ")");
        mibd = mibm.queryMibs(OID_STACK_TOTAL_MEMORY_OID);

        if (mibd.length != 1)
        {
            testResult = "FAIL = Didn't get a MIBDefinition[] of one element";
        }
        else
        {
            int mibDataType = mibd[0].getDataType();
            byte[] mibData = mibd[0].getMIBObject().getData();
            
            System.out.println("SNMPXlet:doTest4:MIBDef[0].getMIBObject().getOID(): " + mibd[0].getMIBObject().getOID());
            System.out.println("                :MIBDef[0].getDataType(): " + mibDataType);
            System.out.println("                :MIBDef[0].getMIBObject().getData: " + getHexString(mibData));
            System.out.println("                :MIBDef[0].getMIBObject().getData.length: " + mibData.length);

            //From an application you cannot read environment vars to see which FullASN.enabled mode the RI is in, 
            // so provide alternative pass/fail options.
            
            testResult = "{if PARTIAL ASN1:"; //Make sure the length is short and the correct type has come through
            testResult += (mibData.length==PARTIAL_ASN1_LENGTH_INTEGER)
                       && (mibDataType == MIBDefinition.SNMP_TYPE_INTEGER)  ? "PASS}" : "FAIL}";
            
            testResult += " {if FULL ASN1:";
            testResult += (mibData.length==FULL_ASN1_LENGTH_INTEGER)
                        &&(mibDataType == MIBDefinition.SNMP_TYPE_INTEGER) 
                        &&((int)mibData[ASN1_POS_TYPE] == mibDataType) 
                        &&((int)mibData[ASN1_POS_LENGTH] == FULL_ASN1_LENGTH_INTEGER - 2) // In this simple test Type and Length fit in just two bytes
                        ? "PASS}" : "FAIL}";
        } 
        System.out.println("SNMPXlet:doTest4:END_TEST result = " + testResult);
    }

    /**
     * CR019 Test
     * Read a Platform MIB, check that the data is in Partial ASN.1 format
     */
    private void doTest5()
    {
        String testResult = "";
        
        MIBDefinition[] mibd;

        System.out.println("SNMPXlet:doTest5:START_TEST: Platform OID data ASN1 check (CR019)");

        System.out.println("SNMPXlet:doTest5:queryMibs((String OID_PLATFORM_SysDescr)" + OID_PLATFORM_SysDescr+ ")");
        mibd = mibm.queryMibs(OID_PLATFORM_SysDescr);

        if (mibd.length != 1)
        {
            testResult = "FAIL = Didn't get a MIBDefinition[] of one element";
        }
        else
        {
            int mibDataType = mibd[0].getDataType();
            byte[] mibData = mibd[0].getMIBObject().getData();
            byte[] mibValue = mibd[0].getMIBObject().getValue();
            
            System.out.println("SNMPXlet:doTest5:MIBDef[0].getMIBObject().getOID(): " + mibd[0].getMIBObject().getOID());
            System.out.println("                :MIBDef[0].getDataType(): " + mibDataType);
            System.out.println("                :MIBDef[0].getMIBObject().getData: " + getHexString(mibData));
            System.out.println("                :MIBDef[0].getMIBObject().getData: " + 
                ((mibData.length==PARTIAL_ASN1_LENGTH_OID_PLATFORM_SysDescr)?
                getCharString(mibData) :
                getCharString(mibValue)));
            System.out.println("                :MIBDef[0].getMIBObject().getData.length: " + mibData.length);

            //From an application you cannot read environment vars to see which FullASN.enabled mode the RI is in, 
            // so provide alternative pass/fail options.
            
            testResult = "{if PARTIAL ASN1:"; //Make sure the length is short and the correct type has come through
            testResult += (mibData.length==PARTIAL_ASN1_LENGTH_OID_PLATFORM_SysDescr)
                       && (mibDataType == MIBDefinition.SNMP_TYPE_OCTETSTRING)  ? "PASS}" : "FAIL}";
            
            testResult += " {if FULL ASN1:";
            testResult += (mibData.length==FULL_ASN1_LENGTH_OID_PLATFORM_SysDescr)
                        &&(mibDataType == MIBDefinition.SNMP_TYPE_OCTETSTRING) 
                        &&((int)mibData[ASN1_POS_TYPE] == mibDataType)
                        ? "PASS}" : "FAIL}";
        } 
        System.out.println("SNMPXlet:doTest5:END_TEST result = " + testResult);
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
        int status = SNMPResponse.SNMP_REQUEST_SUCCESS;

        switch (reqtype)
        {
            case SNMPRequest.SNMP_GET_REQUEST:
                rtstr = SNMP_GET_REQUEST;
                break;
            case SNMPRequest.SNMP_GET_NEXT_REQUEST:
                rtstr = SNMP_GET_NEXT_REQUEST;
                break;
            case SNMPRequest.SNMP_CHECK_FOR_SET_REQUEST:
                rtstr = SNMP_CHECK_FOR_SET_REQUEST;
                break;
            case SNMPRequest.SNMP_SET_REQUEST:
                rtstr = SNMP_SET_REQUEST;
                break;
            default:
                rtstr = SNMP_UNKNOWN_REQUEST;
                break;
        }

        System.out.println("SNMPXlet: notifySNMPRequest: "+rtstr);
        if (reqtype == SNMPRequest.SNMP_GET_REQUEST) 
        {
            if (oid.equals(OID_FOR_REGISTERING))
            {
                moOut = new MIBObject(oid, test1MibData);
            }
            else
            {
                status = SNMPResponse.SNMP_REQUEST_NO_SUCH_NAME;
                moOut = new MIBObject(oid, defaultMibData);
            }

        }
        else if (reqtype == SNMPRequest.SNMP_SET_REQUEST)
        {
            System.out.println("Data length = " + barray.length);
            for (int x = 0; x < barray.length; x++)
            {
                System.out.println("  Data [" + x + "] = " + barray[x]);
            }

            status = SNMPResponse.SNMP_REQUEST_NO_SUCH_NAME;
            moOut = new MIBObject(oid, defaultMibData);
        }

        SNMPResponse sr = new SNMPResponse(status, moOut);
        return sr;
    }

}
