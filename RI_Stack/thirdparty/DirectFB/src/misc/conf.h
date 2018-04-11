/*
   (c) Copyright 2000-2002  convergence integrated media GmbH.
   (c) Copyright 2002       convergence GmbH.
   
   All rights reserved.

   Written by Denis Oliver Kropp <dok@directfb.org>,
              Andreas Hundt <andi@fischlustig.de> and
              Sven Neumann <sven@convergence.de>.

   This library is free software; you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2 of the License, or (at your option) any later version.

   This library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this library; if not, write to the
   Free Software Foundation, Inc., 59 Temple Place - Suite 330,
   Boston, MA 02111-1307, USA.
*/

#ifndef __CONF_H__
#define __CONF_H__

#include <external.h>
#include <directfb.h>
#include <core/fusion/fusion_types.h>

typedef struct
{
     int       buffer_mode;                       /* default buffer mode for
                                                     primary layer */

     bool      software_only;                     /* disable hardware
                                                     acceleration */

     bool      mmx;                               /* mmx support */

     bool      banner;                            /* startup banner */
     bool      quiet;                             /* no output at all
                                                     except debugging */

     bool      debug;                             /* debug output */

     bool      deinit_check;

     bool      argb_font;                         /* whether to load fontmap
                                                     as argb and not a8 */

     struct {
          int                   width;            /* primary layer width */
          int                   height;           /* primary layer height */
          int                   depth;            /* primary layer depth */
          DFBSurfacePixelFormat format;           /* primary layer format */
     } mode;

     int       videoram_limit;                    /* limit amount of video
                                                     memory used by DirectFB */
} DFBConfig;

extern DFBConfig *dfb_config;

/*
 * Allocate Config struct, fill with defaults and parse command line options
 * for overrides. Options identified as DirectFB options are stripped out
 * of the array.
 */
DFBResult dfb_config_init( int *argc, char **argv[] );

/*
 * Set indiviual option. Used by config_init(), config_read() and
 * DirectFBSetOption()
 */
DFBResult dfb_config_set( const char *name, const char *value );

const char *dfb_config_usage( void );

#endif

