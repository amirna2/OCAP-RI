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
package org.ocap.shared.dvr.navigation;

import org.ocap.shared.dvr.RecordingRequest;

// JAS CableLabs should remove both imports from this stub.
/**
 * RecordingList represents a list of recordings.
 */
public interface RecordingList
{

    /**
     * Creates a new <code>RecordingList</code> object that is a subset of this
     * list, based on the conditions specified by a
     * <code>RecordingListFilter</code> object. This method may be used to
     * generate increasingly specialized lists of <code>RecordingRequest</code>
     * objects based on multiple filtering criteria. If the filter is
     * <code>null</code>, the resulting <code>RecordingList</code> will be a
     * duplicate of this list.
     * <p>
     * Note that the <code>accept</code> method of the given
     * <code>RecordingListFilter</code> will be invoked for each
     * <code>RecordingRequest</code> to be filtered using the same application
     * thread that invokes this method.
     * 
     * @param filter
     *            A filter constraining the requested recording list, or
     *            <code>null</code>.
     * @return A <code>RecordingList</code> object created based on the
     *         specified filtering rules.
     */
    public RecordingList filterRecordingList(RecordingListFilter filter);

    /**
     * Generates an iterator on the <code>RecordingRequest</code> elements in
     * this list.
     * 
     * @return A <code>RecordingListIterator</code> on the
     *         <code>RecordingRequest</code>s in this list.
     */
    public RecordingListIterator createRecordingListIterator();

    /**
     * Tests if the indicated <code>RecordingRequest</code> object is contained
     * in the list.
     * 
     * @param entry
     *            The <code>RecordingRequest</code> object for which to search.
     * @return <code>true</code> if the specified <code>RecordingRequest</code>
     *         is member of the list; <code>false</code> otherwise.
     */
    public boolean contains(RecordingRequest entry);

    /**
     * Reports the position of the first occurrence of the indicated
     * <code>RecordingRequest</code> object in the list.
     * 
     * @param entry
     *            The <code>RecordingRequest</code> object for which to search.
     * @return The index of the first occurrence of the <code>entry</code>, or
     *         <code>-1</code> if <code>entry</code> is not contained in the
     *         list.
     */
    public int indexOf(RecordingRequest entry);

    /**
     * Reports the number of <code>RecordingRequest</code> objects in the list.
     * 
     * @return The number of <code>RecordingRequest</code> objects in the list.
     */
    public int size();

    /**
     * Reports the <code>RecordingRequest</code> at the specified index
     * position.
     * 
     * @param index
     *            A position in the <code>RecordingList</code>.
     * @return The <code>RecordingRequest</code> at the specified index.
     * @throws java.lang.IndexOutOfBoundsException
     *             If <code>index</code> < 0 or <code>index</code> >
     *             <code>size()-1</code>.
     */
    public RecordingRequest getRecordingRequest(int index);

    /**
     * Creates a new <code>RecordingList</code> that contains all the elements
     * of this list sorted according to the criteria specified by a
     * <code>RecordingListComparator</code>.
     * 
     * @param sortCriteria
     *            the sort criteria to be applied to sort the entries in the
     *            recording list.
     * 
     * @return A sorted copy of the recording list.
     */
    public RecordingList sortRecordingList(RecordingListComparator sortCriteria);

}
