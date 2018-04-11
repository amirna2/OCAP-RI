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

package org.cablelabs.impl.debug;

import org.cablelabs.impl.manager.GraphicsManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.SystemEventUtil;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.Vector;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBGraphics;
import org.dvb.ui.UnsupportedDrawingOperationException;
import org.havi.ui.HInvalidLookException;
import org.havi.ui.HState;
import org.havi.ui.HTextLook;
import org.havi.ui.HVisible;

/**
 * A Log4J Appender that displays its output using on-screen graphics.
 * <p>
 * Supports the following options:
 * <table border>
 * <tr>
 * <th>Option</th>
 * <th>Value</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>threshold</td>
 * <td>One of <code>"ALL"</code>, <code>"DEBUG"</code>, <code>"INFO"</code>,
 * <code>"WARN"</code>, <code>"ERROR"</code>, <code>"FATAL"</code>,
 * <code>"OFF"</code></td>
 * <td>Threshold required [<code>"ALL"</code>]
 * </tr>
 * <tr>
 * <td>bg</td>
 * <td>Color name or 32-bit ARGB</td>
 * <td>Background color [gray]</td>
 * </tr>
 * <tr>
 * <td>fg</td>
 * <td>Color name or 32-bit ARGB</td>
 * <td>Foreground color [black]</td>
 * </tr>
 * <tr>
 * <td>font</td>
 * <td>string</td>
 * <td>Font name [Tiresias]</td>
 * </tr>
 * <tr>
 * <td>size</td>
 * <td>integer</td>
 * <td>Font size [20]</td>
 * </tr>
 * <tr>
 * <td>valign</td>
 * <td>One of <code>"TOP"</code>, <code>"CENTER"</code>, <code>"JUSTIFY"</code>,
 * <code>"BOTTOM"</code></td>
 * <td>Vertical Alignment</td>
 * </tr>
 * <tr>
 * <td>halign</td>
 * <td>One of <code>"LEFT"</code>, <code>"CENTER"</code>, <code>"JUSTIFY"</code>, <code>"RIGHT"</code></td>
 * <td>Horizontal Alignment</td>
 * </tr>
 * <tr>
 * <td>rule</td>
 * <td>SRC_OVER, SRC, or 0-100 percentage</td>
 * <td>Alpha composition rule [SRC]</td>
 * </tr>
 * <tr>
 * <td>lines</td>
 * <td>integer</td>
 * <td>Number of lines of text to display at a time [1]</td>
 * </tr>
 * <tr>
 * <td>buffer</td>
 * <td>integer</td>
 * <td>Number of events to buffer [1]</td>
 * </tr>
 * </table>
 * 
 * <p>
 * 
 * Note that there is currently no option for modifying the placement of the
 * appender. As such it is not recommended that multiple OsdAppenders be used
 * concurrently as the OsdAppender always uses the same screen area.
 * 
 * @author Aaron Kamienski
 */
public class OsdAppender extends AppenderSkeleton
{
    /**
     * Default constructor.
     * 
     * @see #OsdAppender(Layout)
     */
    public OsdAppender()
    {
        this(null);
    }

    /**
     * Creates a new <code>OsdAppender</code> with the specified
     * <code>Layout</code>.
     * 
     * @param layout
     *            layout to use
     * 
     * @see OsdAppender
     */
    public OsdAppender(Layout layout)
    {
        // this.layout = (layout == null) ? (new SimpleLayout()) : layout;
        this.layout = layout;

        iniz();
    }

    /**
     * Implements the <code>abstract</code> {@link AppenderSkeleton#append} to
     * log the on-screen graphics.
     * 
     * @param event
     */
    protected void append(LoggingEvent event)
    {
        if (this.closed)
        {
            LogLog.warn("Not allowed to write to a closed appender.");
            return;
        }

        Layout layout = getLayout();
        if (layout == null)
        {
            SystemEventUtil.logRecoverableError(new Exception("No layout set for the appender named [" + name + "]."));
            return;
        }

        display(layout.format(event));

        if (layout.ignoresThrowable())
        {
             String[] s = event.getThrowableStrRep();
            if (s != null)
            {
                int len = s.length;
                for (int i = 0; i < len; ++i)
                {
                    appendDisplay(s[i] + Layout.LINE_SEP);
                }
            }
        }

        show();
    }

    // JavaDoc copied from parent
    public void close()
    {
        this.closed = true;
    }

    /**
     * Appender admits to having a layout, but can work without it (by using a
     * default SimpleLayout).
     */
    public boolean requiresLayout()
    {
        return true;
    }

