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
 * The {@link org.havi.ui.HOrientable HOrientable} interface provides support
 * for components which have an orientation.
 * <p>
 * 
 * Widgets of HAVi compliant applications implementing the
 * <code>HOrientable</code> interface must have <code>HComponent</code> in their
 * inheritance tree.
 */

public interface HOrientable
{
    /**
     * A constant which specifies that the {@link org.havi.ui.HOrientable
     * HOrientable} shall be rendered with a horizontal orientation, with the
     * minimum value on the left side, and the maximum value on the right side.
     */
    public static final int ORIENT_LEFT_TO_RIGHT = 0;

    /**
     * A constant which specifies that the {@link org.havi.ui.HOrientable
     * HOrientable} shall be rendered with a horizontal orientation, with the
     * minimum value on the right side, and the maximum value on the left side.
     */
    public static final int ORIENT_RIGHT_TO_LEFT = 1;

    /**
     * A constant which specifies that the {@link org.havi.ui.HOrientable
     * HOrientable} shall be rendered with a vertical orientation, with the
     * minimum value on the top, and the maximum of the range on the bottom.
     */
    public static final int ORIENT_TOP_TO_BOTTOM = 2;

    /**
     * A constant which specifies that the {@link org.havi.ui.HOrientable
     * HOrientable} shall be rendered with a vertical orientation, with the
     * minimum value on the bottom, and the maximum value on the top.
     */
    public static final int ORIENT_BOTTOM_TO_TOP = 3;

    /**
     * Retrieve the orientation of the {@link org.havi.ui.HOrientable
     * HOrientable}. The orientation controls the layout of the component.
     * 
     * @return one of {@link org.havi.ui.HOrientable#ORIENT_LEFT_TO_RIGHT
     *         ORIENT_LEFT_TO_RIGHT},
     *         {@link org.havi.ui.HOrientable#ORIENT_RIGHT_TO_LEFT
     *         ORIENT_RIGHT_TO_LEFT},
     *         {@link org.havi.ui.HOrientable#ORIENT_TOP_TO_BOTTOM
     *         ORIENT_TOP_TO_BOTTOM}, or
     *         {@link org.havi.ui.HOrientable#ORIENT_BOTTOM_TO_TOP
     *         ORIENT_BOTTOM_TO_TOP}.
     */
    public int getOrientation();

    /**
     * Set the orientation of the {@link org.havi.ui.HOrientable HOrientable}.
     * The orientation controls the layout of the component.
     * 
     * @param orient
     *            one of {@link org.havi.ui.HOrientable#ORIENT_LEFT_TO_RIGHT
     *            ORIENT_LEFT_TO_RIGHT},
     *            {@link org.havi.ui.HOrientable#ORIENT_RIGHT_TO_LEFT
     *            ORIENT_RIGHT_TO_LEFT},
     *            {@link org.havi.ui.HOrientable#ORIENT_TOP_TO_BOTTOM
     *            ORIENT_TOP_TO_BOTTOM}, or
     *            {@link org.havi.ui.HOrientable#ORIENT_BOTTOM_TO_TOP
     *            ORIENT_BOTTOM_TO_TOP}.
     */
    public void setOrientation(int orient);
}
