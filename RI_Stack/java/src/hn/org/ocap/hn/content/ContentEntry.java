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

package org.ocap.hn.content;

import java.io.IOException;
import java.util.Date;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * This interface represents a basic content entry. Each ContentEntry instance
 * can only be contained in one ContentContainer and the implementation SHALL
 * create a new ContentEntry for equal entries placed in multiple
 * ContentContainer instances.
 */
public interface ContentEntry
{

    /**
     * Returns the ID of this ContentEntry. The format of this string ID is
     * implementation and protocol mapping dependent.
     *
     * @return The ID of content entry.
     */
    public String getID();

    /**
     * Gets the server where this ContentEntry is located.
     *
     * @return The server housing this container.
     */
    public ContentServerNetModule getServer();

    /**
     * Deletes this ContentEntry. This is a local delete only. If the #isLocal
     * method returns false, this method SHALL throw an exception. This method
     * does not delete any content associated with this content entry.
     *
     * @return True if the ContentEntry was deleted, otherwise returns false.
     *
     * @throws SecurityException
     *             if the calling application does not have write
     *             ExtendedFileAccessPermission for this entry.
     * @throws IOException
     *             if the entry is not local.
     */
    public boolean deleteEntry() throws IOException;

    /**
     * Returns the {@link ContentContainer} this ContentEntry belongs to.
     *
     * This method SHALL return null if this ContentEntry represents a root
     * container.
     *
     * If it is determined that this ContentEntry has a parent container, but
     * the implementation does not have sufficient local cached information to
     * construct the ContentContainer, this method SHALL throw an IOException.
     *
     * @return The parent ContentContainer.
     *
     * @throws IOException
     *             if the implementation does not have sufficient local cached
     *             information to construct the parent ContentContainer
     */
    public ContentContainer getEntryParent() throws IOException;

    /**
     * Returns the ID of {@link ContentContainer} this ContentEntry belongs to.
     *
     * This method SHALL return "-1" if this ContentEntry represents a root
     * container. This method SHALL return null if the parent ID is unknown.
     *
     * @see org.ocap.hn.content.ContentEntry#getID
     * @see org.ocap.hn.content.ContentEntry#getEntryParent
     *
     * @return the ID of this entry's parent container
     */
    public String getParentID();

    /**
     * Gets the size of the content associated with this ContentEntry..
     *
     * @return The content size in bytes or -1 if unknown.
     */
    public long getContentSize();

    /**
     * Gets the creation date of the content associated with this ContentEntry.
     *
     * @return The Date the content was created or null if unknown.
     */
    public Date getCreationDate();

    /**
     * Gets the file permissions of this ContentEntry, or null if unknown.
     *
     * @return The extended file access permissions of this ContentEntry or null
     *         if unknown.
     */
    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions();

    /**
     * Gets the metadata for this ContentEntry.
     *
     * @return Root MetadataNode.
     */
    public MetadataNode getRootMetadataNode();

    /**
     * Returns true if this content entry is on the local device, false if it is
     * hosted by another device on the network.
     *
     * @return true if the content is local, false otherwise
     */
    public boolean isLocal();
}
