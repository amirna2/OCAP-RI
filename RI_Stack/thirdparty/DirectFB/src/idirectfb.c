/*
   (c) Copyright 2000  convergence integrated media GmbH.
   All rights reserved.

   Written by Denis Oliver Kropp <dok@convergence.de> and
              Andreas Hundt <andi@convergence.de>.

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
#include <directfb_version.h>
#include <directfb_internals.h>
#include <core/core.h>
#include <core/coretypes.h>
#include <core/state.h>
#include <core/gfxcard.h>
#include <core/layers.h>
#include <core/palette.h>
#include <core/surfaces.h>
#include <core/surfacemanager.h>
#include <core/system.h>
#include <display/idirectfbpalette.h>
#include <display/idirectfbsurface.h>
#include <display/idirectfbsurface_layer.h>
#include <display/idirectfbdisplaylayer.h>
#include <media/idirectfbfont.h>
#include <idirectfb.h>
#include <gfx/convert.h>
#include <misc/conf.h>
#include <misc/mem.h>
#include <misc/util.h>

/*
 * private data struct of IDirectFB
 */
typedef struct {
     int                  ref;      /* reference counter */
     DFBCooperativeLevel  level;    /* current cooperative level */

     DisplayLayer        *layer;    /* primary display layer */

     struct {
          int             width;    /* IDirectFB stores window width    */
          int             height;   /* and height and the pixel depth   */
          int             bpp;      /* from SetVideoMode() parameters.  */
     } primary;
} IDirectFB_data;

typedef struct {
     DFBDisplayLayerCallback  callback;
     void                    *callback_ctx;
} EnumDisplayLayers_Context;

typedef struct {
     IDirectFBDisplayLayer **interface;
     DFBDisplayLayerID       id;
     DFBResult               ret;
} GetDisplayLayer_Context;

static DFBEnumerationResult EnumDisplayLayers_Callback( DisplayLayer *layer,
                                                        void         *ctx );
static DFBEnumerationResult GetDisplayLayer_Callback  ( DisplayLayer *layer,
                                                        void         *ctx );
/*
 * Destructor
 *
 * Free data structure and set the pointer to NULL,
 * to indicate the dead interface.
 */
static void
IDirectFB_Destruct( IDirectFB *thiz )
{
     dfb_core_unref();     /* TODO: where should we place this call? */

     idirectfb_singleton = NULL;

     DFB_DEALLOCATE_INTERFACE( thiz );
}


static DFBResult
IDirectFB_AddRef( IDirectFB *thiz )
{
     INTERFACE_GET_DATA(IDirectFB)

     data->ref++;

     return DFB_OK;
}

static DFBResult
IDirectFB_Release( IDirectFB *thiz )
{
     INTERFACE_GET_DATA(IDirectFB)

     if (--data->ref == 0)
          IDirectFB_Destruct( thiz );

     return DFB_OK;
}

static DFBResult
IDirectFB_GetCardCapabilities( IDirectFB           *thiz,
                               DFBCardCapabilities *caps )
{
     CardCapabilities card_caps;

     INTERFACE_GET_DATA(IDirectFB)

     if (!caps)
          return DFB_INVARG;

     card_caps = dfb_gfxcard_capabilities();

     caps->acceleration_mask = card_caps.accel;
     caps->blitting_flags    = card_caps.blitting;
     caps->drawing_flags     = card_caps.drawing;
     caps->video_memory      = dfb_gfxcard_memory_length();

     return DFB_OK;
}

static DFBResult
IDirectFB_SetVideoMode( IDirectFB    *thiz,
                        unsigned int  width,
                        unsigned int  height,
                        unsigned int  bpp )
{
     INTERFACE_GET_DATA(IDirectFB)

     if (!width || !height || !bpp)
          return DFB_INVARG;

     switch (data->level) {

          case DFSCL_FULLSCREEN:
          {
               DFBResult ret;
               DFBDisplayLayerConfig config;

               config.width       = width;
               config.height      = height;
               config.pixelformat = dfb_pixelformat_for_depth( bpp );

               if (config.pixelformat == DSPF_UNKNOWN)
                    return DFB_INVARG;

               config.flags = DLCONF_WIDTH | DLCONF_HEIGHT | DLCONF_PIXELFORMAT;

               ret = dfb_layer_set_configuration( data->layer, &config );
               if (ret)
                    return ret;

               break;
          }
     }

     data->primary.width  = width;
     data->primary.height = height;
     data->primary.bpp    = bpp;

     return DFB_OK;
}

