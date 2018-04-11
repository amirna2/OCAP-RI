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

package org.ocap.hn;

import java.util.Enumeration;
import java.util.Properties;

/**
 * The filter for (key,value) pair filtering mechanism. If a device or a
 * NetModule has same value on all of the specified keys, it is regarded as a
 * match.
 */
public class PropertyFilter
{
    private Properties properties = null;

    /**
     * Constructs a PropertyFilter object.
     * 
     * @param prop
     *            Initial properties for this Property filter
     */
    public PropertyFilter(Properties prop)
    {
        if (prop == null)
        {
            this.properties = new Properties();
        }
        else
        {
            this.properties = prop;
        }
    }

    /**
     * Adds a (key,value) pair to the filter. If the key is already in the list,
     * no action is taken.
     * 
     * @param key
     *            New key which will be used for filtering.
     * @param value
     *            Value for the new key.
     */
    public void addProperty(String key, String value)
    {
        if (!properties.containsKey(key))
        {
            properties.setProperty(key, value);
        }
    }

    /**
     * Checks whether a key is in the list.
     * 
     * @param key
     *            Key to be checked against.
     * 
     * @return True if key is in the list; otherwise returns false.
     */
    public boolean contains(String key)
    {
        if (properties.containsKey(key))
        {
            return true;
        }

        return false;
    }

    /**
     * Checks whether an element is accepted by this filter, the element must be
     * either <code>NetModule</code> or <code>Device</code>. If a
     * NetModule/Device's properties share the same value as all properties from
     * this filter, it is accepted and true is returned; otherwise, false is
     * returned.
     * 
     * @param element
     *            Element to be checked against.
     * 
     * @return True if the element is accepted by the PropertyFilter, otherwise
     *         returns false.
     */
    public boolean accept(Object element)
    {
        PropertyContainer pc;

        if (element instanceof Device)
        {
            pc = new PrivateDevice((Device) element);
        }
        else if (element instanceof NetModule)
        {
            pc = new PrivateNetModule((NetModule) element);
        }
        else
        {
            return false;
        }

        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();)
        {
            String propertyName = (String) e.nextElement();

            if (!properties.getProperty(propertyName).equals(pc.getProperty(propertyName)))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Remove a key from the filter, if the key is not in the property list, no
     * action is taken.
     * 
     * @param key
     *            Key to be removed from list.
     */
    public void removeKey(String key)
    {
        if (properties.containsKey(key))
        {
            properties.remove(key);
        }
    }

    /**
     * Remove keys from the filter, if a key is not in the property list, it is
     * disregarded; while others are processed as normal.
     * 
     * @param keys
     *            Keys to be removed from the list.
     */
    public void removeKeys(String[] keys)
    {
        for (int i = 0; i < keys.length; i++)
        {
            if (properties.containsKey(keys[i]))
            {
                properties.remove(keys[i]);
            }
        }
    }

    // If the spec is ever changed to factor common method declarations
    // from Device and NetModule into a superinterface such as this, this
    // silliness will not be necessary.

    private interface PropertyContainer
    {
        String getProperty(String key);
    }

    private static class PrivateDevice implements PropertyContainer
    {
        private Device o;

        PrivateDevice(Device o)
        {
            this.o = o;
        }

        public String getProperty(String key)
        {
            return o.getProperty(key);
        }
    }

    private static class PrivateNetModule implements PropertyContainer
    {
        private NetModule o;

        PrivateNetModule(NetModule o)
        {
            this.o = o;
        }

        public String getProperty(String key)
        {
            return o.getProperty(key);
        }
    }
}
