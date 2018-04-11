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

#include <stdlib.h>
#include <string.h>

#include <mpe_file.h>
#include <mpe_dbg.h>
#include <mpe_error.h>
#include <mpe_file_romfs.h>

#include <mpeos_file.h>
#include <mpeos_mem.h>
#include <mpeos_event.h>

#include <sysmgr.h>
#include <filesysmgr.h>

#include <jni.h>

#include "ObjectCarousel/mpe_file_oc.h"

#define SYSCWD  "/syscwd"   /* absolute "mount" path for system current-working-directory */

typedef struct filesys_mountTable
{
    struct filesys_mountTable *next;
    mpeos_filesys_ftable_t *ftable;
    char *mountPoint;
} filesys_mountTable;

typedef struct filesys_asyncCallbackTable
{
    struct filesys_asyncCallbackTable *next;
    void *data;
    void (*func)(void *data);
} filesys_asyncCallbackTable;

typedef struct filesys_File
{
    mpe_File handle;
    filesys_mountTable *mount;
} filesys_File;

typedef struct filesys_Dir
{
    mpe_Dir handle;
    filesys_mountTable *mount;
} filesys_Dir;

typedef struct filesys_ChangeHandle
{
    mpe_FileChangeHandle handle;
    filesys_mountTable *mount;
} filesys_ChangeHandle;

typedef struct filesys_Stream
{
    mpe_Stream handle;
    filesys_mountTable *mount;
} filesys_Stream;

static filesys_mountTable *filesysMounts = NULL;

/* public functions */
void mpe_filesysSetup(void);

/* public functions (called thru the file system function table) */
static void filesys_init(void);
static mpe_FileError filesys_fileOpen(const char* fileName,
        mpe_FileOpenMode openMode, mpe_File* fileHandleBuffer);
static mpe_FileError filesys_fileClose(mpe_File fileHandle);
static mpe_FileError filesys_fileRead(mpe_File fileHandle, uint32_t* count,
        void* buffer);
static mpe_FileError filesys_fileWrite(mpe_File fileHandle, uint32_t* count,
        void* buffer);
static mpe_FileError filesys_fileSeek(mpe_File fileHandle,
        mpe_FileSeekMode seekMode, int64_t* offset);
static mpe_FileError filesys_fileSync(mpe_File fileHandle);
static mpe_FileError filesys_fileGetStat(const char* name,
        mpe_FileStatMode mode, mpe_FileInfo *info);
static mpe_FileError filesys_fileSetStat(const char* name,
        mpe_FileStatMode mode, mpe_FileInfo *info);
static mpe_FileError filesys_fileGetFStat(mpe_File fileHandle,
        mpe_FileStatMode mode, mpe_FileInfo *info);
static mpe_FileError filesys_fileSetFStat(mpe_File fileHandle,
        mpe_FileStatMode mode, mpe_FileInfo *info);
static mpe_FileError filesys_fileDelete(const char* fileName);
static mpe_FileError filesys_fileRename(const char* oldFileName,
        const char* newFileName);
static mpe_FileError filesys_dirOpen(const char* dirName,
        mpe_Dir* dirHandleBuffer);
static mpe_FileError filesys_dirRead(mpe_Dir dirHandle, mpe_DirEntry* dirEnt);
static mpe_FileError filesys_dirClose(mpe_Dir dirHandle);
static mpe_FileError filesys_dirDelete(const char* dirName);
static mpe_FileError filesys_dirRename(const char* oldDirName,
        const char* newDirName);
static mpe_FileError filesys_dirCreate(const char* dirName);
static mpe_FileError filesys_dirMount(const mpe_DirUrl *dirUrl);
static mpe_FileError filesys_dirUnmount(const mpe_DirUrl *dirUrl);
static mpe_FileError filesys_dirGetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info);
static mpe_FileError filesys_dirSetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info);
static mpe_FileError filesys_fileSetChangeListener(const char *fileName,
        mpe_EventQueue evQueue, void *act, mpe_FileChangeHandle *out);
static mpe_FileError filesys_fileRemoveChangeListener(
        mpe_FileChangeHandle handle);
static mpe_FileError filesys_streamOpen(const char *fileName,
        mpe_Stream *fileHandleBuffer);
static mpe_FileError filesys_streamClose(mpe_File fileHandle);
static mpe_FileError filesys_streamReadEvent(mpe_File fileHandle,
        mpe_StreamEventInfo *event);
static mpe_FileError filesys_streamGetNumTaps(mpe_Stream streamHandle,
        uint16_t tapType, uint32_t *numTaps);
static mpe_FileError filesys_streamReadTap(mpe_Stream streamHandle,
        uint16_t tapType, uint32_t tapNumber, uint16_t *tap, uint16_t *tapID);
static mpe_FileError filesys_filePrefetch(const char *fileName);
static mpe_FileError filesys_filePrefetchModule(const char *mountpoint,
        const char *moduleName);
static mpe_FileError filesys_fileDIILocation(const char *mountpoint,
        uint16_t diiID, uint16_t assocTag);

/* internal functions */
static void filesysRegisterDriver(const mpeos_filesys_ftable_t *ftable,
        const char *mountPoint);
static filesys_mountTable *getMount(const char *name);

static filesys_mountTable *getMount(const char *name)
{
    filesys_mountTable *pCheckMount;
    filesys_mountTable *bestMount = NULL;

    for (pCheckMount = filesysMounts; pCheckMount != NULL; pCheckMount
            = pCheckMount->next)
    {
        if (strncmp(name, pCheckMount->mountPoint, strlen(
                pCheckMount->mountPoint)) == 0)
        {
            bestMount = pCheckMount;
        }
    }

    return bestMount;
}

