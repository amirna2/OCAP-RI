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

#include <org_cablelabs_impl_dvb_dsmcc_ObjectCarousel.h>
#include "org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel.h"
#include "mgrdef.h"
#include "mpe_file.h"
#include <mpe_ed.h>
#include "mpe_dbg.h"

typedef struct
{
    mpe_EdEventInfo *edHandle;
    mpe_FileChangeHandle changeHandle;
} jniObjectChangeHandle;

/**
 * Attach the specified carousel
 *
 * @param env the JNI environment
 * @param cls the ServiceDomain object
 * @param jUrl the URL of the carousel
 * @param siUniqueifier a unique handle to the service in the SI database
 * @param carouselId the carousel ID
 * @throws MPEGDeliveryException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeGoMount
(JNIEnv *env, jclass cls, jstring jUrl, jint siHandle, jint carouselId)
{
    const char *cUrl = NULL;
    mpe_DirUrl url;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RealObjectCarousel:nativeGoMount(%08x, %04x)\n", (int)siHandle, (int)carouselId);
    MPE_UNUSED_PARAM(cls);

    if ((cUrl = (*env)->GetStringUTFChars(env, jUrl, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RealObjectCarousel:nativeGoMount(%s, %08x, %04x)\n", cUrl, (int)siHandle, (int)carouselId);

    /* try to mount the carousel */
    url.url = cUrl;
    url.siHandle = siHandle;
    url.carouselId = carouselId;
    if (mpe_dirMount(&url) != MPE_FS_ERROR_SUCCESS)
    {
        // TODO(Todd): Throw a more specific exception based on the MPE error code
        (*env)->ThrowNew(env,(*env)->FindClass(env,"org/dvb/dsmcc/MPEGDeliveryException"),
                "Cannot mount the indicated URL");
    }

    /* cleanup & return */
    (*env)->ReleaseStringUTFChars(env, jUrl, cUrl);
}

/**
 * Re-attach the specified carousel
 *
 * @param env the JNI environment
 * @param cls the ServiceDomain object
 * @param jUrl the URL of the carousel
 * @param siUniqueifier a unique handle to the service in the SI database
 * @param carouselId the carousel ID
 * @throws MPEGDeliveryException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeReMount
(JNIEnv *env, jclass cls, jstring jPath, jint siHandle, jint carouselId)
{
    // TODO(ERIC): Change to using DirSetStat instead of fileSetStat.
    mpe_FileInfo fileInfo;
    const char *cPath = NULL;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RealObjectCarousel:nativeReMount(%08x, %04x)\n", (int)siHandle, (int)carouselId);
    MPE_UNUSED_PARAM(cls);

    // Grab the C version of the path
    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RealObjectCarousel:nativeReMount(%s, %08x, %04x)\n", cPath, (int)siHandle, (int)carouselId);

    // Force the
    fileInfo.siHandle = siHandle;
    (void)mpe_fileSetStat(cPath, MPE_FS_STAT_SIHANDLE, &fileInfo);

    // TODO: What should we do with the retcode?  Probably through an exception, but which?

    // Release the string
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);
}

/**
 * Detach the specified carousel
 *
 * @param env the JNI environment
 * @param cls the ServiceDomain object
 * @param jUrl the URL of the carousel
 * @param siUniqueifier a unique handle to the service in the SI database
 * @param carouselId the carousel ID
 * @throws NotLoadedException
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeGoUnmount
(JNIEnv *env, jclass cls, jstring jUrl, jint siHandle, jint carouselId)
{
    const char *cUrl = NULL;
    mpe_DirUrl url;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RealObjectCarousel:nativeGoUnmount(%08x, %04x)\n", (int)siHandle, (int)carouselId);
    MPE_UNUSED_PARAM(cls);

    if ((cUrl = (*env)->GetStringUTFChars(env, jUrl, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "RealObjectCarousel:nativeGoUnmount(%s, %08x, %04x)\n", cUrl, (int)siHandle, (int)carouselId);

    /* try to unmount the carousel */
    url.url = cUrl;
    url.siHandle = siHandle;
    url.carouselId = carouselId;
    if (mpe_dirUnmount(&url) != MPE_FS_ERROR_SUCCESS)
    {
        /* throw IOException */
        (*env)->ThrowNew(env,(*env)->FindClass(env,"org/dvb/dsmcc/NotLoadedException"),
                "Cannot unmount the indicated URL");
    }

    /* cleanup & return */
    (*env)->ReleaseStringUTFChars(env, jUrl, cUrl);
}

