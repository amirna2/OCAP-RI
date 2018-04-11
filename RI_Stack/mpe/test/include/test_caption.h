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

#ifndef _TEST_CC_H_
#define _TEST_CC_H_ 1

#include <mpeTest.h>

#ifdef TEST_CC_MPEOS
# include <mpeos_dbg.h>
# include <mpeos_caption.h>
# define MPE_CC(x) mpeos_ ## x
#else
# include <mpe_dbg.h>
# include <mpe_caption.h>
# define MPE_CC(x) mpe_ ## x
#endif  /* TEST_CC_MPEOS */

/* define CC API here */

#define ccSetAttributes				MPE_CC(ccSetAttributes)
#define ccGetAttributes				MPE_CC(ccGetAttributes)
#define ccSetDigitalServices        MPE_CC(ccSetDigitalServices)
#define ccGetDigitalServices		MPE_CC(ccGetDigitalServices)
#define ccSetAnalogServices			MPE_CC(ccSetAnalogServices)
#define ccGetAnalogServices			MPE_CC(ccGetAnalogServices)

#define ccSetClosedCaptioning       MPE_CC(ccSetClosedCaptioning)
#define ccGetClosedCaptioning       MPE_CC(ccGetClosedCaptioning)

/* ccGetAttributes() */
NATIVEEXPORT_API void vpk_test_ccGetAttributes1(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccGetAttributes2(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccAttributes(CuTest *tc);

NATIVEEXPORT_API void vpk_run_ccAttributes(void);

NATIVEEXPORT_API void vpk_run_ccSetAttributes(void);

/* ccGetAnalogServices() */
NATIVEEXPORT_API void vpk_test_ccGetAnalogServices1(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccGetAnalogServices2(CuTest *tc);

NATIVEEXPORT_API void vpk_run_ccGetAnalogServices(void);

/* ccGetDigitalServices() */
NATIVEEXPORT_API void vpk_test_ccGetDigitalServices1(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccGetDigitalServices2(CuTest *tc);

NATIVEEXPORT_API void vpk_run_ccGetDigitalServices(void);

/* ccSetAnalogServices() */
NATIVEEXPORT_API void vpk_test_ccSetAnalogServices1(CuTest *tc);

NATIVEEXPORT_API void vpk_run_ccSetAnalogServices(void);

/* ccSetDigitalServices() */
NATIVEEXPORT_API void vpk_test_ccSetDigitalServices1(CuTest *tc);

NATIVEEXPORT_API void vpk_run_ccSetDigitalServices(void);

/* ccSetClosedCaptioningState() */
NATIVEEXPORT_API void vpk_test_ccSetClosedCaptioningState1(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccSetClosedCaptioningState2(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccSetClosedCaptioningState3(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccSetClosedCaptioningState4(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccSetClosedCaptioningState5(CuTest *tc);
NATIVEEXPORT_API void vpk_test_ccSetClosedCaptioningState6(CuTest *tc);

NATIVEEXPORT_API void vpk_run_ccSetClosedCaptioningStateOn(void);
NATIVEEXPORT_API void vpk_run_ccSetClosedCaptioningStateOff(void);
NATIVEEXPORT_API void vpk_run_ccSetClosedCaptioningStateOnMute(void);

#endif /* #ifndef _TEST_CC_H_ */
