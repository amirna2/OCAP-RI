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

#ifndef __VDFB__PRIMARY_H__
#define __VDFB__PRIMARY_H__

#include <core/layers.h>

extern DisplayLayerFuncs vidfbPrimaryLayerFuncs;
extern DFBConfig *port_dfbConfig;

/**
 * Externs
 */
extern DFBSurfacePixelFormat (*dfb_pixelformat_for_depth_ptr)(int depth);
extern CoreSurface *(*dfb_layer_surface_ptr)(const DisplayLayer *layer);
extern void (*dfb_surface_flip_buffers_ptr)(CoreSurface *surface);
extern DFBResult
        (*dfb_surface_create_preallocated_ptr)(int width, int height,
                DFBSurfacePixelFormat format, CoreSurfacePolicy policy,
                DFBSurfaceCapabilities caps, CorePalette *palette,
                void *front_data, void *back_data, int front_pitch,
                int back_pitch, CoreSurface **surface);
extern DFBResult (*dfb_surface_create_preallocated_video_ptr)(int width,
        int height, DFBSurfacePixelFormat format, CoreSurfacePolicy policy,
        DFBSurfaceCapabilities caps, CorePalette *palette, void *front_data,
        void *back_data, int front_pitch, int back_pitch,
        unsigned long native_surface_context, CoreSurface **surface);
extern DFBResult (*dfb_palette_create_ptr)(unsigned int size,
        CorePalette **ret_palette);
extern void (*dfb_palette_generate_rgb332_map_ptr)(CorePalette *palette);
extern DFBResult (*dfb_surface_set_palette_ptr)(CoreSurface *surface,
        CorePalette *palette);
extern FusionResult (*fusion_object_unref_ptr)(FusionObject *object);

#endif /* __VDFB__PRIMARY_H__ */

