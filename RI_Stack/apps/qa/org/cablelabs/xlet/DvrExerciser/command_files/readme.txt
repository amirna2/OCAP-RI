Description:
The files in this directory are 'command files' that are used to drive the 
CommandProcessor that is built into DvrExerciser.  That is, these command 
files are intended to automate the operation of DvrExerciser.  
Each file is an entire test comprised of a number of atomic test commands.

Specifying a command file:
To specify a specific command file for DvrExerciser to automatically, add a 
line similar to the following to the hostapp.properties file:

	app.0.args.1=command_file=random_tune.cmd
	
On startup DvrExerciser will look for the 'command_file' parameter and if 
found, attempt to process the associated file of commands.  If the 
'command_file' parameter is not found, DvrExerciser will run in its normal 
mode.  

Note that any command file must be in the search path for DvrExerciser.  
One convenient location is to copy the command file to the syscwd/qa/xlet 
directory which is already used to locate the config.properties file.  

Logging output:
If a command file is specified at run time, the name of the command file is 
emitted to the log.  Additionally, as each command is read from the command 
file, it is also emitted to the log.

As each command is executed in turn, its name is output to the log and the 
result is evaluated.  If any command fails, the command processor stops and 
outputs a 'FAILED' message to the log.  If all commands succeed, the command 
processor outputs a 'PASSED' message to the log.

Command file format:
The command file is expected to be a regular text file, where each line of 
text represents a command.  
 - Some commands will have mandatory arguments, while others will none.  
 - Commands and arguments are separated by white space.
 - Comment lines (those beginning with '#') and blank lines are ignored.
 - Lines with unknown commands will result in 'unknown' commands that are 
 	essentially no-ops
 
The following are the commands currently known to the command processor.  
The first group provide a way to simulate key presses from the user, 
while the second group are operational commands.  
The items in brackets identify required arguments for the command:

testName         [name of test being executed - string]
vkChannelUp
vkChannelDown
vkStop
vkLive
vkPlay
vkFastFwd
vkRewind
vkPause
vkNumber         [numeric key - integer]

deleteAllRecordings
createRecording
cdsPublishRecording
makeFirstRecordingCurrent
wait             [number of seconds to wait - integer]
channelUp
channelDown
recordAndWait    [number of seconds to record - integer]
setPlayRate      [play rate - float]
testPlayRate     [returns TRUE if current play rate equals this number - float]
randomTune       [dwell time - integer]    [run time - integer]
enableTsb        [enable state - string = "true" or "false"]
label            [name - string]
loop             [label name - string]     [loop count - integer]