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

#include <osmgr.h>
#include <sysmgr.h>
#include <dispmgr.h>
#include <mgrdef.h>
#include <mpe_os.h>
#include <mpe_file.h>
#include <mpeos_util.h>
#include <mpeos_file.h>

/* resolves display manager os functions and definitions */
#include <mpeos_gfx.h>
#include <gfx/mpeos_screen.h>
#include <gfx/mpeos_context.h>
#include <gfx/mpeos_surface.h>
#include <gfx/mpeos_draw.h>
#include <gfx/mpeos_font.h>
#include <gfx/mpeos_fontfact.h>
#include <gfx/mpeos_uievent.h>
#include "mpeos_disp.h"
#include "mpeos_dbg.h"

#define MAX_SYSFONTS 5 /* maximum number of system read fron the sysfont directory */

/* SYSFONT key table from ini
 * SYSFONT
 * SYSFONT.<logicalfontname>[.<style>][.<n>]
 *
 * 24 keys are generated per font assuming
 * style = BOLD,ITALIC, BOLD_ITALIC and n = 5
 */
#define MAX_KEY_COUNT 240   /* enough to hold 10 fonts */
#define MAX_KEYS 5         /* number of of key unique identifier */
#define MAX_KEY_LEN  256   /* the key string length (left hand side) */
#define MAX_KEYVAL_LEN 80   /* the key value length (right hand side) */

static char *keys[MAX_KEY_COUNT];

/* exported from mpeos_fontfact.c */
extern mpe_Error gfxFactoryCreateDefault(void);
extern mpe_Error
        mbstowcstring(mpe_GfxWchar *wcstr, const char *mbstr, size_t n);
extern mpe_GfxFontFactory sys_fontfactory;
extern mpe_GfxFont sys_font;

/* internal function prototypes */
static mpe_Error mpe_InitSysFontFromIni(void);
static mpe_Error mpe_CreateSystemFont(void);
static mpe_Error mpe_GenerateSysFontKeys(uint32 *key_count);
static void mpe_DiscardSysFontKeys(uint32 key_count);

void mpeos_dispInit(void);

extern mpeos_filesys_ftable_t port_fileSnfsFTable;

/* function table */
mpe_disp_ftable_t dispmgr_ftable =
{ mpeos_dispInit,

/* UIEvent */
mpeos_gfxWaitNextEvent,
/* Context */
mpeos_gfxContextNew, mpeos_gfxContextCreate, mpeos_gfxContextDelete,
        mpeos_gfxGetSurface, mpeos_gfxGetColor, mpeos_gfxSetColor,
        mpeos_gfxGetFont, mpeos_gfxSetFont, mpeos_gfxGetPaintMode,
        mpeos_gfxSetPaintMode, mpeos_gfxGetOrigin, mpeos_gfxSetOrigin,
        mpeos_gfxGetClipRect, mpeos_gfxSetClipRect,
        /* Draw */
        mpeos_gfxDrawLine, mpeos_gfxDrawRect, mpeos_gfxFillRect,
        mpeos_gfxDrawEllipse, mpeos_gfxFillEllipse, mpeos_gfxDrawRoundRect,
        mpeos_gfxFillRoundRect, mpeos_gfxDrawArc, mpeos_gfxFillArc,
        mpeos_gfxDrawPolyline, mpeos_gfxDrawPolygon, mpeos_gfxFillPolygon,
        mpeos_gfxBitBlt, mpeos_gfxStretchBlt, mpeos_gfxDrawString,
        mpeos_gfxDrawString16,
        /* Surface */
        mpeos_gfxSurfaceNew, mpeos_gfxSurfaceCreate, mpeos_gfxSurfaceDelete,
        mpeos_gfxSurfaceGetInfo,
        /* Font */
        mpeos_gfxFontNew, mpeos_gfxFontDelete, mpeos_gfxGetFontMetrics,
        mpeos_gfxGetStringWidth, mpeos_gfxGetString16Width,
        mpeos_gfxGetCharWidth, mpeos_gfxFontHasCode,
        /* Font factory */
        mpeos_gfxFontFactoryNew, mpeos_gfxFontFactoryDelete,
        mpeos_gfxFontFactoryAdd, mpeos_gfxFontGetList,

        /* Additional Draw */
        mpeos_gfxClearRect,

        /* Display Discover/Configuration */
        mpeos_dispGetScreenCount, mpeos_dispGetScreens,
        mpeos_dispGetScreenInfo, mpeos_dispGetDeviceCount,
        mpeos_dispGetDevices, mpeos_dispGetDeviceInfo,
        mpeos_dispGetConfigCount, mpeos_dispGetConfigs,
        mpeos_dispGetCurrConfig, mpeos_dispSetCurrConfig,
        mpeos_dispWouldImpact, mpeos_dispGetConfigInfo,
        mpeos_dispGetCoherentConfigCount, mpeos_dispGetCoherentConfigs,
        mpeos_dispSetCoherentConfig, mpeos_dispGetConfigSetCount,
        mpeos_dispGetConfigSet, mpeos_dispSetBGColor, mpeos_dispGetBGColor,
        mpeos_dispDisplayBGImage, mpeos_dispBGImageGetSize,
        mpeos_dispGetOutputPortCount, mpeos_dispGetOutputPorts,
        mpeos_dispEnableOutputPort, mpeos_dispGetOutputPortInfo,

        /* Palette */
        mpeos_gfxPaletteNew, mpeos_gfxPaletteDelete, mpeos_gfxPaletteSet,
        mpeos_gfxPaletteGet, mpeos_gfxPaletteMatch,

        mpeos_dispBGImageNew, mpeos_dispBGImageDelete,
        mpeos_dispGetRFBypassState, mpeos_dispSetRFBypassState,
        mpeos_dispGetRFChannel, mpeos_dispSetRFChannel,

        mpeos_dispGetDFC, mpeos_dispCheckDFC, mpeos_dispSetDFC,
        mpeos_dispGetVideoOutputPortOption, mpeos_dispSetVideoOutputPortOption,

        /* Gfx Device Surface */
        mpeos_dispGetGfxSurface, mpeos_dispFlushGfxSurface, };

