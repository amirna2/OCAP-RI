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
package org.cablelabs.impl.media.presentation;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.media.Time;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.RemoteServiceSession;
import org.cablelabs.impl.media.session.ServiceSession;
import org.cablelabs.impl.media.session.Session;
import org.cablelabs.impl.media.source.RemoteServiceDataSource;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.HNClientSession;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.content.ContentResourceImpl;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionCompleteListener;
import org.cablelabs.impl.ocap.si.DescriptorImpl;
import org.cablelabs.impl.ocap.si.ProgramMapTableManagerImpl;
import org.cablelabs.impl.service.RemoteServiceImpl;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceChangeEvent;
import org.cablelabs.impl.service.ServiceChangeListener;
import org.cablelabs.impl.service.ServiceChangeMonitor;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.signalling.DescriptorTag;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.SimpleCondition;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.security.NetSecurityManager;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.si.Descriptor;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.ProgramMapTableManager;

/**
 * This is a {@link Presentation} of a remote {@link Service}.
 * 
 * @author schoonma
 */
public class RemoteServicePresentation extends AbstractServicePresentation
{
    private static final Logger log = Logger.getLogger(RemoteServicePresentation.class);

    private int localConnectionId;

    private HNStreamProtocolInfo protocolInfo;

    private final HNClientSession clientSession;

    private final ServiceChangeMonitor serviceChangeMonitor;

    private ConnectionCompleteListener connectionCompleteListener;

    //after components have been retrieved, if presentFromRequestedMediaTime is true, restart playback with the requested mediatime
    //otherwise, just update pids - default to true in case a contentItem is not available (standalone playback on non-dlna server)
    private boolean presentFromRequestedMediaTime = true;

    //current DTCP, should never be null
    private Set currentDTCP = new HashSet();
    private boolean componentsRetrieved;

    /**
     * Constructor
     *
     * @param presentationContext
     *            presentation context
     * @param isShown
     *            is shown
     * @param initialSelection
     *            initial selection
     * @param bounds
     * @param startMediaTime
     * @param startRate
     * @param protocolInfo if available from http headers and no contentItem available (standalone JMF playback) - null otherwise
     */
    public RemoteServicePresentation(ServicePresentationContext presentationContext, boolean isShown, Selection initialSelection, 
            ScalingBounds bounds, Time startMediaTime, float startRate, HNStreamProtocolInfo protocolInfo)

    {
        super(presentationContext, isShown, initialSelection, bounds, startMediaTime, startRate);
        this.protocolInfo = protocolInfo;
        clientSession = new HNClientSession(presentationContext);
        ServiceChangeListener serviceChangeListener = new ServiceChangeListenerImpl();
        serviceChangeMonitor = new ServiceChangeMonitor(getLock(), serviceChangeListener);
        if (log.isDebugEnabled())
        {
            log.debug("constructing RemoteServicePresentation");
        }
    }

    protected void updateSelectionDetails(Selection selection, Time mediaTime, float rate)
    {
        SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();
        ServiceDetailsExt newDetails = Util.getServiceDetails(((ServiceDataSource) context.getSource()).getService());
        if (log.isDebugEnabled())
        {
            log.debug("updateSelectionDetails - new SI manager: " + siManager + ", new details: "
                    + newDetails);
        }
        selection.update(siManager, newDetails);
    }
    
    protected void doStart()
    {
        //call openSession only one time for duration of presentation
        RemoteServiceImpl service = (RemoteServiceImpl)getCurrentSelection().getServiceDetails().getService();
        RemoteServiceDataSource dataSource = ((RemoteServiceDataSource) context.getSource());
        ContentResource resource = dataSource.getContentResource();
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("doStart - opening client session");
            }
           
