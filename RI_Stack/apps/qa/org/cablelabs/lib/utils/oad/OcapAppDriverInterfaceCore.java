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

////////////////////////////////////////////////////////////////////////
/// OcapAppDriverInterfaceCore
//
/// This interface defines the methods which provide the underlying driver-like 
/// functionality for RiScriptlet and RiExerciser (and potentially other applications) 
/// for standard Tuning, Generic Playback, DVR Local Playback,
/// HN Playback, and Resource Contention Handling.
/// 
/// This interface defines core OCAP functionality and can be used by any OCAP
/// Xlet without requiring support for HN or DVR extensions.  No methods which
/// require additional OCAP extensions should be added to this interface.  
/// Methods which require additional OCAP extensions should be added to the
/// applicable OcapAppInterface[OCAP Ext], i.e. OcapAppDriverInterfaceHN, etc.
///
/// Methods defined in this interface should only have java primitive type parameters. 
/// There are no no callbacks or events stemming from these methods.  Some APIs
/// have been added to "WaitFor" asynchrounous APIs to complete.  These types
/// of APIs should be added if there is desire to synchrounously wait for
/// given states, (see WaitForTuningState())...
//
public interface OcapAppDriverInterfaceCore
{
    /**
     * String representing an inconclusive search result
     */
    static final String NOT_FOUND = "not found";
    
    /**
     * String representing an unknown value
     */
    static final String UNKNOWN = "Unknown";

    // Static final strings representing the index of playback rates. More Strings
    // will be added as needed
    public static final int PLAY_RATE_INDEX_RWD_64 = 0;
    public static final int PLAY_RATE_INDEX_RWD_32 = 1;
    public static final int PLAY_RATE_INDEX_RWD_16 = 2;
    public static final int PLAY_RATE_INDEX_RWD_8 = 3;
    public static final int PLAY_RATE_INDEX_RWD_4 = 4;
    public static final int PLAY_RATE_INDEX_RWD_2 = 5;
    public static final int PLAY_RATE_INDEX_RWD_1 = 6;
    public static final int PLAY_RATE_INDEX_RWD_HALF = 7;
    public static final int PLAY_RATE_INDEX_PAUSED = 8;
    public static final int PLAY_RATE_INDEX_FWD_HALF = 9;
    public static final int PLAY_RATE_INDEX_PLAY = 10;
    public static final int PLAY_RATE_INDEX_FWD_2 = 11;
    public static final int PLAY_RATE_INDEX_FWD_4 = 12;
    public static final int PLAY_RATE_INDEX_FWD_8 = 13;
    public static final int PLAY_RATE_INDEX_FWD_16 = 14;
    public static final int PLAY_RATE_INDEX_FWD_32 = 15;
    public static final int PLAY_RATE_INDEX_FWD_64 = 16;
    
    final float m_playRates[] = 
    {(float) -64.0,  // 0
     (float) -32.0,  // 1
     (float) -16.0,  // 2
     (float) -8.0,   // 3
     (float) -4.0,   // 4
     (float) -2.0,   // 5
     (float) -1.0,   // 6
     (float) -0.5,   // 7
     (float) 0.0,    // 8
     (float) 0.5,    // 9
     (float) 1.0,    // 10
     (float) 2.0,    // 11
     (float) 4.0,    // 12
     (float) 8.0,    // 13
     (float) 16.0,   // 14
     (float) 32.0,   // 15
     (float) 64.0, };// 16
    
    // Static final ints to be used for tuner states in Rx scripts
    public static final int TUNING_FAILED = 3;
    public static final int TUNING_IDLE = 0;
    public static final int TUNED = 2;
    public static final int TUNING = 1;
    
