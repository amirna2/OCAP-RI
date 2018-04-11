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

#ifndef __MISC__MEM_H__
#define __MISC__MEM_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <config.h>

#ifndef DFB_PAGE_SIZE
#ifdef _SC_PAGE_SIZE
#define DFB_PAGE_SIZE     (sysconf( _SC_PAGE_SIZE ))
#else
#define DFB_PAGE_SIZE     4096  /* FIXME: do not guess */
#endif
#endif

#define DFB_PAGE_ALIGN(x) ((((x) + DFB_PAGE_SIZE - 1) / DFB_PAGE_SIZE) * DFB_PAGE_SIZE)

void * (dfb_malloc)(size_t bytes);
void * (dfb_calloc)(size_t count, size_t bytes);
void * (dfb_realloc)(void *mem, size_t bytes);
void   (dfb_free)(void* mem);
char * (dfb_strdup)(const char *s);

#ifdef DFB_DEBUG

void  dfb_dbg_print_memleaks();
void  dfb_dbg_free   ( char *file, int line, char *func, char *what, void *mem );
void *dfb_dbg_malloc ( char *file, int line, char *func, size_t bytes );
void *dfb_dbg_calloc ( char *file, int line, char *func, size_t count, size_t bytes);
void *dfb_dbg_realloc( char *file, int line, char *func, char *what, void *mem, size_t bytes );
char *dfb_dbg_strdup ( char *file, int line, char *func, const char *string );

#define DFBFREE(mem)           dfb_dbg_free(    __FILE__, __LINE__, "Unknown", #mem,mem )
#define DFBMALLOC(bytes)       dfb_dbg_malloc(  __FILE__, __LINE__, "Unknown", bytes )
#define DFBCALLOC(count,bytes) dfb_dbg_calloc(  __FILE__, __LINE__, "Unknown", count, bytes )
#define DFBREALLOC(mem,bytes)  dfb_dbg_realloc( __FILE__, __LINE__, "Unknown", #mem, mem, bytes )
#define DFBSTRDUP(string)      dfb_dbg_strdup(  __FILE__, __LINE__, "Unknown", string )

#else

#define DFBFREE     dfb_free
#define DFBMALLOC   dfb_malloc
#define DFBCALLOC   dfb_calloc
#define DFBREALLOC  dfb_realloc
#define DFBSTRDUP   dfb_strdup

#endif

#ifdef __cplusplus
}
#endif

#endif
