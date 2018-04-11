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
import java.util.Collection;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.snmp.OIDDelegationInfo;
import org.cablelabs.impl.manager.snmp.OIDDelegationListener;
import org.cablelabs.impl.ocap.diagnostics.SNMPResponseExt;

public class DelegatorMIBImpl implements DelegatorMIB
{
    private static final Logger log = Logger.getLogger(DelegatorMIBImpl.class);

    private final OIDMap mapRegOidsToDelegates = new OIDMap(true);

    public String[] getOIDs()
    {
        synchronized (mapRegOidsToDelegates)
        {
            return mapRegOidsToDelegates.getOIDs();
        }
    }

    public ArrayList getDelegatedOidsInTree(String rootOid)
    {
        SortedMap parentAndChildren = null;

        synchronized (mapRegOidsToDelegates)
        {
            // children is a thread-safe copy.
            parentAndChildren = mapRegOidsToDelegates.getParentAndChildren(new OID(rootOid));
        }

        Collection infos = parentAndChildren.values();

        if (infos.isEmpty())
        {
            return new ArrayList(); // empty
        }
        else
        {
            return new ArrayList(infos);
        }
    }

    public void registerOidDelegate(String oid, int delegateType, OIDDelegationListener listener, Object userObject)
            throws IllegalArgumentException, OIDAmbiguityException
    {
        synchronized (mapRegOidsToDelegates)
        {
            OID oidO = new OID(oid);
            OIDDelegationInfo info = new OIDDelegationInfo(oid, delegateType, OIDDelegationInfo.SNMP_REQUEST_UNKNOWN,
                    null, listener, userObject);
            mapRegOidsToDelegates.put(oidO, info);

            mapRegOidsToDelegates.printContents("DelegatorMIBImpl.registerOidDelegate:  mapRegOidsToDelegates after reg "
                    + oid);
        }
    }

    public void unregisterOidDelegate(String oid)
    {
        synchronized (mapRegOidsToDelegates)
        {
            OID oidO = new OID(oid);
            mapRegOidsToDelegates.remove(oidO);
        }
        mapRegOidsToDelegates.printContents("DelegatorMIBImpl.unregisterOidDelegate: mapRegOidsToDelegates after unreg "
                + oid);
    }

    public void unregisterOidDelegate(OIDDelegationListener listener)
    {
        synchronized (mapRegOidsToDelegates)
        {
            ArrayList valueList = mapRegOidsToDelegates.getValues();
            int valueListSize = valueList.size();
            for (int i = 0; i < valueListSize; i++)
            {
                OIDDelegationInfo info = (OIDDelegationInfo) valueList.get(i);
                if (info.listener.equals(listener))
                {
                    OID oidO = new OID(info.oid);
                    mapRegOidsToDelegates.remove(oidO);
                }
            }
            mapRegOidsToDelegates.printContents("DelegatorMIBImpl.unregisterOidDelegate: mapRegOidsToDelegates after unreg "
                    + listener);
        }
    }

    private abstract class OIDDelegateActioner
    {
        public abstract SNMPResponseExt doAction(OIDDelegationInfo info, String oid, byte[] setData);
    }

    private SNMPResponseExt actionMIBRequest(String oid, byte[] setData, OIDDelegateActioner actioner)
    {
        // generate an empty SNMPReponseExt to pass back if the access attempt fails
        SNMPResponseExt response = null;
        try
        {
            response = new SNMPResponseExt(oid, SNMPValueError.NULL);
        }
        catch (SNMPBadValueException e) {}

        OIDDelegationInfo registeredInfo;
        // Find the registered OIDDelegationInfo for the requested oid
        if (null != (registeredInfo = (OIDDelegationInfo)mapRegOidsToDelegates.getRegisteredObject(oid)))
        {
            // perform the appropriate action on the OIDDelegationInfo
            response = actioner.doAction(registeredInfo, oid, setData);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("actionMIBRequest: failed to find " + oid + " in mapRegOidsToDelegates");
            }
        }

        return response;
    }

    public SNMPResponseExt getMIBValue(String oid)
    {
        return actionMIBRequest(oid, null, new OIDDelegateActioner()
        {
            public SNMPResponseExt doAction(OIDDelegationInfo info, String oid, byte[] setData)
            {

                OIDDelegationInfo notifyInfo = new OIDDelegationInfo(oid, OIDDelegationInfo.DELEGATE_TYPE_NA,
                    OIDDelegationInfo.SNMP_GET_REQUEST, null,
                    info.listener, info.userObject);

                if (log.isDebugEnabled())
                {
                    log.debug("DelegatorMIB get on = " + oid);
                }
                return info.listener.notifyOidValueRequest(new OIDDelegationInfo(notifyInfo));
            }
        });
    }

    public SNMPResponseExt getNextMIBValue(String oid)
    {
        return actionMIBRequest(oid, null, new OIDDelegateActioner()
        {
            public SNMPResponseExt doAction(OIDDelegationInfo info, String oid, byte[] setData)
            {
                OIDDelegationInfo notifyInfo = new OIDDelegationInfo(oid, OIDDelegationInfo.DELEGATE_TYPE_NA,
                    OIDDelegationInfo.SNMP_GET_NEXT_REQUEST, null,
                    info.listener, info.userObject);

                if (log.isDebugEnabled())
                {
                    log.debug("DelegatorMIB get next on = " + oid);
                }
                return info.listener.notifyOidValueRequest(new OIDDelegationInfo(notifyInfo));
            }
        });
    }

    public SNMPResponseExt setMIBValue(String oid, byte[] setData)
    {
        return actionMIBRequest(oid, setData, new OIDDelegateActioner()
        {
            public SNMPResponseExt doAction(OIDDelegationInfo info, String oid, byte[] setData)
            {

                OIDDelegationInfo notifyInfo = new OIDDelegationInfo(oid, OIDDelegationInfo.DELEGATE_TYPE_NA,
                    OIDDelegationInfo.SNMP_SET_REQUEST, setData,
                    info.listener, info.userObject);

                if (log.isDebugEnabled())
                {
                    log.debug("DelegatorMIB set on = " + oid);
                }
                return info.listener.notifyOidValueRequest(new OIDDelegationInfo(notifyInfo));
            }
        });
    }

    public SNMPResponseExt testSetMIBValue(String oid, byte[] setData)
    {
        return actionMIBRequest(oid, setData, new OIDDelegateActioner()
        {
            public SNMPResponseExt doAction(OIDDelegationInfo info, String oid, byte[] setData)
            {

                OIDDelegationInfo notifyInfo = new OIDDelegationInfo(oid, OIDDelegationInfo.DELEGATE_TYPE_NA,
                    OIDDelegationInfo.SNMP_CHECK_FOR_SET_REQUEST, setData,
                    info.listener, info.userObject);

                if (log.isDebugEnabled())
                {
                    log.debug("DelegatorMIB test set on = " + oid);
                }
                return info.listener.notifyOidValueRequest(new OIDDelegationInfo(notifyInfo));
            }
        });
    }
}