#if 0
void mpe_dispSetup(void)
{
    mpe_sys_install_ftable(&dispmgr_ftable, MPE_MGR_TYPE_DISP);
}
#endif

static void mpe_DiscardSysFontKeys(uint32 key_count)
{
    uint32 n;
    for (n = 0; n < key_count; n++)
    {
        mpeos_memFreeP(MPE_MEM_TEMP, keys[n]);
    }
}

static mpe_Error mpe_GenerateSysFontKeys(uint32 *key_count)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    uint32 s, n;
    char* styles[MPE_GFX_FNT_STYLE_MAX] =
    { "PLAIN", "BOLD", "ITALIC", "BOLD_ITALIC" };
    uint32 nkeys = 0;
    //char sysfont[]="SYSFONT\0";
    const char seps1[] = ".,";
    /* logical font list:
     * logicalfont1,logicalfont2,...
     */
    const char *envLogicalFonts;
    char *logicalFonts;
    char *token = NULL;
    char *logicalNames = ",Tiresias,";

    *key_count = 0;

    /* read logical font names */
    //envLogicalFonts = mpe_envGet(sysfont);
    envLogicalFonts = logicalNames;
    if (NULL == envLogicalFonts)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                "<mpe_GenerateSysFontKeys> ERROR - SYSFONT=<logicalfontname>,... not found\n");
        return MPE_GFX_ERROR_UNKNOWN;
    }

    /* make local copy of font names string (so that we can strtok() on it) */
    err = mpeos_memAllocP(MPE_MEM_TEMP, (strlen(envLogicalFonts) + 1),
            (void**) &logicalFonts);
    if (NULL == logicalFonts)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                "<mpe_GenerateSysFontKeys> ERROR - out of memory\n");
        return MPE_GFX_ERROR_NOMEM;
    }
    strcpy(logicalFonts, envLogicalFonts);

    token = strtok(logicalFonts, seps1); /* extract the first logical font */
    nkeys = 0;

    /* generate all the keys */
    while (token != NULL)
    {
        if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_TEMP, MAX_KEY_LEN,
                (void**) &keys[nkeys])))
        {
            (void) mpeos_memFreeP(MPE_MEM_TEMP, logicalFonts);
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                    "<mpe_GenerateSysFontKeys> ERROR - Failed to allocate buffer\n");
            return MPE_GFX_ERROR_NOMEM;
        }

        /* SYSFONT.<logicalfontname> */
        sprintf(keys[nkeys++], "SYSFONT.%s", token);

        /* SYSFONT.<logicalfontname>[<.n>] with n = [0..MAX_KEYS] */
        for (n = 0; n < MAX_KEYS; n++)
        {
            if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_TEMP,
                    MAX_KEY_LEN, (void**) &keys[nkeys])))
            {
                (void) mpeos_memFreeP(MPE_MEM_TEMP, logicalFonts);
                MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                        "<mpe_GenerateSysFontKeys> ERROR - Failed to allocate buffer\n");
                return MPE_GFX_ERROR_NOMEM;
            }
            sprintf(keys[nkeys++], "SYSFONT.%s.%lu", token, n);
        }

        /* SYSFONT.<logicalfontname>[.<style>] with style = [BOLD,ITALIC,ITALIC_BOLD] */
        for (s = MPE_GFX_BOLD; s < MPE_GFX_FNT_STYLE_MAX; s++)
        {
            if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_TEMP,
                    MAX_KEY_LEN, (void**) &keys[nkeys])))
            {
                (void) mpeos_memFreeP(MPE_MEM_TEMP, logicalFonts);
                MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                        "<mpe_GenerateSysFontKeys> ERROR - Failed to allocate buffer\n");
                return MPE_GFX_ERROR_NOMEM;
            }
            sprintf(keys[nkeys++], "SYSFONT.%s.%s", token, styles[s]);

            /* SYSFONT.<logicalfontname>[.<style>][<.n>] with n = [0..MAX_KEYS] */
            for (n = 0; n < MAX_KEYS; n++)
            {
                if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_TEMP,
                        MAX_KEY_LEN, (void**) &keys[nkeys])))
                {
                    (void) mpeos_memFreeP(MPE_MEM_TEMP, logicalFonts);
                    MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                            "<mpe_GenerateSysFontKeys> ERROR - Failed to allocate buffer\n");
                    return MPE_GFX_ERROR_NOMEM;
                }
                sprintf(keys[nkeys++], "SYSFONT.%s.%s.%lu", token, styles[s], n);
            }

        }

        token = strtok(NULL, seps1);
    }

    *key_count = nkeys;

    /* free up local environment variable copy buffer */
    (void) mpeos_memFreeP(MPE_MEM_TEMP, logicalFonts);

    return err;

}

