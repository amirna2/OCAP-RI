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

import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPActionImpl;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.UPnPStatus;
import java.util.Vector;

/**
 * The class represents a response to a successfully completed 
 * UPnP action. It carries only the OUT arguments from the 
 * action. Instances of this class are constructed by the 
 * <code>UPnPActionHandler</code> on a UPnP server, and are 
 * passed to a client in the {@code UPnPActionResponseHandler}.
 *
 * @see UPnPActionHandler#notifyActionReceived(UPnPActionInvocation)
 * @see UPnPActionResponseHandler#notifyUPnPActionResponse(UPnPResponse)
 */
public class UPnPActionResponse extends UPnPResponse
{
    // Private Attributes
    private String m_argNames[] = null;
    private String m_argVals[] = null;

    /**
     * Constructs a {@code UPnPActionResponse} that conforms to
     * the OUT argument requirements of its associated {@code UPnPAction}.
     * <p>
     * This constructor ensures that the resulting action response
     * provides an argument value, in the proper {@code dataType} format,
     * for each of the OUT arguments of the UPnP action reported by
     * {@code actionInvocation.getAction()}.
     * <p>
     * For objects created through this constructor,
     * {@link #getArgumentNames()} will report, in order,
     * the required OUT argument names of the associated UPnP action.
     *
     * @param argVals An array of argument values corresponding,
     *                in order, to the OUT arguments of
     *                {@code actionInvocation.getAction()}.
     *
     * @param actionInvocation The action invocation that this action response
     *                         relates to.
     *
     * @throws IllegalArgumentException if {@code argVals} does not conform
     * to the OUT argument requirements of {@code actionInvocation.getAction()}.
     *
     * @throws NullPointerException if {@code action} is {@code null},
     * or {@code argVals} or any of its array elements is {@code null}.
     */

    public UPnPActionResponse(String[] argVals,
                              UPnPActionInvocation actionInvocation)
    {
        // Null checks 
        if ((actionInvocation == null) || (argVals == null))
        {
            throw new NullPointerException("Supplied action invocation or value was null");
        }
        for (int i = 0; i < argVals.length; i++)
        {
            if (argVals[i] == null)
            {
               throw new NullPointerException("null argVals element");
            }
        }
        
        // get out arg names ..note that this assumes getArgumentNames
        // returns out arg names in right order
        Vector outArgs = new Vector();
        String[] allArgs = actionInvocation.getAction().getArgumentNames();
        for (int i = 0; i < allArgs.length; i++)
        {
            if (!actionInvocation.getAction().isInputArgument(allArgs[i]))
            {
                outArgs.add(allArgs[i]);
            }
        }
        String[] argNames =
            (String[])outArgs.toArray(new String[outArgs.size()]);
        
        // Verify the array lengths match
        if ((argNames.length != argVals.length))
        {
            // One of the args were null so length of arrays do not match
            throw new IllegalArgumentException("Argument array lengths do not match");
        }
        if (argNames != null)
        {
            m_argNames = new String[argNames.length];
            System.arraycopy(argNames, 0, m_argNames, 0, m_argNames.length);
        }
        if (argVals != null)
        {
            m_argVals = new String[argVals.length];
            System.arraycopy(argVals, 0, m_argVals, 0, m_argVals.length);
        }
        
        //Verify that the argVals conform to the OUT argument requirements of the action
        UPnPAction action = actionInvocation.getAction();
        UPnPStateVariable stVar = null;
        boolean isValid = false;
        for (int x = 0; x < argNames.length; x++)
        {
            stVar = action.getRelatedStateVariable(argNames[x]);
            String dataType = stVar != null ? 
                    stVar.getDataType() != null ? 
                            stVar.getDataType().toLowerCase() : "" : "";
            if (Utils.isUDAFloat(dataType) || Utils.isUDAInt(dataType))
            {
                isValid = Utils.validateUDANumericValue(dataType, argVals[x], stVar.getMinimumValue(), stVar.getMaximumValue());
            }
            else if ("string".equals(dataType))
            {
                isValid = Utils.validateUDAStringValue(argVals[x], stVar.getAllowedValues());
            }
            else 
            {
                isValid = Utils.validateUDAValue(dataType, argVals[x]);
            }
            if (!isValid)
            {
                throw new IllegalArgumentException("Invalid dataType in argument values.");
            }
        }
        
        
        m_actionInvocation = actionInvocation;
    }

    /**
     * Gets the output argument names specified by this action response,
     * in the order they were specified in the constructor.
     *
     * @return The action response output argument names. If the 
     *         action response has no output arguments, returns a
     *         zero-length array.
     */
    public String [] getArgumentNames()
    {
        String names[] = new String[m_argNames.length];
        System.arraycopy(m_argNames, 0, names, 0, names.length);
        return names;
    }

    /**
     * Gets the output argument values specified by this action response,
     * in the order they were specified in the constructor.
     *
     * @return The action response output argument values. If the 
     *         action response has no output arguments, returns a
     *         zero-length array.
     */
    public String [] getArgumentValues()
    {
        String values[] = new String[m_argVals.length];
        System.arraycopy(m_argVals, 0, values, 0, values.length);
        return values;
    }

    /**
     * Gets the value of the specified argument.
     *
     * @param name The name of the argument.
     *
     * @return The value of the argument.
     *
     * @throws IllegalArgumentException if {@code name} does not match one
     * of the argument names specified for this action response.
     *
     * @see #getArgumentNames()
     */
    public String getArgumentValue(String name)
    {
        // Find the name in the array of name and get index to use
        // as look up into values array
        String value = null;
        int idx = -1;
        for (int i = 0; i < m_argNames.length; i++)
        {
            if (name.equals(m_argNames[i]))
            {
                idx = i;
                break;
            }
        }

        if (idx != -1)
        {
            value = m_argVals[idx];
        }
        else
        {
            throw new IllegalArgumentException("Name does not represent valid arg name: " +
                                                name);
        }
        return value;
    }

    /**
     * Gets the HTTP response code from the response.
     * 
     * @return  The HTTP response code associated with the 
     *          response, such as 200 (OK) or 500 (Internal
     *          Server Error).
     */
    public int getHTTPResponseCode()
    {
        int httpCode = -1;
        if (m_actionInvocation != null)
        {
            UPnPActionImpl uai = (UPnPActionImpl)m_actionInvocation.getAction();
            if (uai != null)
            {
                Action action = uai.getAction();
                if (action != null)
                {
                    UPnPStatus status = action.getStatus();
                    if (status != null)
                    {
                        httpCode = status.getCode();
                    }
                }
            }
        }
        return httpCode;
    }
}
