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

#ifndef __INTERFACE_IMPLEMENTATION_H__
#define __INTERFACE_IMPLEMENTATION_H__

#include <directfb_internals.h>

static const char *
GetType(void);

static const char *
GetImplementation(void);

static DFBResult
Allocate( void **interface );

static DFBInterfaceFuncs interface_funcs = {
     GetType,
     GetImplementation,
     Allocate,
     (DFBResult (*)( void *, ... )) Probe,
     (DFBResult (*)( void *, ... )) Construct
};

#define DFB_INTERFACE_IMPLEMENTATION(type, impl)  \
                                                  \
static const char *                               \
GetType(void)                                     \
{                                                 \
     return #type;                                \
}                                                 \
                                                  \
static const char *                               \
GetImplementation(void)                           \
{                                                 \
     return #impl;                                \
}                                                 \
                                                  \
static DFBResult                                  \
Allocate( void **interface )                      \
{                                                 \
     DFB_ALLOCATE_INTERFACE( *interface, type );  \
     if (*interface == NULL)                      \
         return DFB_NOSYSTEMMEMORY;               \
     return DFB_OK;                               \
}                                                 \
                                                  \
DFBResult type##_##impl(void);                    \
DFBResult                                         \
type##_##impl (void)                              \
{                                                 \
     return DFBRegisterInterface( &interface_funcs ); \
}

#endif
