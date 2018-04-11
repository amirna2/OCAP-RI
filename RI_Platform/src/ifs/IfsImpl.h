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

// Private IFS Definitions

#ifndef _IFS_IMPL_H
#define _IFS_IMPL_H "$Rev: 141 $"

#include <glib.h>
#include <stdio.h>
#include "IfsIntf.h"

#define IFS_UNDEFINED_BYTE ((unsigned char)-1)

typedef enum
{
    // Adaptation events:

    IfsIndexAdaptAfeeBit = (unsigned) 1 << 0, // Adaptation field extension exists
    IfsIndexAdaptTpdeBit = (unsigned) 1 << 1, // Transport private data exists
    IfsIndexAdaptSpeBit = (unsigned) 1 << 2, // Splicing point exists
    IfsIndexAdaptOpcreBit = (unsigned) 1 << 3, // Old PCR exists
    IfsIndexAdaptPcreBit = (unsigned) 1 << 4, // PCR exists
    IfsIndexAdaptEspChange = (unsigned) 1 << 5, // Elementary stream priority CHANGED
    IfsIndexAdaptRaiBit = (unsigned) 1 << 6, // Random Access indicator
    IfsIndexAdaptDiBit = (unsigned) 1 << 7, // Discontinuity indicator

    // Transport header events:

    IfsIndexHeaderBadSync = (unsigned) 1 << 8, // Sync byte was not 0x47
    IfsIndexHeaderTpChange = (unsigned) 1 << 9, // Transport Priority CHANGED
    IfsIndexHeaderPusiBit = (unsigned) 1 << 10, // Payload Unit Start Indicator
    IfsIndexHeaderTeiBit = (unsigned) 1 << 11, // Transport Error Indicator
    IfsIndexHeaderCcError = (unsigned) 1 << 12, // Continuity counter ERROR
    IfsIndexHeaderScChange = (unsigned) 1 << 13, // Scrambling control CHANGED
    IfsIndexHeaderAfeBit = (unsigned) 1 << 14, // Adaptation field exists
    IfsIndexHeaderPdeBit = (unsigned) 1 << 15, // Payload data exists

    // Start Code events:

    IfsIndexStartPicture0 = (unsigned) 1 << 16, // PICTURE_START_CODE  00 (bit 0)
    IfsIndexStartPicture1 = (unsigned) 1 << 17, // PICTURE_START_CODE  00 (bit 1)
    IfsIndexStartUserData = (unsigned) 1 << 18, // USER_DATA           B2
    IfsIndexStartSeqHeader = (unsigned) 1 << 19, // SEQUENCE_HEADER     B3
    IfsIndexStartSeqError = (unsigned) 1 << 20, // SEQUENCE_ERROR      B4
    IfsIndexStartSeqEnd = (unsigned) 1 << 21, // SEQUENCE_END        B7
    IfsIndexStartGroup = (unsigned) 1 << 22, // GROUP_START_CODE    B8
    IfsIndexStartExtension = (unsigned) 1 << 23, // EXTENSION_START     B5

    // Extension Events:

    IfsIndexExtReserved = (unsigned) 1 << 24, // RESERVED                        0, 6 and B-F
    IfsIndexExtSequence = (unsigned) 1 << 25, // SEQUENCE_EXTENSION_ID           1
    IfsIndexExtDisplay = (unsigned) 1 << 26, // SEQUENCE_DISPLAY_EXTENSION_ID   2
    IfsIndexExtQuantMat = (unsigned) 1 << 27, // QUANT_MATRIX_EXTENSION_ID       3
    IfsIndexExtCopyright = (unsigned) 1 << 28, // COPYRIGHT_EXTENSION_ID          4
    IfsIndexExtScalable = (unsigned) 1 << 29, // SEQUENCE_SCALABLE_EXTENSION_ID  5
    IfsIndexExtPictOther = (unsigned) 1 << 30, // Other PICTURE_EXTENSION_IDs     7 and 9-A
    IfsIndexExtPictCode = (unsigned) 1 << 31,
// PICTURE_CODING_EXTENSION_ID     8

#ifdef DEBUG_ALL_PES_CODES

IfsIndexStartSlice = 1uLL << 32, // SLICE                     01 - AF
IfsIndexStartReservedB0 = 1uLL << 33, // RESERVED                  B0
IfsIndexStartReservedB1 = 1uLL << 34, // RESERVED                  B1
IfsIndexStartReservedB6 = 1uLL << 35, // RESERVED                  B6
IfsIndexStartMpegEnd = 1uLL << 36, // MPEG_PROGRAM_END_CODE     B9
IfsIndexStartPack = 1uLL << 37, // PACK_START_CODE           BA
IfsIndexStartSysHeader = 1uLL << 38, // SYSTEM_HEADER_START_CODE  BB
IfsIndexStartProgramMap = 1uLL << 39, // PROGRAM_STREAM_MAP        BC
IfsIndexStartPrivate1 = 1uLL << 40, // PRIVATE_STREAM_1          BD
IfsIndexStartPadding = 1uLL << 41, // PADDING_STREAM            BE
IfsIndexStartPrivate2 = 1uLL << 42, // PRIVATE_STREAM_2          BF
IfsIndexStartAudio = 1uLL << 43, // AUDIO                     C0 - DF
IfsIndexStartVideo = 1uLL << 44, // VIDEO                     E0 - EF
IfsIndexStartEcm = 1uLL << 45, // ECM_STREAM                F0
IfsIndexStartEmm = 1uLL << 46, // EMM_STREAM                F1
IfsIndexStartDsmCc = 1uLL << 47, // DSM_CC_STREAM             F2
IfsIndexStart13522 = 1uLL << 48, // ISO_IEC_13522_STREAM      F3
IfsIndexStartItuTypeA = 1uLL << 49, // ITU_T_REC_H_222_1_TYPE_A  F4
IfsIndexStartItuTypeB = 1uLL << 50, // ITU_T_REC_H_222_1_TYPE_B  F5
IfsIndexStartItuTypeC = 1uLL << 51, // ITU_T_REC_H_222_1_TYPE_C  F6
IfsIndexStartItuTypeD = 1uLL << 52, // ITU_T_REC_H_222_1_TYPE_D  F7
IfsIndexStartItuTypeE = 1uLL << 53, // ITU_T_REC_H_222_1_TYPE_E  F8
IfsIndexStartAncillary = 1uLL << 54, // ANCILLARY_STREAM          F9
IfsIndexStartRes_FA_FE = 1uLL << 55, // RESERVED                  FA - FE
IfsIndexStartDirectory = 1uLL << 56, // PROGRAM_STREAM_DIRECTORY  FF

IfsIndexInfoContainsPts = 1uLL << 57,
IfsIndexInfoProgSeq = 1uLL << 58,
IfsIndexInfoRepeatFirst = 1uLL << 59,
IfsIndexInfoTopFirst = 1uLL << 60,
IfsIndexInfoProgFrame = 1uLL << 61,
IfsIndexInfoStructure0 = 1uLL << 62,
IfsIndexInfoStructure1 = 1uLL << 63,

#endif

} IfsIndex;

