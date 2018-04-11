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

#ifndef _REMAP_INTF_H
#define _REMAP_INTF_H "$Rev: 141 $"

#define SAVE_PAT_AND_PMT

#define REMAP_TRANSPORT_PACKET_SIZE 188
#define REMAP_NULL_PID_VALUE        ((RemapPid)0x1FFF)
#define REMAP_UNDEFINED_PID         ((RemapPid)-1)
#define REMAP_UNDEFINED_PACKET      ((NumPackets)-1)
#define llong                       long long
#define ullong                      unsigned long long

#ifndef _IFS_INTF_H
typedef unsigned long NumPackets; // number of packets
typedef unsigned llong NumBytes; // number of bytes
#endif
typedef unsigned short RemapPid; // PID values
typedef unsigned short RemapProg; // Program number values
typedef unsigned short NumPairs; // number of PID pairs

typedef enum
{
    RemapFalse = (0 == 1), RemapTrue = (1 == 1)
} RemapBoolean;

typedef struct RemapHandleImpl * RemapHandle;

typedef enum
{
    RemapReturnCodeNoErrorReported,
    RemapReturnCodeMemAllocationError,
    RemapReturnCodeBadInputParameter,

} RemapReturnCode;

typedef struct RemapPacket
{
    unsigned char bytes[REMAP_TRANSPORT_PACKET_SIZE];

} RemapPacket;

typedef struct RemapPair
{
    RemapPid oldPid;
    RemapPid newPid;

} RemapPair;

//  Opens a remap object in the default mode:
//
//      No PID filtering (all PIDs pass through)
//      No PID remapping (all PIDs unchanged)
//      No PMT rewriting (all PMTs unchanged)
//      No PAT rewriting (all PATs unchanged)
//
//  Note: the RemapHandle must be freed by calling the RemapClose function

RemapReturnCode RemapOpen(RemapHandle * const pRemapHandle // Output
        );

//  Change the PID remapping and filtering settings
//
//  If numPairs is zero:
//
//      ALL PIDs filtered out (no PIDs pass through)
//      pRemapPair MAY be NULL
//
//  If numPairs is not zero and pRemapPair is NULL
//
//      No PID filtering (all PIDs pass through)
//      No PID remapping (all PIDs unchanged)
//
//  If numPairs is not zero and pRemapPair is not NULL
//
//      Only the specified PIDs pass through
//      Optionally remap the PIDs
//
// This same information in a table:
//
//     numPairs  pRemapPair  PIDs passed through
//     --------  ----------  -------------------
//            0        NULL  NONE
//            0    not NULL  NONE
//        not 0        NULL  ALL
//        not 0    not NULL  The selected PIDs
//
//  Note: pRemapPair can be static, stack or heap memory.  Ownership of the
//  memory is returned to and remains with the caller.
//
//  Note: if any of the PIDs are remapped (by setting the newPid to a different
//  value than the oldPid) then you will probably also want to do PMT rewriting.
//  See the RemapPmts() function below.
//
//  Note: the PID pair list SHALL NOT contain a remapping of the PAT PID (an
//  entry with the oldPid set to 0x0000 and the newPid set to a different value)
//  but it MUST contain a filter setting of oldPid=0x0000/newPid=0x0000 if you
//  wish to pass the PATs on to the filtered stream.

RemapReturnCode RemapAndFilterPids(RemapHandle remapHandle, // Input
        const NumPairs numPairs, // Input, the number of PID remap pairs
        RemapPair * const pRemapPair // Input, the PIDs to pass and remap
        );

//  Change the PMT rewriting setting
//
//  If pmtPid is zero:
//
//      No PMT rewriting (all PMTs unchanged)
//
//  If pmtPid is not zero:
//
//      PMTs found on the specified PID are rewritten
//
//  Note:  The PMTs are rewritten using the PID pairs specified in the
//  RemapAndFilterPids() function
//
//  Note:  If there are no PID pairs specified in the RemapAndFilterPids()
//  function then PMT rewriting has no effect
//
//  Note:  If the oldPid equal the newPid in all of the PID pairs specified in
//  the RemapAndFilterPids() function then PMT rewriting has no effect

RemapReturnCode RemapPmts(RemapHandle remapHandle, // Input
        const RemapPid pmtPid // Input, the PMT PID
        );

//  Change the PAT rewriting setting
//
//  If programNumber is zero:
//
//      No PAT rewriting (all PATs unchanged)
//
//  If programNumber is not zero:
//
//      PATs found on the specified PID are rewritten
//
//  Note:  The PATs are rewritten using the specified program number and
//  pmtPid values so if the PMT PID is being remapped you need to specify the
//  new PMT PID here.
//
//  Note:  If PAT rewriting is enabled using this function then all incoming
//  MPTS PATs are converted to SPTS PATs.

RemapReturnCode RemapPats(RemapHandle remapHandle, // Input
        RemapProg programNumber, // Input, the program number
        const RemapPid pmtPid // Input, the new PMT PID
        );

//  Transfer the next buffer of transport packets and associated array of buffer
//  pointers into the Remap module for processing and receive back the processed
//  previous buffer of transport packets and associated array of buffer pointers
//
//  Note that the first time this function is called:
//
//      *pPrevNumPackets will be set to zero
//      *ppPrevPointers will be set to NULL
//
//  Note that in order to retrieve the last buffer call this function as follows:
//
//  remapReturnCode = RemapAndFilter
//  (
//      remapHandle,     // Input
//      NULL,            // Input one residual packet or NULL
//      0,               // Input next number of packets
//      NULL,            // Input next buffer of data
//      NULL,            // Input next array of buffer pointers
//      &prevNumPackets, // Output previous number of packets
//      &pPrevPointers   // Output previous array of buffer pointers
//  );

RemapReturnCode RemapAndFilter(RemapHandle remapHandle, // Input

        RemapPacket * const pResidualPacket, // Input one residual packet or NULL

        const NumPackets nextNumPackets, // Input next number of packets
        RemapPacket * const pNextPackets, // Input next buffer of data
        RemapPacket ** const pNextPointers, // Input next array of buffer pointers

        NumPackets * const pPrevNumPackets, // Output previous number of packets
        RemapPacket *** const ppPrevPointers // Output previous array of buffer pointers
        );

//  Close a Remap object and return any remaining packet and pointer memory to
//  the caller so it can be freed if necessary.

RemapReturnCode RemapClose(RemapHandle remapHandle, // Input

        NumPackets * const pPrevNumPackets, // Output previous number of packets (if any)
        RemapPacket *** const ppPrevPointers // Output previous array of buffer pointers (if any)
        );

#endif
