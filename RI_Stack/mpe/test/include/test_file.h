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

#ifndef _TEST_FILE_H_
#define _TEST_FILE_H_ 

#include <mpeTest.h>
#include <vte_agent.h>
#include <mpetest_file.h>
#include <mpetest_sys.h>
#include "mpeos_time.h"
#include "mpeos_file.h"
#include "os_file.h"
#include "mgrdef.h"

#ifdef TEST_MPEOS
# include <mpeos_dbg.h>
# define TESTNAME         "mpeos_filesysmgr"
#else
# include <mpe_dbg.h>
# include "mpe_sys.h"
# define TESTNAME         "mpe_filesysmgr"
#endif

#define BUFFERSIZE		1024
#define MAX_URL_SIZE		64
#define CHANGE_EVENT_MAX_MS	(240 * 1000)
#define LOAD_SLEEP_TIME_MS	(500)
#define LOAD_SLEEP_MAX_MS	(60 * 1000)
#define BAD_EVENTCODE		(-1)

#define FileSystemType_t	uint32_t

#define BFS     1
#define ROMFS   2
#define SNFS    3
#define SIM     4
#define ITFS    5

/*
 ** Globals
 */
extern mpe_Bool isBfs, isSnfs, isRomfs, isSimulator;

/*
 * Prototypes:
 */
void test_mpe_fileOpenClose(CuTest*);
void test_mpe_fileRead(CuTest*);
void test_mpe_fileWrite(CuTest*);
void test_mpe_fileSeek(CuTest*);
void test_mpe_fileGetEOF(CuTest*);
void test_mpe_fileSetEOF(CuTest*);
void test_mpe_fileSync(CuTest*);
void test_mpe_fileDelete(CuTest*);
void test_mpe_fileRename(CuTest*);
void test_mpe_dirOpenClose(CuTest*);
void test_mpe_dirRead(CuTest*);
void test_mpe_dirRenameDelete(CuTest*);
void mpe_file_SetGlobals(void);
void mpe_test_openclose(CuTest *tc);
void test_mpe_odn_bfs(CuTest *tc);
void mpe_test_read_hiroyo(CuTest *tc);
void mpe_test_bug(CuTest *tc);
void mpe_test_read(CuTest *tc);
void mpe_test_seek(CuTest *tc);
void mpe_test_stat(CuTest *tc);
void mpe_test_write(CuTest *tc);
void mpe_test_dir_openclose(CuTest *tc);
void mpe_test_dirread(CuTest *tc);
void mpe_test_loadunload(CuTest *tc);
void setFileSystem(FileSystemType_t type);
void getpath(char * dest, char * parent, char * name);
void recurseDir(char * parent, CuTest *tc);
void test_mpe_recurseDir(CuTest *tc);
void test_mpe_fileChange(CuTest *tc);

#define SETFS(type)         \
do{                         \
	setFileSystem(type);    \
	mpe_file_SetGlobals();  \
}                           \
while(0) 

#define RUN_FILE_TEST(fs,test,msg)                             \
do{                                                            \
  CuSuite * pSuite;                                            \
  CuString* output;                                            \
  SETFS(fs);                                                   \
  pSuite = CuSuiteNew();                                       \
  SUITE_ADD_TEST( pSuite, test );                              \
  CuSuiteRun( pSuite );                                        \
  output = CuStringNew();                                      \
  CuSuiteSummary(pSuite, output);                              \
  CuSuiteDetails(pSuite, output);                              \
  TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "%s\n", output->buffer );  \
  vte_agent_Log( "Test results: " #msg "\n%s\n",               \
                 output->buffer );                             \
}while(0)

