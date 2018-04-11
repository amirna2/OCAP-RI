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

The SectionFilterResourceTest Xlets allow you to interactively create, start,
and stop DAVIC section filters to observe resource contention handling behavior.
This test consists of a test runner xlet and multiple test xlets.

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

# SectionFilterResourceTest:

app.45.application_identifier=0x000000016610
app.45.application_control_code=PRESENT
app.45.visibility=VISIBLE
app.45.priority=220
app.45.application_name=SFResourceTestRunner
app.45.base_directory=/snfs/qa/xlet
app.45.initial_class_name=org.cablelabs.xlet.SectionFilterResourceTest.SFTestRunnerXlet
app.45.service=0x12353
app.45.args.0=testXlet=0x000000014611
app.45.args.1=testXlet=0x000000014612
app.45.args.2=testXlet=0x000000014613
app.45.args.3=rejectXlet=0x000000014613
app.45.args.4=configFile=config.properties


# SFTest1: This app is run by the SectionFilterResourceTest

app.46.application_identifier=0x000000014611
app.46.application_control_code=PRESENT
app.46.visibility=INVISIBLE
app.46.priority=150
app.46.application_name=SFTest1
app.46.base_directory=/snfs/qa/xlet
app.46.initial_class_name=org.cablelabs.xlet.SectionFilterResourceTest.SFTestXlet
app.46.service=0x12353
app.46.args.0=x=45
app.46.args.1=y=24
app.46.args.2=width=184
app.46.args.3=height=220
app.46.args.4=runner=0x000000016610


The SFTestRunnerXlet takes the following xlet arguments:

testXlet=0x<ORG_ID><APP_ID)

  This argument allows the test runner to launch multiple instances of SFTestXlet.
  For each testXlet argument provided, the test runner will have the ability to
  launch the xlet described by the ORG_ID and APP_ID values.

rejectXlet=0x<ORG_ID><APP_ID>

  The SFTestRunnerXlet also functions as the monapp in this test.  It registers
  with the ResourceManager to perform resource contention.  This argument allows
  you to specify an xlet that will have all attempts to start a section filter
  unconditionally rejected by the monapp.

The SFTestXlet takes the following xlet arguments:

x=<X_POS>
y=<Y_POS>
width=<WIDTH>
height=<HEIGHT>

  These arguments allow you to specify the location and size of the xlet's
  display window in screen coordinates

runner=0x<ORG_ID><APP_ID>

  This argument allows the test xlets to send events back to the test runner
  to report the status of tests being conducted.  NOTE: This functionality is
  not currently used.  In the future, it may be possible to put predictive
  logic in the test runner that allows it to predict the outcome of various
  tests.  The test xlets would report their state and events back to the test
  runner to allow it to predict whether or not the tests have passed or failed.


SAMPLE CONFIG.PROPERTIES PARAMETERS:

#Section Filter Resource Test
sfrt_frequency=591000000

'sfrt_frequency'
  This parameter is used when running the test in the AutoXlet.  It specifies
  the frequency to tune to, which is known to contain an object carousel.
  It is not necessary to set this parameter, when running the xlet manually.

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. Configure the test via the hostapp.properties and the config.properties
   files (see example above).

2. In the QA Test Launcher menu, select the 'Transport Stream Test Service'.

3. Select the SFResourceTestRunner.

4. Upon launching the test runner Xlet, the test information window will appear
   along the bottom of the TV screen.

  TEST RUNNER DISPLAY WINDOW
  - The upper left corner of the the test window displays the remote control
    keys that can be used to tune any of the tuners on the STB to any transport
    stream present on the RF input.
  - The lower left corner of the test window displays the keys used to select,
    launch, pause, and destroy the individual test xlets.
  - The magenta text on the left side of the screen displays the current tuner
    and the currently selected test xlet and its run state.
  - Just to the right of the tuner/xlet info is a list of all the transport
    stream frequencies detected on the RF input
  - On the right side of the display window is a list of elementary stream PIDs
    present on the currently tuned transport stream
  - Once a test xlet has been launched, the center of the screen will display a
    list of commands that can be sent to that test xlet to perform section
    filtering operations

