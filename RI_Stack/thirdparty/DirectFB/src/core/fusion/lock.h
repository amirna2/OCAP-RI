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

#ifndef __LOCK_H__
#define __LOCK_H__

#ifdef __cplusplus
extern "C"
{
#endif

#include "fusion_types.h"
#include <external.h>

#define FusionSkirmish      pthread_mutex_t

/*
 * Initialize.
 */
FusionResult skirmish_init    (FusionSkirmish *skirmish);

#define skirmish_prevail(s) pthread_mutex_lock (s)
#define skirmish_swoop(s)   pthread_mutex_trylock (s)
#define skirmish_dismiss(s) pthread_mutex_unlock (s)
#define skirmish_destroy(s) pthread_mutex_destroy (s)

#ifdef __cplusplus
}
#endif

#endif /* __LOCK_H__ */

