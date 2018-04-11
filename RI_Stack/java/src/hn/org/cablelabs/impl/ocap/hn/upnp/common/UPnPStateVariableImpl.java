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

import org.cybergarage.upnp.AllowedValue;
import org.cybergarage.upnp.AllowedValueList;
import org.cybergarage.upnp.AllowedValueRange;
import org.cybergarage.upnp.StateVariable;
import org.ocap.hn.upnp.common.UPnPStateVariable;

public class UPnPStateVariableImpl implements UPnPStateVariable
{
    protected StateVariable m_stateVariable;
    
    public UPnPStateVariableImpl(StateVariable stateVariable)
    {
        assert stateVariable != null;
        
        m_stateVariable = stateVariable;
    }
    
    /**
     * Gets the allowed values for the UPnP state variable 
     * corresponding to this <code>UPnPStateVariable</code> object. 
     * The value returned is formatted per the UPnP Device 
     * Architecture specification, service description, 
     * allowedValueList element definition. If the 
     * <code>UPnPStateVariable</code> does not have an 
     * allowedValueList specified, returns zero length array. 
     *
     * @return An array containing the allowed values for this state
     *         variable.  Each element in the array contains the
     *         value of one allowedValue element in the
     *         allowedValueList.  The array has the same order as
     *         the allowedValueList element.
     */
    public String[] getAllowedValues()
    {
        AllowedValueList aList = m_stateVariable.getAllowedValueList();
        String aVals[] = new String[aList.size()];
        for (int i = 0; i < aList.size(); i++)
        {
            AllowedValue v = (AllowedValue)aList.get(i);
            aVals[i] = v.getValue();
        }
        return aVals;
    }

    /**
     * Gets the default value as discovered in the defaultValue element in
     * the UPnP service description stateVariable element this
     * <code>UPnPStateVariable</code> object corresponds to.
     *
     * @return The default value of the state variable. Returns an empty
     *         string if the variable does not have a
     *         defaultValue. 
     */
    public String getDefaultValue()
    {
        String result = m_stateVariable.getStateVariableNode().getNodeValue("defaultValue");
        return result != null ? result : "";
    }

    /**
     * Gets the allowedValueRange maximum value for the UPnP state 
     * variable corresponding to this <code>UPnPStateVariable</code> 
     * object.  The value returned is formatted per the UPnP Device 
     * Architecture specification, service description, 
     * allowedValueRange maximum element definition. 
     *
     * @return A <code>String</code> containing the maximum allowed 
     *         value for this state variable. Returns an empty
     *         string if the variable does not have an
     *         allowedValueRange. 
     */
    public String getMaximumValue()
    {
        String maxVal = "";
        AllowedValueRange range = m_stateVariable.getAllowedValueRange();
        if (range != null)
        {    
            maxVal = range.getMaximum();
        }
        return maxVal;
    }

    /**
     * Gets the allowedValueRange minimum value for the UPnP state 
     * variable corresponding to this <code>UPnPStateVariable</code> 
     * object.  The value returned is formatted per the UPnP Device 
     * Architecture specification, service description, 
     * allowedValueRange minimum element definition. 
     *
     * @return A <code>String</code> containing the minimum allowed 
     *         value for this state variable. Returns an empty
     *         string if the variable does not have an
     *         allowedValueRange.
     */
    public String getMinimumValue()
    {
        String minVal = "";
        AllowedValueRange range = m_stateVariable.getAllowedValueRange();
        if (range != null)
        {    
            minVal = range.getMinimum();
        }
        return minVal;
    }

    /**
     * Gets the name of this state variable as discovered in the
     * corresponding UPnP service description in the name element that is
     * part of the stateVariable element.
     *
     * @return The name of the state variable.
     */
    public String getName()
    {
        return m_stateVariable.getName();
    }

    public String getDataType()
    {
        return m_stateVariable.getDataType();
    }

    /**
     * Gets the allowedValueRange step value for the UPnP state 
     * variable.
     * The value returned is formatted per the UPnP Device 
     * Architecture specification, service description, 
     * allowedValueRange step element definition. 
     *
     * Note that if the step element is omitted and the data
     * type of the state variable is an integer, the step
     * value is considered to be 1.
     *
     * @return A <code>String</code> containing the step value for 
     *         this state variable. Returns an empty
     *         string if the service description of this variable
     *         does not specify a step value. 
     */
    public String getStepValue()
    {
        String[] intFormats = {"ui1", "ui2", "ui4", "i1", "i2", "i4", "int"};
        String stepVal = "";
        AllowedValueRange range = m_stateVariable.getAllowedValueRange();
        if (range != null)
        {    
            stepVal = range.getStep();
        }

        // See if dataType is an integer type if stepVal not set..if so return "1"
        if (stepVal == "")
        { 
            for (int i = 0; i < intFormats.length; i++)
            {
                if (m_stateVariable.getDataType().equalsIgnoreCase(intFormats[i]))
                {
                    stepVal = "1";
                    break;
                }
            }
        }

        return stepVal;
    }

    public boolean isEvented()
    {
        return m_stateVariable.isSendEvents();
    }
    
    public void setStateVariable(StateVariable stateVariable)
    {
        m_stateVariable = stateVariable;
    }
    
    protected StateVariable getStateVariable()
    {
        return m_stateVariable;
    }

}
