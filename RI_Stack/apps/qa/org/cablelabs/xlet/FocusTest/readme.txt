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
This is a basic xlet to test Havi HScene and HComponent objects. The xlet manages 4 other xlets that have
coordinated sections of the screen. Each xlet has the ability to grab focus of the screen and controls, giving
the user the ablity to select components with the graphic, give focus to the scene or components, and set 
visibility or active status of the scene. Xlets can be start, paused, or destroyed as many times as needed.
 

To run FocusTestRunnerXlet:
---------------------------

Up and down arrows - select between each of the 4 displayed xlets

Left and Right arrows - select between selection of the scene or text box components

1 - requestFocus() - give the specified component or scene focus. Text box components will be hightlighted once 
	they are given focus

2 - setVisible(true) - Set the scene attribute such that the grphics for the xlet are shown
3 - setVisible(false) - Set the scene attribute such that the grphics for the xlet are hidden
4 - setActive(true) - Set whether the HScene is prepared to accept focus
5 - setActive(false) - Set to cause the scene to lose or not accept focus
 
Play - Starts the Xlet

Stop - Stops the Xlet

Pause - Pauses the Xlet


Test Procedure:
---------------

Start Focus 1 - Press Play

Select Focus1-Text2

Press 1 to request focus on the text

Select Focus1-Text1

Press 1 to request focus on the text


Select Focus 4

Start Focus 4 - Press Play

Press 1 to request focus on the text

Select Focus 4 by pressing left arrow

Press 3 to set the visibility of the scene to false


Select Focus 3

Start Focus 3 - Press Play

Press 1 to request focus on the scene


Select Focus 4

Press 2 to set the visibility of the scene to true

Press 1 to request focus on the scene


Select Focus 3

Press 2 to set the visibility of the scene to true

Press 1 to request focus on the text




