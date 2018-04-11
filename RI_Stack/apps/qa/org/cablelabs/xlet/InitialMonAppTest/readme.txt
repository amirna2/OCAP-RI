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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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
The Initial Monitor Application test application demonstrates the use of 
xait.properties files to launch applications instead of the hostapp.properties
file. This application was written to test workset 86, WS86. Included with 
this application is a document, WS86.doc, that more completely describes this 
application and the different elements that it tests. 

The following system propery must first be added to the mpeenv.ini file before 
the Initial Monitor Application test can be launched:
OCAP.mgrmgr.Signalling=org.cablelabs.impl.manager.signalling.TestSignallingMgr
The system must also be set to listen for xaits. The system property 
OCAP.xait.ignore value must be set to “false”. 

In addition, the hostapp.properties files located under bin\%OCAPTC%\apps\ 
and bin\%OCAPTC%\qa\ must be either removed or given a different name so that 
the stack does not launch these applications as well as the applications
signalled in the xait.properties at the same time. Also, the xait.properties
file will need to be placed in bin\%OCAPTC%\qa\ so that the proper test 
application will be launched at startup.

In WS86.doc, there are eight test cases outlined. Each of these test cases has
a corresponding xait.properties file. Depending on which one of these is 
supplied when the application is launched will determine which test is 
executed.

The names of all xait.properties files are:
xait.properties
xait.properties.test1REMOTE
xait.properties.test2
xait.properties.test3
xait.properties.test4
xait.properties.test5
xait.properties.test6
xait.properties.test7

In order to run one of the test cases, the xait.properties file corresponding
to that test will need to be renamed to just xait.properties so that it will 
be launched.

The application can be evaluated by viewing the screen and the console to 
gather the necessary information as to the out come of the test case. Then 
comparing these results with the expected results outlined in WS86.doc that
corresponds to that particular test.

User Interface
--------------

The user interface contains a menu of the available tests within the application
that can be started or paused.

Control Keys
------------

MENU 		- toggles on and off the menu of available tests.
UP ARROW	- Scroll up through the tests.
DOWN ARROW	- Scroll down through the tests.
SELECT		- Start highlighted test.

Xlet Control:
initXlet
	Obtain xlet context, register key listeners, and set up user interface.
startXlet
	Set up handler via Application Manager Proxy, AppManagerProxy. Start 
	presenting user interface and request focus.
pauseXlet
	Hide User interface.
distroyXlet
	Remove user interface and deallocate resources.
