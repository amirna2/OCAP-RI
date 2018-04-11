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

/*
 * Copyright 2001 Unicode, Inc.
 *
 * Disclaimer
 *
 * This source code is provided as is by Unicode, Inc. No claims are
 * made as to fitness for any particular purpose. No warranties of any
 * kind are expressed or implied. The recipient agrees to determine
 * applicability of information provided. If this file has been
 * purchased on magnetic or optical media from Unicode, Inc., the
 * sole remedy for any claim will be exchange of defective media
 * within 90 days of receipt.
 *
 * Limitations on Rights to Redistribute This Code
 *
 * Unicode, Inc. hereby grants the right to freely use the information
 * supplied in this file in the creation of products supporting the
 * Unicode Standard, and to make copies of this file in any form
 * for internal or external distribution as long as this notice
 * remains attached.
 */

/* ---------------------------------------------------------------------

 Conversions between UTF32, UTF-16, and UTF-8. Source code file.
 Author: Mark E. Davis, 1994.
 Rev History: Rick McGowan, fixes & updates May 2001.
 Sept 2001: fixed const & error conditions per
 mods suggested by S. Parent & A. Lillich.

 See the header file "ConvertUTF.h" for complete documentation.

 ------------------------------------------------------------------------ */

#include "ConvertUTF.h"
#ifdef CVTUTF_DEBUG
#include <stdio.h>
#endif

static const int halfShift = 10; /* used for shifting by 10 bits */

static const UTF32 halfBase = 0x0010000UL;
static const UTF32 halfMask = 0x3FFUL;

#define UNI_SUR_HIGH_START  (UTF32)0xD800
#define UNI_SUR_HIGH_END    (UTF32)0xDBFF
#define UNI_SUR_LOW_START   (UTF32)0xDC00
#define UNI_SUR_LOW_END     (UTF32)0xDFFF
#define false           0
#define true            1

/* --------------------------------------------------------------------- */

ConversionResult ConvertUTF32toUTF16(const UTF32** sourceStart,
        const UTF32* sourceEnd, UTF16** targetStart, UTF16* targetEnd,
        ConversionFlags flags)
{
    ConversionResult result = conversionOK;
    const UTF32* source = *sourceStart;
    UTF16* target = *targetStart;
    while (source < sourceEnd)
    {
        UTF32 ch;
        if (target >= targetEnd)
        {
            result = targetExhausted;
            break;
        }
        ch = *source++;
        if (ch <= UNI_MAX_BMP) /* Target is a character <= 0xFFFF */
        {
            if ((flags == strictConversion) && (ch >= UNI_SUR_HIGH_START && ch
                    <= UNI_SUR_LOW_END))
            {
                result = sourceIllegal;
                break;
            }
            else
            {
                *target++ = (unsigned short) ch; /* normal case */
            }
        }
        else if (ch > UNI_MAX_UTF16)
        {
            if (flags == strictConversion)
            {
                result = sourceIllegal;
            }
            else
            {
                *target++ = UNI_REPLACEMENT_CHAR;
            }
        }
        else
        {
            /* target is a character in range 0xFFFF - 0x10FFFF. */
            if (target + 1 >= targetEnd)
            {
                result = targetExhausted;
                break;
            }
            ch -= halfBase;
            *target++ = (unsigned short) ((ch >> halfShift)
                    + UNI_SUR_HIGH_START);
            *target++ = (unsigned short) ((ch & halfMask) + UNI_SUR_LOW_START);
        }
    }
    return result;
}

/* --------------------------------------------------------------------- */

