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

#include "org_cablelabs_debug_Profile.h"
#include <mpe_dbg.h>
#include <mpe_prof.h>
#include <mpe_sys.h>
#include <mgrdef.h>

// This module enables profiling from Java (which can be mixed with profiling
// from C).  You must start/stop timing around the processes you want timed and then
// do setWhere and popWhere around each area in the code you want profiled. The urlIndex
// is returned by the startTiming. It is a good idea to have setWhere(s) and popWhere(s)
// in the same function, with a setWhere towards the start of the function in a place
// that can't be avoided and a popWhere before every return in the function.

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    startTiming
 * Signature:
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_Profile_startTiming(JNIEnv *env, jclass cls, jstring string)
{
    // get local reference to debug string
#ifdef MPE_FEATURE_PROF
    const char *url;
    uint32_t urlIndex;
    MPE_UNUSED_PARAM(cls);

    if ((url = (*env)->GetStringUTFChars(env,string,NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return;
    }

    // call the profiling start
    mpe_profStartTiming(url, TRUE, &urlIndex);

    // Release string resource.
    (*env)->ReleaseStringUTFChars(env, string, url);
#else
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);
#endif
}

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    stopTiming
 * Signature:
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_Profile_stopTiming
(JNIEnv *env, jclass cls)
{
    // get local reference to debug string
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

#ifdef MPE_FEATURE_PROF
    mpe_profStopTiming(0, TRUE);
#endif
}

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    setWhere
 * Signature:
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_Profile_setWhere(JNIEnv *env, jclass cls, jint index)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

#ifdef MPE_FEATURE_PROF
    mpe_profSetWhere(0, index);
#endif
}

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    popWhere
 * Signature:
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_Profile_popWhere(JNIEnv *env, jclass cls)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

#ifdef MPE_FEATURE_PROF
    mpe_profPopWhereStack(0);
#endif
}

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    dumpProfile
 * Signature:
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_Profile_dumpProfile(JNIEnv *env, jclass cls, jint numPrints)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

#ifdef MPE_FEATURE_PROF
    // call the profiling dump to serial or UDP
    mpe_profDisplayTiming(numPrints);
#endif
}

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    addLabel
 * Signature:
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_debug_Profile_addLabel(JNIEnv *env,
        jclass cls, jstring string)
{
#ifdef MPE_FEATURE_PROF
    const char *labelStr;
    uint32_t labelIndex;
    MPE_UNUSED_PARAM(cls);

    if ((labelStr = (*env)->GetStringUTFChars(env, string, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return(jint)0;
    }

    mpe_profAddLabel(labelStr, &labelIndex);
    (*env)->ReleaseStringUTFChars(env, string, labelStr);
    return(jint)labelIndex;
#else
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);
    return (jint) 0;
#endif
}

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    addComment
 * Signature:
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_Profile_addComment(JNIEnv *env, jclass cls, jstring string)
{
#ifdef MPE_FEATURE_PROF
    const char *labelStr;
    MPE_UNUSED_PARAM(cls);

    if ((labelStr = (*env)->GetStringUTFChars(env, string, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return;
    }

    mpe_profAddComment(labelStr);
    (*env)->ReleaseStringUTFChars(env, string, labelStr);
#else
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);
#endif
}

/*
 * Class:     org_cablelabs_debug_Profile
 * Method:    getIndex
 * Signature:
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_debug_Profile_getIndex(JNIEnv *env,
        jclass cls, jstring string)
{
#ifdef MPE_FEATURE_PROF
    const char *labelStr;
    uint32_t labelIndex;
    MPE_UNUSED_PARAM(cls);

    if ((labelStr = (*env)->GetStringUTFChars(env, string, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return(jint)0;
    }

    mpe_profGetIndex(labelStr, &labelIndex);
    (*env)->ReleaseStringUTFChars(env, string, labelStr);
    return(jint)labelIndex;
#else
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);
    return (jint) 0;
#endif
}

JNIEXPORT jboolean JNICALL Java_org_cablelabs_debug_Profile_init(JNIEnv *env,
        jclass cls)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(cls);

#ifdef MPE_FEATURE_PROF
    return true;
#else
    return false;
#endif
}
