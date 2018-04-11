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

import java.math.BigInteger;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValueInteger;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;

/**
 * This class handles registers and handles all the 
 * ocStbHostContentErrorSummaryInfo MIBs.
 * 
 * @author karunakarm
 */
public class HostSummaryErrorContentInfoMIBModuleHandler extends LeafMIBModuleHandler implements MIBModuleHandler
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(HostSummaryErrorContentInfoMIBModuleHandler.class.getName());

    private static String PAT_TIMEOUT_COUNT_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.5.7.1.0";
    private static String PMT_TIMEOUT_COUNT_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.5.7.2.0";
    private static String OOB_CAROUSEL_TIME_OUT_COUNT_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.5.7.3.0";
    private static String IB_CAROUSEL_TIME_OUT_COUNT_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.5.7.4.0";
    private static String SUPPORTED_OIDS[] = new String[] { PAT_TIMEOUT_COUNT_OID, PMT_TIMEOUT_COUNT_OID,
            OOB_CAROUSEL_TIME_OUT_COUNT_OID, IB_CAROUSEL_TIME_OUT_COUNT_OID };

    /**
     * This method lists all the OID's hosted by this module.
     * 
     * @return String[] - List of OID's
     */
    public String[] getOIDs()
    {
        return SUPPORTED_OIDS;
    }

    /**
     * This method reqisters all the MIBs associated with this module with the
     * <code>MIBManager</code> class.
     * 
     * @param mibManager
     */
    protected SNMPResponse processGetRequest(SNMPRequest request)
    {
        int value = -1;
        String OID = request.getMIBObject().getOID();
        if (PAT_TIMEOUT_COUNT_OID.equals(OID))
        {
            value = getPATTimeoutCount();
        }
        else if (PMT_TIMEOUT_COUNT_OID.equals(OID))
        {
            value = getPMTTimeoutCount();
        }
        else if (OOB_CAROUSEL_TIME_OUT_COUNT_OID.equals(OID))
        {
            value = getOOBTimeoutCount();
        }
        else if (IB_CAROUSEL_TIME_OUT_COUNT_OID.equals(OID))
        {
            value = getIBTimeoutCount();
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unexpected OID: " + OID);
            }
        }

        SNMPResponse response = null;
        if (value == -1)
        {
            response = new SNMPResponse(SNMPResponse.SNMP_REQUEST_GENERIC_ERROR, request.getMIBObject());
        }
        else
        {
            try
            {
                response = new SNMPResponseExt(OID, new SNMPValueInteger((long)value));
            }
            catch (SNMPBadValueException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error while parsing the value", new RuntimeException());
                }
            }
        }
        return response;
    }

    /**
     * This method handles all the GET requests of the incoming SNMP request.
     * 
     * @param request
     *            - <code>SNMPRequest</code> object.
     * @return snmpResponse - The <code>SNMPResponse</code> object for the GET
     *         request.
     */
    public void registerMIBObjects()
    {
        registerOID(PAT_TIMEOUT_COUNT_OID, MIBDefinition.SNMP_TYPE_INTEGER);
        registerOID(PMT_TIMEOUT_COUNT_OID, MIBDefinition.SNMP_TYPE_INTEGER);
        registerOID(OOB_CAROUSEL_TIME_OUT_COUNT_OID, MIBDefinition.SNMP_TYPE_INTEGER);
        registerOID(IB_CAROUSEL_TIME_OUT_COUNT_OID, MIBDefinition.SNMP_TYPE_INTEGER);
    }

    /**
     * This method returns the PAT time out count.
     * 
     * @return the pat time out count.
     */
    public native int getPATTimeoutCount();

    /**
     * This method returns the PMT time out count.
     * 
     * @return the pmt time out count.
     */
    public native int getPMTTimeoutCount();

    /**
     * This method returns the IB time out count.
     * 
     * @return the IB time out count.
     */
    public native int getIBTimeoutCount();

    /**
     * This method returns the OOB time out count.
     * 
     * @return the OOB time out count.
     */
    public native int getOOBTimeoutCount();
}
