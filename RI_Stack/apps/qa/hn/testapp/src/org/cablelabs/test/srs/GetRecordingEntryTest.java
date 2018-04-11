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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.util.Enumeration;

import javax.tv.locator.Locator;

import org.cablelabs.test.Test;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
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

/**
 * GetRecordingEntryTest - A <code>CreateRecordScheduleRequest</code> action is
 * generated through a local NetRecordingRequestManager.requestSchedule()method
 * call. This is an all local test such that all the requests and actions are
 * handled on the same machine as this test is run. It is meant to successfully
 * schedule a recording and add the NetRecordingEntry and RecordingContentItem
 * to the CDS. Then successfully query the previous scheduled recording for
 * <code>NetRecordingEntry</code> that contains the
 * <code>RecordingContentItem</code>.
 * 
 * This test obtains a reference to the <code>NetRecordingRequestManager</code>
 * in order to make a request to schedule a recording. The tests makes a call to
 * {@link org.ocap.hn.recording.NetRecordingRequestManager#requestSchedule(NetRecordingSpec, NetActionHandler)}
 * and then verifies that this action was successful. Then it takes the
 * <code>NetRecordingEntry</code> that was sent in the action response and
 * obtains the <code>recordingContentItem</code>. Then the test makes a call to
 * {@link org.ocap.hn.recording.RecordingContentItem#getRecordingEntry()}. The
 * test verifies that it was successful in obtaining a reference to the same
 * <code>NetRecordingEntry</code> as before, otherwise the test fails. The test
 * will also fail if it can not get a handle to the
 * <code>NetRecordingRequestManager</code>.
 * 
 * @author Dan Woodard (Flashlight Engineering and Consulting)
 * @author Ben Foran (Flashlight Engineering and Consulting)
 */
public class GetRecordingEntryTest extends Test
{
    private NetRecordingRequestManager netRecordingRequestManager = null;

    private MyNetActionHandler netActionHandler = null;

    private String failMessage = "";

    private MyNetRecordingRequestHandler netRecordingRequestHandler = null;

    private ContentServerNetModule contentServerNetModule = null;

    private ContentContainer rootContentContainer = null;

    private NetActionRequest netActionRequest = null;

    private void buildMetadata(MetadataNode node)
    {
        node.addMetadata("title", "HEY");
        node.addMetadata("item@id", "69");
        node.addMetadata("ocap:scheduledStartDateTime", "T19:00:00");
        node.addMetadata("ocapApp:lipo", "LIPO");
    }

    public int clean()
    {
        netActionHandler = null;

        netRecordingRequestHandler = null;

        failMessage = "";

        contentServerNetModule = null;

        netRecordingRequestManager = null;

        netActionRequest = null;

        rootContentContainer = null;

        return Test.TEST_STATUS_PASS;
    }

    public int execute()
    {

        this.testLogger.setPrefix("GetRecordingEntryTest: ");
        this.testLogger.log("Executing Test: " + this.getName());

        if (executeTest())
        {
            this.testLogger.log("test PASSED!");

            return Test.TEST_STATUS_PASS;
        }
        else
        {
            this.testLogger.log("test FAILED! - reason: " + failMessage);

            return Test.TEST_STATUS_FAIL;
        }
    }

    public int prepare()
    {
        netActionHandler = new MyNetActionHandler();

        netRecordingRequestHandler = new MyNetRecordingRequestHandler();

        failMessage = "";

        contentServerNetModule = null;

        netRecordingRequestManager = null;

        netActionRequest = null;

        rootContentContainer = null;

        return Test.TEST_STATUS_PASS;
    }

    public String getName()
    {
        return "srs.GetRecordingEntryTest";
    }

    public String getDescription()
    {
        return "This is the SRS GetRecordingEntryTest";
    }

