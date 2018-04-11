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

#include "org_cablelabs_impl_manager_filesys_OCFileSys.h"
#include "org_dvb_dsmcc_DSMCCObject.h"

#include <mpe_file.h>
#include <mpe_dbg.h>

#include <string.h>

// Forward declaration
static void throwException(JNIEnv *, mpe_Error);

JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_exists(
        JNIEnv *env, jobject obj, jstring jpath)
{
    const char *cpath;
    jboolean returnVal = JNI_FALSE;
    mpe_FileInfo info;
    MPE_UNUSED_PARAM(obj);

    if ((cpath = (*env)->GetStringUTFChars(env, jpath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return returnVal;
    }

    /* if we can't successfully stat the file then assume it doesn't exist */
    if (mpe_fileGetStat(cpath, MPE_FS_STAT_TYPE, &info) == MPE_FS_ERROR_SUCCESS)
    {
        returnVal = JNI_TRUE;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): exists=%d for path=%s\n", returnVal, cpath);
    /* return allocated memory */
    (*env)->ReleaseStringUTFChars(env, jpath, cpath);

    return returnVal;
}

JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_isDir(
        JNIEnv *env, jobject obj, jstring jpath)
{
    const char *cpath;
    mpe_FileInfo info;
    jboolean returnVal = JNI_FALSE;
    MPE_UNUSED_PARAM(obj);

    if ((cpath = (*env)->GetStringUTFChars(env, jpath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return returnVal;
    }

    if (mpe_fileGetStat(cpath, MPE_FS_STAT_TYPE, &info) == MPE_FS_ERROR_SUCCESS)
    {
        if (info.type == MPE_FS_TYPE_DIR)
            returnVal = JNI_TRUE;
    }
    /* return allocated memory */
    (*env)->ReleaseStringUTFChars(env, jpath, cpath);

    return returnVal;
}

/* struct used to make a linked list of directory entries */
typedef struct dirList
{
    jstring path;
    struct dirList *next;
} dirList;

JNIEXPORT jobjectArray JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_list(
        JNIEnv *env, jobject obj, jstring jpath)
{
    const char *cpath;
    mpe_Dir handle;
    mpe_DirEntry entry;
    dirList *first = NULL, *last = NULL, *ptr;
    jobjectArray returnArray = NULL;
    int count = 0;
    mpe_Error retCode = MPE_SUCCESS;

    // TODO: Make stringClass static
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");

    MPE_UNUSED_PARAM(obj);

    if ((cpath = (*env)->GetStringUTFChars(env, jpath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return returnArray;
    }
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "OCFileSys.list: %s\n", cpath);

    if (stringClass == NULL)
    {
        /* FindClass through a "ClassNotFoundException" */
        return returnArray;
    }

    retCode = mpe_dirOpen(cpath, &handle);
    if (retCode != MPE_FS_ERROR_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
                "OCFileSys.list: Open failed: %s %04x\n", cpath, retCode);
        (*env)->ReleaseStringUTFChars(env, jpath, cpath);
        if (retCode == MPE_FS_ERROR_SERVICEXFER)
        {
            // Per MHP 11.5.1, if list results in a ServiceXfr error, return empty list
            return (*env)->NewObjectArray(env, 0, stringClass, NULL);
        }
        else
        {
            return NULL;
        }
    }

    /* Loop over the list, collecting all names of files. */
    while ((retCode = mpe_dirRead(handle, &entry)) == MPE_FS_ERROR_SUCCESS)
    {
        /* File.list() does not include . and .. */
        if (strcmp(entry.name, ".") != 0 && strcmp(entry.name, "..") != 0)
        {
            /* create new java.lang.String */
            jstring string = (*env)->NewStringUTF(env, entry.name);
            if (string != NULL)
            {
                /* add the Java string to a temporary linked list */
                if (mpe_memAllocP(MPE_MEM_FILE, sizeof(dirList), (void**) &ptr)
                        == MPE_SUCCESS)
                {
                    if (first == NULL)
                        first = ptr;
                    else if (last)
                        last->next = ptr;
                    ptr->path = string;
                    ptr->next = NULL;
                    last = ptr;
                    count++;
                }
            }
        }
    }

    /* close the directory as we no longer need it */
    mpe_dirClose(handle);
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI, "OCFileSys.list: %s: %d files\n",
            cpath, count);
    (*env)->ReleaseStringUTFChars(env, jpath, cpath);

    /* Now, make sure read all the entries correctly.  If we did, we ended with retCode == MPE_FS_ERROR_EOF.
     * If not this, just skip this and return null.  Otherwise, copy the list into something which can
     * be returned up to Java.
     */
    if (retCode == MPE_FS_ERROR_EOF)
    {
        if (stringClass != NULL)
        {
            /* allocate a new array */
            returnArray = (*env)->NewObjectArray(env, count, stringClass, NULL);
            if (returnArray != NULL)
            {
                int i;
                for (i = 0, ptr = first; i < count && ptr != NULL; i++, ptr
                        = ptr->next)
                {
                    (*env)->SetObjectArrayElement(env, returnArray, i,
                            ptr->path);
                }
            }
        }
    }

    /* release memory allocated for the dir list */
    for (ptr = first; ptr != NULL;)
    {
        first = ptr->next;
        mpe_memFreeP(MPE_MEM_FILE, ptr);
        ptr = first;
    }

    return returnArray;
}

JNIEXPORT jobject JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_nativeGetFileData(
        JNIEnv *env, jobject obj, jstring jPath, jint jCacheMode)
{
    const char *cPath;
    int cOpenMode;
    mpe_File file;
    mpe_Error retCode;
    mpe_FileInfo fInfo;
    jbyteArray jArray;
    jobject jRetObject = NULL;
    jbyte *cArray;
    uint32_t size;
    jclass ocfdClass;
    jmethodID cid;

    MPE_UNUSED_PARAM(obj);

    ocfdClass = (*env)->FindClass(env,
            "org/cablelabs/impl/manager/filesys/OCFileData");
    if (ocfdClass == NULL)
    {
        return NULL; /* exception thrown */
    }
    cid = (*env)->GetMethodID(env, ocfdClass, "<init>", "([BI)V");
    if (cid == NULL)
    {
        return NULL; /* exception thrown */
    }

    /* get parameters */
    if ((cPath = (*env)->GetStringUTFChars(env, jPath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return NULL;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): getFileData() called for: %s\n", cPath);

    switch (jCacheMode)
    {
    case org_dvb_dsmcc_DSMCCObject_FROM_CACHE:
        cOpenMode = MPE_FS_OPEN_CACHEONLY | MPE_FS_OPEN_NOLINKS
                | MPE_FS_OPEN_READ;
        break;
    case org_dvb_dsmcc_DSMCCObject_FROM_STREAM_ONLY:
        cOpenMode = MPE_FS_OPEN_STREAMONLY | MPE_FS_OPEN_NOLINKS
                | MPE_FS_OPEN_READ;
        break;
    case org_dvb_dsmcc_DSMCCObject_FROM_CACHE_OR_STREAM:
        cOpenMode = MPE_FS_OPEN_NOLINKS | MPE_FS_OPEN_READ;
        break;
    default:
        /* throw IOException */
        (*env)->ThrowNew(env, (*env)->FindClass(env,
                "org/dvb/dsmcc/InvalidPathNameException"),
                "Illegal Cache-Mode for accessing file");
        goto CleanUp;
    }

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): Opening file: %s\n", cPath);

    retCode = mpe_fileOpen(cPath, cOpenMode, &file);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): Open file complete: %s %04x\n", cPath, retCode);
    // Clean this up.  Match to error codes.
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_JNI,
                "OCFileSys(native): Failed to open file %s: %04x\n", cPath,
                retCode);
        throwException(env, retCode);
        goto CleanUp;
    }
    // Ok, we're here.  We're good.

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): Stating file: %s (%08x) \n", cPath, (int) file);

    retCode = mpe_fileGetFStat(file, MPE_FS_STAT_SIZE, &fInfo);
    if (retCode != MPE_SUCCESS)
    {
        throwException(env, retCode);
        mpe_fileClose(file);
        goto CleanUp;
    }
    size = (uint32_t) fInfo.size;
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): Stating file complete: %s %d bytes\n", cPath,
            size);

    /* Allocate up an array to hold the data */
    jArray = (*env)->NewByteArray(env, size);
    if (jArray == NULL)
    {
        /* exception already thrown */
        mpe_fileClose(file);
        goto CleanUp;
    }
    cArray = (*env)->GetByteArrayElements(env, jArray, 0);
    if (cArray == NULL)
    {
        /* exception already thrown */
        (*env)->DeleteLocalRef(env, jArray); // Probably not necessary
        mpe_fileClose(file);
        goto CleanUp;
    }

    // Actually read the data
    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): Reading file : %s %d bytes %p\n", cPath,
            size, cArray);

    retCode = mpe_fileRead(file, &size, cArray);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_JNI,
            "OCFileSys(native): Read complete.  %d bytes read\n", size);

    // Put it back in the JVM
    (*env)->ReleaseByteArrayElements(env, jArray, cArray, 0);
    if (retCode != MPE_SUCCESS)
    {
        // Read Failed.  Close the file, and throw an exception
        mpe_fileClose(file);
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI,
                "OCFileSys(native): Throwing IO Exeception\n");
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"),
                "Could not read file");
        (*env)->DeleteLocalRef(env, jArray); // Probably not necessary
        goto CleanUp;
    }

    MPE_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_JNI,
            "OCFileSys(native): Read complete.  %d bytes read.  Constructing object.\n",
            size);
    jRetObject = (*env)->NewObject(env, ocfdClass, cid, jArray, (jint) file);
    if (jRetObject == NULL)
    {
        // Exception thrown, cleanup.
        mpe_fileClose(file);
        goto CleanUp;
    }

    CleanUp:
    /* cleanup & return file type */
    (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    return jRetObject;
}