5. To tune the STB tuner to a particular transport stream frequency, select the
   desired tuner <LEFT/RIGHT>, select the desired transport stream frequency
   <CH_UP/CH_DOWN>, and then press <SELECT> to tune.  The test runner will tune to
   the desired frequency and then begin retrieving the PAT and all PMTs on the
   transport stream.  Once the SI is retrieved, the list on the right side of the
   display window will be populated with the PIDs of all elementary streams.

6. Use the <PIP_UP/PIP_DOWN> buttons to select a particular elementary stream PID.
   Anytime a test xlet attempts to start a section filter, the filter will be
   configured to start filtering on the currently selected elementary stream PID.

7. The <8> button can be used to mount an object carousel.  If an object carousel
   is present on a tuned transport stream, you will see it indicated in the
   elementary stream PID list.  Only when the object carousel PID is selected,
   can the object carousel be mounted.  Otherwise, no action will be taken.
   Object carousels have a higher resource priority than any application-created
   section filter.  Mounting an object carousel when there are no section filtering
   resource left on the STB should cause an application filter to be preempted.

   Conversely, the <0> button can be used to unmount a previously mounted object
   carousel 

8. Running Test Xlets

   Once a test xlet has been successfully launched, you will see its display window
   somewhere in the upper portion of the TV screen (as configured by your xlet args).

   TEST DISPLAY WINDOW (from top to bottom)
   - The top of the test display window lists the test xlet's name and priority.
   - The next line indicates the current SectionFilterGroup creation policy.  Any
  
   SectionFilterGroup created will inherit the current policy. (more on this later)
   - The rest of the display is dedicated to showing the filters and filter groups
     that have been created by this test xlet.

   All of the following commands are issued to the currently selected test xlet,
   provided that it has been successfully launched:

   Press <1> to create a section filter group with the curent resource policy.  The
   resource policy contains 2 parts:

   Willing / NotWilling -- SectionFilterGroups all act as DAVIC resources.  This
   means that any application that tries to allocate a SectionFilterGroup must
   provide an instance of org.davic.resources.ResourceClient to the group.  A
   resource contention handler will ask all current resource holders if they are
   willing to give up their resources should a resource contention arise.  This
   policy determines how the application will respond when this SectionFilterGroup
   is asked to give up its resources.

   High / Low -- All DAVIC SectionFilterGroups are, by default, created with HIGH
   priority.  However, upon creation, an application may specify a group as
   having LOW priority.  The resource contention handler will use this priority
   to forcefully free section filter resource during resource contention.  NOTE:
   This priority only affects section filters within the SAME APPLICATION.  The
   resource contention handler will not free LOW priority filter reservations in
   applications other than the requesting application

9. Once a filter group is created you will see it displayed in the test xlet's 
   display window along with its resource policy and its current state.

   Press <B> or <BLUE_COLOR_KEY> to toggle the current priority policy between HIGH
   and LOW

   Press <C> or <RED_COLOR_KEY> to toggle the current willing-to-release policy
   between WILLING and NOT WILLING

   Press <4> to create a new section filter within the currently selected filter
   group.  The section filter and its current state will be displayed in the test
   xlet display window directly below its containing group.  NOTE: At this time, the
   section filter is not associated with any particular elementary stream PID.

   Press <5> to start the currently selected section filter.  The current elementary
   stream PID will be associated with this filter and will be displayed to the left
   of the filter's state.  If the containing filter group is ATTACHED, the filter
   will begin filtering immediately.  If the group is DETACHED or DISCONNECTED, the
   filter will be put in a RUN_PENDING state.  In this state, the filter will start
   automatically when the group is attached to a transport stream.  NOTE: A maximum
   of 2 section filters can be started per filter group.

   Press <2> to attach the currently selected filter group to the currently selected
   transport stream.  Any filters in the RUN_PENDING state will be started.

   Press <3> to detach the currently selected filter group from its transport stream.
   All running filters will be placed back into the RUN_PENDING state

   Press <6> to stop the currently selected section filter from filtering.  The
   section filter will be placed in the STOPPED state.

   Press <7> to change the current filter group selection within the current
   application

   Press <9> to change the current section filter selection within the currently
   selected section filter group

   Press <EXIT> to delete the currently selected section filter group

   Press <SETTINGS> to tune to the frequency listed in the config.properties file.
   Sample config.properties entry: sfrt_frequency=591000000