    /**
     * Constants used to represent what kind of playback is presenting. Values are
     * currently either JMF_PLAYBACK when JMF player is used, or SERVICE_PLAYBACK
     * indicating that a service selection is made to perform playback.
     */
    public static final int PLAYBACK_TYPE_UNKNOWN = -1;
    public static final int PLAYBACK_TYPE_SERVICE = 1;
    public static final int PLAYBACK_TYPE_JMF = 2;
    public static final int PLAYBACK_TYPE_TSB = 3;

    // Strings describing playback types    
    public static final String PLAYBACK_TYPE_STR_UNKNOWN = "UNKOWN";
    public static final String PLAYBACK_TYPE_STR_SERVICE = "SERVICE CONTEXT";
    public static final String PLAYBACK_TYPE_STR_JMF = "JMF";
    public static final String PLAYBACK_TYPE_STR_TSB = "TSB";
    
    /**
     * Constants used to represent what type of content the playback is presenting.
     */
    public static final int PLAYBACK_CONTENT_TYPE_UNKNOWN = -1;
    public static final int PLAYBACK_CONTENT_TYPE_DVR = 1;
    public static final int PLAYBACK_CONTENT_TYPE_HN = 2;
    
    // Methods for initializing OcapAppDriverInterfaceCore instances
    void initChannelMap(boolean useJavaTVChannelMap, String fileName);
    
    void setResourceContentionHandling(boolean resContentionHandling);
    
    
    ////////////////////////////////////////////////////////////////////////
    /// Tuning
    //

    /**
     * Returns the number of available tuners.
     * @return the number of available tuners, or -1 if an error occurs
     */
    int getNumTuners();

    /**
     * Returns the index of the Service currently tuned on the OcapTuner at the
     * specified index
     * 
     * @return the Service index of the tuner at index tunerIndex, or -1 for error
     */
    int getServiceIndex();

    /**
     * Returns the number of available Services
     * @return the number of available Services, or -1 for error
     */
    int getNumServices();

    /**
     *  Returns the string representation of the Service at the given index.
     *  @param the index of the Service
     *  @return a String representation of the Service at index serviceIndex
     */
    String getServiceInfo(int serviceIndex);

    /**
     * Returns the Service name for the Service at the given index
     * @param serviceIndex the index of the Service
     * @return the name of the Service as defined by getName() in Service, or
     * null if the index is out of range for the list of Services
     */
    String getServiceName(int serviceIndex);

    /**
     * Select the Service (by the given name) for the given tuner index
     * 
     * @param serviceName the name of the Service as defined by getName() in
     * Service
     * @return true if the Service with the given name at the given tuner index
     * was successfully selected, false if an error occurs selecting the Service
     */
    boolean serviceSelectByName(String serviceName);

    /**
     * Select the service (by the given index) using the current service context.
     *
     * @param serviceIndex the index of the Service to be selected
     * 
     * @return true if the Service at the given index is successfully selected, 
     * false if an error occurs selecting the Service
     */
    boolean serviceSelectByIndex(int serviceIndex);
    
    /**
     * Convenience method for selecting the next LiveService in the list of
     * available LiveServices. In contrast to the methods serviceSelectByIndex()
     * and serviceSelectByName(), the next available Service is selected 
     * asynchronously. This allows the fast selection of Services without blocking
     * between selections.
     */
    void channelUp();
    
    /**
     * Convenience method for selecting the previous LiveService in the list of
     * available LiveServices. In contrast to the methods serviceSelectByIndex()
     * and serviceSelectByName(), the previous available Service is selected 
     * asynchronously. This allows the fast selection of Services without blocking
     * between selections.
     */
    void channelDown();
    
    /**
     * Returns as soon as one of the two following conditions is met: a 
     * NormalContentEvent or SelectionFailedEvent has been received, or the 
     * amount of time indicated has passed.  
     * @param timeout the amount of time to wait in seconds
     * @param tuningState the tuning state to wait for
     * @return true if the tuning state was reached, false if the timeout occurred
     * before reaching the tuning state
     */
    boolean waitForTuningState(long timeout, int tuningState);
    
