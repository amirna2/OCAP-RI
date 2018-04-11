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

package org.cablelabs.gear.havi;

import org.havi.ui.HLook;
import org.havi.ui.HState;
import org.havi.ui.event.HFocusListener;
import org.havi.ui.event.HKeyListener;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

/**
 * The <code>SinglelineEntry</code> class is simply an extension of the original
 * HAVi <code>HSinglelineEntry</code> class with the addition of support for a
 * simplified {@link #getText() text} property. This is to allow the multiline
 * entry class to be used more readily as a JavaBean.
 * 
 * @author Aaron Kamienski
 * @version $Id: SinglelineEntry.java,v 1.3 2002/11/07 21:13:40 aaronk Exp $
 * 
 * @see MultilineEntry
 */
public class SinglelineEntry extends org.havi.ui.HSinglelineEntry
{

    /**
     * Default constructor.
     */
    public SinglelineEntry()
    {
        /*
         * This was added so that our components have a non-zero size when
         * created with the default constructor. When added to a form within an
         * IDE, our components would come up with a preferred size of (0,0).
         * Rule 4 of the HAVi specification for getPreferredSize() on HLook
         * states the following:
         * 
         * 4.If there is no content and no default size set then the return
         * value is the current size of the HVisible as returned by getSize.
         */
        setSize(10, 10);
    }

    /**
     * Retrieves the current text content of this component. This simply
     * provides for simpler access to the <code>HVisible</code>
     * {@link #getTextContent(int) text content} with a single call. (This is
     * valid because <code>HSinglelineEntry</code> only maintains one piece of
     * text content for <i>all</i> states).
     * 
     * @return the current text content of this component
     * @see #setText(String)
     * @see #getTextContent(int)
     */
    public String getText()
    {
        return getTextContent(HState.NORMAL_STATE);
    }

    /**
     * Sets the new text content of this component. This simply provides for
     * simpler access to the <code>HVisible</code>
     * {@link #setTextContent(String,int) text content} with a single call.
     * (This is valid because <code>HSinglelineEntry</code> only maintains one
     * piece of text content for <i>all</i> states).
     * 
     * @param the
     *            new text content of this component
     * @see #getText()
     * @see #getTextContent(int)
     */
    public void setText(String data)
    {
        setTextContent(data, HState.NORMAL_STATE);
    }

    /*  ************************ HVisible Overrides ************************ */

    /**
     * Overrides <code>HSinglelineEntry.getLook()</code>. This is necessary only
     * because {@link org.havi.ui.HSinglelineEntry#setLook(HLook)} is overridden
     * to remove the only-accepts-an-<code>HSinglelineEntryLook</code>
     * restriction.
     * 
     * @return the current look
     */
    public HLook getLook()
    {
        HLook look = super.getLook();
        return (look instanceof Look) ? ((Look) look).getComponentLook() : look;
    }

    /**
     * Overrides <code>HSinglelineEntry.setLook()</code>. This is necessary only
     * to remove the only-accepts-an-<code>HSinglelineEntryLook</code>
     * restriction. No check is made to ensure that the given look will display
     * the content being used by this component.
     * 
     * @param the
     *            new look
     */
    public void setLook(HLook hLook)
    {
        try
        {
            super.setLook((hLook instanceof org.havi.ui.HSinglelineEntryLook) ? hLook : new Look(hLook));
        }
        catch (org.havi.ui.HInvalidLookException never)
        {
            // Should NEVER happen!
            never.printStackTrace();
        }
    }

    /**
     * Determines whether this component is currently enabled or disabled.
     * Equivalent to:
     * 
     * <pre>
     * ((getInteractionState() &amp; HState.DISABLED_STATE_BIT) == 0)
     * </pre>
     * 
     * @return <code>true</code> if this component is not disabled;
     *         <code>false</code> otherwise
     */
    public boolean isEnabled()
    {
        return (getInteractionState() & DISABLED_STATE_BIT) == 0;
    }

