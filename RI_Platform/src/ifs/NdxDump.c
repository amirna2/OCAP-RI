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

#include <stdlib.h>
#include <stdio.h>
#include <time.h>
#include <string.h>
#include <errno.h>

#include <glib.h>

#include "IfsImpl.h"
#include "IfsParse.h"
#include "RemapImpl.h"

#ifndef MAX_PATH
#define MAX_PATH 260
#endif

static char inputFile[MAX_PATH];
static RemapPid thePmtPid = REMAP_UNDEFINED_PID;
static RemapPid theVideoPid = REMAP_UNDEFINED_PID;
static IfsHandle ifsHandle;
static IfsBoolean isAnMpegFile = IfsFalse;

void DumpBadCodes(void);
void DumpBadCodes(void)
{
    printf("                                        top  repeat  Reasons\n");
    printf("BAD  progressive  picture  progressive field  first   it is \n");
    printf("case   sequence  structure    frame    first  field    BAD  \n");
    printf("---- ----------- --------- ----------- ----- ------  -------\n");
    printf("   0      0          0          0        0      0    3      \n");
    printf("   1      0          0          0        0      1    3, 7   \n");
    printf("   2      0          0          0        1      0    3      \n");
    printf("   3      0          0          0        1      1    3, 7   \n");
    printf("   4      0          0          1        0      0    3      \n");
    printf("   5      0          0          1        0      1    3      \n");
    printf("   6      0          0          1        1      0    3      \n");
    printf("   7      0          0          1        1      1    3      \n");
    printf("   9      0          1          0        0      1    6, 7   \n");
    printf("  10      0          1          0        1      0    4      \n");
    printf("  11      0          1          0        1      1    4, 6, 7\n");
    printf("  12      0          1          1        0      0    2      \n");
    printf("  13      0          1          1        0      1    2, 6   \n");
    printf("  14      0          1          1        1      0    2, 4   \n");
    printf("  15      0          1          1        1      1    2, 4, 6\n");
    printf("  17      0          2          0        0      1    6, 7   \n");
    printf("  18      0          2          0        1      0    4      \n");
    printf("  19      0          2          0        1      1    4, 6, 7\n");
    printf("  20      0          2          1        0      0    2      \n");
    printf("  21      0          2          1        0      1    2, 6   \n");
    printf("  22      0          2          1        1      0    2, 4   \n");
    printf("  23      0          2          1        1      1    2, 4, 6\n");
    printf("  25      0          3          0        0      1    7      \n");
    printf("  27      0          3          0        1      1    7      \n");
    printf("  32      1          0          0        0      0    1, 3   \n");
    printf("  33      1          0          0        0      1    1, 3   \n");
    printf("  34      1          0          0        1      0    1, 3   \n");
    printf("  35      1          0          0        1      1    1, 3   \n");
    printf("  36      1          0          1        0      0    1, 3   \n");
    printf("  37      1          0          1        0      1    1, 3   \n");
    printf("  38      1          0          1        1      0    1, 3   \n");
    printf("  39      1          0          1        1      1    1, 3   \n");
    printf("  40      1          1          0        0      0    1      \n");
    printf("  41      1          1          0        0      1    1, 6   \n");
    printf("  42      1          1          0        1      0    1      \n");
    printf("  43      1          1          0        1      1    1, 6   \n");
    printf("  44      1          1          1        0      0    1      \n");
    printf("  45      1          1          1        0      1    1, 6   \n");
    printf("  46      1          1          1        1      0    1      \n");
    printf("  47      1          1          1        1      1    1, 6   \n");
    printf("  48      1          2          0        0      0    1      \n");
    printf("  49      1          2          0        0      1    1, 6   \n");
    printf("  50      1          2          0        1      0    1      \n");
    printf("  51      1          2          0        1      1    1, 6   \n");
    printf("  52      1          2          1        0      0    1      \n");
    printf("  53      1          2          1        0      1    1, 6   \n");
    printf("  54      1          2          1        1      0    1      \n");
    printf("  55      1          2          1        1      1    1, 6   \n");
    printf("  56      1          3          0        0      0    1      \n");
    printf("  57      1          3          0        0      1    1      \n");
    printf("  58      1          3          0        1      0    1      \n");
    printf("  59      1          3          0        1      1    1      \n");
    printf("  62      1          3          1        1      0    5      \n");
    printf("---- ----------- --------- ----------- ----- ------  -------\n");
    printf("\n");
    printf("Reasons:\n");
    printf("\n");
    printf(" 1   When progressive_sequence is 1 the coded video sequence\n");
    printf("     contains only progressive frame pictures\n");
    printf("\n");
    printf(" 2   When progressive_sequence is 0 field pictures must be\n");
    printf("     interlaced\n");
    printf("\n");
    printf(" 3   Reserved picture_structure value\n");
    printf("\n");
    printf(" 4   If progressive_sequence is 0 then top_field_first must\n");
    printf("     be 0 for all field encoded pictures\n");
    printf("\n");
    printf(" 5   If progressive_sequence is 1 then top_field_first 1,\n");
    printf("     repeat_first_field 0 is not a legal combination\n");
    printf("\n");
    printf(" 6   repeat_first_field shall be 0 for all field encoded\n");
    printf("     pictures\n");
    printf("\n");
    printf(" 7   If progressive_sequence and progressive_frame are 0 then\n");
    printf("     repeat_first_frame shall be 0\n");
}

