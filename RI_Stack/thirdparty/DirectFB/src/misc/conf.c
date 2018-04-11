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
#include "directfb.h"
#include "core/coredefs.h"
#include "core/coretypes.h"
#include "core/surfaces.h"
#include "core/layers.h"
#include "conf.h"
#include "util.h"
#include "mem.h"


DFBConfig *dfb_config = NULL;

static const char *config_usage =
    "DirectFB version " DIRECTFB_VERSION "\n\n"
    " --dfb-help                      "
    "Output DirectFB usage information and exit\n"
    " --dfb:<option>[,<option>]...    "
    "Pass options to DirectFB (see below)\n"
    "\n"
    "DirectFB options:\n\n"
    "  mode=<width>x<height>          "
    "Set the default resolution\n"
    "  depth=<pixeldepth>             "
    "Set the default pixel depth\n"
    "  pixelformat=<pixelformat>      "
    "Set the default pixel format\n"
    "  quiet                          "
    "No text output except debugging\n"
    "  [no-]banner                    "
    "Show DirectFB Banner on startup\n"
    "  [no-]debug                     "
    "Enable debug output\n"
#ifdef USE_MMX
    "  [no-]mmx                       "
    "Enable mmx support\n"
#endif
    "  [no-]argb-font                 "
    "Load glyphs into ARGB surfaces\n"
    "  videoram-limit=<amount>        "
    "Limit amount of Video RAM in kb\n"
    "\n";

typedef struct {
     char                  *string;
     DFBSurfacePixelFormat  format;
} FormatString;
 
static const FormatString format_strings[] = {
     { "A8",       DSPF_A8       },
     { "ARGB",     DSPF_ARGB     },
     { "ARGB1555", DSPF_ARGB1555 },
     { "I420",     DSPF_I420     },
     { "LUT8",     DSPF_LUT8     },
     { "RGB16",    DSPF_RGB16    },
     { "RGB24",    DSPF_RGB24    },
     { "RGB32",    DSPF_RGB32    },
     { "RGB332",   DSPF_RGB332   },
     { "UYVY",     DSPF_UYVY     },
     { "YUY2",     DSPF_YUY2     },
     { "YV12",     DSPF_YV12     }
};

#define NUM_FORMAT_STRINGS (sizeof(format_strings) / sizeof(FormatString))

static int
format_string_compare (const void *key,
                       const void *base)
{
  return strcmp ((const char *) key, ((const FormatString *) base)->string);
}

static DFBSurfacePixelFormat
parse_pixelformat( const char *format )
{
     FormatString *format_string;
      
     format_string = bsearch( format, format_strings,
                              NUM_FORMAT_STRINGS, sizeof(FormatString),
                              format_string_compare );
     if (!format_string)
          return DSPF_UNKNOWN;

     return format_string->format;
}


/*
 * The following function isn't used because the configuration should
 * only go away if the application is completely terminated. In that case
 * the memory is freed anyway.
 */

#if 0
static void config_cleanup()
{
     if (!dfb_config) {
          BUG("config_cleanup() called with no config allocated!");
          return;
     }

     if (dfb_config->fb_device)
          DFBFREE( dfb_config->fb_device );

     if (dfb_config->layer_bg_filename)
          DFBFREE( dfb_config->layer_bg_filename );

     DFBFREE( dfb_config );
     dfb_config = NULL;
}
#endif

/*
 * allocates config and fills it with defaults
 */
static DFBResult config_allocate( void )
{
     if (dfb_config)
          return DFB_OK;

     if ((dfb_config = (DFBConfig*) DFBCALLOC( 1, sizeof(DFBConfig) )) == NULL)
         return DFB_NOSYSTEMMEMORY;

     dfb_config->banner                   = true;
     dfb_config->debug                    = true;
     dfb_config->deinit_check             = true;
     dfb_config->software_only            = false;
     dfb_config->mmx                      = true;

     return DFB_OK;
}

const char *dfb_config_usage( void )
{
     return config_usage;
}

