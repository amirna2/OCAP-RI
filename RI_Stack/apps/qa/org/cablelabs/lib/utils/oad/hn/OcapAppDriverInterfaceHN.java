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

package org.cablelabs.lib.utils.oad.hn;

import java.util.ArrayList;
import java.util.Vector;

////////////////////////////////////////////////////////////////////////
/// OcapAppDriverInterfaceHN
//
/// This interface defines the methods which provide the underlying driver-like 
/// functionality for RiScriptlet and RiExerciser (and potentially other applications) 
/// for basic HN Related functionality (without DVR extension support).
/// 
/// This interface defines methods which require HN functionality (but not DVR)
/// and can only be used by OCAP Xlets when Ocap Stack supports the OCAP HN extension.  
/// Methods which require support of OCAP HN extension (but not DVR) should be added to 
/// this interface.  Methods which require support of both OCAP HN extension and DVR
/// extension should not be added to this interface, but should be added to 
/// OcapAppDriverInterfaceHNDVR instead.  If a method does not require the HN OCAP extension, 
/// it should not be added to this interface, but OcapAppInterfaceCore or to any other 
/// applicable OCAP extension interface, i.e. OcapAppDriverInterfaceDVR, etc.
///
/// Methods defined in this interface should only have java primitive type parameters. 
/// There are no callbacks or events stemming from these methods.  Some APIs
/// have been added to "WaitFor" asynchrounous APIs to complete.  These types
/// of APIs should be added if there is desire to synchrounously wait for
/// given states, (see WaitForTuningState())...
//
public interface OcapAppDriverInterfaceHN
{
    ////////////////////////////////////////////////////////////////////////
    /// HN Playback
    //
    /**
     * Obtains the video URL for the item at index contentIndex in media server
     * serverIndex
     * @param serverIndex the index of the media server
     * @param contentIndex the index of the content item stored in the media
     * server
     * @return the URL of the video for JMF playback or NOT_FOUND if the content
     * item is not found
     */
    String getVideoURL(int serverIndex, int contentIndex); 

    /**
     * Create and start a HN player for the given content item (by index) of a given
     * server (by index) using the specified player type.  This method will not return
     * until playback is presenting or failure indication is received.
     * 
     * @param playerType indicates whether to create a JMF player or remote player
     * @param serverIndex the index of the server hosting the remote media
     * @param contentItemIndex the index of the content item in the server
     * @param waitTimeSecs amount of time in seconds to wait for playback to start
     * 
     * @return true if playback was successfully started and is now presenting, false otherwise.
     *         Returns false if playback is already active 
     */
    boolean playbackStart(int playerType, int serverIndex, int contentItemIndex, int waitTimeSecs);

    /**
     * A helper method which will play the next contentItem index - can be called only after createHNPlayback is initially called.
     * Will play index zero if the last contentitem index is currently playing.
     * 
     * Playback is asynchronous - uses ServiceContext.select instead of createHnPlayback to support fast tuning.
     */
    void playNext();

    /**
     * A helper method which will play the previous contentItem index - can be called only after createHNPlayback is initially called.
     * Will play the last contentitem index if index zero is currently playing.
     *
     * Playback is asynchronous - uses ServiceContext.select instead of createHnPlayback to support fast tuning.
     */
    void playPrevious();
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Telnet commands
    //
    /**
     * Returns the value of the supplied field name from current playback session
     * where the current session is the first active session found in list of mpeos
     * hn players.  Field supplied is matched, ignoring case.
     * 
     * @param httpFieldName return the value of this field in http HEAD response
     * 
     * @return  value of specified field in HEAD response, null if header not included
     */
    String getHttpHeadResponseField(String httpFieldName);

    /**
     * Returns the value of the supplied field name from current playback session
     * where the current session is the first active session found in list of mpeos
     * hn players.  Field supplied is matched, ignoring case.
     * 
     * @param httpFieldName return the value of this field in http GET response
     * 
     * @return  value of specified field in GET response, null if header not included
     */
    String getHttpGetResponseField(String httpFieldName);
    
    /**
     * Returns the starting time specified in the available seek range header from
     * last HEAD response.
     * 
     * @return  start time in milliseconds if included in HEAD response, -1 otherwise
     */
    int getHttpAvailableSeekStartTimeMS();

