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

package org.ocap.diagnostics;

/**
 * This interface represents a MIB object that exposes its data type. See RFC
 * 2578 for data type definition.
 */
public interface MIBDefinition
{
    /**
     * Unrecognized type encountered. Not defined by RFC 2578.
     */
    final static int SNMP_TYPE_INVALID = 0;

    /**
     * Base type, built-in ASN.1 integer type.
     */
    final static int SNMP_TYPE_INTEGER = 0x02;

    /**
     * The BITS construct.
     */
    final static int SNMP_TYPE_BITS = 0x03;

    /**
     * Base type, built-in ASN.1 string type.
     */
    final static int SNMP_TYPE_OCTETSTRING = 0x04;

    /**
     * Base type, built-in ASN.1 OBJECT IDENTIFIER type.
     */
    final static int SNMP_TYPE_OBJECTID = 0x06;

    /**
     * Base type, application defined IP address.
     */
    final static int SNMP_TYPE_IPADDRESS = 0x40;

    /**
     * Base type, application defined 32 bit counter.
     */
    final static int SNMP_TYPE_COUNTER32 = 0x41;

    /**
     * Base type, application defined 32 bit gauge.
     */
    final static int SNMP_TYPE_GAUGE32 = 0x42;

    /**
     * Base type, built-in ASN.1 Unsigned32 - non-negative 32-bit values
     */
    final static int SNMP_TYPE_UNSIGNED32 = 0x42;

    /**
     * Base type, application defined time ticks.
     */
    final static int SNMP_TYPE_TIMETICKS = 0x43;

    /**
     * Base type, application defined opaque variable.
     */
    final static int SNMP_TYPE_OPAQUE = 0x44;

    /**
     * Base type, application defined 64 bit counter.
     */
    final static int SNMP_TYPE_COUNTER64 = 0x46;

    /**
     * Gets the SNMP data type of the MIB.
     * 
     * @return An SNMP data type defined by constants in this interface.
     */
    public int getDataType();

    /**
     * Gets the MIB object associated with this MIB definition.
     * 
     * @return The MIB Object for this MIB definition.
     */
    public MIBObject getMIBObject();

}
