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

package org.ocap.hn;

/**
 * Event which will be sent to registered ContentServerListeners when
 * ContentEntrys have been added, changed or removed.
 */
public class ContentServerEvent extends java.util.EventObject
{
    /**
     * Event ID indicating that content got added to a ContentServerNetModule.
     * This event SHALL NOT guarantee that any content items or associated
     * metadata have been communicated to the local device. Applications should
     * utilize the content browsing and searching APIs to retrieve any added
     * content items.
     * 
     * @see org.ocap.hn.ContentServerNetModule
     */
    public static final int CONTENT_ADDED = 0;

    /**
     * Event ID indicating that content got removed from a
     * ContentServerNetModule
     */
    public static final int CONTENT_REMOVED = 1;

    /**
     * Event ID indicating that metadata associated with content has been
     * updated. This event SHALL NOT guarantee that and changes to content items
     * or associated metadata have been communicated to the local device.
     * Applications should utilize the content browsing and searching APIs to
     * retrieve any updated metadata.
     * 
     * @see org.ocap.hn.ContentServerNetModule
     */
    public static final int CONTENT_CHANGED = 2;

    /**
     * Number indicating the type of this event
     */
    private int m_evt;
    
    /**
     * Array of strings which represent the content ids which are
     * associated with this event type
     */
    private String[] m_content;
    
    /**
     * Creates a new ContentServerEvent with the given source object, the
     * ContentItem involved and an event ID indicating whether the content got
     * added or removed.
     * 
     * @param source
     *            The source of this event. This must be a
     *            ContentServerNetModule.
     * @param content
     *            the IDs of the ContentEntrys involved.
     * @param evt
     *            the Event ID, either CONTENT_ADDED,CONTENT_REMOVED or
     *            CONTENT_CHANGED.
     */
    public ContentServerEvent(Object source, String content[], int evt)
    {
        super(source);
        m_evt = evt;
        m_content = content;
    }

    /**
     * Returns the IDs associated with the ContentEntrys involved in this event.
     * 
     * @return the string IDs of the entries involved.
     */
    public String[] getContent()
    {
        return m_content;
    }

    /**
     * Returns the ContentServerNetModule. This is the source object of the
     * event.
     * 
     * @return the ContentServerNetModule containing the ContentItem that was
     *         added/removed/changed
     */
    public ContentServerNetModule getContentServerNetModule()
    {
        return (ContentServerNetModule)source;
    }

    /**
     * Gets the event ID for this event. Valid values are CONTENT_ADDED,
     * CONTENT_CHANGED and CONTENT_REMOVED
     * 
     * @return the ID for this event
     */
    public int getEventID()
    {
        return m_evt;
    }
}
