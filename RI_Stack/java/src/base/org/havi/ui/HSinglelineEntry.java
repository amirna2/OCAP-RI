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

import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.KeySet;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyEvent;

import org.havi.ui.event.HFocusEvent;
import org.havi.ui.event.HFocusListener;
import org.havi.ui.event.HKeyEvent;
import org.havi.ui.event.HKeyListener;
import org.havi.ui.event.HTextEvent;
import org.havi.ui.event.HTextListener;

/**
 * The {@link org.havi.ui.HSinglelineEntry} is a user interface component used
 * to receive a single line of alphanumeric entry from the user and can also be
 * used for password input.
 * 
 * <p>
 * Upon creation the {@link org.havi.ui.HSinglelineEntry} is set to a
 * non-editable mode identical in functionality to an {@link org.havi.ui.HText}.
 * 
 * <p>
 * On keyboard-based systems, if the user navigates to the component using the
 * keyboard then the component must first be switched into edit mode before it
 * will accept any key presses (other than for navigation to another component).
 * The mechanism by which the component is switched into and out of edit mode is
 * via <code>HTextEvent</code> events with ids
 * {@link org.havi.ui.event.HTextEvent#TEXT_START_CHANGE} and
 * {@link org.havi.ui.event.HTextEvent#TEXT_END_CHANGE}, which may be triggered
 * in response to a key stroke or other Java AWT event.
 * 
 * <p>
 * On entering its editable mode the component will send an
 * <code>HTextEvent</code> event with an id of
 * {@link org.havi.ui.event.HTextEvent#TEXT_START_CHANGE} to all registered
 * {@link org.havi.ui.event.HTextListener} listeners. The
 * {@link org.havi.ui.HSinglelineEntry} will then respond to key events by
 * inserting characters into the text string or positioning the insertion point
 * (caret) via further {@link org.havi.ui.event.HTextEvent} events.
 * 
 * <p>
 * For example, on platforms which do not provide a means of positioning the
 * caret independently from navigating to components, the navigation keys will
 * be interpreted as caret positioning keys in this mode.
 * 
 * <p>
 * While in the editing mode, the component will generate an
 * <code>HTextEvent</code> event with an id of
 * {@link org.havi.ui.event.HTextEvent#TEXT_CHANGE} whenever the text content of
 * the HSinglelineEntry changes (e.g. a character is inserted).
 * 
 * <p>
 * 
 * On receiving an <code>HTextEvent</code> event with an id of
 * {@link org.havi.ui.event.HTextEvent#TEXT_END_CHANGE} the component shall
 * leave its editable mode and send an <code>HTextEvent</code> event with an id
 * of {@link org.havi.ui.event.HTextEvent#TEXT_END_CHANGE} to all registered
 * {@link org.havi.ui.event.HTextListener HTextListener} listeners. The user can
 * then navigate out of the {@link org.havi.ui.HSinglelineEntry}.
 * 
 * <p>
 * On mouse-based systems, if the user selects the component by clicking a mouse
 * button inside its bounds then the {@link org.havi.ui.HSinglelineEntry} will
 * automatically switch into edit mode and generate an <code>HTextEvent</code>
 * with an id of {@link org.havi.ui.event.HTextEvent#TEXT_START_CHANGE}. It will
 * stay in edit mode so long as the mouse pointer remains within the bounds of
 * the component. Once the mouse pointer leaves the bounds then it will switch
 * back into non-editable mode and generate an <code>HTextEvent</code> with an
 * id of {@link org.havi.ui.event.HTextEvent#TEXT_END_CHANGE}.
 * 
 * <p>
 * By default {@link org.havi.ui.HSinglelineEntry} uses the
 * {@link org.havi.ui.HSinglelineEntryLook} to render itself.
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
 * <td>text</td>
 * <td>The text within this {@link org.havi.ui.HSinglelineEntry}, to be used as
 * the displayed and editable content for all states.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HVisible#setTextContent}</td>
 * <td>{@link org.havi.ui.HVisible#getTextContent}</td>
 * </tr>
 * 
 * <tr>
 * <td>maxChars</td>
 * <td>The maximum number of characters allowed in this
 * {@link org.havi.ui.HSinglelineEntry}.</td>
 * <td>16 characters</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#setMaxChars}</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#getMaxChars}</td>
 * </tr>
 * 
 * <tr>
 * <td>font</td>
 * <td>The font to be used for this component.</td>
 * <td>---</td>
 * <td><code>java.awt.Component#setFont</code>.</td>
 * <td><code>java.awt.Component#getFont</code>.</td>
 * </tr>
 * 
 * <tr>
 * <td>color</td>
 * <td>The color to be used for this component.</td>
 * <td>---</td>
 * <td><code>java.awt.Component#setForeground</code>.</td>
 * <td><code>java.awt.Component#getForeground</code>.</td>
 * </tr>
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
 * <td>Caret position</td>
 * <td>At the end of the current text string</td>
 * <td>{@link HSinglelineEntry#setCaretCharPosition}</td>
 * <td>{@link HSinglelineEntry#getCaretCharPosition}</td>
 * </tr>
 * 
 * <tr>
 * <td>Input type</td>
 * <td>{@link HSinglelineEntry#INPUT_ANY}</td>
 * <td>{@link HSinglelineEntry#setType}</td>
 * <td>{@link HSinglelineEntry#getType}</td>
 * </tr>
 * 
 * <tr>
 * <td>Customized input range</td>
 * <td>null</td>
 * <td>{@link HSinglelineEntry#setValidInput}</td>
 * <td>{@link HSinglelineEntry#getValidInput}</td>
 * </tr>
 * 
 * <tr>
 * <td>Password protection (the echo character)</td>
 * <td>Zero (ASCII NUL), i.e. not password protected.</td>
 * <td>{@link HSinglelineEntry#setEchoChar}</td>
 * <td>{@link HSinglelineEntry#getEchoChar} and
 * {@link HSinglelineEntry#echoCharIsSet}</td>
 * </tr>
 * <tr>
 * <td>The default &quot;look&quot; for this class.</td>
 * <td>A platform specific {@link org.havi.ui.HSinglelineEntryLook
 * HSinglelineEntryLook}</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#setDefaultLook
 * HSinglelineEntry.setDefaultLook}</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#getDefaultLook
 * HSinglelineEntry.getDefaultLook}</td>
 * </tr>
 * 
 * <tr>
 * <td>The &quot;look&quot; for this object.</td>
 * <td>The {@link org.havi.ui.HSinglelineEntryLook HSinglelineEntryLook}
 * returned from HSinglelineEntry.getDefaultLook when this object was created.</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#setLook HSinglelineEntry.setLook}</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#getLook HSinglelineEntry.getLook}</td>
 * </tr>
 * <tr>
 * <td>The gain focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#setGainFocusSound setGainFocusSound}</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#getGainFocusSound getGainFocusSound}</td>
 * </tr>
 * <tr>
 * <td>The lose focus sound.</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#setLoseFocusSound setLoseFocusSound}</td>
 * <td>{@link org.havi.ui.HSinglelineEntry#getLoseFocusSound getLoseFocusSound}</td>
 * </tr>
 * </table>
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski (1.1 support)
 * @version 1.1
 */