static void
init_palette( CoreSurface *surface, DFBSurfaceDescription *desc )
{
     int          num;
     CorePalette *palette = surface->palette;

     if (!palette || !(desc->flags & DSDESC_PALETTE))
          return;

     num = DFB_MIN( desc->palette.size, (unsigned int)palette->num_entries );

     dfb_memcpy( palette->entries,
                 desc->palette.entries, num * sizeof(DFBColor));

     dfb_palette_update( palette, 0, num - 1 );
}

static DFBResult
IDirectFB_CreateSurface( IDirectFB              *thiz,
                         DFBSurfaceDescription  *desc,
                         IDirectFBSurface      **interface )
{
     DFBResult ret = DFB_OK;
     unsigned int width = 256;
     unsigned int height = 256;
     int policy = CSP_VIDEOLOW;
     DFBSurfacePixelFormat format;
     DFBSurfaceCapabilities caps = 0;
     DFBDisplayLayerConfig  config;
     CoreSurface *surface = NULL;

     INTERFACE_GET_DATA(IDirectFB)

     dfb_layer_get_configuration( data->layer, &config );

     format = config.pixelformat;

     if (!desc || !interface)
          return DFB_INVARG;

     if (desc->flags & DSDESC_WIDTH) {
          width = desc->width;
          if (!width)
               return DFB_INVARG;
     }
     if (desc->flags & DSDESC_HEIGHT) {
          height = desc->height;
          if (!height)
               return DFB_INVARG;
     }

     if (desc->flags & DSDESC_PALETTE)
          if (!desc->palette.entries || !desc->palette.size)
               return DFB_INVARG;
     
     if (desc->flags & DSDESC_CAPS)
          caps = desc->caps;

     if (desc->flags & DSDESC_PIXELFORMAT)
          format = desc->pixelformat;

     switch (format) {
          case DSPF_A8:
          case DSPF_ARGB:
          case DSPF_ARGB1555:
          case DSPF_I420:
          case DSPF_LUT8:
          case DSPF_RGB16:
          case DSPF_RGB24:
          case DSPF_RGB32:
#ifdef SUPPORT_RGB332
          case DSPF_RGB332:
#endif
          case DSPF_UYVY:
          case DSPF_YUY2:
          case DSPF_YV12:
               break;

          default:
               return DFB_INVARG;
     }

     if (caps & DSCAPS_PRIMARY) {
          if (desc->flags & DSDESC_PREALLOCATED)
               return DFB_INVARG;

          /* FIXME: should we allow to create more primaries in windowed mode?
                    should the primary surface be a singleton?
                    or should we return an error? */
          switch (data->level) {
               case DFSCL_FULLSCREEN:
                    config.flags |= DLCONF_BUFFERMODE;

                    if (caps & DSCAPS_FLIPPING) {
                         if (caps & DSCAPS_SYSTEMONLY)
                              config.buffermode = DLBM_BACKSYSTEM;
                         else
                              config.buffermode = DLBM_BACKVIDEO;
                    }
                    else
                         config.buffermode = DLBM_FRONTONLY;

                    if (format != config.pixelformat) {
                         config.flags       |= DLCONF_PIXELFORMAT;
                         config.pixelformat  = format;
                    }
                    else if (!data->primary.bpp && dfb_config->mode.format) {
                         config.pixelformat = dfb_config->mode.format;
                    }
                    
                    /*
                     * If SetVideoMode hasn't been called,
                     * check if the user ran the app with the 'mode=' option.
                     */
                    if (!data->primary.bpp) {
                         if (dfb_config->mode.width)
                              config.width = dfb_config->mode.width;

                         if (dfb_config->mode.height)
                              config.height = dfb_config->mode.height;
                    }
                    
                    ret = dfb_layer_set_configuration( data->layer, &config );
                    if (ret) {
                         if (! (caps & (DSCAPS_SYSTEMONLY|DSCAPS_VIDEOONLY))) {
                              if (config.buffermode == DLBM_BACKVIDEO) {
                                   config.buffermode = DLBM_BACKSYSTEM;
                                   
                                   ret = dfb_layer_set_configuration( data->layer, &config );
                              }
                         }
                         
                         if (ret)
                              return ret;
                    }
                    
                    init_palette( dfb_layer_surface( data->layer ), desc );
                    
                    DFB_ALLOCATE_INTERFACE( *interface, IDirectFBSurface );
                    if (*interface == NULL)
                        return DFB_NOSYSTEMMEMORY;
                    
                    return IDirectFBSurface_Layer_Construct( *interface, NULL,
                                                             NULL, data->layer,
                                                             caps );
          }
     }


     if (caps & DSCAPS_VIDEOONLY)
          policy = CSP_VIDEOONLY;
     else if (caps & DSCAPS_SYSTEMONLY)
          policy = CSP_SYSTEMONLY;

     if (desc->flags & DSDESC_PREALLOCATED) {
          int min_pitch;

          if (policy == CSP_VIDEOONLY)
               return DFB_INVARG;

          min_pitch = DFB_BYTES_PER_LINE(format, width);

          if (!desc->preallocated[0].data ||
               desc->preallocated[0].pitch < min_pitch)
          {
               return DFB_INVARG;
          }

          if ((caps & DSCAPS_FLIPPING) &&
              (!desc->preallocated[1].data ||
                desc->preallocated[1].pitch < min_pitch))
          {
               return DFB_INVARG;
          }

          ret = dfb_surface_create_preallocated( width, height,
                                                 format, policy, caps, NULL,
                                                 desc->preallocated[0].data,
                                                 desc->preallocated[1].data,
                                                 desc->preallocated[0].pitch,
                                                 desc->preallocated[1].pitch,
                                                 &surface );
          if (ret)
               return ret;
     }
     else {
          ret = dfb_surface_create( width, height, format,
                                    policy, caps, NULL, &surface );
          if (ret)
               return ret;
     }

     init_palette( surface, desc );
     
     DFB_ALLOCATE_INTERFACE( *interface, IDirectFBSurface );
     if (*interface == NULL)
         return DFB_NOSYSTEMMEMORY;

     ret = IDirectFBSurface_Construct( *interface, NULL, NULL, surface, caps );

     dfb_surface_unref( surface );

     return ret;
}

