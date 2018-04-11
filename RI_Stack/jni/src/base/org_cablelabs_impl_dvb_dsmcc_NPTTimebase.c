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

#include "org_cablelabs_impl_dvb_dsmcc_NPTTimebase.h"
#include "mgrdef.h"
#include "mpe_file.h"
#include "mpe_dbg.h"
#include "mpe_si.h"
#include "mpe_media.h"

#include <inttypes.h> // for PRIu64 and PRId64

JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_dvb_dsmcc_NPTTimebase_nativeGetSTC(
        JNIEnv *env, jclass clz, jint siHandle)
{
    uint32_t pcrPID;
    uint32_t frequency;
    uint32_t program;
    uint32_t tunerID;
    uint64_t stc;
    mpe_Error retCode;
    jclass errClass;

    retCode = mpe_siLockForRead();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "GetSTC: Lock for read failed: %x\n", retCode);
        goto Fail;
    }

    // Extract the various pieces of important information from the SIDB
    // Namely, frequency, program, PCR PID
    retCode = mpe_siGetPcrPidForServiceHandle(siHandle, &pcrPID);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "GetSTC: Get PID For ServiceHandle failed: %x\n", retCode);
        mpe_siUnLock();
        goto Fail;
    }
    retCode = mpe_siGetFrequencyForServiceHandle(siHandle, &frequency);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "GetSTC: Get Frequency For ServiceHandle failed: %x\n", retCode);
        mpe_siUnLock();
        goto Fail;
    }
    retCode = mpe_siGetProgramNumberForServiceHandle(siHandle, &program);
    mpe_siUnLock();
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "GetSTC: Get Program Number For ServiceHandle failed: %x\n",
                retCode);
        goto Fail;
    }

    // Now, find the frequency.
    // TODO: Can I call MPEOS from here?  Or do I need to go through MPE?
    // FIXME: Probably shouldn't call MPEOS.
    retCode = mpeos_mediaFrequencyToTuner(frequency, &tunerID);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS,
                "GetSTC: Frequency to tuner failed: %x\n", retCode);
        goto Fail;
    }
    // And finally, extract the STC
    retCode = mpe_mediaGetSTC(tunerID, pcrPID, &stc);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILESYS, "GetSTC: GetSTC failed: %x\n",
                retCode);
        goto Fail;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "GetSTC: STC: %"PRIu64"\n", stc);
    return (jlong) stc;

    Fail: errClass = (*env)->FindClass(env,
            "org/dvb/dsmcc/MPEGDeliveryException");
    (*env)->ThrowNew(env, errClass, "Unable to retrieve STC");

    return (jlong) 0;
}