public class HSinglelineEntry extends HVisible implements HTextValue
{
    /**
     * Creates an {@link org.havi.ui.HSinglelineEntry} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HSinglelineEntry()
    {
        super(getDefaultLook());
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HSinglelineEntry} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HSinglelineEntry(String text, int x, int y, int width, int height, int maxChars, Font font, Color color)
    {
        super(getDefaultLook(), x, y, width, height);
        iniz();
        this.maxChars = maxChars;
        setFont(font);
        setForeground(color);
        setTextContent(text, HState.NORMAL_STATE);
    }

    /**
     * Creates an {@link org.havi.ui.HSinglelineEntry} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HSinglelineEntry(int x, int y, int width, int height, int maxChars)
    {
        super(getDefaultLook(), x, y, width, height);
        iniz();
        this.maxChars = maxChars;
    }

    /**
     * Creates an {@link org.havi.ui.HSinglelineEntry} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HSinglelineEntry(String text, int maxChars, Font font, Color color)
    {
        super(getDefaultLook());
        iniz();
        this.maxChars = maxChars;
        setFont(font);
        setForeground(color);
        setTextContent(text, HState.NORMAL_STATE);
    }

    /**
     * Creates an {@link org.havi.ui.HSinglelineEntry} object. See the class
     * description for details of constructor parameters and default values.
     */
    public HSinglelineEntry(int maxChars)
    {
        super(getDefaultLook());
        iniz();
        this.maxChars = maxChars;
    }

    /**
     * Sets the text content used in this {@link org.havi.ui.HSinglelineEntry}.
     * <p>
     * Note that {@link org.havi.ui.HSinglelineEntry} components do not support
     * separate pieces of textual content per state (as defined in
     * {@link org.havi.ui.HState}) --- rather a single piece of content is
     * defined for all its interaction states.
     * <p>
     * Additionally, the {@link org.havi.ui.HSinglelineEntry#setTextContent}
     * method truncates the string according to the current maxChars setting.
     * 
     * @param string
     *            The content. If the content is null, then any currently
     *            assigned content shall be removed for the specified state.
     * @param state
     *            The state of the component for which this content should be
     *            displayed. This parameter shall be ignored and considered to
     *            have the value {@link HState#ALL_STATES}.
     * 
     * @see HSinglelineEntry#getTextContent
     */
    public void setTextContent(String string, int state)
    {
        String oldText = content.toString();

        if (string == null)
        {
            content.clear();
            isTextNull = true;
        }
        else
        {
            isTextNull = false;

            // Truncate the string if it's too long.
            content.setText(string.substring(0, Math.min(string.length(), getMaxChars())));
            // Put the caret at the end of the text.
            setCaretCharPosition(Integer.MAX_VALUE);
        }
        markTextChanged(oldText);
    }

    private void markTextChanged(String oldText)
    {
        addWidgetChanged(HVisible.TEXT_CONTENT_CHANGE, new Object[] { new Integer(HVisible.NORMAL_STATE), oldText,
                oldText, oldText, oldText, oldText, oldText, oldText, oldText, oldText });

        if (textListeners != null) textListeners.textChanged(new HTextEvent(this, HTextEvent.TEXT_CHANGE));
    }

    private void addWidgetChanged(int hint, Object[] data)
    {
        if (getLook() != null) getLook().widgetChanged(this, new HChangeData[] { new HChangeData(hint, data) });
    }

