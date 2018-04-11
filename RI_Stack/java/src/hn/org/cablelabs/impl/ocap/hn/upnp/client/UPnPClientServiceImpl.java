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

package org.cablelabs.impl.ocap.hn.upnp.client;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hn.NetModuleImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPServiceImpl;
import org.cablelabs.impl.util.HNEventMulticaster;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceStateTable;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.UPnPStatus;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.client.UPnPStateVariableListener;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPAdvertisedStateVariable;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPGeneralErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;

/**
 * This interface is the client representation of a UPnP 
 * service. 
 */
public class UPnPClientServiceImpl extends UPnPServiceImpl implements UPnPClientService
{
    // PRIVATE ATTRIBUTES
    
    /**
     * List of <code>CallerContext</code>s that have added listeners.
     * holds <code>UPnPStateVariableChangeListener</code>s for a specific
     * state variable.
     */
    private CallerContext ccList = null;

    // Cybergarage Control Point which is associated this service representation
    private final ControlPoint m_controlPoint;
    
    // Device which owns this service
    private final UPnPClientDeviceImpl m_device;

    // Timeout for a UPnP Action to complete is 30 secs
    private static final int ACTION_TIMEOUT_MS = 30 * 1000;

    // Response received from action in invocation thread
    private UPnPResponse m_response = null;
    
    // Threads used to perform actions and subscription requests
    private Runnable m_actionThread = null;

    // Object used to synchronize access to state variables & actions    
    private Object m_signal = new Object();

    private static final Logger log = Logger.getLogger(UPnPClientServiceImpl.class);

    /**
     * Construct an object of this class from a UPnPControlPoint.
     *     
     * @param service    Cybergarage service which this class wraps
     */
    public UPnPClientServiceImpl(Service service, UPnPClientDeviceImpl device,
                            ControlPoint controlPoint)
    {
        super(service);
        
        if (log.isDebugEnabled())
        {
            log.debug("Creating service for device: " + device.getFriendlyName());
        }                            
        
        assert service != null;
        m_service = service;
        
        assert device != null;
        m_device = device;

        assert controlPoint != null;
        m_controlPoint = controlPoint;
        
     }
    
    /**
     * Construct an object of this class.
     *     
     * @param service    Cybergarage service which this class wraps
     */
    public UPnPClientServiceImpl(Service service, UPnPClientDeviceImpl device)
    {
        super(service);
        assert service != null;
        m_service = service;
        
        assert device != null;
        m_device = device;

        m_controlPoint = null;
     }   

    /**
     * Gets the UPnP SCPDURL of this service. This value is taken 
     * from the value of the SCPDURL element within a device 
     * description. 
     *  
     * <p>For a UPnPService that is not part of a device (for example,
     * following creation of the associated UPnPManagedService
     * but prior to association with a UPnPManagedDevice),
     * the value returned is the empty string.
     *
     * @return The URL used to retrieve the service description of 
     *         this service.
     */
    public String getSCPDURL()
    {
        return m_service.getSCPDURL();
    }

    /**
     * Gets the UPnP controlURL of this service. This value is taken
     * from the value of the controlURL element within a device 
     * description. 
     *  
     * <p>For a UPnPService that is not part of a device (for example,
     * following creation of the associated UPnPManagedService
     * but prior to association with a UPnPManagedDevice),
     * the value returned is the empty string.
     *
     * @return The URL used by a control point to invoke actions on 
     *         this service.
     */
    public String getControlURL()
    {
        return m_service.getControlURL();
    }

    /**
     * Gets the UPnP eventSubURL of this service. This value is 
     * taken from the value of the eventSubURL element within a 
     * device description. 
     *  
     * <p>For a UPnPService that is not part of a device (for example,
     * following creation of the associated UPnPManagedService
     * but prior to association with a UPnPManagedDevice),
     * the value returned is the empty string.
     *  
     * <p>If this service does not have eventing, the value returned 
     * is the empty string. 
     *
     * @return The URL used by a control point to subscribe to 
     *         evented state variables.
     */
    public String getEventSubURL()
    {
        return m_service.getEventSubURL();
    }

