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

package org.cablelabs.test.srs;

import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;

import javax.tv.locator.Locator;

import org.cablelabs.test.Test;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.storage.MediaStorageOption;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.NetRecordingRequestHandler;
import org.ocap.hn.recording.NetRecordingRequestManager;
import org.ocap.hn.recording.NetRecordingSpec;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.recording.RecordingNetModule;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;

/**
 * This test causes a CreateRecordSchedule action through a local
 * NetRecordingRequestManager.requestSchedule(). This is an all local test such
 * that all the requests and actions are handled on the same machine as this
 * test is run. It is meant to successfully schedule a recording and add the
 * NetRecordingEntry and RecordingContentItem to the CDS. Verification that
 * those got added to the CDS is still left TODO.
 * 
 * @author Dan Woodard
 * 
 */
public class CreateRecordSchedule extends Test
{
    public int clean()
    {
        netActionHandler = null;

        netRecordingRequestHandler = null;

        failReason = "";

        contentServerNetModule = null;

        netRecordingRequestManager = null;

        netActionRequest = null;

        rootContentContainer = null;

        return Test.TEST_STATUS_PASS;
    }

    public int execute()
    {
        this.testLogger.setPrefix("CreateRecordSchedule Test: ");
        this.testLogger.log("start");

        if (runTest())
        {
            this.testLogger.log("pass");
            return Test.TEST_STATUS_PASS;
        }
        else
        {
            this.testLogger.log("fail");
            return Test.TEST_STATUS_FAIL;
        }
    }

    public int prepare()
    {
        netActionHandler = new MyNetActionHandler();

        netRecordingRequestHandler = new MyNetRecordingRequestHandler();

        failReason = "";

        contentServerNetModule = null;

        netRecordingRequestManager = null;

        netActionRequest = null;

        rootContentContainer = null;

        return Test.TEST_STATUS_PASS;
    }

    public String getName()
    {
        return "srs.CreateRecordSchedule";
    }

    public String getDescription()
    {
        return "this is the srs.CreateRecordSchedule test";
    }

