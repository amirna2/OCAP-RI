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

The SectionFilteringTest performs the following steps:

1. Tune to a given sourceID.

2. Create a SimpleSectionFilter.

3. Filter the TransportStream for a section with a given PID.  The test offers
   two options for configuring a PID: 

   (a) configure a pid directly, by setting hostapp argument 'pid', or

   (b) search the PMT for a PID associated with the known component tag.  In
       this case, the hostapp parameter 'tag' must be set to a known value.

       The following WIKI page lists association tags for Portland carousels on
       Freq=699MHz, QAM 64: http://ocap/ow.asp?BridgeportPDXObjectCarouselConfig.

4. If a matching section is detected, output the section data to the TV screen
   and the console log.

*******************************************************************************
                                    CONFIGURATION:
*******************************************************************************

SAMPLE HOSTAPP.PROPERTIES PARAMETERS:

app.1.application_identifier=0x000000017202
app.1.application_control_code=PRESENT
app.1.visibility=VISIBLE
app.1.priority=220
app.1.application_name=SectionFilteringTest
app.1.base_directory=/snfs/qa/xlet
app.1.initial_class_name=org.cablelabs.xlet.SectionFilteringTest.SectionFilteringTestXlet
app.1.args.0=source_id=2700
app.1.args.1=pid=5120
app.1.args.2=filter_by_tag=true
app.1.args.3=tag_id=20
app.1.args.4=tag=1
#NOTE: pid=0 should always be available.

'source_id'
   Inband channel source ID.

'pid'
   section ID.

'filter_by_tag'
   'filter_by_tag=false' filters the TransportStream for a section with the
      PID, configured via parameter 'pid'.
      Parameters 'tag_id' and 'tag' are ignored.
      (See option 3a in the Test Description above).

   'filter_by_tag=true' configures the test to search the PMT for a PID,
      associated with the known component tag configured via parameter 'tag'.
      Parameter 'pid' is ignored.
      (See option 3b in the Test Description above).

'tag_id'
   For internal use.  Should not change.

'tag'
   A tag associated with a section PID.

*******************************************************************************
                                     EXECUTION:
*******************************************************************************

1. Configure the test via the hostapp.properties file (see example above).

2. In the QA Test Launcher menu, select the 'Transport Stream Test Service'.

3. Select the SectionFilteringTest.

4. Wait for the requested section data to arrive.  It will be output to the
   screen and to the console log.

*******************************************************************************
                                    EVALUATION:
*******************************************************************************

Evaluate by viewing the TV screen and the console log.

THE TEST PASSED IF:

(a) A section with a given PID is matched, and the following message is printed:

    ********************************
    SECTION DATA: . . .

    ********************************  

(b) There are no exceptions, failures or errors present in the log.

THE TEST MAY HAVE FAILED IF:

(a) The requested section data never arrived.

(b) There are exceptions, failures or errors present in the log.
