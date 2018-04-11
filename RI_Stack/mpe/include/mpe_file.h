#ifndef _MPE_FILE_H_
#define _MPE_FILE_H_
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

#include <mpe_types.h>
#include <mpe_error.h>
#include <mpe_sys.h>
#include <mpe_si.h>

#include <mpeos_time.h>

#include <os_file.h>

#include <mgrdef.h>

/* file return error codes */
#define MPE_FS_ERROR_BASE               (0x00001000)
#define MPE_FS_ERROR_SUCCESS            (MPE_SUCCESS)
#define MPE_FS_ERROR_FAILURE            (MPE_FS_ERROR_BASE + 1)
#define MPE_FS_ERROR_ALREADY_EXISTS     (MPE_FS_ERROR_BASE + 2)
#define MPE_FS_ERROR_NOT_FOUND          (MPE_FS_ERROR_BASE + 3)
#define MPE_FS_ERROR_EOF                (MPE_FS_ERROR_BASE + 4)
#define MPE_FS_ERROR_DEVICE_FAILURE     (MPE_FS_ERROR_BASE + 5)
#define MPE_FS_ERROR_INVALID_STATE      (MPE_FS_ERROR_BASE + 6)
#define MPE_FS_ERROR_READ_ONLY          (MPE_FS_ERROR_BASE + 7)
#define MPE_FS_ERROR_NO_MOUNT           (MPE_FS_ERROR_BASE + 8)
#define MPE_FS_ERROR_UNSUPPORT          (MPE_FS_ERROR_BASE + 9)
#define MPE_FS_ERROR_NOTHING_TO_ABORT   (MPE_FS_ERROR_BASE + 10)
#define MPE_FS_ERROR_UNKNOWN_URL        (MPE_FS_ERROR_BASE + 11)
#define MPE_FS_ERROR_INVALID_DATA       (MPE_FS_ERROR_BASE + 12)
#define MPE_FS_ERROR_DISCONNECTED       (MPE_FS_ERROR_BASE + 13)
#define MPE_FS_ERROR_SERVICEXFER        (MPE_FS_ERROR_BASE + 14)
#define MPE_FS_ERROR_TOOMANYLINKS       (MPE_FS_ERROR_BASE + 15)
#define MPE_FS_ERROR_DEVFULL            (MPE_FS_ERROR_BASE + 16)
#define MPE_FS_ERROR_INVALID_TYPE       (MPE_FS_ERROR_BASE + 17)

/*
 * Duplicate error codes.  These should be removed at some point, but are marked
 * as equivalent to the appropriate MPE error codes currently.
 */
#define MPE_FS_ERROR_OUT_OF_MEM         MPE_ENOMEM
#define MPE_FS_ERROR_TIMEOUT            MPE_ETIMEOUT
#define MPE_FS_ERROR_INVALID_PARAMETER  MPE_EINVAL
#define MPE_FS_ERROR_EVENTS             MPE_EEVENT

/* file seek position parameters */
#define MPE_FS_SEEK_SET                 (1)
#define MPE_FS_SEEK_CUR                 (2)
#define MPE_FS_SEEK_END                 (3)

/* file open attribute bitfields */
#define MPE_FS_OPEN_READ                (0x0001)
#define MPE_FS_OPEN_WRITE               (0x0002)
#define MPE_FS_OPEN_READWRITE           (0x0004)
#define MPE_FS_OPEN_CAN_CREATE          (0x0008)
#define MPE_FS_OPEN_MUST_CREATE         (0x0010)
#define MPE_FS_OPEN_TRUNCATE            (0x0020)
#define MPE_FS_OPEN_APPEND              (0x0040)
#define MPE_FS_OPEN_NOLINKS             (0x0080)
#define MPE_FS_OPEN_STREAMONLY          (0x0100)
#define MPE_FS_OPEN_CACHEONLY           (0x0200)

/* file types */
#define MPE_FS_TYPE_UNKNOWN             (0x0000)
#define MPE_FS_TYPE_FILE                (0x0001)
#define MPE_FS_TYPE_DIR                 (0x0002)
#define MPE_FS_TYPE_STREAM              (0x0003)
#define MPE_FS_TYPE_STREAMEVENT         (0x0004)
#define MPE_FS_TYPE_OTHER               (0xffff)

