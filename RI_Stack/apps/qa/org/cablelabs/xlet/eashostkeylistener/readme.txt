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
EASHostKeyListenerXlet is a host application used in conjunction with Emergency Alert System (EAS).  

Configuration Parameters:
The following parameter can be added to the hostapp.properties file.
app.0.args.0=t  
where t is time in seconds (0-n) to delay befor displaying a prompt screen when user press remote keys.

Example hostapp.properties entry:
app.0.application_identifier=0x000000017000
app.0.application_control_code=AUTOSTART (test standalone) or PRESENT (runs via EASManager)
app.0.visibility=VISIBLE
app.0.priority=254 (Very high)
app.0.base_directory=/syscwd/qa/xlet
app.0.application_name=EASHostKeyListenerXlet
app.0.initial_class_name=org.cablelabs.xlet.eashostkeylistener.EASHostKeyListenerXlet
app.0.args.0=5 (wait 5 seconds before accepting first remote key press)

Evaluate by viewing the TV screen.
Repeatedly press 'Last' button on remote to display/undisplay prompt screen.
Press 'Exit' button when prompt is displayed to terminate application and any EAS message from screen.

For set-top developers, you can run this application on your device as a hostapp using the above signaling.  Add the following line to your "final.properties" file:

OCAP.eas.presentation.interrupt.appId=0x000000017000

Then, trigger an EAS message on your headend.  If the message has an indefinite presentation time, the stack will automatically register the EASHostKeyListenerXlet.  Press the "SELECT" button to bring up the menu.  You can either continue the EAS presentation or terminate it.

For app developers running the RI on the PC Platform, see $RICOMMONROOT/resources/fdcdata/eas-test-files/README for information on how to simulate the delivery of EAS messages
