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

/**
 * This iterator could be used to traverse entries in a RecordingList.
 * An iterator for lists that allows the programmer to traverse the list in either
 * direction and to obtain the iterator's current position in the list. A
 * RecordingListIterator has no current element; its cursor position always lies
 * between the element that would be returned by a call to previousEntry(),
 * previousEntries(n), nextEntry(),and nextEntries(n), where each traverse
 * backward by 1, n, and forward by 1, n respectively. In a list of length n, there are N+1
 * valid index values, from 0 to n, inclusive.
 *<pre>
 *        Element(0)   Element(1)   Element(2)   ... Element(n)
 *        ^            ^            ^            ^               ^
 * Index: 0            1            2            3               N+1
 *</pre>
 * Total failure to traverse the list shall not change the position.
 * When a request to traverses by n entries would result in going beyond the list, it
 * shall traverse as much possible and change the position accordingly. The orders of
 * the entries in the array returned by both nextEntries(n) or previousEntries(n)
 * shall be the same the order maintained within the implementation.
 */
public interface RecordingListIterator
{

    /**
     * Resets the iterator to the beginning of the list, such that
     * <code>hasPrevious()</code> returns <code>false</code> and
     * <code>nextEntry()</code> returns the first <code>RecordingRequest</code>
     * in the list (if the list is not empty).
     */
    public void toBeginning();

    /**
     * Sets the iterator to the end of the list, such that
     * <code>hasNext()</code> returns <code>false</code> and
     * <code>previousEntry()</code> returns the last
     * <code>RecordingRequest</code> in the list (if the list is not empty).
     */
    public void toEnd();

    /**
     * Gets the next <code>RecordingRequest</code> object in the list. This
     * method may be called repeatedly to iterate through the list.
     *
     * @return The <code>RecordingRequest</code> object at the next position in
     *         the list.
     *
     * @throws java.util.NoSuchElementException
     *             If the iteration has no next <code>RecordingRequest</code>.
     */
    public RecordingRequest nextEntry();

    /**
     * Gets the previous <code>RecordingRequest</code> object in the list. This
     * method may be called repeatedly to iterate through the list in reverse
     * order.
     *
     * @return The <code>RecordingRequest</code> object at the previous position
     *         in the list.
     *
     * @throws java.util.NoSuchElementException
     *             If the iteration has no previous
     *             <code>RecordingRequest</code>.
     */
    public RecordingRequest previousEntry();

    /**
     * Tests if there is a <code>RecordingRequest</code> in the next position in
     * the list.
     *
     * @return <code>true</code> if there is a <code>RecordingRequest</code> in
     *         the next position in the list; <code>false</code> otherwise.
     */
    public boolean hasNext();

    /**
     * Tests if there is a <code>RecordingRequest</code> in the previous
     * position in the list.
     *
     * @return <code>true</code> if there is a <code>RecordingRequest</code> in
     *         the previous position in the list; <code>false</code> otherwise.
     */
    public boolean hasPrevious();

    /**
     * Gets the next 'n' <code>RecordingRequest</code> objects in the list. This
     * method also advances the current position within the list. If the
     * requested number of entries are not available, the remaining elements are
     * returned. If the current position is at the end of the iterator, this
     * method returns an array with length zero.
     *
     * @param n
     *            the number of next entries requested.
     *
     * @return an array containing the next 'n' <code>RecordingRequest</code>
     *         object from the current position in the list.
     */
    public RecordingRequest[] nextEntries(int n);

    /**
     * Gets the previous 'n' <code>RecordingRequest</code> objects in the list.
     * This method also changes the current position within the list. If the
     * requested number of entries are not available, the remaining elements are
     * returned. If the current position is at the beginning of the iterator,
     * this method returns an array with length zero.
     *
     * @param n
     *            the number of previous entries requested.
     *
     * @return an array containing the previous 'n'
     *         <code>RecordingRequest</code> object from the current position in
     *         the list.
     */
    public RecordingRequest[] previousEntries(int n);

    /**
     * Gets the <code>RecordingRequest</code> object at the specified position.
     * This method does not advance the current position within the list.
     *
     * @param index
     *            the position of the RecordingRequest to be retrieved.
     *
     * @return the RecordingRequest at the specified position.
     *
     * @throws IndexOutOfBoundsException
     *             if the index is greater than the size of the list.
     */
    public RecordingRequest getEntry(int index);

    /**
     * Gets the position of a specified recording request in the list.
     *
     * @param entry
     *            The recording request for which the position is sought.
     *
     * @return The position of the specified recording; -1 if the entry is not
     *         found.
     */
    public int getPosition(RecordingRequest entry);

    /**
     * Gets the current position of the RecordingListIterator. This would be the
     * position from where the next RecordingRequest will be retrieved when an
     * application calls the nextEntry.
     *
     * @return the current position of the RecordingListIterator.
     */
    public int getPosition();

    /**
     * Sets the current position of the RecordingListIterator. This would be the
     * position from where the next RecordingRequest will be retrieved when an
     * application calls the nextEntry.
     *
     * @param index
     *            the current position of the RecordingListIterator would be set
     *            to this value.
     *
     * @throws IndexOutOfBoundsException
     *             if the index is greater than the size of the list.
     */
    public void setPosition(int index) throws IndexOutOfBoundsException;

    /**
     * Gets the recording list corresponding to this RecordingListIterator.
     *
     * @return the RecordingList corresponding to this iterator.
     */
    public RecordingList getRecordingList();
}
