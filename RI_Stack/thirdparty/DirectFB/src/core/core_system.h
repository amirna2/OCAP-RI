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

#ifndef __DFB__CORE__CORE_SYSTEM_H__
#define __DFB__CORE__CORE_SYSTEM_H__

#include <core/system.h>

#define static		// These functions are compiled into the DirectFB image
					// instead of being in a seperate module (driver)

static void
system_get_info( CoreSystemInfo *info );

static DFBResult
system_initialize(void);

static DFBResult
system_join(void);

static DFBResult
system_shutdown( bool emergency );

static DFBResult
system_leave( bool emergency );

static DFBResult
system_suspend(void);

static DFBResult
system_resume(void);

static CoreSystemVideomode*
system_get_modes(void);

static CoreSystemVideomode*
system_get_current_mode(void);

static volatile void*
system_map_mmio( unsigned int    offset,
                 int             length );

static void
system_unmap_mmio( volatile void  *addr,
                   int             length );
     
static int
system_get_accelerator(void);
     
static unsigned long
system_video_memory_physical( unsigned int offset );

static void*
system_video_memory_virtual( unsigned int offset );
     
static unsigned int
system_videoram_length(void);

#undef static		// Restore normal semantics for "static"

#ifdef CORE_SYSTEM_MAIN
/*
 * Declare empty structure initially, and in mpe.../cs_funcs_populate
 * This is just the extern. The actual one must be put in a file where this
 * include file doesn't occur....src/directfb.c .
 */
extern CoreSystemFuncs core_system_funcs;
/*
static CoreSystemFuncs core_system_funcs = {
     system_get_info,
     system_initialize,
     system_join,
     system_shutdown,
     system_leave,
     system_suspend,
     system_resume,
     system_get_modes,
     system_get_current_mode,
     system_map_mmio,
     system_unmap_mmio,
     system_get_accelerator,
     system_video_memory_physical,
     system_video_memory_virtual,
     system_videoram_length
};
*/
#endif

#endif
