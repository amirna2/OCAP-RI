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

*******************************************************************************

DESCRIPTION:

*******************************************************************************

The HConfig Xlet allows a user to examine and configure the screen
devices of the system.  The Xlet presents a split screen with textual
descriptions of devices and configurations in the upper half and buttons
for selecting configurations (and other operations) in the bottom half.

Along the right side of each half-screen is a scroll bar.  To use the
scroll bar move the focus to the bar and then hit SELECT.  The scroll
position can be manipulated with the cursor keys as well as the page
up and down keys.  Use the EXIT key when finished manipulating the
scroll bar.

Their are 5 types of buttons displayed in the bottom half.  Most likely
not all buttons will be displayed.  The half screen will auto-scroll
as you move focus to buttons that are off-screen.  The types of buttons
are:

- Screen configuration        : BG-* and VID-* buttons select background
                                and video configurations.
- Background color            : Color names set the background color (e.g., 
                                the bars that are visible when 4:3 video is
                                presented on a 16:9 screen).
- Background stills           : test1.m2v and test2.m2v are sample background
                                stills.
- Video output port selection : Used to select composite, component, or DVI

The screen configuration names are in the following format:

   [BG|VID]-<resolution>[i|p][-STILL]-<par>-<loc>

Where

- BG           : Indicates background configuration, and VID is for video.

- <resolution> : Is the pixel resolution of the configuration.

- i            : Indicates interlaced; p indicates progressive.

- STILL        : On a BG config indicates that still image display is supported.
                 (Note that STILL display implies a non-contributing video config.)

- <par>        : Is the pixel aspect ratio.

- <loc>        : Is the location and size of the device relative to the entire
                 screen.

*******************************************************************************

TIPS:

*******************************************************************************

- Changing configuration of a device will change other devices, as
  necessary, as long as the other devices aren't reserved.  As such, if video
  is currently being presented, the background configuration cannot be changed.
  Change the video configuration first (which will reserve the video device
  explicitly), then you are free to change the background configuration.

- After setting the background still or color, use the applauncher to pause
  the HConfig xlet so that the background video can be visible.

- Changing the output port settings currently only affects the component video
  output: RF,composite, SVideo

- The Y-output   : Is used for composite, all other component outputs are non-
                   functional.

- Component      : Will use the component outputs in component fashion.

- DVI            : Turns off Component outputs.


*******************************************************************************

IMPORTANT NOTE:

*******************************************************************************

Use the old AppLauncher to run the test, because the current QA test launcher
covers up the test graphics.  Sample hostapp.properties file:

maxapps=99

app.0.application_identifier=0x000000014000
app.0.application_control_code=AUTOSTART
app.0.visibility=INVISIBLE
app.0.priority=220
app.0.application_name=AppLauncher
app.0.base_directory=/snfs/apps/launcher
app.0.initial_class_name=org.cablelabs.xlet.launcher.AppLauncher$Xlet
app.0.args.0=showOnStart

app.1.application_identifier=0x000000014225
app.1.application_control_code=PRESENT
app.1.visibility=VISIBLE
app.1.priority=220
app.1.application_name=HConfig
app.1.base_directory=/snfs/qa/xlet
app.1.initial_class_name=org.cablelabs.xlet.config.HConfig$Xlet


*******************************************************************************

HCONFIG TEST PROCEDURE:

*******************************************************************************

This test divides the screen into two parts; the upper half of the screen
displays information about the state of the display. It gives information
about the aspect ratio, resolution, and other such information. The bottom of
the screen has a list of buttons that do different things. Specifically there
are four different categories that the buttons fall into. These categories are: 

- Screen configuration : BG-* and VID-* buttons select background and video
                         configurations.

- Background color     : Black, gray, blue, green, red can be selected to set the
                         background color (e.g., the bars that are visible when
                         4:3 video is presented on a 16:9 screen).

- Background stills    : test1.m2v and test2.m2v are sample background stills.

- Video output port selection : Component, DVI, composite, RF, or S-Video.


There are several things that need to be tested for when running HConfig. The first
part of the test is checking for the background color selection.

*******************************************************************************

HOW TO TEST BACKGROUND COLOR SELECTION:

*******************************************************************************

1. Launch HConfig Test.

2. Press the LEFT arrow button on the remote to highlight the buttons at the bottom
   of the screen.

3. Use the DOWN arrow button to scroll through the buttons until one of the color
   buttons is highlighted.

4. Press the SELECT key to change the background color.

5. Press the MENU button to return to the Test Launcher.

6. Pause HConfig in the Test Launcher: the new background color should appear.

