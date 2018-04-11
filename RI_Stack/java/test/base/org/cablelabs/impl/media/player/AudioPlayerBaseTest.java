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
import java.net.URL;

import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.EndOfMediaEvent;
import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.protocol.DataSource;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;

import org.dvb.media.StopByResourceLossEvent;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.SoundManager;
import org.cablelabs.impl.media.JMFFactory;
import org.cablelabs.impl.media.source.CannedOcapServiceDataSource;
import org.cablelabs.impl.sound.PlaybackOwner;
import org.cablelabs.impl.sound.mpe.CannedPlayback;
import org.cablelabs.impl.sound.mpe.CannedSound;
import org.cablelabs.impl.sound.mpe.CannedSoundMgr;

public class AudioPlayerBaseTest extends TestCase
{
    private AbstractAudioPlayer player;

    private CannedControllerListener listener;

    private PlayerHelper playerHelper;

    private CannedSoundMgr soundManager;

    private JMFFactory playerFactory;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(AudioPlayerBaseTest.suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(AudioPlayerBaseTest.class);
        ImplFactory factory = new AudioPlayerFactory();

        // Add the PlayerInterfaceTest and control tests
        suite.addTest(PlayerInterfaceTest.isuite(factory));
        suite.addTest(MediaTimePositionControlTest.isuite(factory));
        suite.addTest(PlayerStopTimeTest.isuite(factory));

        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        playerFactory = new AudioPlayerFactory();
        playerFactory.setUp();

        soundManager = (CannedSoundMgr) ManagerManager.getInstance(SoundManager.class);

        AudioPlayerFactory factory = new AudioPlayerFactory();
        player = (AbstractAudioPlayer) factory.createImplObject();

        listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        playerHelper = new PlayerHelper(player, listener);
    }

    public void tearDown() throws Exception
    {
        playerFactory.tearDown();
        player.close();
        player.removeControllerListener(listener);
        listener = null;
        player = null;
        playerHelper = null;
        soundManager = null;
        playerFactory = null;

        super.tearDown();
    }

    private void waitForSoundPlayed(CannedSound sound)
    {
        long time = System.currentTimeMillis();
        long endTime = time + 1000;
        while (!sound.cannedGetPlayed() && System.currentTimeMillis() < endTime)
        {
            Thread.yield();
        }
    }

    public void testDoAcquirePrefetchResources()
    {
        soundManager.reset();
        Object ret = player.doAcquirePrefetchResources();
        if (ret != null && ret instanceof Throwable)
        {
            ((Throwable) ret).printStackTrace();
        }
        assertTrue("Unexpected error prefetching resources " + ret, ret == null);
        assertTrue(soundManager.cannedGetSoundCreated());
    }

    public void testDoAcquireReleasePrefetchResourcesBadURL()
    {
        DataSource ds = player.getSource();
        try
        {
            MediaLocator ml = new MediaLocator("xxx://asdfasdf");
            CannedOcapServiceDataSource newDs = new CannedOcapServiceDataSource();
            newDs.setLocator(ml);
            player.setSource(newDs);

            soundManager.reset();
            player.doAcquirePrefetchResources();
            assertTrue(!soundManager.cannedGetSoundCreated());

            player.doReleasePrefetchedResources();
        }
        catch (Exception exc)
        {
            fail("Could not set data source on player " + exc);
        }
        finally
        {
            try
            {
                player.setSource(ds);
            }
            catch (Exception exc)
            {
                //
                // I don't see how this could happen since
                // we got this from the player at the start
                // of the test
                // 
                fail("Unexpected Exception resetting player source");
            }
        }
    }

    public void testDoAcquirePrefetchResourcesNullSoundReturned()
    {
        soundManager.reset();
        soundManager.cannedSetReturnNullSound(true);
        Object ret = player.doAcquirePrefetchResources();
        assertTrue(soundManager.cannedGetSoundCreated());
        assertTrue(ret instanceof Exception);
    }

