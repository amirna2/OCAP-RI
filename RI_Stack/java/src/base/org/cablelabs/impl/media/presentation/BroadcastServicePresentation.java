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

import javax.media.Time;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.media.access.CASessionMonitor;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.player.BroadcastAuthorization;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.media.session.BroadcastSession;
import org.cablelabs.impl.media.session.MPEException;
import org.cablelabs.impl.media.session.NoSourceException;
import org.cablelabs.impl.media.session.ServiceSession;
import org.cablelabs.impl.media.session.Session;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.pod.mpe.CASessionEvent;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceChangeEvent;
import org.cablelabs.impl.service.ServiceChangeListener;
import org.cablelabs.impl.service.ServiceChangeMonitor;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.service.AlternativeContentErrorEvent;

/**
 * This is a {@link Presentation} of a live broadcast {@link Service}.
 * 
 * @author schoonma
 */
public class BroadcastServicePresentation extends AbstractServicePresentation
{
    private static final Logger log = Logger.getLogger(BroadcastServicePresentation.class);

    private final NetworkConditionMonitor networkConditionMonitor;
    private final ServiceChangeMonitor serviceChangeMonitor;
    private final CASessionMonitor caSessionMonitor;

    private boolean decoderStarved;

    public BroadcastServicePresentation(ServicePresentationContext pc, boolean isShown, Selection initialSelection,
            ScalingBounds bounds, Time startMediaTime, float startRate)