7. Repeat the above steps for all of the different background color selections.


The next thing that can be tested for is the background stills selection. This is
performed in much the same way as  the background color selection.

*******************************************************************************

HOW TO TEST STILL IMAGE DISPLAY:

*******************************************************************************

1. Launch HConfig Test.

2. Press the LEFT arrow button on the remote to highlight the buttons at the
   bottom of the screen.

3. Use the DOWN arrow button to scroll through the buttons, and select a screen
   configuration that has STILL image display supported.  For example:
   
   BG – 640x480p – STILL – 1:1 – 0.0, 0.0, 1.0, 1.0

4. Use the DOWN arrow button to scroll through the buttons until either test1.m2v
   or test2.m2v is highlighted.

5. Press the SELECT key to enable the background still image.

6. Press the MENU button to return to the Test Launcher.

7. Pause HConfig in the Test Launcher: the image should get displayed on the screen.

8. Repeat the above steps for the other background still image.


There are 42 multiple screen configurations that can be selected. Not all of these
screen configurations may be supported by the TV screen that is connected to the
set-top box. In order to perform this section of the test properly, the test 
needs to be performed with a TV that is connected to all of the outputs on the
set-top box.

*******************************************************************************

HOW TO TEST SCREEN CONFIGURATIONS:

*******************************************************************************

1. Launch HConfig Test.

2. Press the LEFT arrow button on the remote to highlight the buttons at the
   bottom of the screen.

3. Use the DOWN arrow button to scroll through the buttons until one of the screen
   configuration buttons is highlighted.

4. Press the SELECT key to enable the screen configuration.

5. Once the selection has been made the output to the TV screen may be presented
   in a different aspect ratio or background size depending on the configuration
   chosen.

6. The above steps can be repeated for the different screen configurations. 

Here is a list of all the screen configurations:

VID – 1920x1080i – 1:1 – 0.0, 0.0, 1.0, 1.0
VID – 1920x1080i – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
VID – 1920x1080i – 4:3 – -0.16666667, 0.0, 1.3333334, 1.0
VID – 1280x720p – 1:1 – 0.0, 0.0, 1.0, 1.0
VID – 1280x720p – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
VID – 1280x720p – 4:3 – -0.16666667, 0.0, 1.3333334, 1.0
VID – 640x480p – 1:1 – 0.0, 0.0, 1.0, 1.0
VID – 640x480p – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
VID – 640x480p – 3:4 – 0.0, -0.16666667, 1.0, 1.3333334
VID – 640x480i – 1:1 – 0.0, 0.0, 1.0, 1.0
VID – 640x480i – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
VID – 640x480i – 3:4 – 0.0, -0.16666667, 1.0, 1.3333334
VID – 853x480p – 1:1 – 0.0, 0.0, 1.0, 1.0
VID – 853x480i – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 1920x1080i – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 1920x1080i – STILL – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 1920x1080i – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
BG – 1920x1080i – STILL – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
BG – 1920x1080i – 4:3 – -0.16666667, 0.0, 1.3333334, 1.0
BG – 1920x1080i – STILL – 4:3 – -0.16666667, 0.0, 1.3333334, 1.0
BG – 1280x720p – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 1280x720p – STILL – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 1280x720p – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
BG – 1280x720p – STILL – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
BG – 1280x720p – 4:3 – -0.16666667, 0.0, 1.3333334, 1.0
BG – 1280x720p – STILL – 4:3 – -0.16666667, 0.0, 1.3333334, 1.0
BG – 640x480p – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 640x480p – STILL – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 640x480p – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
BG – 640x480p – STILL – 1:1 – -0.16666667, -0.16666667, 1.3333334, 1.3333334
BG – 640x480p – 3:4 – 0.0, -0.16666667, 1.0, 1.3333334
BG – 640x480p – STILL – 3:4 – 0.0, -0.16666667, 1.0, 1.3333334
BG – 640x480i – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 640x480i – STILL – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 640x480i – 1:1 – -0.16666667, -0.16666667, 1.33333334, 1.3333334
BG – 640x480i – STILL – 1:1 – -0.16666667, -0.16666667, 1.33333334, 1.3333334
BG – 640x480i – 3:4 – 0.0, -0.16666667, 1.0, 1.3333334
BG – 640x480i – STILL – 3:4 – 0.0, -0.16666667, 1.0, 1.3333334
BG – 853x480p – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 853x480p – STILL – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 853x480i – 1:1 – 0.0, 0.0, 1.0, 1.0
BG – 853x480i – STILL – 1:1 – 0.0, 0.0, 1.0, 1.0
