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

#include <stddef.h>
#include <string.h>

#include "mgrdef.h"
#include "mpe_file.h"
#include "mpeos_file.h"
#include "mpe_file_romfs.h"

#include "mpeos_dbg.h"
#include "mpeos_mem.h"
#include "mpeos_sync.h"
#include "mpeos_dll.h"
#include "mpeos_util.h"

typedef struct RomfsDevHandle
{
    mpe_fileRomfsDirEnt *root;
    mpe_Mutex mutex;
} RomfsDevHandle;

typedef struct RomfsHandle
{
    uint32_t pos;
    mpe_fileRomfsDirEnt *entry;
    mpe_Dlmod dlmodId;
} RomfsHandle;

static RomfsDevHandle mpe_fileRomfsDevHandle[] =
{
{ 0 } };

#define OS_FS_ROMFS_ROOTDIRENTRY      mpe_fileRomfsDirEnt0
#define OS_FS_ROMFS_ROOTDIRENTRY_NAME "mpe_fileRomfsDirEnt0"
extern mpe_fileRomfsDirEnt OS_FS_ROMFS_ROOTDIRENTRY;

#define OS_FS_ENV_ROMFS_LIB_DIR       "FS.ROMFS.LIBDIR"

typedef struct RomfsDllEntry
{
    uint32_t refs;
    mpe_Dlmod dlmodId;
    mpe_fileRomfsDirEnt *rootDir;
    char *name;
} RomfsDllEntry;

#define ROMFS_DLL_CACHE_NUM      (100)
#define ROMFS_DLL_CACHE_REFS_MAX (0xffffffff)

static RomfsDllEntry RomfsDllCache[ROMFS_DLL_CACHE_NUM];

static mpe_FileError romfsInit(RomfsDevHandle *devHandle,
        mpe_fileRomfsDirEnt *rootDir)
{
    int i;

    // clear device handle
    devHandle->root = rootDir;
    devHandle->mutex = NULL;

    // clear out the internal cache structure
    for (i = 0; i < ROMFS_DLL_CACHE_NUM; i++)
    {
        RomfsDllCache[i].refs = 0;
        RomfsDllCache[i].dlmodId = NULL;
        RomfsDllCache[i].rootDir = NULL;
        RomfsDllCache[i].name = NULL;
    }

    // create the multithread protections mutex
    if (mpeos_mutexNew(&devHandle->mutex) != MPE_SUCCESS)
    {
        return MPE_FS_ERROR_OUT_OF_MEM;
    }

    return MPE_FS_ERROR_SUCCESS;
}

/* path independent limited string compare
 ** (eg, treat '/' & '\' the same)
 **
 ** return:
 **    0 : strings are equal
 **   -1 : string1 is "greater than" string 2
 **    1 : string2 is "greater than" string 1
 */
static int path_strncmp(const char *str1, const char *str2, int len)
{
    char *p1;
    char *p2;

    if (str1 == str2)
    {
        return (0);
    }
    if (str1 == NULL)
    {
        return (1);
    }
    if (str2 == NULL)
    {
        return (-1);
    }

    // cycle thru 'len' characters in the strings checking for a match
    p1 = (char*) str1;
    p2 = (char*) str2;
    while ((len) && (*p1) && (*p2))
    {
        // do these characters match
        if ((*p1) != (*p2))
        {
            // special case for path separator chars (treat '/' & '\' as equals)
            if (!((((*p1) == '/') || ((*p1) == '\\')) && (((*p2) == '/')
                    || ((*p2) == '\\'))))
            {
                // strings don't match
                return (((*p1) > (*p2)) ? 1 : -1);
            }
        }

        // check for end-of-string terminator before incrementing pointer
        if (((*p1) == 0) || ((*p2) == 0))
        {
            if ((*p1) == (*p2))
            {
                // strings do match
                return (0);
            }
            else
            {
                // strings don't match
                return (((*p1) > (*p2)) ? 1 : -1);
            }
        }

        // go to next character
        len--;
        p1++;
        p2++;
    }

    // if we got this far, the strings are equal
    return (0);
}

