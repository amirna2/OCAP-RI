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
#include <ctype.h>
#include <string.h>

#include <mpe_types.h>
#include <mpeos_dbg.h>
#include <mpeos_mem.h>
#include "os_gfx.h"
#include "mpeos_font.h"
#include "mpeos_fontfact.h"
#include "mpeos_screen.h"
#include "mpeos_util.h"
#include "mpe_file.h"
#include "ConvertUTF.h"

/******************************************************************************
 *  Private definitions
 *
 *****************************************************************************/

mpe_GfxFontFactory sys_fontfactory;
mpe_GfxFont sys_font;

char FONT_PATH[MPE_FS_MAX_PATH + 1]; /* to store the system font path */

#define FF_LOCK(ff)     mpeos_mutexAcquire(ff->mutex)
#define FF_UNLOCK(ff)   mpeos_mutexRelease(ff->mutex)

/******************************************************************************
 *  Wide character string functions
 *
 *****************************************************************************/
size_t wcstringlen(const mpe_GfxWchar *ws);
mpe_GfxWchar* wcstringcpy(mpe_GfxWchar *wd, const mpe_GfxWchar *ws);
mpe_GfxWchar * wcstringncpy(mpe_GfxWchar *wd, const mpe_GfxWchar *ws, size_t n);
int wcstringnicmp(const mpe_GfxWchar *ws1, const mpe_GfxWchar *ws2, size_t n1,
        size_t n2);
int wcstringicmp(const mpe_GfxWchar *ws1, const mpe_GfxWchar *ws2);
mpe_Error wcstringtombs(char *mbstr, const mpe_GfxWchar *wcstr, size_t n);
mpe_Error mbstowcstring(mpe_GfxWchar *wcstr, const char *mbstr, size_t n);

/******************************************************************************
 *  Imported functions
 *
 *****************************************************************************/
extern int gfxFontUpdateCount(mpe_GfxFont font, int value);

/******************************************************************************
 *  Exported functions
 *
 *****************************************************************************/
void gfxFactoryUpdateFontCount(mpe_GfxFontFactory ff, int inc);
mpe_Error gfxFactoryCreateFont(mpe_GfxFontFactory ff, const mpe_GfxWchar *name,
        uint32_t namelength, uint32_t size, mpe_GfxFontStyle style,
        mpe_GfxFontDesc *font_desc, mpeos_GfxFont **fontptr);
void gfxFactoryRemoveFont(mpe_GfxFontFactory ff, mpeos_GfxFont *font);
mpe_Error gfxFactoryCreateDefault(void);

/******************************************************************************
 *  Private functions
 *
 *****************************************************************************/
static mpe_Error descListAllocDesc(mpe_GfxFontDesc **fd);
static void descListPrepend(mpeos_GfxFontFactory *ff, mpe_GfxFontDesc *desc);
static mpe_Bool descListSearch(mpe_GfxFontDesc **list, mpe_GfxFontDesc *desc);
static void descListRemove(mpe_GfxFontDesc *desc);

static mpe_Error fontListAllocFont(mpeos_GfxFont **font);
static void fontListPrepend(mpeos_GfxFontFactory *ff, mpeos_GfxFont *font);

static void fontListRemove(mpeos_GfxFont *font);
static mpeos_GfxFont* fontListFind(mpeos_GfxFont *fonthead,
        const mpe_GfxWchar *name, uint32_t namelength, uint32_t size,
        mpe_GfxFontStyle style);

/******************************************************************************
 *  Public functions
 *
 *****************************************************************************/

