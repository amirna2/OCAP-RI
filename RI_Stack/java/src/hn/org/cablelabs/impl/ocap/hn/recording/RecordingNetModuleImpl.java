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

package org.cablelabs.impl.ocap.hn.recording;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.NetActionEventImpl;
import org.cablelabs.impl.ocap.hn.NetActionRequestImpl;
import org.cablelabs.impl.ocap.hn.NetModuleActionInvocation;
import org.cablelabs.impl.ocap.hn.NetModuleImpl;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordingActions;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.ocap.hn.Device;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetModuleEventListener;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.NetRecordingSpec;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.recording.RecordingNetModule;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPResponse;

import org.cablelabs.impl.util.SecurityUtil;

/**
 * RecordingNetModuleImpl - implementation class for
 * <code>RecordingNetModule</code>
 *
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * @author Dan Woodard (Flashlight Engineering and Consulting)
 *
 * @version $Revision$
 *
 * @see {@link org.ocap.hn.recording.RecordingNetModule}
 */
public class RecordingNetModuleImpl extends NetModuleImpl implements RecordingNetModule, UPnPActionResponseHandler
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(RecordingNetModuleImpl.class);

    /**
     * permissions
     */
    private static final HomeNetPermission RECORDING_PERMISSION = new HomeNetPermission("recording");
    
    public RecordingNetModuleImpl(UPnPClientService service)
    {
        super(service);
    }

    /**
     * Override implementation of NetModule method.
     */
    public String getNetModuleType()
    {
        return NetModule.CONTENT_RECORDER;
    }

    /**
     * Implementation of RecordingNetModule.requestDelete().
     */
    public NetActionRequest requestDelete(ContentEntry recording, NetActionHandler handler)
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestDelete() called with " + (recording != null ? recording.getClass().getName() : "null"));
        }

        SecurityUtil.checkPermission(RECORDING_PERMISSION);

        // get the CDS ObjectID parameter
        String objectID = recording instanceof RecordingContentItem || recording instanceof NetRecordingEntry
                            ? recording.getID()
                            : null;

        if (objectID == null || objectID.equals(""))
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestDelete() called with ContentEntry that did not contain a CDS objectID");
            }

            return null;
        }
        // else we have the objectID parameter for the action request

        //get the ContentServerNetModule in the Device
        ContentServerNetModuleImpl contentServerNetModule = getContentServerNetModuleImpl();

        if (contentServerNetModule == null) // didn't find it
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestDelete() - Could not find ContentServerNetModule in the Device that contains this RecordingNetModule");
            }

            return null;
        }

        UPnPAction action = contentServerNetModule.getActionByName(ContentDirectoryService.DESTROY_OBJECT);
        ActionRequest request = new ActionRequest(handler);
        UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { objectID }, action, request);

        if (log.isDebugEnabled())
        {
            log.debug("Calling doAction for requestDelete()");
        }

        UPnPClientService service = (UPnPClientService)action.getService();
        service.postActionInvocation(invocation, this);

        return request;
    }

    /**
     * Implementation for RecordingNetModule.requestDeleteService()
     */
    public NetActionRequest requestDeleteService(ContentEntry recording, NetActionHandler handler)
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestDeleteService() called");
        }

        SecurityUtil.checkPermission(RECORDING_PERMISSION);

        if (recording instanceof RecordingContentItem)
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestDeleteService() called - RecordingContentItem");
            }

            MetadataNodeImpl metadataNode = (MetadataNodeImpl) ((RecordingContentItem) recording).getRootMetadataNode();
            String objectID = (String) metadataNode.getMetadata(RecordingActions.RECORD_TASK_ID_KEY);// RecordTask objectID

            if (objectID == null || objectID.equals(""))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("requestDeleteService() called with - RecordingContentItem that did not contain a RecordTask objectID");
                }
                return null;
            }

            UPnPAction action = getActionByName(RecordingActions.DELETE_RECORD_TASK_ACTION_NAME);
            ActionRequest request = new ActionRequest(handler);
            UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { objectID }, action, request);            

            if (log.isDebugEnabled())
            {
                log.debug("Calling doAction for requestDeleteService() - RecordingContentItem");
            }

            // perform action for the DeleteService request
            UPnPClientService service = (UPnPClientService)action.getService();
            service.postActionInvocation(invocation, this);

            return request;

        }
        else if (recording instanceof NetRecordingEntry)
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestDeleteService() called - NetRecordingEntry");
            }

            MetadataNodeImpl metadataNode = (MetadataNodeImpl) ((NetRecordingEntry) recording).getRootMetadataNode();
            String objectID = (String) metadataNode.getMetadata(RecordingActions.RECORD_SCHEDULE_ID_KEY);// RecordSchedule objectID

            if (objectID == null || objectID.equals(""))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("requestDeleteService() called with - NetRecordingEntry that did not contain a RecordSchedule objectID");
                }
                return null;
            }

            UPnPAction action = getActionByName(RecordingActions.DELETE_RECORD_SCHEDULE_ACTION_NAME);
            ActionRequest request = new ActionRequest(handler);
            UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { objectID }, action, request);

            if (log.isDebugEnabled())
            {
                log.debug("Calling doAction for requestDeleteService() - NetRecordingEntry ID= " + objectID);
            }

            // perform action for the DeleteService request
            UPnPClientService service = (UPnPClientService)action.getService();
            service.postActionInvocation(invocation, this);

            return request;

        }
        else
        {
            /**
             * A requestDeleteService can only be performed on a
             * RecordingContentItem or a NetRecordingEntry. Otherwise, null is
             * returned for the NetActionRequest indicating that the request was
             * not valid.
             */
            return null;
        }
    }

    /**
     * Implementation of RecordingNetModule.requestDisable.
     */
    public NetActionRequest requestDisable(ContentEntry recording, NetActionHandler handler)
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestDisable() called");
        }

        SecurityUtil.checkPermission(RECORDING_PERMISSION);

        if (recording instanceof RecordingContentItem)
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestDisable() called - RecordingContentItem");
            }

            MetadataNodeImpl metadataNode = (MetadataNodeImpl) ((RecordingContentItem) recording).getRootMetadataNode();
            String recordTaskID = (String) metadataNode.getMetadata(RecordingActions.RECORD_TASK_ID_KEY);// RecordTask objectID

            if (recordTaskID == null || recordTaskID.equals(""))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("requestDisable() called with - RecordingContentItem that did not contain a RecordTask objectID");
                }
                return null;
            }

            UPnPAction action = getActionByName(RecordingActions.DISABLE_RECORD_TASK_ACTION_NAME);
            ActionRequest request = new ActionRequest(handler);
            UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { recordTaskID }, action, request);

            if (log.isDebugEnabled())
            {
                log.debug("Calling doAction for requestDisable() - RecordingContentItem");
            }

            // perform action for the Disable request
            UPnPClientService service = (UPnPClientService)action.getService();
            service.postActionInvocation(invocation, this);

            return request;

        }
        else if (recording instanceof NetRecordingEntry)
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestDisable() called - NetRecordingEntry");
            }

            MetadataNodeImpl metadataNode = (MetadataNodeImpl) ((NetRecordingEntry) recording).getRootMetadataNode();
            String recordScheduleID = (String) metadataNode.getMetadata(RecordingActions.RECORD_SCHEDULE_ID_KEY);// RecordSchedule objectID

            if (recordScheduleID == null || recordScheduleID.equals(""))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("requestDisable() called with - NetRecordingEntry that did not contain a RecordSchedule objectID");
                }
                return null;
            }


            UPnPAction action = getActionByName(RecordingActions.DISABLE_RECORD_SCHEDULE_ACTION_NAME);
            ActionRequest request = new ActionRequest(handler);
            UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { recordScheduleID }, action, request);
            
            if (log.isDebugEnabled())
            {
                log.debug("Calling doAction for requestDisable() - NetRecordingEntry recordScheduleID= "
                + recordScheduleID);
            }

            // perform action for the Disable request

            UPnPClientService service = (UPnPClientService)action.getService();
            service.postActionInvocation(invocation, this);

            return request;

        }
        else
        {
            /**
             * A requestDisable can only be performed on a RecordingContentItem
             * or a NetRecordingEntry. Otherwise, null is returned for the
             * NetActionRequest indicating that the request was not valid.
             */
            return null;
        }
    }

    /**
     * Implementation for
     * RecordingNetModule.requestPrioritize(RecordingContentItem[],...)
     */
    public NetActionRequest requestPrioritize(RecordingContentItem[] recordings, NetActionHandler handler)
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestPrioritize( RecordingContentItem[],...) called");
        }

        SecurityUtil.checkPermission(RECORDING_PERMISSION);

        if (recordings == null || recordings.length < 1)
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestPrioritize(RecordingContentItem[],...) called with null or 0 length RecordingContentItem[]");
            }

            return null;
        }

        UPnPAction action = getActionByName(RecordingActions.X_PRIORITIZE_RECORDINGS_ACTION_NAME);

        String recordTaskIDs = "";

        // get array of RecordTask IDs from RecordingContentItem MetadataNode
        //
        for (int i = 0; i < recordings.length; i++)
        {
            RecordingContentItem rci = recordings[i];
            MetadataNode node = rci.getRootMetadataNode();

            String id = (String) node.getMetadata(RecordingActions.RECORD_TASK_ID_KEY);

            if (id == null || id.equals(""))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("requestPrioritize( RecordingContentItem[],) error, could not find upnp:srsRecordTaskID in MetadataNode");
                }
                return null;
            }

            if (recordTaskIDs.equals(""))
            {
                recordTaskIDs += id;
            }
            else
            {
                recordTaskIDs += "," + id;
            }

        }

        ActionRequest request = new ActionRequest(handler);
        UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { recordTaskIDs }, action, request);

        if (log.isDebugEnabled())
        {
            log.debug("Calling X_PrioritizeRecordings doAction for requestPrioritize() - RecordingContentItem[]");
        }

        // perform action for the Prioritize request
        UPnPClientService service = (UPnPClientService)action.getService();
        service.postActionInvocation(invocation, this);

        return request;
    }

    /**
     * Implementation for RecordingNetModule.requestPrioritize(
     * NetRecordingEntry[],... )
     */
    public NetActionRequest requestPrioritize(NetRecordingEntry[] recordings, NetActionHandler handler)
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestPrioritize( NetRecordingEntry[],...) called");
        }

        SecurityUtil.checkPermission(RECORDING_PERMISSION);

        if (recordings == null || recordings.length < 1)
        {
            if (log.isDebugEnabled())
            {
                log.debug("requestPrioritize(NetRecordingEntry[]) called with null or 0 length NetRecordingEntry[]");
            }

            return null;
        }

        UPnPAction action = getActionByName(RecordingActions.X_PRIORITIZE_RECORDINGS_ACTION_NAME);

        String recordScheduleIDs = "";

        // get array of RecordTask IDs from RecordingContentItem MetadataNode
        //
        for (int i = 0; i < recordings.length; i++)
        {
            NetRecordingEntry nre = recordings[i];
            MetadataNode node = nre.getRootMetadataNode();

            String id = (String) node.getMetadata(RecordingActions.RECORD_SCHEDULE_ID_KEY);

            if (id == null || id.equals(""))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("requestPrioritize( NetRecordingEntry[],) error, could not find upnp:srsRecordScheduleID in MetadataNode");
                }
                return null;
            }

            if (recordScheduleIDs.equals(""))
            {
                recordScheduleIDs += id;
            }
            else
            {
                recordScheduleIDs += "," + id;
            }
        }

        ActionRequest request = new ActionRequest(handler);
        UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { recordScheduleIDs }, action, request);

        if (log.isDebugEnabled())
        {
            log.debug("Calling X_PrioritizeRecordings doAction for requestPrioritize() - RecordingContentItem[]");
        }

        // perform action for the Prioritize request
        UPnPClientService service = (UPnPClientService)action.getService();
        service.postActionInvocation(invocation, this);

        return request;
    }

    /**
     * Implementation for RecordingNetModule.requestReschedule()
     */
    public NetActionRequest requestReschedule(ContentEntry recording, NetRecordingSpec recordingSpec,
            NetActionHandler handler)
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestReschedule() called");
        }

        SecurityUtil.checkPermission(RECORDING_PERMISSION);

        MetadataNodeImpl metadataNode = (MetadataNodeImpl) recordingSpec.getMetadata();

        // create the CreateRecordSchedule() action request parameter
        RecordScheduleDirectManual parts = new RecordScheduleDirectManual(metadataNode,
                RecordScheduleDirectManual.RECORD_SCHEDULE_PARTS_USAGE);

        if (!parts.isValid())
        {
            throw new IllegalArgumentException("Invalid metadata.");
        }

        // set "srs:@id" value to RecordScheduleID or RecordTaskID
        //
        String srsId = null;
        MetadataNode node = recording.getRootMetadataNode();

        if (recording instanceof NetRecordingEntry)
        {
            srsId = (String) node.getMetadata(RecordingActions.RECORD_SCHEDULE_ID_KEY);
        }
        else if (recording instanceof RecordingContentItem)
        {
            srsId = (String) node.getMetadata(RecordingActions.RECORD_TASK_ID_KEY);
        }
        else
        {
            throw new IllegalArgumentException();
        }

        if (srsId == null || srsId.equals(""))
        {
            throw new IllegalArgumentException();
        }

        parts.setValue(UPnPConstants.QN_SRS_ID_ATTR, srsId);

        UPnPAction action = getActionByName(RecordingActions.CREATE_RECORD_SCHEDULE_ACTION_NAME);

        // get xml elements from parts instance
        StringBuffer sb = new StringBuffer("<srs ");
        sb.append("xmlns=\"urn:schemas-upnp-org:av:srs\" ");
        sb.append("xmlns:ocap=\"urn:schemas-cablelabs-org:metadata-1-0/\" ");
        sb.append("xmlns:ocapApp=\"urn:schemas-opencable-com:ocap-application\" ");
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        sb.append("xsi:schemaLocation=\"urn:schemas-upnp-org:av:srs http://www.upnp.org/schemas/av/srs-v1-20060531.xsd\">\n");
        sb.append(parts.toXMLString(null));
        sb.append("</srs>");

        String elements = sb.toString();

        if (log.isDebugEnabled())
        {
            log.debug("Create Elements parameter " + elements);
        }

        ActionRequest request = new ActionRequest(handler);
        UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { elements }, action, request);

        if (log.isDebugEnabled())
        {
            log.debug("Calling doAction for requestReschedule");
        }

        // do the action
        UPnPClientService service = (UPnPClientService)action.getService();
        service.postActionInvocation(invocation, this);

        return request;
    }

    /**
     * Implementation of RecordingNetModule.requestSchedule().
     */
    public NetActionRequest requestSchedule(NetRecordingSpec recording, NetActionHandler handler)
    {
        if (log.isDebugEnabled())
        {
            log.debug("requestSchedule() called");
        }

        SecurityUtil.checkPermission(RECORDING_PERMISSION);

        MetadataNodeImpl metadataNode = (MetadataNodeImpl) recording.getMetadata();

        // create the CreateRecordSchedule() action request parameter
        RecordScheduleDirectManual parts = new RecordScheduleDirectManual(metadataNode,
                RecordScheduleDirectManual.RECORD_SCHEDULE_PARTS_USAGE);

        if (!parts.isValid())
        {
            throw new IllegalArgumentException("Invalid metadata.");
        }

        UPnPAction action = getActionByName(RecordingActions.CREATE_RECORD_SCHEDULE_ACTION_NAME);

        // get xml elements from parts instance
        StringBuffer sb = new StringBuffer("<srs ");
        sb.append("xmlns=\"urn:schemas-upnp-org:av:srs\" ");
        sb.append("xmlns:ocap=\"urn:schemas-cablelabs-org:metadata-1-0/\" ");
        sb.append("xmlns:ocapApp=\"urn:schemas-opencable-com:ocap-application\" ");
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        sb.append("xsi:schemaLocation=\"urn:schemas-upnp-org:av:srs http://www.upnp.org/schemas/av/srs-v1-20060531.xsd\">\n");
        sb.append(parts.toXMLString(null));
        sb.append("</srs>");

        String elements = sb.toString();

        // Setup arguments
        // *TODO* - Possible spec issue - would be nice if there was a method on UPnPAction.getINArguments()
        // so this logic doesn't have to be repeated through out the code
        Vector inArgs = new Vector(); 
        String[] allArgs = action.getArgumentNames();
        for (int i = 0; i < allArgs.length; i++)
        {
            if (action.isInputArgument(allArgs[i]))
            {
                inArgs.add(allArgs[i]);
            }
        }
        String[] argNames =
            (String[])inArgs.toArray(new String[inArgs.size()]);
        String[] argVals = new String[argNames.length];
        if (log.isDebugEnabled())
        {
            log.debug("arg name length: " + argNames.length + ", values length: " + argVals.length);
        }

        for (int i = 0; i < argNames.length; i++)
        {
            if (argNames[i].equals(RecordingActions.ELEMENTS_ARG_NAME))
            {
                argVals[i] = elements;
            }
            else
            {
                argVals[i] = "";
            }
        }
        
        // Create invocation & request and then invoke action
        NetActionRequest request = new NetActionRequestImpl(handler);
        UPnPActionInvocation invocation = new NetModuleActionInvocation(argVals, action, request);
        
        ((UPnPClientService)action.getService()).postActionInvocation(invocation, this);
        ((NetActionRequestImpl)request).setActionStatus(NetActionEvent.ACTION_IN_PROGRESS);

        return request;
    }

    /**
     * Implementation of NetModule.addNetModuleEventListener
     */
    public void addNetModuleEventListener(NetModuleEventListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addNetModuleEventListener - " + listener.toString());
        }

        // The base class maintains the list of listeners
        super.addNetModuleEventListener(listener);
    }

    /**
     * Implementation of NetModule.removeNetModuleEventListener
     */
    public void removeNetModuleEventListener(NetModuleEventListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeNetModuleEventListener - " + listener.toString());
        }

        // The base class maintains the list of listeners
        super.removeNetModuleEventListener(listener);
    }

    /**
     * Notifies the listener that the value of the UPnP state variable being
     * listened to has changed.
     *
     * @param variable The UPnP state variable that changed.
     */
    public void notifyValueChanged(UPnPClientStateVariable variable)
    {
        // *TODO* - need to move the logic from notifyEvent() to here
    }
    
    /**
     * Creates a NetRecordingEntry based on the response from a
     * CreateRecordSchedule action.
     *
     * @param response The response.
     *
     * @return A reference to the NetRecordingEntry if the operation is successful; else null.
     */
    private NetRecordingEntry buildNetRecordingEntry(UPnPActionResponse response)
    {
        String result = response.getArgumentValue(RecordingActions.RESULT_ARG_NAME);

        if (result == null || result.length() == 0)
        {
            if (log.isErrorEnabled())
            {
                log.error("buildNetRecordingEntry() - A_ARG_TYPE_RecordSchedule result is null");
            }

            return null;
        }

        NetRecordingEntry netRecordingEntry;

        RecordScheduleDirectManual[] rsdma = RecordScheduleDirectManualBuilder.build(result,
                RecordScheduleDirectManualBuilder.A_ARG_TYPE_RECORD_SCHEDULE_USAGE);

        if (rsdma.length < 1)
        {
            if (log.isErrorEnabled())
            {
                log.error("buildNetRecordingEntry() - failed to build a RecordScheduleDirectManual from an A_ARG_TYPE_RecordSchedule result");
            }

            netRecordingEntry = null;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("buildNetRecordingEntry() - built a RecordScheduleDirectManual from an A_ARG_TYPE_RecordSchedule result");
            }

            String cdsReference = rsdma[0].getCdsReference();

            if (cdsReference != null)
            {
                MetadataNodeImpl metadataNode;

                try
                {
                    metadataNode = new MetadataNodeImpl(cdsReference);
                    if (log.isDebugEnabled())
                    {
                        log.debug("buildNetRecordingEntry() - parsed cdsReference '" + cdsReference + "'");
                    }
                }
                catch (IllegalArgumentException e)
                {
                    metadataNode = null;
                    if (log.isErrorEnabled())
                    {
                        log.error("buildNetRecordingEntry() - failed to parse cdsReference '" + cdsReference + "'");
                    }
                }

                UPnPAction action = response.getActionInvocation().getAction();
                netRecordingEntry = metadataNode != null
                ? new NetRecordingEntryImpl(action, metadataNode)
                : null;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("buildNetRecordingEntry() - cdsReference from an A_ARG_TYPE_RecordSchedule result is null");
                }

                netRecordingEntry = null;
            }
        }

        return netRecordingEntry;
    }
    
    public void notifyUPnPActionResponse(UPnPResponse uResponse)
    {
        // Programmatic error if a different type.
        assert uResponse.getActionInvocation() instanceof NetModuleActionInvocation;
        
        NetModuleActionInvocation invocation = (NetModuleActionInvocation)uResponse.getActionInvocation();
        UPnPAction action = invocation.getAction();
        
        // Get NetActionEvent error code 
        int error = getNetActionEventErrorCode(uResponse);
        
        // Get NetActionEvent status code
        int actionStatus = getNetActionEventStatusCode(uResponse);
        
        // Response may be a ContentListImpl
        UPnPActionResponse aResponse = null;
        Object response = null;
        if (uResponse instanceof UPnPActionResponse)
        {
            aResponse = (UPnPActionResponse)uResponse;
        }
        else
        {
            // *TODO* - add logic to handle error response
        }
        if (aResponse != null)
        {
            // *TODO* - add cases for all supported actions
            // Handle special case ContentServerNetModule.getRootContainer() browse action
            if (action.getName().equals(RecordingActions.CREATE_RECORD_SCHEDULE_ACTION_NAME))
            {
                response = handleScheduleActionResponse(aResponse, actionStatus);
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("notifyUPnPActionResponse() - received response from unrecognized action: " +
                    action.getName());
                }            
            }            
        }
        
        // Get associated NetActionRequestImpl in order to notify associated handlers
        // *TODO* - how do we do this?  Added a method, is this the correct approach?
        NetActionRequestImpl request = (NetActionRequestImpl)invocation.getNetActionRequest();
        request.notifyHandler(new NetActionEventImpl(request, response, error, actionStatus));
    }
    
    private Object handleScheduleActionResponse(UPnPActionResponse aResponse, int actionStatus)
    {
        Object response = null;
        
        // Generate and check the ContentList if the action succeeded 
        if (actionStatus == NetActionEvent.ACTION_COMPLETED)
        {
            // Generate the netRecordingEntry if the action succeeded 
            response = createNetRecordingEntry(aResponse);
        }
        else
        {
            response = null;
        }
        
        return response;
    }
    
    /**
     * Creates or finds a NetRecordingEntry based on the response from the
     * CreateRecordSchedule action.
     *
     * @param response The response.
     *
     * @return A reference to the NetRecordingEntry if the operation is successful; else null.
     */
    private NetRecordingEntry createNetRecordingEntry(UPnPActionResponse response)
    {
        if (log.isDebugEnabled())
        {
            //IArgument[] args = response.getResponseArguments();
            String argNames[] = response.getArgumentNames();
            String argVals[] = response.getArgumentValues();
            
            if (argNames.length == argVals.length)
        {               
            for (int i = 0, n = argNames.length; i < n; i++)
        {
            if (log.isDebugEnabled())
        {
            log.debug("createNetRecordingEntry() - arg: " + argNames[i] + " = " + argVals[i]);
        }
                }
            }
        }

        NetRecordingEntry netRecordingEntry;

        String updateID = response.getArgumentValue(RecordingActions.UPDATEID_ARG_NAME);

        if (updateID != null)
        {
            //
            // create or find the NetRecordingEntry
            //
            netRecordingEntry = buildNetRecordingEntry(response);

            if (netRecordingEntry != null)
            {
                netRecordingEntry.getRootMetadataNode().addMetadata("upnp:" + 
                        RecordingActions.UPDATEID_ARG_NAME, updateID);
            }
        }
        else
        {
            netRecordingEntry = null;

            if (log.isErrorEnabled())
            {
                log.error("createNetRecordingEntry() - updateID is null");
            }
        }

        return netRecordingEntry;
    }
    
    /**
     * Find the ContentServerNetModule (CDS) It should be in the same Device as
     * this RecordingNetModule (SRS) is contained in.
     */
    private ContentServerNetModuleImpl getContentServerNetModuleImpl()
    {
        Device device = this.getDevice(); // get the Device for this
                                          // RecordingNetModule
        NetList netList = device.getNetModuleList(); // get the NetModules in
                                                     // the Device
        Enumeration enumeration = netList.getElements();
        ContentServerNetModuleImpl contentServerNetModule = null;

        while (enumeration.hasMoreElements())
        {
            Object obj = enumeration.nextElement();

            if (obj instanceof ContentServerNetModuleImpl)
            {
                contentServerNetModule = (ContentServerNetModuleImpl) obj;
                break; // found it
            }
        }

        return contentServerNetModule;
    }    
}
