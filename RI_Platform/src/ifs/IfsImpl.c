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

#define _IFS_IMPL_C "$Rev: 141 $"

#define FLUSH_ALL_WRITES
//#define DEBUG_SEEKS
//#define DEBUG_CONVERT_APPEND

#ifdef WIN32
#define makedir(p) mkdir(p)
#else
#define makedir(p) mkdir(p,0777)
#include <unistd.h>
#endif

#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <dirent.h>
#include <time.h>
#include <errno.h>
#include <glib.h>
#ifdef STAND_ALONE
#include "my_ri_log.h"
#else
#include "ri_log.h"
#include "ri_config.h"
#endif
#include "IfsParse.h"

log4c_category_t * ifs_RILogCategory = NULL;
#define RILOG_CATEGORY ifs_RILogCategory

#define DEFAULT_IFS_CHUNK_SIZE "600"

static IfsIndexDumpMode indexDumpMode = IfsIndexDumpModeOff;

static IfsIndex whatAll = 0;
static unsigned indexCase = 0;
static unsigned ifsFileChunkSize = 600;

// Maintain a list of active writers
//
#define MAX_WRITER_LIST_CNT 128
static IfsHandle writerListHandles[MAX_WRITER_LIST_CNT];
static unsigned writerListCnt = 0;
static GStaticMutex writerListMutex = G_STATIC_MUTEX_INIT;

static void writerListAdd(IfsHandle pIfsHandle);
static void writerListRemove(IfsHandle pIfsHandle);


// Utility operations:

IfsIndex IfsGetWhatAll(void)
{
    return whatAll;
}

const char * IfsReturnCodeToString(const IfsReturnCode ifsReturnCode)
{
    switch (ifsReturnCode)
    {
    case IfsReturnCodeNoErrorReported:
        return "No Error Reported";
    case IfsReturnCodeBadInputParameter:
        return "Bad Input Parameter";
    case IfsReturnCodeBadMaxSizeValue:
        return "Bad MaxSize Value";
    case IfsReturnCodeMemAllocationError:
        return "Mem Allocation Error";
    case IfsReturnCodeIllegalOperation:
        return "Illegal Operation";
    case IfsReturnCodeMustBeAnIfsWriter:
        return "Must be an IFS writer";
    case IfsReturnCodeSprintfError:
        return "Sprintf Error";
    case IfsReturnCodeStatError:
        return "Stat Error";
    case IfsReturnCodeMakeDirectoryError:
        return "Make Directory Error";
    case IfsReturnCodeOpenDirectoryError:
        return "Open Directory Error";
    case IfsReturnCodeCloseDirectoryError:
        return "Close Directory Error";
    case IfsReturnCodeDeleteDirectoryError:
        return "Delete Directory Error";
    case IfsReturnCodeFileOpeningError:
        return "File Opening Error";
    case IfsReturnCodeFileSeekingError:
        return "File Seeking Error";
    case IfsReturnCodeFileWritingError:
        return "File Writing Error";
    case IfsReturnCodeFileReadingError:
        return "File Reading Error";
    case IfsReturnCodeFileFlushingError:
        return "File Flushing Error";
    case IfsReturnCodeFileClosingError:
        return "File Closing Error";
    case IfsReturnCodeFileDeletingError:
        return "File Deleting Error";
    case IfsReturnCodeFileWasNotFound:
        return "File Was Not Found";
    case IfsReturnCodeReadPastEndOfFile:
        return "Read Past End Of File";
    case IfsReturnCodeSeekOutsideFile:
        return "Seek Outside File";
    case IfsReturnCodeIframeNotFound:
        return "I Frame Not Found";
    case IfsReturnCodeFoundPbeforeIframe:
        return "Found P Before I Frame";
    case IfsReturnCodeFoundBbeforeIframe:
        return "Found B Before I Frame";
    case IfsReturnCodeIframeStartNotFound:
        return "I Frame Start Not Found";
    case IfsReturnCodeIframeEndNotFound:
        return "I Frame End Not Found";
    }
    return "unknown";
}

void IfsInit(void)
{
#ifdef STAND_ALONE
    ifs_RILogCategory = log4c_category_get("RI.IFS");
    ifsFileChunkSize = atoi(DEFAULT_IFS_CHUNK_SIZE);
#else
    const char * chunk;

    ifs_RILogCategory = log4c_category_get("RI.IFS");

    if ((chunk = ricfg_getValue("RIPlatform", "RI.Platform.dvr.IfsChunkSize"))
            == NULL)
    {
        RILOG_WARN(
                "RI.Platform.dvr.IfsChunkSize not specified in the platform config file!\n");
        chunk = DEFAULT_IFS_CHUNK_SIZE;
    }
    ifsFileChunkSize = atoi(chunk);

    int i = 0;
    for (i = 0; i < MAX_WRITER_LIST_CNT; i++)
    {
        writerListHandles[i] = NULL;
    }
#endif
}

char * IfsToSecs(const IfsClock ifsClock, char * const temp) // temp[] must be at least 23 characters, returns temp
{
    // Largest number is -18446744073.709551615 = 22 characters

    if ((llong) ifsClock < 0)
    {
        long secs = -((llong) ifsClock / (llong) NSEC_PER_SEC);
        long nsec = -((llong) ifsClock % (llong) NSEC_PER_SEC);

        if (secs)
        {
            if (nsec)
                sprintf(temp, "%5ld.%09ld", -secs, nsec);
            else
                sprintf(temp, "%5ld", -secs);
        }
        else
        {
            if (nsec)
                sprintf(temp, "   -0.%09ld", nsec);
            else
                sprintf(temp, "   -0");
        }
    }
    else
    {
        long secs = ifsClock / NSEC_PER_SEC;
        long nsec = ifsClock % NSEC_PER_SEC;

        if (nsec)
            sprintf(temp, "%5ld.%09ld", secs, nsec);
        else
            sprintf(temp, "%5ld", secs);
    }

    return temp;
}

char * IfsLongLongToString(ullong value, char * const temp) // temp[] must be at least 27 characters, returns temp
{
    // Largest number is 18,446,744,073,709,551,615 = 26 characters
    // Largest divide is  1,000,000,000,000,000,000

    IfsBoolean zeros = IfsFalse;
    int where = 0;

#define TestAndPrint(div)                     \
    if (value >= div)                             \
    {                                             \
        where += sprintf(&temp[where],            \
                         zeros ? "%03d," : "%d,", \
                         (short)(value/div));     \
        value %= div;                             \
        zeros = IfsTrue;                          \
    }                                             \
    else if (zeros)                               \
        where += sprintf(&temp[where], "000,");

    TestAndPrint(1000000000000000000LL)
    TestAndPrint(1000000000000000LL )
    TestAndPrint(1000000000000LL )
    TestAndPrint(1000000000LL )
    TestAndPrint(1000000LL )
    TestAndPrint(1000LL )

#undef TestAndPrint

    sprintf(&temp[where], zeros ? "%03d" : "%d", (short) value);

    return temp;
}

void IfsDumpInfo(const IfsInfo * const pIfsInfo)
{
    char temp[32];

    RILOG_INFO("pIfsInfo->path     %s\n", pIfsInfo->path);
    RILOG_INFO("pIfsInfo->name     %s\n", pIfsInfo->name);
    RILOG_INFO("pIfsInfo->mpegSize %s\n", IfsLongLongToString(
            pIfsInfo->mpegSize, temp));
    RILOG_INFO("pIfsInfo->ndexSize %s\n", IfsLongLongToString(
            pIfsInfo->ndexSize, temp));
    RILOG_INFO("pIfsInfo->begClock %s\n", IfsToSecs(pIfsInfo->begClock, temp));
    RILOG_INFO("pIfsInfo->endClock %s\n", IfsToSecs(pIfsInfo->endClock, temp));
    RILOG_INFO("pIfsInfo->videoPid %d\n", pIfsInfo->videoPid);
    RILOG_INFO("pIfsInfo->audioPid %d\n", pIfsInfo->audioPid);
    RILOG_INFO("pIfsInfo->maxSize  %ld\n", pIfsInfo->maxSize);
}

void IfsDumpHandle(const IfsHandle ifsHandle)
{
    char temp[256]; // Used by ParseWhat

    g_static_mutex_lock(&(ifsHandle->mutex));
    RILOG_INFO("ifsHandle->path             %s\n", ifsHandle->path); // char *
    RILOG_INFO("ifsHandle->name             %s\n", ifsHandle->name); // char *
    RILOG_INFO("ifsHandle->mpegSize         %s\n", IfsLongLongToString //
            (ifsHandle->mpegSize, temp)); // NumBytes
    RILOG_INFO("ifsHandle->ndexSize         %s\n", IfsLongLongToString //
            (ifsHandle->ndexSize, temp)); // NumBytes
    RILOG_INFO("ifsHandle->both             %s\n", ifsHandle->both); // char *
    RILOG_INFO("ifsHandle->mpeg             %s\n", ifsHandle->mpeg); // char *
    RILOG_INFO("ifsHandle->ndex             %s\n", ifsHandle->ndex); // char *
    RILOG_INFO("ifsHandle->begFileNumber    %ld\n", ifsHandle->begFileNumber); // FileNumber
    RILOG_INFO("ifsHandle->endFileNumber    %ld\n", ifsHandle->endFileNumber); // FileNumber
    RILOG_INFO("ifsHandle->pMpeg            %p\n", ifsHandle->pMpeg); // FILE *
    RILOG_INFO("ifsHandle->pNdex            %p\n", ifsHandle->pNdex); // FILE *
    RILOG_INFO("ifsHandle->realLoc          %ld\n", ifsHandle->realLoc); // NumPackets
    RILOG_INFO("ifsHandle->virtLoc          %ld\n", ifsHandle->virtLoc); // NumPackets
    RILOG_INFO("ifsHandle->begClock         %s\n", IfsToSecs //
            (ifsHandle->begClock, temp)); // IfsClock
    RILOG_INFO("ifsHandle->endClock         %s\n", IfsToSecs //
            (ifsHandle->endClock, temp)); // IfsClock
    RILOG_INFO("ifsHandle->nxtClock         %s\n", IfsToSecs //
            (ifsHandle->nxtClock, temp)); // IfsClock
    RILOG_INFO("ifsHandle->entry.when       %s\n", IfsToSecs //
            (ifsHandle->entry.when, temp)); // IfsClock
    RILOG_INFO("ifsHandle->entry.what       %s\n", ParseWhat(ifsHandle, temp, //
            IfsIndexDumpModeDef, //
            IfsFalse)); // IfsIndex
    RILOG_INFO("ifsHandle->entry.realWhere  %ld\n", ifsHandle->entry.realWhere); // NumPackets
    RILOG_INFO("ifsHandle->entry.virtWhere  %ld\n", ifsHandle->entry.virtWhere); // NumPackets
    RILOG_INFO("ifsHandle->ifsPcr           %s\n", IfsLongLongToString //
            (ifsHandle->ifsPcr, temp)); // IfsPcr
    RILOG_INFO("ifsHandle->ifsPts           %s\n", IfsLongLongToString //
            (ifsHandle->ifsPts, temp)); // IfsPts
    RILOG_INFO("ifsHandle->videoPid         %d\n", ifsHandle->videoPid); // IfsPid
    RILOG_INFO("ifsHandle->audioPid         %d\n", ifsHandle->audioPid); // IfsPid
    RILOG_INFO("ifsHandle->ifsState         %d\n", ifsHandle->ifsState); // IfsState
    RILOG_INFO("ifsHandle->oldEsp           %d\n", ifsHandle->oldEsp); // unsigned char
    RILOG_INFO("ifsHandle->oldSc            %d\n", ifsHandle->oldSc); // unsigned char
    RILOG_INFO("ifsHandle->oldTp            %d\n", ifsHandle->oldTp); // unsigned char
    RILOG_INFO("ifsHandle->oldCc            %d\n", ifsHandle->oldCc); // unsigned char
    RILOG_INFO("ifsHandle->maxPacket        %ld\n", ifsHandle->maxPacket); // NumPackets
    RILOG_INFO("ifsHandle->curFileNumber    %ld\n", ifsHandle->curFileNumber); // FileNumber
    RILOG_INFO("ifsHandle->entryNum         %ld\n", ifsHandle->entryNum); // NumEntries
    RILOG_INFO("ifsHandle->maxEntry         %ld\n", ifsHandle->maxEntry); // NumEntries
    RILOG_INFO("ifsHandle->appendFileNumber %ld\n", ifsHandle->appendFileNumber); // FileNumber
    RILOG_INFO("ifsHandle->appendPacketNum  %ld\n", ifsHandle->appendPacketNum); // NumPackets
    RILOG_INFO("ifsHandle->appendEntryNum   %ld\n", ifsHandle->appendEntryNum); // NumEntries
    RILOG_INFO("ifsHandle->appendIndexShift %ld\n", ifsHandle->appendIndexShift); // NumPackets
    RILOG_INFO("ifsHandle->maxSize          %ld\n", ifsHandle->maxSize); // IfsTime
    g_static_mutex_unlock(&(ifsHandle->mutex));
}

void IfsSetMode(const IfsIndexDumpMode ifsIndexDumpMode,
        const IfsIndexerSetting ifsIndexerSetting)
{
    indexDumpMode = ifsIndexDumpMode;
    SetIndexer(ifsIndexerSetting);
}