    public void testDoReleasePrefetchedResources()
    {
        soundManager.reset();
        CannedSound sound = new CannedSound();
        soundManager.cannedSetCachedSound(sound);

        player.doAcquirePrefetchResources();
        assertTrue(soundManager.cannedGetSoundCreated());
        player.doReleasePrefetchedResources();
        assertTrue(sound.cannedGetDisposed());
    }

    public void testDoReleasePrefetchedResourcesNotAcquired()
    {
        player.doReleasePrefetchedResources();
        // this shouldn't do anything, just make sure we don't get an
        // exception
    }

    public void testDoReleasePrefetchResourcesNullSoundReturned()
    {
        soundManager.reset();
        soundManager.cannedSetReturnNullSound(true);
        Object ret = player.doAcquirePrefetchResources();
        assertTrue(soundManager.cannedGetSoundCreated());
        assertTrue(ret instanceof Exception);
        player.doReleasePrefetchedResources();
        // again, make sure an exception isn't thrown
    }

    public void testDoStartDecoding() throws Exception
    {
        soundManager.reset();
        CannedSound sound = new CannedSound();
        CannedPlayback playback = new CannedPlayback();
        sound.cannedSetPlayback(playback);
        soundManager.cannedSetCachedSound(sound);

        playerHelper.startPlayer();

        listener.reset();
        waitForSoundPlayed(sound);
        assertTrue(sound.cannedGetPlayed());

    }

    public void testDoStopDecoding() throws Exception
    {
        soundManager.reset();
        CannedSound sound = new CannedSound();
        CannedPlayback playback = new CannedPlayback();
        sound.cannedSetPlayback(playback);
        soundManager.cannedSetCachedSound(sound);
        Object ret = player.doAcquirePrefetchResources();
        assertTrue(ret == null);

        playerHelper.startPlayer();
        waitForSoundPlayed(sound);
        assertTrue(sound.cannedGetPlayed());

        playerHelper.stopPlayer();
        listener.reset();
        listener.waitForEvents(1);
        assertTrue(playback.cannedGetStopped());
    }

    public void testDoStartDecodingNullPlayback() throws Exception
    {
        soundManager.reset();
        CannedSound sound = new CannedSound();
        sound.cannedSetReturnNullPlayback(true);
        soundManager.cannedSetCachedSound(sound);

        player.start();
        //
        // we should get a stop event since the playback
        // fails
        //
        boolean eventReceived = listener.waitForStopEvent();

        assertTrue(eventReceived);
        assertTrue(sound.cannedGetPlayed());
    }

    public void testDecodeErrorEvent()
    {
        listener.reset();
        CannedSound sound = new CannedSound();
        soundManager.cannedSetCachedSound(sound);

        playerHelper.startPlayer();
        waitForSoundPlayed(sound);
        PlaybackOwner pOwner = sound.cannedGetPlaybackOwner();
        assertTrue("Playback owner is null", pOwner != null);

        listener.reset();
        pOwner.playbackStopped(PlaybackOwner.DECODE_ERROR);
        listener.waitForEvents(1);
        assertTrue(listener.events.size() > 0);
        ControllerEvent evt = listener.getEvent(0);
        assertTrue(evt.getSource().equals(player));
        assertTrue(evt instanceof ControllerErrorEvent);
    }

    public void testEndOfContentEvent()
    {
        listener.reset();
        CannedSound sound = new CannedSound();
        soundManager.cannedSetCachedSound(sound);

        playerHelper.startPlayer();
        waitForSoundPlayed(sound);
        PlaybackOwner pOwner = sound.cannedGetPlaybackOwner();
        assertTrue("Playback owner is null", pOwner != null);
        listener.reset();
        pOwner.playbackStopped(PlaybackOwner.END_OF_CONTENT);
        listener.waitForEvents(1);
        assertTrue(listener.events.size() > 0);
        ControllerEvent evt = listener.getEvent(0);
        assertTrue(evt.getSource().equals(player));
        assertTrue(evt instanceof EndOfMediaEvent);
        EndOfMediaEvent endEvent = (EndOfMediaEvent) evt;
        assertTrue(endEvent.getTargetState() == player.getState());
    }

