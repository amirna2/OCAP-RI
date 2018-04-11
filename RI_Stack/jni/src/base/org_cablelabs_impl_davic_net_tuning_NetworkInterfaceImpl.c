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

#include "org_cablelabs_impl_davic_net_tuning_NetworkInterfaceImpl.h"
#include <mpe_types.h>
#include <mpe_media.h>
#include <mpe_os.h>
#include <mpe_dbg.h>
#include <mpe_ed.h>
#include <mpe_prof.h>
#include "jni_util.h"
#include "profmgr.h"

/* if the QAM mode is not specified (eg, == -1), then default to this QAM mode */
#define DEFAULT_QAM_MODE    (0x10)  /* QAM256 */

// for profiling
#ifdef MPE_PROF_TUNING
static uint32_t ProfileChannelTune1 = 0; // impossible value, for clock time
#endif

/*
 * Class:     org_cablelabs_impl_davic_net_tuning_NetworkInterfaceImpl
 * Method:    nativeTune
 * Signature:
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_davic_net_tuning_NetworkInterfaceImpl_nativeTune(
        JNIEnv *env, jobject obj, jint tunerHandle, jobject listener,
        jint frequency, jint program_num, jint qam)
{
    mpe_EdEventInfo *edHandle;
    mpe_MediaTuneRequestParams reqParams;
    jboolean returnVal = JNI_TRUE;
    JNI_UNUSED(env);
    JNI_UNUSED(obj);

    // for profiling
    // unfortunately, there is no obvious initialization prior to this
    // which is where mpe_profAddLabel should go...
#ifdef MPE_PROF_TUNING
    mpe_profPopWhereStack(0); // pops key to select
    if (ProfileChannelTune1 == 0)
    {
        mpe_profAddLabel("native tune to tune complete", &ProfileChannelTune1);
        //MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI, "Add Label, ProfileChannelTune1 = %d\n",
        //  ProfileChannelTune1);
    }
    mpe_profSetWhere(0, ProfileChannelTune1); // select to tune complete
#endif

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "NetworkInterface JNI: nativeTune(tunerId=0x%X, freq=0x%X, progId=0x%X, mode=%d)\n",
            (int)tunerHandle, (int)frequency, (int)program_num, (int)qam);

    /* prepare notification data */
    if (mpe_edCreateHandle(listener, MPE_ED_QUEUE_TUNE_EVENTS, NULL,
            MPE_ED_TERMINATION_EVCODE, MPE_EVENT_SHUTDOWN, &edHandle)
            != MPE_SUCCESS)
    {
        return JNI_FALSE;
    }

    reqParams.tunerId = tunerHandle;
    reqParams.tuneParams.tuneType = MPE_MEDIA_TUNE_BY_TUNING_PARAMS;
    reqParams.tuneParams.frequency = frequency;
    reqParams.tuneParams.qamMode = (qam == -1) ? DEFAULT_QAM_MODE : qam;
    reqParams.tuneParams.programNumber = program_num;

    if (mpe_mediaTune(&reqParams, edHandle->eventQ, (void *) edHandle)
            != MPE_SUCCESS)
    {
        mpe_edDeleteHandle(edHandle);
        returnVal = JNI_FALSE;
    }

    return returnVal;
}