static mpe_Error mpe_InitSysFontFromIni(void)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    char *fontName;
    mpe_GfxWchar fname[64];
    char *style;
    mpe_GfxFontDesc fdesc;
    mpe_FileInfo fileInfo;
    uint8* fdata;
    uint32 count = 0;
    uint32 size;
    mpe_FileError returnValue;
    mpe_File fileHandle;

    err = mpe_GenerateSysFontKeys(&count);

    if (err != MPE_GFX_ERROR_NOERR)
    {
        return err;
    }

#define DEFAULT_FONT "Tires-o_802.pfr"

    /*
     * Get the font data from file and data size
     */
    returnValue = (port_fileSnfsFTable.mpeos_fileOpen_ptr)(DEFAULT_FONT,
            MPE_FS_OPEN_READ, &fileHandle);
    if (returnValue != MPE_FS_ERROR_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "<mpe_InitSysFontFromIni> ERROR - Unable to open %s\n",
                DEFAULT_FONT);
        return MPE_GFX_ERROR_UNKNOWN;
    }

    returnValue = (port_fileSnfsFTable.mpeos_fileGetFStat_ptr)(fileHandle,
            MPE_FS_STAT_SIZE, &fileInfo);
    if (returnValue != MPE_FS_ERROR_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "<mpe_InitSysFontFromIni> ERROR - Can't FStat file %s\n",
                DEFAULT_FONT);
        (void) (port_fileSnfsFTable.mpeos_fileClose_ptr)(fileHandle);
        return MPE_GFX_ERROR_UNKNOWN;
    }

    if (fileInfo.size == 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "<mpe_InitSysFontFromIni> ERROR - empty file %s\n",
                DEFAULT_FONT );
        (void) (port_fileSnfsFTable.mpeos_fileClose_ptr)(fileHandle);
        return MPE_GFX_ERROR_UNKNOWN;
    }

    if (MPE_SUCCESS != (err = mpeos_memAllocP(MPE_MEM_TEMP,
            (uint32) fileInfo.size, (void**) &fdata)))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "<mpe_InitSysFontFromIni> ERROR - Can't alloc System font data buffer\n");
        (void) (port_fileSnfsFTable.mpeos_fileClose_ptr)(fileHandle);
        return MPE_GFX_ERROR_NOMEM;
    }

    size = (uint32) fileInfo.size;

    returnValue = (port_fileSnfsFTable.mpeos_fileRead_ptr)(fileHandle, &size,
            fdata);
    if (returnValue != MPE_FS_ERROR_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_TEMP, fdata);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "<mpe_InitSysFontFromIni> ERROR - Can't read file %s\n",
                DEFAULT_FONT );
        (void) (port_fileSnfsFTable.mpeos_fileClose_ptr)(fileHandle);
        return MPE_GFX_ERROR_UNKNOWN;
    }

    fdesc.data = fdata;
    fdesc.datasize = size;

    /*
     * Extract maxsize and minsize
     */
    fdesc.minsize = 1;
    fdesc.maxsize = 0xFFFFFF;

    /*
     * Extract name and style
     */
    fontName = "Tiresias";
    style = NULL;

    mbstowcstring(fname, fontName, strlen(fontName));
    fdesc.name = fname;
    fdesc.namelength = strlen(fontName);

    fdesc.fnt_format = GFX_FONT_PFR;

    if (style)
    {
        if (!strcmp(style, "BOLD"))
        {
            fdesc.style = MPE_GFX_BOLD;
        }
        else if (!strcmp(style, "ITALIC"))
        {
            fdesc.style = MPE_GFX_ITALIC;
        }
        else if (!strcmp(style, "BOLD_ITALIC"))
        {
            fdesc.style = MPE_GFX_BOLD_ITALIC;
        }
        else
        {
            fdesc.style = MPE_GFX_PLAIN;
        }
    }
    else
    {
        fdesc.style = MPE_GFX_PLAIN;
    }

    (void) (port_fileSnfsFTable.mpeos_fileClose_ptr)(fileHandle);

    err = mpeos_gfxFontFactoryAdd(sys_fontfactory, &fdesc);

    mpeos_memFreeP(MPE_MEM_TEMP, fdata);

    if (err != MPE_GFX_ERROR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                "<mpe_InitSysFontFromIni> ERROR - mpeos_gfxFontFactoryAdd Failed\n");
        return MPE_GFX_ERROR_UNKNOWN;
    }

    mpe_DiscardSysFontKeys(count);

    if (count == 0)
    {
        err = MPE_GFX_ERROR_UNKNOWN;
    }
    else
    {
        err = mpe_CreateSystemFont();
    }

    return err;
}

