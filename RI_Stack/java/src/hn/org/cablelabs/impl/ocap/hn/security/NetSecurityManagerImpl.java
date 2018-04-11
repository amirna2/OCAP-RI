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

package org.cablelabs.impl.ocap.hn.security;

import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.streaming.session.HNClientSession;
import org.cablelabs.impl.media.streaming.session.HNServerSessionManager;
import org.cablelabs.impl.ocap.hn.recording.RecordingActions;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.cm.ConnectionManagerService;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.security.NetAuthorizationHandler;
import org.ocap.hn.security.NetAuthorizationHandler2;
import org.ocap.hn.security.NetSecurityManager;
import org.ocap.system.MonitorAppPermission;

/**
 * Implementation class of NetSecurityManager
 */
public class NetSecurityManagerImpl extends NetSecurityManager
{
    private static final Logger log = Logger.getLogger(NetSecurityManagerImpl.class);

    private static final int MIN_VALID_PASSWORD_LENGTH = 12;

    private static final int MAX_VALID_PASSWORD_LENGTH = 17;

    /** Local Handle to the implemented NetSecurityManager */
    private static NetSecurityManagerImpl netSecurityManager = null;

    /** local handle to the NetAuthorizationHandler */
    private Object netAuthorizationHandler = null;

    private CallerContext nahCallerContext;

    private String[] nahAuthorizedActions;

    /** password list used to keep track of passwords and NetworkInterfaces */
    private NetPasswordList netPasswordList = null;

    // a map of client sessions to activity ids
    private Map clientSessions = new HashMap();

    // a map of client session activity ids to ClientSessionHolders
    private final Map clientSessionHolders = new HashMap();

    // table of authorized upnp actions
    private AuthorizedActionTable authorizedActionTable = new AuthorizedActionTable();

    public static final String ALL_ACTIONS = "*:*";

    public static final String CDS_ALL =        "CDS:*";
    public static final String CDS_BROWSE =     "CDS:" + ContentDirectoryService.BROWSE;
    public static final String CDS_SEARCH =     "CDS:" + ContentDirectoryService.SEARCH;
    public static final String CDS_SEARCH_CAP = "CDS:" + ContentDirectoryService.GET_SEARCH_CAPABILITIES;
    public static final String CDS_SORT_CAP =   "CDS:" + ContentDirectoryService.GET_SORT_CAPABILITIES;
    public static final String CDS_DESTROY =    "CDS:" + ContentDirectoryService.DESTROY_OBJECT;
    public static final String CDS_UPDATE =     "CDS:" + ContentDirectoryService.UPDATE_OBJECT;

    public static final String CM_ALL = "CM:*";
    public static final String CM_GET_CONN_ID =     "CM:" + ConnectionManagerService.GET_CURRENT_CONNECTION_IDS;
    public static final String CM_GET_CONN_INFO =   "CM:" + ConnectionManagerService.GET_CURRENT_CONNECTION_INFO;
    public static final String CM_GET_PROTO_INFO =  "CM:" + ConnectionManagerService.GET_PROTOCOL_INFO;
    public static final String CM_CONN_COMPLETE =   "CM:" + ConnectionManagerService.CONNECTION_COMPLETE;

