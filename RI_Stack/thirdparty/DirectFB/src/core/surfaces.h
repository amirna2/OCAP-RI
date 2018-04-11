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

#ifndef __SURFACES_H__
#define __SURFACES_H__

#include <directfb.h>
#include <core/fusion/object.h>
#include <core/fusion/list.h>
#include <core/fusion/lock.h>
#include <core/coretypes.h>

/* defined in the makefile
   #define DFB_USE_NATIVE_SURFACES 1
*/

#if !DFB_USE_NATIVE_SURFACES
	struct _Chunk;

#endif

typedef enum {
     CSNF_SIZEFORMAT     = 0x00000001,  /* width, height, format */
     CSNF_SYSTEM         = 0x00000002,  /* system instance information */
     CSNF_VIDEO          = 0x00000004,  /* video instance information */
     CSNF_DESTROY        = 0x00000008,  /* surface is about to be destroyed */
     CSNF_FLIP           = 0x00000010,  /* surface buffer pointer swapped */
     CSNF_FIELD          = 0x00000020,  /* active (displayed) field switched */
     CSNF_PALETTE_CHANGE = 0x00000040,  /* another palette has been set */
     CSNF_PALETTE_UPDATE = 0x00000080   /* current palette has been altered */
} CoreSurfaceNotificationFlags;

typedef struct {
     CoreSurfaceNotificationFlags  flags;
     CoreSurface                  *surface;
} CoreSurfaceNotification;

typedef enum {
     CSH_INVALID         = 0x00000000,  /* surface isn't stored */
     CSH_STORED          = 0x00000001,  /* surface is stored,
                                           well and kicking */
     CSH_RESTORE         = 0x00000002   /* surface needs to be
                                           reloaded into area */
} CoreSurfaceHealth;

typedef enum {
     CSP_SYSTEMONLY      = 0x00000000,  /* never try to swap
                                           into video memory */
     CSP_VIDEOONLY       = 0x00000001,  /* always and only
                                           store in video memory */
     CSP_VIDEOLOW        = 0x00000002,  /* try to store in video memory,
                                           low priority */
     CSP_VIDEOHIGH       = 0x00000003   /* try to store in video memory,
                                           high priority */
} CoreSurfacePolicy;

typedef enum {
     SBF_NONE            = 0x00000000,
     SBF_FOREIGN_SYSTEM  = 0x00000001   /* system memory is preallocated by
                                           application, won't be freed */
} SurfaceBufferFlags;

typedef enum {
     VAF_NONE            = 0x00000000,  /* no write access happened since last
                                           clearing of all bits */
     VAF_SOFTWARE_WRITE  = 0x00000001,  /* software wrote to buffer */
     VAF_HARDWARE_WRITE  = 0x00000002,  /* hardware wrote to buffer */
     VAF_SOFTWARE_READ   = 0x00000004,  /* software read from buffer */
     VAF_HARDWARE_READ   = 0x00000008   /* hardware read from buffer */
} VideoAccessFlags;


struct _SurfaceBuffer
{
     SurfaceBufferFlags      flags;     /* additional information */
     CoreSurfacePolicy       policy;    /* swapping policy for surfacemanager */

     struct {
          CoreSurfaceHealth  health;    /* currently stored in system memory? */
          int                locked;    /* system instance is locked,
                                           stick to it */

          int                pitch;     /* number of bytes til next line */
          void              *addr;      /* address pointing to surface data */
     } system;

     struct {
          CoreSurfaceHealth  health;    /* currently stored in video memory? */
          int                locked;    /* video instance is locked, don't
                                           try to kick out, could deadlock */

          VideoAccessFlags   access;    /* information about recent read/write
                                           accesses to video buffer memory */

          int                pitch;     /* number of bytes til next line */
          int                offset;    /* byte offset from the beginning
                                           of the framebuffer */

		  bool				native_surface_context_valid;
		  unsigned long		native_surface_context;

#if !DFB_USE_NATIVE_SURFACES
          struct _Chunk     *chunk;     /* points to the allocated chunk */
#endif
     } video;

     CoreSurface            *surface;   /* always pointing to the surface this
                                           buffer belongs to, surfacemanger
                                           always depends on this! */
};

struct _CoreSurface
{
     FusionObject           object;

     FusionSkirmish         lock;
     bool                   destroyed;

     /* size/format and instances */
     int                    width;         /* pixel width of the surface */
     int                    height;        /* pixel height of the surface */
     DFBSurfacePixelFormat  format;        /* pixel format of the surface */
     DFBSurfaceCapabilities caps;

     int                    min_width;     /* minimum allocation width */
     int                    min_height;    /* minimum allocation height */

     CorePalette           *palette;

     int                    field;

     SurfaceBuffer         *front_buffer;  /* buffer for reading
                                              (blit from or display buffer) */
     FusionSkirmish         front_lock;    /* skirmish lock for front buffer */

