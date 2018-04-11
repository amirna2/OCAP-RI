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
#include <stdlib.h>
#include <mpe_types.h>
#include <mpeos_dbg.h>
#include <mpeos_mem.h>
#include "os_gfx.h"
#include "mpeos_font.h"
#include "mpeos_fontfact.h"
#include "mpeos_screen.h"

#define FF_LOCK(ff)     mpeos_mutexAcquire(ff->mutex)
#define FF_UNLOCK(ff)   mpeos_mutexRelease(ff->mutex)

/******************************************************************************
 *  Imported functions
 *
 *****************************************************************************/
extern mpe_GfxFontFactory sys_fontfactory;
extern mpe_GfxFont sys_font;
extern mpe_Error
        wcstringtombs(char *mbstr, const mpe_GfxWchar *wcstr, size_t n);
extern mpe_Error gfxFactoryCreateFont(mpe_GfxFontFactory ff,
        const mpe_GfxWchar *name, uint32_t namelength, uint32_t size,
        mpe_GfxFontStyle style, mpe_GfxFontDesc *font_desc,
        mpeos_GfxFont **fontptr);

extern void gfxFactoryUpdateFontCount(mpe_GfxFontFactory ff, int inc);
extern void gfxFactoryRemoveFont(mpe_GfxFontFactory ff, mpeos_GfxFont *font);

/******************************************************************************
 *  Internal functions
 *
 *****************************************************************************/
int gfxFontUpdateCount(mpe_GfxFont font, int value);

/**
 * <i>gfxFontUpdateCount()</i>
 * Updates the reference count for the given font.
 * It is incremented every time it is returned from gfxFactoryCreateFont
 * (this includes initial creation and subsequent reuse) and
 * every time it is used by a context.
 * It is decremented every time is is deleted by gfxFontDelete and
 * every time is is un-referenced by a context.
 * <p>
 * One the value is decremented to 0 by gfxFontDelete, then the
 * font will be removed.
 *
 * @param font the font to update
 * @param value the change in reference value (generally +1 or -1)
 * @return the updated count value for reference
 */
int gfxFontUpdateCount(mpe_GfxFont font, int value)
{
    mpeos_GfxFont *fnt = (mpeos_GfxFont *) font;
    int count;
    if (!fnt)
        return -1;

    if (font == sys_font)
        return 1; /* Never delete, never update */

    mpeos_mutexAcquire(fnt->mutex);
    count = fnt->refCount += value;
    if (count < 0)
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "gfxFontUpdateCount() - WARNING - ref count < 0 (f=%p,%d)\n",
                font, count);
    mpeos_mutexRelease(fnt->mutex);

    return count;
}

/******************************************************************************
 * Public functions
 *
 *****************************************************************************/
mpe_Error mpeos_gfxFontNew(mpe_GfxFontFactory ff, const mpe_GfxWchar *name,
        const uint32_t namelength, mpe_GfxFontStyle style, int32_t size,
        mpe_GfxFont *font)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpeos_GfxFont *fnt = NULL; /* the new font to create */

    mpe_GfxFontDesc font_desc; /* matching font description in the font factory */

    mpe_Error st; /* Status returned from gfxFactoryCreateFont */

    if (name == NULL || font == NULL)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "mpeos_gfxFontNew() - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }

    /* returning default system font when font factory is not specified */
    if (ff == NULL)
    {
        ff = sys_fontfactory;
    }

    if (size < 0)
    {
        size = 0; /* There are actaully tests that supply negative
         font sizes... let's just call them 0 */
    }

    // this call will increment the font ref counter on returned font
    st = gfxFactoryCreateFont(ff, name, namelength, size, style, &font_desc,
            &fnt);

    if (!fnt)
    {
        /* Finally fall back on default system font. */
        if (ff == sys_fontfactory)
        {
            //fnt = &_sysfont;
            fnt = (mpeos_GfxFont*) sys_font;

            // increment font's ref counter
            gfxFontUpdateCount((mpe_GfxFont) fnt, 1);
        }
        else
        {
            *font = NULL;
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                    "mpeos_gfxFontNew() - ERROR - factory cannot create font\n");
            if (st != MPE_GFX_ERROR_NOERR)
            {
                return st;
            }
            else
            {
                return MPE_GFX_ERROR_NOFONT;
            }
        }
    }

    *font = (mpe_GfxFont) fnt;
    return err;
}

