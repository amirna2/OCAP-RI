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

package org.cablelabs.impl.ocap.hn.upnp.common;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.NetworkInterfaceImpl;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.cybergarage.http.HTTP;
import org.cybergarage.http.HTTPHeader;
import org.cybergarage.http.HTTPRequest;
import org.cybergarage.http.HTTPResponse;
import org.cybergarage.http.Parameter;
import org.cybergarage.http.ParameterList;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.control.ActionRequest;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPService;
import org.ocap.hn.upnp.common.UPnPStateVariable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPStateVariableImpl;

/**
 * This class represents a UPnP service action, parsed from the UPnP service description XML. 
 * It contains both IN and OUT argument descriptions, but does not 
 * carry any values. 
 */
public class UPnPActionImpl implements UPnPAction
{
    // PRIVATE ATTRIBUTES
    
    // Cybergarage representation of a UPnP Action
    protected final Action m_action;
    
    // Service this action is associated with
    private final UPnPService m_service;
    
    private Map m_argVarMap = null;

    private static final Logger log = Logger.getLogger(UPnPActionImpl.class);

    /**
     * Construct an object of this class.
     *
     * @param action    Cybergarage action which this class wraps
     */
    public UPnPActionImpl(Action action, UPnPService service)
    {
        assert action != null;
        m_action = action;
        assert service != null;
        m_service = service;
    }
    
    /**
     * Gets the name of the action from the action name element in the
     * UPnP service description.
     *
     * @return name of the action.
     */
    public String getName()
    {
        return m_action.getName();
    }

    /**
     * Gets the <code>UPnPService</code> that this 
     * <code>UPnPAction</code> is associated with. 
     *
     * @return The <code>UPnPService</code> that this action is 
     *         associated with.
     */

    public UPnPService getService()
    {
        return m_service;
    }

    /**
     * Gets the action argument names from the action description in the UPnP
     * service description.
     *
     * @return The IN and OUT argument names for this action, in the order
     * specified by the UPnP service description defining this action.
     * If the action has no arguments, returns a zero length array.
     */
    public String[] getArgumentNames()
    {
        // Get the service description
        Document doc = ((UPnPServiceImpl)m_service).getXML();
        
        // Get all the action from the service description
        NodeList actionNodes = doc.getElementsByTagName("action");
         
        // Search for this action in the node list by matching name
        Node actionNode = null;
        for (int i = 0; i < actionNodes.getLength(); i++)
        {
            Node curNode = (Node)actionNodes.item(i);

            // Get the name of this action
            Node nameNode = XMLUtil.getNamedChild(curNode, "name");
            if (nameNode != null)
            {
                String name = nameNode.getTextContent();
                if ((name != null) && (name.equalsIgnoreCase(getName())))
                {
                    actionNode = curNode;
                    break;
                }
            }
        }

        // Create the ordered list of argument names to return
        String argNames[] = null;
        if (actionNode != null)
        {
            NodeList argNodes = ((Element)actionNode).getElementsByTagName("argument");
            argNames = new String[argNodes.getLength()];
            for (int i = 0; i < argNodes.getLength(); i++)
            {
                Node argNode = argNodes.item(i);
                Node nameNode = XMLUtil.getNamedChild(argNode, "name");
                argNames[i] = nameNode.getTextContent();
            }            
        }
        return argNames;
    }

