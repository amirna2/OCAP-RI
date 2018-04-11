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
#include <directfb_internals.h>
#include <core/core.h>
#include <core/coredefs.h>
#include <core/coretypes.h>
#include <core/gfxcard.h>
#include <core/fonts.h>
#include <core/state.h>
#include <core/palette.h>
#include <core/surfaces.h>
#include <core/surfacemanager.h>
#include <media/idirectfbfont.h>
#include <display/idirectfbsurface.h>
#include <display/idirectfbpalette.h>
#include <misc/util.h>
#include <misc/mem.h>
#include <gfx/convert.h>
#include <gfx/util.h>


void
IDirectFBSurface_Destruct( IDirectFBSurface *thiz )
{
     IDirectFBSurface_data *data = (IDirectFBSurface_data*)thiz->priv;

     dfb_state_set_destination( &data->state, NULL );
     dfb_state_set_source( &data->state, NULL );

     dfb_state_destroy( &data->state );
     
     if (data->surface)
     {
         CoreSurface* surface = data->surface;

         data->surface = NULL;
         dfb_surface_detach( surface, &data->reaction );
         dfb_surface_unref( surface );

         /* collect fusion object and surface data */
         fusion_object_collect( &surface->object );
     }

     if (data->font)
          data->font->Release (data->font);

     DFB_DEALLOCATE_INTERFACE( thiz );
}

static DFBResult
IDirectFBSurface_AddRef( IDirectFBSurface *thiz )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     data->ref++;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_Release( IDirectFBSurface *thiz )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (--data->ref == 0)
          IDirectFBSurface_Destruct( thiz );

     return DFB_OK;
}