    /**
     * Resets the index of the currently selected Service as indicated by the
     * LiveServiceManager. 
     */
    void resetLiveServiceIndex();
    
    /**
     * Modify the supplied tuner sync state to the specified value.  Tuner sync
     * states are SYNC and UNSYNC.
     * 
     * @param tunerIndex        index of tuner to modify
     * @param tunerSync         true if tuner should be synced, false to unsync tuner
     * 
     * @return  true if tuner sync state set to supplied state, false if problems encountered
     */
    boolean setTunerSyncState(int tunerIndex, boolean tunerSync);
    
    

    ////////////////////////////////////////////////////////////////////////
    /// Generic Playback
    //
    /**
     * Toggle authorization and trigger an authorization check
     *
     * @param toggleAuthorizationFirst if true, toggle authorization before running authorization check
     */
    void runAuthorization(boolean toggleAuthorizationFirst);

    /**
     * Gets the MediaTime in nanoSeconds from the Player
     * 
     * @return the MediaTime in nanoseconds of the indicated Player, or Long.MIN_VALUE
     * if the indicated Player is null
     */
    long getMediaTime();
    
    /**
     * Returns the duration in nanoseconds of the Player. 
     * 
     * @return total duration of playback in nanoseconds, Long.MIN_VALUE if
     * unknown
     */
    long getPlaybackDurationNS();
    
    /**
     * Stops service selection which was initiated either by serviceSelectByIndex() or 
     * serviceSelectByName().
     * 
     * @param waitTimeSecs  amount of time in seconds to wait for playback to stop 
     *  
     * @return  true if service was stopped, false if it was stopped already
     */
    boolean serviceSelectStop(int waitTimeSecs);
    
    /**
     * Returns total duration of content in seconds associated with player.
     *  
     * @return  duration of content in seconds, -1 if not available
     */
    double getPlaybackDurationSecs();

    /**
     * This method resizes the playback at the given index to the indicated 
     * horizontal and vertical scaling factors.
     * 
     * @param horizontalScalingFactor the factor by which to scale the horizontal
     *  dimension of the playback
     * @param verticalScalingFactor the factor by which to scale the vertical
     * dimension of the playback
     * @param startX the x starting point of the video playback
     * @param startY the y starting point of the video playback
     * 
     * @return true if the transformation was successful, false otherwise
     */
    boolean playbackTransformVideo(float horizontalScalingFactor,
                                   float verticalScalingFactor,
                                   int startX,
                                   int startY);
    
    /**
     * Set the playback at the given index to fullscreen
     * 
     * @return true if the given playback was set to fullscreen, false if the
     * resizing is unsuccessful or an error occurs.
     */
    boolean setPlaybackFullscreen();

    /**
     *  This method will stop playback.
     *  
     * @param waitTimeSecs  amount of time in seconds to wait for playback to stop
     *  
     *  @return true if successful, false if an error occurs
     */
    boolean playbackStop(int waitTimeSecs);
    
    /**
     * Gets the playback rate of the Player.
     * 
     * @return the given playback's current rate or Float.Nan if the Player is null
     */
    float getPlaybackRate();

    /**
     * Set the given playback's rate
     * 
     * @param rateIndex the index of the playback rate
     * 
     * @return true if the playback rate was successful, false otherwise
     */
    boolean playbackChangeRate(int rateIndex);

    /**
     * Return the given playback's current position
     * 
     * @return the current position of the playback in nanoseconds or
     * 0 for error
     */
    long getPlaybackPosition();

    /**
     * Returns the current media time or playback position for the player.
     * 
     * @return  current playback media time for this player
     */
    double getPlaybackPositionSecs();

    /**
     * Set the given playback's position
     * 
     * @param time the time in nanoseconds to set this playback position to
     * 
     * @return true if the playback position was successfully set, false otherwise
     */
    boolean setPlaybackPosition(long time);
    
