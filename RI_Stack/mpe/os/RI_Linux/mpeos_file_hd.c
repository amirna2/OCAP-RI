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

#include <string.h>
#include <sys/types.h> /* fstat(2),lseek(2),open(2) */
#include <sys/stat.h>  /* fstat(2),open(2) */
#include <fcntl.h>     /* open(2) */
#include <unistd.h>    /* close(2),lseek(2),read(2) */
#include <dirent.h>     // directory stuff
#include <errno.h>
#include <inttypes.h>

#include <mpe_file.h>
#include <mpe_error.h>
#include <mpeos_file.h>
#include <mpeos_mem.h>
#include <mpeos_time.h>
#include <mpeos_dbg.h>
#include <mpeos_mem.h>

#define MPE_MEM_DEFAULT MPE_MEM_FILE

#define MAX_OPEN_DIRECTORIES 10

mpeos_filesys_ftable_t port_fileHDFTable;

typedef struct
{
    mpe_Bool opened; // 1=yes 0=no
    char path[MPE_FS_MAX_PATH + 1]; // place to keep the name of the directory
    DIR * fsHandle; // file system handle from opendur
} DirHandle;

static DirHandle gOpenDirs[MAX_OPEN_DIRECTORIES];

static DirHandle *getDirHandle(DIR* dp, char *path)
{
    int i;

    for (i = 0; i < MAX_OPEN_DIRECTORIES; i++)
    {
        if (gOpenDirs[i].opened == FALSE)
        {
            gOpenDirs[i].fsHandle = dp;
            gOpenDirs[i].opened = TRUE;
            strcpy(gOpenDirs[i].path, path);
            break;
        }
    }

    if (i == MAX_OPEN_DIRECTORIES)
        return NULL;

    return &gOpenDirs[i];
}

static void freeDirHandle(DirHandle *dp)
{
    dp->opened = FALSE;
}

typedef struct mpeos_fileHD
{
    int fd;
} mpeos_fileHD;

typedef struct mpeos_dirHD
{
    int fd;
} mpeos_dirHD;

// MPEOS private function called by other mpeos modules for this port
// Ensures that the full path specified exists
int createFullPath(const char* pathname)
{
    char mypath[MPE_FS_MAX_PATH];
    const char* walker = pathname;
    mpe_Error err;

    // Skip past the root
    if (pathname[0] == MPE_FS_SEPARATION_CHAR)
        walker++;

    // Walk the string looking for path elements and ensure that each
    // is created
    while ((walker = strchr(walker, MPE_FS_SEPARATION_CHAR)) != NULL)
    {
        strncpy(mypath, pathname, walker - pathname);
        mypath[walker - pathname] = '\0';

        err = mpe_dirCreate(mypath);
        if (err != MPE_SUCCESS && err != MPE_FS_ERROR_ALREADY_EXISTS)
        {
            return -1;
        }
        walker++;
    }

    // Finally, create the last part of the path
    if (pathname[strlen(pathname) - 1] != MPE_FS_SEPARATION_CHAR)
    {
        err = mpe_dirCreate(pathname);
        if (err != MPE_SUCCESS && err != MPE_FS_ERROR_ALREADY_EXISTS)
        {
            return -1;
        }
    }

    return 0;
}

/*
 * Public Functions
 */

/*
 * Some filesystems require that we prefix the original pathname
 * (e.g., "/itfs").  This is not necessary (at this time) for this
 * POSIX1 implementation.
 */
mpe_Error convertName(const char *name, char **newName)
{
    char *full_name;
    int mpeRet;
    size_t newNameSz;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:convertName() name: '%s'\n", name);

    newNameSz = strlen(name) + 1;
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:convertName() allocating storage for %d characters\n",
            newNameSz);

    mpeRet = mpe_memAlloc(newNameSz, (void **) &full_name);
    if (MPE_SUCCESS != mpeRet)
    {
        return MPE_FS_ERROR_OUT_OF_MEM;
    }

    (void) strcpy(full_name, name);

    if ('/' == full_name[strlen(full_name) - 1])
    {
        full_name[strlen(full_name) - 1] = '\0';
    }

    *newName = full_name;
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:convertName() *newName: '%s'\n", *newName);
    return MPE_SUCCESS;
}

/*
 * File-Scope Functions.  Those that map to function-pointer
 * table entries are listed first and in the order in which
 * they appear in the function table.
 */

/*
 * <i>mpeos_fileHDInit()</i>
 *
 * Implementation complete.
 */