NATIVEEXPORT_API void test_fileOpenClose_snfs(void);
NATIVEEXPORT_API void test_fileOpenClose_itfs(void);
NATIVEEXPORT_API void test_fileOpenClose_romfs(void);
NATIVEEXPORT_API void test_fileOpenClose_bfs(void);
NATIVEEXPORT_API void test_fileOpenClose_sim(void);
NATIVEEXPORT_API void test_fileRead_sim(void);
NATIVEEXPORT_API void test_fileRead_romfs(void);
NATIVEEXPORT_API void test_fileRead_snfs(void);
NATIVEEXPORT_API void test_fileRead_itfs(void);
NATIVEEXPORT_API void test_fileRead_bfs(void);
NATIVEEXPORT_API void test_fileWrite_sim(void);
NATIVEEXPORT_API void test_fileWrite_snfs(void);
NATIVEEXPORT_API void test_fileWrite_itfs(void);
NATIVEEXPORT_API void test_fileSeek_snfs(void);
NATIVEEXPORT_API void test_fileSeek_itfs(void);
NATIVEEXPORT_API void test_fileSeek_romfs(void);
NATIVEEXPORT_API void test_fileSeek_bfs(void);
NATIVEEXPORT_API void test_fileSeek_sim(void);
NATIVEEXPORT_API void test_fileSync_sim(void);
NATIVEEXPORT_API void test_fileSync_snfs(void);
NATIVEEXPORT_API void test_fileSync_itfs(void);
NATIVEEXPORT_API void test_fileSync_bfs(void);
NATIVEEXPORT_API void test_fileSync_romfs(void);
NATIVEEXPORT_API void test_fileDelete_sim(void);
NATIVEEXPORT_API void test_fileDelete_snfs(void);
NATIVEEXPORT_API void test_fileDelete_itfs(void);
NATIVEEXPORT_API void test_fileRename_sim(void);
NATIVEEXPORT_API void test_fileRename_snfs(void);
NATIVEEXPORT_API void test_fileRename_itfs(void);
NATIVEEXPORT_API void test_fileDirOpenClose_sim(void);
NATIVEEXPORT_API void test_fileDirOpenClose_romfs(void);
NATIVEEXPORT_API void test_fileDirOpenClose_snfs(void);
NATIVEEXPORT_API void test_fileDirOpenClose_itfs(void);
NATIVEEXPORT_API void test_fileDirRead_sim(void);
NATIVEEXPORT_API void test_fileDirRead_snfs(void);
NATIVEEXPORT_API void test_fileDirRead_itfs(void);
NATIVEEXPORT_API void test_fileDirRenameDelete_sim(void);
NATIVEEXPORT_API void test_fileDirRenameDelete_snfs(void);
NATIVEEXPORT_API void test_fileDirRenameDelete_itfs(void);
NATIVEEXPORT_API void mpe_test_openclose_snfs(void);
NATIVEEXPORT_API void mpe_test_openclose_itfs(void);
NATIVEEXPORT_API void mpe_test_openclose_romfs(void);
NATIVEEXPORT_API void mpe_test_openclose_bfs(void);
NATIVEEXPORT_API void mpe_test_openclose_sim(void);
NATIVEEXPORT_API void mpe_test_read_sim(void);
NATIVEEXPORT_API void mpe_test_read_hiroyo_bfs(void);
NATIVEEXPORT_API void test_mpe_odn(void);
NATIVEEXPORT_API void mpe_test_read_bug(void);
NATIVEEXPORT_API void mpe_test_read_bfs(void);
NATIVEEXPORT_API void mpe_test_read_romfs(void);
NATIVEEXPORT_API void mpe_test_read_snfs(void);
NATIVEEXPORT_API void mpe_test_read_itfs(void);
NATIVEEXPORT_API void mpe_test_seek_sim(void);
NATIVEEXPORT_API void mpe_test_seek_snfs(void);
NATIVEEXPORT_API void mpe_test_seek_itfs(void);
NATIVEEXPORT_API void mpe_test_seek_romfs(void);
NATIVEEXPORT_API void mpe_test_seek_bfs(void);
NATIVEEXPORT_API void mpe_test_stat_sim(void);
NATIVEEXPORT_API void mpe_test_stat_snfs(void);
NATIVEEXPORT_API void mpe_test_stat_itfs(void);
NATIVEEXPORT_API void mpe_test_stat_romfs(void);
NATIVEEXPORT_API void mpe_test_stat_bfs(void);
NATIVEEXPORT_API void mpe_test_write_sim(void);
NATIVEEXPORT_API void mpe_test_write_snfs(void);
NATIVEEXPORT_API void mpe_test_write_itfs(void);
NATIVEEXPORT_API void mpe_test_dir_openclose_sim(void);
NATIVEEXPORT_API void mpe_test_dir_openclose_snfs(void);
NATIVEEXPORT_API void mpe_test_dir_openclose_itfs(void);
NATIVEEXPORT_API void mpe_test_dir_openclose_romfs(void);
NATIVEEXPORT_API void mpe_test_dirread_sim(void);
NATIVEEXPORT_API void mpe_test_dirread_snfs(void);
NATIVEEXPORT_API void mpe_test_dirread_itfs(void);
NATIVEEXPORT_API void mpe_test_dirread_romfs(void);
NATIVEEXPORT_API void mpe_test_loadunload_bfs(void);
NATIVEEXPORT_API void test_mpe_recurseDir_romfs(void);
NATIVEEXPORT_API void test_mpe_recurseDir_snfs(void);
NATIVEEXPORT_API void test_mpe_recurseDir_itfs(void);
NATIVEEXPORT_API void test_mpe_recurseDir_sim(void);
NATIVEEXPORT_API void test_mpe_fileChange_bfs(void);

#endif  /* _TEST_FILE_H_ */