/**
 * Get the native portion of the path for this carousel
 *
 * @param env the JNI environment
 * @param cls the ServiceDomain object
 * @param jUrl the URL of the carousel
 * @param siUniqueifier a unique handle to the service in the SI database
 * @param carouselId the carousel ID
 * @throws NotLoadedException
 * @throws java.io.FileNotFoundException
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeGetPath(
        JNIEnv *env, jclass cls, jstring jUrl, jint siHandle, jint carouselId)
{
    jstring jPath = (jstring) NULL;
    const char *cUrl;
    mpe_DirInfo info;
    mpe_DirUrl url;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "RealObjectCarousel:nativeGetPath(%08x, %04x)\n", (int)siHandle, (int)carouselId);
    MPE_UNUSED_PARAM(cls);

    if ((cUrl = (*env)->GetStringUTFChars(env, jUrl, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return jPath;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "RealObjectCarousel:nativeGetPath(%s, %08x, %04x)\n", cUrl,
            (int)siHandle, (int)carouselId);

    /* get the file-system path for the indicated url */
    url.url = cUrl;
    url.siHandle = siHandle;
    url.carouselId = carouselId;
    if (mpe_dirGetUStat(&url, MPE_FS_STAT_MOUNTPATH, &info)
            != MPE_FS_ERROR_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env,
                "org/dvb/dsmcc/NotLoadedException"),
                "Cannot retrieve the file-system path for the indicated URL");
    }
    else
    {
        /* return a copy of the file-system path string (as long as it's not empty) */
        if (info.path[0] != 0)
        {
            jPath = (*env)->NewStringUTF(env, info.path);
        }
    }

    /* cleanup & return */
    (*env)->ReleaseStringUTFChars(env, jUrl, cUrl);
    return jPath;
}

