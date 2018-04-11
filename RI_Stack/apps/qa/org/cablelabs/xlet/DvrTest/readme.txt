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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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
Description: 
This test contains a collection of sub-tests which exersize the DVR stack. 

Configuration Parameters:

All of the tests contained within the DVR test xlet utilize an array of locators for their various tuning parameters. The locators are specified in the xlet args as shown above (Service:XXX - the code just takes anything following the Service: and prepends ocap:// to create the locator). 
Most of the tests will use the first locator, with a few using additional locators (eg, for simultaneous records).

This will change based upon open loactors, check with Cullen to see what the status is of the listed locators

DVR Test Runner now has the capability to record by frequency, program#, QAM

All the parameters are specified in the config file. It is required to have 5 channels listed in DVRTestRunner to have it work properly

It is optional to have the frequency, program#, QAM. If DVR_by_FPQ is set to true, DVR_FPQ_# values will be used. Otherwise DVR_sourceId_#
will be used. 

#DVR Tests
DVR_by_FPQ=TRUE
DVR_sourceId_0=0x87B
DVR_sourceId_1=0x3eb
DVR_sourceId_2=0x429
DVR_sourceId_3=0x86f
DVR_sourceId_4=0x871
DVR_FPQ_0=471000000,2,255
DVR_FPQ_1=63000000,1,255
DVR_FPQ_2=561000000,3,255
DVR_FPQ_3=801000000,4,255
DVR_FPQ_4=351000000,5,255

Setup:
-------------
To run DVRTestRunner, make sure the following changes are done in the mpeenv.ini for proper testing


# Set to true to ignore XAITs (monitor app + unbound apps)
OCAP.xait.ignore=true

# For regular, repeating TVTimers, this value represents the maximum tardiness
# (in milliseconds) of a timer task.  If a timer task is tardy, then a system
# time change may have taken place.  In this case, the timer task will be
# rescheduled to its next execution time in the future based on the new
# system time.  The system default is 5 minutes.
#OCAP.timer.maxtardiness=300000

# Set to true to enable I15 and I16 XAIT
OCAP.xait.I15=true

# Set to true to ignore AITs (bound apps)
OCAP.ait.ignore=true

# tell the RecordingManager not to clean up orphaned recordings
org.cablelabs.impl.manager.recording.leaveOrphans=true

# Define the amount of time that the RecordingManager should wait for an
# explicit start call to initiate schedule processing, in milliseconds.
# The default value should be 5 minutes (or 300,000 milliseconds,
# according to the SCR 20051104)
OCAP.dvr.recording.delayScheduletimeout=0

#### DVR CONFIGURATION ####

# define extra space required by TSB allocation (in bytes)
DVR_TSB_EXTRA_SPACE=616038400

# These values are in minutes and control the amount of idle time
# before DVR devices are placed in a low power state.  Set to 0
# to disable  low power mode.
DVR_ON_LOW_POWER_IDLE_TIME=10
DVR_STANDBY_LOW_POWER_IDLE_TIME=3

# Amount of time in ms that the slowest DVR device takes to resume
# from a low power state.  Set to 0 to disable.  Default 2 minutes.
DVR_LOW_POWER_RESUME_TIME=120000

# when set to 1, sess_DisableUNSession() will be called when MPE starts
MPE_TARGET_DISABLE_UNSESS=1

# Default power state {1, 2} for {on, standby}
DEFAULT_POWER_STATE=2

Afterward use the snfs_hostapp and config files for debug use.

Most of these tests can be run in automation. The following tests cannot:

Test 3: Test Interrupted Recordings : if no conflicts
Test 4: Test Interrupted Recordings : with conflicts
Test 5: Test Persistent Purge
Test 6: Test Persistent Recording Contention: if no conflicts
Test 7: Test Persistent Recording Contention: with conflicts
Test 8: TestInterruptedRecordingCheck_wConflicts
Test 9: TestInterruptedRecordingCheck_noConflicts
Test 10: TestPersistentContentionCheck : if no conflict
Test 11: TestPersistentContentionCheck : with conflict
Test 12: Test Failed Delayed Start Recording
Test 13: Test Failed Delayed Start Recording - Pre boot run
Test 14: TestStartRecordingManager
Test 15: Test Basic Low Power Resume Recording
Test 16: TestPropertiesChangesOnCompletedRecording - create recording w/ AppData
Test 17: TestPropertiesChangesOnCompletedRecording - find recording and modify
Test 18: Fill the Disk but Stop early
Test 19: Fill the Disk
Test 20: Check large recordings on disk from Fill the Disk

These test should be under the Manuual Test Group

Test Control:
-------------
View the console 

<info>    - lists the tests that are available
<1...0>   - enter group #/test #
<select>  - allows runs the test corresponding to the current test number
<left>    - backs up the group #/test #
<A>       - adds the test corresponding to the current test number into the looping test list
<B>		  - deletes the list
<C>       - runs the tests in the looping test list, one after the other, indefinitely
<LIST>    - prints the tests in the looping test list
<GUIDE>   - selects the Group list

To Use:
--------------
On start up, select the group of tests to use. This will then load in the tests needed to run. Loading is complete
once the menu of tests are displayed. To go back to the Group menu, press GUIDE. To either get the Group Menu while 
in the Group Menu state or the Test Menu when accessing a Group, press INFO. Also when setting up a test loop,
test from various groups can be enterer into the list.   

Test Specifics
--------------
The test cases use event object to schedule out actions and checks. These Object reside in DvrTest.java file.
Some NotifyShell objects can be found as inner classes located within indvdual test cases

Event NotfyShell Objects:

 PrintRecordings 
 PrintOut
 CountRecordings
 DeleteRecordings
 Record
 Reschedule
 StopRecording
 Cancel
 SelectService
 StopService
 DestroyService
 TuneNetworkInterface
 SIChecker
 RecordedSIChecker
 StopBroadcastService
 DestroyBroadcastService
 SelectRecordedService
 SelectRecordedServiceUsingJMFPlayer
 SetMediaTimeTest
 ConfirmRecordingSuccess
 ConfirmRecordingInProgress
 ConfirmRecordingStateCount
 ConfirmRecordingReq_CheckState
 ConfirmRecordingDeletion
 RecordBySC
 FilterCompleted
 FilterPendingNoConflict
 SortStartTime
 RegisterResourceContentionHandler
 GetPrioritizedResourceUsages
 AddEpisode
 AddSeason
 NewSeriesRoot
 DeleteRecordedService
 DeleteRecordingRequest
 PrintEventListElems
 CancelRecordingRequest

This list is

Common Methods used:

reset() – resets EventScheduler
findObject(String key) 
findKey(Object object)
insertObject(Object item, String key)
removeObject(Object item)
dumpDvrRecordings() - prints out all  recording requests in the database
deleteAllRecordings() – deletes all recording requests from the database
printRecording(RecordingRequest rr) – prints info on recording 
    (soon to be removed: do not add more functionality)
diskFreeCheck() – checks for disk space and flags if not enough 
initSC() – creates a service context object
getNameForResourceUsage(final ResourceUsage ru)

Procedure:
-----------
Make sure that the Recording Manager variable is changed to allow it to be started at the loading of the stack.
Evaluate by: Most test can be evaluated by examining logs for "FAILED" and "PASSED". In release mode,
the message is printed out to the screen. 

the following is a brief desciption and steps to run each test which is documented in the Wiki:

  Group 1 Tests

  Test 1: Delete All Recordings  - Deletes all recording requests that are currently stored on the STB
   
  Test 3: TestScheduledDigitalRecording - Tunes to a digital channel and records from it via Network 
          Interface.  Run the test and see the result.

  Test 4: TestScheduledAnalogRecording - Tunes to an analog channel and records from it via Network 
          Interface.  Run the test and see the result. 
    
  Test 5: Test Recording Playback - Tunes to a channel via Network Interface and records. After recording
          is finished, it is then played back through a Service Context. Verification of playback and test result needed.

  Test 6: Test Recording Playback Using JMF -   Similar to TestRecordingPlayback except the playback is
          presented in a JMF object. Verification of playback and test result needed.
   
  Test 7: Test Recording Playback Set Media Time – Similar to TestRecordingPlayback except that there is
          a call to set the playback 10 seconds into the future. Verification of playback and test result needed.       

  Test 8: TestBasicConsecutiveRecordings – Schedules a series of 20 second recordings 10 seconds apart from each other
          Run the test and see the result.
        
  Test 9: TestBasicConsecutiveRecordings2? - distinct locators - Schedules two 20 second recordings 10 seconds
          apart from each other on different channels. Run the test and see the result Run the test and see the result.       

  Test 10: TestSimultaneousRecordings? - distinct locators – Schedules 2 recordings on different channels to record at 
  the same time 5 times in a row  Run the test and see the result. Run the test and see the result.

  Test 11: TestAdjacentRecordings -  Schedules a series of 30 second recordings back to back. Run the test and see the result.    

  Test 12: Test Basic TS Recording:ocap://0x868 – Tunes by ServiceContext with an active TSB and creates a 30 second 
  recording from the TSB that is in the past. Verification of service presentation and playback and test result needed.       

  Test 13: Test Immediate TS Recording (recording <now> bug):ocap://0x868  Similar to above test but the recording start 
  time is at time of converting from TSB. This action happens at the end.
       
  Test 14: Test Basic TS Recording:ocap://0x3eb Same as test 12 but on a different channel
         
  Test 15: Test Immediate TS Recording (recording <now> bug):ocap://0x3eb Same as test 13 but on a different channel
      
  Test 16: Test TSBRecording NIRecording  Records off of a presenting service context while having an ongoing recording by 
   network interface. Check is done by viewing partial playback of both videos after the presenting service context is destroyed.
   Pass/Fail messaging at the end of the test run  SPECIFIC GATING TEST Check for pass/fail. 
    
  Test 17: TestSCrecordingTuneAway1 - Checks for when a buffered SC is tuned to a different service while an
   ongoing instant recording is in progress - recording is set to RECORD_WITH_CONFICTS
 
  Test 18: TestSCrecordingTuneAway2 - Checks for when a buffered SC is tuned to a different service while an
   ongoing instant recording is in progress - recording is set to RECORD_IF_NO_CONFICTS
   
  Test 19: Test Overlapping Entries  Checks for overlapping entries though call to Recording Manager. 
   Recordings do not execute and are cancelled. SPECIFIC GATING TEST Check for pass/fail. 
    
  Test 20: Test Interrupted Recordings:if no Conflicts.  Test that is used for checking the state of recordings under the 5 cases – A recording 
  who's start and stop time are before a reboot. Currently active  recording that has this stop time during a reboot, currently
  active recording who has its start time before a reboot but will stop sometime after the reboot cycle, a recording whos start
  and stop time are during the reboot cycle, a recording who’s start time is during a reboot,and a recording that is scheduled
  shortly after reboot. 
  The procedure of this test is as such:

    1. Run Delete All Recordings 
    2. Run Test Interrupted Recordings and wait till message “Reboot box now - Leave down for 30 seconds.” 
    3. Shut down box and wait 60 seconds 
    4. Power up box and Run DVRTestRunner?. The Record LED should begin some time during bootup. 
    5. Run TestInterruptedRecordingCheck_wConflicts - check for pas or fail
    6. Delete all recordings when finished.

       Note: There may be a chance that the oldest recording goes to the incomplete state. Reverify in relase if this is true.

  Test 21: Test Interrupted Recordings:with Conflicts.  Test that is used for checking the state of recordings under the 5 cases – A recording 
  who's start and stop time are before a reboot. Currently active  recording that has this stop time during a reboot, currently
  active recording who has its start time before a reboot but will stop sometime after the reboot cycle, a recording whos start
  and stop time are during the reboot cycle, a recording who’s start time is during a reboot,and a recording that is scheduled
  shortly after reboot. 
  The procedure of this test is as such:

    1. Run Delete All Recordings 
    2. Run Test Interrupted Recordings and wait till message “Reboot box now - Leave down for 30 seconds.” 
    3. Shut down box and wait 60 seconds 
    4. Power up box and Run DVRTestRunner?. No recordings shall start up
    5. Run TestInterruptedRecordingCheck_ifNoConflicts - check for pas or fail
    6. Delete all recordings when finished.

  Test 22: Test Persistent Purge   Test designed to check if a recoridng requests expiration persists after reboot 
  			and to test if the purge timer is activated after reboot. To test, this requires commenting and uncommenting the 
  			purge timer variable located in the mpeenv.ini The procedures are as followed:

	1. Delete all recordings if possible 
	2. Start the test and wait for it to complete w/ message of reboot 
	3. Reboot the box 
	4. Run UtilPrintRecording? - there should be 1 recording in the DELETED_STATE if STB was clean at start 
	5. Delete all recordings if possible 
  
  Test 23: Test Persistent Conflict if no conflicts  Schedule 3 recordings at the same time
                         *  The first two should fire off and go to the INCOMPLETE_STATE
                         *  The last should be in the FAILED_STATE
                         
    1. Delete all recordings if possible 
	2. Start the test and wait for it to complete w/ message of reboot 
	3. Reboot the box 
	4. Run Test Persistent Contention Check: if no conflict under Manual Tests
	The screen should show the following recordings:
       IN_PROGRESS_INCOMPLETE_STATE 
       IN_PROGRESS_INCOMPLETE_STATE
       FAILED_STATE w/ reason POWER INTERRUPTION         
      
 
  Test 24: Test Persistent Conflict with conflicts  Schedule 3 recordings at the same time
                         *  The first two should fire off and go to the INCOMPLETE_STATE
                         *  The last should be in the FAILED_STATE
                         
    1. Delete all recordings if possible 
	2. Start the test and wait for it to complete w/ message of reboot 
	3. Reboot the box 
	4. Run Test Persistent Contention Check: with conflicts under Manual Tests
	The screen should show the following recordings: 
       IN_PROGRESS_INCOMPLETE_STATE 
       IN_PROGRESS_INCOMPLETE_STATE
       IN_PROGRESS_ERROR_STATE w/ reason POWER INTERRUPTION   

  Test 25: Test Basic Record and Cancel Recordings do not execute and are cancelled. Schedules
	 a recording and then cancels prior to start time.  Check for pass/fail. 
       
  Test 26: Test Basic Schedule. Recordings do not execute and are cancelled. Check for pass/fail. 
        
  Test 27: Test Basic Recording Alert Listener. Checks whether or not the registered listenter is called.
	    Check for pass/fail.

  Test 28-29: Test Fill The Disk
	1. Delete all recordings if possible 
	2. Start the test and wait for it to complete w/ message of reboot
	3. Reboot STB
	4. Run Check large recordings on disk from Fill the Disk
	5. Verify that tere are 2 segments and the recordings are in IN_PROGRESS_INCOMPLETE
        
  Test 30: TestMgrGetEntriesComplete_B2102_1 SPECIFIC GATING TEST Schedules a series of recordings 
       and verifies entries created are complete. Check for pass/fail.  
        
  Test 31: TestMgrGetEntriesPending_B2102_2 SPECIFIC GATING TEST Recordings do not execute 
       and are cancelled. Check for pass/fail.
          
  Test 32: TestSortRecordingListTimeOrderB2102_3 SPECIFIC GAITING TEST Recordings do 
       not execute and are cancelled. Check for pass/fail.
         
  Test 33: TestInProgToInsufSpace_B2102_4 SPECIFIC GATING TEST  Check for pass/fail.
       Verifies the trasition of IN_PROGRESS_INSUFFICENT_SPACE state from IN_PROGRESS state though
       the scheduling of a large recording where there is not enough disk space for two ongoing recordings
       
  Test 34: Test Insuf To In Prog B2102 5 SPECIFIC GATING TEST  Check for pass/fail.  
       Verifies the trasition of IN_PROGRESS state from IN_PROGRESS_INSUFFICENT_SPACE state though
       the deletion of a large recording where there is not enough disk space for two ongoing recordings             
  
  Test 35: Test Incomplete State Test B2102 6  SPECIFIC GATING TEST This test is used to test out a 
           recording going to the INCOMLETE_STATE from IN_PROGRESS_INSUFFICIENT_RESOURCES. To test on a nearly enpty
           STB run the following commands (in quotes) in the serial terminal progrm prior to test run:
           
           "dvr settsb" 
           Enter filename: "filler"
           Enter bitrate: "50000"
           Enter duration: "10000"
           "dvr settsb" 
           Enter filename: "filler2"
           Enter bitrate: "50000"
           Enter duration: "10000"
           "dvr settsb" 
           Enter filename: "filler3"
           Enter bitrate: "50000"
           Enter duration: "5500"
           
           This will leave roughly 800 - 900 clusters left (under 2 GBytes) prior to launching the stack. In running the test,]
           please leave a STB alone for 30 minutes to record. To check on the status of space left type
           "dvr space". 
           
           Check for pass/fail.
  
  Test 36: TestAppData: Simple Test for:addAppData(),getKeys(),getAppData(),removeAppData
   Tests out calling the methsds for a given recording request.
   Recordings do not execute and are cancelled. Check for pass/fail. 
       
  Test 37: Tests ParentRecordingRequest addAppData,getAppData,getKeys,removeAppData functionality. 
   Similar as above but done of a Parent Recording Request
   Recordings do not execute and are cancelled. Check for pass/fail. 
        
  Test 38: Test if addAppData supports the expected 64 maximum keys 
   Recordings do not execute and are cancelled. Check for pass/fail. 
         
  Test 39: Tests addAppData IllegalArgumentException when Data exceeds implementation limit 
   Recordings do not execute and are cancelled. Check for pass/fail. 
         
  Test 40: Test addAppData generates exception if apps data entries is exceed > 64 
   Recordings do not execute and are cancelled. Check for pass/fail. 
         
   Tests 41 through 55 are tests containg recordings that will
   cause the Resource Contention Handler to get envoked. All tests
   use the state of the recordings as a check against whether the 
   the tests have passed or failed.
      
  Test 41: Test Simple Scheduled Recording Contention
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
  Test 42: Test SimpleScheduledRecordingContentionHandler
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf 
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
  Test 43: Test OverlappingScheduledRecordingContention1 
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled.  Check for pass/fail. 
          
  Test 44: Test OverlappingScheduledRecordingContention2 
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
   The following tests (45-48) verify if deletion of a recording request will 
   chnge its state

  Test 45: Test ScheduledRecordingContention_5 
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
  Test 46: Test ScheduledRecordingContention_6 
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
        
  Test 47: Test ScheduledRecordingContention_7 
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
  Test 48: Test ScheduledRecordingContention_8 
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 

   The following tests (49-55) verify if the states of the recording requests change
   upon rescheduling of one recording request
          
  Test 49: Test SimpleRescheduledRecording1
     Please refer to  Visio-ScheduledRecordingTestScenarios.pdf 
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
  Test 50: Test SimpleRescheduledRecording2
     Please refer to  Visio-ScheduledRecordingTestScenarios.pdf  
    Recordings do not execute and are cancelled. Check for pass/fail. 
         
  Test 51: TestOverlappingRescheduledRecording 
     Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
  Test 52: TestOverlappingRescheduledRecording2 
     Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
          
  Test 53: TestMultOverlapRescheduledRecording 
     Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 
  
  Test 54: TestOverlappingRescheduledRecording3 
    Please refer to  Visio-ScheduledRecordingTestScenarios.pdf
   Recordings do not execute and are cancelled. Check for pass/fail. 

  Test 55: TestRecordingReservationReassignment-1 
    Please refer to  Visio-ScheduledRecordingTestScenarios.pdf  
   Recordings do not execute and are cancelled. Check for pass/fail. 

  Test 56: TestRecordingReservationReassignment-2
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf 
   An onging recording is intercepted by a new scheduled recording    
   Check for pass/fail. 

  Test 57: SCHEDULE_SERIES Test is for basic hierachial recording request structure can be scheduled.
   Check for pass/fail. 
         
  Test 58: secExpirationScenario_1 Test that verified expiration of a recording request and purging.
  	 NOTE: requires the purge timer variable to be uncommented to allow purge to happen 20 seconds after 
  	 the recording has expired. If not uncommented, test will fail. Check for pass/fail.
 
   For tests 59 - 66 please refer to Visio-ScheduledRecordingTestScenarios.pdf         
  Test 61: Delete at root level  Recordings do not execute and are cancelled. Check for pass/fail.        
  Test 62: Test DeletionDetails.UserDeleted Recordings do not execute and are cancelled. Check for pass/fail. 
  Test 63: Test DeletionDetails.Expired  Recordings do not execute and are cancelled. Check for pass/fail. 
  Test 64: Test DeletionDetails.(s4) Recordings do not execute and are cancelled. Check for pass/fail.
  Test 65: Test DeletionDetails.(s5)  Check for pass/fail.
  Test 66: Test DeletionDetails.(s6)  Check for pass/fail.
  Test 67: Test DeletionDetails.(s7)  Check for pass/fail.
  Test 68: Test DeletionDetails.(S8)  Check for pass/fail.        


  Test 69: Test Series Reschedule. Recordings do not execute and are cancelled.  Check for pass/fail. 
      
   Gating Tests for 0.9 - as specified in the Gating Requirements documentation:
    A: Scheduled recording starts in 1 minute on service A
    B: Scheduled recording strats in 2 minutes on service B
    C: Buffered Service Context that tunes in 20 seconds on service C
   The arrangement of the letter determines prioritiation. Thus the last letter in the
   grougp of three will not be fullfilled
  Test 70: GatingTest?-RecordingandServiceConflict_ABC  SPECIFIC GATING TEST  Check for pass/fail.                 
  Test 71: GatingTest?-RecordingandServiceConfilct_ACB  SPECIFIC GATING TEST  Check for pass/fail.
  Test 72: GatingTest?-RecordingandServiceConfilct_CBA  SPECIFIC GATING TEST  Check for pass/fail.
      
   Gating Tests for 0.9 - as specified in the Gating Requirements documentation:
    A: Scheduled recording starts in 1 minute on service A 
    B: Network Interface that will be reserved in 40 second on service B
    C: Buffered Service Context that tunes in 10 seconds on service C
   The arrangement of the letter determines prioritiation. Thus the last letter in the
   grougp of three will not be fullfilled          
  Test 73: TestRecordingAndNIConflict_ABC SPECIFIC GATING TEST   Check for pass/fail. 
  Test 74: TestRecordingAndNIConflict_ACB SPECIFIC GATING TEST   Check for pass/fail.
  Test 75: TestRecordingAndNIConflict_CBA SPECIFIC GATING TEST   Check for pass/fail.
   
   As above
   A: Scheduled recording starts in 1 minute on service A
   B: Scheduled recording strats in 2 minutes on service B
   C: Scheduled recording starts in 1 minute on service C
   D: Buffered Service Context that tunes in 20 seconds on service C
   The last two items shall fail to be fullfilled
  Test 76 TestRecordingAndService_ABCD Check for pass/fail.

  Test 77: Test Basic Low Power Resume Recording - Listening test - Upon start of
    test, drive will spin up and test will schedule a recording.
    If the drive was already active, there will be no change 
    The test will wait for the drive to spin down and print a
    notification at about the time the drive should be spinning up. Another notification
    will be printed when recording should be starting. Note that the drive may
    not always spin down due to other activity on the STB, in which case the
    test should just be re-executed. Verification is as such:

    1. Start test
    2. Upon message "Drive will spin up now, but should spin back down in about 3 minutes", 
       a whir from the hard drive should be made(unless already active)
    3. After 3 minutes, the hard drive will spin down
    4. Drive will spin up again upon the following message:
       "Drive should spin up now.  Recording starts in about 2 minutes" 
    5. Recording should become active.

  Test 78: TestRetentionPriority1 Schedules 5 recordings with varying lenghts, expiration times,
    and retention priority settings

      Recording 1
    Start time - 30 sec from 'now' (current system time)
    duration - 70 sec
    task trigger time - 500 msec
    Expire - 20 sec from start time
    rentention priority set to 'DELETE_AT_EXPIRATION'

      Recording 2
    Start time - 60 sec from 'now' (current system time)
    duration - 60 sec
    task trigger time - 510 msec
    Expire - 60 sec from start time
    rentention priority set to 1   

      Recording 3
    Start time - 100 sec from 'now' (current system time)
    duration - 40 sec
    task trigger time - 520 msec
    Expire - 60 sec from start time
    rentention priority set to 1   

      Recording 4
    Start time - 200 sec from 'now' (current system time)
    duration - 30 sec
    task trigger time - 530 msec
    Expire - 120 sec from start time
    rentention priority set to 2 

      Recording 5
    Start time - 300 sec from 'now' (current system time) 
    duration - 60 sec
    task trigger time - 540 msec
    Expire - 120 sec from start time
    rentention priority set to 2

    The Recordings shall go to the following states:
    Recording 1 - DELETED_STATE
    Recording 2 - COMPLETED_STATE
    Recording 3 - COMPLETED_STATE
    Recording 4 - COMPLETED_STATE
    Recording 5 - COMPLETED_STATE

    Check for pass/fail. Test requrires space for given recordings
                      
  Test 79: TestRetentionPriority2 - This is a recording that is
    in_progress at the time it expires. The recording should be
    terminated and the recorded service should be deleted.

    Check for pass/fail.

  Test 80: TestRetentionPriority3 - This is a recording that is
    in 'in_progress' state at the time it expires. Since the 
    retention priority is NOT set to delete_at_expiration the
    recording gets added as purgable to retention manager.

    Check for pass/fail.

  Test 81: TestRetentionPriority4 - Same as TestRetentionPriority3
    except that retention priority is set to 2

  Test 82: TestPlaybackExpiredRecording1 - Schedules the recording with 
    the following parameters

       Recording 1
    Start time - 1 sec from 'now' (current system time)
    duration - 30 sec
    task trigger time - 500 msec
    Expire - 60 sec from start time
    rentention priority set to delete_at_expiration 

    After recording is completed, a state check is done and the recording is
    played back
    After expiration a check is done to make sure the recording is in the deleted state

    Visual of playback and pass/fail messaging must be checked.  

  Test 83: TestPlaybackExpiredRecording2 - Similar to TestPlaybackExpiredRecording1
    except retention priority is set to 1. Upon expiration, the recording
    is not deleted but is retained.

    Visual of playback and pass/fail messaging must be checked.

  Test 84: TestScheduleRealLongRecording - Test that creates a very long
    recording intended to cause purging of other recordings.
    State checks are for long recording
 
    Check for pass/fail.

  Test 85: TestPurgeDuringPlayback - Schedules 2 recordings and
    tries to purge one recording in playback with the activation
    of the second. First recording is 90 seconds long and will be
    played back after recording is finished. Second recording will 
    bacome active during playback of first. This should not cause the 
    playback recording to delete.

    Check for pass/fail.

  Test 86: TestPurgeExpiration - Schedules 4 recordings with retention
    priority of 1. One recording causes purging of the other three recordings

    Check for pass/fail. 

  Test 87: TestPurgeOnExpired - Simlar to above test except the expiration
    of the three recordings occur during the active fourth recording

    Check for pass/fail.

  Test 88: Test reschedule past expiration - Scheduels a recording with a
    retention priority of 1 and changes it.

    Check for pass/fail.

  Test 89: TestRecordingID:Basic - Tests the OcapRecordingRequestgetID() method

    Check for pass/fail.
 
  Test 90: TestRecordingID:IllegalArgumentException - Tests if an Illega eception
    is thrown by the OcapRecordingManager when an invalid ID is passed

    Check for pass/fail.


  Please refer to  Visio-ScheduledRecordingTestScenarios.pdf 
  Test 91: Series: TestSeriesContention1 Check for pass/fail.
  Test 92: Series: TestSeriesContention2 Check for pass/fail.
  Test 93: TestSeriesContention_3_delete_request Check for pass/fail.
  Test 94: TestSeriesContention_3_delete_service Check for pass/fail.

  Test 95 / 96: Test Failed Delayed Start Recording
	1.Boot the STB
	2.Run Test Failed Delayed Start Recording - Pre boot run Check for pass/fail.
	3.Set the following vaiable in the mpeenv.ini to the following
		OCAP.dvr.recording.delayScheduletimeout=300000
	4.When notified, reboot the box
	5.Run Test Failed Delayed Start Recording  Check for pass/fail.
	6.Set the following vaiable in the mpeenv.ini to the following
		OCAP.dvr.recording.delayScheduletimeout=0 after test run

  Test 97: TestStartRecordingManager Utility for starting the recording manage if 
		OCAP.dvr.recording.delayScheduletimeout=300000

  
  Test schedule 3 recordings simultaneously with RECORD_IF_NO_CONFICTS with resouce contention. Recording3 shall
  always fail in each case. THe variance is o
  Test 98: TestRetentionContention Check for pass/fail. - Retention policy set to 1. Check done after expiration period
  Test 99: TestRetentionContention2 Check for pass/fail. - Retention policy set to 1. Expiration period at start of recording
  Test 100: TestRetentionContention3 Check for pass/fail. - Retention policy set to DELETE_AT_EXPIRATION. Check done after expiration period

  The following two test, refer to SegmentedRecordings_STP_v01.doc in /docs/QA/TPS/WS22 - 
                    Completion of DVR IO2/SegmentedRecordings_STP_v01.doc
  Test 101: Tests in-progress recording losing resources Check for pass/fail.
  Test 102: Tests in-progress recording losing resources - Long rec Check for pass/fail.

  The following tests 97-100 are similar test cases 45-48. The difference is that cancel is called instead. 
   Please refer to  Visio-ScheduledRecordingTestScenarios.pdf  
  Test 103: TestCancelRecording1 Recordings do not execute and are cancelled. Check for pass/fail.
  Test 104: TestCancelRecording2 Recordings do not execute and are cancelled. Check for pass/fail.
  Test 105: TestCancelRecording3 Recordings do not execute and are cancelled. Check for pass/fail.
  Test 106: TestCancelRecording4 Recordings do not execute and are cancelled. Check for pass/fail.

  These tests test if a event is sent prior to recording start. Test 107 will check for a single notification.
  Test 108 will check for 2 seperate notifications at specified time intervals while Test 109 will check for
  3 separate instaces of events
  Test 107: TestAddBeforeStartListener1 Recordings do not execute and are cancelled. Check for pass/fail.
  Test 108: TestAddBeforeStartListener2 Recordings do not execute and are cancelled. Check for pass/fail.
  Test 109: TestAddBeforeStartListener3 Recordings do not execute and are cancelled. Check for pass/fail.

  Test 110 / 111: Test TestPropertiesChangesOnCompletedRecording
	1.Run Delete All Recordings
	2.Run TestPropertiesChangesOnCompletedRecording - create recording w/ AppData Check for pass/fail.
	3.When notified, reboot the box
	4.Run TestPropertiesChangesOnCompletedRecording - find recording and modify Check for pass/fail.

  Schedules 3 recordings with one set to RECORD_WITH_CONFLICT
    that starts first but has lowest priority to the other two recordings.
    Recording is checked if it will go to the IN_PROGRESS_ERROR statethen to INCOMPLETE
  Test 112: TestRecordwithConflicts Check for pass/fail.
    
  The following tests check the prioritzation methods found in OcapRecording Manager
    Senarios use 3 recording shedules all at the same time and prioritzed prior to start. 
    Recordings do not execute and their pending states only checked
    All recordings set to RECORD_IF_NO_CONFILCT
  Test 113: TestGetPrioritizationList Check for pass/fail.
  Test 114: TestSetPrioritizationList Check for pass/fail.

  Test 115: Fill the Disk but Stop early: 
	1. Run Delete All Recordings
	2. Run Fill the Disk but Stop Early
	3. Verify once the STB is booted up and the app with AUTOSTART setting is running 
		that the recrding light is not lit
	4. Delete recordings

  The following test will check if prioritzation will function after recording start with expiration  
  Test 116: TestSimpleScheduledRecordingContention Check for pass/fail.
 
  Tests a rescheduled recoring by changingthe service it was to record. Recoring is allowed to take place.
  Test 117: TestRescheduling - locators (source IDs) Check for pass/fail.

  The following test test for resource contetion waring firing. Please refer to Jeff Spruiel or WS40.doc in
  C:\perforce_dir\SA\main\docs\QA\TPS\WS40 - Resource Contention Warning
  Test 118: Test_RCW_0 (basic 2 pending; pri) Check for pass/fail.
  Test 119: Test_RCW_3 (2 inProg; no pri)  Check for pass/fail.
  Test 120: Test_RCW_5  Check for pass/fail.
  Test 121: Test_RCW_6  Check for pass/fail.
  Test 122: Test_RCW_7  Check for pass/fail.
  Test 123: Test_RCW_8  Check for pass/fail.
  Test 124: TesRCW - reset warning period to be in the future  Check for pass/fail.
  Test 125: TesRCW - reset warning period to be in the past  Check for pass/fail.
  Test 126: TesRCW - delete recording before waring is fired  Check for pass/fail.


  Group 2 Tests
 
  Tests 1 -7 same as above

  New ITSB reports
  Test 8: TestRecByLoc_wUnBufferedSC 
	    Test will start up an unbuffered Service Context then start a scheduled recording by locator.
            Verification of the recording state (COMPLETED_STATE) made near the end of the test run. Check for pass/fail. 

  Test 9: TestRecByLoc_w_UnBufferedSC_FailedRec
	    SAME AS ABOVE except recording is in the past. Recording will go to the FAILED_STATE. Check for pass/fail. 
	   
  Test 10: TestRecByLoc_wBufferedSC 
	    Test will start up a buffered Service Context then start a scheduled recording by locator. Partial content
	    will be in the past. Verification of the recording state (COMPLETED_STATE) made near the end of the test run. 
            Check for pass/fail. 
  
  Test 11: TestRecByServ_wBufferedSC 
	    Test will start up a buffered Service Context then start a scheduled recording by service. Partial content
	    will be in the past. Verification of the recording state (COMPLETED_STATE) made near the end of the test run. 
            Check for pass/fail.

  Test 12: TestRecByLoc_wBufferedSC 
	    Test will start up a buffered Service Context then start a scheduled recording by locator. All content
	    will be in the past. Verification of the recording state (COMPLETED_STATE) made near the end of the test run. 
            Check for pass/fail. 

  Test 13: TestRecByServ_wBufferedSC 
	    Test will start up a buffered Service Context then start a scheduled recording by service. All content
	    will be in the past. Verification of the recording state (COMPLETED_STATE) made near the end of the test run. 
            Check for pass/fail.

  Test 14: TestUnBufferedSC_w_RecByLoc
            Test will start up a scheduled recording by locator and then an unbuffered ServiceContext without buffering
	    Check for pass/fail

  Test 15: TestBufferedSC_w_RecByLoc_A
	    Test will start up a scheduled recording by locator and then a buffered Service Context with buffering. A 
	    call is made to jump beyond the start of the Service Context and verification that a BeginingofContentEvent
	    is not produced. Check for pass/fail  

  Test 16: TestBufferedSC_w_RecByServ_A
	    Test will start up a scheduled recording by service and then a buffered Service Context with buffering. A 
	    call is made to jump beyond the start of the Service Context and verification that a BeginingofContentEvent
	    is not produced.  Check for pass/fail 

  Test 17: LastChannelBuffering_A 
            This test test the last channel buffering feature by selecting a service, hopping to another service and back
	    and calling content in the past. If a begining of content message is not received, then the test passes. 
            Check for pass/fail

  Test 18: RecByLoc_w_LastChBuffer_A
	    Test will tune to a service using a buffered Servvice Context, then tune to another service. The previous service should be'
            actively buffered. This is verified by scheduling a recording by locator in the past for the first service.
            Check for pass/fail

  Test 19: RecByServ_w_LastChBuffer_B 
            SAME AS ABOVE only recording by service

  Test 20: RecByLoc_w_LastChBuffer_C
            SAME AS RecByLoc_w_LastChBuffer_A but with content all in the past

  Test 21: RecByServ_w_LastChBuffer_D
            SAME AS RecByServ_w_LastChBuffer_B but with content all in the past

  Test 22: JumpInBufferedSC_w_LastChBuffer_E
            Test will tune to a service using a buffered Servvice Context, then tune to another service. The previous service should be'
            actively buffered. This is verified by starting up a buffered PIP window and time shift back with out getting a 
            Begining of Content Event

  Test 23: RecByUnbufferedSC_w_LastChBuffer_F
            Test will tune to a service using a buffered Service Context, then tune to another service. The previous service should be'
            actively buffered. This is verified by scheduling a recording assocaited with a non-buffered PIP window partially
            in the past for the first service.
            Check for pass/fail

  Test 24: RecByBufferedSC_w_LastChBuffer_G
	    Test will tune to a service using a buffered Service Context, then tune to another service. The previous service should be'
            actively buffered. This is verified by scheduling a recording assocaited with a buffered PIP window partially
            in the past for the first service.
            Check for pass/fail

  Test 25: RecByUnbufferedSC_w_LastChBuffer_H 
            Test will tune to a service using a buffered Service Context, then tune to another service. The previous service should be'
            actively buffered. This is verified by scheduling a recording assocaited with a non-buffered PIP window in the past for the
            first service.
            Check for pass/fail

  Test 26: RecByBufferedSC_w_LastChBuffer_I
            Test will tune to a service using a buffered Service Context, then tune to another service. The previous service should be'
            actively buffered. This is verified by scheduling a recording assocaited with a buffered PIP window in the past for the
            first service.
            Check for pass/fail

  Tests 27 to 60 : Please refer to TestPlan WS13d in /docs/QA/TPS/WS13d - ImplicitTSB/WS13d.doc
  Tests 61 to 88 : Please refer to TestPlan SegmentedRecordings_STP_v01.doc in /docs/QA/TPS/WS22 - 
                    Completion of DVR IO2/SegmentedRecordings_STP_v01.doc
  
  Test 89 Check for pass/fail.
  Test 90 Check for pass/fail.
  Test 91 Check for pass/fail.
  Test 92 Check for pass/fail.
  Test 93 Check for pass/fail.
  Test 94 Check for pass/fail.
  
  Test InProgress Deletion
  	1. Run Delete All Recordings 
	2. Run TestDigitalRecording 
	3. Wait till RECORD LED lights 
	4. Run UtilDelRecording within 30 seconds 
	5. RECORD LED should extinguish        


