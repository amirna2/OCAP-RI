#if !defined(_MPEOS_FILE_H)
#define _MPEOS_FILE_H
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

#include "mpe_file.h"

/* file system driver function call table */
typedef struct mpeos_filesys_ftable_t
{
    void (*mpeos_init_ptr)(const char* mountPoint);
    mpe_FileError (*mpeos_fileOpen_ptr)(const char* fileName,
            mpe_FileOpenMode openMode, mpe_File* returnHandle);
    mpe_FileError (*mpeos_fileClose_ptr)(mpe_File handle);
    mpe_FileError (*mpeos_fileRead_ptr)(mpe_File handle, uint32_t* count,
            void* buffer);
    mpe_FileError (*mpeos_fileWrite_ptr)(mpe_File handle, uint32_t* count,
            void* buffer);
    mpe_FileError (*mpeos_fileSeek_ptr)(mpe_File handle,
            mpe_FileSeekMode seekMode, int64_t* offset);
    mpe_FileError (*mpeos_fileSync_ptr)(mpe_File handle);
    mpe_FileError (*mpeos_fileGetStat_ptr)(const char* fileName,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpeos_fileSetStat_ptr)(const char* fileName,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpeos_fileGetFStat_ptr)(mpe_File handle,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpeos_fileSetFStat_ptr)(mpe_File handle,
            mpe_FileStatMode mode, mpe_FileInfo *info);
    mpe_FileError (*mpeos_fileDelete_ptr)(const char* fileName);
    mpe_FileError (*mpeos_fileRename_ptr)(const char* oldName,
            const char* newName);
    mpe_FileError (*mpeos_dirOpen_ptr)(const char* name, mpe_Dir* returnHandle);
    mpe_FileError (*mpeos_dirRead_ptr)(mpe_Dir handle, mpe_DirEntry* dirEnt);
    mpe_FileError (*mpeos_dirClose_ptr)(mpe_Dir handle);
    mpe_FileError (*mpeos_dirDelete_ptr)(const char* dirName);
    mpe_FileError (*mpeos_dirRename_ptr)(const char* oldName,
            const char* newName);
    mpe_FileError (*mpeos_dirCreate_ptr)(const char* dirName);
    mpe_FileError (*mpeos_dirMount_ptr)(const mpe_DirUrl *dirUrl);
    mpe_FileError (*mpeos_dirUnmount_ptr)(const mpe_DirUrl *dirUrl);
    mpe_FileError (*mpeos_dirGetUStat_ptr)(const mpe_DirUrl *dirUrl,
            mpe_DirStatMode mode, mpe_DirInfo *info);
    mpe_FileError (*mpeos_dirSetUStat_ptr)(const mpe_DirUrl *dirUrl,
            mpe_DirStatMode mode, mpe_DirInfo *info);
    mpe_FileError (*mpeos_streamOpen_ptr)(const char *fileName,
            mpe_Stream *streamHandlePtr);
    mpe_FileError (*mpeos_streamClose_ptr)(mpe_Stream streamHandle);
    mpe_FileError (*mpeos_streamReadEvent_ptr)(mpe_Stream streamHandle,
            mpe_StreamEventInfo *event);
    mpe_FileError (*mpeos_streamGetNumTaps_ptr)(mpe_Stream streamHandle,
            uint16_t tapType, uint32_t *numTaps);
    mpe_FileError (*mpeos_streamReadTap_ptr)(mpe_Stream streamHandle,
            uint16_t tapType, uint32_t tapNumber, uint16_t *tap,
            uint16_t *tapId);
    mpe_FileError (*mpeos_filePrefetch_ptr)(const char *fileName);
    mpe_FileError (*mpeos_filePrefetchModule_ptr)(const char *mountpoint,
            const char *moduleName);
    mpe_FileError (*mpeos_fileAddDII_ptr)(const char *mountpoint,
            uint16_t diiId, uint16_t assocTag);
    mpe_FileError (*mpeos_fileSetChangeListener_ptr)(const char *fileName,
            mpe_EventQueue queueId, void * act, mpe_FileChangeHandle *handle);
    mpe_FileError (*mpeos_fileRemoveChangeListener_ptr)(
            mpe_FileChangeHandle handle);
} mpeos_filesys_ftable_t;

#ifdef __cplusplus
extern "C"
{
#endif

/* port-specific file system initialization (to setup all file system drivers) */
void mpeos_filesysInit(void(*mpe_filesysRegisterDriver)(
        const mpeos_filesys_ftable_t *, const char *));

/* port-specific default-system-directory lookup */
mpe_FileError mpeos_filesysGetDefaultDir(char *buf, uint32_t bufSz);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_FILE_H */
