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

#ifndef _MPE_DISPMGR_BINDINGS1_H_
#define _MPE_DISPMGR_BINDINGS1_H_

#include "mpe_sys.h"
#include "../mgr/include/mgrdef.h"
#include "../mgr/include/dispmgr.h"

#define mpe_dispmgr_ftable ((mpe_disp_ftable_t*)(FTABLE[MPE_MGR_TYPE_DISP]))

#define mpe_dispInit (mpe_dispmgr_ftable->mpe_disp_init_ptr)

/* Display Discovery/Configuration */
#define mpe_dispGetScreenCount (mpe_dispmgr_ftable->mpe_dispGetScreenCount_ptr)
#define mpe_dispGetScreens (mpe_dispmgr_ftable->mpe_dispGetScreens_ptr)
#define mpe_dispGetScreenInfo (mpe_dispmgr_ftable->mpe_dispGetScreenInfo_ptr)
#define mpe_dispGetDeviceCount (mpe_dispmgr_ftable->mpe_dispGetDeviceCount_ptr)
#define mpe_dispGetDevices (mpe_dispmgr_ftable->mpe_dispGetDevices_ptr)
#define mpe_dispGetDeviceInfo (mpe_dispmgr_ftable->mpe_dispGetDeviceInfo_ptr)
#define mpe_dispGetConfigCount (mpe_dispmgr_ftable->mpe_dispGetConfigCount_ptr)
#define mpe_dispGetConfigs (mpe_dispmgr_ftable->mpe_dispGetConfigs_ptr)
#define mpe_dispGetCurrConfig (mpe_dispmgr_ftable->mpe_dispGetCurrConfig_ptr)
#define mpe_dispSetCurrConfig (mpe_dispmgr_ftable->mpe_dispSetCurrConfig_ptr)
#define mpe_dispWouldImpact (mpe_dispmgr_ftable->mpe_dispWouldImpact_ptr)
#define mpe_dispGetConfigInfo (mpe_dispmgr_ftable->mpe_dispGetConfigInfo_ptr)
#define mpe_dispGetCoherentConfigCount (mpe_dispmgr_ftable->mpe_dispGetCoherentConfigCount_ptr)
#define mpe_dispGetCoherentConfigs (mpe_dispmgr_ftable->mpe_dispGetCoherentConfigs_ptr)
#define mpe_dispSetCoherentConfig (mpe_dispmgr_ftable->mpe_dispSetCoherentConfig_ptr)
#define mpe_dispGetConfigSetCount (mpe_dispmgr_ftable->mpe_dispGetConfigSetCount_ptr)
#define mpe_dispGetConfigSet (mpe_dispmgr_ftable->mpe_dispGetConfigSet_ptr)
#define mpe_dispSetBGColor (mpe_dispmgr_ftable->mpe_dispSetBGColor_ptr)
#define mpe_dispGetBGColor (mpe_dispmgr_ftable->mpe_dispGetBGColor_ptr)
#define mpe_dispDisplayBGImage (mpe_dispmgr_ftable->mpe_dispDisplayBGImage_ptr)
#define mpe_dispBGImageGetSize (mpe_dispmgr_ftable->mpe_dispBGImageGetSize_ptr)
#define mpe_dispBGImageNew (mpe_dispmgr_ftable->mpe_dispBGImageNew_ptr)
#define mpe_dispBGImageDelete (mpe_dispmgr_ftable->mpe_dispBGImageDelete_ptr)
#define mpe_dispGetOutputPortCount (mpe_dispmgr_ftable->mpe_dispGetOutputPortCount_ptr)
#define mpe_dispGetOutputPorts (mpe_dispmgr_ftable->mpe_dispGetOutputPorts_ptr)
#define mpe_dispEnableOutputPort (mpe_dispmgr_ftable->mpe_dispEnableOutputPort_ptr)
#define mpe_dispGetOutputPortInfo (mpe_dispmgr_ftable->mpe_dispGetOutputPortInfo_ptr)