#define IfsIndexStartPicture  (IfsIndexStartPicture0|IfsIndexStartPicture1)
#define IfsIndexInfoStructure (IfsIndexInfoStructure0|IfsIndexInfoStructure1)
#define IfsIndexInfoProgRep   (IfsIndexInfoRepeatFirst|IfsIndexInfoTopFirst)

typedef enum
{

    IfsIndexerSettingVerbose = // index all possible events

    // Adaptation events:

    IfsIndexAdaptAfeeBit | IfsIndexAdaptTpdeBit | IfsIndexAdaptSpeBit
            | IfsIndexAdaptOpcreBit | IfsIndexAdaptPcreBit
            | IfsIndexAdaptEspChange | IfsIndexAdaptRaiBit | IfsIndexAdaptDiBit
            |

            // Transport header events:

            IfsIndexHeaderBadSync | IfsIndexHeaderTpChange
            | IfsIndexHeaderPusiBit | IfsIndexHeaderTeiBit
            | IfsIndexHeaderCcError | IfsIndexHeaderPdeBit
            | IfsIndexHeaderAfeBit | IfsIndexHeaderScChange |

    // Start Code events:

            IfsIndexStartPicture | IfsIndexStartUserData
            | IfsIndexStartSeqHeader | IfsIndexStartSeqError
            | IfsIndexStartExtension | IfsIndexStartSeqEnd | IfsIndexStartGroup
            |

            // Extension Events:

            IfsIndexExtReserved | IfsIndexExtSequence | IfsIndexExtDisplay
            | IfsIndexExtQuantMat | IfsIndexExtCopyright | IfsIndexExtScalable
            | IfsIndexExtPictOther | IfsIndexExtPictCode |

#ifdef DEBUG_ALL_PES_CODES

            IfsIndexStartSlice |
            IfsIndexStartReservedB0 |
            IfsIndexStartReservedB1 |
            IfsIndexStartReservedB6 |
            IfsIndexStartMpegEnd |
            IfsIndexStartPack |
            IfsIndexStartSysHeader |
            IfsIndexStartProgramMap |
            IfsIndexStartPrivate1 |
            IfsIndexStartPadding |
            IfsIndexStartPrivate2 |
            IfsIndexStartAudio |
            IfsIndexStartVideo |
            IfsIndexStartEcm |
            IfsIndexStartEmm |
            IfsIndexStartDsmCc |
            IfsIndexStart13522 |
            IfsIndexStartItuTypeA |
            IfsIndexStartItuTypeB |
            IfsIndexStartItuTypeC |
            IfsIndexStartItuTypeD |
            IfsIndexStartItuTypeE |
            IfsIndexStartAncillary |
            IfsIndexStartRes_FA_FE |
            IfsIndexStartDirectory |

            IfsIndexInfoContainsPts |
            IfsIndexInfoProgSeq |
            IfsIndexInfoRepeatFirst |
            IfsIndexInfoTopFirst |
            IfsIndexInfoProgFrame |
            IfsIndexInfoStructure0 |
            IfsIndexInfoStructure1 |

#endif

            0,

    IfsIndexerSettingUnitest =

    // Adaptation events:

    IfsIndexAdaptAfeeBit | IfsIndexAdaptTpdeBit | IfsIndexAdaptSpeBit
            | IfsIndexAdaptOpcreBit | IfsIndexAdaptPcreBit
            | IfsIndexAdaptEspChange | IfsIndexAdaptRaiBit | IfsIndexAdaptDiBit
            |

            // Transport header events:

            IfsIndexHeaderBadSync | IfsIndexHeaderTpChange
            | IfsIndexHeaderPusiBit | IfsIndexHeaderTeiBit
            | IfsIndexHeaderCcError | IfsIndexHeaderScChange |

    // Start Code events:

            IfsIndexStartPicture | IfsIndexStartUserData
            | IfsIndexStartSeqHeader | IfsIndexStartSeqError
            | IfsIndexStartExtension | IfsIndexStartSeqEnd | IfsIndexStartGroup
            |

            // Extension Events:

            IfsIndexExtReserved | IfsIndexExtSequence | IfsIndexExtDisplay
            | IfsIndexExtQuantMat | IfsIndexExtCopyright | IfsIndexExtScalable
            | IfsIndexExtPictOther | IfsIndexExtPictCode |

#ifdef DEBUG_ALL_PES_CODES

            IfsIndexStartSlice |
            IfsIndexStartReservedB0 |
            IfsIndexStartReservedB1 |
            IfsIndexStartReservedB6 |
            IfsIndexStartMpegEnd |
            IfsIndexStartPack |
            IfsIndexStartSysHeader |
            IfsIndexStartProgramMap |
            IfsIndexStartPrivate1 |
            IfsIndexStartPadding |
            IfsIndexStartPrivate2 |
            IfsIndexStartAudio |
            IfsIndexStartVideo |
            IfsIndexStartEcm |
            IfsIndexStartEmm |
            IfsIndexStartDsmCc |
            IfsIndexStart13522 |
            IfsIndexStartItuTypeA |
            IfsIndexStartItuTypeB |
            IfsIndexStartItuTypeC |
            IfsIndexStartItuTypeD |
            IfsIndexStartItuTypeE |
            IfsIndexStartAncillary |
            IfsIndexStartRes_FA_FE |
            IfsIndexStartDirectory |

            IfsIndexInfoContainsPts |
            IfsIndexInfoProgSeq |
            IfsIndexInfoRepeatFirst |
            IfsIndexInfoTopFirst |
            IfsIndexInfoProgFrame |
            IfsIndexInfoStructure0 |
            IfsIndexInfoStructure1 |

#endif

            0,

    IfsIndexerSettingDefPlus = // Default plus PCR and PTS indexing

    IfsIndexAdaptPcreBit | IfsIndexStartPicture | IfsIndexStartSeqHeader |

#ifdef DEBUG_ALL_PES_CODES

            IfsIndexStartVideo |
            IfsIndexInfoContainsPts |

#endif

            0,

    IfsIndexerSettingDefault =

    IfsIndexStartPicture | IfsIndexStartSeqHeader |

    0,

} IfsIndexerSetting;

