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
This test reads and logs the application data base.

Configuration Parameters:
-none-

Test Control:
-none-

Evaluate by viewing the log (sample) and screen
Each test application should be shown as que'ed by the 
hostapplication file.  Verify data is the same as is
in the hostapplication file. This test only shows apps that
reside in the same service as this one.
********************************************************************************
AppsDBTestXlet::pauseXlet Enter
AppsBDTestXlet Found appDB
found attributes
AppName: AppsDatabase
AppID: 14205
isStartable = true
isServiceBound = true
AppName: UnsignedProperties
AppID: 13fff
isStartable = true
isServiceBound = true
AppName: HDSample
AppID: 14204
isStartable = true
isServiceBound = true
AppName: Tune
AppID: 14203
isStartable = true
isServiceBound = true
AppName: ServiceList
AppID: 14202
isStartable = true
isServiceBound = true
AppName: GraphicSpeed
AppID: 14201
isStartable = true
isServiceBound = true
AppName: UDPEchoClient
AppID: 17e08
isStartable = true
isServiceBound = true
AppName: TCPEchoClient
AppID: 17e07
isStartable = true
isServiceBound = true
AppName: AllProperties
AppID: 17fff
isStartable = true
isServiceBound = true
AppName: MemoryTest
AppID: 17206
isStartable = true
isServiceBound = true
AppName: HConfig
AppID: 10003
isStartable = true
isServiceBound = true
AppName: HSampler
AppID: 10002
isStartable = true
isServiceBound = true
AppName: JSSEInstall
AppID: 17205
isStartable = true
isServiceBound = true
AppName: MonAppProperties
AppID: 16fff
isStartable = true
isServiceBound = true
AppName: BFSConcurrent
AppID: 17204
isStartable = true
isServiceBound = true
AppName: BFS
AppID: 17202
isStartable = true
isServiceBound = true
AppName: TestLauncher
AppID: 17201
isStartable = true
isServiceBound = true
AppName: QAwatchTV
AppID: 16002
isStartable = true
isServiceBound = true
AppName: Time
AppID: 10201
isStartable = true
isServiceBound = true
AppName: SignedProperties
AppID: 15fff
isStartable = true
isServiceBound = true
AppName: Caption
AppID: 16202
isStartable = true
isServiceBound = true
AppName: MHPTester
AppID: 14207
isStartable = true
isServiceBound = true
AppName: ObjectCarousel
AppID: 14206
isStartable = true
isServiceBound = true
AppName: Stupid
AppID: 14001
isStartable = true
isServiceBound = true
********************************************************************************

isStartable() returns false because of changes introduced with MHP 1.0.3. 
Specifically the documentation for isStartable() now says:

     * An Application is not startable if any of the following apply.<ul>
     * <li>The application is transmitted on a remote connection.
     * <li>The caller of the method does not have the Permissions to start it.
     * <li>if the application is signalled with a control code which is neither AUTOSTART nor PRESENT.
     * </ul>

Since OCAP specifies that all unbound apps are signalled as REMOTE, all unbound apps will have isStartable() return false.  (Kinda silly, I know.)


Xlet Control:
initXlet
    no parameters to configure
        read application data base
startXlet
    display application data base
pauseXlet
    clear video
distroyXlet
    clear video