mpe_Error mpeos_gfxFontFactoryNew(mpe_GfxFontFactory *ff)
{
    mpe_Error err;
    mpeos_GfxFontFactory *ffact = NULL;

    /* check parameters */
    if (!ff)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxFontFactoryNew() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }

    /* allocate a new font factory */
    if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX, sizeof *ffact,
            (void**) &ffact)))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxFontFactoryNew() - ERROR - memAllocP() failed\n");
        return err;
    }

    ffact->tbd = false;
    ffact->descList[0] = NULL;
    ffact->descList[1] = NULL;

    ffact->fontList[0] = NULL;
    ffact->fontList[1] = NULL;

    ffact->mutex = NULL;
    ffact->fontCount = 0;

    /* allocate a mutex for thread safe font list */
    if (mpeos_mutexNew(&ffact->mutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxFontFactoryNew() - ERROR - Failed to create Mutex\n");
        mpeos_memFreeP(MPE_MEM_GFX, (void*) ffact);
        return MPE_GFX_ERROR_UNKNOWN;
    }

    /* Check for need to initialize descriptor list. */
    if (ffact->descList[0] == NULL)
    {
        mpe_GfxFontDesc *head = MPE_FAKEHEAD(mpe_GfxFontDesc,
                ffact->descList[0], prev);
        head->prev = head->next = head;
    }

    /* Check for need to initialize font list. */
    if (ffact->fontList[0] == NULL)
    {
        mpeos_GfxFont *head = MPE_FAKEHEAD(mpeos_GfxFont, ffact->fontList[0],
                prev);
        head->prev = head->next = head;
    }

    *ff = (mpe_GfxFontFactory) ffact;

    return MPE_GFX_ERROR_NOERR;
}

mpe_Error mpeos_gfxFontFactoryDelete(mpe_GfxFontFactory ff)
{

    mpeos_GfxFontFactory *ffact = (mpeos_GfxFontFactory *) ff;
    mpe_GfxFontDesc *head;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_GFX,
            "<<GFX>> mpeos_gfxFontFactoryDelete() - deleting font factory %x\n", ffact);

    if (!ff)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxFontFactoryDelete() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }

    head = MPE_FAKEHEAD(mpe_GfxFontDesc, ffact->descList[0], prev);

    if (ffact->fontCount == 0)
    {
        mpe_GfxFontDesc *elt;

        elt = head->next;
        while (elt != head)
        {
            mpeos_memFreeP(MPE_MEM_GFX, elt->name);
            mpeos_memFreeP(MPE_MEM_GFX, elt->data);
            descListRemove(elt);
            mpeos_memFreeP(MPE_MEM_GFX, elt);
            elt = head->next;
        }

        mpeos_mutexDelete(ffact->mutex);
        mpeos_memFreeP(MPE_MEM_GFX, ffact);

    }
    else
    {
        ffact->tbd = true;
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxFontFactoryDelete() - WARNING deleting a factory that has open fonts\n");
    }

    return MPE_GFX_ERROR_NOERR;

}

static mpe_GfxFontDesc* dupFontDesc(mpe_GfxFontDesc* desc)
{
    mpe_GfxFontDesc *copy = NULL;

    if (mpeos_memAllocP(MPE_MEM_GFX, sizeof(*copy), (void**) &copy)
            == MPE_SUCCESS)
    {
        copy->datasize = desc->datasize;
        copy->data = NULL;
        if (desc->datasize && desc->data)
        {
            if (mpeos_memAllocP(MPE_MEM_GFX, desc->datasize,
                    (void**) &copy->data) != MPE_SUCCESS)
            {
                mpeos_memFreeP(MPE_MEM_GFX, copy);
                return NULL;
            }
            memcpy(copy->data, desc->data, desc->datasize);
        }
        copy->name = NULL;
        if (desc->name)
        {
            if (mpeos_memAllocP(MPE_MEM_GFX, (desc->namelength + 1)
                    * sizeof(mpe_GfxWchar), (void**) &copy->name)
                    != MPE_SUCCESS)
            {
                mpeos_memFreeP(MPE_MEM_GFX, copy->data);
                mpeos_memFreeP(MPE_MEM_GFX, copy);
                return NULL;
            }
            copy->name[(desc->namelength)] = 0;
            memcpy(copy->name, desc->name, desc->namelength
                    * sizeof(mpe_GfxWchar));
        }
        copy->fnt_format = desc->fnt_format;
        copy->minsize = desc->minsize;
        copy->maxsize = desc->maxsize;
        copy->style = desc->style;

        copy->namelength = desc->namelength;
        copy->next = desc->next;
        copy->prev = desc->prev;

    }

    return copy;
}

