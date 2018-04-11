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

import org.ocap.system.MonitorAppPermission;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.manager.snmp.MIBRouter;
import org.cablelabs.impl.ocap.diagnostics.MIBManagerImpl;

/**
 * The MIBManager class provides Management Information Base (MIB)
 * access to the host's MIB.  Applications may use the MIBManager 
 * to make MIB queries.  In addition, applications can set a MIB Object
 * that can be retrieved from the Host device using SNMP.
 */
public abstract class MIBManager
{
    // member variables
    static private MIBManager instance = null;

    static private Object lock = new Object();

    // MIB Accessibility
    public final static int MIB_ACCESS_READONLY = 0;
    public final static int MIB_ACCESS_READWRITE = 1;
    public final static int MIB_ACCESS_WRITEONLY = 2;

    /**
     * Used to identify the subdevice type and query eCM MIB values.
     */
    public final static int ECM_SUBDEVICE = 1;

    /**
     * Used to identify the subdevice type and query Host MIB values.
     */
    public final static int ESTB_SUBDEVICE = ECM_SUBDEVICE + 1;


    /**
     * Protected constructor, no application access.
     */
    protected MIBManager()
    {
    }

    /**
     * Gets the MIBManager.
     * 
     * @return The MIBManager.
     * 
     * @throws SecurityException if the calling application does not have
     *      MonitorAppPermission("diagnostics"). 
     */
    public static MIBManager getInstance()
    {
        // Check with security manager to verify application has access to
        // diagnostics api
        SecurityUtil.checkPermission(new MonitorAppPermission("diagnostics"));

        synchronized (lock)
        {
            if (null == instance) instance = new MIBManagerImpl(null);
        }
        return instance;
    }

    /**
     * Registers a MIB object by adding the OID and listener to the MIB tables.  
     * OID must be unique and not pre-existing.
     * OID must include a trailing ".0" for leaf items or but not include the instance ID
     * for table column items.  (The same listener may be installed to will be invoked
     * for all row instances for table column items. any number of OIDs, if desired.)
     * Table OIDs may be the OID of the start of the table or a column 
     * within the table, depending upon whether the implementation desires
     * handling all table values with a single listener, or have different
     * listeners for each column.  
     * 
     * @param oid The Object Identifier of the MIB being registered.  The
     *      format of the string is based on the format defined by RFC 2578 for
     *      OBJECT IDENTIFIER definition.  Terms in the string are period
     *      delimited, e.g. "1.3.6.1.4.1".
     * @param leaf When true indicates if the object is a leaf, otherwise the
     *      object is a non-leaf table column.
     * @param access Indicates allowed access to the MIB being registered.  See
     *      access constants in the class for valid values.
     * @param dataType The data type of the MIB being registered.  See constants
     *      in {@link MIBDefinition} for valid values.
     * @param listener Listener to the MIB being registered.  Must not be null.
     * 
     * @throws IllegalArgumentException if oid is an invalid oid string, 
     *      if oid is already installed, or any other parameter has an 
     *      invalid value.
     */
    public abstract void registerOID(String oid,
                                     int access,
                                     boolean leaf,
                                     int dataType,
                                     MIBListener listener);
                   
    /**
     * Unregisters a previously registered OID if the OID was
     * registered by the same application. 
     * 
     * @param oid An object identifier that was passed to the registerOID
     *      method.
     * 
     * @throws IllegalArgumentException if parameter does not match an OID
     *      registered with the registerOID method.
     */ 
    public abstract void unregisterOID(String oid);