static mpe_Error mpe_CreateSystemFont(void)
{
    mpe_Error err = MPE_GFX_ERROR_NOERR;
    mpe_GfxFontDesc *sys_fd;

    err = mpeos_gfxFontGetList(&sys_fd);

    if (err == MPE_GFX_ERROR_NOERR)
    {
        if (sys_fd->next)
        {
            err = mpeos_gfxFontNew(NULL, sys_fd->next->name,
                    sys_fd->next->namelength, sys_fd->next->style, 26,
                    &sys_font);
        }
        else
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                    "<mpe_CreateSystemFont> ERROR - System Font Factory is Empty\n");
            return MPE_GFX_ERROR_UNKNOWN;
        }
    }

    if (err != MPE_GFX_ERROR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DISP,
                "<mpe_CreateSystemFont> ERROR - mpeos_gfxFontNew Failed\n");
    }
    return err;
}

static mpe_Bool inited = false;
void mpeos_dispInit()
{
    uint32 nScreens = 0;

    if (!inited)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DISP,
                "<mpeos_dispInit> Initializing MPE GFX\n");

        /* Initialize any other managers that are needed
         perform this manager initialization */

        /* Create the default screen and initialize DirectFB */
        if (mpeos_gfxCreateDefaultScreen() != MPE_GFX_ERROR_NOERR)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "<mpeos_dispInit> ERROR Cannot initialize Default Screen\n");
            return;
        }
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP,
                "<mpeos_dispInit> DIRECTFB is initialized\n");

        /* Init the default system font factory and add the font descriptors to it*/
        if (gfxFactoryCreateDefault() != MPE_GFX_ERROR_NOERR)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "<mpeos_dispInit> ERROR Cannot create system font factory\n");
            return;
        }
        if (mpe_InitSysFontFromIni() != MPE_GFX_ERROR_NOERR)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "<mpeos_dispInit> ERROR Cannot initialize Default Font Factory\n");
            return;
        }
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP,
                "<mpeos_dispInit> FontFactory is initialized\n");

        /* get the screen count to initialize (as a by-product) the video output ports */
        if (mpeos_dispGetScreenCount(&nScreens) != MPE_GFX_ERROR_NOERR)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DISP,
                    "<mpeos_dispInit> ERROR Cannot retrieve the screen count\n");
            return;
        }

        inited = true;

        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP,
                "<mpeos_dispInit> MPE GFX Init Complete\n");
#if 0
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DISP, "<mpeos_dispInit> Running Smoke Test\n");
        testRun();
#endif

    }
}
