################################################################################
				Testing ECN 1057
################################################################################

This ECN does some clarification of the usage of the organization ID in 
OcapRecordingProperties object and in setting organization access on 
MediaStorageVolume. Previously no tests were deveped to execise the
functional propr to this ECN. The procedue section outlines the senarios
tested and the assocaed action that accompanies them.

#################################################################################
				Requirements
#################################################################################

Leaf certificates musht be generated for each of the recording applications
and for each of the playback applications. Leaf certificates shall contain
shall caontain the respective org ID of the application being used.

#################################################################################
                           xlet_desription_file
#################################################################################



#################################################################################
                       config.properties parameters
#################################################################################

The RecordingXlet xlets require a config.properties file to exist with the
following information: 

#$Locator parameter for Recording Xlet
RecFPQ=63000000,1,255

This defines the service that the RecordingXlet will record in
frequency, program number, and QAM respectively. 

This file should be placed at the base directory of the apps.

#################################################################################
                          manual control keys
#################################################################################

The numeric keys can be used for executing actions in the RecordingXlet and 
PlaybackXlet apps. The following is the keys used in the RecordingXlet app and 
the associated action:

        1: Create a recording granting access from all organizations
        2: Create a recording granting access to only org 1
        3: Remove recording access from orgs 1 and 2
        4: Grant access to organizations 1 and 2
        5: Delete recordings created from this app
        6: Attempt recording but fail with IllegalArgumentException

For PlaybackXlet the following numeric keys are used:

        1: Attempt Playback, expect playback success
        2: Attempt Playback, expect exception to be thrown

#################################################################################
				PROCEDURE
#################################################################################

Validation of creator's org present in the organization ID 
OcapRecordingProperties results in successful playback after 
the authentication of the leaf certificate

	* Start RecordingXlet1
	* Create a recording with org 1 specified in OcapRecordingProperties under 
		organization_id
	* Destroy RecordingXlet1
	* Start PlaybackXlet1
	* Attempt playback of the recording and verify success
	* Destroy PlaybackXlet1
	* Start PlaybackXlet2
	* Attempt playback of the recording and verify failure 
	* Destroy PlaybackXlet2
	* Start PlaybackXlet3
	* Attempt playback of the recording and verify failure 
	* Destroy PlaybackXlet3

--------------------------------------------------------------------------------

Validation of null being used in the organization ID to disable 
autentication, allowing all apps to successfully playback

	* Start RecordingXlet1
	* Create a recording with null specified in OcapRecordingProperties under 
		organization_id
	* Destroy RecordingXlet1
	* Start PlaybackXlet1
	* Attempt playback of the recording and verify success
	* Destroy PlaybackXlet1
	* Start PlaybackXlet2
	* Attempt playback of the recording and verify success
	* Destroy PlaybackXlet2
	* Start PlaybackXlet3
	* Attempt playback of the recording and verify success
	* Destroy PlaybackXlet3
	
--------------------------------------------------------------------------------

Validation of IllegalArgumentException is thrown when the organization_id defined
in OcapRecordingProperties does not match with the Org ID of the creator

	* Start RecordingXlet1
	* Attempt recording creation and failure with IllegalArgumentException thrown.
	* Destroy RecordingXlet1

--------------------------------------------------------------------------------

Validation of the removal of organizations from a MediaStorageVolume results
in a SecurityException thrown from apps existing in those organizations and 
successful playback elsewhere

NOTE: This does not require the use of certificates directly for granting of 
	app permissions
	
	* Start RecordingXlet1
	* Create a recording with null specified in OcapRecordingProperties under 
		organization_id
	* Remove access from organization 1 and 2 to the default MediaStorageVolume
	* Destroy RecordingXlet1
	* Start PlaybackXlet1
	* Attempt playback of the recording and verify failure and that SecurityException
		 is thrown
	* Destroy PlaybackXlet1
	* Start PlaybackXlet2
	* Attempt playback of the recording and verify failure and that SecurityException
		 is thrown
	* Destroy PlaybackXlet2
	* Start PlaybackXlet3
	* Attempt playback of the recording and verify success
	
Validation of the addition of organizations to a MediaStorageVolume results in
successful playback from apps existing 

NOTE: This does not require the use of certificates directly for granting of 
	app permissions

	* Give access to organizations 1 and 2 on the default MediaStorageVolume
	* Destroy PlaybackXlet3
	* Start PlaybackXlet1
	* Attempt playback of the recording and verify success
	* Destroy PlaybackXlet1
	* Start PlaybackXlet2
	* Attempt playback of the recording and verify success
	* Destroy PlaybackXlet2
	* Start PlaybackXlet3
	* Attempt playback of the recording and verify success
	* Destroy PlaybackXlet3
	
--------------------------------------------------------------------------------

###################################################################################
				AUTOMATION
###################################################################################

Included is the automation file AutoTest.xml with the source code. This can be used 
to drvie the applications automatedly such that the all the procedures above are done.

Plese refer to AutoXlet documentation prior to using the automation.

For running in automation, the file should be placed in the base directory and 
renamed to XletDriver.xml


