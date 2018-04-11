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
#include <config.h>

#include <stdio.h>

#include <directfb.h>
                                   
#include <core/coredefs.h>
#include <core/coretypes.h>
#include <core/layers.h>
#include <core/palette.h>
#include <core/surfaces.h>
#include <core/system.h>

#include <gfx/convert.h>

#include <misc/conf.h>

#include "wfb.h"

#include "primary.h"

#include <core/core_system.h>


void
system_get_info( CoreSystemInfo *info )
{
     info->type = CORE_WIN32FB;

     snprintf( info->name, DFB_CORE_SYSTEM_INFO_NAME_LENGTH, "Win32FB" );
}

DFBResult
system_initialize()
{
     WFB_Result result;

     // Initialize graphics sub-system (driver)
     if ((result = WFB_Init()) != WFB_SUCCESS) {
          ERRORMSG("DirectFB: Couldn't initialize WFB: %s\n", result);
          return DFB_INIT;
     }
     
     // Register the functions for operating on the primary layer
     return dfb_layers_register( NULL, NULL, &win32fbPrimaryLayerFuncs );
}

DFBResult
system_join()
{
     return DFB_UNSUPPORTED;
}

DFBResult
system_shutdown( bool emergency )
{
     DFB_UNUSED_PARAM(emergency);

     WFB_Term();

     return DFB_OK;
}

DFBResult
system_leave( bool emergency )
{
     DFB_UNUSED_PARAM(emergency);

     return DFB_UNSUPPORTED;
}

DFBResult
system_suspend()
{
     return DFB_UNSUPPORTED;
}

DFBResult
system_resume()
{
     return DFB_UNSUPPORTED;
}

volatile void *
system_map_mmio( unsigned int    offset,
                 int             length )
{
    DFB_UNUSED_PARAM(offset);
    DFB_UNUSED_PARAM(length);
    return NULL;
}

void
system_unmap_mmio( volatile void  *addr,
                   int             length )
{
     DFB_UNUSED_PARAM(addr);
     DFB_UNUSED_PARAM(length);
     return;
}

int
system_get_accelerator()
{
     return -1;
}

CoreSystemVideomode *
system_get_modes()
{
     return NULL;
}

CoreSystemVideomode *
system_get_current_mode()
{
     return NULL;
}

unsigned long
system_video_memory_physical( unsigned int offset )
{
     DFB_UNUSED_PARAM(offset);
     return 0;
}

void *
system_video_memory_virtual( unsigned int offset )
{
     DFB_UNUSED_PARAM(offset);
     return NULL;
}

unsigned int
system_videoram_length()
{
     return 0;
}
