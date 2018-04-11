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

/* Header Files */
#include "mfgstk_uievent.h"
#include <stdio.h>
#include <glib.h>
#include <ri_ui_manager.h>

#ifdef WIN32
#include <windows.h> // for Sleep
#endif

// Header files.
#include <string.h>
#include <stdlib.h>

#include "ui_window_common.h"
#include <ri_pipeline_manager.h>
#include <ri_pipeline.h>

static void mfgstk_key_event_cb(ri_event_type type, ri_event_code code);

// GLib does not support mutex locks with timeout, so we will need
// a conditional variable.
static ri_ui_manager_t *ri_ui_manager = NULL;
static inline void test_drawPixel(unsigned char* pGraphics, unsigned char red,
        unsigned char green, unsigned char blue, unsigned char alpha);
static inline void test_drawSquare();
static int counter = 30;
static int mfg_active = 0;
static int counter1 = 250;
static ri_display_t* pDisplay = NULL;
static unsigned char* pucData = NULL;
/**
 * <i>mpeos_initUIEvents</i> initializes a queue, and registerhs it for reception
 * of user input events. This queue will be utilized by the mpeos_gfxWaitNextEvent set 
 * of functions
 */
void mfgstk_initUIEvents(void)
{
    ri_ui_manager = ri_get_ui_manager();
    printf("registering for events in MfgEnv..\n");
    ri_ui_manager->register_key_event_cb_mfg(ri_ui_manager, mfgstk_key_event_cb);
    test_drawSquare();
}

/**
 * The <i>mpeos_gfxWaitNextEvent</i> waits for the system to generate a user input event
 * for a specified length of time. If an event is received in the specified time,
 * the caller's mpe_GfxEvent structure will be filled with the event data.
 *
 * @param event    the caller's event structure to be filled
 * @param timeout  the length of time to wait, in milliseconds
 *
 * @return         MPE_SUCCESS if an event is received, otherwise MPE_ETIMEOUT 
 */

/**
 * Platform key event callback - registered in mpeos_initUIEvents().
 **/
void mfgstk_key_event_cb(ri_event_type type, ri_event_code code)
{
    printf(
            "--- MFG STACK ---> received event code type in MfgEnv is..: %d, code: %d \n",
            type, code);
    if (type == 1)
    {
        if (code == 14)
        {
            mfg_active = 1;
            if (pDisplay != NULL)
                pDisplay->draw_graphics_buffer(pDisplay, ENV_MFG);
        }

        else if (code == 12)
        {
            mfg_active = 0;
        }
        else
        {
            if (mfg_active)
                test_drawSquare();
        }
    }
}

static inline void test_drawSquare()
{
    uint32_t cnt;
    ri_pipeline_t* pPipeline;
    ri_pipeline_t** ppPipeline;

    if (pDisplay == NULL)
    {
        // Get the RI Platform display element from pipeline manager
        ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
        if (NULL != pMgr)
        {
            // Get the live pipelines from pipeline manager
            ppPipeline = (ri_pipeline_t**) pMgr->get_live_pipelines(pMgr, &cnt);
            if (NULL != ppPipeline)
            {
                // Use the first pipeline
                pPipeline = *ppPipeline;
                if (NULL != pPipeline)
                {
                    // Get the display from the pipeline
                    pDisplay = pMgr->get_display(pMgr);

                }
            }
        }

        pDisplay->update_configuration(pDisplay, 640, 480, 1, 1, 640, 480, 1,
                1, 640, 480, 1, 1, ENV_MFG);

        pucData = pDisplay->get_graphics_buffer(pDisplay, ENV_MFG);
    }

    int x;
    int y;

    int bytes_per_pixel = 32 >> 3; // bits/pixel divided by 8 bits/byte
    int i_pitch = bytes_per_pixel * 640;

    // Makes square when PAR is 1/1
    int rect1Width = 100;
    int rect1Height = 100;

    // Calculate the center of the graphics plane
    int centerX = 200;
    int centerY = 100;

    int rect1Xpos = centerX - (rect1Width / 2);
    int rect1Ypos = centerY - (rect1Height / 2);
    // pucData = pDisplay->get_graphics_buffer(pDisplay);
    //printf("pucdata  is..%s", pucData);

    for (x = rect1Xpos; x < rect1Width + rect1Xpos; x++)
    {
        for (y = rect1Ypos; y < rect1Height + rect1Ypos; y++)
        {
            //printf("i_pitch is..%d" i_pitch);
            //printf("pucdata  is..%s", pucData);
            test_drawPixel(&pucData[(x * bytes_per_pixel) + (y * i_pitch)],
                    255, counter, counter1, 255);
        }
    }

    //printf("counter is.. %d\n", counter);
    if (counter <= 150)
        counter = counter + 155;
    else
        counter = 0;

    if (counter1 > 20)
        counter1 = counter1 - 100;
    else
        counter1 = 245;

    if (mfg_active && pDisplay != NULL)
        pDisplay->draw_graphics_buffer(pDisplay, ENV_MFG);

    printf("end of the method in mfgEnv..\n");

}
static inline void test_drawPixel(unsigned char* pGraphics, unsigned char red,
        unsigned char green, unsigned char blue, unsigned char alpha)
{
    *pGraphics++ = red;
    *pGraphics++ = green;
    *pGraphics++ = blue;
    *pGraphics = alpha;
}