/* file stat info */
#define MPE_FS_STAT_SIZE                (1)
#define MPE_FS_STAT_TYPE                (3)
#define MPE_FS_STAT_ISKNOWN             (6)
#define MPE_FS_STAT_CREATEDATE          (7)
#define MPE_FS_STAT_MODDATE             (8)
#define MPE_FS_STAT_SOURCEID            (12)
#define MPE_FS_STAT_DEFAULTSYSDIR       (13)
#define MPE_FS_STAT_IS_VIDEO            (16)
#define MPE_FS_STAT_IS_AUDIO            (17)
#define MPE_FS_STAT_IS_DATA             (18)
#define MPE_FS_STAT_DURATION            (19)
#define MPE_FS_STAT_TUNING_INFO         (20)
#define MPE_FS_STAT_TARGET_INFO         (22)
#define MPE_FS_STAT_SIHANDLE            (23)
#define MPE_FS_STAT_ISCURRENT           (25)
#define MPE_FS_STAT_CONTENTTYPE         (26)

/* dir stat info */
#define MPE_FS_STAT_MOUNTPATH           (101)
#define MPE_FS_STAT_CONNECTIONAVAIL     (102)

#define MPE_FS_SID_OOB                  (0xdeadbeef)

#define MPE_FS_OC_ALL_TAPS                 (0xffff)        /* Any or all taps */

/* async-load event types (must match DSMCCObject.java values!) */
#define MPE_FS_EVENT_UNKNOWN            (0)
#define MPE_FS_EVENT_SUCCESS            (1)
#define MPE_FS_EVENT_INVALIDFORMAT      (2)
#define MPE_FS_EVENT_INVALIDPATH        (3)
#define MPE_FS_EVENT_LOADABORT          (4)
#define MPE_FS_EVENT_MPEGERR            (5)
#define MPE_FS_EVENT_NOTENTITLED        (6)
#define MPE_FS_EVENT_SERVERERR          (7)
#define MPE_FS_EVENT_SERVICEXFR         (8)

/* general parameters */
#define MPE_FS_NSAP_LENGTH              (20)
#define MPE_FS_ENV_DEFAULT_SYS_DIR      "FS.DEFSYSDIR"

/* os-specific parameters */
#define MPE_FS_MAX_PATH                 OS_FS_MAX_PATH
#define MPE_FS_SEPARATION_STRING        OS_FS_SEPARATION_STRING
#define MPE_FS_SEPARATION_CHAR          MPE_FS_SEPARATION_STRING[0]
#define MPE_FS_DEFAULT_SYS_DIR          OS_FS_DEFAULT_SYS_DIR

/* the maximum length of the virtual mount for MPE file system **
 ** drivers (e.g. "/itfs") excluding the null terminator        */
#define MPE_FS_MAX_MOUNT_POINT_SIZE     OS_FS_MAX_MOUNT_POINT_SIZE

/* file system type mappings */

typedef void* mpe_File;
typedef uint32_t mpe_FileSeekMode;
typedef uint32_t mpe_FileOpenMode;
typedef int32_t mpe_FileError;
typedef uint32_t mpe_FileStatMode;
typedef void* mpe_FileChangeHandle;

typedef void* mpe_Dir;
typedef uint32_t mpe_DirStatMode;
typedef struct mpe_DirEntry
{
    char name[MPE_FS_MAX_PATH + 1];
    uint64_t fileSize;
    mpe_Bool isDir;
} mpe_DirEntry;

typedef union
{
    mpe_Bool isConnectAvail; /* is connection available */
    char path[MPE_FS_MAX_PATH + 1];/* file-system path */
} mpe_DirInfo;

typedef struct mpe_DirUrl
{
    const char *url;
    mpe_SiServiceHandle siHandle;
    uint32_t carouselId;
} mpe_DirUrl;

typedef void *mpe_Stream;
typedef struct
{
    uint32_t eventId;
    char name[MPE_FS_MAX_PATH + 1]; /* name of the event */
} mpe_StreamEventInfo;

/* getstat/setstat info */
typedef struct mpe_FileInfo mpe_FileInfo;