    /**
     * Sets the enabled/disabled state of this component. Equivalent to:
     * 
     * <pre>
     * setInteraction(enable ? (getInteractionState() &amp; &tilde;DISABLED_STATE_BIT) : (getInteractionState() | DISABLED_STATE_BIT));
     * </pre>
     * 
     * @param enable
     *            if <code>true</code> then the component will be enabled; if
     *            <code>false</code> then the component will be disabled
     */
    public void setEnabled(boolean enable)
    {
        int state = getInteractionState();

        if (enable)
            state &= ~DISABLED_STATE_BIT;
        else
            state |= DISABLED_STATE_BIT;

        setInteractionState(state);
    }

    /*  ************************* Other ************************** */

    /**
     * Returns whether this component is focus traversable or not. By default,
     * it is (because it implements the <code>HNavigable</code> interface), but
     * this can be changed.
     * 
     * @return whether this component is focus traversable or not
     */
    public boolean isFocusTraversable()
    {
        return traversable;
    }

    /**
     * Sets the focus traversable status of this component. By default, it is
     * <code>true</code> (because it implements the <code>HNavigable</code>
     * interface), but this can be changed.
     * 
     * @param traversable
     *            whether this component should be focus traversable or not
     */
    public void setFocusTraversable(boolean traversable)
    {
        this.traversable = traversable;
    }

    /**
     * Defines the set of the characters which are valid for customized keyboard
     * input, i.e. when the input type is set to
     * {@link org.havi.ui.HKeyboardInputPreferred#INPUT_CUSTOMIZED
     * INPUT_CUSTOMIZED}.
     * 
     * @param inputChars
     *            a <code>String</code> which contains all of the valid input
     *            characters.
     */
    public void setValidInputString(String inputChars)
    {
        if (inputChars != null)
            setValidInput(inputChars.toCharArray());
        else
            setValidInput(null);
    }

    /**
     * Retrieve the customized input character range. The return value of this
     * method should reflect the range of input keys which the component wishes
     * to see, should {@link org.havi.ui.HKeyboardInputPreferred#getType()
     * getType} return a value with the
     * {@link org.havi.ui.HKeyboardInputPreferred#INPUT_CUSTOMIZED
     * INPUT_CUSTOMIZED} bit set. This method may return <code>null</code> if
     * customized input is not requested.
     * 
     * 
     * 
     * 
     * @return a <code>String</code> containing the characters which this
     *         component expects the platform to provide, or <code>null</code>.
     */
    public String getValidInputString()
    {
        char[] validInput = getValidInput();
        return validInput == null ? null : new String(getValidInput());
    }

    /*
     * NOTE: the reason (add|remove)FocusListener/KeyListener is overridden here
     * is because of a "bug" in Forte/Netbeans that doesn't allow for event
     * subclasses. It figures the "declaring" class of a listener method (e.g.,
     * focusGained() or actionPerformed()) and expects that to be the event
     * listener class (e.g., FocusEvent or ActionEvent) instead of the actual
     * listener type (e.g., HFocusEvent or HActionEvent).
     */

    /**
     * <i>Use {@link #addHFocusListener} instead</i>.
     * <p>
     * Adds the specified {@link FocusListener} to receive {@link FocusEvent}
     * events sent from this component. If the listener has already been added
     * further calls will add further references to the listener, which will
     * then receive multiple copies of a single event.
     * <p>
     * It should be noted that only <code>HFocusEvents</code> will be received
     * by added <code>FocusListeners</code>; no <i>plain</i> AWT
     * <code>FocusEvents</code> can be expected.
     * 
     * @param l
     *            the FocusListener.
     * @see #addHFocusListener(HFocusListener)
     */
    public synchronized void addFocusListener(final FocusListener l)
    {
        HFocusListener hl;

        if (listeners == null) listeners = new Hash();
        if ((hl = (HFocusListener) listeners.get(l)) == null) hl = ListenerAdapter.getAdapter(l);
        listeners.put(l, hl);
        addHFocusListener(hl);
    }

    /**
     * <i>Use {@link #removeHFocusListener} instead</i>.
     * <p>
     * Removes the specified {@link FocusListener}so that it no longer receives
     * {@link FocusEvent} events from this component. If the specified listener
     * is not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param l
     *            the FocusListener.
     * @see #removeHFocusListener(HFocusListener)
     */
    public synchronized void removeFocusListener(FocusListener l)
    {
        HFocusListener hfl = (HFocusListener) listeners.remove(l);
        if (hfl != null) removeHFocusListener(hfl);
    }

