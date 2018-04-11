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

#ifndef _MPETEST_FILE_H_
#define _MPETEST_FILE_H_ 1

/**
 * MPE / MPEOS function names are re-defined here using macros, in order to
 * support MPE or MPEOS tests using the same test code.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */

#ifdef TEST_MPEOS
# define MPETEST_FILE(x)  mpeos_ ## x
#else
# define MPETEST_FILE(x)  mpe_ ## x
#endif /* TEST_MPEOS */

#define fileOpen  MPETEST_FILE(fileOpen)
#define fileClose  MPETEST_FILE(fileClose)
#define fileRead  MPETEST_FILE(fileRead)
#define fileWrite  MPETEST_FILE(fileWrite)
#define fileSeek  MPETEST_FILE(fileSeek)
#define fileSync  MPETEST_FILE(fileSync)
#define fileGetStat  MPETEST_FILE(fileGetStat)
#define fileSetStat  MPETEST_FILE(fileSetStat)
#define fileGetFStat  MPETEST_FILE(fileGetFStat)
#define fileSetFStat  MPETEST_FILE(fileSetFStat)
#define fileDelete  MPETEST_FILE(fileDelete)
#define fileRename  MPETEST_FILE(fileRename)
#define fileLoad  MPETEST_FILE(fileLoad)
#define fileUnload  MPETEST_FILE(fileUnload)
#define fileHandleName  MPETEST_FILE(fileHandleName)
#define dirOpen  MPETEST_FILE(dirOpen)
#define dirClose  MPETEST_FILE(dirClose)
#define dirRead  MPETEST_FILE(dirRead)
#define dirDelete  MPETEST_FILE(dirDelete)
#define dirRename  MPETEST_FILE(dirRename)
#define dirCreate  MPETEST_FILE(dirCreate)
#define dirMount  MPETEST_FILE(dirMount)
#define dirUnmount  MPETEST_FILE(dirUnmount)
#define dirGetUStat  MPETEST_FILE(dirGetUStat)
#define dirSetUStat  MPETEST_FILE(dirSetUStat)

#define filesysGetDefaultDir  MPETEST_FILE(filesysGetDefaultDir)
#define filesysInit           MPETEST_FILE(filesysInit)
#define dirClose_ptr          MPETEST_FILE(dirClose_ptr)
#define dirCreate_ptr         MPETEST_FILE(dirCreate_ptr)
#define dirDelete_ptr         MPETEST_FILE(dirDelete_ptr)
#define dirGetUStat_ptr       MPETEST_FILE(dirGetUStat_ptr)
#define dirMount_ptr          MPETEST_FILE(dirMount_ptr)
#define dirOpen_ptr           MPETEST_FILE(dirOpen_ptr)
#define dirRead_ptr           MPETEST_FILE(dirRead_ptr)
#define dirRename_ptr         MPETEST_FILE(dirRename_ptr)
#define dirSetUStat_ptr       MPETEST_FILE(dirSetUStat_ptr)
#define dirUnmount_ptr        MPETEST_FILE(dirUnmount_ptr)
#define fileClose_ptr         MPETEST_FILE(fileClose_ptr)
#define fileDelete_ptr        MPETEST_FILE(fileDelete_ptr)
#define fileGetFStat_ptr      MPETEST_FILE(fileGetFStat_ptr)
#define fileGetStat_ptr       MPETEST_FILE(fileGetStat_ptr)
#define fileLoad_ptr          MPETEST_FILE(fileLoad_ptr)
#define fileOpen_ptr          MPETEST_FILE(fileOpen_ptr)
#define fileRead_ptr          MPETEST_FILE(fileRead_ptr)
#define fileRename_ptr        MPETEST_FILE(fileRename_ptr)
#define fileSeek_ptr          MPETEST_FILE(fileSeek_ptr)
#define fileSetFStat_ptr      MPETEST_FILE(fileSetFStat_ptr)
#define fileSetStat_ptr       MPETEST_FILE(fileSetStat_ptr)
#define fileSync_ptr          MPETEST_FILE(fileSync_ptr)
#define fileUnload_ptr        MPETEST_FILE(fileUnload_ptr)
#define fileWrite_ptr         MPETEST_FILE(fileWrite_ptr)
#define init_ptr              MPETEST_FILE(init_ptr)

#endif /* _MPETEST_FILE_H_ */ 
