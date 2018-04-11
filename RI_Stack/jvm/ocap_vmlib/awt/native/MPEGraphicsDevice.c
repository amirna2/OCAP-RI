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

#include "awt.h"
#include "common.h"

#include <mpe_gfx.h>
#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_os.h>
#include <mpe_disp.h>

#include <jni.h>

/*
 * function prototypes
 */

JNIEXPORT jint JNICALL Java_java_awt_MPEGraphicsDevice_pGetSurface(JNIEnv *env,
        jclass cls, jint deviceHandle)
{
    mpe_GfxSurface surface;
    UNUSED(cls);

    if (MPE_SUCCESS != mpe_dispGetGfxSurface((mpe_DispDevice) deviceHandle,
            &surface))
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError,
                "Could not access device surface!");
    }
    return (jint) surface;
}

/*
 * returns current config for the supplied device handle.
 */
JNIEXPORT jint JNICALL Java_java_awt_MPEGraphicsDevice_pGetConfig(JNIEnv *env,
        jobject cls, jint deviceHandle)
{
    mpe_Error err;
    mpe_DispDeviceConfig config;
    UNUSED(cls);

    err = mpe_dispGetCurrConfig((mpe_DispDevice) deviceHandle, &config);
    if (err != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, MPECachedIDs.AWTError, "Error getting config!");
        return 0;
    }
    return (jint) config;
}

/*
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * NOTE:
 * Under the ClientSim, having JNI_NAME() enabled for sync
 * can result in a hang.  For more information please see
 * Bugzilla #579.
 * Until this is fixed, this must remain as xJNI_NAME().
 *!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */

/**
 * The Toolkit.sync() method updates the visible screen.
 * If all drawing operations are immediate-mode, then this is
 * a no-op.
 */
JNIEXPORT void JNICALL Java_java_awt_MPEGraphicsDevice_pSync
(JNIEnv *env, jclass cls, jint deviceHandle)
{
    MPEAWT_TIME_INIT();
    UNUSED(env);
    UNUSED(cls);

    MPE_LOG( MPEAWT_LOGSYNC, MPEAWT_LOG_MOD, "MPEGraphicsDevice::pSync(%d)\n", deviceHandle );

    MPEAWT_TIME_START();

    mpe_dispFlushGfxSurface((mpe_DispDevice)deviceHandle);

    MPEAWT_TIME_END();
#if MPEAWT_DBGTIME
    MPE_LOG( MPEAWT_LOGTIME, MPEAWT_LOG_MOD, "MPEGraphicsDevice::pSync elapsed %d ms\n", time_elapsed );
#endif
}