mpe_Error mpeos_gfxFontFactoryAdd(mpe_GfxFontFactory ff, mpe_GfxFontDesc* desc)
{

    mpe_Error err;
    mpe_GfxFontDesc *a_font; /* font descriptor to add */
    DFBResult res;
    char* family;
    DFBFontStyle style;
    mpeos_GfxFontFactory *ffact = (mpeos_GfxFontFactory*) ff;

    mpe_Bool found = false;

    /* check parameters */
    if ((ffact == NULL) || (desc == NULL) || ((ff != sys_fontfactory)
            && ((desc->data == NULL) || (desc->datasize == 0))))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxFontFactoryAdd() - ERROR - Invalid Parameter(s)\n");
        return MPE_GFX_ERROR_INVALID;
    }

    /*
     * Support two version:
     * 1) data only (key off of lack of name)
     * 2) full description
     */
    if (desc->name == NULL)
    {
        /* Create the DFB font, determine the name/style, and release it. */
        IDirectFB* thiz = mpeos_gfxGetScreen()->osScr.dfb;
        DFBFontDescription dfb_fd;
        IDirectFBFont* dfb_font;
        dfb_fd.flags = DFDESC_HEIGHT;
        dfb_fd.height = 26; // arbitrary size

        if ((res = thiz->CreateFontFromBuffer(thiz, (char*) desc->data,
                desc->datasize, &dfb_fd, &dfb_font)) != DFB_OK)
        {
            MPEOS_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_GFX,
                    "<<GFX>> mpeos_gfxFontFactoryAdd() - ERROR - CreateFontFromBuffer Failed data=0x%p, datasize=%d err = %d\n",
                    desc->data, desc->datasize, res);
            return MPE_GFX_ERROR_FONTFORMAT;
        }

        /* Get the font name/style information. */
        dfb_font->GetFamilyName(dfb_font, &family);
        dfb_font->GetStyleFlags(dfb_font, &style);

        /* !!! At this point we could search through list... */
        /* If it made sense... */

        // Allocate a new font descriptor
        if (MPE_SUCCESS != (err = descListAllocDesc(&a_font)))
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                    "<<GFX>> mpeos_gfxFontFactoryAdd() - ERROR - memAllocP() failed\n");
        else
        {
            // Dup font data
            if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX,
                    desc->datasize, (void**) &a_font->data)))
            {
                mpeos_memFreeP(MPE_MEM_GFX, a_font);
                MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                        "<<GFX>> mpeos_gfxFontFactoryAdd() - ERROR - memAllocP() failed\n");
            }
            else
            {
                // Dup the data
                memcpy(a_font->data, desc->data, desc->datasize);
                a_font->datasize = desc->datasize;
                a_font->fnt_format = desc->fnt_format;
                a_font->minsize = desc->minsize;
                a_font->maxsize = desc->maxsize;
                a_font->next = desc->next;
                a_font->prev = desc->prev;

                // Translate style
                switch ((int) style)
                /* cast to 'int' as these enums are really bit-flags */
                {
                case DFFS_BOLD:
                    a_font->style = MPE_GFX_BOLD;
                    break;
                case DFFS_ITALIC:
                    a_font->style = MPE_GFX_ITALIC;
                    break;
                case (DFBFontStyle)(DFFS_BOLD | DFFS_ITALIC):
                    a_font->style = MPE_GFX_BOLD_ITALIC;
                    break;
                default: /* pick NORMAL by default */
                case DFFS_NORMAL:
                    a_font->style = MPE_GFX_PLAIN;
                    break;
                }

                // Convert name to mpe_GfxWchar*
                a_font->namelength = strlen(family);
                if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX,
                        (a_font->namelength + 1) * sizeof(mpe_GfxWchar),
                        (void**) &a_font->name)))
                {
                    mpeos_memFreeP(MPE_MEM_GFX, a_font->data);
                    mpeos_memFreeP(MPE_MEM_GFX, a_font);
                    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                            "<<GFX>> mpeos_gfxFontFactoryAdd() - ERROR - memAllocP() failed\n");
                }
                else
                {
                    a_font->name[a_font->namelength] = '\0';
                    mbstowcstring(a_font->name, family, a_font->namelength + 1);
                    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                            "<<GFX>> mpeos_gfxFontFactoryAdd() family=%s\n",
                            family);
                }
            }

            /* Release font. */
            dfb_font->Release(dfb_font);

            /* Add descriptor to list */
            FF_LOCK(ffact);
            descListPrepend(ffact, a_font);
            FF_UNLOCK(ffact);
        }

        return err;
    }
    else
    {

        /* look for a matching font in the list */
        FF_LOCK(ffact);

        found = descListSearch(ffact->descList, desc);

        if (found == true)
        {
            FF_UNLOCK(ffact);
            MPEOS_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_GFX,
                    "<<GFX>> mpeos_gfxFontFactoryAdd() - WARNING - font descriptor already exists\n");
            return MPE_GFX_ERROR_UNKNOWN;
        }

        /* Copy the font descriptor data. */
        if ((a_font = dupFontDesc(desc)) == NULL)
        {
            FF_UNLOCK(ffact);
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                    "<<GFX>> mpeos_gfxFontFactoryAdd() - ERROR - memAllocP() failed\n");
            return MPE_GFX_ERROR_UNKNOWN;
        }

        /* the new font descriptor is added to the font list */
        descListPrepend(ffact, a_font);

        FF_UNLOCK(ffact);
    }

    return MPE_GFX_ERROR_NOERR;
}

