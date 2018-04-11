readme.txt - Sound Manager Resource Contention (RC)
2007/02/09 tk

==== SoundContention test xlet:
There are two players (two applications) in this test xlets.  Both players have 
derived from IXCSample sample application in VWB and SoundTest, a QA test 
application. The players could be toggled between the HSound and JMF Player 
implementations interactively by the user / tester.

AutoXlet featur is included in the test xlet but due to the limitation in AutoXlet, 
this option is not implemented.  See more info at the bottom of this readme.txt file.

==== Required resources to run this xlet:
A - Normal execution
.\cablelabs\lib\utils\*
.\cablelabs\test\autoxlet\* 	

B - Additional sound files required in running this test are located in 
\\fender\ServerProxy\VWB\Files\SoundFiles.


==== Limitations:
- PowerTV only supports AIFF sound files and it does not support MP1, MP2, MP3 and 
AC3 sound files.
- SA Stack complains and not able to build the qa disk with the "omake build.qadisk" 
because the sound files are just too large for the buffer.  You must remove some of 
the sound files.


==== Operating the Test Xlet
1. Use the Left Arrow and Right Arrow keys to focus on the respective window.  
Initially you must first focus on one of the two windows to start.

2. Use the "MENU" key to toggle between the HSound and the JMF player 
implementations. You need to focus on the desired screen/window before pressing 
"MENU" to do this.

3. Use the "0" key to toggle between On and Off mode of the resource contention 
(RC).  If you turn off RC, the Player interrupts and stops the player stream before 
starting a new player.  If the RC feature in the stack works correctly, the player 
should not start another player before finishing the previously running stream.

4. If you start the JMF player and change the player mode to HSound Player mode 
while in the same application / player, both players will continue to play until the 
players reach to the end of the respective sound file. 

5. Selecting/Entering "9" (not shown on the screen) causes to start the automated 
test.  However, this option still requires more work to provide robust automated 
tests.

6. The "0", "9" and "Menu" keys are used as special option keys.

7. Player options on the remote
	1 - "08_11025_Mono.AIF"								
	2 - "08_22050_Mono.AIF"
	3 - "08_22050_Stereo.AIF"					
	4 - "16_22050_Mono.AIF"
	5 - "16_22050_Stereo.AIF"
	6 - "16_44100_Mono.AIF"
	7 - "16_44100_Stereo.AIF"					
	8 - "Frieds.aiff"

==== With RC Condition (No forceful stop of player before starting a new player)
Note: This option eventually ends in the resource depletion currently.

1. If you press the left arrow key while focusing on Player 1 and playing a sound 
file, the new player should interrupt the currently played sound file.  

2. If you press the right arrow key while focusing on Player 1 and playing a sound 
file, the new player should interrupt the player and start playing the new sound 
file.  

3. Keys 1 thru 8 do not stop the currently played sound file and start the new sound 
file according to the key pressed.


==== Without RC Condition (Forcefully stopping the player before starting a new 
player)
1. If you press the left arrow key while focusing on Player 1 and playing a sound 
file, the player interrupts the sound file.

2. If you press the right arrow key while focusing on Player 1 and playing a sound 
file, the player stops playing the sound file.

3. If you press the left arrow key while focused on Player 2 and playing a sound 
file, the player stops playing the sound file.

4. Keys 1 thru 8 stop the currently played sound file and start the new sound file 
according to the key pressed.


==== AutoXlet Implementation (Not implemented)
