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

#include <org_cablelabs_impl_manager_vbi_NativeVBIFilterApi.h>
#include <mpe_types.h>
#include <mpe_dbg.h>
#include <mpe_ed.h>
#include <mpe_filter.h>
#include <mpe_vbi.h>
#include "jni_util.h"

#include <string.h>

static void edCallback(JNIEnv*, void*, mpe_EdEventInfo*, uint32_t*, void **,
        void **, uint32_t*);
static int throwByError(JNIEnv* env, mpe_Error err, int illArg);
static void* copyJavaArray(JNIEnv* env, jbyteArray jArray, unsigned char* dest,
        size_t len);
static void* copyJavaIntArray(JNIEnv* env, jintArray jArray, int* dest,
        size_t len);

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nStartFiltering
 * Signature: (Lorg/cablelabs/impl/manager/ed/EDListener;II[IIIII[B[B[B[B)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nStartFiltering(
        JNIEnv *env, jclass cls, jobject jEdListener, jint sessionType,
        jint sessionHandle, jintArray jLineNumbers, jint field,
        jint dataFormat, jint unitLength, jint bufferSize,
        jint dataUnitThreshold, jbyteArray jPosMask, jbyteArray jPosFilter,
        jbyteArray jNegMask, jbyteArray jNegFilter)
{
    mpe_Error err;

    jsize posArraySize;
    jsize negArraySize;

    jsize linesAraySize;

    mpe_FilterSpec *filterSpec = NULL;
    mpe_VBISource source =
    { NULL, 0, 1, 1,
    { 0 } };
    mpe_EdHandle ed = NULL;

    mpe_VBIFilterSession vbiFilter;
    int i = 0;

    JNI_UNUSED(cls);

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "VBI::nStartFiltering(field=%d dataFormat=%d unitLength=%d bufferSize=%d dataUnitThreshold=%d)\n",
            (int)field, (int)dataFormat, (int)unitLength, (int)bufferSize,(int)dataUnitThreshold);

    posArraySize = (jPosMask == NULL) ? 0 : (*env)->GetArrayLength(env,
            jPosMask);
    negArraySize = (jNegMask == NULL) ? 0 : (*env)->GetArrayLength(env,
            jNegMask);

    /* Create FilterSpec */
    if (MPE_SUCCESS != (err = mpe_filterCreateFilterSpec(
            (uint8_t) posArraySize, (uint8_t) negArraySize, &filterSpec)))
    {
        // Throw exception
        throwByError(env, err, 0);
        return 0;
    }
    if (MPE_SUCCESS != (err = mpe_filterZeroSpec(filterSpec)))
    {
        // Throw exception
        throwByError(env, err, 0);
        return 0;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "VBI::nStartFiltering (posArraySize=%d) \n", (int)posArraySize);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "VBI::nStartFiltering (negArraySize=%d) \n", (int)negArraySize);

    /* Fill in positive filter */
    if (posArraySize > 0)
    {
        if (!copyJavaArray(env, jPosFilter, filterSpec->pos.vals, posArraySize)
                || !copyJavaArray(env, jPosMask, filterSpec->pos.mask,
                        posArraySize))
        {
            // An exception occurred
            mpe_filterDestroyFilterSpec(filterSpec);
            throwByError(env, MPE_EINVAL, 0);
            return 0;
        }
    }
    /* Fill in negative filter */
    if (negArraySize > 0)
    {
        if (!copyJavaArray(env, jNegFilter, filterSpec->neg.vals, negArraySize)
                || !copyJavaArray(env, jNegMask, filterSpec->neg.mask,
                        negArraySize))
        {
            // An exception occurred
            mpe_filterDestroyFilterSpec(filterSpec);
            throwByError(env, MPE_EINVAL, 0);
            return 0;
        }
    }

    for (i = 0; i < posArraySize; i++)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "VBI::nStartFiltering(posValue=0x%x)\n",
                filterSpec->pos.vals[i]);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "VBI::nStartFiltering(posMask=0x%x)\n", filterSpec->pos.mask[i]);
    }

    for (i = 0; i < negArraySize; i++)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "VBI::nStartFiltering(negValue=0x%x)\n",
                filterSpec->neg.vals[i]);
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "VBI::nStartFiltering(negMask=0x%x)\n", filterSpec->neg.mask[i]);
    }

    /* Create ED handle */
    if (MPE_SUCCESS != (err = mpe_edCreateHandle(jEdListener, MPE_ED_QUEUE_NORMAL,
            edCallback, MPE_ED_TERMINATION_OPEN, 0, &ed)))
    {
        throwByError(env, err, 0);
        mpe_filterDestroyFilterSpec(filterSpec);
        return 0;
    }

    linesAraySize = (*env)->GetArrayLength(env, jLineNumbers);

    // FIX ME: hardcode the session type for now....
    sessionType = MPE_VBI_SOURCE_DECODE_SESSION;

    if (sessionType == MPE_VBI_SOURCE_DECODE_SESSION)
    {
        /* Fill in source information */
        source.vbiLineCount = linesAraySize;
        source.vbiFields = field;
        source.sourceType = (mpe_VBISourceType) sessionType;
        source.parm.mediaSessionId = (mpe_MediaDecodeSession) sessionHandle;
    }
    else
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "failed to start filtering vbi - invalid session type: %d\n",
                (int)sessionType);
        mpe_edDeleteHandle(ed);
        throwByError(env, MPE_EINVAL, 0);
        return 0;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "VBI::nStartFiltering(linesAraySize=%d sessionHandle=0x%x)\n",
            (int)linesAraySize, (int)sessionHandle);
    if (linesAraySize > 0)
    {
        if ((err = mpe_memAllocP(MPE_MEM_VBI, (sizeof(source.vbiLines)
                * linesAraySize), (void **) &source.vbiLines)) != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "failed to allocate memory\n");
            mpe_edDeleteHandle(ed);
            throwByError(env, err, 0);
            return 0;
        }

        if (!copyJavaIntArray(env, jLineNumbers, (int*) source.vbiLines,
                linesAraySize))
        {
            // An exception occurred
            // Clean-up
            mpe_edDeleteHandle(ed);
            mpe_memFreeP(MPE_MEM_VBI, source.vbiLines);
            throwByError(env, MPE_EINVAL, 0);
            return 0;
        }
    }

    /* Set filter */
    if (MPE_SUCCESS != (err = mpe_vbiFilterStart(&source, filterSpec,
            ed->eventQ, ed, dataFormat, unitLength, bufferSize,
            dataUnitThreshold, 0, // flags,
            &vbiFilter)))
    {
        throwByError(env, err, 0);
        if (source.vbiLines != NULL)
        {
            mpe_memFreeP(MPE_MEM_VBI, source.vbiLines);
        }
        mpe_edDeleteHandle(ed);
        mpe_filterDestroyFilterSpec(filterSpec);
        return 0;
    }

    // Delete filter spec
    mpe_filterDestroyFilterSpec(filterSpec);

    // Return vbi filter
    return (jint) vbiFilter;

}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nStopFilter
 * Signature: (I)V
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nStopFilter(
        JNIEnv *env, jclass cls, jint vbiFilterSession)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    return mpe_vbiFilterStop((mpe_VBIFilterSession) vbiFilterSession);
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nReleaseFilter
 * Signature: (I)V
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nReleaseFilter(
        JNIEnv *env, jclass cls, jint vbiFilterSession)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    return mpe_vbiFilterRelease((mpe_VBIFilterSession) vbiFilterSession);
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nVBIFilterSetParam
 * Signature: (I)V
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nVBIFilterSetParam(
        JNIEnv *env, jclass cls, jint vbiFilterSession, jint param, jint value)
{
    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    return mpe_vbiFilterSetParam((mpe_VBIFilterSession) vbiFilterSession,
            param, value);
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nVBIFilterGetParam
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nVBIFilterGetParam(
        JNIEnv *env, jclass cls, jint vbiFilterSession, jint param)
{
    uint32_t value = 0;

    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    (void) mpe_vbiFilterGetParam((mpe_VBIFilterSession) vbiFilterSession,
            param, &value);

    return value;
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nVBIGet
 * Signature: ()I
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nVBIGet(
        JNIEnv *env, jclass cls, jint param1, jintArray param2, jint param3)
{
    uint32_t value = 0;

    JNI_UNUSED(env);
    JNI_UNUSED(cls);

    (void) mpe_vbiGetParam((mpe_VBIParameter) param1, (int) param2, param3,
            &value);

    return (jboolean) value;
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nGetVBIData
 * Signature: (II)V
 */
JNIEXPORT jbyteArray JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nGetVBIData(
        JNIEnv *env, jclass cls, jint vbiFilterSession, jint byteCount)
{
    jint bytesRead = 0;
    JNI_UNUSED(env);
    JNI_UNUSED(cls);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "VBI::nGetVBIData Enter...\n");

    /* Figure section size. */
    if (vbiFilterSession != 0 && byteCount != 0)
    {
        jbyteArray jbarray = (*env)->NewByteArray(env, byteCount);
        jbyte* array;

        if (jbarray == NULL || (array = (*env)->GetByteArrayElements(env,
                jbarray, NULL)) == NULL)
        {
            return NULL;
        }

        /* get data */
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "VBI::nGetVBIData reading %d bytes from  session: %08x \n",
                (int)byteCount, (int)vbiFilterSession);

        (void) mpe_vbiFilterReadData((mpe_VBIFilterSession) vbiFilterSession,
                0, byteCount, MPE_VBI_OPTION_CLEAR_READ_DATA, (uint8_t*) array,
                (uint32_t*) &bytesRead);

        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "VBI::nGetVBIData actual number of bytes read %d\n", (int)bytesRead);

        (*env)->ReleaseByteArrayElements(env, jbarray, array, 0);

        return jbarray;
    }

    return NULL;
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nClearData
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nClearData(
        JNIEnv *env, jclass cls, jint vbiFilterSession)
{
    uint8_t scratch[4096];
    uint32_t bytesRead = 0;
    JNI_UNUSED(env);
    JNI_UNUSED(cls);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "VBI::nGetVBIData read %u bytes from  session: %08x \n", bytesRead,
            (int)vbiFilterSession);

    if (0 != vbiFilterSession)
    {
        // Special read call that clears the buffer
        (void) mpe_vbiFilterReadData((mpe_VBIFilterSession) vbiFilterSession,
                0,
                1, // This can't be 0, pass some non-zero value for now
                MPE_VBI_OPTION_CLEAR_BUFFER, (uint8_t*) scratch,
                (uint32_t*) &bytesRead);

    }
    return 0;
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nRegisterAvailability
 * Signature: (Lorg/cablelabs/impl/manager/ed/EDListener;)I
 *
 * Registers the given <code>EDListener</code> to be notified of resource availability
 * events.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nRegisterAvailability(
        JNIEnv *env, jclass cls, jobject jEdListener)
{
    mpe_EdEventInfo *edHandle;

    JNI_UNUSED(cls);

    /* Create ED handle */
    if (MPE_SUCCESS != mpe_edCreateHandle(jEdListener, MPE_ED_QUEUE_NORMAL,
            NULL, MPE_ED_TERMINATION_OPEN, 0, &edHandle))
    {
        return 0;
    }

    if (MPE_SUCCESS != mpe_vbiRegisterAvailability(edHandle->eventQ, edHandle))
    {
        mpe_edDeleteHandle(edHandle);
        return 0;
    }

    return (jint) edHandle;
}

/*
 * Class:     org_cablelabs_impl_manager_vbi_NativeVBIFilterApi
 * Method:    nInit
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_vbi_NativeVBIFilterApi_nInit
(JNIEnv *env, jclass cls)
{
}

/**
 * Deletes the given ED handle when a section filtering termination code is received.
 */
static void edCallback(JNIEnv* env, void* jListenerObject,
        mpe_EdEventInfo* edHandle, uint32_t *event, void **data1, void **data2,
        uint32_t *data3)
{
    JNI_UNUSED(env);
    JNI_UNUSED(jListenerObject);
    JNI_UNUSED(data1);
    JNI_UNUSED(data2);
    JNI_UNUSED(data3);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "\n<VBI:edCallback> handle:0x%p event:%u\n", edHandle, *event);

    switch (*event)
    {
    case MPE_VBI_EVENT_FIRST_DATAUNIT:
    case MPE_VBI_EVENT_DATAUNITS_RECEIVED:
        break;
    default:
        //MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "<VBI:edCallback> deleting handle %08x for %08x\n",
        //       edHandle, *event);
        //mpe_edDeleteHandle(edHandle);
        break;
    }
}