    /**
     * Returns the end time specified in the available seek range header from
     * last HEAD response.
     * 
     * @return  end time in milliseconds if included in HEAD response, -1 otherwise
     */
    int getHttpAvailableSeekEndTimeMS();

    /**
     * Sets the CCIbits for the current recording.
     * 
     * @param cascadedInput
     *            - Array of telnet commands to chain
     * @param expectedResult
     *            - Array of expected output for corresponding telnet commands.
     * @return true - setting CCI was successful else false as a result
     */
    boolean setCCIbits(String[] cascadedInput, String[] expectedResult);

    ////////////////////////////////////////////////////////////////////////
    /// Net Authorization Handler
    //
    /**
     * A method to register a NetAuthorizationHandler
     * @return true if the NetAuthorizationHandler is registered
     */
    boolean registerNAH();
    
    /**
     * A method to register a NetAuthorizationHandler2
     * @return true if the NetAuthorizationHandler2 was registered
     */
    boolean registerNAH2();
    
    /**
     * A method to unregister a NetAuthorizationHandler
     */
    void unregisterNAH();
    
    /**
     * A method to toggle the current NetAuthorizationHandler return policy
     */
    void toggleNAHReturnPolicy();
    
    /**
     * A method to get the current NetAuthorizationHandler return policy
     * @return the current return policy of the NetAuthorizationHandler
     */
    boolean getNAHReturnPolicy();
    
    /**
     * Toggles the NetAuthorizationHandler between logging only first message
     * of an activity or not
     */
    void toggleNAHFirstMessageOnlyPolicy();
    
    /**
     * A method to access the current NAH first message only policy
     * @return the current NAH first message only policy
     */
    boolean getNAHFirstMessageOnlyPolicy();
    
    /**
     * A method to have the NetAuthorizationHandler revoke the last activity
     */
    void revokeActivity();
    
    /**
     * A method to access the number of NetAuthorizationHandler messages
     * @return the number of NetAuthorizationHandlerMessages
     */
    int getNumNotifyMessages();
    
    /**
     * A method to access the NetAuthorizationHandler message at the given index.
     * @param i the index of the message to be retrieved
     * @return the message stored at the given index, if one exists. Null otherwise.
     */
    String getNotifyMessage(int i);
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Refresh functions
    //
    /**
     * Performs network related calls to refresh the list of devices.
     * This method should be called whenever the list of devices on the
     * network may have changed.  Calling this method may invalidate
     * prior indices or cause them to point to a different device.
     */
    void refreshDeviceList();

    /**
     * Performs network related calls to refresh the associated net module lists.
     * This method should be called whenever the list of net modules which
     * includes ContentServerNetModules may have changed.  Calling this method
     * may invalidate indices of previous net modules (including 
     * ContentServerNetModules/media server) or cause them to point to a different
     * module.  This method will also clear the content item lists associated with
     * content server net module/media servers.  
     */
    void refreshNetModules();
    
    /**
     * Looks up server in list of content servers and initiates a search.
     * The content items found from any previous search results will be removed.
     * The resulting content items from the search will be saved.  This method
     * should be called whenever the list of content items associated with a 
     * server may have changed or after net module list has been refreshed.
     * Calling this method may invalidate indices or cause them to point to
     * different content items.
     * 
     * @param serverIndex   perform search on server retrieved using supplied index
     * @param containerID the ID of the container to be searched or browsed
     * @param timeoutMS amount of time in milliseconds to find the root container of
     *        server at supplied index root container
     * @param browse indicates whether browse action should be used (search action
     * is used if false)
     * @param flattenSearch indicates whether a search for flatten or maintain the
     * container structure
     */
    void refreshServerContentItems(int serverIndex, String containerID, long timeoutMS, boolean browse, boolean flattenSearch);


    ////////////////////////////////////////////////////////////////////////
    /// HN Devices
    //
    /**
     * Returns the number of non-local devices on the network
     * @return the number of non-local devices on the network, or -1 for error
     */
    int getNumDevicesOnNetwork();

    /**
     * Return a string representation of the given device
     * @param deviceIndex the index of the device
     * @return a String representation of the device at the given index, or
     * NOT_FOUND if the device is not found
     */
    String getDeviceInfo(int deviceIndex);
    
