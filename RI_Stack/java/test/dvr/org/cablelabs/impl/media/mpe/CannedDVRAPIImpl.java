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

/**
 * CannedDVRAPIImpl
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedDVRAPIImpl extends DVRAPIImpl
{

    private float[] rates;

    private long[] times;

    /**
     * 
     */
    public CannedDVRAPIImpl()
    {
        super();
        rates = new float[] { 1.0f, 1.0f };
        times = new long[] { 10000000L, 10000000L };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.DVRAPIImpl#nativeDecodeRecording(org.cablelabs
     * .impl.manager.ed.EDListener, int, java.lang.String, int[], short[],
     * int[], long)
     */
    protected int nativeDecodeRecording(EDListener listener, int decoder, String recording, int[] pids, short[] types,
            int[] dvr, long start)
    {
        if (recording == null) return 2;
        if (decoder > 1 || decoder < 0)
            return 1;
        else
            dvr[0] = decoder;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.mpe.DVRAPIImpl#nativeDecodeTSB(org.cablelabs
     * .impl.manager.ed.EDListener, int, int, int[], short[], int[], long)
     */
    protected int nativeDecodeTSB(EDListener listener, int decoder, int tsb, int[] pids, short[] types, int[] dvr,
            long start, float rate, boolean blocked)
    {
        if (decoder > 1 || decoder < 0)
            return 1;
        else
        {
            dvr[0] = decoder;
            for (int i = 0; i < dvr.length; i++)
            {
                rates[dvr[i]] = rate;
            }
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.DVRAPIImpl#nativeGetMediaTime(int,
     * long[])
     */
    protected int nativeGetMediaTime(int dvr, long[] time)
    {
        time[0] = times[dvr];
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.DVRAPIImpl#nativeGetRate(int, float[])
     */
    protected int nativeGetRate(int dvr, float[] rate)
    {
        rate[0] = rates[dvr];
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.DVRAPIImpl#nativeSetMediaTime(int,
     * long)
     */
    protected int nativeSetMediaTime(int dvr, long time)
    {
        times[dvr] = time;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.DVRAPIImpl#nativeSetRate(int, float[])
     */
    protected int nativeSetRate(int dvr, float[] rate)
    {
        rates[dvr] = rate[0];
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.mpe.DVRAPIImpl#nativeStop(int)
     */
    protected int nativeStopDVRDecode(int dvr)
    {
        return 0;
    }
}
