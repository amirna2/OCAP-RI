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

import org.apache.log4j.Logger;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPValueInteger;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;
import org.ocap.hardware.Host;

/**
 * This class handles registers and handles all the Host power info related OID
 * requests.
 * 
 * @author karunakarm
 */
public class HostPowerMIBModuleHandler extends LeafMIBModuleHandler implements MIBModuleHandler
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(HostPowerMIBModuleHandler.class
            .getName());

    private static final int AC_OUTLET_NOT_INSTALLED = 4;
    private static final int AC_OUTLET_SWITCHED_OFF = 3;
    private static final int AC_OUTLET_SWITCHED_ON = 2;
    private static final String POWER_STATUS_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.1.1.0";
    private static final String AC_OUTLET_STATUS_OID = "1.3.6.1.4.1.4491.2.3.1.1.4.1.2.0";
    private static final String[] SUPPORTED_OIDS = new String[] { POWER_STATUS_OID, AC_OUTLET_STATUS_OID };

    /**
     * This method reqisters all the MIBs associated with this module with the
     * <code>MIBManager</code> class.
     */
    public void registerMIBObjects()
    {
        registerOID(POWER_STATUS_OID, MIBDefinition.SNMP_TYPE_INTEGER);
        registerOID(AC_OUTLET_STATUS_OID, MIBDefinition.SNMP_TYPE_INTEGER);
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
        SNMPValue snmpValue = null;
        String oID = request.getMIBObject().getOID();
        if (POWER_STATUS_OID.equals(oID))
        {
            int powerMode = Host.getInstance().getPowerMode();
            snmpValue = new SNMPValueInteger(powerMode);
        }
        else if (AC_OUTLET_STATUS_OID.equals(oID))
        {
            snmpValue = getAcOutletStatus();
        }

        SNMPResponse snmpResponse = null;

        try
        {
            snmpResponse = new SNMPResponseExt(oID, snmpValue);
        }
        catch (SNMPBadValueException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Error while parsing the value");
            }
        }
        return snmpResponse;
    }

    /**
     * The method returns status of the AC outlet status in byte array. This
     * method returns the following codes: <li>4 = AC out let not present <li>2
     * = AC out currently ON. <li>4 = AC out currently OFF.
     * 
     * @return res - The status code in byte array.
     */
    private SNMPValue getAcOutletStatus()
    {
        SNMPValue snmpValue = null;
        try
        {
            int status = AC_OUTLET_NOT_INSTALLED;
            if (Host.getInstance().isACOutletPresent())
            {
                if (Host.getInstance().getACOutlet())
                {
                    status = AC_OUTLET_SWITCHED_ON;
                }
                else
                {
                    status = AC_OUTLET_SWITCHED_OFF;
                }
            }

            snmpValue = new SNMPValueInteger(status);

        }
        catch (IllegalStateException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Eror occurred While getting AC outlet status");
            }
            e.printStackTrace();
        }
        return snmpValue;
    }
}
