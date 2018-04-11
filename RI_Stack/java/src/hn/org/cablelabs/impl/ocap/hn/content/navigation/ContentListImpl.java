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

package org.cablelabs.impl.ocap.hn.content.navigation;

import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import org.cablelabs.impl.ocap.hn.content.ContentEntryComparator;
import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.DatabaseExceptionImpl;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.DatabaseException;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.content.navigation.ContentDatabaseFilter;
import org.ocap.hn.content.navigation.ContentList;

/**
 * Implements HN.ContentList which contains a list of ContentEntry objects.
 * 
 * @author Michael A. Jastad
 * @version $Revision$
 * 
 * @see
 */
public class ContentListImpl extends Vector implements ContentList, Cloneable
{
    /** serialization ID */
    private static final long serialVersionUID = 1L;

    /** instance iterator */
    private Iterator m_iterator = null;

    /** sort order */
    private String m_sortOrder = null;

    /**
     * Creates a new ContentListImpl object.
     */
    public ContentListImpl()
    {
    }

    /**
     * Returns a Filtered ContentList.
     * 
     * @param filter
     *            is a ContentDatabaseFilter used to filter the ContentList.
     * 
     * @return ContentList which returns a list of objects from this list based
     *         on the filter.
     * 
     * @throws DatabaseException
     *             Thrown if the Database filter is incorrect.
     */
    public ContentList filterContentList(ContentDatabaseFilter filter) throws DatabaseException
    {
        if (null == filter)
        {
            throw new DatabaseExceptionImpl("", DatabaseException.QUERY_IS_INVALID);
        }

        ContentListImpl contentList = new ContentListImpl();

        if (filter instanceof DatabaseQueryImpl)
        {
            for (int i = 0; i < super.size(); ++i)
            {
                ContentEntryImpl contentEntry = (ContentEntryImpl) get(i);

                /* re-get the original filter */
                DatabaseQueryImpl dqFilter = (DatabaseQueryImpl) filter;

                /*
                 * Iterate through the query terms checking field exists in this
                 * contentEntry
                 */
                do
                {
                    if (null == contentEntry.getRootMetadataNode().getMetadata(dqFilter.getField()))
                    {
                        throw new DatabaseExceptionImpl("", DatabaseException.FIELD_NAME_DOES_NOT_EXIST);
                    }

                    /* Get any subordinate query term */
                    dqFilter = dqFilter.getRemoteQuery();

                }
                while (null != dqFilter);

                if (filter.accept(contentEntry))
                {
                    contentList.add(contentEntry);
                }
            }
        }
        else
        {
            for (int i = 0; i < super.size(); ++i)
            {
                ContentEntry contentEntry = (ContentEntry) get(i);

                if (filter.accept(contentEntry))
                {
                    contentList.add(contentEntry);
                }
            }
        }

        return contentList;
    }

    /**
     * Searches through its list of Content Entries based on a <key,value> pair.
     * The key must match a property name of the element.
     * 
     * @param key
     *            The property name of the element.
     * @param value
     *            The value of the property name.
     * 
     * @return ContentEntry is the first item that matches the <key,value>,
     *         pair. If the Key,value pair doesn't match any element properties
     *         contained within the list the ContentEntry will be null.
     */
    public ContentEntry find(String key, Object value)
    {
        return find(new String[] { key }, new Object[] { value });
    }

    /**
     * Compares each Element.Property and Element.Value in the ElementList with
     * the array of Keys, and the array of values passed in, and returns the
     * first ContentEntry that matches all the specified values.
     * 
     * @param keys
     *            An array of property names to search for
     * @param values
     *            An array of property values to search for
     * 
     * @return ContentEntry the first instance of the ContentEntry that matches
     *         all of the specified values. If there is no element that matches
     *         all of the specified values, ContentEntry will be null.
     */
    public ContentEntry find(String[] keys, Object[] values)
    {
        ContentListImpl contentList = find(keys, values, true);
        return contentList.size() == 1 ? (ContentEntry) contentList.firstElement() : null;
    }

