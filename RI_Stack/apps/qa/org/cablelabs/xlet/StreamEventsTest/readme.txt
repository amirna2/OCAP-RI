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

The StreamEventsTest xlet performs the following tasks:

(1) Read the Object Carousel file system as indicated by frequency, qam,
    pid, at the root directory of the object carousel.

(2) Identify stream event files in the carousel.

(3) Subscribe to stream events listed in the config.properties file.  Please
    see the TSBroadcaster User Manual for the information on how to create
    stream events on the headend.

(4) Wait for a stream event to be sent out via the TSBroadcaster UI.  When the
    StreamEventsTest receives a stream event, it prints it to the console
    log and to the TV screen.

(5) Unsubscribing from and re-subscribing to events can be tested by pressing
    the digit keys on the remote (1, 2, 3 etc.)  Key 1 subscribes to/unsubscribes
    from the first event listed in the config file;  key 2 subscribes to/
    unsubscribes from the second event, etc.
    Test Case: unsubscribe from an event, then send it out from the
    TSBroadcaster, and make sure it is not received by the xlet.

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS (for the Portland headend):

    app.49.application_identifier=0x000000017303
    app.49.application_control_code=PRESENT
    app.49.visibility=VISIBLE
    app.49.priority=220
    app.49.application_name=StreamEventsTest
    app.49.base_directory=/snfs/qa/xlet
    app.49.initial_class_name=org.cablelabs.xlet.StreamEventsTest.StreamEventsTestXlet
    app.49.service=0x12353
    app.49.args.0=config_file=config.properties
    app.49.args.1=file_system=IBOC
    app.49.args.2=frequency=591000000
    app.49.args.3=prog_num=1
    app.49.args.4=qam=8
    app.49.args.5=carousel_id=25

SAMPLE CONFIG.PROPERTIES PARAMETERS (for the Portland headend):

    event_path_0=TW_EVENTS/TW_EVENT_GROUP_1
    event_name_0=TW_EVENT_1A
    event_id_0=70

    event_path_1=TW_EVENTS/TW_EVENT_GROUP_1
    event_name_1=TW_EVENT_1B
    event_id_1=71

    event_path_2=TW_EVENTS/TW_EVENT_GROUP_2
    event_name_2=TW_EVENT_2A
    event_id_2=72

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. Use the TSBroadcaster to create events on the headend.

2. Configure the test via the hostapp.properties and the config.properties
   files to subscribe to the events you created (see example above).

3. In the QA Test Launcher menu, select the 'Transport Stream Test Service'.

4. Select the StreamEvents test.

5. Using the TSBroadcaster UI, trigger events that the test is configured to
   subscribe to.  Wait for the xlet to receive the events.

6. Test subscribing to / unsubscribing from events, as described in point (5)
   of the 'DESCRIPTION' section.

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate test results by viewing the TV screen.

The screen lists:

(1) All files present on a given object carousel.

(2) Stream event files present on the carousel, e.g.:

    *************************************************************
    STREAM EVENTS XLET: Event Files:
      /oc/f=0x2339f1c0.0x1.m=0x8.@0x19/TW_EVENTS/TW_EVENT_GROUP_1
      /oc/f=0x2339f1c0.0x1.m=0x8.@0x19/TW_EVENTS/TW_EVENT_GROUP_2

    *************************************************************

(3) All the stream events in the DSMCCStreamEvent objects created
    by the xlet, e.g.:

    *******************************
    STREAM EVENTS XLET: Event List:
    TW_EVENT_1A
    TW_EVENT_1B
    TW_EVENT_2A

    *******************************

(4) Stream events the test subscribed to.

(5) Stream events received by the test, after they had been sent out
    from the headend.

THE TEST PASSED IF:

(a) The test is able to subscribe to, receive, and unsubscribe from
    configured events.

(b) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) The test fails to subscribe to, receive, or unsubscribe from
    configured events.

(b) There are exceptions, failures or errors present in the log.