ConversionResult ConvertUTF16toUTF32(const UTF16** sourceStart,
        const UTF16* sourceEnd, UTF32** targetStart, UTF32* targetEnd,
        ConversionFlags flags)
{
    ConversionResult result = conversionOK;
    const UTF16* source = *sourceStart;
    UTF32* target = *targetStart;
    UTF32 ch, ch2;
    while (source < sourceEnd)
    {
        ch = *source++;
        if (ch >= UNI_SUR_HIGH_START && ch <= UNI_SUR_HIGH_END && source
                < sourceEnd)
        {
            ch2 = *source;
            if (ch2 >= UNI_SUR_LOW_START && ch2 <= UNI_SUR_LOW_END)
            {
                ch = ((ch - UNI_SUR_HIGH_START) << halfShift) + (ch2
                        - UNI_SUR_LOW_START) + halfBase;
                ++source;
            }
            else if (flags == strictConversion) /* it's an unpaired high surrogate */
            {
                result = sourceIllegal;
                break;
            }
        }
        else if ((flags == strictConversion) && (ch >= UNI_SUR_LOW_START && ch
                <= UNI_SUR_LOW_END))
        {
            /* an unpaired low surrogate */
            result = sourceIllegal;
            break;
        }
        if (target >= targetEnd)
        {
            result = targetExhausted;
            break;
        }
        *target++ = ch;
    }
#ifdef CVTUTF_DEBUG
    if (result == sourceIllegal)
    {
        fprintf(stderr, "ConvertUTF16toUTF32 illegal seq 0x%04x,%04x\n", ch, ch2);
        fflush(stderr);
    }
#endif
    return result;
}

/* --------------------------------------------------------------------- */

/*
 * Index into the table below with the first byte of a UTF-8 sequence to
 * get the number of trailing bytes that are supposed to follow it.
 */
static const char trailingBytesForUTF8[256] =
{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1,
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
        1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3,
        3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5 };

/*
 * Magic values subtracted from a buffer value during UTF8 conversion.
 * This table contains as many values as there might be trailing bytes
 * in a UTF-8 sequence.
 */
static const UTF32 offsetsFromUTF8[6] =
{ 0x00000000UL, 0x00003080UL, 0x000E2080UL, 0x03C82080UL, 0xFA082080UL,
        0x82082080UL };

/*
 * Once the bits are split out into bytes of UTF-8, this is a mask OR-ed
 * into the first byte, depending on how many bytes follow.  There are
 * as many entries in this table as there are UTF-8 sequence types.
 * (I.e., one byte sequence, two byte... six byte sequence.)
 */
static const UTF8 firstByteMark[7] =
{ 0x00, 0x00, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC };

// RI -- Add this function to calculate the output buffer length before
// performing the actual conversion 
ConversionResult SizeUTF16toUTF8(const UTF16** sourceStart,
        const UTF16* sourceEnd, ConversionFlags flags, long *totalBytes)
{
    ConversionResult result = conversionOK;
    const UTF16* source = *sourceStart;

    *totalBytes = 0;

    while (source < sourceEnd)
    {
        UTF32 ch;
        unsigned short bytesToWrite = 0;
        ch = *source++;
        /* If we have a surrogate pair, convert to UTF32 first. */
        if (ch >= UNI_SUR_HIGH_START && ch <= UNI_SUR_HIGH_END && source
                < sourceEnd)
        {
            UTF32 ch2 = *source;
            if (ch2 >= UNI_SUR_LOW_START && ch2 <= UNI_SUR_LOW_END)
            {
                ch = ((ch - UNI_SUR_HIGH_START) << halfShift) + (ch2
                        - UNI_SUR_LOW_START) + halfBase;
                ++source;
            }
            else if (flags == strictConversion) /* it's an unpaired high surrogate */
            {
                result = sourceIllegal;
                break;
            }
        }
        else if ((flags == strictConversion) && (ch >= UNI_SUR_LOW_START && ch
                <= UNI_SUR_LOW_END))
        {
            result = sourceIllegal;
            break;
        }
        /* Figure out how many bytes the result will require */
        if (ch < (UTF32) 0x80)
        {
            bytesToWrite = 1;
        }
        else if (ch < (UTF32) 0x800)
        {
            bytesToWrite = 2;
        }
        else if (ch < (UTF32) 0x10000)
        {
            bytesToWrite = 3;
        }
        else if (ch < (UTF32) 0x200000)
        {
            bytesToWrite = 4;
        }
        else
        {
            bytesToWrite = 2;
            ch = UNI_REPLACEMENT_CHAR;
        }

        *totalBytes += bytesToWrite;
    }
    return result;
}

