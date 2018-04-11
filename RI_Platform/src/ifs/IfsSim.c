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

#define DO_RANDOM_SEEKS

#define BACKGROUND_PROGRAM_1    0
#define PLAIN_PROGRAM_1         1
#define PLAIN_PROGRAM_2         2
#define PLAIN_PROGRAM_3         3
#define PLAIN_PROGRAM_4         4

#define TEST_CASE PLAIN_PROGRAM_1

#define _IFS_SIM_C "$Rev: 144 $"

#include <glib.h>
#include <stdlib.h>
#include <string.h>

#include "IfsImpl.h"
#include "RemapImpl.h"
#include "IfsParse.h"

#define DEFAULT_BACKG_NAME "background.mpg"
#define DEFAULT_PLAIN_NAME "plain.mpg"
#define DEFAULT_INP_PATH   "../Streams"
#define DEFAULT_OUT_PATH   "."

static char * pBackgMpg = NULL;
static char * pPlainMpg = NULL;
static char * pInpPath = NULL;
static char * pOutPath = NULL;
static IfsBoolean debug = IfsFalse;
static unsigned int testNumber = 0;
static unsigned int testPassed = 0;
static unsigned int testFailed = 0;

#define test(function, expect)           \
{                                        \
    IfsReturnCode got = (function);      \
    if (got == (expect)) testPassed++;   \
    else                                 \
    {                                    \
        printf("Test #%d, got %d, "      \
               "expected %d, line %d\n", \
               testNumber, got,          \
               (expect), __LINE__);      \
        testFailed++;                    \
    }                                    \
    testNumber++;                        \
}

#define testRemap(function, expect)      \
{                                        \
    RemapReturnCode got = (function);    \
    if (got == (expect)) testPassed++;   \
    else                                 \
    {                                    \
        printf("Test #%d, got %d, "      \
               "expected %d, line %d\n", \
               testNumber, got,          \
               (expect), __LINE__);      \
        testFailed++;                    \
    }                                    \
    testNumber++;                        \
}

#define isne(thingone, thingtwo)         \
{                                        \
    if ((thingone) != (thingtwo))        \
        testPassed++;                    \
    else                                 \
    {                                    \
        printf("Test #%d, NE assertion " \
               "failed, line %d\n",      \
               testNumber, __LINE__);    \
        testFailed++;                    \
    }                                    \
    testNumber++;                        \
}

#define iseq(thingone, thingtwo)         \
{                                        \
    if ((thingone) == (thingtwo))        \
        testPassed++;                    \
    else                                 \
    {                                    \
        printf("Test #%d, EQ assertion " \
               "failed, line %d\n",      \
               testNumber, __LINE__);    \
        testFailed++;                    \
    }                                    \
    testNumber++;                        \
}

#define isle(thingone, thingtwo)         \
{                                        \
    if ((thingone) <= (thingtwo))        \
        testPassed++;                    \
    else                                 \
    {                                    \
        printf("Test #%d, LE assertion " \
               "failed, line %d\n",      \
               testNumber, __LINE__);    \
        testFailed++;                    \
    }                                    \
    testNumber++;                        \
}

#define isge(thingone, thingtwo)         \
{                                        \
    if ((thingone) >= (thingtwo))        \
        testPassed++;                    \
    else                                 \
    {                                    \
        printf("Test #%d, GE assertion " \
               "failed, line %d\n",      \
               testNumber, __LINE__);    \
        testFailed++;                    \
    }                                    \
    testNumber++;                        \
}

static IfsPacket
        data[] =
        {
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x20, 0x02, 0xC0, 0x00 } }, //    (set initial state)
                /* 4*/
                {
                { 0x38, 0x20, 0x00, 0xC0, 0x00 } }, //  0 Bad sync byte
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x00, 0x02, 0xC1, 0x00 } }, //  1 TP change
                {
                { 0x47, 0x40, 0x02, 0xC2, 0x00 } }, //  2 PUSI
                {
                { 0x47, 0x80, 0x02, 0xC3, 0x00 } }, //  3 TEI
                {
                { 0x47, 0x00, 0x02, 0xC3, 0x00 } }, //  4 CC error
                {
                { 0x47, 0x00, 0x02, 0x04, 0x00 } }, //  5 SC change
                {
                { 0x47, 0x00, 0x02, 0x25, 0x00 } }, //  6 AFE with 0 length AF
                {
                { 0x47, 0x00, 0x02, 0x26, 0x01, 0x01 } }, //  7 AFE with AfeBit
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                /*17*/
                {
                { 0x47, 0x00, 0x02, 0x27, 0x01, 0x02 } }, //  8 AFE with TpdBit
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                /*22*/
                {
                { 0x47, 0x00, 0x02, 0x28, 0x01, 0x04 } }, //  9 AFE with SpBit
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x00, 0x02, 0x29, 0x01, 0x08 } }, // 10 AFE with OpcrBit
                {
                { 0x47, 0x00, 0x02, 0x2A, 0x01, 0x10 } }, // 11 AFE with PcrBit
                {
                { 0x47, 0x00, 0x02, 0x2B, 0x01, 0x20 } }, // 12 AFE with EspChange
                {
                { 0x47, 0x00, 0x02, 0x2C, 0x01, 0x60 } }, // 13 AFE with RaiBit
                {
                { 0x47, 0x00, 0x02, 0x2D, 0x01, 0xA0 } }, // 14 AFE with DiBit
                {
                { 0x47, 0x00, 0x02, 0x1E, 0x00, 0x00, 0x00, 0x00, 0x00 } }, // 15 PDE
                {
                { 0x47, 0x00, 0x02, 0x1F, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 1
                        << 3 } }, // 16 PDE PICTURE_START_CODE
                {
                { 0x47, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 2
                        << 3 } }, // 17 PDE PICTURE_START_CODE
                {
                { 0x47, 0x00, 0x02, 0x11, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 3
                        << 3 } }, // 18 PDE PICTURE_START_CODE
                {
                { 0x47, 0x00, 0x02, 0x12, 0x00, 0x00, 0x00, 0x01, 0xB2 } }, // 19 PDE USER_DATA
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                /*39*/
                {
                { 0x47, 0x00, 0x02, 0x13, 0x00, 0x00, 0x00, 0x01, 0xB3 } }, // 20 PDE SEQUENCE_HEADER
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                /*43*/
                {
                { 0x47, 0x00, 0x02, 0x14, 0x00, 0x00, 0x00, 0x01, 0xB4 } }, // 21 PDE SEQUENCE_ERROR
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x00, 0x02, 0x15, 0x00, 0x00, 0x00, 0x01, 0xB7 } }, // 22 PDE SEQUENCE_END
                {
                { 0x47, 0x00, 0x02, 0x16, 0x00, 0x00, 0x00, 0x01, 0xB8 } }, // 23 PDE GROUP_START_CODE
                {
                { 0x47, 0x00, 0x02, 0x17, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x00 } }, // 24 PDE EXTENSION_START RESERVED (0, 6 and B-F)
                {
                { 0x47, 0x00, 0x02, 0x18, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x10,
                        0x08 } }, // 25 PDE EXTENSION_START SEQUENCE_EXTENSION_ID
                {
                { 0x47, 0x00, 0x02, 0x19, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x10,
                        0x00 } }, // 26 PDE EXTENSION_START SEQUENCE_EXTENSION_ID
                {
                { 0x47, 0x00, 0x02, 0x1A, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x20 } }, // 27 PDE EXTENSION_START SEQUENCE_DISPLAY_EXTENSION_ID
                {
                { 0x47, 0x00, 0x02, 0x1B, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x30 } }, // 28 PDE EXTENSION_START QUANT_MATRIX_EXTENSION_ID
                {
                { 0x47, 0x00, 0x02, 0x1C, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x40 } }, // 29 PDE EXTENSION_START COPYRIGHT_EXTENSION_ID
                {
                { 0x47, 0x00, 0x02, 0x1D, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x50 } }, // 30 PDE EXTENSION_START SEQUENCE_SCALABLE_EXTENSION_ID
                {
                { 0x47, 0x00, 0x02, 0x1E, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x90 } }, // 31 PDE EXTENSION_START Other PICTURE_EXTENSION_IDs
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },
                /*60*/
                {
                { 0x47, 0x00, 0x02, 0x1F, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80,
                        0x00, 0x00, 0x00, 0x00 } }, // 32 PDE EXTENSION_START PICTURE_CODING_EXTENSION
                {
                { 0x47, 0x1F, 0xFF } },
                {
                { 0x47, 0x1F, 0xFF } },

#ifdef DEBUG_ALL_PES_CODES

        /*63*/
        {
            {   0x47, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x00, 0x02, 0x00}}, // 33 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x11, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x00, 0x80, 0x00}}, // 34 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x12, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x00, 0x82, 0x00}}, // 35 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x13, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x00, 0x00, 0x80}}, // 36 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x14, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x00, 0x02, 0x80}}, // 37 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x15, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x00, 0x80, 0x80}}, // 38 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x16, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x00, 0x82, 0x80}}, // 39 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x17, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x00, 0x00}}, // 40 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x18, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x02, 0x00}}, // 41 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x19, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x80, 0x00}}, // 42 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1A, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x82, 0x00}}, // 43 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1B, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x00, 0x80}}, // 44 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1C, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x02, 0x80}}, // 45 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1D, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x80, 0x80}}, // 46 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1E, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x01, 0x82, 0x80}}, // 47 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1F, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x00, 0x00}}, // 48 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x02, 0x00}}, // 49 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x1F, 0xFF}},
        {
            {   0x47, 0x1F, 0xFF}},
        {
            {   0x47, 0x1F, 0xFF}},
        /*83*/
        {
            {   0x47, 0x00, 0x02, 0x11, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x80, 0x00}}, // 50 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x1F, 0xFF}},
        {
            {   0x47, 0x1F, 0xFF}},
        {
            {   0x47, 0x1F, 0xFF}},
        /*87*/
        {
            {   0x47, 0x00, 0x02, 0x12, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x82, 0x00}}, // 51 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x13, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x00, 0x80}}, // 52 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x14, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x02, 0x80}}, // 53 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x15, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x80, 0x80}}, // 54 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x16, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x02, 0x82, 0x80}}, // 55 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x17, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x00, 0x00}}, // 56 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x18, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x02, 0x00}}, // 57 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x19, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x80, 0x00}}, // 58 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1A, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x82, 0x00}}, // 59 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1B, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x00, 0x80}}, // 60 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1C, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x02, 0x80}}, // 61 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1D, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x80, 0x80}}, // 62 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1E, 0x00, 0x00, 0x00, 0x01, 0xB5, 0x80, 0x00, 0x03, 0x82, 0x80}}, // 63 PDE EXTENSION_START PICTURE_CODING_EXTENSION

        {
            {   0x47, 0x00, 0x02, 0x1F, 0x00, 0x00, 0x00, 0x01, 0xAF}}, // 64 SLICE

        {
            {   0x47, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x01, 0xB0}}, // 65 RESERVED

        {
            {   0x47, 0x00, 0x02, 0x11, 0x00, 0x00, 0x00, 0x01, 0xB1}}, // 66 RESERVED

        {
            {   0x47, 0x00, 0x02, 0x12, 0x00, 0x00, 0x00, 0x01, 0xB6}}, // 67 RESERVED

        {
            {   0x47, 0x00, 0x02, 0x13, 0x00, 0x00, 0x00, 0x01, 0xB9}}, // 68 MPEG_PROGRAM_END_CODE

        {
            {   0x47, 0x00, 0x02, 0x14, 0x00, 0x00, 0x00, 0x01, 0xBA}}, // 69 PACK_START_CODE

        {
            {   0x47, 0x00, 0x02, 0x15, 0x00, 0x00, 0x00, 0x01, 0xBB}}, // 70 SYSTEM_HEADER_START_CODE

        {
            {   0x47, 0x00, 0x02, 0x16, 0x00, 0x00, 0x00, 0x01, 0xBC}}, // 71 PROGRAM_STREAM_MAP

        {
            {   0x47, 0x00, 0x02, 0x17, 0x00, 0x00, 0x00, 0x01, 0xBD}}, // 72 PRIVATE_STREAM_1

        {
            {   0x47, 0x00, 0x02, 0x18, 0x00, 0x00, 0x00, 0x01, 0xBE}}, // 73 PADDING_STREAM

        {
            {   0x47, 0x00, 0x02, 0x19, 0x00, 0x00, 0x00, 0x01, 0xBF}}, // 74 PRIVATE_STREAM_2

        {
            {   0x47, 0x00, 0x02, 0x1A, 0x00, 0x00, 0x00, 0x01, 0xC0}}, // 75 AUDIO

        {
            {   0x47, 0x00, 0x02, 0x1B, 0x00, 0x00, 0x00, 0x01, 0xE0}}, // 76 VIDEO

        {
            {   0x47, 0x00, 0x02, 0x1C, 0x00, 0x00, 0x00, 0x01, 0xEF, 0x00, 0x00, 0x00, 0x80}}, // 77 VIDEO(0)

        {
            {   0x47, 0x00, 0x02, 0x1D, 0x00, 0x00, 0x00, 0x01, 0xF0}}, // 78 ECM_STREAM

        {
            {   0x47, 0x00, 0x02, 0x1E, 0x00, 0x00, 0x00, 0x01, 0xF1}}, // 79 EMM_STREAM

        {
            {   0x47, 0x00, 0x02, 0x1F, 0x00, 0x00, 0x00, 0x01, 0xF2}}, // 80 DSM_CC_STREAM

        {
            {   0x47, 0x00, 0x02, 0x10, 0x00, 0x00, 0x00, 0x01, 0xF3}}, // 81 ISO_IEC_13522_STREAM

        {
            {   0x47, 0x00, 0x02, 0x11, 0x00, 0x00, 0x00, 0x01, 0xF4}}, // 82 ITU_T_REC_H_222_1_TYPE_A

        {
            {   0x47, 0x00, 0x02, 0x12, 0x00, 0x00, 0x00, 0x01, 0xF5}}, // 83 ITU_T_REC_H_222_1_TYPE_B

        {
            {   0x47, 0x00, 0x02, 0x13, 0x00, 0x00, 0x00, 0x01, 0xF6}}, // 84 ITU_T_REC_H_222_1_TYPE_C

        {
            {   0x47, 0x00, 0x02, 0x14, 0x00, 0x00, 0x00, 0x01, 0xF7}}, // 85 ITU_T_REC_H_222_1_TYPE_D

        {
            {   0x47, 0x00, 0x02, 0x15, 0x00, 0x00, 0x00, 0x01, 0xF8}}, // 86 ITU_T_REC_H_222_1_TYPE_E

        {
            {   0x47, 0x00, 0x02, 0x16, 0x00, 0x00, 0x00, 0x01, 0xF9}}, // 87 ANCILLARY_STREAM

        {
            {   0x47, 0x00, 0x02, 0x17, 0x00, 0x00, 0x00, 0x01, 0xFE}}, // 88 RESERVED

        {
            {   0x47, 0x1F, 0xFF}},
        {
            {   0x47, 0x1F, 0xFF}},
        /*127*/
        {
            {   0x47, 0x00, 0x02, 0x18, 0x00, 0x00, 0x00, 0x01, 0xFF}}, // 89 PROGRAM_STREAM_DIRECTORY

#define NUM_NULLS ((2+3+3+2)*3+8)
#else
#define NUM_NULLS ((2+3+3+2)*3)
#endif
        };

void IfsConvertExample(IfsHandle tsbHandle, // Input
        IfsHandle recHandle, // Input
        IfsClock * pBegClock, // Input request/Output actual, in nanoseconds
        IfsClock * pEndClock // Input request/Output actual, in nanoseconds
        );

void IfsConvertExample(IfsHandle tsbHandle, // Input
        IfsHandle recHandle, // Input
        IfsClock * pBegClock, // Input request/Output actual, in nanoseconds
        IfsClock * pEndClock // Input request/Output actual, in nanoseconds
)
{
    const IfsClock doneClock = *pEndClock; // save the requested end clock (in nanoseconds)

    if (IfsConvert(tsbHandle, recHandle, pBegClock, pEndClock))
        exit(-1);

    while (*pEndClock < doneClock)
    {
        // SLEEP HERE

        if (IfsAppend(tsbHandle, recHandle, pEndClock))
            exit(-2);
    }
}

static IfsBoolean ProcessArguments(int argc, char *argv[])
{
    int arg;

    for (arg = 1; arg < argc; arg++)
    {
        char * const pArg = argv[arg];

        if (!strncmp(pArg, "inpPath=", 8))
        {
            pInpPath = g_try_malloc(strlen(pArg) - 8);
            if (pInpPath == NULL)
                return IfsTrue;
            strcpy(pInpPath, &pArg[8]);
        }
        else if (!strncmp(pArg, "outPath=", 8))
        {
            pOutPath = g_try_malloc(strlen(pArg) - 8);
            if (pOutPath == NULL)
                return IfsTrue;
            strcpy(pOutPath, &pArg[8]);
        }
        else if (!strcmp(pArg, "debug=on"))
        {
            debug = IfsTrue;
        }
        else if (!strcmp(pArg, "debug=off"))
        {
            debug = IfsFalse;
        }
        else
        {
            printf("\nIfsSim.exe, version %d\n", INTF_RELEASE_VERSION);
            printf("Options are:\n");
            printf("   inpPath=<input path>  (default is '%s')\n",
                    DEFAULT_INP_PATH);
            printf("   outPath=<output path> (default is '%s')\n",
                    DEFAULT_OUT_PATH);
            printf("   debug=on|off (default is off)\n");
            return IfsTrue;
        }
    }

    if (pInpPath == NULL)
    {
        pInpPath = g_try_malloc(strlen(DEFAULT_INP_PATH) + 1);
        if (pInpPath == NULL)
            return IfsTrue;
        strcpy(pInpPath, DEFAULT_INP_PATH);
    }

    if (pOutPath == NULL)
    {
        pOutPath = g_try_malloc(strlen(DEFAULT_OUT_PATH) + 1);
        if (pOutPath == NULL)
            return IfsTrue;
        strcpy(pOutPath, DEFAULT_OUT_PATH);
    }

    pBackgMpg = g_try_malloc(strlen(pInpPath) + strlen(DEFAULT_BACKG_NAME) + 2);
    if (pBackgMpg == NULL)
        return IfsTrue;
    strcpy(pBackgMpg, pInpPath);
    strcat(pBackgMpg, "/");
    strcat(pBackgMpg, DEFAULT_BACKG_NAME);

    pPlainMpg = g_try_malloc(strlen(pInpPath) + strlen(DEFAULT_PLAIN_NAME) + 2);
    if (pPlainMpg == NULL)
        return IfsTrue;
    strcpy(pPlainMpg, pInpPath);
    strcat(pPlainMpg, "/");
    strcat(pPlainMpg, DEFAULT_PLAIN_NAME);

    printf("\nIfsSim.exe version %d\n", INTF_RELEASE_VERSION);
    printf("The input path is '%s'\n", pInpPath);
    printf("The background input file is '%s'\n", pBackgMpg);
    printf("The plain input file is '%s'\n", pPlainMpg);
    printf("The output path is '%s'\n", pOutPath);
    if (debug)
        printf("The program is in debug mode\n");

    return IfsFalse;
}