struct mpe_FileInfo
{
    /* passed in/back */
    uint64_t size; /* file size */
    uint16_t type; /* see MPE_FS_TYPE_xxx */
    mpe_Bool isKnown; /* is parent directory loaded */
    uint32_t sourceId; /* the source ID this file is coming on (BFS only) */
    mpe_Time createDate; /* creation date */
    mpe_Time modDate; /* last modified date */
    mpe_Time accessDate; /* last accessed date */
    uint32_t freq; /* Frequency, for GET_TUNINGINFO */
    uint32_t prog; /* Program Number, for GET_TUNINGINFO */
    mpe_SiModulationMode qam; /* QAM mode, for GET_TUNINGINFO */
    mpe_Bool hasType; /* Boolean, if this matches the requested type */
    uint64_t duration; /* Duration of a stream event, in milliseconds */
    uint32_t numTaps; /* Number of taps in this stream */
    void *buf; /* pointer to caller's buffer (of size 'size') */
    uint8_t nsap[MPE_FS_NSAP_LENGTH];
    mpe_SiServiceHandle siHandle; /* New SI Handle to set. */
    mpe_Bool isCurrent; /* Is this version of the file current?  GetFStat only */
};

/* file system function call table */
typedef struct mpe_filesys_ftable_t
{
    void (*mpe_filesys_init_ptr)(void);
    /* TODO: add "term" function? */

    /* High level file system API */
    mpe_FileError (*mpe_fileOpen_ptr)(const char *fileName,
            mpe_FileOpenMode openMode, mpe_File *fileHandlePtr);
    mpe_FileError (*mpe_fileClose_ptr)(mpe_File fileHandle);
    mpe_FileError (*mpe_fileRead_ptr)(mpe_File fileHandle, uint32_t *count,
            void *buffer);
    mpe_FileError (*mpe_fileWrite_ptr)(mpe_File fileHandle, uint32_t *count,
            void *buffer);
    mpe_FileError (*mpe_fileSeek_ptr)(mpe_File fileHandle,
            mpe_FileSeekMode seekMode, int64_t *offset);
    mpe_FileError (*mpe_fileSync_ptr)(mpe_File fileHandle);
    mpe_FileError (*mpe_fileGetStat_ptr)(const char *fileName,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpe_fileSetStat_ptr)(const char *fileName,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpe_fileGetFStat_ptr)(mpe_File fileHandle,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpe_fileSetFStat_ptr)(mpe_File fileHandle,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpe_fileDelete_ptr)(const char *fileName);
    mpe_FileError (*mpe_fileRename_ptr)(const char *oldFileName,
            const char *newFileName);
    mpe_FileError
            (*mpe_dirOpen_ptr)(const char *dirName, mpe_Dir *dirHandlePtr);
    mpe_FileError (*mpe_dirRead_ptr)(mpe_Dir dirHandle, mpe_DirEntry *dirEnt);
    mpe_FileError (*mpe_dirClose_ptr)(mpe_Dir dirHandle);
    mpe_FileError (*mpe_dirDelete_ptr)(const char *dirName);
    mpe_FileError (*mpe_dirRename_ptr)(const char *oldDirName,
            const char *newDirName);
    mpe_FileError (*mpe_dirCreate_ptr)(const char *dirName);
    mpe_FileError (*mpe_dirMount_ptr)(const mpe_DirUrl *dirUrl);
    mpe_FileError (*mpe_dirUnmount_ptr)(const mpe_DirUrl *dirUrl);
    mpe_FileError (*mpe_dirGetUStat_ptr)(const mpe_DirUrl *dirUrl,
            mpe_DirStatMode mode, mpe_DirInfo *info);
    mpe_FileError (*mpe_dirSetUStat_ptr)(const mpe_DirUrl *dirUrl,
            mpe_DirStatMode mode, mpe_DirInfo *info);
    mpe_FileError (*mpe_streamOpen_ptr)(const char *fileName,
            mpe_Stream *streamHandlePtr);
    mpe_FileError (*mpe_streamClose_ptr)(mpe_Stream streamHandle);
    mpe_FileError (*mpe_streamReadEvent_ptr)(mpe_Stream streamHandle,
            mpe_StreamEventInfo *streamEvent);
    mpe_FileError (*mpe_streamGetNumTaps_ptr)(mpe_Stream streamHandle,
            uint16_t tapType, uint32_t *numTaps);
    mpe_FileError (*mpe_streamReadTap_ptr)(mpe_Stream streamHandle,
            uint16_t tapType, uint32_t tapNumber, uint16_t *tap,
            uint16_t* tapId);
    mpe_FileError (*mpe_filePrefetch_ptr)(const char *fileName);
    mpe_FileError (*mpe_filePrefetchModule_ptr)(const char *mountPoint,
            const char *moduleName);
    mpe_FileError (*mpe_fileDIILocation)(const char *mountPoint,
            uint16_t diiIdentification, uint16_t associationTag);
    mpe_FileError (*mpe_fileSetChangeListener)(const char *fileName,
            mpe_EventQueue queueId, void *act, mpe_FileChangeHandle *out);
    mpe_FileError (*mpe_fileRemoveChangeListener)(mpe_FileChangeHandle out);
} mpe_filesys_ftable_t;

