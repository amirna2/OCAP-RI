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

The TransportLocatorTest xlet tests tuning with an OCAPLocator that only
specifies frequency and modulation mode.  The list of frequency/qam/tsid
combinations for the test is available in the config.properties file:

tsid_freq_0=591000000
tsid_qam_0=8
tsid_id_0=1

tsid_freq_1=639000000
tsid_qam_1=16
tsid_id_1=10022

etc.

The xlet performs the following steps for each config.properties entry:

1. Tune with a locator constructed from frequency and qam.
2. Retrieve the transport stream id the NetworkInterface is currently tuned to.
3. Compare the retrieved tsid with the tsid configured in the config.properties
   file.  They should match.

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.42.application_identifier=0x000000014304
app.42.application_control_code=PRESENT
app.42.visibility=VISIBLE
app.42.priority=220
app.42.application_name=TransportLocatorTest
app.42.base_directory=/snfs/qa/xlet
app.42.initial_class_name=org.cablelabs.xlet.TransportLocatorTest.TransportLocatorTestXlet
app.42.service=0x12353
app.42.args.0=config.properties

SAMPLE CONFIG.PROPERTIES PARAMETERS (for the Portland headend):

tsid_freq_0=591000000
tsid_qam_0=8
tsid_id_0=1

tsid_freq_1=639000000
tsid_qam_1=16
tsid_id_1=10022

tsid_freq_2=585000000
tsid_qam_2=16
tsid_id_2=102

tsid_freq_3=633000000
tsid_qam_3=16
tsid_id_3=10021

'tsid_freq'
 Frequency to tune to.

'tsid_qam'
 Modulation mode

'tsid_id'
 Transport stream id to compare to the one returned from the Transport locator
 call.  The tsid values specified in the config.properties file are used for
 comparison with the actual tsids retrived by the xlet, after it tunes to a
 stream.  The tsid values for your config.properties file can be retrieved from
 the DNCS by your local headend admin.

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. Configure the test via the config.properties file (see example above).  The
   tsid values for your config.properties file can be retrieved from the DNCS
   by your local headend admin.

2. In the QA Test Launcher menu, select the 'Transport Stream Test Service'.

3. Select the TransportLocatorTest, and wait for the test to tune to all
   the frequencies listed in the config.properties file.

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.  Sample test output:

------------------------------------------------------
Test 0:
Acquired TSID for loc ocap://f=0x2339f1c0.m=0x8  : 1
Configured TSID for loc ocap://f=0x2339f1c0.m=0x8: 1
------------------------------------------------------
TSIDS MATCH for freq=591000000, qam=8 : PASS
------------------------------------------------------

THE TEST PASSED IF:

(a) The test successfully tuned to each frequency in the config file.

(b) The configured tsids matched the tsids retrieved by the xlet.  

(c) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) The test failed to tune to one or more frequencies in the config file.
    NOTE: If a tune failed, the test may mark the comparison as 'INCONCLUSIVE'.

(b) A configured tsid differed from the one retrieved by the xlet.

(c) There are exceptions, failures or errors present in the log.
