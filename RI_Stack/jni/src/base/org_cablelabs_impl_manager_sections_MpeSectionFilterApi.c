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

#include <org_cablelabs_impl_manager_sections_MpeSectionFilterApi.h>
#include <org_cablelabs_impl_manager_SectionFilterManager_FilterSpec.h>
#include <org_davic_mpeg_NotAuthorizedInterface.h>
#include <mpe_types.h>
#include <mpe_dbg.h>
#include <mpe_ed.h>
#include <mpe_filter.h>
#include <mpe_filterevents.h>
#include "jni_util.h"

#include <string.h>

#define DBG_ALL 1
#define DBG_ENTRY (DBG_ALL| 0)
#define DBG_INFO  (DBG_ALL| 0)
#define DBG_ERROR (DBG_ALL| 1)
#define DBGMSG(cond, params)  // do { if (cond) mpe_dbgMsg params ; } while(0)
static void edCleanup(JNIEnv*, void*, mpe_EdEventInfo*, uint32_t*, void **,
        void **, uint32_t*);
static int throwByError(JNIEnv* env, mpe_Error err, int illArg);
static void* copyJavaArray(JNIEnv* env, jbyteArray jArray, unsigned char* dest,
        size_t len);
static void throwNotAuthorized(JNIEnv *env, jint major, jint minor);

/**
 * Invokes <code>mpe_filterSetFilter()</code> to start filtering.
 *
 * @param edListener listener to be notified
 * @param tunerID specifies the tuner as {@link FilterSpec#tunerId}
 * @param frequency specifies frequency as {@link FilterSpec#frequency}
 * @param tsid specifies transport stream ID as {@link FilterSpec#transportStreamId}
 * @param isInBand specifies if in-band as {@link FilterSpec#isInBand}
 * @param pid specifies elementary stream as {@link FilterSpec#pid}
 * @param posMask specifies filtering mask as {@link FilterSpec#posMask}
 * @param posFilter specifies filter as {@link FilterSpec#posFilter}
 * @param negMask specifies filtering mask as {@link FilterSpec#negMask}
 * @param negFilter specifies filter as {@link FilterSpec#negFilter}
 * @param timesToMatch specifies number of sections as {@link FilterSpec#timesToMatch}
 * @param priority specifies filtering priority as {@link FilterSpec#priority}
 *
 * @return the unique native handle for the started filter
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nStartFiltering(
        JNIEnv *env, jclass cls, jobject jEdListener, jint tunerID, jshort ltsid,
        jint frequency, jint tsid, jboolean isInBand, jint pid,
        jbyteArray jPosMask, jbyteArray jPosFilter, jbyteArray jNegMask,
        jbyteArray jNegFilter, jint timesToMatch, jint priority)
{
    mpe_Error err;

    jsize posArraySize;
    jsize negArraySize;

    mpe_FilterSpec *filterSpec = NULL;
    mpe_FilterSource source;
    uint8_t mpePriority;

    mpe_EdHandle ed = NULL;

    uint32_t sectionFilter = 0;

    JNI_UNUSED(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "SF::nStartFiltering ( tuner=%d ltsid=%d freq=%d tsid=%d ib=%d pid=%d )\n",
            (int)tunerID, (int)ltsid, (int)frequency, (int)tsid, isInBand, (int)pid);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "                    ( times=%d priority=%d )\n", (int)timesToMatch,
            (int)priority);

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

    /* Fill in positive filter */
    if (posArraySize > 0)
    {
        if (!copyJavaArray(env, jPosFilter, filterSpec->pos.vals, posArraySize)
                || !copyJavaArray(env, jPosMask, filterSpec->pos.mask,
                        posArraySize))
        {
            // An exception occurred
            mpe_filterDestroyFilterSpec(filterSpec);
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
            return 0;
        }
    }

    /* Create ED handle */
    if (MPE_SUCCESS != mpe_edCreateHandle(jEdListener, MPE_ED_QUEUE_NORMAL,
            edCleanup, MPE_ED_TERMINATION_OPEN, 0, &ed))
    {
        throwByError(env, err, 0);
        mpe_filterDestroyFilterSpec(filterSpec);
        return 0;
    }

    /* Fill in source information */
    source.pid = pid;

    if (isInBand)
    {
        source.sourceType = MPE_FILTER_SOURCE_INB;
        source.parm.p_INB.tunerId = tunerID;
        source.parm.p_INB.ltsid = ltsid;
        source.parm.p_INB.freq = frequency;
        source.parm.p_INB.tsId = tsid;
    }
    else
    {
        source.sourceType = MPE_FILTER_SOURCE_OOB;
        source.parm.p_OOB.tsId = tsid;
    }

    /* Convert Java priority to MPE priority */
    switch (priority)
    {
    case org_cablelabs_impl_manager_SectionFilterManager_FilterSpec_FILTER_PRIORITY_EAS:
        mpePriority = MPE_SF_FILTER_PRIORITY_EAS;
        break;
    case org_cablelabs_impl_manager_SectionFilterManager_FilterSpec_FILTER_PRIORITY_SITP:
        mpePriority = MPE_SF_FILTER_PRIORITY_SITP;
        break;
    case org_cablelabs_impl_manager_SectionFilterManager_FilterSpec_FILTER_PRIORITY_XAIT:
        mpePriority = MPE_SF_FILTER_PRIORITY_XAIT;
        break;
    case org_cablelabs_impl_manager_SectionFilterManager_FilterSpec_FILTER_PRIORITY_AIT:
        mpePriority = MPE_SF_FILTER_PRIORITY_AIT;
        break;
    case org_cablelabs_impl_manager_SectionFilterManager_FilterSpec_FILTER_PRIORITY_OC:
        mpePriority = MPE_SF_FILTER_PRIORITY_OC;
        break;
    case org_cablelabs_impl_manager_SectionFilterManager_FilterSpec_FILTER_PRIORITY_DAVIC:
        mpePriority = MPE_SF_FILTER_PRIORITY_DAVIC;
        break;
    default:
        throwByError(env, MPE_EINVAL, 0);
        mpe_edDeleteHandle(ed);
        mpe_filterDestroyFilterSpec(filterSpec);
        return 0;
    }

    /* Set filter */
    if (MPE_SUCCESS != (err = mpe_filterSetFilter(&source, filterSpec,
            ed->eventQ, ed, mpePriority, timesToMatch, 0, // flags,
            &sectionFilter)))
    {
        throwByError(env, err, 0);
        mpe_edDeleteHandle(ed);
        mpe_filterDestroyFilterSpec(filterSpec);
        return 0;
    }

    // Delete filter spec
    mpe_filterDestroyFilterSpec(filterSpec);

    // Return section filter
    return sectionFilter;
}

