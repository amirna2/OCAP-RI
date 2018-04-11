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
#include "directfb.h"
#include "core/coretypes.h"
#include "core/state.h"
#include "core/gfxcard.h"
#include "core/surfacemanager.h"
#include "util.h"


static int       copy_state_inited = 0;
static CardState copy_state;

static int       btf_state_inited = 0;
static CardState btf_state;

static pthread_mutex_t copy_lock = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t btf_lock = PTHREAD_MUTEX_INITIALIZER;

void dfb_gfx_copy( CoreSurface *source, CoreSurface *destination, DFBRectangle *rect )
{
     pthread_mutex_lock( &copy_lock );

     if (!copy_state_inited) {
          dfb_state_init( &copy_state );
          copy_state_inited = 1;
     }

     copy_state.modified   |= SMF_CLIP | SMF_SOURCE | SMF_DESTINATION;

     copy_state.clip.x1     = 0;
     copy_state.clip.y1     = 0;
     copy_state.clip.x2     = destination->width - 1;
     copy_state.clip.y2     = destination->height - 1;
     copy_state.source      = source;
     copy_state.destination = destination;

     if (rect) {
          dfb_gfxcard_blit( rect, rect->x, rect->y, &copy_state );
     }
     else {
          DFBRectangle sourcerect;
           
          sourcerect.x = 0;
          sourcerect.y = 0;
          sourcerect.w = source->width;
          sourcerect.h = source->height;
          dfb_gfxcard_blit( &sourcerect, 0, 0, &copy_state );
     }

     pthread_mutex_unlock( &copy_lock );
}

void dfb_back_to_front_copy( CoreSurface *surface, DFBRectangle *rect )
{
     SurfaceBuffer *tmp;

     pthread_mutex_lock( &btf_lock );

     if (!btf_state_inited) {
          dfb_state_init( &btf_state );
          btf_state_inited = 1;
     }

     btf_state.modified   |= SMF_CLIP | SMF_SOURCE | SMF_DESTINATION;

     btf_state.clip.x1     = 0;
     btf_state.clip.y1     = 0;
     btf_state.clip.x2     = surface->width - 1;
     btf_state.clip.y2     = surface->height - 1;
     btf_state.source      = surface;
     btf_state.destination = surface;

     dfb_surfacemanager_lock( surface->manager );

     skirmish_prevail( &surface->front_lock );
     skirmish_prevail( &surface->back_lock );

     tmp = surface->front_buffer;
     surface->front_buffer = surface->back_buffer;
     surface->back_buffer = tmp;

     if (rect) {
          dfb_gfxcard_blit( rect, rect->x, rect->y, &btf_state );
     }
     else {
          DFBRectangle sourcerect;
           
          sourcerect.x = 0;
          sourcerect.y = 0;
          sourcerect.w = surface->width;
          sourcerect.h = surface->height;
          dfb_gfxcard_blit( &sourcerect, 0, 0, &btf_state );
     }

     tmp = surface->front_buffer;
     surface->front_buffer = surface->back_buffer;
     surface->back_buffer = tmp;

     skirmish_dismiss( &surface->front_lock );
     skirmish_dismiss( &surface->back_lock );
     
     dfb_surfacemanager_unlock( surface->manager );

     pthread_mutex_unlock( &btf_lock );
}