static void filesys_init(void)
{
    static int inited = 0;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "filesys_init\n");

    if (inited == 0)
    {
        inited = 1;
        filesysMounts = NULL;
        mpeos_filesysInit(filesysRegisterDriver);

        // Register machine independent file systems.
        filesysRegisterDriver(&mpe_fileOCFTable, "/oc");
        filesysRegisterDriver(&mpe_fileRomfsFTable, "/romfs");
    }
}

static void filesysRegisterDriver(const mpeos_filesys_ftable_t *ftable,
        const char *mountPoint)
{
    filesys_mountTable *pNewMount;
    filesys_mountTable **ppCheckMount;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "RegisterMount('%s')\n", mountPoint);

    /* valid parameters? */
    if ((ftable == NULL) || (mountPoint == NULL))
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "FILESYS: RegisterMount('%s') failed - invalid paramters\n",
                mountPoint);
        return; /* MPE_FS_ERROR_INVALID_PARAMETER */
    }

    /* are all table function pointers valid             **
     ** (so that later we don't have to check every time) */

    if ((ftable->mpeos_init_ptr == NULL)
            || (ftable->mpeos_fileOpen_ptr == NULL)
            || (ftable->mpeos_fileClose_ptr == NULL)
            || (ftable->mpeos_fileRead_ptr == NULL)
            || (ftable->mpeos_fileWrite_ptr == NULL)
            || (ftable->mpeos_fileSeek_ptr == NULL)
            || (ftable->mpeos_fileSync_ptr == NULL)
            || (ftable->mpeos_fileGetStat_ptr == NULL)
            || (ftable->mpeos_fileSetStat_ptr == NULL)
            || (ftable->mpeos_fileDelete_ptr == NULL)
            || (ftable->mpeos_fileRename_ptr == NULL)
            || (ftable->mpeos_dirOpen_ptr == NULL)
            || (ftable->mpeos_dirRead_ptr == NULL)
            || (ftable->mpeos_dirClose_ptr == NULL)
            || (ftable->mpeos_dirDelete_ptr == NULL)
            || (ftable->mpeos_dirRename_ptr == NULL)
            || (ftable->mpeos_dirCreate_ptr == NULL)
            || (ftable->mpeos_dirMount_ptr == NULL)
            || (ftable->mpeos_dirUnmount_ptr == NULL)
            || (ftable->mpeos_dirGetUStat_ptr == NULL)
            || (ftable->mpeos_dirSetUStat_ptr == NULL))
    {
        MPE_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "FILESYS: RegisterMount('%s') failed - invalid table function paramters\n",
                mountPoint);
        return; // MPE_FS_ERROR_INVALID_PARAMETER
    }

    /* allocate a new entry for the mount table */
    if (mpeos_memAllocP(MPE_MEM_FILE, sizeof(filesys_mountTable), (void**) &pNewMount) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FILESYS: RegisterMount('%s') failed - out of memory\n",
                mountPoint);
        return; /* MPE_FS_ERROR_OUT_OF_MEM */
    }

    /* allocate our own function table and mount point string to hold caller's info */
    if (mpeos_memAllocP(MPE_MEM_FILE, sizeof(mpeos_filesys_ftable_t),
                        (void**)&pNewMount->ftable) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FILESYS: RegisterMount('%s') failed - out of memory\n",
                mountPoint);
        mpeos_memFreeP(MPE_MEM_FILE, pNewMount);
        return; /* MPE_FS_ERROR_OUT_OF_MEM */
    }
    if (mpeos_memAllocP(MPE_MEM_FILE, strlen(mountPoint) + 1, 
                        (void**)&pNewMount->mountPoint) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FILESYS: RegisterMount('%s') failed - out of memory\n",
                mountPoint);
        mpeos_memFreeP(MPE_MEM_FILE, pNewMount->ftable);
        mpeos_memFreeP(MPE_MEM_FILE, pNewMount);
        return; /* MPE_FS_ERROR_OUT_OF_MEM */
    }

    /* Copy function table and mount point to our structure */
    pNewMount->ftable->mpeos_init_ptr = ftable->mpeos_init_ptr;
    pNewMount->ftable->mpeos_fileOpen_ptr = ftable->mpeos_fileOpen_ptr;
    pNewMount->ftable->mpeos_fileClose_ptr = ftable->mpeos_fileClose_ptr;
    pNewMount->ftable->mpeos_fileRead_ptr = ftable->mpeos_fileRead_ptr;
    pNewMount->ftable->mpeos_fileWrite_ptr = ftable->mpeos_fileWrite_ptr;
    pNewMount->ftable->mpeos_fileSeek_ptr = ftable->mpeos_fileSeek_ptr;
    pNewMount->ftable->mpeos_fileSync_ptr = ftable->mpeos_fileSync_ptr;
    pNewMount->ftable->mpeos_fileGetStat_ptr = ftable->mpeos_fileGetStat_ptr;
    pNewMount->ftable->mpeos_fileSetStat_ptr = ftable->mpeos_fileSetStat_ptr;
    pNewMount->ftable->mpeos_fileGetFStat_ptr = ftable->mpeos_fileGetFStat_ptr;
    pNewMount->ftable->mpeos_fileSetFStat_ptr = ftable->mpeos_fileSetFStat_ptr;
    pNewMount->ftable->mpeos_fileDelete_ptr = ftable->mpeos_fileDelete_ptr;
    pNewMount->ftable->mpeos_fileRename_ptr = ftable->mpeos_fileRename_ptr;
    pNewMount->ftable->mpeos_dirOpen_ptr = ftable->mpeos_dirOpen_ptr;
    pNewMount->ftable->mpeos_dirRead_ptr = ftable->mpeos_dirRead_ptr;
    pNewMount->ftable->mpeos_dirClose_ptr = ftable->mpeos_dirClose_ptr;
    pNewMount->ftable->mpeos_dirDelete_ptr = ftable->mpeos_dirDelete_ptr;
    pNewMount->ftable->mpeos_dirRename_ptr = ftable->mpeos_dirRename_ptr;
    pNewMount->ftable->mpeos_dirCreate_ptr = ftable->mpeos_dirCreate_ptr;
    pNewMount->ftable->mpeos_dirMount_ptr = ftable->mpeos_dirMount_ptr;
    pNewMount->ftable->mpeos_dirUnmount_ptr = ftable->mpeos_dirUnmount_ptr;
    pNewMount->ftable->mpeos_dirGetUStat_ptr = ftable->mpeos_dirGetUStat_ptr;
    pNewMount->ftable->mpeos_dirSetUStat_ptr = ftable->mpeos_dirSetUStat_ptr;
    pNewMount->ftable->mpeos_streamOpen_ptr = ftable->mpeos_streamOpen_ptr;
    pNewMount->ftable->mpeos_streamClose_ptr = ftable->mpeos_streamClose_ptr;
    pNewMount->ftable->mpeos_streamReadEvent_ptr = ftable->mpeos_streamReadEvent_ptr;
    pNewMount->ftable->mpeos_streamGetNumTaps_ptr = ftable->mpeos_streamGetNumTaps_ptr;
    pNewMount->ftable->mpeos_streamReadTap_ptr = ftable->mpeos_streamReadTap_ptr;
    pNewMount->ftable->mpeos_filePrefetch_ptr = ftable->mpeos_filePrefetch_ptr;
    pNewMount->ftable->mpeos_filePrefetchModule_ptr = ftable->mpeos_filePrefetchModule_ptr;
    pNewMount->ftable->mpeos_fileAddDII_ptr = ftable->mpeos_fileAddDII_ptr;
    pNewMount->ftable->mpeos_fileSetChangeListener_ptr = ftable->mpeos_fileSetChangeListener_ptr;
    pNewMount->ftable->mpeos_fileRemoveChangeListener_ptr = ftable->mpeos_fileRemoveChangeListener_ptr;
    
    strcpy(pNewMount->mountPoint, mountPoint);

    /* add the new entry into its ordered place in the mount list **
     ** so that parent directories appear before child directories */
    pNewMount->next = NULL;

    ppCheckMount = &filesysMounts;
    while (*ppCheckMount != NULL)
    {
        /* compare mount points, and stick us in alphabetically in order */
        if (strcmp((*ppCheckMount)->mountPoint, mountPoint) >= 0)
        {
            pNewMount->next = (*ppCheckMount);
            break;
        }

        ppCheckMount = &((*ppCheckMount)->next);
    }
    *ppCheckMount = pNewMount;

    /* call the new file system driver's initialization routine */
    (ftable->mpeos_init_ptr)(mountPoint);
}

