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

package org.havi.ui;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.sound.mpe.CannedCallerContext;
import org.cablelabs.test.GUITest;
import org.cablelabs.test.TestUtils;

/**
 * Tests {@link #HSound}.
 * 
 * @author Aaron Kamienski
 * @author Tom Henriksen
 * @version $Id: HSoundTest.java,v 1.5 2002/06/03 21:32:21 aaronk Exp $
 */
public class HSoundTest extends GUITest
{

    /** True if mattes are supported on this platform */
    boolean soundSupported;

    private CallerContextManager save;

    private CCMgr ccmgr;

    private HSound hsound;

    /**
     * Standard constructor.
     */
    public HSoundTest(String str)
    {
        super(str);
        soundSupported = TestSupport.getProperty("snap2.havi.test.soundSupported", true);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HSoundTest.class);
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccmgr = new CCMgr(save));

    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        if (hsound != null)
        {
            hsound.dispose();
        }
        if (save != null)
        {
            ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
        }
        super.tearDown();
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HSound.class, Object.class);
    }

    /**
     * Test the single default constructor.
     * <ul>
     * <li>HSound()
     * </ul>
     */
    public void testConstructors()
    {
        // Nothing to really test.
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HSound.class);
    }

    /**
     * Tests load(String).
     * <ul>
     * <li>after loading (asynchronously), should be able to playback
     * <li>if sound already contains data should:
     * <ol>
     * <li>stop the sample if playing
     * <li>dispose of old data AS IF dispose had been called (cannot test)
     * <li>load the new data
     * </ol>
     * <li>IO problem should throw IOException (e.g., file not found)
     * <li>SecurityException should be thrown... when?
     * </ul>
     */
    public void testLoadFile() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!soundSupported) return;

        // Try invalid data
        CannedCallerContext cc = new CannedCallerContext();
        final Exception[] exc = new Exception[1];
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {

                // Try invalid data
                hsound = new HSound();
                try
                {

                    try
                    {
                        hsound.load((String) null);
                        fail("Expected an exception");
                    }
                    catch (IllegalArgumentException expected)
                    {
                    }
                    catch (IOException expected)
                    {
                    }
                    catch (NullPointerException expected)
                    {
                    }

                    try
                    {
                        hsound.load("unfound.data");
                        fail("Expected an IOException for unfound data");
                    }
                    catch (IOException expected)
                    {
                    }

                    hsound.load(TestSupport.getBaseDirectory() + "/mpeenv.ini");
                    // this should fail silently if data is not a valid sound

                    // Load a sound and start it looping
                    startPlayback();
                    delay(3000);
                    // Load another sound, should stop previous sound

                    hsound.load(TestSupport.getBaseDirectory() + "/mpeenv.ini");
                    assertQuestions("testLoad: ->Should stop previously played sound", new String[] {
                            "Did the sound play for a period?", "Did the sound stop playing?" });
                    // Play new sound
                    checkPlay("a single cymbal");
                }
                catch (Exception excCaught)
                {
                    exc[0] = excCaught;
                }
            }
        });

        if (exc[0] != null)
        {
            throw exc[0];
        }
    }

    /**
     * Test load(URL).
     * <ul>
     * <li>after loading (asynchronously), should be able to playback
     * <li>if sound already contains data should:
     * <ol>
     * <li>stop the sample if playing
     * <li>dispose of old data AS IF dispose had been called (cannot test)
     * <li>load the new data
     * </ol>
     * <li>IO problem should throw IOException (e.g., URL not found)
     * <li>SecurityException should be thrown... when?
     * </ul>
     */
    public void testLoadURL() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!soundSupported) return;

        CannedCallerContext cc = new CannedCallerContext();
        final Exception[] exc = new Exception[1];
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {

                // Try invalid data
                hsound = new HSound();
                try
                {
                    hsound.load((URL) null);
                    fail("Expected an exception");
                }
                catch (IllegalArgumentException expected)
                {
                }
                catch (IOException expected)
                {
                }
                catch (NullPointerException expected)
                {
                }

                try
                {
                    hsound.load(new URL("http://invalid.cablelabs.org/invalid"));
                    fail("Expected an IOException for unfound data");
                }
                catch (IOException expected)
                {
                }

                try
                {
                    hsound.load(getClass().getResource("HSoundTest.class"));
                }
                catch (Exception e)
                {
                    fail("Should fail silently if data is not a valid sound");
                }
                // Should test that it won't play

                // Load a sound and start it looping
                try
                {
                    startPlayback();
                    delay(3000);
                    // Load another sound, should stop previous sound
                    hsound.load(getClass().getResource("sounds/hat.wav"));
                    assertQuestions("testLoad: ->Should stop previously played sound", new String[] {
                            "Did the sound play for a period?", "Did the sound stop playing?" });
                    // Play new sound
                    checkPlay("a single cymbal");
                }
                catch (Exception excCaught)
                {
                    exc[0] = excCaught;
                }
            }
        });

        if (exc[0] != null)
        {
            throw exc[0];
        }
    }

    /**
     * Test set(byte[]).
     * <ul>
     * <li>after setting (synchronously), should be able to playback
     * <li>if sound already contains data should:
     * <ol>
     * <li>stop the sample if playing
     * <li>dispose of old data AS IF dispose had been called (cannot test)
     * <li>load the new data
     * </ol>
     * <li>IllegalArgumentException should be thrown for invalid sound data
     * </ul>
     * 
     * @todo re-enable after 5589 has been fixed
     */
    public void testSet() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!soundSupported) return;

        // Try invalid data
        hsound = new HSound();
        try
        {
            hsound.set(null);
            fail("Expected an exception");
        }
        catch (IllegalArgumentException expected)
        {
        }
        catch (NullPointerException expected)
        {
        }

        // Try invalid data
        try
        {
            hsound.set(new byte[1]);
            fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Load a sound and start it looping
        startPlayback();
        delay(3000);
        // Load another sound, should stop previous sound
        // Read a sound into a buffer
        URL url = getClass().getResource("sounds/hat.wav");
        InputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1)
            buffer.write(c);
        in.close();
        // Set the data
        hsound.set(buffer.toByteArray());
        assertQuestions("testLoad: ->Should stop previously played sound", new String[] {
                "Did the sound play for a period?", "Did the sound stop playing?" });
        // Play new sound
        checkPlay("a single cymbal");
    }

    /**
     * Tests play().
     * <ul>
     * <li>Has no effect if data hasn't been loaded
     * <li>Sound should play in it's entirety ONCE
     * <li>If currently in loop(), should cause it to play ONCE and stop
     * </ul>
     */
    public void testPlay() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!soundSupported) return;
        hsound = new HSound();

        hsound.load(getClass().getResource("sounds/hat.wav"));

        checkPlay("single cymbal");
    }

    private void checkPlay(String description)
    {
        // message("The following test will attempt to play a "+description+" sound");
        hsound = new HSound();
        hsound.play();
        assertQuestions("testPlay: ->Test playing sound ", new String[] { "Did you hear a " + description + " sound" });
    }

    /**
     * Tests loop().
     * <ul>
     * <li>Has no effect if data hasn't been loaded
     * <li>Sound should play in it's entirety until stop() is called
     * </ul>
     */
    public void testLoop() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!soundSupported) return;

        CannedCallerContext cc = new CannedCallerContext();
        final Exception[] exc = new Exception[1];
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                try
                {
                    hsound = new HSound();
                    hsound.load(getClass().getResource("sounds/handclap.wav"));

                    // message("The following test will attempt to play 3 seconds of a "+
                    // "repeated \"clapping\" sound");
                    hsound.loop();
                    try
                    {
                        Thread.sleep(3000);
                    }
                    catch (java.lang.InterruptedException e)
                    {
                    }
                    hsound.stop();
                    assertQuestions("testLoop: ->Test looping sound ",
                            new String[] { "Did you hear 3 seconds of a repeated \"clapping\" sound" });
                }
                catch (Exception excCaught)
                {
                    exc[0] = excCaught;
                }
            }
        });

        if (exc[0] != null)
        {
            throw exc[0];
        }
    }

    /**
     * Tests dispose().
     * <ul>
     * <li>Should stop playback (if playing or looping).
     * <li>Playback or loop() after dispose should be ineffectual
     * </ul>
     */
    public void testDispose() throws Exception
    {
        // Do not run this test if mattes are not supported on this platform.
        if (!soundSupported) return;

        CannedCallerContext cc = new CannedCallerContext();
        final Exception[] exc = new Exception[1];
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                try
                {
                    hsound = new HSound();
                    hsound.load(getClass().getResource("sounds/bass.wav"));

                    // message("The following test will play one bass drum sound");
                    hsound.play();

                    assertQuestions("testPlay: ->Test playing sound ",
                            new String[] { "Did you hear one bass drum sound" });

                    hsound.dispose();
                    hsound.play();

                    assertQuestions(
                            "testPlay: ->Test playing sound ",
                            new String[] { "Sound was disposed after last sound.  No sound should have played since last dialog" });
                }
                catch (Exception excCaught)
                {
                    exc[0] = excCaught;
                }
            }
        });

        if (exc[0] != null)
        {
            throw exc[0];
        }
    }

    /**
     * Plays a sound in a loop.
     */
    private void startPlayback() throws Exception
    {
        hsound = new HSound();
        hsound.load(getClass().getResource("sounds/bass.wav"));
        // message("A bass drum sound will be played repeatedly");
        hsound.loop();
    }
}
