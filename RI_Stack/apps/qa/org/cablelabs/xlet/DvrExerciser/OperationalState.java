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

package org.cablelabs.xlet.DvrExerciser;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.media.RateChangeEvent;
import javax.media.Time;
import javax.tv.service.Service;

import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.media.BeginningOfContentEvent;
import org.ocap.shared.media.EndOfContentEvent;

//////////////////////////////////////////////////////////////////////////////
//Operational state implementations
//
//The following are used to managed the operational state of the application.
//The states are:
//1. Stopped
//2. Live
//3. Playback
// Note that it is expected that only one of the states will be 'active'
//  at any instant in time.
//
//Each state has a name and behavior that is expressed by invoking methods
//that are declared on the base class.  These states are designed to
//process command messages as specified by the user.
//
// 
//It is expected that extending classes will override these methods as 
//required.
//////////////////////////////////////////////////////////////////////////////

/**
 * Base operational state. This base class declares common behaviors for all
 * operational states.
 */
public class OperationalState implements ControllerListener
{
    protected DvrExerciser m_dvrExerciser = null;

    protected String m_strName;

    protected boolean m_playingBack;

    protected boolean m_bStarted = false;

    // operational states of the application
    private static OperationalState m_currentOperationalState = null;

    private static LiveOperationalState m_liveOperationalState = null;

    private static StoppedOperationalState m_stoppedOperationalState = null;

    private static PlaybackOperationalState m_playbackOperationalState = null;
    
    private static DeleteOperationalState m_deleteOperationalState = null;

    private static HNPlaybackOperationalState m_hnPlaybackOperationalState = null;

    /**
     * Instantiates and initializes the operational states for usage. This
     * method must be called before any of the operational states can be used.
     * 
     * @param dvrExerciser
     * @param liveContent
     */
    public static void initializeOperationalStates(DvrExerciser dvrExerciser, LiveContent liveContent)
    {
        // create operational states
        m_liveOperationalState = new LiveOperationalState(dvrExerciser, liveContent);
        m_stoppedOperationalState = new StoppedOperationalState();
        m_playbackOperationalState = new PlaybackOperationalState(dvrExerciser);
        m_deleteOperationalState = new DeleteOperationalState(dvrExerciser);
        m_hnPlaybackOperationalState = new HNPlaybackOperationalState(dvrExerciser);

        // set the initial operational to 'stopped'
        m_currentOperationalState = m_stoppedOperationalState;
    }

    /**
     * Accessor method for the 'current' operational state.
     * 
     * @return the current operational state.
     */
    public static OperationalState getCurrentOperationalState()
    {
        return m_currentOperationalState;
    }

    /**
     * Sets the 'live' operational state as current.
     * 
     * @return the live operational state.
     */
    public static LiveOperationalState setLiveOperationalState()
    {
        if (m_currentOperationalState != m_liveOperationalState)
        {
            // ...stop it and...
            m_currentOperationalState.stop();

            // ...transition to the live operational state and finally...
            m_currentOperationalState = m_liveOperationalState;

            // ...start it up
            m_currentOperationalState.start();
        }

        return (LiveOperationalState) m_currentOperationalState;
    }

    /**
     * Sets the 'playback' operational state as current.
     * 
     * @return the playback operational state.
     */
    public static PlaybackOperationalState setPlaybackOperationalState()
    {
        if (m_currentOperationalState != m_playbackOperationalState)
        {
            // ...stop it and...
            m_currentOperationalState.stop();

            // ...transition to the live operational state and finally...
            m_currentOperationalState = m_playbackOperationalState;

            // ...start it up
            m_currentOperationalState.start();
        }
        return (PlaybackOperationalState) m_currentOperationalState;
    }

    public static DeleteOperationalState setDeleteOperationalState()
    {
        if (m_currentOperationalState != m_deleteOperationalState)
        {
            // ...stop it and...
            //m_currentOperationalState.stop();

            // ...transition to the live operational state and finally...
            m_currentOperationalState = m_deleteOperationalState;

            // ...start it up
            m_currentOperationalState.start();
        }
        return (DeleteOperationalState) m_currentOperationalState;
    }
    