    {
        super(pc, isShown, initialSelection, bounds, startMediaTime, startRate);
        if (log.isDebugEnabled())
        {
            log.debug("constructing BroadcastServicePresentation");
        }

        NetworkConditionListener networkConditionListener = new NetworkConditionListenerImpl();
        ServiceChangeListener serviceChangeListener = new ServiceChangeListenerImpl();
        //register for all events from networkconditionmonitor
        networkConditionMonitor = new NetworkConditionMonitor(getLock(), networkConditionListener, false);
        serviceChangeMonitor = new ServiceChangeMonitor(getLock(), serviceChangeListener);
        caSessionMonitor = new CASessionMonitor(getLock(), new CASessionListenerImpl());
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

    protected CreateSessionResult doCreateSession(Selection selection, Time mt, float rate) throws NoSourceException,
            MPEException
    {
        synchronized (getLock())
        {
            if (Asserting.ASSERTING)
            {
                Assert.condition(selection != null);
            }
            //default to copy freely - will be updated when the CCI UPDATE event is received
            byte cci = CopyControlInfo.EMI_COPY_FREELY;
            return new CreateSessionResult(false, mt, rate, new BroadcastSession(getLock(), sessionListener, selection.getServiceDetails(),
                    getVideoDevice(), ((ServicePresentationContext) context).getNetworkInterface(), caSessionMonitor.getLTSID(), context.getMute(), context.getGain(), cci));
        }
    }

    protected void doStartSession(ServiceSession session, Selection selection, Time mediaTime, float rate, boolean mediaTimeChangeRequest, MediaPresentationEvaluationTrigger trigger) throws NoSourceException, MPEException
    {
        session.present(selection.getServiceDetails(), selection.getMediaAccessComponentAuthorization().getAuthorizedStreams());
    }

    protected void doUpdateSession(Selection selection) throws NoSourceException, MPEException
    {
        synchronized (getLock())
        {
            if (Asserting.ASSERTING)
            {
                Assert.condition(selection != null);
            }
            currentSession.updatePresentation(context.getClock().getMediaTime(), selection.getMediaAccessComponentAuthorization()
                    .getAuthorizedStreams());
        }
    }

    protected void doSetMediaTime(Time mt, boolean postMediaTimeSetEvent)
    {
        //don't change mediatime but trigger the event
        context.clockSetMediaTime(context.getClock().getMediaTime(), postMediaTimeSetEvent);
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

    protected float doSetRate(float rate)
    {
        // Can't change from rate 1.
        return 1;
    }

    /**
     * Authorization -is- required for broadcast service presentation
     * 
     * @return true
     */
    protected boolean mediaAccessAuthorizationRequired()
    {
        return true;
    }

    protected boolean conditionalAccessAuthorizationRequired()
    {
        return true;
    }

    protected void updateMediaAccessAuthorization(Selection selection)
    {
        if (log.isDebugEnabled())
        {
            log.debug("updateMediaAccessAuthorization: " + selection);
        }
        BroadcastAuthorization broadcastAuthorization = ((ServicePresentationContext) context).getBroadcastAuthorization();
        selection.setMediaAccessAuthorization(broadcastAuthorization.verifyMediaAccessAuthorization(selection,
                ((ServicePresentationContext) context).getNetworkInterface()));
        if (log.isInfoEnabled())
        {
            log.info("selection after media access authorization: " + selection);
        }
    }

    protected void updateConditionalAccessAuthorization(Selection selection) throws MPEException
    {
        if (log.isDebugEnabled())
        {
            log.debug("updateConditionalAccessAuthorization: " + selection);
        }
        //component validation (and retrieval) happens prior to CA - use current components
        BroadcastAuthorization broadcastAuthorization = ((ServicePresentationContext) context).getBroadcastAuthorization();
        ServiceComponentExt[] components = selection.getCurrentComponents();
        MediaPresentationEvaluationTrigger trigger = selection.getTrigger();
        boolean startNewSession = (trigger == MediaPresentationEvaluationTrigger.PMT_CHANGED) ||
                (trigger == MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE) ||
                (trigger == MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS) ||
                (trigger == SelectionTrigger.SERVICE_CONTEXT_RESELECT);
        selection.setConditionalAccessAuthorization(broadcastAuthorization.verifyConditionalAccessAuthorization(selection.getServiceDetails(), 
                ((ServicePresentationContext) context).getNetworkInterface(), selection.getElementaryStreams(((ServicePresentationContext) context).getNetworkInterface(),
                components), components, caSessionMonitor, selection.isDefault(),
                (OcapLocator) selection.getServiceDetails().getService().getLocator(), selection.getTrigger(), selection.isDigital(), startNewSession));
        if (log.isInfoEnabled())
        {
            log.info("selection after conditional access authorization: " + selection);
        }
    }

    protected boolean validateResources()
    {
        synchronized (getLock())
        {
            // don't validate 'resources' (network) unless we're presenting live
            if (networkConditionMonitor.isNetworkSyncLost())
            {
                if (log.isInfoEnabled())
                {
                    log.info("initial network sync lost");
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.TUNING_FAILURE);
                return false;
            }
            if (serviceChangeMonitor.isPMTRemoved())
            {
                if (log.isInfoEnabled())
                {
                    log.info("pmt is removed");
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                return false;
            }
            //TODO: determine validation conditions for CASessionMonitor
            if (decoderStarved)
            {
                if (log.isInfoEnabled())
                {
                    log.info("decoder starved");
                }
                switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
                return false;
            }
            return super.validateResources();
        }
    }

    protected void releaseResources(boolean shuttingDown)
    {
        synchronized(getLock())
        {
            //only shut down the CASessionMonitor and NetworkConditionMonitor if presentation is shutting down
            //NetworkConditionMonitor is needed to recover from SPI-initiated service changes
            if (shuttingDown)
            {
                caSessionMonitor.cleanup();
                networkConditionMonitor.cleanup();
            }

            serviceChangeMonitor.cleanup();
            super.releaseResources(shuttingDown);
        }
    }

    protected void initializeResources(Selection selection)
    {
        synchronized (getLock())
        {
            networkConditionMonitor.initialize(getNetworkInterface());
            serviceChangeMonitor.initialize(selection.getServiceDetails());
            super.initializeResources(selection);
        }
    }

    protected void doStopInternal(boolean shuttingDown)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doStopInternal - removing any existing CCI entry from PODManager for current service");
        }
        ((PODManager) ManagerManager.getInstance(PODManager.class)).removeCCIForService(this);
        super.doStopInternal(shuttingDown);
    }

    protected String eventToString(int event)
    {
        switch (event)
        {
            case ServiceChangeEvent.PMT_CHANGED:
                return "ServiceChangeEvent.PMT_CHANGED";
            case ServiceChangeEvent.PMT_REMOVED:
                return "ServiceChangeEvent.PMT_REMOVED";
            case NetworkConditionEvent.RETUNE_PENDING:
                return "NetworkConditionEvent.RETUNE_PENDING";
            case NetworkConditionEvent.RETUNE_FAILED:
                return "NetworkConditionEvent.RETUNE_FAILED";
            case NetworkConditionEvent.UNTUNED:
                return "NetworkConditionEvent.UNTUNED";
            case NetworkConditionEvent.TUNE_SYNC_ACQUIRED:
                return "NetworkConditionEvent.TUNE_SYNC_ACQUIRED";
            case NetworkConditionEvent.TUNE_SYNC_LOST:
                return "NetworkConditionEvent.TUNE_SYNC_LOST";
            default:
                return super.eventToString(event);
        }
    }

    protected void handleNetworkConditionEventAsync(int event)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("network condition event: " + eventToString(event) + ", mediatime: "
                        + getMediaTime());
            }

