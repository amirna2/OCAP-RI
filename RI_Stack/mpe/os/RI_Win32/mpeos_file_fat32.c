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

#include <mpe_file.h>

#include <mpeos_file.h>
#include <mpeos_mem.h>
#include <mpeos_dbg.h>
#include <mpeos_util.h>

#include <stdlib.h>
#include <errno.h>
#include <dirent.h>
#include <direct.h>
#include <io.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>
#include <wchar.h>

typedef struct DirHandle
{
    _WDIR * dir;
    wchar_t path[MPE_FS_MAX_PATH];
} DirHandle;

// NEW STUFF starts here

#define LOCAL_FILESYS_PATH      L"/filesys"
#define UNICODE_MARKER          L"//?/"
mpe_FileError mpeos_fileFAT32DirCreate(const char * name);

static mpe_FileError mapErrToMPE(const int errnoError)
{
    switch (errnoError)
    {
    case EEXIST:
        return MPE_FS_ERROR_ALREADY_EXISTS;
    case ENOFILE:
        return MPE_FS_ERROR_NOT_FOUND;
    case ENOMEM:
        return MPE_FS_ERROR_OUT_OF_MEM;
    case EINVAL:
        return MPE_FS_ERROR_INVALID_PARAMETER;
    case EROFS:
        return MPE_FS_ERROR_READ_ONLY;
    default:
        return MPE_FS_ERROR_FAILURE;
    }
}

static const char * mapErrToString(const int errnoError)
{
    switch (errnoError)
    {
    case EEXIST:
        return "File already exists";
    case ENOFILE:
        return "File not found";
    case ENOMEM:
        return "Out of memory or storage";
    case EINVAL:
        return "Invalid parameter";
    case EROFS:
        return "Read-only filesystem error";
    default:
        return "Unknown Error";
    }
}