     SurfaceBuffer         *back_buffer;   /* buffer for (reading&)writing
                                              (drawing/blitting destination) */
     FusionSkirmish         back_lock;     /* skirmish lock for back buffer,
                                              mutexes are outside of
                                              SurfaceBuffer because of flipping
                                              that just swaps the pointers */

     SurfaceManager        *manager;
};

/*
 * creates a surface pool
 */
FusionObjectPool *dfb_surface_pool_create(void);

#define dfb_surface_pool_destroy(pool) fusion_object_pool_destroy(pool)

/*
 * creates a surface with specified width and height in the specified
 * pixelformat using the specified swapping policy
 */
DFBResult dfb_surface_create( int                      width,
                              int                      height,
                              DFBSurfacePixelFormat    format,
                              CoreSurfacePolicy        policy,
                              DFBSurfaceCapabilities   caps,
                              CorePalette             *palette,
                              CoreSurface            **surface );

/*
 * like surface_create, but with preallocated system memory that won't be
 * freed on surface destruction
 */
LIBEXPORT DFBResult dfb_surface_create_preallocated( int                      width,
                                           int                      height,
                                           DFBSurfacePixelFormat    format,
                                           CoreSurfacePolicy        policy,
                                           DFBSurfaceCapabilities   caps,
                                           CorePalette             *palette,
                                           void                    *front_data,
                                           void                    *back_data,
                                           int                      front_pitch,
                                           int                      back_pitch,
                                           CoreSurface            **surface );

/*
 * like surface_create, but with preallocated video memory that won't be
 * freed on surface destruction
 */
LIBEXPORT DFBResult dfb_surface_create_preallocated_video( int                      width,
												 int                      height,
												 DFBSurfacePixelFormat    format,
												 CoreSurfacePolicy        policy,
												 DFBSurfaceCapabilities   caps,
												 CorePalette             *palette,
												 void                    *front_data,
												 void                    *back_data,
												 int                      front_pitch,
												 int                      back_pitch,
												 unsigned long            native_surface_context,
												 CoreSurface            **surface );

/*
 * initialize surface structure, not required for surface_create_*
 */
DFBResult dfb_surface_init ( CoreSurface            *surface,
                             int                     width,
                             int                     height,
                             DFBSurfacePixelFormat   format,
                             DFBSurfaceCapabilities  caps,
                             CorePalette            *palette );

/*
 * reallocates data for the specified surface
 */
DFBResult dfb_surface_reformat( CoreSurface           *surface,
                                int                    width,
                                int                    height,
                                DFBSurfacePixelFormat  format );

/*
 * Change policies of buffers.
 */
DFBResult dfb_surface_reconfig( CoreSurface       *surface,
                                CoreSurfacePolicy  front_policy,
                                CoreSurfacePolicy  back_policy );

/*
 * Change the palette of the surface.
 */
LIBEXPORT DFBResult dfb_surface_set_palette( CoreSurface *surface,
                                   CorePalette *palette );

/*
 * helper function
 */
#define dfb_surface_attach( surface, react, ctx, reaction ) \
     fusion_object_attach( &surface->object, react, ctx, reaction )
#define dfb_surface_detach( surface, reaction) \
     fusion_object_detach( &surface->object, reaction )
#define dfb_surface_attach_global( surface, react_index, ctx, reaction) \
     fusion_object_attach_global( &surface->object, react_index, ctx, reaction )
#define dfb_surface_detach_global( surface, reaction) \
     fusion_object_detach_global( &surface->object, reaction )
#define dfb_surface_ref( surface )\
     fusion_object_ref( &surface->object )
#define dfb_surface_unref( surface) \
     fusion_object_unref( &surface->object )
#define dfb_surface_link( link, surface) \
     fusion_object_link( (FusionObject**) link, &surface->object )
#define dfb_surface_unlink( surface ) \
     fusion_object_unlink( &surface->object )

/*
 * really swaps front_buffer and back_buffer if they have the same policy,
 * otherwise it does a back_to_front_copy, notifies listeners
 */
LIBEXPORT void dfb_surface_flip_buffers( CoreSurface *surface );

void dfb_surface_set_field( CoreSurface *surface, int field );

/*
 * This is a utility function for easier usage.
 * It locks the surface maneger, does a surface_software_lock, and unlocks
 * the surface manager.
 */
DFBResult dfb_surface_soft_lock( CoreSurface          *surface,
                                 DFBSurfaceLockFlags   flags,
                                 void                **data,
                                 int                  *pitch,
                                 int                   front );

/*
 * unlocks a previously locked surface
 * note that the other instance's health is CSH_RESTORE now, if it has
 * been CSH_STORED before
 */
void dfb_surface_unlock( CoreSurface *surface, int front );

/*
 * destroy the surface and free its instances
 */
void dfb_surface_destroy( CoreSurface *surface, bool unref );


/* global reactions */

typedef enum {
     DFB_LAYER_SURFACE_LISTENER,
     DFB_LAYER_BACKGROUND_IMAGE_LISTENER
} DFB_SURFACE_GLOBALS;

#endif

