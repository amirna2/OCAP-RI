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

The PATPMTTest xlet scans the entire channel map, retrieves the In-Band PAT and
PMT tables as populated in org.ocap.si.ProgramAssociationTable and 
org.ocap.si.ProgramMapTable, and displays them to the screen and to the console
log.  The Xlet tests these methods:

    org.ocap.si.SIRequest.cancel()
    org.ocap.si.ProgramMapTableManager.retrieveInBand()  << 1-time request >>
    org.ocap.si.ProgramMapTableManager.addInBandChangeListener()
    org.ocap.si.ProgramMapTableManager.removeInBandChangeListener()
    org.ocap.si.ProgramAssociationTableManager.retrieveInBand()  << 1-time request >>
    org.ocap.si.ProgramAssociationTableManager.addInBandChangeListener()
    org.ocap.si.ProgramAssociationTableManager.removeInBandChangeListener()

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE CONFIG.PROPERTIES PARAMETERS:

pat_pmt_no_pat_locator_0=ocap://0x3eb
pat_pmt_no_pat_locator_1=ocap://0x3ee
pat_pmt_no_pat_locator_2=ocap://0x3f0
pat_pmt_no_pat_locator_3=ocap://0x429

pat_pmt_no_pat_locator_4=ocap://0x7e5
pat_pmt_no_pat_locator_5=ocap://0x7e6
pat_pmt_no_pat_locator_6=ocap://0x7e7
pat_pmt_no_pat_locator_7=ocap://0x7e8
pat_pmt_no_pat_locator_8=ocap://0x7e9
pat_pmt_no_pat_locator_9=ocap://0x7ea
pat_pmt_no_pat_locator_10=ocap://0x7eb
pat_pmt_no_pat_locator_11=ocap://0x7ec
pat_pmt_no_pat_locator_12=ocap://0x7ed
pat_pmt_no_pat_locator_13=ocap://0x7ee
pat_pmt_no_pat_locator_14=ocap://0x7ef

pat_pmt_no_pat_locator_15=ocap://0xfd9
pat_pmt_no_pat_locator_16=ocap://0xfda
pat_pmt_no_pat_locator_17=ocap://0xfdb

pat_pmt_no_pat_locator_18=ocap://0x7d2
pat_pmt_no_pat_locator_19=ocap://0x7d3
pat_pmt_no_pat_locator_20=ocap://0x7d4
pat_pmt_no_pat_locator_21=ocap://0x7d5
pat_pmt_no_pat_locator_22=ocap://0x7d6
pat_pmt_no_pat_locator_23=ocap://0x7d7
pat_pmt_no_pat_locator_24=ocap://0x7d8

pat_pmt_no_pat_locator_25=ocap://0x870

pat_pmt_no_pat_locator_26=ocap://0xa8c
pat_pmt_no_pat_locator_27=ocap://0xa8d
pat_pmt_no_pat_locator_28=ocap://0xa8e
pat_pmt_no_pat_locator_29=ocap://0xa8f
pat_pmt_no_pat_locator_30=ocap://0xa90
pat_pmt_no_pat_locator_31=ocap://0xa91
pat_pmt_no_pat_locator_32=ocap://0xa92
pat_pmt_no_pat_locator_33=ocap://0xa93
pat_pmt_no_pat_locator_34=ocap://0xa94
pat_pmt_no_pat_locator_35=ocap://0xa95

pat_pmt_hidden_frequency_0=591000000
pat_pmt_hidden_qam_0=8
pat_pmt_hidden_prog_num_0=1

pat_pmt_hidden_frequency_1=591000000
pat_pmt_hidden_qam_1=8
pat_pmt_hidden_prog_num_1=2

pat_pmt_hidden_frequency_2=591000000
pat_pmt_hidden_qam_2=8
pat_pmt_hidden_prog_num_2=3

'pat_pmt_no_pat_locator'
 Some services in the channel map do not have a PAT and/or a PMT.  Listing
 them in the config.properties file is optional.  It enables the test to
 exclude those services from its channel map.

'pat_pmt_hidden_frequency'
'pat_pmt_hidden_qam'
'pat_pmt_hidden_prog_num'
 The above parameters allow adding hidden channels to the channel map,
 and getting their PAT and PMT tables.  This feature is optional.

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.43.application_identifier=0x000000014305
app.43.application_control_code=PRESENT
app.43.visibility=VISIBLE
app.43.priority=220
app.43.application_name=PATPMTTest
app.43.base_directory=/snfs/qa/xlet
app.43.initial_class_name=org.cablelabs.xlet.PATPMTTest.PATPMTTestXlet
app.43.service=0x12353
app.43.args.0=config_file=config.properties

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. Configure the test via the hostapp.properties and the config.properties
   files (see example above).

2. In the QA Test Launcher menu, select the 'Transport Stream Test Service'.

3. Select the PATPMTTest.

