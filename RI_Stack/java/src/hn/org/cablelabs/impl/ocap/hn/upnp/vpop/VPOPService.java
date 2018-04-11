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

package org.cablelabs.impl.ocap.hn.upnp.vpop;

import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.mpe.MediaAPIImpl;
import org.cablelabs.impl.ocap.hardware.ExtendedHost;
import org.cablelabs.impl.ocap.hardware.HostImpl;
import org.cablelabs.impl.ocap.hn.security.NetSecurityManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.ocap.hardware.Host;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPManagedService;

public class VPOPService implements UPnPActionHandler
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(VPOPService.class);
    
    /** Defines the type for the ContentDirectoryService */
    public static final String SERVICE_TYPE = "urn:cablelabs-com:service:ViewPrimaryOutputPort:1";
    
    private static final String SERVICEID = "VPOP:";
    
    // ACTION DEFINITIONS

    public static final String MUTE = "AudioMute";
    public static final String AUDIO_RESTORE = "AudioRestore";
    public static final String POWER_OFF = "PowerOff";
    public static final String POWER_ON = "PowerOn";
    public static final String TUNE = "Tune";
    public static final String POWER_STATUS = "PowerStatus";
    
    /**
     * Handle to the NetSecurityManager. Used to authorize the action.
     */
    private static final NetSecurityManagerImpl securityManager = (NetSecurityManagerImpl) NetSecurityManagerImpl.getInstance();
    
    public void registerService(UPnPManagedService service)
    {
        if(service == null)
        {
            return;
        }
        
        service.setActionHandler(this);
    }
    
    public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
    {
        if (log.isInfoEnabled())
        {
            log.info("notifyActionReceived() - called");
        }            
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
        
        // TODO : OCTECH-88 having to use implementation class.
        final InetAddress client = ((UPnPActionImpl)action.getAction()).getInetAddress();
        final String[] requestStrings = ((UPnPActionImpl)action.getAction()).getRequestStrings();
        final NetworkInterface netInt = ((UPnPActionImpl)action.getAction()).getNetworkInterface();

        // These are actions required to be implemented
        if (    actionName.equals(MUTE) ||
                actionName.equals(AUDIO_RESTORE) ||
                actionName.equals(POWER_ON) ||
                actionName.equals(POWER_OFF) ||
                actionName.equals(TUNE) ||
                actionName.equals(POWER_STATUS))
        {
            // Begin NetSecurityManager authorization before processing any valid action        
            if (!securityManager.notifyAction(SERVICEID + actionName, client, "", -1, requestStrings, netInt))
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ContentDirectoryService.browseAction() - unauthorized");
                }
                return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                        ActionStatus.UPNP_UNAUTHORIZED.getDescription(), action);
            }
        
            // Process actions called out to be required in UPnP CDS Spec
            if (actionName.equals(MUTE))
            {
                response = muteAction(action, values);
            }
            else if (actionName.equals(AUDIO_RESTORE))
            {
                response = audioRestoreAction(action, values);
            }
            else if (actionName.equals(POWER_ON))
            {
                response = powerOnAction(action, values);
            }
            else if (actionName.equals(POWER_OFF))
            {
                response = powerOffAction(action, values);
            }
            else if (actionName.equals(TUNE))
            {
                response = tuneAction(action, values);
            }
            else if (actionName.equals(POWER_STATUS))
            {
                response = powerStatusAction(action, values);
            }
      
            // End NetSecurityManager
            securityManager.notifyActionEnd(client, SERVICEID + actionName);                       
        }
       
        // Return response or Invalid Action if not set
        return response != null ? response : new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ACTION.getCode(),
                ActionStatus.UPNP_INVALID_ACTION.getDescription(), action);
    }
    
    // Action implementation for power status OCAP extension to the CDS to support VPOP functionality
    private UPnPResponse powerStatusAction(UPnPActionInvocation invocation, String[] values)
    {
        if (log.isDebugEnabled())
        {
            log.debug("powerStatusAction() - called");
        }            
        if (values == null || values.length > 0)
        {
            if (log.isWarnEnabled())
            {
                log.warn("powerStatusAction() - Received invalid arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        // Query platform for current power status 
        int powerMode = Host.getInstance().getPowerMode();
        
        String powerStatus = "FULL POWER";
        if (powerMode == Host.LOW_POWER)
        {
            powerStatus = "STANDBY";
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("powerStatusAction() - completed");
        }            
        
        return new UPnPActionResponse(new String[] { powerStatus }, invocation);
    }

    /**
     * Initiate a tune by translating the value supplied in action string as key events
     * to the platform.
     * 
     * @param invocation    action recieved
     * @param values        string indicating major & optional minor channel number
     * 
     * @return  response to this action, may be an error response
     */
    private UPnPResponse tuneAction(UPnPActionInvocation invocation, String[] values)
    {
        if (log.isDebugEnabled())
        {
            log.debug("tuneAction() - called");
        }            
        if (values == null || values.length != 2)
        {
            if (log.isWarnEnabled())
            {
                log.warn("tuneAction() - Received incorrect number of arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }
        
        if (!isValidConnectionId(values[0]))
        {
            if (log.isWarnEnabled())
            {
                log.warn("tuneAction() - Received invalid connection id: " + values[0]);
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), invocation);                       
        }
            
        // Determine if minor channel is present
        String minorChannel = null;
        String majorChannel = values[1];
        int endIdx = values[1].indexOf(',');
        if (endIdx != -1)
        {
            majorChannel = values[1].substring(0, endIdx);
            minorChannel = values[1].substring(endIdx + 1);
        }
        
        // Translate the string into a vector of key events
        Vector keyCodes = new Vector();
        for (int i = 0; i < majorChannel.length(); i++)
        {
            Integer code = char2code(majorChannel.charAt(i));
            if (code.intValue() != -1)
            {
                keyCodes.add(code);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("tuneAction() - major channel unsupported character: " + 
                            majorChannel.charAt(i) + " in value: " + values[1]);
                }                                        
                return new UPnPErrorResponse(ActionStatus.UPNP_ARGUMENT_INVALID.getCode(),
                        ActionStatus.UPNP_ARGUMENT_INVALID.getDescription(), invocation);            
            }
        }
        
        // Include minor channel if non-null
        if (minorChannel != null)
        {
            keyCodes.add(new Integer(KeyEvent.VK_PERIOD));
            for (int i = 0; i < minorChannel.length(); i++)
            {
                Integer code = char2code(minorChannel.charAt(i));
                if (code.intValue() != -1)
                {
                    keyCodes.add(code);
                }
                else
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("tuneAction() - minor channel unsupported character: " + 
                                minorChannel.charAt(i) + " in value: " + values[1]);
                    }                                        
                    return new UPnPErrorResponse(ActionStatus.UPNP_ARGUMENT_INVALID.getCode(),
                            ActionStatus.UPNP_ARGUMENT_INVALID.getDescription(), invocation);            
                }
            }            
        }
        
        // Make sure got at least one digit as an argument
        if (keyCodes.size() < 1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("tuneAction() - Received empty list of keys for tune action");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_ARGUMENT_INVALID.getCode(),
                    ActionStatus.UPNP_ARGUMENT_INVALID.getDescription(), invocation);                        
        }
        
        // add VK_ENTER as last keyEvent
        keyCodes.add(new Integer(KeyEvent.VK_ENTER));

        // Initiate generation of each key through platform
        MediaAPIManager mediaMgr = MediaAPIImpl.getInstance();
        for (int i = 0; i < keyCodes.size(); i++)
        {
            int code = ((Integer)keyCodes.get(i)).intValue();
            // *TODO* - assuming KEY_TYPE, is this correct? KEY_PRESSED or KEY_RELEASED?
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("tuneAction() - generating key event for code: " + code);
                }                                        
                mediaMgr.generatePlatformKeyEvent(KeyEvent.KEY_RELEASED, code);
            }
            catch (MPEMediaError e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("tuneAction() - exception encountered generating key event: ", e);
                }                                        
                return new UPnPErrorResponse(ActionStatus.UPNP_ARGUMENT_INVALID.getCode(),
                        ActionStatus.UPNP_ARGUMENT_INVALID.getDescription(), invocation);                            
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("tuneAction() - completed");
        }            
        
        return new UPnPActionResponse(new String[] { }, invocation);
    }

    private UPnPResponse powerOnAction(UPnPActionInvocation invocation, String[] values)
    {
        if (log.isDebugEnabled())
        {
            log.debug("powerOnAction() - called");
        }            
        if (values == null || values.length > 0)
        {
            if (log.isWarnEnabled())
            {
                log.warn("powerOnAction() - Invalid number of received arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }

        Host.getInstance().setPowerMode(Host.FULL_POWER);            

        if (log.isDebugEnabled())
        {
            log.debug("powerOnAction() - completed");
        }            
        return new UPnPActionResponse(new String[] { }, invocation);
    }

    private UPnPResponse powerOffAction(UPnPActionInvocation invocation, String[] values)
    {
        if (log.isDebugEnabled())
        {
            log.debug("powerOffAction() - called");
        }            
        if (values == null || values.length != 1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("powerOffAction() - Received incorrect number of arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }

        if (!isValidConnectionId(values[0]))
        {
            if (log.isWarnEnabled())
            {
                log.warn("powerOffAction() - Received invalid connection id: " + values[0]);
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), invocation);                       
        }

        Host.getInstance().setPowerMode(Host.LOW_POWER);                        
        
        if (log.isDebugEnabled())
        {
            log.debug("powerOffAction() - completed");
        }            
        return new UPnPActionResponse(new String[] { }, invocation);
    }
    
    private UPnPResponse audioRestoreAction(UPnPActionInvocation invocation, String[] values)
    {
        if (log.isDebugEnabled())
        {
            log.debug("audioRestoreAction() - called");
        }            
        if (values == null || values.length != 1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("audioRestoreAction() - received incorrect number of arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }

        if (!isValidConnectionId(values[0]))
        {
            if (log.isWarnEnabled())
            {
                log.warn("audioRestoreAction() - received invalid connection id: " + values[0]);
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), invocation);                       
        }
                    
        ((HostImpl) Host.getInstance()).setAudioMode(ExtendedHost.AUDIO_ON);            

        if (log.isDebugEnabled())
        {
            log.debug("audioRestoreAction() - completed");
        }            

        return new UPnPActionResponse(new String[] { }, invocation);
    }

    private UPnPResponse muteAction(UPnPActionInvocation invocation, String[] values)
    {
        if (log.isDebugEnabled())
        {
            log.debug("muteAction() - called");
        }            
        if (values == null || values.length != 1)
        {
            if (log.isWarnEnabled())
            {
                log.warn("muteAction() - received incorrect number of arguments");
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), invocation);            
        }

        if (!isValidConnectionId(values[0]))
        {
            if (log.isWarnEnabled())
            {
                log.warn("muteAction() - received invalid connection id: " + values[0]);
            }            
            return new UPnPErrorResponse(ActionStatus.UPNP_UNAUTHORIZED.getCode(),
                    ActionStatus.UPNP_UNAUTHORIZED.getDescription(), invocation);                       
        }
        
        ((HostImpl) Host.getInstance()).setAudioMode(ExtendedHost.AUDIO_MUTED);
        if (log.isDebugEnabled())
        {
            log.debug("muteAction() - completed");
        }            
        return new UPnPActionResponse(new String[] { }, invocation);
    }

    public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
    {
        if (log.isInfoEnabled())
        {
            log.info("VPOPService action handler replaced");
        } 
    }
    
    /**
     * Verifies the supplied connection ID is valid VPOP connection id.
     * 
     * @param idStr connection id to validate
     * 
     * @return  true if connection id matches current vpop connection id, false otherwise
     */
    private boolean isValidConnectionId(String idStr)
    {
        boolean isValid = false;
        try
        {
            if (idStr != null)
            {
                if (new Integer(idStr).equals(ContentDirectoryService.getVPOPConnectionId()))
                {
                    isValid = true;
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("isValidConnectionId() - invalid id: " + idStr + ", current id: " +
                                ContentDirectoryService.getVPOPConnectionId());
                    }             
                }
            }
        }
        catch (NumberFormatException e)
        {
            if (log.isInfoEnabled())
            {
                log.info("isValidConnectionId() - invalid id string: " + idStr + ", current id: " +
                        ContentDirectoryService.getVPOPConnectionId());
            }                         
        }
        return isValid;
    }

    /**
     * Translates supplied character into corresponding key code.
     * Currently the only valid keys which can be translated into key codes
     * are digits.
     * 
     * @param keyChar   character to translate to key code
     * @return  key code, -1 if unsupported character
     */
    private Integer char2code(char keyChar)
    {
        Integer code = new Integer(-1);
        if (keyChar == '0')
        {
            code = new Integer(KeyEvent.VK_0);
        }
        else if (keyChar == '1')
        {
            code = new Integer(KeyEvent.VK_1);
        }
        else if (keyChar == '2')
        {
            code = new Integer(KeyEvent.VK_2);
        }
        else if (keyChar == '3')
        {
            code = new Integer(KeyEvent.VK_3);
        }
        else if (keyChar == '4')
        {
            code = new Integer(KeyEvent.VK_4);
        }
        else if (keyChar == '5')
        {
            code = new Integer(KeyEvent.VK_5);
        }
        else if (keyChar == '6')
        {
            code = new Integer(KeyEvent.VK_6);
        }
        else if (keyChar == '7')
        {
            code = new Integer(KeyEvent.VK_7);
        }
        else if (keyChar == '8')
        {
            code = new Integer(KeyEvent.VK_8);
        }
        else if (keyChar == '9')
        {
            code = new Integer(KeyEvent.VK_9);
        }
        else 
        {
            if (log.isWarnEnabled())
            {
                log.warn("char2code() - unsupported character: " + keyChar);
            }                        
        }
        return code;
    }
}
