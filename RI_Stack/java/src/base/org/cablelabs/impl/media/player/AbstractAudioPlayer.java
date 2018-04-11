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
package org.cablelabs.impl.media.player;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.media.EndOfMediaEvent;
import javax.media.GainControl;
import javax.media.Time;
import javax.media.protocol.DataSource;

import org.apache.log4j.Logger;
import org.davic.media.MediaTimePositionChangedEvent;
import org.davic.media.MediaTimePositionControl;
import org.dvb.media.StopByResourceLossEvent;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.SoundManager;
import org.cablelabs.impl.media.presentation.AudioPresentation;
import org.cablelabs.impl.media.presentation.AudioPresentationContext;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.sound.Sound;

/**
 * AudioPlayerBase This class is the core implementation of the audio Player
 * hierarchy.
 * 
 * @author Joshua Keplinger
 * 
 */
public class AbstractAudioPlayer extends AbstractPlayer implements AudioPresentationContext
{

    private static final Logger log = Logger.getLogger(AbstractAudioPlayer.class);

    private Sound sound;

    private SoundManager sndmgr;
    private GainControlImpl gainControl;

    /**
     * @param cc
     */
    public AbstractAudioPlayer(CallerContext cc)
    {
        super(cc, new Object(), new ResourceUsageImpl(cc));
        this.gainControl = new GainControlImpl();
        addControls(new ControlBase[] { new MediaTimePositionControlImpl(true), gainControl });
        sndmgr = (SoundManager) ManagerManager.getInstance(SoundManager.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.player.PlayerBase#doGetVisualComponent()
     */
    protected Component doGetVisualComponent()
    {
        // No visual component to return on an audio Player
        return null;
    }

    public GainControl getGainControl()
    {
        return gainControl;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.mediax.player.PlayerBase#doAcquireRealizeResources
     * (org.cablelabs.impl.ocap.resource.ResourceUsageImpl)
     */
    protected Object doAcquireRealizeResources()
    {
        // Nothing to acquire here...
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.mediax.player.PlayerBase#doReleaseRealizedResources
     * (org.cablelabs.impl.ocap.resource.ResourceUsageImpl)
     */
    protected void doReleaseRealizedResources()
    {
        // Nothing to release here...
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.mediax.player.PlayerBase#doAcquirePrefetchResources
     * (org.cablelabs.impl.ocap.resource.ResourceUsageImpl)
     */
    protected Object doAcquirePrefetchResources()
    {
        // We're gonna grab the URL from the DataSource/MediaLocator
        DataSource datasource = getSource();
        URL url;
        try
        {
            url = datasource.getLocator().getURL();
        }
        catch (MalformedURLException ex)
        {
            return ex;
        }

        // We have the URL, so open up a connection
        URLConnection urlConnection;
        InputStream is = null;
        byte[] newSoundData;
        int size, newSoundDataSize = 0;
        String mimeType;
        try
        {
            // Open up the connection
            urlConnection = url.openConnection();
            // Get the reported size of the content
            size = urlConnection.getContentLength();
            // Allocate our byte array
            newSoundData = new byte[size];
            // Get the mimeType for future use
            mimeType = urlConnection.getContentType();
            // Grab the InputStream for reading in the bytes
            is = urlConnection.getInputStream();

            // Read in the data until we get EOF
            int count = 0;
            while (newSoundDataSize < size && count != -1)
            {
                count = is.read(newSoundData, newSoundDataSize, size - newSoundDataSize);
                if (count != -1) newSoundDataSize += count;
            }
        }
        catch (IOException ex)
        {
            return ex;
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                return ex;
            }
        }

        // Got the data we need, let's create the native sound
        sound = sndmgr.createSound(mimeType, newSoundData, 0, newSoundDataSize);
        if (sound == null) return new Exception("Unable to create native sound resource");

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.mediax.player.PlayerBase#doReleasePrefetchedResources
     * (org.cablelabs.impl.ocap.resource.ResourceUsageImpl)
     */
    protected void doReleasePrefetchedResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doReleasePrefetchedResources");
        }

        if (sound != null)
            sound.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.player.PlayerBase#doReleaseAllResources()
     */
    protected void doReleaseAllResources()
    {
        // Releasing the resources is done in the other doRelease*() methods
    }

    public boolean getMute()
    {
        return gainControl.getMute();
    }

    public float getGain()
    {
        return gainControl.getDB();
    }

    private class MediaTimePositionControlImpl extends ControlBase implements MediaTimePositionControl
    {
        public MediaTimePositionControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public Time getMediaTimePosition()
        {
            synchronized (getLock())
            {
                return isEnabled() ? getMediaTime() : CLOSED_TIME;
            }
        }

        public Time setMediaTimePosition(Time mediaTime)
        {
            synchronized (getLock())
            {
                if (isEnabled())
                {
                    //relying on the fact that the update to the audio playback mediatime is synchronous, so querying
                    //the clock mediatime immediately after calling setMediaTime is acceptable
                    //this is NOT true for subclasses of AbstractServicePresentation, but is true for AudioPresentation
                    setMediaTime(mediaTime, false);
                    Time actualMt = getClock().getMediaTime();
                    if (actualMt != CLOSED_TIME)
                        postEvent(new MediaTimePositionChangedEvent(AbstractAudioPlayer.this, getState(), getState(),
                                getTargetState(), actualMt));
                    return actualMt;
                }
                else
                    return CLOSED_TIME;
            }
        }

    }

    public DataSource getDataSource()
    {
        return getSource();
    }

    protected Presentation createPresentation()
    {
        return new AudioPresentation(this, sound);
    }

    public void setMediaTime(Time now)
    {
        setMediaTime(now, true);
    }

    public void notifyEndOfMedia()
    {
        stop(new EndOfMediaEvent(this, Started, Started, Prefetched, getMediaTime()));
    }

    public void notifyStopByResourceLoss()
    {
        stop(new StopByResourceLossEvent(this, Started, Started, Prefetched, getSource().getLocator()));
    }
}
