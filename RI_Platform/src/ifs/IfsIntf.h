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

// PIMPL Public IFS Interface

#ifndef _IFS_INTF_H
#define _IFS_INTF_H "$Rev: 141 $"

#define INTF_RELEASE_VERSION  7

#define DEBUG_ALL_PES_CODES

#define IFS_TRANSPORT_PACKET_SIZE 188
#define IFS_NULL_PID_VALUE        ((IfsPid)0x1FFF)
#define IFS_UNDEFINED_PID         ((IfsPid)-1)
#define IFS_UNDEFINED_CLOCK       ((IfsClock)-1)
#define IFS_UNDEFINED_ENTRY       ((NumEntries)-1)
#define IFS_UNDEFINED_PACKET      ((NumPackets)-1)
#define IFS_UNDEFINED_FILENUMBER  ((FileNumber)-1)
#define NSEC_PER_SEC              (1000000000uLL)
#define llong                     long long
#define ullong                    unsigned long long

typedef unsigned long IfsTime; // time in seconds
typedef unsigned llong IfsClock; // YYYY/MM/DD HH:MM:SS.MMMUUUNNN, in nanoseconds
#ifndef _REMAP_INTF_H
typedef unsigned long NumPackets; // number of packets
typedef unsigned llong NumBytes; // number of bytes
#endif
typedef unsigned long NumEntries; // number of index entries
typedef unsigned short IfsPid; // PID values
typedef unsigned llong IfsPcr; // PCR values
typedef unsigned llong IfsPts; // PTS values

typedef struct IfsHandleImpl * IfsHandle;

typedef enum
{
    IfsFalse = (0 == 1), IfsTrue = (1 == 1)
} IfsBoolean;

typedef enum
{
    IfsDirectBegin, // Seek to the begin of any range of entries with the same time stamp
    IfsDirectEnd, // Seek to the end of any range of entries with the same time stamp
    IfsDirectEither,
// Seek to either the begin or end of the range, whichever is fastest

} IfsDirect;

typedef enum
{
    IfsReturnCodeNoErrorReported      =  0,

    IfsReturnCodeBadInputParameter    =  1,
    IfsReturnCodeBadMaxSizeValue      =  2,
    IfsReturnCodeMemAllocationError   =  3,
    IfsReturnCodeIllegalOperation     =  4,
    IfsReturnCodeMustBeAnIfsWriter    =  5,

    IfsReturnCodeSprintfError         =  6,
    IfsReturnCodeStatError            =  7,

    IfsReturnCodeMakeDirectoryError   =  8,
    IfsReturnCodeOpenDirectoryError   =  9,
    IfsReturnCodeCloseDirectoryError  = 10,
    IfsReturnCodeDeleteDirectoryError = 11,

    IfsReturnCodeFileOpeningError     = 12,
    IfsReturnCodeFileSeekingError     = 13,
    IfsReturnCodeFileWritingError     = 14,
    IfsReturnCodeFileReadingError     = 15,
    IfsReturnCodeFileFlushingError    = 16,
    IfsReturnCodeFileClosingError     = 17,
    IfsReturnCodeFileDeletingError    = 18,
    IfsReturnCodeFileWasNotFound      = 19,

    IfsReturnCodeSeekOutsideFile      = 20, // warning
    IfsReturnCodeReadPastEndOfFile    = 21, // warning
    IfsReturnCodeIframeNotFound       = 22, // warning
    IfsReturnCodeFoundPbeforeIframe   = 23, // warning
    IfsReturnCodeFoundBbeforeIframe   = 24, // warning
    IfsReturnCodeIframeStartNotFound  = 25, // warning
    IfsReturnCodeIframeEndNotFound    = 26, // warning

} IfsReturnCode;

typedef struct IfsInfo
{
    char * path;
    char * name;
    NumBytes mpegSize; // size of the MPEG file(s)
    NumBytes ndexSize; // size of the NDEX file(s)
    IfsClock begClock; // in nanoseconds
    IfsClock endClock; // in nanoseconds
    IfsPid videoPid;
    IfsPid audioPid;
    IfsTime maxSize; // in seconds, 0 = value not used

} IfsInfo;

typedef struct IfsPacket
{
    unsigned char bytes[IFS_TRANSPORT_PACKET_SIZE];

} IfsPacket;

// Utility operations:

const char * IfsReturnCodeToString(const IfsReturnCode ifsReturnCode);

