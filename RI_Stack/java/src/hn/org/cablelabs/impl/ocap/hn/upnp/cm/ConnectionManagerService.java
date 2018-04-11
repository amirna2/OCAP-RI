/*Copyright (c) 2004-2006, Satoshi Konno Copyright (c) 2005-2006,
Nokia Corporation Copyright (c) 2005-2006, Theo Beisch Collectively
the Copyright Owners All rights reserved
 */
package org.cablelabs.impl.ocap.hn.upnp.cm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedServiceImpl;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.hn.upnp.server.UPnPManagedStateVariable;
import org.ocap.hn.upnp.server.UPnPStateVariableHandler;

/**
 * Connection Manager
 *
 * @author Michael Jastad
 * @version $Revision$
 */
public final class ConnectionManagerService implements UPnPActionHandler, UPnPStateVariableHandler
{
    private static final Logger log = Logger.getLogger(ConnectionManagerService.class);

    /** UPnP Connection Manager service Type */
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:2";

    // UPnP Connection Manager Actions 
    //
    public static final String GET_PROTOCOL_INFO = "GetProtocolInfo";
    public static final String CONNECTION_COMPLETE = "ConnectionComplete";
    public static final String GET_CURRENT_CONNECTION_IDS = "GetCurrentConnectionIDs";
    public static final String GET_CURRENT_CONNECTION_INFO = "GetCurrentConnectionInfo";

    // List of additional CMS Optional actions which are not supported 
    public static final String PREPARE_FOR_CONNECTION = "PrepareForConnection";

    // UPnP Connection Manager State Variables
    //
    public static final String SINK_PROTOCOL_INFO = "SinkProtocolInfo";
    public static final String SOURCE_PROTOCOL_INFO = "SourceProtocolInfo";
    public static final String CURRENT_CONNECTION_IDS = "CurrentConnectionIDs";

    // UPnP Connection Manager Arguments for Actions
    //
    private final static String CONNECTION_ID = "ConnectionID";
 
    /** Container for Connection Information */
    private final List conInfoList = new ArrayList();

    /** ConnectionIDs */
    private int currentConnectionID;
    //ensure connection ID stays within the range
    private final int maxConnectionId = (int) (Math.pow(2, 31) - 1);
    private final int minConnectionId = 1;

    /** Mutex for locking shared resources */
    private final Object mutex = new Object();

    /** list of connectionCompleteListeners **/
    private Vector connectionCompleteListeners = new Vector();
    
    private Set m_services = new HashSet();

    /**
     * Handle to the NetSecurityManager. Used to authorize the action.
     */
    private NetSecurityManagerImpl securityManager = (NetSecurityManagerImpl) NetSecurityManagerImpl.getInstance();
    
    /**
     * Construct a <code>ConnectionManager</code>.
     */
    public ConnectionManagerService()
    {
        Random random = new Random();
        
        // Seeding currentConnectionID with a valid value
        currentConnectionID = random.nextInt(maxConnectionId - minConnectionId + 1) + minConnectionId;
    }
    
    public void registerService(UPnPManagedService service)
    {
        if(service != null)
        {
            service.setActionHandler(this);
            m_services.add(service);
        }
    }
    
    /**
     * Monotonically Increments the Connection ID
     *
     * @return the next connection id.
     */
    public int getNextConnectionID()
    {
        synchronized(mutex)
        {
            currentConnectionID++;
            //rolling over to min (1)
            if (currentConnectionID > maxConnectionId)
            {
                currentConnectionID = minConnectionId;
            }
        }

        return currentConnectionID;
    }

    /**
     * Returns a ConnectionInfo object from a specified Connection ID
     *
     * @param id
     *            The connection ID to be used as a reference.
     *
     * @return ConnectionInfo that corresponds to the specified Connection ID.
     */
    public ConnectionInfo getConnectionInfo(int id)
    {
        ConnectionInfo info = null;
        synchronized(mutex)
        {
            int size = conInfoList.size();
            for (int n = 0; n < size; n++)
            {
                ConnectionInfo entry = (ConnectionInfo) conInfoList.get(n);

                if (entry.getID() == id)
                {
                    info = entry;
                    break;
                }
            }
        }
    
        // need to return minimal info for id == 0
        if ((info == null) && (id == 0))
        {
            info = new ConnectionInfo(0, -1, -1, new HNStreamProtocolInfo(""), "", -1);
            
        }
        return info;
    }