    /**
     * A method to access the friendly name of the root device
     * @return the friendly name of the root device, or null for error
     */
    String getRootDeviceFriendlyName();
    
    /**
     * Returns the friendly name for the HN Media server.
     * @return - the name or null
     */
    String getHnLocalMediaServerFriendlyName();

    /** 
     * Update the Friendly Name of the root device to the given name
     * @param newName the new friendly name of the root device
     * @return true if the root device friendly name is successfully channged, 
     * false otherwise
    */
    boolean changeRootDeviceFriendlyName(String newName);
    
    /**
     * Changes and re-broadcasts the friendly name of the HN local media server via UPnP.
     * @param newName - the new friendly name
     * @return - true when successful
     */
    boolean hnChangeLocalMediaServerFriendlyName(String string);

    /**
     * Force the root device to send a UPnP bye-bye
     * @return true if the root device UPnP bye-bye messages are successfully
     * sent, false otherwise
     */
    boolean sendRootDeviceByeBye();

    /**
     * Force the root device to send a UPnP alive
     * @return true if the root device UPnP alive message was successfully sent,
     * false otherwise
     */
    boolean sendRootDeviceAlive();

    /**
     * Listen for events from all devices on the NetManager
     */
    void listenForAllDeviceEvents();
    
    /**
     * Listen for events from the indicated Device
     * @param deviceIndex the index of the Device to listen to
     */
    void listenForDeviceEvents(int deviceIndex);
    
    /**
     * Returns a String description of the last DeviceEvent received
     * @return a String representing the last DeviceEvent received, or null if
     * no event has been received
     */
    String getLastDeviceEvent();
        
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Content Servers
    //
    /**
     * Returns the number of ContentServerNetModules on the network
     * @return the number of ContentServerNetModules on the network
     */
    int getNumMediaServersOnNetwork();

    /**
     * Return a String representation of the media server at the given index
     * @param serverIndex the index of the media server.
     * @return a String representation of the media server at the given index, 
     * or NOT_FOUND if the media server is not located
     */
    String getMediaServerInfo(int serverIndex);
    
    /**
     * A method to access the friendly name of the media server at the given
     * index.
     * @param serverIndex the index of the server
     * @return the friendly name of the media server, or NOT_FOUND if the 
     * media server was not found
     */
    String getMediaServerFriendlyName(int serverIndex);
    
    /** 
     * Return the count of media servers on network with the given friendly name
     * 
     * @param mediaServerName the friendly name of the media server to search for
     * 
     * @return number of media servers with supplied name on network
     */
    int getMediaServerCountByName(String mediaServerName);

    /** 
     * Return the index of the media server with the given friendly name
     * @param mediaServerName the friendly name of the media server to search for
     * @return the index of the media server with the matching friendly name, or
     * -1 for error
     */
    int getMediaServerIndexByName(String mediaServerName);

    
    ////////////////////////////////////////////////////////////////////////
    /// HN Local Media Server
    //
    /**
     * Wait specified number of seconds for the local content server 
     * net module to be found.
     * 
     * @param timeoutSecs   max number of seconds to wait
     * @return  true if local content server net module is found
     *          within specified number of seconds, false otherwise
     */
    boolean waitForLocalContentServerNetModule(long timeoutSecs);
    
    /**
     * A method to access the UDN of the Local Media Server
     * @return the UDN of the local media server, or null if no local media
     * server is found
     */
    String getLocalMediaServerUDN();
    
    /** 
     * Update the UDN of the local media server to the given UDN
     * @param newUDN the new value of the UDN of the local media server
     * @return true if the UDN of the local media server is successfully changed,
     * false otherwise
    */
    boolean changeLocalMediaServerUDN(String newUDN);

    /**
     * Return the index of the local media server
     * @return the index of the local media server, or -1 for error
     */
    int findLocalMediaServer();

    /**
     * Returns the number of network interfaces
     * @return the number of network interfaces, or -1 for error
     */
    int getNumNetworkInterfaces();

    /**
     * Return a String representation of the given network interface
     * @param interfaceIndex the index of the network interface
     * @return a String representation of the given network interface, or NOT_FOUND
     * if the network interface is not found
     */
    String getNetworkInterfaceInfo(int interfaceIndex);