    /**
     * Gets the text content used in this {@link org.havi.ui.HSinglelineEntry
     * HSinglelineEntry}.
     * <p>
     * Note that {@link org.havi.ui.HSinglelineEntry} components do not support
     * separate pieces of textual content per state (as defined in
     * {@link org.havi.ui.HState}) --- rather a single piece of content is
     * defined for all its interaction states.
     * 
     * @param state
     *            The state of the component for which this content should be
     *            displayed. This parameter shall be ignored.
     * @return the text content used in this
     *         {@link org.havi.ui.HSinglelineEntry}
     * @see HSinglelineEntry#setTextContent
     */
    public String getTextContent(int state)
    {
        return isTextNull ? null : content.toString();
    }

    /**
     * Gets the position of the text insertion caret for this the current line
     * in this text component. The valid values of the caret position are from 0
     * to the length of the string retrieved using
     * {@link org.havi.ui.HSinglelineEntry#getTextContent getTextContent}, where
     * 0 implies insertion as the first character (i.e. at the start of the
     * string) and {@link org.havi.ui.HSinglelineEntry#getTextContent
     * (getTextContent()).length()} implies that further characters are to be
     * appended onto the end of the string. Hence, the valid caret positions for
     * the string &quot;abc&quot; of length 3, are 0, 1, 2 and 3 --- with caret
     * locations as shown below:
     * 
     * <pre>
     * 0 &quot;a&quot; 1 &quot;b&quot; 2 &quot;c&quot; 3
     * </pre>
     * 
     * @return the position of the text insertion caret.
     */
    public int getCaretCharPosition()
    {
        return caretPos;
    }

    /**
     * Sets the position of the text insertion caret for this text component. If
     * position is not valid for the current content the caret is moved to the
     * nearest position.
     * 
     * @param position
     *            the new position of the text insertion caret.
     * @return the new caret position.
     */
    public int setCaretCharPosition(int position)
    {
        int size = content.getSize();
        int oldPos = caretPos;

        // Make sure that the desired position is within range and that we are
        // trying to set it to a new value.
        position = (position < 0) ? 0 : ((position > size) ? size : position);

        if (position != caretPos)
        {
            caretPos = position;

            addWidgetChanged(HVisible.CARET_POSITION_CHANGE, new Object[] { new Integer(oldPos) });

            if (textListeners != null) textListeners.caretMoved(new HTextEvent(this, HTextEvent.TEXT_CARET_CHANGE));
        }
        return caretPos;
    }

    /**
     * Set to indicate to the system which input keys are required by this
     * component. The input type constants can be added to define the union of
     * the character sets corresponding to the respective constants.
     * 
     * @param type
     *            sum of one or several of
     *            {@link org.havi.ui.HKeyboardInputPreferred#INPUT_ANY},
     *            {@link org.havi.ui.HKeyboardInputPreferred#INPUT_NUMERIC},
     *            {@link org.havi.ui.HKeyboardInputPreferred#INPUT_ALPHA} or
     *            {@link org.havi.ui.HKeyboardInputPreferred#INPUT_CUSTOMIZED}.
     */
    public void setType(int type)
    {
        // Make sure it's not the same value.
        if (type != this.type)
        {
            // Make sure it's a valid type.
            // Bitwise ORing of all types accepted.
            if (0 != (type & ~(INPUT_ANY | INPUT_ALPHA | INPUT_NUMERIC | INPUT_CUSTOMIZED)))
            {
                throw new IllegalArgumentException("See API documentation");
            }
            this.type = type;
        }
    }

    /**
     * Defines the set of the characters which are valid for customized keyboard
     * input, i.e. when the input type is set to
     * {@link org.havi.ui.HKeyboardInputPreferred#INPUT_CUSTOMIZED}.
     * 
     * @param inputChars
     *            an array of characters which comprises the valid input
     *            characters.
     */
    public void setValidInput(char[] inputChars)
    {
        // Empty the custom input set
        customInputChars = new KeySet();

        // Add the new custom input characters into the set
        for (int x = 0; x < inputChars.length; x++)
            customInputChars.put(inputChars[x], this);
    }

    /**
     * Determine if this component has an echo character set, i.e. if the echo
     * character is non-zero.
     * 
     * @return true if an echo character is set, false otherwise.
     */
    public boolean echoCharIsSet()
    {
        return (echoChar != '\0');
    }

    /**
     * Returns the character to be used for echoing.
     * 
     * @return the character to be used for echoing or 0 (ASCII NUL) if no echo
     *         character is set.
     */
    public char getEchoChar()
    {
        return echoChar;
    }

    /**
     * Sets the number of character to echo for this component.
     * 
     * @param c
     *            the character used to echo any input, e.g. if c == '*' a
     *            password-style input will be displayed. If c is zero (ASCII
     *            NUL), then all characters will be echoed on-screen, this is
     *            the default behavior.
     */
    public void setEchoChar(char c)
    {
        if (c != echoChar)
        {
            char oldChar = echoChar;

            echoChar = c;

            addWidgetChanged(HVisible.ECHO_CHAR_CHANGE, new Object[] { new Character(oldChar) });
        }
    }

    /**
     * Sets the default {@link org.havi.ui.HLook HLook} for further
     * {@link org.havi.ui.HSinglelineEntry} Components.
     * 
     * @param look
     *            The {@link org.havi.ui.HLook} that will be used by default
     *            when creating a new {@link org.havi.ui.HSinglelineEntry}
     *            component. Note that this parameter may be null, in which case
     *            newly created components shall not draw themselves until a
     *            non-null look is set using the
     *            {@link org.havi.ui.HSinglelineEntry#setLook} method.
     */
    public static void setDefaultLook(HSinglelineEntryLook look)
    {
        setDefaultLookImpl(PROPERTY_LOOK, look);
    }