static mpe_Error getNewPath(const char * const origPath,
        wchar_t ** const pNewPath)
{
    const size_t umLen = wcslen(UNICODE_MARKER);
    const size_t opLen = strlen(origPath);

    mpe_Error mpeError = MPE_SUCCESS;

    if (((opLen >= 3) && (origPath[0] == '/') && (origPath[2] == '/')) || // eg: "/c/..."
            ((opLen == 2) && (origPath[0] == '/'))) // eg: "/c"
    {
        // 1) Allocate enough space for the entire path:
        //      the length of the UNICODE_MARKER +
        //      the length of the original path +
        //      the string termination character

        mpeError = mpeos_memAllocP(MPE_MEM_FILE, (umLen + opLen + 1)
                * sizeof(wchar_t), (void **) pNewPath);
        if (mpeError != MPE_SUCCESS)
            return mpeError;

        // 2) Create the full path

        wcscpy(*pNewPath, UNICODE_MARKER);
        mbstowcs(*pNewPath + umLen, origPath, opLen + 1);

        // 3) re-map UNIX drive designations into FAT32 drive designations (eg, "/C"->"C:")

        *(*pNewPath + umLen + 0) = *(*pNewPath + umLen + 1);
        *(*pNewPath + umLen + 1) = L':';
    }
    else if ((opLen >= 2) && (origPath[1] == ':'))
    {
        // Note that we do not handle the case X:foo/bar/test.txt
        // I do not think this case ever happens.  If it does it will need to be handled as a new case here
        // We would have to determine the current working directory on the requested drive letter...
        //
        // 1) Allocate enough space for the entire path:
        //      the length of the UNICODE_MARKER +
        //      the length of the original path +
        //      the string termination character

        mpeError = mpeos_memAllocP(MPE_MEM_FILE, (umLen + opLen + 1)
                * sizeof(wchar_t), (void **) pNewPath);
        if (mpeError != MPE_SUCCESS)
            return mpeError;

        // 2) Create the full path

        wcscpy(*pNewPath, UNICODE_MARKER);
        mbstowcs(*pNewPath + umLen, origPath, opLen + 1);
    }
    else if ((opLen >= 1) && (origPath[0] == '/'))
    {
        // Turn this absolute path into absolute unicode wide paths
        //
        // 1) Allocate enough space for the entire path:
        //      the length of the UNICODE_MARKER +
        //      the length of space for the drive designator +
        //      the length of the original path +
        //      the string termination character

        mpeError = mpeos_memAllocP(MPE_MEM_FILE, (umLen + 2 + // "D:"
                opLen + 1) * sizeof(wchar_t), (void **) pNewPath);
        if (mpeError != MPE_SUCCESS)
            return mpeError;

        // 2) Create the full path

        wcscpy(*pNewPath, UNICODE_MARKER);
        *(*pNewPath + umLen + 0) = _getdrive() - 1 + L'A';
        *(*pNewPath + umLen + 1) = L':';
        mbstowcs(*pNewPath + umLen + 2, origPath, opLen + 1);
    }
    else // it is a relative path
    {
        //  1) Get the current working directory

        wchar_t * cwd = _wgetcwd(NULL, 0); // could not have ending /
        if (cwd == NULL)
        {
            const int errnoError = errno;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                    "getNewPath('%s') error getting CWD: %s\n", origPath,
                    mapErrToString(errnoError));
            return mapErrToMPE(errnoError);
        }

        // 2) Allocate enough space for the entire path:
        //      the length of the UNICODE_MARKER +
        //      the length of the CWD +
        //      the length of one '/' character +
        //      the length of the original path +
        //      the string termination character

        mpeError = mpeos_memAllocP(MPE_MEM_FILE, (umLen + wcslen(cwd) + 1 + // the '/' character
                opLen + 1) * sizeof(wchar_t), (void **) pNewPath);
        if (mpeError != MPE_SUCCESS)
        {
            free(cwd); // Note: do not use mpeos_memFreeP here!
            return mpeError;
        }

        // 3) Create the full path

        wcscpy(*pNewPath, UNICODE_MARKER);
        wcscat(*pNewPath, cwd);
        wcscat(*pNewPath, L"/");
        mbstowcs(*pNewPath + wcslen(*pNewPath), origPath, opLen + 1);
        free(cwd); // Note: do not use mpeos_memFreeP here!
    }

    // Convert to Windows slashes
    {
        const size_t npLen = wcslen(*pNewPath);
        size_t i;
        for (i = 0; i < npLen; i++)
            if (*(*pNewPath + i) == L'/')
                *(*pNewPath + i) = L'\\';
    }

    return mpeError;
}

// NEW STUFF ends here

// MPEOS private function called by other mpeos modules for this port
// Ensures that the full path specified exists
int createFullPath(const char * pathname)
{
    const char * walker = pathname;
    char mypath[MPE_FS_MAX_PATH];
    mpe_Error mpeError;

    // Skip past the drive letter portion of the path for absolute paths

    if ((pathname[2] == MPE_FS_SEPARATION_CHAR) && ((pathname[0]
            == MPE_FS_SEPARATION_CHAR) || // "/c/..." case
            (pathname[1] == ':'))) // "c:/..." case
        walker += 3;
    else if (pathname[0] == MPE_FS_SEPARATION_CHAR) // "/..." case
        walker++;

    // Walk the string looking for path elements and ensure that each
    // is created
    while ((walker = strchr(walker, MPE_FS_SEPARATION_CHAR)) != NULL)
    {
        strncpy(mypath, pathname, walker - pathname);
        mypath[walker - pathname] = '\0';

        mpeError = mpeos_fileFAT32DirCreate(mypath);
        if (mpeError != MPE_SUCCESS && mpeError != MPE_FS_ERROR_ALREADY_EXISTS)
        {
            return -1;
        }
        walker++;
    }

    // Finally, create the last part of the path
    if (pathname[strlen(pathname) - 1] != MPE_FS_SEPARATION_CHAR)
    {
        mpeError = mpeos_fileFAT32DirCreate(pathname);
        if (mpeError != MPE_SUCCESS && mpeError != MPE_FS_ERROR_ALREADY_EXISTS)
        {
            return -1;
        }
    }

    return 0;
}