    /**
     * A method to get a String representing the content that has been published,
     * starting with the oldest item in the list of published content Strings.
     * @return a String representation of the oldest published content, or an 
     * empty String if the list of published content Strings is empty.
     */
    String getPublishedContentString();
    
    /**
     * Returns the number of content items that have been published.
     * @return the number of published content items
     */
    int getNumPublishedContentItems();
    
    ////////////////////////////////////////////////////////////////////////
    /// HN General Content Info
    //
    public static final int HN_CONTENT_TYPE_UNKNOWN = 0;
    public static final int HN_CONTENT_TYPE_RECORDING = 1;
    public static final int HN_CONTENT_TYPE_LIVE_STREAM = 2;
    public static final int HN_CONTENT_TYPE_VPOP = 3;
    public static final int HN_CONTENT_TYPE_URL = 4;

    public static final String HN_CONTENT_TYPE_STR_UNKNOWN = "Unknown";
    public static final String HN_CONTENT_TYPE_STR_RECORDING = "Recording";
    public static final String HN_CONTENT_TYPE_STR_LIVE_STREAM = "Live Stream";
    public static final String HN_CONTENT_TYPE_STR_VPOP = "VPOP";
    public static final String HN_CONTENT_TYPE_STR_URL = "URL";

    /**
     * Returns the number of content items on the given server
     * @param serverIndex the index of the server
     * @return the number of content items on the given server, or -1 for error
     */
    int getNumServerContentItems(int serverIndex);

    /**
     * Return a String representation of the given content on the given server
     * @param serverIndex the index of the server
     * @param contentItemIndex the index of the ContentItem on the server
     * @return a String representation of the content item at the given index
     * on the server of the given index, or NOT_FOUND if the item is not found
     */
    String getServerContentItemInfo(int serverIndex,
                                             int contentItemIndex);

    /**
     * A method to get a String representation of the content item 
     * @param serverIndex the index of the MediaServer index
     * @param contentItemName the name of the contentItem
     * @return a String representation of the content item 
     */
    String getServerContentItemInfo(int serverIndex, String contentItemName);
    
    /**
     * Determines if the content item at the supplied index on the specified
     * server is a recording content item.
     * 
     * @param   serverIndex index of server hosting content item
     * @param   index       index of content item in server's list of content items
     * 
     * @return  true if content item is a recording content item, false otherwise
     */
    boolean isRecordingContentItem(int serverIndex, int index);

    /**
     * Determines if the content item at the supplied index on the specified
     * server is a channel content item.
     * 
     * @param   serverIndex index of server hosting content item
     * @param   index       index of content item in server's list of content items
     * 
     * @return  true if content item is a channel content item, false otherwise
     */
    boolean isChannelContentItem(int serverIndex, int index);
    
    /**
     * Determines if the content entry at the supplied index on the specified
     * server is a content container.
     * 
     * @param   serverIndex index of server hosting content item
     * @param   index       index of content item in server's list of content entries
     * 
     * @return  true if content item is a content container, false otherwise
     */
    boolean isContentContainer(int serverIndex, int index);
    
    /**
     * Determines if the content item at the supplied index on the specified
     * server is a VPOP content item.
     * 
     * @param   serverIndex index of server hosting content item
     * @param   index       index of content item in server's list of content items
     * 
     * @return  true if content item is a VPOP content item, false otherwise
     */
    boolean isVPOPContentItem(int serverIndex, int index);
    
    /**
     * Returns the ID of the Content Entry at the given index in the media server
     * at the 
     * @param serverIndex the index of the media server
     * @param index the index of the content entry
     * @return the id of the content entry at the given index, or null if no 
     * entry is found
     */
    String getEntryID(int serverIndex, int index);
    
    /**
     * Returns the name of the Content Container at the index for the server
     * at serverIndex
     * @param serverIndex the index of the server where the Content Container is stored
     * @param index the index of the Content Container in the server
     * @return the name of the Content Container at the given index, or null if
     * the item at the index is not a ContentContainer
     */
    String getContainerName(int serverIndex, int index);

