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
#include <core/fusion/shmalloc.h>
#include <core/fusion/arena.h>
#include <directfb.h>
#include <core/core.h>
#include <core/coredefs.h>
#include <core/coretypes.h>
#include <core/core_parts.h>
#include <core/gfxcard.h>
#include <core/fonts.h>
#include <core/state.h>
#include <core/palette.h>
#include <core/surfaces.h>
#include <core/surfacemanager.h>
#include <core/system.h>
#include <gfx/generic/generic.h>
#include <gfx/clip.h>
#include <gfx/util.h>
#include <misc/utf8.h>
#include <misc/mem.h>
#include <misc/util.h>
#include <ocap/extensions.h>

/*
 * struct for graphics cards
 */
typedef struct {
     /* amount of usable video memory */
     unsigned int          videoram_length;

     GraphicsDriverInfo    driver_info;
     GraphicsDeviceInfo    device_info;
     void                 *device_data;

     FusionSkirmish        lock;

     SurfaceManager       *surface_manager;

     FusionObjectPool     *surface_pool;
     FusionObjectPool     *palette_pool;

     /*
      * Points to the current state of the graphics card.
      */
     CardState            *state;
} GraphicsDeviceShared;

struct _GraphicsDevice {
     GraphicsDeviceShared      *shared;

     const GraphicsDriverFuncs *driver_funcs;

     void                      *driver_data;
     void                      *device_data; /* copy of shared->device_data */

     GraphicsDeviceFuncs        funcs;
};

/* Pointer to function to setup Graphics Driver Funcs */
void (*getgraphicsdriverfuncs)(GraphicsDriverFuncs *gd_funcs);

static GraphicsDevice *card = NULL;


static void dfb_gfxcard_find_driver(void);

DFB_CORE_PART( gfxcard, sizeof(GraphicsDevice), sizeof(GraphicsDeviceShared) );


/** public **/

static DFBResult
dfb_gfxcard_initialize( void *data_local, void *data_shared )
{
     DFBResult    ret;
     unsigned int videoram_length;

     DFB_ASSERT( card == NULL );

     card         = data_local;
     card->shared = data_shared;

     /* fill generic driver info */
     gGetDriverInfo( &card->shared->driver_info );

     /* fill generic device info */
     gGetDeviceInfo( &card->shared->device_info );

     /* Limit video ram length */
     videoram_length = dfb_system_videoram_length();
     if (videoram_length) {
          if (dfb_config->videoram_limit > 0 &&
              dfb_config->videoram_limit < (int)videoram_length)
               card->shared->videoram_length = dfb_config->videoram_limit;
          else
               card->shared->videoram_length = videoram_length;
     }

     /* Load driver */
     dfb_gfxcard_find_driver();
     if (card->driver_funcs) {
          const GraphicsDriverFuncs *funcs = card->driver_funcs;
          
          if ((card->driver_data =
              DFBCALLOC( 1, card->shared->driver_info.driver_data_size )) == NULL)
              return DFB_NOSYSTEMMEMORY;

          ret = funcs->InitDriver( card, &card->funcs, card->driver_data );
          if (ret) {
               DFBFREE( card->driver_data );
               card = NULL;
               return ret;
          }

          if ((card->shared->device_data =
               shcalloc( 1, card->shared->driver_info.device_data_size )) == NULL)
                   return DFB_NOSYSTEMMEMORY;

          ret = funcs->InitDevice( card, &card->shared->device_info,
                                   card->driver_data, card->shared->device_data );
          if (ret) {
               funcs->CloseDriver( card, card->driver_data );
               shfree( card->shared->device_data );
               DFBFREE( card->driver_data );
               card = NULL;
               return ret;
          }

          card->device_data = card->shared->device_data;
     }

     INITMSG( "DirectFB/GraphicsDevice: %s %s %d.%d (%s)\n",
              card->shared->device_info.vendor, card->shared->device_info.name,
              card->shared->driver_info.version.major,
              card->shared->driver_info.version.minor, card->shared->driver_info.vendor );

     if (dfb_config->software_only) {
          memset( &card->shared->device_info.caps, 0, sizeof(CardCapabilities) );

          if (card->funcs.CheckState) {
               card->funcs.CheckState = NULL;
               
               INITMSG( "DirectFB/GraphicsDevice: "
                        "acceleration disabled (by 'no-hardware')\n" );
          }
     }
     
     card->shared->surface_manager = dfb_surfacemanager_create( card->shared->videoram_length,
                card->shared->device_info.limits.surface_byteoffset_alignment,
                card->shared->device_info.limits.surface_pixelpitch_alignment );

     card->shared->palette_pool = dfb_palette_pool_create();
     card->shared->surface_pool = dfb_surface_pool_create();

     skirmish_init( &card->shared->lock );

     return DFB_OK;
}

static DFBResult
dfb_gfxcard_join( void *data_local, void *data_shared )
{
     DFB_ASSERT( card == NULL );
     
     card         = data_local;
     card->shared = data_shared;
     
     if (dfb_config->software_only && card->funcs.CheckState) {
          card->funcs.CheckState = NULL;
          
          INITMSG( "DirectFB/GraphicsDevice: "
                   "acceleration disabled (by 'no-hardware')\n" );
     }
     
     return DFB_OK;
}

static DFBResult
dfb_gfxcard_shutdown( bool emergency )
{
     DFB_ASSERT( card != NULL );

     DFB_UNUSED_PARAM(emergency);

     dfb_gfxcard_lock();
     dfb_gfxcard_sync();

     if (card->driver_funcs) {
          const GraphicsDriverFuncs *funcs = card->driver_funcs;
          
          funcs->CloseDevice( card, card->driver_data, card->device_data );
          funcs->CloseDriver( card, card->driver_data );

          shfree( card->device_data );
          DFBFREE( card->driver_data );
     }

     dfb_surface_pool_destroy( card->shared->surface_pool );
     dfb_palette_pool_destroy( card->shared->palette_pool );

     dfb_surfacemanager_destroy( card->shared->surface_manager );

     skirmish_destroy( &card->shared->lock );

     card = NULL;

     return DFB_OK;
}

static DFBResult
dfb_gfxcard_leave( bool emergency )
{
     DFB_ASSERT( card != NULL );

     DFB_UNUSED_PARAM(emergency);

     dfb_gfxcard_sync();

     if (card->driver_funcs) {
          card->driver_funcs->CloseDriver( card, card->driver_data );

          DFBFREE( card->driver_data );
     }

     card = NULL;
     return DFB_OK;
}

