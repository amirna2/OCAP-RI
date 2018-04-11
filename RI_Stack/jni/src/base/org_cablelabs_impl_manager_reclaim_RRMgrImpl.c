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

#include <org_cablelabs_impl_manager_reclaim_RRMgrImpl.h>
#include <org_cablelabs_impl_manager_reclaim_Callback.h>

#include <mpe_dbg.h>
#include <mpe_types.h>
#include <mpe_os.h>

#include "jni_util.h"

static mpe_MemColor type2Color(jint type);
static jint color2Type(mpe_MemColor color);
static jint color2Reason(mpe_MemColor color);
static int32_t releaseResources(mpe_MemColor color, int32_t size,
        int64_t contextId, void* data);
static void initIds(JNIEnv *env);

/**
 * Cached value for JVM to be used on subsequent attaches.
 */
static JavaVM *gJvm = NULL;

/**
 * Convert RRMgrImpl type flag to color.
 * Valid types are CALLBACK_JAVA, CALLBACK_MONAPP, and CALLBACK_DESTROY.
 */
static mpe_MemColor type2Color(jint type)
{
    switch (type)
    {
    default:
    case org_cablelabs_impl_manager_reclaim_RRMgrImpl_CALLBACK_JAVA:
        return MPE_MEM_JVM;
    case org_cablelabs_impl_manager_reclaim_RRMgrImpl_CALLBACK_MONAPP:
        return MPE_MEM_CALLBACK_SYSEVENT;
    case org_cablelabs_impl_manager_reclaim_RRMgrImpl_CALLBACK_DESTROY:
        return MPE_MEM_CALLBACK_KILLAPP;
    }
}

/**
 * Convert mpe_MemColor (plus flags) to Callback type.
 * Types returned are TYPE_JAVA and TYPE_SYSTEM.
 */
static jint color2Type(mpe_MemColor color)
{
    if ((color & MPE_MEM_COLOR_MASK) == MPE_MEM_JVM)
        return org_cablelabs_impl_manager_reclaim_Callback_TYPE_JAVA;
    else
        return org_cablelabs_impl_manager_reclaim_Callback_TYPE_SYSTEM;
}

/**
 * Convert mpe_MemColor (plus flags) to Callback reason.
 * Reaons returned are REASON_THRESHOLD or REASON_FAILURE.
 */
static jint color2Reason(mpe_MemColor color)
{
    if ((color & MPE_MEM_RECLAIM_MASK) == MPE_MEM_RECLAIM_THRESHOLD)
        return org_cablelabs_impl_manager_reclaim_Callback_REASON_THRESHOLD;
    else
        return org_cablelabs_impl_manager_reclaim_Callback_REASON_FAILURE;
}

/**
 * Invokes Callback.releaseResources().
 */
static int32_t releaseResources(mpe_MemColor color, int32_t size,
        int64_t contextId, void* data)
{
    JNIEnv* env;
    jobject callback;
    jboolean detach = JNI_FALSE;
    int32_t rc = 0;
    JNI_UNUSED(size);

    /* Return immediately for incompatible reclamation requests. */
    switch (color)
    {
    /*
     * Avoid handling MPE_MEM_JVM allocations.
     * Re-entering the JVM to enable a JVM allocation is not recommended.
     */
    case MPE_MEM_JVM:
        /*
         * Avoid handling of MPE_MEM_THREAD allocations.
         * At least with the Esmertec VM, thread creation occurs while holding a lock.
         * Re-entering the JVM is not recommended.
         */
    case MPE_MEM_THREAD:
        MPE_LOG(
                MPE_LOG_TRACE1,
                MPE_MOD_JNI,
                "IGNORING RRMgrImpl::releaseResources(%08x, %d, %016llx, %p)\n",
                color, size, contextId, data);
        return 0;

        /*
         * All others should be valid.
         */
    default:
        /* FALLTHROUGH */
        ;
    }

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_JNI,
            "RRMgrImpl::releaseResources(%08x, %d, %016llx, %p)\n", color,
            size, contextId, data);

    if (data == NULL)
        return 0;
    callback = (jobject) data;

    // Get JNIEnv*
    env = JNI_GET_ENV(gJvm, &detach);
    if (env != NULL)
    {
        // Invoke releaseResources
        jboolean ok = (*env)->CallBooleanMethod(env, callback,
                jniutil_CachedIds.Callback_releaseResources, color2Type(color),
                color2Reason(color), contextId);
        // Indicate that released "unknown" amount of memory
        if (ok)
            rc = -1;

        // Clear any exceptions
        if ((*env)->ExceptionCheck(env))
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI,
                    "RRMgrImpl::releaseResources: exception occurred during callback!\n");
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }

        // Detach the JNIEnv, if necessary
        JNI_DETACH_ENV(gJvm, detach);
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "RRMgrImpl::releaseResources: Cannot get JNIEnv to callback()\n");
    }

    // Return indicating that should not be called again right away
    return rc;
}

