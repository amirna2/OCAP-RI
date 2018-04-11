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
#include <core/layers.h>
#include <core/state.h>
#include <core/palette.h>
#include <core/system.h>
#include <core/surfacemanager.h>
#include <gfx/convert.h>
#include <gfx/util.h>
#include <misc/mem.h>
#include <misc/util.h>

typedef struct {
     DFBDisplayLayerID        id;      /* unique id, functions as an index,
                                          primary layer has a fixed id */

     DisplayLayerInfo         layer_info;  
     void                    *layer_data;    

     /****/

     DFBDisplayLayerConfig    config;  /* current configuration */
     
     /* these are normalized values for stretching layers in hardware */
     struct {
          float     x, y;  /* 0,0 for the primary layer */
          float     w, h;  /* 1,1 for the primary layer */
     } screen;  

     DFBColorAdjustment       adjustment;      

     /****/

     int                      enabled; /* layers can be turned on and off */

     CoreSurface             *surface; /* surface of the layer */
} DisplayLayerShared;  

struct _DisplayLayer {
     DisplayLayerShared *shared;

     GraphicsDevice     *device;

     void               *driver_data;
     void               *layer_data;   /* copy of shared->layer_data */

     DisplayLayerFuncs  *funcs;

     CardState           state;
};  

typedef struct {
     unsigned int        num;
     DisplayLayerShared *layers[MAX_LAYERS];
} CoreLayersField;

static CoreLayersField *layersfield = NULL;

static int           dfb_num_layers = 0;
static DisplayLayer *dfb_layers[MAX_LAYERS] = { NULL };


DFB_CORE_PART( layers, 0, sizeof(CoreLayersField) );


static DFBResult allocate_surface    ( DisplayLayer          *layer );
static DFBResult reallocate_surface  ( DisplayLayer          *layer,
                                       DFBDisplayLayerConfig *config );
static DFBResult deallocate_surface  ( DisplayLayer          *layer );

/** public **/

static DFBResult
dfb_layers_initialize( void *data_local, void *data_shared )
{
     int       i;
     DFBResult ret;

     DFB_ASSERT( layersfield == NULL );
     DFB_ASSERT( data_shared != NULL );
     
     DFB_UNUSED_PARAM(data_local);

     layersfield = data_shared;

     for (i=0; i<dfb_num_layers; i++) {
          int                 layer_data_size;
          DisplayLayerShared *shared;
          DisplayLayer       *layer = dfb_layers[i];
          
          /* allocate shared data */
          if ((shared = shcalloc( 1, sizeof(DisplayLayerShared) )) == NULL)
              return DFB_NOSYSTEMMEMORY;

          /* zero based counting */
          shared->id = i;

		  shared->layer_data = NULL;
          /* allocate shared layer driver data */
          layer_data_size = layer->funcs->LayerDataSize();
          if (layer_data_size > 0)
               if ((shared->layer_data = shcalloc( 1, layer_data_size )) == NULL)
                   return DFB_NOSYSTEMMEMORY;

          /* set default screen location */
          shared->screen.x = 0.0f;
          shared->screen.y = 0.0f;
          shared->screen.w = 1.0f;
          shared->screen.h = 1.0f;

          /* initialize the layer gaining the default configuration,
             the default color adjustment and the layer information */
          ret = layer->funcs->InitLayer( layer->device, layer,
                                         &shared->layer_info,
                                         &shared->config,
                                         &shared->adjustment,
                                         layer->driver_data,
                                         shared->layer_data );
          if (ret) {
               ERRORMSG("DirectFB/Core/layers: "
                        "Failed to initialize layer %d!\n", shared->id);

               shfree( shared->layer_data );
               shfree( shared );

               return ret;
          }

          /* make a copy for faster access */
          layer->layer_data = shared->layer_data;
          
          /* store pointer to shared data */
          layer->shared = shared;
          
          /* add it to the shared list */
          layersfield->layers[ layersfield->num++ ] = shared;
     }

     /* enable the primary layer now */
     ret = dfb_layer_enable( dfb_layers[DLID_PRIMARY] );
     if (ret) {
          ERRORMSG("DirectFB/Core/layers: Failed to enable primary layer!\n");
          return ret;
     }
     
     return DFB_OK;
}

