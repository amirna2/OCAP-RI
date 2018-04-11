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
package org.cablelabs.impl.media.presentation;

import junit.framework.TestCase;

import org.dvb.media.VideoFormatControl;
import org.havi.ui.HScreenRectangle;

import org.cablelabs.impl.media.JMFTests;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.mpe.ScalingBoundsDfc;

/**
 * AbstractVideoPresentationTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class AbstractVideoPresentationTest extends TestCase
{

    private CannedAbstractVideoPresentation pres;

    private CannedVideoPresentationContext context;

    /**
	 * 
	 */
    public AbstractVideoPresentationTest()
    {
        this("AbstractVideoPresentationTest");
    }

    /**
     * @param arg0
     */
    public AbstractVideoPresentationTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(AbstractVideoPresentationTest.class);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        JMFTests.setUpJMF();

        context = new CannedVideoPresentationContext();
        pres = new CannedAbstractVideoPresentation(context, true, CannedMediaAPI.dfltSB);
    }

    public void tearDown() throws Exception
    {
        pres = null;
        context = null;

        JMFTests.tearDownJMF();

        super.tearDown();
    }

    // Test Section

    public void testGetInputSize() throws Exception
    {
        pres.start();
        assertNotNull("Returned size should not be null", pres.getInputSize());
    }

    public void testSetBounds() throws Exception
    {
        HScreenRectangle hsrSrc = new HScreenRectangle(0.0f, 0.0f, 1.0f, 1.0f);
        HScreenRectangle hsrDest = new HScreenRectangle(0.5f, 0.5f, 0.5f, 0.5f);
        assertTrue(pres.setBounds(new ScalingBounds(hsrSrc, hsrDest)));

        // Now we'll try a ScalingBoundsDfc
        ScalingBoundsDfc sbdfc = new ScalingBoundsDfc(CannedMediaAPI.dfltSB, VideoFormatControl.DFC_PLATFORM);
        assertTrue(pres.setBounds(sbdfc));
    }

    public void testGetBounds() throws Exception
    {
        assertEquals("Bounds does not match", CannedMediaAPI.dfltSB, pres.getBounds());
    }

    public void testShowAndHide() throws Exception
    {
        assertTrue(pres.getShowVideo());
        pres.hide();
        assertFalse(pres.getShowVideo());
        pres.show();
        assertTrue(pres.getShowVideo());
    }

    public void testSetDecoderFormatConversion() throws Exception
    {
        pres.setDecoderFormatConversion(VideoFormatControl.DFC_PROCESSING_LB_16_9);
    }

}
