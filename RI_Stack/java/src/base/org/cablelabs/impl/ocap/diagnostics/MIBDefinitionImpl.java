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

package org.cablelabs.impl.ocap.diagnostics;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.diagnostics.MIBDefinitionExt;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBObject;
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPClient;
import org.cablelabs.impl.snmp.SNMPBadValueException;

public class MIBDefinitionImpl implements MIBDefinitionExt
{
    private static final Logger log = Logger.getLogger(MIBDefinitionImpl.class);

    private int m_dataType;

    private MIBObject m_mibObj;

    private SNMPValue m_snmpValue;

    // Constructor
    public MIBDefinitionImpl(int dataType, MIBObject mibObj)
    {
        m_dataType = dataType;
        m_mibObj = mibObj;
        try
        {
            m_snmpValue = SNMPClient.getSNMPValueFromBER(mibObj.getData());
        }
        catch (SNMPBadValueException e)
        {
            //no exception in interface
        }
    }

    // Constructor
    public MIBDefinitionImpl(MIBObject mibObj, SNMPValue snmpValue)
    {
        m_dataType = snmpValue.getType();
        m_mibObj = mibObj;
        m_snmpValue = snmpValue;
    }

    /**
     *@see org.ocap.diagnostics.MIBDefinition#getDataType()
     */
    public int getDataType()
    {
        return m_dataType;
    }

    /**
     *@see org.ocap.diagnostics.MIBDefinition#getMIBObject()
     */
    public MIBObject getMIBObject()
    {
        return m_mibObj;
    }

    /**
     * Get the particular SNMPValue subclass.
     */
    public SNMPValue getValue()
    {
        return m_snmpValue;
    }

    /**
     * Converts an integer {@link MIBDefinition} SNMP_TYPE_ into a human readable String for debug purposes
     * @param type {@link MIBDefinition} SNMP_TYPE_ to be converted
     * @return human readable {@link String}
     */
    public static String typeToString(int type)
    {
        String s = "";

            switch(type)
            {
            case MIBDefinition.SNMP_TYPE_INVALID:
                s = "SNMP_TYPE_INVALID";
                break;
            case MIBDefinition.SNMP_TYPE_INTEGER:
                s = "SNMP_TYPE_INTEGER";
                break;
            case MIBDefinition.SNMP_TYPE_BITS:
                s = "SNMP_TYPE_BITS";
                break;
            case MIBDefinition.SNMP_TYPE_OCTETSTRING:
                s = "SNMP_TYPE_OCTETSTRING";
                break;
            case MIBDefinition.SNMP_TYPE_OBJECTID:
                s = "SNMP_TYPE_OBJECTID";
                break;
            case MIBDefinition.SNMP_TYPE_IPADDRESS:
                s = "SNMP_TYPE_IPADDRESS";
                break;
            case MIBDefinition.SNMP_TYPE_COUNTER32:
                s = "SNMP_TYPE_COUNTER32";
                break;
            case MIBDefinition.SNMP_TYPE_GAUGE32:
                s = "SNMP_TYPE_GAUGE32";
                break;
            case MIBDefinition.SNMP_TYPE_TIMETICKS:
                s = "SNMP_TYPE_TIMETICKS";
                break;
            case MIBDefinition.SNMP_TYPE_OPAQUE:
                s = "SNMP_TYPE_OPAQUE";
                break;
            case MIBDefinition.SNMP_TYPE_COUNTER64:
                s = "SNMP_TYPE_COUNTER64";
                break;
            default:
                s = "unknown type";
                break;
            }

        return s;
    }
}