    /**
     * Skip the player forward by the indicated seconds
     * 
     * @param seconds the number of seconds to skip forward
     */
    void skipForward(int seconds);
    
    /**
     * Skip the player backward by the indicated seconds
     * 
     * @param seconds the number of seconds to skip backward
     */
    void skipBackward(int seconds);

    /**
     * Return the value in the table of possible rates at the given index 
     * @param rateIndex the index in the table of possible rates
     * @return the value stored at the indicated index in the table, or Float.Nan
     * for error
     */
    float getPlayRate(int rateIndex);

    /**
     * Return the index of the given rate in the table of possible rates
     * @param rate the rate in question
     * @return the index of the given rate in the table of possible rates, or -1
     * for error
     */
    int getPlayRateIndex(float rate);
        
    /**
     * Retrieves the current event id.  Callers can use this value when
     * waiting for upcoming events to ensure they did not miss any events which
     * may have been recieved prior to waiting for a specific event.
     * 
     * @return  current event id representing last event received
     */
    int playbackEventGetLastIndex(); 
    
    // Types of playback events which caller can wait for
    int PLAYBACK_EVENT_TYPE_UNKNOWN = -1;
    int PLAYBACK_EVENT_TYPE_CONTROLLER = 1;
    int PLAYBACK_EVENT_TYPE_SERVICE_CONTEXT = 2;  

    String PLAYBACK_EVENT_TYPE_STR_UNKNOWN = "Unknown";
    String PLAYBACK_EVENT_TYPE_STR_CONTROLLER = "Controller";
    String PLAYBACK_EVENT_TYPE_STR_SERVICE_CONTEXT = "Service Context";  

    /**
     * Waits for the next event of the specified type.  It uses the
     * starting id supplied as a starting point for events.  If an event
     * has already been received that has an id greated thatn the id supplied,
     * this method will return immediately.  This method will return if no
     * event has been received in the specified timeout.
     * 
     * @param   type        playback event type, a PLAYBACK_EVENT_TYPE... value
     * @param   startId     start looking for events whose id is greater than
     *                      this supplied value
     * @param   timeoutSecs max number of seconds to wait for this event,
     *          -1 indicates wait forever
     * 
     * @return  returns event id of received event, -1 if timed out
     */
    int playbackWaitForNextEvent(int type, int startEventIndex, long timeoutSecs);  
    
    /**
     * Returns the type of the event at the specified index.  The type
     * will be one of the PLAYBACK_EVENT_TYPE... values.
     * 
     * @param eventIndex    get type of this event
     * 
     * @return  event type identified via PLAYBACK_EVENT_TYPE...
     */
    int playbackEventGetType(int eventIndex); 
   
    /**
     * Returns description associated with this event which was set
     * when event was received.
     * 
     * @param   eventIndex  get description of event assigned to this id
     * 
     * @return  description of event, may be null
     */
    String playbackEventGetDescription(int eventIndex);
    
    // Integers representing playback states
    public static final int PLAYBACK_STATE_UNKNOWN              = 0;
    public static final int PLAYBACK_STATE_PRESENTING           = 1;
    public static final int PLAYBACK_STATE_BEGINNING_OF_CONTENT = 2;
    public static final int PLAYBACK_STATE_END_OF_CONTENT       = 3;
    public static final int PLAYBACK_STATE_PAUSED               = 4;
    public static final int PLAYBACK_STATE_FAILED               = 5;

    // Strings describing playback states    
    public static final String PLAYBACK_STATE_STRS[] = new String[]{
        "UNKNOWN",
        "PRESENTING",
        "BEGINNING_OF_CONTENT",
        "END_OF_CONTENT",
        "PAUSED",               
        "FAILED"
    };

    /**
     * Returns current state of playback which is one of the
     * PLAYBACK_STATE_... values.  This value is internally updated
     * in driver based on registered listeners for playback related
     * events.  The state will be updated as events are received by
     * driver.
     * 
     * @return  current playback state
     */
    int playbackGetState();