/*
 ** file system call shortcuts (macros going thru function table)
 */

/* MPE FileSystem function table */
#define mpe_filesys_ftable  ((mpe_filesys_ftable_t*)(FTABLE[MPE_MGR_TYPE_FILESYS]))

/* MPE FileSystem initialization routine */
#define mpe_filesysInit ((mpe_filesys_ftable->mpe_filesys_init_ptr))

/*
 ** public file system API
 */

/**
 * <i>mpe_FileError mpe_fileOpen(const char* name, mpe_FileOpenMode openMode, mpe_File* fileHandlePtr);</i>
 *
 * Opens a file in the file system namespace.
 *
 * @param name The path to the file to open.
 * @param openMode The type of access requested (read, write, create, truncate, append).
 * @param fileHandle A pointer to an mpe_File handle, through which the opened file is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileOpen        ((mpe_filesys_ftable->mpe_fileOpen_ptr))

/**
 * <i>mpe_FileError mpe_fileClose(mpe_File fileHandle);</i>
 *
 * Closes a file previously opened with <i>mpe_FileOpen()</i>.
 *
 * @param fileHandle A mpe_File handle, previously returned by <i>mpe_FileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileClose       ((mpe_filesys_ftable->mpe_fileClose_ptr))

/**
 * <i>mpe_FileError mpe_fileRead (mpe_File fileHandle, uint32_t* count, void* buffer);</i>
 *
 * Reads data from a file previously opened with <i>mpe_FileOpen()</i>.
 *
 * @param fileHandle A mpe_File handle, previously returned by <i>mpe_FileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                  read.  On exit, this will indicate the number of bytes actually read.
 * @param buffer A pointer to a buffer to receive the data.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileRead        ((mpe_filesys_ftable->mpe_fileRead_ptr))

/**
 * <i>mpe_FileError mpe_fileWrite(mpe_File fileHandle, uint32_t* count, void* buffer);</i>
 *
 * Writes data to a file previously opened with <i>mpe_FileOpen()</i>.
 *
 * @param fileHandle A mpe_File handle, previously returned by <i>mpe_FileOpen()</i>.
 * @param count A pointer to a byte count.  On entry, this must point to the number of bytes to
 *                  write.  On exit, this will indicate the number of bytes actually written.
 * @param buffer A pointer to a buffer containing the data to send.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileWrite       ((mpe_filesys_ftable->mpe_fileWrite_ptr))

/**
 * <i>mpe_FileError mpe_fileSeek(mpe_File fileHandle, mpe_FileSeekMode seekMode, int64_t* offset);</i>
 *
 * Changes and reports the current position within a file previously opened with <i>mpe_FileOpen()</i>.
 *
 * @param fileHandle A mpe_File handle, previously returned by <i>mpe_FileOpen()</i>.
 * @param seekMode A seek mode constant indicating whether the offset value should be considered
 *                  relative to the start, end, or current position within the file.
 * @param offset A pointer to a file position offset.  On entry, this should indicate the number
 *                  of bytes to seek, offset from the seekMode.  On exit, this will indicate the
 *                  new absolute position within the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileSeek        ((mpe_filesys_ftable->mpe_fileSeek_ptr))

/**
 * <i>mpe_FileError mpe_fileSync(mpe_File fileHandle);</i>
 *
 * Synchronizes the contents of a file previously opened with <i>mpe_FileOpen()</i>.  This will write
 *  any data that is pending.  Pending data is data that has been written to a file, but which hasn't
 *  been flushed to the storage device yet.
 *
 * @param fileHandle A mpe_File handle, previously returned by <i>mpe_FileOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileSync        ((mpe_filesys_ftable->mpe_fileSync_ptr))

/**
 * <i>mpe_FileError mpe_fileGetStat(const char* name, mpe_FileStatMode mode, mpe_FileInfo *info);</i>
 *
 * Retrieve some file status info on a file.
 *
 * @param name The path to the file on which to retrieve information
 * @param mode The specific file stat to get.
 * @param info A pointer to the buffer in which to return the indicated file stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileGetStat     ((mpe_filesys_ftable->mpe_fileGetStat_ptr))

/**
 * <i>mpe_FileError mpe_fileSetStat(const char* name, mpe_FileStatMode mode, mpe_FileInfo *info);</i>
 *
 * Set some file status info on a file.
 *
 * @param name The path to the file on which to update its status information.
 * @param mode The specific file stat to set.
 * @param info A pointer to the buffer from which to set the indicated file stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileSetStat     ((mpe_filesys_ftable->mpe_fileSetStat_ptr))

/**
 * <i>mpe_FileError mpe_fileGetFStat(mpe_File fileHandle, mpe_FileStatMode mode, mpe_FileInfo *info);</i>
 *
 * Retrieve some file status info on a file previously opened with <i>mpe_FileOpen()</i>.
 *
 * @param fileHandle A mpe_File handle, previously returned by <i>mpe_FileOpen()</i>.
 * @param mode The specific file stat to get.
 * @param info A pointer to the buffer in which to return the indicated file stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileGetFStat        ((mpe_filesys_ftable->mpe_fileGetFStat_ptr))

/**
 * <i>mpe_FileError mpe_fileSetFStat(mpe_File fileHandle, mpe_FileStatMode mode, mpe_FileInfo *info);</i>
 *
 * Set some file status info on a file previously opened with <i>mpe_FileOpen()</i>.
 *
 * @param fileHandle A mpe_File handle, previously returned by <i>mpe_FileOpen()</i>.
 * @param mode The specific file stat to get.
 * @param info A pointer to the buffer from which to set the indicated file stat info.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileSetFStat        ((mpe_filesys_ftable->mpe_fileSetFStat_ptr))

/**
 * <i>mpe_FileError mpe_fileDelete(const char* name);</i>
 *
 * Deletes the specific file from the the file system namespace.
 *
 * @param name The path to the file to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileDelete      ((mpe_filesys_ftable->mpe_fileDelete_ptr))

/**
 * <i>mpe_FileError mpe_fileRename(const char* oldFileName, const char* newFileName);</i>
 *
 * Renames or moves the specific file in the file system namespace.
 *
 * @param oldFileName The path to the file to rename or move.
 * @param newFileName The new path and/or name for the file.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileRename      ((mpe_filesys_ftable->mpe_fileRename_ptr))

/**
 * <i>mpe_FileError (*mpe_fileHandleName_ptr)(mpe_File fileHandle, char **fileName);</i>
 *
 * Retrieve the file path name for the indicated open file handle.
 *
 * @param fileHandle The handle to an open file.
 * @param fileName The returned pointer to the file path associated
 *   with this open file handle.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_fileHandleName      ((mpe_filesys_ftable->mpe_fileHandleName_ptr))

/**
 * <i>mpe_FileError mpe_dirOpen(const char *dirName, mpe_Dir *dirHandlePtr);</i>
 *
 * Opens a directory in the file system namespace.
 *
 * @param dirName The path to the directory to open.
 * @param dirHandlePtr A pointer to an mpe_Dir handle, through which the opened
 *   directory is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirOpen         ((mpe_filesys_ftable->mpe_dirOpen_ptr))

/**
 * <i>mpe_FileError mpe_dirClose(mpe_Dir dirHandle);</i>
 *
 * Closes a directory previously opened with <i>mpe_DirOpen</i>.
 *
 * @param dirHandle A mpe_Dir handle, previously returned by <i>mpe_DirOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirClose        ((mpe_filesys_ftable->mpe_dirClose_ptr))

/**
 * <i>mpe_FileError mpe_dirRead(mpe_Dir dirHandle, mpe_DirEntry *dirEnt);</i>
 *
 * Reads the contents of a directory previously opened with <i>mpe_DirOpen</i>.
 *  This can be used to iterate through the contents a directory in the file system namespace.
 *
 * @param dirHandle A mpe_Dir handle, previously returned by <i>mpe_DirOpen()</i>.
 * @param dirEnt A pointer to a mpe_DirEntry object.  On return, this will contain
 *                   data about a directory entry.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirRead         ((mpe_filesys_ftable->mpe_dirRead_ptr))

/**
 * <i>mpe_FileError mpe_dirDelete(const char *dirName);</i>
 *
 * Deletes a directory from the file system namespace.
 *
 * @param dirName The path to the directory to delete.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirDelete       ((mpe_filesys_ftable->mpe_dirDelete_ptr))

/**
 * <i>mpe_FileError mpe_dirRename(const char *oldDirName, const char *newDirName);</i>
 *
 * Renames or moves the specific directory in the file system namespace.
 *
 * @param oldDirName The path to the directory to rename or move.
 * @param newDirName The new path and/or name for the directory.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirRename       ((mpe_filesys_ftable->mpe_dirRename_ptr))

/**
 * <i>mpe_FileError mpe_dirCreate(const char *dirName);</i>
 *
 * Creates the specific directory in the file system namespace.
 *
 * @param dirName The path to the directory to create.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirCreate       ((mpe_filesys_ftable->mpe_dirCreate_ptr))

/**
 * <i>mpe_FileError (*mpe_dirMount_ptr)(const mpe_DirUrl *dirUrl);</i>
 *
 * Mount the indicated URL on the optional (non '-1) indicated carousel id
 * into the object carousel file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirMount        ((mpe_filesys_ftable->mpe_dirMount_ptr))

/**
 * <i>mpe_FileError (*mpe_dirUnmount_ptr)(const mpe_DirUrl *dirUrl);</i>
 *
 * Unmount the indicated URL from the object carousel file-system namespace.
 *
 * @param dirUrl The URL of the directory to mount.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_dirUnmount      ((mpe_filesys_ftable->mpe_dirUnmount_ptr))

/**
 * <i>mpe_FileError mpe_dirGetUStat(const mpe_DirUrl *dirUrl, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
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
#define mpe_dirGetUStat     ((mpe_filesys_ftable->mpe_dirGetUStat_ptr))

/**
 * <i>mpe_FileError mpe_dirSetUStat(const mpe_DirUrl *url, mpe_DirStatMode mode, mpe_DirInfo *info);</i>
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
#define mpe_dirSetUStat     ((mpe_filesys_ftable->mpe_dirSetUStat_ptr))

/**
 * <i>mpe_FileError mpe_streamOpen(const char* name, mpe_Stream* streamHandlePtr);</i>
 *
 * Opens a DSMCC Stream or Stream Event in the file system namespace.
 *
 * @param name The path to the Stream object to open.
 * @param streamHandle A pointer to an mpe_File handle, through which the opened file is returned.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_streamOpen      ((mpe_filesys_ftable->mpe_streamOpen_ptr))

/**
 * <i>mpe_FileError mpe_streamClose(mpe_Stream streamHandle);</i>
 *
 * Closes a file previously opened with <i>mpe_streamOpen()</i>.
 *
 * @param streamHandle A mpe_Stream handle, previously returned by <i>mpe_StreamOpen()</i>.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_streamClose     ((mpe_filesys_ftable->mpe_streamClose_ptr))

/**
 * <i>mpe_FileError mpe_streamReadEvent(mpe_Stream streamHandle, mpe_StreamEventInfo *event);</i>
 *
 * Reads the contents of a StreamEvent previously opened with <i>mpe_streamOpen</i>.
 * This can be used to iterate through all the events.
 * Will return no data if this is a stream, and not a stream event.
 *
 * @param streamHandle  A mpe_Stream handle, previously returned by <i>mpe_DirOpen()</i>.
 * @param event         A pointer to a mpe_StreamEventInfo object.  On return, this will contain
 *                      data about a event description.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_streamReadEvent         ((mpe_filesys_ftable->mpe_streamReadEvent_ptr))

/**
 * <i>mpe_FileError mpe_streamGetNumTaps(mpe_Stream streamHandle, uint32_t tapType, uint32_t *numTaps);</i>
 * Read the number of taps of a certain type (or all types) in a stream object.
 *
 * @param streamHandle  A mpe_Stream handle, previously returned by <i>mpe_DirOpen()</i>.
 * @param tapType       The type of tap to look for.  (MPE_OC_ALL_TAPS means all TAPS)
 * @param numTaps       The number of taps
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_streamGetNumTaps        ((mpe_filesys_ftable->mpe_streamGetNumTaps_ptr))

/**
 * <i>mpe_FileError mpe_streamReadTap(mpe_Stream streamHandle, uint32_t tapType, uint32_t tapNumber, uint32_t *tap);</i>
 * Read a TAP tag out of a stream or stream event object.
 *
 * @param streamHandle  A mpe_Stream handle, previously returned by <i>mpe_DirOpen()</i>.
 * @param tapType       The type of tap to look for.  (MPE_OC_ALL_TAPS means all TAPS)
 * @param tapNumber     The instance of this tap to read.  0 = first tap.
 * @param tap           [out] pointer to a tap object where to fill with the tap
 * @param tapId         [out] pointer to where to fill in the ID field.  Normally 0, but can be non-zero on NPT taps.
 *
 * @return The error code if the operation fails, otherwise <i>MPE_FS_ERROR_SUCCESS</i>
 *          is returned.
 */
