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

package org.cablelabs.impl.media.session;

import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.player.VideoDevice;

public abstract class AbstractSession implements Session, EDListener
{
    /*
     * Fixed Fields - fields that are set at construction and remain in effect
     * until termination
     */

    /**
     * The {@link MediaAPI} to use to perform native operations.
     */
    protected final MediaAPI mediaAPI = (MediaAPIManager) ManagerManager.getInstance(MediaAPIManager.class);

    /**
     * Object to use for synchronization.
     */
    protected final Object lock;

    /**
     * {@link SessionListener} to receive asynchronous session events.
     */
    protected final SessionListener listener;

    /**
     * The current video device being used.
     */
    protected VideoDevice videoDevice;

    /**
     * Initial blocking status.
     */
    protected boolean blocked;

    /** Blocking monitor associated with this Session */
    // protected BlockingMonitor blockingMonitor;

    protected AbstractSession(Object sync, SessionListener lstnr, VideoDevice vd)
    {
        if (Asserting.ASSERTING)
        {
            Assert.condition(sync != null);
            Assert.condition(lstnr != null);
            Assert.condition(vd != null);
        }

        lock = sync;
        listener = lstnr;
        videoDevice = vd;
    }

    public void setVideoDevice(VideoDevice vd)
    {
        synchronized (lock)
        {
            videoDevice = vd;
        }
    }

    public void freeze() throws MPEException
    {
        synchronized (lock)
        {
            if (!isStarted())
            {
                return;
            }

            try
            {
                if (videoDevice != null)
                {
                    mediaAPI.freeze(videoDevice.getHandle());
                }
            }
            catch (MPEMediaError err)
            {
                throw new MPEException(err.getMessage());
            }
        }
    }

    public void resume() throws MPEException
    {
        synchronized (lock)
        {
            if (!isStarted())
            {
                return;
            }

            try
            {
                if (videoDevice != null)
                {
                    mediaAPI.resume(videoDevice.getHandle());
                }
            }
            catch (MPEMediaError err)
            {
                throw new MPEException(err.getMessage());
            }
        }
    }

    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        if (listener != null)
        {
            listener.handleSessionEvent(this, eventCode, eventData1, eventData2);
        }
    }

    /**
     * This helper class encapsulates the parameters that must be passed to the
     * native decode call&mdash;i.e., arrays of PIDs and PID types. The
     * constructor builds these arrays, which are then accessed via the final
     * fields {@link #pids} and {@link #types}, respectively.
     */
    static class StreamToPIDConverter
    {
        public final int[] pids;

        public final short[] types;

        StreamToPIDConverter(ElementaryStreamExt[] streams)
        {
            int length = streams.length;
            pids = new int[length];
            types = new short[length];

            for (int i = 0; i < length; ++i)
            {
                ElementaryStreamExt stream = streams[i];
                pids[i] = stream.getPID();
                types[i] = stream.getElementaryStreamType();
            }
        }
    }
}
