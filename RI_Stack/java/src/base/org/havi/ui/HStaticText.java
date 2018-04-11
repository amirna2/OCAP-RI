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

package org.havi.ui;

/**
 * The {@link org.havi.ui.HStaticText HStaticText} is a user interface component
 * used to display static read-only textual content which does <i>not</i> permit
 * the user to navigate (focus) upon it. By default it uses the
 * {@link org.havi.ui.HTextLook HTextLook} to render itself.
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
 * <td>x</td>
 * <td>x-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>y</td>
 * <td>y-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>width</td>
 * <td>width of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>height</td>
 * <td>height of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * 
 * 
 * 
 * <tr>
 * <td>textNormal</td>
 * <td>The string to be used as the content for the
 * {@link org.havi.ui.HState#NORMAL_STATE HState.NORMAL_STATE} state of this
 * component.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setTextContent setTextContent}</td>
 * <td>{@link org.havi.ui.HVisible#getTextContent getTextContent}</td>
 * </tr>
 * <tr>
 * <td>tlm</td>
 * <td>The text layout manager responsible for text formatting.</td>
 * <td>An {@link org.havi.ui.HDefaultTextLayoutManager
 * HDefaultTextLayoutManager} object.</td>
 * <td> {@link org.havi.ui.HVisible#setTextLayoutManager setTextLayoutManager}</td>
 * <td> {@link org.havi.ui.HVisible#getTextLayoutManager getTextLayoutManager}</td>
 * </tr>
 * 
 * <tr>
 * <td>font</td>
 * <td>The font for this component.</td>
 * <td>---</td>
 * <td><code>java.awt.Component#setFont</code></td>
 * <td><code>java.awt.Component#getFont</code></td>
 * </tr>
 * 
 * <tr>
 * <td>background</td>
 * <td>The background color for this component.</td>
 * <td>---</td>
 * <td><code>java.awt.Component#getBackground</code></td>
 * <td><code>java.awt.Component#setBackground</code></td>
 * </tr>
 * 
 * <tr>
 * <td>foreground</td>
 * <td>The foreground color for this component.</td>
 * <td>---</td>
 * <td><code>java.awt.Component#getForeground</code></td>
 * <td><code>java.awt.Component#setForeground</code></td>
 * </tr>
 * 
 * </table>
 * 
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>Associated matte ({@link org.havi.ui.HMatte HMatte}).</td>
 * <td>none (i.e. getMatte() returns <code>null</code>)</td>
 * <td>{@link org.havi.ui.HComponent#setMatte setMatte}</td>
 * <td>{@link org.havi.ui.HComponent#getMatte getMatte}</td>
 * </tr>
 * <tr>
 * <td>The text layout manager responsible for text formatting.</td>
 * <td>An {@link org.havi.ui.HDefaultTextLayoutManager} object.</td>
 * <td> {@link org.havi.ui.HVisible#setTextLayoutManager}</td>
 * <td> {@link org.havi.ui.HVisible#getTextLayoutManager}</td>
 * </tr>
 * 
 * <tr>
 * <td>The background painting mode</td>
 * <td>{@link org.havi.ui.HVisible#NO_BACKGROUND_FILL}</td>
 * 
 * <td>{@link org.havi.ui.HVisible#setBackgroundMode}</td>
 * <td>{@link org.havi.ui.HVisible#getBackgroundMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The default preferred size</td>
 * <td>not set (i.e. NO_DEFAULT_SIZE) unless specified by <code>width</code> and
 * <code>height</code> parameters</td>
 * <td>{@link org.havi.ui.HVisible#setDefaultSize}</td>
 * <td>{@link org.havi.ui.HVisible#getDefaultSize}</td>
 * </tr>
 * 
 * <tr>
 * <td>The horizontal content alignment</td>
 * <td>{@link org.havi.ui.HVisible#HALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setHorizontalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getHorizontalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The vertical content alignment</td>
 * <td>{@link org.havi.ui.HVisible#VALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setVerticalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getVerticalAlignment}</td>
 * </tr>
 * 
 * <tr>
 * <td>The content scaling mode</td>
 * <td>{@link org.havi.ui.HVisible#RESIZE_NONE}</td>
 * <td>{@link org.havi.ui.HVisible#setResizeMode}</td>
 * <td>{@link org.havi.ui.HVisible#getResizeMode}</td>
 * </tr>
 * 
 * <tr>
 * <td>The border mode</td>
 * <td><code>true</code></td>
 * <td>{@link org.havi.ui.HVisible#setBordersEnabled}</td>
 * <td>{@link org.havi.ui.HVisible#getBordersEnabled}</td>
 * </tr>
 * 
 * 
 * 
 * 
 * <tr>
 * <td>The default &quot;look&quot; for this class.</td>
 * <td>A platform specific {@link org.havi.ui.HTextLook HTextLook}</td>
 * <td>{@link org.havi.ui.HStaticText#setDefaultLook HStaticText.setDefaultLook}
 * </td>
 * <td>{@link org.havi.ui.HStaticText#getDefaultLook HStaticText.getDefaultLook}
 * </td>
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HTextLook HTextLook} returned from
 * HStaticText.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HStaticText#setLook HStaticText.setLook}</td>
 * <td>{@link org.havi.ui.HStaticText#getLook HStaticText.getLook}</td>
 * </tr>
 * </table>
 * 
 * @see HDefaultTextLayoutManager
 * @see HTextLayoutManager
 * @author Todd Earles
 * @author Aaron Kamienski (1.0.1 support)
 * @version 1.1
 */

