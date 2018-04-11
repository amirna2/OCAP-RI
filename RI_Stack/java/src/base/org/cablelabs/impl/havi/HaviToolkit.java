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

package org.cablelabs.impl.havi;

import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import org.havi.ui.HAdjustmentInputPreferred;
import org.havi.ui.HBackgroundImage;
import org.havi.ui.HDefaultTextLayoutManager;
import org.havi.ui.HKeyboardInputPreferred;
import org.havi.ui.HLook;
import org.havi.ui.HMatte;
import org.havi.ui.HMultilineEntry;
import org.havi.ui.HNavigationInputPreferred;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenDimension;
import org.havi.ui.HScreenPoint;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HSelectionInputPreferred;
import org.havi.ui.HState;
import org.havi.ui.HTextLayoutManager;
import org.havi.ui.HTextValue;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVisible;
import org.havi.ui.event.HActionEvent;
import org.havi.ui.event.HAdjustmentEvent;
import org.havi.ui.event.HFocusEvent;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HKeyEvent;
import org.havi.ui.event.HTextEvent;

/**
 * The <code>HaviToolkit</code> class is the abstract superclass of all actual
 * implementations of the CableLabs HAVi Level2 UI Toolkit. Subclasses of
 * <code>HaviToolkit</code> are used to bind the various components to a
 * particular implementation or port.
 * 
 * <p>
 * 
 * Most applications should not call any of the methods in this class directly.
 * The methods defined by <code>HaviToolkit</code> are the "glue" that joins the
 * platform-independent classes in the {@link org.havi.ui} and
 * {@link org.havi.ui.event} packages with their port-specific support classes.
 * 
 * <p>
 * 
 * For example, the toolkit is used to translate <code>java.awt</code> events to
 * HAVi events as is appropriate for a given platform. This allows the
 * implementation of the component classes (
 * {@link org.havi.ui.HComponent#processEvent(AWTEvent) HComponent} in
 * particular) to be coded independent of any knowlege of how events are
 * generated on a given platform. The relevant methods are:
 * 
 * <ul>
 * <li> {@link #makeKeyEvent(AWTEvent)}
 * <li> {@link #makeFocusEvent(AWTEvent)}
 * <li> {@link #makeActionEvent(AWTEvent)}
 * <li> {@link #makeAdjustmentEvent(AWTEvent)}
 * <li> {@link #makeItemEvent(AWTEvent)}
 * <li> {@link #makeTextEvent(AWTEvent)}
 * </ul>
 * 
 * <p>
 * 
 * <i>Other explanations go here...</i>
 * 
 * @author Aaron Kamienski
 * @version $Id: HaviToolkit.java,v 1.3 2002/11/07 21:13:41 aaronk Exp $
 */
public abstract class HaviToolkit
{
    /**
     * Not publicly instantiable.
     * 
     * @see #getToolkit()
     */
    protected HaviToolkit()
    {
        // Not publicly instantiable
    }

    /**
     * Gets the current toolkit.
     * <p>
     * If there is a system {@link #getProperty(String) property} named
     * <code>"cablelabs.havi.toolkit"</code>, that property is treated as the
     * name of a class that is a subclass of <code>HaviToolkit</code>.
     * <p>
     * If the system property does not exist, then a runtime exception is
     * thrown.
     * 
     * @return the current toolkit
     */
    public static synchronized HaviToolkit getToolkit()
    {
        if (toolkit == null)
        {
            String className;
            if ((className = setup.getProperty(TOOLKIT_PROPERTY)) == null)
                throw new RuntimeException("Property '" + TOOLKIT_PROPERTY + "' not defined.");

            String error = "instantiated";
            try
            {
                Class toolkitClass = Class.forName(className);
                Method factory = toolkitClass.getMethod("getToolkit", new Class[0]);
                toolkit = (HaviToolkit) factory.invoke(null, new Object[0]);
            }
            catch (ClassNotFoundException ignored)
            {
                error = "found";
                ignored.printStackTrace();
            }
            catch (IllegalAccessException ignored)
            {
                ignored.printStackTrace();
            }
            catch (NoSuchMethodException ignored)
            {
                ignored.printStackTrace();
            }
            catch (InvocationTargetException ignored)
            {
                ignored.printStackTrace();
            }

            if (toolkit == null) throw new RuntimeException("Toolkit class '" + className + "' could not be " + error);
        }
        return toolkit;
    }

