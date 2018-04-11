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

#ifndef _MPEOS_GFX_H_
#define _MPEOS_GFX_H_

#include <mpe_types.h>
#include <mpe_uievents.h>

/***************************************************************
 * GRAPHICS defines
 ***************************************************************/
#define MPE_GFX_UNKNOWN (0)

/* Infinite wait for gfxWaitNextEvent().  */
#define MPE_GFX_WAIT_INFINITE 0xFFFFFFFF

/* Creates a color value from ARGB values. */
#define mpe_gfxArgbToColor(a,r,g,b) ((mpe_GfxColor)((((a)&0xFF) << 24) | (((r)&0xFF) << 16) | (((g)&0xFF) << 8) | ((b)&0xFF)))

/* Creates a color value from RGB values. */
#define mpe_gfxRgbToColor(r,g,b) mpe_gfxArgbToColor((mpe_GfxColor)0xFF, r, g, b)

/* extract a, r,g, b values from a mpe_GfxColor */
#define mpe_gfxGetRed(argb)   ((uint8_t)(((argb) >> 16) & 0xff))
#define mpe_gfxGetGreen(argb) ((uint8_t)(((argb) >> 8) & 0xff))
#define mpe_gfxGetBlue(argb)  ((uint8_t)((argb) & 0xff))
#define mpe_gfxGetAlpha(argb) ((uint8_t)(((argb) >> 24) & 0xff))

typedef uint32_t mpe_GfxColor;
typedef uint16_t mpe_GfxWchar;

/***************************************************************
 * GRAPHICS Handles
 ***************************************************************/

typedef struct _mpeH_GfxScreen_t
{
    int unused1;
}*mpe_GfxScreen;
typedef struct _mpeH_GfxSurface_t
{
    int unused1;
}*mpe_GfxSurface;
typedef struct _mpeH_GfxContext_t
{
    int unused1;
}*mpe_GfxContext;
typedef struct _mpeH_GfxFont_t
{
    int unused1;
}*mpe_GfxFont;
typedef struct _mpeH_GfxFontFactory_t
{
    int unused1;
}*mpe_GfxFontFactory;
typedef struct _mpeH_GfxPalette_t
{
    int unused1;
}*mpe_GfxPalette;

/***************************************************************
 * GRAPHICS public data sturctures
 ***************************************************************/

typedef enum mpe_GfxError
{
    MPE_GFX_ERROR_NOERR = 0, /* no error */
    MPE_GFX_ERROR_UNKNOWN, /* a generic error */
    MPE_GFX_ERROR_OSERR, /* OS failure */
    MPE_GFX_ERROR_NOMEM, /* out of memory */
    MPE_GFX_ERROR_INVALID, /* invalid parameters passed in MPE functions */
    MPE_GFX_ERROR_NOT_SUPPORTED, /* feature not supported */
    MPE_GFX_ERROR_OUT_OF_BOUNDS, /* graphics area is out of bounds */
    MPE_GFX_ERROR_UNIMPLEMENTED, /* feature not implemented */
    MPE_GFX_ERROR_FALSE, /* statement is false */
    MPE_GFX_ERROR_FF_DEL_PENDING, /* font factory deletion is pending */
    MPE_GFX_ERROR_NOFONT, /* no font found in factory */
    MPE_GFX_ERROR_FONTFORMAT,
/* font cannot be created: wrong format */
} mpe_GfxError;

typedef enum mpe_GfxFontFormat
{
    GFX_FONT_PFR, GFX_FONTFORMAT_MAX,
} mpe_GfxFontFormat;

typedef enum mpe_GfxPaintMode
{
    MPE_GFX_XOR = 0,
    MPE_GFX_CLR = 1,
    MPE_GFX_SRC = 2,
    MPE_GFX_SRCOVER = 3,
    MPE_GFX_DSTOVER = 4,
    MPE_GFX_SRCIN = 5,
    MPE_GFX_DSTIN = 6,
    MPE_GFX_SRCOUT = 7,
    MPE_GFX_DSTOUT = 8,
    MPE_GFX_DST = 9,
    MPE_GFX_MODE_MAX
} mpe_GfxPaintMode;