void mpeos_fileHDInit(const char *mountPoint)
{
    int i;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDInit() mountPoint: '%s'\n", mountPoint);

    for (i = 0; i < MAX_OPEN_DIRECTORIES; i++)
        gOpenDirs[i].opened = FALSE;
}

/*
 * <i>mpeos_fileHDFileOpen()</i>
 *
 * Opens a file from a writable persistent file system.</br>
 * Wraps POSIX.1 <i>open(2)</i> function.
 *
 * @param name Pathname of the file to be opened.
 * @param openMode Type of access requested (read, write, create, truncate, append).
 * @param handle Upon success, points to the opened file.
 *
 * @return The error code if the operation fails, otherwise
 *     <i>MPE_FS_ERROR_SUCCESS</i> is returned.  Note well that an error code
 *     is returned if the specified file is a directory.  In that case,
 *     <i>mpeos_fileHDDirOpen()</i> should be used to open the directory.
 */
mpe_FileError mpeos_fileHDFileOpen(const char* name, mpe_FileOpenMode openMode,
        mpe_File* handle)
{
    struct stat buf;
    mpe_Error ec;
    char *full_name;
    mpeos_fileHD *hdHandle;
    int localFd;
    int oflag = 0;
    int statRet;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileOpen()     name: '%s'\n", name);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileOpen() openMode: 0x%x\n", openMode);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileOpen()   handle: %p (addr)\n", handle);

    ec = convertName(name, &full_name);
    if (MPE_SUCCESS != ec)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileOpen() convertName() failed: %d\n",
                ec);
        return ec;
    }

    if (openMode & MPE_FS_OPEN_MUST_CREATE)
    {
        oflag |= O_CREAT;
        oflag |= O_EXCL;
    }
    else if (openMode & MPE_FS_OPEN_CAN_CREATE)
    {
        oflag |= O_CREAT;
    }

    if ((openMode & MPE_FS_OPEN_READWRITE) || ((openMode & MPE_FS_OPEN_READ)
            && (openMode & MPE_FS_OPEN_WRITE)))
    {
        oflag |= O_RDWR;
    }
    else if (openMode & MPE_FS_OPEN_WRITE)
    {
        oflag |= O_WRONLY;
    }
    else
    {
        oflag |= O_RDONLY;
    }

    if (openMode & MPE_FS_OPEN_TRUNCATE)
    {
        oflag |= O_TRUNC;
    }

    if (openMode & MPE_FS_OPEN_APPEND)
    {
        oflag |= O_APPEND;
    }

    ec = mpe_memAlloc(sizeof(mpeos_fileHD), (void **) &hdHandle);
    if (MPE_SUCCESS != ec)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileOpen() MPE_FS_ERROR_OUT_OF_MEM\n");
        mpe_memFree(full_name);
        return MPE_FS_ERROR_OUT_OF_MEM;
    }

    localFd = open((const char *) full_name, oflag, S_IRUSR | S_IWUSR | S_IRGRP
            | S_IWGRP | S_IROTH | S_IWOTH);
    mpe_memFree(full_name);
    if (-1 == localFd)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileOpen() open(%s) failed errno: %d\n",
                name, errno);
        mpe_memFree(hdHandle);
        if (errno == EEXIST)
            return MPE_FS_ERROR_ALREADY_EXISTS;

        return MPE_FS_ERROR_FAILURE;
    }

    /*
     * We must return an error condition if the specified file is
     * a directory.  This is not explicitly stated in any document
     * I can find, but appears to be a "de facto" requirement
     * (confirmed by phone conversation with Tom Brasier 051116).
     *
     * If the call to fstat() fails, we ignore the error.
     */
    statRet = fstat(localFd, &buf);
    if (-1 == statRet)
    {
        MPEOS_LOG(
                MPE_LOG_INFO,
                MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileOpen() dir-check fstat() failed; ignoring error ...\n");
    }
    else
    {
        if (S_ISDIR(buf.st_mode))
        {
            MPEOS_LOG(
                    MPE_LOG_TRACE1,
                    MPE_MOD_FILESYS,
                    "HDFILESYS:mpeos_fileHDFileOpen() specified file is a directory; return error code ...\n");
            close(localFd);
            mpe_memFree(hdHandle);
            return MPE_FS_ERROR_FAILURE;
        }
    }

    hdHandle->fd = localFd;
    *handle = (mpe_File *) hdHandle;

    // Make sure that the file descriptor is not inherited by child processes
    if (fcntl(localFd, F_SETFD, FD_CLOEXEC) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                  "HDFILESYS:mpeos_fileHDFileOpen() failed to set FD_CLOEXEC flag on fd: %d\n",
                  localFd);
        return MPE_FS_ERROR_FAILURE;
    }

    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileOpen() open successful (descriptor %d)\n",
            hdHandle->fd);

    return MPE_FS_ERROR_SUCCESS;
}