static IfsBoolean ProcessArguments(int argc, char *argv[]) // returns IfsTrue = failure, IfsFalse = success
{
    const time_t now = time(NULL);

    printf("\nNdxDump.exe, version %d, at %ld %s\n", INTF_RELEASE_VERSION, now,
            asctime(gmtime(&now)));

    if (argc == 2)
    {
        printf("Processing two args...\n");
        strcpy(inputFile, argv[1]);
        if (strstr(argv[1], ".mpg"))
        {
            printf("Opening mpeg file...\n");
            FILE * const pInFile = fopen(inputFile, "rb");

            size_t i = 2;
            RemapHandle remapHandle;
            NumPackets nextNumPackets;
            RemapPacket packets[2][16];
            RemapPacket * pointers[2][16];
            NumPackets prevNumPackets;
            RemapPacket ** ppPrevPointers;

            if (pInFile == NULL)
            {
                printf("Unable to open input file: %s\n", inputFile);
                return IfsTrue;
            }

            isAnMpegFile = IfsTrue;

            if (RemapOpen(&remapHandle) != RemapReturnCodeNoErrorReported)
            {
                printf("Problems remapping mpeg file\n");
                return IfsTrue;
            }

            while ((nextNumPackets = fread(&packets[i & 1][0],
                    IFS_TRANSPORT_PACKET_SIZE, 16, pInFile)) != 0)
            {
                if (RemapAndFilter(remapHandle, NULL, nextNumPackets,
                        &packets[i & 1][0], &pointers[i & 1][0],
                        &prevNumPackets, &ppPrevPointers)
                        != RemapReturnCodeNoErrorReported)
                {
                    printf("Problems remapping and filtering mpeg packets\n");
                    return IfsTrue;
                }
                i++;
            }
            if (RemapAndFilter(remapHandle, NULL, 0, NULL, NULL,
                    &prevNumPackets, &ppPrevPointers)
                    != RemapReturnCodeNoErrorReported)
            {
                printf("Problems remapping and filtering prev mpeg file\n");
                return IfsTrue;
            }
            if (remapHandle->nextPat == 0)
                printf("Could not find a valid PAT in the file\n");
            else if (remapHandle->nextPat == 1)
                printf("PMT PID (%4d) found\n", thePmtPid
                        = remapHandle->pSavePat[0].pid);
            else
            {
                int selection;
                printf(
                        "This is an MPTS, please select the desired PMT PID:\n\n");
                for (i = 0; i < remapHandle->nextPat; i++)
                    printf(
                            "   Selection %d will select program %d, PMT PID %d (0x%X)\n",
                            i, remapHandle->pSavePat[i].prog,
                            remapHandle->pSavePat[i].pid,
                            remapHandle->pSavePat[i].pid);
                printf("\n   Selection ");
                if (scanf("%d", &selection) != 1)
                {
                    printf("ERROR -- scanf returned wrong number of parsed items!\n");
                }
                else
                {
                    thePmtPid = remapHandle->pSavePat[selection].pid; 
                    printf("PMT PID set to %4d\n", thePmtPid);
                }
            }

            if (RemapClose(remapHandle, &prevNumPackets, &ppPrevPointers)
                    != RemapReturnCodeNoErrorReported)
            {
                printf("Problems closing file after remapping\n");
                return IfsTrue;
            }

            if (thePmtPid != REMAP_UNDEFINED_PID)
            {
                rewind(pInFile);

                if (RemapOpen(&remapHandle) != RemapReturnCodeNoErrorReported)
                {
                    printf("Remap after rewind failed\n");
                    return IfsTrue;
                }
                if (RemapPmts(remapHandle, thePmtPid)
                        != RemapReturnCodeNoErrorReported)
                {
                    printf("Remap of PMTs failed after rewind\n");
                    return IfsTrue;
                }
                while ((nextNumPackets = fread(&packets[i & 1][0],
                        IFS_TRANSPORT_PACKET_SIZE, 16, pInFile)) != 0)
                {
                    if (RemapAndFilter(remapHandle, NULL, nextNumPackets,
                            &packets[i & 1][0], &pointers[i & 1][0],
                            &prevNumPackets, &ppPrevPointers)
                            != RemapReturnCodeNoErrorReported)
                    {
                        printf("Remap and filtering of rewind packets failed\n");
                        return IfsTrue;
                    }
                    i++;
                }
                if (RemapAndFilter(remapHandle, NULL, 0, NULL, NULL,
                        &prevNumPackets, &ppPrevPointers)
                        != RemapReturnCodeNoErrorReported)
                {
                    printf("Remap and filter of rewind file failed\n");
                    return IfsTrue;
                }

                theVideoPid = remapHandle->videoPid;

                if (RemapClose(remapHandle, &prevNumPackets, &ppPrevPointers)
                        != RemapReturnCodeNoErrorReported)
                {
                    printf("Problems closing file after rewind remap\n");
                    return IfsTrue;
                }
                if (theVideoPid == REMAP_UNDEFINED_PID)
                    printf("Could not find a valid PMT in the file\n");
                else
                {
                    NumPackets numPackets;
                    IfsPacket ifsPacket;

                    printf("Vid PID (%4d) found\n", theVideoPid);

                    rewind(pInFile);

                    IfsSetMode(IfsIndexDumpModeOff, IfsIndexerSettingDefault);

                    if (IfsOpenWriter(".", NULL, 0, &ifsHandle)
                            != IfsReturnCodeNoErrorReported)
                    {
                        printf("Problems opening ifs writer\n");
                        return IfsTrue;
                    }
                    if (IfsStart(ifsHandle, theVideoPid, 0)
                            != IfsReturnCodeNoErrorReported)
                    {
                        printf("Problems starting ifs writer\n");
                        return IfsTrue;
                    }
                    i = 0;

                    // Pretend each packet contains 1 second of data...
                    while ((numPackets = fread(&ifsPacket,
                            IFS_TRANSPORT_PACKET_SIZE, 1, pInFile)) != 0)
                        if (IfsWrite(ifsHandle, ++i * NSEC_PER_SEC, numPackets,
                                &ifsPacket) != IfsReturnCodeNoErrorReported)
                        {
                            printf("Problems writing packet\n");
                            return IfsTrue;
                        }
                    fclose(pInFile);
                    if (IfsSeekToPacket(ifsHandle, 0, NULL)
                            != IfsReturnCodeNoErrorReported)
                    {
                        printf("Problems seeking to packet\n");
                        return IfsTrue;
                    }
                    if (fread(&ifsHandle->entry, 1, sizeof(IfsIndexEntry),
                            ifsHandle->pNdex) != sizeof(IfsIndexEntry))
                    {
                        printf("Problems reading entry\n");
                        return IfsTrue;
                    }
                    printf("Successfully opened file...\n");
                    return IfsFalse;
                }
            }

            fclose(pInFile);
        }
        else if (strstr(argv[1], ".ndx"))
        {
            printf("Processing index file...");
            // Simulate (part of) an IfsOpen command...

            ifsHandle = g_try_malloc0(sizeof(IfsHandleImpl));
            if (ifsHandle == NULL)
            {
                printf("Could not allocate memory\n");
                return IfsTrue;
            }

            ifsHandle->pNdex = fopen(inputFile, "rb");

            if (ifsHandle->pNdex)
            {
                printf("The input file name and path will be '%s'\n\n",
                        inputFile);
                if (fread(&ifsHandle->entry, 1, sizeof(IfsIndexEntry),
                        ifsHandle->pNdex) == sizeof(IfsIndexEntry))
                {
                    return IfsFalse;
                }
                else
                    printf("Could not read '%s', errno %d\n\n", inputFile,
                            errno);
            }
            else
                printf("Could not open '%s', errno %d\n\n", inputFile, errno);
        }
        else
        {
            printf("Processing one arg...");
            IfsReturnCode ifsReturnCode;
            size_t length = strlen(inputFile);
            while (length && (inputFile[length] != '\\') && (inputFile[length]
                    != '/'))
                length--;
            if (length)
            {
                inputFile[length] = 0;
                ifsReturnCode = IfsOpenReader(inputFile,
                        &inputFile[length + 1], &ifsHandle);
            }
            else
                ifsReturnCode = IfsOpenReader(".", inputFile, &ifsHandle);

            if (ifsReturnCode == IfsReturnCodeNoErrorReported)
            {
                //IfsDumpHandle(ifsHandle);
                printf("Opened file for processing...");
                return IfsFalse;

            }
            else
                printf("IfsPathNameInfo failed with error \"%s\"\n\n",
                        IfsReturnCodeToString(ifsReturnCode));
        }
    }
    else
    {
        //      DumpBadCodes();
        printf("Usage:  NdxDump <input>\n");
        printf("        Where <input> is one of the following three cases:\n");
        printf("          1) an IFS index file such as 00000000.ndx\n");
        printf("          2) an IFS directory containing .mpg and .ndx files\n");
        printf("             such as 4B59D994_0029\n");
        printf("          3) an MPEG file such as plain.mpg\n");
    }

    printf("Returning true...");
    return IfsTrue;
}

