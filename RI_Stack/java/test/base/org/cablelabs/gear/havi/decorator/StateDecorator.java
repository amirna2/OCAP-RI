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

package org.cablelabs.gear.havi.decorator;

import java.awt.Graphics;

import org.havi.ui.HLook;
import org.havi.ui.HState;
import org.havi.ui.HVisible;

/**
 * <code>StateDecorator</code> is an abstract base class that provides support
 * for state-dependent decorator support. A <code>StateDecorator</code> <i>does
 * its thing</i> only when the <code>HVisible</code> being rendered is in a
 * specific state. A <code>StateDecorator</code> instance can be set up to
 * function when the <code>HVisible</code> is in any arbitrary
 * {@link org.havi.ui.HState state} (e.g., only in
 * {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE} or in all non-disabled
 * state).
 * <p>
 * One caveat of the way that <code>StateDecorators</code> operate is that they
 * function the same for <i>all</i> states that they operate on. In order to do
 * act differently for different states, one must chain multiple
 * <code>StateDecorators</code> together -- each one operating on a different
 * set of states.
 * <p>
 * Subclasses are required to implement the {@link #showLook(Graphics,HVisible)}
 * method instead of overriding the {@link #showLook(Graphics,HVisible,int)}
 * (which is <code>final</code>) method to <i>do their thing</i>.
 * 
 * @author Aaron Kamienski
 * @version $Id: StateDecorator.java,v 1.7 2002/06/03 21:32:31 aaronk Exp $
 * 
 * @see BGColorDecorator
 * @see FGColorDecorator
 * @see FontDecorator
 * @see org.havi.ui.HState
 * 
 */
public abstract class StateDecorator extends DecoratorLook implements HState
{
    /**
     * Default constructor. Creates a <code>StateDecorator</code>, initialized
     * to operate in no states.
     * 
     * @see #setStateMask(int)
     * @see #addState(int)
     * @see #stateToMask(int)
     */
    public StateDecorator()
    {
        this(null, 0);
    }

    /**
     * Creates a <code>StateDecorator</code>, initialized to operate in no
     * states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * 
     * @see #setStateMask(int)
     * @see #addState(int)
     * @see #stateToMask(int)
     */
    public StateDecorator(HLook look)
    {
        this(look, 0);
    }

    /**
     * Creates a <code>StateDecorator</code>, intialized to operate in the given
     * set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param bitMask
     *            the bitMask specifying the states to operate in
     * 
     * @see #setStateMask(int)
     * @see #addState(int)
     * @see #stateToMask(int)
     */
    public StateDecorator(HLook look, int bitMask)
    {
        super(look);
        setStateMask(bitMask);
    }

    /**
     * Creates a <code>StateDecorator</code>, initialized to operation in the
     * given set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param states
     *            an array of {@link org.havi.ui.HState HState-defined} states
     * 
     * @throws IllegalArgumentException
     *             if an invalid state is specified
     * @throws NullPointerException
     *             if <code>states</code> is <code>null</code>
     */
    public StateDecorator(HLook look, int[] states)
    {
        super(look);
        for (int i = 0; i < states.length; ++i)
            addState(states[i]);
    }

    /**
     * Returns the bit mask that represents the states on which this
     * <code>StateDecorator</code> will operate.
     * <p>
     * This bit mask can be interpreted by using the {@link #stateToMask(int)}
     * method. For example, to determine if <code>NORMAL_STATE</code> is amongst
     * the states being operated on (without using the simpler
     * {@link #isStateEnabled(int state)} method):
     * 
     * <pre>
     * StateDecorator d = ...;
     * boolean normal = (d.getStateMask() & d.stateToMask(NORMAL_STATE)) != 0;
     * </pre>
     * 
     * @return the bit mask that represents the states on which this
     *         <code>StateDecorator</code> will operate
     * 
     * @see #setStateMask(int)
     * @see #addState(int)
     * @see #removeState(int)
     */
    public int getStateMask()
    {
        return states;
    }

    /**
     * Sets the bit mask that represents the states on which this
     * <code>StateDecorator</code> will operate.
     * <p>
     * This bit mask can be created using the {@link #stateToMask(int)} method.
     * For example, to enable operation in <code>NORMAL_STATE</code> and
     * <code>FOCUSED_STATE</code> in addition to other currently enabled states:
     * 
     * <pre>
     * StateDecorator d = ...;
     * d.setStateMask(d.getStateMask | 
     *                d.stateToMask(NORMAL_STATE) |
     *                d.stateToMask(FOCUSED_STATE));
     * </pre>
     * 
     * @see #getStateMask()
     * @see #addState(int)
     * @see #removeState(int)
     */
    public void setStateMask(int mask)
    {
        states = mask;
    }

