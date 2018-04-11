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

#ifndef __LAYERS_H__
#define __LAYERS_H__

#include <dfb_types.h>
#include <external.h>
#include <directfb.h>
#include <core/coretypes.h>

typedef struct {
     DFBDisplayLayerDescription  desc;  /* description of the layer's caps */
} DisplayLayerInfo;

typedef struct {
     int       (*LayerDataSize)     (void);
     
     DFBResult (*InitLayer)         ( GraphicsDevice             *device,
                                      DisplayLayer               *layer,
                                      DisplayLayerInfo           *layer_info,
                                      DFBDisplayLayerConfig      *default_config,
                                      DFBColorAdjustment         *default_adj,
                                      void                       *driver_data,
                                      void                       *layer_data );

     /*
      * internal layer driver API
      */

     DFBResult (*Enable)            ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data );

     DFBResult (*Disable)           ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data );

     DFBResult (*TestConfiguration) ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data,
                                      DFBDisplayLayerConfig      *config,
                                      DFBDisplayLayerConfigFlags *failed );

     DFBResult (*SetConfiguration)  ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data,
                                      DFBDisplayLayerConfig      *config );

     DFBResult (*FlipBuffers)       ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data,
                                      DFBSurfaceFlipFlags         flags );
     
     DFBResult (*UpdateRegion)      ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data,
                                      DFBRegion                  *region,
                                      DFBSurfaceFlipFlags         flags );
     
     DFBResult (*SetPalette)        ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data,
                                      CorePalette                *palette );

     /*
      * optional to override default surface (re)allocation
      */

     DFBResult (*AllocateSurface) ( DisplayLayer               *layer,
                                    void                       *driver_data,
                                    void                       *layer_data,
                                    DFBDisplayLayerConfig      *config,
                                    CoreSurface               **surface );

     DFBResult (*ReallocateSurface) ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data,
                                      DFBDisplayLayerConfig      *config,
                                      CoreSurface                *surface );

     DFBResult (*DeallocateSurface) ( DisplayLayer               *layer,
                                      void                       *driver_data,
                                      void                       *layer_data,
                                      CoreSurface                *surface );
} DisplayLayerFuncs;


/*
 * Add a layer to a graphics device by pointing to a table
 * containing driver functions. The supplied driver data
 * will be passed to these functions.
 */
LIBEXPORT DFBResult dfb_layers_register( GraphicsDevice    *device,
                          void              *driver_data,
                          DisplayLayerFuncs *funcs );

typedef DFBEnumerationResult (*DisplayLayerCallback) (DisplayLayer *layer,
                                                      void         *ctx);

void dfb_layers_enumerate( DisplayLayerCallback  callback,
                           void                 *ctx );

DisplayLayer *dfb_layer_at( DFBDisplayLayerID id );

/*
 * Release layer after lease/purchase.
 * Repaints the window stack if 'repaint' is true.
 */
void dfb_layer_release( DisplayLayer *layer, bool repaint );


/*
 * enable/disable layer
 */
DFBResult dfb_layer_enable( DisplayLayer *layer );
DFBResult dfb_layer_disable( DisplayLayer *layer );

/*
 * configuration testing/setting/getting
 */
DFBResult dfb_layer_test_configuration( DisplayLayer               *layer,
                                        DFBDisplayLayerConfig      *config,
                                        DFBDisplayLayerConfigFlags *failed );

DFBResult dfb_layer_set_configuration( DisplayLayer          *layer,
                                       DFBDisplayLayerConfig *config );

DFBResult dfb_layer_get_configuration( DisplayLayer          *layer,
                                       DFBDisplayLayerConfig *config );

/*
 * various functions
 */
LIBEXPORT CoreSurface       *dfb_layer_surface( const DisplayLayer *layer );
CardState         *dfb_layer_state( DisplayLayer *layer );
void               dfb_layer_description( const DisplayLayer         *layer,
                                          DFBDisplayLayerDescription *desc );
DFBDisplayLayerID  dfb_layer_id( const DisplayLayer *layer );

DFBResult          dfb_layer_flip_buffers ( DisplayLayer        *layer,
                                            DFBSurfaceFlipFlags  flags );
DFBResult          dfb_layer_update_region( DisplayLayer        *layer,
                                            DFBRegion           *region,
                                            DFBSurfaceFlipFlags  flags );

#endif