    /**
     * Returns the currently set default {@link org.havi.ui.HLook} for
     * {@link org.havi.ui.HSinglelineEntry} components.
     * 
     * @return The {@link org.havi.ui.HLook} that is used by default when
     *         creating a new {@link org.havi.ui.HSinglelineEntry} component.
     */
    public static HSinglelineEntryLook getDefaultLook()
    {
        return (HSinglelineEntryLook) getDefaultLookImpl(PROPERTY_LOOK, DEFAULT_LOOK);
    }

    /**
     * Sets the {@link org.havi.ui.HLook} for this component.
     * 
     * @param hlook
     *            The {@link org.havi.ui.HLook} that is to be used for this
     *            component. Note that this parameter may be null, in which case
     *            the component will not draw itself until a look is set.
     * @exception HInvalidLookException
     *                If the Look is not an
     *                {@link org.havi.ui.HSinglelineEntryLook}.
     */
    public void setLook(HLook hlook) throws HInvalidLookException
    {
        if ((hlook != null) && !(hlook instanceof HSinglelineEntryLook)) throw new HInvalidLookException();
        super.setLook(hlook);
    }

    /**
     * Insert a character at the current caret position, subject to the maximum
     * number of input characters.
     * 
     * @param c
     *            the character to insert
     * @return true if the character was inserted, false otherwise.
     */
    public boolean insertChar(char c)
    {
        if (content.getSize() < getMaxChars()
                && ((0 != (type & INPUT_ANY) && Character.isDefined(c))
                        || (0 != (type & INPUT_NUMERIC) && Character.isDigit(c))
                        || (0 != (type & INPUT_ALPHA) && Character.isLetter(c)) || (0 != (type & INPUT_CUSTOMIZED) && customInputChars.get(c) != null)))
        {
            // This is a special case
            if (c == KeyEvent.VK_ENTER) c = '\n';

            String oldText = content.toString();

            // Add the character
            content.insert(c, caretPos);
            isTextNull = false;
            markTextChanged(oldText);
            caretNextCharacter();

            return true;
        }
        else
        {
            if (getEditMode()) beep();
            return false;
        }
    }

    /**
     * Delete a character behind the current caret position.
     * 
     * @return true if a character was deleted, false otherwise.
     */
    public boolean deletePreviousChar()
    {
        try
        {
            String oldText = content.toString();

            content.remove(caretPos - 1);
            markTextChanged(oldText);
            caretPreviousCharacter();
            return true;
        }
        catch (Exception e)
        {
        }

        if (getEditMode()) beep();
        return false;
    }

    /**
     * Delete a character forward of the current caret position.
     * 
     * @return true if a character was deleted, false otherwise.
     */
    public boolean deleteNextChar()
    {
        try
        {
            String oldText = content.toString();

            content.remove(caretPos);
            markTextChanged(oldText);
            return true;
        }
        catch (Exception e)
        {
        }

        if (getEditMode()) beep();
        return false;
    }

    /**
     * Move the caret to the next character. The caret position is constrained
     * such that the insert point lies within the range as defined by the
     * {@link org.havi.ui.HSinglelineEntry#getCaretCharPosition} method.
     */
    public void caretNextCharacter()
    {
        setCaretCharPosition(getCaretCharPosition() + 1);
    }

    /**
     * Move the caret to the previous character. The caret position is
     * constrained such that the insert point lies within the range as defined
     * by the {@link org.havi.ui.HSinglelineEntry#getCaretCharPosition} method.
     */
    public void caretPreviousCharacter()
    {
        setCaretCharPosition(getCaretCharPosition() - 1);
    }

    /**
     * Set maximum number of characters.
     * 
     * @param maxChars
     *            - the maximum number of characters.
     */
    public void setMaxChars(int maxChars)
    {
        this.maxChars = maxChars;
        // Should we truncate here if necessary?
    }

    /**
     * Get maximum number of characters. The behavior of the component when the
     * last character on a line is typed is implementation dependent.
     * 
     * @return the maximum number of characters.
     */
    public int getMaxChars()
    {
        return maxChars;
    }

    /**
     * Defines the navigation path from the current
     * {@link org.havi.ui.HNavigable HNavigable} to another
     * {@link org.havi.ui.HNavigable HNavigable} when a particular key is
     * pressed.
     * <p>
     * Note that {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is equivalent to multiple calls to
     * {@link org.havi.ui.HNavigable#setMove setMove}, where the key codes
     * <code>VK_UP</code>, <code>VK_DOWN</code>, <code>VK_LEFT</code>,
     * <code>VK_RIGHT</code> are used.
     * 
     * @param keyCode
     *            The key code of the pressed key. Any numerical keycode is
     *            allowed, but the platform may not be able to generate all
     *            keycodes. Application authors should only use keys for which
     *            <code>HRcCapabilities.isSupported()</code> or
     *            <code>HKeyCapabilities.isSupported()</code> returns true.
     * @param target
     *            The target {@link org.havi.ui.HNavigable HNavigable} object
     *            that should be navigated to. If a target is to be removed from
     *            a particular navigation path, then <code>null</code> shall be
     *            specified.
     */
    public void setMove(int keyCode, HNavigable target)
    {
        moves.put(keyCode, target);
    }

