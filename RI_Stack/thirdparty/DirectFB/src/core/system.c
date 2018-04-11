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
#include <external.h>
#include <directfb.h>
#include <core/fusion/list.h>
#include <core/coredefs.h>
#include <core/coretypes.h>
#include <core/core_parts.h>
#include <core/layers.h>
#include <core/palette.h>
#include <core/surfaces.h>
#include <core/system.h>
#include <gfx/convert.h>
#include <misc/conf.h>
#include <misc/mem.h>

DFB_CORE_PART( system, 0, 0 );

#define CORE_SYSTEM_MAIN
#include <core/core_system.h>
static CoreSystemFuncs *system_funcs   = &core_system_funcs;
static CoreSystemInfo   system_info;

DFBResult
dfb_system_lookup()
{
     return DFB_OK;
}

static DFBResult
dfb_system_initialize( void *data_local, void *data_shared )
{
     DFB_ASSERT( system_funcs != NULL );

     DFB_UNUSED_PARAM(data_local);
     DFB_UNUSED_PARAM(data_shared);

     return system_funcs->Initialize();
}

static DFBResult
dfb_system_join( void *data_local, void *data_shared )
{
     DFB_ASSERT( system_funcs != NULL );

     DFB_UNUSED_PARAM(data_local);
     DFB_UNUSED_PARAM(data_shared);

     return system_funcs->Join();
}

static DFBResult
dfb_system_shutdown( bool emergency )
{
     DFB_UNUSED_PARAM(emergency);
     return DFB_OK;
}

static DFBResult
dfb_system_leave( bool emergency )
{
     DFB_UNUSED_PARAM(emergency);
     return DFB_OK;
}

static DFBResult
dfb_system_suspend()
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->Suspend();
}

static DFBResult
dfb_system_resume()
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->Resume();
}

CoreSystemType
dfb_system_type()
{
     return system_info.type;
}

volatile void *
dfb_system_map_mmio( unsigned int    offset,
                     int             length )
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->MapMMIO( offset, length );
}

void
dfb_system_unmap_mmio( volatile void  *addr,
                       int             length )
{
     DFB_ASSERT( system_funcs != NULL );

     system_funcs->UnmapMMIO( addr, length );
}

int
dfb_system_get_accelerator()
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->GetAccelerator();
}

CoreSystemVideomode *
dfb_system_modes()
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->GetModes();
}

CoreSystemVideomode *
dfb_system_current_mode()
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->GetCurrentMode();
}

unsigned long
dfb_system_video_memory_physical( unsigned int offset )
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->VideoMemoryPhysical( offset );
}

void *
dfb_system_video_memory_virtual( unsigned int offset )
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->VideoMemoryVirtual( offset );
}

unsigned int
dfb_system_videoram_length()
{
     DFB_ASSERT( system_funcs != NULL );

     return system_funcs->VideoRamLength();
}

