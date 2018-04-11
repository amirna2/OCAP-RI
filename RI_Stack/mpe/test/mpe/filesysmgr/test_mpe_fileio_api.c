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

#include "test_file.h"

/*
 ** Define our globals here
 */
static mpe_Bool isBfs, isSnfs, isRomfs, isSimulator, isItfs;

static char * fileToOpen;
static int32_t fileToOpenSize;
static char * fileToCreate;
static char * fileToRead;
static int32_t fileToReadSize;
static char * fileToWrite;
static char * fileWriteBuf;
static int32_t fileToWriteSize;
static char * fileNewName;
static char * dirName1;
static char * dirName2;
static char * dirName3;
static char * dirName4;
static char * dirName4slash;
static char * fileDoesNotExist;
static char * dirDoesNotExist1;
static char * dirDoesNotExist2;
static char * fileChangeName;
static char * hiroyo;
static char * readFile;
static int32_t readFileSize;
static char * otherReadFile;
static int32_t otherReadFileSize;
static char * parentDir;
/* this is only needed for ITFS */
static char * fileToReadBuf;
static char * dirFile1;
static char * dirName1Dir2;

/* Local Prototypes ************
 */

//#define PRINT_BUFFER
#ifdef PRINT_BUFFER
static void printBuffer( char* buffer, int buffSize );
#endif // PRINT_BUFFER
/*static void fileChangeEventCallback(mpe_EdHandle edHandle, int eventCode);*/

//#define FILE_CHANGE_WAIT_EVENT
#ifdef FILE_CHANGE_WAIT_EVENT
static void fileChangeWaitEvent(CuTest *tc, int *eventCode, int maxTime);
#endif // FILE_CHANGE_WAIT_EVENT

void setFileSystem(FileSystemType_t type)
{
    if (BFS == type)
    {
        isBfs = true;
        isSimulator = false;
        isSnfs = false;
        isRomfs = false;
        isItfs = false;
    }
    else if (SIM == type)
    {
        isBfs = false;
        isSimulator = true;
        isSnfs = false;
        isRomfs = false;
        isItfs = false;

    }
    else if (ROMFS == type)
    {
        isBfs = false;
        isSimulator = false;
        isSnfs = false;
        isRomfs = true;
        isItfs = false;

    }
    else if (SNFS == type)
    {
        isBfs = false;
        isSimulator = false;
        isSnfs = true;
        isRomfs = false;
        isItfs = false;

    }
    else if (ITFS == type)
    {
        isBfs = false;
        isSimulator = false;
        isSnfs = false;
        isRomfs = false;
        isItfs = true;
    }
    else
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "Unknown file system: setGlobals will fail\n");
    }
}

/*
 ** Set the appropriate file url to test from based on what file system
 ** that is being tested.
 */
void mpe_file_SetGlobals(void)
{
    if (isBfs)
    {
        fileToOpen = "/bfs/TestServer/alphabet";
        fileToOpenSize = 27;
        fileToRead = fileToOpen;
        fileToReadSize = fileToOpenSize;
        fileDoesNotExist = "/bfs/fileDoesNotExist";
        fileChangeName = "/bfs/TestServer2/testfile2";
        hiroyo = "/bfs/TestServer2/org/cablelabs/xlet/vGuide/vGuideXlet.class";
        readFile = "/bfs/TestServer2/testfile2";
        readFileSize = 109593;
        otherReadFile = "/bfs/TestServer3/testfile3";
        otherReadFileSize = 15;
        return;
    }
    else if (isSnfs)
    {
        fileToOpen = "/snfs/qa/filesys/fileToOpen.txt";
        fileToOpenSize = 36;
        fileToCreate = "/snfs/qa/filesys/fileToCreate.txt";
        fileToWrite = "/snfs/qa/filesys/fileToWrite.txt";
        fileWriteBuf = "abcdef";
        fileToWriteSize = 6;
        fileNewName = "/snfs/qa/filesys/fileNewName.txt";
        fileToRead = "/snfs/qa/filesys/fileToRead.txt";
        fileToReadSize = 8;
        dirName1 = "/snfs/qa/filesys/dir1";
        dirName2 = "/snfs/qa/filesys/dir2";
        dirName3 = "/snfs/qa/filesys/dir3";
        dirName4 = "/snfs/qa/filesys/dir4";
        dirName4slash = "/snfs/qa/filesys/dir4/";
        fileDoesNotExist = "/snfs/qa/filesys/fileDoesNotExist.txt";
        dirDoesNotExist1 = "/snfs/qa/filesys/Notdir4";
        dirDoesNotExist2 = "/snfs/qa/filesys/Notdir5";
        fileChangeName = "/snfs/TestServer2/testfile2";
        hiroyo = "/snfs/TestServer2/org/cablelabs/xlet/vGuide/vGuideXlet.class";
        readFile = "/snfs/qa/filesys/testfile2";
        readFileSize = 113119;
        otherReadFile = "/snfs/qa/filesys/testfile3";
        otherReadFileSize = 16;
        parentDir = "/snfs/apps";
        return;
    }
    else if (isRomfs)
    {
        fileToOpen = "/romfs/qa/filesys/fileToOpen.txt";
        fileToOpenSize = 36;
        fileToCreate = "/romfs/qa/filesys/fileToCreate.txt";
        fileToWrite = "/romfs/qa/filesys/fileToWrite.txt";
        fileWriteBuf = "abcdef";
        fileToWriteSize = 6;
        fileNewName = "/romfs/qa/filesys/fileNewName.txt";
        fileToRead = "/romfs/qa/filesys/fileToRead.txt";
        fileToReadSize = 8;
        dirName1 = "/romfs/qa/filesys/dir1";
        dirName2 = "/romfs/qa/filesys/dir2";
        dirName3 = "/romfs/qa/filesys/dir3";
        dirName4 = "/romfs/qa/filesys/dir4";
        dirName4slash = "/romfs/qa/filesys/dir4/";
        fileDoesNotExist = "/romfs/qa/filesys/fileDoesNotExist";
        dirDoesNotExist1 = "/romfs/qa/filesys/Notdir4";
        dirDoesNotExist2 = "/romfs/qa/filesys/Notdir5";
        otherReadFile = "/romfs/qa/filesys/testfile3";
        otherReadFileSize = 16;
        fileChangeName = "/romfs/TestServer2/testfile2";
        hiroyo
                = "/romfs/TestServer2/org/cablelabs/xlet/vGuide/vGuideXlet.class";
        readFile = "/romfs/qa/filesys/testfile2";
        readFileSize = 113119;
        parentDir = "/romfs/apps";
        return;
    }
    else if (isSimulator)
    {
        fileToOpen = "./qa/filesys/fileToOpen.txt";
        fileToOpenSize = 36;
        fileToCreate = "./qa/filesys/fileToCreate.txt";
        fileToWrite = "./qa/filesys/fileToWrite.txt";
        fileWriteBuf = "abcdef";
        fileToWriteSize = 6;
        fileNewName = "./qa/filesys/fileNewName.txt";
        fileToRead = "./qa/filesys/fileToRead.txt";
        fileToReadSize = 8;
        dirName1 = "./qa/filesys/dir1";
        dirName2 = "./qa/filesys/dir2";
        dirName3 = "./qa/filesys/dir3";
        dirName4 = "./qa/filesys/dir4";
        dirName4slash = "./qa/filesys/dir4/";
        dirDoesNotExist1 = "./qa/filesys/Notdir4";
        dirDoesNotExist2 = "./qa/filesys/Notdir5";
        fileDoesNotExist = "./qa/filesys/fileDoesNotExist";
        fileChangeName = "./qa/filesys/testfile2";
        hiroyo = "./qa/TestServer2/org/cablelabs/xlet/vGuide/vGuideXlet.class";
        readFile = "./qa/filesys/testfile2";
        readFileSize = 113119;
        otherReadFile = "./qa/filesys/testfile3";
        otherReadFileSize = 16;
        parentDir = "./filesys";
        return;
    }
    else if (isItfs)
    {
        /* fileOpenClose needs to create the file first before it can be 
         * opened.  So maybe that test should be renamed something else
         *
         * fileRead could be merged to fileReadWrite since fileRead and 
         * fileWrite do the same thing for Itfs.
         *
         */
        fileToOpen = "/itfs/fileToOpen.txt";
        fileToOpenSize = 36;
        fileToCreate = "/itfs/fileToCreate.txt";
        fileToWrite = "/itfs/fileToWrite.txt";
        fileWriteBuf = "abcdef";
        fileToWriteSize = 6;
        fileNewName = "/itfs/fileNewName.txt";
        fileToRead = "/itfs/fileToRead.txt";
        fileToReadBuf = "abcdef\r\n";
        fileToReadSize = 8;
        dirName1 = "/itfs/dir1";
        dirFile1 = "/itfs/dir1/file1";
        dirName1Dir2 = "/itfs/dir1/dir2";
        dirName2 = "/itfs/dir2";
        dirName3 = "/itfs/dir3";
        dirName4 = "/itfs/dir4";
        dirName4slash = "/itfs/dir4/";
        fileDoesNotExist = "/itfs/fileDoesNotExist";
        dirDoesNotExist1 = "/itfs/Notdir4";
        dirDoesNotExist2 = "/itfs/Notdir5";
        otherReadFile = "/itfs/testfile3";
        otherReadFileSize = 16;
        fileChangeName = "/itfs/TestServer2/testfile2";
        readFile = "/itfs/testfile2";
        readFileSize = 113119;
        parentDir = "/itfs/apps";
        return;
    }
}