#define mpe_streamReadTap           ((mpe_filesys_ftable->mpe_streamReadTap_ptr))

/**
 * <i>mpe_FileError mpe_filePrefetch(char *filename);</i>
 * Start prefetching a file into the cache.  The prefetch will continue asynchronously, and will prefetch
 * the entire module containing the file.
 *
 * @param fileName       Name of the file to prefetch.
 *
 * @return MPE_SUCCESS if the prefetch starts, or the file is already prefetched, error
 *          codes if the prefetch cannot be started.
 */
#define mpe_filePrefetch            ((mpe_filesys_ftable->mpe_filePrefetch_ptr))

/**
 * <i>mpe_FileError mpe_filePrefetchModule(char *mountPoint, char *moduleName);</i>
 * Prefetch a module (or modules) by label.  The label is found in the label_descriptor in the
 * DII for the data carousel.
 *
 * @param mountPoint        Name where the carousel in question is mounted.
 * @param moduleName        Name of the modules, corresponding to the label_descriptor associated with the module
 *                          in the DII.
 *
 * @return MPE_SUCCESS if the prefetch starts, or the file is already prefetched, error
 *          codes if the prefetch cannot be started.
 */
#define mpe_filePrefetchModule      ((mpe_filesys_ftable->mpe_filePrefetchModule_ptr))

