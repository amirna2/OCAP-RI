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
#include "util.h"

/*
 * translates errno to DirectFB DFBResult
 */
DFBResult errno2dfb( int erno )
{
     switch (erno) {
          case 0:
               return DFB_OK;
          case ENOENT:
               return DFB_FILENOTFOUND;
          case EACCES:
          case EPERM:
               return DFB_ACCESSDENIED;
          case EBUSY:
          case EAGAIN:
               return DFB_BUSY;
          case ENODEV:
          case ENXIO:
#ifdef ENOTSUP
          /* ENOTSUP is not defined on NetBSD */
          case ENOTSUP:
#endif
               return DFB_UNSUPPORTED;
     }

     return DFB_FAILURE;
}

int dfb_region_rectangle_intersect( DFBRegion          *region,
                                    const DFBRectangle *rect )
{
     int x2 = rect->x + rect->w - 1;
     int y2 = rect->y + rect->h - 1;

     if (region->x2 < rect->x ||
         region->y2 < rect->y ||
         region->x1 > x2 ||
         region->y1 > y2)
          return 0;

     region->x1 = DFB_MAX( region->x1, rect->x );
     region->y1 = DFB_MAX( region->y1, rect->y );
     region->x2 = DFB_MIN( region->x2, x2 );
     region->y2 = DFB_MIN( region->y2, y2 );

     return 1;
}

int dfb_unsafe_region_rectangle_intersect( DFBRegion          *region,
                                           const DFBRectangle *rect )
{
     if (region->x1 > region->x2) {
          int temp = region->x1;
          region->x1 = region->x2;
          region->x2 = temp;
     }

     if (region->y1 > region->y2) {
          int temp = region->y1;
          region->y1 = region->y2;
          region->y2 = temp;
     }

     return dfb_region_rectangle_intersect( region, rect );
}

int dfb_rectangle_intersect_by_unsafe_region( DFBRectangle *rectangle,
                                              DFBRegion    *region )
{
     /* validate region */
     if (region->x1 > region->x2) {
          int temp = region->x1;
          region->x1 = region->x2;
          region->x2 = temp;
     }

     if (region->y1 > region->y2) {
          int temp = region->y1;
          region->y1 = region->y2;
          region->y2 = temp;
     }

     /* adjust position */
     if (region->x1 > rectangle->x) {
          rectangle->w -= region->x1 - rectangle->x;
          rectangle->x = region->x1;
     }

     if (region->y1 > rectangle->y) {
          rectangle->h -= region->y1 - rectangle->y;
          rectangle->y = region->y1;
     }

     /* adjust size */
     if (region->x2 <= rectangle->x + rectangle->w)
        rectangle->w = region->x2 - rectangle->x + 1;

     if (region->y2 <= rectangle->y + rectangle->h)
        rectangle->h = region->y2 - rectangle->y + 1;

     /* set size to zero if there's no intersection */
     if (rectangle->w <= 0 || rectangle->h <= 0) {
          rectangle->w = 0;
          rectangle->h = 0;

          return 0;
     }

     return 1;
}

int dfb_rectangle_intersect( DFBRectangle       *rectangle,
                             const DFBRectangle *clip )
{
     DFBRegion region;
      
     region.x1 = clip->x;
     region.y1 = clip->y;
     region.x2 = clip->x + clip->w - 1;
     region.y2 = clip->y + clip->h - 1;

     /* adjust position */
     if (region.x1 > rectangle->x) {
          rectangle->w -= region.x1 - rectangle->x;
          rectangle->x = region.x1;
     }

     if (region.y1 > rectangle->y) {
          rectangle->h -= region.y1 - rectangle->y;
          rectangle->y = region.y1;
     }

     /* adjust size */
     if (region.x2 <= rectangle->x + rectangle->w)
          rectangle->w = region.x2 - rectangle->x + 1;

     if (region.y2 <= rectangle->y + rectangle->h)
          rectangle->h = region.y2 - rectangle->y + 1;

     /* set size to zero if there's no intersection */
     if (rectangle->w <= 0 || rectangle->h <= 0) {
          rectangle->w = 0;
          rectangle->h = 0;

          return 0;
     }

     return 1;
}

void dfb_rectangle_union ( DFBRectangle       *rect1,
                           const DFBRectangle *rect2 )
{
     if (!rect2->w || !rect2->h)
          return;

     if (rect1->w) {
          int temp = DFB_MIN (rect1->x, rect2->x);
          rect1->w = DFB_MAX (rect1->x + rect1->w, rect2->x + rect2->w) - temp;
          rect1->x = temp;
     }
     else {
          rect1->x = rect2->x;
          rect1->w = rect2->w;
     }

     if (rect1->h) {
          int temp = DFB_MIN (rect1->y, rect2->y);
          rect1->h = DFB_MAX (rect1->y + rect1->h, rect2->y + rect2->h) - temp;
          rect1->y = temp;
     }
     else {
          rect1->y = rect2->y;
          rect1->h = rect2->h;
     }
}

int DFB_ICEIL(float f)
{
        int ai, bi;
        double af, bf;

        af = (3 << 22) + 0.5 + (double)f;
        bf = (3 << 22) + 0.5 - (double)f;

#if defined(__GNUC__) && defined(__i386__)
        /*
         GCC generates an extra fstp/fld without this.
        */
        asm ("fstps %0" : "=m" (ai) : "t" (af) : "st");
        asm ("fstps %0" : "=m" (bi) : "t" (bf) : "st");
#else
        {
                union { int i; float f; } u;
                u.f = (float)af; ai = u.i;
                u.f = (float)bf; bi = u.i;
        }
#endif

        return (ai - bi + 1) >> 1;
}