    /**
     * Provides the {@link org.havi.ui.HNavigable HNavigable} object that is
     * navigated to when a particular key is pressed.
     * 
     * @param keyCode
     *            The key code of the pressed key.
     * @return Returns the {@link org.havi.ui.HNavigable HNavigable} object or
     *         <code>null</code> if no {@link org.havi.ui.HNavigable HNavigable}
     *         is associated with the keyCode.
     */
    public HNavigable getMove(int keyCode)
    {
        return (HNavigable) moves.get(keyCode);
    }

    /**
     * Set the focus control for an {@link org.havi.ui.HNavigable HNavigable}
     * component. Note {@link org.havi.ui.HNavigable#setFocusTraversal
     * setFocusTraversal} is a convenience function for application programmers
     * where a standard up, down, left and right focus traversal between
     * components is required.
     * <p>
     * Note {@link org.havi.ui.HNavigable#setFocusTraversal setFocusTraversal}
     * is equivalent to multiple calls to {@link org.havi.ui.HNavigable#setMove
     * setMove}, where the key codes VK_UP, VK_DOWN, VK_LEFT, VK_RIGHT are used.
     * <p>
     * Note that this API does not prevent the creation of &quot;isolated&quot;
     * {@link org.havi.ui.HNavigable HNavigable} components --- authors should
     * endeavor to avoid confusing the user.
     * 
     * @param up
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_UP KeyEvent. If there is
     *            no {@link org.havi.ui.HNavigable HNavigable} component to move
     *            &quot;up&quot; to, then null shall be specified.
     * @param down
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_DOWN KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;down&quot; to, then null shall be specified.
     * @param left
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_LEFT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;left&quot; to, then null shall be specified.
     * @param right
     *            The {@link org.havi.ui.HNavigable HNavigable} component to
     *            move to, when the user generates a VK_RIGHT KeyEvent. If there
     *            is no {@link org.havi.ui.HNavigable HNavigable} component to
     *            move &quot;right&quot; to, then null shall be specified.
     */
    public void setFocusTraversal(HNavigable up, HNavigable down, HNavigable left, HNavigable right)
    {
        setMove(KeyEvent.VK_UP, up);
        setMove(KeyEvent.VK_DOWN, down);
        setMove(KeyEvent.VK_LEFT, left);
        setMove(KeyEvent.VK_RIGHT, right);
    }

    /**
     * Indicates if this component has focus.
     * 
     * @return <code>true</code> if the component has focus, otherwise returns
     *         <code>false</code>.
     */
    public boolean isSelected()
    {
        return (getInteractionState() & FOCUSED_STATE_BIT) != 0;
    }

    /**
     * Associate a sound with gaining focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * <code>FOCUS_GAINED</code>. This sound will start to be played when an
     * object implementing this interface gains focus. It is not guaranteed to
     * be played to completion. If the object implementing this interface loses
     * focus before the audio completes playing, the audio will be truncated.
     * Applications wishing to ensure the audio is always played to completion
     * must implement special logic to slow down the focus transitions.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any gain focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component gains focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setGainFocusSound(HSound sound)
    {
        gainFocusSound = sound;
    }

    /**
     * Associate a sound with losing focus, i.e. when the
     * {@link org.havi.ui.HNavigable HNavigable} receives a
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} event of type
     * FOCUS_LOST. This sound will start to be played when an object
     * implementing this interface loses focus. It is not guaranteed to be
     * played to completion. It is implementation dependent whether and when
     * this sound will be truncated by any gain focus sound played by the next
     * object to gain focus.
     * <p>
     * By default, an {@link org.havi.ui.HNavigable HNavigable} object does not
     * have any lose focus sound associated with it.
     * <p>
     * Note that the ordering of playing sounds is dependent on the order of the
     * focus lost, gained events.
     * 
     * @param sound
     *            the sound to be played, when the component loses focus. If
     *            sound content is already set, the original content is
     *            replaced. To remove the sound specify a null
     *            {@link org.havi.ui.HSound HSound}.
     */
    public void setLoseFocusSound(HSound sound)
    {
        loseFocusSound = sound;
    }

    /**
     * Get the sound associated with the gain focus event.
     * 
     * @return The sound played when the component gains focus. If no sound is
     *         associated with gaining focus, then null shall be returned.
     */
    public HSound getGainFocusSound()
    {
        return gainFocusSound;
    }

    /**
     * Get the sound associated with the lost focus event.
     * 
     * @return The sound played when the component loses focus. If no sound is
     *         associated with losing focus, then null shall be returned.
     */
    public HSound getLoseFocusSound()
    {
        return loseFocusSound;
    }

    /**
     * Adds the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} to receive {@link org.havi.ui.event.HFocusEvent
     * HFocusEvent} events sent from this {@link org.havi.ui.HNavigable
     * HNavigable}: If the listener has already been added further calls will
     * add further references to the listener, which will then receive multiple
     * copies of a single event.
     * 
     * @param l
     *            the HFocusListener to add
     */
    public void addHFocusListener(org.havi.ui.event.HFocusListener l)
    {
        listeners = HEventMulticaster.add(listeners, l);
    }

