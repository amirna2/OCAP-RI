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
DripFeedTest takes N arguments names (FRAME0 ... FRAMEN) which are names of files 
containing individual MPEG2 I or P-frames for rendering by a drip feed
Additionally, DripFeedTest optionally takes an argument (ITERATIONS) to specify the
number of iterations to run the test.

Upon starting the Xlet, the first image in the list will be displayed. To switch
between the images, use the LEFT and RIGHT keys on the remote. To start the test
cycle with the number of image displays defined by ITERATIONS, press the PLAY button.
This will cycle through the images, displaying each for 1 second. After the image
loop is complete, it will display test results at the console. To end the image loop
prematurely, press the STOP button.

Testing Drip Feed (DF) in Scaled Video mode:
If the ENTER (SELECT) button is pressed While the loop mode (press PLAY button), is 
selected, the scaled video mode could be displayed.  The DF video will appear in the 
scaled video located at the left top corner of the screen.


Sample hostapp.properties entry:

###############################################
## Application 87 - DripFeedTest
###############################################
app.87.application_identifier=0x000000014226
app.87.application_control_code=PRESENT
app.87.visibility=VISIBLE
app.87.priority=220
app.87.application_name=DripFeedTest
app.87.base_directory=/syscwd/qa/xlet
app.87.initial_class_name=org.cablelabs.xlet.DripFeedTest.DripFeedTestXlet
app.87.service=0x12348
# Up to N frames can be specified using consecutive FRAMEX numbering, 
# Note that all frames will be loaded into memory
app.87.args.0=FRAME0=/images/test1.m2v
app.87.args.1=FRAME1=/images/test2.m2v
#app.87.args.2=FRAME2=<XYZ>.m2v
#app.87.args.3=FRAME3=<ETC>.m2v
app.87.args.2=ITERATIONS=150

################################################
