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
package org.cablelabs.xlet.hn.HNIAPP.util;
/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class HNConstants
{
	public static final String HOMEPAGE_NAME = "HomePage";
	public static final String DEVICEPAGE_NAME = "DevicePage";
	public static final String CONTENTLISTPAGE_NAME = "ContentListPage";
	public static final String CONTENTITEMSPAGE_NAME = "ContentItemPage";
	public static final String FULLSCREENPAGE_NAME = "FullScreenImagePage";
	public static final String MESSAGETYPELIST_NAME = "MessageTypeList";
	public static final String MESSAGECATEGORYLIST_NAME ="MessageCategoryList";
	
    public static final String CREATEMESSAGE_NAME = "CreateMessage";
	
	public static final String serverEntity = "server";
	public static final String clientEntity = "client";
	
	public static final String DISCOVER_MSG = "Discovery";
	public static final String DESCRIPTION_MSG = "Description";
	public static final String CONTROL_MSG = "Control";
	public static final String EVENTING_MSG = "Eventing";
	
	// To send the request for the type of devices to display
	public static final String DEVICE_TYPE_ALL = "All Devices";
	public static final String DEVICE_TYPE_MEDIASERVERS = "MediaServers";

    // This is the default device that is assumed to be found and tested against
    // message creation
    public static final String DEVICE_UDN_VALUE = "uuid:1b62c460-1dd2-11b2-bc1a-000391d934a2";

    public static final String DEVICE_FRIENDLY_NAME = "HNI Application";
    
    public static final String CONTENT_DISCOVERY_TIME_PROPERTY = "OCAP.hn.HNIApp.content.discovery.timeout.millis";
    public static final String DEVICE_DISCOVERY_TIME_PROPERTY = "OCAP.hn.HNIApp.device.discovery.timeout.millis";
    public static final String PROPERTIES_FILE_NAME = "final.properties";
}