static mpe_FileError romfsFindDirEnt(RomfsDevHandle *devHandle,
        const char *name, uint8_t type, RomfsHandle **handleReturned)
{
    mpe_FileError err;
    uint32_t nameLen;

    mpe_fileRomfsDirEnt *curDir = NULL;
    RomfsDllEntry *cachedRomfsDll = NULL;
    RomfsDllEntry *curRomfsDll = NULL;
    mpe_Dlmod dlmodId = NULL;

    // initialize return parameters
    *handleReturned = NULL;

    // is this a directory path with a trailing slash?
    nameLen = strlen(name);
    if (nameLen > 0)
    {
        char lastChar = name[nameLen - 1];
        if ((lastChar == '/') || (lastChar == '\\'))
        {
            nameLen--;
        }
    }

    // only look for DLL if we aren't looking for the directory "","/","\\"
    // (ie, the RomFS root)
    if (((*name) != 0) && ((*name) != '/') && ((*name) != '\\'))
    {
        int i;
        uint32_t dirSize;
        const char *end;

        // create stand-along base directory string
        end = name;
        while ((*end != 0) && (*end != '/') && (*end != '\\'))
        {
            end++;
        }
        dirSize = (end - name);

        // have we already cached this DLL?
        cachedRomfsDll = NULL;
        for (i = 0; i < ROMFS_DLL_CACHE_NUM; i++)
        {
            if ((RomfsDllCache[i].name != NULL) && (strncmp(name,
                    RomfsDllCache[i].name, dirSize) == 0))
            {
                // found a match
                if (RomfsDllCache[i].refs < ROMFS_DLL_CACHE_REFS_MAX)
                {
                    RomfsDllCache[i].refs++;
                }
                cachedRomfsDll = &RomfsDllCache[i];
                curDir = RomfsDllCache[i].rootDir;
                break;
            }
        }

        // if we didn't find a cached dll entry, try to load this as a dll
        if ((curDir == NULL) && (cachedRomfsDll == NULL))
        {
            uint32_t minRef;

            // find an empty cache dll entry (or least used one)
            curRomfsDll = &RomfsDllCache[0];
            minRef = 0;
            for (i = 0; i < ROMFS_DLL_CACHE_NUM; i++)
            {
                if (RomfsDllCache[i].name == NULL)
                {
                    // found an empty entry, so use it now
                    curRomfsDll = &RomfsDllCache[i];
                    break;
                }
                if (RomfsDllCache[i].refs > minRef)
                {
                    // this is the least-used entry found so far
                    curRomfsDll = &RomfsDllCache[i];
                    minRef = RomfsDllCache[i].refs;
                }
            }

            // if this entry is still in use, empty it
            if (curRomfsDll->name != NULL)
            {
                (void) mpeos_memFreeP(MPE_MEM_FILE, curRomfsDll->name);
                curRomfsDll->name = NULL;
            }
            if (curRomfsDll->dlmodId != NULL)
            {
                (void) mpeos_dlmodClose(curRomfsDll->dlmodId);
                curRomfsDll->dlmodId = NULL;
            }
            curRomfsDll->rootDir = NULL;
            curRomfsDll->refs = 1;

            // save off this directory name
            if (mpeos_memAllocP(MPE_MEM_FILE, (dirSize + 1),
                    (void**) &curRomfsDll->name) != MPE_SUCCESS)
            {
                // couldn't allocate name string, so didn't look for an empty cache entry
                curRomfsDll = NULL;
            }
            else
            {
                uint32_t libPathLen;
                char *dllName = NULL;
                const char *dllLib;

                // save off the directory name
                curRomfsDll->name[0] = 0;
                strncat(curRomfsDll->name, name, dirSize);

                // try to get environment variable with DLL path location
                libPathLen = 0;
                if ((dllLib = mpeos_envGet(OS_FS_ENV_ROMFS_LIB_DIR)) != NULL)
                {
                    libPathLen = strlen(dllLib); /* Get length of lib path. */
                }

                // allocate path string buffer
                if (mpeos_memAllocP(MPE_MEM_FILE, (dirSize
                        + sizeof(OS_FS_ROMFS_DLL_PREFIX)
                        + sizeof(OS_FS_ROMFS_DLL_SUFFIX) + libPathLen + 1),
                        (void**) &dllName) == MPE_SUCCESS)
                {
                    dllName[0] = 0;

                    // first put in the lib path (if it exists)
                    if (NULL != dllLib)
                    {
                        // add lib path, then add in a directory separator
                        strcpy(dllName, dllLib);
                        strcat(dllName, MPE_FS_SEPARATION_STRING);
                    }

                    // now add in file prefix & file name & file suffix
                    strcat(dllName, OS_FS_ROMFS_DLL_PREFIX);
                    strncat(dllName, name, dirSize);
                    strcat(dllName, OS_FS_ROMFS_DLL_SUFFIX);

                    // try to link to a DLL with the same name as the base directory
                    if (mpeos_dlmodOpen(dllName, &dlmodId) == MPE_SUCCESS)
                    {
                        // try to grab the top-level RomFS directory entry within this DLL
                        if (mpeos_dlmodGetSymbol(dlmodId,
                                OS_FS_ROMFS_ROOTDIRENTRY_NAME, (void**) &curDir)
                                == MPE_SUCCESS)
                        {
                            // add this entry to the dll cache
                            curRomfsDll->dlmodId = dlmodId;
                            curRomfsDll->rootDir = curDir;
                        }
                        else
                        {
                            (void) mpeos_dlmodClose(dlmodId);
                            curDir = NULL;
                        }
                    }

                    // free up the allocated path string buffer
                    (void) mpeos_memFreeP(MPE_MEM_FILE, dllName);
                }
            }
        }
    }

    // if not, try to look in the compiled-in RomFS image
    if ((curDir == NULL) && (cachedRomfsDll == NULL))
    {
        curDir = devHandle->root;
        if (curRomfsDll != NULL)
        {
            curRomfsDll->rootDir = curDir;
        }
    }

    // now look around in the current directory structure for this path
    while (curDir)
    {
        // is this entry a subdirectory or file of the target
        uint32_t curNameLen = strlen(curDir->name);
        if ((nameLen >= curNameLen) && (path_strncmp(name, curDir->name,
                curNameLen) == 0))
        {
            if (nameLen == curNameLen)
            {
                // found the matching file
                break;
            }
            else
            {
                // follow this subdirectory
                curDir
                        = (curDir->type == MPE_FS_TYPE_DIR) ? (mpe_fileRomfsDirEnt*) curDir->data
                                : NULL;
            }
        }
        else
        {
            // look at the next entry in this directory
            curDir = curDir->next;
        }
    }

    // check to see if we found a matching entry of the correct type
    if ((curDir) && (curDir->type == type))
    {
        // create handle to this directory entry to be returned
        RomfsHandle *romfsHandle;
        if (mpeos_memAllocP(MPE_MEM_FILE, sizeof(RomfsHandle),
                (void**) &romfsHandle) != MPE_SUCCESS)
        {
            err = MPE_FS_ERROR_OUT_OF_MEM;
        }
        else
        {
            // fill out the handle
            romfsHandle->pos = 0;
            romfsHandle->entry = curDir;
            romfsHandle->dlmodId = dlmodId;
            *handleReturned = romfsHandle;
            //mpeos_dbgMsg("     FindDirEnt('%s') - success\n",name);
            err = MPE_FS_ERROR_SUCCESS;
        }
    }
    else
    {
        //mpeos_dbgMsg("     FindDirEnt('%s') - error: not found\n",name);
        err = MPE_FS_ERROR_NOT_FOUND;
    }

    return err;
}

