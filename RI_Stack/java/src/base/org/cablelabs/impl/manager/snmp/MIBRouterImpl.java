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
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;
import org.cablelabs.impl.snmp.DelegatorMIB;
import org.cablelabs.impl.snmp.DelegatorMIBImpl;
import org.cablelabs.impl.snmp.MIBDelegate;
import org.cablelabs.impl.snmp.MIBTransaction;
import org.cablelabs.impl.snmp.MIBTransactionManager;
import org.cablelabs.impl.snmp.OID;
import org.cablelabs.impl.snmp.OIDAmbiguityException;
import org.cablelabs.impl.snmp.OIDMap;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValueError;

import org.cablelabs.impl.ocap.diagnostics.MIBDefinitionExt;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPResponse;

public class MIBRouterImpl implements MIBRouter, MIBDelegate, MIBTransactionManager
{
    private static final Logger log = Logger.getLogger(MIBRouterImpl.class);

    private static final MIBRouterImpl singleton = new MIBRouterImpl();
    private Object lock = new Object(); // Lock object
    private OIDMap mapOidsToMib;
    private ArrayList mibList;
    private DelegatorMIB delegatorMIB = new DelegatorMIBImpl();

    private MIBRouterImpl()
    {
        mapOidsToMib = new OIDMap(true);
    }

    public static MIBRouter getInstance()
    {
        return singleton;
    }

    public boolean isMIBAdded(MIB mib)
    {
        synchronized (lock)
        {
            return mibList.contains(mib);
        }
    }

    public void addMIB(MIB mib) throws IllegalArgumentException, OIDAmbiguityException, IllegalStateException
    {
        synchronized (lock)
        {
            mibList.add(mib);
            String[] oids = mib.getOIDs();

            if (oids.length == 0)
            {
                throw new IllegalStateException("MIB has no OIDs: " + mib);
            }

            for (int i = 0; i < oids.length; i++)
            {
                String oid = oids[i];

                putOidInMap(oid, mib, "MIBRouterImpl.addMIB -- mapOidsToMib");
            }
        }
    }

    private void putOidInMap(String oid, MIB mib, String title) throws OIDAmbiguityException
    {
        OID.isWellFormed(oid);

        OID oidO = new OID(oid);

        synchronized (mapOidsToMib)
        {
            mapOidsToMib.put(oidO, mib);
            mapOidsToMib.printContents(title);
        }

    }

    public void removeMIB(MIB mib)
    {
        synchronized (lock)
        {
            if (mibList.remove(mib))
            {
                removeOidsFromMap(mib);
            }
        }
    }

    private void removeOidsFromMap(MIB mib)
    {

        String[] oids = mib.getOIDs();
        for (int i = 0; i < oids.length; i++)
        {
            String oid = oids[i];

            if (log.isDebugEnabled())
            {
                log.debug("unregisterOidDelegate: oid= " + oid);
            }

            OID oidO = new OID(oid);
            mapOidsToMib.remove(oidO);
        }
    }

