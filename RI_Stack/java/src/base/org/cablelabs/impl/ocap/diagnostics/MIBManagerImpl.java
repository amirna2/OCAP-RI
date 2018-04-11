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
/**
 * @author Alan Cohn
 */
package org.cablelabs.impl.ocap.diagnostics;

import org.apache.log4j.Logger;

import org.ocap.diagnostics.MIBDefinition;
import org.cablelabs.impl.ocap.diagnostics.MIBDefinitionExt;
import org.ocap.diagnostics.MIBListener;
import org.ocap.diagnostics.MIBManager;
import org.ocap.diagnostics.MIBObject;
import org.ocap.diagnostics.SNMPRequest;
import org.ocap.diagnostics.SNMPResponse;

import org.cablelabs.impl.ocap.diagnostics.SNMPRequestImpl;

import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

import org.cablelabs.impl.manager.snmp.MIBRouter;
import org.cablelabs.impl.manager.snmp.MIBRouterImpl;
import org.cablelabs.impl.manager.snmp.OIDDelegationInfo;
import org.cablelabs.impl.manager.SNMPManager;
import org.cablelabs.impl.manager.snmp.OIDDelegationListener;
import org.cablelabs.impl.manager.snmp.SNMPManagerImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import org.cablelabs.impl.snmp.AgentXClient;
import org.cablelabs.impl.snmp.MIBDelegate;
import org.cablelabs.impl.snmp.OIDAmbiguityException;
import org.cablelabs.impl.snmp.SNMPTrapException;
import org.cablelabs.impl.snmp.SNMPClient;
import org.cablelabs.impl.snmp.SNMPBadValueException;
import org.cablelabs.impl.snmp.SNMPValueBitString;
import org.cablelabs.impl.snmp.SNMPValueError;

public class MIBManagerImpl extends MIBManagerExt implements OIDDelegationListener
{
    // member variables
    private static final Logger log = Logger.getLogger(MIBManagerImpl.class);

    static private final MIBManagerImpl instance = new MIBManagerImpl(null);

    static private Object lock = new Object();

    private static SNMPManager snmpManagerInst = null;

    private static MIBRouter mibRouterInst = null;

    private static AgentXClient agentXClient = null;

    private static Hashtable registeredOids = new Hashtable();

    private static CallerContextManager ctx = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    // constructor
    public MIBManagerImpl(MIBRouter router)
    {
        if (null == router)
        {
            snmpManagerInst = (SNMPManager) ManagerManager.getInstance(SNMPManager.class);
            if (null != snmpManagerInst)
            {
                mibRouterInst = snmpManagerInst.getMibRouter();
            }
            else
            {
                mibRouterInst = MIBRouterImpl.getInstance();
            }

            if (log.isDebugEnabled())
            {
                log.debug("MIBManagerImpl constructor called, "
                        + (null == snmpManagerInst ? "no SNMP Mgr" : "got SNMP Mgr"));
            }
        }
        else
        {
            mibRouterInst = router;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Calling the AgentXClient constructor");
        }

        agentXClient = new AgentXClient((MIBDelegate)mibRouterInst);

        if (log.isDebugEnabled())
        {
            log.debug("agentXClient = " + agentXClient);
        }
    }

    /**
     * Gets the MIBManagerImpl.
     * 
     * @param router - the router to use, or null if the system is to obtain
     * one from SNMPManager.
     * 
     * @return The MIBManagerImpl.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("diagnostics").
     */
    public static MIBManagerImpl getInstance(MIBRouter router)
    {
        // Check with security manager to verify application has access to
        // diagnostics api
        SecurityUtil.checkPermission(new MonitorAppPermission("diagnostics"));

        return instance;
    }