/*
 * <i>mpeos_fileHDFileClose</i>
 *
 * Closes a file previously opened with <i>mpeos_fileHDFileOpen()</i>.</br>
 * Wraps POSIX.1 <i>close(2)</i> function.
 *
 * @param handle  MPEOS HD file handle, which must have been provided by
 *                a previous call to <i>mpeos_fileHDFileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise
 *     <i>MPE_FS_ERROR_SUCCESS</i> is returned.
 */
mpe_FileError mpeos_fileHDFileClose(mpe_File handle)
{
    int closeRet;
    int mpeRet;
    mpeos_fileHD *hdHandle = (mpeos_fileHD*) handle;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileClose() handle: %p (addr)\n", handle);

    if (NULL == handle)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileClose() MPE_FS_ERROR_INVALID_PARAMETER (NULL handle)\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    closeRet = close(hdHandle->fd);

    mpeRet = mpe_memFree(handle);
    if (MPE_SUCCESS != mpeRet)
    {
        return MPE_FS_ERROR_FAILURE;
    }

    if (-1 == closeRet)
    {
        return MPE_FS_ERROR_FAILURE;
    }

    handle = NULL;
    return MPE_FS_ERROR_SUCCESS;
}

/*
 * <i>mpeos_fileHDFileRead</i>
 *
 * Reads data from specified hard-disk file.</br>
 * Wraps POSIX.1 <i>read(2)</i> function.
 *
 * @param handle  MPEOS HD file handle, which must have been provided by
 *                a previous call to <i>mpeos_fileHDFileOpen()</i>.
 * @param count  A pointer to a byte-count.  On entry, this must point to
 *               the number of bytes to attempt to read.  Upon return,
 *               this will point to the number of bytes actually read.
 * @param buffer A pointer to the buffer that will receive the data.
 *               The buffer size must be greater than or equal to the
 *               entry-point value of <i>count</i>.
 *
 *
 */
mpe_FileError mpeos_fileHDFileRead(mpe_File handle, uint32_t *count,
        void* buffer)
{
    int bytesRead = 0;
    mpeos_fileHD *hdHandle = (mpeos_fileHD*) handle;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileRead() handle: %p (addr)\n", handle);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileRead()  count: %p (addr)\n", count);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileRead() buffer: %p (addr)\n", buffer);

    if ((NULL == handle) || (NULL == count) || (NULL == buffer))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileRead() MPE_FS_ERROR_INVALID_PARAMETER\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    if (0 == *count)
    {
        return MPE_FS_ERROR_SUCCESS;
    }

    bytesRead = read(hdHandle->fd, buffer, (unsigned int) *count);
    if (-1 == bytesRead)
    {
        if (EISDIR == errno)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "HDFILESYS:mpeos_fileHDFileRead() read() from a directory ... (errno: %d) ... do not treat as an error ...\n",
                    errno);
            bytesRead = 0;
        }
        else
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "HDFILESYS:mpeos_fileHDFileRead() read() failed (errno: %d)\n",
                    errno);
            return MPE_FS_ERROR_DEVICE_FAILURE;
        }
    }
    *count = (uint32_t) bytesRead;
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileRead() successful read (*count: %d)\n",
            *count);
    return MPE_FS_ERROR_SUCCESS;
}

/*
 *  - <i>mpeos_fileHDFileWrite</i>
 */
mpe_FileError mpeos_fileHDFileWrite(mpe_File handle, uint32_t *count,
        void* buffer)
{
    int bytesWritten = 0;
    mpeos_fileHD *hdHandle = (mpeos_fileHD*) handle;
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;

    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileWrite\n\thandle:%p (addr)\n\tcount: %p (addr)\n\tbuffer: %p (addr)\n",
            handle, count, buffer);

    if ((NULL == handle) || (NULL == count) || (NULL == buffer))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileWrite() MPE_FS_ERROR_INVALID_PARAMETER\n");
        retval = MPE_FS_ERROR_INVALID_PARAMETER;
    }
    else if (0 != *count)
    {
        bytesWritten = write(hdHandle->fd, buffer, (unsigned int) *count);

        if (-1 != bytesWritten)
        {
            *count = (uint32_t) bytesWritten;
            MPEOS_LOG(
                    MPE_LOG_TRACE1,
                    MPE_MOD_FILESYS,
                    "HDFILESYS:mpeos_fileHDFileWrite() successful write (*count: %d)\n",
                    *count);
        }
        else
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILESYS,
                    "HDFILESYS:mpeos_fileHDFileWrite() write failed (errno: %d)\n",
                    errno);
            *count = 0;
            retval = MPE_FS_ERROR_DEVICE_FAILURE;
        }
    }
    return (retval);
}

