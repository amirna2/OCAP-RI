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

#ifndef __FUSION__OBJECT_H__
#define __FUSION__OBJECT_H__

#ifdef __cplusplus
extern "C"
{
#endif

#include "fusion_types.h"
#include <core/fusion/lock.h>
#include <core/fusion/list.h>
#include <core/fusion/ref.h>

typedef void (*FusionObjectDestructor)( FusionObject *object, bool zombie );

typedef enum {
     FOS_INIT,
     FOS_ACTIVE,
     FOS_DEINIT
} FusionObjectState;

struct _FusionObject {
     FusionLink         link;
     FusionObjectPool  *pool;

     FusionObjectState  state;

     FusionRef          ref;
};


FusionObjectPool *fusion_object_pool_create ( const char            *name,
                                              int                    object_size,
                                              int                    message_size,
                                              FusionObjectDestructor destructor );

FusionResult      fusion_object_pool_destroy( FusionObjectPool      *pool );


FusionObject     *fusion_object_create        ( FusionObjectPool *pool );

FusionResult      fusion_object_activate      ( FusionObject     *object );

#define fusion_object_attach( object, react, ctx, reaction ) FUSION_SUCCESS
#define fusion_object_detach( object, react ) 
#define fusion_object_attach_global( object, react_index, ctx, reaction ) FUSION_SUCCESS
#define fusion_object_detach_global( object, react ) 
#define fusion_object_dispatch( object, message, globals ) FUSION_SUCCESS

FusionResult      fusion_object_ref     ( FusionObject     *object );
LIBEXPORT FusionResult      fusion_object_unref   ( FusionObject     *object );

FusionResult      fusion_object_link    ( FusionObject    **link,
                                          FusionObject     *object );
FusionResult      fusion_object_unlink  ( FusionObject     *object );

FusionResult      fusion_object_destroy ( FusionObject     *object );

/** Added to replace work performed by bone_collector_loop. */
void              fusion_object_collect ( FusionObject	*object );


#ifdef __cplusplus
}
#endif

#endif /* __FUSION__OBJECT_H__ */

