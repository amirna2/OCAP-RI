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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

package org.cablelabs.impl.manager.recording;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListComparator;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.shared.dvr.navigation.RecordingListIterator;

/**
 * @author Burt Wagner
 * 
 *         This class provides the implementation for both the RecordingList and
 *         RecordingListIterator interfaces.
 */
public class RecordingListImpl implements RecordingList
{
    private static final RecordingRequest[] EMPTY_RECORDING_REQUEST_ARRAY = {};

    /**
     * <code>recordings</code> holds the list of recordings obtained from the
     * main recording list maintained by the NavigationManager.
     */
    private List recordings;

    /**
     * Constructor
     * 
     * @param collection
     *            collection of recordings.
     */
    public RecordingListImpl(Collection collection)
    {
        recordings = new ArrayList(collection);
    }

    public RecordingListImpl()
    {
        recordings = new ArrayList();
    }

    protected List getRecordings()
    {
        return recordings;
    }

    // Creates a new RecordingList object that is a subset of this list,
    // based on the conditions specified by a RecordingListFilter object.
    // This method may be used to generate increasingly specialized lists
    // of RecordingRequest objects based on multiple filtering criteria.
    // If the filter is null, the resulting RecordingList will be a
    // duplicate of this list.
    // Note that the accept method of the given RecordingListFilter will
    // be invoked for each RecordingRequest to be filtered using the same
    // application thread that invokes this method.
    // Parameters: filter - A filter constraining the requested recording list,
    // or null.
    // Returns: A RecordingList object created based on the specified filtering
    // rules.
    public RecordingList filterRecordingList(RecordingListFilter recordingListFilter)
    {
        if (recordingListFilter == null)
        {
            return new RecordingListImpl(recordings);
        }

        List filteredList = new ArrayList();

        // Brief: Step through each RecordingListEntry which the caller has read
        // access,
        // apply the filter, add the entry to the list and so on.
        //
        // Acquire the AppID and FAP based from current caller context
        // Determine whether caller has monitor access or read access
        // using determineAccessLevel helper method, then store boolean result
        // to
        // access level flag... alf.

        Iterator iterator = recordings.iterator();
        while (iterator.hasNext())
        {
            RecordingRequest recordingRequest = (RecordingRequest) iterator.next();
            if (recordingListFilter.accept(recordingRequest))
            {
                filteredList.add(recordingRequest);
            }
        }

        return new RecordingListImpl(filteredList);
    }

    // Generates an iterator on the RecordingRequest elements in this list.
    // Returns: A RecordingListIterator on the RecordingRequests in this list.
    public RecordingListIterator createRecordingListIterator()
    {
        return new RecordingListIteratorImpl();
    }

    // Creates a new RecordingList that contains all the elements of this list
    // sorted according to the criteria specified by a RecordingListComparator.
    // Parameters: sortCriteria - the sort criteria to be applied to sort the
    // entries in the recording list.
    // Returns: A sorted copy of the recording list.
    public RecordingList sortRecordingList(RecordingListComparator recordingListComparator)
    {
        class ComparatorImpl implements Comparator
        {
            private RecordingListComparator recordingListComparator;

            ComparatorImpl(RecordingListComparator recordingListComparator)
            {
                this.recordingListComparator = recordingListComparator;
            }

            public int compare(Object first, Object second)
            {
                // I think this is correct. We need to swap the order here.
                return recordingListComparator.compare((RecordingRequest) first, (RecordingRequest) second);
            }
        }

        ArrayList arrayList = new ArrayList(recordings);
        Collections.sort(arrayList, new ComparatorImpl(recordingListComparator));
        return new RecordingListImpl(arrayList);
    }

    // Tests if the indicated RecordingRequest object is contained in the list.
    // Parameters: entry - The RecordingRequest object for which to search.
    // Returns: true if the specified RecordingRequest is member of the list;
    // false otherwise.
    public boolean contains(RecordingRequest entry)
    {
        return recordings.contains(entry);
    }

    // Reports the position of the first occurrence of the indicated
    // RecordingRequest object in the list.
    // Parameters: entry - The RecordingRequest object for which to search.
    // Returns: The index of the first occurrence of the entry,
    // or -1 if entry is not contained in the list.
    public int indexOf(RecordingRequest entry)
    {
        return recordings.indexOf(entry);
    }

    // Reports the number of RecordingRequest objects in the list.
    // Returns: The number of RecordingRequest objects in the list.
    public int size()
    {
        return recordings.size();
    }

    // Reports the RecordingRequest at the specified index position.
    // Parameters: index - A position in the RecordingList.
    // Returns: The RecordingRequest at the specified index.
    // Throws: java.lang.IndexOutOfBoundsException
    // - If index < 0 or index > size()-1.
    public RecordingRequest getRecordingRequest(int index)
    {
        return (RecordingRequest) recordings.get(index);
    }

    private class RecordingListIteratorImpl implements RecordingListIterator
    {
        private List arrayList;

        private int index;

        RecordingListIteratorImpl()
        {
            arrayList = new ArrayList(recordings);
            index = 0;
        }

        // Resets the iterator to the beginning of the list,
        // such that hasPrevious() returns false and
        // nextEntry() returns the first RecordingRequest in the list
        // (if the list is not empty).
        public void toBeginning()
        {
            index = 0;
        }