    public static HNPlaybackOperationalState setHNPlaybackOperationalState()
    {
        if (m_currentOperationalState != m_hnPlaybackOperationalState)
        {
            // ...stop it and...
            m_currentOperationalState.stop();

            // ...transition to the live operational state and finally...
            m_currentOperationalState = m_hnPlaybackOperationalState;

            // ...start it up
            m_currentOperationalState.start();
        }
        return (HNPlaybackOperationalState) m_currentOperationalState;
    }

    /**
     * Sets the 'stopped' operational state as current.
     * 
     * @return the stopped operational state.
     */
    public static StoppedOperationalState setStoppedOperationalState()
    {
        if (m_currentOperationalState != m_stoppedOperationalState)
        {
            // ...stop it and...
            m_currentOperationalState.stop();

            // ...transition to the stopped operational state and finally...
            m_currentOperationalState = m_stoppedOperationalState;

            // ...start it up
            m_currentOperationalState.start();
        }
        return (StoppedOperationalState) m_currentOperationalState;
    }

    /**
     * Constructor Internalizes the global DvrExerciser reference.
     * 
     * @param dvrExerciser
     */
    protected OperationalState(DvrExerciser dvrExerciser)
    {
        // internalize global DvrExerciser reference
        m_dvrExerciser = dvrExerciser;

        // set the state name to something other than null
        m_strName = "Base";
    }

    /**
     * Accessor method to obtain the name of this state.
     * 
     * @return the name of this state.
     */
    public String getName()
    {
        return m_strName;
    }

    /**
     * Polymorphic function intended to be overridden by inheriting operational
     * states as needed. This base function does nothing.
     */
    public void channelUp()
    {
    }

    /**
     * Polymorphic function intended to be overridden by inheriting operational
     * states as needed. This base function does nothing.
     */
    public void channelDown()
    {
    }

    /**
     * Sets the play rate to 1.0.
     */
    public void play()
    {
        // ...update its play rate rate to 1.0
        DvrTest.getInstance().setPlaybackRate((float) 1.0);
    }

    /*
     * Starts the operational state. This is a polymorphic method that can be
     * overridden by extending classes. In this base class, it does nothing.
     */
    public void start()
    {
        m_bStarted = true;
    }

    /**
     * Stops this operational state. Stops the service context so that it no
     * longer presents content.
     * 
     */
    public void stop()
    {
        m_bStarted = false;
    }

    /**
     * Stops this operational state. Stops the service context so that it no
     * longer presents content.
     * 
     */
    public void delete()
    {
        m_bStarted = false;
    }
    
    /**
     * Increases the play rate by 1 step.
     * 
     */
    public void fastForward()
    {
        float rate;

        // get the current playback rate
        rate = DvrTest.getInstance().getPlaybackRate();

        // now get the next fastest playback rate
        rate = DvrTest.getInstance().getNextPlayRate(rate);

        // skip over 0.0 (paused)
        if (0.0 == rate)
        {
            rate = DvrTest.getInstance().getNextPlayRate(rate);
        }
        // set the faster playback rate
        DvrTest.getInstance().setPlaybackRate(rate);
    }

    /**
     * Decreases the play rate by 1 step.
     * 
     */
    public void rewind()
    {
        float rate;

        // get the current playback rate
        rate = DvrTest.getInstance().getPlaybackRate();

        // now get the next fastest rewind rate
        rate = DvrTest.getInstance().getPreviousPlayRate(rate);

        // skip over 0.0 (paused)
        if (0.0 == rate)
        {
            rate = DvrTest.getInstance().getPreviousPlayRate(rate);
        }

        // set the faster rewind rate
        DvrTest.getInstance().setPlaybackRate(rate);
    }

    /**
     * Sets the play rate to 0.0.
     * 
     */
    public void pause()
    {
        DvrTest.getInstance().setPlaybackRate((float) 0.0);
    }

    /**
     * Accessor method for the current play rate.
     * 
     * @return the current play rate.
     */
    public float getPlayRate()
    {
        return DvrTest.getInstance().getPlaybackRate();
    }

    /**
     * Jumps forward or backward the specified number of seconds.
     * 
     * If step > 0, the jump will be forward
     * 
     * If step <0, the jump will be backward
     * 
     * @param step
     */
    public void jumpPosition(int step)
    {
        Time currentTime = DvrTest.getInstance().getCurrentPosition();
        if (null != currentTime)
        {
            Time nextTime = new Time(currentTime.getSeconds() + (double) step);
            DvrTest.getInstance().setPlaybackPosition(nextTime);
        }
    }