static DFBResult
dfb_gfxcard_suspend()
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );

     dfb_gfxcard_sync();

     return dfb_surfacemanager_suspend( card->shared->surface_manager );
}

static DFBResult
dfb_gfxcard_resume()
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );

     return dfb_surfacemanager_resume( card->shared->surface_manager );
}

void
dfb_gfxcard_lock()
{
     if (card && card->shared)
          skirmish_prevail( &card->shared->lock );
}

void
dfb_gfxcard_unlock( bool invalidate_state )
{
     if (card && card->shared) {
          if (invalidate_state)
               card->shared->state = NULL;

          skirmish_dismiss( &card->shared->lock );
     }
}

/*
 * This function returns non zero if acceleration is available
 * for the specific function using the given state.
 */
bool
dfb_gfxcard_state_check( CardState *state, DFBAccelerationMask accel )
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( state != NULL );

     /*
      * If there's no CheckState function there's no acceleration at all.
      */
     if (!card->funcs.CheckState)
          return 0;

     /* Debug checks */
#ifdef DFB_DEBUG
     if (!state->destination) {
          BUG("state check: no destination");
          return 0;
     }
     if (!state->source  &&  DFB_BLITTING_FUNCTION( accel )) {
          BUG("state check: no source");
          return 0;
     }
#endif

     /*
      * If back_buffer policy is 'system only' there's no acceleration
      * available.
      */
     if (state->destination->back_buffer->policy == CSP_SYSTEMONLY) {
          /* Clear 'accelerated functions'. */
          state->accel = 0;

          /* Return immediately. */
          return 0;
     }

     /*
      * If the front buffer policy of the source is 'system only'
      * no accelerated blitting is available.
      */
     if (state->source &&
         state->source->front_buffer->policy == CSP_SYSTEMONLY)
     {
          /* Clear 'accelerated blitting functions'. */
          state->accel &= 0x0000FFFF;

          /* Return if a blitting function was requested. */
          if (DFB_BLITTING_FUNCTION( accel ))
               return 0;
     }

     /* If destination or blend functions have been changed... */
     if (state->modified & (SMF_DESTINATION | SMF_SRC_BLEND | SMF_DST_BLEND)) {
          /* ...force rechecking for all functions. */
          state->checked = 0;
     }
     else {
          /* If source or blitting flags have been changed... */
          if (state->modified & (SMF_SOURCE | SMF_BLITTING_FLAGS)) {
               /* ...force rechecking for all blitting functions. */
               state->checked &= 0x0000FFFF;
          }

          /* If drawing flags have been changed... */
          if (state->modified & SMF_DRAWING_FLAGS) {
               /* ...force rechecking for all drawing functions. */
               state->checked &= 0xFFFF0000;
          }
     }

     /* If the function needs to be checked... */
     if (!(state->checked & accel)) {
          /* Unset function bit. */
          state->accel &= ~accel;

          /* Call driver to (re)set the bit if the function is supported. */
          card->funcs.CheckState( card->driver_data,
                                  card->device_data, state, accel );

          /* Add the function to 'checked functions'. */
          state->checked |= accel;
     }

     /* Return whether the function bit is set. */
     return (state->accel & accel);
}

/*
 * This function returns non zero after successful locking the surface(s)
 * for access by hardware. Propagate state changes to driver.
 */
static bool
dfb_gfxcard_state_acquire( CardState *state, DFBAccelerationMask accel )
{
     DFBSurfaceLockFlags lock_flags;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     
     /* Debug checks */
#ifdef DFB_DEBUG
     if (!state->destination) {
          BUG("state check: no destination");
          return 0;
     }
     if (!state->source  &&  DFB_BLITTING_FUNCTION( accel )) {
          BUG("state check: no source");
          return 0;
     }
#endif

     /* find locking flags */
     if (DFB_BLITTING_FUNCTION( accel ))
          lock_flags = (state->blittingflags & ( DSBLIT_BLEND_ALPHACHANNEL |
                                                 DSBLIT_BLEND_COLORALPHA   |
                                                 DSBLIT_DST_COLORKEY ) ?
                        DSLF_READ | DSLF_WRITE : DSLF_WRITE) | CSLF_FORCE;
     else
          lock_flags = state->drawingflags & ( DSDRAW_BLEND |
                                               DSDRAW_DST_COLORKEY ) ?
                       DSLF_READ | DSLF_WRITE : DSLF_WRITE;

     /* lock surface manager */
     dfb_surfacemanager_lock( card->shared->surface_manager );

     /* if blitting... */
     if (DFB_BLITTING_FUNCTION( accel )) {
          /* ...lock source for reading */
          if (dfb_surface_hardware_lock( state->source, DSLF_READ, 1 )) {
               dfb_surfacemanager_unlock( card->shared->surface_manager );
               return 0;
          }

          state->source_locked = 1;
     }
     else
          state->source_locked = 0;

     /* lock destination */
     if (dfb_surface_hardware_lock( state->destination, lock_flags, 0 )) {
          if (state->source_locked)
               dfb_surface_unlock( state->source, 1 );

          dfb_surfacemanager_unlock( card->shared->surface_manager );
          return 0;
     }

     /* unlock surface manager */
     dfb_surfacemanager_unlock( card->shared->surface_manager );

     /*
      * Make sure that state setting with subsequent command execution
      * isn't done by two processes simultaneously.
      */
     if (skirmish_prevail( &card->shared->lock ))
          return 0;

     /* if we are switching to another state... */
     if (state != card->shared->state) {
          /* ...set all modification bits and clear 'set functions' */
          state->modified |= SMF_ALL;
          state->set       = 0;

          card->shared->state = state;
     }

     /*
      * If function hasn't been set or state is modified
      * call the driver function to propagate the state changes.
      */
     if (!(state->set & accel) || state->modified)
          card->funcs.SetState( card->driver_data, card->device_data,
                                &card->funcs, state, accel );

     return 1;
}

/*
 * Unlock destination and possibly the source.
 */
static void
dfb_gfxcard_state_release( CardState *state )
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     
     /* destination always gets locked during acquisition */
     dfb_surface_unlock( state->destination, 0 );

     /* if source got locked this value is true */
     if (state->source_locked)
          dfb_surface_unlock( state->source, 1 );

     /* allow others to use the hardware */
     skirmish_dismiss( &card->shared->lock );
}

/** SURFACE FUNCTIONS **/

