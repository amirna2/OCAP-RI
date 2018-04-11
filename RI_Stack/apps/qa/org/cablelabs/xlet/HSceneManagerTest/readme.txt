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
 
The HSceneManagerTest xlet tests the org.ocap.ui.HSceneManager API.  The test
provides menu options to perform the following tasks:

1. Create and run multiple test xlets.

2. Change the z-ordering of the xlets' scenes on the TV screen.

3. Change the position and the size of the xlets' scenes.

*******************************************************************************
                                   CONFIGURATION:
*******************************************************************************

The hostapp.properties file should define the test xlets to be created by the
HSceneManagerTest.

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

###############################################
## HSceneManagerTest
###############################################
app.1.application_identifier=0x000000016670
app.1.application_control_code=PRESENT
app.1.visibility=VISIBLE
app.1.priority=220
app.1.application_name=HSMTestRunner
app.1.base_directory=/snfs/qa/xlet
app.1.initial_class_name=org.cablelabs.xlet.HSceneManagerTest.HSMTestRunnerXlet
app.1.args.0=testXlet1=0x000000014671
app.1.args.1=testXlet2=0x000000014672
app.1.args.2=testXlet3=0x000000014673
app.1.args.3=testXlet4=0x000000014674
app.1.args.4=testXlet5=0x000000014675
app.1.args.5=testXlet6=0x000000014676
app.1.args.6=noShow=0x000000014675
app.1.args.7=noOrder=0x000000014674
app.1.args.8=noMove=0x000000014673

###############################################

app.2.application_identifier=0x000000014671
app.2.application_control_code=PRESENT
app.2.visibility=VISIBLE
app.2.priority=100
app.2.application_name=HSMTest1
app.2.base_directory=/snfs/qa/xlet
app.2.initial_class_name=org.cablelabs.xlet.HSceneManagerTest.HSMTestXlet
app.2.args.0=x=25
app.2.args.1=y=25
app.2.args.2=width=250
app.2.args.3=height=250
app.2.args.4=runner=0x000000016670
app.2.args.5=color=green

###############################################

app.3.application_identifier=0x000000014672
app.3.application_control_code=PRESENT
app.3.visibility=VISIBLE
app.3.priority=150
app.3.application_name=HSMTest2
app.3.base_directory=/snfs/qa/xlet
app.3.initial_class_name=org.cablelabs.xlet.HSceneManagerTest.HSMTestXlet
app.3.args.0=x=50
app.3.args.1=y=50
app.3.args.2=width=250
app.3.args.3=height=250
app.3.args.4=runner=0x000000016670
app.3.args.5=color=cyan

###############################################

app.4.application_identifier=0x000000014673
app.4.application_control_code=PRESENT
app.4.visibility=VISIBLE
app.4.priority=150
app.4.application_name=HSMTest3
app.4.base_directory=/snfs/qa/xlet
app.4.initial_class_name=org.cablelabs.xlet.HSceneManagerTest.HSMTestXlet
app.4.args.0=x=75
app.4.args.1=y=75
app.4.args.2=width=250
app.4.args.3=height=250
app.4.args.4=runner=0x000000016670
app.4.args.5=color=orange

###############################################


*******************************************************************************
                                  TEST CONTROL:
*******************************************************************************

The HSceneManagerTest provides the following menu options:

<< CH UP / CH DOWN >>
 Change Current Xlet

(PLAY)   Start Xlet
(STOP)   Stop  Xlet
(PAUSE)  Pause Xlet

(1) Toggle Move Type (between Size and Position)
(2) Pop to Front     (brings the currently selected xlet's scene to the front)

(UP) Decrease Y Size/Position
(DN) Increase Y Size/Position
(LT) Decrease X Size/Position
(RT) Increase X Size/Position

*******************************************************************************
                                   EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.  The z-order of the test
xlets is printed to the TV screen and to the console log.
