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

package javax.media.protocol;

/**
 * A <CODE>ContentDescriptor</CODE> identifies media data containers.
 * 
 * @see SourceStream
 * @version 1.12, 05/10/24.
 */

public class ContentDescriptor
{

    static public final String CONTENT_UNKNOWN = "UnknownContent";

    static private final int MAX_MIMETYPE_LENGTH = 255; // based on rfc 4288

    /**
     * Obtain a string that represents the content-name for this descriptor. It
     * is identical to the string passed to the constructor.
     * 
     * @return The content-type name.
     */
    public String getContentType()
    {
        return typeName;
    }

    protected String typeName;

    /**
     * Create a content descriptor with the specified name.
     * <p>
     * To create a <CODE>ContentDescriptor</CODE> from a MIME type, use the
     * <code>mimeTypeToPackageName</code> static member.
     * 
     * @param cdName
     *            The name of the content-type.
     */
    public ContentDescriptor(String cdName)
    {
        typeName = cdName;
    }

    /**
     * Map a MIME content-type to an equivalent string of class-name components.
     * <p>
     * The MIME type is mapped to a string by:
     * <ol>
     * <li>Replacing all slashes with a period.
     * <li>Converting all alphabetic characters to lower case.
     * <li>Converting all non-alpha-numeric characters other than periods to
     * underscores (_).
     * </ol>
     * <p>
     * For example, "text/html" would be converted to "text.html"
     * 
     * @param mimeType
     *            The MIME type to map to a string.
     */
    static final public String mimeTypeToPackageName(String mimeType)
    {

        // All to lower case ...
        mimeType = mimeType.toLowerCase();

        // ... run through each char and convert
        // '/' -> '.'
        // !([A-Za-z0--9]) -> '_'
        int len = mimeType.length(); // length of mimetype
        int j = 0; // indexer for the new array
        boolean newdir = true;
        char mt[] = new char[len]; // char array of mimetype
        char nm[] = new char[MAX_MIMETYPE_LENGTH]; // char array of package name
        mimeType.getChars(0, len, mt, 0);
        for (int i = 0; i < len; i++)
        {
            char c = mt[i];
            if (c == '/' || c == '.')
            {
                nm[j] = '.';
                newdir = true;
            }
            else if (('0' <= c && c <= '9') && newdir == true)
            {
                nm[j] = '_';
                j++;
                nm[j] = c;
                newdir = false;
            }
            else if (!('A' <= c && c <= 'Z' || 'a' <= c && c <= 'z' || '0' <= c && c <= '9'))
            {
                nm[j] = '_';
                newdir = false;
            }
            else
            {
                nm[j] = c;
                newdir = false;
            }
            j++;
        }
        String pkgName = new String(nm);
        return pkgName.trim();
    }

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
