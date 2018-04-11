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
#include "external.h"
#include "directfb.h"
#include "directfb_internals.h"
#include "directfb_version.h"
#include "misc/conf.h"
#include "core/core.h"
#include "core/coredefs.h"
#include "core/coretypes.h"
#include "core/system.h"	/* Needed for CoreSystemFuncs */
#include "core/layers.h"
#include "core/state.h"
#include "core/gfxcard.h"
#include "core/surfaces.h"
#include "gfx/convert.h"
#include "misc/mem.h"
#include "display/idirectfbsurface.h"
#include "idirectfb.h"


IDirectFB *idirectfb_singleton = NULL;

DirectFB2External* g_dfb2ext = NULL;


static DFBResult apply_configuration( IDirectFB *dfb );

DFBResult IDirectFBFont_FT2(void);

/* Function prototypes for calling from MPE */
LIBEXPORT void SetupDirectFB(void *fgptr, CoreSystemFuncs **csptr);
LIBEXPORT void dfb_get_structs(void **sptr1, void **sptr2);

/*
 * Version checking
 */
const unsigned int directfb_major_version = DIRECTFB_MAJOR_VERSION;
const unsigned int directfb_minor_version = DIRECTFB_MINOR_VERSION;
const unsigned int directfb_micro_version = DIRECTFB_MICRO_VERSION;
const unsigned int directfb_binary_age    = DIRECTFB_BINARY_AGE;
const unsigned int directfb_interface_age = DIRECTFB_INTERFACE_AGE;

/* core_system_funcs...holds system funcs, populated on initialization.
 */
CoreSystemFuncs core_system_funcs;	/* Global...extern in core_system.h */

/* Pointer to function to setup Graphics Driver Funcs ...gfxcard.c*/
extern void (*getgraphicsdriverfuncs)(GraphicsDriverFuncs *gd_funcs);

const char *
DirectFBCheckVersion( unsigned int required_major,
                      unsigned int required_minor,
                      unsigned int required_micro )
{
     if (required_major > DIRECTFB_MAJOR_VERSION)
          return "DirectFB version too old (major mismatch)";
     if (required_major < DIRECTFB_MAJOR_VERSION)
          return "DirectFB version too new (major mismatch)";
     if (required_minor > DIRECTFB_MINOR_VERSION)
          return "DirectFB version too old (minor mismatch)";
     if (required_minor < DIRECTFB_MINOR_VERSION)
          return "DirectFB version too new (minor mismatch)";
     if (required_micro < DIRECTFB_MICRO_VERSION - DIRECTFB_BINARY_AGE)
          return "DirectFB version too new (micro mismatch)";
     if (required_micro > DIRECTFB_MICRO_VERSION)
          return "DirectFB version too old (micro mismatch)";

     return NULL;
}

const char *
DirectFBUsageString( void )
{
     return dfb_config_usage();
}

DFBResult
DirectFBInit( int *argc, char **argv[], DirectFB2External *ext)
{
     DFBResult ret;

     g_dfb2ext = ext;

     ret = dfb_config_init( argc, argv );
     if (ret)
          return ret;

     ret = dfb_core_init( argc, argv );
     if (ret)
          return ret;

     return DFB_OK;
}

DFBResult
DirectFBSetOption( const char *name, const char *value )
{
     DFBResult ret;

     if (dfb_config == NULL) {
          ERRORMSG( "DirectFBSetOption: DirectFBInit has to be "
                    "called before DirectFBSetOption!\n" );
          return DFB_INIT;
     }

     if (idirectfb_singleton) {
          ERRORMSG( "DirectFBSetOption: DirectFBSetOption has to be "
                    "called before DirectFBCreate!\n" );
          return DFB_INIT;
     }

     if (!name)
          return DFB_INVARG;

     ret = dfb_config_set( name, value );
     if (ret)
          return ret;

     return DFB_OK;
}

/* Called from MPE.
 */
/* Sets up a function pointer and  get function struct for startup */
void SetupDirectFB(void *fgptr, CoreSystemFuncs **csptr)
{
	getgraphicsdriverfuncs = fgptr;	/* Pass funcs setup ptr into DirectFB */
	*csptr = &core_system_funcs;		/* Pass back table to store sys funcs */
}

/* Gets extra structs needed by driver */
void dfb_get_structs(void **sptr1, void **sptr2)
{
	/* e.g. sptr1 points to MPE::g_dfb2ext, *sptr1 = x is MPE::g_dfb2ext = x.
	 * Therefor, *sptr1 = g_dfb2ext is MPE::g_dfb2ext = DFB::g_dfb2ext, so
	 * that MPE::g_dfb2ext points to the same structure in DFB. In these cases
	 * we are dealing with pointers to structs.
	 */
	*sptr1 = g_dfb2ext;
	*sptr2 = dfb_config;
}

/*
 * Programs have to call this to get the super interface
 * which is needed to access other functions
 */
