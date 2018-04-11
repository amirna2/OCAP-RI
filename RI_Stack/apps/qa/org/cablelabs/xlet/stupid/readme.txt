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
Stupid test application presents information about itself on the screen as well
as to the console. The application will behave according to the paramaters that 
it receives via Xlet arguments contained in the signalling. 

The user interface of the application displays:
 - the name of the test application
 - application ID
 - the service for the test application
 - the priority of the application
 - optionally it displays a message if supplied in the xlet arguments

How to run Stupid Test
----------------------

Application properties should be defined in the hostapp.properties file. 
For example:

###############################################
## Application 1 - Stupid Test
###############################################
app.0.application_identifier=0x000000017002
app.0.application_control_code=PRESENT
app.0.visibility=VISIBLE
app.0.priority=220
app.0.application_name=StupidTest
app.0.base_directory=/syscwd/qa/xlet
app.0.initial_class_name=org.cablelabs.xlet.stupid.StupidXlet
app.0.args.0=x=175
app.0.args.1=y=100
app.0.args.2=width=300
app.0.args.3=height=150
app.0.args.4=msg=Hello World
app.0.args.5=font=15
app.0.args.6=bg=0xFFff4500
app.0.args.7=fg=0xFF696969
app.0.args.8=focus
app.0.args.9=cwd
app.0.args.10=monitor
app.0.args.11=props
app.0.args.12=hang=5000
###############################################

Xlet arguments
--------------

There are fourteen parameters defined by the application. If no parameters are 
specified, then the application will use default settings. In this case, the 
test application will only display the name of the application, application ID,
the service of the application, and the priority of the applicaiton. The 
following fourteen parameters can be added to the hostapp.properties file:

app.0.args.0=x=         - x coordinate of the top left corner of the output box.
app.0.args.1=y=         - y coordinate of the top left corner of the output box.
app.0.args.2=width=     - width of the output box.
app.0.args.3=height=    - height of the output box.
app.0.args.4=msg=	- the supplied text string will be displayed on the screen.
app.0.args.5=font=	- supplied integer will be used for font size.
app.0.args.6=bg=	- integer in rgb format for background color.
app.0.args.7=fg=	- integer in rgb format for foreground color.
app.0.args.8=focus	- if supplied, gives application focus.
app.0.args.9=cwd	- display absolute and relative paths to application.
app.0.args.10=monitor	- if supplied, signals that the monitor is configured.
app.0.args.11=props	- if supplied, prints out a list of system properties.
app.0.args.12=hang	- if supplied, the application will either hang forever or for a specified number of milliseconds.
app.0.args.13=crash:	- to be followed with either zero, null, or static to create a crash.

NOTE: there are several things to be mindful of when assigning values to these 
parameters:
 - If the test output does not appear on the screen and the default x, y, width,
   and height paramters are not being used, then they need to be readjusted so
   the output is within the viewable area of the screen.
 - The integer used for determining either the foreground or background colors 
   needs to be a hexadecimal in rgb format and preceeded with 0xFF. For example, 
   the color red as an rgb integer in hexadecimal is ff0000. To assign this 
   color to the background it would appear as follows, 0xFFff0000.
 - Depending on the permissions level of the xlet or the permission that have 
   been granted to the xlet via a perm file, the cwd command may not be able to
   retrieve some information.
 - If no value is supplied for the parameter, hang, the application will never 
   launch. If a value is supplied, for example, hang=5000, then the application 
   will be suspended from launching until the time has expired. This value is 
   in milliseconds.
 - If the xlet parameter crash: is used the application will never launch 
   because an error will occur before it is able to finish loading. For example, 
   crash:zero will cause the application to perform division by zero which will 
   cause a java.lang.ArithmeticException: zero divisor to occur.

The application can be evaluated by viewing the screen as well as the console 
output.

Sample Xlet Parameters
----------------------
app.0.args.0=x=175
app.0.args.1=y=100
app.0.args.2=width=300
app.0.args.3=height=150
app.0.args.4=msg=Hello World
app.0.args.5=font=15
app.0.args.6=bg=0xFFff4500
app.0.args.7=fg=0xFF696969
app.0.args.8=focus
app.0.args.9=cwd
app.0.args.10=monitor
app.0.args.11=props
app.0.args.12=hang=5000

Explanation of sample parameters:

arg.0 and arg.1   - The x coordinate of the output box will be located at 175 
  		    pixels and the y coordinate will be located at 100 pixels. 
arg.2 and arg.3   - The output box is 300 pixels wide and 150 pixels tall.
arg.4 		  - Hello World will be displayed in the output box. 
arg.5		  - The font will have a size of 15.
arg.6 and arg.7   - The background color is a dark orange and the foreground 
		    color is a dark gray. 
arg.8 and arg. 10 - The application has focus as well as signalling that the 
		    monitor application has been configured. 
arg. 9 and arg.11 - The absolute and relative paths to the application will be 
		    printed to the console as well as a list of system 
		    properties. 
arg.12		  - There will be a 5 second delay before the application 
		    displays information to the screen.

Xlet Control:
initXlet
	Obtain xlet context.
startXlet
	Read xlet configuration parameters contained in the signalling. Display 
	user interface onto the screen.
pauseXlet
	Stop presenting user interface on the screen.
distroyXlet
	Dispose of user interface, remove key listeners, and free resources.
