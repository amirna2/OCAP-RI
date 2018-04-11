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
 * The {@link org.havi.ui.HFlatMatte HFlatMatte} class represents a matte that
 * is constant over space and time. It is specified as a floating point value in
 * the range 0.0 to 1.0 where:
 * <ul>
 * <li>0.0 is fully transparent
 * <li>values between 0.0 and 1.0 are partially transparent to the nearest
 * supported transparency value.
 * <li>1.0 is fully opaque
 * </ul>
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
 * <td>data</td>
 * <td>The transparency value for this flat effect matte.</td>
 * <td>1.0</td>
 * <td>{@link org.havi.ui.HFlatMatte#setMatteData setMatteData}</td>
 * <td>{@link org.havi.ui.HFlatMatte#getMatteData getMatteData}</td>
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
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 *
 * @author Todd Earles
 * @author Aaron Kamienski (1.0.1 support)
 * @version 1.1
 */

public class HFlatMatte implements HMatte
{

    /**
     * Creates an {@link org.havi.ui.HFlatMatte HFlatMatte} object. See the
     * class description for details of constructor parameters and default
     * values.
     */
    public HFlatMatte()
    {
        this(1.0f);
    }

    /**
     * Creates an {@link org.havi.ui.HFlatMatte HFlatMatte} object. See the class description for
     * details of constructor parameters and default values.
     */
    public HFlatMatte(float data)
    {
        setMatteData(data);
    }

    /**
     * Sets the data for this matte. Any previously set data is replaced.
     *
     * @param data
     *            the data for this matte.
     */
    public void setMatteData(float data)
    {
        if (data < 0.0f || data > 1.0f) throw new IllegalArgumentException("See API documentation");

        alpha = data;
    }

    /**
     * Returns the data used for this matte.
     *
     * @return the data used for this matte (a single number).
     */
    public float getMatteData()
    {
        return alpha;
    }

    /** Current alpha value for the matte */
    private float alpha;
}