    /**
     * Add the oid and listener to the registeredOids hash table if not present.
     * If oid already present then throw an IllegalArgumentException exception.

     * @see org.ocap.diagnostics.MIBManager#registerOID(java.lang.String, int, boolean, int, org.ocap.diagnostics.MIBListener)
     *
     * @param MIBListener
     *            listener
     * @param String
     *            oid
     * @param int data type
     * @param boolean leaf
     */
    private synchronized void addContext(MIBListener listener, String oid, int dataType, boolean leaf, int access) throws IllegalArgumentException
    {
        MIBListenerData mibData = new MIBListenerData();
        mibData.listener = listener;
        mibData.oid = oid;
        mibData.context = getContext(); // get callers context
        mibData.dataType = dataType;
        mibData.leaf = leaf;
        mibData.access = access;

        // atomically register OID with both MIBRouter and AgentXClient
        try
        {
            // try registering OID with MIBRouter first
            mibRouterInst.registerOidDelegate(oid, (leaf ? OIDDelegationInfo.DELEGATE_TYPE_LEAF
                    : OIDDelegationInfo.DELEGATE_TYPE_TABLE), this, mibData);

            // now try the sub-agent registration
            boolean agentXRegisterSucceeded = false;

            try
            {
                agentXRegisterSucceeded = agentXClient.registerOid(oid);
            }
            catch (IllegalArgumentException e)
            {
                // registration failed in agentX: need to unregister with the MIBRouter
                mibRouterInst.unregisterOidDelegate(oid);
                throw e;
            }

            if (!agentXRegisterSucceeded)
            {
                // registration still failed in agentX: still need to unregister with the MIBRouter
                mibRouterInst.unregisterOidDelegate(oid);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("agentXRegisterSucceeded for OID: " + oid);
                }

                // success: save oid and MIB Data
                registeredOids.put(oid, mibData);
            }
        }
        catch (OIDAmbiguityException e)
        {
            throw new IllegalArgumentException("MIBRouter registration OIDAmbiguityException: " + e);
        }
    }

    /**
     * Remove the oid from the registeredOids hash table.
     *
     * @param String
     *            oid
     */
    private synchronized void removeContext(String oid)
    {
        registeredOids.remove(oid); // remove from hash table

        mibRouterInst.unregisterOidDelegate(oid); // tell mib router to remove
        agentXClient.unregisterOid(oid, 0); // tell agentXClient to remove
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ocap.diagnostics.MIBManager#queryMibs(java.lang.String)
     */
    public MIBDefinition[] queryMibs(String oid)
    {
        MIBDefinitionExt[] mibDefArray = new MIBDefinitionExt[0]; //default return value

        // Conditionally fake out the MOCA MIB
        String envVal = MPEEnv.getEnv("OCAP.guides.enableMOCA");
        if (envVal != null && "true".equalsIgnoreCase(envVal) && "1.3.6.1.4.1.31621.1.1.1.1.1.1.3".equals(oid))
        {
            MIBDefinition[] retVal = new MIBDefinition[1];
            retVal[0] = new MIBDefinition() {
                public int getDataType()
                {
                    return MIBDefinition.SNMP_TYPE_BITS;
                }

                public MIBObject getMIBObject()
                {
                    return new MIBObject("1.3.6.1.4.1.31621.1.1.1.1.1.1.3",
                                         new byte[] {0x0, 0x0, 0x0, 0x1});
                }
            };
            return retVal;
        }

        // return if oid is null or no SNMP Manager
        if (null == oid || null == mibRouterInst)
        {
            return mibDefArray;
        }

        if (log.isDebugEnabled())
        {
            log.debug("MIBManagerImpl::queryMibs("+oid+")");
        }


        /* The OID can either be registered with the MIBRouter (for Stack or Client MIBs) or
           in the Platform where it is queried using SNMPClient.
           First see if it has been registered with the MIBRouter...
         */
        if (mibRouterInst.isOidAdded(oid) == false)
        {
            if (log.isDebugEnabled())
            {
                log.debug(oid + " has not been registered by a Client, or by the Stack/MPE Mibs so ask the Platform SNMP Master");
            }
            /* The OID has not been registered by a Client, or by the Stack/MPE Mibs
               so ask the Platform SNMP Master. If that fails to get the value, return
               an empty array.
             */
            try
            {
                mibDefArray = SNMPClient.getInstance().getMIBDefinition(oid);
            }
            catch (IllegalArgumentException e)
            {
                // leave mibDefArray as default empty array
                if (log.isDebugEnabled())
                {
                    log.debug("MIBManagerImpl queryMibs IllegalArgumentException for OID " + oid + " Exception " + e);
                }
            }
            catch (IOException e)
            {
                // leave mibDefArray as default empty array
                if (log.isDebugEnabled())
                {
                    log.debug("MIBManagerImpl queryMibs IOException for OID " + oid + "Exception " + e);
                }
            }
        }
        else
        {
            /* Shortcut the process by getting the MIBDefinitions from the MIBRouter directly
               (Stack or Client registered MIBs).
             */
            try
            {
                mibDefArray = mibRouterInst.queryMIBRouter(oid);
            }
            catch (IllegalArgumentException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("MIB queryMibs IllegalArgumentException for OID " + oid + "Exception " + e);
                }
            }
            catch (IOException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("MIB queryMibs IOException for OID " + oid + "Exception " + e);
                }
            }
        }

        if (mibDefArray.length == 1 && mibDefArray[0].getMIBObject().getOID().equals(oid) && !oid.endsWith(".0"))
        {
            // queryMibs: 'A query for a non-leaf OID SHALL return all MIB objects below that OID.'
            // therefore a request for a non-leaf table cell should not return the value of the table cell
            mibDefArray = new MIBDefinitionExt[0];
        }

        return mibDefArray;
    }

    public void sendTrap(String oid, MIBObject[] data) throws SNMPTrapException
    {
        agentXClient.sendTrap(oid, data);
    }

    public void registerOID(String oid, int access, int dataType, MIBListener listener) throws IllegalArgumentException
    {
        registerOID(oid, access, isLeaf(oid), dataType, listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.diagnostics.MIBManager#registerOID(java.lang.String, int,
     * boolean, int, org.ocap.diagnostics.MIBListener)
     */
    public void registerOID(String oid, int access, boolean leaf, int dataType, MIBListener listener)
    {
        if (null == mibRouterInst)
        {
            return; // no snmp
        }

        if (log.isDebugEnabled())
        {
            log.debug("MIB registerOID called OID " + oid);
        }

        // Validate all parameters. Throw IllegalArgumentException when errors.

        // Validate OID form 1.2.3.4.0 or 1.2.33.1.157
        int oidLength;
        if (null == oid || (oidLength = oid.length()) == 0)
        {
            throw new IllegalArgumentException("Invalid OID param");
        }

        if (oid.charAt(0) == '.' || oid.charAt(oidLength - 1) == '.')
        {
            throw new IllegalArgumentException("Invalid OID string");
        }

        char c;
        for (int x = 0; x < oidLength; x++)
        {
            c = oid.charAt(x);
            if (c >= '0' && c <= '9')
            {
                continue;
            }
            if (c == '.')
            {
                continue;
            }
            throw new IllegalArgumentException("Invalid OID string");
        }

        // check if OID ends with ".0"
        if ((leaf && !isLeaf(oid)))
        {
            throw new IllegalArgumentException("Invalid oid leaf value");
        }

        switch (dataType)
        {// valid data types
            case MIBDefinition.SNMP_TYPE_INTEGER:
            case MIBDefinition.SNMP_TYPE_BITS:
            case MIBDefinition.SNMP_TYPE_COUNTER32:
            case MIBDefinition.SNMP_TYPE_COUNTER64:
            case MIBDefinition.SNMP_TYPE_GAUGE32:
            case MIBDefinition.SNMP_TYPE_IPADDRESS:
            case MIBDefinition.SNMP_TYPE_OBJECTID:
            case MIBDefinition.SNMP_TYPE_OCTETSTRING:
            case MIBDefinition.SNMP_TYPE_OPAQUE:
            case MIBDefinition.SNMP_TYPE_TIMETICKS:
                break;
            default:// invalid
                throw new IllegalArgumentException("Invalid dataType param");
        }

        if (access != MIBManager.MIB_ACCESS_READONLY && access != MIBManager.MIB_ACCESS_READWRITE
                && access != MIBManager.MIB_ACCESS_WRITEONLY)
        {
            throw new IllegalArgumentException("Invalid access param");
        }

        if (null == listener)
        {
            throw new IllegalArgumentException("Invalid listener param");
        }

        // Error if OID is already known
        if (true == registeredOids.containsKey(oid))
        {
            throw new IllegalArgumentException("oid already registered");
        }

        addContext(listener, oid, dataType, leaf, access);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ocap.diagnostics.MIBManager#setMIBObject(org.ocap.diagnostics.MIBObject
     * )
     */
    public void setMIBObject(MIBObject mibToSet) throws IllegalArgumentException
    {
        boolean illegalArgExceptionThrown = false;
        IllegalArgumentException iae = null;
        // Check with security manager to verify application has access to
        // Diagnostics API
        SecurityUtil.checkPermission(new MonitorAppPermission("diagnostics"));

        String oid = mibToSet.getOID();

        if (log.isDebugEnabled())
        {
            log.debug("MIB setMIBObject called for OID " + oid);
        }

        // Make sure there is a MIBRouter
        if (null != mibRouterInst)
        {
            try
            {
                if (SNMPValueBitString.isBITS(mibToSet))
                {
                    // an SNMP_TYPE_BITS (not supported by AgentX) should be converted into an SNMP_TYPE_OCTETSTRING
                    mibToSet = SNMPValueBitString.bitsToOctet(mibToSet);
                }
            }
            catch (SNMPBadValueException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("SNMPClient setMIBObject failed to process MIBObject with OID " + oid + ": " + e);
                }
            }

            // Set using master SNMP if not registered with MIBRouter (i.e not Client/Stack MIB)
            if (mibRouterInst.isOidAdded(oid) == false)
            {
                try
                {
                    SNMPClient.getInstance().setMIBValue(oid, mibToSet.getData());
                }
                catch (IllegalArgumentException e2)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("SNMPClient setMIBObject IllegalArgumentException [" + e2 + "] for OID " + oid);
                    }

                    throw e2;
                }
                catch (IOException e2)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("SNMPClient setMIBObject IOException [" + e2 + "] for OID " + oid);
                    }

                    // convert I/O Exception to Security Exception to satisfy
                    // CTP MIBManager 150 assertion that an attempt to write a
                    // read-only MIB will throw a SecurityException...
                    throw new SecurityException(e2.toString());
                }
            }
            // Otherwise set using the shortcut via MIBRouter, no need to go through master SNMP.
            else
            {
                // Obtain the MIBListener in the Client or the Stack
                MIBListenerData mdata = (MIBListenerData) registeredOids.get(oid);
                if (null == mdata)
                {
                    // oid not registered, attempt to set anyway
                    try
                    {
                        mibRouterInst.setMIBValue(oid, mibToSet.getData());
                    }
                    catch (IllegalArgumentException e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("MIB setMIBObject IllegalArgumentException for OID " + oid);
                        }

                        throw e;
                    }
                }
                else
                {
                    // get callback method and listeners context
                    final MIBListener listner = mdata.listener;
                    CallerContext ctx = mdata.context;

                    // create mibObject with oid and any value byte array, and the
                    // SNMPRequest
                    MIBObject mibObject = new MIBObject(oid, mibToSet.getData());
                    final SNMPRequest request = new SNMPRequestImpl(SNMPRequest.SNMP_SET_REQUEST, mibObject);

                    // return if context is not alive
                    if (!ctx.isAlive())
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("MIB setMIBObject caller context not alive.");
                        }
                    }
                    else
                    {
                        // make callback in callers context
                        try
                        {
                            ctx.runInContextSync(new Runnable()
                            {
                                public void run()
                                {
                                    listner.notifySNMPRequest(request);
                                }
                            });
                        }
                        catch (SecurityException e)
                        {
                        }
                        catch (IllegalStateException e)
                        {
                        }
                        catch (InvocationTargetException e)
                        {
                        }
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ocap.diagnostics.MIBManager#unregisterOID(java.lang.String)
     */
    public void unregisterOID(String oid)
    {
        if (null == mibRouterInst)
        {
            return;
        }

        if (null == oid || oid.length() == 0)
        {
            throw new IllegalArgumentException("Invalid OID");
        }

        if (log.isDebugEnabled())
        {
            log.debug("MIB unregisterOID called OID " + oid);
        }

        CallerContext ctx = getContext();
        MIBListenerData mdata = (MIBListenerData) registeredOids.get(oid);
        if (null == mdata || mdata.context != ctx)
        {
            throw new IllegalArgumentException("Invalid OID Context");
        }

        removeContext(oid);
    }

    /**
     * Common method used to determine the current CallerContext.
     *
     * @return the CallerContextManager
     */
    private CallerContext getContext()
    {
        return ctx.getCurrentContext();
    }

    /**
     * Determine if the oid is a leaf or branck
     *
     *@param String
     *            oid
     *@return true if oid ends with ".0"
     */
    private boolean isLeaf(String oid)
    {
        int length = oid.length();

        if (length < 2)
        {
            return false;
        }

        return (oid.charAt(length - 2) == '.' && oid.charAt(length - 1) == '0');
    }

    /*
     * This public method is for testing purposes only.
     */
    public MIBListenerData getRegisteredOids(String oid)
    {
        return (MIBListenerData) registeredOids.get(oid);
    }

    /**
     *
     * Used to define the caller context
     *
     */
    private static class ExecInContext implements Runnable
    {
        private SNMPResponse response;
        private MIBListener listener;
        private SNMPRequest request;

        public ExecInContext(MIBListener l, SNMPRequest req)
        {
           listener = l;
           request = req;
        }

        public void run()
        {
           response = listener.notifySNMPRequest(request);
        }

        public SNMPResponse getResponse()
        {
           return response;
        }
     }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.manager.snmp.OIDDelegationListener#notifyOidValueRequest
     * (org.cablelabs.impl.manager.snmp.OIDDelegationInfo)
     */
    public SNMPResponseExt notifyOidValueRequest(OIDDelegationInfo info)
    {
        int requestType = info.requestType;
        String oid = info.oid;
        byte[] setValue = info.setValue;
        MIBListenerData mibData = (MIBListenerData) info.userObject;

        SNMPResponseExt response = null;
        try
        {
            response = new SNMPResponseExt(oid, SNMPValueError.NULL);
        }
        catch (SNMPBadValueException e)
        {
        }

        if (log.isDebugEnabled())
        {
            log.debug("MIB notifyOidValueRequest called for OID " + oid + " - request type: " + info.getRequestTypeString());
        }

        final MIBListener listner = mibData.listener;
        CallerContext ctx = mibData.context;

        MIBObject mibObject = new MIBObject(oid, setValue);
        final SNMPRequest request = new SNMPRequestImpl(requestType, mibObject);

        SNMPResponse resp = null;
        try
        {
           ExecInContext exec = new ExecInContext(listner, request);
           ctx.runInContextSync(exec);
           resp = exec.getResponse();
        }
        catch (SecurityException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("MIB notifyOidValueRequest SecurityException: " + e);
            }
        }
        catch (IllegalStateException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("MIB notifyOidValueRequest IllegalStateException: " + e);
            }
        }
        catch (InvocationTargetException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("MIB notifyOidValueRequest InvocationTargetException: " + e);
            }
        }

        if (null == resp)
        {
            if (log.isErrorEnabled())
            {
                log.error("MIB notifyOidValueRequest MIBListener returned a null pointer");
            }
        }
        else
        {
            try
            {
                response = new SNMPResponseExt(resp);
            }
            catch (SNMPBadValueException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("MIB notifyOidValueRequest SNMPBadValueException: " + e);
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("MIB notifyOidValueRequest response returned from listener with OID " + response.getOID());
            }
        }

        return response;
    } // notifyOidValueRequest()

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
    public MIBDefinition[] queryMibs(int source, String oid)
    {
        MIBDefinitionExt def[] = null;
        if (ESTB_SUBDEVICE == source)
        {
            def = (MIBDefinitionExt[]) queryMibs(oid);
        }
        else if (ECM_SUBDEVICE == source)
        {
            try
            {
                def = SNMPClient.getInstance(ECM_SUBDEVICE).getMIBDefinition(oid);
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error occured while getting MIBdef for OID= " + oid + ", host = " + source, e);
                }

                def = new MIBDefinitionExt[0];
            }
        }
        return def;
    }
}
