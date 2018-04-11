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

package org.cablelabs.impl.ocap.hn.upnp.bms;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.bms.NSLookupResult;
import org.cablelabs.impl.ocap.hn.upnp.bms.TestID;
import org.cablelabs.impl.ocap.hn.upnp.bms.TestManager;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedServiceImpl;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.xml.sax.SAXException;

/**
 * BasicManagementService- Provides a set of states and actions to provide 
 * diagnostic endpoint functions.
 *
 */
public class BasicManagementService implements UPnPActionHandler
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(BasicManagementService.class);

    /** Defines the type for the BasicManagementService-*/
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:BasicManagement:2";
    
    public static final String SERVICEID = "urn:upnp-org:serviceId:BasicManagement";

    /**
     * Handle to the NetSecurityManager. Used to authorize the action.
     */
    private static final NetSecurityManagerImpl securityManager = (NetSecurityManagerImpl) NetSecurityManagerImpl.getInstance();

    // Members
    private final TestManager m_testManager;
    private final List m_services = new ArrayList();

    // ACTION DEFINITIONS

    /** Get Device Status action definition */
    public static final String GET_DEVICE_STATUS = "GetDeviceStatus";

    /** Get Test IDs action definition */
    public static final String GET_TEST_IDS = "GetTestIDs";

    /** Get Active Test IDs action definition */
    public static final String GET_ACTIVE_TEST_IDS = "GetActiveTestIDs";

    /** Get Test Info action definition */
    public static final String GET_TEST_INFO = "GetTestInfo";

    /** Cancel Test action definition */
    public static final String CANCEL_TEST = "CancelTest";

    /** Ping action definition */
    public static final String PING = "Ping";

    /** Get Ping Results action definition */
    public static final String GET_PING_RESULT = "GetPingResult";

    /** NSLookup action definition */
    public static final String NSLOOKUP = "NSLookup";

    /** Get NSLookup Results action definition */
    public static final String GET_NSLOOKUP_RESULT = "GetNSLookupResult";

    /** Traceroute action definition */
    public static final String TRACEROUTE = "Traceroute";

    /** Get Traceroute Results action definition */
    public static final String GET_TRACEROUTE_RESULT = "GetTracerouteResult";
    
    /** List of additional BMS Optional actions which are not supported **/
    public static final String REBOOT = "Reboot";
    public static final String BASE_LINE_RESET= "BaselineReset";
    public static final String SET_SEQUENCE_MODE = "SetSequenceMode";
    public static final String GET_SEQUENCE_MODE = "GetSequenceMode";
    public static final String GET_BANDWIDTH_TEST_INFO = "GetBandwidthTestInfo";
    public static final String BANDWIDTH_TEST = "BandwidthTest";
    public static final String GET_BANDWIDTH_TEST_RESULT = "GetBandwidthTestResult";
    public static final String INTERFACE_RESET = "InterfaceReset";
    public static final String GET_INTERFACE_RESET_RESULT = "GetInterfaceResetResult";
    public static final String SELF_TEST = "SelfTest";
    public static final String GET_SELF_TEST_RESULT = "GetSelfTestResult";
    public static final String GET_LOG_URIS = "GetLogURIs";
    public static final String SET_LOG_INFO = "SetLogInfo";
    public static final String GET_LOG_INFO = "GetLogInfo";
    public static final String GET_ACL_DATA= "GetACLData";

    // STATE VARIABLE DEFINITIONS
    public static final String DEVICE_STATUS = "DeviceStatus";
    private final DeviceStatusEventer m_deviceStatusEventer;
    public static final String TESTIDS = "TestIDs";
    private final TestIDsEventer m_testIDsEventer;
    public static final String ACTIVETESTIDS = "ActiveTestIDs";
    private final ActiveTestIDsEventer m_activeTestIDsEventer;

    // DSCP range values
    private final String minDSCP = "0";
    private final String maxDSCP = "63";

    // Default values
    private final int DEFAULT_REPS = 4;
    private final int DEFAULT_TIMEOUT_MS = 5000; 
    private final int DEFAULT_BLOCKSIZE = 32;
    private final int DEFAULT_HOPS = 30;

    /**
     * Construct a <code>BasicManagementService</code>.
     */
    public BasicManagementService()
    {
        // Get TestManager instance
        m_testManager = TestManager.getInstance();

        m_deviceStatusEventer = new DeviceStatusEventer();
        m_testIDsEventer = new TestIDsEventer();
        m_activeTestIDsEventer = new ActiveTestIDsEventer();
    }
    
    public void registerService(UPnPManagedService service)
    {
        assert service != null;
        
        service.setActionHandler(this);
        m_deviceStatusEventer.registerVariable(
                ((UPnPManagedServiceImpl)service).getManagedStateVariable(DEVICE_STATUS));
        
        m_testIDsEventer.registerVariable(
                ((UPnPManagedServiceImpl)service).getManagedStateVariable(TESTIDS));
        
        m_activeTestIDsEventer.registerVariable(
                ((UPnPManagedServiceImpl)service).getManagedStateVariable(ACTIVETESTIDS));
        
        m_services.add(service);
    }
    
    public void initializeStateVariables()
    {
        long startTime = MediaServer.getInstance().getDeviceStartTime();
        String status = "OK," + Utils.formatDateISO8601(startTime);
        m_deviceStatusEventer.set(status);        
        m_activeTestIDsEventer.set(""); 
        m_testIDsEventer.set("");
    }

    public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
    {
        String actionName = action.getAction().getName();
        String[] args = action.getArgumentNames();
        String[] values = new String[args.length];
        
        for(int i = 0; i < args.length; i++)
        {
            values[i] = action.getArgumentValue(args[i]);
            if (log.isDebugEnabled())
            {
                log.debug(actionName + " param = " + args[i] + " = " + values[i]);
            }
        }
        
        UPnPResponse response = null;
        
        final InetAddress client = ((UPnPActionImpl)action.getAction()).getInetAddress();
        final String[] requestStrings = ((UPnPActionImpl)action.getAction()).getRequestStrings();
        final NetworkInterface netInt = ((UPnPActionImpl)action.getAction()).getNetworkInterface();

        // These are actions required to be implemented
        if (actionName.equals(GET_DEVICE_STATUS) ||
                actionName.equals(GET_TEST_IDS) ||   
                actionName.equals(GET_ACTIVE_TEST_IDS) ||
                actionName.equals(GET_TEST_INFO) ||
                actionName.equals(CANCEL_TEST) ||
                actionName.equals(PING) ||
                actionName.equals(GET_PING_RESULT) ||
                actionName.equals(NSLOOKUP) ||              
                actionName.equals(GET_NSLOOKUP_RESULT) ||              
                actionName.equals(TRACEROUTE) ||
                actionName.equals(GET_TRACEROUTE_RESULT))
        {
            // Begin NetSecurityManager authorization before processing any valid action        
            if (!securityManager.notifyAction(SERVICEID + actionName, client, "", -1, requestStrings, netInt))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("BasicManagementService.browseAction() - unauthorized");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                        ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
            }
        
            // Process actions called out to be required in DLNA DIAGE Spec
            if (actionName.equals(GET_DEVICE_STATUS))
            {
                response = getDeviceStatus(action, values);
            }
            else if (actionName.equals(GET_TEST_IDS))
            {
                response = getTestIDs(action, values);
            }
            else if (actionName.equals(GET_ACTIVE_TEST_IDS))
            {
                response = getActiveTestIDs(action, values);
            }
            else if (actionName.equals(GET_TEST_INFO))
            {
                response = getTestInfo(action, values);
            }
            else if (actionName.equals(CANCEL_TEST))
            {
                response = cancelTest(action, values);
            }
            else if (actionName.equals(PING))
            {         
                response = ping(action, values);
            }
            else if (actionName.equals(GET_PING_RESULT))
            {
                response = getPingResult(action, values);
            }
            else if (actionName.equals(NSLOOKUP))
            {
                response = nslookup(action, values);
            }
            else if (actionName.equals(GET_NSLOOKUP_RESULT))
            {
                response = getNSLookupResult(action, values);
            }
            else if (actionName.equals(TRACEROUTE))
            {
                response = traceroute(action, values);
            }
            else if (actionName.equals(GET_TRACEROUTE_RESULT))
            {
                response = getTracerouteResult(action, values);
            }
      
            // End NetSecurityManager
            securityManager.notifyActionEnd(client, SERVICEID + actionName);                       
        }
        // These are optional actions which are not supported by RI
        else if ((actionName.equals(REBOOT)) ||
                 (actionName.equals(BASE_LINE_RESET)) ||   
                 (actionName.equals(SET_SEQUENCE_MODE)) ||
                 (actionName.equals(GET_SEQUENCE_MODE)) ||
                 (actionName.equals(GET_BANDWIDTH_TEST_INFO)) ||
                 (actionName.equals(BANDWIDTH_TEST)) ||
                 (actionName.equals(GET_BANDWIDTH_TEST_RESULT)) ||
                 (actionName.equals(INTERFACE_RESET)) ||
                 (actionName.equals(GET_INTERFACE_RESET_RESULT)) ||
                 (actionName.equals(SELF_TEST)) ||
                 (actionName.equals(GET_SELF_TEST_RESULT)) ||
                 (actionName.equals(GET_LOG_URIS)) ||
                 (actionName.equals(SET_LOG_INFO)) ||
                 (actionName.equals(GET_LOG_INFO)) ||
                 (actionName.equals(GET_ACL_DATA)))
        {
            response = new UPnPErrorResponse(ActionStatus.UPNP_UNSUPPORTED_ACTION.getCode(), 
                    ActionStatus.UPNP_UNSUPPORTED_ACTION.getDescription(), action);
        }
       
        // Return response or Invalid Action if not set
        return response != null ? response : new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ACTION.getCode(),
                ActionStatus.UPNP_INVALID_ACTION.getDescription(), action);
    }

    public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
    {
        if (log.isWarnEnabled())
        {
            log.warn("Default Basic Management Service action handler being replaced");
        }
    }

    /**
     * Gets the Device Status
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse The DeviceStatus string 
     */
    private UPnPResponse getDeviceStatus(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values != null && values.length > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved arguments when none expected");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        // return OK and date for now
        long startTime = MediaServer.getInstance().getDeviceStartTime();
        String status = "OK," + Utils.formatDateISO8601(startTime);
        try
        {
            response = new UPnPActionResponse(new String[] { status }, action); 
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }

    /**
     * Gets list of test ids 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse csv list of test ids 
     */
    private UPnPResponse getTestIDs(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;
        
        // Verify the arguments are valid
        if (values != null && values.length > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved arguments when none expected");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        // Get sorted test ids csv 
        String responseStr = m_testManager.getTestIDsCSV();

        try
        {
            response = new UPnPActionResponse(new String[] { responseStr },
                action); 
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }

    /**
     * Gets list of active test ids
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse csv list of active test ids 
     */
    private UPnPResponse getActiveTestIDs(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values != null && values.length > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved arguments when none expected");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        // Get tests and sort
        HashMap tests = (HashMap) m_testManager.getTestIDs();
        Integer[] ids = (Integer[]) (tests.keySet().toArray( new Integer[tests.size()]));
        Arrays.sort(ids);

        // Build response only including REQUESTED and IN_PROGRESS tests 
        String responseStr = m_testManager.getActiveTestIDsCSV();

        try
        {
            response = new UPnPActionResponse(new String[] { responseStr },
                action); 
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }

    /**
     * Gets information about a test. 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse The information of a test 
     */
    private UPnPResponse getTestInfo(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
      
        if (!Utils.validateUDANumericValue("ui4", values[0], null, null))
        {
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        String idStr = values[0];
        int id = Integer.parseInt(idStr);
        TestID testID = m_testManager.getTestID(id);

        if (testID == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Test " + id + " not found"); 
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_TEST.getCode(),
                    ActionStatus.UPNP_NO_SUCH_TEST.getDescription(), action);
        }

        String[] resultsArray = {testID.getTestType(), testID.getTestState()};

        // create response
        try
        {
            response = new UPnPActionResponse( resultsArray, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }
        
    /**
     * Cancel a test 
     *
     * @param action
     *            The action
     *
     */
    private UPnPResponse cancelTest(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
      
        if (!Utils.validateUDANumericValue("ui4", values[0], null, null))
        {
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        String idStr = values[0];
        int id = Integer.parseInt(idStr);
        TestID testID = m_testManager.getTestID(id);

        if (testID == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Test " + id + " not found"); 
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_TEST.getCode(),
                    ActionStatus.UPNP_NO_SUCH_TEST.getDescription(), action);
        }
       
        String state = testID.getTestState();
        if (!(TestID.REQUESTED.equals(state) || TestID.IN_PROGRESS.equals(state)))
        {
            if (log.isInfoEnabled())
            {
                log.info("Test " + id + " in wrong state to be cancelled"); 
            }
            return new UPnPErrorResponse(
                ActionStatus.UPNP_STATE_PRECLUDES_CANCEL.getCode(),
                ActionStatus.UPNP_STATE_PRECLUDES_CANCEL.getDescription(), action);
        }
      
        // Cancel the test 
        HNAPIImpl.nativeCancelTest(id); 
        testID.setTestState(TestID.CANCELED);
        updateActiveTestIDs(m_testManager.getActiveTestIDsCSV());

        String[] resultsArray = {};
        // create response
        try
        {
            response = new UPnPActionResponse( resultsArray, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }

    /**
     * Requests a ping test. 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse test id of ping test 
     */
    private UPnPResponse ping(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 5)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        final String host = values[0];
        String repsStr = values[1];
        String timeoutStr = values[2];
        String blockSizeStr = values[3];
        String dscpStr = values[4];

        if (host == null || "".equals(host)
            || !Utils.validateUDANumericValue("ui4", repsStr, null, null)
            || !Utils.validateUDANumericValue("ui4", timeoutStr, null, null)
            || !Utils.validateUDANumericValue("ui2", blockSizeStr, null, null)
            || !Utils.validateUDANumericValue("ui1", dscpStr, minDSCP, maxDSCP))
        { 
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        final int testID = m_testManager.addTest(TestID.PING);

        // Implementation defaults
        int repsCheck = Integer.parseInt(repsStr);
        if (repsCheck == 0) {
            repsCheck = DEFAULT_REPS;
        }
        int timeoutCheck = Integer.parseInt(timeoutStr);
        if (timeoutCheck == 0) 
        {
            timeoutCheck = DEFAULT_TIMEOUT_MS;
        }
        int blockSizeCheck = Integer.parseInt(blockSizeStr);
        if (blockSizeCheck == 0)
        {
            blockSizeCheck = DEFAULT_BLOCKSIZE;
        } 

        final int reps = repsCheck; 
        final int timeout = timeoutCheck; 
        final int blockSize = blockSizeCheck; 
        final int dscp = Integer.parseInt(dscpStr); 

        TestID pingTestID = m_testManager.getTestID(testID);
        pingTestID.setTestState(TestID.IN_PROGRESS);

        // Do ping here in separate thread and write results
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(
                                   CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                String[] pingResults = HNAPIImpl.nativePing( testID, host, reps, timeout,
                    blockSize, dscp); 

                setPingResults(testID, pingResults);
            }
        });

        // return test id  in response
        String status = Integer.toString(testID); 
        try
        {
            response = new UPnPActionResponse(new String[] { status }, action); 
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }
        
        return response;
    }

    /**
     * Requests results of a ping test. 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse results of ping test 
     */
    private UPnPResponse getPingResult(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
      
        if (!Utils.validateUDANumericValue("ui4", values[0], null, null))
        {
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        String idStr = values[0];
        int id = Integer.parseInt(idStr);
        TestID testID = m_testManager.getTestID(id);

        if (testID == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Test " + id + " not found");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_TEST.getCode(),
                    ActionStatus.UPNP_NO_SUCH_TEST.getDescription(), action);
        }

        String type = testID.getTestType();
        if (!TestID.PING.equals(type))
        {
            if (log.isInfoEnabled())
            {
                log.info("Wrong test type");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_WRONG_TEST_TYPE.getCode(),
                    ActionStatus.UPNP_WRONG_TEST_TYPE.getDescription(), action);
        }

        String state = testID.getTestState();
        if (!TestID.COMPLETED.equals(state) && !TestID.CANCELED.equals(state))
        {
            if (log.isInfoEnabled())
            {
                log.info("Wrong test state");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_TEST_STATE.getCode(),
                    ActionStatus.UPNP_INVALID_TEST_STATE.getDescription(), action);
        }

        // Get test results
        Vector results = null;
        if (TestID.CANCELED.equals(state))
        {
            testID.setTestStatus("Error_Other");
            results = new Vector();
            results.add("Error_Other");
            results.add("Test cancelled");
            results.add("0");
            results.add("0");
            results.add("0");
            results.add("0");
            results.add("0");
        }
        else
        {
            results = (Vector) testID.getTestResults();
        }

        // create response
        try
        {
            String[] resultsArray =
                (String[]) results.toArray(new String[results.size()]);
            response = new UPnPActionResponse( resultsArray, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }

    /**
     * Requests a nslookup test. 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse test id of nslookup test 
     */
    private UPnPResponse nslookup(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 4)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        final String host = values[0];
        final String server = values[1];
        String repsStr = values[2];
        String timeoutStr = values[3];

        if (host == null || "".equals(host)
            || !Utils.validateUDANumericValue("ui4", repsStr, null, null)
            || !Utils.validateUDANumericValue("ui4", timeoutStr, null, null))
        { 
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        // Implementation specifics defaults and limits 
        int repsCheck = Integer.parseInt(repsStr);
        if (repsCheck == 0)
        {
            // default repetition 
            repsCheck = DEFAULT_REPS;
        }
        int timeoutCheck = Integer.parseInt(timeoutStr);
        if (timeoutCheck == 0)
        {
            // default timeout
            timeoutCheck = DEFAULT_TIMEOUT_MS;
        }
     
        final int testID = m_testManager.addTest(TestID.NSLOOKUP); 
        final int reps = repsCheck; 
        final int timeout = timeoutCheck; 

        TestID nslookupTestID = m_testManager.getTestID(testID);
        nslookupTestID.setTestState(TestID.IN_PROGRESS);

        // Do nslookup here in separate thread 
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(
                                   CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                Vector savedlookupResults = new Vector();
                String overallStatus = "Success";
                String addInfo = "";
                int successes = 0;
                for (int i = 0; i < reps; i++)
                {
                    String[] nslookupResults = HNAPIImpl.nativeNSLookup(testID, host, server, timeout);
                    savedlookupResults.add(nslookupResults);
                    if (TestID.SUCCESS.equals(nslookupResults[0])) 
                    {
                        // increment sucessful lookup count
                        successes++;
                    }
                    // bail if we fail lookup
                    if (TestID.ERROR_OTHER.equals(nslookupResults[0]) || 
                        TestID.ERROR_NODNSSERVER.equals(nslookupResults[0]) ||
                        TestID.ERROR_INTERNAL.equals(nslookupResults[0]))
                    {
                        // if a test is not successful bail here and set
                        // overall status to that lookup error/info.
                        overallStatus = nslookupResults[0];
                        addInfo = nslookupResults[1];
                        break;
                    }
                }
                setNSLookupResults(testID, overallStatus, addInfo, successes, savedlookupResults);
            }
        });

        String status = Integer.toString(testID); 
        try
        {
            response = new UPnPActionResponse(new String[] { status }, action); 
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }
        
        return response;
    }

    /**
     * Requests results of a nslookup test. 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse results of nslookup test 
     */
    private UPnPResponse getNSLookupResult(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
      
        if (!Utils.validateUDANumericValue("ui4", values[0], null, null))
        {
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        String idStr = values[0];
        int id = Integer.parseInt(idStr);
        TestID testID = m_testManager.getTestID(id);

        if (testID == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Test " + id + " not found");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_TEST.getCode(),
                    ActionStatus.UPNP_NO_SUCH_TEST.getDescription(), action);
        }

        String type = testID.getTestType();
        if (!TestID.NSLOOKUP.equals(type))
        {
            if (log.isInfoEnabled())
            {
                log.info("Wrong test type");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_WRONG_TEST_TYPE.getCode(),
                    ActionStatus.UPNP_WRONG_TEST_TYPE.getDescription(), action);
        }

        String state = testID.getTestState();
        if (!TestID.COMPLETED.equals(state) && !TestID.CANCELED.equals(state))
        {
            if (log.isInfoEnabled())
            {
                log.info("Wrong test state");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_TEST_STATE.getCode(),
                    ActionStatus.UPNP_INVALID_TEST_STATE.getDescription(), action);
        }

        // Get test results
        Vector results = null;
        if (TestID.CANCELED.equals(state))
        {
            testID.setTestStatus("Error_Other");
            results = new Vector();
            results.add("Error_Other");
            results.add("Test cancelled");
            results.add("0");
            results.add("");
        }
        else
        {
            results = (Vector) testID.getTestResults();
        }

        // create response
        try
        {
            String[] resultsArray =
                (String[]) results.toArray(new String[results.size()]);
            response = new UPnPActionResponse( resultsArray, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }

    /**
     * Requests a traceroute test. 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse test id of traceroute test 
     */
    private UPnPResponse traceroute(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 5)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        final String host = values[0];
        String timeoutStr = values[1];
        String blockSizeStr = values[2];
        String hopsStr = values[3];
        String dscpStr = values[4];

        if (host == null || "".equals(host)
            || !Utils.validateUDANumericValue("ui4", hopsStr, null, null)
            || !Utils.validateUDANumericValue("ui4", timeoutStr, null, null)
            || !Utils.validateUDANumericValue("ui2", blockSizeStr, null, null)
            || !Utils.validateUDANumericValue("ui1", dscpStr, minDSCP, maxDSCP))
        { 
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        // Implementation defaults
        int hopsCheck = Integer.parseInt(hopsStr);
        if (hopsCheck == 0)
        {
            hopsCheck = DEFAULT_HOPS;
        } 
        int timeoutCheck = Integer.parseInt(timeoutStr);
        if (timeoutCheck == 0) 
        {
            timeoutCheck = DEFAULT_TIMEOUT_MS;
        }
        int blockSizeCheck = Integer.parseInt(blockSizeStr);
        if (blockSizeCheck == 0)
        {
            blockSizeCheck = DEFAULT_BLOCKSIZE;
        } 

        final int testID = m_testManager.addTest(TestID.TRACEROUTE); 
        final int hops = hopsCheck; 
        final int timeout = timeoutCheck; 
        final int blockSize = blockSizeCheck;
        final int dscp = Integer.parseInt(dscpStr);

        TestID tracerouteTestID = m_testManager.getTestID(testID);
        tracerouteTestID.setTestState(TestID.IN_PROGRESS);

        // Do traceroute here in separate thread by calling into mpeos
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(
                                   CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                String[] tracerouteResults = HNAPIImpl.nativeTraceroute(testID, host,
                    hops, timeout, blockSize, dscp);

                setTracerouteResults(testID, tracerouteResults);
            }
        });
       
        String status = Integer.toString(testID); 
        try
        {
            response = new UPnPActionResponse(new String[] { status }, action); 
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }
        

        return response;
    }

    /**
     * Requests results of a traceroute test. 
     *
     * @param action
     *            The action
     *
     * @return UPnPResponse results of traceroute test 
     */
    private UPnPResponse getTracerouteResult(UPnPActionInvocation action, String[] values)
    {
        UPnPResponse response = null;

        // Verify the arguments are valid
        if (values == null || values.length != 1)
        {
            if (log.isInfoEnabled())
            {
                log.info("Received wrong number of arguments");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }
      
        if (!Utils.validateUDANumericValue("ui4", values[0], null, null))
        {
            if (log.isInfoEnabled())
            {
                log.info("Argument validation failed");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);
        }

        String idStr = values[0];
        int id = Integer.parseInt(idStr);
        TestID testID = m_testManager.getTestID(id);

        if (testID == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Test " + id + " not found");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_NO_SUCH_TEST.getCode(),
                    ActionStatus.UPNP_NO_SUCH_TEST.getDescription(), action);
        }

        String type = testID.getTestType();
        if (!TestID.TRACEROUTE.equals(type))
        {
            if (log.isInfoEnabled())
            {
                log.info("Wrong test type");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_WRONG_TEST_TYPE.getCode(),
                    ActionStatus.UPNP_WRONG_TEST_TYPE.getDescription(), action);
        }

        String state = testID.getTestState();
        if (!TestID.COMPLETED.equals(state) && !TestID.CANCELED.equals(state))
        {
            if (log.isInfoEnabled())
            {
                log.info("Wrong test state");
            }
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_TEST_STATE.getCode(),
                    ActionStatus.UPNP_INVALID_TEST_STATE.getDescription(), action);
        }

        // Get test results
        Vector results = null;
        if (TestID.CANCELED.equals(state))
        {
            testID.setTestStatus("Error_Other");
            results = new Vector();
            results.add("Error_Other");
            results.add("Test cancelled");
            results.add(" ");
            results.add("0");
            results.add(" ");
        }
        else
        {
            results = (Vector) testID.getTestResults();
        }

        // create response
        try
        {
            String[] resultsArray =
                (String[]) results.toArray(new String[results.size()]);
            response = new UPnPActionResponse( resultsArray, action);
        }
        catch (Exception e)
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), action);         
        }

        return response;
    }

    public void updateTestIDs(String value)
    {
        m_testIDsEventer.set(value);
    }

    public void updateActiveTestIDs(String value)
    {
        m_activeTestIDsEventer.set(value);
    }


    private void setPingResults(int id, String[] values)
    {
        TestID testID = m_testManager.getTestID(id);
        if (testID.getTestState().equals(TestID.CANCELED))
        {
            return;
        }
        testID.setTestState(TestID.COMPLETED);
        Vector results = new Vector();
        results.add(values[0]);
        results.add(values[1]);
        if (TestID.SUCCESS.equals(values[0]))
        {
            results.add(values[2]);
            results.add(values[3]);
            results.add(values[4]);
            results.add(values[5]);
            results.add(values[6]);
        }
        else
        {
            results.add("0");
            results.add("0");
            results.add("0");
            results.add("0");
            results.add("0");
        }
        testID.setTestStatus(values[0]);
        testID.setTestResults(results);
        updateActiveTestIDs(m_testManager.getActiveTestIDsCSV());
    }

    private void setNSLookupResults(int id, String overallStatus, String addInfo, int successes, Vector values)
    {

        
        TestID testID = m_testManager.getTestID(id);
        if (testID.getTestState().equals(TestID.CANCELED))
        {
            return;
        }
        testID.setTestState(TestID.COMPLETED);
        Vector results = new Vector();
        testID.setTestStatus(overallStatus);
        results.add(overallStatus);
        results.add(addInfo);
        Integer numSuccess = new Integer(successes);
        results.add(numSuccess.toString());
        if (TestID.SUCCESS.equals(overallStatus))
        {
            StringBuffer finalResults = new StringBuffer();
            
            // now concatenate the results strings returned and create results XML
            for (int i = 0; i < values.size(); i++)
            {
                String[] testresults = (String[]) values.get(i);
                for (int j = 0; j < testresults.length; j++)
                {
                    // skip additional info
                    if (j == 1)
                    {
                        continue;
                    }
                    if ("".equals(testresults[j]))
                    {
                        finalResults.append("  ");
                    }
                    else
                    {
                        finalResults.append(testresults[j]);
                    }
                    finalResults.append(";");
                }
            }
            NSLookupResult nsResult = new NSLookupResult(values.size(), finalResults.toString()); 
            results.add(nsResult.toString());
        }
        else
        {
            // set result to empty string in overall failure case 
            results.add(" ");
        }
        testID.setTestResults(results);

        updateActiveTestIDs(m_testManager.getActiveTestIDsCSV());
    }

    private void setTracerouteResults(int id, String[] values)
    {
        TestID testID = m_testManager.getTestID(id);
        if (testID.getTestState().equals(TestID.CANCELED))
        {
            return;
        }
        Vector results = new Vector();
        testID.setTestState(TestID.COMPLETED);
        results.add(values[0]);
        if (TestID.SUCCESS.equals(values[0]))
        {
            results.add(values[1]);
            results.add(values[2]);
            results.add(values[3]);
        }
        else
        {
            results.add(" ");
            results.add("0");
            results.add(" ");
        }

        testID.setTestResults(results);
        testID.setTestStatus(values[0]);
        updateActiveTestIDs(m_testManager.getActiveTestIDsCSV());
    }
}