    /**
     * ControllerListener implementation The purpose of this method is primarily
     * to report any ControllerEvents to the user. However, if a
     * ControllerClosedEvent is received, the flag used to indicate that a
     * controller event handler is registered will be updated to indicate that
     * no controller event handler is registered.
     */
    public void controllerUpdate(ControllerEvent event)
    {
        String eventInfo = "";
        float newRate = Float.NaN;

        // get the event class name (w/o package name) for reporting
        String className = event.getClass().getName();
        eventInfo = className.substring(event.getClass().getPackage().getName().length() + 1);

        // if this is a rate change event...
        if (event instanceof RateChangeEvent)
        {
            // ...display the new rate
            newRate = ((RateChangeEvent) event).getRate();
            eventInfo += ", newRate = " + newRate;
            m_playingBack = (Math.abs(newRate) > 0.0001F);
        }

        if (DvrExerciser.getInstance().isDvrEnabled())
        {
            if (event instanceof EndOfContentEvent)
            {
                newRate = ((EndOfContentEvent) event).getRate();
            }

            if (event instanceof BeginningOfContentEvent)
            {
                newRate = ((BeginningOfContentEvent) event).getRate();
            }
        }

        // display the event information
        m_dvrExerciser.logIt("Received ControllerEvent: " + eventInfo);
    }
}

/**
 * Handles the 'live' operational state.
 * 
 */
class LiveOperationalState extends OperationalState
{
    private LiveContent m_liveContent;

    /**
     * Constructor
     * 
     * @param dvrExerciser
     *            'parent' global DvrExerciser application instance.
     * @param liveContent
     *            entity that maintains a list of 'live' services available for
     *            presentation - essentially a channel map.
     */
    public LiveOperationalState(DvrExerciser dvrExerciser, LiveContent liveContent)
    {
        super(dvrExerciser);
        m_liveContent = liveContent;

        m_strName = "Live";
    }

    /**
     * Starting the 'live' operational state means tuning to the 'current'
     * service.
     */
    public void start()
    {
        selectService(m_liveContent.getCurrentService());
        super.start();
    }

    /**
     * Stops this operational state. Stops the service context so that it no
     * longer presents content.
     * 
     */
    public void stop()
    {
        if (true == m_bStarted)
        {
            DvrTest.getInstance().doStop();
        }
        super.stop();
    }

    /**
     * Selects the 'next' service (as known by the LiveContent entity) into the
     * service context, which starts it playing.
     * 
     */
    public void channelUp()
    {
        selectService(m_liveContent.getNextService());
    }

    /**
     * Selects the 'previous' service (as known by the LiveContent entity) into
     * the service context, which starts it playing.
     * 
     */
    public void channelDown()
    {
        selectService(m_liveContent.getPreviousService());
    }

    /**
     * Selects the specified service into the service context for presentation.
     * The function also adds this instance as a listener for controller events.
     * 
     * @param service
     *            the Service to be selected.
     */
    private void selectService(Service service)
    {
        Player player;

        if (null != service)
        {
            if (DvrExerciser.getInstance().isDvrEnabled())
            {
                DvrTest.getInstance().doTuneLive(service);

            }
            else
            {
                DvrExerciser.getInstance().getNonDvrTest().doTuneLive(service);
            }

            if (DvrExerciser.getInstance().isDvrEnabled())
            {
                player = DvrTest.getInstance().getServiceContextPlayer();
            }
            else
            {
                player = DvrExerciser.getInstance().getNonDvrTest().getServiceContextPlayer();
            }
            if (null != player)
            {
                player.addControllerListener(this);
            }
        }
    }
}

/**
 * Handles the 'playback' operational state. This class is used to playback
 * recording requests.
 * 
 */
class PlaybackOperationalState extends OperationalState implements Runnable
{
    private long m_durationNs;

    private Player m_player;

    private boolean m_bUpdatePlaybackIndicator;

    public PlaybackOperationalState(DvrExerciser dvrExerciser)
    {
        super(dvrExerciser);
        m_strName = "Playback";
    }

    /**
     * 
     */
    public void start()
    {
        m_dvrExerciser.getBargraph().setVisible(true);
        super.start();
    }

