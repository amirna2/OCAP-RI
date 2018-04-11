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

We strongly recommend using TuneTest or RiExerciser to demonstrate and test 
the tuning functions of the OCAP RI.  

TuneTest tunes by frequency, pid and qam.  It also has a feature for tuning to
analog channels.

RiExerciser/RiScriptlet tunes, records and publishes video with both server
and client side HN support.

See each xlet's readme.txt file for more details.  Code may be found at:
$OCAPROOT/apps/qa/org.cablelabs.xlet


*******************************************************************************
                                    DESCRIPTION:
*******************************************************************************
 
The WatchTV xlet tests the following features:

1. Tuning.
2. Recording.
3. Time Shift.
4. PIP.

*******************************************************************************
                                   CONFIGURATION:
*******************************************************************************

The xlet provides two options for building the channel map:

1. Use the JavaTV channel map by setting 'use_javatv_channel_map=true' in the
   config.properties file.

2. Configure your own channel map via the config.properties file, and set
   'use_javatv_channel_map=false'.

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.15.application_identifier=0x000000016004
app.15.application_control_code=PRESENT
app.15.visibility=VISIBLE
app.15.priority=220
app.15.application_name=WatchTV
app.15.base_directory=/snfs/apps/watchtv
app.15.initial_class_name=org.cablelabs.xlet.watchtv.WatchTVXlet
app.15.service=0x12348
app.15.args.0=config_file=config.properties

SAMPLE CONFIG.PROPERTIES PARAMETERS:

# used by watchTV
session_gw=192.168.137.13
service_gw=BMSsvcgateway
service=MOD
movie_name=iN_DEMAND::MOD::TheMatrixReloadedWidescreenpackage::The Matrix Reloaded Widescreen feature
sc_creation=SC_NEW
ts_behavior=TS_REUSE

use_javatv_channel_map=false

gen_channel_freq_0=639000000
gen_channel_program_number_0=2
gen_channel_qam_0=16 
gen_channel_name_0=FitTV
gen_channel_description_0="Fit TV"

digital_channel_sourceId_0=880
digital_channel_name_0=ILifeTV
digital_channel_description_0="I-Life TV"
digital_channel_num_0=1

analog_channel_sourceId_0=3eb
analog_channel_name_0=Weather
analog_channel_description_0="Weather"
analog_channel_num_0=10

*******************************************************************************
                                   TEST CONTROL:
*******************************************************************************

The xlet implements the following remote control key presses:

CHANNEL_UP   - Tune to the next channel in the channel map.

CHANNEL_DOWN - Tune to the previous channel in the channel map.

INFO         - Show / hide the information banner at the bottom of the screen.

GUIDE        - Show / hide the channel map.

0 THROUGH 9  - Change a channel by typing in a channel number.

RECORD       - Record current service.

STOP         - Stop recording current service.

A            - Play last recording.

PAUSE        - Pause current service or recording.

PLAY         - Play current service or recording.  Typically used after a
               PAUSE, FAST FORWARD or REWIND.

FAST FORWARD - Fast forward current service or recording.

REWIND       - Rewind current service or recording.

LIVE         - Watch live TV.

PIP_TOGGLE   - Turn PIP on / off.

PIP_MOVE     - Change position / size of the PIP window.

PIP_UP       - Tune PIP to the next channel in the channel map.

PIP_DOWN     - Tune PIP to the previous channel in the channel map.

DISPLAY_SWAP - Swap the main window display with PIP.

*******************************************************************************
                                   EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.