    public static final String SRS_ALL =                "SRS:" + "*";
    public static final String SRS_BROWSE_REC_SCHEDS =  "SRS:" + RecordingActions.BROWSE_RECORD_SCHEDULES_ACTION_NAME;
    public static final String SRS_BROWSE_REC_TASKS =   "SRS:" + RecordingActions.BROWSE_RECORD_TASKS_ACTION_NAME;
    public static final String SRS_CREATE_REC_SCHED =   "SRS:" + RecordingActions.CREATE_RECORD_SCHEDULE_ACTION_NAME;
    public static final String SRS_DELETE_REC_SCHED =   "SRS:" + RecordingActions.DELETE_RECORD_SCHEDULE_ACTION_NAME;
    public static final String SRS_DELETE_REC_TASK =    "SRS:" + RecordingActions.DELETE_RECORD_TASK_ACTION_NAME;
    public static final String SRS_DISABLE_REC_SCHED =  "SRS:" + RecordingActions.DISABLE_RECORD_SCHEDULE_ACTION_NAME;
    public static final String SRS_DISABLE_REC_TASK =   "SRS:" + RecordingActions.DISABLE_RECORD_TASK_ACTION_NAME;
    public static final String SRS_ENABLE_REC_SCHED =   "SRS:" + RecordingActions.ENABLE_RECORD_SCHEDULE_ACTION_NAME;
    public static final String SRS_GET_ALLOWED_VALUES = "SRS:" + RecordingActions.GET_ALLOWED_VALUES_ACTION_NAME;
    public static final String SRS_GET_PROPERTY_LIST =  "SRS:" + RecordingActions.GET_PROPERTY_LIST_ACTION_NAME;
    public static final String SRS_GET_REC_SCHED =      "SRS:" + RecordingActions.GET_RECORD_SCHEDULE_ACTION_NAME;
    public static final String SRS_GET_REC_TASK =       "SRS:" + RecordingActions.GET_RECORD_TASK_ACTION_NAME;
    public static final String SRS_GET_REC_TASK_CONFLICTS = "SRS:" + RecordingActions.GET_RECORD_TASK_CONFLICTS_ACTION_NAME;
    public static final String SRS_GET_SORT_CAPS =      "SRS:" + RecordingActions.GET_SORT_CAPABILITIES_ACTION_NAME;
    public static final String SRS_GET_STATE_UPDATE_ID ="SRS:" + RecordingActions.GET_STATE_UPDATE_ID_ACTION_NAME;
    public static final String SRS_X_PRIORITIZE_REC =   "SRS:" + RecordingActions.X_PRIORITIZE_RECORDINGS_ACTION_NAME;

    private static final String[] VALID_ACTION_ARRAY = { ALL_ACTIONS,
    CDS_ALL, CDS_BROWSE, CDS_SEARCH, CDS_SEARCH_CAP, CDS_DESTROY, CDS_UPDATE,
    SRS_ALL, SRS_BROWSE_REC_SCHEDS, SRS_BROWSE_REC_TASKS, SRS_CREATE_REC_SCHED, SRS_DELETE_REC_SCHED,
             SRS_DELETE_REC_TASK, SRS_DISABLE_REC_SCHED, SRS_DISABLE_REC_TASK, SRS_ENABLE_REC_SCHED,
             SRS_GET_ALLOWED_VALUES, SRS_GET_PROPERTY_LIST, SRS_GET_REC_SCHED, SRS_GET_REC_TASK,
             SRS_GET_REC_TASK_CONFLICTS, SRS_GET_SORT_CAPS, SRS_GET_STATE_UPDATE_ID, SRS_X_PRIORITIZE_REC,
    CM_ALL,  CM_GET_CONN_ID, CM_GET_CONN_INFO, CM_GET_PROTO_INFO, CM_CONN_COMPLETE };

    /**
     * Protected constructor.
     */
    private NetSecurityManagerImpl()
    {
        netPasswordList = new NetPasswordList();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized static NetSecurityManager getInstance()
    {
        if (netSecurityManager == null)
        {
            netSecurityManager = new NetSecurityManagerImpl();
        }

        return netSecurityManager;
    }

    /**
     * {@inheritDoc}
     */
    public String getNetworkPassword(org.ocap.hn.NetworkInterface networkInterface)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        NetPassword np;
        String password;

        np = netPasswordList.getNetPassword(networkInterface);

        if (np != null)
        {
            password = np.getPassword();
        }
        else
        {
            throw new UnsupportedOperationException();
        }

        return password;
    }