#define IS_SLASH(pathChar) ( ((pathChar) == '/') || ((pathChar) == '\\') )

char *filesys_canonicalPath(const char *rawPath)
{
    char *canonicalPathBuf = NULL;
    char *canonicalPath = NULL;

    /* parameter check (insure we don't have an empty input path) */
    if ((rawPath != NULL) && (rawPath[0] != 0))
    {
        uint32_t rawPathLen = strlen(rawPath);

        /* allocate a buffer big enough for the biggest potential resulting path */
        if (mpeos_memAllocP(MPE_MEM_FILE, (MPE_FS_MAX_PATH + 1),
                (void**) &canonicalPathBuf) == MPE_SUCCESS)
        {
            canonicalPath = canonicalPathBuf;
            canonicalPath[0] = 0;

#ifdef MPE_WINDOWS
            /* special case for windows drive letter absolute paths (eg, "C:\xxx") */
            if ( (rawPathLen >= 2) && (rawPath[1] == ':') )
            {
                canonicalPath[0] = MPE_FS_SEPARATION_CHAR;
                canonicalPath[1] = rawPath[0];
                canonicalPath[2] = 0;
                canonicalPath += 2;
                rawPath += 2;
                rawPathLen -= 2;
            }
            else
#endif
            /* handle fake "system cwd" absolute path by changing it to be a        *
             * relative path (update pointer past the "absolute" part of the path)  */
            /* do this in two parts because we want to insure that SYSCWD is a real *
             * directory name, not just part of a name                              */

            if ((strncmp(SYSCWD, rawPath, sizeof(SYSCWD) - 1) == 0)
                    && ((rawPath[sizeof(SYSCWD) - 1] == 0)
                            || (IS_SLASH(rawPath[sizeof(SYSCWD)-1]))))
            {
                /* go past the "absolute" part of the path plus the next slash (if it's there) */
                rawPath += sizeof(SYSCWD) - 1;
                rawPathLen -= sizeof(SYSCWD) - 1;
                if (IS_SLASH(rawPath[0]))
                {
                    rawPath += 1;
                    rawPathLen -= 1;
                }
            }

            /* handle relative paths by adding in our home path before the indicated path */
            if (!IS_SLASH(rawPath[0]))
            {
                uint32_t len = MPE_FS_MAX_PATH - 1; /* leave room for a trailing slash */
                if (mpeos_filesysGetDefaultDir(canonicalPath, len)
                        != MPE_SUCCESS)
                {
                    /* don't have room for this default path, so we punt out */
                    (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPathBuf);
                    return NULL;
                }

                /* add trailing slash */
                if (canonicalPath != NULL)
                {
                    strcat(canonicalPath, MPE_FS_SEPARATION_STRING);
                }
            }

            /* handle any "." & ".." in path while copying old path into new buffer */
            if (canonicalPath != NULL)
            {
                uint32_t rawPathCur = 0;
                uint32_t canonicalPathCur = strlen(canonicalPath);

                while (rawPathCur < rawPathLen)
                {
                    /* make sure we have room to the next character(s) below */
                    if (canonicalPathCur > (MPE_FS_MAX_PATH - 1))
                    {
                        /* this path is ending up to be too big */
                        MPE_LOG(
                                MPE_LOG_ERROR,
                                MPE_MOD_FILESYS,
                                "FILESYS: filesys_canonicalPath FILENAME_TOO_LONG!!! rawPath = %s\n",
                                rawPath);
                        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPathBuf);
                        return NULL;
                    }

                    if (rawPath[rawPathCur] == '.')
                    {
                        if (IS_SLASH(rawPath[rawPathCur+1]))
                        {
                            /* found "./" (current directory),             **
                             ** so just "remove" it from the canonical path */
                            rawPathCur++;
                        }
                        else if ((rawPath[rawPathCur + 1] == '.')
                                && (IS_SLASH(rawPath[rawPathCur+2])))
                        {
                            /* found "../", */
                            rawPathCur += 2;
                            /* so (try to) back up one directory */
                            if (canonicalPathCur > 0)
                            {
                                canonicalPathCur--;
                                while ((canonicalPathCur > 0)
                                        && (!IS_SLASH(canonicalPath[canonicalPathCur-1])))
                                {
                                    canonicalPathCur--;
                                }
                                canonicalPath[canonicalPathCur] = 0;
                            }
                        }
                        else
                        {
                            /* else this is just a "." in a file/dir name */
                            canonicalPath[canonicalPathCur++] = '.';
                            canonicalPath[canonicalPathCur] = 0;
                        }
                    }
                    else if (IS_SLASH(rawPath[rawPathCur]))
                    {
                        /* insure that our slashes are the proper type */
                        canonicalPath[canonicalPathCur++]
                                = MPE_FS_SEPARATION_CHAR;
                        canonicalPath[canonicalPathCur] = 0;
                    }
                    else
                    {
                        /* else just copy the character over */
                        canonicalPath[canonicalPathCur++] = rawPath[rawPathCur];
                        canonicalPath[canonicalPathCur] = 0;
                    }

                    /* go to next input character */
                    rawPathCur++;
                }
            }

        }
    }

    return canonicalPathBuf;
}

