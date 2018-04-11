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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui.event;

/**
 * This class is able to describe the representation of an event generator as a
 * string, color or symbol (such as a triangle, '>', for 'play'). This allows an
 * application to describe a button on an input device correctly for a given
 * platform.
 * 
 * <p>
 * 
 * The particular text, color, or symbol can be determined by calling the
 * {@link org.havi.ui.event.HEventRepresentation#getString getString},
 * {@link org.havi.ui.event.HEventRepresentation#getColor getColor} or
 * {@link org.havi.ui.event.HEventRepresentation#getSymbol getSymbol} methods
 * respectively. All available events should return a valid text representation
 * from the {@link org.havi.ui.event.HEventRepresentation#getString getString}
 * method.
 * 
 * <p>
 * If supported the six colored key events (<code>VK_COLORED_KEY_0</code> thru
 * <code>VK_COLORED_KEY_5</code>) must also be represented by a color, i.e. the
 * {@link org.havi.ui.event.HEventRepresentation#getColor getColor} method must
 * return a valid <code>java.awt.Color</code> object.
 * 
 * <p>
 * Key events may also be represented as a symbol - if the platform does not
 * support a symbolic representation for a given event, then the application is
 * responsible for rendering the symbol itself. The rendering of keys with a
 * commonly known representation should follow the guidelines given here, as
 * defined in the following table.
 * <p>
 * <table border>
 * <tr>
 * <th>Event</th>
 * <th>Implied symbol</th>
 * <th>Sample</th>
 * </tr>
 * <tr>
 * <td>VK_GO_TO_START</td>
 * <td>Two equilateral triangles, pointing at a line to the left</td>
 * <td><img src="{@docRoot}/images/start.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_REWIND</td>
 * <td>Two equilateral triangles, pointing to the left</td>
 * <td><img src="{@docRoot}/images/rewind.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_STOP</td>
 * <td>A square</td>
 * <td><img src="{@docRoot}/images/stop.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_PAUSE</td>
 * <td>Two vertical lines, side by side</td>
 * <td><img src="{@docRoot}/images/pause.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_PLAY</td>
 * <td>One equilateral triangle, pointing to the right</td>
 * <td><img src="{@docRoot}/images/play.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_FAST_FWD</td>
 * <td>Two equilateral triangles, pointing to the right</td>
 * <td><img src="{@docRoot}/images/fastfwd.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_GO_TO_END</td>
 * <td>Two equilateral triangles, pointing to a line at the right</td>
 * <td><img src="{@docRoot}/images/end.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_TRACK_PREV</td>
 * <td>One equilateral triangle, pointing to a line at the left</td>
 * <td><img src="{@docRoot}/images/prevtrack.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_TRACK_NEXT</td>
 * <td>One equilateral triangle, pointing to a line at the right</td>
 * <td><img src="{@docRoot}/images/nexttrack.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_RECORD</td>
 * <td>A circle, normally red</td>
 * <td><img src="{@docRoot}/images/record.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_EJECT_TOGGLE</td>
 * <td>A line under a wide triangle which points up</td>
 * <td><img src="{@docRoot}/images/eject.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_VOLUME_UP</td>
 * <td>A ramp, increasing to the right, near a plus sign</td>
 * <td><img src="{@docRoot}/images/volup.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_VOLUME_DOWN</td>
 * <td>A ramp, increasing to the right, near a minus sign</td>
 * <td><img src="{@docRoot}/images/voldown.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_UP</td>
 * <td>An arrow pointing up</td>
 * <td><img src="{@docRoot}/images/up.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_DOWN</td>
 * <td>An arrow pointing down</td>
 * <td><img src="{@docRoot}/images/down.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_LEFT</td>
 * <td>An arrow pointing to the left</td>
 * <td><img src="{@docRoot}/images/left.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_RIGHT</td>
 * <td>An arrow pointing to the right</td>
 * <td><img src="{@docRoot}/images/right.gif" alt="*"></td>
 * </tr>
 * <tr>
 * <td>VK_POWER</td>
 * <td>A circle, broken at the top, with a vertical line in the break</td>
 * <td><img src="{@docRoot}/images/power.gif" alt="*"></td>
 * </tr>
 * </table>
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 */

public class HEventRepresentation extends Object
{
    /**
     * The event representation type for the current event is not supported.
     * <p>
     * The four ER_TYPE integers describe if an input mechanism is not
     * supported, or is described by a string, color or symbol.
     * <p>
     * The values of the four ER_TYPE integers are required to be bitwise
     * distinct, and the value of ER_TYPE_NOT_SUPPORTED should be 0.
     * 
     * @see org.havi.ui.event.HEventRepresentation#ER_TYPE_STRING
     * @see org.havi.ui.event.HEventRepresentation#ER_TYPE_COLOR
     * @see org.havi.ui.event.HEventRepresentation#ER_TYPE_SYMBOL
     */
    public static final int ER_TYPE_NOT_SUPPORTED = 0;

    /**
     * The event representation type for the current event is supported as a
     * string.
     * 
     * @see org.havi.ui.event.HEventRepresentation#ER_TYPE_NOT_SUPPORTED
     */
    public static final int ER_TYPE_STRING = 1 << 0;

    /**
     * The event representation type for the current event is supported as a
     * color.
     * 
     * @see org.havi.ui.event.HEventRepresentation#ER_TYPE_NOT_SUPPORTED
     */
    public static final int ER_TYPE_COLOR = 1 << 1;

