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

import java.awt.Dimension;

import javax.media.GainControl;

import org.ocap.media.S3DConfiguration;


public interface MediaAPI
{
    /*
     * Event Support
     */

    /**
     * This defines the native event codes that can be received asynchronously
     * for a decoding session started by the
     * {@link MediaAPIImpl#decodeBroadcast(MediaDecodeParams)} method.
     */
    public interface Event
    {
        /*
         * WARNING: These constants represent *NATIVE* event codes. If the
         * native event codes change, then these values will need to change
         * accordingly.
         */

        // Generic Decode Session Events

        /** Decoding session successfully started presentation. */
        static final int CONTENT_PRESENTING = 0x5;

        /** ED Queue for decoding session terminated. */
        static final int QUEUE_TERMINATED = 0x8;

        // Dripfeed Events

        /** Still frame decoded for dripfeed session. */
        static final int STILL_FRAME_DECODED = 0x7;

        // MPEG Program Decode Events

        /** Unknown failure while decoding. */
        static final int FAILURE_UNKNOWN = 0x6;

        /** Decode starvation */
        static final int DECODER_STARVED = 0x9;

        /** Recovered from decode starvation */
        static final int DECODER_NO_LONGER_STARVED = 0X10;

        // MPEG Stream Decode Events
        // data1 of ED event contains PID of denied/returned stream

        /** A stream that was previously denied has been returned (allowed). */
        static final int STREAM_RETURNED = 0xC;

        /** No data is arriving for a stream. */
        static final int STREAM_NO_DATA = 0xD;

        /** Stream denied because payment dialog is required. */
        static final int STREAM_CA_DIALOG_PAYMENT = 0x02;

        /**
         * Stream denied because technical dialog (e.g., PIN entry) is required.
         */
        static final int STREAM_CA_DIALOG_TECHNICAL = 0x03;

        /** Stream denied due to lack of conditional access entitlement. */
        static final int STREAM_CA_DENIED_ENTITLEMENT = 0x71;

        /** Stream denied due to conditional access technical reasons. */
        static final int STREAM_CA_DENIED_TECHNICAL = 0x73;

        // MPEG Video Events

        /** The Active Format Descriptor (AFD) changed. */
        static final int ACTIVE_FORMAT_CHANGED = 0x15;

        /** The Aspect Ration (AR) changed. */
        static final int ASPECT_RATIO_CHANGED = 0x16;

        /** The Decoder Format Conversion (DFC) changed. */
        static final int DFC_CHANGED = 0x17;

        /** The 3D format has changed. */
        static final int S3D_FORMAT_CHANGED = 0x18;

        static final int CCI_UPDATE = 0x19;
                
        /** The content cannot be presented. */
        static final int CONTENT_NOT_PRESENTING = 0x0F;

        /* Reasons sent with CONTENT_PRESENTING event map to mpe_PresentingReasons enumeration */

        /**
         * 2D content is being successfully presented
         */
         static final int CONTENT_PRESENTING_2D_SUCCESS = 0;

        /**
         * 3D content is being successfully presented
         */
         static final int CONTENT_PRESENTING_3D_SUCCESS = 1;

        /**
         * 3D content is being presented, but the content is encoded in a format which may or may not be supported by the
         * 3D-capable display device.
         */
        static final int CONTENT_PRESENTING_3D_FORMAT_NOT_CONFIRMED = 2;

        /* Reasons sent with CONTENT_NOT_PRESENTING event map to mpe_NotPresentingReasons enumeration */

        /**
         * Data starvation is preventing presentation
         */
        static final int CONTENT_NOT_PRESENTING_NO_DATA = 0;

        /**
         * The display device is not capable of presenting 3D formatted content.
         */
        static final int CONTENT_NOT_PRESENTING_3D_DISPLAY_DEVICE_NOT_CAPABLE = 1;