    private boolean executeTest()
    {
        /**
         * Obtain a handle to the local NetRecordingRequestManager (Schedule
         * Recording Service) and local ContentServerNetModule (Content
         * Directory Service).
         */
        if (!getLocalServices())
        {
            this.failExit("Could not find local NetRecordingRequestManager or ContentServerNetModule. Reason: "
                    + this.failMessage);
            return false;
        }

        this.netActionRequest = this.contentServerNetModule.requestRootContainer(this.netActionHandler);

        if (!this.netActionHandler.waitRequestResponse())
        {
            this.failExit("ContentServerNetModule.requestRootConainer response failed. Reason: "
                    + this.netActionHandler.getFailReason());
            return false;
        }

        Object objectFromEvent = this.getResponseFromEvent(this.netActionHandler.getNetActionEvent());

        if (objectFromEvent == null)
        {
            this.failExit("Could not get ContentServerNetModule.requestRootConainer response from event. Reason: "
                    + this.failMessage);
            return false;
        }

        if ((objectFromEvent instanceof ContentContainer) == false)
        {
            this.failExit("NetActionEvent did not contain a ContentContainer instance");
            return false;
        }

        this.rootContentContainer = (ContentContainer) objectFromEvent;

        /**
         * Create requestSchedule() request
         */
        NetRecordingSpec spec = new NetRecordingSpec();
        MetadataNode node = spec.getMetadata();

        this.buildMetadata(node);

        this.netRecordingRequestManager.setNetRecordingRequestHandler(this.netRecordingRequestHandler);

        this.testLogger.log("Calling NetRecordingRequestManager.requestSchedule()");

        this.netActionRequest = this.netRecordingRequestManager.requestSchedule(spec, this.netActionHandler);

        if (!this.netActionHandler.waitRequestResponse())
        {
            this.failExit("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                    + this.netActionHandler.getFailReason());
            return false;
        }

        objectFromEvent = this.getResponseFromEvent(this.netActionHandler.getNetActionEvent());

        if (objectFromEvent == null)
        {
            this.failExit("Failed to get response from event for requestSchedule. Reason: " + this.failMessage);
            return false;
        }

        if ((objectFromEvent instanceof NetRecordingEntry) == false)
        {
            this.failExit("NetActionEvent does not contain an instance of NetRecordingEntry");
            return false;
        }

        this.testLogger.log("requestSchedule action status: " + netActionRequest.getActionStatus());

        NetRecordingEntry netRecordingEntry = (NetRecordingEntry) objectFromEvent;

        MetadataNode metadataNode = netRecordingEntry.getRootMetadataNode();

        if (metadataNode == null)
        {
            this.failExit("NetRecordingEntry contains a null MetadataNode.");
            return false;
        }

        this.testLogger.log("NetActionEvent NetRecordingEntry MetadataNode contents:");
        logMetadataNodeContents(metadataNode, 0);

        RecordingContentItem[] recordingContentItems = null;

        try
        {
            recordingContentItems = netRecordingEntry.getRecordingContentItems();
        }
        catch (IOException e)
        {
            this.testLogger.log("got IOException");
            return false;
        }

        this.testLogger.log("Total number of RecordingContentItems for this NetRecordingEntry: "
                + recordingContentItems.length);

        for (int i = 0; i < recordingContentItems.length; i++)
        {
            if (netRecordingEntry.equals(recordingContentItems[i].getRecordingEntry()))
            {
                this.testLogger.log("RecordingContentItem.getRecordingEntry returned the same NetRecordingEntry");
                return true;
            }
        }

        this.failExit("RecordingContentItem.getRecordingEntry did NOT return the same NetRecordingEntry");
        return false;
    }

    /**
     * getLocalServices - this method gets the
     * <code>NetRecordingRequestManager</code> and local
     * <code>ContentServerNetModule</code>.
     * 
     * @return a boolean value indicating if the
     *         <code>NetRecordingRequestManager</code> and local
     *         <code>ContentServerNetModule</code> were successfully discovered.
     *         If the method returns TRUE then the
     *         <code>NetRecordingRequestManager</code> and local
     *         <code>ContentServerNetModule</code> were found, otherwise it
     *         failed.
     */
    private boolean getLocalServices()
    {
        NetList netList = NetManager.getInstance().getNetModuleList(null);

        /**
         * Make a second attempt to obtain a list of all the NetModules in the
         * event that none were discovered during the first attempt.
         */
        if (netList.size() <= 0)
        {
            try
            {
                Thread.sleep(30000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            netList = NetManager.getInstance().getNetModuleList(null);
        }

        if (netList.size() == 0)
        {
            this.failMessage = "NetManager returned 0 size NetModuleList";
            return false;
        }

        this.testLogger.log("NetManager returned NetModuleList size = " + String.valueOf(netList.size()));

        Enumeration enumerator = netList.getElements();

        while (enumerator.hasMoreElements())
        {
            Object netListObject = enumerator.nextElement();

            if (netListObject instanceof ContentServerNetModule)
            {
                if (((ContentServerNetModule) netListObject).isLocal())
                {
                    contentServerNetModule = (ContentServerNetModule) netListObject;
                }

                this.testLogger.log("found a ContentServerNetModule, isLocal = "
                        + String.valueOf(((ContentServerNetModule) netListObject).isLocal()) + " Device - "
                        + ((ContentServerNetModule) netListObject).getDevice().getName());
            }
            else if (netListObject instanceof NetRecordingRequestManager)
            {
                netRecordingRequestManager = (NetRecordingRequestManager) netListObject;

                if (netRecordingRequestManager.isLocal() == false)
                {
                    netRecordingRequestManager = null;
                    this.failMessage = "found a non-local netRecordingRequestManager";
                    return false;
                }

                this.testLogger.log("found a NetRecordingRequestManager: isLocal - "
                        + String.valueOf(((NetRecordingRequestManager) netListObject).isLocal()) + " Device - "
                        + ((NetRecordingRequestManager) netListObject).getDevice().getName());
            }
            else if (netListObject instanceof RecordingNetModule)
            {
                this.testLogger.log("found a RecordingNetModule: isLocal - "
                        + String.valueOf(((RecordingNetModule) netListObject).isLocal()) + " Device - "
                        + ((RecordingNetModule) netListObject).getDevice().getName());
            }
            else if (netListObject instanceof NetModule)
            {
                this.testLogger.log("found a NetModule: isLocal - "
                        + String.valueOf(((NetModule) netListObject).isLocal()) + " Device - "
                        + ((NetModule) netListObject).getDevice().getName());
            }
            else
            {
                this.failMessage = "found unknown type, isLocal = "
                        + String.valueOf(((NetModule) netListObject).isLocal());
                return false;
            }
        }

        if (contentServerNetModule == null)
        {
            this.failMessage = "could not find ContentServerNetModule";

            return false;
        }
        else if (netRecordingRequestManager == null)
        {
            this.failMessage = "could not find NetRecordingRequestManager";

            return false;
        }

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

        // Check that request is same as sent, comparing references only
        if (receivedNetActionRequest != this.netActionRequest)
        {
            this.failMessage = "Recieved NetActionRequest is not the same as the request return value.";
            return null;
        }

        // Check action status
        if (receivedActionStatus != NetActionEvent.ACTION_COMPLETED)
        {
            this.failMessage = "The NetActionRequest returned ActionStatus = ";

            if (receivedActionStatus == NetActionEvent.ACTION_CANCELED)
            {
                this.failMessage += "ACTION_CANCELED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_FAILED)
            {
                this.failMessage += "ACTION_FAILED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_IN_PROGRESS)
            {
                this.failMessage += "ACTION_IN_PROGRESS ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_STATUS_NOT_AVAILABLE)
            {
                this.failMessage += "ACTION_STATUS_NOT_AVAILABLE ";
            }
            else
            {
                this.failMessage += "UNKONWN ACTION STATUS value=" + receivedActionStatus;
            }

            this.failMessage += ", Error value " + String.valueOf(receivedError);

            return null;
        }

        return receivedResponse;
    }

    /**
     * MyNetRecordingRequestHandler - inner class
     * 
     * @author Dan Woodard (Flashlight Engineering and Consulting)
     */
    private class MyNetRecordingRequestHandler implements NetRecordingRequestHandler
    {
        OcapRecordingManager recordingManager = null;

        LocatorRecordingSpec spec = null;

        RecordingRequest request = null;

        RecordingContentItem recordingContentItem = null;

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

        public boolean notifyDisable(InetAddress inetAddress, ContentEntry contentEntry)
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
            recordingManager = (OcapRecordingManager) org.ocap.shared.dvr.RecordingManager.getInstance();

            // TODO: use values from input MetadataNode, for now just hard code
            // in the values
            Locator[] la = new Locator[1];
            try
            {
                la[0] = new org.ocap.net.OcapLocator(2001);// sourceID = 2001
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
                            true, true, true, new int[0], new int[0]), "organization", null);

            try
            {
                spec = new LocatorRecordingSpec(la, new Date(), 100000L, properties);
            }
            catch (Exception e)
            {
                testLogger.log("MyNetRecordingRequestHandler: createRecordingContentItem() Exception when creating LocatorRecordingSpec: "
                        + e.toString());
                return null;
            }

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

            recordingContentItem = createRecordingContentItem(metadataNode);

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

            if (rootContentContainer.addContentEntry(netRecordingEntry) == false)
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
     * MyNetActionHandler - inner class to manage asynchronous calls that
     * require a <code>NetActionHandler</code>.
     * 
     * @author Dan Woodard (Flashlight Engineering and Consulting)
     */
    private class MyNetActionHandler implements NetActionHandler
    {
        // responses from received NetActionEvent
        private NetActionEvent netActionEvent = null;

        // signals and flags for received NetActionHandler NetActionEvent
        private boolean receivedNetActionEvent = false;

        // use for signaling completion of async calls
        private Object signal = new Object();

        // timeout to wait for NetActionHandler NetActionEvents
        private static final long TIMEOUT = 10000;// 10 seconds

        private String failReason = "";

        /**
         * notify - <code>NetActionHandler</code> callback.
         */
        public void notify(NetActionEvent netActionEvent)
        {
            testLogger.log("MyNetActionHandler: got notify to MyNetActionHandler");

            this.netActionEvent = netActionEvent;

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
                e.printStackTrace();
            }
        }

        /**
         * waitRequestResponse - waits for async response from a
         * NetActionRequest method. If successful, the ActionEvent will be in
         * this.netActionEvent.
         * 
         * @return true if the response was received, otherwise return false.
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
                    e.printStackTrace();
                }
            }

            if (!this.receivedNetActionEvent)
            {
                failReason = "MyNetActionHandler: Failed to get NetActionEvent within " + MyNetActionHandler.TIMEOUT
                        + " milliseconds";
                return false;
            }

            // reset for next request
            this.receivedNetActionEvent = false;

            testLogger.log("MyNetActionHandler: got NetActionEvent to MyNetActionHandler");

            return true;
        }

        /**
         * getNetActionEvent - gets the <code>NetActionEvent</code> received by
         * this <code>NetActionHandler</code>.
         * 
         * @return a <code>NetActionEvent</code>, this object will be null if it
         *         was not received, otherwise non-null if it is a valid event.
         */
        public NetActionEvent getNetActionEvent()
        {
            return this.netActionEvent;
        }

        /**
         * getFailReason - returns the reason for the failure.
         * 
         * @return the reason for the failure.
         */
        public String getFailReason()
        {
            return this.failReason;
        }
    }

    /**
     * output MetadataNode contents to logging
     */
    private void logMetadataNodeContents(MetadataNode metadataNode, int printOffset)
    {
        String spaces = "";
        for (int j = 0; j < printOffset; spaces += " ", j++)
        {
            // No op
        }

        String[] keys = metadataNode.getKeys();

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

}
