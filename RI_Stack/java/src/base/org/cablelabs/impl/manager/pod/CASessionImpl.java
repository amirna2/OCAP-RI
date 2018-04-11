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

package org.cablelabs.impl.manager.pod;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.TransportStreamExt;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.util.NativeHandle;
import org.cablelabs.impl.util.SimpleCondition;

/**
 * @author Craig Pratt - EnableTV
 */
public class CASessionImpl implements CASession, EDListener
{
    // Log4J Logger
    private static final Logger log = Logger.getLogger(CASessionImpl.class.getName());

    static final long START_TIMEOUT = 60000;

    private final String m_logPrefix;
    private CADecryptParams m_decryptParams;
    private int m_nativeSessionHandle = NativeHandle.NULL_HANDLE;
    private short m_ltsid = CASession.LTSID_UNDEFINED;
    private CASessionEvent m_lastEvent;
    private SimpleCondition m_startedCondition;

    CASessionImpl(CADecryptParams decryptParams)
    {
        this.m_decryptParams = decryptParams;
        m_startedCondition = new SimpleCondition(false);
        m_logPrefix = "CAS 0x" + Integer.toHexString(this.hashCode()) + ": ";
    }

    public int getHandle()
    {
        synchronized(this)
        {
            return m_nativeSessionHandle;
        }
    }

    public String toString()
    {
        return m_logPrefix + "{nativeHandle 0x" + Integer.toHexString(m_nativeSessionHandle)
                           + ", " + m_decryptParams 
                           + ", " + m_lastEvent + '}';
    }
    
