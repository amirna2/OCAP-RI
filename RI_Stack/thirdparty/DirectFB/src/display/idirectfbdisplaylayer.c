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
#include "directfb_internals.h"
#include "core/core.h"
#include "core/coredefs.h"
#include "core/coretypes.h"
#include "core/surfaces.h"
#include "core/gfxcard.h"
#include "core/layers.h"
#include "core/state.h"
#include "idirectfbdisplaylayer.h"
#include "idirectfbsurface.h"
#include "idirectfbsurface_layer.h"
#include "gfx/convert.h"
#include "misc/mem.h"

/*
 * private data struct of IDirectFB
 */
typedef struct {
     int                             ref;    /* reference counter */
     DFBDisplayLayerCooperativeLevel level;  /* current cooperative level */
     DisplayLayer                    *layer; /* pointer to core data */
} IDirectFBDisplayLayer_data;



static void
IDirectFBDisplayLayer_Destruct( IDirectFBDisplayLayer *thiz )
{
     IDirectFBDisplayLayer_data *data = (IDirectFBDisplayLayer_data*)thiz->priv;

     if (data->level == DLSCL_EXCLUSIVE)
          dfb_layer_release( data->layer, true );
     
     DFB_DEALLOCATE_INTERFACE( thiz );
}

static DFBResult
IDirectFBDisplayLayer_AddRef( IDirectFBDisplayLayer *thiz )
{
     INTERFACE_GET_DATA(IDirectFBDisplayLayer)

     data->ref++;

     return DFB_OK;
}

static DFBResult
IDirectFBDisplayLayer_Release( IDirectFBDisplayLayer *thiz )
{
     INTERFACE_GET_DATA(IDirectFBDisplayLayer)

     if (--data->ref == 0)
          IDirectFBDisplayLayer_Destruct( thiz );

     return DFB_OK;
}

static DFBResult
IDirectFBDisplayLayer_GetID( IDirectFBDisplayLayer *thiz,
                             DFBDisplayLayerID     *id )
{
     INTERFACE_GET_DATA(IDirectFBDisplayLayer)

     if (!id)
          return DFB_INVARG;

     *id = dfb_layer_id( data->layer );

     return DFB_OK;
}

static DFBResult
IDirectFBDisplayLayer_GetDescription( IDirectFBDisplayLayer      *thiz,
                                      DFBDisplayLayerDescription *desc )
{
     DFBDisplayLayerDescription description;

     INTERFACE_GET_DATA(IDirectFBDisplayLayer)

     if (!desc)
          return DFB_INVARG;

     dfb_layer_description( data->layer, &description );

     *desc = description;

     return DFB_OK;
}

static DFBResult
IDirectFBDisplayLayer_GetSurface( IDirectFBDisplayLayer  *thiz,
                                  IDirectFBSurface      **interface )
{
     DFBResult         ret;
     IDirectFBSurface *surface;

     INTERFACE_GET_DATA(IDirectFBDisplayLayer)

     if (!interface)
          return DFB_INVARG;

     DFB_ALLOCATE_INTERFACE( surface, IDirectFBSurface );
     if (surface == NULL)
         return DFB_NOSYSTEMMEMORY;

     ret = IDirectFBSurface_Layer_Construct( surface, NULL, NULL,
                                             data->layer, DSCAPS_NONE );
     if (ret)
          return ret;

     *interface = surface;

     return DFB_OK;
}

static DFBResult
IDirectFBDisplayLayer_GetConfiguration( IDirectFBDisplayLayer *thiz,
                                        DFBDisplayLayerConfig *config )
{
     INTERFACE_GET_DATA(IDirectFBDisplayLayer)

     if (!config)
          return DFB_INVARG;

     return dfb_layer_get_configuration( data->layer, config );
}

static DFBResult
IDirectFBDisplayLayer_TestConfiguration( IDirectFBDisplayLayer      *thiz,
                                         DFBDisplayLayerConfig      *config,
                                         DFBDisplayLayerConfigFlags *failed )
{
     INTERFACE_GET_DATA(IDirectFBDisplayLayer)

     if (!config)
          return DFB_INVARG;

     return dfb_layer_test_configuration( data->layer, config, failed );
}

DFBResult
IDirectFBDisplayLayer_Construct( IDirectFBDisplayLayer *thiz,
                                 DisplayLayer          *layer )
{
     DFB_ALLOCATE_INTERFACE_DATA(thiz, IDirectFBDisplayLayer)
     if (data == NULL)
         return DFB_NOSYSTEMMEMORY;

     data->ref = 1;
     data->layer = layer;

     thiz->AddRef = IDirectFBDisplayLayer_AddRef;
     thiz->Release = IDirectFBDisplayLayer_Release;
     thiz->GetID = IDirectFBDisplayLayer_GetID;
     thiz->GetDescription = IDirectFBDisplayLayer_GetDescription;
     thiz->GetSurface = IDirectFBDisplayLayer_GetSurface;
     thiz->GetConfiguration = IDirectFBDisplayLayer_GetConfiguration;
     thiz->TestConfiguration = IDirectFBDisplayLayer_TestConfiguration;

     return DFB_OK;
}

