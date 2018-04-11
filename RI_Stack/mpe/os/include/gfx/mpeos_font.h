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

#if !defined(_MPEOS_FONT_H)
#define _MPEOS_FONT_H

#include <mpe_types.h>      /* Resolve basic type references. */
#include <mpe_error.h>
#include <mpeos_sync.h>
#include "mpeos_gfx.h"      /* graphics public definitions */

#ifdef __cplusplus
extern "C"
{
#endif

/**
 * <code>mpeos_GfxFont()</code> - Font internal representation.
 * <ul>
 * <li> osf			os specific font definition
 * <li> ff          factory it is created from
 * <li> *name       font family name
 * <li> namelength  number of wide char in the string
 * <li> size        font size
 * <li> style       font style
 * <li> refCount    count font usage
 * <li> mutex       guarding the refCount
 * <li> *prev       previous font created in a font factory
 * <li> *next       next font created in a font factory
 * </ul>
 */
typedef struct mpeos_GfxFont
{
    os_GfxFont osf;
    mpe_GfxFontFactory ff;
    mpe_GfxWchar *name;
    uint32_t namelength;
    uint32_t size;
    mpe_GfxFontStyle style;
    uint32_t refCount;
    os_Mutex mutex;
    struct mpeos_GfxFont *prev;
    struct mpeos_GfxFont *next;
} mpeos_GfxFont;

/***
 * Graphics - Font support API prototypes:
 */

/**
 * <i>mpeos_gfxFontNew()</i>
 * Creates a new Font from a given font factory and font attributes.
 * If the font factory is NULL, the font is created from the system font factory.
 * If a font cannot be created from the system font factory. The default system
 * font (tiresias, 26, PLAIN) is returned.
 * If a font cannot be created from a a non system font factory
 * MPE_GFX_ERROR_NOFONT error code is returned.
 * 
 * @param ff			Font Factory from which the font is created.
 * @param name			The typeface of the font (e.g. Arial, Times New Roman, etc...
 * @param namelength	Length in character of the font name
 * @param style			The style of the font (e.g. BOLD, PLAIN, ...).
 * @param size			The size in pixel of the font.
 * @param font			A handle to the new font.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxFontNew(mpe_GfxFontFactory ff, const mpe_GfxWchar *name,
        const uint32_t namelength, mpe_GfxFontStyle style, int32_t size,
        mpe_GfxFont *font);

/**
 * <i>mpeos_gfxFontDelete()</i>
 * Deletes a font.
 * The default system font (tiresias, 26, PLAIN) cannot be deleted
 * A font is physically deleted only when its refecerence count is zero 
 *
 * @param font   Handle to the font to be deleted.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxFontDelete(mpe_GfxFont font);

/**
 * <i>mpeos_gfxGetFontMetrics()</i>
 * Returns the font metrics for a given font. Unknown metrics are set to MPE_GFX_UNKNOWN
 * This function calls the following directfb functions:
 * <ul>
 * <li> GetAscender()
 * <li> GetDescender()
 * <li> GetHeight()
 * <li> GetMaxAdvance()
 * </ul>
 * @param font    Font to get the metrics from
 * @param metrics Pointer to the font metrics
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error
        mpeos_gfxGetFontMetrics(mpe_GfxFont font, mpe_GfxFontMetrics *metrics);

/**
 * <i>mpeos_gfxGetStringWidth()</i>
 * Returns the width of a string (in pixels).
 * This function calls the following directfb functions:
 * <ul>
 * <li> GetStringWidth()
 * </ul>
 * @param font   handle to a font.
 * @param str    the string.
 * @param len    number of characters in the string.
 * @param width  Address where the width is to be stored.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxGetStringWidth(mpe_GfxFont font, const char *str,
        int32_t len, int32_t *width);

/**
 * <i>mpeos_gfxGetString16Width()</i>
 * Returns the width of a UTF-16 formatted string (in pixels).
 *
 * @param font   handle to a font.
 * @param str    the string.
 * @param len    number of characters in the string.
 * @param width  Address where the width is to be stored.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxGetString16Width(mpe_GfxFont font, const mpe_GfxWchar *str,
        int32_t len, int32_t *width);

/**
 * <i>mpeos_gfxGetCharWidth()</i>
 * Returns the width of a character in a given font (in pixels).
 * This function calls the following directfb functions:
 * <ul>
 * <li> GetGlyphExtents()
 * </ul>
 * @param font   handle to a font.
 * @param ch     a wide char.
 * @param width  Address where the width is to be stored.
 *
 * @return a specific <code>mpe_GfxError</code> error code if failed, 
 *         or MPE_GFX_ERROR_NOERR otherwise.
 */
mpe_Error mpeos_gfxGetCharWidth(mpe_GfxFont font, mpe_GfxWchar ch,
        int32_t *width);

/**
 * <i>mpeos_gfxFontHasCode()</i>
 * Checks if a given code is in the font.
 * This function calls the following directfb functions:
 * <ul>
 * <li> GetGlyphExtents()
 * </ul>
 * @note  There is currently no directfb API to check if a code is in the font.
 *        We rely on the return error code of GetGlyphExtents to check if the glyph code
 *        is valid or not for teh given font
 *
 * @param font   handle to a font
 * @param code   the code be checked
 *
 * @return MPE_GFX_ERROR_NOERR if the code is part of the charset, 
 *         or MPE_GFX_ERROR_FALSE otherwise.
 */
mpe_Error mpeos_gfxFontHasCode(mpe_GfxFont font, mpe_GfxWchar code);

/**
 * Returns the head of the font list of the system font factory
 * 
 * @param desc          Address of the head of the list
 *						desc->next is the first element in the list
 * @return				MPE_GFX_ERROR_NOERR
 */
mpe_Error mpeos_gfxFontGetList(mpe_GfxFontDesc** desc);

#ifdef __cplusplus
}
#endif

#endif /* _MPEOS_FONT_H */

