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

#include "org_cablelabs_impl_io_DefaultWriteableFileSys.h"
#include "org_cablelabs_impl_io_DefaultOpenFile.h"

#include <mpe_file.h>

static int mpe_fs_error_devfull = 0;

/*
 * Class:     org_cablelabs_impl_io_DefaultWriteableFileSys
 * Method:    nativeWrite
 * Signature: (I[BII)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_io_DefaultWriteableFileSys_nativeWrite
(JNIEnv *env, jobject obj, jint handle, jbyteArray src, jint off, jint length)
{
    jbyte* data = (*env)->GetByteArrayElements(env, src, NULL);
    uint32_t count = length;

    if (mpe_fileWrite((mpe_File)handle, &count, (void*)(data + off)) != MPE_FS_ERROR_SUCCESS ||
            length != count)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
    }

    (*env)->ReleaseByteArrayElements(env, src, data, 0);
}

/*
 * Class:     org_cablelabs_impl_io_DefaultWriteableFileSys
 * Method:    setLength
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_io_DefaultWriteableFileSys_setLength
(JNIEnv *env, jobject obj, jint handle, jlong length)
{
    mpe_FileInfo info;
    info.size = (int64_t)length;

    if (mpe_fileSetFStat((mpe_File)handle, MPE_FS_STAT_SIZE, &info) != MPE_FS_ERROR_SUCCESS)
    {
        jclass excClass = (*env)->FindClass(env, "java/io/IOException");
        if (excClass != NULL)
        {
            (*env)->ThrowNew(env, excClass, "");
        }
    }
}

/*
 * Class:     org_cablelabs_impl_io_DefaultWriteableFileSys
 * Method:    seek
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_io_DefaultWriteableFileSys_seek
(JNIEnv *env, jobject obj, jint handle, jlong pos)
{
    Java_org_cablelabs_impl_io_DefaultOpenFile_seek(env,obj,handle,pos);
}

/*
 * Class:     org_cablelabs_impl_io_DefaultWriteableFileSys
 * Method:    getFilePointer
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_io_DefaultWriteableFileSys_getFilePointer(
        JNIEnv *env, jobject obj, jint handle)
{
    return Java_org_cablelabs_impl_io_DefaultOpenFile_getFilePointer(env, obj,
            handle);
}

/*
 * Class:     org_cablelabs_impl_io_DefaultWriteableFileSys
 * Method:    setDevFullErrVal
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_org_cablelabs_impl_io_DefaultWriteableFileSys_setDevFullErrVal
(JNIEnv *env, jobject this, jint err)
{
    mpe_fs_error_devfull = err;
}

