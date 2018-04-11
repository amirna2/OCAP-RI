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

package org.cablelabs.test.autoxlet;

import java.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * A simple parser which extracts integer and string arguments contained within
 * a String array in the form of {argname}={argvalue}
 * 
 * @author Greg Rutz
 */
public class ArgParser
{
    /**
     * Load all of the arguments and values into an internal data structure
     * 
     * @param arg
     *            the raw argument strings
     * @throws java.lang.Exception
     *             if parsing of the arguments fails
     */
    public ArgParser(String arg[]) throws java.lang.Exception
    {
        StringBuffer sb = new StringBuffer();
        _prop = new Properties();

        if (null == arg) throw new Exception("ARG value is null");

        for (int i = 0; i < arg.length; i++)
        {
            sb.append(arg[i]);
            sb.append("\n");
        }

        try
        {
            _prop.load(new ByteArrayInputStream((sb.toString()).getBytes()));
        }
        catch (java.io.IOException ioe)
        {
            System.err.println("Failed to load properties: " + ioe);
            ioe.printStackTrace();
        }
    }

    public ArgParser(InputStream is) throws IOException
    {
        _prop = new Properties();
        _prop.load(is);
    }

    /**
     * Get the String value of a particular argument
     * 
     * @param key
     *            the argument name
     * @return the value of the argument, or null if the argument name was not
     *         found
     */
    public String getStringArg(String key)
    {
        String tmp;

        tmp = _prop.getProperty(key);

        if (null == tmp) return null;

        return tmp.trim();
    }

    /**
     * Get the Integer value of a particular argument
     * 
     * @param key
     *            the argument name
     * @return the value of the argument, or null if the argument name was not
     *         found or if the argument value could not be parsed to an Integer
     */
    public Integer getIntegerArg(String key)
    {
        String tmp;

        tmp = _prop.getProperty(key);
        if (null == tmp)
        {
            return null;
        }

        Integer value = null;

        try
        {
            value = Integer.valueOf(tmp.trim());
        }
        catch (NumberFormatException e)
        {
            return null;
        }

        return value;
    }

    protected Properties _prop;
}