    /**
     * {@inheritDoc}
     */
    public void setNetworkPassword(org.ocap.hn.NetworkInterface networkInterface, String password)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        if ((networkInterface != null) && (password != null) && isValidPassword(password))
        {
            netPasswordList.addNetPassword(new NetPassword(networkInterface, password));
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthorizationHandler(NetAuthorizationHandler nah)
    {
        setAuthorizationHandler(nah, new String[0], true);
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthorizationHandler(NetAuthorizationHandler nah, String[] actionNames,
            boolean notifyTransportRequests)
    {
        setAuthHandler(nah, actionNames, notifyTransportRequests);
    }

    /**
     * {@inheritDoc}
     */
    public void setAuthorizationHandler(NetAuthorizationHandler2 nah2,
            String[] actionNames,
            boolean notifyTransportRequests)
    {
        setAuthHandler(nah2, actionNames, notifyTransportRequests);
    } // END setAuthorizationHandler(NetAuthorizationHandler2,...)

    /**
     * Helper method for common registration functionality.
     */
    private synchronized void setAuthHandler(Object nah, String[] actionNames, boolean notifyTransportRequests)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if (actionNames == null)
        {
            actionNames = new String[0];
        }
        if (!isValidActionList(actionNames))
        {
            throw new IllegalArgumentException();
        }

        if (netAuthorizationHandler != null)
        { // Remove the old NAH
            HNServerSessionManager.getInstance().netAuthorizationHandlerRemoved();
            unregisterCallbackData(nahCallerContext);
            nahCallerContext = null;
            authorizedActionTable.clear();
        }

        netAuthorizationHandler = nah;

        if (nah != null)
        { // Add the new NAH
            // Hold a ref to the context in which setauthorizationhandler was
            // called, will be used in notify methods.
            nahCallerContext = ccm.getCurrentContext();
            registerCallbackData(nahCallerContext);
            if (actionNames != null)
            {
                nahAuthorizedActions = actionNames;
            }
            else
            { // Guarantee that nahAuthorizedActions is not null when a nah is set
                nahAuthorizedActions = new String[0];
            }
            HNServerSessionManager.getInstance().netAuthorizationHandlerSet(!notifyTransportRequests);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void revokeAuthorization(int activityId)
    {
        if (log.isInfoEnabled())
        {
            log.info("revokeAuthorization: " + activityId);
        }
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // TODO: Provide a proper result code
        HNServerSessionManager.getInstance().revoke(activityId, HttpURLConnection.HTTP_OK);
    }

    /**
     * {@inheritDoc}
     */
    public boolean queryTransaction(String actionName, InetAddress inetAddress, String macAddress, URL url,
            int activityId)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // create a holder for the passed in parameters (in order to be able to
        // use equals for comparison)
        ClientSessionHolder tempHolder = new ClientSessionHolder(inetAddress, macAddress, url, activityId);

        if (isMalformedMACAddress(macAddress) || isMalformed(url))
        {
            if (log.isWarnEnabled())
            {
                log.warn("queryTransaction - malformed mac address or url: " + tempHolder);
            }
            return false;
        }
        else if ((!(actionName == null) && !(actionName.equals(""))) && (inetAddress != null))
        {
            if (authorizedActionTable.exists(inetAddress, actionName))
            {
                return true;
            }
        }

        for (Iterator iter = clientSessionHolders.values().iterator(); iter.hasNext();)
        {
            ClientSessionHolder holder = (ClientSessionHolder) iter.next();
            // equals implementation does not consider activity Id of -1 in
            // equality evaluation, leveraging equals to compare instances
            if (tempHolder.equals(holder))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("queryTransaction - found holder");
                }
                return true;
            }
        }

        if (activityId != -1)
        {
            return HNServerSessionManager.getInstance().isSessionActive(activityId);
        }

        if (log.isDebugEnabled())
        {
            log.debug("queryTransaction - unable to find cached authorization for activityID "
                      + activityId);
        }
        return false;
    }

    /**
     * Checks valid password length
     *
     * @param pw
     *            The password to be verified
     *
     * @return boolean true indicates that the password is within the valid
     *         range. False indicates the password is too long or too short.
     */
    private boolean isValidPassword(String pw)
    {
        return ((pw.length() >= MIN_VALID_PASSWORD_LENGTH) && (pw.length() <= MAX_VALID_PASSWORD_LENGTH));
    }