The exception would be int TestPersistentRecordings the following steps should be taken:

1. Run utility UtilPrintRecordings: note the number of recordings 
2. Run Test Persistent Recordings and wait till message “Test Persistent Recordings - Create recordings finished.” 
3. Run utility UtilPrintRecordings: note the number of recordings had increased by 5 
4. Reboot box 
5. Run utility UtilPrintRecordings: note the number of recordings should be the same as before 

Certain tests that verify the storage of video files will return
an internal error message if the hard drive is low on space and the activity cannot be completed.
NOTE: not all tests will return an INTERNAL ERROR! 

reset()
findObject(String key)
findKey(Object object)
insertObject(Object item, String key)
removeObject(Object item)
dumpDvrRecordings()
deleteAllRecordings() 
printRecording(RecordingRequest rr)
diskFreeCheck()
initSC()
getNameForResourceUsage(final ResourceUsage ru)

Utility Description and Use:
----------------------------
TestFreeDiskSpace

This utility is checking for disk space available in the MediaStorageVolume objects.
Currently there is one large MediaStorageVolume object that absorbes the AVFS partition of 
the hard drive space. 

FilltheDisk

This utility is designed to fill the AVFS partion of the hard drive 
with 2 hour long recordings. At the end of the recording session,
the disk space left is displayed. If this test is setup in looping mode,
the space remaining will be printed out at the end of each recording iteration.