static mpe_FileError filesys_fileOpen(const char *fileName,
        mpe_FileOpenMode openMode, mpe_File *fileHandleBuffer)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_File *pHandle;

    /* parameter check */
    if ((fileName == NULL) || (fileHandleBuffer == NULL) || (strlen(fileName)
            > MPE_FS_MAX_PATH))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    pHandle = NULL;
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* create our file handle */
        if (mpeos_memAllocP(MPE_MEM_FILE, sizeof(*pHandle), (void**) &pHandle)
                != MPE_SUCCESS)
        {
            err = MPE_FS_ERROR_OUT_OF_MEM;
        }
        else
        {
            /* find the driver mount for the requested file */
            pHandle->mount = getMount(canonicalPath);
            if (pHandle->mount == NULL)
            {
                /* cleanup upon errors */
                (void) mpeos_memFreeP(MPE_MEM_FILE, (void*) pHandle);
                pHandle = NULL;
                err = MPE_FS_ERROR_NO_MOUNT;
            }
            else
            {
                /* Strip out the mountpoint part of the name. */
                const char *nomountPath = canonicalPath + strlen(
                        pHandle->mount->mountPoint);

                /* call the driver to open the file */
                err = (pHandle->mount->ftable->mpeos_fileOpen_ptr)(nomountPath,
                        openMode, &pHandle->handle);
                if (err != MPE_FS_ERROR_SUCCESS)
                {
                    /* cleanup upon errors */
                    (void) mpeos_memFreeP(MPE_MEM_FILE, pHandle);
                    pHandle = NULL;
                }
            }
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    *fileHandleBuffer = (mpe_File) pHandle;
    return err;
}

static mpe_FileError filesys_fileClose(mpe_File fileHandle)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if (fileHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_fileClose_ptr)(pHandle->handle);

    /* return allocated memory */
    (void) mpeos_memFreeP(MPE_MEM_FILE, pHandle);

    return err;
}

static mpe_FileError filesys_fileRead(mpe_File fileHandle, uint32_t* count,
        void* buffer)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if ((fileHandle == NULL) || (count == NULL) || (buffer == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_fileRead_ptr)(pHandle->handle, count,
            buffer);

    return err;
}

static mpe_FileError filesys_fileWrite(mpe_File fileHandle, uint32_t* count,
        void* buffer)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if ((fileHandle == NULL) || (count == NULL) || (buffer == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_fileWrite_ptr)(pHandle->handle, count,
            buffer);

    return err;
}

static mpe_FileError filesys_fileSeek(mpe_File fileHandle,
        mpe_FileSeekMode seekMode, int64_t* offset)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if ((fileHandle == NULL) || (offset == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_fileSeek_ptr)(pHandle->handle,
            seekMode, offset);

    return err;
}

static mpe_FileError filesys_fileSync(mpe_File fileHandle)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if (fileHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_fileSync_ptr)(pHandle->handle);

    return err;
}

