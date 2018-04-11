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
#include "win32fb.h"
#include "wfb.h"

static int
primaryLayerDataSize     (void);
     
static DFBResult
primaryInitLayer         ( GraphicsDevice             *device,
                           DisplayLayer               *layer,
                           DisplayLayerInfo           *layer_info,
                           DFBDisplayLayerConfig      *default_config,
                           DFBColorAdjustment         *default_adj,
                           void                       *driver_data,
                           void                       *layer_data );

static DFBResult
primaryEnable            ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data );

static DFBResult
primaryDisable           ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data );

static DFBResult
primaryTestConfiguration ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config,
                           DFBDisplayLayerConfigFlags *failed );

static DFBResult
primarySetConfiguration  ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config );


static DFBResult
primaryFlipBuffers       ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBSurfaceFlipFlags         flags );
     
static DFBResult
primaryUpdateRegion      ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBRegion                  *region,
                           DFBSurfaceFlipFlags         flags );

static DFBResult
primarySetPalette        ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           CorePalette                *palette );

static DFBResult
primaryAllocateSurface   ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config,
                           CoreSurface               **surface );

static DFBResult
primaryReallocateSurface ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config,
                           CoreSurface                *surface );

static DFBResult
primaryDeallocateSurface ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           CoreSurface                *surface );

DisplayLayerFuncs win32fbPrimaryLayerFuncs = {
     primaryLayerDataSize,
     primaryInitLayer,
     primaryEnable,
     primaryDisable,
     primaryTestConfiguration,
     primarySetConfiguration,
     primaryFlipBuffers,
     primaryUpdateRegion,
     primarySetPalette,
     primaryAllocateSurface,
     primaryReallocateSurface,
     primaryDeallocateSurface,
};


static DFBResult
update_screen( CoreSurface *surface, int x, int y, int w, int h );

static WFB_Screen *screen = NULL;

/** primary layer functions **/

static int
primaryLayerDataSize     (void)
{
     return 0;
}

static DFBResult
primaryInitLayer         ( GraphicsDevice             *device,
                           DisplayLayer               *layer,
                           DisplayLayerInfo           *layer_info,
                           DFBDisplayLayerConfig      *default_config,
                           DFBColorAdjustment         *default_adj,
                           void                       *driver_data,
                           void                       *layer_data )
{
     WFB_Result result;

     DFB_UNUSED_PARAM(device);
     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(default_adj);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);

     /* set capabilities and type */
     layer_info->desc.caps = DLCAPS_SURFACE;
     layer_info->desc.type = DLTF_GRAPHICS;

     /* set name */
     snprintf( layer_info->desc.name,
               DFB_DISPLAY_LAYER_DESC_NAME_LENGTH, "Win32FB Primary Layer" );

     /* fill out the default configuration */
     default_config->flags       = DLCONF_WIDTH | DLCONF_HEIGHT |
                                   DLCONF_PIXELFORMAT | DLCONF_BUFFERMODE;
     default_config->buffermode  = DLBM_FRONTONLY;

     if (dfb_config->mode.width)
          default_config->width  = dfb_config->mode.width;
     else
          default_config->width  = 640;

     if (dfb_config->mode.height)
          default_config->height = dfb_config->mode.height;
     else
          default_config->height = 480;
     
     if (dfb_config->mode.format != DSPF_UNKNOWN)
          default_config->pixelformat = dfb_config->mode.format;
     else if (dfb_config->mode.depth > 0)
          default_config->pixelformat = dfb_pixelformat_for_depth( dfb_config->mode.depth );
     else
          default_config->pixelformat = DSPF_ARGB;

     /* Set video mode */
     if ( (result = WFB_SetVideoMode(default_config->width,
                                   default_config->height,
                                   DFB_BYTES_PER_PIXEL(default_config->pixelformat),
                                   DFB_BITS_PER_PIXEL(default_config->pixelformat),
                                   &screen)) != WFB_SUCCESS ) {
             ERRORMSG("Couldn't set %dx%dx%d video mode: %s\n",
                      default_config->width, default_config->height,
                      DFB_BITS_PER_PIXEL(default_config->pixelformat), result);
             return DFB_FAILURE;
     }
     
     return DFB_OK;
}

