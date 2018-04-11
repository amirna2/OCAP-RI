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

package org.cablelabs.impl.net;

import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Hashtable;

/**
 * Implements support for mapping filenames to MIME types. This implementation
 * considers only the filename extension, although the implementation should
 * potentially consider the content type descriptor carried in the object
 * carousel (see "MHP B.2.3.4").
 * <p>
 * The following extension to MIME types are supported:
 * <table border="yes">
 * <tr>
 * <th>Extension</th>
 * <th>MIME type</th>
 * </tr>
 * <tr>
 * <td>.jpg</td>
 * <td>image/jpeg</td>
 * </tr>
 * <tr>
 * <td>.png</td>
 * <td>image/png</td>
 * </tr>
 * <tr>
 * <td>.gif</td>
 * <td>image/gif</td>
 * </tr>
 * <tr>
 * <td>.mpg</td>
 * <td>image/mpeg</td>
 * </tr>
 * <!--
 * <tr>
 * <td>.mpg</td>
 * <td>video/mpeg</td>
 * </tr>
 * -->
 * <tr>
 * <td>.drip</td>
 * <td>video/dvb.mpeg.drip</td>
 * <tr>
 * <td>.mp1</td>
 * <td>audio/mpeg</td>
 * </tr>
 * <tr>
 * <td>.mp2</td>
 * <td>audio/mpeg</td>
 * </tr>
 * <tr>
 * <td>.mp3</td>
 * <td>audio/mpeg</td>
 * </tr>
 * <tr>
 * <td>.ac3</td>
 * <td>audio/ac3</td>
 * </tr>
 * <tr>
 * <td>.txt</td>
 * <td>text/dvb.utf8</td>
 * </tr>
 * <!--
 * <tr>
 * <td>.sub</td>
 * <td>image/dvb.subtitle</td>
 * </tr>
 * -->
 * <tr>
 * <td>.sub</td>
 * <td>text/dvb.subtitle</td>
 * </tr>
 * <tr>
 * <td>.tlx</td>
 * <td>text/dvb.teletext</td>
 * </tr>
 * <tr>
 * <td>.pfr</td>
 * <td>application/dvb.pfr</td>
 * </tr>
 * <tr>
 * <td>.class</td>
 * <td>application/dvbj</td>
 * </tr>
 * <tr>
 * <td>.svc</td>
 * <td>multipart/dvb.service</td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 */
class MimeTable implements FileNameMap
{
    /**
     * Initializes an instance of <code>MimeTable</code> per MHP.
     * 
     * @param base
     *            the original FileNameMap that can be falled back on if
     *            necessary
     */
    public MimeTable(FileNameMap base)
    {
        this.base = base;

        map = new Hashtable();
        map.put("jpg", "image/jpeg");
        map.put("png", "image/png");
        map.put("gif", "image/gif");
        map.put("mpg", "image/mpeg");
        map.put("drip", "video/dvb.mpeg.drip");
        map.put("mp1", "audio/mpeg");
        map.put("mp2", "audio/mpeg");
        map.put("mp3", "audio/mpeg");
        map.put("aif", "audio/x-aiff");
        map.put("aiff", "audio/x-aiff");
        map.put("aifc", "audio/x-aiff");
        map.put("ac3", "audio/ac3");
        map.put("txt", "text/dvb.utf8");
        map.put("sub", "text/dvb.subtitle");
        map.put("tlx", "text/dvb.teletext");
        map.put("pfr", "application/dvb.pfr");
        map.put("class", "application/dvbj");
        map.put("svc", "multipart/dvb.service");
    }

    /**
     * Initializes an instance of <code>MimeTable</code> per MHP.
     */
    public MimeTable()
    {
        // I'm not completely confident that this is the behavior that we want.
        // I.e., I'm not sure base should be the currently installed FileNameMap
        // Would it be better to only support the MHP-required mappings?
        // Or is it better to simply override the defaults per MHP?
        this(URLConnection.getFileNameMap());
    }

    /**
     * Gets the MIME type for the specified file name per MHP.
     * 
     * @param filename
     *            the name of the file
     * @return MIME type for the file or <code>null</code> if unknown
     */
    public String getContentTypeFor(String filename)
    {
        // Locate the filename extension
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx + 1 >= filename.length() - 1) return null;

        // Lookup MIME type in table
        String ext = filename.substring(idx + 1).toLowerCase();
        String type = (String) map.get(ext);

        // Use fallback, if necessary
        if (type == null && base != null) type = base.getContentTypeFor(filename);

        return type;
    }

    /**
     * Mapping of filename extensions to MIME types.
     */
    private Hashtable map;

    /**
     * The original <code>FileNameMap</code> that can be falled back on if
     * necessary.
     */
    private FileNameMap base;
}
