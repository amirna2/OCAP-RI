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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordScheduleDirectManual;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.cablelabs.impl.ocap.hn.upnp.srs.ScheduledRecordingService;


/**
 * This class represents a UPnP ScheduledRecordingService RecordSchedule. It is
 * tightly coupled with the local representation of an
 * org.ocap.hn.recording.NetRecordingEntry:
 * org.cablelabs.impl.ocap.hn.upnp.srs.NetRecordingEntryLocal.
 * 
 * It is only accessible by the org.cablelabs.impl.ocap.hn.upnp.srs package.
 * 
 * @author Dan Woodard
 * 
 */
class RecordSchedule
{
    public static final String OPERATIONAL_STATE = "OPERATIONAL";

    public static final String COMPLETED_STATE = "COMPLETED";

    public static final String ERROR_STATE = "ERROR";

    private static final Logger log = Logger.getLogger(RecordSchedule.class);

    /**
     * Create a new RecordSchedule that is not associated with a
     * NetRecordingEntry.
     */
    RecordSchedule()
    {
        // create a unique ObjectID
        objectIDint++;

        this.objectID = "rs" + Integer.toString(objectIDint);
    }

    /**
     * Create a new RecordSchedule and associate it with a NetRecordingEntry.
     * 
     * @param entry
     *            NetRecordingEntry to make the association with
     */
    RecordSchedule(NetRecordingEntryLocal entry)
    {
        this();

        // save association between this instance and the entry
        this.netRecordingEntry = entry;
    }

    /**
     * Creates a new RecordTask and adds it to this RecordSchedule
     * 
     * @return the new RecordTask
     */
    RecordTask createRecordTask(RecordingContentItemLocal item)
    {
        RecordTask recordTask = new RecordTask(this, item);

        this.recordTaskList.add(recordTask);

        this.currentRecordTaskCount++;

        ((ScheduledRecordingService) MediaServer.getInstance().getSRS()).notifyChange(new LastChangeReason(LastChangeReason.RECORD_TASK_CREATED, recordTask.getObjectID()));

        return recordTask;
    }