//NOTE:For these test to run without failure, you need to creat a dir under 'qa' called 'qa\tests\assets2'
//
/**
 * This test will exercise the file open and close apis with all valid parameters.
 * @note Valid asset files must be in place for the 
 *    following tests to complete with a passing result.
 * @assert 1. Opening and closing a read only file 
 *    with MPE_FS_OPEN_READ flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 2. Creating, opening and closing a file 
 *    with flage MPE_FS_OPEN_CAN_CREATE flag returns \" MPE_FS_ERROR_SUCCESS\" 
 * @assert 3. Opening and closing a permission 'write'
 *    file with MPE_FS_OPEN_WRITE flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 4. Opening and closing a permission 'read/write'
 *    file with MPE_FS_OPEN_READWRITE flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 5. Open and close a file that already exits with permissions 'can create'
 *    returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 6. Open and close a non-existent file with permissions 'must create'
 *    returns \"MPE_FS_ERROR_SUCCESS\" 
 * @assert 7. Open, write-to, and close a file with permissions MPE_FS_OPEN_READWRITE
 *    returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 8. Open, append-to and close a file with permissions MPE_FS_OPEN_APPEND
 *    returns \"MPE_FS_ERROR_SUCCESS\"
 * 
 */
void test_mpe_fileOpenClose(CuTest *tc)
{
    mpe_File h = 0;
    mpe_FileError rc;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******test_mpe_fileOpenClose Enter...\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "******test_mpe_fileOpenClose: fileToOpen = %s\n", fileToOpen);

    //assert 1.
    {
        if (isItfs)
        {

            rc = fileOpen(fileToOpen,
                    MPE_FS_OPEN_READ | MPE_FS_OPEN_CAN_CREATE, &h);
            CuAssertIntEquals_Msg(tc, "fileOpen(ITFS OPEN_CAN_CREATE)",
                    MPE_FS_ERROR_SUCCESS, rc);
            rc = fileClose(h);
            CuAssertIntEquals_Msg(tc, "fileClose()", MPE_FS_ERROR_SUCCESS, rc);
        }

        rc = fileOpen(fileToOpen, MPE_FS_OPEN_READ, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READ",
                MPE_FS_ERROR_SUCCESS, rc);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_READ",
                MPE_FS_ERROR_SUCCESS, rc);
        if (isItfs)
        {
            rc = fileDelete(fileToOpen);
            CuAssertIntEquals_Msg(tc, "fileDelete - ITFS init cleanup",
                    MPE_FS_ERROR_SUCCESS, rc);
        }
    }

    if (!isBfs && !isRomfs)
    {
        //assert 2.
        {
            rc = fileOpen(fileToCreate, MPE_FS_OPEN_CAN_CREATE, &h);
            CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_CAN_CREATE",
                    MPE_FS_ERROR_SUCCESS, rc);
            rc = fileClose(h);
            CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_CAN_CREATE",
                    MPE_FS_ERROR_SUCCESS, rc);
            rc = fileDelete(fileToCreate);
            CuAssertIntEquals_Msg(tc, "fileDelete - MPE_FS_OPEN_CAN_CREATE",
                    MPE_FS_ERROR_SUCCESS, rc);
        }

#if 0
        //assert 3.

        {
            rc = fileOpen( fileToCreate, MPE_FS_OPEN_WRITE, &h );
            CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_WRITE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileClose( h );
            CuAssertIntEquals_Msg(tc,"fileClose - MPE_FS_OPEN_WRITE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileDelete( fileToCreate );
            CuAssertIntEquals_Msg(tc,"fileDelete - MPE_FS_OPEN_WRITE",MPE_FS_ERROR_SUCCESS,rc);
        }

        //assert 4.

        {
            rc = fileOpen( fileToCreate, MPE_FS_OPEN_READWRITE, &h );
            CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_READWRITE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileClose( h );
            CuAssertIntEquals_Msg(tc,"fileClose - MPE_FS_OPEN_READWRITE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileDelete( fileToCreate );
            CuAssertIntEquals_Msg(tc,"fileDelete - MPE_FS_OPEN_READWRITE",MPE_FS_ERROR_SUCCESS,rc);
        }

        //assert 5.

        {
            // can-create new file
            rc = fileOpen( fileToCreate, MPE_FS_OPEN_CAN_CREATE, &h );
            CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_CAN_CREATE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileClose( h );
            CuAssertIntEquals_Msg(tc,"fileClose - MPE_FS_OPEN_CAN_CREATE",MPE_FS_ERROR_SUCCESS,rc);

            // can-create existing file
            rc = fileOpen( fileToCreate, MPE_FS_OPEN_CAN_CREATE, &h );
            CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_CAN_CREATE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileClose( h );
            CuAssertIntEquals_Msg(tc,"fileClose - MPE_FS_OPEN_CAN_CREATE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileDelete( fileToCreate );
            CuAssertIntEquals_Msg(tc,"fileDelete - MPE_FS_OPEN_CAN_CREATE",MPE_FS_ERROR_SUCCESS,rc);
        }

        //assert 6.

        {
            (void)fileDelete( fileToCreate );
            rc = fileOpen( fileToCreate, MPE_FS_OPEN_MUST_CREATE, &h );
            CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_MUST_CREATE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileClose( h );
            CuAssertIntEquals_Msg(tc,"fileClose - MPE_FS_OPEN_MUST_CREATE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileDelete( fileToCreate );
            CuAssertIntEquals_Msg(tc,"fileDelete - MPE_FS_OPEN_MUST_CREATE",MPE_FS_ERROR_SUCCESS,rc);
        }

        //assert 7.

        {
            rc = fileOpen( fileToCreate, MPE_FS_OPEN_READWRITE, &h );
            CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_READWRITE",MPE_FS_ERROR_SUCCESS,rc);
            uint32_t count = 6;
            char * buff = "ABCEFG";
            rc = fileWrite( h, &count, buff );
            CuAssertIntEquals_Msg(tc,"fileWrite",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileClose( h );
            CuAssertIntEquals_Msg(tc,"fileClose - MPE_FS_OPEN_READWRITE",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileOpen( fileToCreate, MPE_FS_OPEN_APPEND, &h );
            CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_APPEND",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileClose( h );
            CuAssertIntEquals_Msg(tc,"fileClose - MPE_FS_OPEN_APPEND",MPE_FS_ERROR_SUCCESS,rc);
            rc = fileDelete( fileToCreate );
            CuAssertIntEquals_Msg(tc,"fileDelete - MPE_FS_OPEN_APPEND",MPE_FS_ERROR_SUCCESS,rc);
        }
#endif
    }
}
/**
 * This test will exercise the file open and close apis with permission parameters
 * MPE_FS_OPEN_READ.
 * @note Valid asset files must be in place for the 
 *    following tests to complete with a passing result.
 * @assert 1. Opening a file with permissions 
 *    with MPE_FS_OPEN_READ flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 2. Reading bytes from the file handle returned from theMPE_FS_OPEN_READ
 *   returns \"MPE_FS_OPEN_SUCCESS\"
 * @assert 3. The call to read from the valid file handle should return the correct number 
 *    of characters read.
 * @assert 4. The bytes read and returned in the associated buffer 
 *    from the file should match the bytes in the file. 
 * @assert 5. The file should close successfully and return \"MPE_FS_ERROR_SUCCESS\"
 */
void test_mpe_fileRead(CuTest *tc)
{
    mpe_File h = 0;
    mpe_Error rc;
    uint32_t count;
    char buff[6] =
    { 0 };
    const char * compareBuf = "abcdef";

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******test_mpe_fileRead Enter...\n");
    // API: mpe_Error mpe_fileRead (mpe_File h, uint32_t* count, void* buffer);

    if (isItfs)
    {
        /* do some initialization for this test */
        uint32_t size = fileToReadSize;
        rc = fileOpen(fileToRead, MPE_FS_OPEN_WRITE | MPE_FS_OPEN_CAN_CREATE,
                &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, h != NULL);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Start to write the file!\n");
        rc = fileWrite(h, &size, fileToReadBuf);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Done writing the file!\n");
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, size == fileToReadSize);
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Close the file!\n");
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
    }

    //assert 1.
    rc = fileOpen(fileToRead, MPE_FS_OPEN_READ, &h);
    CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READ",
            MPE_FS_ERROR_SUCCESS, rc);
    CuAssertTrue(tc, h != 0);

    count = 6; //abcdef
    //assert 2.
    //assert 3.
    rc = fileRead(h, &count, buff);
    CuAssertIntEquals_Msg(tc, "fileRead", MPE_FS_ERROR_SUCCESS, rc);
    CuAssertIntEquals_Msg(tc, "count should equal 6", count, 6);
    //assert 4.
    buff[6] = '\0';
    CuAssertStrEquals_Msg(tc, "file contents should be abcdef", buff,
            compareBuf);
    //assert 5.
    rc = fileClose(h);
    CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_READWRITE",
            MPE_FS_ERROR_SUCCESS, rc);

    if (isItfs)
    {
        rc = fileDelete(fileToRead);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);
    }
}

