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

import java.awt.*;

/**
 * Tests {@link #HAnimateLook}.
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski (rewrite)
 * @version $Id: HAnimateLookTest.java,v 1.8 2002/06/03 21:32:10 aaronk Exp $
 */
public class HAnimateLookTest extends AbstractLookTest
{
    /**
     * Standard constructor.
     */
    public HAnimateLookTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public HAnimateLookTest(String s, Object params)
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

        // Create a "pristine" look for ancestor testing.
        htestlook = new HAnimateLook();

        hlook = new HAnimateLook()
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

        // don't do automatic state transitions
        hvisible = new HAnimation()
        {
            public void processHFocusEvent(HFocusEvent e)
            {
            }
        };
    }

    /**
     * Suite.
     */
    public static TestSuite suite() throws Exception
    {
        return AbstractLookTest.suite(HAnimateLookTest.class);
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
        for (int s = 0; s < 8; ++s)
        {
            int state = s | HVisible.NORMAL_STATE;

            Image[] array = v.getAnimateContent(state);
            if (array != null)
            {
                for (int i = 0; i < array.length; ++i)
                {
                    Image content = array[i];
                    int w, h;
                    if (content != null && (w = content.getWidth(null)) != -1 && (h = content.getHeight(null)) != -1)
                    {
                        maxW = Math.max(maxW, w);
                        maxH = Math.max(maxH, h);
                    }
                }
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
        int minW = Short.MAX_VALUE, minH = Short.MAX_VALUE;
        for (int s = 0; s < 8; ++s)
        {
            int state = s | HVisible.NORMAL_STATE;

            Image[] array = v.getAnimateContent(state);
            if (array != null)
            {
                for (int i = 0; i < array.length; ++i)
                {
                    Image content = array[i];
                    int w, h;
                    if (content != null && (w = content.getWidth(null)) != -1 && (h = content.getHeight(null)) != -1)
                    {
                        minW = Math.min(minW, w);
                        minH = Math.min(minH, h);
                    }
                }
            }
        }
        if (minW == Short.MAX_VALUE) minW = 0;
        if (minH == Short.MAX_VALUE) minH = 0;
        return new Dimension(minW, minH);
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
        for (int s = 0; s < 8; ++s)
        {
            int state = s | HVisible.NORMAL_STATE;

            v.setAnimateContent(new Image[] { TestSupport.getState(state) }, state);
        }
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
        for (int s = 0; s < 8; ++s)
        {
            int state = s | HVisible.NORMAL_STATE;
            v.setAnimateContent(new Image[] { TestSupport.getArrow(s % 4 + 1) }, state);
        }
    }

    /**
     * Creates content that would be displayed by the tested look. The content
     * is added to the given <code>HVisible</code> component to test appropriate
     * alignment of the content. Default implementation calls
     * {@link #addSizedContent(HVisible)}.
     * 
     * @param v
     *            the component to add content to
     */
    protected void addAlignedContent(HVisible v)
    {
        v.setAnimateContent(new Image[] { TestSupport.getArrow(4) }, HVisible.ALL_STATES);
    }

    /**
     * Used to determine if the look being tested supports scaling or not.
     * 
     * @return <code>true</cod> if scaling is supported;
     * <code>false</code> otherwise
     */
    protected boolean isScalingSupported()
    {
        return true;
    }

    /**
     * Test default constructor.
     * <ul>
     * <li>HAnimateLook()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HAnimateLook()", (HAnimateLook) hlook);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HAnimateLook look)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", look);
    }

    /**
     * Test showLook() given varying positions/states.
     */
    public void testShowLookPosition() throws Exception
    {
        HStaticAnimation animation = new HAnimation()
        {
            public void processHFocusEvent(HFocusEvent e)
            {
                // no automatic state changes
            }
        };
        // Create content for all states
        org.cablelabs.gear.util.ImagePortfolio images = new org.cablelabs.gear.util.ImagePortfolio();
        for (int i = 0; i < 10; ++i)
        {
            images.addImage("" + i, getClass().getResource("/images/numbers_0" + i + ".gif"), 0);
        }
        for (int s = 0; s < 8; ++s)
        {
            int state = s | HState.NORMAL_STATE;
            Image content[] = new Image[10];
            for (int i = 0; i < 10; ++i)
                content[i] = images.getImage("" + mapStateAndI(state, i));
            animation.setAnimateContent(content, state);
        }

        animation.setBackground(Color.magenta);
        animation.setSize(animation.getPreferredSize());

        HScene testScene = getHScene();
        testScene.setBackground(Color.black);
        testScene.add(animation);

        try
        {
            animation.setSize(animation.getPreferredSize());
            testScene.show();

            // Check each state
            // Check multiple positions
            for (int s = 0; s < 8; ++s)
            {
                int state = s | HState.NORMAL_STATE;

                try
                {
                    TestSupport.setInteractionState(animation, state);
                }
                catch (IllegalArgumentException e)
                {
                    /*
                     * System.out.println("State not allowed: "+
                     * TestSupport.getStateName(state));
                     */
                    continue;
                }

                // Try 3 positions
                // NORMAL 0, 1, 2, ...
                // FOCUSED 1, 2, 3, ...
                // ACTIONED 2, 3, 4, ...
                // ACTIONED_FOCUSED 3, 4, 5, ...
                // DISABLED 4, 5, 6, ...
                // DISABLED_FOCUSED 5, 6, 7, ...
                // DISABLED_ACTIONED 6, 7, 8, ...
                // DISABLED_ACTIONED_FOCUSED 7, 8, 9, ...
                for (int p = s; p < s + 3; ++p)
                {
                    final int pos = p % 10;

                    animation.setPosition(pos);
                    TestSupport.checkDisplay(animation, "Display positional content", new String[] {
                            "Is content displayed (not just bg)?",
                            "Is content the number " + mapStateAndI(state, pos) + "?" }, "showPosition" + state + "_"
                            + pos, this);
                }
            }
        }
        finally
        {
            testScene.remove(animation);
        }
    }

    private int mapStateAndI(int state, int i)
    {
        return (i + (state & ~HState.NORMAL_STATE)) % 10;
    }
}