    /**
     * Generates the event mask which should enable events for the given
     * <code>Component</code> such that appropriate native (i.e., AWT) events
     * can be dispatched to the component for proper translation.
     * <p>
     * The default implementation enables the AWT events that the it intends to
     * translate into <code>HKeyEvents</code> and <code>HFocusEvents</code> (if
     * the proper interfaces are implemented by the component). These are:
     * <ul>
     * <li> {@link AWTEvent#KEY_EVENT_MASK}
     * <li> {@link AWTEvent#MOUSE_EVENT_MASK}
     * <li> {@link AWTEvent#FOCUS_EVENT_MASK}
     * </ul>
     * In general, this is all that will ever be necessary, so subclasses may
     * not need to override.
     * 
     * @param c
     *            the component to enable events on
     * @return the <code>AWTEvent</code> mask specifying the events to enable;
     *         if none should be enabled then <code>0</code> is returned
     */
    public int getEventMask(Component c)
    {
        int mask = 0;

        if (c instanceof HKeyboardInputPreferred) mask |= AWTEvent.KEY_EVENT_MASK;
        if (c instanceof HNavigationInputPreferred)
        {
            mask |= AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.FOCUS_EVENT_MASK;
        }
        return mask;
    }

    /**
     * Attempt to translate the given <code>AWTEvent</code> into a an
     * <code>HKeyEvent</code>, as is appropriate for the given platform. The
     * returned <code>HKeyEvent</code> is intended to processed by a
     * {@link org.havi.ui.HComponent component} implementing the
     * {@link org.havi.ui.HKeyboardInputPreferred} interface.
     * <p>
     * Note that it is up to the implementation to ensure that HAVi events are
     * not translated to HAVi events (which may lead to <i>very</i> undesirable
     * behavior).
     * <p>
     * A basic implementation would simply translate all
     * {@link java.awt.event.KeyEvent#KEY_PRESSED} events to
     * <code>HKeyEvents</code>.
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     * 
     * @see org.havi.ui.HComponent#processEvent(AWTEvent)
     */
    public HKeyEvent makeKeyEvent(AWTEvent evt)
    {
        if (evt instanceof KeyEvent && !(evt instanceof HKeyEvent) && evt.getID() == KeyEvent.KEY_PRESSED &&
        // If NOT in edit mode, no key events...
                // Not sure if it belongs here,
                // or if it would be better off in HComponent.processEvent...
                isEditable(evt.getSource()))
        {
            KeyEvent e = (KeyEvent) evt;
            return new HKeyEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiers(), e.getKeyCode(),
                    e.getKeyChar());
        }
        return null;
    }

    /**
     * Attempt to translate the given <code>AWTEvent</code> into a an
     * <code>HFocusEvent</code>, as is appropriate for the given platform. The
     * returned <code>HFocusEvent</code> is intended to processed by a
     * {@link org.havi.ui.HComponent component} implementing the
     * {@link org.havi.ui.HNavigationInputPreferred} interface.
     * <p>
     * Note that it is up to the implementation to ensure that HAVi events are
     * not translated to HAVi events (which may lead to <i>very</i> undesirable
     * behavior).
     * <p>
     * A basic implementation would simply translate all
     * {@link java.awt.event.FocusEvent#FOCUS_GAINED} and
     * {@link java.awt.event.FocusEvent#FOCUS_LOST} events to appropriate
     * <code>HFocusEvents</code>.
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     * 
     * @see org.havi.ui.HComponent#processEvent(AWTEvent)
     */
    public HFocusEvent makeFocusEvent(AWTEvent evt)
    {
        if (evt instanceof FocusEvent && !(evt instanceof HFocusEvent))
        {
            return new HFocusEvent((Component) evt.getSource(), evt.getID());
        }
        // SA: the following if used to contain an expression excluding
        // HKeyEvent's from
        // generating HFocusEvent's -- removed it to fix bug 583
        else if (evt instanceof KeyEvent && evt.getID() == KeyEvent.KEY_PRESSED &&
        // If in edit mode, no transfer events...
                // Not sure if it belongs here,
                // or if it would be better off in HComponent.processEvent...
                !isEditable(evt.getSource()))
        {
            return new HFocusEvent((Component) evt.getSource(), HFocusEvent.FOCUS_TRANSFER,
                    ((KeyEvent) evt).getKeyCode());
        }
        return null;
    }

    /**
     * Returns whether the given component object is editable or not. This
     * depends on the type of the component:
     * 
     * <table border>
     * <tr>
     * <th>if instanceof of</th>
     * <th>check</th>
     * </tr>
     * <tr>
     * <td> <code>HKeyboardInputPreferred</code></td>
     * <td> <code> getEditMode() == true </code></td>
     * </tr>
     * <tr>
     * <td> <code>HSelectionInputPreferred</code></td>
     * <td> <code> getSelectionMode() == true </code></td>
     * </tr>
     * <tr>
     * <td> <code>HAdjustmentInputPreferred</code></td>
     * <td> <code> getAdjustMode() == true </code></td>
     * </tr>
     * </table>
     * 
     * @return <code>true</code> if the given component is editable
     */
    protected boolean isEditable(Object o)
    {
        if (o instanceof HKeyboardInputPreferred)
            return ((HKeyboardInputPreferred) o).getEditMode();
        else if (o instanceof HSelectionInputPreferred)
            return ((HSelectionInputPreferred) o).getSelectionMode();
        else if (o instanceof HAdjustmentInputPreferred)
            return ((HAdjustmentInputPreferred) o).getAdjustMode();
        else
            return false;
    }

    /**
     * Attempt to translate the given <code>AWTEvent</code> into a an
     * <code>HActionEvent</code>, as is appropriate for the given platform. The
     * returned <code>HActionEvent</code> is intended to processed by a
     * {@link org.havi.ui.HComponent component} implementing the
     * {@link org.havi.ui.HActionInputPreferred} interface.
     * <p>
     * Note that it is up to the implementation to ensure that HAVi events are
     * not translated to HAVi events (which may lead to <i>very</i> undesirable
     * behavior).
     * <p>
     * A simple implementation would simply translate all
     * {@link java.awt.event.KeyEvent#VK_ENTER} events to appropriate
     * <code>HActionEvents</code>.
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     * 
     * @see org.havi.ui.HComponent#processEvent(AWTEvent)
     */
    public abstract HActionEvent makeActionEvent(AWTEvent evt);

    /**
     * Attempt to translate the given <code>AWTEvent</code> into a an
     * <code>HAdjustmentEvent</code>, as is appropriate for the given platform.
     * The returned <code>HAdjustmentEvent</code> is intended to processed by a
     * {@link org.havi.ui.HComponent component} implementing the
     * {@link org.havi.ui.HAdjustmentInputPreferred} interface.
     * <p>
     * Note that it is up to the implementation to ensure that HAVi events are
     * not translated to HAVi events (which may lead to <i>very</i> undesirable
     * behavior).
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     * 
     * @see org.havi.ui.HComponent#processEvent(AWTEvent)
     */
    public abstract HAdjustmentEvent makeAdjustmentEvent(AWTEvent evt);

    /**
     * Attempt to translate the given <code>AWTEvent</code> into a an
     * <code>HItemEvent</code>, as is appropriate for the given platform. The
     * returned <code>HItemEvent</code> is intended to processed by a
     * {@link org.havi.ui.HComponent component} implementing the
     * {@link org.havi.ui.HSelectionInputPreferred} interface.
     * <p>
     * Note that it is up to the implementation to ensure that HAVi events are
     * not translated to HAVi events (which may lead to <i>very</i> undesirable
     * behavior).
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     * 
     * @see org.havi.ui.HComponent#processEvent(AWTEvent)
     */
    public abstract HItemEvent makeItemEvent(AWTEvent evt);

    /**
     * Attempt to translate the given <code>AWTEvent</code> into a an
     * <code>HTextEvent</code>, as is appropriate for the given platform. The
     * returned <code>HTextEvent</code> is intended to processed by a
     * {@link org.havi.ui.HComponent component} implementing the
     * {@link org.havi.ui.HKeyboardInputPreferred} interface.
     * <p>
     * Note that it is up to the implementation to ensure that HAVi events are
     * not translated to HAVi events (which may lead to <i>very</i> undesirable
     * behavior).
     * 
     * @param evt
     *            the input <code>AWTEvent</code> which may be translated to a
     *            HAVi event.
     * @return the HAVi event if one could be generated from the given
     *         <code>AWTEvent</code>; <code>null</code> otherwise
     * 
     * @see org.havi.ui.HComponent#processEvent(AWTEvent)
     */
    public abstract HTextEvent makeTextEvent(AWTEvent evt);

    /**
     * Returns an instance of the default font to be used by components in the
     * system.
     * 
     * @return the default <code>Font</code>
     */
    public abstract Font getDefaultFont();

    /**
     * Draws a component border in a port-specific manner. See the following
     * table for a description of what the default implementation does. This
     * method should be overridden if different behavior is desired.
     * 
     * <table border>
     * <tr>
     * <th>State</th>
     * <th>Border</th>
     * </tr>
     * <tr>
     * <td>FOCUS_STATE</td>
     * <td>border in yellow</td>
     * </tr>
     * <tr>
     * <td>ACTIONED_FOCUSED_STATE</td>
     * <td>border in foreground color</td>
     * </tr>
     * <tr>
     * <td>ACTIONED_STATE</td>
     * <td>border in foreground color, xored against the background color</td>
     * </tr>
     * <tr>
     * <td>all other stats</td>
     * <td>nothing</td>
     * </table>
     * Note that the pixel border is 2 pixels.
     * 
     * @param g
     *            the current graphics context
     * @param visible
     *            the <code>HVisible</code> component being drawn
     * @param state
     *            the current {@link org.havi.ui.HState}-defined state for
     *            <code>visible</code>
     * @param insets
     *            the insets for the component
     * 
     * @see org.havi.ui.HLook
     */
    public void drawBorder(Graphics g, HVisible visible, int state, Insets insets)
    {
        Color color;
        switch (state)
        {
            default:
                return;
            case HState.FOCUSED_STATE:
                color = Color.yellow;
                break;
            case HState.ACTIONED_STATE:
                g.setXORMode(visible.getBackground());
            case HState.ACTIONED_FOCUSED_STATE:
                color = visible.getForeground();
                break;
        }

        // Draw the border in the desired color
        java.awt.Rectangle r = visible.getBounds();
        g.setColor(color);
        g.drawRect(0, 0, r.width - 1, r.height - 1);
        g.drawRect(1, 1, r.width - 3, r.height - 3);

        // Restore if set
        g.setPaintMode();
    }

    /**
     * Returns the platform-specific minimum size for the given component. This
     * size is dependent upon the type of component (if anything at all); it
     * should not inspect the content of the component.
     * <p>
     * The default implementation is to return a new <code>Dimension</code>
     * object specifying a <code>with</code> and <code>height</code> of 4. If
     * the component is a text entry component, then the implementation-specific
     * minimum size is a size suitable for holding some content should it be
     * entered.
     * 
     * @param hvisible
     *            the <code>HVisible</code> component to determine a minimu size
     *            for
     * @return the platform-specific minimum size; the default implementation
     *         always returns <code>new Dimension(2,2)</code>
     * 
     * @see org.havi.ui.HLook#getMinimumSize(HVisible)
     * @see SizingHelper#getMinimumSize(HVisible,HLook,SizingHelper.Strategy)
     */
    public Dimension getMinimumSize(HVisible hvisible)
    {
        int width = 4, height = 4;
        if (hvisible instanceof HTextValue)
        {
            Font f = hvisible.getFont();
            if (f == null) f = HaviToolkit.getToolkit().getDefaultFont();
            java.awt.FontMetrics metrics = hvisible.getFontMetrics(f);
            int padding = metrics.getMaxDescent() * 2;

            // 8 characters in width (completely arbitrary)
            width = (metrics.getMaxAdvance() * 8) + padding;
            // 2 lines if multi (arbitrary), 1 line if single
            height = (((hvisible instanceof HMultilineEntry) ? 2 : 1) * TextSupport.getFontHeight(metrics)) + padding;
        }

        return new Dimension(width, height);
    }

    /**
     * Returns the <code>HTextLayoutManager</code> that should be used with a
     * newly instantiated {@link org.havi.ui.HVisible}. This may either be a
     * <i>singleton</i> object or a fresh instance.
     * <p>
     * This simple implementation simply returns a new instance of an
     * {@link org.havi.ui.HDefaultTextLayoutManager}. Subclasses are free to
     * implement this as appropriate.
     * 
     * @see org.havi.ui.HVisible#getTextLayoutManager()
     */
    public HTextLayoutManager getTextLayoutManager()
    {
        return new HDefaultTextLayoutManager();
    }

    /**
     * Determines whether the given <code>matte</code> is supported by this
     * platform. The default implementation returns <code>false</code>.
     * 
     * @param matte
     *            the <code>HMatte</code> to test
     * @return <code>true</code> if the matte is supported; <code>false</code>
     *         otherwise
     * 
     * @see org.havi.ui.HMatteLayer#setMatte(HMatte)
     * @see org.havi.ui.HComponent#setMatte(HMatte)
     * @see org.havi.ui.HContainer#setMatte(HMatte)
     */
    public boolean isMatteSupported(HMatte matte)
    {
        return false;
    }

    /**
     * Creates and returns the platform-specific matte compositor that is to be
     * used to provide matte support. The default implementation returns
     * <code>null</code>.
     * 
     * @param the
     *            component that this compositor is to be used with
     * @return the <code>MatteCompositor</code> to be used or <code>null</code>
     *         if no compositor is available
     * 
     * @see org.havi.ui.HComponent#setMatte(HMatte)
     * @see org.havi.ui.HContainer#setMatte(HMatte)
     */
    public MatteCompositor getMatteCompositor(Component component)
    {
        return null;
    }

    /**
     * Returns the default setting for number of characters per line in a
     * {@link org.havi.ui.HMultilineEntry} component. This is used by the
     * default implementation of {@link org.havi.ui.HMultilineEntryLook} to
     * determine the preferred size of the component.
     * <p>
     * This could be a function of the current <code>Font</code> and/or the
     * graphics configuration.
     * 
     * @return the default setting for number of characters per line
     */
    public int getMaxCharsPerLine(HVisible hvisible)
    {
        return 30;
    }

    /**
     * Create a platform and/or media specific background image object. If the
     * object cannot be created, then null is returned.
     * 
     * @param filename
     *            is a filename that specifies the location of the background
     *            image.
     * @return an {@link HBackgroundImage} object capable of loading the
     *         specified image on the current platform. Returns null if the
     *         object cannot be created.
     */
    public abstract HBackgroundImage createBackgroundImage(HBackgroundImage hbi, String filename);

    /**
     * Create a platform and/or media specific background image object. If the
     * object cannot be created, then null is returned.
     * 
     * @param imageURL
     *            is a URL that specifies the location of the background image.
     * @return an {@link HBackgroundImage} object capable of loading the
     *         specified image on the current platform. Returns null if the
     *         object cannot be created.
     */
    public abstract HBackgroundImage createBackgroundImage(HBackgroundImage hbi, URL imageURL);

    /**
     * Create a platform and/or media specific background image object. If the
     * object cannot be created, then null is returned.
     * 
     * @param pixels
     *            is the data for the background image.
     * @return an {@link HBackgroundImage} object capable of loading the
     *         specified image on the current platform. Returns null if the
     *         object cannot be created.
     */
    public abstract HBackgroundImage createBackgroundImage(HBackgroundImage hbi, byte[] pixels);

    /**
     * Creates the (potentially) platform-specific instance of the
     * <code>HSceneFactory</code>. It is up to the implementation to ensure a
     * singleton is returned.
     * 
     * @return the <code>HSceneFactory</code> instance to use
     */
    public abstract HSceneFactory getHSceneFactory();

    /**
     * Returns the platform-specific capabilities support singleton. It is up to
     * the implementation to ensure a singleton is returned.
     * <p>
     * The <code>CapabilitiesSupport</code> object is used to query information
     * about what capabilities are provided by the port with respect to
     * keyboard, mouse, remote control, and fonts.
     * 
     * @see org.havi.ui.HFontCapabilities
     * @see org.havi.ui.event.HMouseCapabilities
     * @see org.havi.ui.event.HKeyCapabilities
     * @see org.havi.ui.event.HRcCapabilities
     * @see org.havi.ui.event.HEventRepresentation
     */
    public abstract CapabilitiesSupport getCapabilities();

    /**
     * Determines if the given <code>Color</code> is opaque or at least
     * partially transparent.
     * 
     * @param c
     * @return <code>true</code> if the given <code>Color</code> is completely
     *         opaque
     * 
     * @see org.havi.ui.HLook#isOpaque(HVisible)
     */
    public abstract boolean isOpaque(Color c);

    /**
     * Determines if the given <code>Image</code> is opaque or at least
     * partially transparent.
     * 
     * @param img
     * @return <code>true</code> if the given <code>Image</code> is completely
     *         opaque
     * 
     * @see org.havi.ui.HScene#isOpaque()
     */
    public abstract boolean isOpaque(Image img);

    /**
     * Return the NOT_CONTRIBUTING video configuration for the platform. The
     * default implementation returns null.
     * 
     * @see org.havi.ui.HVideoDevice#NOT_CONTRIBUTING
     */
    public HVideoConfiguration getVideoNotContributing()
    {
        return null;
    }

    /**
     * Returns all {@link org.havi.ui.HScreen HScreens} in this system.
     * 
     * @return an array of <code>HScreens</code> representing all screens in
     *         this system.
     */
    public abstract HScreen[] getHScreens();

    /**
     * Returns the default {@link org.havi.ui.HScreen HScreen} for this
     * application. For systems where an application is associated with audio or
     * video which is started before the application starts, this method will
     * return the <code>HScreen</code> where that associated audio / video is
     * being output.
     * 
     * @return the default <code>HScreen</code> for this application.
     */
    public abstract HScreen getDefaultHScreen();

    /**
     * Show the virtual keyboard and feed all user input to the specified edit
     * component. While the virtual keyboard is displayed it is the only
     * component that the user can interact with.
     * 
     * @param editComponent
     *            the component to receive simulated key events.
     * @return true if a virtual keyboard is displayed (supported by the
     *         platform), false otherwise.
     */
    public abstract boolean showVirtualKeyboard(HTextValue editComponent);

    /**
     * Retrieves a container that is always above all <code>HScenes</code> that
     * may be displayed below it. This isn't currently used within the HAVi
     * implementation directly, but may be used outside the HAVi implementation
     * for various purposes. Including, but not limited to:
     * <ul>
     * <li>Display of system splash screens
     * <li>Display of Emergency Alert Messages.
     * <li>Display of system dialogs
     * </ul>
     * <p>
     * If no pane is supported, then <code>null</code> will be returned. Every
     * call should return the same instance of <code>Container</code>.
     * <p>
     * The size of the retrieved container should not be modified. It will
     * automatically be adjusted to cover the maximum area of the screen allowed
     * by the implementation. The caller can make use of a
     * <code>ComponentListener</code> in order to be notified of changes in
     * size.
     * <p>
     * It is expected that only one <i>caller</i> access this container at a
     * time; otherwise the results are undefined.
     * <p>
     * It is expected that the <i>caller</i> limit its interaction with the
     * retrieved <code>Container</code> to the following methods:
     * <ul>
     * <li> {@link Container#setVisible}
     * <li> {@link Container#add}
     * <li> {@link Container#remove}
     * <li> {@link Container#setLayout}
     * <li> {@link Container#repaint}
     * <li> {@link Container#addComponentListener} (for being notified of size
     * changes)
     * <li> {@link Container#removeComponentListener}
     * </ul>
     * 
     * @return the <code>Container</code> a default window pane suitable for
     *         non-HAVi use; or <code>null</code> if no such
     *         <code>Container</code> is supported
     */
    public java.awt.Container getDefaultWindowPane()
    {
        return null;
    }

    /**
     * If supported, the visible state of application graphics is adjusted based
     * upon the given <i>visible</i> parameter. If not supported, then this
     * method does nothing and graphics are always visible.
     * <p>
     * By default, this method does nothing.
     * 
     * @param visible
     *            new visible state of application graphics
     */
    public void setGraphicsVisible(boolean visible)
    {
        // Default implementation does nothing
    }

    /**
     * Plays an audio beep. Generally used to notify the user of some invalid
     * input or some other such error. The default implementation simply uses
     * {@link java.awt.Toolkit#beep() Toolkit.beep()}. Individual ports may wish
     * to override this, perhaps even implement it as a <i>no-op</i>.
     */
    public void beep()
    {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Return whether the specified {@link HScreenRectangle}s are equal.
     * 
     * @param r1
     * @param r2
     * @return <pre>
     * (r1.x == r2.x) &amp;&amp; (r1.y == r2.y) &amp;&amp; (r1.width == r2.width) &amp;&amp; (r1.height == r2.height)
     * </pre>
     */
    public boolean isEqual(HScreenRectangle r1, HScreenRectangle r2)
    {
        return ((r1.x == r2.x) && (r1.y == r2.y) && (r1.width == r2.width) && (r1.height == r2.height));
    }

    /**
     * Return whether the specified {@link HScreenPoint}s are equal.
     * 
     * @param p1
     * @param p2
     * @return <pre>
     * (p1.x == p2.x) &amp;&amp; (p1.y == p2.y)
     * </pre>
     */
    public boolean isEqual(HScreenPoint p1, HScreenPoint p2)
    {
        return ((p1.x == p2.x) && (p1.y == p2.y));
    }

    /**
     * Return whether the specified {@link HScreenDimension}s are equal.
     * 
     * @param d1
     * @param d2
     * @return <pre>
     * (d1.width == d2.width) &amp;&amp; (d1.height == d2.height)
     * </pre>
     */
    public boolean isEqual(HScreenDimension d1, HScreenDimension d2)
    {
        return ((d1.width == d2.width) && (d1.height == d2.height));
    }

    /**
     * Returns the property value for the given property key.
     * 
     * @param key
     *            the property key
     * @return the property value for the given property key
     */
    public String getProperty(String key)
    {
        String value = MPEEnv.getSystemProperty(key);
        return (value != null) ? value : setup.getProperty(key);
    }

    /**
     * Returns the property value for the given property key; if not set then
     * the given default value is returned.
     * 
     * @param key
     *            the property key
     * @param defValue
     *            the property value default
     * @return the property value for the given property key; if not set then
     *         the given default value is returned.
     */
    public String getProperty(String key, String defValue)
    {
        String value = MPEEnv.getSystemProperty(key);
        return (value != null) ? value : setup.getProperty(key, defValue);
    }

    /**
     * Flushes any buffered graphics operations performed directly to the
     * screen. This is equivalent to {@link java.awt.Toolkit#sync()} by default.
     */
    public void flush()
    {
        java.awt.Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Return the value that should be used for
     * <code>KeyEvent.CHAR_UNDEFINED</code>. This may be different than the
     * actual value for {@link KeyEvent#CHAR_UNDEFINED} because of differences
     * between Java 1 and Java 2 (where it moved from 0 to 65535).
     * <p>
     * The default version of this used the runtime value (not the compile-time
     * value).
     * 
     * @return the value that should be used for <code>CHAR_UNDEFINED</code>
     * 
     * @see #getCharUndefined()
     */
    public char getCharUndefinedValue()
    {
        return KeyEventConstant.CHAR_UNDEFINED;
    }

    /**
     * Invokes {@link #getCharUndefinedValue()} on the current
     * <code>HaviToolkit</code>.
     */
    public static char getCharUndefined()
    {
        return getToolkit().getCharUndefinedValue();
    }

    /**
     * Returns the default Insets for all HLook instances
     * 
     * @return default HLook Insets
     */
    public Insets getDefaultHLookInsets()
    {
        return defaultHLookInsets;
    }

    /**
     * Returns the default Insets for all HLook instances
     * 
     * @return default HLook Insets
     */
    public Insets getDefaultHListGroupLookElementInsets()
    {
        return defaultHListGroupLookElementInsets;
    }

    /**
     * Returns the caller-specific global data indicated by the given key.
     * 
     * This facility should be used rather than <code>static</code> class
     * variables so that different values can be stored for different
     * applications. An example would be a component class' <i>default look</i>
     * -- changing the default look in one application shouldn't affect another.
     * 
     * @param key
     *            <code>Object</code> used as a key when previously storing
     *            data.
     * @return the data <code>Object</code> previously stored using
     *         {@link #setGlobalData}, or <code>null</code> if no such data is
     *         available.
     * @throws NullPointerException
     *             if <code>key == null</code>.
     */
    public abstract Object getGlobalData(Object key) throws NullPointerException;

    /**
     * Stores caller-specific global data using the given key. The key will be
     * required to request the data at a later time.
     * 
     * This facility should be used rather than <code>static</code> class
     * variables so that different values can be stored for different
     * applications. An example would be a component class' <i>default look</i>
     * -- changing the default look in one application shouldn't affect another.
     * 
     * @param key
     *            <code>Object</code> later to be used to retrieve the stored
     *            data.
     * @param data
     *            <code>Object</code> to be stored or <code>null</code> if any
     *            current data is to be removed.
     * @throws NullPointerException
     *             if <code>key == null</code>.
     */
    public abstract void setGlobalData(Object key, Object data) throws NullPointerException;

    /*
     * Default Insets
     */
    protected Insets defaultHLookInsets = new Insets(3, 3, 3, 3);

    protected Insets defaultHListGroupLookElementInsets = new Insets(6, 6, 6, 6);

    /**
     * The <code>Properties</code> file that describes the current CableLabs
     * HAVi setup.
     */
    protected static final Properties setup;

    /**
     * The property which specifies the setup properties file to use.
     */
    private static String SETUP_PROPERTY = "OCAP.havi.setup";

    /**
     * The property which references the toolkit implementation.
     */
    private static String TOOLKIT_PROPERTY = "cablelabs.havi.toolkit";

    /**
     * The current toolkit.
     */
    private static HaviToolkit toolkit;

    /**
     * Static initialization block.
     */
    static
    {
        // Create our properties and initialize from a file
        setup = new Properties();

        // Check property, else get local file
        String propsFile = MPEEnv.getEnv(SETUP_PROPERTY, "havi.properties");

        // Load properties from file
        java.io.InputStream in = HaviToolkit.class.getResourceAsStream(propsFile);
        if (in != null)
        {
            try
            {
                in = new java.io.BufferedInputStream(in);
                setup.load(in);
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
            finally
            {
                if (in != null) try
                {
                    in.close();
                }
                catch (Exception e)
                {
                }
            }
        }
    }
}

/**
 * Defines {@link #CHAR_UNDEFINED}.
 * <p>
 * Class should only be loaded if actually referenced at runtime.
 * 
 * @author Aaron Kamienski
 */
class KeyEventConstant
{
    /*
     * Since CHAR_UNDEFINED changed between Java 1 and Java 2, HAVi
     * implementation code should avoid referencing it directly. Instead, we'll
     * look it up at runtime and assign it here.
     * 
     * @see java.awt.event.KeyEvent#CHAR_UNDEFINED
     */
    public static final char CHAR_UNDEFINED = getCharUndefined();

    /**
     * Retrieve our own private CHAR_UNDEFINED using the runtime system's
     * definition, rather than the compile-time system's definition. This is
     * necessary because this value changed between Java 1 and Java 2 (from 0 to
     * 65535).
     * 
     * @return runtime value for CHAR_UNDEFINED; fallback to compile-time
     *         definition
     * 
     * @see #getCharUndefined()
     * @see #CHAR_UNDEFINED
     * @see java.awt.event.KeyEvent#CHAR_UNDEFINED
     */
    private static char getCharUndefined()
    {
        try
        {
            Class cl = Class.forName("java.awt.event.KeyEvent");
            java.lang.reflect.Field fl = cl.getField("CHAR_UNDEFINED");;

            return fl.getChar(null);
        }
        catch (Exception e)
        {
            // Fine, use the compiled in version
            return java.awt.event.KeyEvent.CHAR_UNDEFINED;
        }
    }
}
