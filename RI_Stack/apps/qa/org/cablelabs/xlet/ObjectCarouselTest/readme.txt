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
 
The ObjectCarousel test reads the Object Carousel file system and outputs to
the screen a list of all the files present in the carousel directory tree, and
the the length of time in milliseconds it took to read the file. Depending on
the configuration, the test will read either an In-Band or an Out-Of-Band
Object Carousel.

The user interface of the application allows for scrolling through the list of
files read from the object carousel, by pressing the UP_ or DOWN_ARROW remote
control key.

*******************************************************************************
                                   CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS FOR AN INBAND CAROUSEL:

app.35.application_identifier=0x000000014302
app.35.application_control_code=PRESENT
app.35.visibility=VISIBLE
app.35.priority=220
app.35.application_name=ObjectCarouselIB
app.35.base_directory=/snfs/qa/xlet
app.35.initial_class_name=org.cablelabs.xlet.ObjectCarouselTest.ObjectCarouselTestXlet
app.35.service=0x12352
app.35.args.0=file_system=IBOC
app.35.args.1=frequency=591000000
app.35.args.2=prog_num=1
app.35.args.3=qam=8
app.35.args.4=carousel_id=25

SAMPLE HOSTAPP.PROPERTIES PARAMETERS FOR AN OUT-OF-BAND CAROUSEL:

app.36.application_identifier=0x000000014303
app.36.application_control_code=PRESENT
app.36.visibility=VISIBLE
app.36.priority=220
app.36.application_name=ObjectCarouselOOB
app.36.base_directory=/snfs/qa/xlet
app.36.initial_class_name=org.cablelabs.xlet.ObjectCarouselTest.ObjectCarouselTestXlet
app.36.service=0x12352
app.36.args.0=file_system=OOBOC
app.36.args.1=frequency=-1
app.36.args.2=prog_num=99
app.36.args.3=qam=-1
app.36.args.4=carousel_id=6

'file_system'
 This parameter can be set to IBOC or OOBOC.

'frequency'
 Frequency of the service to which the object carousel is bound.
 Set to '-1' for out-of-band.

'prog_num'
 Program number of the service to which the object carousel is bound.

'qam'
 Modulation mode of the service to which the object carousel is bound.
 Set to '-1' for out-of-band.

'carousel_id
 Carousel ID.

*******************************************************************************
                                    EXECUTION:
*******************************************************************************

1. Configure the test via the hostapp.properties file (see example above).

2. In the QA Test Launcher menu, select the 'Object Carousel Test Service'.

3. Select the ObjectCarouselIB, or the ObjectCarouselOOB test name, and wait
   for the test output to appear on the TV screen and in the console log.

*******************************************************************************
                                   EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.

THE TEST PASSED IF:

(a) A list of files was printed to the TV screen and the console log.

(b) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) No file names were printed to the TV screen or the console log.

(b) There are exceptions, failures or errors present in the log.