#define mpe_dispSetRFBypassState (mpe_dispmgr_ftable->mpe_dispSetRFBypassState_ptr)
#define mpe_dispGetRFBypassState (mpe_dispmgr_ftable->mpe_dispGetRFBypassState_ptr)
#define mpe_dispSetRFChannel     (mpe_dispmgr_ftable->mpe_dispSetRFChannel_ptr)
#define mpe_dispGetRFChannel     (mpe_dispmgr_ftable->mpe_dispGetRFChannel_ptr)
#define mpe_dispGetDFC           (mpe_dispmgr_ftable->mpe_dispGetDFC_ptr)
#define mpe_dispGetSupportedDFCCount (mpe_dispmgr_ftable->mpe_dispGetSupportedDFCCount_ptr)
#define mpe_dispGetSupportedDFCs (mpe_dispmgr_ftable->mpe_dispGetSupportedDFCs_ptr)
#define mpe_dispCheckDFC         (mpe_dispmgr_ftable->mpe_dispCheckDFC_ptr)
#define mpe_dispSetDFC           (mpe_dispmgr_ftable->mpe_dispSetDFC_ptr)
#define mpe_dispSetDefaultPlatformDFC (mpe_dispmgr_ftable->mpe_dispSetDefaultPlatformDFC_ptr)

#define mpe_dispGetVideoOutputPortOption (mpe_dispmgr_ftable->mpe_dispGetVideoOutputPortOption_ptr)
#define mpe_dispSetVideoOutputPortOption (mpe_dispmgr_ftable->mpe_dispSetVideoOutputPortOption_ptr)

#define mpe_dispGetGfxSurface   (mpe_dispmgr_ftable->mpe_dispGetGfxSurface_ptr)
#define mpe_dispFlushGfxSurface (mpe_dispmgr_ftable->mpe_dispFlushGfxSurface_ptr)

/* DSExt */
#define mpe_dispSetMainVideoOutputPort  (mpe_dispmgr_ftable->mpe_dispSetMainVideoOutputPort_ptr)
#define mpe_dispGetDisplayAttributes    (mpe_dispmgr_ftable->mpe_dispGetDisplayAttributes_ptr)
#define mpe_dispIsDisplayConnected      (mpe_dispmgr_ftable->mpe_dispIsDisplayConnected_ptr)
#define mpe_dispIsContentProtected      (mpe_dispmgr_ftable->mpe_dispIsContentProtected_ptr)
//#define mpe_dispIsDynamicConfigurationSupported                 (mpe_dispmgr_ftable->mpe_dispIsDynamicConfigurationSupported_ptr)

#define mpe_dispGetSupportedFixedVideoOutputConfigurations      (mpe_dispmgr_ftable->mpe_dispGetSupportedFixedVideoOutputConfigurations_ptr)
#define mpe_dispGetSupportedFixedVideoOutputConfigurationCount  (mpe_dispmgr_ftable->mpe_dispGetSupportedFixedVideoOutputConfigurationCount_ptr)

#define mpe_dispGetSupportedDynamicVideoOutputConfigurations    (mpe_dispmgr_ftable->mpe_dispGetSupportedDynamicVideoOutputConfigurations_ptr)
#define mpe_dispGetSupportedDynamicVideoOutputConfigurationCount  (mpe_dispmgr_ftable->mpe_dispGetSupportedDynamicVideoOutputConfigurationCount_ptr)
#define mpe_dispGetCurVideoOutputConfiguration                  (mpe_dispmgr_ftable->mpe_dispGetCurVideoOutputConfiguration_ptr)
#define mpe_dispSetCurVideoOutputConfiguration                  (mpe_dispmgr_ftable->mpe_dispSetCurVideoOutputConfiguration_ptr)
#define mpe_dispRegister    (mpe_dispmgr_ftable->mpe_dispRegister_ptr)
#define mpe_dispUnregister  (mpe_dispmgr_ftable->mpe_dispUnregister_ptr)
#define mpe_dispGetDeviceDest  (mpe_dispmgr_ftable->mpe_dispGetDeviceDest_ptr)

#endif
