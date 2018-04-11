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

import java.awt.Image;

/**
 * {@link org.havi.ui.HListElement HListElement} is a holder for content used
 * with the {@link org.havi.ui.HListGroup HListGroup} component. It must contain
 * a text string, and may also contain a single graphical image.
 * 
 * <p>
 * Applications should <b>not</b> directly manipulate <code>HListElement</code>
 * objects. They are intended to be used in conjunction with an
 * {@link org.havi.ui.HListGroup} which maintains a list of them, and is
 * responsible for their rendering via the {@link org.havi.ui.HListGroupLook}
 * class. The methods <code>setIcon()</code> and <code>setLabel()</code> of
 * <code>HListElement</code> shall not be used for elements, which are part of
 * <code>HListGroup</code>. If an application requires to alter the content, it
 * shall either replace the entire element, or remove it temporarily and re-add
 * it after the content was changed.
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
 * <td>label</td>
 * <td>text content of this item</td>
 * <td>no default</td>
 * <td>-</td>
 * <td>{@link org.havi.ui.HListElement#getLabel getLabel}</td>
 * </tr>
 * <tr>
 * <td>icon</td>
 * <td>image content of this item</td>
 * <td>null</td>
 * <td>-</td>
 * <td>{@link org.havi.ui.HListElement#getIcon getIcon}</td>
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
 * @see HListGroup
 * @see HListGroupLook
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HListElement
{
    /**
     * Creates an HListElement object. See the class description for details of
     * constructor parameters and default values.
     * 
     * @param label
     *            The label for this HListElement.
     */
    public HListElement(java.lang.String label)
    {
        setLabel(label);
    }

    /**
     * Creates an HListElement object. See the class description for details of
     * constructor parameters and default values.
     * 
     * @param icon
     *            The icon for this HListElement.
     * @param label
     *            The label for this HListElement.
     */
    public HListElement(java.awt.Image icon, java.lang.String label)
    {
        setLabel(label);
        setIcon(icon);
    }

    /**
     * Retrieve the label for this HListElement.
     * 
     * @return the text label for this HListElement.
     */
    public java.lang.String getLabel()
    {
        return label;
    }

    /**
     * Retrieve the icon for this HListElement.
     * 
     * @return the graphical icon for this HListElement, or <code>null</code> if
     *         no icon was set.
     */
    public java.awt.Image getIcon()
    {
        return icon;
    }

    /**
     * Set the label for this HListElement.
     * 
     * @param label
     *            The label for this HListElement.
     */
    public void setLabel(java.lang.String label)
    {
        this.label = label;
    }

    /**
     * Set the icon for this HListElement. If icon is null, the HListElement
     * will be in the same state as if no icon was set.
     * 
     * @param icon
     *            The icon for this HListElement.
     */
    public void setIcon(java.awt.Image icon)
    {
        this.icon = icon;
    }

    /** The Image icon associated with this element. */
    private Image icon;

    /** The String label associated with this element. */
    private String label;
}