    /**
     * Adds the given <code>state</code> to the set of states on which this
     * <code>StateDecorator</code> will operate.
     * 
     * @param state
     *            the state to add
     * 
     * @throws IllegalArgumentException
     *             if <code>state</code> is not a valid
     *             {@link org.havi.ui.HState} state.
     * 
     * @see #setStateMask(int)
     * @see #getStateMask()
     */
    public void addState(int state) throws IllegalArgumentException
    {
        setStateMask(getStateMask() | stateToMask(state));
    }

    /**
     * Determines whether the given state is enabled or not.
     * 
     * @param state
     *            the state the check
     * 
     * @throws IllegalArgumentException
     *             if <code>state</code> is not a valid
     *             {@link org.havi.ui.HState} state.
     * 
     * @see #getStateMask()
     * @see #stateToMask(int)
     */
    public boolean isStateEnabled(int state)
    {
        return (getStateMask() & stateToMask(state)) != 0;
    }

    /**
     * Removes the given <code>state</code> from the set of states on which this
     * <code>StateDecorator</code> will operate.
     * 
     * @param state
     *            the state to remove
     * 
     * @throws IllegalArgumentException
     *             if <code>state</code> is not a valid
     *             {@link org.havi.ui.HState} state.
     * 
     * @see #setStateMask(int)
     * @see #getStateMask()
     */
    public void removeState(int state) throws IllegalArgumentException
    {
        setStateMask(getStateMask() & ~stateToMask(state));
    }

    /**
     * Maps the given <code>state</code> to the appropriate bit mask for use in
     * {@link #setStateMask(int)}.
     * 
     * @param state
     *            the state to map
     * @return the unique bit mask that represents the given state
     * 
     * @throws IllegalArgumentException
     *             if <code>state</code> is not a valid
     *             {@link org.havi.ui.HState} state.
     */
    public static int stateToMask(int state) throws IllegalArgumentException
    {
        switch (state)
        {
            case NORMAL_STATE:
            case FOCUSED_STATE:
            case ACTIONED_STATE:
            case ACTIONED_FOCUSED_STATE:
            case DISABLED_STATE:
            case DISABLED_FOCUSED_STATE:
            case DISABLED_ACTIONED_STATE:
            case DISABLED_ACTIONED_FOCUSED_STATE:
                return 1 << (state & ~NORMAL_STATE);
            case ALL_STATES:
                return 0xff;
            default:
                throw new IllegalArgumentException("Invalid state: " + state);
        }
    }

    /**
     * Implements the subclass' {@link #showLook(Graphics,HVisible)} method only
     * if this <code>StateDecorator</code> is {@link #isStateEnabled(int)
     * enabled} for the given <code>state</code>.
     * <p>
     * This method is declared <code>final</code>; subclasses should override
     * the <code>showLook(Graphics, HVisible)</code> method in order to
     * implement the class.
     * 
     * @param g
     * @param visible
     * @param state
     */
    public final void showLook(Graphics g, HVisible visible, int state)
    {
        final boolean enabled = isStateEnabled(state);
        Object restore = null;

        if (enabled) restore = showLook(g, visible);

        super.showLook(g, visible, state);

        if (enabled) postShowLook(g, visible, restore);
    }

    /**
     * Subclasses implement this method to <i>do their thing</i>.
     * 
     * @param g
     * @param visible
     * @return an <code>Object</code> representing any state of the
     *         <code>visible</code> which should be restored after calling
     *         <code>super.showLook</code>
     */
    protected abstract Object showLook(Graphics g, HVisible visible);

    /**
     * Called following <code>super.showLook</code> to restore any state which
     * may have been altered by {@link #showLook(Graphics,HVisible)}.
     * 
     * @param g
     * @param visible
     * @param restore
     *            an <code>Object</code> representing the state to restore
     */
    protected void postShowLook(Graphics g, HVisible visible, Object restore)
    {
    }

    /**
     * The set of states (mapped to by <code>stateToMask</code>) that this
     * <code>StateDecorator</code> operates under.
     */
    private int states;
}