IfsReturnCode IfsFreeInfo(IfsInfo * pIfsInfo // Input
)
{
    if (pIfsInfo == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pIfsInfo == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    if (pIfsInfo->path)
    {
        g_free(pIfsInfo->path);
        pIfsInfo->path = NULL;
    }
    if (pIfsInfo->name)
    {
        g_free(pIfsInfo->name);
        pIfsInfo->name = NULL;
    }
    g_free(pIfsInfo);
    pIfsInfo = NULL;

    return IfsReturnCodeNoErrorReported;
}

// Path/Name operations:
// WARNING: it is assumed that the ifsHandle->mutex is locked outside this func.
static IfsReturnCode GenerateFileNames(IfsHandle ifsHandle,
        FileNumber fileNumber, char ** const pMpeg, char ** const pNdex)
{
    // The maximum file name value is 4294967295
    //
    // If each file is only one SECOND of recording time then this is:
    //
    //      4,294,967,295 seconds
    //         71,582,788 minutes
    //          1,193,046 hours
    //             49,710 days
    //                136 years
    //
    // of continuous recording time in the TSB.
    //
    // And more resonable values for the amount of time stored in each file
    // will provide an even larger possible recording range.  So all is well.

    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    const size_t bothSize = strlen(ifsHandle->both); // path + / + name
    const size_t fullSize = bothSize + 16; // path + / + name [ + / + <10> + . + <3> + \000 ] = +16

    if (*pMpeg)
        g_free(*pMpeg);
    *pMpeg = g_try_malloc(fullSize);

    if (*pNdex)
        g_free(*pNdex);
    *pNdex = g_try_malloc(fullSize);

    if (*pMpeg == NULL)
    {
        RILOG_CRIT(
                "IfsReturnCodeMemAllocationError: *pMpeg == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        ifsReturnCode = IfsReturnCodeMemAllocationError;
    }

    if (*pNdex == NULL)
    {
        RILOG_CRIT(
                "IfsReturnCodeMemAllocationError: *pNdex == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        ifsReturnCode = IfsReturnCodeMemAllocationError;
    }

    if (ifsReturnCode == IfsReturnCodeNoErrorReported)
    {
        memcpy(*pMpeg, ifsHandle->both, bothSize);
        memcpy(*pMpeg + bothSize, "/", 1);
        if (sprintf(*pMpeg + bothSize + 1, "%010lu", fileNumber) == 10) // max value is 4294967295
        {
            memcpy(*pNdex, *pMpeg, fullSize);
            strcat(*pMpeg, ".mpg");
            strcat(*pNdex, ".ndx");
            return IfsReturnCodeNoErrorReported;
        }

        RILOG_ERROR(
                "IfsReturnCodeSprintfError: sprintf() != 10 in line %d of %s\n",
                __LINE__, __FILE__);
        ifsReturnCode = IfsReturnCodeSprintfError;
    }

    if (*pMpeg)
    {
        g_free(*pMpeg);
        *pMpeg = NULL;
    }
    if (*pNdex)
    {
        g_free(*pNdex);
        *pNdex = NULL;
    }

    return ifsReturnCode;
}

// WARNING: it is assumed that the ifsHandle->mutex is locked outside this func.
static IfsReturnCode CloseActualFiles(IfsHandle ifsHandle)
{
    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    ifsHandle->curFileNumber = 0;

    if (ifsHandle->pMpeg)
    {
        if (fflush(ifsHandle->pMpeg))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileFlushingError: fflush(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->mpeg, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileFlushingError;
        }
        if (fclose(ifsHandle->pMpeg))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileClosingError: fclose(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->mpeg, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileClosingError;
        }
        ifsHandle->pMpeg = NULL;
    }

    if (ifsHandle->pNdex)
    {
        if (fflush(ifsHandle->pNdex))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileFlushingError: fflush(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->ndex, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileFlushingError;
        }
        if (fclose(ifsHandle->pNdex))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileClosingError: fclose(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->ndex, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileClosingError;
        }
        ifsHandle->pNdex = NULL;
    }

    return ifsReturnCode;
}

// WARNING: it is assumed that the ifsHandle->mutex is locked outside this func.
IfsReturnCode IfsOpenActualFiles(IfsHandle ifsHandle, FileNumber fileNumber,
        const char * const mode)
{
    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    (void) CloseActualFiles(ifsHandle); // Don't care about errors here

    ifsReturnCode = GenerateFileNames(ifsHandle, fileNumber, &ifsHandle->mpeg,
            &ifsHandle->ndex);
    if (ifsReturnCode == IfsReturnCodeNoErrorReported)
    {
        ifsHandle->pMpeg = fopen(ifsHandle->mpeg, mode);
        ifsHandle->pNdex = fopen(ifsHandle->ndex, mode);

        if (ifsHandle->pMpeg == NULL)
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileOpeningError: fopen(%s, \"%s\") failed (%d: %s) in line %d of %s\n",
                    ifsHandle->mpeg, mode, errno, strerror(errno), __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileOpeningError;
        }
        if (ifsHandle->pNdex == NULL)
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileOpeningError: fopen(%s, \"%s\") failed (%d: %s) in line %d of %s\n",
                    ifsHandle->ndex, mode, errno, strerror(errno), __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileOpeningError;
        }
    }

    if (ifsReturnCode != IfsReturnCodeNoErrorReported) // Clean up and report the error
    {
        (void) CloseActualFiles(ifsHandle); // Don't care about errors here
    }
    else
        ifsHandle->curFileNumber = fileNumber;

    return ifsReturnCode;
}

// WARNING: it is assumed that the ifsHandle->mutex is locked outside this func.
static IfsReturnCode IfsReadNdexEntryAt(IfsHandle ifsHandle, NumEntries entry)
{
    if (fseek(ifsHandle->pNdex, entry * sizeof(IfsIndexEntry), SEEK_SET))
    {
        RILOG_ERROR(
                "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) failed (%d) in line %d of %s\n",
                ifsHandle->ndex, entry * sizeof(IfsIndexEntry), errno,
                __LINE__, __FILE__);
        return IfsReturnCodeFileSeekingError;
    }
    if (fread(&ifsHandle->entry, 1, sizeof(IfsIndexEntry), ifsHandle->pNdex)
            != sizeof(IfsIndexEntry))
    {
        RILOG_ERROR(
                "IfsReturnCodeFileReadingError: fread(%p, 1, %d, %s) failed (%d) in line %d of %s\n",
                &ifsHandle->entry, sizeof(IfsIndexEntry), ifsHandle->ndex,
                errno, __LINE__, __FILE__);
        return IfsReturnCodeFileReadingError;
    }
    return IfsReturnCodeNoErrorReported;
}

// WARNING: it is assumed that the ifsHandle->mutex is locked outside this func.
static IfsReturnCode GetCurrentFileSizeAndCount(IfsHandle ifsHandle)
{
    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    struct stat statBuffer;
    struct dirent * pDirent;
    DIR * pDir = opendir(ifsHandle->both);
    if (pDir == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeOpenDirectoryError: opendir(%s) failed (%d) in line %d of %s\n",
                ifsHandle->both, errno, __LINE__, __FILE__);
        return IfsReturnCodeOpenDirectoryError;
    }

    ifsHandle->mpegSize = 0;
    ifsHandle->ndexSize = 0;
    ifsHandle->begFileNumber = 0xFFFFFFFF;
    ifsHandle->endFileNumber = 0x00000000;

    while ((pDirent = readdir(pDir)) != NULL)
    {
        FileNumber newFileNumber;
        char temp[1024]; // path and filename

        if (sscanf(pDirent->d_name, "%010lu.%s", &newFileNumber, temp))
        {
            IfsBoolean isMpeg = !strcmp(temp, "mpg");
            IfsBoolean isNdex = !strcmp(temp, "ndx");

            if (newFileNumber > ifsHandle->endFileNumber)
                ifsHandle->endFileNumber = newFileNumber;
            if (newFileNumber < ifsHandle->begFileNumber)
                ifsHandle->begFileNumber = newFileNumber;

            strcpy(temp, ifsHandle->both);
            strcat(temp, "/");
            strcat(temp, pDirent->d_name);

            if (stat(temp, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        temp, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }

            if (isMpeg)
                ifsHandle->mpegSize += (size_t) statBuffer.st_size;
            if (isNdex)
                ifsHandle->ndexSize += (size_t) statBuffer.st_size;
        }
    }

    if (closedir(pDir))
    {
        RILOG_ERROR(
                "IfsReturnCodeCloseDirectoryError: closedir(%s) failed (%d) in line %d of %s\n",
                ifsHandle->both, errno, __LINE__, __FILE__);
        ifsReturnCode = IfsReturnCodeCloseDirectoryError;
    }

    if (ifsHandle->begFileNumber == 0xFFFFFFFF) // did not find any files
    {
        RILOG_ERROR(
                "IfsReturnCodeFileOpeningError: did not find any files in line %d of %s\n",
                __LINE__, __FILE__);
        ifsReturnCode = IfsReturnCodeFileOpeningError;
    }

    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
    {
        ifsHandle->mpegSize = 0;
        ifsHandle->ndexSize = 0;
    }

    return ifsReturnCode;
}

// WARNING: it is assumed that the ifsHandle->mutex is locked outside this func.
static IfsReturnCode GetCurrentFileParameters(IfsHandle ifsHandle)
{
    // NOTE:  if this is a reader then ignore maxSize
    //
    // Gets the current values of all the following parameters:
    //
    //      ifsHandle->mpegSize      from GetCurrentFileSizeAndCount()
    //      ifsHandle->ndexSize      from GetCurrentFileSizeAndCount()
    //      ifsHandle->begFileNumber from GetCurrentFileSizeAndCount()
    //      ifsHandle->endFileNumber from GetCurrentFileSizeAndCount()
    //      ifsHandle->begClock
    //      ifsHandle->endClock
    //      ifsHandle->nxtClock
    //      ifsHandle->mpeg          from IfsOpenActualFiles()
    //      ifsHandle->ndex          from IfsOpenActualFiles()
    //      ifsHandle->pMpeg         from IfsOpenActualFiles()
    //      ifsHandle->pNdex         from IfsOpenActualFiles()
    //      ifsHandle->curFileNumber from IfsOpenActualFiles()

    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    struct stat statBuffer;

    // Save the runtime data before performing the necessary operations.
    FileNumber curFile = ifsHandle->curFileNumber;
    long       posMpeg = 0;
    long       posNdex = 0;
    if (curFile != 0)
    {
        posMpeg = ftell(ifsHandle->pMpeg);
        posNdex = ftell(ifsHandle->pNdex);
    }

    ifsHandle->begClock = 0;
    ifsHandle->endClock = 0;
    ifsHandle->nxtClock = 0;

    // This is needed so the size calculation will be accurate
    (void) CloseActualFiles(ifsHandle); // Don't care about errors here

    ifsReturnCode = GetCurrentFileSizeAndCount(ifsHandle);
    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
        return ifsReturnCode;

    // If we get here then ifsHandle->mpegSize, ifsHandle->ndexSize,
    // ifsHandle->begFileNumber and ifsHandle->endFileNumber are all valid

    if (ifsHandle->begFileNumber && ifsHandle->endFileNumber) // The file is circular
    {
        FileNumber fileNumber = ifsHandle->endFileNumber;

        if (!ifsHandle->isReading && !ifsHandle->maxSize) // The handle is not circular
        {
            RILOG_ERROR(
                    "IfsReturnCodeBadMaxSizeValue: ifsHandle should be circular but it is not in line %d of %s\n",
                    __LINE__, __FILE__);
            return IfsReturnCodeBadMaxSizeValue; // Should be circular but it is not
        }

        do // Process the first entry
        {
            ifsReturnCode = IfsOpenActualFiles(ifsHandle,
                    ifsHandle->begFileNumber, "rb+");
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;

            if (stat(ifsHandle->ndex, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }

            if ((size_t) statBuffer.st_size >= sizeof(IfsIndexEntry)) // has at least one entry
            {
                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, 0);
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    break;

                ifsHandle->begClock = ifsHandle->entry.when; // nanoseconds
            }
            // else this is an empty index file

        } while (0);

        do // Find the last indexing entry
        {
            ifsReturnCode = IfsOpenActualFiles(ifsHandle, fileNumber, "rb+");
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;

            if (stat(ifsHandle->ndex, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }

            if ((size_t) statBuffer.st_size >= sizeof(IfsIndexEntry)) // has at least one entry
            {
                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, 0);
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    break;

                ifsHandle->nxtClock = ifsHandle->entry.when + (ifsFileChunkSize
                        * NSEC_PER_SEC); // nanoseconds

                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle,
                        ((size_t) statBuffer.st_size / sizeof(IfsIndexEntry)
                                - 1));
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    break;

                ifsHandle->endClock = ifsHandle->entry.when; // nanoseconds

                break; // DONE with no error
            }
            // else this is an empty index file, try the previous file
        } while (--fileNumber);

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            return ifsReturnCode;

        // If we get here then ifsHandle->mpegSize, ifsHandle->ndexSize,
        // ifsHandle->begFileNumber, ifsHandle->endFileNumber,
        // ifsHandle->nxtClock and ifsHandle->endClock are all valid
    }
    else if (!ifsHandle->begFileNumber && !ifsHandle->endFileNumber) // The file is not circular
    {
        if (!ifsHandle->isReading && ifsHandle->maxSize) // The handle is circular
        {
            RILOG_ERROR(
                    "IfsReturnCodeBadMaxSizeValue: ifsHandle should not be circular but it is in line %d of %s\n",
                    __LINE__, __FILE__);
            return IfsReturnCodeBadMaxSizeValue; // Should be one file but it is not
        }

        do
        {
            ifsReturnCode = IfsOpenActualFiles(ifsHandle,
                    ifsHandle->begFileNumber, "rb+");
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;

            if (stat(ifsHandle->ndex, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }

            if ((size_t) statBuffer.st_size >= sizeof(IfsIndexEntry)) // has at least one entry
            {
                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, 0);
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    break;

                ifsHandle->begClock = ifsHandle->entry.when; // nanoseconds

                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle,
                        ((size_t) statBuffer.st_size / sizeof(IfsIndexEntry)
                                - 1));
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    break;

                ifsHandle->endClock = ifsHandle->entry.when; // nanoseconds
            }

        } while (0);
    }
    else
    {
        RILOG_ERROR(
                "IfsReturnCodeBadMaxSizeValue: ifsHandle is corrupted in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    // Restore previously saved runtime parameters.
    if (curFile != 0)
    {
        ifsReturnCode = IfsOpenActualFiles(ifsHandle, curFile, "rb+");
        if (ifsReturnCode == IfsReturnCodeNoErrorReported)
        {
            if (fseek(ifsHandle->pMpeg, posMpeg, SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) failed (%d) in line %d of %s\n",
                        ifsHandle->mpeg, posMpeg, errno, __LINE__, __FILE__);
                return IfsReturnCodeFileSeekingError;
            }
            if (fseek(ifsHandle->pNdex, posNdex, SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, posNdex, errno, __LINE__, __FILE__);
                return IfsReturnCodeFileSeekingError;
            }
        }
    }

    return ifsReturnCode;
}

static IfsReturnCode IfsOpenImpl(IfsBoolean isReading, // Input  (if true then path+name must exist and maxSize is ignored)
        const char * path, // Input
        const char * name, // Input  (if writing and NULL the name is generated)
        IfsTime maxSize, // Input  (in seconds, 0 = no max, ignored if reading)
        IfsHandle * pIfsHandle // Output (use IfsClose() to g_free)
)
{
    // The various open cases for a writer are:
    //
    //  case  char*name  maxSize  Found   Description
    //  ----  ---------  -------  ------  ------------
    //
    //     1       NULL        0      NA  Generate directory and create the single 0000000000 file
    //
    //     2       NULL    not 0      NA  Generate directory and create the 0000000001 circular file
    //
    //     3   not NULL        0      No  Create the specified directory and single 0000000000 file
    //
    //     4   not NULL    not 0      No  Create the specified directory and the 0000000001 circular file
    //
    //     5   not NULL        0     Yes  If 0000000000 file found open it otherwise report an error
    //
    //     6   not NULL    not 0     Yes  If 0000000000 file found error, otherwise open lowest file name
    //
    // The various open cases for a reader are:
    //
    //  case  char*name  Found   Description
    //  ----  ---------  ------  ------------
    //
    //     7       NULL      NA  Report IfsReturnCodeBadInputParameter
    //
    //     8   not NULL      No  Report IfsReturnCodeFileWasNotFound
    //
    //     9   not NULL     Yes  Open the file for reading

    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported; // used to report errors
    IfsHandle ifsHandle; // IfsHandle being created/opened
    char temp[256]; // filename only  // temporary string storage

    // Check the input parameters, initialize the output parameters and report any errors

    if (pIfsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pIfsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    else
        *pIfsHandle = NULL;

    if (path == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: path == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    // check for NULL or empty string name
    if ((name == NULL) || ('\0' == name[0]))
    {
        if (isReading) // Case 7 - report error
        {
            RILOG_ERROR(
                    "IfsReturnCodeBadInputParameter: Reader with name == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            return IfsReturnCodeBadInputParameter;
        }
        else // Cases 1 and 2 - generate a directory name
        {
            // Generated a direcory name of the form XXXXXXXX_xxxx
            // where XXXXXXXX is the hex representation of the epoch seconds
            // and xxxx is a unique number
            static unsigned short uniqueNum = 0u;
            static GStaticMutex uniqueLock = G_STATIC_MUTEX_INIT;

            unsigned short localUniqueNum = 0u;
            g_static_mutex_lock(&uniqueLock);
            localUniqueNum = uniqueNum++;
            g_static_mutex_unlock(&uniqueLock);

            if (sprintf(temp, "%08lX_%04X", time(NULL), localUniqueNum)
                    != 13)
            {
                RILOG_ERROR(
                        "IfsReturnCodeSprintfError: sprintf() != 13 in line %d of %s\n",
                        __LINE__, __FILE__);
                return IfsReturnCodeSprintfError;
            }

            name = temp;
        }
    }

    // Start the process by allocating memory for the IfsHandle

    ifsHandle = g_try_malloc0(sizeof(IfsHandleImpl));
    if (ifsHandle == NULL)
    {
        RILOG_CRIT(
                "IfsReturnCodeMemAllocationError: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeMemAllocationError;
    }

    // Initialize all the pointers in the IfsHandle
    g_static_mutex_init(&(ifsHandle->mutex));

    // (done by g_try_malloc0) ifsHandle->path  = NULL; // current path
    // (done by g_try_malloc0) ifsHandle->name  = NULL; // current name
    // (done by g_try_malloc0) ifsHandle->both  = NULL; // current path + name
    // (done by g_try_malloc0) ifsHandle->mpeg  = NULL; // current path + name + filename.mpg
    // (done by g_try_malloc0) ifsHandle->ndex  = NULL; // current path + name + filename.ndx
    // (done by g_try_malloc0) ifsHandle->pMpeg = NULL; // current MPEG file
    // (done by g_try_malloc0) ifsHandle->pNdex = NULL; // current NDEX file

    // Now fill in all the values of the IfsHandle

    do
    {
        struct stat statBuffer;

        // Process the input parameters

        const size_t pathSize = strlen(path) + 1; // path + \000
        const size_t nameSize = strlen(name) + 1; // name + \000
        const size_t bothSize = pathSize + nameSize; // path + / + name + \000

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        ifsHandle->numEmptyFreads = 0;
        ifsHandle->isReading = isReading;
        ifsHandle->maxSize = maxSize; // in seconds, 0 = value not used

        ifsHandle->path = g_try_malloc(pathSize);
        ifsHandle->name = g_try_malloc(nameSize);
        ifsHandle->both = g_try_malloc(bothSize);

        if (ifsHandle->path == NULL)
        {
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: ifsHandle->path == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeMemAllocationError;
        }
        if (ifsHandle->name == NULL)
        {
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: ifsHandle->name == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeMemAllocationError;
        }
        if (ifsHandle->both == NULL)
        {
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: ifsHandle->both == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeMemAllocationError;
        }
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        memcpy(ifsHandle->path, path, pathSize);
        memcpy(ifsHandle->name, name, nameSize);
        memcpy(ifsHandle->both, path, pathSize);
        strcat(ifsHandle->both, "/");
        strcat(ifsHandle->both, name);

        if (stat(ifsHandle->both, &statBuffer)) // The directory was NOT found
        {
            if (isReading) // Case 8 - report error
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileWasNotFound: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->both, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileWasNotFound;
                break;
            }

            // Cases 1 through 4 - make the specified (or generated) directory

            if (makedir(ifsHandle->both))
            {
                RILOG_ERROR(
                        "IfsReturnCodeMakeDirectoryError: makedir(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->both, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeMakeDirectoryError;
                break;
            }

            // (done by g_try_malloc0) ifsHandle->mpegSize = 0;
            // (done by g_try_malloc0) ifsHandle->ndexSize = 0;

            ifsHandle->begFileNumber = ifsHandle->endFileNumber = maxSize ? 1
                    : 0;

            // (done by g_try_malloc0) ifsHandle->begClock =
            // (done by g_try_malloc0) ifsHandle->endClock =
            // (done by g_try_malloc0) ifsHandle->nxtClock = 0; // nanoseconds

            ifsReturnCode = IfsOpenActualFiles(ifsHandle,
                    ifsHandle->begFileNumber, "wb+");
        }
        else // The directory was found
        {
            // Cases 5, 6 and 9 - open the existing IFS file

            ifsReturnCode = GetCurrentFileParameters(ifsHandle);
        }

        // (done by g_try_malloc0) ifsHandle->realLoc = 0; // offset in packets
        // (done by g_try_malloc0) ifsHandle->virtLoc = 0; // offset in packets

        ifsHandle->videoPid = ifsHandle->audioPid = IFS_UNDEFINED_PID;

        ifsHandle->ifsState = IfsStateInitial;

        ifsHandle->oldEsp = ifsHandle->oldSc = ifsHandle->oldTp
                = ifsHandle->oldCc = IFS_UNDEFINED_BYTE;

    } while (0);

    if (ifsReturnCode == IfsReturnCodeNoErrorReported)
    {
        *pIfsHandle = ifsHandle;

        if ((indexDumpMode == IfsIndexDumpModeAll) && whatAll)
        {
#ifdef DEBUG_ALL_PES_CODES
            RILOG_INFO("----  ---------------- = -------- --------------- ------------\n");
            RILOG_INFO("      %08lX%08lX\n", (unsigned long)(whatAll>>32), (unsigned long)whatAll);
#else
            RILOG_INFO(
                    "----  -------- = -------- --------------- ------------\n");
            RILOG_INFO("      %08X\n", whatAll);
#endif
        }
    }
    else
    {
        (void) IfsClose(ifsHandle); // Ignore any errors, we already have one...
    }

    return ifsReturnCode;
}

IfsReturnCode IfsOpenWriter(const char * path, // Input
        const char * name, // Input  (if NULL the name is generated)
        IfsTime maxSize, // Input  (in seconds, 0 = no max)
        IfsHandle * pIfsHandle // Output (use IfsClose() to g_free)
)
{
    IfsReturnCode ifsReturnCode =
        IfsOpenImpl(IfsFalse, // Input  (if true then path+name must exist and maxSize is ignored)
            path, // Input
            name, // Input  (if writing and NULL the name is generated)
            maxSize, // Input  (in seconds, 0 = no max, ignored if reading)
            pIfsHandle); // Output (use IfsClose() to g_free)

    // If writer was successfully opened, add to writer list
    if (ifsReturnCode == IfsReturnCodeNoErrorReported)
    {
        writerListAdd(*pIfsHandle);
    }

    return ifsReturnCode;
}

IfsReturnCode IfsOpenReader(const char * path, // Input
        const char * name, // Input
        IfsHandle * pIfsHandle // Output (use IfsClose() to g_free)
)
{
    return IfsOpenImpl(IfsTrue, // Input  (if true then path+name must exist and maxSize is ignored)
            path, // Input
            name, // Input  (if writing and NULL the name is generated)
            0, // Input  (in seconds, 0 = no max, ignored if reading)
            pIfsHandle); // Output (use IfsClose() to g_free)
}

IfsReturnCode IfsPathNameInfo(const char * path, // Input
        const char * name, // Input
        IfsInfo ** ppIfsInfo // Output (use IfsFreeInfo() to g_free)
)
{
    IfsReturnCode ifsReturnCode;
    IfsHandle ifsHandle;

    if (ppIfsInfo == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ppIfsInfo == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    else
        *ppIfsInfo = NULL;

    if (path == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: path == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (name == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: name == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    ifsReturnCode = IfsOpenReader(path, name, &ifsHandle);
    if (ifsReturnCode == IfsReturnCodeNoErrorReported)
    {
        ifsReturnCode = IfsHandleInfo(ifsHandle, ppIfsInfo);
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
        {
            (void) IfsClose(ifsHandle); // Ignore any errors, we already have one...
        }
        else
        {
            ifsReturnCode = IfsClose(ifsHandle);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            {
                (void) IfsFreeInfo(*ppIfsInfo); // Ignore any errors, we already have one...
                *ppIfsInfo = NULL;
            }
        }
    }

    return ifsReturnCode;
}

IfsReturnCode IfsDelete(const char * path, // Input
        const char * name // Input
)
{
    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    if (path == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: path == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (name == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: name == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    {
        const size_t pathSize = strlen(path) + 1; // path + \000
        const size_t nameSize = strlen(name) + 1; // name + \000
        const size_t bothSize = pathSize + nameSize; // path + / + name + \000

        char * both = g_try_malloc(bothSize); // g_free here
        if (both == NULL)
        {
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: both == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            return IfsReturnCodeMemAllocationError;
        }

        memcpy(both, path, pathSize);
        strcat(both, "/");
        strcat(both, name);

        do
        {
            struct dirent * pDirent;
            DIR * pDir = opendir(both);
            if (pDir == NULL)
            {
                RILOG_ERROR(
                        "IfsReturnCodeOpenDirectoryError: opendir(%s) failed (%d) in line %d of %s\n",
                        both, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeOpenDirectoryError;
                break;
            }

            //          RILOG_INFO("\nDeleting files found in %s:\n", both);

            while ((pDirent = readdir(pDir)) != NULL)
            {
                FileNumber newFileNumber;
                char temp[1024]; // path and filename

                if (sscanf(pDirent->d_name, "%010lu.", &newFileNumber))
                {
                    strcpy(temp, both);
                    strcat(temp, "/");
                    strcat(temp, pDirent->d_name);

                    if (remove(temp))
                    {
                        RILOG_ERROR(
                                "IfsReturnCodeFileDeletingError: remove(%s) failed (%d) in line %d of %s\n",
                                temp, errno, __LINE__, __FILE__);
                        ifsReturnCode = IfsReturnCodeFileDeletingError;
                    }

                    //                  RILOG_INFO("   %s\n", pDirent->d_name);
                }
            }

            if (closedir(pDir))
            {
                RILOG_ERROR(
                        "IfsReturnCodeCloseDirectoryError: closedir(%s) failed (%d) in line %d of %s\n",
                        both, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeCloseDirectoryError;
            }

            if (rmdir(both))
            {
                RILOG_ERROR(
                        "IfsReturnCodeDeleteDirectoryError: rmdir(%s) failed (%d) in line %d of %s\n",
                        both, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeDeleteDirectoryError;
            }

        } while (0);

        g_free(both);
    }

    return ifsReturnCode;
}

// IfsHandle operations:

IfsReturnCode IfsStart(IfsHandle ifsHandle, // Input (must be a writer)
        IfsPid videoPid, // Input
        IfsPid audioPid // Input
)
{
    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));

    if (ifsHandle->isReading)
    {
        RILOG_ERROR("IfsReturnCodeMustBeAnIfsWriter: in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeMustBeAnIfsWriter;
    }

    ifsHandle->videoPid = videoPid;
    ifsHandle->audioPid = audioPid;

    if (indexDumpMode == IfsIndexDumpModeAll)
    {
        RILOG_INFO("\n");

#ifdef DEBUG_ALL_PES_CODES
        RILOG_INFO("Case  What             = Header   Start/Adapt     Extension   \n");
        RILOG_INFO("----  ---------------- = -------- --------------- ------------\n");
#else
        RILOG_INFO("Case  What     = Header   Start/Adapt     Extension   \n");
        RILOG_INFO("----  -------- = -------- --------------- ------------\n");
#endif
    }

    g_static_mutex_unlock(&(ifsHandle->mutex));
    return IfsReturnCodeNoErrorReported;
}

IfsReturnCode IfsSetMaxSize(IfsHandle ifsHandle, // Input (must be a writer)
        IfsTime maxSize // Input (in seconds, 0 is illegal)
)
{
    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (maxSize == 0)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: maxSize == 0 in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));

    if (ifsHandle->isReading)
    {
        RILOG_ERROR("IfsReturnCodeMustBeAnIfsWriter: in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeMustBeAnIfsWriter;
    }
    if (ifsHandle->maxSize == 0)
    {
        RILOG_ERROR(
                "IfsReturnCodeIllegalOperation: ifsHandle->maxSize == 0 in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeIllegalOperation;
    }

    ifsHandle->maxSize = maxSize; // in seconds, 0 is illegal

    g_static_mutex_unlock(&(ifsHandle->mutex));
    return IfsReturnCodeNoErrorReported;
}

IfsReturnCode IfsStop(IfsHandle ifsHandle // Input (must be a writer)
)
{
    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));

    if (ifsHandle->isReading)
    {
        RILOG_ERROR("IfsReturnCodeMustBeAnIfsWriter: in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeMustBeAnIfsWriter;
    }

    ifsHandle->ifsState = IfsStateInitial; // Reset the PES state machine

    (void) CloseActualFiles(ifsHandle); // Don't care about errors here
    g_static_mutex_unlock(&(ifsHandle->mutex));

    do
    {
        struct dirent * pDirent;
        DIR * pDir = opendir(ifsHandle->both);
        if (pDir == NULL)
        {
            RILOG_ERROR(
                    "IfsReturnCodeOpenDirectoryError: opendir(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->both, errno, __LINE__, __FILE__);
            return IfsReturnCodeOpenDirectoryError;
        }

        //      RILOG_INFO("\nDeleting files found in %s:\n", ifsHandle->both);

        while ((pDirent = readdir(pDir)) != NULL)
        {
            FileNumber newFileNumber;
            char temp[1024]; // path and filename

            if (sscanf(pDirent->d_name, "%010lu.", &newFileNumber))
            {
                strcpy(temp, ifsHandle->both);
                strcat(temp, "/");
                strcat(temp, pDirent->d_name);

                if (remove(temp))
                {
                    RILOG_ERROR(
                            "IfsReturnCodeFileDeletingError: remove(%s) failed (%d) in line %d of %s\n",
                            temp, errno, __LINE__, __FILE__);
                    ifsReturnCode = IfsReturnCodeFileDeletingError;
                }

                //              RILOG_INFO("   %s\n", pDirent->d_name);
            }
        }

        if (closedir(pDir))
        {
            RILOG_ERROR(
                    "IfsReturnCodeCloseDirectoryError: closedir(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->both, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeCloseDirectoryError;
        }
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            return ifsReturnCode;

        g_static_mutex_lock(&(ifsHandle->mutex));
        ifsHandle->numEmptyFreads = 0;
        ifsHandle->mpegSize = 0;
        ifsHandle->ndexSize = 0;

        ifsHandle->begClock = ifsHandle->endClock = ifsHandle->nxtClock = 0;

        ifsHandle->begFileNumber = ifsHandle->endFileNumber
                = ifsHandle->maxSize ? 1 : 0;

        ifsReturnCode = IfsOpenActualFiles(ifsHandle, ifsHandle->begFileNumber,
                "wb+");

        ifsHandle->realLoc = 0; // offset in packets
        ifsHandle->virtLoc = 0; // offset in packets

        ifsHandle->videoPid = ifsHandle->audioPid = IFS_UNDEFINED_PID;

        ifsHandle->ifsState = IfsStateInitial;

        ifsHandle->oldEsp = ifsHandle->oldSc = ifsHandle->oldTp
                = ifsHandle->oldCc = IFS_UNDEFINED_BYTE;
        g_static_mutex_unlock(&(ifsHandle->mutex));

    } while (0);

    return ifsReturnCode;
}

IfsReturnCode IfsHandleInfo(IfsHandle ifsHandle, // Input
        IfsInfo ** ppIfsInfo // Output (use IfsFreeInfo() to g_free)
)
{
    IfsReturnCode ifsReturnCode;
    IfsBoolean retry = IfsTrue;

    if (ppIfsInfo == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ppIfsInfo == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    else
        *ppIfsInfo = NULL;

    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));

    do
    {
        ifsReturnCode = GetCurrentFileParameters(ifsHandle);

        if (ifsReturnCode == IfsReturnCodeNoErrorReported)
        {
            break;
        }
        else if (retry == IfsFalse)
        {
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return ifsReturnCode;
        }
        else
        {
            retry = IfsFalse;
            RILOG_INFO("%s: GetCurrentFileParameters retry at %d of %s\n",
                        __FUNCTION__, __LINE__, __FILE__);
        }

    } while (retry == IfsTrue);


    {   // scope
        IfsInfo * pIfsInfo;

        const size_t pathSize = strlen(ifsHandle->path) + 1;
        const size_t nameSize = strlen(ifsHandle->name) + 1;
        const IfsClock difClock = ifsHandle->endClock - ifsHandle->begClock;

        pIfsInfo = g_try_malloc(sizeof(IfsInfo)); // g_free in IfsFreeInfo()
        if (pIfsInfo == NULL)
        {
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: pIfsInfo == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return IfsReturnCodeMemAllocationError;
        }

        pIfsInfo->maxSize = ifsHandle->maxSize; // in seconds, 0 = value not used
        pIfsInfo->path = NULL; // filled in below
        pIfsInfo->name = NULL; // filled in below
        pIfsInfo->mpegSize = ifsHandle->mpegSize;
        pIfsInfo->ndexSize = ifsHandle->ndexSize;
        pIfsInfo->begClock = (ifsHandle->maxSize && (difClock
                > ifsHandle->maxSize * NSEC_PER_SEC) ? ifsHandle->endClock
                - ifsHandle->maxSize * NSEC_PER_SEC : ifsHandle->begClock);
        pIfsInfo->endClock = ifsHandle->endClock; // in nanoseconds
        pIfsInfo->videoPid = ifsHandle->videoPid;
        pIfsInfo->audioPid = ifsHandle->audioPid;

        pIfsInfo->path = g_try_malloc(pathSize); // g_free in IfsFreeInfo()
        pIfsInfo->name = g_try_malloc(nameSize); // g_free in IfsFreeInfo()

        if (pIfsInfo->path == NULL)
        {
            (void) IfsFreeInfo(pIfsInfo); // Ignore any errors, we already have one...
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: pIfsInfo->path == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return IfsReturnCodeMemAllocationError;
        }

        if (pIfsInfo->name == NULL)
        {
            (void) IfsFreeInfo(pIfsInfo); // Ignore any errors, we already have one...
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: pIfsInfo->name == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return IfsReturnCodeMemAllocationError;
        }

        memcpy(pIfsInfo->path, ifsHandle->path, pathSize);
        memcpy(pIfsInfo->name, ifsHandle->name, nameSize);

        *ppIfsInfo = pIfsInfo;
    }

    g_static_mutex_unlock(&(ifsHandle->mutex));
    return IfsReturnCodeNoErrorReported;
}

IfsReturnCode IfsWrite(IfsHandle ifsHandle, // Input (must be a writer)
        IfsClock ifsClock, // Input, in nanoseconds
        NumPackets numPackets, // Input
        IfsPacket * pData // Input
)
{
    NumPackets i;

    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));

    if (ifsHandle->isReading)
    {
        RILOG_ERROR("IfsReturnCodeMustBeAnIfsWriter: in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeMustBeAnIfsWriter;
    }
    if (numPackets && (pData == NULL))
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: numPackets && (pData == NULL) in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeBadInputParameter;
    }

    ifsHandle->entry.when = ifsClock; // nanoseconds

    if (ifsHandle->maxSize) // in seconds, not 0 then this is a circular buffer
    {
        if (!ifsHandle->begClock) // beg not set yet (first write)
        {
            ifsHandle->begClock = ifsClock; // set beg clock in nanoseconds
            char temp[32];
            RILOG_INFO( "set begClock to %s at line %d of %s\n",
                IfsToSecs(ifsClock, temp), __LINE__, __FILE__);
            ifsHandle->nxtClock = ifsClock + (ifsFileChunkSize * NSEC_PER_SEC); // in nanoseconds
        }
        else // not first write
        {
            if (ifsClock >= ifsHandle->nxtClock) // Need to move to the next file
            {
                char tmp[32];
                RILOG_INFO("move to next file (beg:%lu, nxt:%lu) at %s"
                    "at line %d of %s\n",
                    ifsHandle->begFileNumber, ifsHandle->endFileNumber,
                    IfsToSecs(ifsClock, tmp), __LINE__, __FILE__);
                IfsReturnCode ifsReturnCode;

                char * mpeg = NULL;
                char * ndex = NULL;

                ifsHandle->nxtClock += (ifsFileChunkSize * NSEC_PER_SEC); // in nanoseconds
                ifsHandle->realLoc = 0;

                ifsHandle->endFileNumber++;

                g_static_mutex_unlock(&(ifsHandle->mutex));
                ifsReturnCode = IfsOpenActualFiles(ifsHandle,
                        ifsHandle->endFileNumber, "wb+");
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    return ifsReturnCode;

                g_static_mutex_lock(&(ifsHandle->mutex));

                ifsReturnCode = GenerateFileNames(ifsHandle,
                        ifsHandle->begFileNumber, &mpeg, &ndex);
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                {
                    g_static_mutex_unlock(&(ifsHandle->mutex));
                    return ifsReturnCode;
                }

                while (((ifsClock - ifsHandle->begClock) / NSEC_PER_SEC)
                        >= (ifsHandle->maxSize + ifsFileChunkSize))
                {
                    FILE * pNdex;
                    struct stat statBuffer;
                    IfsIndexEntry ifsIndexEntry;

                    // Delete the oldest files
                    RILOG_INFO("deleting begFileNumber:%lu at line %d of %s\n",
                        ifsHandle->begFileNumber, __LINE__, __FILE__);

                    if (stat(mpeg, &statBuffer))
                    {
                        RILOG_ERROR(
                                "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                                mpeg, errno, __LINE__, __FILE__);
                        ifsReturnCode = IfsReturnCodeStatError;
                        break;
                    }
                    //                  RILOG_INFO("Deleting %s %s bytes\n", mpeg, IfsLongLongToString((size_t)statBuffer.st_size));
                    ifsHandle->mpegSize -= (size_t) statBuffer.st_size;

                    if (stat(ndex, &statBuffer))
                    {
                        RILOG_ERROR(
                                "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                                ndex, errno, __LINE__, __FILE__);
                        ifsReturnCode = IfsReturnCodeStatError;
                        break;
                    }
                    //                  RILOG_INFO("Deleting %s %s bytes\n", ndex, IfsLongLongToString((size_t)statBuffer.st_size));
                    ifsHandle->ndexSize -= (size_t) statBuffer.st_size;

                    if (remove(mpeg))
                    {
                        RILOG_ERROR(
                                "IfsReturnCodeFileDeletingError: remove(%s) failed (%d) in line %d of %s\n",
                                mpeg, errno, __LINE__, __FILE__);
                        ifsReturnCode = IfsReturnCodeFileDeletingError;
                    }
                    if (remove(ndex))
                    {
                        RILOG_ERROR(
                                "IfsReturnCodeFileDeletingError: remove(%s) failed (%d) in line %d of %s\n",
                                ndex, errno, __LINE__, __FILE__);
                        ifsReturnCode = IfsReturnCodeFileDeletingError;
                    }
                    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                        break;

                    ifsHandle->begFileNumber++;

                    // Set the begClock to the new value

                    ifsReturnCode = GenerateFileNames(ifsHandle,
                            ifsHandle->begFileNumber, &mpeg, &ndex);
                    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                        break;

                    pNdex = fopen(ndex, "rb+"); // close here [error checked]
                    if (pNdex == NULL)
                    {
                        RILOG_ERROR(
                                "IfsReturnCodeFileOpeningError: fopen(%s, \"rb+\") failed (%d) in line %d of %s\n",
                                ndex, errno, __LINE__, __FILE__);
                        ifsReturnCode = IfsReturnCodeFileOpeningError;
                        break;
                    }

                    rewind(pNdex);

                    if (fread(&ifsIndexEntry, 1, sizeof(IfsIndexEntry), pNdex)
                            != sizeof(IfsIndexEntry))
                    {
                        RILOG_ERROR(
                                "IfsReturnCodeFileReadingError: fread(%p, 1, %d, %s) failed (%d) in line %d of %s\n",
                                &ifsIndexEntry, sizeof(IfsIndexEntry), ndex,
                                errno, __LINE__, __FILE__);
                        ifsReturnCode = IfsReturnCodeFileReadingError;
                    }
                    else
                    {
                        ifsHandle->begClock = ifsIndexEntry.when; // nanoseconds
                        char temp[32];
                        RILOG_INFO( "set begClock to %s at line %d of %s\n",
                            IfsToSecs(ifsIndexEntry.when, temp), __LINE__, __FILE__);
                    }

                    if (pNdex)
                    {
                        if (fflush(pNdex))
                        {
                            RILOG_ERROR(
                                    "IfsReturnCodeFileFlushingError: fflush(%s) failed (%d) in line %d of %s\n",
                                    ifsHandle->ndex, errno, __LINE__, __FILE__);
                            ifsReturnCode = IfsReturnCodeFileFlushingError;
                        }
                        fclose(pNdex);
                    }

                    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                        break;
                }

                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                {
                    if (mpeg)
                        g_free(mpeg);
                    if (ndex)
                        g_free(ndex);

                    g_static_mutex_unlock(&(ifsHandle->mutex));
                    return ifsReturnCode;
                }

                if (indexDumpMode == IfsIndexDumpModeDef)
                    RILOG_INFO("--------- --------- -\n");
            }
        }
        ifsHandle->endClock = ifsClock; // move end clock (in nanoseconds)
    }
    else // This is NOT a circular buffer
    {
        if (!ifsHandle->begClock) // beg not set yet
        {
            ifsHandle->begClock = ifsClock; // set beg clock in nanoseconds
        }
        ifsHandle->endClock = ifsClock; // move end clock (in nanoseconds)
    }

    if (fseek(ifsHandle->pNdex, 0, SEEK_END))
    {
        RILOG_ERROR(
                "IfsReturnCodeFileSeekingError: fseek(%s, 0, SEEK_END) failed (%d) in line %d of %s\n",
                ifsHandle->ndex, errno, __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeFileSeekingError;
    }

    for (i = 0; i < numPackets; i++)
    {
        if (IfsParsePacket(ifsHandle, pData + i)) // Set and test WHAT
        {
            char temp[256]; // ParseWhat

            ifsHandle->entry.realWhere = ifsHandle->realLoc; // Set real WHERE, offset in packets
            ifsHandle->entry.virtWhere = ifsHandle->virtLoc; // Set virtual WHERE, offset in packets

            if (indexDumpMode == IfsIndexDumpModeAll)
            {
#ifdef DEBUG_ALL_PES_CODES
                RILOG_INFO("%4d  %08lX%08lX = %s\n",
                        indexCase++,
                        (unsigned long)(ifsHandle->entry.what>>32),
                        (unsigned long)ifsHandle->entry.what,
                        ParseWhat(ifsHandle, temp, indexDumpMode, IfsTrue));
#else
                RILOG_INFO("%4d  %08X = %s\n", indexCase++,
                        ifsHandle->entry.what, ParseWhat(ifsHandle, temp,
                                indexDumpMode, IfsTrue));
#endif
                whatAll |= ifsHandle->entry.what;
            }
            if (indexDumpMode == IfsIndexDumpModeDef)
            {
                RILOG_TRACE("%9ld %9ld %s\n", ifsHandle->entry.realWhere, // offset in packets
                        ifsHandle->entry.virtWhere, // offset in packets
                        ParseWhat(ifsHandle, temp, indexDumpMode, IfsTrue));
            }

            if (fwrite(&ifsHandle->entry, 1, sizeof(IfsIndexEntry),
                    ifsHandle->pNdex) != sizeof(IfsIndexEntry))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileWritingError: fwrite(%p, 1, %d, %s) failed (%d) in line %d of %s\n",
                        &ifsHandle->entry, sizeof(IfsIndexEntry),
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                g_static_mutex_unlock(&(ifsHandle->mutex));
                return IfsReturnCodeFileWritingError;
            }
#ifdef FLUSH_ALL_WRITES
            if (fflush(ifsHandle->pNdex))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileFlushingError: fflush(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                g_static_mutex_unlock(&(ifsHandle->mutex));
                return IfsReturnCodeFileFlushingError;
            }
#endif
            ifsHandle->ndexSize += sizeof(IfsIndexEntry);
        }

        ifsHandle->realLoc++; // offset in packets
        ifsHandle->virtLoc++; // offset in packets
    }

    if (fseek(ifsHandle->pMpeg, 0, SEEK_END))
    {
        RILOG_ERROR(
                "IfsReturnCodeFileSeekingError: fseek(%s, 0, SEEK_END) failed (%d) in line %d of %s\n",
                ifsHandle->mpeg, errno, __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeFileSeekingError;
    }
    if (fwrite(pData, sizeof(IfsPacket), numPackets, ifsHandle->pMpeg)
            != numPackets)
    {
        RILOG_ERROR(
                "IfsReturnCodeFileWritingError: fwrite(%p, %d, %ld, %s) failed (%d) in line %d of %s\n",
                pData, sizeof(IfsPacket), numPackets, ifsHandle->mpeg, errno,
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeFileWritingError;
    }
#ifdef FLUSH_ALL_WRITES
    if (fflush(ifsHandle->pMpeg))
    {
        RILOG_ERROR(
                "IfsReturnCodeFileFlushingError: fflush(%s) failed (%d) in line %d of %s\n",
                ifsHandle->mpeg, errno, __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeFileFlushingError;
    }
#endif
    ifsHandle->mpegSize += (NumBytes) sizeof(IfsPacket) * (NumBytes) numPackets;

    g_static_mutex_unlock(&(ifsHandle->mutex));
    return IfsReturnCodeNoErrorReported;
}

IfsReturnCode IfsConvert(IfsHandle srcHandle, // Input
        IfsHandle dstHandle, // Input (must be a writer)
        IfsClock * pBegClock, // Input request/Output actual, in nanoseconds
        IfsClock * pEndClock // Input request/Output actual, in nanoseconds
)
{
    IfsIndexEntry * buffer = NULL;
    IfsReturnCode ifsReturnCode;
    FileNumber begFileNumber, endFileNumber;
    NumPackets begPacketNum, endPacketNum, numPackets;
    NumEntries begEntryNum, endEntryNum, numEntries, i;
    IfsClock begClock, endClock;

#ifdef DEBUG_CONVERT_APPEND
    printf("Convert:\n");
#endif

    if (srcHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: srcHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (dstHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: dstHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (pBegClock == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pBegClock == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (pEndClock == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pEndClock == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(dstHandle->mutex));

    if (dstHandle->isReading)
    {
        RILOG_ERROR("IfsReturnCodeMustBeAnIfsWriter: in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(dstHandle->mutex));
        return IfsReturnCodeMustBeAnIfsWriter;
    }

    do
    {
#ifdef DEBUG_CONVERT_APPEND
        char temp[32]; // IfsToSecs only
#endif
        int b = 0;
        g_static_mutex_lock(&(srcHandle->mutex));

        ifsReturnCode = GetCurrentFileParameters(srcHandle);
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        endClock = srcHandle->endClock < *pEndClock ? srcHandle->endClock
                : *pEndClock;
        begClock = srcHandle->begClock > *pBegClock ? srcHandle->begClock
                : *pBegClock;

#ifdef DEBUG_CONVERT_APPEND
        printf("endClock %s\n", IfsToSecs(endClock, temp));
        printf("begClock %s\n", IfsToSecs(begClock, temp));
#endif

        g_static_mutex_unlock(&(srcHandle->mutex));
        ifsReturnCode = IfsSeekToTimeImpl(srcHandle, IfsDirectEnd, &endClock,
                NULL);
        g_static_mutex_lock(&(srcHandle->mutex));

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        endPacketNum = srcHandle->realLoc;
        endFileNumber = srcHandle->curFileNumber;
        endEntryNum = srcHandle->entryNum;

#ifdef DEBUG_CONVERT_APPEND
        printf("endClock %s endPacketNum %5ld endFileNumber %3ld endEntryNum %4ld\n",
                IfsToSecs(endClock, temp), endPacketNum, endFileNumber, endEntryNum);
#endif

        g_static_mutex_unlock(&(srcHandle->mutex));
        ifsReturnCode = IfsSeekToTimeImpl(srcHandle, IfsDirectBegin, &begClock,
                NULL);
        g_static_mutex_lock(&(srcHandle->mutex));

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        begPacketNum = srcHandle->realLoc;
        begFileNumber = srcHandle->curFileNumber;
        begEntryNum = srcHandle->entryNum;

#ifdef DEBUG_CONVERT_APPEND
        printf("begClock %s begPacketNum %5ld begFileNumber %3ld begEntryNum %4ld\n",
                IfsToSecs(begClock, temp), begPacketNum, begFileNumber, begEntryNum);
#endif

        if (begFileNumber != endFileNumber) // Just convert to the end of the file for now, append more later
        {
            endPacketNum = srcHandle->maxPacket;
            endEntryNum = srcHandle->maxEntry;

#ifdef DEBUG_CONVERT_APPEND
            printf("maxPacket %5ld maxEntry %4ld\n",
                    srcHandle->maxPacket, srcHandle->maxEntry);
#endif

            // Need to calculate the correct endClock time:
            ifsReturnCode = IfsReadNdexEntryAt(srcHandle, endEntryNum);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;
            endClock = srcHandle->entry.when;
            if (fseek(srcHandle->pNdex, begEntryNum * sizeof(IfsIndexEntry),
                    SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) in line %d of %s\n",
                        srcHandle->ndex, begEntryNum * sizeof(IfsIndexEntry),
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }
        }

        numPackets = endPacketNum - begPacketNum + 1;
        numEntries = endEntryNum - begEntryNum + 1;

#ifdef DEBUG_CONVERT_APPEND
        printf("numPackets(%5ld) = endPacketNum(%5ld) - begPacketNum(%5ld) + 1\n",
                numPackets, endPacketNum, begPacketNum);
        printf("numEntries(%5ld) = endEntryNum (%5ld) - begEntryNum (%5ld) + 1\n",
                numEntries, endEntryNum, begEntryNum);
#endif
        if (0 == numPackets)
        {
            RILOG_DEBUG("%s numPackets(%5ld) = "
                        "endPacketNum(%5ld) - begPacketNum(%5ld) + 1\n",
                        __func__, numPackets, endPacketNum, begPacketNum);
            RILOG_DEBUG("%s numEntries(%5ld) = "
                        "endEntryNum (%5ld) - begEntryNum (%5ld) + 1\n",
                        __func__, numEntries, endEntryNum, begEntryNum);
            break;
        }
        else
        {
            buffer = g_try_malloc(numPackets * sizeof(IfsPacket));
            if (buffer == NULL)
            {
                RILOG_CRIT(
                        "IfsReturnCodeMemAllocationError: buffer == NULL in line %d of %s trying to allocate %lu\n",
                        __LINE__, __FILE__, numPackets * sizeof(IfsPacket));;
                ifsReturnCode = IfsReturnCodeMemAllocationError;
                break;
            }
        }

        RILOG_TRACE("Copy   %5ld MPEG packets [%5ld to %5ld] (%7ld bytes)\n",
                numPackets, begPacketNum, endPacketNum, sizeof(IfsPacket)
                        * numPackets);
        RILOG_TRACE("Copy   %5ld NDEX entries [%5ld to %5ld] (%7ld bytes)\n",
                numEntries, begEntryNum, endEntryNum, sizeof(IfsIndexEntry)
                        * numEntries);

        // Copy the MPEG data
        if ((b = fread(buffer, sizeof(IfsPacket), numPackets, srcHandle->pMpeg))
                != numPackets)
        {
            RILOG_ERROR("IfsReturnCodeFileReadingError: %d = "
                        "fread(%p, %d, %ld, %s) in line %d of %s\n",
                        b, buffer, sizeof(IfsPacket), numPackets,
                        srcHandle->mpeg, __LINE__, __FILE__);
            if (0 == b)
            {
                // if we've had 4 successive empty fread calls, stop trying!
                if (++srcHandle->numEmptyFreads > 4)
                {
                    ifsReturnCode = IfsReturnCodeFileReadingError;
                    break;
                }
            }
            else
            {
                // we read less than expected, reset our numPackets
                numPackets = b;
            }
        }

        // we've read something, reset our empty read counter...
        srcHandle->numEmptyFreads = 0;

        if (fseek(dstHandle->pMpeg, 0, SEEK_END))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileSeekingError: fseek(%s, 0, SEEK_END) in line %d of %s\n",
                    dstHandle->mpeg, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileSeekingError;
            break;
        }
        if (fwrite(buffer, sizeof(IfsPacket), numPackets, dstHandle->pMpeg)
                != numPackets)
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileWritingError: fwrite(%p, %d, %ld, %s) in line %d of %s\n",
                    buffer, sizeof(IfsPacket), numPackets, dstHandle->mpeg,
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileWritingError;
            break;
        }
#ifdef FLUSH_ALL_WRITES
        if (fflush(dstHandle->pMpeg))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileFlushingError: fflush(%s) in line %d of %s\n",
                    dstHandle->mpeg, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileFlushingError;
            break;
        }
#endif
        dstHandle->mpegSize = (NumBytes) sizeof(IfsPacket)
                * (NumBytes) numPackets;

        // Copy the NDEX data (with modification)
        if ((b = fread(buffer, sizeof(IfsIndexEntry), numEntries,
                 srcHandle->pNdex)) != numEntries)
        {
            RILOG_ERROR("IfsReturnCodeFileReadingError: %d = "
                        "fread(%p, %d, %ld, %s) in line %d of %s\n",
                        b, buffer, sizeof(IfsIndexEntry), numEntries,
                        srcHandle->ndex, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileReadingError;
            break;
        }
        for (i = 0; i < numEntries; i++)
        {
            //          RILOG_INFO("%10ld %3ld -> %3ld\n", i, buffer[i].where, buffer[i].where - begPacketNum);
            buffer[i].virtWhere = (buffer[i].realWhere -= begPacketNum);
        }
        if (fseek(dstHandle->pNdex, 0, SEEK_END))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileSeekingError: fseek(%s, 0, SEEK_END) in line %d of %s\n",
                    dstHandle->ndex, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileSeekingError;
            break;
        }
        if (fwrite(buffer, sizeof(IfsIndexEntry), numEntries, dstHandle->pNdex)
                != numEntries)
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileWritingError: fwrite(%p, %d, %ld, %s) in line %d of %s\n",
                    buffer, sizeof(IfsIndexEntry), numEntries, dstHandle->ndex,
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileWritingError;
            break;
        }
#ifdef FLUSH_ALL_WRITES
        if (fflush(dstHandle->pNdex))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileFlushingError: fflush(%s) in line %d of %s\n",
                    dstHandle->ndex, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileFlushingError;
            break;
        }
#endif
        dstHandle->ndexSize = (NumBytes) sizeof(IfsIndexEntry)
                * (NumBytes) numEntries;

        if (begFileNumber == endFileNumber) // Everything we need is in one file (so far)
        {
            srcHandle->appendFileNumber = endFileNumber;
            srcHandle->appendPacketNum = endPacketNum + 1;
            srcHandle->appendEntryNum = endEntryNum + 1;
            srcHandle->appendIndexShift = begPacketNum;
            srcHandle->appendPrevFiles = 0;
        }
        else // Just converted to the end of the file, set up for the next append
        {
            srcHandle->appendFileNumber = begFileNumber + 1;
            srcHandle->appendPacketNum = 0;
            srcHandle->appendEntryNum = 0;
            srcHandle->appendIndexShift = 0;
            srcHandle->appendPrevFiles = numPackets;
        }

    } while (0);

    if (ifsReturnCode == IfsReturnCodeNoErrorReported)
    {
        dstHandle->begClock = *pBegClock = begClock;
        dstHandle->endClock = *pEndClock = endClock;
    }
    else
    {
        dstHandle->begClock = *pBegClock = 0;
        dstHandle->endClock = *pEndClock = 0;
        dstHandle->mpegSize = 0;
        dstHandle->ndexSize = 0;
    }

    g_static_mutex_unlock(&(srcHandle->mutex));
    g_static_mutex_unlock(&(dstHandle->mutex));

    if (buffer)
        g_free(buffer);

    return ifsReturnCode;
}

IfsReturnCode IfsAppend // Must call IfsConvert() before calling this function
(IfsHandle srcHandle, // Input
        IfsHandle dstHandle, // Input (must be a writer)
        IfsClock * pEndClock // Input request/Output actual, in nanoseconds
)
{
    IfsIndexEntry * buffer = NULL;
    IfsReturnCode ifsReturnCode;
    NumEntries numEntries, i;
    NumPackets numPackets;

    FileNumber begFileNumber; // from srcHandle via append info
    FileNumber endFileNumber; // from srcHandle via seek to endClock
    NumPackets begPacketNum; // from srcHandle via append info
    NumPackets endPacketNum; // from srcHandle via seek to endClock
    NumEntries begEntryNum; // from srcHandle via append info
    NumEntries endEntryNum; // from srcHandle via seek to endClock
    IfsClock endClock; // from srcHandle via IfsInfo then seek to endClock

#ifdef DEBUG_CONVERT_APPEND
    printf("Append:\n");
#endif

    if (srcHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: srcHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (dstHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: dstHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (pEndClock == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pEndClock == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(dstHandle->mutex));

    if (dstHandle->isReading)
    {
        RILOG_ERROR("IfsReturnCodeMustBeAnIfsWriter: in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(dstHandle->mutex));
        return IfsReturnCodeMustBeAnIfsWriter;
    }

    do
    {
#ifdef DEBUG_CONVERT_APPEND
        char temp[32]; // IfsToSecs only
#endif
        g_static_mutex_lock(&(srcHandle->mutex));

        // See how much data is currently available
        ifsReturnCode = GetCurrentFileParameters(srcHandle);

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        endClock = srcHandle->endClock < *pEndClock ? srcHandle->endClock
                : *pEndClock;

#ifdef DEBUG_CONVERT_APPEND
        printf("endClock %s\n", IfsToSecs(endClock, temp));
#endif

        g_static_mutex_unlock(&(srcHandle->mutex));
        ifsReturnCode = IfsSeekToTimeImpl(srcHandle, IfsDirectEnd, &endClock,
                NULL);
        g_static_mutex_lock(&(srcHandle->mutex));

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        endPacketNum = srcHandle->realLoc;
        endFileNumber = srcHandle->curFileNumber;
        endEntryNum = srcHandle->entryNum;

#ifdef DEBUG_CONVERT_APPEND
        printf("endClock %s endPacketNum %5ld endFileNumber %3ld endEntryNum %4ld\n",
                IfsToSecs(endClock, temp), endPacketNum, endFileNumber, endEntryNum);
#endif

        begPacketNum = srcHandle->appendPacketNum;
        begFileNumber = srcHandle->appendFileNumber;
        begEntryNum = srcHandle->appendEntryNum;

#ifdef DEBUG_CONVERT_APPEND
        printf("begClock    NA begPacketNum %5ld begFileNumber %3ld begEntryNum %4ld\n",
                begPacketNum, begFileNumber, begEntryNum);
#endif

        g_static_mutex_unlock(&(srcHandle->mutex));
        ifsReturnCode = IfsOpenActualFiles(srcHandle, begFileNumber, "rb+");
        g_static_mutex_lock(&(srcHandle->mutex));

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        if (fseek(srcHandle->pMpeg, begPacketNum * sizeof(IfsPacket), SEEK_SET))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) in line %d of %s\n",
                    srcHandle->mpeg, begPacketNum * sizeof(IfsPacket),
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileSeekingError;
            break;
        }
        if (fseek(srcHandle->pNdex, begEntryNum * sizeof(IfsIndexEntry),
                SEEK_SET))
        {
            RILOG_ERROR(
                    "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) in line %d of %s\n",
                    srcHandle->ndex, begEntryNum * sizeof(IfsIndexEntry),
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeFileSeekingError;
            break;
        }

        if (begFileNumber != endFileNumber) // Just convert to the end of the file for now, append more later
        {
            struct stat statBuffer;

            if (stat(srcHandle->mpeg, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        srcHandle->mpeg, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }
            endPacketNum = (size_t) statBuffer.st_size / sizeof(IfsPacket) - 1;

            if (stat(srcHandle->ndex, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        srcHandle->ndex, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }
            endEntryNum = (size_t) statBuffer.st_size / sizeof(IfsIndexEntry)
                    - 1;

#ifdef DEBUG_CONVERT_APPEND
            printf("maxPacket %5ld maxEntry %4ld\n", endPacketNum, endEntryNum);
#endif

            // Need to calculate the correct endClock time:
            ifsReturnCode = IfsReadNdexEntryAt(srcHandle, endEntryNum);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;
            endClock = srcHandle->entry.when;
            if (fseek(srcHandle->pNdex, begEntryNum * sizeof(IfsIndexEntry),
                    SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) in line %d of %s\n",
                        srcHandle->ndex, begEntryNum * sizeof(IfsIndexEntry),
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }
        }

        numPackets = endPacketNum - begPacketNum + 1;
        numEntries = endEntryNum - begEntryNum + 1;

#ifdef DEBUG_CONVERT_APPEND
        printf("numPackets(%5ld) = endPacketNum(%5ld) - begPacketNum(%5ld) + 1\n",
                numPackets, endPacketNum, begPacketNum);
        printf("numEntries(%5ld) = endEntryNum (%5ld) - begEntryNum (%5ld) + 1\n",
                numEntries, endEntryNum, begEntryNum);
#endif
        if (0 == numPackets)
        {
            RILOG_DEBUG("%s numPackets(%5ld) = "
                        "endPacketNum(%5ld) - begPacketNum(%5ld) + 1\n",
                        __func__, numPackets, endPacketNum, begPacketNum);
            RILOG_DEBUG("%s numEntries(%5ld) = "
                        "endEntryNum (%5ld) - begEntryNum (%5ld) + 1\n",
                        __func__, numEntries, endEntryNum, begEntryNum);
        }
        else
        {
            buffer = g_try_malloc(numPackets * sizeof(IfsPacket));
            if (buffer == NULL)
            {
                RILOG_CRIT(
                        "IfsReturnCodeMemAllocationError: buffer == NULL in line %d of %s trying to allocate %lu\n",
                        __LINE__, __FILE__, numPackets * sizeof(IfsPacket));;
                ifsReturnCode = IfsReturnCodeMemAllocationError;
                break;
            }
        }

        if (numPackets || numEntries)
        {
            int b = 0;
            RILOG_TRACE(
                    "Append %5ld MPEG packets [%5ld to %5ld] (%7ld bytes)\n",
                    numPackets, begPacketNum, endPacketNum, sizeof(IfsPacket)
                            * numPackets);
            RILOG_TRACE(
                    "Append %5ld NDEX entries [%5ld to %5ld] (%7ld bytes)\n",
                    numEntries, begEntryNum, endEntryNum, sizeof(IfsIndexEntry)
                            * numEntries);

            // Copy the MPEG data
            if ((b = fread(buffer, sizeof(IfsPacket), numPackets,
                     srcHandle->pMpeg)) != numPackets)
            {
                RILOG_WARN("IfsReturnCodeFileReadingError: %d = "
                           "fread(%p, %d, %ld, %s) in line %d of %s\n",
                           b, buffer, sizeof(IfsPacket), numPackets,
                           srcHandle->mpeg, __LINE__, __FILE__);

                if (0 == b)
                {
                    // if we've had 4 successive empty fread calls, stop trying!
                    if (++srcHandle->numEmptyFreads > 4)
                    {
                        ifsReturnCode = IfsReturnCodeFileReadingError;
                        break;
                    }
                }
                else
                {
                    // we read less than expected, reset our numPackets
                    numPackets = b;
                }
            }

            // we've read something, reset our empty read counter...
            srcHandle->numEmptyFreads = 0;

            if (fseek(dstHandle->pMpeg, 0, SEEK_END))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, 0, SEEK_END) in line %d of %s\n",
                        dstHandle->mpeg, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }
            if (fwrite(buffer, sizeof(IfsPacket), numPackets, dstHandle->pMpeg)
                    != numPackets)
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileWritingError: fwrite(%p, %d, %ld, %s) in line %d of %s\n",
                        buffer, sizeof(IfsPacket), numPackets, dstHandle->mpeg,
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileWritingError;
                break;
            }
#ifdef FLUSH_ALL_WRITES
            if (fflush(dstHandle->pMpeg))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileFlushingError: fflush(%s) in line %d of %s\n",
                        dstHandle->mpeg, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileFlushingError;
                break;
            }
#endif
            dstHandle->mpegSize += (NumBytes) sizeof(IfsPacket)
                    * (NumBytes) numPackets;

            // Copy the NDEX data (with modification)
            if ((b = fread(buffer, sizeof(IfsIndexEntry), numEntries,
                     srcHandle->pNdex)) != numEntries)
            {
                RILOG_ERROR("IfsReturnCodeFileReadingError: %d = "
                            "fread(%p, %d, %ld, %s) in line %d of %s\n",
                            b, buffer, sizeof(IfsIndexEntry), numEntries,
                            srcHandle->ndex, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileReadingError;
                break;
            }
            for (i = 0; i < numEntries; i++)
            {
                //              RILOG_INFO("%10ld %3ld -> %3ld\n",
                //                         i+numEntriesOut,
                //                         buffer[i].where,
                //                         buffer[i].where - srcHandle->appendIndexShift);
                buffer[i].realWhere -= srcHandle->appendIndexShift;
                buffer[i].realWhere += srcHandle->appendPrevFiles;
                buffer[i].virtWhere = buffer[i].realWhere;
            }
            if (fseek(dstHandle->pNdex, 0, SEEK_END))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, 0, SEEK_END) in line %d of %s\n",
                        dstHandle->ndex, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }
            if (fwrite(buffer, sizeof(IfsIndexEntry), numEntries,
                    dstHandle->pNdex) != numEntries)
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileWritingError: fwrite(%p, %d, %ld, %s) in line %d of %s\n",
                        buffer, sizeof(IfsIndexEntry), numEntries,
                        dstHandle->ndex, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileWritingError;
                break;
            }
#ifdef FLUSH_ALL_WRITES
            if (fflush(dstHandle->pNdex))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileFlushingError: fflush(%s) in line %d of %s\n",
                        dstHandle->ndex, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileFlushingError;
                break;
            }
#endif
            dstHandle->ndexSize += (NumBytes) sizeof(IfsIndexEntry)
                    * (NumBytes) numEntries;
        }
        else
        {
            struct stat statBuffer;
            NumPackets maxPacketNum;

            if (stat(srcHandle->mpeg, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        srcHandle->mpeg, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }
            maxPacketNum = (size_t) statBuffer.st_size / sizeof(IfsPacket) - 1;

#ifdef DEBUG_CONVERT_APPEND
            printf("maxPacketNum %5ld endPacketNum %5ld\n", maxPacketNum, endPacketNum);
#endif

            if (endPacketNum < maxPacketNum)
            {
#ifdef DEBUG_CONVERT_APPEND
                printf("SPECIAL CASE DETECTED\n");
#endif

                endClock = *pEndClock;
            }
        }

        if (begFileNumber == endFileNumber) // Everything we need is in one file (so far)
        {
            srcHandle->appendFileNumber = endFileNumber;
            srcHandle->appendPacketNum = endPacketNum + 1;
            srcHandle->appendEntryNum = endEntryNum + 1;
            //cHandle->appendIndexShift = <does not change>
            //cHandle->appendPrevFiles  = <does not change>

        }
        else // Just converted to the end of the file, set up for the next append
        {
            srcHandle->appendFileNumber = begFileNumber + 1;
            srcHandle->appendPacketNum = 0;
            srcHandle->appendEntryNum = 0;
            srcHandle->appendIndexShift = 0;
            srcHandle->appendPrevFiles = dstHandle->mpegSize
                    / sizeof(IfsPacket);
        }

    } while (0);

    if (ifsReturnCode == IfsReturnCodeNoErrorReported)
    {
        dstHandle->endClock = *pEndClock = endClock;
    }
    else
    {
        dstHandle->begClock = 0;
        dstHandle->endClock = *pEndClock = 0;
        dstHandle->mpegSize = 0;
        dstHandle->ndexSize = 0;
    }

    g_static_mutex_unlock(&(srcHandle->mutex));
    g_static_mutex_unlock(&(dstHandle->mutex));

    if (buffer)
        g_free(buffer);

    return ifsReturnCode;
}

IfsReturnCode IfsSeekToTimeImpl // Must call GetCurrentFileParameters() before calling this function
(IfsHandle ifsHandle, // Input
        IfsDirect ifsDirect, // Input  either IfsDirectBegin,
        //        IfsDirectEnd or IfsDirectEither
        IfsClock * pIfsClock, // Input  requested/Output actual, in nanoseconds
        NumPackets * pPosition // Output packet position, optional, can be NULL
)
{
#ifdef STAND_ALONE
#ifdef DEBUG_SEEKS
    char temp1[32], temp2[32], temp3[32]; // IfsToSecs only
#else
#ifdef DEBUG_ERROR_LOGS
    char temp1[32], temp2[32]; // IfsToSecs only
#endif
#endif
#else // not STAND_ALONE
#ifdef DEBUG_SEEKS
    char temp1[32], temp2[32], temp3[32]; // IfsToSecs only
#else
    char temp1[32], temp2[32]; // IfsToSecs only
#endif
#endif

    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;
    IfsClock ifsClock = *pIfsClock;
    g_static_mutex_lock(&(ifsHandle->mutex));

    if (ifsClock < ifsHandle->begClock)
    {
        RILOG_WARN(
                "IfsReturnCodeSeekOutsideFile: %s < %s (begClock) in line %d of %s\n",
                IfsToSecs(ifsClock, temp1), IfsToSecs(ifsHandle->begClock,
                        temp2), __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeSeekOutsideFile;
    }

    if (ifsClock > ifsHandle->endClock)
    {
        RILOG_WARN(
                "IfsReturnCodeSeekOutsideFile: %s > %s (endClock) in line %d of %s\n",
                IfsToSecs(ifsClock, temp1), IfsToSecs(ifsHandle->endClock,
                        temp2), __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeSeekOutsideFile;
    }

    for (ifsHandle->curFileNumber = ifsHandle->begFileNumber; ifsHandle->curFileNumber
            <= ifsHandle->endFileNumber; ifsHandle->curFileNumber++)
    {
        IfsClock begClock;
        IfsClock endClock;
        struct stat statBuffer;

        g_static_mutex_unlock(&(ifsHandle->mutex));
        ifsReturnCode = IfsOpenActualFiles(ifsHandle, ifsHandle->curFileNumber,
                "rb+");
        g_static_mutex_lock(&(ifsHandle->mutex));

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        if (stat(ifsHandle->ndex, &statBuffer))
        {
            RILOG_ERROR(
                    "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->ndex, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeStatError;
            break;
        }

        ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, 0);
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        begClock = ifsHandle->entry.when - 1; // nanoseconds

        ifsReturnCode = IfsReadNdexEntryAt(ifsHandle,
                ((size_t) statBuffer.st_size / sizeof(IfsIndexEntry) - 1));
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        endClock = ifsHandle->entry.when + 1; // nanoseconds

#ifdef DEBUG_SEEKS
        RILOG_INFO("%s <= %s <= %s ?\n",
                IfsToSecs(begClock, temp1),
                IfsToSecs(ifsClock, temp2),
                IfsToSecs(endClock, temp3));
#endif

        if (ifsClock <= endClock)
        {
            // Found the file!
            //
            // entry = m(x - b)
            //
            // x = ifsClock(nsec)
            //
            // b = begTime(nsec)
            //
            // m = rise / run
            //   = numEntries(entries) / (endClock(nsec) - begClock(nsec))
            //
            // entry = [numEntries(entries) / (endClock(nsec) - begClock(nsec))][locations(nsec) - begClock(nsec)]
            //       = numEntries(entries)[locations(nsec) - begClock(nsec)] / [(endClock(nsec) - begClock(nsec))]

            llong diff;
            NumEntries entry;

            ifsHandle->maxEntry = (size_t) statBuffer.st_size
                    / sizeof(IfsIndexEntry) - 1;

            entry = (ifsClock <= begClock ? 0 : ifsHandle->maxEntry * (ifsClock
                    - begClock) / (endClock - begClock));

            ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, entry);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;

            diff = ifsClock - ifsHandle->entry.when; // nanoseconds

#ifdef DEBUG_SEEKS
            RILOG_INFO("\nSeek to file number %3ld about entry %3ld want %s got %s diff %s\n",
                    ifsHandle->curFileNumber,
                    entry,
                    IfsToSecs(ifsClock, temp1),
                    IfsToSecs(ifsHandle->entry.when, temp2),
                    IfsToSecs(diff, temp3));
#endif

            if (diff > 0) // scan forward for best seek position
            {
                while (entry < ifsHandle->maxEntry)
                {
                    llong temp;

                    ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, ++entry);
                    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                        break;

                    temp = ifsClock - ifsHandle->entry.when; // nanoseconds
#ifdef DEBUG_SEEKS
                    RILOG_INFO("                          Try entry %3ld want %s got %s diff %s\n",
                            entry,
                            IfsToSecs(ifsClock, temp1),
                            IfsToSecs(ifsHandle->entry.when, temp2),
                            IfsToSecs(temp, temp3));
#endif
                    if (temp >= 0)
                    {
                        // Must be better (or the same), keep trying (unless done)
                        diff = temp;
                    }
                    else
                    {
                        // We are done here but which one is better?
                        if (-temp < diff)
                        {
                            // The negative one is better
                            diff = temp;
                        }
                        else // The old positive one was better
                        {
                            entry--;
                        }
                        break;
                    }
                }

                if (ifsDirect == IfsDirectBegin)
                {
#ifdef DEBUG_SEEKS
                    RILOG_INFO("                          Try backup\n");
#endif
                    while (entry)
                    {
                        llong temp;

                        ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, --entry);
                        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                            break;

                        temp = ifsClock - ifsHandle->entry.when; // nanoseconds
#ifdef DEBUG_SEEKS
                        RILOG_INFO("                          Try entry %3ld want %s got %s diff %s\n",
                                entry,
                                IfsToSecs(ifsClock, temp1),
                                IfsToSecs(ifsHandle->entry.when, temp2),
                                IfsToSecs(temp, temp3));
#endif
                        if (temp != diff) // Moved back as far as we can go
                        {
                            entry++;
                            break;
                        }
                    }
                }
            }
            else if (diff < 0)
            {
                while (entry)
                {
                    llong temp;

                    ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, --entry);
                    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                        break;

                    temp = ifsClock - ifsHandle->entry.when; // nanoseconds
#ifdef DEBUG_SEEKS
                    RILOG_INFO("                          Try entry %3ld want %s got %s diff %s\n",
                            entry,
                            IfsToSecs(ifsClock, temp1),
                            IfsToSecs(ifsHandle->entry.when, temp2),
                            IfsToSecs(temp, temp3));
#endif
                    if (temp <= 0)
                    {
                        // Must be better (or the same), keep trying (unless done)
                        diff = temp;
                    }
                    else
                    {
                        // We are done here but which one is better?
                        if (temp < -diff)
                        {
                            // The positive one is better
                            diff = temp;
                        }
                        else // The old negative one was better
                        {
                            entry++;
                        }
                        break;
                    }
                }

                if (ifsDirect == IfsDirectEnd)
                {
#ifdef DEBUG_SEEKS
                    RILOG_INFO("                          Try forward\n");
#endif
                    while (entry < ifsHandle->maxEntry)
                    {
                        llong temp;

                        ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, ++entry);
                        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                            break;

                        temp = ifsClock - ifsHandle->entry.when; // nanoseconds
#ifdef DEBUG_SEEKS
                        RILOG_INFO("                          Try entry %3ld want %s got %s diff %s\n",
                                entry,
                                IfsToSecs(ifsClock, temp1),
                                IfsToSecs(ifsHandle->entry.when, temp2),
                                IfsToSecs(temp, temp3));
#endif
                        if (temp != diff) // Moved forward as far as we can go
                        {
                            entry--;
                            break;
                        }
                    }
                }
            }
            else
            {
                switch (ifsDirect)
                {
                case IfsDirectBegin:
#ifdef DEBUG_SEEKS
                    RILOG_INFO("                          Try backup\n");
#endif
                    while (entry)
                    {
                        llong temp;

                        ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, --entry);
                        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                            break;

                        temp = ifsClock - ifsHandle->entry.when; // nanoseconds
#ifdef DEBUG_SEEKS
                        RILOG_INFO("                          Try entry %3ld want %s got %s diff %s\n",
                                entry,
                                IfsToSecs(ifsClock, temp1),
                                IfsToSecs(ifsHandle->entry.when, temp2),
                                IfsToSecs(temp, temp3));
#endif
                        if (temp != diff) // Moved back as far as we can go
                        {
                            entry++;
                            break;
                        }
                    }
                    break;

                case IfsDirectEnd:
#ifdef DEBUG_SEEKS
                    RILOG_INFO("                          Try forward %ld\n",
                            ifsHandle->maxEntry);
#endif
                    while (entry < ifsHandle->maxEntry)
                    {
                        llong temp;

                        ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, ++entry);
                        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                            break;

                        temp = ifsClock - ifsHandle->entry.when; // nanoseconds
#ifdef DEBUG_SEEKS
                        RILOG_INFO("                          Try entry %3ld want %s got %s diff %s\n",
                                entry,
                                IfsToSecs(ifsClock, temp1),
                                IfsToSecs(ifsHandle->entry.when, temp2),
                                IfsToSecs(temp, temp3));
#endif
                        if (temp != diff) // Moved forward as far as we can go
                        {
                            entry--;
                            break;
                        }
                    }
                    break;

                case IfsDirectEither:
                    break;
                }
            }

            ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, entry);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;
            if (fseek(ifsHandle->pNdex, entry * sizeof(IfsIndexEntry), SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, entry * sizeof(IfsIndexEntry), errno,
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }

#ifdef DEBUG_SEEKS
            RILOG_INFO("                      Best is entry %3ld want %s got %s diff %s\n",
                    entry,
                    IfsToSecs(ifsClock, temp1),
                    IfsToSecs(ifsHandle->entry.when, temp2),
                    IfsToSecs(diff, temp3));
            RILOG_INFO("                                        when %s where %ld/%ld\n",
                    IfsToSecs(ifsHandle->entry.when, temp1),
                    ifsHandle->entry.realWhere,
                    ifsHandle->entry.virtWhere);
#endif

            if (fseek(ifsHandle->pMpeg, ifsHandle->entry.realWhere
                    * sizeof(IfsPacket), SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) failed (%d) in line %d of %s\n",
                        ifsHandle->mpeg, ifsHandle->entry.realWhere
                                * sizeof(IfsPacket), errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }

            if (stat(ifsHandle->mpeg, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->mpeg, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }

            ifsHandle->maxPacket = (size_t) statBuffer.st_size
                    / sizeof(IfsPacket) - 1;
            ifsHandle->realLoc = ifsHandle->entry.realWhere;
            ifsHandle->virtLoc = ifsHandle->entry.virtWhere;
            ifsHandle->entryNum = entry;

            *pIfsClock = ifsHandle->entry.when;
            if (pPosition)
                *pPosition = ifsHandle->virtLoc;
            break;
        }
    }

    g_static_mutex_unlock(&(ifsHandle->mutex));
    return ifsReturnCode;
}

IfsReturnCode IfsSeekToTime(IfsHandle ifsHandle, // Input
        IfsDirect ifsDirect, // Input  either IfsDirectBegin,
        //        IfsDirectEnd or IfsDirectEither
        IfsClock * pIfsClock, // Input  requested/Output actual, in nanoseconds
        NumPackets * pPosition // Output packet position, optional, can be NULL
)
{
    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    if ((ifsDirect != IfsDirectBegin) && (ifsDirect != IfsDirectEnd)
            && (ifsDirect != IfsDirectEither))
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsDirect is not valid in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    if (pIfsClock == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pIfsClock == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));

    if ((*pIfsClock < ifsHandle->begClock)
            || (*pIfsClock > ifsHandle->endClock))
    {
        IfsReturnCode ifsReturnCode = GetCurrentFileParameters(ifsHandle);
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
        {
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return ifsReturnCode;
        }
    }

    g_static_mutex_unlock(&(ifsHandle->mutex));
    return IfsSeekToTimeImpl(ifsHandle, ifsDirect, pIfsClock, pPosition);
}

IfsReturnCode IfsSeekToPacketImpl // Must call GetCurrentFileSizeAndCount() before calling this function
(IfsHandle ifsHandle, // Input
        NumPackets virtPos, // Input desired (virtual) packet position
        IfsClock * pIfsClock // Output clock value, optional, can be NULL
)
{
    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;
    NumPackets begPosition = 0;
    NumPackets endPosition = 0;
    NumPackets realPos; // desired (real) packet position
    g_static_mutex_lock(&(ifsHandle->mutex));

    for (ifsHandle->curFileNumber = ifsHandle->begFileNumber; ifsHandle->curFileNumber
            <= ifsHandle->endFileNumber; ifsHandle->curFileNumber++)
    {
        struct stat statBuffer;

        g_static_mutex_unlock(&(ifsHandle->mutex));
        ifsReturnCode = IfsOpenActualFiles(ifsHandle, ifsHandle->curFileNumber,
                "rb+");
        g_static_mutex_lock(&(ifsHandle->mutex));

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        if (stat(ifsHandle->mpeg, &statBuffer))
        {
            RILOG_ERROR(
                    "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->mpeg, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeStatError;
            break;
        }

        ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, 0);
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            break;

        begPosition = ifsHandle->entry.virtWhere - ifsHandle->entry.realWhere;
        endPosition = begPosition + (size_t) statBuffer.st_size
                / sizeof(IfsPacket);

#ifdef DEBUG_SEEKS
        RILOG_INFO("%ld <= %ld < %ld ?\n", begPosition, virtPos, endPosition);
#endif

        if ((begPosition <= virtPos) && (virtPos < endPosition))
        {
            // Found the file!
            //
            // Estimate the starting point for the seek into the ndex file by
            // calculating the percentage into the mpeg file:
            //
            // percentage = (virtPos-begPosition) / (endPosition-begPosition)
            //

#ifdef DEBUG_SEEKS
            char temp[32]; // IfsToSecs only
            long realDiff;
#endif
            long virtDiff;
            NumEntries entry;
            if (stat(ifsHandle->ndex, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }

            realPos = virtPos - begPosition;

            ifsHandle->maxEntry = (size_t) statBuffer.st_size
                    / sizeof(IfsIndexEntry) - 1;

            entry = ifsHandle->maxEntry * realPos / (endPosition - begPosition);

            ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, entry);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;

            virtDiff = virtPos - ifsHandle->entry.virtWhere;
#ifdef DEBUG_SEEKS
            realDiff = realPos - ifsHandle->entry.realWhere;

            RILOG_INFO("\nSeek to file number %3ld about entry %3ld want %ld/%ld got %ld/%ld diff %ld/%ld\n",
                    ifsHandle->curFileNumber,
                    entry,
                    realPos,
                    virtPos,
                    ifsHandle->entry.realWhere,
                    ifsHandle->entry.virtWhere,
                    realDiff,
                    virtDiff);
#endif

            if (virtDiff > 0) // scan forward for best seek
            {
                while (entry < ifsHandle->maxEntry)
                {
#ifdef DEBUG_SEEKS
                    long realTemp;
#endif
                    long virtTemp;

                    ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, ++entry);
                    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                        break;

#ifdef DEBUG_SEEKS
                    realTemp = realPos - ifsHandle->entry.realWhere;
#endif
                    virtTemp = virtPos - ifsHandle->entry.virtWhere;

#ifdef DEBUG_SEEKS
                    RILOG_INFO("                          Try entry %3ld want %ld/%ld got %ld/%ld diff %ld/%ld\n",
                            entry,
                            realPos,
                            virtPos,
                            ifsHandle->entry.realWhere,
                            ifsHandle->entry.virtWhere,
                            realTemp,
                            virtTemp);
#endif

                    if (virtTemp >= 0)
                    {
                        // Must be better (or the same), keep trying (unless done)
#ifdef DEBUG_SEEKS
                        realDiff = realTemp;
#endif
                        virtDiff = virtTemp;
                    }
                    else
                    {
                        // We are done here but which one is better?
                        if (-virtTemp < virtDiff)
                        {
                            // The negative one is better
#ifdef DEBUG_SEEKS
                            realDiff = realTemp;
                            virtDiff = virtTemp;
#endif
                        }
                        else // The old positive one was better
                        {
                            entry--;
                        }
                        break;
                    }
                }
            }
            else if (virtDiff < 0)
            {
                while (entry)
                {
#ifdef DEBUG_SEEKS
                    long realTemp;
#endif
                    long virtTemp;

                    ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, --entry);
                    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                        break;

                    virtTemp = virtPos - ifsHandle->entry.virtWhere;
#ifdef DEBUG_SEEKS
                    realTemp = realPos - ifsHandle->entry.realWhere;

                    RILOG_INFO("                          Try entry %3ld want %ld/%ld got %ld/%ld diff %ld/%ld\n",
                            entry,
                            realPos,
                            virtPos,
                            ifsHandle->entry.realWhere,
                            ifsHandle->entry.virtWhere,
                            realTemp,
                            virtTemp);
#endif

                    if (virtTemp <= 0)
                    {
                        // Must be better (or the same), keep trying (unless done)
#ifdef DEBUG_SEEKS
                        realDiff = realTemp;
#endif
                        virtDiff = virtTemp;
                    }
                    else
                    {
                        // We are done here but which one is better?
                        if (virtTemp < -virtDiff)
                        {
                            // The positive one is better
#ifdef DEBUG_SEEKS
                            realDiff = realTemp;
                            virtDiff = virtTemp;
#endif
                        }
                        else // The old negative one was better
                        {
                            entry++;
                        }
                        break;
                    }
                }
            }

            ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, entry);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;
            if (fseek(ifsHandle->pNdex, entry * sizeof(IfsIndexEntry), SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, entry * sizeof(IfsIndexEntry), errno,
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }

#ifdef DEBUG_SEEKS
            RILOG_INFO("                      Best is entry %3ld want %ld/%ld got %ld/%ld diff %ld/%ld\n",
                    entry,
                    realPos,
                    virtPos,
                    ifsHandle->entry.realWhere,
                    ifsHandle->entry.virtWhere,
                    realDiff,
                    virtDiff);
            RILOG_INFO("                                        when %s where %ld/%ld\n",
                    IfsToSecs(ifsHandle->entry.when, temp),
                    ifsHandle->entry.realWhere,
                    ifsHandle->entry.virtWhere);
#endif

            if (fseek(ifsHandle->pMpeg, realPos * sizeof(IfsPacket), SEEK_SET))
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileSeekingError: fseek(%s, %ld, SEEK_SET) failed (%d) in line %d of %s\n",
                        ifsHandle->mpeg, realPos * sizeof(IfsPacket), errno,
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileSeekingError;
                break;
            }

            if (stat(ifsHandle->mpeg, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->mpeg, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeStatError;
                break;
            }

            ifsHandle->maxPacket = (size_t) statBuffer.st_size
                    / sizeof(IfsPacket) - 1;
            ifsHandle->realLoc = realPos;
            ifsHandle->virtLoc = virtPos;
            ifsHandle->entryNum = entry;

            if (pIfsClock)
                *pIfsClock = ifsHandle->entry.when;
            break;
        }
    }

    // All errors come here
    g_static_mutex_unlock(&(ifsHandle->mutex));

    return ifsReturnCode;
}

IfsReturnCode IfsSeekToPacket(IfsHandle ifsHandle, // Input
        NumPackets virtPos, // Input desired (virtual) packet position
        IfsClock * pIfsClock // Output clock value, optional, can be NULL
)
{
    IfsReturnCode ifsReturnCode;

    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));
    ifsReturnCode = GetCurrentFileSizeAndCount(ifsHandle);
    g_static_mutex_unlock(&(ifsHandle->mutex));

    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
        return ifsReturnCode;

    return IfsSeekToPacketImpl(ifsHandle, virtPos, pIfsClock);
}

IfsReturnCode IfsRead(IfsHandle ifsHandle, // Input
        NumPackets * pNumPackets, // Input request/Output actual
        IfsClock * pCurClock, // Output current clock value
        IfsPacket ** ppData // Output
)
{
    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    if (ppData != NULL)
        *ppData = NULL;
    if (pNumPackets == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pNumPackets == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    do
    {
        struct stat statBuffer;
        NumBytes numBytes;
        NumPackets numPackets;
        NumPackets havePackets;

        if (ifsHandle == NULL)
        {
            RILOG_ERROR(
                    "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            *pNumPackets = 0;
            return IfsReturnCodeBadInputParameter;
        }

        g_static_mutex_lock(&(ifsHandle->mutex));

        if (ppData == NULL)
        {
            RILOG_ERROR(
                    "IfsReturnCodeBadInputParameter: ppData == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeBadInputParameter;
            break;
        }

        if (stat(ifsHandle->mpeg, &statBuffer))
        {
            RILOG_ERROR(
                    "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->mpeg, errno, __LINE__, __FILE__);
            ifsReturnCode = IfsReturnCodeStatError;
            break;
        }

        numBytes = (size_t) statBuffer.st_size;
        numPackets = numBytes / sizeof(IfsPacket);
        havePackets = numPackets - ifsHandle->realLoc;

        //      {   char temp[32];
        //
        //          RILOG_INFO("-----------------------\n");
        //          RILOG_INFO("*pNumPackets = %ld packets\n", *pNumPackets);
        //          RILOG_INFO("MPEG file    = %s\n"         , ifsHandle->mpeg);
        //          RILOG_INFO("numBytes     = %s bytes\n"   , IfsLongLongToString(numBytes, temp));
        //          RILOG_INFO("numPackets   = %ld packets\n", numPackets);
        //          RILOG_INFO("realLoc      = %ld packets\n", ifsHandle->realLoc);
        //          RILOG_INFO("virtLoc      = %ld packets\n", ifsHandle->virtLoc);
        //          RILOG_INFO("havePackets  = %ld packets\n", havePackets);
        //      }

        if (havePackets == 0)
        {
            ifsReturnCode = GetCurrentFileSizeAndCount(ifsHandle);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                break;

            if (ifsHandle->curFileNumber == ifsHandle->endFileNumber)
            {
                RILOG_INFO("IfsReturnCodeReadPastEndOfFile in line %d of %s\n",
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeReadPastEndOfFile;
            }
            else // go to the next file
            {
                g_static_mutex_unlock(&(ifsHandle->mutex));
                ifsReturnCode = IfsOpenActualFiles(ifsHandle,
                        ++ifsHandle->curFileNumber, "rb+");
                g_static_mutex_lock(&(ifsHandle->mutex));

                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    break;

                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, 0);
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                    break;

                ifsHandle->realLoc = 0;

                *pNumPackets = 0;
            }
            break;
        }
        else
        {
            if (*pNumPackets > havePackets)
                *pNumPackets = havePackets;

            //      RILOG_INFO("*pNumPackets = %ld packets\n", *pNumPackets);

            numBytes = (NumBytes) * pNumPackets * (NumBytes) sizeof(IfsPacket);

            //      RILOG_INFO("numBytes     = %s bytes\n", IfsLongLongToString(numBytes));

            *ppData = g_try_malloc(numBytes);
            if (*ppData == NULL)
            {
                RILOG_CRIT(
                        "IfsReturnCodeMemAllocationError: *ppData == NULL in line %d of %s\n",
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeMemAllocationError;
                break;
            }

            if (fread(*ppData, 1, numBytes, ifsHandle->pMpeg) != numBytes)
            {
#ifdef STAND_ALONE
#ifdef DEBUG_ERROR_LOGS
                char temp[32];
#endif
#else // not STAND_ALONE
                char temp[32];
#endif

                RILOG_ERROR(
                        "IfsReturnCodeFileReadingError: fread(%p, 1, %s, %s) failed (%d) in line %d of %s\n",
                        *ppData, IfsLongLongToString(numBytes, temp),
                        ifsHandle->mpeg, errno, __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileReadingError;
                break;
            }

            ifsHandle->realLoc += *pNumPackets;
            ifsHandle->virtLoc += *pNumPackets;
        }

        if (pCurClock)
        {
            *pCurClock = ifsHandle->entry.when;
            //printf("Clock init %s, %ld %ld\n", IfsToSecs(*pCurClock), ifsHandle->entry.where, ifsHandle->location);

            while (ifsHandle->entry.realWhere < ifsHandle->realLoc)
            {
                if (fread(&ifsHandle->entry, 1, sizeof(IfsIndexEntry),
                        ifsHandle->pNdex) != sizeof(IfsIndexEntry))
                {
                    break;
                }
                *pCurClock = ifsHandle->entry.when;
                //printf("Clock next %s, %ld %ld\n", IfsToSecs(*pCurClock), ifsHandle->entry.where, ifsHandle->location);
            }
        }
    } while (0);

    g_static_mutex_unlock(&(ifsHandle->mutex));

    if (ifsReturnCode != IfsReturnCodeNoErrorReported)
    {
        *pNumPackets = 0;
        return ifsReturnCode;
    }

    //printf("CurClock is %s\n", IfsToSecs(*pCurClock));

    return IfsReturnCodeNoErrorReported;
}

IfsReturnCode IfsReadPicture // Must call IfsSeekToTime() before calling this function
(IfsHandle ifsHandle, // Input
        IfsPcr ifsPcr, // Input
        IfsPts ifsPts, // Input
        IfsReadType ifsReadType, // Input
        NumPackets * pNumPackets, // Output
        IfsPacket ** ppData, // Output
        NumPackets * pStartPacket // Output
)
{
    if (pNumPackets != NULL)
        *pNumPackets = 0;
    if (ppData != NULL)
        *ppData = NULL;

    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (pNumPackets == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: pNumPackets == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }
    if (ppData == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ppData == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    IfsReturnCode ifsReturnCode = IfsReturnCodeNoErrorReported;

    g_static_mutex_lock(&(ifsHandle->mutex));
    IfsClock location = ifsHandle->entry.when; // nanoseconds

    IfsClock nextDiff = IFS_UNDEFINED_CLOCK;
    IfsClock prevDiff = IFS_UNDEFINED_CLOCK;
    IfsClock bestDiff = IFS_UNDEFINED_CLOCK;

    NumEntries nextEntry = ifsHandle->entryNum;
    NumEntries prevEntry = ifsHandle->entryNum;
    NumEntries bestEntry = IFS_UNDEFINED_ENTRY;

    NumPackets nextWhere = IFS_UNDEFINED_PACKET;
    NumPackets prevWhere = IFS_UNDEFINED_PACKET;
    NumPackets bestWhere = IFS_UNDEFINED_PACKET;

    FileNumber nextFile = ifsHandle->curFileNumber;
    FileNumber prevFile = ifsHandle->curFileNumber;
    FileNumber bestFile = IFS_UNDEFINED_FILENUMBER;

    struct stat statBuffer;

    (void) ifsPcr;
    (void) ifsPts;

    // Start by figuring out what file to use to find next I frame
    // it could be current, next or previous file
    if (ifsReadType != IfsReadTypePrevious) // scan forward for the next I frame
    {
        // Look forward for start of seq hdr which indicates start of next I frame
        while (IfsTrue)
        {
            // Read entries in current file until end of file reached
            while (nextEntry <= ifsHandle->maxEntry)
            {
                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, nextEntry);
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                {                     
                    g_static_mutex_unlock(&(ifsHandle->mutex));
                    return ifsReturnCode;
                }
                if (ifsHandle->entry.what & IfsIndexStartSeqHeader)
                {
                    // Found the starting location!
                    // Set next time difference to time of this entry and starting entry time when method was called
                    nextDiff = ifsHandle->entry.when - location; // nanoseconds

                    // Set next where (nbr of pkts) to this entry's where
                    // Virtual is position in overall stream vs Real which is position relative to this file
                    nextWhere = ifsHandle->entry.virtWhere;

#ifndef PRODUCTION_BUILD
                    char temp1[32], temp2[32], temp3[32]; // IfsToSecs only

                    RILOG_TRACE(
                            "\n IndexStartSeqHeader found in entry %3ld in file %2ld init %s now %s diff %s\n",
                            nextEntry, nextFile, IfsToSecs(location, temp1),
                            IfsToSecs(ifsHandle->entry.when, temp2), IfsToSecs(
                                    nextDiff, temp3));
#endif
                    break;
                }

                // Go on to next entry
                nextEntry++;
            }

            // If start of IFrame/seq hdr found, skip opening next file
            if (nextDiff != IFS_UNDEFINED_CLOCK)
                break;

            // If at end of files, not another file to open, skip opening next file
            if (nextFile == ifsHandle->endFileNumber)
                break;

            // Did not find start of IFrame/seq hdr, open next file
            g_static_mutex_unlock(&(ifsHandle->mutex));
            ifsReturnCode = IfsOpenActualFiles(ifsHandle, ++nextFile, "rb+");
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                return ifsReturnCode;

            g_static_mutex_lock(&(ifsHandle->mutex));

            if (stat(ifsHandle->ndex, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                g_static_mutex_unlock(&(ifsHandle->mutex));
                return IfsReturnCodeStatError;
            }

            // Reset these two vars so above while loop will restart with new file
            ifsHandle->maxEntry = (size_t) statBuffer.st_size
                    / sizeof(IfsIndexEntry) - 1;
            nextEntry = 0;
        }
    }

    // If current file has changed, open prev file which will be "old" current file
    if (ifsHandle->curFileNumber != prevFile)
    {
        g_static_mutex_unlock(&(ifsHandle->mutex));
        ifsReturnCode = IfsOpenActualFiles(ifsHandle, prevFile, "rb+");
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            return ifsReturnCode;

        g_static_mutex_lock(&(ifsHandle->mutex));
        if (stat(ifsHandle->ndex, &statBuffer))
        {
            RILOG_ERROR(
                    "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->ndex, errno, __LINE__, __FILE__);
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return IfsReturnCodeStatError;
        }
        ifsHandle->maxEntry = (size_t) statBuffer.st_size
                / sizeof(IfsIndexEntry) - 1;
    }

    if (ifsReadType != IfsReadTypeNext) // scan backward for the prev I frame
    {
        // Look backward for start of seq hdr which indicates start of previous I frame
        while (IfsTrue)
        {
            // Read entries in current file until beginning of file reached
            while (prevEntry > 0)
            {
                ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, prevEntry - 1);
                if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                {
                    g_static_mutex_unlock(&(ifsHandle->mutex));
                    return ifsReturnCode;
                }
                if (ifsHandle->entry.what & IfsIndexStartSeqHeader)
                {
                    // Found the starting location!
                    // Set prev time difference to starting entry time when method was called and time of this entry
                    prevDiff = location - ifsHandle->entry.when; // nanoseconds

                    // Set prev where (nbr of pkts) to this entry's where
                    // Virtual is position in overall stream vs Real which is position relative to this file
                    prevWhere = ifsHandle->entry.virtWhere;

                    // ??? Why are we decrementing this here???
                    prevEntry--;

#ifndef PRODUCTION_BUILD
                    char temp1[32], temp2[32], temp3[32]; // IfsToSecs only

                    RILOG_TRACE(
                            " IndexStartSeqHeader found in entry %3ld in file %2ld init %s now %s diff %s\n",
                            prevEntry, prevFile, IfsToSecs(location, temp1),
                            IfsToSecs(ifsHandle->entry.when, temp2), IfsToSecs(
                                    prevDiff, temp3));
#endif
                    break;
                }
                // Go on to previous entry
                prevEntry--;
            }

            // If start of IFrame found, skip opening next file
            if (prevDiff != IFS_UNDEFINED_CLOCK)
                break;

            // If at first file, not another file to open, skip opening previous file
            if (prevFile == ifsHandle->begFileNumber)
                break;

            // Did not find start of I Frame, open prev file
            g_static_mutex_unlock(&(ifsHandle->mutex));
            ifsReturnCode = IfsOpenActualFiles(ifsHandle, --prevFile, "rb+");
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                return ifsReturnCode;

            g_static_mutex_lock(&(ifsHandle->mutex));

            if (stat(ifsHandle->ndex, &statBuffer))
            {
                RILOG_ERROR(
                        "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                        ifsHandle->ndex, errno, __LINE__, __FILE__);
                g_static_mutex_unlock(&(ifsHandle->mutex));
                return IfsReturnCodeStatError;
            }

            // Reset these two vars so above while loop will restart with new file
            ifsHandle->maxEntry = (size_t) statBuffer.st_size
                    / sizeof(IfsIndexEntry) - 1;
            prevEntry = ifsHandle->maxEntry + 1;
        }
    }

    // Based on if reading nearest, previous or next, set "best" which is
    // the most likely spot (file, entry and packet) to find a I-Frame
    // from this call's starting entry
    switch (ifsReadType)
    {
    case IfsReadTypeNext:
        // Going forward so use next
        bestDiff = nextDiff;
        bestEntry = nextEntry;
        bestWhere = nextWhere;
        bestFile = nextFile;
        break;

    case IfsReadTypePrevious:
        // Going backward so use prev
        bestDiff = prevDiff;
        bestEntry = prevEntry;
        bestWhere = prevWhere;
        bestFile = prevFile;
        break;

    case IfsReadTypeNearest:
        // Want nearest but next is undefined, so use prev
        if (nextDiff == IFS_UNDEFINED_CLOCK)
        {
            bestDiff = prevDiff;
            bestEntry = prevEntry;
            bestWhere = prevWhere;
            bestFile = prevFile;
        }
        // Want nearest but prev is undefined, so use next
        else if (prevDiff == IFS_UNDEFINED_CLOCK)
        {
            bestDiff = nextDiff;
            bestEntry = nextEntry;
            bestWhere = nextWhere;
            bestFile = nextFile;
        }
        // Both prev & next defined, look at differences b/w next and prev
        // to determine which one is closer
        // Use prev because it is closer than next
        else if (prevDiff < nextDiff)
        {
            bestDiff = prevDiff;
            bestEntry = prevEntry;
            bestWhere = prevWhere;
            bestFile = prevFile;
        }
        // Use next because it is closer than prev
        else
        {
            bestDiff = nextDiff;
            bestEntry = nextEntry;
            bestWhere = nextWhere;
            bestFile = nextFile;
        }
        break;
    }

    // Make sure there is a "best" available
    if (bestDiff == IFS_UNDEFINED_CLOCK)
    {
        RILOG_WARN("IfsReturnCodeIframeNotFound in line %d of %s\n", __LINE__,
                __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeIframeNotFound;
    }

    RILOG_TRACE(" Best entry is %ld in file %2ld\n", bestEntry, bestFile);

    // End up here when next file did not contain a complete IFrame,
    // looking in previous file for a complete IFrame
    TryAgain:

    // If best file is different than current file, open it
    if (ifsHandle->curFileNumber != bestFile)
    {
        g_static_mutex_unlock(&(ifsHandle->mutex));
        ifsReturnCode = IfsOpenActualFiles(ifsHandle, bestFile, "rb+");

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            return ifsReturnCode;

        g_static_mutex_lock(&(ifsHandle->mutex));

        if (stat(ifsHandle->ndex, &statBuffer))
        {
            RILOG_ERROR(
                    "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->ndex, errno, __LINE__, __FILE__);
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return IfsReturnCodeStatError;
        }
        ifsHandle->maxEntry = (size_t) statBuffer.st_size
                / sizeof(IfsIndexEntry) - 1;
    }

    // Initialize flag vars to indicate looking for start of I Frame and
    // end of I Frame has not yet been found
    IfsBoolean endOfIframeFound = IfsFalse;
    IfsBoolean lookingForIframe = IfsTrue;

    // Start with best, use next as looping var
    nextEntry = bestEntry;
    nextFile = bestFile;

    while (IfsTrue)
    {
        // Make sure there is another entry to read and don't go off end
        while (nextEntry <= ifsHandle->maxEntry)
        {
            ifsReturnCode = IfsReadNdexEntryAt(ifsHandle, nextEntry);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            {
                g_static_mutex_unlock(&(ifsHandle->mutex));
                return ifsReturnCode;
            }

            // Look at current entry's type
            switch (ifsHandle->entry.what & IfsIndexStartPicture)
            {
            // Current entry is start of I Frame
            case IfsIndexStartPicture0: // I
                RILOG_TRACE(
                        " IndexStartPicture I found in entry %3ld in file %2ld\n",
                        nextEntry, nextFile);
                // Found start of I Frame
                if (lookingForIframe)
                    // No longer looking for I frame since it has been found
                    lookingForIframe = IfsFalse;
                else
                    // Already found start of I Frame, found start of next I Frame
                    // so end of current I Frame has been found
                    endOfIframeFound = IfsTrue;
                break;

            // Current entry is start of P Frame
            case IfsIndexStartPicture1: // P
                RILOG_TRACE(
                        " IndexStartPicture P found in entry %3ld in file %2ld\n",
                        nextEntry, nextFile);
                // If found P frame prior to I frame, report error & return
                if (lookingForIframe)
                {
                    RILOG_WARN(
                            "IfsReturnCodeFoundPbeforeIframe in line %d of %s\n",
                            __LINE__, __FILE__);
                    g_static_mutex_unlock(&(ifsHandle->mutex));
                    return IfsReturnCodeFoundPbeforeIframe;
                }
                else
                    // Already found start of I Frame, found start of P Frame
                    // so end of current I Frame has been found
                    endOfIframeFound = IfsTrue;
                break;

            // Current entry is start of B Frame
            case IfsIndexStartPicture: // B
                RILOG_TRACE(
                        " IndexStartPicture B found in entry %3ld in file %2ld\n",
                        nextEntry, nextFile);
                // If found B frame prior to I frame, report error & return
                if (lookingForIframe)
                {
                    RILOG_WARN(
                            "IfsReturnCodeFoundBbeforeIframe in line %d of %s\n",
                            __LINE__, __FILE__);
                    g_static_mutex_unlock(&(ifsHandle->mutex));
                    return IfsReturnCodeFoundBbeforeIframe;
                }
                else
                    // Already found start of I Frame, found start of B Frame
                    // so end of current I Frame has been found
                    endOfIframeFound = IfsTrue;
                break;
            }

            // Get out of loop if end of I frame is found
            if (endOfIframeFound)
                break;

            // Still looking, go on to next entry
            nextEntry++;
        }

        // If end of frame was found, done exit loop
        if (endOfIframeFound)
            break;

        // Got to end of entries, exit loop since no more files to look in
        if (nextFile == ifsHandle->endFileNumber)
            break;

        // Open up next file to search
        g_static_mutex_unlock(&(ifsHandle->mutex));
        ifsReturnCode = IfsOpenActualFiles(ifsHandle, ++nextFile, "rb+");

        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
            return ifsReturnCode;

        g_static_mutex_lock(&(ifsHandle->mutex));

        if (stat(ifsHandle->ndex, &statBuffer))
        {
            RILOG_ERROR(
                    "IfsReturnCodeStatError: stat(%s) failed (%d) in line %d of %s\n",
                    ifsHandle->ndex, errno, __LINE__, __FILE__);
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return IfsReturnCodeStatError;
        }

        // Reset these two vars so above while loop will restart with new file
        ifsHandle->maxEntry = (size_t) statBuffer.st_size
                / sizeof(IfsIndexEntry) - 1;
        nextEntry = 0;
    }

    // If found complete I-Frame, get packets of I-Frame to return
    if (endOfIframeFound)
    {
        NumPackets need = ifsHandle->entry.virtWhere - bestWhere + 1;

        // Adjust by one to prevent getting one extra packet which contains start of P frame
        // for trick modes which just want i frame
        if (ifsReadType == IfsReadTypeNearest)
        {
            need--;
        }

        // Determine number of bytes which constitutes picture to be returned
        const size_t bytes = need * sizeof(IfsPacket);

        RILOG_TRACE(" Return data from entry %ld in file %2ld to entry %ld in file %2ld\n",
                bestEntry, bestFile, nextEntry, nextFile);
        RILOG_TRACE(" Return data from packet %ld to %ld (%ld packets, %d bytes)\n",
                bestWhere, ifsHandle->entry.virtWhere, need, bytes);

        // If pointer to start packet is not null, set it to point to best entry's packet position
        if (pStartPacket)
            *pStartPacket = bestWhere;

        // Allocate memory required based on calculated number of bytes
        *ppData = g_try_malloc(bytes);
        if (*ppData == NULL)
        {
            RILOG_CRIT(
                    "IfsReturnCodeMemAllocationError: *ppData == NULL in line %d of %s\n",
                    __LINE__, __FILE__);
            g_static_mutex_unlock(&(ifsHandle->mutex));
            return IfsReturnCodeMemAllocationError;
        }

        // Initialize counter of number of packets read, starting at best packet position
        NumPackets have = 0;
        while (IfsTrue)
        {
            // File seek to best packet position
            NumPackets got;

            g_static_mutex_unlock(&(ifsHandle->mutex));
            ifsReturnCode = IfsSeekToPacketImpl(ifsHandle, bestWhere, NULL);
            if (ifsReturnCode != IfsReturnCodeNoErrorReported)
                // Unable to go to that packet position, bailing out
                break;

            g_static_mutex_lock(&(ifsHandle->mutex));

            // Read number of packets still needed (??? could we be off by 1 here???)
            if ((got = fread(*ppData + have, sizeof(IfsPacket), need - have,
                    ifsHandle->pMpeg)) == 0)
            {
                RILOG_ERROR(
                        "IfsReturnCodeFileReadingError: fread(%p, %d, %ld, %s) in line %d of %s\n",
                        *ppData, sizeof(IfsPacket), need, ifsHandle->mpeg,
                        __LINE__, __FILE__);
                ifsReturnCode = IfsReturnCodeFileReadingError;
                break;
            }

            // Increment number of packets read so far based on number which was just read
            have += got;

            // Have read all packets that are needed?
            if (have == need)
                break;

            // Unable to get all needed packets when read starts at this packet position,
            // Need to increment packet position and read again
            // How can we be sure we are not crossing file or entry boundary????
            RILOG_TRACE(" Crossing file boundary, still need %ld packets\n",
                    need - have);
            bestWhere += got;
        }

        // If bailed out of loop due to unsuccessful read, free allocated memory and return
        if (ifsReturnCode != IfsReturnCodeNoErrorReported)
        {
            if (*ppData)
            {
                g_free(*ppData);
                *ppData = NULL;
            }

            g_static_mutex_unlock(&(ifsHandle->mutex));
            return ifsReturnCode;
        }

        // Return number of packet which have been read
        *pNumPackets = have;

        // Update current location so any subsequent reads will be correctly tracked
        ifsHandle->realLoc += *pNumPackets;
        ifsHandle->virtLoc += *pNumPackets;
    }
    // Out of loop but never found start of I-Frame, log and return error
    else if (lookingForIframe)
    {
        RILOG_WARN("IfsReturnCodeIframeStartNotFound in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeIframeStartNotFound;
    }
    // End of IFrame not found but found start of IFrame, got here because got to end of files
    else
    {
        // If start of IFrame was found but not end of IFrame, look in previous file
        // for a complete IFrame
        if ((ifsReadType == IfsReadTypeNearest) && // Should not fail if a previous I frame was found
                (prevDiff != IFS_UNDEFINED_CLOCK) && // A previous I frame was found
                (bestDiff == nextDiff)) // Just got done trying the next I frame and if failed
        {
            bestDiff = prevDiff; // Setup to try the previous I frame
            bestEntry = prevEntry; //
            bestWhere = prevWhere; //
            bestFile = prevFile; //

            prevDiff = IFS_UNDEFINED_CLOCK; // This will prevent an infinite loop

            RILOG_INFO(
                    " Next entry failed, try the previous entry (%ld in file %2ld)\n",
                    bestEntry, bestFile);

            goto TryAgain;
        }

        RILOG_WARN("IfsReturnCodeIframeEndNotFound in line %d of %s\n",
                __LINE__, __FILE__);
        g_static_mutex_unlock(&(ifsHandle->mutex));
        return IfsReturnCodeIframeEndNotFound;
    }

    g_static_mutex_unlock(&(ifsHandle->mutex));
    return IfsReturnCodeNoErrorReported;
}

IfsReturnCode IfsReadNearestPicture // Must call IfsSeekToTime() before calling this function
(IfsHandle ifsHandle, // Input
        IfsPcr ifsPcr, // Input
        IfsPts ifsPts, // Input
        NumPackets * pNumPackets, // Output
        IfsPacket ** ppData // Output
)
{
    return IfsReadPicture(ifsHandle, ifsPcr, ifsPts, IfsReadTypeNearest,
            pNumPackets, ppData, NULL);
}

IfsReturnCode IfsReadNextPicture // Must call IfsSeekToTime() before calling this function
(IfsHandle ifsHandle, // Input
        IfsPcr ifsPcr, // Input
        IfsPts ifsPts, // Input
        NumPackets * pNumPackets, // Output
        IfsPacket ** ppData // Output
)
{
    return IfsReadPicture(ifsHandle, ifsPcr, ifsPts, IfsReadTypeNext,
            pNumPackets, ppData, NULL);
}

IfsReturnCode IfsReadPreviousPicture // Must call IfsSeekToTime() before calling this function
(IfsHandle ifsHandle, // Input
        IfsPcr ifsPcr, // Input
        IfsPts ifsPts, // Input
        NumPackets * pNumPackets, // Output
        IfsPacket ** ppData // Output
)
{
    return IfsReadPicture(ifsHandle, ifsPcr, ifsPts, IfsReadTypePrevious,
            pNumPackets, ppData, NULL);
}

IfsReturnCode IfsClose(IfsHandle ifsHandle // Input
)
{
    if (ifsHandle == NULL)
    {
        RILOG_ERROR(
                "IfsReturnCodeBadInputParameter: ifsHandle == NULL in line %d of %s\n",
                __LINE__, __FILE__);
        return IfsReturnCodeBadInputParameter;
    }

    g_static_mutex_lock(&(ifsHandle->mutex));

    // If this is a writer, remove it from list
    if (!ifsHandle->isReading)
    {
        // Remove this writer from the list
        writerListRemove(ifsHandle);
    }

    if (ifsHandle->path)
        g_free(ifsHandle->path); // g_try_malloc in IfsOpenImpl()
    if (ifsHandle->name)
        g_free(ifsHandle->name); // g_try_malloc in IfsOpenImpl()
    if (ifsHandle->both)
        g_free(ifsHandle->both); // g_try_malloc in IfsOpenImpl()
    if (ifsHandle->mpeg)
        g_free(ifsHandle->mpeg); // g_try_malloc in GenerateFileNames()
    if (ifsHandle->ndex)
        g_free(ifsHandle->ndex); // g_try_malloc in GenerateFileNames()
    if (ifsHandle->pMpeg)
        fclose(ifsHandle->pMpeg); // open in IfsOpenActualFiles()
    if (ifsHandle->pNdex)
        fclose(ifsHandle->pNdex); // open in IfsOpenActualFiles()

    g_static_mutex_unlock(&(ifsHandle->mutex));
    g_free(ifsHandle); // g_try_malloc0 in IfsOpenImpl()

    return IfsReturnCodeNoErrorReported;
}

IfsBoolean IfsHasWriter(IfsHandle ifsHandle)
{
    if ((ifsHandle->path == NULL) || (ifsHandle->name == NULL))
    {
        RILOG_WARN("supplied handle had null name and or path, unable to check for writer\n");
        return IfsFalse;
    }

    g_static_mutex_lock(&writerListMutex);

    IfsHandle curHandle = NULL;
    int i = 0;
    for (i = 0; i < MAX_WRITER_LIST_CNT; i++)
    {
        if (writerListHandles[i] != NULL)
        {
            curHandle = writerListHandles[i];
            if ((curHandle->path != NULL) && (curHandle->name != NULL))
            {
                if (strcmp(curHandle->path, ifsHandle->path) == 0)
                {
                    if (strcmp(curHandle->name, ifsHandle->name) == 0)
                    {
                        // Found writer in list, return true
                        //RILOG_INFO("found active writer with path: %s, name: %s\n",
                        //        curHandle->path, curHandle->name);
                        g_static_mutex_unlock(&writerListMutex);
                        return IfsTrue;
                    }
                }
            }
        }
    }
    g_static_mutex_unlock(&writerListMutex);

    return IfsFalse;
}

static void writerListAdd(IfsHandle ifsHandle)
{
    g_static_mutex_lock(&writerListMutex);

    // Find the next available slot to stick this writer
    int idx = -1;
    int i = 0;
    for (i = 0; i < MAX_WRITER_LIST_CNT; i++)
    {
        if (writerListHandles[i] == NULL)
        {
            writerListHandles[i] = ifsHandle;
            idx = i;
            break;
        }
    }
    if (idx == -1)
    {
        RILOG_WARN("No more slots available, current cnt: %d\n", writerListCnt);
    }
    else
    {
        writerListCnt++;
    }
    g_static_mutex_unlock(&writerListMutex);
}

static void writerListRemove(IfsHandle ifsHandle)
{
    g_static_mutex_lock(&writerListMutex);

    // Find this writer in the list
    int idx = -1;
    int i = 0;
    for (i = 0; i < MAX_WRITER_LIST_CNT; i++)
    {
        if (writerListHandles[i] == ifsHandle)
        {
            writerListHandles[i] = NULL;
            idx = i;
            break;
        }
    }
    if (idx == -1)
    {
        RILOG_WARN("Unable to find writer, current cnt: %d\n", writerListCnt);
    }
    else
    {
        writerListCnt--;
    }
    g_static_mutex_unlock(&writerListMutex);
}