/**
 * Check the connection to the carousel
 *
 * @param env the JNI environment
 * @param cls the ServiceDomain object
 * @param jUrl the URL of the carousel
 * @throws NotLoadedException
 * @throws java.io.FileNotFoundException
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeCheckConnection(
        JNIEnv *env, jclass cls, jstring jUrl, jint siHandle, jint carouselId)
{
    jboolean jConnection = (jboolean) 0;
    const char *cUrl;
    mpe_DirInfo info;
    mpe_DirUrl url;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "RealObjectCarousel:nativeCheckConnection(%08x, %04x)\n", (int)siHandle,
            (int)carouselId);
    MPE_UNUSED_PARAM(cls);

    /* get parameters */
    if ((cUrl = (*env)->GetStringUTFChars(env, jUrl, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return jConnection;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "RealObjectCarousel:nativeCheckConnection(%s, %08x, %04x)\n", cUrl,
            (int)siHandle, (int)carouselId);

    /* get the connection status for the indicated url */
    url.url = cUrl;
    url.siHandle = siHandle;
    url.carouselId = carouselId;
    if (mpe_dirGetUStat(&url, MPE_FS_STAT_CONNECTIONAVAIL, &info)
            != MPE_FS_ERROR_SUCCESS)
    {
        /* throw IOException */
        (*env)->ThrowNew(env, (*env)->FindClass(env,
                "org/dvb/dsmcc/NotLoadedException"),
                "Cannot retrieve the file-system path for the indicated URL");
    }
    else
    {
        jConnection = (jboolean) info.isConnectAvail;
    }

    /* cleanup & return */
    (*env)->ReleaseStringUTFChars(env, jUrl, cUrl);
    return jConnection;
}

/**
 * Method:    nativeEnableObjectChangeEvents
 * Signature: (Ljava/lang/String;Lorg/dvb/dsmcc/DSMCCObject$VersionEdListener;I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeEnableObjectChangeEvents(
        JNIEnv *env, jclass cls, jstring jPath, jobject jReturnObj,
        jint jQueueType)
{
    const char *cPath;
    jniObjectChangeHandle *ocHandle;
    int edQueueType = (int) jQueueType;

    MPE_UNUSED_PARAM(cls);

    /* get the indicated file path */
    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return 0;
    }

    /* Create the internal tracking structure */

    if (mpe_memAllocP(MPE_MEM_FILE, sizeof(jniObjectChangeHandle),
            (void **) &ocHandle) != MPE_SUCCESS)
    {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot create object change handle");
        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
        return 0;
    }

    /* get ED handle for listener callback */
    if (mpe_edCreateHandle(
            jReturnObj,
            edQueueType,
            NULL,
            MPE_ED_TERMINATION_EVCODE,
            org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_EVENTCODE_OBJECTCHANGE_DONE,
            &(ocHandle->edHandle)) != MPE_SUCCESS)
    {
        /* throw IOException */
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot create listener callback handle");
        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
        mpe_memFreeP(MPE_MEM_FILE, ocHandle);
        return 0;
    }

    /* register ourselves for object change events */
    if (mpe_fileSetChangeListener(cPath, ocHandle->edHandle->eventQ,
            (void *) ocHandle->edHandle, &(ocHandle->changeHandle))
            != MPE_FS_ERROR_SUCCESS)
    {
        /* throw IOException */
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot disable registration for object change events");
        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
        mpe_edDeleteHandle(ocHandle->edHandle);
        mpe_memFreeP(MPE_MEM_FILE, ocHandle);
        return 0;
    }

    /* cleanup & return file known status */
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    return (jint) ocHandle;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel
 * Method:    nativeDisableObjectChangeEvents
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeDisableObjectChangeEvents
(JNIEnv *env, jclass cls, jint jHandle)
{
    jniObjectChangeHandle *ocHandle = (jniObjectChangeHandle *) jHandle;

    MPE_UNUSED_PARAM(cls);

    /* unregister ourselves for object change events */
    if (mpe_fileRemoveChangeListener(ocHandle->changeHandle) != MPE_FS_ERROR_SUCCESS)
    {
        /* throw IOException */
        (*env)->ThrowNew(env,(*env)->FindClass(env,"java/io/IOException"),
                "Cannot disable registration for object change events");
        // Just fall through, and send the cleanup event to the queue anyhow.
        // TODO: FIXME: BUG:
        // This could be a potential problem if the underlying filesystem can somehow fail while doing
        // this.  The object carousel cannot, so this is safe.
        // return;
    }

    // Send an event to ED to have him shut down this event.
    mpe_eventQueueSend(ocHandle->edHandle->eventQ, org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_EVENTCODE_OBJECTCHANGE_DONE, NULL, ocHandle->edHandle, 0);

    /* cleanup & return file known status */
    mpe_memFreeP(MPE_MEM_FILE, ocHandle);
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel
 * Method:    nativeGetFileInfoIsKnown
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeGetFileInfoIsKnown(
        JNIEnv *env, jclass cls, jstring jPath)
{
    const char *cPath;
    mpe_FileInfo info;

    MPE_UNUSED_PARAM(cls);

    /* get the indicated file path */
    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return (jboolean) JNI_FALSE;
    }

    /* get the referenced file's known status */
    if (mpe_fileGetStat(cPath, MPE_FS_STAT_ISKNOWN, &info)
            != MPE_FS_ERROR_SUCCESS)
    {
        /* throw IOException */
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot retrieve file known status");
        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
        return (jboolean) JNI_FALSE;
    }

    /* cleanup & return file known status */
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    return info.isKnown ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel
 * Method:    nativeGetFileInfoType
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeGetFileInfoType(
        JNIEnv *env, jclass cls, jstring jPath)
{
    const char *cPath;
    mpe_FileInfo info;
    mpe_Error retCode;

    MPE_UNUSED_PARAM(cls);

    /* get the indicated file path */
    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return org_cablelabs_impl_dvb_dsmcc_ObjectCarousel_TYPE_UNKNOWN;
    }

    /* get the referenced file's type */
    retCode = mpe_fileGetStat(cPath, MPE_FS_STAT_TYPE, &info);
    if (retCode != MPE_FS_ERROR_SUCCESS)
    {
        if (retCode == MPE_FS_ERROR_NOT_FOUND)
        {
            (*env)->ThrowNew(env, (*env)->FindClass(env,
                    "java/io/FileNotFoundException"), "Cannot open");
        }
        else
        {
            /* throw IOException */
            (*env)->ThrowNew(env,
                    (*env)->FindClass(env, "java/io/IOException"),
                    "Cannot retrieve file type");
        }
        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
        return org_cablelabs_impl_dvb_dsmcc_ObjectCarousel_TYPE_UNKNOWN;
    }

    /* cleanup & return file type */
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    return info.type;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel
 * Method:    nativePrefetchFile
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativePrefetchFile(
        JNIEnv *env, jclass cls, jstring jPath)
{
    const char *cPath;
    mpe_Error retCode;

    /* get the indicated file path */
    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return (jboolean) JNI_FALSE;
    }

    retCode = mpe_filePrefetch(cPath);

    /* cleanup & return file known status */
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    return (retCode == MPE_FS_ERROR_UNSUPPORT) ? (jboolean) JNI_FALSE
            : (jboolean) JNI_TRUE;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel
 * Method:    nativeResolveServiceXfr
 * Signature: (Ljava/lang/String;[B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_dvb_dsmcc_RealObjectCarousel_nativeResolveServiceXfr(
        JNIEnv *env, jclass cls, jstring jPath, jbyteArray jNSAP)
{
    const char *cPath;
    mpe_FileInfo fInfo;
    char buffer[MPE_FS_MAX_PATH]; // BUG:? Should this be malloced?
    jobject retString = NULL;

    /* Get the path */
    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return retString;
    }

    /* Fill in the buffer structure */
    fInfo.buf = buffer;
    fInfo.size = MPE_FS_MAX_PATH;

    /* Get the name and NSAP from MPE */
    if (mpe_fileGetStat(cPath, MPE_FS_STAT_TARGET_INFO, &fInfo) != MPE_SUCCESS)
    {
        /* Throw an exception.  Don't set a return string */
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Cannot resolve target info");
    }
    else
    {
        (*env)->SetByteArrayRegion(env, jNSAP, 0, 20, (jbyte *) fInfo.nsap);
        retString = (*env)->NewStringUTF(env, buffer);
    }
    /* Release the original buffer */
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);

    /* Return the string */
    return retString;
}