void IfsInit(void);
char * IfsToSecs(const IfsClock ifsClock, char * const temp); // temp[] must be at least 23 characters, returns temp
char * IfsLongLongToString(ullong value, char * const temp); // temp[] must be at least 27 characters, returns temp
void IfsDumpInfo(const IfsInfo * const pIfsInfo);
void IfsDumpHandle(const IfsHandle ifsHandle);

IfsReturnCode IfsFreeInfo(IfsInfo * pIfsInfo // Input
        );

// Path/Name operations:

IfsReturnCode IfsOpenWriter(const char * path, // Input
        const char * name, // Input  (if NULL the name is generated)
        IfsTime maxSize, // Input  (in seconds, 0 = no max)
        IfsHandle * pIfsHandle // Output (use IfsClose() to free)
        );

IfsReturnCode IfsOpenReader(const char * path, // Input
        const char * name, // Input
        IfsHandle * pIfsHandle // Output (use IfsClose() to free)
        );

IfsReturnCode IfsPathNameInfo(const char * path, // Input
        const char * name, // Input
        IfsInfo ** ppIfsInfo // Output (use IfsFreeInfo() to free)
        );

IfsReturnCode IfsDelete(const char * path, // Input
        const char * name // Input
        );

// IfsHandle operations:

IfsReturnCode IfsStart(IfsHandle ifsHandle, // Input (must be a writer)
        IfsPid videoPid, // Input
        IfsPid audioPid // Input
        );

IfsReturnCode IfsSetMaxSize(IfsHandle ifsHandle, // Input (must be a writer)
        IfsTime maxSize // Input (in seconds, 0 is illegal)
        );

IfsReturnCode IfsStop(IfsHandle ifsHandle // Input (must be a writer)
        );

IfsReturnCode IfsHandleInfo(IfsHandle ifsHandle, // Input
        IfsInfo ** ppIfsInfo // Output (use IfsFreeInfo() to free)
        );

IfsReturnCode IfsWrite(IfsHandle ifsHandle, // Input (must be a writer)
        IfsClock ifsClock, // Input, in nanoseconds
        NumPackets numPackets, // Input
        IfsPacket * pData // Input
        );

IfsReturnCode IfsConvert(IfsHandle srcHandle, // Input
        IfsHandle dstHandle, // Input (must be a writer)
        IfsClock * pBegClock, // Input requested/Output actual, in nanoseconds
        IfsClock * pEndClock // Input requested/Output actual, in nanoseconds
        );

IfsReturnCode IfsAppend // Must call IfsConvert() before calling this function
        (IfsHandle srcHandle, // Input
                IfsHandle dstHandle, // Input (must be a writer)
                IfsClock * pEndClock // Input requested/Output actual, in nanoseconds
        );

IfsReturnCode IfsSeekToTime(IfsHandle ifsHandle, // Input
        IfsDirect ifsDirect, // Input  either IfsDirectBegin,
        //        IfsDirectEnd or IfsDirectEither
        IfsClock * pIfsClock, // Input  requested/Output actual, in nanoseconds
        NumPackets * pPosition // Output packet position, optional, can be NULL
        );

IfsReturnCode IfsSeekToPacket(IfsHandle ifsHandle, // Input
        NumPackets position, // Input  requested packet position
        IfsClock * pIfsClock // Output clock value, optional, can be NULL
        );

IfsReturnCode IfsRead(IfsHandle ifsHandle, // Input
        NumPackets * pNumPackets, // Input  requested
        // Output actual
        IfsClock * pCurClock, // Output current clock value
        IfsPacket ** ppData // Output
        );

IfsReturnCode IfsReadNearestPicture // Must call IfsSeekToTime() before calling this function
        (IfsHandle ifsHandle, // Input
                IfsPcr ifsPcr, // Input
                IfsPts ifsPts, // Input
                NumPackets * pNumPackets, // Output
                IfsPacket ** ppData // Output
        );

IfsReturnCode IfsReadNextPicture // Must call IfsSeekToTime() before calling this function
        (IfsHandle ifsHandle, // Input
                IfsPcr ifsPcr, // Input
                IfsPts ifsPts, // Input
                NumPackets * pNumPackets, // Output
                IfsPacket ** ppData // Output
        );

IfsReturnCode IfsReadPreviousPicture // Must call IfsSeekToTime() before calling this function
        (IfsHandle ifsHandle, // Input
                IfsPcr ifsPcr, // Input
                IfsPts ifsPts, // Input
                NumPackets * pNumPackets, // Output
                IfsPacket ** ppData // Output
        );

IfsReturnCode IfsClose(IfsHandle ifsHandle // Input
        );

IfsBoolean IfsHasWriter(IfsHandle ifsHandle);

#endif