bool dfb_gfxcard_createsurface( unsigned long *surface_context, void **surface_address, long *surface_pitch, DFBSurfacePixelFormat pixel_format, int w, int h )
{
	DFB_ASSERT( card != NULL );
	DFB_ASSERT( card->shared != NULL );

	DFB_ASSERT( surface_context != NULL );
	DFB_ASSERT( surface_address != NULL );
	DFB_ASSERT( surface_pitch != NULL );

	if ((surface_context != NULL) && (surface_address != NULL) && (surface_pitch != NULL))
	{
		if (card->funcs.CreateSurface(card->driver_data, card->device_data, surface_context, surface_address, surface_pitch, pixel_format, w, h))
		{
			return true;
		}
	}

	return false;
}

bool dfb_gfxcard_deletesurface( unsigned long surface_context )
{
	DFB_ASSERT( card != NULL );
	DFB_ASSERT( card->shared != NULL );

	if (card->funcs.DeleteSurface(card->driver_data, card->device_data, surface_context))
	{
		return true;
	}

	return false;
}

/** DRAWING FUNCTIONS **/

void dfb_gfxcard_fillrectangle( DFBRectangle *rect, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );

     /* The state is locked during graphics operations. */
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
         dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
     {
          /*
           * Either hardware has clipping support or the software clipping
           * routine returned that there's something to do.
           */
          if ((card->shared->device_info.caps.flags & CCF_CLIPPING) ||
              dfb_clip_rectangle( &state->clip, rect ))
          {
               /*
                * Now everything is prepared for execution of the
                * FillRectangle driver function.
                */
               hw = card->funcs.FillRectangle( card->driver_data,
                                               card->device_data, rect );
          }

          /* Release after state acquisition. */
          dfb_gfxcard_state_release( state );
     }

     if (!hw) {
          /*
           * Otherwise use the software clipping routine and execute the
           * software fallback if the rectangle isn't completely clipped.
           */
          if (dfb_clip_rectangle( &state->clip, rect ) &&
              gAquire( state, DFXL_FILLRECTANGLE ))
          {
               gFillRectangle( rect );
               gRelease( state );
          }
     }

     /* Unlock after execution. */
     dfb_state_unlock( state );
}

/*
 * This was extracted from the latest version of DirectFB (DirectFB-0.9.24).
 * Note that there an enumerated type was added for the edges and that the
 * values of those edges differ from those used here.
 */
static void
build_clipped_rectangle_outlines( DFBRectangle    *rect,
                                  const DFBRegion *clip,
                                  DFBRectangle    *ret_outlines,
                                  int             *ret_num )
{
     /* Enumeration made to match what is currently returned from dfb_clip_rectangle() */
     enum { DFEF_NONE   = 0,
            DFEF_LEFT   = 1,
            DFEF_TOP    = 2,
            DFEF_RIGHT  = 4,
            DFEF_BOTTOM = 8 };
     unsigned int edges = dfb_clip_rectangle( clip, rect );
     int          t     = (edges & DFEF_TOP ? 1 : 0);
     int          tb    = t + (edges & DFEF_BOTTOM ? 1 : 0);
     int          num   = 0;

     DFB_ASSERT( rect != NULL );
     DFB_ASSERT( rect->w >= 0 );
     DFB_ASSERT( rect->h >= 0 );

     DFB_ASSERT( ret_outlines != NULL );
     DFB_ASSERT( ret_num != NULL );

     if (edges & DFEF_TOP) {
          DFBRectangle *out = &ret_outlines[num++];

          out->x = rect->x;
          out->y = rect->y;
          out->w = rect->w;
          out->h = 1;
     }

     if (rect->h > t) {
          if (edges & DFEF_BOTTOM) {
               DFBRectangle *out = &ret_outlines[num++];

               out->x = rect->x;
               out->y = rect->y + rect->h - 1;
               out->w = rect->w;
               out->h = 1;
          }

          if (rect->h > tb) {
               if (edges & DFEF_LEFT) {
                    DFBRectangle *out = &ret_outlines[num++];

                    out->x = rect->x;
                    out->y = rect->y + t;
                    out->w = 1;
                    out->h = rect->h - tb;
               }

               if (rect->w > 1 || !(edges & DFEF_LEFT)) {
                    if (edges & DFEF_RIGHT) {
                         DFBRectangle *out = &ret_outlines[num++];

                         out->x = rect->x + rect->w - 1;
                         out->y = rect->y + t;
                         out->w = 1;
                         out->h = rect->h - tb;
                    }
               }
          }
     }

     *ret_num = num;
}


void dfb_gfxcard_drawrectangle( DFBRectangle *rect, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     
     dfb_state_lock( state );

     if (dfb_gfxcard_state_check( state, DFXL_DRAWRECTANGLE ) &&
         dfb_gfxcard_state_acquire( state, DFXL_DRAWRECTANGLE ))
     {
          if (card->shared->device_info.caps.flags & CCF_CLIPPING  ||
              dfb_clip_rectangle( &state->clip, rect ))
          {
               /* FIXME: correct clipping like below */
               hw = card->funcs.DrawRectangle( card->driver_data,
                                               card->device_data, rect );
          }

          dfb_gfxcard_state_release( state );
     }
     
     if ( !hw && gAquire(state, DFXL_FILLRECTANGLE) )
     {
          DFBRectangle rects[4];
          int i;
          int num = 0;

          build_clipped_rectangle_outlines(rect, &state->clip, rects, &num);

          for (i = 0; i<num; i++)
               gFillRectangle( &rects[i] );

                    gRelease (state);
               }

     dfb_state_unlock( state );
}

void dfb_gfxcard_drawlines( DFBRegion *lines, int num_lines, CardState *state )
{
     int i;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( lines != NULL );
     DFB_ASSERT( num_lines > 0 );
     
     dfb_state_lock( state );

     if (dfb_gfxcard_state_check( state, DFXL_DRAWLINE ) &&
         dfb_gfxcard_state_acquire( state, DFXL_DRAWLINE ))
     {
          if (card->shared->device_info.caps.flags & CCF_CLIPPING)
               for (i=0; i<num_lines; i++)
                    card->funcs.DrawLine( card->driver_data,
                                          card->device_data, &lines[i] );
          else
               for (i=0; i<num_lines; i++) {
                    if (dfb_clip_line( &state->clip, &lines[i] ))
                         card->funcs.DrawLine( card->driver_data,
                                               card->device_data, &lines[i] );
               }

          dfb_gfxcard_state_release( state );
     }
     else {
          if (gAquire( state, DFXL_DRAWLINE )) {
               for (i=0; i<num_lines; i++) {
                    if (dfb_clip_line( &state->clip, &lines[i] ))
                         gDrawLine( &lines[i] );
               }

               gRelease( state );
          }
     }

     dfb_state_unlock( state );
}