static DFBResult
dfb_layers_join( void *data_local, void *data_shared )
{
     int i;

     DFB_ASSERT( layersfield == NULL );
     DFB_ASSERT( data_shared != NULL );
     
     DFB_UNUSED_PARAM(data_local);

     layersfield = data_shared;

     if ((unsigned int)dfb_num_layers != layersfield->num)
          CAUTION("Number of layers does not match!");

     for (i=0; i<dfb_num_layers; i++) {
          DisplayLayer       *layer  = dfb_layers[i];
          DisplayLayerShared *shared = layersfield->layers[i];

          /* make a copy for faster access */
          layer->layer_data = shared->layer_data;
          
          /* store pointer to shared data */
          layer->shared = shared;
     }
     
     return DFB_OK;
}

static DFBResult
dfb_layers_shutdown( bool emergency )
{
     int i;

     DFB_ASSERT( layersfield != NULL );
     
     /* Begin with the most recently added */
     for (i=layersfield->num-1; i>=0; i--) {
          DisplayLayer *l = dfb_layers[i];

          if (emergency && l->shared->enabled) {
               /* Just turn it off during emergency shutdown */
               l->funcs->Disable( l, l->driver_data, l->layer_data );
          }
          else {
               /* Disable layer, destroy surface and
                  window stack (including windows and their surfaces) */
               dfb_layer_disable( l );
          }

          /* Free shared layer driver data */
          if (l->shared->layer_data)
               shfree( l->shared->layer_data );

          /* Free shared layer data */
          shfree( l->shared );

          /* Deinit state for stack repaints. */
          dfb_state_set_destination( &l->state, NULL );
          dfb_state_destroy( &l->state );
          
          /* Free local layer data */
          DFBFREE( l );
     }

     layersfield = NULL;

     dfb_num_layers = 0;

     return DFB_OK;
}

static DFBResult
dfb_layers_leave( bool emergency )
{
     int i;

     DFB_ASSERT( layersfield != NULL );
     
     DFB_UNUSED_PARAM(emergency);

     /* Free all local data */
     for (i=0; (unsigned int)i < layersfield->num; i++) {
          DisplayLayer *layer = dfb_layers[i];

          /* Deinit state for stack repaints. */
          dfb_state_set_destination( &layer->state, NULL );
          dfb_state_destroy( &layer->state );

          /* Free local layer data */
          DFBFREE( layer );
     }

     layersfield = NULL;
     
     dfb_num_layers = 0;

     return DFB_OK;
}

static DFBResult
dfb_layers_suspend()
{
     int i;

     DFB_ASSERT( layersfield != NULL );
     
     for (i=layersfield->num-1; i>=0; i--) {
          DisplayLayer *l = dfb_layers[i];

          if (l->shared->enabled)
               l->funcs->Disable( l, l->driver_data, l->layer_data );
     }

     return DFB_OK;
}

static DFBResult
dfb_layers_resume()
{
     int i;

     DFB_ASSERT( layersfield != NULL );
     
     for (i=0; (unsigned int)i < layersfield->num; i++) {
          DisplayLayer *l = dfb_layers[i];

          if (l->shared->enabled) {
               l->funcs->Enable( l, l->driver_data, l->layer_data );
               
               l->funcs->SetConfiguration( l, l->driver_data,
                                           l->layer_data, &l->shared->config );
          }
     }

     return DFB_OK;
}

DFBResult
dfb_layers_register( GraphicsDevice    *device,
                     void              *driver_data,
                     DisplayLayerFuncs *funcs )
{
     DisplayLayer *layer;

     DFB_ASSERT( funcs != NULL );
     
     if (dfb_num_layers == MAX_LAYERS) {
          ERRORMSG( "DirectFB/Core/Layers: "
                    "Maximum number of layers reached!\n" );
          return DFB_FAILURE;
     }

     /* allocate local data */
     if ((layer = DFBCALLOC( 1, sizeof(DisplayLayer) )) == NULL)
         return DFB_NOSYSTEMMEMORY;

     /* assign local pointers */
     layer->device      = device;
     layer->driver_data = driver_data;
     layer->funcs       = funcs;

     /* add it to the local list */
     dfb_layers[dfb_num_layers++] = layer;

     return DFB_OK;
}

