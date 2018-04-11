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
 * This {@link org.havi.ui.HMatteLayer HMatteLayer} interface enables the
 * presentation of components, together with an associated
 * {@link org.havi.ui.HMatte HMatte}, for matte compositing.
 * 
 * @see HMatte
 */

public interface HMatteLayer
{
    /**
     * Applies an {@link org.havi.ui.HMatte HMatte} to this component, for matte
     * compositing. Any existing animated matte must be stopped before this
     * method is called or an HMatteException will be thrown.
     * 
     * @param m
     *            The {@link org.havi.ui.HMatte HMatte} to be applied to this
     *            component -- note that only one matte may be associated with
     *            the component, thus any previous matte will be replaced. If m
     *            is null, then any matte associated with the component is
     *            removed and further calls to getMatte() shall return null. The
     *            component shall behave as if it had a fully opaque
     *            {@link org.havi.ui.HFlatMatte HFlatMatte} associated with it
     *            (i.e an HFlatMatte with the default value of 1.0.)
     * @exception HMatteException
     *                if the {@link org.havi.ui.HMatte HMatte} cannot be
     *                associated with the component. This can occur:
     *                <ul>
     *                <li>if the specific matte type is not supported
     *                <li>if the platform does not support any matte type
     *                <li>if the component is associated with an already running
     *                {@link org.havi.ui.HFlatEffectMatte HFlatEffectMatte} or
     *                {@link org.havi.ui.HImageEffectMatte HImageEffectMatte}.
     *                The exception is thrown even if m is null.
     *                </ul>
     * @see HMatte
     */
    public void setMatte(HMatte m) throws HMatteException;

    /**
     * Get any {@link org.havi.ui.HMatte HMatte} currently associated with this
     * component.
     * 
     * @return the {@link org.havi.ui.HMatte HMatte} currently associated with
     *         this component or null if there is no associated matte.
     */
    public HMatte getMatte();
}
