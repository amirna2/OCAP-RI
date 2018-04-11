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

#if !defined(_MPEOS_FONTFACTORY_H)
#define _MPEOS_FONTFACTORY_H

#include <mpe_types.h>      /* Resolve basic type references. */
#include <mpe_error.h>
#include <mpeos_sync.h>
#include <mpeos_font.h>
#include "mpeos_gfx.h"      /* graphics public definitions */

#ifdef __cplusplus
extern "C"
{
#endif

/**
 * <code>mpeos_GfxFontFactory()</code> - Font factory internal representation.
 * <ul>
 * <li> mutex         guarding the lists
 * <li> tbd           true if to be deleted
 * <li> fontCount     count of active fonts in the factory
 * <li> *descList[2]  list of available font descriptors
 * <li> *fontList[2]  list of created fonts
 * </ul>
 */
typedef struct mpeos_GfxFontFactory
{
    os_Mutex mutex;
    mpe_Bool tbd;
    uint32_t fontCount;
    mpe_GfxFontDesc *descList[2];
    mpeos_GfxFont *fontList[2];
} mpeos_GfxFontFactory;

/***
 * Graphics - Font Factory support API prototypes:
 */

/**
 * <i>mpeos_gfxFontFactoryNew()</i>
 * Creates and initializes a new font factory.
 *
 * @param ff    handle to the created font factory
 *              NULL if failed to create.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxFontFactoryNew(mpe_GfxFontFactory *ff);

/**
 * <i>mpeos_gfxFontFactoryDelete()</i>
 * Deletes an existing font factory if the global font count is zero
 * (i.e. there is no more font produced from the factory).
 * If the count is non-zero then we flag the factory as <i>to-be-deleted</i>
 * and it will be deleted after the last produced font has been deleted 
 * (see mpeos_gfxFontDelete in mpeos_font.cpp)
 * 
 * @param ff    handle of the font to delete
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxFontFactoryDelete(mpe_GfxFontFactory ff);

/**
 * <i>mpeos_gfxFontFactoryAdd()</i>
 * Add a new font to the font factory.
 * Two formats for the description are accepted.
 * <ul>
 * <li> With data and datasize only (keys off of NULL name)
 * <li> With all information included.
 * </ul>
 *
 * @param ff    the font factory to add the font to.
 * @param desc  the description of the font to add.         
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise
 */
mpe_Error mpeos_gfxFontFactoryAdd(mpe_GfxFontFactory ff, mpe_GfxFontDesc* desc);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_FONT_H */