/**
 * This test will exercise the file open and close apis with permission parameters
 * MPE_FS_OPEN_WRITE.
 * @note Valid asset files must be in place for the 
 *    following tests to complete with a passing result.
 * @assert 1. Opening a file with permissions 
 *    with MPE_FS_OPEN_MUST_CREATE flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 2. Opening a file with permissions 
 *    with MPE_FS_OPEN_READWRITE flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 3. Writing bytes to the file handle returned from the MPE_FS_OPEN_READWRITE
 *    returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 4. The call to write from the valid file handle returns the correct number 
 *    of characters written.
 * @assert 5. The file should close successfully and return \"MPE_FS_ERROR_SUCCESS\"
 * @assert 6. The bytes written to the file are read successful, exactly as they are 
 *    written.
 */
void test_mpe_fileWrite(CuTest * tc)
{
    mpe_File h = 0;
    mpe_Error rc;
    uint32_t count = 6; //ABC123
    char buff[6] =
    { 0 };
    const char * compareBuf = "ABC123";

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******test_mpe_fileWrite Enter...\n");
    /* Can't write to romfs or bfs */
    if (!isBfs && !isRomfs)
    {

        //Have to create this file here because grabbing files from p4 results in readonly perms.
        //and causes an immediate failure.
        //assert 1.
        (void) fileDelete(fileToWrite);
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_MUST_CREATE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_MUST_CREATE",
                MPE_FS_ERROR_SUCCESS, rc);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

        //assert 2.
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_READWRITE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READWRITE",
                MPE_FS_ERROR_SUCCESS, rc);

        //assert 3.
        rc = fileWrite(h, &count, (void *) compareBuf);
        CuAssertIntEquals_Msg(tc, "fileWrite ", MPE_FS_ERROR_SUCCESS, rc);
        //assert 4.
        CuAssertTrue(tc, count == 6);
        count = 6;

        //assert 5.
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

        rc = fileOpen(fileToWrite, MPE_FS_OPEN_READWRITE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READWRITE",
                MPE_FS_ERROR_SUCCESS, rc);

        rc = fileRead(h, &count, buff);
        CuAssertIntEquals_Msg(tc, "FileRead", MPE_FS_ERROR_SUCCESS, rc);
        CuAssert(tc, "count should equal 6", count == 6);

        buff[6] = '\0';

        //assert 6.
        CuAssertStrEquals_Msg(tc, "file contents should be ABC123", buff,
                compareBuf);

        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose ", MPE_FS_ERROR_SUCCESS, rc);

        rc = fileDelete(fileToWrite);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);
    }
}

/**
 * This test will exercise basic fuctionality of the api mpe/mpeos_fileSeek() by
 *    establishing the correct size of a file.
 * @note Valid asset files must be in place for the 
 *    following tests to complete with a passing result.
 * @assert 1. Opening a file with permissions 
 *    with MPE_FS_OPEN_READ flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 2. Calling mpe/mpeos_fileSeek() with seek flag MPE_FS_SEEK_CUR returns
 *    MPE_FS_ERROR_SUCCESS.
 * @assert 3. Calling mpe/mpeos_fileSeek() with seek flag MPE_FS_SEEK_END returns
 *    MPE_FS_ERROR_SUCCESS.
 * @assert 4. Calling mpe/mpeos_fileSeek() with seek flag MPE_FS_SEEK_SET returns
 *    MPE_FS_ERROR_SUCCESS.
 */
void test_mpe_fileSeek(CuTest *tc)
{
    mpe_File h = 0;
    mpe_Error rc;
    int64_t fs = 0;
    int64_t offset = 0;
    int64_t oldPos = 0;
    int64_t eofPos = 0;
    int64_t fileSize = (int64_t) fileToReadSize;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******test_mpe_fileSeek Enter...\n");

    if (isItfs)
    {
        /* do some initialization for this test */
        uint32_t size = fileToReadSize;
        rc = fileOpen(fileToRead, MPE_FS_OPEN_WRITE | MPE_FS_OPEN_CAN_CREATE,
                &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, h != NULL);
        rc = fileWrite(h, &size, fileToReadBuf);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, size == fileToReadSize);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
    }

    //assert 1.
    rc = fileOpen(fileToRead, MPE_FS_OPEN_READ, &h);
    CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READ",
            MPE_FS_ERROR_SUCCESS, rc);

    //assert 2.
    rc = fileSeek(h, MPE_FS_SEEK_CUR, &offset);
    CuAssertIntEquals_Msg(tc, "fileSeek", MPE_FS_ERROR_SUCCESS, rc);
    oldPos = offset;
    offset = 0;

    //assert 3.
    rc = fileSeek(h, MPE_FS_SEEK_END, &offset);
    CuAssertIntEquals_Msg(tc, "fileSeek", MPE_FS_ERROR_SUCCESS, rc);
    eofPos = offset;
    offset = 0;

    //assert 4.
    rc = fileSeek(h, MPE_FS_SEEK_SET, &oldPos);
    CuAssert(tc, "fileSeek", MPE_FS_ERROR_SUCCESS == rc);
    fs = eofPos - oldPos;
    //assert 5.
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "filesize = %d, fs = %d\n",
            (int) fileSize, (int) fs);
    CuAssertTrue(tc, fileSize == fs);

    rc = fileClose(h);
    CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

    if (isItfs)
    {
        rc = fileDelete(fileToRead);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);
    }
}

/**
 * This test will exercise basic fuctionality of the api mpeos/mpe_fileSeek() by
 *    establishing the correct size of a file.
 * @note Valid asset files must be in place for the 
 *    following tests to complete with a passing result.
 * @assert 1. Opening a file with permissions 
 *    with MPE_FS_OPEN_READ flag returns \"MPE_FS_ERROR_SUCCESS\"
 * @assert 2. Calling mpeos/mpe_fileSeek() with seek flag MPE_FS_SEEK_END returns
 *    MPE_FS_ERROR_SUCCESS.
 * @assert 3. Calling mpeos/mpe_fileGetEOF() with returns MPE_FS_ERROR_SUCCESS.
 * @assert 4. Comparing eof position returned with seek to end and eof returned from
 *    call to mpeos/mpe_fileGetEOF() should be the same.
 * @assert 5. File close should return MPE_FS_ERROR_SUCCESS
 */
void test_mpe_fileGetEOF(CuTest *tc)
{
#if 0
    const char * fileToRead = "../../../../../qa/tests/assets/fileToRead.txt";
    //fileHandle to use
    mpe_File h = 0;
    mpe_Error rc;

    //assert 1.
    rc = fileOpen( fileToRead, MPE_FS_OPEN_READ, &h );
    CuAssertIntEquals_Msg(tc,"fileOpen - MPE_FS_OPEN_READ",MPE_FS_ERROR_SUCCESS,rc);

    int64_t fs = 0;
    int64_t offset = 0;
    int64_t eofPos = 0;
    uint64_t eof = 0;
    const int64_t fileSize = 6;

    //I know I'm at the beginnig so seek to the end.
    //assert 2.
    rc = fileSeek( h, MPE_FS_SEEK_END, &offset );
    CuAssertIntEquals_Msg(tc,"fileSeek",MPE_FS_ERROR_SUCCESS,rc);
    eofPos = offset;
    offset = 0;

    //assert 3.
    rc = mpe_fileGetEOF( h, &eof );
    CuAssertIntEquals_Msg(tc,"mpe_fileGetEOF",MPE_FS_ERROR_SUCCESS,rc);
    //If Seek worked, this should return PASS
    CuAssertTrue( tc , (int64_t)eof == eofPos );

    //assert 4.
    rc = fileClose( h );
    CuAssertIntEquals_Msg(tc,"fileClose",MPE_FS_ERROR_SUCCESS,rc);
#endif
}
/**
 * This test will exercise basic fuctionality of the api mpe_fileSetEOF() by
 *    establishing that the api executes successfully.
 *    @assert 1. Calling mpe_fileSetEOF() with valid parameters returns MPE_FW_ERROR_SUCCESS
 */