/**
 * Deletes the given ED handle when a section filtering termination code is received.
 */
static void edCleanup(JNIEnv* env, void* jListenerObject,
        mpe_EdEventInfo* edHandle, uint32_t *event, void **data1, void **data2,
        uint32_t *data3)
{
    JNI_UNUSED(env);
    JNI_UNUSED(jListenerObject);
    JNI_UNUSED(data1);
    JNI_UNUSED(data2);
    JNI_UNUSED(data3);
    switch (*event)
    {
    case MPE_SF_EVENT_SECTION_FOUND:
        break;
    default:
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "<SF:edCleanup> deleting handle %p for %u\n", edHandle,
                *event);
        mpe_edDeleteHandle(edHandle);
        break;
    }
}

/**
 * Equivalent to:
 * <pre>
 * throw new org.cablelabs.impl.davic.mpeg.NotAuthorizedException(major,minor);
 * </pre>
 */
static void throwNotAuthorized(JNIEnv *env, jint major, jint minor)
{
    // Create a new exception
    jobject ex = (*env)->NewObject(env,
            jniutil_CachedIds.NotAuthorizedException,
            jniutil_CachedIds.NotAuthorizedException_init, major, minor);
    // If exception could not be created
    if (ex == NULL)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "SF::throwNotAuthorized() could not create exception\n");
        return;
    }

    // Throw the new exception
    (void) (*env)->Throw(env, ex);
}

/**
 * Throws an exception for the given error.
 * Throws nothing, and returns 0, if MPE_SUCCESS or MPE_SF_ERROR_SECTION_NOT_AVAILABLE are
 * specified.
 *
 * @param err the MPE error
 * @throws FilterResourceException given MPE_SF_ERROR_FILTER_NOT_AVAILABLE
 * @throws TuningException given MPE_SF_TUNER_NOT_TUNED or MPE_SF_TUNER_NOT_AT_FREQUENCY
 * @throws NotAuthorizedException given a CA error
 * @throws IllegalFilterDefinitionException given any other problems
 * @throws IllegalArgumentException given any other problems
 *
 * @return 1 if an exception was thrown
 */