static mpe_FileError filesys_fileGetStat(const char* fileName,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err;
    char *canonicalPath;
    const char *nomountPath;
    filesys_mountTable *pMount;

    /* special case for returning default-system-directory */
    if (mode == MPE_FS_STAT_DEFAULTSYSDIR)
    {
        err = mpeos_filesysGetDefaultDir((char*) info->buf,
                (uint32_t) info->size);
        if (err == MPE_FS_ERROR_SUCCESS)
        {
            /* return the size of our default path string */
            info->size = strlen((char*) info->buf);
        }

        /* return with the default-system-path string & string length */
        return err;
    }

    /* parameter check */
    if (fileName == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pMount = getMount(canonicalPath);
        if (pMount == NULL)
        {
            /* cleanup upon errors */
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            /* Strip out the mountpoint part of the name. */
            nomountPath = canonicalPath + strlen(pMount->mountPoint);

            /* call the driver */
            err = (pMount->ftable->mpeos_fileGetStat_ptr)(nomountPath, mode,
                    info);
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_fileSetStat(const char* fileName,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *pMount;
    const char *nomountPath;

    /* parameter check */
    if (fileName == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pMount = getMount(fileName);
        if (pMount == NULL)
        {
            /* cleanup upon errors */
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            /* Strip out the mountpoint part of the name. */
            nomountPath = canonicalPath + strlen(pMount->mountPoint);

            /* call the driver */
            err = (pMount->ftable->mpeos_fileSetStat_ptr)(nomountPath, mode,
                    info);
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_fileGetFStat(mpe_File fileHandle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if (fileHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_fileGetFStat_ptr)(pHandle->handle,
            mode, info);

    return err;
}

static mpe_FileError filesys_fileSetFStat(mpe_File fileHandle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if (fileHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_fileSetFStat_ptr)(pHandle->handle,
            mode, info);

    return err;
}

static mpe_FileError filesys_fileDelete(const char* fileName)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *pMount;
    const char *nomountPath;

    /* parameter check */
    if (fileName == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pMount = getMount(fileName);
        if (pMount == NULL)
        {
            /* cleanup upon errors */
            return MPE_FS_ERROR_NO_MOUNT;
        }

        /* Strip out the mountpoint part of the name */
        nomountPath = canonicalPath + strlen(pMount->mountPoint);

        /* call the driver */
        err = (pMount->ftable->mpeos_fileDelete_ptr)(nomountPath);

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_fileRename(const char* fileName,
        const char* newFileName)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *pMount;
    char *newCanonicalPath;
    filesys_mountTable *pNewMount;
    const char *nomountPath;
    const char *newNomountPath;

    /* parameter check */
    if ((fileName == NULL) || (newFileName == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pMount = getMount(fileName);
        if (pMount == NULL)
        {
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            newCanonicalPath = filesys_canonicalPath(newFileName);
            if (newCanonicalPath == NULL)
            {
                err = MPE_FS_ERROR_OUT_OF_MEM;
            }
            else
            {
                /* insure that the new name is on the same mount */
                pNewMount = getMount(newFileName);
                if (pNewMount == NULL)
                {
                    err = MPE_FS_ERROR_NO_MOUNT;
                }
                else
                {
                    if (pMount != pNewMount)
                    {
                        err = MPE_FS_ERROR_NO_MOUNT;
                    }
                    else
                    {
                        /* Strip out the mountpoint part of the name. */
                        nomountPath = canonicalPath
                                + strlen(pMount->mountPoint);
                        newNomountPath = newCanonicalPath + strlen(
                                pMount->mountPoint);

                        /* call the driver */
                        err = (pMount->ftable->mpeos_fileRename_ptr)(
                                nomountPath, newNomountPath);
                    }
                }

                /* free up canonical path buffer */
                (void) mpeos_memFreeP(MPE_MEM_FILE, newCanonicalPath);
            }
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_dirOpen(const char* dirName,
        mpe_Dir* dirHandleBuffer)
{
    mpe_FileError err;
    filesys_Dir *pHandle;
    char *canonicalPath;
    const char *nomountPath;

    /* parameter check */
    if ((dirName == NULL) || (dirHandleBuffer == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* create our file handle */
    if (mpeos_memAllocP(MPE_MEM_FILE, sizeof(*pHandle), (void**) &pHandle)
            != MPE_SUCCESS)
    {
        return MPE_FS_ERROR_OUT_OF_MEM;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(dirName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pHandle->mount = getMount(canonicalPath);
        if (pHandle->mount == NULL)
        {
            /* cleanup upon errors */
            mpeos_memFreeP(MPE_MEM_FILE, (void*) pHandle);
            pHandle = NULL;
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            /* Strip out the mountpoint part of the name */
            nomountPath = canonicalPath + strlen(pHandle->mount->mountPoint);

            /* call the driver to open the file */
            err = (pHandle->mount->ftable->mpeos_dirOpen_ptr)(nomountPath,
                    &pHandle->handle);
            if (err != MPE_FS_ERROR_SUCCESS)
            {
                /* cleanup upon errors */
                mpeos_memFreeP(MPE_MEM_FILE, pHandle);
                pHandle = NULL;
            }
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    *dirHandleBuffer = (mpe_Dir) pHandle;
    return err;
}

static mpe_FileError filesys_dirClose(mpe_Dir dirHandle)
{
    mpe_FileError err;
    filesys_Dir *pHandle;

    /* parameter check */
    if (dirHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_Dir*) dirHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_dirClose_ptr)(pHandle->handle);

    /* return allocated memory */
    (void) mpeos_memFreeP(MPE_MEM_FILE, pHandle);

    return err;
}

static mpe_FileError filesys_dirRead(mpe_Dir dirHandle, mpe_DirEntry* dirEnt)
{
    mpe_FileError err;
    filesys_Dir *pHandle;

    /* parameter check */
    if ((dirHandle == NULL) || (dirEnt == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_Dir*) dirHandle;

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_dirRead_ptr)(pHandle->handle, dirEnt);

    return err;
}

static mpe_FileError filesys_dirDelete(const char* dirName)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *pMount;
    const char *nomountPath;

    /* parameter check */
    if (dirName == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(dirName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pMount = getMount(dirName);
        if (pMount == NULL)
        {
            /* cleanup upon errors */
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            /* Strip out the mountpoint part of the name */
            nomountPath = canonicalPath + strlen(pMount->mountPoint);

            /* call the driver */
            err = (pMount->ftable->mpeos_dirDelete_ptr)(nomountPath);
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_dirRename(const char* dirName,
        const char* newDirName)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *pMount;
    char *newCanonicalPath;
    filesys_mountTable *pNewMount;
    const char *nomountPath;
    const char *newNomountPath;

    /* parameter check */
    if ((dirName == NULL) || (newDirName == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(dirName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pMount = getMount(dirName);
        if (pMount == NULL)
        {
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            newCanonicalPath = filesys_canonicalPath(newDirName);
            if (newCanonicalPath == NULL)
            {
                err = MPE_FS_ERROR_OUT_OF_MEM;
            }
            else
            {
                /* insure that the new name is on the same mount */
                pNewMount = getMount(newDirName);
                if (pNewMount == NULL)
                {
                    err = MPE_FS_ERROR_NO_MOUNT;
                }
                else
                {
                    if (pMount != pNewMount)
                    {
                        err = MPE_FS_ERROR_NO_MOUNT;
                    }
                    else
                    {
                        /* Strip out the mountpoint part of the name */
                        nomountPath = canonicalPath
                                + strlen(pMount->mountPoint);
                        newNomountPath = newCanonicalPath + strlen(
                                pMount->mountPoint);

                        /* call the driver */
                        err = (pMount->ftable->mpeos_dirRename_ptr)(
                                nomountPath, newNomountPath);
                    }
                }

                /* free up canonical path buffer */
                (void) mpeos_memFreeP(MPE_MEM_FILE, newCanonicalPath);
            }
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_dirCreate(const char* dirName)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *pMount;
    const char *nomountPath;

    /* parameter check */
    if (dirName == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(dirName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        pMount = getMount(dirName);
        if (pMount == NULL)
        {
            /* cleanup upon errors */
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else
        {
            /* Strip out the mountpoint part of the name */
            nomountPath = canonicalPath + strlen(pMount->mountPoint);

            /* call the driver */
            err = (pMount->ftable->mpeos_dirCreate_ptr)(nomountPath);
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_dirMount(const mpe_DirUrl *dirUrl)
{
    mpe_FileError err;
    filesys_mountTable *pMount;

    /* parameter check */
    if (dirUrl == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* cycle through all of the drivers to see which one can handle this mount */
    for (pMount = filesysMounts; pMount != NULL; pMount = pMount->next)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "FS: Trying mount: %p\n",
                pMount);
        /* see if this driver can handle this url type */
        err = (pMount->ftable->mpeos_dirMount_ptr)(dirUrl);
        if (err != MPE_FS_ERROR_UNKNOWN_URL && err != MPE_FS_ERROR_UNSUPPORT)
        {
            /* it could handle this mount type, so return err/success */
            return err;
        }
    }

    return MPE_FS_ERROR_UNKNOWN_URL;
}

static mpe_FileError filesys_dirUnmount(const mpe_DirUrl *dirUrl)
{
    mpe_FileError err;
    filesys_mountTable *pMount;

    /* parameter check */
    if (dirUrl == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* cycle through all of the drivers to see which one can handle this mount */
    for (pMount = filesysMounts; pMount != NULL; pMount = pMount->next)
    {
        /* see if this driver can handle this url type */
        err = (pMount->ftable->mpeos_dirUnmount_ptr)(dirUrl);
        if (err != MPE_FS_ERROR_UNKNOWN_URL && err != MPE_FS_ERROR_UNSUPPORT)
        {
            /* it could handle this mount type, so return err/success */
            return err;
        }
    }

    return MPE_FS_ERROR_UNKNOWN_URL;
}

static mpe_FileError filesys_dirGetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    mpe_FileError err;
    filesys_mountTable *pMount;

    /* parameter check */
    if (dirUrl == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* cycle through all of the drivers to see which one can handle this mount stat */
    for (pMount = filesysMounts; pMount != NULL; pMount = pMount->next)
    {
        /* special case for MPE_FS_STAT_MOUNTPATH                           **
         ** first add in to the returning buffer the mount point             **
         ** and then the MPEOS file-driver will append in it's relative path */
        if (mode == MPE_FS_STAT_MOUNTPATH)
        {
            strcpy(info->path, pMount->mountPoint);
        }

        /* see if this driver can handle this url type */
        err = (pMount->ftable->mpeos_dirGetUStat_ptr)(dirUrl, mode, info);
        if (err != MPE_FS_ERROR_UNKNOWN_URL && err != MPE_FS_ERROR_UNSUPPORT)
        {
            /* it could handle this mount type, so return (success or failure) */
            return err;
        }
    }

    return MPE_FS_ERROR_UNKNOWN_URL;
}

static mpe_FileError filesys_dirSetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    mpe_FileError err;
    filesys_mountTable *pMount;

    /* parameter check */
    if (dirUrl == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* cycle through all of the drivers to see which one can handle this mount stat */
    for (pMount = filesysMounts; pMount != NULL; pMount = pMount->next)
    {
        /* see if this driver can handle this url type */
        err = (pMount->ftable->mpeos_dirSetUStat_ptr)(dirUrl, mode, info);
        if (err != MPE_FS_ERROR_UNKNOWN_URL && err != MPE_FS_ERROR_UNSUPPORT)
        {
            /* it could handle this mount type, so return err/success */
            return err;
        }
    }

    return MPE_FS_ERROR_UNKNOWN_URL;
}

static mpe_FileError filesys_streamOpen(const char *fileName,
        mpe_Stream *fileHandleBuffer)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_File *pHandle;

    /* parameter check */
    if ((fileName == NULL) || (fileHandleBuffer == NULL) || (strlen(fileName)
            > MPE_FS_MAX_PATH))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    pHandle = NULL;
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* create our file handle */
        if (mpeos_memAllocP(MPE_MEM_FILE, sizeof(*pHandle), (void**) &pHandle)
                != MPE_SUCCESS)
        {
            err = MPE_FS_ERROR_OUT_OF_MEM;
        }
        else
        {
            /* find the driver mount for the requested file */
            pHandle->mount = getMount(canonicalPath);
            if (pHandle->mount == NULL)
            {
                /* cleanup upon errors */
                (void) mpeos_memFreeP(MPE_MEM_FILE, (void*) pHandle);
                pHandle = NULL;
                err = MPE_FS_ERROR_NO_MOUNT;
            }
            else if (pHandle->mount->ftable->mpeos_streamOpen_ptr == NULL)
            {
                /* cleanup upon errors */
                (void) mpeos_memFreeP(MPE_MEM_FILE, (void*) pHandle);
                pHandle = NULL;
                err = MPE_FS_ERROR_UNSUPPORT;
            }
            else
            {
                /* Strip out the mountpoint part of the name. */
                const char *nomountPath = canonicalPath + strlen(
                        pHandle->mount->mountPoint);

                /* call the driver to open the file */
                err = (pHandle->mount->ftable->mpeos_streamOpen_ptr)(
                        nomountPath, &pHandle->handle);
                if (err != MPE_FS_ERROR_SUCCESS)
                {
                    /* cleanup upon errors */
                    (void) mpeos_memFreeP(MPE_MEM_FILE, pHandle);
                    pHandle = NULL;
                }
            }
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    *fileHandleBuffer = (mpe_Stream) pHandle;
    return err;
}

static mpe_FileError filesys_streamClose(mpe_File fileHandle)
{
    mpe_FileError err;
    filesys_File *pHandle;

    /* parameter check */
    if (fileHandle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_File*) fileHandle;

    // Make sure this is implemented
    if (pHandle->mount->ftable->mpeos_streamClose_ptr == NULL)
    {
        return MPE_FS_ERROR_UNSUPPORT;
    }

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_streamClose_ptr)(pHandle->handle);

    /* return allocated memory */
    (void) mpeos_memFreeP(MPE_MEM_FILE, pHandle);

    return err;
}

static mpe_FileError filesys_streamReadEvent(mpe_Stream streamHandle,
        mpe_StreamEventInfo* event)
{
    mpe_FileError err;
    filesys_Dir *pHandle;

    /* parameter check */
    if ((streamHandle == NULL) || (event == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_Dir *) streamHandle;

    // Make sure this is implemented
    if (pHandle->mount->ftable->mpeos_streamReadEvent_ptr == NULL)
    {
        return MPE_FS_ERROR_UNSUPPORT;
    }

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_streamReadEvent_ptr)(pHandle->handle,
            event);

    return err;
}

static mpe_FileError filesys_streamGetNumTaps(mpe_Stream streamHandle,
        uint16_t tapType, uint32_t *numTaps)
{
    mpe_FileError err;
    filesys_Dir *pHandle;

    /* parameter check */
    if ((streamHandle == NULL) || (numTaps == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_Dir *) streamHandle;

    // Make sure this is implemented
    if (pHandle->mount->ftable->mpeos_streamGetNumTaps_ptr == NULL)
    {
        return MPE_FS_ERROR_UNSUPPORT;
    }

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_streamGetNumTaps_ptr)(pHandle->handle,
            tapType, numTaps);

    return err;
}

static mpe_FileError filesys_streamReadTap(mpe_Stream streamHandle,
        uint16_t tapType, uint32_t tapNumber, uint16_t *tap, uint16_t *tapID)
{
    mpe_FileError err;
    filesys_Dir *pHandle;

    /* parameter check */
    if ((streamHandle == NULL) || (tap == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    pHandle = (filesys_Dir *) streamHandle;

    // Make sure this is implemented
    if (pHandle->mount->ftable->mpeos_streamReadTap_ptr == NULL)
    {
        return MPE_FS_ERROR_UNSUPPORT;
    }

    /* call the driver */
    err = (pHandle->mount->ftable->mpeos_streamReadTap_ptr)(pHandle->handle,
            tapType, tapNumber, tap, tapID);

    return err;
}

static mpe_FileError filesys_filePrefetch(const char *fileName)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *mt;

    /* parameter check */
    if ((fileName == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        mt = getMount(canonicalPath);
        if (mt == NULL)
        {
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else if (mt->ftable->mpeos_filePrefetch_ptr == NULL)
        {
            err = MPE_FS_ERROR_UNSUPPORT;
        }
        else
        {
            /* Strip out the mountpoint part of the name. */
            const char *nomountPath = canonicalPath + strlen(mt->mountPoint);

            /* call the driver to open the file */
            err = (mt->ftable->mpeos_filePrefetch_ptr)(nomountPath);
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_filePrefetchModule(const char *mountpoint,
        const char *moduleName)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *mt;

    /* parameter check */
    if ((mountpoint == NULL) || (moduleName == NULL))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(mountpoint);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        mt = getMount(canonicalPath);
        if (mt == NULL)
        {
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else if (mt->ftable->mpeos_filePrefetchModule_ptr == NULL)
        {
            err = MPE_FS_ERROR_UNSUPPORT;
        }
        else
        {
            /* Strip out the mountpoint part of the name. */
            const char *nomountPath = canonicalPath + strlen(mt->mountPoint);
            /* call the driver to open the file */
            err = (mt->ftable->mpeos_filePrefetchModule_ptr)(nomountPath,
                    moduleName);
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_fileDIILocation(const char *mountpoint,
        uint16_t diiID, uint16_t assocTag)
{
    mpe_FileError err;
    char *canonicalPath;
    filesys_mountTable *mt;

    /* parameter check */
    if (mountpoint == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(mountpoint);
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* find the driver mount for the requested file */
        mt = getMount(canonicalPath);
        if (mt == NULL)
        {
            err = MPE_FS_ERROR_NO_MOUNT;
        }
        else if (mt->ftable->mpeos_fileAddDII_ptr == NULL)
        {
            err = MPE_FS_ERROR_UNSUPPORT;
        }
        else
        {
            /* Strip out the mountpoint part of the name. */
            const char *nomountPath = canonicalPath + strlen(mt->mountPoint);
            /* call the driver to open the file */
            err = (mt->ftable->mpeos_fileAddDII_ptr)(nomountPath, diiID,
                    assocTag);
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    return err;
}

static mpe_FileError filesys_fileSetChangeListener(const char *fileName,
        mpe_EventQueue evQueue, void *act, mpe_FileChangeHandle *out)
{

    mpe_FileError err;
    char *canonicalPath;
    filesys_ChangeHandle *cHandle;

    /* parameter check */
    if ((fileName == NULL) || (out == NULL) || (strlen(fileName)
            > MPE_FS_MAX_PATH))
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* get canonical path (remember to free up buffer when finished) */
    canonicalPath = filesys_canonicalPath(fileName);
    cHandle = NULL;
    if (canonicalPath == NULL)
    {
        err = MPE_FS_ERROR_OUT_OF_MEM;
    }
    else
    {
        /* create our file handle */
        if (mpeos_memAllocP(MPE_MEM_FILE, sizeof(*cHandle), (void**) &cHandle)
                != MPE_SUCCESS)
        {
            err = MPE_FS_ERROR_OUT_OF_MEM;
        }
        else
        {
            /* find the driver mount for the requested file */
            cHandle->mount = getMount(canonicalPath);
            if (cHandle->mount == NULL)
            {
                /* cleanup upon errors */
                (void) mpeos_memFreeP(MPE_MEM_FILE, (void*) cHandle);
                cHandle = NULL;
                err = MPE_FS_ERROR_NO_MOUNT;
            }
            else if (cHandle->mount->ftable->mpeos_fileSetChangeListener_ptr
                    == NULL)
            {
                /* cleanup upon errors */
                (void) mpeos_memFreeP(MPE_MEM_FILE, (void*) cHandle);
                cHandle = NULL;
                err = MPE_FS_ERROR_UNSUPPORT;
            }
            else
            {
                /* Strip out the mountpoint part of the name. */
                const char *nomountPath = canonicalPath + strlen(
                        cHandle->mount->mountPoint);

                /* call the driver to open the file */
                err
                        = (cHandle->mount->ftable->mpeos_fileSetChangeListener_ptr)(
                                nomountPath, evQueue, act, &cHandle->handle);
                if (err != MPE_FS_ERROR_SUCCESS)
                {
                    /* cleanup upon errors */
                    (void) mpeos_memFreeP(MPE_MEM_FILE, cHandle);
                    cHandle = NULL;
                }
            }
        }

        /* free up canonical path buffer */
        (void) mpeos_memFreeP(MPE_MEM_FILE, canonicalPath);
    }

    *out = (mpe_FileChangeHandle) cHandle;
    return err;
}

static mpe_FileError filesys_fileRemoveChangeListener(
        mpe_FileChangeHandle handle)
{
    filesys_ChangeHandle *cHandle;
    mpe_FileError err;

    /* parameter check */
    if (handle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }
    cHandle = (filesys_ChangeHandle *) handle;
    err = (cHandle->mount->ftable->mpeos_fileRemoveChangeListener_ptr)(
            cHandle->handle);

    // Get rid of the handle.
    (void) mpeos_memFreeP(MPE_MEM_FILE, handle);
    return err;
}

/* file system function table */
mpe_filesys_ftable_t filesys_ftable =
{ filesys_init, filesys_fileOpen, filesys_fileClose, filesys_fileRead,
        filesys_fileWrite, filesys_fileSeek, filesys_fileSync,
        filesys_fileGetStat, filesys_fileSetStat, filesys_fileGetFStat,
        filesys_fileSetFStat, filesys_fileDelete, filesys_fileRename,
        filesys_dirOpen, filesys_dirRead, filesys_dirClose, filesys_dirDelete,
        filesys_dirRename, filesys_dirCreate, filesys_dirMount,
        filesys_dirUnmount, filesys_dirGetUStat, filesys_dirSetUStat,
        filesys_streamOpen, filesys_streamClose, filesys_streamReadEvent,
        filesys_streamGetNumTaps, filesys_streamReadTap, filesys_filePrefetch,
        filesys_filePrefetchModule, filesys_fileDIILocation,
        filesys_fileSetChangeListener, filesys_fileRemoveChangeListener, };

void mpe_filesysSetup(void)
{
    mpe_sys_install_ftable(&filesys_ftable, MPE_MGR_TYPE_FILESYS);
}