typedef enum
{

    IfsIndexDumpModeOff = 0, IfsIndexDumpModeDef = 1, IfsIndexDumpModeAll = 2,

} IfsIndexDumpMode;

typedef enum // next 00 next 01 next B5 next EX next 1X next 8X   else   where
{ // ------- ------- ------- ------- ------- ------- ------- -------
    IfsStateInitial, // Zero_01                                         Initial buf[ 0]
    IfsStateZero_01, // Zero_02                                         Initial buf[ 1]
    IfsStateZero_02, // Zero_02 Found_1                                 Initial buf[ 2]
    IfsStateFound_1, // GotPic0         GotExt0 GotVid0                 Initial buf[ 3]
    //
    IfsStateGotPic0, //                                                 GotPic1 buf[ 4]
    IfsStateGotPic1, //                                                 Initial buf[ 5]
    //
    IfsStateGotExt0, //                                 GotExt1 GotExt2 Initial buf[ 4]
    IfsStateGotExt1, //                                                 Initial buf[ 5]
    IfsStateGotExt2, //                                                 GotExt3 buf[ 5]
    IfsStateGotExt3, //                                                 GotExt4 buf[ 6]
    IfsStateGotExt4, //                                                 GotExt5 buf[ 7]
    IfsStateGotExt5, //                                                 Initial buf[ 8]

    IfsStateGotVid0, //                                                 GotVid1 buf[ 4]
    IfsStateGotVid1, //                                                 GotVid2 buf[ 5]
    IfsStateGotVid2, //                                                 GotVid3 buf[ 6]
    IfsStateGotVid3, //                                         GotVid4 Initial buf[ 7]
    IfsStateGotVid4, //                                                 GotVid5 buf[ 8]
    IfsStateGotVid5, //                                                 GotVid6 buf[ 9]
    IfsStateGotVid6, //                                                 GotVid7 buf[10]
    IfsStateGotVid7, //                                                 GotVid8 buf[11]
    IfsStateGotVid8, //                                                 GotVid9 buf[12]
    IfsStateGotVid9,
//                                                 Initial buf[13]

} IfsState;

