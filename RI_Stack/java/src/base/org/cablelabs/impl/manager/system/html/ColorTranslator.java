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

import java.util.HashMap;
import java.awt.Color;

/**
 * Translates strings containing RGB color values or color names into
 * Java.awt.color equivalents.
 * 
 * @author Spencer Schumann
 * 
 */
public class ColorTranslator
{
    // Map of named colors
    private static final HashMap colorNames = new HashMap();

    /**
     * Translates color string formatted as "#RGB" or "#RRGGBB", where R, G, and
     * B are 1 or 2 digit hexadecimal digits representing red, green, and blue,
     * respectively; or, a color name as defined by the HTML5 CSS color table.
     * 
     * @param colorValue
     *            Color to translate
     * @return java.awt.Color value
     */
    public static Color translate(String colorValue)
    {
        Color color = null;

        if (null == colorValue)
        {
            return null;
        }

        colorValue = colorValue.toLowerCase();
        color = (Color) colorNames.get(colorValue);
        if (null != color)
        {
            return color;
        }

        color = translateRGB(colorValue);
        if (null != color)
        {
            return color;
        }

        return null;
    }

    /**
     * Private constructor: prevent instantiation and subclassing.
     * 
     */
    private ColorTranslator()
    {
    }

    /**
     * Translates color string containing RGB color values.
     * 
     * @param colorValue
     *            Color to translate
     * @return java.awt.Color value
     */
    private static Color translateRGB(String colorValue)
    {
        // Color must start with '#'
        if (colorValue.charAt(0) != '#')
        {
            return null;
        }

        try
        {
            int r = 0;
            int g = 0;
            int b = 0;
            if (colorValue.length() == 7)
            {
                r = Integer.parseInt(colorValue.substring(1, 3), 16);
                g = Integer.parseInt(colorValue.substring(3, 5), 16);
                b = Integer.parseInt(colorValue.substring(5, 7), 16);
            }
            else if (colorValue.length() == 4)
            {
                r = Integer.parseInt(colorValue.substring(1, 2), 16);
                g = Integer.parseInt(colorValue.substring(2, 3), 16);
                b = Integer.parseInt(colorValue.substring(3, 4), 16);
            }
            else
            {
                return null;
            }

            return new Color(r, g, b);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * Convenience method for initializing the color name map
     * 
     * @param name
     *            color name to add
     * @param color
     *            color value to add
     */
    private static void addColor(String name, int color)
    {
        colorNames.put(name.toLowerCase(), new Color(color));
    }

    static
    {
        // Set up color name mappings (from HTML 5 draft spec)
        addColor("aliceblue", 0xf0f8ff);
        addColor("antiquewhite", 0xfaebd7);
        addColor("aqua", 0x00ffff);
        addColor("aquamarine", 0x7fffd4);
        addColor("azure", 0xf0ffff);
        addColor("beige", 0xf5f5dc);
        addColor("bisque", 0xffe4c4);
        addColor("black", 0x000000);
        addColor("blanchedalmond", 0xffebcd);
        addColor("blue", 0x0000ff);
        addColor("blueviolet", 0x8a2be2);
        addColor("brown", 0xa52a2a);
        addColor("burlywood", 0xdeb887);
        addColor("cadetblue", 0x5f9ea0);
        addColor("chartreuse", 0x7fff00);
        addColor("chocolate", 0xd2691e);
        addColor("coral", 0xff7f50);
        addColor("cornflowerblue", 0x6495ed);
        addColor("cornsilk", 0xfff8dc);
        addColor("crimson", 0xdc143c);
        addColor("cyan", 0x00ffff);
        addColor("darkblue", 0x00008b);
        addColor("darkcyan", 0x008b8b);
        addColor("darkgoldenrod", 0xb8860b);
        addColor("darkgray", 0xa9a9a9);
        addColor("darkgreen", 0x006400);
        addColor("darkgrey", 0xa9a9a9);
        addColor("darkkhaki", 0xbdb76b);
        addColor("darkmagenta", 0x8b008b);
        addColor("darkolivegreen", 0x556b2f);
        addColor("darkorange", 0xff8c00);
        addColor("darkorchid", 0x9932cc);
        addColor("darkred", 0x8b0000);
        addColor("darksalmon", 0xe9967a);
        addColor("darkseagreen", 0x8fbc8f);
        addColor("darkslateblue", 0x483d8b);
        addColor("darkslategray", 0x2f4f4f);
        addColor("darkslategrey", 0x2f4f4f);
        addColor("darkturquoise", 0x00ced1);
        addColor("darkviolet", 0x9400d3);
        addColor("deeppink", 0xff1493);
        addColor("deepskyblue", 0x00bfff);
        addColor("dimgray", 0x696969);
        addColor("dimgrey", 0x696969);
        addColor("dodgerblue", 0x1e90ff);
        addColor("firebrick", 0xb22222);
        addColor("floralwhite", 0xfffaf0);
        addColor("forestgreen", 0x228b22);
        addColor("fuchsia", 0xff00ff);
        addColor("gainsboro", 0xdcdcdc);
        addColor("ghostwhite", 0xf8f8ff);
        addColor("gold", 0xffd700);
        addColor("goldenrod", 0xdaa520);
        addColor("gray", 0x808080);
        addColor("green", 0x008000);
        addColor("greenyellow", 0xadff2f);
        addColor("grey", 0x808080);
        addColor("honeydew", 0xf0fff0);
        addColor("hotpink", 0xff69b4);
        addColor("indianred", 0xcd5c5c);
        addColor("indigo", 0x4b0082);
        addColor("ivory", 0xfffff0);
        addColor("khaki", 0xf0e68c);
        addColor("lavender", 0xe6e6fa);
        addColor("lavenderblush", 0xfff0f5);
        addColor("lawngreen", 0x7cfc00);
        addColor("lemonchiffon", 0xfffacd);
        addColor("lightblue", 0xadd8e6);
        addColor("lightcoral", 0xf08080);
        addColor("lightcyan", 0xe0ffff);
        addColor("lightgoldenrodyellow", 0xfafad2);
        addColor("lightgray", 0xd3d3d3);
        addColor("lightgreen", 0x90ee90);
        addColor("lightgrey", 0xd3d3d3);
        addColor("lightpink", 0xffb6c1);
        addColor("lightsalmon", 0xffa07a);
        addColor("lightseagreen", 0x20b2aa);
        addColor("lightskyblue", 0x87cefa);
        addColor("lightslategray", 0x778899);
        addColor("lightslategrey", 0x778899);
        addColor("lightsteelblue", 0xb0c4de);
        addColor("lightyellow", 0xffffe0);
        addColor("lime", 0x00ff00);
        addColor("limegreen", 0x32cd32);
        addColor("linen", 0xfaf0e6);
        addColor("magenta", 0xff00ff);
        addColor("maroon", 0x800000);
        addColor("mediumaquamarine", 0x66cdaa);
        addColor("mediumblue", 0x0000cd);
        addColor("mediumorchid", 0xba55d3);
        addColor("mediumpurple", 0x9370db);
        addColor("mediumseagreen", 0x3cb371);
        addColor("mediumslateblue", 0x7b68ee);
        addColor("mediumspringgreen", 0x00fa9a);
        addColor("mediumturquoise", 0x48d1cc);
        addColor("mediumvioletred", 0xc71585);
        addColor("midnightblue", 0x191970);
        addColor("mintcream", 0xf5fffa);
        addColor("mistyrose", 0xffe4e1);
        addColor("moccasin", 0xffe4b5);
        addColor("navajowhite", 0xffdead);
        addColor("navy", 0x000080);
        addColor("oldlace", 0xfdf5e6);
        addColor("olive", 0x808000);
        addColor("olivedrab", 0x6b8e23);
        addColor("orange", 0xffa500);
        addColor("orangered", 0xff4500);
        addColor("orchid", 0xda70d6);
        addColor("palegoldenrod", 0xeee8aa);
        addColor("palegreen", 0x98fb98);
        addColor("paleturquoise", 0xafeeee);
        addColor("palevioletred", 0xdb7093);
        addColor("papayawhip", 0xffefd5);
        addColor("peachpuff", 0xffdab9);
        addColor("peru", 0xcd853f);
        addColor("pink", 0xffc0cb);
        addColor("plum", 0xdda0dd);
        addColor("powderblue", 0xb0e0e6);
        addColor("purple", 0x800080);
        addColor("red", 0xff0000);
        addColor("rosybrown", 0xbc8f8f);
        addColor("royalblue", 0x4169e1);
        addColor("saddlebrown", 0x8b4513);
        addColor("salmon", 0xfa8072);
        addColor("sandybrown", 0xf4a460);
        addColor("seagreen", 0x2e8b57);
        addColor("seashell", 0xfff5ee);
        addColor("sienna", 0xa0522d);
        addColor("silver", 0xc0c0c0);
        addColor("skyblue", 0x87ceeb);
        addColor("slateblue", 0x6a5acd);
        addColor("slategray", 0x708090);
        addColor("slategrey", 0x708090);
        addColor("snow", 0xfffafa);
        addColor("springgreen", 0x00ff7f);
        addColor("steelblue", 0x4682b4);
        addColor("tan", 0xd2b48c);
        addColor("teal", 0x008080);
        addColor("thistle", 0xd8bfd8);
        addColor("tomato", 0xff6347);
        addColor("turquoise", 0x40e0d0);
        addColor("violet", 0xee82ee);
        addColor("wheat", 0xf5deb3);
        addColor("white", 0xffffff);
        addColor("whitesmoke", 0xf5f5f5);
        addColor("yellow", 0xffff00);
        addColor("yellowgreen", 0x9acd32);
    }
}
