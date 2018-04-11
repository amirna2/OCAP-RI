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

#include <mpeos_dbg.h>

#include <config.h>

#include <stdio.h>

#include <directfb.h>

#include <core/coredefs.h>
#include <core/coretypes.h>
#include <core/layers.h>
#include <core/palette.h>
#include <core/surfaces.h>
#include <core/system.h>
#include <core/gfxcard.h>

#include <gfx/convert.h>

#include <misc/conf.h>
#include <misc/util.h>

#include "rip_display.h"
#include "vdfb_primary.h"

static int
primaryLayerDataSize(void);

static DFBResult
primaryInitLayer(GraphicsDevice *device, DisplayLayer *layer,
        DisplayLayerInfo *layer_info, DFBDisplayLayerConfig *default_config,
        DFBColorAdjustment *default_adj, void *driver_data, void *layer_data);

static DFBResult
primaryEnable(DisplayLayer *layer, void *driver_data, void *layer_data);

static DFBResult
primaryDisable(DisplayLayer *layer, void *driver_data, void *layer_data);

static DFBResult
primaryTestConfiguration(DisplayLayer *layer, void *driver_data,
        void *layer_data, DFBDisplayLayerConfig *config,
        DFBDisplayLayerConfigFlags *failed);

static DFBResult
primarySetConfiguration(DisplayLayer *layer, void *driver_data,
        void *layer_data, DFBDisplayLayerConfig *config);

static DFBResult
primaryFlipBuffers(DisplayLayer *layer, void *driver_data, void *layer_data,
        DFBSurfaceFlipFlags flags);

static DFBResult
primaryUpdateRegion(DisplayLayer *layer, void *driver_data, void *layer_data,
        DFBRegion *region, DFBSurfaceFlipFlags flags);

static DFBResult
primarySetPalette(DisplayLayer *layer, void *driver_data, void *layer_data,
        CorePalette *palette);

static DFBResult
primaryAllocateSurface(DisplayLayer *layer, void *driver_data,
        void *layer_data, DFBDisplayLayerConfig *config, CoreSurface **surface);

static DFBResult
primaryReallocateSurface(DisplayLayer *layer, void *driver_data,
        void *layer_data, DFBDisplayLayerConfig *config, CoreSurface *surface);

static DFBResult
primaryDeallocateSurface(DisplayLayer *layer, void *driver_data,
        void *layer_data, CoreSurface *surface);

DisplayLayerFuncs vidfbPrimaryLayerFuncs =
{ primaryLayerDataSize, primaryInitLayer, primaryEnable, primaryDisable,
        primaryTestConfiguration, primarySetConfiguration, primaryFlipBuffers,
        primaryUpdateRegion, primarySetPalette, primaryAllocateSurface,
        primaryReallocateSurface, primaryDeallocateSurface, };

static DFBResult
update_screen(CoreSurface *surface, int x, int y, int w, int h);

//static SharedMemoryFBSession *g_framebuffer = NULL;
static RIPDisplaySession *g_framebuffer = NULL;

/******************************************************************************/
/** First, dummy functions **/
/******************************************************************************/

void GetGraphicsDriverFuncs(GraphicsDriverFuncs *graphics_driver_funcs)
{
} // END GetGraphicsDriverFuncs()

/******************************************************************************/
/** primary layer functions **/
/******************************************************************************/

/******************************************************************************/

static int primaryLayerDataSize(void)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DIRECTFB,
            "<<GFX>> primaryLayerDataSize: returning 0");
    return 0;
} // END primaryLayerDataSize()

/******************************************************************************/

