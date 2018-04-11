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

These Xlets can be used to exercise DVR Security functionality, especially
with regard to ExtendedFileAccessPermissions (implementation of which
motivated creation of these Xlets).

As of 20050812, only LocatorRecordingSpecs are exercised.

Configuration Parameters:

    1. Refer to the sample hostapp.properties files (*-ha.properties).
    2. Refer to the sample primary configuration files (*-primary.cfg)
    3. Refer to the sample secondary configuration files (other *.cfg files)
    4. Refer to the sample Permission Request Files (PRFs) (ocap.*.perm)

TODOs:

    1. After Bug #2501 is resolved, remove seconds-to-milliseconds
       RecordingProperties expiration period hack.
    2. Add support for ServiceContext RecordingSpecs.

Test Control:

Use TestLauncher to run these Xlets.

View the TV display

    Use the usual TestLauncher key presses to launch the TestMgrXlet

TestMgrXlet
===========
The main purpose of this Xlet is to cleanup and initialize recording requests.

After launching TestMgrxlet, view the console.
Press the INFO button to display a list of options at the console.

Secondary Xlets
===============
Refer to individual Test Descriptions (test-desc*.txt) for procedures.

    RecReqXlet - Non-MonApp Xlet used to exercise access to recording requests
    ListAccessMAXlet - MonApp Xlet used to exercise access to recording lists
    RecordingAlertXlet - Non-MonApp Xlet used to exercise RecordingAlertEvent
        registration and notification
    RecordingChangedMAXlet - MonApp Xlet used to exercise RecordingChangedEvent
        registration and notification

Primary Configuration File Format
=================================
The format of the *-primary.cfg files.

#
# Format (the entire first token uniquely identifies each request):
#
# Token 1:
#     1a. File Access Permissions of form "rwrwrw" or "null".
#         "-" indicates no access.  "null" indicates a null Extended
#         File Access Permissions Object ref should be specified.
#         If this token is "null", the subsequent "other" Org IDs
#         tokens will be ignored.
#     1b. ":" field separator
#     1c. Nearly-arbitrary Request Identifier (no ":" or "|" allowed)
# Token 2:
#     2a. "null" or comma-separated list of "other" Org IDs granted
#         Read Access
#     2b. ":" field separator
#     2c. "null" or comma-separated list of "other" Org IDs granted
#         Write Access
# Token 3:  OCAP Locator URL
# Token 4:  Start Time yyyy:MM:dd:hh:mm:ss
# Token 5:  duration (milliseconds)
# Token 6:  expiration period (seconds)
#

RecReqXlet (secondary) Configuration File Format
=================================
The format of the RR-#-xlet.cfg files.

# Pipe-delimited fields.
#
# Field 1: the request identifier (must map to primary config file)
# Field 2: the action
# Field 3: the expected exception (or null
