Group 1
==========

Test 3-4 TestBasicRecordingAndPlayback.TestScheduledRecording(): 
    * Schedules a 30 seconds recording to start 30 seconds from now.  
    * At the end of recording (60secs), print out all recordings.  
    * test3 records a digital service; test 4 records an analog service
  Test verifications: 
    * Verifies recording is in the COMPLETED_STATE (70 seconds)
    * Verifies that the recording changed state 3 times - recordingChanged()
      was called 3 times:
          enter PENDING_NO_CONFLICT_STATE when the entry was first adde
          PENDING_NO_CONFLICT_STATE to IN_PROGRESS_STATE
          IN_PROGRESS_STATE to COMPLETE_STATE

Test 5 TestBasicRecordingAndPlayback.TestRecordingPlayback():
  * initialize ServiceContext
  * Schedule a 30 seconds recording to start in 2 seconds
  * after recording is done (41 seconds), select the recorded service and 
    wait 30 seconds before cleaning up the ServiceContext
    (tune to the RecordingRequest's service using ServiceContext.select()
  Test verification:
    * Verifies recording is in the COMPLETED_STATE (40 seconds)
    * Verifies no issues with presenting the recorded service 
      (NormalContentEvent was received).


Test 6 TestBasicRecordingAndPlayback.TestRecordingPlaybackDeleteInProgress():
  * initialize ServiceContext
  * Schedule a 40 seconds recording to start in 2 seconds
  * select the recorded service while recording is in progress (12 seconds)
    Instead of waiting blindly for 30 seconds, wait for the 
    PresentationTerminatedEvent which should be 
    (tune to the RecordingRequest's service using ServiceContext.select()
  * Delete the RecordingRequest while it is in progress (30 seconds)
  Test verification:
    * Verifies recording gets to in the IN_PROGRESS_STATE (19 seconds)
    * Verifies no issues with presenting the recorded service 
      (NormalContentEvent was received then the PresentationTerminatedEvent, 
      and ServiceContext is cleaned up successfully).


Test 7 TestBasicRecordingAndPlayback.TestRecordingPlaybackUsingJMF():


Test 8 TestBasicRecordingAndPlayback.TestRecordingPlaybackSetMediaTime():


Test 9 TestConsecutiveRecordings.TestBasicConsecutiveRecordings()
  * Schedule eight 1-minute recordings back to back.
      - "Recording1" to start 1min from now and ends 2min from now
      - "Recording2" to start 2min from now and ends 3min from now
      - "Recording3" to start 3min from now and ends 4min from now
      - "Recording4" to start 4min from now and ends 5min from now
      - "Recording5" to start 5min from now and ends 6min from now
      - "Recording6" to start 6min from now and ends 7min from now
      - "Recording7" to start 7min from now and ends 8min from now
      - "Recording8" to start 8min from now and ends 9min from now
  Test verification:
    * verify all eight recordings reached the COMPLETED_STATE 10mins from now


Test 10 TestConsecutiveRecordings.TestBasicConsecutiveRecordings2()
  * Schedule two 40 second recordings 10 seconds apart on different locators
      - "Recording1" to start 50 seconds from now and ends 1.5 minutes from now
      - "Recording2" to start 1min40seconds from now and ends 2min20sec from now
  Test verification:
    * verify both recordings reached the COMPLETED_STATE 2min30secs from now


Test 11 TestConsecutiveRecordings.TestSimultaneousRecordings()
  * Schedule four pairs of 45 secs recordings on the same locator 1 min apart
      - Recording1 & Recording2 starts 10secs from now and ends 55secs from now
      - Recording3 & Recording4 starts at 1min10secs and ends at 1min55secs 
      - Recording5 & Recording6 starts at 2min10secs and ends at 2min55secs 
      - Recording7 & Recording8 starts at 3min10secs and ends at 3min55secs
  Test verification:
    * verify all eight recordings reached the COMPLETED_STATE 4mins from now



Test 12, 13 TestImmediateRecording.TestBasicTSRecording()
  * Select a service via ServiceContext
  * at 1min10seconds from now, schedule a 30 secs long recording 
    (by ServiceContext), "Recording1", in the past (starts at 30secs and ends 
    at 1min).
  * at 1min23secs from now, stop the broadcast
  * at 1min32secs from now, select "Recording1".
  Test verification:
  * Verify recording reached the COMPLETED_STATE at 1min15secs (before
    broadcast was stopped). 
  * Verify "Recording1" is still in the COMPLETED_STATE at 2min10secs
    (after playback of "Recording1" is complete".


Test 14, 15 TestImmediateRecording.TestImmediateTSRecording()
  * Select a service via ServiceContext
  * at 50seconds from now, schedule a 30 secs long recording 
    (by ServiceContext), "Recording1", to start NOW which mean "Recording1" 
    will go from 50secs to 1min20secs.
  * at 1min23secs from now (after the recording is done), stop the broadcast
  * at 1min32secs from now, select "Recording1".
  Test verification:
  * Verify recording reached the COMPLETED_STATE at 1min22secs (before
    broadcast was stopped). 
  * Verify "Recording1" is still in the COMPLETED_STATE at 2min10secs
    (after playback of "Recording1" is complete".


Test 16 TestImmediateRecording.TestSCRecordingTuneAway()
  * Select a service (loc0) via ServiceContext
  * at 40seconds from now, schedule a 6min long recording (by ServiceContext), 
    "Recording1", of loc0 to start 30 seconds from now which means "Recording1"
    will complete at 6min30secs.
  * at 1min10secs from now (while "Recording1" is IN_PROGRESS), select a 
    different service (loc1) via ServiceContext.
  * at 2min5secs from now, stop the broadcast
  * at 2min20secs from now, select "Recording1"
  Test verification:
    * Verify "Recording1" is in the INCOMPLETE_STATE after the service
      has been tuned away (at 1min25secs and 2min30secs)

Test 17 TestImmediateRecording.TestTSRecordingSchedRecording()
  * Select a service via ServiceContext
  * at 30seconds from now, schedule a 2min20secs long recording (by
    ServiceContext), "SCRecording1", to start NOW which mean "SCRecording1" 
    will go from 30secs to 2min50secs.
  * at 1min from now, schedule a 30 secs long recording, "Recording1", to 
    start NOW which means "Recording1" will go from 1min to 1min30secs.
    "Recording1" overlaps the middle section of "SCRecording1"
  Test verification:
    * at 1min50secs, verify "Recording1" is in COMPLETE_STATE and 
      "SCRecording1" is in "IN_PROGRESS_STATE"
    * after 3mins (post end time of "SCRecording1"), verify both recordings
      are in COMPLETE_STATE and both recordings can be played back successfully
  Note: Requires 2 tuners.


Test 18 TestOverlappingEntries.TestOverlappingEntries()
  * Schedule 12 recordings to start the next day and verify 
    RecordingRequest.getOverLappingEntries() returns correctly.  
  * None of the recording actually execute, all recordings are in the PENDING 
    state
  Test verification
    * Verify getOverLappingEntries() for "Recording9" returned correctly: 
      there should be 8 overlapping recordings (0, 1, 2, 3, 4, 5, 7, 8)


Test 19, 20, 21 TestOverlappingRecordings.TestOverlappingSchedRecs()
  * Schedule two 90 seconds recordings to overlap by 40 seconds:
      - "Recording1" starts 10 seconds from now and ends 1min40secs from now
      - "Recording2" starts 1 minute from now and ends 2min30secs from now
  Test verification
    * Verify both recordings reached the COMPLETED_STATE 2 min40secs from now
  

Test 22, 23 TestOverlappingRecordings.TestOverlappingRecsWithSC()
  * Start up a buffering service context
  * Schedule two 60 seconds recordings to overlap by 30 seconds:
      - "Recording1" starts 1minute from now and ends 2minutes from now
      - "Recording2" starts 30 seconds from now and ends 1min30sec from now
  Test verification
    * Verify both recordings reached the COMPLETED_STATE 2 min20secs from now



Test 24 TestRecordAndCancel.TestBasicRecordAndCancel()
  * Schedule an one hour long recording "Recording1" to start in one hour
  * Cancel Recording1 before it starts.
  * Clean up the test by deleting Recording1 after it has been canceled
  Test verification
    * Verify "Recording1" reached the correct states: PENDING_NO_CONFLICT,
      then CANCELLED_STATE.

Test 25 TestRecordAndCancel.TestBasicSchedule()
  * Schedule five one hour long recordings to start in one day.
  Test verification
    * Verify the number of entries reported by RecordingManager increases
      by 5 the five recordings have been scheduled.
  

Test 26, 27 TestRecordingAlertLisener.TestBasicRecordingAlertListener()
  * Schedule 3 recordings 
  * Test 27 deletes one of the PENDING recordings seconds before it is 
    scheduled to start.
  Test verification
    * Verify the correct number of RecordingAlertEvent is received.


Test 28, 29 TestRecordingPlaybackListener.TestSingleListnerCallback()
  * Register a RecordingPlabyckListener
  * Create a recording "Recording1" and then playback once (test 31) or
    twice (Test 32).
  Test verification
    * Verify notifyRecordingPlayback(...) was called the expected number
      of times 
        - once for test 31; twice for test 32.


Test 30, 31 TestRecordingPlaybackListener.TestMultiListnerCallback()
  * Register 100 RecordingPlabyckListener to the OcapRecordingManager 
    (each listener is registered to ORM twice
  * Create a recording "Recording1" and then playback once (test 33) or
    twice (Test 34).
  Test verification
    * Verify notifyRecordingPlayback(...) was called the expected number
      of times for each of the 100 listeners.
        - once for test 33; twice for test 34.

Test 32 TestRecordingID_ECN829.TestRecordingIDBasic()
  * Verify that an IllegalArgumentException is thrown if 
    getRecordingRequest(...) is called with a non-existing id.
  * Verify all existing recordings ID (retrieve by calling getId() are unique
  * schdule 8 recordings
  Test verification
    * Verify RecordingRequest.getId() returns a unique ID for each of the 
      8 recordings 
    * Verify RecordingManager.getRecordingRequest(...) returns valid
      RecordingRequest for all ids (existing recoridng as well as the 8 
      newly added.

Test 33-38 TestRecMgrGetEntries.TestRecMgrGetEntriesFilter()
  * Schedules three 30 seconds recordings that overlap eachother: 
        - Recording1a, Recording1b, Recording1c starts 10 secs after eachother
  * Also schedules five 30 seconds back-to-back recordings (Recording2-6) with 
    the first of these recordings (Recording2) starting 10 seconds after 
    Recording1c's end time.  
  Test Verification
    * Verifies RecordingManager.getEntries(RecordingStateFilter) is correctly 
      implemented by confirming the expected RecordingList is returned for 
      various States:
        - Test 33: PENDING_NO_CONFLICT_STATE
        - Test 34: PENDING_WITH_CONFLICT_STATE
        - Test 35: IN_PROGRESS_STATE
        - Test 36: INCOMPLETE_STATE
        - Test 37: FAILED_STATE
        - Test 38: COMPLETED_STATE


Test 39 TestSortRecordinList.TestSortRecordingListTimeOrder()
  * Schedule eight 90 seconds long recording in the far future.  The start
    time of each recording are out of order.
  * Implement a RecordingListComparator that sorts on the recording's start 
    time
  Test Verification
    * Verify the RecordingList returned by the sortRecordingList()  call
      is in order based on the Recording's start time


Test 40, 41 TestInsufficientStateChange.TestRecordWithInsufficientSpace()
  * Schedule 2 large recordings:
      - duration of Rec1 takes up almost all the remaining disk space
      - duration of Rec2Helper can not fit on the remaining disk space 
  Test Verification
    * Verifies that Rec1 transitions from PENDING_NO_CONFLICT_STATE to 
      IN_PROGRESS_STATE to IN_PROGRESS_INSUFFICIENT_SPACE_STATE when 
      Rec2Helper starts.
        - test 40: Rec2Helper gets deleted which then causes Rec1 to transition
          ba to the IN_PROGRESS_STATE
    * Verifies that Rec2Helper transitions from PENDING_NO_CONFLICT_STATE to
      IN_PROGRESS_INSUFFICIENT_SPACE_STATE
        - test 41: Rec2Helper does not gets deleted and will then transition 
          to INCOMPLETE_STATE due to SPACE_FULL


Test 42, 43 TestScheduledRecordingContention.TestSimpleScheduledRecordingContention
    * Schedules three 3-minutes long parallel RecordingRequests: 
        rch3-recFirst: scheduled first
        rch2-recMiddle: scheduled second
        rch1-recLast: scheduledLast
      Test 42: does not have a ResourceContentionHandler registered
      Test 43: has a default ResourceContentionHandler that prioritizes  
                recordings by name lexigraphically registered
    TestVerification
    * Verify recordings are prioritized correctly:
        Test 42 (no RCH): rch3-recFirst and rch2_recMiddle gets the resources 
                          and transitions from PENDING_NO_CONFLICT to 
                          IN_PROGRESS while rch1-recLast transition from 
                          PENDING_WITH_CONFLICT to FAILED
        Test 43 (w/ RCH): rch1-recLast and rch2_recMiddle gets the resources 
                          and transitions from PENDING_NO_CONFLICT to 
                          IN_PROGRESS while rch3-recFirst transition from 
                          PENDING_WITH_CONFLICT to FAILED

Test 44-46 TestScheduledRecordingContention.TestReprioritizedContention*
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule a higher priority RecordingRequest (R1) after 4 recordings
      requests (R2-R5) have already been scheduled:
        ~ R4 and R5 are parallel;  
        ~ R2 end when R4/5 starts;  
        ~ R3 spans R2 and R4/5

        Test 44: --------------------------------
                             -R4-
                         -R2-
                             -R5-
                         ---R3---
                         -R1- scheduled while R2-R5 are in PENDING*State

        Test 45: --------------------------------
                             -R4-
                         -R2-
                             -R5-
                       ---R3---
                         -R1- scheduled while R2-R5 are in PENDING*State
        Test 46: same as 48 except R1 is scheduled after R3 has started
                 but R2 is still in the PENDING_NO_CONFLICT_STATE

  Test Verification
    * Verify resource contentions are resolved correctly where recordings 
      are given priority according to their name.
    * Verify each recording transitions through the correct recording states
      

Test 47-54 TestScheduledRecordingContention1.TestRRContention*
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Then schedule multiple overlapping recordings.  Then verify contention
      are resolved as expected by rescheduling/deleteing/cancelling one or
      more of the recordings

    Test Verifications:
      * Verify each recording transition to the correct recording stats


    Test 47, 48: four recordings with first three in parallel, and last one
                 starts immediately after
                 Time Line  -------------------
                              |-R1-|
                              |-R2-|
                              |-R3-|
                                   |-R4-|
        Test 47: reschedules/deletes/cancels R1 so it is parallel to R4
        Test 48: reschedules/deletes/cancels R3 so it is parallel to R4
               
    Test 49, 50: four recordings with first two in parallel (R1, R2), and
                 last two in series (R3, R4) and spans the first 2 recording
                 Time Line  -------------------
                              |----R1---|
                              |----R2---|
                              |-R3-|-R4-|
        Test 49: reschedules/deletes/cancels R1 so it starts after R4 ends
        Test 50: reschedules/deletes/cancels R3 so it starts after R4 ends

    Test 51: firve recordings where R1&R2 and R4&R5 in series while R3 spans
             in the middle:
                 Time Line  -------------------
                              |-R1-|
                                   |-R2-|
                              |-R4-|
                                   |-R5-|
                                 |-R3-|
              Reschedule/delete/cancel R3 to resolve contention

    Test 52: six recordings: 3 pairs of parallel recordings in series
                 Time Line  -------------------
                             |-R1-|
                                  |-R4-|
                                       |-R2-|
                             |-R5-|
                                       |-R6-|
                                  |-R3-|
              ~ Reschedule R3 to have it start earlier which causes contention
                with R5
              ~ Reschedule R3 back to resolve contention
              ~ Reschedule R3 again to have it start earlier and end later
                to cause contention with both R5 and R6
              ~ Reschedule R3 again so it has the original start time so 
                contention with R5 is resolved, still has contention with R6
              ~ Delete R3 to resolve contention
              ~ Create R3 again with the earlier start time so there is 
                contention with R5 again.
              ~ Cancel R3 to resolve contention
    Test 53, 54: Six recordings: R1&R2 in parallel, R3&R4 in parallel with 
                 each other and in series with R1&R2.  R5 starts first and
                 intersects with R1&R2.  R6 intersects R1&R2 and R3&R4:
                 Time Line  -------------------
                                |-R1-|
                                |-R2-|
                                     |-R3-|
                                     |-R4-|
                             |-R5-|
                                    |-R6-|
              Resolve contention by:
              ~ Reschedule R1 and R4 to start after R3 is done 
              ~ Delete R1 and R4 
              ~ Cancel R1 and R4 

          
   
Test 55-58  TestAppData:TestAddAppData
  * Schedule a RecordingRequest and then make various call rr.addAppData() 
      (test 55 and 57 schedules a LeafRecordingRequest)
      (test 56 and 58 schedules a ParentRecordingRequest)
      (test 57 & 58 creates the RecordingRequest with one AppData entry 
       associated.
  Test Verification
    * Verifies that IllegalArgumentException is correctly thrown if 
      AppData exceeds limit
    * Verifies that addAppData() can supports up to 64 keys
    * Verifies that NoMoreDataEntriesException is thrown by addAppDAta()
      if there are already 64 app data entries.
    * Verifies AppData (keys and AppData) are correct and can be removed 
      successfully.


Test 59  TestSetParent:TestSetParentRecording
  * Schedules 4 RecordingRequests
      - orr1 and orr2 are both long OcapRecordingRequests to start tomorrow
      - prr1 and prr2 are both ParentRecordingRequests that will be set as
        parent of orr1 and orr2
  Test Verification: OcapRecordingRequest.setParent(): 
      ~ Verify ocapRecordingRequest does not have a parent when it is first 
        created (getParent() should return null) 
      ~ Verify prr1 can be set as parent of orr1 correctly with prr1 
        transitioning to COMPLETED_RESOLVED_STATE
      ~ Verify orr1 can be reset with prr2 as its parent and prr2 is
        transition to PARTIALLY_RESOLVED_STATE; verify prr1's state is 
        transitioned (to PARTIALLY_RESOLVED_STATE) correctly as result
      ~ Verify ISE is thrown when setting ParentRecordingRequest(prr2) in 
        CANCELLED_STATE as the parent of an OcapRecordingRequets (orr2)
      ~ Verify orr1 can be reset with prr1 as its parent and prr2 is correctly
        deleted from the recording database as it is in the CANCELLED_STATE
        and contains no other RecordingRequests. 


Test 60-63  TestSeriesRecordingExpiration:TestSeriesRec_Season
    * Set up a series recording with a Root ParentRecordingRequest(Show) which
      has a child branch that is also a ParentRecordingRequest (Season) which 
      has two parallel children that are LeafRecordingRequests (Episodes)
    * Delete one of the LeafRecordingRequests (Episode2) while both leafs are 
      IN_PROGRESS
    * Wait till Recoridng expiration time has run out.
        Test 60: 24 hours expiration time - long expiration time
        Test 61: 90 sec expiration time - expires after recording's end time
        Test 62: 1 minute expiration time - expires at recording's end time
        Test 63: 30 sec expiration time - expired before recording's end time
  Test Verification: 
    ~ Verify the DeletaionDetails of Episode2 shows it was "USER_DELETED"
    ~ if expiration is longer than recording's end time, then verify Episode1
      is in COMPLETED_STATE after its end time.
    ~ Verify LeafRecordingRequets is in the DELETED_STATE with DeletionDetail
      "EXPIRED" after the expiration period is up.


Test 64-67  TestSeriesDeleteRecording:TestDeleteSeriesParent-*
    * Set up a series recording with a Root ParentRecordingRequest(Show) which
      has a child branch that is also a ParentRecordingRequest (Season) which 
      has a child that is a LeafRecordingRequest (Episode)
        Test 64: Delete Root (Show) before Leaf (Episode's) recording startTime
        Test 65: Delete Root (Show) after Leaf (Episode's) recording startTime
        Test 66: Delete Branch (Season) before Leaf (Episode's) startTime
        Test 67: Delete Branch(Season) after Leaf (Episode's) startTime
  Test Verification: 
    ~ Verify the deleted ParentRecordingRequest and it's children are no
      longer a member of in OcapRecordingManager's recordingList
    ~ Verify an IllegalStateException is thrown when calling getKnownChildren()
      on the deleted ParentRecordingRequest.
    ~ Verify the leaf of the deleted ParentRecordingRequest shows 
      "USER_DELETED" as its DeletionDetails reason.

Test 68-73  TestSeriesDeleteRecording:TestDeleteSeriesLeaf
    * Set up a series recording with a Root ParentRecordingRequest(Show) which
      has a child branch that is also a ParentRecordingRequest (Season) which 
      has a child that is a LeafRecordingRequest (Episode)
    * Delete the Episode (LeafRecordingRequest) either while it is 
      PENDING (71, 73, 75) or while it is IN_PROGRESS (72, 74, 76)
        Test 68, 69: Root - PARTIALLY_RESOLVED_STATE
                     Branch - COMPLETELY_RESOLVED_STATE
        Test 70, 71: Root - PARTIALLY_RESOLVED_STATE
                     Branch - PARTIALLY_RESOLVED_STATE
        Test 72, 73: Root - COMPLETELY_RESOLVED_STATE
                     Branch - COMPLETELY_RESOLVED_STATE
  Test Verification: 
    ~ Verify all ParentRecordingRequests (Show and Season) are still present
    ~ Verify getKnownChildren() size: Root (Show) is 1 and Branch (Season) is 0
    ~ Verify LeafRecordingRequest (Episode) is not part of RecordingList
      returned by the OcapRecordingManager
    ~ Verify the leaf of the deleted ParentRecordingRequest shows 
      "USER_DELETED" as its DeletionDetails reason.


Test 74  TestSeriesReschedule:TestParentReschedule
    * Set up a series recording with 2 leaf recordingRequests both are 
      children of the same ParentRecordingRequest(Branch1) which is a child 
      of the Root ParentRecordingRequest:
            Root 
            |
         Branch1
         /     \
      Leaf1   Leaf2

    * Reschedule so that another ParentRecordingRequest is added as a child
      of Root ParentRecordingRequest; then add a child LeafRecordingRequest
      to the new child ParentRecordingRequest:
            Root 
            |   \
        Branch1  Branch2
        /     \     |
     Leaf1   Leaf2  Leaf3
  Test Verification: 
    ~ Test validates the children cout of Root, Branch1, and Branch2



Test 75-76  TestSeriesContention:TestSeriesContention1
    * Schedules three sets of parallel series recordings 
        -Root1/Branch1/Leaf1 with Leaf1 to record from minute1 to minute2
        -Root2/Branch2/Leaf2 with Leaf2 to record from minute1 to minute2
        -Root3/Branch3/Leaf3 with Leaf3 to record from minute1 to minute2
  Test Verification: 
    ~ After recording's endtime, Leaf1 and Leaf2 should bein the COMPLETED_
      STATE while Leaf3 should be in the FAILED_STATE.

Test 77-78  TestSeriesContention:TestSeriesContention_delete*
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedules three sets of series recordings:
        -Root3/Branch3/Leaf3 with Leaf3 to record from minute2 to minute4
        -Root1/Branch1/Leaf1 with Leaf1 to record from minute1 to minute3
        -Root2/Branch2/Leaf2 with Leaf2 to record from minute1 to minute3
    * Test then deletes (Root1 by request for test 77; or Leaf1 by service
      for test 78).
  Test Verification: 
    ~ Leaf1 and Leaf2 goes from PENDING_NO_CONFLICT_STATE to IN_PROGRESS_STATE 
      while Leaf3 stays in PENDING_WITH_CONFLICT_STATE prior to deletion
    ~ After deleteion, test verified Leaf1 goes to DELETED_STATE while Leaf2
      stays in IN_PROGRESS_STATE then moves to COMPLETED_STATE; Leaf3 will
      also transition to PENDING_NO_CONFLICT_STATE followed by IN_PROGRESS
      and end in COMPLETED_STATE.

 

Test 79 TestTunerConflict_Gating:TestRecordingAndServiceConflict_ABC
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule 2 overlapping recordings ("#1-RecA" and "#2-RecB") to start
      while a serviceContext ("#3-SvcCtxtC") is presenting.
  Test Verification: 
    ~ Verify Resource Contention Handler prioritized the two recordings
      over the serviceContext and the two recordings transitions to 
      IN_PROGRESS_STATE successfully.
    ~ Verify resource contention warning was only envoked once.

Test 80 TestTunerConflict_Gating:TestRecordingAndServiceConflict_ACB
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule 2 overlapping recordings ("#1-RecA" and "#3-RecB") to start
      while a serviceContext ("#2-SvcCtxtC") is presenting.
  Test Verification: 
    ~ Verify Resource Contention Handler prioritized "#1-RecA" and 
      "#2-SvcCtxtC" over recording "#3-RecB" such that "#3-RecB" was never
      started and transitioned to FAILED_STATE"
      over the serviceContext and the two recordings transitions to 
    ~ Verify resource contention warning was only envoked once.

Test 81 TestTunerConflict_Gating:TestRecordingAndServiceConflict_CBA
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule 2 overlapping recordings ("#3-RecA" and "#2-RecB") to start
      while a serviceContext ("#1-SvcCtxtC") is presenting.
  Test Verification: 
    ~ Verify Resource Contention Handler prioritized "#2-RecB" and 
      "#1-SvcCtxtC" over recording "#3-RecA" such that "#3-RecA" transitions
      from IN_PROGRESS_STATE to IN_PROGRESS_WITH_ERROR state when "#2-RecB"
      starts
    ~ Verify resource contention warning was only envoked once.

Test 82 TestTunerConflict_Gating:TestRecordingNIAndSCConflict_ABC
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule a recordings ("#1-RecA") to start while both a
      ServiceContext ("#2-SvcCtxtB") and a NetworkInterface ("#3-niC") are
      presenting.
  Test Verification: 
    ~ Verify Resource Contention Handler prioritized "#1-RecA" and 
      "#2-SvcCtxtB" over NI presentation "#3-niC" such that "#1-RecA" 
      transitions to IN_PROGRESS_STATE successfully while the Network 
      Interface becomes unreserved.
    ~ Verify resource contention warning was not envoked. 

Test 83 TestTunerConflict_Gating:TestRecordingNIAndSCConflict_ABC
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule a recordings ("#1-RecA") to start while both a
      ServiceContext ("#3-SvcCtxtB") and a NetworkInterface ("#2-niC") are
      presenting.
  Test Verification: 
    ~ Verify Resource Contention Handler prioritized "#1-RecA" and 
      "#2-niC" over "#3-svcCtxtB" such that "#1-RecA" transitions to 
      IN_PROGRESS_STATE successfully while the Network Interface remains
      reserved.
    ~ Verify resource contention warning was not envoked.

Test 84 TestTunerConflict_Gating:TestRecordingNIAndSCConflict_CBA
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule a recordings ("#3-RecA") to start while both a
      ServiceContext ("#2-SvcCtxtB") and a NetworkInterface ("#1-niC") are
      presenting.
  Test Verification: 
    ~ Verify Resource Contention Handler prioritized "#1-niC" and 
      "#2-SvcCtxtB" over recording "#3-recA" such that "#1-RecA" transitions
      to FAILED_STATE from the PENDING_STATE while the Network Interface 
      remains reserved.
    ~ Verify resource contention warning was envoked once.

Test 85 TestTunerConflict_Gating:TestRecordingAndServiceConflict_ABCD
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule three overlapping recordings ("#1-RecA", "#3-RecC", "#2-RecB") 
      to start while a ServiceContext ("#4-SvcCtxtC") is presenting.
  Test Verification: 
    ~ Verify Resource Contention Handler prioritized "#1-recA" over "#2-RecB"
      over "#3-recC" over "#4-svcCtxtC" such that "#1-RecA" and "#2-RecB" 
      transitioned to IN_PROGRESS_STATE from the PENDING_NO_CONFLICT_STATE
      while "#3-recC" transitioned to FAILED_STATE from the 
      PENDING_WITH_CONFLICT_STATE
      remains reserved.
    ~ Verify resource contention warning was envoked once.

Test 86-89 TestRetentionContention:TestRetentionContention1
    * Registers a default ResourceContentionHandler that prioritizes  
      recordings by name lexigraphically
    * Schedule three minute-long parallel recordings ("R1", "R2", "R3").
  Test Verification: 
    ~ Verify each recording reaches the expected state at different times
    Test 86: delete at expiration and expiration is after recordings' end time
           : R1 and R2 should transition to IN_PROGRESS then COMPLETE then
             DELETED states
    Test 87: expiration is after recordings' end time and don't delete at 
             expiration
           : R1 and R2 should transition to IN_PROGRESS then COMPLETE
    Test 88: delete at expiration and expiration is before recordings' end time 
           : all three recordings should be in the deleted state before 
             recordings' end time
    Test 89: expiration is after recordings' end time and don't delete at 
             expiration
           : R1 and R2 should transition to IN_PROGRESS then COMPLETE