    /**
     * Returns a string representation of the content item's type.
     * 
     * @param serverIndex   server of this content item
     * @param index         index of the content item
     * @return  string describing type of content, null if problems encountered
     */
    String getContentItemTypeStr(int serverIndex, int index);
 
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Channel Content
    //
 
    /**
     * Returns the number of channel content items on the indicated media server
     * @param serverIndex the index of the media server
     * @return the number of channel content items, or -1 for error
     */
    int getNumChannelContentItems(int serverIndex);

    /**
     * Returns the ChannelItemURL of the ChannelContentItem at the given index
     * @param index the index of the ChannelContentItem
     * @return a String representation of the ChannelContentItemURL at the 
     * given index
     */
    String getChannelContentItemURL(int serverIndex, int channelContentItemIndex);
    
    /**
     * This method will return the index of ChannelContentItem by providing the
     * serverindex and channelIndex.
     * @param serverIndex the index of the media server to search
     * @param channelName the name of the ChannelContentItem to search for
     * @return the index of the channel witht the given name stored in the media
     * server indicated by the given index, or -1 for error
     */
    // *TODO* - rename to getContentItemIndexUsingChannelName()
    int getChannelItemIndexByName(int serverIndex, String channelName);

    /**
     * Returns the index of the channel content item stored in the server at the
     * given index
     * @param serverIndex the index of the server where the channel is stored
     * @param channelIndex the index of the channel content item in the server
     * @return the index of the channel to tune to, or -1 if error
     */
    int getChannelItemIndex(int serverIndex, int channelIndex);
    
    /** 
     * Returns the index of the ChannelContentItem in the CDS that relates to
     * the Service at the given index
     * @param channelIndex the index of the local Service
     * @return the index in the CDS of the ChannelContentItem in the CDS matching
     * the Service at the given index, or -1 for error
     */
    int getIndexForLocalChannel(int channelIndex);

    /**
     * Publish the given Service (by index) to the root container of the CDS.
     * @param serviceIndex the index of the Service to be published
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * local server
     * @return true if the Service is successfully published, false otherwise 
     */
    boolean publishService(int serviceIndex, long timeoutMS);
    
    /**
     * Publish all Services to the CDS.
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * local server
     * @return true if all Services were successfully published, false otherwise
     */
    boolean publishAllServices(long timeoutMS);
    
    /**
     * Publish the given Service (by index) to the Channel container of the CDS.
     * @param serviceIndex the index of the Service to be published
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * local server
     * @param publishAsVOD set to true if channel is published as VOD type
     * @return true if the Service is successfully published, false otherwise 
     */
    boolean publishServiceToChannelContainer(int serviceIndex, long timeoutMS, boolean publishAsVOD);
    
    /**
     * Publish all Services to the Channel Container in the CDS.
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * local server
     * @return true if all Services were successfully published, false otherwise 
     */
    boolean publishAllServicesToChannelContainer(long timeoutMS);
    
    /**
     * Publishes all Services to the CDS, in addition to Services to be used
     * for ServiceResolutionHandling.
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * local server
     * @return true if all Services were successfully published, false otherwise
     */
    boolean publishAllServicesWithSRH(long timeoutMS);
    
    /**
     * Publish the given Service (by index) to the CDS with an alternate URI.
     * @param serviceIndex the index of the Service to be published
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * local server
     * @return true if the Service was successfully published, false otherwise
     */
    boolean publishServiceUsingAltRes(int serviceIndex, long timeoutMS);

    /**
     * Publish all services found to the CDS with an alternate URI.
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * local server
     * @return true if all Services were successfully published, false otherwise
     */
    boolean publishAllServicesUsingAltRes(long timeoutMS);

    /**
     * A method to unpublish all channels from the local media server
     * @param timeoutSecs the amount of time in seconds to wait for the local
     * media server to be discovered
     * @return true if the channels were successfully unpublished, false otherwise
     */
    boolean unPublishChannels(long timeoutSecs);

    boolean updateTuningLocatorWithSRH();

    int getUpnpRuiServerIndexByName(String string);

    String invokeRuissGetCompatibleUIs(int init, String deviceInputProfile,
            String uIFilter);
    
