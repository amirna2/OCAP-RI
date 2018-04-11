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
import java.awt.Rectangle;

import org.dvb.media.VideoFormatControl;
import org.dvb.media.VideoPresentationControl;
import org.havi.ui.HScreenRectangle;
import org.ocap.media.S3DConfiguration;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.player.VideoDevice;

/**
 * CannedMediaAPI
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedMediaAPI implements MediaAPIManager
{
    protected DecodingInfo info1;

    protected DecodingInfo info2;

    private boolean freezeException;

    private int[] decodePIDs;

    private boolean checkValidVideoDecoder;

    private boolean stallDecodeFirstFrameEvent;

    private byte[] lastDripFeedFrame;

    // default to true since that will be the common case
    private boolean deliverContentPresentingEvent = true;

    private int decodeBroadcastEvent = Event.CONTENT_PRESENTING;

    private int decodeBroadcastEvent2 = Event.CONTENT_PRESENTING;

    private int oldEvent = Event.CONTENT_PRESENTING;

    /** indicates whether presentation is currently blocked */
    private boolean blocked;

    public static final Rectangle small = new Rectangle(640, 480);

    public static final Rectangle medium = new Rectangle(1280, 720);

    public static final Rectangle large = new Rectangle(1920, 1080);

    public static final ScalingBounds dfltSB = new ScalingBounds();

    public static CannedMediaAPI instance;

    public CannedMediaAPI()
    {
        info1 = new DecodingInfo(1);
        info2 = new DecodingInfo(2);
    }

    public static MediaAPIManager getInstance()
    {
        if (instance == null) instance = new CannedMediaAPI();
        return instance;
    }

    public void destroy()
    {
        instance = null;
    }

    public int decodeBroadcast(final MediaDecodeParams params)
    {
        DecodingInfo info = getDecodingInfo(params.getVideoHandle());
        info.setDecodingValues(params.getListener(), params.getTunerHandle(), params.getStreamPids(),
                params.getStreamTypes(), params.isBlocked());
        if (deliverContentPresentingEvent)
        {
            if (stallDecodeFirstFrameEvent)
            {
                synchronized (this)
                {
                    try
                    {
                        wait(10000);
                    }
                    catch (InterruptedException exc)
                    {
                        // just continue on and deliver the event
                    }
                    stallDecodeFirstFrameEvent = false;
                }
            }

            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            ccm.getSystemContext().runInContext(new Runnable()
            {
                public void run()
                {
                    params.getListener().asyncEvent(decodeBroadcastEvent, 0, 0);
                    decodeBroadcastEvent = decodeBroadcastEvent2;
                }
            });
        }
        else
        {
            deliverContentPresentingEvent = true;
        }
        //
        // keep track of the pids we were asked to decode
        //
        decodePIDs = params.getStreamPids();
        //
        // keep track of blocking
        //
        blocked = params.isBlocked();
        return params.getVideoHandle();
    }

    public void cannedSetStallDecodeFirstFrameEvent(boolean b)
    {
        stallDecodeFirstFrameEvent = b;
    }

    public int cannedSetDecodeBroadcastEvent(int eventToSend, int eventToSend2)
    {
        oldEvent = decodeBroadcastEvent;
        decodeBroadcastEvent = eventToSend;
        decodeBroadcastEvent2 = eventToSend2;
        return oldEvent;
    }

    public void cannedSetDeliverContentPresentingEvent(boolean b)
    {
        deliverContentPresentingEvent = b;
    }

    public boolean cannedGetDeliverContentPresentingEvent()
    {
        return deliverContentPresentingEvent;
    }

    public void stopBroadcastDecode(int vd, boolean holdFrame)
    {
        DecodingInfo info = getDecodingInfo(vd);

        if (!info.decoding) return;

        final EDListener listener = info.listener;
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                listener.asyncEvent(0x8, 0, 0); // Shutdown queue
            }
        });
        info.reset();
    }

    private boolean withinHScreenRect(HScreenRectangle check, HScreenRectangle bounds)
    {
        float x = check.x;
        float y = check.y;
        float w = check.width;
        float h = check.height;
        float bx = bounds.x;
        float by = bounds.y;
        float bw = bounds.width;
        float bh = bounds.height;
        return w >= 0 && h >= 0 && x >= bx && y >= by && x + w <= bx + bw && y + h <= by + bh;

    }

    private boolean withinScalingBounds(ScalingBounds check, ScalingBounds bounds)
    {
        return withinHScreenRect(check.src, bounds.src) && withinHScreenRect(check.dst, bounds.dst);
    }

    /**
     * Start a drip feed session.
     * 
     * @param params
     *            - the {@link MediaDripFeedParams} to use.
     * @return Returns a handle to a drip feed decode session
     */
    public int dripFeedStart(final MediaDripFeedParams params)
    {
        DecodingInfo info = getDecodingInfo(params.videoHandle);
        info.setDecodingValues(params.listener, 0, new int[0], new short[0], false);

        return params.videoHandle;
    }

    /**
     * Submit a frame of data to a drip feed session.
     * 
     * @param dripFeedHandle
     *            - A drip feed session returned by {@link dripFeedStart}.
     * @param byte - An array of bytes containing an MPEG2 I,P-frame to be
     *        decoded
     */
    public void dripFeedRenderFrame(int dripFeedHandle, byte[] frameData)
    {
        lastDripFeedFrame = frameData;
        getDecodingInfo(dripFeedHandle).listener.asyncEvent(MediaAPI.Event.STILL_FRAME_DECODED, 0, 0);
        return;
    }

    /**
     * Stop a drip feed session.
     * 
     * @param dripFeedHandle
     *            - A drip feed session returned by {@link dripFeedStart}.
     */
    public void dripFeedStop(int dripFeedHandle)
    {
        stopBroadcastDecode(dripFeedHandle, false);
    }

    public ScalingBounds checkBounds(int vd, ScalingBounds desired)
    {
        return withinScalingBounds(desired, dfltSB) ? desired : dfltSB;
    }

    public boolean setBounds(int vd, ScalingBounds size)
    {
        if (size == null) throw new IllegalArgumentException("null size");
        DecodingInfo info = getDecodingInfo(vd);
        info.size = new ScalingBounds(size);
        return true;
    }

    public ScalingBounds getBounds(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return new ScalingBounds(info.size);
    }

    public void blockPresentation(int sessionHandle, boolean block)
    {
        DecodingInfo info = getDecodingInfo(sessionHandle);
        info.blocked = block;
    }

    public void changePids(int sessionHandle, int pcrPid, int[] pids, short[] types)
    {
        //
        // TODO: This function is completely untested. It is put in place as
        // part of
        // bug 5116 to support the new changePids api in mpe
        //

        //
        // Update decoding info
        //
        DecodingInfo info = getDecodingInfo(sessionHandle);
        info.setDecodingValues(info.listener, info.tuner, pids, types, info.blocked);

        //
        // keep track of the pids we were asked to decode
        //
        decodePIDs = pids;
    }

    public void freeze(int vd)
    {
        if (freezeException) throw new MPEMediaError(7);
        DecodingInfo info = getDecodingInfo(vd);
        info.frozen = true;
    }

    public void resume(int vd)
    {
        if (freezeException)
        {
            freezeException = false;
            throw new MPEMediaError(8);
        }
        DecodingInfo info = getDecodingInfo(vd);
        info.frozen = false;
    }

    public void swapDecoders(int vd1, int vd2, boolean audioUse)
    {
        if (vd1 == vd2) return;

        DecodingInfo infoA = getDecodingInfo(vd1);
        DecodingInfo infoB = getDecodingInfo(vd2);

        DecodingInfo temp = infoA;
        infoA = infoB;
        infoB = temp;

        temp.decoder = infoA.decoder;
        infoA.decoder = infoB.decoder;
        infoB.decoder = temp.decoder;
    }

    public boolean supportsComponentVideo(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return info.scaling;
    }

    public boolean cannedIsFrozen(VideoDevice vd)
    {
        return getDecodingInfo(vd.getHandle()).frozen;
    }

    public boolean cannedIsBlocked(VideoDevice vd)
    {
        return getDecodingInfo(vd.getHandle()).blocked;
    }

    public void cannedCauseFreezeException(boolean fe)
    {
        this.freezeException = fe;
    }

    public int[] cannedGetDecodeRecordingPIDs()
    {
        return decodePIDs;
    }

    public void generatePlatformKeyEvent(int type, int code)
    {
        // No junit tests
    }
    
    //    
    // private int calcDiff(ScalingBounds sb1, ScalingBounds sb2)
    // {
    // int diffx = Math.abs(sb1.src.x - sb2.src.x) + Math.abs(sb1.dst.x -
    // sb2.dst.x);
    // int diffy = Math.abs(sb1.src.y - sb2.src.y) + Math.abs(sb1.dst.y -
    // sb2.dst.y);
    // int diffwidth = Math.abs(sb1.src.width - sb2.src.width) +
    // Math.abs(sb1.dst.width - sb2.dst.width);
    // int diffheight = Math.abs(sb1.src.height - sb2.src.height) +
    // Math.abs(sb1.dst.height - sb2.dst.height);
    // return diffx + diffy + diffwidth + diffheight;
    // }

    private DecodingInfo getDecodingInfo(int vd)
    {
        switch (vd)
        {
            case 1:
                return info1;
            case 2:
                return info2;
            default:
                if (checkValidVideoDecoder)
                {
                    checkValidVideoDecoder = false;
                    throw new MPEMediaError(vd, "Invalid decoder");
                }
                else
                {
                    return info1;
                }
        }
    }

    public void cannedSetCheckValidDecoder(boolean b)
    {
        checkValidVideoDecoder = b;
    }

    private class DecodingInfo
    {
        public EDListener listener;

        public int decoder;

        public int tuner;

        public int[] pids;

        public short[] types;

        public boolean blocked;

        public ScalingBounds size;

        public Dimension inputVideoSize;

        public boolean frozen = false;

        public boolean scaling = true;

        public boolean decoding = false;

        public boolean clipping = true;

        public ScalingCaps scalingCaps;

        public byte posCap = VideoPresentationControl.POS_CAP_FULL;

        public int aspectRatio = VideoFormatControl.ASPECT_RATIO_4_3;

        public int afd = VideoFormatControl.AFD_4_3;

        public int platformDFC = VideoFormatControl.DFC_PROCESSING_NONE;

        public int applicationDFC = VideoFormatControl.DFC_PROCESSING_NONE;

        public DecodingInfo(int decoder)
        {
            this.decoder = decoder;

            size = dfltSB;
            inputVideoSize = small.getSize();
            scalingCaps = new ScalingCaps();
            scalingCaps.supportsArbScaling = true;
            scalingCaps.hScalingFactors = new float[] { 0.5f, 2.0f };
            scalingCaps.vScalingFactors = new float[] { 0.5f, 2.0f };
        }

        public void setDecodingValues(EDListener listener, int tuner, int[] pids, short[] types, boolean blocked)
        {
            this.listener = listener;
            this.tuner = tuner;
            this.pids = pids;
            this.types = types;
            this.decoding = true;
            this.blocked = blocked;
        }

        public boolean equals(Object obj)
        {
            if (obj == null || !(obj instanceof DecodingInfo)) return false;
            DecodingInfo info = (DecodingInfo) obj;

            if (pids.length != info.pids.length || types.length != info.types.length || decoder != info.decoder
                    || tuner != info.tuner) return false;

            for (int i = 0; i < pids.length; i++)
            {
                boolean match = false;
                for (int j = 0; j < info.pids.length; j++)
                {
                    if (pids[i] == info.pids[j])
                    {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }
            for (int i = 0; i < types.length; i++)
            {
                boolean match = false;
                for (int j = 0; j < info.types.length; j++)
                {
                    if (types[i] == info.types[j])
                    {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            }

            return true;
        }

        public void reset()
        {
            listener = null;
            tuner = 0;
            pids = null;
            types = null;
            size = dfltSB;
            decoding = false;
            frozen = false;
            clipping = true;
            blocked = false;

            inputVideoSize = small.getSize();
            scalingCaps = new ScalingCaps();
            scalingCaps.supportsArbScaling = true;
            scalingCaps.hScalingFactors = new float[] { 0.5f, 2.0f };
            scalingCaps.vScalingFactors = new float[] { 0.5f, 2.0f };

            posCap = VideoPresentationControl.POS_CAP_FULL;
            aspectRatio = VideoFormatControl.ASPECT_RATIO_4_3;
            afd = VideoFormatControl.AFD_4_3;
            platformDFC = VideoFormatControl.DFC_PROCESSING_NONE;
            applicationDFC = VideoFormatControl.DFC_PROCESSING_NONE;

        }
    }

    public String toString(int eventCode)
    {
        // TODO Auto-generated method stub
        return "event(" + eventCode + ")";
    }

    public int getCCState()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getAspectRatio(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return info.aspectRatio;
    }

    public void cannedSetAspectRatio(int vd, int ar)
    {
        DecodingInfo info = getDecodingInfo(vd);
        info.aspectRatio = ar;
    }

    public int getActiveFormatDefinition(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return info.afd;
    }

    public void cannedSetAFD(int vd, int afd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        info.afd = afd;
    }

    public boolean isPlatformDFC(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return (info.applicationDFC == VideoFormatControl.DFC_PLATFORM);
    }

    public int getDFC(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return (info.applicationDFC == VideoFormatControl.DFC_PLATFORM) ? info.platformDFC : info.applicationDFC;
    }

    public int getPlatformDFC(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return info.platformDFC;
    }

    public boolean checkDFC(int vd, int dfc)
    {
        return true;
    }

    public void setDFC(int vd, int dfc)
    {
        DecodingInfo info = getDecodingInfo(vd);

        info.applicationDFC = dfc;
        if (dfc != VideoFormatControl.DFC_PLATFORM)
        {
            info.platformDFC = dfc;
        }
    }

    public void setCCState(int ccState)
    {
        // TODO Auto-generated method stub

    }

    public void setCCServiceNumbers(int analogSvcNo, int digitalSvcNo)
    {
        // TODO Auto-generated method stub

    }

    public EDListener cannedGetEDListener(VideoDevice vd)
    {
        DecodingInfo info = getDecodingInfo(vd.getHandle());
        return info.listener;
    }

    public int[] getCCSupportedServiceNumbers()
    {
        final int maxServices = (8 + 63); // max number of analog (8) + digital
                                          // (63) CC services
        int[] services = new int[maxServices];

        for (int ii = 0; ii < maxServices; ii++)
        {
            if (ii < 8)
            {
                services[ii] = 1000 + ii;
            }
            else
            {
                services[ii] = ii - 7;
            }
        }

        return services;
    }

    public Dimension getVideoInputSize(int vd)
    {
        DecodingInfo info = getDecodingInfo(vd);
        return info.inputVideoSize.getSize();
    }

    public void cannedSetVideoInputSize(int vd, Dimension size)
    {
        DecodingInfo info = getDecodingInfo(vd);
        info.inputVideoSize.setSize(size);
    }

    public boolean supportsClipping(int vd)
    {
        return getDecodingInfo(vd).clipping;
    }

    public void cannedSetClipping(int vd, boolean clipping)
    {
        getDecodingInfo(vd).clipping = clipping;
    }

    public byte getPositioningCapability(int vd)
    {
        return getDecodingInfo(vd).posCap;
    }

    public void cannedSetPositionCapability(int vd, byte posCap)
    {
        getDecodingInfo(vd).posCap = posCap;
    }

    public ScalingCaps getScalingCaps(int vd)
    {
        return getDecodingInfo(vd).scalingCaps;
    }

    public void setMute(int sessionHandle, boolean mute)
    {
        //no-op;
    }

    public float setGain(int sessionHandle, float gain)
    {
        return 0.0F;
    }

    public void setCCI(int sessionHandle, byte cci)
    {
        //no-op
    }

    public void cannedSetScalingCaps(int vd, ScalingCaps caps)
    {
        getDecodingInfo(vd).scalingCaps = caps;
    }

    public byte[] cannedGetLastDripFeedFrame()
    {
        return lastDripFeedFrame;
    }

    public S3DConfiguration getS3DConfiguration(int vd)
    {
        return null;
    }

    public int getInputVideoScanMode(int vd)
    {
        return 0;
    }

}
