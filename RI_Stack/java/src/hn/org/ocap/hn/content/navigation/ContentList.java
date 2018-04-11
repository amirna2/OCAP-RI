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

package org.ocap.hn.content.navigation;

import java.util.Enumeration;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.DatabaseException;

/**
 * This interface represents a list of filtered ContentEntry objects. A
 * ContentList may contain a complete or partial subset of entries resulting
 * from an application requested filter, browse or search.
 */
public interface ContentList extends Enumeration
{
    /**
     * Gets the number of ContentEntry objects in this ContentList.
     * 
     * @return Number of entries in this list. Returns 0 if the list is empty.
     */
    public int size();

    /**
     * Gets to total number of ContentEntry matches in the filter, browse or
     * search operation that generated this ContentList. This value SHALL be
     * greater than or equal to the value returned from the size() method of
     * this ContentList.
     * 
     * This value SHALL be greater than the value returned from the size()
     * method of this ContentList if the <i>requestedCount</i> parameter of the
     * originating content entry request was less than the total entry matches
     * of the requesting operation.
     * 
     * See {@link org.ocap.hn.ContentServerNetModule}.
     * 
     * 
     * @return the total number of ContentEntry matches from the originating
     *         content entry request
     */
    public int totalMatches();

    /**
     * Sets the metadata sort order of the items in this list based on metadata
     * key identifiers using signed property values.
     * 
     * The sortOrder parameter of this method is a string containing the
     * properties and sort modifiers to be used to sort the resulting
     * ContentList. The format of the string containing the sort criteria shall
     * follow the format defined in UPnP Content Directory Service 3.0
     * specification section 2.3.16: A_ARG_TYPE_SortCriteria.
     * 
     * @param sortOrder
     *            a String representing the sortOrder for this ContentList.
     */
    public void setSortOrder(String sortOrder);

    /**
     * Gets the sort order set by the #setSortOrder method.
     * 
     * @return The array of sort keys, or null if the setPreferredSortOrder
     *         method has not been called for this list.
     */
    public String getSortOrder();

    /**
     * Finds the first {@link ContentEntry} which identifier for the key '
     * <code>key</code>' equals the given object <code>obj</code>. For instance,
     * if key == "Title" then obj represents the title, e.g. "Best movie ever"
     * and this method will return the first ContentEntry in the list than
     * contains a match for the (key, value) pair.
     * 
     * @param key
     *            The identifier key.
     * @param value
     *            The object to compare to
     * 
     * @return The first matched ContentEntry, or null if no match found.
     */
    public ContentEntry find(String key, Object value);

    /**
     * Finds the first ContentEntry which matches the search. The keys and
     * values parameters are parallel arrays. For example, if keys[0] == "TITLE"
     * and values[0] == "Best movie ever", the implementation SHALL match the
     * first ContentEntry in the list where the metadata contains that (key,
     * value) pair, as well as matches any other entries in the parameter
     * arrays.
     * 
     * @param keys
     *            Array of identifier keys.
     * @param values
     *            Array of values.
     * 
     * @return The first matching ContentEntry found, or null if no match. If
     *         the parameter arrays are not the same length this method returns
     *         null.
     */
    public ContentEntry find(String[] keys, Object[] values);

    /**
     * Finds all ContentEntry objects which match the search. Same as the
     * #find(String[], Object[]) method except all matches are returned instead
     * of just the first match.
     * 
     * @param keys
     *            Array of identifier keys.
     * @param values
     *            Array of values.
     * 
     * @return A ContentList containing all matches, or null if no matches were
     *         found.
     */
    public ContentList findAll(String[] keys, Object[] values);

    /**
     * Filters the ContentList. The returned ContentList is a new ContentList
     * only containing ContentItems on which ContentDatabaseFilter.accept
     * returned true.
     * 
     * @param filter
     *            the ContentDatabaseFilter
     * 
     * @return newly created ContentList containing only the filtered
     *         ContentItems.
     * 
     * @throws DatabaseException
     *             ; see DatabaseException for exception reasons.
     */
    public ContentList filterContentList(ContentDatabaseFilter filter) throws DatabaseException;
}