typedef enum
{
    IfsReadTypePrevious = -1, IfsReadTypeNearest = 0, IfsReadTypeNext = +1,

} IfsReadType;

typedef struct IfsIndexEntry
{
    IfsClock when; // nanoseconds
    IfsIndex what;
    NumPackets realWhere;
    NumPackets virtWhere;

#ifndef DEBUG_ALL_PES_CODES
    unsigned long pad;
#endif

} IfsIndexEntry;

typedef unsigned long FileNumber;

typedef struct IfsHandleImpl
{
    // The input parameters
    char * path;
    char * name;

    // Current logical file information
    NumBytes mpegSize; // current total number of MPEG bytes
    NumBytes ndexSize; // current total number of NDEX bytes
    char * both; // path + name
    char * mpeg; // path + name + filename.mpg
    char * ndex; // path + name + filename.ndx
    FileNumber begFileNumber; // lowest file name/number
    FileNumber endFileNumber; // highest file name/number
    FILE * pMpeg; // current MPEG file
    FILE * pNdex; // current NDEX file
    NumPackets realLoc; // current real location in current file, offset in packets
    NumPackets virtLoc; // current virtual location in current file, offset in packets
    IfsBoolean isReading; // true if this a reader, false if it is a writer
    IfsClock begClock; // clock at beg of recording, in nanoseconds
    IfsClock endClock; // clock at end of recording, in nanoseconds
    IfsClock nxtClock; // clock at next file change, in nanoseconds

    // Indexer settings and state
    IfsIndexEntry entry;
#ifdef DEBUG_ALL_PES_CODES
    IfsIndex extWhat;
    IfsBoolean isProgSeq;
    long pad1;
#endif
    IfsPcr ifsPcr;
    IfsPts ifsPts;
    IfsPid videoPid;
    IfsPid audioPid;
    IfsState ifsState;
    unsigned char oldEsp;
    unsigned char oldSc;
    unsigned char oldTp;
    unsigned char oldCc;

    // Seek state, all real, no virtual numbers
    NumPackets maxPacket;
    FileNumber curFileNumber;
    NumEntries entryNum;
    NumEntries maxEntry;

    // Append state, all real, no virtual numbers
    FileNumber appendFileNumber;
    NumPackets appendPacketNum;
    NumEntries appendEntryNum;
    NumPackets appendIndexShift;
    NumPackets appendPrevFiles;
    long pad2;

    IfsTime maxSize; // in seconds, 0 = value not used

    int numEmptyFreads;  // the number of times we performed an fread and got 0

    GStaticMutex mutex;  // TSB thread versus IFS thread protection

} IfsHandleImpl;

