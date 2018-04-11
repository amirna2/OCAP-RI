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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.
 */

package org.havi.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.media.Time;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.SoundManager;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.sound.Playback;
import org.cablelabs.impl.sound.PlaybackOwner;
import org.cablelabs.impl.sound.Sound;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The {@link org.havi.ui.HSound HSound} class is used to represent an audio
 * clip.
 *
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>The starting position of any audio clip to be played.</td>
 * <td>At the beginning of the audio clip.</td>
 * <td>---</td>
 * <td>---</td>
 * </tr>
 * </table>
 *
 * @author Todd Earles
 * @author Aaron Kamienski (1.0.1 support)
 * @version 1.1
 */

public class HSound extends java.lang.Object
{

    private SoundManager sm;

    private Sound sound;

    private Playback playback;

    private PlaybackOwner owner;

    private CallerContext cc;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(HSound.class.getName());

    /**
     * Creates an {@link org.havi.ui.HSound HSound} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HSound()
    {
        // Get the SoundManager instance
        sm = (SoundManager) ManagerManager.getInstance(SoundManager.class);

        // create an handler for SoundManagerListener events.
        owner = new HSoundOwner();

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        cc = ccm.getCurrentContext();
        cc.addCallbackData(new CallbackData()
        {
            public void active(CallerContext ctx)
            { /* empty */
            }

            public void pause(CallerContext ctx)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CallbackData - pause()");
                }
                stop();
            }

            public void destroy(CallerContext cc)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CallbackData - destroy()");
                }
                dispose();
            }
        }, this);

    }

    /**
     * Loads data synchronously into an {@link org.havi.ui.HSound HSound} object
     * from an audio sample in the specified file. If the object already
     * contains data, this method shall perform the following sequence:
     * <p>
     * <ul>
     * <li>stop the sample if it is playing or looping.
     * <li>dispose of the old data and any associated resources, as if the
     * {@link org.havi.ui.HSound#dispose dispose} method had been called.
     * <li>load the new data synchronously.
     * </ul>
     *
     * @param location
     *            the name of a file containing audio data in a recognized file
     *            format.
     * @exception IOException
     *                if the sample cannot be loaded due to an IO problem.
     * @exception SecurityException
     *                if the caller does not have sufficient rights to access
     *                the specified audio sample.
     */
    public void load(String location) throws IOException, SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("load file - file = " + location);
        }

        URL url = new URL("file:" + (new java.io.File(location)).getCanonicalPath());
        load(url);
    }

    /**
     * Loads data synchronously into an {@link org.havi.ui.HSound HSound} object
     * from an audio sample indicated by a URL. If the object already contains
     * data, this method shall perform the following sequence:
     * <p>
     * <ul>
     * <li>stop the sample if it is playing or looping.
     * <li>dispose of the old data and any associated resources, as if the
     * {@link org.havi.ui.HSound#dispose dispose} method had been called.
     * <li>load the new data synchronously.
     * </ul>
     *
     * @param contents
     *            a URL referring to the data to load.
     * @exception IOException
     *                if the audio sample cannot be loaded due to an IO problem.
     * @exception SecurityException
     *                if the caller does not have sufficient rights to access
     *                the specified audio sample.
     */
    public void load(URL url) throws IOException, SecurityException
    {

        dispose();

        URLConnection urlConnection;

        if (log.isDebugEnabled())
        {
            log.debug("load URL - URL = " + url.toString());
        }

        try
        {
            urlConnection = url.openConnection();
        }
        catch (IOException e)
        {
            SystemEventUtil.logRecoverableError(new Exception("HSound load URL - "
                    + "url.openConnection() threw IOException " + e));
            throw (e);
        }

        int size = urlConnection.getContentLength();
        if (size < 0)
            throw new FileNotFoundException("file does not exist");

        if (log.isDebugEnabled())
        {
            log.debug("load URL - getContentLength() returned = " + size);
        }

        // get the MIME type
        String mimeType = urlConnection.getContentType();
        if (log.isDebugEnabled())
        {
            log.debug("load URL - " + "urlConnection.getContentType() returned " + mimeType);
        }

        // create a byte array to hold the sound data
        byte[] newSoundData = new byte[size];

        // read data from the input stream into the byte array
        InputStream is = urlConnection.getInputStream();
        int newSoundDataSize = 0;
        try
        {
            int count = 0;
            while (newSoundDataSize < size && count != -1)
            {
                count = is.read(newSoundData, newSoundDataSize, size);
                if (count != -1) newSoundDataSize += count;
            }
        }
        finally
        {
            is.close();
        }

        sound = sm.createSound(mimeType, newSoundData, 0, newSoundDataSize);

    }

    /**
     * Constructs an {@link org.havi.ui.HSound HSound} object from an array of
     * bytes encoded in the same encoding format as when reading this type of
     * audio sample data from a file. If the object already contains data, this
     * method shall perform the following sequence:
     * <p>
     * <ul>
     * <li>stop the sample if it is playing or looping.
     * <li>dispose of the old data and any associated resources, as if the
     * {@link org.havi.ui.HSound#dispose dispose} method had been called.
     * <li>load the new data synchronously.
     * </ul>
     * <p>
     * If the byte array does not contain a valid audio sample then this method
     * shall throw a <code>java.lang.IllegalArgumentException</code>.
     *
     * @param data
     *            the data for the {@link org.havi.ui.HSound HSound} object
     *            encoded in the specified format for audio sample files of this
     *            type.
     */
    public void set(byte[] data)
    {
        if (log.isDebugEnabled())
        {
            log.debug("set - entering");
        }

        dispose();

        sound = sm.createSound(null, data, 0, data.length);
        if (sound == null) throw new IllegalArgumentException();

    }

    /**
     * Starts the {@link org.havi.ui.HSound HSound} class playing from the
     * beginning of its associated audio data. If the sample data has not been
     * completely loaded, this method has no effect.
     * <p>
     * When the audio data has been played in its entirety then no further
     * audible output should be made until the next play or loop method is
     * invoked. Note that the audio data is played back asynchronously. There is
     * no mechanism for synchronization with other classes presenting sounds,
     * images, or video.
     * <p>
     * This method may fail &quot;silently&quot; if (local) audio facilities are
     * unavailable on the platform.
     */
    public void play()
    {
        if (log.isDebugEnabled())
        {
            log.debug("play - entering");
        }

        playSound(false); // don't play in loop mode.
    }

    /**
     * Starts the {@link org.havi.ui.HSound HSound} class looping from the
     * beginning of its associated audio data. If the sample data has not been
     * completely loaded, this method has no effect.
     * <p>
     * When the audio data has been played in its entirety, then it should be
     * played again from the beginning of its associated data, so as to cause a
     * &quot;seamless&quot; continuous (infinite) audio playback - until the
     * next stop, or play method is invoked. Note that the audio data is played
     * back asynchronously, there is no mechanism for synchronization with other
     * classes presenting sounds, images, or video.
     * <p>
     * This method may fail &quot;silently&quot; if (local) audio facilities are
     * unavailable on the platform.
     */
    public void loop()
    {
        if (log.isDebugEnabled())
        {
            log.debug("loop - entering");
        }

        playSound(true); // play in loop mode
    }

    /*
     * (non-Javadoc)
     *
     * Play the sound, optionally, in loop mode <p>
     *
     * @param loop true to play sound in loop mode
     */
    private void playSound(boolean loop)
    {
        if (log.isDebugEnabled())
        {
            log.debug("playSound - entering");
        }

        if (sound == null || playback != null)
            return;

        try
        {
            //default mute to false, gain to no gain (0.0F)
            playback = sound.play(owner, new Time(0L), loop, cc, false, 0.0F);
        }
        catch (MPEMediaError err)
        {
            return;
        }
    }

    /**
     * Stops the {@link org.havi.ui.HSound HSound} class playing its associated
     * audio data.
     * <p>
     * Note that, if a play or loop method is invoked, after a stop, then
     * presentation of the audio data will restart from the beginning of the
     * audio data, rather than from the position where the audio data was
     * stopped.
     */
    public void stop()
    {
        if (log.isDebugEnabled())
        {
            log.debug("stop - entering");
        }

        if (playback != null)
        {
            playback.stop();
            playback = null;
        }
    }

    /**
     * If the {@link org.havi.ui.HSound HSound} object is playing / looping then
     * it will be stopped. The dispose method then discards all sample resources
     * used by the {@link org.havi.ui.HSound HSound} object. This mechanism
     * resets the {@link org.havi.ui.HSound HSound} object to the state before a
     * load() method was invoked.
     */
    public void dispose()
    {
        if (log.isDebugEnabled())
        {
            log.debug("dispose - entering");
        }

        // stop playing
        stop();

        if (sound != null)
        {
            sound.dispose();
            sound = null;
        }
    }

    /**
     * Stop playing the sound and release associated resources.
     */
    protected void finalize()
    {
        if (log.isDebugEnabled())
        {
            log.debug("finalize - entering");
        }

        // This ensures that the native resources are released.
        dispose();
    }

    class HSoundOwner implements PlaybackOwner
    {

        public void playbackStopped(int reason)
        {
            // We don't really care why we stopped. Just release the playback
            playback = null;
        }

    }

}