DFBResult
DirectFBCreate( IDirectFB **interface )
{
     DFBResult ret;

     if (!dfb_config) {
          /*  don't use ERRORMSG() here, it uses dfb_config  */
          dfb_printf(
                   "(!) DirectFBCreate: DirectFBInit has to be "
                   "called before DirectFBCreate!\n" );
          return DFB_INIT;
     }

     if (!interface)
          return DFB_INVARG;

     if (idirectfb_singleton) {
          idirectfb_singleton->AddRef( idirectfb_singleton );
          *interface = idirectfb_singleton;
          return DFB_OK;
     }

     ret = dfb_core_ref();
     if (ret)
          return ret;

     DFB_ALLOCATE_INTERFACE( idirectfb_singleton, IDirectFB );
     if (idirectfb_singleton == NULL)
         return DFB_NOSYSTEMMEMORY;

     IDirectFB_Construct( idirectfb_singleton );
     
     if (dfb_core_is_master()) {
          ret = apply_configuration( idirectfb_singleton );
          if (ret) {
               idirectfb_singleton->Release( idirectfb_singleton );
               idirectfb_singleton = NULL;
               return ret;
          }
     }

     if ((ret = IDirectFBFont_FT2()) != DFB_OK)
         return ret;

     *interface = idirectfb_singleton;

     
     if (!dfb_config->quiet && dfb_config->banner) {
          dfb_printf( "\n" );
          dfb_printf( "       ---------------------- DirectFB v%d.%d.%d ---------------------\n",
                           DIRECTFB_MAJOR_VERSION, DIRECTFB_MINOR_VERSION, DIRECTFB_MICRO_VERSION );
          dfb_printf( "             (c) 2000-2002  convergence integrated media GmbH  \n" );
          dfb_printf( "             (c) 2002       convergence GmbH                   \n" );
          dfb_printf( "        -----------------------------------------------------------\n" );
          dfb_printf( "\n" );
     }


     return DFB_OK;
}

DFBResult
DirectFBError( const char *msg, DFBResult error )
{
     if (msg)
          dfb_printf( "(#) DirectFBError [%s]: %s\n", msg,
                   DirectFBErrorString( error ) );
     else
          dfb_printf( "(#) DirectFBError: %s\n",
                   DirectFBErrorString( error ) );

     return error;
}

const char *
DirectFBErrorString( DFBResult error )
{
     switch (error) {
          case DFB_OK:
               return "Everything OK!";
          case DFB_FAILURE:
               return "General failure!";
          case DFB_INIT:
               return "General initialization failure!";
          case DFB_BUG:
               return "Internal bug!";
          case DFB_DEAD:
               return "Interface is dead!";
          case DFB_UNSUPPORTED:
               return "Not supported!";
          case DFB_UNIMPLEMENTED:
               return "Unimplemented!";
          case DFB_ACCESSDENIED:
               return "Access denied!";
          case DFB_INVARG:
               return "Invalid argument(s)!";
          case DFB_NOSYSTEMMEMORY:
               return "Out of system memory!";
          case DFB_NOVIDEOMEMORY:
               return "Out of video memory!";
          case DFB_LOCKED:
               return "Resource (already) locked!";
          case DFB_BUFFEREMPTY:
               return "Buffer is empty!";
          case DFB_FILENOTFOUND:
               return "File not found!";
          case DFB_IO:
               return "General I/O failure!";
          case DFB_NOIMPL:
               return "Interface implementation not available!";
          case DFB_MISSINGFONT:
               return "No font has been set!";
          case DFB_TIMEOUT:
               return "Operation timed out!";
          case DFB_MISSINGIMAGE:
               return "No image has been set!";
          case DFB_BUSY:
               return "Resource in use (busy)!";
          case DFB_THIZNULL:
               return "'thiz' pointer is NULL!";
          case DFB_IDNOTFOUND:
               return "ID not found!";
          case DFB_INVAREA:
               return "Invalid area specified or detected!";
          case DFB_DESTROYED:
               return "Object has been destroyed!";
          case DFB_FUSION:
               return "Internal fusion (IPC) error detected!";
     }

     return "<UNKNOWN ERROR CODE>!";
}

DFBResult
DirectFBErrorFatal( const char *msg, DFBResult error )
{
     DirectFBError( msg, error );

     /* Deinit all stuff here. */
     if (dfb_core)
          dfb_core->refs = 1;
     dfb_core_unref();     /* for now, this dirty thing should work */

     exit( error );

     return error;
}

static DFBResult
apply_configuration( IDirectFB *dfb )
{
     DisplayLayer          *layer;
     DFBDisplayLayerConfig  layer_config;
     
     DFB_UNUSED_PARAM(dfb);

     /* the primary layer */
     layer = dfb_layer_at( DLID_PRIMARY );
     
     /* set buffer mode for desktop */
     layer_config.flags = DLCONF_BUFFERMODE;

     if (dfb_config->buffer_mode == -1) {
          CardCapabilities caps = dfb_gfxcard_capabilities();

          if (caps.accel & DFXL_BLIT)
               layer_config.buffermode = DLBM_BACKVIDEO;
          else
               layer_config.buffermode = DLBM_BACKSYSTEM;
     }
     else
          layer_config.buffermode = dfb_config->buffer_mode;

     if (dfb_layer_set_configuration( layer, &layer_config )) {
          ERRORMSG( "DirectFB/DirectFBCreate: "
                    "Setting desktop buffer mode failed!\n"
                    "     -> No virtual resolution support or not enough memory?\n"
                    "        Falling back to system back buffer.\n" );

          layer_config.buffermode = DLBM_BACKSYSTEM;

          if (dfb_layer_set_configuration( layer, &layer_config ))
               ERRORMSG( "DirectFB/DirectFBCreate: "
                         "Setting system memory desktop back buffer failed!\n"
                         "     -> Using front buffer only mode.\n" );
     }

     return DFB_OK;
}