/*
 * <i>mpeos_fileHDFileSeek</i>
 *
 * Repositions read/write file offset.</br>
 * Wraps POSIX.1 <i>lseek(2)</i> function.
 *
 * @param handle  MPEOS HD file handle, which must have been provided by
 *                a previous call to <i>mpeos_fileHDFileOpen()</i>.
 * @param seekMode  One of:  <i>MPE_FS_SEEK_CUR</i>, <i>MPE_FS_SEEK_END</i>,
 <i>MPE_FS_SEEK_SET</i>.
 * @param offset  New offset.
 *
 * @return The error code if the operation fails, otherwise
 *         <i>MPE_FS_ERROR_SUCCESS</i> is returned.
 */
mpe_FileError mpeos_fileHDFileSeek(mpe_File handle, mpe_FileSeekMode seekMode,
        int64_t* offset)
{
    mpeos_fileHD *hdHandle = (mpeos_fileHD*) handle;
    off_t lseekOffset = 0;
    off_t lseekRet;
    int lseekWhence;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileSeek()      handle: %p (addr)\n", handle);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileSeek()    seekMode: 0x%x\n", seekMode);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileSeek()      offset: %p (addr)\n", offset);

    if (NULL == handle)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileSeek() MPE_FS_ERROR_INVALID_PARAMETER (NULL handle)\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    switch (seekMode)
    {
    case MPE_FS_SEEK_CUR:
        lseekOffset = (off_t) *offset;
        lseekWhence = SEEK_CUR;
        break;
    case MPE_FS_SEEK_END:
        lseekWhence = SEEK_END;
        break;
    case MPE_FS_SEEK_SET:
        lseekOffset = (off_t) * offset;
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileSeek() current offset: 0x%"PRIx64"\n",
                *offset);
        lseekWhence = SEEK_SET;
        break;
    default:
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileSeek() MPE_FS_ERROR_INVALID_PARAMETER (invalid seekMode)\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    lseekRet = lseek(hdHandle->fd, lseekOffset, lseekWhence);
    if ((off_t) - 1 == lseekRet)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileSeek() lseek() failed (errno: %d)\n",
                errno);
        return MPE_FS_ERROR_DEVICE_FAILURE;
    }

    *offset = lseekRet;
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileSeek() new offset: 0x%"PRIx64"\n", *offset);

    return MPE_FS_ERROR_SUCCESS;
}

/*
 * TODO - <i>mpeos_fileHDFileSync</i>
 */

mpe_FileError mpeos_fileHDFileSync(mpe_File handle)
{
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;

    printf("HDFILESYS:mpeos_fileHDFileSync() STUB\n");
    return (retval);
}

/*
 * TODO - <i>mpeos_fileHDFileGetStat</i>
 *
 * Gets information about the specified file.</br>
 * Wraps POSIX.1 <i>stat(2)</i> function.
 *
 * @param fileName  Pathname of file to research
 * @param mode  Must be:  TBD
 * @param info A pointer to storage for file status information.
 *
 * @return The error code if the operation fails, otherwise
 *         <i>MPE_FS_ERROR_SUCCESS</i> is returned.
 */
mpe_FileError mpeos_fileHDFileGetStat(const char* fileName,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    struct stat buf;
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetStat() fileName: %s\n", fileName);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetStat()     mode: 0x%x\n", mode);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetStat()     info: %p (addr)\n", info);

    if (fileName == NULL || info == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileGetStat() MPE_FS_ERROR_INVALID_PARAMETER\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // Ensure the file or directory exists
    if (stat(fileName, &buf) == -1)
        return MPE_FS_ERROR_NOT_FOUND;

    switch ((int) mode)
    {
    case MPE_FS_STAT_TYPE:
        if (S_ISDIR(buf.st_mode))
            info->type = MPE_FS_TYPE_DIR;
        else if (S_ISREG(buf.st_mode))
            info->type = MPE_FS_TYPE_FILE;
        else
            info->type = MPE_FS_TYPE_OTHER;
        break;

    case MPE_FS_STAT_DEFAULTSYSDIR:
        printf(
                "HDFILESYS:mpeos_fileHDFileGetStat STUB: mode: MPE_FS_STAT_DEFAULTSYSDIR (%d)\n",
                mode);
        break;

    case MPE_FS_STAT_SIZE:
        info->size = (uint64_t) buf.st_size;
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileGetStat() info->size: %"PRId64" (dec)\n",
                info->size);
        break;

    case MPE_FS_STAT_MODDATE:
        info->modDate = buf.st_mtime;
        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileGetStat()   info->modDate: 0x%lu\n",
                info->modDate);
        break;

    case MPE_FS_STAT_CREATEDATE:
        info->createDate = buf.st_ctime;
        MPEOS_LOG(
                MPE_LOG_TRACE1,
                MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileGetStat()   info->createDate: 0x%lu\n",
                info->createDate);
        break;

    default:
        printf("HDFILESYS:mpeos_fileHDFileGetStat STUB: unknown mode: %d\n",
                mode);
        retval = MPE_FS_ERROR_UNSUPPORT;
        break;
    }

    return (retval);
}

