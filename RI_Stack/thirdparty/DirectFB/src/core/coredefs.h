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

#ifndef __COREDEFS_H__
#define __COREDEFS_H__

#include <config.h>
#include <core/fusion/fusion.h>

/* #define HEAVYDEBUG */

#include <misc/conf.h>

#ifdef PIC
#define DFB_DYNAMIC_LINKING
#endif

#define MAX_LAYERS       1

#if !defined(MPE_FEATURE_DEBUG)
    #define DFB_NOTEXT
#endif
    
    
#if defined(DFB_NOTEXT)

/* define non-debug macros such that 'statement with no effect' warnings aren't generated */
# ifdef __GNUC__
     /* define empty macros */
     #define INITMSG(x,...)		;
     #define ERRORMSG(x,...)	;
     #define PERRORMSG(x,...)	;
     #define ONCE(x)			;
     #define BUG(x)				;
     #define CAUTION(x)			;
# else
     /* assuming dfb_printf will simply 'eat' output logging strings in NOTEXT mode */
     #define INITMSG			dfb_printf
     #define ERRORMSG			dfb_printf
     #define PERRORMSG			dfb_printf
     #define ONCE				dfb_printf
     #define BUG				dfb_printf
     #define CAUTION			dfb_printf
# endif

#else

#define INITMSG    if (!dfb_config->quiet) dfb_printf
#define ERRORMSG   if (!dfb_config->quiet) dfb_printf
#define PERRORMSG  if (!dfb_config->quiet) dfb_printf
#define DLERRORMSG if (!dfb_config->quiet) dfb_printf
#define ONCE(msg)   {                                                          \
                         static int print = 1;                                 \
                         if (print) {                                          \
                              dfb_printf( "(!) *** [%s] *** %s (%d)\n",        \
                                       msg, __FILE__, __LINE__ );              \
                              print = 0;                                       \
                         }                                                     \
                    }

#define BUG(x)     dfb_printf( " (!?!)  *** BUG ALERT [%s] *** %s (%d)\n",\
                               x, __FILE__, __LINE__ )

#define CAUTION(x) dfb_printf( " (!!!)  *** CAUTION [%s] *** %s (%d)\n",  \
                               x, __FILE__, __LINE__ )
#endif


#if defined(DFB_DEBUG) && !defined(DFB_NOTEXT)

     #include <misc/util.h>   /* for dfb_get_millis() */
     
     #ifdef HEAVYDEBUG
          #define HEAVYDEBUGMSG(args) if (!dfb_config || dfb_config->debug){ \
										  dfb_printf("(=) ");                \
										  dfb_printf args ;                  \
									  }
     #else
          #define HEAVYDEBUGMSG(args)
     #endif

          #define DEBUGMSG(args) if (!dfb_config || dfb_config->debug){ \
									 dfb_printf("(-) ");                \
									 dfb_printf args ;                  \
								 }

     #define DFB_ASSERT(exp)  if (!(exp)) {                                    \
                                   dfb_printf( "(!) [%d: %5lld] *** "     \
                                                    "Assertion [%s] failed! "  \
                                                    "*** %s (%d)\n", 0,        \
                                                    0, #exp,                   \
                                                    __FILE__, __LINE__ );      \
                              }

#else
     #define HEAVYDEBUGMSG(args)
     #define DEBUGMSG(args)
     #define DFB_ASSERT(exp)       do { } while (0)
#endif

#endif
