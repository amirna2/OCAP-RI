DvrTestMonAppXlet - Tests the setRecordingDelay() method and the
monitorConfiguredSignal() method of the stack. This xlet contains the test
cases for WS22b ECR856. The test cases are as follows:
 		- Set Recording Delay Within Range.
 		- Set Recording Delay Within Range, Recording End After Delay Start.
		- Set Recording Delay Within Range, Recording End Before Delay Start.
 		- Set Recording Delay Within Range, Recording Begin And End Before Delay Start.
 		- Set Recording Delay Within Range, Recording Begin Before Delay Start.
 		- Set Recording Delay Within Range, Delay Recording Start Called After Monitor Configured Signal Call.
 		- Set Recording Delay Within Range, Delay Recording Start Called After Monitor Configured Signal TimeOut.
 
In order to run these test cases the mpeenv.ini file must contain the 
following parameter:
		OCAP.monapp.resident=true
This parameter allows the first application that is launched when the stack 
starts to be the resident mon app, in this case it will be DvrTestMonAppXlet.
 
This xlet also assumes that the DvrTestRunnerXlet has been launched and a 
recording has been scheduled from the ECR856 Tests group. If a recording has
not been scheduled then this xlet will return with a failure stating that no 
recording was found. 

Once a recording has been scheduled from the ECR856 Tests group within the 
DVRTestRunner, the hostapp.properties file must be changed before restarting
the set-top box and launching DvrTestMonAppXlet. Rename the file 
ecr856_hostapp.properties to hostapp.properties. 
 
The config.properties file must also be modified prior to running this xlet.
The file looks as follows:
 		#Used by ECR856 testing to choose which test to perform
		#
 		# 1.1 = SetRecDelayWithinRange
		# 1.2 = SetRecDelayWithinRangeStopAfterDelayStart
		# 1.3 = SetRecDelayWithinRangeStopBeforeDelayStart
		# 1.4 = SetRecDelayWithinRangeStartStopBeforeDelayStart
		# 1.5 = SetRecDelayWithinRangeStartBeforeDelayStart
		# 1.6 = SetRecDelayWithinRangeAfterMonConfSignal
		# 1.7 = SetRecDelayWithinRangeAfterMonConfSignalTimeOut
 		#
		DVR_ecr856_test=1.1
 
The parameter, DVR_ecr856_test= determines which test case will be run when
DvrTestMonAppXlet is launched. The other caveat for successfully running 
the test is that the previously scheduled recording must correspond to the 
test case being launched by DvrTestMonAppXlet. For example, in order to 
launch test case 1.1 - Set Recording Delay Within Range, TestECR856 - 
ScheduleLongRecording - 1.1, 1.2, 1.6, 1.7 must first be selected from the
ECR856 Tests group within DvrTestRunnerXlet.