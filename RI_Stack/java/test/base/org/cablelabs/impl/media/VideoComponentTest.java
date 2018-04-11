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

package org.cablelabs.impl.media;

import org.havi.ui.HComponent;
import org.havi.ui.HVideoComponentTest;
import org.havi.ui.HVideoDevice;

import org.cablelabs.impl.media.content.ocap.service.Handler;
import org.cablelabs.impl.media.decoder.Decoder;

/**
 * Tests VideoComponent.
 * 
 * @author Aaron Kamienski
 */
public class VideoComponentTest extends HVideoComponentTest
{
    /**
     * Tests that the expected video device is returned.
     */
    public void testVideoDevice_correct()
    {
        HVideoDevice videoDevice = hvideo.getVideoDevice();

        assertNotNull("Internal error - videoDevice is null", videoDevice);
        assertSame("Unexpected video device returned", videoDevice, hvideo.getVideoDevice());
    }

    /**
     * Tests paint() on first call.
     */
    public void X_testPaint_first()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests paint() when player is stopped.
     */
    public void X_testPaint_stopped()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests paint() following a "move" not caught by listeners.
     */
    public void X_testPaint_moved()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests event generation in paint() following a "move" not caught by
     * listeners.
     */
    public void X_testPaint_movedEvent()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests generation of HScreenLocationModifiedEvent if parent container is
     * moved.
     */
    public void X_testParentContainerMovedGeneratesEvent()
    {
        fail("Unimplemented test");
    }

    /**
     * Standard constructor.
     */
    public VideoComponentTest(String str)
    {
        super(str);
    }

    /**
     * Should be overridden to create subclass of HComponent.
     * 
     * @return the instance of HComponent to test
     */
    protected HComponent createHComponent()
    {
        return new VideoComponent(handler);
    }

    protected Handler handler;

    protected Decoder decoder;

    protected HVideoDevice device;

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        /*
         * device = new TestVideoDevice(); decoder = new TestDecoder(); player =
         * new TestPlayer();
         */
        device = null;
        decoder = null;
        handler = null;

        super.setUp();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(VideoComponentTest.class);
    }
}
