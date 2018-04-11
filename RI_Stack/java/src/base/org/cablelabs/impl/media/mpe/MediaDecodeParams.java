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

import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.util.Arrays;

/**
 * Immutable value object that defines parameters required for a call to decode
 * at the native layer (via
 * {@link MediaAPI#decodeBroadcast(org.cablelabs.impl.media.mpe.MediaAPI.MediaDecodeParams)
 * decodeBroadcast()}.)
 */
public class MediaDecodeParams
{
    /**
     * Construct the {@link MediaDecodeParams} instance from the specified
     * arguments.
     *
     * @param edl
     *            - ED listener to receive events from the decode session
     * @param vd
     *            - native handle of the video device to send output to
     * @param tuner
     *            - native handle of the input source
     * @param pcr
     *            - PCR PID
     * @param pids
     *            - <code>int</code> array of PIDs to decode; this should be
     *            empty for an analog service
     * @param types
     *            -
     *            <code>short<code> array of types of the PIDS identified in the
     * @param pids
     *            array
     * @param block
     *            - the initial blocking state of the presentation: <code>true
     *            </code> means it should initially be blocked;
     *            <code>false</code> means it should be clear
     * @param mute
     *            - mute audio
     * @param gain
     *            - requested gain (updated when decode is initiated with actual gain)
     * @param cci
     *            - current CCI
     */
    public MediaDecodeParams(final EDListener edl, final int vd, final int tuner, final short ltsid, 
                             final int pcr, final int pids[], final short types[], 
                             boolean block, boolean mute, float[] gain, byte cci)
    {
        if (edl == null) throw new IllegalArgumentException("null listener");
        if (pids == null) throw new IllegalArgumentException("null PID array");
        if (types == null) throw new IllegalArgumentException("null types array");
        if (pids.length != types.length) throw new IllegalArgumentException("pid and type array lengths don't match");

        this.listener = edl;
        this.videoHandle = vd;
        this.tunerHandle = tuner;
        this.ltsid = ltsid;
        this.pcrPid = pcr;
        this.streamPids = Arrays.copy(pids);
        this.streamTypes = Arrays.copy(types);
        this.blocked = block;
        this.mute = mute;
        this.gain = gain;
        this.cci = cci;
    }

    private final EDListener listener;

    private final int videoHandle;

    private final int tunerHandle;

    private final short ltsid;

    private final int pcrPid;

    private final int streamPids[];

    private final short streamTypes[];

    private final boolean blocked;

    private final boolean mute;

    private final float[] gain;

    private final byte cci;

    public String toString()
    {
            return "MediaDecodeParams[" + " vd=" + "0x" + Integer.toHexString(videoHandle) + ", tuner=" + "0x" 
                    + Integer.toHexString(tunerHandle) + ", ltsid=" + ltsid + ", pcr=" + pcrPid + ", pids=" + Arrays.toString(streamPids)
                    + ", types=" + Arrays.toString(streamTypes) + ", block=" + blocked + ", mute: " + mute + ", gain: " + Arrays.toString(gain) + "]";
        }

    public EDListener getListener()
    {
        return listener;
    }

    public int getPcrPid()
    {
        return pcrPid;
    }

    public int[] getStreamPids()
    {
        return Arrays.copy(streamPids);
    }

    public short[] getStreamTypes()
    {
        return Arrays.copy(streamTypes);
    }

    public int getTunerHandle()
    {
        return tunerHandle;
    }

    public int getVideoHandle()
    {
        return videoHandle;
    }

    public boolean isBlocked()
    {
        return blocked;
    }

    public boolean isMuted()
    {
        return mute;
    }

    public float getGain()
    {
        return gain[0];
    }

    public int getCCI()
    {
        return cci;
    }
}
