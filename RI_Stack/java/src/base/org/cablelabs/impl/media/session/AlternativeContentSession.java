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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.MediaDripFeedParams;
import org.cablelabs.impl.media.player.VideoDevice;

/**
 * Present alternative content (a black screen) using
 * {@link MediaAPI#dripFeedStart(MediaDripFeedParams)}.
 * 
 * File rendered is blackScreen.m2v.
 */
public class AlternativeContentSession extends AbstractSession implements StartableSession
{
    private static final Logger log = Logger.getLogger(AlternativeContentSession.class);

    private int mediaAPIHandle = 0;

    private final byte[] blackScreen;

    private static final String BLACK_SCREEN_FILE_NAME = "/blackScreen.m2v";

    private boolean started = false;

    public AlternativeContentSession(Object lock, SessionListener listener, VideoDevice videoDevice)
    {
        super(lock, listener, videoDevice);
        byte[] screen = new byte[0];
        InputStream resourceStream = null;
        try
        {
            resourceStream = getClass().getResourceAsStream(BLACK_SCREEN_FILE_NAME);

            if (resourceStream == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to create inputStream from file: " + BLACK_SCREEN_FILE_NAME);
                }
            }
            else
            {
                screen = new byte[resourceStream.available()];
                DataInputStream das = new DataInputStream(resourceStream);
                das.readFully(screen);
            }
        }
        catch (IOException ioe)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to load black screen: " + BLACK_SCREEN_FILE_NAME, ioe);
            }
        } finally
        {
            if (resourceStream != null)
            {
                try
                {
                    resourceStream.close();
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }

        blackScreen = screen;
    }

    public boolean isStarted()
    {
        return started;
    }

    /**
     * Present the alternative content.
     */
    public void start()
    {
        if (log.isInfoEnabled())
        {
            log.info("start: " + this);
        }
        synchronized (lock)
        {
            if (started)
            {
                if (log.isInfoEnabled())
                {
                    log.info("start called when already started - ignoring: " + this);
                }
                return;
            }
            if (log.isDebugEnabled())
            {
                log.debug("calling dripFeedStart and dripFeedRenderFrame");
            }
            MediaDripFeedParams params = new MediaDripFeedParams(this, videoDevice.getHandle());
            mediaAPIHandle = mediaAPI.dripFeedStart(params);
            mediaAPI.dripFeedRenderFrame(mediaAPIHandle, blackScreen);
            started = true;
            if (log.isInfoEnabled())
            {
                log.info("started: " + this);
            }
        }
    }

    /**
     * Stop presenting alternative content.
     * @param holdFrame not supported by this session implementation
     */
    public void stop(boolean holdFrame)
    {
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("stop: " + this);
            }
            if (!started)
            {
                if (log.isInfoEnabled())
                {
                    log.info("stop called when already stopped - ignoring: " + this);
                }
                return;
            }
            mediaAPI.dripFeedStop(mediaAPIHandle);
            started = false;
            if (log.isInfoEnabled())
            {
                log.info("stopped: " + this);
            }
        }
    }

    public Time setMediaTime(Time mediaTime) throws MPEException
    {
        if (log.isInfoEnabled())
        {
            log.info("setMediaTime: " + mediaTime + ", ignoring");
        }
        return null;
    }

    public float setRate(float rate) throws MPEException
    {
        if (log.isInfoEnabled())
        {
            log.info("setRate: " + rate + ", ignoring");
        }
        return 1.0f;
    }

    public Time getMediaTime() throws MPEException
    {
        return null;
    }

    public float getRate() throws MPEException
    {
        return 1.0f;
    }

    public int getNativeHandle()
    {
        synchronized (lock)
        {
            return mediaAPIHandle;
        }
    }

    public String toString()
    {
        return "AlternativeContentSession " + Integer.toHexString(hashCode());
    }
}