#if 0

static
void fastDrawPixel(SCSTATE *scs, int x, int y)
{
     DFBRectangle rect;
      
     rect.x = x;
     rect.y = y;
     rect.w = 1;
     rect.h = 1;
     card->funcs.FillRectangle( card->driver_data, card->device_data, &rect );
}
#endif

#if 0

static
void fastDrawHorizLine(SCSTATE *scs, int x1, int x2, int y)
{
     DFBRectangle rect;
      
     rect.x = x1;
     rect.y = y;
     rect.w = x2 - x1 + 1;
     rect.h = 1;
     card->funcs.FillRectangle( card->driver_data, card->device_data, &rect );
}
#endif

static
void slowDrawPixel(SCSTATE *scs, int x, int y)
{
     DFBRectangle rect;
      
     rect.x = x;
     rect.y = y;
     rect.w = 1; 
     rect.h = 1;
     if (dfb_clip_rectangle( &scs->state->clip, &rect ))
          gFillSegment (&rect);
}

static
void slowDrawHorizLine(SCSTATE *scs, int x1, int x2, int y)
{
     DFBRectangle rect;
      
     rect.x = x1;
     rect.y = y;
     rect.w = x2 - x1 + 1;
     rect.h = 1;
     if (dfb_clip_rectangle( &scs->state->clip, &rect ))
          gFillSegment (&rect);
}

void dfb_gfxcard_fillroundrect( DFBRectangle *rect, DFBDimension *oval, CardState *state )
{    
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     DFB_ASSERT( oval != NULL );
     
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_FILLROUNDRECT ) &&
         dfb_gfxcard_state_acquire( state, DFXL_FILLROUNDRECT ))
     {
          /* FIXME: need to check the driver's CCF_CLIPPING flag, and execute the software       */
          /* fallback if clipping is required, but not supported, by the driver                  */

          hw = card->funcs.FillRoundRect( card->driver_data,
                                          card->device_data, rect, oval );
          dfb_gfxcard_state_release( state );
     }
#if 0
     /* The hardware-accelerated DRXL_FILLRECTANGLE case doesn't work, as written.          */
	 /*                                                                                     */
	 /* See dfb_gfxcard_fillrectangle() for the preparation that must be done around before */
	 /* and after calling the driver's FillRectangle().                                     */
	 /*                                                                                     */
	 /*   1st) clipping must be done in software if the driver doesn't support it (i.e.     */
	 /*        check the driver's CCF_CLIPPING flag and call dfb_clip_rectangle if not set) */
	 /*   2nd) the software fallback must be performed on a per-rectangle basis (i.e. check */
	 /*        return from the driver's FillRectangle() and draw in software if not done in */
	 /*        hardware)                                                                    */
	 
     else {
          /* try hardware accelerated rectangle filling */
          if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
              dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, fastDrawPixel, fastDrawHorizLine };
               gRoundRect(&scs, rect, oval, true);
               dfb_gfxcard_state_release( state );
          }
          else if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, slowDrawPixel, slowDrawHorizLine };
               gRoundRect(&scs, rect, oval, true);
               gRelease( state );
          }
     }
#else
     if (!hw)
	 {
          /* use software fallback (clipping is done per rect inside the slowDraw*() routines)   */
          if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = {0};
               
               scs.state = state;
               scs.drawPixel = slowDrawPixel;
               scs.drawHorizLine = slowDrawHorizLine;
               gRoundRect(&scs, rect, oval, true);
               gRelease( state );
          }
	 }
#endif

     dfb_state_unlock( state );
}

void dfb_gfxcard_drawroundrect( DFBRectangle *rect, DFBDimension *oval, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     DFB_ASSERT( oval != NULL );
     
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_DRAWROUNDRECT ) &&
         dfb_gfxcard_state_acquire( state, DFXL_DRAWROUNDRECT ))
     {
          /* FIXME: need to check the driver's CCF_CLIPPING flag, and execute the software       */
          /* fallback if clipping is required, but not supported, by the driver                  */

          hw = card->funcs.DrawRoundRect( card->driver_data,
                                          card->device_data, rect, oval );
          dfb_gfxcard_state_release( state );
     }
#if 0
     /* The hardware-accelerated DRXL_FILLRECTANGLE case doesn't work, as written.          */
	 /*                                                                                     */
	 /* See dfb_gfxcard_fillrectangle() for the preparation that must be done around before */
	 /* and after calling the driver's FillRectangle().                                     */
	 /*                                                                                     */
	 /*   1st) clipping must be done in software if the driver doesn't support it (i.e.     */
	 /*        check the driver's CCF_CLIPPING flag and call dfb_clip_rectangle if not set) */
	 /*   2nd) the software fallback must be performed on a per-rectangle basis (i.e. check */
	 /*        return from the driver's FillRectangle() and draw in software if not done in */
	 /*        hardware)                                                                    */
	 
     else {
          /* try hardware accelerated rectangle filling */
          if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
              dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, fastDrawPixel, fastDrawHorizLine };
               gRoundRect(&scs, rect, oval, false);
               dfb_gfxcard_state_release( state );
          }
          else if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, slowDrawPixel, slowDrawHorizLine };
               gRoundRect(&scs, rect, oval, false);
               gRelease( state );
          }
     }
#else
     if (!hw)
	 {
          /* use software fallback (clipping is done per rect inside the slowDraw*() routines)   */
          if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
              SCSTATE scs = {0};

              scs.state = state;
              scs.drawPixel = slowDrawPixel;
              scs.drawHorizLine = slowDrawHorizLine;
              gRoundRect(&scs, rect, oval, false);
              gRelease( state );
          }
	 }
#endif

     dfb_state_unlock( state );
}

void dfb_gfxcard_fillOval( DFBRectangle *rect, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_FILLOVAL ) &&
         dfb_gfxcard_state_acquire( state, DFXL_FILLOVAL ))
     {
          /* FIXME: need to check the driver's CCF_CLIPPING flag, and execute the software       */
          /* fallback if clipping is required, but not supported, by the driver                  */

          hw = card->funcs.FillOval( card->driver_data,
                                     card->device_data, rect );
          dfb_gfxcard_state_release( state );
     }
