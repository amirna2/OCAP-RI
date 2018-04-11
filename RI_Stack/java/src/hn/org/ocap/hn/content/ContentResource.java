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

/*
 * Created on 06-Jan-2005 by
 * Dr. Immo Benjes <immo.benjes@philips.com>, Philips Digital Systems Labs, Redhill, UK
 *
 */
package org.ocap.hn.content;

import java.io.IOException;
import java.util.Date;
import javax.tv.locator.Locator;

import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Abstract class representing a media stream/file. Subclasses of this class can
 * implement {@link AudioResource} and/or {@link VideoResource} depending on
 * whether the content represents audio and/or video.
 */
public interface ContentResource
{

    /**
     * Constant for an unknown MIME type.
     */
    public static final String UNKNOWN_MIME_TYPE = "unknown";

    /**
     * Deletes the binary representation of this <code>ContentResource</code>
     * and the <code>ContentResource</code> is removed from any containing
     * <code>ContentEntry</code>. The <code>ContentResource</code> is not
     * valid anymore after this call. This method deletes a local
     * <code>ContentResource</code> only. If the #isLocal
     * method on the containing <code>ContentItem</code>
     * returns false an exception is thrown.  Does not delete the content
     * associated with this <code>ContentResource</code>; see #getLocator.
     *
     * @return True if this ContentResource was deleted, otherwise returns
     *         false.
     *
     * @throws java.lang.SecurityException
     *             if the application is does not have a write
     *             ExtendedFileAccessPermission.
     * @throws java.io.IOException
     *             if the ContentResource is not local.
     *
     */
    public boolean delete() throws IOException;

    /**
     * Gets the ContentItem this resource belongs to.
     *
     * @return The ContentItem parent of this resource or null if this
     *         ContentResource is an independent ContentEntry.
     */
    public ContentItem getContentItem();

    /**
     * Gets the size of the content in bytes or -1 if not known.
     *
     * @return the content size in bytes
     */
    public long getContentSize();

    /**
     * Gets the creation date of the content or NULL if not known.
     *
     * @return The Date the content was created.
     */
    public Date getCreationDate();

    /**
     * Gets an OcapLocator to the content associated with this ContentResource
     * if the content can be located with that Locator type, otherwise returns
     * an implementation specific Locator to the content.
     *
     * @return Locator to the content associated with this entry.
     */
    public Locator getLocator();

    /**
     * Returns the file permissions of a ContentResource.
     *
     * @return the extended file access permissions of the ContentEntry.
     */
    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions();

    /**
     * Returns the protocol which can be used to retrieve the content. The
     * returned String can be a wild card "*". <br>
     * Possible protocols are
     * <ul>
     * <li>"http-get"</li>
     * <li>"rtsp-rtp-udp"</li>
     * <li>"internal"</li>
     * <li>"iec61883"</li>
     * <li>Registered ICANN domain name of vendor</li>
     * </ul>
     *
     * @return String representation of the protocol
     */
    public String getProtocol();

    /**
     * Returns the network on which the content is available. The returned
     * String can be a wild card "*".
     * <table>
     * <tbody>
     * <tr>
     * <td><b>&lt;protocol&gt;</b></td>
     * <td><b>&lt;network&gt;</b></td>
     * </tr>
     * <tr>
     * <td>"http-get"</td>
     * <td>"*"</td>
     * </tr>
     * <tr>
     * <td>"rtsp-rtp-udp"</td>
     * <td>"*"</td>
     * </tr>
     * <tr>
     * <td>"internal"</td>
     * <td>IP address of the device hosting the Connection manager</td>
     * </tr>
     * <tr>
     * <td>"iec61883"</td>
     * <td>GUID of the 1394 bus Isochronous Resource Manager</td>
     * </tr>
     * <tr>
     * <td>ICANN domain</td>
     * <td>Vendor-defined, may be "*"</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @return String describing the network on which the resource is available.
     */
    public String getNetwork();

    /**
     * Returns the content format. The returned String can be a wild card "*".
     * <table>
     * <tbody>
     * <tr>
     * <td><b>&lt;protocol&gt;</b></td>
     * <td><b>&lt;network&gt;</b></td>
     * </tr>
     * <tr>
     * <td>"http-get"</td>
     * <td>MIME-type</td>
     * </tr>
     * <tr>
     * <td>"rtsp-rtp-udp"</td>
     * <td>Name of RTP payload type</td>
     * </tr>
     * <tr>
     * <td>"internal"</td>
     * <td>Vendor-defined, may be "*"</td>
     * </tr>
     * <tr>
     * <td>"iec61883"</td>
     * <td>Name standardised by IEC61883</td>
     * </tr>
     * <tr>
     * <td>ICANN domain</td>
     * <td>Vendor-defined, may be "*"</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @return String describing the content format.
     */
    public String getContentFormat();

    /**
     * Returns properties of the resource. There is a set of defined properties
     * which can be accessed via the methods in {@link AudioResource} and
     * {@link VideoResource}. This method allows for custom or new properties.
     *
     * @param key
     *            The key of the property.
     *
     * @return The value of the property, or null if the key parameter does not
     *         match any property.
     */
    public Object getResourceProperty(String key);

    /**
     * Checks whether the local device has the capabilities to render this
     * content resource. This includes the ability to negotiate media protocol
     * with the host device, the ability of the local device to render this
     * content item's media format, and sufficient access permissions for the
     * calling application.
     *
     * This call does not consider immediate availability of resources required
     * for presentation of this content.
     *
     * @return true if this content is renderable on the local device, false
     *         otherwise.
     */
    public boolean isRenderable();
    
    /**
    * Returns a list of transformed <code>ContentFormat</code> that are
    * available. There may be multiple <code>ContentFormat</code> instances
    * returned that correspond to a single <code>ContentResource</code>
    * in case of HTTP Adaptive content. If the #isLocal method on the
    * containing <code>ContentItem</code> returns false an exception is thrown.
    * 
    * @return Array of <code>ContentFormat</code>.
    */
    public ContentFormat[] getTransformedContentFormats() throws IOException;    
}