    public void testPreemptedEvent()
    {
        listener.reset();
        CannedSound sound = new CannedSound();
        soundManager.cannedSetCachedSound(sound);

        playerHelper.startPlayer();
        waitForSoundPlayed(sound);
        PlaybackOwner pOwner = sound.cannedGetPlaybackOwner();
        assertTrue("Playback owner is null", pOwner != null);
        listener.reset();
        pOwner.playbackStopped(PlaybackOwner.PREEMPTED);
        listener.waitForEvents(1);
        assertTrue(listener.events.size() > 0);
        ControllerEvent evt = listener.getEvent(0);
        assertTrue(evt.getSource().equals(player));
        assertTrue(evt instanceof StopByResourceLossEvent);
    }

    //    
    // public void testDoGetMediaTimeNoPlayback()
    // {
    // Time time = player.doGetMediaTime();
    // assertTrue(time == null);
    // }

    public void testDoGetMediaTimeWhilePlaying() throws Exception
    {
        soundManager.reset();
        CannedSound sound = new CannedSound();
        CannedPlayback playback = new CannedPlayback();
        sound.cannedSetPlayback(playback);
        soundManager.cannedSetCachedSound(sound);

        playerHelper.startPlayer();
        waitForSoundPlayed(sound);

        Time t = new Time(12300000000L); // 12.3 seconds
        playback.setTime(t);

        Time time = player.getMediaTime();
        assertTrue(time != null);
        //
        // since the player is playing, the player forwards to the media time
        // and then resumes playing, so the returned value will be after the
        // time set in the playback
        //
        assertTrue(time.getSeconds() - t.getSeconds() < 2);
    }

    public void testDoSetMediaTime() throws Exception
    {
        soundManager.reset();
        CannedSound sound = new CannedSound();
        CannedPlayback playback = new CannedPlayback();
        Time time = new Time(123);
        sound.cannedSetPlayback(playback);
        soundManager.cannedSetCachedSound(sound);
        playerHelper.startPlayer();
        waitForSoundPlayed(sound);
        player.setMediaTime(time);

        assertTrue(time.equals(playback.getTime()));
    }

    public void testDoGetVisualComponent()
    {
        Component c = player.doGetVisualComponent();
        //
        // no visual component for an audio player
        //
        assertTrue(c == null);
    }

    private static class AudioPlayerFactory extends JMFFactory
    {
        private SoundManager oldSoundMgr;

        private CannedSoundMgr soundManager;

        public void setUp() throws Exception
        {
            super.setUp();

            oldSoundMgr = (SoundManager) ManagerManager.getInstance(SoundManager.class);
            soundManager = (CannedSoundMgr) CannedSoundMgr.getInstance();
            ManagerManagerTest.updateManager(SoundManager.class, CannedSoundMgr.class, true, soundManager);
        }

        public void tearDown() throws Exception
        {
            ManagerManagerTest.updateManager(SoundManager.class, CannedSoundMgr.class, true, oldSoundMgr);
            oldSoundMgr = null;
            soundManager = null;

            super.tearDown();
        }

        public Object createImplObject() throws Exception
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            AbstractAudioPlayer player = new AbstractAudioPlayer(ccm.getCurrentContext());

            URL dataURL = null;
            dataURL = getClass().getResource("/hostapp.properties");
            if (dataURL == null)
            {
                dataURL = getClass().getResource("hostapp.properties");
            }
            if (dataURL == null)
            {
                throw new RuntimeException("Could not construct test URL");
            }
            MediaLocator locator = new MediaLocator(dataURL);
            CannedOcapServiceDataSource ds = new CannedOcapServiceDataSource();
            ds.setLocator(locator);
            player.setSource(ds);

            return player;
        }

    }
}
