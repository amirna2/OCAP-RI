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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cablelabs.impl.snmp.OIDAmbiguityException;
import org.ocap.diagnostics.MIBDefinition;
import org.cablelabs.impl.ocap.diagnostics.MIBDefinitionExt;

/**
 * This interface defines a class that acts as a central clearing house for all
 * MIB information in the stack. Implementation of this class is designed to be
 * a thread-safe singleton.
 *
 * @author Alan Cossitt
 *
 */
public interface MIBRouter extends MIBValueAccess, OIDDelegator
{
    /**
     * Set a value in a MIB
     *
     * @param oid
     *            OID in {@link String} form. Must be leaf or value in table
     * @param newValue
     *            the new value in {@code byte} array form.
     *
     * @throws IllegalArgumentException
     *             if invalid OID or OID does not exist
     * @throws IOException
     *             if trying to write an read-only value
     */
    void setMIBRouterValue(String oid, byte[] newValue) throws IllegalArgumentException, IOException;

    /**
     * @param oid
     *            OID in {@link String} form
     *
     * @return an array of all values represented by OID in {@link MIBValueMap}
     *         form. If the OID does not exist the method will return an empty
     *         array (zero length).
     *
     * @throws IllegalArgumentException
     *             if invalid OID.
     * @throws IOException
     *             for some reason not able to access the MIB that contains the
     *             value
     */
    MIBDefinitionExt[] queryMIBRouter(String oid) throws IllegalArgumentException, IOException;

    /**
     *
     *
     * @param mib
     * @return true if MIB has been previously added, false otherwise
     */
    boolean isMIBAdded(MIB mib);

    /**
     * Add a {@link MIB} to the list of MIBs registered with the MIBRouter. Once
     * a MIB has been added its contents can be accessed through the
     * MIBValueAccess interface.
     *
     * @param mib
     * @throws IllegalArgumentException
     *             if {@link MIB} has poorly formed {@link OID}
     * @throws IllegalStateException
     *             if {@link MIB} is empty or there is some other problem with
     *             the {@link MIB}.
     * @throws OIDAmbiguityException
     *             if one of the {@link OID}s in the {@link MIB} overlaps an
     *             {@link OID} in a previously added {@link MIB}.
     */
    void addMIB(MIB mib) throws IllegalArgumentException, IllegalStateException, OIDAmbiguityException;

    /**
     *
     * Remove a {@link MIB} to the list of MIBs registered with the MIBRouter
     *
     * @param mib
     */
    void removeMIB(MIB mib);

    /**
     * Returns true if all the values for the OID passed in can be returned by
     * this class through queryMIBRouter.
     * If an OID is passed in for which some values may be hosted elsewhere then
     * false is returned. For example if the OID table 1.2.3 has been registered
     * with this class through addMIB, and isOidAdded(1.2) is called, then false
     * is returned since this class has no knowlege of 1.2.4 for instance which may
     * be hosted in elsewhere in the Platform but none the less needs to be included
     * in the results.
     *
     * @param oid
     *            OID in {@link String} form
     *
     * @return  true if the oid values can be completely returned through this class
     *          or false if the oid values should be retrieved from the Master SNMP
     *          instead.
     */
    public boolean isOidAdded(String oid);

}
