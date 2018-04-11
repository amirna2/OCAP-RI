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

#include <mpe_file.h>
#include <mpe_dbg.h>
#include "javavm/include/errno_md.h"
#include "javavm/include/porting/io.h"
#include "javavm/include/porting/int.h"
#include "javavm/include/porting/sync.h"
#include "javavm/include/porting/threads.h"
#include "javavm/include/porting/doubleword.h"
#include "javavm/include/porting/globals.h"
#include "javavm/include/utils.h"

#include <stdlib.h>

/****
 *
 * posix_to_mpe_mode - map POSIX file permissions to MPE permissions.
 *
 * IN:
 * @param oflag - POSIX file access permissions
 *
 * OUT:
 * @return MPE mapped file access permissions
 *
 * DESCRIPTION:
 * The POSIX access bit pattern is mapped to the MPE pattern.  POSIX says
 * that read, write and read/write are all mutually exclusive, hence the
 * conditional statement starting with read/write.
 */
static uint32_t posix_to_mpe_mode(int oflag)
{
    uint32_t mode = 0;

    /* Check read/write first. */
    if ((oflag & O_ACCMODE) == O_RDWR)
        mode |= MPE_FS_OPEN_READWRITE;
    else if ((oflag & O_ACCMODE) == O_RDONLY)
        mode |= MPE_FS_OPEN_READ;
    else if ((oflag & O_ACCMODE) == O_WRONLY)
        mode |= MPE_FS_OPEN_WRITE;

    /* Check for file creattion. */
    if (oflag & O_CREAT)
        mode |= ((oflag & O_EXCL) ? MPE_FS_OPEN_MUST_CREATE
                : MPE_FS_OPEN_CAN_CREATE);

    /* Check for file truncation. */
    if (oflag & O_TRUNC)
        mode |= MPE_FS_OPEN_TRUNCATE;

    /* Check for file append. */
    if (oflag & O_APPEND)
        mode |= MPE_FS_OPEN_APPEND;

    return mode;
}

static inline mpe_File fd2mpeFile(CVMInt32 fd)
{
    return (mpe_File)(fd << 1);
}

char*
CVMioNativePath(char* path)
{
    return path;
}

/**
 * <i>CVMioSync</i>
 *
 * Synchronize the file descriptor's in memory state with that of the
 * physical device.  NOTE: JVM_ version of this used to throw an error.
 *
 * @param fd is the file descriptor to synchronize from.
 *
 * @return value of -1 is an error, 0 on success.
 */
CVMInt32 CVMioSync(CVMInt32 fd)
{
    mpe_FileError ec;
    if ((ec = mpe_fileSync(fd2mpeFile(fd))) != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return -1;
    }
    return 0;
}

/**
 * <i>CVMioFileType</i>
 *
 * Retrieve the file type at the given file
 *
 * @param path is the path to the file
 *
 * @return CVM_IO_FILETYPE_REGULAR or CVM_IO_FILETYPE_DIRECTORY on
 *  success and CVM_IO_ERR on failure.
 */
CVMInt32 CVMioFileType(const char *path)
{
    CVMInt32 type = CVM_IO_ERR;
    mpe_File file;
    mpe_Dir dir;
    mpe_FileError ec;

    if ((ec = mpe_fileOpen(path, MPE_FS_OPEN_READ, &file))
            == MPE_FS_ERROR_SUCCESS)
    {
        /* this is a regular file */
        type = CVM_IO_FILETYPE_REGULAR;
        mpe_fileClose(file);
    }
    else if ((ec = mpe_dirOpen(path, &dir)) == MPE_FS_ERROR_SUCCESS)
    {
        /* this is a directory */
        type = CVM_IO_FILETYPE_DIRECTORY;
        mpe_dirClose(dir);
    }
    else
        /* don't know what we have */
        CVMsetErrno(ec);

    return type;
}

CVMInt32 CVMioOpen(const char *name, CVMInt32 openMode, CVMInt32 filePerm)
{
    mpe_FileError ec;
    mpe_File fd;
    CVMInt32 retFD;

    if ((ec = mpe_fileOpen(name, posix_to_mpe_mode(openMode), &fd))
            != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return -1;
    }

    // Ensure that mpe_File conforms to our contract
    if ((CVMInt32) fd & 1)
    {
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**********************************************************************************\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**********************************************************************************\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**********************************************************************************\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**                                                                              **\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "** mpe_fileOpen must always return an mpe_File with least significant bit == 0! **\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**                                                                              **\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**********************************************************************************\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**********************************************************************************\n");
        MPE_LOG(
                MPE_LOG_FATAL,
                MPE_MOD_JVM,
                "**********************************************************************************\n");
        exit(1);
    }

    retFD = (CVMInt32)(((CVMUint32) fd) >> 1);
    return retFD;
}

/**
 * <i>CVMioWrite()</i>
 *
 * Write data from an untyped buffer to a file decriptor.
 *
 * @param fd is the file descriptor to read from.
 * @param buf is the buffer from which to fetch the data.
 * @param nbytes is the number of bytes to write.
 *
 * @return This function returns -1 on error, and the number of bytes
 * written on success.
 */
CVMInt32 CVMioWrite(CVMInt32 fd, const void *buf, CVMUint32 nBytes)
{
    mpe_FileError ec;

    if ((ec = mpe_fileWrite(fd2mpeFile(fd), &nBytes, (void *) buf))
            != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return -1;
    }

    return nBytes;
}