static DFBResult
primaryEnable            ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data )
{
     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);

     /* always enabled */
     return DFB_OK;
}

static DFBResult
primaryDisable           ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data )
{
     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);

     /* cannot be disabled */
     return DFB_UNSUPPORTED;
}

static DFBResult
primaryTestConfiguration ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config,
                           DFBDisplayLayerConfigFlags *failed )
{
     DFBDisplayLayerConfigFlags fail = 0;

     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);
     DFB_UNUSED_PARAM(config);

     if (failed)
          *failed = fail;

     if (fail)
          return DFB_UNSUPPORTED;

     return DFB_OK;
}

static DFBResult
primarySetConfiguration  ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config )
{
     WFB_Result result;

     CoreSurface *surface = dfb_layer_surface( layer );

     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);

     /* Set video mode */
     if ( (result = WFB_SetVideoMode(config->width,
                                   config->height,
                                   DFB_BYTES_PER_PIXEL(config->pixelformat),
                                   DFB_BITS_PER_PIXEL(config->pixelformat),
                                   &screen)) != WFB_SUCCESS ) {
             ERRORMSG("Couldn't set %dx%dx%d video mode: %s\n",
                      config->width, config->height,
                      DFB_BITS_PER_PIXEL(config->pixelformat), result);
             return DFB_FAILURE;
     }

     surface->back_buffer->system.addr  = screen->pixels;
     surface->back_buffer->system.pitch = screen->pitch;
     
     surface->front_buffer->system.addr  = screen->pixels;
     surface->front_buffer->system.pitch = screen->pitch;
     
     return DFB_OK;
}

static DFBResult
primaryFlipBuffers       ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBSurfaceFlipFlags         flags )
{
     CoreSurface *surface = dfb_layer_surface( layer );

     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);
     DFB_UNUSED_PARAM(flags);

     dfb_surface_flip_buffers( surface );

     if (WFB_Flip() != WFB_SUCCESS)
          return DFB_FAILURE;

     surface->back_buffer->system.addr  = screen->pixels;
     surface->back_buffer->system.pitch = screen->pitch;

     surface->front_buffer->system.addr  = screen->pixels;
     surface->front_buffer->system.pitch = screen->pitch;
     
     return DFB_OK;
}
     
static DFBResult
primaryUpdateRegion      ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBRegion                  *region,
                           DFBSurfaceFlipFlags         flags )
{
     CoreSurface *surface = dfb_layer_surface( layer );

     DFB_UNUSED_PARAM(layer_data);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(flags);

     if (!region)
          return update_screen( surface,
                                0, 0, surface->width, surface->height );

     return update_screen( surface,
                           region->x1, region->y1,
                           region->x2 - region->x1 + 1,
                           region->y2 - region->y1 + 1 );
}
     
static DFBResult
primarySetPalette ( DisplayLayer               *layer,
                    void                       *driver_data,
                    void                       *layer_data,
                    CorePalette                *palette )
{
     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);
     DFB_UNUSED_PARAM(palette);

#ifdef NOTYET
     int       i;
     Win32FB_Color colors[palette->num_entries];

     for (i=0; i<palette->num_entries; i++) {
          colors[i].r = palette->entries[i].r;
          colors[i].g = palette->entries[i].g;
          colors[i].b = palette->entries[i].b;
     }
     
     Win32FB_SetColors( screen, colors, 0, palette->num_entries );
#endif

     return DFB_OK;
}

static DFBResult
primaryAllocateSurface   ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config,
                           CoreSurface               **ret_surface )
{
     DFBSurfaceCapabilities caps = DSCAPS_SYSTEMONLY;

     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);

     if (config->buffermode != DLBM_FRONTONLY)
          caps |= DSCAPS_FLIPPING;

     return dfb_surface_create_preallocated( config->width, config->height,
                                config->pixelformat, CSP_SYSTEMONLY,
                                caps, NULL, screen->pixels, screen->pixels,
                                screen->pitch, screen->pitch, ret_surface );
}

static DFBResult
primaryReallocateSurface ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           DFBDisplayLayerConfig      *config,
                           CoreSurface                *surface )
{
     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);

