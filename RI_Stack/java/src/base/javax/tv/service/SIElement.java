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

package javax.tv.service;

import javax.tv.locator.Locator;

/**
 * The base interface of elements provided by the SI database.
 * <code>SIElement</code> objects represent immutable <em>copies</em> of the
 * service information data contained in the SI database. If the information
 * represented by an <code>SIElement</code> <em>E</em> changes in the database,
 * <em>E</em> will not be changed. The value of the <code>SIElement</code>'s
 * locator (obtained by the <code>getLocator()</code> method) will remain
 * unchanged in this case; the locator may be used to retrieve a copy of the SI
 * element with the new data. Two <code>SIElement</code> objects retrieved from
 * the SI database using the same input <code>Locator</code> at different times
 * will report <code>Locator</code> objects that are equal according to
 * <code>Locator.equal()</code>. However, the <code>SIElement</code> objects
 * themselves will not be <code>equal()</code> if the corresponding data changed
 * in the SI database between the times of their respective retrievals.
 * 
 * @see #getLocator
 * @see SIManager#retrieveSIElement
 */
public interface SIElement extends SIRetrievable
{

    /**
     * Reports the <code>Locator</code> of this <code>SIElement</code>.
     * 
     * @return Locator The locator referencing this <code>SIElement</code>
     */
    public Locator getLocator();

    /**
     * Tests two <code>SIElement</code> objects for equality. Returns
     * <code>true</code> if and only if:
     * <ul>
     * <li><code>obj</code>'s class is the same as the class of this
     * <code>SIElement</code>, and
     * <p>
     * <li><code>obj</code>'s <code>Locator</code> is equal to the
     * <code>Locator</code> of this object (as reported by
     * <code>SIElement.getLocator()</code>, and
     * <p>
     * <li><code>obj</code> and this object encapsulate identical data.
     * </ul>
     * 
     * @param obj
     *            The object against which to test for equality.
     * 
     * @return <code>true</code> if the two <code>SIElement</code> objects are
     *         equal; <code>false</code> otherwise.
     */
    public boolean equals(Object obj);

    /**
     * Reports the hash code value of this <code>SIElement</code>. Two
     * <code>SIElement</code> objects that are equal will have identical hash
     * codes.
     * 
     * @return The hash code value of this <code>SIElement</code>.
     */
    public int hashCode();

    /**
     * Reports the SI format in which this <code>SIElement</code> was delivered.
     * <p>
     * If the SI format of the <code>SIElement</code> is not represented as a
     * constant in the class {@link ServiceInformationType}, the value returned
     * by this method may be outside the defined set of constants.
     * 
     * @return The SI format in which this SI element was delivered.
     */
    public ServiceInformationType getServiceInformationType();
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