/**
 * <i>mpeos_fileFAT32FileOpen(const char* name, mpe_FileOpenMode openMode, mpe_File * h)</i>
 *
 * Opens a file from a writable persistent file system.
 *
 * @param name The path to the file to open.
 * @param openMode The type of access requested (read, write, create, truncate, append).
 * @param h A pointer to an mpe_File handle, through which the opened file is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileOpen(const char * name,
        mpe_FileOpenMode openMode, mpe_File *h)
{
    int oflag = O_BINARY;
    mpe_Error mpeError;
    wchar_t * newName;
    int fd;

    /* parameter checking */
    if (name == NULL || h == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* Create? */
    if (openMode & MPE_FS_OPEN_MUST_CREATE)
    {
        oflag |= O_CREAT;
        oflag |= O_EXCL;
    }
    else if (openMode & MPE_FS_OPEN_CAN_CREATE)
    {
        oflag |= O_CREAT;
    }

    /* Read/Write */
    if (openMode & MPE_FS_OPEN_READWRITE || (openMode & MPE_FS_OPEN_READ
            && openMode & MPE_FS_OPEN_WRITE))
    {
        oflag |= O_RDWR;
    }
    else if (openMode & MPE_FS_OPEN_READ)
    {
        oflag |= O_RDONLY;
    }
    else if (openMode & MPE_FS_OPEN_WRITE)
    {
        oflag |= O_WRONLY;
    }
    else
    {
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILESYS,
                "FAT32FileOpen('%s') error.  Must specify either MPE_FS_OPEN_READ or MPE_FS_OPEN_WRITE or MPE_FS_OPEN_READWRITE\n",
                name);
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    /* Append */
    if (openMode & MPE_FS_OPEN_APPEND)
    {
        oflag |= O_APPEND;
    }

    /* Truncate */
    if (openMode & MPE_FS_OPEN_TRUNCATE)
    {
        oflag |= O_TRUNC;
    }

    mpeError = getNewPath(name, &newName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    if ((fd = _wopen(newName, oflag)) == -1)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "FAT32FileOpen('%s') error opening file. %s\n", name,
                mapErrToString(errnoError));
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mapErrToMPE(errnoError);
    }

    _wchmod(newName, S_IREAD | S_IWRITE);

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS, "FAT32FileOpen('%s') = 0x%x\n",
            name, fd);
    *h = (mpe_File) fd;
    mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32FileClose()</i>
 *
 * Closes a file previously opened with <i>mpeos_fileFAT32FileOpen()</i>.
 *
 * @param h A mpe_File handle, previously returned by <i>mpeos_fileFAT32FileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileClose(mpe_File h)
{
    int result = close((int) h);

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS, "FAT32FileClose(0x%x)\n", h);

    if (result == -1)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32FileClose('0x%x') error closing file. %s\n", h,
                mapErrToString(errnoError));
        return mapErrToMPE(errnoError);
    }

    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32FileRead()</i>
 *
 * Reads data from a file in persistent storage.
 *
 * @param h A mpe_File handle, previously returned by <i>mpeos_fileFAT32FileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                     read.  On exit, this will indicate the number of bytes actually read.
 * @param buffer A pointer to a buffer to receive the data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileRead(mpe_File h, uint32_t* count, void* buffer)
{
    int bytesRead = 0;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "Fat32FileRead() handle: 0x%x. count: %d. buffer: 0x%x\n", h,
            count, buffer);

    /* Validate parameters */
    if (count == NULL || buffer == NULL)
        return MPE_FS_ERROR_INVALID_PARAMETER;

    /* A count of 0 always succeeds */
    if (*count == 0)
        return MPE_FS_ERROR_SUCCESS;

    bytesRead = read((int) h, buffer, *count);
    if (bytesRead == -1)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32FileRead('0x%x') error. %s\n", h, mapErrToString(
                        errnoError));
        return mapErrToMPE(errnoError);
    }

    *count = (uint32_t) bytesRead;
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32FileWrite()</i>
 *
 * Writes data to a file in persistent storage.
 *
 * @param h A mpe_File handle, previously returned by <i>mpeos_fileFAT32FileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                     write.  On exit, this will indicate the number of bytes actually written.
 * @param buffer A pointer to a buffer containing the data to send.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileWrite(mpe_File h, uint32_t* count,
        void* buffer)
{
    int bytesWritten = 0;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "Fat32FileWrite() handle: 0x%x. count: %d. buffer: 0x%x\n", h,
            count, buffer);

    /* Validate parameters */
    if (count == NULL || buffer == NULL)
        return MPE_FS_ERROR_INVALID_PARAMETER;

    /* A count of 0 always succeeds */
    if (*count == 0)
        return MPE_FS_ERROR_SUCCESS;

    bytesWritten = write((int) h, buffer, *count);
    if (bytesWritten == -1)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32FileWrite('0x%x') error. %s\n", h, mapErrToString(
                        errnoError));
        *count = 0;
        return mapErrToMPE(errnoError);
    }

    *count = (uint32_t) bytesWritten;
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32FileSeek()</i>
 *
 * Changes and reports the current position within a file in persistent storage.
 *
 * @param h A mpe_File handle, previously returned by <i>mpeos_fileFAT32FileOpen()</i>.
 * @param seekMode A seek mode constant indicating whether the offset value should be considered
 *                    relative to the start, end, or current position within the file.
 * @param offset A pointer to a file position offset.  On entry, this should indicate the number
 *                    of bytes to seek, offset from the seekMode.  On exit, this will indicate the
 *                    new absolute position within the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileSeek(mpe_File h, mpe_FileSeekMode seekMode,
        int64_t* offset)
{
    long seekWhence;
    long seekRet;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "Fat32FileSeek() handle: 0x%x. seekMode: %d. offset: %d\n", h,
            seekMode, *offset);

    /* Validate parameters */
    if (offset == NULL)
        return MPE_FS_ERROR_INVALID_PARAMETER;

    /* Convert seek mode */
    switch (seekMode)
    {
    case MPE_FS_SEEK_SET:
        seekWhence = SEEK_SET;
        break;
    case MPE_FS_SEEK_CUR:
        seekWhence = SEEK_CUR;
        break;
    case MPE_FS_SEEK_END:
        seekWhence = SEEK_END;
        break;
    default:
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    seekRet = lseek((int) h, (long) (*offset), seekWhence);
    if (seekRet == -1)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32FileSeek('0x%x') error. %s\n", h, mapErrToString(
                        errnoError));
        *offset = 0;
        return mapErrToMPE(errnoError);
    }

    *offset = (int64_t) seekRet;
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32FileSync()</i>
 *
 * Synchronizes the contents of a file in persistent storage.  This will write any data that is
 *    pending.  Pending data is data that has been written to a file, but which hasn't been flushed
 *    to the storage device yet.
 *
 * @param h A mpe_File handle, previously returned by <i>mpeos_fileFAT32FileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileSync(mpe_File h)
{
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32FileDelete()</i>
 *
 * Deletes the specific file from the persistent storage file system.
 *
 * @param name The path to the file to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileDelete(const char* name)
{
    wchar_t * newName;
    mpe_Error mpeError;
    int retVal;

    mpeError = getNewPath(name, &newName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    // make sure permissions are set for writing
    _wchmod(newName, S_IWRITE);

    retVal = _wunlink(newName);
    if (retVal != 0)
    {
        int errnoError = errno;
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mapErrToMPE(errnoError);
    }

    mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32FileRename()</i>
 *
 * Renames or moves the specific file in persistent storage.
 *
 * @param old_name The path to the file to rename or move.
 * @param new_name The new path and/or name for the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileRename(const char* old_name,
        const char* new_name)
{
    wchar_t * newOldName;
    wchar_t * newNewName;
    mpe_Error mpeError;
    int retVal;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "FAT32FileRename('%s'->'%s')\n",
            old_name, new_name);

    /* Just return SUCCESS if old and new names are the same */
    if (strcmp(old_name, new_name) == 0)
        return MPE_FS_ERROR_SUCCESS;

    mpeError = getNewPath(old_name, &newOldName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    mpeError = getNewPath(new_name, &newNewName);
    if (mpeError != MPE_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newOldName);
        return mpeError;
    }

    // make sure permissions are set for writing the old file so it can be deleted
    _wchmod(newOldName, S_IWRITE);

    retVal = _wrename(newOldName, newNewName);
    if (retVal != 0)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32FileRename('%s','%s') error. %s\n", old_name, new_name,
                mapErrToString(errnoError));
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newOldName);
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newNewName);
        return mapErrToMPE(errnoError);
    }

    mpeos_memFreeP(MPE_MEM_FILE, (void *) newOldName);
    mpeos_memFreeP(MPE_MEM_FILE, (void *) newNewName);
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32DirOpen()</i>
 *
 * Opens a directory in persistent storage.
 *
 * @param name The path to the directory to open.
 * @param h A pointer to an mpe_File handle, through which the opened directory is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirOpen(const char * path, mpe_Dir * h)
{
    wchar_t * newName;
    mpe_Error mpeError;
    DirHandle * dh;
    _WDIR * dir;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS, "FAT32DirOpen('%s') = 0x%x\n",
            path, *h);

    mpeError = getNewPath(path, &newName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    dir = _wopendir(newName);
    if (dir == NULL)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "FAT32DirOpen('%s') error. %s\n", path, mapErrToString(
                        errnoError));
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mapErrToMPE(errnoError);
    }

    /* Allocate our directory structure */
    mpeError = mpeos_memAllocP(MPE_MEM_FILE, sizeof(struct DirHandle),
            (void**) &dh);
    if (mpeError != MPE_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mpeError;
    }

    // Copy info to our file handle structure
    dh->dir = dir;
    wcscpy(dh->path, newName); // newName is assumed to be less than MPE_FS_MAX_PATH characters long

    *h = (mpe_Dir) dh;
    mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32DirClose()</i>
 *
 * Closes a directory in persistent storage.
 *
 * @param h A mpe_Dir handle, previously returned by <i>mpeos_fileFAT32DirOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirClose(mpe_Dir h)
{
    DirHandle * dh = (DirHandle *) h;
    int retVal = _wclosedir(dh->dir);

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS, "FAT32DirClose(0x%x)\n", h);

    /* Free DirHandle memory */
    mpeos_memFreeP(MPE_MEM_FILE, (void*) dh); // do we care about returned errors?

    if (retVal != 0)
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32DirClose('0x%x') error. %s\n", h, mapErrToString(
                        errnoError));
        return mapErrToMPE(errnoError);
    }

    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32DirRead()</i>
 *
 * Reads the contents of a directory in persistent storage.  This can be used to iterate
 *    through the contents a directory in persistent storage.
 *
 * @param h A mpe_Dir handle, previously returned by
 *          <i>mpeos_fileFAT32DirOpen()</i>.
 * @param dirEnt A pointer to an mpe_DirEntry object.  On return, this will contain
 *                    data about the next directory entry.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirRead(mpe_Dir h, mpe_DirEntry * dirEnt)
{
    DirHandle * dh = (DirHandle *) h;
    struct _wdirent * aDirent;
    mpe_Error mpeError;

    /*
     * We want to remove any file attribute file names from the directory listing
     * so keep reading entries until we are either done or have found a
     * non-attribute file
     */

    while (1)
    {
        aDirent = _wreaddir(dh->dir);
        if (aDirent != NULL)
        {
            struct _stat statStruct;
            wchar_t * path;
            size_t i;

            /* Skip .. and . entries */
            /* Also skip persistent file attributes file */
            if ((wcscmp(aDirent->d_name, L"..") == 0) || (wcscmp(
                    aDirent->d_name, L".") == 0))
            {
                continue;
            }

            /* convert a Sim_HD_DirEntry into an mpe_DirEntry */

            wcstombs(dirEnt->name, aDirent->d_name, sizeof(dirEnt->name));

            dirEnt->name[sizeof(dirEnt->name) - 1] = '\0'; // make sure it is null terminated

            /* Build full pathname for stat operation */

            mpeError = mpeos_memAllocP(MPE_MEM_FILE, (wcslen(dh->path) + 1
                    + wcslen(aDirent->d_name) + 1) * sizeof(wchar_t),
                    (void **) &path);
            if (mpeError != MPE_SUCCESS)
            {
                return mpeError;
            }

            wcscpy(path, dh->path);
            wcscat(path, L"/");
            wcscat(path, aDirent->d_name);

            // Convert to Windows slashes
            for (i = 0; i < wcslen(path); i++)
                if (path[i] == L'/')
                    path[i] = L'\\';

            /* Stat the file to determine file size and dir/nodir */
            if (_wstat(&path[4], &statStruct) != 0) // BOZO KLUDGE
            {
                int errnoError = errno;
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                        "FAT32DirRead('%s') stat error. %s\n", path,
                        mapErrToString(errnoError));
                mpeos_memFreeP(MPE_MEM_FILE, (void *) path);
                return mapErrToMPE(errnoError);
            }
            dirEnt->fileSize = statStruct.st_size;
            dirEnt->isDir = S_ISDIR(statStruct.st_mode) ? TRUE : FALSE;

            mpeos_memFreeP(MPE_MEM_FILE, (void *) path);
            return MPE_FS_ERROR_SUCCESS;
        }
        else /* No more dir entries, so we are done */
        {
            return MPE_FS_ERROR_FAILURE;
        }
    }
}

