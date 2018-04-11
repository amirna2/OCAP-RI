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

#ifndef _FRONTPANEL_H__
#define _FRONTPANEL_H__

// Include RI Platform header files.
#include <ri_frontpanel.h>

// Default width of LED in pixels. XXX - need to move to frontpanel.im file.
//#define RI_FP_LED_WIDTH 10
// Default height of LED in pixels. XXX - need to move to frontpanel.im file.
//#define RI_FP_LED_HEIGHT 10

/**
 * Create the front panel.
 *
 * @return A pointer to the Front Panel object is returned.
 */
ri_frontpanel_t *create_frontpanel();

/**
 * Destroy the front panel.
 *
 * @param A pointer to the Front Panel object to destroy.
 */
void destroy_frontpanel(ri_frontpanel_t *);

/**
 * Retrieve the singleton instance of the front panel.
 *
 * @return A pointer to the Front Panel object is returned.
 */
ri_frontpanel_t *get_frontpanel();

/**
 * Create an Indicator LED.
 *
 * @param   name            The name of the LED, reported to and used by
 *                          OCAP/MPE/MPEOS code to access this LED.
 * @param   numBrightnesses The number of brightness levels supported by the
 *                          LED.  Must be at least 1.
 * @param   colors          An or'd list of the available colors for the
 *                          LED.  Must include at least one color.
 * @param   maxCycleRate    The maximum number of times per minute that the
 *                          LED can be made to blink.  Must be >=0, with 0
 *                          meaning that blinking is not supported.
 */
void create_led(const char* name, unsigned int numBrightnesses,
        RI_FP_INDICATOR_COLOR colors, unsigned int maxCycleRate);

/**
 * Destroy a Indicator LED.
 *
 * @param led A pointer to a LED to destroy.
 */
void destroy_led(ri_led_t *led);

/**
 * Create a Text Display LED.
 *
 * @param   name            The name of the Text LED, reported to and used by
 *                          OCAP/MPE/MPEOS code to access this Text LED.
 * @param   numBrightnesses The number of brightness levels supported by the
 *                          Text LED.  Must be at least 1.
 * @param   colors          An or'd list of the available colors for the
 *                          Text LED.  Must include at least one color.
 * @param   maxCycleRate    The maximum number of times per minute that the
 *                          Text LED can be made to blink.  Must be >=0, with 0
 *                          meaning that blinking is not supported.
 */
void create_textled(const char* name, unsigned int numBrightnesses,
        RI_FP_INDICATOR_COLOR colors, unsigned int maxCycleRate,
        unsigned int maxHorizScrollsPerMinute);

/**
 * Destroy a Text Display LED.
 *
 * @param led A pointer to a Text LED to destroy.
 */
void destroy_textled(ri_textled_t *led);

#endif /* _FRONTPANEL_H__ */
