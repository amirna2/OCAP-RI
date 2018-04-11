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
package org.cablelabs.impl.ocap.hn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.ContentFactory;
import org.cablelabs.impl.ocap.hn.content.navigation.ContentListImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.MiniDomParser;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.NamedNodeMap;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.Node;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.NodeList;
import org.cablelabs.impl.spi.ProviderRegistryExt;
import org.cablelabs.impl.util.HNEventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SimpleCondition;
import org.ocap.hn.ContentServerEvent;
import org.ocap.hn.ContentServerListener;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.StreamingActivityListener;
import org.ocap.hn.service.ServiceResolutionHandler;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.davic.net.tuning.NetworkInterface;
import org.dvb.application.AppID;

/**
 * ContentServerNetModuleImpl - implementation class for
 * <code>ContentServerNetModule</code>.
 *
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * @version $Revision$
 *
 * @see {@link org.ocap.hn.ContentServerNetModule}
 */
public class ContentServerNetModuleImpl extends NetModuleImpl implements ContentServerNetModule, UPnPActionResponseHandler
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(ContentServerNetModuleImpl.class);

    /** Browse Action Qualifier string to indicate requestRootContainer() **/
    private final String ROOT_CONTAINER_REQUEST = "RootContainerRequest";
    
    /** Browse Root Container parameter */
    private final String ROOT = "0";

    /** Browse Root Container parameter */
    private final String ROOT_FILTER = "*";

    /** Browse Root Container parameter */
    private final String ROOT_SORT = "";

    /** Browse Root Container parameter */
    private final int ROOT_START = 0;

    /** Browse Root Container parameter */
    private final int ROOT_RETURN = 0;

    public ServiceResolutionHandlerProxy srhProxy = null;
    
    /** permissions */
    private static final HomeNetPermission CONTENT_LISTING_PERMISSION = new HomeNetPermission("contentlisting");
    private static final HomeNetPermission CONTENT_MANAGEMENT_PERMISSION = new HomeNetPermission("contentmanagement");

    /** Action Object ID key flag */
    private static final String OBJECT_ID = "ObjectID";

    /** Action Browse Flag key flag */
    private static final String BROWSE_FLAG = "BrowseFlag";

    /** Action Filter key flag */
    private static final String FILTER = "Filter";

    /** Action Starting Index key flag */
    private static final String STARTING_INDEX = "StartingIndex";

    /** Action Requested Count key flag */
    private static final String REQUESTED_COUNT = "RequestedCount";

    /** Action Sort Criteria key flag */
    private static final String SORT_CRITERIA = "SortCriteria";

    /** Action Browse Metadata key flag */
    private static final String BROWSE_METADATA = "BrowseMetadata";

    /** Action Browse Direct Children key flag */
    private static final String BROWSE_DIRECT_CHILDREN = "BrowseDirectChildren";
    
    /** Action Container ID key flag */
    private static final String CONTAINER_ID = "ContainerID";

    /** Action Search Criteria key flag */
    private static final String SEARCH_CRITERIA = "SearchCriteria";
    
    private static final String SEARCH_CAPS = "SearchCaps";

    /** Hashmap to store wrappers for Streaming Activity listeners */
    private HashMap saListeners = new HashMap();
 
    // StreamingActivityListener data key Object
    private final Object m_streamingActDataKey = new Object();

    /**
     * Creates a new ContentServerNetModuleImpl object.
     *
     * @param device
     *            the device this NetModule belongs to
     * @param service
     *            the service representing this NetModule
     * @param actions
     *            A list of actions relative to the service.
     */
    public ContentServerNetModuleImpl(UPnPClientService service)
    {
        super(service);
    }
 

    /**
     * {@inheritDoc}
     */
    public void setServiceResolutionHandler(ServiceResolutionHandler handler)
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        
        if (!this.isLocal())
        {
            throw new UnsupportedOperationException("setServiceResolutionHandler was called on a remote ContentServerNetModule");
        }
        
        // If an SPI provider is already registered for 'ocap://' locatorScheme
        // Throw an exception
        String scheme = "ocap";
        ProviderRegistryExt providerRegistry = (ProviderRegistryExt) (ProviderRegistryExt.getInstance());
        if(providerRegistry.isLocatorSchemeRegistered(scheme))
        {
            throw new UnsupportedOperationException("setServiceResolutionHandler was called with ocap locator scheme which is already registered..");
        }  
        
        // If a ServiceResolutionHandler was previously set, remove it now       
        if (srhProxy != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setServiceResolutionHandler() disposing the ServiceResolutionHandler: " + srhProxy);
            }
            srhProxy.dispose();
            srhProxy = null;
        }
       
        // If a null handler is passed in, nothing to do, return
        if (handler == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setServiceResolutionHandler() removed ServiceResHandler..");
            }
            return;
        }
        
        // Create ServiceResolutionHandlerProxy (container for the handler)
        srhProxy = new ServiceResolutionHandlerProxy(handler);
        
        if (log.isDebugEnabled())
        {
            log.debug("setServiceResolutionHandler() - created srhProxy: " + srhProxy);
        }
    }

    public ServiceResolutionHandlerProxy getServiceResolutionHandlerProxy()
    {
        return srhProxy;
    }
    
    // Called from ChannelContentItemImpl
    public boolean resolveTuningLocator(final ChannelContentItem channelItem)
    {
        // If there is no SRH registered return false..
        if(srhProxy == null)
        {
            if (log.isInfoEnabled()) 
            {
                log.info("resolveTuningLocator - srhProxy is null - returning false");
            }
            return false;
        }
        
        // We don't have tuning parameters for the channel locator
        // If a ServiceResolutionHandler is registered resolve channel locator
        final boolean[] resolve = new boolean[] { false};
        // This call needs to be made on the ServiceResolutionHandler context
        if (CallerContext.Util.doRunInContextSync(srhProxy.getHandlerCC(), new Runnable()
        {
            public void run()
            {
                resolve[0] = srhProxy.getHandler().resolveChannelItem(channelItem);
            }
        }))
        {        
            if (log.isInfoEnabled()) 
            {
                log.info("resolveTuningLocator - srhProxy is not null - returning: " + resolve[0]);
            }
            return resolve[0];
        }
        
        return false;
    }
        
    public boolean notifyTuningFailed(final ChannelContentItem channelItem)
    {
        // If there is no SRH registered return false..
        // If 'false' tuning will not be retried
        if(srhProxy == null)
        {
            return false;
        }

        final boolean[] retry = new boolean[] { false};
        // This call needs to be made on the ServiceResolutionHandler context
        if (CallerContext.Util.doRunInContextSync(srhProxy.getHandlerCC(), new Runnable()
        {
            public void run()
            {
                retry[0] = srhProxy.getHandler().notifyTuneFailed(channelItem);
            }
        }))
        {        
            return retry[0];
        }
        
        return false;
    }
    
    /**
     * Adds a listener
     *
     * @param listener
     *            to be added.
     */
    public synchronized void addContentServerListener(ContentServerListener listener)
    {
        boolean needToSubscribe = (ccList == null);
        
        Data data = getData(ccm.getCurrentContext());
        data.contentServerListeners = HNEventMulticaster.add(data.contentServerListeners, listener);
        
        if (needToSubscribe && ccList != null)
        {
            // Since this is the first listener, need to subscribe to the
            // service
            UPnPClientService service = this.getService();
            service.setSubscribedStatus(true);
            service.addStateVariableListener(this);
        }
    }

    /**
     * Removes a specified Content Change Listener
     *
     * @param listener
     *            reference to be removed from the contetnServerListener list.
     */
    public void removeContentServerListener(ContentServerListener listener)
    {
        CallerContext ctx = ccm.getCurrentContext();
        Data data = (Data)ctx.getCallbackData(this);
        if (data != null)
        {
            if (data.contentServerListeners != null)
            {
                data.contentServerListeners = HNEventMulticaster.remove(data.contentServerListeners, listener);
            }
            if (data.contentServerListeners == null)
            {
                ctx.removeCallbackData(this);
                ccList = CallerContext.Multicaster.remove(ccList, ctx);
            }
        }
        
        if (ccList == null)
        {
            UPnPClientService service = this.getService();
            service.setSubscribedStatus(false);
            service.removeStateVariableListener(this);
        }
    }

    /**
     * Request Browse Entries from the CDS this Service belongs to.
     *
     * @param startingEntryID
     *            The parent container ID
     * @param propertyFilter
     *            A comma delimited string of properties the returned items must
     *            contain.
     * @param browseChildren
     *            Browse all of the children of the startingEntryID.
     * @param startingIndex
     *            The Starting child index to start the browse.
     * @param requestedCount
     *            The number of items to be returned.
     * @param sortCriteria
     *            A comma delimited string defining how to sort the browsed
     *            content.
     * @param handler
     *            An event notification object used to notify the consumer when
     *            the action has completed.
     *
     * @return NetActionRequest
     *
     * @throws IllegalArgumentException
     *             Thrown if the arguments passed in are out of range.
     * @throws SecurityException
     *             Thrown if this request is made by an application that is not
     *             authorized.
     */
    public NetActionRequest requestBrowseEntries(String startingEntryID, String propertyFilter, 
            boolean browseChildren, int startingIndex, int requestedCount, String sortCriteria, 
            NetActionHandler handler)
    throws IllegalArgumentException, SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_LISTING_PERMISSION);

        if (handler == null)
        {
            throw new IllegalArgumentException("requestBrowseEntries() - handler is null");
        }
        
        NetActionRequestImpl browseNetActionRequest = null;
        UPnPAction browseAction = uService.getAction(ContentDirectoryService.BROWSE);
        
        if (browseAction != null)
        {
            browseNetActionRequest = new NetActionRequestImpl(handler);
            
            UPnPActionInvocation invocation = createBrowseActionInvocation(browseAction, startingEntryID, 
                                                propertyFilter, browseChildren,
                                                startingIndex, requestedCount, sortCriteria, browseNetActionRequest);

            uService.postActionInvocation(invocation, this);
            
            browseNetActionRequest.setActionStatus(NetActionEvent.ACTION_IN_PROGRESS);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("requestBrowseEntries() - Unable to get action named: " +
                ContentDirectoryService.BROWSE);
            }
        }

        return browseNetActionRequest;
    }

    /**
     * Returns the Root Container of this CDS
     *
     * @param handler
     *            The Event Notification Handler
     *
     * @return NetActionRequest
     *
     * @throws SecurityException
     *             Thrown if the caller is not authorized.
     * @throws IllegalArgumentException
     *             Thrown if the arguments are out of range.
     */
    public NetActionRequest requestRootContainer(NetActionHandler handler) throws SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_LISTING_PERMISSION);

        if (handler == null)
        {
            throw new IllegalArgumentException("requestRootContainer() - handler is null");
        }

        NetActionRequestImpl browseRootNetActionRequest = null;
        UPnPAction browseAction = uService.getAction(ContentDirectoryService.BROWSE);
        
        if (browseAction != null)
        {
            browseRootNetActionRequest = new NetActionRequestImpl(handler, ROOT_CONTAINER_REQUEST);
            
            UPnPActionInvocation invocation = createBrowseActionInvocation(browseAction, ROOT, 
                                                ROOT_FILTER, false, ROOT_START, ROOT_RETURN, ROOT_SORT,
                                                browseRootNetActionRequest);


            uService.postActionInvocation(invocation, this);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("requestBrowseEntries() - Unable to get action named: " +
                ContentDirectoryService.BROWSE);
            }
        }

        return browseRootNetActionRequest;
    }

    /**
     * Returns the Search capabilities for this Service
     *
     * @param handler
     *            Event notification
     *
     * @return NetActionRequest.
     */
    public NetActionRequest requestSearchCapabilities(NetActionHandler handler)
    {
        NetActionRequestImpl searchCapabilitiesActionRequest = null;
        UPnPAction searchCapabilitiesAction = uService.getAction(ContentDirectoryService.GET_SEARCH_CAPABILITIES);
        
        if (searchCapabilitiesAction != null)
        {
            String[] argVals = new String[0];
            
            searchCapabilitiesActionRequest = new NetActionRequestImpl(handler);            
            UPnPActionInvocation invocation = new NetModuleActionInvocation(argVals, searchCapabilitiesAction, 
                    searchCapabilitiesActionRequest);

            uService.postActionInvocation(invocation, this);
            
            searchCapabilitiesActionRequest.setActionStatus(NetActionEvent.ACTION_IN_PROGRESS);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("requestBrowseEntries() - Unable to get action named: " +
                ContentDirectoryService.GET_SEARCH_CAPABILITIES);
            }
        }

        return searchCapabilitiesActionRequest;
    }

    /**
     * Request Search Entries
     *
     * @param parentID
     *            The parent Container ID
     * @param propertyFilter
     *            A filter specifying properties
     * @param startingIndex
     *            The starting index
     * @param requestedCount
     *            The number of items to return from the search
     * @param searchCriteria
     *            The criteria for the search as a comma delimited string.
     * @param sortCriteria
     *            The criteria to sort the items returned from the search
     * @param handler
     *            An event handler passed in by the consumer.
     *
     * @return NetActionRequest
     *
     * @throws IllegalArgumentException
     *             If the arguments passed in don't meet a specified range.
     * @throws SecurityException
     *             Unauthorized request.
     */
    public NetActionRequest requestSearchEntries(String parentID, String propertyFilter, int startingIndex,
            int requestedCount, String searchCriteria, String sortCriteria, NetActionHandler handler)
            throws IllegalArgumentException, SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_LISTING_PERMISSION);

        if (handler == null)
        {
            throw new IllegalArgumentException("requestSearchEntries() - handler is null");
        }

        NetActionRequestImpl searchNetActionRequest = null;
        UPnPAction searchAction = uService.getAction(ContentDirectoryService.SEARCH);
        
        if (searchAction != null)
        {
            searchNetActionRequest = new NetActionRequestImpl(handler);
            
            UPnPActionInvocation invocation = createSearchActionInvocation(searchAction, parentID,
                                                propertyFilter, startingIndex, requestedCount, 
                                                searchCriteria, sortCriteria, searchNetActionRequest);
            
            uService.postActionInvocation(invocation, this);
            
            searchNetActionRequest.setActionStatus(NetActionEvent.ACTION_IN_PROGRESS);
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("requestBrowseEntries() - Unable to get action named: " +
                ContentDirectoryService.SEARCH);
            }
        }

        return searchNetActionRequest;
    }
    /**
     * Notifies the listener that the value of the UPnP state variable being
     * listened to has changed.
     *
     * @param variable The UPnP state variable that changed.
     */
    public void notifyValueChanged(UPnPClientStateVariable variable)
    {

        if(variable == null)
        {
            return;
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("ContentServerNetModuleImpl.notifyEvent() - " + 
            "Name: " + variable.getName() + ", value: '" + variable.getEventedValue() + "'");
        }

        if (ContentDirectoryService.LAST_CHANGE.equals(variable.getName()))
        {
            // Get the value which will be an XML document
            Node valueDoc = MiniDomParser.parse(variable.getEventedValue());

            if (valueDoc != null)
            {
                // Get the name of the root node which indicates specific service event type
                if (log.isDebugEnabled())
                {
                    log.debug("ContentServerNetModuleImpl.notifyEvent() - " +
                    "Service Event type name: " + valueDoc.getName().localPart());
                }

                // Determine if this is a Last Change State event
                if (ContentDirectoryService.STATE_EVENT.equals(valueDoc.getName().localPart()))
                {
                    notifyStateEvent(valueDoc);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentServerNetModuleImpl.notifyEvent() - value document is null");
                }
            }
        }
    }
    
    /**
     * Method invoked when UPnP action completes due to UPnPActionResponseHandler
     * registration.  This method performs the necessary actions and initiates
     * notification of Net
     */
    public void notifyUPnPActionResponse(UPnPResponse uResponse)
    {
        // Programmatic error if this comes back as another type.
        assert (uResponse.getActionInvocation() instanceof NetModuleActionInvocation);

        NetModuleActionInvocation invocation = (NetModuleActionInvocation)uResponse.getActionInvocation();
        NetActionRequestImpl request = (NetActionRequestImpl)invocation.getNetActionRequest();
        AppID callerAppId = request.getCallerContext() != null ? (AppID)request.getCallerContext().get(CallerContext.APP_ID) : null;

        UPnPAction action = invocation.getAction();
        
        // Get NetActionEvent error code 
        int error = getNetActionEventErrorCode(uResponse);
        
        // Get NetActionEvent status code
        int actionStatus = getNetActionEventStatusCode(uResponse);
        
        // Response may be a ContentListImpl
        Object response = null;
        
        // *TODO* - add cases for all supported actions
        // Make sure this is an expected action response
        UPnPActionResponse aResponse = null;
        if (uResponse instanceof UPnPActionResponse)
        {
            aResponse = (UPnPActionResponse)uResponse;

            // Handle special case ContentServerNetModule.getRootContainer() browse action
            if (action.getName().equalsIgnoreCase(ContentDirectoryService.BROWSE))
            {
                if ((((NetActionRequestImpl)invocation.getNetActionRequest()).getActionQualifier() != null) &&
                    (((NetActionRequestImpl)invocation.getNetActionRequest()).getActionQualifier().equals(
                            ROOT_CONTAINER_REQUEST)))
                {
                    // This w<s a requestRootContainer() browse action 
                    response = handleRootContainerActionResponse(aResponse, actionStatus);            
                }
                else
                {
                    response = handleContentListActionResponse(aResponse, actionStatus, callerAppId);
                }
            }
            else if (action.getName().equalsIgnoreCase(ContentDirectoryService.SEARCH))
            {
                response = handleContentListActionResponse(aResponse, actionStatus, callerAppId);
            }
            else if (action.getName().equalsIgnoreCase(ContentDirectoryService.GET_SEARCH_CAPABILITIES))
            {
                response = handleGetSearchCapabilitiesActionResponse(aResponse, actionStatus);
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
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("notifyUPnPActionResponse() - received error response for action: " +
                action.getName());
            }
        }
                
        request.notifyHandler(new NetActionEventImpl(request, response, error, actionStatus));
    }
    
    /**
     * Add a StreamingActivityListener 
     *
     * @param listener - StreamingActivityListener to add 
     * @param contentTypes - Type of streaming activity to be notified of 
     */
    public synchronized void addStreamingActivityListener(StreamingActivityListener listener, int contentTypes) 
    {
        if (!this.isLocal())
        {
            if (log.isErrorEnabled())
            {
                log.error("addStreamingActivityListener was called on a remote ContentServerNetModule.");
            }
            throw new UnsupportedOperationException();
        }

        if (null == listener)
        {
            throw new IllegalArgumentException("Null StreamingActivityListener passed to AddStreamingActivityListener - contentTypes: " + contentTypes);
        }

        StreamingActivityData data = getStreamingActivityData(ccm.getCurrentContext());

        if (StreamingActivityListener.CONTENT_TYPE_ALL_RESOURCES == contentTypes)
        {
            AllStreamingActivityListenerImpl allListener = new AllStreamingActivityListenerImpl(listener);
            data.allStreamingActivityListeners = HNEventMulticaster.add(data.allStreamingActivityListeners, allListener);
            saListeners.put(listener.toString(), allListener);
        }
        else if (StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES == contentTypes)
        {
            ChannelStreamingActivityListenerImpl channelListener = new ChannelStreamingActivityListenerImpl(listener);
            data.channelStreamingActivityListeners = HNEventMulticaster.add(data.channelStreamingActivityListeners, channelListener);
            saListeners.put(listener.toString(), channelListener);
        }
        else if (StreamingActivityListener.CONTENT_TYPE_RECORDED_RESOURCES == contentTypes)
        {
            RecordingStreamingActivityListenerImpl recordingListener = new RecordingStreamingActivityListenerImpl(listener);
            data.recordingStreamingActivityListeners = HNEventMulticaster.add(data.recordingStreamingActivityListeners, recordingListener);
            saListeners.put(listener.toString(), recordingListener);
        }
        else
        {
            throw new IllegalArgumentException("Unexpected content types in addStreamingActivityListener - listener: " + listener + ", content types: " + contentTypes);
        }

    }

    /**
     * Remove a StreamingActivityListener 
     *
     * @param listener - StreamingActivityListener to remove 
     */
    public synchronized void removeStreamingActivityListener(StreamingActivityListener listener) 
    {

        CallerContext ctx = ccm.getCurrentContext();
        StreamingActivityData data = getStreamingActivityData(ccm.getCurrentContext());

        // Go thru each list and look up the wrapper object associated with the listener
        // and remove the wrapper and the listener 
        if (data != null)
        {
            if (data.allStreamingActivityListeners != null)
            {
               
                AllStreamingActivityListenerImpl allListener = (AllStreamingActivityListenerImpl) saListeners.get(listener.toString());
                data.allStreamingActivityListeners = HNEventMulticaster.remove(data.allStreamingActivityListeners, allListener);
            }
            if (data.allStreamingActivityListeners == null)
            {
                ctx.removeCallbackData(this);
                ccList = CallerContext.Multicaster.remove(streamingActivityCCList, ctx);
            }
        }
        if (data != null && data.channelStreamingActivityListeners != null)
        {
            if (data.channelStreamingActivityListeners != null)
            {
                ChannelStreamingActivityListenerImpl chanListener = (ChannelStreamingActivityListenerImpl) saListeners.get(listener.toString());
                data.channelStreamingActivityListeners = HNEventMulticaster.remove(data.channelStreamingActivityListeners, chanListener);
            }
            if (data.channelStreamingActivityListeners == null)
            {
                ctx.removeCallbackData(this);
                ccList = CallerContext.Multicaster.remove(streamingActivityCCList, ctx);
            }
        }
        if (data != null && data.recordingStreamingActivityListeners != null)
        {
            if (data.recordingStreamingActivityListeners != null)
            {
                RecordingStreamingActivityListenerImpl recListener = (RecordingStreamingActivityListenerImpl) saListeners.get(listener.toString());
                data.recordingStreamingActivityListeners = HNEventMulticaster.remove(data.recordingStreamingActivityListeners, recListener); 
            }
            if (data.allStreamingActivityListeners == null)
            {
                ctx.removeCallbackData(this);
                ccList = CallerContext.Multicaster.remove(streamingActivityCCList, ctx);
            }
        }
       
        // remove listener from wrapper list 
        saListeners.remove(listener.toString());
        
    } 

    /**
     * Notification of streaming started event to be sent to registered listener 
     *
     * @param item - ContentItem streaming is associated with 
     * @param id - ConnectionId of connection 
     * @param uri - uri associated with res block 
     * @param ni - NetworkInterface associated with streaming activity 
     * @param type - type of stream(Live streaming or Recording
     */
    public synchronized void notifyStreamStarted(ContentItem item, int id, String uri, NetworkInterface ni, int type)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentServerNetModuleImpl.notifyChannelStreamStarted() - " + 
            "uri: '" + uri +
            " id " + id + " type: " + type);
        }
        CallerContext ctx = streamingActivityCCList;
        final ContentItem newItem = item;
        final int newId = id;
        final String newURI = uri;
        final NetworkInterface newNI = ni;
        final int newType = type;
        if (ctx != null)
        {
            ctx.runInContextAsync(new Runnable() {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();
                    StreamingActivityData data = (StreamingActivityData)ctx.getCallbackData(m_streamingActDataKey);
                    if (data != null && data.allStreamingActivityListeners != null)
                    {
                            data.allStreamingActivityListeners.notifyStreamingStarted(newItem,
                                newId, newURI, newNI);
                    }
                    if (data != null && data.channelStreamingActivityListeners != null
                        && newType == StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES)
                    {
                            data.channelStreamingActivityListeners.notifyStreamingStarted(newItem,
                                newId, newURI, newNI);
                    }
                    if (data != null && data.recordingStreamingActivityListeners != null
                        && newType == StreamingActivityListener.CONTENT_TYPE_RECORDED_RESOURCES)
                    {
                            data.recordingStreamingActivityListeners.notifyStreamingStarted(newItem,
                                newId, newURI, newNI);
                    }
                }
            });
        }


    }
  
    /**
     * Notification of streaming change event to be sent to registered listener 
     *
     * @param item - ContentItem streaming is associated with 
     * @param id - ConnectionId of connection 
     * @param uri - uri associated with res block 
     * @param ni - NetworkInterface associated with streaming activity 
     * @param type - type of stream
     */
    public synchronized void notifyStreamChange(ContentItem item, int id, String uri, NetworkInterface ni, int type)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentServerNetModuleImpl.notifyChannelStreamChange() - " + 
            "uri: '" + uri +
            " id " + id + " type " + type);
        }
        CallerContext ctx = streamingActivityCCList;
        final ContentItem newItem = item;
        final int newId = id;
        final String newURI = uri;
        final NetworkInterface newNI = ni;
        final int newType = type;

        if (ctx != null)
        {
            ctx.runInContextAsync(new Runnable() {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();
                    StreamingActivityData data = (StreamingActivityData)ctx.getCallbackData(m_streamingActDataKey);
                    if (data != null && data.allStreamingActivityListeners != null)
                    {
                            data.allStreamingActivityListeners.notifyStreamingChange(newItem,
                                newId, newURI, newNI);
                    }
                    if (data != null && data.channelStreamingActivityListeners != null
                        && newType == StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES)
                    {
                            data.channelStreamingActivityListeners.notifyStreamingChange(newItem,
                                newId, newURI, newNI);
                    }
                    if (data != null && data.channelStreamingActivityListeners != null
                        && newType == StreamingActivityListener.CONTENT_TYPE_RECORDED_RESOURCES)
                    {
                            data.recordingStreamingActivityListeners.notifyStreamingChange(newItem,
                                newId, newURI, newNI);
                    }
                }
            });
        }


    }
  
    /**
     * Notification of streaming ended event to be sent to registered listener 
     *
     * @param item - ContentItem streaming is associated with 
     * @param id - ConnectionId of connection 
     * @param reason - reason streaming activity ended 
     * @param type - type of stream
     */
    public synchronized void notifyStreamEnded(ContentItem item, int id, int reason, int type)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentServerNetModuleImpl.notifyChannelStreamEnded() - " + 
            "reason: " + reason +
            ", id: " + id + ", type: " + type);
        }
        CallerContext ctx = streamingActivityCCList;
        final ContentItem newItem = item;
        final int newId = id;
        final int newReason = reason;
        final int newType = type;

        if (ctx != null)
        {
            ctx.runInContextAsync(new Runnable() {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();
                    StreamingActivityData data = (StreamingActivityData)ctx.getCallbackData(m_streamingActDataKey);
                    if (data != null && data.allStreamingActivityListeners != null)
                    {
                            data.allStreamingActivityListeners.notifyStreamingEnded(newItem,
                                newId, newReason);
                    }
                    if (data != null && data.channelStreamingActivityListeners != null
                        && newType == StreamingActivityListener.CONTENT_TYPE_LIVE_RESOURCES )
                    {
                            data.channelStreamingActivityListeners.notifyStreamingEnded(newItem,
                                newId, newReason);
                    }
                    if (data != null && data.recordingStreamingActivityListeners != null
                        && newType == StreamingActivityListener.CONTENT_TYPE_RECORDED_RESOURCES )
                    {
                            data.recordingStreamingActivityListeners.notifyStreamingEnded(newItem,
                                newId, newReason);
                    }
                }
            });
        }


    }
  
    /**
     * This method creates array of string argument values and uses array to create
     * UPnPActionInvocation.
     * 
     * @param browseAction
     * @param startingEntryID
     * @param propertyFilter
     * @param browseChildren
     * @param startingIndex
     * @param requestedCount
     * @param sortCriteria
     * 
     * @return  created UPnPActionInvocation with associated arg values set to supplied values.
     */
    private UPnPActionInvocation createBrowseActionInvocation(UPnPAction browseAction, 
                                                              String startingEntryID, 
                                                              String propertyFilter, 
                                                              boolean browseChildren, 
                                                              int startingIndex, 
                                                              int requestedCount, 
                                                              String sortCriteria,
                                                              NetActionRequest request)
    {
        String argNames[] = browseAction.getArgumentNames();
        
        // Create string array for each of supplied parameter values
        // Not setting values for other arguments
        String argVals[] = new String[6];
        int j = 0;
        
        for (int i = 0; i < argNames.length; i++)
        {
            if (argNames[i].equalsIgnoreCase(BROWSE_FLAG))
            {
                if (browseChildren)
                {
                    argVals[j++] = BROWSE_DIRECT_CHILDREN;
                }
                else
                {
                    argVals[j++] = BROWSE_METADATA;
                }
            }
            else if (argNames[i].equalsIgnoreCase(OBJECT_ID))
            {
                // *TODO* - I thought this was a workaround for HN Interop issues
                // Is this in accordance with the spec?
                if (startingEntryID == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("createBrowseActionInvocation() - " +
                        "setting null starting entry ID to 0");
                    }
                    argVals[j++] = "0";
                }
                else if (startingEntryID.equals(""))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("createBrowseActionInvocation() - " +
                        "setting empty string starting entry ID to 0");
                    }
                    argVals[j++] = "0";
                }
                else
                {
                    argVals[j++] = startingEntryID;
                }
            }
            else if (argNames[i].equalsIgnoreCase(FILTER))
            {
                if (propertyFilter == null)
                {
                    argVals[j++] = "";
                }
                else
                {
                    argVals[j++] = propertyFilter;
                }
            }
            else if (argNames[i].equalsIgnoreCase(STARTING_INDEX))
            {
                argVals[j++] = Integer.toString(startingIndex);
            }
            else if (argNames[i].equalsIgnoreCase(REQUESTED_COUNT))
            {
                argVals[j++] = Integer.toString(requestedCount);
            }
            else if (argNames[i].equalsIgnoreCase(SORT_CRITERIA))
            {
                if (sortCriteria == null)
                {
                    argVals[j++] = "";
                }
                else
                {
                    argVals[j++] = sortCriteria;
                }
            }
        }

        // *TODO* - used to verify the args are valid, used to throw invalid arg exception
        // Is this still the right thing to do?
        if (propertyFilter == null || sortCriteria == null || requestedCount < 0 || startingIndex < 0)
        {
            throw new IllegalArgumentException("createBrowseActionInvocation() - has invalid arguments");
        }
        
        //*TODO* - handle any exceptions which can be thrown here
        return new NetModuleActionInvocation(argVals, browseAction, request);
    }
    
    /**
     * 
     * @param parentID
     * @param propertyFilter
     * @param startingIndex
     * @param requestedCount
     * @param searchCriteria
     * @param sortCriteria
     * @return
     */
    private UPnPActionInvocation createSearchActionInvocation(UPnPAction searchAction, 
                                                              String parentID, 
                                                              String propertyFilter, 
                                                              int startingIndex, 
                                                              int requestedCount, 
                                                              String searchCriteria, 
                                                              String sortCriteria,
                                                              NetActionRequest request)
    {
        String argNames[] = searchAction.getArgumentNames();
        
        // Create string array for each of supplied parameter values
        // Not setting values for other arguments
        String argVals[] = new String[6];
        int j = 0;
        
        for (int i = 0; i < argNames.length; i++)
        {
            if (argNames[i].equalsIgnoreCase(SEARCH_CRITERIA))
            {
                if (searchCriteria == null)
                {
                    searchCriteria = "*";
                }
                argVals[j++] = searchCriteria;
            }
            else if (argNames[i].equalsIgnoreCase(SORT_CRITERIA))
            {
                if (sortCriteria == null)
                {
                    sortCriteria = "";
                }
                argVals[j++] = sortCriteria;
            }
            else if (argNames[i].equalsIgnoreCase(CONTAINER_ID))
            {
                // *TODO* - isn't this an hn-interop issue?
                if (parentID == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("ContentServerNetModuleImpl.requestSearchEntries() - " +
                        "setting null parent ID to 0");
                    }
                    parentID = "0";
                }
                else if ("".equals(parentID))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("ContentServerNetModuleImpl.requestSearchEntries() - " +
                        "setting empty string parent ID to 0");
                    }
                    parentID = "0";
                }                
                argVals[j++] = parentID;
            }
            else if (argNames[i].equalsIgnoreCase(FILTER))
            {
                if (propertyFilter == null)
                {
                    argVals[j++] = "";
                }
                else
                {
                    argVals[j++] = propertyFilter;
                }
            }
            else if (argNames[i].equalsIgnoreCase(STARTING_INDEX))
            {
                argVals[j++] = Integer.toString(startingIndex);
            }
            else if (argNames[i].equalsIgnoreCase(REQUESTED_COUNT))
            {
                argVals[j++] = Integer.toString(requestedCount);
            }
        }
        
        // *TODO* - used to verify the args are valid, used to throw invalid arg exception
        // Is this still the right thing to do?
        if (propertyFilter == null || requestedCount < 0 || startingIndex < 0)
        {
            throw new IllegalArgumentException("createSearchActionInvocation() - has invalid arguments");
        }
        
        //*TODO* - handle any exceptions which can be thrown here?
        return new NetModuleActionInvocation(argVals, searchAction, request);
    }
    
    /**
     * Extract the necessary information from the UPnP action response
     * and create content list to return entries.
     * 
     * @param   uResponse   response received from UPnP browse action
     * 
     * @return  content list containing returned entries
     */
    private Object handleContentListActionResponse(UPnPActionResponse aResponse, int actionStatus, AppID callerAppId)
    {
        Object response = null;
        
        // Generate and check the ContentList if the action succeeded 
        if (actionStatus == NetActionEvent.ACTION_COMPLETED)
        {
            // TODO : investigate usage, should throw NPE now
            ContentListImpl contentList = ContentFactory.getInstance().getContentEntries(aResponse);

            response = new ContentListImpl();

            for (Iterator i = contentList.iterator(); i.hasNext();)
            {
                Object o = i.next();

                if (o instanceof ContentEntryImpl)
                {
                    // Locally filter out hidden items prior
                    // to returning to calling application.
                    if(((ContentEntry)o).isLocal())
                    {
                        // Resolve returned entry to local CDS entry
                        // So we can walk tree to figure out permissions
                        ContentEntry cdsEntry = 
                            MediaServer.getInstance().getCDS().getRootContainer().getEntry(
                                    ((ContentEntry)o).getID());
                        
                        if(canBeRead((ContentEntryImpl)cdsEntry, callerAppId))
                        {
                            ((ContentListImpl)response).add(o);
                        }
                    }
                    else  // Remote query, no filtering required.
                    {
                        ((ContentListImpl)response).add(o);
                    }
                }
            }
        }
        else
        {
            response = null;
        }
        
        return response;
    }
    
    private boolean canBeRead(ContentEntryImpl ce, AppID callerAppId)
    {
        // If entry is null it could be end of recursive search
        // In either case there are no permissions to check so
        // default would be true.
        if(ce == null)
        {
            return true;
        }
        
        // If this entry can not be accessed then fail and stop
        // recursing.
        if(!ce.canBeAccessed(true, callerAppId))
        {
            return false;
        }

        // Recursively check parent entries for access rights
        try
        {
            return canBeRead((ContentEntryImpl)ce.getEntryParent(), callerAppId);
        }
        catch(IOException ioe)
        {
            if(log.isWarnEnabled())
            {
                log.warn("ContentEntry parent not found in local CDS id = " + ce.getID());
            }
        }

        // Everything checked out.
        return true;
    }

    private Object handleRootContainerActionResponse(UPnPActionResponse aResponse, int actionStatus)
    {
        Object response = null;
        
        // Generate and check the ContentList if the action succeeded 
        if (actionStatus == NetActionEvent.ACTION_COMPLETED)
        {
            ContentListImpl contentList = ContentFactory.getInstance().getContentEntries(aResponse);
            response = (ContentContainerImpl) contentList.getContentEntry(0);
        }
        
        return response;
    }
    
    private Object handleGetSearchCapabilitiesActionResponse(UPnPActionResponse aResponse, int actionStatus)
    {
        String[] searchResponse = null;
        
        if (actionStatus == NetActionEvent.ACTION_COMPLETED)
        {
            // Get out arg named SearchCap from response
            String searchCapabilities = aResponse.getArgumentValue(SEARCH_CAPS);
            
            // Must parse the returned value, which is a String comma separated list, into String[].
            int commas = 0;
            int ndx;
            if (searchCapabilities != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("handleGetSearchCapabilitiesActionResponse() - " + 
                            "Search Capabilities returned string was " + searchCapabilities);
                }
                // if no search capabilities are returned, return empty string array
                if (searchCapabilities.length() == 0)
                {
                    searchResponse = new String[0];
                }
                else
                {
                    // count commas in string
                    for (ndx = 0; ndx < searchCapabilities.length(); ndx++)
                    {
                        if (searchCapabilities.charAt(ndx) == ',') ++commas;
                    }
                    // allocate the String array
                    searchResponse = new String[commas + 1];
                    int start = 0;
                    int last;
                    // Add parsed strings to array
                    for (ndx = 0; ndx < commas; ndx++)
                    {
                        last = searchCapabilities.indexOf(',', start);
                        searchResponse[ndx] = new String(searchCapabilities.substring(start, last));
                        start = last + 1;
                    }
                    // assign last part of list to array (could be whole string
                    // if no commas)
                    searchResponse[ndx] = new String(searchCapabilities.substring(start));
                }
            }
        }
        
        return searchResponse;
    }
    
    /**
     * Extract the information from the supplied Last Change State Event
     * and generate ContentServerEvents of type content added, changed or
     * removed and send to registered listeners.
     *
     * @param valueDoc The XML document from the event.
     */
    private void notifyStateEvent(Node valueDoc)
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentServerNetModuleImpl.notifyStateEvent() - called");
        }

        // Gather a list of added, modified and deleted object id's
        List addIDs = new ArrayList();
        List modIDs = new ArrayList();
        List delIDs = new ArrayList();

        // Find nodes and add to associated lists

        // Look for addObj, modObj, and delObj child nodes
        if (valueDoc.hasChildNodes())
        {
            NodeList childNodes = valueDoc.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                Node childNode = childNodes.item(i);
                String childName = childNode.getName() != null ? childNode.getName().localPart() : null;

                if (ContentDirectoryService.OBJ_ADD.equals(childName) ||
                    ContentDirectoryService.OBJ_MOD.equals(childName) ||
                    ContentDirectoryService.OBJ_DEL.equals(childName))
                {
                    // Found an add, mod or del node, get the value of the ID from attribute
                    if (log.isDebugEnabled())
                    {
                        log.debug("ContentServerNetModuleImpl.notifyStateEvent() - " +
                        "found object child node");
                    }
                    NamedNodeMap attributes = childNode.getAttributes();
                    if (attributes != null)
                    {
                        // Look for the ID attribute
                        boolean foundID = false;
                        for (int j = 0; j < attributes.getLength(); j++)
                        {
                            Node attr = attributes.item(j);
                            String attrName = attr.getName().localPart();
                            if (ContentDirectoryService.OBJ_ID.equals(attrName))
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("ContentServerNetModuleImpl.notifyStateEvent() - " +
                                    "found object id attribute, adding to list");
                                }
                                // Found ID attribute, add ID value to appropriate list
                                if (ContentDirectoryService.OBJ_ADD.equals(childName))
                                {
                                    addIDs.add(attr.getValue());
                                }
                                else if (ContentDirectoryService.OBJ_MOD.equals(childName))
                                {
                                    modIDs.add(attr.getValue());
                                }
                                else // ContentDirectoryService.OBJ_DEL.equals(childName))
                                {
                                    delIDs.add(attr.getValue());
                                }
                                foundID = true;
                                break;
                            }
                        }
                        if (!foundID)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("ContentServerNetModuleImpl.notifyStateEvent() - " +
                                "Event Node " + childNode.getName().localPart() + " had no ID attr, event XML: " +
                                valueDoc);
                            }
                        }
                    }
                }
            }
        }

        // Create an event for each type of object that was in the last change state event
        if (!addIDs.isEmpty())
        {
            final ContentServerEvent addEvent = new ContentServerEvent(this,
                                                (String[])addIDs.toArray(new String[addIDs.size()]),
                                                ContentServerEvent.CONTENT_ADDED);
            CallerContext ctx = ccList;
            if (ctx != null)
            {
                ctx.runInContextAsync(new Runnable() {
                    public void run()
                    {
                        CallerContext ctx = ccm.getCurrentContext();
                        Data data = (Data)ctx.getCallbackData(ContentServerNetModuleImpl.this);
                        if (data != null && data.contentServerListeners != null)
                        {
                            data.contentServerListeners.contentUpdated(addEvent);
                        }
                    }
                });
            }
        }
    
        if (!modIDs.isEmpty())
        {
            final ContentServerEvent modEvent = new ContentServerEvent(this,
                                                (String[])modIDs.toArray(new String[modIDs.size()]),
                                                ContentServerEvent.CONTENT_CHANGED);
            CallerContext ctx = ccList;
            if (ctx != null)
            {
                ctx.runInContextAsync(new Runnable() {
                    public void run()
                    {
                        CallerContext ctx = ccm.getCurrentContext();
                        Data data = (Data)ctx.getCallbackData(ContentServerNetModuleImpl.this);
                        if (data != null && data.contentServerListeners != null)
                        {
                            data.contentServerListeners.contentUpdated(modEvent);
                        }
                    }
                });
            }
        }
        if (!delIDs.isEmpty())
        {
            final ContentServerEvent delEvent = new ContentServerEvent(this,
                                                (String[])delIDs.toArray(new String[delIDs.size()]),
                                                ContentServerEvent.CONTENT_REMOVED);
            CallerContext ctx = ccList;
            if (ctx != null)
            {
                ctx.runInContextAsync(new Runnable() {
                    public void run()
                    {
                        CallerContext ctx = ccm.getCurrentContext();
                        Data data = (Data)ctx.getCallbackData(ContentServerNetModuleImpl.this);
                        if (data != null && data.contentServerListeners != null)
                        {
                            data.contentServerListeners.contentUpdated(delEvent);
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Access this object's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * 
     * @param ctx the context to access
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx)
    {
        Data data = (Data) ctx.getCallbackData(this);
        if (data == null)
        {
            data = new Data();
            ctx.addCallbackData(data, this);
            ccList = CallerContext.Multicaster.add(ccList, ctx);
        }
        return data;
    }

    /**
     * Access this object's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     *
     * @param ctx the context to access
     * @return the <code>Data</code> object
     */
    private StreamingActivityData getStreamingActivityData(CallerContext ctx)
    {
        StreamingActivityData data = (StreamingActivityData) ctx.getCallbackData(m_streamingActDataKey);
        if (data == null)
        {
            data = new StreamingActivityData();
            ctx.addCallbackData(data, m_streamingActDataKey);
            streamingActivityCCList = CallerContext.Multicaster.add(streamingActivityCCList, ctx);
        }
        return data;
    }

    
    /**
     * Per-context global data. Remembers per-context
     * <code>DeviceEventListener</code>s. and
     * <code>NetModuleEventListeners</code>s.
     */
    private class Data implements CallbackData
    {
        public ContentServerListener contentServerListeners;

        public void destroy(CallerContext cc)
        {
            synchronized (ContentServerNetModuleImpl.this)
            {
                // Simply forget the given cc
                // No harm done if never added
                cc.removeCallbackData(ContentServerNetModuleImpl.this);
                ccList = CallerContext.Multicaster.remove(ccList, cc);
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }

    /**
     * Per-context global data. Remembers per-context
     * StreamingActivityListener.
     */
    private class StreamingActivityData implements CallbackData
    {
        public AllStreamingActivityListenerImpl allStreamingActivityListeners;
        public RecordingStreamingActivityListenerImpl recordingStreamingActivityListeners;
        public ChannelStreamingActivityListenerImpl channelStreamingActivityListeners;

        public void destroy(CallerContext cc)
        {
            synchronized (ContentServerNetModuleImpl.this)
            {
                // Simply forget the given cc
                // No harm done if never added
                cc.removeCallbackData(m_streamingActDataKey);
                streamingActivityCCList = CallerContext.Multicaster.remove(streamingActivityCCList, cc);
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }
    /**
     * List of <code>CallerContext</code>s that have added listeners.
     */
    private CallerContext ccList;
    private CallerContext streamingActivityCCList;
    
    private static CallerContextManager ccm =
        (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);
}