        /**
         * 3D content is being presented but no 3D-capable display device is connected.
         */
        static final int CONTENT_NOT_PRESENTING_3D_NO_CONNECTED_DISPLAY_DEVICE = 2;
    }

    /*
     * Clock Support
     */

    /**
     * Start a native decode session.
     * 
     * @param params
     *            - the {@link MediaDecodeParams} to use.
     * @return Returns a handle to a broadcast decode session.
     */
    int decodeBroadcast(final MediaDecodeParams params);

    /**
     * Change the active pids on a decode session.
     * 
     * @param sessionHandle
     *            The native handle of the video device that is decoding.
     * @param pcrPid
     *            - the new pcrPid to use.
     * @param pids
     *            - the new set of pids to use for decoding
     * @param types
     *            - corresponding pid types for the pids
     */
    void changePids(int sessionHandle, int pcrPid, int[] pids, short[] types);

    /**
     * Stop an in-progress decoding session that was started by a prior call to
     * {@link #decodeBroadcast(MediaDecodeParams) decode()}.
     * 
     * @param sessionHandle The native handle of the video device that is decoding.
     * @param holdFrame   display the last frame if true
     *
     */
    void stopBroadcastDecode(int sessionHandle,boolean holdFrame);

    /*
     * Parental Control Support
     */

    /**
     * Set the blocking state of the presentation.
     * 
     * @param sessionHandle
     *            - handle of the decode session
     * @param block
     *            - indicates whether to block: <code>true</code> means block;
     *            <code>false</code> means not.
     */
    void blockPresentation(int sessionHandle, boolean block);

    /*
     * DripFeed Support
     */

    /**
     * Start a drip feed session.
     * 
     * @param params
     *            - the {@link MediaDripFeedParams} to use.
     * @return Returns a handle to a drip feed decode session
     */
    int dripFeedStart(final MediaDripFeedParams params);

    /**
     * Submit a frame of data to a drip feed session.
     * 
     * @param dripFeedHandle
     *            - A drip feed session returned by {@link dripFeedStart}.
     * @param frameData
     *            - An array of bytes containing an MPEG2 I,P-frame to be
     *            decoded
     */
    void dripFeedRenderFrame(int dripFeedHandle, byte[] frameData);

    /**
     * Stop a drip feed session.
     * 
     * @param dripFeedHandle
     *            - A drip feed session returned by {@link dripFeedStart}.
     */
    void dripFeedStop(int dripFeedHandle);

    /*
     * ScaledVideoManager Support
     */

    /**
     * Swap decoders at native layer.
     * 
     * @param vd1
     *            The native handle of the 1st video device to swap.
     * @param vd2
     *            The native handle of the 2nd video device to swap.
     * @param audioUse
     *            Whether to use the audio of the swapped decoder.
     * 
     */
    void swapDecoders(int vd1, int vd2, boolean audioUse);

    /*
     * AWTVideoSizeControl / VideoPresentationControl /
     * BGVideoPresentationControl Support
     */

    /**
     * Call native method to check video bounds.
     * 
     * @param vd
     *            The native handle of the video device for which to check the
     *            size.
     * @param desired
     *            {@link ScalingBounds} indicating the desired
     *            {@link javax.tv.media.AWTVideoSize AWTVideoSize} in native
     *            coordinates.
     * @return Returns the {@link ScalingBounds} that are the closest match to
     *         the desired bounds.
     */
    ScalingBounds checkBounds(int vd, ScalingBounds desired);

    /**
     * Call native to set the video bounds.
     * 
     * @param vd
     *            The native handle of the video device for which to set the
     *            size.
     * @param size
     *            The new {@link ScalingBounds} to set.
     * @return Returns <code>true</code> if the size could be assigned;
     *         otherwise, <code>false</code>.
     */
    boolean setBounds(int vd, ScalingBounds size);

