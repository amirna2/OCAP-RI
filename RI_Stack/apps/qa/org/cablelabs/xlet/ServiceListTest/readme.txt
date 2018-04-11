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
This test reads and logs the service data base. It also loads a file that lists
the sourceIDs of the services that are expected to be found. This list is compared
to the service list returned from the SIManager and discrepencies are logged
accordingly.
If no file is specified or the file is not found, the Xlet proceeds to log
the sourceIDs of the services it found to both the screen and the log file (Debug.txt)

Configuration Parameters:
If desired, specify the path to the file that contains the expected service list, e.g.
app.1.args.0=config_file=/snfs/path/to/config.properties

Test Control:
-none-

Evaluate by viewing the log -- sample

SourceIDs Found:
3e8
44d
44e
44f
450

OR

There were 2 failures:
1) org.cablelabs.test.autoxlet.AssertionFailedError: Found an unexpected service: AbstractService[0x12345]
        at org.cablelabs.xlet.ServiceListTest.ServiceListTestXlet.doListComparison (ServiceListTestXlet.java, line 157)
        at org.cablelabs.xlet.ServiceListTest.ServiceListTestXlet.startXlet (ServiceListTestXlet.java, line 123)
        at org.cablelabs.impl.manager.application.XletApp.doStart (SourceFile, line 532)
        at org.cablelabs.impl.manager.application.XletApp.doChangeState (SourceFile, line 377)
        at org.cablelabs.impl.manager.application.XletStateTransition$1.run (SourceFile, line 54)
        at org.cablelabs.impl.manager.application.WorkerTask.run (SourceFile, line 75)
        at org.cablelabs.impl.manager.application.AbstractCallerContext$1.run (SourceFile, line 222)
        at java.security.AccessController.doPrivileged (AccessController.java, line 280)
        at java.security.AccessController.doPrivileged (AccessController.java, line 254)
        at org.cablelabs.impl.manager.application.AbstractCallerContext.runAsContext (SourceFile, line 219)
        at org.cablelabs.impl.manager.application.ContextTask.run (SourceFile, line 62)
        at org.cablelabs.impl.manager.application.DemandExecQueue$DemandTask.run (SourceFile, line 198)
        at org.cablelabs.impl.manager.application.WorkerTask.run (SourceFile, line 75)
        at org.cablelabs.impl.manager.application.ThreadPool$1.run (SourceFile, line 117)
        at java.lang.Thread.run (Thread.java, line 669)
2) org.cablelabs.test.autoxlet.AssertionFailedError: Did not find service with locator: ocap://0x7777
        at org.cablelabs.xlet.ServiceListTest.ServiceListTestXlet.doListComparison (ServiceListTestXlet.java, line 165)
        at org.cablelabs.xlet.ServiceListTest.ServiceListTestXlet.startXlet (ServiceListTestXlet.java, line 123)
        at org.cablelabs.impl.manager.application.XletApp.doStart (SourceFile, line 532)
        at org.cablelabs.impl.manager.application.XletApp.doChangeState (SourceFile, line 377)
        at org.cablelabs.impl.manager.application.XletStateTransition$1.run (SourceFile, line 54)
        at org.cablelabs.impl.manager.application.WorkerTask.run (SourceFile, line 75)
        at org.cablelabs.impl.manager.application.AbstractCallerContext$1.run (SourceFile, line 222)
        at java.security.AccessController.doPrivileged (AccessController.java, line 280)
        at java.security.AccessController.doPrivileged (AccessController.java, line 254)
        at org.cablelabs.impl.manager.application.AbstractCallerContext.runAsContext (SourceFile, line 219)
        at org.cablelabs.impl.manager.application.ContextTask.run (SourceFile, line 62)
        at org.cablelabs.impl.manager.application.DemandExecQueue$DemandTask.run (SourceFile, line 198)
        at org.cablelabs.impl.manager.application.WorkerTask.run (SourceFile, line 75)
        at org.cablelabs.impl.manager.application.ThreadPool$1.run (SourceFile, line 117)
        at java.lang.Thread.run (Thread.java, line 669)

FAILURES!!!
Tests run: 50,  Failures: 2

Xlet Control:
initXlet
    no parameters to configure
    read service data base
startXlet
    display and test service data base
pauseXlet
    clear video
distroyXlet
    clear video

Configuration file contents:
The file containing the service list will contain one sourceID in hexidecimal
per line, e.g.

3e8
44d
44e
44f
450

Note the lack of a preceding '0x' or any other content.