#if 0
     /* The hardware-accelerated DRXL_FILLRECTANGLE case doesn't work, as written.          */
	 /*                                                                                     */
	 /* See dfb_gfxcard_fillrectangle() for the preparation that must be done around before */
	 /* and after calling the driver's FillRectangle().                                     */
	 /*                                                                                     */
	 /*   1st) clipping must be done in software if the driver doesn't support it (i.e.     */
	 /*        check the driver's CCF_CLIPPING flag and call dfb_clip_rectangle if not set) */
	 /*   2nd) the software fallback must be performed on a per-rectangle basis (i.e. check */
	 /*        return from the driver's FillRectangle() and draw in software if not done in */
	 /*        hardware)                                                                    */
	 
     else {
          /* try hardware accelerated rectangle filling */
          if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
              dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, fastDrawPixel, fastDrawHorizLine };
               gOval(&scs, rect, true);
               dfb_gfxcard_state_release( state );
          }
          else if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, slowDrawPixel, slowDrawHorizLine };
               gOval(&scs, rect, true);
               gRelease( state );
          }
     }
#else
     if (!hw)
	 {
          /* use software fallback (clipping is done per rect inside the slowDraw*() routines)   */
          if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
              SCSTATE scs = {0};

              scs.state = state;
              scs.drawPixel = slowDrawPixel;
              scs.drawHorizLine = slowDrawHorizLine;
              gOval(&scs, rect, true);
              gRelease( state );
          }
	 }
#endif

     dfb_state_unlock( state );
}

void dfb_gfxcard_drawOval( DFBRectangle *rect, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_DRAWOVAL ) &&
         dfb_gfxcard_state_acquire( state, DFXL_DRAWOVAL ))
     {
          /* FIXME: need to check the driver's CCF_CLIPPING flag, and execute the software       */
          /* fallback if clipping is required, but not supported, by the driver                  */

          hw = card->funcs.DrawOval( card->driver_data,
                                     card->device_data, rect );
          dfb_gfxcard_state_release( state );
     }
#if 0
     /* The hardware-accelerated DRXL_FILLRECTANGLE case doesn't work, as written.          */
	 /*                                                                                     */
	 /* See dfb_gfxcard_fillrectangle() for the preparation that must be done around before */
	 /* and after calling the driver's FillRectangle().                                     */
	 /*                                                                                     */
	 /*   1st) clipping must be done in software if the driver doesn't support it (i.e.     */
	 /*        check the driver's CCF_CLIPPING flag and call dfb_clip_rectangle if not set) */
	 /*   2nd) the software fallback must be performed on a per-rectangle basis (i.e. check */
	 /*        return from the driver's FillRectangle() and draw in software if not done in */
	 /*        hardware)                                                                    */
	 
     else {
          /* try hardware accelerated rectangle filling */
          if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
              dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, fastDrawPixel, fastDrawHorizLine };
               gOval(&scs, rect, false);
               dfb_gfxcard_state_release( state );
          }
          else if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, slowDrawPixel, slowDrawHorizLine };
               gOval(&scs, rect, false);
               gRelease( state );
          }
     }
#else
     if (!hw)
	 {
          /* use software fallback (clipping is done per rect inside the slowDraw*() routines)   */
          if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = {0};

               scs.state = state;
               scs.drawPixel = slowDrawPixel;
               scs.drawHorizLine = slowDrawHorizLine;
               gOval(&scs, rect, false);
               gRelease( state );
          }
	 }
#endif

     dfb_state_unlock( state );
}

void dfb_gfxcard_fillArc( DFBRectangle *rect, int startAngle, int arcAngle, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_FILLARC ) &&
         dfb_gfxcard_state_acquire( state, DFXL_FILLARC ))
     {
          /* FIXME: need to check the driver's CCF_CLIPPING flag, and execute the software       */
          /* fallback if clipping is required, but not supported, by the driver                  */

          hw = card->funcs.FillArc( card->driver_data,
                                    card->device_data, rect, startAngle, arcAngle );
          dfb_gfxcard_state_release( state );
     }
#if 0
     /* The hardware-accelerated DRXL_FILLRECTANGLE case doesn't work, as written.          */
	 /*                                                                                     */
	 /* See dfb_gfxcard_fillrectangle() for the preparation that must be done around before */
	 /* and after calling the driver's FillRectangle().                                     */
	 /*                                                                                     */
	 /*   1st) clipping must be done in software if the driver doesn't support it (i.e.     */
	 /*        check the driver's CCF_CLIPPING flag and call dfb_clip_rectangle if not set) */
	 /*   2nd) the software fallback must be performed on a per-rectangle basis (i.e. check */
	 /*        return from the driver's FillRectangle() and draw in software if not done in */
	 /*        hardware)                                                                    */
	 
     else {
          /* try hardware accelerated rectangle filling */
          if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
              dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, fastDrawPixel, fastDrawHorizLine };
               gArc(&scs, rect, startAngle, arcAngle, true);
               dfb_gfxcard_state_release( state );
          }
          else if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, slowDrawPixel, slowDrawHorizLine };
               gArc(&scs, rect, startAngle, arcAngle, true);
               gRelease( state );
          }
     }
#else
     if (!hw)
	 {
          /* use software fallback (clipping is done per rect inside the slowDraw*() routines)   */
          if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = {0};

               scs.state = state;
               scs.drawPixel = slowDrawPixel;
               scs.drawHorizLine = slowDrawHorizLine;
               gArc(&scs, rect, startAngle, arcAngle, true);
               gRelease( state );
          }
	 }
#endif

     dfb_state_unlock( state );
}

void dfb_gfxcard_drawArc( DFBRectangle *rect, int startAngle, int arcAngle, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_DRAWARC ) &&
         dfb_gfxcard_state_acquire( state, DFXL_DRAWARC ))
     {
          /* FIXME: need to check the driver's CCF_CLIPPING flag, and execute the software       */
          /* fallback if clipping is required, but not supported, by the driver                  */

          hw = card->funcs.DrawArc( card->driver_data,
                                    card->device_data, rect, startAngle, arcAngle );
          dfb_gfxcard_state_release( state );
     }
#if 0
     /* The hardware-accelerated DRXL_FILLRECTANGLE case doesn't work, as written.          */
	 /*                                                                                     */
	 /* See dfb_gfxcard_fillrectangle() for the preparation that must be done around before */
	 /* and after calling the driver's FillRectangle().                                     */
	 /*                                                                                     */
	 /*   1st) clipping must be done in software if the driver doesn't support it (i.e.     */
	 /*        check the driver's CCF_CLIPPING flag and call dfb_clip_rectangle if not set) */
	 /*   2nd) the software fallback must be performed on a per-rectangle basis (i.e. check */
	 /*        return from the driver's FillRectangle() and draw in software if not done in */
	 /*        hardware)                                                                    */
	 
     else {
          /* try hardware accelerated rectangle filling */
          if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
              dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, fastDrawPixel, fastDrawHorizLine };
               gArc(&scs, rect, startAngle, arcAngle, false);
               dfb_gfxcard_state_release( state );
          }
          else if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, slowDrawPixel, slowDrawHorizLine };
               gArc(&scs, rect, startAngle, arcAngle, false);
               gRelease( state );
          }
     }