/**
 * <i>mpe_FileError mpe_fileDIILocation(char *mountPoint, char *moduleName);</i>
 * Add the location of a DII to the carousel.  Corresponds to a DII Location Descriptor in the AIT.
 *
 * @param mountPoint            Name where the carousel in question is mounted.
 * @param diiIdentification     The ID bits of the transaction ID for the DII.
 * @param associationTag        The Association Tag of the PID containing the DII.
 *
 * @return MPE_SUCCESS if the prefetch starts, or the file is already prefetched, error
 *          codes if the prefetch cannot be started.
 */
#define mpe_fileDIILocation      ((mpe_filesys_ftable->mpe_fileDIILocation))

/**
 * <i>mpe_FileError mpe_fileSetChangeListener(const char *fileName, mpe_EventQueue evQueue, void *act, mpe_FileChangeHandle *out) </i>
 * Add a change listener on a file.
 *
 * @param fileName  The name of the file in the to watch for changes on.
 * @param queueId   The event queue to send signals to when the file changes.
 * @param act       Data to send along with the event when it sends the signal of a version change.
 * @param out       [out] A Pointer to where to fill a handle to be used to remove this listener.
 *
 * @return MPE_SUCCESS if the listener is added correctly, MPE_FS_ERROR_UNSUPPORT if the filesystem
 *         does not support change listeners, other errors codes otherwise.
 */
#define mpe_fileSetChangeListener       ((mpe_filesys_ftable->mpe_fileSetChangeListener))

/**
 * <i>mpe_FileError mpe_fileRemoveChangeListener(mpe_FileChangeHandle out) </i>
 * Remove a previously set change listener.
 *
 * @param out       The handle to remove.
 *
 * @return MPE_SUCCESS if the listener is removed.  Errors otherwise.
 */
#define mpe_fileRemoveChangeListener    ((mpe_filesys_ftable->mpe_fileRemoveChangeListener))

#ifdef __cplusplus
extern "C"
{
#endif

/* file system setup function (to register MPE table functions w/ MPE system manager) */
void mpe_filesysSetup(void);

#ifdef __cplusplus
}
;
#endif

#endif /* _MPE_FILE_H_ */
