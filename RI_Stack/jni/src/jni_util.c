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

#include "jni_util.h"
#include "mgrdef.h"
#include <mpe_os.h>
#include <mpe_dbg.h>
#include <inttypes.h>

JNICachedIds_t jniutil_CachedIds =
{ 0 };

/**
 * Sets the fields of the given <code>Dimension</code> object to
 * <i>w</i> and <i>h</i>.
 *
 * @param dimension the <code>Dimension</code> to modify
 * @param w the new width
 * @param h the new height
 */
void jniutil_setDimension(JNIEnv *env, jobject dimension, jint w, jint h)
{
    (*env)->SetIntField(env, dimension, jniutil_CachedIds.Dimension_width, w);
    (*env)->SetIntField(env, dimension, jniutil_CachedIds.Dimension_height, h);
}

/**
 * Sets the fields of the given <code>Rectangle</code> object to
 * <i>x</i>, <i>y</i>, <i>w</i>, and <i>h</i>.
 *
 * @param rectangle the <code>Rectangle</code> to modify
 * @param x the new x
 * @param y the new y
 * @param w the new width
 * @param h the new height
 */
void jniutil_setRectangle(JNIEnv *env, jobject rectangle, jint x, jint y,
        jint w, jint h)
{
    (*env)->SetIntField(env, rectangle, jniutil_CachedIds.Rectangle_x, x);
    (*env)->SetIntField(env, rectangle, jniutil_CachedIds.Rectangle_y, y);
    (*env)->SetIntField(env, rectangle, jniutil_CachedIds.Rectangle_width, w);
    (*env)->SetIntField(env, rectangle, jniutil_CachedIds.Rectangle_height, h);
}

/**
 * Gets the fields of the given <code>Rectangle</code>.
 *
 * @param rectangle the <code>Rectangle</code> to read from
 * @param x the address to write x
 * @param y the address to write y
 * @param w the address to write width
 * @param h the address to write height
 */
void jniutil_getRectangle(JNIEnv *env, jobject rectangle, jint *x, jint *y,
        jint *w, jint *h)
{
    *x = (*env)->GetIntField(env, rectangle, jniutil_CachedIds.Rectangle_x);
    *y = (*env)->GetIntField(env, rectangle, jniutil_CachedIds.Rectangle_y);
    *w = (*env)->GetIntField(env, rectangle, jniutil_CachedIds.Rectangle_width);
    *h
            = (*env)->GetIntField(env, rectangle,
                    jniutil_CachedIds.Rectangle_height);
}

/**
 * Sets the fields of the given <code>Rectangle</code> object from an mpe_MediaRectangle.
 *
 * @param rectangle the <code>Rectangle</code> to copy to
 * @param grOut the mpe_MediaRectangle from which values are copied
 */
void jniutil_setRectangleMedia(JNIEnv *env, jobject rectangle,
        mpe_MediaRectangle *mediaRect)
{
    jniutil_setHScreenRectangle(env, rectangle, mediaRect->x, mediaRect->y,
            mediaRect->width, mediaRect->height);
}

/**
 * Gets the fields of the given <code>Rectangle</code> into an mpe_MediaRectangle.
 *
 * @param rectangle the <code>Rectangle</code> to read from
 * @param grOut pointer to the mpe_MediaRectangle into which result is copied
 */
void jniutil_getRectangleMedia(JNIEnv *env, jobject rectangle,
        mpe_MediaRectangle *mediaRect)
{
    jniutil_getHScreenRectangle(env, rectangle, &mediaRect->x, &mediaRect->y,
            &mediaRect->width, &mediaRect->height);
}

/**
 * Sets the fields of the given <code>HScreenRectangle</code> object to
 * <i>x</i>, <i>y</i>, <i>w</i>, and <i>h</i>.
 *
 * @param hrect the <code>HScreenRectangle</code> to modify
 * @param x the new x
 * @param y the new y
 * @param w the new width
 * @param h the new height
 */