*******************************************************************************
                                  TEST CASES:
*******************************************************************************

The SectionFilterResourceTest is automated to run the following test cases in
the AutoXlet.  The XletDriver.xml is included in the test directory.

Press <LIST> to list the results of all the test cases below (run the test cases
first, either manually or automatically).

Press <LAST> to print the results of the next test case in the list (run that
test case first).

TEST CASE 1:      
DESCRIPTION:      Test that only two filters per group can be created.
KEY SEQUENCE:     ENTER PLAY 12 45 45 45 95 95
PROCEDURE:        (1) Tune and start SFTest1.
                  (2) Create a filter group and attach it to the stream.
                  (3) Create and start three filters.
                  (4) Make sure the first two filters are still running,
                      after the third one fails to start.
EXPECTED RESULTS: The first two filters run, the third filter fails to start.
CLEANUP SEQUENCE: VK_EXIT


TEST CASE 2:
DESCRIPTION:      Test that only two filters per group can be created.  Then stop
                  one of them, and verify that the third filter can run.
KEY SEQUENCE:     ENTER PLAY 12 45 45 45 96 95 95
PROCEDURE:        (1) Tune and start SFTest1.
                  (2) Create a filter group and attach it to the stream.
                  (3) Create and start three filters.
                  (4) Stop the first filter.  Make sure the second one is still running.
                      Start the third one again.
EXPECTED RESULTS: The third filter should run.
CLEANUP SEQUENCE: VK_EXIT


TEST CASE 3:
DESCRIPTION:      Test that monapp-rejected applications are not allowed to use any
                  filtering resources.
KEY SEQUENCE:     UP_ARROW UP_ARROW PLAY 12 45
PROCEDURE:        (1)In the hostapp.properties, configure the SectionFilterResourceTest
                  to run SFTest3 as monapp-rejected.  For example, set:
                  app.xx.args.x=rejectXlet=0x000000014613,
                  where 0x000000014613 is the appId of SFTest3.
                  (2) Start SFTest3.
                  (3) Create a filter group and attach it to the stream.
                  (4) Create and start a filter.
EXPECTED RESULTS: The filter fails to start.
CLEANUP SEQUENCE: VK_EXIT : VK_DOWN : VK_DOWN


TEST CASE 4:
DESCRIPTION:      Test the 8300HD ability to filter on 6 different PIDs,
                  before section filtering resources run out.
KEY SEQUENCE:     ENTER PLAY 12 45 -45 12 -45 -45 UP_ARROW PLAY 12 -45 -45 12 -45 -45
PROCEDURE:        (1) Tune and start SFTest1 (priority=150).
                  (2) Create a filter group and attach it to the stream.
                  (3) Create and start two filters on different PIDs: both should run.
                  (4) Create another filter group and attach it to the stream.
                  (5) Create and start two filters on different PIDs: both should run.
                  (6) Start SFTest2 (priority=100), and repeat steps (2) through (4).
                  (7) Create and start two filters.
EXPECTED RESULTS: The last filter, or the last two filters created should fail to run.
CLEANUP SEQUENCE: VK_EXIT : VK_EXIT : VK_DOWN : VK_EXIT : VK_EXIT


TEST CASE 5:
DESCRIPTION:      Test that filters with a willing-to-release policy give up
                  their resources, when conflicts exist.
KEY SEQUENCE:     ENTER PLAY C 12 45 -45 12 -45 -45 UP_ARROW PLAY 12 -45 -45 12 -45
                  7 +5 9 +5 ARROW_DOWN +5 9 +5 7 +5 9 +5
