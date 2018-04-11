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

import org.cablelabs.gear.data.AnimationData;

/**
 * Classes that implement the <code>HasAnimationData</code> interface support an
 * indexed property composed of <code>AnimationData</code> objects.
 * <p>
 * For classes which extend from {@link org.havi.ui.HVisible} (e.g.,
 * <code>Label</code>), the indexed property size is fixed at 8 elements (one
 * for each possible {@link org.havi.ui.HState state}). However, it should be
 * noted that the indices passed to the methods should actually be the object's
 * state values (e.g., {@link org.havi.ui.HState#NORMAL_STATE NORMAL_STATE}).
 * 
 * @author Aaron Kamienski
 * @author $Id: HasAnimationData.java,v 1.2 2002/06/03 21:33:16 aaronk Exp $
 * 
 * @see Label
 */
public interface HasAnimationData extends HasDataContent
{
    /**
     * Returns the <code>animationData</code> indexed property as an array of
     * <code>AnimationData</code> objects. The array is a <i>clone</i> of the
     * internal array maintained by the implementing class.
     * <p>
     * {@link org.havi.ui.HVisible} objects always return an array of 8 data
     * objects.
     * 
     * @return the <code>animationData</code> indexed property
     */
    public AnimationData[] getAnimationData();

    /**
     * Sets the <code>animationData</code> indexed property as an array of
     * <code>AnimationData</code> objects.
     * <p>
     * {@link org.havi.ui.HVisible} objects only accept arrays of 8 data
     * objects. <code>HVisible</code> objects should notify their current look
     * of the change by calling the {@link org.havi.ui.HLook#widgetChanged
     * widgetChanged} method <i>once</i> with a single
     * {@link org.havi.ui.HVisible#UNKNOWN_CHANGE UNKNOWN_CHANGE} hint.
     * 
     * @param data
     *            the new values for the <code>animationData</code> indexed
     *            property
     */
    public void setAnimationData(AnimationData[] data);

    /**
     * Returns the <code>animationData</code> indexed property indexed by
     * <code>i</code>
     * <p>
     * {@link org.havi.ui.HVisible} objects only accept indices corresponding to
     * the {@link org.havi.ui.HState HState-defined} states.
     * 
     * @param i
     *            index into the <code>animationData</code> indexed property
     * @return the <code>AnimationData</code> indexed by <code>i</code>
     * 
     * @throws IllegalArgumentException
     *             if <code>i</code> is not among the set of accepted values
     *             (e.g., <code>HState</code>-defined states)
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is out of the acceptable bounds
     */
    public AnimationData getAnimationData(int i) throws IllegalArgumentException, IndexOutOfBoundsException;

    /**
     * Sets the <code>animationData</code> indexed property indexed by
     * <code>i</code>
     * <p>
     * {@link org.havi.ui.HVisible} objects only accept indices corresponding to
     * the {@link org.havi.ui.HState HState-defined} states.
     * <code>HVisible</code> objects should notify their current look of the
     * change by calling the {@link org.havi.ui.HLook#widgetChanged
     * widgetChanged} method with a single
     * {@link org.havi.ui.HVisible#UNKNOWN_CHANGE UNKNOWN_CHANGE} hint.
     * 
     * @param i
     *            index into the <code>animationData</code> indexed property
     * @param data
     *            the new value for the <code>AnimationData</code> indexed by
     *            <code>i</code>
     * 
     * @throws IllegalArgumentException
     *             if <code>i</code> is not among the set of accepted values
     *             (e.g., <code>HState</code>-defined states)
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is out of the acceptable bounds
     */
    public void setAnimationData(int i, AnimationData data) throws IllegalArgumentException, IndexOutOfBoundsException;
}