/*
 * TODO - <i>mpeos_fileHDFileSetFStat</i>
 */
mpe_FileError mpeos_fileHDFileSetFStat(mpe_File handle, mpe_FileStatMode mode,
        mpe_FileInfo *info)
{
    mpeos_fileHD *hdHandle = (mpeos_fileHD*) handle;

    if (info == NULL || hdHandle == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileSetFStat() MPE_FS_ERROR_INVALID_PARAMETER\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    switch (mode)
    {
    /* set file size */
    case MPE_FS_STAT_SIZE:
        if (ftruncate64(hdHandle->fd, info->size) == -1)
            return MPE_FS_ERROR_FAILURE;
        break;

    default:
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileSetFStat() Unsupported stat (%d)\n",
                mode);
        return MPE_FS_ERROR_FAILURE;
    }

    return MPE_FS_ERROR_SUCCESS;
}

/*
 * TODO - <i>mpeos_fileHDFileSetStat</i>
 */
mpe_FileError mpeos_fileHDFileSetStat(const char* fileName,
        mpe_FileStatMode mode, mpe_FileInfo *info)
{
    mpe_FileError err = MPE_FS_ERROR_SUCCESS;
    struct stat buf;

    if (fileName == NULL || info == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileGetStat() MPE_FS_ERROR_INVALID_PARAMETER\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    // Ensure the file or directory exists
    if (stat(fileName, &buf) == -1)
        return MPE_FS_ERROR_NOT_FOUND;

    switch (mode)
    {
    /* set file type */
    case MPE_FS_STAT_TYPE:
        /* The user should not be able to set this */
        err = MPE_FS_ERROR_READ_ONLY;
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
        if ((err = mpeos_fileHDFileOpen(fileName, MPE_FS_OPEN_READ, &h))
                == MPE_FS_ERROR_SUCCESS)
        {
            /* call SetFStat */
            err = mpeos_fileHDFileSetFStat(h, mode, info);
            (void) mpeos_fileHDFileClose(h);
        }
    }
        break;
    }

    return err;
}

/*
 * <i>mpeos_fileHDFileGetFStat</i>
 *
 * Gets the size of the specified file.</br>
 * Wraps POSIX.1 <i>fstat(2)</i> function.
 *
 * @param handle  MPEOS HD file handle, which must have been provided by
 *                a previous call to <i>mpeos_fileHDFileOpen()</i>.
 * @param mode  Must be:  <i>MPE_FS_STAT_SIZE</i>.
 * @param info A pointer to storage for file status information.
 *
 * @return The error code if the operation fails, otherwise
 *         <i>MPE_FS_ERROR_SUCCESS</i> is returned.
 */
