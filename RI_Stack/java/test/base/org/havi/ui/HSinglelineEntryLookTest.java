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

import org.havi.ui.event.*;
import junit.framework.*;
import org.cablelabs.gear.util.*;
import java.awt.*;

/**
 * Tests {@link #HSinglelineEntryLook}.
 * 
 * @author Tom Henriksen
 * @version $Revision: 1.3 $, $Date: 2002/11/07 21:14:09 $
 */
public class HSinglelineEntryLookTest extends AbstractLookTest
{
    /**
     * Standard constructor.
     */
    public HSinglelineEntryLookTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public HSinglelineEntryLookTest(String s, Object params)
    {
        super(s, params);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        try
        {
            // junit.textui.TestRunner.run(HSinglelineEntryTest.class);
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        htestlook = new HSinglelineEntryLook();

        hlook = new HSinglelineEntryLook()
        {
            public void fillBackground(Graphics g, HVisible visible, int state)
            {
                super.fillBackground(g, visible, state);
                backgroundCall = callIndex++;
            }

            public void renderBorders(Graphics g, HVisible visible, int state)
            {
                super.renderBorders(g, visible, state);
                borderCall = callIndex++;
            }

            public void renderVisible(Graphics g, HVisible visible, int state)
            {
                super.renderVisible(g, visible, state);
                visibleCall = callIndex++;
            }
        };

        // so we can be in ALL states
        // don't do automatic state transitions
        hvisible = new HSinglelineEntry()
        {
            public void processHActionEvent(HActionEvent e)
            {
            }

            public void processHFocusEvent(HFocusEvent e)
            {
            }
        };
        hvisible.setFont(font);
        hvisible.setBackground(Color.white);
        hvisible.setForeground(Color.red);
        ((HSinglelineEntry) hvisible).setMaxChars(100);
    }

    /**
     * Suite.
     */
    public static TestSuite suite() throws Exception
    {
        return AbstractLookTest.suite(HSinglelineEntryLookTest.class);
    }

    /**
     * Calculates the maximum size of the content handled by the tested class
     * contained within the given <code>HVisible</code>.
     * 
     * @param v
     *            the component containing the content
     * @return the maximum dimensions of the content in question
     */
    protected Dimension getContentMaxSize(HVisible v)
    {
        int maxW = 0, maxH = 0;
        Font f = v.getFont();

        if (f != null)
        {
            FontMetrics metrics = v.getFontMetrics(f);
            int lineCount = 0;

            String content = v.getTextContent(HState.NORMAL_STATE);
            if (content != null)
            {
                maxW = metrics.stringWidth(content) + metrics.getMaxDescent() * 2;
                maxH = TextRender.getFontHeight(metrics) + metrics.getMaxDescent() * 2;
            }
        }

        return new Dimension(maxW, maxH);
    }

    /**
     * Calculates the minimum size of the content handled by the tested class
     * contained within the given <code>HVisible</code>.
     * 
     * @param v
     *            the component containing the content
     * @return the minimum dimensions of the content in question
     */
    protected Dimension getContentMinSize(HVisible v)
    {
        // Same
        return getContentMaxSize(v);
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addStateContent(HVisible v)
    {
        v.setTextContent("ALL_STATES", v.NORMAL_STATE);
    }

    /**
     * Test default constructor.
     * <ul>
     * <li>HSinglelineEntryLook()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HSinglelineEntryLook()", (HSinglelineEntryLook) hlook);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HSinglelineEntryLook look)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", look);
    }

    /**
     * Used to determine if the look being tested supports scaling or not.
     * 
     * @return <code>true</cod> if scaling is supported;
     * <code>false</code> otherwise
     */
    protected boolean isScalingSupported()
    {
        return false;
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addSizedContent(HVisible v)
    {
        v.setTextContent("Howdy", v.NORMAL_STATE);
    }
}