static int throwByError(JNIEnv *env, mpe_Error err, int illArg)
{
    switch (err)
    {
    case MPE_SUCCESS:
    case MPE_SF_ERROR_SECTION_NOT_AVAILABLE: // not currently treated as an error
        return 0;
    case MPE_SF_ERROR_FILTER_NOT_AVAILABLE:
        jniutil_throwByName(env,
                "org/davic/mpeg/sections/FilterResourceException",
                "tuner not tuned to necessary stream");
        break;
    case MPE_SF_ERROR_TUNER_NOT_TUNED:
    case MPE_SF_ERROR_TUNER_NOT_AT_FREQUENCY:
        jniutil_throwByName(env, "org/davic/mpeg/TuningException",
                "native filter unavailable");
        break;
    case MPE_SF_ERROR_INVALID_SECTION_HANDLE:
    case MPE_EINVAL:
        if (illArg)
            jniutil_throwByName(env, "java/lang/IllegalArgumentException", "");
        else
            jniutil_throwByName(env,
                    "org/davic/mpeg/sections/IllegalFilterDefinitionException",
                    "");
        break;
    case MPE_SF_ERROR_UNSUPPORTED:
        jniutil_throwByName(env, "java/lang/UnsupportedOperationException",
                "Unsupported filter type");
        break;
    case MPE_ENOMEM:
        jniutil_throwByName(env, "java/lang/OutOfMemoryError", "");
        break;
    case MPE_SF_ERROR_CA:
        throwNotAuthorized(env,
                org_davic_mpeg_NotAuthorizedInterface_NOT_POSSIBLE,
                org_davic_mpeg_NotAuthorizedInterface_OTHER);
        break;
    case MPE_SF_ERROR_CA_ENTITLEMENT:
        throwNotAuthorized(env,
                org_davic_mpeg_NotAuthorizedInterface_NOT_POSSIBLE,
                org_davic_mpeg_NotAuthorizedInterface_NO_ENTITLEMENT);
        break;
    case MPE_SF_ERROR_CA_RATING:
        throwNotAuthorized(env,
                org_davic_mpeg_NotAuthorizedInterface_NOT_POSSIBLE,
                org_davic_mpeg_NotAuthorizedInterface_MATURITY_RATING);
        break;
    case MPE_SF_ERROR_CA_TECHNICAL:
        throwNotAuthorized(env,
                org_davic_mpeg_NotAuthorizedInterface_NOT_POSSIBLE,
                org_davic_mpeg_NotAuthorizedInterface_TECHNICAL);
        break;
    case MPE_SF_ERROR_CA_BLACKOUT:
        throwNotAuthorized(env,
                org_davic_mpeg_NotAuthorizedInterface_NOT_POSSIBLE,
                org_davic_mpeg_NotAuthorizedInterface_GEOGRAPHICAL_BLACKOUT);
        break;
    case MPE_SF_ERROR_CA_DIAG:
        throwNotAuthorized(
                env,
                org_davic_mpeg_NotAuthorizedInterface_POSSIBLE_UNDER_CONDITIONS,
                org_davic_mpeg_NotAuthorizedInterface_OTHER);
        break;
    case MPE_SF_ERROR_CA_DIAG_PAYMENT:
        throwNotAuthorized(
                env,
                org_davic_mpeg_NotAuthorizedInterface_POSSIBLE_UNDER_CONDITIONS,
                org_davic_mpeg_NotAuthorizedInterface_COMMERCIAL_DIALOG);
        break;
    case MPE_SF_ERROR_CA_DIAG_RATING:
        throwNotAuthorized(
                env,
                org_davic_mpeg_NotAuthorizedInterface_POSSIBLE_UNDER_CONDITIONS,
                org_davic_mpeg_NotAuthorizedInterface_MATURITY_RATING_DIALOG);
        break;
    case MPE_SF_ERROR_CA_DIAG_TECHNICAL:
        throwNotAuthorized(
                env,
                org_davic_mpeg_NotAuthorizedInterface_POSSIBLE_UNDER_CONDITIONS,
                org_davic_mpeg_NotAuthorizedInterface_TECHNICAL_DIALOG);
        break;
    case MPE_SF_ERROR_CA_DIAG_PREVIEW:
        throwNotAuthorized(
                env,
                org_davic_mpeg_NotAuthorizedInterface_POSSIBLE_UNDER_CONDITIONS,
                org_davic_mpeg_NotAuthorizedInterface_FREE_PREVIEW_DIALOG);
        break;
    }
    return 1;
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
 * Invokes <code>mpe_filterCancelFilter()</code> to stop filtering.
 *
 * @param filterHandle handle previously returned from {@link #nStartFiltering}
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nCancelFiltering
(JNIEnv *env, jclass cls, jint filterHandle)
{
    JNI_UNUSED(cls);
    if ( MPE_SUCCESS != mpe_filterRelease(filterHandle) )
    jniutil_throwByName(env, "java/lang/IllegalArgumentException", "Invalid handle");
}

/**
 * Returns a section for the given filter.
 *
 * @param filterHandle handle previously returned from {@link #nStartFiltering}
 * @return the unique native handle for the filtered section
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nGetSection(
        JNIEnv *env, jclass cls, jint filterHandle)
{
    mpe_FilterSectionHandle sectionHandle = (mpe_FilterSectionHandle) 0;

    JNI_UNUSED(cls);

    // Get the section handle and throw exception if necessary
    throwByError(env, mpe_filterGetSectionHandle(filterHandle, 0,
            &sectionHandle), 1);

    // Return the section handle if we have one
    return sectionHandle;
}

/**
 * Retrieves a copy of the data maintained by the native section
 * referenced by the given handle.
 *
 * @param handle
 * @return the data or <code>null</code> if no data is available
 */
JNIEXPORT jbyteArray JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nGetSectionData__I(
        JNIEnv *env, jclass cls, jint handle)
{
    uint32_t size;
    mpe_FilterSectionHandle section = (mpe_FilterSectionHandle) handle;
    JNI_UNUSED(cls);

    DBGMSG( DBG_ENTRY, ("<<NativeSection>>: nGetData(handle=%08x)\n", handle) );

    /* Figure section size. */
    if ((handle != 0) && (MPE_SUCCESS == mpe_filterGetSectionSize(section,
            &size)))
    {
        uint32_t nbytes;
        jbyteArray jbarray = (*env)->NewByteArray(env, size);
        jbyte* array;

        if ((jbarray == NULL) || ((array = (*env)->GetByteArrayElements(env,
                jbarray, NULL)) == NULL))
        {
            return NULL;
        }

        /* get data from section*/
        DBGMSG( DBG_INFO, ("<<NativeSection>>: reading %d bytes\n", size) );
        (void) mpe_filterSectionRead(section, 0, size, 0, (uint8_t*) array,
                &nbytes);
        (*env)->ReleaseByteArrayElements(env, jbarray, array, 0);

        if (nbytes == size)
        {
            return jbarray;
        }

        /* Let GC cleanup array data? */
        DBGMSG( DBG_ERROR, ("<<NativeSection>>: could not read entire section (%d)!\n", nbytes) );
    }

    /* return array */
    return NULL;
}

/**
 * Retrieves a copy of the data maintained by the native section
 * referenced by the given handle.
 *
 * @param handle native section handle
 * @param index defines within the filtered section the index of the first byte of the data to be retrieved.
 * The first byte of the section (the table_id field) has index 1.
 * @param length defines the number of consecutive bytes from the filtered section to be retrieved.
 * @return the data or <code>null</code> if no data is available
 * @exception java.lang.IndexOutOfBoundsException if any part of the filtered data requested would be
 * outside the range of data in the section.
 */
JNIEXPORT jbyteArray JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nGetSectionData__III(
        JNIEnv *env, jclass cls, jint handle, jint offset, jint length)
{
    uint32_t size;
    mpe_FilterSectionHandle section = (mpe_FilterSectionHandle) handle;
    JNI_UNUSED(cls);

    DBGMSG( DBG_ENTRY, ("<<NativeSection>>: nGetData(handle=%08x, offset=%d, length=%d)\n", handle, offset, length) );

    /* Figure section size. */
    if ((handle != 0) && (MPE_SUCCESS == mpe_filterGetSectionSize(section,
            &size)))
    {
        uint32_t nbytes;
        jbyteArray jbarray;
        jbyte* array;

        if ((offset < 0) || (offset + length > (jint) size))
        {
            jniutil_throwByName(env, "java/lang/IndexOutOfBoundsException",
                    "offset/length out-of-range");
            return NULL;
        }

        jbarray = (*env)->NewByteArray(env, length);
        if (jbarray == NULL || (array = (*env)->GetByteArrayElements(env,
                jbarray, NULL)) == NULL)
        {
            return NULL;
        }

        /* get data from section*/
        (void) mpe_filterSectionRead(section, offset, length, 0,
                (uint8_t*) array, &nbytes);
        (*env)->ReleaseByteArrayElements(env, jbarray, array, 0);

        if (nbytes == (uint8_t) length)
        {
            return jbarray;
        }

        /* Let GC cleanup array data */
        DBGMSG( DBG_ERROR, ("<<NativeSection>>: could not read entire section (%d)!\n", nbytes) );
    }

    /* return array */
    return NULL;
}

/**
 * Retrieves the single byte maintained by the native section
 * referenced by the given handle at the given index.
 *
 * @param handle native section handle
 * @param index defines within the filtered section the index of the first byte of the data to be retrieved.
 * The first byte of the section (the table_id field) has index 1.
 * @exception java.lang.IndexOutOfBoundsException if any part of the filtered data requested would be
 * outside the range of data in the section.
 * @exception NoDataAvailableException if no valid data is available
 * (i.e., the native section is empty).
 * @exception IndexOutOfBoundsException if callever specified out-of-range index
 * (i.e., the native section is empty).
 */
JNIEXPORT jbyte JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nGetSectionByteAt(
        JNIEnv *env, jclass cls, jint handle, jint offset)
{
    mpe_Error retCode = MPE_SUCCESS;
    mpe_FilterSectionHandle section = (mpe_FilterSectionHandle) handle;
    uint8_t data = 0;
    uint32_t nbytes;
    JNI_UNUSED(cls);

    DBGMSG( DBG_ENTRY, ("<<NativeSection>>: nGetByteAt(handle=%08x, offset=%d)\n", handle, offset) );

    /* get data */
    if (handle != 0)
    {
        retCode = mpe_filterSectionRead(section, offset, 1, 0, &data, &nbytes);

        if (retCode != MPE_SUCCESS || nbytes != 1)
        {
            DBGMSG( DBG_ERROR, ("<<NativeSection>>: could not read from offset %d!\n", offset) );

            if (retCode == MPE_EINVAL)
            {
                jniutil_throwByName(env, "java/lang/IndexOutOfBoundsException",
                        "no data available at this offset");
            }
            else
            {
                jniutil_throwByName(env,
                        "org/davic/mpeg/sections/NoDataAvailableException",
                        "no data available at this offset");
            }
        }
    }
    return data;
}

/**
 * Returns <code>true</code> if the given native handle is considered
 * full.
 *
 * @param handle native section to test for <i>fullness</i>
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nGetSectionFullStatus(
        JNIEnv *env, jclass cls, jint handle)
{
    mpe_FilterSectionHandle section = (mpe_FilterSectionHandle) handle;
    uint32_t size = 0;
    JNI_UNUSED(cls);
    JNI_UNUSED(env);

    DBGMSG( DBG_ENTRY, ("<<NativeSection>>: nGetFullStatus(handle=%08x)\n") );

    /* Simply check section size. */
    return (jboolean)((handle != 0 && MPE_SUCCESS == mpe_filterGetSectionSize(
            section, &size) && size > 0) ? JNI_TRUE : JNI_FALSE);
}

/**
 * Disposes of the native section handle.
 *
 * @param  handle the handle for the native section to stop use of
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nDisposeSection
(JNIEnv *env, jclass cls, jint handle)
{
    JNI_UNUSED(cls);
    JNI_UNUSED(env);

    DBGMSG( DBG_ENTRY, ("<<NativeSection>>: nDispose(handle=%08x)\n") );

    if ( handle != 0
            && MPE_SUCCESS != mpe_filterSectionRelease((mpe_FilterSectionHandle)handle) )
    {
        DBGMSG( DBG_ERROR, ("<<NativeSection>>: failed to dispose %08x\n", handle) );
    }
}

/**
 * Registers the given <code>EDListener</code> to be notified of resource availability
 * events.
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nRegisterAvailability(
        JNIEnv *env, jclass cls, jobject jEdListener)
{
    mpe_Error err;
    mpe_EdHandle ed = NULL;

    JNI_UNUSED(cls);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "SF::nRegisterAvailability\n");

    /* Create ED handle */
    if (MPE_SUCCESS != (err = mpe_edCreateHandle(jEdListener,
            MPE_ED_QUEUE_NORMAL, NULL, MPE_ED_TERMINATION_OPEN, 0, &ed)))
    {
        throwByError(env, err, 0);
        return 0;
    }

    if (MPE_SUCCESS != mpe_filterRegisterAvailability(ed->eventQ, ed))
    {
        throwByError(env, err, 0);
        mpe_edDeleteHandle(ed);
        return 0;
    }

    return (jint) ed;
}

/**
 * Initializes native interface.
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_manager_sections_MpeSectionFilterApi_nInit
(JNIEnv *env, jclass cls)
{
    // Pre-cache exception class/constructor
    GET_CLASS(NotAuthorizedException, "org/cablelabs/impl/davic/mpeg/NotAuthorizedException");
    GET_METHOD_ID(NotAuthorizedException_init, "<init>", "(II)V");
}