static void UnitTest100(char * saveName)                                        // 100.0000 - verify the indexer operation
{
    // This test verifies the indexer operation and verifies the interface
    // behavior of several functions.  It does this by indexing a canned
    // artificial MPEG stream that contains every single indexable event.

    NumPackets numData = sizeof(data) / sizeof(IfsPacket);

    // Output parameters:
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // writer, then reader

    IfsSetMode(IfsIndexDumpModeAll, IfsIndexerSettingVerbose);

    ifsHandle = (IfsHandle) 1;                                                  // 100.0010 - NULL path param to IfsOpenReader
    isne(ifsHandle, NULL);                                                      //
    test(IfsOpenReader(NULL, "test", &ifsHandle),                               //
            IfsReturnCodeBadInputParameter);                                    //
    iseq(ifsHandle, NULL);                                                      //

    ifsHandle = (IfsHandle) 1;                                                  // 100.0020 - NULL path param to IfsOpenWriter
    isne(ifsHandle, NULL);                                                      //
    test(IfsOpenWriter(NULL, NULL, 0, &ifsHandle),                              //
            IfsReturnCodeBadInputParameter);                                    //
    iseq(ifsHandle, NULL);                                                      //

    ifsHandle = (IfsHandle) 1;                                                  // 100.0030 - NULL name param to IfsOpenReader
    isne(ifsHandle, NULL);                                                      //
    test(IfsOpenReader(pOutPath, NULL, &ifsHandle),                             //
            IfsReturnCodeBadInputParameter);                                    //
    iseq(ifsHandle, NULL);                                                      //

    test(IfsOpenWriter(pOutPath, NULL, 0, NULL),                                // 100.0040 - NULL pIfsHandle param to IfsOpenWriter
            IfsReturnCodeBadInputParameter);                                    //

    test(IfsOpenReader(pOutPath, "test", NULL),                                 // 100.0050 - NULL pIfsHandle param to IfsOpenReader
            IfsReturnCodeBadInputParameter);                                    //

    test(IfsOpenWriter(pOutPath, NULL, 0, &ifsHandle),                          // 100.0060 - IfsOpenWriter
            IfsReturnCodeNoErrorReported);                                      //
    isne(ifsHandle, NULL);                                                      //
    if (ifsHandle == NULL)                                                      //
        return;                                                                 //

    test(IfsStart(NULL, 2, 1), IfsReturnCodeBadInputParameter);                 // 100.0070 - NULL ifsHandle param to IfsStart

    test(IfsStart(ifsHandle, 2, 1), IfsReturnCodeNoErrorReported);              // 100.0080 - IfsStart

    pIfsInfo = (IfsInfo *) 1;                                                   // 100.0090 - NULL ifsHandle param to IfsHandleInfo
    isne(pIfsInfo, NULL);                                                       //
    test(IfsHandleInfo(NULL, &pIfsInfo), IfsReturnCodeBadInputParameter);       //
    iseq(pIfsInfo, NULL);                                                       //

    test(IfsHandleInfo(ifsHandle, NULL), IfsReturnCodeBadInputParameter);       // 100.0100 - NULL ppIfsInfo param to IfsHandleInfo

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 100.0110 - IfsHandleInfo
    iseq(pIfsInfo->mpegSize, 0 );                                               //
    iseq(pIfsInfo->ndexSize, 0 );                                               //
    iseq(pIfsInfo->begClock, 0 );                                               //
    iseq(pIfsInfo->endClock, 0 );                                               //
    iseq(pIfsInfo->videoPid, 2 );                                               //
    iseq(pIfsInfo->audioPid, 1 );                                               //
    isne(pIfsInfo->path , NULL);                                                //
    isne(pIfsInfo->name , NULL);                                                //

    strcpy(saveName, pIfsInfo->name);

    test(IfsFreeInfo(NULL), IfsReturnCodeBadInputParameter);                    // 100.0120 - NULL pIfsInfo param to IfsFreeInfo

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  // 100.0130 - IfsFreeInfo

    test(IfsSetMaxSize(NULL, 90*60), IfsReturnCodeBadInputParameter);           // 100.0140 - illegal params to IfsSetMaxSize
    test(IfsSetMaxSize(ifsHandle, 0), IfsReturnCodeBadInputParameter);          //
    test(IfsSetMaxSize(ifsHandle, 90*60), IfsReturnCodeIllegalOperation);       //
    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->mpegSize, 0 );                                               //
    iseq(pIfsInfo->ndexSize, 0 );                                               //
    iseq(pIfsInfo->begClock, 0 );                                               //
    iseq(pIfsInfo->endClock, 0 );                                               //
    iseq(pIfsInfo->videoPid, 2 );                                               //
    iseq(pIfsInfo->audioPid, 1 );                                               //
    isne(pIfsInfo->path , NULL);                                                //
    isne(pIfsInfo->name , NULL);                                                //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    test(IfsWrite(NULL, 2*NSEC_PER_SEC, numData, data),                         // 100.0150 - NULL ifsHandle param to IfsWrite
            IfsReturnCodeBadInputParameter);                                    //

    test(IfsWrite(ifsHandle, 2*NSEC_PER_SEC, numData, NULL),                    // 100.0160 - NULL pData param to IfsWrite
            IfsReturnCodeBadInputParameter);                                    //

    test(IfsWrite(ifsHandle, 2*NSEC_PER_SEC, numData*1/3-numData*0/3,           // 100.0170 - 3 sequential IfsWrite commands
            &data[numData*0/3]), IfsReturnCodeNoErrorReported);                 //
    test(IfsWrite(ifsHandle, 3*NSEC_PER_SEC, numData*2/3-numData*1/3,           //
            &data[numData*1/3]), IfsReturnCodeNoErrorReported);                 //
    test(IfsWrite(ifsHandle, 4*NSEC_PER_SEC, numData*3/3-numData*2/3,           //
            &data[numData*2/3]), IfsReturnCodeNoErrorReported);                 //
    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->mpegSize, (NumBytes)numData*(NumBytes)sizeof(IfsPacket));    //
    iseq(pIfsInfo->ndexSize, ((NumBytes)(numData-NUM_NULLS)*                    //
                              (NumBytes)sizeof(IfsIndexEntry)));                //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->videoPid, 2 );                                               //
    iseq(pIfsInfo->audioPid, 1 );                                               //
    isne(pIfsInfo->path , NULL);                                                //
    isne(pIfsInfo->name , NULL);                                                //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    test(IfsClose(NULL), IfsReturnCodeBadInputParameter);                       // 100.0180 - NULL ifsHandle param to IfsClose

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 100.0190 - IfsClose

    isne(ifsHandle, NULL);                                                      // 100.0200 - illegal maxSize param to IfsOpenWriter
    test(IfsOpenWriter(pOutPath, saveName, 90*60, &ifsHandle),                  //
            IfsReturnCodeBadMaxSizeValue);                                      //
    iseq(ifsHandle, NULL);                                                      //

    test(IfsOpenReader(pOutPath, saveName, &ifsHandle),                         // 100.0210 - IfsOpenReader
            IfsReturnCodeNoErrorReported);                                      //
    isne(ifsHandle, NULL);                                                      //
    if (ifsHandle == NULL)                                                      //
        return;                                                                 //

    test(IfsStart(ifsHandle, 2, 1), IfsReturnCodeMustBeAnIfsWriter);            // 100.0220 - attempt illegal write ops to an IfsReader
    test(IfsSetMaxSize(ifsHandle, 60*60), IfsReturnCodeMustBeAnIfsWriter);      //
    test(IfsStop(ifsHandle), IfsReturnCodeMustBeAnIfsWriter);                   //
    test(IfsWrite(ifsHandle,                                                    //
                    2*NSEC_PER_SEC,                                             //
                    numData*1/3-numData*0/3,                                    //
                    &data[numData*0/3]), IfsReturnCodeMustBeAnIfsWriter);       //
    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->mpegSize, (NumBytes)numData*(NumBytes)sizeof(IfsPacket));    //
    iseq(pIfsInfo->ndexSize, ((NumBytes)(numData-NUM_NULLS)*                    //
                              (NumBytes)sizeof(IfsIndexEntry)));                //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC );                                  //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC );                                  //
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);                                //
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);                                //
    isne(pIfsInfo->path , NULL );                                               //
    isne(pIfsInfo->name , NULL );                                               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 100.0230 - IfsClose

    iseq((IfsIndex)-1, IfsGetWhatAll());                                        // 100.0240 - verify every indexing bit was set by test 100

    IfsSetMode(IfsIndexDumpModeDef, IfsIndexerSettingUnitest);
}

static void UnitTest101(char * saveName)                                        // 101.0000 - verify basic IfsSeekToTime and IfsSeekToPacket operation
{
    // This test verifies the basic operation of the IfsSeekToTime and
    // IfsSeekToPacket functions.  It uses the IFS file (mpeg and index data)
    // created in test 100, seeks to known positions in the file and verifies
    // the results.  It also verifies the interface behavior of the
    // IfsSeekToTime and IfsSeekToPacket functions.

    NumPackets position, numData = sizeof(data) / sizeof(IfsPacket);

    // Output parameters:
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    IfsClock ifsClock;

    test(IfsOpenReader(pOutPath, saveName, &ifsHandle),
            IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    iseq(pIfsInfo->mpegSize, (NumBytes)numData*(NumBytes)sizeof(IfsPacket));
    iseq(pIfsInfo->ndexSize, ((NumBytes)(numData-NUM_NULLS)*
                              (NumBytes)sizeof(IfsIndexEntry)));
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC );
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsSeekToTime(NULL, IfsDirectEither, &ifsClock, NULL),                 // 101.0010 - NULL ifsHandle param to IfsSeekToTime
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsSeekToTime(ifsHandle, IfsDirectEither, NULL, NULL),                 // 101.0020 - NULL pIfsClock param to IfsSeekToTime
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsSeekToTime(ifsHandle, -1, &ifsClock, NULL),                         // 101.0030 - illegal ifsDirect param to IfsSeekToTime
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsSeekToPacket(NULL, 0, NULL),                                        // 101.0040 - NULL ifsHandle param to IfsSeekToPacket
        IfsReturnCodeBadInputParameter);                                        //

    ifsClock = 1 * NSEC_PER_SEC;                                                // 101.0050 - seek outside (before) the file
    test(IfsSeekToTime(ifsHandle, IfsDirectBegin , &ifsClock, NULL),            //
        IfsReturnCodeSeekOutsideFile);                                          //
    test(IfsSeekToTime(ifsHandle, IfsDirectEnd , &ifsClock, NULL),              //
        IfsReturnCodeSeekOutsideFile);                                          //
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),            //
        IfsReturnCodeSeekOutsideFile);                                          //

    ifsClock = 2 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectBegin, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 2*NSEC_PER_SEC);
    iseq(position, 4);
    iseq(ifsHandle->entry.when, 2*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere, 4);
    iseq(ifsHandle->entry.virtWhere, 4);
    iseq(ifsHandle->realLoc, 4);
    iseq(ifsHandle->virtLoc, 4);
    ifsClock = 0;
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 2*NSEC_PER_SEC);

    ifsClock = 2 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectEnd, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 2*NSEC_PER_SEC);
    iseq(position, 39);
    iseq(ifsHandle->entry.when, 2*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere, 39);
    iseq(ifsHandle->entry.virtWhere, 39);
    iseq(ifsHandle->realLoc, 39);
    iseq(ifsHandle->virtLoc, 39);
    ifsClock = 0;
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 2*NSEC_PER_SEC);

    ifsClock = 2 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 2*NSEC_PER_SEC);
    iseq(position, 4);
    iseq(ifsHandle->entry.when, 2*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere, 4);
    iseq(ifsHandle->entry.virtWhere, 4);
    iseq(ifsHandle->realLoc, 4);
    iseq(ifsHandle->virtLoc, 4);
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 2*NSEC_PER_SEC);

    ifsClock = 3 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectBegin, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 3*NSEC_PER_SEC);
    iseq(position, 43);
    iseq(ifsHandle->entry.when, 3*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere, 43);
    iseq(ifsHandle->entry.virtWhere, 43);
    iseq(ifsHandle->realLoc, 43);
    iseq(ifsHandle->virtLoc, 43);
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 3*NSEC_PER_SEC);

    ifsClock = 3 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectEnd, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 3*NSEC_PER_SEC);
    iseq(position, 83);
    iseq(ifsHandle->entry.when, 3*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere, 83);
    iseq(ifsHandle->entry.virtWhere, 83);
    iseq(ifsHandle->realLoc, 83);
    iseq(ifsHandle->virtLoc, 83);
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 3*NSEC_PER_SEC);

    ifsClock = 3 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 3*NSEC_PER_SEC);
    iseq(position, 74);
    iseq(ifsHandle->entry.when, 3*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere, 74);
    iseq(ifsHandle->entry.virtWhere, 74);
    iseq(ifsHandle->realLoc, 74);
    iseq(ifsHandle->virtLoc, 74);
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 3*NSEC_PER_SEC);

    ifsClock = 4 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectBegin, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 4*NSEC_PER_SEC);
    iseq(position, 87);
    iseq(ifsHandle->entry.when, 4*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere, 87);
    iseq(ifsHandle->entry.virtWhere, 87);
    iseq(ifsHandle->realLoc, 87);
    iseq(ifsHandle->virtLoc, 87);
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 4*NSEC_PER_SEC);

    ifsClock = 4 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectEnd, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 4*NSEC_PER_SEC);
    iseq(position, 127);
    iseq(ifsHandle->entry.when, 4*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere,127);
    iseq(ifsHandle->entry.virtWhere,127);
    iseq(ifsHandle->realLoc, 127);
    iseq(ifsHandle->virtLoc, 127);
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 4*NSEC_PER_SEC);

    ifsClock = 4 * NSEC_PER_SEC;
    position = 0;
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, &position),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 4*NSEC_PER_SEC);
    iseq(position, 124);
    iseq(ifsHandle->entry.when, 4*NSEC_PER_SEC);
    iseq(ifsHandle->entry.realWhere,124);
    iseq(ifsHandle->entry.virtWhere,124);
    iseq(ifsHandle->realLoc, 124);
    iseq(ifsHandle->virtLoc, 124);
    test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
        IfsReturnCodeNoErrorReported);
    iseq(ifsClock, 4*NSEC_PER_SEC);

    ifsClock = 5 * NSEC_PER_SEC;                                                // 101.0060 - seek outside (after) the file
    test(IfsSeekToTime(ifsHandle, IfsDirectBegin , &ifsClock, NULL),            //
        IfsReturnCodeSeekOutsideFile);                                          //
    test(IfsSeekToTime(ifsHandle, IfsDirectEnd , &ifsClock, NULL),              //
        IfsReturnCodeSeekOutsideFile);                                          //
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),            //
        IfsReturnCodeSeekOutsideFile);                                          //

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);
}

static void UnitTest102(char * saveName)                                        // 102.0000 - verify basic IfsConvert operation
{
    // This test verifies the basic operation of the IfsConvert function. It
    // uses the IFS file (mpeg and index data) created in test 100, converts
    // various known segments of that file and verifies the results.  It also
    // verifies the interface behavior of the IfsConvert function.

    NumPackets numData = sizeof(data) / sizeof(IfsPacket);

    IfsClock begClock = 0; // nanoseconds
    IfsClock endClock = 0; // nanoseconds

    // Output parameters:
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    IfsHandle dstHandle; // writer

    test(IfsOpenReader(pOutPath, saveName, &ifsHandle),
            IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    iseq(pIfsInfo->mpegSize, (NumBytes)numData*(NumBytes)sizeof(IfsPacket));
    iseq(pIfsInfo->ndexSize, ((NumBytes)(numData-NUM_NULLS)*
                              (NumBytes)sizeof(IfsIndexEntry)));
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC );
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Conv_1_1");
    begClock = 1 * NSEC_PER_SEC;
    endClock = 1 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Conv_1_1", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(NULL, dstHandle, &begClock, &endClock),                     // 102.0010 - NULL srcHandle param to IfsConvert
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsConvert(ifsHandle, NULL, &begClock, &endClock),                     // 102.0020 - NULL dstHandle param to IfsConvert
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsConvert(ifsHandle, dstHandle, NULL, &endClock),                     // 102.0030 - NULL pBegClock param to IfsConvert
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsConvert(ifsHandle, dstHandle, &begClock, NULL),                     // 102.0040 - NULL pEndClock param to IfsConvert
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsConvert(dstHandle, ifsHandle, &begClock, &endClock),                // 102.0050 - swapped src and dst handles to IfsConvert
        IfsReturnCodeMustBeAnIfsWriter);                                        //

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 102.0060 - convert outside (before) the file
            IfsReturnCodeSeekOutsideFile);                                      //
    iseq(begClock, 0*NSEC_PER_SEC);                                             //
    iseq(endClock, 0*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 0*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 0*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 0 );                                               //
    iseq(pIfsInfo->ndexSize, 0 );                                               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //
    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);                    //

    test(IfsDelete(pOutPath, "Conv_1_1"), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Conv_5_5");
    begClock = 5 * NSEC_PER_SEC;
    endClock = 5 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Conv_5_5", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 102.0070 - convert outside (after) the file
            IfsReturnCodeSeekOutsideFile);                                      //
    iseq(begClock, 0*NSEC_PER_SEC);                                             //
    iseq(endClock, 0*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 0*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 0*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 0 );                                               //
    iseq(pIfsInfo->ndexSize, 0 );                                               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //
    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);                    //

    test(IfsDelete(pOutPath, "Conv_5_5"), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Conv_1_3");
    begClock = 1 * NSEC_PER_SEC;
    endClock = 3 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Conv_1_3", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 102.0080 - convert the first two thirds of the file
            IfsReturnCodeNoErrorReported);                                      //
    iseq(begClock, 2*NSEC_PER_SEC);                                             //
    iseq(endClock, 3*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 80*(NumBytes)sizeof(IfsPacket));                   //
    iseq(pIfsInfo->ndexSize, 51*(NumBytes)sizeof(IfsIndexEntry));               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //
    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);                    //

    test(IfsDelete(pOutPath, "Conv_1_3"), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Conv_3_5");
    begClock = 3 * NSEC_PER_SEC;
    endClock = 5 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Conv_3_5", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 102.0090 - convert the last two thirds of the file
            IfsReturnCodeNoErrorReported);                                      //
    iseq(begClock, 3*NSEC_PER_SEC);                                             //
    iseq(endClock, 4*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 85*(NumBytes)sizeof(IfsPacket));                   //
    iseq(pIfsInfo->ndexSize, 69*(NumBytes)sizeof(IfsIndexEntry));               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //
    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);                    //

    test(IfsDelete(pOutPath, "Conv_3_5"), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Conv_1_5");
    begClock = 1 * NSEC_PER_SEC;
    endClock = 5 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Conv_1_5", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 102.0100 - convert the entire file
            IfsReturnCodeNoErrorReported);                                      //
    iseq(begClock, 2*NSEC_PER_SEC);                                             //
    iseq(endClock, 4*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 124*(NumBytes)sizeof(IfsPacket));                  //
    iseq(pIfsInfo->ndexSize, 90*(NumBytes)sizeof(IfsIndexEntry));               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //
    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);                    //

    test(IfsDelete(pOutPath, "Conv_1_5"), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Conv_3_3");
    begClock = 3 * NSEC_PER_SEC;
    endClock = 3 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Conv_3_3", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 102.0110 - convert just the middle third of the file
            IfsReturnCodeNoErrorReported);                                      //
    iseq(begClock, 3*NSEC_PER_SEC);                                             //
    iseq(endClock, 3*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 41*(NumBytes)sizeof(IfsPacket));                   //
    iseq(pIfsInfo->ndexSize, 30*(NumBytes)sizeof(IfsIndexEntry));               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);

    test(IfsDelete(pOutPath, "Conv_3_3"), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);
}