    /**
     * Makes a query for all MIB objects matching the oid parameter, as
     * well as any descendants in the MIB tree.  If the object to be searched
     * for is a leaf the trailing ".0" must be included for an exact match.
     * A query for a leaf object SHALL return just that object if found.  A
     * query for a non-leaf OID SHALL return all MIB objects below that OID.
     * Existing leaf and table items SHALL be included in the results;
     * branch-nodes without data SHALL NOT.  For example; If a query is for
     * OID 1.2.3.4 then all table items and leafs below that OID are returned.
     * If OIDs 1.2.3.4.1 and 1.2.3.4.2 are the only items below the query object
     * they would be returned.  The query SHALL NOT return items outside the
     * OID.  For example; if 1.2.3.4 is the query OID then 1.2.3.5 is not
     * returned.</p><p>
     * 
     * If the CableCARD supports MIB APDUs then this method SHALL be able to
     * query the CableCARD MIB objects.</p>
     *
     * @param oid The object identifier to search for.  The format of the
     *      string is based on the format defined by RFC 2578 for
     *      OBJECT IDENTIFIER definition.  Terms in the string
     *      are period delimited, e.g. "1.3.6.1.4.1".
     * 
     * @return An array of MIB definitions.  The array is lexographically ordered
     *      by increasing value of OID with the lowest value in the first element
     *      of the array.
     */ 
    public abstract MIBDefinition [] queryMibs(String oid);

    /**
     * Makes a query for all MIB objects matching the oid parameter, as
     * well as any descendants in the MIB tree, in a specific sub-device
     * including eSTB and eCM.  If the object to be searched
     * for is a leaf the trailing ".0" must be included for an exact match.
     * A query for a leaf object SHALL return just that object if found.  A
     * query for a non-leaf OID SHALL return all MIB objects below that OID.
     * Existing leaf and table items SHALL be included in the results;
     * branch-nodes without data SHALL NOT.  For example; If a query is for
     * OID 1.2.3.4 then all table items and leafs below that OID are returned.
     * If OIDs 1.2.3.4.1 and 1.2.3.4.2 are the only items below the query object
     * they would be returned.  The query SHALL NOT return items outside the
     * OID.  For example; if 1.2.3.4 is the query OID then 1.2.3.5 is not
     * returned.</p><p>
     *
     * If ECM_SUBDEVICE is the subDevice argument value the implementation SHALL
     * allow eCM MIB query in an implementation specific fashion.  The Host
     * device may restrict eCM MIB access to a particular subset of MIB objects;
     * see Host CFR for required eCM MIB support.
     *
     * @param subDevice The sub-device to read the MIB from.  Using
     *      ESTB_SUBDEVICE is equivalent to calling
     *      <code>queryMibs(String)</code>.
     * @param oid The object identifier to search for.  The format of the
     *      string is based on the format defined by RFC 2578 for
     *      OBJECT IDENTIFIER definition.  Terms in the string
     *      are period delimited, e.g. "1.3.6.1.4.1".
     *
     * @return An array of MIB definitions.  The array is lexographically ordered
     *      by increasing value of OID with the lowest value in the first element
     *      of the array.
     *
     * @throws IllegalArgumentException if the subDevice is not one of the
     *      ECM_SUBDEVICE or EHOST_SUBDEVICE.
     */
    public abstract MIBDefinition [] queryMibs(int subDevice, String oid);

    /**
     * Sets a Host MIB object.  The OID in the MIBObject parameter must reference
     * a writable MIB object supported by the implementation. </p><p>
     * 
     * Note; This method can only set writable MIBs in the Host device
     * (i.e. ESTB_SUBDEVICE). Writable MIBs for other sub-devices such as
     * CableCARD and eCM cannot be set using this method.</p>
     * 
     * @param mibToSet The MIB to set.  The OID in the MIBObject SHALL be used to
     *      replace values with the data array in the parameter.
     * 
     * @throws IllegalArgumentException if the OID in the MIBOject parameter does
     *      not reference a supported Host MIB or if the data attribute in the MIBObject
     *      parameter is not in the correct ASN.1 form for the OID.
     * @throws SecurityException if the MIB is not writable or if the calling
     *      application does not have MonitorAppPermission("diagnostics").
     */
    public abstract void setMIBObject(MIBObject mibToSet);
}