    /**
     * Gets the direction of an argument.
     *
     * @param name Name of the argument.
     *
     * @return True if the argument is an input argument.
     *
     * @throws IllegalArgumentException if the name does not represent a
     *      valid argument name for the action.
     */
    public boolean isInputArgument(String name)
    {
        boolean isArg = false;
        
        // Get the input arguments for this action and see if supplied name is valid
        String args[] = getArgumentStrs(m_action, 0, 0);
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(name))
            {
                isArg = true;
                break;
            }
        }
        
        // Supplied arg is not a valid input arg, see if it is a valid out arg
        if (!isArg)
        {
            boolean isOutArg = false;

            // Determine if this is an output argument
            args = getArgumentStrs(m_action, 1, 0);
            for (int i = 0; i < args.length; i++)
            {
                if (args[i].equals(name))
                {
                    isOutArg = true;
                    break;
                }
            }    
            if (!isOutArg)
            {
                throw new IllegalArgumentException("Arg name does not represent valid arg: " +
                        name);
            }
        }
        return isArg;
    }

    /**
     * Returns the associated cybergarge action which this object wraps.
     * 
     * @return  cybergarage action
     */
    public Action getAction()
    {
        return m_action;
    }

    /**
     * Determines whether the specified argument is flagged as a 
     * return value in the service description 
     *
     * @param name Name of the argument.
     *
     * @return true if the argument is flagged as a retval.
     *
     * @throws IllegalArgumentException if the name does not represent a
     *      valid argument name for the action.
     */
    public boolean isRetval(String argName)
    {
        boolean isRetVal = false;
        
        // Get output arguments for this action and see if supplied name is valid
        String args[] = getArgumentStrs(m_action, 1, 0);
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equals(argName))
            {
                isRetVal = true;
                break;
            }
        }
        
        // Supplied arg is not a valid input arg, see if it is a valid out arg
        if (!isRetVal)
        {
            boolean isInArg = false;

            // Determine if this is an output argument
            args = getArgumentStrs(m_action, 0, 0);
            for (int i = 0; i < args.length; i++)
            {
                if (args[i].equals(argName))
                {
                    isInArg = true;
                    break;
                }
            }    
            if (!isInArg)
            {
                throw new IllegalArgumentException("Arg name does not represent valid arg: " +
                        argName);
            }
        }
        return isRetVal;
    }
   
    /**
     * Gets the <code>UPnPStateVariable</code> associated with the 
     * specified argument name. 
     *
     * @param name Name of the argument.
     *
     * @return The <code>UPnPStateVariable</code> associated with 
     *         the specified argument name.
     *
     * @throws IllegalArgumentException if the name does not represent a
     *      valid argument name for the action.
     */
    public UPnPStateVariable getRelatedStateVariable(String argName)
    {
        buildArgVarMap();
        UPnPStateVariable var = null;
        if (m_argVarMap.containsKey(argName))
        {
            var = (UPnPStateVariable)m_argVarMap.get(argName);
        }
        else
        {
            throw new IllegalArgumentException("No argument named: " + argName);
        }
        return var;
    }
    
    /**
     * Sets the argument list associated with this action
     * 
     * @param argList   list of arguments which consists of arg name and value strings
     */
    public void setArgumentList(ArgumentList argList)
    {
        m_action.setArgumentValues(argList);
    }
    
    // *TODO* - end of methods
    /**
     * Return the inet address associated with underlying action.
     * 
     * @return  inet associated with underlying action
     */
    public InetAddress getInetAddress()
    {
        return m_action.getInetAddress();
    }
    
    /**
     * Builds a set of argument names and their associated related state 
     * variable.
     */
    private void buildArgVarMap()
    {
        if (m_argVarMap == null)
        {
            m_argVarMap = new HashMap();
            ArgumentList list = m_action.getArgumentList();
            for (int i = 0; i < list.size(); i++)
            {
                Argument arg = (Argument)list.get(i);
                StateVariable stateVar = arg.getRelatedStateVariable();
                UPnPStateVariableImpl var = null;
                if (stateVar != null)
                {
                    var = new UPnPStateVariableImpl(stateVar);
                }
                m_argVarMap.put(arg.getName(), var);
            }        
        }
    }
    
    /**
     * Get the associated argument name or value for in or out arguments from
     * underlying cybergarage action
     * 
     * @param   action      cybergarage action to retrieve arguments from
     * @param   direction   if equals 0, direction of args is IN, otherwise OUT
     * @param   type        if equals 0, type is name, otherwise value
     * @return  array of strings either in or out, argument name or values
     */
    public static String[] getArgumentStrs(Action action, int direction, int type)
    {
        // Get argument list from cybergarage action
        ArgumentList list = action.getArgumentList();
        String argStrs[] = new String[0];
        if (list != null)
        {
            Vector v = new Vector();
            int idx = 0;
            for (int i = 0; i < list.size(); i++)
            {
                Argument arg = (Argument)list.get(i);
                if (((direction == 0) && (arg.isInDirection())) ||
                        ((direction == 1) && (arg.isOutDirection())))
                {
                    if (type == 0)
                    {
                        v.add(idx, arg.getName());
                        idx++;
                    }
                    else
                    {
                        v.add(idx, arg.getValue());
                        idx++;
                    }
                }
            }
            argStrs = new String[v.size()];
            v.copyInto(argStrs);
        }
        return argStrs;
    }    
    
    // TEST CODE ONLY //
    /*
    public static String[] testGetArgumentNames()
    {
        // paste in code from getArgumentNames() here to test
    }
    
    private static Document getTestDoc()
    {
        String xmlStr = "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">" +
        "<specVersion><major>1</major><minor>0</minor></specVersion>" +
        "<actionList>\n" +
        "   <action>\n" +
        "       <name>actionName1</name>" +
        "       <argumentList>" +
        "           <argument>" +
        "               <name>formalParameterName1-1</name>" +
        "               <direction>in</direction>" +
        "               <retval />" +
        "               <relatedStateVariable>stateVariableName1</relatedStateVariable>" +
        "           </argument>" +
        "           <argument>" +
        "               <name>formalParameterName1-2</name>" +
        "               <direction>out</direction>" +
        "               <retval />" +
        "               <relatedStateVariable>stateVariableName2</relatedStateVariable>" +
        "           </argument>" +
        "       </argumentList>" +
        "   </action>" +
        "   <action>\n" +
        "       <name>actionName2</name>" +
        "       <argumentList>" +
        "           <argument>" +
        "               <name>formalParameterName2-1</name>" +
        "               <direction>in</direction>" +
        "               <retval />" +
        "               <relatedStateVariable>stateVariableName2-1</relatedStateVariable>" +
        "           </argument>" +
        "           <argument>" +
        "               <name>formalParameterName2-2</name>" +
        "               <direction>out</direction>" +
        "               <retval />" +
        "               <relatedStateVariable>stateVariableName2-2</relatedStateVariable>" +
        "           </argument>" +
        "       </argumentList>" +
        "   </action>" +
        "</actionList>" +
        "<serviceStateTable>" +
        "   <stateVariable sendEvents=\"yes\">" +
        "       <name>variableName1</name>" +
        "       <dataType>variable data type</dataType>" +
        "       <defaultValue>default value</defaultValue>" +
        "       <allowedValueList>" +
        "           <allowedValue>enumerated value</allowedValue>" +
        "       </allowedValueList>" +
        "   </stateVariable>" +
        "   <stateVariable sendEvents=\"yes\">" +
        "       <name>variableName2</name>" +
        "       <dataType>variable data type</dataType>" +
        "       <defaultValue>default value</defaultValue>" +
        "       <allowedValueRange>" +
        "           <minimum>minimum value</minimum>" +
        "           <maximum>maximum value</maximum>" +
        "           <step>increment value</step>" +
        "       </allowedValueRange>" +
        "   </stateVariable>" +
        "</serviceStateTable>" +
        "</scpd>";

        Document doc = null;
        try
        {
            doc = XMLUtil.toNode(xmlStr);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return doc;
    }
    */
    
    //  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // *TODO* - Starting here to remainder of file is a bunch of code that needs a real home.
    //
    // The action request related code is to support browse and search actions which need 
   
    //  This was code that was in server/facade CConnection / IConnection.
  
    // *TODO* - action request stored to support browse & search responses
    // must be a simpler way to get address request was received on
    private ActionRequest m_actionRequest;

    public void setActionRequest(ActionRequest request)
    {
        m_actionRequest = request;
    }

    private HTTPRequest m_httpRequest;

    private ConnectionParameterList m_connParamList = null;

    // temp constructor to maintain original request
    public UPnPActionImpl(HTTPRequest httpRequest)
    {
        //m_action = new Action(null, actionRequest.getActionNode());
        m_action = null;
        m_service = null;
        m_httpRequest = httpRequest;
    }
   
    public String getLocalAddress()
    {
        if(m_httpRequest != null)
        {
            return m_httpRequest.getLocalAddress();
        }
        else if(m_actionRequest != null)
        {
            return m_actionRequest.getLocalAddress();
        }
        
        return "";        
    }    

    public int getLocalPort()
    {
        if(m_httpRequest != null)
        {
            return m_httpRequest.getLocalPort();
        }
        else if(m_action != null
                && m_action.getService() != null
                && m_action.getService().getDevice() != null)
        {
            return m_action.getService().getDevice().getHTTPPort();
        }

        return -1;
    }
    
    public Socket getSocket()
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.getSocket().getSocket();
        }
        else if (m_actionRequest != null)
        {
            return m_actionRequest.getSocket().getSocket();
        }
        else
        {
            return null;
        }
    }

    public NetworkInterface getNetworkInterface()
    {
        final InetAddress localAddress = getSocket().getLocalAddress();
        
        // Create a NI who's getInetAddress() returns the socket the request was received on
        return new NetworkInterfaceImpl(localAddress);
    }
    
    public String getHeader()
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.getHeader();
        }
        else if (m_actionRequest != null)
        {
            return m_actionRequest.getHeader();
        }
        else
        {
            return null;
        }
    }

    public String getHeaderValue(String name)
    {
        HTTPRequest request;
        
        if (m_httpRequest != null)
        {
            request = m_httpRequest;
        }
        else if (m_actionRequest != null)
        {
            request = m_actionRequest;
        }
        else
        {
            return null;
        }

        String result = request.getHeaderValue(name);
        if (result == null || result.trim().equals(""))
        {
            return null;
        }
        return result;
    }

    public String getHTTPVersion()
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.getHTTPVersion();
        }
        else if (m_actionRequest != null)
        {
            return m_actionRequest.getHTTPVersion();
        }
        else
        {
            return null;
        }
    }

    public InetAddress getRequestInetAddress()
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.getInetAddress();
        }
        else if (m_actionRequest != null)
        {
            return m_actionRequest.getInetAddress();
        }
        else
        {
            return null;
        }
    }

    public void returnResponse(int response)
    {
        if (m_httpRequest != null)
        {
            m_httpRequest.returnResponse(response);
        }
        else if (m_actionRequest != null)
        {
            m_actionRequest.returnResponse(response);
        }
    }

    private URL m_url = null;
    
    public URL getURL()
    {
        HTTPRequest request;
        
        if (m_httpRequest != null)
        {
            request = m_httpRequest;
        }
        else if (m_actionRequest != null)
        {
            request = m_actionRequest;
        }
        else
        {
            return null;
        }
        
        if (m_url == null)
        {
            String uriString = request.getURI();
            try
            {
                if (uriString.charAt(0) != '/')
                { // URI is absolute
                    m_url = new URL(uriString);
                }
                else
                { // URI is server-relative
                    m_url = new URL("http://" + getHeaderValue("host") + uriString); 
                }
            }
            catch (MalformedURLException murle)
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("Malformed URL in getURL: " + uriString + ": " + murle.getMessage());
                }
                m_url = null;
            }
        }
        
        return m_url;
    }
    
    public boolean returnBadRequest()
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.returnBadRequest();
        }
        else 
        {
            return m_actionRequest.returnBadRequest();
        }
    }

    public boolean isGetRequest()
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.isMethod(HTTP.GET);
        }
        else 
        {
            return m_actionRequest.isMethod(HTTP.GET);
        }
    }

    public boolean isHeadRequest()
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.isMethod(HTTP.HEAD);
        }
        else 
        {
            return m_actionRequest.isMethod(HTTP.HEAD);
        }
    }

    public ConnectionParameterList getParameterList()
    {
        if (m_httpRequest != null)
        {
            return new ConnectionParameterList(m_httpRequest.getParameterList());
        }
        else if (m_actionRequest != null)
        {
            return new ConnectionParameterList(m_actionRequest.getParameterList());
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Return the request line plus the headers in a String array
     */
    public String [] getRequestStrings()
    {
        HTTPRequest request;
        
        if (m_httpRequest != null)
        {
            request = m_httpRequest;
        }
        else if (m_actionRequest != null)
        {
            request = m_actionRequest;
        }
        else
        {
            return new String[0];
        }
        
        final int numHeaders = request.getNHeaders();
        
        String [] headerVals = new String[numHeaders+1];
        headerVals[0] = request.getFirstLineString();
        for (int i=0; i<numHeaders; i++)
        {
            final HTTPHeader curHeader = request.getHeader(i);
            headerVals[i+1] = curHeader.getName() + ':' + curHeader.getValue();
        }

        return headerVals;
    }
    
    
    public boolean post(HTTPResponse response)
    {
        if (m_httpRequest != null)
        {
            return m_httpRequest.post(response);
        }
        else if (m_actionRequest != null)
        {
            return m_actionRequest.post(response);
        }
        else
        {
            return false;
        }
    }

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("UPnPActionImpl");
        if (m_action != null)
        {
            buffer.append(" action: ");
            buffer.append(m_action);
        }
        if (m_httpRequest != null)
        {
            buffer.append(" http request: ");
            buffer.append(m_httpRequest.getMethod());
            buffer.append(" ");
            buffer.append(m_httpRequest.getURI());
            buffer.append(", headers: ");
            for (int i=0;i<m_httpRequest.getNHeaders();i++)
            {
                buffer.append("[");
                buffer.append(m_httpRequest.getHeader(i).getName());
                buffer.append(": ");
                buffer.append(m_httpRequest.getHeader(i).getValue());
                buffer.append("]");
            }
            Socket socket = getSocket();
            buffer.append(", port: ");
            buffer.append(socket.getPort());
        }
        if (m_actionRequest != null)
        {
            buffer.append(" actionRequest: ");
            buffer.append(m_actionRequest);
        }
        return buffer.toString();
    }
    
    public class ConnectionParameterList
    {
        ParameterList m_pl = null;

        public ConnectionParameterList(ParameterList list)
        {
            m_pl = list;
        }

        public String getValue(String name)
        {
            return m_pl.getValue(name);
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer();
            for (int ii = 0; ii < m_pl.size(); ii++)
            {
                Parameter param = m_pl.getParameter(ii);
                buffer.append("[" + param.getName() + "] = " + param.getValue());
            }
            return buffer.toString();
        }
    }
}
