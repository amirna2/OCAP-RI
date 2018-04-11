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

package org.cablelabs.impl.ocap.manager.eas;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URL;
import java.util.Iterator;

import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Duration;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.StopEvent;
import javax.media.Time;
import javax.tv.locator.Locator;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage.EASAudioFileObjectCarouselSource;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage.EASAudioFileSource;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage.EASDescriptor;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.net.InvalidLocatorException;
import org.dvb.dsmcc.DSMCCObject;
import org.dvb.dsmcc.InvalidPathNameException;
import org.dvb.dsmcc.NotLoadedException;
import org.dvb.dsmcc.ServiceDomain;
import org.ocap.media.AlternativeMediaPresentationEvent;
import org.ocap.media.NormalMediaPresentationEvent;
import org.ocap.net.OcapLocator;
import org.ocap.system.EASEvent;
import org.ocap.system.EASHandler;
import org.ocap.system.EASListener;

/**
 * A concrete instance of {@link EASAlert} that represents the text+audio alert
 * strategy. This strategy scrolls the formatted alert text along the top edge
 * of the screen from right to left, while playing back an audio override track
 * in the background. The entire text is guaranteed to be scrolled at least once
 * unless the presentation is forcibly terminated.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class EASAlertTextAudio extends EASAlertTextOnly
{
    /**
     * An inner class that encapsulates the playing back of an audio source.
     * Some operations occur asynchronously so a callback mechanism is used to
     * notify the <code>EASAlert</code> strategy when the playback has started,
     * when the playback has terminated, or when the playback has failed.
     */
    class EASPlayer implements ControllerListener
    {
        private EASAlert m_alertCallback;

        private Player m_player;

        /**
         * Constructs a new instance of the receiver with a <code>url</code>
         * indicating the audio file to play back.
         */
        public EASPlayer(final EASAlert callback, final URL url) throws NoPlayerException, IOException
        {
            this.m_alertCallback = callback;

            if (log.isInfoEnabled())
            {
                log.info("Creating player for URL:<" + url + ">");
            }
            this.m_player = Manager.createPlayer(url);
            this.m_player.addControllerListener(this);
        }

        /*
         * ControllerListener for audio file source case - DO NOT register as ControllerListener when presenting via ServiceContext,
         * as an AlternativeMediaPresentationEvent will be received when AltContentErrorEvent is received, and ServiceContext presentation may recover
         * due to AltContent, but standalone JMF player playback is assumed to not recover (and signals presentationFailed when  
         * an AlternativeMediaPresentationEvent is received in this controllerlistener.
         *  
         * @see javax.media.ControllerListener#controllerUpdate(javax.media.ControllerEvent)
         */
        public void controllerUpdate(ControllerEvent event)
        {
            if (event instanceof AlternativeMediaPresentationEvent)
            {
                // Presenting alternative content (if any) which is not what we
                // want - not presenting via servicecontext, no recovery, notify presentationfailed
                if (log.isWarnEnabled())
                {
                    log.warn("Alternative content playing back, stopping presentation");
                }
                dispose();
                this.m_alertCallback.presentationFailed();
            }
            if (event instanceof ControllerErrorEvent)
            {
                // Failed to start audio playback
                if (log.isInfoEnabled())
                {
                    log.info("Failed to present audio:<" + event + ">");
                }
                dispose();
                this.m_alertCallback.presentationFailed();
            }
            else if (event instanceof NormalMediaPresentationEvent)
            {
                // Presenting normal content.
                if (log.isInfoEnabled())
                {
                    log.info("Audio playback started");
                }
                this.m_alertCallback.presentationStarted();
            }
            else if (event instanceof StopEvent)
            {
                // Presentation stopped
                if (log.isInfoEnabled())
                {
                    log.info("Audio playback stopped:<" + event + ">");
                }
                dispose();
                this.m_alertCallback.presentationTerminated();
            }
            else
            {
                // Intentionally do nothing -- ignore all other events.
            }
        }

        /**
         * Returns the duration, in milliseconds, of the audio override track or
         * the presentation time from the alert message, whichever is greater.
         * 
         * @param presentationTime
         *            the presentation time, in milliseconds, from the alert
         *            message (i.e. <code>lert_message_time_remaining</code>)
         * @return the maximum time, in milliseconds, in which to present the
         *         message
         */
        public long getDuration(final long presentationTime)
        {
            Time duration = this.m_player.getDuration();
            return (Duration.DURATION_UNKNOWN != duration) ? Math.max((long) (duration.getSeconds() * 1000L),
                    presentationTime) : presentationTime;
        }

        /**
         * Starts presenting the audio content (asynchronous call).
         */
        public void start()
        {
            this.m_player.start();
        }

        /**
         * Stops presenting the audio content (asynchronous call).
         */
        public void stop()
        {
            this.m_player.stop();
        }

        /**
         * Cleans up player resource after presentation terminated or failed.
         */
        private void dispose()
        {
            this.m_player.removeControllerListener(this);
            this.m_player.stop();
            this.m_player.deallocate();
            this.m_player.close();
            this.m_player = null;
        }
    }

    // Class Fields

    // Class Fields

    // Class Fields

    private static final Logger log = Logger.getLogger(EASAlertTextAudio.class.getName());

    // Instance Fields

    private EASPlayer m_audioPlayer;

    private Service m_audioService;

    private ServiceDomain m_serviceDomain;

    private int m_tasksToStop;

    private EASTuner m_tuner;

    // TODO: the use of these flags scream out for the strategy design pattern
    // (future refactoring)
    private boolean m_usingAudioFile;

    private boolean m_usingAudioSourceId;

    private boolean m_usingPrivateDescriptor;

    // Constructors

    /**
     * Constructs a new instance of the text+audio alert strategy with the given
     * parameters.
     * 
     * @param state
     *            a {@link EASState} reference for method callbacks
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     */
    public EASAlertTextAudio(EASState state, EASMessage message)
    {
        super(state, message);
        this.m_tuner = new EASTuner(this);
    }

    /**
     * Returns the time, in milliseconds, that the alert should be presented to
     * the user. The presentation time is the greater of the audio track
     * duration and the <code>alert_message_time_remaining</code> field in the
     * alert message.
     * 
     * @return the presentation of the alert, in milliseconds, or 0 if the
     *         presentation time is indefinite.
     */
    public long getPresentationTime()
    {
        long presentationTime = super.getPresentationTime();

        if (0 != presentationTime && null != this.m_audioPlayer)
        {
            presentationTime = this.m_audioPlayer.getDuration(presentationTime);
        }

        return presentationTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#getReason()
     */
    public int getReason()
    {
        if (m_usingAudioSourceId)
        {
            return EASEvent.EAS_DETAILS_CHANNEL;
        }
        else
        {
            return EASEvent.EAS_TEXT_DISPLAY;
        }
    }



    /**
     * Call back method from {@link EASTuner} indicating that the presentation
     * failed.
     */
    public void presentationFailed()
    {
        EASState.s_easManagerContext.getCurrentState().retryAlert();
    }

    /**
     * Call back method indicating that the presentation has terminated. This is
     * a synchronization point for separate threads that are terminating the
     * audio presentation and text scrolling -- we don't want to change states
     * until all termination tasks are done.
     */
    public synchronized void presentationTerminated()
    {
        if ((--this.m_tasksToStop) <= 0)
        {
            // Detach service domain (carousel) if used.
            if (null != this.m_serviceDomain)
            {
                try
                {
                    this.m_serviceDomain.detach();
                }
                catch (NotLoadedException e)
                {
                    // Intentionally do nothing -- don't care if it's already
                    // unloaded.
                }
                finally
                {
                    this.m_serviceDomain = null;
                }
            }

            // Clean up resources used.
            this.m_audioPlayer = null;
            this.m_audioService = null;
            this.m_tuner = null;

            // Complete alert processing.
            super.presentationTerminated();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#processAlert()
     */
    public void processAlert()
    {
        if (log.isInfoEnabled())
        {
            log.info("Processing text+audio alert...");
        }
        if (handledPrivateDescriptor())
        {
            // EASHandler started audio presentation from private descriptor, no
            // need to warn EASListeners.
            // Start text presentation, no need to sync text with audio since
            // handler already started presentation.
            this.m_usingPrivateDescriptor = true;
            super.presentationStarted();
        }
        else if (handledAudioFileSources())
        {
            // Warn registered listeners that EAS is about to acquire resources.
            super.m_easState.warnEASListeners(getReason());

            // Start audio presentation (asynchronously), text scroll starts on
            // callback.
            this.m_audioPlayer.start();
            this.m_usingAudioFile = true;
        }
        else if (handledAudioSourceID())
        {
            // EAS force tuned to an audio OOB source ID with the side-effect of
            // stopping non-abstract services.
            // Intentionally do nothing -- asynchronous force tune to audio
            // service in-progress, text scroll starts on callback.
            this.m_usingAudioSourceId = true;
        }
        else
        {
            // None of the audio sources panned out -- try next alert strategy.
            presentationFailed();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.ocap.manager.eas.EASAlert#startPresentation()
     */
    public void startPresentation()
    {
        // Start scrolling alert text across the screen. Although override not
        // technically needed, it shows intent.
        super.startPresentation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASAlert#stopPresentation(boolean)
     */
    public void stopPresentation(final boolean force)
    {
        if (log.isInfoEnabled())
        {
            log.info("stopPresentation - force: " + force);
        }

        // Stop audio play back of private descriptor if current audio strategy.
        stopPrivateDescriptor();

        // Stop audio play back of audio file if current audio strategy.
        stopAudioFile();

        // Stop audio play back of OOB source ID if current audio strategy.
        stopAudioSourceID();

        // Stop scrolling alert text and complete alert processing (must be last
        // due to state change).
        super.stopPresentation(force);
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        return "EASAlertTextAudio";
    }

    /**
     * Iterates through the list of descriptors in the EAS message looking for
     * an audio file descriptor. When an audio file descriptor is detected, it
     * is processed to determine if it contains an accessible audio file with a
     * supported audio format. Any failure to process a given audio file source
     * causes that source to be skipped and the next audio source to be
     * processed.
     * <p>
     * <b>Note</b>: this method is being executed as part of a
     * <code>Runnable</code> block on the <code>SystemContext</code> task queue
     * (see {@link EASStateReceived#receiveAlert(EASMessage)}) -- the blocking
     * calls in this method should not impede processing within the rest of the
     * stack.
     * <p>
     * <b>Note</b>: the assumption is that JMF will override the current audio
     * playback with the EAS audio file contents and not mix the two audio
     * sources together into a single unintelligible audio output.
     * 
     * @return <code>true</code> if an audio file descriptor was found with an
     *         accessible audio file for play back; otherwise <code>false</code>
     */
	// Added for findbugs issues fix
	// Added synchronization keyword
    protected synchronized boolean handledAudioFileSources()
    {
        boolean handled = false;
        ServiceDomain serviceDomain = new ServiceDomain();
        Iterator iter = super.m_easMessage.getAudioFileSources().iterator();

        while (!handled && iter.hasNext())
        {
            EASAudioFileSource entry = (EASAudioFileSource) iter.next();
            int audioSource = entry.getAudioSource();
            switch (audioSource)
            {
                case EASAudioFileSource.OOB_DSMCC_OBJECT_CAROUSEL:
                {
                    try
                    {
                        // Attach object carousel (blocking call to attach
                        // service domain).
                        EASAudioFileObjectCarouselSource source = (EASAudioFileObjectCarouselSource) entry;
                        OcapLocator locator = new OcapLocator(-1, source.getProgramNumber(), -1);
                        serviceDomain.attach(locator, source.getCarouselId());

                        // Access and load audio file from carousel (blocking
                        // call to load file).
                        DSMCCObject dsmccObject = new DSMCCObject(serviceDomain.getMountPoint(), source.getFileName());
                        dsmccObject.synchronousLoad();
                        URL url = dsmccObject.getURL();

                        // Create JMF player for audio file.
                        this.m_audioPlayer = new EASPlayer(this, url);
                        this.m_serviceDomain = serviceDomain;
                        handled = true;
                    }
                    catch (InvalidLocatorException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Disregarding audio source - " + e.getMessage());
                        }
                    }
                    catch (InterruptedIOException e)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("Disregarding audio source, attachment or load aborted by another thread");
                        }
                    }
                    catch (InvalidPathNameException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Disregarding audio source - audio file not found:<" + entry.getFileName() + ">");
                        }
                    }
                    catch (NoPlayerException e)
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("Disregarding audio source, no player available - " + e.getMessage());
                        }
                    }
                    catch (IOException e)
                    {
                        SystemEventUtil.logRecoverableError("Disregarding audio source", e);
                    }

                    continue;
                }
                case EASAudioFileSource.OOB_DSMCC_DATA_CAROUSEL:
                default:
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Disregarding audio source - unsupported source type:<0x"
                                        + Integer.toHexString(audioSource) + ">");
                    }
                    continue;
                }
            }
        }

        // Clean up any loose ends if unable to use an audio file source.
        if (!handled)
        {
            try
            {
                serviceDomain.detach();
            }
            catch (NotLoadedException e)
            {
                // Intentionally do nothing -- don't care if it's already
                // unloaded.
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("handledAudioFileSources returning: " + handled);
        }

        return handled;
    }

    /**
     * Forcibly tunes to the given non-zero OOB audio source ID. Tuning occurs
     * asynchronously so text scrolling is started on successful callback.
     * 
     * @return <code>true</code> if a force tune to the audio-only service is in
     *         progress; otherwise <code>false</code>
     */
    protected boolean handledAudioSourceID()
    {
        int audioSourceID = super.m_easMessage.getAudioOOBSourceID();
        boolean handled = false;
        Locator locator = null;

        if (0 != audioSourceID)
        {
            try
            {
                locator = new OcapLocator(audioSourceID);
                this.m_audioService = this.m_siManager.getService(locator);

                // Warn registered listeners that EAS is about to acquire
                // resources.
                super.m_easState.warnEASListeners(getReason());

                // Select and present the service (potential asynchronous call).
                if (this.m_tuner.select(this.m_audioService))
                {
                    // Details channel already tuned and presenting.
                    presentationStarted();
                }

                handled = true;
            }
            catch (InvalidLocatorException e)
            {
                SystemEventUtil.logRecoverableError("Invalid audio source ID:<" + audioSourceID + ">", e);
            }
            catch (javax.tv.locator.InvalidLocatorException e)
            {
                SystemEventUtil.logRecoverableError("Failed to lookup audio service for locator:<" + locator + ">", e);
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("handledAudioSourceID returning: " + handled);
        }

        return handled;
    }

    /**
     * Notifies the registered {@link EASHandler} of any private descriptors in
     * this EAS message. The handler returns <code>true</code> if was able to
     * process a private descriptor and start presenting audio. In this case, no
     * warning is issued to registered {@link EASListener} implementations since
     * EAS did not take away resources.
     * 
     * @return <code>true</code> if the registered handler started playing back
     *         the audio of one of the private descriptors; otherwise
     *         <code>false</code>
     */
    protected boolean handledPrivateDescriptor()
    {
        boolean handled = false;
        synchronized (EASState.s_handlerMutexLock)
        {
            if (null != EASState.s_currentHandler)
            { // TODO: do we always do private descriptor or only when
              // alert_priority > 11
                Iterator iter = super.m_easMessage.getDescriptors().iterator();
                while (!handled && iter.hasNext())
                {
                    EASDescriptor entry = (EASDescriptor) iter.next();
                    if (entry.isPrivate())
                    {
                        handled = EASState.s_currentHandler.notifyPrivateDescriptor(entry.getDescriptor());
                    }
                }
            }
        }
        if (log.isInfoEnabled())
        {
            log.info("handledPrivateDescriptor returning: " + handled);
        }

        return handled;
    }

    /**
     * Stops the audio override file presentation (asynchronous call). Need to
     * synchronize stopping of audio source and text scrolling tasks.
     */
    private void stopAudioFile()
    {
        if (this.m_usingAudioFile)
        {
            this.m_tasksToStop = 2; // need to synchronize on two tasks to stop
                                    // (audio playback and text scrolling)
            this.m_audioPlayer.stop();
        }
    }

    /**
     * Stops the audio override service presentation (asynchronous call). Need
     * to synchronize stopping of audio source and text scrolling tasks.
     */
    private void stopAudioSourceID()
    {
        if (this.m_usingAudioSourceId)
        {
            this.m_tasksToStop = 2; // need to synchronize on two tasks to stop
                                    // (service presentation and text scrolling)
            this.m_tuner.stop();
        }
    }

    /**
     * Notifies the registered {@link EASHandler} to stop playing back the audio
     * acquired via a private descriptor. Need to synchronize stopping of the
     * text scrolling task only.
     */
    private void stopPrivateDescriptor()
    {
        if (this.m_usingPrivateDescriptor)
        {
            synchronized (EASState.s_handlerMutexLock)
            {
                this.m_tasksToStop = 1; // need to synchronize on one task to
                                        // stop (text scrolling)
                EASState.s_currentHandler.stopAudio();
            }
        }
    }
}
