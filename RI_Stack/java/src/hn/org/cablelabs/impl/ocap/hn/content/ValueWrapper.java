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

/**
 * This interface is implemented by classes used to convert MetadataNode property values
 * to DIDL-Lite formatted strings and back. Each supported conversion is represented
 * by an implementing class. Implementing classes should be registered statically
 * with {@link MetadataNodeImpl#registerProperty(QualifiedName, boolean, Class)}
 * in <code>MetadataNodeImpl</code>.
 *
 * Each implementing class supports both single-valued and multivalued properties,
 * by treating both single values and multiple values simply as sequences of values.
 *
 * An implementing class must implement four constructors. Each must take as its first
 * argument a boolean indicating whether or not the property is multivalued, and as
 * its second argument
 * <ol>
 * <li> a String <code>s</code> to be converted to a sequence (of length
 *      <code>1</code>) of values by the specific conversion
 * <li> a String[] <code>sa</code> to be converted to a sequence (of length
 *      <code>sa.length</code>) of values by the specific conversion
 * <li> an Object <code>o</code> that is a single property value subject
 *      to the specific conversion
 * <li> an Object[] <code>oa</code> that is a multiple property value subject
 *      to the specific conversion
 * </ol>
 *
 * @author Doug Galligan
 */
public interface ValueWrapper
{
    /**
     * Add an element to the sequence.
     * @param o The element.
     */
    public void add(Object o);

    /**
     * Add an element (represented by a formatted String) to the sequence.
     * @param s A formatted String representation of the element.
     */
    public void add(String s);

    /**
     * Returns the number of values in the sequence.
     * @return The number of values in the sequence.
     */
    public int getLength();

    /**
     * Returns the property value as per HNP section 6.3.6.1.
     * @return The property value as per HNP section 6.3.6.1.
     */
    public Object getSection6361Value();

    /**
     * Returns the property value as per HNP section 6.3.6.2.
     * @return The property value as per HNP section 6.3.6.2.
     */
    public Object getSection6362Value();

    /**
     * Returns an element of the sequence.
     * @param i The index of the element.
     * @return The element.
     */
    public Object getValue(int i);

    /**
     * Returns a formatted String representation of an element of the sequence,
     * to be used in the construction of a DIDL-Lite XML document.
     * @param i The index of the element.
     * @return A formatted String representation of the element.
     */
    public String getXMLValue(int i);
}