/**
 * Finds the desired font within the given font factory and creates if if necessary.
 * It follows these steps:
 * <ol>
 * <li> Search the cache of previously created fonts for an exact name/size/style match.
 *      If found, return it.  Incrementing the reference count.
 * <li> Search the list of font descriptions for an exact match match.
 * <li> If an exact match isn't found and this is the system factory:
 *      <ol>
 *      <li> Search for a name/size match (ignore style)
 *      <li> Search for a name/closest size match
 *      <li> Finally, fall back on the default system font the closest size
 *      </ol>
 * <li> Create a new font instance, create the DirectFB font, and update the reference count.
 * </ol>
 */
mpe_Error gfxFactoryCreateFont(mpe_GfxFontFactory ff, const mpe_GfxWchar *name,
        uint32_t namelength, uint32_t size, mpe_GfxFontStyle style,
        mpe_GfxFontDesc *font_desc, mpeos_GfxFont **fontptr)
{
    (void) font_desc;
    mpeos_GfxFontFactory *ffact = (mpeos_GfxFontFactory*) ff;
    mpeos_GfxFont *font = NULL; /* font to return */
    mpe_GfxFontDesc *elt = NULL; /* element in the description list */
    mpe_Bool found = false;

    IDirectFB* thiz = mpeos_gfxGetScreen()->osScr.dfb;
    DFBFontDescription dfb_fd;

    mpe_GfxFontDesc *head = MPE_FAKEHEAD(mpe_GfxFontDesc, ffact->descList[0],
            prev);
    mpeos_GfxFont *fonthead = MPE_FAKEHEAD(mpeos_GfxFont, ffact->fontList[0],
            prev);

    /* checking in the font list if we already have created the font
     update the font count and return the font if we have found one */
    FF_LOCK(ffact);
    font = fontListFind(fonthead, name, namelength, size, style);
    if (font != NULL)
    {
        FF_UNLOCK(ffact);
        *fontptr = font; // Return successfully
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_GFX,
            "<<GFX>> gfxFactoryCreateFont() - returning font %x\n", font);

        return MPE_GFX_ERROR_NOERR;
    }

    /* checking for an exact match */

    for (elt = head->next; elt != head; elt = elt->next)
    {
        /* try to find the exact match */
        if (!wcstringnicmp(elt->name, name, elt->namelength, namelength)
                && size >= (uint32_t) elt->minsize && size
                <= (uint32_t) elt->maxsize && style == elt->style)
        {
            /* stop searching, found points to entry */
            found = true;
            break;
        }
    }

    /*
     * If no match was found (in the system font factory), then search for
     * a "fuzzy" font match.
     * - First try without the style.
     * - Then try best possible size.
     * - Then simply fall back to the system default w/ best possible size.
     */
    if (!found && ff == sys_fontfactory)
    {
        /* Ignore the style. */
        for (elt = head->next; elt != head; elt = elt->next)
        {
            if (namelength == elt->namelength && !wcstringnicmp(elt->name,
                    name, elt->namelength, namelength) && size
                    >= (uint32_t) elt->minsize && size
                    <= (uint32_t) elt->maxsize)
            {
                style = elt->style;
                found = true;
                break;
            }
        }
        /* Trying closest possible size. */
        if (!found)
        {
            for (elt = head->next; elt != head; elt = elt->next)
            {
                if (namelength == elt->namelength && !wcstringnicmp(elt->name,
                        name, elt->namelength, namelength))
                {
                    style = elt->style;
                    if (size < (uint32_t) elt->minsize)
                        size = elt->minsize;
                    else if (size > (uint32_t) elt->maxsize)
                        size = elt->maxsize;
                    found = true;
                    break;
                }
            }
        }
        /* at last...trying default font with the closest possible size */
        if (!found)
        {

            elt = head->next;

            style = elt->style;
            if (size < (uint32_t) elt->minsize)
                size = elt->minsize;
            else if (size > (uint32_t) elt->maxsize)
                size = elt->maxsize;

            // Use system font name
            name = elt->name;

            found = true;
        }
    }

    if (found)
    {
        namelength = elt->namelength;
        name = elt->name;

        /* If found a (real or "fuzzy") match, see if font has already been created first */
        font = fontListFind(fonthead, name, namelength, size, style);
        if (font != NULL)
        {
            // since we are returning a copy of the native font ptr, increment ref count

            FF_UNLOCK(ffact);
            *fontptr = font;

            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_GFX,
                "<<GFX>> gfxFactoryCreateFont() - returning font %x\n", font);

            return MPE_GFX_ERROR_NOERR;
        }
    }
    /* this should never happen if we are looking in the system font factory */
    if (!found)
    {
        FF_UNLOCK(ffact);
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_GFX,
                "<<GFX>> gfxFactoryCreateFont() - ERROR - No matching description for that font\n");
        *fontptr = NULL; // Return no font found
        return MPE_GFX_ERROR_NOFONT;
    }

    /* allocate a new font and initialize it */
    if (MPE_SUCCESS != (fontListAllocFont(&font)))
    {
        FF_UNLOCK(ffact);
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxFactoryCreateFont() - ERROR - fontListAllocFont() failed\n");
        *fontptr = NULL; // return a memory allocation error
        return MPE_GFX_ERROR_NOMEM;
    }

    /* create the mutex */
    if (mpeos_mutexNew(&(font->mutex)) != MPE_SUCCESS)
    {
        FF_UNLOCK(ffact);
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxFactoryCreateFont() - ERROR - Failed to create Mutex\n");
        mpeos_memFreeP(MPE_MEM_GFX, font);
        *fontptr = NULL; // return an OS failure
        return MPE_GFX_ERROR_OSERR;
    }

    /* allocate space for the font name */
    if (MPE_SUCCESS
            != (mpeos_memAllocP(MPE_MEM_GFX, namelength * sizeof(mpe_GfxWchar)
                    + sizeof(mpe_GfxWchar), (void**) &font->name)))
    {
        FF_UNLOCK(ffact);
        mpeos_mutexDelete(font->mutex);
        mpeos_memFreeP(MPE_MEM_GFX, font);
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> gfxFactoryCreateFont() - ERROR - memAllocP() failed\n");
        *fontptr = NULL; // return an allocation error again
        return MPE_GFX_ERROR_NOMEM;
    }
        
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_GFX,
                "<<GFX>> gfxFactoryCreateFont() - creating font %x\n", font);

    font->namelength = namelength;
    memcpy(font->name, name, namelength * sizeof(mpe_GfxWchar));
    font->name[namelength] = 0;
    font->ff = ff;
    font->style = style;
    font->size = size;
    font->refCount = 0; /* updated in the gfx context when the font is set */

    fontListPrepend(ffact, font);

    /* Now create the DirectFB font. */
    dfb_fd.flags = DFDESC_HEIGHT;
    dfb_fd.height = font->size;

    /* all the fonts are created from buffer */
    if (DFB_OK != thiz->CreateFontFromBuffer(thiz, (char*) elt->data,
            elt->datasize, &dfb_fd, &(font->osf.dfb_fnt)))
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
                "<<GFX>> mpeos_gfxFontNew() - ERROR - failed to Create font from buffer\n");
        gfxFactoryRemoveFont(ff, font);

        FF_UNLOCK(ffact);
        *fontptr = NULL; // Return a Font Format error
        return MPE_GFX_ERROR_FONTFORMAT;
    }

    /* increment the reference count (done for each create + each context reference) */
    (void) gfxFontUpdateCount((mpe_GfxFont) font, 1);

    /* increment the count of created fonts in the factory */
    gfxFactoryUpdateFontCount(ff, 1);

    FF_UNLOCK(ffact);

    *fontptr = font; // return font and no errors
    return MPE_GFX_ERROR_NOERR;
}