#else
     if (!hw)
	 {
          /* use software fallback (clipping is done per rect inside the slowDraw*() routines)   */
          if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = {0};

               scs.state = state;
               scs.drawPixel = slowDrawPixel;
               scs.drawHorizLine = slowDrawHorizLine;
               gArc(&scs, rect, startAngle, arcAngle, false);
               gRelease( state );
          }
	 }
#endif

     dfb_state_unlock( state );
}

void dfb_gfxcard_fillPolygon( int *xPoints, int *yPoints, int nPoints, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( xPoints != NULL );
     DFB_ASSERT( yPoints != NULL );
     DFB_ASSERT( nPoints != 0 );
     
     dfb_state_lock( state );

     /* Check for acceleration and setup execution. */
     if (dfb_gfxcard_state_check( state, DFXL_FILLPOLYGON ) &&
         dfb_gfxcard_state_acquire( state, DFXL_FILLPOLYGON ))
     {
          /* FIXME: need to check the driver's CCF_CLIPPING flag, and execute the software       */
          /* fallback if clipping is required, but not supported, by the driver                  */

          hw = card->funcs.FillPolygon( card->driver_data,
                                        card->device_data, xPoints, yPoints, nPoints );
          dfb_gfxcard_state_release( state );
     }
#if 0
     /* The hardware-accelerated DRXL_FILLRECTANGLE case doesn't work, as written.          */
	 /*                                                                                     */
	 /* See dfb_gfxcard_fillrectangle() for the preparation that must be done around before */
	 /* and after calling the driver's FillRectangle().                                     */
	 /*                                                                                     */
	 /*   1st) clipping must be done in software if the driver doesn't support it (i.e.     */
	 /*        check the driver's CCF_CLIPPING flag and call dfb_clip_rectangle if not set) */
	 /*   2nd) the software fallback must be performed on a per-rectangle basis (i.e. check */
	 /*        return from the driver's FillRectangle() and draw in software if not done in */
	 /*        hardware)                                                                    */
	 
     else {
          /* try hardware accelerated rectangle filling */
          if (dfb_gfxcard_state_check( state, DFXL_FILLRECTANGLE ) &&
              dfb_gfxcard_state_acquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, fastDrawPixel, fastDrawHorizLine };
               gFillPolygon(&scs, xPoints, yPoints, nPoints);
               dfb_gfxcard_state_release( state );
          }
          else if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = { state, slowDrawPixel, slowDrawHorizLine };
               gFillPolygon(&scs, xPoints, yPoints, nPoints);
               gRelease( state );
          }
     }
#else
     if (!hw)
	 {
          /* use software fallback (clipping is done per rect inside the slowDraw*() routines)   */
          if (gAquire( state, DFXL_FILLRECTANGLE ))
          {
               SCSTATE scs = {0};

               scs.state = state;
               scs.drawPixel = slowDrawPixel;
               scs.drawHorizLine = slowDrawHorizLine;
               gFillPolygon(&scs, xPoints, yPoints, nPoints);
               gRelease( state );
          }
	 }
#endif

     dfb_state_unlock( state );
}

void dfb_gfxcard_blit( DFBRectangle *rect, int dx, int dy, CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );
     
     dfb_state_lock( state );

     if (!dfb_clip_blit_precheck( &state->clip, rect->w, rect->h, dx, dy )) {
          /* no work at all */
          dfb_state_unlock( state );
          return;
     }

     if (dfb_gfxcard_state_check( state, DFXL_BLIT ) &&
         dfb_gfxcard_state_acquire( state, DFXL_BLIT ))
     {
          if (!(card->shared->device_info.caps.flags & CCF_CLIPPING))
               dfb_clip_blit( &state->clip, rect, &dx, &dy );

          hw = card->funcs.Blit( card->driver_data, card->device_data,
                                 rect, dx, dy );

          dfb_gfxcard_state_release( state );
     }
     
     if (!hw) {
          if (gAquire( state, DFXL_BLIT )) {
               dfb_clip_blit( &state->clip, rect, &dx, &dy );
               gBlit( rect, dx, dy );
               gRelease( state );
          }
     }

     dfb_state_unlock( state );
}

void dfb_gfxcard_tileblit( DFBRectangle *rect, int dx, int dy, int w, int h,
                           CardState *state )
{
     int          x, y;
     int          odx;
     DFBRectangle srect;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( rect != NULL );

     /* If called with an invalid rectangle, the algorithm goes into an
        infinite loop. This should never happen but it's safer to check. */
     DFB_ASSERT( rect->w >= 1 );
     DFB_ASSERT( rect->h >= 1 );

     odx = dx;

     dfb_state_lock( state );

     if (dfb_gfxcard_state_check( state, DFXL_BLIT ) &&
         dfb_gfxcard_state_acquire( state, DFXL_BLIT )) {

          for (; dy < h; dy += rect->h) {
               for (dx = odx; dx < w; dx += rect->w) {

                    if (!dfb_clip_blit_precheck( &state->clip,
                                                 rect->w, rect->h, dx, dy ))
                         continue;

                    x = dx;
                    y = dy;
                    srect = *rect;

                    if (!(card->shared->device_info.caps.flags & CCF_CLIPPING))
                         dfb_clip_blit( &state->clip, &srect, &x, &y );

                    card->funcs.Blit( card->driver_data, card->device_data,
                                      &srect, x, y );
               }
          }
          dfb_gfxcard_state_release( state );
     }
     else {
          if (gAquire( state, DFXL_BLIT )) {

               for (; dy < h; dy += rect->h) {
                    for (dx = odx; dx < w; dx += rect->w) {

                         if (!dfb_clip_blit_precheck( &state->clip,
                                                      rect->w, rect->h,
                                                      dx, dy ))
                              continue;

                         x = dx;
                         y = dy;
                         srect = *rect;

                         dfb_clip_blit( &state->clip, &srect, &x, &y );

                         gBlit( &srect, x, y );
                    }
               }
               gRelease( state );
          }
     }

     dfb_state_unlock( state );
}

