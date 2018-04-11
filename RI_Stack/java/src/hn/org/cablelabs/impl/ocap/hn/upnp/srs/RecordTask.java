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

import java.util.Map;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.recording.RecordScheduleDirectManual;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.srs.ScheduledRecordingService;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.ocap.dvr.OcapRecordingRequest;

/**
 * This class represents a UPnP ScheduledRecordingService RecordSTask. It is
 * tightly coupled with the local representation of an
 * org.ocap.hn.recording.RecordingContentItem.
 * 
 * It is only accessible by the org.cablelabs.impl.ocap.hn.upnp.srs package.
 * 
 * @author Dan Woodard
 * 
 */
class RecordTask
{
    /**
     * State string definitions
     */
    private static final String IDLE_STATE = "IDLE";

    private static final String DONE_STATE = "DONE";

    private static final String IDLE_READY_STATE = "IDLE.READY";

    private static final Logger log = Logger.getLogger(RecordTask.class);
    
    private static final String CLASS_PROPERTY = "OBJECT.RECORDTASK";


    /**
     * Creates a RecordTask instance that is associated with a RecordSchedule
     * that this instance is contained in and the RecordingContentItem that is
     * stored in the CDS.
     * 
     * @param recordSchedule
     *            The containing RecordSchedule instance.
     * @param item
     *            The RecordingContentItem instance that is stored in the CDS.
     */
    RecordTask(RecordSchedule recordSchedule, RecordingContentItemLocal item)
    {
        // create a unique ObjectID
        RecordTask.objectIDint++;

        this.objectID = "rt" + Integer.toString(RecordTask.objectIDint);

        // save RecordSchedule that this instance is contained in.
        this.recordSchedule = recordSchedule;

        // save item that is associated with this instance.
        this.recordingContentItemLocal = item;

    }

    /**
     * Returns the unique ObjectID for this instance
     * 
     * @return upnp:srsRecordTaskID value
     */
    String getObjectID()
    {
        return this.objectID;
    }

    RecordingContentItemLocal getRecordingContentItem()
    {
        return this.recordingContentItemLocal;
    }

    RecordSchedule getRecordSchedule()
    {
        return this.recordSchedule;
    }

    boolean isIDLE()
    {
        return this.state.startsWith(IDLE_STATE);
    }

    boolean isDONE()
    {
        return this.state.startsWith(DONE_STATE);
    }

    /**
     * Returns the CDS entry ID that represents the RecordingContentItem in the
     * CDS.
     */
    String getScheduledCDSEntryID()
    {
        if (this.recordingContentItemLocal != null)
        {
            return this.recordingContentItemLocal.getID();
        }

        return "";
    }

    /**
     * Get the XML fragment which is included in UPnp BrowseRecordTask 
     * action.
     * 
     * @param filteredProperties    list of properties to include in response
     * @return  XML <item> which describes record task
     */
    protected String getBrowseItem(String filteredProperties[])
    {
        // Get a copy of the properties associated with parent record schedule
        Map props = getRecordSchedule().getPropertiesCopy();
        
        // Update the properties with necessary property values
        props.put(UPnPConstants.QN_SRS_CLASS, CLASS_PROPERTY);                    
        props.put(UPnPConstants.QN_SRS_ID_ATTR, objectID);            
        props.put(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID, recordSchedule.getObjectID()); 
        
        String scheduleChannelID = (String)props.get(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID);
        props.put(UPnPConstants.QN_SRS_TASK_CHANNEL_ID, scheduleChannelID); 

        String scheduleChannelIDType = (String)props.get(UPnPConstants.QN_SRS_SCHEDULED_CHANNEL_ID_TYPE_ATTR);
        props.put(UPnPConstants.QN_SRS_TASK_CHANNEL_ID_TYPE_ATTR, scheduleChannelIDType); 

        String startDateTime = (String)props.get(UPnPConstants.QN_SRS_SCHEDULED_START_DATE_TIME);
        props.put(UPnPConstants.QN_SRS_TASK_START_DATE_TIME, startDateTime); 
                
        String duration = (String)props.get(UPnPConstants.QN_SRS_SCHEDULED_DURATION);
        props.put(UPnPConstants.QN_SRS_TASK_DURATION, duration); 

        props.put(UPnPConstants.QN_SRS_TASK_STATE, "DONE.FULL"); 

        props.put(UPnPConstants.QN_SRS_RECORD_QUALITY, "SD"); 
        props.put(UPnPConstants.QN_SRS_RECORD_QUALITY_TYPE_ATTR, "DEFAULT");       
        props.put(UPnPConstants.QN_SRS_FATAL_ERROR, "0");
        props.put(UPnPConstants.QN_SRS_BITS_RECORDED, "0"); 
        props.put(UPnPConstants.QN_SRS_BITS_MISSING, "0");  
        props.put(UPnPConstants.QN_SRS_INFO_LIST, ""); 
        props.put(UPnPConstants.QN_SRS_CURRENT_ERRORS, "");
        props.put(UPnPConstants.QN_SRS_PENDING_ERRORS, ""); 
        props.put(UPnPConstants.QN_SRS_RECORDING, "0"); 
        props.put(UPnPConstants.QN_SRS_PHASE, "DONE");
        props.put(UPnPConstants.QN_SRS_ERROR_HISTORY, "");

        return getRecordSchedule().getBrowseItem(filteredProperties, props);
    }

    /**
     * Generate LastChange event
     */
    private void stateChanged()
    {
        // leave this recording if it's done partial or full (can be streamed)
        if (this.state.equals("DONE.EMPTY"))
        {
            // remove this RecordTask from RecordSchedule
            this.recordSchedule.removeRecordTask(this);
        }

        LastChangeReason lcr = new LastChangeReason(LastChangeReason.RECORD_TASK_MODIFIED, this.objectID);
        ((ScheduledRecordingService) MediaServer.getInstance().getSRS()).notifyChange(lcr);
    }

    // RecordSchedule instance this instance is contained in.
    private RecordSchedule recordSchedule;

    // association between this RecordTask and the RecordingContentItem
    private RecordingContentItemLocal recordingContentItemLocal;

    // incremented each new RecordTask to create the unique ObjectID
    private static int objectIDint = 0;

    // The ObjectID of this instance
    private String objectID = ""; // upnp:srsRecordTaskID

    private String state = IDLE_READY_STATE;
}
