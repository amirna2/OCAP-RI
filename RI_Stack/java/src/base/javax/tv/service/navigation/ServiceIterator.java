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

package javax.tv.service.navigation;

import java.util.NoSuchElementException;
import javax.tv.service.Service;

/**
 * <code>ServiceIterator</code> permits iteration over an ordered list of
 * <code>Service</code> objects. Applications may use the
 * <code>ServiceIterator</code> interface to browse a <code>ServiceList</code>
 * forward or backward.
 * <p>
 * 
 * Upon initial usage, <code>hasPrevious()</code> will return <code>false</code>
 * and <code>nextService()</code> will return the first <code>Service</code> in
 * the list, if present.
 * 
 * @see ServiceList
 */
public interface ServiceIterator
{

    /**
     * Resets the iterator to the beginning of the list, such that
     * <code>hasPrevious()</code> returns <code>false</code> and
     * <code>nextService()</code> returns the first <code>Service</code> in the
     * list (if the list is not empty).
     * 
     * 
     */
    public abstract void toBeginning();

    /**
     * Sets the iterator to the end of the list, such that
     * <code>hasNext()</code> returns <code>false</code> and
     * <code>previousService()</code> returns the last <code>Service</code> in
     * the list (if the list is not empty).
     */
    public abstract void toEnd();

    /**
     * Reports the next <code>Service</code> object in the list. This method may
     * be called repeatedly to iterate through the list.
     * 
     * @return The <code>Service</code> object at the next position in the list.
     * 
     * @throws NoSuchElementException
     *             If the iteration has no next <code>Service</code>.
     */
    public abstract Service nextService();

    /**
     * Reports the previous <code>Service</code> object in the list. This method
     * may be called repeatedly to iterate through the list in reverse order.
     * 
     * @return The <code>Service</code> object at the previous position in the
     *         list.
     * 
     * @throws NoSuchElementException
     *             If the iteration has no previous <code>Service</code>.
     */
    public abstract Service previousService();

    /**
     * Tests if there is a <code>Service</code> in the next position in the
     * list.
     * 
     * @return <code>true</code> if there is a <code>Service</code> in the next
     *         position in the list; <code>false</code> otherwise.
     */
    public abstract boolean hasNext();

    /**
     * Tests if there is a <code>Service</code> in the previous position in the
     * list.
     * 
     * @return <code>true</code> if there is a <code>Service</code> in the
     *         previous position in the list; <code>false</code> otherwise.
     */
    public abstract boolean hasPrevious();

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