/**
 * <i>mpeos_fileFAT32DirDelete()</i>
 *
 * Deletes a directory from persistent storage.
 *
 * @param name The path to the directory to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirDelete(const char* name)
{
    mpe_Error mpeError;
    wchar_t * newName;

    mpeError = getNewPath(name, &newName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    _wchmod(newName, S_IRWXU);
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS, "Removing directory - %s \n",
            name);

    if (_wrmdir(newName) == -1)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "mpeos_fileFAT32DirDelete Return Value Error - %d \n", errno);
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mapErrToMPE(errno);
    }

    mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpeos_fileFAT32DirRename()</i>
 *
 * Renames or moves the specific directory in persistent storage.
 *
 * @param old_name The path to the directory to rename or move.
 * @param new_name The new path and/or name for the directory.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirRename(const char * old_name,
        const char * new_name)
{
    return mpeos_fileFAT32FileRename(old_name, new_name);
}

/**
 * <i>mpeos_fileFAT32DirCreate()</i>
 *
 * Creates the specific directory in persistent storage.
 *
 * @param name The path to the directory to create.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirCreate(const char* name)
{
    wchar_t * newName;
    mpe_Error mpeError;
    int retVal;

    mpeError = getNewPath(name, &newName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    retVal = _wmkdir(newName);
    if (retVal != 0)
    {
        // When recursively creating directories, Java will attempt to create the
        // child directory. If that fails because the parent doesn't exist, then it
        // will try to create the parent.  Therefore, we expect there to be failures
        // with ENOFILE and EEXIST in normal operation, but we don't want to spew
        // into the log file

        int errnoError = errno;
        if (errnoError == EEXIST || errnoError == ENOFILE)
        {
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
                    "FAT32DirCreate('%s') failed -- probably OK . %s\n", name,
                    mapErrToString(errnoError));
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "FAT32DirCreate('%s') error. %s\n", name, mapErrToString(
                            errnoError));
        }
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mapErrToMPE(errnoError);
    }

    mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
    return MPE_FS_ERROR_SUCCESS;
}

/**
 * <i>mpe_FileError mpeos_fileFAT32DirMount(const mpe_DirUrl *dirUrl);</i>
 *
 * Mount the indicated URL on the optional (non '-1) indicated persistent id
 * into the object persistent file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirMount(const mpe_DirUrl *dirUrl)
{
    MPE_UNUSED_PARAM(dirUrl);
    return MPE_FS_ERROR_UNKNOWN_URL;
}

/**
 * <i>mpe_FileError mpeos_fileFAT32DirUnmount(const mpe_DirUrl *dirUrl);</i>
 *
 * Unmount the indicated URL from the object persistent file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32DirUnmount(const mpe_DirUrl *dirUrl)
{
    MPE_UNUSED_PARAM(dirUrl);
    return MPE_FS_ERROR_UNKNOWN_URL;
}

/**
 * <i>mpe_FileError mpeos_fileFAT32DirGetUStat(const mpe_DirUrl *dirUrl, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
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
mpe_FileError mpeos_fileFAT32DirGetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    MPE_UNUSED_PARAM(dirUrl);
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);
    return MPE_FS_ERROR_UNKNOWN_URL;
}

/**
 * <i>mpe_FileError mpeos_fileFAT32DirSetUStat(const mpe_DirUrl *dirUrl, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
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
mpe_FileError mpeos_fileFAT32DirSetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    MPE_UNUSED_PARAM(dirUrl);
    MPE_UNUSED_PARAM(mode);
    MPE_UNUSED_PARAM(info);
    return MPE_FS_ERROR_UNKNOWN_URL;
}

/**
 * <i>mpeos_fileFAT32FileGetFStat()</i>
 *
 * Get the status for the open file.
 *
 * @param handle An mpe_File handle, previously returned by <i>mpeos_fileFAT32FileOpen()</i>.
 * @param mode The specific status of the file to return (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer in which to receive the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileGetFStat(mpe_File handle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err = MPE_FS_ERROR_SUCCESS;
    struct stat statStruct;

    if (handle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    switch (mode)
    {
    /* return file size (end-of-file) */
    case MPE_FS_STAT_SIZE:
    {
        if (fstat((int) handle, &statStruct) != 0)
        {
            int errnoError = errno;
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                    "FAT32FileGetFStat('0x%x') error. %s\n", handle,
                    mapErrToString(errnoError));
            return mapErrToMPE(errnoError);
        }

        info->size = statStruct.st_size;
        break;
    }

        /* unknown getstat call */
    default:
    {
        err = MPE_FS_ERROR_INVALID_PARAMETER;
        break;
    }
    }

    /* cleanup & exit */
    return err;
}

