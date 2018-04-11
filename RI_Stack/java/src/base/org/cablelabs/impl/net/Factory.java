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

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.ContentHandler;
import java.net.ContentHandlerFactory;
import java.net.URLConnection;

import org.havi.ui.HBackgroundImage;

/**
 * Implements support for creating MHP-compliant <code>ContentHandler</code>s
 * given the MIME type.
 * <p>
 * The following MIME types return specific <code>ContentHandler</code> type
 * instances whose {@link ContentHandler#getContent} methods return a specific
 * type. Whenever a simple <code>InputStream</code> will suffice,
 * <code>null</code> will be returned indicating that the default should
 * suffice.
 * <table border="yes">
 * <tr>
 * <th>MIME type</th>
 * <th>Content Type</th>
 * </tr>
 * <tr>
 * <td>image/png</td>
 * <td>java.awt.image.ImageProducer</td>
 * </tr>
 * <tr>
 * <td>image/jpeg</td>
 * <td>java.awt.image.ImageProducer</td>
 * </tr>
 * <tr>
 * <td>image/mpeg</td>
 * <td>org.havi.ui.HBackgroundImage</td>
 * </tr>
 * </table>
 * 
 * Note that MHP doesn't specify "image/gif" should imply a content type of
 * <code>ImageProducer</code>, but we'll assume it.
 * 
 * @author Aaron Kamienski
 */
class Factory implements ContentHandlerFactory
{

    /**
     * Creates a new <code>ContentHandler</code> to read an object from a
     * <code>URLStreamHandler</code>.
     * 
     * @param mimetype
     *            the MIME type for which a content handler is desired.
     * 
     * @return a <code>ContentHandler</code> for the specified MIME type;
     *         <code>null</code> if default support will suffice
     * 
     * @see URLConnection#getContent()
     */
    public ContentHandler createContentHandler(String mimetype)
    {
        if ("image/png".equals(mimetype) || "image/jpeg".equals(mimetype) || "image/jpg".equals(mimetype)
                || "image/gif".equals(mimetype))
        {
            return new ImageProducerContentHandler();
        }
        else if ("image/mpeg".equals(mimetype))
        {
            return new HBackgroundContentHandler();
        }
        else
            return null;
    }
}

/**
 * <code>ContentHandler</code> implementation that returns instances of
 * <code>ImageProducer</code>.
 * 
 * @author Aaron Kamienski
 */
class ImageProducerContentHandler extends ContentHandler
{
    /**
     * Returns an instance of <code>ImageProducer</code>, or <code>null</code>
     * if not possible.
     * 
     * @param urlc
     *            the <code>URLConnection</code>
     * 
     * @see java.net.ContentHandler#getContent(java.net.URLConnection)
     */
    public Object getContent(URLConnection urlc) throws IOException
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        if (tk == null) return null;

        Image image = tk.createImage(urlc.getURL());
        return (image == null) ? null : image.getSource();
    }
}

/**
 * <code>ContentHandler</code> implementation that returns instances of
 * <code>HBackgroundImage</code>.
 * 
 * @author Aaron Kamienski
 */
class HBackgroundContentHandler extends ContentHandler
{
    /**
     * Returns an instance of <code>HBackgroundImage</code>, or
     * <code>null</code> if not possible.
     * 
     * @param urlc
     *            the <code>URLConnection</code>
     * 
     * @see java.net.ContentHandler#getContent(java.net.URLConnection)
     */
    public Object getContent(URLConnection urlc) throws IOException
    {
        return new HBackgroundImage(urlc.getURL());
    }
}