void test_mpe_fileSetEOF(CuTest *tc)
{
#if 0 
    const char * fileToCreate = "../../../../../qa/tests/assets/fileToCreate.txt";
    mpe_File h = 0;
    mpe_Error rc;
    uint32_t count = 6; //ABC123  6 chars in the buffer
    char buff[6] =
    {   0};
    const char * compareBuf = "ABC123";

    (void)fileDelete( fileToCreate );
    rc = mpe_fileOpen( fileToCreate, MPE_FS_OPEN_MUST_CREATE, &h );
    CuAssertIntEquals_Msg(tc,"mpe_fileOpen - MPE_MUST_CREATE",MPE_FS_ERROR_SUCCESS,rc);
    CuAssertTrue( tc, h != 0 );

    rc = mpe_fileClose( h );
    CuAssertIntEquals_Msg(tc,"mpe_fileClose ",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileOpen( fileToCreate, MPE_FS_OPEN_WRITE, &h );
    CuAssertIntEquals_Msg(tc,"mpe_fileOpen - MPE_OPEN_WRITE",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileWrite( h, &count, (void *)compareBuf );
    CuAssertIntEquals_Msg(tc,"mpe_fileWrite ",MPE_FS_ERROR_SUCCESS,rc);

    //assert 1.
    rc = mpe_fileSetEOF( h );
    CuAssertIntEquals_Msg(tc,"mpe_fileSetEOF",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileClose( h );
    CuAssertIntEquals_Msg(tc,"mpe_fileClose",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileDelete( fileToCreate );
    CuAssertIntEquals_Msg(tc,"mpe_fileDelete",MPE_FS_ERROR_SUCCESS,rc);
#endif
}

/**
 * This test will exercise basic fuctionality of the api mpe_fileSync() by
 *    establishing that the api executes successfully.
 *    @assert 1. Calling mpe_fileSync() with valid parameters returns MPE_FW_ERROR_SUCCESS
 */
void test_mpe_fileSync(CuTest *tc)
{
#if 0   
    mpe_File h = 0;
    mpe_Error rc;
    uint32_t count = 6; //ABC123
    char buff[6] =
    {   0};
    const char * compareBuf = "ABC123";

    (void)fileDelete( fileToWrite );
    rc = mpe_fileOpen( fileToWrite, MPE_FS_OPEN_MUST_CREATE, &h );
    CuAssertIntEquals_Msg(tc,"mpe_fileOpen - MPE_FS_OPEN_MUST_CREATE",MPE_FS_ERROR_SUCCESS,rc);
    rc = mpe_fileClose( h );
    CuAssertIntEquals_Msg(tc,"mpe_fileClose",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileOpen( fileToWrite, MPE_FS_OPEN_READWRITE, &h );
    CuAssertIntEquals_Msg(tc,"mpe_fileOpen - MPE_FS_OPEN_READWRITE",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileWrite( h, &count, (void *)compareBuf );
    CuAssertIntEquals_Msg(tc,"mpe_fileWrite ",MPE_FS_ERROR_SUCCESS,rc);

    //assert 1.
    rc = mpe_fileSync( h );
    CuAssertIntEquals_Msg(tc,"mpe_fileSync ",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileClose( h );
    CuAssertIntEquals_Msg(tc,"mpe_fileClose",MPE_FS_ERROR_SUCCESS,rc);

    rc = mpe_fileDelete(fileToWrite);
    CuAssertIntEquals_Msg(tc,"mpe_fileDelete ",MPE_FS_ERROR_SUCCESS,rc);
#endif
}

/**
 * This test will exercise basic fuctionality of the api mpe_fileDelete() by
 *    establishing that the api executes successfully.
 *    @assert 1. Opening a file with permissions MPE_FS_OPEN_MUST_CREATE returns
 *       MPE_FS_ERROR_SUCCESS.
 *    @assert 2. Calling mpe_fileDelete on a valid file returns MPE_FS_ERROR_SUCCESS.
 *    @assert 3. Attempting to open a non-existent file with permissions MPE_FS_OPEN_READ
 *       should return MPE_FS_ERROR_NOT_FOUND.
 */
void test_mpe_fileDelete(CuTest *tc)
{
    mpe_File h = 0;
    mpe_Error rc;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******test_mpe_fileDelete Enter...\n");
    if (!isBfs && !isRomfs)
    {

        //assert 1.
        (void) fileDelete(fileToWrite);
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_MUST_CREATE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_MUST_CREATE",
                MPE_FS_ERROR_SUCCESS, rc);

        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

        //assert 2.
        rc = fileDelete(fileToWrite);
        CuAssertIntEquals_Msg(tc, "fileDelete ", MPE_FS_ERROR_SUCCESS, rc);

        //assert 3.
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_READ, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READ",
                MPE_FS_ERROR_NOT_FOUND, rc);
    }
}

/**
 * This test will exercise basic fuctionality of the api mpeos/mpe_fileRename() by
 *    establishing that the api executes successfully.
 *    @assert 1. Opening a file with permissions MPE_FS_OPEN_MUST_CREATE returns
 *       MPE_FS_ERROR_SUCCESS.
 *    @assert 2. Calling mpeos/mpe_fileRename on a valid file returns MPE_FS_ERROR_SUCCESS.
 *    @assert 3. Attempting to open the renamed file with permissions MPE_FS_OPEN_READ
 *       should return MPE_FS_ERROR_SUCCESS.
 *    @assert 4. Deleteing the renamed file should return MPE_FS_ERROR_SUCCESS.
 */
void test_mpe_fileRename(CuTest * tc)
{
    mpe_File h = 0;
    mpe_Error rc;

    TRACE(MPE_LOG_TRACE1, MPE_MOD_TEST, "Entering 'test_mpe_fileRename()'\n");
    if (!isBfs && !isRomfs)
    {

        //assert 1.
        (void) fileDelete(fileToWrite);
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_MUST_CREATE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_MUST_CREATE",
                MPE_FS_ERROR_SUCCESS, rc);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

        //assert 2.
        rc = fileRename(fileToWrite, fileNewName);
        CuAssertIntEquals_Msg(tc, "fileRename", MPE_FS_ERROR_SUCCESS, rc);

        //assert 3.
        rc = fileOpen(fileNewName, MPE_FS_OPEN_READ, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READ",
                MPE_FS_ERROR_SUCCESS, rc);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

        //assert 4.
        rc = fileDelete(fileNewName);
        CuAssertIntEquals_Msg(tc, "fileDelete ", MPE_FS_ERROR_SUCCESS, rc);

        // verify that the original file is gone 
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_READ, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_READ",
                MPE_FS_ERROR_NOT_FOUND, rc);
    }
}

/**
 * This test will exercise basic fuctionality of the apis mpeos/mpe_dirOpen() and mpeos/mpe_dirClose() by
 *    establishing that the api executes successfully.
 *    @assert 1. Opening a directory with valid parameters returns
 *       MPE_FS_ERROR_SUCCESS.
 *    @assert 2. Closing a directory with valid parameters returns 
 *       MPE_FS_ERROR_SUCCESS.
 */
void test_mpe_dirOpenClose(CuTest *tc)
{
    mpe_Dir dirHdle;
    mpe_Error rc;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******test_mpe_dirOpenClose Enter...\n");
    if (isItfs)
    {
        rc = dirCreate(dirName1);
        CuAssertIntEquals_Msg(tc, "dirCreate", MPE_FS_ERROR_SUCCESS, rc);
    }

    if (!isBfs)
    {

        //assert 1.
        rc = dirOpen(dirName1, &dirHdle);
        CuAssertIntEquals_Msg(tc, "dirOpen", MPE_FS_ERROR_SUCCESS, rc);

        //assert 2.
        rc = dirClose(dirHdle);
        CuAssertIntEquals_Msg(tc, "dirClose", MPE_FS_ERROR_SUCCESS, rc);
    }

    if (isItfs)
    {
        rc = dirDelete(dirName1);
        CuAssertIntEquals_Msg(tc, "dirDelete", MPE_FS_ERROR_SUCCESS, rc);
    }

}

/**
 * This test will exercise basic fuctionality of the apis mpeos/mpe_dirOpen() and mpeos/mpe_dirClose() by
 *    establishing that the api executes successfully.
 *    @assert 1. Opening a directory with valid parameters returns
 *       MPE_FS_ERROR_SUCCESS.
 *    @assert 2. Reading entries from a directory using mpeos/mpe_dirRead()
 *       with valid parameters should return MPE_FS_ERROR_SUCCESS while
 *       unread entries still exits and MPE_FS_ERROR_FAILURE when all entries 
 *       have been read.
 *    @assert 3. Closing a directory with valid parameters returns 
 *       MPE_FS_ERROR_SUCCESS.
 */
void test_mpe_dirRead(CuTest *tc)
{
    mpe_Dir dirHdle;
    mpe_Error rc;
    mpe_DirEntry dirEnt;
    int ii = 1;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******test_mpe_dirRead Enter...\n");
    if (isItfs)
    {
        mpe_File h;
        rc = dirCreate(dirName1);
        CuAssertIntEquals_Msg(tc, "dirCreate", MPE_FS_ERROR_SUCCESS, rc);

        rc = fileOpen(dirFile1, MPE_FS_OPEN_CAN_CREATE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_CAN_CREATE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, h != NULL);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);
    }

    if (!isBfs)
    {

        //assert 1.
        rc = dirOpen(dirName1, &dirHdle);
        CuAssertIntEquals_Msg(tc, "dirOpen", MPE_FS_ERROR_SUCCESS, rc);

        //assert 2.
        do
        {
            rc = dirRead(dirHdle, &dirEnt);
            //There is only 1 file in the directory. That is 3 including "." and ".."
            if (ii < 2)
            {
                CuAssertIntEquals_Msg(tc, "dirRead", MPE_FS_ERROR_SUCCESS, rc);
                // removed the file size from the trace statement because it
                // cannot handle 64-bit values.
                TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                        "Entry: %d, name: %s isDir: %d\n", ii, dirEnt.name,
                        dirEnt.isDir);
            }
            ii++;
        } while (rc == MPE_FS_ERROR_SUCCESS);

        //assert 3.
        rc = dirClose(dirHdle);
        CuAssertIntEquals_Msg(tc, "dirClose", MPE_FS_ERROR_SUCCESS, rc);
    }

    if (isItfs)
    {
        rc = fileDelete(dirFile1);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);
        rc = dirDelete(dirName1);
        CuAssertIntEquals_Msg(tc, "dirDelete", MPE_FS_ERROR_SUCCESS, rc);
    }
}

/**
 * This test will exercise basic fuctionality of the apis mpeos/mpe_dirRename() and mpeos/mpe_dirDelete() by
 *    establishing that the api executes successfully.
 *    @assert 1. Renaming a directory with mpeos/mpe_dirRename() passing valid parameters returns
 *       MPE_FS_ERROR_SUCCESS.
 *    @assert 2. Opening and closing a newly created directory should return MPE_FS_ERROR_SUCCESS.
 *    @assert 3. Rename the new dir back to its original name should return MPE_FS_ERROR_SUCCESS.
 *    @assert 4. Attempting to delete a dir that doesn't exist should return MPE_FS_ERROR_FAILURE.
 *    @assert 5. Verify that the dir is deleted by attempting to create the dir.  This creation and 
 *       deletion should return MPE_FS_ERROR_SUCCESS.
 */