    ////////////////////////////////////////////////////////////////////////
    /// HN Hidden Content
    //
    /**
     * Returns the index of the hidden container 
     * @param containerName the name of the container to located
     * @return the index of the hidden container, or -1 for error
     */
    int getHiddenContainerIndex(String containerName);
    
    /**
     * Returns the total number of containers created as a part of hidden content
     * test
     * @return the number of hidden containers, or -1 for error
     */
    int getNumHiddenContainer();
    
    /**
     * This method will create a content container either visible or invisible 
     * to search based on the parameters passed to create an efab.
     * returns true if success or false.
     * @param readWorld read access for all applications
     * @param writeWorld write access for all applications
     * @param readOrg read access for applications with the same organisation
     * as the granting application.
     * @param writeOrg  write access for applications with the same organisation
     * as the granting application.
     * @param readApp read access for the owner
     * @param writeApp write access for the owner
     * @param otherOrgRead array of other organisation identifiers with read 
     * access. Applications with an organisation identifier matching one of 
     * these organisation identifiers will be given read access.
     * @param otherOrgWrite array of other organisation identifiers with write 
     * access. Applications with an organisation identifier matching one of 
     * these organisation identifiers will be given write access.
     * @param containerName the name of the created content container
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * server at supplied index root container
     * @return true if the content container was successfully created, false otherwise
     */
    boolean createContentContainer(boolean readWorld, boolean writeWorld, 
            boolean readOrg, boolean writeOrg, boolean readApp, boolean writeApp,
            int[] otherOrgRead, int[] otherOrgWrite, String containerName, long timeoutMS);

    /**
     * 
     * This method will create content items either visible or invisible to search
     * based on the efab object.
     * @param noOfItemstoCreate the number of content items to create
     * @param containerIndex the index of the content container to contain the
     * created items
     * @param contentItemName the base name of the created content items
     * @param readWorld read access for all applications
     * @param writeWorld write access for all applications
     * @param readOrg read access for applications with the same organisation
     * as the granting application.
     * @param writeOrg write access for applications with the same organisation
     * as the granting application.
     * @param readApp read access for the owner
     * @param writeApp write access for the owner
     * @param otherOrgRead array of other organisation identifiers with read 
     * access. Applications with an organisation identifier matching one of 
     * these organisation identifiers will be given read access.
     * @param otherOrgWrite array of other organisation identifiers with write 
     * access. Applications with an organisation identifier matching one of 
     * these organisation identifiers will be given write access.
     * @param timeoutMS amount of time in milliseconds to find the root container of
     * server at supplied index root container
     * @return true if the content items were successfully created, false otherwise
     */
    boolean createItemsForContainer(int noOfItemstoCreate,int containerIndex ,String contentItemName, 
            boolean readWorld, boolean writeWorld, boolean readOrg, boolean writeOrg, boolean readApp, 
            boolean writeApp, int[] otherOrgRead, int[] otherOrgWrite, long timeoutMS);


    
    ////////////////////////////////////////////////////////////////////////
    /// Testing UPnP Service and actions
    //
    
    /**
     * Updates the internal list of UPnPClientDevices that advertise ContentDirectoryService.
     * and returns the number of devices.
     *  
     * @return the total number of media servers on the network. 
     */
    int getNumUPnPMediaServersOnNetwork();

    /**
     * Gets the index number of remote media server with the provided friendly name from the 
     * internal list of UPnPClientDevices.
     * 
     * @param name - the friendly name of the device.
     * 
     * @return serverIndex - the position of the device in the internal list of UPnPClientDevices.  Note that 
     * if there are two UPnPClientDevices on the network with the same friendly name, the index of the first
     * instance in the list will be returned. 
     * 
     */
    int getUpnpMediaServerIndexByName(String name);
    
    /**
     * Gets the list of ConnectionIDs of currently ongoing Connections established with indexed media server.
     * 
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * @return a <code>String[]</code> object containing the the connectionIDs derived from the action response.
     *         Or a <code>String[]</code> with no elements if there are no connectionIds.
     */
    String[] invokeCmGetConnectionIds(int serverIndex);

    /**
     * Returns the number of media servers that support live streaming
     * @return the number of media servers that support live streaming
     */
    int getUpnpNumLiveStreamingMediaServers();
    
