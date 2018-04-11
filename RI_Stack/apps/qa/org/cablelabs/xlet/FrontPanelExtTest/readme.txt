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
 
The FrontPanelExtTest xlet tests the following settop box front panel
indicators: POWER, RECORD, MESSAGE, RFBYPASS, TEXT DISPLAY, and TEXT DISPLAY
CLOCK.

For the TEXT DISPLAY tests, "text" should be included in the space-separated
list of values for the OCAP.fp.indicators property in the mpeenv.ini file (for
example: OCAP.fp.indicators=message power text).

SA8300HD Note: Although the PowerTV API supports a range of brightness levels
from 0x0001 through 0xFFFF, the SA8300HD appears to support only ON (0xFFFF)
and OFF (any value < 0xFFFF).

*******************************************************************************
                                   CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.57.application_identifier=0x000000016619
app.57.application_control_code=PRESENT
app.57.visibility=VISIBLE
app.57.priority=220
app.57.application_name=FrontPanelExt
app.57.base_directory=/snfs/qa/xlet
app.57.initial_class_name=org.cablelabs.xlet.FrontPanelExtTest.FrontPanelExtXlet
app.57.service=0x12355

*******************************************************************************
                                   TEST CONTROL:
*******************************************************************************

The following list of test cases is available.  To run a test case, key in the
test case number, then press ENTER.

    Press A (yellow trianble) for current status of all available indicators
    Press B (blue square) for current status of Text Display
    Test 0 : toggle POWER indicator reservation
    Test 1 : toggle RECORD indicator reservation
    Test 2 : toggle MESSAGE indicator reservation
    Test 3 : toggle RFBYPASS indicator reservation
    Test 4 : toggle TEXT DISPLAY reservation
    Test 5 : reset POWER indicator blink spec (rate)
    Test 6 : reset POWER indicator blink spec (duration)
    Test 7 : reset POWER indicator bright spec
    Test 8 : reset POWER indicator color spec
    Test 9 : reset RECORD indicator blink spec (rate)
    Test 10 : reset RECORD indicator blink spec (duration)
    Test 11 : reset RECORD indicator bright spec
    Test 12 : reset RECORD indicator color spec
    Test 13 : reset MESSAGE indicator blink spec (rate)
    Test 14 : reset MESSAGE indicator blink spec (duration)
    Test 15 : reset MESSAGE indicator bright spec
    Test 16 : reset MESSAGE indicator color spec
    Test 17 : reset RFBYPASS indicator blink spec (rate)
    Test 18 : reset RFBYPASS indicator blink spec (duration)
    Test 19 : reset RFBYPASS indicator bright spec
    Test 20 : reset RFBYPASS indicator color spec
    Test 21 : reset TEXT DISPLAY text string setting
    Test 22 : erase text from text display
    Test 23 : toggle TEXT DISPLAY text wrap setting
    Test 24 : reset TEXT DISPLAY blink spec (rate)
    Test 25 : reset TEXT DISPLAY blink spec (duration)
    Test 26 : reset TEXT DISPLAY bright spec
    Test 27 : reset TEXT DISPLAY color spe
    Test 28 : reset TEXT DISPLAY scroll spec (horizontal rate)
    Test 29 : reset TEXT DISPLAY scroll spec (vertical rate)
    Test 30 : reset TEXT DISPLAY scroll spec (hold duration)
    Test 31 : reset TEXT DISPLAY CLOCK mode setting
    Test 32 : reset TEXT DISPLAY CLOCK blink spec (rate)
    Test 33 : reset TEXT DISPLAY CLOCK blink spec (duration)
    Test 34 : reset TEXT DISPLAY CLOCK bright spec
    Test 35 : reset TEXT DISPLAY CLOCK color spec
    Test 36 : reset TEXT DISPLAY CLOCK scroll spec (horizontal rate)
    Test 37 : reset TEXT DISPLAY CLOCK scroll spec (vertical rate)
    Test 38 : reset TEXT DISPLAY CLOCK scroll spec (hold duration)

*******************************************************************************
                                   EVALUATION:
*******************************************************************************

Evaluate test results by viewing the TV screen and the console log.