void test_mpe_dirRenameDelete(CuTest *tc)
{
    mpe_Dir dirHdle;
    mpe_Error rc;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "******test_mpe_dirRenameDelete Enter...\n");

    if (isItfs)
    {
        rc = dirCreate(dirName1);
        CuAssertIntEquals_Msg(tc, "dirCreate", MPE_FS_ERROR_SUCCESS, rc);
    }

    if (!isBfs && !isRomfs)
    {

        //assert 1.
        rc = dirRename(dirName1, dirName2);
        CuAssertIntEquals_Msg(tc, "dirRename", MPE_FS_ERROR_SUCCESS, rc);

        //assert 2.
        rc = dirOpen(dirName2, &dirHdle);
        CuAssertIntEquals_Msg(tc, "dirOpen", MPE_FS_ERROR_SUCCESS, rc);

        rc = dirClose(dirHdle);
        CuAssertIntEquals_Msg(tc, "dirClose", MPE_FS_ERROR_SUCCESS, rc);

        //assert 3.
        rc = dirRename(dirName2, dirName1);
        CuAssertIntEquals_Msg(tc, "dirRename", MPE_FS_ERROR_SUCCESS, rc);

        // Dir not exist, should return error.
        //assert 4.
        rc = dirDelete(dirName3);
        CuAssertIntEquals_Msg(tc, "dirDelete", MPE_FS_ERROR_NOT_FOUND, rc);

        // Creation of the dir, chose prev deleted one, should not exist.
        //assert 5
        rc = dirCreate(dirName3);
        CuAssertIntEquals(tc, MPE_FS_ERROR_SUCCESS, rc);

        rc = dirDelete(dirName3);
        CuAssertIntEquals(tc, MPE_FS_ERROR_SUCCESS, rc);
    }

    if (isItfs)
    {
        rc = dirDelete(dirName1);
        CuAssertIntEquals_Msg(tc, "dirDelete", MPE_FS_ERROR_SUCCESS, rc);
    }
}

/*
 ** These are test from Brent that I am currently adding to the automated test environment.
 **
 */

/*
 ** Open and close should be support by all file systems: bfs, snfs, hd, romfs
 */
void mpe_test_openclose(CuTest *tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File h = NULL;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_openclose Enter...\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "----- %s: Test Open & Close (%s:%d) ----------\n", TESTNAME,
            __FILE__, __LINE__);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  open & close file '%s'\n", fileToOpen);

    if (isItfs)
    {
        rc
                = fileOpen(fileToOpen, MPE_FS_OPEN_READ
                        | MPE_FS_OPEN_CAN_CREATE, &h);
        CuAssertIntEquals_Msg(tc,
                "fileOpen(ITFS OPEN_CAN_CREATE|MPE_FS_OPEN_READ)",
                MPE_FS_ERROR_SUCCESS, rc);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose()", MPE_FS_ERROR_SUCCESS, rc);
    }

    // Open the file
    rc = fileOpen(fileToOpen, MPE_FS_OPEN_READ, &h);
    CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_READ) - return code",
            MPE_FS_ERROR_SUCCESS, rc);

    // Close the file
    rc = fileClose(h);
    CuAssertIntEquals_Msg(tc, "fileClose - return code", MPE_FS_ERROR_SUCCESS,
            rc);

    if (isItfs)
    {
        rc = fileDelete(fileToOpen);
        CuAssertIntEquals_Msg(tc, "fileDelete()", MPE_FS_ERROR_SUCCESS, rc);
    }

    // try to open a non-existant file
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  open bogus file '%s'\n",
            fileDoesNotExist);

    rc = fileOpen(fileDoesNotExist, MPE_FS_OPEN_READ, &h);
    CuAssert(tc, "fileOpen(non-existant file) - file not found", (rc
            != MPE_FS_ERROR_SUCCESS));
    return;
}

void mpe_test_read_hiroyo(CuTest *tc)
{
    mpe_File hFile = NULL;
    char acBuffer[BUFFERSIZE];
    uint32_t bufferSize;

    CuAssertIntEquals(tc, MPE_FS_ERROR_SUCCESS, fileOpen(hiroyo,
            MPE_FS_OPEN_READ, &hFile));

    bufferSize = BUFFERSIZE;
    CuAssertIntEquals(tc, MPE_FS_ERROR_SUCCESS, fileRead(hFile, &bufferSize,
            acBuffer));

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "DEBUG: (%s)\n", acBuffer);

    CuAssertIntEquals(tc, MPE_FS_ERROR_SUCCESS, fileClose(hFile));
}

void mpe_test_bug(CuTest *tc)
{
    mpe_File hFile = NULL;
    char buffer[BUFFERSIZE];
    uint32_t count = readFileSize - 1;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Trying to read %s starting at %d.\n",
            readFile, count);

    fileOpen(readFile, MPE_FS_OPEN_READ, &hFile);
    fileRead(hFile, &count, buffer);
    fileClose(hFile);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "TEST Done\n");
} /* end mpe_test_bug(CuTest*) */

/*
 ** File reading should be support by the all file systems: bfs, snfs, hd, romfs
 */

void mpe_test_read(CuTest *tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File hFile = NULL;
    char buffer[BUFFERSIZE];
    uint32_t count;
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_read Enter...\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "----- %s: Test Read From Existing File (%s:%d) ----------\n",
            TESTNAME, __FILE__, __LINE__);

    /* Test 1: Read file using big buffers. Will not have to multi call to fileRead().
     */
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test 1: Read file '%s' with big buffers\n", readFile);

    if (isItfs)
    {
        int32_t count = otherReadFileSize;
        char *buffer = NULL;
        /* allocate a buffer to write */
        rc = mpe_memAllocP(MPE_MEM_TEST, otherReadFileSize, (void **) &buffer);
        CuAssertIntEquals_Msg(tc, "mpe_memAllocP()", MPE_SUCCESS, rc);
        /* fill the buffer with data */
        memset(buffer, 'a', sizeof(buffer));
        /* open the file to write first */
        rc = fileOpen(otherReadFile,
                MPE_FS_OPEN_WRITE | MPE_FS_OPEN_CAN_CREATE, &hFile);
        CuAssertIntEquals_Msg(tc,
                "fileOpen(MPE_FS_OPEN_WRITE | MPE_FS_OPEN_CAN_CREATE)",
                MPE_FS_ERROR_SUCCESS, rc);
        /* write the data */
        rc = fileWrite(hFile, (unsigned long *) &count, buffer);
        CuAssertIntEquals_Msg(tc, "fileWrite()", MPE_FS_ERROR_SUCCESS, rc);
        CuAssertIntEquals_Msg(tc, "fileWrite()", otherReadFileSize, count);
        rc = fileClose(hFile);
        CuAssertIntEquals_Msg(tc, "fileClose()", MPE_FS_ERROR_SUCCESS, rc);

        /* free the buffer */
        mpe_memFreeP(MPE_MEM_TEST, buffer);
    }

    rc = fileOpen(otherReadFile, MPE_FS_OPEN_READ, &hFile);
    CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_READ)",
            MPE_FS_ERROR_SUCCESS, rc);

    /* read w/ bigger buffer than necessary */
    count = BUFFERSIZE;
    rc = fileRead(hFile, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead - return code", MPE_FS_ERROR_SUCCESS,
            rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", otherReadFileSize, count);
    count = 1;
    rc = fileRead(hFile, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead - return code", MPE_FS_ERROR_SUCCESS,
            rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", 0, count);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "mpe_test_read: Test 1 results [%d] (%s)\n", count, buffer);

    /* Finish test 1 of readFile
     */
    rc = fileClose(hFile);
    CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

    /* start DEBUG
     */
    rc = fileOpen(readFile, MPE_FS_OPEN_READ, &hFile);
    rc = fileClose(hFile);
    /* end DEBUG
     */

    /* Test 2: readFile using smaller buffers then the file size.
     */
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test 2: Read file '%s' with small buffers\n", readFile);

    /* open file */
    rc = fileOpen(otherReadFile, MPE_FS_OPEN_READ, &hFile);
    CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_READ)",
            MPE_FS_ERROR_SUCCESS, rc);

    /* read w/ smaller buffers */
    count = otherReadFileSize - 1;
    count = (BUFFERSIZE < count) ? BUFFERSIZE : count;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "DEBUG: %d count\n", count);

    rc = fileRead(hFile, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead - return code", MPE_FS_ERROR_SUCCESS,
            rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", (otherReadFileSize - 1), count);
    count = otherReadFileSize - 1;
    rc = fileRead(hFile, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead - return code", MPE_FS_ERROR_SUCCESS,
            rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", 1, count);
    count = 1;
    rc = fileRead(hFile, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead - return code", MPE_FS_ERROR_SUCCESS,
            rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", 0, count);

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "mpe_test_read: Test 2 results [%d] (%s)\n", count, buffer);

    /* close file */
    rc = fileClose(hFile);
    CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

    /* Test 3: Use a buffer with the exact size of the file.
     */
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "Test 3: Read file '%s' with exact buffers\n", readFile);

    rc = fileOpen(otherReadFile, MPE_FS_OPEN_READ, &hFile);
    CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_READ)",
            MPE_FS_ERROR_SUCCESS, rc);

    count = otherReadFileSize;
    rc = fileRead(hFile, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead - return code", MPE_FS_ERROR_SUCCESS,
            rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", otherReadFileSize, count);
    count = 1;
    rc = fileRead(hFile, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead - return code", MPE_FS_ERROR_SUCCESS,
            rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", 0, count);

    /* close file */
    rc = fileClose(hFile);
    CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

    if (isItfs)
    {
        rc = fileDelete(otherReadFile);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);
    }

    return;
}