void dfb_gfxcard_stretchblit( DFBRectangle *srect, DFBRectangle *drect,
                              CardState *state )
{
     bool hw = false;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( srect != NULL );
     DFB_ASSERT( drect != NULL );
     
     dfb_state_lock( state );

     // Time to make the destination width/height positive and set negative
     // flags where appropriate. These need to be passed to the generic.c lib
     // later on in this function. NOTE THAT THIS IS FOR A SOFTWARE SOLUTION
     // ONLY. IMPLEMENTING A HARDWARE SOLUTION WILL HAVE TO TAKE CARE OF THIS
     // IN A HARDWARE DEPENDENT MANNER!
     if (drect->h < 0)
	 {
		 state->flaghneg = true;	
		 drect->h = -(drect->h);
	 }
	 else
		 state->flaghneg = false;

	 if (drect->w < 0)
	 {
		 state->flagwneg = true;
		 drect->w = -(drect->w);
	 }
	 else
		 state->flagwneg = false;


     if (!dfb_clip_blit_precheck( &state->clip, drect->w, drect->h,
                                  drect->x, drect->y ))
     {
          dfb_state_unlock( state );
          return;
     }

     if (dfb_gfxcard_state_check( state, DFXL_STRETCHBLIT ) &&
         dfb_gfxcard_state_acquire( state, DFXL_STRETCHBLIT ))
     {
          if (!(card->shared->device_info.caps.flags & CCF_CLIPPING))
               dfb_clip_stretchblit( &state->clip, srect, drect );

          hw = card->funcs.StretchBlit( card->driver_data,
                                        card->device_data, srect, drect );

          dfb_gfxcard_state_release( state );
     }
     
     if (!hw) {
          if (gAquire( state, DFXL_STRETCHBLIT ))
		  {
               dfb_clip_stretchblit( &state->clip, srect, drect );
               gStretchBlit( srect, drect );
               gRelease( state );
          }
     }

     dfb_state_unlock( state );
}

void dfb_gfxcard_drawstring( const __u8 *text, int bytes,
                             int x, int y,
                             CoreFont *font, CardState *state )
{
     int           *steps = NULL;
     unichar       *chars = NULL;
     CoreGlyphData **glyphs = NULL;

     unichar prev = 0;

     int hw_clipping = (card->shared->device_info.caps.flags & CCF_CLIPPING);
     int kern_x;
     int kern_y;
     int offset;
     int blit = 0;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( text != NULL );
     DFB_ASSERT( bytes > 0 );
     DFB_ASSERT( font != NULL );

     if ((steps = DFBMALLOC((sizeof(int) + sizeof(unichar) + sizeof(CoreGlyphData*)) * bytes)) == NULL)
          return;
     chars = (unichar*)((char*)steps + (sizeof(int) * bytes));
     glyphs = (CoreGlyphData**)((char*)chars + (sizeof(unichar) * bytes));

     dfb_font_lock( font );

     /* preload glyphs to avoid deadlock */
     for (offset = 0; offset < bytes; offset += steps[offset]) {
          steps[offset] = dfb_utf8_skip[text[offset]];
          chars[offset] = dfb_utf8_get_char ((const char *)(&text[offset]));

          if (dfb_font_get_glyph_data (font, chars[offset],
                                       &glyphs[offset]) != DFB_OK)
               glyphs[offset] = NULL;
     }
     
     /* simple prechecks */
     if (x > state->clip.x2 || y > state->clip.y2 ||
         y + font->ascender - font->descender <= state->clip.y1) {
          dfb_font_unlock( font );
          {
               DFBFREE(steps);
               return;
          }
     }

     dfb_state_set_destination( &font->state, state->destination );


	font->state.clip         = state->clip;
	font->state.color        = state->color;
	font->state.color_index  = state->color_index;
	font->state.modified |= (SMF_CLIP | SMF_COLOR);

	/* ALPHA BLENDED TEXT ADD-ON FROM gfxcard.c 1.101 */

	if (state->drawingflags & DSDRAW_BLEND)
		font->state.blittingflags |= DSBLIT_BLEND_COLORALPHA;
	else
		font->state.blittingflags &= ~DSBLIT_BLEND_COLORALPHA;
	font->state.modified |= SMF_BLITTING_FLAGS;

	font->state.porter_duff_rule = state->porter_duff_rule;
	font->state.modified |= SMF_PORTER_DUFF;

	font->state.alpha_const = state->alpha_const;
	font->state.modified |= SMF_ALPHA_CONST;


     for (offset = 0; offset < bytes; offset += steps[offset]) {

          unichar current = chars[offset];

          if (glyphs[offset]) {
               CoreGlyphData *data = glyphs[offset];

               if (prev && font->GetKerning &&
                   (* font->GetKerning) (font, 
                                         prev, current, 
                                         &kern_x, &kern_y) == DFB_OK) {
                    x += kern_x;
                    y += kern_y;
               }

               if (data->width) {
                    int xx = x + data->left;
                    int yy = y + data->top;
                    DFBRectangle rect;
                     
                    rect.x = data->start;
                    rect.y = 0;
                    rect.w = data->width;
                    rect.h = data->height;

                    if (font->state.source != data->surface || !blit) {
                         switch (blit) {
                              case 1:
                                   dfb_gfxcard_state_release( &font->state );
                                   break;
                              case 2:
                                   gRelease( &font->state );
                                   break;
                              default:
                                   break;
                         }
                         dfb_state_set_source( &font->state, data->surface );

                         if (dfb_gfxcard_state_check( &font->state, DFXL_DRAWSTRING ) &&
                             dfb_gfxcard_state_acquire( &font->state, DFXL_DRAWSTRING ))
                              blit = 1;
                         else if (gAquire( &font->state, DFXL_DRAWSTRING ))
                              blit = 2;
                         else
                              blit = 0;
                    }

                    if (dfb_clip_blit_precheck( &font->state.clip,
                                                rect.w, rect.h, xx, yy )) {
                         switch (blit) {
                              case 1:
                                   if (!hw_clipping)
                                        dfb_clip_blit( &font->state.clip,
                                                       &rect, &xx, &yy );
                                   card->funcs.Blit( card->driver_data,
                                                     card->device_data,
                                                     &rect, xx, yy );
                                   break;
                              case 2:
                                   dfb_clip_blit( &font->state.clip,
                                                  &rect, &xx, &yy );
                                   gBlit( &rect, xx, yy );
                                   break;
                              default:
                                   break;
                         }
                    }
               }
               x += data->advance;
               prev = current;
          }
     }

     switch (blit) {
          case 1:
               dfb_gfxcard_state_release( &font->state );
               break;
          case 2:
               gRelease( &font->state );
               break;
          default:
               break;
     }

     dfb_font_unlock( font );
     DFBFREE(steps);
     return;
}

