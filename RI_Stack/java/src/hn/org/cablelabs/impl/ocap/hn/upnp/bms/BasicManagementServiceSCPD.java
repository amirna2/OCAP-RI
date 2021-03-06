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

/**
 * This is the main class for implementation of the BasicManagementService
 * It contains implementation classes for the BasicManagementService
 * associated with the root device required for the DLNA DIAGE feature. 
 */

public final class BasicManagementServiceSCPD
{
    /**
     * Basic Management Service Description
     */
    
    public static final String getSCPD()
    {
        return
                "<?xml version=\"1.0\"?>\n"
            +   "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\n"
            +   "   <specVersion>\n"
            +   "      <major>1</major>\n"
            +   "      <minor>0</minor>\n"
            +   "   </specVersion>\n"
            +   "   <actionList>\n"
            +   "      <action>\n"
            +   "         <name>GetDeviceStatus</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>DeviceStatus</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>DeviceStatus</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>Ping</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>Host</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Host</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>NumberOfRepetitions</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UInt</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Timeout</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_MSecs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>DataBlockSize</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UShort</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>DSCP</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_DSCP</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetPingResult</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Status</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_PingStatus</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>AdditionalInfo</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_String</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>SuccessCount</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UInt</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>FailureCount</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UInt</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>AverageResponseTime</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_MSecs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>MinimumResponseTime</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_MSecs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>MaximumResponseTime</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_MSecs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>NSLookup</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>HostName</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_HostName</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>DNSServer</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Host</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>NumberOfRepetitions</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UInt</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Timeout</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_MSecs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetNSLookupResult</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Status</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_NSLookupStatus</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>AdditionalInfo</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_String</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>SuccessCount</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UInt</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Result</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_NSLookupResult</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>Traceroute</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>Host</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Host</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Timeout</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_MSecs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>DataBlockSize</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UShort</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>MaxHopCount</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UInt</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>DSCP</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_DSCP</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetTracerouteResult</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Status</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TracerouteStatus</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>AdditionalInfo</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_String</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>ResponseTime</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_MSecs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>HopHosts</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Hosts</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetTestIDs</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>TestIDs</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>TestIDs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetActiveTestIDs</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>TestIDs</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>ActiveTestIDs</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetTestInfo</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Type</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestType</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>State</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestState</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>CancelTest</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>TestID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TestID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "   </actionList>\n"
            +   "   <serviceStateTable>\n"
            +   "      <stateVariable sendEvents=\"yes\">\n"
            +   "         <name>DeviceStatus</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"yes\">\n"
            +   "         <name>TestIDs</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <defaultValue></defaultValue>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"yes\">\n"
            +   "         <name>ActiveTestIDs</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <defaultValue></defaultValue>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_Boolean</name>\n"
            +   "         <dataType>boolean</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_String</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_UShort</name>\n"
            +   "         <dataType>ui2</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_UInt</name>\n"
            +   "         <dataType>ui4</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_DateTime</name>\n"
            +   "         <dataType>dateTime.tz</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_MSecs</name>\n"
            +   "         <dataType>ui4</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_TestID</name>\n"
            +   "         <dataType>ui4</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_TestType</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <allowedValueList>\n"
            +   "            <allowedValue>BandwidthTest</allowedValue>\n"
            +   "            <allowedValue>InterfaceReset</allowedValue>\n"
            +   "            <allowedValue>NSLookup</allowedValue>\n"
            +   "            <allowedValue>Ping</allowedValue>\n"
            +   "            <allowedValue>SelfTest</allowedValue>\n"
            +   "            <allowedValue>Traceroute</allowedValue>\n"
            +   "         </allowedValueList>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_TestState</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <allowedValueList>\n"
            +   "            <allowedValue>Requested</allowedValue>\n"
            +   "            <allowedValue>InProgress</allowedValue>\n"
            +   "            <allowedValue>Canceled</allowedValue>\n"
            +   "            <allowedValue>Completed</allowedValue>\n"
            +   "         </allowedValueList>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_DSCP</name>\n"
            +   "         <dataType>ui1</dataType>\n"
            +   "         <allowedValueRange>\n"
            +   "            <minimum>0</minimum>\n"
            +   "            <maximum>63</maximum>\n"
            +   "         </allowedValueRange>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_Host</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_Hosts</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_HostName</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_PingStatus</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <allowedValueList>\n"
            +   "            <allowedValue>Success</allowedValue>\n"
            +   "            <allowedValue>Error_CannotResolveHostName</allowedValue>\n"
            +   "            <allowedValue>Error_Internal</allowedValue>\n"
            +   "            <allowedValue>Error_Other</allowedValue>\n"
            +   "         </allowedValueList>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_NSLookupStatus</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <allowedValueList>\n"
            +   "            <allowedValue>Success</allowedValue>\n"
            +   "            <allowedValue>Error_DNSServerNotResolved</allowedValue>\n"
            +   "            <allowedValue>Error_Internal</allowedValue>\n"
            +   "            <allowedValue>Error_Other</allowedValue>\n"
            +   "         </allowedValueList>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_NSLookupResult</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_TracerouteStatus</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <allowedValueList>\n"
            +   "            <allowedValue>Success</allowedValue>\n"
            +   "            <allowedValue>Error_CannotResolveHostName</allowedValue>\n"
            +   "            <allowedValue>Error_MaxHopCountExceeded</allowedValue>\n"
            +   "            <allowedValue>Error_Internal</allowedValue>\n"
            +   "            <allowedValue>Error_Other</allowedValue>\n"
            +   "         </allowedValueList>\n"
            +   "      </stateVariable>\n"
            +   "   </serviceStateTable>\n"
            +   "</scpd>";
    }
}
