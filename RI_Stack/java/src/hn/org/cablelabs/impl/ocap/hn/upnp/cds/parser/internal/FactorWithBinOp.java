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

package org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal;

import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.ocap.hn.content.ContentEntry;

public class FactorWithBinOp implements Factor
{
    private final String property;

    private final int binOp;

    private final String string;

    public FactorWithBinOp(String property, int binOp, String string)
    {
        this.property = DIDLLite.prefixed(property);
        this.binOp = binOp;
        this.string = string;
    }

    public boolean isSatisfiedBy(ContentEntry entry)
    {
        if (entry == null || entry.getRootMetadataNode() == null
                || entry.getRootMetadataNode().getMetadata(property) == null)
        {
            return false;
        }

        Object value = entry.getRootMetadataNode().getMetadata(property);
        
        switch (binOp)
        {
            case Scanner.EQUAL:
                if (value instanceof String)
                {
                    String s = getNumericString(string);
                    String v = getNumericString((String)value);
                    if(v != null && s != null)
                    {
                        int si = Integer.parseInt(s);
                        int vi = Integer.parseInt(v);
                        
                        return vi == si;
                    }
                    return ((String) value).toLowerCase().equals(string.toLowerCase());
                }
                return false;
            case Scanner.NOT_EQUAL:
                if (value instanceof String)
                {
                    String s = getNumericString(string);
                    String v = getNumericString((String)value);
                    if(v != null && s != null)
                    {
                        int si = Integer.parseInt(s);
                        int vi = Integer.parseInt(v);
                        
                        return vi != si;
                    }                    
                    return !((String) value).toLowerCase().equals(string.toLowerCase());
                }
                return true;
            case Scanner.LESS_OR_EQUAL:
                if (value instanceof String)
                {
                    String s = getNumericString(string);
                    String v = getNumericString((String)value);
                    if(v != null && s != null)
                    {
                        int si = Integer.parseInt(s);
                        int vi = Integer.parseInt(v);
                        
                        return vi <= si;
                    }
                    return ((String) value).toLowerCase().compareTo(string.toLowerCase()) < 1;
                }
                return false;
            case Scanner.LESS:
                if (value instanceof String)
                {
                    String s = getNumericString(string);
                    String v = getNumericString((String)value);
                    if(v != null && s != null)
                    {
                        int si = Integer.parseInt(s);
                        int vi = Integer.parseInt(v);
                        
                        return vi < si;
                    }                    
                    return ((String) value).toLowerCase().compareTo(string.toLowerCase()) < 0;
                }
                return false;
            case Scanner.GREATER_OR_EQUAL:
                if (value instanceof String)
                {
                    String s = getNumericString(string);
                    String v = getNumericString((String)value);
                    if(v != null && s != null)
                    {
                        int si = Integer.parseInt(s);
                        int vi = Integer.parseInt(v);
                        
                        return vi >= si;
                    }                    
                    return ((String) value).toLowerCase().compareTo(string.toLowerCase()) > -1;
                }
                return false;
            case Scanner.GREATER:
                if (value instanceof String)
                {
                    String s = getNumericString(string);
                    String v = getNumericString((String)value);
                    if(v != null && s != null)
                    {
                        int si = Integer.parseInt(s);
                        int vi = Integer.parseInt(v);
                        
                        return vi > si;
                    }                    
                    return ((String) value).toLowerCase().compareTo(string.toLowerCase()) > 0;
                }
                return false;
            case Scanner.CONTAINS:
                if (value instanceof String)
                {
                    return ((String) value).toLowerCase().indexOf(string.toLowerCase()) > -1;
                }
                return false;
            case Scanner.DERIVED_FROM:
                if (value instanceof String)
                {
                    return ((String) value).toLowerCase().startsWith(string.toLowerCase());
                }
                return false;
            case Scanner.DOES_NOT_CONTAIN:
                if (value instanceof String)
                {
                    return ((String) value).toLowerCase().indexOf(string.toLowerCase()) == -1;
                }
                return true;
            default:
                throw new IllegalStateException();
        }
    }

    public String toString()
    {
        return "(property " + property + " " + Scanner.prettyNameBinOp(binOp) + " string " + string + ")";
    }
    
    /**
     *  @param String to be checked to see if it is a valid UPnP numeric value.
     *   
     *  @return String that can be parsed as an int or null if it can not be parsed as int.
     */
    private String getNumericString(String str)
    {
        // Plus is allowed, but strip it off for parsing.
        String value = str.startsWith("+") ? str.substring(1, str.length()) : str;
        
        // Minus is allowed, but don't check to see if that character is a digit.
        int startPos = value.startsWith("-") ? 1 : 0;
        
        for (int i = startPos; i < value.length(); i++)
        {
            if (!Character.isDigit(value.charAt(i)))
            {        
                return null;
            }
        }
        
        return value;
    }
}