mpe_FileError mpeos_fileHDFileGetFStat(mpe_File handle, mpe_FileStatMode mode,
        mpe_FileInfo *info)
{
    struct stat buf;
    mpeos_fileHD *hdHandle = (mpeos_fileHD*) handle;
    int statRet;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat() handle: %p (addr)\n", handle);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()   mode: 0x%x\n", mode);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()   info: %p (addr)\n", info);

    if ((NULL == handle) || (NULL == info))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDFileGetFStat() MPE_FS_ERROR_INVALID_PARAMETER\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    if (MPE_FS_STAT_SIZE != mode)
    {
        return MPE_FS_ERROR_UNSUPPORT;
    }

    statRet = fstat(hdHandle->fd, &buf);
    if (-1 == statRet)
    {
        return MPE_FS_ERROR_FAILURE;
    }

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()     buf.st_dev: major(0x%x) minor(0x%x)\n",
              major(buf.st_dev), minor(buf.st_dev));
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()     buf.st_ino: 0x%lx\n",
            buf.st_ino);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()    buf.st_mode: 0x%x\n",
            buf.st_mode);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()   buf.st_nlink: 0x%x\n",
            buf.st_nlink);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()     buf.st_uid: 0x%x\n",
            buf.st_uid);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()     buf.st_gid: 0x%x\n",
            buf.st_gid);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()    buf.st_rdev: major(0x%x) minor(0x%x)\n",
              major(buf.st_rdev), minor(buf.st_rdev));
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()    buf.st_size: %lu (dec)\n",
            buf.st_size);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()   buf.st_atime: 0x%lx\n",
            buf.st_atime);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()   buf.st_mtime: 0x%lx\n",
            buf.st_mtime);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()   buf.st_ctime: 0x%lx\n",
            buf.st_ctime);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat() buf.st_blksize: 0x%lx\n",
            buf.st_blksize);
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat()  buf.st_blocks: 0x%lx\n",
            buf.st_blocks);

    info->size = (uint64_t) buf.st_size;
    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileGetFStat() successful info->size: %llu (dec)\n",
            info->size);
    return MPE_FS_ERROR_SUCCESS;
}

/*
 * TODO - <i>mpeos_fileHDFileDelete</i>
 */

mpe_FileError mpeos_fileHDFileDelete(const char* fileName)
{
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;
    if (0 == fileName)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS::mpeos_fileHDFileDelete - NULL fileName!\n");
        return MPE_FS_ERROR_INVALID_PARAMETER;
    }

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileDelete() fileName: \"%s\"\n", fileName);
    errno = 0;
    if (0 != unlink(fileName))
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILESYS,
                "HDFILESYS::mpeos_fileHDFileDelete - unlink() set errno %d\n",
                errno);
        retval = MPE_FS_ERROR_FAILURE;
    }
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileDelete(\"%s\") returns: %d\n", fileName,
            retval);

    return (retval);
}

/*
 * TODO - <i>mpeos_fileHDFileRename</i>
 */
mpe_FileError mpeos_fileHDFileRename(const char* oldName, const char* newName)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDFileRename(<%s>,<%s>\n", oldName, newName);

    if (-1 == rename(oldName, newName))
    {
        int ccode = errno;
        switch (ccode)
        {
        default:
            return MPE_FS_ERROR_INVALID_PARAMETER;

        case EBUSY:
        case ENOSPC:
            return MPE_FS_ERROR_DEVICE_FAILURE;

        case ENOENT:
        case ENOTDIR:
            return MPE_FS_ERROR_NOT_FOUND;

        case ENOMEM:
            return MPE_FS_ERROR_OUT_OF_MEM;

        }
    }

    return MPE_FS_ERROR_SUCCESS;
}

///////////////////////////////////////////////////////////////////////////////////////////////////////

/*
 * <i>mpeos_fileHDDirOpen()</i>
 *
 * Opens a directory from a writable persistent file system.</br>
 * Wraps POSIX.1 <i>open(2)</i> function.
 *
 * @param name Pathname of the directory to be opened.
 * @param handle Upon success, points to the opened file.
 *
 * @return The error code if the operation fails, otherwise
 *     <i>MPE_FS_ERROR_SUCCESS</i> is returned.
 */
mpe_FileError mpeos_fileHDDirOpen(const char *name, mpe_Dir* handle)
{
    mpe_Error retval = MPE_FS_ERROR_SUCCESS;
    int ccode;
    char *full_name = 0;
    DIR *pDir = 0;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDDirOpen() name:<%s> handle@%p\n", name,
            handle);

    retval = convertName(name, &full_name);
    if (MPE_SUCCESS == retval)
    {
        if ((pDir = opendir(full_name)) != 0)
        {
            // 20061206 - ABD fixed bug while working on BCM97456 DVR Playback
            //            mpeos_memFreeP(MPE_MEM_FILE, full_name);
            // 20061206 - ABD fixed bug while working on BCM97456 DVR Playback
            *handle = (mpe_File *) getDirHandle(pDir, full_name);
            if (*handle == 0)
            {
                MPEOS_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "HDFILESYS:mpeos_fileHDDirOpen() getDirHandle failed - %d concurrently open directories max\n",
                        MAX_OPEN_DIRECTORIES);
                retval = MPE_FS_ERROR_OUT_OF_MEM;
            }
            else
            {
                MPEOS_LOG(
                        MPE_LOG_TRACE1,
                        MPE_MOD_FILESYS,
                        "HDFILESYS:mpeos_fileHDDirOpen() open successful (descriptor %p)\n",
                        *handle);
            }
        }
        else
        {
            ccode = errno;
            switch (ccode)
            {
            case ENOTDIR:
            case ENOENT:
                retval = MPE_FS_ERROR_NOT_FOUND;
                break;

            case ENOMEM:
                retval = MPE_FS_ERROR_OUT_OF_MEM;
                break;

            case EACCES:
            case EMFILE:
            case ENFILE:
            default:
                retval = MPE_FS_ERROR_DEVICE_FAILURE;
                break;
            }

            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,

            "HDFILESYS:mpeos_fileHDDirOpen() diropen(%s) failed: %d\n",
                    full_name, errno);
        }

        mpe_memFree(full_name);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDDirOpen() convertName() failed: %d\n",
                retval);
    }

    return (retval);
}