4. Test control options (also listed in a text window at the bottom of the
   TV screen):

   [CHANNEL UP]   - move up to the next channel in your channel map.
   [CHANNEL DOWN] - move down to the previous channel in your channel map.
   [SELECT]       - tune to the selected channel.
   [1]            - cancel inband PAT request.
   [2]            - cancel inband PMT request.
   [3]            - register inband PAT change listener.
   [4]            - register inband PMT chaneg listener.
   [5]            - request out-of-band PAT.
   [6]            - request out-of-band PMT.
   [7]            - register out-of-band PAT change listener.
   [8]            - register out-of-band PMT change listener.
   
5. Press[SELECT] to tune to the first channel in your channel map.  The
   requested PAT/PMT data will be displayed on the screen.
   
   The box will remain tuned to the current channel, until you select a new
   channel with the [CHANNEL UP] or [CHANNEL DOWN] buttons.  During this time
   you may cause a PAT/PMT change from the headend, which will be reflected
   on-screen, if you had an appropriate PAT/PMT change listener registered.

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

1. Evaluate test results by viewing the TV screen and the console log.  After
   tuning to a channel, you will start by seeing:

   <<<<<<<<<<<<<<<<<<<<<<<<<< Source ID: 0x44d >>>>>>>>>>>>>>>>>>>>>>>>>
   
   **************** P A T ****************
   !!!!! PAT is null !!!!!
   **************** P M T ****************
   !!!!! PMT is null !!!!!

2. The display will remain this way until a PAT or PMT is received.  Once a PAT
   or a PMT is received, the screen will look similar to this:

   <<<<<<<<<<<<<<<<<<<<<<<<<< Source ID: 0x44e >>>>>>>>>>>>>>>>>>>>>>>>>

   **************** P A T ****************
   PAT Program 0
       Program Number: 71
       PMT PID: 5
   PAT Program 1
       Program Number: 87
       PMT PID: 1

   **************** P M T ****************
   Program Number: 1
	PCR PID: 67
	No Outer Descriptors
	Elementary Stream 0
		Stream Type: 128
		Elementary PID: 67
		Locator String: ocap://0x44e.0x80
		Descriptor 0
			Tag: 9
			Content Length: 7
			e 0 6e 56 1 1 1 
		Descriptor 1
			Tag: 131
			Content Length: 1
			0 
	Elementary Stream 1
		Stream Type: 129
		Elementary PID: 68
		Locator String: ocap://0x44e.0x81
		Descriptor 0
		    Tag: 9
			Content Length: 7
			e 0 6e 56 1 1 2 
		Descriptor 1
			Tag: 10
			Content Length: 4
			65 6e 67 0 
	Elementary Stream 2
		Stream Type: 134
		Elementary PID: 69
		Locator String: ocap://0x44e.0x86
		Descriptor 0
			Tag: 9
			Content Length: 7
			e 0 6e 56 1 1 3

   Note that it will be somewhat difficult to read the PAT/PMT out of the
   console log due to the large amount of logging going on.  However, the
   screen should be very easy to read.

3. For TableChangeListeners, you must instigate a change to the PAT or PMT
   from the headend while the channel is tuned.  Once OCAP receives the
   change, it will update the display with the new table data, and indicate
   the change type, - 'Modify', next to the table name:

   <<<<<<<<<<<<<<<<<<<<<<<<<< Source ID: 0x44e >>>>>>>>>>>>>>>>>>>>>>>>>

   **************** P A T (Modify) ****************
   PAT Program 0
       Program Number: 71
       PMT PID: 5
   PAT Program 1
       Program Number: 87
       PMT PID: 1

   **************** P M T (Modify) ****************
   Program Number: 1
	PCR PID: 67
	No Outer Descriptors
	Elementary Stream 0
		Stream Type: 128
		Elementary PID: 67
		Locator String: ocap://0x44e.0x80
		Descriptor 0
			Tag: 9
			Content Length: 7
			e 0 6e 56 1 1 1 
		Descriptor 1
			Tag: 131
			Content Length: 1
			0 
	Elementary Stream 1
		Stream Type: 129
		Elementary PID: 68
		Locator String: ocap://0x44e.0x81
		Descriptor 0
		    Tag: 9
			Content Length: 7
			e 0 6e 56 1 1 2 
		Descriptor 1
			Tag: 10
			Content Length: 4
			65 6e 67 0 
	Elementary Stream 2
		Stream Type: 134
		Elementary PID: 69
		Locator String: ocap://0x44e.0x86
		Descriptor 0
			Tag: 9
			Content Length: 7
			e 0 6e 56 1 1 3 


THE TEST PASSED IF:

(a) Both PAT and PMT tables are retrieved for all the channels in the
    channel map.

(b) The TV screen is updated with a 'Modify' event, each time a PAT
    or a PMT change is triggered from the headend.

(c) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) The test fails to display either a PAT or a PMT table for a tuned
    channel.

(b) The test fails to update the screen with PAT/PMT changes, which
    means that the TableChangeListeners do not work.

(c) There are exceptions, failures or errors present in the log.
