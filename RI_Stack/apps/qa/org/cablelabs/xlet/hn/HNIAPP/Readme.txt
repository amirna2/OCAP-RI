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

Setup :
1) Copy xait.properties and final.properties provided with the source code into \RI_Stack\bin\CableLabs\simulator\Win32\debug\env folder.
2) Replace the OCAP.hn.multicast.iface property in final.properties with an active NetworkInterface name(ex: eth0)
3) Remove hostapp.properties if any from env folder.

Description :
- This application is useful in exercising many of the HomeNetworking features/ Content Directory services.
	
	Option 1:
	- The initial screen has an option to search for devices(MediaServers) on the homenetwork and display them in a graphical grid view.
	- After traversing to a particular device and selecting the device, the content containers are shown in the next screen with a blue colored folder icon for
	  each ContentContainer.
	- Clicking on each of the ContentContainer will provide a new screen which displays the contents available in that content container and so on.
	- The identified file format will have their thumbnails displayed and other unidentified formats will be displaying a default thumbnail image.
	
	At any point in time, VK_BACKSPACE key in the remote will take you to the previous page and GREEN  COLORED KEY will refresh the page contents(Device display page
	and Content Container Page). To scroll to multiple page contents use pageup and pagedown

	Option 2:
	- The second option is to generate UPnP messages using this application.
	- Once this option is selected, there are options to generate server or client side messages.
	- On selecting either of the options, the next screen will provide the types of messages to be generated.
	
	Option 3:
	- The third option is to identify all the available types of devices in contrast to identifying only the Mediaserver using the first option.
	- Using the second option, after a device is created the UPnP device will be listed using this option.
