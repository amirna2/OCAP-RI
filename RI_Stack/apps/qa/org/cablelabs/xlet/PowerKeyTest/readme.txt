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

This Xlet tests the power key event listener by allowing to register and
unregister event listeners. 

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.67.application_identifier=0x000000016305
app.67.application_control_code=PRESENT
app.67.visibility=VISIBLE
app.67.priority=220
app.67.application_name=PowerKeyTest
app.67.base_directory=/snfs/qa/xlet
app.67.initial_class_name=org.cablelabs.xlet.PowerKeyTest.PowerKeyTestXlet
app.67.service=0x12356

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. In the QA Test Launcher menu, select the 'Miscellaneous Test Service'.

2. Select the PowerKeyTest.

3. Test Control:
 
   Key "INFo"   - print test usage info
   Key "1"      - register a listener 
   Key "2"      - unregister a listener
   Key "3"      - perform synchronous call to get power status
   Key "Power"  - remote/front-panel toggles the power state

4. Sample Test Case:

   - Press the "Power" key a few times and verify that the current power 
     mode of the host toggles between Low and FULL, however asynchronous 
     power change event should not have been received.

   - Use the key "3" to retrieve the current state from the STB via the
     synchronous call. With this key both the current state and the event
     state should match because there was no state change at the system level.
     This is just a call to get the actual status.

   - Press key "1" to register the listener.
   
   - Press the "Power" key and verify that the xlet receives power key events.
     The values displayed on the screen should toggle between LOW and FULL power.
     The cached value should be opposite the event value, which means that the
     application was in one state before it received the notification of the
     state change event. 

   - Unregister the listener by pressing the key "2". Verify that no state
     changes happen when the power key is pressed.

   - Use the key "3" to retrieve the current state from the STB via the
     synchronous call. With this key both the current state and the event
     state should match because there was no state change at the system level.
     This is just a call to get the actual status.

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.

THE TEST PASSED IF:

(a) The xlet behaves as described in the test case above.  

(b) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) The xlet shows a different pattern of behavior then outlined in the above
    test case.

(b) There are exceptions, failures or errors present in the log.

Logs are available to confirm the behavior described above.  If necessary,
enable the MPE logs by changing the mpeenv.ini file and enabling the logs
(ALL DEBUG) in the UTIL module. This will show the consistency between the mpe
layer and the notification events received by the registered client.


****************************
Automation
****************************

Although the xlet implements the driveable interface which allows it to 
be run in the autoxlet framework, it does not make sense to run this
xlet automatically due to the need to press the VK_POWER key.  The POWER 
key must be received by the native code to actually change the power
state of the box.  When the xlet is run manually, the remote
sends the key code up through native and then on to the keyPressed()
callback in the xlet.  In AutoXlet mode, native is completely bypassed.

