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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.media.Time;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.davic.mpeg.ElementaryStream;
import org.dvb.media.VideoTransformation;
import org.dvb.service.selection.DvbServiceContext;
import org.havi.ui.HListElement;
import org.havi.ui.HListGroup;
import org.havi.ui.HScreenPoint;
import org.havi.ui.HVisible;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingPlaybackListener;
import org.ocap.dvr.TimeShiftEvent;
import org.ocap.dvr.TimeShiftListener;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.hardware.CopyControl;
import org.ocap.media.AlternativeMediaPresentationReason;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaAccessConditionControl;
import org.ocap.media.MediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.shared.media.TimeShiftControl;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;
import org.ocap.ui.event.OCRcEvent;

/**
 * This is a 'workbench' class that can be used to exercise various kinds of DVR
 * functionality.
 * 
 * It was designed to work in concert with an xlet application that that can
 * instantiate it and
 * 
 * @author andy
 * 
 */
public class DvrTest extends NonDvrTest implements RecordingPlaybackListener, TimeShiftListener,
        ControllerListener
{
    // number of times to loop, presenting 'live' and 'recorded' content
    static final int REPEAT_COUNT = 1;

    // 'true' to present the first existing recording, 'false' to present a new
    // recording
    static final boolean USE_EXISTING = true;

    // 'true' to play from TSB, 'false' to play from disk recording
    static final boolean PLAYBACK_FROM_TSB = false;

    // 'true' to delete existing recordings, 'false' to leave existing
    // recordings
    static final boolean DELETE_EXISTING_RECORDINGS = false;

    // minimum and maximum TSB duration (in sec)
    static final int MIN_TSB_DURATION = 3000;

    static final int MAX_TSB_DURATION = 10000;

    // length of recording to make (in seconds)
    static final int SHORT_RECORDING_LENGTH = 10;

    static final int TEST_RECORDING_LENGTH = 20;

    static final int MEDIUM_RECORDING_LENGTH = 30;

    static final int LONG_RECORDING_LENGTH = 60 * 5;

    // amount of time to playback the recording (in seconds)
    static final int PLAYBACK_TIME = 15;

    static final SimpleDateFormat m_shortDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ssZ");
    
    private int m_recordingState = 0;// NO_RECORDING;

    private OcapRecordingManager m_recordingManager;

    private TVTimerSpec m_tsbLoggingTimerSpec;

    private TVTimer m_tsbLoggingTimer;

    private boolean m_tsbLoggingEnabled;

    /**
     * The 'current' recording request - this will be non-null if a recording is
     * made during an execution of this program.
     */
    private OcapRecordingRequest m_recordingRequestCurrent;

    /**
     * The 'current' buffering request - this will be non-null if a buffering request is
     * started during an execution of this program.
     */
    private Map m_bufferingRequests = new HashMap();
    
    /**
     * OcapRecordingManager bufferingEnable/Disable flag (buffering is enabled at startup)
     */
    private boolean m_bufferingEnabled = true;

    // this is a list of permissible playback rates - pressing the '<<' or '>>'
    // keys on the remote control will
    float m_playRates[] = new float[] { (float) -64.0, (float) -32.0, (float) -16.0, (float) -8.0, (float) -4.0,
            (float) -2.0, (float) -1.0, (float) 0.0, (float) 0.5, (float) 1.0, (float) 2.0, (float) 4.0, (float) 8.0,
            (float) 16.0, (float) 32.0, (float) 64.0, };

    // number of bytes to use for disk free space test
    public static final long DISK_MIN = 616038400;

    private TVTimerWentOffListener m_tsbLoggingTimerWentOffListener;

    //media access authorization - default to full authorization
    private boolean isFullAuth = true;

    private RecordingSelector m_recordingSelector = null;

    private int m_customRecordingDuration;

    private int m_customRecordingDelta;
    
    private static DvrTest m_instance = null;

    public static DvrTest getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new DvrTest();
        }
        return m_instance;
    }
   
    /**
     * Constructor
     * 
     * Just initializes an instance of the class for use
     * 
     * @param dvrExerciser
     */
    private DvrTest()
    {
        super();

        // get the recording manager
        m_recordingManager = (OcapRecordingManager) OcapRecordingManager.getInstance();

        m_recordingManager.addRecordingPlaybackListener(this);

        m_tsbLoggingTimerWentOffListener = new TSBLoggingTimerWentOffListener();
        m_tsbLoggingTimer = TVTimer.getTimer();
        m_tsbLoggingTimerSpec = new TVTimerSpec();
        m_tsbLoggingTimerSpec.setAbsolute(false); // Always a delay
        m_tsbLoggingTimerSpec.setTime(1000); // Always the same delay
        m_tsbLoggingTimerSpec.setRepeat(true); // Only once
        m_customRecordingDuration = 30;
        m_customRecordingDelta = 0;
    }

    /**
     * Obtains a single service context for use by the application, and
     * associates it with a ScaledVideoManager that places the video in the
     * upper-left quadrant of the screen.
     * 
     * @return <code>true</code> if the service context and scaled video manager
     *         are instantiated and set up without error, <code>false
     * </code> otherwise.
     */
    public boolean init(DvrExerciser dvrExerciser)
    {
        m_dvrExerciser = dvrExerciser;

        boolean bRetVal = true;
        m_dvrExerciser.logIt("initTest():entry");
        ServiceContextFactory scf = ServiceContextFactory.getInstance();
        try
        {
            // create the service context for general use
            m_serviceContext = (DvbServiceContext) scf.createServiceContext();

            m_serviceContext.addListener(this);
            MediaAccessHandlerRegistrar.getInstance().registerMediaAccessHandler(new MediaAccessHandler()
            {
                public MediaAccessAuthorization checkMediaAccessAuthorization(Player p, OcapLocator sourceURL, boolean isSourceDigital,
                                                                              final ElementaryStream[] esList, MediaPresentationEvaluationTrigger evaluationTrigger)
                {
                    return new MediaAccessAuthorization()
                    {
                        public boolean isFullAuthorization()
                        {
                            return isFullAuth;
                        }

                        public Enumeration getDeniedElementaryStreams()
                        {
                            if (isFullAuth)
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
                            if (isFullAuth)
                            {
                                return 0;
                            }
                            return AlternativeMediaPresentationReason.RATING_PROBLEM;
                        }
                    };
                }
            });

            // set default video window to quarter screen in upper left corner
            VideoTransformation videoTransform = new VideoTransformation(null, (float) 0.5, (float) 0.5,
                    new HScreenPoint(0, 0));
            m_serviceContext.setDefaultVideoTransformation(videoTransform);

            if (m_serviceContext instanceof TimeShiftProperties)
            {
                // Duration is in seconds
                ((TimeShiftProperties) m_serviceContext).setMinimumDuration(MIN_TSB_DURATION);
                ((TimeShiftProperties) m_serviceContext).setMaximumDuration(MAX_TSB_DURATION);
            }
        }
        catch (Exception e)
        {
            bRetVal = false;
            m_dvrExerciser.logIt("DvrTest: unable to create service context, exception: " + e.toString());
            e.printStackTrace();
        }
        m_dvrExerciser.logIt("initTest():exit");
        return bRetVal;
    }

    /**
     * Toggle authorization and trigger an authorization check
     * @param toggleAuthorizationFirst if true, toggle authorization before running authorization check
     */
    public void runAuthorization(boolean toggleAuthorizationFirst)
    {
        if (toggleAuthorizationFirst)
        {
            isFullAuth = !isFullAuth;
        }
        m_dvrExerciser.logIt("Triggering authorization check with full authorization set to: " + isFullAuth);
        Player player = getServiceContextPlayer();
        if (null != player)
        {
            MediaAccessConditionControl control = (MediaAccessConditionControl)player.getControl("org.ocap.media.MediaAccessConditionControl");
            if (control != null)
            {
                control.conditionHasChanged(MediaPresentationEvaluationTrigger.USER_RATING_CHANGED);
            }
            else
            {
                m_dvrExerciser.logIt("No MediaAccessConditionControl found");
            }
        }
        else
        {
            m_dvrExerciser.logIt("Service context player not found");
        }
    }

    public void skipForward(int seconds)
    {
        Player player = getServiceContextPlayer();
        Time newMediaTime = new Time(player.getMediaTime().getSeconds() + seconds);
        m_dvrExerciser.logIt("Skipping " + seconds + " forward to: " + newMediaTime);
        player.setMediaTime(newMediaTime);
    }

    public void skipBackward(int seconds)
    {
        Player player = getServiceContextPlayer();
        Time newMediaTime = new Time(Math.max(0, player.getMediaTime().getSeconds() - seconds));
        m_dvrExerciser.logIt("Skipping " + seconds + " seconds backward to: " + newMediaTime);
        player.setMediaTime(newMediaTime);
    }
    /**
     * Mutator method to enable/disable TSB. Enables TSB by setting a non-zero
     * (MIN_TSB_DURATION) minimum duration on the service context. Disables TSB
     * by setting the minimum duration on the service context to 0.
     * 
     * 
     * @param enable
     *            <code>true</code> to enable the TSB, <code>false</code> to
     *            disable.
     */
    public void enableTsb(boolean enable)
    {
        if (true == enable)
        {
            ((TimeShiftProperties) m_serviceContext).setMinimumDuration(MIN_TSB_DURATION);
        }
        else
        {
            ((TimeShiftProperties) m_serviceContext).setMinimumDuration(0);
        }
    }

    /**
     * Mutator method to enable/disable TSB logging.
     * 
     * @param enable
     *            <code>true</code> to enable TSB logging, <code>false</code> to
     *            disable tsb logging.
     */
    public void enableTsbLogging(boolean enable)
    {
        if (enable)
        {
            // no need to hold ref, listener will remove itself when triggered
            m_tsbLoggingTimerSpec.addTVTimerWentOffListener(m_tsbLoggingTimerWentOffListener);
            try
            {
                m_tsbLoggingTimerSpec = m_tsbLoggingTimer.scheduleTimerSpec(m_tsbLoggingTimerSpec);
            }
            catch (Exception e)
            {
                m_dvrExerciser.logIt("Unable to schedule timer spec - " + e.getMessage());
            }
        }
        else
        {
            m_tsbLoggingTimerSpec.removeTVTimerWentOffListener(m_tsbLoggingTimerWentOffListener);
            m_tsbLoggingTimer.deschedule(m_tsbLoggingTimerSpec);
        }
        m_tsbLoggingEnabled = enable;
    }

    /**
     * Accessor method for the current state of the TSB.
     * 
     * Returns the current state of the TSB by requesting the minimum duration
     * on the service context. A value of '0' implies that the TSB is disabled,
     * while any other value is the minimum duration of the TSB.
     * 
     * @return <code>true</code> if the TSB is enabled, <code>false</code>
     *         otherwise.
     */
    public boolean isTsbEnabled()
    {
        boolean bRetVal;
        if (0 == ((TimeShiftProperties) m_serviceContext).getMinimumDuration())
        {
            bRetVal = false;
        }
        else
        {
            bRetVal = true;
        }
        return bRetVal;
    }

    /**
     * Accessor returning current state of TSB logging flag
     * 
     * @return true if tsb logging is enabled
     */
    public boolean isTsbLoggingEnabled()
    {
        return m_tsbLoggingEnabled;
    }

    /**
     * Gets the next highest valid play rate.
     * 
     * @param currentPlayRate
     * @return
     */
    public float getNextPlayRate(float currentPlayRate)
    {
        int index;
        float fRetVal = 1.0f; // default is 1x

        // find the index of the current play rate in the table of
        // valid play rates
        index = getPlayRateIndex(currentPlayRate);

        if (-1 != index)
        {
            index++;

            // make sure the index doesn't run past the end of the table
            if (m_playRates.length <= index)
            {
                index = m_playRates.length - 1;
            }
        }
        else
        {
            index = m_playRates.length - 1;
        }

        fRetVal = m_playRates[index];
        return fRetVal;
    }

    /**
     * Gets the next lowest valid play rate. TODO: document
     * 
     * @param currentPlayRate
     * @return
     */
    public float getPreviousPlayRate(float currentPlayRate)
    {
        int index;
        float fRetVal = 1.0f; // default is 1x

        // find the index of the current play rate in the table of
        // valid play rates
        index = getPlayRateIndex(currentPlayRate);

        if (-1 != index)
        {
            index--;

            // make sure the index doesn't run past the beginning of the table
            if (0 > index)
            {
                index = 0;
            }
        }
        else
        {
            index = 0;
        }

        fRetVal = m_playRates[index];
        return fRetVal;
    }

    /**
     * Finds the index of the current play rate in the table of valid play rates
     * 
     * If the current play rate is not in the table of valid play rates, the
     * index of the entry that represents 1.0 is returned.
     * 
     * @param currentPlayRate
     * @return
     */
    private int getPlayRateIndex(float currentPlayRate)
    {
        int i;
        int retVal = -1;

        // find current play rate in the table of valid play rates
        for (i = 0; i < m_playRates.length; i++)
        {
            if (m_playRates[i] == currentPlayRate)
            {
                retVal = i;
                break;
            }
        }

        return retVal;
    }


    // /////////////////////////////////////////////////////
    // TimeShiftListener interface implementation
    // /////////////////////////////////////////////////////
    public void receiveTimeShiftevent(TimeShiftEvent tsevent)
    {
        m_dvrExerciser.logIt("Received TimeShiftEvent: " + tsevent);
    }

    // /////////////////////////////////////////////////////
    // ControllerListener interface implementation
    // /////////////////////////////////////////////////////
    public void controllerUpdate(ControllerEvent cevent)
    {
        m_dvrExerciser.logIt("Received ControllerEvent: " + cevent);
    }

    public RecordingList getRecordings()
    {
        return m_recordingManager.getEntries();
    }

    public RecordingList getPresentableRecordings()
    {
        class CompletedRecordingFilter extends RecordingListFilter
        {
            public boolean accept(RecordingRequest entry)
            {
                if (entry instanceof LeafRecordingRequest)
                {
                    LeafRecordingRequest lrr = (LeafRecordingRequest) entry;

                    try
                    {
                        if (lrr.getService() != null)
                        {
                            return true;
                        }
                    }
                    catch (Throwable e)
                    {
                        return false;
                    }

                }

                return false;
            }
        } // END class CompletedRecordingFilter

        return m_recordingManager.getEntries(new CompletedRecordingFilter());
    }

    /**
     * Utility method to delete current recording
     */
    public boolean doDeleteCurrentRecording()
    {
        boolean bRetVal = false;
        m_dvrExerciser.logIt("doDeleteCurrentRecording():entry");
        try
        {
            if(m_recordingRequestCurrent != null)
                m_recordingRequestCurrent.delete();
            bRetVal = true;
            m_dvrExerciser.logIt("Recording deleted successfully");
        }
        catch (Exception ex)
        {
            m_dvrExerciser.logIt("Exception in doDeleteCurrentRecording: " + ex);
        }
        m_dvrExerciser.logIt("doDeleteCurrentRecording():exit");

        return bRetVal;
    }
    /**
     * Utility method to delete all recordings
     */
    public boolean doDeleteAllRecordings()
    {
        boolean bRetVal = false;
        m_dvrExerciser.logIt("doDeleteAllRecordings():entry");
        try
        {
            m_recordingManager.deleteAllRecordings();
            bRetVal = true;
            m_dvrExerciser.logIt("All recordings deleted successfully");
        }
        catch (Exception ex)
        {
            m_dvrExerciser.logIt("Exception deleting all recordings: " + ex);
        }
        m_dvrExerciser.logIt("doDeleteAllRecordings():exit");

        return bRetVal;
    }

    // /////////////////////////////////////////////////////
    // DVR Recording functions
    // /////////////////////////////////////////////////////

    public int getRecordingState()
    {
        return m_recordingState;
    }

    /**
     * Accessor method to obtain the 'current' recording request.
     * 
     * @return the 'current' recording request.
     */
    protected OcapRecordingRequest getCurrentRecordingRequest()
    {
        return m_recordingRequestCurrent;
    }

    /**
     * Returns the length of the specified recording request in milliseconds.
     * 
     * @param orr
     * @return
     */
    public static long getRecordingDuration(OcapRecordingRequest orr)
    {
        long retVal = -1;

        // get the recording duration (in msec) based on the type of
        // recordingSpec
        try
        {
            RecordedService rs = orr.getService();
            return rs.getRecordedDuration();
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    /**
     * Records a service for a specified amount of time.
     * 
     * This method will perform a recording operation given a service specified
     * as a URI and a length of time.
     * @param length
     *            number of seconds to be recorded.
     * @param background Perform recording in background (ServiceRecordingSpec)
     * @param uri
     *            the source ID specified in the format of a URI that looks
     *            like: "ocap://source_id".
     * @param startAdjustment
     *            seconds of delay to apply to the recording start time 
     *            (may be negative)
     * 
     * @return <code>true</code> if the recording request was submitted without
     *         error, <code>false</code> otherwise.
     */
    public boolean doRecording(int length, boolean background, int startAdjustment)
    {
        boolean bRetVal = true;
        int retentionPriority;
        byte recordingPriority;
        int resourcePriority = 0;
        String recordingName = "TestoRecording";
        MediaStorageVolume m_msv = null;
        ExtendedFileAccessPermissions efap =
            new ExtendedFileAccessPermissions(true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              true,
                                              null,
                                              null);

        m_dvrExerciser.logIt("doRecording():entry");

        // start recording - adjusting for the delay factor
        final Date startTime = new Date(System.currentTimeMillis() + startAdjustment*1000);

        // the duration needs to be in msec
        long duration = length * 1000;

        long expiration = length+10000;
//        long expiration = length; // Recording will be eligible for purge as soon as it's complete
        retentionPriority = 10; // This will prevent the recording from being deleted immediately
                                //  upon expiration
        recordingPriority = OcapRecordingProperties.RECORD_IF_NO_CONFLICTS;

        try
        {
            OcapRecordingProperties orp;

            // establish the recording properties
            orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, // bitRate
                    expiration, // expirationPeriod
                    retentionPriority, // retentionPriority
                    recordingPriority, // priorityFlag
                    efap, // access
                    null, // organization
                    m_msv, // destination
                    resourcePriority); // resourcePriority

            RecordingSpec rs;
            
            // formulate the recording request
            if (background)
            {
                m_dvrExerciser.logIt( "doRecording():Starting " + length 
                                      + " second background recording starting " 
                                      + startAdjustment + " seconds from now (" 
                                      + m_shortDateFormat.format(startTime) + ')' );

                rs = new ServiceRecordingSpec( m_serviceContext.getService(), startTime,
                                               duration, orp ); // recordingProperties (expiration time)
            }
            else
            {
                m_dvrExerciser.logIt( "doRecording():Starting " + duration 
                        + " second instant recording starting  " 
                        + startAdjustment + " second from now (" 
                        + m_shortDateFormat.format(startTime) + ')' );

                rs = new ServiceContextRecordingSpec( m_serviceContext, startTime,
                                                      duration, orp ); // recordingProperties (expiration time)
            }

            // submit the recording request
            m_recordingRequestCurrent = (OcapRecordingRequest) m_recordingManager.record(rs);

            if (m_recordingRequestCurrent != null)
            {
                m_dvrExerciser.logIt("****" + recordingName + " scheduled as " + m_recordingRequestCurrent.toString()
                        + "*****");
            }
        }
        catch (Exception e)
        {
            m_dvrExerciser.logIt("doRecording(): Record: FAILED");
            e.printStackTrace();
            m_dvrExerciser.logIt("doRecording(): Flagged FAILURE in Record due to rm.record() exception: "
                    + e.toString());
            bRetVal = false;
        }
        m_dvrExerciser.logIt("doRecording():exit");
        return bRetVal;
    }

    /**
     * Waits for the currently scheduled recording to complete.
     * 
     * This method just waits for the state of the recording request to
     * transition to 'completed'.
     * 
     * @param recordingTime
     *            = expected recording time, in seconds
     * 
     * @return <code>true</code> if the recording has completed,
     *         <code>false</code> otherwise (i.e. timeout)
     */
    public boolean waitForRecordingToComplete(int recordingTime)
    {
        boolean bRetVal = false;
        boolean bDone = false;
        m_dvrExerciser.logIt("waitForRecording():entry");

        // wait for recording to complete (recording time + 3 seconds 'fudge
        // factor'
        int retries = recordingTime + 5;
        while (0 < retries--)
        {
            m_recordingState = m_recordingRequestCurrent.getState();
            switch (m_recordingState)
            {
                case LeafRecordingRequest.COMPLETED_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.COMPLETED_STATE");
                    bRetVal = true;
                    bDone = true;
                    break;

                case LeafRecordingRequest.DELETED_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.DELETED_STATE");
                    break;

                case LeafRecordingRequest.FAILED_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.FAILED_STATE");
                    bDone = true;
                    break;

                case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE");
                    break;

                case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE");
                    break;

                case LeafRecordingRequest.IN_PROGRESS_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.IN_PROGRESS_STATE");
                    break;

                case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE");
                    break;

                case LeafRecordingRequest.INCOMPLETE_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.INCOMPLETE_STATE");
                    bDone = true;
                    break;

                case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.PENDING_NO_CONFLICT_STATE");
                    break;

                case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                    m_dvrExerciser.logIt("LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE");
                    break;

                default:
                    m_dvrExerciser.logIt("Received invalid request state");
                    break;
            }
            m_dvrExerciser.logIt("retries = " + retries);

            if (true == bDone)
            {
                break;
            }

            // sleep for 1 second
            sleepyTime(1000);
        }

        // if we have waited long enough...
        if (-1 == retries)
        {
            // ...assume that it is not going to happen
            m_dvrExerciser.logIt("Recording not completed - time expired - stopping recording request");
            try
            {
                m_recordingRequestCurrent.cancel();
                m_dvrExerciser.logIt("Recording request stopped");
            }
            catch (Exception ex)
            {
                m_dvrExerciser.logIt("Exception occurred stopping recording request: " + ex);
                ex.printStackTrace();
            }
        }
        m_dvrExerciser.logIt("waitForRecording():exit, retValue = " + bRetVal);
        return bRetVal;
    }

    /**
     * Utility method for checking that a certain minimum amount of disk space
     * is available.
     * 
     * This method will log the amount of available disk space found.
     * 
     * @param amount
     *            the minimum amount of space expected.
     * @return the amount of available disk space remaining.
     */
    public long diskFreeCheck(long amount)
    {
        long size = 0;
        m_dvrExerciser.logIt("diskFreeCheck():entry");
        StorageProxy[] sproxy = StorageManager.getInstance().getStorageProxies();

        for (int i = 0; i < sproxy.length; i++)
        {
            LogicalStorageVolume[] lsv = sproxy[i].getVolumes();

            for (int j = 0; j < lsv.length; j++)
            {
                if (lsv[j] instanceof MediaStorageVolume)
                {
                    size += ((MediaStorageVolume) (lsv[j])).getFreeSpace();
                }
            }
        }
        m_dvrExerciser.logIt("Disk space left is " + size + " bytes.");
        if (size < amount)
        {
            m_dvrExerciser.logIt("diskFreeCheck: Required " + DISK_MIN + " bytes of space. Currently only " + size
                    + " bytes left.");
        }
        m_dvrExerciser.logIt("diskFreeChek():exit");
        return size;
    }

    // ///////////////////////////////////////////
    // recording playback management routines
    // ///////////////////////////////////////////

    /**
     * Plays a specified recording request via service selection.
     * 
     * 
     * @param recordingRequest
     */
    public void doPlaybackServiceSelection(OcapRecordingRequest recordingRequest)
    {
        // assume failure
        boolean bRetVal = false;

        m_recordingRequestCurrent = recordingRequest;

        m_dvrExerciser.logIt("doPlayback():entry");

        // prepare for detection of this event if we reach the end of the
        // playback
        // before we request a playback stop
        m_bPresentationTerminatedEventReceived = false;

        // reset the normal content event received flag
        m_bNormalContentEventReceived = false;

        try
        {
            m_serviceContext.select(recordingRequest.getService());

        }
        catch (Exception ex)
        {
            m_dvrExerciser.logIt("doPlayback Exception occurred: " + ex);
        }

        m_dvrExerciser.logIt("doPlayback():exit, result = " + bRetVal);
    }

    /**
     * Stops a recording play of recorded or live content that is currently in
     * progress. This routine will wait until the presentation terminates.
     * 
     */
    public void doStop()
    {
       m_dvrExerciser.logIt("doStop():entry");

        m_serviceContext.getService();

        m_bPresentationTerminatedEventReceived = false;

        long minDur = ((TimeShiftProperties) m_serviceContext).getMinimumDuration();
        if (minDur > 0)
        {
            Player player = getServiceContextPlayer();
            if (player != null)
            {
                ((TimeShiftProperties) m_serviceContext).setPresentation(m_serviceContext.getService(), new Time(
                        Long.MAX_VALUE), player.getRate(), true, true);
            }
        }
        DvrExerciser.getInstance().stopServiceContextAndWaitForPTE(m_serviceContext);

        m_dvrExerciser.logIt("doStop():exit");
    }

    /**
     * This listener logs recording playback events, and updates the flag
     * indicating the playback has started.
     */
    public void notifyRecordingPlayback(ServiceContext context, int artificialCarouselID, int[] carouselIDs)
    {
        // get the class name w/o the package prefix
        String name = context.getClass().getName();
        int firstChar = name.lastIndexOf('.') + 1;
        if (0 < firstChar)
        {
            name = name.substring(firstChar);
        }
        m_dvrExerciser.logIt("Recording playback event received: context = " + name + ", artificialCarouselID = "
                + artificialCarouselID);
        if (null != carouselIDs)
        {
            for (int i = 0; i < carouselIDs.length; i++)
            {
                m_dvrExerciser.logIt("   carouselIDs[" + i + "] = " + carouselIDs[i]);
            }
        }

        // // release threads waiting for playback started notification
        // synchronized(m_objSceLock)
        // {
        // m_objSceLock.notifyAll();
        // }
    }

    /**
     * Sets the playback rate to the specified value.
     * 
     * @param rate
     */
    public float getPlaybackRate()
    {
        float retVal = Float.NaN;

        Player player = null;
        player = getServiceContextPlayer();
        if (null != player)
        {
            retVal = player.getRate();
        }

        return retVal;
    }

    /**
     * Sets the playback rate to the specified value.
     * 
     * @param rate
     */
    public void setPlaybackRate(float rate)
    {
        m_dvrExerciser.logIt("setPlaybackRate():entry, rate = " + rate);

        Player player = null;
        player = getServiceContextPlayer();

        if (null != player)
        {
            player.setRate(rate);
        }

        m_dvrExerciser.logIt("setPlaybackRate():exit");
    }

    /**
     * Sets the playback position to the specified time, relative to the
     * beginning of the recording.
     * 
     * @param rate
     */
    public void setPlaybackPosition(Time time)
    {
        m_dvrExerciser.logIt("setPlaybackPosition():entry, time = " + time);

        Player player = null;
        player = getServiceContextPlayer();
        if (null != player)
        {
            player.setMediaTime(time);
        }

        m_dvrExerciser.logIt("setPlaybackPosition():exit");
    }

    public Time getCurrentPosition()
    {
        m_dvrExerciser.logIt("getPlaybackPosition():entry");
        Time retVal = null;

        Player player = null;
        player = getServiceContextPlayer();
        if (null != player)
        {
            retVal = player.getMediaTime();
        }

        m_dvrExerciser.logIt("getPlaybackPosition():exit, time = " + retVal);
        return retVal;
    }

    private void sleepyTime(int time)
    {
        try
        {
            Thread.sleep(time);
        }
        catch (InterruptedException iex)
        {
            System.out.println();
            m_dvrExerciser.logIt("sleepyTime() interrupted");
        }
    }

    class TSBLoggingTimerWentOffListener implements TVTimerWentOffListener
    {
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            Player player = getServiceContextPlayer();
            if (player != null)
            {
                Service service = m_serviceContext.getService();
                if (service instanceof RecordedService)
                {
                    RecordedService recService = (RecordedService)service;
                    float nanosPerSecond = 1000000000.0F;
                    float millisPerSecond = 1000.0F;
                    int precision = 100; // two digits
                    double buffStart = Math.floor((recService.getFirstMediaTime().getNanoseconds() / nanosPerSecond)
                            * precision + .5)
                            / precision;
                    double mediaTime = Math.floor((player.getMediaTime().getNanoseconds() / nanosPerSecond) * precision
                            + .5)
                            / precision;
                    double buffEnd = Math.floor((recService.getRecordedDuration() / millisPerSecond) * precision
                            + .5)
                            / precision;
                    double deltaToBuffEnd = Math.floor((buffEnd - mediaTime) * precision + .5) / precision;
                    m_dvrExerciser.logIt("buffer start: " + buffStart + ", media time: " + mediaTime + ", buffer end: "
                            + buffEnd + ", secs to buff end: " + deltaToBuffEnd);
                }
                else
                {
                    TimeShiftControl control = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                    if (control != null)
                    {
                        float nanosPerMilli = 1000000000.0F;
                        int precision = 100; // two digits
                        double buffStart = Math.floor((control.getBeginningOfBuffer().getNanoseconds() / nanosPerMilli)
                                * precision + .5)
                                / precision;
                        double mediaTime = Math.floor((player.getMediaTime().getNanoseconds() / nanosPerMilli) * precision
                                + .5)
                                / precision;
                        double buffEnd = Math.floor((control.getEndOfBuffer().getNanoseconds() / nanosPerMilli) * precision
                                + .5)
                                / precision;
                        double deltaToBuffEnd = Math.floor((buffEnd - mediaTime) * precision + .5) / precision;
                        m_dvrExerciser.logIt("buffer start: " + buffStart + ", media time: " + mediaTime + ", buffer end: "
                                + buffEnd + ", secs to buff end: " + deltaToBuffEnd);
                    }
                    else
                    {
                        m_dvrExerciser.logIt("no timeshiftControl for player: " + player);
                    }
                }
            }
            else
            {
                m_dvrExerciser.logIt("no serviceContext player - unable to display buffer info");
            }
        }
    }
    
    protected void displayRecordingList()
    {
        // if the recording selector is not currently being displayed...
        if (null == m_recordingSelector)
        {
            // ...create an instance of the recording selector so the
            // user
            // can select an existing recording
            m_recordingSelector = new RecordingSelector(getPresentableRecordings());
            m_recordingSelector.setBounds(10, 10, DvrExerciser.SCREEN_WIDTH - 100, DvrExerciser.SCREEN_HEIGHT - 100);

            m_recordingSelector.addItemListener(new HItemListener()
            {
                // exists to satisfy HItemListener declaration
                public void currentItemChanged(HItemEvent e)
                {
                }

                /**
                 * Called when the user makes a recording selection.
                 * This method processes the user's selection by
                 * starting playback of the selected recording. Then the
                 * recording selector instance is removed from the
                 * display and released for garbage collection.
                 */
                public void selectionChanged(HItemEvent e)
                {
                    m_dvrExerciser.logIt("Selection changed: " + e.getItem());

                    m_recordingSelector.setVisible(false);
                    m_recordingSelector.setEnabled(false);

                    m_dvrExerciser.remove(m_recordingSelector);

                    m_dvrExerciser.m_scene.requestFocus();

                    m_dvrExerciser.m_scene.repaint();

                    // get the recording selected by the user
                    OcapRecordingRequest orr = m_recordingSelector.getSelectedRecording();

                    if (null != orr)
                    {
                        switch (m_dvrExerciser.m_menuMode)
                        {
                            // In DVR mode, playback the recording
                            case DvrExerciser.MENU_MODE_DVR:
                            {
                                // make sure we are in the playback operational
                                // state
                                PlaybackOperationalState playbackOperationalState = OperationalState.setPlaybackOperationalState();

                                // set the recording to be played
                                playbackOperationalState.setRecordingRequest(orr);

                                // play it
                                playbackOperationalState.play();
                            }
                                break;
                                // In DVR mode, playback the recording
                            case DvrExerciser.MENU_MODE_DVR_DELETE:
                            {
                                // make sure we are in the playback operational
                                // state
                                DeleteOperationalState deleteOperationalState = OperationalState.setDeleteOperationalState();

                                // set the recording to be deleted
                                deleteOperationalState.setRecordingRequest(orr);

                                // delete it
                                deleteOperationalState.start();
                            }
                                break;
                            case DvrExerciser.MENU_MODE_HN:
                                if (DvrHNTest.getInstance().publishRecordingToCDS(orr))
                                {
                                    DvrExerciser.getInstance().logIt("Recording has been published to CDS.");
                                }
                                else
                                {
                                    DvrExerciser.getInstance().logIt("Failed to published recording to CDS.");                    
                                }
                                break;                               

                            default:
                                System.out.println("ERROR - DVRExerciser.displayRecordingList() - " + "unrecognized menu mode = " + 
                                        m_dvrExerciser.m_menuMode);
                        }
                    }

                    // now done with the recording selector, so
                    // relinquish its memory
                    m_recordingSelector = null;
                }
            });

            // place the recording selector on the top of the Z order
            // and...
            m_dvrExerciser.add(m_recordingSelector);
            m_dvrExerciser.popToFront(m_recordingSelector);

            // ... have it process input
            m_recordingSelector.setVisible(true);
            m_dvrExerciser.popToFront(m_recordingSelector);
            m_recordingSelector.setEnabled(true);
            m_recordingSelector.setFocusable(true);

            m_recordingSelector.requestFocus();
        }        
    }
    
    /**
     * Manages a list of recordings from which the user can select. The
     * recordings are displayed using their service names (ugly).
     * 
     * Usage notes: 1. Create an instance of this class, specifying a list of
     * recordings to display. 2. Add the new instance to a container for
     * display. 3. Add an instance of HItemListener to listen for selection
     * events. 4. When a selection event has been detected, call
     * <code>getSelectedRecording()</code> to obtain the selected recording
     * 
     */
    public static class RecordingSelector extends HListGroup
    {
        private static final long serialVersionUID = 1L;

        RecordingList m_recordingList;

        /**
         * Constructor
         * 
         * @param recordingList
         *            list of recordings to display.
         */
        public RecordingSelector(RecordingList recordingList)
        {
            m_recordingList = recordingList;

            // establish some display characteristics
            setBackground(Color.white);
            setBackgroundMode(HVisible.BACKGROUND_FILL);
            setFont(new Font("Tiresias", Font.PLAIN, 14));
            setHorizontalAlignment(HALIGN_LEFT);

            // only allow a single selection
            setMultiSelection(false);

            addItem(new HListElement("Close selection window (press select key to begin)"), 0);

            for (int i = 0; i < m_recordingList.size(); i++)
            {
                OcapRecordingRequest recordingRequest = (OcapRecordingRequest) m_recordingList.getRecordingRequest(i);
                try
                {
                    // get the recorded on date
                    Date date = recordingRequest.getService().getRecordingStartTime();

                    // get the recording length
                    long duration = getRecordingDuration(recordingRequest) / 1000;
                    addItem(new HListElement(date.toString() + ", (" + duration + " sec): "
                            + ((OcapRecordingRequest) recordingRequest).getService().getName()), i + 1);
                }
                catch (Exception ex)
                {
                    System.out.println("Exception getting recorded service name: " + ex);
                    ex.printStackTrace(System.out);
                }
            }
            setCurrentItem(0);
        }

        /**
         * Obtain the recording selected by the user.
         * 
         * @return the selected recording request, may be null, if no recordings
         *         are available.
         */
        public OcapRecordingRequest getSelectedRecording()
        {
            OcapRecordingRequest retVal = null;

            // get the selection index (there should only be one)
            int[] selection = getSelectionIndices();

            // if the user made a selection (note that selecting the item at
            // index = 0 dismisses the selection menu w/o making a selection)...
            if ((null != selection) && (0 != selection[0]))
            {
                // ...get the recording corresponding to the selection
                retVal = (OcapRecordingRequest) m_recordingList.getRecordingRequest(selection[0] - 1);
            }
            return retVal;
        }
    }
    
    protected void keyReleasedDvr(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_1:
                // Display the recording menu
                m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR_RECORD;
                m_dvrExerciser.updateMenuBox();
                break;

            case OCRcEvent.VK_2:
                // Display the playback menu
                m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR_PLAYBACK;
                m_dvrExerciser.updateMenuBox();
                break;
            case OCRcEvent.VK_3:
                // Delete current recording
                //doDeleteCurrentRecording();
                m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR_DELETE;
                m_dvrExerciser.updateMenuBox();
                break;
            case OCRcEvent.VK_4:
                doDeleteAllRecordings();
                break;
            case OCRcEvent.VK_5:
                // Disable buffering (via OcapRecordingManager)
                toggleBufferingEnabled();
                m_dvrExerciser.updateMenuBox();
                break;

            case OCRcEvent.VK_7:
                // Display the media control menu
                m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_MEDIA_CONTROL;
                m_dvrExerciser.updateMenuBox();
                break;

            case OCRcEvent.VK_8:
                // Start a buffering request on the current channel
                toggleBufferingRequest();
                m_dvrExerciser.updateMenuBox();
                break;

            case OCRcEvent.VK_9:
                m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_GENERAL;
                m_dvrExerciser.updateMenuBox();
                break;

            default:
                keyReleasedCommonDvr(e);
        }

        // causes the current mode string to be drawn on the screen
        m_dvrExerciser.repaint();

    } // end keyReleased()


    protected void keyReleasedMediaControl(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_1:
                runAuthorization(true);
                break;
            case OCRcEvent.VK_2:
                runAuthorization(false);
                break;
            case OCRcEvent.VK_3:
                skipBackward(70);
                break;
            case OCRcEvent.VK_4:
                skipForward(70);
                break;
            case OCRcEvent.VK_5:
                // toggle logging of the TSB
                if (true == isTsbLoggingEnabled())
                {
                    enableTsbLogging(false);
                }
                else
                {
                    enableTsbLogging(true);
                }
                break;
            case OCRcEvent.VK_6:
                // toggle the state of the TSB
                if (true == isTsbEnabled())
                {
                    enableTsb(false);
                }
                else
                {
                    enableTsb(true);
                }
                break;
            case OCRcEvent.VK_7:
                int cci = CopyControl.getCCIBits(m_serviceContext.getService());
                m_dvrExerciser.logIt("Retrieved CCI: " + cci);
                break;
            case OCRcEvent.VK_9:
                m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR;
                m_dvrExerciser.updateMenuBox();
                break;
            default:
                keyReleasedCommonDvr(e);
        }
        m_dvrExerciser.repaint();
    }

    protected void keyReleasedRecording(KeyEvent e)
    {
        int delay = 0;
        boolean returnToParentMenu = false;

        switch (e.getKeyCode())
        {
            // 1: Start 10-sec instant recording");
            case OCRcEvent.VK_1:
                recordAndWait(DvrTest.SHORT_RECORDING_LENGTH, false, 0);
                returnToParentMenu = true;
                break;

            // 2: Start 30-sec instant recording");
            case OCRcEvent.VK_2:
                recordAndWait(DvrTest.MEDIUM_RECORDING_LENGTH, false, 0);
                returnToParentMenu = true;
                break;

            // 3: Start 5-min instant recording");
            case OCRcEvent.VK_3:
                recordAndWait(DvrTest.LONG_RECORDING_LENGTH, false, 0);
                returnToParentMenu = true;
                break;

            // 4: Start 10-sec background recording");
            case OCRcEvent.VK_4:
                recordAndWait(DvrTest.SHORT_RECORDING_LENGTH, true, 0);
                returnToParentMenu = true;
                break;

            // 5: Start 30-sec background recording");
            case OCRcEvent.VK_5:
                recordAndWait(DvrTest.MEDIUM_RECORDING_LENGTH, true, 0);
                returnToParentMenu = true;
                break;

            // 6: Start 5-min background recording");
            case OCRcEvent.VK_6:
                recordAndWait(DvrTest.LONG_RECORDING_LENGTH, true, 0);
                returnToParentMenu = true;
                break;
                
            // 7: Schedule 3 20-sec b2b background recordings");
            case OCRcEvent.VK_7:
                delay = 5;    // start 5, 25, and 45 seconds from now
                delayedRecording(DvrTest.TEST_RECORDING_LENGTH, true, delay);
                delay += (DvrTest.TEST_RECORDING_LENGTH);
                delayedRecording(DvrTest.TEST_RECORDING_LENGTH, true, delay);
                delay += (DvrTest.TEST_RECORDING_LENGTH);
                delayedRecording(DvrTest.TEST_RECORDING_LENGTH, true, delay);
                delay += (DvrTest.TEST_RECORDING_LENGTH);
                waitForRecordingToComplete(delay);
                returnToParentMenu = true;
                break;

            // 8: Start custom recording");
            case OCRcEvent.VK_8:
                recordAndWait( m_customRecordingDuration, 
                               true, 
                               m_customRecordingDelta );
                returnToParentMenu = true;
                break;
                
            // UP: Increase the recording duration");
            case OCRcEvent.VK_UP:
                this.m_customRecordingDuration += 10;
                break;

            // DOWN: Decrease the recording duration");
            case OCRcEvent.VK_DOWN:
                this.m_customRecordingDuration -= 10;
                break;

            // LEFT: Adjust the start time earlier");
            case OCRcEvent.VK_LEFT:
                this.m_customRecordingDelta -= 10;
                break;

            // DOWN: Adjust the start time later");
            case OCRcEvent.VK_RIGHT:
                this.m_customRecordingDelta += 10;
                break;

            // 9: Return to DVR Specific Menu");
            case OCRcEvent.VK_9:
                // Fall out
                returnToParentMenu = true;
                break;

            default:
                keyReleasedCommonDvr(e);
        }
        
        if (returnToParentMenu)
        {
            m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR;
        }
        m_dvrExerciser.updateMenuBox();
        m_dvrExerciser.repaint();
    }

    protected void keyReleasedPlayback(KeyEvent e)
    {
        int delay = 0;

        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_1:
                // stop the current operational state
                OperationalState.getCurrentOperationalState().stop();

                // get the 'current' recording
                OcapRecordingRequest ocr = getCurrentRecordingRequest();

                if (null == ocr)
                {
                    m_dvrExerciser.logIt("No current recording to play");
                }
                else
                {
                    // transition to the playback operational state
                    PlaybackOperationalState playbackOperationalState = OperationalState.setPlaybackOperationalState();

                    // indicate to the playback operational state what should be
                    // recorded
                    playbackOperationalState.setRecordingRequest(ocr);

                    playbackOperationalState.play();
                }
                break;

            case OCRcEvent.VK_2:
                displayRecordingList();
                break;

            // 9: Return to DVR Specific Menu");
            case OCRcEvent.VK_9:
                // Fall out
                break;

            default:
                keyReleasedCommonDvr(e);
        }
        
        // Always jump back to the DVR menu
        m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR;
        m_dvrExerciser.updateMenuBox();
        m_dvrExerciser.repaint();
    }
    
    protected void keyReleasedDelete(KeyEvent e)
    {
        boolean returnToParentMenu = false;
        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_1:
                // stop the current operational state
                System.out.println("OperationalState name: " + OperationalState.getCurrentOperationalState().getName());

                OperationalState.getCurrentOperationalState().stop();

                // get the 'current' recording
                OcapRecordingRequest ocr = getCurrentRecordingRequest();

                if (null == ocr)
                {
                    m_dvrExerciser.logIt("No current recording to delete");
                }
                else
                {
                    DeleteOperationalState deleteOperationalState = OperationalState.setDeleteOperationalState();
                    deleteOperationalState.setRecordingRequest(ocr);

                    deleteOperationalState.start();
                }
                returnToParentMenu = true;
                break;

            case OCRcEvent.VK_2:
                displayRecordingList();
                break;

            // 3: Return to DVR Specific Menu");
            case OCRcEvent.VK_3:
                returnToParentMenu = true;
                break;

            default:
                keyReleasedCommonDvr(e);
        }
        
        if (returnToParentMenu)
        {
            m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR;
        }
        else
        {
            m_dvrExerciser.m_menuMode = DvrExerciser.MENU_MODE_DVR_DELETE;            
        }

        m_dvrExerciser.updateMenuBox();
        m_dvrExerciser.repaint();
    }
    
    protected void keyReleasedCommonDvr(KeyEvent e)
    {
        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_CHANNEL_UP:
            case KeyEvent.VK_PAGE_UP:
                // make sure we are in the live operational state and channel up
                OperationalState.setLiveOperationalState().channelUp();
                break;
            case OCRcEvent.VK_CHANNEL_DOWN:
            case KeyEvent.VK_PAGE_DOWN:
                // make sure we are in the live operational state and channel
                // down
                OperationalState.setLiveOperationalState().channelDown();
                break;
            case OCRcEvent.VK_RECORD:
                break;

            case OCRcEvent.VK_STOP:
                // stop the current operational state and go to the stopped
                // state
                OperationalState.setStoppedOperationalState();
                break;

            case OCRcEvent.VK_LIVE:
                // make sure we are in the live operational state and start
                // playing
                OperationalState.setLiveOperationalState().play();
                break;

            case OCRcEvent.VK_PLAY:
                // if we are currently in the stopped operational state...
                OperationalState currentOperationalState = OperationalState.getCurrentOperationalState();
                if (true == (currentOperationalState instanceof StoppedOperationalState))
                {
                    // ...transition to the live operational state
                    currentOperationalState = OperationalState.setLiveOperationalState();
                }

                // now start the current operational state
                currentOperationalState.play();
                break;

            case OCRcEvent.VK_FAST_FWD:
                OperationalState.getCurrentOperationalState().fastForward();
                break;

            case OCRcEvent.VK_REWIND:
                OperationalState.getCurrentOperationalState().rewind();
                break;

            case OCRcEvent.VK_PAUSE:
                OperationalState.getCurrentOperationalState().pause();
                break;

            default:
                break;
        }
    }

    protected void updateMenuBoxDvr()
    {
        // Reset the menu box
        m_dvrExerciser.m_menuBox.reset();

        switch (m_dvrExerciser.m_menuMode)
        {
            case DvrExerciser.MENU_MODE_DVR:
 
                // DVR Exerciser Options title
                m_dvrExerciser.m_menuBox.write("DVR Exerciser Options");

                /*
                 * Display the options
                 */
                m_dvrExerciser.m_menuBox.write("1: Start a recording");
                m_dvrExerciser.m_menuBox.write("2: Playback a recording");
                m_dvrExerciser.m_menuBox.write("3: Delete a recording");
                m_dvrExerciser.m_menuBox.write("4: Erase all recordings");
                m_dvrExerciser.m_menuBox.write("5: " + (m_bufferingEnabled ? "Disable" : "Enable") 
                                                     + " buffering");
                m_dvrExerciser.m_menuBox.write("7: Display media control menu");
                m_dvrExerciser.m_menuBox.write("8: " + (isBufferingRequestActive() ? "Disable" : "Enable") 
                                                     + " BufferingRequest");
                m_dvrExerciser.m_menuBox.write("9: Return to Dvr Exerciser GENERAL Menu");
                break;

            case DvrExerciser.MENU_MODE_MEDIA_CONTROL:
                m_dvrExerciser.m_menuBox.write("Media control options");
                m_dvrExerciser.m_menuBox.write("1: Toggle authorization and run auth check");
                m_dvrExerciser.m_menuBox.write("2: Run auth check without toggling authorization");
                m_dvrExerciser.m_menuBox.write("3: Skip 10 seconds back");
                m_dvrExerciser.m_menuBox.write("4: Skip 10 seconds forward");
                m_dvrExerciser.m_menuBox.write("5: Enable/disable logging of buffer stats");
                m_dvrExerciser.m_menuBox.write("6: Enable/disable TSB");
                m_dvrExerciser.m_menuBox.write("7: Print CCI for current service");
                m_dvrExerciser.m_menuBox.write("9: Return to DVR Specific Menu");

                break;

            case DvrExerciser.MENU_MODE_DVR_RECORD:
                m_dvrExerciser.m_menuBox.write("Recording Menu");
                m_dvrExerciser.m_menuBox.write("1: Start 10-sec instant recording");
                m_dvrExerciser.m_menuBox.write("2: Start 30-sec instant recording");
                m_dvrExerciser.m_menuBox.write("3: Start 5-min instant recording");
                m_dvrExerciser.m_menuBox.write("4: Start 10-sec background recording");
                m_dvrExerciser.m_menuBox.write("5: Start 30-sec background recording");
                m_dvrExerciser.m_menuBox.write("6: Start 5-min background recording");
                m_dvrExerciser.m_menuBox.write("7: Schedule 3 20-sec b2b background recordings");
                m_dvrExerciser.m_menuBox.write("8: Start " + m_customRecordingDuration 
                                               + "-second recording " 
                                               + m_customRecordingDelta + " seconds from now" );
                m_dvrExerciser.m_menuBox.write("   LEFT/RIGHT: Adjust recording start time" );
                m_dvrExerciser.m_menuBox.write("   UP/DOWN: Adjust recording duration" );
                m_dvrExerciser.m_menuBox.write("9: Return to DVR Specific Menu");
                break;

            case DvrExerciser.MENU_MODE_DVR_PLAYBACK:
                m_dvrExerciser.m_menuBox.write("Playback Menu");
                m_dvrExerciser.m_menuBox.write("1: Playback current recording");
                m_dvrExerciser.m_menuBox.write("2: Select recording for playback");
                break;

            case DvrExerciser.MENU_MODE_DVR_DELETE:
                m_dvrExerciser.m_menuBox.write("Delete Menu");
                m_dvrExerciser.m_menuBox.write("1: Delete current recording");
                m_dvrExerciser.m_menuBox.write("2: Select recording for delete");
                m_dvrExerciser.m_menuBox.write("3: Return to DVR Specific Menu");
                break;
                
            default:
                System.out.println("DvrExerciser.updateMenuBox() - Unsupported menu mode " + 
                        m_dvrExerciser.m_menuMode);
        }
    }
    
    /**
     * Schedules a recording for a specified number of seconds, then schedules a
     * new thread that waits for the recording to complete.
     * 
     * @param seconds
     * @param background Recording is a background recording (ServiceRecordingSpec) if true
     *                   or ServiceContext-bound (ServiceContextRecordingSpec) recording
     *                   if false
     * @param startAdjustment Seconds of adjustment to apply to the recording start time 
     *              (can be seconds)
     */
    public void recordAndWait(int seconds, boolean background, int startAdjustment)
    {
        class WaitForRecording implements Runnable
        {
            int m_waitTime;

            public WaitForRecording(int waitTime)
            {
                m_waitTime = waitTime;
            }

            public void run()
            {
                waitForRecordingToComplete(m_waitTime);
            }
        }

        if (true == doRecording(seconds, background, startAdjustment))
        {
            // Allow for retroactive recordings to take 1/2 of their duration time to convert
            new Thread(new WaitForRecording(Math.max( seconds+startAdjustment,
                                                      seconds/2 ) ) )
            {
            }.start();
        }
        else
        {
            m_dvrExerciser.logIt("Unable to start recording");
        }
    }

    public void delayedRecording(int seconds, boolean background, int delay)
    {
        if (true != doRecording(seconds, background, delay))
        {
            m_dvrExerciser.logIt("Unable to start recording");
        }
    }

    public String getCurrentRecState()
    {
        if (m_recordingRequestCurrent == null)
        {
            return "NULL";
        }

        int state = m_recordingRequestCurrent.getState();
        switch (state)
        {
            case LeafRecordingRequest.INCOMPLETE_STATE:
                return "INCOMPLETE";

            case LeafRecordingRequest.COMPLETED_STATE:
                return "COMPLETED";

            case LeafRecordingRequest.FAILED_STATE:
                return "FAILED";

            case LeafRecordingRequest.DELETED_STATE:
                return "DELETED";

            case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                return "PENDING_NO_CONFLICT";

            case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                return "PENDING_WITH_CONFLICT";

            case LeafRecordingRequest.IN_PROGRESS_STATE:
                return "IN_PROGRESS";

            case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                return "IN_PROGRESS_INSUFFICIENT_SPACE";

            case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                return "IN_PROGRESS_WITH_ERROR";

            case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                return "IN_PROGRESS_INCOMPLETE";
        }
        return "UNKNOWN";
    }
    
    void toggleBufferingRequest()
    {
        if (this.m_bufferingRequests.get(m_serviceContext.getService()) != null)
        {
            m_recordingManager.cancelBufferingRequest((BufferingRequest)m_bufferingRequests.get(m_serviceContext.getService()));
            m_bufferingRequests.remove(m_serviceContext.getService());
        }
        else
        {
            BufferingRequest bufferingRequest = BufferingRequest.createInstance( m_serviceContext.getService(), 
                    MIN_TSB_DURATION, MAX_TSB_DURATION, 
                    new ExtendedFileAccessPermissions(true,true,true,true,true,true,null,null));
            m_recordingManager.requestBuffering(bufferingRequest);
            m_bufferingRequests.put(m_serviceContext.getService(), bufferingRequest);
        }
    }

    void toggleBufferingEnabled()
    {
        m_bufferingEnabled = !m_bufferingEnabled;
        
        if (m_bufferingEnabled)
        {
            m_recordingManager.enableBuffering();
        }
        else
        {
            m_recordingManager.disableBuffering();
        }
    }
    boolean isBufferingRequestActive()
    {
        return (m_bufferingRequests.get(m_serviceContext.getService()) != null);
    }
    
    public void setDefaultCurrentRecording()
    {
        RecordingList recordingList = getRecordings();
        if (recordingList.size() > 0)
        {
            m_recordingRequestCurrent = (OcapRecordingRequest)recordingList.getRecordingRequest(0);
        }
        else
        {
            m_dvrExerciser.logIt("Unable to set current recording to empty recording list");            
        }
    }
}
