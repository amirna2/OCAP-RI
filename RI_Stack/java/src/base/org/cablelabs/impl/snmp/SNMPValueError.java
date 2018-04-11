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

import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.SNMPResponse;

/**
 * Representation of SNMP ErrorValues
 */

public class SNMPValueError extends SNMPValue {

    private final static byte NO_SUCH_OBJECT_TYPE = (byte)0x80;
    private final static byte NO_SUCH_INSTANCE_TYPE = (byte)0x81;
    private final static byte END_OF_MIB_VIEW_TYPE = (byte)0x82;
    private final static byte NULL_TYPE = (byte)0x05;

    private final static byte[] NO_SUCH_OBJECT_BYTES = {NO_SUCH_OBJECT_TYPE, (byte)0x00};
    private final static byte[] NO_SUCH_INSTANCE_BYTES = {NO_SUCH_INSTANCE_TYPE, (byte)0x00};
    private final static byte[] END_OF_MIB_VIEW_BYTES = {END_OF_MIB_VIEW_TYPE, (byte)0x00};
    private final static byte[] NULL_BYTES = {NULL_TYPE, (byte)0x00};

    /**
     * The OID presented represents the prefix of a value, but does
     * not fully define a value
     */
    public static final SNMPValueError NO_SUCH_OBJECT = new SNMPValueError(NO_SUCH_OBJECT_BYTES, "noSuchObject");

    /**
     * The OID presented doesn't match any values or prefixes.
     */
    public static final SNMPValueError NO_SUCH_INSTANCE = new SNMPValueError(NO_SUCH_INSTANCE_BYTES, "noSuchInstance");

    /**
     * No lexicographical value could be found that follows
     * presented OID.
     */
    public static final SNMPValueError END_OF_MIB_VIEW = new SNMPValueError(END_OF_MIB_VIEW_BYTES, "endOfMibView");

    /**
     * A SNMPValueError representing a null value.
     */
    public static final SNMPValueError NULL = new SNMPValueError(NULL_BYTES, "null");

    private byte[] asn1Data;

    private String text;

    /**
     * Constructs an SNMPValueError with appropriate ASN.1 data
     *
     * @param asn1Data
     */
    private SNMPValueError(byte[] asn1Data, String text) {
        super(null);
        this.asn1Data = asn1Data;
        this.text = text;
    }

    /**
     * Gets a String representing the Error
     */
    public String toString() {
        return text;
    }

    /**
     * Gets the ASN.1 byte array
     */
    public byte[] getBEREncoding() {
        return asn1Data;
    }

    /**
     * @return the MIBDefinition::SNMP_TYPE<xxx> value
     */
    public int getType() {
        return MIBDefinition.SNMP_TYPE_INVALID;
    }

    public int getErrorCode() {
        return SNMPResponse.SNMP_REQUEST_GENERIC_ERROR;
    }
}
