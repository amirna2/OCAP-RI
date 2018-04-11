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
The HostTestXlet is designed to exercise the following API packages:

import org.ocap.hardware.Host;
import org.ocap.hardware.IEEE1394Node;
import org.ocap.hardware.VideoOutputPort;

Controls: 
<0> - Current Host Information	: Displays information all the ports avaliable
				  on the host

<1> - Enable AC-Outlet	: Calls setACOutlet() and passes true. This turns on the 
			  use of the power outlet on the back

<2> - Disable AC-Outlet	: Calls setACOutlet() and passes false. This turns off  
			  the use of the power outlet on the back

<4> - Enable RF-Bypass	: Calls setRFBypass() and passes true. This allows RF
			  signals to pass through the STB

<5> - Disable RF-Bypass	: Calls setRFBypass() and passes false. This denies RF
			  signals to pass through the STB

<9> - Run Tests		: Tests the enabling and disabling of setACOutlet() and 
			  setRFBypass(). Check are done prior to testing to see
			  if the support exists (isACOutletPresent() and 
			  getRFBypassCapability()).

<EXIT> - exit the application	: destroyXlet() is called
<INFO> - Prints out a menu of the controls

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************
The hostapp shall be as followed:

###############################################
## Application 89 - HostTestXlet
###############################################
app.89.application_identifier=0x000000017042
app.89.application_control_code=PRESENT
app.89.visibility=VISIBLE
app.89.priority=220
app.89.application_name=HostTestXlet
app.89.base_directory=/syscwd/qa/xlet
app.89.initial_class_name=org.cablelabs.xlet.HostTest.HostTestXlet
app.89.service=0x12356

This will make it accessable through the Miscellaneous category in the 
App Launcher

*******************************************************************************
                                     EXECUTION:
*******************************************************************************
Automation:

1). Start Application
2). Press 9 to run automted tests
3). Look for failures and exception thrown in the teraterm log

Manual:

1). Plug in TV to back of STB 
2). Start Application
3). Press 0 and verify that the list returns no nulls or 
	no exceptions are thrown in the teraterm log
4). Press 1 to turn off the AC outlet. Verify the TV goes off
5). Press 2 to tunr back on the AC outlet. Verify the TV goes on.

NOTE: 8300HD do not support RF Bypass. There will be no noticable difference in 
toggling the RF port. Automation not on RFBypass not supported is OK.