static void UnitTest103(char * saveName)                                        // 103.0000 - verify basic IfsAppend operation
{
    // This test verifies the basic IfsConvert followed by IfsAppend operation.
    // It uses the IFS file (mpeg and index data) created in test 100, converts
    // various known segments of that file, appends to those conversions, and
    // then verifies the results.  It also verifies the interface behavior of
    // the IfsAppend function.

    NumPackets numData = sizeof(data) / sizeof(IfsPacket);

    IfsClock begClock = 0; // nanoseconds
    IfsClock endClock = 0; // nanoseconds

    // Output parameters:
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    IfsHandle dstHandle; // writer

    test(IfsOpenReader(pOutPath, saveName, &ifsHandle),
            IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    iseq(pIfsInfo->mpegSize, (NumBytes)numData*(NumBytes)sizeof(IfsPacket));
    iseq(pIfsInfo->ndexSize, ((NumBytes)(numData-NUM_NULLS)*
                              (NumBytes)sizeof(IfsIndexEntry)));
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC );
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path, NULL );
    isne(pIfsInfo->name, NULL );

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Append_1");

    begClock = 2 * NSEC_PER_SEC;
    endClock = 2 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Append_1", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 103.0010 - convert the first third of the file
            IfsReturnCodeNoErrorReported);                                      //
    iseq(begClock, 2*NSEC_PER_SEC);                                             //
    iseq(endClock, 2*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 36*(NumBytes)sizeof(IfsPacket));                   //
    iseq(pIfsInfo->ndexSize, 21*(NumBytes)sizeof(IfsIndexEntry));               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    test(IfsAppend( NULL, dstHandle, &endClock),                                // 103.0020 - NULL srcHandle param to IfsAppend
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsAppend(ifsHandle, NULL, &endClock),                                 // 103.0030 - NULL dstHandle param to IfsAppend
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsAppend(ifsHandle, dstHandle, NULL),                                 // 103.0040 - NULL pEndClock param to IfsAppend
        IfsReturnCodeBadInputParameter);                                        //

    test(IfsAppend(ifsHandle, ifsHandle, &endClock),                            // 103.0050 - swapped src and dst handles to IfsAppend
        IfsReturnCodeMustBeAnIfsWriter);                                        //

    endClock = 3 * NSEC_PER_SEC;                                                // 103.0060 - append the second third of the file
    test(IfsAppend(ifsHandle, dstHandle, &endClock),                            //
            IfsReturnCodeNoErrorReported);                                      //
    iseq(endClock, 3*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, (36+44)*(NumBytes)sizeof(IfsPacket));              //
    iseq(pIfsInfo->ndexSize, (21+30)*(NumBytes)sizeof(IfsIndexEntry));          //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    endClock = 4 * NSEC_PER_SEC;                                                // 103.0070 - append the third third of the file
    test(IfsAppend(ifsHandle, dstHandle, &endClock),                            //
            IfsReturnCodeNoErrorReported);                                      //
    iseq(endClock, 4*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, (36+44+44)*(NumBytes)sizeof(IfsPacket));           //
    iseq(pIfsInfo->ndexSize, (21+30+39)*(NumBytes)sizeof(IfsIndexEntry));       //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    endClock = 5 * NSEC_PER_SEC;                                                // 103.0080 - attempt to append past the end of the file
    test(IfsAppend(ifsHandle, dstHandle, &endClock),                            //
            IfsReturnCodeNoErrorReported);                                      //
    iseq(endClock, 4*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 2*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, (36+44+44+0)*(NumBytes)sizeof(IfsPacket));         //
    iseq(pIfsInfo->ndexSize, (21+30+39+0)*(NumBytes)sizeof(IfsIndexEntry));     //

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);
    test(IfsDelete(pOutPath, "Append_1"), IfsReturnCodeNoErrorReported);

    (void) IfsDelete(pOutPath, "Append_2");

    begClock = 3 * NSEC_PER_SEC;
    endClock = 3 * NSEC_PER_SEC;
    test(IfsOpenWriter(pOutPath, "Append_2", 0, &dstHandle),
            IfsReturnCodeNoErrorReported);
    isne(dstHandle, NULL);
    if (dstHandle == NULL)
        return;

    test(IfsConvert(ifsHandle, dstHandle, &begClock, &endClock),                // 103.0090 - convert the second third of the file
            IfsReturnCodeNoErrorReported);                                      //
    iseq(begClock, 3*NSEC_PER_SEC);                                             //
    iseq(endClock, 3*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, 41*(NumBytes)sizeof(IfsPacket));                   //
    iseq(pIfsInfo->ndexSize, 30*(NumBytes)sizeof(IfsIndexEntry));               //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    endClock = 4 * NSEC_PER_SEC;                                                // 103.0100 - append the third third of the file
    test(IfsAppend(ifsHandle, dstHandle, &endClock),                            //
            IfsReturnCodeNoErrorReported);                                      //
    iseq(endClock, 4*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, (41+44)*(NumBytes)sizeof(IfsPacket));              //
    iseq(pIfsInfo->ndexSize, (30+39)*(NumBytes)sizeof(IfsIndexEntry))           //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    endClock = 5 * NSEC_PER_SEC;                                                // 103.0110 - attempt to append past the end of the file
    test(IfsAppend(ifsHandle, dstHandle, &endClock),                            //
            IfsReturnCodeNoErrorReported);                                      //
    iseq(endClock, 4*NSEC_PER_SEC);                                             //
    test(IfsHandleInfo(dstHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    //
    iseq(pIfsInfo->begClock, 3*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->endClock, 4*NSEC_PER_SEC);                                   //
    iseq(pIfsInfo->mpegSize, (41+44+0)*(NumBytes)sizeof(IfsPacket));            //
    iseq(pIfsInfo->ndexSize, (30+39+0)*(NumBytes)sizeof(IfsIndexEntry));        //
    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);                  //

    test(IfsClose(dstHandle), IfsReturnCodeNoErrorReported);
    test(IfsDelete(pOutPath, "Append_2"), IfsReturnCodeNoErrorReported);

    test(IfsStop(NULL), IfsReturnCodeBadInputParameter);                        // 103.0120 - NULL ifsHandle param to IfsStop

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    test(IfsDelete(pOutPath, saveName), IfsReturnCodeNoErrorReported);
}

static void UnitTest200(void)                                                   // 200.0000 - test the IfsPathNameInfo function error paths
{
    // This test verifies the interface behavior of the IfsPathNameInfo
    // function.

    IfsInfo * pIfsInfo = (IfsInfo *) 1;

    isne(pIfsInfo, NULL);                                                       // 200.0010 - NULL path param to IfsPathNameInfo
    test(IfsPathNameInfo(NULL, "FindMeIfYouCan", &pIfsInfo),                    //
            IfsReturnCodeBadInputParameter);                                    //
    iseq(pIfsInfo, NULL);                                                       //

    pIfsInfo = (IfsInfo *) 1;                                                   // 200.0020 - NULL name param to IfsPathNameInfo
    isne(pIfsInfo, NULL);                                                       //
    test(IfsPathNameInfo(pOutPath, NULL, &pIfsInfo),                            //
            IfsReturnCodeBadInputParameter);                                    //
    iseq(pIfsInfo, NULL);                                                       //

    test(IfsPathNameInfo(pOutPath, "FindMeIfYouCan", NULL),                     // 200.0030 - NULL ppIfsInfo param to IfsPathNameInfo
            IfsReturnCodeBadInputParameter);                                    //

    pIfsInfo = (IfsInfo *) 1;                                                   // 200.0040 - attempt to find unfindable file
    isne(pIfsInfo, NULL);                                                       //
    test(IfsPathNameInfo(pOutPath, "FindMeIfYouCan", &pIfsInfo),                //
            IfsReturnCodeFileWasNotFound);                                      //
    iseq(pIfsInfo, NULL);                                                       //
}

static void UnitTest300(void)                                                   // 300.0000 - test the IfsRead* function error paths
{
    // This test verifies the interface behavior of the IfsRead,
    // IfsReadNearestPicture, IfsReadNextPicture and IfsReadPreviousPicture
    // functions.

    IfsHandle ifsHandle = (IfsHandle) 1;
    NumPackets numPackets;
    IfsPacket * pData;

    pData = (void *) (numPackets = 1);
    test(IfsRead(NULL, &numPackets, NULL, &pData),                              // 300.0010 - NULL ifsHandle param to IfsRead
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);
    iseq(pData, NULL);
    pData = (void *) (numPackets = 1);
    test(IfsRead(ifsHandle, NULL, NULL, &pData),                                // 300.0020 - NULL pNumPackets param to IfsRead
        IfsReturnCodeBadInputParameter);
    iseq(pData, NULL);
    test(IfsRead(ifsHandle, &numPackets, NULL, NULL),                           // 300.0030 - NULL ppData param to IfsRead
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);

    pData = (void *) (numPackets = 1);
    test(IfsReadNearestPicture(NULL, 0, 0, &numPackets, &pData),                // 300.0040 - NULL ifsHandle param to IfsReadNearestPicture
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);
    iseq(pData, NULL);
    pData = (void *) (numPackets = 1);
    test(IfsReadNearestPicture(ifsHandle, 0, 0, NULL, &pData),                  // 300.0050 - NULL pNumPackets param to IfsReadNearestPicture
        IfsReturnCodeBadInputParameter);
    iseq(pData, NULL);
    test(IfsReadNearestPicture(ifsHandle, 0, 0, &numPackets, NULL),             // 300.0060 - NULL ppData param to IfsReadNearestPicture
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);

    pData = (void *) (numPackets = 1);
    test(IfsReadNextPicture(NULL, 0, 0, &numPackets, &pData),                   // 300.0070 - NULL ifsHandle param to IfsReadNextPicture
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);
    iseq(pData, NULL);
    pData = (void *) (numPackets = 1);
    test(IfsReadNextPicture(ifsHandle, 0, 0, NULL, &pData),                     // 300.0080 - NULL pNumPackets param to IfsReadNextPicture
        IfsReturnCodeBadInputParameter);
    iseq(pData, NULL);
    test(IfsReadNextPicture(ifsHandle, 0, 0, &numPackets, NULL),                // 300.0090 - NULL ppData param to IfsReadNextPicture
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);

    pData = (void *) (numPackets = 1);
    test(IfsReadPreviousPicture(NULL, 0, 0, &numPackets, &pData),               // 300.0100 - NULL ifsHandle param to IfsReadPreviousPicture
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);
    iseq(pData, NULL);
    pData = (void *) (numPackets = 1);
    test(IfsReadPreviousPicture(ifsHandle, 0, 0, NULL, &pData),                 // 300.0110 - NULL pNumPackets param to IfsReadPreviousPicture
        IfsReturnCodeBadInputParameter);
    iseq(pData, NULL);
    test(IfsReadPreviousPicture(ifsHandle, 0, 0, &numPackets, NULL),            // 300.0120 - NULL ppData param to IfsReadPreviousPicture
        IfsReturnCodeBadInputParameter);
    iseq(numPackets, 0);
}

static void UnitTest400(void)                                                   // 400.0000 - test PMT remapping
{
    // This test sends a series of artificial packets through the remap
    // system and verifies the results.  The 13 packets sent are/contain:
    //
    //  1    1FFF NULL
    //  2    0064 PMT (PCR=07D0, CA=07D0, video=07D0, audio=07D1)
    //  3    1FFF NULL
    //  4    0064 PMT (PCR=07...
    //  5    1FFF NULL
    //  6    0064 PMT     ...D0, CA=07...
    //  7    1FFF NULL
    //  8    0064 PMT              ...D0, video=07...
    //  9    1FFF NULL
    // 10    0064 PMT                          ...D0, audio=07...
    // 11    1FFF NULL
    // 12    0064 PMT                                      ...D1)
    // 13    1FFF NULL
    //
    // After PMT remapping the expected result is:
    //
    //  1    1FFF NULL
    //  2    03E8 PMT (PCR=00C8, CA=00C8, video=00C8, audio=00C9)
    //  3    1FFF NULL
    //  4    03E8 PMT (PCR=00...
    //  5    1FFF NULL
    //  6    03E8 PMT     ...C8, CA=00...
    //  7    1FFF NULL
    //  8    03E8 PMT              ...C8, video=00...
    //  9    1FFF NULL
    // 10    03E8 PMT                          ...C8, audio=00...
    // 11    1FFF NULL
    // 12    03E8 PMT                                      ...C9)
    // 13    1FFF NULL
    //
    // And after filtering out the NULL packets the expected result is:
    //
    //  1    03E8 PMT (PCR=00C8, CA=00C8, video=00C8, audio=00C9)
    //  2    03E8 PMT (PCR=00...
    //  3    03E8 PMT     ...C8, CA=00...
    //  4    03E8 PMT              ...C8, video=00...
    //  5    03E8 PMT                          ...C8, audio=00...
    //  6    03E8 PMT                                      ...C9)
    //
    // Notes on plain.mpg:
    //
    // PAT table_id                 0x00
    // PAT section_syntax_indicator 1
    // PAT section_length           25
    // PAT transport_stream_id      0x054D (1357)
    // PAT version_number           0
    // PAT current_next_indicator   1
    // PAT section_number           0
    // PAT last_section_number      0
    // PAT program_number           1
    // PAT PID                      0x0064 (100)
    // PAT program_number           2
    // PAT PID                      0x0065 (101)
    // PAT program_number           3
    // PAT PID                      0x0066 (102)
    // PAT program_number           4
    // PAT PID                      0x0067 (103)
    // PAT CRC_32                   0xD46D3CBC
    //
    // PMT table_id                 0x02
    // PMT section_syntax_indicator 1
    // PMT section_length           60
    // PMT transport_stream_id      0x0001 (1)
    // PMT version_number           0
    // PMT current_next_indicator   1
    // PMT section_number           0
    // PMT last_section_number      0
    // PMT PCR_PID                  0x07D0 (2000)
    // PMT program_info_length      37
    // PMT descriptor:              129 = [User Private] (3) 0x08 0xBC 0x1B
    // PMT descriptor:              5 = [registration] (4) 0x47 0x41 0x39 0x34
    // PMT descriptor:              135 = [User Private] (24) 0xC1 0x01 0x01
    //                              0x00 0xF4 0x12 0x01 0x65 0x6E 0x67 0x01
    //                              0x00 0x3F 0x0A 0x00 0x54 0x00 0x56 0x00
    //                              0x2D 0x00 0x31 0x00 0x34
    // PMT stream_type              0x02 = [ITU-T Rec. H.262 | ISO/IEC 13818-2
    //                              Video or ISO/IEC 11172-2 constrained
    //                              parameter video stream]
    // PMT elementary_PID           0x07D0 (2000)
    // PMT ES_info_length           0
    // PMT stream_type              0x81 = [User Private]
    // PMT elementary_PID           0x07D1 (2001)
    // PMT ES_info_length           0
    // PMT CRC_32                   0xAA8B2805

    RemapProg programNumber = 1;
    RemapPid oldPmtPid = 100;
    RemapPid newPmtPid = 1000;
    NumPairs numPairs = 4;

    size_t i;
    RemapHandle remapHandle;
    NumPackets prevNumPackets;
    RemapPacket ** ppPrevPointers;

    RemapPacket inpPackets[] =
    {
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (1 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x00)), // 5 bits MSB of the PID
            ((0x64)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x00)), // 4 bits Continuity counter
            ((0x00)), // 8 bits point

            0x02, // PMT table_id
            1 << 7, // PMT section_syntax_indicator
            60 + 8, // PMT section_length
            0x00, 0x01, // PMT transport_stream_id
            (0 << 1) | // PMT version_number
                    1, // PMT current_next_indicator
            0, // PMT section_number
            0, // PMT last_section_number
            0x07, 0xD0, // PMT PCR_PID
            0, 37 + 8, // PMT program_info_length

            129, 3, 0x08, 0xBC, 0x1B, // PMT descriptor
            5, 4, 0x47, 0x41, 0x39, 0x34, // PMT descriptor
            135, 24, 0xC1, 0x01, 0x01, 0x00, // PMT descriptor
            0xF4, 0x12, 0x01, 0x65, //
            0x6E, 0x67, 0x01, 0x00, //
            0x3F, 0x0A, 0x00, 0x54, //
            0x00, 0x56, 0x00, 0x2D, //
            0x00, 0x31, 0x00, 0x34, //
            9, 6, 0xDE, 0xAD, 0x07, 0xD0, // PMT CA descriptor (has the CA_PID!)
            0x00, 0x00,

            0x02, // PMT stream_type
            0x07, 0xD0, // PMT elementary_PID
            0, 0, // PMT ES_info_length
            0x81, // PMT stream_type
            0x07, 0xD1, // PMT elementary_PID
            0, 0, // PMT ES_info_length
            0xDE, 0xAD, 0xC0, 0xDE, // PMT CRC_32
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (1 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x00)), // 5 bits MSB of the PID
            ((0x64)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x01)), // 4 bits Continuity counter
            ((0xAE)), // 8 bits point

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            0x02, // PMT table_id
            1 << 7, // PMT section_syntax_indicator
            60 + 8, // PMT section_length
            0x00, 0x01, // PMT transport_stream_id
            (0 << 1) | // PMT version_number
                    1, // PMT current_next_indicator
            0, // PMT section_number
            0, // PMT last_section_number
            0x07, // PMT PCR_PID MSB
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x00)), // 5 bits MSB of the PID
            ((0x64)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (3 << 4) | // 2 bits Adaptation Field Control
                    (0x02)), // 4 bits Continuity counter
            ((0x8A)), // 8 bits AF length

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            0xD0, // PMT PCR_PID LSB
            0, 37 + 8, // PMT program_info_length

            129, 3, 0x08, 0xBC, 0x1B, // PMT descriptor
            5, 4, 0x47, 0x41, 0x39, 0x34, // PMT descriptor
            135, 24, 0xC1, 0x01, 0x01, 0x00, // PMT descriptor
            0xF4, 0x12, 0x01, 0x65, //
            0x6E, 0x67, 0x01, 0x00, //
            0x3F, 0x0A, 0x00, 0x54, //
            0x00, 0x56, 0x00, 0x2D, //
            0x00, 0x31, 0x00, 0x34, //
            9, 6, 0xDE, 0xAD, 0x07, // PMT CA descriptor (MSB of CA PID)
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x00)), // 5 bits MSB of the PID
            ((0x64)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (3 << 4) | // 2 bits Adaptation Field Control
                    (0x03)), // 4 bits Continuity counter
            ((0xB2)), // 8 bits AF length

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00,

            0xD0, 0x00, 0x00, // PMT CA descriptor (LSB of CA PID)
            0x02, // PMT stream_type
            0x07, // PMT elementary_PID MSB
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x00)), // 5 bits MSB of the PID
            ((0x64)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (3 << 4) | // 2 bits Adaptation Field Control
                    (0x04)), // 4 bits Continuity counter
            ((0xB2)), // 8 bits AF length

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00,

            0xD0, // PMT elementary_PID LSB
            0, 0, // PMT ES_info_length
            0x81, // PMT stream_type
            0x07, // PMT elementary_PID MSB
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x00)), // 5 bits MSB of the PID
            ((0x64)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x05)), // 4 bits Continuity counter

            0xD1, // PMT elementary_PID LSB
            0, 0, // PMT ES_info_length
            0xDE, 0xAD, 0xC0, 0xDE, // PMT CRC_32
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } }, };

    RemapPacket outPackets[] =
    {
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (1 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x03)), // 5 bits MSB of the PID
            ((0xE8)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x00)), // 4 bits Continuity counter
            ((0x00)), // 8 bits point

            0x02, // PMT table_id
            1 << 7, // PMT section_syntax_indicator
            60 + 8, // PMT section_length
            0x00, 0x01, // PMT transport_stream_id
            (0 << 1) | // PMT version_number
                    1, // PMT current_next_indicator
            0, // PMT section_number
            0, // PMT last_section_number
            0x00, 0xC8, // PMT PCR_PID
            0, 37 + 8, // PMT program_info_length

            129, 3, 0x08, 0xBC, 0x1B, // PMT descriptor
            5, 4, 0x47, 0x41, 0x39, 0x34, // PMT descriptor
            135, 24, 0xC1, 0x01, 0x01, 0x00, // PMT descriptor
            0xF4, 0x12, 0x01, 0x65, //
            0x6E, 0x67, 0x01, 0x00, //
            0x3F, 0x0A, 0x00, 0x54, //
            0x00, 0x56, 0x00, 0x2D, //
            0x00, 0x31, 0x00, 0x34, //
            9, 6, 0xDE, 0xAD, 0x00, 0xC8, // PMT CA descriptor (has the CA_PID!)
            0x00, 0x00,

            0x02, // PMT stream_type
            0x00, 0xC8, // PMT elementary_PID
            0, 0, // PMT ES_info_length
            0x81, // PMT stream_type
            0x00, 0xC9, // PMT elementary_PID
            0, 0, // PMT ES_info_length
            0x1F, 0x8A, 0xC4, 0x22, // PMT CRC_32
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (1 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x03)), // 5 bits MSB of the PID
            ((0xE8)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x01)), // 4 bits Continuity counter
            ((0xAE)), // 8 bits point

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            0x02, // PMT table_id
            1 << 7, // PMT section_syntax_indicator
            60 + 8, // PMT section_length
            0x00, 0x01, // PMT transport_stream_id
            (0 << 1) | // PMT version_number
                    1, // PMT current_next_indicator
            0, // PMT section_number
            0, // PMT last_section_number
            0x00, // PMT PCR_PID MSB
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x03)), // 5 bits MSB of the PID
            ((0xE8)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (3 << 4) | // 2 bits Adaptation Field Control
                    (0x02)), // 4 bits Continuity counter
            ((0x8A)), // 8 bits AF length

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            0xC8, // PMT PCR_PID LSB
            0, 37 + 8, // PMT program_info_length

            129, 3, 0x08, 0xBC, 0x1B, // PMT descriptor
            5, 4, 0x47, 0x41, 0x39, 0x34, // PMT descriptor
            135, 24, 0xC1, 0x01, 0x01, 0x00, // PMT descriptor
            0xF4, 0x12, 0x01, 0x65, //
            0x6E, 0x67, 0x01, 0x00, //
            0x3F, 0x0A, 0x00, 0x54, //
            0x00, 0x56, 0x00, 0x2D, //
            0x00, 0x31, 0x00, 0x34, //
            9, 6, 0xDE, 0xAD, 0x00, // PMT CA descriptor (MSB of CA PID)
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x03)), // 5 bits MSB of the PID
            ((0xE8)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (3 << 4) | // 2 bits Adaptation Field Control
                    (0x03)), // 4 bits Continuity counter
            ((0xB2)), // 8 bits AF length

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00,

            0xC8, 0x00, 0x00, // PMT CA descriptor (LSB of CA PID)
            0x02, // PMT stream_type
            0x00, // PMT elementary_PID MSB
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    {
            ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x03)), // 5 bits MSB of the PID
            ((0xE8)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (3 << 4) | // 2 bits Adaptation Field Control
                    (0x04)), // 4 bits Continuity counter
            ((0xB2)), // 8 bits AF length

            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00,

            0xC8, // PMT elementary_PID LSB
            0, 0, // PMT ES_info_length
            0x81, // PMT stream_type
            0x00, // PMT elementary_PID MSB
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x03)), // 5 bits MSB of the PID
            ((0xE8)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x05)), // 4 bits Continuity counter

            0xC9, // PMT elementary_PID LSB
            0, 0, // PMT ES_info_length
            0x1F, 0x8A, 0xC4, 0x22, // PMT CRC_32
            } },
    {
    { ((0x47)), // 8 bits sync byte
            ((0 << 7) | // 1 bit  Transport Error Indicator
                    (0 << 6) | // 1 bit  Payload Unit Start Indicator
                    (0 << 5) | // 1 bit  Transport Priority
                    (0x1F)), // 5 bits MSB of the PID
            ((0xFF)), // 8 bits LSB of the PID
            ((0 << 6) | // 2 bits Scrambling control
                    (1 << 4) | // 2 bits Adaptation Field Control
                    (0x0F)), // 4 bits Continuity counter
            } }, };

    const size_t numInpPackets = sizeof(inpPackets) / sizeof(RemapPacket);

    RemapPacket * pointers[numInpPackets];
    RemapPacket tmpPackets[numInpPackets];

    RemapPair remapPairs[4];

    remapPairs[0].oldPid = 0;    // Old PAT
    remapPairs[0].newPid = 0;    // New PAT
    remapPairs[1].oldPid = 100;  // Old PMT
    remapPairs[1].newPid = 1000; // New PMT
    remapPairs[2].oldPid = 2000; // Old Video/PCR
    remapPairs[2].newPid = 200;  // New Video/PCR
    remapPairs[3].oldPid = 2001; // Old Audio
    remapPairs[3].newPid = 201;  // New Audio

    testRemap(RemapOpen(&remapHandle), RemapReturnCodeNoErrorReported);
    isne(remapHandle, NULL);
    testRemap(RemapAndFilterPids(remapHandle, numPairs, remapPairs),
        RemapReturnCodeNoErrorReported);
    testRemap(RemapPmts(remapHandle, oldPmtPid), RemapReturnCodeNoErrorReported);
    testRemap(RemapPats(remapHandle, programNumber, newPmtPid),
        RemapReturnCodeNoErrorReported)

    memcpy(tmpPackets, inpPackets, sizeof(inpPackets));

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0010 - remap all the packets in one operation
                    NULL,
                    numInpPackets,
                    tmpPackets,
                    &pointers[0],
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);

    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);

    testRemap(RemapAndFilter(remapHandle,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported)

    iseq(prevNumPackets, 6);
    iseq(ppPrevPointers, &pointers[0]);

    for (i = 0; i < numInpPackets * sizeof(RemapPacket); i++)
    {
        iseq(((unsigned char *)tmpPackets)[i],
             ((unsigned char *)outPackets)[i]);
    }

    memcpy(tmpPackets, inpPackets, sizeof(inpPackets));

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0020 - no residual, one non-PMT packet
                    NULL,
                    1,
                    &tmpPackets[0],
                    &pointers[0],
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0030 - no residual, one PMT packet
                    NULL,
                    1,
                    &tmpPackets[1],
                    &pointers[1],
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, &pointers[0]);

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0040 - non PMT residual, no packets
                    &tmpPackets[2],
                    0,
                    NULL,
                    &pointers[2],
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 1);
    iseq(ppPrevPointers, &pointers[1]);

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0050 - PMT residual, no packets
                    &tmpPackets[3],
                    0,
                    NULL,
                    &pointers[3],
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, &pointers[2]);

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0060 - non PMT residual, two packets (one PMT, one not)
                    &tmpPackets[4],
                    2,
                    &tmpPackets[5], // and 6
                    &pointers[4], // 5 and 6
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 1);
    iseq(ppPrevPointers, &pointers[3]);

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0070 - PMT residual, one non PMT packet
                    &tmpPackets[7],
                    1,
                    &tmpPackets[8],
                    &pointers[7], // and 8
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 1);
    iseq(ppPrevPointers, &pointers[4]);

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0080 - PMT residual, three packets (one PMT, two not)
                    &tmpPackets[9],
                    3,
                    &tmpPackets[10], // 11 and 12
                    &pointers[9], // 10, 11 and 12
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 1);
    iseq(ppPrevPointers, &pointers[7]);

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0090 - request last packet info
                    NULL,
                    0,
                    NULL,
                    NULL,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported)
    iseq(prevNumPackets, 2);
    iseq(ppPrevPointers, &pointers[9]);

    for (i = 0; i < numInpPackets * sizeof(RemapPacket); i++)
    {
        iseq(((unsigned char *)tmpPackets)[i], ((unsigned char *)outPackets)[i]);
    }

    // Try to send one at a time (this will cause CRC errors)

    memcpy(tmpPackets, inpPackets, sizeof(inpPackets));

    testRemap(RemapAndFilter(remapHandle,                                            // 400.0100 - test remap fail due to buffer pipeline constraint case
                    NULL,
                    1,
                    &tmpPackets[0],
                    &pointers[0],
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);
    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);
    for (i = 1; i < numInpPackets; i++)
    {
        testRemap(RemapAndFilter(remapHandle,
                        NULL,
                        1,
                        &tmpPackets[i],
                        &pointers[i],
                        &prevNumPackets,
                        &ppPrevPointers),
                RemapReturnCodeNoErrorReported);
        iseq(prevNumPackets, i&1 ? 0 : 1);
        iseq(ppPrevPointers, &pointers[i-1]);
    }
    // Request last packet info
    testRemap(RemapAndFilter(remapHandle,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported)
    iseq(prevNumPackets, i&1 ? 0 : 1);
    iseq(ppPrevPointers, &pointers[i-1]);

    for (i = 0; i < numInpPackets * sizeof(RemapPacket); i++)
    {
        const unsigned char got = ((unsigned char *) tmpPackets)[i];
        const unsigned char exp = ((unsigned char *) outPackets)[i];

        if (got != exp)
        {
            if ((((i + 1) % sizeof(IfsPacket) == 0) && (got == 0x07) && (exp
                    == 0x00)) || ((got == 0xD0) && (exp == 0xC8)) || ((got
                    == 0xD1) && (exp == 0xC9)))
            {
                printf(
                        "Got an expected miscompare at %4d:  0x%02X != 0x%02X\n",
                        i, got, exp);
            }
            else
            {
                iseq(got, exp);
            }
        }
    }

    testRemap(RemapClose(remapHandle,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);

    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);
}