/*
 ** file seek should be support by all file systems: bfs, snfs, hd, romfs
 */

void mpe_test_seek(CuTest *tc)
{

    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File h = NULL;
    char buffer[BUFFERSIZE];
    uint32_t count;
    int64_t offset;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_seek Enter...\n");
#define SET_POS		6
#define CUR_POS		3
#define END_POS		fileToReadSize

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "----- %s: Test Seek Within Existing File (%s:%d) ----------\n",
            TESTNAME, __FILE__, __LINE__);

    if (isItfs)
    {
        /* do some initialization for this test */
        uint32_t size = fileToReadSize;
        rc = fileOpen(fileToRead, MPE_FS_OPEN_WRITE | MPE_FS_OPEN_CAN_CREATE,
                &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, h != NULL);
        rc = fileWrite(h, &size, fileToReadBuf);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, size == fileToReadSize);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
    }

    // open file 
    rc = fileOpen(fileToRead, MPE_FS_OPEN_READ, &h);
    CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_READ)",
            MPE_FS_ERROR_SUCCESS, rc);

    // seek into file from beginning
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  seek using SET_POS in file '%s'\n",
            fileToRead);

    offset = SET_POS;
    rc = fileSeek(h, MPE_FS_SEEK_SET, &offset);
    CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_SET) - return code",
            MPE_FS_ERROR_SUCCESS, rc);
    CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_SET) - returned offset",
            (SET_POS), (int32_t) offset);

    // read in rest of file
    count = BUFFERSIZE;
    rc = fileRead(h, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead(from MPE_FS_SEEK_SET) - return code",
            MPE_FS_ERROR_SUCCESS, rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", (fileToReadSize - SET_POS),
            count);

    // seek into file from current
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  seek using CUR_POS in file '%s'\n",
            fileToRead);

    if (isItfs)
    {
        offset = fileToReadSize - (CUR_POS * 2);
        rc = fileSeek(h, MPE_FS_SEEK_SET, &offset);
        CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_CUR) - return code",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertIntEquals_Msg(tc,
                "fileSeek(MPE_FS_SEEK_CUR) - returned offset", (fileToReadSize
                        - (CUR_POS * 2)), (int32_t) offset);
    }
    else
    {
        offset = -(CUR_POS * 2);
        rc = fileSeek(h, MPE_FS_SEEK_CUR, &offset);
        CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_CUR) - return code",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertIntEquals_Msg(tc,
                "fileSeek(MPE_FS_SEEK_CUR) - returned offset", (fileToReadSize
                        - (CUR_POS * 2)), (int32_t) offset);
    }

    offset = +(CUR_POS);
    rc = fileSeek(h, MPE_FS_SEEK_CUR, &offset);
    CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_CUR) - return code",
            MPE_FS_ERROR_SUCCESS, rc);
    CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_CUR) - returned offset",
            (fileToReadSize - (CUR_POS)), (int32_t) offset);

    // read in rest of file
    count = BUFFERSIZE;
    rc = fileRead(h, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead(from MPE_FS_SEEK_CUR) - return code",
            MPE_FS_ERROR_SUCCESS, rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", (CUR_POS), count);

    // seek into file from end
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  seek using END_POS in file '%s'\n",
            fileToRead);

    // ITFS cannot handle negative offsets
    if (isItfs)
    {
        offset = 0;
        rc = fileSeek(h, MPE_FS_SEEK_SET, &offset);
        CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_END) - return code",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertIntEquals_Msg(tc,
                "fileSeek(MPE_FS_SEEK_END) - returned offset", (fileToReadSize
                        -END_POS), (int32_t) offset);

    }
    else
    {
        offset = -(END_POS);
        rc = fileSeek(h, MPE_FS_SEEK_END, &offset);
        CuAssertIntEquals_Msg(tc, "fileSeek(MPE_FS_SEEK_END) - return code",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertIntEquals_Msg(tc,
                "fileSeek(MPE_FS_SEEK_END) - returned offset", (fileToReadSize
                        -END_POS), (int32_t) offset);
    }
    // read in rest of file
    count = BUFFERSIZE;
    rc = fileRead(h, &count, buffer);
    CuAssertIntEquals_Msg(tc, "fileRead(from MPE_FS_SEEK_END) - return code",
            MPE_FS_ERROR_SUCCESS, rc);
    CuAssertIntEquals_Msg(tc, "fileRead - size", (END_POS), count);

    // close file
    rc = fileClose(h);
    CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

    if (isItfs)
    {
        /* remove the file */
        rc = fileDelete(fileToRead);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);
    }

    return;
}

/*
 ** Get Stat should be support by all file systems: bfs, snfs, hd, romfs
 */

void mpe_test_stat(CuTest *tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File h = NULL;
    mpe_FileInfo info;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_stat Enter...\n");

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "----- %s: Test Stats (%s:%d) ----------\n", TESTNAME, __FILE__,
            __LINE__);

    if (isItfs)
    {
        /* do some initialization for this test */
        uint32_t size = fileToReadSize;
        rc = fileOpen(fileToRead, MPE_FS_OPEN_WRITE | MPE_FS_OPEN_CAN_CREATE,
                &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, h != NULL);
        rc = fileWrite(h, &size, fileToReadBuf);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, size == fileToReadSize);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_WRITE",
                MPE_FS_ERROR_SUCCESS, rc);
    }

    // open file
    rc = fileOpen(fileToRead, MPE_FS_OPEN_READ, &h);
    CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_READ)",
            MPE_FS_ERROR_SUCCESS, rc);

    // get file size
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  get STAT:SIZE of file '%s'\n",
            fileToRead);

    rc = fileGetFStat(h, MPE_FS_STAT_SIZE, &info);
    CuAssertIntEquals_Msg(tc, "fileGetFStat - return code",
            MPE_FS_ERROR_SUCCESS, rc);
    CuAssertIntEquals_Msg(tc, "fileGetStat - size", fileToReadSize,
            (uint32_t) info.size);

    // close file
    rc = fileClose(h);
    CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);
    if (isItfs)
    {
        rc = fileDelete(fileToRead);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);
    }

    return;
}

/*
 ** File write operations should be should be supported by the following file systems: snfs, sim, hd
 */

void mpe_test_write(CuTest *tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File h = NULL;
    char buffer[BUFFERSIZE];
    uint32_t count;
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_write Enter...\n");

    if (!isBfs && !isRomfs)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "----- %s: Test Write To New File (%s:%d) ----------\n",
                TESTNAME, __FILE__, __LINE__);

        // open file for writing
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  write to file '%s'\n", fileToWrite);

        //assert 6.
        (void) fileDelete(fileToWrite);
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_MUST_CREATE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_OPEN_MUST_CREATE",
                MPE_FS_ERROR_SUCCESS, rc);

        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_MUST_CREATE",
                MPE_FS_ERROR_SUCCESS, rc);

        rc = fileOpen(fileToWrite, MPE_FS_OPEN_WRITE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_WRITE)",
                MPE_FS_ERROR_SUCCESS, rc);
        // write to file
        count = fileToWriteSize;
        rc = fileWrite(h, &count, fileWriteBuf);
        CuAssertIntEquals_Msg(tc, "fileWrite - return code",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertIntEquals_Msg(tc, "fileWrite - size", fileToWriteSize, count);

        // close file
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

        // open file for reading
        rc = fileOpen(fileToWrite, MPE_FS_OPEN_READ, &h);
        CuAssertIntEquals_Msg(tc, "fileOpenMPE_FS_OPEN_READ)",
                MPE_FS_ERROR_SUCCESS, rc);

        // read back in new file
        count = BUFFERSIZE;
        rc = fileRead(h, &count, buffer);
        CuAssertIntEquals_Msg(tc, "fileRead - return code",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertIntEquals_Msg(tc, "fileRead - size", fileToWriteSize, count);

        // close file
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose - MPE_FS_OPEN_READ",
                MPE_FS_ERROR_SUCCESS, rc);

        fileDelete(fileToWrite);
    }
    return;
}

/*
 ** Dir apis should be support by the following file systems: snfs, hd, romfs
 */

