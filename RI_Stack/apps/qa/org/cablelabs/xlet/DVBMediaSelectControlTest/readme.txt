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

*******************************************************************************
                                      DESCRIPTION:
*******************************************************************************
The DVBMediaSelectControlXlet is designed to exercise the following API packages:

import org.dvb.media.DVBMediaSelectControl;

Controls: 

<1> - adds to the current service components a single pid 
	 refrence to the DVBMediaSelectControl object 

<2> - removes the current service component in locator form
	 from the DVBMediaSelectControl object

<3> - replaces the current service component in locator form
	 from the DVBMediaSelectControl object with the alternate service
	 component specifed by the second set of locator passed defined 
	 in the config file

<4> - selects the the alternate service component specifed by 
	 the second set of locator defined in the config file

<5> - selects the the alternate service component specifed by 
	 the second set of locator plus a single PID refernce 
         defined in the config file

<5> - Nothing at this time


*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************
The hostapp shall be as followed:

###############################################
## Application 64 - DVBMediaSelectControlTest
###############################################
app.64.application_identifier=0x000000014642
app.64.application_control_code=PRESENT
app.64.visibility=VISIBLE
app.64.priority=220
app.64.application_name=DVBMediaControl
app.64.base_directory=/snfs/qa/xlet
app.64.initial_class_name=org.cablelabs.xlet.DVBMediaSelectControlTest.DVBMediaSelectControlXlet
app.64.service=0x12356
app.64.args.0=config_file=config.properties

This will make it accessable through the Miscellaneous category in the 
App Launcher

The following config paramenters must be defined:

#DVBMediaSelectControl Channel
DVBfreq=657000000
DVBProgNum=5
DVBQam=16
DVBSecondaryPidNum = 1
DVB_PID_HEX = false
DVBVideoPid=50
#audio pid (english) = 51
DVBAudioPid1=51
#audio pid (spanish) = 52
DVBAudioPid2=52

The locators are defined
locator1 = DVBVideoPid,DVBAudioPid1
locator2 = DVBVideoPid,DVBAudioPid2
Single PID = DVBAudioPid2

This is hard coded into the test

The parameters will vary from location to location. This is only a sample of
Broomfield's paramenters.

*******************************************************************************
                                     EXECUTION:
*******************************************************************************
This xlet is primarily to be run in automation:
 The script code shall be used:
   <Xlet name="DVBMediaSelectControl" orgID="0x1" appID="0x4642" pauseAfterLast="1000">
      <RCEvent name="Add new service component (spanish audio)" pauseBeforeNext="1000" monitorTimeout="30000" getResultsAfter="true">VK_1</RCEvent>
      <RCEvent name="Remove service component (audio)" pauseBeforeNext="1000" monitorTimeout="30000" getResultsAfter="true">VK_2</RCEvent>
      <RCEvent name="Replace service component (english to spanish)" pauseBeforeNext="1000" monitorTimeout="30000" getResultsAfter="true">VK_3</RCEvent>
      <RCEvent name="Select service component (vidio and spanish audio)" pauseBeforeNext="1000" monitorTimeout="30000" getResultsAfter="true">VK_4</RCEvent>
      <RCEvent name="Select multiple service component (vidio, english and spanish audio)" pauseBeforeNext="1000" monitorTimeout="30000" getResultsAfter="true">
VK_5</RCEvent>

 Verify afterwards if there is a pass or fail.

This can be run manually but the deterministic outcome will not be factored.
This xlet should only be run this way if repetion of exceptions or debugging
through stack logging is used. 

To run,
	Select the test to be run
