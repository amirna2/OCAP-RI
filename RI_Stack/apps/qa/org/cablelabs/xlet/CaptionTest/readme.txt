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

Description: 
The Caption application demonstrates the Closed Captioning (CC) control as part 
of the JMF player in the OCAP stack.

The user interface of the application allows for:
 - turning on/off closed captioning
 - setting CC background color
 - setting CC foreground color
 - setting CC font size
 - selecting an ANALOG (CC1..CC4), or a Digital (Digital1..Digital3) CC service.


How to run Caption Test
-----------------------

Caption is part of the CableLabs OCAP stack distribution. Application properties 
should be defined in the hostapp.properties file. 
For example:

###############################################
## Application 2 - Caption Test
###############################################
app.2.application_identifier=0x000000016201
app.2.application_control_code=PRESENT
app.2.visibility=VISIBLE
app.2.priority=220
app.2.application_name=Caption
app.2.base_directory=/syscwd/qa/xlet
app.2.initial_class_name=org.cablelabs.xlet.CaptionTest.CaptionTestXlet
app.2.args.0=config_file=/syscwd/qa/xlet/config.properties
###############################################

The application takes two arguments that specify sourceIDs of channels containing 
closed captioning and one argument to allow for logging of the supported services. 
Typically one channel will contain analog CC and the other digital CC.  
Configuration parameters should be defined in the config.properties file.

Configuration Parameters:

# Closed Caption
caption_source0=2151
caption_source1=2006
log_supported_svc=1
# 0=logging disabled, 1=logging enabled

NOTE: menu options "Set Background Color" and "Set Foreground Color" only work 
for channels that have Digital Closed Captioning.


User Interface
--------------

When the application is loaded and executed, full screen video will be presented 
on the screen. The video player will tune to the source ID defined by 
caption_source0. "No service" is shown in the bottom left corner of the screen, 
indicating there is currently no closed captioning service selected. Pressing the 
INFO button will display the CC window.


General User Interface Options
------------------------------

9	  Dismiss CC window.
ARROW UP  Go back to the previous window.
MUTE      mute/un-mute the AUDIO.


"CC Settings" Window
--------------------

Pressing the INFO button will bring up a "CC Settings" window with the following 
options:

A. TURN ON		(Turn on CC, if a CC service is selected)
B. TURN ON MUTE		(Turn CC on only when AUDIO is muted)
C. TURN OFF		(Turn off CC)
1. CC SERVICES          (Display "CC Services" window)
2. USER SETTINGS        (Display "User Settings" window)


"CC Services" Window
--------------------

This window allows the user to select a CC service. The choices are:

1. Analog CC1 - Digital 1
2. Analog CC1 - Digital 2
3. Analog CC1 - Digital 3
4. Analog CC2 - Digital 1
5. Analog CC3 - Digital 1
6. Analog CC4 - Digital 1

Note that only one CC service can be selected at a time.

When a service is selected, the name of the service is shown in the bottom left 
corner of the screen. For example "CC1 Digital 1".


"User Settings" Window
----------------------

0. RESET ALL		(Reset all attributes to embedded values)
1. BACKGROUND COLOR	(Set CC background color from the Color Map window)
2. FOREGROUND COLOR     (Set CC foreground color from the Color Map window)
3. FONT SIZE            (Set CC font size from the Font Size window)


"Color Map" Window
------------------
0. RESET COLOR
1. BLACK
2. WHITE
3. RED
4. GREEN
5. BLUE
6. CYAN
7. MAGENTA
8. YELLOW


"Font Size" Window
------------------
0. RESET FONT SIZE	(Reset font size attribute to embedded value)
1. SMALL		(Set font size to small)
2. STANDARD		(Set font size to standard)
3. LARGE		(Set font size to large)


Sample Test Case
----------------

1. Press "Info" to bring up the "CC Settings" window.

2. Press "A" to turn on closed captioning.

3. Press "1" to bring up the "CC Services" window.

4. Press "1" to select "Analog CC1 - Digital1" service.

5. Press "Arrow Up" to go back to the "CC Settings" window.

6. Press "2" to bring up the "User Settings" window.

7. Press "1" to set the CC background color.

8. Select a color.

9. Press "Arrow Up" to go back to the "User Settings" window.

10. Press "2" to set the CC foreground color.

11. Select a color.

12. Press "Channel Up" to tune to the channel that has digital CC enabled (Note 
    that "Set Background Color" and "Set foreground color" options only work on 
    channels that have digital closed captioning.)


Xlet Control:
initXlet
	Read configuration parameters to configuration file.
startXlet
	Manage user control and display image, closed captioning, and mute/volume 
	control.
pauseXlet
	Clear video.
distroyXlet
	Clear video.