static DFBResult
IDirectFB_CreatePalette( IDirectFB              *thiz,
                         DFBPaletteDescription  *desc,
                         IDirectFBPalette      **interface )
{
     DFBResult         ret = DFB_OK;
     IDirectFBPalette *iface;
     unsigned int      size    = 256;
     CorePalette      *palette = NULL;

     INTERFACE_GET_DATA(IDirectFB)

     if (!interface)
          return DFB_INVARG;

     if (desc && desc->flags & DPDESC_SIZE) {
          if (!desc->size)
               return DFB_INVARG;

          size = desc->size;
     }

     ret = dfb_palette_create( size, &palette );
     if (ret)
          return ret;
     
     if (desc && desc->flags & DPDESC_ENTRIES) {
          dfb_memcpy( palette->entries, desc->entries, size * sizeof(DFBColor));

          dfb_palette_update( palette, 0, size - 1 );
     }
     else
          dfb_palette_generate_rgb332_map( palette );
     
     DFB_ALLOCATE_INTERFACE( iface, IDirectFBPalette );
     if (iface == NULL)
         return DFB_NOSYSTEMMEMORY;

     ret = IDirectFBPalette_Construct( iface, palette );

     dfb_palette_unref( palette );

     if (!ret)
          *interface = iface;

     return ret;
}

static DFBResult
IDirectFB_EnumDisplayLayers( IDirectFB               *thiz,
                             DFBDisplayLayerCallback  callbackfunc,
                             void                    *callbackdata )
{
     EnumDisplayLayers_Context context;

     INTERFACE_GET_DATA(IDirectFB)

     if (!callbackfunc)
          return DFB_INVARG;

     context.callback     = callbackfunc;
     context.callback_ctx = callbackdata;

     dfb_layers_enumerate( EnumDisplayLayers_Callback, &context );

     return DFB_OK;
}

