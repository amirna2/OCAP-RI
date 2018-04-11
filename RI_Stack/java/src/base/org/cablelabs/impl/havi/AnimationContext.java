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

import org.cablelabs.impl.manager.AnimationManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * The <code>AnimationContext</code> class is used to represent an
 * <i>animation</i> implementation. It is the interface through which the
 * <code>AnimationManager</code> controls the animation. The
 * {@link AnimationManager.AnimationContext#advancePosition()} method is used by
 * the <code>AnimationManager</code> to advance the current frame position of
 * the <i>animation</i> .
 * 
 * @see AnimationManager
 */
public abstract class AnimationContext
{
    private CallerContext context = null;

    public AnimationContext()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        context = ccm.getCurrentContext();
    }

    /**
     * @return the <code>CallerContext</code> under which this
     *         <code>AnimationContext</code> was instantiated.
     */
    public CallerContext getCallerContext()
    {
        return context;
    }

    /**
     * Countdown variable used to keep track of how many time periods before
     * next position advancement. Basically a scratch area for AnimationManager
     * implementation to work.
     */
    public int wait = 0;

    /**
     * Returns the <code>Object</code> which uniquely represents the animation
     * in question. Generally, this will be the <i>animation</i> object.
     * 
     * @return the animation <code>Object</code> which this
     *         <code>AnimationContext</code> represents.
     */
    public abstract Object getAnimation();

    /**
     * Returns the <i>delay</i> between frames of the animation. This should be
     * specified in units of 100 milliseconds. For example, a delay of 2 means
     * 200 milliseconds.
     * 
     * @return the <i>delay</i> in milliseconds divided by 100
     */
    public abstract int getDelay();

    /**
     * Advances the represented <i>animation</i> by one frame position according
     * to the current playback mode.
     * 
     * @return <code>true</code> if the advanced to frame is the last frame to
     *         play
     */
    public abstract boolean advancePosition();

    /**
     * Used to determine if this animation context is the current one and should
     * be used.
     * 
     * @return <code>true</code> if this <code>AnimationContext</code> is
     *         current and should be used
     */
    public abstract boolean isAnimated();
}
