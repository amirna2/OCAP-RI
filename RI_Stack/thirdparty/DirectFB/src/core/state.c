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

#ifndef _GNU_SOURCE
#define _GNU_SOURCE
#endif

#include "directfb.h"
#include "core.h"
#include "coretypes.h"
#include "state.h"
#include "surfaces.h"

int
dfb_state_init( CardState *state )
{
     DFB_ASSERT( state != NULL );
     
     memset( state, 0, sizeof(CardState) );
     
     state->modified  = SMF_ALL;
     state->src_blend = DSBF_ONE;
     state->dst_blend = DSBF_INVSRCALPHA;
	 state->alpha_const = 0xff;
     state->porter_duff_rule = DSPD_SRC_OVER;

     pthread_mutex_init( &state->lock, PTHREAD_MUTEX_RECURSIVE );
     
     return 0;
}

void
dfb_state_destroy( CardState *state )
{
     DFB_ASSERT( state != NULL );

     pthread_mutex_destroy( &state->lock );
}

void
dfb_state_set_destination( CardState *state, CoreSurface *destination )
{
     DFB_ASSERT( state != NULL );

     dfb_state_lock( state );
     
     if (state->destination != destination) {
          if (state->destination) {
               dfb_surface_detach( state->destination,
                                   &state->destination_reaction );
               dfb_surface_unref( state->destination );
          }

          state->destination  = destination;
          state->modified    |= SMF_DESTINATION;
          
          if (destination) {
               dfb_surface_ref( destination );
               (void) dfb_surface_attach( destination, destination_listener,
                                          state, &state->destination_reaction );
          }
     }
     
     dfb_state_unlock( state );
}

void
dfb_state_set_source( CardState *state, CoreSurface *source )
{
     DFB_ASSERT( state != NULL );

     dfb_state_lock( state );
     
     if (state->source != source) {
          if (state->source) {
               dfb_surface_detach( state->source,
                                   &state->source_reaction );
               dfb_surface_unref( state->source );
          }

          state->source    = source;
          state->modified |= SMF_SOURCE;
          
          if (source) {
               dfb_surface_ref( source );
               (void) dfb_surface_attach( source, source_listener,
                                          state, &state->source_reaction );
          }
     }
     
     dfb_state_unlock( state );
}