public class HStaticText extends HVisible implements HNoInputPreferred
{
    /**
     * Creates an {@link org.havi.ui.HStaticText HStaticText} object. See the
     * class description for details of constructor parameters and default
     * values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticText()
    {
        super(getDefaultLook());
    }

    /**
     * Creates an {@link org.havi.ui.HStaticText HStaticText} object. See the
     * class description for details of constructor parameters and default
     * values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticText(java.lang.String textNormal, int x, int y, int width, int height)
    {
        super(getDefaultLook(), x, y, width, height);
        setTextContent(textNormal, NORMAL_STATE);
    }

    /**
     * Creates an {@link org.havi.ui.HStaticText HStaticText} object. See the
     * class description for details of constructor parameters and default
     * values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticText(java.lang.String textNormal, int x, int y, int width, int height, java.awt.Font font,
            java.awt.Color foreground, java.awt.Color background, HTextLayoutManager tlm)
    {
        this(textNormal, x, y, width, height);
        setFont(font);
        setForeground(foreground);
        setBackground(background);
        setTextLayoutManager(tlm);
    }

    /**
     * Creates an {@link org.havi.ui.HStaticText HStaticText} object. See the
     * class description for details of constructor parameters and default
     * values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticText(java.lang.String textNormal)
    {
        this();
        setTextContent(textNormal, NORMAL_STATE);
    }

    /**
     * Creates an {@link org.havi.ui.HStaticText HStaticText} object. See the
     * class description for details of constructor parameters and default
     * values. For constructors which specify content as parameters, see
     * 'State-based content' in HVisible for unspecified content associated with
     * other HStates.
     */
    public HStaticText(java.lang.String textNormal, java.awt.Font font, java.awt.Color foreground,
            java.awt.Color background, HTextLayoutManager tlm)
    {
        this(textNormal);
        setFont(font);
        setForeground(foreground);
        setBackground(background);
        setTextLayoutManager(tlm);
    }

    /**
     * Sets the {@link org.havi.ui.HLook HLook} for this component.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook HLook} that is to be used for
     *            this component. Note that this parameter may be null, in which
     *            case the component will not draw itself until a look is set.
     * @exception HInvalidLookException
     *                if the Look is not an {@link org.havi.ui.HTextLook
     *                HTextLook}.
     */
    public void setLook(HLook hlook) throws HInvalidLookException
    {
        if ((hlook != null) && !(hlook instanceof HTextLook)) throw new HInvalidLookException();
        super.setLook(hlook);
    }

    /**
     * Sets the default {@link org.havi.ui.HLook HLook} for further
     * {@link org.havi.ui.HStaticText HStaticText} Components.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook HLook} that will be used by
     *            default when creating a new {@link org.havi.ui.HStaticText
     *            HStaticText} component. Note that this parameter may be null,
     *            in which case newly created components shall not draw
     *            themselves until a non-null look is set using the
     *            {@link org.havi.ui.HStaticText#setLook setLook} method.
     */
    public static void setDefaultLook(HTextLook hlook)
    {
        setDefaultLookImpl(PROPERTY_LOOK, hlook);
    }

    /**
     * Returns the currently set default {@link org.havi.ui.HLook HLook} for
     * {@link org.havi.ui.HStaticText HStaticText} components.
     * 
     * @return The {@link org.havi.ui.HLook HLook} that is used by default when
     *         creating a new {@link org.havi.ui.HStaticText HStaticText}
     *         component.
     */
    public static HTextLook getDefaultLook()
    {
        return (HTextLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * The property which specifies the platform-specific default look for this
     * class. Can be retreived with HaviToolkit.getProperty().
     */
    private static final String PROPERTY_LOOK = "org.havi.ui.HStaticText.defaultLook";

    /**
     * The type of <code>HLook</code> to use as the default if not explicitly
     * overridden.
     */
    static final Class DEFAULT_LOOK = HTextLook.class;
}
