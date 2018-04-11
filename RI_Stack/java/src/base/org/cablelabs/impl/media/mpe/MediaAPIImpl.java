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

import org.apache.log4j.Logger;
import org.cablelabs.impl.util.Arrays;
import org.havi.ui.HScreenRectangle;
import org.dvb.media.VideoFormatControl;

import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.presentation.S3DConfigurationImpl;

import org.ocap.media.S3DConfiguration;


/**
 * This class is used to interact with the native MPE Media API. An instance of
 * this class is created via the {@link org.cablelabs.impl.media.mpe.APIFactory}
 * .
 * 
 * @author schoonma
 */
public class MediaAPIImpl implements MediaAPIManager
{
    private static final Logger log = Logger.getLogger(MediaAPIImpl.class);

    private static MediaAPIImpl instance;

    private ScalingCaps scalingCaps;

    // Initialize class IDs used by JNI layer.
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        jniInit();
    }

    protected MediaAPIImpl()
    {
    }

    public static synchronized MediaAPIManager getInstance()
    {
        if (instance == null) instance = new MediaAPIImpl();

        return instance;
    }

    public void destroy()
    {
        clearInstance();
    }

    private static synchronized void clearInstance()
    {
        instance = null;
    }

    /**
     * @return Returns a String representing a Media decode event.
     */
    static public String eventToString(int event)
    {
        switch (event)
        {
            case Event.CONTENT_PRESENTING:
                return "MEDIA_CONTENT_PRESENTING";
            case Event.CONTENT_NOT_PRESENTING:
                return "MEDIA_CONTENT_NOT_PRESENTING";
            case Event.QUEUE_TERMINATED:
                return "ED_QUEUE_TERMINATED";
            case Event.STILL_FRAME_DECODED:
                return "MEDIA_STILL_FRAME_DECODED";
            case Event.FAILURE_UNKNOWN:
                return "MEDIA_FAILURE_UNKNOWN";
            case Event.STREAM_RETURNED:
                return "STREAM_RETURNED";
            case Event.STREAM_NO_DATA:
                return "STREAM_NO_DATA";
            case Event.STREAM_CA_DIALOG_PAYMENT:
                return "STREAM_CA_DIALOG_PAYMENT";
            case Event.STREAM_CA_DIALOG_TECHNICAL:
                return "STREAM_CA_DIALOG_TECHNICAL";
            case Event.STREAM_CA_DENIED_ENTITLEMENT:
                return "STREAM_CA_DENIED_ENTITLEMENT";
            case Event.STREAM_CA_DENIED_TECHNICAL:
                return "STREAM_CA_DENIED_TECHNICAL";
            case Event.ACTIVE_FORMAT_CHANGED:
                return "ACTIVE_FORMAT_CHANGED";
            case Event.ASPECT_RATIO_CHANGED:
                return "ASPECT_RATIO_CHANGED";
            case Event.DFC_CHANGED:
                return "DFC_CHANGED";
            case Event.S3D_FORMAT_CHANGED:
                return "S3D_FORMAT_CHANGED";

            default:
                return "UNKNOWN(" + event + ")";
        }
    }

    static public String handleToString(int handle)
    {
        return "0x" + Integer.toHexString(handle);
    }

    public String toString(int event)
    {
        return eventToString(event);
    }

    public synchronized int decodeBroadcast(final MediaDecodeParams params)
    {
        if (log.isInfoEnabled())
        {
            log.info("decodeBroadcast(params=" + params + ")");
        }

        int handle[] = new int[1];
        int err = nativeDecodeBroadcast(params, handle);
        if (err != 0)
            throw new MPEMediaError(err, "decode");
        int sessionHandle = handle[0];
        if (log.isDebugEnabled())
        {
            log.debug("decodeBroadcast() sessionHandle = " + handleToString(sessionHandle) + ", gain: " + params.getGain());
        }
        return sessionHandle;
    }

    public void blockPresentation(int sessionHandle, boolean block)
    {
        if (log.isDebugEnabled())
        {
            log.debug("blockPresentation(sessionHandle=" + handleToString(sessionHandle) + ", block=" + block + ")");
        }

        int err = nativeBlockPresentation(sessionHandle, block);
        if (err != 0)
            throw new MPEMediaError(err, "blockPresentation");
    }

    public synchronized void changePids(int sessionHandle, int pcrPid, int[] pids, short[] types)
    {
        if (log.isInfoEnabled())
        {
            log.info("changePids(pcrPid=" + pcrPid + " pids=" + Arrays.toString(pids) + " types="
                    + Arrays.toString(types) + ")");
        }

        int err = nativeChangePids(sessionHandle, pcrPid, pids, types);
        if (err != 0) throw new MPEMediaError(err, "changePids");
    }

    public synchronized void stopBroadcastDecode(int sessionHandle, boolean holdFrame)
    {
        if (log.isInfoEnabled())
        {
            log.info("stopBroadcastDecode(sessionHandle=" + handleToString(sessionHandle) + "), hold frame: " + holdFrame);
        }

        nativeStopBroadcastDecode(sessionHandle, holdFrame);
    }

    public synchronized int dripFeedStart(final MediaDripFeedParams params)
    {
        if (params == null)
            throw new IllegalArgumentException("null drip feed params");

        if (log.isDebugEnabled())
        {
            log.debug("dripFeedStart(vd=" + handleToString(params.videoHandle) + ")");
        }

        int dripFeedHandle[] = new int[1];
        int err = nativeDripFeedStart(params, dripFeedHandle);
        if (err != 0)
            throw new MPEMediaError(err, "dripFeedStart");

        return dripFeedHandle[0];
    }

    public synchronized void dripFeedRenderFrame(int dripFeedHandle, byte[] frameData)
    {
        if (log.isDebugEnabled())
        {
            log.debug("dripFeedRenderFrame(dripFeedHandle=" + handleToString(dripFeedHandle) + ")");
        }

        int err = nativeDripFeedRenderFrame(dripFeedHandle, frameData);
        if (err != 0)
            throw new MPEMediaError(err, "dripFeedRenderFrame");

        return;
    }

    public synchronized void dripFeedStop(int dripFeedHandle)
    {
        if (log.isDebugEnabled())
        {
            log.debug("dripFeedStop(dripFeedHandle=" + handleToString(dripFeedHandle) + ")");
        }

        int err = nativeDripFeedStop(dripFeedHandle);
        if (err != 0)
            throw new MPEMediaError(err, "dripFeedStop");

        return;
    }

    public synchronized ScalingBounds checkBounds(int vd, ScalingBounds desired)
    {
        if (log.isDebugEnabled())
        {
            log.debug("checkBounds(vd=" + handleToString(vd) + ", desired=" + desired + ")");
        }

        if (desired == null)
            throw new IllegalArgumentException("null bounds");

        ScalingBounds closest = new ScalingBounds();
        int err = nativeCheckBounds(vd, desired.src, desired.dst, closest.src, closest.dst);
        if (err != 0)
            throw new MPEMediaError(err, "checkBounds");

        return closest;
    }

    public synchronized boolean setBounds(int vd, ScalingBounds size)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setBounds(vd=" + handleToString(vd) + ", size=" + size + ")");
        }

        if (size == null)
            throw new IllegalArgumentException("null size");

        int err = nativeSetBounds(vd, size.src, size.dst);
        return err == 0;
    }

    public synchronized ScalingBounds getBounds(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getBounds(vd=" + handleToString(vd) + ")");
        }

        ScalingBounds size = new ScalingBounds();
        int err = nativeGetBounds(vd, size.src, size.dst);
        if (err != 0)
            throw new MPEMediaError(err, "getSize");
        return size;
    }

    public synchronized Dimension getVideoInputSize(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getVideoInputSize(vd=" + handleToString(vd) + ")");
        }

        Dimension size = new Dimension();
        int err = nativeGetVideoInputSize(vd, size);
        if (err != 0)
            throw new MPEMediaError(err, "getVideoInputSize");
        return size;
    }

    public synchronized void freeze(int vd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("freeze(vd=" + handleToString(vd) + ")");
        }

        int err = nativeFreeze(vd);
        if (err != 0)
            throw new MPEMediaError(err, "freeze");
    }

    public synchronized void resume(int vd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("resume(vd=" + handleToString(vd) + ")");
        }

        int err = nativeResume(vd);
        if (err != 0)
            throw new MPEMediaError(err, "resume");
    }

    public synchronized void swapDecoders(int vd1, int vd2, boolean audioUse)
    {
        if (log.isDebugEnabled())
        {
            log.debug("swapDecoders(vd1=" + handleToString(vd1) + ", vd2=" + handleToString(vd2) + ", audioUse="
                    + audioUse + ")");
        }

        int err = nativeSwapDecoders(vd1, vd2, audioUse);
        if (err != 0)
            throw new MPEMediaError(err, "swapDecoders");
    }

    public synchronized boolean supportsComponentVideo(int vd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("supportsScaling(vd=" + handleToString(vd) + ")");
        }

        boolean result[] = new boolean[1];
        int err = nativeSupportsComponentVideo(vd, result);
        if (err != 0)
            throw new MPEMediaError(err, "supportsVideoComponent");
        return result[0];
    }

    public int getCCState()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getCCState()");
        }

        int result[] = new int[1];
        int err = nativeGetCCState(result);
        if (err != 0)
            throw new MPEMediaError(err, "getCCState");
        return result[0];
    }

    public int getAspectRatio(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getAspectRatio(" + vd + ")");
        }

        int ratio[] = new int[1];
        int err = nativeGetAspectRatio(vd, ratio);
        if (err != 0)
            throw new MPEMediaError(err, "getAspectRatio");
        return ratio[0];
    }

    public int getActiveFormatDefinition(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getActiveFormatDefinition(" + vd + ")");
        }

        int afd[] = new int[1];
        int err = nativeGetAFD(vd, afd);
        if (err != 0)
            throw new MPEMediaError(err, "getActiveFormatDefinition");
        return afd[0];
    }

    public boolean isPlatformDFC(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("isPlatform(" + vd + ")");
        }

        int dfc[] = new int[2];
        int err = nativeGetDFC(vd, dfc);
        if (err != 0)
            throw new MPEMediaError(err, "isPlatformDFC");

        // by convention with JNI, applicationDfc is element 0 and platformDfc
        // is element 1
        int applicationDfc = dfc[0];

        return (applicationDfc == VideoFormatControl.DFC_PLATFORM);
    }

    /* TODO, TODO_DS: javadoc */
    public int[] getSupportedDFCs(int vd)
    {
        int count[] = { 0 };

        int err = nativeGetSupportedDFCCount(vd, count);

        if (err != 0) throw new MPEMediaError(err, "getSupportedDFCs -- jniGetSupportedDFCCount");

        int numDfcs = count[0];

        int[] dfcArray = new int[numDfcs];

        if (numDfcs > 0)
        {
            err = nativeGetSupportedDFCs(vd, dfcArray);
            if (err != 0) throw new MPEMediaError(err, "getSupportedDFCs -- jniGetSupportedDFCs");
        }

        return dfcArray;
    }

    public S3DConfiguration getS3DConfiguration(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getS3DConfiguration(" + vd + ")");
        }

        int[] specifierArray = new int[3];
        int payloadArraySz = 1000;

        byte[] payloadArray;

        while (true)
        {
            payloadArray = new byte[payloadArraySz];

            int err = nativeGetS3DConfiguration(vd, specifierArray, payloadArray, payloadArraySz);

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

        if (log.isTraceEnabled())
        {
            log.trace("formatType = " + formatType);
        }
        if (log.isTraceEnabled())
        {
            log.trace("payloadType = " + payloadType);
        }
        if (log.isTraceEnabled())
        {
            log.trace("payloadSz = " + payloadSz);
        }
        if (log.isTraceEnabled())
        {
            log.trace("PAYLOAD:");
        }
            int numLines = payloadSz/16;
            int leftover = payloadSz%16;
            for (int i=0; i<numLines; i++)
            {
            if (log.isTraceEnabled())
            {
                log.trace(payload[i+0] + ", " + payload[i+1] + ", " + payload[i+2] + ", " + payload[i+3] + ", " +
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

            if (log.isTraceEnabled())
            {
                log.trace(tmp);
            }
        }
        
        if (formatType == 0)  // 2D
        {
            return null;
        }
        else
        {
            return new S3DConfigurationImpl(payloadType, formatType, payload);
        }
    }

    /*
     * Returns the input video scan mode via the 
     * call mpe_mediaGetInputVideoScanMode
     *
     * @see org.cablelabs.impl.media.mpe.MediaAPI#getInputVideoScanMode(int)
     */
    public int getInputVideoScanMode(int vd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getInputVideoScanMode(" + vd + ")");
        }

        int scanModeArray[] = new int[1];
        int err = nativeGetVideoScanMode(vd, scanModeArray);
        if (err != 0)
        {
            throw new MPEMediaError(err, "getInputVideoScanMode");
        }

        // by convention with JNI, scanMode is element 0
        return scanModeArray[0];
    }

    /*
     * The native getDFC routine returns two DFC modes (one set by the
     * application and one used by the platform). See mpeos_dispGetDfc.
     *
     * @see org.cablelabs.impl.media.mpe.MediaAPI#getDFC(int)
     */
    public int getDFC(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getDFC(" + vd + ")");
        }

        int dfc[] = new int[2];
        int err = nativeGetDFC(vd, dfc);
        if (err != 0)
            throw new MPEMediaError(err, "getDFC");

        // by convention with JNI, applicationDfc is element 0 and platformDfc
        // is element 1
        int applicationDfc = dfc[0];
        int platformDfc = dfc[1];

        // never return DFC_PLATFORM from this routine. DFC_PLATFORM indicates
        // the application has placed DFC processing under control of the
        // system.
        // Return the platformDFC in this case.
        return (applicationDfc == VideoFormatControl.DFC_PLATFORM) ? platformDfc : applicationDfc;
    }

    /*
     * The native getDFC routine returns two DFC modes (one set by the
     * application and one used by the platform). See mpeos_dispGetDfc.
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPI#getDFC(int)
     */
    public int getPlatformDFC(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getPlatformDFC(" + vd + ")");
        }

        int dfc[] = new int[2];
        int err = nativeGetDFC(vd, dfc);
        if (err != 0)
            throw new MPEMediaError(err, "getDFC");

        // by convention with JNI, applicationDfc is element 0 and platformDfc
        // is element 1
        int platformDfc = dfc[1];

        return platformDfc;
    }

    public boolean checkDFC(int vd, int dfc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("checkDFC(" + vd + ", " + dfc + ")");
        }

        int err = nativeCheckDFC(vd, dfc);
        if (err != 0)
            return false;
        return true;
    }

    public void setDFC(int vd, int dfc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setDFC(" + vd + ", " + dfc + ")");
        }

        int err = nativeSetDFC(vd, dfc);
        if (err != 0)
            throw new MPEMediaError(err, "setDFC(" + vd + ", " + dfc + ")");
    }

    public void setCCState(int ccState)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setCCState(" + ccState + ")");
        }

        int err = nativeSetCCState(ccState);
        if (err != 0)
            throw new MPEMediaError(err, "setCCState(" + ccState + ")");
    }

    public void setCCServiceNumbers(int analogSvcNo, int digitalSvcNo)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setCCServiceNumbers(" + analogSvcNo + ", " + digitalSvcNo + ")");
        }

        int err = nativeSetCCServiceNumbers(analogSvcNo, digitalSvcNo);
        if (err != 0)
            throw new MPEMediaError(err, "setCCServiceNumbers(" + analogSvcNo + ", " + digitalSvcNo + ")");
    }

    public int[] getCCSupportedServiceNumbers()
    {
        int[] result = nativeGetCCSupportedServiceNumbers();
        if (log.isTraceEnabled())
        {
            log.trace("getCCSupportedServiceNumbers - returning: " + Arrays.toString(result));
        }

        return result;
    }

    public synchronized boolean supportsClipping(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("supportsClipping(vd=" + handleToString(vd) + ")");
        }

        boolean result[] = new boolean[1];
        int err = nativeSupportsClipping(vd, result);
        if (err != 0)
            throw new MPEMediaError(err, "supportsClipping");
        return result[0];
    }

    public synchronized byte getPositioningCapability(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getPositioningCapability(vd=" + handleToString(vd) + ")");
        }

        byte result[] = new byte[1];
        int err = nativeGetPositioningCapability(vd, result);
        if (err != 0)
            throw new MPEMediaError(err, "supportsClipping");
        return result[0];
    }

    public ScalingCaps getScalingCaps(int vd)
    {
        if (scalingCaps == null)
        {
            scalingCaps = new ScalingCaps();
        }

        int error = nativeGetScalingCaps(vd, scalingCaps);
        if (error != 0)
        {
            throw new MPEMediaError(error, "nativeGetScalingCaps");
        }
        return scalingCaps;
    }

    public void setMute(int sessionHandle, boolean mute)
    {
        int err = jniSetMute(sessionHandle, mute);
        if (err != 0)
        {
            throw new MPEMediaError(err, "setMute: " + mute);
        }
    }

    public float setGain(int sessionHandle, float gain)
    {
        float[] arg = new float[]{gain};
        int err = jniSetGain(sessionHandle, arg);
        if (err != 0)
        {
            throw new MPEMediaError(err, "setGain: " + gain);
        }
        return arg[0];
    }

    public void generatePlatformKeyEvent(int type, int code)
    {
        int error = nativeGeneratePlatformKeyEvent(type, code);
        if (error != 0)
        {
            throw new MPEMediaError(error, "nativeGeneratePlatformKeyEvent");
        }
    }

    public void setCCI(int sessionHandle, byte cci)
    {
        int err = jniSetCCI(sessionHandle, cci);
        if (err != 0)
        {
            throw new MPEMediaError(err, "setCCI: " + cci);
        }
    }

    /*
     * 
     * Native Wrapper Methods These are provided so that MediaAPIImpl can be
     * subclassed to bypass the native calls (for testing purposes).
     */

    /** Call mpe_mediaCheckBounds(). */
    protected int nativeCheckBounds(int decoder, HScreenRectangle desiredSrc, HScreenRectangle desiredDst,
            HScreenRectangle closestSrc, HScreenRectangle closestDst)
    {
        return jniCheckBounds(decoder, desiredSrc, desiredDst, closestSrc, closestDst);
    }

    protected int nativeSetBounds(int decoder, HScreenRectangle src, HScreenRectangle dst)
    {
        return jniSetBounds(decoder, src, dst);
    }

    protected int nativeGetBounds(int decoder, HScreenRectangle src, HScreenRectangle dst)
    {
        return jniGetBounds(decoder, src, dst);
    }

    protected int nativeGetVideoInputSize(int decoder, Dimension size)
    {
        return jniGetVideoInputSize(decoder, size);
    }

    protected int nativeSwapDecoders(int decoder1, int decoder2, boolean audioUse)
    {
        return jniSwapDecoders(decoder1, decoder2, audioUse);
    }

    protected int nativeDecodeBroadcast(final MediaDecodeParams params, int[] sessionHandle)
    {
        return jniDecode(params, sessionHandle);
    }

    protected int nativeBlockPresentation(int sessionHandle, boolean block)
    {
        return jniBlockPresentation(sessionHandle, block);
    }

    protected int nativeChangePids(int sessionHandle, int pcrPid, int[] pids, short[] types)
    {
        return jniChangePids(sessionHandle, pcrPid, pids, types);
    }

    protected int nativeStopBroadcastDecode(int sessionHandle, boolean holdFrame)
    {
        return jniStop(sessionHandle, holdFrame);
    }

    protected int nativeDripFeedStart(final MediaDripFeedParams params, int[] dripFeedHandle)
    {
        return jniDripFeedStart(params, dripFeedHandle);
    }

    protected int nativeDripFeedRenderFrame(int dripFeedHandle, byte[] frameData)
    {
        return jniDripFeedRenderFrame(dripFeedHandle, frameData);
    }

    protected int nativeDripFeedStop(int dripFeedHandle)
    {
        return jniDripFeedStop(dripFeedHandle);
    }

    protected int nativeFreeze(int decoder)
    {
        return jniFreeze(decoder);
    }

    protected int nativeResume(int decoder)
    {
        return jniResume(decoder);
    }

    protected int nativeSupportsComponentVideo(int decoder, boolean result[])
    {
        return jniSupportsComponentVideo(decoder, result);
    }

    protected int nativeGetCCState(int[] state)
    {
        return jniGetCCState(state);
    }

    protected int nativeSetCCState(int state)
    {
        return jniSetCCState(state);
    }

    protected int nativeSetCCServiceNumbers(int analog, int digital)
    {
        return jniSetCCServiceNumbers(analog, digital);
    }

    protected int[] nativeGetCCSupportedServiceNumbers()
    {
        return jniGetSupportedServiceNumbers();
    }

    protected int nativeGetAspectRatio(int vd, int[] ratio)
    {
        return jniGetAspectRatio(vd, ratio);
    }

    protected int nativeGetAFD(int vd, int[] afd)
    {
        return jniGetAFD(vd, afd);
    }

    protected int nativeGetSupportedDFCs(int vd, int[] dfcArray)
    {
        return jniGetSupportedDFCs(vd, dfcArray);
    }

    protected int nativeGetSupportedDFCCount(int vd, int[] count)
    {
        return jniGetSupportedDFCCount(vd, count);
    }

    protected int nativeGetDFC(int vd, int[] dfcArray)
    {
        return jniGetDFC(vd, dfcArray);
    }

    protected int nativeGetVideoScanMode(int vd, int[] scanModeArray)
    {
        return jniGetVideoScanMode(vd, scanModeArray);
    }

    protected int nativeGetS3DConfiguration(int vd, int[] specifierArray, byte[] payloadArray, int payloadArraySz)
    {
        return jniGetS3DConfiguration(vd, specifierArray, payloadArray, payloadArraySz);
    }

    protected int nativeCheckDFC(int vd, int dfc)
    {
        return jniCheckDFC(vd, dfc);
    }

    protected int nativeSetDFC(int vd, int dfc)
    {
        return jniSetDFC(vd, dfc);
    }

    protected int nativeSupportsClipping(int vd, boolean result[])
    {
        return jniSupportsClipping(vd, result);
    }

    protected int nativeGetPositioningCapability(int vd, byte result[])
    {
        return jniGetPositioningCapability(vd, result);
    }

    protected int nativeGetScalingCaps(int vd, ScalingCaps result)
    {
        return jniGetScalingCaps(vd, result);
    }

    protected int nativeGeneratePlatformKeyEvent(int type, int code)
    {
        return jniGeneratePlatformKeyEvent(type, code);
    }


    /*
     * Native Methods All of the native methods below (except jniInit()) call an
     * MPE method (as indicated by their documentation) and return the resulting
     * MPE return value.
     */

    /** Initialize Java class IDs in the native layer. */
    native private static void jniInit();

    /** Call mpe_mediaCheckBounds(). */
    native private static int jniCheckBounds(int decoder, HScreenRectangle desiredSrc, HScreenRectangle desiredDst,
            HScreenRectangle closestSrc, HScreenRectangle closestDst);

    /** Call mpe_mediaSetBounds(). */
    native private static int jniSetBounds(int decoder, HScreenRectangle src, HScreenRectangle dst);

    /** Call mpe_mediaGetBounds(). */
    native private static int jniGetBounds(int decoder, HScreenRectangle src, HScreenRectangle dst);

    /** Call ??? */
    native private static int jniGetVideoInputSize(int decoderHandle, Dimension source);

    /** Call mpe_mediaSwapDecoders(). */
    native private static int jniSwapDecoders(int decoder1, int decoder2, boolean audioUse);

    /** Call mpe_mediaDecode(). */
    native private static int jniDecode(final MediaDecodeParams params, int[] sessionHandle);

    /** Call mpe_mediaChangePids(). */
    native private static int jniChangePids(int sessionHandle, int pcrPid, int[] pids, short[] types);

    /** Call mpe_mediaDripFeedStart(). */
    native private static int jniDripFeedStart(final MediaDripFeedParams params, int[] dripFeedHandle);

    /** Call mpe_mediaDripFeedRenderFrame(). */
    native private static int jniDripFeedRenderFrame(int dripFeedHandle, byte[] frameData);

    /** Call mpe_mediaDripFeedStop(). */
    native private static int jniDripFeedStop(int dripFeedHandle);

    /** Call mpe_mediaBlockPresentation(). */
    native private static int jniBlockPresentation(int sessionHandle, boolean block);

    /** Call mpe_mediaStop(). */
    native private static int jniStop(int decoder, boolean holdFrame);

    /** Call mpe_mediaFreeze(). */
    native private static int jniFreeze(int decoder);

    /** Call mpe_mediaResume(). */
    native private static int jniResume(int decoder);

    /** Call mpe_mediaGetScaling(). */
    native private static int jniSupportsComponentVideo(int decoder, boolean result[]);

    /** Call mpe_mediaGetAspectRatio(). */
    native private static int jniGetAspectRatio(int decoder, int[] ratio);

    /** Call mpe_mediaGetAFD(). */
    native private static int jniGetAFD(int decoder, int[] afd);

    /** Call mpe_dispGetDFC(). */
    native private static int jniGetDFC(int decoder, int[] dfcArray);

    /** Call mpe_mediaGetInputVideoScanMode(). */
    native private static int jniGetVideoScanMode(int decoder, int[] dfcArray);

    private static native int jniGetS3DConfiguration(int decoder, int[] specifierArray, byte[] payloadArray, int payloadArraySz);

    /** Call mpe_GetSupportedDFCs(). */
    native private static int jniGetSupportedDFCs(int decoder, int[] dfcArray);

    /** Call jniGetSupportedDFCCount. */
    native private static int jniGetSupportedDFCCount(int decoder, int[] count);

    /** Call mpe_dispCheckDFC(). */
    native private static int jniCheckDFC(int decoder, int dfc);

    /** Call mpe_dispSetDFC(). */
    native private static int jniSetDFC(int decoder, int dfc);

    /** Call mpe_ccGetClosedCaptioning() */
    native private static int jniGetCCState(int[] state);

    /** Call mpe_ccSetClosedCaptioning() */
    native private static int jniSetCCState(int state);

    /** Call mpe_ccSetAnalogServices() and mpe_ccSetDigitalServices() */
    native private static int jniSetCCServiceNumbers(int analog, int digital);

    /** Call mpe_ccGetSupportedServiceNumbers() */
    native private static int[] jniGetSupportedServiceNumbers();

    /** Call mpe_mediaGetScaling() */
    native private static int jniSupportsClipping(int decoder, boolean result[]);

    /** Call mpe_mediaGetScaling() */
    native private static int jniGetPositioningCapability(int decoder, byte result[]);

    /** Call mpe_mediaGetScaling() */
    native private static int jniGetScalingCaps(int decoder, ScalingCaps results);

    /** Call mpe_mediaSetMute() */
    private static native int jniSetMute(int sessionHandle, boolean mute);

    /** Call mpe_mediaSetGain() */
    private static native int jniSetGain(int sessionHandle, float[] arg);

    /** Call mpe_gfxGenerateKeyEvent() */
    private static native int jniGeneratePlatformKeyEvent(int type, int code);

    /** Call mpe_mediaSetCCI() */
    private static native int jniSetCCI(int sessionHandle, byte cci);
}