    /**
     * <i>Use {@link #addHKeyListener} instead</i>.
     * <p>
     * Adds the specified {@link KeyListener} to receive {@link KeyEvent} events
     * sent from this component. If the listener has already been added further
     * calls will add further references to the listener, which will then
     * receive multiple copies of a single event.
     * <p>
     * It should be noted that only <code>HKeyEvents</code> will be received by
     * added <code>KeyListeners</code>; no <i>plain</i> AWT
     * <code>KeyEvents</code> can be expected.
     * 
     * @param l
     *            the KeyListener.
     * @see #addHKeyListener(HKeyListener)
     */
    public synchronized void addKeyListener(final KeyListener l)
    {
        HKeyListener hl;

        if (listeners == null) listeners = new Hash();
        if ((hl = (HKeyListener) listeners.get(l)) == null) hl = ListenerAdapter.getAdapter(l);
        listeners.put(l, hl);
        addHKeyListener(hl);
    }

    /**
     * <i>Use {@link #removeHKeyListener} instead</i>.
     * <p>
     * Removes the specified {@link KeyListener} so that it no longer receives
     * {@link KeyEvent} events from this component. If the specified listener is
     * not registered, the method has no effect. If multiple references to a
     * single listener have been registered it should be noted that this method
     * will only remove one reference per call.
     * 
     * @param l
     *            the KeyListener.
     * @see #removeHKeyListener(HKeyListener)
     */
    public synchronized void removeKeyListener(KeyListener l)
    {
        HKeyListener hkl = (HKeyListener) listeners.remove(l);
        if (hkl != null) removeHKeyListener(hkl);
    }

    /** Focus traversable property. */
    private boolean traversable = true;

    /** Maps FocusListeners/KeyListeners to HFocusListener/HKeyListener adapters */
    private java.util.Hashtable listeners;

    /**
     * Private look adapter class used to wrap other kinds of looks so that they
     * are suitable for use by <code>HSinglelineEntry</code> superclass.
     * 
     * @see #getLook()
     * @see #setLook(HLook)
     */
    private class Look extends org.cablelabs.gear.havi.decorator.SinglelineEntryLookAdapter
    {
        public Look(HLook look)
        {
            super(look);
        }
    }

    /**
     * Private listener adapter for wrapping AWT listener classes.
     */
    /* package */static class ListenerAdapter implements HKeyListener, HFocusListener
    {
        FocusListener fl;

        KeyListener kl;

        ListenerAdapter(FocusListener l)
        {
            fl = l;
        }

        ListenerAdapter(KeyListener l)
        {
            kl = l;
        }

        static HKeyListener getAdapter(KeyListener l)
        {
            if (l instanceof HKeyListener)
                return (HKeyListener) l;
            else
                return new ListenerAdapter(l);
        }

        static HFocusListener getAdapter(FocusListener l)
        {
            if (l instanceof HFocusListener)
                return (HFocusListener) l;
            else
                return new ListenerAdapter(l);
        }

        public void keyPressed(KeyEvent e)
        {
            kl.keyPressed(e);
        }

        public void keyReleased(KeyEvent e)
        {
            kl.keyPressed(e);
        }

        public void keyTyped(KeyEvent e)
        {
            kl.keyPressed(e);
        }

        public void focusGained(FocusEvent e)
        {
            fl.focusGained(e);
        }

        public void focusLost(FocusEvent e)
        {
            fl.focusLost(e);
        }
    }

    /**
     * Private Hashtable extension class that accepts multiple puts and requires
     * just as many removes.
     */
    /* package */static class Hash extends java.util.Hashtable
    {
        private class Count
        {
            int n;

            Object o;

            public Count(Object o)
            {
                this.o = o;
                n = 1;
            }
        }

        public Object put(Object key, Object value)
        {
            Count exists = (Count) get(key);
            // Won't replace with new value... don't care
            if (exists != null)
            {
                exists.n++;
                return value;
            }
            else
            {
                super.put(key, new Count(value));
                return null;
            }
        }

        public Object remove(Object key)
        {
            Count exists = (Count) get(key);
            if (exists != null)
            {
                if (--exists.n == 0) super.remove(key);
                return exists.o;
            }
            return null;
        }

        public Object get(Object key)
        {
            Count exists = (Count) get(key);
            return (exists == null) ? null : exists.o;
        }
    }
}