#if 0
     DFBResult ret;
     
     /* FIXME: write surface management functions
               for easier configuration changes */
     
     switch (config->buffermode) {
          case DLBM_BACKVIDEO:
          case DLBM_BACKSYSTEM:
               surface->caps |= DSCAPS_FLIPPING;

               ret = dfb_surface_reconfig( surface,
                                           CSP_SYSTEMONLY, CSP_SYSTEMONLY );
               break;

          case DLBM_FRONTONLY:
               surface->caps &= ~DSCAPS_FLIPPING;

               ret = dfb_surface_reconfig( surface,
                                           CSP_SYSTEMONLY, CSP_SYSTEMONLY );
               break;
          
          default:
               BUG("unknown buffermode");
               return DFB_BUG;
     }
     if (ret)
          return ret;

     ret = dfb_surface_reformat( surface, config->width,
                                 config->height, config->pixelformat );
     if (ret)
          return ret;

     if (config->options & DLOP_DEINTERLACING)
          surface->caps |= DSCAPS_INTERLACED;
     else
          surface->caps &= ~DSCAPS_INTERLACED;
#endif

     surface->width  = config->width;
     surface->height = config->height;
     surface->format = config->pixelformat;

     switch (config->buffermode) {
          case DLBM_BACKVIDEO:
          case DLBM_BACKSYSTEM:
               surface->caps |= DSCAPS_FLIPPING;
               break;

          case DLBM_FRONTONLY:
               surface->caps &= ~DSCAPS_FLIPPING;
               break;
          
          default:
               BUG("unknown buffermode");
               return DFB_BUG;
     }
     
     if (DFB_PIXELFORMAT_IS_INDEXED(config->pixelformat) && !surface->palette) {
          DFBResult    ret;
          CorePalette *palette;
           
          ret = dfb_palette_create( 256, &palette );
          if (ret)
               return ret;

          if (config->pixelformat == DSPF_LUT8)
               dfb_palette_generate_rgb332_map( palette );
          
          dfb_surface_set_palette( surface, palette );

          dfb_palette_unref( palette );
     }
     
     return DFB_OK;
}

static DFBResult
primaryDeallocateSurface ( DisplayLayer               *layer,
                           void                       *driver_data,
                           void                       *layer_data,
                           CoreSurface                *surface )
{
     DFB_UNUSED_PARAM(layer);
     DFB_UNUSED_PARAM(driver_data);
     DFB_UNUSED_PARAM(layer_data);
     DFB_UNUSED_PARAM(surface);

     // Anything to do here?
     return DFB_OK;
}


/******************************************************************************/

static DFBResult
update_screen( CoreSurface *surface, int x, int y, int w, int h )
{
     DFB_UNUSED_PARAM(surface);
     DFB_UNUSED_PARAM(x);
     DFB_UNUSED_PARAM(y);
     DFB_UNUSED_PARAM(w);
     DFB_UNUSED_PARAM(h);

#ifdef NOTYET
#if 0
     int          i;
     void        *dst;
     void        *src;
     int          pitch;
     DFBResult    ret;

     DFB_ASSERT( surface != NULL );
     
     if (Win32FB_LockSurface( screen ) < 0) {
          ERRORMSG( "DirectFB/Win32FB: "
                    "Couldn't lock the display surface: %s\n", WFB_GetError() );
          return DFB_FAILURE;
     }

     ret = dfb_surface_soft_lock( surface, DSLF_READ, &src, &pitch, true );
     if (ret) {
          ERRORMSG( "DirectFB/Win32FB: Couldn't lock layer surface: %s\n",
                    DirectFBErrorString( ret ) );
          Win32FB_UnlockSurface(screen);
          return ret;
     }

     dst = screen->pixels;

     src += DFB_BYTES_PER_LINE( surface->format, x ) + y * pitch;
     dst += DFB_BYTES_PER_LINE( surface->format, x ) + y * screen->pitch;

     for (i=0; i<h; ++i) {
          dfb_memcpy( dst, src,
                      DFB_BYTES_PER_LINE( surface->format, w ) );

          src += pitch;
          dst += screen->pitch;
     }

     dfb_surface_unlock( surface, true );
     
     Win32FB_UnlockSurface( screen );
#endif     
     
     Win32FB_UpdateRect( screen, x, y, w, h );
#endif
     
     return DFB_OK;
}