mpe_FileError mpeos_fileHDDirRead(mpe_Dir handle, mpe_DirEntry* dirEnt)
{
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;
    struct dirent *pde = 0;
    DirHandle *dhp = (DirHandle *) handle;
    struct stat fileStat;
    int ccode;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDDirRead() handle@%p\n", handle);

    /***
     *** 20061213 Porting-Projects BUG#16
     *** NOTE WELL: when returning the filename to the caller, we must remove
     ***     the directory prefix required by the call to stat(2)
     ***/

    // prep the path for the full filename
    // first, check that dirEnt->name has enough space
    if ((strlen(dhp->path) + strlen(MPE_FS_SEPARATION_STRING) + 1 /* null terminator */) <= sizeof (dirEnt->name))
    {
        strcpy(dirEnt->name, dhp->path);
        strcat(dirEnt->name, MPE_FS_SEPARATION_STRING);
    }
    else
    {
        dirEnt->name[0] = '\0';
        dirEnt->fileSize = 0;
        dirEnt->isDir = FALSE;

        return MPE_FS_ERROR_OUT_OF_MEM;
    }


    while (1)
    {
        pde = readdir((DIR*) dhp->fsHandle); // get the next file from this directory
        if (pde > 0)
        {
            /* Skip .. and . entries */
            /* Also skip persistent file attributes file */
            if (strcmp(pde->d_name, "..") == 0 || strcmp(pde->d_name, ".") == 0)
            {
                continue;
            }

            // check that dirEnt->name can fit the entire path
            if ((strlen(dirEnt->name) + strlen(pde->d_name) + 1 /* null terminator */) > sizeof (dirEnt->name))
            {
                retval = MPE_FS_ERROR_OUT_OF_MEM;
                break;
            }

            strcat(dirEnt->name, pde->d_name); // add the filename to the path for a complete path

            ccode = stat(dirEnt->name, &fileStat);
            if (ccode == 0)
            {
                dirEnt->fileSize = fileStat.st_size;
                dirEnt->isDir = S_ISDIR(fileStat.st_mode) ? TRUE : FALSE;
                break;
            }
            else
            {
                // failed to stat the file
                ccode = errno;
                switch (ccode)
                {
                case EBADF:
                case EFAULT:
                case ELOOP:
                case ENAMETOOLONG:
                case ENOTDIR:
                case ENOENT:
                    retval = MPE_FS_ERROR_INVALID_PARAMETER;
                    break;

                case ENOMEM:
                    retval = MPE_FS_ERROR_OUT_OF_MEM;
                    break;

                default:
                    retval = MPE_FS_ERROR_DEVICE_FAILURE;
                    break;

                }
                MPEOS_LOG(
                        MPE_LOG_ERROR,
                        MPE_MOD_FILESYS,
                        "HDFILESYS:mpeos_fileHDDirRead() stat <%s> failed %d\n",
                        dirEnt->name, ccode);

                break;  // exit the while loop
            }
        }
        else
        {
            retval = MPE_FS_ERROR_NOT_FOUND;
            break;
        }
    }

    if (retval == MPE_FS_ERROR_SUCCESS)
    {
        dirEnt->name[0] = '\0';
        strcat(dirEnt->name, pde->d_name); // caller receives only the "short" filename
    }
    else
    {
        dirEnt->name[0] = '\0';
        dirEnt->fileSize = 0;
        dirEnt->isDir = FALSE;
    }

    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDDirRead(\"%s\") returns dirEnt->name: \"%s\"\n",
            dhp->path, dirEnt->name);

    return (retval);
}

