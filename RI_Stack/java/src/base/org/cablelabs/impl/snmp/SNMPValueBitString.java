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
import org.ocap.diagnostics.MIBObject;

import org.cablelabs.impl.snmp.drexel.SNMPBitString;

import org.cablelabs.impl.snmp.SNMPValueOctetString;
import org.cablelabs.impl.snmp.SNMPBadValueException;

/**
 * This Class represents an SNMPValue of type
 * MIBDefinition.SNMP_TYPE_BITS
 */
public class SNMPValueBitString extends SNMPValueOctetString
{
    /**
     * Constructor
     * Create an empty Object.
     */
    public SNMPValueBitString()
    {
        super(new SNMPBitString());
    }

    /**
     *    Constructor
     *    Create a bit string from the bytes of the supplied String.
     */
    public SNMPValueBitString(String value)
    {
        super(new SNMPBitString(value));
    }

    /**
     *    Constructor
     * @param enc
     *           The byte representation of the value without
     *           any type or length bytes.
     */
    public SNMPValueBitString(byte[] value) throws SNMPBadValueException
    {
        super(new SNMPBitString(value));
    }

    /**
     * @return the MIBDefinition::SNMP_TYPE<xxx> value
     */
    public int getType()
    {
        return MIBDefinition.SNMP_TYPE_BITS;
    }

    /**
     * Converts a MIBObject with BITS encoded byte array into a MIBObject containing an
     * OCTET_STRING encoded byte array
     * @param mibObject must contain a BITS encoded byte array
     * @return {@link MIBObject} with OCTET_STRING encoded byte array
     * @throws SNMPBadValueException
     */
    static public MIBObject bitsToOctet(MIBObject mibObject) throws SNMPBadValueException
    {
        if (null == mibObject)
        {
            throw new SNMPBadValueException("recieved a null pointer");
        }

        if (!isBITS(mibObject))
        {
            throw new SNMPBadValueException("bitsToOctet conversion was not passed a valid SNMP_TYPE_BITS MIBObject");
        }

        SNMPValueBitString bits = (SNMPValueBitString)SNMPClient.getSNMPValueFromBER(mibObject.getData());
        return new MIBObject(mibObject.getOID(), ((SNMPValueOctetString)bits).getBEREncoding());
    }

    /**
     * test to determine if a {@link MIBObject} contains a BITS encoded byte array
     * @param mibObject MIBObject to test
     * @return <b>true</b> if mibObject contains a BITS encoded byte array
     * @throws SNMPBadValueException
     */
    static public boolean isBITS(MIBObject mibObject) throws SNMPBadValueException
    {
        if (null == mibObject)
        {
            throw new SNMPBadValueException("recieved a null pointer");
        }

        SNMPValue value = SNMPClient.getSNMPValueFromBER(mibObject.getData());
        return MIBDefinition.SNMP_TYPE_BITS == value.getType();
    }
}


