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

TuneTest is a general tuning program.  Use channel_up and channel_down keys to
change channels. Use keys 1, 2, 3 or 5 for special repeat tuning modes.  The
special case of tuning to an analog channel by frequency, qam, pid is noted in
the next section.

*******************************************************************************
                                   CONFIGURATION:
*******************************************************************************

Three types of channels can be tuned.  The config.properties file contains a
running channel list.  The list stops when it fails to encounter a channel map
increment by one (your list must start at zero and proceed to end, if you skip
and increment, like say 4, the list will have element 0, 1, 2, 3, and nothing
above four).

SAMPLE CONFIG.PROPERTIES PARAMETERS:

  #set min and max delay and interval in msec for repeat tune tests
  min_delay=30000
  max_delay=4500
  interval=1000

  # 699MHz is Object Carousel
  # Source ID's 2700-2709
  # QAM 8
  gen_channel_freq_0=699000000
  gen_channel_program_number_0=7
  gen_channel_qam_0=8 
  gen_channel_name_0=Carousel7
  gen_channel_description_0="Carousel7"

  gen_channel_freq_1=699000000
  gen_channel_program_number_1=8
  gen_channel_qam_1=8 
  gen_channel_name_1=Carousel8
  gen_channel_description_1="Carousel8"

  # Digital Channels tuned by Source ID
  digital_channel_sourceId_0=880
  digital_channel_name_0=ILifeTV
  digital_channel_description_0="I-Life TV"

  digital_channel_sourceId_1=881
  digital_channel_name_1=ILifeTV
  digital_channel_description_1="Wisdom TV"

  analog_channel_sourceId_1=3eb
  analog_channel_name_1=Weather
  analog_channel_description_1="Weather"

SCALED VIDEO CONTROL:

  By default, video comes up full-screen (un-scaled).  To scale the initial
  video, set the following property in the config.properties file:

  video_scaling=X,Y,HORZ,VERT

  X, Y, HORZ, and VERT are float constants between 0.0 and 1.0, inclusive.
  X and Y represent the screen location where the video should appear.
  HORZ and VERT represent the horizontal and vertical scaling factors.
  
  Example:  "0.25,0.25,0.5,0.5" means to display the video 1/4 from the left
  and top of the screen, scaled to 50% on both the horizontal and vertical
  axes.

TUNING TO ANALOG CHANNELS BY FREQUENCY, QAM, PID:

  The following is a special case where analog channels can be tuned by freq,qam,
  pid.  Set QAM to 255 (magic number).

  # magic tuning for atg-west
  # analog channel by freq, qam, pid
  # magic number qam=255
  gen_channel_freq_1=699000000
  gen_channel_program_number_1=9
  gen_channel_qam_1=8
  gen_channel_name_1=WeatherChannel
  gen_channel_description_1="Weather Channel"

PERFORMANCE LOGGING:

  To enable performance logging in the config.properties file set the variable
  PerfReporter to the ip address of the UDPPerfLogServer and set the
  PerfReporterPort to the port that the UDPPerfLogServer is listening on.
  To disable performance logging which should be the default in the
  config.properties file, set PerfReporter to no value, like so PerfReporter=.  

  Since performance logging opens a UDP socket, it requires permission from
  the SecurityManager.  The AppId of the TuneTestXlet in the hostapp.properties
  file should be have the last 4 digits in the 7000 range. Apps with Ids in this
  range are granted permissions to open sockets by the SecurityManager.  The
  AppId of the TuneTestXlet should be set in this range by default.  If
  performance logging is turned off, this is not an issue.

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

  app.14.application_identifier=0x000000015204
  app.14.application_control_code=PRESENT
  app.14.visibility=VISIBLE
  app.14.priority=220
  app.14.application_name=TuneTest
  app.14.base_directory=/snfs/qa/xlet
  app.14.initial_class_name=org.cablelabs.xlet.TuneTest.TuneTestXlet
  app.14.service=0x12348
  app.14.args.0=config_file=config.properties

*******************************************************************************
                                   TEST CONTROL:
*******************************************************************************

Tune up and down surfs the complete list of channels (combined from three
configuration methods.  The auto-tune modes (keys 2, 3 and 5) are used to
stress test the stack.

To tune to a channel directly press ENTER, type in the channel sourceId into
the small text box in the right-hand corner, then press ENTER again.

#1) Re-reads the channels file again, allowing you to change the list without
    rebooting.

#2) Tunes up and down with random waits between min_delay and 15 seconds.

#3) Tunes up and down with min_delay between.

#5) Tunes randomly through list at an increasing delay of 'interval' ms between
    min and max delay.  Delay decreases once the max is reached and continues
    down the the min, where it starts increasing again.

The xlet now also checks to see if min_delay, max_delay, and interval are not
specified. If omitted from the config file, they will default to values of:

    min_delay=5000
    max_delay=30000
    interval=100

Current tune times for unencrypted channels are 1 and 5 seconds for min and
max times respectively.

*******************************************************************************
                                   EVALUATION:
*******************************************************************************

Evaluate by viewing the tv screen. Object Carousel channels, BFS channels, and
encrypted channels usually have no video.