    /**
     * A method to access the CCI Bits from the current Service
     * @return the CCI bits for the current service
     */
    int getCCIBits();   

    /**
     * A method to access the informative name of a Channel, if one exists. The
     * informative name of the Channel is the name defined in the config.properties
     * channel map by the arg _channel_name
     * @param serviceIndex the index of the Service
     * @return the informative name of the Channel, if one exists, otherwise
     * UNKNOWN
     */
    String getInformativeChannelName(int serviceIndex);
    
    /**
     * Returns the application's organization name which is used in controlling access to
     * recorded or published content.
     * @return organization ID as a fixed-length 8 character hexadecimal string, padded
     *         with leading zeroes as needed, or null to disable playback authentication
     *         for associated recordings.
     */
    String getOrganization();
    
    /**
     * Sets the application's organization name to be used in playback authentication of
     * recorded or published content.
     * @param oid the hexadecimal string representation of the "dvb.org.id" property
     *            from the Xlet context, or null to disable playback authentication
     *            for associated recordings.
     */
    void setOrganization(String oid);
    
    ////////////////////////////////////////////////////////////////////////
    /// Resource Contention Handling
    //
    /**
     * Returns the number of ResourceUsages, both current requests and new
     * requests
     * @return the total number of ResourceUsage reservations
     * -1 for error
     */
    int getNumReservations();
    
    /**
     * Returns a String representing the ResourceUsage reservation located at
     * the specified index in the List of ResourceUsage reservations.
     * @param index the index of the ResourceUsage reservation
     * @return a String representing the ResourceUsage reservation at index, or
     * null if the supplied index is out of bounds for the List of ResourceUsage
     * reservations
     */
    String getReservationString(int index);
    
    /**
     *  Returns the number of pre-defined Resource Contention Handlers
     * @return the number of Resource Contention Handlers, or -1 for error
     */
    int getNumResourceContentionHandlers();
    
    /**
     * Indicates whether ResouceContentionHandling is currently active
     * @return true if InteractiveResourceContentionHandler is handling
     * ResourceContention issues, false otherwise
     */
    boolean resourceContentionActive();
    
    /**
     * A method to move a ResourceUsage request to the bottom of the list of
     * ResourceUsage requests
     * @param index the index of the ResourceUsage request to be moved to the
     * bottom of the list.
     * @return true if the ResourceUsage was moved to the bottom of the list, 
     * false otherwise
     */
    boolean moveResourceUsageToBottom(int index);
    
    /**
     * Informs the InteractiveResourceUsageSorter that the user has selected
     * the priorities for ResourceUsage requests, so the ResourceContention has
     * been handled.
     */
    void setResourceContentionHandled();

    /**
     * Return a string representation of the given Resource Contention Handler
     * @param rchIndex the index of the Resource Contention Handler.
     * @return a String representation of the Resource Contention Handler at the
     * given index, or null for error
     */
    String getResourceContentionHandlerInfo(int rchIndex);

    /**
     * Pre-select a pre-defined Resource Contention Handler to use on the next
     * contention.
     * @param rchIndex the index of the Resource Contention Handler to use
     * @return true if successful, false otherwise
     */
    boolean preselectResourceContentionHandler(int rchIndex);

    /**
     * Select a pre-defined Resource Contention Handler to use for 
     * the current contention.
     * @param rchIndex the index of the chosen Resource Contention Handler
     * @return true if successful, false otherwise
     */
    boolean selectResourceContentionHandler(int rchIndex);
    
    /** 
     * Get the language preference array - a list of 3-character language codes 
     * with element [0] having the highest preference.
     * 
     */
    public String [] getLanguagePreference();

    /** 
     * Set the language preference to the given list of 3-character language codes 
     * with element [0] having the highest preference.
     * 
     * @param languages Array of language codes.
     */
    void setLanguagePreference(String languages[]);
}