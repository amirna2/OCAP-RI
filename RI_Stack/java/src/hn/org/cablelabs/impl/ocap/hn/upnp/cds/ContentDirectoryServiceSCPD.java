/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 *  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
 * 
 *  This software is available under multiple licenses: 
 * 
 *  (1) BSD 2-clause 
 *
 *   Copyright (c) 2004-2006, Satoshi Konno
 *   Copyright (c) 2005-2006, Nokia Corporation
 *   Copyright (c) 2005-2006, Theo Beisch
 *   Copyright (c) 2013, Cable Television Laboratories, Inc.
 *   Collectively the Copyright Owners
 *   All rights reserved
 *
 *   Redistribution and use in source and binary forms, with or without modification, are
 *   permitted provided that the following conditions are met:
 *        *Redistributions of source code must retain the above copyright notice, this list 
 *             of conditions and the following disclaimer.
 *        *Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
 *             and the following disclaimer in the documentation and/or other materials provided with the 
 *             distribution.
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 *   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
 *   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 *   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 *   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 *   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 *   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 *   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 *   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 *  (2) GPL Version 2
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, version 2. This program is distributed
 *   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *   PURPOSE. See the GNU General Public License for more details.
 *  
 *   You should have received a copy of the GNU General Public License along
 *   with this program.If not, see<http:www.gnu.org/licenses/>.
 *  
 *  (3)CableLabs License
 *   If you or the company you represent has a separate agreement with CableLabs
 *   concerning the use of this code, your rights and obligations with respect
 *   to this code shall be as set forth therein. No license is granted hereunder
 *   for any other purpose.
 * 
 *   Please contact CableLabs if you need additional information or 
 *   have any questions.
 * 
 *       CableLabs
 *       858 Coal Creek Cir
 *       Louisville, CO 80027-9750
 *       303 661-9100
 */
package org.cablelabs.impl.ocap.hn.upnp.cds;

/**
 * Content Directory Service Description
 *
 * @author Michael A. Jastad
 * @version 1.0
 * @see
 */
public final class ContentDirectoryServiceSCPD
{
    /**
     * Content Directory Service Description
     */
    
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
            +   "      <action>\n"
            +   "         <name>DestroyObject</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>ObjectID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_ObjectID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>DeleteResource</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>ResourceURI</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_URI</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>UpdateObject</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>ObjectID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_ObjectID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>CurrentTagValue</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TagValueList</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>NewTagValue</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_TagValueList</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>Browse</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>ObjectID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_ObjectID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>BrowseFlag</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_BrowseFlag</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Filter</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Filter</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>StartingIndex</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Index</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>RequestedCount</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>SortCriteria</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_SortCriteria</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Result</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Result</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>NumberReturned</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>TotalMatches</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>UpdateID</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UpdateID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetSearchCapabilities</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>SearchCaps</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>SearchCapabilities</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>Search</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>ContainerID</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_ObjectID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>SearchCriteria</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_SearchCriteria</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Filter</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Filter</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>StartingIndex</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Index</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>RequestedCount</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>SortCriteria</name>\n"
            +   "               <direction>in</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_SortCriteria</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>Result</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Result</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>NumberReturned</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>TotalMatches</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_Count</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "            <argument>\n"
            +   "               <name>UpdateID</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>A_ARG_TYPE_UpdateID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetSortCapabilities</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>SortCaps</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>SortCapabilities</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetSystemUpdateID</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>Id</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>SystemUpdateID</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetFeatureList</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>FeatureList</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>FeatureList</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "      <action>\n"
            +   "         <name>GetServiceResetToken</name>\n"
            +   "         <argumentList>\n"
            +   "            <argument>\n"
            +   "               <name>ResetToken</name>\n"
            +   "               <direction>out</direction>\n"
            +   "               <relatedStateVariable>ServiceResetToken</relatedStateVariable>\n"
            +   "            </argument>\n"
            +   "         </argumentList>\n"
            +   "      </action>\n"
            +   "   </actionList>\n"
            +   "   <serviceStateTable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_SortCriteria</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_UpdateID</name>\n"
            +   "         <dataType>ui4</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_SearchCriteria</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_Filter</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_Result</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_Index</name>\n"
            +   "         <dataType>ui4</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_TagValueList</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_URI</name>\n"
            +   "         <dataType>uri</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_ObjectID</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>SortCapabilities</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>SearchCapabilities</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_Count</name>\n"
            +   "         <dataType>ui4</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>A_ARG_TYPE_BrowseFlag</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "         <allowedValueList>\n"
            +   "            <allowedValue>BrowseMetadata</allowedValue>\n"
            +   "            <allowedValue>BrowseDirectChildren</allowedValue>\n"
            +   "         </allowedValueList>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"yes\">\n"
            +   "         <name>SystemUpdateID</name>\n"
            +   "         <dataType>ui4</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>ServiceResetToken</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"yes\">\n"
            +   "         <name>LastChange</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "      <stateVariable sendEvents=\"no\">\n"
            +   "         <name>FeatureList</name>\n"
            +   "         <dataType>string</dataType>\n"
            +   "      </stateVariable>\n"
            +   "   </serviceStateTable>\n"
            +   "</scpd>";
    }
}
