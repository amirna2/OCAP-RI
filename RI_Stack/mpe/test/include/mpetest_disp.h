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


/**
 * MPE / MPEOS functions will be re-defined here using macros.
 * This will make it easy to support MPE or MPEOS tests.  A simple
 * recompilation will be required to compile a set of tests for MPE or MPEOS.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */
#ifndef _MPETEST_DISP_H_
#define _MPETEST_DISP_H_ 1

#ifdef TEST_MPEOS
# define MPETEST_DISP(x)  mpeos_ ## x
#else
# define MPETEST_DISP(x)  mpe_ ## x
#endif /* TEST_MPEOS */

#define dispGetGfxSurface  MPETEST_DISP(dispGetGfxSurface)
#define dispFlushGfxSurface  MPETEST_DISP(dispFlushGfxSurface)
#define dispGetScreenCount  MPETEST_DISP(dispGetScreenCount)
#define dispGetScreens  MPETEST_DISP(dispGetScreens)
#define dispGetScreenInfo  MPETEST_DISP(dispGetScreenInfo)
#define dispGetDeviceCount  MPETEST_DISP(dispGetDeviceCount)
#define dispGetDevices  MPETEST_DISP(dispGetDevices)
#define dispGetDeviceInfo  MPETEST_DISP(dispGetDeviceInfo)
#define dispGetConfigCount  MPETEST_DISP(dispGetConfigCount)
#define dispGetConfigs  MPETEST_DISP(dispGetConfigs)
#define dispGetCurrConfig  MPETEST_DISP(dispGetCurrConfig)
#define dispSetCurrConfig  MPETEST_DISP(dispSetCurrConfig)
#define dispWouldImpact  MPETEST_DISP(dispWouldImpact)
#define dispGetConfigInfo  MPETEST_DISP(dispGetConfigInfo)
#define dispGetCoherentConfigCount  MPETEST_DISP(dispGetCoherentConfigCount)
#define dispGetCoherentConfigs  MPETEST_DISP(dispGetCoherentConfigs)
#define dispSetCoherentConfig  MPETEST_DISP(dispSetCoherentConfig)
#define dispGetConfigSetCount  MPETEST_DISP(dispGetConfigSetCount)
#define dispGetConfigSet  MPETEST_DISP(dispGetConfigSet)
#define dispSetBGColor  MPETEST_DISP(dispSetBGColor)
#define dispGetBGColor  MPETEST_DISP(dispGetBGColor)
#define dispDisplayBGImage  MPETEST_DISP(dispDisplayBGImage)
#define dispGetBGImageSize  MPETEST_DISP(dispGetBGImageSize)
#define dispGetOutputPortCount  MPETEST_DISP(dispGetOutputPortCount)
#define dispGetOutputPorts  MPETEST_DISP(dispGetOutputPorts)
#define dispEnableOutputPort  MPETEST_DISP(dispEnableOutputPort)
#define dispGetOutputPortInfo  MPETEST_DISP(dispGetOutputPortInfo)

// #define threadSleep            MPETEST_DISP(threadSleep)
#endif /* _MPETEST_DISP_H_ */ 
