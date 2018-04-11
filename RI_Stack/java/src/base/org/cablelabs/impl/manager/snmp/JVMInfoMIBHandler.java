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
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPValueInteger;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;

/**
 * This class handles registers and handles all the JVM info related OID
 * requests.
 * 
 * @author karunakarm
 */
public class JVMInfoMIBHandler extends LeafMIBModuleHandler implements MIBModuleHandler
{

    private static final String TOTAL_MEMORY_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.8.1.0";
    private static final String FREE_MEMORY_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.8.2.0";
    private static final String[] SUPPORTED_OIDS = new String[] { TOTAL_MEMORY_OID, FREE_MEMORY_OID };

    /**
     * This method handles all the GET requests of the incoming SNMP request.
     * 
     * @param request
     *            - <code>SNMPRequest</code> object.
     * @return snmpResponse - The <code>SNMPResponse</code> object for the GET
     *         request.
     */
    public SNMPResponse processGetRequest(SNMPRequest request)
    {
        SNMPResponse response = null;
        String oID = request.getMIBObject().getOID();
        Runtime rTime = Runtime.getRuntime();
        long result = -1;
        SNMPValue snmpValue = null;
        if (TOTAL_MEMORY_OID.equals(oID))
        {
            result = rTime.totalMemory();
        }
        else if (FREE_MEMORY_OID.equals(oID))
        {
            result = rTime.freeMemory();
        }

        snmpValue = new SNMPValueInteger(result);

        try
        {
            response = new SNMPResponseExt(oID, snmpValue);
        }
        catch (SNMPBadValueException e)
        {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * This method reqisters all the MIBs associated with this module with the
     * <code>MIBManager</code> class.
     */
    public void registerMIBObjects()
    {
        registerOID(TOTAL_MEMORY_OID, MIBDefinition.SNMP_TYPE_INTEGER);
        registerOID(FREE_MEMORY_OID, MIBDefinition.SNMP_TYPE_INTEGER);
    }

    /**
     * This method lists all the OID's hosted by this module.
     * 
     * @return String[] - List of OID's
     */
    public String[] getOIDs()
    {
        return SUPPORTED_OIDS;
    }
}
