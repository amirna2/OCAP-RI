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
// // COPYRIGHT_END

#include <mpe_file.h>
#include <string.h>
#include "jni_str_util.h"
#include "org_cablelabs_impl_io_DefaultFileSys.h"

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    exists
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_exists(
        JNIEnv *env, jobject obj, jstring path)
{
    mpe_File file;
    mpe_Dir dir;
    jboolean result = JNI_FALSE;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_fileOpen(cpath, MPE_FS_OPEN_READ, &file) == MPE_FS_ERROR_SUCCESS)
        {
            mpe_fileClose(file);
            result = JNI_TRUE;
        }
        else if (mpe_dirOpen(cpath, &dir) == MPE_FS_ERROR_SUCCESS)
        {
            mpe_dirClose(dir);
            result = JNI_TRUE;
        }

    }
    END_NATIVE_STRING(env, cpath);

    return result;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    isFile
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_isFile(
        JNIEnv *env, jobject obj, jstring path)
{
    mpe_File file;
    jboolean result = JNI_FALSE;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_fileOpen(cpath, MPE_FS_OPEN_READ, &file) == MPE_FS_ERROR_SUCCESS)
        {
            mpe_fileClose(file);
            result = JNI_TRUE;
        }

    }
    END_NATIVE_STRING(env, cpath);

    return result;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    isDir
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_isDir(
        JNIEnv *env, jobject obj, jstring path)
{
    mpe_Dir dir;
    jboolean result = JNI_FALSE;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_dirOpen(cpath, &dir) == MPE_FS_ERROR_SUCCESS)
        {
            mpe_dirClose(dir);
            result = JNI_TRUE;
        }

    }
    END_NATIVE_STRING(env, cpath);

    return result;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    list
 * Signature: (Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_list(
        JNIEnv *env, jobject obj, jstring path)
{
    int32_t len, maxlen, i;
    jobjectArray rv, old;
    mpe_Dir handle;
    mpe_DirEntry entry;
    mpe_FileError err = MPE_FS_ERROR_SUCCESS;
    jclass strClass = (*env)->FindClass(env,"java/lang/String");

    WITH_NATIVE_STRING(env, path, cpath)
    {
        err = mpe_dirOpen(cpath, &handle);
    }
    END_NATIVE_STRING(env, cpath);

    if (err != MPE_FS_ERROR_SUCCESS)
        return NULL;

    /* Allocate an initial String array */
    len = 0;
    maxlen = 16;
    rv = (*env)->NewObjectArray(env, maxlen, strClass, NULL);
    if (rv == NULL)
        goto error;

    /* Scan the directory */

    while (mpe_dirRead(handle, &entry) == MPE_FS_ERROR_SUCCESS)
    {
        jstring name;

        if (len == maxlen)
        {
            old = rv;
            rv = (*env)->NewObjectArray(env, maxlen <<= 1, strClass, NULL);
            if (rv == NULL)
                goto error;
            for (i = 0; i < len; i++)
            {
                jstring p = (*env)->GetObjectArrayElement(env, old, i);
                (*env)->SetObjectArrayElement(env, rv, i, p);
                (*env)->DeleteLocalRef(env, p);
            }
            (*env)->DeleteLocalRef(env, old);
        }

        // Do not show ".." or "." entries
        if (strcmp(entry.name, "..") != 0 && strcmp(entry.name, ".") != 0)
        {
            name = (*env)->NewStringUTF(env, entry.name);
            if (name == NULL)
                goto error;
            (*env)->SetObjectArrayElement(env, rv, len++, name);
            (*env)->DeleteLocalRef(env, name);
        }
    }

    /* Copy the final results into an appropriately-sized array */
    old = rv;
    rv = (*env)->NewObjectArray(env, len, strClass, NULL);
    if (rv == NULL)
        goto error;
    for (i = 0; i < len; i++)
    {
        jstring p = (*env)->GetObjectArrayElement(env, old, i);
        (*env)->SetObjectArrayElement(env, rv, i, p);
        (*env)->DeleteLocalRef(env, p);
    }

    mpe_dirClose(handle);
    return rv;

    error: mpe_dirClose(handle);
    return NULL;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    length
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_length(
        JNIEnv *env, jobject obj, jstring path)
{
    jlong rv = 0;
    mpe_FileStatMode mode = MPE_FS_STAT_SIZE;
    mpe_FileInfo info;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_fileGetStat(cpath, mode, &info) == MPE_FS_ERROR_SUCCESS)
        {
            rv = info.size;
        }

    }
    END_NATIVE_STRING(env, cpath);

    return rv;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    canRead
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_canRead(
        JNIEnv *env, jobject obj, jstring path)
{
    mpe_Error errcode = MPE_FS_ERROR_FAILURE;
    mpe_File fh;
    mpe_Dir dh;
    mpe_Stream sh;
    mpe_FileStatMode mode = MPE_FS_OPEN_READ;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if ((errcode = mpe_fileOpen(cpath, mode, &fh)) == MPE_FS_ERROR_SUCCESS)
        mpe_fileClose(fh);
        else if ((errcode = mpe_dirOpen(cpath, &dh)) == MPE_FS_ERROR_SUCCESS)
        mpe_dirClose(dh);
        else if ((errcode = mpe_streamOpen(cpath, &sh)) == MPE_FS_ERROR_SUCCESS)
        (void)mpe_streamClose(sh);

    }
    END_NATIVE_STRING(env, cpath);

    return errcode == MPE_FS_ERROR_SUCCESS ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    canWrite
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_canWrite(
        JNIEnv *env, jobject obj, jstring path)
{
    mpe_Error errcode = MPE_FS_ERROR_FAILURE;
    mpe_File fh;
    mpe_Dir dh;
    mpe_Stream sh;
    mpe_FileStatMode mode = MPE_FS_OPEN_WRITE;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if ((errcode = mpe_fileOpen(cpath, mode, &fh)) == MPE_FS_ERROR_SUCCESS)
        mpe_fileClose(fh);
        else if ((errcode = mpe_dirOpen(cpath, &dh)) == MPE_FS_ERROR_SUCCESS)
        mpe_dirClose(dh);
        else if ((errcode = mpe_streamOpen(cpath, &sh)) == MPE_FS_ERROR_SUCCESS)
        {
            errcode = MPE_FS_ERROR_FAILURE; // Streams are never writeable
            (void)mpe_streamClose(sh);
        }

    }
    END_NATIVE_STRING(env, cpath);

    return errcode == MPE_FS_ERROR_SUCCESS ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    create
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_ncreate(
        JNIEnv *env, jobject obj, jstring path)
{
    jboolean rv = JNI_FALSE;
    mpe_FileOpenMode mode = MPE_FS_OPEN_MUST_CREATE | MPE_FS_OPEN_WRITE;
    mpe_File handle;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_fileOpen(cpath, mode, &handle) == MPE_FS_ERROR_SUCCESS)
        {
            mpe_fileClose(handle);
            rv = JNI_TRUE;
        }

    }
    END_NATIVE_STRING(env, cpath);

    return rv;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    delete
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_ndelete(
        JNIEnv *env, jobject obj, jstring path)
{
    jboolean rv = JNI_FALSE;
    jboolean isDir = Java_org_cablelabs_impl_io_DefaultFileSys_isDir(env, obj,
            path);

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (isDir && (mpe_dirDelete(cpath) == MPE_FS_ERROR_SUCCESS))
        rv = JNI_TRUE;
        else if (mpe_fileDelete(cpath) == MPE_FS_ERROR_SUCCESS)
        rv = JNI_TRUE;

    }
    END_NATIVE_STRING(env, cpath);

    return rv;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    lastModified
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_lastModified(
        JNIEnv *env, jobject obj, jstring path)
{
    jlong rv = 0;
    mpe_FileStatMode mode = MPE_FS_STAT_MODDATE;
    mpe_FileInfo info;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_fileGetStat(cpath, mode, &info) == MPE_FS_ERROR_SUCCESS)
        rv = ((jlong)info.modDate) * 1000;

    }
    END_NATIVE_STRING(env, cpath);

    return rv;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    nmkdir
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_nmkdir(
        JNIEnv *env, jobject obj, jstring path)
{
    jboolean rv = JNI_FALSE;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        if (mpe_dirCreate(cpath) == MPE_FS_ERROR_SUCCESS)
        rv = JNI_TRUE;

    }
    END_NATIVE_STRING(env, cpath);

    return rv;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    renameTo
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_nrenameTo(
        JNIEnv *env, jobject obj, jstring fromPath, jstring toPath)
{
    jboolean rv = JNI_FALSE;

    WITH_NATIVE_STRING(env, fromPath, fromCPath)
    {
        WITH_NATIVE_STRING(env, toPath, toCPath)
        {
            if (mpe_fileRename(fromCPath, toCPath) == MPE_FS_ERROR_SUCCESS)
            rv = JNI_TRUE;

        }END_NATIVE_STRING(env, toCPath);

    }
    END_NATIVE_STRING(env, fromCPath);

    return rv;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    setLastModified
 * Signature: (Ljava/lang/String;J)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_setLastModified(
        JNIEnv *env, jobject obj, jstring path, jlong date)
{
    jboolean rv = JNI_FALSE;
    mpe_FileStatMode mode = MPE_FS_STAT_MODDATE;
    mpe_FileInfo info;

    WITH_NATIVE_STRING(env, path, cpath)
    {
        /* FIX: determine whether there should be a 32/64 bit conversion*/
        info.modDate = date / 1000;
        if (mpe_fileSetStat(cpath, mode, &info) == MPE_FS_ERROR_SUCCESS)
        {
            rv = JNI_TRUE;
        }

    }
    END_NATIVE_STRING(env, cpath);

    return rv;
}

/*
 * Class:     org_cablelabs_impl_io_DefaultFileSys
 * Method:    deleteOnExit
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_io_DefaultFileSys_deleteOnExit(
        JNIEnv *env, jobject obj, jstring path)
{
    return JNI_FALSE; // Implement me
}