typedef void (*AnyFunc)(void);

void
dfb_layers_enumerate( DisplayLayerCallback  callback,
                      void                 *ctx )
{
     int i;

     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( callback != NULL );
     
     for (i=0; (unsigned int)i < layersfield->num; i++) {
          if (callback( dfb_layers[i], ctx ) == DFENUM_CANCEL)
               break;
     }
}

DisplayLayer *
dfb_layer_at( DFBDisplayLayerID id )
{
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( id < layersfield->num);

     return dfb_layers[id];
}

/*
 * Release layer after lease/purchase.
 */
void
dfb_layer_release( DisplayLayer *layer, bool repaint )
{
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( layer->shared->enabled );

     DFB_UNUSED_PARAM(repaint);
}


DFBResult
dfb_layer_enable( DisplayLayer *layer )
{
     DFBResult           ret;
     DisplayLayerShared *shared;
     
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->funcs != NULL );
     DFB_ASSERT( layer->shared != NULL );
     
     shared = layer->shared;

     /* FIXME: add reference counting */
     if (shared->enabled)
          return DFB_OK;

     /* allocate the surface before enabling it */
     if (shared->layer_info.desc.caps & DLCAPS_SURFACE) {
          ret = allocate_surface( layer );
          if (ret) {
               ERRORMSG("DirectFB/Core/layers: Could not allocate surface!\n");
               return ret;
          }
     }
     
     /* set default/last configuation, this shouldn't fail */
     ret = layer->funcs->SetConfiguration( layer, layer->driver_data,
                                           layer->layer_data, &shared->config );
     if (ret) {
          ERRORMSG("DirectFB/Core/layers: "
                   "Setting default/last configuration failed!\n");

          if (shared->surface)
               deallocate_surface( layer );

          return ret;
     }

     /* enable the display layer */
     ret = layer->funcs->Enable( layer,
                                 layer->driver_data, layer->layer_data );
     if (ret) {
          if (shared->surface)
               deallocate_surface( layer );
          
          return ret;
     }

     shared->enabled = true;

     if (shared->surface) {
          CoreSurface *surface = shared->surface;

          dfb_surface_link( &shared->surface, surface );
          dfb_surface_unref( surface );
          
          /* set default palette */
          if (surface->palette && layer->funcs->SetPalette)
               layer->funcs->SetPalette( layer, layer->driver_data,
                                         layer->layer_data, surface->palette );
     }

     INITMSG( "DirectFB/Layer: Enabled '%s'.\n", shared->layer_info.desc.name );

     return DFB_OK;
}

DFBResult
dfb_layer_disable( DisplayLayer *layer )
{
     DFB_UNUSED_PARAM(layer);
     return DFB_OK;
}

/*
 * configuration management
 */

DFBResult
dfb_layer_test_configuration( DisplayLayer               *layer,
                              DFBDisplayLayerConfig      *config,
                              DFBDisplayLayerConfigFlags *failed )
{
     DFBDisplayLayerConfigFlags  unchanged;
     DisplayLayerShared         *shared;

     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->funcs != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( config != NULL );

     unchanged = ~(config->flags);
     shared    = layer->shared;
     
     /*
      * Fill all unchanged values with their current setting.
      */
     if (unchanged & DLCONF_BUFFERMODE)
          config->buffermode = shared->config.buffermode;

     if (unchanged & DLCONF_HEIGHT)
          config->height = shared->config.height;

     if (unchanged & DLCONF_OPTIONS)
          config->options = shared->config.options;

     if (unchanged & DLCONF_PIXELFORMAT)
          config->pixelformat = shared->config.pixelformat;

     if (unchanged & DLCONF_WIDTH)
          config->width = shared->config.width;

     /* call driver function now with a complete configuration */
     return layer->funcs->TestConfiguration( layer, layer->driver_data,
                                             layer->layer_data, config,
                                             failed );
}