static unsigned long indexCounts[64] =
{ 0 };
static unsigned long iPictureCount = 0;
static unsigned long pPictureCount = 0;
static unsigned long bPictureCount = 0;
static unsigned long iSequence = 0;
static unsigned long pSequence = 0;
static unsigned long pictCodeCounts[64] =
{ 0 };
static unsigned long videoNonePts = 0;
static unsigned long videoWithPts = 0;

static void CountIndexes(IfsIndex ifsIndex)
{
    // Special cases:
    //
    //  IfsIndexStartPicture
    //
    //  16  0000000000018000 = PdeBit   StartIpicture
    //  17  0000000000028000 = PdeBit   StartPpicture
    //  18  0000000000038000 = PdeBit   StartBpicture
    //
    //  IfsIndexExtSequence
    //      IfsIndexInfoProgSeq
    //
    //  25  0000000002808000 = PdeBit   StartExtension  ExtSequence   i
    //  26  0400000002808000 = PdeBit   StartExtension  ExtSequence   p
    //
    //  IfsIndexExtPictCode
    //      IfsIndexInfoProgFrame
    //      IfsIndexInfoStructure
    //      IfsIndexInfoProgRep
    //
    //  32  0000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  0
    //  33  0800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  1
    //  34  1000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  2
    //  35  1800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  3
    //  36  2000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  4
    //  37  2800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  5
    //  38  3000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  6
    //  39  3800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  7
    //  40  4000000080808000 = PdeBit   StartExtension  ExtPictCode   iT
    //  41  4800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD  9
    //  42  5000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 10
    //  43  5800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 11
    //  44  6000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 12
    //  45  6800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 13
    //  46  7000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 14
    //  47  7800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 15
    //  48  8000000080808000 = PdeBit   StartExtension  ExtPictCode   iB
    //  49  8800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 17
    //  50  9000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 18
    //  51  9800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 19
    //  52  A000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 20
    //  53  A800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 21
    //  54  B000000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 22
    //  55  B800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 23
    //  56  C000000080808000 = PdeBit   StartExtension  ExtPictCode   iBT
    //  57  C800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 25
    //  58  D000000080808000 = PdeBit   StartExtension  ExtPictCode   iTB
    //  59  D800000080808000 = PdeBit   StartExtension  ExtPictCode   BAD 27
    //  60  E000000080808000 = PdeBit   StartExtension  ExtPictCode   pBT
    //  61  E800000080808000 = PdeBit   StartExtension  ExtPictCode   pBTB
    //  62  F000000080808000 = PdeBit   StartExtension  ExtPictCode   pTB
    //  63  F800000080808000 = PdeBit   StartExtension  ExtPictCode   pTBT
    //
    //  IfsIndexStartVideo
    //      IfsIndexInfoContainsPts
    //
    //  76  0000100000008000 = PdeBit   StartVideo
    //  77  0200100000008000 = PdeBit   StartVideo(0)
    //
    // Normal cases:
    //
    //   0  0000000000000100 = BadSync
    //   1  0000000000000200 = TpChange
    //   2  0000000000000400 = PusiBit
    //   3  0000000000000800 = TeiBit
    //
    //   4  0000000000001000 = CcError
    //   5  0000000000002000 = ScChange
    //   6  0000000000004000 = AfeBit
    //  15  0000000000008000 = PdeBit
    //
    //   7  0000000000004001 = AfeBit   AdaptAfeeBit
    //   8  0000000000004002 = AfeBit   AdaptTpdeBit
    //   9  0000000000004004 = AfeBit   AdaptSpeBit
    //  10  0000000000004008 = AfeBit   AdaptOpcreBit
    //
    //  11  0000000000004010 = AfeBit   AdaptPcreBit(0)
    //  12  0000000000004020 = AfeBit   AdaptEspChange
    //  13  0000000000004040 = AfeBit   AdaptRaiBit
    //  14  0000000000004080 = AfeBit   AdaptDiBit
    //
    //  19  0000000000048000 = PdeBit   StartUserData
    //  20  0000000000088000 = PdeBit   StartSeqHeader
    //
    //  21  0000000000108000 = PdeBit   StartSeqError
    //  22  0000000000208000 = PdeBit   StartSeqEnd
    //  23  0000000000408000 = PdeBit   StartGroup
    //  24  0000000001808000 = PdeBit   StartExtension  ExtReserved
    //  27  0000000004808000 = PdeBit   StartExtension  ExtDisplay
    //  28  0000000008808000 = PdeBit   StartExtension  ExtQuantMat
    //  29  0000000010808000 = PdeBit   StartExtension  ExtCopyright
    //  30  0000000020808000 = PdeBit   StartExtension  ExtScalable
    //  31  0000000040808000 = PdeBit   StartExtension  ExtPictOther
    //
    //  64  0000000100008000 = PdeBit   StartSlice
    //  65  0000000200008000 = PdeBit   StartReservedB0
    //  66  0000000400008000 = PdeBit   StartReservedB1
    //  67  0000000800008000 = PdeBit   StartReservedB6
    //
    //  68  0000001000008000 = PdeBit   StartMpegEnd
    //  69  0000002000008000 = PdeBit   StartPack
    //  70  0000004000008000 = PdeBit   StartSysHeader
    //  71  0000008000008000 = PdeBit   StartProgramMap
    //
    //  72  0000010000008000 = PdeBit   StartPrivate1
    //  73  0000020000008000 = PdeBit   StartPadding
    //  74  0000040000008000 = PdeBit   StartPrivate2
    //  75  0000080000008000 = PdeBit   StartAudio
    //
    //  78  0000200000008000 = PdeBit   StartEcm
    //  79  0000400000008000 = PdeBit   StartEmm
    //  80  0000800000008000 = PdeBit   StartDsmCc
    //
    //  81  0001000000008000 = PdeBit   Start13522
    //  82  0002000000008000 = PdeBit   StartItuTypeA
    //  83  0004000000008000 = PdeBit   StartItuTypeB
    //  84  0008000000008000 = PdeBit   StartItuTypeC
    //
    //  85  0010000000008000 = PdeBit   StartItuTypeD
    //  86  0020000000008000 = PdeBit   StartItuTypeE
    //  87  0040000000008000 = PdeBit   StartAncillary
    //  88  0080000000008000 = PdeBit   StartRes_FA_FE
    //
    //  89  0100000000008000 = PdeBit   StartDirectory

    int i;

    for (i = 0; i < 64; i++)
    {
        IfsIndex mask = ((IfsIndex) 1) << i;

        if (mask & IfsIndexStartPicture0)
        {
            switch (ifsIndex & IfsIndexStartPicture)
            {
            case IfsIndexStartPicture0:
                iPictureCount++;
                break;
            case IfsIndexStartPicture1:
                pPictureCount++;
                break;
            case IfsIndexStartPicture:
                bPictureCount++;
                break;
            }
        }
        else if (mask & IfsIndexExtSequence)
        {
            if (ifsIndex & IfsIndexExtSequence)
            {
                switch (ifsIndex & IfsIndexInfoProgSeq)
                {
                case 0:
                    iSequence++;
                    break;
                case IfsIndexInfoProgSeq:
                    pSequence++;
                    break;
                }
            }
        }
        else if (mask & IfsIndexExtPictCode)
        {
            if (ifsIndex & IfsIndexExtPictCode)
            {
                size_t code = ((ifsIndex & IfsIndexInfoProgSeq ? 32 : 0)
                        | (ifsIndex & IfsIndexInfoStructure1 ? 16 : 0)
                        | (ifsIndex & IfsIndexInfoStructure0 ? 8 : 0)
                        | (ifsIndex & IfsIndexInfoProgFrame ? 4 : 0)
                        | (ifsIndex & IfsIndexInfoTopFirst ? 2 : 0) | (ifsIndex
                        & IfsIndexInfoRepeatFirst ? 1 : 0));
                pictCodeCounts[code]++;
            }
        }
        else if (mask & IfsIndexStartVideo)
        {
            if (ifsIndex & IfsIndexStartVideo)
            {
                switch (ifsIndex & IfsIndexInfoContainsPts)
                {
                case 0:
                    videoNonePts++;
                    break;
                case IfsIndexInfoContainsPts:
                    videoWithPts++;
                    break;
                }
            }
        }
        else if (mask & (IfsIndexStartPicture1 | IfsIndexInfoProgSeq
                | IfsIndexInfoProgFrame | IfsIndexInfoStructure
                | IfsIndexInfoProgRep | IfsIndexInfoContainsPts))
        {
            // Do nothing
        }
        else if (mask & ifsIndex)
            indexCounts[i]++;
    }
}

