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

package org.cablelabs.xlet.MIBDiagnosticsTest;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.ocap.diagnostics.*;

/*
 * The purpose of this test is to validate that a context switch takes place
 * between this xlet and the ocap stack when the MIBManager queryMib() method is called. 
 */
public class MIBDiagnosticsTestXlet implements Xlet, MIBListener
{
    MIBManager mibm = null;

    XletContext m_ctx = null;

    boolean started = false;

    String oid123456710 = "1.22.33.44.55.66.77.1.0";

    byte[] baInteger = { 2, 1, 2, 3 };

    // @Override
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("MIBDiagnosticsTestXlet.initXlet()");

        m_ctx = ctx;
        // Get a MIB Manager
        mibm = MIBManager.getInstance();
        if (mibm == null) throw new XletStateChangeException("No MIBManager Found");

    }

    // @Override
    public void startXlet() throws XletStateChangeException
    {
        System.out.println("MIBDiagnosticsTestXlet.startXlet()");

        if (started == true) return;

        started = true; // remember we've been here.

        // First register the OID
        try
        {
            mibm.registerOID(oid123456710, MIBManager.MIB_ACCESS_READWRITE, // access
                    true, // leaf
                    MIBDefinition.SNMP_TYPE_INTEGER, // 4 bytes
                    this); // MIBListener -> notifySNMPRequest
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("MIBDiagnosticsTestXlet registerOID exception" + e.getMessage());
            return;
        }

        // Now query for the same oid
        MIBDefinition[] mibd = mibm.queryMibs(oid123456710);
        if (mibd == null)
        {
            System.out.println("MIBDiagnosticsTestXlet Null returned from queryMibs");
            return;
        }

        if (mibd.length != 1)
        {
            System.out.println("MIBDiagnosticsTestXlet queryMibs array length not 1 is " + mibd.length);
            return;
        }

        if (mibd[0].getDataType() != MIBDefinition.SNMP_TYPE_INTEGER)
        {
            System.out.println("MIBDiagnosticsTestXlet queryMibs returned dataType not "
                    + MIBDefinition.SNMP_TYPE_INTEGER + " is " + mibd[0].getDataType());
            return;
        }

        MIBObject mibo = mibd[0].getMIBObject();
        if (mibo == null)
        {
            System.out.println("MIBDiagnosticsTestXlet queryMibs getMIBObject returned null");
            return;
        }

        String mibObjOid = mibo.getOID();
        if (!mibObjOid.equals(oid123456710))
        {
            System.out.println("MIBDiagnosticsTestXlet queryMibs getOID OID not as expected is " + mibObjOid);
            return;
        }

        byte[] mibObjBytes = mibo.getData();
        if (mibObjBytes.length == baInteger.length)
        {
            for (int x = 0; x < baInteger.length; x++)
                if (mibObjBytes[x] != baInteger[x])
                    System.out.println("MIBDiagnosticsTestXlet Returned byte not same [" + x + "] act "
                            + mibObjBytes[x] + " exp " + baInteger[x]);
        }
        else
        {
            System.out.println("MIBDiagnosticsTestXlet MIBObject getData data length not " + baInteger.length + " is "
                    + mibObjBytes.length);
            return;
        }

        // finally unregister OID
        mibm.unregisterOID(oid123456710);

        System.out.println("MIBDiagnosticsTestXlet  done.");

    }

    // @Override
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // TODO Auto-generated method stub
    }

    // @Override
    public void pauseXlet()
    {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.diagnostics.MIBListener#notifySNMPRequest(org.ocap.diagnostics
     * .SNMPRequest)
     */
    public SNMPResponse notifySNMPRequest(SNMPRequest request)
    {
        int reqType = request.getRequestType();
        MIBObject mibo = request.getMIBObject();
        String oid = mibo.getOID();

        System.out.println("MIBDiagnosticsTestXlet In notifySNMPRequest with OID " + oid);

        return new SNMPResponse(SNMPResponse.SNMP_REQUEST_SUCCESS, new MIBObject(oid, baInteger));
    }
}
