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
package org.cablelabs.impl.ocap.hn.upnp.server;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPServiceImpl;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceStateTable;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.control.ActionRequest;
import org.cybergarage.upnp.control.SubscriptionListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.cybergarage.upnp.event.Subscriber;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPAdvertisedService;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.hn.upnp.server.UPnPManagedStateVariable;
import org.ocap.hn.upnp.server.UPnPStateVariableHandler;
import org.ocap.system.MonitorAppPermission;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

public class UPnPManagedServiceImpl extends UPnPServiceImpl implements UPnPManagedService, ActionListener, SubscriptionListener
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(UPnPManagedServiceImpl.class);

    // Managed components
    private final List m_managedStateVariables = new ArrayList();

    private final String m_SCPD;

    private final UPnPManagedDevice m_managedDevice;

    // Generated properties based off of serviceType passed in.
    private Map m_properties = new LinkedHashMap();

    private UPnPActionHandler m_actionHandler = null;
    private UPnPStateVariableHandler m_stateVariableHandler = null;

    /**
     * The system context.
     */
    private static CallerContext systemContext;

    public UPnPManagedServiceImpl(UPnPManagedDevice device, String serviceType, String SCPD)
    {
        assert serviceType != null;
        assert SCPD != null;
        assert device != null;

        m_managedDevice = device;
        m_SCPD = SCPD;

        //Validate general XML structure of SCPD node
        Node node = null;
        try 
        {
            node = XMLUtil.toNode(SCPD);
        }
        catch (Exception e) 
        {
            if (log.isErrorEnabled())
            {
                log.error("XMLUtil.toNode(SCDP) threw: " + e.getClass().getName() + " - " + e.getMessage());
            }
            throw new IllegalArgumentException();
        }
        if (node == null) {
            if (log.isErrorEnabled())
            {
                log.error("XMLUtil.toNode(SCDP) returned null.");
            }
            throw new IllegalArgumentException();
        }
        
        if (!validateSCPD(node))
        {
            if (log.isErrorEnabled())
            {
                log.error("SCPD was invalid.");
            }
            throw new IllegalArgumentException("SCPD was invalid");
        }
        
        // Parse service type.  Expecting 5 elements, each separated by a colon.
        // TODO : lookup to be sure the format is a requirement UPnP requirement.
        // TODO : Service Id et. al. are being auto-generated here.  Are these good
        // values, should the spec allow for customizations?
        String[] parts = Utils.split(serviceType, ":");
        if(parts != null && parts.length == 5)
        {
            // Constructing service id domain based off of service type domain, if
            // from upnp.org remove schemas as per UPnP Device spec.
            String serviceIdDomain = "schemas-upnp-org".equals(parts[1]) ? "upnp-org" : parts[1];
            
            m_properties.put("serviceType", serviceType);
            m_properties.put("serviceId", "urn:" + serviceIdDomain + ":serviceId:" + parts[3]);
            m_properties.put("SCPDURL",     "/service/" + parts[3] + "/description.xml");
            m_properties.put("controlURL",  "/service/" + parts[3] + "/control");
            m_properties.put("eventSubURL", "/service/" + parts[3] + "/eventSubURL");
        }
    }

    // Begin UPnPManagedService Interface

    synchronized public UPnPActionHandler setActionHandler(UPnPActionHandler actionHandler)
                                throws SecurityException
    {
        // TODO : Implement with Context Sensitive Callbacks
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        UPnPActionHandler previous = m_actionHandler;

        m_actionHandler = actionHandler;

        if(previous != null)
        {
            previous.notifyActionHandlerReplaced(actionHandler);
        }

        return previous;
    }

    synchronized public UPnPActionHandler getActionHandler()
    {
        return m_actionHandler;
    }

    public void respondToQueries(boolean respond) throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        for(Iterator i = m_managedStateVariables.iterator(); i.hasNext(); )
        {
            ((UPnPManagedStateVariableImpl)i.next()).setRespondToQueries(respond);
        }
    }

    public UPnPManagedStateVariable[] getManagedStateVariables()
    {
        return (UPnPManagedStateVariable[]) m_managedStateVariables.toArray(new UPnPManagedStateVariable[m_managedStateVariables.size()]);
    }

    public UPnPManagedDevice getManagedDevice()
    {
        return m_managedDevice;
    }

    // End UPnPManagedService Interface

    public String getXMLDescription()
    {
        StringBuffer description = new StringBuffer();

        description.append("<service>\n");
        for(Iterator i = m_properties.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)i.next();
            description.append("    <" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey() + ">\n");
        }
        description.append("</service>\n");

        return description.toString();
    }

    public String getSCPD()
    {
        return m_SCPD;
    }

    public String getServiceType()
    {
        return (String)m_properties.get("serviceType");
    }
    
    public String getServiceId()
    {
        return (String)m_properties.get("serviceId");        
    }

    public boolean actionControlReceived(Action action, ActionRequest actionRequest)
    {
        // Ensure all requested arguments are in order and additional ones are ignored.
        if(!matchingArguments(action.getInputArgumentList(), actionRequest.getArgumentList()))
        {
            action.setStatus(ActionStatus.UPNP_INVALID_ARGUMENTS.getCode(),
                    ActionStatus.UPNP_INVALID_ARGUMENTS.getDescription());
            return false;
        }
        
        // TODO : Implement using Context Sensitive Callbacks
        if(m_actionHandler != null)
        {
            ArgumentList alist = action.getArgumentList();
            String[] argNames = new String[alist.size()];
            String[] argValues = new String[alist.size()];
            Vector inArgs = new Vector();
            Vector inVals = new Vector();
            int x = 0;
            for(Iterator i = alist.iterator(); i.hasNext(); )
            {
               Argument arg = (Argument)i.next();
               if(arg.isInDirection())
               {
                   inArgs.add(arg.getName());
                   inVals.add(arg.getValue());
               }
            }
       
            argNames = (String[])inArgs.toArray(new String[inArgs.size()]);
            argValues = (String[])inVals.toArray(new String[inVals.size()]);

            // TODO : Which service do we choose here?  We need network context in API.
            UPnPActionImpl uAction = new UPnPActionImpl(action, this);
            uAction.setActionRequest(actionRequest);
            UPnPActionInvocation reqAI = new UPnPActionInvocation(argValues, uAction);
            
            UPnPResponse response = m_actionHandler.notifyActionReceived(reqAI);
            if (response == null)
            {
                return false;
            }

            // If this a UPnPActionResponse vs. UPnPErrorResponse, then set out argument values
            if(response instanceof UPnPActionResponse)
            {
                argNames = ((UPnPActionResponse)response).getArgumentNames();
                for(int i = 0; i < argNames.length; ++i)
                {
                    action.setArgumentValue(argNames[i], ((UPnPActionResponse)response).getArgumentValue(argNames[i]));
                }
            }
            
            if(response instanceof UPnPErrorResponse)
            {
                action.setStatus(response.getHTTPResponseCode(), 
                        ((UPnPErrorResponse)response).getErrorDescription());
                return false;                
            }
            
            action.setStatus(response.getHTTPResponseCode());
            return true;
        }
        return false;
    }

    public void addManagedStateVariable(UPnPManagedStateVariable variable)
    {
        m_managedStateVariables.add(variable);
    }

    protected void refreshService(Service service) throws InvalidDescriptionException
    {
        assert service != null;
      
        // Get state from previous service if there is one 
        Service previousService = getService();
        ServiceStateTable oldstateVars = null;
        if (previousService != null)
        {
            oldstateVars = previousService.getServiceStateTable();
        }

        boolean state = service.loadSCPD(m_SCPD);
        if(state)
        {
            setService(service);
            service.setActionListener(this);
            service.setSubscriptionListener(this);
            
            // Need to sync the managed state variables with state variables defined in the SCPD.
            // May have added or removed state variables, all others have changed and need to be
            // reassigned to managed state variables that themselves may be referenced in applications.
            ServiceStateTable stateVars = service.getServiceStateTable();
            List removeStateVariables = new ArrayList(m_managedStateVariables);
            
            for(Iterator stateVarIter = stateVars.iterator(); stateVarIter.hasNext(); )
            {
                StateVariable sv = (StateVariable)stateVarIter.next();
                
                boolean found = false;
                for(Iterator mStateVarIter = m_managedStateVariables.iterator(); mStateVarIter.hasNext(); )
                {
                    UPnPManagedStateVariableImpl msv = (UPnPManagedStateVariableImpl)mStateVarIter.next();
                    // If we already have variable, reset underlying state variable.
                    if(msv != null && msv.getName().equals(sv.getName()))
                    {
                        msv.setStateVariable(sv);
                        //sync up values if previous service existed 
                        if (oldstateVars != null)
                        {
                            for(Iterator oldstateVarIter = oldstateVars.iterator();
                                oldstateVarIter.hasNext(); )
                            {
                                StateVariable oldsv = (StateVariable)oldstateVarIter.next();
                                if (sv.getName().equals(oldsv.getName()))
                                {
                                    try
                                    {
                                        // only set if theres a previous value
                                        if ((oldsv.getValue() != null) &&
                                            (oldsv.getValue().length() != 0))
                                        {
                                            msv.setValue(oldsv.getValue());
                                        }
                                    }
                                    catch(Exception e)
                                    {
                                        if (log.isWarnEnabled())
                                        {
                                            log.warn("Warning couldn't set value " +
                                                oldsv.getValue() + " on " + sv.getName());
                                        }
                                    }
                                }
                            }
                        } 
                        // Don't remove this state variable, it is being used.
                        removeStateVariables.remove(msv);
                        found = true;
                        break;
                    }
                }
                
                // Did not find a pre-existing managed state variable, so create a new one.
                if(!found)
                {
                    addManagedStateVariable(new UPnPManagedStateVariableImpl(sv, this));                    
                }
            }
            
            // Remove any remaining state variables no longer defined for this service.
            for(Iterator mStateVarIter = removeStateVariables.iterator(); mStateVarIter.hasNext(); )
            {
                UPnPManagedStateVariable msv = (UPnPManagedStateVariable)mStateVarIter.next();
                m_managedStateVariables.remove(msv);
            }

        }
        else
        {
            throw new IllegalArgumentException("Failed to load SCPD for " + service.getServiceType());
        }
    }
    
    /**
     * Implements interface for added Cybergarage callbacks.  Current specs requires the calls
     * to go to all variables, though the subscribe/unsubscribe is at the service level.
     */
    public synchronized void subscribeReceived(Subscriber sub)
    {
        if (log.isDebugEnabled())
        {
            log.debug("subscribeReceived() - for service: " + getServiceType());
        }
        if(m_stateVariableHandler != null)
        {
            final UPnPManagedService service = this;
            getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    m_stateVariableHandler.notifySubscribed(service);
                }
            });
        }
    }

    public synchronized void unsubscribeRecieved(Subscriber sub, int remainingSubs)
    {
        if(m_stateVariableHandler != null)
        {
            final UPnPManagedService service = this;
            final int subs = remainingSubs;
            getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    m_stateVariableHandler.notifyUnsubscribed(service, subs);
                }
            });
        }
    }

    private static synchronized CallerContext getSystemContext()
    {
        if (systemContext == null)
        {
            CallerContextManager ccm = (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);
            systemContext = ccm.getSystemContext();
        }

        return systemContext;
    }

    public UPnPAdvertisedService[] getAdvertisedServices()
    {
        // Return empty array if not advertised.
        if(!getDevice().isAlive())
        {
            return new UPnPAdvertisedService[0];
        }
        
        InetAddress[] inets = getDevice().getInetAddresses();
        Set services = new HashSet();
        for(int i = 0; i < inets.length; ++i)
        {
            services.add(new UPnPAdvertisedServiceImpl(getService(), inets[i]));
        }
        return (UPnPAdvertisedService[]) services.toArray(new UPnPAdvertisedService[services.size()]);
    }

    public UPnPManagedStateVariable[] getStateVariables()
    {
        return (UPnPManagedStateVariable[]) m_managedStateVariables.toArray(new UPnPManagedStateVariable[m_managedStateVariables.size()]);        
    }

    // *TODO* - this would be a nice method to add to the spec
    public UPnPManagedStateVariable getManagedStateVariable(String name)
    {
        UPnPManagedStateVariable var = null;
        for (int i = 0; i < m_managedStateVariables.size(); i++)
        {
            if (((UPnPManagedStateVariable)m_managedStateVariables.get(i)).getName().equalsIgnoreCase(name))
            {
                var = (UPnPManagedStateVariable)m_managedStateVariables.get(i);
                break;
            }
        }
        return var;        
    }

    public UPnPManagedDevice getDevice()
    {
        return m_managedDevice;
    }

    public UPnPStateVariableHandler setHandler(UPnPStateVariableHandler handler)
    {
        UPnPStateVariableHandler old = m_stateVariableHandler;
        m_stateVariableHandler = handler; 
        return old;
    }

    public boolean getSubscribedStatus()
    {
        boolean retVal = false;
        if (getService().getSubscriberList().size() > 0)
        {
            retVal = true;
        } 
        return retVal;
    }
    
    protected UPnPStateVariableHandler getHandler()
    {
        return m_stateVariableHandler;
    }
    
    private boolean validateSCPD(Node node) 
    {
        boolean hasSpecVersion = false;
        boolean hasServiveStateTable = false;
        boolean hasActionList = false;
        if ("#document".equals(node.getNodeName())) {
            node = node.getChildNodes().item(0);
        }
        if (!"scpd".equals(node.getNodeName())) 
        {
            if (log.isErrorEnabled())
            {
                log.error("Root element was not <scpd ...>: " + node.getNodeName());
            }
            return false;
        }
        else
        {
            NamedNodeMap nMap = node.getAttributes();
            Node attribute = nMap.getNamedItem("xmlns");
            if (!"urn:schemas-upnp-org:service-1-0".equals(attribute.getNodeValue()))
            {
                if (log.isErrorEnabled())
                {
                    log.error("scpd element contained invalid xmlns attribute: " + attribute.getNodeValue());
                }
                return false;
            }
        }

        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        for(int x = 0; x < childNodes.getLength(); x++) 
        {
            childNode = childNodes.item(x);
            if ("specVersion".equals(childNode.getNodeName()) && !hasSpecVersion) 
            {
                hasSpecVersion = true;
                if (!validateSpecVersion(childNode)) 
                {
                    return false;
                }
            }
            else if ("actionList".equals(childNode.getNodeName()) && !hasActionList) 
            {
                hasActionList = true;
                if (!validateActionList(childNode)) 
                {
                    return false;
                }
            }
            else if ("serviceStateTable".equals(childNode.getNodeName()) && !hasServiveStateTable) 
            {
                hasServiveStateTable = true;
                if (!validateServiceStateTable(childNode)) 
                {
                    return false;
                }
            }
            else if ("#text".equals(childNode.getNodeName())) 
            {
              //ignore whitespace elements: #text
            }
            else {
                if (log.isErrorEnabled())
                {
                    log.error("<scpd> contained invalid or duplicate sub-element: " + childNode.getNodeName());
                }
                return false;
            }  
        }
        if (!hasSpecVersion || !hasServiveStateTable) {
            if (log.isErrorEnabled())
            {
                log.error("<scpd> block missing <specVersion> and/or <serviceStateTable> sub-elements in SCPD.");
            }
            return false;
        }
        return true;
    }

    private boolean validateSpecVersion(Node node) 
    {
        boolean hasMajor = false;
        boolean hasMinor = false;
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        for(int x = 0; x < childNodes.getLength(); x++)
        {
            childNode = childNodes.item(x);
            if ("major".equals(childNode.getNodeName()) && !hasMajor) 
            {
                hasMajor = true;
            }
            else if ("minor".equals(childNode.getNodeName()) && !hasMinor) 
            {
                hasMinor = true;
            } 
            else if ("#text".equals(childNode.getNodeName())) 
            {
              //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Invalid or duplicate element in <specVersion> block of SCPD: " + childNode.getNodeName());
                }
                return false;
            }
        }
        if (!hasMajor || !hasMinor) 
        {
            if (log.isErrorEnabled())
            {
                log.error("<specVersion> missing <major> and/or <minor> sub-elements in SCPD.");
            }
            return false;
        }     
        return true;
    }
        
    private boolean validateActionList(Node node) 
    {
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        if (childNodes.getLength() == 0) 
        {
            if (log.isErrorEnabled())
            {
                log.error("Empty <actionList> block in SCPD.");
            }
            return false;
        }
        
        //Validate action elements
        // "Required. Repeat once for each action defined by a UPnP Forum working committee."
        for (int x = 0; x < childNodes.getLength(); x++) 
        {
            childNode = childNodes.item(x);
            if ("action".equals(childNode.getNodeName())) 
            {
                if (!validateAction(childNode))
                {
                    return false;
                }
            }
            else if ("#text".equals(childNode.getNodeName())) 
            {
                //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("<actionList> block in SCPD contained an invalid sub-element: " + childNode.getNodeName());
                }
                return false;
            }   
        }
        return true;
    }
    
    private boolean validateServiceStateTable(Node node) 
    {
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        if (childNodes.getLength() == 0) 
        {
            if (log.isErrorEnabled())
            {
                log.error("<serviceStateTable> block in SCPD contained an no sub-elements.");
            }
            return false;
        }
        for (int x = 0; x < childNodes.getLength(); x++) 
        {
            childNode = childNodes.item(x);
            if ("stateVariable".equals(childNode.getNodeName())) 
            {
                if (!validateStateVariable(childNode))
                {
                    return false;
                }
            }
            else if ("#text".equals(childNode.getNodeName())) 
            {
                //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("<serviceStateTable> block in SCPD contained a direct sub-element other than <stateVariable>: " + childNode.getNodeName());
                }
                return false;
            }    
        }
        return true;
    }      
    private boolean validateArgumentList(Node node) 
    {
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        
        //Validate argumentList required sub-elements.
        for(int idx = 0; idx < childNodes.getLength(); idx++)
        {
            childNode = childNodes.item(idx);
            if ("argument".equals(childNode.getNodeName())) 
            {
                if (!validateArgument(childNode)) {
                    return false;
                }
            }
            else if ("#text".equals(childNode.getNodeName())) 
            {
              //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Invalid element in <argumentList> block of SCPD: " + childNode.getNodeName());
                }
                return false;
            }
        }
        return true;
    }
    
    private boolean validateAction(Node node) 
    {
        boolean hasName = false;
        boolean hasArgumentList = false;
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        for (int x = 0; x < childNodes.getLength(); x++)
        {
            childNode = childNodes.item(x);
            if ("name".equals(childNode.getNodeName()) && !hasName) //name: required
            {
                hasName = true;
            }
            else if ("argumentList".equals(childNode.getNodeName()) && !hasArgumentList) //argumentList: "Required if and only if parameters are defined for action. (Each action may have >= 0 parameters.)"
            {
                hasArgumentList = true;
                if (!validateArgumentList(childNode)) 
                {
                    return false;
                }
            }
            else if ("#text".equals(childNode.getNodeName())) 
            {
              //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Invalid or duplicate element in <action> block of SCPD: " + childNode.getNodeName());
                }
                return false;
            }
        }
        if (!hasName)
        {
            if (log.isErrorEnabled())
            {
                log.error("<action> block missing <name> sub-element in SCPD.");
            }
            return false;
        }
        return true;
    }
    
    private boolean validateArgument(Node node)  {
        boolean hasName = false;
        boolean hasDirection = false;
        boolean hasRetVal = false;
        boolean hasRelatedStVar = false;
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        for(int x = 0; x < childNodes.getLength(); x++)
        {
            childNode = childNodes.item(x);
            if ("name".equals(childNode.getNodeName()) && !hasName) 
            {
                hasName = true;
            }
            else if ("direction".equals(childNode.getNodeName()) && !hasDirection) 
            {
                hasDirection = true;
            }
            else if ("retVal".equals(childNode.getNodeName()) && !hasRetVal) 
            {
                hasRetVal = true;
            }
            else if ("relatedStateVariable".equals(childNode.getNodeName()) && !hasRelatedStVar) 
            {
                hasRelatedStVar = true;
            }
            else if ("#text".equals(childNode.getNodeName())) 
            {
              //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Invalid element in <argument> block of SCPD: " + childNode.getNodeName());
                }
                return false;
            }
        }
        if (!hasName || !hasDirection || !hasRelatedStVar) 
        {
            if (log.isErrorEnabled())
            {
                log.error("<argument> block missing <name> and/or <direction> and/or <relatedStateVariable> sub-elements in SCPD.");
            }
            return false;
        }
        return true;
    }
    
    private boolean validateStateVariable(Node node) {
        boolean hasDataType = false;
        boolean hasName = false;
        boolean hasAllowedValueList = false;
        boolean hasAllowedValueRange = false;
        String dataType = null;
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;                   
        for (int x = 0; x < childNodes.getLength(); x++) 
        {
            childNode = childNodes.item(x);
            if ("name".equals(childNode.getNodeName()) && !hasName) //name: required
            {
                hasName = true;
            }
            else if ("dataType".equals(childNode.getNodeName()) && !hasDataType) //dataType: required
            {
                dataType = childNode.getTextContent();
                if (!Utils.isUDADataType(dataType)) 
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("<dataType> value is invalid: " + dataType);
                    }
                    return false;
                }
                hasDataType = true;
            }
            else if ("allowedValueList".equals(childNode.getNodeName()) && !hasAllowedValueList) //allowedValueList: recommended
            {
                hasAllowedValueList = true;
                if (!validateAllowedValueList(childNode)) 
                {
                    return false;
                }  
            }
            else if ("allowedValueRange".equals(childNode.getNodeName()) && !hasAllowedValueRange) //allowedValueRange: recommended
            {
                hasAllowedValueRange = true;
                if (!validateAllowedValueRange(childNode)) 
                {
                    return false;
                }
            }         
        }
        
        //TODO : validate that defaultValue is valid depending on the dataType and that it adheres
        //       to allowedValueList or allowedValueRange
        
        if (!hasDataType || !hasName) 
        {
            if (log.isErrorEnabled())
            {
                log.error("<stateVariable> block missing <name> and/or <dataType> sub-elements in SCPD.");
            }
            return false;
        }
        return true;
    }
    
    private boolean validateAllowedValueList(Node node)
    {
        NodeList childNodes = node.getChildNodes();
        Node childNode = null;
        if (childNodes.getLength() == 0) 
        {
            if (log.isErrorEnabled())
            {
                log.error("SCPD contained a <allowedValueList> block with no sub-elements.");
            }
            return false;
        }
        for (int x = 0; x < childNodes.getLength(); x++) {
            childNode = childNodes.item(x);
            if (("allowedValue".equals(childNode.getNodeName()))) //allowedValue: required
            {
              //TODO : implement dataType validation of the allowed value
            }
            else if ("#text".equals(childNode.getNodeName())) 
            {
              //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Invalid element in <allowedValueList> block of SCPD: " + childNode.getNodeName());
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if all a1 arguments are found in a2 in the same position.
     * Should a2 contain extra arguments, these will be ignored
     * @param a1 - the required arguments
     * @param a2 - the requested arguments
     * @return - true when a2 contains all required arguments
     */
    private boolean matchingArguments(ArgumentList a1, ArgumentList a2)
    {
        // Precondition is that both lists are non-null.
        if(a1 == null || a2 == null)
        {
            return false;
        }
        
        for(int i = 0; i < a1.size(); i++)
        {
            if(a1.get(i) != null)
            {
                final String arg1Name = ((Argument)a1.get(i)).getName();
                if (a2.size() > i && a2.get(i) != null)
                {
                    final String arg2Name = ((Argument)a2.get(i)).getName();
                    if (!arg1Name.equals(arg2Name))
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }
            else
            {
                return false;
            }
        }
        // All is good
        return true;
    }
    
    private boolean validateAllowedValueRange(Node node)
    {
        boolean hasRangeMinimum = false;
        boolean hasRangeMaximum = false;
        boolean hasStep = false;
        NodeList childNodes = node.getChildNodes();
        Node childNode = null; 
        for (int x = 0; x < childNodes.getLength(); x++) 
        {
            childNode = childNodes.item(x);           
            if ("minimum".equals(childNode.getNodeName()) && !hasRangeMinimum) //minimum: required
            {
                //TODO : implement dataType validation.
                hasRangeMinimum = true;
            } 
            else if ("maximum".equals(childNode.getNodeName()) && !hasRangeMaximum) //maximum: required
            {
                //TODO : implement dataType validation.
                hasRangeMaximum = true;
            } 
            else if ("step".equals(childNode.getNodeName()) && !hasStep) //step: recommended
            {
                hasStep = true;
            } 
            else if ("#text".equals(childNode.getNodeName())) 
            {
              //ignore whitespace elements: #text
            }
            else
            {
                if (log.isErrorEnabled())
                {
                    log.error("Invalid or duplicate element in <allowedValueRange> block of SCPD: " + childNode.getNodeName());
                }
                return false;
            }
        }
        if (!hasRangeMinimum || !hasRangeMaximum) 
        {
            if (log.isErrorEnabled())
            {
                log.error("<stateVariable> block missing <name> and/or <dataType> sub-elements in SCPD.");
            }
            return false;
        }
        return true;
    }
}