/* --------------------------------------------------------------------- */

/* The interface converts a whole buffer to avoid function-call overhead.
 * Constants have been gathered. Loops & conditionals have been removed as
 * much as possible for efficiency, in favor of drop-through switches.
 * (See "Note A" at the bottom of the file for equivalent code.)
 * If your compiler supports it, the "isLegalUTF8" call can be turned
 * into an inline function.
 */

/* --------------------------------------------------------------------- */

ConversionResult ConvertUTF16toUTF8(const UTF16** sourceStart,
        const UTF16* sourceEnd, UTF8** targetStart, UTF8* targetEnd,
        ConversionFlags flags)
{
    ConversionResult result = conversionOK;
    const UTF16* source = *sourceStart;

    UTF8* target = *targetStart;

    while (source < sourceEnd)
    {
        UTF32 ch;
        unsigned short bytesToWrite = 0;
        const UTF32 byteMask = 0xBF;
        const UTF32 byteMark = 0x80;
        ch = *source++;
        /* If we have a surrogate pair, convert to UTF32 first. */
        if (ch >= UNI_SUR_HIGH_START && ch <= UNI_SUR_HIGH_END && source
                < sourceEnd)
        {
            UTF32 ch2 = *source;
            if (ch2 >= UNI_SUR_LOW_START && ch2 <= UNI_SUR_LOW_END)
            {
                ch = ((ch - UNI_SUR_HIGH_START) << halfShift) + (ch2
                        - UNI_SUR_LOW_START) + halfBase;
                ++source;
            }
            else if (flags == strictConversion) /* it's an unpaired high surrogate */
            {
                result = sourceIllegal;
                break;
            }
        }
        else if ((flags == strictConversion) && (ch >= UNI_SUR_LOW_START && ch
                <= UNI_SUR_LOW_END))
        {
            result = sourceIllegal;
            break;
        }
        /* Figure out how many bytes the result will require */
        if (ch < (UTF32) 0x80)
        {
            bytesToWrite = 1;
        }
        else if (ch < (UTF32) 0x800)
        {
            bytesToWrite = 2;
        }
        else if (ch < (UTF32) 0x10000)
        {
            bytesToWrite = 3;
        }
        else if (ch < (UTF32) 0x200000)
        {
            bytesToWrite = 4;
        }
        else
        {
            bytesToWrite = 2;
            ch = UNI_REPLACEMENT_CHAR;
        }

        target += bytesToWrite;
        if (target > targetEnd)
        {
            result = targetExhausted;
            break;
        }
        switch (bytesToWrite)
        /* note: everything falls through. */
        {
        case 4:
            *--target = (unsigned char) ((ch | byteMark) & byteMask);
            ch >>= 6; //lint -e(616)
        case 3:
            *--target = (unsigned char) ((ch | byteMark) & byteMask);
            ch >>= 6; //lint -e(616)
        case 2:
            *--target = (unsigned char) ((ch | byteMark) & byteMask);
            ch >>= 6; //lint -e(616)
        case 1:
            *--target = (unsigned char) (ch | firstByteMark[bytesToWrite]); //lint -e(616)
        }
        target += bytesToWrite;
    }
    return result;
}

/* --------------------------------------------------------------------- */

/*
 * Utility routine to tell whether a sequence of bytes is legal UTF-8.
 * This must be called with the length pre-determined by the first byte.
 * If not calling this from ConvertUTF8to*, then the length can be set by:
 *  length = trailingBytesForUTF8[*source]+1;
 * and the sequence is illegal right away if there aren't that many bytes
 * available.
 * If presented with a length > 4, this returns false.  The Unicode
 * definition of UTF-8 goes up to 4-byte sequences.
 */

