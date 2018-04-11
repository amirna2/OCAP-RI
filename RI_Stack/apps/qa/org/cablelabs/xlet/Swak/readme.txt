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
              WARNING: THIS APPLICATION USES NON-COMPLIANT APIs:
*******************************************************************************

This application uses non-compliant APIs.  
The last svn revision to contain the source files is 31430.

Swak (Swiss Army Knife).

The objective of Swak is to create a single unified test xlet framework designed
from the outset as a tool for OCAP developers rather than application developers.
It seeks to remove the need to create variants, being extensible by design. New
functionality can be added just by dropping another class file into the source
directory. The various functional areas are decoupled (to the extent that
OCAP permits) so they can be exercised independently. A very flexible scripted
and threaded automation mechanism makes complex sequences and combinations
of functions arbitrary and controllable. The emphasis is on accessing low
level control at the OCAP API rather than emulating some sophisticated user
service layered over that API. Swak subsumes the xlets WatchTV, Tunetest,
ScaledVideo, DVRTestRunner, and WatchTVLive without loss of testability.


For lots more detail, see the design document: "Swak: the Swiss Army Knife of
OCAP Test Xlets", R. Trammell, November 2005.

The primitive on-screen-menu GUI is "self-explanatory". Scripts can be read from
files, short ones from hostapp.properties args. Swak initially comes with plug-ins
to support these functional areas: Automation, Service Discovery, Service Selection, 
PIP/Scaled-Video, DVR, and TimeShiftBuffer. Others for Audio and
Closed Captions are planned.

The executable should be found in /snfs/qa/xlet/org/cablelabs/xlet/Swak.
The config files (channel maps, TestBot scripts) are expected in /snfs/qa/xlet.

For an example hostapp.properties entry, see the accompanying file:
   hostapp.properties.swak