static DFBResult primaryInitLayer(GraphicsDevice *device, DisplayLayer *layer,
        DisplayLayerInfo *layer_info, DFBDisplayLayerConfig *default_config,
        DFBColorAdjustment *default_adj, void *driver_data, void *layer_data)
{
    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DIRECTFB,
            "<<GFX>> primaryInitLayer: device 0x%p, layer 0x%p (%s)\n",
            device, layer, layer_info->desc.name);

    /*
     // Describe our surface to DirectFB
     */

    /* set capabilities and type */
    layer_info->desc.caps = DLCAPS_SURFACE;
    layer_info->desc.type = DLTF_GRAPHICS;

    /* set name */
    snprintf(layer_info->desc.name, DFB_DISPLAY_LAYER_DESC_NAME_LENGTH,
            "PrimaryLayer");

    /* fill out the default configuration */
    default_config->flags = DLCONF_WIDTH | DLCONF_HEIGHT | DLCONF_PIXELFORMAT
            | DLCONF_BUFFERMODE;
    default_config->buffermode = DLBM_FRONTONLY; /* or DLBM_BACKVIDEO, DLBM_BACKSYSTEM */

    if (port_dfbConfig->mode.width)
        default_config->width = port_dfbConfig->mode.width;
    else
        default_config->width = 640;

    if (port_dfbConfig->mode.height)
        default_config->height = port_dfbConfig->mode.height;
    else
        default_config->height = 480;

    /* Regardless of what is requested, the MCE IiTvDisplay interface only supports ARGB32 */
    default_config->pixelformat = DSPF_ARGB;

    /*
     // Setup the actual surface memory - in shared memory
     */
    //MPEOS_LOG( MPE_LOG_DEBUG, MPE_MOD_DIRECTFB,
    //           "<<GFX>> primaryInitLayer: Calling WFB_SetDisplayMode for %dx%d %d bpp video mode\n",
    //           default_config->width, default_config->height,
    //           DFB_BITS_PER_PIXEL(default_config->pixelformat) );

    //if ( (result = initSharedGraphicsMemory( default_config->width,
    //                                         default_config->height,
    //                                         DFB_BYTES_PER_PIXEL(default_config->pixelformat),
    //                                         DFB_BITS_PER_PIXEL(default_config->pixelformat) ) )
    //     != TRUE )
    //{
    //        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DIRECTFB, "Couldn't initialize %dx%dx%d shared memory\n",
    //                 default_config->width, default_config->height,
    //                 DFB_BITS_PER_PIXEL(default_config->pixelformat) );
    //        return DFB_FAILURE;
    //}

    //     InitSharedMemoryFB();
    (void) InitSurface();

    return DFB_OK;
} // END primaryInitLayer()

/******************************************************************************/

static DFBResult primaryEnable(DisplayLayer *layer, void *driver_data,
        void *layer_data)
{
    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primaryEnable: layer 0x%p, driver_data 0x%p, layer_data 0x%p\n",
            layer, driver_data, layer_data);

    /* always enabled */
    return DFB_OK;
} // END primaryEnable()

/******************************************************************************/

static DFBResult primaryDisable(DisplayLayer *layer, void *driver_data,
        void *layer_data)
{
    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primaryDisable: layer 0x%p, driver_data 0x%p, layer_data 0x%p\n",
            layer, driver_data, layer_data);

    /* cannot be disabled */
    return DFB_UNSUPPORTED;
} // END primaryDisable()

/******************************************************************************/

static DFBResult primaryAllocateSurface(DisplayLayer *layer, void *driver_data,
        void *layer_data, DFBDisplayLayerConfig *config,
        CoreSurface **ret_surface)
{
    DFBSurfaceCapabilities caps = DSCAPS_SYSTEMONLY;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DIRECTFB,
            "<<GFX>> primaryAllocateSurface: layer 0x%p, %dx%d %d bpp\n",
            layer, config->width, config->height, DFB_BITS_PER_PIXEL(
                    config->pixelformat));

    if (config->buffermode != DLBM_FRONTONLY)
        caps |= DSCAPS_FLIPPING;
    /*
     if ( AllocateSharedMemoryFB( config->width, config->height,
     DFB_BITS_PER_PIXEL(config->pixelformat),
     DFB_BYTES_PER_PIXEL(config->pixelformat),
     ( (config->buffermode == DLBM_FRONTONLY)
     ? SMFB_SINGLEBUFFER : SMFB_DOUBLEBUFFER ),
     &g_framebuffer )
     != SMFB_SUCCESS )
     {
     g_framebuffer = NULL;
     return DFB_FAILURE;
     }
     */
    if (AllocateSurface(config->width, config->height, DFB_BITS_PER_PIXEL(
            config->pixelformat), DFB_BYTES_PER_PIXEL(config->pixelformat),
            ((config->buffermode == DLBM_FRONTONLY) ? RIPD_SINGLEBUFFER
                    : RIPD_DOUBLEBUFFER), &g_framebuffer) != RIPD_SUCCESS)
    {
        g_framebuffer = NULL;
        return DFB_FAILURE;
    }

    // Assert: g_framebuffer is alive and well

    // TODO: FIXME FOR DOUBLE-BUFFERING

    return (*dfb_surface_create_preallocated_ptr)(config->width,
            config->height, config->pixelformat, CSP_SYSTEMONLY, caps, NULL,
            g_framebuffer->primaryBuf, g_framebuffer->primaryBuf, /* FIXME */
            g_framebuffer->ripDisplay->bytesPerLine,
            g_framebuffer->ripDisplay->bytesPerLine, /* FIXME */
            //                                g_framebuffer->shmemfb->bytesPerLine,
            //                                g_framebuffer->shmemfb->bytesPerLine, /* FIXME */
            ret_surface);
} // END primaryAllocateSurface()

/******************************************************************************/