DFBResult dfb_config_set( const char *name, const char *value )
{
     if (strcmp (name, "mode" ) == 0) {
          if (value) {
               int width, height;

               if (sscanf( value, "%dx%d", &width, &height ) < 2) {
                    ERRORMSG("DirectFB/Config 'mode': Could not parse mode!\n");
                    return DFB_INVARG;
               }

               dfb_config->mode.width  = width;
               dfb_config->mode.height = height;
          }
          else {
               ERRORMSG("DirectFB/Config 'mode': No mode specified!\n");
               return DFB_INVARG;
          }
     } else
     if (strcmp (name, "depth" ) == 0) {
          if (value) {
               int depth;

               if (sscanf( value, "%d", &depth ) < 1) {
                    ERRORMSG("DirectFB/Config 'depth': Could not parse value!\n");
                    return DFB_INVARG;
               }

               dfb_config->mode.depth = depth;
          }
          else {
               ERRORMSG("DirectFB/Config 'depth': No value specified!\n");
               return DFB_INVARG;
          }
     } else
     if (strcmp (name, "pixelformat" ) == 0) {
          if (value) {
               DFBSurfacePixelFormat format;

               format = parse_pixelformat( value );
               if (format == DSPF_UNKNOWN) {
                    ERRORMSG("DirectFB/Config 'pixelformat': Could not parse format!\n");
                    return DFB_INVARG;
               }

               dfb_config->mode.format = format;
          }
          else {
               ERRORMSG("DirectFB/Config 'pixelformat': No format specified!\n");
               return DFB_INVARG;
          }
     } else
     if (strcmp (name, "videoram-limit" ) == 0) {
          if (value) {
               int limit;
               
               if (sscanf( value, "%d", &limit ) < 1) {
                    ERRORMSG("DirectFB/Config 'videoram-limit': Could not parse value!\n");
                    return DFB_INVARG;
               }

               if (limit < 0)
                    limit = 0;
               
               dfb_config->videoram_limit = DFB_PAGE_ALIGN(limit<<10);
          }
          else {
               ERRORMSG("DirectFB/Config 'videoram-limit': No value specified!\n");
               return DFB_INVARG;
          }
     } else
     if (strcmp (name, "quiet" ) == 0) {
          dfb_config->quiet = true;
     } else
     if (strcmp (name, "banner" ) == 0) {
          dfb_config->banner = true;
     } else
     if (strcmp (name, "no-banner" ) == 0) {
          dfb_config->banner = false;
     } else
     if (strcmp (name, "debug" ) == 0) {
          dfb_config->debug = true;
     } else
     if (strcmp (name, "no-debug" ) == 0) {
          dfb_config->debug = false;
     } else
     if (strcmp (name, "mmx" ) == 0) {
          dfb_config->mmx = true;
     } else
     if (strcmp (name, "no-mmx" ) == 0) {
          dfb_config->mmx = false;
     } else
     if (strcmp (name, "argb-font" ) == 0) {
          dfb_config->argb_font = true;
     } else
     if (strcmp (name, "no-argb-font" ) == 0) {
          dfb_config->argb_font = false;
     } else
     if (strcmp (name, "desktop-buffer-mode" ) == 0) {
          if (value) {
               if (strcmp( value, "auto" ) == 0) {
                    dfb_config->buffer_mode = -1;
               } else
               if (strcmp( value, "backvideo" ) == 0) {
                    dfb_config->buffer_mode = DLBM_BACKVIDEO;
               } else
               if (strcmp( value, "backsystem" ) == 0) {
                    dfb_config->buffer_mode = DLBM_BACKSYSTEM;
               } else
               if (strcmp( value, "frontonly" ) == 0) {
                    dfb_config->buffer_mode = DLBM_FRONTONLY;
               } else {
                    ERRORMSG( "DirectFB/Config: Unknown buffer mode "
                              "'%s'!\n", value );
                    return DFB_INVARG;
               }
          }
          else {
               ERRORMSG( "DirectFB/Config: "
                         "No desktop buffer mode specified!\n" );
               return DFB_INVARG;
          }
     } else
          return DFB_UNSUPPORTED;

     return DFB_OK;
}

DFBResult dfb_config_init( int *argc, char **argv[] )
{
     DFBResult ret;
     int i;

     if (dfb_config)
          return DFB_OK;

     if ((ret = config_allocate()) != DFB_OK)
         return ret;

     if (argc && argv) {
          for (i = 1; i < *argc; i++) {

               if (strcmp ((*argv)[i], "--dfb-help") == 0) {
                    dfb_printf( config_usage );
                    exit(1);
               }

               if (strncmp ((*argv)[i], "--dfb:", 6) == 0) {
                    int len = strlen( (*argv)[i] ) - 6;
                    char *arg = (*argv)[i] + 6;

                    while (len) {
                         char *name, *value, *comma;
                         
                         if ((comma = strchr( arg, ',' )) != NULL)
                              *comma = '\0';

                         if (strcmp (arg, "help") == 0) {
                              dfb_printf( config_usage );
                              exit(1);
                         }

                         if ((name = DFBSTRDUP( arg )) == NULL)
                             return DFB_NOSYSTEMMEMORY;
                         len -= strlen( arg );

                         value = strchr( name, '=' );
                         if (value)
                              *value++ = '\0';

                         ret = dfb_config_set( name, value );

                         DFBFREE( name );

                         if (ret == DFB_OK)
                              (*argv)[i] = NULL;
                         else if (ret != DFB_UNSUPPORTED)
                              return ret;
                         
                         if (comma && len) {
                              arg = comma + 1;
                              len--;
                         }
                    }
               }
          }

          for (i = 1; i < *argc; i++) {
               int k;

               for (k = i; k < *argc; k++)
                   if ((*argv)[k] != NULL)
                       break;

               if (k > i)
                   {
                   int j;

               k -= i;

               for (j = i + k; j < *argc; j++)
                       (*argv)[j-k] = (*argv)[j];

                       *argc -= k;
               }
          }
     }

     return DFB_OK;
}