void mpe_test_dir_openclose(CuTest *tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File h = NULL;
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_dir_openclose Enter...\n");

    if (isItfs)
    {
        rc = dirCreate(dirName1);
        CuAssertIntEquals_Msg(tc, "dirCreate(dirName1)", MPE_FS_ERROR_SUCCESS,
                rc);
        rc = dirCreate(dirName4);
        CuAssertIntEquals_Msg(tc, "dirCreate(dirName4)", MPE_FS_ERROR_SUCCESS,
                rc);
    }

    if (!isBfs)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "----- %s: Test Open Directories (%s:%d) ----------\n",
                TESTNAME, __FILE__, __LINE__);

        // open 1st directory
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  open directory '%s'\n", dirName1);

        rc = dirOpen(dirName1, &h);
        CuAssertIntEquals_Msg(tc, "dirOpen()", MPE_FS_ERROR_SUCCESS, rc);

        // close 1st directory
        rc = dirClose(h);
        CuAssertIntEquals_Msg(tc, "dirClose()", MPE_FS_ERROR_SUCCESS, rc);

        // open 2nd directory
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  open directory '%s'\n", dirName4);

        rc = dirOpen(dirName4, &h);
        CuAssertIntEquals_Msg(tc, "dirOpen()", MPE_FS_ERROR_SUCCESS, rc);

        // close 2nd directory
        rc = dirClose(h);
        CuAssertIntEquals_Msg(tc, "dirClose()", MPE_FS_ERROR_SUCCESS, rc);

        // open 2nd directory w/ trailing slash
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  open directory '%s'\n",
                dirName4slash);

        rc = dirOpen(dirName4slash, &h);
        CuAssertIntEquals_Msg(tc, "dirOpen()", MPE_FS_ERROR_SUCCESS, rc);

        // close 2nd directory w/ trailing slash
        rc = dirClose(h);
        CuAssertIntEquals_Msg(tc, "dirClose()", MPE_FS_ERROR_SUCCESS, rc);

        // open bogus directory path #1
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  open bogus directory '%s'\n",
                dirDoesNotExist1);

        rc = dirOpen(dirDoesNotExist1, &h);
        CuAssert(tc, "dirOpen()", (MPE_FS_ERROR_SUCCESS != rc));

        // open bogus directory path #2
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "  open bogus directory '%s'\n",
                dirDoesNotExist2);

        rc = dirOpen(dirDoesNotExist2, &h);
        CuAssert(tc, "dirOpen()", (MPE_FS_ERROR_SUCCESS != rc));
    }

    if (isItfs)
    {
        rc = dirDelete(dirName1);
        CuAssertIntEquals_Msg(tc, "dirDelete(dirName1)", MPE_FS_ERROR_SUCCESS,
                rc);
        rc = dirDelete(dirName4);
        CuAssertIntEquals_Msg(tc, "dirDelete(dirName4)", MPE_FS_ERROR_SUCCESS,
                rc);
    }

    return;
}

/*
 ** Dir apis should be support by the following file systems: snfs, hd, romfs
 */
void mpe_test_dirread(CuTest *tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File h = NULL;
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_dirread Enter...\n");

    if (isItfs)
    {
        rc = dirCreate(dirName1);
        CuAssertIntEquals_Msg(tc, "dirCreate(dirName1)", MPE_FS_ERROR_SUCCESS,
                rc);
    }

    if (!isBfs)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                "----- %s: Test Read Directory Entries (%s:%d) ----------\n",
                TESTNAME, __FILE__, __LINE__);

        // open file for writing
        rc = dirOpen(dirName1, &h);
        CuAssertIntEquals_Msg(tc, "dirOpen()", MPE_FS_ERROR_SUCCESS, rc);

        // close file
        rc = dirClose(h);
        CuAssertIntEquals_Msg(tc, "dirClose()", MPE_FS_ERROR_SUCCESS, rc);
    }

    if (isItfs)
    {
        rc = dirDelete(dirName1);
        CuAssertIntEquals_Msg(tc, "dirDelete(dirName1)", MPE_FS_ERROR_SUCCESS,
                rc);
    }

    return;
}

#if 0 /* TODO: Update test to agree with ED implementation */
static void eventCallback(mpe_EdHandle edHandle, int eventCode)
{
    // save event code back into caller's buffer
    int *asyncEventCode = (int*)edHandle->data;
    *asyncEventCode = eventCode;

    return;
}
#endif

void mpe_test_loadunload(CuTest *tc)
{
#if 0 /* TODO: Update test to agree with ED implementation */
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    int asyncEventCode;
    mpe_File h = NULL;
    mpe_FileInfo fileInfo;
    mpe_EdEventInfo edHandleBuf =
    {   NULL,NULL,FALSE,0,eventCallback,NULL};
    mpe_EdHandle edHandle = &edHandleBuf;
    int elapsedTime;

    edHandleBuf.data = (void *)&asyncEventCode;
    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "******mpe_test_loadunload Enter...\n");

    if(isBfs)
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "----- %s: Test Load of Existing File (%s:%d) ----------\n", TESTNAME, __FILE__, __LINE__);

        // start loading existing file
        asyncEventCode = BAD_EVENTCODE;
        fileInfo.edHandle = edHandle;

        rc = fileLoad( fileToRead, edHandle, MPE_FS_CACHEMODE_CACHEORSTREAM );
        CuAssertIntEquals_Msg(tc, "fileLoad() - return code",
                MPE_FS_ERROR_SUCCESS, rc);

        // wait for a bit for the file to be loaded
        for ( elapsedTime = 0;
                elapsedTime < LOAD_SLEEP_MAX_MS;
                elapsedTime += LOAD_SLEEP_TIME_MS
        )
        {
            // did we get "loaded" yet :-)
            if (asyncEventCode != BAD_EVENTCODE)
            {
                // got an the async response, so drop out of this loop
                break;
            }

            // sleep for a bit to wait for async load response
            rc = threadSleep(LOAD_SLEEP_TIME_MS,0); // give async thread a chance to start
            CuAssertIntEquals_Msg(tc, "sleeping to wait for async load to complete",
                    MPE_FS_ERROR_SUCCESS, rc);

        }
        CuAssert(tc, "async file load took too long",
                (elapsedTime < LOAD_SLEEP_MAX_MS) );

        rc = fileOpen( fileToRead, MPE_FS_OPEN_WRITE, &h );
        CuAssertIntEquals_Msg(tc, "fileOpen(MPE_FS_OPEN_WRITE)",
                MPE_FS_ERROR_SUCCESS, rc);

        // did we successfully & asynchronously load the file
        CuAssertIntEquals_Msg(tc, "async file loaded completed successfully",
                MPE_FS_EVENT_SUCCESS, asyncEventCode );

        rc = fileGetFStat( h, MPE_FS_STAT_ISLOADED, &fileInfo );

        CuAssertIntEquals_Msg(tc, "fileGetFStat(MPE_FS_STAT_ISLOADED) - return code",
                MPE_FS_ERROR_SUCCESS, rc);

        // call the seek tests to test open/close/read/seek on this loaded file
        mpe_test_seek(tc);

        // unload the file
        rc = fileUnload( fileToRead );
        CuAssertIntEquals_Msg(tc, "fileUnload() - return code",
                MPE_FS_ERROR_SUCCESS, rc);

        // RomFS, for example, "fakes" an async file load by simply checking to see if the file 
        // is already within its file-system, and it's file unload doesn't do anything.  
        // Similarly, ISLOADED will always return True for valid files, so don't do the 
        // unload check here for RomFS. 

        // insure the file has been unloaded
        rc = fileGetFStat( h, MPE_FS_STAT_ISLOADED, &fileInfo );
        CuAssertIntEquals_Msg(tc, "fileGetFStat(MPE_FS_STAT_ISLOADED) - return code",
                MPE_FS_ERROR_INVALID_PARAMETER, rc);
    }
#endif
    return;
}

void test_mpe_recurseDir(CuTest * tc)
{
    mpe_FileError rc;

    if (isItfs)
    {
        mpe_File h;
        rc = dirCreate(dirName1);
        CuAssertIntEquals_Msg(tc, "dirCreate", MPE_FS_ERROR_SUCCESS, rc);

        rc = fileOpen(dirFile1, MPE_FS_OPEN_CAN_CREATE, &h);
        CuAssertIntEquals_Msg(tc, "fileOpen - MPE_FS_CAN_CREATE",
                MPE_FS_ERROR_SUCCESS, rc);
        CuAssertTrue(tc, h != NULL);
        rc = fileClose(h);
        CuAssertIntEquals_Msg(tc, "fileClose", MPE_FS_ERROR_SUCCESS, rc);

        rc = dirCreate(dirName1Dir2);
        CuAssertIntEquals_Msg(tc, "dirCreate", MPE_FS_ERROR_SUCCESS, rc);
    }

    if (!isBfs)
    {
        recurseDir(dirName1, tc);
    }

    if (isItfs)
    {
        rc = dirDelete(dirName1Dir2);
        CuAssertIntEquals_Msg(tc, "dirDelete", MPE_FS_ERROR_SUCCESS, rc);

        rc = fileDelete(dirFile1);
        CuAssertIntEquals_Msg(tc, "fileDelete", MPE_FS_ERROR_SUCCESS, rc);

        rc = dirDelete(dirName1);
        CuAssertIntEquals_Msg(tc, "dirDelete", MPE_FS_ERROR_SUCCESS, rc);
    }

    return;
}

/**
 * test_mpe_fileChange **
 * Run this test to verify Bugzilla bug #605
 */