static void DumpIndexes(void)
{
    int i;

    printf("Occurances  Event\n");
    printf("----------  -----\n");

    for (i = 0; i < 64; i++)
    {
        char temp[256]; // ParseWhat
        IfsHandleImpl tempHandleImpl;
        tempHandleImpl.entry.what = ((IfsIndex) 1) << i;

        if (tempHandleImpl.entry.what & IfsIndexStartPicture0)
        {
            if (iPictureCount)
            {
                tempHandleImpl.entry.what = IfsIndexStartPicture0;
                printf("%10ld  %s frame\n", iPictureCount, ParseWhat(
                        &tempHandleImpl, temp, IfsIndexDumpModeDef, IfsFalse));
            }
            if (pPictureCount)
            {
                tempHandleImpl.entry.what = IfsIndexStartPicture1;
                printf("%10ld  %s frame\n", pPictureCount, ParseWhat(
                        &tempHandleImpl, temp, IfsIndexDumpModeDef, IfsFalse));
            }
            if (bPictureCount)
            {
                tempHandleImpl.entry.what = IfsIndexStartPicture;
                printf("%10ld  %s frame\n", bPictureCount, ParseWhat(
                        &tempHandleImpl, temp, IfsIndexDumpModeDef, IfsFalse));
            }
        }
        else if (tempHandleImpl.entry.what & IfsIndexExtSequence)
        {
            if (iSequence)
            {
                printf("%10ld%s\n", iSequence, ParseWhat(&tempHandleImpl, temp,
                        IfsIndexDumpModeDef, IfsFalse));
            }
            if (pSequence)
            {
                tempHandleImpl.entry.what |= IfsIndexInfoProgSeq;
                printf("%10ld%s\n", pSequence, ParseWhat(&tempHandleImpl, temp,
                        IfsIndexDumpModeDef, IfsFalse));
            }
        }
        else if (tempHandleImpl.entry.what & IfsIndexExtPictCode)
        {
            IfsIndex j;

            for (j = 0; j < 64; j++)
            {
                if (pictCodeCounts[j])
                {
                    tempHandleImpl.entry.what = (IfsIndexExtPictCode
                            | (j & 32 ? IfsIndexInfoProgSeq : 0)
                            | (j & 16 ? IfsIndexInfoStructure1 : 0)
                            | (j & 8 ? IfsIndexInfoStructure0 : 0)
                            | (j & 4 ? IfsIndexInfoProgFrame : 0)
                            | (j & 2 ? IfsIndexInfoTopFirst : 0)
                            | (j & 1 ? IfsIndexInfoRepeatFirst : 0));

                    printf("%10ld%s\n", pictCodeCounts[j], ParseWhat(
                            &tempHandleImpl, temp, IfsIndexDumpModeDef,
                            IfsFalse));
                }
            }
        }
        else if (tempHandleImpl.entry.what & IfsIndexStartVideo)
        {
            if (videoNonePts)
            {
                printf("%10ld%s\n", videoNonePts, ParseWhat(&tempHandleImpl,
                        temp, IfsIndexDumpModeDef, IfsFalse));
            }
            if (videoWithPts)
            {
                tempHandleImpl.entry.what |= IfsIndexInfoContainsPts;
                printf("%10ld%s\n", videoWithPts, ParseWhat(&tempHandleImpl,
                        temp, IfsIndexDumpModeDef, IfsFalse));
            }
        }
        else if (tempHandleImpl.entry.what & (IfsIndexStartPicture1
                | IfsIndexInfoProgSeq | IfsIndexInfoProgFrame
                | IfsIndexInfoStructure | IfsIndexInfoProgRep
                | IfsIndexInfoContainsPts))
        {
            // Do nothing
        }
        else if (indexCounts[i])
            printf("%10ld%s\n", indexCounts[i], ParseWhat(&tempHandleImpl,
                    temp, IfsIndexDumpModeDef, IfsFalse));
    }
}

