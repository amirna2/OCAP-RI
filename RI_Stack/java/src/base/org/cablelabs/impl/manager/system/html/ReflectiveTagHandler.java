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

package org.cablelabs.impl.manager.system.html;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

/**
 * TagHandler that uses reflection to select the methods to call for start and
 * end tags. The methods are stored in a map to avoid having to reflectively
 * find them each time, and benchmarking shows that with this caching the
 * overhead of reflective invocation is negligible.
 * 
 * @author Spencer Schumann
 * 
 */
public abstract class ReflectiveTagHandler implements TagHandler
{
    /**
     * Get a method to invoke for a given start tag name.
     * 
     * @param name
     *            name of start tag
     * @return method to invoke
     */
    public abstract Method getStartTagMethod(String name);

    /**
     * Get a method to invoke for a given end tag name.
     * 
     * @param name
     *            name of end tag
     * @return method to invoke
     */
    public abstract Method getEndTagMethod(String name);

    /**
     * Convenience method to create a map of methods for a class that have a
     * given prefix on their names.
     * 
     * @param cls class to reflectively query
     * @param prefix method name prefix
     * @return map whose keys are method names and values are methods
     */
    public static Map createMethodMap(Class cls, String prefix)
    {
        Method[] methods;
        HashMap map = new HashMap();

        try
        {
            methods = cls.getDeclaredMethods();
        }
        catch (SecurityException e)
        {
            methods = new Method[0];
        }

        for (int i = 0; i < methods.length; i++)
        {
            String name = methods[i].getName();
            if (name.startsWith(prefix))
            {
                name = name.substring(prefix.length()); // remove prefix
                map.put(Parser.toLowerCase(name), methods[i]);
            }
        }

        return map;
    }

    /* (non-Javadoc)
     * @see org.cablelabs.impl.manager.system.html.TagHandler#startTag(java.lang.String, java.util.Map)
     */
    public void startTag(String name, Map attributes)
    {
        Method method = getStartTagMethod(name);
        invoke(method, new Object[] { attributes });
    }

    /* (non-Javadoc)
     * @see org.cablelabs.impl.manager.system.html.TagHandler#endTag(java.lang.String)
     */
    public void endTag(String name)
    {
        Method method = getEndTagMethod(name);
        invoke(method, null);
    }

    /**
     * Convenience method to reflectively invoke a method.
     * 
     * @param method method to invoke
     * @param args arguments for method
     * @return true if invocation succeeds, false otherwise
     */
    private final boolean invoke(Method method, Object[] args)
    {
        if (null != method)
        {
            try
            {
                method.invoke(this, args);
                return true;
            }
            catch (InvocationTargetException e)
            {
            }
            catch (IllegalAccessException e)
            {
            }
        }
        return false;
    }
}