    /**
     * Adds a ConnectionInfo object to the connection List.
     *
     * @param info
     *            The ConnectionInfo object to be added.
     */
    public void addConnectionInfo(ConnectionInfo info)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addConnectionInfo() - called with: " + info);
        }
        String currentConnectionIds;
        synchronized (mutex)
        {
            conInfoList.add(info);
            currentConnectionIds = getCurrentConnectionIDsStr();
        }
        // Update associated managed state variable
        updateCurrentConnectionIDsStateVariable(currentConnectionIds);
    }

    /**
     * Removes a ConnectionInfo object from the connection list. by a specified
     * connection id.
     *
     * @param id
     *            The connection ID to be removed from the list.
     */
    public void removeConnectionInfo(int id)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeConnectionInfo() - called with: " + id);
        }
        boolean remove = false;
        String currentConnectionIds = null;
        synchronized(mutex)
        {
            int size = conInfoList.size();

            for (int n = 0; n < size; n++)
            {
                ConnectionInfo info = (ConnectionInfo) conInfoList.get(n);

                if (info.getID() == id)
                {
                    conInfoList.remove(info);

                    // Update associated managed state variable
                    remove = true;
                    currentConnectionIds = getCurrentConnectionIDsStr(); 
                    break;
                }
            }
        }
        if (remove)
        {
            updateCurrentConnectionIDsStateVariable(currentConnectionIds);
        }
    }

    /**
     * Performs the necessary logic to update associated state variable.
     * @param currentConnectionIDsStr
     */
    private void updateCurrentConnectionIDsStateVariable(String currentConnectionIDsStr)
    {
        for(Iterator i = m_services.iterator(); i.hasNext();)
        {
            UPnPManagedServiceImpl service = (UPnPManagedServiceImpl)i.next();
            UPnPManagedStateVariable var = service.getManagedStateVariable(CURRENT_CONNECTION_IDS);
            if (var != null)
            {
                var.setValue(currentConnectionIDsStr);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("updateCurrentConnectionIDsStateVariable() - " +
                            "No associated state variable named: " + CURRENT_CONNECTION_IDS);
                }
            }
        }
    }
    
    /**
     * Performs the necessary logic to update associated state variable.
     */
    protected void updateSourceProtocolInfoStateVariable()
    {
        for(Iterator i = m_services.iterator(); i.hasNext();)
        {
            UPnPManagedServiceImpl service = (UPnPManagedServiceImpl)i.next();
            UPnPManagedStateVariable sv = service.getManagedStateVariable(SOURCE_PROTOCOL_INFO);
            if (sv != null)
            {
                sv.setValue(CMSProtocolInfo.getInstance().getSourceProtocolStateVariableValue());
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("updateSourceProtocolInfoStateVariable() - " +
                            "No associated state variable named: " + SOURCE_PROTOCOL_INFO);
                }                            
            }
        }
    }
    
    /**
     * This method validates remote and local protocolInfo are compatible and registers a local ConnectionInfo entry with 
     * the connectionmanager
     * 
     * @param protocol
     *            protocol used to establish connection
     * @param contentType
     *            type type of content being streamed
     * @param contentItem the content item
     * @param sink true if registering a connection on the sink
     * @param localConnectionId local connection ID
     * @param peerConnectionId remote connection ID
     *
     * @throws HNStreamingException
     *             if local and remote protocolInfo are not compatible
     */
    public void validateProtocol(HNStreamProtocolInfo contentItemProtocolInfo)
    throws HNStreamingException
    {
        // *TODO* - this needs to be reworked when player side changes are made since it is only
        // called from RemoteServicePresentation
        /*
        if (log.isInfoEnabled())
        {
            log.info("validateProtocolAndRegisterLocalConnection - protocol: " + protocol + " contentType: "
            + contentType + ", content item: " + contentItem + ", sink: " + sink + ", local connection ID: " + localConnectionId + 
            ", peer connection ID: " + peerConnectionId);
        }


        //source protocolInfo is available as metadata of the root metadata node
        String[] contentItemProtocolInfoArray = (String[]) ((MetadataNodeImpl) contentItem.getRootMetadataNode()).getMetadata(UPnPConstants.QN_DIDL_LITE_RES_PROTOCOL_INFO);
        //convert array to string
        StringBuffer contentItemProtocolInfoString = new StringBuffer();
        for (int ii = 0; ii < contentItemProtocolInfoArray.length; ii++)
        {
            contentItemProtocolInfoString.append(contentItemProtocolInfoArray[ii]);
            if (ii < (contentItemProtocolInfoArray.length - 1))
            {
                contentItemProtocolInfoString.append(",");
            }
        }

        ProtocolInfo contentItemProtocolInfo = new ProtocolInfo(contentItemProtocolInfoString.toString());
        
        // If sink, which means this validation is to determine if this player supports this content item
        if (sink)
        {
            // Determine if this player supports content profile requested
            if (CMSProtocolInfo.getInstance().isSinkProtocolSupported(contentItemProtocolInfo))
            {
                throw new HNStreamingException("This Player is unable to render requested content, protocol Info: " +
                                                contentItemProtocolInfo.getAsString());                
            }
        }
        else
        {
            // Determine if this server supports content profile requested
        }
        
        ProtocolInfo localProtocolInfo = new ProtocolInfo(ProtocolInfo.getSinkProtocolInfoStr());

        if (!localProtocolInfo.isSupported(protocol, contentType))
        {
        }

        if (!contentItemProtocolInfo.isSupported(protocol, contentType))
        {
            throw new HNStreamingException("Unable to verify content item protocol is compatible with requested protocol: " + protocol + " or content type: "
                    + contentType);
        }
        */
    }
    
    /**
     * 
     * @param contentItemProtocolInfo
     * @param localConnectionId
     * @param peerConnectionId
     * @throws HNStreamingException
     */
    public void registerLocalConnection(HNStreamProtocolInfo contentItemProtocolInfo, 
                                        int localConnectionId, int peerConnectionId)
    throws HNStreamingException
    {
        if (log.isDebugEnabled())
        {
            log.debug("registerLocalConnection() - local connection id: " + localConnectionId + 
                    ", peer connection id: " + peerConnectionId + ", protocol info: " + contentItemProtocolInfo);
        }

        // Set in connection Info
        ConnectionInfo cInfo = new ConnectionInfo(localConnectionId,
                -1,                              // No rcsID
                -1,                              // No AvtId
                contentItemProtocolInfo,    // Server protocolInfo
                "",                              // No peerCM info
                peerConnectionId);   // Server connection ID
        
        addConnectionInfo(cInfo);
    }

    /**
     * Adds listener to be notified when connection completes
     * 
     * @param ccl   listener to add
     */
    public void addConnectionCompleteListener(ConnectionCompleteListener ccl)
    {
        this.connectionCompleteListeners.add(ccl);
    }

    /**
     * Removes this listener from list to be notified when connections complete.
     * 
     * @param ccl   listener to remove
     */
    public void removeConnectionCompleteListener(ConnectionCompleteListener ccl)
    {
        this.connectionCompleteListeners.remove(ccl);
    }
    
    /**
     * Notifies the listener that a control point has requested the
     * value of a state variable through the {@code QueryStateVariable} action.
     * The handler must return the current value of the
     * requested state variable.
     *
     * @param variable The UPnP state variable that was queried.
     *
     * @return The current value of the state variable.
     */
    public String getValue(UPnPManagedStateVariable variable)
    {
        if (variable.getName().equalsIgnoreCase(SINK_PROTOCOL_INFO))
        {
            return CMSProtocolInfo.getInstance().getSinkProtocolStateVariableValue();
        }
        else if (variable.getName().equalsIgnoreCase(SOURCE_PROTOCOL_INFO))
        {
            return CMSProtocolInfo.getInstance().getSourceProtocolStateVariableValue();
        }
        else if (variable.getName().equalsIgnoreCase(CURRENT_CONNECTION_IDS))
        {
            return getCurrentConnectionIDsStr();
        }
        else
        {
            return "";
        }
    }

    /**
     * Notifies the listener that a control point has subscribed to 
     * state variable eventing on the specified service.
     * This method is called subsequent to the transmission of
     * subscription response message,
     * but prior to the transmission of the initial event message.
     * The eventing process blocks until this method returns,
     * permitting the handler to set the initial values of the service's
     * state variables as desired.
     *
     * @param service The UPnP service that was subscribed to.
     */
    public void notifySubscribed(UPnPManagedService service)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifySubscribed() - called");
        }
    }

    /**
     * Notifies the listener that a control point has successfully
     * unsubscribed from state variable eventing on the specified service,
     * or that a prior subscription has expired.
     * This method is called subsequent to the transmission of the
     * unsubscription response message.
     *
     * @param service The UPnP service that was unsubscribed from.
     *  
     * @param remainingSubs The number of remaining active 
     *                      subscriptions to this service.
     */
    public void notifyUnsubscribed(UPnPManagedService service,
                                    int remainingSubs)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyUnsubscribed() - called");
        }
    }

    /**
     * Perform the necessary logic to support this action invocation for this 
     * Connection Manager Service.
     * 
     * @param   action  requested action to be performed
     * 
     * @return  response based on action invocation
     */
    public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
    {
        String actionName = action.getAction().getName();

        // Support the actions called out to be required in UPnP CDS Spec
        if (actionName.equals(GET_PROTOCOL_INFO))
        {
            return performGetProtocolInfoAction(action);
        }
        else if (actionName.equals(CONNECTION_COMPLETE))
        {
            return performConnectionCompleteAction(action);
        }
        else if (actionName.equals(GET_CURRENT_CONNECTION_IDS))
        {
            return performGetCurrentConnectionIDsAction(action);
        }
        else if (actionName.equals(GET_CURRENT_CONNECTION_INFO))
        {
            return performGetCurrentConnectionInfoAction(action);
        }
        // Optional actions which are not supported by RI
        else if (actionName.equals(PREPARE_FOR_CONNECTION))
        {
            // *TODO* - OCORI-3240 This code does not get exercised with Cybergarage UPnP stack because
            // of the listener to action to service relationship within Cybergarage
            return new UPnPErrorResponse(ActionStatus.UPNP_UNSUPPORTED_ACTION.getCode(),
                    ActionStatus.UPNP_UNSUPPORTED_ACTION.getDescription(), action);
        }
        else
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ACTION.getCode(),
                    ActionStatus.UPNP_INVALID_ACTION.getDescription(), action);
        }        
    }

    /**
     * Method called when RI ConnectionManagerService's UPnPActionHandler has been replaced.
     * 
     * @param   replacement new handler
     */
    public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
    {
        if (log.isWarnEnabled())
        {
            log.warn("notifyActionHandlerReplaced() - RI's Connection Manager has been replaced by: " +
            replacement.getClass().getName());
        }
    }

    /**
     * Respond to connection complete UPnP Action by closing down connection.
     * 
     * @param action    action to be performed
     * 
     * @return  response based on performing action
     */
    private UPnPResponse performConnectionCompleteAction(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyConnectionComplete() - called");
        }

        // Get ID argument from UPnP action
        String idStr = action.getArgumentValue(CONNECTION_ID);       
        int id = 0;
        try
        {
            id = Integer.valueOf(idStr).intValue();                
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("notifyConnectionComplete() - called with invalid id = " + 
                idStr,e);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
        
        if (!isValidConnectionID(id))
        {
            if (log.isWarnEnabled())
            {
                log.warn("notifyConnectionComplete - called with invalid id = " + id);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_CONNECTION.getCode(),
                    ActionStatus.UPNP_INVALID_CONNECTION.getDescription(), action);
        }
        
        final String[] requestStrings = ((UPnPActionImpl)action.getAction()).getRequestStrings();
        final NetworkInterface netInt = ((UPnPActionImpl)action.getAction()).getNetworkInterface();
        
        // Checks securityManager to verify if action is authorized
        if (!securityManager.notifyAction(NetSecurityManagerImpl.CM_CONN_COMPLETE, 
                ((UPnPActionImpl)action.getAction()).getInetAddress(), "", id, requestStrings, netInt))
        {
            if (log.isWarnEnabled())
            {
                log.warn("performConnectionCompleteAction() - Action is not authorized: " + 
                action.getAction().getName());
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
        }

        if (log.isDebugEnabled())
        {
            log.debug("notifyConnectionComplete() - called with id = " + id);
        }
        
        // Notify listeners
        for (int ii = 0; ii < this.connectionCompleteListeners.size(); ii++)
        {
            ConnectionCompleteListener ccl = (ConnectionCompleteListener) this.connectionCompleteListeners.elementAt(ii);
            if (log.isDebugEnabled())
            {
                log.debug("notifyConnectionComplete() - " + "calling listener " + ii + ", " + ccl);
            }
            ccl.notifyComplete(id);
        }
        
        // Get rid of this connection
        removeConnectionInfo(id);
        
        // Remove action from NetSecurityTable
        securityManager.notifyActionEnd(((UPnPActionImpl)action.getAction()).getInetAddress(), 
                NetSecurityManagerImpl.CM_CONN_COMPLETE);

        // Return response to managed service which will set status code
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
     * Respond to get protocol info UPnP Action by retrieving associated information.
     * 
     * @param action    action to be performed
     * 
     * @return  response based on performing action
     */
    private UPnPResponse performGetProtocolInfoAction(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performGetProtocolInfoAction() - called");
        }
        
        final String[] requestStrings = ((UPnPActionImpl)action.getAction()).getRequestStrings();
        NetworkInterface netInt = ((UPnPActionImpl)action.getAction()).getNetworkInterface();

        if (!securityManager.notifyAction(NetSecurityManagerImpl.CM_GET_PROTO_INFO, 
                ((UPnPActionImpl)action.getAction()).getInetAddress(), "", -1, requestStrings, netInt))
        {
            if (log.isWarnEnabled())
            {
                log.warn("performGetProtocolInfoAction() - Action is not authorized: " + 
                action.getAction().getName());
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
        }

        // Get the source protocol supported by this Connection Manager
        final String sourceStrTemp = CMSProtocolInfo.getInstance().getSourceProtocolStateVariableValue();
        String sourceStr = MediaServer.substitute(sourceStrTemp, MediaServer.HOST_PLACEHOLDER,
                ((UPnPActionImpl) action.getAction()).getLocalAddress());

        // Get the sink protocol info supported by this Connection Manager
        final String sinkStr = CMSProtocolInfo.getInstance().getSinkProtocolStateVariableValue();
        
        if (log.isDebugEnabled())
        {
            log.debug("performGetProtocolInfoAction() - source: " + sourceStr + ", sink: " + sinkStr);
        }

        // Remove action from NetSecurityTable
        securityManager.notifyActionEnd(((UPnPActionImpl)action.getAction()).getInetAddress(), 
                NetSecurityManagerImpl.CM_GET_PROTO_INFO);

        // Return response to managed service which will set status code
        try
        {
            return new UPnPActionResponse(new String[] { sourceStr, sinkStr }, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }

    /**
     * UPnP Action that returns the current list of connection IDs
     *
     * @param action
     *            The requesting Action.
     *
     * @return boolean - True indicates the connection IDs were returned.
     */
    private UPnPResponse performGetCurrentConnectionIDsAction(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performGetCurrentConnectionIDsAction() - called");
        }

        if(action.getArgumentNames() != null && action.getArgumentNames().length > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved arguments, none expected.");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);            
        } 
        
        // Checks securityManager to verify if action is authorized
        final String[] requestStrings = ((UPnPActionImpl)action.getAction()).getRequestStrings();
        NetworkInterface netInt = ((UPnPActionImpl)action.getAction()).getNetworkInterface();        
        
        if (!securityManager.notifyAction(NetSecurityManagerImpl.CM_GET_CONN_ID, 
                ((UPnPActionImpl)action.getAction()).getInetAddress(), "", -1, requestStrings, netInt))
        {
            if (log.isWarnEnabled())
            {
                log.warn("performGetCurrentConnectionIDsAction() - Action is not authorized: " + 
                action.getAction().getName());
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
        }

        String idsStr = getCurrentConnectionIDsStr();
        
        if (log.isDebugEnabled())
        {
            log.debug("performGetCurrentConnectionIDsAction() - connection ids: " + idsStr);
        }
        
        // Remove action from NetSecurityTable
        securityManager.notifyActionEnd(((UPnPActionImpl)action.getAction()).getInetAddress(), 
                NetSecurityManagerImpl.CM_GET_CONN_ID);

        // Return response to managed service which will set status code
        try
        {
            return new UPnPActionResponse(new String[]{idsStr}, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }
    
    /**
     * UPnP Action returning Connection Information managed by this Connection
     * Manager Service.
     *
     * @param action
     *            The requesting UPnP Action.
     *
     * @return boolean True indicates that the information was returned.
     */
    private UPnPResponse performGetCurrentConnectionInfoAction(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performGetCurrentConnectionInfoAction() - called");
        }
        
        // Get ID argument from UPnP action
        String idStr = action.getArgumentValue(CONNECTION_ID);       
        int id = 0;
        try
        {
            id = Integer.valueOf(idStr).intValue();                
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("performGetCurrentConnectionInfoAction() - called with invalid id = " + 
                idStr,e);
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("performGetCurrentConnectionInfoAction() - called with id = " + id);
        }
        
        final String[] requestStrings = ((UPnPActionImpl)action.getAction()).getRequestStrings();
        NetworkInterface netInt = ((UPnPActionImpl)action.getAction()).getNetworkInterface();        
        
        if (!securityManager.notifyAction(NetSecurityManagerImpl.CM_GET_CONN_INFO, 
                ((UPnPActionImpl)action.getAction()).getInetAddress(), "", id, requestStrings, netInt))
        {
            if (log.isWarnEnabled())
            {
                log.warn("performGetCurrentConnectionInfoAction() - Action is not authorized: " + 
                action.getAction().getName());
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
        }

        ConnectionInfo info = null;
        synchronized (mutex)
        {
            // Allow id == 0 to be valid since PrepareForConnection not supported
            // getConnectionInfo(0) not returns minimal info as per CM spec
            if ((!isValidConnectionID(id) && (id != 0))
                || ((info = getConnectionInfo(id)) == null))
            {
                if (log.isWarnEnabled())
                {
                    log.warn("performGetCurrentConnectionInfoAction() - called with invalid id = " + id);
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_CONNECTION.getCode(),
                        ActionStatus.UPNP_INVALID_CONNECTION.getDescription(), action);
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("performGetCurrentConnectionInfoAction() - info: " + info + ", id: " + id);
        }

        // Remove action from NetSecurityTable
        securityManager.notifyActionEnd(((UPnPActionImpl)action.getAction()).getInetAddress(), 
                NetSecurityManagerImpl.CM_GET_CONN_INFO);

        // Return response to managed service which will set status code
        try
        {
            String substitutedProtocolInfoString = MediaServer.substitute(info.getProtocolInfo(), MediaServer.HOST_PLACEHOLDER, ((UPnPActionImpl) action.getAction()).getLocalAddress());
            return new UPnPActionResponse(new String[]{
                info.getRcsIDStr(),
                info.getAVTransportIDStr(),
                substitutedProtocolInfoString,
                info.getPeerConnectionManager(),
                info.getPeerConnectionIDStr(),
                info.getDirection(),
                info.getStatus()}, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);                        
        }
    }

     /**
     * Utility method which formulates the string which represents the 
     * current connection IDs.
     * 
     * @return  string containing current connection IDs
     */
    private String getCurrentConnectionIDsStr()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getCurrentConnectionIDsStr() - called");
        }

        // Need to support conID of 0 as initial value instead of empty string
        // since DLNA BCM is supported we'll also return addition non zero values which
        // overrides the UPnP CM requirement. 
        StringBuffer conIDs = new StringBuffer("0");
        synchronized(mutex)
        {
            int size = conInfoList.size();

            for (int n = 0; n < size; n++)
            {
                ConnectionInfo info = (ConnectionInfo) conInfoList.get(n);

                if (0 <= n)
                {
                    conIDs.append(",");
                }

                conIDs.append(info.getID());
            }
        }

        return conIDs.toString();
    }

    /**
     * Determines if the supplied id is a valid connection id associated
     * with this connection manager.
     * 
     * @param conID check validity of this connection ID
     * @return  true if the supplied connection id is valid, false otherwise
     */
    private boolean isValidConnectionID(int conID)
    {
        boolean isValid = false;


        synchronized (mutex)
        {
            int size = conInfoList.size();

            for (int n = 0; n < size; n++)
            {
                ConnectionInfo info = (ConnectionInfo) conInfoList.get(n);
                if (conID == info.getID())
                {
                    isValid = true;
                    break;
                }
            }
        }

        return isValid;
    }

    /**
     * Initializes this service's state variables.
     */
    public final void initializeStateVariables()
    {
        for(Iterator i = m_services.iterator(); i.hasNext();)
        {
            UPnPManagedServiceImpl service = (UPnPManagedServiceImpl)i.next();
            
            UPnPManagedStateVariable sv = service.getManagedStateVariable(SINK_PROTOCOL_INFO);
            if(sv != null)
            {
                sv.setValue("");
            }

            sv = service.getManagedStateVariable(SOURCE_PROTOCOL_INFO);
            if(sv != null)
            {
                sv.setValue("");
            }

            sv = service.getManagedStateVariable(CURRENT_CONNECTION_IDS);
            if(sv != null)
            {
                sv.setValue("0");
            }
        }
    }
}