    /**
     * The event representation type for the current event is supported as a
     * symbol.
     * 
     * @see org.havi.ui.event.HEventRepresentation#ER_TYPE_NOT_SUPPORTED
     */
    public static final int ER_TYPE_SYMBOL = 1 << 2;

    /**
     * It is not intended that applications should directly construct
     * {@link org.havi.ui.event.HEventRepresentation HEventRepresentation}
     * objects.
     * <p>
     * Creates an {@link org.havi.ui.event.HEventRepresentation
     * HEventRepresentation} object. See the class description for details of
     * constructor parameters and default values.
     * <p>
     * This method is protected to allow the platform to override it in a
     * different package scope.
     */
    protected HEventRepresentation()
    {
        // Not publicly instantiable
    }

    /**
     * This method returns true if the current event is supported by the
     * platform.
     */
    public boolean isSupported()
    {
        // Subclass should override
        return false;
    }

    /**
     * Sets the type of representation(s) available for the event code which
     * this this instance of {@link org.havi.ui.event.HEventRepresentation
     * HEventRepresentation} represents. If no representation(s) are available
     * then <code>aType</code> should be set to ER_TYPE_NOT_SUPPORTED. Otherwise
     * the representation should be set to the sum of one or more of the
     * following: ER_TYPE_STRING, ER_TYPE_COLOR, ER_TYPE_SYMBOL.
     * <p>
     * For example, if both string and color representations are available then
     * the system should call this method with the parameter set to
     * <code>ER_TYPE_STRING + ER_TYPE_COLOR</code>.
     * <p>
     * This method is protected to allow the platform to override it in
     * subclasses of HEventRepresentation. It is not intended for use by the
     * application and conformant applications shall not use this method.
     * 
     * @param aType
     *            the type of representation(s) available for this event
     */
    protected void setType(int aType)
    {
        // Only the least significant 3 bits of the type are valid to be set
        if ((aType & 0xFFFFFFF8) != 0) throw new IllegalArgumentException("See API documentation");

        type = aType;
    }

    /**
     * This returns the type of representation(s) available for the event code
     * which this instance of {@link org.havi.ui.event.HEventRepresentation
     * HEventRepresentation} represents.
     * <p>
     * If the event code can be represented in multiple ways, then the returned
     * type will be the sum of the supported types, e.g. an event generated by a
     * key with a particular font representation of an &quot;A&quot; in yellow
     * might return ER_TYPE_STRING + ER_TYPE_COLOR + ER_TYPE_SYMBOL. Where the
     * string representation is &quot;A&quot;, the color representation is
     * &quot;yellow&quot; and the symbol representation might be a likeness of
     * the &quot;A glyph&quot; from a particular font.
     */
    public int getType()
    {
        return type;
    }

    /**
     * Sets the Color representation for this
     * {@link org.havi.ui.event.HEventRepresentation HEventRepresentation}. Any
     * previous value is overwritten.
     * <p>
     * This method is protected to allow the platform to override it in
     * subclasses of HEventRepresentation. It is not intended for use by the
     * application and conformant applications shall not use this method.
     * 
     * @param aColor
     *            - the color to be associated with this event.
     */
    protected void setColor(java.awt.Color aColor)
    {
        color = aColor;
    }

    /**
     * This returns the color representation (generally used for colored soft
     * keys) of the current event code.
     * 
     * @return The color representation of the current event code, or null if
     *         not available.
     */
    public java.awt.Color getColor()
    {
        return color;
    }

    /**
     * Sets the string representation for this
     * {@link org.havi.ui.event.HEventRepresentation HEventRepresentation}. Any
     * previous value is overwritten.
     * <p>
     * This method is protected to allow the platform to override it in
     * subclasses of HEventRepresentation. It is not intended for use by the
     * application and conformant applications shall not use this method.
     * 
     * @param aText
     *            - the text string to be associated with this event.
     */
    protected void setString(java.lang.String aText)
    {
        string = aText;
    }

    /**
     * Returns the text representation of the current event code.
     * 
     * @return The text representation of the current event code, or null if not
     *         available.
     */
    public java.lang.String getString()
    {
        return string;
    }

    /**
     * Sets the symbolic representation for this
     * {@link org.havi.ui.event.HEventRepresentation HEventRepresentation}. Any
     * previous value is overwritten.
     * <p>
     * This method is protected to allow the platform to override it in
     * subclasses of HEventRepresentation. It is not intended for use by the
     * application and conformant applications shall not use this method.
     * 
     * @param aSymbol
     *            - the symbol image to be associated with this event.
     */
    protected void setSymbol(java.awt.Image aSymbol)
    {
        symbol = aSymbol;
    }

    /**
     * This returns an image-based representation (generally used for symbolic
     * keys) of the current event code.
     * <p>
     * Note that it is platform specific whether this method will return a valid
     * Image, in particular it is a valid implementation option to always return
     * null. Note that for non-null Images, the size and other Image
     * characteristics are dependent on particular manufacturer implementation.
     * 
     * @return The symbolic representation of the current event code, or null if
     *         not available.
     */
    public java.awt.Image getSymbol()
    {
        return symbol;
    }

    /** @see #getType() */
    private int type;

    /** @see #getSymbol() */
    private java.awt.Image symbol;

    /** @see #getString() */
    private String string;

    /** @see #getColor */
    private java.awt.Color color;
}