/*
 * Class:     org_cablelabs_impl_manager_filesys_OCFileSys
 * Method:    length
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_length(
        JNIEnv *env, jobject obj, jstring jPath)
{
    mpe_FileInfo info;
    jlong length;
    const char *cPath;

    MPE_UNUSED_PARAM(obj);

    length = 0L;
    if (NULL != jPath && NULL != (cPath = (*env)->GetStringUTFChars(env, jPath,
            NULL)))
    {
        if (MPE_FS_ERROR_SUCCESS == mpe_fileGetStat(cPath, MPE_FS_STAT_SIZE,
                &info))
            length = info.size;

        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    }

    // Note: if GetStringUTFChars() returned NULL, an exception has been thrown

    return length;
}

/*
 * Class:     org_cablelabs_impl_manager_filesys_OCFileSys
 * Method:    canRead
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_canRead(
        JNIEnv *env, jobject obj, jstring jPath)
{
    mpe_File fh;
    mpe_Dir dh;
    mpe_Stream sh;
    const char *cPath;
    mpe_Error ec = MPE_EINVAL;
    MPE_UNUSED_PARAM(obj);

    if (NULL != jPath && NULL != (cPath = (*env)->GetStringUTFChars(env, jPath,
            NULL)))
    {
        if ((ec = mpe_fileOpen(cPath, MPE_FS_OPEN_READ, &fh))
                == MPE_FS_ERROR_SUCCESS)
            mpe_fileClose(fh);
        else if ((ec = mpe_dirOpen(cPath, &dh)) == MPE_FS_ERROR_SUCCESS)
            mpe_dirClose(dh);
        else if ((ec = mpe_streamOpen(cPath, &sh)) == MPE_FS_ERROR_SUCCESS)
            (void) mpe_streamClose(sh);

        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    }
    /* Return result. */
    return ((ec == MPE_FS_ERROR_SUCCESS) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     org_cablelabs_impl_manager_filesys_OCFileSys
 * Method:    isFile
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_isFile(
        JNIEnv *env, jobject obj, jstring jPath)
{
    mpe_FileInfo info;
    const char *cPath;
    jboolean returnVal = JNI_FALSE;
    MPE_UNUSED_PARAM(obj);

    if (NULL != jPath && NULL != (cPath = (*env)->GetStringUTFChars(env, jPath,
            NULL)))
    {
        if (MPE_FS_ERROR_SUCCESS == mpe_fileGetStat(cPath, MPE_FS_STAT_TYPE,
                &info))
        {
            if (MPE_FS_TYPE_FILE == info.type)
                returnVal = JNI_TRUE;
        }
        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    }
    return returnVal;
}

/*
 * Class:     org_cablelabs_impl_manager_filesys_OCFileSys
 * Method:    lastModified
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jlong JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_lastModified(
        JNIEnv *env, jobject obj, jstring jPath)
{
    mpe_FileInfo info;
    const char *cPath;
    jlong returnVal = (jlong) 0;
    MPE_UNUSED_PARAM(obj);

    if (NULL != jPath && NULL != (cPath = (*env)->GetStringUTFChars(env, jPath,
            NULL)))
    {
        if (MPE_FS_ERROR_SUCCESS == mpe_fileGetStat(cPath, MPE_FS_STAT_MODDATE,
                &info))
            returnVal = (jlong) info.modDate; // OC return value is actually the version number.  Argh.

        (*env)->ReleaseStringUTFChars(env, jPath, cPath);
    }
    return returnVal;
}

/**
 * Convert an MPE error code into a Java IOException.
 * This should only be used by a: methods which actually throw IOException, and b: are part of the load flow.
 * May need to expand this to other methods, but currently, just these.
 */
static void throwException(JNIEnv *env, mpe_Error rc)
{
    char *className = NULL;

    switch (rc)
    {
    case MPE_FS_ERROR_FAILURE:
    case MPE_FS_ERROR_DEVICE_FAILURE:
    case MPE_FS_ERROR_INVALID_STATE:
    case MPE_FS_ERROR_UNSUPPORT:
    case MPE_FS_ERROR_DISCONNECTED:
        className = "org/dvb/dsmcc/MPEGDeliveryException";
        break;
    case MPE_FS_ERROR_NOT_FOUND:
    case MPE_FS_ERROR_INVALID_TYPE:
        className = "java/io/FileNotFoundException";
        break;
    case MPE_FS_ERROR_INVALID_DATA:
        className = "org/dvb/dsmcc/InvalidFormatException";
        break;
    case MPE_FS_ERROR_SERVICEXFER:
        className = "org/dvb/dsmcc/ServiceXFRException";
        break;
    default:
        // Uh?????
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_JNI,
                "OCFileSys(native): Error doesn't match expected exceptions: %04x\n",
                rc);
        className = "java/io/IOException";
        break;
    }
    MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI,
            "OCFileSys(native): Throwing exception %s for error %04x\n",
            className, rc);
    (*env)->ThrowNew(env, (*env)->FindClass(env, className),
            "Error occurred while loading file");
}

