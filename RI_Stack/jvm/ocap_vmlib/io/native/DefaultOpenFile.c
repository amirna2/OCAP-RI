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

#include "org_cablelabs_impl_io_DefaultOpenFile.h"

#include <mpe_file.h>
#include <mpe_os.h>
#include "jni_str_util.h"

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    open
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_open(
        JNIEnv *env, jobject obj, jstring path, jboolean read, jboolean write,
        jboolean append)
{
    mpe_File handle;
    jint retVal = 0;
    mpe_FileOpenMode mode = 0;

    if (write)
    {
        mode |= MPE_FS_OPEN_WRITE;
        mode |= MPE_FS_OPEN_CAN_CREATE;
        mode |= MPE_FS_OPEN_TRUNCATE;
    }
    if (append)
    {

        mode |= MPE_FS_OPEN_WRITE;
        mode |= MPE_FS_OPEN_CAN_CREATE;
        mode |= MPE_FS_OPEN_APPEND;
        mode &= ~MPE_FS_OPEN_TRUNCATE;
    }
    // Setup open mode
    if (read)
    {
        mode |= MPE_FS_OPEN_READ;
        mode &= ~MPE_FS_OPEN_TRUNCATE;
    }

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_fileOpen(cpath, mode, &handle) == MPE_FS_ERROR_SUCCESS)
        {
            retVal = (jint)handle;
        }
        else
        {
            jclass excClass = (*env)->FindClass(env, "java/io/IOException");
            if (excClass != NULL)
            {
                (*env)->ThrowNew(env, excClass, "");
            }
            (*env)->DeleteLocalRef(env, excClass);
        }

    }
    END_NATIVE_STRING(env, cpath);

    return retVal;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    read
 * Signature: ([BII)I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_read__I_3BII(
        JNIEnv *env, jobject obj, jint handle, jbyteArray dest, jint offset,
        jint length)
{
    uint8_t *data;
    uint32_t count = length;

    if (mpe_memAllocP(MPE_MEM_FILE, count, (void**) &data) != MPE_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
        return 0;
    }

    if (mpe_fileRead((mpe_File) handle, &count, data) != MPE_FS_ERROR_SUCCESS)
    {
        mpe_memFreeP(MPE_MEM_FILE, data);
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
        return 0;
    }

    (*env)->SetByteArrayRegion(env, dest, offset, count, (jbyte*) data);
    mpe_memFreeP(MPE_MEM_FILE, data);

    // If there is not data left in the file to read, return -1
    if (count == 0)
        return -1;

    return (jint) count;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    read
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_read__I(
        JNIEnv *env, jobject obj, jint handle)
{
    uint8_t data;
    uint32_t count = 1;

    if (mpe_fileRead((mpe_File) handle, &count, &data) != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
        return 0;
    }

    // If there is not data left in the file to read, return -1
    if (count == 0)
        return -1;

    return (jint) data;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    available
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_available(
        JNIEnv *env, jobject obj, jint handle)
{
    int64_t offset = 0;
    mpe_FileInfo info;

    if (mpe_fileSeek((mpe_File) handle, MPE_FS_SEEK_CUR, &offset)
            != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
        return 0;
    }

    if (mpe_fileGetFStat((mpe_File) handle, MPE_FS_STAT_SIZE, &info)
            != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
        return 0;
    }

    // If the file pointer is beyond the length of the file, return 0
    if (info.size < offset)
        return 0;

    return (jint)(info.size - offset);
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    skip
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_skip(
        JNIEnv *env, jobject obj, jint handle, jlong n)
{
    int64_t offset = 0;
    int64_t curOffset;

    if (mpe_fileSeek((mpe_File) handle, MPE_FS_SEEK_CUR, &offset)
            != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
        return 0;
    }

    curOffset = offset;
    offset = (int64_t) n;

    if (mpe_fileSeek((mpe_File) handle, MPE_FS_SEEK_CUR, &offset)
            != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
        return 0;
    }

    return (jint)(offset - curOffset);
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_close
(JNIEnv *env, jobject obj, jint handle)
{
    if (mpe_fileClose((mpe_File)handle) != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
    }
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    length
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_length(
        JNIEnv *env, jobject obj, jint handle)
{
    mpe_FileInfo info;

    if (mpe_fileGetFStat((mpe_File) handle, MPE_FS_STAT_SIZE, &info)
            != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
    }

    return (jint) info.size;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    getFilePointer
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_getFilePointer(
        JNIEnv *env, jobject obj, jint handle)
{
    int64_t offset = 0;

    if (mpe_fileSeek((mpe_File) handle, MPE_FS_SEEK_CUR, &offset)
            != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
    }

    return (jint) offset;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultOpenFile
 * Method:    seek
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_io_DefaultOpenFile_seek
(JNIEnv *env, jobject obj, jint handle, jlong pos)
{
    int64_t offset = (int64_t)pos;

    if (mpe_fileSeek((mpe_File)handle, MPE_FS_SEEK_SET, &offset) != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
        (*env)->DeleteLocalRef(env, excClass);
    }
}