static DFBResult primaryReallocateSurface(DisplayLayer *layer,
        void *driver_data, void *layer_data, DFBDisplayLayerConfig *config,
        CoreSurface *surface)
{
#if 0
    DFBResult ret;

    /* FIXME: write surface management functions
     for easier configuration changes */

    switch (config->buffermode)
    {
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
        MPEOS_LOG(MPE_LOG_FATAL,MPE_MOD_DIRECTFB,"unknown buffermode");
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

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primaryReallocateSurface: layer 0x%p, from surface 0x%p (%dx%d %d bpp) to %dx%d %d bpp\n",
            layer, surface, surface->width, surface->height,
            DFB_BITS_PER_PIXEL(surface->format), config->width, config->height,
            DFB_BITS_PER_PIXEL(config->pixelformat));

    surface->width = config->width;
    surface->height = config->height;
    surface->format = config->pixelformat;

    switch (config->buffermode)
    {
    case DLBM_BACKVIDEO:
    case DLBM_BACKSYSTEM:
        surface->caps |= DSCAPS_FLIPPING;
        break;

    case DLBM_FRONTONLY:
        surface->caps &= ~DSCAPS_FLIPPING;
        break;

    default:
        MPEOS_LOG(MPE_LOG_FATAL, MPE_MOD_DIRECTFB, "unknown buffermode");
        return DFB_BUG;
    }

    // refresh g_framebuffer, and write new buffer addr and pitch into DisplayLayer

    GetCurrentPrimaryBuffer(&(surface->front_buffer->system.addr));

    surface->front_buffer->system.pitch = DFB_BYTES_PER_LINE(surface->format,
            DFB_MAX(surface->width, surface->min_width));
    surface->back_buffer->system.addr = surface->front_buffer->system.addr;
    surface->back_buffer->system.pitch = surface->front_buffer->system.pitch;

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primaryReallocateSurface: setting system.addr to %p and system.pitch to %d\n",
            surface->front_buffer->system.addr,
            surface->front_buffer->system.pitch);

    if (DFB_PIXELFORMAT_IS_INDEXED(config->pixelformat) && !surface->palette)
    {
        DFBResult ret;
        CorePalette *palette;

        ret = (*dfb_palette_create_ptr)(256, &palette);
        if (ret)
            return ret;

        if (config->pixelformat == DSPF_LUT8)
            (*dfb_palette_generate_rgb332_map_ptr)(palette);

        (void) (*dfb_surface_set_palette_ptr)(surface, palette);

        //dfb_palette_unref( palette );
        (void) (*fusion_object_unref_ptr)(&palette->object);
    }

    return DFB_OK;
} // END primaryReallocateSurface()

/******************************************************************************/

static DFBResult primarySetConfiguration(DisplayLayer *layer,
        void *driver_data, void *layer_data, DFBDisplayLayerConfig *config)
{
    //CoreSurface *surface = (*dfb_layer_surface_ptr)( layer );

    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primarySetConfiguration: layer 0x%p, config 0x%p (%dx%d %d bpp)\n",
            layer, config, config->width, config->height, DFB_BITS_PER_PIXEL(
                    config->pixelformat));

    /* Set video mode */
    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primarySetConfiguration: Calling WFB_SetDisplayMode for %dx%dx%d video mode\n",
            config->width, config->height, DFB_BITS_PER_PIXEL(
                    config->pixelformat));

    ///* Set video mode */
    //if ( (result = WFB_SetDisplayMode(config->width,
    //                              config->height,
    //                              DFB_BYTES_PER_PIXEL(config->pixelformat),
    //                              DFB_BITS_PER_PIXEL(config->pixelformat),
    //                              &screen)) != TRUE ) {
    //        MPEOS_LOG(MPE_LOG_ERROR,MPE_MOD_DIRECTFB,"Couldn't set %dx%dx%d video mode: %s\n",
    //                 config->width, config->height,
    //                 DFB_BITS_PER_PIXEL(config->pixelformat), result);
    //        return DFB_FAILURE;
    //}

    //surface->back_buffer->system.addr  = g_framebuffer->pixels;
    //surface->back_buffer->system.pitch = g_framebuffer->pitch;
    //
    //surface->front_buffer->system.addr  = g_framebuffer->pixels;
    //surface->front_buffer->system.pitch = g_framebuffer->pitch;

    return DFB_OK;
} // END primarySetConfiguration()

/******************************************************************************/

static DFBResult primaryTestConfiguration(DisplayLayer *layer,
        void *driver_data, void *layer_data, DFBDisplayLayerConfig *config,
        DFBDisplayLayerConfigFlags *failed)
{
    DFBDisplayLayerConfigFlags fail = 0;

    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primaryTestConfiguration: layer 0x%p, driver_data 0x%p, layer_data 0x%p, config 0x%p (%dx%d %d bpp)\n",
            layer, driver_data, layer_data, config, config->width,
            config->height, DFB_BITS_PER_PIXEL(config->pixelformat));

    if (failed)
        *failed = fail;

    if (fail)
        return DFB_UNSUPPORTED;

    return DFB_OK;
} // END primaryTestConfiguration()

