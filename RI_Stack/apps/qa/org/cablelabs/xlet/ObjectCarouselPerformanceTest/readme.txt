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
 
The ObjectCarouselPerformance test reads object carousel files listed in
config.properties, and records the time it took to read them.  The test can be
configured to:

- read files from in-band or out-of-band object carousels;

- read a given list of files concurrently or sequentially;

- prefetch files by name;

- load files before reading them;

- load directories asynchronously before reading them;

- use the NSAP address format to connect to a carousel;

- read a list of files for a given number of iterations;

The user interface of the application allows for scrolling through the list of
files and directories read from the object carousel, by pressing the UP_ or
DOWN_ARROW remote control key.

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

The test is expected to read a list of files from either an inband or an
out-of-band carousel, specified via the 'carousel_id' parameter in the
hostapp.properties file.  The files to read are listed in the config.properties
file.

SAMPLE CONFIG.PROPERTIES PARAMETERS:

oc_perf_oob_0=/TW_SIMPLE_APP/org/cablelabs/lib/utils/ArgParser.class
oc_perf_oob_1=/TW_SIMPLE_APP/org/cablelabs/lib/utils/FillBuffer.class
oc_perf_oob_2=/TW_SIMPLE_APP/org/cablelabs/lib/utils/OcapTuner.class
oc_perf_oob_3=/TW_SIMPLE_APP/org/cablelabs/lib/utils/PerfLog.class
oc_perf_oob_4=/TW_SIMPLE_APP/org/cablelabs/lib/utils/PerfLogTest$1.class

oc_perf_inb_0=/TW_EVENTS/SimpleTest/org/cablelabs/lib
oc_perf_inb_1=/TW_EVENTS/SimpleTest/org/cablelabs/lib/utils
oc_perf_inb_2=/TW_EVENTS/SimpleTest/org/cablelabs/lib/utils/OcapTuner.class
oc_perf_inb_3=/TW_EVENTS/SimpleTest/org/cablelabs/lib/utils/PerfLog.class
oc_perf_inb_4=/TW_EVENTS/SimpleTest/org/cablelabs/lib/utils/PerfLogTest$1.class

'oc_perf_oob'
Path name of a file to read from an out-of-band carousel.

'oc_perf_inb'
Path name of a file to read from an inband carousel.

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.37.application_identifier=0x000000017300
app.37.application_control_code=PRESENT
app.37.visibility=VISIBLE
app.37.priority=220
app.37.application_name=OCPERF_IB_CONC_ASYNCH_LOAD
app.37.base_directory=/snfs/qa/xlet
app.37.initial_class_name=org.cablelabs.xlet.ObjectCarouselPerformanceTest.ObjectCarouselPerformanceXlet
app.37.service=0x12352
app.37.args.0=config_file=/snfs/qa/xlet/config.properties
app.37.args.1=file_system=InbandOC
app.37.args.2=source_id=4050
app.37.args.3=carousel_id=25
app.37.args.4=read_concurrently=true
app.37.args.5=use_nsap_address=false
app.37.args.6=prefetch=false
app.37.args.7=synch_load=false
app.37.args.8=asynch_load=true
app.37.args.9=block_size=0
app.37.args.10=iterations=3

'config_file'
 Points to the location of config.properties file.

'file_system'
 This parameter can be set to InbandOC or OOBOC.

'source_id'
 Source ID of the service to which the object carousel is bound.

'carousel_id
 Carousel ID.

'use_nsap_address'
 Can be set to 'true' or 'false'.  When set to 'true', the xlet uses the NSAP
 address format to attach to a carousel.

'read_concurrently'
 Can be set to 'true' or 'false'.  When set to 'true', the xlet reads all the
 files listed in config.properties concurrently.  When 'read_concurrently=false',
 the xlet reads files sequentially.

'prefetch'
 Can be set to 'true' or 'false'. Setting 'prefetch' to 'true' enables
 prefetching of files and directories.

'synch_load'
 Can be set to 'true' or 'false'.  Setting 'synch_load' to 'true' enables
 synchronous loading of files, before they are read.

'asynch_load'
 Can be set to 'true' or 'false'.  Setting 'asynch_load' to 'true' enables
 asynchronous loading of directories, before they are read.

NOTE 1: 'synch_load' and 'asynch_load' cannot both be set to true at the same
        time.

NOTE 2: 'prefetch' and 'synch_load' or 'asynch_load' can both be set to true
        at the same time.

'block_size'
 This parameter is used, when reading a file.  If 'block_size' is set to '0'
 (default), the xlet will reset blocksize to equal filesize for each file.

'iterations'
 The files are read as many times, as it is specified by the 'iterations'
 parameter.  The point of reading files from an object carousel multiple times
 is to compare performance data (the time it takes to read files).

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. Configure the test via the hostapp.properties and the config.properties
   files (see example above).

2. In the QA Test Launcher menu, select the 'Object Carousel Test Service'.

3. Select the name of your test case.  Test case name is the value of the
   'application_name' parameter in the hostapp file.  Currently, the hostapp
   file contains four ObjectCarouselPerformance test cases:

   - OCPERF_IB_CONC_ASYNCH_LOAD
   - OCPERF_IB_NSAP_PREFETCH
   - OCPERF_OOB_CONC_NSAP_PREFETCH
   - OCPERF_OOB_SYNCH_LOAD

   Test cases are named based on test options turned on in the hostapp.properties
   for the test.

4. Wait for the test output to appear on the TV screen and in the console log.

*******************************************************************************
                                   SAMPLE TEST CASES:
*******************************************************************************

- InbandOC, Sequential Reading, Prefetch On, Load Off
- InbandOC, Sequential Reading, Prefetch Off, Load On
- InbandOC, Sequential Reading, Prefetch On, Load On
- InbandOC, Concurrent Reading, Prefetch On, Load Off
- InbandOC, Concurrent Reading, Prefetch Off, Load On
- InbandOC, Concurrent Reading, Prefetch On, Load On
- OOBOC, Sequential Reading, Prefetch On, Load Off
- OOBOC, Sequential Reading, Prefetch Off, Load On
- OOBOC, Sequential Reading, Prefetch On, Load On
- OOBOC, Concurrent Reading, Prefetch On, Load Off
- OOBOC, Concurrent Reading, Prefetch Off, Load On
- OOBOC, Concurrent Reading, Prefetch On, Load On
- InbandOC, NSAP On
- InbandOC, NSAP Off
- OOBOC, NSAP On
- OOBOC, NSAP Off

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.

THE TEST PASSED IF:

(a) All the files and directories listed in the config.properties were read
    successfully and printed to the screen, for the number of iterations
    specified in the hostapp.properties.  

(b) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) Some or all of the files listed in the config.properties were not read.
    If this happens, the file name will not be printed to the screen,
    or you will see a "READ FAILED" statement next to file's name.

(b) There are exceptions, failures or errors present in the log.