    /**
     * Removes the RecordTask from this RecordSchedule
     * 
     * @param recordTask
     *            The RecordTask to remove.
     * @return true if successful, false if not.
     */
    boolean removeRecordTask(RecordTask recordTask)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeRecordTask: " + recordTask);
        }
        if (this.recordTaskList.remove(recordTask))
        {
            if (this.recordTaskList.size() == 0)
            {
                // no more RecordTask means that the RecordSchedule is complete
                // and can be removed
                ((ScheduledRecordingService) MediaServer.getInstance().getSRS()).removeRecordSchedule(this);
            }

            ((ScheduledRecordingService) MediaServer.getInstance().getSRS()).notifyChange(new LastChangeReason(LastChangeReason.RECORD_TASK_DELETED, recordTask.getObjectID()));

            return true;
        }
        return false;
    }

    /***
     * Check if this RecordSchedule contains any RecordTasks
     * 
     * @return true if there is one or more RecordTasks, false if none
     */
    boolean hasRecordTasks()
    {
        return this.recordTaskList.size() > 0;
    }

    boolean allRecordTask_IDLEorDONE()
    {
        Iterator iter = this.recordTaskList.iterator();

        while (iter.hasNext())
        {
            RecordTask recordTask = (RecordTask) iter.next();

            if ((recordTask.isDONE() || recordTask.isIDLE()) == false)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the associated NetRecordingEntryLocal instance if any.
     * 
     * @return null if no NetRecordingEntryLocal is associated.
     */
    NetRecordingEntryLocal getNetRecordingEntry()
    {
        return this.netRecordingEntry;
    }

    /**
     * Returns the unique ObjectID for this instance
     * 
     * @return upnp RecordSchedule ObjectID value
     */
    String getObjectID()
    {
        return this.objectID;
    }

    /**
     * Returns the RecordTask with UPnP objectID or null if it doesn't exist.
     */
    RecordTask getRecordTask(String objectID)
    {
        RecordTask recordTask = null;

        Iterator iter = this.recordTaskList.iterator();

        while (iter.hasNext())
        {
            RecordTask rt = (RecordTask) iter.next();
            if (rt.getObjectID().equals(objectID))
            {
                recordTask = rt;
                break;
            }
        }
        return recordTask;
    }

    /**
     * Returns list of record tasks associated with this record schedule.
     * 
     * @param taskList  list containing associated RecordTasks
     */
    void getRecordTasks(ArrayList taskList)
    {
        Iterator iter = this.recordTaskList.iterator();
        while (iter.hasNext())
        {
            taskList.add(iter.next());
        }
    }

    /**
     * Returns the RecordTask that contains a RecordingContentItem that has been
     * stored in the CDS with the input objectID.
     */
    RecordTask getRecordTaskByCDSID(String objectID)
    {
        RecordTask recordTask = null;

        Iterator iter = this.recordTaskList.iterator();

        while (iter.hasNext())
        {
            recordTask = (RecordTask) iter.next();

            if (recordTask.getScheduledCDSEntryID().equals(objectID))
            {
                break;
            }
            else
            {
                recordTask = null;
            }
        }
        return recordTask;
    }

    private String cdsReference;

    String getCdsReference()
    {
        return cdsReference;
    }

    void setCdsReference(String cdsReference)
    {
        this.cdsReference = cdsReference;
    }

    private String scheduledCDSEntryID;

    String getScheduledCDSEntryID()
    {
        return scheduledCDSEntryID;
    }

    void setScheduledCDSEntryID(String scheduledCDSEntryID)
    {
        this.scheduledCDSEntryID = scheduledCDSEntryID;
    }

    /**
     * Returns the XML A_ARG_TYPE_RecordSchedule state variable for this
     * RecordSchedule.
     * 
     * @return A_ARG_TYPE_RecordSchedule state variable
     */
    String get_A_ARG_TYPE_RecordSchedule()
    {
        RecordScheduleDirectManual recordScheduleDirectManual = buildRecordScheduleDirectManual();
        
        StringBuffer sb = new StringBuffer("<srs ");
        sb.append("xmlns=\"urn:schemas-upnp-org:av:srs\"\n");
        sb.append("xmlns:ocap=\"urn:schemas-cablelabs-org:metadata-1-0/\"\n");
        sb.append("xmlns:ocapApp=\"urn:schemas-opencable-com:ocap-application\"\n");
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("xsi:schemaLocation=\"urn:schemas-upnp-org:av:srs http://www.upnp.org/schemas/av/srs-v1-20060531.xsd\">\n");
        sb.append(recordScheduleDirectManual.toXMLString(null));
        sb.append("</srs>");

        return sb.toString();
    }

    /**
     * Retrieve the XML which represents an item node which is returned in response to 
     * an BrowseRecordSchedule() UPnP action.
     * 
     * @param filteredProperties    build the item node using this as a filter which indicates
     *                              which properties to include
     * @return  item node XML fragment string
     */
    protected String getBrowseItem(String filteredProperties[], Map properties)
    {
        String result;

        if (properties != null)
        {
            result = RecordScheduleDirectManual.toXMLString(filteredProperties, properties);
        }
        else
        {
            RecordScheduleDirectManual recordScheduleDirectManual = buildRecordScheduleDirectManual();
            result = recordScheduleDirectManual.toXMLString(filteredProperties);
        }

        return result;
    }

    /**
     * Returns a map containing the QualifiedNames and values of properties associated
     * with this record schedule.  Used to build response to UPnP Browse actions.
     * 
     * @return  copy of properties map
     */
    public Map getPropertiesCopy()
    {
        RecordScheduleDirectManual recordScheduleDirectManual = buildRecordScheduleDirectManual();
        
        return recordScheduleDirectManual.getPropertiesCopy();
    }

    private RecordScheduleDirectManual buildRecordScheduleDirectManual()
    {
        assert netRecordingEntry != null;

        MetadataNodeImpl metadataNode = (MetadataNodeImpl) netRecordingEntry.getRootMetadataNode();

        RecordScheduleDirectManual recordScheduleDirectManual = new RecordScheduleDirectManual(metadataNode,
                RecordScheduleDirectManual.RECORD_SCHEDULE_USAGE);

        recordScheduleDirectManual.setValue(UPnPConstants.QN_OCAP_SCHEDULED_CDS_ENTRY_ID, scheduledCDSEntryID);
        recordScheduleDirectManual.setValue(UPnPConstants.QN_OCAP_CDS_REFERENCE,
                DIDLLite.getView(netRecordingEntry));
        // Adding srs class property before validation for mandatory property is carried out later.
        recordScheduleDirectManual.setValue(UPnPConstants.QN_SRS_CLASS, RecordScheduleDirectManual.CLASS_PROPERTY);
        // override and set the properties that only the RecordSchedule can set
        recordScheduleDirectManual.setValue(UPnPConstants.QN_SRS_ID_ATTR, objectID);
        recordScheduleDirectManual.setValue(UPnPConstants.QN_SRS_ABNORMAL_TASKS_EXIST, abnormalTasksExist);
        recordScheduleDirectManual.setValue(UPnPConstants.QN_SRS_CURRENT_RECORD_TASK_COUNT,
                String.valueOf(currentRecordTaskCount));
        recordScheduleDirectManual.setValue(UPnPConstants.QN_SRS_SCHEDULE_STATE, scheduleState);
        recordScheduleDirectManual.setValue(UPnPConstants.QN_SRS_SCHEDULE_STATE_CURRENT_ERRORS_ATTR,
                scheduleState_currentErrors);
        recordScheduleDirectManual.setValue(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID, objectID);

        if (!recordScheduleDirectManual.isValid())
        {
            throw new IllegalArgumentException();
        }
        // Adding validation for existence of mandatory srs properties.
        if (!recordScheduleDirectManual.isAllowedPropertyValue())
        {
            throw new IllegalArgumentException();
        }
        
        return recordScheduleDirectManual;
    }
    
    // incremented each new RecordSchedule to create the unique ObjectID
    private static int objectIDint = 0;

    private final String objectID;

    // association to the NetRecordingEntryLocal instance
    private NetRecordingEntryLocal netRecordingEntry = null;

    private LinkedList recordTaskList = new LinkedList();

    // properties defined by UPnP SRS appendix B
    private String scheduleState = OPERATIONAL_STATE;

    private String scheduleState_currentErrors = "";

    private String abnormalTasksExist = "0";// no

    private int currentRecordTaskCount = 0;
}
