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
              WARNING: THIS APPLICATION USES NON-COMPLIANT APIs:
*******************************************************************************

This application uses non-compliant APIs. 
The last svn revision to contain the source files is 31430.

*******************************************************************************
                                      DESCRIPTION:
*******************************************************************************

The PSIPTuneTest xlet tests the ability to tune by frequency, pid, qam, when
the OOB SI subcomponent is disabled by setting "SITP.SI.ENABLED==0" in the
mpeenv.ini file.

The xlet performs the following steps:

1. Attempt to acquire services.  The service list should be empty.

2. Tune to the first freq/pid/qam listed in the Aconfig.properties file for the
   PSITuneTest.

3. Attempt to aquire services again.  The service list should contain all the
   services on the tuned frequency.

4. Tune to all the services in the service list.

5. Repeat the above steps for the next frequency listed in the
   Aconfig.properties file.


*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

    app.24.application_identifier=0x000000015301
    app.24.application_control_code=PRESENT
    app.24.visibility=VISIBLE
    app.24.priority=220
    app.24.application_name=PSIPTuneTest
    app.24.base_directory=/snfs/qa/xlet
    app.24.initial_class_name=org.cablelabs.xlet.PSIPTuneTest.PSIPTuneTestXlet
    app.24.args.0=config_file=config.properties

SAMPLE CONFIG.PROPERTIES PARAMETERS: (for the Portland headend):

    psip_channel_freq_0=633000000
    psip_channel_program_number_0=5
    psip_channel_qam_0=16 
    psip_channel_name_0=ToonDisney
    psip_channel_description_0="Toon Disney"

    psip_channel_freq_1=639000000
    psip_channel_program_number_1=2
    psip_channel_qam_1=16 
    psip_channel_name_1=FitTV
    psip_channel_description_1="Fit TV"

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. Set "SITP.SI.ENABLED==0" in the mpeenv.ini file.

2. List in the config.properties file the frequency/pid/qam of the services
   you want the test to tune to.

3. In the QA Test Launcher menu, select the 'Transport Stream Test Service'.

4. Select the PSIPTuneTest. 

5. Wait for the test to finish tuning to all the services on the frequencies
   listed in the config.properties file.


*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate test results by viewing the TV screen and the console log.

THE TEST PASSED IF:

(a) The test successfully tunes to all the services on the configured
    frequencies.  The xlet logs the acquired service lists, and all the channels
    it tuned to.  The following message should get printed to the console log:
    "PSIP TUNE TEST: Completed the test."  

(b) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) The test failed to tune to some or all of the services on the configured
    frequencies.

(b) There are exceptions, failures or errors present in the log.