DFBResult
dfb_layer_set_configuration( DisplayLayer          *layer,
                             DFBDisplayLayerConfig *config )
{
     DFBResult           ret;
     DisplayLayerShared *shared;

     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->funcs != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( layer->shared->enabled );
     DFB_ASSERT( config != NULL );

     shared = layer->shared;

     /* build new configuration and test it */
     ret = dfb_layer_test_configuration( layer, config, NULL );
     if (ret)
          return ret;

     /* reallocate the surface before setting the new configuration */
     if (shared->layer_info.desc.caps & DLCAPS_SURFACE) {
          ret = reallocate_surface( layer, config );
          if (ret) {
               ERRORMSG("DirectFB/Core/layers: "
                        "Reallocation of layer surface failed!\n");
               return ret;
          }
     }
     
     /* apply new configuration, this shouldn't fail */
     ret = layer->funcs->SetConfiguration( layer, layer->driver_data,
                                           layer->layer_data, config );
     if (ret) {
          CAUTION("setting new configuration failed");
          return ret;
     }
     
     if (shared->layer_info.desc.caps & DLCAPS_SURFACE) {
          CoreSurface *surface = shared->surface;

          /* reset palette */
          if (DFB_PIXELFORMAT_IS_INDEXED( surface->format ) &&
              surface->palette && layer->funcs->SetPalette)
          {
               layer->funcs->SetPalette( layer, layer->driver_data,
                                         layer->layer_data, surface->palette );
          }
     }
     
     /*
      * Write back modified entries.
      */
     if (config->flags & DLCONF_BUFFERMODE)
          shared->config.buffermode = config->buffermode;

     if (config->flags & DLCONF_HEIGHT)
          shared->config.height = config->height;

     if (config->flags & DLCONF_OPTIONS)
          shared->config.options = config->options;

     if (config->flags & DLCONF_PIXELFORMAT)
          shared->config.pixelformat = config->pixelformat;

     if (config->flags & DLCONF_WIDTH)
          shared->config.width = config->width;

     return DFB_OK;
}

DFBResult
dfb_layer_get_configuration( DisplayLayer          *layer,
                             DFBDisplayLayerConfig *config )
{
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( config != NULL );
     
     *config = layer->shared->config;

     return DFB_OK;
}


/*
 * various functions
 */

CoreSurface *
dfb_layer_surface( const DisplayLayer *layer )
{
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->shared != NULL );
//FIXME     DFB_ASSERT( layer->shared->enabled );
     DFB_ASSERT( layer->shared->surface );

     return layer->shared->surface;
}

void
dfb_layer_description( const DisplayLayer         *layer,
                       DFBDisplayLayerDescription *desc )
{
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( desc != NULL );
     
     *desc = layer->shared->layer_info.desc;
}

DFBDisplayLayerID
dfb_layer_id( const DisplayLayer *layer )
{
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->shared != NULL );
     
     return layer->shared->id;
}

DFBResult
dfb_layer_flip_buffers( DisplayLayer *layer, DFBSurfaceFlipFlags flags )
{
     DisplayLayerShared *shared;

     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( layer->shared->enabled );
     DFB_ASSERT( layer->shared->surface );

     shared = layer->shared;
     
     switch (shared->config.buffermode) {
          case DLBM_FRONTONLY:
               return DFB_UNSUPPORTED;

          case DLBM_BACKVIDEO:
               return layer->funcs->FlipBuffers( layer,
                                                 layer->driver_data,
                                                 layer->layer_data, flags );
          
          case DLBM_BACKSYSTEM:
               dfb_back_to_front_copy( shared->surface, NULL );
               dfb_layer_update_region( layer, NULL, flags );
               break;

          default:
               BUG("unknown buffer mode");
               return DFB_BUG;
     }

     return DFB_OK;
}