            switch (event)
            {
                case NetworkConditionEvent.TUNE_SYNC_LOST:
                    handleTuneLockLostNotification();
                    break;
                case NetworkConditionEvent.TUNE_SYNC_ACQUIRED:
                    handleTuneLockAcquiredNotification();
                    break;
                case NetworkConditionEvent.RETUNE_PENDING:
                    handleRetunePendingNotification();
                    break;
                case NetworkConditionEvent.RETUNE_FAILED:
                    handleRetuneFailed();
                    return;
                case NetworkConditionEvent.UNTUNED:
                    handleUntuned();
                    return;
                default:
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unknown network condition event type - ignoring: " + event);
                    }
            }
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

    /**
     * Tuner is moving.  Release resources.  Reselection will be triggered by a retune complete (signaling tune sync, same handling)
     */
    private void handleRetunePendingNotification()
    {
        synchronized(getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("retune pending - stopping session");
            }
            try
            {
                doStop(false);
            }
            finally
            {
                releaseResources(false);
            }
        }
    }

    private void handleTuneLockLostNotification()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("tune lock lost - reselecting");
            }
            //reselect - validation will result in a transition to altconent/tuning failure based on order of validation
            reselect(SelectionTrigger.RESOURCES);
        }
    }

    private void handleTuneLockAcquiredNotification()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("tune lock acquired - triggering reselection");
            }
            reselect(SelectionTrigger.RESOURCES);
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
            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_STOP_DECODE, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
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

    private void handleRetuneFailed()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("retune failed - closing presentation");
            }
            closePresentation("retune failed", null);
        }
    }

    private void handleUntuned()
    {
        //TODO: update to be recoverable altcontent (untuned represents service_unmapped) - OCORI-4537
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("untuned - closing presentation");
            }
            closePresentation("untuned", null);
        }
    }

    private void handleDecoderStarvedNotification()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("decoder starved - not presenting alt content and components valid - switching to alt content");
            }
            switchToAlternativeContent(ALTERNATIVE_CONTENT_MODE_RENDER_BLACK, AlternativeContentErrorEvent.class, AlternativeContentErrorEvent.CONTENT_NOT_FOUND);
            decoderStarved = true;
        }
    }

    private void handleDecoderNoLongerStarvedNotification()
    {
        synchronized (getLock())
        {
            decoderStarved = false;
            if (log.isInfoEnabled())
            {
                log.info("decoder no longer starved - presenting alt content, components and state valid");
            }
            reselect(SelectionTrigger.RESOURCES);
        }
    }

    private void handleCCIUpdate(byte cci)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info("handleCCI update: " + cci + " - updating CCI on the session and on PODManager for the current service");
            }
            ((BroadcastSession)currentSession).setCCI(cci);
            ((PODManager) ManagerManager.getInstance(PODManager.class)).setCCIForService(this, getCurrentSelection().getServiceDetails(), cci);
        }
    }

    protected void handleSessionEventAsync(Session session, int event, int data1, int data2)
    {
        synchronized (getLock())
        {
            switch (event)
            {
                case MediaAPI.Event.DECODER_STARVED:
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    handleDecoderStarvedNotification();
                    break;
                case MediaAPI.Event.DECODER_NO_LONGER_STARVED:
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    handleDecoderNoLongerStarvedNotification();
                    break;
                case MediaAPI.Event.CCI_UPDATE:
                    if (session != currentSession || !isSessionStarted())
                    {
                        return;
                    }
                    handleCCIUpdate((byte)data1);
                    break;
                default:
                    // Not handled by this class, so pass up to the parent to
                    // handle.
                    super.handleSessionEventAsync(session, event, data1, data2);
            }
        }
    }

    ExtendedNetworkInterface getNetworkInterface()
    {
        return ((ServicePresentationContext) context).getNetworkInterface();
    }

    public void setMute(boolean mute)
    {
        currentSession.setMute(mute);
    }

    public float setGain(float gain)
    {
        return currentSession.setGain(gain);
    }

    class NetworkConditionListenerImpl implements NetworkConditionListener
    {
        public void networkConditionEvent(final int event)
        {
            synchronized (getLock())
            {
                context.getTaskQueue().post(new Runnable()
                {
                    public void run()
                    {
                        synchronized (getLock())
                        {
                            handleNetworkConditionEventAsync(event);
                        }
                    }
                });
            }
        }
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

    private class CASessionListenerImpl implements CASessionListener
    {
        public void notifyCASessionChange(CASession session, CASessionEvent event)
        {
            if (conditionalAccessAuthorizationRequired())
            {
                if (log.isInfoEnabled())
                {
                    log.info("CASessionListener received CASessionChange event - notification session: " + session + ", notification event id: 0x" + Integer.toHexString(event.getEventID()));
                }
                reselect(SelectionTrigger.CONDITIONAL_ACCESS);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ignoring CASessionChange notification when conditional access not required - session: " + session + ", notification event id: 0x" + Integer.toHexString(event.getEventID()));
                }
            }
        }
    }
}