CVMInt64 CVMioSeek(CVMInt32 fd, CVMInt64 offset, CVMInt32 whence)
{
    mpe_FileError ec;
    int8_t mpeWhence;

    /* Just to be safe map to MPE file positions. */
    switch (whence)
    {
    case SEEK_SET:
        mpeWhence = MPE_FS_SEEK_SET;
        break;
    case SEEK_END:
        mpeWhence = MPE_FS_SEEK_END;
        break;
    case SEEK_CUR:
        mpeWhence = MPE_FS_SEEK_CUR;
        break;
    default:
        return (-1);
    }

    /* Call MPE to perform seek operation. */
    if ((ec = mpe_fileSeek(fd2mpeFile(fd), mpeWhence, &offset))
            != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return (-1);
    }

    /* Return new position. */
    return offset;
}

CVMInt32 CVMioSetLength(CVMInt32 fd, CVMInt64 length)
{
    mpe_FileInfo info;
    mpe_FileError ec;

    info.size = length;
    if ((ec = mpe_fileSetFStat(fd2mpeFile(fd), MPE_FS_STAT_SIZE, &info))
            != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return -1;
    }
    return 0;
}

CVMInt32 CVMioAvailable(CVMInt32 fd, CVMInt64 *bytes)
{
    mpe_FileError ec;
    CVMInt64 current, end;

    current = CVMlongConstZero();
    end = CVMlongConstZero();

    if ((ec = mpe_fileSeek(fd2mpeFile(fd), MPE_FS_SEEK_CUR, &current))
            != MPE_FS_ERROR_SUCCESS || (ec = mpe_fileSeek(fd2mpeFile(fd),
            MPE_FS_SEEK_END, &end)) != MPE_FS_ERROR_SUCCESS || (ec
            = mpe_fileSeek(fd2mpeFile(fd), MPE_FS_SEEK_SET, &current))
            != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return 0;
    }

    *bytes = CVMlongSub(end, current);
    return 1;
}

CVMInt32 CVMioFileSizeFD(CVMInt32 fd, CVMInt64 *size)
{
    mpe_FileError ec;
    mpe_FileInfo info;

    if ((ec = mpe_fileGetFStat(fd2mpeFile(fd), MPE_FS_STAT_SIZE, &info))
            != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return -1;
    }
    *size = info.size;
    return 0;
}

CVMInt32 CVMioRead(CVMInt32 fd, void *buf, CVMUint32 nBytes)
{
    mpe_FileError ec;

    // WE DO NOT SUPPORT READING FROM STDIN AT THIS TIME
    /*
     if (fd <= STDERR_FILENO)
     {
     nBytes = fread(buf, sizeof(int8_t), nBytes, stdin);
     return nBytes;
     }
     */

    /* FIX: need to take a look at the conversion here */
    if ((ec = mpe_fileRead(fd2mpeFile(fd), &nBytes, buf))
            != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return -1;
    }

    return nBytes;
}

CVMInt32 CVMioClose(CVMInt32 fd)
{
    mpe_FileError ec;

    if (fd < 0)
        return 0;

    if ((ec = mpe_fileClose(fd2mpeFile(fd))) != MPE_FS_ERROR_SUCCESS)
    {
        CVMsetErrno(ec);
        return -1;
    }
    return 0;
}

/**
 * <i>CVMioReturnLastErrorString()</i>
 *
 * Return the error string for the last error that occurred.
 *
 * @return a pointer to the string or NULL or error.
 */
char *
CVMioReturnLastErrorString()
{
    char *str;

    switch (CVMgetErrno())
    {
    case MPE_FS_ERROR_SUCCESS:
        str = "No error condition.";
        break;
    case MPE_FS_ERROR_OUT_OF_MEM:
        str = "Insufficient memory available.";
        break;
    case MPE_FS_ERROR_INVALID_PARAMETER:
        str = "Invalid parameter.";
        break;
    case MPE_FS_ERROR_NOT_FOUND:
        str = "File not found.";
        break;
    case MPE_FS_ERROR_READ_ONLY:
        str = "File is read-only";
        break;
    case MPE_FS_ERROR_UNSUPPORT:
        str = "Unsupported operation.";
        break;
    case MPE_FS_ERROR_FAILURE:
        str = "Operation failed.";
        break;
    case MPE_FS_ERROR_UNKNOWN_URL:
        str = "URL is unknown.";
        break;
    case MPE_FS_ERROR_ALREADY_EXISTS:
        str = "File already exists.";
        break;
    case MPE_FS_ERROR_EOF:
        str = "File at EOF.";
        break;
    case MPE_FS_ERROR_TIMEOUT:
        str = "Operation timed out.";
        break;
    case MPE_FS_ERROR_DEVICE_FAILURE:
        str = "Device failed.";
        break;
    case MPE_FS_ERROR_INVALID_STATE:
        str = "Invalid state.";
        break;
    case MPE_FS_ERROR_NO_MOUNT:
        str = "Device not mounted";
        break;
    case MPE_FS_ERROR_EVENTS:
        str = "Event error";
        break;
    case MPE_FS_ERROR_NOTHING_TO_ABORT:
        str = "Nothing to abort";
        break;
    default:
        str = "Unknown error";
        break;
    }
    return str;
}

/**
 * <i>CVMioGetLastErrorString()</i>
 *
 * Get the error string for the last error that occurred.
 *
 * @param buf is a pointer to a buffer to copy the string to.
 * @param len is a the maximum number of character to copy.
 *
 * @return the size of the string copied or (-1) on error.
 */
CVMInt32 CVMioGetLastErrorString(char *buf, CVMInt32 len)
{
    char *str;
    int32_t nbytes;

    /* Get last error string. */
    if ((str = CVMioReturnLastErrorString()) == NULL)
        return 0;

    /* Get size & copy to buffer. */
    nbytes = strlen(str);
    if (nbytes >= len)
        nbytes = len - 1;
    strncpy(buf, str, nbytes);
    buf[nbytes] = '\0';

    /* Return bytes copied. */
    return nbytes;
}
