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

This Xlet verifies that VK_RC_LOW_BATTERY, VK_USER and VK_CC have been added to the OCAP RemoteControl Key Event Codes as part of the OCVET-72 implementation. This xlet maps VK_RC_LOW_BATTERY, VK_USER and VK_CC to the remote control keys VK_1, VK_2 and VK_3, respectively. When one of the mapped keys is pressed a OCRcEvent keypress is generated for the corresponding, newly implemented, key. When the keyListener identifies one of these keyPresses a message is printed to the GUI.

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.0.application_identifier=0x000000016405
app.0.application_control_code=AUTOSTART
app.0.visibility=VISIBLE
app.0.priority=220
app.0.application_name=OCVET72Test
app.0.base_directory=/syscwd/qa/xlet
app.0.initial_class_name=org.cablelabs.xlet.OCVET72Test.OCVET72TestXlet

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. From $PLATFORMROOT enter:
	>./runRI.sh -setup -xlet OCVET72Test

2. Test Control:
 
   Key "INFO"   - print test usage info
   Key "1"      - generate a VK_RC_LOW_BATTERY keypress 
   Key "2"      - generate a VK_USER keypress
   Key "3"      - generate a VK_CC keypress

3. Sample Test Case:

   - Press the key "1". Verify that the GUI shows that a VK_RC_LOW_BATTERY keypress was generated.
   - Press the key "2". Verify that the GUI shows that a VK_USER keypress was generated. 
   - Press the key "3". Verify that the GUI shows that a VK_CC keypress was generated.

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.

THE TEST PASSED IF:

(a) The xlet behaves as described in the test case above.  

THE TEST MAY HAVE FAILED IF:

(a) The xlet shows a different pattern of behavior then outlined in the above
    test case.