    /**
     * Only to be called by PODManager
     * @throws InterruptedException 
     */
    boolean start() throws MPEException, IllegalStateException, InterruptedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "start()");
        }

        synchronized(this)
        {
            if (m_nativeSessionHandle != NativeHandle.NULL_HANDLE)
            {
                throw new IllegalStateException("Attempted to start already-started CASession ");
            }

            ServiceExt service = null;
            int handle = 0;
            
            if(m_decryptParams.handleType == CADecryptParams.SERVICE_DETAILS_HANDLE)
            {
                service = (ServiceExt)(m_decryptParams.serviceDetails.getService());
                if (service instanceof SPIService)
                {                	
                    ProviderInstance spi = (ProviderInstance) ((SPIService) service).getProviderInstance();
                    SelectionSessionWrapper session = (SelectionSessionWrapper) spi.getSelectionSession( (SPIService)service);
                    service = session.getMappedService();
                }
                handle = service.getServiceHandle().getHandle();
            }    
            else if(m_decryptParams.handleType == CADecryptParams.TRANSPORT_STREAM_HANDLE)
            {
                TransportStreamExt tsExt = (TransportStreamExt)m_decryptParams.transportStream;
                handle = tsExt.getTransportStreamHandle().getHandle();
            }
                
            int sessionHandleArray[] = new int[1];
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "start: Calling nativeStartDecrypt for service " + service);
            }
            int mpeerr =  PodManagerImpl.nativeStartDecrypt( m_decryptParams.handleType,
                                                             handle,
                                                             m_decryptParams.tunerId,
                                                             this,
                                                             m_decryptParams.streamPids,
                                                             m_decryptParams.streamTypes,
                                                             m_decryptParams.priority,
                                                             sessionHandleArray );

            if (mpeerr != MPEException.MPE_SUCCESS)
            {
                m_nativeSessionHandle = NativeHandle.NULL_HANDLE;
                m_ltsid = CASession.LTSID_UNDEFINED;
                throw new MPEException("MPE error initiating decryption - nativeStartDecrypt returned " + mpeerr);
            }
            
            m_nativeSessionHandle = sessionHandleArray[0];

            if (m_nativeSessionHandle == NativeHandle.NULL_HANDLE)
            {
                // No session created/required
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "start: No session required - returning false");
                }
                return false;
            }
            
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "start: waiting for session 0x" + Integer.toHexString(m_nativeSessionHandle) + " to start...");
            }
            
            // Block the caller until we've received indication that we've started
            m_lastEvent = null;
            m_startedCondition.setFalse();
        } // END synchronized(this)
        
        m_startedCondition.waitUntilTrue(START_TIMEOUT);

        synchronized (this)
        {
            if (m_startedCondition.getState() == false)
            { // Timed out - Signal that we're denied for now - and hope we get an indication at some point...
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "start: timed out waiting for session 0x" + Integer.toHexString(m_nativeSessionHandle) + " to start. Signaling CA denied...");
                }
                m_startedCondition.setTrue();
                m_lastEvent = new CASessionEvent(CASessionEvent.EventID.TIMEOUT, 0, 0);
                m_ltsid = CASession.LTSID_UNDEFINED;
            }
        }
        
        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "start: session 0x" + Integer.toHexString(m_nativeSessionHandle) + " started.");
        }

        return true;
    } // END start()

    /**
     * {@inheritDoc}
     */
    public void stop() throws IllegalStateException
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "stop()");
        }
        
        int nativeSessionHandle;
        
        synchronized(this)
        {
            if ( (m_nativeSessionHandle == NativeHandle.NULL_HANDLE)
                 || (m_startedCondition == null) )
            {
                throw new IllegalStateException("Attempted to stop non-started CASession ");
            }
            
            nativeSessionHandle = m_nativeSessionHandle;
            m_nativeSessionHandle = NativeHandle.NULL_HANDLE;
        }

        try
        {
            PodManagerImpl.nativeStopDecrypt(nativeSessionHandle);
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "Caught exception calling nativeStopDecrypt", e);
            }
        }
    } // END stop()

    public void getStreamAuthorizations(CAElementaryStreamAuthorization[] authorizations) throws MPEException
    {
        if ( (m_nativeSessionHandle == NativeHandle.NULL_HANDLE) || (m_startedCondition == null) )
        {
            throw new IllegalStateException("Attempted to update stream authorizations for a non-started CASession ");
        }
        int result = PodManagerImpl.nativeGetDecryptStreamAuthorizations(m_nativeSessionHandle, authorizations);
        if (result != MPEException.MPE_SUCCESS)
        {
            throw new MPEException("GetDecryptStreamAuthorizations returned MPE error: " + result);
        }
    }
    
    /** 
     * {@inheritDoc}
     */
    public CASessionEvent getLastEvent()
    {
        synchronized (this)
        {
            return m_lastEvent;
        }
    }
    
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Received asyncEvent 0x" + Integer.toHexString(eventCode) 
                                  + " (0x" + Integer.toHexString(eventData1) 
                                  + ", 0x" + Integer.toHexString(eventData2) + ')');
        }
        
        CASessionEvent caEvent = new CASessionEvent(eventCode, eventData1, eventData2);
        
        CASessionListener listener;
        
        boolean notify;
        
        synchronized (this)
        {
            if (caEvent.getEventID() == CASessionEvent.EventID.SESSION_SHUTDOWN)
            { // We're done. No more events will be received for this session
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Queue shutdown - cleaning up...");
                }
                m_decryptParams = null;
                listener = null;
                notify = false;
                m_ltsid = CASession.LTSID_UNDEFINED;
            }
            else
            {
                listener = m_decryptParams.caListener;
                m_lastEvent = caEvent;
                if (m_ltsid == CASession.LTSID_UNDEFINED)
                {
                    m_ltsid = caEvent.getLTSID();
                }
                notify = this.m_startedCondition.getState();
                this.m_startedCondition.setTrue();
            }
        } // END synchronized (this)

        // Notify if there's a listener registered and we're not in startup
        //  (Caller only gets events after startDecrypt() has returned)
        // Note: Notifying the listener outside of the lock
        if ((listener != null) && notify)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Notifying listener of " + caEvent);
            }
            listener.notifyCASessionChange(this, caEvent);
        }
    } // END asyncEvent

    /**
     * {@inheritDoc}
     */
    public short getLTSID()
    {
        return m_ltsid;
    }
} // END class CASessionImpl
