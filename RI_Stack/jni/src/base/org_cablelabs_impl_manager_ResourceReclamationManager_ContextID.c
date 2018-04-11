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

#include <org_cablelabs_impl_manager_ResourceReclamationManager_ContextID.h>

#include <mpe_dbg.h>
#include <mpe_os.h>

#include "jni_util.h"

#if defined(JVM_FEATURE_EVM)
/* This isn't prototyped by javah. */
JNIEXPORT jlong JNICALL Evm_org_cablelabs_impl_manager_ResourceReclamationManager_00024ContextID_nSet
(JNIEnv *env, jclass cls, jlong id);

/**
 * EVM-specific version of nSet().
 * This version can be invoked in 1/100th the time due to a more specific contract.
 * The code promises to be "fast", allowing for simplified transition to native.
 */
JNIEXPORT jlong JNICALL Evm_org_cablelabs_impl_manager_ResourceReclamationManager_00024ContextID_nSet
(JNIEnv *env, jclass cls, jlong id)
{
    return Java_org_cablelabs_impl_manager_ResourceReclamationManager_00024ContextID_nSet(env, cls, id);
}
#endif

/**
 * Native method used to implement {@link #set(long)}.
 * Sets the <i>context id</i> for the current thread.
 * @param contextId the new contextId
 * @return the old contextId
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_ResourceReclamationManager_00024ContextID_nSet(
        JNIEnv *env, jclass cls, jlong id)
{
    mpe_ThreadPrivateData *data = NULL;
    mpe_Error err;
    jlong oldId = 0;
    JNI_UNUSED(cls);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_JNI, "ContextID::nSet(%016llx)\n", id);

    if ((MPE_SUCCESS != (err
            = mpe_threadGetPrivateData((mpe_ThreadId) 0, &data))) || (data
            == NULL))
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "ContextID::nSet() - could not get thread private data (%08x)\n",
                err);
        jniutil_throwByName(env, "java/lang/RuntimeException",
                "Could not get private thread data");
    }
    else
    {
        oldId = data->memCallbackId;
        data->memCallbackId = id;

        MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_JNI,
                "ContextID::nSet(%016llx) -> %016llx\n", id, oldId);
    }

    return oldId;
}