    public void activateOptions()
    {
        super.activateOptions();

        if (bufferLength > 0 && lines < bufferLength) lines = bufferLength;

        if (lines > 0)
        {
            StringBuffer sb = new StringBuffer(lines * 2);
            for (int i = 0; i < lines; ++i)
                sb.append("M\n");
            display.setTextContent(sb.toString(), HState.NORMAL_STATE);
            Dimension preferredSize = display.getPreferredSize();
            display.setTextContent("", HState.NORMAL_STATE);
            preferredSize.width = display.getParent().getSize().width;
            display.setSize(preferredSize);
            display.setLocation(0, display.getParent().getSize().height - preferredSize.height);
        }
        if (background != null)
        {
            display.setBackground(getColor(background));
        }
        if (foreground != null)
        {
            display.setForeground(getColor(foreground));
        }
        if (font != null)
        {
            int size = display.getFont().getSize();
            display.setFont(new Font(font, 0, size));
        }

        if (fontSize != 0)
        {
            display.setFont(new Font(display.getFont().getName(), 0, fontSize));
        }

        if (compositionRule != null)
        {
            display.setComposite(compositionRule);
        }
        if (verticalAlignment != null)
        {
            display.setVerticalAlignment(verticalAlignment);
        }
        if (horizontalAlignment != null)
        {
            display.setHorizontalAlignment(horizontalAlignment);
        }
    }

    /**
     * Parses the given color value.
     * 
     * @param value
     *            the color string
     * @throws IllegalArgumentException
     * 
     * @note somewhat stolen from HAVi MPE Toolkit
     */
    private Color getColor(String value)
    {
        try
        {
            // static color name
            Class c = Color.class;
            java.lang.reflect.Field f = c.getField(value);
            return (Color) f.get(null);
        }
        catch (Exception e)
        {
            try
            {
                // decode color numbers
                return decodeColor(value);
            }
            catch (NumberFormatException e2)
            { /* IGNORED */
            }
        }
        throw new IllegalArgumentException("Cannot intepret '" + value + "' as a Color");
    }

    /**
     * Initializes the on-screen graphics display.
     */
    private void iniz()
    {
        GraphicsManager gfx = (GraphicsManager) ManagerManager.getInstance(GraphicsManager.class);
        Container plane = gfx.getEmergencyAlertPlane(); // should get
                                                        // alternative plane!!!!

        // Create on-screen display component
        display = new Display();

        // Add component
        plane.setVisible(false);
        plane.add(display);

        Dimension parentSize = plane.getSize();

        // Set size
        display.setTextContent("OsdDisplay...", HState.NORMAL_STATE);
        Dimension size = display.getPreferredSize();
        size.width = plane.getSize().width;
        display.setSize(size);

        // Set location
        display.setLocation(0, parentSize.height - size.height);

        plane.setVisible(true);
    }

    public String getBackground()
    {
        return background;
    }

    public void setBackground(String background)
    {
        this.background = background;
    }

    public String getForeground()
    {
        return foreground;
    }

    public void setForeground(String foreground)
    {
        this.foreground = foreground;
    }

    public String getFont()
    {
        return font;
    }

    public void setFont(String font)
    {
        this.font = font;
    }

    public int getFontSize()
    {
        return fontSize;
    }

    public void setFontSize(int fontSize)
    {
        this.fontSize = fontSize;
    }

    public String getCompositionRule()
    {
        return compositionRule;
    }

    public void setCompositionRule(String compositionRule)
    {
        this.compositionRule = compositionRule;
    }

    public String getVerticalAlignment()
    {
        return verticalAlignment;
    }

    public void setVerticalAlignment(String verticalAlignment)
    {
        this.verticalAlignment = verticalAlignment;
    }

