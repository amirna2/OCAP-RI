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

import java.util.ArrayList;

import org.cablelabs.impl.manager.snmp.MIB;
import org.cablelabs.impl.manager.snmp.OIDDelegator;

public interface DelegatorMIB extends MIB, OIDDelegator
{
    /**
     * Gets the OID delegate information for OIDs that have been delegated and
     * that are part of the subtree defined the input parameter. If the input
     * OID represents a scalar or array the returned data will be for the input
     * OID. If the OID represents a tree and there are no OIDs delegated that
     * are part of the tree, the OIDDelgationInfo for the tree will be returned.
     * Examples:
     * <ol>
     * <li>user registers 1.2.3.4.1.1.0 (scalar)
     * <li>user registers 1.2.3.4.1.2.0 (scalar)
     * <li>user registers 1.2.3.4.3 (in this case, this represents an array)
     * <li>user registers 1.2.3.5 (in this case, this represents a tree)
     * 
     * <li> {@code getDelegatedOidsInTree("1.2.3.4.1.0")} returns information
     * about "1.2.3.4.1.0" (array with one object)
     * <li> {@code getDelegatedOidsInTree("1.2.3.4.1")} returns information about
     * "1.2.3.4.1.1.0" and "1.2.3.4.1.2.0" (array with two objects)
     * <li> {@code getDelegatedOidsInTree("1.2.3.4.3")} returns information about
     * "1.2.3.4.3" (array of one object)
     * <li> {@code getDelegatedOidsInTree("1.2.3")} returns information about
     * "1.2.3.4.1.1.0", "1.2.3.4.1.2.0", "1.2.3.4.3", "1.2.3.5" (array with four
     * objects)
     * <li> {@code getDelegatedOidsInTree("1.4")} returns {@code null}
     * </ol>
     * 
     * 
     * 
     * @param rootOid
     *            OID that represents root of tree, scalar or array.
     * @return OIDDelegationInfo array of all OIDs in tree of {@code rootOid} .
     *         This will include the root and children. Array will be empty if
     *         there are no delegated OIDs in the tree.
     */
    public abstract ArrayList getDelegatedOidsInTree(String rootOid);
}
