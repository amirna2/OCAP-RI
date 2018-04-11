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

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.recording.NavigationManager;
import org.cablelabs.impl.manager.recording.RecordingListImpl;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.NetManagerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.ContentItemImpl;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordScheduleDirectManual;
import org.cablelabs.impl.ocap.hn.recording.RecordScheduleDirectManualBuilder;
import org.cablelabs.impl.ocap.hn.recording.RecordingActions;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.cablelabs.impl.ocap.hn.upnp.cds.AddEntryAugmenter;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.DestructionApprover;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedServiceImpl;
import org.cablelabs.impl.ocap.hn.upnp.srs.ScheduledRecordingServiceSCPD;
import org.cablelabs.impl.util.Arrays;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.NetRecordingRequestManager;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.hn.upnp.server.UPnPManagedStateVariable;

/**
 * This is the implementation class for the UPnP ScheduledRecording Service
 * (SRS). It enables the connection between the OCAP HN API and the low level
 * SRS mechanisms.
 *
 * @author Dan Woodard
 * @version $Revision$
 * @see
 */
public final class ScheduledRecordingService implements UPnPActionHandler
{
    /**
     * UPnP service type urn.
     */
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:ScheduledRecording:1";
    
    // Handle to the NetSecurityManager. Used to authorize the action.
    private NetSecurityManagerImpl securityManager = (NetSecurityManagerImpl) NetSecurityManagerImpl.getInstance();

    private static final long MAX_UNSIGNED_INT = 4294967295L; // ui4 max 2e32-1

    private static final String SERVICEID = "SRS:";
    private static final String LAST_CHANGE= "LastChange";
        
    private static final String DATA_TYPE_RECORD_SCHEDULE = "A_ARG_TYPE_RecordSchedule";
    private static final String DATA_TYPE_RECORD_TASK = "A_ARG_TYPE_RecordTask";
    private static final String DATA_TYPE_RECORD_SCHEDULE_PARTS = "A_ARG_TYPE_RecordScheduleParts";

    // State Variable Name Constants
    //
    private static final String SORT_CAPABILITIES_VAR_NAME = "SortCapabilities";
    private static final String SORT_LEVEL_CAPABILITY_VAR_NAME = "SortLevelCapability";
    private static final String PROPERTY_LIST_VAR_NAME = "A_ARG_TYPE_PropertyList";

    // Action Argument Name Constants
    //
    public static final String DATA_TYPE_ID_ARG_NAME= "DataTypeID";
    public static final String FILTER_ARG_NAME= "Filter";
    public static final String STARTING_INDEX_ARG_NAME = "StartingIndex";
    public static final String REQUESTED_COUNT_ARG_NAME = "RequestedCount";
    public static final String SORT_CRITERIA_ARG_NAME= "SortCriteria";
    public static final String RECORD_SCHEDULE_ID_ARG_NAME= "RecordScheduleID";
    public static final String RECORD_TASK_ID_ARG_NAME = "RecordTaskID";
    public static final String RECORD_TASK_CONFLICT_ID_LIST_ARG_NAME = "RecordTaskConflictIDList";
    public static final String RECORDING_IDS_ARG_NAME = "RecordingIDs";
    public static final String OBJECT_ID_ARG_NAME = "ObjectID";   
          
    public static final String SRS_SORT_CAPABILITIES = "";
    public static final String SRS_SORT_LEVEL_CAP = "1";
    
    private static final String FILTER_ALL = "*:*";

    private static final String RESULT_BEG_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<srs " +
    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
    "xmlns:srs=\"urn:schemas-upnp-org:av:srs\" " +
    "xmlns=\"urn:schemas-upnp-org:av:srs\" " +
    "xmlns:ocap=\"urn:schemas-cablelabs-org:metadata-1-0/\" " +
    "xmlns:ocapApp=\"urn:schemas-opencable-com:ocap-application\" "+
    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
    "xsi:schemaLocation=\"urn:schemas-upnp-org:av:srs " +
    "http://www.upnp.org/schemas/av/srs-v1-20060531.xsd\">\n";

    private static final String RESULT_END_XML = "</srs>";

    private static final String AVDT_BEG_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<AVDT  " +
    "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
    "xmlns:srs=\"urn:schemas-upnp-org:av:srs\" " +
    "xmlns=\"urn:schemas-upnp-org:av:avdt\" " +
    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
    "xsi:schemaLocation=\" " +
    "urn:schemas-upnp-org:av:srs " +
    "http://www.upnp.org/schemas/av/srs-v1-20060531.xsd " +
    "urn:schemas-upnp-org:av:avdt " +
    "http://www.upnp.org/schemas/av/avdt-v1-20060531.xsd\">\n" +
    "<contextID>\n" +
    "uuid:device-UUID::urn:schemas-upnp-org:service:ScheduledRecording:1" +
    "</contextID>";

    private static final String AVDT_END_XML =
        "</fieldTable>\n" +
        "</AVDT>";

    private static final String AVDT_RECORD_SCHEDULE_BEG_XML =
        "<dataStructType>A_ARG_TYPE_RecordSchedule</dataStructType>\n" +
        "<fieldTable>\n";

    private static final String AVDT_RECORD_TASK_BEG_XML =
        "<dataStructType>A_ARG_TYPE_RecordSchedule</dataStructType>\n" +
        "<fieldTable>\n";

    private static final String AVDT_RECORD_SCHEDULE_PARTS_BEG_XML =
        "<dataStructType>A_ARG_TYPE_RecordScheduleParts</dataStructType>\n" +
        "<fieldTable>\n";

    private static final NetSecurityManagerImpl netSecurityManager = ((NetSecurityManagerImpl) NetSecurityManagerImpl.getInstance());

    // Log4J logger.
    private static final Logger log = Logger.getLogger(ScheduledRecordingService.class);

    private RecordSchedules recordSchedules = new RecordSchedules(); // list of
                                                                     // RecordSchedule
                                                                     // instances

    private long stateUpdateID = 0; // incremented each SRS change and sent with
                                    // LastChange event

    private static ContentServerNetModuleImpl contentServerNetModuleImpl = null;

    private static NetRecordingRequestManagerImpl netRecordingRequestManager = null;
    
    private ArrayList m_services = new ArrayList();
    private final LastChange m_lastChange;

