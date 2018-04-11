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

#include <org_cablelabs_debug_Memory.h>
#include <mpe_types.h>
#include <mpe_os.h>

/*
 * Class:     org_cablelabs_debug_Memory
 * Method:    dumpStats
 * Signature: (ZILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_debug_Memory_dumpStats(JNIEnv *env, jclass clazz
        , jboolean toConsole
        , jint color
        , jstring label)
{
    const char *str = NULL;

    MPE_UNUSED_PARAM(clazz);

    /* Acquire the label string. */
    if (NULL != label)
    {
        if ((str = (*env)->GetStringUTFChars(env, label, NULL)) == NULL)
        {
            /* GetStringUTFChars threw a memory exception */
            return;
        }
    }

    /* Call MPE to do the dump. */
    mpe_memStats((mpe_Bool)toConsole, (int)color, str);

    /* Release the label string. */
    if (NULL != str)
    {
        (*env)->ReleaseStringUTFChars(env, label, str);
    }
}

/*
 * Class:     org_cablelabs_debug_Memory
 * Method:    getNumColorsImpl
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_org_cablelabs_debug_Memory_getNumColorsImpl(JNIEnv *env,
        jobject obj)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    return MPE_MEM_NCOLORS;
}

/*
 * Class:     org_cablelabs_debug_Memory
 * Method:    getStats
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_org_cablelabs_debug_Memory_getStats(JNIEnv *env,
        jobject obj)
{
    mpe_MemStatsInfo *array;
    mpe_Error err;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    err = mpe_memAllocP(MPE_MEM_GENERAL, sizeof(mpe_MemStatsInfo) * MPE_MEM_NCOLORS, (void **)&array);
    if (err != MPE_SUCCESS)
    {
        return 0;
    }

    err = mpe_memGetStats(sizeof(mpe_MemStatsInfo) * MPE_MEM_NCOLORS, array);
    if (err != MPE_SUCCESS)
    {
        mpe_memFreeP(MPE_MEM_GENERAL, array);
        return 0;
    }

    return(jint)array;
}

/*
 * Class:     org_cablelabs_debug_Memory
 * Method:    disposeImpl
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_org_cablelabs_debug_Memory_disposeImpl(JNIEnv *env,
        jobject obj,
        jint handle)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    if (handle)
    mpe_memFreeP(MPE_MEM_GENERAL, (void *)handle);
}

/*
 * Class:     org_cablelabs_debug_Memory
 * Method:    getIntStat
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL
Java_org_cablelabs_debug_Memory_getIntStat(JNIEnv *env,
        jobject obj,
        jint handle,
        jint color,
        jint stat)
{
    mpe_MemStatsInfo *stats;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(obj);

    // avoid blowing up, don't be very helpful
    if (handle == 0 || color < 0 || color >= MPE_MEM_NCOLORS)
    return 0;

    stats = (mpe_MemStatsInfo *)handle + color;

    switch (stat)
    {
        case org_cablelabs_debug_Memory_MEMSTAT_ALLOCATED:
        return stats->currAllocated;
        case org_cablelabs_debug_Memory_MEMSTAT_MAX_ALLOCATED:
        return stats->maxAllocated;
        default:
        return 0;
    }
}

/*
 * Class:     org_cablelabs_debug_Memory
 * Method:    getStringStat
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_org_cablelabs_debug_Memory_getStringStat(JNIEnv *env,
        jobject obj,
        jint handle,
        jint color,
        jint stat)
{
    mpe_MemStatsInfo *stats;
    MPE_UNUSED_PARAM(obj);

    // avoid blowing up, don't be very helpful
    if (handle == 0 || color < 0 || color >= MPE_MEM_NCOLORS)
    return NULL;

    stats = (mpe_MemStatsInfo *)handle + color;

    switch (stat)
    {
        case org_cablelabs_debug_Memory_MEMSTAT_NAME:
        return(*env)->NewStringUTF(env, stats->name);
        default:
        return NULL;
    }
}
