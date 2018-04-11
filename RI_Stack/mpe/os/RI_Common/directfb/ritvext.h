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

#ifndef __RITVEXT_H__
#define __RITVEXT_H__

#include <mpe_types.h>

/**
 * Screen Data.
 *
 * This structure defines the structure of a screen.
 */
typedef struct
{
    /** Width in pixels. */
    unsigned int width;

    /** Height in pixels. */
    unsigned int height;

    /** Bytes-per-pixel. */
    unsigned int Bpp;

    /** bits-per-pixel. */
    unsigned int bpp;

    /** Length of scanline in bytes. */
    unsigned short pitch;

    /** Pointer to actual pixel data. */
    void *pixels;
} WFB_Screen;

//
// DirectFB APIs
//

/**
 * Resets or obtains the WFB_Screen sructure based on the passed
 * size and pixel information.
 *
 * @return  TRUE if successful, FALSE if not.
 *
 * @param   width       The width of the display.
 * @param   height      The height of the display.
 * @param   Bpp         The pixel byte count.
 * @param   bpp         The bits per pixel.
 * @param   ret_screen  The generated or retrieved screen structure
 *
 * @remarks If the passed width and height match the current display,
 *          the returned screen structure will be the current object.
 *
 * @warning  Note that the returned structure pointer is owned by the 
 *           called code.  This buffer must NOT be deleted by the caller.
 */
mpe_Bool WFB_SetDisplayMode(unsigned int width, unsigned int height,
        unsigned int Bpp, unsigned int bpp, WFB_Screen **ret_screen);

/**
 * Updates the screen display.
 *
 * @return  TRUE if successful, FALSE if the video mixer does
 *          not yet exist.
 *
 */
mpe_Bool WFB_Flip();

/**
 * Initializes the frame buffer.
 *
 * @return  TRUE if successful, FALSE if not.
 *
 * @remarks Currently has no implementation (returns TRUE).
 */
mpe_Bool WFB_Init();

/**
 * Uninitializes the frame buffer.
 *
 * @return  TRUE if successful, FALSE if not.
 *
 * @remarks Currently has no implementation (returns TRUE).
 */
mpe_Bool WFB_Term();

/**
 * Refreshes the screen display.
 *
 * @return  TRUE if successful, FALSE if not.
 *
 * @remarks A synonym for WFB_Flip().
 */
mpe_Bool WFB_Refresh();

#endif /* __RITVEXT_H__ */
