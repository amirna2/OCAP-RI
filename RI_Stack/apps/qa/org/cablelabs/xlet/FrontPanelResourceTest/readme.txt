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

Description:
The Front Panel Resource test application exercises allocation of resources 
among multiple applications and resolving resource contention. The test 
application can launch up to four additional applications with varying 
priorities. Each of these applications can then be set to reserve or release 
resources and indicate whether or not they are willing to give up a resource 
when there is contention. There are two front panel components that an 
application can reserve, text or message component. Depending on an applications 
priority, it can lose a resource even if it is unwilling because an application
with higher priority will be given the resource when it requests it. The Front 
Panel Resource test application verifies that resource contention is resolved
properly.


The user interface of the application allows for:
 - Starting, stopping, or pausing any of the four different applications
 - Reserving or releasing a front panel component for an application
 - Making an application willing to release a component
 - Printing out the test results

How to run Front Panel Resource test
------------------------------------

Application properties for the test should be defined in the hostapp.properties 
file. The application contains a hostapp.properties file that should be used for
launching this application. 
For example:

###############################################
## Application 1 - Front Panel Resource Test
###############################################
app.1.application_identifier=0x000000016610
app.1.application_control_code=PRESENT
app.1.visibility=VISIBLE
app.1.priority=220
app.1.application_name=FPResourceTestRunner
app.1.base_directory=/snfs/qa/xlet
app.1.initial_class_name=org.cablelabs.xlet.FrontPanelResourceTest.FPTestRunnerXlet
app.1.service=0x12355
app.1.args.0=0x000000016611
app.1.args.1=0x000000016612
app.1.args.2=0x000000016613
app.1.args.3=0x000000016614
###############################################

###############################################
## Application 2 - Front Panel Resource Test
###############################################
app.2.application_identifier=0x000000016611
app.2.application_control_code=PRESENT
app.2.visibility=INVISIBLE
app.2.priority=220
app.2.application_name=FPTest1
app.2.base_directory=/snfs/qa/xlet
app.2.initial_class_name=org.cablelabs.xlet.FrontPanelResourceTest.FPTestXlet
app.2.service=0x12355
app.2.args.0=x=0
app.2.args.1=y=0
app.2.args.2=width=213
app.2.args.3=height=240
app.2.args.4=runner=0x000000016610
###############################################

###############################################
## Application 3 - Front Panel Resource Test
###############################################
app.3.application_identifier=0x000000016612
app.3.application_control_code=PRESENT
app.3.visibility=INVISIBLE
app.3.priority=220
app.3.application_name=FPTest2
app.3.base_directory=/snfs/qa/xlet
app.3.initial_class_name=org.cablelabs.xlet.FrontPanelResourceTest.FPTestXlet
app.3.service=0x12355
app.3.args.0=x=213
app.3.args.1=y=0
app.3.args.2=width=213
app.3.args.3=height=240
app.3.args.4=runner=0x000000016610
###############################################

###############################################
## Application 4 - Front Panel Resource Test
###############################################
app.4.application_identifier=0x000000016613
app.4.application_control_code=PRESENT
app.4.visibility=INVISIBLE
app.4.priority=220
app.4.application_name=FPTest3
app.4.base_directory=/snfs/qa/xlet
app.4.initial_class_name=org.cablelabs.xlet.FrontPanelResourceTest.FPTestXlet
app.4.service=0x12355
app.4.args.0=x=0
app.4.args.1=y=240
app.4.args.2=width=213
app.4.args.3=height=240
app.4.args.4=runner=0x000000016610
###############################################

###############################################
## Application 5 - Front Panel Resource Test
###############################################
app.5.application_identifier=0x000000016614
app.5.application_control_code=PRESENT
app.5.visibility=INVISIBLE
app.5.priority=220
app.5.application_name=FPTest4
app.5.base_directory=/snfs/qa/xlet
app.5.initial_class_name=org.cablelabs.xlet.FrontPanelResourceTest.FPTestXlet
app.5.service=0x12355
app.5.args.0=x=213
app.5.args.1=y=240
app.5.args.2=width=213
app.5.args.3=height=240
app.5.args.4=runner=0x000000016610
###############################################

NOTE: This application has been designed so that it can be set up to run in 
automation. The hostapp.properties file that is supplied with the application
must be modified so that the XletDriver is enabled. 

The application can be evaluated by viewing the screen and the console logging
to verify that the test results are correct after running the application and
dumping the test results to the console. 

User Interface
--------------

When the application is loaded, a test launcher will be presented on the screen.
Selecting FPResourceTestRunner, will launch the test application.

Control Keys
------------
UP ARROW	- Scroll up through the four xlets
DOWN ARROW	- Scroll down through the four xlets
LEFT ARROW	- Change through the different Front Panel components
RIGHT ARROW 	- Change through the different Front Panel components
PLAY		- Start currently selected xlet
STOP		- Stop currently selected xlet
PAUSE		- Pause currently selected xlet
1		- Reserve indicator for current xlet
2		- Release indicator for current xlet
3		- Toggle willing to release resource for current xlet
0		- Print test results to console

Xlet Control:
initXlet
	Obtain xlet name and context, parse and store xlet arguments, and set up
	user interface for presenting information to the screen.
startXlet
	Make the user interface visible.
pauseXlet
	Hide user interface.
distroyXlet
	Destroy user interface and deallocate resources.
