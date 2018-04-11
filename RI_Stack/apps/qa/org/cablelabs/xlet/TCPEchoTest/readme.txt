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

This test collects and records performance data on the time it takes a TCP Echo
request and response to go from STB to the TCPEchoServer running on the network
at the Headend.

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE CONFIG.PROPERTIES PARAMETERS:

The values shown are examples that must be replaced with the values of your
test network.

PerfReporter=192.168.168.58
PerfReporterPort=8777
TCPEchoServer=192.168.168.58
TCPEchoPort=8778
TCPIterations=10

The UDPPerfLogServer must be running on the network at the Headend. The
PerfReporter ip address and port parameters listed above are the ip address and
port that the PerfReporter is listening on.

The TCPEchoServer must be running on the network at the Headend.  The
TCPEchoClientXlet uses the TCPEchoServer ip address and port parameters above
to send test messages to the TCPEchoServer.

TCPIterations is the number of times the echo test will be performed.

*******************************************************************************
                                    TEST CONTROL:
*******************************************************************************

The TCP message used in the TCP echo test is 1024 bytes.

XLET CONTROL:

initXlet     The above performance reporting and TCPEchoServer parameters are
             read into the Xlet.

startXlet    Sends the echo test messages, records the timing, logs the results.

pauseXlet    No actions.

destroyXlet  Close the sockets and files.


HOW TO RUN TCPEchoTest, with TCPEchoServer Running on Local Machine:

  Step 1:  Edit file \OCAPROOT\bin\OCAPTC\qa\xlet\config.properties:

           PerfReporter=
           PerfReporterPort=8777
           TCPEchoServer=
           TCPEchoPort=8778

  Step 2:  Open Command Prompt. Cd to \OCAPROOT\bin\OCAPTC\qa\xlet.
           Run the following command:

             java org.cablelabs.xlet.TCPEchoTest.TCPEchoServer 8778

  Step 3:  Open another Command Prompt. Cd to \OCAPROOT\bin\OCAPTC\qa\xlet.
           Run the following command:

             java org.cablelabs.lib.utils.UDPPerfLogServer 8777

  Step 4:  See output on console, and in
           \OCAPROOT\bin\OCAPTC\qa\xlet\perflog.txt.

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate by viewing the performance log file.
