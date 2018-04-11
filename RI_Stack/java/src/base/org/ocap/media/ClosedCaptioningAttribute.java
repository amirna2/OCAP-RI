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

package org.ocap.media;

import org.apache.log4j.Logger;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

import java.awt.Color;
import java.util.*;

/**
 * <p>
 * This class represents a system wide preference of closed-captioning
 * representation. The OCAP implementation shall display closed-captioning
 * according to preference values that is specified by this class. Application
 * developers should be aware that the FCC has defined strict rules regarding
 * display of CC and EAS (see http://ftp.fcc.gov/cgb/dro/caption.html for FCC
 * closed captioning rules).
 *
 * @author Shigeaki Watanabe (Panasonic)
 * @version 1.0
 */
public class ClosedCaptioningAttribute
{
    /**
     * Indicates a pen color attribute to draw closed-captioning text. Identical
     * to the "fg color" parameter of SetPenColor command of EIA-708-B. For an
     * analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_PEN_FG_COLOR = 0;

    /**
     * Indicates a pen back ground color attribute to draw closed-captioning
     * text. Identical to the "bg color" parameter of SetPenColor command of
     * EIA-708-B. For an analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_PEN_BG_COLOR = 1;

    /**
     * Indicates a pen opacity attribute of a closed-captioning text. Identical
     * to the "fg opacity" parameter of SetPenColor command of EIA-708-B. For an
     * analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_PEN_FG_OPACITY = 2;

    /**
     * Indicates a pen back ground opacity attribute of a closed-captioning
     * text. Identical to the "bg opacity" parameter of SetPenColor command of
     * EIA-708-B. For an analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_PEN_BG_OPACITY = 3;

    /**
     * Indicates a font style attribute of a closed-captioning text. Identical
     * to the "font style" parameter of SetPenAttributes command of EIA-708-B.
     * For an analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_FONT_STYLE = 4;

    /**
     * Indicates a font size attribute of a closed-captioning text. Identical to
     * the "pen size" parameter of SetPenAttributes command of EIA-708-B. For an
     * analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_PEN_SIZE = 5;

    /**
     * Indicates a font face attribute of a closed-captioning text. Identical to
     * the "italics" parameter of SetPenAttributes command of EIA-708-B. For an
     * analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_FONT_ITALICIZED = 6;

    /**
     * Indicates a font face attribute of a closed-captioning text. Identical to
     * the "underline" parameter of SetPenAttributes command of EIA-708-B. For
     * an analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_FONT_UNDERLINE = 7;

    /**
     * Indicates a window fill color attribute of a closed-captioning window.
     * Identical to the "fill color" parameter of SetWindowAttributes command of
     * EIA-708-B. For an analog captioning, an equivalent attribute is assigned.
     */
    public final static int CC_ATTRIBUTE_WINDOW_FILL_COLOR = 8;

    /**
     * Indicates a border type attribute of a closed-captioning window.
     * Identical to the "fill opacity" parameter of SetWindowAttributes command
     * of EIA-708-B. For an analog captioning, an equivalent attribute is
     * assigned.
     */
    public final static int CC_ATTRIBUTE_WINDOW_FILL_OPACITY = 9;

    /**
     * Indicates a border type attribute of a closed-captioning window.
     * Identical to the "border color" parameter of SetWindowAttributes command
     * of EIA-708-B. For an analog captioning, an equivalent attribute is
     * assigned.
     */
    public final static int CC_ATTRIBUTE_WINDOW_BORDER_TYPE = 10;

    /**
     * Indicates a border color attribute of a closed-captioning window.
     * Identical to the "border color" parameter of SetWindowAttributes command
     * of EIA-708-B. For an analog captioning, an equivalent attribute is
     * assigned.
     */
    public final static int CC_ATTRIBUTE_WINDOW_BORDER_COLOR = 11;

    /**
     * Indicates a small pen size.
     */
    public final static int CC_PEN_SIZE_SMALL = 0;

    /**
     * Indicates a standard pen size.
     */
    public final static int CC_PEN_SIZE_STANDARD = 1;

    /**
     * Indicates a large pen size.
     */
    public final static int CC_PEN_SIZE_LARGE = 2;

    /**
     * Indicates a opacity value for a solid.
     */
    public final static int CC_OPACITY_SOLID = 0;