/**
 * Look and initialize Class/method ids.
 * If there is a failure, then an exception is thrown and the function
 * returns prematurely.
 */
static void initIds(JNIEnv *env)
{
    jclass cls = NULL;

    // Lookup method ids and squirrel them away
    GET_CLASS(Callback, "org/cablelabs/impl/manager/reclaim/Callback");
    GET_METHOD_ID(Callback_releaseResources, "releaseResources", "(IIJ)Z");
}

/**
 * Adds the given <code>Callback</code> object of the specified type.
 *
 * @param type {@link #CALLBACK_JAVA}, {@link #CALLBACK_MONAPP}, or {@link #CALLBACK_DESTROY}
 * @param callback <code>Callback</code> object
 * @return token that can be passed on {@link #nRemoveCallback(int, int)}
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_reclaim_RRMgrImpl_nAddCallback(
        JNIEnv *env, jclass cls, jint type, jobject callback)
{
    JNI_UNUSED(cls);

    initIds(env);
    if ((*env)->ExceptionCheck(env))
        return (jint) NULL; // Exception in ID lookup

    // Remember the JavaVM for later}
    if (gJvm == NULL && (*env)->GetJavaVM(env, &gJvm))
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "RRMgrImpl::nAddCallback: could not get JavaVM!\n");
        return (jint) NULL;
    }

    // Cache pointer to Callback
    callback = (*env)->NewGlobalRef(env, callback);
    if (callback == NULL)
        return (jint) NULL; // Exception

    // Register callback
    if (MPE_SUCCESS != mpe_memRegisterMemFreeCallback(type2Color(type),
            releaseResources, (void*) callback))
    {
        (*env)->DeleteGlobalRef(env, callback);

        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "RRMgrImpl::nAddCallback: could not register Callback\n");
        return (jint) NULL;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "RRMgrImpl::nAddCallback: callback registered\n");

#if 0 /* DEBUG, show proper linking */
    if ( type == org_cablelabs_impl_manager_reclaim_RRMgrImpl_CALLBACK_JAVA )
    {
        // For giggles... invoke callback
        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_JNI,
                "RRMgrImpl::nAddCallback: Test callback() returned %d\n",
                releaseResources(MPE_MEM_JVM, 1024, 0L, callback));
    }
#endif

    // Return global reference as removal token
    return (jint) callback;
}

/**
 * Removes the callback previously added with {@link #nAddCallback(int, Callback)}.
 *
 * @param type type originall specified when added
 * @param token token returned by <code>nAddCallback</code>.
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_reclaim_RRMgrImpl_nRemoveCallback
(JNIEnv *env, jclass cls, jint type, jint token)
{
    jobject callback = (jobject)token;
    JNI_UNUSED(cls);

    // Unregister
    (void)mpe_memUnregisterMemFreeCallback( type2Color(type),
            releaseResources,
            callback );

    // Delete GlobalRef
    (*env)->DeleteGlobalRef(env, callback);

    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_JNI,
            "RRMgrImpl::nRemoveCallback: callback unregistered\n" );
}
