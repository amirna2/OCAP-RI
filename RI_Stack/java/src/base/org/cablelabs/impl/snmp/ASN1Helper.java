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

package org.cablelabs.impl.snmp;

import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.diagnostics.MIBManagerImpl;
import org.cablelabs.impl.util.SystemEventUtil;

public abstract class ASN1Helper
{
    private static final Logger log = Logger.getLogger(ASN1Helper.class);

    /**
     * The String charset used to encode and decode the byte array
     */
    public static final String CONVERSION_CHARSET = "ISO-8859-1"; // from String class

    /**
     *
     * @param string
     *            string in for of data (no type information)
     * @return converted byte[] using CONVERSION_CHARSET
     */
    public static byte[] getDataFromString(String dataStr)
    {
        try
        {
            return dataStr.getBytes(CONVERSION_CHARSET);
        }
        catch (UnsupportedEncodingException e)
        {
            SystemEventUtil.logCatastrophicError(e);
            return null;
        }
    }

    public static String getDataAsString(byte[] data)
    {
        try
        {
            return new String(data, 0, data.length, CONVERSION_CHARSET);
        }
        catch (UnsupportedEncodingException e)
        {
            SystemEventUtil.logCatastrophicError(e);
            return null;
        }
    }

    /**
     * If logging is enabled this method will convert a byte array into a {@link String}
     * displaying the bytes in hexadecimal notation for debug purposes.<br><br>
     *
     * example output: "[0x01][0x02][0x03][0x04]"<br><br>
     *
     * If logging is not enabled this method will return an empty {@link String}: ""
     *
     * @param bytes byte array to convert
     * @return {@link String} showing hex values
     */
    public static String dataToString(byte[] bytes)
    {
        String s = "";

        for (int i=0; i<bytes.length; i++)
        {
            s += "[";
            s += "0x";
            s += Integer.toString( ( bytes[i] & 0xff ) + 0x100, 16).substring( 1 );
            s += "]";
        }

        return s;
    }
}