/**
 * <i>mpeos_fileFAT32FileGetStat()</i>
 *
 * Get the status for the open file.
 *
 * @param name The pathlist to the file of which to get the status.
 * @param mode The specific status of the file to get (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer in which to get the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileGetStat(const char* name,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err = MPE_FS_ERROR_SUCCESS;
    struct _stat statStruct;
    mpe_Error mpeError;
    wchar_t * newName;

    mpeError = getNewPath(name, &newName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    if (_wstat(&newName[4], &statStruct) != 0) // BOZO KLUDGE
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32FileGetStat('%s') error. %s\n", name, mapErrToString(
                        errnoError));
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mapErrToMPE(errnoError);
    }

    switch (mode)
    {
    /* return file type (always FILE) */
    case MPE_FS_STAT_TYPE:
        if (S_ISDIR(statStruct.st_mode))
            info->type = MPE_FS_TYPE_DIR;
        else if (S_ISREG(statStruct.st_mode))
            info->type = MPE_FS_TYPE_FILE;
        else
            info->type = MPE_FS_TYPE_OTHER;
        err = MPE_FS_ERROR_SUCCESS;
        break;

    /* return last modified date */
    case MPE_FS_STAT_MODDATE:
        info->modDate = statStruct.st_mtime;
        break;

    /* return create date */
    case MPE_FS_STAT_CREATEDATE:
        info->createDate = statStruct.st_ctime;
        break;

    default:
    {
        mpe_File h;

        /* open a handle to the requested file (for now) */
        if ((err = mpeos_fileFAT32FileOpen(name, MPE_FS_OPEN_READ, &h))
                == MPE_FS_ERROR_SUCCESS)
        {
            /* call GetFStat */
            err = mpeos_fileFAT32FileGetFStat(h, mode, info);
            (void) mpeos_fileFAT32FileClose(h);
        }
    }
        break;
    }

    mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
    return (err);
}