void jniutil_setHScreenRectangle(JNIEnv *env, jobject hrect, jfloat x,
        jfloat y, jfloat w, jfloat h)
{
    (*env)->SetFloatField(env, hrect, jniutil_CachedIds.HScreenRectangle_x, x);
    (*env)->SetFloatField(env, hrect, jniutil_CachedIds.HScreenRectangle_y, y);
    (*env)->SetFloatField(env, hrect, jniutil_CachedIds.HScreenRectangle_width,
            w);
    (*env)->SetFloatField(env, hrect,
            jniutil_CachedIds.HScreenRectangle_height, h);
}

/**
 * Gets the fields of the given <code>HScreenRectangle</code>
 *
 * @param hrect the <code>HScreenRectangle</code> to read from
 * @param x the address to write x
 * @param y the address to write y
 * @param w the address to write width
 * @param h the address to write height
 */
void jniutil_getHScreenRectangle(JNIEnv *env, jobject hrect, jfloat *x,
        jfloat *y, jfloat *w, jfloat *h)
{
    *x
            = (*env)->GetFloatField(env, hrect,
                    jniutil_CachedIds.HScreenRectangle_x);
    *y
            = (*env)->GetFloatField(env, hrect,
                    jniutil_CachedIds.HScreenRectangle_y);
    *w = (*env)->GetFloatField(env, hrect,
            jniutil_CachedIds.HScreenRectangle_width);
    *h = (*env)->GetFloatField(env, hrect,
            jniutil_CachedIds.HScreenRectangle_height);
}

/**
 * Throws an exception, specifying the given message.
 *
 * @param name exception class name
 * @param msg message to pass to constructor
 */
void jniutil_throwByName(JNIEnv *env, const char *name, const char *msg)
{
    jclass cls = (*env)->FindClass(env, name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL)
    {
        (*env)->ThrowNew(env, cls, msg);
    }
    /* free the local ref */
    (*env)->DeleteLocalRef(env, cls);
}

/**
 * Create mpe_MediaPID array from array of PIDs and array of elementary stream types.
 *
 * @param env the JNI environment handle
 * @param pidArray the array of PID values
 * @param typeArray the array of elementary stream types
 * @param pidCount number of PIDs to copy
 * @param returned mpe_MediaPID array, which must be freed by caller
 */
void jniutil_createPidArray(JNIEnv *env, jintArray pidArray,
        jshortArray typeArray, int pidCount, mpe_MediaPID* pids)
{
    jint *pidValues;
    jshort *typeValues;
    int i;

    /* get the Java array values */
    pidValues = (*env)->GetIntArrayElements(env, pidArray, 0);
    typeValues = (*env)->GetShortArrayElements(env, typeArray, 0);

    if (pids)
    {
        // copy the pid values and stream types into the array of mpe_MediaPID structures
        for (i = 0; i < pidCount; i++)
        {
            pids[i].pid = pidValues[i];
            pids[i].pidType = typeValues[i];
        }
    }

    /* release the Java arrays */
    (*env)->ReleaseIntArrayElements(env, pidArray, pidValues, 0);
    (*env)->ReleaseShortArrayElements(env, typeArray, typeValues, 0);
}

/**
 * Create mpe_HnPidInfo array from array of PIDs and array of elementary stream types.
 *
 * @param env the JNI environment handle
 * @param pidArray the array of PID values
 * @param elemTypeArray the array of elementary stream types
 * @param mediaTypeArray the array of media stream types
 * @param pidCount number of PIDs to copy
 * @param returned mpe_HnPidInfo array, which must be freed by caller
 */
void jniutil_createHnPidArray(JNIEnv *env, jintArray pidArray,
        jshortArray elemTypeArray, jshortArray mediaTypeArray, int pidCount, mpe_HnPidInfo* pids)
{
    jint *pidValues;
    jshort *elemTypeValues;
    jshort *mediaTypeValues;
    int i;

    /* get the Java array values */
    pidValues = (*env)->GetIntArrayElements(env, pidArray, 0);
    elemTypeValues = (*env)->GetShortArrayElements(env, elemTypeArray, 0);
    mediaTypeValues = (*env)->GetShortArrayElements(env, mediaTypeArray, 0);

    if (pids)
    {
        // copy the pid values and stream types into the array of mpe_MediaPID structures
        for (i = 0; i < pidCount; i++)
        {
            pids[i].streamType = mediaTypeValues[i];
            pids[i].pid = pidValues[i];
            pids[i].eltStreamType = elemTypeValues[i];
        }
    }

    /* release the Java arrays */
    (*env)->ReleaseIntArrayElements(env, pidArray, pidValues, 0);
    (*env)->ReleaseShortArrayElements(env, elemTypeArray, elemTypeValues, 0);
    (*env)->ReleaseShortArrayElements(env, mediaTypeArray, mediaTypeValues, 0);
}

