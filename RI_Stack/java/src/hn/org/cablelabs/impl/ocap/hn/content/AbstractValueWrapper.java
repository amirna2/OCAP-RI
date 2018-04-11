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

package org.cablelabs.impl.ocap.hn.content;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * This abstract class is intended to be the direct superclass of every class
 * implementing the <code>ValueWrapper</code> interface.
 *
 * Classes extending this class need do only these things:
 * <ul>
 * <li> define the four required constructors, each one simply delegating
 *      to "super"
 * <li> implement the elementClass method, defining a common superclass or
 *      superinterface of element values
 * <li> implement the fromString method, for the conversion from formatted
 *      string to property value
 * <li> implement the toString method, for the conversion from property
 *      value to formatted string
 * </ul>
 */
public abstract class AbstractValueWrapper implements ValueWrapper
{
    private final boolean multiple;
    private final List sequence = new ArrayList();
    private final Class elementClass;

    protected AbstractValueWrapper(boolean multiple, String s)
    {
        this.multiple = multiple;

        assert s != null;

        sequence.add(fromString(s));

        elementClass = elementClass();
    }

    protected AbstractValueWrapper(boolean multiple, String[] sa)
    {
        this.multiple = multiple;

        assert sa != null && sa.length > 0;

        for (int i = 0, n = sa.length; i < n; ++ i)
        {
            sequence.add(fromString(sa[i]));
        }

        elementClass = elementClass();
    }

    protected AbstractValueWrapper(boolean multiple, Object o)
    {
        this.multiple = multiple;

        assert o != null;

        sequence.add(o);

        elementClass = o.getClass();
    }

    protected AbstractValueWrapper(boolean multiple, Object[] oa)
    {
        this.multiple = multiple;

        assert oa != null && oa.length > 0;

        for (int i = 0, n = oa.length; i < n; ++ i)
        {
            sequence.add(oa[i]);
        }

        elementClass = oa.getClass().getComponentType();
    }

// TODO: disallow if single-valued?
    /**
     * Add an element to the sequence.
     * @param o The element.
     */
    public void add(Object o)
    {
        sequence.add(o);
    }

// TODO: disallow if single-valued?
    /**
     * Add an element (represented by a formatted String) to the sequence.
     * @param s A formatted String representation of the element.
     */
    public void add(String s)
    {
        sequence.add(fromString(s));
    }

    /**
     * Returns the number of values in the sequence.
     * @return The number of values in the sequence.
     */
    public int getLength()
    {
        return sequence.size();
    }

    /**
     * Returns the property value as per section 6.3.6.1.
     * @return The property value as per section 6.3.6.1.
     */
    public final Object getSection6361Value()
    {
        int n = sequence.size();

        if (n == 1)
        {
            return sequence.get(0);
        }

        assert n > 1;

        Object[] result = (Object[]) Array.newInstance(elementClass, n);

        for (int i = 0; i < n; ++ i)
        {
            result[i] = sequence.get(i);
        }

        return result;
    }

    /**
     * Returns the property value as per section 6.3.6.2.
     * @return The property value as per section 6.3.6.2.
     */
    public final Object getSection6362Value()
    {
        assert sequence.size() > 0;

        if (! multiple)
        {
            return sequence.get(0);
        }

        int n = sequence.size();

        Object[] result = (Object[]) Array.newInstance(elementClass, n);

        for (int i = 0; i < n; ++ i)
        {
            result[i] = sequence.get(i);
        }

        return result;
    }

    /**
     * Returns an element of the sequence.
     * @param i The index of the element.
     * @return The element.
     */
    public Object getValue(int i)
    {
        assert 0 <= i && i < sequence.size();

        return sequence.get(i);
    }

    /**
     * Returns a formatted String representation of an element of the sequence,
     * to be used in the construction of a DIDL-Lite XML document.
     * @param i The index of the element.
     * @return A formatted String representation of the element.
     */
    public String getXMLValue(int i)
    {
        assert 0 <= i && i < sequence.size();

        return toString(sequence.get(i));
    }

    protected abstract Class elementClass();

    protected abstract Object fromString(String s);

    protected abstract String toString(Object o);
}
