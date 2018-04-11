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

package org.cablelabs.impl.manager.snmp;

import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValueError;
import org.ocap.diagnostics.MIBListener;
import org.ocap.diagnostics.MIBManager;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;

/**
 * This class is the base class for all the table MIB classes.
 * 
 * @author karunakarm
 */
public abstract class LeafMIBModuleHandler implements MIBListener
{

    /**
     * This is the call back method for the <code>MIBListener</code>. Handles
     * and process the SNMP requests.
     * 
     * @param request
     *            - The incoming SNMP request.
     * @return snmpResponse - The <code>SNMPResponse</code> for the request.
     */
    public SNMPResponse notifySNMPRequest(SNMPRequest request)
    {
        MIBObject reqMIBObj = request.getMIBObject();
        String mibOID = reqMIBObj.getOID();
        SNMPResponse response = null;
        if (validateOID(mibOID))
        {
            response = processRequest(request);
        }
        else
        {
            try
            {
                response = new SNMPResponseExt(mibOID, SNMPValueError.NO_SUCH_INSTANCE);
            }
            catch (SNMPBadValueException e)
            {

                e.printStackTrace();
            }
        }
        return response;
    }

    /**
     * This method checks whether incoming OID belongs to this listener or not.
     * 
     * @param mibOID
     *            - The OId for which data is requested.
     * @return found - Returns true if the OID belongs to the listener.
     */
    private boolean validateOID(String mibOID)
    {
        boolean found = false;
        String[] oID = getOIDs();
        if (oID == null)
        {
            return found;
        }

        int len = oID.length;
        for (int i = 0; i < len; i++)
        {
            if (oID[i].equals(mibOID))
            {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * This method handles all the SNMP GET requests according to their types
     * 
     * @param request
     *            - The <code>SNMPRequest</code> object.
     * 
     * @return snmpResponse - The <code>SNMPResponse</code> for the request.
     */
    protected SNMPResponse processRequest(SNMPRequest request)
    {
        SNMPResponse snmpResponse = null;
        int type = request.getRequestType();
        MIBObject mibObject = request.getMIBObject();
        switch (type)
        {

        case SNMPRequest.SNMP_CHECK_FOR_SET_REQUEST:
            snmpResponse = new SNMPResponse(SNMPResponse.SNMP_REQUEST_NOT_WRITABLE, mibObject);
            break;

        case SNMPRequest.SNMP_GET_NEXT_REQUEST:
            snmpResponse = new SNMPResponse(SNMPResponse.SNMP_REQUEST_WRONG_TYPE, mibObject);
            break;

        case SNMPRequest.SNMP_GET_REQUEST:
            snmpResponse = processGetRequest(request);
            break;

        case SNMPRequest.SNMP_SET_REQUEST:
            snmpResponse = new SNMPResponse(SNMPResponse.SNMP_REQUEST_READ_ONLY, mibObject);
            break;

        default:
            break;
        }

        return snmpResponse;
    }

    /**
     * This method registers the OIDs with the <code>MIBManager</code>
     * 
     * @param oID
     *            - The OID to be registered.
     * @param dataType
     *            - The data type of the OID.
     * @param leaf
     *            - The boolean indicating whether OID is leaf or not.
     */
    protected void registerOID(String oID, int dataType)
    {
        MIBManager.getInstance().registerOID(oID, MIBManager.MIB_ACCESS_READONLY, true, dataType, this);
    }

    /**
     * This method handles all the GET requests of the incoming SNMP request.
     * 
     * @param request
     *            - <code>SNMPRequest</code> object.
     * @return snmpResponse - The <code>SNMPResponse</code> object for the GET
     *         request.
     */
    protected abstract SNMPResponse processGetRequest(SNMPRequest request);

    /**
     * This method lists all the OID's hosted by this module. In case of table
     * MIB it returns only the base table OID.
     * 
     * @return String[] - List of OID's
     */
    public abstract String[] getOIDs();
}