static void UnitTest450(void)                                                   // 450.0000 - test interface for all Remap* functions
{
    // This test verifies the interface behavior of  the RemapOpen,
    // RemapAndFilterPids, RemapPmts, RemapPats, RemapAndFilter and RemapClose
    // functions.

    RemapHandle remapHandle = NULL;
    NumPackets prevNumPackets = 1;
    RemapPacket ** pPrevPointers = (RemapPacket **) 1;
    NumPairs numPairs = 4;
    RemapProg programNumber = 2;
    RemapPid oldPmtPid = 101;
    RemapPid newPmtPid = 1001;
    NumPackets nextNumPackets = 3;
    RemapPacket residualPacket;

    // Arrays that need to be initialized
    RemapPair remapPairs[numPairs + 1];
    RemapPacket nextPackets[nextNumPackets];
    RemapPacket * nextPointers[nextNumPackets];

    remapPairs[0].oldPid = 101;
    remapPairs[0].newPid = 1001;
    remapPairs[1].oldPid = 3000;
    remapPairs[1].newPid = 300;
    remapPairs[2].oldPid = 3001;
    remapPairs[2].newPid = 301;
    remapPairs[3].oldPid = 3002;
    remapPairs[3].newPid = 302;
    remapPairs[4].oldPid = 0;
    remapPairs[4].newPid = 1234;

    testRemap(RemapOpen( NULL), RemapReturnCodeBadInputParameter);                   // 450.0010 - NULL pRemapHandle to RemapOpen

    testRemap(RemapOpen(&remapHandle), RemapReturnCodeNoErrorReported);              // 450.0020 - RemapOpen
    isne(remapHandle, NULL);

    testRemap(RemapAndFilterPids( NULL, numPairs, remapPairs),                       // 450.0030 - NULL remapHandle to RemapAndFilterPids
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilterPids(remapHandle, 0, NULL),                              // 450.0040 - pass none case A to remapAndFilterPids
        RemapReturnCodeNoErrorReported);
    testRemap(RemapAndFilterPids(remapHandle, 0, remapPairs),                        // 450.0050 - pass none case B to remapAndFilterPids
        RemapReturnCodeNoErrorReported);
    testRemap(RemapAndFilterPids(remapHandle, numPairs, NULL),                       // 450.0060 - pass all case to remapAndFilterPids
        RemapReturnCodeNoErrorReported);
    testRemap(RemapAndFilterPids(remapHandle, numPairs+1, remapPairs),               // 450.0070 - attempt to remap PID 0 (the PAT PID)
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilterPids(remapHandle, numPairs, remapPairs),                 // 450.0080 - pass selected PIDs case to remapAndFilterPids
        RemapReturnCodeNoErrorReported);

    testRemap(RemapPmts( NULL, oldPmtPid), RemapReturnCodeBadInputParameter);        // 450.0090 - NULL remapHandle to RemapPmts

    testRemap(RemapPmts(remapHandle, oldPmtPid), RemapReturnCodeNoErrorReported);    // 450.0100 - turn on PMT remapping

    testRemap(RemapPats( NULL, programNumber, newPmtPid),                            // 450.0110 - NULL remapHandle to RemapPats
        RemapReturnCodeBadInputParameter);
    testRemap(RemapPats(remapHandle, programNumber, newPmtPid),                      // 450.0120 - turn on program (PAT) remapping
        RemapReturnCodeNoErrorReported);

    testRemap(RemapAndFilter( NULL, &residualPacket, nextNumPackets, nextPackets,    // 450.0130 - NULL remapHandle to RemapAndFilter
        nextPointers, &prevNumPackets, &pPrevPointers),
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilter(remapHandle, NULL, nextNumPackets, NULL,                // 450.0140 - NULL pResidualPacket and pNextPackets to RemapAndFilter
        nextPointers, &prevNumPackets, &pPrevPointers),
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilter(remapHandle, NULL, nextNumPackets, nextPackets,         // 450.0150 - NULL pResidualPacket and pNextPointers to RemapAndFilter
        NULL, &prevNumPackets, &pPrevPointers),
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilter(remapHandle, &residualPacket, nextNumPackets, NULL,     // 450.0160 - NULL pNextPackets to RemapAndFilter
        nextPointers, &prevNumPackets, &pPrevPointers),
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilter(remapHandle, &residualPacket, nextNumPackets,           // 450.0170 - NULL pNextPointers to RemapAndFilter
        nextPackets, NULL, &prevNumPackets, &pPrevPointers),
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilter(remapHandle, &residualPacket, nextNumPackets,           // 450.0180 - NULL pPrevNumPackets to RemapAndFilter
        nextPackets, nextPointers, NULL, &pPrevPointers),
        RemapReturnCodeBadInputParameter);
    testRemap(RemapAndFilter(remapHandle, &residualPacket, nextNumPackets,           // 450.0190 - NULL ppPrevPointers to RemapAndFilter
        nextPackets, nextPointers, &prevNumPackets, NULL),
        RemapReturnCodeBadInputParameter);

    testRemap(RemapClose( NULL, &prevNumPackets, &pPrevPointers),                    // 450.0200 - NULL remapHandle to RemapClose
        RemapReturnCodeBadInputParameter);
    testRemap(RemapClose(remapHandle, NULL, &pPrevPointers),                         // 450.0210 - NULL pPrevNumPackets to RemapClose
        RemapReturnCodeBadInputParameter);
    testRemap(RemapClose(remapHandle, &prevNumPackets, NULL),                        // 450.0220 - NULL ppPrevPointers to RemapClose
        RemapReturnCodeBadInputParameter);
    testRemap(RemapClose(remapHandle, &prevNumPackets, &pPrevPointers),              // 450.0230 - RemapClose
        RemapReturnCodeNoErrorReported);

    iseq(prevNumPackets, 0);
    iseq(pPrevPointers, NULL);
}