/**
 * Deletes the given font from the factory.
 * Searches the font list for the font and deletes it if found.
 * This completes font deletion started by gfxFontDelete().
 * gfxFactoryRemoveFont() is call from the font factory critical section
 */
void gfxFactoryRemoveFont(mpe_GfxFontFactory ff, mpeos_GfxFont *font)
{
    mpeos_GfxFontFactory *ffact = (mpeos_GfxFontFactory*) ff;
    mpeos_GfxFont *f = NULL;
    mpeos_GfxFont *fonthead = MPE_FAKEHEAD(mpeos_GfxFont, ffact->fontList[0],
            prev);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_GFX,
            "<<GFX>> gfxFactoryRemoveFont() removing font %x\n", font);

    for (f = fonthead->next; f != fonthead; f = f->next)
    {
        if (f == font)
        {
            /* Delete the name buffer */
            mpeos_memFreeP(MPE_MEM_GFX, f->name);
            /* Delete the mutex */
            mpeos_mutexDelete(f->mutex);
            /* Delete the DFB font. */
            if (f->osf.dfb_fnt)
                f->osf.dfb_fnt->Release(f->osf.dfb_fnt);
            /* Remove from list and delete font. */

            fontListRemove(f);
            mpeos_memFreeP(MPE_MEM_GFX, f);

            /* update the font count in the factory and delete it if requested */
            gfxFactoryUpdateFontCount(ff, -1);
            return;
        }
    }
    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_GFX,
            "<<GFX>> gfxFactoryRemoveFont() could not find %p!!!\n", font);
}