void test_mpe_fileChange(CuTest *tc)
{
    //mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    //int asyncEventCode;
    //mpe_EdEventInfo edHandleBuf = {0};
    //mpe_EdHandle edHandle = &edHandleBuf; 
    //mpe_FileInfo fileInfo; 

    //mpe_EventQueue
    //TRACE(MPE_LOG_INFO, MPE_MOD_TEST,   "----- %s: Test Watching for Version Change (%s:%d) ----------\n",
    //       TESTNAME, __FILE__, __LINE__ );

    /* start loading existing file */
    //asyncEventCode = BAD_EVENTCODE;
    //edHandle->jSourceObject = &asyncEventCode; /* HACK FOR TESTING */
    // freds: This test needs to be updated to fix this hack to reflect
    // updates to the event dispatching mechanism.
    // After discussing with KeithM, I was advised that this test
    // is not being maintained, and that I should remove this test from the build.
    // Instead, I will comment out this functionality and leave the other tests in 
    // place.

    //edHandle->SendEvent = fileChangeEventCallback; 
    //fileInfo.edHandle = edHandle; 
    //rc = fileSetStat( fileChangeName, MPE_FS_STAT_OBJCHANGE_ON, &fileInfo );
    //CuAssertIntEquals_Msg(tc, "fileSetStat() - return code",
    //                     MPE_FS_ERROR_SUCCESS, rc);

    /* wait for a bit for the file to be loaded */
    //#ifdef FILE_CHANGE_WAIT_EVENT
    //fileChangeWaitEvent(tc, &asyncEventCode, CHANGE_EVENT_MAX_MS);
    //#endif // FILE_CHANGE_WAIT_EVENT

    /* did we successfully & asynchronously load the file */
    //CuAssert(tc, "async file loaded completed successfully",
    //                      asyncEventCode != BAD_EVENTCODE );

    /* Shut down the watcher */
    //rc = fileSetStat( fileChangeName, MPE_FS_STAT_OBJCHANGE_OFF, &fileInfo );

    return;
} /* end test_mpe_fileChange(CuTest*) */

void test_mpe_odn_bfs(CuTest* tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File hFile = NULL;
    char buffer[BUFFERSIZE];
    char* asODN[29];
    uint32_t count;
    uint32_t size;
    int step;

    asODN[0] = "/bfs/MYRA_IB_ROOT/channels";
    asODN[1] = "/bfs/MYRA_IB_ROOT/svclonative";
    asODN[2] = "/bfs/MYRA_IB_ROOT/svchinative";
    asODN[3] = "/bfs/MYRA_IB_ROOT/brandlonative";
    asODN[4] = "/bfs/MYRA_IB_ROOT/brandhinative";
    asODN[5] = "/bfs/MYRA_IB_ROOT/svcloocap";
    asODN[6] = "/bfs/MYRA_IB_ROOT/svchiocap";
    asODN[7] = "/bfs/MYRA_IB_ROOT/brandloocap";
    asODN[8] = "/bfs/MYRA_IB_ROOT/brandhiocap";
    asODN[9] = "/bfs/MYRA_IB_ROOT/profile2";
    asODN[10] = "/bfs/MYRA_IB_ROOT/apps";
    asODN[11] = "/bfs/MYRA_IB_ROOT/svcs";
    asODN[12] = "/bfs/MYRA_IB_GUIDE0/20041014_ppv.grp.z";
    asODN[13] = "/bfs/MYRA_IB_GUIDE1/20041012_ppv.grp.z";
    asODN[14] = "/bfs/MYRA_IB_GUIDE2/20041013_ppv.grp.z";
    asODN[15] = "/bfs/MYRA_OOB_SVR/channels";
    asODN[16] = "/bfs/MYRA_OOB_SVR/svclonative";
    asODN[17] = "/bfs/MYRA_OOB_SVR/svchinative";
    asODN[18] = "/bfs/MYRA_OOB_SVR/brandlonative";
    asODN[19] = "/bfs/MYRA_OOB_SVR/brandhinative";
    asODN[20] = "/bfs/MYRA_OOB_SVR/svcloocap";
    asODN[21] = "/bfs/MYRA_OOB_SVR/svchiocap";
    asODN[22] = "/bfs/MYRA_OOB_SVR/brandloocap";
    asODN[23] = "/bfs/MYRA_OOB_SVR/brandhiocap";
    asODN[24] = "/bfs/MYRA_OOB_SVR/profile2";
    asODN[25] = "/bfs/MYRA_OOB_SVR/apps";
    asODN[26] = "/bfs/MYRA_OOB_SVR/svcs";
    asODN[27] = "/bfs/MYRA_OOB_SVR/20041012_ppv.grp.z";
    asODN[28] = "/bfs/MYRA_OOB_SVR/20041013_ppv.grp.z";

    for (step = 0; step < 29; ++step)
    {
        size = 0;
        rc = fileOpen(asODN[step], MPE_FS_OPEN_READ, &hFile);
        CuAssertIntEquals_Msg(tc, "fileOpen - return code",
                MPE_FS_ERROR_SUCCESS, rc);

        do
        {
            count = BUFFERSIZE;
            rc = fileRead(hFile, &count, buffer);
            CuAssertIntEquals_Msg(tc, "fileRead - return code",
                    MPE_FS_ERROR_SUCCESS, rc);

            size += count;
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                    "fileContent -- %s bufsize[%d]\n", asODN[step], count);

#ifdef PRINT_BUFFER
            printBuffer( buffer, count );
#endif // PRINT_BUFFER
        } while (count == BUFFERSIZE);

        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "fileSize -- %s size[%d]\n",
                asODN[step], size);

        rc = fileClose(hFile);
        CuAssertIntEquals_Msg(tc, "fileClose - return code",
                MPE_FS_ERROR_SUCCESS, rc);
    } /* end for(step) */
} /* end test_mpe_odn_bfs(CuTest*) */

/**
 * printBuffer will print the content of the buffer to stdout.  This is usefull
 * when the buffer content is binary...TeraTerm freaks out with bin data.
 */

#ifdef PRINT_BUFFER

static void printBuffer( char* buffer, int buffSize )
{
    int count;
    for( count=0; count<buffSize; count+=20 )
    {
        if( (count + 20) < buffSize )
        {
            TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "<%x> <%x> <%x> <%x> <%x> "
                    "<%x> <%x> <%x> <%x> <%x> "
                    "<%x> <%x> <%x> <%x> <%x> "
                    "<%x> <%x> <%x> <%x> <%x> ",
                    buffer[count],buffer[count+1],buffer[count+2],
                    buffer[count+3],buffer[count+4],buffer[count+5],
                    buffer[count+6],buffer[count+7],buffer[count+8],
                    buffer[count+9],
                    buffer[count+10],buffer[count+11],buffer[count+12],
                    buffer[count+13],buffer[count+14],buffer[count+15],
                    buffer[count+16],buffer[count+17],buffer[count+18],
                    buffer[count+19] );
        }
        else
        {
            while(count<buffSize) TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "<%x> ", buffer[count++] );
        }
    }
}

#endif // PRINT_BUFFER

/*static void fileChangeEventCallback(mpe_EdHandle edHandle, int eventCode)
 {
 int *asyncEventCode;

 TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Callback called :----\n");
 // Save event code back into caller's buffer 
 //asyncEventCode = (int*)edHandle->jSourceObject;
 *asyncEventCode = eventCode; 

 TRACE(MPE_LOG_INFO, MPE_MOD_TEST,   "EventCallback called: %08x %08x\n",
 asyncEventCode,
 *asyncEventCode);
 }*/

#ifdef FILE_CHANGE_WAIT_EVENT
static void fileChangeWaitEvent(CuTest *tc, int *eventCode, int maxTime)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    int elapsedTime = 0;

    for( elapsedTime = 0;
            elapsedTime < maxTime;
            elapsedTime += LOAD_SLEEP_TIME_MS )
    {
        TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "-");

        /* did we get "done" yet :-)
         */
        if( *eventCode != BAD_EVENTCODE )
        {
            break;
        }

        /* Sleep for a bit to wait for async load response
         */
        rc = mpe_threadSleep(LOAD_SLEEP_TIME_MS,0);
        CuAssertIntEquals_Msg( tc,
                "sleeping to wait for event",
                MPE_FS_ERROR_SUCCESS,
                rc );
    } /* end for */
}
#endif // FILE_CHANGE_WAIT_EVENT

void recurseDir(char * parent, CuTest *tc)
{
    mpe_Error rc = MPE_FS_ERROR_SUCCESS;
    mpe_File h = NULL;
    char dest[MAX_URL_SIZE] =
    { 0 };
    mpe_DirEntry dirEnt;

    TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
            "----- Reading directory %s ----------\n", parent);

    // open file for writing
    rc = dirOpen(parent, &h);
    CuAssertIntEquals_Msg(tc, "dirOpen()", MPE_FS_ERROR_SUCCESS, rc);

    if (MPE_FS_ERROR_SUCCESS == rc)
    {
        do
        {
            rc = dirRead(h, &dirEnt);
            if (rc == MPE_FS_ERROR_SUCCESS)
            {
                if (dirEnt.isDir)
                {
                    if ((strcmp(dirEnt.name, ".") == 0) || (strcmp(dirEnt.name,
                            "..") == 0))
                    {
                        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                                "Entry is a directory: %s\n", dirEnt.name);
                    }
                    else
                    {
                        getpath(dest, parent, dirEnt.name);
                        TRACE(MPE_LOG_INFO, MPE_MOD_TEST,
                                "Entry is a directory: %s\n", dest);
                        recurseDir(dest, tc);
                    }
                }
                else
                {
                    getpath(dest, parent, dirEnt.name);
                    TRACE(MPE_LOG_INFO, MPE_MOD_TEST, "Entry is a file: %s\n",
                            dirEnt.name);
                }
            }
        } while (rc == MPE_FS_ERROR_SUCCESS);
    }

    // close file
    rc = dirClose(h);
    CuAssertIntEquals_Msg(tc, "dirClose()", MPE_FS_ERROR_SUCCESS, rc);
}

void getpath(char * dest, char * parent, char * name)
{
    dest[0] = '\0';
    strcat(dest, parent);

    if (strcmp(&(parent[(strlen(parent) - 1)]), "/") != 0)
    {
        strcat(dest, "/");
    }
    strcat(dest, name);
}
