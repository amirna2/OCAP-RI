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
#include <core/core.h>
#include <core/coredefs.h>
#include <core/coretypes.h>
#include <core/fonts.h>
#include <core/gfxcard.h>
#include <core/surfaces.h>
#include <misc/mem.h>
#include <misc/tree.h>
#include <misc/util.h>

CoreFont *
dfb_font_create(void)
{
     CoreFont *font;

     if ((font = (CoreFont *) DFBCALLOC( 1, sizeof(CoreFont) )) == NULL)
         return NULL;

     pthread_mutex_init( &font->lock, PTHREAD_MUTEX_FAST );

     /* the proposed pixel_format, may be changed by the font provider */
     font->pixel_format = dfb_config->argb_font ? DSPF_ARGB : DSPF_A8;

     /* the state used to blit the glyphs, may be changed by the font
        provider */
     dfb_state_init( &font->state );
     font->state.blittingflags = DSBLIT_BLEND_ALPHACHANNEL | DSBLIT_COLORIZE;

     font->glyph_infos = dfb_tree_new ();

     return font;
}

void
dfb_font_destroy( CoreFont *font )
{
     int i;

     pthread_mutex_lock( &font->lock );

     dfb_state_set_source( &font->state, NULL );
     dfb_state_set_destination( &font->state, NULL );
     dfb_state_destroy( &font->state );
     
     dfb_tree_destroy( font->glyph_infos );

     if (font->surfaces) {
          for (i = 0; i < font->rows; i++)
          {
               dfb_surface_unref( font->surfaces[i] );

               /* collect fusion object and surface data */
               fusion_object_collect( &font->surfaces[i]->object );
          }

          DFBFREE( font->surfaces );
     }

     pthread_mutex_unlock( &font->lock );
     pthread_mutex_destroy( &font->lock );

     DFBFREE( font );
}

DFBResult
dfb_font_get_glyph_data( CoreFont        *font,
                         unichar          glyph,
                         CoreGlyphData  **glyph_data )
{
     DFBResult ret;
     CoreGlyphData *data;

     if ((data = dfb_tree_lookup (font->glyph_infos, (void *)glyph)) != NULL) {
          *glyph_data = data;
          return DFB_OK;
     }

     if (!data) {
          data = (CoreGlyphData *) DFBCALLOC(1, sizeof (CoreGlyphData));
          if (!data) {
               return DFB_NOSYSTEMMEMORY;
          }

          if (font->GetGlyphInfo &&
              (* font->GetGlyphInfo) (font, glyph, data) == DFB_OK &&
              data->width > 0 && data->height > 0)
          {

               if (font->next_x + data->width > font->row_width) {
                    if (font->row_width == 0) {
                         int width = 8192 / font->height;

                         if (width < font->maxadvance)
                              width = font->maxadvance;
                         else if (width > 2048)
                              width = 2048;

                         font->row_width = width;
                    }

                    font->next_x = 0;
                    font->rows++;

                    if ((font->surfaces =
                        DFBREALLOC( font->surfaces, sizeof(void *) * font->rows )) == NULL)
                        return DFB_NOSYSTEMMEMORY;

                    /* FIXME: error checking! */
                    dfb_surface_create( font->row_width,
                                        DFB_MAX( font->ascender - font->descender,
                                             8 ),
                                        font->pixel_format,
                                        CSP_VIDEOHIGH, DSCAPS_NONE, NULL,
                                        &font->surfaces[font->rows - 1] );
               }

               if ((* font->RenderGlyph)
                   (font, glyph, data, font->surfaces[font->rows - 1]) == DFB_OK)
               {
                    data->surface = font->surfaces[font->rows - 1];
                    data->start   = font->next_x;
                    font->next_x += data->width;

                    dfb_gfxcard_flush_texture_cache();
               }
               else {
                    data->start = data->width = data->height = 0;
               }
          }
          else {
               data->start = data->width = data->height = 0;
          }

          if ((ret = dfb_tree_insert (font->glyph_infos, (void *) glyph, data)) != DFB_OK)
              return ret;
     }

     *glyph_data = data;

     return DFB_OK;
}

/*
 * lock the font before accessing it
 */
void dfb_font_lock( CoreFont *font )
{
     pthread_mutex_lock( &font->lock );
     dfb_state_lock( &font->state );
}

/*
 * unlock the font after access
 */
void dfb_font_unlock( CoreFont *font )
{
     dfb_state_unlock( &font->state );
     pthread_mutex_unlock( &font->lock );
}