/**
 * Updates the count of fonts produced by the font factory.
 * This count is incremented as fonts are created and decremented
 * as they are deleted.
 * Only when the count is zero can the font factory be deleted.
 */
void gfxFactoryUpdateFontCount(mpe_GfxFontFactory ff, int inc)
{
    mpeos_GfxFontFactory *ffact = (mpeos_GfxFontFactory*) ff;
    ffact->fontCount += inc;

    if (ffact->tbd && ffact->fontCount == 0)
    {
        (void) mpeos_gfxFontFactoryDelete(ff);
    }
}

/**
 * Create and initialize sys_fontfactory.
 */
mpe_Error gfxFactoryCreateDefault(void)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;

    /* !!!!! Should create system font here !!!!! */
    if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_GFX,
            sizeof(mpe_GfxFontFactory), (void**) &sys_fontfactory)))
    {
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_GFX,
                "<<GFX>> gfxFactoryCreateDefault() - ERROR - Failed to allocate System Factory handle\n");
        return MPE_GFX_ERROR_NOMEM;
    }

    err = mpeos_gfxFontFactoryNew(&sys_fontfactory);

    return err;
}

/**
 *
 */
static mpe_Error descListAllocDesc(mpe_GfxFontDesc **fd)
{
    mpe_Error ec;
    /* Allocate a new font descriptor structure. */
    if ((ec = mpeos_memAllocP(MPE_MEM_GFX, sizeof(mpe_GfxFontDesc),
            (void **) fd)) != MPE_SUCCESS)
        return ec;

    /* Initialize fields. */

    (*fd)->data = NULL;
    (*fd)->datasize = 0;
    (*fd)->fnt_format = (mpe_GfxFontFormat) 0;
    (*fd)->maxsize = 0;
    (*fd)->minsize = 0;
    (*fd)->name = NULL;
    (*fd)->namelength = 0;
    (*fd)->style = (mpe_GfxFontStyle) 0;
    (*fd)->prev = (*fd)->next = *fd;

    return ec;
}

/**
 *
 */
static void descListPrepend(mpeos_GfxFontFactory *ff, mpe_GfxFontDesc *desc)
{
    mpe_GfxFontDesc *head;

    /* Get virtual head pointer. */
    head = MPE_FAKEHEAD(mpe_GfxFontDesc, ff->descList[0], prev);

    /* Link font descriptor on to the head of the list. */
    desc->next = head->next;
    desc->prev = head;
    head->next->prev = desc;
    head->next = desc;

}

/**
 *
 */