PROCEDURE:        (1) Tune and start SFTest1 (priority=150).
                  (2) Change SFTest1's priority to Willing.
                  (3) Create a filter group and attach it to the stream.
                  (4) Create and start two filters on different PIDs: both should run.
                  (5) Create another filter group and attach it to the stream.
                  (6) Create and start two filters on different PIDs: both should run.
                  (7) Start SFTest2 (priority=100), and repeat steps (3) through (6).
EXPECTED RESULTS: The two filters created in SFTest2, Group1 should run,
                  while both filters in SFTest1/Group0, or SFTest1/Group1 get aborted.
CLEANUP SEQUENCE: VK_UP : VK_EXIT : VK_EXIT : VK_DOWN : VK_EXIT : VK_EXIT : VK_COLORED_KEY_0


TEST CASE 6:
DESCRIPTION:      Test that filters with low priority can be preempted by filters
                  with high priority within the same application.
KEY SEQUENCE:     ENTER PLAY 12 45 -45 12 -45 -45 UP_ARROW PLAY B 12 -45 -45 B 12 -45 -45
                  7 +5 9 +5 ARROW_DOWN +5 9 +5 7 +5 9 +5
PROCEDURE:        (1) Tune and start SFTest1 (priority=150).
                  (2) Create a filter group and attach it to the stream.
                  (3) Create and start two filters on different PIDs: both should run.
                  (4) Create another filter group and attach it to the stream.
                  (5) Create and start two filters on different PIDs: both should run.
                  (6) Start SFTest2 (priority=100).
                  (7) Change priority of SFTest2 to Low (it affects Group0).
                  (8) Repeat steps (2) and (3).
                  (9) Change SFTest2 priority for Group1 to High, then repeat steps (2) and (3).
EXPECTED RESULTS: The two filters created in SFTest2, Group1 should run, while
                  both filters in SFTest2, Group0 get aborted.
CLEANUP SEQUENCE: VK_UP : VK_EXIT : VK_EXIT : VK_DOWN : VK_EXIT : VK_EXIT


TEST CASE 7:
DESCRIPTION:      Test that mounting an object carousel will always pre-empt
                  one of the application filter groups, if conflicts exist.
NOTE 1:           Xlet priorities are only be taken into account when the filter request
                  comes from DAVIC.  Filters requested by OC or SITP will preempt ANY
                  filters currently in use by DAVIC regardless of the xlet priority.
NOTE 2:           This test case is dependent on the current Portland channel map, and
                  on having an OC on freq=591, PID=0x120.
KEY SEQUENCE:     CHANNEL_UP ENTER PLAY 12 ---45 -45 12 -45 -45 UP_ARROW PLAY 12 -45 -45
                  ++++++ 8 9 +5 ARROW_DOWN +5 9 +5 7 +5 9 +5
PROCEDURE:        (1) Tune to freq=591 and start SFTest1.
                  (2) Create a filter group and attach it to the stream.
                  (3) Create and start two filters on different PIDs: both should run.
                  (4) Create another filter group and attach it to the stream.
                  (5) Create and start two filters on different PIDs: both should run.
                  (6) Start SFTest2.
                  (7) Repeat steps (2) and (3).
                  (8) Select PID=0x120, which contains an Object Carousel.
                  (9) Mount the Object Carousel.
                  (10)Restart existing filters to verify that at least one
                      filter group lost resources.
EXPECTED RESULTS: One of the existing filter groups is disconnected.
CLEANUP SEQUENCE: VK_EXIT : VK_EXIT : VK_UP : VK_EXIT : VK_DOWN


*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.

THE TEST PASSED IF:

(a) When running one of the Test Cases listed above, the SFResourceTestRunner
    should behave exactly as described in the section 'EXPECTED RESULTS' for
    each test case.

(b) There are no unexpected exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) Test case results did not match the 'EXPECTED RESULTS' for that test case.

(b) There are unexpected exceptions, failures or errors present in the log.



*******************************************************************************
                               VISION WORKBENCH NOTES:
*******************************************************************************

The VISION Workbench environment does not limit the amount of filters that can
run on unique pids.  Therefore, only test cases 1, 2 and 6 make sense in VISION
Workbench, since the remaining test cases test resource contension.
