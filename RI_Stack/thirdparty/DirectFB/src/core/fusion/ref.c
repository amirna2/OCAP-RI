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

#include <config.h>
#include <stdlib.h>
#include <stdio.h>
/*#include <unistd.h>*/
#include <errno.h>

#include "fusion_types.h"
#include "ref.h"
#include <external.h>

#include "fusion_internal.h"

/*#include <signal.h>*/
/***************************
 *  Internal declarations  *
 ***************************/


/*******************
 *  Internal data  *
 *******************/



/****************
 *  Public API  *
 ****************/

FusionResult
fusion_ref_init (FusionRef *ref)
{
     DFB_ASSERT( ref != NULL );
     
     pthread_mutex_init (&ref->lock, NULL);
#if 0
     pthread_cond_init (&ref->cond, NULL);
#endif

     ref->refs      = 0;
     ref->destroyed = false;
     ref->waiting   = 0;

     return FUSION_SUCCESS;
}

FusionResult
fusion_ref_up (FusionRef *ref, bool global)
{
     FusionResult ret = FUSION_SUCCESS;

     DFB_ASSERT( ref != NULL );
     
     pthread_mutex_lock (&ref->lock);

     if (ref->destroyed)
          ret = FUSION_DESTROYED;
     else
          ref->refs++;
     
     pthread_mutex_unlock (&ref->lock);
     
     return ret;
}

FusionResult
fusion_ref_down (FusionRef *ref, bool global)
{
     FusionResult ret = FUSION_SUCCESS;

     DFB_ASSERT( ref != NULL );
     
     pthread_mutex_lock (&ref->lock);

     if (ref->destroyed)
          ret = FUSION_DESTROYED;
     else
          ref->refs--;
     
#if 0
     if (ref->waiting)
          pthread_cond_broadcast (&ref->cond);
#endif
     
     pthread_mutex_unlock (&ref->lock);
     
     return ret;
}

FusionResult
fusion_ref_stat (FusionRef *ref, int *refs)
{
     DFB_ASSERT( ref != NULL );
     DFB_ASSERT( refs != NULL );

     if (ref->destroyed)
          return FUSION_DESTROYED;

     *refs = ref->refs;

     return FUSION_SUCCESS;
}

#if 0
FusionResult
fusion_ref_zero_lock (FusionRef *ref)
{
     FusionResult ret = FUSION_SUCCESS;

     DFB_ASSERT( ref != NULL );
     
     pthread_mutex_lock (&ref->lock);

     if (ref->destroyed)
          ret = FUSION_DESTROYED;
     else while (ref->refs && !ret) {
          ref->waiting++;
          pthread_cond_wait (&ref->cond, &ref->lock);
          ref->waiting--;
          
          if (ref->destroyed)
               ret = FUSION_DESTROYED;
     }
     
     if (ret != FUSION_SUCCESS)
          pthread_mutex_unlock (&ref->lock);
     
     return ret;
}
#endif

FusionResult
fusion_ref_zero_trylock (FusionRef *ref)
{
     FusionResult ret = FUSION_SUCCESS;

     DFB_ASSERT( ref != NULL );
     
     pthread_mutex_lock (&ref->lock);

     if (ref->destroyed)
          ret = FUSION_DESTROYED;
     else if (ref->refs)
          ret = FUSION_INUSE;
     
     if (ret != FUSION_SUCCESS)
          pthread_mutex_unlock (&ref->lock);
     
     return ret;
}

FusionResult
fusion_ref_unlock (FusionRef *ref)
{
     DFB_ASSERT( ref != NULL );
     
     pthread_mutex_unlock (&ref->lock);

     return FUSION_SUCCESS;
}

FusionResult
fusion_ref_destroy (FusionRef *ref)
{
     DFB_ASSERT( ref != NULL );
     
     ref->destroyed = true;

#if 0
     if (ref->waiting)
          pthread_cond_broadcast (&ref->cond);
#endif

     pthread_mutex_unlock (&ref->lock);
#if 0
     pthread_cond_destroy (&ref->cond);
#endif
     
     return FUSION_SUCCESS;
}

/*******************************
 *  Fusion internal functions  *
 *******************************/



/*****************************
 *  File internal functions  *
 *****************************/