    /**
     * Returns a String description of the media server at the indicated index
     * that supports live streaming
     * @param serverIndex the index of the media server supporting live streaming
     * @return a String representation of the indicated media server that supports
     * live streaming, or NOT_FOUND if the live streaming media server is not found
     */
    String getUpnpLiveStreamingMediaServerInfo(int serverIndex);
    

    
    ////////////////////////////////////////////////////////////////////////
    /// VPOP Content
    //
    
    /**
     * Gets the URI of a published VPOP
     * 
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * 
     * @return The URI of the VPOP published by the media server.  Empty string is returned if 
     * the index value is invalid or the media server is not publishing a VPOP.
     */
    String getVpopUri(int serverIndex);
    
    /**
     * Returns the index of the VPOP content item stored in the server at the
     * given index.
     * @param m_serverIndex the index of the Server to search for VPOP content
     * @return the index of the VPOP content item, or -1 if the server does not
     * contain a VPOP content item
     */
    int getVpopContentItemIndex(int m_serverIndex);
    
    /**
     * Invokes the VPOP service's PowerStatus action.
     *  
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * @return String value containing either the the OUT arg value from the action response or the HTTP error
     *  response if the invocation was not successful.
     */
    String invokeVpopPowerStatus(int serverIndex);
    
    /**
     * Invokes the VPOP service's Mute action.
     * 
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * @param connectionID - ConnectionID of the in-progress streaming of VPOP content item on media server.
     * @return String value containing the HTTP response for the action invocation.
     */
    String invokeVpopAudioMute(int serverIndex, String connectionID);
    
    /**
     * Invokes the VPOP service's AudioRestore action.
     * 
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * @param connectionID - ConnectionID of the in-progress streaming of VPOP content item on media server.
     * @return String value containing the HTTP response for the action invocation.
     */
    String invokeVpopAudioRestore(int serverIndex, String connectionID);
    
    /**
     * Invokes the VPOP service's PowerOn action.
     * 
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * @return String value containing the HTTP response for the action invocation.
     */
    String invokeVpopPowerOn(int serverIndex);
    
    /**
     * Invokes the VPOP service's PowerOff action.
     * 
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * @param connectionID - ConnectionID of the in-progress streaming of VPOP content item on media server.
     * @return String value containing the HTTP response for the action invocation.
     */
    String invokeVpopPowerOff(int serverIndex, String connectionID);
    
    /**
     * 
     * @param serverIndex - index of the UPnPClientDevice Media Server in the internal list of UPnPClientDevices.
     * @param connectionID - ConnectionID of the in-progress streaming of VPOP content item on media server.
     * @param tuneParameters - the channel values used as an IN argument to the Tune() action
     * @return String value containing the HTTP response for the action invocation.
     */
    String invokeVpopTune(int serverIndex, String connectionID, String tuneParameters);
    
    ////////////////////////////////////////////////////////////////////////
    /// RUI5 (Remote User Interface HTML5)
    //
    /**
     * Publishes a UI in the RUI Device.  
     * 
     * @param XMLDescription - XML describing the device or null
     * @return String - null if succcessfully published (or cleared) otherwise exception string
     */
    String setUIList (String XMLDescription);
    
    /**
     * Returns the XML 
     *
     * @return String - the XML 
     */
    String getXML(String string);
    
    /**
     * Assigns the Http request resolution handler.
     *
     * @param handler   http request resolution handler.
     */
    void setHttpRequestResolutionHandler();

    /**
     * Sets a new path component in the URL returned by http request resolution handler.
     *
     * @param newPath new path component
     */
    void setReturnURLPath(String newPath);

    /**
     * Returns whether http request resolution handler was called.
     *
     */
    boolean wasHttpRequestResolutionHandlerCalled();

    /**
     * Sets the UPnPStateVariableListener
     *
     * @param device    String representing friendly name of root device
     * @param subDevice String representing model name of embedded device
     * @param service   String representing service type of service
     *
     * @returns true if listener set.. false otherwise
     */

    boolean setUPnPStateVariableListener(String device, String subDevice, String service);

    /**
     * Gets the last StateVariable event received
     *
     * @returns name of last event received
     */

    String getStateVariableEvent();
    