static void UnitTest500(void)                                                   // 500.0000 - remap plain.mpg in drop all mode and verify the results
{
    // This test processes the entire plain.mpg file with the pid filter set up
    // to drop all the packets and then verifies that all the packets were
    // dropped.

    FILE * const pInFile = fopen(pPlainMpg, "rb+");

    size_t i = 2;
    IfsBoolean firstOne = IfsTrue;
    RemapHandle remapHandle;
    NumPackets nextNumPackets;
    RemapPacket packets[2][16];
    RemapPacket * pointers[2][16];
    NumPackets prevNumPackets;
    RemapPacket ** ppPrevPointers;

    NumPackets totalPassed = 0;
    NumPackets totalDropped = 0;

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    testRemap(RemapOpen(&remapHandle), RemapReturnCodeNoErrorReported);
    testRemap(RemapAndFilterPids(remapHandle, 0, NULL),
        RemapReturnCodeNoErrorReported);
    isne(remapHandle, NULL);

    while ((nextNumPackets = fread(&packets[i & 1][0],
            IFS_TRANSPORT_PACKET_SIZE, 16, pInFile)) != 0)
    {
        testRemap(RemapAndFilter(remapHandle,
                        NULL,
                        nextNumPackets,
                        &packets[i&1][0],
                        &pointers[i&1][0],
                        &prevNumPackets,
                        &ppPrevPointers),
                RemapReturnCodeNoErrorReported)
        i++;

        if (firstOne)
        {
            iseq(prevNumPackets, 0);
            iseq(ppPrevPointers, NULL);
            firstOne = IfsFalse;
        }
        else
        {
            iseq(ppPrevPointers, &pointers[i&1][0]);
            totalPassed += prevNumPackets;
            totalDropped += 16 - prevNumPackets;
        }
    }
    // Get the last packet
    testRemap(RemapAndFilter(remapHandle,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported)

    iseq(ppPrevPointers, &pointers[(i+1)&1][0]);
    printf("%6ld passed, %6ld dropped\n", totalPassed += prevNumPackets,
            totalDropped);

    iseq(totalPassed, 0);
    isne(totalDropped, 0);

    testRemap(RemapClose(remapHandle,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);

    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);

    fclose(pInFile);
}

static void UnitTest600(void)                                                   // 600.0000 - remap plain.mpg in pass all mode and verify the results
{
    // This test processes the entire plain.mpg file with the pid filter set up
    // to pass all the packets and then verifies that all the packets were
    // passed.

    FILE * const pInFile = fopen(pPlainMpg, "rb+");

    size_t i = 2;
    IfsBoolean firstOne = IfsTrue;
    RemapHandle remapHandle;
    NumPackets nextNumPackets;
    RemapPacket packets[2][16];
    RemapPacket * pointers[2][16];
    NumPackets prevNumPackets;
    RemapPacket ** ppPrevPointers;

    NumPackets totalPassed = 0;
    NumPackets totalDropped = 0;

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    testRemap(RemapOpen(&remapHandle), RemapReturnCodeNoErrorReported);
    testRemap(RemapAndFilterPids(remapHandle, 1, NULL),
        RemapReturnCodeNoErrorReported);
    isne(remapHandle, NULL);

    while ((nextNumPackets = fread(&packets[i & 1][0],
            IFS_TRANSPORT_PACKET_SIZE, 16, pInFile)) != 0)
    {
        testRemap(RemapAndFilter(remapHandle,
                        NULL,
                        nextNumPackets,
                        &packets[i&1][0],
                        &pointers[i&1][0],
                        &prevNumPackets,
                        &ppPrevPointers),
                RemapReturnCodeNoErrorReported)
        i++;

        if (firstOne)
        {
            iseq(prevNumPackets, 0);
            iseq(ppPrevPointers, NULL);
            firstOne = IfsFalse;
        }
        else
        {
            iseq(ppPrevPointers, &pointers[i&1][0]);
            totalPassed += prevNumPackets;
            totalDropped += 16 - prevNumPackets;
        }
    }
    // Get the last packet
    testRemap(RemapAndFilter(remapHandle,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported)

    iseq(ppPrevPointers, &pointers[(i+1)&1][0]);
    printf("%6ld passed, %6ld dropped\n", totalPassed += prevNumPackets,
            totalDropped);

    isne(totalPassed, 0);
    iseq(totalDropped, 0);

    testRemap(RemapClose(remapHandle,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);

    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);

    fclose(pInFile);
}

static void UnitTest7XX(const char * fileName, RemapProg programNumber,         // 7XX.0000 - subroutine used by tests 700 - 704
        RemapPid oldPmtPid, RemapPid newPmtPid, NumPairs numPairs,
        RemapPair * pRemapPairs)
{
    FILE * const pInFile = fopen(fileName, "rb+");

    size_t i = 2;
    IfsBoolean firstOne = IfsTrue;
    RemapHandle remapHandle;
    NumPackets nextNumPackets;
    RemapPacket packets[2][16];
    RemapPacket * pointers[2][16];
    NumPackets prevNumPackets;
    RemapPacket ** ppPrevPointers;

    NumPackets totalPassed = 0;
    NumPackets totalDropped = 0;

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    testRemap(RemapOpen(&remapHandle), RemapReturnCodeNoErrorReported);
    testRemap(RemapAndFilterPids(remapHandle, numPairs, pRemapPairs),
        RemapReturnCodeNoErrorReported);
    testRemap(RemapPmts(remapHandle, oldPmtPid), RemapReturnCodeNoErrorReported);
    testRemap(RemapPats(remapHandle, programNumber, newPmtPid),
        RemapReturnCodeNoErrorReported)

    isne(remapHandle, NULL);

    while ((nextNumPackets = fread(&packets[i & 1][0],
            IFS_TRANSPORT_PACKET_SIZE, 16, pInFile)) != 0)
    {
        testRemap(RemapAndFilter(remapHandle,
                        NULL,
                        nextNumPackets,
                        &packets[i&1][0],
                        &pointers[i&1][0],
                        &prevNumPackets,
                        &ppPrevPointers),
                RemapReturnCodeNoErrorReported)
        i++;

        if (firstOne)
        {
            iseq(prevNumPackets, 0);
            iseq(ppPrevPointers, NULL);
            firstOne = IfsFalse;
        }
        else
        {
            iseq(ppPrevPointers, &pointers[i&1][0]);
            totalPassed += prevNumPackets;
            totalDropped += 16 - prevNumPackets;
        }
    }
    // Get the last packet
    testRemap(RemapAndFilter(remapHandle,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported)

    iseq(ppPrevPointers, &pointers[(i+1)&1][0]);
    printf("%6ld passed, %6ld dropped\n", totalPassed += prevNumPackets,
            totalDropped += 16 - prevNumPackets);

    testRemap(RemapClose(remapHandle,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);

    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);

    fclose(pInFile);
}

static void UnitTest700(void)                                                   // 700.0000 - remap and filter program 1 of background.mpg
{
    // This test processes the entire background.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //       66     660     PMT
    //       68     680     Video/PCR
    //
    // and the remapper set up to remap PATs and PMTs.
    //
    // background.mpg:
    //
    // PAT table_id                 0x00
    // PAT section_syntax_indicator 1
    // PAT section_length           13
    // PAT transport_stream_id      0x3D6C (15724)
    // PAT version_number           8
    // PAT current_next_indicator   1
    // PAT section_number           0
    // PAT last_section_number      0
    // PAT program_number           1
    // PAT PID                      0x0042 (66)
    // PAT CRC_32                   0x74A1B009
    //
    // PMT table_id                 0x02
    // PMT section_syntax_indicator 1
    // PMT section_length           18
    // PMT transport_stream_id      0x0001 (1)
    // PMT version_number           25
    // PMT current_next_indicator   1
    // PMT section_number           0
    // PMT last_section_number      0
    // PMT PCR_PID                  0x0044 (68)
    // PMT program_info_length      0
    // PMT stream_type              0x02 = [ITU-T Rec. H.262 | ISO/IEC 13818-2
    //                              Video or ISO/IEC 11172-2 constrained
    //                              parameter video stream]
    // PMT elementary_PID           0x0044 (68)
    // PMT ES_info_length           0
    // PMT CRC_32                   0x9B56DCAD 0x9B56DCAD

    RemapPair remapPairs[3];

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 66; // Old PMT
    remapPairs[1].newPid = 660; // New PMT
    remapPairs[2].oldPid = 68; // Old Video/PCR
    remapPairs[2].newPid = 680; // New Video/PCR

    UnitTest7XX(pBackgMpg, 1, 66, 660, 3, remapPairs);
}

static void UnitTest701(void)                                                   // 701.0000 - remap and filter program 1 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      100    1000     PMT
    //     2000     200     Video/PCR
    //     2001     201     Audio
    //
    // and the remapper set up to remap PATs and PMTs.
    //
    // plain.mpg:
    //
    // PAT table_id                 0x00
    // PAT section_syntax_indicator 1
    // PAT section_length           25
    // PAT transport_stream_id      0x054D (1357)
    // PAT version_number           0
    // PAT current_next_indicator   1
    // PAT section_number           0
    // PAT last_section_number      0
    // PAT program_number           1
    // PAT PID                      0x0064 (100)
    // PAT program_number           2
    // PAT PID                      0x0065 (101)
    // PAT program_number           3
    // PAT PID                      0x0066 (102)
    // PAT program_number           4
    // PAT PID                      0x0067 (103)
    // PAT CRC_32                   0xD46D3CBC
    //
    // PMT table_id                 0x02
    // PMT section_syntax_indicator 1
    // PMT section_length           60
    // PMT transport_stream_id      0x0001 (1)
    // PMT version_number           0
    // PMT current_next_indicator   1
    // PMT section_number           0
    // PMT last_section_number      0
    // PMT PCR_PID                  0x07D0 (2000)
    // PMT program_info_length      37
    // PMT descriptor:              129 = [User Private] (3) 0x08 0xBC 0x1B
    // PMT descriptor:              5 = [registration] (4) 0x47 0x41 0x39 0x34
    // PMT descriptor:              135 = [User Private] (24) 0xC1 0x01 0x01
    //                              0x00 0xF4 0x12 0x01 0x65 0x6E 0x67 0x01
    //                              0x00 0x3F 0x0A 0x00 0x54 0x00 0x56 0x00
    //                              0x2D 0x00 0x31 0x00 0x34
    // PMT stream_type              0x02 = [ITU-T Rec. H.262 | ISO/IEC 13818-2
    //                              Video or ISO/IEC 11172-2 constrained
    //                              parameter video stream]
    // PMT elementary_PID           0x07D0 (2000)
    // PMT ES_info_length           0
    // PMT stream_type              0x81 = [User Private]
    // PMT elementary_PID           0x07D1 (2001)
    // PMT ES_info_length           0
    // PMT CRC_32                   0xAA8B2805 0xAA8B2805

    RemapPair remapPairs[4];

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 100; // Old PMT
    remapPairs[1].newPid = 1000; // New PMT
    remapPairs[2].oldPid = 2000; // Old Video/PCR
    remapPairs[2].newPid = 200; // New Video/PCR
    remapPairs[3].oldPid = 2001; // Old Audio
    remapPairs[3].newPid = 201; // New Audio

    UnitTest7XX(pPlainMpg, 1, 100, 1000, 4, remapPairs);
}

static void UnitTest702(void)                                                   // 702.0000 - remap and filter program 2 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      101    1001     PMT
    //     3000     300     Video/PCR
    //     3001     301     Audio
    //     3002     302     Audio
    //
    // and the remapper set up to remap PATs and PMTs.
    //
    // plain.mpg:
    //
    // PAT table_id                 0x00
    // PAT section_syntax_indicator 1
    // PAT section_length           25
    // PAT transport_stream_id      0x054D (1357)
    // PAT version_number           0
    // PAT current_next_indicator   1
    // PAT section_number           0
    // PAT last_section_number      0
    // PAT program_number           1
    // PAT PID                      0x0064 (100)
    // PAT program_number           2
    // PAT PID                      0x0065 (101)
    // PAT program_number           3
    // PAT PID                      0x0066 (102)
    // PAT program_number           4
    // PAT PID                      0x0067 (103)
    // PAT CRC_32                   0xD46D3CBC
    //
    // PMT table_id                 0x02
    // PMT section_syntax_indicator 1
    // PMT section_length           100
    // PMT transport_stream_id      0x0002 (2)
    // PMT version_number           0
    // PMT current_next_indicator   1
    // PMT section_number           0
    // PMT last_section_number      0
    // PMT PCR_PID                  0x0BB8 (3000)
    // PMT program_info_length      50
    // PMT descriptor:              135 = [User Private] (30) 0xC1 0x01 0x02
    //                              0x00 0xF3 0x02 0xF1 0x16 0x01 0x65 0x6E
    //                              0x67 0x01 0x00 0x3F 0x0E 0x00 0x54 0x00
    //                              0x56 0x00 0x2D 0x00 0x50 0x00 0x47 0x00
    //                              0x2D 0x00 0x4C
    // PMT descriptor:              63 = [ITU-T Rec. H.222.0 | ISO/IEC 13818-1
    //                              Reserved] (16) 0x4A 0x75 0x6E 0x6B 0x20
    //                              0x64 0x65 0x73 0x63 0x72 0x69 0x70 0x74
    //                              0x6F 0x72 0x31
    // PMT stream_type              0x02 = [ITU-T Rec. H.262 | ISO/IEC 13818-2
    //                              Video or ISO/IEC 11172-2 constrained
    //                              parameter video stream]
    // PMT elementary_PID           0x0BB8 (3000)
    // PMT ES_info_length           0
    // PMT stream_type              0x81 = [User Private]
    // PMT elementary_PID           0x0BB9 (3001)
    // PMT ES_info_length           11
    // PMT descriptor:              10 = [ISO_639_language] (4) 0x65 0x6E 0x67 0x00
    // PMT descriptor:              129 = [User Private] (3) 0x08 0xBC 0x1B
    // PMT stream_type              0x81 = [User Private]
    // PMT elementary_PID           0x0BBA (3002)
    // PMT ES_info_length           11
    // PMT descriptor:              10 = [ISO_639_language] (4) 0x73 0x70 0x61 0x00
    // PMT descriptor:              129 = [User Private] (3) 0x08 0xBC 0x1B
    // PMT CRC_32                   0xFFB102AB 0xFFB102AB

    RemapPair remapPairs[5];

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 101; // Old PMT
    remapPairs[1].newPid = 1001; // New PMT
    remapPairs[2].oldPid = 3000; // Old Video/PCR
    remapPairs[2].newPid = 300; // New Video/PCR
    remapPairs[3].oldPid = 3001; // Old Audio
    remapPairs[3].newPid = 301; // New Audio
    remapPairs[4].oldPid = 3002; // Old Audio
    remapPairs[4].newPid = 302; // New Audio

    UnitTest7XX(pPlainMpg, 2, 101, 1001, 5, remapPairs);
}

static void UnitTest703(void)                                                   // 703.0000 - remap and filter program 3 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      102    1002     PMT
    //     5000     500     Video/PCR
    //     3999     399     Audio
    //
    // and the remapper set up to remap PATs and PMTs.
    //
    // plain.mpg:
    //
    // PAT table_id                 0x00
    // PAT section_syntax_indicator 1
    // PAT section_length           25
    // PAT transport_stream_id      0x054D (1357)
    // PAT version_number           0
    // PAT current_next_indicator   1
    // PAT section_number           0
    // PAT last_section_number      0
    // PAT program_number           1
    // PAT PID                      0x0064 (100)
    // PAT program_number           2
    // PAT PID                      0x0065 (101)
    // PAT program_number           3
    // PAT PID                      0x0066 (102)
    // PAT program_number           4
    // PAT PID                      0x0067 (103)
    // PAT CRC_32                   0xD46D3CBC
    //
    // PMT table_id                 0x02
    // PMT section_syntax_indicator 1
    // PMT section_length           18
    // PMT transport_stream_id      0x0003 (3)
    // PMT version_number           0
    // PMT current_next_indicator   1
    // PMT section_number           0
    // PMT last_section_number      0
    // PMT PCR_PID                  0x1388 (5000)
    // PMT program_info_length      0
    // PMT stream_type              0x90 = [User Private]
    // PMT elementary_PID           0x0F9F (3999)
    // PMT ES_info_length           0
    // PMT CRC_32                   0xC140DBE6 0xC140DBE6

    RemapPair remapPairs[4];

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 102; // Old PMT
    remapPairs[1].newPid = 1002; // New PMT
    remapPairs[2].oldPid = 5000; // Old Video/PCR
    remapPairs[2].newPid = 500; // New Video/PCR
    remapPairs[3].oldPid = 3999; // Old Audio
    remapPairs[3].newPid = 399; // New Audio

    UnitTest7XX(pPlainMpg, 3, 102, 1002, 4, remapPairs);
}

static void UnitTest704(void)                                                   // 704.0000 - remap and filter program 4 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      103    1003     PMT
    //     5000     500     Video/PCR
    //     5001     501     Audio
    //
    // and the remapper set up to remap PATs and PMTs.
    //
    // plain.mpg:
    //
    // PAT table_id                 0x00
    // PAT section_syntax_indicator 1
    // PAT section_length           25
    // PAT transport_stream_id      0x054D (1357)
    // PAT version_number           0
    // PAT current_next_indicator   1
    // PAT section_number           0
    // PAT last_section_number      0
    // PAT program_number           1
    // PAT PID                      0x0064 (100)
    // PAT program_number           2
    // PAT PID                      0x0065 (101)
    // PAT program_number           3
    // PAT PID                      0x0066 (102)
    // PAT program_number           4
    // PAT PID                      0x0067 (103)
    // PAT CRC_32                   0xD46D3CBC
    //
    // PMT table_id                 0x02
    // PMT section_syntax_indicator 1
    // PMT section_length           23
    // PMT transport_stream_id      0x0004 (4)
    // PMT version_number           0
    // PMT current_next_indicator   1
    // PMT section_number           0
    // PMT last_section_number      0
    // PMT PCR_PID                  0x1388 (5000)
    // PMT program_info_length      0
    // PMT stream_type              0x80 = [User Private]
    // PMT elementary_PID           0x1388 (5000)
    // PMT ES_info_length           0
    // PMT stream_type              0x81 = [User Private]
    // PMT elementary_PID           0x1389 (5001)
    // PMT ES_info_length           0
    // PMT CRC_32                   0x78751603 0x78751603

    RemapPair remapPairs[4];

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 103; // Old PMT
    remapPairs[1].newPid = 1003; // New PMT
    remapPairs[2].oldPid = 5000; // Old Video/PCR
    remapPairs[2].newPid = 500; // New Video/PCR
    remapPairs[3].oldPid = 5001; // Old Audio
    remapPairs[3].newPid = 501; // New Audio

    UnitTest7XX(pPlainMpg, 4, 103, 1003, 4, remapPairs);
}

static void UnitTest8XX(const char * fileName, char * saveName,                 // 8XX.0000 - subroutine used by tests 800 - 804
        RemapPid videoPid, RemapPid audioPid)
{
    // Index the input file into a single IFS output file, 10 packets at a time
    // assuming that each group of 10 packtes represents 1 second of time.
    // The output of each of these tests (800, 801, 802, 803 and 804) is used
    // as the input into a corresponding series of 901-904 tests, like this:
    //
    //       UnitTest800(out saveName);
    //       UnitTest901(in  saveName);
    //       UnitTest902(in  saveName);
    //       UnitTest903(in  saveName);
    //       UnitTest904(in  saveName);
    //
    //       UnitTest801(out saveName);
    //       UnitTest901(in  saveName);
    //       UnitTest902(in  saveName);
    //       UnitTest903(in  saveName);
    //       UnitTest904(in  saveName);
    //
    //       UnitTest802(out saveName);
    //       UnitTest901(in  saveName);
    //       UnitTest902(in  saveName);
    //       UnitTest903(in  saveName);
    //       UnitTest904(in  saveName);
    //
    //       UnitTest803(out saveName);
    //       UnitTest901(in  saveName);
    //       UnitTest902(in  saveName);
    //       UnitTest903(in  saveName);
    //       UnitTest904(in  saveName);
    //
    //       UnitTest804(out saveName);
    //       UnitTest901(in  saveName);
    //       UnitTest902(in  saveName);
    //       UnitTest903(in  saveName);
    //       UnitTest904(in  saveName);

    NumBytes saveSize;

    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // writer, then reader
    NumPackets numPackets;

    FILE * const pInFile = fopen(fileName, "rb+");

    size_t i = 0;
    IfsPacket packets[2][16];

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    test(IfsOpenWriter(pOutPath,                                                // 8XX.0010 - open the output IFS file as a single file
                    NULL,
                    0,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsStart(ifsHandle, videoPid, audioPid),                               // 8XX.0020 - start the indexer
        IfsReturnCodeNoErrorReported);

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 8XX.0030 - verify the starting state of the indexer
    iseq(pIfsInfo->mpegSize, 0 );
    iseq(pIfsInfo->ndexSize, 0 );
    iseq(pIfsInfo->begClock, 0 );
    iseq(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, videoPid);
    iseq(pIfsInfo->audioPid, audioPid);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    strcpy(saveName, pIfsInfo->name);                                           // 8XX.0040 - save the generated IFS file name for the 9XX tests

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    i = 0;

    while ((numPackets = fread(packets[1], IFS_TRANSPORT_PACKET_SIZE, 10,
            pInFile)) != 0)
    {
        // Pretend each set of packets contains 1 second of data...

        test(IfsWrite(ifsHandle, ++i*NSEC_PER_SEC, numPackets,                  // 8XX.0050 - index the file 10 packets at a time
            (IfsPacket *)packets[1]), IfsReturnCodeNoErrorReported);
    }

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 8XX.0060 - verify the ending state of the indexer
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, videoPid);
    iseq(pIfsInfo->audioPid, audioPid);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    saveSize = pIfsInfo->mpegSize + pIfsInfo->ndexSize;

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 8XX.0070 - close the output IFS file

    test(IfsPathNameInfo(pOutPath, saveName, &pIfsInfo),                        // 8XX.0080 - verify the state of the closed output IFS file
        IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(saveSize, pIfsInfo->mpegSize+pIfsInfo->ndexSize);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    isne(ifsHandle, NULL);
    test(IfsOpenWriter(pOutPath, saveName, 90*60, &ifsHandle),                  // 8XX.0090 - try to reopen the output IFS file as a TSB
            IfsReturnCodeBadMaxSizeValue);
    iseq(ifsHandle, NULL);

    test(IfsOpenReader(pOutPath,                                                // 8XX.0100 - properly reopen the output IFS file
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    iseq(saveSize, ifsHandle->mpegSize+ifsHandle->ndexSize);

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 8XX.0110 - verify the state of the reopened file
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(saveSize, pIfsInfo->mpegSize+pIfsInfo->ndexSize);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 8XX.0120 - close the output IFS file

    fclose(pInFile);
}

static void UnitTest800(char * saveName)                                        // 800.0000 - index program 1 of background.mpg
{
    UnitTest8XX(pBackgMpg, saveName, 68, 0);
}
static void UnitTest801(char * saveName)                                        // 801.0000 - index program 1 of plain.mpg
{
    UnitTest8XX(pPlainMpg, saveName, 2000, 2001);
}
static void UnitTest802(char * saveName)                                        // 802.0000 - index program 2 of plain.mpg
{
    UnitTest8XX(pPlainMpg, saveName, 3000, 3001);
}
static void UnitTest803(char * saveName)                                        // 803.0000 - index program 3 of plain.mpg
{
    UnitTest8XX(pPlainMpg, saveName, 5000, 3999);
}
static void UnitTest804(char * saveName)                                        // 804.0000 - index program 4 of plain.mpg
{
    UnitTest8XX(pPlainMpg, saveName, 2000, 5001);
}

static void UnitTest901(char * saveName)                                        // 901.0000 - test several IfsRead* cases on each IFS file
{
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    IfsPacket * pData;
    NumPackets numPackets;
    IfsClock ifsClock;

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    ifsClock = pIfsInfo->begClock;                                              // 901.0010 - begClock, IfsDirectEither, NearestPicture case
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadNearestPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeNoErrorReported);
    isne(numPackets, 0);
    isne(pData, NULL);
    g_free(pData);

    ifsClock = pIfsInfo->begClock;                                              // 901.0020 - begClock, IfsDirectEither, NextPicture case
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadNextPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeNoErrorReported);
    isne(numPackets, 0);
    isne(pData, NULL);
    g_free(pData);

    ifsClock = pIfsInfo->begClock;                                              // 901.0030 - begClock, IfsDirectEither, PreviousPicture case (should fail)
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadPreviousPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeIframeNotFound);
    iseq(numPackets, 0);
    iseq(pData, NULL);

    ifsClock = (pIfsInfo->begClock + pIfsInfo->endClock) / 2;                   // 901.0040 - midClock, IfsDirectEither, NearestPicture case
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadNearestPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeNoErrorReported);
    isne(numPackets, 0);
    isne(pData, NULL);
    g_free(pData);

    ifsClock = (pIfsInfo->begClock + pIfsInfo->endClock) / 2;                   // 901.0050 - midClock, IfsDirectEither, PreviousPicture case
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadPreviousPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeNoErrorReported);
    isne(numPackets, 0);
    isne(pData, NULL);
    g_free(pData);

    ifsClock = (pIfsInfo->begClock + pIfsInfo->endClock) / 2;                   // 901.0060 - midClock, IfsDirectEither, NextPicture case
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadNextPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeNoErrorReported);
    isne(numPackets, 0);
    isne(pData, NULL);
    g_free(pData);

    ifsClock = pIfsInfo->endClock;                                              // 901.0070 - endClock, IfsDirectEither, NearestPicture case
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadNearestPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeNoErrorReported);
    isne(numPackets, 0);
    isne(pData, NULL);
    g_free(pData);

    ifsClock = pIfsInfo->endClock;                                              // 901.0080 - endClock, IfsDirectEither, PreviousPicture case
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadPreviousPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeNoErrorReported);
    isne(numPackets, 0);
    isne(pData, NULL);
    g_free(pData);

    ifsClock = pIfsInfo->endClock;                                              // 901.0090 - endClock, IfsDirectEither, NextPicture case (should fail)
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);
    test(IfsReadNextPicture(ifsHandle, 0, 0, &numPackets, &pData),
        IfsReturnCodeIframeNotFound);
    iseq(numPackets, 0);
    iseq(pData, NULL);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);
}

