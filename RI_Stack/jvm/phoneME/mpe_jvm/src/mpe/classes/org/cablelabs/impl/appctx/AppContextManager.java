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

package org.cablelabs.impl.appctx;

import com.sun.cdc.config.DynamicProperties;
import com.sun.cdc.config.PropertyProvider;

/**
 * This class must be implemented for each VM integrated with the stack. The
 * <code>AppContextManager</code> is responsible for managing the current
 * <code>AppContextHandler</code>. All app-context specific VM operations must
 * go through the registered <code>AppContextHandler</code>. At this time, the
 * app-context specific operations are:
 * <p>
 * <ul>
 * <li>Retrieval of Java system properties
 * </ul>
 * 
 * @author Greg Rutz
 */
public class AppContextManager
{
    /**
     * Registers the <code>AppContextHandler</code> that should be used by the
     * VM to implement app-specific operations
     * 
     * @param handler
     *            the handler to register
     */
    public static void registerAppContextHandler(AppContextHandler handler)
    {
        s_handler = handler;

        // Create a dynamic property provider that will return the value
        // returned by our handler
        PropertyProvider appPropProvider = new PropertyProvider()
        {

            public String getValue(String key, boolean fromCache)
            {
                return s_handler.getProperty(key);
            }

            public boolean cacheProperties()
            {
                return false;
            }
        };

        // Register our provider for properties that specific to each
        // application
        DynamicProperties.put("user.dir", appPropProvider);
        DynamicProperties.put("ocap.j.location", appPropProvider);
        DynamicProperties.put("java.io.tmpdir", appPropProvider);
    }

    /**
     * Returns the current <code>AppContextHandler</code>
     * 
     * @return the current <code>AppContextHandler</code>
     */
    public static AppContextHandler getHandler()
    {
        return s_handler;
    }

    // The current registered handler
    private static AppContextHandler s_handler = new AppContextHandler()
    {

        // Default implementation
        public String getProperty(String name)
        {
            return null;
        }

    };
}