    /**
     * Gets the list of IP addresses the Media Server is listening on
     *
     * @returns list of IP addresses
     */   
    ArrayList getIPAddressesMediaServer();
    
    /**
     * Gets the list of IP addresses the Control Point is listening on
     *
     * @returns list of IP addresses
     */   
    ArrayList getIPAddressesControlPoint();
    
    /**
     * Returns a String representation of the TransformationEvent description
     * from the list of received Transformation events
     * @return a String representation of the TransformationEvent at index 0 in
     * the list of Transformation Event Strings, or null if the list is empty
     */
    String getNextTransformationEventString();
    
    /**
     * Returns the number of TransformationEvents currently in the list of
     * TransformationEvents
     * @return the number of TransformationEvents currently in the list of
     * TransformationEvents
     */
    int getNumTransformationEvents();
    
    /**
     * Either activates listening for TransformationEvents or deactivates 
     * listening for TransformationEvents
     * @param activateListener indicates whether TransformationEvent listening
     * should be activated (true), or deactivated (false)
     */
    void listenForTransformationEvents(boolean activateListener);
    
    /**
     * Returns an array of Strings representing the Content Transformations supported
     * @return a array of Strings representing the Content Transformations supported
     */
    String[] getSupportedTransformations();
    
    /**
     * Sets the default Transformations for the TransformationManager
     */
    void setDefaultTransformations();
    
    /**
     * Sets Transformations for existing ContentItems.
     */
    void setTransformations();
    
    /**
     * Returns the list of Transformations that are currently default   
     * @return array of Strings representing the default Content Transformations
     */
    String[] getDefaultTransformations();
    
    /**
     * 
     * @param contentItemIndex the index of the ContentItem 
     * @return an array of Strings representing the Transformations applied to
     * the ContentItem at index contentItemIndex
     */
    String[] getTransformations(int contentItemIndex);
    
    /**
     * Applies the Transformation at the given index to the ContentItem at the
     * given index
     * @param contentItemIndex the index of the ContenItem to apply the Transformation
     * @param transformationIndex the index of the Transformation to be applied.
     * If -1 is supplied for this index, then all Transformations will be removed
     * from the ContentItem.
     */
    void setTransformation(int contentItemIndex, int transformationIndex);
    
    /**
     * Removes the Transformation at the given index from the ContentItem at the
     * given index in the local CDS
     * @param contentItemIndex the index of the ContentItem
     * @param transformationIndex the index of the Transformation to be removed
     */
    void removeTransformation(int contentItemIndex, int transformationIndex);
    
    /**
     * Returns a String representation of a ContentResource at the given index for
     * a ContentItem at the given index in the local CDS
     * @param contentItemIndex the index of the ContentItem
     * @param resourceIndex the index of the ContentResource of the ContentItem
     * @return a String description of the ContentResource from 
     * ContentResource.getContentFormat() or null if invalid indices are supplied
     * for the ContentItem or ContentResource
     */
    String getContentResourceInfo(int contentItemIndex, int resourceIndex);
    
    /**
     * Returns an array of String representations of the ContentResources for
     * the ContentItem at the given index
     * @param contentItemIndex the index of the ContentItem
     * @return an array of String representations of the ContentResources for
     * the ContentItem at the given index, or null if the ContentItem or the
     * ContentResources are null
     */
    String[] getContentResourceStrings(int contentItemIndex);
    
    /**
     * Deletes the ContentResource at the given index for the ContentItem at the given
     * index in the local CDS.
     * @param contentItemIndex the index of the ContentItem in the local CDS
     * @param resourceIndex the index of the ContentResource to be deleted
     * @return true if the ContentResource is deleted, false if an error occurs
     * or the ContentResource was not successfully deleted
     */
    boolean deleteContentResource(int contentItemIndex, int resourceIndex);
    
    /**
     * Removes a Transformation from the list of default Transformations
     * @param transformationIndex the index of the Transformation to be removed
     * from the list of default Transformations
     */
    void removeDefaultTransformation(int transformationIndex);
    
    /**
     * Adds a Transformation from the list of supported Transformations to the
     * list of default Transformations
     * @param transformationIndex the index of the Transformation in the list of
     * supported Transformations to be added to the list of default Transformations
     */
    void addDefaultTransformation(int transformationIndex);
}   