static boolean isLegalUTF8(const UTF8 *source, int length)
{
    UTF8 a;
    const UTF8 *srcptr = source + length;
    switch (length)
    {
    default:
        return false;
        /* Everything else falls through when "true"... */
    case 4:
        if ((a = (*--srcptr)) < 0x80 || a > 0xBF)
            return false; //lint -e(616)
    case 3:
        if ((a = (*--srcptr)) < 0x80 || a > 0xBF)
            return false; //lint -e(616)
    case 2:
        if ((a = (*--srcptr)) > 0xBF)
            return false;
        switch (*source)
        {
        /* no fall-through in this inner switch */
        case 0xE0:
            if (a < 0xA0)
                return false;
            break;
        case 0xF0:
            if (a < 0x90)
                return false;
            break;
        case 0xF4:
            if (a > 0x8F)
                return false;
            break;
        default:
            if (a < 0x80)
                return false;
        } //lint -e(616)
    case 1:
        if (*source >= 0x80 && *source < 0xC2)
            return false;
        if (*source > 0xF4)
            return false;
    }
    return true;
}

/* --------------------------------------------------------------------- */

/*
 * Exported function to return whether a UTF-8 sequence is legal or not.
 * This is not used here; it's just exported.
 */
boolean isLegalUTF8Sequence(const UTF8 *source, const UTF8 *sourceEnd)
{
    int length = trailingBytesForUTF8[*source] + 1;
    if (source + length > sourceEnd)
    {
        return false;
    }
    return isLegalUTF8(source, length);
}

/* --------------------------------------------------------------------- */

ConversionResult ConvertUTF8toUTF16(const UTF8** sourceStart,
        const UTF8* sourceEnd, UTF16** targetStart, UTF16* targetEnd,
        ConversionFlags flags)
{
    ConversionResult result = conversionOK;
    const UTF8* source = *sourceStart;
    UTF16* target = *targetStart;
    while (source < sourceEnd)
    {
        UTF32 ch = 0;
        unsigned short extraBytesToRead = trailingBytesForUTF8[*source];
        if (source + extraBytesToRead >= sourceEnd)
        {
            result = sourceExhausted;
            break;
        }
        /* Do this check whether lenient or strict */
        if (!isLegalUTF8(source, extraBytesToRead + 1))
        {
            result = sourceIllegal;
            break;
        }
        /*
         * The cases all fall through. See "Note A" below.
         */
        switch (extraBytesToRead)
        {
        case 3:
            ch += *source++;
            ch <<= 6; //lint -e(616)
        case 2:
            ch += *source++;
            ch <<= 6; //lint -e(616)
        case 1:
            ch += *source++;
            ch <<= 6; //lint -e(616)
        case 0:
            ch += *source++; //lint -e(616)
        }
        ch -= offsetsFromUTF8[extraBytesToRead];

        if (target >= targetEnd)
        {
            result = targetExhausted;
            break;
        }
        if (ch <= UNI_MAX_BMP) /* Target is a character <= 0xFFFF */
        {
            if ((flags == strictConversion) && (ch >= UNI_SUR_HIGH_START && ch
                    <= UNI_SUR_LOW_END))
            {
                result = sourceIllegal;
                break;
            }
            else
            {
                *target++ = (unsigned short) ch; /* normal case */
            }
        }
        else if (ch > UNI_MAX_UTF16)
        {
            if (flags == strictConversion)
            {
                result = sourceIllegal;
                break; /* Bail out; shouldn't continue */
            }
            else
            {
                *target++ = UNI_REPLACEMENT_CHAR;
            }
        }
        else
        {
            /* target is a character in range 0xFFFF - 0x10FFFF. */
            if (target + 1 >= targetEnd)
            {
                result = targetExhausted;
                break;
            }
            ch -= halfBase;
            *target++ = (unsigned short) ((ch >> halfShift)
                    + UNI_SUR_HIGH_START);
            *target++ = (unsigned short) ((ch & halfMask) + UNI_SUR_LOW_START);
        }
    }
    return result;
}

/* --------------------------------------------------------------------- */