    /**
     * Gets all of the UPnP state variables supported by this 
     * service.  UPnP state variable information is taken from the 
     * stateVariable elements in the UPnP service description. 
     *
     * @return An array containing the 
     *         <code>UPnPStateVariable</code>s. If the service
     *         contains no state variables, returns a zero-length
     *         array.
     */
    public UPnPClientStateVariable[] getStateVariables()
    {
        final ServiceStateTable table[] = { null };
        try
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            ccm.getSystemContext().runInContextSync(new Runnable()
            {
                public void run()
                {
                    table[0] = m_service.getServiceStateTable();
                }
            });            
        }
        catch (InvocationTargetException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("getStateVariables() - exception while getting state variables: ",e);
            }                                        
        }
       
        UPnPClientStateVariable stateVars[] = null;
        if ((table[0] != null) && (!table[0].isEmpty()))
        {
            stateVars = new UPnPClientStateVariable[table[0].size()];
            for (int i = 0; i < table[0].size(); i++)
            {
                StateVariable var = (StateVariable)table[0].get(i);
                stateVars[i] = new UPnPClientStateVariableImpl(var, this);
            }
        }
        else
        {
            stateVars = new UPnPClientStateVariable[0];
        }
        return stateVars;        
    }

    /**
     * Gets a UPnP state variable from the UPnP description of this
     * service.  Supported state variable names are provided by a UPnP device
     * in the name element of each stateVariable element in a device
     * service description.
     *
     * @param stateVariableName The name of the state variable to get.
     *
     * @return The state variable corresponding to the
     *      <code>stateVariableName</code> parameter.
     *
     * @throws IllegalArgumentException if the <code>stateVariableName</code>
     *      does not match a state variable name in this service.
     */
    public UPnPClientStateVariable getStateVariable(final String stateVariableName)
    {  
        final StateVariable[] stateVar = { null };
        try
        {
            // Run in system context since underlying call utilizes socket call
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            ccm.getSystemContext().runInContextSync(new Runnable()
            {
                public void run()
                {
                    stateVar[0] = m_service.getStateVariable(stateVariableName);
                }
            });            
        }
        catch (InvocationTargetException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("getStateVariable() - exception while getting state variable: ",e);
            }                                        
        }

        if (stateVar[0] != null)
        {
            return (UPnPClientStateVariable)(new UPnPClientStateVariableImpl(stateVar[0], this));
        }
        else
        {
            throw new IllegalArgumentException("No state variable matches name: " + stateVariableName);            
        }
    }

    /**
     * Posts an action to the network.  Sends the action from the control
     * point to the device the service is in.  The device MAY be on 
     * the local host.  If no handler is set when this method is 
     * called, the response is consumed by the implementation in an 
     * implementation-specific fashion. 
     *
     * @param actionInvocation The action invocation to post.
     *
     * @param handler The handler that will be notified when the action
     *      response is received. May be null, in which case the
     *      action response will be discarded.
     *
     * @throws NullPointerException if action is null.
     *
     * @see UPnPActionInvocation
     */
    public void postActionInvocation(UPnPActionInvocation actionInvocation, 
                                     UPnPActionResponseHandler handler)
    {
        if (actionInvocation == null)
        {
            throw new NullPointerException("Action invocation was null");
        }
        
        // Get the action associated with this invocation
        UPnPActionImpl actionImpl = (UPnPActionImpl)actionInvocation.getAction();
        Action action = actionImpl.getAction();
        
        // Create the argument list for cybergarage action using action invocation args
        ArgumentList argList = action.getArgumentList();
        
        // Set the arguments of underlying Cybergarage action
        action.setArgumentValues(argList);
        
        // Posting action        
        if (log.isDebugEnabled())
        {
            log.debug("postActionInvocation() - Posting action " + action.getName() + 
            " with args: " + dumpArgStr(action) + " on device: " + getDevice().getFriendlyName() + 
            " / " + getDevice().getInetAddress());
        }                            

        // Invoke this cybergarage action in a thread and notify listener when it completes
        final UPnPActionInvocation ai = actionInvocation;
        final UPnPActionResponseHandler h = handler;
        final Action a = action;
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(m_actionThread = new Runnable()
        {
            public void run()
            {
                invokeAction(ai, h, a);
            }
        });
    }
    
    /**
     * Call underlying cybergarage action which is synchronous and report results
     * when it completes.
     * 
     * @param   action  cybergarage action
     * @param   handler notify this handler when action completes
     */
    private void invokeAction(UPnPActionInvocation actionInvocation,
                              UPnPActionResponseHandler handler,
                              Action action)
    {
        // Spawn a thread and wait 30 secs for UPnP Action to complete
        m_response = null;
        final UPnPActionInvocation ai = actionInvocation;
        final Action a = action;
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                invokeCybergarageAction(a, ai);
            }
        });
       
        // Wait here to be signaled that the action completed, handle timeout by notifying
        // handler and killing thread
        try
        {
            synchronized (m_actionThread)
            {
                m_actionThread.wait(ACTION_TIMEOUT_MS);
            }
        }
        catch (InterruptedException e)
        {
            // ignore exception
        }

        // If response is null, action timed out
        if (m_response == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Create GeneralErrorResponse due to timeout while waiting for cybergarage action to complete");
            }                            
            m_response = new UPnPGeneralErrorResponse(UPnPGeneralErrorResponse.NETWORK_TIMEOUT, actionInvocation);         
            
            // Thread will be terminated when socket times out so no thread cleanup needed here
        }
        
        // Notify the handler of the response
        if (handler != null)
        {            
            handler.notifyUPnPActionResponse(m_response);
       }
    }

    /**
     * 
     * @param action
     * @param actionInvocation
     */
    private void invokeCybergarageAction(Action action, UPnPActionInvocation actionInvocation)
    {
        if (log.isDebugEnabled())
        {
            log.debug("invokeCybergarageAction() - invoking action: " + action.getName() +
            " on device: " + getDevice().getFriendlyName() + " / " + getDevice().getInetAddress());
        }                                        
        // Initiate the underlying cybergarage action, make sure it completes within 30 secs
        // True will be returned if HTTP status code is less than 300, false otherwise
        boolean isHTTPSuccess = action.postControlAction();        
 
        // Formulate the response for this action based on HTTP status and UPnP Control Status
        UPnPStatus aStatus = action.getStatus();
        UPnPStatus cStatus = action.getControlStatus();
         
        // If action was successful
        if (isHTTPSuccess)
        {
            if (log.isDebugEnabled())
            {
                log.debug("invokeCybergarageAction() - action " + action.getName() + 
                " invocation was successful");
            }                                        
            String outArgVals[] = UPnPActionImpl.getArgumentStrs(action, 1, 1);
            try
            {
                m_response = new UPnPActionResponse(outArgVals, actionInvocation);
            }
            catch (Exception e)
            {
                m_response = new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                        ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription(), actionInvocation);    
            }
        }
        else
        {
            // If HTTP status code was 500, create error response
            if (aStatus.getCode() == 500)
            {
                m_response = new UPnPErrorResponse(cStatus.getCode(), cStatus.getDescription(), actionInvocation);
                if (log.isDebugEnabled())
                {
                    log.debug("invokeCybergarageAction() - action " + action.getName() + 
                    " invocation was not successful, http response code: " + m_response.getHTTPResponseCode());
                }                            
            }
            // cybergarage specific .. returns status code 0 on no response 
            else if (aStatus.getCode() == 0)
            {
                m_response = new UPnPGeneralErrorResponse(UPnPGeneralErrorResponse.NETWORK_TIMEOUT, actionInvocation);
                if (log.isDebugEnabled())
                {
                    log.debug("invokeCybergarageAction() - action " + action.getName() + 
                    " invocation was not successful, http response code: " + m_response.getHTTPResponseCode());
                } 
            }
            else
            {
                // For all other error codes, create a general error response
                m_response = new UPnPGeneralErrorResponse(aStatus.getCode(), actionInvocation);
                if (log.isDebugEnabled())
                {
                    log.debug("invokeCybergarageAction() - action " + action.getName() + 
                    " invocation was not successful, http response code: " + m_response.getHTTPResponseCode());
                }                            
            }                    
        }
        
        // Notify main thread that action invocation has completed
        synchronized (m_actionThread)
        {
            m_actionThread.notifyAll();
        }        
    }
    
    /**
     * Adds a state variable listener to this UPnPClientService. If this
     * service has evented state variables, this method will cause the
     * control point to attempt to subscribe to the service if it is
     * not already subscribed. See UPnP Device Architecture specification
     * for UPnP service and state variable subscription.
     * Adding a listener which is the same instance as a previously added
     * (and not removed) listener has no effect.
     *
     * Parameters:
     * listener - The listener to add.
     */
    public void addStateVariableListener(UPnPStateVariableListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addStateVariableListener() - called for " + this.getServiceId());
        }                                        
        // Make sure this service is evented
        if (!hasEventedStateVariable())
        {
            return; 
        }
        
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        Data data = getData(ccm.getCurrentContext());
        UPnPStateVariableListener previousListeners = data.stateVariableListeners;
        data.stateVariableListeners = HNEventMulticaster.add(data.stateVariableListeners, listener);
        //Make sure that listener wasn't already registered
        if (previousListeners == data.stateVariableListeners)
        {
            return;
        }
        // subscribe if not already subscribed
        if ((!m_service.isSubscribed()) && (!m_service.isSubscriptionPending()))
        {
            setSubscribedStatus(true);
        }
        else 
        {
            //Even if service has been previously subscribed to still notify listeners of a new subscriber
            notifyListenersSubscribed(true);
        }
    }
    
    /**
     * Called when a service should notify listeners that a state variable has changed.
     * Will notify listeners that state variable value has changes.
     * 
     * @param variable  state variable which has changed
     */
    public void notifyListenersValueChanged(final UPnPClientStateVariable variable)
    {
        CallerContext ctx = ccList;
        if (ctx != null)
        {
            ctx.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    CallerContext ctx = ccm.getCurrentContext();
                    Data data = (Data)ctx.getCallbackData(UPnPClientServiceImpl.this);
                    if (data != null && data.stateVariableListeners != null)
                    {
                        data.stateVariableListeners.notifyValueChanged(variable);
                    }
                }
            });
        }
    }
    
    /**
     * Send either a subscribed or unsubscribed event to all listeners for all evented 
     * variables of this service. 
     * 
     * @param isSubscribed  true if subscribed, false otherwise
     */
    public void notifyListenersSubscribed(final boolean isSubscribed)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyListenersSubscribed() - is subscribed: " + isSubscribed);
        }                                        
        final UPnPClientService service = this;
        CallerContext ctx = ccList;
        if (ctx != null)
        {
            ctx.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    CallerContext ctx = ccm.getCurrentContext();
                    Data data = (Data)ctx.getCallbackData(UPnPClientServiceImpl.this);
                    if (data != null && data.stateVariableListeners != null)
                    {
                        if (isSubscribed)
                        {
                            data.stateVariableListeners.notifySubscribed(service);
                        }
                        else
                        {
                            data.stateVariableListeners.notifyUnsubscribed(service);
                        }
                    }
                }
            });
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("notifyListenersSubscribed() - done");
        }                                        
    }
    
    /**
     * Determine if this service has any evented state variables.
     * 
     * @return  true if this service has at least one evented state variable,
     *          false if it has none
     */
    public boolean hasEventedStateVariable()
    {
        boolean hasEvents = false;
        UPnPClientStateVariable vars[] = getStateVariables();
        for (int i = 0; i < vars.length; i++)
        {
            if (vars[i].isEvented())
            {
                hasEvents = true;
                break;
            }
        }
        return hasEvents;
    }

    /**
     * Removes a change listener.
     *
     * @param listener The listener to remove.
     */
    public void removeStateVariableListener(UPnPStateVariableListener listener)
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext ctx = ccm.getCurrentContext();
        Data data = (Data)ctx.getCallbackData(this);
        if (data != null)
        {
            if (data.stateVariableListeners != null)
            {
                data.stateVariableListeners = HNEventMulticaster.remove(data.stateVariableListeners, listener);
            }
            if (data.stateVariableListeners == null)
            {
                ctx.removeCallbackData(this);
                ccList = CallerContext.Multicaster.remove(ccList, ctx);
            }
        }
    }

    /**
     * Sets the subscription status of the service (evented state 
     * variables). UPnPService defaults to subscribed.
     *  
     * @param subscribed True to subscribe to evented state variable
     *                   updates, false to unsubscribe.
     *
     * @return True if the control point is registered to receive UPnP events
     *      from the service.
     *      
     *  @throws UnsupportedOperationException - if subscribed is true but the service has no 
     *      evented state variables.     
     */
     public synchronized void setSubscribedStatus(boolean doSubscribe)
     {
         if (log.isDebugEnabled())
         {
             log.debug("setSubscribedStatus() - called with subscribe: "
             + doSubscribe + ", pending subscriptons? " + m_service.isSubscriptionPending());
         }
         if (doSubscribe)
         {
             // Check if not currently subscribed and not currently pending
             if ((!m_service.isSubscribed()) && (!m_service.isSubscriptionPending()))
             {
                 if (!hasEventedStateVariable())
                 {
                     if (log.isErrorEnabled())
                     {
                         log.error("setSubscribedStatus(true) called on a service with out evented variables.");
                     }
                     throw new UnsupportedOperationException();
                 }
                 m_service.setSubscriptionPending(true);
                 subscribeToService(true);
             }
             else
             {
                 // Already subscribed 
                 if (log.isDebugEnabled())
                 {
                     log.debug("setSubscribedStatus() - already subscribed");
                 }
             }
         }
         else
         {
             // Check if currently subscribed
             if (m_service.isSubscribed())
             {
                 subscribeToService(false);
             }
             else
             {
                 // Already unsubscribed 
                 if (log.isDebugEnabled())
                 {
                     log.debug("setSubscribedStatus() - already unsubscribed");
                 }
             }
         }
     } 


    /**
     * Gets the subscription status of the service. For a 
     * <code>UPnPService</code> obtained from a 
     * <code>UPnPControlPoint</code>, returns whether this 
     * UPnPControlPoint subscribed to the 
     * service. For a <code>UPnPService</code> obtained from a 
     * <code>UPnPManagedDevice</code>, returns whether any control 
     * points are subscribed to this service. 
     *
     * @return True if the control point is registered to receive UPnP events
     *      from the service, false if not.
     */
    public boolean getSubscribedStatus()
    {
        // TODO: - made comment on OCVET-2 about relationship to UPnPManagedDevice
        return m_service.isSubscribed();
    }

    /**
     * Determines if the supplied subscription id matches SID associated with
     * this service.
     * 
     * @param sid   determines if this is subscription associated with this service
     * @return  true if SID matches, false otherwise
     */
    public boolean matchesSID(String sid)
    {
        boolean matches = false;
        if (sid.equals(m_service.getSID()))
        {
            matches = true;
        }
        return matches;
    }
    
	/**
	 * Returns the subscription ID associated with this service.
     * 
     * @return subscription ID of this service
	 */
    public String getSID()
    {
        return m_service.getSID();
    }
    
    /**
     * Gets the UPnPDevice that this service is a part of (if 
     * any).
     *  
     * @return The UPnPDevice that this UPnPService is a part 
     *      of, if any. Returns null if this service is not part of
     *      a UPnPDevice.
     */
    public UPnPClientDevice getDevice()
    {
        return m_device;
    }
    
    /**
     * Debug method to print out the arguments associated with action invocation.
     * 
     * @param action    print out args associated with this action
     * 
     * @return  string of arg names and values
     */
    private String dumpArgStr(Action action)
    {
        StringBuffer sb = new StringBuffer();
        ArgumentList argList = action.getArgumentList();
        if (argList != null)
        {
            for (int i = 0; i < argList.size(); i++)
            {
                Argument arg = (Argument)argList.get(i);
                sb.append("Arg ");
                sb.append(i+1);
                sb.append(": Name - ");
                sb.append(arg.getName());
                sb.append(", Value: ");
                sb.append(arg.getValue());
                sb.append("\n");
            }
        }
        return sb.toString();
    }



    /**
     * Call underlying cybergarage control point to either subscribe or unsubscribe
     * from service.
     * 
     * @param   doSubscribe if true, subscribe if not already subscribed.  
     *                      if false, unsubscribe if subscribed 
     */
    private void subscribeToService(final boolean doSubscribe)
    {
        // Need to perform following in System Context due to socket access
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {            
                if (doSubscribe)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("subscribeToService() - subscribing for service: "  +
                        m_service.getServiceID() + ", device: " + m_service.getDevice().getFriendlyName());
                    }    
                    if (m_controlPoint.subscribe(m_service) )
                    {                   
                        if (log.isDebugEnabled())
                        {
                            log.debug("subscribeToService() - subscription complete for service: "  +
                            m_service.getServiceID() + ", device: " + m_service.getDevice().getFriendlyName());
                        }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("subscribeToService() - failed to subscribe for service: " +
                            m_service.getServiceID() + ", device: " + m_service.getDevice().getFriendlyName());
                        }
                    }
                    m_service.setSubscriptionPending(false);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("subscribeToService() - unsubscribing for service: "  +
                        m_service.getServiceID() + ", device: " + m_service.getDevice().getFriendlyName());
                    }     
                    if( m_controlPoint.unsubscribe(m_service))
                    {                    
                        if (log.isDebugEnabled())
                        {
                            log.debug("subscribeToService() - unsubscription complete for service: "  +
                            m_service.getServiceID() + ", device: " + m_service.getDevice().getFriendlyName());
                        }
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("subscribeToService()- failed to unsubscribe for service: " + 
                            m_service.getServiceID() + ", device: " + m_service.getDevice().getFriendlyName());
                        }
                    } 
                        
                }
                
                // Notify listeners of change if subscription status and device is not destroyed
                if (!m_device.isDestroyed())
                {
                    notifyListenersSubscribed(doSubscribe);
                }        
             }
        });
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
     * Per-context global data. Remembers per-context
     * <code>UPnPStateVariableChangeListener</code>s.
     */
    private class Data implements CallbackData
    {
        public UPnPStateVariableListener stateVariableListeners;

        public void destroy(CallerContext cc)
        {
            synchronized (UPnPClientServiceImpl.this)
            {
                // Simply forget the given cc
                // No harm done if never added
                cc.removeCallbackData(UPnPClientServiceImpl.this);
                ccList = CallerContext.Multicaster.remove(ccList, cc);
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }

    /**
     * *TODO* - add javadoc
     */
    public UPnPAdvertisedStateVariable getAdvertisedStateVariable(String stateVariableName)
    {
        return getStateVariable(stateVariableName);
    }

    /**
     * *TODO* - add javadoc
     */
    public UPnPAdvertisedStateVariable[] getAdvertisedStateVariables()
    {
        return getStateVariables();
    }
}