/*
 * <i>mpeos_fileHDDirClose</i>
 *
 * Closes a directory previously opened with <i>mpeos_fileHDDirOpen()</i>.</br>
 * Wraps POSIX.1 <i>close(2)</i> function.
 *
 * @param handle  MPEOS HD file handle, which must have been provided by
 *                a previous call to <i>mpeos_fileHDDirOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise
 *     <i>MPE_FS_ERROR_SUCCESS</i> is returned.
 */
mpe_FileError mpeos_fileHDDirClose(mpe_Dir handle)
{
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;
    DirHandle *dhp = (DirHandle *) handle;
    int ccode;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDDirClose() handle: %p (addr)\n", handle);

    ccode = closedir(dhp->fsHandle);
    if (ccode == 0)
    {
        freeDirHandle(dhp);
    }
    else
    {
        retval = MPE_FS_ERROR_INVALID_PARAMETER;
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILESYS,
                "HDFILESYS:mpeos_fileHDDirClose() failed\n");
    }

    return (retval);
}

/*
 * <i>mpeos_fileHDDirDelete</i>
 *
 */
mpe_FileError mpeos_fileHDDirDelete(const char* dirName)
{
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;
    char *full_name = 0;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDDirDelete<%s>\n", dirName);

    retval = convertName(dirName, &full_name);

    if (rmdir(full_name) != 0)
    {
        int errCode = errno;

        if (errCode != ENOENT)
        {
            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
                    "HDFILESYS:mpeos_fileHDDirDelete<%s> -- error = %d\n",
                    dirName, errCode);

            retval = MPE_FS_ERROR_FAILURE;
        }
    }

    mpe_memFree(full_name);

    return (retval);
}

mpe_FileError mpeos_fileHDDirRename(const char* oldName, const char* newName)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDDirRename(<%s>,<%s>\n", oldName, newName);
    return mpeos_fileHDFileRename(oldName, newName);
}

mpe_FileError mpeos_fileHDDirCreate(const char* dirName)
{
    mpe_FileError retval = MPE_FS_ERROR_SUCCESS;
    char *full_name = 0;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_FILESYS,
            "HDFILESYS:mpeos_fileHDDirCreate <%s>\n", dirName);

    retval = convertName(dirName, &full_name);
    if (retval == MPE_SUCCESS)
    {
        if (-1 == mkdir(full_name, 0755))
        {
            int ccode = errno;
            switch (ccode)
            {
            case ENOSPC:
                retval = MPE_FS_ERROR_DEVICE_FAILURE;
                break;

            case ENOENT:
            case ENOTDIR:
                retval = MPE_FS_ERROR_NOT_FOUND;
                break;

            case ENOMEM:
                retval = MPE_FS_ERROR_OUT_OF_MEM;
                break;

            case EEXIST:
                retval = MPE_FS_ERROR_ALREADY_EXISTS;
                break;

            default:
                retval = MPE_FS_ERROR_INVALID_PARAMETER;
                break;

            } // end error switch
        }
        mpe_memFree(full_name);
    }

    return (retval);
}

mpe_FileError mpeos_fileHDDirMount(const mpe_DirUrl *dirUrl)
{
    return MPE_FS_ERROR_UNKNOWN_URL;
}

mpe_FileError mpeos_fileHDDirUnmount(const mpe_DirUrl *dirUrl)
{
    return MPE_FS_ERROR_UNKNOWN_URL;
}

mpe_FileError mpeos_fileHDDirGetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    return MPE_FS_ERROR_UNKNOWN_URL;
}

mpe_FileError mpeos_fileHDDirSetUStat(const mpe_DirUrl *dirUrl,
        mpe_DirStatMode mode, mpe_DirInfo *info)
{
    return MPE_FS_ERROR_UNKNOWN_URL;
}

mpeos_filesys_ftable_t port_fileHDFTable =
{ mpeos_fileHDInit, mpeos_fileHDFileOpen, mpeos_fileHDFileClose,
        mpeos_fileHDFileRead, mpeos_fileHDFileWrite, mpeos_fileHDFileSeek,
        mpeos_fileHDFileSync, mpeos_fileHDFileGetStat, mpeos_fileHDFileSetStat,
        mpeos_fileHDFileGetFStat, mpeos_fileHDFileSetFStat,
        mpeos_fileHDFileDelete, mpeos_fileHDFileRename, mpeos_fileHDDirOpen,
        mpeos_fileHDDirRead, mpeos_fileHDDirClose, mpeos_fileHDDirDelete,
        mpeos_fileHDDirRename, mpeos_fileHDDirCreate, mpeos_fileHDDirMount,
        mpeos_fileHDDirUnmount, mpeos_fileHDDirGetUStat,
        mpeos_fileHDDirSetUStat };
