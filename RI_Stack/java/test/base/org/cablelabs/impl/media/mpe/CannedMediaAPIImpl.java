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

import org.havi.ui.HScreenRectangle;
import org.ocap.media.ClosedCaptioningControl;

/**
 * CannedMediaAPIImpl
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedMediaAPIImpl extends MediaAPIImpl
{

    private ScalingBounds currentSize;

    private int ccState;

    private int analogCCService;

    private int digitalCCService;

    private int[] dfcs = { 2, 2 };

    private boolean[] platforms = { true, true };

    private float[][] hscale = { { 0.0f, 1.0f }, { 0.0f, 1.0f } };

    private float[][] vscale = { { 0.0f, 1.0f }, { 0.0f, 1.0f } };

    private boolean[] arbscale = { true, true };

    /**
     * 
     */
    public CannedMediaAPIImpl()
    {
        super();
        currentSize = CannedMediaAPI.dfltSB;
        ccState = ClosedCaptioningControl.CC_TURN_OFF;
        analogCCService = ClosedCaptioningControl.CC_ANALOG_SERVICE_CC1;
        digitalCCService = 100;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeCheckSize(int,
     * java.awt.Rectangle, java.awt.Rectangle, java.awt.Rectangle,
     * java.awt.Rectangle)
     */
    protected int nativeCheckBounds(int decoder, HScreenRectangle desiredSrc, HScreenRectangle desiredDst,
            HScreenRectangle closestSrc, HScreenRectangle closestDst)
    {
        if (decoder != 1 && decoder != 2) return 1;
        closestSrc.setLocation(desiredSrc.x, desiredSrc.y);
        closestSrc.setSize(desiredSrc.width, desiredSrc.height);
        closestDst.setLocation(desiredDst.x, desiredDst.y);
        closestDst.setSize(desiredDst.width, desiredDst.height);
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeDecode(org.cablelabs.
     * impl.manager.ed.EDListener, int, int, int[], short[])
     */
    protected int nativeDecodeBroadcast(final MediaDecodeParams params, int[] sessionHandle)
    {
        if (params.getVideoHandle() != 1 && params.getVideoHandle() != 2)
        {
            return 1;
        }
        else
        {
            // Return a session handle that is the same as the video device
            // handle.
            sessionHandle[0] = params.getVideoHandle();
            return 0;
        }
    }

    protected int nativeBlockPresentation(int sessionHandle, boolean block)
    {
        if (sessionHandle != 1 && sessionHandle != 2)
            return 1;
        else
            return 0;
    }

    protected int nativeChangePids(int sessionHandle, int pcrPid, int[] pids, short[] types)
    {
        //
        // TODO: This function is completely untested. It is put in place as
        // part of
        // bug 5116 to support the new changePids api in mpe
        //

        if (sessionHandle != 1 && sessionHandle != 2)
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeFreeze(int)
     */
    protected int nativeFreeze(int decoder)
    {
        if (decoder != 1 && decoder != 2)
            return 1;
        else
            return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetSize(int,
     * java.awt.Rectangle, java.awt.Rectangle)
     */
    protected int nativeGetBounds(int decoder, HScreenRectangle src, HScreenRectangle dst)
    {
        if (decoder != 1 && decoder != 2) return 1;
        src.setLocation(currentSize.src.x, currentSize.src.y);
        src.setSize(currentSize.src.width, currentSize.src.height);
        dst.setLocation(currentSize.dst.x, currentSize.dst.y);
        dst.setSize(currentSize.dst.width, currentSize.dst.height);
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeResume(int)
     */
    protected int nativeResume(int decoder)
    {
        if (decoder != 1 && decoder != 2)
            return 1;
        else
            return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeSetSize(int,
     * java.awt.Rectangle, java.awt.Rectangle)
     */
    protected int nativeSetBounds(int decoder, HScreenRectangle src, HScreenRectangle dst)
    {
        if (decoder != 1 && decoder != 2) return 1;
        currentSize.src = src;
        currentSize.dst = dst;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeStop(int)
     */
    protected int nativeStopBroadcastDecode(int decoder)
    {
        if (decoder != 1 && decoder != 2)
            return 1;
        else
            return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeSupportsScaling(int,
     * boolean[])
     */
    protected int nativeSupportsComponentVideo(int decoder, boolean[] result)
    {
        if (decoder != 1 && decoder != 2) return 1;
        result[0] = true;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeSwapDecoders(int,
     * int, boolean)
     */
    protected int nativeSwapDecoders(int decoder1, int decoder2, boolean audioUse)
    {
        if (decoder1 != 1 && decoder1 != 2) return 1;
        if (decoder2 != 1 && decoder2 != 2) return 1;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetCCState(int[])
     */
    protected int nativeGetCCState(int[] state)
    {
        state[0] = ccState;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetCCSupportedServiceNumbers
     * (int)
     */
    protected int[] nativeGetCCSupportedServiceNumbers()
    {
        return new int[] { analogCCService, digitalCCService };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeSetCCServiceNumbers(int,
     * int)
     */
    protected int nativeSetCCServiceNumbers(int analog, int digital)
    {
        if (analog < 1000 || analog > 1007) return 1;

        analogCCService = analog;
        digitalCCService = digital;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeSetCCState(int)
     */
    protected int nativeSetCCState(int state)
    {
        if (state != ClosedCaptioningControl.CC_TURN_OFF && state != ClosedCaptioningControl.CC_TURN_ON
                && state != ClosedCaptioningControl.CC_TURN_ON_MUTE)
            return 1;
        else
            ccState = state;

        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeCheckDFC(int, int)
     */
    protected int nativeCheckDFC(int vd, int dfc)
    {
        if (vd != 1 && vd != 2) return 1;

        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeDripFeedRenderFrame(int,
     * byte[])
     */
    protected int nativeDripFeedRenderFrame(int dripFeedHandle, byte[] frameData)
    {
        if (dripFeedHandle != 1 && dripFeedHandle != 2) return 1;
        if (frameData == null || frameData.length < 1) return 2;

        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeDripFeedStart(org.cablelabs
     * .impl.media.mpe.MediaDripFeedParams, int[])
     */
    protected int nativeDripFeedStart(MediaDripFeedParams params, int[] dripFeedHandle)
    {
        if (params == null) return 1;
        if (dripFeedHandle == null || dripFeedHandle.length != 1) return 2;
        if (params.videoHandle != 1 && params.videoHandle != 2) return 3;

        dripFeedHandle[0] = params.videoHandle;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeDripFeedStop(int)
     */
    protected int nativeDripFeedStop(int dripFeedHandle)
    {
        if (dripFeedHandle != 1 && dripFeedHandle != 2) return 1;

        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetAFD(int, int[])
     */
    protected int nativeGetAFD(int vd, int[] afd)
    {
        if (vd != 1 && vd != 2) return 1;
        if (afd == null || afd.length != 1) return 2;

        afd[0] = 15;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetAspectRatio(int,
     * int[])
     */
    protected int nativeGetAspectRatio(int vd, int[] ratio)
    {
        if (vd != 1 && vd != 2) return 1;
        if (ratio == null || ratio.length != 1) return 2;

        ratio[0] = 2;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetDFC(int, int[],
     * boolean[])
     */
    protected int nativeGetDFC(int vd, int[] dfc, boolean[] isPlatform)
    {
        if (vd != 1 && vd != 2) return 1;
        if (dfc == null || dfc.length != 1) return 2;
        if (isPlatform == null || isPlatform.length != 1) return 3;

        dfc[0] = dfcs[vd];
        isPlatform[0] = platforms[vd];
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetPositioningCapability
     * (int, byte[])
     */
    protected int nativeGetPositioningCapability(int vd, byte[] result)
    {
        if (vd != 1 && vd != 2) return 1;
        if (result == null || result.length != 1) return 2;

        result[0] = 0;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetScalingCaps(int,
     * org.cablelabs.impl.media.mpe.ScalingCaps)
     */
    protected int nativeGetScalingCaps(int vd, ScalingCaps result)
    {
        if (vd != 1 && vd != 2) return 1;
        if (result == null) return 2;

        result.hScalingFactors = hscale[vd];
        result.vScalingFactors = vscale[vd];
        result.supportsArbScaling = arbscale[vd];
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeGetVideoInputSize(int,
     * java.awt.Dimension)
     */
    protected int nativeGetVideoInputSize(int decoder, Dimension size)
    {
        if (decoder != 1 && decoder != 2) return 1;
        if (size == null) return 2;

        size.height = 480;
        size.width = 640;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeSetDFC(int, int)
     */
    protected int nativeSetDFC(int vd, int dfc)
    {
        if (vd != 1 && vd != 2) return 1;

        if (dfc == 8)
        {
            dfcs[vd] = 2;
            platforms[vd] = true;
        }
        else
        {
            dfcs[vd] = dfc;
            platforms[vd] = false;
        }

        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.MediaAPIImpl#nativeSupportsClipping(int,
     * boolean[])
     */
    protected int nativeSupportsClipping(int vd, boolean[] result)
    {
        if (vd != 1 && vd != 2) return 1;
        if (result == null || result.length != 1) return 2;

        result[0] = true;
        return 0;
    }

}