static mpe_Bool descListSearch(mpe_GfxFontDesc **list, mpe_GfxFontDesc *desc)
{
    mpe_GfxFontDesc *head;
    mpe_GfxFontDesc *elt;

    head = MPE_FAKEHEAD(mpe_GfxFontDesc, list[0], prev);
    /* sanity check */
    if ((head->next == NULL) || (head->next == head))
        return false;

    for (elt = head->next; elt != head; elt = elt->next)
    {
        if (desc->namelength == elt->namelength && !wcstringnicmp(desc->name,
                elt->name, desc->namelength, elt->namelength) && elt->minsize
                == desc->minsize && elt->maxsize == desc->maxsize && elt->style
                == desc->style && elt->fnt_format == desc->fnt_format)
        {
            return true;
        }
    }
    return false;
}

/**
 *
 */
static void descListRemove(mpe_GfxFontDesc *desc)
{
    /* Remove the descriptor from the list. */
    desc->prev->next = desc->next;
    desc->next->prev = desc->prev;
}

/**
 *
 */
static mpe_Error fontListAllocFont(mpeos_GfxFont **font)
{
    mpe_Error ec;
    /* Allocate a new font */
    if ((ec = mpeos_memAllocP(MPE_MEM_GFX, sizeof(mpeos_GfxFont),
            (void **) font)) != MPE_SUCCESS)
        return ec;

    /* Initialize fields. */

    (*font)->ff = 0;
    (*font)->mutex = 0;
    (*font)->name = NULL;
    (*font)->namelength = 0;
    (*font)->osf.dfb_fnt = 0;
    (*font)->refCount = 0;
    (*font)->size = 0;
    (*font)->style = (mpe_GfxFontStyle) 0;

    (*font)->prev = (*font)->next = *font;

    return ec;
}

/**
 *
 */
static void fontListPrepend(mpeos_GfxFontFactory *ff, mpeos_GfxFont *font)
{
    mpeos_GfxFont *head;

    /* Get virtual head pointer. */
    head = MPE_FAKEHEAD(mpeos_GfxFont, ff->fontList[0], prev);

    /* Link font on to the head of the list. */
    font->next = head->next;
    font->prev = head;
    head->next->prev = font;
    head->next = font;

}

/**
 *
 */
static void fontListRemove(mpeos_GfxFont *font)
{
    /* Remove the font from the list. */
    font->prev->next = font->next;
    font->next->prev = font->prev;
}

/**
 * Search the given font list for an exact match.
 * An appropriate lock (e.g., the lock for the associated font factory) should be
 * held during this search to ensure that the list doesn't change during the search
 *
 * @param fonthead font list to search
 * @param name font name
 * @param namelength length of font name
 * @param size size of requested font
 * @param style of requested font
 */
static mpeos_GfxFont* fontListFind(mpeos_GfxFont *fonthead,
        const mpe_GfxWchar *name, uint32_t namelength, uint32_t size,
        mpe_GfxFontStyle style)
{
    mpeos_GfxFont* font;
    for (font = fonthead->next; font != fonthead; font = font->next)
    {
        if (font->namelength == namelength && !wcstringnicmp(font->name, name,
                font->namelength, namelength) && size == font->size && style
                == font->style)
        {
            (void) gfxFontUpdateCount((mpe_GfxFont) font, 1);
            return font;
        }
    }
    return NULL;
}

/*
 * Wide character string support functions
 */

/**
 * Counts the number of wide char in a string until NULL char is reached
 * @param   ws      a NULL wide character string
 * @return          the number of wide character is ws
 */
size_t wcstringlen(const mpe_GfxWchar *ws)
{
    size_t len = 0;

    while (*ws++ != 0)
        len++;

    return len;
}

/**
 * Copies wide character string, including the terminating null character,
 * to the specified location. No overflow checking is performed when strings are copied.
 * @param   wd      the destination string
 * @param   ws      the source string (NULL terminated)
 * @return          a copied string
 */
mpe_GfxWchar* wcstringcpy(mpe_GfxWchar *wd, const mpe_GfxWchar *ws)
{
    mpe_GfxWchar *top = wd;

    while ((*wd++ = *ws++) != 0)
        ;
    return top;
}

/**
 * Copies wide character string upto a given number of characters or
 * until the NULL character is reached, whichever comes first.
 * No boundary check on the string buffer.
 *
 * @param   wd      the destination string
 * @param   ws      the source string (NULL terminated)
 *
 * @return          a copy of the string
 */
