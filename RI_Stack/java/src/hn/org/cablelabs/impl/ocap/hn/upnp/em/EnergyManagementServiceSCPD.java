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

package org.cablelabs.impl.ocap.hn.upnp.em;

public class EnergyManagementServiceSCPD
{
    public final static String getSCPD()
    {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\n" 
            + " <specVersion>\n" 
            + "     <major>1</major>\n"
            + "     <minor>0</minor>\n" 
            + " </specVersion>\n" 
            + " <actionList>\n" 
            + "      <action>\n"
            + "         <name>GetInterfaceInfo</name>\n"
            + "         <argumentList>\n"
            + "            <argument>\n"
            + "               <name>NetworkInterfaceInfo</name>\n"
            + "               <direction>out</direction>\n"
            + "               <relatedStateVariable>NetworkInterfaceInfo</relatedStateVariable>\n"
            + "            </argument>\n"
            + "            <argument>\n"
            + "               <name>ProxiedNetworkInterfaceInfo</name>\n"
            + "               <direction>out</direction>\n"
            + "               <relatedStateVariable>ProxiedNetworkInterfaceInfo</relatedStateVariable>\n"
            + "            </argument>\n"
            + "         </argumentList>\n"
            + "      </action>\n"
            + "      <action>\n"
            + "         <name>ServiceSubscription</name>\n"
            + "         <argumentList>\n"
            + "            <argument>\n"
            + "               <name>UniqueServiceName</name>\n"
            + "               <direction>in</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_UniqueServiceName</relatedStateVariable>\n"
            + "            </argument>\n"
            + "            <argument>\n"
            + "               <name>ResourceURI</name>\n"
            + "               <direction>in</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_URI</relatedStateVariable>\n"
            + "            </argument>\n"
            + "            <argument>\n"
            + "               <name>DurationRequest</name>\n"
            + "               <direction>in</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_Duration</relatedStateVariable>\n"
            + "            </argument>\n"
            + "            <argument>\n"
            + "               <name>ServiceSubscriptionID</name>\n"
            + "               <direction>out</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_ServiceSubsciptionID</relatedStateVariable>\n"
            + "            </argument>\n"                
            + "            <argument>\n"
            + "               <name>Duration</name>\n"
            + "               <direction>out</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_Duration</relatedStateVariable>\n"
            + "            </argument>\n"            
            + "         </argumentList>\n"
            + "      </action>\n"
            + "      <action>\n"
            + "         <name>ServiceRelease</name>\n"
            + "         <argumentList>\n"
            + "            <argument>\n"
            + "               <name>ServiceSubscriptionID</name>\n"
            + "               <direction>in</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_ServiceSubsciptionID</relatedStateVariable>\n"
            + "            </argument>\n"                
            + "         </argumentList>\n"
            + "      </action>\n"
            + "      <action>\n"
            + "         <name>ServiceRenewal</name>\n"
            + "         <argumentList>\n"
            + "            <argument>\n"
            + "               <name>DurationRequest</name>\n"
            + "               <direction>in</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_Duration</relatedStateVariable>\n"
            + "            </argument>\n"
            + "            <argument>\n"
            + "               <name>ServiceSubscriptionID</name>\n"
            + "               <direction>in</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_ServiceSubsciptionID</relatedStateVariable>\n"
            + "            </argument>\n"                
            + "            <argument>\n"
            + "               <name>Duration</name>\n"
            + "               <direction>out</direction>\n"
            + "               <relatedStateVariable>A_ARG_TYPE_Duration</relatedStateVariable>\n"
            + "            </argument>\n"            
            + "         </argumentList>\n"
            + "      </action>\n"            
            + "   </actionList>\n"
            + "   <serviceStateTable>\n" 
            + "      <stateVariable sendEvents=\"yes\">\n"
            + "         <name>NetworkInterfaceInfo</name>\n"
            + "         <dataType>string</dataType>\n"
            + "      </stateVariable>\n"
            + "      <stateVariable sendEvents=\"no\">\n"
            + "         <name>ProxiedNetworkInterfaceInfo</name>\n"
            + "         <dataType>string</dataType>\n"
            + "      </stateVariable>\n"
            + "      <stateVariable sendEvents=\"no\">\n"
            + "         <name>A_ARG_TYPE_Duration</name>\n"
            + "         <dataType>ui4</dataType>\n"
            + "      </stateVariable>\n"
            + "      <stateVariable sendEvents=\"no\">\n"
            + "         <name>A_ARG_TYPE_ServiceSubsciptionID</name>\n"
            + "         <dataType>ui4</dataType>\n"
            + "      </stateVariable>\n"
            + "      <stateVariable sendEvents=\"no\">\n"
            + "         <name>A_ARG_TYPE_UniqueServiceName</name>\n"
            + "         <dataType>string</dataType>\n"
            + "      </stateVariable>\n"
            + "      <stateVariable sendEvents=\"no\">\n"
            + "         <name>A_ARG_TYPE_URI</name>\n"
            + "         <dataType>string</dataType>\n"
            + "      </stateVariable>\n"                        
            + "   </serviceStateTable>\n" 
            + "</scpd>";
    }
}