    /**
     * Indicates a opacity value for a flash.
     */
    public final static int CC_OPACITY_FLASH = 1;

    /**
     * Indicates a opacity value for a translucent.
     */
    public final static int CC_OPACITY_TRANSLUCENT = 2;

    /**
     * Indicates a opacity value for a transparent.
     */
    public final static int CC_OPACITY_TRANSPARENT = 3;

    /**
     * Indicates a border type of NONE.
     */
    public final static int CC_BORDER_NONE = 0;

    /**
     * Indicates a border type of RAISED.
     */
    public final static int CC_BORDER_RAISED = 1;

    /**
     * Indicates a border type of DEPRESSED.
     */
    public final static int CC_BORDER_DEPRESSED = 2;

    /**
     * Indicates a border type of UNIFORM.
     */
    public final static int CC_BORDER_UNIFORM = 3;

    /**
     * Indicates a border type of SHADOW_LEFT.
     */
    public final static int CC_BORDER_SHADOW_LEFT = 4;

    /**
     * Indicates a border type of SHADOW_RIGHT.
     */
    public final static int CC_BORDER_SHADOW_RIGHT = 5;

    /**
     * Indicates an analog type closed-captioning.
     */
    public final static int CC_TYPE_ANALOG = 0;

    /**
     * Indicates an digital type closed-captioning.
     */
    public final static int CC_TYPE_DIGITAL = 1;

    /**
     * A constructor of this class. An application shall not call this
     * constructor directly.
     */
    protected ClosedCaptioningAttribute()
    {
    }

