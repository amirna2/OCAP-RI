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

package org.cablelabs.impl.manager.system.html;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Text styles to be used with Paragraph.
 * 
 * @author Spencer Schumann
 * 
 */
public final class TextStyle
{
    /**
     * Attribute key for hyperlinks. The associated value is a string containing
     * the link.
     */
    public static final String HYPERLINK = "hyperlink";

    /**
     * Attribute key for font size. The associated value is a Float representing
     * a fraction of the default font size.
     */
    public static final String FONTSIZE = "fontsize";

    /**
     * Attribute key for bold text. The associated value is a Boolean.
     */
    public static final String BOLD = "bold";

    /**
     * Attribute key for italic text. The associated value is a Boolean.
     */
    public static final String ITALIC = "italic";

    /**
     * Attribute key for underlined text. The associated value is a Boolean.
     */
    public static final String UNDERLINED = "underlined";

    /**
     * Attribute key for text color. The associated value is a java.awt.Color
     * value.
     */
    public static final String COLOR = "color";

    // Font size defaults to 3, as per the HTML 3.2 specification. This is used
    // with the FONT tag, which is marked obsolete in the HTML5 specification
    // but is still used for MMI HTML pages since they do not support CSS.
    private static int DEFAULT_FONT_SIZE = 3;

    private int boldCount = 0;
    private int italicCount = 0;
    private int underlinedCount = 0;
    private Stack colorStack = new Stack();
    private Stack sizeStack = new Stack();

    // Font sizes associated with the integral size values 1 through 7.
    private static float[] scaleFactors = { 0.0f, 0.7f, 0.8f, 1.0f, 1.3f, 1.6f,
            2.0f, 3.0f };

    /**
     * Constructs a text style object with default attributes.
     * 
     */
    public TextStyle()
    {
        setAbsoluteSize(DEFAULT_FONT_SIZE);
    }

    /**
     * Sets the bold style. If set multiple times, an equal number of clearBold
     * calls are needed to clear the style.
     * 
     */
    public void setBold()
    {
        boldCount++;
    }

    /**
     * Clears the bold style.
     * 
     */
    public void clearBold()
    {
        if (boldCount > 0)
        {
            boldCount--;
        }
    }

    /**
     * Sets the italic style. If set multiple times, an equal number of
     * clearItalic calls are needed to clear the style.
     * 
     */
    public void setItalic()
    {
        italicCount++;
    }

    /**
     * Clear italic style.
     * 
     */
    public void clearItalic()
    {
        if (italicCount > 0)
        {
            italicCount--;
        }
    }

    /**
     * Sets the underlined style. If set multiple times, an equal number of
     * clearUnderlined calls are needed to clear the style.
     * 
     */
    public void setUnderlined()
    {
        underlinedCount++;
    }

    /**
     * Clear underlined style.
     * 
     */
    public void clearUnderlined()
    {
        if (underlinedCount > 0)
        {
            underlinedCount--;
        }
    }

    /**
     * Sets the text color.
     * 
     */
    public void setColor(Color color)
    {
        colorStack.push(color);
    }

    /**
     * Clears the text color, restoring the color that was in use before the
     * last call to setColor.
     * 
     */
    public void clearColor()
    {
        if (!colorStack.isEmpty())
        {
            colorStack.pop();
        }
    }

    /**
     * Get the current text color.
     * 
     * @return text color
     */
    public Color getColor()
    {
        if (!colorStack.isEmpty())
        {
            return (Color) colorStack.peek();
        }
        else
        {
            return null;
        }
    }

    /**
     * Set the current font size based on the SIZE attribute value of a FONT
     * tag.
     * 
     * @param size
     *            size attribute value
     */
    public void setSize(String size)
    {
        // Size is (+|-)?[digit]+
        if (null == size || size.length() == 0)
        {
            sizeStack.push(sizeStack.peek());
            return;
        }

        boolean relative = false;
        if (size.charAt(0) == '-')
        {
            relative = true;
        }
        else if (size.charAt(0) == '+')
        {
            relative = true;
            // Remove the '+', since Integer.parseInt can't handle it
            size = size.substring(1);
        }

        try
        {
            int s = Integer.parseInt(size);
            if (relative)
            {
                setRelativeSize(s);
            }
            else
            {
                setAbsoluteSize(s);
            }
        }
        catch (NumberFormatException e)
        {
            sizeStack.push(sizeStack.peek());
            return;
        }
    }

    /**
     * Set font size. Size is an integral value between 1 and 7.
     * 
     * @param size absolute font size
     */
    public void setAbsoluteSize(int size)
    {
        // This is what Firefox does; Chrome treats an absolute size of 0 as
        // invalid, and uses the default size instead. Both treat a large
        // negative relative value (like -10) the same.
        size = Math.max(size, 1);
        size = Math.min(size, 7);

        sizeStack.push(new Float(scaleFactors[size]));
    }

    /**
     * Set relative font size. Size is an integral offset from the default font
     * size.
     * 
     * @param size relative font size
     */
    public void setRelativeSize(int size)
    {
        setAbsoluteSize(size + DEFAULT_FONT_SIZE);
    }

    /**
     * Clears the font size, restoring the size that was in use before the
     * last call to setSize, setAbsoluteSize, or setRelativeSize.
     * 
     */
    public void clearSize()
    {
        // Don't allow the default size to be removed.
        if (sizeStack.size() > 1)
        {
            sizeStack.pop();
        }
    }

    /**
     * Get the current font size
     * 
     * @return font size as fraction of the default font size.
     */
    public float getFontSize()
    {
        return ((Float) sizeStack.peek()).floatValue();
    }

    /**
     * Create a map of the current set of attributes.
     * 
     * @return attribute map
     */
    public Map build()
    {
        // There will usually only be a few style entries, so create a HashMap
        // with a starting size smaller than the default of 16.
        HashMap attributes = new HashMap(4);

        if (boldCount > 0)
        {
            attributes.put(BOLD, Boolean.TRUE);
        }

        if (italicCount > 0)
        {
            attributes.put(ITALIC, Boolean.TRUE);
        }

        if (underlinedCount > 0)
        {
            attributes.put(UNDERLINED, Boolean.TRUE);
        }

        Color color = getColor();
        if (null != color)
        {
            attributes.put(COLOR, color);
        }

        attributes.put(FONTSIZE, sizeStack.peek());

        return attributes;
    }
}