static DFBResult
IDirectFBSurface_GetPixelFormat( IDirectFBSurface      *thiz,
                                 DFBSurfacePixelFormat *format )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;

     if (!format)
          return DFB_INVARG;

     *format = data->surface->format;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_GetAccelerationMask( IDirectFBSurface    *thiz,
                                      IDirectFBSurface    *source,
                                      DFBAccelerationMask *mask )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;

     if (!mask)
          return DFB_INVARG;

     if (source) {
          IDirectFBSurface_data *src_data = (IDirectFBSurface_data*)source->priv;

          dfb_state_set_source( &data->state, src_data->surface );
     }

     dfb_gfxcard_state_check( &data->state, DFXL_FILLRECTANGLE );
     dfb_gfxcard_state_check( &data->state, DFXL_DRAWRECTANGLE );
     dfb_gfxcard_state_check( &data->state, DFXL_FILLROUNDRECT );
     dfb_gfxcard_state_check( &data->state, DFXL_DRAWROUNDRECT );
     dfb_gfxcard_state_check( &data->state, DFXL_FILLOVAL );
     dfb_gfxcard_state_check( &data->state, DFXL_DRAWOVAL );
     dfb_gfxcard_state_check( &data->state, DFXL_FILLARC );
     dfb_gfxcard_state_check( &data->state, DFXL_DRAWARC );
     dfb_gfxcard_state_check( &data->state, DFXL_FILLPOLYGON );
     dfb_gfxcard_state_check( &data->state, DFXL_DRAWLINE );

     if (source) {
          dfb_gfxcard_state_check( &data->state, DFXL_BLIT );
          dfb_gfxcard_state_check( &data->state, DFXL_STRETCHBLIT );
     }

     dfb_gfxcard_state_check( &data->state, DFXL_DRAWSTRING );

     *mask = data->state.accel;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_GetSize( IDirectFBSurface *thiz,
                          unsigned int     *width,
                          unsigned int     *height )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!width && !height)
          return DFB_INVARG;

     if (width)
          *width = data->area.wanted.w;

     if (height)
          *height = data->area.wanted.h;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_GetVisibleRectangle( IDirectFBSurface *thiz,
                                      DFBRectangle     *rect )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!rect)
          return DFB_INVARG;

     rect->x = data->area.current.x - data->area.wanted.x;
     rect->y = data->area.current.y - data->area.wanted.y;
     rect->w = data->area.current.w;
     rect->h = data->area.current.h;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_GetCapabilities( IDirectFBSurface       *thiz,
                                  DFBSurfaceCapabilities *caps )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!caps)
          return DFB_INVARG;

     *caps = data->caps;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_GetPalette( IDirectFBSurface  *thiz,
                             IDirectFBPalette **interface )
{
     DFBResult         ret;
     CoreSurface      *surface;
     IDirectFBPalette *palette;

     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     if (!surface->palette)
          return DFB_UNSUPPORTED;

     if (!interface)
          return DFB_INVARG;

     DFB_ALLOCATE_INTERFACE( palette, IDirectFBPalette );
     if (palette == NULL)
         return DFB_NOSYSTEMMEMORY;

     ret = IDirectFBPalette_Construct( palette, surface->palette );
     if (ret)
          return ret;

     *interface = palette;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetPalette( IDirectFBSurface *thiz,
                             IDirectFBPalette *palette )
{
     CoreSurface           *surface;
     IDirectFBPalette_data *palette_data;

     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     if (!palette)
          return DFB_INVARG;

     if (! DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          return DFB_UNSUPPORTED;

     palette_data = (IDirectFBPalette_data*) palette->priv;
     if (!palette_data)
          return DFB_DEAD;

     if (!palette_data->palette)
          return DFB_DESTROYED;

     dfb_surface_set_palette( surface, palette_data->palette );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_Lock( IDirectFBSurface *thiz,
                       DFBSurfaceLockFlags flags,
                       void **ptr, int *pitch )
{
     int front;
     DFBResult ret;
     __u8* ptr8;

     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;


     if (!flags || !ptr || !pitch)
          return DFB_INVARG;

     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     front = (flags & DSLF_WRITE) ? 0 : 1;

     ret = dfb_surface_soft_lock( data->surface, flags, ptr, pitch, front );
     if (ret)
          return ret;

     ptr8 = (__u8*)(*ptr);
     ptr8 += data->area.current.y * (*pitch) +
             data->area.current.x *
             DFB_BYTES_PER_PIXEL(data->surface->format);
     *ptr = ptr8;

     data->locked = front + 1;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_Unlock( IDirectFBSurface *thiz )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (data->locked)
          dfb_surface_unlock( data->surface, data->locked - 1 );

     data->locked = 0;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_Flip( IDirectFBSurface    *thiz,
                       const DFBRegion     *region,
                       DFBSurfaceFlipFlags  flags )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;

     if (data->locked)
          return DFB_LOCKED;

     if (!(data->caps & DSCAPS_FLIPPING))
          return DFB_UNSUPPORTED;

     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (flags & DSFLIP_BLIT  ||  region  ||  data->caps & DSCAPS_SUBSURFACE) {
          if (region) {
               DFBRegion    reg  = *region;
               DFBRectangle rect = data->area.current;

               reg.x1 += data->area.wanted.x;
               reg.x2 += data->area.wanted.x;
               reg.y1 += data->area.wanted.y;
               reg.y2 += data->area.wanted.y;

               if (dfb_rectangle_intersect_by_unsafe_region( &rect, &reg ))
                    dfb_back_to_front_copy( data->surface, &rect );
          }
          else {
               DFBRectangle rect = data->area.current;

               dfb_back_to_front_copy( data->surface, &rect );
          }
     }
     else
          dfb_surface_flip_buffers( data->surface );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetField( IDirectFBSurface    *thiz,
                           int                  field )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;

     if (!(data->caps & DSCAPS_INTERLACED))
          return DFB_UNSUPPORTED;

     if (field < 0 || field > 1)
          return DFB_INVARG;

     dfb_surface_set_field( data->surface, field );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_Clear( IDirectFBSurface *thiz,
                        __u8 r, __u8 g, __u8 b, __u8 a )
{
     DFBColor                old_color;
     DFBSurfaceDrawingFlags  old_flags;
     DFBRectangle            rect;
     CoreSurface            *surface;
     
     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     /* save current color and drawing flags */
     old_color = data->state.color;
     old_flags = data->state.drawingflags;

     /* set drawing flags */
     if (old_flags != DSDRAW_NOFX) {
          data->state.drawingflags  = DSDRAW_NOFX;
          data->state.modified     |= SMF_DRAWING_FLAGS;
     }
     
     /* set color */
     data->state.color.r = r;
     data->state.color.g = g;
     data->state.color.b = b;
     data->state.color.a = a;
     
     if (DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          data->state.color_index = dfb_palette_search( surface->palette,
                                                        r, g, b, a );
     
     data->state.modified |= SMF_COLOR;
     
     /* fill the visible rectangle */
     rect = data->area.current;
     dfb_gfxcard_fillrectangle( &rect, &data->state );

     /* restore drawing flags */
     if (old_flags != DSDRAW_NOFX) {
          data->state.drawingflags  = old_flags;
          data->state.modified     |= SMF_DRAWING_FLAGS;
     }

     /* restore color */
     data->state.color     = old_color;
     data->state.modified |= SMF_COLOR;
     
     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetClip( IDirectFBSurface *thiz, const DFBRegion *clip )
{
     DFBRegion newclip;

     INTERFACE_GET_DATA(IDirectFBSurface)


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (clip) {
          newclip = *clip;

          newclip.x1 += data->area.wanted.x;
          newclip.x2 += data->area.wanted.x;
          newclip.y1 += data->area.wanted.y;
          newclip.y2 += data->area.wanted.y;

	  // Bug 4221: Fixes offscreen areas by creating null clip rectangle in clip_wanted area.
	  // Non-intersecting area creates null rectangle in visual area.
          if (!dfb_unsafe_region_rectangle_intersect( &newclip,
                                                      &data->area.wanted ))
	  {
	      newclip.x1 = 0;
	      newclip.y1 = 0;
	      newclip.x2 = 0;
	      newclip.y2 = 0;
              // return DFB_INVARG;
	  }

          data->clip_set = 1;
          data->clip_wanted = newclip;

          if (!dfb_region_rectangle_intersect( &newclip, &data->area.current ))
               return DFB_INVAREA;
     }
     else {
          newclip.x1 = data->area.current.x;
          newclip.y1 = data->area.current.y;
          newclip.x2 = data->area.current.x + data->area.current.w - 1;
          newclip.y2 = data->area.current.y + data->area.current.h - 1;

          data->clip_set = 0;
     }

     data->state.clip      = newclip;
     data->state.modified |= SMF_CLIP;

     return DFB_OK;
}




static DFBResult
IDirectFBSurface_SetAlphaConstant( IDirectFBSurface *thiz, __u8 a )
{
     CoreSurface *surface;
     
     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     data->state.alpha_const = a;
     data->state.modified |= SMF_ALPHA_CONST;

     return DFB_OK;
}


static DFBResult
IDirectFBSurface_SetColor( IDirectFBSurface *thiz,
                           __u8 r, __u8 g, __u8 b, __u8 a )
{
     CoreSurface *surface;
     
     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     data->state.color.a = a;
     data->state.color.r = r;
     data->state.color.g = g;
     data->state.color.b = b;

     if (DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          data->state.color_index = dfb_palette_search( surface->palette,
                                                        r, g, b, a );

     data->state.modified |= SMF_COLOR;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetColorIndex( IDirectFBSurface *thiz,
                                unsigned int      index )
{
     CoreSurface *surface;
     CorePalette *palette;

     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     if (! DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          return DFB_UNSUPPORTED;

     palette = surface->palette;
     if (!palette)
          return DFB_UNSUPPORTED;

     if (index > (unsigned int)palette->num_entries)
          return DFB_INVARG;

     data->state.color        = palette->entries[index];
     data->state.color_index  = index;
     data->state.modified    |= SMF_COLOR;

     return DFB_OK;
}

#define MAKE_BLEND_MODE_KEY(src_blend, dst_blend) ((src_blend << 8) | dst_blend)

static DFBSurfacePorterDuffRule
MapBlendFuncsToPorterDuffRule( DFBSurfaceBlendFunction src_blend,
                               DFBSurfaceBlendFunction dst_blend )
{
	switch (MAKE_BLEND_MODE_KEY(src_blend, dst_blend))
	{
		case MAKE_BLEND_MODE_KEY(DSBF_ZERO, DSBF_ZERO):
			return DSPD_CLEAR;
		case MAKE_BLEND_MODE_KEY(DSBF_ONE, DSBF_ZERO):
			return DSPD_SRC;
		case MAKE_BLEND_MODE_KEY(DSBF_ONE, DSBF_INVSRCALPHA):
			return DSPD_SRC_OVER;
		case MAKE_BLEND_MODE_KEY(DSBF_INVDESTALPHA, DSBF_ONE):
			return DSPD_DST_OVER;
		case MAKE_BLEND_MODE_KEY(DSBF_DESTALPHA, DSBF_ZERO):
			return DSPD_SRC_IN;
		case MAKE_BLEND_MODE_KEY(DSBF_ZERO, DSBF_SRCALPHA):
			return DSPD_DST_IN;
		case MAKE_BLEND_MODE_KEY(DSBF_INVDESTALPHA, DSBF_ZERO):
			return DSPD_SRC_OUT;
		case MAKE_BLEND_MODE_KEY(DSBF_ZERO, DSBF_INVSRCALPHA):
			return DSPD_DST_OUT;
		case MAKE_BLEND_MODE_KEY(DSBF_ZERO, DSBF_XOR):
			return DSPD_XOR;
	}
	return DSPD_NONE;
}

static DFBResult
IDirectFBSurface_SetSrcBlendFunction( IDirectFBSurface        *thiz,
                                      DFBSurfaceBlendFunction  src )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (data->state.src_blend != src) {
          switch (src) {
               case DSBF_ZERO:
               case DSBF_ONE:
               case DSBF_SRCCOLOR:
               case DSBF_INVSRCCOLOR:
               case DSBF_SRCALPHA:
               case DSBF_INVSRCALPHA:
               case DSBF_DESTALPHA:
               case DSBF_INVDESTALPHA:
               case DSBF_DESTCOLOR:
               case DSBF_INVDESTCOLOR:
               case DSBF_SRCALPHASAT:
               case DSBF_XOR:
					data->state.src_blend = src;
					data->state.modified |= SMF_SRC_BLEND;
					data->state.porter_duff_rule = MapBlendFuncsToPorterDuffRule(data->state.src_blend, data->state.dst_blend);
					data->state.modified |= SMF_PORTER_DUFF;
					return DFB_OK;
          }

          return DFB_INVARG;
     }

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetDstBlendFunction( IDirectFBSurface        *thiz,
                                      DFBSurfaceBlendFunction  dst )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (data->state.dst_blend != dst) {
          switch (dst) {
               case DSBF_ZERO:
               case DSBF_ONE:
               case DSBF_SRCCOLOR:
               case DSBF_INVSRCCOLOR:
               case DSBF_SRCALPHA:
               case DSBF_INVSRCALPHA:
               case DSBF_DESTALPHA:
               case DSBF_INVDESTALPHA:
               case DSBF_DESTCOLOR:
               case DSBF_INVDESTCOLOR:
               case DSBF_SRCALPHASAT:
               case DSBF_XOR:
					data->state.dst_blend = dst;
					data->state.modified |= SMF_DST_BLEND;
					data->state.porter_duff_rule = MapBlendFuncsToPorterDuffRule(data->state.src_blend, data->state.dst_blend);
					data->state.modified |= SMF_PORTER_DUFF;
					return DFB_OK;
          }

          return DFB_INVARG;
     }

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetPorterDuff( IDirectFBSurface         *thiz,
                                DFBSurfacePorterDuffRule  rule )
{
     DFBSurfaceBlendFunction src;
     DFBSurfaceBlendFunction dst;

     INTERFACE_GET_DATA(IDirectFBSurface)

     switch (rule) {
          case DSPD_NONE:
               src = DSBF_SRCALPHA;
               dst = DSBF_INVSRCALPHA;
               break;
          case DSPD_CLEAR:
               src = DSBF_ZERO;
               dst = DSBF_ZERO;
               break;
          case DSPD_SRC:
               src = DSBF_ONE;
               dst = DSBF_ZERO;
               break;
          case DSPD_SRC_OVER:
               src = DSBF_ONE;
               dst = DSBF_INVSRCALPHA;
               break;
          case DSPD_DST_OVER:
               src = DSBF_INVDESTALPHA;
               dst = DSBF_ONE;
               break;
          case DSPD_SRC_IN:
               src = DSBF_DESTALPHA;
               dst = DSBF_ZERO;
               break;
          case DSPD_DST_IN:
               src = DSBF_ZERO;
               dst = DSBF_SRCALPHA;
               break;
          case DSPD_SRC_OUT:
               src = DSBF_INVDESTALPHA;
               dst = DSBF_ZERO;
               break;
          case DSPD_DST_OUT:
               src = DSBF_ZERO;
               dst = DSBF_INVSRCALPHA;
               break;
          case DSPD_XOR:
               src = DSBF_ZERO;
               dst = DSBF_XOR;
               break;
          default:
               return DFB_INVARG;
     }

	if (data->state.src_blend != src) {
		data->state.src_blend = src;
		data->state.modified |= SMF_SRC_BLEND;
	}

	if (data->state.dst_blend != dst) {
		data->state.dst_blend = dst;
		data->state.modified |= SMF_DST_BLEND;
	}

	if (data->state.porter_duff_rule != rule) {
		data->state.porter_duff_rule = rule;
		data->state.modified |= SMF_PORTER_DUFF;
	}

	return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetSrcColorKey( IDirectFBSurface *thiz,
                                 __u8              r,
                                 __u8              g,
                                 __u8              b )
{
     CoreSurface *surface;

     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     data->src_key.r = r;
     data->src_key.g = g;
     data->src_key.b = b;
     
     if (DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          data->src_key.value = dfb_palette_search( surface->palette,
                                                    r, g, b, 0x80 );
     else
          data->src_key.value = dfb_color_to_pixel( surface->format, r, g, b );

     /* The new key won't be applied to this surface's state.
        The key will be taken by the destination surface to apply it
        to its state when source color keying is used. */

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetSrcColorKeyIndex( IDirectFBSurface *thiz,
                                      unsigned int      index )
{
     CoreSurface *surface;
     CorePalette *palette;

     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     if (! DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          return DFB_UNSUPPORTED;

     palette = surface->palette;
     if (!palette)
          return DFB_UNSUPPORTED;

     if (index > (unsigned int)palette->num_entries)
          return DFB_INVARG;
     
     data->src_key.r = palette->entries[index].r;
     data->src_key.g = palette->entries[index].g;
     data->src_key.b = palette->entries[index].b;
     
     data->src_key.value = index;

     /* The new key won't be applied to this surface's state.
        The key will be taken by the destination surface to apply it
        to its state when source color keying is used. */

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetDstColorKey( IDirectFBSurface *thiz,
                                 __u8              r,
                                 __u8              g,
                                 __u8              b )
{
     CoreSurface *surface;

     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     data->dst_key.r = r;
     data->dst_key.g = g;
     data->dst_key.b = b;

     if (DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          data->dst_key.value = dfb_palette_search( surface->palette,
                                                    r, g, b, 0x80 );
     else
          data->dst_key.value = dfb_color_to_pixel( surface->format, r, g, b );

     if (data->state.dst_colorkey != data->dst_key.value) {
          data->state.dst_colorkey = data->dst_key.value;
          data->state.modified    |= SMF_DST_COLORKEY;
     }

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetDstColorKeyIndex( IDirectFBSurface *thiz,
                                      unsigned int      index )
{
     CoreSurface *surface;
     CorePalette *palette;

     INTERFACE_GET_DATA(IDirectFBSurface)

     surface = data->surface;
     if (!surface)
          return DFB_DESTROYED;

     if (! DFB_PIXELFORMAT_IS_INDEXED( surface->format ))
          return DFB_UNSUPPORTED;

     palette = surface->palette;
     if (!palette)
          return DFB_UNSUPPORTED;

     if (index > (unsigned int)palette->num_entries)
          return DFB_INVARG;
     
     data->dst_key.r = palette->entries[index].r;
     data->dst_key.g = palette->entries[index].g;
     data->dst_key.b = palette->entries[index].b;
     
     data->dst_key.value = index;

     if (data->state.dst_colorkey != data->dst_key.value) {
          data->state.dst_colorkey = data->dst_key.value;
          data->state.modified    |= SMF_DST_COLORKEY;
     }

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetFont( IDirectFBSurface *thiz,
                          IDirectFBFont    *font )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (data->locked)
          return DFB_LOCKED;

     if (font)
          font->AddRef (font);

     if (data->font)
          data->font->Release (data->font);

     data->font = font;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_GetFont( IDirectFBSurface  *thiz,
                          IDirectFBFont    **font )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!font)
          return DFB_INVARG;

     if (!data->font) {
      *font = NULL;
          return DFB_MISSINGFONT;
     }

     data->font->AddRef (data->font);
     *font = data->font;

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetDrawingFlags( IDirectFBSurface       *thiz,
                                  DFBSurfaceDrawingFlags  flags )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (data->state.drawingflags != flags) {
          data->state.drawingflags = flags;
          data->state.modified |= SMF_DRAWING_FLAGS;
     }

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_FillRectangle( IDirectFBSurface *thiz,
                                int x, int y, int w, int h )
{
     DFBRectangle rect;

     
     INTERFACE_GET_DATA(IDirectFBSurface)

     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;
     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_fillrectangle( &rect, &data->state );

     return DFB_OK;
}


static DFBResult
IDirectFBSurface_FillRoundRect( IDirectFBSurface *thiz,
                                int x, int y, int w, int h, int xd, int yd )
{
     DFBRectangle rect;
     DFBDimension oval;
              
     INTERFACE_GET_DATA(IDirectFBSurface)

     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;

     oval.w = xd;
     oval.h = yd;

     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

	 if (oval.w < 0)
		 oval.w = 0;

	 if (oval.h < 0)
		 oval.h = 0;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_fillroundrect( &rect, &oval, &data->state );

     return DFB_OK;
}


static DFBResult
IDirectFBSurface_DrawRoundRect( IDirectFBSurface *thiz,
                                int x, int y, int w, int h, int xd, int yd )
{
     DFBRectangle rect;
     DFBDimension oval;
          
     INTERFACE_GET_DATA(IDirectFBSurface)
     
     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;

     oval.w = xd;
     oval.h = yd;


     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

	 if (oval.w < 0)
		 oval.w = 0;

	 if (oval.h < 0)
		 oval.h = 0;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_drawroundrect( &rect, &oval, &data->state );

     return DFB_OK;
}


static DFBResult
IDirectFBSurface_DrawLine( IDirectFBSurface *thiz,
                           int x1, int y1, int x2, int y2 )
{
     DFBRegion line;
      
     INTERFACE_GET_DATA(IDirectFBSurface)
     
     line.x1 = x1;
     line.y1 = y1;
     line.x2 = x2;
     line.y2 = y2;


     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     line.x1 += data->area.wanted.x;
     line.x2 += data->area.wanted.x;
     line.y1 += data->area.wanted.y;
     line.y2 += data->area.wanted.y;

     dfb_gfxcard_drawlines( &line, 1, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_DrawLines( IDirectFBSurface *thiz,
                            const DFBRegion  *lines,
                            unsigned int      num_lines )
{
     DFBRegion *local_lines;

     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (!lines || !num_lines)
          return DFB_INVARG;

     if ((local_lines = DFBMALLOC(sizeof(DFBRegion) * num_lines)) == NULL)
         return DFB_NOSYSTEMMEMORY;

     if (data->area.wanted.x || data->area.wanted.y) {
          unsigned int i;

          for (i=0; i<num_lines; i++) {
               local_lines[i].x1 = lines[i].x1 + data->area.wanted.x;
               local_lines[i].x2 = lines[i].x2 + data->area.wanted.x;
               local_lines[i].y1 = lines[i].y1 + data->area.wanted.y;
               local_lines[i].y2 = lines[i].y2 + data->area.wanted.y;
          }
     }
     else
          /* clipping may modify lines, so we copy them */
          memcpy( local_lines, lines, sizeof(DFBRegion) * num_lines );

     dfb_gfxcard_drawlines( local_lines, num_lines, &data->state );

     DFBFREE( local_lines );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_DrawRectangle( IDirectFBSurface *thiz,
                                int x, int y, int w, int h )
{
     DFBRectangle rect;

     INTERFACE_GET_DATA(IDirectFBSurface)
     
     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;


     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_drawrectangle( &rect, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_FillOval( IDirectFBSurface *thiz,
                                int x, int y, int w, int h )
{
     DFBRectangle rect;

     INTERFACE_GET_DATA(IDirectFBSurface)
     
     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;


     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_fillOval( &rect, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_DrawOval( IDirectFBSurface *thiz,
                                int x, int y, int w, int h )
{
     DFBRectangle rect;
     
     INTERFACE_GET_DATA(IDirectFBSurface)
     
     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;


     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_drawOval( &rect, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_FillArc( IDirectFBSurface *thiz,
                                int x, int y, int w, int h, int start, int arcAngle )
{
     DFBRectangle rect;
     INTERFACE_GET_DATA(IDirectFBSurface)
      
     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;


     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_fillArc( &rect, start, arcAngle, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_DrawArc( IDirectFBSurface *thiz,
                                int x, int y, int w, int h, int start, int arcAngle )
{
     DFBRectangle rect;
     INTERFACE_GET_DATA(IDirectFBSurface)
      
     rect.x = x;
     rect.y = y;
     rect.w = w;
     rect.h = h;


     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (w<=0 || h<=0)
          return DFB_INVARG;

     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;

     dfb_gfxcard_drawArc( &rect, start, arcAngle, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_FillPolygon( IDirectFBSurface *thiz,
                                int *xPoints, int *yPoints, int nPoints )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;

     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

#ifdef NOTYET // Is this used to translate coordinates?
     rect.x += data->area.wanted.x;
     rect.y += data->area.wanted.y;
#endif

     dfb_gfxcard_fillPolygon( xPoints, yPoints, nPoints, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_SetBlittingFlags( IDirectFBSurface        *thiz,
                                   DFBSurfaceBlittingFlags  flags )
{
     INTERFACE_GET_DATA(IDirectFBSurface)

     if (data->state.blittingflags != flags) {
          data->state.blittingflags = flags;
          data->state.modified |= SMF_BLITTING_FLAGS;
     }

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_Blit( IDirectFBSurface   *thiz,
                       IDirectFBSurface   *source,
                       const DFBRectangle *sr,
                       int dx, int dy )
{
     DFBRectangle srect;
     IDirectFBSurface_data *src_data;

     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (!source)
          return DFB_INVARG;


     src_data = (IDirectFBSurface_data*)source->priv;

     if (!src_data->area.current.w || !src_data->area.current.h)
          return DFB_INVAREA;


     if (sr) {
          if (sr->w < 1  ||  sr->h < 1)
               return DFB_OK;

          srect = *sr;

          srect.x += src_data->area.wanted.x;
          srect.y += src_data->area.wanted.y;

          if (!dfb_rectangle_intersect( &srect, &src_data->area.current ))
               return DFB_INVAREA;

          dx += srect.x - (sr->x + src_data->area.wanted.x);
          dy += srect.y - (sr->y + src_data->area.wanted.y);
     }
     else {
          srect = src_data->area.current;

          dx += srect.x - src_data->area.wanted.x;
          dy += srect.y - src_data->area.wanted.y;
     }

     dfb_state_set_source( &data->state, src_data->surface );

     /* fetch the source color key from the source if necessary */
     if (data->state.blittingflags & DSBLIT_SRC_COLORKEY) {
          if (data->state.src_colorkey != src_data->src_key.value) {
               data->state.src_colorkey = src_data->src_key.value;
               data->state.modified |= SMF_SRC_COLORKEY;
          }
     }

     dfb_gfxcard_blit( &srect,
                       data->area.wanted.x + dx,
                       data->area.wanted.y + dy, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_TileBlit( IDirectFBSurface   *thiz,
                           IDirectFBSurface   *source,
                           const DFBRectangle *sr,
                           int dx, int dy )
{
     DFBRectangle srect;
     IDirectFBSurface_data *src_data;

     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (!source)
          return DFB_INVARG;


     src_data = (IDirectFBSurface_data*)source->priv;

     if (!src_data->area.current.w || !src_data->area.current.h)
          return DFB_INVAREA;


     if (sr) {
          if (sr->w < 1  ||  sr->h < 1)
               return DFB_OK;

          srect = *sr;

          srect.x += src_data->area.wanted.x;
          srect.y += src_data->area.wanted.y;

          if (!dfb_rectangle_intersect( &srect, &src_data->area.current ))
               return DFB_INVAREA;

          dx += srect.x - (sr->x + src_data->area.wanted.x);
          dy += srect.y - (sr->y + src_data->area.wanted.y);
     }
     else {
          srect = src_data->area.current;

          dx += srect.x - src_data->area.wanted.x;
          dy += srect.y - src_data->area.wanted.y;
     }

     dfb_state_set_source( &data->state, src_data->surface );

     /* fetch the source color key from the source if necessary */
     if (data->state.blittingflags & DSBLIT_SRC_COLORKEY) {
          if (data->state.src_colorkey != src_data->src_key.value) {
               data->state.src_colorkey = src_data->src_key.value;
               data->state.modified |= SMF_SRC_COLORKEY;
          }
     }

     dx %= srect.w;
     if (dx > 0)
       dx -= srect.w;

     dy %= srect.h;
     if (dy > 0)
       dy -= srect.h;

     dfb_gfxcard_tileblit( &srect,
                           data->area.wanted.x + dx,
                           data->area.wanted.y + dy,
                           data->area.wanted.x + data->area.wanted.w,
                           data->area.wanted.y + data->area.wanted.h, 
                           &data->state );

     return DFB_OK;
}

// This is the main low level stretch blit function. It is generally called in
// the form dsts->StretchBlit(dsts, srcs, srect, drect);
// Is called from mpeos_draw.c gfxStretchBlt() function.
// It is in this function that the width and height are forced positive just
// for the clipping and restored later if negative.
// Note we changed the rectangles to be non-const to handle negative values on
// 12/2/2005 (DONM).
static DFBResult
IDirectFBSurface_StretchBlit( IDirectFBSurface   *thiz,
                              IDirectFBSurface   *source,
                              const DFBRectangle *source_rect,
                              const DFBRectangle *destination_rect )
{
     DFBRectangle srect, drect;
     IDirectFBSurface_data *src_data;
	 bool flagswneg, flagshneg;		// flags indicate if width or height are < 0
	 bool flagdwneg, flagdhneg;		// Same for destination as for source

     INTERFACE_GET_DATA(IDirectFBSurface)

	 // Get the flags and absolute value of widths and heights
	 
	 // First assign x and y values
	 srect.x = source_rect->x;
	 srect.y = source_rect->y;
	 drect.x = destination_rect->x;
	 drect.y = destination_rect->y;

	 // Now deal with widths and heights and special case of negative values.
	 if (source_rect->w < 0)
	 {
	     srect.w = -(source_rect->w);
		 flagswneg = true;
	 }
	 else
	 {
	     flagswneg = false;
		 srect.w = source_rect->w;
	 }

	 if (source_rect->h < 0)
	 {
		 flagshneg = true;
		 srect.h = -(source_rect->h);
	 }
	 else
	 {
	     flagshneg = false;
		 srect.h = source_rect->h;
	 }

	 if (destination_rect->w < 0)
	 {
		 flagdwneg = true;
		 drect.w = -(destination_rect->w);
	 }
	 else
	 {
		 flagdwneg = false;
		 drect.w = destination_rect->w;
	 }

	 if (destination_rect->h < 0)
	 {
		 flagdhneg = true;
		 drect.h = -(destination_rect->h);
	 }
	 else
	 {
		 flagdhneg = false;
		 drect.h = destination_rect->h;
	 }

	 // Now we can get rid of obsolete negative signs (if source and dest both
	 // have the same dimension negative, it's the same as if they are both
	 // positive). So, we can reduce them down to one set in only the dest
	 // rectangle and force the srect to be always positive.
	 flagdwneg = flagdwneg ^ flagswneg;
	 flagdhneg = flagdhneg ^ flagshneg;

     if (!data->surface)
          return DFB_DESTROYED;


     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (!source)
          return DFB_INVARG;


     src_data = (IDirectFBSurface_data*)source->priv;

     if (!src_data->area.current.w || !src_data->area.current.h)
          return DFB_INVAREA;


     /* do destination rectangle */
     if (destination_rect) {
          if (drect.w < 1  ||  drect.h < 1)
              return DFB_INVARG;


          drect.x += data->area.wanted.x;
          drect.y += data->area.wanted.y;
     }
     else
          drect = data->area.wanted;

     /* do source rectangle */
     if (source_rect) {
          if (srect.w < 1  ||  srect.h < 1)
               return DFB_INVARG;

          srect.x += src_data->area.wanted.x;
          srect.y += src_data->area.wanted.y;
     }
     else
          srect = src_data->area.wanted;


     /* clipping of the source rectangle must be applied to the destination */
     {
          DFBRectangle orig_src = srect;

          if (!dfb_rectangle_intersect( &srect, &src_data->area.current ))
               return DFB_INVAREA;

          if (srect.x != orig_src.x)
               drect.x += (int)( (srect.x - orig_src.x) *
                                 (drect.w / (float)orig_src.w) + 0.5f);

          if (srect.y != orig_src.y)
               drect.y += (int)( (srect.y - orig_src.y) *
                                 (drect.h / (float)orig_src.h) + 0.5f);

          if (srect.w != orig_src.w)
               drect.w = DFB_ICEIL(drect.w * (srect.w / (float)orig_src.w));
          if (srect.h != orig_src.h)
               drect.h = DFB_ICEIL(drect.h * (srect.h / (float)orig_src.h));
     }

     dfb_state_set_source( &data->state, src_data->surface );

     /* fetch the source color key from the source if necessary */
     if (data->state.blittingflags & DSBLIT_SRC_COLORKEY) {
          if (data->state.src_colorkey != src_data->src_key.value) {
               data->state.src_colorkey = src_data->src_key.value;
               data->state.modified |= SMF_SRC_COLORKEY;
          }
     }

	 // Now restore negative width or height to destination rect only.
	 if (flagdwneg)
		 drect.w = -(drect.w);
	 if (flagdhneg)
		 drect.h = -(drect.h);
	 // Now inside the lower level functions the negative values are extracted
	 // again from the destination rectangle only.
	 
     dfb_gfxcard_stretchblit( &srect, &drect, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_DrawString( IDirectFBSurface *thiz,
                             const char *text, int bytes,
                             int x, int y,
                             DFBSurfaceTextFlags flags )
{
     IDirectFBFont_data *font_data;

     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;

     if (!text)
          return DFB_INVARG;

     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (!data->font)
          return DFB_MISSINGFONT;


     if (bytes < 0)
          bytes = strlen (text);

     if (bytes == 0)
          return DFB_OK;

     if (!(flags & DSTF_TOP)) {
          int offset = 0;

          data->font->GetAscender (data->font, &offset);
          y -= offset;

          if ((flags & DSTF_BOTTOM)) {
               offset = 0;
               data->font->GetDescender (data->font, &offset);
               y += offset;
          }
     }

     if (flags & (DSTF_RIGHT | DSTF_CENTER)) {
          int width = 0;

          if (data->font->GetStringWidth (data->font,
                                          text, bytes, &width) == DFB_OK) {
               if (flags & DSTF_RIGHT) {
                    x -= width;
               }
               else if (flags & DSTF_CENTER) {
                    x -= width >> 1;
               }
          }
     }

     font_data = (IDirectFBFont_data *)data->font->priv;

     dfb_gfxcard_drawstring( (const __u8 *)text, bytes,
                             data->area.wanted.x + x, data->area.wanted.y + y,
                             font_data->font, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_DrawGlyph( IDirectFBSurface *thiz,
                            unsigned int index, int x, int y,
                            DFBSurfaceTextFlags flags )
{
     IDirectFBFont_data *font_data;

     INTERFACE_GET_DATA(IDirectFBSurface)

     if (!data->surface)
          return DFB_DESTROYED;

     if (!index)
          return DFB_INVARG;

     if (!data->area.current.w || !data->area.current.h)
          return DFB_INVAREA;

     if (data->locked)
          return DFB_LOCKED;

     if (!data->font)
          return DFB_MISSINGFONT;

     if (!(flags & DSTF_TOP)) {
          int offset = 0;

          data->font->GetAscender (data->font, &offset);
          y -= offset;

          if ((flags & DSTF_BOTTOM)) {
               offset = 0;
               data->font->GetDescender (data->font, &offset);
               y += offset;
          }
     }

     if (flags & (DSTF_RIGHT | DSTF_CENTER)) {
          int advance;

          if (data->font->GetGlyphExtents (data->font,
                                           index, NULL, &advance) == DFB_OK) {
               if (flags & DSTF_RIGHT) {
                    x -= advance;
               }
               else if (flags & DSTF_CENTER) {
                    x -= advance >> 1;
               }
          }
     }

     font_data = (IDirectFBFont_data *)data->font->priv;

     dfb_gfxcard_drawglyph( index,
                            data->area.wanted.x + x, data->area.wanted.y + y,
                            font_data->font, &data->state );

     return DFB_OK;
}

static DFBResult
IDirectFBSurface_GetSubSurface( IDirectFBSurface    *thiz,
                                const DFBRectangle  *rect,
                                IDirectFBSurface   **surface )
{
     DFBRectangle wanted, granted;

     INTERFACE_GET_DATA(IDirectFBSurface)

     /* Check arguments */
     if (!data->surface)
          return DFB_DESTROYED;

     if (!surface)
          return DFB_INVARG;

     /* Compute wanted rectangle */
     if (rect) {
          wanted = *rect;

          wanted.x += data->area.wanted.x;
          wanted.y += data->area.wanted.y;

          if (wanted.w <= 0 || wanted.h <= 0) {
               wanted.w = 0;
               wanted.h = 0;
          }
     }
     else
          wanted = data->area.wanted;

     /* Compute granted rectangle */
     granted = wanted;

     dfb_rectangle_intersect( &granted, &data->area.granted );

     /* Allocate and construct */
     DFB_ALLOCATE_INTERFACE( *surface, IDirectFBSurface );
     if (*surface == NULL)
         return DFB_NOSYSTEMMEMORY;

     return IDirectFBSurface_Construct( *surface, &wanted, &granted,
                                        data->surface,
                                        data->caps | DSCAPS_SUBSURFACE );
}

DFBResult IDirectFBSurface_Construct( IDirectFBSurface       *thiz,
                                      DFBRectangle           *wanted,
                                      DFBRectangle           *granted,
                                      CoreSurface            *surface,
                                      DFBSurfaceCapabilities  caps )
{
     DFBRectangle rect;
     DFB_ALLOCATE_INTERFACE_DATA(thiz, IDirectFBSurface)
      
     rect.x = 0;
     rect.y = 0;
     rect.w = surface->width;
     rect.h = surface->height;

     if (data == NULL)
         return DFB_NOSYSTEMMEMORY;

     data->ref = 1;
     data->caps = caps;

     if (dfb_surface_ref( surface ) != FUSION_SUCCESS) {
          DFB_DEALLOCATE_INTERFACE(thiz);
          return DFB_FAILURE;
     }

     /* The area that was requested */
     if (wanted)
          data->area.wanted = *wanted;
     else
          data->area.wanted = rect;

     /* The area that will never be exceeded */
     if (granted)
          data->area.granted = *granted;
     else
          data->area.granted = data->area.wanted;

     /* The currently accessible rectangle */
     data->area.current = data->area.granted;
     dfb_rectangle_intersect( &data->area.current, &rect );

     data->surface = surface;

     dfb_state_init( &data->state );
     dfb_state_set_destination( &data->state, surface );

     data->state.clip.x1 = data->area.current.x;
     data->state.clip.y1 = data->area.current.y;
     data->state.clip.x2 = data->area.current.x + data->area.current.w - 1;
     data->state.clip.y2 = data->area.current.y + data->area.current.h - 1;
     data->state.dst_blend = DSBF_INVSRCALPHA;
     data->state.src_blend = DSBF_SRCALPHA;
     data->state.modified = SMF_ALL;

     thiz->AddRef = IDirectFBSurface_AddRef;
     thiz->Release = IDirectFBSurface_Release;

     thiz->GetCapabilities = IDirectFBSurface_GetCapabilities;
     thiz->GetSize = IDirectFBSurface_GetSize;
     thiz->GetVisibleRectangle = IDirectFBSurface_GetVisibleRectangle;
     thiz->GetPixelFormat = IDirectFBSurface_GetPixelFormat;
     thiz->GetAccelerationMask = IDirectFBSurface_GetAccelerationMask;

     thiz->GetPalette = IDirectFBSurface_GetPalette;
     thiz->SetPalette = IDirectFBSurface_SetPalette;
     
     thiz->Lock = IDirectFBSurface_Lock;
     thiz->Unlock = IDirectFBSurface_Unlock;
     thiz->Flip = IDirectFBSurface_Flip;
     thiz->SetField = IDirectFBSurface_SetField;
     thiz->Clear = IDirectFBSurface_Clear;

     thiz->SetClip = IDirectFBSurface_SetClip;
     thiz->SetAlphaConstant = IDirectFBSurface_SetAlphaConstant;
     thiz->SetColor = IDirectFBSurface_SetColor;
     thiz->SetColorIndex = IDirectFBSurface_SetColorIndex;
     thiz->SetSrcBlendFunction = IDirectFBSurface_SetSrcBlendFunction;
     thiz->SetDstBlendFunction = IDirectFBSurface_SetDstBlendFunction;
     thiz->SetPorterDuff = IDirectFBSurface_SetPorterDuff;
     thiz->SetSrcColorKey = IDirectFBSurface_SetSrcColorKey;
     thiz->SetSrcColorKeyIndex = IDirectFBSurface_SetSrcColorKeyIndex;
     thiz->SetDstColorKey = IDirectFBSurface_SetDstColorKey;
     thiz->SetDstColorKeyIndex = IDirectFBSurface_SetDstColorKeyIndex;

     thiz->SetBlittingFlags = IDirectFBSurface_SetBlittingFlags;
     thiz->Blit = IDirectFBSurface_Blit;
     thiz->TileBlit = IDirectFBSurface_TileBlit;
     thiz->StretchBlit = IDirectFBSurface_StretchBlit;

     thiz->SetDrawingFlags = IDirectFBSurface_SetDrawingFlags;
     thiz->FillRectangle = IDirectFBSurface_FillRectangle;
     thiz->FillRoundRect = IDirectFBSurface_FillRoundRect;
     thiz->DrawRoundRect = IDirectFBSurface_DrawRoundRect;
     thiz->DrawLine = IDirectFBSurface_DrawLine;
     thiz->DrawLines = IDirectFBSurface_DrawLines;
     thiz->DrawRectangle = IDirectFBSurface_DrawRectangle;
     thiz->FillOval = IDirectFBSurface_FillOval;
     thiz->DrawOval = IDirectFBSurface_DrawOval;
     thiz->FillArc = IDirectFBSurface_FillArc;
     thiz->DrawArc = IDirectFBSurface_DrawArc;
     thiz->FillPolygon = IDirectFBSurface_FillPolygon;

     thiz->SetFont = IDirectFBSurface_SetFont;
     thiz->GetFont = IDirectFBSurface_GetFont;
     thiz->DrawString = IDirectFBSurface_DrawString;
     thiz->DrawGlyph = IDirectFBSurface_DrawGlyph;

     thiz->GetSubSurface = IDirectFBSurface_GetSubSurface;

     return dfb_surface_attach( surface,
                                IDirectFBSurface_listener, thiz, &data->reaction );
}