    /**
     * Stops the service context so that it no longer presents content. This
     * method will make the progress bargraph invisible.
     */
    public void stop()
    {
        m_dvrExerciser.getBargraph().setVisible(false);
        m_bUpdatePlaybackIndicator = false;

        if (true == m_bStarted)
        {
            DvrTest.getInstance().doStop();
        }

        super.stop();
    }
    
    /**
     * Set the recording request to be played and select it into the service
     * context for presentation.
     * 
     * @param orr
     *            the recording request to be played.
     */
    public void setRecordingRequest(OcapRecordingRequest orr)
    {
        // get the recording duration from the recording request (in msec),
        // and convert it to nanoseconds
        m_durationNs = DvrTest.getInstance().getRecordingDuration(orr) * 1000000;

        // print out the duration of the recording spec
        m_dvrExerciser.logIt("Recording duration = " + m_durationNs + " ns");

        // start up the playback indicator polling thread
        m_bUpdatePlaybackIndicator = true;
        new Thread(this).start();
        DvrTest.getInstance().doPlaybackServiceSelection(orr);

        if (m_player != null)
        {
            m_player.removeControllerListener(this);
        }

        // ... add this as a listener.
        Player player = DvrTest.getInstance().getServiceContextPlayer();

        if (null != player)
        {
            m_player = player;
            m_player.addControllerListener(this);
        }
        else
        {
            m_dvrExerciser.logIt("SetRecordingRequest - unable to retrieve player for ServiceContext");
        }
    }

    public void run()
    {
        // if we are still playing...
        while (m_bUpdatePlaybackIndicator)
        {
            try
            {
                // ...calculate the completion ratio
                // may never have received a player - avoid an NPE in that case
                if (m_player != null && m_playingBack)
                {
                    float completionRatio = ((float) (m_player.getMediaTime().getNanoseconds())) / (float) m_durationNs;
                    m_dvrExerciser.getBargraph().setCompletionRatio(completionRatio);
                }

                // wait for 1 second before the next update
                Thread.sleep(1000);
            }
            catch (InterruptedException ex)
            {
                ex.printStackTrace();
            }
        }
    }
}

class DeleteOperationalState extends OperationalState implements Runnable
{
    OcapRecordingRequest recordingRequest = null;

    public DeleteOperationalState(DvrExerciser dvrExerciser)
    {
        super(dvrExerciser);
        m_strName = "Delete";
    }

    /**
     * 
     */
    public void start()
    {
        m_dvrExerciser.getBargraph().setVisible(true);
        super.start();
    }

    /**
     * Stops the service context so that it no longer presents content. This
     * method will make the progress bargraph invisible.
     */
    public void stop()
    {
        m_dvrExerciser.getBargraph().setVisible(false);

        super.stop();
    }

    /**
     * Set the recording request to be played and select it into the service
     * context for presentation.
     * 
     * @param orr
     *            the recording request to be played.
     */
    public void setRecordingRequest(OcapRecordingRequest orr)
    {
        // get the recording duration from the recording request (in msec),
        // and convert it to nanoseconds
        recordingRequest = orr;
        System.out.println("recordingRequest: " + orr);
        new Thread(this).start();
    }

    public void run()
    {
        // delete it
        System.out.println("deleting recordingRequest...");
        try {
            recordingRequest.delete();
        } catch (AccessDeniedException e) {

        }
    }
}

/**
 * Handles the 'playback' operational state. This class is used to playback
 * recording requests.
 * 
 */
class HNPlaybackOperationalState extends OperationalState
{
    public HNPlaybackOperationalState(DvrExerciser dvrExerciser)
    {
        super(dvrExerciser);
        m_strName = "HN Playback";
    }

    public void start()
    {
         super.start();
    }

    public void stop()
    {
        super.stop();
    }
}


/**
 * Handles the 'stopped' state. This class overrides a number of base class
 * methods so that this state essentially does nothing.
 * 
 */
class StoppedOperationalState extends OperationalState
{
    StoppedOperationalState()
    {
        super(null);
        m_strName = "Stopped";
    }

    /**
     * Overrides base class function to do nothing.
     */
    public void play()
    {
    }

    /**
     * Overrides base class function to do nothing.
     */
    public void fastForward()
    {
    }

    /**
     * Overrides base class function to do nothing.
     */
    public void rewind()
    {
    }

    /**
     * Overrides base class function to do nothing.
     */
    public void pause()
    {
    }
}
