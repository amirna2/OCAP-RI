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

#ifndef __CORE_H__
#define __CORE_H__

#include <core/fusion/fusion_types.h>
#include <core/fusion/lock.h>
#include <directfb.h>
#include "coretypes.h"
#include "coredefs.h"

/*
 * Cleanup function, callback of a cleanup stack entry.
 */
typedef void (*CoreCleanupFunc)(void *data, int emergency);

/*
 * Process local core data. Shared between threads.
 */
typedef struct {
     int                    refs;       /* references to the core */
     int                    fid;        /* fusion id */
     bool                   master;     /* if we are the master fusionee */
     FusionArena           *arena;      /* DirectFB Core arena */
} CoreData;

extern CoreData *dfb_core;


/*
 * called by DirectFBInit
 */
DFBResult
dfb_core_init( int *argc, char **argv[] );

/*
 * Called by DirectFBCreate(), initializes all core parts if needed and
 * increases the core reference counter.
 */
DFBResult
dfb_core_ref(void);

/*
 * Called by IDirectFB::Destruct() or by core_deinit_check() via atexit(),
 * decreases the core reference counter and deinitializes all core parts
 * if reference counter reaches zero.
 */
void
dfb_core_unref(void);

/*
 * Returns true if the calling process is the master fusionee,
 * i.e. handles input drivers running their threads.
 */
bool
dfb_core_is_master(void);

/*
 * Suspends all core parts, stopping input threads, closing devices...
 */
DFBResult
dfb_core_suspend(void);

/*
 * Resumes all core parts, reopening devices, starting input threads...
 */
DFBResult
dfb_core_resume(void);

#endif

