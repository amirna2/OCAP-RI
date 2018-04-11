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

package org.cablelabs.impl.util.string;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * An immutable string with multiple versions based on language.
 * 
 * @author Todd Earles
 */
public class MultiString implements Serializable
{
    // Serialized version
    private static final long serialVersionUID = -2460910594359815738L;

    // Language array used to construct this object
    private final String[] languages;

    // Language array used to construct this object
    private final String[] values;

    // Mappings from language to value
    private transient Hashtable mapping;

    /**
     * Construct a <code>MultiString</code> from a set of language codes and a
     * corresponding set of values.
     * 
     * @param languages
     *            The language codes associated with each value. If the language
     *            code for a value is unknown then the corresponding entry in
     *            this array should be the empty string. If the same language
     *            code appears more than once the one used is undefined.
     * @param values
     *            The values associated with each language. The first entry in
     *            this array is the default value.
     * @throws IllegalArgumentException
     *             If <code>languages</code> or <code>values</code> is null or
     *             have different lengths or have a length of 0 or have any null
     *             elements.
     */
    public MultiString(String[] languages, String[] values)
    {
        // Verify arguments
        if (languages == null || values == null || languages.length == 0 || languages.length != values.length)
            throw new IllegalArgumentException();
        for (int i = 0; i < languages.length; i++)
            if (languages[i] == null || values[i] == null) throw new IllegalArgumentException();

        // Save arguments
        this.languages = languages;
        this.values = values;

        // Create hashtable
        createHashtable();
    }

    // Create the hashtable for easy access by lanugage code
    private void createHashtable()
    {
        int numLanguages = languages.length;
        mapping = new Hashtable(numLanguages, 1.0f);
        for (int i = 0; i < numLanguages; i++)
            mapping.put(languages[i], values[i]);
    }

    /**
     * Returns the languages array used to construct this object
     */
    public String[] getLanguages()
    {
        return languages;
    }

    /**
     * Returns the values array used to construct this object
     */
    public String[] getValues()
    {
        return values;
    }

    /**
     * Return the value corresponding to the requested language. The default
     * value is returned if a value in the requested language is not available
     * or <code>language</code> is null.
     */
    public String getValue(String language)
    {
        // Create the hashtable if not created during construction. This happens
        // when the object is created via de-serialization.
        if (mapping == null) createHashtable();

        // Lookup the value
        String value = null;
        if (language != null) value = (String) mapping.get(language);
        return (value == null) ? values[0] : value;
    }
}
