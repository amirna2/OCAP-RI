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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.EventListener;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.havi.ui.event.HActionListener;
import org.havi.ui.event.HAdjustmentEvent;
import org.havi.ui.event.HAdjustmentListener;
import org.havi.ui.event.HBackgroundImageEvent;
import org.havi.ui.event.HBackgroundImageListener;
import org.havi.ui.event.HFocusListener;
import org.havi.ui.event.HItemEvent;
import org.havi.ui.event.HItemListener;
import org.havi.ui.event.HKeyListener;
import org.havi.ui.event.HScreenConfigurationEvent;
import org.havi.ui.event.HScreenConfigurationListener;
import org.havi.ui.event.HScreenLocationModifiedEvent;
import org.havi.ui.event.HScreenLocationModifiedListener;
import org.havi.ui.event.HTextEvent;
import org.havi.ui.event.HTextListener;

/**
 * The <code>HEventMulticaster</code> class is intended to assist platform or
 * subclass implementers with the handling of HAVi events. Implementations are
 * not required to use this class to dispatch HAVi events. Applications should
 * not extend the HEventMulticaster class and implementations are not required
 * to behave correctly if an application does extend this class. If an extended
 * multicaster is desired, <code>AWTEventMulticaster</code> should be used
 * rather than <code>HEventMulticaster</code>
 * <p>
 * The <code>HEventMulticaster</code> class is intended to handle event
 * dispatching for the following HAVi events:
 * 
 * <ul>
 * <li>{@link org.havi.ui.event.HBackgroundImageEvent HBackgroundImageEvent}
 * <li>{@link org.havi.ui.event.HScreenConfigurationEvent
 * HScreenConfigurationEvent}
 * <li>{@link org.havi.ui.event.HScreenLocationModifiedEvent
 * HScreenLocationModifiedEvent}
 * <li>{@link org.havi.ui.event.HActionEvent HActionEvent}
 * <li>{@link org.havi.ui.event.HFocusEvent HFocusEvent}
 * <li>{@link org.havi.ui.event.HItemEvent HItemEvent}
 * <li>{@link org.havi.ui.event.HTextEvent HTextEvent}
 * <li>{@link org.havi.ui.event.HKeyEvent HKeyEvent}
 * <li>{@link org.havi.ui.event.HAdjustmentEvent HAdjustmentEvent}
 * <li>java.awt.event.WindowEvent
 * <li>org.davic.resources.ResourceStatusEvent
 * </ul>
 * 
 * <p>
 * It is an implementation option for this class to insert other classes in the
 * inheritance tree (for example java.awt.AWTEventMulticaster). It is allowed
 * that this may result in HEventMulticaster inheriting additional methods
 * beyond those specified here. If this class does extend
 * <code>java.awt.AWTEventMulticaster</code>, it is allowed for the fields
 * defined in this class to be inherited from that parent class.
 * </p>
 * 
 * <p>
 * Note: the org.davic.resources.ResourceStatusListener specification does not
 * require EventListener to be present. In a HAVi UI implementation,
 * ResourceStatusListener shall extend EventListener.
 * </p>
 * 
 * <hr>
 * 
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
 * @see org.havi.ui.event.HBackgroundImageListener
 * @see org.havi.ui.event.HScreenConfigurationListener
 * @see org.havi.ui.event.HScreenLocationModifiedListener
 * @see org.havi.ui.event.HActionListener
 * @see org.havi.ui.event.HAdjustmentListener
 * @see org.havi.ui.event.HFocusListener
 * @see org.havi.ui.event.HItemListener
 * @see org.havi.ui.event.HTextListener
 * @see org.havi.ui.event.HKeyListener
 * @see java.awt.event.WindowListener
 * @see org.davic.resources.ResourceStatusListener
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HEventMulticaster implements HBackgroundImageListener, HScreenConfigurationListener,
        HScreenLocationModifiedListener, java.awt.event.WindowListener, HActionListener, HAdjustmentListener,
        HFocusListener, HItemListener, HTextListener, HKeyListener, ResourceStatusListener
{

    /** 
     * 
     */
    protected final EventListener a;

    /** 
     * 
     */
    protected final EventListener b;

    /**
     * Creates an event multicaster instance which chains listener-a with
     * listener-b. The parameters a and b passed to the constructor shall be
     * used to populate the fields a and b of the instance.
     * 
     * @param a
     *            listener-a
     * @param b
     *            listener-b
     */
    protected HEventMulticaster(EventListener a, EventListener b)
    {
        this.a = a;
        this.b = b;
    }

    /**
     * Removes a listener from this multicaster and returns the result.
     * 
     * @param oldl
     *            the listener to be removed
     */
    protected EventListener remove(EventListener oldl)
    {
        if (oldl == a) return b;
        if (oldl == b) return a;
        EventListener a2 = removeInternal(a, oldl);
        EventListener b2 = removeInternal(b, oldl);
        if (a2 == a && b2 == b)
        {
            return this;
        }
        return addInternal(a2, b2);
    }

    /**
     * Returns the resulting multicast listener from adding listener-a and
     * listener-b together. If listener-a is null, it returns listener-b; If
     * listener-b is null, it returns listener-a If neither are null, then it
     * creates and returns a new HEventMulticaster instance which chains a with
     * b.
     * 
     * @param a
     *            event listener-a
     * @param b
     *            event listener-b
     */
    protected static EventListener addInternal(EventListener a, EventListener b)
    {
        if (a == null) return b;
        if (b == null) return a;
        return new HEventMulticaster(a, b);
    }

    /**
     * Returns the resulting multicast listener after removing the old listener
     * from listener-l. If listener-l equals the old listener OR listener-l is
     * null, returns null. Else if listener-l is an instance of
     * HEventMulticaster, then it removes the old listener from it. Else,
     * returns listener l.
     * 
     * @param l
     *            the listener being removed from
     * @param oldl
     *            the listener being removed
     */
    protected static EventListener removeInternal(EventListener l, EventListener oldl)
    {
        if (l == oldl || l == null)
        {
            return null;
        }
        else if (l instanceof HEventMulticaster)
        {
            return ((HEventMulticaster) l).remove(oldl);
        }
        else
        {
            return l;
        }
    }

    /**
     * Adds {@link org.havi.ui.event.HBackgroundImageListener
     * HBackgroundImageListener}-a with
     * {@link org.havi.ui.event.HBackgroundImageListener
     * HBackgroundImageListener}-b and returns the resulting multicast listener.
     * 
     * @param a
     *            HBackgroundImageListener-a
     * @param b
     *            HBackgroundImageListener-b
     */
    public static HBackgroundImageListener add(HBackgroundImageListener a, HBackgroundImageListener b)
    {
        return (HBackgroundImageListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HBackgroundImageListener
     * HBackgroundImageListener} from
     * {@link org.havi.ui.event.HBackgroundImageListener
     * HBackgroundImageListener}-l and returns the resulting multicast listener.
     * 
     * @param l
     *            HBackgroundImageListener-l
     * @param oldl
     *            the HBackgroundImageListener being removed
     */
    public static HBackgroundImageListener remove(HBackgroundImageListener l, HBackgroundImageListener oldl)
    {
        return (HBackgroundImageListener) removeInternal(l, oldl);
    }

    /**
     * Adds WindowListener-a with WindowListener-b and returns the resulting
     * multicast listener.
     * 
     * @param a
     *            WindowListener-a
     * @param b
     *            WindowListener-b
     */
    public static WindowListener add(WindowListener a, WindowListener b)
    {
        return (WindowListener) addInternal(a, b);
    }

    /**
     * Removes the old WindowListener from WindowListener-l and returns the
     * resulting multicast listener.
     * 
     * @param l
     *            WindowListener-l
     * @param oldl
     *            the WindowListener being removed
     */
    public static WindowListener remove(WindowListener l, WindowListener oldl)
    {
        return (WindowListener) removeInternal(l, oldl);
    }

    /**
     * Adds {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener}-a with
     * {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener}-b and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            HScreenConfigurationListener-a
     * @param b
     *            HScreenConfigurationListener-b
     */
    public static HScreenConfigurationListener add(HScreenConfigurationListener a, HScreenConfigurationListener b)
    {
        return (HScreenConfigurationListener) addInternal(a, b);
    }

    /**
     * Adds {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener}-a with
     * {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener}-b, which is notified when the
     * {@link org.havi.ui.HScreenDevice HScreenDevice's} configuration is
     * modified so that it is no longer compatible with the
     * {@link org.havi.ui.HScreenConfigTemplate HScreenConfigTemplate} tb. It
     * returns the resulting multicast listener.
     * 
     * @param a
     *            HScreenConfigurationListener-a
     * @param b
     *            HScreenConfigurationListener-b
     * @param tb
     *            HScreenConfigTemplate associated with
     *            HScreenConfigurationListener-b
     */
    public static HScreenConfigurationListener add(HScreenConfigurationListener a, HScreenConfigurationListener b,
            HScreenConfigTemplate tb)
    {
        // What to do with tb?
        return (HScreenConfigurationListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener} from
     * {@link org.havi.ui.event.HScreenConfigurationListener
     * HScreenConfigurationListener}-l and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            HScreenConfigurationListener-l
     * @param oldl
     *            the HScreenConfigurationListener being removed
     */
    public static HScreenConfigurationListener remove(HScreenConfigurationListener l, HScreenConfigurationListener oldl)
    {
        return (HScreenConfigurationListener) removeInternal(l, oldl);
    }

    /**
     * Adds {@link org.havi.ui.event.HScreenLocationModifiedListener
     * HScreenLocationModifiedListener}-a with
     * {@link org.havi.ui.event.HScreenLocationModifiedListener
     * HScreenLocationModifiedListener}-b and returns the resulting multicast
     * listener.
     * 
     * @param a
     *            HScreenLocationModifiedListener-a
     * @param b
     *            HScreenLocationModifiedListener-b
     */
    public static HScreenLocationModifiedListener add(HScreenLocationModifiedListener a,
            HScreenLocationModifiedListener b)
    {
        return (HScreenLocationModifiedListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HScreenLocationModifiedListener
     * HScreenLocationModifiedListener} from
     * {@link org.havi.ui.event.HScreenLocationModifiedListener
     * HScreenLocationModifiedListener}-l and returns the resulting multicast
     * listener.
     * 
     * @param l
     *            HScreenLocationModifiedListener-l
     * @param oldl
     *            the HScreenLocationModifiedListener being removed
     */
    public static HScreenLocationModifiedListener remove(HScreenLocationModifiedListener l,
            HScreenLocationModifiedListener oldl)
    {
        return (HScreenLocationModifiedListener) removeInternal(l, oldl);
    }

    /**
     * Handles the {@link org.havi.ui.event.HBackgroundImageEvent
     * HBackgroundImageEvent} by invoking the
     * {@link org.havi.ui.event.HBackgroundImageListener#imageLoaded
     * imageLoaded} methods on listener-a and listener-b.
     * 
     * @param e
     *            the HBackgroundImageEvent event
     */
    public void imageLoaded(HBackgroundImageEvent e)
    {
        if (a != null) ((HBackgroundImageListener) a).imageLoaded(e);
        if (b != null) ((HBackgroundImageListener) b).imageLoaded(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HBackgroundImageEvent
     * HBackgroundImageEvent} by invoking the
     * {@link org.havi.ui.event.HBackgroundImageListener#imageLoadFailed
     * imageLoadFailed} methods on listener-a and listener-b.
     * 
     * @param e
     *            the HBackgroundImageEvent event
     */
    public void imageLoadFailed(HBackgroundImageEvent e)
    {
        if (a != null) ((HBackgroundImageListener) a).imageLoadFailed(e);
        if (b != null) ((HBackgroundImageListener) b).imageLoadFailed(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HScreenConfigurationEvent
     * HScreenConfigurationEvent} by invoking the
     * {@link org.havi.ui.event.HScreenConfigurationListener#report report}
     * methods on listener-a and listener-b.
     * 
     * @param e
     *            the HScreenConfigurationEvent event
     */
    public void report(HScreenConfigurationEvent e)
    {
        if (a != null) ((HScreenConfigurationListener) a).report(e);
        if (b != null) ((HScreenConfigurationListener) b).report(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HScreenLocationModifiedEvent
     * HScreenLocationModifiedEvent} by invoking the
     * {@link org.havi.ui.event.HScreenLocationModifiedListener#report report}
     * methods on listener-a and listener-b.
     * 
     * @param e
     *            the HScreenLocationModifiedEvent event
     */
    public void report(HScreenLocationModifiedEvent e)
    {
        if (a != null) ((HScreenLocationModifiedListener) a).report(e);
        if (b != null) ((HScreenLocationModifiedListener) b).report(e);
    }

    /**
     * Handles the windowOpened event by invoking the windowOpened methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the window event
     */
    public void windowOpened(WindowEvent e)
    {
        if (a != null) ((WindowListener) a).windowOpened(e);
        if (b != null) ((WindowListener) b).windowOpened(e);
    }

    /**
     * Handles the windowClosing event by invoking the windowClosing methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the window event
     */
    public void windowClosing(WindowEvent e)
    {
        if (a != null) ((WindowListener) a).windowClosing(e);
        if (b != null) ((WindowListener) b).windowClosing(e);
    }

    /**
     * Handles the windowClosed event by invoking the windowClosed methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the window event
     */
    public void windowClosed(WindowEvent e)
    {
        if (a != null) ((WindowListener) a).windowClosed(e);
        if (b != null) ((WindowListener) b).windowClosed(e);
    }

    /**
     * Handles the windowIconified event by invoking the windowIconified methods
     * on listener-a and listener-b.
     * 
     * @param e
     *            the window event
     */
    public void windowIconified(WindowEvent e)
    {
        if (a != null) ((WindowListener) a).windowIconified(e);
        if (b != null) ((WindowListener) b).windowIconified(e);
    }

    /**
     * Handles the windowDeiconified event by invoking the windowDeiconified
     * methods on listener-a and listener-b.
     * 
     * @param e
     *            the window event
     */
    public void windowDeiconified(WindowEvent e)
    {
        if (a != null) ((WindowListener) a).windowDeiconified(e);
        if (b != null) ((WindowListener) b).windowDeiconified(e);
    }

    /**
     * Handles the windowActivated event by invoking the windowActivated methods
     * on listener-a and listener-b.
     * 
     * @param e
     *            the window event
     */
    public void windowActivated(WindowEvent e)
    {
        if (a != null) ((WindowListener) a).windowActivated(e);
        if (b != null) ((WindowListener) b).windowActivated(e);
    }

    /**
     * Handles the windowDeactivated event by invoking the windowDeactivated
     * methods on listener-a and listener-b.
     * 
     * @param e
     *            the window event
     */
    public void windowDeactivated(WindowEvent e)
    {
        if (a != null) ((WindowListener) a).windowDeactivated(e);
        if (b != null) ((WindowListener) b).windowDeactivated(e);
    }

    /**
     * Handles the ActionEvent by invoking the actionPerformed methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the ActionEvent event
     */
    public void actionPerformed(ActionEvent e)
    {
        if (a != null) ((ActionListener) a).actionPerformed(e);
        if (b != null) ((ActionListener) b).actionPerformed(e);
    }

    /**
     * Handles the FocusEvent by invoking the focusLost methods on listener-a
     * and listener-b.
     * 
     * @param e
     *            the FocusEvent event
     */
    public void focusLost(FocusEvent e)
    {
        if (a != null) ((FocusListener) a).focusLost(e);
        if (b != null) ((FocusListener) b).focusLost(e);
    }

    /**
     * Handles the FocusEvent by invoking the focusGained methods on listener-a
     * and listener-b.
     * 
     * @param e
     *            the FocusEvent event
     */
    public void focusGained(FocusEvent e)
    {
        if (a != null) ((FocusListener) a).focusGained(e);
        if (b != null) ((FocusListener) b).focusGained(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HAdjustmentEvent HAdjustmentEvent}
     * by invoking the
     * {@link org.havi.ui.event.HAdjustmentListener#valueChanged valueChanged}
     * methods on listener-a and listener-b.
     * 
     * @param e
     *            the HAdjustmentEvent event
     */
    public void valueChanged(HAdjustmentEvent e)
    {
        if (a != null) ((HAdjustmentListener) a).valueChanged(e);
        if (b != null) ((HAdjustmentListener) b).valueChanged(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HItemEvent HItemEvent} by invoking
     * the {@link org.havi.ui.event.HItemListener#selectionChanged
     * selectionChanged} methods on listener-a and listener-b.
     * 
     * @param e
     *            the HItemEvent event
     */
    public void selectionChanged(HItemEvent e)
    {
        if (a != null) ((HItemListener) a).selectionChanged(e);
        if (b != null) ((HItemListener) b).selectionChanged(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HItemEvent HItemEvent} by invoking
     * the {@link org.havi.ui.event.HItemListener#currentItemChanged
     * currentItemChanged} methods on listener-a and listener-b.
     * 
     * @param e
     *            the HItemEvent event
     */
    public void currentItemChanged(HItemEvent e)
    {
        if (a != null) ((HItemListener) a).currentItemChanged(e);
        if (b != null) ((HItemListener) b).currentItemChanged(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HTextEvent HTextEvent} by invoking
     * the {@link org.havi.ui.event.HTextListener#textChanged textChanged}
     * methods on listener-a and listener-b.
     * 
     * @param e
     *            the HTextEvent event
     */
    public void textChanged(HTextEvent e)
    {
        if (a != null) ((HTextListener) a).textChanged(e);
        if (b != null) ((HTextListener) b).textChanged(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HTextEvent HTextEvent} by invoking
     * the {@link org.havi.ui.event.HTextListener#caretMoved caretMoved} methods
     * on listener-a and listener-b.
     * 
     * @param e
     *            the HTextEvent event
     */
    public void caretMoved(HTextEvent e)
    {
        if (a != null) ((HTextListener) a).caretMoved(e);
        if (b != null) ((HTextListener) b).caretMoved(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HKeyEvent HKeyEvent} by invoking the
     * {@link org.havi.ui.event.HKeyListener#keyTyped keyTyped} methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the HKeyEvent event
     */
    public void keyTyped(KeyEvent e)
    {
        if (a != null) ((KeyListener) a).keyTyped(e);
        if (b != null) ((KeyListener) b).keyTyped(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HKeyEvent HKeyEvent} by invoking the
     * {@link org.havi.ui.event.HKeyListener#keyPressed keyPressed} methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the HKeyEvent event
     */
    public void keyPressed(KeyEvent e)
    {
        if (a != null) ((KeyListener) a).keyPressed(e);
        if (b != null) ((KeyListener) b).keyPressed(e);
    }

    /**
     * Handles the {@link org.havi.ui.event.HKeyEvent HKeyEvent} by invoking the
     * {@link org.havi.ui.event.HKeyListener#keyReleased keyReleased} methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the HKeyEvent event
     */
    public void keyReleased(KeyEvent e)
    {
        if (a != null) ((KeyListener) a).keyReleased(e);
        if (b != null) ((KeyListener) b).keyReleased(e);
    }

    /**
     * Adds {@link org.havi.ui.event.HTextListener HTextListener}-a with
     * {@link org.havi.ui.event.HTextListener HTextListener}-b and returns the
     * resulting multicast listener.
     * 
     * @param a
     *            HTextListener-a
     * @param b
     *            HTextListener-b
     */
    public static HTextListener add(HTextListener a, HTextListener b)
    {
        return (HTextListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HTextListener HTextListener}
     * from {@link org.havi.ui.event.HTextListener HTextListener}-l and returns
     * the resulting multicast listener.
     * 
     * @param l
     *            HTextListener-l
     * @param oldl
     *            the HTextListener being removed
     */
    public static HTextListener remove(HTextListener l, HTextListener oldl)
    {
        return (HTextListener) removeInternal(l, oldl);
    }

    /**
     * Adds {@link org.havi.ui.event.HItemListener HItemListener}-a with
     * {@link org.havi.ui.event.HItemListener HItemListener}-b and returns the
     * resulting multicast listener.
     * 
     * @param a
     *            HItemListener-a
     * @param b
     *            HItemListener-b
     */
    public static HItemListener add(HItemListener a, HItemListener b)
    {
        return (HItemListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HItemListener HItemListener}
     * from {@link org.havi.ui.event.HItemListener HItemListener}-l and returns
     * the resulting multicast listener.
     * 
     * @param l
     *            HItemListener-l
     * @param oldl
     *            the HItemListener being removed
     */
    public static HItemListener remove(HItemListener l, HItemListener oldl)
    {
        return (HItemListener) removeInternal(l, oldl);
    }

    /**
     * Adds {@link org.havi.ui.event.HFocusListener HFocusListener}-a with
     * {@link org.havi.ui.event.HFocusListener HFocusListener}-b and returns the
     * resulting multicast listener.
     * 
     * @param a
     *            HFocusListener-a
     * @param b
     *            HFocusListener-b
     */
    public static HFocusListener add(HFocusListener a, HFocusListener b)
    {
        return (HFocusListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HFocusListener HFocusListener}
     * from {@link org.havi.ui.event.HFocusListener HFocusListener}-l and
     * returns the resulting multicast listener.
     * 
     * @param l
     *            HFocusListener-l
     * @param oldl
     *            the HFocusListener being removed
     */
    public static HFocusListener remove(HFocusListener l, HFocusListener oldl)
    {
        return (HFocusListener) removeInternal(l, oldl);
    }

    /**
     * Adds {@link org.havi.ui.event.HAdjustmentListener HAdjustmentListener}-a
     * with {@link org.havi.ui.event.HAdjustmentListener HAdjustmentListener}-b
     * and returns the resulting multicast listener.
     * 
     * @param a
     *            HAdjustmentListener-a
     * @param b
     *            HAdjustmentListener-b
     */
    public static HAdjustmentListener add(HAdjustmentListener a, HAdjustmentListener b)
    {
        return (HAdjustmentListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HAdjustmentListener
     * HAdjustmentListener} from {@link org.havi.ui.event.HAdjustmentListener
     * HAdjustmentListener}-l and returns the resulting multicast listener.
     * 
     * @param l
     *            HAdjustmentListener-l
     * @param oldl
     *            the HAdjustmentListener being removed
     */
    public static HAdjustmentListener remove(HAdjustmentListener l, HAdjustmentListener oldl)
    {
        return (HAdjustmentListener) removeInternal(l, oldl);
    }

    /**
     * Adds {@link org.havi.ui.event.HActionListener HActionListener}-a with
     * {@link org.havi.ui.event.HActionListener HActionListener}-b and returns
     * the resulting multicast listener.
     * 
     * @param a
     *            HActionListener-a
     * @param b
     *            HActionListener-b
     */
    public static HActionListener add(HActionListener a, HActionListener b)
    {
        return (HActionListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HActionListener HActionListener}
     * from {@link org.havi.ui.event.HActionListener HActionListener}-l and
     * returns the resulting multicast listener.
     * 
     * @param l
     *            HActionListener-l
     * @param oldl
     *            the HActionListener being removed
     */
    public static HActionListener remove(HActionListener l, HActionListener oldl)
    {
        return (HActionListener) removeInternal(l, oldl);
    }

    /**
     * Adds {@link org.havi.ui.event.HKeyListener HKeyListener}-a with
     * {@link org.havi.ui.event.HKeyListener HKeyListener}-b and returns the
     * resulting multicast listener.
     * 
     * @param a
     *            HKeyListener-a
     * @param b
     *            HKeyListener-b
     */
    public static HKeyListener add(HKeyListener a, HKeyListener b)
    {
        return (HKeyListener) addInternal(a, b);
    }

    /**
     * Removes the old {@link org.havi.ui.event.HKeyListener HKeyListener} from
     * {@link org.havi.ui.event.HKeyListener HKeyListener}-l and returns the
     * resulting multicast listener.
     * 
     * @param l
     *            HKeyListener-l
     * @param oldl
     *            the HKeyListener being removed
     */
    public static HKeyListener remove(HKeyListener l, HKeyListener oldl)
    {
        return (HKeyListener) removeInternal(l, oldl);
    }

    /**
     * Handles the ResourceStatusEvent by invoking the statusChanged methods on
     * listener-a and listener-b.
     * 
     * @param e
     *            the ResourceStatusEvent event
     */
    public void statusChanged(ResourceStatusEvent e)
    {
        if (a != null) ((ResourceStatusListener) a).statusChanged(e);
        if (b != null) ((ResourceStatusListener) b).statusChanged(e);
    }

    /**
     * Adds ResourceStatusListener-a with listener-b and returns the resulting
     * multicast listener. In a HAVi UI implementation, ResourceStatusListener
     * shall extend EventListener.
     * 
     * @param a
     *            listener-a
     * @param b
     *            listener-b
     */
    public static ResourceStatusListener add(ResourceStatusListener a, ResourceStatusListener b)
    {
        return (ResourceStatusListener) addInternal(a, b);
    }

    /**
     * Removes the old ResourceStatusListener from ResourceStatusListener-l and
     * returns the resulting multicast listener. In a HAVi UI implementation,
     * ResourceStatusListener shall extend EventListener.
     * 
     * @param l
     *            ResourceStatusListener-l
     * @param oldl
     *            the ResourceStatusListener being removed
     */
    public static ResourceStatusListener remove(ResourceStatusListener l, ResourceStatusListener oldl)
    {
        return (ResourceStatusListener) removeInternal(l, oldl);
    }

}