    /**
     * Call native to determine current video bounds.
     * 
     * @param vd
     *            The native handle of the video device for which to query the
     *            current size.
     * @return @returns Returns the current video size in native coordinates (
     *         {@link ScalingBounds}).
     */
    ScalingBounds getBounds(int vd);

    /**
     * Call native to determine the size of the input video.
     * 
     * @param vd
     *            The native handle of the video device for which to query the
     *            input size.
     * @return Returns a {@link Dimension} representing the width and height of
     *         the input video stream.
     */
    Dimension getVideoInputSize(int vd);

    /*
     * FreezeControl Support
     */

    /**
     * Freeze the decoding session, displaying the last frame decoded until a
     * subsequent call to {@link #resume(int) resume()}.
     * 
     * @param vd
     *            The native handle of the video device to freeze.
     * 
     */
    void freeze(int vd);

    /**
     * Resume a decoding session that was frozen by a prior call to
     * {@link #freeze(int) freeze()}.
     * 
     * @param vd
     *            The native handle of the video device to un-freeze.
     * 
     */
    void resume(int vd);

    /**
     * This method determines whether the video device should be used for
     * component video. This is not the same as whether the device supports
     * scaling, although scaling support is necessary. Some devices, in
     * particular the background video device, do not make sense for component
     * video.
     * 
     * @param vd
     *            The native handle of the video device.
     * @return Returns <code>true</code> if the video device can be used for
     *         component video; otherwise, <code>false</code>.
     */
    boolean supportsComponentVideo(int vd);

    /*
     * ClosedCaptioningControl Support
     */

    /**
     * @return Returns the current state of the closed-captioning subsystem.
     * @see org.ocap.media.ClosedCaptioningControl#getClosedCaptioningState()
     */
    int getCCState();

    /**
     * Sets the state of the closed-captioning subsystem.
     * 
     * @param ccState
     *            This <code>int</code> value indicates the new state.
     * @see org.ocap.media.ClosedCaptioningControl#setClosedCaptioningState(int)
     */
    void setCCState(int ccState);

    /**
     * Set the closed-captioning service numbers.
     * 
     * @param analogSvcNo
     *            The analog service number.
     * @param digitalSvcNo
     *            The digital service number.
     * @see org.ocap.media.ClosedCaptioningControl#setClosedCaptioningServiceNumber(int,
     *      int)
     */
    void setCCServiceNumbers(int analogSvcNo, int digitalSvcNo);

    /**
     * Retrieves the services that support closed captioning.
     * 
     * @param maxServices
     * @return the array of services that support closed captioning
     */
    int[] getCCSupportedServiceNumbers();

    /*
     * VideoPresentationControl Support
     */

    /**
     * Get the current aspect ratio for an active decode session.
     * 
     * @param vd
     *            - video device being used by the decode session to check.
     * @return Returns the current aspect ratio.
     * @see org.ocap.media.VideoFormatControl#getAspectRatio()
     */
    int getAspectRatio(int vd);

    /**
     * Get the current active format definition (AFD).
     * 
     * @param vd
     *            - video device representing the decode session for which to
     *            get the AFD.
     * @return Returns the current AFD.
     * @see org.ocap.media.VideoFormatControl#getActiveFormatDefinition()
     */
    int getActiveFormatDefinition(int vd);

    /**
     * Check whether the a decode session is currently in platform mode.
     * 
     * @param vd
     *            - video device representing the decode session to check.
     * @return Returns whether or not the system is in platform mode; implies
     *         the current DFC is DFC_PROCESSING_PLATFORM
     * @see org.ocap.media.VideoFormatControl#getDecoderFormatConversion()
     */
    boolean isPlatformDFC(int vd);

    /**
     * Get the current decoder format conversion (DFC) for an ongoing decode
     * session. The DFC returned will never be DFC_PLATFORM and will represent
     * the DFC mode being applied by the system at the time of this call. This
     * DFC can either be set by the application or dictated by the platform.
     * 
     * @see isPlatformDFC
     * 
     * @param vd
     *            - the video device representing the decode session.
     * @return Returns the current decoder format conversion in use by the
     *         session.
     * @see org.ocap.media.VideoFormatControl#getDecoderFormatConversion()
     */
    int getDFC(int vd);

