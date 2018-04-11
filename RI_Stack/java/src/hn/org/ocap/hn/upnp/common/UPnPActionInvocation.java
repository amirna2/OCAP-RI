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

package org.ocap.hn.upnp.common;

import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.ocap.hn.upnp.common.UPnPAction;
import java.util.Vector;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.server.UPnPActionHandler;

/**
 * This class represents a UPnP service action invocation, 
 * carrying only IN arguments and a reference to the action 
 * definition (<code>UPnPAction</code>). It is constructed by a 
 * client application and passed to a <code>UPnPService</code> 
 * via the <code>postActionInvocation</code> method in order to 
 * invoke an action on a UPnP server.
 *
 * @see UPnPClientService#postActionInvocation(UPnPActionInvocation, UPnPActionResponseHandler)
 * @see UPnPActionHandler#notifyActionReceived(UPnPActionInvocation)
 */
public class UPnPActionInvocation
{
    // Private Attributes
    private UPnPActionImpl m_action = null;
   
    /**
     * Constructs a {@code UPnPActionInvocation} that conforms to
     * the IN argument requirements of its associated {@code UPnPAction}.
     * <p>
     * This constructor ensures that the resulting action invocation
     * provides an argument value, in the proper {@code dataType} format,
     * for each of the IN arguments of the specified UPnP action.
     * <p>
     * For objects created through this constructor,
     * {@link #getArgumentNames()} will report, in order,
     * the required IN argument names of the specified {@code UPnPAction}.
     *
     * @param argVals An array of argument values corresponding,
     *                in order, to the IN arguments of {@code action}.
     * @param action The UPnP action that this action invocation relates to.
     *
     * @throws IllegalArgumentException if {@code argVals} does not conform
     * to the IN argument requirements of {@code action}.
     *
     * @throws NullPointerException if {@code action} is {@code null},
     * or {@code argVals} or any of its array elements is {@code null}.
     */
 
    public UPnPActionInvocation(String[] argVals, UPnPAction action)
    {
        // get IN arg names ..note that this assumes getArgumentNames
        // returns IN arg names in right order
        Vector inArgs = new Vector(); 
        String[] allArgs = action.getArgumentNames();
        for (int i = 0; i < allArgs.length; i++)
        {
            if (action.isInputArgument(allArgs[i]))
            {
                inArgs.add(allArgs[i]);
            }
        }
        String[] argNames =
            (String[])inArgs.toArray(new String[inArgs.size()]);
        
        // Verify action,argVals are not null
        if ((action == null) || (argVals == null))
        {
            throw new NullPointerException("Supplied action was null");
        }
        for (int i = 0; i < argVals.length; i++)
        {
            if (argVals[i] == null)
            {
               throw new NullPointerException("null argVals element index: " + i);
            }
        }
        
        m_action = (UPnPActionImpl)action;
        
        // Verify the array lengths match
        if (argNames.length != argVals.length)
        {
            // One of the args were null so length of arrays do not match
            throw new IllegalArgumentException("Argument array lengths do not match, name length: " +
                    argNames.length + ", values length: " + argVals.length);
        }
        
        if ((argNames != null) && (argVals != null))
        {
            Action cAction = m_action.getAction();
            for (int i = 0; i < argNames.length; i++)
            {
                cAction.setArgumentValue(argNames[i], argVals[i]);
            }
        }
    }

   /**
     * Gets the name of the action as specifed by the action name element
     * in the UPnP service description.  Calls {@code getAction().getName()}.
     *
     * @return the name of the action.
     *
     * @see #getAction()
     */
    public String getName()
    {
        return m_action.getName();
    }

    /**
     * Gets the argument names specified by this action invocation,
     * in the order they were specified in the constructor.
     *
     * @return The argument names of this action invocation. If no arguments
     * have been specified, returns a zero-length array.
     */
    public String[] getArgumentNames()
    {
        String argNames[] = new String[0];
        Action cAction = m_action.getAction();
        ArgumentList list = cAction.getArgumentList();
        if (list != null)
        {
            Vector v = new Vector();
            for (int i = 0; i < list.size(); i++)
            {
                Argument arg = (Argument)list.get(i);
                if (arg.isInDirection())
                {
                    v.add(i,arg.getName());
                }
            }
            argNames = new String[v.size()];
            v.copyInto(argNames);
        }
        return argNames;
    }

    /**
     * Gets the value of the specified argument.
     *
     * @param name The name of the argument.
     *
     * @return The value of the argument.
     *
     * @throws IllegalArgumentException if {@code name} does not match one
     * of the argument names specified for this action invocation.
     *
     * @see #getArgumentNames()
     */
    public String getArgumentValue(String name)
    {
        // Find the name in the array of name and get index to use
        // as look up into values array
        String value = null;
        boolean found = false;
        Action cAction = m_action.getAction();
        ArgumentList list = cAction.getArgumentList();
        if (list != null)
        {
            for (int i = 0; i < list.size(); i++)
            {
                Argument arg = (Argument)list.get(i);
                if (arg.getName().equals(name))
                {
                    value = arg.getValue();
                    found = true;
                    break;
                }
            }
        }
        if (!found)
        {
            throw new IllegalArgumentException("Name does not represent valid arg name: " +
                                                name);
        }
        return value;
    }

    /**
     * Gets the {@code UPnPAction} that this {@code UPnPActionInvocation}
     * is associated with.
     *
     * @return The {@code UPnPAction} that this action invocation is
     * associated with.
     */
    public UPnPAction getAction()
    {
        return m_action;
    }
}
