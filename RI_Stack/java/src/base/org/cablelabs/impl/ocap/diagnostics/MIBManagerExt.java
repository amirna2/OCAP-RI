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

import org.ocap.diagnostics.MIBDefinition;
import org.ocap.diagnostics.MIBManager;

/**
 * @author karunakarm
 *
 */
public abstract class MIBManagerExt extends MIBManager 
{
    public static final int ESTB_SUBDEVICE = 0;
    public static final int ECM_SUBDEVICE = 1;
    
    /**
     * Makes a query for all MIB objects matching the oid parameter, as well as
     * any descendants in the MIB tree. If the object to be searched for is a
     * leaf the trailing ".0" must be included for an exact match. A query for a
     * leaf object SHALL return just that object if found. A query for a
     * non-leaf OID SHALL return all MIB objects below that OID. Existing leaf
     * and table items SHALL be included in the results; branch-nodes without
     * data SHALL NOT. For example; If a query is for OID 1.2.3.4 then all table
     * items and leafs below that OID are returned. If OIDs 1.2.3.4.1 and
     * 1.2.3.4.2 are the only items below the query object they would be
     * returned. The query SHALL NOT return items outside the OID. For example;
     * if 1.2.3.4 is the query OID then 1.2.3.5 is not returned. </p>
     * <p>
     * When both the Host device and CableCARD support the CARD MIB Access
     * resource introduced by CCIF2.0-O-08.1267-4 and the oid parameter is equal
     * to the OID or within the subtree of the OID returned by the
     * get_rootOID_req APDU, then the implementation SHALL use the snmp_request
     * APDU in order to satisfy the query.
     * </p>
     * @param <String>
     * 
     * @param source
     *            The source where the OID is hosted.
     * 
     * @param oid
     *            The object identifier to search for. The format of the string
     *            is based on the format defined by RFC 2578 for OBJECT
     *            IDENTIFIER definition. Terms in the string are period
     *            delimited, e.g. "1.3.6.1.4.1".
     * 
     * @return An array of MIB definitions. The array is lexographically ordered
     *         by increasing value of OID with the lowest value in the first
     *         element of the array.
     */
    public abstract MIBDefinition[] queryMibs(int source, String oid);
}
