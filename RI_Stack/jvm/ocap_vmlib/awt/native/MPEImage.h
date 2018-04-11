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

#ifndef MPE_IMAGE_H /* { MPE_IMAGE_H */
# define MPE_IMAGE_H

/**
 * Defines the image type used for offscreen images.
 * It also implicitly defines the ColorModel and image type
 * returned for the MPEDefaultGraphicsConfiguration.
 */
#if 1
#define MPE_AWTIMAGE MPE_AWTIMAGE_ARGB32
#else
#define MPE_AWTIMAGE MPE_AWTIMAGE_CLUT8
#endif

#ifndef MPE_AWTIMAGE
/*
 * NOTE: We should switch to using the img_scaleloop.h 
 * setup if we want to support more than one type of image
 * or support a dynamic type.
 */
# error To be dynamic, we really should use img_scaleloop.h
#endif

#define MPE_AWTIMAGE_ARGB32  0
#define MPE_AWTIMAGE_RGB565  1
#define MPE_AWTIMAGE_BGRA888 2
#define MPE_AWTIMAGE_CLUT8   3

#if 0 /* templates */
/**
 * Sets the pixel at the given (x,y) location to the given ARGB32 color.
 * @param info the mpe_GfxSurfaceInfo for the surface to write to
 * @param x x-coordinate of destination pixel
 * @param y y-coordinate of destination pixel
 * @param argb source color
 */
# define SET_PIXEL(info, x, y, argb)

/**
 * Returns the pixel at the given (x,y) location in the variable argb.
 * @param info the mpe_GfxSurfaceInfo for the surface to read from
 * @param x x-coordinate of source pixel
 * @param y y-coordinate of source pixel
 * @param argb destination variable for color
 */
# define GET_PIXEL(info, x, y, argb)

/**
 * Sets the pixel at the given (x,y) location to the given a/r/g/b color.
 * @param info the mpe_GfxSurfaceInfo for the surface to write to
 * @param x x-coordinate of destination pixel
 * @param y y-coordinate of destination pixel
 * @param a alpha of source color
 * @param r red of source color
 * @param g green of source color
 * @param b blue of source color
 */
# define SET_PIXEL_ARGB(info, x, y, a, r, g, b)

/**
 * If set, MPE_AWTIMAGE_TYPE specifies the image format to be used.
 * If not set, then... perhaps the screen format should be assumed?
 */
# define MPE_AWTIMAGE_TYPE ???
#endif

#if MPE_AWTIMAGE==MPE_AWTIMAGE_ARGB32
/******** Generic ARGB32 format w/ top-down/left-right lines ********/
# define MPE_AWTIMAGE_TYPE MPE_GFX_ARGB8888
# define MPE_AWTIMAGE_BPP  32
# define PIXEL_LOC(info, x, y, p)                                                       \
do {                                                                                    \
    p = (uint32_t*)((int)(info).pixeldata + ((x)*(info).bpp)/8 + (y)*(info).widthbytes);  \
} while(0)
/* !!!!!Everything else is IDENTICAL to BGRA8888!!!!! */

# define PIXEL_FORMAT(argb)            (argb)

# define FORMAT_PIXEL(pix)             (pix)

# define PIXEL_FORMAT_ARGB(a, r, g, b) (((a) << 24) | ((r) << 16) | ((g) << 8) | (b))

# define SET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint32_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        *p = PIXEL_FORMAT(argb);                \
    } while(0)

# define GET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint32_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        argb = FORMAT_PIXEL( *p );              \
    } while(0)

# define SET_PIXEL_ARGB(info, x, y, a, r, g, b) \
    do {                                        \
        uint32_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        *p = PIXEL_FORMAT_ARGB(a, r, g, b) ;    \
    } while(0)

#elif MPE_AWTIMAGE==MPE_AWTIMAGE_RGB565
/******* Generic RGB565 format w/ top-down/left-right lines ******/
# define MPE_AWTIMAGE_TYPE MPE_GFX_RGB565
# define MPE_AWTIMAGE_BPP  16
# define PIXEL_LOC(info, x, y, p)                                                       \
do {                                                                                    \
    p = (uint16_t*)((int)(info).pixeldata + ((x)*(info).bpp)/8 + (y)*(info).widthbytes);  \
} while(0)

# define RGB565_RED(rgb)    (((rgb) >> 11) & 0x1F)
# define RGB565_GREEN(rgb)  (((rgb) >> 5) & 0x3F)
# define RGB565_BLUE(rgb)   ((rgb) & 0x1F)
# define RGB565_TO_ARGB32(rgb)  ((RGB565_RED(rgb)<<19) | (RGB565_GREEN(rgb)<<10) | (RGB565_BLUE(rgb)<<3) | 0xFF000000)