ConversionResult ConvertUTF32toUTF8(const UTF32** sourceStart,
        const UTF32* sourceEnd, UTF8** targetStart, UTF8* targetEnd,
        ConversionFlags flags)
{
    ConversionResult result = conversionOK;
    const UTF32* source = *sourceStart;
    UTF8* target = *targetStart;
    while (source < sourceEnd)
    {
        UTF32 ch;
        unsigned short bytesToWrite = 0;
        const UTF32 byteMask = 0xBF;
        const UTF32 byteMark = 0x80;
        ch = *source++;
        /* surrogates of any stripe are not legal UTF32 characters */
        if (flags == strictConversion)
        {
            if ((ch >= UNI_SUR_HIGH_START) && (ch <= UNI_SUR_LOW_END))
            {
                result = sourceIllegal;
                break;
            }
        }
        /* Figure out how many bytes the result will require */
        if (ch < (UTF32) 0x80)
        {
            bytesToWrite = 1;
        }
        else if (ch < (UTF32) 0x800)
        {
            bytesToWrite = 2;
        }
        else if (ch < (UTF32) 0x10000)
        {
            bytesToWrite = 3;
        }
        else if (ch < (UTF32) 0x200000)
        {
            bytesToWrite = 4;
        }
        else
        {
            bytesToWrite = 2;
            ch = UNI_REPLACEMENT_CHAR;
        }

        target += bytesToWrite;
        if (target > targetEnd)
        {
            result = targetExhausted;
            break;
        }
        switch (bytesToWrite)
        /* note: everything falls through. */
        {
        case 4:
            *--target = (unsigned char) ((ch | byteMark) & byteMask);
            ch >>= 6; //lint -e(616)
        case 3:
            *--target = (unsigned char) ((ch | byteMark) & byteMask);
            ch >>= 6; //lint -e(616)
        case 2:
            *--target = (unsigned char) ((ch | byteMark) & byteMask);
            ch >>= 6; //lint -e(616)
        case 1:
            *--target = (unsigned char) (ch | firstByteMark[bytesToWrite]); //lint -e(616)
        }
        target += bytesToWrite;
    }
    return result;
}

/* --------------------------------------------------------------------- */

ConversionResult ConvertUTF8toUTF32(const UTF8** sourceStart,
        const UTF8* sourceEnd, UTF32** targetStart, UTF32* targetEnd,
        ConversionFlags flags)
{
    ConversionResult result = conversionOK;
    const UTF8* source = *sourceStart;
    UTF32* target = *targetStart;
    (void) flags;/* flags parameter is unused here */
    while (source < sourceEnd)
    {
        UTF32 ch = 0;
        unsigned short extraBytesToRead = trailingBytesForUTF8[*source];
        if (source + extraBytesToRead >= sourceEnd)
        {
            result = sourceExhausted;
            break;
        }
        /* Do this check whether lenient or strict */
        if (!isLegalUTF8(source, extraBytesToRead + 1))
        {
            result = sourceIllegal;
            break;
        }
        /*
         * The cases all fall through. See "Note A" below.
         */
        switch (extraBytesToRead)
        {
        case 3:
            ch += *source++;
            ch <<= 6; //lint -e(616)
        case 2:
            ch += *source++;
            ch <<= 6; //lint -e(616)
        case 1:
            ch += *source++;
            ch <<= 6; //lint -e(616)
        case 0:
            ch += *source++; //lint -e(616)
        }
        ch -= offsetsFromUTF8[extraBytesToRead];

        if (target >= targetEnd)
        {
            result = targetExhausted;
            break;
        }
        if (ch <= UNI_MAX_UTF32)
        {
            *target++ = ch;
        }
        else /* i.e., ch > UNI_MAX_UTF32 */
        {
            *target++ = UNI_REPLACEMENT_CHAR;
        }
    }
    return result;
}

/* ---------------------------------------------------------------------

 Note A.
 The fall-through switches in UTF-8 reading code save a
 temp variable, some decrements & conditionals.  The switches
 are equivalent to the following loop:
 {
 int tmpBytesToRead = extraBytesToRead+1;
 do {
 ch += *source++;
 --tmpBytesToRead;
 if (tmpBytesToRead) ch <<= 6;
 } while (tmpBytesToRead > 0);
 }
 In UTF-8 writing code, the switches on "bytesToWrite" are
 similarly unrolled loops.

 --------------------------------------------------------------------- */