    /**
     * This method returns an instance of this class. It is not required to be a
     * singleton manner.
     *
     * @return A ClosedCaptioningAttribute instance.
     *
     * @throws SecurityException
     *             if the caller doesn't have
     *             MotinorAppPermission("handler.closedCaptioning").
     */
    public static ClosedCaptioningAttribute getInstance()
    {

        ClosedCaptioningAttribute aCCAttribute = null;

        checkPermission();

        try
        {
            aCCAttribute = new ClosedCaptioningAttribute();
            if (log.isDebugEnabled())
            {
                log.debug("::Created ClosedCaptioningAttribute");
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(new Exception("::Failed to Create ClosedCaptioningAttribute"));
        }

        return aCCAttribute;
    }

    /**
     * <p>
     * This method returns a possible attribute values applied to an
     * closed-captioning text on a screen. Note that the possible font attribute
     * may be different from the possible font for Java application since the
     * closed-captioning module may be implemented by native language.
     * </p>
     *
     * @return an array of possible attribute values of an closed-captioning
     *         text corresponding to the specified attribute parameter.
     *         <ul>
     *         <li>If the attribute parameter is CC_ATTRIBUTE_PEN_FG_COLOR or
     *         CC_ATTRIBUTE_PEN_BG_COLOR, an array of java.awt.Color that
     *         represents possible font color returns. The Color.getString()
     *         shall return a text expression of its color to show a user.
     *         <li>If the attribute parameter is CC_ATTRIBUTE_PEN_FG_OPACITY or
     *         CC_ATTRIBUTE_PEN_BG_OPACITY, an array of constants that
     *         represents possible opacity returns. The opacity constants has a
     *         prefix of CC_OPACITY_.
     *         <li>If the attribute parameter is CC_ATTRIBUTE_FONT_STYLE, an
     *         array of String that represents possible font style returns. It
     *         is recommended that the String is one of font style defined in
     *         EIA-708-B but not restricted to it. The host device can provide a
     *         new style.
     *         <li>If the attribute parameter is CC_ATTRIBUTE_PEN_SIZE, an array
     *         of constants that represents possible pen size returns. The pen
     *         size constants has a prefix of CC_PEN_SIZE_.
     *         <li>If the attribute parameter is CC_ATTRIBUTE_FONT_ITALICIZED,
     *         an array of possible Integer value (YES=1, NO=0) returns. I.e.,
     *         if the host can select a plane font or an italicized font, an
     *         array of [0, 1] (or [1, 0]) returns. If the host only supports a
     *         plane font, [0] returns.
     *         <li>If the attribute parameter is CC_ATTRIBUTE_FONT_UNDERLINE, an
     *         array of possible Integer value (YES=1, NO=0) returns. See also
     *         the CC_ATTRIBUTE_FONT_ITALICIZED description.
     *         <li>If the attribute parameter is CC_ATTRIBUTE_WINDOW_FILL_COLOR,
     *         an array of java.awt.Color that represents possible window fill
     *         color returns. The Color.getString() shall return a text
     *         expression of its color to show a user.
     *         <li>If the attribute parameter is
     *         CC_ATTRIBUTE_WINDOW_FILL_OPACITY an array of constants that
     *         represents possible opacity returns. The opacity constants has a
     *         prefix of CC_OPACITY_.
     *         <li>If the attribute parameter is CC_ATTRIBUTE_WINDOW_BORDER_TYPE
     *         an array of constants that represents possible border type
     *         returns. The border type constants has a prefix of CC_BORDER_.
     *         <li>If the attribute parameter is
     *         CC_ATTRIBUTE_WINDOW_BORDER_COLOR, an array of java.awt.Color that
     *         represents possible window border color returns. The
     *         Color.getString() shall return a text expression of its color to
     *         show a user.
     *         </ul>
     *
     * @param attribute
     *            specify an attribute to get possible values. One of constants
     *            that has a CC_ATTRIBUTE_ prefix shall be specified.
     *
     * @param ccType
     *            either CC_ANALOG or CC_DIGITAL to specify a type of
     *            closed-captioning.
     *
     * @throws IllegalArgumentException
     *             if a specified attribute or ccType parameter is out of range.
     *
     */
    public Object[] getCCCapability(int attribute, int ccType)
    {

        if (attribute < CC_ATTRIBUTE_PEN_FG_COLOR || attribute > CC_ATTRIBUTE_WINDOW_BORDER_COLOR)
        {
            throw new IllegalArgumentException("Invalid Attribute");
        }
        if (ccType != CC_TYPE_ANALOG && ccType != CC_TYPE_DIGITAL)
        {
            throw new IllegalArgumentException("Invalid CC Type");
        }

        if (allAttributes[ccType][attribute] == null)
        {
            allAttributes[ccType][attribute] = getCapability(attribute, ccType);
        }

        if (allAttributes[ccType][attribute].isEmpty() == false)
        {
            return (allAttributes[ccType][attribute].toArray());
        }

        if (log.isDebugEnabled())
        {
            log.debug("getCCCapability returned null");
        }

        return null;
    }

    /**
     * <p>
     * This method sets a preferred attribute values applied to a
     * closed-captioning text on a screen. Some attribute values can be
     * specified by one call of this method. If one of the specified attribute
     * value is invalid, i.e., the value is not included in the return value of
     * the {@link #getCCCapability} method, this method changes none of current
     * attribute values and throw an exception.
     * </p>
     *
     * @param attribute
     *            an array of attributes to be set a prefered value. One of of
     *            constants that has a CC_ATTRIBUTE_ prefix shall be specified.
     *
     * @param value
     *            an array of prefered values to be used to draw a
     *            closed-captioning text. The value shall be one of the return
     *            value from the
     *            {@link ClosedCaptioningAttribute#getCCCapability} method for
     *            the specified attribute, or null to set a host�s default
     *            value. The i-th item of the value array corresponds to the
     *            i-th item of the attribute array.
     *
     * @param ccType
     *            either CC_ANALOG or CC_DIGITAL to specify a type of
     *            closed-captioning.
     *
     * @throws IllegalArgumentException
     *             if a specified attribute, value, or ccType parameter is out
     *             of range or not a capable value, or if a length of a
     *             specified attribute array doesn�t matches with a length of a
     *             specified value array.
     */
    public void setCCAttribute(int attribute[], Object value[], int ccType)
    {

        // If attribute[i] contains one of the CC_ATTRIBUTE_*,
        // value[i] should contain one of its possible values.
        // Example:
        // attribute[0] = CC_ATTRIBUTE_PEN_SIZE
        // value[0] = CC_PEN_SIZE_SMALL

        // check for invalid attribute type and value

        if ((attribute == null) || (attribute.length == 0))
        {
            throw new IllegalArgumentException("null input param 'attribute[]'");
        }

        if ((value == null) || (value.length == 0))
        {
            throw new IllegalArgumentException("null input param 'value[]'");
        }

        if (value.length < attribute.length)
        {
            throw new IllegalArgumentException("'value[]' parameter doesn't have enough fields to match 'attribute[]'");
        }

        if (ccType != CC_TYPE_ANALOG && ccType != CC_TYPE_DIGITAL)
        {
            throw new IllegalArgumentException("Invalid CC Type");
        }

        for (int i = 0; i < attribute.length; i++)
        {
            if (attribute[i] < CC_ATTRIBUTE_PEN_FG_COLOR || attribute[i] > CC_ATTRIBUTE_WINDOW_BORDER_COLOR)
            {
                throw new IllegalArgumentException("Invalid Attribute");
            }

            if (allAttributes[ccType][attribute[i]] == null)
            {
                allAttributes[ccType][attribute[i]] = getCapability(attribute[i], ccType);
                if (log.isDebugEnabled())
                {
                    log.debug("parseCapability returned " + allAttributes[ccType][attribute[i]]);
                }
            }

            if (value[i] != null && allAttributes[ccType][attribute[i]].contains(value[i]) == false)
            {
                throw new IllegalArgumentException("Invalid Attribute Value");
            }
        }
        for (int i = 0; i < attribute.length; i++)
        {
            if (value[i] == null)
            {
                nativeSetCCEmbeddedValue(attribute[i], ccType);
            }
            else
            {
                switch (attribute[i])
                {
                    case CC_ATTRIBUTE_PEN_FG_COLOR:
                    case CC_ATTRIBUTE_PEN_BG_COLOR:
                    case CC_ATTRIBUTE_WINDOW_FILL_COLOR:
                    case CC_ATTRIBUTE_WINDOW_BORDER_COLOR:
                    {
                        Color c = (Color) value[i];
                        nativeSetCCColorValue(attribute[i], c.getRed(), c.getGreen(), c.getBlue(), ccType);
                        break;
                    }
                    case CC_ATTRIBUTE_PEN_FG_OPACITY:
                    case CC_ATTRIBUTE_PEN_BG_OPACITY:
                    case CC_ATTRIBUTE_WINDOW_FILL_OPACITY:
                    case CC_ATTRIBUTE_WINDOW_BORDER_TYPE:
                    case CC_ATTRIBUTE_PEN_SIZE:
                    case CC_ATTRIBUTE_FONT_ITALICIZED:
                    case CC_ATTRIBUTE_FONT_UNDERLINE:
                    {
                        Integer v = (Integer) value[i];
                        nativeSetCCIntValue(attribute[i], v.intValue(), ccType);
                        break;
                    }
                    case CC_ATTRIBUTE_FONT_STYLE:
                    {
                        String v = (String) value[i];
                        nativeSetCCStringValue(attribute[i], v, ccType);
                        break;
                    }
                }
            }
        }
    }

    /**
     * <p>
     * This method returns a current attribute values applied to a
     * closed-captioning text on a screen.
     * </p>
     *
     * @return a current attribute value corresponding to the specified
     *         closed-captioning attribute parameter. See the
     *         {@link #getCCCapability} method for an applicable value.
     *
     * @param attribute
     *            specify an attribute to get a prefered values. One of of
     *            constants that has a CC_ATTRIBUTE_ prefix shall be specified.
     *            See the {@link #getCCCapability} method also.
     *
     * @param ccType
     *            either CC_ANALOG or CC_DIGITAL to specify a type of
     *            closed-captioning.
     *
     * @throws IllegalArgumentException
     *             if a specified attribute or ccType parameter is out of range.
     */
    public Object getCCAttribute(int attribute, int ccType)
    {

        if (attribute < CC_ATTRIBUTE_PEN_FG_COLOR || attribute > CC_ATTRIBUTE_WINDOW_BORDER_COLOR)
        {
            throw new IllegalArgumentException("Invalid Attribute");
        }

        if (ccType != CC_TYPE_ANALOG && ccType != CC_TYPE_DIGITAL)
        {
            throw new IllegalArgumentException("Invalid CC Type");
        }

        return nativeGetCCAttribute(attribute, ccType);
    }

    /**
     * Indicates number of attributes.
     */
    private final static int CC_ATTRIBUTES = CC_ATTRIBUTE_WINDOW_BORDER_COLOR + 1;

    /**
     * Indicates values for attribute font italicized and font underline.
     */
    private final static int CC_ATTRIBUTE_NO = 0;

    /**
     * Indicates values for attribute font italicized and font underline.
     */
    private final static int CC_ATTRIBUTE_YES = 1;

    /**
     * Font styles supported by EIA-708-B.  Not sure why these constants are not
     * part of the spec.
     */
    private final static int CC_STYLE_DEFAULT = 0;
    private final static int CC_STYLE_MONOSPACED_SERIF = 1;
    private final static int CC_STYLE_PROPORTIONAL_SERIF = 2;
    private final static int CC_STYLE_MONOSPACED_SANSSERIF = 3;
    private final static int CC_STYLE_PROPORTIONAL_SANSSERIF = 4;
    private final static int CC_STYLE_CASUAL = 0;
    private final static int CC_STYLE_CURSIVE = 0;
    private final static int CC_STYLE_SMALL_CAPITALS = 0;

    /**
     * First array index is CC_TYPE_ANALOG or CC_TYPE_DIGITAL.
     * Second array index is CC_ATTRIBUTE_* value
     */
    private Vector[][] allAttributes = new Vector[2][CC_ATTRIBUTES];

    private Vector getCapability(int attribute, int analogDigital)
    {
        Vector vect = new Vector();

        switch (attribute)
        {
            case CC_ATTRIBUTE_PEN_FG_COLOR:
            case CC_ATTRIBUTE_PEN_BG_COLOR:
            case CC_ATTRIBUTE_WINDOW_FILL_COLOR:
            case CC_ATTRIBUTE_WINDOW_BORDER_COLOR:
            {

                MpeColor[] colors = (MpeColor[])nativeGetCCCapability(attribute,analogDigital);
                for (int i = 0; i < colors.length; i++)
                {
                    MpeColor color = colors[i];
                    vect.addElement(color);
                    if (log.isDebugEnabled())
                    {
                        log.debug("CC GetCapability (" + ((analogDigital == CC_TYPE_DIGITAL) ? "digital)" : "analog)") + "attr =  " + attribute + ", value = " + color);
                    }
                }
                break;
            }

            case CC_ATTRIBUTE_PEN_FG_OPACITY:
            case CC_ATTRIBUTE_PEN_BG_OPACITY:
            case CC_ATTRIBUTE_WINDOW_FILL_OPACITY:
            case CC_ATTRIBUTE_PEN_SIZE:
            case CC_ATTRIBUTE_WINDOW_BORDER_TYPE:
            case CC_ATTRIBUTE_FONT_ITALICIZED:
            case CC_ATTRIBUTE_FONT_UNDERLINE:
            {
                Integer[] caps = (Integer[])nativeGetCCCapability(attribute,analogDigital);
                for (int i = 0; i < caps.length; i++)
                {
                    vect.addElement(caps[i]);
                    if (log.isDebugEnabled())
                    {
                        log.debug("CC GetCapability (" + ((analogDigital == CC_TYPE_DIGITAL) ? "digital)" : "analog)") + "attr =  " + attribute + ", value = " + caps[i]);
                    }
                }
                break;
            }

            case CC_ATTRIBUTE_FONT_STYLE:
            {
                String[] fonts = (String[])nativeGetCCCapability(attribute,analogDigital);
                for (int i = 0; i < fonts.length; i++)
                {
                    vect.addElement(fonts[i]);
                    if (log.isDebugEnabled())
                    {
                        log.debug("CC GetCapability (" + ((analogDigital == CC_TYPE_DIGITAL) ? "digital)" : "analog)") + "attr =  " + attribute + ", value = " + fonts[i]);
                    }
                }
                break;
            }

            default:
                return null;
        }

        return vect;
    }

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(ClosedCaptioningAttribute.class.getName());

    private static void checkPermission() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.closedCaptioning"));
    }

    private native void nativeSetCCEmbeddedValue(int attribute, int ccType);
    private native void nativeSetCCIntValue(int attribute, int value, int ccType);
    private native void nativeSetCCColorValue(int attribute, int r, int g, int b, int ccType);
    private native void nativeSetCCStringValue(int attribute, String value, int ccType);

    private native static void init();

    private native Object nativeGetCCAttribute(int attribute, int ccType);
    private native Object[] nativeGetCCCapability(int attribute, int ccType);

    private class MpeColor extends Color
    {
        public MpeColor(int rgb, String name)
        {
            super((rgb & 0xFF0000) >> 16, (rgb & 0xFF00) >> 8, rgb & 0xFF);
            this.name = name;
        }

        public String toString()
        {
            return name;
        }

        private String name;
    }

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        init();
    }
}