            // Verify resources have been initialized and protocol info selected
            if (resource != null)
            {
                protocolInfo = ((ContentResourceImpl)resource).getProtocolInfo();
                //if s0 increasing (live presentation, no need to present from requested mediatime)
                presentFromRequestedMediaTime = !((ContentResourceImpl)resource).getProtocolInfo().isS0Increasing();
                if (log.isDebugEnabled()) 
                {
                    log.debug("using resource-provided protocol info: " + protocolInfo);
                }
            }
            else if (protocolInfo != null)
            {
                presentFromRequestedMediaTime = !protocolInfo.isS0Increasing();
                if (log.isDebugEnabled()) 
                {
                    log.debug("doStart() - using protocol info s0 increasing: " + protocolInfo.isS0Increasing());
                }                    
            }
            else
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("doStart() - null content item and null protocol info");
                }                                    
            }

            //use connection ID zero as initial ID (will be retrieved via connection ID header)
            //protocolInfo may be null if one wasn't provided           
            URI uri = null;
            if(resource != null)
            {
                // Use uri retrieved from ContentResource
                uri = new URI(resource.getLocator().toExternalForm());  
            }
            else if(service.getResourceUrl() != null)
            {
                uri = new URI(service.getResourceUrl());
            }
            
            if (log.isInfoEnabled()) 
            {
                log.info("RemoteServicePresentation doStart() - uri: " + uri);
            }  
            
            if(uri != null)
            {
                clientSession.openSession(uri, protocolInfo, 0,
                                          service.getRemoteConnectionManagerService());
            }
            // TODO: If uri is null?
            
            //requestTransmissionAndDecode must be called prior to actual decode start
            // in order to initiate SI retrieval (needed to retrieve DTCP descriptor)
            // *TODO* - how is initial block state determined? 
            boolean initialBlockingState = false;
            //use -1 if setMediaTime has not yet been called on the player (supporting 'default' mediatime - live point for TSBs, zero for recordings)
            //passing null in for CCI descriptors initially, will be updated when CCI is retrieved
            clientSession.requestTransmissionAndDecode(getVideoDevice().getHandle(), context.isMediaTimeSet() ? startMediaTime.getNanoseconds() : -1,
                    initialBlockingState, context.getMute(), context.getGain(), startRate, null);
            
            ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            ServiceDetailsHandle handle = sm.getSIDatabase().registerForHNPSIAcquisition(clientSession.getNativeHandle());
            if (log.isInfoEnabled())
            {
                log.info("doStart - registered for HN PSI acquisition - service details handle: " + handle);
            }

            service.setServiceHandle(handle);
            localConnectionId = MediaServer.getInstance().getCMS().getNextConnectionID();
            if (log.isInfoEnabled())
            {
                log.info("session started - source connection ID: " + clientSession.getSourceConnectionId() + ", service handle: " + handle + ", local connection id: " + localConnectionId);
            }
            super.doStart();
        }
        catch (HNStreamingException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to open session or start transmission", e);
            }
            throw new IllegalStateException(e.toString());
        }
        catch (SIDatabaseException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Failure registering for HN PSI acquisition", e);
            }
            throw new IllegalStateException(e.toString());
        }
        catch (URISyntaxException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to construct URI for service: " + service, e);
            }
            throw new IllegalStateException(e.toString());
        }
    }

    protected CreateSessionResult doCreateSession(Selection selection, Time mt, float rate) throws NoSourceException,
            MPEException
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("doCreateSession - selection: " + selection + ", time: " + mt + ", rate: " + rate);
            }
            // register connection complete listener to clean up session
            connectionCompleteListener = new ConnectionCompleteListenerImpl(localConnectionId);
            MediaServer.getInstance().getCMS().addConnectionCompleteListener(connectionCompleteListener);

            return new CreateSessionResult(false, mt, rate, new RemoteServiceSession(getLock(), sessionListener, selection.getServiceDetails(),
                    context.getMute(), context.getGain(), getVideoDevice(),
                    clientSession));
        }
    }

    protected void doStartSession(final ServiceSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest, MediaPresentationEvaluationTrigger trigger) throws MPEException, NoSourceException
    {
        if (log.isDebugEnabled())
        {
            log.debug("doStartSession - selection: " + selection + ", time: " + mediaTime + ", rate: " + rate + ", trigger: " + trigger);
        }
        //component retrieval may fail...catch separately
        try
        {
            clientSession.setComponents(presentFromRequestedMediaTime, selection.getServiceDetails().getComponents(), (HNPlaybackCopyControlInfo[]) Arrays.copy(currentDTCP.toArray(), HNPlaybackCopyControlInfo.class));
            componentsRetrieved = true;
        }
        catch (HNStreamingException e)
        {
            componentsRetrieved = false;
            if (log.isWarnEnabled())
            {
                log.warn("Unable to retrieve components for current selection", e);
            }
            throw new NoSourceException("Unable to retrieve components");
        }
        catch (InterruptedException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("interrupted retrieving components for current selection", e);
            }
            throw new NoSourceException("Unable to retrieve components");
        }
        catch (SIRequestException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("interrupted retrieving components for current selection", e);
            }
            throw new NoSourceException("Unable to retrieve components");
        }
        //enqueue the state and notification update that was received via native event prior to creation of the RemoteServiceSession
        context.getTaskQueue().post(new Runnable()
        {
            public void run()
            {
                ((RemoteServiceSession) session).notifyPresentationStateAsync();
            }
        });
    }

    protected void doUpdateSession(Selection selection) throws NoSourceException, MPEException
    {
        if (log.isInfoEnabled())
        {
            log.info("doUpdateSession calling setComponents..");
        }
        try 
        {
            clientSession.setComponents(true, selection.getCurrentComponents(), (HNPlaybackCopyControlInfo[]) Arrays.copy(retrieveDTCP().toArray(), HNPlaybackCopyControlInfo.class));
        } 
        catch (HNStreamingException e) 
        {
            throw new MPEException("doUpdateSession failed", e);
        }
        //enqueue the state and notification update that was received via native event prior to creation of the RemoteServiceSession
        context.getTaskQueue().post(new Runnable()
        {
            public void run()
            {
                ((RemoteServiceSession)currentSession).notifyPresentationStateAsync();
            }
        });
    }

    protected float doSetRate(float rate)
    {
        synchronized (getLock())
        {
            // a session change may be in progress
            waitForSessionChangeToComplete();
            if (currentSession != null)
            {
                try
                {
                    return currentSession.setRate(rate);
                }
                catch (MPEException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("unable to set rate: " + rate, e);
                    }
                    context.notifyStopByError("Unable to set rate to: " + rate, e);
                }
            }
            // failed, return current rate from the context's clock
            return context.getClock().getRate();
        }
    }

    protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
    {
        synchronized (getLock())
        {
            // a session change may be in progress
            waitForSessionChangeToComplete();
            if (currentSession != null)
            {
                try
                {
                    currentSession.setMediaTime(mt);
                    context.clockSetMediaTime(mt, postMediaTimeSetEvent);
                }
                catch (MPEException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("unable to set media time: " + mt, e);
                    }
                }
            }
        }
    }

    protected Time doGetMediaTime()
    {
        synchronized (getLock())
        {
            if (!isSessionStarted())
            {
                return null;
            }
            try
            {
                return currentSession.getMediaTime();
            }
            catch (MPEException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("unable to get mediatime from current session: " + currentSession, e);
                }
                return null;
            }
        }
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        synchronized (getLock())
        {
            if (connectionCompleteListener != null)
            {
                MediaServer.getInstance().getCMS().removeConnectionCompleteListener(connectionCompleteListener);
            }
            super.doStopInternal(shuttingDown);
        }
    }

    protected boolean isCCIUpdated()
    {
        Set newDTCP = retrieveDTCP();
        //currentCCI never null
        return (!currentDTCP.equals(newDTCP));
    }

    private void handleConnectionComplete()
    {
        if (log.isDebugEnabled())
        {
            log.debug("RemoteServicePresentation.handleConnectionComplete() - called");
        }
        synchronized (getLock())
        {
            // we've received a connection complete notification, close the
            // presentation
            closePresentation("connection complete", null);
        }
    }

    private void handleEndOfContent()
    {
        synchronized (getLock())
        {
            //set the jmf clock's rate - no need to update session, already at rate 0.0
            float rate = 0.0F;
            context.clockSetRate(rate, false);
            ((PlaybackPresentationContext) context).notifyEndOfContent(rate);
        }
    }

    private void handleStartOfContent()
    {
        synchronized (getLock())
        {
            //set the jmf clock's rate - no need to update session, already at rate 0.0
            float rate = 0.0F;
            context.clockSetRate(rate, false);
            ((PlaybackPresentationContext) context).notifyBeginningOfContent(rate);
        }
    }

    protected void handleSessionEventAsync(Session session, int event, int data1, int data2)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug("RemoteServicePresentation.handleSessionEventAsync() - called - event: " + getEventStr(event));
            }
            switch (event)
            {
                case HNAPI.Event.HN_EVT_END_OF_CONTENT:
                    handleEndOfContent();
                    break;
                case HNAPI.Event.HN_EVT_BEGINNING_OF_CONTENT:
                    handleStartOfContent();
                    break;
                case HNAPI.Event.HN_EVT_SESSION_CLOSED:
                    // ignore but don't pass up
                    break;
                case HNAPI.Event.HN_EVT_SESSION_OPENED:
                    // ignore but don't pass up - should not be received (session should already be open)
                    break;
                case HNAPI.Event.HN_EVT_PLAYBACK_STOPPED:
                    // ignore but don't pass up
                    break;
                case HNAPI.Event.HN_EVT_INACTIVITY_TIMEOUT:
                    handleFailureEventAsync(event, data1, data2);
                    break;
                default:
                    // Not handled by this class, so pass up to the parent to
                    // handle.
                    super.handleSessionEventAsync(session, event, data1, data2);
                    break;
            }
        }
    }

    private void handlePMTRemovedNotification()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("pmt removed - triggering switchToAlternativeContent");
            }
            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
        }
    }

    private void handlePMTChangedNotification()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("pmt changed - triggering reselection");
            }
            reselect(MediaPresentationEvaluationTrigger.PMT_CHANGED);
        }
    }

    protected boolean validateResources()
    {
        synchronized (getLock())
        {
            if (clientSession == null || !clientSession.isPresenting())
            {
                if (log.isInfoEnabled())
                {
                    log.info("clientSession is null or not presenting - clientsession: " + clientSession);
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                return false;
            }
            if (!componentsRetrieved)
            {
                if (log.isInfoEnabled())
                {
                    log.info("components not retrieved");
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                return false;
            }
            if (serviceChangeMonitor.isPMTRemoved())
            {
                if (log.isInfoEnabled())
                {
                    log.info("pmt is removed");
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                return false;
            }

            return super.validateResources();
        }
    }

    protected void initializeResources(Selection selection)
    {
        if (log.isDebugEnabled())
        {
            log.debug("initializeResources() - called");
        }
        RemoteServiceImpl service = (RemoteServiceImpl)selection.getServiceDetails().getService();
        RemoteServiceDataSource dataSource = ((RemoteServiceDataSource) context.getSource());
        ContentResource resource = dataSource.getContentResource();
        try
        {
            //if this wasn't a remoteservice provided via CDS, don't attempt to check protocolInfo or register with connectionmanager
            if (resource != null)
            {
                // TODO (Prasanna): When do we throw HNStreamingException??
                //if (resource == null)
                {
                    // *TODO* - create string of platform profiles to include in err msg
                    // *TODO* - create string of content item profiles to include in err msg
                    //throw new HNStreamingException("Platform does not support any of available profile IDs");
                }
                HNStreamProtocolInfo protocolInfo = ((ContentResourceImpl)resource).getProtocolInfo();

                MediaServer.getInstance().getCMS().validateProtocol(protocolInfo);
                MediaServer.getInstance().getCMS().registerLocalConnection(protocolInfo, localConnectionId, -1);

                if (log.isDebugEnabled())
                {
                    log.debug("initializeResources() - using protocol info: " + protocolInfo.getAsString());
                }
            }
            else if (protocolInfo != null)
            {
                presentFromRequestedMediaTime = !protocolInfo.isS0Increasing();
                if (log.isDebugEnabled())
                {
                    log.debug("initializeResources() - content item was null, used protocol info, " +
                            "set flag: " + presentFromRequestedMediaTime);
                    log.debug("initializeResources() - protocol info: " + protocolInfo.getAsString());
                    log.debug("initializeResources() - s0 Increasing: " + protocolInfo.isS0Increasing());
                }                   
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("initializeResources() - content item and protocol info were null");
                }                                
            }

            NetSecurityManagerImpl netSecurityManager = (NetSecurityManagerImpl) NetSecurityManager.getInstance();
            URL url = null;
            if(resource != null)
            {
                // Use url retrieved from ContentResource
                url = new URL(resource.getLocator().toExternalForm().trim());
            }
            else if(service.getResourceUrl() != null)
            {
                url = new URL(service.getResourceUrl());
            }
            
            if (log.isInfoEnabled()) 
            {
                log.info("RemoteServicePresentation initializeResources() - url: " + url);
            }  
            
            if(url != null)
            {
                // TODO: need macaddress
                netSecurityManager.addClientSession(clientSession, InetAddress.getByName(url.getHost()), null, url, localConnectionId);
            }


            //retrieve initial DTCP
            //restart if presentation should be restarted from the requested mediatime (recordings or unknown)
            currentDTCP = retrieveDTCP();
            componentsRetrieved = true;

            ServiceDetailsExt serviceDetails = selection.getServiceDetails();
            //register service change monitor after setting the service handle
            serviceChangeMonitor.initialize(serviceDetails);
        }
        catch (HNStreamingException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to establish a connection", e);
            }
        }
        catch (MalformedURLException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to build URL from: " + service.getLocator().toExternalForm(), e);
            }
        }
        catch (UnknownHostException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unknown host: " + service.getLocator().toExternalForm(), e);
            }
        }
        super.initializeResources(selection);
    }

    /**
     * Retrieve DTCP for PMT, or null if DTCP could not be retrieved.
     *
     * @throws SIRequestException
     * @throws InterruptedException
     */
    private Set retrieveDTCP()
    {
        ProgramMapTableManagerImpl pmtManager = (ProgramMapTableManagerImpl) ProgramMapTableManager.getInstance();

        class PMTRequestor implements SIRequestor
        {
            public ProgramMapTable pmt = null;
            public boolean timedOut = true;
            private final SimpleCondition pmtAcquired = new SimpleCondition(false);

            public void notifyFailure(SIRequestFailureType reason)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("retrieveDTCP - notifyFailure: " + reason);
                }
                timedOut = false;
                pmtAcquired.setTrue();
            }

            public void notifySuccess(SIRetrievable[] result)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("retrieveDTCP - notifySuccess");
                }
                if (result != null && result.length > 0)
                {
                    pmt = (ProgramMapTable) result[0];
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("retrieveDTCP - notifySuccess but empty or null result");
                    }
                }
                timedOut = false;
                pmtAcquired.setTrue();
            }

            public ProgramMapTable retrievePMT()
            {
                try
                {
                    pmtAcquired.waitUntilTrue(20000);
                }
                catch (InterruptedException e)
                {
                    //ignore
                }

                return pmt;
            }
        }

        PMTRequestor pmtRequestor = new PMTRequestor();

        SIRequest siRequest = pmtManager.retrievePMT(pmtRequestor, getCurrentSelection().getServiceDetails().getService());

        ProgramMapTable result = pmtRequestor.retrievePMT();
        if (result != null)
        {
            return getDTCPEntriesFromPMT(result);
        }
        if (pmtRequestor.timedOut)
        {
            if (log.isInfoEnabled())
            {
                log.info("retrieveDTCP - PMT retrieval timed out");
            }
            siRequest.cancel();
        }
        return new HashSet();
    }
    
    private Set getDTCPEntriesFromPMT(ProgramMapTable programMapTable)
    {
        Set dtcpEntries = new HashSet();
        Descriptor[] outerDescriptors = programMapTable.getOuterDescriptorLoop();
        if (outerDescriptors != null)
        {
            for (int i = 0; i < outerDescriptors.length; ++i)
            {
                if (DescriptorTag.DTCP == outerDescriptors[i].getTag() &&
                        outerDescriptors[i] instanceof DescriptorImpl)
                {
                    //outer-loop (program-level), always use -1 for pmt pid
                    DTCP thisDTCP = new DTCP(true, (short)-1, outerDescriptors[i]);
                    if (thisDTCP.isAudioDescriptor())
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("ignoring program-level DTCP audio descriptor: " + thisDTCP);
                        }
                    }
                    else
                    {
                        dtcpEntries.add(thisDTCP.toPlaybackCopyControlInfo());
                    }
                }
            }
        }
        //examine elementary streams
        PMTElementaryStreamInfo[] elementaryStreamInfoEntries = programMapTable.getPMTElementaryStreamInfoLoop();

        for (int i=0;i<elementaryStreamInfoEntries.length;i++)
        {
            //walk inner descriptor loop
            Descriptor[] innerDescriptors = elementaryStreamInfoEntries[i].getDescriptorLoop();
            if (innerDescriptors != null)
            {
                for (int j = 0; j < innerDescriptors.length; ++j)
                {
                    if (DescriptorTag.DTCP == innerDescriptors[j].getTag() &&
                            innerDescriptors[j] instanceof DescriptorImpl)
                    {
                        //inner-loop (component-level)
                        DTCP thisDTCP = new DTCP(false, elementaryStreamInfoEntries[i].getElementaryPID(), innerDescriptors[j]);
                        if (!thisDTCP.isAudioDescriptor())
                        {
                            dtcpEntries.add(thisDTCP.toPlaybackCopyControlInfo());
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("ignoring elementary stream-level DTCP audio descriptor: " + thisDTCP + ", elementary stream info: " + elementaryStreamInfoEntries[i]);
                            }
                        }
                    }
                }
            }
        }
        
        if (dtcpEntries.size() > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("found DTCP descriptors: " + dtcpEntries);
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("no elementary stream-level or program-level DTCP descriptors found");
            }
        }
        return dtcpEntries;
    }

    protected void releaseResources(boolean shuttingDown)
    {
        if (log.isDebugEnabled())
        {
            log.debug("releaseResources - ,local connection id: " + localConnectionId + ", clientSession: " + clientSession);
        }
        serviceChangeMonitor.cleanup();
        RemoteServiceImpl service = (RemoteServiceImpl) getCurrentSelection().getServiceDetails().getService();
        service.setServiceHandle(null);
        NetSecurityManagerImpl netSecurityManager = (NetSecurityManagerImpl) NetSecurityManager.getInstance();
        netSecurityManager.removeClientSession(clientSession);
        if (shuttingDown)
        {
            //only tear down the connection if shutting down
            if (localConnectionId != 0)
            {
                MediaServer.getInstance().getCMS().removeConnectionInfo(localConnectionId);

                if (log.isInfoEnabled())
                {
                    log.info("tearDownConnection - connection torn down for localConnectionId: " + localConnectionId);
                }
            }
            localConnectionId = 0;
            clientSession.closeSession();
        }
        super.releaseResources(shuttingDown);
    }

    ExtendedNetworkInterface getNetworkInterface()
    {
        return null;
    }

    private class ConnectionCompleteListenerImpl implements ConnectionCompleteListener
    {
        private final int connectionId;

        private ConnectionCompleteListenerImpl(int connectionId)
        {
            this.connectionId = connectionId;
        }

        public void notifyComplete(int connectionId)
        {
            if (log.isDebugEnabled())
            {
                log.debug("RemoteServicePresentation-ConnectionCompleteListenerImpl.notifyComplete() - "
                + "called with id = " + connectionId);
            }
            if (this.connectionId == connectionId)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("RemoteServicePresentation-ConnectionCompleteListenerImpl.notifyComplete() - "
                    + "connection id matches local connection ID, calling handleConnectionComplete()");
                }
                handleConnectionComplete();
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("RemoteServicePresentation-ConnectionCompleteListenerImpl.notifyComplete() - "
                    + "connection ID does not match local connection ID, ignoring");
                }
            }
        }

        public String toString()
        {
            return "HnServerSessionManager ConnectionCompleteListenerImpl - id: " + connectionId;
        }
    }

    protected String eventToString(int event)
    {
        switch (event)
        {
            case ServiceChangeEvent.PMT_CHANGED:
                return "ServiceChangeEvent.PMT_CHANGED";
            case ServiceChangeEvent.PMT_REMOVED:
                return "ServiceChangeEvent.PMT_REMOVED";
            default:
                return super.eventToString(event);
        }
    }

    protected void handleServiceChangeEventAsync(int event)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("service change event: " + eventToString(event) + ", mediatime: "
                + getMediaTime());
            }

            // note: PAT removal will trigger PMT removal and generate alt
            // content with the same reason code,
            // not adding a separate check for that
            switch (event)
            {
                case ServiceChangeEvent.PMT_REMOVED:
                    handlePMTRemovedNotification();
                    return;
                case ServiceChangeEvent.PMT_CHANGED:
                    handlePMTChangedNotification();
                    return;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unknown service change event type - ignoring: " + event);
                    }
            }
        }
    }

    private String getEventStr(int event)
    {
        switch (event)
        {
            case HNAPI.Event.HN_EVT_BEGINNING_OF_CONTENT:
                return "Beginning Of File Event";
            case HNAPI.Event.HN_EVT_END_OF_CONTENT:
                return "End Of File Event";
            case HNAPI.Event.HN_EVT_SESSION_CLOSED:
                return "Session Closed Event";
            case HNAPI.Event.HN_EVT_SESSION_OPENED:
                return "Session Opened Event";
            case HNAPI.Event.HN_EVT_PLAYBACK_STOPPED:
                return "Playback Stopped Event";
            case HNAPI.Event.HN_EVT_INACTIVITY_TIMEOUT:
                return "Inactivity timeout";
            default:
                return "Unknown (" + event + ")";
        }
    }

    public void setMute(boolean mute)
    {
        clientSession.setMute(mute);
    }

    public float setGain(float gain)
    {
        return clientSession.setGain(gain);
    }

    class ServiceChangeListenerImpl implements ServiceChangeListener
    {
        public void serviceChangeEvent(final int event)
        {
            synchronized (getLock())
            {
                context.getTaskQueue().post(new Runnable()
                {
                    public void run()
                    {
                        synchronized (getLock())
                        {
                            handleServiceChangeEventAsync(event);
                        }
                    }
                });
            }
        }
    }

    class DTCP
    {
        /*
        //DESCRIPTORS THEMSELVES HAVE TWO BYTES UP FRONT 
        DTCP_descriptor() {
                                  size(bits)        format      value
            descriptor_tag          8               uimsbf      0x88
            descriptor_length       8               uimsbf
            CA_System_ID            16              uimsbf      0x0fff
            for (i=0;i<descriptor_length+2;i++) {
                private_data_byte   8               bslbf
            }
        }
        
        private_data_byte {
                                  size(bits)        format
            reserved                1               bslbf
            retention_move_mode     1               bslbf            
            retention_state         3               bslbf            
            epn                     1               bslbf            
            dtcp_cci                2               bslbf            
            reserved                3               bslbf            
            dot                     1               bslbf            
            ast                     1               bslbf            
            image_constraint_token  1               bslbf            
            aps                     1               bslbf
        }
        
        cci 
        00: copy-free
        01: no-more-copies
        10: copy-one-generation
        11: copy-never

        dtcp_audio_descriptor same as dtcp_descriptor except private_data_byte field 1st bit value of private_data_byte is used to distinguish dtcp_descriptor and dtcp_audio_descriptor

        private_data_byte {
                                  size(bits)        format
            descriptor_id           1               bslbf
            reserved                5               bslbf
            dtcp_cci_audio          2               bslbf            
            audio_type              3               bslbf            
            reserved                5               bslbf
        }
        
        */

        private final boolean isProgram;
        private final short pid;
        final boolean audioDescriptor;
        final byte cci;

        /**
         * Parse CCI from descriptor if not an audio DTCP descriptor - CCI will be zero if this is an audio DTCP descriptor
         * @param isProgram
         * @param pid
         * @param descriptor
         */
        DTCP(boolean isProgram, short pid, Descriptor descriptor)
        {
            this.isProgram = isProgram;
            this.pid = pid;
            //walk structure to find audio descriptor/cci

            //audio descriptor if first bit value of first byte of private data is 1 
            audioDescriptor = ((descriptor.getContent()[0] >>> 7) & 0x01) == 1;
            //if not an audio descriptor, calculate cci byte
            //bottom two bits of first byte are cci
            cci = (audioDescriptor ? 0 : (byte)(descriptor.getContent()[0] & 0x3));
        }

        public boolean isAudioDescriptor()
        {
            return audioDescriptor;
        }

        public String toString()
        {
            return (audioDescriptor ? "DTCP descriptor - audio" : "DTCP descriptor - cci: " + cci); 
        }
        
        public HNPlaybackCopyControlInfo toPlaybackCopyControlInfo()
        {
            //never audio
            return new HNPlaybackCopyControlInfo(pid, isProgram, false, cci);
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DTCP dtcp = (DTCP) o;

            if (audioDescriptor != dtcp.audioDescriptor) return false;
            if (cci != dtcp.cci) return false;
            if (isProgram != dtcp.isProgram) return false;
            return pid == dtcp.pid;
        }

        public int hashCode()
        {
            int result = (audioDescriptor ? 1 : 0);
            result = 31 * result + (int) cci;
            result = 31 * result + (isProgram ? 1 : 0);
            result = 31 * result + (int) pid;
            return result;
        }
    }
}
