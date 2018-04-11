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

package org.cablelabs.impl.media.access;

import javax.tv.service.navigation.StreamType;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.pod.CADecryptParams;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.manager.pod.CAElementaryStreamAuthorization;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;

/**
 * Monitors changes to the CA session.
 * <p>
 * <p/>
 * Notifies the registered {@link CASessionListener} of any updates.
 */
public class CASessionMonitor
{
    private static final Logger log = Logger.getLogger(CASessionMonitor.class);
    private static final Logger performanceLog = Logger.getLogger("Performance.ServiceSelection");

    private final CASessionListener caSessionListener;
    //null if initialized but CA descriptors not provided or service is analog
    private CASession caSession;
    private final Object lock;
    private boolean started;

    /**
     * Constructor
     *
     * @param lock
     *            the shared lock
     */
    public CASessionMonitor(Object lock, CASessionListener caSessionListener)
    {
        this.lock = lock;
        this.caSessionListener = caSessionListener;
    }

    public void startDecryptSession(ServiceDetailsExt serviceDetails, ExtendedNetworkInterface networkInterface, ServiceComponentExt[] components) throws MPEException
    {
        if (log.isDebugEnabled())
        {
            log.debug("startDecryptSession - service details: " + serviceDetails + ", components: " + Arrays.toString(components));
        }
        synchronized (lock)
        {
            int[] pids = new int[components.length];
            short[] types = new short[components.length];
            for (int i=0;i<components.length;i++)
            {
                pids[i] = components[i].getPID();
                types[i] = streamTypeToMediaStreamType(components[i].getStreamType());
            }
            // add these listeners once
            if (!started)
            {
                CADecryptParams decryptParams = new CADecryptParams(caSessionListener, serviceDetails, networkInterface.getHandle(), pids, types, CADecryptParams.SERVICECONTEXT_PRIORITY);
                PODManager podManager = (PODManager) ManagerManager.getInstance(PODManager.class);
                //null session here means no decrypt required
                if (performanceLog.isInfoEnabled())
                {
                    performanceLog.info("CA requested: Tuner " + networkInterface.getHandle() + ", Locator " + serviceDetails.getLocator().toExternalForm());
                }
                caSession = podManager.startDecrypt(decryptParams);
                 if (getLastCASessionEventID() == CASessionEvent.EventID.FULLY_AUTHORIZED)
                {
                    if (performanceLog.isInfoEnabled())
                    {
                        performanceLog.info("CA Granted: Tuner " + networkInterface.getHandle() + ", Locator " + serviceDetails.getLocator().toExternalForm());
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("startDecryptSession - already started - ignoring");
                }
            }
            started = true;
        }
    }

    public static short streamTypeToMediaStreamType(StreamType strType)
    {
        short UNKNOWN = 0x00;
        short VIDEO = 0x01;
        short AUDIO = 0x02;
        short DATA = 0x03;
        short SUBTITLES = 0x04;
        short SECTIONS = 0x05;
        short PCR = 0x06;
        short PMT = 0x07;
        
        if (strType == StreamType.VIDEO)
        {
            return VIDEO;
        }
        if (strType == StreamType.AUDIO)
        {
            return AUDIO;
        }
        if (strType == StreamType.DATA)
        {
            return DATA;
        }
        if (strType == StreamType.SUBTITLES)
        {
            return SUBTITLES;
        }
        if (strType == StreamType.SECTIONS)
        {
            return SECTIONS;
        }

        return UNKNOWN;
    }

    /**
     * Release resources. Safe to be called multiple times.
     */
    public void cleanup()
    {
        synchronized (lock)
        {
            // don't clean up if not started
            if (!started)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("cleanup called when not started - ignoring");
                }
                return;
            }
            if (log.isDebugEnabled())
            {
                log.debug("cleanup: " + this);
            }
            if (caSession != null)
            {
                caSession.stop();
                caSession = null;
            }
            started = false;
        }
    }

    public String toString()
    {
        return "CA session monitor - started: " + started + ", session: " + caSession + " - " + super.toString();
    }

    public int getLastCASessionEventID()
    {
        synchronized(lock)
        {
            if (caSession != null)
            {
                int id = caSession.getLastEvent().getEventID();
                if (log.isDebugEnabled())
                {
                    log.debug("getLastCASessionEventID - session is not null - returning: 0x" + Integer.toHexString(id) + ", session: " + caSession);
                }
                return id;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("getLastCASessionEventID - session is null - returning FULLY_AUTHORIZED");
        }
        return CASessionEvent.EventID.FULLY_AUTHORIZED;
    }

    public short getLTSID()
    {
        synchronized(lock)
        {
            if (caSession != null)
            {
                final short ltsid = caSession.getLTSID();
                if (log.isDebugEnabled())
                {
                    log.debug("getLTSID - session is not null - returning: " + ltsid + ", session: " + caSession);
                }
                return ltsid;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("getLastCASessionEventID - session is null - returning CASession.LTSID_UNDEFINED");
        }
        return CASession.LTSID_UNDEFINED;
        
    }
    
    public void populateElementaryStreamCADenialReasons(CAElementaryStreamAuthorization[] authorizations) throws MPEException
    {
        if (caSession != null)
        {
            caSession.getStreamAuthorizations(authorizations);
        }
    }
}