/**
 * <i>mpeos_fileFAT32FileSetFStat()</i>
 *
 * Set the status for the open file.
 *
 * @param handle An mpe_File handle, previously returned by <i>mpeos_fileFAT32FileOpen()</i>.
 * @param mode The specific status of the file to set (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer with which to set the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileSetFStat(mpe_File handle,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err = MPE_FS_ERROR_SUCCESS;

    MPE_UNUSED_PARAM(info);

    if (handle == NULL)
    {
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    switch (mode)
    {
    case MPE_FS_STAT_SIZE:
        if (_chsize((int) handle, info->size) == -1)
        {
            err = MPE_FS_ERROR_FAILURE;
        }
        break;

    case MPE_FS_STAT_MODDATE:
        return MPE_FS_ERROR_SUCCESS;

        /* unknown setstat call */
    default:
        err = MPE_FS_ERROR_READ_ONLY;
        break;
    }

    /* cleanup & exit */
    return err;
}

/**
 * <i>mpeos_fileFAT32FileSetStat()</i>
 *
 * Set the status for the open file.
 *
 * @param name The pathlist to the file of which to set the status.
 * @param mode The specific status of the file to set (see MPE_FS_STAT_xxx).
 * @param info A pointer to the buffer with which to set the indicated file stat data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
mpe_FileError mpeos_fileFAT32FileSetStat(const char* name,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err = MPE_FS_ERROR_SUCCESS;
    struct _stat statStruct;
    mpe_Error mpeError;
    wchar_t * newName;

    mpeError = getNewPath(name, &newName);
    if (mpeError != MPE_SUCCESS)
        return mpeError;

    /* verify the file or directory exists */
    if (_wstat(&newName[4], &statStruct) != 0) // BOZO KLUDGE
    {
        int errnoError = errno;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "FAT32FileSetStat('%s') error. %s\n", name, mapErrToString(
                        errnoError));
        mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
        return mapErrToMPE(errnoError);
    }

    switch (mode)
    {
    /* set file type */
    case MPE_FS_STAT_TYPE:
        /* The user should not be able to set this */
        break;

        /* set file known (parent directory loaded) status */
    case MPE_FS_STAT_ISKNOWN:
        err = MPE_FS_ERROR_READ_ONLY;
        break;

        /* set file create date */
    case MPE_FS_STAT_CREATEDATE:
        err = MPE_FS_ERROR_READ_ONLY;
        break;

        /* set file last-modified date */
    case MPE_FS_STAT_MODDATE:
        err = MPE_FS_ERROR_READ_ONLY;
        break;

        /* unknown setstat call */
    default:
    {
        mpe_File h;

        /* open a handle to the requested file (for now) */
        if ((err = mpeos_fileFAT32FileOpen(name, MPE_FS_OPEN_READ, &h))
                == MPE_FS_ERROR_SUCCESS)
        {
            /* call SetFStat */
            err = mpeos_fileFAT32FileSetFStat(h, mode, info);
            (void) mpeos_fileFAT32FileClose(h);
        }
    }
        break;
    }

    mpeos_memFreeP(MPE_MEM_FILE, (void *) newName);
    return (err);
}

