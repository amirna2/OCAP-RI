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

#ifndef _TEST_OC_H
#define _TEST_OC_H 1

#include <mpeTest.h>
#include <mpetest_file.h>
#include <mpetest_sys.h>

#define OCTEST_SLEEPDURATION       (1 * 1000)
#define OCTEST_CAROUSELID          (1)
#define OCTEST_FREQUENCY           699000000
#define OCTEST_PROGRAM             2
#define OCTEST_BUFSIZE             (4096)
#define OCTEST_PATHLEN             (256)
#define OCTEST_MAX_FILES           (4096)
#define OCTEST_NUM_THREADS         8
#define OCTEST_THREAD_SUCCEEDED    42
#define OCTEST_THREAD_FAILED       43
#define OCTEST_READFILENAME        "/Arena/fi/sfd/stxt/ui/StxtLine.class"

typedef struct
{
    char *filename;
    uint32_t crc;
    uint32_t size;
} OCTEST_FileCRC2;

typedef struct
{
    char name[OCTEST_PATHLEN];
    uint32_t crc;
    uint32_t size;
} OCTEST_FileCRC;

typedef struct
{
    uint32_t threadNum;
    uint32_t status;
    uint32_t errorCode;
    uint32_t iteration;
    uint32_t fileNum;
    uint8_t threadName[64];
    uint8_t buffer[OCTEST_BUFSIZE];
} OCTEST_ThreadArgs;

/* Test definitions for the MPE/MPEOS Object Carousel.
 */
NATIVEEXPORT_API void test_mpe_fileocRunAllTests(void);
NATIVEEXPORT_API void test_mpe_fileocFileRead(void);
NATIVEEXPORT_API void test_mpe_fileocGenCRC(void);
NATIVEEXPORT_API void test_mpe_fileocMountUnmount(void);
NATIVEEXPORT_API void test_mpe_fileocMultiMount(void);
NATIVEEXPORT_API void test_mpe_fileocMultiThreadCRC(void);
NATIVEEXPORT_API void test_mpe_fileocPrintDirTreeCRC(void);
NATIVEEXPORT_API void test_mpe_fileocRepeatedMount(void);
NATIVEEXPORT_API void test_mpe_fileocRunAllTests(void);
NATIVEEXPORT_API void test_mpe_fileocWalkDirTree(void);

#endif /* end #ifndef _TEST_OC_H */
