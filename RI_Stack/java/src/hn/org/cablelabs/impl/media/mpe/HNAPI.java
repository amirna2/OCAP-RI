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
package org.cablelabs.impl.media.mpe;

public interface HNAPI
{
    public interface MediaHoldFrameMode
    {
       /*
        * WARNING: These constants represent *NATIVE* enum values defined in 
        * mpeos_media.h - mpe_MediaHoldFrameMode. If the enum values change, 
        * then these values will need to change accordingly.
        */
        
        /* Display black frame when playback is stopped */
        static final int HN_MEDIA_STOP_MODE_BLACK = 0;
        
        /* Display a still of the last frame displayed when playback is stopped */
        static final int HN_MEDIA_STOP_MODE_HOLD_FRAME = 1;
    }
    
    /**
     * This defines the native event codes that can be received asynchronously
     * for a hn streaming session started by the
     * {@link HNAPIImpl#openSession(Params)} method.
     */
    public interface Event
    {
        /*
         * WARNING: These constants represent *NATIVE* event codes. If the
         * native event codes change, then these values will need to change
         * accordingly.
         */
        static final int HN_EVENT_BASE = 100;
        static final int SOCKET_EVENT_BASE = 500;

        /**
         * This event is sent during player or server playback sessions when
         * rate is positive and the OS session closes without error (player) or
         * the end of streamable content is reached(server).
         */
        static final int HN_EVT_END_OF_CONTENT = HN_EVENT_BASE + 1; /*
                                                                  * reached the
                                                                  * end of the
                                                                  * streaming
                                                                  * /streamable
                                                                  * content
                                                                  */

        /**
         * This event is sent during player or server playback sessions when
         * rate is negative and the OS session closes without error (player) or
         * the end of streamable content is reached(server).
         */
        static final int HN_EVT_BEGINNING_OF_CONTENT = HN_EVENT_BASE + 2; /*
                                                                        * beginning
                                                                        * of the
                                                                        * streaming
                                                                        * /
                                                                        * streamable
                                                                        * content
                                                                        */

        /**
         * This event is sent in response to a mpeos_hnSessionClose() call. It
         * represents the successful closing and shuting down of an OS streaming
         * session for both server and player.
         */
        static final int HN_EVT_SESSION_CLOSED = HN_EVENT_BASE + 3; /*
                                                                     * indicates
                                                                     * a hn
                                                                     * session
                                                                     * is
                                                                     * complete
                                                                     */

        /**
         * This event is sent in response to a mpeos_hnSessionOpen() call. It
         * represents the successful initialization and opening of an OS
         * streaming session for both server and player.
         */
        static final int HN_EVT_SESSION_OPENED = HN_EVENT_BASE + 4; /*
                                                                     * indicates
                                                                     * a hn
                                                                     * session
                                                                     * is start
                                                                     */

        //MPE_HN_EVT_PLAYBACK START = MPE_CONTENT_PRESENTING (0x5)
        
        /**
         * This event is sent in response to a mpeos_hnPlaybackStop() call. It
         * represents the successful closing and shuting down of an HN playback
         * session for both server and player.
         */
        static final int HN_EVT_PLAYBACK_STOPPED = HN_EVENT_BASE + 6; /*
                                                                       * indicates
                                                                       * a hn
                                                                       * playback
                                                                       * has
                                                                       * stopped
                                                                       */

        /**
         * This event is sent during ongoing streaming and playback sessions when the
         * session or playback fails unexpectedly on both server and player. An error
         * code will be set describing the error condition. Examples include:
         * The PLAYER fails to decode an incoming transmission due to stream format issues.
         * The SERVER fails to encode outgoing transmission due to internal resource problems.
         */
        static final int MPE_HN_EVT_SESSION_NO_LONGER_AUTHORIZED = HN_EVENT_BASE + 7;
        
        //MPE_HN_EVT_FAILURE = MPE_FAILURE_UNKNOWN (0x06)
        
        /**
         * For SERVER playback, this event is sent in reponse to mpeos_playbackStart()
         * call when the platform has been unable to send content for 
         * the supplied connection stalling timeout number of seconds.
         */
        static final int HN_EVT_INACTIVITY_TIMEOUT = HN_EVENT_BASE + 8;

        /**
         * This event is sent when platform detects an IP address has been added
         * to an interface. Uses mpe socket events
         */
        static final int SOCKET_EVT_IP_ADDED = SOCKET_EVENT_BASE + 1;

        /**
         * This event is sent when platform detects an IP address has been removed 
         * from an interface.
         */
        static final int SOCKET_EVT_IP_REMOVED = SOCKET_EVENT_BASE + 2;
    }

    /**
     * This represents the data returned from starting a playback session.
     */
    public static class Playback
    {
        public Playback(int playbackHandle, float playbackRate, float initialGain)
        {
            this.handle = playbackHandle;
            this.rate = playbackRate;
            this.initialGain = initialGain;
        }

        public final int handle;

        public final float rate;

        public final float initialGain;

        public String toString()
        {
            return "Playback {handle=" + MediaAPIImpl.handleToString(handle) + ", rate=" + rate + ", gain: " + initialGain + "}";
        }
    }

   /**
     * This represents the data returned for the WakeUp related information
     * for a LPE.
     */
    public static class LPEWakeUp
    {
        public LPEWakeUp(String wakeOnPattern, String wakeSupportedTransport, int maxWakeOnDelay, int dozeDuration)
        {
            this.wakeOnPattern = wakeOnPattern;
            this.wakeSupportedTransport = wakeSupportedTransport;
            this.maxWakeOnDelay = maxWakeOnDelay;
            this.dozeDuration = dozeDuration;
        }

        public final String wakeOnPattern;

        public final String wakeSupportedTransport;

        public final int maxWakeOnDelay;
        
        public final int dozeDuration;
        
    }

}