static void UnitTest902(char * saveName)                                        // 902.0000 - copy compare test using IfsSeekToTime and IfsRead(10,000)
{
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    IfsPacket * pData;
    IfsClock ifsClock;

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    ifsClock = pIfsInfo->begClock;
    test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
        IfsReturnCodeNoErrorReported);

    { // Copy compare test using the IfsRead command.

        // Calculate the number of bytes that need to be copied
        // "by hand" so the compare will work
        NumBytes numBytes = (NumBytes) ifsHandle->realLoc
                * (NumBytes) sizeof(IfsPacket);
        FILE * pCopy;
        char temp[1024]; // path and filename

        pData = g_try_malloc(numBytes);
        isne(pData, NULL);
        if (pData == NULL)
            return;

        strcpy(temp, ifsHandle->both);
        strcat(temp, "/");
        strcat(temp, "CopyOf.mpg");

        pCopy = fopen(temp, "wb+");
        isne(pCopy, NULL);
        if (pCopy == NULL)
        {
            g_free(pData);
            return;
        }

        rewind(ifsHandle->pMpeg);

        // Copy the "junk" packets at the beginning of the stream by hand
        iseq(fread(pData, 1, numBytes, ifsHandle->pMpeg), numBytes);
        iseq(fwrite(pData, 1, numBytes, pCopy), numBytes);
        g_free(pData);

        ifsClock = pIfsInfo->begClock;
        test(IfsSeekToTime(ifsHandle, IfsDirectEither, &ifsClock, NULL),
            IfsReturnCodeNoErrorReported);

        while (1)
        {
            IfsClock curClock;
            NumPackets numPackets = 10000;
            IfsReturnCode ifsReturnCode = IfsRead(ifsHandle, &numPackets,
                    &curClock, &pData);

            if (ifsReturnCode == IfsReturnCodeNoErrorReported)
            {
                if (numPackets)
                {
                    isne(pData, NULL);
                    if (pData == NULL)
                        return;
                    numBytes = (NumBytes) numPackets
                            * (NumBytes) sizeof(IfsPacket);
                    iseq(fwrite(pData, 1, numBytes, pCopy), numBytes);
                    g_free(pData);
                }
                else
                {
                    iseq(pData, NULL);
                }
            }
            else
            {
                iseq(numPackets, 0);
                iseq(pData, NULL);
                break;
            }
        }

        rewind(ifsHandle->pMpeg);
        rewind(pCopy);

        while (1) // compare the copy to the original
        {
            char origData[sizeof(IfsPacket)];
            char copyData[sizeof(IfsPacket)];
            NumBytes origBytes = fread(origData, 1, sizeof(IfsPacket),
                    ifsHandle->pMpeg);
            NumBytes copyBytes = fread(copyData, 1, sizeof(IfsPacket), pCopy);
            iseq(origBytes, copyBytes);
            if (origBytes)
            {
                iseq(origBytes, sizeof(IfsPacket));
                iseq(memcmp(origData, copyData, origBytes), 0);
            }
            else
            {
                break;
            }
        }

        fclose(pCopy);
        iseq(remove(temp), 0);
    }

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);
}

static void UnitTest903(char * saveName)                                        // 903.0000 - copy compare test using IfsSeekToPacket and IfsRead(1000)
{
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    IfsPacket * pData;

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    { // Copy compare test using the IfsRead command.

        NumBytes numBytes = (NumBytes) ifsHandle->realLoc
                * (NumBytes) sizeof(IfsPacket);
        FILE * pCopy;
        char temp[1024]; // path and filename

        strcpy(temp, ifsHandle->both);
        strcat(temp, "/");
        strcat(temp, "CopyOf.mpg");

        pCopy = fopen(temp, "wb+");
        isne(pCopy, NULL);
        if (pCopy == NULL)
            return;

        test(IfsSeekToPacket(ifsHandle, 0, NULL),
            IfsReturnCodeNoErrorReported);

        while (1)
        {
            IfsClock curClock;
            NumPackets numPackets = 1000;
            IfsReturnCode ifsReturnCode = IfsRead(ifsHandle, &numPackets,
                    &curClock, &pData);

            if (ifsReturnCode == IfsReturnCodeNoErrorReported)
            {
                if (numPackets)
                {
                    isne(pData, NULL);
                    if (pData == NULL)
                        return;
                    numBytes = (NumBytes) numPackets
                            * (NumBytes) sizeof(IfsPacket);
                    iseq(fwrite(pData, 1, numBytes, pCopy), numBytes);
                    g_free(pData);
                }
                else
                {
                    iseq(pData, NULL);
                }
            }
            else
            {
                iseq(numPackets, 0);
                iseq(pData, NULL);
                break;
            }
        }

        rewind(ifsHandle->pMpeg);
        rewind(pCopy);

        while (1) // compare the copy to the original
        {
            char origData[sizeof(IfsPacket)];
            char copyData[sizeof(IfsPacket)];
            NumBytes origBytes = fread(origData, 1, sizeof(IfsPacket),
                    ifsHandle->pMpeg);
            NumBytes copyBytes = fread(copyData, 1, sizeof(IfsPacket), pCopy);
            iseq(origBytes, copyBytes);
            if (origBytes)
            {
                iseq(origBytes, sizeof(IfsPacket));
                iseq(memcmp(origData, copyData, origBytes), 0);
            }
            else
            {
                break;
            }
        }

        fclose(pCopy);
        iseq(remove(temp), 0);
    }

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);
}

static void UnitTest904(char * saveName)                                        // 904.0000 - random compare of IfsSeekToTime to IfsSeekToPacket in a file
{
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader

    size_t i = 0;

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    for (i = 0; i < 1000; i++)
    {
        NumPackets position;

        IfsClock location = ((pIfsInfo->endClock - pIfsInfo->begClock) * rand()
                / RAND_MAX) + pIfsInfo->begClock;
        IfsClock ifsClock;

        test(IfsSeekToTime(ifsHandle, IfsDirectEither, &location, &position),
            IfsReturnCodeNoErrorReported);
        test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
            IfsReturnCodeNoErrorReported);
        iseq(ifsClock, location);
    }

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    test(IfsDelete(pOutPath, saveName), IfsReturnCodeNoErrorReported);
}

static void UnitTest101X(char * saveName, const char * fileName,                // 101X.0000 - create a TSB, verify resizing functionality
        RemapPid videoPid, RemapPid audioPid)
{
    unsigned char packet[10 * IFS_TRANSPORT_PACKET_SIZE];
    NumBytes saveSize;
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // writer
    FILE * pInFile = fopen(fileName, "rb+");
    size_t i, total;

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenWriter(pOutPath,                                                // 101X.0010 - open a 90 minute TSB
                    NULL,
                    90*60, // 90 minute TSB
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsStart(ifsHandle, videoPid, audioPid),                               // 101X.0020 - start the TSB
        IfsReturnCodeNoErrorReported);

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 101X.0030 - verify the TSB start state
    iseq(pIfsInfo->mpegSize, 0 );
    iseq(pIfsInfo->ndexSize, 0 );
    iseq(pIfsInfo->begClock, 0 );
    iseq(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, videoPid);
    iseq(pIfsInfo->audioPid, audioPid);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    strcpy(saveName, pIfsInfo->name);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    i = 0;

    while ((total = fread(packet, IFS_TRANSPORT_PACKET_SIZE, 10, pInFile)) != 0)
    {
        // Pretend each set of packets contains 1 second of data...

        if ((i + 1) % 16000 == 0)
        {
            printf("maxsize set to 60 minutes\n");
            test(IfsSetMaxSize(ifsHandle, 60*60),                               // 101X.0040 - switch to a 60 minute TSB
                IfsReturnCodeNoErrorReported);
        }
        else if ((i + 1) % 12000 == 0)
        {
            // (do nothing)
            test(IfsSetMaxSize(NULL, 60*60),                                    // 101X.0050 - NULL ifsHandle to IfsSetMaxSize
                IfsReturnCodeBadInputParameter);
            test(IfsSetMaxSize(ifsHandle, 0),                                   // 101X.0060 - attempt to switch the IFS output file to a single file
                IfsReturnCodeBadInputParameter);
        }
        else if ((i + 1) % 8000 == 0)
        {
            printf("maxsize set to 90 minutes\n");
            test(IfsSetMaxSize(ifsHandle, 90*60),                               // 101X.0070 - switch back to a 90 minute TSB
                IfsReturnCodeNoErrorReported);
        }
        else if ((i + 1) % 4000 == 0)
        {
            printf("maxsize set to 30 minutes\n");
            test(IfsSetMaxSize(ifsHandle, 30*60),                               // 101X.0080 - switch to a 30 minute TSB
                IfsReturnCodeNoErrorReported);
        }

        test(IfsWrite(ifsHandle, ++i*NSEC_PER_SEC, total,
            (IfsPacket *)packet), IfsReturnCodeNoErrorReported);
    }

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 101X.0090 - verify the TSB ending state
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, videoPid);
    iseq(pIfsInfo->audioPid, audioPid);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    saveSize = pIfsInfo->mpegSize + pIfsInfo->ndexSize;

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 101X.0100 - close the TSB

    test(IfsPathNameInfo(pOutPath, saveName, &pIfsInfo),                        // 101X.0110 - verify the TSB closed state
        IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(saveSize, pIfsInfo->mpegSize+pIfsInfo->ndexSize);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    isne(ifsHandle, NULL);
    test(IfsOpenWriter(pOutPath, saveName, 0, &ifsHandle),                      // 101X.0120 - attempt to reopen the TSB as a single file
            IfsReturnCodeBadMaxSizeValue);
    iseq(ifsHandle, NULL);

    test(IfsOpenWriter(pOutPath,                                                // 101X.0130 - properly reopen the TSB
                    saveName,
                    90*60, // 90 minute TSB
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    iseq(saveSize, ifsHandle->mpegSize+ifsHandle->ndexSize);

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 101X.0140 - verify the TSB reopen state
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(saveSize, pIfsInfo->mpegSize+pIfsInfo->ndexSize);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 101X.0150 - close the TSB

    fclose(pInFile);
}

#ifdef DO_RANDOM_SEEKS
static void UnitTest102X(char * saveName)                                       // 102X.0000 - random compare of IfsSeekToTime to IfsSeekToPacket in a TSB
{
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    size_t i;

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    printf("Start random TSB seek test\n");

    for (i = 0; i < 1000; i++)
    {
        NumPackets position;

        IfsClock location = ((pIfsInfo->endClock - pIfsInfo->begClock) * rand()
                / RAND_MAX) + pIfsInfo->begClock;
        IfsClock ifsClock;

        test(IfsSeekToTime(ifsHandle, IfsDirectEither, &location, &position),
            IfsReturnCodeNoErrorReported);
        test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
            IfsReturnCodeNoErrorReported);
        iseq(ifsClock, location);
    }

    printf("EndOf random TSB seek test\n");

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);
}
#endif

