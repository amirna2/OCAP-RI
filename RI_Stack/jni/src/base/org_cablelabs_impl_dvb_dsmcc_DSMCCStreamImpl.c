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

#include <org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl.h>
#include <mgrdef.h>
#include <mpe_file.h>
#include <mpe_ed.h>
#include <mpe_dbg.h>
#include <mpe_file.h>

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeOpenStream
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeOpenStream(
        JNIEnv *env, jclass clazz, jstring jPath)
{
    const char *cPath;
    mpe_Stream stream = NULL;
    mpe_Error retCode;
    jclass errClass;
    char *exceptionClass = NULL;

    MPE_UNUSED_PARAM(clazz);

    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return (jint) stream;
    }

    retCode = mpe_streamOpen(cPath, &stream);
    // Decode the return code a bit.
    if (retCode == MPE_FS_ERROR_INVALID_TYPE)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "DSMCCStreamImpl: %s: Not a Stream.  Throwing IllegalObjectTypeException\n",
                cPath);
        exceptionClass = "org/dvb/dsmcc/IllegalObjectTypeException";
    }
    else if (retCode == MPE_FS_ERROR_NOT_FOUND)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "DSMCCStreamImpl: %s: File not found.  Throwing FileNotFound\n",
                cPath);
        exceptionClass = "java/io/FileNotFoundException";
    }
    else if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "DSMCCStreamImpl: %s: Other error (%d).  Throwing MPEGDeliveryException\n",
                cPath, retCode);
        exceptionClass = "org/dvb/dsmcc/MPEGDeliveryException";
    }
    if (exceptionClass != NULL)
    {
        errClass = (*env)->FindClass(env, exceptionClass);
        (*env)->ThrowNew(env, errClass, cPath);
        stream = 0;
    }
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    return (jint)(stream);
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeHasStream
 * Signature: (JI)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeHasStream(
        JNIEnv *env, jclass clazz, jint handle, jint streamType)
{
    mpe_Error retCode;
    mpe_FileInfo fInfo;
    mpe_FileStatMode fsStreamType;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    switch (streamType)
    {
    case org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_VIDEO:
        fsStreamType = MPE_FS_STAT_IS_VIDEO;
        break;
    case org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_AUDIO:
        fsStreamType = MPE_FS_STAT_IS_AUDIO;
        break;
    case org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_DATA:
        fsStreamType = MPE_FS_STAT_IS_DATA;
        break;
    default:
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "DSMCCStreamImpl: Unexpected argument to nativeHasStream: %d\n",
                (int) streamType);
        return JNI_FALSE;
    }

    retCode = mpe_fileGetFStat((mpe_File) handle, fsStreamType, &fInfo);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "DSMCCStreamImpl: Could not get Stream Type\n");
        return JNI_FALSE;
    }

    return (jboolean) fInfo.hasType;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeGetDuration
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeGetDuration(
        JNIEnv *env, jclass clazz, jint handle)
{
    mpe_Error retCode;
    mpe_FileInfo fInfo;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    retCode = mpe_fileGetFStat((mpe_File) handle, MPE_FS_STAT_DURATION, &fInfo);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "DSMCCStreamImpl: Could not get Duration\n");
        return 0;
    }

    return (jlong) fInfo.duration;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeCloseStream
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeCloseStream
(JNIEnv *env, jclass clazz, jint handle)
{
    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    (void)mpe_streamClose((mpe_Stream) handle);
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeGetNumTAPs
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeGetNumTAPs(
        JNIEnv *env, jclass clazz, jint handle, jint tapType)
{
    mpe_Error retCode;
    uint32_t numTaps;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    retCode = mpe_streamGetNumTaps((mpe_File) handle, (uint16_t) tapType,
            &numTaps);
    if (retCode != MPE_SUCCESS)
    {
        return 0;
    }
    return numTaps;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeGetComponentTag
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeGetComponentTag(
        JNIEnv *env, jclass clazz, jint handle, jint tapType, jint tapNumber)
{
    mpe_Error retCode;
    uint16_t tapTag;
    uint16_t tapID;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    retCode = mpe_streamReadTap((mpe_File) handle, (uint16_t) tapType,
            tapNumber, &tapTag, &tapID);
    if (retCode == MPE_SUCCESS)
    {
        return (jint) tapTag;
    }
    else
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "DSMCCStreamImpl: Could not the %d tap of type %x in stream %08x\n",
                (int)tapNumber, (int)tapType, (int)handle);
        return -1;
    }
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeGetNptID
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeGetNptID(
        JNIEnv *env, jclass clazz, jint handle)
{
    mpe_Error retCode;
    uint16_t tapTag;
    uint16_t tapID;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    retCode = mpe_streamReadTap((mpe_File) handle,
            org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_STR_NPT_USE, 0,
            &tapTag, &tapID);
    if (retCode == MPE_SUCCESS)
    {
        return (jint) tapID;
    }
    else
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "DSMCCStreamImpl: Could not the NPT tap stream %08x\n", (int)handle);
        return -1;
    }
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeGetFrequency
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeGetFrequency(
        JNIEnv *env, jclass clazz, jint handle)
{
    mpe_Error retCode;
    mpe_FileInfo info;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    retCode = mpe_fileGetFStat((mpe_File) handle, MPE_FS_STAT_TUNING_INFO,
            &info);
    if (retCode != MPE_SUCCESS)
    {
        return 0;
    }
    return info.freq;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeGetProgram
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeGetProgram(
        JNIEnv *env, jclass clazz, jint handle)
{
    mpe_Error retCode;
    mpe_FileInfo info;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    retCode = mpe_fileGetFStat((mpe_File) handle, MPE_FS_STAT_TUNING_INFO,
            &info);
    if (retCode != MPE_SUCCESS)
    {
        return -1;
    }
    return info.prog;
}

/*
 * Class:     org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl
 * Method:    nativeGetTargetProgram
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_nativeGetTargetProgram(
        JNIEnv *env, jclass clazz, jint handle)
{
    mpe_Error retCode;
    uint16_t tap;
    uint32_t targetProg;
    uint16_t tapID; // Sink location, not used here.
    mpe_FileInfo info;
    mpe_SiServiceHandle siHandle;

    MPE_UNUSED_PARAM(env);
    MPE_UNUSED_PARAM(clazz);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "DSMCCStreamImpl: Getting target program for stream %08x\n", (int)handle);
    // Read the tap
    retCode = mpe_streamReadTap((mpe_Stream) handle,
            org_cablelabs_impl_dvb_dsmcc_DSMCCStreamImpl_BIOP_PROGRAM_USE, 0,
            &tap, &tapID);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "DSMCCStreamImpl: No BIOP_PROGRAM_USE Tap found\n");
        return -1;
    }

    // Figure out where we are right now.
    retCode = mpe_fileGetFStat((mpe_File) handle, MPE_FS_STAT_TUNING_INFO,
            &info);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "DSMCCStreamImpl: Could not get current frequency\n");
        return -1;
    }

    retCode = mpe_fileGetFStat((mpe_File) handle, MPE_FS_STAT_SIHANDLE, &info);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "DSMCCStreamImpl: Could not get SI Handle\n");
        return -1;
    }
    siHandle = info.siHandle;

    // lock SI Handle
    if (mpe_siLockForRead() != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "DSMCCStreamImpl: Could not lock SI Handle\n");
        return -1;
    }

    // Translate the program number
    retCode = mpe_siGetProgramNumberByDeferredAssociationTag(siHandle, tap,
            &targetProg);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_JNI,
                "DSMCCStreamImpl: No deferred association tag for %04x (%d)\n",
                tap, (int) retCode);
        mpe_siUnLock();
        return -1;
    }

    mpe_siUnLock();

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "DSMCCStreamImpl: Translated tap %d into program number %d\n", tap,
            targetProg);
    return targetProg;
}
