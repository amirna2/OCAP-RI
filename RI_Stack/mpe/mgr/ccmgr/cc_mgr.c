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

#include <osmgr.h>
#include <sysmgr.h>
#include <cc_mgr.h>
#include <mgrdef.h>
#include <mpe_os.h>
#include <mpe_file.h>
#include <mpeos_util.h>

/* resolves closed captioning manager os functions and definitions */
#include <mpeos_caption.h>
#include <mpe_dbg.h>

/* exported from mpeos_caption.c */

extern mpe_Error mpeos_ccInit(void);

mpe_cc_ftable_t cc_ftable =
{ mpe_ccInit,

mpeos_ccSetAttributes, mpeos_ccGetAttributes, mpeos_ccSetAnalogServices,
        mpeos_ccSetDigitalServices, mpeos_ccGetAnalogServices,
        mpeos_ccGetDigitalServices, mpeos_ccSetClosedCaptioning,
        mpeos_ccGetCapability, mpeos_ccGetClosedCaptioning, mpeos_ccGetSupportedServiceNumbersCount,
        mpeos_ccGetSupportedServiceNumbers };

void mpe_ccSetup(void)
{
    mpe_sys_install_ftable(&cc_ftable, MPE_MGR_TYPE_CC);
}

static mpe_Bool inited = false;
void mpe_ccInit()
{

    if (!inited)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_CC,
                "<ccinit> Initializing MPE CLOSED CAPTIONING\n");

        inited = true;
        (void) mpeos_ccInit();

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_CC, "<ccinit> MPE CC Init Complete\n");
    }
}