        // Sets the iterator to the end of the list,
        // such that hasNext() returns false and
        // previousEntry() returns the last RecordingRequest in the list
        // (if the list is not empty).
        public void toEnd()
        {
            index = arrayList.size();
        }

        // Tests if there is a RecordingRequest in the next position in the
        // list.
        // Returns: true if there is a RecordingRequest in the next position in
        // the list;
        // false otherwise.
        public boolean hasNext()
        {
            return index < arrayList.size();
        }

        // Tests if there is a RecordingRequest in the previous position in the
        // list.
        // Returns: true if there is a RecordingRequest in the previous position
        // in the list;
        // false otherwise.
        public boolean hasPrevious()
        {
            return index != 0;
        }

        // Gets the next RecordingRequest object in the list.
        // This method may be called repeatedly to iterate through the list.
        // Returns: The RecordingRequest object at the next position in the
        // list.
        // Throws: java.util.NoSuchElementException
        // - If the iteration has no next RecordingRequest.
        public RecordingRequest nextEntry()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException("Next index (" + index + ") is out of range.");
            }
            return (RecordingRequest) arrayList.get(index++);
        }

        // Gets the previous RecordingRequest object in the list.
        // This method may be called repeatedly to iterate through the list in
        // reverse order.
        // Returns: The RecordingRequest object at the previous position in the
        // list.
        // Throws: java.util.NoSuchElementException
        // - If the iteration has no previous RecordingRequest.
        public RecordingRequest previousEntry()
        {
            if (!hasPrevious())
            {
                throw new NoSuchElementException("Previous index (" + (index - 1) + ") is out of range.");
            }
            return (RecordingRequest) arrayList.get(--index);
        }

        // Gets the next ’n’ RecordingRequest objects in the list.
        // This method also advances the current position within the list.
        // If the requested number of entries are not available,
        // the remaining elements are returned.
        // If the current position is at the end of the iterator,
        // this method returns an array with length zero.
        // Parameter: n - the number of next entries requested.
        // Returns: an array containing the next ’n’ RecordingRequest object
        // from the current position in the list.
        public RecordingRequest[] nextEntries(int n)
        {
            RecordingRequest[] returned = EMPTY_RECORDING_REQUEST_ARRAY;
            int haveLeft = arrayList.size() - index;
            int numReturned = n > haveLeft ? haveLeft : n;

            if (numReturned > 0)
            {
                returned = (RecordingRequest[]) arrayList.subList(index, index + numReturned).toArray(returned);
                index += numReturned;
            }

            return returned;
        }

        // Gets the previous ’n’ RecordingRequest objects in the list.
        // This method also changes the current position within the list.
        // If the requested number of entries are not available,
        // the remaining elements are returned.
        // If the current position is at the beginning of the iterator,
        // this method returns an array with length zero.
        // Parameters: n - the number of previous entries requested.
        // Returns: an array containing the previous ’n’ RecordingRequest object
        // from the current position in the list.
        public RecordingRequest[] previousEntries(int n)
        {
            RecordingRequest[] returned = EMPTY_RECORDING_REQUEST_ARRAY;
            int numReturned = n > index ? index : n;

            if (numReturned > 0)
            {
                index -= numReturned;
                returned = (RecordingRequest[]) arrayList.subList(index, index + numReturned).toArray(returned);
            }

            return returned;
        }

        // Gets the RecordingRequest object at the specified position.
        // This method does not advance the current position within the list.
        // Parameters: index - the position of the RecordingRequest to be
        // retrieved.
        // Returns: the RecordingRequest at the specified position.
        // Throws: java.lang.IndexOutOfBoundsException
        // - if the index is greater than the size of the list.
        public RecordingRequest getEntry(int index)
        {
            return (RecordingRequest) arrayList.get(index);
        }

        // Gets the position of a specified recording request in the list.
        // Parameters: entry - The recording request for which the position is
        // sought.
        // Returns: The position of the specified recording; -1 if the entry is
        // not found.
        public int getPosition(RecordingRequest entry)
        {
            return arrayList.indexOf(entry);
        }

        // Gets the current position of the RecordingListIterator.
        // This would be the position from where the next RecordingRequest
        // will be retrieved when an application calls the nextEntry.
        // Returns: the current position of the RecordingListIterator.
        public int getPosition()
        {
            return index;
        }

        // Gets the recording list corresponding to this RecordingListIterator.
        // Returns: the RecordingList corresponding to this iterator.
        public RecordingList getRecordingList()
        {
            return RecordingListImpl.this;
        }

        // Sets the current position of the RecordingListIterator.
        // This would be the position from where the next RecordingRequest
        // will be retrieved when an application calls the nextEntry.
        // Parameters: index - the current position of the RecordingListIterator
        // would be set to this value.
        // Throws: java.lang.IndexOutOfBoundsException
        // - if the index is greater than the size of the list.
        public void setPosition(int index) throws IndexOutOfBoundsException
        {
            if ((index < 0) || (index > arrayList.size()))
            {
                throw new IndexOutOfBoundsException();
            }
            this.index = index;
        }

    } // End of RecordingListIteratorImpl class
}