    /**
     * Removes the specified {@link org.havi.ui.event.HFocusListener
     * HFocusListener} so that it no longer receives
     * {@link org.havi.ui.event.HFocusEvent HFocusEvent} events from this
     * {@link org.havi.ui.HNavigable HNavigable}. If the specified listener is
     * not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param l
     *            the HFocusListener to remove
     */
    public void removeHFocusListener(org.havi.ui.event.HFocusListener l)
    {
        listeners = HEventMulticaster.remove(listeners, l);
    }

    /**
     * Retrieve the set of key codes which this component maps to navigation
     * targets.
     * 
     * @return an array of key codes, or <code>null</code> if no navigation
     *         targets are set on this component.
     */
    public int[] getNavigationKeys()
    {
        return moves.getKeysNull();
    }

    /**
     * Process an {@link org.havi.ui.event.HFocusEvent HFocusEvent} sent to this
     * {@link org.havi.ui.HSinglelineEntry HSinglelineEntry}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HFocusEvent HFocusEvent} to
     *            process.
     */
    public void processHFocusEvent(HFocusEvent evt)
    {
        HSound sound = null;
        int state = getInteractionState();

        switch (evt.getID())
        {
            case HFocusEvent.FOCUS_GAINED:
                // Enter the focused state.
                if (!isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    setInteractionState(state);
                    sound = getGainFocusSound();
                    if (sound != null) sound.play();
                }
                if (listeners != null) listeners.focusGained(evt);
                break;

            case HFocusEvent.FOCUS_LOST:
                // Leave the focused state.
                if (isSelected())
                {
                    state ^= FOCUSED_STATE_BIT;
                    setInteractionState(state);
                    sound = getLoseFocusSound();
                    if (sound != null) sound.play();
                }
                if (listeners != null) listeners.focusLost(evt);
                break;

            case HFocusEvent.FOCUS_TRANSFER:
                // Transfer focus.
                int id = evt.getTransferId();
                HNavigable target;
                if (id != HFocusEvent.NO_TRANSFER_ID && (target = getMove(id)) != null)
                    ((Component) target).requestFocus();
                // Does not notify listeners.
                break;
        }
    }

    /**
     * Adds the specified {@link org.havi.ui.event.HKeyListener HKeyListener} to
     * receive {@link org.havi.ui.event.HKeyEvent HKeyEvent} events sent from
     * this {@link org.havi.ui.HTextValue HTextValue}: If the listener has
     * already been added further calls will add further references to the
     * listener, which will then receive multiple copies of a single event.
     * 
     * @param l
     *            the HKeyListener to add
     */
    public void addHKeyListener(org.havi.ui.event.HKeyListener l)
    {
        keyListeners = HEventMulticaster.add(keyListeners, l);
    }

    /**
     * Removes the specified {@link org.havi.ui.event.HKeyListener HKeyListener}
     * so that it no longer receives {@link org.havi.ui.event.HKeyEvent
     * HKeyEvent} events from this {@link org.havi.ui.HTextValue HTextValue}. If
     * the specified listener is not registered, the method has no effect. If
     * multiple references to a single listener have been registered it should
     * be noted that this method will only remove one reference per call.
     * 
     * @param l
     *            the HKeyListener to remove
     */
    public void removeHKeyListener(org.havi.ui.event.HKeyListener l)
    {
        keyListeners = HEventMulticaster.remove(keyListeners, l);
    }

    /**
     * Adds the specified {@link org.havi.ui.event.HTextListener HTextListener}
     * to receive {@link org.havi.ui.event.HTextEvent HTextEvent} events sent
     * from this {@link org.havi.ui.HTextValue HTextValue}: If the listener has
     * already been added further calls will add further references to the
     * listener, which will then receive multiple copies of a single event.
     * 
     * @param l
     *            the HTextListener to add
     */
    public void addHTextListener(org.havi.ui.event.HTextListener l)
    {
        textListeners = HEventMulticaster.add(textListeners, l);
    }

    /**
     * Removes the specified {@link org.havi.ui.event.HTextListener
     * HTextListener} so that it no longer receives
     * {@link org.havi.ui.event.HTextEvent HTextEvent} events from this
     * {@link org.havi.ui.HTextValue HTextValue}. If the specified listener is
     * not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param l
     *            the HTextListener to remove
     */
    public void removeHTextListener(org.havi.ui.event.HTextListener l)
    {
        textListeners = HEventMulticaster.remove(textListeners, l);
    }

    /**
     * Get the editing mode for this {@link org.havi.ui.HSinglelineEntry}. If
     * the returned value is <code>true</code> the component is in edit mode,
     * and its textual content may be changed through user interaction such as
     * keyboard events.
     * <p>
     * The component is switched into and out of edit mode on receiving
     * {@link org.havi.ui.event.HTextEvent#TEXT_START_CHANGE} and
     * {@link org.havi.ui.event.HTextEvent#TEXT_END_CHANGE} events.
     * 
     * @return <code>true</code> if this component is in edit mode,
     *         <code>false</code> otherwise.
     */
    public boolean getEditMode()
    {
        return editMode;
    }

