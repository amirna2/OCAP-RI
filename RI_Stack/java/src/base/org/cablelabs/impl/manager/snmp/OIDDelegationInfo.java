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

import org.ocap.diagnostics.SNMPRequest;

public class OIDDelegationInfo
{
    public static final int DELEGATE_TYPE_NA = -1;

    public static final int DELEGATE_TYPE_TREE = 0;

    public static final int DELEGATE_TYPE_LEAF = 1; // aka scalar

    public static final int DELEGATE_TYPE_TABLE = 2; // entire table or column
                                                     // in table
    /**
     * Unknown or un-set SNMP request type.
     */
    public final static int SNMP_REQUEST_UNKNOWN = -1;

    /**
     * Used to validate a MIB value before doing a set.
     */
    public final static int SNMP_CHECK_FOR_SET_REQUEST = 0;

    /**
     * Set (modify) the value in the MIB for an OID.
     */
    public final static int SNMP_SET_REQUEST = 1;

    /**
     * Get data for the exact OID passed in.
     */
    public final static int SNMP_GET_REQUEST = 2;

    /**
     * Get data for the next OID beyond the one passed in.
     */
    public final static int SNMP_GET_NEXT_REQUEST = 4;

    /**
     * As defined in this class.
     */
    public int delegateType;

    /**
     * As defined in this class
     */
    // public int access;

    /**
     * As defined in the SNMP wire protocol.
     */
    public int requestType;

    /**
     * OID in the form 1.2.3.4.0 etc.
     */
    public String oid;

    /**
     * The value to be set by the delegate. For gets this will be null.
     */
    public byte[] setValue;

    /**
     * User defined object. This will be passed back as part of this object when
     * {@link OIDDelegationListener#notifyOidValueRequest(OIDDelegationInfo) or
     * {@link MIBRouter#getDelegatedOidsInTree(String)} is called.
     */
    public Object userObject;

    public OIDDelegationListener listener;

    public OIDDelegationInfo(String oid, int delegateType, int reqType, byte[] setValue,
            OIDDelegationListener listener, Object userObj)
    {
        this.oid = oid;
        this.delegateType = delegateType;
        this.requestType = reqType;
        this.setValue = setValue;
        this.listener = listener;
        this.userObject = userObj;
    }

    public OIDDelegationInfo(OIDDelegationInfo infoToCopy)
    {
        this.oid = infoToCopy.oid;
        this.delegateType = infoToCopy.delegateType;
        this.requestType = infoToCopy.requestType;
        this.setValue = infoToCopy.setValue;
        this.listener = infoToCopy.listener;
        this.userObject = infoToCopy.userObject;
    }

    public String getRequestTypeString()
    {
        switch (requestType)
        {
            case SNMPRequest.SNMP_GET_REQUEST: return "SNMP_GET_REQUEST";
            case SNMPRequest.SNMP_GET_NEXT_REQUEST: return "SNMP_GET_NEXT_REQUEST";
            case SNMPRequest.SNMP_SET_REQUEST: return "SNMP_SET_REQUEST";
            case SNMPRequest.SNMP_CHECK_FOR_SET_REQUEST: return "SNMP_CHECK_FOR_SET_REQUEST";
            default: return "SNMP_REQUEST_UNKNOWN";
        }
    }
}