typedef enum mpe_GfxFontStyle
{
    MPE_GFX_PLAIN,
    MPE_GFX_BOLD,
    MPE_GFX_ITALIC,
    MPE_GFX_BOLD_ITALIC,
    MPE_GFX_FNT_STYLE_MAX
} mpe_GfxFontStyle;

typedef enum mpe_GfxColorFormat
{
    MPE_GFX_RGB888, /* 24-bit RGB color w/ no alpha */
    MPE_GFX_RGB565, /* 16-bit RGB color w/ no alpha */
    MPE_GFX_ARGB8888, /* 32-bit RGB color w/ 8-bit alpha in the pixel */
    MPE_GFX_ARGB1555, /* 15-bit RGB color w/ 1-bit alpha in the pixel */
    MPE_GFX_UNDEFINED, /* Undefined format */
    MPE_GFX_CLUT8, /* 8-bit color lookup index */
    MPE_GFX_A8RGB888, /* 24-bit RGB color w/ separate 8-bit alpha) */
    MPE_GFX_A2RGB565, /* 16-bit RGB color w/ separate 2-bit alpha) */
    MPE_GFX_COLOR_FORMAT_MAX
} mpe_GfxColorFormat;

typedef enum mpe_GfxBitDepth
{
    MPE_GFX_1BPP = 1,
    MPE_GFX_2BPP = 2,
    MPE_GFX_4BPP = 4,
    MPE_GFX_8BPP = 8,
    MPE_GFX_16BPP = 16,
    MPE_GFX_24BPP = 24,
    MPE_GFX_32BPP = 32,
    MPE_GFX_BPP_MAX
} mpe_GfxBitDepth;

typedef struct mpe_GfxDimensions
{
    int32_t width;
    int32_t height;
} mpe_GfxDimensions;

typedef struct mpe_GfxPoint
{
    int32_t x;
    int32_t y;
} mpe_GfxPoint;

typedef struct mpe_GfxSurfaceInfo
{
    mpe_GfxColorFormat format; /* color format */
    mpe_GfxBitDepth bpp; /* bits per pixel */
    mpe_GfxDimensions dim; /* width and height in pixels */
    uint32_t widthbytes; /* bytes per line */
    void *pixeldata; /* pixel data */
    mpe_GfxPalette clut; /* color palette, if format is MPE_GFX_CLUT8 */
} mpe_GfxSurfaceInfo;

typedef struct mpe_GfxEvent
{
    int32_t eventId; /* event type: e.g., OCAP_KEY_PRESSED, OCAP_KEY_RELEASED */
    int32_t eventCode; /* VK key code: e.g., VK_ENTER, VK_EXIT */
    int32_t eventChar; /* Unicode character corresponding to event; OCAP_CHAR_UNDEFINED if none; OCAP_CHAR_UNKNOWN if unknown. */
    int32_t rsvd[1]; /* reserved for future use */
} mpe_GfxEvent;

typedef struct mpe_GfxRectangle
{
    int32_t x;
    int32_t y;
    int32_t width;
    int32_t height;
} mpe_GfxRectangle;

typedef struct mpe_GfxFontMetrics
{
    int32_t height;
    int32_t ascent;
    int32_t descent;
    int32_t leading;
    int32_t maxadvance;
    int32_t maxascent;
    int32_t maxdescent;
    int32_t first_char;
    int32_t last_char;
} mpe_GfxFontMetrics;

typedef struct mpe_GfxFontDesc
{
    mpe_GfxWchar *name; /* font typeface name */
    uint32_t namelength; /* number of characters in the name */
    mpe_GfxFontFormat fnt_format; /* font format */
    mpe_GfxFontStyle style; /* font style  */
    int32_t minsize; /* minimum size */
    int32_t maxsize; /* maximum size */
    uint8_t *data; /* font file buffer */
    uint32_t datasize; /* buffer size */
    struct mpe_GfxFontDesc *prev; /* point to the previous font description in a font factory */
    struct mpe_GfxFontDesc *next; /* point to the next font description in a font factory */
} mpe_GfxFontDesc;

#endif