    /**
     * Set the editing mode for this {@link org.havi.ui.HSinglelineEntry}.
     * <p>
     * This method is provided for the convenience of component implementors.
     * Interoperable applications shall not call this method. It cannot be made
     * protected because interfaces cannot have protected methods.
     * 
     * @param edit
     *            true to switch this component into edit mode, false otherwise.
     * @see HKeyboardInputPreferred#getEditMode
     */
    public void setEditMode(boolean edit)
    {
        // Ignore changes to edit mode while the virtual keyboard is displayed
        if (virtualKeyboardShowing) return;

        // Check to make sure we're actually trying to change
        // the edit mode and that the component is not currently disabled.
        if ((edit != editMode) && ((getInteractionState() & DISABLED_STATE_BIT) == 0))
        {
            boolean oldEditMode = editMode;

            editMode = edit;

            // Let the look know that the edit mode of this component
            // has changed.
            addWidgetChanged(HVisible.EDIT_MODE_CHANGE, new Object[] { new Boolean(oldEditMode) });

            // Send an event to any HTextListeners specifying whether we're
            // entering or leaving edit mode.
            if (textListeners != null)
                textListeners.textChanged(new HTextEvent(this, edit == true ? HTextEvent.TEXT_START_CHANGE
                        : HTextEvent.TEXT_END_CHANGE));

            // If we entered edit mode then call the toolkit to let it show
            // the virtual keyboard. If there is a virtual keyboard then the
            // toolkit will block until all input has been entered and the user
            // exits edit mode at which time it will return true. Otherwise, it
            // returns false immediately. In the latter case we should stay in
            // edit mode.
            if (editMode == true)
            {
                virtualKeyboardShowing = true;
                boolean showed = HaviToolkit.getToolkit().showVirtualKeyboard(this);
                virtualKeyboardShowing = false;
                if (showed) setEditMode(false);
            }
        }
    }

    /**
     * Retrieve the desired input type for this component. This value should be
     * set to indicate to the system which input keys are required by this
     * component. The input type constants can be added to define the union of
     * the character sets corresponding to the respective constants.
     * 
     * 
     * @return The sum of one or several of
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_ANY},
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_NUMERIC},
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_ALPHA}, or
     *         {@link org.havi.ui.HKeyboardInputPreferred#INPUT_CUSTOMIZED}.
     */
    public int getType()
    {
        return type;
    }

    /**
     * Retrieve the customized input character range. If <code>getType()</code>
     * returns a value with the INPUT_CUSTOMIZED bit set then this method shall
     * return an array containing the range of customized input keys. If the
     * range of customized input keys has not been set then this method shall
     * return a zero length char array. This method shall return null if
     * <code>getType()</code> returns a value without the INPUT_CUSTOMIZED bit
     * set.
     * 
     * @return an array containing the characters which this component expects
     *         the platform to provide, or <code>null</code>.
     */
    public char[] getValidInput()
    {
        return ((type & INPUT_CUSTOMIZED) == 0) ? null : customInputChars.getChars();
    }

    /**
     * Process an {@link org.havi.ui.event.HTextEvent HTextEvent} sent to this
     * {@link org.havi.ui.HSinglelineEntry}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HTextEvent} to process.
     */
    public void processHTextEvent(org.havi.ui.event.HTextEvent evt)
    {
        switch (evt.getID())
        {
            case HTextEvent.CARET_NEXT_CHAR:
                caretNextCharacter();
                break;
            case HTextEvent.CARET_PREV_CHAR:
                caretPreviousCharacter();
                break;
            case HTextEvent.CARET_NEXT_LINE:
            case HTextEvent.CARET_NEXT_PAGE:
                setCaretCharPosition(Integer.MAX_VALUE);
                break;
            case HTextEvent.CARET_PREV_LINE:
            case HTextEvent.CARET_PREV_PAGE:
                setCaretCharPosition(0);
                break;
            case HTextEvent.TEXT_END_CHANGE:
                setEditMode(false);
                break;
            case HTextEvent.TEXT_START_CHANGE:
                setEditMode(true);
                break;
            default:
                break;
        }
    }

    /**
     * Process an {@link org.havi.ui.event.HKeyEvent} sent to this
     * {@link org.havi.ui.HSinglelineEntry}.
     * 
     * @param evt
     *            the {@link org.havi.ui.event.HKeyEvent} to process.
     */
    public void processHKeyEvent(org.havi.ui.event.HKeyEvent evt)
    {
        int keyCode = evt.getKeyCode();

        if (editMode == true)
        {
            if (evt.getID() == KeyEvent.KEY_PRESSED)
            {
                switch (keyCode)
                {
                    case KeyEvent.VK_TAB:
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_ESCAPE:
                        // ignored keys, that aren't CHAR_UNDEFINED
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                        deletePreviousChar();
                        break;
                    case KeyEvent.VK_DELETE:
                        deleteNextChar();
                        break;
                    case KeyEvent.VK_END:
                    case KeyEvent.VK_PAGE_DOWN:
                        setCaretCharPosition(Integer.MAX_VALUE);
                        break;
                    case KeyEvent.VK_HOME:
                    case KeyEvent.VK_PAGE_UP:
                        setCaretCharPosition(0);
                        break;
                    default:
                        // do nothing
                }
            }
            else if (evt.getID() == KeyEvent.KEY_TYPED)
            {
                char ch = evt.getKeyChar();

                // ch != KeyEvent.CHAR_UNDEFINED
                if (ch != HaviToolkit.getCharUndefined()) insertChar(ch);
            }
            else
            {
                // do nothing
            }

            // Should this always be done? Even if not in edit mode?
            notifyListeners(evt);
        }
    }

    /**
     * Common constructor initialization.
     */
    protected synchronized void iniz()
    {
        enableEvents(AWTEvent.ACTION_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK | AWTEvent.KEY_EVENT_MASK
                | AWTEvent.MOUSE_EVENT_MASK);
    }

