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

/**
 * View Primary Output Port Service
 */
public final class VPOPServiceSCPD
{
    /**
     * VPOPService Description
     */
    
    private static final String VPOP_ACTIONS = 
            "      <action>\n"
        +   "         <name>Tune</name>\n"
        +   "         <argumentList>\n"
        +   "            <argument>\n"
        +   "               <name>ConnectionID</name>\n"
        +   "               <direction>in</direction>\n"
        +   "               <relatedStateVariable>A_ARG_TYPE_ConnectionID</relatedStateVariable>\n"
        +   "            </argument>\n"
        +   "            <argument>\n"
        +   "               <name>TuneParameters</name>\n"
        +   "               <direction>in</direction>\n"
        +   "               <relatedStateVariable>X_ARG_TYPE_TuneParameters</relatedStateVariable>\n"
        +   "            </argument>\n"        
        +   "         </argumentList>\n"
        +   "      </action>\n"
        +   "      <action>\n"        
        +   "         <name>AudioMute</name>\n"
        +   "         <argumentList>\n"
        +   "            <argument>\n"
        +   "               <name>ConnectionID</name>\n"
        +   "               <direction>in</direction>\n"
        +   "               <relatedStateVariable>A_ARG_TYPE_ConnectionID</relatedStateVariable>\n"
        +   "            </argument>\n"
        +   "         </argumentList>\n"
        +   "      </action>\n"
        +   "      <action>\n"        
        +   "         <name>AudioRestore</name>\n"
        +   "         <argumentList>\n"
        +   "            <argument>\n"
        +   "               <name>ConnectionID</name>\n"
        +   "               <direction>in</direction>\n"
        +   "               <relatedStateVariable>A_ARG_TYPE_ConnectionID</relatedStateVariable>\n"
        +   "            </argument>\n"
        +   "         </argumentList>\n"
        +   "      </action>\n"
        +   "      <action>\n"        
        +   "         <name>PowerOn</name>\n"        
        +   "      </action>\n"        
        +   "      <action>\n"        
        +   "         <name>PowerOff</name>\n"
        +   "         <argumentList>\n"
        +   "            <argument>\n"
        +   "               <name>ConnectionID</name>\n"
        +   "               <direction>in</direction>\n"
        +   "               <relatedStateVariable>A_ARG_TYPE_ConnectionID</relatedStateVariable>\n"
        +   "            </argument>\n"
        +   "         </argumentList>\n"
        +   "      </action>\n"
        +   "      <action>\n"
        +   "         <name>PowerStatus</name>\n"
        +   "         <argumentList>\n"
        +   "            <argument>\n"
        +   "               <name>PowerStatus</name>\n"
        +   "               <direction>out</direction>\n"
        +   "               <relatedStateVariable>X_ARG_TYPE_PowerStatus</relatedStateVariable>\n"
        +   "            </argument>\n"
        +   "         </argumentList>\n"
        +   "      </action>\n";    
    
    private static final String VPOP_STATE_VARIABLES = 
            "      <stateVariable sendEvents=\"no\">\n"
        +   "         <name>X_ARG_TYPE_TuneParameters</name>\n"
        +   "         <dataType>string</dataType>\n"
        +   "      </stateVariable>\n"
        +   "      <stateVariable sendEvents=\"no\">\n"
        +   "         <name>A_ARG_TYPE_ConnectionID</name>\n"
        +   "         <dataType>i4</dataType>\n"
        +   "      </stateVariable>\n"        
        +   "      <stateVariable sendEvents=\"no\">\n"
        +   "         <name>X_ARG_TYPE_PowerStatus</name>\n"
        +   "         <dataType>string</dataType>\n"
        +   "            <allowedValue>FULL POWER</allowedValue>\n"
        +   "            <allowedValue>STANDBY</allowedValue>\n"        
        +   "      </stateVariable>\n";        
    
    public static final String getSCPD()
    {
        return
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            +   "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">\n"
            +   "   <specVersion>\n"
            +   "      <major>1</major>\n"
            +   "      <minor>0</minor>\n"
            +   "   </specVersion>\n"
            +   "   <actionList>\n"
            +   VPOP_ACTIONS
            +   "   </actionList>\n"
            +   "   <serviceStateTable>\n"
            +   VPOP_STATE_VARIABLES
            +   "   </serviceStateTable>\n"
            +   "</scpd>";
    }
}