    /**
     * Runs the test. Discovers the local NetRecordingRequestManager
     * 
     * @return true for pass, false for fail
     */
    private boolean runTest()
    {
        // Find local SRS and CDS
        if (!getServices())
        {
            failExit("Could not find local NetRecordingRequestManager or ContentServerNetModule. Reason: "
                    + this.failReason);
            return false;
        }

        //
        // Get Root ContentConainer
        //

        this.netActionRequest = this.contentServerNetModule.requestRootContainer(this.netActionHandler);

        if (!this.netActionHandler.waitRequestResponse())
        {
            failExit("ContentServerNetModule.requestRootConainer response failed. Reason: "
                    + this.netActionHandler.getFailReason());
            return false;
        }

        Object obj = this.getResponseFromEvent(this.netActionHandler.getNetActionEvent());

        if (obj == null)
        {
            failExit("Could not get ContentServerNetModule.requestRootConainer response from event. Reason: "
                    + this.failReason);
            return false;
        }

        if ((obj instanceof ContentContainer) == false)
        {
            failExit("NetActionEvent did not contain a ContentContainer instance");
            return false;
        }

        this.rootContentContainer = (ContentContainer) obj;

        //
        // Make requestSchedule() request
        //
        NetRecordingSpec spec = new NetRecordingSpec();
        MetadataNode node = spec.getMetadata();

        this.buildMetadata(node);

        this.netRecordingRequestManager.setNetRecordingRequestHandler(this.netRecordingRequestHandler);

        this.netActionRequest = this.netRecordingRequestManager.requestSchedule(spec, this.netActionHandler);

        if (!this.netActionHandler.waitRequestResponse())
        {
            failExit("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                    + this.netActionHandler.getFailReason());
            return false;
        }

        obj = this.getResponseFromEvent(this.netActionHandler.getNetActionEvent());

        if (obj == null)
        {
            failExit("Could not get Response from event. Reason: " + this.failReason);
            return false;
        }

        if ((obj instanceof NetRecordingEntry) == false)
        {
            failExit("NetActionEvent did not contain a NetRecordingEntry instance");
            return false;
        }

        NetRecordingEntry entry = (NetRecordingEntry) obj;

        MetadataNode metadataNode = entry.getRootMetadataNode();

        if (metadataNode == null)
        {
            failExit("NetRecordingEntry contains a null MetadataNode.");
            return false;
        }

        testLogger.log("NetActionEvent NetRecordingEntry MetadataNode contents:");
        logMetadataNodeContents(metadataNode, 0);

        if (!checkMetadata(metadataNode))
        {
            testLogger.log("NetActionEvent NetRecordingEntry MetadataNode contents are not valid.:");
            failExit("NetRecordingEntry NetRecordingEntry MetadataNode contents are not valid.");
            return false;
        }
        return true;
    }

    /**
     * Get the local ContentServerNetModule and NetRecordingRequestManager
     * 
     * @return true if got them, false if not
     */
    private boolean getServices()
    {
        testLogger.log("getServices");
        NetList serviceList = NetManager.getInstance().getNetModuleList(null);

        if (serviceList.size() <= 0)
        {
            testLogger.log("no services found - wait 30 seconds and re-query");
            try
            {
                Thread.sleep(30000);// wait for 30 seconds in case discovery is
                                    // in progress
            }
            catch (InterruptedException e)
            {
            }

            // try again
            serviceList = NetManager.getInstance().getNetModuleList(null);
        }

        if (serviceList.size() == 0)
        {
            this.failReason = "NetManager returned 0 size NetModuleList";
            return false;
        }

        this.testLogger.log("NetManager returned NetModuleList size = " + String.valueOf(serviceList.size()));

        Enumeration enumerator = serviceList.getElements();

        //
        // find the local SRS and CDS services
        //
        while (enumerator.hasMoreElements())
        {
            Object obj = enumerator.nextElement();

            if (obj instanceof ContentServerNetModule)
            {
                if (((ContentServerNetModule) obj).isLocal())
                {
                    contentServerNetModule = (ContentServerNetModule) obj;
                }

                this.testLogger.log("found a ContentServerNetModule, isLocal = "
                        + String.valueOf(((ContentServerNetModule) obj).isLocal()) + " "
                        + ((ContentServerNetModule) obj).getDevice().getName());
            }
            else if (obj instanceof NetRecordingRequestManager)
            {
                netRecordingRequestManager = (NetRecordingRequestManager) obj;

                if (netRecordingRequestManager.isLocal())
                {
                    netRecordingRequestManager = (NetRecordingRequestManager) obj;
                }

                this.testLogger.log("found a NetRecordingRequestManager, isLocal = "
                        + String.valueOf(((NetRecordingRequestManager) obj).isLocal()) + " "
                        + ((NetRecordingRequestManager) obj).getDevice().getName());
            }
            else if (obj instanceof RecordingNetModule)
            {
                this.testLogger.log("found a RecordingNetModule, isLocal = "
                        + String.valueOf(((RecordingNetModule) obj).isLocal()) + " "
                        + ((RecordingNetModule) obj).getDevice().getName());
            }
            else if (obj instanceof NetModule)
            {
                this.testLogger.log("found a NetModule, isLocal = " + String.valueOf(((NetModule) obj).isLocal()));
            }
            else
            {
                this.testLogger.log("found unknown type, isLocal = " + String.valueOf(((NetModule) obj).isLocal())
                        + ", ignoring");
            }
        }

        if (contentServerNetModule == null)
        {
            this.failReason = "could not find ContentServerNetModule";

            return false;
        }
        else if (netRecordingRequestManager == null)
        {
            this.failReason = "could not find NetRecordingRequestManager";

            return false;
        }

        // found local SRS and CDS services
        return true;
    }

    /**
     * Failure log and cleanup
     * 
     * @param reason
     *            The failure reason string to output to log.
     */
    private void failExit(String reason)
    {
        this.testLogger.logError("FAIL: " + reason);
        this.setFailedDescription(reason);
        this.netActionHandler = null;
        this.contentServerNetModule = null;
        this.netRecordingRequestManager = null;
        this.netActionRequest = null;

    }

    private Object getResponseFromEvent(NetActionEvent event)
    {
        if (event == null)
        {
            return null;
        }

        NetActionRequest receivedNetActionRequest = event.getActionRequest();
        int receivedActionStatus = event.getActionStatus();
        int receivedError = event.getError();
        Object receivedResponse = event.getResponse();

        // check that request is same as sent
        if (receivedNetActionRequest != this.netActionRequest) // compare
                                                               // references
                                                               // only
        {
            failReason = "Recieved NetActionRequest is not the same as the request return value.";
            return null;
        }

        // Check action status
        if (receivedActionStatus != NetActionEvent.ACTION_COMPLETED)
        {
            failReason = "The NetActionRequest returned ActionStatus = ";

            if (receivedActionStatus == NetActionEvent.ACTION_CANCELED)
            {
                failReason += "ACTION_CANCELED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_FAILED)
            {
                failReason += "ACTION_FAILED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_IN_PROGRESS)
            {
                failReason += "ACTION_IN_PROGRESS ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_STATUS_NOT_AVAILABLE)
            {
                failReason += "ACTION_STATUS_NOT_AVAILABLE ";
            }
            else
            {
                failReason += "UNKONWN ACTION STATUS value=" + receivedActionStatus;
            }

            failReason += ", Error value " + String.valueOf(receivedError);

            return null;
        }

        return receivedResponse;
    }

    private class MyNetRecordingRequestHandler implements NetRecordingRequestHandler
    {

        public boolean notifyDelete(InetAddress arg0, ContentEntry arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean notifyDeleteService(InetAddress arg0, ContentEntry arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean notifyDisable(InetAddress arg0, ContentEntry arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean notifyPrioritization(InetAddress arg0, NetRecordingEntry[] arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean notifyPrioritization(InetAddress arg0, ResourceUsage[] arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean notifyReschedule(InetAddress arg0, ContentEntry arg1, NetRecordingEntry arg2)
        {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * Creates a RecordingContentItem using MetdataNode contents sent from
         * remote client via requestSchedule(spec);
         */
        private RecordingContentItem createRecordingContentItem(MetadataNode node)
        {
            OcapRecordingManager recordingManager = (OcapRecordingManager) org.ocap.shared.dvr.RecordingManager.getInstance();

            try
            {
                StorageProxy proxy = StorageManager.getInstance().getStorageProxies()[0];
                StorageOption[] options = proxy.getOptions();
                for (int i = 0; i < options.length; i++)
                {
                    if (options[i] instanceof MediaStorageOption)
                    {
                        ((MediaStorageOption) options[i]).allocateMediaVolume("test",
                                new ExtendedFileAccessPermissions(true, true, true, true, true, true, null, null));
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            // using source id 0x709/1801 since it signals pmt & pat - not
            // looking up the service
            int locator = 1801;

            testLogger.log("MyNetRecordingRequestHandler: createRecordingContentItem() using sourceID: " + locator);

            // TODO: use values from input MetadataNode, for now just make up
            // our own properties
            Locator[] la = new Locator[1];
            try
            {
                la[0] = new org.ocap.net.OcapLocator(locator);
            }
            catch (Exception e)
            {
                testLogger.log("MyNetRecordingRequestHandler: createRecordingContentItem() Exception when creating OcapLocator: "
                        + e.toString());
                return null;
            }

            OcapRecordingProperties properties = new org.ocap.dvr.OcapRecordingProperties(
                    OcapRecordingProperties.HIGH_BIT_RATE, 20000L, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, new ExtendedFileAccessPermissions(true, true, true,
                            true, true, true, new int[0], new int[0]), (String) null, (MediaStorageVolume) null);

            LocatorRecordingSpec spec;

            Date startDate = new Date(System.currentTimeMillis() + 5000L);// 5
                                                                          // seconds
                                                                          // in
                                                                          // the
                                                                          // future

            try
            {
                spec = new LocatorRecordingSpec(la, startDate, 7000L, properties);
            }
            catch (Exception e)
            {
                testLogger.log("MyNetRecordingRequestHandler: createRecordingContentItem() Exception when creating LocatorRecordingSpec: "
                        + e.toString());
                return null;
            }

            RecordingRequest request;

            try
            {
                request = recordingManager.record(spec);
            }
            catch (Exception e)
            {
                testLogger.log("MyNetRecordingRequestHandler: createRecordingContentItem() Exception when creating recording: "
                        + e.toString());
                return null;
            }

            if ((request instanceof RecordingContentItem) == false)
            {
                testLogger.log("MyNetRecordingRequestHandler: createRecordingContentItem() ERROR: returned recordingRequest is not a RecordingContentItem");
                return null;
            }

            if (rootContentContainer.isLocal() == false)
            {
                testLogger.log("MyNetRecordingRequestHandler: createRecordingContentItem() ERROR: rootContentContainer is not local.");
                return null;
            }

            return (RecordingContentItem) request;
        }

        /**
         * This is where the NetRecordingEntry is added to the ContentContainer,
         * a RecordingContentItem is created and added both the
         * NetRecordingEntry and the ContentContainer.
         */
        public boolean notifySchedule(InetAddress arg0, NetRecordingEntry netRecordingEntry)
        {
            testLogger.log("MyNetRecordingRequestHandler: notifySchedule() called with InetAddress= " + arg0);

            // The NetRecordingEntry must be local to the SRS.
            if (netRecordingEntry.isLocal() == false)
            {
                testLogger.log("MyNetRecordingRequestHandler: notifySchedule() ERROR:  netRecordingEntry is not local.");
                return false;
            }

            //
            // Add the NetRecordingEntry to the CDS. This will create the SRS
            // RecordSchedule and associate it with the NetRecordingEntry.
            // This is required before a RecordContentItem is added to the
            // NetRecordingEntry and the CDS.
            //
            testLogger.log("MyNetRecordingRequestHandler: notifySchedule() adding NetRecordingEntry to ContentContainer.");
            if (rootContentContainer.addContentEntry(netRecordingEntry) == false)
            {
                testLogger.log("MyNetRecordingRequestHandler: notifySchedule() ERROR: failed to add NetRecordingEntry to Root ContentContainer:");
                return false;
            }

            //
            // Get the MetadataNode that contains the instructions for creating
            // the recording
            //
            MetadataNode metadataNode = netRecordingEntry.getRootMetadataNode();

            if (metadataNode == null)
            {
                testLogger.log("MyNetRecordingRequestHandler: notifySchedule() ERROR NetRecordingEntry MetadataNode is null.");
                return false;
            }

            testLogger.log("MyNetRecordingRequestHandler: notifySchedule() NetRecordingEntry MetadataNode contents:");
            logMetadataNodeContents(metadataNode, 0);

            //
            // Create the RecordingContentItem from the MetadataNode contents.
            //
            testLogger.log("MyNetRecordingRequestHandler: notifySchedule() create RecordingContentItem.");

            RecordingContentItem recordingContentItem = createRecordingContentItem(metadataNode);

            if (recordingContentItem == null)
            {
                testLogger.log("MyNetRecordingRequestHandler: notifySchedule() ERROR:  failed to create a RecordingContentItem.");
                return false;
            }

            //
            // Add the RecordingContentItem to the NetRecordingEntry before
            // adding it to the CDS.
            // The order of adds is important.
            // This will create an SRS RecordTask and associate it with the
            // RecordingContentItem.
            //
            testLogger.log("MyNetRecordingRequestHandler: notifySchedule() add RecordingContentItem to NetRecordingEntry");

            try
            {
                netRecordingEntry.addRecordingContentItem((RecordingContentItem) recordingContentItem);
            }
            catch (Exception e)
            {
                testLogger.log("MyNetRecordingRequestHandler: notifySchedule() ERROR: expeption when adding RecordingContentItem to NetRecordingEntry. "
                        + e.toString());
                return false;
            }

            //
            // Add the RecordingContentItem to the CDS
            //
            testLogger.log("MyNetRecordingRequestHandler: notifySchedule() add RecordingContentItem to ContentContainer");

            if (rootContentContainer.addContentEntry(recordingContentItem) == false)
            {
                testLogger.log("MyNetRecordingRequestHandler: notifySchedule() ERROR: failed to add NetRecordingEntry to Root ContentContainer:");
                return false;
            }

            return true;
        }

        public boolean notifyPrioritization(InetAddress arg0, RecordingContentItem[] arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }
    }

    /**
     * output MetadataNode contents to logging
     */
    private void logMetadataNodeContents(MetadataNode metadataNode, int printOffset)
    {
        String spaces = "";
        for (int j = 0; j < printOffset; spaces += " ", j++);

        Enumeration e = metadataNode.getMetadata();

        String[] keys = metadataNode.getKeys();

        // testLogger.log( spaces + "MetadataNode contains " + keys.length +
        // " keys");

        for (int i = 0; i < keys.length; i++)
        {
            Object obj = metadataNode.getMetadata(keys[i]);

            if (obj instanceof String)
            {
                testLogger.log(spaces + keys[i] + " = " + (String) obj);
            }
            else if (obj instanceof MetadataNode)
            {
                testLogger.log(spaces + keys[i] + " = MetadataNode");
                logMetadataNodeContents((MetadataNode) obj, printOffset + 4);
            }
            else
            {
                testLogger.log(spaces + keys[i] + " = Unknown data type");
            }
        }

    }

    /**
     * Class to manage asynchronous calls that require a NetActionHandler
     * 
     * @author Dan Woodard
     * 
     */
    private class MyNetActionHandler implements NetActionHandler
    {
        /**
         * NetActionHandler callback
         */
        public void notify(NetActionEvent arg0)
        {
            testLogger.log("MyNetActionHandler: got notify to MyNetActionHandler");

            this.netActionEvent = arg0;

            this.receivedNetActionEvent = true;

            try
            {
                synchronized (this.signal)
                {
                    this.signal.notifyAll();
                }
            }
            catch (Exception e)
            {
            }
        }

        /**
         * Waits for async response from a NetActionRequest method If
         * successful, the ActionEvent will be in this.netActionEvent
         * 
         * @return true if got the response, false if not
         */
        public boolean waitRequestResponse()
        {
            testLogger.log("MyNetActionHandler: wait for notify to MyNetActionHandler");

            if (!this.receivedNetActionEvent)
            {
                try
                {
                    synchronized (this.signal)
                    {
                        this.signal.wait(TIMEOUT);
                    }
                }
                catch (InterruptedException e)
                {
                }
            }

            if (!this.receivedNetActionEvent)
            {
                failReason = "MyNetActionHandler: Failed to get NetActionEvent within " + this.TIMEOUT
                        + " milliseconds";
                return false;
            }

            this.receivedNetActionEvent = false;// reset for next request

            testLogger.log("MyNetActionHandler: got NetActionEvent to MyNetActionHandler");

            return true;
        }

        /**
         * Gets the NetActionEvent received by this NetActionHandler
         * 
         * @return netActionEvent, null if not received, non-null if valid event
         */
        public NetActionEvent getNetActionEvent()
        {
            return this.netActionEvent;
        }

        public String getFailReason()
        {
            return this.failReason;
        }

        // responses from received NetActionEvent
        private NetActionEvent netActionEvent = null;

        // signals and flags for received NetActionHandler NetActionEvent
        private boolean receivedNetActionEvent = false;

        // use for signaling completion of async calls
        private Object signal = new Object();

        // timeout to wait for NetActionHandler NetActionEvents
        private static final long TIMEOUT = 5000;// 5 seconds

        private String failReason = "";
    }

    // ///////////////
    // privates
    // ///////////////

    private MyNetActionHandler netActionHandler = null;

    private MyNetRecordingRequestHandler netRecordingRequestHandler = null;

    private String failReason = "";

    private ContentServerNetModule contentServerNetModule = null;

    private NetRecordingRequestManager netRecordingRequestManager = null;

    private ContentContainer rootContentContainer = null;

    private NetActionRequest netActionRequest = null;

    private void buildMetadata(MetadataNode node)
    {
        node.addMetadata("srs:scheduledChannelID", "scheduledChannelIDParameter");
        node.addMetadata("srs:scheduledChannelID@type", "scheduledChannelID@typeParameter");
        node.addMetadata("srs:@id", "");// must be set to empty string for
                                        // recordSchedule
        node.addMetadata("srs:title", "titleParameter");
        node.addMetadata("srs:class", "object.recordSchedule.direct.manual");
        node.addMetadata("srs:scheduledStartDateTime", "T20:02:03");
        node.addMetadata("srs:scheduledDuration", "3");

        node.addMetadata("ocap:scheduledStartDateTime", "T19:00:00");
        node.addMetadata("ocapApp:gobledygoop", "valueSetIn_requestScheduleCaller");
    }

    private boolean checkMetadata(MetadataNode node)
    {
        // TODO:
        /*
         * if( !((String)node.getMetadata("title")).equals("HEY") ) {
         * testLogger.log("checkMetadata: expected HEY, got " +
         * (String)node.getMetadata("title")); return false; } if(
         * !((String)node
         * .getMetadata("ocap:scheduledStartDateTime")).equals("T19:00:00") ) {
         * testLogger.log("checkMetadata: expected T19:00:00, got " +
         * (String)node.getMetadata("ocap:scheduledStartDateTime")); return
         * false; } if(
         * !((String)node.getMetadata("ocapApp:lipo")).equals("LIPO") ) {
         * testLogger.log("checkMetadata: expected LIPO, got " +
         * (String)node.getMetadata("ocapApp:lipo")); return false; }
         */
        return true;
    }
}