    public String getHorizontalAlignment()
    {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(String horizontalAlignment)
    {
        this.horizontalAlignment = horizontalAlignment;
    }

    public int getLines()
    {
        return lines;
    }

    public void setLines(int lines)
    {
        this.lines = lines;
    }

    public int getBufferLength()
    {
        return bufferLength;
    }

    public void setBufferLength(int bufferLength)
    {
        this.bufferLength = bufferLength;
    }

    /**
     * Sets the current displayed message. Replaces the current message with the
     * new message.
     * 
     * @param string
     *            the message to display
     */
    private void display(String string)
    {
        msgSb = new StringBuffer(string);
    }

    /**
     * Appends the given message to the display. Adds the given string to the
     * current messages as set by {@link #display}.
     * 
     * @param string
     *            the string to add to the current message
     */
    private void appendDisplay(String string)
    {
        msgSb.append(string);
    }

    /**
     * Flushes the current display to the on-screen graphics.
     */
    private void show()
    {
        if (buffer.size() >= bufferLength) buffer.removeElementAt(0);
        buffer.addElement(msgSb);

        StringBuffer sb = new StringBuffer();
        final int size = buffer.size();
        for (int i = 0; i < size; ++i)
            sb.append(buffer.elementAt(i));

        display.setTextContent(sb.toString(), HState.NORMAL_STATE);
        if (!display.isVisible()) display.setVisible(true);
    }

    /**
     * Decodes a color in a manner similar to <code>Color.decode()</code>,
     * except it maintains the alpha value.
     * 
     * @param number
     *            the color value encoded in a String
     * @return <code>Color</code> representation of the given string value
     * 
     * @note stolen from HAVi MPE Toolkit
     */
    private Color decodeColor(String number) throws NumberFormatException
    {
        // Use long to avoid number format exception for "negative" values.
        // Use parseLong() instead of decode() for PJava compatibility
        int rgb;
        if (number.startsWith("0x"))
            rgb = (int) Long.parseLong(number.substring(2), 16);
        else if (number.startsWith("#"))
            rgb = (int) Long.parseLong(number.substring(1), 16);
        else if (number.startsWith("0") && number.length() > 1)
            rgb = (int) Long.parseLong(number.substring(1), 8);
        else
            rgb = (int) Long.parseLong(number);
        return new org.dvb.ui.DVBColor(rgb, true);
    }

    /**
     * Parses the given <code>String</code> as an integer.
     * 
     * @return the integer
     */
    private int getInt(String value)
    {
        if (value.startsWith("0x"))
            return Integer.parseInt(value.substring(2), 16);
        else
            return Integer.parseInt(value);
    }

    /**
     * The component used to display on-screen messages.
     * 
     * @author Aaron Kamienski
     */
    private class Display extends org.havi.ui.HStaticText
    {
        public Display()
        {
            super();

            // Default colors and font
            setBackground(Color.black);
            setForeground(Color.white.darker());
            setFont(new Font("Tiresias", 0, 20));

            // background fill
            setBackgroundMode(BACKGROUND_FILL);

            // top/left aligned
            setHorizontalAlignment(HALIGN_LEFT);
            setVerticalAlignment(VALIGN_TOP);

            try
            {
                setLook(new Look());
            }
            catch (HInvalidLookException e)
            { /* EMPTY */
            }
        }

        /**
         * Extension of <code>HTextLook</code>. Overrides {@link #getInsets} to
         * account for overscan area.
         * 
         * @author Aaron Kamienski
         */
        private class Look extends HTextLook
        {
            private Insets insets = new Insets(0, 35, 35, 35);

            public Insets getInsets(HVisible v)
            {
                return insets;
            }
        }

        /**
         * Overrides {@link java.awt.Component#paint} to modify the alpha
         * composite rule used.
         */
        public void paint(Graphics g)
        {
            if (g instanceof DVBGraphics)
            {
                try
                {
                    ((DVBGraphics) g).setDVBComposite(composite);
                }
                catch (UnsupportedDrawingOperationException e)
                { /* IGNORED */
                }
            }
            super.paint(g);

            // restore????
        }

        /**
         * Set the composite rule used in {@link #paint} to the named rule.
         * 
         * @param rule
         *            string representation of composite rule (one of
         *            <code>SRC</code>, <code>SRC_OVER</code>, or percentage
         *            expressed as an integer)
         */
        public void setComposite(String rule)
        {
            rule = rule.toLowerCase();

            try
            {
                composite = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER, getInt(rule) / 100F);
                return;
            }
            catch (Exception e)
            { /* IGNORED */
            }

            if ("SRCOVER".equalsIgnoreCase(rule))
                composite = DVBAlphaComposite.SrcOver;
            else
                // ("SRC".equalsIgnoreCase(rule))
                composite = DVBAlphaComposite.Src;
        }

        /**
         * Lookup up a static int field in this class.
         * 
         * @param field
         *            field name
         * @return field value or 0 if not found
         */
        private int lookup(String field)
        {
            try
            {
                Class c = getClass();
                java.lang.reflect.Field f = c.getField(field);
                Integer i = (Integer) f.get(null);
                return i.intValue();
            }
            catch (Exception e)
            { /* IGNORED */
            }

            return 0;
        }

        /**
         * Set vertical alignment to that named by given parameter.
         * 
         * @param align
         *            string representation of vertical alignment
         */
        public void setVerticalAlignment(String align)
        {
            setVerticalAlignment(lookup("VALIGN_" + align.toUpperCase()));
        }

        /**
         * Set horizontal alignment to that named by given parameter.
         * 
         * @param align
         *            string representation of horizontal alignment
         */
        public void setHorizontalAlignment(String align)
        {
            setHorizontalAlignment(lookup("HALIGN_" + align.toUpperCase()));
        }

        private DVBAlphaComposite composite = DVBAlphaComposite.Src;
    }

    private Display display;

    private StringBuffer msgSb;

    private int lines;

    private int bufferLength;

    private String background;

    private String foreground;

    private String font;

    private int fontSize;

    private String compositionRule;

    private String verticalAlignment;

    private String horizontalAlignment;

    private Vector buffer = new Vector();
}