    /**
     * Implementation of {@link MIBRouter} interface.
     *
     * @param oid the requested OID - this may be the base OID of a subtree
     * or table or may directly reference an SNMP value
     * @return an array of MIB data representing the sub tree below and including the requested OID
     *
     */
    public MIBDefinitionExt[] queryMIBRouter(String requestedOid) throws IllegalArgumentException, IOException
    {
        List resultsList = new ArrayList();

        MIB registeredMIB = (MIB)(mapOidsToMib.getRegisteredObject(requestedOid));

        if (registeredMIB == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("queryMIBRouter: no MIBs have been registered that are responsible for oid " + requestedOid);
            }
        }
        else
        {
            SNMPResponseExt response = null;

            if (log.isDebugEnabled())
            {
                log.debug("queryMIBRouter: attempting get for oid " + requestedOid + " in MIB registered at " + registeredMIB);
            }

            // attempt a get on the registered MIB for the requested OID
            response = registeredMIB.getMIBValue(requestedOid);

            boolean moreMIBtoFind = true;
            String lastOIDRequest = response.getOID();

            if (SNMPResponse.SNMP_REQUEST_SUCCESS == response.getStatus())
            {
                resultsList.add(response.getMIBDefiniton());
            }
            else
            {
                // requested OID must be the base OID of a sub-tree/table/column: walk through it with get next requests
                while (moreMIBtoFind)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("queryMIBRouter: mib walking: get next requested for " + response.getOID());
                    }

                    // use the OID returned in the response of the last access
                    response = registeredMIB.getNextMIBValue(response.getOID());

                    if (SNMPResponse.SNMP_REQUEST_SUCCESS != response.getStatus())
                    {
                        // stop the walk if get next was unsuccessful
                        moreMIBtoFind = false;
                        if (log.isDebugEnabled())
                        {
                            log.debug("queryMIBRouter: mib walk stopped: response status = " + response.getStatus());
                        }
                    }
                    else if (lastOIDRequest.equals(response.getOID()))
                    {
                        // stop the walk if the response OID is identical to requested OID
                        moreMIBtoFind = false;
                        if (log.isErrorEnabled())
                        {
                            log.error("queryMIBRouter: mib walk stopped: get next is returned the same OID: " + lastOIDRequest);
                        }
                    }
                    else if(OID.outOfScope(requestedOid, response.getOID()))
                    {
                        // stop the walk if the next OID String is beyond the scope of the requested OID String
                        moreMIBtoFind = false;
                        if (log.isDebugEnabled())
                        {
                            log.debug("queryMIBRouter: mib walk stopped: next OID: " + response.getOID() + " is out of scope of the requested OID: " + requestedOid);
                        }
                    }
                    else
                    {
                        // otherwise add this result to the list and continue with the walk
                        resultsList.add(response.getMIBDefiniton());
                        lastOIDRequest = response.getOID();
                        if (log.isDebugEnabled())
                        {
                            log.debug("queryMIBRouter: mib walk continues");
                        }
                    }
                }
            }
        }

        /* Package the Stack/MPE or Client Value into a MIBDefinition array */
        return (MIBDefinitionExt[])(resultsList.toArray(new MIBDefinitionExt[resultsList.size()]));
    }

    /**
     * Implementation of {@link MIBRouter} interface.
     *
     */
    public void setMIBRouterValue(String oid, byte[] value) throws IllegalArgumentException, IOException
    {
        SortedMap map = mapOidsToMib.getRegiteredTree(oid);

        if (map.size() == 0)
        {
            throw new IllegalArgumentException("OID not found");
        }
        if (map.size() > 1)
        {
            throw new IllegalArgumentException("OID is a tree.  Can only set leaves or table values");
        }

        setMIBValue(oid, value);
    }

    public boolean isOidAdded(String oid)
    {
        return (mapOidsToMib.getRegisteredObject(oid) == null ? false : true);
    }

    public void performTransaction(MIBTransaction transaction)
    {
        synchronized (lock)
        {
            transaction.executeTransaction();
        }
    }

    public MIBTransactionManager getTransactionManager()
    {
        return this;
    }

    public void registerOidDelegate(String oid, int delegateType, OIDDelegationListener listener, Object userObject)
        throws OIDAmbiguityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("registerOidDelegate: oid= " + oid);
        }

        synchronized (lock)
        {
            putOidInMap(oid, delegatorMIB, "MIBRouterImpl.registerOidDelegate -- mapOidsToMib");
            this.delegatorMIB.registerOidDelegate(oid, delegateType, listener, userObject);
        }
    }

    public void unregisterOidDelegate(OIDDelegationListener listener)
    {
        synchronized(lock)
        {
            removeOidsFromMap(this.delegatorMIB);
            this.delegatorMIB.unregisterOidDelegate(listener);
        }
    }

    public void unregisterOidDelegate(String oid)
    {
        if (log.isDebugEnabled())
        {
            log.debug("unregisterOidDelegate: oid= " + oid);
        }

        synchronized (lock)
        {
            // unregister OID from delegator MIB if it is there
            delegatorMIB.unregisterOidDelegate(oid);

            // unregister the MIB object
            mapOidsToMib.remove(new OID(oid));
       }
    }

    private abstract class OIDActioner
    {
        public abstract SNMPResponseExt doAction(MIB mib, String oid, byte[] setData);
    }

    private SNMPResponseExt actionMIBRequest(String requestedOid, byte[] setValue, OIDActioner actioner)
    {
        // generate an empty SNMPReponseExt to pass back if the access attempt fails
        SNMPResponseExt response = null;

        MIB registeredMIB = (MIB)mapOidsToMib.getRegisteredObject(requestedOid);
        // Find the registered MIB for the requested oid.  It may be a PropetiesMIB or a DelegatorMIB
        if (null != registeredMIB)
        {
            // perform the correct action on the MIB
            response = actioner.doAction(registeredMIB, requestedOid, setValue);
        }

        if (null == response)
        {
            try
            {
                response = new SNMPResponseExt(requestedOid, SNMPValueError.NULL);
            }
            catch (SNMPBadValueException e)
            {
            }

            if (log.isDebugEnabled())
            {
                log.debug("failed to find a registered object for MIB oid: " + requestedOid);
            }
        }

        return response;
    }

    /**
     * Implementation of {@link MIBValueAccess} interface
     */
    public SNMPResponseExt getMIBValue(String oid)
    {
        return actionMIBRequest(oid, null, new OIDActioner()
        {
            public SNMPResponseExt doAction(MIB mib, String requestedOid, byte[] setData)
            {
                return mib.getMIBValue(requestedOid);
            }
        });
    }

    /**
     * Implementation of {@link MIBValueAccess} interface
     */
    public SNMPResponseExt getNextMIBValue(String oid)
    {
        return actionMIBRequest(oid, null, new OIDActioner()
        {
            public SNMPResponseExt doAction(MIB mib, String requestedOid, byte[] setData)
            {
                return mib.getNextMIBValue(requestedOid);
            }
        });
    }

    /**
     * Implementation of {@link MIBValueAccess} interface
     */
    public SNMPResponseExt setMIBValue(String oid, byte[] setData)
    {
        return actionMIBRequest(oid, setData, new OIDActioner()
        {
            public SNMPResponseExt doAction(MIB mib, String requestedOid, byte[] setData)
            {
                return mib.setMIBValue(requestedOid, setData);
            }
        });
    }

    /**
     * Implementation of {@link MIBValueAccess} interface
     */
    public SNMPResponseExt testSetMIBValue(String oid, byte[] setData)
    {
        return actionMIBRequest(oid, setData, new OIDActioner()
        {
            public SNMPResponseExt doAction(MIB mib, String requestedOid, byte[] setData)
            {
                return mib.testSetMIBValue(requestedOid, setData);
            }
        });
    }

    /**
     * Implementation of {@link MIBDelegate} interface
     */
    public MIBObject get(String oid)
    {
        return getMIBValue(oid).getMIBObject();
    }

    /**
     * Implementation of {@link MIBDelegate} interface
     */
    public MIBObject getNext(String oid)
    {
        return getNextMIBValue(oid).getMIBObject();
    }

    /**
     * Implementation of {@link MIBDelegate} interface
     */
    public boolean set(String oid, byte[] setData)
    {
        return (setMIBValue(oid, setData).getStatus() == SNMPResponse.SNMP_REQUEST_SUCCESS);
    }

    /**
     * Implementation of {@link MIBDelegate} interface
     */
    public int testSet(String oid, byte[] setData)
    {
        return testSetMIBValue(oid, setData).getStatus();
    }
}