    /**
     * Construct a <code>ScheduledRecordingService</code>.
     */
    public ScheduledRecordingService(ContentDirectoryService cds)
    {
        // Install interceptor with DVR / SRS dependency to allow non-DVR compiles.
        // If SRS is ever required to be disabled, this intereceptor will need to find a new home.
        MediaServer.getInstance().addHTTPRequestInterceptor(new RecordedServiceInterceptor());
        
        m_lastChange = new LastChange();

        cds.registerDestructionApprover(new DestructionApprover()
        {
            public boolean allowDestroy(ContentEntry entry, InetAddress client)
            {
                if (entry instanceof RecordingContentItem || entry instanceof NetRecordingEntry)
                {
                    if ((getNetRecordingRequestManager() == null) ||
                        (!getNetRecordingRequestManager().isHandlerSet()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("allowDestroy: NetRecordingRequestManager handler not set");
                        }
                        // setting handler is optional ,, pass thru if not set
                        return true;
                    }

                    if (!getNetRecordingRequestManager().notifyDelete(client, entry))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("allowDestroy: NetRecordingRequestManagerImpl.notifyDelete() returned false");
                        }
                        return false;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("allowDestroy: NetRecordingRequestManagerImpl.notifyDelete() returned true");
                    }
                }

                return true;
            }
        });

        cds.registerAddEntryAugmenter(new AddEntryAugmenter()
        {
            public void augmentAddEntry(ContentEntryImpl entry)
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("augmentAddEntry: " + entry);
                }
                // Updating the MetadataNode for known type mappings
                if (entry instanceof NetRecordingEntry)
                {
                    // HNP 2.0 leaves this intentionally unspecified.
                    MetadataNode mdn = entry.getRootMetadataNode();
                    if (mdn instanceof MetadataNodeImpl)
                    {
                        MetadataNodeImpl mni = (MetadataNodeImpl) mdn;

                        mni.addMetadataRegardless(UPnPConstants.QN_UPNP_CLASS, "object.item.videoItems");
                    }
                }
            }
        });
    }

    public void registerMediaServer(UPnPManagedDevice mediaDev)
    {
        UPnPManagedService service = null;
        
        // Create service
        try
        {
            service = mediaDev.createService(ScheduledRecordingService.SERVICE_TYPE,
                    new ByteArrayInputStream(XMLUtil.toByteArray(ScheduledRecordingServiceSCPD.getSCPD())), null);
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("ScheduledRecordingService: failed to create ScheduledRecordingService ", e);
            }
        }

        // Register as listener to actions for this service.
        service.setActionHandler(this);
        
        m_services.add(service);
        m_lastChange.registerVariable(((UPnPManagedServiceImpl)service).getManagedStateVariable(LAST_CHANGE));
    }
    
    /**
     * Finds the NetRecordingEntryLocal associated with the RecordSchedule that
     * has an ObjectID equal to the input ID.
     *
     * @param objectID
     *            The ObjectID of the RecordSchedule.
     * @return null if not found, non-null if found
     */
    public NetRecordingEntryLocal getNetRecordingEntry(String objectID)
    {
        RecordSchedule recordSchedule = this.recordSchedules.getRecordSchedule(objectID);

        if (recordSchedule != null)
        {
            return recordSchedule.getNetRecordingEntry();
        }

        return null;
    }

    /**
     * Returns a RecordingContentItemLocal instance that is associated with the
     * RecordingContentItem metadata stored in the CDS referenced by
     * CDS_ObjectID. If CDS_ObjectID does not refer to a valid
     * RecordingContentItem, null is returned.
     */
    public RecordingContentItemLocal getRecordingContentItemLocal(String CDS_ObjectID)
    {
        RecordTask recordTask = this.recordSchedules.getRecordTaskByCDSID(CDS_ObjectID);

        if (recordTask != null)
        {
            return recordTask.getRecordingContentItem();
        }

        return null;
    }
    
    // //////////////////////////////////////////////////////////////////
    //
    // SRS Package Private
    //
    // //////////////////////////////////////////////////////////////////

    /**
     * Creates a new RecordSchedule, adds it to the list of RecordSchedule
     * instances and sends a LastChange event.
     *
     * @param entry
     *            The NetRecordingEntry to associate with the new RecordSchedule
     *            instance.
     * @return true if successful, false if not.
     */
    RecordSchedule createRecordSchedule(NetRecordingEntryLocal entry)
    {
        if (log.isDebugEnabled())
        {
            log.debug("createRecordSchedule(NetRecordingEntryLocal) called");
        }

        RecordSchedule recordSchedule = new RecordSchedule(entry);

        this.recordSchedules.add(recordSchedule);

        this.notifyChange(new LastChangeReason(LastChangeReason.RECORD_SCHEDULE_CREATED, recordSchedule.getObjectID()));

        return recordSchedule;
    }

    /**
     * Removes a RecordSchedule from the list if it does not have any
     * RecordTasks.
     *
     * @param recordSchedule
     *            The RecordSchedule to remove.
     * @return true if the recordSchedule was successfully removed or didn't
     *         exist. false if the recordSchedule contains any RecordTasks.
     */
    boolean removeRecordSchedule(RecordSchedule recordSchedule)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeRecordSchedule: " + recordSchedule);
        }
        if (recordSchedule.hasRecordTasks())
        {
            // Remove the tasks associated with this record schedule
            ArrayList taskList = new ArrayList();
            recordSchedule.getRecordTasks(taskList);
            for (int i = 0; i < taskList.size(); i++)
            {
                RecordTask task = (RecordTask)taskList.get(i);
                RecordingContentItemLocal item = task.getRecordingContentItem();
                item.removeRecordTask();
                recordSchedule.removeRecordTask(task);
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("removing");
        }
        this.recordSchedules.remove(recordSchedule);

        this.notifyChange(new LastChangeReason(LastChangeReason.RECORD_SCHEDULE_DELETED, recordSchedule.getObjectID()));

        return true;
    }

    /**
     * Creates a new RecordSchedule, adds it to the list of RecordSchedule
     * instances and sends a LastChange event.
     *
     * @return true if successful, false if not.
     */
    RecordSchedule createRecordSchedule()
    {
        if (log.isDebugEnabled())
        {
            log.debug("createRecordSchedule() called");
        }

        RecordSchedule recordSchedule = new RecordSchedule();

        this.recordSchedules.add(recordSchedule);

        this.notifyChange(new LastChangeReason(LastChangeReason.RECORD_SCHEDULE_CREATED, recordSchedule.getObjectID()));

        return recordSchedule;
    }

    /**
     * SRS state change notify.
     */
    void notifyChange(LastChangeReason reason)
    {
        if (stateUpdateID == MAX_UNSIGNED_INT)
        {
            stateUpdateID = 0; // roll over
        }
        else
        {
            ++stateUpdateID;
        }

        if (log.isDebugEnabled())
        {
            log.debug("stateUpdateID=" + stateUpdateID);
        }

        reason.setUpdateID(stateUpdateID);
        m_lastChange.change(reason);
    }

    /**
     * Initialize this service's state variables.
     */
    public final void initializeStateVariables()
    {
        for(Iterator i = m_services.iterator(); i.hasNext();)
        {
            UPnPManagedServiceImpl service = (UPnPManagedServiceImpl)i.next();
            UPnPManagedStateVariable sv = service.getManagedStateVariable(SORT_CAPABILITIES_VAR_NAME);
            if(sv != null)
            {
                sv.setValue(SRS_SORT_CAPABILITIES);
            }
        
            sv = service.getManagedStateVariable(SORT_LEVEL_CAPABILITY_VAR_NAME);
            if(sv != null)
            {
                sv.setValue(SRS_SORT_LEVEL_CAP);        
            }
        
            sv = service.getManagedStateVariable(PROPERTY_LIST_VAR_NAME);
            if(sv != null)
            {
                sv.setValue(null);
            }
        }
    }

    /**
     * Get the NetRecordingRequestManagerImpl.
     *
     * @return The NetRecordingRequestManagerImpl.
     */
    private NetRecordingRequestManagerImpl getNetRecordingRequestManager()
    {
        if (netRecordingRequestManager == null)
        {
            NetList netList = NetManagerImpl.getInstance().getNetModuleList(null);

            Enumeration enumeration = netList.getElements();

            while (enumeration.hasMoreElements())
            {
                Object obj = enumeration.nextElement();

                if (obj instanceof NetRecordingRequestManager)
                {
                    if (((NetRecordingRequestManagerImpl)obj).isLocal())
                    {
                        netRecordingRequestManager = ((NetRecordingRequestManagerImpl) obj);
                        break;
                    }
                }
            }
        }

        return netRecordingRequestManager;
    }

    /**
     * Process CreateRecordSchedule action from UPnP network ControlPoint. Call
     * notifySchedule() if id attribute is not set, notifyReschedule if id
     * attribute is set.
     */
    private UPnPResponse performCreateRecordScheduleAction(UPnPActionInvocation action, InetAddress client)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performCreateRecordScheduleAction() - called");
        }

        if ((getNetRecordingRequestManager() == null) ||
            (!getNetRecordingRequestManager().isHandlerSet()))
        {
            if (log.isInfoEnabled())
            {
                log.info("performCreateRecordScheduleAction(), " +
                "returning false, NetRecordingRequestManager handler not set.");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                        ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), action);
        }
        
        String elements = action.getArgumentValue(RecordingActions.ELEMENTS_ARG_NAME);

        if (log.isDebugEnabled())
        {
            log.debug("performCreateRecordScheduleAction() - element: " + elements);
        }

        if (elements == null || elements.trim().equals("") || elements.length() == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("performCreateRecordScheduleAction() - " +
                "returning error, no elements");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        RecordScheduleDirectManual[] rsdma = RecordScheduleDirectManualBuilder.build(elements,
                RecordScheduleDirectManualBuilder.A_ARG_TYPE_RECORD_SCHEDULE_PARTS_USAGE);


        if (rsdma.length < 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("performCreateRecordScheduleAction(), " +
                "returning false, failed to parse RecordScheduleDirectManual from input Elements.");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_SYNTAX.getCode(),
                    ActionStatus.UPNP_SRS_INVALID_SYNTAX.getDescription(), action);
        }
        else
        {
            // If RecordScheduleDirectManual instance is created then validate
            // them for mandatory values and allowed property values.
            int validationErrorCode = RecordScheduleDirectManualBuilder.validateRecordScheduleDirectManual(rsdma);

            if (validationErrorCode == RecordScheduleDirectManualBuilder.ERROR_PROPERTY_VALUE_NOT_ALLOWED)
            {
                if (log.isDebugEnabled())
                {
                    log.info("performCreateRecordScheduleAction(), "
                            + "returning  error response, One of the properties has a value that is not allowed");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                        ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
            }

            if (validationErrorCode == RecordScheduleDirectManualBuilder.ERROR_MISSING_MANDATORY_PROPERTY)
            {
                if (log.isDebugEnabled())
                {
                    log.info("performCreateRecordScheduleAction(), "
                            + "returning error response, One of the Mandatory property is missing");
                }

                return new UPnPErrorResponse(ActionStatus.UPNP_SRS_MISSING_MANDATORY.getCode(),
                        ActionStatus.UPNP_SRS_MISSING_MANDATORY.getDescription(), action);
            }
        }

        MetadataNodeImpl metadataNode = rsdma[0].getNetRecordingEntryMetadataNode();

        RecordSchedule recordSchedule;

        // Either call notifySchedule (if id attribute is not set) or
        // notifyReschedule (if id attribute is set).

        String id = rsdma[0].getID();

        if (id == null || id.equals("")) // no id attribute means call
                                         // notifySchedule
        {
            NetRecordingEntryLocal netRecordingEntry = new NetRecordingEntryLocal(metadataNode);

            if (!getNetRecordingRequestManager().notifySchedule(client, netRecordingEntry))
            {
                if (log.isInfoEnabled())
                {
                    log.info("performCreateRecordScheduleAction(), " +
                    "returning false, NetRecordingRequestManagerImpl.notifySchedule() returned false");
                }

                return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                        ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), action);
            }
            else
            // the NetRecordingRequestHandler approves of this schedule
            {
                if (log.isDebugEnabled())
                {
                    log.debug("performCreateRecordScheduleAction(), NetRecordingRequestManagerImpl.notifySchedule() returned true");
                }
                
                // Verify NetRecordingEntry
                //
                String cdsID = netRecordingEntry.getScheduledCDSEntryID();

                ContentEntry contentEntry = getContentEntry(cdsID);

                if (contentEntry instanceof NetRecordingEntry)
                {
                    String parentID = contentEntry.getParentID();

                    if (parentID == null || parentID.equals("")) // Verify that
                                                                 // NetRecordingEntry
                                                                 // has a parent
                                                                 // container ID
                                                                 // property
                    { // to verify that it has been added to CDS
                        if (log.isInfoEnabled())
                        {
                            log.info("performCreateRecordScheduleAction(), " +
                            "returning false, NetRecordingRequestHandler returned true but "
                            + "netRecordingEntry does not have a parentID");
                        }
                       return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                                   ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
                    }
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("performCreateRecordScheduleAction(), " +
                        "returning false, NetRecordingRequestHandler returned true but "
                        + "netRecordingEntry does not have a corresponding CDS entry; " + "cdsID is "
                        + (cdsID == null ? "null" : "\"" + cdsID + "\"") + ", contentEntry is "
                        + (contentEntry == null ? "null" : "a(n) " + contentEntry.getClass().getName()));
                    }
                    return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                            ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
                }

                recordSchedule = netRecordingEntry.getRecordSchedule();

                if (recordSchedule == null) // Verify that the NetRecordingEntry
                                            // has an associated RecordSchedule
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("performCreateRecordScheduleAction(), " +
                        "returning false, NetRecordingRequestHandler returned true but "
                        + "NetRecordingEntry does not contain a RecordSchedule");
                    }
                    return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                            ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
                }
                else if (netRecordingEntry.hasCdsReferences() == false) // Verify
                                                                        // that
                                                                        // ocap:cdsReference
                                                                        // and
                                                                        // ocap:scheduledCDSEntryID
                // was added to RecordSchedule
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("performCreateRecordScheduleAction(), " +
                        "returning false, NetRecordingRequestHandler returned true but "
                        + "NetRecordingEntry contained RecordSchedule does not have any CDS references");
                    }
                    return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                            ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
                }
            }
        }
        else
        // id attribute means call notifyReschedule
        {
            NetRecordingEntryLocal netRecordingEntry = new NetRecordingEntryLocal(metadataNode);

            // find ContentEntry for the input @id

            ContentEntry contentEntry;

            recordSchedule = this.recordSchedules.getRecordSchedule(id);

            if (recordSchedule == null)
            {
                RecordTask recordTask = this.recordSchedules.getRecordTask(id);

                if (recordTask == null || recordTask.getRecordSchedule() == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("performCreateRecordScheduleAction(), " +
                        "returning false, could not find RecordSchedule or RecordTask to reschedule id = "
                        + id);
                    }
                    return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                            ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
                }
                else
                {
                    recordSchedule = recordTask.getRecordSchedule();
                    contentEntry = recordTask.getRecordingContentItem().getContentItemImpl();
                }
            }
            else
            {
                contentEntry = recordSchedule.getNetRecordingEntry();
            }

            if (contentEntry == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("performCreateRecordScheduleAction(), " +
                    " returning false, could not find ContentEntry to reschedule id = " + id);
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                        ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
            }

            if (!getNetRecordingRequestManager().notifyReschedule(client, contentEntry, netRecordingEntry))
            {
                if (log.isInfoEnabled())
                {
                    log.info("performCreateRecordScheduleAction(), " +
                    "returning false, NetRecordingRequestManagerImpl.notifyReschedule() returned false");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                        ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), action);
            }
            else
            // the NetRecordingRequestHandler approves of this reschedule
            {
                if (log.isDebugEnabled())
                {
                    log.debug("performCreateRecordScheduleAction(), " +
                    " NetRecordingRequestManagerImpl.notifyReschedule() returned true");
                }
            }
        }

        // I don't believe the below condition is possible at this point,
        // though I have not yet been able to prove that to my satisfaction.
        // I am adding this test so that if it ever DOES arise, someone
        // can investigate the use case and be sure that it is dealt with
        // appropriately. (It is important, because it is a precondition of
        // get_A_ARG_TYPE_RecordSchedule.)
        if (recordSchedule.getNetRecordingEntry() == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("performCreateRecordScheduleAction(),  " +
                "returning false, RecordSchedule has no NetRecordingEntry");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_SRS_INVALID_VALUE.getCode(),
                    ActionStatus.UPNP_SRS_INVALID_VALUE.getDescription(), action);
        }

        //
        // Send response to action with <A_ARG_TYPE_ObjectID,
        // A_ARG_TYPE_RecordSchedule, StateUpdateID>.
        //
        String recordScheduleID = recordSchedule.getObjectID();
        String result = recordSchedule.get_A_ARG_TYPE_RecordSchedule();
        String updateID = String.valueOf(this.stateUpdateID);

        if (log.isDebugEnabled())
        {
            log.debug("result = " + result);
        }
        if (log.isDebugEnabled())
        {
            log.debug("performCreateRecordScheduleAction(), sending action response");
        }

        try
        {
            return new UPnPActionResponse(new String[] { recordScheduleID, result, updateID }, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }
    
    /**
     * Process EnableRecordSchedule action from UPnP network ControlPoint.
     *
     * TODO: can this be implemented? See OCSPEC-289.
     */
    private UPnPResponse performEnableRecordScheduleAction(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performEnableRecordScheduleAction() - called");
        }

        if (log.isDebugEnabled())
        {
            log.debug("performEnableRecordScheduleAction()");
        }
        
        if ((getNetRecordingRequestManager() == null) ||
                (!getNetRecordingRequestManager().isHandlerSet()))
        {
            if (log.isWarnEnabled())
            {
                log.warn("performEnableRecordScheduleAction() - " +
                "returning false, NetRecordingRequestManager handler not set.");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                        ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), action);
        }

        String recordScheduleID = action.getArgumentValue(RECORD_SCHEDULE_ID_ARG_NAME);
        
        if (recordScheduleID == null || recordScheduleID.trim().equals(""))
        {
            if (log.isWarnEnabled())
            {
                log.warn("performEnableRecordScheduleAction() - called with a null or empty record schedule id");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        if (log.isDebugEnabled())
        {
            log.debug("performEnableRecordScheduleAction() - called with record schedule id: " + 
            recordScheduleID);
        }
        RecordSchedule recordSchedule = this.recordSchedules.getRecordSchedule(recordScheduleID);

        if (recordSchedule == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("performEnableRecordScheduleAction() - could not find recordSchedule for ObjectID "
                + recordScheduleID);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), action);
        }

        NetRecordingEntryLocal netRecordingEntry = recordSchedule.getNetRecordingEntry();

        if (recordSchedule == null || netRecordingEntry == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("performEnableRecordScheduleAction(), could not find netRecordingEntry for ObjectID "
                + recordScheduleID);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), action);
        }

        String cdsID = netRecordingEntry.getScheduledCDSEntryID();

        ContentEntry cdsContentEntry = getContentEntry(cdsID);

        // if ContentEntry is not a NetRecordingEntry, then failure
        if (cdsContentEntry == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("performEnableRecordScheduleAction() getContentEntry returned null for cdsID " + cdsID);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), action);
        }

        if (!(cdsContentEntry instanceof NetRecordingEntry))
        {
            if (log.isWarnEnabled())
            {
                log.warn("performEnableRecordScheduleAction() could not find NetRecordingEntry for cdsID " + cdsID);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), action);
        }
        if (log.isDebugEnabled())
        {
            log.debug("performEnableRecordScheduleAction(), sending action response");
        }
        try
        {
            return new UPnPActionResponse(new String[0], action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }

    /**
     * Process DisableRecordSchedule action from UPnP network ControlPoint.
     */
    private UPnPResponse processDisableRecordSchedule(UPnPActionInvocation invocation)
    {

        if ((getNetRecordingRequestManager() == null) ||
            (!getNetRecordingRequestManager().isHandlerSet()))
        {
            if (log.isInfoEnabled())
            {
                log.info("NetRecordingRequestManager handler not set.");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }

        if (invocation.getArgumentNames().length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordSchedule() called with invalid arg count, arg count = "
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String recordScheduleID = invocation.getArgumentValue(RECORD_SCHEDULE_ID_ARG_NAME);

        if (log.isDebugEnabled())
        {
            log.debug("processDisableRecordSchedule(), called with recordScheduleID " + recordScheduleID);
        }

        if (recordScheduleID == null || recordScheduleID.trim().equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordSchedule(), called with a null or empty objectID");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        RecordSchedule recordSchedule = this.recordSchedules.getRecordSchedule(recordScheduleID);

        if (recordSchedule == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordSchedule(), could not find recordSchedule for ObjectID "
                + recordScheduleID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), invocation);
        }

        NetRecordingEntryLocal netRecordingEntry = recordSchedule.getNetRecordingEntry();

        if (recordSchedule == null || netRecordingEntry == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordSchedule(), could not find netRecordingEntry for ObjectID "
                + recordScheduleID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), invocation);            
        }
        // TODO: if the recording is already completed, error 740 recordSchedule "COMPLETED"
        // Can't finish this until the state mapping in HNP spec is resolved.  There is no logical
        // mapping to the COMPLETED state the way table C-2 is organized and there is no other
        // explanation to quantify.

        String cdsID = netRecordingEntry.getScheduledCDSEntryID();

        ContentEntry cdsContentEntry = getContentEntry(cdsID);

        // if ContentEntry is not a NetRecordingEntry, then failure
        if (cdsContentEntry == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordSchedule() getContentEntry returned null for cdsID " + cdsID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), invocation);
        }

        if (!(cdsContentEntry instanceof NetRecordingEntry))
        {
            if (log.isDebugEnabled())
            {
                log.debug("processDisableRecordSchedule() could not find NetRecordingEntry for cdsID " + cdsID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(),
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), invocation);
        }

        // TODO : need to go into underlying implementation. OCTECH-88
        InetAddress address = ((UPnPActionImpl)invocation.getAction()).getInetAddress();

        if (! getNetRecordingRequestManager().notifyDisable(address, cdsContentEntry))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordSchedule(), NetRecordingRequestManagerImpl.notifyDisable() returned false");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }
        else
        // the NetRecordingRequestHandler approves of this action
        {
            if (log.isDebugEnabled())
            {
                log.debug("processDisableRecordSchedule(), NetRecordingRequestManagerImpl.notifyDisable() returned true, sending action response");
            }

            try
            {
                return new UPnPActionResponse(new String[] {}, invocation);
            }
            catch (Exception e)
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
            }
        }
    }
    /**
     * Process DisableRecordTask action from UPnP network ControlPoint.
     */
    private UPnPResponse processDisableRecordTask(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processDisableRecordTask()");
        }

        if ((getNetRecordingRequestManager() == null) ||
            (!getNetRecordingRequestManager().isHandlerSet()))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordTask(), NetRecordingRequestManager handler not set.");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);        }

        if (invocation.getArgumentNames().length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisbleRecordTask() called with invalid arg count, arg count = "
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String recordTaskID = invocation.getArgumentValue(RECORD_TASK_ID_ARG_NAME);

        if (recordTaskID == null || recordTaskID.equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordTask(), called with a null or empty objectID");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        RecordTask recordTask = this.recordSchedules.getRecordTask(recordTaskID);
        RecordingContentItemLocal recordingContentItem = null;

        if (recordTask != null)
        {
            recordingContentItem = recordTask.getRecordingContentItem();
        }

        // if no such recordTask ID, error 713
        if (recordTask == null || recordingContentItem == null)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getCode(),
                    ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getDescription(), invocation);        
        }

        // if the recording is already completed, error 741 recordTask in "DONE"
        // phase
        if (recordTask.isDONE())
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_RECORDTASK_DONE.getCode(),
                    ActionStatus.UPNP_RECORDTASK_DONE.getDescription(), invocation);
        }

        String cdsID = recordTask.getRecordingContentItem().getID();

        ContentEntry cdsContentEntry = getContentEntry(cdsID);

        // if ContentEntry is not a NetRecordingEntry, then failure
        if (cdsContentEntry == null || !(cdsContentEntry instanceof RecordingContentItem))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordTask() could not find RecordingContentItem for id " + cdsID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getCode(),
                    ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getDescription(), invocation); 
        }

        InetAddress address = ((UPnPActionImpl)invocation.getAction()).getInetAddress();

        if (! getNetRecordingRequestManager().notifyDisable(address, cdsContentEntry))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDisableRecordTask(), NetRecordingRequestManagerImpl.notifyDisable() returned false");
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(),
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }
        else
        // the NetRecordingRequestHandler approves of this action
        {
            if (log.isDebugEnabled())
            {
                log.debug("processDisableRecordTask(), NetRecordingRequestManagerImpl.notifyDisable() returned true, sending action response");
            }

            try
            {
                return new UPnPActionResponse(new String[] {}, invocation);
            }
            catch (Exception e)
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
            }
        }
    }

    /**
     * Process X_PrioritizeRecordings action from UPnP network ControlPoint.
     * This will be caused by a client call to
     * RecordingContentItem.requestPrioritize().
     */
    private UPnPResponse processX_PrioritizeRecordings(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processX_PrioritizeRecordings() for requestPrioritize() processing.");
        }

        if (invocation.getArgumentNames().length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("processX_PrioritizeRecordings() called with wrong number of arguments. " +
                invocation.getArgumentNames().length);
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String objectIDs = invocation.getArgumentValue(RECORDING_IDS_ARG_NAME);
        if ((objectIDs == null) || (objectIDs.trim().equals("")))
        {
            if (log.isInfoEnabled())
            {
                log.info("processX_PrioritizeRecordings() - invalid objectIDs");
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        if (log.isDebugEnabled())
        {
            log.debug("processX_PrioritizeRecordings() got objectIDs arg: " + objectIDs);
        }

        // get the list of either RecordTask or RecordSchedule IDs from input CSV list,
        // ignoring nonexistent ones

        boolean recordTaskIDs = false;
        boolean recordScheduleIDs = false;

        List recordThings = new ArrayList();

        StringTokenizer tok = new StringTokenizer(objectIDs, ",");

        while (tok.hasMoreElements())
        {
            String objectID = ((String) tok.nextElement()).trim();

            RecordSchedule recordSchedule = recordSchedules.getRecordSchedule(objectID);
            if (recordSchedule != null)
            {
                recordScheduleIDs = true;
                recordThings.add(recordSchedule);
                continue;
            }

            RecordTask recordTask = recordSchedules.getRecordTask(objectID);
            if (recordTask != null)
            {
                recordTaskIDs = true;
                recordThings.add(recordTask);
                continue;
            }
        }

        if (recordThings.size() == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("processX_PrioritizeRecordings() called with 0 objects");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }

        if (recordScheduleIDs && recordTaskIDs)
        {
            if (log.isInfoEnabled())
            {
                log.info("processX_PrioritizeRecordings() called with nonhomogeneous IDs");
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_NON_HOMOGENEOUS_IDS.getCode(), 
                    ActionStatus.UPNP_NON_HOMOGENEOUS_IDS.getDescription(), invocation);           
        }

        List list = new ArrayList();

        for (Iterator i = recordThings.iterator(); i.hasNext(); )
        {
            if (recordScheduleIDs)
            {
                // all of the record things must be RecordSchedules
                RecordSchedule recordSchedule = (RecordSchedule) i.next();

                if (recordSchedule.getNetRecordingEntry() == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("processX_PrioritizeRecordings() called with bad recordSchedule: "
                        + recordSchedule.getObjectID());
                    }

                    return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                            ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
                }

                String cdsID = recordSchedule.getNetRecordingEntry().getScheduledCDSEntryID();

                ContentEntry contentEntry = getContentEntry(cdsID);

                list.add(contentEntry);
            }
            else
            {
                // all of the record things must be RecordTasks
                RecordTask recordTask = (RecordTask) i.next();

                if (recordTask.getRecordingContentItem() == null)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("processX_PrioritizeRecordings() called with bad recordTask: "
                        + recordTask.getObjectID());
                    }

                    return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                            ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
                }

                String cdsID = recordTask.getRecordingContentItem().getID();

                ContentEntry contentEntry = getContentEntry(cdsID);

                list.add(contentEntry);
            }
        }

        // now call notifyPrioritization()

        InetAddress address = ((UPnPActionImpl)invocation.getAction()).getInetAddress();

        boolean notifyResult;

        if (recordScheduleIDs)
        {
            // recordScheduleIDs
            NetRecordingEntry[] nrea = new NetRecordingEntry[list.size()];

            Iterator iter = list.iterator();
            int i = 0;
            while (iter.hasNext())
            {
                nrea[i++] = (NetRecordingEntry) iter.next();
            }

            if (getNetRecordingRequestManager() != null)
            {
                notifyResult = getNetRecordingRequestManager().notifyPrioritization(address, nrea);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("processX_PrioritizeRecordings(), NetRecordingRequestManagerImpl is null");
                }
                
                return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                        ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
            }
        }
        else
        {
            // recordTaskIDs
            RecordingContentItem[] rcia = new RecordingContentItem[list.size()];

            Iterator iter = list.iterator();
            int i = 0;
            while (iter.hasNext())
            {
                rcia[i++] = (RecordingContentItem) iter.next();
            }

            if (getNetRecordingRequestManager() != null)
            {
                notifyResult = getNetRecordingRequestManager().notifyPrioritization(address, rcia);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("processX_PrioritizeRecordings(), NetRecordingRequestManagerImpl is null");
                }
                
                return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                        ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);                
            }
        }

        if (notifyResult == false)
        {
            if (log.isInfoEnabled())
            {
                log.info("processX_PrioritizeRecordings(), NetRecordingRequestManagerImpl.notifyPrioritization() returned false");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("processX_PrioritizeRecordings(), NetRecordingRequestManagerImpl.notifyPrioritization() returned true");
            }

            try
            {
                return new UPnPActionResponse(new String[] {}, invocation);
            }
            catch (Exception e)
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
            }
        }
    }

    /**
     * Process DeleteRecordTask action from UPnP network ControlPoint. This will
     * be caused by a client call to RecordingContentItem.requestDeleteService(
     * RecordingContentItem ).
     */
    private UPnPResponse processDeleteRecordTask(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processDeleteRecordTask() for requestDeleteService() processing.");
        }

        if (invocation.getAction().getArgumentNames().length < 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordTask() called with less than 1 argument. arg count = "
                + invocation.getAction().getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
            
        }

        String recordTaskID = invocation.getArgumentValue(RECORD_TASK_ID_ARG_NAME);

        if (recordTaskID == null || recordTaskID.equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordTask(), called with a null or empty objectID");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        RecordTask recordTask = this.recordSchedules.getRecordTask(recordTaskID);
        RecordingContentItemLocal recordingContentItem = null;

        if (recordTask != null)
        {
            recordingContentItem = recordTask.getRecordingContentItem();
        }

        // if no such recordTask ID, error 713
        if (recordTask == null || recordingContentItem == null)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getCode(), 
                    ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getDescription(), invocation);
        }

        String cdsID = recordTask.getRecordingContentItem().getID();

        ContentEntry contentEntry = getContentEntry(cdsID);

        // if ContentEntry is not a RecordingContentItem
        if (contentEntry == null || !(contentEntry instanceof RecordingContentItem))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordTask() could not find CDS RecordingContentItem for recordTaskID "
                + recordTaskID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_OBJECT.getCode(), 
                    ActionStatus.UPNP_NO_SUCH_OBJECT.getDescription(), invocation);
        }

        InetAddress address = ((UPnPActionImpl)invocation.getAction()).getInetAddress();

        if ((getNetRecordingRequestManager() == null) ||
            (!getNetRecordingRequestManager().notifyDeleteService(address, contentEntry)))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordTask(), NetRecordingRequestManagerImpl.notifyDeleteService() returned false");
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);           
        }
        else
        // the NetRecordingRequestHandler approves of this action
        {
            if (log.isDebugEnabled())
            {
                log.debug("processDeleteRecordTask(), NetRecordingRequestManagerImpl.notifyDeleteService() returned true, sending action response");
            }

            // remove RecordTask from RecordSchedule
            recordTask.getRecordSchedule().removeRecordTask(recordTask);

            try
            {
                return new UPnPActionResponse(new String[] {}, invocation);
            }
            catch (Exception e)
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
            }
        }
    }
    
    /**
     * Process DeleteRecordSchedule action from UPnP network ControlPoint. This
     * will be caused by a client call to
     * RecordingContentItem.requestDeleteService( NetRecordingEntry ).
     */
    private UPnPResponse processDeleteRecordSchedule(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processDeleteRecordSchedule() for requestDeleteService() processing.");
        }

        if (invocation.getArgumentNames().length < 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordSchedule() called with less than 1 argument. arg count = "
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String recordScheduleID = invocation.getArgumentValue(RECORD_SCHEDULE_ID_ARG_NAME);

        if (recordScheduleID == null || recordScheduleID.equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordSchedule(), called with a null or empty objectID");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        RecordSchedule recordSchedule = this.recordSchedules.getRecordSchedule(recordScheduleID);

        if (recordSchedule == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordSchedule(), could not find recordSchedule for RecordScheduleID "
                + recordScheduleID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(), 
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), invocation);
        }

        NetRecordingEntryLocal netRecordingEntry = recordSchedule.getNetRecordingEntry();

        if (recordSchedule == null || netRecordingEntry == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordSchedule(), could not find netRecordingEntry for RecordScheduleID "
                + recordScheduleID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }

        String cdsID = netRecordingEntry.getScheduledCDSEntryID();

        ContentEntry contentEntry = getContentEntry(cdsID);

        // if ContentEntry is not a NetRecordingEntry
        if (contentEntry == null || !(contentEntry instanceof NetRecordingEntry))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordSchedule() could not find CDS NetRecordingEntry for RecordScheduleID "
                + recordScheduleID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_OBJECT.getCode(), 
                    ActionStatus.UPNP_NO_SUCH_OBJECT.getDescription(), invocation);
        }

        InetAddress address = ((UPnPActionImpl)invocation.getAction()).getInetAddress();

        if ((getNetRecordingRequestManager() == null) || 
            (!getNetRecordingRequestManager().notifyDeleteService(address, contentEntry)))
        {
            if (log.isInfoEnabled())
            {
                log.info("processDeleteRecordSchedule(), NetRecordingRequestManagerImpl.notifyDeleteService() returned false");
            }
            
            
            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }
        else
        // the NetRecordingRequestHandler approves of this action
        {
            if (log.isDebugEnabled())
            {
                log.debug("processDeleteRecordSchedule(), NetRecordingRequestManagerImpl.notifyDeleteService() returned true, sending action response");
            }

            // if all RecordTasks in the RecordSchedule are in IDLE or DONE
            // phase, remove
            // the RecordSchedule from RecordSchedules, error 705 if not
            if (recordSchedule.allRecordTask_IDLEorDONE())
            {
                removeRecordSchedule(recordSchedule);
            }
            else
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_RECORDTASK_ACTIVE.getCode(), 
                        ActionStatus.UPNP_RECORDTASK_ACTIVE.getDescription(), invocation);
            }
            try
            {
                return new UPnPActionResponse(new String[] {}, invocation);
            }
            catch (Exception e)
            {
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                        
            }
        }
    }

    /**
     * Process GetRecordTaskConflicts action from UPnP network ControlPoint.
     * This will be caused by a client call to
     * RecordingContentItem.requestConflictingRecordings().
     */
    private UPnPResponse processGetRecordTaskConflicts(UPnPActionInvocation invocation)
    {
        if (log.isInfoEnabled())
        {
            log.info("processGetRecordTaskConflicts() for requestConflictingRecordings() processing.");
        }

        if (invocation.getArgumentNames().length < 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordTaskConflicts() called with less than 1 argument. arg count = "
                + invocation.getArgumentNames().length);
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String recordTaskID = invocation.getArgumentValue(RECORD_TASK_ID_ARG_NAME);
        
        if (recordTaskID == null || recordTaskID.trim().equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordTaskConflicts() - invalid arg for record task id");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        RecordTask recordTask = this.recordSchedules.getRecordTask(recordTaskID);

        if (recordTask == null || recordTask.getRecordingContentItem() == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordTaskConflicts(), RecordTask or RecordingContentItem not found for input recordTaskID: "
                + recordTaskID);
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getCode(), 
                    ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getDescription(), invocation);           
        }

        // find conflicting recordings where "usage of resources that conflict".
        //
        int[] states = new int[] { OcapRecordingRequest.PENDING_NO_CONFLICT_STATE,
                OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, OcapRecordingRequest.IN_PROGRESS_STATE,
                OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE };

        RecordingListImpl recordingList = NavigationManager.getInstance().getOverlappingEntriesInStates(
                recordTask.getRecordingContentItem(), states);

        // build CSV list of conflicting RecordTask ids
        //
        String csvRecordings = "";

        for (int i = 0; i < recordingList.size(); i++)
        {
            RecordingContentItemLocal recording = (RecordingContentItemLocal) recordingList.getRecordingRequest(i);

            if (csvRecordings.equals("") == false)
            {
                csvRecordings += ",";
            }

            csvRecordings += recording.getSrsRecordTaskID();
        }

        // build response
        //
        String updateID = String.valueOf(this.stateUpdateID);

        try
        {
            return new UPnPActionResponse(new String[] { csvRecordings, updateID }, invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
        }
    }

    /**
     * Handles the response to SOAP action to get sort capabilities.
     *
     * @param action SOAP action which needs response
     * @return  true if valid action request, false otherwise
     */
    private UPnPResponse processGetSortCapabilities(UPnPActionInvocation invocation)
    {
        if (log.isInfoEnabled())
        {
            log.info("processGetSortCapabilities() called");
        }
        
        if ((invocation.getArgumentNames() != null) &&
            (invocation.getArgumentNames().length > 0))
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetSortCapabilities() called with arguments. arg count = "
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        try
        {
            return new UPnPActionResponse(new String[] { SRS_SORT_CAPABILITIES, SRS_SORT_LEVEL_CAP }, invocation);            
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
        }
    }

    /**
     * Handles the response to SOAP action to get the value of the StateUpdateID state variable.
     *
     * @param action SOAP action which needs response
     * @return  true if valid action request, false otherwise
     */
    private UPnPResponse processGetStateUpdateID(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processGetStateUpdateID() called");
        }
        if ((invocation.getArgumentNames() != null) &&
            (invocation.getArgumentNames().length > 0))
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetStateUpdateID() called with arguments. arg count = "
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        try
        {
            return new UPnPActionResponse(new String[] { Long.toString(stateUpdateID) }, invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
        }
    }
    
    /**
     * Handles the response to SOAP action to get allowed values.
     *
     * @param action SOAP action which needs response
     * @return  true if valid action request, false otherwise
     */
    private UPnPResponse processGetAllowedValues(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processGetAllowedValues() called");
        }

        if ((invocation.getArgumentNames() != null) &&
            (invocation.getArgumentNames().length != 2))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetAllowedValues() called with arguments. arg count = "
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String dataType = invocation.getArgumentValue(DATA_TYPE_ID_ARG_NAME).trim();
        
        if (dataType.length() == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetAllowedValues() - missing DataTypeID arg");
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        if (!isValidDataType(dataType))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetAllowedValues() called with Invalid DataTypeID");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_DATATYPE.getCode(), 
                    ActionStatus.UPNP_INVALID_DATATYPE.getDescription(), invocation);            

        }

        // Create XML string response
        StringBuffer sb = new StringBuffer(AVDT_BEG_XML);

        String allPropList[] = null;
        if (dataType.equals(DATA_TYPE_RECORD_SCHEDULE))
        {
            sb.append(AVDT_RECORD_SCHEDULE_BEG_XML);
            allPropList = RecordScheduleDirectManual.getRecordScheduleProperties();
        }
        else if (dataType.equals(DATA_TYPE_RECORD_TASK))
        {
            sb.append(AVDT_RECORD_TASK_BEG_XML);
            allPropList = RecordingContentItemLocal.getRecordTaskProperties();
        }
        else if (dataType.equals(DATA_TYPE_RECORD_SCHEDULE_PARTS))
        {
            sb.append(AVDT_RECORD_SCHEDULE_PARTS_BEG_XML);
            allPropList = RecordScheduleDirectManual.getRecordSchedulePartsProperties();
        }

        // Verify filter argument and build filtered property list
        String filter = invocation.getArgumentValue(FILTER_ARG_NAME);
        String filteredPropList[] = null;
        if (filter != null && isFilterValid(filter))
        {
            filteredPropList = buildFilteredPropertyList(filter, allPropList, null);
        }
        else
        {
            if (filter == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("processGetAllowedValues() - no filter argument supplied");
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("processGetAllowedValues() - invalid filter argument supplied: "
                    + filter);
                }
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String allowedFieldsXMLStr = null;
        if (dataType.equals(DATA_TYPE_RECORD_SCHEDULE))
        {
             allowedFieldsXMLStr = RecordScheduleDirectManual.getRecordScheduleAllowedFieldsXMLStr(filteredPropList);
        }
        else if (dataType.equals(DATA_TYPE_RECORD_TASK))
        {
            allowedFieldsXMLStr = RecordingContentItemLocal.getRecordTaskAllowedFieldsXMLStr(filteredPropList);
        }
        else if (dataType.equals(DATA_TYPE_RECORD_SCHEDULE_PARTS))
        {
            allowedFieldsXMLStr = RecordScheduleDirectManual.getRecordSchedulePartsAllowedFieldsXMLStr(filteredPropList);
        }

        // Get the allowed values for each property in the filtered list
        sb.append(allowedFieldsXMLStr);

        // Complete the XML by adding the ending XML fragment
        sb.append(AVDT_END_XML);

        if (log.isDebugEnabled())
        {
            log.debug("processGetAllowedValues() - for data type:  " + dataType +
            " and filter: " + filter + ", returning response: " + sb.toString());
        }

        // Return the list of allowed values for each of the property names listed
        try
        {
            return new UPnPActionResponse(new String[] { sb.toString() }, invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
        }
    }

    /**
     * Handles the response to SOAP action to browse record schedules.
     *
     * @param action SOAP action which needs response
     * @return  true if valid action request, false otherwise
     */
    private UPnPResponse processBrowseRecordSchedules(UPnPActionInvocation invocation)
    {
        if (log.isInfoEnabled())
        {
            log.info("processBrowseRecordSchedules() called");
        }
        String allPropList[] = RecordScheduleDirectManual.getRecordScheduleProperties();
        String reqPropList[] = RecordScheduleDirectManual.getRecordSchedulePropertiesRequired();

        return processBrowseRecords(invocation, true, 4, allPropList, reqPropList, null);
    }

    /**
     * Handles the response to SOAP action to browse record tasks.
     *
     * @param action SOAP action which needs response
     * @return  true if valid action request, false otherwise
     */
    private UPnPResponse processBrowseRecordTasks(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processBrowseRecordTasks() called");
        }
        String allPropList[] = RecordingContentItemLocal.getRecordTaskProperties();
        String reqPropList[] = RecordingContentItemLocal.getRecordTaskPropertiesRequired();

        // Verify record schedule ID is valid
        String rsID = invocation.getArgumentValue(RECORD_SCHEDULE_ID_ARG_NAME);
        if (rsID == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processBrowseRecordTasks() - missing recordScheduleID arg");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);

        }
        else if (!rsID.trim().equals(""))
        {
            // Make sure this record schedule ID is valid
            if (recordSchedules.getRecordSchedule(rsID) == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("processBrowseRecordTasks() - invalid recordScheduleID: " + rsID);
                }
                
                return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(), 
                        ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), invocation);               
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("processBrowseRecordTasks() - using rsID: " + rsID);
        }

        return processBrowseRecords(invocation, false, 5, allPropList, reqPropList, rsID);
    }
    
    private UPnPResponse processBrowseRecords(UPnPActionInvocation invocation, boolean isSchedules, int expectedArgCnt,
            String allPropList[], String reqPropList[], String recordScheduleID)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processBrowseRecords() called");
        }

        if ((invocation.getArgumentNames() != null) &&
            (invocation.getArgumentNames().length != expectedArgCnt))
        {
            if (log.isInfoEnabled())
            {
                log.info("processBrowseRecords() - expecting " + expectedArgCnt +
                " args, called with arg count = "
                + invocation.getArgumentNames().length);
            }
                
            String argumentNames[] = invocation.getArgumentNames();
            for (int i = 0; i < argumentNames.length; i++)
            {
                if (log.isInfoEnabled())
                {
                    log.info("processBrowseRecords() - arg " + i + ": " +
                    argumentNames[i]);
                }
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);        
        }

        // Verify starting index argument
        String startIdxStr = invocation.getArgumentValue(STARTING_INDEX_ARG_NAME);
        int startIdx = -1;
        if (startIdxStr != null)
        {
            try
            {
                startIdx = Integer.parseInt(startIdxStr);
            }
            catch (NumberFormatException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("processBrowseRecords() - specified invalid value for starting idx = "
                    + startIdxStr);
                }
            }
        }
        if (startIdx < 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("processBrowseRecords() - invalid starting idx argument supplied"
                + startIdxStr);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        // Verify requested count argument
        String reqCntStr = invocation.getArgumentValue(REQUESTED_COUNT_ARG_NAME);
        int reqCnt = -1;
        if (reqCntStr != null)
        {
            try
            {
                reqCnt = Integer.parseInt(reqCntStr);
            }
            catch (NumberFormatException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("processBrowseRecords() - specified invalid value for requested cnt = "
                    + reqCntStr);
                }
            }
        }
        if (reqCnt < 1)
        {
            if (log.isDebugEnabled())
            {
                log.debug("processBrowseRecords() - invalid requested count argument supplied"
                + reqCntStr);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_ARGUMENT_INVALID.getCode(), 
                    ActionStatus.UPNP_ARGUMENT_INVALID.getDescription(), invocation);
        }

        // Verify the sort criteria is empty string since that is the only one supported currently
        String sortCriteria = invocation.getArgumentValue(SORT_CRITERIA_ARG_NAME).trim();
        if ((sortCriteria == null) || (!sortCriteria.equals("")))
        {
            if (log.isDebugEnabled())
            {
                log.debug("processBrowseRecords() - invalid sort criteria argument supplied"
                + sortCriteria);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        // Verify filter argument and build filtered property list
        String filter = invocation.getArgumentValue(FILTER_ARG_NAME);
        if (log.isDebugEnabled())
        {
            log.debug("buildResult() - using filter: " + filter);
        }
        String filteredPropList[] = null;
        if (filter != null && isFilterValid(filter))
        {
            filteredPropList = buildFilteredPropertyList(filter, allPropList, reqPropList);
        	        	
            //Ensure that all required srs namespace properties are included regardless of the 
            // filter parameter value
            ArrayList currentProps = new ArrayList();
            for (int x = 0; x < filteredPropList.length; x++)
            {
                currentProps.add(filteredPropList[x]);
            }
            for (int x = 0; x < reqPropList.length; x++)
            {
                if (reqPropList[x].indexOf(UPnPConstants.NSN_SRS_PREFIX) == 0)
                {
                    if(!currentProps.contains(reqPropList[x])){
                        currentProps.add(reqPropList[x]);
                    }
                }
            }
            if (currentProps.size() != filteredPropList.length) {
                filteredPropList = new String[currentProps.size()];
                for (int x = 0; x < currentProps.size(); x++)
                {
                    filteredPropList[x] = (String) currentProps.get(x);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("buildResult() - no filter argument supplied"
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        if (filteredPropList != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("processBrowseRecords() - filtered properties: ");
            }
            for (int i = 0; i < filteredPropList.length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Filtered Prop " + (i+1) + ": " + filteredPropList[i]);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("processBrowseRecords() - no filtered properties");
            }
        }

        // Create the XML document for the response
        StringBuffer sb = new StringBuffer(RESULT_BEG_XML);

        int totalMatches = 0;
        int numberReturned = 0;
        if (filteredPropList != null)
        {
            // Add an item for each record schedule
            ArrayList list = null;
            if (isSchedules)
            {
                list = recordSchedules.getRecordSchedules();
                if (log.isDebugEnabled())
                {
                    log.debug("processBrowseRecords() - got list cnt: " + list.size());
                }
            }
            else
            {
                list = new ArrayList();
                recordSchedules.getRecordTasks(recordScheduleID, list);
            }

            totalMatches = list.size();
            numberReturned = totalMatches - startIdx;
            if (numberReturned > reqCnt)
            {
                numberReturned = reqCnt;
            }
            else if (numberReturned < 0)
            {
                numberReturned = 0;
            }
            for (int i = 0; i < numberReturned; i++)
            {
                if (isSchedules)
                {
                    sb.append(((RecordSchedule)list.get(i)).getBrowseItem(filteredPropList, null));
                }
                else
                {
                    sb.append(((RecordTask)list.get(i)).getBrowseItem(filteredPropList));
                }
            }
        }
        sb.append(RESULT_END_XML);

        if (log.isDebugEnabled())
        {
            log.debug("processBrowseRecords() - returning result:  " + sb.toString() +
            " and number returned: " + numberReturned + ", totalMatches: " + totalMatches +
            ", updateID: " + stateUpdateID);
        }

        try
        {
            return new UPnPActionResponse(new String[] { 
                sb.toString(), 
                Integer.toString(numberReturned), 
                Integer.toString(totalMatches), 
                Long.toString(stateUpdateID)} , invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                        
        }
    }

    /**
     * Handles the response to SOAP action to get a record schedule.
     *
     * @param action SOAP action which needs response
     * @return  true if valid action request, false otherwise
     */
    private UPnPResponse processGetRecordSchedule(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processGetRecordSchedule() called");
        }

        if ((invocation.getArgumentNames() != null) &&
            (invocation.getArgumentNames().length != 2))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordSchedule() - expecting 2 args, called with arg count = "
                + invocation.getArgumentNames().length);
            }
            String values[] = invocation.getArgumentNames();
            for (int i = 0; i < values.length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("processGetRecordSchedule() - arg " + i + ": " +
                    values[i]);
                }
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String recordScheduleID = invocation.getArgumentValue(RECORD_SCHEDULE_ID_ARG_NAME);

        if (log.isDebugEnabled())
        {
            log.debug("processGetRecordSchedule(), called with argument " +  RECORD_SCHEDULE_ID_ARG_NAME + " value "
            + recordScheduleID);
        }

        if (recordScheduleID == null || recordScheduleID.equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordSchedule(), called with a null or empty objectID");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_CANNOT_PROCESS.getCode(), 
                    ActionStatus.UPNP_CANNOT_PROCESS.getDescription(), invocation);
        }

        RecordSchedule recordSchedule = this.recordSchedules.getRecordSchedule(recordScheduleID);

        if (recordSchedule == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordSchedule(), could not find recordSchedule for ObjectID "
                + recordScheduleID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getCode(), 
                    ActionStatus.UPNP_NO_SUCH_RECORD_SCHEDULE_TAG.getDescription(), invocation);            
        }

        // Verify filter argument and build filtered property list
        String filter = invocation.getArgumentValue(FILTER_ARG_NAME);
        if (log.isDebugEnabled())
        {
            log.debug("processGetRecordSchedule() - using filter: \"" + filter + "\"");
        }

        String allPropList[] = RecordScheduleDirectManual.getRecordScheduleProperties();
        String reqPropList[] = RecordScheduleDirectManual.getRecordSchedulePropertiesRequired();
        String filteredPropList[] = null;
        if (filter != null && isFilterValid(filter))
        {
            filteredPropList = buildFilteredPropertyList(filter, allPropList, reqPropList);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetRecordSchedule() - no filter argument supplied"
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        if (filteredPropList != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetRecordSchedule() - filtered properties: ");
            }
            for (int i = 0; i < filteredPropList.length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Filtered Prop " + (i+1) + ": " + filteredPropList[i]);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetRecordSchedule() - no filtered properties");
            }
        }

        // Create the XML document for the response
        StringBuffer sb = new StringBuffer(RESULT_BEG_XML);

        if (filteredPropList != null)
        {
            // Add an item for the record schedule
            sb.append(recordSchedule.getBrowseItem(filteredPropList, null));
        }
        sb.append(RESULT_END_XML);

        if (log.isDebugEnabled())
        {
            log.debug("processGetRecordSchedule() - returning result:  " + sb.toString() +
            " and updateID: " + stateUpdateID);
        }

        try
        {
            return new UPnPActionResponse(new String[] { sb.toString(), Long.toString(stateUpdateID) }, invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
        }
    }

    private String[] buildFilteredPropertyList(String filter, String allPropList[], String reqPropList[])
    {
        String filteredPropList[] = null;
        String csvList[] = null;

        // Get the list of properties based on the filter
        if (filter.equals(FILTER_ALL))
        {
            filteredPropList = allPropList;
        }
        else if (filter.equals(""))
        {
            filteredPropList = reqPropList;
        }
        else if ((filter.indexOf("<") != -1) && (filter.indexOf(">:*") != -1))
        {
            filteredPropList = getPropertiesFromFilterNamespace(filter, allPropList, true);
        }
        // this the case where the filter might be like ocap:* instead of
        // <ocap>:
        else
        {
            if (filter.indexOf(":*") != -1)
            {
                csvList = getPropertiesFromFilterNamespace(filter, allPropList, false);
            }
            else
            {
                // Create array of properties which are contained in the filter
                csvList = fromCSV(filter, allPropList);
            }
            
            // Add mandatory properties to the existing array if the list is not
            // empty.
            if (reqPropList != null)
            {
                filteredPropList = addRequiredProperties(csvList, reqPropList);
            }
            else
            {
                filteredPropList = (String[]) Arrays.copy(csvList, java.lang.String.class);
            }
        }
        return filteredPropList;
    }

    /**
     * @param filterProperties
     *            Properties filtered using the filter parameter
     * @param requiredProperties
     *            All required properties
     * @return merged array with all required properties and the filtered
     *         properties
     */
    private String[] addRequiredProperties(String[] filterProperties, String[] requiredProperties)
    {

        // Combine the comma separated value list with required list
        ArrayList list = new ArrayList();
        for (int i = 0; i < requiredProperties.length; i++)
        {
            list.add(requiredProperties[i]);
        }

        if (filterProperties != null)
        {
            for (int i = 0; i < filterProperties.length; i++)
            {
                if (!list.contains(filterProperties[i]))
                {
                    list.add(filterProperties[i]);
                }
            }
        }

        String[] filteredPropList = new String[list.size()];
        for (int i = 0; i < filteredPropList.length; i++)
        {
            filteredPropList[i] = (String) list.get(i);
        }

        return filteredPropList;
    }

    private boolean isFilterValid(String filter)
    {
        boolean isValid = true;

        if (filter.indexOf("*:,") != -1)
        {
            isValid = false;
        }
        return isValid;
    }

    /**
     * Handles the response to SOAP action to get the property list.
     *
     * @param action SOAP action which needs response
     * @return  true if valid action request, false otherwise
     */
    private UPnPResponse processGetPropertyList(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processGetPropertyList() called");
        }

        if ((invocation.getArgumentNames() == null) ||
            (invocation.getArgumentNames().length > 1))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetPropertyList() called with wrong number of arguments");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String dataType = invocation.getArgumentValue(DATA_TYPE_ID_ARG_NAME);
        
        if (dataType == null || dataType.length() == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetPropertyList() - missing DataTypeID arg");
            }
            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        if (!isValidDataType(dataType))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetPropertyList() called with Invalid DataTypeID");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_RESTRICTED_OBJECT.getCode(), 
                    ActionStatus.UPNP_RESTRICTED_OBJECT.getDescription(), invocation);
        }

        String propList = null;
        if (dataType.equals(DATA_TYPE_RECORD_SCHEDULE))
        {
            propList = RecordScheduleDirectManual.getRecordSchedulePropertyCSVStr();
        }
        else if (dataType.equals(DATA_TYPE_RECORD_TASK))
        {
             propList = RecordingContentItemLocal.getRecordTaskPropertyCSVStr();
        }
        else if (dataType.equals(DATA_TYPE_RECORD_SCHEDULE_PARTS))
        {
              propList = RecordScheduleDirectManual.getRecordSchedulePartsPropertyCSVStr();
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetPropertyList() called with unsupported DataTypeID: " + dataType);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_RESTRICTED_OBJECT.getCode(), 
                    ActionStatus.UPNP_RESTRICTED_OBJECT.getDescription(), invocation);
        }


        if (log.isDebugEnabled())
        {
            log.debug("processGetPropertyList() - for data type:  " + dataType +
            ", returning response: " + propList);
        }

        try
        {
            return new UPnPActionResponse(new String[] { propList }, invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
        }
    }

    /**
     * Determines if the data type id argument has a valid value
     *
     * @param args  list of arguments to check
     * @return  false if data type arg has a invalid value, true otherwise
     */
    private boolean isValidDataType(String dataType)
    {
        if (!dataType.equals(DATA_TYPE_RECORD_SCHEDULE) &&
                !dataType.equals(DATA_TYPE_RECORD_TASK) &&
                !dataType.equals(DATA_TYPE_RECORD_SCHEDULE_PARTS))
        {
            return false;
        }

        return true;
    }

    /**
     * Convert the array of strings into a single comma separated value string
     *
     * @param values    array of string to use when creating comma separated value string
     * @return  string of comma separated values
     */
    private String toCSV(String values[])
    {
        String csv = "";
        if (values != null)
        {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < values.length; i++)
            {
                if (i > 0)
                {
                    sb.append(",");
                }
                sb.append(values[i]);
            }
            csv = sb.toString();
        }
        return csv;
    }

    /**
     * Create an array of strings based on supplied comma separated value list
     *
     * @param values    string containing list of comma separated values
     * @return  array of individual strings
     */
    private String[] fromCSV(String values, String allPropList[])
    {
        String csv[] = null;
        ArrayList al = new ArrayList();
        if (values != null)
        {
            StringTokenizer st = new StringTokenizer(values, ",");
            while (st.hasMoreTokens())
            {
                String csvProp = st.nextToken().trim();

                // Make sure this is a valid property
                boolean isValid = false;
                for (int i = 0; i < allPropList.length; i++)
                {
                    if (csvProp.equalsIgnoreCase(allPropList[i]))
                    {
                        isValid = true;
                        break;
                    }
                }
                if (isValid)
                {
                    al.add(csvProp);
                }
            }

            csv = new String[al.size()];
            for (int i = 0; i < csv.length; i++)
            {
                csv[i] = (String)al.get(i);
            }
         }

        return csv;
    }

    /**
     * Supplied filter is namespace, so build list of properties which match
     * this namespace.
     * 
     * @param filter
     *            filter supplied as action argument
     * @param propNames
     *            list of property names to filter
     * @param nameSpaceAvailable
     *            boolean to indicate if the namespace is available or not
     * @return filtered list of property names to use
     */
    private String[] getPropertiesFromFilterNamespace(String filter, String propNames[], boolean nameSpaceAvailable)
    {
        // Determine the namespace specified in the filter
        String nsPrefix = null;
        if (nameSpaceAvailable)
        {
            int startIdx = filter.indexOf("<");
            if (startIdx != -1)
            {
                int endIdx = filter.indexOf(">:*");
                if ((endIdx != -1) && (endIdx > startIdx))
                {
                    nsPrefix = filter.substring(startIdx + 1, endIdx) + ":";
                }
            }
        }
        else
        {
            int endIdx = filter.indexOf(":*");
            if (endIdx > 0)
            {
                nsPrefix = filter.substring(0, endIdx) + ":";;
            }
        }

        String props[] = null;
        if (nsPrefix != null)
        {
            ArrayList list = new ArrayList();
            for (int i = 0; i < propNames.length; i++)
            {
                if (propNames[i].indexOf(nsPrefix) != -1)
                {
                    list.add(propNames[i]);
                }
            }

            props = new String[list.size()];
            for (int i = 0; i < list.size(); i++)
            {
                props[i] = (String)list.get(i);
            }
        }

        return props;
    }

    /**
     * Process GetRecordTask action from UPnP network ControlPoint.
     */
    private UPnPResponse processGetRecordTask(UPnPActionInvocation invocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("processGetRecordTask() - called");
        }

        if (invocation.getArgumentNames().length < 2)
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordTask() called with less than 2 arguments. arg count = "
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        String recordTaskID = invocation.getArgumentValue(RECORD_TASK_ID_ARG_NAME);
        if (recordTaskID == null || recordTaskID.trim().equals(""))
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordTask() - RecordTaskID was null");
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }

        RecordTask recordTask = this.recordSchedules.getRecordTask(recordTaskID);

        if (recordTask == null || recordTask.getRecordingContentItem() == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("processGetRecordTask() - RecordTask not found for recordTaskID: "
                + recordTaskID);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getCode(), 
                    ActionStatus.UPNP_RECORDTASK_NOT_FOUND.getDescription(), invocation);            
        }

        // Verify filter argument and build filtered property list
        String filter = invocation.getArgumentValue(FILTER_ARG_NAME);
        if (log.isDebugEnabled())
        {
            log.debug("processGetRecordTask() - using filter: " + filter);
        }

        String filteredPropList[] = null;
        String allPropList[] = RecordingContentItemLocal.getRecordTaskProperties();
        String reqPropList[] = RecordingContentItemLocal.getRecordTaskPropertiesRequired();
        if (filter != null && isFilterValid(filter))
        {
            filteredPropList = buildFilteredPropertyList(filter, allPropList, reqPropList);
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetRecordTask() - no filter argument supplied"
                + invocation.getArgumentNames().length);
            }

            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(), 
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);
        }
        if (filteredPropList != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetRecordTask() - filtered properties: ");
            }
            for (int i = 0; i < filteredPropList.length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Filtered Prop " + (i+1) + ": " + filteredPropList[i]);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("processGetRecordTask() - no filtered properties");
            }
        }

        // Create the XML document for the response
        StringBuffer sb = new StringBuffer(RESULT_BEG_XML);
        sb.append(recordTask.getBrowseItem(filteredPropList));
        sb.append(RESULT_END_XML);

        if (log.isDebugEnabled())
        {
            log.debug("processGetRecordTask() - returning result:  " + sb.toString() +
            ", updateID: " + stateUpdateID);
        }

        try
        {
            return new UPnPActionResponse(new String[] { sb.toString(), Long.toString(stateUpdateID) }, invocation);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);                                    
        }
    }

    /**
     * Finds and returns the local ContentServerNetModule. Returns null if not
     * found.
     */
    private void initLocalContentServerNetModuleImpl()
    {
        if (contentServerNetModuleImpl == null)
        {
            NetList netList = NetManagerImpl.getInstance().getNetModuleList(null);

            Enumeration enumeration = netList.getElements();

            while (enumeration.hasMoreElements())
            {
                Object obj = enumeration.nextElement();

                if (obj instanceof ContentServerNetModuleImpl)
                {
                    if (((ContentServerNetModuleImpl) obj).isLocal())
                    {
                        contentServerNetModuleImpl = ((ContentServerNetModuleImpl) obj);
                        break;
                    }
                }
            }
        }

        if (contentServerNetModuleImpl == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("initLocalContentServerNetModuleImpl() could not find local ContentServerNetModuleImpl");
            }
        }
    }

    /**
     * Returns a ContentEntry instance built from a CDS item referenced by input
     * ObjectID. Returns null if can't find the local ContentServerNetModule or
     * CDS entry doesn't exist.
     */
    public ContentEntry getContentEntry(String objectID)
    {
        if (objectID == null)
        {
            return null;
        }

        if (contentServerNetModuleImpl == null)
        {
            // Find local ContentServerNetModuleImpl
            initLocalContentServerNetModuleImpl();

            if (contentServerNetModuleImpl == null)
            {
                return null; // couldn't find
            }
        }

        // lookup RecordingContentItem or NetRecordingEntry by CDS objectID

        ContentDirectoryService cds = MediaServer.getInstance().getCDS();

        ContentEntry contentEntry = cds.getRootContainer().getEntry(objectID);
        if (log.isDebugEnabled())
        {
            if (contentEntry != null)
        {
            MetadataNode node = contentEntry.getRootMetadataNode();
            String recordScheduleID = (String) node.getMetadata(RecordingActions.RECORD_SCHEDULE_ID_KEY);
            String recordTaskID = (String) node.getMetadata(RecordingActions.RECORD_TASK_ID_KEY);
            
            if (log.isDebugEnabled())
        {
            log.debug("getContentEntry() contentEntry recordScheduleID = " + recordScheduleID + " recordTaskID = "
            + recordTaskID);
        }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getContentEntry() returned null");
                }
            }
        }

        return contentEntry;
    }


    /**
     * Perform the necessary logic to support this action invocation for this 
     * Scheduled Recording Service.
     * 
     * @param   action  requested action to be performed
     * 
     * @return  response based on action invocation
     */
    public UPnPResponse notifyActionReceived(UPnPActionInvocation invocation)
    {
        UPnPAction action = invocation.getAction();
        String actionName = action.getName();
        
        String[] args = action.getArgumentNames();
        String[] values = new String[args.length];
        
        for(int i = 0; i < args.length; i++)
        {
            values[i] = invocation.getArgumentValue(args[i]);
            if (log.isDebugEnabled())
            {
                log.debug(actionName + " param = " + args[i] + " = " + values[i]);
            }
        }
        
        InetAddress client = ((UPnPActionImpl)action).getInetAddress();
        final String[] requestStrings = ((UPnPActionImpl)action).getRequestStrings();
        NetworkInterface netInt = ((UPnPActionImpl)action).getNetworkInterface();
        
        // These are actions required to be implemented
        if(RecordingActions.BROWSE_RECORD_SCHEDULES_ACTION_NAME.equals(actionName) ||
                RecordingActions.BROWSE_RECORD_TASKS_ACTION_NAME.equals(actionName) ||
                RecordingActions.CREATE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName) ||
                RecordingActions.DISABLE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName) ||
                RecordingActions.DISABLE_RECORD_TASK_ACTION_NAME.equals(actionName) ||
                RecordingActions.DELETE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName) ||
                RecordingActions.DELETE_RECORD_TASK_ACTION_NAME.equals(actionName) ||
                RecordingActions.ENABLE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName) ||
                RecordingActions.GET_ALLOWED_VALUES_ACTION_NAME.equals(actionName) ||
                RecordingActions.GET_PROPERTY_LIST_ACTION_NAME.equals(actionName) ||
                RecordingActions.GET_RECORD_SCHEDULE_ACTION_NAME.equals(actionName) ||
                RecordingActions.GET_RECORD_TASK_ACTION_NAME.equals(actionName) ||
                RecordingActions.GET_RECORD_TASK_CONFLICTS_ACTION_NAME.equals(actionName) ||
                RecordingActions.GET_SORT_CAPABILITIES_ACTION_NAME.equals(actionName) ||
                RecordingActions.GET_STATE_UPDATE_ID_ACTION_NAME.equals(actionName) ||
                RecordingActions.X_PRIORITIZE_RECORDINGS_ACTION_NAME.equals(actionName))
        {
            // Begin NetSecurityManager authorization before processing any valid action        
            if (!securityManager.notifyAction(SERVICEID + actionName, client, "", -1, requestStrings, netInt))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentDirectoryService.browseAction() - unauthorized");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                        ActionStatus.UPNP_UNAUTHORIZED.getDescription(), invocation);
            }

            UPnPResponse actionResponse = null;            
        
            // Support the actions called out to be required in UPnP CDS Spec
            if(RecordingActions.CREATE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName))
            {
                actionResponse = performCreateRecordScheduleAction(invocation, client);
            }
            else if(RecordingActions.ENABLE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName))
            {
                actionResponse = performEnableRecordScheduleAction(invocation);
            }
            else if(RecordingActions.DISABLE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName))
            {
                actionResponse = processDisableRecordSchedule(invocation);
            }
            else if(RecordingActions.DISABLE_RECORD_TASK_ACTION_NAME.equals(actionName))
            {
                actionResponse = this.processDisableRecordTask(invocation);
            }
            else if(RecordingActions.DELETE_RECORD_SCHEDULE_ACTION_NAME.equals(actionName))
            {
                actionResponse = processDeleteRecordSchedule(invocation);
            }
            else if(RecordingActions.DELETE_RECORD_TASK_ACTION_NAME.equals(actionName))
            {
                actionResponse = processDeleteRecordTask(invocation);
            }
            else if(RecordingActions.GET_RECORD_TASK_CONFLICTS_ACTION_NAME.equals(actionName))
            {
                actionResponse = processGetRecordTaskConflicts(invocation);
            }
            else if(RecordingActions.GET_SORT_CAPABILITIES_ACTION_NAME.equals(actionName))
            {
                actionResponse = processGetSortCapabilities(invocation);
            }
            else if(RecordingActions.GET_STATE_UPDATE_ID_ACTION_NAME.equals(actionName))
            {
                actionResponse = processGetStateUpdateID(invocation);
            }
            else if(RecordingActions.GET_ALLOWED_VALUES_ACTION_NAME.equals(actionName))
            {
                actionResponse = processGetAllowedValues(invocation);
            }
            else if(RecordingActions.BROWSE_RECORD_TASKS_ACTION_NAME.equals(actionName))
            {
                actionResponse = processBrowseRecordTasks(invocation);
            }
            else if(RecordingActions.GET_RECORD_SCHEDULE_ACTION_NAME.equals(actionName))
            {
                actionResponse = processGetRecordSchedule(invocation);
            }
            else if(RecordingActions.GET_PROPERTY_LIST_ACTION_NAME.equals(actionName))
            {
                actionResponse = processGetPropertyList(invocation);
            }
            else if(RecordingActions.GET_RECORD_TASK_ACTION_NAME.equals(actionName))
            {
                actionResponse = processGetRecordTask(invocation);
            }
            else if(RecordingActions.BROWSE_RECORD_SCHEDULES_ACTION_NAME.equals(actionName))
            {
                actionResponse = processBrowseRecordSchedules(invocation);
            }
            else if(RecordingActions.X_PRIORITIZE_RECORDINGS_ACTION_NAME.equals(actionName))
            {
                actionResponse = this.processX_PrioritizeRecordings(invocation);
            }
          
            // End NetSecurityManager
            securityManager.notifyActionEnd(client, SERVICEID + actionName);

            return actionResponse;
        }

        if (log.isWarnEnabled())
        {
            log.warn("notifyActionReceived() - unsupported action: " + actionName);
        }            
        return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ACTION.getCode(),
                ActionStatus.UPNP_INVALID_ACTION.getDescription(), invocation);
    }

    /**
     * Method called when RI ScheduledRecordingService's UPnPActionHandler has been replaced.
     * 
     * @param   replacement new handler
     */
    public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
    {
        if (log.isWarnEnabled())
        {
            log.warn("notifyActionHandlerReplaced() - RI's Scheduled Recording Service has been replaced by: " +
            replacement.getClass().getName());
        }
    }
}
