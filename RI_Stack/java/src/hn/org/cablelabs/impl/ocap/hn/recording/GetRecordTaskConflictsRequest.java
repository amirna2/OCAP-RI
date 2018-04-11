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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.NetActionEventImpl;
import org.cablelabs.impl.ocap.hn.NetActionRequestImpl;
import org.cablelabs.impl.ocap.hn.NetModuleActionInvocation;
import org.cablelabs.impl.ocap.hn.recording.RecordingActions;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPResponse;

/**
 *
*/
public class GetRecordTaskConflictsRequest extends NetActionRequestImpl implements UPnPActionResponseHandler
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(GetRecordTaskConflictsRequest.class);

    private ContentServerNetModule contentServerNetModule;

    /**
     * Package private constructor.
     */
    GetRecordTaskConflictsRequest(NetActionHandler hnHandler, 
            ContentServerNetModule requestorsContentServerNetModule)
    {
        super(hnHandler);
        super.error = -1;// no error
        super.actionStatus = NetActionEvent.ACTION_IN_PROGRESS;
        this.contentServerNetModule = requestorsContentServerNetModule;
    }
    
    /**
     * Process the GetRecordTaskConflicts action return.
     */    
    public void notifyUPnPActionResponse(UPnPResponse response)
    {        
        NetModuleActionInvocation invocation = (NetModuleActionInvocation)response.getActionInvocation();
//        UPnPAction action = invocation.getAction();
//
        NetActionRequestImpl request = (NetActionRequestImpl)invocation.getNetActionRequest();
//        request.notifyHandler(new NetActionEventImpl(request, response, response.getHTTPResponseCode(), NetActionEvent.ACTION_COMPLETED));        
        
        boolean requestedContentList = false; // set to true if content list is
                                              // requested
        if (log.isDebugEnabled())
        {
            log.debug("notifyEvent() - action response received from GetRecordTaskConflicts");
        }

        /**
         * If the GetRecordTaskConflicts action succeeded, search for the CDS
         * entries.
         *
        */

        String conflictList = invocation.getArgumentValue("RecordTaskConflictIDList");
        if (conflictList.equals("")) // no conflicts
        {
            /**
             * Notify the completion of the action with an empty
             * array
             *
             */
            super.notifyHandler(new NetActionEventImpl(this, new RecordingContentItem[]{},
                    super.error, super.actionStatus));
            return;
        }

        requestedContentList = requestContentList(conflictList);
        
        if (!requestedContentList) // failure
        {
            // Notify the completion of the action with an empty list 
            notifyHandler(new NetActionEventImpl(this, null, super.error, super.actionStatus));
        }
        
    }

    /**
     * Sets the error and status fields and then notifies the registered handler.
     */
    private void notifyTheHandler(List contentList, int error, int actionStatus)
    {
        super.error = error;

        super.actionStatus = actionStatus;

        Object[] response = contentList == null ? null : contentList.toArray(new RecordingContentItem[contentList.size()]);

        notifyHandler(new NetActionEventImpl(this, response, super.error, super.actionStatus));
    }

    /**
     * Initiate a search for CDS items based on a record task conflict ID list.
     *
     * @param recordTaskConflictIDList A comma-separated list of record task
     *                                 conflict IDs.
     *
     * @return True if the search action successfully begins; else false.
     */
    private boolean requestContentList(String recordTaskConflictIDList)
    {
        NetActionHandler handler = new SearchNetActionHandler(recordTaskConflictIDList);

        NetActionRequest request =
            contentServerNetModule.requestSearchEntries
                (
                    "0",    // parentID (root container)
                    "*",    // propertyFilter (all properties)
                    0,      // startingIndex (all children)
                    0,      // requestedCount (all entries)
                    null,   // searchCriteria (all entries)
                    "",     // sortCriteria (no sort)
                    handler // handler
                );

        return request.getActionStatus() == NetActionEvent.ACTION_IN_PROGRESS;
    }

    /**
     * This is the NetActionHandler for the requestSearchEntries call.
     */
    private class SearchNetActionHandler implements NetActionHandler
    {
        private final List recordTaskConflictIDList = new ArrayList();

        /**
         * Construct this object from a record task conflict ID list.
         *
         * @param recordTaskConflictIDList A comma-separated list of record task
         *                                 conflict IDs; must be non-null and
         *                                 nonempty.
         */
        public SearchNetActionHandler(String recordTaskConflictIDList)
        {
            assert recordTaskConflictIDList != null && recordTaskConflictIDList.length() != 0;

            for (StringTokenizer st = new StringTokenizer(recordTaskConflictIDList, ","); st.hasMoreTokens(); )
            {
                this.recordTaskConflictIDList.add(st.nextToken());
            }
        }

        /**
         * Process the requestSearchEntries return.
         *
         * @param event The NetActionEvent.
         */
        public void notify(NetActionEvent event)
        {
            List conflictingRecordingList;
            int actionStatus = event.getActionStatus();

            if (actionStatus == NetActionEvent.ACTION_COMPLETED)
            {
                conflictingRecordingList = new ArrayList();

                // get the list and filter it
                List totalList = (List) event.getResponse();

                for (Iterator i = totalList.iterator(); i.hasNext(); )
                {
                    ContentEntry ce = (ContentEntry) i.next();

                    /*
                    if (LOGGING)
                    {
                        log.debug("SearchNetActionHandler.notify: testing ContentEntry with id " + ce.getID());
                    }
                    */

                    if (matches(ce))
                    {
                        conflictingRecordingList.add(ce);
                    }
                }
            }
            else
            {
                conflictingRecordingList = null;
            }

            notifyTheHandler(conflictingRecordingList, event.getError(), actionStatus);
        }

        /**
         * Determine whether a given ContentEntry represents a conflicting
         * record task.
         *
         * @param ce The ContentEntry.
         *
         * @return True if the ContentEntry represents a conflicting record task;
         *         else false.
         */
        private boolean matches(ContentEntry ce)
        {
            if (! (ce instanceof RecordingContentItem))
            {
                /*
                if (LOGGING)
                {
                    log.debug("SearchNetActionHandler.matches: ContentEntry is not a RecordingContentItem");
                }
                */

                return false;
            }

            String recordTaskID = (String) ce.getRootMetadataNode().getMetadata(RecordingActions.RECORD_TASK_ID_KEY);

            if (recordTaskID == null)
            {
                /*
                if (LOGGING)
                {
                    log.debug("SearchNetActionHandler.matches: ContentEntry has no " + RecordingNetModuleImpl.RECORD_TASK_ID_KEY + " property");
                }
                */

                return false;
            }

            /*
            if (LOGGING)
            {
                log.debug("SearchNetActionHandler.matches: ContentEntry has record task ID " + recordTaskID);
            }
            */

            for (Iterator i = recordTaskConflictIDList.iterator(); i.hasNext(); )
            {
                String recordTaskConflictID = (String) i.next();

                /*
                if (LOGGING)
                {
                    log.debug("SearchNetActionHandler.matches: matching with record task ID " + recordTaskConflictID);
                }
                */

                if (recordTaskConflictID.equals(recordTaskID))
                {
                    return true;
                }
            }

            /*
            if (LOGGING)
            {
                log.debug("SearchNetActionHandler.matches: no match");
            }
            */

            return false;
        }
    }
}
