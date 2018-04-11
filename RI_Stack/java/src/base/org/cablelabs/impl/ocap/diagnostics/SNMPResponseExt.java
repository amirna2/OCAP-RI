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

import org.cablelabs.impl.snmp.DelegatorMIBImpl;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPClient;
import org.cablelabs.impl.snmp.SNMPValue;
import org.cablelabs.impl.snmp.SNMPValueError;
import org.cablelabs.impl.snmp.SNMPValueBitString;
import org.cablelabs.impl.snmp.SNMPValueOctetString;
import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPResponse;

import org.apache.log4j.Logger;

/**
 * This class extends the SNMPResponse to allow for access to
 * agentX value status, oid and snmp value information and saves
 * on unnecessary encoding and decoding of ASN.1 format.
 *
 * This class should be used in place of SNMPResponse within
 * MIBListeners.
 *
 * @author Alan Glynne-Jones
 */
public class SNMPResponseExt extends SNMPResponse
{
    private static final Logger log = Logger.getLogger(SNMPResponseExt.class);

    private String oid;
    private SNMPValue value;

    /**
     * Constructs an SNMPResponsExt.
     * status and agentx_value_status will default to success values
     *
     * @param oid
     *            oid String representing the result.
     * @param value
     *            The SNMPValue subclass result set to null if no result is available.
     */
    public SNMPResponseExt(String oid, SNMPValue value) throws SNMPBadValueException
    {
        this(SNMP_REQUEST_SUCCESS, oid, value);
    }

    /**
     * Constructs an SNMPResponse.
     *
     * @param status
     *            Status of the corresponding request. Possible values include
     *            any of the SNMP_REQUEST_* constants in this class.
     * @param agentx_value_status
     *            AgentX error status.
     * @param oid
     *            oid String representing the result.
     * @param value
     *            The SNMPValue subclass result set to null if no result is available.
     */
    public SNMPResponseExt(int status, String oid, SNMPValue value) throws SNMPBadValueException
    {
        super(value instanceof SNMPValueError ? ((SNMPValueError)value).getErrorCode() : status,
              value instanceof SNMPValueBitString ? new MIBObject(oid, ((SNMPValueOctetString)value).getBEREncoding()) :
                                                    new MIBObject(oid, value.getBEREncoding()));

        if (value instanceof SNMPValueBitString)
        {
            // degrade an SNMP_TYPE_BITS object (not supported by AgentX) into an SNMP_TYPE_OCTET_STRING object
            value = (SNMPValueOctetString)value;
        }

        this.oid = oid;
        this.value = value;
    }

    /**
     * Constructs an SNMPResponseExt from an SNMPResponse.
     * This constructor will take an SNMPResponse or an SNMPResponseExt class
     *
     * @param response The SNMPResponse to copy
     */
    public SNMPResponseExt(SNMPResponse response) throws SNMPBadValueException
    {
        super(response.getStatus(), SNMPValueBitString.isBITS(response.getMIBObject()) ?
              SNMPValueBitString.bitsToOctet(response.getMIBObject()) : response.getMIBObject());

        MIBObject mibObj = response.getMIBObject();
        if (mibObj == null)
        {
            throw new SNMPBadValueException("Null MIBObject found in SNMPResponse");
        }
        else
        {
            // extract information from the ASN.1 data in the MIBObject
            oid = response.getMIBObject().getOID();

            // handle an SNMPValueError
            if (response instanceof SNMPResponseExt)
            {
                this.value = ((SNMPResponseExt) response).getValue();
            }
            else
            {
                value = SNMPClient.getSNMPValueFromBER(response.getMIBObject().getData());

                if (value instanceof SNMPValueBitString)
                {
                    value = (SNMPValueOctetString)value;
                }
            }
        }
    }

    /**
     * Get the OID of the response.
     *
     * @return the OID String.
     */
    public String getOID()
    {
        return oid;
    }

    /**
     * Get the SNMPValue of the response.
     *
     * @return an SNMPValue.
     */
    public SNMPValue getValue()
    {
        return value;
    }

