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
The Abstract Service Management test application exercises tuning based on a 
list of services. This list is obtained from the Service Information Manager, 
or SIManager, which will provide the application with a list of available 
services. Once the list of services is made available to the application, the 
application can then tune to these services. The name of the application as  
well as the current service tuned to, are displayed in the lower left corner  
of the screen. This information can be toggled on and off. The application has
two service contexts which it uses to tune to the abstract services. Each one 
can be tuned to different channels and registered or unregistered to an 
unbound application via the application manager proxy, appManagerProxy, 
independently of each other.

The user interface of the application allows for:
 - Channel up and down through the list of available services
 - Refresh the list of available services	
 - Register an unbound application through the appManagerProxy
 - Unregister an unboud application through the appManagerProxy
 - Tune to one or two abstract services
 - List available service contexts and which are presenting and which are not 

How to run Abstract Service Management Test
-------------------------------------------

Application properties for the test should be defined in the hostapp.properties 
file. 
For example:

###############################################
## Application 1 - TuneAbstractSvcTestXlet
###############################################
app.1.application_identifier=0x000000016681
app.1.application_control_code=PRESENT
app.1.visibility=VISIBLE
app.1.priority=220
app.1.application_name=TuneAbstractSvcTest
app.1.base_directory=/syscwd/qa/xlet
app.1.initial_class_name=org.cablelabs.xlet.AbstractSvcMgmtTest.TuneAbstractSvcTestXlet
app.1.service=0x12348
app.1.args.0=AbstractServiceName=stupidCK
###############################################

Configuration Parameters:
none

User Interface
--------------

When the application is loaded and executed, the channel map will be created. 
The application will then tune to the first service and full screen video will
be presented on the screen. The name of the application and the current service 
will be shown in the bottom left corner of the screen. Pressing the INFO button 
will toggle this information on and off.

Process Keys
------------
Channel up 	 - Tune forward through the list of available services for first service context.
Channel down 	 - Tune backwards through the list of available services for first service context.
INFO		 - Toggle on and off the information banner.
1		 - Refresh the list of available services.
2		 - Register unbound application via appManagerProxy.
3		 - Unregister unbound application via appManagerProxy.
4		 - Tune to abstract service with first service context.
6		 - Tune to abstract service with second service context.
7		 - Tune to abstract service registered with appManagerProxy with first service context.
9		 - Tune to abstract service registered with appManagerProxy with second service context.
0		 - List available service contexts and which are presenting and which are not  
PIP Channel Up	 - Tune forward through the list of available services for second service context. 
PIP Channel Down - Tune backwards through the list of available services for second service context.

The application can be evaluated by viewing the screen to verify the channel 
changes. Observe the console output to verify registering and unregistering 
of unbound applications. The list of available services will also be printed
to the console.

Xlet Control:
initXlet
	Obtain xlet context and create scene. Parse through xlet parameters to 
	obtain names of abstract services. Populate channel map with list of 
	channenls.
startXlet
	Add service listeners and set up the initial abstract service. Add key
	listeners and display the scene to the screen.
pauseXlet
	Hide scene, stop service, and remove key listeners.
distroyXlet
	Remove key listeners, dispose of scene, remove service, and clean up 
	resources.
