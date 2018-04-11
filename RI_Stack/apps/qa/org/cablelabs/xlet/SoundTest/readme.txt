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
 
The SoundTest xlet tests the org.havi.ui.HSound API.  It offers the ability to
play the following types of sound files: mp1, mp2, mp3, ac3 and aiff.

*******************************************************************************
                                   CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.65.application_identifier=0x000000017641
app.65.application_control_code=PRESENT
app.65.visibility=VISIBLE
app.65.priority=220
app.65.application_name=Sound
app.65.base_directory=/snfs/qa/xlet
app.65.initial_class_name=org.cablelabs.xlet.SoundTest.SoundTestXlet
app.65.service=0x12356

*******************************************************************************
                                   TEST CONTROL:
*******************************************************************************

SoundTest xlet user menu contains the following options:

1 - HSOUND TESTS
    1 - MPEG-1 Layer-1 (SoundTest.mp1)
    2 - MPEG-1 Layer-2 (SoundTest.mp2)
    3 - MPEG-1 Layer-3 (SoundTest.mp3)
    4 - AC3 (SoundTest.ac3)
    5 - AIFF (SoundTest.aiff)
    9 - Stop
    0 - Toggle Loop Mode
    C - Go to Main Menu
2 - JMF TESTS
    1 - MPEG-1 Layer-1 (SoundTest.mp1)
    2 - MPEG-1 Layer-2 (SoundTest.mp2)
    3 - MPEG-1 Layer-3 (SoundTest.mp3)
    4 - AC3 (SoundTest.ac3)
    5 - AIFF (SoundTest.aiff)
    9 - Stop
    C - Go to Main Menu
3 - OTHER TESTS
    1 - Run Automated Test Cases
    C - Go to Main Menu

Note: The sound file must be present in the xlet directory, for the xlet to
      be able to play it, and the TV volume should not be muted.

SAMPLE TEST CASE:

1. Start the SoundTest.
2. Press '2' to go to the JMF Tests Menu.
3. Press '5'  to play the sound file SoundTest.aiff.

*******************************************************************************
                                   EVALUATION:
*******************************************************************************

Evaluate by listening to the playout of the sound files.