int main(int argc, char *argv[])
{
    IfsClock duration;
    time_t seconds;
    IfsIndexEntry firstEntry, lastEntry;
    NumEntries numEntries = 0;

    printf("processing cmd line args...\n");

    if (ProcessArguments(argc, argv))
        return 0;

    printf("starting analysis...\n");

    IfsSetMode(IfsIndexDumpModeDef, IfsIndexerSettingVerbose);

    firstEntry = ifsHandle->entry;

    while (1)
    {
        if (ifsHandle->ndex)
            printf("\n%s:\n\n", ifsHandle->ndex);

        if (isAnMpegFile)
        {
            printf(
                    "Packet(where)  Index Information (what happened at that time and location)\n");
            printf(
                    "-------------  -----------------------------------------------------------\n");
            do
            {
                char temp[256]; // ParseWhat
                CountIndexes(ifsHandle->entry.what);
                printf("%6ld/%6ld  %s\n", ifsHandle->entry.realWhere,
                        ifsHandle->entry.virtWhere, ParseWhat(ifsHandle, temp,
                                IfsIndexDumpModeDef, IfsFalse));
                numEntries++;

            } while (fread(&ifsHandle->entry, 1, sizeof(IfsIndexEntry),
                    ifsHandle->pNdex) == sizeof(IfsIndexEntry));

            printf(
                    "-------------  -----------------------------------------------------------\n");
            printf(
                    "Packet(where)  Index Information (what happened at that time and location)\n");
        }
        else
        {
            printf(
                    "Packet(where)  Date/TimeStamp(when)  Index Information (what happened at that time and location)\n");
            printf(
                    "-------------  --------------------  -----------------------------------------------------------\n");
            do
            {
                char temp1[32]; // IfsToSecs only
                char temp2[256]; // ParseWhat
                CountIndexes(ifsHandle->entry.what);
                printf("%6ld/%6ld  %s  %s\n", ifsHandle->entry.realWhere,
                        ifsHandle->entry.virtWhere, IfsToSecs(
                                ifsHandle->entry.when, temp1),
                        ParseWhat(ifsHandle, temp2, IfsIndexDumpModeDef,
                                IfsFalse));
                numEntries++;

            } while (fread(&ifsHandle->entry, 1, sizeof(IfsIndexEntry),
                    ifsHandle->pNdex) == sizeof(IfsIndexEntry));

            printf(
                    "-------------  --------------------  -----------------------------------------------------------\n");
            printf(
                    "Packet(where)  Date/TimeStamp(when)  Index Information (what happened at that time and location)\n");
        }

        if (!ifsHandle->curFileNumber)
            break;

        if (ifsHandle->curFileNumber >= ifsHandle->endFileNumber)
            break;

        if (IfsOpenActualFiles(ifsHandle, ifsHandle->curFileNumber + 1, "rb+"))
            break;

        if (fread(&ifsHandle->entry, 1, sizeof(IfsIndexEntry), ifsHandle->pNdex)
                != sizeof(IfsIndexEntry))
            break;
    }

    lastEntry = ifsHandle->entry;
    duration = lastEntry.when - firstEntry.when;

    printf("\n");
    DumpIndexes();
    printf("\n");

    if (isAnMpegFile)
    {
        printf("First packet indexed %7ld/%7ld\n", firstEntry.realWhere,
                firstEntry.virtWhere);
        printf("Last packet indexed  %7ld/%7ld\n", lastEntry.realWhere,
                lastEntry.virtWhere);
        printf("Number of entries %10ld\n", numEntries);
        printf("Rate of indexing  %20.9f packets/entry\n",
                ((lastEntry.virtWhere - firstEntry.virtWhere) * 1.0)
                        / (numEntries * 1.0));
        printf("I frame rate      %20.9f frames/I-frame\n",
                (iPictureCount ? ((iPictureCount + pPictureCount
                        + bPictureCount) * 1.0) / (iPictureCount * 1.0) : 0.0));
    }
    else
    {
        char temp[32]; // IfsToSecs only
        seconds = firstEntry.when / NSEC_PER_SEC;
        printf("Date/Time Start   %s %s", IfsToSecs(firstEntry.when, temp),
                asctime(gmtime(&seconds)));
        seconds = lastEntry.when / NSEC_PER_SEC;
        printf("Date/Time Ended   %s %s", IfsToSecs(lastEntry.when, temp),
                asctime(gmtime(&seconds)));
        printf("Recording Lasted  %s seconds\n", IfsToSecs(duration, temp));
        printf("First packet indexed %7ld/%7ld\n", firstEntry.realWhere,
                firstEntry.virtWhere);
        printf("Last packet indexed  %7ld/%7ld\n", lastEntry.realWhere,
                lastEntry.virtWhere);
        printf("Number of entries %10ld\n", numEntries);
        printf("Rate of indexing  %20.9f entries/second\n", (numEntries * 1.0)
                / ((duration * 1.0) / (NSEC_PER_SEC * 1.0)));
        printf("                  %20.9f packets/entry\n",
                ((lastEntry.virtWhere - firstEntry.virtWhere) * 1.0)
                        / (numEntries * 1.0));
        printf("I frame rate      %20.9f frames/I-frame\n",
                (iPictureCount ? ((iPictureCount + pPictureCount
                        + bPictureCount) * 1.0) / (iPictureCount * 1.0) : 0.0));
    }
    return IfsClose(ifsHandle);
}