/**
 * Create mpe_DvrPidInfo array from array of PIDs and array of elementary stream types.
 *
 * @param env the JNI environment handle
 * @param pidArray the array of PID values
 * @param typeArray the array of elementary stream types
 * @param pidCount number of PIDs to copy
 * @param returned mpe_DvrPidInfo array, which must be freed by caller
 */
void jniutil_createDvrPidInfoArray(JNIEnv *env, jintArray pidArray,
        jshortArray typeArray, int pidCount, mpe_DvrPidInfo* pids)
{
    jint *pidValues;
    jshort *typeValues;
    int i;

    /* get the Java array values */
    pidValues = (*env)->GetIntArrayElements(env, pidArray, 0);
    typeValues = (*env)->GetShortArrayElements(env, typeArray, 0);

    if (pids)
    {
        // copy the pid values and stream types into the array of mpe_DvrPidInfo structures
        for (i = 0; i < pidCount; i++)
        {
            pids[i].srcPid = (int16_t) pidValues[i];
            pids[i].srcEltStreamType = typeValues[i];
        }
    }

    /* release the Java arrays */
    (*env)->ReleaseIntArrayElements(env, pidArray, pidValues, 0);
    (*env)->ReleaseShortArrayElements(env, typeArray, typeValues, 0);
}

/**
 * Update the Pid Map table with recorded pids from native dvr
 *
 * @param env the JNI environment handle
 * @param pidCount number of pids in the pidMapTable
 * @param dvrPidInfo native dvr pid information (contains the recorded pids)
 * @param pidMapTable the Java pid map table to update
 */
void jniutil_updatePidMapTable(JNIEnv *env, jint pidCount,
        mpe_DvrPidInfo dvrPidInfo[], jobjectArray pidMapEntryArray)
{

    jint p;
    jobject pidMapEntry;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "jniutil_updatePidMapTable - ENTER -\n");

    for (p = 0; p < pidCount; p++)
    {
        // get pidMapEntryArray[p] (a PidMapEntry object)
        pidMapEntry = (*env)->GetObjectArrayElement(env, pidMapEntryArray, p);
        if (pidMapEntry != NULL)
        {
            // update the PidmapEntry with recorded pids
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "- dvrPidInfo[%d].recPid= 0x%X\n", (int)p, (int)(dvrPidInfo[p].recPid));
            (*env)->SetIntField(env, pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_recPID, dvrPidInfo[p].recPid);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "- dvrPidInfo[%d].recEltStreamType= 0x%X\n\n", (int)p,
                    (int)(dvrPidInfo[p].recEltStreamType));
            (*env)->SetShortField(env, pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_recElementaryStreamType,
                    (jshort) dvrPidInfo[p].recEltStreamType);
        }
    }
}

/**
 * Create a pid table from TimeTable object
 *
 * @param env the JNI environment handle
 * @param elementArray array  of TimeTable elements
 * @param pidTableCount number of elements in the array
 * @param pidTable the dvr pid tables to create
 */
