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
package org.cablelabs.lib.utils;

import java.util.Properties;

import java.awt.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.dvb.ui.DVBColor;

public class ArgParser
{
    public ArgParser(String arg[]) throws IOException
    {
        StringBuffer sb = new StringBuffer();
        _prop = new Properties();

        for (int ii = 0; ii < arg.length; ii++)
        {
            sb.append(arg[ii]);
            sb.append("\n");
        }

        _prop.load(new ByteArrayInputStream((sb.toString()).getBytes()));
    }

    public ArgParser(InputStream is) throws IOException
    {
        _prop = new Properties();
        _prop.load(is);
    }

    public Color getColorArg(String colorStr)
    {
        int val = 0;
        if (colorStr.startsWith("0x"))
        {
            val = (int) Long.parseLong(colorStr.substring(2), 16);
        }
        else
        {
            val = Integer.parseInt(colorStr);
        }
        return new DVBColor(val, true);
    }

    public String getStringArg(String key) throws java.lang.Exception
    {
        String tmp;

        if (null == key) throw new Exception("Key value is null");

        tmp = _prop.getProperty(key);

        if (null == tmp) throw new Exception("String value could not be found; Key: " + key);

        return tmp.trim();

    }

    public int getIntArg(String key) throws java.lang.Exception
    {
        String tmp;

        if (null == key) throw new Exception("Key value is null");

        tmp = _prop.getProperty(key);

        if (null == tmp) throw new Exception("Int value could not be found; Key: " + key);

        return Integer.parseInt(tmp.trim());
    }

    public long getLongArg(String key) throws java.lang.Exception
    {
        String tmp;

        if (null == key) throw new Exception("Key value is null");

        tmp = _prop.getProperty(key);

        if (null == tmp) throw new Exception("Int value could not be found; Key: " + key);

        return Long.parseLong(tmp.trim());
    }

    public int getInt16Arg(String key) throws java.lang.Exception
    {
        if (null == key) throw new Exception("Key value is null");

        String tmp = _prop.getProperty(key);
        if (null != tmp)
        {
            return Integer.parseInt(tmp.trim(), 16);
        }
        else
            throw new Exception("Value for key could not be found for key: " + key);
    }

    public int getIPArg(String key) throws java.lang.Exception
    {
        String tmp;
        String[] aTmp;

        if (null == key) throw new Exception("Key value is null");

        tmp = _prop.getProperty(key);
        if (null != tmp)
        {
            aTmp = this.tokenise(tmp.trim(), '.');
            return ((int) (Integer.parseInt(aTmp[0])) << 24) & 0xFF000000 | ((int) (Integer.parseInt(aTmp[1])) << 16)
                    & 0x00FF0000 | ((int) (Integer.parseInt(aTmp[2])) << 8) & 0x0000FF00
                    | ((int) (Integer.parseInt(aTmp[3]))) & 0x000000FF;
        }
        else
            throw new Exception("Value for key could not be found for key: " + key);
    }

    String[] tokenise(String source, char sep)
    {
        int ntokens = 0, i, j;

        for (i = 0; i < source.length(); i++)
            if (source.charAt(i) == sep) ntokens++;

        String[] tokens = new String[ntokens + 1];
        i = 0;
        ntokens = 0;
        for (j = 0; j < source.length(); j++)
        {
            if (source.charAt(j) == sep)
            {
                tokens[ntokens++] = source.substring(i, j);
                i = j + 1;
            }
        }

        tokens[ntokens] = source.substring(i);
        return tokens;
    }

    /**
     * Testing only
     */
    public static void main(String args[])
    {
        System.out.println("Just used for testing");

        try
        {
            ArgParser opts = new ArgParser(args);
            System.out.println("Arg: foo = " + opts.getStringArg("foo"));
            System.out.println("Arg: Number2 = " + opts.getInt16Arg("Number2"));
            System.out.println("Arg: Number1 = " + opts.getIntArg("Number1"));
            System.out.println("Arg: IP = " + opts.getIPArg("IP"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected Properties _prop;

} // end class GetOpt

