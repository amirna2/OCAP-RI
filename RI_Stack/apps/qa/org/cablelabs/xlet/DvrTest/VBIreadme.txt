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
The following is a test suite to test VBI API in the OCAP stack. This requires
Specific test samples to played out over an analog signal at 63 MHz on a DVD
player capable of handling both even and odd field of line 21 on VBI. 

The following test have dependencies on assets were VBI filtering is validated
against.

   # TestXDSFiltering : TestGroup6 - Test Case 2.1.1.2.4 : Clips 9, 12, 16, 22, 23, 25
   
   # TestXDSFiltering : TestGroup7 - Test Case 2.1.1.2.5 : Clips 9, 12, 24, 25
   
   # TestXDSFiltering : TestGroup8 - Test Case 2.1.1.2.6 : Clips 4, 5, 9, 12 ,14, 15, 22, 23, 24, 25
   
   # TestXDSFiltering : TestGroup9 - Test Case 2.1.1.2.7 : Clips 4, 5, 6, 14, 15, 16, 20, 22, 23, 24, 25
   
   # TestXDSFiltering : TestGroup10 - Test Case 2.1.1.2.8 : Clips 4, 5, 6, 14, 15, 16, 20, 22, 23, 24, 25
   
   # TestMultipleFilters : TestGroup11 - Test Case 2.1.2.1 : Clips 9, 12, 24, 25
   
   # TestRapidXDSFiltering on Multiple Filters - Test Case 2.4.2 and Test Case 2.4.3 : Clips 9, 12, 24, 25
   
   # TestFiltering On Seperate Services - Test Case 2.4.2 and Test Case 2.4.3 : Clips 9, 12, 24, 25
   
The other test cases shall be playable on most clips. Some clips cantain garbled or unusable data.
These clips are as such:
			Clip 2 - No VBI data
			Clip 6 - Much garble
			Clip 7 - Almost unusable data
			Clip 8 - Wierd stuff on even field (Spanish channel)
			Clip 13 - No XDS but even field CC
			Clip 17 - Lots of null packets in the even field
			Clip 18 - No XDS but even field CC
			Clip 21 - Lots of null packets in the even field

*******************************************************************************
                                   CONFIGURATION:
*******************************************************************************

Please refer to VBIconfig for more details.

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

###############################################
## Application 91 - VBITestRunnerXlet
###############################################
app.91.application_identifier=0x000000017213
app.91.application_control_code=PRESENT
app.91.visibility=VISIBLE
app.91.priority=0xff
app.91.launchOrder=0x0
app.91.platform_version_major=0x1
app.91.platform_version_minor=0x0
app.91.platform_version_micro=0x0
app.91.application_version=0x0
app.91.application_name=VBITestRunner
app.91.base_directory=/syscwd/qa/xlet
app.91.classpath_extension=
app.91.initial_class_name=org.cablelabs.xlet.DvrTest.VBITestRunnerXlet
app.91.service=0x12346
app.91.args.0=config_file=VBIconfig.properties

Use config file located in DvrTest directory  and place at /snfs/qa/xlet

*******************************************************************************
                                   TEST CONTROL:
*******************************************************************************
VBITestRunner is split into 3 groups, they are as followed:

Group 1 - Group 1 Automation
Test 1: Delete All Recordings
Test 2: CheckFreeDiskSpace
Test 3: TestBasicVBIFiltering : TestVBIBasicFilterFormat1
Test 4: TestBasicVBIFiltering : TestVBIBasicFilterFormat2
Test 5: TestBasicVBIFiltering : TestVBIBasicFilterFormat3
Test 6: TestNullFiltering : TestNullFiltering
Test 7: TestCCFiltering : TestGroup4
Test 8: TestCCFiltering : TestGroup5
Test 9: TestXDSFiltering : TestGroup6
Test 10: TestXDSFiltering : TestGroup7
Test 11: TestXDSFiltering : TestGroup8
Test 12: TestXDSFiltering : TestGroup9
Test 13: TestXDSFiltering : TestGroup10
Test 14: TestMultipleFilters : TestGroup11
Test 15: TestClearFiltering

Group 2 - Group 2 Automation
Test 1: Delete All Recordings
Test 2: CheckFreeDiskSpace
Test 3: TestVBIGroupMethods
Test 4: TestVBINotification : TestTimeout12
Test 5: TestVBINotification : TestTimeout13
Test 6: TestVBINotification : TestTimeout14
Test 7: TestVBINotification : TestTimeout15
Test 8: TestVBIZeroNotification
Test 9: TestVBITimeNotification : TimeNotifyGroup 17
Test 10: TestVBITimeNotification : TimeNotifyGroup 18
Test 11: TestVBITimeNotification : TimeNotifyGroup 19
Test 12: TestVBITimeNotification : TimeNotifyGroup 20
Test 13: TestVBITimeNotification : TimeNotifyGroup 21
Test 14: TestVBIDataNotification : DataNotifyGroup 22
Test 15: TestVBIDataNotification : DataNotifyGroup 23
Test 16: TestVBIDataNotification : DataNotifyGroup 24
Test 17: TestVBIDataNotification : DataNotifyGroup 25
Test 18: TestVBIDataNotification : DataNotifyGroup 26

Group 3 - Stress Tests
Test 1: Delete All Recordings
Test 2: CheckFreeDiskSpace
Test 3: TestRapidXDSFiltering: Group 27
Test 4: TestRapidXDSFiltering on Multiple Filters
Test 5: TestFiltering On Seperate Services
Test 6: TestExceptionBySetNotoficationByDataUnits
Test 7: TestEventCodeVideoSourceChanged

Please refer to VBI Test Plan for more details on each test case.

*******************************************************************************
                                   EVALUATION:
*******************************************************************************
Watch for PASS/FAIL messaging. Find exceptions. Parse through the log for an errors.