IfsIndex IfsGetWhatAll(void);
void IfsSetMode(const IfsIndexDumpMode ifsIndexDumpMode,
        const IfsIndexerSetting ifsIndexerSetting);

IfsReturnCode IfsOpenActualFiles(IfsHandle ifsHandle, FileNumber fileNumber,
        const char * const mode);

IfsReturnCode IfsSeekToTimeImpl // Must call GetCurrentFileParameters() before calling this function
        (IfsHandle ifsHandle, // Input
                IfsDirect ifsDirect, // Input  either IfsDirectBegin,
                //        IfsDirectEnd or IfsDirectEither
                IfsClock * pIfsClock, // Input  requested/Output actual, in nanoseconds
                NumPackets * pPosition // Output packet position, optional, can be NULL
        );

IfsReturnCode IfsSeekToPacketImpl // Must call GetCurrentFileSizeAndCount() before calling this function
        (IfsHandle ifsHandle, // Input
                NumPackets virtPos, // Input desired (virtual) packet position
                IfsClock * pIfsClock // Output clock value, optional, can be NULL
        );

IfsReturnCode IfsReadPicture // Must call IfsSeekToTime() before calling this function
        (IfsHandle ifsHandle, // Input
                IfsPcr ifsPcr, // Input
                IfsPts ifsPts, // Input
                IfsReadType ifsReadType, // Input
                NumPackets * pNumPackets, // Output
                IfsPacket ** ppData, // Output
                NumPackets * pStartPacket // Output
        );

#endif