DFBResult
dfb_layer_update_region( DisplayLayer        *layer,
                         DFBRegion           *region,
                         DFBSurfaceFlipFlags  flags )
{
     DFB_ASSERT( layer );
     DFB_ASSERT( layer->funcs );
     DFB_ASSERT( layer->shared );
     DFB_ASSERT( layer->shared->enabled );
     DFB_ASSERT( region != NULL );
     
     if (layer->funcs->UpdateRegion)
          return layer->funcs->UpdateRegion( layer,
                                             layer->driver_data,
                                             layer->layer_data,
                                             region, flags );
     
     return DFB_OK;
}

/*
 * layer surface (re/de)allocation
 */

static DFBResult
allocate_surface( DisplayLayer *layer )
{
     DFBSurfaceCapabilities  caps   = DSCAPS_VIDEOONLY;
     DisplayLayerShared     *shared;
     
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->funcs != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( layer->shared->surface == NULL );

     shared = layer->shared;

     if (layer->funcs->AllocateSurface)
          return layer->funcs->AllocateSurface( layer, layer->driver_data,
                                                layer->layer_data,
                                                &shared->config,
                                                &shared->surface );

     /* choose buffermode */
     if (shared->config.flags & DLCONF_BUFFERMODE) {
          switch (shared->config.buffermode) {
               case DLBM_FRONTONLY:
                    break;

               case DLBM_BACKVIDEO:
                    caps |= DSCAPS_FLIPPING;
                    break;

               case DLBM_BACKSYSTEM:
                    ONCE("DLBM_BACKSYSTEM in default config is unimplemented");
                    break;

               default:
                    BUG("unknown buffermode");
                    break;
          }
     }

     return dfb_surface_create( shared->config.width, shared->config.height,
                                shared->config.pixelformat, CSP_VIDEOONLY,
                                caps, NULL, &shared->surface );
}

static DFBResult
reallocate_surface( DisplayLayer *layer, DFBDisplayLayerConfig *config )
{
     DFBResult           ret;
     DisplayLayerShared *shared;
     
     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->funcs != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( layer->shared->surface != NULL );

     shared = layer->shared;
     
     if (layer->funcs->ReallocateSurface)
          return layer->funcs->ReallocateSurface( layer, layer->driver_data,
                                                  layer->layer_data, config,
                                                  shared->surface );

     /* FIXME: write surface management functions
               for easier configuration changes */
     
     if (shared->config.buffermode != config->buffermode) {
          switch (config->buffermode) {
               case DLBM_BACKVIDEO:
                    shared->surface->caps |= DSCAPS_FLIPPING;
                    ret = dfb_surface_reconfig( shared->surface,
                                                CSP_VIDEOONLY, CSP_VIDEOONLY );
                    break;
               case DLBM_BACKSYSTEM:
                    shared->surface->caps |= DSCAPS_FLIPPING;
                    ret = dfb_surface_reconfig( shared->surface,
                                                CSP_VIDEOONLY, CSP_SYSTEMONLY );
                    break;
               case DLBM_FRONTONLY:
                    shared->surface->caps &= ~DSCAPS_FLIPPING;
                    ret = dfb_surface_reconfig( shared->surface,
                                                CSP_VIDEOONLY, CSP_VIDEOONLY );
                    break;
               
               default:
                    BUG("unknown buffermode");
                    return DFB_BUG;
          }
          
          if (ret)
               return ret;
     }

     ret = dfb_surface_reformat( shared->surface, config->width,
                                 config->height, config->pixelformat );
     if (ret)
          return ret;

          shared->surface->caps &= ~DSCAPS_INTERLACED;

     return DFB_OK;
}

static DFBResult
deallocate_surface( DisplayLayer *layer )
{
     DisplayLayerShared *shared;
     CoreSurface        *surface;

     DFB_ASSERT( layersfield != NULL );
     DFB_ASSERT( layer != NULL );
     DFB_ASSERT( layer->funcs != NULL );
     DFB_ASSERT( layer->shared != NULL );
     DFB_ASSERT( layer->shared->surface != NULL );

     shared  = layer->shared;
     surface = shared->surface;
     
     shared->surface = NULL;

     if (layer->funcs->DeallocateSurface)
          return layer->funcs->DeallocateSurface( layer, layer->driver_data,
                                                  layer->layer_data, surface );
     
     return DFB_OK;
}