mpe_Error mpeos_gfxFontDelete(mpe_GfxFont font)
{

    mpeos_GfxFont *fnt = (mpeos_GfxFont *) font;
    mpeos_GfxFontFactory *ffact;
    int count;

    if (!fnt)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "mpeos_gfxFontDelete() - ERROR - invalid font handle\n");
        return MPE_GFX_ERROR_INVALID;
    }

    if (font == sys_font)
    {
        /* nothing to do ! teh system font cannot be deleted */
        return MPE_GFX_ERROR_NOERR;
    }

    ffact = (mpeos_GfxFontFactory*) fnt->ff;
    /* Must lock FontFactory FIRST, so no one else can get a reference to the font */
    FF_LOCK(ffact);

    if (0 == (count = gfxFontUpdateCount(font, -1)))
    {
        gfxFactoryRemoveFont(fnt->ff, fnt);
    }
    else
    {
        // Note that native font may have been returned multiple times for same/different requests
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_GFX,
                "mpeos_gfxFontDelete() - still referenced (%d).\n", count);
        FF_UNLOCK(ffact);
        return MPE_GFX_ERROR_INVALID;
    }

    FF_UNLOCK(ffact);

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxGetFontMetrics(mpe_GfxFont font, mpe_GfxFontMetrics* metrics)
{
    mpeos_GfxFont* f = (mpeos_GfxFont*) font;
    int value = 0;

    if (!metrics || !font)
        return MPE_GFX_ERROR_INVALID;

    f->osf.dfb_fnt->GetAscender(f->osf.dfb_fnt, &value);
    metrics->ascent = value;

    f->osf.dfb_fnt->GetDescender(f->osf.dfb_fnt, &value);
    metrics->descent = value;

    f->osf.dfb_fnt->GetHeight(f->osf.dfb_fnt, &value);
    metrics->height = value;

    f->osf.dfb_fnt->GetMaxAdvance(f->osf.dfb_fnt, &value);
    metrics->maxadvance = value;

    /* Make sure we have absolute value of descender */
    if (metrics->descent < 0)
        metrics->descent = -metrics->descent;

    metrics->first_char = MPE_GFX_UNKNOWN;
    metrics->last_char = MPE_GFX_UNKNOWN;
    metrics->leading = metrics->height - metrics->descent - metrics->ascent;
    metrics->maxascent = metrics->ascent;
    metrics->maxdescent = metrics->descent;

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxGetStringWidth(mpe_GfxFont font, const char* str,
        int32_t len, int32_t* width)
{
    mpeos_GfxFont *fnt = (mpeos_GfxFont *) font;
    int value = 0;

    if (!font || !str || !width)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    if (fnt->osf.dfb_fnt->GetStringWidth(fnt->osf.dfb_fnt, str, len, &value)
            != DFB_OK)
        return MPE_GFX_ERROR_OSERR;

    *width = value;

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxGetString16Width(mpe_GfxFont font, const mpe_GfxWchar* str,
        int32_t len, int32_t* width)
{

    char *string8;
    int strsize;

    mpe_Error err;

    if (!font || !str || !width)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    /* allocate a temporary UTF8 buffer : we might need upto 4 bytes per character */

    /* !FIXME:
     * wcstringtombs() should return the required number of bytes for the utf16 buffer
     * when the NULL is passed as the utf16 buffer, instead of allocating the maximum possible
     * size everytime. This should also remove the calls to memset and strlen.
     */

    strsize = (len << 2) + 1;

    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_GFX, strsize, (void**) &string8))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "mpeos_gfxFontNew() - ERROR - Failed to allocate string8\n");
        return MPE_GFX_ERROR_NOMEM;
    }

    memset(string8, 0, strsize);

    if (wcstringtombs(string8, str, len) != MPE_GFX_ERROR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "mpeos_gfxFontNew() - ERROR - Failed to convert string to multi-bytes\n");
        mpeos_memFreeP(MPE_MEM_GFX, string8);
        return MPE_GFX_ERROR_INVALID;
    }
    err = mpeos_gfxGetStringWidth(font, string8, strlen(string8), width);
    mpeos_memFreeP(MPE_MEM_GFX, string8);

    return (err);
}

mpe_Error mpeos_gfxGetCharWidth(mpe_GfxFont font, mpe_GfxWchar ch,
        int32_t* width)
{

    DFBRectangle rect;
    int adv;
    mpeos_GfxFont *fnt = (mpeos_GfxFont *) font;

    if (!font || !width)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    if (fnt->osf.dfb_fnt->GetGlyphExtents(fnt->osf.dfb_fnt, ch, &rect, &adv)
            != DFB_OK)
        return MPE_GFX_ERROR_OSERR;

    *width = adv;

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxFontHasCode(mpe_GfxFont font, mpe_GfxWchar code)
{
    mpeos_GfxFont *fnt = (mpeos_GfxFont *) font;
    int adv;

    if (!font)
    {
        return MPE_GFX_ERROR_INVALID;
    }

    if (fnt->osf.dfb_fnt->GetGlyphExtents(fnt->osf.dfb_fnt, code, NULL, &adv)
            != DFB_OK)
    {
        return MPE_GFX_ERROR_FALSE;
    }

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxFontGetList(mpe_GfxFontDesc** desc)
{
    mpeos_GfxFontFactory *ff = (mpeos_GfxFontFactory*) sys_fontfactory;

    *desc = MPE_FAKEHEAD(mpe_GfxFontDesc, ff->descList[0], prev);

    return MPE_GFX_ERROR_NOERR;
}