    /**
     * @return a MIBObject with the correct SNMPValueError encoding
     *         any other SNMPValue types call the parent class method
     */
    public MIBObject getMIBObject()
    {
        MIBObject mibObject = null;

        // handle an SNMPValueError
        if (value instanceof SNMPValueError)
        {
            try
            {
                mibObject = new MIBObject(oid, value.getBEREncoding());
            }
            catch(SNMPBadValueException e)
            {
                // give up and use the parent class
                mibObject = super.getMIBObject();
                if (log.isErrorEnabled())
                {
                    log.error("Failed to assign BEREncoding to SNMPError value");
                }
            }
        }
        else
        {
            mibObject = super.getMIBObject();
        }

        return mibObject;
    }

    public MIBDefinition getMIBDefiniton()
    {
        return new MIBDefinitionImpl(super.getMIBObject(), getValue());
    }

    /**
     * Converts the integer returned by {@link SNMPResponse#getStatus()} into a human readable String for debug purposes
     * @param status value returned from {@link SNMPResponse#getStatus()}
     * @return human readable {@link String}
     */
    public static String errorStatusToString(int status)
    {
            switch(status)
            {
            case SNMPResponse.SNMP_REQUEST_AUTHORIZATION_ERROR:
                return "SNMP_REQUEST_AUTHORIZATION_ERROR";
            case SNMPResponse.SNMP_REQUEST_BAD_VALUE:
                return "SNMP_REQUEST_BAD_VALUE";
            case SNMPResponse.SNMP_REQUEST_COMMIT_FAILED:
                return "SNMP_REQUEST_COMMIT_FAILED";
            case SNMPResponse.SNMP_REQUEST_GENERIC_ERROR:
                return "SNMP_REQUEST_GENERIC_ERROR";
            case SNMPResponse.SNMP_REQUEST_INCONSISTENT_NAME:
                return "SNMP_REQUEST_INCONSISTENT_NAME";
            case SNMPResponse.SNMP_REQUEST_INCONSISTENT_VALUE:
                return "SNMP_REQUEST_INCONSISTENT_VALUE";
            case SNMPResponse.SNMP_REQUEST_NO_ACCESS:
                return "SNMP_REQUEST_NO_ACCESS";
            case SNMPResponse.SNMP_REQUEST_NO_CREATION:
                return "SNMP_REQUEST_NO_CREATION";
            case SNMPResponse.SNMP_REQUEST_NO_SUCH_NAME:
                return "SNMP_REQUEST_NO_SUCH_NAME";
            case SNMPResponse.SNMP_REQUEST_NOT_WRITABLE:
                return "SNMP_REQUEST_NOT_WRITABLE";
            case SNMPResponse.SNMP_REQUEST_READ_ONLY:
                return "SNMP_REQUEST_READ_ONLY";
            case SNMPResponse.SNMP_REQUEST_RESOURCE_UNAVAILABLE:
                return "SNMP_REQUEST_RESOURCE_UNAVAILABLE";
            case SNMPResponse.SNMP_REQUEST_SUCCESS:
                return "SNMP_REQUEST_SUCCESS";
            case SNMPResponse.SNMP_REQUEST_TOO_BIG:
                return "SNMP_REQUEST_TOO_BIG";
            case SNMPResponse.SNMP_REQUEST_UNDO_FAILED:
                return "SNMP_REQUEST_UNDO_FAILED";
            case SNMPResponse.SNMP_REQUEST_WRONG_ENCODING:
                return "SNMP_REQUEST_WRONG_ENCODING";
            case SNMPResponse.SNMP_REQUEST_WRONG_LENGTH:
                return "SNMP_REQUEST_WRONG_LENGTH";
            case SNMPResponse.SNMP_REQUEST_WRONG_TYPE:
                return "SNMP_REQUEST_WRONG_TYPE";
            case SNMPResponse.SNMP_REQUEST_WRONG_VALUE:
                return "SNMP_REQUEST_WRONG_VALUE";
            default:
                return "ERROR: status not recognised";
            }
        }
    }
