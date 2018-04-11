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
#include <core/fusion/object.h>
#include <core/fusion/shmalloc.h>
#include "fusion_internal.h"

struct _FusionObjectPool {
     FusionSkirmish          lock;
     FusionLink             *objects;

     char                   *name;
     int                     object_size;
     FusionObjectDestructor  destructor;

     bool                    shutdown;
};

FusionObjectPool *
fusion_object_pool_create( const char             *name,
                           int                     object_size,
                           int                     message_size,
                           FusionObjectDestructor  destructor )
{
     FusionObjectPool *pool;

     DFB_ASSERT( name != NULL );
     DFB_ASSERT( object_size >= sizeof(FusionObject) );
     DFB_ASSERT( message_size > 0 );
     DFB_ASSERT( destructor != NULL );

     /* Allocate shared memory for the pool. */
     pool = shcalloc( 1, sizeof(FusionObjectPool) );
     if (!pool)
          return NULL;

     /* Initialize the pool lock. */
     skirmish_init( &pool->lock );

     /* Fill information. */
     pool->name         = (char *)shstrdup( name );
     if (!pool)
          return NULL;
     pool->object_size  = object_size;
     pool->destructor   = destructor;

     return pool;
}

FusionResult
fusion_object_pool_destroy( FusionObjectPool *pool )
{
     DFB_ASSERT( pool != NULL );

     /* Destroy the pool lock. */
     skirmish_destroy( &pool->lock );

     /* Deallocate shared memory. */
     shfree( pool->name );
     shfree( pool );

     return FUSION_SUCCESS;
}

FusionObject *
fusion_object_create( FusionObjectPool *pool )
{
     FusionObject *object;

     DFB_ASSERT( pool != NULL );

     /* Allocate shared memory for the object. */
     object = shcalloc( 1, pool->object_size );
     if (!object)
          return NULL;

     /* Set "initializing" state. */
     object->state = FOS_INIT;

     /* Initialize the reference counter. */
     if (fusion_ref_init( &object->ref )) {
          shfree( object );
          return NULL;
     }

     /* Increase the object's reference counter. */
     fusion_ref_up( &object->ref, false );

     /* Lock the pool's object list. */
     skirmish_prevail( &pool->lock );

     HEAVYDEBUGMSG(("{%s} adding %p\n", pool->name, object));
     
     /* Set pool back pointer. */
     object->pool = pool;

     /* Add the object to the pool. */
     fusion_list_prepend( &pool->objects, &object->link );

     /* Unlock the pool's object list. */
     skirmish_dismiss( &pool->lock );
     
     return object;
}

FusionResult
fusion_object_activate( FusionObject *object )
{
     /* Set "active" state. */
     object->state = FOS_ACTIVE;
     
     return FUSION_SUCCESS;
}

FusionResult
fusion_object_ref( FusionObject     *object )
{
     DFB_UNUSED_PARAM(object);
     return fusion_ref_up( &object->ref, false );
}

FusionResult
fusion_object_unref( FusionObject     *object )
{
     DFB_UNUSED_PARAM(object);
     return fusion_ref_down( &object->ref, false );
}

FusionResult
fusion_object_link( FusionObject    **link,
                    FusionObject     *object )
{
     FusionResult ret;

     ret = fusion_ref_up( &object->ref, true );
     if (ret)
          return ret;

     *link = object;

     return FUSION_SUCCESS;
}

FusionResult
fusion_object_unlink( FusionObject     *object )
{
     DFB_UNUSED_PARAM(object);
     return fusion_ref_down( &object->ref, true );
}

FusionResult
fusion_object_destroy( FusionObject     *object )
{
     DFB_ASSERT( object != NULL );

     /* Set "deinitializing" state. */
     object->state = FOS_DEINIT;
     
     /* Remove the object from the pool. */
     if (object->pool) {
          FusionObjectPool *pool = object->pool;

          /* Lock the pool's object list. */
          skirmish_prevail( &pool->lock );

          /* Remove the object from the pool. */
          if (object->pool) {
               object->pool = NULL;
               fusion_list_remove( &pool->objects, &object->link );
          }
          
          /* Unlock the pool's object list. */
          skirmish_dismiss( &pool->lock );
     }
     
     fusion_ref_destroy( &object->ref );

     shfree( object );

     return FUSION_SUCCESS;
}

/**
 * Performs the operations that previously (by the original
 * DirectFB implementation before we hacked at it) would've
 * been performed by the bone_collector_loop().
 * <p>
 * Question: can we remove the fusion_list_prepend/remove for
 * the pool and it's objects?  With the bone_collector removed,
 * do we not need it?  (I don't know.)
 * <p>
 * Since fusion_object_destroy() does much of this, and that's called
 * by the destructor... can we avoid all but calling the destructor?
 */
void
fusion_object_collect( FusionObject     *object )
{
    FusionObjectPool *pool;

    DFB_ASSERT( object != NULL );

    pool = object->pool;

    /* Lock the pool's object list. */
    skirmish_prevail( &pool->lock );

    switch(fusion_ref_zero_trylock( &object->ref ))
    {
        case FUSION_SUCCESS:
            /*FDEBUG("{%s} dead object: %p\n", pool->name, object);*/

            /* Set "deinitializing" state. */
            object->state = FOS_DEINIT;

            /* Remove the object from the pool. */
            object->pool = NULL;
            fusion_list_remove( &pool->objects, &object->link );

            /* Call the destructor. */
            pool->destructor( object, false );
            
            break;

        case FUSION_DESTROYED:
            /*FDEBUG("already destroyed! removing %p from '%s'\n",
              object, pool->name);*/
            
            /* Remove the object from the pool. */
            fusion_list_remove( &pool->objects, &object->link );
            
        default:
            break;
    }

    /* Unlock the pool's object list. */
    skirmish_dismiss( &pool->lock );
}


