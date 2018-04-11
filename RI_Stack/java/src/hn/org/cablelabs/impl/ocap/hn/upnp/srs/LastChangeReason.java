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

package org.cablelabs.impl.ocap.hn.upnp.srs;

/**
 * LastChangeReason - The reason for the <code>LastChange</code> event.
 *
 * @author Ben Foran (Flashlight Engineering and Consulting)
 *
 * @version $Revision$
 *
 * @see {@link org.cablelabs.impl.ocap.hn.upnp.srs.LastChange}
 */
class LastChangeReason
{
    public final static String RECORD_SCHEDULE_CREATED = "RecordScheduleCreated";

    public final static String RECORD_SCHEDULE_MODIFIED = "RecordScheduleModified";

    public final static String RECORD_SCHEDULE_DELETED = "RecordScheduleDeleted";

    public final static String RECORD_TASK_CREATED = "RecordTaskCreated";

    public final static String RECORD_TASK_MODIFIED = "RecordTaskModified";

    public final static String RECORD_TASK_DELETED = "RecordTaskDeleted";

    // vendor defined reasons ...

    private String reason = null;

    private long updateID = 0;

    private String objectID = null;

    /**
     * LastChangeReason - takes a string value representing the reason.
     *
     * @param reason
     *            - the reason that caused the <code>LastChange</code> event.
     */
    LastChangeReason(String reason, String objectID)
    {
        if (!reason.equals(RECORD_SCHEDULE_CREATED) && !reason.equals(RECORD_SCHEDULE_MODIFIED)
                && !reason.equals(RECORD_SCHEDULE_DELETED) && !reason.equals(RECORD_TASK_CREATED)
                && !reason.equals(RECORD_TASK_MODIFIED) && !reason.equals(RECORD_TASK_DELETED))
        {
            throw new IllegalArgumentException("LastChange reason is not valid!");
        }

        this.reason = reason;
        this.objectID = objectID;

    }

    /**
     * getReason - returns the reason for the <code>LastChange</code> event.
     *
     * @return a string value that represents the reason for the
     *         <code>LastChange</code> event.
     */
    String getReason()
    {
        return this.reason;
    }

    /**
     * getObjectID - returns the object ID for the <code>LastChange</code>
     * event.
     *
     * @return a string value that represents the object ID for the
     *         <code>LastChange</code> event.
     */
    String getObjectID()
    {
        return this.objectID;
    }

    /**
     * setUpdateID - sets the value for the updateID variable. This must be
     * incremented so that each <code>LastChange</code> event has a different
     * ID.
     *
     * @param ID
     *            - the value to set the updateID to.
     */
    void setUpdateID(long ID)
    {
        this.updateID = ID;
    }

    /**
     * toString - returns the value of this <code>LastChangeReason</code>
     * instance as an xml string.
     *
     * @return a string representation of this object.
     */
    public String toString()
    {
        return "LastChangeReason\n" + this.toXmlString();
    }

    /**
     * toSmlString - creates an xml document for this
     * <code>LastChangeReason</code> with the values contained in this instance.
     *
     * @return an xml string for this <code>LastChangeReason</code>.
     */
    String toXmlString()
    {
        String xml = "";

        // An example xml string is: <RecordTaskCreated updateID="1" objectID="s001">
        xml += "<" + reason + " updateID=\"" + updateID + "\"" + " objectID=\"" + objectID + "\">";

        // Then the xml string is appended with the closing tag, i.e. </RecordTaskCreated>
        xml += "</" + reason + ">";

        return xml;
    }
}
