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

package org.cablelabs.lib.utils.oad;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.RateChangeEvent;
import javax.media.StopEvent;
import javax.media.Time;

import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;
import javax.tv.service.Service;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.LiveServiceManager;
import org.cablelabs.lib.utils.oad.InteractiveResourceContentionHandler;
import org.cablelabs.lib.utils.oad.TelnetRICmd;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.davic.media.MediaPresentedEvent;
import org.davic.mpeg.ElementaryStream;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.dvb.media.VideoTransformation;
import org.dvb.service.selection.DvbServiceContext;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceManager;
import org.havi.ui.HScreenPoint;
import org.ocap.hardware.CopyControl;
import org.ocap.media.AlternativeMediaPresentationEvent;
import org.ocap.media.AlternativeMediaPresentationReason;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaAccessConditionControl;
import org.ocap.media.MediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.service.AlternativeContentErrorEvent;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;

/**
 * Purpose: This class contains methods defined in OcapAppDriverInterfaceCore
 * which includes basic functionality for Ocap Xlets without any specific
 * OCAP extension to be supported.
*/
public class OcapAppDriverCore implements OcapAppDriverInterfaceCore,
                                          ControllerListener,
                                          ServiceContextListener
{
    /**
     * The Singleton instance of this class, as type OcapAppDriverInterfaceCore
     * in order to restrict method calls to the Interface methods only
     */
    private static OcapAppDriverCore s_instance;
    
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(OcapAppDriverCore.class);

    // Utility to support date string conversion
    public static final SimpleDateFormat SHORT_DATE_FORMAT =
        new SimpleDateFormat("yyyy/MM/dd HH:mm:ssZ");

    // Configuration parameters
    private int m_numTuners = 2;
    
    private LiveServiceManager m_liveServices;
    //private volatile boolean m_scEventFired = false;
    private final Object m_eventMonitor;

    // Application organization name used in playback authentication
    private String m_organization = null;
    
    private final Object m_tuneStateMonitor;

    // list of permissible playback rates
        
    // Represents whether a Service was successfully selected
    private int m_tuneState;
    
    private boolean m_bufferingEnabled[];
    
    // Register a ResourceContentionHandler if true
    boolean m_registerRCH = true;
    
    // A boolean indicating media access authorization- default is true
    private boolean m_isFullAuth = true;
    
    /**
     * An integer representing what type of content the playback is presenting.
     * Possible values are PLAYBACK_CONTENT_TYPE_....
     */
    private int m_playbackContentType;

    /**
     * An integer representing what kind of playback is presenting. Values are
     * currently either JMF_PLAYBACK when JMF player is used, or SERVICE_PLAYBACK
     * indicating that a service selection is made to perform playback.
     */
    private int m_playbackType;

    /** 
     * JMFPlayer to be used for JMF playback
     */
    private Player m_jmfPlayer;

    /**
     * Service context to be used for Service Selection playback
     */
    private ServiceContext m_serviceContext;

    /**
     * Current state of playback will is maintain by this class
     * using ControllerListener and ServiceContextListener callbacks 
     */
    private int m_playbackState = PLAYBACK_STATE_UNKNOWN;

    /**
     * The URL for the video to be used for JMF playback
     */
    private String m_videoURL;
    
    /**
     * Resource contention handling resource usage sorter.
     * Exposed as a member so other extension specific usages
     * can be added.
     */
    private InteractiveResourceUsageSorter m_ruSorter;
        
    /*
     * Hash map used to store PlaybackEvents which contain an event 
     * received related to playback.
     * It includes both controller and service context events.
     * The key used for storing is the event id which is an integer
     * which is incremented as each event is received.
     */
    private final HashMap m_events = new HashMap();
    private int m_currentEventIndex = 0;
    
    private InteractiveResourceContentionHandler m_resContentionHandler;
        
    private OcapAppDriverCore()
    {

        if (log.isInfoEnabled())
        {
            log.info("OcapAppDriver()");
        }

        // create the service context for this tuner
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try
        {
            m_serviceContext = scf.createServiceContext();
            m_serviceContext.addListener(this);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("OcapAppDriverCore() - ERROR creating playback SC, exception", e);
            }
        }
        
        // Get the number of tuners (org.davic.net.tuning.NetworkInterfaces)
        // from org.davic.net.tuning.NetworkInterfaceManager
        NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
        NetworkInterface[] interfaces = nim.getNetworkInterfaces();
        m_numTuners = interfaces.length;
        
        m_bufferingEnabled = new boolean[m_numTuners];
        for (int i = 0; i < m_numTuners; i++)
        {
            m_bufferingEnabled[i] = true;   // buffer at start
        }
        
        m_eventMonitor = new Object();
        m_tuneStateMonitor = new Object();
        
        registerMediaAccessHandler();
        
        // Initialize playback related values
        m_playbackType = PLAYBACK_TYPE_UNKNOWN;
        m_playbackContentType = PLAYBACK_CONTENT_TYPE_UNKNOWN;
        m_playbackState = PLAYBACK_STATE_UNKNOWN;
    }
    
    /**
     * Registers this class as media access handler
     */
    private void registerMediaAccessHandler()
    {
        MediaAccessHandlerRegistrar.getInstance().registerMediaAccessHandler(new MediaAccessHandler()
        {
            public MediaAccessAuthorization checkMediaAccessAuthorization(
                    Player p, OcapLocator sourceURL, boolean isSourceDigital,
                    final ElementaryStream[] esList, MediaPresentationEvaluationTrigger evaluationTrigger)
            {
                double mediaTimeSeconds = p.getMediaTime().getSeconds();
                float rate = p.getRate();
                if (log.isInfoEnabled())
                {
                    log.info("MediaAccessHandler.checkAccessAuthorization() " +
                            "- Player Media time in seconds: " + mediaTimeSeconds);
                    log.info("MediaAccessHandler.checkAccessAuthorization() " +
                            "- Player Current play rate: " + rate);
                }
                return new MediaAccessAuthorization()
                {
                    public boolean isFullAuthorization()
                    {
                        return m_isFullAuth;
                    }

                    public Enumeration getDeniedElementaryStreams()
                    {
                        if (m_isFullAuth)
                        {
                            return new Vector().elements();
                        }
                        Vector vector = new Vector();
                        for (int i=0;i<esList.length;i++)
                        {
                            vector.add(esList[i]);
                        }
                        return vector.elements();
                    }

                    public int getDenialReasons(ElementaryStream es)
                    {
                        if (m_isFullAuth)
                        {
                            return 0;
                        }
                        return AlternativeMediaPresentationReason.RATING_PROBLEM;
                    }
                };
            }
        });        
    }

   /**
     * Gets an instance of the OcapAppDriverCore, but as a OcapAppDriverInterfaceCore
     * to enforce that all methods be defined in the OcapAppDriverInterfaceCore class.
     * Using lazy initialization since the constructor requires parameters, and thus
     * the s_instance cannot be instantiated at class-loading time.
   */
 
    public static OcapAppDriverInterfaceCore getOADCoreInterface()
    {
        if (s_instance == null)
        {
            s_instance = new OcapAppDriverCore();
        }
        return s_instance;
    }
    
    public void initChannelMap(boolean useJavaTVChannelMap, String fileName)
    {

        if (log.isInfoEnabled())
        {
            log.info("OcapAppDriverCore() - Use Java TV Channel Map=" + useJavaTVChannelMap);
        }
        
        m_liveServices = new LiveServiceManager();

        if (!m_liveServices.buildChannelVector(useJavaTVChannelMap, fileName))
        {
            if (log.isInfoEnabled())
            {
                log.info("OcapAppDriverCore() - could not find any services");
            }
        }
    }
    
    public void setResourceContentionHandling(boolean resContentionHandling)
    {
        if (resContentionHandling)
        {
            m_resContentionHandler = new InteractiveResourceContentionHandler();
            m_ruSorter = new InteractiveResourceUsageSorter();
            m_resContentionHandler.setResourceUsageSorter(m_ruSorter);
            ResourceContentionManager.getInstance().setResourceContentionHandler(m_resContentionHandler);
        }
    }
    
    /**
     * Adds a OCAP extension specific resource usage to list used for resource
     * contention handling.
     * 
     * @param usage OCAP extension specific usage string generator
     */
    public void addInteractiveResourceUsage(InteractiveResourceUsage usage)
    {
        if (m_ruSorter != null)
        {
            m_ruSorter.addInteractiveResourceUsage(usage);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("OcapAppDriverCore: addInteractiveResourceUsage(): m_ruSorter is null");            
            }
        }
    }
    
    ////////////////////////////////////////
    // Telnet Command public method wrappers
    //
    public boolean setTunerSyncState(int tunerIndex, boolean tunerSync)
    {
        boolean rc = false;
        try
        {
            rc = new TelnetRICmd().setTunerSyncState(tunerIndex, tunerSync);
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("OcapAppDriverCore:setTunerSyncState(): encountered exception: ", e);            
            }            
        }
        return rc;
    }
    
    //////////////////////////////////////////////////////
    // Resource Contention Handling public method wrappers
    //
    public int getNumResourceContentionHandlers()
    {
        return 0;
    }
    
    public void setResourceContentionHandled()
    {
        if (m_resContentionHandler == null)
        {
            log.error("OcapAppDriverCore: setResourceContentionHandled(): m_resContentionHandler is null");
            return;
        }
        m_resContentionHandler.setResourceContentionHandled();
    }
    
    public boolean resourceContentionActive()
    {
        if (m_resContentionHandler == null)
        {
            log.error("OcapAppDriverCore: resourceContentionActive(): m_resContentionHandler is null");
            return false;
        }
        return m_resContentionHandler.resourceContentionActive();
    }
    
    public String getReservationString(int index)
    {
        if (m_resContentionHandler == null)
        {
            log.error("OcapAppDriverCore: getReservationString(): m_resContentionHandler is null");
            return null;
        }
        return m_resContentionHandler.getReservationString(index);        
    }

    public boolean moveResourceUsageToBottom(int index)
    {
        if (m_resContentionHandler == null)
        {
            log.error("OcapAppDriverCore: moveResourceUsageToBottom(): m_resContentionHandler is null");
            return false;
        }
        return m_resContentionHandler.moveResourceUsageToBottom(index);
    }

    public int getNumReservations()
    {
        if (m_resContentionHandler == null)
        {
            log.error("OcapAppDriverCore: getNumReservations(): m_resContentionHandler is null");
            return -1;
        }
        return m_resContentionHandler.getNumReservations();        
    }

    /**
     * This class is used to store received events in the event
     * table.  This class will contain the data associated with a
     * playback related event.  The types of events
     * include ControllerEvents and ServiceContextEvents. 
     */
    private class PlaybackEvent
    {
        public final int m_eventId;
        public final int m_type;
        public final Object m_event;
        public final String m_description;
        
        public PlaybackEvent(int eventId, int type, String description, Object event)
        {
            m_eventId = eventId;
            m_event = event;
            m_type = type;
            m_description = description;
        }
    }
    
    /**
     * Implementation of method inherited from ControllerListener which
     * is called when events are received. The event received is logged 
     * and also stored in a hash map of events which include both controller
     * and service context events.  The events are stored in the hash map
     * based on key which is the event id which is assigned when the event is
     * received.
     * 
     * @param   event   controller event received
     */
    public void controllerUpdate(ControllerEvent event)
    {
        String eventName = event.getClass().getName();
        
        int firstChar = eventName.lastIndexOf('.') + 1;
        if (0 < firstChar)
        {
            eventName = eventName.substring(firstChar);
        }
        String controllerEventString = "Received ControllerEvent: " + eventName;
        if (log.isInfoEnabled())
        {
            log.info(controllerEventString);
        }
       
        synchronized (m_events)
        {
            m_events.put(new Integer(++m_currentEventIndex), 
                new PlaybackEvent(m_currentEventIndex, PLAYBACK_EVENT_TYPE_CONTROLLER,
                                    controllerEventString, event));
        }
        
        updatePlaybackStateControllerEvent(event);
        
        synchronized (m_eventMonitor)
        {
            if (log.isInfoEnabled())
            {
                log.info("controllerUpdate() - changing last player controller state, notifying all.");
            }
            m_eventMonitor.notifyAll();
        }
    }
    
    /**
     * General handler for events generated by the service context. This method
     * is used to handle all events that are of interest to this application by
     * setting internal flags based on the event received.
     *
     * All received events are displayed in the log.
     */
    public void receiveServiceContextEvent(ServiceContextEvent event)
    {
        // get the class name w/o the package prefix and...
        String contextName = event.getServiceContext().getClass().getName();
        String eventName = event.getClass().getName();
        int firstChar = contextName.lastIndexOf('.') + 1;
        if (0 < firstChar)
        {
            contextName = contextName.substring(firstChar);
        }
        firstChar = eventName.lastIndexOf('.') + 1;
        if (0 < firstChar)
        {
            eventName = eventName.substring(firstChar);
        }
        String serviceContextEventString = "Received ServiceContextEvent: " + eventName;
        if (log.isInfoEnabled())
        {
            log.info(serviceContextEventString);
        }
        
        // Add reasons for PresentationTerminatedEvent, SelectionFailedEvent,
        // and AlternativeContentErrorEvent to list of context event messages
        String reason = null;
        if (event instanceof PresentationTerminatedEvent)
        {
            PresentationTerminatedEvent pte = (PresentationTerminatedEvent)event;
            reason = eventName + "- " + getPTEReason(pte.getReason());
            if (log.isInfoEnabled())
            {
                log.info("receiveServiceContextEvent() - " + 
                        "presentation terminated, setting tune state to failed");
            }
            synchronized(m_tuneStateMonitor)
            {
                m_tuneState = TUNING_FAILED;
                m_tuneStateMonitor.notifyAll();
            }
        }
        else if (event instanceof SelectionFailedEvent)
        {
            SelectionFailedEvent sfe = (SelectionFailedEvent)event;
            reason = eventName + "- " + getSFEReason(sfe.getReason());
            if (log.isInfoEnabled())
            {
                log.info("receiveServiceContextEvent() - " + 
                        "selection failed, setting tune state to failed");
            }
            synchronized(m_tuneStateMonitor)
            {
                m_tuneState = TUNING_FAILED;
                m_tuneStateMonitor.notifyAll();
            }
        }
        else if (event instanceof AlternativeContentErrorEvent)
        {
            AlternativeContentErrorEvent acee = (AlternativeContentErrorEvent)event;
            reason = eventName + "- " + getACEEReason(acee.getReason());
            if (log.isInfoEnabled())
            {
                log.info("receiveServiceContextEvent() - " + 
                        "alternate content error, setting tune state to failed");
            }
            synchronized(m_tuneStateMonitor)
            {
                m_tuneState = TUNING_FAILED;
                m_tuneStateMonitor.notifyAll();
            }
        }
        // Set m_tuneState to TUNED if the event is NormalContentEvent
        else if (event instanceof NormalContentEvent)
        {
            reason = eventName + "- setting tune state to tuned";
            if (log.isInfoEnabled())
            {
                log.info("receiveServiceContextEvent() - " + 
                        eventName + ", setting tune state to tuned");
            }
            synchronized(m_tuneStateMonitor)
            {
                m_tuneState = TUNED;
                m_tuneStateMonitor.notifyAll();
            }
            // Add this class instance as the ControllerListener to the Player
            // This allows ControllerEvents to be received
            Player player = getPlayer();
            if (player != null)
            {
                player.addControllerListener(this);
            }
        }
        
        else
        {
            reason = eventName;
        }

        // Synchronize access to event index and event table
        synchronized (m_events)
        {
            m_events.put(new Integer(++m_currentEventIndex), 
                new PlaybackEvent(m_currentEventIndex, PLAYBACK_EVENT_TYPE_SERVICE_CONTEXT,
                        reason, event));
        }
        
        updatePlaybackStateServiceEvent(event);
        
        synchronized (m_eventMonitor)
        {
            if (log.isInfoEnabled())
            {
                log.info("receiveServiceContextEvent - sc: " + contextName +
                         ", event: " + eventName + " - notifying...");
            }

            m_eventMonitor.notifyAll();
        }
    }

    /**
     * Update playback state based on received controller event
     * during JMF Playback.
     * 
     * @param event received controller event
     */
    private void updatePlaybackStateControllerEvent(ControllerEvent event)
    {
        // Make sure there is a playback active prior to updating playback state
        if (m_playbackType == PLAYBACK_TYPE_UNKNOWN)
        {
            if (log.isDebugEnabled())
            {
                log.debug("updatePlaybackStateControllerEvent() - returning since no playback is active");
            }
            return;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("updatePlaybackStateControllerEvent() - called with current state: " +
                        getPlaybackStateStr(m_playbackState) + ", playback type: " + 
                        getPlaybackTypeStr(m_playbackType));
            }
        }
        if ((event instanceof MediaPresentedEvent) 
            || (event instanceof javax.media.StartEvent)) 
        {
            if (log.isInfoEnabled())
            {
                log.info("updatePlaybackStateControllerEvent() - setting state to presenting");
            }
            m_playbackState = PLAYBACK_STATE_PRESENTING;
        }
        else if (event instanceof BeginningOfContentEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info("updatePlaybackStateControllerEvent() - setting state to BOS");
            }
            m_playbackState = PLAYBACK_STATE_BEGINNING_OF_CONTENT;
        }
        else if ((event instanceof EndOfContentEvent) ||
                 (event instanceof EndOfMediaEvent))
        {
            if (log.isInfoEnabled())
            {
                log.info("updatePlaybackStateControllerEvent() - setting state to EOS");
            }
            m_playbackState = PLAYBACK_STATE_END_OF_CONTENT;
        }
        else if (event instanceof RateChangeEvent) 
        {
            RateChangeEvent rce = (RateChangeEvent)event;
            float newRate = rce.getRate();
            if (newRate == 0f)
            {
                if (log.isInfoEnabled())
                {
                    log.info("updatePlaybackStateControllerEvent() - setting state to pause");
                }
                m_playbackState = PLAYBACK_STATE_PAUSED;
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("updatePlaybackStateControllerEvent() - setting state to presenting");
                }
                m_playbackState = PLAYBACK_STATE_PRESENTING;                
            }
        }
        else if (event instanceof ControllerErrorEvent)
        {
                if (log.isInfoEnabled())
                {
                    log.info("updatePlaybackStateControllerEvent() - setting state to failed due to error event: " + event);
                }
                m_playbackState = PLAYBACK_STATE_FAILED;
        }
        else if (event instanceof AlternativeMediaPresentationEvent)
        {
           if (log.isInfoEnabled())
           {
               log.info("updatePlaybackStateServiceEvent() - setting state to failed due to event: " +
                       event);
           }
           m_playbackState = PLAYBACK_STATE_FAILED;
       }
        else if (event instanceof StopEvent)
        {
            // Only set for JMF playback, wait for presentation terminated for service context
            if (m_playbackType == PLAYBACK_TYPE_JMF)
            {
                if (log.isInfoEnabled())
                {
                    log.info("updatePlaybackStateControllerEvent() - setting state to unknown due to stop event: " + event);
                }
                m_playbackState = PLAYBACK_STATE_UNKNOWN;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("updatePlaybackStateControllerEvent() - ignoring stop event for service context: " + event);
                }                
            }
        }
        else // All other events are ignored
        {
            if (log.isDebugEnabled())
            {
                log.debug("updatePlaybackStateControllerEvent() - ignoring unhandled event: " + event);
            }
        }                    
        if (log.isDebugEnabled())
        {
            log.debug("updatePlaybackStateControllerEvent() - returning with current playback state: " +
                    getPlaybackStateStr(m_playbackState));
        }
    }
    
    /**
     * Update playback state based on received service context events
     * during service selection.
     * 
     * @param event received service context event
     */
    private void updatePlaybackStateServiceEvent(ServiceContextEvent event)
    {
        if (m_playbackType == PLAYBACK_TYPE_UNKNOWN)
        {
            if (log.isDebugEnabled())
            {
                log.debug("updatePlaybackStateServiceEvent() - returning since no playback is active");
            }
            return;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("updatePlaybackStateServiceEvent() - called with current state: " +
                        getPlaybackStateStr(m_playbackState) + ", playback type: " + 
                        getPlaybackTypeStr(m_playbackType));
            }
        }

        if (event instanceof NormalContentEvent) 
        {
            if (log.isInfoEnabled())
            {
                log.info("updatePlaybackStateServiceEvent() - setting state to presenting");
            }
            m_playbackState = PLAYBACK_STATE_PRESENTING;
        }
        else if ((event instanceof SelectionFailedEvent) ||
                 (event instanceof AlternativeContentErrorEvent))
        {
            if (log.isInfoEnabled())
            {
                log.info("updatePlaybackStateServiceEvent() - setting state to failed/stopped due to event: " +
                        event);
            }
            m_playbackState = PLAYBACK_STATE_FAILED;
        }
        else if (event instanceof PresentationTerminatedEvent)
        {
            // If state is not failed, set state to unknown, otherwise leave as failed
            if (m_playbackState != PLAYBACK_STATE_FAILED)
            {
                if (log.isInfoEnabled())
                {
                    log.info("updatePlaybackStateServiceEvent() - setting state to unknown due to event: " +
                            event);
                }
                m_playbackState = PLAYBACK_STATE_UNKNOWN;                
            }
        }
        else // All other events are ignored
        {
            if (log.isDebugEnabled())
            {
                log.debug("updatePlaybackStateServiceEvent() - ignored unhandled event: " + event);
            }
        }                    
        if (log.isDebugEnabled())
        {
            log.debug("updatePlaybackStateServiceEvent() - returning with current playback state: " +
                    getPlaybackStateStr(m_playbackState));
        }
    }
        
    private String getPTEReason(int reason)
    {
        String reasonString = null;
        switch(reason)
        {
            case PresentationTerminatedEvent.ACCESS_WITHDRAWN:
            {
                reasonString = "Access Withdrawn";
                break;
            }
            case PresentationTerminatedEvent.RESOURCES_REMOVED:
            {
                reasonString = "Resources Removed";
                break;
            }
            case PresentationTerminatedEvent.SERVICE_VANISHED:
            {
                reasonString = "Service Vanished";
                break;
            }
            case PresentationTerminatedEvent.TUNED_AWAY:
            {
                reasonString = "Tuned Away";
                break;
            }
            case PresentationTerminatedEvent.USER_STOP:
            {
                reasonString = "User Stop";
                break;
            }
            case PresentationTerminatedEvent.OTHER:
            {
                reasonString = "Other";
                break;
            }
            default:
            {
                reasonString = "Unknown";
            }
        }
        return reasonString;
    }
    
    private String getSFEReason(int reason)
    {
        String reasonString = null;
        switch(reason)
        {
            case SelectionFailedEvent.CA_REFUSAL:
            {
               reasonString = "CA Refusal";
               break;
            }
            case SelectionFailedEvent.CONTENT_NOT_FOUND:
            {
                reasonString = "Content Not Found";
                break;
            }
            case SelectionFailedEvent.INSUFFICIENT_RESOURCES:
            {
                reasonString = "Insufficient Resources";
                break;
            }
            case SelectionFailedEvent.INTERRUPTED:
            {
                reasonString = "Interrupted";
                break;
            }
            case SelectionFailedEvent.MISSING_HANDLER:
            {
                reasonString = "Missing Handler";
                break;
            }
            case SelectionFailedEvent.TUNING_FAILURE:
            {
                reasonString = "Tuning Failure";
                break;
            }
            case SelectionFailedEvent.OTHER:
            {
                reasonString = "Other";
                break;
            }
            default:
            {
                reasonString = "Unknown";
            }
        }
        return reasonString;
    }
    
    private String getACEEReason(int reason)
    {
        String reasonString = null;
        switch (reason)
        {
            case AlternativeContentErrorEvent.CA_REFUSAL:
            {
                reasonString = "CA Refusal";
                break;
            }
            case AlternativeContentErrorEvent.CONTENT_NOT_FOUND:
            {
                reasonString = "Content Not Found";
                break;
            }
            case AlternativeContentErrorEvent.MISSING_HANDLER:
            {
                reasonString = "Missing Handler";
                break;
            }
            case AlternativeContentErrorEvent.RATING_PROBLEM:
            {
                reasonString = "Rating Problem";
                break;
            }
            case AlternativeContentErrorEvent.TUNING_FAILURE:
            {
                reasonString = "Tuning Failure";
                break;
            }
            default:
            {
                reasonString = "Unknown";
            }
        }
        return reasonString;
    }

    /**
     * This listener logs recording playback events, and updates the flag
     * indicating the playback has started.
     */
    public void notifyRecordingPlayback(ServiceContext context,
                                        int artificialCarouselID,
                                        int[] carouselIDs)
    {
        // get the class name w/o the package prefix
        String name = context.getClass().getName();
        int firstChar = name.lastIndexOf('.') + 1;

        if (0 < firstChar)
        {
            name = name.substring(firstChar);
        }

        if (null != carouselIDs)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recording playback event received for: " + name +
                         ", artificialCarouselID = " + artificialCarouselID);
            }

            for (int i = 0; i < carouselIDs.length; i++)
            {
                if (log.isInfoEnabled())
                {
                    log.info("carouselIDs[" + i + "] = " + carouselIDs[i]);
                }
            }
        }
        else if (log.isInfoEnabled())
        {
            log.info("Recording playback event received for: " + name);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    /// Tuning
    //

    public String getServiceInfo(int serviceIndex)
    {
        Vector services = m_liveServices.getServiceList();
        Service svc = (Service) services.elementAt(serviceIndex);
        return ((OcapLocator)svc.getLocator()).toString();
    }

    public int getNumTuners()
    {
        return m_numTuners;
    }
    
    public int getServiceIndex()
    {
        if (m_serviceContext != null)
        {
            Service svc = m_serviceContext.getService();
            Vector services = m_liveServices.getServiceList();

            for (int i = 0; i < services.size(); i++)
            {
                Service testSvc = (Service) services.elementAt(i);

                if (svc == testSvc)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("current service = " + svc.getName());
                    }
                    return i;
                }
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("getServiceIndex() - service context is null");
            }
        }
        return -1;
    }
    
    public int getNumServices()
    {
        Vector services = m_liveServices.getServiceList();
        return services.size();
    }

    public String getServiceName(int serviceIndex)
    {
        Vector services = m_liveServices.getServiceList();
        if (serviceIndex < services.size())
        {
            Service svc = (Service) services.elementAt(serviceIndex);
            return svc.getName();
        }
        else
        {
            return null;
        }
    }

    public boolean serviceSelectByName(String serviceName)
    {
        boolean retVal = false;
        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("serviceSelectByName() - service context is null");
            }
            return retVal;
        }
        
        // Set m_tuned to false until a new Service has been successfully selected
        if (log.isInfoEnabled())
        {
            log.info("serviceSelectByName() - setting tune state to tuning");
        }
        m_tuneState = TUNING;
        Vector services = m_liveServices.getServiceList();

        for (int i = 0; i < services.size(); i++)
        {
            Service svc = (Service) services.elementAt(i);

            if (getInformativeChannelName(i).equals(serviceName))
            {
                try
                {
                    // select the given service
                    m_serviceContext.select(svc);
                    retVal = true;
                    m_liveServices.setServiceIndex(i);
                    
                    if (System.getProperty("ocap.api.option.dvr") != null)
                    {
                        if (OcapAppDriverDVR.getOADDVRInterface().isTsbEnabled())
                        {
                            m_playbackType = PLAYBACK_TYPE_TSB;
                        }
                    }

                    if (log.isInfoEnabled())
                    {
                        log.info("selected service = " + svc.getName());
                    }
                }
                catch (Exception e)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("ERROR in ServiceSelectByName()", e);
                    }
                }
                break;
            }
        }

        return retVal;
    }

    public boolean serviceSelectByIndex(int serviceIndex)
    {
        boolean retVal = false;
        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("serviceSelectByIndex() - service context is null");
            }
            return retVal;
        }

        // Set m_tuned to false until a new Service has been successfully selected
        if (log.isInfoEnabled())
        {
            log.info("serviceSelectByIndex() - setting tune state to tuning");
        }
        m_tuneState = TUNING;
        Vector services = m_liveServices.getServiceList();
        Service svc = (Service) services.elementAt(serviceIndex);
        try
        {
            // select the given service   
            m_serviceContext.select(svc);
            m_liveServices.setServiceIndex(serviceIndex);
            retVal = true;
            
            if (System.getProperty("ocap.api.option.dvr") != null)
            {
                if (OcapAppDriverDVR.getOADDVRInterface().isTsbEnabled())
                {
                    m_playbackType = PLAYBACK_TYPE_TSB;
                }
            }

            if (log.isInfoEnabled())
            {
                log.info("selected service = " + svc.getName());
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("ERROR in ServiceSelectByIndex()", e);
            }
        }
        return retVal;
    }
    
    public void channelUp()
    {
        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("channelUp() - service context is null");
            }
            return;
        }
        
        try
        {
            // Set m_tuned to false until a new Service has been successfully selected
            m_tuneState = TUNING;
            m_serviceContext.select(m_liveServices.getNextService());
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception in channelUp()", e);
            }
        }
    }
    
    public void channelDown()
    {
        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("channelDown() - service context is null");
            }
            return;
        }
        
        try
        {
            // Set m_tuned to false until a new Service has been successfully selected
            m_tuneState = TUNING;
            m_serviceContext.select(m_liveServices.getPreviousService());
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception in channelDown()", e);
            }
        }
    }
        
    /**
     * {@inheritDoc}
     * Since OcapTuner is no longer being used, the ServiceContext can only
     * detect a tuned state (NormalContentEvent) or a tune failed state. When the
     * ServiceContext selects a new Service, it may receive a SelectionFailedEcent.
     * In that case, the ServiceContext will attempt to select the previous Service, 
     * which will generate a NormalContentEvent if the selection is successful. 
     * In order to avoid a false positive, this method waits to be notified from
     * the first relevant event that would indicate tune status (NormalContentEvent,
     * SelectionFailedEvent, PresentationTerminatedEvent, AlternativeContentEvent)
     * and then returns true if m_tuneState has reached the desired state. 
     */
    public boolean waitForTuningState(long timeout, int tuningState)
    {
        if (log.isInfoEnabled())
        {
            log.info("waitForTuningState() - Waiting " + timeout + " seconds for tuning state: " 
                    + tuningState + ", current state: " + m_tuneState);
        }
        synchronized(m_tuneStateMonitor)
        {
        	// In order to avoid a race condition, check whether the tuning state has already
        	// been achieved
            if(m_tuneState == tuningState)
            {
                if (log.isInfoEnabled())
                {
                    log.info("waitForTuningState() - current state is already " + m_tuneState + ", returning...");
                }
                return true;
            }

            try
            {
                // Wait to be notified from receiveServiceContextEvent()
                m_tuneStateMonitor.wait(timeout * 1000);
            }
            catch (InterruptedException e)
            {
                // Ignore InterruptedException
            }
        }
        return m_tuneState == tuningState;
    }
    
    public void resetLiveServiceIndex()
    {
        int currentServiceIndex = getServiceIndex();
        if (currentServiceIndex > -1)
        {
            m_liveServices.setServiceIndex(currentServiceIndex);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("resetServiceIndex() - error getting current Service index");
            }
        }
    }
    
    public long getMediaTime()
    {
        //method called in paint - not logging if there are issues retrieving a player
        Player player = getPlayer();

        long timeNS = Long.MIN_VALUE;
        if (player != null)
        {
            timeNS = player.getMediaNanoseconds();
        }
        return timeNS;
    }
    
    public long getPlaybackDurationNS()
    {
        long durationNS = Long.MIN_VALUE;
        Time timeDuration = getPlaybackDuration();
        if (timeDuration != null)
        {
            durationNS = timeDuration.getNanoseconds();
        }
        return durationNS;

    }
    
    public double getPlaybackDurationSecs()
    {
        double durationSecs = -1;
        Time timeDuration = getPlaybackDuration();
        if (timeDuration != null)
        {
            durationSecs = timeDuration.getSeconds();
        }
        return durationSecs;
    }
    
    private Time getPlaybackDuration()
    {
        Time durationTime = null;
        Player player = getPlayer();
        if (player != null)
        {
            if (log.isTraceEnabled())
            {
                log.trace("ServiceContext player - returning duration: " + 
                        player.getDuration().getNanoseconds() + ", seconds: " +
                        player.getDuration().getSeconds());
            }
            
            durationTime = player.getDuration();
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("No serviceContext player - unable to obtain duration");
            }
        }
        return durationTime;
    }
    
    public void runAuthorization(boolean toggleAuthorizationFirst)
    {
        if (toggleAuthorizationFirst)
        {
            m_isFullAuth = !m_isFullAuth;
        }
        if (log.isInfoEnabled())
        {
            log.info("Triggering authorization check with full authorization set to: " + m_isFullAuth);
        }
        Player player = getPlayer();
        if (null != player)
        {
            MediaAccessConditionControl control = (MediaAccessConditionControl)player.getControl("org.ocap.media.MediaAccessConditionControl");
            if (control != null)
            {
                control.conditionHasChanged(MediaPresentationEvaluationTrigger.USER_RATING_CHANGED);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("No MediaAccessConditionControl found");
                }
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Service context player not found");
            }
        }
    }
    
    ////////////////////////////////////////////////////////////////////////
    /// Generic Playback
    //

    /**
     * Returns the current player based on current player type.
     * NOTE: This method is public because it is used in OAD DVR.
     * 
     * @return  currently active player, maybe JMF or Service,
     *          null if player type is invalid
     */
    public Player getPlayer()
    {
        Player retVal = null;
        if (m_playbackType == PLAYBACK_TYPE_JMF)
        {
            retVal = m_jmfPlayer;
        }
        else if (m_playbackType == PLAYBACK_TYPE_SERVICE || 
                m_playbackType == PLAYBACK_TYPE_TSB)
        {
            if (m_serviceContext == null)
            {
                return retVal;
            }
            
            ServiceContentHandler[] handlers = m_serviceContext.getServiceContentHandlers();
            for (int i = 0; i < handlers.length; i++)
            {
                if (handlers[i] instanceof Player)
                {
                    retVal = (Player) handlers[i];
                    break;
                }
            }
        }

        return retVal;
    }
    
    /** 
     * {@inheritDoc}
     * The playback at the given index is transformed once the next Service is 
     * selected, so this method should preceed the selection of the Service that
     * is intended to be resized. Otherwise, the resizing won't take affect
     * until the next Service is selected.
     */
    public boolean playbackTransformVideo(float horizontalScalingFactor,
                                          float verticalScalingFactor,
                                          int startX,
                                          int startY)
    {
        boolean retVal = false;
        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("playbackTransformVideo() - service context is null");
            }
            return retVal;
        }
        
        try
        {
            VideoTransformation videoTransform = new VideoTransformation(null, (float) 0.5, (float) 0.5,
                    new HScreenPoint(0, 0));
            DvbServiceContext dsc = (DvbServiceContext)m_serviceContext;
            dsc.setDefaultVideoTransformation(videoTransform);
            retVal = true;
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Exception in playbackTransformVideo()",  e);
            }
        }
        return retVal;
    }
    
    /**
     * {@inheritDoc}
     * This method sets the given playback index to fullscreen. This method 
     * is intended to be used for remote and DVR playback only. Resizing the
     * playback to occupy a smaller portion of the screen should be done with
     * playbackTransformVideo() when playback is a live service.
     */
    public boolean setPlaybackFullscreen()
    {
        boolean retVal =  false;
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenDim = toolkit.getScreenSize();
        int screenHeight = screenDim.height;
        int screenWidth = screenDim.width;
        Player player = getPlayer();
        if (player != null)
        {
            AWTVideoSizeControl avsc = (AWTVideoSizeControl) player.getControl("javax.tv.media.AWTVideoSizeControl");
            Rectangle source = new Rectangle(screenWidth, screenHeight);
            Rectangle dest = new Rectangle(screenWidth, screenHeight);
            AWTVideoSize avs = new AWTVideoSize(source, dest);
            retVal = avsc.setSize(avs);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Unable to set playback to fullscreen. Player is null");
            }
        }
        return retVal;
    }
    
    /**
     * Starts playback of the specified type either JMF or service select.
     * It will stop any existing playbacks that may be active.  This method
     * will not return until the playback is in the presenting state or has
     * failed.
     * 
     * @param   playerType          either JMF or Service type playback
     * @param   playbackContentType type of content to playback, maybe 
     *                              one of the PLAYBACK_CONTENT_TYPE_* values.
     * @param   videoURL            URL of content to playback using JMF player, 
     *                              maybe null if service playback
     * @param   service             service to playback if using service type player,
     *                              null if JMF type playback
     * @param   waitTimeSecs        amount of time in seconds to wait for playback to start
     *   
     * @return true if successful, false if an error occurs
     */
    public boolean playbackStart(int playerType, int playbackContentType,
                                 String videoURL, Service service, int waitTimeSecs)
    {
        boolean isPresenting = false;
        if (log.isInfoEnabled())
        {
            log.info("playbackStart() - called");
        }

        // Stop playback if currently active
        if ((m_playbackState != PLAYBACK_STATE_UNKNOWN) &&
            (m_playbackState != PLAYBACK_STATE_FAILED))
        {
            if (log.isInfoEnabled())
            {
                log.info("playbackStart() - playback state in unexpected state: " + 
                        getPlaybackStateStr(m_playbackState) + ", calling stop");
            }
            playbackStop(waitTimeSecs);
        }

        m_playbackType = playerType;
        m_playbackContentType = playbackContentType;
        m_playbackState = PLAYBACK_STATE_UNKNOWN;
        if (log.isInfoEnabled())
        {
            log.info("playbackStart() - resetting state: " + getPlaybackStateStr(m_playbackState));
        }
 
        if (m_playbackType == PLAYBACK_TYPE_JMF)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("playbackStart() - type is JMF");
            }
            isPresenting = playbackStartJMF(videoURL, waitTimeSecs);
        }
        else if (m_playbackType == PLAYBACK_TYPE_SERVICE)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("playbackStart() - type is Service");
            }
            isPresenting = playbackStartService(service, waitTimeSecs);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playbackStart() - unsupported player type: " + playerType);
            }            
        }
        
        return isPresenting;
    }
    
    /**
     * Starts up playback using JMF player using supplied URL as content.
     * 
     * @param videoURL  content to playback
     * @param waitTimeSecs  number of seconds to wait for playback to start
     * 
     * @return  true if playback is presenting, false if failed to present content
     */
    private boolean playbackStartJMF(String videoURL, int waitTimeSecs)
    {
        boolean isPresenting = false;
        
        // Get the index prior to starting
        int curEventIdx = m_currentEventIndex;

        if (videoURL != null)
        {
            m_videoURL = videoURL;
            if (log.isInfoEnabled())
            {
                log.info("playbackStartJMF() - about to create Player for URL: " + m_videoURL);
            }

            try
            {
                m_jmfPlayer = Manager.createPlayer(new MediaLocator(m_videoURL));
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("playbackStartJMF() - exception creating JMFPlayer", e);
                }                
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playbackStartJMF() - passed NULL video url for JMF playback");
            }                
        }

        if (m_jmfPlayer != null)
        {
            m_playbackState = PLAYBACK_STATE_UNKNOWN;
            if (log.isDebugEnabled())
            {
                log.debug("playbackStartJMF() - resetting state to: " +
                        getPlaybackStateStr(m_playbackState));
            }                
            m_jmfPlayer.addControllerListener(this);
            m_jmfPlayer.start();
            
            int curTimeSecs = 0;
            boolean isStarted = false;
            int idx = -1;
            do
            {
                if (log.isDebugEnabled())
                {
                    log.debug("playbackStartJMF() - elapsed time waiting for start: " +
                            curTimeSecs + ", current state: " + getPlaybackStateStr(m_playbackState));
                }                
                idx = playbackWaitForNextEvent(PLAYBACK_EVENT_TYPE_CONTROLLER, curEventIdx, 1);
                if (idx != -1)
                {
                    curEventIdx = idx + 1;
                }
                else
                {
                    // No event so we must have timed out
                    curTimeSecs++;                    
                }
                if (m_playbackState > PLAYBACK_STATE_UNKNOWN)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("playbackStartJMF() - started based on current state: " + 
                                getPlaybackStateStr(m_playbackState));
                    }                
                    isStarted = true;
                }
            }
            while ((!isStarted) && (curTimeSecs < waitTimeSecs));
            
            if (isStarted)
            {
                isPresenting = true;                
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("playbackStartJMF() - did not see JMFPlayer start within " +
                            waitTimeSecs + " secs");
                }                                
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playbackStartJMF() - problems creating JMFPlayer");
            }                
        }                        
        return isPresenting;
    }
    
    /**
     * Starts up playback using service context select on supplied service 
     * and uses associated player.
     * 
     * @param service  content to playback
     * @param waitTimeSecs  number of seconds to wait for playback to start
     * 
     * @return  true if playback is presenting, false if failed to present content
     */
    private boolean playbackStartService(Service service, int waitTimeSecs)
    {
        if (log.isDebugEnabled())
        {
            log.debug("playbackStartService() - called for service: " + service);
        }
        boolean isPresenting = false;
        
        // Get the index prior to starting
        int curEventIdx = m_currentEventIndex;

        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("playbackStartService() - service context is null");
            }
            return isPresenting;
        }
        
        if (service != null)
        {
            try
            { 
                m_playbackState = PLAYBACK_STATE_UNKNOWN;
                m_serviceContext.addListener(this);
                m_serviceContext.select(service);

                // Wait for service selection...
                boolean isSelected = false;
                int nextIdx = -1;
                int curTimeSecs = 0;
                do
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("playbackStartService() - elapsed time waiting for start: " +
                                curTimeSecs);
                    }                
                    nextIdx = playbackWaitForNextEvent(PLAYBACK_EVENT_TYPE_SERVICE_CONTEXT, curEventIdx, 1);
                    if (nextIdx != -1)
                    {
                        curEventIdx = nextIdx + 1;
                    }
                    else
                    {
                        // No event so must of timed out
                        curTimeSecs++;
                    }
                    if (m_playbackState > PLAYBACK_STATE_UNKNOWN)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("playbackStartService() - state changed from UNKNOWN to: " + getPlaybackStateStr(m_playbackState));
                        }
                        isSelected = true;
                    }
                }
                while ((!isSelected) && (curTimeSecs < waitTimeSecs));

                // If service selection worked, add listener
                Player player = getPlayer();
                if (player != null)
                {
                    // In order to avoid duplicating ControllerEvent messages
                    // remove this instance as the Player's ControllerListener
                    player.removeControllerListener(this);
                    player.addControllerListener(this);
                    if (log.isInfoEnabled())
                    {
                        log.info("playbackStartService() - Player is not null - returning true");
                    }
                    isPresenting = true;
                }
                else
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("playbackStartService() - Player is null - selection did not succeed - returning false");
                    }
                }
             }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("playbackStartService() - exception starting playback - returning false", e);
                }
            }                
        }
        return isPresenting;
    }
    
    public boolean playbackStop(int waitTimeSecs)
    {
        if (log.isDebugEnabled())
        {
            log.debug("playbackStop() - called");
        }
        boolean wasStopped = false;

        // Get the index prior to stopping
        int curEventIdx = m_currentEventIndex;
        
        if (m_playbackType == PLAYBACK_TYPE_JMF)
        {
            if (m_jmfPlayer != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("playbackStop() - calling jmf player stop");
                }
                m_jmfPlayer.stop();
         
                int curTimeSecs = 0;
                boolean isDone = false;
                int idx = -1;
                do
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("playbackStop() - elapsed time waiting for stop: " +
                                curTimeSecs);
                    }                
                    idx = playbackWaitForNextEvent(PLAYBACK_EVENT_TYPE_CONTROLLER, curEventIdx, 1);
                    if (idx != -1)
                    {
                        curEventIdx = idx + 1;
                    }
                    else
                    {
                        // No event so must of timed out
                        curTimeSecs++;
                    }
                    if ((m_playbackState == PLAYBACK_STATE_FAILED) || 
                        (m_playbackState == PLAYBACK_STATE_UNKNOWN))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("playbackStop() - is stopped");
                        }
                        wasStopped = true;
                        isDone = true;
                    }
                }
                while ((!isDone) && (curTimeSecs < waitTimeSecs));

                if (m_jmfPlayer != null)
                {
                    // Destroy any existing player
                    destroyJmfPlayer();
                }            
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("playbackStop() - jmf player was already null");
                }
            }
        }
        else // m_playerType == SERVICE_PLAYBACK
        {
            if (m_serviceContext != null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("playbackStop() - calling service context stop");
                }
                m_serviceContext.stop();
                
                // Wait for playback to stop...
                boolean isDone = false;
                int curTimeSecs = 0;
                int idx = -1;
                do
                {
                    idx = playbackWaitForNextEvent(PLAYBACK_EVENT_TYPE_SERVICE_CONTEXT, curEventIdx, 1);
                    if (idx != -1)
                    {
                        curEventIdx = idx + 1;
                    }
                    if ((m_playbackState == PLAYBACK_STATE_FAILED) || 
                        (m_playbackState == PLAYBACK_STATE_UNKNOWN))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("playbackStop() - done since playback state transistioned to: " + 
                                    getPlaybackStateStr(m_playbackState));
                        }
                        isDone = true;
                        wasStopped = true;
                    }
                    curTimeSecs++;
                    if (log.isDebugEnabled())
                    {
                        log.debug("playbackStop() - waiting " + waitTimeSecs + " secs for event, elapsed secs: " +
                                    curTimeSecs);
                    }
                }
                while ((!isDone) && (curTimeSecs < waitTimeSecs));
                
                if (wasStopped)
                {
                    Player player = getPlayer();
                    if (player != null)
                    {
                        player.removeControllerListener(this);
                    }
                }
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("playbackStop() - service context is null");
                }
                wasStopped = true;                
            }
        }

        // Reset to default values
        m_playbackState = PLAYBACK_STATE_UNKNOWN;
        m_playbackType = PLAYBACK_TYPE_UNKNOWN;
        m_playbackContentType = PLAYBACK_CONTENT_TYPE_UNKNOWN;
        
        return wasStopped;
    }

    public boolean serviceSelectStop(int waitTimeSecs)
    {
        boolean wasStopped = false;
    
        if (m_serviceContext != null)
        {
            int curEventIdx = m_currentEventIndex;

            if (log.isInfoEnabled())
            {
                log.info("serviceSelectStop() - calling service context stop with current playback state: "
                        + getPlaybackStateStr(m_playbackState));
            }
            m_serviceContext.stop();
            
            // Wait for presentation terminated event
            boolean isDone = false;
            int curTimeSecs = 0;
            int idx = -1;
            do
            {
                if (log.isDebugEnabled())
                {
                    log.debug("serviceSelectStop() - elapsed time waiting for stop: " +
                            curTimeSecs);
                }                
                // Wait for service context to stop which is notified via PresentationTerminatedEvent
                // which when received will set the tune state to failed 
                idx = playbackWaitForNextEvent(PLAYBACK_EVENT_TYPE_SERVICE_CONTEXT, curEventIdx, 1);
                if (idx != -1)
                {
                    curEventIdx = idx + 1;
                }
                else
                {
                    // No event so must of timed out
                    curTimeSecs++;
                }
                if (m_tuneState == TUNING_FAILED)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("serviceSelectStop() - done since tune transistioned to: " + m_tuneState);
                    }
                    isDone = true;
                    wasStopped = true;
                }
            }
            while ((!isDone) && (curTimeSecs < waitTimeSecs));
            
            // Need to remove player listener
            Player player = getPlayer();
            if (player != null)
            {
                player.removeControllerListener(this);
            }
        } 
        return wasStopped;
    }
    
    /**
     * Performs actions necessary to destroy JMF player.
     * 
     * @return  true if actions are performed without exception, false otherwise
     */
    private boolean destroyJmfPlayer()
    {
        boolean wasDeleted = false;
        if (m_jmfPlayer != null)
        {
            try
            {
                m_jmfPlayer.stop();
                m_jmfPlayer.deallocate();
                m_jmfPlayer.close();
                m_jmfPlayer.removeControllerListener(this);
                m_jmfPlayer = null;
                wasDeleted = true;
            }
            catch (Throwable t)
            {
                if (log.isErrorEnabled())
                {
                    log.error("destroyJmfPlayer(): exception while destroying ", t);
                }
            }
        }
        return wasDeleted;
    }
    
    public int playbackGetState()
    {
        return m_playbackState;
    }

    /**
     * Utility method which returns a string describing supplied playback state.
     * 
     * @param playbackState get string representation of this state
     * @return  string describing supplied state
     */
    private String getPlaybackStateStr(int playbackState)
    {
        return OcapAppDriverInterfaceCore.PLAYBACK_STATE_STRS[playbackState];
    }

    private String getPlaybackTypeStr(int playbackType)
    {
        String str = PLAYBACK_TYPE_STR_UNKNOWN;
        switch (playbackType)
        {
        case PLAYBACK_TYPE_SERVICE:
            str = PLAYBACK_TYPE_STR_SERVICE;
            break;
        case PLAYBACK_TYPE_JMF:
            str = PLAYBACK_TYPE_STR_JMF;
            break;
        case PLAYBACK_TYPE_TSB:
            str = PLAYBACK_TYPE_STR_TSB;
            break;
        }
        return str;
    }
    
    private String getEventTypeStr(int type)
    {
        String str = PLAYBACK_EVENT_TYPE_STR_UNKNOWN;
        switch (type)
        {
        case PLAYBACK_EVENT_TYPE_CONTROLLER:
            str = PLAYBACK_EVENT_TYPE_STR_CONTROLLER;
            break;
        case PLAYBACK_EVENT_TYPE_SERVICE_CONTEXT:
            str = PLAYBACK_EVENT_TYPE_STR_SERVICE_CONTEXT;
            break;
        }
        return str;        
    }
    
    public float getPlaybackRate()
    {
        float retVal = Float.NaN;
        Player player = getPlayer();
        if (null != player)
        {
            retVal = player.getRate();
        }

        return retVal;
    }
    
    public boolean playbackChangeRate(int rateIndex)
    {
        boolean isRateSet = false;
        
        float rate = m_playRates[rateIndex];

        Player player = getPlayer();
        if (player != null)
        {
            float newRate = player.setRate(rate);
            isRateSet = (Float.compare(newRate,rate) == 0);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("playbackChangeRate() - Player is null");
            }
        }
        
        return isRateSet;
    }

    public long getPlaybackPosition()
    {
        //long retVal = 0;
        long retVal = Long.MIN_VALUE;
        Player player = getPlayer();
        if (null != player)
        {
            retVal = player.getMediaTime().getNanoseconds();
        }

        return retVal;
    }
    
    public double getPlaybackPositionSecs()
    {
        //double retVal = 0;
        double retVal = Long.MIN_VALUE;
        Player player = getPlayer();
        if (null != player)
        {
            retVal = player.getMediaTime().getSeconds();
        }

        return retVal;
    }
    
    public boolean setPlaybackPosition(long time)
    {
        boolean positionSet = false;
        Player player = getPlayer();
        if (player != null)
        {
            int curEventIdx = m_currentEventIndex;
            player.setMediaTime(new Time(time));
            playbackWaitForNextEvent(PLAYBACK_EVENT_TYPE_CONTROLLER, curEventIdx, 1);
            positionSet = true;
        }
        return positionSet;
    }
    
    public void skipForward(int seconds)
    {
        Player player = getPlayer();
        if (player != null)
        {
            Time newMediaTime = new Time(player.getMediaTime().getSeconds() + seconds);
            if (log.isInfoEnabled())
            {
                log.info("Skipping " + seconds + " seconds forward to: " + newMediaTime);
            }
            player.setMediaTime(newMediaTime);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("skipForward() - Player is null");
            }
        }
    }
        
    public void skipBackward(int seconds)
    {
        Player player = getPlayer();
        if (player != null)
        {
            Time newMediaTime = new Time(Math.max(0, player.getMediaTime().getSeconds() - seconds));
            if (log.isInfoEnabled())
            {
                log.info("Skipping " + seconds + " seconds backward to: " + newMediaTime);
            }
            player.setMediaTime(newMediaTime);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("skipBackward() - Player is null");
            }
        }
    }
    
    public float getPlayRate(int rateIndex)
    {
        if ((rateIndex >= 0) && (rateIndex < m_playRates.length))
        {
            return m_playRates[rateIndex];
        }
        else
        {
            return Float.NaN;
        }
    }
    
    public int getPlayRateIndex(float rate)
    {
        for (int i = 0; i < m_playRates.length; i++)
        {
            if (rate == m_playRates[i])
                return i;
        }

        return -1;
    }
    
    ////////////////////////////////////////////////////////////////////////
    /// Playback Events
    //

    public int playbackEventGetLastIndex()
    {
        return m_currentEventIndex;
    }

    public int playbackEventGetType(int eventIndex)
    {
        int type = -1;
        
        PlaybackEvent event = (PlaybackEvent)m_events.get(new Integer(eventIndex));
        if (event != null)
        {
            type = event.m_type;
        }
        
        return type;
    }

    public String playbackEventGetDescription(int eventIndex)
    {
        String description = null;
        
        PlaybackEvent event = (PlaybackEvent)m_events.get(new Integer(eventIndex));
        if (event != null)
        {
            description = event.m_description;
        }
        
        return description;
    }

    public int playbackWaitForNextEvent(int type, int startEventIndex, long timeoutSecs)
    {
        // Only print out msg if not one since when equal to one this is redundant with other msgs
        //if (timeoutSecs != 1)
        //{
            if (log.isDebugEnabled())
            {
                log.debug("playbackWaitForNextEvent - type: " + getEventTypeStr(type) +
                        ", timeout secs: " + timeoutSecs + ", start idx: " + startEventIndex +
                        ", current idx: " + m_currentEventIndex);
            }
        //}
        // Determine if event has already been received
        int startIndex = startEventIndex;
        while (m_currentEventIndex > startIndex)
        {
            PlaybackEvent event = (PlaybackEvent)m_events.get(new Integer(startIndex));
            if (event != null)
            {
                if (event.m_type == type)
                {
                    // Found event of requested type which has been received already
                    if (log.isTraceEnabled())
                    {
                        log.trace("playbackWaitForNextEvent - found event with type: " + getEventTypeStr(type) +
                                ", returning index: " + startIndex);
                    }
                    return startIndex;
                }
            }
            startIndex++;
        }
        
        // Did not receive the event already, wait to be notified
        long endTimeSecs = (System.currentTimeMillis() / 1000) + timeoutSecs;
        while ((System.currentTimeMillis() / 1000) < endTimeSecs)
        {
            try
            {
                synchronized(m_eventMonitor)
                {
                    m_eventMonitor.wait(1000);
                }
            }
            catch (InterruptedException e)
            {
                // Ignore interruptions
            }
            
            // Determine if event waiting for has been received
            while (m_currentEventIndex > startIndex)
            {
                PlaybackEvent event = (PlaybackEvent)m_events.get(new Integer(startIndex));
                if (event != null)
                {
                    if (event.m_type == type)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Found event of type: " + getEventTypeStr(type) + 
                                    ", returning index: " + startIndex);
                        }
                        // Found event of requested type
                        return startIndex;
                    }
                }
                startIndex++;
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("playbackWaitForNextEvent - returning -1");                    
        }
        return -1;
    }

    public String getOrganization()
    {
        return m_organization;
    }
    
    public void setOrganization(String oid)
    {
        if (oid == null)
        {
            m_organization = null;
            
            if (log.isInfoEnabled())
            {
                log.info("setOrganization() - ocap:organization set to null, recording playback authentication disabled");
            }
        }
        else
        {
            m_organization = "00000000".substring(0, 8 - oid.length()) + oid;

            if (log.isInfoEnabled())
            {
                log.info("setOrganization() - ocap:organization set to " + m_organization + ", recording playback authentication enabled");
            }
        }
    }

   

    public String getInformativeChannelName(int serviceIndex)
    {
        Vector vn = m_liveServices.getChannelName();
        String name = UNKNOWN;
        if (vn == null)
        {
            // return UNKNOWN
            return name;
        }
        if (serviceIndex < vn.size())
        {
            name = (String)vn.get(serviceIndex);
            if (log.isInfoEnabled())
            {
                log.info("Channel name is " + (String)vn.get(serviceIndex));
            }
        }
        return name;
    }
    
    public String getInformativeChannelName(Service service)
    {
        Vector vs = m_liveServices.getServiceList();
        Vector vn = m_liveServices.getChannelName();
        String name = " ";
        int unknownCount = 1;
        for ( int i = 0; i < vs.size(); i++){
            Service cs = (Service) vs.get(i);
            if ( cs.getLocator().equals (service.getLocator())) 
            {   
                name = (String) vn.get(i);
                if (name.equals(m_liveServices.UNKNOWN)) 
                {
                    name = service.getName();
                    if (name.equals("") )
                    {
                        name = UNKNOWN + " " + unknownCount; 
                        unknownCount++;
                    }
                }
                break;
            }
        } 
        return name;
    }
    
    public int getCCIBits()
    {
        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("getCCIBits() - service context is null");
            }
            return -1;
        }
        
        int cci = CopyControl.getCCIBits(m_serviceContext.getService());
        return cci;
    }
    
    /**
     * {@inheritDoc}
     * Not currently implemented.
     */
    public String getResourceContentionHandlerInfo(int rchIndex)
    {
        return "not implemented";
    }
    
    /**
     * {@inheritDoc}
     * Not currently implemented.
     */
    public boolean preselectResourceContentionHandler(int rchIndex)
    {
        return false;
    }
    
    /**
     * {@inheritDoc}
     * Not currently implemented.
     */
    public boolean selectResourceContentionHandler(int rchIndex)
    {
        return false;
    }

    public Vector getServicesList()
    {
        return m_liveServices.getServiceList();
    }
    
    public ServiceContext getPlaybackServiceContext()
    {
        return m_serviceContext;
    }

    public Service getPlaybackService()
    {
        if (m_serviceContext == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("getPlaybackService() - service context is null");
            }
            return null;
        }
        return m_serviceContext.getService();
    }

    public String [] getLanguagePreference()
    {
        UserPreferenceManager manager = UserPreferenceManager.getInstance();

        GeneralPreference pref = new GeneralPreference("User Language");
        try
        {
            manager.read(pref);
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("setLanguagePreference() - error writing preference", e);
            }
            return new String[0];
        }
        return pref.getFavourites();
    }
    
    public void setLanguagePreference(String[] languages)
    {
        UserPreferenceManager manager = UserPreferenceManager.getInstance();

        GeneralPreference pref = new GeneralPreference("User Language");
        pref.add(languages);
        try
        {
            manager.write(pref);
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("setLanguagePreference() - error writing preference", e);
            }
        }
    }    
}