/**
 * <i>mpeos_fileFAT32Init(const char* mountPoint)</i>
 *
 * Initializes the Win32 Simulator persistent file system.
 *
 * @param mountPoint The file-system namespace in which this driver is mounted.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
void mpeos_fileFAT32Init(const char* mountPoint)
{
    MPE_UNUSED_PARAM(mountPoint);
}

/* file system driver entry-point function table */
mpeos_filesys_ftable_t mpeos_fileFAT32FTable =
{ mpeos_fileFAT32Init, mpeos_fileFAT32FileOpen, mpeos_fileFAT32FileClose,
        mpeos_fileFAT32FileRead, mpeos_fileFAT32FileWrite,
        mpeos_fileFAT32FileSeek, mpeos_fileFAT32FileSync,
        mpeos_fileFAT32FileGetStat, mpeos_fileFAT32FileSetStat,
        mpeos_fileFAT32FileGetFStat, mpeos_fileFAT32FileSetFStat,
        mpeos_fileFAT32FileDelete, mpeos_fileFAT32FileRename,
        mpeos_fileFAT32DirOpen, mpeos_fileFAT32DirRead,
        mpeos_fileFAT32DirClose, mpeos_fileFAT32DirDelete,
        mpeos_fileFAT32DirRename, mpeos_fileFAT32DirCreate,
        mpeos_fileFAT32DirMount, mpeos_fileFAT32DirUnmount,
        mpeos_fileFAT32DirGetUStat, mpeos_fileFAT32DirSetUStat };