# define PIXEL_FORMAT(argb, p)                       \
    do {                                             \
        int r = (argb >> 19) & 0x1F;                 \
        int g = (argb >> 10) & 0x3F;                 \
        int b = (argb >> 3) & 0x1F;                  \
        *(p) = (uint16_t)((r << 10) | (g << 5) | b);   \
    } while(0)

# define FORMAT_PIXEL(pix) RGB565_TO_ARGB32(pix)

# define PIXEL_FORMAT_ARGB(a, r, g, b) ((uint16_t)((((r)>>3) << 10) | (((g)>>2) << 5) | ((b)>>3)))

# define SET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint16_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        PIXEL_FORMAT(argb, p);                  \
    } while(0)

# define GET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint16_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        argb = FORMAT_PIXEL( *p );              \
    } while(0)

# define SET_PIXEL_ARGB(info, x, y, a, r, g, b) \
    do {                                        \
        uint16_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        *p = PIXEL_FORMAT_ARGB(a, r, g, b) ;    \
    } while(0)

#elif MPE_AWTIMAGE==MPE_AWTIMAGE_CLUT8
/******* Generic CLUT-8 format w/ top-down/left-right lines ******/
# define MPE_AWTIMAGE_TYPE MPE_GFX_CLUT8
# define MPE_AWTIMAGE_BPP  8

# define SET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint8_t *p;                               \
        PIXEL_LOC(info, x, y, p);               \
        PIXEL_FORMAT(info, argb, p);            \
    } while(0)

# define GET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint8_t *p;                               \
        PIXEL_LOC(info, x, y, p);               \
        FORMAT_PIXEL(info, p, argb);            \
    } while(0)

# define SET_PIXEL_ARGB(info, x, y, a, r, g, b)                 \
    do {                                                        \
        uint8_t *p;                                               \
        PIXEL_LOC(info, x, y, p);                               \
        PIXEL_FORMAT(info, PIXEL_FORMAT_ARGB(a, r, g, b), p);   \
    } while(0)

# define PIXEL_LOC(info, x, y, p)                                                       \
do {                                                                                    \
    p = (uint8_t*)((int)(info).pixeldata + ((x)*(info).bpp)/8 + (y)*(info).widthbytes);   \
} while(0)

# define PIXEL_FORMAT(info, argb, p)                            \
    do {                                                        \
        static int last_index = -1;                             \
        static mpe_GfxColor last_argb = 0;                    \
        int index = 0;                                          \
        if (last_index != -1 && argb == last_argb)              \
            *(p) = last_index;                                  \
        else                                                    \
        {                                                       \
            last_argb = argb;                                   \
            mpe_gfxPaletteMatch(info.clut, argb, &index);       \
            *(p) = index;                                       \
            last_index = index;                                 \
        }                                                       \
    } while(0)

# define FORMAT_PIXEL(info, index, argb)                        \
    do {                                                        \
        static int last_index = -1;                             \
        static mpe_GfxColor last_argb = 0;                    \
        if (last_index != -1 && index == last_index)            \
            argb = last_argb;                                   \
        else                                                    \
        {                                                       \
            last_index = index;                                 \
            mpe_gfxPaletteGet(info.clut, 1, index, &argb);      \
            last_argb = argb;                                   \
        }                                                       \
    } while(0)

# define PIXEL_FORMAT_ARGB(a, r, g, b) (((a) << 24) | ((r) << 16) | ((g) << 8) | (b))

extern mpe_GfxPalette gOcapClut;

#elif MPE_AWTIMAGE==MPE_AWTIMAGE_BGRA8888
# define MPE_AWTIMAGE_TYPE MPE_GFX_BGRA8888
# define MPE_AWTIMAGE_BPP  32
/******* Windows BGRA8888 format w/ bottom-up/left-right lines ********/
# define PIXEL_LOC(info, x, y, p)                                                       \
do {                                                                                    \
    int myY = (info).dim.height - (y) - 1;                                              \
    p = (uint32_t*)((int)(info).pixeldata + ((x)*(info).bpp)/8 + myY*(info).widthbytes);  \
} while(0)

# define PIXEL_FORMAT(argb)            (argb)

# define FORMAT_PIXEL(pix)             (pix)

# define PIXEL_FORMAT_ARGB(a, r, g, b) (((a) << 24) | ((r) << 16) | ((g) << 8) | (b))

# define SET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint32_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        *p = PIXEL_FORMAT(argb);                \
    } while(0)

# define GET_PIXEL(info, x, y, argb)            \
    do {                                        \
        uint32_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        argb = FORMAT_PIXEL( *p );              \
    } while(0)

# define SET_PIXEL_ARGB(info, x, y, a, r, g, b) \
    do {                                        \
        uint32_t *p;                              \
        PIXEL_LOC(info, x, y, p);               \
        *p = PIXEL_FORMAT_ARGB(a, r, g, b) ;    \
    } while(0)

#else
# error undefined MPE_AWTIMAGE_XXXX
#endif

#endif /* } MPE_IMAGE_H */
