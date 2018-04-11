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
The MonApp test application is a simple and straightforward implementation of 
a Monitor Application. The MonApp Xlet does not present any visible UI.  

It allows for the demonstration/testing of various MonApp features including:
 - Security Policy Handler
 - App Filtering
 - Resource Contention Handler
 - App Signalling Handler
 - Reboot 
The policy enforced by MonApp is specified via Xlet arguments contained
in the signalling (e.g., in the XAIT or hostapp.properties).  When the
MonApp Xlet is launched, it sets up the specified policy.  When the MonApp
Xlet is shutdown and destroyed, all policy settings are no longer enforced.

Requesting Reboot on PowerTV
----------------------------
One option this test provides is to request a reboot of the system.  On debug
power tv images, this will not work and instead will cause the following 
assertion failure:
DIAG 0x92: Cause: 0x92 - production_assert
DIAG 0x92: Reason: OCAP initiated reset...

How to run MonApp Test
-----------------------  

Application properties should be defined in the hostapp.properties file. 
For example:

###############################################
## Application 1 - MonApp
###############################################
app.1.application_identifier=0x000000016004
app.1.application_control_code=PRESENT
app.1.visibility=VISIBLE
app.1.priority=255
app.1.application_name=MonApp
app.1.base_directory=/syscwd/qa/xlet
app.1.initial_class_name=org.cablelabs.xlet.monapp.MonApp
app.1.service=0x12356
# Keep HSampler from launching
app.1.args.0=app.deny=1:0002
# Don't allow any XAIT updates
app.1.args.1=disallowXAIT
# Deny HConfig from getting resources
app.1.args.2=rez.deny=org.havi.ui.HVideoDevice:1:6003
# Never allow resource contention to result in "stealing"
app.1.args.3=rez.contention=never
# All apps can only have unsigned permissions
app.1.args.4=perm.unsigned=1-FFFFFF
###############################################

To see the specification of supported Xlet arguments, run MonApp with "help"
as one of the Xlet arguments. Below are the results of running this command:

MonApp options:
signal                     : Invoke monitorSignalConfigured() upon completion
allowXAIT                  : Allow XAIT updates (default)
disallowXAIT               : Disallow XAIT updates
app.deny=<pattern>         : Deny apps specified by <pattern>
app.allow=<pattern>        : Allow apps specified by <pattern>
app.ask=<pattern>          : Ask before allow/deny apps specified by <pattern>
rez.deny=<proxy>:<pattern> : Deny apps specified by <pattern>
rez.allow=<proxy>:<pattern>: Allow apps specified by <pattern>
rez.ask=<proxy>:<pattern>  : Ask before allow/deny apps specified by <pattern>
rez.contention=<method>    : Resource contention method, <method> is one of default, always, never
  where <pattern> is as for org.ocap.application.AppPattern
  where <proxy> is a Resource class name
perm.unsigned=<pattern>    : AppID's matching pattern only get unsigned perms
perm.add=<perm>:<pattern>  : Add given permission for AppID's matching pattern
perm.deny=<perm>:<pattern> : Remove given permission for AppID's matching pattern
perm.null=<pattern>        : Return null for AppID's matching pattern
  where <pattern> is as for org.ocap.application.AppPattern
  where <perm> is permission definition: <class>[ <name> [<actions>]]
priority=<oid>:<aid>=<n>   : Set priority of app with AppID(<oid>,<aid>) to <n>
launch=<oid>:<aid>         : Launch app with AppID(<oid>,<aid>)
query bound/unbound        : Query all the bound and unbound apps



Xlet Arguments for Sample hostapp.properties File Explained
-----------------------------------------------------------

app.1.args.0=app.deny=1:0002 - prevent the app with an AppID==1:0002 (i.e., 
			       HSampler) from executing.

app.1.args.1=disallowXAIT    - the stack will ignore all XAIT updates 
			       signalled by the network.

app.1.args.2=rez.deny=org.havi.ui.HVideoDevice:1:0003 - prevent the app with 
			       an AppID==1:0003 (i.e., HConfig) from reserving 
			       any resources.  This will prevent HConfig from 
			       being able to reserve HScreenDevices in order 
			       to configure them.

app.1.args.3=rez.contention=never - specifies that when faced with Resource 
				    Contention, the MonApp favors the current 
				    reserveration.

app.1.args.4=perm.unsigned=1-FFFFFF - specifies that all applications are given 
				      "unsigned" permissions only.

Evaluate test results by viewing the console log and verifying that the policies
set by the Xlet arguments are being enforced.

In order to gain meaningful test results from running this test application, 
certain Xlet arguments should be used in conjunction with each other. An example
of this would be to set up the MonApp application so that it denies Caption Test
application the resource, org.havi.ui.Hscene. Then have the MonApp application
launch Caption Test and observe that Caption Test is not able to present video to 
the screen. An example of the Xlet arguments would appear as follows:
	app.1.args.0=rez.deny=org.havi.ui.HScene:1:6302
	app.1.args.1=launch=1:6302

Xlet Control:
initXlet
	Obtain xlet context.
startXlet
	Read xlet configuration parameters contained in the signalling.
pauseXlet
	Do nothing.
distroyXlet
	Deallocate resources.