UtilPrintRecordings

This utility is designed to be run in DEBUG mode only. It is designed to print out the recording requests in
the Recording List object. 

UtilDelRecordings

This utility is designed to delete Recording Requests from the Recording List.


AutoXlet Information
-----------------------
There are 2 .xml files in the AutoXlet directory:

pdx_dvr_XletDriver.xml  -- this file is to be used for running half of the DVRTestRunner test cases w/ the
			AutoXlet automation mode
pdx_dvr_XletDriver2.xml -- This file covers most of the other half

These files should be placed in the snfs/qa/xlet dir. To use the files, remane them XletDriver.xml

Currently these files are being run under nightly drops on the following machines under an ant script

pdx_dvr_AutoXlet.xml  -- 10.0.1.108 (MachineName:GOZER)
pdx_dvr_AutoXlet2.xml -- 10.0.1.129

There is also a specific hostapp and config file that will need to be used.
These can also be found in the AutoXlet directory as the folloing

pdx_dvr_snfs_hostapp - rename to hsotapp and place in snfs/qa dir
pdx_dvr_snfs_config - rename to config and place in snfs/qa/xlet dir

Utilities:
----------
The following programs are utilities and are not to be run as test features:

FilltheDisk
UtilPrintRecordings
UtilDelRecordings

