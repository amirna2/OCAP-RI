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
import org.cablelabs.test.*;
import junit.framework.*;
import org.cablelabs.gear.util.*;
import java.awt.*;

/**
 * Tests {@link #HMultilineEntryLook}.
 * 
 * @author Tom Henriksen
 * @version $Revision: 1.3 $, $Date: 2002/11/07 21:14:07 $
 */
public class HMultilineEntryLookTest extends HSinglelineEntryLookTest
{
    /**
     * Standard constructor.
     */
    public HMultilineEntryLookTest(String s)
    {
        super(s);
    }

    /**
     * Parameterized test constructor.
     */
    public HMultilineEntryLookTest(String s, Object params)
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
        hlook = new HMultilineEntryLook();

        hvisible = new HMultilineEntry()
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
        ((HMultilineEntry) hvisible).setMaxChars(100);
    }

    public void testAncestry()
    {
        TestUtils.testExtends(hlook.getClass(), HSinglelineEntryLook.class);
    }

    /**
     * Suite.
     */
    public static TestSuite suite() throws Exception
    {
        return AbstractLookTest.suite(HMultilineEntryLookTest.class);
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

            String content = v.getTextContent(v.NORMAL_STATE);
            if (content != null)
            {
                /* !!!FINISH!!! */
                /* maxCharsPerLine!!! */

                // Calculate width based on 30 chars per line
                // Calculate word wrapping
                // Calculate height based on that

                maxW = 30 * metrics.charWidth('a') + metrics.getMaxDescent() * 2;
                String[] lines = TextLines.getLines(TextLines.breakLines(content, maxW, metrics));
                maxH = ((TextRender.getFontHeight(metrics) + metrics.getMaxDescent()) * lines.length)
                        + metrics.getMaxDescent() * 2;
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
        v.setTextContent("ALL_STATES\nALL_STATES", v.ALL_STATES);
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
        v.setTextContent("Hello, world!\n" + "Hola, mundo!\n" + "Bonjour, monde!\n" + "Hallo, Welt!\n"
                + "Ciao, mondo!\n", HVisible.NORMAL_STATE);
    }

    /**
     * Test default constructor.
     * <ul>
     * <li>HMultilineEntryLook()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HMultilineEntryLook()", (HMultilineEntryLook) hlook);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HMultilineEntryLook look)
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

    private void forceRepaint(HVisible visible)
    {
        visible.getLook().showLook(visible.getGraphics(), visible, HState.NORMAL_STATE);
    }

    private void compareLineBreaks(String title, int[] retrieved, int[] set)
    {
        assertEquals(title + " The line break count is incorrect", set.length, retrieved.length);
        for (int x = 0; x < retrieved.length; x++)
        {
            assertEquals(title + "  The line break at " + set[x] + " should have been " + retrieved[x], retrieved[x],
                    set[x]);
        }
    }

    private void checkVisibleBreaks(HVisible visible, String title, String content, int[] breaks)
    {
        String sizeString = "0123456789\n0123456789";

        // Size the component so 2 lines are visible. Now we can perform the
        // visible
        // line break test.
        visible.setTextContent(sizeString, HState.NORMAL_STATE);
        visible.setSize(visible.getLook().getMaximumSize(visible));

        // Set the text content and manually paint it so the line breaks have
        // been established. Set the cursor to the start before the test begins.
        visible.setTextContent(content, HState.NORMAL_STATE);
        ((HMultilineEntry) visible).setCaretCharPosition(0);
        forceRepaint(visible);

        int[] allLineBreaks = ((HMultilineEntryLook) visible.getLook()).getSoftLineBreakPositions(visible);
        ((HMultilineEntry) visible).caretNextLine();
        forceRepaint(visible);

        for (int x = 1; x < allLineBreaks.length - 1; x++)
        {
            compareLineBreaks(title,
                    ((HMultilineEntryLook) visible.getLook()).getVisibleSoftLineBreakPositions(visible), new int[] {
                            breaks[x - 1], breaks[x] });

            ((HMultilineEntry) hvisible).caretNextLine();
            forceRepaint(visible);
        }
    }

    /**
     * Tests getSoftLineBreakPositions() and getVisibleSoftLineBreakPositions().
     * <ul>
     * <li>
     * </ul>
     */
    public void testGetSoftLineBreakPositions()
    {
        String testString = "This\nis\na\ntest\nto\nsee\nif\nthis\nwill\nwork.";
        int[] lineBreaks = { 0, 5, 8, 10, 15, 18, 22, 25, 30, 35 };
        String testString2 = "Test\nthe\ncomponent\nto\nsee\nif\nthe\nline\nbreak\nmethods\nare\nworking";
        int[] lineBreaks2 = { 0, 5, 9, 19, 22, 26, 29, 33, 38, 44, 52, 56 };
        String testString3 = "Jack\nand Jill\nran\nup the\nhill\nto\nfetch\na pail\nof\nwater.\nYou\nknow\nthe rest.";
        int[] lineBreaks3 = { 0, 5, 14, 18, 25, 30, 33, 39, 46, 49, 56, 60, 65 };

        HScene testScene = getHScene();
        try
        {
            testScene.add(hvisible);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();

            hvisible.setSize(100, 100);
            hvisible.setTextContent(testString, HState.NORMAL_STATE);
            hvisible.getLook().showLook(hvisible.getGraphics(), hvisible, HState.NORMAL_STATE);
            compareLineBreaks("Line Break Test #1:",
                    ((HMultilineEntryLook) hvisible.getLook()).getSoftLineBreakPositions(hvisible), lineBreaks);

            hvisible.setTextContent(testString2, HState.NORMAL_STATE);
            hvisible.getLook().showLook(hvisible.getGraphics(), hvisible, HState.NORMAL_STATE);
            compareLineBreaks("Line Break Test #2:",
                    ((HMultilineEntryLook) hvisible.getLook()).getSoftLineBreakPositions(hvisible), lineBreaks2);

            hvisible.setTextContent(testString3, HState.NORMAL_STATE);
            hvisible.getLook().showLook(hvisible.getGraphics(), hvisible, HState.NORMAL_STATE);
            compareLineBreaks("Line Break Test #3:",
                    ((HMultilineEntryLook) hvisible.getLook()).getSoftLineBreakPositions(hvisible), lineBreaks3);

            checkVisibleBreaks(hvisible, "Visible Line Break Test #1:", testString, lineBreaks);
            checkVisibleBreaks(hvisible, "Visible Line Break Test #2:", testString2, lineBreaks2);
            checkVisibleBreaks(hvisible, "Visible Line Break Test #3:", testString3, lineBreaks3);
        }
        finally
        {
            testScene.remove(hvisible);
        }
    }

    /**
     * Tests getCaretPosition{Previous|Next}Line()
     * <ul>
     * <li>Returns the caret position if the caret was moved to the
     * next/previous line (if possible)
     * <li>Does not (need to) do anything if cannot be moved further
     * </ul>
     */
    public void testGetCaretPosition()
    {
        String content = "Test\nthe\ncomponent\nto\nsee\nif\nthe\nline\nbreak\nmethods\nare\nworking";
        int[] forwardOffset = { 8, 12, 21, 24, 28, 31, 35, 40, 46, 54, 58 };
        int[] backwardOffset = { 2, 7, 11, 21, 24, 28, 32, 36, 41, 47, 55 };

        HScene testScene = getHScene();
        try
        {
            testScene.add(hvisible);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();

            hvisible.setTextContent(content, HState.NORMAL_STATE);

            ((HMultilineEntry) hvisible).setCaretCharPosition(4);
            forceRepaint(hvisible);

            HMultilineEntryLook multiLook = (HMultilineEntryLook) hvisible.getLook();
            int[] allLineBreaks = multiLook.getSoftLineBreakPositions(hvisible);

            for (int x = 0; x < allLineBreaks.length - 1; x++)
            {
                assertEquals("The saved caret position for line " + x + " should equal "
                        + "the return from method getCaretPositionNextLine.", forwardOffset[x],
                        multiLook.getCaretPositionNextLine(hvisible));

                ((HMultilineEntry) hvisible).caretNextLine();
                forceRepaint(hvisible);
            }

            ((HMultilineEntry) hvisible).setCaretCharPosition(Integer.MAX_VALUE);
            forceRepaint(hvisible);

            for (int x = allLineBreaks.length - 2; x >= 0; x--)
            {
                assertEquals("The saved caret position for line " + x + " should equal "
                        + "the return from method getCaretPositionPreviousLine.", backwardOffset[x],
                        multiLook.getCaretPositionPreviousLine(hvisible));

                ((HMultilineEntry) hvisible).caretPreviousLine();
                forceRepaint(hvisible);
            }
        }
        finally
        {
            testScene.remove(hvisible);
        }
    }

    /**
     * Tests getCaretCharPositionForLine()
     * <ul>
     * <li>
     * </ul>
     */
    public void testGetCaretPositionForLine()
    {
        String content = "Line #0\nLine #1\nLine #2\nLine #3\nLine #4\nLine #5\nLine #6\nLine #7\nLine #8\nLine #9.";
        int lineLength = 8;
        int lines = 10;
        HMultilineEntry multiLine = (HMultilineEntry) hvisible;
        HMultilineEntryLook multiLook = (HMultilineEntryLook) multiLine.getLook();

        HScene testScene = getHScene();
        try
        {
            int caret = 0;

            testScene.add(hvisible);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();

            hvisible.setTextContent(content, HState.NORMAL_STATE);

            // Initially place the caret at the end of the first line.
            caret = multiLine.setCaretCharPosition(7);
            forceRepaint(hvisible);
            int[] allLineBreaks = multiLook.getSoftLineBreakPositions(hvisible);

            for (int x = 0; x < lineLength - 1; x++)
            {

                multiLine.setCaretCharPosition(x);
                multiLine.getLook().showLook(multiLine.getGraphics(), multiLine, HState.NORMAL_STATE);

                // Iterate 100 times. This should test things well.
                for (int y = 0; y < 100; y++)
                {

                    // Generate an index between 0 and 9.
                    int newLine = (int) (Math.random() * 10);

                    int newCaret = multiLook.getCaretCharPositionForLine(hvisible, newLine);

                    assertEquals("The calulated character position should equal "
                            + "the character position returned from getCaretCharPositionForLine.", newLine * lineLength
                            + x, newCaret);
                }
            }

            // Test to make sure we catch an illegal argument.
            try
            {
                multiLook.getCaretCharPositionForLine(hvisible, 1000);
                fail("Illegal line number was accepted to getCaretCharPositionForLine.");
            }
            catch (IllegalArgumentException e)
            {
            }
        }
        finally
        {
            testScene.remove(hvisible);
        }
    }
}