static mpe_FileError romfsFileOpen(RomfsDevHandle *devHandle,
        const char *fileName, RomfsHandle **fileHandleReturned)
{
    mpe_FileError err;

    mpeos_mutexAcquire(devHandle->mutex);
    {
        err = romfsFindDirEnt(devHandle, fileName, MPE_FS_TYPE_FILE,
                fileHandleReturned);
        if (err != MPE_FS_ERROR_SUCCESS)
        {
            *fileHandleReturned = NULL;
        }
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

static mpe_FileError romfsFileClose(RomfsDevHandle *devHandle,
        RomfsHandle *fileHandle)
{
    mpe_FileError err;

    mpeos_mutexAcquire(devHandle->mutex);
    {
        (void) mpeos_memFreeP(MPE_MEM_FILE, fileHandle);
        err = MPE_FS_ERROR_SUCCESS;
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

static mpe_FileError romfsFileRead(RomfsDevHandle *devHandle,
        RomfsHandle *fileHandle, uint32_t* count, void* buffer)
{
    mpe_FileError err;

    // acquire mutex since we're mucking on potentially sharable 'pos'
    mpeos_mutexAcquire(devHandle->mutex);
    {
        // are we already at the end-of-file
        if (fileHandle->pos >= fileHandle->entry->size)
        {
            *count = 0;
            err = MPE_FS_ERROR_SUCCESS;
        }
        else
        {
            uint8_t *from;
            // copy <count> bytes into caller's <buffer> (but only until end-of-file)
            if ((*count) > (fileHandle->entry->size - fileHandle->pos))
            {
                (*count) = (fileHandle->entry->size - fileHandle->pos);
            }
            from = (uint8_t*) (fileHandle->entry->data);
            memcpy(buffer, &from[fileHandle->pos], (*count));
            fileHandle->pos += (*count);
            err = MPE_FS_ERROR_SUCCESS;
        }
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

static mpe_FileError romfsFileSeek(RomfsDevHandle *devHandle,
        RomfsHandle *fileHandle, mpe_FileSeekMode seekMode, int32_t* offset)
{
    mpe_FileError err;

    int32_t newOffset;
    switch (seekMode)
    {
    case MPE_FS_SEEK_CUR:
        newOffset = fileHandle->pos + (*offset);
        break;
    case MPE_FS_SEEK_END:
        newOffset = fileHandle->entry->size + (*offset);
        break;
    case MPE_FS_SEEK_SET:
        newOffset = (*offset);
        break;
    default:
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // boundry check the resulting offset
    if (newOffset < 0)
    {
        // we can't seek beyond the beginning of the file
        newOffset = 0;
    }
    if (newOffset > (int32_t) fileHandle->entry->size)
    {
        // we can't seek beyond the end of the file
        newOffset = fileHandle->entry->size;
    }

    mpeos_mutexAcquire(devHandle->mutex);
    {
        // update the current file position
        (*offset) = newOffset;
        fileHandle->pos = newOffset;

        err = MPE_FS_ERROR_SUCCESS;
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

static mpe_FileError romfsFileGetSize(RomfsDevHandle *devHandle,
        RomfsHandle *fileHandle, uint32_t *size)
{
    mpe_FileError err;

    mpeos_mutexAcquire(devHandle->mutex);
    {
        *size = fileHandle->entry->size;
        err = MPE_FS_ERROR_SUCCESS;
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

static mpe_FileError romfsDirOpen(RomfsDevHandle *devHandle,
        const char* dirName, RomfsHandle **dirHandleReturned)
{
    mpe_FileError err;

    mpeos_mutexAcquire(devHandle->mutex);
    {
        err = romfsFindDirEnt(devHandle, dirName, MPE_FS_TYPE_DIR,
                dirHandleReturned);
        if (err != MPE_FS_ERROR_SUCCESS)
        {
            *dirHandleReturned = NULL;
        }
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

static mpe_FileError romfsDirClose(RomfsDevHandle *devHandle,
        RomfsHandle *dirHandle)
{
    mpe_FileError err;

    mpeos_mutexAcquire(devHandle->mutex);
    {
        (void) mpeos_memFreeP(MPE_MEM_FILE, dirHandle);
        err = MPE_FS_ERROR_SUCCESS;
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

static mpe_FileError romfsDirRead(RomfsDevHandle *devHandle,
        RomfsHandle *dirHandle, mpe_DirEntry* dirEnt)
{
    mpe_FileError err = MPE_FS_ERROR_NOT_FOUND;
    const char * sPos;

    mpeos_mutexAcquire(devHandle->mutex);
    {
        uint32_t pos = 0;
        const mpe_fileRomfsDirEnt * curEntry = dirHandle->entry->data;
        while (curEntry)
        {
            // is this the next entry
            if (pos == dirHandle->pos)
            {
                // return this entry info
                sPos = curEntry->name + strlen(curEntry->name);
                // strip the path name
                while (sPos != curEntry->name)
                {
                    sPos--;
                    if (*sPos == '/')
                    {
                        sPos++;
                        break;
                    }
                }

                strcpy(dirEnt->name, sPos);
                dirEnt->fileSize = curEntry->size;
                dirEnt->isDir = (uint8_t)(curEntry->type == MPE_FS_TYPE_DIR);

                dirHandle->pos++;
                err = MPE_FS_ERROR_SUCCESS;
                break;
            }

            // go to the next directory entry
            pos++;
            curEntry = curEntry->next;
        }
    }
    mpeos_mutexRelease(devHandle->mutex);

    return err;
}

/**
 * <i>mpe_fileRomfsDirOpen(const char* dirName, mpe_Dir* dirPathReturned)</i>
 *
 * Opens a directory in RomFS storage.
 *
 * @param dirName The path to the directory to open.
 * @param dirHandleReturned A pointer to an mpe_fileRomfsDir handle, through which the opened directory is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirOpen(const char* dirName,
        mpe_Dir* dirHandleReturned)
{
    mpe_FileError err;
    RomfsHandle *romfsDirHandle;

    // parameter checking
    if ((dirName == NULL) || (dirHandleReturned == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    *dirHandleReturned = NULL;

    // ignore any leading slashes in the pathname
    if ((dirName[0] == '/') || (dirName[0] == '\\'))
    {
        dirName++;
    }

    err = romfsDirOpen(mpe_fileRomfsDevHandle, dirName, &romfsDirHandle);

    *dirHandleReturned = (mpe_Dir) romfsDirHandle;
    return err;
}

/**
 * <i>mpe_fileRomfsDirClose(mpe_Dir dirHandle)</i>
 *
 * Closes a directory in RomFS storage.
 *
 * @param dirHandle A mpe_fileRomfsDir handle, previously returned by <i>mpe_fileRomfsDirOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirClose(mpe_Dir dirHandle)
{
    mpe_FileError err;
    RomfsHandle *romfsDirHandle = (RomfsHandle*) dirHandle;

    // parameter checking
    if (dirHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    err = romfsDirClose(mpe_fileRomfsDevHandle, romfsDirHandle);

    return err;
}

/**
 * <i>mpe_fileRomfsDirRead(mpe_Dir dirHandle, mpe_DirEntry* dirEnt)</i>
 *
 * Reads the contents of a directory in RomFS storage.  This can be used to iterate
 *  through the contents a directory in RomFS storage.
 *
 * @param dirHandle A mpe_fileRomfsDir handle, previously returned by <i>mpe_fileRomfsDirOpen()</i>.
 * @param dirEnt A pointer to a mpe_fileRomfsDirEntry object.  On return, this will contain
 *                  data about a directory entry.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirRead(mpe_Dir dirHandle,
        mpe_DirEntry* dirEnt)
{
    mpe_FileError err;
    RomfsHandle *romfsDirHandle = (RomfsHandle*) dirHandle;

    // parameter checking
    if ((dirHandle == NULL) || (dirEnt == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    err = romfsDirRead(mpe_fileRomfsDevHandle, romfsDirHandle, dirEnt);

    return err;
}

/**
 * <i>mpe_fileRomfsDirDelete(const char* dirName)</i>
 *
 * Deletes a directory from RomFS storage.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param dirName The path to the directory to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirDelete(const char* dirName)
{
    MPE_UNUSED_PARAM(dirName);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>mpe_fileRomfsDirRename(const char* dirName, const char* newDirName)</i>
 *
 * Renames or moves the specific directory in RomFS storage.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param dirName The path to the directory to rename or move.
 * @param newDirName The new path and/or name for the directory.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirRename(const char* dirName,
        const char* newDirName)
{
    MPE_UNUSED_PARAM(dirName);
    MPE_UNUSED_PARAM(newDirName);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>mpe_fileRomfsDirCreate(const char* dirName)</i>
 *
 * Creates the specific directory in RomFS storage.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param dirName The path to the directory to create.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirCreate(const char* dirName)
{
    MPE_UNUSED_PARAM(dirName);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>mpe_FileError mpe_fileRomfsDirMount(const mpe_DirUrl *dirUrl);</i>
 *
 * Mount the indicated URL on the optional (non '-1) indicated RomFS id
 * into the object RomFS file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirMount(const mpe_DirUrl *dirUrl)
{
    MPE_UNUSED_PARAM(dirUrl);

    return MPE_FS_ERROR_UNSUPPORT;
}

/**
 * <i>mpe_FileError mpe_fileRomfsDirUnmount(const mpe_DirUrl *dirUrl);</i>
 *
 * Unmount the indicated URL from the object RomFS file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirUnmount(const mpe_DirUrl *dirUrl)
{
    MPE_UNUSED_PARAM(dirUrl);

    return MPE_FS_ERROR_UNSUPPORT;
}

/**
 * <i>mpe_FileError mpe_fileRomfsDirGetUStat(const mpe_DirUrl *dirUrl, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
 *
 * Retrieve some status info on a directory object.
 *
 * @param dirUrl The URL to the directory on which to update its status information.
 * @param mode The specific directory stat to get.
 * @param info A pointer to the buffer in which to return the indicated directory stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirGetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    MPE_UNUSED_PARAM(dirUrl);
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);

    return MPE_FS_ERROR_UNSUPPORT;
}

/**
 * <i>mpe_FileError mpe_fileRomfsDirSetUStat(const mpe_DirUrl *dirUrl, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
 *
 * Set some status info on a directory object.
 *
 * @param dirUrl The URL to the directory on which to update its status information.
 * @param mode The specific directory stat to get.
 * @param info A pointer to the buffer in which to return the indicated directory stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsDirSetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    MPE_UNUSED_PARAM(dirUrl);
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);

    return MPE_FS_ERROR_UNSUPPORT;
}

/**
 * <i>mpe_fileRomfsInit(const char* mountPoint)</i>
 *
 * Initializes the Win32 Simulator RomFS file system.
 *
 * @param mountPoint The file-system namespace in which this driver is mounted.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static void mpe_fileRomfsInit(const char* mountPoint)
{
    mpe_FileError err;

    MPE_UNUSED_PARAM(mountPoint);

    // initialize the RomFS with the loaded file
    err = romfsInit(mpe_fileRomfsDevHandle, &OS_FS_ROMFS_ROOTDIRENTRY);
    if (err != MPE_FS_ERROR_SUCCESS)
    {
        // what to do on errors (anything?)
    }
}

/**
 * <i>mpe_fileRomfsFileOpen(const char* fileName, mpe_FileOpenMode openMode, mpe_File *fileHandleReturned)</i>
 *
 * Opens a file from the RomFS file system.
 *
 * @param fileName The path to the file to open.
 * @param openMode The type of access requested (read, write, create, truncate, append).
 * @param fileHandleReturned A pointer to the returned file handle to the opened file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileOpen(const char* fileName,
        mpe_FileOpenMode openMode, mpe_File *fileHandleReturned)
{
    mpe_FileError err;
    RomfsHandle *romfsFileHandle;

    // parameter checking
    if ((fileName == NULL) || (fileHandleReturned == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }
    if (openMode != MPE_FS_OPEN_READ)
    {
        return MPE_FS_ERROR_READ_ONLY;
    }

    *fileHandleReturned = NULL;

    // ignore any leading slashes in the pathname
    if ((fileName[0] == '/') || (fileName[0] == '\\'))
    {
        fileName++;
    }

    err = romfsFileOpen(mpe_fileRomfsDevHandle, fileName, &romfsFileHandle);

    *fileHandleReturned = (mpe_File) romfsFileHandle;
    return err;
}

/**
 * <i>mpe_fileRomfsFileClose(mpe_File fileHandle)</i>
 *
 * Closes a file previously opened with <i>mpe_fileRomfsFileOpen()</i>.
 *
 * @param fileHandle A mpe_fileRomfsFile handle, previously returned by <i>mpe_fileRomfsFileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileClose(mpe_File fileHandle)
{
    mpe_FileError err;
    RomfsHandle *romfsFileHandle = (RomfsHandle*) fileHandle;

    // parameter checking
    if (fileHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    err = romfsFileClose(mpe_fileRomfsDevHandle, romfsFileHandle);

    return err;
}

/**
 * <i>mpe_fileRomfsFileRead(mpe_File fileHandle, uint32_t* count, void* buffer)</i>
 *
 * Reads data from a file in RomFS storage.
 *
 * @param fileHandle A mpe_fileRomfsFile handle, previously returned by <i>mpe_fileRomfsFileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                  read.  On exit, this will indicate the number of bytes actually read.
 * @param buffer A pointer to a buffer to receive the data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileRead(mpe_File fileHandle,
        uint32_t* count, void* buffer)
{
    mpe_FileError err;
    RomfsHandle *romfsFileHandle = (RomfsHandle*) fileHandle;

    // parameter checking
    if ((fileHandle == NULL) || (count == NULL) || (buffer == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    if ((*count) == 0)
    {
        // read 0 bytes? ok, we did!
        return MPE_FS_ERROR_SUCCESS;
    }

    err = romfsFileRead(mpe_fileRomfsDevHandle, romfsFileHandle, count, buffer);

    return err;
}

/**
 * <i>mpe_fileRomfsFileWrite(mpe_File fileHandle, uint32_t* count, void* buffer)</i>
 *
 * Writes data to a file in RomFS storage.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param fileHandle A mpe_fileRomfsFile handle, previously returned by <i>mpe_fileRomfsFileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                  write.  On exit, this will indicate the number of bytes actually written.
 * @param buffer A pointer to a buffer containing the data to send.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileWrite(mpe_File fileHandle,
        uint32_t* count, void* buffer)
{
    MPE_UNUSED_PARAM(fileHandle);
    MPE_UNUSED_PARAM(count);
    MPE_UNUSED_PARAM(buffer);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>mpe_fileRomfsFileSeek(mpe_File fileHandle, mpe_FileSeekMode seekMode, int64_t* offset)</i>
 *
 * Changes and reports the current position within a file in RomFS storage.
 *
 * @param fileHandle A mpe_fileRomfsFile handle, previously returned by <i>mpe_fileRomfsFileOpen()</i>.
 * @param seekMode A seek mode constant indicating whether the offset value should be considered
 *                  relative to the start, end, or current position within the file.
 * @param offset A pointer to a file position offset.  On entry, this should indicate the number
 *                  of bytes to seek, offset from the seekMode.  On exit, this will indicate the
 *                  new absolute position within the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileSeek(mpe_File fileHandle,
        mpe_FileSeekMode seekMode, int64_t* offset)
{
    mpe_FileError err;
    int32_t offset32 = (int32_t)(*offset);
    RomfsHandle *romfsFileHandle = (RomfsHandle*) fileHandle;

    // parameter checking
    if ((fileHandle == NULL) || (offset == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // check for out-of-range offset (bigger than i32)
    if (*offset > 0x7fffffff)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    err = romfsFileSeek(mpe_fileRomfsDevHandle, romfsFileHandle, seekMode,
            &offset32);

    (*offset) = (int64_t) offset32;
    return err;
}

/**
 * <i>mpe_fileRomfsFileSync(mpe_File fileHandle)</i>
 *
 * Synchronizes the contents of a file in RomFS storage.  This will write any data that is
 *  pending.  Pending data is data that has been written to a file, but which hasn't been flushed
 *  to the storage device yet.
 *
 * @param fileHandle A mpe_fileRomfsFile handle, previously returned by <i>mpe_fileRomfsFileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileSync(mpe_File fileHandle)
{
    MPE_UNUSED_PARAM(fileHandle);

    // nothing to really sync, so just return success
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpe_fileRomfsFileGetFStat(mpe_File fileHandle, mpe_FileStatMode mode, mpe_FileInfo *info)</i>
 *
 * Get the status for the open file.
 *
 * @param fileHandle An mpe_File handle, previously returned by <i>mpe_fileRomfsFileOpen()</i>.
 * @param mode The specific status of the file to return (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer in which to receive the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileGetFStat(mpe_File fileHandle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err;

    if (fileHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    switch (mode)
    {
    // return file size (end-of-file)
    case MPE_FS_STAT_SIZE:
    {
        RomfsHandle *romfsFileHandle = (RomfsHandle*) fileHandle;
        uint32_t fileSize;

        // parameter checking
        if (info == NULL)
        {
            return MPE_FS_ERROR_INVALID_PARAMETER;
        }

        err = romfsFileGetSize(mpe_fileRomfsDevHandle, romfsFileHandle,
                &fileSize);
        info->size = fileSize;
        break;
    }

    case MPE_FS_STAT_ISKNOWN:
    {
        // if we can open the file assume we can read the directories
        info->isKnown = TRUE;
        err = MPE_FS_ERROR_SUCCESS;
        break;
    }

    case MPE_FS_STAT_TYPE:
        // already opened as a file
        info->type = MPE_FS_TYPE_FILE;
        err = MPE_FS_ERROR_SUCCESS;
        break;

        // unknown getstat call
    default:
    {
        err = MPE_FS_ERROR_INVALID_PARAMETER;
        break;
    }
    }

    // cleanup & exit
    return err;
}

/**
 * <i>mpe_fileRomfsFileGetStat(const char* fileName, mpe_FileStatMode mode, mpe_FileInfo *info)</i>
 *
 * Get the status for the open file.
 *
 * @param fileName The pathlist to the file of which to obtain the status.
 * @param mode The specific status of the file to return (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer in which to receive the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileGetStat(const char* fileName,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err;
    mpe_File handle;

    // ignore any leading slashes in the pathname
    if ((fileName[0] == '/') || (fileName[0] == '\\'))
    {
        fileName++;
    }

    // open a handle to the requested file (for now)
    err = mpe_fileRomfsFileOpen(fileName, MPE_FS_OPEN_READ, &handle);
    if (err != MPE_FS_ERROR_SUCCESS)
    {
        handle = NULL;
    }

    switch (mode)
    {
    case MPE_FS_STAT_CREATEDATE:
    case MPE_FS_STAT_MODDATE:
        err = MPE_FS_ERROR_UNSUPPORT;
        break;

        // return file known (parent directory loaded) status
    case MPE_FS_STAT_ISKNOWN:
    {
        // was file opened?
        if (handle == NULL)
        {
            // no, so break with open error
            err = MPE_FS_ERROR_NOT_FOUND;
            break;
        }

        // if we can open the file assume we can read the directories
        info->isKnown = TRUE;
        break;
    }

    case MPE_FS_STAT_TYPE:
    {
        mpe_Dir dirHandle;
        if (handle != NULL)
        {
            // already opened as a file
            info->type = MPE_FS_TYPE_FILE;
        }
        else if ((err = mpe_fileRomfsDirOpen(fileName, &dirHandle))
                == MPE_FS_ERROR_SUCCESS)
        {
            // opened as a directory
            info->type = MPE_FS_TYPE_DIR;
            (void) mpe_fileRomfsDirClose(dirHandle);
        }
        else
        {
            err = MPE_FS_ERROR_NOT_FOUND;
        }
        break;
    }
    default:
    {
        // call GetFStat
        err = mpe_fileRomfsFileGetFStat(handle, mode, info);
        break;
    }
    }

    if (handle != NULL)
    {
        (void) mpe_fileRomfsFileClose(handle);
    }
    return (err);
}

/**
 * <i>mpe_fileRomfsFileSetFStat(mpe_File fileHandle, mpe_FileStatMode mode, mpe_FileInfo *info)</i>
 *
 * Set the status for the open file.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param fileHandle An mpe_File handle, previously returned by <i>mpe_fileRomfsFileOpen()</i>.
 * @param mode The specific status of the file to set (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer with which to set the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileSetFStat(mpe_File fileHandle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    MPE_UNUSED_PARAM(fileHandle);
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>mpe_fileRomfsFileSetStat(const char* name, mpe_FileStatMode mode, mpe_FileInfo *info)</i>
 *
 * Set the status for the open file.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param name The pathlist to the file of which to obtain the status.
 * @param mode The specific status of the file to set (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer with which to set the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileSetStat(const char* name,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    MPE_UNUSED_PARAM(name);
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>mpe_fileRomfsFileDelete(const char* fileName)</i>
 *
 * Deletes the specific file from the RomFS storage file system.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param fileName The path to the file to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileDelete(const char* fileName)
{
    MPE_UNUSED_PARAM(fileName);

    return MPE_FS_ERROR_READ_ONLY;
}

/**
 * <i>mpe_fileRomfsFileRename(const char* fileName, const char* newFileName)</i>
 *
 * Renames or moves the specific file in RomFS storage.
 * NOTE: Currently, RomFS is a read-only file-system, so this call will always
 *       return MPE_FS_ERROR_READ_ONLY.
 *
 * @param fileName The path to the file to rename or move.
 * @param newFileName The new path and/or name for the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
static mpe_FileError mpe_fileRomfsFileRename(const char* fileName,
        const char* newFileName)
{
    MPE_UNUSED_PARAM(fileName);
    MPE_UNUSED_PARAM(newFileName);

    return MPE_FS_ERROR_READ_ONLY;
}

/* file system driver entry-point function table */
mpeos_filesys_ftable_t mpe_fileRomfsFTable =
{ mpe_fileRomfsInit, mpe_fileRomfsFileOpen, mpe_fileRomfsFileClose,
        mpe_fileRomfsFileRead, mpe_fileRomfsFileWrite, mpe_fileRomfsFileSeek,
        mpe_fileRomfsFileSync, mpe_fileRomfsFileGetStat,
        mpe_fileRomfsFileSetStat, mpe_fileRomfsFileGetFStat,
        mpe_fileRomfsFileSetFStat, mpe_fileRomfsFileDelete,
        mpe_fileRomfsFileRename, mpe_fileRomfsDirOpen, mpe_fileRomfsDirRead,
        mpe_fileRomfsDirClose, mpe_fileRomfsDirDelete, mpe_fileRomfsDirRename,
        mpe_fileRomfsDirCreate, mpe_fileRomfsDirMount, mpe_fileRomfsDirUnmount,
        mpe_fileRomfsDirGetUStat, mpe_fileRomfsDirSetUStat };