In viewing the file sytem on the SA 8300HD the following commands can be used in the Power TV prompt

dvr list - this will produce all the files listed in the AVFS partition

vendor,<enter>, a ,<enter>, 2,<enter>, a,<enter>, - will put you in the AVFS menu system.
Here, you can find out AVFS file sytem info and run utilities

	AVFS Driver Tests
	-----------------
0.  Start Recording Tuner 1
1.  Start Recording Tuner 2
2.  Start Playback 1  <  > 1/1 To MainTV
3.  Start Playback 2  <  > 1/1 To MainVCR
4.  Start Playback 3  <  > 1/1 To QAMMOD1
5.  Start Playback 4  <  > 1/1 To QAMMOD2
6.  Start Playback 5  <  > 1/1 To QAMMOD3
7.  Set Init scale for playback:
8.  Use Saved NPT on playback(On):
9.  Setup Tuners
a.  Select currentUnit for trick(2):
b.  Select destination for a unit
c.  Check Disk
d.  Disk info
e.  Erase File
g.  TrickGen
h.  Dump Remapped Pids for a unit
i.  Get Indices
j.  Jump
l.  List Files
m.  Start Decoding Audio
n.  Stop Decoding Audio
p.  AVFS Performance Tests
t.  Set TSB Size
v.  Stop Decoding Video
*.  Convert TSB to file
$.  Play to Lan IP Address(10.1.1.4):
+.  PVRDemo menu
@.  Drive Setup
!.  itfsCheckDisk
|.  play audio(Yes):
#.  avfs_SetSystem()
{.  Memory Test
/.  IdeLogTestMenu()
`.  set errNow
?.  Toggle AVFS log (0-off, 1-severe, 2-info, 3-noise, next value is 0)

Trick Modes:
 . Pause/resume
 ; Slow motion
 < Rewind
 > Fast forward
 - Resume normal play
 = Step forward
 , Step backward

q. Quit


Directions for Autoxlet use
---------------------------------

All automation xlet files are located in OCAPROOT/bin/OCAPTC/qa/xlet.

You will find these files:

pdx_dvr_hostapp
pdx_dvr_snfs_hostapp

This contatins info for the AutoXlet app and the DVRTestRunner. The pdx_dvr_hostapp
File is to be used in release while the pdx_dvr_snfs_hostapp
Is to be used in debug.  The other important file is in OCAPROOT/bin/OCAPTC/qa/xlet/org/cablelabs/xlet/DVRTestRunner. This file is

XletDriver.xml

This file contatins the keypresses that are to execute the test cases.

The pdx_dvr_hostapp has the following info:

app.0.application_identifier=0x000000017000
app.0.application_control_code=AUTOSTART
app.0.visibility=VISIBLE
app.0.priority=220
app.0.application_name=XletDriver
app.0.base_directory=/romfs/qa/xlet
app.0.initial_class_name=org.cablelabs.test.autoxlet.XletDriver
app.0.args.0=XletDriverScript=XletDriver.xml
#app.0.args.1=ResultsFile=Results.txt
app.0.args.1=ResultsServer=10.0.1.174
app.0.args.2=ResultsPort=8000
#app.0.args.4=DebugFile=Debug.txt
app.0.args.3=DebugServer=10.0.1.174
app.0.args.4=DebugPort=8010

app.1.application_identifier=0x000000017103
app.1.application_control_code=PRESENT
app.1.visibility=VISIBLE
app.1.priority=0xff
app.1.launchOrder=0x0
app.1.platform_version_major=0x1
app.1.platform_version_minor=0x0
app.1.platform_version_micro=0x0
app.1.application_version=0x0
app.1.application_name=DVRTestRunner
app.1.base_directory=/romfs/qa/xlet
app.1.classpath_extension=
app.1.initial_class_name=org.cablelabs.xlet.DvrTest.DVRTestRunnerXlet
app.1.args.0=config_file=/romfs/qa/xlet/config.properties

The last 6 entries in the AutoXlet app are UDP port information and will need to to be updated based on the configuration of the computer that is running the server. 

To setup automation in debug:

1.	Setup the mpeenv.ini file as stated in the readme.txt for DVRTestRunner Make sure the VMOPT.0 entry in front points to the hostapp file.
2.	Take  pdx_dvr_snfs_hostapp and place it into the proper directory and rename it hostapp.
3.	Copy the XletDriver.xml file and place it in OCAPROOT/bin/OCAPTC/qa/xlet.
4.	Change the Aconfig file to config
5.	Change autoboot-example to autoboot
6.	Start up STB
7.	Watch for launch of app

To setup automation in release:

1.	Setup the mpeenv.ini file as stated in the readme.txt for DVRTestRunner 
2.	Take  pdx_dvr_hostapp and rename it hostapp.
3.	Edit the hostapp to include the port # and IP address to the computer you will be logging to.
4.	Copy the XletDriver.xml file and place it in OCAPROOT/bin/OCAPTC/qa/xlet/AutoXlet.
5.	Go to OCAPROOT/target/OCAPTC and do a omake build.mpeenv build.romfs
6.	Do a omake build.autoxlet.image
7.	Burn image to STB
8.	Before reboot, go to OCAPROOT/bin/OCAPTC/qa/xlet and type java org.cablelabs.lib.utils.UDPPerfLogServer 8000 [or whatever port # you specified] Results.txt  
9.	Optional: Before reboot, go to OCAPROOT/bin/OCAPTC/qa/xlet and type java org.cablelabs.lib.utils.UDPPerfLogServer 8010 [or whatever port # you specified] Results.txt  
10.	Connect up the Ethernet cable to the PC and boot the STB.

Results of the test run will show up in OCAPROOT/bin/OCAPTC/qa/xlet




Xlet Control:
initXlet
	no parameters to configure
startXlet
	-
pauseXlet
	-
distroyXlet
	-