static void UnitTest103Y(IfsHandle ifsHandle, IfsHandle outHandle,              // 103Y.0000 - subroutime to test the IfsConvert/IfsAppend functionality
        FILE * pInFile, IfsClock begClock, IfsClock endClock)
{
    unsigned char packet[IFS_TRANSPORT_PACKET_SIZE];
    IfsClock tmpBegClock, tmpEndClock;
    NumPackets position;
    IfsInfo * pIfsInfo;
    size_t i;
    char temp[32];

    tmpBegClock = begClock;
    tmpEndClock = endClock;

    test(IfsConvert(ifsHandle, outHandle, &tmpBegClock, &tmpEndClock),          // 103Y.0010 - start the conversion (call IfsConvert)
        IfsReturnCodeNoErrorReported);
    test(IfsSeekToTime(ifsHandle, IfsDirectBegin, &tmpBegClock, &position),
        IfsReturnCodeNoErrorReported);

    while (tmpEndClock < endClock)                                              // 103Y.0020 - finish the conversion (call IfsAppend until done)
    {
        //printf("tmpEndClock %s LT ", IfsLongLongToString(tmpEndClock));
        //printf("endClock %s\n", IfsLongLongToString(endClock   ));
        tmpEndClock = endClock;
        test(IfsAppend(ifsHandle, outHandle, &tmpEndClock),
            IfsReturnCodeNoErrorReported);
    }
    //printf("tmpEndClock %s GE ", IfsLongLongToString(tmpEndClock));
    //printf("endClock %s\n", IfsLongLongToString(endClock   ));

    // Check the results

    test(IfsHandleInfo(outHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 103Y.0030 - verify the ending state
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isge(pIfsInfo->begClock, tmpBegClock );
    isle(pIfsInfo->endClock, tmpEndClock );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(fseek(pInFile, position*IFS_TRANSPORT_PACKET_SIZE, SEEK_SET), 0);
    test(IfsSeekToPacket(outHandle, 0, NULL), IfsReturnCodeNoErrorReported);

    printf("Comparing %s bytes, %ld packets, position %ld\n",                   // 103Y.0040 - Compare the converted file to the original TSB
            IfsLongLongToString(pIfsInfo->mpegSize, temp),
            (long) (pIfsInfo->mpegSize / IFS_TRANSPORT_PACKET_SIZE), position);

    for (i = 0; i < pIfsInfo->mpegSize / IFS_TRANSPORT_PACKET_SIZE; i++)
    {
        IfsPacket * pBuffer = NULL;
        NumPackets numPackets = 1;

        test(IfsRead(outHandle, &numPackets, NULL, &pBuffer),
            IfsReturnCodeNoErrorReported);
        if (numPackets)
        {
            iseq(fread(packet, 1, IFS_TRANSPORT_PACKET_SIZE, pInFile),
                IFS_TRANSPORT_PACKET_SIZE);
            isne(pBuffer, NULL);
            if (pBuffer == NULL)
                return;
            iseq(memcmp(packet, pBuffer, IFS_TRANSPORT_PACKET_SIZE), 0);
            g_free(pBuffer);
        }
        else
        {
            i--; // back up and try again
            iseq(pBuffer, NULL);
        }
    }

#ifdef DO_RANDOM_SEEKS

    printf("Start random saved recording seek test\n");                         // 103Y.0050 - random compare of IfsSeekToTime to IfsSeekToPacket on the converted file

    for (i = 0; i < 1000; i++)
    {
        IfsClock location = ((pIfsInfo->endClock - pIfsInfo->begClock) * rand()
                / RAND_MAX) + pIfsInfo->begClock;
        IfsClock ifsClock;

        test(IfsSeekToTime(outHandle, IfsDirectEither, &location, &position),
            IfsReturnCodeNoErrorReported);
        test(IfsSeekToPacket(outHandle, position, &ifsClock),
            IfsReturnCodeNoErrorReported);
        iseq(ifsClock, location);
    }

    printf("EndOf random saved recording seek test\n");

#endif

    test(IfsStop(outHandle), IfsReturnCodeNoErrorReported);
}

static void UnitTest103X(char * saveName, const char * fileName)                // 103X.0000 - subroutine to call test 103Y for various conversion possibilites
{
    IfsClock hw, p1, p2, p3, p4;
    IfsHandle ifsHandle; // reader
    IfsHandle outHandle; // writer
    IfsInfo * pIfsInfo;

    FILE * const pInFile = fopen(fileName, "rb+");
    char saveOutName[256];

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenWriter(pOutPath,
                    NULL,
                    0,
                    &outHandle), IfsReturnCodeNoErrorReported);
    isne(outHandle, NULL);
    if (outHandle == NULL)
        return;

    test(IfsHandleInfo(outHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    iseq(pIfsInfo->mpegSize, 0 );
    iseq(pIfsInfo->ndexSize, 0 );
    iseq(pIfsInfo->begClock, 0 );
    iseq(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    strcpy(saveOutName, pIfsInfo->name);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    hw = (pIfsInfo->endClock - pIfsInfo->begClock) / 2;
    p1 = pIfsInfo->begClock > hw ? pIfsInfo->begClock - hw : 0;
    p2 = pIfsInfo->begClock;
    p3 = pIfsInfo->begClock + hw;
    p4 = pIfsInfo->endClock;

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    UnitTest103Y(ifsHandle, outHandle, pInFile, p1, p4);                        // 103X.0010 - call test 103Y from begin-50% to end
    UnitTest103Y(ifsHandle, outHandle, pInFile, p1, p3);                        // 103X.0020 - call test 103Y from begin-50% to begin+50%
    UnitTest103Y(ifsHandle, outHandle, pInFile, p2, p4);                        // 103X.0030 - call test 103Y from begin to end
    UnitTest103Y(ifsHandle, outHandle, pInFile, p2, p3);                        // 103X.0040 - call test 103Y from begin to begin+50%
    UnitTest103Y(ifsHandle, outHandle, pInFile, p3, p4);                        // 103X.0050 - call test 103Y from begin+50% to the end

    test(IfsClose(outHandle), IfsReturnCodeNoErrorReported);
    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    test(IfsDelete(pOutPath, saveOutName), IfsReturnCodeNoErrorReported);

    fclose(pInFile);
}

#define MAX_RAND_BUFF_SIZE 64
static void UnitTest104X(char * saveName, const char * fileName)                // 104X.0000 - full file compare with random transfer lengths
{
    unsigned char packet[MAX_RAND_BUFF_SIZE * IFS_TRANSPORT_PACKET_SIZE];
    FILE * pInFile = fopen(fileName, "rb+");
    IfsHandle ifsHandle; // reader
    IfsInfo * pIfsInfo;
    NumPackets position;
    NumBytes numBytes = 0;
    char temp[32];

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    test(IfsSeekToTime(ifsHandle, IfsDirectBegin, &pIfsInfo->begClock,
        &position), IfsReturnCodeNoErrorReported);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    iseq(fseek(pInFile, position*IFS_TRANSPORT_PACKET_SIZE, SEEK_SET), 0);

    while (IfsTrue)
    {
        IfsPacket * pBuffer = NULL;
        NumPackets numPackets = (rand() & (MAX_RAND_BUFF_SIZE - 1)) + 1; // 1..32
        IfsReturnCode ifsReturnCode = IfsRead(ifsHandle, &numPackets, NULL,
                &pBuffer);

        if (ifsReturnCode == IfsReturnCodeNoErrorReported)
        {
            if (numPackets)
            {
                isne(pBuffer, NULL);
                if (pBuffer == NULL)
                    return;
                iseq(fread(packet, IFS_TRANSPORT_PACKET_SIZE, numPackets,
                    pInFile), numPackets);
                iseq(memcmp(packet, pBuffer,
                    IFS_TRANSPORT_PACKET_SIZE*numPackets), 0);
                g_free(pBuffer);
                numBytes += (NumBytes) IFS_TRANSPORT_PACKET_SIZE
                        * (NumBytes) numPackets;
            }
            else
            {
                iseq(pBuffer, NULL);
            }
        }
        else if (ifsReturnCode == IfsReturnCodeReadPastEndOfFile)
        {
            iseq(pBuffer, NULL);
            break;
        }
        else
        {
            iseq(pBuffer, NULL);
            iseq(ifsReturnCode, IfsReturnCodeNoErrorReported);
            break;
        }
    }

    printf("Compared  %s bytes, %ld packets, position %ld\n",
            IfsLongLongToString(numBytes, temp), (long) (numBytes
                    / IFS_TRANSPORT_PACKET_SIZE), position);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    fclose(pInFile);
}

#define NUMBER_OF_RANDOM_I_FRAMES 5
static void UnitTest105X(char * saveName, const char * fileName)                // 105X.0000 - random I frame seek, read, verify test
{
    FILE * pInFile = fopen(fileName, "rb+");
    IfsHandle ifsHandle; // reader
    IfsInfo * pIfsInfo;
    IfsPacket * pData;
    NumPackets numPackets;
    NumPackets startPacket;
    NumBytes numBytes = 0;
    int i, iFrames = 0;
    char temp[32];

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    printf("Start random I frame test\n");

    for (i = 0; i < NUMBER_OF_RANDOM_I_FRAMES; i++)
    {
        IfsReturnCode ifsReturnCode;
        IfsClock location = ((pIfsInfo->endClock - pIfsInfo->begClock) * rand()
                / RAND_MAX) + pIfsInfo->begClock;

        test(IfsSeekToTime(ifsHandle, IfsDirectEither, &location, NULL),
            IfsReturnCodeNoErrorReported);

        ifsReturnCode = IfsReadPicture(ifsHandle, 0, 0, IfsReadTypeNearest,
                &numPackets, &pData, &startPacket);

        if (ifsReturnCode == IfsReturnCodeNoErrorReported)
        {
            IfsPacket * pBuffer;
            isne(numPackets, 0);
            isne(pData, NULL);
            if (pData == NULL)
                return;

            pBuffer = g_try_malloc(IFS_TRANSPORT_PACKET_SIZE * numPackets);
            isne(pBuffer, NULL);
            if (pBuffer == NULL)
                return;

            iseq(fseek(pInFile, startPacket*IFS_TRANSPORT_PACKET_SIZE,
                SEEK_SET), 0);
            iseq(fread(pBuffer, IFS_TRANSPORT_PACKET_SIZE, numPackets,
                pInFile), numPackets);
            iseq(memcmp(pData, pBuffer,
                IFS_TRANSPORT_PACKET_SIZE*numPackets), 0);

            g_free(pBuffer);
            g_free(pData);
            iFrames++;
            numBytes += (NumBytes) IFS_TRANSPORT_PACKET_SIZE
                    * (NumBytes) numPackets;
        }
        else
        {
            printf("Error is %s\n", IfsReturnCodeToString(ifsReturnCode));
            iseq(numPackets, 0);
            iseq(pData, NULL);
        }
    }

    printf("EndOf random I frame test\n");

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    printf("Compared %s bytes, %ld packets, %d of %d I frames\n",
            IfsLongLongToString(numBytes, temp), (long) (numBytes
                    / IFS_TRANSPORT_PACKET_SIZE), iFrames,
            NUMBER_OF_RANDOM_I_FRAMES);

    fclose(pInFile);
}

static void UnitTest10XX(const char * fileName, RemapPid videoPid,              // 10XX.0000 - call all the 10XX tests on the given program in the given file
        RemapPid audioPid)
{
    char saveName[256];

    UnitTest101X(saveName, fileName, videoPid, audioPid);                       // 10XX.0010 - call test 101X on the given program in the given file
#ifdef DO_RANDOM_SEEKS
    UnitTest102X(saveName);                                                     // 10XX.0020 - call test 102X with the file produced by test 101X
#endif
    UnitTest103X(saveName, fileName);                                           // 10XX.0030 - call test 103X with the file produced by test 101X
    UnitTest104X(saveName, fileName);                                           // 10XX.0040 - call test 104X with the file produced by test 101X
    UnitTest105X(saveName, fileName);                                           // 10XX.0050 - call test 105X with the file produced by test 101X

    test(IfsDelete(pOutPath, saveName), IfsReturnCodeNoErrorReported);          // 10XX.0060 - delete the file produced by test 101X
}

static void UnitTest1000(void)                                                  // 1000.0000 - call all the 10XX tests on program 1 of background.mpg
{
    UnitTest10XX(pBackgMpg, 68, 0);
}
static void UnitTest1001(void)                                                  // 1001.0000 - call all the 10XX tests on program 1 of plain.mpg
{
    UnitTest10XX(pPlainMpg, 2000, 2001);
}
static void UnitTest1002(void)                                                  // 1002.0000 - call all the 10XX tests on program 2 of plain.mpg
{
    UnitTest10XX(pPlainMpg, 3000, 3001);
}
static void UnitTest1003(void)                                                  // 1003.0000 - call all the 10XX tests on program 3 of plain.mpg
{
    UnitTest10XX(pPlainMpg, 5000, 3999);
}
static void UnitTest1004(void)                                                  // 1004.0000 - call all the 10XX tests on program 4 of plain.mpg
{
    UnitTest10XX(pPlainMpg, 2000, 5001);
}

static NumPackets UnitTest11XX                                                  // 11XX.0000 - filter PIDs and remap PATs, PMTs and PIDs with various residuals
(const char * fileName, RemapProg programNumber, RemapPid oldPmtPid,
        RemapPid newPmtPid, NumPairs numPairs, RemapPair * pRemapPairs,
        size_t offset)
{
    FILE * const pInFile = fopen(fileName, "rb+");

    size_t i = 2;
    IfsBoolean firstOne = IfsTrue;
    RemapHandle remapHandle;
    NumPackets nextNumPackets;
    RemapPacket packets[2][16];
    RemapPacket residual[2];
    RemapPacket * pointers[2][16];
    NumPackets prevNumPackets;
    RemapPacket ** ppPrevPointers;
    NumPackets totalPassed = 0;
    NumPackets totalTested = 0;
    NumPackets readSize = offset ? (offset & 7) + 8 : 16;

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return IFS_UNDEFINED_PACKET;

    testRemap(RemapOpen(&remapHandle), RemapReturnCodeNoErrorReported);
    testRemap(RemapAndFilterPids(remapHandle, numPairs, pRemapPairs),
        RemapReturnCodeNoErrorReported);
    testRemap(RemapPmts(remapHandle, oldPmtPid), RemapReturnCodeNoErrorReported);
    testRemap(RemapPats(remapHandle, programNumber, newPmtPid),
        RemapReturnCodeNoErrorReported)

    isne(remapHandle, NULL);

    while ((nextNumPackets = fread(&packets[i & 1][0],
            IFS_TRANSPORT_PACKET_SIZE, readSize, pInFile)) != 0)
    {
        if (offset) // read the TOP of the NEXT residual buffer (if any)
            isle(fread(&residual[(i+1)&1].bytes[0], 1, offset,
                pInFile), offset);

        if (firstOne)
        {
            testRemap(RemapAndFilter(remapHandle,
                            NULL,
                            nextNumPackets,
                            &packets[i&1][0],
                            &pointers[i&1][0],
                            &prevNumPackets,
                            &ppPrevPointers),
                    RemapReturnCodeNoErrorReported);

            i++;

            iseq(prevNumPackets, 0);
            iseq(ppPrevPointers, NULL);
            firstOne = IfsFalse;

            totalTested += nextNumPackets;
        }
        else
        {
            testRemap(RemapAndFilter(remapHandle,
                            offset ? &residual[i&1] : NULL,
                            nextNumPackets,
                            &packets[i&1][0],
                            &pointers[i&1][0],
                            &prevNumPackets,
                            &ppPrevPointers),
                    RemapReturnCodeNoErrorReported);

            i++;

            iseq(ppPrevPointers, &pointers[i&1][0]);
            totalPassed += prevNumPackets;

            totalTested += nextNumPackets + (offset ? 1 : 0);
        }

        if (offset) // read the BOT of the residual buffer (if any)
            isle(fread(&residual[i&1].bytes[offset],
                            1,
                            IFS_TRANSPORT_PACKET_SIZE-offset,
                            pInFile), IFS_TRANSPORT_PACKET_SIZE-offset);
    }

    // Get the last packet
    testRemap(RemapAndFilter(remapHandle,
                    NULL,
                    0,
                    NULL,
                    NULL,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported)

    iseq(ppPrevPointers, &pointers[(i+1)&1][0]);
    totalPassed += prevNumPackets;
    if (!offset)
        printf("%6ld passed of %6ld\n", totalPassed, totalTested);

    testRemap(RemapClose(remapHandle,
                    &prevNumPackets,
                    &ppPrevPointers),
            RemapReturnCodeNoErrorReported);

    iseq(prevNumPackets, 0);
    iseq(ppPrevPointers, NULL);

    fclose(pInFile);

    return totalPassed;
}

static void UnitTest1100(void)                                                  // 1100.0000 - remap and filter program 1 of background.mpg
{
    // This test processes the entire background.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //       66     660     PMT
    //       68     680     Video/PCR
    //
    // and the remapper set up to remap PATs and PMTs for all the various
    // residual offset possibilites.

    size_t offset;
    RemapPair remapPairs[3];
    NumPackets totalPassed;

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 66; // Old PMT
    remapPairs[1].newPid = 660; // New PMT
    remapPairs[2].oldPid = 68; // Old Video/PCR
    remapPairs[2].newPid = 680; // New Video/PCR

    totalPassed = UnitTest11XX(pBackgMpg, 1, 66, 660, 3, remapPairs, 0);
    for (offset = 1; offset < IFS_TRANSPORT_PACKET_SIZE; offset++)
        iseq(totalPassed, UnitTest11XX(pBackgMpg, 1, 66, 660, 3,
            remapPairs, offset));
}

static void UnitTest1101(void)                                                  // 1101.0000 - remap and filter program 1 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      100    1000     PMT
    //     2000     200     Video/PCR
    //     2001     201     Audio
    //
    // and the remapper set up to remap PATs and PMTs for all the various
    // residual offset possibilites.

    size_t offset;
    RemapPair remapPairs[4];
    NumPackets totalPassed;

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 100; // Old PMT
    remapPairs[1].newPid = 1000; // New PMT
    remapPairs[2].oldPid = 2000; // Old Video/PCR
    remapPairs[2].newPid = 200; // New Video/PCR
    remapPairs[3].oldPid = 2001; // Old Audio
    remapPairs[3].newPid = 201; // New Audio

    totalPassed = UnitTest11XX(pPlainMpg, 1, 100, 1000, 4, remapPairs, 0);
    for (offset = 1; offset < IFS_TRANSPORT_PACKET_SIZE; offset++)
        iseq(totalPassed, UnitTest11XX(pPlainMpg, 1, 100, 1000, 4,
            remapPairs, offset));
}

static void UnitTest1102(void)                                                  // 1102.0000 - remap and filter program 2 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      101    1001     PMT
    //     3000     300     Video/PCR
    //     3001     301     Audio
    //     3002     302     Audio
    //
    // and the remapper set up to remap PATs and PMTs for all the various
    // residual offset possibilites.

    size_t offset;
    RemapPair remapPairs[5];
    NumPackets totalPassed;

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 101; // Old PMT
    remapPairs[1].newPid = 1001; // New PMT
    remapPairs[2].oldPid = 3000; // Old Video/PCR
    remapPairs[2].newPid = 300; // New Video/PCR
    remapPairs[3].oldPid = 3001; // Old Audio
    remapPairs[3].newPid = 301; // New Audio
    remapPairs[4].oldPid = 3002; // Old Audio
    remapPairs[4].newPid = 302; // New Audio

    totalPassed = UnitTest11XX(pPlainMpg, 2, 101, 1001, 5, remapPairs, 0);
    for (offset = 1; offset < IFS_TRANSPORT_PACKET_SIZE; offset++)
        iseq(totalPassed, UnitTest11XX(pPlainMpg, 2, 101, 1001, 5,
            remapPairs, offset));
}

static void UnitTest1103(void)                                                  // 1103.0000 - remap and filter program 3 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      102    1002     PMT
    //     5000     500     Video/PCR
    //     3999     399     Audio
    //
    // and the remapper set up to remap PATs and PMTs for all the various
    // residual offset possibilites.

    size_t offset;
    RemapPair remapPairs[4];
    NumPackets totalPassed;

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 102; // Old PMT
    remapPairs[1].newPid = 1002; // New PMT
    remapPairs[2].oldPid = 5000; // Old Video/PCR
    remapPairs[2].newPid = 500; // New Video/PCR
    remapPairs[3].oldPid = 3999; // Old Audio
    remapPairs[3].newPid = 399; // New Audio

    totalPassed = UnitTest11XX(pPlainMpg, 3, 102, 1002, 4, remapPairs, 0);
    for (offset = 1; offset < IFS_TRANSPORT_PACKET_SIZE; offset++)
        iseq(totalPassed, UnitTest11XX(pPlainMpg, 3, 102, 1002, 4,
            remapPairs, offset));
}

static void UnitTest1104(void)                                                  // 1104.0000 - remap and filter program 4 of plain.mpg
{
    // This test processes the entire plain.mpg file with the pid filter
    // set up as follows:
    //
    //  OLD_PID NEW_PID
    //        0       0     PAT
    //      103    1003     PMT
    //     5000     500     Video/PCR
    //     5001     501     Audio
    //
    // and the remapper set up to remap PATs and PMTs for all the various
    // residual offset possibilites.

    size_t offset;
    RemapPair remapPairs[4];
    NumPackets totalPassed;

    remapPairs[0].oldPid = 0; // Old PAT
    remapPairs[0].newPid = 0; // New PAT
    remapPairs[1].oldPid = 103; // Old PMT
    remapPairs[1].newPid = 1003; // New PMT
    remapPairs[2].oldPid = 5000; // Old Video/PCR
    remapPairs[2].newPid = 500; // New Video/PCR
    remapPairs[3].oldPid = 5001; // Old Audio
    remapPairs[3].newPid = 501; // New Audio

    totalPassed = UnitTest11XX(pPlainMpg, 4, 103, 1003, 4, remapPairs, 0);
    for (offset = 1; offset < IFS_TRANSPORT_PACKET_SIZE; offset++)
        iseq(totalPassed, UnitTest11XX(pPlainMpg, 4, 103, 1003, 4,
            remapPairs, offset));
}

#define NO_WRAP_CIRCULAR_BUFFER (365*24*60*60) // One year in seconds
static void UnitTest121X(char * saveName, const char * fileName,                // 121X.0000 - create a very large TSB
        RemapPid videoPid, RemapPid audioPid)
{
    unsigned char packet[10 * IFS_TRANSPORT_PACKET_SIZE];
    NumBytes saveSize;
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // writer
    FILE * pInFile = fopen(fileName, "rb+");
    size_t i, total;

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenWriter(pOutPath,                                                // 121X.0010 - open a maximum sized TSB
                    NULL,
                    NO_WRAP_CIRCULAR_BUFFER,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsStart(ifsHandle, videoPid, audioPid),                               // 121X.0020 - start the TSB
        IfsReturnCodeNoErrorReported);

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 121X.0030 - verify the TSB start state
    iseq(pIfsInfo->mpegSize, 0 );
    iseq(pIfsInfo->ndexSize, 0 );
    iseq(pIfsInfo->begClock, 0 );
    iseq(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, videoPid);
    iseq(pIfsInfo->audioPid, audioPid);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    strcpy(saveName, pIfsInfo->name);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    i = 0;

    while ((total = fread(packet, IFS_TRANSPORT_PACKET_SIZE, 10, pInFile)) != 0)
    {
        // Pretend each set of packets contains 1 second of data...
        test(IfsWrite(ifsHandle, ++i*NSEC_PER_SEC, total, (IfsPacket *)packet),
            IfsReturnCodeNoErrorReported);
    }

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 121X.0040 - verify the TSB ending state
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, videoPid);
    iseq(pIfsInfo->audioPid, audioPid);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    saveSize = pIfsInfo->mpegSize + pIfsInfo->ndexSize;

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 121X.0050 - close the TSB

    test(IfsPathNameInfo(pOutPath, saveName, &pIfsInfo),                        // 121X.0060 - verify the TSB closed state
        IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(saveSize, pIfsInfo->mpegSize+pIfsInfo->ndexSize);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    isne(ifsHandle, NULL);
    test(IfsOpenWriter(pOutPath, saveName, 0, &ifsHandle),                      // 121X.0070 - attempt to reopen the TSB as a single file
            IfsReturnCodeBadMaxSizeValue);
    iseq(ifsHandle, NULL);

    test(IfsOpenWriter(pOutPath,                                                // 121X.0080 - properly reopen the TSB
                    saveName,
                    NO_WRAP_CIRCULAR_BUFFER,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    iseq(saveSize, ifsHandle->mpegSize+ifsHandle->ndexSize);

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 121X.0090 - verify the TSB reopen state
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(saveSize, pIfsInfo->mpegSize+pIfsInfo->ndexSize);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);                    // 121X.0100 - close the TSB

    fclose(pInFile);
}

#ifdef DO_RANDOM_SEEKS
static void UnitTest122X(char * saveName)                                       // 122X.0000 - random compare of IfsSeekToTime to IfsSeekToPacket on a large TSB
{
    IfsInfo * pIfsInfo;
    IfsHandle ifsHandle; // reader
    size_t i;

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    printf("Start random Large TSB seek test\n");

    for (i = 0; i < 1000; i++)
    {
        NumPackets position;

        IfsClock location = ((pIfsInfo->endClock - pIfsInfo->begClock) * rand()
                / RAND_MAX) + pIfsInfo->begClock;
        IfsClock ifsClock;

        test(IfsSeekToTime(ifsHandle, IfsDirectEither, &location, &position),
            IfsReturnCodeNoErrorReported);
        test(IfsSeekToPacket(ifsHandle, position, &ifsClock),
            IfsReturnCodeNoErrorReported);
        iseq(ifsClock, location);
    }

    printf("EndOf random Large TSB seek test\n");

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);
}
#endif

static void UnitTest123Y(IfsHandle ifsHandle, IfsHandle outHandle,              // 123Y.0000 - subroutime to test the IfsConvert/IfsAppend functionality on a large TSB
        FILE * pInFile, IfsClock begClock, IfsClock endClock)
{
    unsigned char packet[IFS_TRANSPORT_PACKET_SIZE];
    IfsClock tmpBegClock, tmpEndClock;
    NumPackets position;
    IfsInfo * pIfsInfo;
    size_t i;
    char temp[32];

    tmpBegClock = begClock;
    tmpEndClock = endClock;

    test(IfsConvert(ifsHandle, outHandle, &tmpBegClock, &tmpEndClock),          // 123Y.0010 - start the conversion (call IfsConvert)
        IfsReturnCodeNoErrorReported);
    test(IfsSeekToTime(ifsHandle, IfsDirectBegin, &tmpBegClock, &position),
        IfsReturnCodeNoErrorReported);

    while (tmpEndClock < endClock)                                              // 123Y.0020 - finish the conversion (call IfsAppend until done)
    {
        //printf("tmpEndClock %s LT ", IfsLongLongToString(tmpEndClock));
        //printf("endClock %s\n", IfsLongLongToString(endClock   ));
        tmpEndClock = endClock;
        test(IfsAppend(ifsHandle, outHandle, &tmpEndClock),
            IfsReturnCodeNoErrorReported);
    }
    //printf("tmpEndClock %s GE ", IfsLongLongToString(tmpEndClock));
    //printf("endClock %s\n", IfsLongLongToString(endClock   ));

    // Check the results

    test(IfsHandleInfo(outHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);    // 123Y.0030 - verify the ending state
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isge(pIfsInfo->begClock, tmpBegClock );
    isle(pIfsInfo->endClock, tmpEndClock );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    iseq(fseek(pInFile, position*IFS_TRANSPORT_PACKET_SIZE, SEEK_SET), 0);
    test(IfsSeekToPacket(outHandle, 0, NULL), IfsReturnCodeNoErrorReported);

    printf("Comparing %s bytes, %ld packets, position %ld\n",                   // 123Y.0040 - Compare the converted file to the original TSB
            IfsLongLongToString(pIfsInfo->mpegSize, temp),
            (long) (pIfsInfo->mpegSize / IFS_TRANSPORT_PACKET_SIZE), position);

    for (i = 0; i < pIfsInfo->mpegSize / IFS_TRANSPORT_PACKET_SIZE; i++)
    {
        IfsPacket * pBuffer = NULL;
        NumPackets numPackets = 1;

        test(IfsRead(outHandle, &numPackets, NULL, &pBuffer),
            IfsReturnCodeNoErrorReported);
        if (numPackets)
        {
            iseq(fread(packet, 1, IFS_TRANSPORT_PACKET_SIZE, pInFile),
                IFS_TRANSPORT_PACKET_SIZE);
            isne(pBuffer, NULL);
            if (pBuffer == NULL)
                return;
            iseq(memcmp(packet, pBuffer, IFS_TRANSPORT_PACKET_SIZE), 0);
            g_free(pBuffer);
        }
        else
        {
            i--; // back up and try again
            iseq(pBuffer, NULL);
        }
    }

#ifdef DO_RANDOM_SEEKS

    printf("Start random Large saved recording seek test\n");                   // 123Y.0050 - random compare of IfsSeekToTime to IfsSeekToPacket on the converted file

    for (i = 0; i < 1000; i++)
    {
        IfsClock location = ((pIfsInfo->endClock - pIfsInfo->begClock) * rand()
                / RAND_MAX) + pIfsInfo->begClock;
        IfsClock ifsClock;

        test(IfsSeekToTime(outHandle, IfsDirectEither, &location, &position),
            IfsReturnCodeNoErrorReported);
        test(IfsSeekToPacket(outHandle, position, &ifsClock),
            IfsReturnCodeNoErrorReported);
        iseq(ifsClock, location);
    }

    printf("EndOf random Large saved recording seek test\n");

#endif

    test(IfsStop(outHandle), IfsReturnCodeNoErrorReported);
}

static void UnitTest123X(char * saveName, const char * fileName)                // 123X.0000 - subroutine to call test 123Y for various conversion possibilites
{
    IfsClock hw, p1, p2, p3, p4;
    IfsHandle ifsHandle; // reader
    IfsHandle outHandle; // writer
    IfsInfo * pIfsInfo;

    FILE * const pInFile = fopen(fileName, "rb+");
    char saveOutName[256];

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenWriter(pOutPath,
                    NULL,
                    0,
                    &outHandle), IfsReturnCodeNoErrorReported);
    isne(outHandle, NULL);
    if (outHandle == NULL)
        return;

    test(IfsHandleInfo(outHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    iseq(pIfsInfo->mpegSize, 0 );
    iseq(pIfsInfo->ndexSize, 0 );
    iseq(pIfsInfo->begClock, 0 );
    iseq(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    strcpy(saveOutName, pIfsInfo->name);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    hw = (pIfsInfo->endClock - pIfsInfo->begClock) / 2;
    p1 = pIfsInfo->begClock > hw ? pIfsInfo->begClock - hw : 0;
    p2 = pIfsInfo->begClock;
    p3 = pIfsInfo->begClock + hw;
    p4 = pIfsInfo->endClock;

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    UnitTest123Y(ifsHandle, outHandle, pInFile, p1, p4);                        // 123X.0010 - call test 123Y from begin-50% to end
    UnitTest123Y(ifsHandle, outHandle, pInFile, p1, p3);                        // 123X.0020 - call test 123Y from begin-50% to begin+50%
    UnitTest123Y(ifsHandle, outHandle, pInFile, p2, p4);                        // 123X.0030 - call test 123Y from begin to end
    UnitTest123Y(ifsHandle, outHandle, pInFile, p2, p3);                        // 123X.0040 - call test 123Y from begin to begin+50%
    UnitTest123Y(ifsHandle, outHandle, pInFile, p3, p4);                        // 123X.0050 - call test 123Y from begin+50% to the end

    test(IfsClose(outHandle), IfsReturnCodeNoErrorReported);
    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    test(IfsDelete(pOutPath, saveOutName), IfsReturnCodeNoErrorReported);

    fclose(pInFile);
}

#define MAX_RAND_BUFF_SIZE 64
static void UnitTest124X(char * saveName, const char * fileName)                // 124X.0000 - full file compare with random transfer lengths on a large TSB
{
    unsigned char packet[MAX_RAND_BUFF_SIZE * IFS_TRANSPORT_PACKET_SIZE];
    FILE * pInFile = fopen(fileName, "rb+");
    IfsHandle ifsHandle; // reader
    IfsInfo * pIfsInfo;
    NumPackets position;
    NumBytes numBytes = 0;
    char temp[32];

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    test(IfsSeekToTime(ifsHandle, IfsDirectBegin, &pIfsInfo->begClock,
        &position), IfsReturnCodeNoErrorReported);

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    iseq(fseek(pInFile, position*IFS_TRANSPORT_PACKET_SIZE, SEEK_SET), 0);

    while (IfsTrue)
    {
        IfsPacket * pBuffer = NULL;
        NumPackets numPackets = (rand() & (MAX_RAND_BUFF_SIZE - 1)) + 1; // 1..32
        IfsReturnCode ifsReturnCode = IfsRead(ifsHandle, &numPackets, NULL,
                &pBuffer);

        if (ifsReturnCode == IfsReturnCodeNoErrorReported)
        {
            if (numPackets)
            {
                isne(pBuffer, NULL);
                if (pBuffer == NULL)
                    return;

                iseq(fread(packet, IFS_TRANSPORT_PACKET_SIZE, numPackets,
                    pInFile), numPackets);
                iseq(memcmp(packet, pBuffer,
                    IFS_TRANSPORT_PACKET_SIZE*numPackets), 0);
                g_free(pBuffer);
                numBytes += (NumBytes) IFS_TRANSPORT_PACKET_SIZE
                        * (NumBytes) numPackets;
            }
            else
            {
                iseq(pBuffer, NULL);
            }
        }
        else if (ifsReturnCode == IfsReturnCodeReadPastEndOfFile)
        {
            iseq(pBuffer, NULL);
            break;
        }
        else
        {
            iseq(pBuffer, NULL);
            iseq(ifsReturnCode, IfsReturnCodeNoErrorReported);
            break;
        }
    }

    printf("Compared  %s bytes, %ld packets, position %ld\n",
            IfsLongLongToString(numBytes, temp), (long) (numBytes
                    / IFS_TRANSPORT_PACKET_SIZE), position);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    fclose(pInFile);
}

#undef NUMBER_OF_RANDOM_I_FRAMES
#define NUMBER_OF_RANDOM_I_FRAMES 20
static void UnitTest125X(char * saveName, const char * fileName)                // 125X.0000 - random I frame seek, read, verify test on a large TSB
{
    FILE * pInFile = fopen(fileName, "rb+");
    IfsHandle ifsHandle; // reader
    IfsInfo * pIfsInfo;
    IfsPacket * pData;
    NumPackets numPackets;
    NumPackets startPacket;
    NumBytes numBytes = 0;
    int i, iFrames = 0;
    char temp[32];

    isne(pInFile, NULL);
    if (pInFile == NULL)
        return;

    rewind(pInFile);

    test(IfsOpenReader(pOutPath,
                    saveName,
                    &ifsHandle), IfsReturnCodeNoErrorReported);
    isne(ifsHandle, NULL);
    if (ifsHandle == NULL)
        return;

    test(IfsHandleInfo(ifsHandle, &pIfsInfo), IfsReturnCodeNoErrorReported);
    isne(pIfsInfo->mpegSize, 0 );
    isne(pIfsInfo->ndexSize, 0 );
    isne(pIfsInfo->begClock, 0 );
    isne(pIfsInfo->endClock, 0 );
    iseq(pIfsInfo->videoPid, IFS_UNDEFINED_PID);
    iseq(pIfsInfo->audioPid, IFS_UNDEFINED_PID);
    isne(pIfsInfo->path , NULL );
    isne(pIfsInfo->name , NULL );

    printf("Start random Large I frame test\n");

    for (i = 0; i < NUMBER_OF_RANDOM_I_FRAMES; i++)
    {
        IfsReturnCode ifsReturnCode;
        IfsClock location = ((pIfsInfo->endClock - pIfsInfo->begClock) * rand()
                / RAND_MAX) + pIfsInfo->begClock;

        test(IfsSeekToTime(ifsHandle, IfsDirectEither, &location, NULL),
            IfsReturnCodeNoErrorReported);

        ifsReturnCode = IfsReadPicture(ifsHandle, 0, 0, IfsReadTypeNearest,
                &numPackets, &pData, &startPacket);

        if (ifsReturnCode == IfsReturnCodeNoErrorReported)
        {
            IfsPacket * pBuffer;
            isne(numPackets, 0);
            isne(pData, NULL);
            if (pData == NULL)
                return;

            pBuffer = g_try_malloc(IFS_TRANSPORT_PACKET_SIZE * numPackets);
            isne(pBuffer, NULL);
            if (pBuffer == NULL)
                return;

            iseq(fseek(pInFile, startPacket*IFS_TRANSPORT_PACKET_SIZE,
                SEEK_SET), 0);
            iseq(fread(pBuffer, IFS_TRANSPORT_PACKET_SIZE, numPackets,
                pInFile), numPackets);
            iseq(memcmp(pData, pBuffer,
                IFS_TRANSPORT_PACKET_SIZE*numPackets), 0);

            g_free(pBuffer);
            g_free(pData);
            iFrames++;
            numBytes += (NumBytes) IFS_TRANSPORT_PACKET_SIZE
                    * (NumBytes) numPackets;
        }
        else
        {
            printf("Error is %s\n", IfsReturnCodeToString(ifsReturnCode));
            iseq(numPackets, 0);
            iseq(pData, NULL);
        }
    }

    printf("EndOf random Large I frame test\n");

    test(IfsFreeInfo(pIfsInfo), IfsReturnCodeNoErrorReported);

    test(IfsClose(ifsHandle), IfsReturnCodeNoErrorReported);

    printf("Compared %s bytes, %ld packets, %d of %d I frames\n",
            IfsLongLongToString(numBytes, temp), (long) (numBytes
                    / IFS_TRANSPORT_PACKET_SIZE), iFrames,
            NUMBER_OF_RANDOM_I_FRAMES);

    fclose(pInFile);
}

static void UnitTest12XX(const char * fileName, RemapPid videoPid,              // 12XX.0000 - call all the 12XX tests on the given program in the given file
        RemapPid audioPid)
{
    char saveName[256];

    UnitTest121X(saveName, fileName, videoPid, audioPid);                       // 12XX.0010 - call test 121X on the given program in the given file
#ifdef DO_RANDOM_SEEKS
    UnitTest122X(saveName);                                                     // 12XX.0020 - call test 122X with the file produced by test 121X
#endif
    UnitTest123X(saveName, fileName);                                           // 12XX.0030 - call test 123X with the file produced by test 121X
    UnitTest124X(saveName, fileName);                                           // 12XX.0040 - call test 124X with the file produced by test 121X
    UnitTest125X(saveName, fileName);                                           // 12XX.0050 - call test 125X with the file produced by test 121X

    test(IfsDelete(pOutPath, saveName), IfsReturnCodeNoErrorReported);          // 12XX.0060 - delete the file produced by test 121X
}

static void UnitTest1200(void)                                                  // 1200.0000 - call all the 12XX tests on program 1 of background.mpg
{
    UnitTest12XX(pBackgMpg, 68, 0);
}
static void UnitTest1201(void)                                                  // 1201.0000 - call all the 12XX tests on program 1 of plain.mpg
{
    UnitTest12XX(pPlainMpg, 2000, 2001);
}
static void UnitTest1202(void)                                                  // 1202.0000 - call all the 12XX tests on program 2 of plain.mpg
{
    UnitTest12XX(pPlainMpg, 3000, 3001);
}
static void UnitTest1203(void)                                                  // 1203.0000 - call all the 12XX tests on program 3 of plain.mpg
{
    UnitTest12XX(pPlainMpg, 5000, 3999);
}
static void UnitTest1204(void)                                                  // 1204.0000 - call all the 12XX tests on program 4 of plain.mpg
{
    UnitTest12XX(pPlainMpg, 2000, 5001);
}

int main(int argc, char *argv[])
{
    char temp[32];

    if (ProcessArguments(argc, argv))
        return 0;

    IfsInit();

    IfsSetMode(IfsIndexDumpModeDef, IfsIndexerSettingUnitest);

    if (debug)
    {
        char saveName[256];
        printf("Starting Unit Test  100\n");
        UnitTest100(saveName);
        printf("Starting Unit Test  101\n");
        UnitTest101(saveName);
        printf("Starting Unit Test  102\n");
        UnitTest102(saveName);
        printf("Starting Unit Test  103\n");
        UnitTest103(saveName);
        printf("Starting Unit Test  200\n");
        UnitTest200();
        printf("Starting Unit Test  300\n");
        UnitTest300();
        printf("Starting Unit Test  400\n");
        UnitTest400();
        printf("Starting Unit Test  450\n");
        UnitTest450();
        printf("Starting Unit Test  500\n");
        UnitTest500();
        printf("Starting Unit Test  600\n");
        UnitTest600();
        printf("Starting Unit Test  700\n");
        UnitTest700();
        printf("Starting Unit Test  701\n");
        UnitTest701();
        printf("Starting Unit Test  702\n");
        UnitTest702();
        printf("Starting Unit Test  703\n");
        UnitTest703();
        printf("Starting Unit Test  704\n");
        UnitTest704();
        printf("Starting Unit Test  800\n");
        UnitTest800(saveName);
        printf("Starting Unit Test  800-901\n");
        UnitTest901(saveName);
        printf("Starting Unit Test  800-902\n");
        UnitTest902(saveName);
        printf("Starting Unit Test  800-903\n");
        UnitTest903(saveName);
        printf("Starting Unit Test  800-904\n");
        UnitTest904(saveName);
        printf("Starting Unit Test  801\n");
        UnitTest801(saveName);
        printf("Starting Unit Test  801-901\n");
        UnitTest901(saveName);
        printf("Starting Unit Test  801-902\n");
        UnitTest902(saveName);
        printf("Starting Unit Test  801-903\n");
        UnitTest903(saveName);
        printf("Starting Unit Test  801-904\n");
        UnitTest904(saveName);
        printf("Starting Unit Test  802\n");
        UnitTest802(saveName);
        printf("Starting Unit Test  802-901\n");
        UnitTest901(saveName);
        printf("Starting Unit Test  802-902\n");
        UnitTest902(saveName);
        printf("Starting Unit Test  802-903\n");
        UnitTest903(saveName);
        printf("Starting Unit Test  802-904\n");
        UnitTest904(saveName);
        printf("Starting Unit Test  803\n");
        UnitTest803(saveName);
        printf("Starting Unit Test  803-901\n");
        UnitTest901(saveName);
        printf("Starting Unit Test  803-902\n");
        UnitTest902(saveName);
        printf("Starting Unit Test  803-903\n");
        UnitTest903(saveName);
        printf("Starting Unit Test  803-904\n");
        UnitTest904(saveName);
        printf("Starting Unit Test  804\n");
        UnitTest804(saveName);
        printf("Starting Unit Test  804-901\n");
        UnitTest901(saveName);
        printf("Starting Unit Test  804-902\n");
        UnitTest902(saveName);
        printf("Starting Unit Test  804-903\n");
        UnitTest903(saveName);
        printf("Starting Unit Test  804-904\n");
        UnitTest904(saveName);
        printf("Starting Unit Test 1000\n");
        UnitTest1000();
        printf("Starting Unit Test 1001\n");
        UnitTest1001();
        printf("Starting Unit Test 1002\n");
        UnitTest1002();
        printf("Starting Unit Test 1003\n");
        UnitTest1003();
        printf("Starting Unit Test 1004\n");
        UnitTest1004();
        printf("Starting Unit Test 1100\n");
        UnitTest1100();
        printf("Starting Unit Test 1101\n");
        UnitTest1101();
        printf("Starting Unit Test 1102\n");
        UnitTest1102();
        printf("Starting Unit Test 1103\n");
        UnitTest1103();
        printf("Starting Unit Test 1104\n");
        UnitTest1104();
        printf("Starting Unit Test 1200\n");
        UnitTest1200();
        printf("Starting Unit Test 1201\n");
        UnitTest1201();
        printf("Starting Unit Test 1202\n");
        UnitTest1202();
        printf("Starting Unit Test 1203\n");
        UnitTest1203();
        printf("Starting Unit Test 1204\n");
        UnitTest1204();

        printf("\nRan %s tests", IfsLongLongToString(testNumber, temp));
        printf(", %s passed, ", IfsLongLongToString(testPassed, temp));
        printf("%s failed\n", IfsLongLongToString(testFailed, temp));
    }
    else
    {
        char saveName[256];

        UnitTest100(saveName);
        UnitTest101(saveName);
        UnitTest102(saveName);
        UnitTest103(saveName);

        UnitTest200();
        UnitTest300();
        UnitTest400();
        UnitTest450();
        UnitTest500();
        UnitTest600();

        UnitTest700();
        UnitTest701();
        UnitTest702();
        UnitTest703();
        UnitTest704();

        UnitTest800(saveName);
        UnitTest901(saveName);
        UnitTest902(saveName);
        UnitTest903(saveName);
        UnitTest904(saveName);

        UnitTest801(saveName);
        UnitTest901(saveName);
        UnitTest902(saveName);
        UnitTest903(saveName);
        UnitTest904(saveName);

        UnitTest802(saveName);
        UnitTest901(saveName);
        UnitTest902(saveName);
        UnitTest903(saveName);
        UnitTest904(saveName);

        UnitTest803(saveName);
        UnitTest901(saveName);
        UnitTest902(saveName);
        UnitTest903(saveName);
        UnitTest904(saveName);

        UnitTest804(saveName);
        UnitTest901(saveName);
        UnitTest902(saveName);
        UnitTest903(saveName);
        UnitTest904(saveName);

        UnitTest1000();
        UnitTest1001();
        UnitTest1002();
        UnitTest1003();
        UnitTest1004();

        UnitTest1100();
        UnitTest1101();
        UnitTest1102();
        UnitTest1103();
        UnitTest1104();

        UnitTest1200();
        UnitTest1201();
        UnitTest1202();
        UnitTest1203();
        UnitTest1204();

        printf("\nRan %s tests", IfsLongLongToString(testNumber, temp));
        printf(", %s passed, ", IfsLongLongToString(testPassed, temp));
        printf("%s failed\n", IfsLongLongToString(testFailed, temp));
    }

    return testFailed;
}