/**
 * Copy the given source byte[] into the given destination char* buffer.
 * @param env
 * @param jArray Java byte array object (must be at least len bytes)
 * @param dest destination buffer (must be at least len bytes)
 * @param len number of bytes to copy
 * @return dest if successful; NULL if unsuccessful
 */
static void* copyJavaArray(JNIEnv* env, jbyteArray jArray, unsigned char* dest,
        size_t len)
{
    jbyte* cArray = (*env)->GetByteArrayElements(env, jArray, NULL);
    if (cArray != NULL)
    {
        memcpy(dest, cArray, len);
        (*env)->ReleaseByteArrayElements(env, jArray, cArray, 0);
        return dest;
    }
    return NULL;
}

/**
 * Copy the given source int[] into the given destination buffer.
 * @param env
 * @param jArray Java int array object (must be at least len number of ints)
 * @param dest destination buffer (must be at least len bytes)
 * @param len number of ints to copy
 * @return dest if successful; NULL if unsuccessful
 */
static void* copyJavaIntArray(JNIEnv* env, jintArray jArray, int* dest,
        size_t len)
{
    jint* iArray = (*env)->GetIntArrayElements(env, jArray, NULL);
    if (iArray != NULL)
    {
        memcpy(dest, iArray, len * sizeof(int));
        (*env)->ReleaseIntArrayElements(env, jArray, iArray, 0);
        return dest;
    }
    return NULL;
}

/**
 * Currently this method throws no exceptions. But it should throw
 * appropriate java exceptions if errors are encountered.
 *
 * Pending ECR revision this method is a place holder for future
 * modifications.
 *
 * Returns 0, if MPE_SUCCESS is specified.
 *
 * @param err the MPE error
 *
 * @return 1 if an error was returned
 */
static int throwByError(JNIEnv *env, mpe_Error err, int illArg)
{
    switch (err)
    {
    case MPE_SUCCESS:
        return 0;
    case MPE_EINVAL:
        jniutil_throwByName(env, "java/lang/IllegalArgumentException", "");
        break;
    case MPE_ENOMEM:
        jniutil_throwByName(env, "java/lang/UnsupportedOperationException",
                "Unsupported filter type");
        break;
    case MPE_VBI_ERROR_FILTER_NOT_AVAILABLE:
        break;
    case MPE_VBI_ERROR_SOURCE_CLOSED:
        break;
    case MPE_VBI_ERROR_SOURCE_SCRAMBLED:
        break;
    case MPE_VBI_ERROR_UNSUPPORTED:
        jniutil_throwByName(env, "java/lang/UnsupportedOperationException",
                "Unsupported filter type");
        break;
    default:
        break;
    }
    return 1;
}
