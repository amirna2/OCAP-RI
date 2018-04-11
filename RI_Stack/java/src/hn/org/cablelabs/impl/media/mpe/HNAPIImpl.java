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

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.HNAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.presentation.S3DConfigurationImpl;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNHttpHeaderAVStreamParameters;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackCopyControlInfo;
import org.cablelabs.impl.media.streaming.session.data.HNPlaybackParams;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamParams;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.ocap.media.S3DConfiguration;

public abstract class HNAPIImpl implements HNAPIManager
{
    private static final Logger log = Logger.getLogger(HNAPIImpl.class);

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        // Initialize the base JNI
        jniInit();
        
        // If dvr extension is present, initialize dvr dependent jni
        if (System.getProperty("ocap.api.option.dvr") != null)
        {
            jniInitDVR();
        }
    }

    /** Initialize Java class IDs in the native layer. */
    private static native void jniInit();

    /** Initialize Java class IDs in the native layer which have DVR dependencies. */
    private static native void jniInitDVR();

    /*************************************************************************/
    /***                                                                   ***/
    /***                       Shared player/server                        ***/
    /***                                                                   ***/
    /************************************************************************ */

    public static native void nativeStreamClose(
            int nativeStreamSession);

    public static native int nativeStreamOpen(
            EDListener edListener, HNStreamParams streamParams);

    public static native Playback nativePlaybackStart(
            int nativeStreamSession, HNPlaybackParams playbackParams,
            float rate, boolean blocked, boolean mute, float gain);

    public static native void nativePlaybackStop(
            int nativeStreamSession, int holdFrameMode);

    public static native String nativeGetMacAddress(
            String displayName);

    public static native int nativeGetNetworkInterfaceType(
            String displayName);

    public static native LPEWakeUp nativeGetLPEWakeUpVariables(
            String interfaceName);
    
    public static native String nativeGetLPENetworkInterfaceMode(
            String interfaceName);

    public static native String[] nativePing( 
            int testID, String host, int reps, int timeout, int blocksize, int dscp);

    public static native String[] nativeTraceroute( 
            int testID, String host, int hops, int timeout, int blocksize, int dscp);

    public static native String[] nativeNSLookup( 
            int testID, String host, String server, int timeout);

    public static native void nativeCancelTest(
            int testID); 

    public static native boolean nativeSetLinkLocalAddress(String interfaceName);

    /*************************************************************************/
    /***                                                                   ***/
    /***                            Server only                            ***/
    /***                                                                   ***/
    /************************************************************************ */

    public static native long nativeServerUpdateEndPosition(
            int nativePlaybackSession, long endBytePosition);
    
    public static native long nativeServerGetNetworkContentItemSize(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType, NativeContentTransformation transformation);
    
    public static native long nativeServerGetNetworkBytePosition(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType, NativeContentTransformation transformation,
            long localBytePosition);
    
    public static native long nativeServerGetNetworkBytePositionForMediaTimeNS(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType, NativeContentTransformation transformation,
            long mediaTimeNS);
    
    public static native String[] nativeServerGetDLNAProfileIds(
            int contentLocationType, HNStreamContentDescription contentDescription);

    public static native String[] nativeServerGetMimeTypes(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId);

    public static native String[] nativeServerGetPlayspeeds(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType, NativeContentTransformation transformation);
    
    public static native int nativeServerGetFrameTypesInTrickMode(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType, NativeContentTransformation transformation, float playspeedRate);
    
    public static native int nativeServerGetFrameRateInTrickMode(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType,  NativeContentTransformation transformation, float playspeedRate);
    
    public static native boolean nativeServerGetConnectionStallingFlag(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType, NativeContentTransformation transformation);
    
    public static native boolean nativeServerGetServerSidePacingRestampFlag(
            int contentLocationType, HNStreamContentDescription contentDescription,
            String profileId, String mimeType, NativeContentTransformation transformation);

    /*************************************************************************/
    /***                                                                   ***/
    /***                            Player only                            ***/
    /***                                                                   ***/
    /************************************************************************ */

    public static native void nativePlayerPlaybackPause(
            int nativePlaybackSession)
            throws HNStreamingException;

    public static native void nativePlayerPlaybackResume(
            int nativePlaybackSession)
            throws HNStreamingException;

    public static native int nativePlayerGetConnectionId(
            int nativeStreamSession);

    public static native void nativePlayerPlaybackChangePIDs(
            int nativePlaybackSession, HNHttpHeaderAVStreamParameters avsParams);

    // TODO missing method structure
    public static native void nativePlayerPlaybackUpdateCCI(
            int nativePlaybackSession, HNPlaybackCopyControlInfo[] copyControl);

    public static native Time nativePlayerPlaybackGetTime(
            int nativePlaybackSession);

    public static native float nativePlayerPlaybackGetRate(
            int nativePlaybackSession);
    
    public static native void nativePlayerPlaybackBlockPresentation(
            int nativePlaybackSession, boolean blockPresentation);

    public static native void nativePlayerPlaybackSetMute(
            int nativePlaybackSession, boolean mute);

    public static native float nativePlayerPlaybackSetGain(
            int nativePlaybackSession, float gain);

    // TODO clean this up
    private static native int nativePlayerPlaybackGetS3DConfiguration(
            int nativePlaybackSession, int[] specifierArray,
            byte[] payloadArray, int payloadArraySz);

    private static native int nativePlayerGetVideoScanMode(
            int nativePlaybackSession, int[] specifierArray);

    public static native String[] nativePlayerGetDLNAProfileIds();

    public static native String[] nativePlayerGetMimeTypes(
            String profileId);

    public static native String[] nativePlayerGetPlayspeeds(
            String profileId, String mimeType);





    public static int getInputVideoScanMode(int vd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getInputVideoScanMode(" + vd + ")");
        }

        int scanModeArray[] = new int[1];
        int err = nativePlayerGetVideoScanMode(vd, scanModeArray);
        if (err != 0)
        {
            throw new MPEMediaError(err, "getInputVideoScanMode");
        }

        // by convention with JNI, scanMode is element 0
        return scanModeArray[0];
    }

    public static S3DConfiguration getS3DConfiguration(int vd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getS3DConfiguration(" + vd + ")");
        }

        int[] specifierArray = new int[3];
        int payloadArraySz = 1000;

        byte[] payloadArray;

        while (true)
        {
            payloadArray = new byte[payloadArraySz];

            int err = nativePlayerPlaybackGetS3DConfiguration(vd, specifierArray, payloadArray, payloadArraySz);

            // if MPE_ENOMEM, payload size is too small, so increase it and redo call
            if (err == 2 /* MPE_ENOMEM */)
            {
                payloadArraySz = 2 * payloadArraySz;
                continue;
            }
            else if (err != 0)
            {
                throw new MPEMediaError(err, "getS3DConfiguration");
            }

            break;
        }

        int formatType = specifierArray[0];
        int payloadType = specifierArray[1];
        int payloadSz = specifierArray[2];

        byte[] payload = new byte[payloadSz];
        System.arraycopy(payloadArray, 0, payload, 0, payloadSz);

        if (log.isDebugEnabled())
        {
            log.debug("formatType = " + formatType);
        }
        if (log.isDebugEnabled())
        {
            log.debug("payloadType = " + payloadType);
        }
        if (log.isDebugEnabled())
        {
            log.debug("payloadSz = " + payloadSz);
        }
        if (log.isDebugEnabled())
        {
            log.debug("PAYLOAD:");
        }
            int numLines = payloadSz/16;
            int leftover = payloadSz%16;
            for (int i=0; i<numLines; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug(payload[i+0] + ", " + payload[i+1] + ", " + payload[i+2] + ", " + payload[i+3] + ", " + 
                payload[i+4] + ", " + payload[i+5] + ", " + payload[i+6] + ", " + payload[i+7] + ", " + 
                payload[i+8] + ", " + payload[i+9] + ", " + payload[i+10] + ", " + payload[i+11] + ", " + 
                payload[i+12] + ", " + payload[i+13] + ", " + payload[i+14] + ", " + payload[i+15]);
            }
        }

            if (leftover != 0)
            {
                String tmp = payload[numLines*16] + "";
                for (int i=numLines*16+1; i<payloadSz; i++)
                {
                    tmp += ", " + payload[i];
                }

            if (log.isDebugEnabled())
            {
                log.debug(tmp);
            }
        }
        
        if (formatType == 0)
        {
            return null;
        }
        else
        {
            return new S3DConfigurationImpl(payloadType, formatType, payload);
        }
    }
}