    /**
     * Get the input video scan mode for an ongoing decode
     * session.  Returns 
     * 
     * @param vd
     *            - the video device representing the decode session.
     * @return Returns the input video scan mode: one of
     *                org.ocap.media.VideoFormatControl#SCANMODE_UNKNOWN,
     *                org.ocap.media.VideoFormatControl#SCANMODE_INTERLACED,
     *             or org.ocap.media.VideoFormatControl#SCANMODE_PROGRESSIVE.

     * @see org.ocap.media.VideoFormatControl#getDecoderFormatConversion()
     */
    int getInputVideoScanMode(int vd);

    /**
     * Returns the 3D configuration info of the video.
     * See [OCCEP] for the 3D formatting data definition.
     * Returns <code>null</code> if no 3D formatting data is present
     * (e.g., in the case of 2D video).  Note: Rapid changes in
     * 3D signaling may cause the returned S3DConfiguration object
     * to be stale as soon as this method completes.
     *
     *
     * @return The signaled 3D formatting data, or <code>null</code> if no
     * 3D formatting data is present.
     */
    S3DConfiguration getS3DConfiguration(int vd);

    /**
     * Get the decoder format conversion (DFC) that is dictated by the platform
     * for an ongoing decode session. This may differ from the DFC currently set
     * by the application.
     * 
     * @param vd
     *            - the video device representing the decode session.
     * @return Returns the current decoder format conversion in use by the
     *         session.
     * @see org.ocap.media.VideoFormatControl#getDecoderFormatConversion()
     */
    int getPlatformDFC(int vd);

    /**
     * Check whether a decoder format conversion (DFC) is valid for a decode
     * session.
     * 
     * @param vd
     *            - the video device representing the decode session.
     * @param dfc
     *            - the DFC to check.
     * @return Checks whether the current DFC is valid.
     */
    boolean checkDFC(int vd, int dfc);

    /**
     * Set the decoder format conversion (DFC) to use for the decode session
     * represented by the video device.
     * 
     * @param vd
     *            - the video device representing the decode session.
     * @param dfc
     *            - the DFC to assign.
     */
    void setDFC(int vd, int dfc);

    /**
     * Tests whether the decoder supports clipping.
     * 
     * @param vd
     *            - the video device.
     * @return true if and only if the decoder supports clipping.
     */
    boolean supportsClipping(int vd);

    /**
     * Returns positioning capability.
     * 
     * @param vd
     *            - the video device.
     * @return positioning capability.
     */
    public byte getPositioningCapability(int vd);

    /**
     * Get the scaling capabilities for a video device.
     * 
     * @param vd
     *            - the video device.
     * @return a {@link ScalingCaps} object that defines the scaling
     *         capabilities of the specified video device.
     */
    public ScalingCaps getScalingCaps(int vd);

    /**
     * Mute or unmute the audio of a decode session
     *
     * @param sessionHandle the session
     * @param mute new mute state
     */
    void setMute(int sessionHandle, boolean mute);

    /**
     * Update the audio gain value as described by {@link GainControl#setDB}
     *
     * @param sessionHandle the session
     * @param gain new audio gain value in decibels
     * @return actual gain
     */
    float setGain(int sessionHandle, float gain);
    
    /**
     * Creates a key event which appears to have been generated by the platform.
     * 
     * @param type  key event type such as KEY_TYPED, KEY_PRESSED, etc.
     * @param code  key event code such as VK_1, VK_ENTER, etc.
     */
    public void generatePlatformKeyEvent(int type, int code);


    /**
     * Set current CCI
     * @param sessionHandle
     * @param cci the CCI for the decode session
     */
    void setCCI(int sessionHandle, byte cci);
}
