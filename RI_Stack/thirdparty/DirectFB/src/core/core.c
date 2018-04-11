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
#include <core/fusion/fusion.h>
#include <core/fusion/arena.h>
#include <core/fusion/list.h>
#include <core/fusion/shmalloc.h>
#include <directfb.h>
#include <core/coredefs.h>
#include <core/coretypes.h>
#include <core/core.h>
#include <core/core_parts.h>
#include <core/system.h>
#include <misc/mem.h>
#include <misc/memcpy.h>
#include <misc/util.h>

/*
 * one entry in the cleanup stack
 */
struct _CoreCleanup {
     FusionLink       link;

     CoreCleanupFunc  func;        /* the cleanup function to be called */
     void            *data;        /* context of the cleanup function */
     bool             emergency;   /* if true, cleanup is also done during
                                      emergency shutdown (from signal hadler) */
};

/*
 * list of cleanup functions
 */
static FusionLink *core_cleanups = NULL;

CoreData *dfb_core = NULL;

#ifdef DFB_DYNAMIC_LINKING
/*
 * the library handle for dlopen'ing ourselves
 */
static void* dfb_lib_handle = NULL;
#endif


static CorePart *core_parts[] = {
     &dfb_core_colorhash,
     &dfb_core_system,
     &dfb_core_gfxcard,
     &dfb_core_layers
};

#define NUM_CORE_PARTS (sizeof(core_parts)/sizeof(CorePart*))

static int
dfb_core_initialize( FusionArena *arena, void *ctx );

static int
dfb_core_shutdown( FusionArena *arena, void *ctx, bool emergency );

#ifdef NOTYET
/*
 * ckecks if stack is clean, otherwise prints warning, then calls core_deinit()
 */
static void
dfb_core_deinit_check(void)
{
     if (dfb_core && dfb_core->refs) {
          DEBUGMSG( ("DirectFB/core: WARNING - Application "
                     "exitted without deinitialization of DirectFB!\n") );
          if (dfb_core->master) {
               dfb_core->refs = 1;
               dfb_core_unref();
          }
     }

#ifdef DFB_DEBUG
     dfb_dbg_print_memleaks();
#endif
}
#endif

DFBResult
dfb_core_init( int *argc, char **argv[] )
{
     DFB_UNUSED_PARAM(argc);
     DFB_UNUSED_PARAM(argv);
     return DFB_OK;
}

void dfb_core_deinit_emergency(void);

DFBResult
dfb_core_ref()
{
     int fid;
     int ret;

     /* check for multiple calls, do reference counting */
     if (dfb_core && dfb_core->refs++)
          return DFB_OK;

     INITMSG( "Single Application Core.%s ("__DATE__" "__TIME__")\n", 
#ifdef USE_MMX
		" (with MMX support)"
#else
		""
#endif
		 );

     fid = fusion_init();
     if (fid < 0)
          return DFB_INIT;

     DEBUGMSG( ("DirectFB/Core: fusion id %d\n", fid) );

     /* allocate local data */
     if ((dfb_core = DFBCALLOC( 1, sizeof(CoreData) )) == NULL)
         return DFB_NOSYSTEMMEMORY;

     dfb_core->refs  = 1;
     dfb_core->fid   = fid;

     ret = dfb_core_initialize( NULL, NULL );
     if (ret) {
          ERRORMSG("DirectFB/Core: Error during initialization (%s)\n",
                   DirectFBErrorString( ret ));
          dfb_core_deinit_emergency();
          return ret;
     }

     return DFB_OK;
}

bool
dfb_core_is_master()
{
     return dfb_core->master;
}

void
dfb_core_unref()
{
     if (!dfb_core)
          return;

     if (!dfb_core->refs)
          return;

     if (--dfb_core->refs)
          return;

     dfb_core_shutdown( NULL, NULL, false );

     fusion_exit();

     DFBFREE( dfb_core );
     dfb_core = NULL;

#ifdef DFB_DYNAMIC_LINKING
     if (dfb_lib_handle) {
          dlclose(dfb_lib_handle);
          dfb_lib_handle = NULL;
     }
#endif
}

void
dfb_core_deinit_emergency(void)
{
     if (!dfb_core || !dfb_core->refs)
          return;

     dfb_core->refs = 0;

     dfb_core_shutdown( NULL, NULL, true );

     fusion_exit();

     DFBFREE( dfb_core );
     dfb_core = NULL;
}

static int
dfb_core_initialize( FusionArena *arena, void *ctx )
{
     int       i;
     DFBResult ret;

     DFB_UNUSED_PARAM(arena);
     DFB_UNUSED_PARAM(ctx);

     DEBUGMSG( ("DirectFB/Core: we are the master, initializing...\n") );

     dfb_core->master = true;

     for (i=0; i<NUM_CORE_PARTS; i++)
     {
          ret = dfb_core_part_initialize( core_parts[i] );
          if (ret)
               return ret;
     }

     return 0;
}

static int
dfb_core_shutdown( FusionArena *arena, void *ctx, bool emergency )
{
     int i;

     DFB_UNUSED_PARAM(arena);
     DFB_UNUSED_PARAM(ctx);

     DEBUGMSG( ("DirectFB/Core: shutting down!\n") );

     while (core_cleanups) {
          CoreCleanup *cleanup = (CoreCleanup *)core_cleanups;

          core_cleanups = core_cleanups->next;

          if (cleanup->emergency || !emergency)
               cleanup->func( cleanup->data, emergency );

          DFBFREE( cleanup );
     }

     for (i=NUM_CORE_PARTS-1; i>=0; i--)
          dfb_core_part_shutdown( core_parts[i], emergency );

     return 0;
}