/******************************************************************************/

static DFBResult primaryUpdateRegion(DisplayLayer *layer, void *driver_data,
        void *layer_data, DFBRegion *region, DFBSurfaceFlipFlags flags)
{
    CoreSurface *surface = (*dfb_layer_surface_ptr)(layer);

    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primaryUpdateRegion: layer 0x%p, region (%d,%d)-(%d,%d), flags 0x%08x\n",
            layer, region->x1, region->y1, region->x2, region->y2, flags);

    if (!region)
        return update_screen(surface, 0, 0, surface->width, surface->height);

    return update_screen(surface, region->x1, region->y1, region->x2
            - region->x1 + 1, region->y2 - region->y1 + 1);
} // END primaryUpdateRegion()

/******************************************************************************/

static DFBResult primarySetPalette(DisplayLayer *layer, void *driver_data,
        void *layer_data, CorePalette *palette)
{
#ifdef NOTYET
    int i;
    Win32FB_Color colors[palette->num_entries];

    for (i=0; i<palette->num_entries; i++)
    {
        colors[i].r = palette->entries[i].r;
        colors[i].g = palette->entries[i].g;
        colors[i].b = palette->entries[i].b;
    }

    Win32FB_SetColors( screen, colors, 0, palette->num_entries );
#endif

    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primarySetPalette: layer 0x%p, driver_data 0x%p, layer_data 0x%p\n",
            layer, driver_data, layer_data);

    return DFB_OK;
} // primarySetPalette()

/******************************************************************************/

static DFBResult primaryFlipBuffers(DisplayLayer *layer, void *driver_data,
        void *layer_data, DFBSurfaceFlipFlags flags)
{
    CoreSurface *surface = (*dfb_layer_surface_ptr)(layer);

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DIRECTFB,
            "<<GFX>> primaryFlipBuffers: layer 0x%p, flags 0x%08x\n", layer,
            flags);

    // Signal a refresh
    //RefreshSharedMemoryFB(g_framebuffer, SMFB_PRIMARY);
    RefreshSurface(g_framebuffer, RIPD_PRIMARY);

    // Swap surface->front_buffer and surface->back_buffer pointers
    (*dfb_surface_flip_buffers_ptr)(surface);

    //if (WFB_Flip() != TRUE)
    //     return DFB_FAILURE;

    return DFB_OK;
} // END primaryFlipBuffers()

/******************************************************************************/

static DFBResult primaryDeallocateSurface(DisplayLayer *layer,
        void *driver_data, void *layer_data, CoreSurface *surface)
{
    MPEOS_LOG(
            MPE_LOG_TRACE1,
            MPE_MOD_DIRECTFB,
            "<<GFX>> primaryDeallocateSurface: layer 0x%p, surface 0x%p (%dx%d %d bpp)\n",
            layer, surface, surface->width, surface->height,
            DFB_BITS_PER_PIXEL(surface->format));

    // Anything to do here?
    return DFB_OK;
} // END primaryDeallocateSurface()

/******************************************************************************/

static DFBResult update_screen(CoreSurface *surface, int x, int y, int w, int h)
{
#ifdef NOTYET
#if 0
    int i;
    void *dst;
    void *src;
    int pitch;
    DFBResult ret;

    //     DFB_ASSERT( surface != NULL );

    if (Win32FB_LockSurface( screen ) < 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR,MPE_MOD_DIRECTFB, "DirectFB/Win32FB: "
                "Couldn't lock the display surface: %s\n", WFB_GetError() );
        return DFB_FAILURE;
    }

    ret = dfb_surface_soft_lock( surface, DSLF_READ, &src, &pitch, true );
    if (ret)
    {
        MPEOS_LOG(MPE_LOG_ERROR,MPE_MOD_DIRECTFB, "DirectFB/Win32FB: Couldn't lock layer surface: %s\n",
                DirectFBErrorString( ret ) );
        Win32FB_UnlockSurface(screen);
        return ret;
    }

    dst = screen->pixels;

    src += DFB_BYTES_PER_LINE( surface->format, x ) + y * pitch;
    dst += DFB_BYTES_PER_LINE( surface->format, x ) + y * screen->pitch;

    for (i=0; i<h; ++i)
    {
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

    MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DIRECTFB,
            "<<GFX>> update_screen: surface 0x%p (%dx%d %d bpp)\n", surface,
            surface->width, surface->height,
            DFB_BITS_PER_PIXEL(surface->format));

    //if (WFB_Flip() != TRUE)
    //        return DFB_FAILURE;

    return DFB_OK;
}