void dfb_gfxcard_drawglyph( unichar index, int x, int y,
                            CoreFont *font, CardState *state )
{
     CoreGlyphData *data;
     DFBRectangle   rect;

     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     DFB_ASSERT( state != NULL );
     DFB_ASSERT( font != NULL );
     
     dfb_font_lock( font );

     if (dfb_font_get_glyph_data (font, index, &data) != DFB_OK ||
         !data->width) {

          dfb_font_unlock( font );
          return;
     }

     x += data->left;
     y += data->top;

     if (! dfb_clip_blit_precheck( &state->clip,
                                   data->width, data->height, x, y )) {
          dfb_font_unlock( font );
          return;
     }

     dfb_state_set_destination( &font->state, state->destination );


	font->state.clip         = state->clip;
	font->state.color        = state->color;
	font->state.color_index  = state->color_index;
	font->state.modified |= (SMF_CLIP | SMF_COLOR);

	/* ALPHA BLENDED TEXT ADD-ON FROM gfxcard.c 1.101 */

	if (state->drawingflags & DSDRAW_BLEND)
		font->state.blittingflags |= DSBLIT_BLEND_COLORALPHA;
	else
		font->state.blittingflags &= ~DSBLIT_BLEND_COLORALPHA;
	font->state.modified |= SMF_BLITTING_FLAGS;

	font->state.porter_duff_rule = state->porter_duff_rule;
	font->state.modified |= SMF_PORTER_DUFF;

	font->state.alpha_const = state->alpha_const;
	font->state.modified |= SMF_ALPHA_CONST;


     dfb_state_set_source( &font->state, data->surface );

     rect.x = data->start;
     rect.y = 0;
     rect.w = data->width;
     rect.h = data->height;

     if (dfb_gfxcard_state_check( &font->state, DFXL_BLIT ) &&
         dfb_gfxcard_state_acquire( &font->state, DFXL_BLIT )) {

          if (!(card->shared->device_info.caps.flags & CCF_CLIPPING))
               dfb_clip_blit( &font->state.clip, &rect, &x, &y );

          card->funcs.Blit( card->driver_data, card->device_data, &rect, x, y);
          dfb_gfxcard_state_release( &font->state );
     }
     else if (gAquire( &font->state, DFXL_BLIT )) {

          dfb_clip_blit( &font->state.clip, &rect, &x, &y );
          gBlit( &rect, x, y );
          gRelease( &font->state );
     }

     dfb_font_unlock( font );
}

void dfb_gfxcard_sync()
{
     if (card && card->funcs.EngineSync)
          card->funcs.EngineSync( card->driver_data, card->device_data );
}

void dfb_gfxcard_flush_texture_cache()
{
     DFB_ASSERT( card != NULL );
     
     if (card->funcs.FlushTextureCache)
          card->funcs.FlushTextureCache( card->driver_data, card->device_data );
}

void dfb_gfxcard_after_set_var()
{
     DFB_ASSERT( card != NULL );
     
     if (card->funcs.AfterSetVar)
          card->funcs.AfterSetVar( card->driver_data, card->device_data );
}

DFBResult
dfb_gfxcard_adjust_heap_offset( unsigned int offset )
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     
     return dfb_surfacemanager_adjust_heap_offset( card->shared->surface_manager, offset );
}

SurfaceManager *
dfb_gfxcard_surface_manager()
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     
     return card->shared->surface_manager;
}

FusionObjectPool *
dfb_gfxcard_surface_pool()
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     
     return card->shared->surface_pool;
}

FusionObjectPool *
dfb_gfxcard_palette_pool()
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     
     return card->shared->palette_pool;
}

CardCapabilities
dfb_gfxcard_capabilities()
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     
     return card->shared->device_info.caps;
}

#if 0
int
dfb_gfxcard_reserve_memory( GraphicsDevice *device, unsigned int size )
{
     GraphicsDeviceShared *shared;

     DFB_ASSERT( device != NULL );
     DFB_ASSERT( device->shared != NULL );

     shared = device->shared;

     if (shared->surface_manager)
          return -1;

     if (shared->videoram_length < size)
          return -1;

     shared->videoram_length -= size;

     return shared->videoram_length;
}
#endif

unsigned int
dfb_gfxcard_memory_length()
{
     DFB_ASSERT( card != NULL );
     DFB_ASSERT( card->shared != NULL );
     
     return card->shared->videoram_length;
}

#if 0
volatile void *
dfb_gfxcard_map_mmio( GraphicsDevice *device,
                      unsigned int    offset,
                      int             length )
{
     DFB_UNUSED_PARAM(device);
     return dfb_system_map_mmio( offset, length );
}

void
dfb_gfxcard_unmap_mmio( GraphicsDevice *device,
                        volatile void  *addr,
                        int             length )
{
     DFB_UNUSED_PARAM(device);
     dfb_system_unmap_mmio( addr, length );
}

int
dfb_gfxcard_get_accelerator( GraphicsDevice *device )
{
     DFB_UNUSED_PARAM(device);
     return dfb_system_get_accelerator();
}

unsigned long
dfb_gfxcard_memory_physical( GraphicsDevice *device,
                             unsigned int    offset )
{
     DFB_UNUSED_PARAM(device);
     return dfb_system_video_memory_physical( offset );
}

void *
dfb_gfxcard_memory_virtual( GraphicsDevice *device,
                            unsigned int    offset )
{
     DFB_UNUSED_PARAM(device);
     return dfb_system_video_memory_virtual( offset );
}
#endif

/** internal **/
/* Should only do this if actual hardware used */

#ifdef HW_DFBDRIVER
	static GraphicsDriverFuncs driver_funcs;
#endif
/*
 * loads/probes/unloads one driver module after another until a suitable
 * driver is found and returns its symlinked functions
 */
static void dfb_gfxcard_find_driver()
{
// Only does something if there is real hardware present.
#ifdef HW_DFBDRIVER
	/** defined in <core/motorola/6412/dfbdriver.h> for example **/
	//GetGraphicsDriverFuncs(&driver_funcs);
	(*getgraphicsdriverfuncs)(&driver_funcs);  /* Call MPE driver func */

	driver_funcs.GetDriverInfo(card, &card->shared->driver_info);
	card->driver_funcs = &driver_funcs;
#endif
}