/*
 * Class:     org_cablelabs_impl_manager_filesys_OCFileSys
 * Method:    fileType
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL
Java_org_cablelabs_impl_manager_filesys_OCFileSys_fileType(JNIEnv *env, jobject obj, jstring jpath)
{
    const char *cpath;
    mpe_FileInfo info;
    jint returnVal = 0;
    mpe_Error retCode;
    MPE_UNUSED_PARAM(obj);

    if ((cpath = (*env)->GetStringUTFChars(env, jpath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return returnVal;
    }

    retCode = mpe_fileGetStat(cpath, MPE_FS_STAT_TYPE, &info);
    if (retCode == MPE_FS_ERROR_SUCCESS)
    {
        switch (info.type)
        {
            case MPE_FS_TYPE_FILE:
            returnVal = org_cablelabs_impl_manager_filesys_OCFileSys_TYPE_FILE;
            break;
            case MPE_FS_TYPE_DIR:
            returnVal = org_cablelabs_impl_manager_filesys_OCFileSys_TYPE_DIR;
            break;
            case MPE_FS_TYPE_STREAM:
            returnVal = org_cablelabs_impl_manager_filesys_OCFileSys_TYPE_STREAM;
            break;
            case MPE_FS_TYPE_STREAMEVENT:
            returnVal = org_cablelabs_impl_manager_filesys_OCFileSys_TYPE_STREAMEVENT;
            break;
            default:
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_JNI, "OCFileSys(native): Unknown filetype: 0x%04x\n", info.type);
            break;
        }
    }
    else
    {
        throwException(env, retCode);
    }
    /* return allocated memory */
    (*env)->ReleaseStringUTFChars(env, jpath, cpath);

    return returnVal;
}

/*
 * Class:     org_cablelabs_impl_manager_filesys_OCFileSys
 * Method:    contentType
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cablelabs_impl_manager_filesys_OCFileSys_contentType(
        JNIEnv *env, jobject obj, jstring jpath)
{
    const char *cpath;
    mpe_FileInfo info;
    jstring retString = NULL;
    mpe_Error retCode;
    uint8_t buffer[256];

    MPE_UNUSED_PARAM(obj);

    if ((cpath = (*env)->GetStringUTFChars(env, jpath, NULL)) == NULL)
    {
        /* GetStringUTFChars threw a memory exception */
        return NULL;
    }

    info.buf = buffer;
    info.size = 256;
    retCode = mpe_fileGetStat(cpath, MPE_FS_STAT_CONTENTTYPE, &info);
    if (retCode == MPE_FS_ERROR_SUCCESS)
    {
        if (strlen(info.buf) > 0)
        {
            retString = (*env)->NewStringUTF(env, (const char *) buffer);
        }
    }
    (*env)->ReleaseStringUTFChars(env, jpath, cpath);

    return retString;
}