void jniutil_createDvrPidTable(JNIEnv *env, jobjectArray elementArray,
        int pidTableCount, mpe_DvrPidTable *dvrPidTable)
{
    int t;
    jobject element;
    jobject value;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI, "jniutil_createDvrPidTable - ENTER -\n");

    for (t = 0; t < pidTableCount; t++)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "jniutil_createDvrPidTable - processing PidTable %d\n", t);

        // get elememtArray[t];
        element = (*env)->GetObjectArrayElement(env, elementArray, t);
        // get time attribute
        dvrPidTable[t].mediaTime = (*env)->GetLongField(env, element,
                jniutil_CachedIds.TimeElement_time);

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "- dvrPidTable[%d].mediaTime= %"PRIu64"\n",t,dvrPidTable[t].mediaTime);

        // get the value attribute (a PidMaptable object)
        value = (*env)->GetObjectField(env, element,
                jniutil_CachedIds.TimeElement_value);
        dvrPidTable[t].count = (*env)->GetIntField(env, value,
                jniutil_CachedIds.PidMapTable_pidTableSize);

        jniutil_convertToDvrPidInfoArray(env, value, dvrPidTable[t].pids);
    }
}

/**
 * Create a pid info array from a PidMapTable object
 *
 * @param env the JNI environment handle
 * @param pidMapTable a pidMapTable
 * @param pidTableCount number of elements in the array
 * @param pidInfo array
 */
void jniutil_convertToDvrPidInfoArray(JNIEnv *env, jobject pidMapTable,
        mpe_DvrPidInfo *dvrPidInfo)
{
    uint32_t pidEntryCount;
    uint32_t p;
    jobject pidMapEntry;
    jobjectArray pidMapEntryArray;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI,
            "jniutil_convertToDvrPidInfoArray - ENTER -\n");

    // get the array of PidMapEntries
    pidMapEntryArray = (*env)->GetObjectField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidMapEntryArray);
    pidEntryCount = (*env)->GetIntField(env, pidMapTable,
            jniutil_CachedIds.PidMapTable_pidTableSize);
    for (p = 0; p < pidEntryCount; p++)
    {
        // get pidMapEntryArray[p] (a PidMapEntry object)
        pidMapEntry = (*env)->GetObjectArrayElement(env, pidMapEntryArray, p);
        if (pidMapEntry != NULL)
        {
            // get the PidmapEntry fields
            dvrPidInfo[p].srcPid = (int16_t)(*env)->GetIntField(env,
                    pidMapEntry, jniutil_CachedIds.PidMapEntry_srcPID);
            dvrPidInfo[p].srcEltStreamType = (*env)->GetShortField(env,
                    pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_srcElementaryStreamType);
            dvrPidInfo[p].recPid = (int16_t)(*env)->GetIntField(env,
                    pidMapEntry, jniutil_CachedIds.PidMapEntry_recPID);
            dvrPidInfo[p].recEltStreamType = (*env)->GetShortField(env,
                    pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_recElementaryStreamType);
            dvrPidInfo[p].streamType = (*env)->GetShortField(env, pidMapEntry,
                    jniutil_CachedIds.PidMapEntry_streamType);

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "- dvrPidInfo.pids[%d].srcPid= 0x%X\n", p,
                    dvrPidInfo[p].srcPid);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "- dvrPidInfo.pids[%d].recPid= 0x%X\n", p,
                    dvrPidInfo[p].recPid);
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                    "- dvrPidInfo.pids[%d].streamType= %d\n", p,
                    dvrPidInfo[p].streamType);
        }
    }
}

/**
 * Acquire a JNIEnv* for the current  thread.
 *
 * @param jvm the current JavaVM*
 * @param detach pointer to jboolean where JNI_TRUE is stored if detach should be performed
 * @return JNIEnv* or NULL
 */
JNIEnv* jniutil_getJNIEnv(JavaVM *jvm, jboolean *detachp)
{
    JNIEnv* env = NULL;

    if (jvm == NULL || detachp == NULL)
        return NULL;

    // Try to get the environment
    if ((*jvm)->GetEnv(jvm, (void**) &env, JNI_VERSION_1_2) != JNI_OK)
        env = NULL;

    // If no environment was returned, this is an MPE thread and must be detached when
    // we're done.
    if (env != NULL)
        *detachp = JNI_FALSE;
    else
    {
        // Ok, attach it.
        jboolean detach = JNI_ISTRUE(JNI_OK == (*jvm)->AttachCurrentThread(jvm,
                (void**) &env, NULL) && env != NULL);

        if (!detach)
            MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI, "Could not attach thread!");

        *detachp = detach;
    }
    return env;
}
