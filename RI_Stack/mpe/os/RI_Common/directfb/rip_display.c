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

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

#include <stdlib.h>

/* MPE/MPEOS includes */

#ifdef NON_MPEOS_USAGE
extern void MPEOS_LOG(char * level, char * module, char * format, ...);
#else
#include <mpeos_dbg.h>
#endif

/* Local includes */
#include "rip_display.h"
#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>
#include <ri_display.h>

////////////////////////////////////////////////////////////////////////////
// GLOBALS
////////////////////////////////////////////////////////////////////////////

//static SECURITY_ATTRIBUTES * g_smfbSecurityAttributes = NULL;
static ri_display_t* pDisplay = NULL;

////////////////////////////////////////////////////////////////////////////
// PRIVATE FUNCTION DEFINITIONS
////////////////////////////////////////////////////////////////////////////
static RIPD_Result initializeDisplay();

////////////////////////////////////////////////////////////////////////////
// PUBLIC FUNCTIONS
////////////////////////////////////////////////////////////////////////////

/**
 * Initialize the display with the selected coherent configuration
 */
RIPD_Result rip_InitDisplay(unsigned int graphicsWidth,
        unsigned int graphicsHeight, unsigned int graphicsPARx,
        unsigned int graphicsPARy, unsigned int videoWidth,
        unsigned int videoHeight, unsigned int videoPARx,
        unsigned int videoPARy, unsigned int backgroundWidth,
        unsigned int backgroundHeight, unsigned int backgroundPARx,
        unsigned int backgroundPARy)
{
    RIPD_Result result = RIPD_FAILED_INIT;

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_DIRECTFB,
            "<<GFX>> rip_InitDisplay: graphicsWidth = %d, graphicsHeight = %d\n",
            graphicsWidth, graphicsHeight);

    // Initialize the display component reference
    if (NULL == pDisplay)
    {
        result = initializeDisplay();
    }
    else
    {
        result = RIPD_SUCCESS;
    }

    // If initialization was successful, update the display configuration
    if ((RIPD_SUCCESS == result) && (NULL != pDisplay))
    {
        pDisplay->update_configuration(pDisplay, graphicsWidth, graphicsHeight,
                graphicsPARx, graphicsPARy, videoWidth, videoHeight, videoPARx,
                videoPARy, backgroundWidth, backgroundHeight, backgroundPARx,
                backgroundPARy, ENV_STACK);
    }

    return result;
}

/**
 * Initialize the surface supplied by RI Platform elements.
 * This should only be called once, prior to any other functions.
 */
RIPD_Result InitSurface(void)
{
    // Make sure the display access has been initialized
    RIPD_Result result = RIPD_FAILED_INIT;
    if (NULL == pDisplay)
    {
        result = initializeDisplay();
    }
    return result;
}

/**
 * Get access to display component within RI pipeline
 */
static RIPD_Result initializeDisplay()
{
    RIPD_Result result = RIPD_FAILED_INIT;

    // Get the RI Platform display element from pipeline manager
    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        // Get the display from the pipeline
        pDisplay = pMgr->get_display(pMgr);
        if (NULL != pDisplay)
        {
            result = RIPD_SUCCESS;
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DIRECTFB,
                    "<<GFX>> Unable to get display from pipeline mgr (%d)\n",
                    result);
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DIRECTFB,
                "<<GFX>> Unable to get pipeline mgr (%d)\n", result);
    }

    return result;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

void GetCurrentPrimaryBuffer(void** ppPrimaryBuf)
{
    if (NULL != pDisplay)
    {
        *ppPrimaryBuf = pDisplay->get_graphics_buffer(pDisplay, ENV_STACK);
    }
}

/**
 * Allocate a framebuffer which is associated with RI Platform elements and
 * return a pointer to the control structure.
 * Returns 0 on success and non-zero on failure.
 */
RIPD_Result AllocateSurface(unsigned int width, unsigned int height,
        unsigned int bitsPerPixel, unsigned int bytesPerPixel,
        RIPD_BufferMode bufMode, RIPDisplaySession ** ppDisplaySession)
{
    // We get to pick bytesPerLine. We assume there's no need for padding,
    // currently
    int bytesPerLine = width * bytesPerPixel;
    mpe_Bool isDoubleBuffered = (bufMode == RIPD_DOUBLEBUFFER);
    RIPDisplay * pRipDisplay = NULL;
    RIPDisplaySession * pDisplaySession = NULL;

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DIRECTFB,
            "<<GFX>> AllocateSurface: %dx%d %d bpp, bufmode %d\n",
            width, height, bitsPerPixel, bufMode);

    pRipDisplay = (RIPDisplay *) malloc(sizeof(RIPDisplay));
    if (NULL == pRipDisplay)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DIRECTFB,
                "<<GFX>> Could not allocate a Display.\n");
        return RIPD_FAILED_LOCAL_ALLOC;
    }
    pRipDisplay->width = width;
    pRipDisplay->height = height;
    pRipDisplay->bitsPerPixel = bitsPerPixel;
    pRipDisplay->bytesPerPixel = bytesPerPixel;
    pRipDisplay->bytesPerLine = bytesPerLine;
    pRipDisplay->bufMode = isDoubleBuffered;

    // We'll assume the buffer(s) are going to be written to first
    pRipDisplay->primaryBufState = RIPD_BUF_WRITING;
    if (RIPD_DOUBLEBUFFER == bufMode)
    {
        pRipDisplay->secondaryBufState = RIPD_BUF_READY;
    }
    else
    {
        pRipDisplay->secondaryBufState = RIPD_BUF_DISABLED;
    }

    // Determine if configuration matches what display is currently set at
    //if (!configMatches())
    //{
    // Ask for a new buffer which matches current config to be created
    //}

    // Assign the graphics display buffer as the primar buffer
    if (NULL != pDisplay)
    {
        pRipDisplay->primaryBuf = pDisplay->get_graphics_buffer(pDisplay,
                ENV_STACK);
    }

    pDisplaySession = (RIPDisplaySession *) malloc(sizeof(RIPDisplaySession));
    if (NULL == pDisplaySession)
    {
        free(pRipDisplay);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DIRECTFB,
                "<<GFX>> Could not allocate a DisplaySession.\n");
        return RIPD_FAILED_LOCAL_ALLOC;
    }

    pDisplaySession->ripDisplay = pRipDisplay;
    pDisplaySession->primaryBuf = pRipDisplay->primaryBuf;
    pDisplaySession->secondaryBuf = pRipDisplay->primaryBuf;

    *ppDisplaySession = pDisplaySession;

    return RIPD_SUCCESS;
}

////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////

/**
 * Signal that a framebuffer is ready to be displayed on-screen.
 *  If double-buffering is enabled, bufToPaint designates which buffer to
 *  display on-screen from the designated framebuffer.
 *  Returns 0 on success and non-zero on failure.
 */
void RefreshSurface(RIPDisplaySession * pDisplaySession,
        RIPD_BufferType bufToPaint)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DIRECTFB,
            "<<GFX>> RefreshSurface: pDisplaySession 0x%p, bufToPaint %d\n",
            pDisplaySession, bufToPaint);

    // Notify display that screen should be repainted
    if (NULL != pDisplay)
    {
        pDisplay->draw_graphics_buffer(pDisplay, ENV_STACK);
    }
}
