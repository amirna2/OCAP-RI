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
PropertiesTest gets the values of application properties, and displays them
on a TV screen.  The hostapp.properties file contains three entries for the
PropertiesTest: UnsignedPropertiesTest, SignedPropertiesTest, and
MonAppPropertiesTest, each testing a different security setting.

Configuration Parameters:
-none-

Test Control:
-none-

Evaluate by viewing the TV screen.

Sample Output for the AllPropertiesTest:

 "user.dir"                         :/snfs/qa/xlet
 "ocap.j.location"                  :/snfs/qa/xlet
 "havi.specification.vendor "       :HAVi
 "havi.specification.name "         :HAVi 1.1
 "havi.specifiation.version "       :1.1
 "havi.implementation.vendor "      :Cable Television Laboratories, Inc.
 "ocap.version"                     :1.0
 "org.cablelabs.ocap.build"            :OCAP1-v0.8.1-build.426
 "dvb.returnchannel.timeout "       :10000
 "mhp.profile.enhanced_broadcast "  :YES
 "mhp.eb.version.major "            :1
 "mhp.eb.version.minor "            :0
 "mhp.eb.version.micro "            :0
 "mhp.profile.interactive_broadcast":YES
 "mhp.ib.version.major "            :1
 "mhp.ib.version.minor "            :0
 "mhp.ib.version.micro "            :0
 "mhp.profile.internet_access "     :NULL
 "mhp.ia.version.major "            :NULL
 "mhp.ia.version.minor "            :NULL
 "mhp.ia.version.micro "            :NULL

Sample Output for the UnsignedPropertiesTest, SignedPropertiesTest
and MonAppPropertiesTest:

"user.dir"                         :AccessControlException
 "ocap.j.location"                  :/snfs/qa/xlet
 "havi.specification.vendor "       :HAVi
 "havi.specification.name "         :HAVi 1.1
 "havi.specifiation.version "       :1.1
 "havi.implementation.vendor "      :Cable Television Laboratories, Inc.
 "ocap.version"                     :1.0
 "org.cablelabs.ocap.build"            :AccessControlException
 "dvb.returnchannel.timeout "       :10000
 "mhp.profile.enhanced_broadcast "  :YES
 "mhp.eb.version.major "            :1
 "mhp.eb.version.minor "            :0
 "mhp.eb.version.micro "            :0
 "mhp.profile.interactive_broadcast":YES
 "mhp.ib.version.major "            :1
 "mhp.ib.version.minor "            :0
 "mhp.ib.version.micro "            :0
 "mhp.profile.internet_access "     :NULL
 "mhp.ia.version.major "            :NULL
 "mhp.ia.version.minor "            :NULL
 "mhp.ia.version.micro "            :NULL

Xlet Control:
initXlet
	Get values of application properties.
	Set up scene to display text strings.
startXlet
	Display list of properties on TV screen.
pauseXlet
	Clear video.
destroyXlet
	Clear video.