static DFBResult
IDirectFB_GetDisplayLayer( IDirectFB              *thiz,
                           DFBDisplayLayerID       id,
                           IDirectFBDisplayLayer **interface )
{
     GetDisplayLayer_Context context;

     INTERFACE_GET_DATA(IDirectFB)

     if (!interface)
          return DFB_INVARG;

     context.interface = interface;
     context.id        = id;
     context.ret       = DFB_IDNOTFOUND;

     dfb_layers_enumerate( GetDisplayLayer_Callback, &context );

     return context.ret;
}

static DFBResult
IDirectFB_CreateFontFromBuffer( IDirectFB           *thiz,
                      const char          *fontbuffer,
                      int                  fontsize,
                      DFBFontDescription  *desc,
                      IDirectFBFont      **interface )
{
     DFBResult                   ret;
     DFBInterfaceFuncs          *funcs = NULL;
     IDirectFBFont              *font;

     INTERFACE_GET_DATA(IDirectFB)

     /* Check arguments */
     if (!interface)
          return DFB_INVARG;

     if (fontbuffer) {
          if (!desc)
               return DFB_INVARG;
          if (fontsize < 1)
               return DFB_INVARG;
     }

     /* Find a suitable implemenation */
     ret = DFBGetInterface( &funcs,
                            "IDirectFBFont", NULL,
                            NULL, NULL);
     if (ret)
          return ret;

     DFB_ALLOCATE_INTERFACE( font, IDirectFBFont );
     if (font == NULL)
         return DFB_NOSYSTEMMEMORY;

     /* Construct the interface */
     ret = funcs->Construct( font, NULL, fontbuffer, fontsize, desc );

     if (ret)
          return ret;

     *interface = font;
     
     return DFB_OK;
}

/*
 * Constructor
 *
 * Fills in function pointers and intializes data structure.
 */
DFBResult
IDirectFB_Construct( IDirectFB *thiz )
{
     DFB_ALLOCATE_INTERFACE_DATA(thiz, IDirectFB)
     if (data == NULL)
         return DFB_NOSYSTEMMEMORY;

     data->ref = 1;

     data->level = DFSCL_FULLSCREEN;

     if (dfb_config->mode.width)
          data->primary.width  = dfb_config->mode.width;
     else
          data->primary.width  = 640;

     if (dfb_config->mode.height)
          data->primary.height = dfb_config->mode.height;
     else
          data->primary.height = 480;

     data->layer = dfb_layer_at( DLID_PRIMARY );

     thiz->AddRef = IDirectFB_AddRef;
     thiz->Release = IDirectFB_Release;
     thiz->GetCardCapabilities = IDirectFB_GetCardCapabilities;
     thiz->SetVideoMode = IDirectFB_SetVideoMode;
     thiz->CreateSurface = IDirectFB_CreateSurface;
     thiz->CreatePalette = IDirectFB_CreatePalette;
     thiz->EnumDisplayLayers = IDirectFB_EnumDisplayLayers;
     thiz->GetDisplayLayer = IDirectFB_GetDisplayLayer;
     thiz->CreateFontFromBuffer = IDirectFB_CreateFontFromBuffer;

     return DFB_OK;
}


/*
 * internal functions
 */

static DFBEnumerationResult
EnumDisplayLayers_Callback( DisplayLayer *layer, void *ctx )
{
     DFBDisplayLayerDescription  desc;
     EnumDisplayLayers_Context  *context = (EnumDisplayLayers_Context*) ctx;

     dfb_layer_description( layer, &desc );

     return context->callback( dfb_layer_id( layer ), desc,
                               context->callback_ctx );
}

static DFBEnumerationResult
GetDisplayLayer_Callback( DisplayLayer *layer, void *ctx )
{
     GetDisplayLayer_Context *context = (GetDisplayLayer_Context*) ctx;

     if (dfb_layer_id( layer ) != context->id)
          return DFENUM_OK;

     if ((context->ret = dfb_layer_enable( layer )) == DFB_OK) {
          DFB_ALLOCATE_INTERFACE( *context->interface, IDirectFBDisplayLayer );
          if (*context->interface == NULL)
              return DFB_NOSYSTEMMEMORY;

          IDirectFBDisplayLayer_Construct( *context->interface, layer );
     }

     return DFENUM_CANCEL;
}