    /**
     * Notify <code>HKeyListener</code>s about the given <code>HKeyEvent</code>.
     * 
     * Since only {@link java.awt.event.KeyEvent#KEY_PRESSED} events events
     * should be generated (according to the <code>HKeyEvent</code>
     * documentation, only {@link HKeyListener#keyPressed} will be called.
     * 
     * @param e
     *            the <code>HKeyEvent</code>
     */
    protected void notifyListeners(HKeyEvent e)
    {
        if (keyListeners != null)
        {
            switch (e.getID())
            {
                case HKeyEvent.KEY_PRESSED:
                    keyListeners.keyPressed(e);
                    break;
                default:
                    // Don't bother with any others
                    return;
            }
        }
    }

    /**
     * Using a port-specific audio beep, notify the user of an invalid
     * operation.
     * 
     * @see org.cablelabs.impl.havi.HaviToolkit#beep()
     */
    private void beep()
    {
        HaviToolkit.getToolkit().beep();
    }

    /**
     * Private <i>StringBuffer-</i> or <code>char</code> <code>Vector-</code>
     * like class.
     */
    private class Text
    {
        private static final int INITIAL_CAPACITY = 10;

        private static final int INCREMENT = 20;

        private char[] array = new char[INITIAL_CAPACITY];

        private int size;

        /**
         * Returns this <code>Text</code> object as a <code>String</code>.
         * 
         * @return this <code>Text</code> object as a <code>String</code>
         */
        public String toString()
        {
            int s;
            char[] a;
            synchronized (this)
            {
                s = size;
                if (s == 0) return "";
                a = array;
            }

            return new String(a, 0, s);
        }

        /**
         * Sets this <code>Text</code> to be equivalent to the given
         * <code>String</code>.
         * 
         * @param str
         */
        public synchronized void setText(String str)
        {
            size = str.length();
            if (size > array.length) array = new char[size];
            str.getChars(0, size, array, 0);
        }

        /**
         * Inserts the given <code>char</code> at the given position.
         * 
         * @param c
         *            character to insert
         * @param i
         *            position
         */
        public synchronized void insert(char c, int i)
        {
            if (i >= size + 1) throw new ArrayIndexOutOfBoundsException(i + " > " + size);

            // ensure capacity >= size+1
            int capacity = array.length;
            if (size + 1 > capacity)
            {
                char[] old = array;

                capacity = Math.max(size + 1, capacity + INCREMENT);
                array = new char[capacity];
                System.arraycopy(old, 0, array, 0, size);
            }

            // Move following data
            System.arraycopy(array, i, array, i + 1, size - i);

            // Insert char
            array[i] = c;
            ++size;
        }

        /**
         * Removes the <code>char</code> at the given position.
         * 
         * @param i
         *            position
         */
        public synchronized void remove(int i)
        {
            if (i >= size)
                throw new ArrayIndexOutOfBoundsException(i + " >= " + size);
            else if (i < 0) throw new ArrayIndexOutOfBoundsException(i + " < " + 0);

            int j = size - i - 1;
            if (j > 0) System.arraycopy(array, i + 1, array, i, j);
            size--;
        }

        /**
         * Removes all content.
         */
        public synchronized void clear()
        {
            size = 0;
        }

        /**
         * Returns the size of this <code>Text</code>.
         */
        public synchronized int getSize()
        {
            return size;
        }

    }

    /**
     * The set of listeners. Note that the API currently does not have a way to
     * add HFocusListeners. This will be added in version 1.1.
     */
    private HFocusListener listeners;

    /**
     * This defines what type of input is allowed.
     */
    private int type = HKeyboardInputPreferred.INPUT_ANY;

    /*
     * The position of the caret within the text.
     */
    private int caretPos = 0;

    /*
     * A specialized Vector which holds the text content.
     */
    private Text content = new Text();

    /**
     * A flag which is used to specify whether the text content should be
     * treated as <code>null</code> or <code>""</code>.
     */
    private boolean isTextNull = true;

    /*
     * A specialized KeySet which holds the allowed characters when the type of
     * input is INPUT_CUSTOMIZED.
     */
    private KeySet customInputChars = new KeySet();

    /*
     * If this character has a value, it is displayed for all characters.
     */
    private char echoChar = '\0';

    /*
     * Listeners of text events.
     */
    private HTextListener textListeners = null;

    /*
     * Listeners of key events.
     */
    private HKeyListener keyListeners = null;

    /**
     * The default maximum characters per line
     */
    private int maxChars = 16;

    /**
     * Hashtable that maps key values to HNavigable movements.
     */
    private KeySet moves = new KeySet();

    /**
     * This flag is used to determine if we're in edit mode.
     */
    private boolean editMode = false;

    /**
     * This flag is used to determine if the virtual keyboard is displayed.
     */
    private boolean virtualKeyboardShowing = false;

    /**
     * The sound played when this component gains focus.
     */
    private HSound gainFocusSound = null;

    /**
     * The sound played when this component loses focus.
     */
    private HSound loseFocusSound = null;

    /**
     * The property which specifies the platform-specific default look for this
     * class. Can be retreived with HaviToolkit.getProperty().
     */
    private static final String PROPERTY_LOOK = "org.havi.ui.HSinglelineEntry.defaultLook";

    /**
     * The type of <code>HLook</code> to use as the default if not explicitly
     * overridden.
     */
    private static final Class DEFAULT_LOOK = HSinglelineEntryLook.class;
}
