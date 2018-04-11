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

package org.cablelabs.impl.util;

import java.lang.reflect.Array;

/**
 * A stand-in for the java.util.Arrays class that is not available before Java2.
 * Only some of the methods are implemented.
 * 
 * @author Aaron Kamienski
 * @author schoonma
 */
public abstract class Arrays
{
    /**
     * A much more reasonable toString() for arrays.
     * 
     * @throws IllegalArgumentException
     *             - if argument is not an array.
     */
    public static String toString(Object array)
    {
        StringBuffer b = new StringBuffer();
        try
        {
            b.append("[ ");

            for (int i = 0; i < Array.getLength(array); ++i)
            {
                if (i > 0) b.append(", ");

                Object o = Array.get(array, i);

                if (o == null)
                    b.append("null");
                else if (o.getClass().isArray())
                    b.append(toString(o));
                else
                    b.append(o.toString());
            }

            b.append(" ]");
        }
        catch (NullPointerException e)
        {
            return "null";
        }
        return b.toString();
    }

    /**
     * Create a new array that contains the same objects as the original array.
     * 
     * @param from
     *            the array to copy.
     * @param elementClass
     *            the class of the elements in the
     * @param from
     *            array.
     * @return an array containing the same objects as the original (
     * @param from
     *            ) array, in the same order.
     */
    public static Object copy(Object[] from, Class elementClass)
    {
        // Null in, null out.
        if (from == null) return null;

        // Allocate new array of same element type and fill its entries with
        // from elements.
        Object to = Array.newInstance(elementClass, from.length);
        System.arraycopy(from, 0, to, 0, from.length);
        return to;
    }

    /**
     * Copy an <code>int</code> array.
     * 
     * @param from
     *            - array copy.
     * @return a copy of
     * @param from
     *            .
     */
    public static int[] copy(int[] from)
    {
        if (from == null) return null;
        int[] to = new int[from.length];
        System.arraycopy(from, 0, to, 0, from.length);
        return to;
    }

    /**
     * Copy an <code>short</code> array.
     * 
     * @param from
     *            - array copy.
     * @return a copy of
     * @param from
     *            .
     */
    public static short[] copy(short[] from)
    {
        if (from == null) return null;
        short[] to = new short[from.length];
        System.arraycopy(from, 0, to, 0, from.length);
        return to;
    }

    /**
     * Copy an <code>float</code> array.
     * 
     * @param from
     *            - array copy.
     * @return a copy of
     * @param from
     *            .
     */
    public static float[] copy(float[] from)
    {
        if (from == null) return null;
        float[] to = new float[from.length];
        System.arraycopy(from, 0, to, 0, from.length);
        return to;
    }
}