    /**
     * Validates the list of upnpActions defined by the OSH.
     *
     * @param actions
     *            The action list to be verified.
     *
     * @return boolean true if the list is valid. False indicates that an action
     *         is unsupported.
     */
    private boolean isValidActionList(String[] actions)
    {
        boolean isValid = true; // 0-length action list is valid
        boolean actionFound = false;

        for (int i = 0; i < actions.length; ++i)
        {
            for (int j = 0; j < VALID_ACTION_ARRAY.length; ++j)
            {
                if (actions[i].equalsIgnoreCase(VALID_ACTION_ARRAY[j]))
                {
                    actionFound = true;
                    break;
                }
            }

            if (actionFound)
            { // OSH specified action is valid
                actionFound = false;
                isValid = true;

                continue;
            }
            else
            { // OSH specified action is unsupported.
                isValid = false;
                break;
            }
        }

        return isValid;
    }

    /**
     * Checks the action against the OSH actionList.
     *
     * @param action
     *            to be verified for authorization.
     *
     * @return boolean true indicates that the action requires authorization.
     *         False indicates that the action doesn't require authorization.
     */
    private boolean isAuthorizedAction(final String action)
    {
        boolean isAuthorized = false;

        String[] serviceId = Utils.split(action, ":");
        for (int i = 0; i < nahAuthorizedActions.length; ++i)
        {
            if (nahAuthorizedActions[i].equalsIgnoreCase(ALL_ACTIONS)
                    || nahAuthorizedActions[i].equalsIgnoreCase(serviceId[0] + ":*")
                    || action.equalsIgnoreCase(nahAuthorizedActions[i]))
            {
                isAuthorized = true;
                break;
            }
        }

        return isAuthorized;
    }

    /**
     * Validates a malformed URL
     *
     * @param url
     *            The url to be validated. Null url is not malformed.
     *
     * @return boolean true if the url is malformed. False indicates that the
     *         url is properly formated
     */
    private boolean isMalformed(URL url)
    {
        boolean malformed = false;

        if (url != null)
        {
            malformed = ((!url.getProtocol().equalsIgnoreCase("http"))
                    || (!url.getProtocol().equalsIgnoreCase("https")) || (!url.getProtocol().equalsIgnoreCase("rtsp")) || (url.getPath().equalsIgnoreCase("")));
        }

        return malformed;
    }

    /**
     * Validates a malformed MAC Address
     *
     * @param macAddress
     *            The MAC Address to be validated. Null MAC address is not
     *            malformed.
     *
     * @return boolean true if the MAC Address is malformed. False indicates
     *         that the MAC Address is properly formatted
     */
    private boolean isMalformedMACAddress(String macAddress)
    {
        boolean malformed = false;

        // empty strings("") are allowed.
        if (macAddress != null && !macAddress.equalsIgnoreCase(""))
        {
            for (int i = 0; i < macAddress.length(); ++i)
            {
                if (isFieldSeperator(i, macAddress.charAt(i)) || isHex(macAddress.charAt(i)))
                {
                    continue;
                }
                else
                {
                    malformed = true;
                    break;
                }
            }
        }

        return malformed;
    }

    private boolean isHex(char c)
    {
        int base16 = 16; // Hex
        return (Character.digit(c, base16) != -1);
    }

    private boolean isFieldSeperator(int i, char c)
    {
        // integers 2, 5, 8, 11 & 14 represent offsets within a MAC address
        // string containing a ':'.
        return ((c == ':') && ((i == 2) || (i == 5) || (i == 8) || (i == 11) || (i == 14)));
    }

