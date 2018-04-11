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

import org.cablelabs.gear.data.*;

import org.havi.ui.HSwitchable;
import org.havi.ui.HLook;
import org.havi.ui.HSound;
import org.havi.ui.event.HActionEvent;

/**
 * <code>Toggle</code> extends the <code>Button</code> to implement the
 * {@link org.havi.ui.HSwitchable} interface, providing for toggle- and
 * radio-button-like user interaction.
 * <p>
 * <code>Toggle</code> is a replacement for {@link org.havi.ui.HToggleButton};
 * the main difference being that it does not restrict itself to one type of
 * data (namely <code>Image</code>-based graphics). It supports text and
 * animation data as well as graphics. Whether it is a text or graphic button
 * depends on the type of data and look associated with the component.
 * 
 * @author Aaron Kamienski
 * @version $Id: Toggle.java,v 1.2 2002/06/03 21:33:20 aaronk Exp $
 */
public class Toggle extends Button implements HSwitchable
{
    /*  ************************ Constructors ************************ */

    /**
     * Default constructor. The constructed <code>Toggle</code> is initialized
     * with no <i>content</i> and the standard look.
     */
    public Toggle()
    {
    }

    /**
     * Look constructor. The constructed <code>Toggle</code> is initialized with
     * no <i>content</i> and the given look.
     * 
     * @param look
     *            the look to be used by this component
     */
    public Toggle(HLook look)
    {
        super(look);
    }

    /**
     * Icon constructor. The constructed <code>Toggle</code> is initialized with
     * graphic data content and the standard look. The same content is used for
     * all states.
     * 
     * @param icon
     *            the graphic data content
     */
    public Toggle(GraphicData icon)
    {
        super(icon);
    }

    /**
     * Text constructor. The constructed <code>Toggle</code> is initialized with
     * string data content and the standard look. The same content is used for
     * all states.
     * 
     * @param text
     *            the string data content
     */
    public Toggle(String text)
    {
        super(text);
    }

    /**
     * Animation constructor. The constructed <code>Toggle</code> is initialized
     * with animation data content and the standard look. The same content is
     * used for all states.
     * 
     * @param anim
     *            the animation data content
     */
    public Toggle(AnimationData anim)
    {
        super(anim);
    }

    /**
     * Constructor which takes sizing and positioning parameters.
     * 
     * @param look
     *            the look to be used by this component
     * @param x
     *            the x-coordinate
     * @param y
     *            the y-coordinate
     * @param width
     *            the width
     * @param height
     *            the height
     */
    public Toggle(HLook look, int x, int y, int width, int height)
    {
        super(look, x, y, width, height);
    }

    /**
     * Constructor which takes all types of content as well as sizing and
     * positioning parameters.
     * 
     * @param look
     *            the look to be used by this component
     * @param x
     *            the x-coordinate
     * @param y
     *            the y-coordinate
     * @param width
     *            the width
     * @param height
     *            the height
     * @param icon
     *            the graphic data content
     * @param text
     *            the string data content
     * @param anim
     *            the animation data content
     */
    public Toggle(HLook look, int x, int y, int width, int height, GraphicData icon, String text, AnimationData anim)
    {
        super(look, x, y, width, height, icon, text, anim);
    }

    /*  ************************ Switch Group ************************ */

    /**
     * This is a convenience property, allowing one to add this
     * <code>HSwitchable</code> toggle to a <code>SwitchGroup</code>. This
     * effectively does the following:
     * 
     * <pre>
     * if (this.group != null) this.group.remove(this);
     * this.group = group;
     * group.add(this);
     * </pre>
     * 
     * Note that this object is removed from any previously assigned group.
     * 
     * <p>
     * 
     * This component can be added to only one <code>SwitchGroup</code> in this
     * manner. It can, however, be added to others directly.
     * 
     * @param group
     *            the <code>SwitchGroup</code> to add this toggle to
     */
    public void setGroup(SwitchGroup group)
    {
        SwitchGroup tmp = switchGroup;
        if (tmp != null) tmp.remove(this);
        switchGroup = group;
        if (group != null) group.add(this);
    }

    /**
     * Returns the <code>SwitchGroup</code> to which this component was assigned
     * using {@link #setGroup(SwitchGroup)}. This component may have been
     * assigned to a group directly, which will not be reflected in the return
     * value of this method.
     * 
     * @param the
     *            <code>SwitchGroup</code> that this toggle was added to using
     *            <code>setGroup</code>.
     */
    public SwitchGroup getGroup()
    {
        return switchGroup;
    }

    /*  ************************* HSwitchable ************************** */

    // Description copied from HSwitchable
    public boolean getSwitchableState()
    {
        return (getInteractionState() & ACTIONED_STATE_BIT) != 0;
    }

    // Description copied from HSwitchable
    public void setSwitchableState(boolean state)
    {
        int old = getInteractionState();
        setInteractionState(state ? (old | ACTIONED_STATE_BIT) : (old & ~ACTIONED_STATE_BIT));
    }

    // Description copied from HSwitchable
    public void setUnsetActionSound(HSound sound)
    {
        unsetActionSound = sound;
    }

    // Description copied from HSwitchable
    public HSound getUnsetActionSound()
    {
        return unsetActionSound;
    }

    /*  ************************* Event Processing ************************** */

    /**
     * Processes action events.
     * 
     * @param e
     *            the action event
     */
    public void processHActionEvent(HActionEvent evt)
    {
        int state = getInteractionState();

        // If enabled, then process event
        if ((state & DISABLED_STATE_BIT) == 0)
        {
            // Toggle the ACTIONED_STATE_BIT
            setInteractionState(state ^= ACTIONED_STATE_BIT);

            // Play the action sound if available
            HSound sound = ((state & ACTIONED_STATE_BIT) != 0) ? getActionSound() : getUnsetActionSound();
            if (sound != null) sound.play();

            fireActionEvent(evt);
        }
    }

    /**
     * The sound played when this HSwitchable transitions from the
     * {@link org.havi.ui.HState#ACTIONED_STATE_BIT ACTIONED_STATE}.
     */
    private HSound unsetActionSound = null;

    /**
     * The <code>SwitchGroup</code> that this <code>Toggle</code> belongs to.
     */
    private SwitchGroup switchGroup;
}