mpe_GfxWchar * wcstringncpy(mpe_GfxWchar *wd, const mpe_GfxWchar *ws, size_t n)
{
    mpe_GfxWchar *top = wd;

    size_t count = n;

    if (count == 0)
        return top;

    while (count > 0)
    {
        if ((*wd++ = *ws++) == 0)
            break;
        count--;
    }

    while (count-- > 0)
        *wd++ = 0;

    return top;
}

/**
 * Compares 2 wide character strings upto a given number of characters.
 * The case does not matter.
 * @param   ws1     the first string
 * @param   ws2     the second string
 * @param   n1      the number of characters in ws1
 * @param   n2      the number of characters in ws2
 * @return
 *          < 0 ws1 lesser than ws2
 *            0 ws1 identical to ws2
 *          > 0 ws1 greater than ws2
 */
int wcstringnicmp(const mpe_GfxWchar *ws1, const mpe_GfxWchar *ws2, size_t n1,
        size_t n2)
{
    int cmp;
    mpe_GfxWchar *end;

    int count = n1;

    if (n1 != n2)
        return (n1 - n2);

    /* should check parameters here !!! */
    end = (mpe_GfxWchar *) ws1 + count;
    do
    {
        cmp = tolower((int) *ws1) - tolower((int) *ws2);
    } while (*ws1++ && *ws2++ && cmp == 0 && ws1 < end);
    return (cmp);
}

/**
 * Compares 2 NULL terminated wide character strings.
 * The case does not matter.
 * @param   ws1     the first string
 * @param   ws2     the second string
 * @return
 *          < 0 ws1 lesser than ws2
 *            0 ws1 identical to ws2
 *          > 0 ws1 greater than ws2
 */
int wcstringicmp(const mpe_GfxWchar *ws1, const mpe_GfxWchar *ws2)
{

    int cmp;
    mpe_GfxWchar *end;

    int count = wcstringlen(ws1);

    /* should check parameters here !!! */
    end = (mpe_GfxWchar *) ws1 + count;
    do
    {
        cmp = tolower((int) *ws1) - tolower((int) *ws2);
    } while (*ws1++ && *ws2++ && cmp == 0 && ws1 < end);
    return (cmp);
}

/**
 * Converts a sequence of wide characters to a corresponding sequence of multibyte characters.
 * @param   mbstr   The address of a sequence of multibyte characters
 * @param   wcstr   The address of a sequence of wide characters
 * @param   n       The maximum number of bytes that can be stored in the multibyte output string
 * @return          MPE_GFX_ERROR_NOERR if the conversion was successfull and
 *                  MPE_GFX_ERROR_UNKNOWN otherwise.
 */
mpe_Error wcstringtombs(char *mbstr, const mpe_GfxWchar *wcstr, size_t n)
{
    ConversionResult res;
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    int32_t target_size = (n << 2) + 1;

    res = ConvertUTF16toUTF8((const UTF16**) &wcstr, (UTF16*) (wcstr + n),
            (UTF8**) &mbstr, (UTF8*) (mbstr + target_size),
            (ConversionFlags) strictConversion);

    if (res != conversionOK)
        err = MPE_GFX_ERROR_UNKNOWN;

    return err;

}

/**
 * Converts multibyte characters to a corresponding sequence of wide characters .
 * @param   wcstr   The address of a sequence of wide characters
 * @param   mbstr   The address of a sequence of multibyte characters
 * @param   n       The maximum number of bytes that can be stored in the multibyte output string
 * @return          If mbstowcstring successfully converts the multibyte string,
 *                  it returns the number of bytes written into the output string,
 *                  excluding the terminating NULL (if any).
 *                  If the mbstr argument is NULL, mbstowcstring returns the required size
 *                  of the destination string.
 */
mpe_Error mbstowcstring(mpe_GfxWchar *wcstr, const char *mbstr, size_t n)
{

    ConversionResult res;
    mpe_Error err = MPE_GFX_ERROR_NOERR;

    if (wcstr == NULL)
    {
        return strlen(mbstr) * sizeof(mpe_GfxWchar);
    }

    res = ConvertUTF8toUTF16((const UTF8 **) &mbstr, (UTF8*) (mbstr + strlen(
            mbstr)), &wcstr, (wcstr + n), strictConversion);

    if (res != conversionOK)
        err = MPE_GFX_ERROR_UNKNOWN;

    return err;
}