    /**
     * Notifies the authorization handler application that an action it
     * registered interest in has been received.
     *
     * @param actionName
     *            Name of the action received. Will match the name passed to the
     *            <code>NetSecurityManager.setAuthorizationApplication</code>
     *            method.
     * @param inetAddress
     *            IP address the transaction was sent from.
     * @param macAddress
     *            MAC address the transaction was sent from if present at any
     *            layer of the received communications protocol. Can be empty
     *            <code>String</code> if not present. The format is EUI-48 with
     *            6 colon separated 2 digit bytes in hexadecimal notation with
     *            no leading "0x", e.g. "00:11:22:AA:BB:CC".
     * @param activityID
     *            The unique identifier of the activity if known. If no
     *            activityId is association with the transaction the
     *            implementation SHALL pass a value of -1;
     * @param requestHeaderLines
     *            the HTTP request line. For an HTTP-induced action, this should be a POST.
     *            A null or 0-length array can be passed.
     * @param netInterface
     *            The NetworkInterface the action was received on.
     * @return True if the activity is accepted, otherwise returns false.
     */
    public synchronized boolean notifyAction( final String actionName,
                                 final InetAddress inetAddress,
                                 final String macAddress,
                                 final int activityID,
                                 final String[] requestHeaderLines,
                                 final NetworkInterface netInterface )
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyAction - name: " + actionName + ", address: " + inetAddress + ", activityID: "
            + activityID + ", actions: " + nahAuthorizedActions + ", netAuthHandler: " + netAuthorizationHandler );
        }

        if (netAuthorizationHandler == null)
        {
            return true;
        }

        if (!isAuthorizedAction(actionName))
        { // Action is not in the list requested by the NAH
            if (log.isDebugEnabled())
            {
                log.debug("notifyActivityStart: Action " + actionName
                          + " was not registered for interest - action is authorized" );
            }
            return true;
        }

        if (authorizedActionTable.exists(inetAddress, actionName))
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyActivityStart: Action " + actionName
                          + " was not registered for interest - action is authorized" );
            }
            return true;
        }

        final boolean result[] = new boolean[1];
        try
        {
            if (netAuthorizationHandler instanceof NetAuthorizationHandler)
            {
                final NetAuthorizationHandler nah = (NetAuthorizationHandler)netAuthorizationHandler;
                nahCallerContext.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("notifyActivityStart: Invoking NAH " + nah );
                        }
                        result[0] = nah.notifyAction(actionName, inetAddress, macAddress, activityID);
                    }
                });
            }
            else if (netAuthorizationHandler instanceof NetAuthorizationHandler2)
            {
                final NetAuthorizationHandler2 nah2 = (NetAuthorizationHandler2)netAuthorizationHandler;
                nahCallerContext.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("notifyActivityStart: Invoking NAH2 " + nah2 );
                        }
                        result[0] = nah2.notifyAction( actionName, inetAddress, activityID,
                                                       requestHeaderLines, netInterface);
                    }
                });
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unexpected/unknown NAH type: " + netAuthorizationHandler.getClass());
                }

                // Let's consider it authorized if this happens...
                result[0] = true;
            }
        }
        catch (InvocationTargetException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("exception calling notifyAction", e);
            }
            result[0] = true;
        }

        if (result[0])
        {
            if (log.isDebugEnabled())
            {
                log.debug("adding action to authorized action table - name: " + actionName
                + ", address: " + inetAddress + ", activity id: " + activityID);
            }
            authorizedActionTable.addAuthorizedAction(new AuthorizedAction(actionName, inetAddress,
                    macAddress, activityID));
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("not authorized - not adding action to authorized action table - name: "
                + actionName + ", address: " + inetAddress + ", activity id: " + activityID);
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("notifyAction returning: " + result[0]);
        }
        return result[0];
    }

    /**
     * Remove authorized action activity.
     */
    public synchronized void notifyActionEnd(final InetAddress inetAddress, final String actionName)
    {
        if (netAuthorizationHandler == null)
        {
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug("notifyActionEnd - action: " + actionName + ", address: " + inetAddress);
        }

        authorizedActionTable.removeAuthorizedAction(inetAddress, actionName);
        if (log.isDebugEnabled())
        {
            log.debug("removed action from authorized action table - action: " + actionName + ", address: "
                      + inetAddress);
        }
    }

    /**
     * Implementation method allowing a server session to be authorized. Called
     * on the server side to ensure a session is authorized prior to
     * transmission.
     *
     * @param inetAddress
     *            the inetAddress
     * @param macAddress
     *            the mac address
     * @param url
     *            the url
     * @param activityID
     *            the activity ID
     * @param requestHeaderLines
     *            the HTTP request line and all headers, one string per header
     *            A null or 0-length array can be passed.
     * @return true if NetAuthorization authorized the session or if no
     *         NetAuthorizationHandler was registered
     */
    public synchronized boolean notifyActivityStart( final InetAddress inetAddress,
                                        final String macAddress,
                                        final URL url,
                                        final int activityID,
                                        final ContentEntry contentEntry,
                                        final String[] requestHeaderLines,
                                        final NetworkInterface netInterface )
    {
        if (netAuthorizationHandler == null)
        {
            return true;
        }

        if (log.isDebugEnabled())
        {
            log.debug("notifyActivityStart: Authorizing inetAddr " + inetAddress
                      + ",url " + url + ",activityID " + activityID
                      + ",contentEntry " + contentEntry
                      + ",requestHeaderSize " + requestHeaderLines.length
                      + ",ni " + netInterface );
        }

        final boolean[] result = new boolean[1];

        try
        {
            if (netAuthorizationHandler instanceof NetAuthorizationHandler)
            {
                final NetAuthorizationHandler nah = (NetAuthorizationHandler)netAuthorizationHandler;
                nahCallerContext.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("notifyActivityStart: Invoking NAH " + nah );
                        }
                        result[0] = nah.notifyActivityStart( inetAddress, macAddress, url,
                                                             activityID );
                    }
                });
            }
            else if (netAuthorizationHandler instanceof NetAuthorizationHandler2)
            {
                final NetAuthorizationHandler2 nah2 = (NetAuthorizationHandler2)netAuthorizationHandler;
                nahCallerContext.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("notifyActivityStart: Invoking NAH2 " + nah2 );
                        }
                        result[0] = nah2.notifyActivityStart( inetAddress, url, activityID,
                                                              contentEntry, requestHeaderLines,
                                                              netInterface );
                    }
                });
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unexpected/unknown NAH type: " + netAuthorizationHandler.getClass());
                }

                // Let's consider it authorized if this happens...
                result[0] = true;
            }
        }
        catch (InvocationTargetException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("exception calling notifyactivitystart", e);
            }
            // Let's consider it authorized if this happens...
            result[0] = true;
        }

        if (log.isDebugEnabled())
        {
            log.debug("notifyActivityStart: NAH returned " + result[0] );
        }

        return result[0];
    }

    /**
     * Notify auth handler that the activity has ended.
     *
     * <p>Requirements from HNP 2.0 section 6.6.2.2
     *
     * <p>When an application has set a NetAuthorizationHandler2 object by calling the overloaded
     * NetSecurityManager.setAuthorizationHandler method that takes a NetAuthorizationHandler2
     * parameter, the OC-DMS HNIMP SHALL call the notifyActivityEnd method when the activity
     * terminates.  Identification of activity termination is defined in ...
     *
     * <p>The OC-DMS HNIMP SHALL match activity end occurrences with activity start occurrences and
     * pass the activityID parameter to the notifyActivityEnd method that was previously provided
     * in the corresponding call to the notifyActivityStart method.
     *
     * <p>The OC-DMS HNIMP SHALL set the resultCode parameter as follows:</p>
     *
     * <p>1. If the activity was terminated by a message from the OC-DMS HNIMP to a remote device
     * then set the resultCode to the response value returned in the message.  This will be one
     * of the HTTP, RTSP, or UPnP response codes.</p>
     *
     * <p>2. If the activity was terminated by a detected network failure or timeout, set the
     * resultCode to 408 Request Timeout.</p>
     *
     * @param activityId
     *            the activity id
     * @param resultCode
     *            the result code causing the activity to end
     */
    public synchronized void notifyActivityEnd(final int activityId, final int resultCode)
    {
        if (netAuthorizationHandler == null)
        {
            return;
        }

        if (netAuthorizationHandler instanceof NetAuthorizationHandler)
        {
            final NetAuthorizationHandler nah = (NetAuthorizationHandler)netAuthorizationHandler;
            nahCallerContext.runInContext(new Runnable()
            {
                public void run()
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifyActivityEnd(" + activityId + "): Invoking NAH " + nah );
                    }
                    nah.notifyActivityEnd(activityId);
                }
            });
        }
        else if (netAuthorizationHandler instanceof NetAuthorizationHandler2)
        {
            final NetAuthorizationHandler2 nah2 = (NetAuthorizationHandler2)netAuthorizationHandler;
            nahCallerContext.runInContext(new Runnable()
            {
                public void run()
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifyActivityEnd(" + activityId + "): Invoking NAH2 " + nah2 );
                    }
                    nah2.notifyActivityEnd(activityId, resultCode);
                }
            });
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unexpected/unknown NAH type: " + netAuthorizationHandler.getClass());
            }
        }
    }

    /**
     * Store client session information
     *
     * @param session
     *            the client session
     * @param address
     *            the IP address
     * @param macAddress
     *            the mac address
     * @param url
     *            the URL
     * @param connectionId
     *            the connection id
     */
    public void addClientSession(HNClientSession session, InetAddress address, String macAddress, URL url,
            int connectionId)
    {
        Integer connectionInt = new Integer(connectionId);
        // add to session to activity id map
        clientSessions.put(session, connectionInt);

        // add to activity
        clientSessionHolders.put(connectionInt, new ClientSessionHolder(address, macAddress, url, connectionId));
    }

    /**
     * Remove a client session
     *
     * @param session
     *            the client session to remove
     */
    public void removeClientSession(HNClientSession session)
    {
        Integer activityId = (Integer) clientSessions.remove(session);
        if (activityId != null)
        {
            clientSessionHolders.remove(activityId);
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("unable to find client session to remove: " + session);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void enableMocaPrivacy(org.ocap.hn.NetworkInterface networkInterface)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void disableMocaPrivacy(org.ocap.hn.NetworkInterface networkInterface)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Holder class containing fields that are passed into the client session
     * started notification
     */
    private static class ClientSessionHolder
    {
        private final InetAddress address;

        private final String macAddress;

        private final URL url;

        private final int activityId;

        private URI uri;

        public ClientSessionHolder(InetAddress address, String macAddress, URL url, int activityId)
        {
            this.address = address;
            this.macAddress = macAddress;
            this.url = url;
            this.activityId = activityId;
            try
            {
                this.uri = new URI(url.toString());
            }
            catch (URISyntaxException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct URI from URL - this should never happen!");
                }
                uri = null;
            }
        }

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            ClientSessionHolder that = (ClientSessionHolder) o;

            if (activityId != -1 && that.activityId != -1)
            {
                if (activityId != that.activityId)
                {
                    return false;
                }
            }
            if (address != null ? !address.equals(that.address) : that.address != null)
            {
                return false;
            }
            if (macAddress != null ? !macAddress.equals(that.macAddress) : that.macAddress != null)
            {
                return false;
            }
            if (uri != null ? !uri.equals(that.uri) : that.uri != null)
            {
                return false;
            }

            return true;
        }

        // session is not used in equals or hashcode calculations
        public int hashCode()
        {
            int result = 0;
            if (address != null)
            {
                result = result + address.hashCode();
            }
            if (macAddress != null)
            {
                result = 31 * result + macAddress.hashCode();
            }
            if (uri != null)
            {
                result = 31 * result + uri.hashCode();
            }
            if (activityId != -1)
            {
                result = 31 * result + activityId;
            }
            return result;
        }

        public String toString()
        {
            return "ClientSessionHolder - address: " + address + ", " + macAddress + ", " + url + ", " + activityId;
        }
    }

    /**
     * Per-context global data.
     * Clean up when CC destroyed
     */
    private class Data implements CallbackData
    {
        public void destroy(CallerContext cc)
        {
            synchronized (NetSecurityManagerImpl.this)
            {
                // Simply forget the given cc
                // No harm done if never added
                cc.removeCallbackData(NetSecurityManagerImpl.this);
                nahCallerContext = null;
                netAuthorizationHandler = null;
                authorizedActionTable.clear();
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }

    /**
     * Register for application state change notifications.
     *
     * @param ctx to register with
     */
    private void registerCallbackData(CallerContext ctx)
    {
        ctx.addCallbackData(new Data(), this);
    }
    
    /**
     * Unregister for application state change notifications.
     *
     * @param ctx to unregister with
     */
    private void unregisterCallbackData(CallerContext ctx)
    {
        ctx.removeCallbackData(this);
    }
}
