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

package org.dvb.user;

import java.util.Vector;

/**
 * This abstract class defines the Preference object. A Preference maps a name
 * to a list of favourite values. The first element in the list is the favourite
 * value for this preference.
 * <p>
 * The preference names are treated as case-insensitive. The preference names
 * shall be considered equal at least when the method
 * java.lang.String.equalsIgnoreCase() returns true for the strings when the
 * locale "EN.UK" is used. Depending on the locale used in the implementation,
 * implementations are allowed to consider equal also other upper and lower case
 * character pairs in addition to those defined by the "EN.UK" locale.
 * <p>
 * The standardized preference names in the present document shall only use such
 * letters where the upper and lower case characters are recognized by the
 * "EN.UK" locale.
 *
 * @author Todd Earles
 */
public abstract class Preference
{
    /**
     * Preference name
     */
    private String name;

    /**
     * Preference values
     */
    private Vector values;

    /**
     * This protected constructor is only present to enable sub-classes of this
     * one to be defined by the platform. It is not intended to be used by
     * inter-operable applications.
     */
    protected Preference()
    {
        this.name = null;
        this.values = new Vector();
    }

    /**
     * Creates a new preference with the specified name and the specified value.
     * This single value will be the favourite one for this preference.
     *
     * @param name
     *            a String object representing the name of the preference.
     * @param value
     *            a String object representing the value of the preference.
     */
    public Preference(String name, String value)
    {
        this.name = name;
        this.values = new Vector();
        if (value != null) add(value);
    }

    /**
     * Creates a new preference with the specified name and the specified value
     * set. Each value in the value set must appear only once. The behaviour
     * if a value is duplicated is implementation dependent.
     *
     * @param name
     *            a String object representing the name of the preference.
     * @param values
     *            an array of String objects representing the set of values for
     *            this preference ordered from the most favourite to the least
     *            favourite.
     */
    public Preference(String name, String values[])
    {
        this.name = name;
        this.values = new Vector();
        if ((values != null) && (values.length != 0)) add(values);
    }

    /**
     * Adds a new value for this preference. The value is added to the end of
     * the list. If the value is already in the list then it is moved to the end
     * of the list.
     *
     * @param value
     *            a String object representing the new value.
     */
    public void add(String value)
    {
        // Remove the value from the list if present then add the value to the
        // end of the list.
        synchronized (values)
        {
            values.removeElement(value);
            values.addElement(value);
        }
    }

    /**
     * Adds several new values for this preferences. The values are added to the
     * end of the list in the same order as they are found in the array passed
     * to this method. Any values already in the list are moved to the position
     * in the list which they would have if they were not already present.
     *
     * @param values
     *            an array of strings representing the values to add
     * @since MHP 1.0.1
     */
    public void add(String values[])
    {
        // Add each value to the list
        int i;
        for (i = 0; i < values.length; i++)
        {
            add(values[i]);
        }
    }

    /**
     * Adds a new value for this preference. The value is inserted at the
     * specified position. If the value is already in the list then it is moved
     * to the position specified. If the position is greater than the length of
     * the list, then the value is added to the end of this list. If the
     * position is negative, then the value is added to the beginning of this
     * list.
     *
     * @param position
     *            an int representing the position in the list.
     * @param value
     *            a String representing the new value to insert.
     */
    public void add(int position, String value)
    {
        synchronized (values)
        {
            // Remove the value from the list
            values.removeElement(value);

            // Add the value at the specified location
            int size = values.size();
            if (position < 0)
                position = 0;
            else if (position > size) position = size;
            values.insertElementAt(value, position);
        }
    }

    /**
     * Returns the list of favourite values for this preference. Returns an
     * empty array if no value sets are defined for this preference.
     *
     * @return an array of String representing the favourite values for this
     *         preference.
     */
    public String[] getFavourites()
    {
        synchronized (values)
        {
            // Create an array large enough to hold all the values
            String favorites[] = new String[values.size()];

            // Copy each string
            values.copyInto(favorites);

            return favorites;
        }
    }

    /**
     * Returns the most favourite value for this preference, that is, the first
     * element of the list.
     *
     * @return a String representing the favourite values Returns null if no
     *         value is defined for this preference.
     */
    public String getMostFavourite()
    {
        synchronized (values)
        {
            if (values.isEmpty())
                return null;
            else
                return (String) (values.firstElement());
        }
    }

    /**
     * Returns the name of the preference.
     *
     * @return a String object representing the name of the preference.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the position in the list of the specified value.
     *
     * @param value
     *            a String representing the value to look for.
     *
     * @return an integer representing the position of the value in the list
     *         counting from zero. If the value is not found then it returns -1.
     */
    public int getPosition(String value)
    {
        return values.indexOf(value);
    }

    /**
     * Tests if this preference has at least one value set.
     *
     * @return true if this preference has at least one value set, false
     *         otherwise.
     */
    public boolean hasValue()
    {
        return !values.isEmpty();
    }

    /**
     * Removes the specified value from the list of favourites. If the value is
     * not in the list then the method call has no effect.
     *
     * @param value
     *            a String representing the value to remove.
     */
    public void remove(String value)
    {
        values.removeElement(value);
    }

    /**
     * Removes all the values of a preference
     *
     * @since MHP 1.0.1
     */
    public void removeAll()
    {
        values.removeAllElements();
    }

    /**
     * Sets the most favourite value for this preference. If the value is
     * already in the list, then it is moved to the head. If the value is not
     * already in the list then it is added at the head.
     *
     * @param value
     *            the most favourite value
     */
    public void setMostFavourite(String value)
    {
        // Add or move value to the first position in the list
        add(0, value);
    }

    /**
     * Convert name and favourites to a String.
     *
     * @return the preference name and favourites
     */
    public String toString()
    {
        String className = getClass().getName();
        int index = className.lastIndexOf('.');
        if (index > 0) className = className.substring(index);

        StringBuffer sb = new StringBuffer(className);
        sb.append(':');
        sb.append(getName());
        sb.append('[');
        String[] values = getFavourites();
        String comma = "";
        for (int i = 0; i < values.length; ++i)
        {
            sb.append(comma);
            sb.append(values[i]);
            comma = ",";
        }
        sb.append(']');

        return sb.toString();
    }
}