    /**
     * Searches the entire List of ContentEntrys for specific properties (keys)
     * and their values.
     * 
     * @param keys
     *            The property to search for
     * @param values
     *            The value of the property that must match
     * 
     * @return ContentList containing Elements matching the search criteria
     *         specified by the <Key, Value>, pairs.
     */
    public ContentList findAll(String[] keys, Object[] values)
    {
        ContentList contentList = find(keys, values, false);
        return contentList.size() > 0 ? contentList : null;
    }

    /**
     * Returns the sort order value initialized at startup or by the
     * "setSortOrder", method call.
     * 
     * @return String representing the sort order.
     */
    public String getSortOrder()
    {
        return m_sortOrder;
    }

    /**
     * Sets the sort order of ContentList returned during a find or findAll.
     *
     * @param sortOrder
     *            is a comma delimited string defining how Items should be
     *            sorted.
     */
    public void setSortOrder(String sortOrder)
    {
        if (Utils.checkSortCriteria(UPnPConstants.SORT_CAPABILITIES, sortOrder))
        {
            m_sortOrder = sortOrder;
            Collections.sort(this, new ContentEntryComparator(m_sortOrder));
        }
    }

    /**
     * Returns the size of the ContentList
     * 
     * @return int value representing the number of objects on the list
     */
    public int totalMatches()
    {
        String result = DIDLLite.getView(this);

        return super.size();
    }

    /**
     * Determines if this ContentList has more objects to iterate over.
     * 
     * @return boolean value of True if there is more objects on the
     *         ContentList. False indicates that there are no more objects to
     *         iterate on.
     */
    public boolean hasMoreElements()
    {
        return getIterator().hasNext();
    }

    /**
     * Returns the next object on this ContentList using the local Iterator
     * 
     * @return Object
     */
    public Object nextElement()
    {
        return getIterator().next();
    }

    /**
     * DOCUMENT ME!
     * 
     * @param entry
     *            DOCUMENT ME!
     */
    public void addContentEntry(ContentEntryImpl entry)
    {
        this.add(entry);
    }

    /**
     * DOCUMENT ME!
     * 
     * @param i
     *            DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public ContentEntryImpl getContentEntry(int i)
    {
        ContentEntryImpl contentEntry = null;

        if ((i >= 0) && (i < super.size()))
        {
            contentEntry = (ContentEntryImpl) this.get(i);
        }

        return contentEntry;
    }

    /**
     * Method combines find logic to support both cases of returning first entry
     * or returning all entries.
     * 
     * @param keys
     * @param values
     * @param firstOnly
     *            if true returns list after first match
     * @return conentList with the appropriate amount of entries
     */
    private ContentListImpl find(String[] keys, Object[] values, boolean firstOnly)
    {
        ContentListImpl contentList = new ContentListImpl();
        boolean found = false;

        if (keys.length == values.length)
        {
            MetadataNode metadataNode = null;

            // For each content entry in the list
            for (int i = 0; (i < super.size()) && !found; ++i)
            {
                metadataNode = getContentEntry(i).getRootMetadataNode();

                if ((metadataNode != null))
                {
                    // Make sure every key / value pair matches metadata value
                    int matches = 0;
                    for (int j = 0; j < keys.length; ++j)
                    {
                        Object value = metadataNode.getMetadata(keys[j]);
                        if (value != null)
                        {
                            if (value.equals(values[j]))
                            {
                                ++matches;
                            }
                        }
                    }

                    if (matches == keys.length)
                    {
                        contentList.addContentEntry(getContentEntry(i));

                        // Return list with first content entry matched
                        if (firstOnly)
                        {
                            return contentList;
                        }
                    }
                }
            }
        }

        return contentList;
    }

    /**
     * Returns this ContentList's iterator
     * 
     * @return Iterator
     */
    private Iterator getIterator()
    {
        if (m_iterator == null)
        {
            m_iterator = this.iterator();
        }

        return m_iterator;
    }

    public Object clone()
    {

        ContentListImpl cli = (ContentListImpl) super.clone();
        cli.setSortOrder(m_sortOrder);
        return (Object) cli;
    }
}
