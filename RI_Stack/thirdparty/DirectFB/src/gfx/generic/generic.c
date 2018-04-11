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
#include <config.h>
#include <external.h>
#include <dfb_types.h>
#include "directfb.h"
#include "core/core.h"
#include "core/coredefs.h"
#include "core/coretypes.h"
#include "core/gfxcard.h"
#include "core/state.h"
#include "core/surfacemanager.h"
#include "core/palette.h"
#include "misc/gfx_util.h"
#include "misc/util.h"
#include "misc/conf.h"
#include "misc/memcpy.h"
#include "misc/cpu_accel.h"
#include "gfx/convert.h"
#include "gfx/util.h"
#include "generic.h"



#define MUL8RND(x, y, z)\
{ 						\
	register __u16 i;	\
						\
	i = (x * y) + 128;	\
	z = (i + (i >> 8)) >> 8;\
}

/*
 * state values
 */
static void *dst_org = NULL;
static void *src_org = NULL;
static unsigned int dst_pitch = 0;
static unsigned int src_pitch = 0;

static int dst_bpp   = 0;
static int src_bpp   = 0;

static DFBSurfaceCapabilities dst_caps = DSCAPS_NONE;
static DFBSurfaceCapabilities src_caps = DSCAPS_NONE;

static DFBSurfacePixelFormat src_format = DSPF_UNKNOWN;
static DFBSurfacePixelFormat dst_format = DSPF_UNKNOWN;

static int dst_height = 0;
static int src_height = 0;

static int dst_field_offset = 0;
static int src_field_offset = 0;

// Flags for negative reflective blits (translated to only destination values).
static int flaghneg = false;	// Assumed initially height and width are > 0
static int flagwneg = false;

DFBColor color;
__u16     alpha_const;

/*
 * operands
 */
void *Aop = NULL;
static void *Bop = NULL;

static __u32 Cop = 0;

#ifdef OPTIMIZATION
static __u32 Kop = 0;
#endif

static __u8 CbCop = 0;
static __u8 CrCop = 0;

/*
 * color keys
 */
static __u32 Dkey = 0;
static __u32 Skey = 0;

/*
 * color lookup tables
 */
static CorePalette *Alut = NULL;
static CorePalette *Blut = NULL;

/*
 * accumulators
 */
static Accumulator Aacc[ACC_WIDTH]; // FIXME: dynamically
static Accumulator Bacc[ACC_WIDTH]; // FIXME: dynamically
Accumulator Cacc;

/*
 * operations
 */
static GFunc gfuncs[32];

/*
 * dataflow control
 */
Accumulator *Xacc = NULL;
Accumulator *Dacc = NULL;
Accumulator *Sacc = NULL;

void        *Sop  = NULL;
CorePalette *Slut = NULL;

/* controls horizontal blitting direction */
int Ostep = 0;

int Dlength = 0;
int SperD = 0; /* for scaled routines only */

static int use_mmx = 0;

static pthread_mutex_t generic_lock = PTHREAD_MUTEX_INITIALIZER;

/* lookup tables for 2/3bit to 8bit color conversion */
#ifdef SUPPORT_RGB332
static const __u8 lookup3to8[] = { 0x00, 0x24, 0x49, 0x6d, 0x92, 0xb6, 0xdb, 0xff };
static const __u8 lookup2to8[] = { 0x00, 0x55, 0xaa, 0xff };
#endif

/********************************* Cop_to_Aop_PFI *****************************/
static void Cop_to_Aop_8(void)
{
     memset( Aop, (__u8)Cop, Dlength );
}

static void Cop_to_Aop_16(void)
{
     int    w, l = Dlength;
     __u32 *D = (__u32*)Aop;

     __u32 DCop = ((Cop << 16) | Cop);

     if (((long)D)&2) {         /* align */
          __u16* tmp=Aop;
          --l;
          *tmp = (__u16)Cop;
          D = (__u32*)(tmp+1);
     }

     w = (l >> 1);
     while (w) {
          *D = DCop;
          --w;
          ++D;
     }

     if (l & 1)                 /* do the last ential pixel */
          *((__u16*)D) = (__u16)Cop;
}

static void Cop_to_Aop_24(void)
{
     int   w = Dlength;
     __u8 *D = (__u8*)Aop;

     while (w) {
          D[0] = color.b;
          D[1] = color.g;
          D[2] = color.r;

          D += 3;
          --w;
     }
}

static void Cop_to_Aop_32(void)
{
     int    w = Dlength;
     __u32 *D = (__u32*)Aop;

     while (w--)
          *D++ = (__u32)Cop;
}

static GFunc Cop_to_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     Cop_to_Aop_16,
     Cop_to_Aop_16,
     Cop_to_Aop_24,
     Cop_to_Aop_32,
     Cop_to_Aop_32,
     Cop_to_Aop_8,
     Cop_to_Aop_32,
#ifdef SUPPORT_RGB332
     Cop_to_Aop_8,
#else     
     NULL,
#endif
     Cop_to_Aop_32,
     Cop_to_Aop_8,
     Cop_to_Aop_8,
     Cop_to_Aop_8
};

/********************************* Cop_toK_Aop_PFI ****************************/

static void Cop_toK_Aop_8(void)
{
     int   w = Dlength;
     __u8 *D = (__u8*)Aop;

     while (w--) {
          if ((__u8)Dkey == *D)
               *D = (__u8)Cop;

          D++;
     }
}

static void Cop_toK_Aop_16(void)
{
     int    w = Dlength;
     __u16 *D = (__u16*)Aop;

     while (w--) {
          if ((__u16)Dkey == *D)
               *D = (__u16)Cop;

          D++;
     }
}

static void Cop_toK_Aop_24(void)
{
     ONCE("Cop_toK_Aop_24() unimplemented");
}

static void Cop_toK_Aop_32(void)
{
     int    w = Dlength;
     __u32 *D = (__u32*)Aop;

     while (w--) {
          if ((__u32)Dkey == *D)
               *D = (__u32)Cop;

          D++;
     }
}




static GFunc Cop_toK_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     Cop_toK_Aop_16,
     Cop_toK_Aop_16,
     Cop_toK_Aop_24,
     Cop_toK_Aop_32,
     Cop_toK_Aop_32,
     Cop_toK_Aop_8,
     NULL,
     Cop_toK_Aop_8,
     NULL,
     NULL,
     NULL,
     Cop_toK_Aop_8
};




/********************************* Bop_PFI_to_Aop_PFI *************************/

static void Bop_8_to_Aop(void)
{
     dfb_memmove( Aop, Bop, Dlength );
}

static void Bop_16_to_Aop(void)
{
     dfb_memmove( Aop, Bop, Dlength*2 );
}

static void Bop_24_to_Aop(void)
{
     dfb_memmove( Aop, Bop, Dlength*3 );
}

static void Bop_32_to_Aop(void)
{
     dfb_memmove( Aop, Bop, Dlength*4 );
}

static GFunc Bop_PFI_to_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     Bop_16_to_Aop,      /* DSPF_ARGB1555 */
     Bop_16_to_Aop,      /* DSPF_RGB16 */
     Bop_24_to_Aop,      /* DSPF_RGB24 */
     Bop_32_to_Aop,      /* DSPF_RGB32 */
     Bop_32_to_Aop,      /* DSPF_ARGB */
     Bop_8_to_Aop,       /* DSPF_A8 */
     Bop_16_to_Aop,      /* DSPF_YUY2 */
#ifdef SUPPORT_RGB332
     Bop_8_to_Aop,       /* DSPF_RGB332 */
#else
     NULL,
#endif
     Bop_16_to_Aop,      /* DSPF_UYVY */
     Bop_8_to_Aop,       /* DSPF_I420 */
     Bop_8_to_Aop,       /* DSPF_YV12 */
     Bop_8_to_Aop        /* DSPF_LUT8 */
};

/********************************* Bop_PFI_Kto_Aop_PFI ************************/

static void Bop_rgb15_Kto_Aop(void)
{
     int    w, l = Dlength;
     __u32 *D = (__u32*)Aop;
     __u32 *S = (__u32*)Bop;

     __u32 DSkey = (Skey << 16) | (Skey & 0x0000FFFF);

     if (((long)D)&2) {         /* align */
          __u16 *tmp = Aop;
          --l;
          if ((*((__u16*)S) & 0x7FFF) != (__u16)Skey)
           *tmp = *((__u16*)S);

          D = (__u32*)((__u16*)D+1);
          S = (__u32*)((__u16*)S+1);
     }

     w = (l >> 1);
     while (w) {
          __u32 dpixel = *S;
          __u16 *tmp = (__u16*)D;

          if ((dpixel & 0x7FFF7FFF) != DSkey) {
               if ((dpixel & 0x7FFF0000) != (DSkey & 0x7FFF0000)) {
                    if ((dpixel & 0x00007FFF) != (DSkey & 0x00007FFF)) {
                         *D = dpixel;
                    }
                    else {
#ifdef MPE_BIG_ENDIAN
                         tmp[0] = (__u16)(dpixel >> 16);
#else
                         tmp[1] = (__u16)(dpixel >> 16);
#endif
                    }
               }
               else {
#ifdef MPE_BIG_ENDIAN
                    tmp[1] = (__u16)dpixel;
#else
                    tmp[0] = (__u16)dpixel;
#endif
               }
          }
          ++S;
          ++D;
          --w;
     }

     if (l & 1) {                 /* do the last potential pixel */
          if ((*((__u16*)S) & 0x7FFF) != (__u16)Skey)
               *((__u16*)D) = *((__u16*)S);
     }
}

static void Bop_rgb16_Kto_Aop(void)
{
     int    w, l = Dlength;
     __u32 *D = (__u32*)Aop;
     __u32 *S = (__u32*)Bop;

     __u32 DSkey = (Skey << 16) | (Skey & 0x0000FFFF);

     if (((long)D)&2) {         /* align */
          __u16 *tmp = Aop;
          --l;
          if (*((__u16*)S) != (__u16)Skey)
           *tmp = *((__u16*)S);

          D = (__u32*)((__u16*)D+1);
          S = (__u32*)((__u16*)S+1);
     }

     w = (l >> 1);
     while (w) {
          __u32 dpixel = *S;
          __u16 *tmp = (__u16*)D;

          if (dpixel != DSkey) {
               if ((dpixel & 0xFFFF0000) != (DSkey & 0xFFFF0000)) {
                    if ((dpixel & 0x0000FFFF) != (DSkey & 0x0000FFFF)) {
                         *D = dpixel;
                    }
                    else {
#ifdef MPE_BIG_ENDIAN
                         tmp[0] = (__u16)(dpixel >> 16);
#else
                         tmp[1] = (__u16)(dpixel >> 16);
#endif
                    }
               }
               else {
#ifdef MPE_BIG_ENDIAN
                    tmp[1] = (__u16)dpixel;
#else
                    tmp[0] = (__u16)dpixel;
#endif
               }
          }
          ++S;
          ++D;
          --w;
     }

     if (l & 1) {                 /* do the last potential pixel */
          if (*((__u16*)S) != (__u16)Skey)
               *((__u16*)D) = *((__u16*)S);
     }
}

static void Bop_rgb24_Kto_Aop(void)
{
     int    w = Dlength;
     __u8 *D = (__u8*)Aop;
     __u8 *S = (__u8*)Bop;

     if (Ostep < 0) {
          D+= (Dlength - 1) * 3;
          S+= (Dlength - 1) * 3;
     }

     while (w--) {
          __u8 b = *S;
          __u8 g = *(S+1);
          __u8 r = *(S+2);

          if (Skey != (__u32)(r<<16 | g<<8 | b ))
          {
               *D     = b;
               *(D+1) = g;
               *(D+2) = r;
          }

          S+=Ostep * 3;
          D+=Ostep * 3 ;
     }
}

static void Bop_rgb32_Kto_Aop(void)
{
     int    w = Dlength;
     __u32 *D = (__u32*)Aop;
     __u32 *S = (__u32*)Bop;

     if (Ostep < 0) {
          D+= Dlength - 1;
          S+= Dlength - 1;
     }

     while (w--) {
          __u32 spixel = *S & 0x00FFFFFF;

          if (spixel != Skey)
               *D = spixel;

          S+=Ostep;
          D+=Ostep;
     }
}

static void Bop_argb_Kto_Aop(void)
{
     int    w = Dlength;
     __u32 *D = (__u32*)Aop;
     __u32 *S = (__u32*)Bop;

     if (Ostep < 0) {
          D+= Dlength - 1;
          S+= Dlength - 1;
     }

     while (w--) {
          __u32 spixel = *S;

          if (spixel != Skey)
               *D = spixel;

          S+=Ostep;
          D+=Ostep;
     }
}

static void Bop_a8_Kto_Aop(void)
{
     /* no color to key */
     dfb_memmove( Aop, Bop, Dlength );
}

static void Bop_8_Kto_Aop(void)
{
     int    w = Dlength;
     __u8 *D = (__u8*)Aop;
     __u8 *S = (__u8*)Bop;

     if (Ostep < 0) {
          D+= Dlength - 1;
          S+= Dlength - 1;
     }

     while (w--) {
          __u8 spixel = *S;

          if (spixel != (__u8)Skey)
               *D = spixel;

          S+=Ostep;
          D+=Ostep;
     }
}

static GFunc Bop_PFI_Kto_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     Bop_rgb15_Kto_Aop,
     Bop_rgb16_Kto_Aop,
     Bop_rgb24_Kto_Aop,
     Bop_rgb32_Kto_Aop,
     Bop_argb_Kto_Aop,
     Bop_a8_Kto_Aop,
     NULL,
     Bop_8_Kto_Aop,
     NULL,
     NULL,
     NULL,
     Bop_8_Kto_Aop
};

/********************************* Bop_PFI_Sto_Aop ****************************/

static void Bop_16_Sto_Aop(void)
{
     int    w  = Dlength;
     int    w2;
     int    i = 0;
     __u32 *D = (__u32*)Aop;
     __u16 *S = (__u16*)Bop;

     if (((long)D)&2) {
        *((__u16*)D++) = *S;
        i += SperD;
        w--;
     }

     w2 = (w >> 1);
     while (w2--) {
          int SperD2 = (SperD << 1);
#ifdef MPE_BIG_ENDIAN
          *D++ =  S[i>>16] << 16 | S[(i+SperD)>>16];
#else
          *D++ = (S[(i+SperD)>>16] << 16) | S[i>>16];
#endif
          i += SperD2;
     }
     if (w&1) {
          *((__u16*)D) = S[i>>16];
     }     
}

static void Bop_24_Sto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u8 *D = (__u8*)Aop;
     __u8 *S = (__u8*)Bop;

     while (w--) {
          int pixelstart = (i>>16)*3;

          *D++ = S[pixelstart+0];
          *D++ = S[pixelstart+1];
          *D++ = S[pixelstart+2];

          i += SperD;
     }
}

static void Bop_32_Sto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u32 *D = (__u32*)Aop;
     __u32 *S = (__u32*)Bop;

     while (w--) {
          *D++ = S[i>>16];

          i += SperD;
     }
}

static void Bop_8_Sto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u8 *D = (__u8*)Aop;
     __u8 *S = (__u8*)Bop;

     while (w--) {
          *D++ = S[i>>16];

          i += SperD;
     }
}

static GFunc Bop_PFI_Sto_Aop[DFB_NUM_PIXELFORMATS] = {
     Bop_16_Sto_Aop,
     Bop_16_Sto_Aop,
     Bop_24_Sto_Aop,
     Bop_32_Sto_Aop,
     Bop_32_Sto_Aop,
     Bop_8_Sto_Aop,
     NULL,
     Bop_8_Sto_Aop,
     NULL,
     NULL,
     NULL,
     Bop_8_Sto_Aop
};

/********************************* Bop_PFI_SKto_Aop ***************************/

static void Bop_rgb15_SKto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u16 *D = (__u16*)Aop;
     __u16 *S = (__u16*)Bop;

     while (w--) {
          __u16 s = S[i>>16] & 0x7FFF;

          if (s != Skey)
               *D = s;

          D++;
          i += SperD;
     }
}

static void Bop_rgb16_SKto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u16 *D = (__u16*)Aop;
     __u16 *S = (__u16*)Bop;

     while (w--) {
          __u16 s = S[i>>16];

          if (s != Skey)
               *D = s;

          D++;
          i += SperD;
     }
}

static void Bop_rgb24_SKto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u8 *D = (__u8*)Aop;
     __u8 *S = (__u8*)Bop;

     while (w--) {
          int pixelstart = (i>>16)*3;

          __u8 b = S[pixelstart+0];
          __u8 g = S[pixelstart+1];
          __u8 r = S[pixelstart+2];

          if (Skey != (__u32)(r<<16 | g<<8 | b )) {
               *D     = b;
               *(D+1) = g;
               *(D+2) = r;
          }

          D += 3;
          i += SperD;
     }
}

static void Bop_rgb32_SKto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u32 *D = (__u32*)Aop;
     __u32 *S = (__u32*)Bop;

     while (w--) {
          __u32 s = S[i>>16] & 0x00FFFFFF;

          if (s != Skey)
               *D = s;

          D++;
          i += SperD;
     }
}

static void Bop_argb_SKto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u32 *D = (__u32*)Aop;
     __u32 *S = (__u32*)Bop;

     while (w--) {
          __u32 s = S[i>>16];

          if (s != Skey)
               *D = s;

          D++;
          i += SperD;
     }
}

static void Bop_a8_SKto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u8 *D = (__u8*)Aop;
     __u8 *S = (__u8*)Bop;

     /* no color to key */
     while (w--) {
          *D++ = S[i>>16];

          i += SperD;
     }
}

static void Bop_8_SKto_Aop(void)
{
     int    w = Dlength;
     int    i = 0;
     __u8 *D = (__u8*)Aop;
     __u8 *S = (__u8*)Bop;

     while (w--) {
          __u8 s = S[i>>16];

          if (s != Skey)
               *D = s;

          D++;
          i += SperD;
     }
}

static GFunc Bop_PFI_SKto_Aop[DFB_NUM_PIXELFORMATS] = {
     Bop_rgb15_SKto_Aop,
     Bop_rgb16_SKto_Aop,
     Bop_rgb24_SKto_Aop,
     Bop_rgb32_SKto_Aop,
     Bop_argb_SKto_Aop,
     Bop_a8_SKto_Aop,
     NULL,
     Bop_8_SKto_Aop,
     NULL,
     NULL,
     NULL,
     Bop_8_SKto_Aop
};

/********************************* Sop_PFI_Sto_Dacc ***************************/

#ifdef USE_MMX
void Sop_argb_Sto_Dacc_MMX(void);
#endif

static void Sop_argb1555_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     while (w--) {
          __u16 s = S[i>>16];

          D->a = (s & 0x8000) ? 0xff : 0;
          D->r = (s & 0x7C00) >> 7;
          D->g = (s & 0x03E0) >> 2;
          D->b = (s & 0x001F) << 3;

          i += SperD;

          D++;
     }
}

static void Sop_rgb16_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     while (w--) {
          __u16 s = S[i>>16];

          D->a = 0xFF;
          D->r = (s & 0xF800) >> 8;
          D->g = (s & 0x07E0) >> 3;
          D->b = (s & 0x001F) << 3;

          i += SperD;

          D++;
     }
}

static void Sop_rgb24_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          int pixelstart = (i>>16)*3;

          D->a = 0xFF;
          D->r = S[pixelstart+2];
          D->g = S[pixelstart+1];
          D->b = S[pixelstart+0];

          i += SperD;

          D++;
     }
}

static void Sop_rgb32_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = S[i>>16];

          D->a = 0xFF;
          D->r = (__u16)((s & 0x00FF0000) >> 16);
          D->g = (__u16)((s & 0x0000FF00) >>  8);
          D->b = (__u16)((s & 0x000000FF));

          i += SperD;

          D++;
     }
}

static void Sop_argb_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = S[i>>16];

          D->a = (__u16)((s & 0xFF000000) >> 24);
          D->r = (__u16)((s & 0x00FF0000) >> 16);
          D->g = (__u16)((s & 0x0000FF00) >>  8);
          D->b = (__u16)((s & 0x000000FF));

          i += SperD;

          D++;
     }
}

static void Sop_a8_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          __u8 s = S[i>>16];

          D->a = s;
          D->r = 0xFF;
          D->g = 0xFF;
          D->b = 0xFF;

          i += SperD;

          D++;
     }
}

#ifdef SUPPORT_RGB332
static void Sop_rgb332_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          __u8 s = S[i>>16];

          D->a = 0xFF;
          D->r = lookup3to8[s >> 5];
          D->g = lookup3to8[(s & 0x1C) >> 2];
          D->b = lookup2to8[s & 0x03];

          i += SperD;

          D++;
     }
}
#endif

static void Sop_lut8_Sto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     DFBColor *entries = Slut->entries;

     while (w--) {
          __u8 s = S[i>>16];

          D->a = entries[s].a;
          D->r = entries[s].r;
          D->g = entries[s].g;
          D->b = entries[s].b;

          i += SperD;

          D++;
     }
}

static GFunc Sop_PFI_Sto_Dacc[DFB_NUM_PIXELFORMATS] = {
     Sop_argb1555_Sto_Dacc,
     Sop_rgb16_Sto_Dacc,
     Sop_rgb24_Sto_Dacc,
     Sop_rgb32_Sto_Dacc,
     Sop_argb_Sto_Dacc,
     Sop_a8_Sto_Dacc,
     NULL,
#ifdef SUPPORT_RGB332
     Sop_rgb332_Sto_Dacc,
#else
     NULL,
#endif
     NULL,
     NULL,
     NULL,
     Sop_lut8_Sto_Dacc
};

/********************************* Sop_PFI_SKto_Dacc **************************/

static void Sop_argb1555_SKto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     while (w--) {
          __u16 s = S[i>>16];

          if ((__u32)(s & 0x7FFF) != Skey) {
               D->a = (s & 0x8000) ? 0xff : 0;
               D->r = (s & 0x7C00) >> 7;
               D->g = (s & 0x03E0) >> 2;
               D->b = (s & 0x001F) << 3;
          }
          else
               D->a = 0xF000;

          i += SperD;

          D++;
     }
}

static void Sop_rgb16_SKto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     while (w--) {
          __u16 s = S[i>>16];

          if (s != Skey) {
               D->a = 0xFF;
               D->r = (s & 0xF800) >> 8;
               D->g = (s & 0x07E0) >> 3;
               D->b = (s & 0x001F) << 3;
          }
          else
               D->a = 0xF000;

          i += SperD;

          D++;
     }
}

static void Sop_rgb24_SKto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          int pixelstart = (i>>16)*3;

          __u8 b = S[pixelstart+0];
          __u8 g = S[pixelstart+1];
          __u8 r = S[pixelstart+2];

          if (Skey != (__u32)(r<<16 | g<<8 | b )) {
               D->a = 0xFF;
               D->r = r;
               D->g = g;
               D->b = b;
          }
          else
               D->a = 0xFF00;

          i += SperD;

          D++;
     }
}

static void Sop_rgb32_SKto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = S[i>>16] & 0x00FFFFFF;

          if (s != Skey) {
               D->a = 0xFF;
               D->r = (__u16)((s & 0x00FF0000) >> 16);
               D->g = (__u16)((s & 0x0000FF00) >>  8);
               D->b = (__u16)((s & 0x000000FF));
          }
          else
               D->a = 0xF000;

          i += SperD;

          D++;
     }
}


static void Sop_argb_SKto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = S[i>>16];

          if (s != Skey) {
               D->a = (__u16)((s & 0xFF000000) >> 24);
               D->r = (__u16)((s & 0x00FF0000) >> 16);
               D->g = (__u16)((s & 0x0000FF00) >>  8);
               D->b = (__u16)((s & 0x000000FF));
          }
          else
               D->a = 0xF000;

          i += SperD;

          D++;
     }
}

static void Sop_a8_SKto_Dacc(void)
{
     int    w = Dlength;
     int    i = 0;

     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     /* no color to key */
     while (w--) {
          __u8 s = S[i>>16];

          D->a = s;
          D->r = 0xFF;
          D->g = 0xFF;
          D->b = 0xFF;

          i += SperD;

          D++;
     }
}

static GFunc Sop_PFI_SKto_Dacc[DFB_NUM_PIXELFORMATS] = {
     Sop_argb1555_SKto_Dacc,
     Sop_rgb16_SKto_Dacc,
     Sop_rgb24_SKto_Dacc,
     Sop_rgb32_SKto_Dacc,
     Sop_argb_SKto_Dacc,
     Sop_a8_SKto_Dacc,
     NULL,
     NULL,     /* FIXME: RGB332 */
     NULL,
     NULL,
     NULL,
     NULL
};

/********************************* Sop_PFI_to_Dacc ****************************/

#ifdef USE_MMX
void Sop_rgb16_to_Dacc_MMX(void);
void Sop_rgb32_to_Dacc_MMX(void);
void Sop_argb_to_Dacc_MMX(void);
#endif


static void Sop_argb1555_to_Dacc(void)
{
     int       l, w = Dlength;
     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     if (((long)S)&2) {
          __u16 spixel = *S;

          D->a = (spixel & 0x8000) ? 0xff : 0;
          D->r = (spixel & 0x7C00) >> 7;
          D->g = (spixel & 0x03E0) >> 2;
          D->b = (spixel & 0x001F) << 3;

          ++S;
          ++D;
          --w;
     }

     l = w >> 1;
     while (l) {
          __u32 spixel2 = *((__u32*)S);

#ifdef MPE_BIG_ENDIAN
          D[0].a = 0xFF;
          D[0].r = (__u16)((spixel2 & 0x7C000000) >> 23);
          D[0].g = (__u16)((spixel2 & 0x03E00000) >> 18);
          D[0].b = (__u16)((spixel2 & 0x001F0000) >> 13);

          D[1].a = 0xFF;
          D[1].r = (__u16)((spixel2 & 0x7C00) >> 7);
          D[1].g = (__u16)((spixel2 & 0x03E0) >> 2);
          D[1].b = (__u16)((spixel2 & 0x001F) << 3);
#else
          D[0].a = 0xFF;
          D[0].r = (__u16)((spixel2 & 0x7C00) >> 7);
          D[0].g = (__u16)((spixel2 & 0x03E0) >> 2);
          D[0].b = (__u16)((spixel2 & 0x001F) << 3);

          D[1].a = 0xFF;
          D[1].r = (__u16)((spixel2 & 0x7C000000) >> 23);
          D[1].g = (__u16)((spixel2 & 0x03E00000) >> 18);
          D[1].b = (__u16)((spixel2 & 0x001F0000) >> 13);
#endif

          S += 2;
          D += 2;

          --l;
     }

     if (w&1) {
          __u16 spixel = *S;

          D->a = 0xFF;
          D->r = (spixel & 0x7C00) >> 7;
          D->g = (spixel & 0x03E0) >> 2;
          D->b = (spixel & 0x001F) << 3;
     }
}

static void Sop_rgb16_to_Dacc(void)
{
     int       l, w = Dlength;
     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     if (((long)S)&2) {
          __u16 spixel = *S;

          D->a = 0xFF;
          D->r = (spixel & 0xF800) >> 8;
          D->g = (spixel & 0x07E0) >> 3;
          D->b = (spixel & 0x001F) << 3;

          ++S;
          ++D;
          --w;
     }

     l = w >> 1;
     while (l) {
          __u32 spixel2 = *((__u32*)S);

#ifdef MPE_BIG_ENDIAN
          D[0].a = 0xFF;
          D[0].r = (__u16)((spixel2 & 0xF8000000) >> 24);
          D[0].g = (__u16)((spixel2 & 0x07E00000) >> 19);
          D[0].b = (__u16)((spixel2 & 0x001F0000) >> 13);

          D[1].a = 0xFF;
          D[1].r = (__u16)((spixel2 & 0xF800) >> 8);
          D[1].g = (__u16)((spixel2 & 0x07E0) >> 3);
          D[1].b = (__u16)((spixel2 & 0x001F) << 3);
#else
          D[0].a = 0xFF;
          D[0].r = (__u16)((spixel2 & 0xF800) >> 8);
          D[0].g = (__u16)((spixel2 & 0x07E0) >> 3);
          D[0].b = (__u16)((spixel2 & 0x001F) << 3);

          D[1].a = 0xFF;
          D[1].r = (__u16)((spixel2 & 0xF8000000) >> 24);
          D[1].g = (__u16)((spixel2 & 0x07E00000) >> 19);
          D[1].b = (__u16)((spixel2 & 0x001F0000) >> 13);
#endif

          S += 2;
          D += 2;

          --l;
     }

     if (w&1) {
          __u16 spixel = *S;

          D->a = 0xFF;
          D->r = (spixel & 0xF800) >> 8;
          D->g = (spixel & 0x07E0) >> 3;
          D->b = (spixel & 0x001F) << 3;
     }
}

static void Sop_rgb24_to_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          D->a = 0xFF;
          D->b = *S++;
          D->g = *S++;
          D->r = *S++;

          D++;
     }
}

static void Sop_a8_to_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          D->a = *S++;
          D->r = 0xFF;
          D->g = 0xFF;
          D->b = 0xFF;

          D++;
     }
}

static void Sop_rgb32_to_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = *S++;

          D->a = 0xFF;
          D->r = (__u16)((s & 0xFF0000) >> 16);
          D->g = (__u16)((s & 0x00FF00) >>  8);
          D->b = (__u16)((s & 0x0000FF));

          D++;
     }
}

static void Sop_argb_to_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = *S++;
	  //__u32 s = *S--;

          D->a = (__u16)((s & 0xFF000000) >> 24);
          D->r = (__u16)((s & 0x00FF0000) >> 16);
          D->g = (__u16)((s & 0x0000FF00) >>  8);
          D->b = (__u16)((s & 0x000000FF));

          D++;
     }
}

#ifdef SUPPORT_RGB332
static void Sop_rgb332_to_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          __u8 s = *S++;

          D->a = 0xFF;
          D->r = lookup3to8[s >> 5];
          D->g = lookup3to8[(s & 0x1C) >> 2];
          D->b = lookup2to8[s & 0x03];

          D++;
     }
}
#endif

#define LOOKUP_COLOR(D,S)     \
     D.a = entries[S].a;      \
     D.r = entries[S].r;      \
     D.g = entries[S].g;      \
     D.b = entries[S].b;

static void Sop_lut8_to_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     DFBColor *entries = Slut->entries;

     while (w) {
          int l = w & 7;

          switch (l) {
               default:
                    l = 8;
                    LOOKUP_COLOR( D[7], S[7] );
               case 7:
                    LOOKUP_COLOR( D[6], S[6] );
               case 6:
                    LOOKUP_COLOR( D[5], S[5] );
               case 5:
                    LOOKUP_COLOR( D[4], S[4] );
               case 4:
                    LOOKUP_COLOR( D[3], S[3] );
               case 3:
                    LOOKUP_COLOR( D[2], S[2] );
               case 2:
                    LOOKUP_COLOR( D[1], S[1] );
               case 1:
                    LOOKUP_COLOR( D[0], S[0] );
          }

          D += l;
          S += l;
          w -= l;
     }
}


static GFunc Sop_PFI_to_Dacc[DFB_NUM_PIXELFORMATS] = {
     Sop_argb1555_to_Dacc,
     Sop_rgb16_to_Dacc,
     Sop_rgb24_to_Dacc,
     Sop_rgb32_to_Dacc,
     Sop_argb_to_Dacc,
     Sop_a8_to_Dacc,
     NULL,
#ifdef SUPPORT_RGB332
     Sop_rgb332_to_Dacc,
#else
     NULL,
#endif
     NULL,
     NULL,
     NULL,
     Sop_lut8_to_Dacc
};

/********************************* Sop_PFI_Kto_Dacc ***************************/

static void Sop_argb1555_Kto_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     while (w--) {
          __u16 s = *S++;

          if ((__u32)(s & 0x7FFF) != Skey) {
               D->a = (s & 0x8000) ? 0xff : 0;
               D->r = (s & 0x7C00) >> 7;
               D->g = (s & 0x03E0) >> 2;
               D->b = (s & 0x001F) << 3;
          }
          else
               D->a = 0xF000;

          D++;
     }
}

static void Sop_rgb16_Kto_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u16       *S = (__u16*)Sop;

     while (w--) {
          __u16 s = *S++;

          if (s != (__u16)Skey) {
               D->a = 0xFF;
               D->r = (s & 0xF800) >> 8;
               D->g = (s & 0x07E0) >> 3;
               D->b = (s & 0x001F) << 3;
          }
          else
               D->a = 0xF000;

          D++;
     }
}

static void Sop_rgb24_Kto_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          __u8 b = *S++;
          __u8 g = *S++;
          __u8 r = *S++;

          if (Skey != (__u32)(r<<16 | g<<8 | b ))
          {
               D->a = 0xFF;
               D->r = r;
               D->g = g;
               D->b = b;
          }
          else
               D->a = 0xF000;

          D++;
     }
}

static void Sop_rgb32_Kto_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = *S++ & 0x00FFFFFF;

          if (s != Skey) {
               D->a = 0xFF;
               D->r = (__u16)(s >> 16);
               D->g = (__u16)((s & 0x00FF00) >>  8);
               D->b = (__u16)((s & 0x0000FF));
          }
          else
               D->a = 0xF000;

          D++;
     }
}

static void Sop_argb_Kto_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u32       *S = (__u32*)Sop;

     while (w--) {
          __u32 s = *S++;

          if ((s & 0xFFFFFF) != Skey) {
               D->a = (__u16)(s >> 24);
               D->r = (__u16)((s & 0x00FF0000) >> 16);
               D->g = (__u16)((s & 0x0000FF00) >>  8);
               D->b = (__u16)((s & 0x000000FF));
          }
          else
               D->a = 0xF000;

          D++;
     }
}

static void Sop_a8_Kto_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     /* no color to key */
     while (w--) {
          D->a = *S++;
          D->r = 0xFF;
          D->g = 0xFF;
          D->b = 0xFF;

          D++;
     }
}

#ifdef SUPPORT_RGB332
static void Sop_rgb332_Kto_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;
     __u8        *S = (__u8*)Sop;

     while (w--) {
          __u8 s = *S++;

          if (s != (__u8)Skey) {
               D->a = 0xFF;
               D->r = lookup3to8[s >> 5];
               D->g = lookup3to8[(s & 0x1C) >> 2];
               D->b = lookup2to8[s & 0x03];
          }
          else
               D->a = 0xF000;

          D++;
     }
}

#endif

static GFunc Sop_PFI_Kto_Dacc[DFB_NUM_PIXELFORMATS] = {
     Sop_argb1555_Kto_Dacc,
     Sop_rgb16_Kto_Dacc,
     Sop_rgb24_Kto_Dacc,
     Sop_rgb32_Kto_Dacc,
     Sop_argb_Kto_Dacc,
     Sop_a8_Kto_Dacc,
     NULL,
#ifdef SUPPORT_RGB332
     Sop_rgb332_Kto_Dacc,
#else
     NULL,
#endif
     NULL,
     NULL,
     NULL,
     NULL
};



/********************************* Sacc_to_xor_Aop_PFI ****************************/
#ifdef OPTIMIZATION
static void Sacc_to_xor_Aop_argb(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u32       *D = (__u32*)Aop;

     while (w--) {
          if (!(S->a & 0xF000)) {
               *D ^= PIXEL_ARGB((S->a & 0xFF00) ? 0xFF : S->a,
                                (S->r & 0xFF00) ? 0xFF : S->r^color.r,
                                (S->g & 0xFF00) ? 0xFF : S->g^color.g,
                                (S->b & 0xFF00) ? 0xFF : S->b^color.b );
          }

          D++;
          S++;
     }
}



static void Sacc_to_xor_Aop_rgb16(void)
{
     int          l;
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u16       *D = (__u16*)Aop;

     if ((long) D & 2) {
          if (!(S->a & 0xF000)) {
               *D ^= PIXEL_RGB16( (S->r & 0xFF00) ? 0xFF : S->r ^ color.r,
                                 (S->g & 0xFF00) ? 0xFF : S->g ^ color.g,
                                 (S->b & 0xFF00) ? 0xFF : S->b ^ color.b );
          }
          
          ++S;
          ++D;
          --w;
     }

     l = w >> 1;
     while (l) {
          __u32 *D2 = (__u32*) D;

          if (!(S[0].a & 0xF000) && !(S[1].a & 0xF000)) {
#ifdef MPE_BIG_ENDIAN
               *D2 ^= PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r^color.r,
                                  (S[1].g & 0xFF00) ? 0xFF : S[1].g^color.g,
                                  (S[1].b & 0xFF00) ? 0xFF : S[1].b^color.b ) |
                     PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r^color.r,
                                  (S[0].g & 0xFF00) ? 0xFF : S[0].g^color.g,
                                  (S[0].b & 0xFF00) ? 0xFF : S[0].b^color.b ) << 16;
#else
               *D2 ^= PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r ^ color.r,
                                  (S[0].g & 0xFF00) ? 0xFF  : S[0].g ^ color.b,
                                  (S[0].b & 0xFF00) ? 0xFF  : S[0].b ^ color.b) |
                     PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF  : S[1].r ^ color.r,
                                  (S[1].g & 0xFF00) ? 0xFF  : S[1].g ^ color.g,
                                  (S[1].b & 0xFF00) ? 0xFF  : S[1].b ^ color.b) << 16;
#endif
          }
          else {
               if (!(S[0].a & 0xF000)) {
                    D[0] ^= PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r^color.r,
                                        (S[0].g & 0xFF00) ? 0xFF : S[0].g^color.g,
                                        (S[0].b & 0xFF00) ? 0xFF : S[0].b^color.b );
               } else
               if (!(S[1].a & 0xF000)) {
                    D[1] ^= PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r^color.r,
                                        (S[1].g & 0xFF00) ? 0xFF : S[1].g^color.g,
                                        (S[1].b & 0xFF00) ? 0xFF : S[1].b^color.b );
               }
          }
          
          S += 2;
          D += 2;

          --l;
     }
     
     if (w & 1) {
          if (!(S->a & 0xF000)) {
               *D ^= PIXEL_RGB16( (S->r & 0xFF00) ? 0xFF : S->r^color.r,
                                 (S->g & 0xFF00) ? 0xFF : S->g^color.g,
                                 (S->b & 0xFF00) ? 0xFF : S->b^color.b );
          }
     }
}


GFunc Sacc_to_xor_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     NULL, /*Sacc_to_xor_Aop_argb1555,*/
     Sacc_to_xor_Aop_rgb16,
     NULL, /*Sacc_to_xor_Aop_rgb24,*/
     NULL, /*Sacc_to_xor_Aop_rgb32,*/
     Sacc_to_xor_Aop_argb, /*Sacc_to_xor_Aop_argb, */
     NULL,
     NULL,
#ifdef SUPPORT_RGB332
     NULL,
#else
     NULL,
#endif
     NULL,
     NULL,
     NULL,
     NULL /*Sacc_to_xor_Aop_lut8*/
};

#endif /* OPTIMIZATION */




/********************************* Sacc_to_Aop_PFI ****************************/

#ifdef USE_MMX
void Sacc_to_Aop_rgb16_MMX(void);
void Sacc_to_Aop_rgb32_MMX(void);
#endif

// DONM - 12/2/2005
// Also take care of horizontal reflective processing where a negative width
// is passed in (in a stretchblit for example).
static void Sacc_to_Aop_argb1555(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u16       *D = (__u16*)Aop;

     // Handle reflective processing case.
     if (flagwneg)
	     S += w;	// Process from right to left.

     while (w--) {
          if (!(S->a & 0xF000)) {
               *D = PIXEL_ARGB1555( (S->a & 0xFF00) ? 0xFF : S->a,
                                    (S->r & 0xFF00) ? 0xFF : S->r,
                                    (S->g & 0xFF00) ? 0xFF : S->g,
                                    (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
		  // Handle reflective processing case
		  if (flagwneg)
		      S--;			// process from right to left
		  else
			  S++;			// default is to process from left to right
     }
}

static void Sacc_to_Aop_rgb16(void)
{
     int          l;
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u16       *D = (__u16*)Aop;

	 // Handle reflective processing
	 if (flagwneg)
	     S += w;		// Process from right to left

     if ((long) D & 2) {
          if (!(S->a & 0xF000)) {
               *D = PIXEL_RGB16( (S->r & 0xFF00) ? 0xFF : S->r,
                                 (S->g & 0xFF00) ? 0xFF : S->g,
                                 (S->b & 0xFF00) ? 0xFF : S->b );
          }
          
		  // Handle reflective processing for first pixel if odd numbered
		  if (flagwneg)
		      --S;			// Process from right to left
		  else
			  ++S;			// Default is from left to right
          ++D;
          --w;
     }

     l = w >> 1;
     while (l) {
          __u32 *D2 = (__u32*) D;

          if (!(S[0].a & 0xF000) && !(S[1].a & 0xF000)) {
#ifdef MPE_BIG_ENDIAN
			   if (flagwneg)	// process reflective situation
			   {
                   *D2 = PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r,
                                      (S[0].g & 0xFF00) ? 0xFF : S[0].g,
                                      (S[0].b & 0xFF00) ? 0xFF : S[0].b ) |
                         PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r,
                                      (S[1].g & 0xFF00) ? 0xFF : S[1].g,
                                      (S[1].b & 0xFF00) ? 0xFF : S[1].b ) << 16;
			   }
			   else		// Default
			   {
                   *D2 = PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r,
                                      (S[1].g & 0xFF00) ? 0xFF : S[1].g,
                                      (S[1].b & 0xFF00) ? 0xFF : S[1].b ) |
                         PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r,
                                      (S[0].g & 0xFF00) ? 0xFF : S[0].g,
                                      (S[0].b & 0xFF00) ? 0xFF : S[0].b ) << 16;
			   }
#else
			   if (flagwneg)	// process reflective situation
			   {
                   *D2 = PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r,
                                      (S[0].g & 0xFF00) ? 0xFF : S[0].g,
                                      (S[0].b & 0xFF00) ? 0xFF : S[0].b ) |
                         PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r,
                                      (S[1].g & 0xFF00) ? 0xFF : S[1].g,
                                      (S[1].b & 0xFF00) ? 0xFF : S[1].b ) << 16;
			   }
			   else			// Default
			   {
                   *D2 = PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r,
                                      (S[1].g & 0xFF00) ? 0xFF : S[1].g,
                                      (S[1].b & 0xFF00) ? 0xFF : S[1].b ) |
                         PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r,
                                      (S[0].g & 0xFF00) ? 0xFF : S[0].g,
                                      (S[0].b & 0xFF00) ? 0xFF : S[0].b ) << 16;
			   }
#endif
          }
          else {
               if (!(S[0].a & 0xF000)) {
			       if (flagwneg)	// Process reflective situation
				   {
                       D[0] = PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r,
                                           (S[1].g & 0xFF00) ? 0xFF : S[1].g,
                                           (S[1].b & 0xFF00) ? 0xFF : S[1].b );
				   }
				   else		// Default
				   {
                       D[0] = PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r,
                                           (S[0].g & 0xFF00) ? 0xFF : S[0].g,
                                           (S[0].b & 0xFF00) ? 0xFF : S[0].b );
				   }
               } else
               if (!(S[1].a & 0xF000)) {
				   if (flagwneg)	// Process reflective situation
				   {
                       D[1] = PIXEL_RGB16( (S[0].r & 0xFF00) ? 0xFF : S[0].r,
                                           (S[0].g & 0xFF00) ? 0xFF : S[0].g,
                                           (S[0].b & 0xFF00) ? 0xFF : S[0].b );
				   }
				   else		// Default
				   {
                       D[1] = PIXEL_RGB16( (S[1].r & 0xFF00) ? 0xFF : S[1].r,
                                           (S[1].g & 0xFF00) ? 0xFF : S[1].g,
                                           (S[1].b & 0xFF00) ? 0xFF : S[1].b );
				   }
               }
          }
          
		  if (flagwneg)		// Process reflective situation
		      S -= 2;		// Process from right to left
		  else				// Default
		      S += 2;

          D += 2;

          --l;
     }
     
     if (w & 1) {
          if (!(S->a & 0xF000)) {
              *D = PIXEL_RGB16( (S->r & 0xFF00) ? 0xFF : S->r,
                                (S->g & 0xFF00) ? 0xFF : S->g,
                                (S->b & 0xFF00) ? 0xFF : S->b );
          }
     }
}

static void Sacc_to_Aop_rgb24(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

	 if (flagwneg)		// Process reflective situ
	     S += w;		// Go right to left
	 
     while (w--) {
          if (!(S->a & 0xF000)) {
               *D++ = (S->b & 0xFF00) ? 0xFF : S->b;
               *D++ = (S->g & 0xFF00) ? 0xFF : S->g;
               *D++ = (S->r & 0xFF00) ? 0xFF : S->r;
          }
          else
               D += 3;

		  if (flagwneg)	// go right to left if neg width
		      S--;
		  else
			  S++;
     }
}

static void Sacc_to_Aop_rgb32(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u32       *D = (__u32*)Aop;

	 if (flagwneg)	// negative width
	     S += w;	// process from right to left
	 
     while (w--) {
          if (!(S->a & 0xF000)) {
               *D = PIXEL_RGB32( (S->r & 0xFF00) ? 0xFF : S->r,
                                 (S->g & 0xFF00) ? 0xFF : S->g,
                                 (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
		  if (flagwneg)		// negative width
		      S--;			// Process from right to left
		  else
		      S++;			// process from left to right (default)
     }
}

static void Sacc_to_Aop_argb(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u32       *D = (__u32*)Aop;

     // if negative width, work from right to left
     if (flagwneg)
     {
         S += w;	// Points to right hand side of input buffer
     }

     while (w--) {
          if (!(S->a & 0xF000)) {
               *D = PIXEL_ARGB( (S->a & 0xFF00) ? 0xFF : S->a,
                                (S->r & 0xFF00) ? 0xFF : S->r,
                                (S->g & 0xFF00) ? 0xFF : S->g,
                                (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
		  
	  // if negative width decrement instead of increment to change dir.
	  if (flagwneg)
	      S--;
	  else
	      S++;
     }
}

static void Sacc_to_Aop_a8(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

	 if (flagwneg)	// if negative width
	     S += w;	// process pixels from right to left.

     while (w--) {
          if (!(S->a & 0xF000))
               *D = (S->a & 0xFF00) ? 0xFF : S->a;

          D++;
		  if (flagwneg)	// if negative width
		      S--;		// process from right to left
		  else			// default
          	  S++;		// process from left to right
     }
}

#ifdef SUPPORT_RGB332
static void Sacc_to_Aop_rgb332(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

	 if (flagwneg)	// negative width?
	     S += w;	// Process pixels from right to left
	 
     while (w--) {
          if (!(S->a & 0xF000)) {
               *D = PIXEL_RGB332( (S->r & 0xFF00) ? 0xFF : S->r,
                                  (S->g & 0xFF00) ? 0xFF : S->g,
                                  (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
     	  if (flagwneg)	// negative width?
		      S--;		// Process from right to left
		  else			// Default
			  S++;		// Process from left to right
     }
}
#endif

static void Sacc_to_Aop_lut8(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

	 if (flagwneg)	// negative width
	     S += w;	// Process from right to left

     while (w--) {
          if (!(S->a & 0xF000)) {
               *D = dfb_palette_search( Alut,
                                        (__u8)((S->r & 0xFF00) ? 0xFF : S->r),
                                        (__u8)((S->g & 0xFF00) ? 0xFF : S->g),
                                        (__u8)((S->b & 0xFF00) ? 0xFF : S->b),
                                        (__u8)((S->a & 0xFF00) ? 0xFF : S->a));
          }

          D++;
		  if (flagwneg)		// Negative width
		      S--;			// Process from right to left
		  else				// Default
			  S++;			// process from left to right
     }
}

GFunc Sacc_to_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     Sacc_to_Aop_argb1555,
     Sacc_to_Aop_rgb16,
     Sacc_to_Aop_rgb24,
     Sacc_to_Aop_rgb32,
     Sacc_to_Aop_argb,
     Sacc_to_Aop_a8,
     NULL,
#ifdef SUPPORT_RGB332
     Sacc_to_Aop_rgb332,
#else
     NULL,
#endif
     NULL,
     NULL,
     NULL,
     Sacc_to_Aop_lut8
};

/******************************** Sacc_toK_Aop_PFI ****************************/

static void Sacc_toK_Aop_argb1555(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u16       *D = (__u16*)Aop;

     while (w--) {
          if (!(S->a & 0xF000) && ((*D & 0x7fff) == (__u16)Dkey)) {
               *D = PIXEL_ARGB1555( (S->a & 0xFF00) ? 0xFF : S->a,
                                    (S->r & 0xFF00) ? 0xFF : S->r,
                                    (S->g & 0xFF00) ? 0xFF : S->g,
                                    (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
          S++;
     }
}

static void Sacc_toK_Aop_rgb16(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u16       *D = (__u16*)Aop;

     while (w--) {
          if (!(S->a & 0xF000) && (*D == (__u16)Dkey)) {
               *D = PIXEL_RGB16( (S->r & 0xFF00) ? 0xFF : S->r,
                                 (S->g & 0xFF00) ? 0xFF : S->g,
                                 (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
          S++;
     }
}

static void Sacc_toK_Aop_rgb24(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

     /* FIXME: implement keying */
     while (w--) {
          if (!(S->a & 0xF000)) {
               *D++ = (S->b & 0xFF00) ? 0xFF : S->b;
               *D++ = (S->g & 0xFF00) ? 0xFF : S->g;
               *D++ = (S->r & 0xFF00) ? 0xFF : S->r;
          }
          else
               D += 3;

          S++;
     }
}

static void Sacc_toK_Aop_rgb32(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u32       *D = (__u32*)Aop;

     while (w--) {
          if (!(S->a & 0xF000) && ((*D & 0xffffff) == Dkey)) {
               *D = PIXEL_RGB32( (S->r & 0xFF00) ? 0xFF : S->r,
                                 (S->g & 0xFF00) ? 0xFF : S->g,
                                 (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
          S++;
     }
}

static void Sacc_toK_Aop_argb(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u32       *D = (__u32*)Aop;

     while (w--) {
          if (!(S->a & 0xF000) && ((*D & 0xffffff) == Dkey)) {
               *D = PIXEL_ARGB( (S->a & 0xFF00) ? 0xFF : S->a,
                                (S->r & 0xFF00) ? 0xFF : S->r,
                                (S->g & 0xFF00) ? 0xFF : S->g,
                                (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
          S++;
     }
}

static void Sacc_toK_Aop_a8(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

     /* FIXME: do all or do none? */
     while (w--) {
          if (!(S->a & 0xF000))
               *D = (S->a & 0xFF00) ? 0xFF : S->a;

          D++;
          S++;
     }
}

#ifdef SUPPORT_RGB332
static void Sacc_toK_Aop_rgb332(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

     while (w--) {
          if (!(S->a & 0xF000) && (*D == (__u8)Dkey)) {
               *D = PIXEL_RGB332( (S->r & 0xFF00) ? 0xFF : S->r,
                                  (S->g & 0xFF00) ? 0xFF : S->g,
                                  (S->b & 0xFF00) ? 0xFF : S->b );
          }

          D++;
          S++;
     }
}
#endif

static void Sacc_toK_Aop_lut8(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     __u8        *D = (__u8*)Aop;

     while (w--) {
          if (!(S->a & 0xF000) && (*D == (__u8)Dkey)) {
               *D = dfb_palette_search( Alut,
                                        (__u8)((S->r & 0xFF00) ? 0xFF : S->r),
                                        (__u8)((S->g & 0xFF00) ? 0xFF : S->g),
                                        (__u8)((S->b & 0xFF00) ? 0xFF : S->b),
                                        (__u8)((S->a & 0xFF00) ? 0xFF : S->a));
          }

          D++;
          S++;
     }
}

GFunc Sacc_toK_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     Sacc_toK_Aop_argb1555,
     Sacc_toK_Aop_rgb16,
     Sacc_toK_Aop_rgb24,
     Sacc_toK_Aop_rgb32,
     Sacc_toK_Aop_argb,
     Sacc_toK_Aop_a8,
     NULL,
#ifdef SUPPORT_RGB332
     Sacc_toK_Aop_rgb332,
#else
     NULL,
#endif
     NULL,
     NULL,
     NULL,
     Sacc_toK_Aop_lut8
};

/************** Bop_a8_set_alphapixel_Aop_PFI *********************************/

#define SET_ALPHA_PIXEL_DUFFS_DEVICE(D, S, w, format) \
     while (w) {\
          int l = w & 7;\
          switch (l) {\
               default:\
                    l = 8;\
                    SET_ALPHA_PIXEL_##format( D[7], S[7] );\
               case 7:\
                    SET_ALPHA_PIXEL_##format( D[6], S[6] );\
               case 6:\
                    SET_ALPHA_PIXEL_##format( D[5], S[5] );\
               case 5:\
                    SET_ALPHA_PIXEL_##format( D[4], S[4] );\
               case 4:\
                    SET_ALPHA_PIXEL_##format( D[3], S[3] );\
               case 3:\
                    SET_ALPHA_PIXEL_##format( D[2], S[2] );\
               case 2:\
                    SET_ALPHA_PIXEL_##format( D[1], S[1] );\
               case 1:\
                    SET_ALPHA_PIXEL_##format( D[0], S[0] );\
          }\
          D += l;\
          S += l;\
          w -= l;\
     }

static void Bop_a8_set_alphapixel_Aop_argb1555(void)
{
     int    w  = Dlength;
     __u8  *S  = Bop;
     __u16 *D  = Aop;
     __u32  rb = Cop & 0x7c1f;
     __u32  g  = Cop & 0x03e0;

#define SET_ALPHA_PIXEL_ARGB1555(d,a) \
     switch (a) {\
          case 0xff: d = (__u16)Cop;\
          case 0: break;\
          default: {\
               register __u8   s = (a>>3)+1;\
               register __u32 t1 = (d & 0x7c1f);\
               register __u32 t2 = (d & 0x03e0);\
               d = (__u16)(((a & 0x80) << 8) | \
                           ((((rb-t1)*s+(t1<<5)) & 0x000f83e0) + \
                            ((( g-t2)*s+(t2<<5)) & 0x00007c00)) >> 5);\
          }\
     }

     SET_ALPHA_PIXEL_DUFFS_DEVICE( D, S, w, ARGB1555 );

#undef SET_ALPHA_PIXEL_ARGB1555
}


static void Bop_a8_set_alphapixel_Aop_rgb16(void)
{
     int    w  = Dlength;
     __u8  *S  = Bop;
     __u16 *D  = Aop;
     __u32  rb = Cop & 0xf81f;
     __u32  g  = Cop & 0x07e0;

#define SET_ALPHA_PIXEL_RGB16(d,a)\
     switch (a) {\
          case 0xff: d = (__u16)Cop;\
          case 0: break;\
          default: {\
               register __u8   s = (a>>2)+1;\
               register __u32 t1 = (d & 0xf81f);\
               register __u32 t2 = (d & 0x07e0);\
               d  = (__u16)(((((rb-t1)*s+(t1<<6)) & 0x003e07c0) + \
                             ((( g-t2)*s+(t2<<6)) & 0x0001f800)) >> 6);\
          }\
     }

     SET_ALPHA_PIXEL_DUFFS_DEVICE( D, S, w, RGB16 );

#undef SET_ALPHA_PIXEL_RGB16
}

static void Bop_a8_set_alphapixel_Aop_rgb24(void)
{
     int    w = Dlength;
     __u8  *S = Bop;
     __u8  *D = Aop;

#define SET_ALPHA_PIXEL_RGB24(d,r,g,b,a)\
     switch (a) {\
         case 0xff:\
               d[0] = b;\
               d[1] = g;\
               d[2] = r;\
          case 0: break;\
          default: {\
               register __u16 s = a+1;\
               d[0] = ((b-d[0]) * s + (d[0] << 8)) >> 8;\
               d[1] = ((g-d[1]) * s + (d[1] << 8)) >> 8;\
               d[2] = ((r-d[2]) * s + (d[2] << 8)) >> 8;\
          }\
     }

     while (w>4) {
          SET_ALPHA_PIXEL_RGB24( D, color.r, color.g, color.b, *S ); D+=3; S++;
          SET_ALPHA_PIXEL_RGB24( D, color.r, color.g, color.b, *S ); D+=3; S++;
          SET_ALPHA_PIXEL_RGB24( D, color.r, color.g, color.b, *S ); D+=3; S++;
          SET_ALPHA_PIXEL_RGB24( D, color.r, color.g, color.b, *S ); D+=3; S++;
      w-=4;
     }
     while (w--) {
          SET_ALPHA_PIXEL_RGB24( D, color.r, color.g, color.b, *S ); D+=3, S++;
     }

#undef SET_ALPHA_PIXEL_RGB24
}

static void Bop_a8_set_alphapixel_Aop_rgb32(void)
{
     int    w  = Dlength;
     __u8  *S  = Bop;
     __u32 *D  = Aop;
     __u32  rb = Cop & 0xff00ff;
     __u32  g  = Cop & 0x00ff00;

#define SET_ALPHA_PIXEL_RGB32(d,a)\
     switch (a) {\
          case 0xff: d = Cop;\
          case 0: break;\
          default: {\
               register __u16  s = a+1;\
               register __u32 t1 = (d & 0x00ff00ff);\
               register __u32 t2 = (d & 0x0000ff00);\
               d = ((((rb-t1)*s+(t1<<8)) & 0xff00ff00) + \
                    ((( g-t2)*s+(t2<<8)) & 0x00ff0000)) >> 8;\
          }\
     }

     SET_ALPHA_PIXEL_DUFFS_DEVICE( D, S, w, RGB32 );

#undef SET_ALPHA_PIXEL_RGB32
}


/* saturating alpha blend */

static void Bop_a8_set_alphapixel_Aop_argb(void)
{
     int    w  = Dlength;
     __u8  *S  = Bop;
     __u32 *D  = (__u32*)Aop;
     __u32  rb = Cop & 0x00ff00ff;
     __u32  g  = color.g;

#define SET_ALPHA_PIXEL_ARGB(d,a)\
     switch (a) {\
          case 0xff: d = 0xff000000 | Cop;\
          case 0: break;\
          default: {\
               register __u16  s = a+1;\
               register __u32 s1 = 256-s;\
               register __u32 sa = (d >> 24) + a;\
               if (sa & 0xff00) sa = 0xff;\
               d = (sa << 24) + \
                    (((((d & 0x00ff00ff)       * s1) + (rb  * s)) >> 8) & 0x00ff00ff) + \
                    (((((d & 0x0000ff00) >> 8) * s1) + ((g) * s))       & 0x0000ff00);  \
          }\
     }

     SET_ALPHA_PIXEL_DUFFS_DEVICE( D, S, w, ARGB );

#undef SET_ALPHA_PIXEL_ARGB
}

static void Bop_a8_DrawString_Clr_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
	__u32	s = 0;   // source color (forced to zero)

	// separate components...

	__u32 As = s >> 24;
	__u32 Rs = (s >> 16) & 255;
	__u32 Gs = (s >> 8) & 255;
	__u32 Bs = s & 255;

	int w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha

		__u32 Ag = *g;

		if (Ag == 255)
		{
			*d = s;
		}
		else if (Ag != 0)
		{
			// fetch dest

			__u32 dd = *d;

			// separate components...

			__u32 Ad = dd >> 24;
			__u32 Rd = (dd >> 16) & 255;
			__u32 Gd = (dd >> 8) & 255;
			__u32 Bd = dd & 255;

			// d = (1 - a) * d + a * s + 128;

			Ad = (255 - Ag) * Ad + Ag * As + 128;
			Rd = (255 - Ag) * Rd + Ag * Rs + 128;
			Gd = (255 - Ag) * Gd + Ag * Gs + 128;
			Bd = (255 - Ag) * Bd + Ag * Bs + 128;

			// d /= 255;

			Ad = (Ad + (Ad >> 8)) >> 8;
			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// store dest

			*d = (Ad << 24) | (Rd << 16) | (Gd << 8) | Bd;
		}

		d++;
		g++;

		w--;
	}
}

static void Bop_a8_DrawString_Src_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
	__u32	s = Cop; // source color

	// separate components...

	__u32 As = s >> 24;
	__u32 Rs = (s >> 16) & 255;
	__u32 Gs = (s >> 8) & 255;
	__u32 Bs = s & 255;

	int w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha

		__u32 Ag = *g;

		if (Ag == 255)
		{
			*d = s;
		}
		else if (Ag != 0)
		{
			// fetch dest

			__u32 dd = *d;

			// separate components...

			__u32 Ad = dd >> 24;
			__u32 Rd = (dd >> 16) & 255;
			__u32 Gd = (dd >> 8) & 255;
			__u32 Bd = dd & 255;

			// d = (1 - a) * d + a * s + 128;

			Ad = (255 - Ag) * Ad + Ag * As + 128;
			Rd = (255 - Ag) * Rd + Ag * Rs + 128;
			Gd = (255 - Ag) * Gd + Ag * Gs + 128;
			Bd = (255 - Ag) * Bd + Ag * Bs + 128;

			// d /= 255;

			Ad = (Ad + (Ad >> 8)) >> 8;
			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// store dest

			*d = (Ad << 24) | (Rd << 16) | (Gd << 8) | Bd;
		}

		d++;
		g++;

		w--;
	}
}

static void Bop_a8_DrawString_SrcOver_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
	__u32	s = Cop; // source color

	// separate components...

	__u32 As = s >> 24;
	__u32 Rs = (s >> 16) & 255;
	__u32 Gs = (s >> 8) & 255;
	__u32 Bs = s & 255;

	int w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha (modulated by source alpha)

		__u32 Ag = *g;

		// a = Ag * As + 128;

		__u32 AgAs = Ag * As + 128;

		// a /= 255;

		AgAs = (AgAs + (AgAs >> 8)) >> 8;

		if (AgAs == 255)
		{
			*d = s;
		}
		else if (AgAs != 0)
		{
			// fetch dest

			__u32 dd = *d;

			// separate components...

			__u32 Ad = dd >> 24;
			__u32 Rd = (dd >> 16) & 255;
			__u32 Gd = (dd >> 8) & 255;
			__u32 Bd = dd & 255;

			// d = (1 - a) * d + a * s + 128;

			Ad = (255 - AgAs) * Ad + AgAs * As + 128;
			Rd = (255 - AgAs) * Rd + AgAs * Rs + 128;
			Gd = (255 - AgAs) * Gd + AgAs * Gs + 128;
			Bd = (255 - AgAs) * Bd + AgAs * Bs + 128;

			// d /= 255;

			Ad = (Ad + (Ad >> 8)) >> 8;
			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// store dest

			*d = (Ad << 24) | (Rd << 16) | (Gd << 8) | Bd;
		}

		d++;
		g++;

		w--;
	}
}

static void Bop_a8_DrawString_Xor_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
	__u32	s = Cop; // source color (already xor'ed with the xorcolor)

	// separate components...

//	__u32 As = s >> 24;
	__u32 Rs = (s >> 16) & 255;
	__u32 Gs = (s >> 8) & 255;
	__u32 Bs = s & 255;

	int w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha

		__u32 Ag = *g;

		// modulate source color by glyph alpha, s = (s * a + 128) / 255;

		__u32 RsAg = Rs * Ag + 128;			// s = s * a + 128;
		__u32 GsAg = Gs * Ag + 128; 
		__u32 BsAg = Bs * Ag + 128; 

		RsAg = (RsAg + (RsAg >> 8)) >> 8;	// s = s / 255;
		GsAg = (GsAg + (GsAg >> 8)) >> 8;
		BsAg = (BsAg + (BsAg >> 8)) >> 8;

		// no need to xor components separately - xor is a bitwise operation...

		*d ^= (RsAg << 16) | (GsAg << 8) | BsAg;	// leave alpha untouched...

		d++;
		g++;

		w--;
	}
}

static void Bop_a8_DrawString_Clr_Aop_rgb16(void)
{
	__u16 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
	__u16	s = 0;   // source color (forced to zero)
	int		w = 0;

	// separate components...

//	__u32 As = 0;
	__u32 Rs = 0;
	__u32 Gs = 0;
	__u32 Bs = 0;

	w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha

		__u32 Ag = *g;

		if (Ag == 255)
		{
			*d = s;
		}
		else if (Ag != 0)
		{
			// fetch dest

			__u32 dd = *d;

			// separate components...

			__u32 Rd = (dd >> 11) & 31;
			__u32 Gd = (dd >> 5) & 63;
			__u32 Bd = dd & 31;

			// scale components...

			Rd = (Rd * 2106 + 123) >> 8;	// scale by (255 / 31)
			Gd = (Gd * 1036 + 132) >> 8;	// scale by (255 / 63)
			Bd = (Bd * 2106 + 123) >> 8;	// scale by (255 / 31)

			// d = (1 - a) * d + a * s + 128;

			Rd = (255 - Ag) * Rd + Ag * Rs + 128;
			Gd = (255 - Ag) * Gd + Ag * Gs + 128;
			Bd = (255 - Ag) * Bd + Ag * Bs + 128;

			// d /= 255;

			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// scale components...

			Rd = Rd * 31 + 128;				// scale by (31 / 255)
			Gd = Gd * 63 + 128;				// scale by (63 / 255)
			Bd = Bd * 31 + 128;				// scale by (31 / 255)

			// d /= 255;

			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// store dest

			*d = (__u16)((Rd << 11) | (Gd << 5) | Bd);
		}

		d++;
		g++;

		w--;
	}
}

static void Bop_a8_DrawString_Src_Aop_rgb16(void)
{
	__u16 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
	__u16	s = 0;   // source color
	int		w = 0;

	// separate components...

//	__u32 As = Cop >> 24;
	__u32 Rs = (Cop >> 16) & 255;
	__u32 Gs = (Cop >> 8) & 255;
	__u32 Bs = Cop & 255;

	// scale components...

	Rs = Rs * 31 + 128;						// scale by (31 / 255)
	Gs = Gs * 63 + 128;						// scale by (63 / 255)
	Bs = Bs * 31 + 128;						// scale by (31 / 255)

	// d /= 255;

	Rs = (Rs + (Rs >> 8)) >> 8;
	Gs = (Gs + (Gs >> 8)) >> 8;
	Bs = (Bs + (Bs >> 8)) >> 8;

	// initialize source

	s = (__u16)((Rs << 11) | (Gs << 5) | Bs);

	// separate components, again...

//	As = Cop >> 24;
	Rs = (Cop >> 16) & 255;
	Gs = (Cop >> 8) & 255;
	Bs = Cop & 255;

	w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha

		__u32 Ag = *g;

		if (Ag == 255)
		{
			*d = s;
		}
		else if (Ag != 0)
		{
			// fetch dest

			__u32 dd = *d;

			// separate components...

			__u32 Rd = (dd >> 11) & 31;
			__u32 Gd = (dd >> 5) & 63;
			__u32 Bd = dd & 31;

			// scale components...

			Rd = (Rd * 2106 + 123) >> 8;	// scale by (255 / 31)
			Gd = (Gd * 1036 + 132) >> 8;	// scale by (255 / 63)
			Bd = (Bd * 2106 + 123) >> 8;	// scale by (255 / 31)

			// d = (1 - a) * d + a * s + 128;

			Rd = (255 - Ag) * Rd + Ag * Rs + 128;
			Gd = (255 - Ag) * Gd + Ag * Gs + 128;
			Bd = (255 - Ag) * Bd + Ag * Bs + 128;

			// d /= 255;

			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// scale components...

			Rd = Rd * 31 + 128;				// scale by (31 / 255)
			Gd = Gd * 63 + 128;				// scale by (63 / 255)
			Bd = Bd * 31 + 128;				// scale by (31 / 255)

			// d /= 255;

			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// store dest

			*d = (__u16)((Rd << 11) | (Gd << 5) | Bd);
		}

		d++;
		g++;

		w--;
	}
}

static void Bop_a8_DrawString_SrcOver_Aop_rgb16(void)
{
	__u16 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
	__u16	s = 0;   // source color
	int		w = 0;

	// separate components...

	__u32 As = Cop >> 24;
	__u32 Rs = (Cop >> 16) & 255;
	__u32 Gs = (Cop >> 8) & 255;
	__u32 Bs = Cop & 255;

	// scale components...

	Rs = Rs * 31 + 128;						// scale by (31 / 255)
	Gs = Gs * 63 + 128;						// scale by (63 / 255)
	Bs = Bs * 31 + 128;						// scale by (31 / 255)

	// d /= 255;

	Rs = (Rs + (Rs >> 8)) >> 8;
	Gs = (Gs + (Gs >> 8)) >> 8;
	Bs = (Bs + (Bs >> 8)) >> 8;

	// initialize source

	s = (__u16)((Rs << 11) | (Gs << 5) | Bs);

	// separate components, again...

	As = Cop >> 24;
	Rs = (Cop >> 16) & 255;
	Gs = (Cop >> 8) & 255;
	Bs = Cop & 255;

	w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha (modulated by source alpha)

		__u32 Ag = *g;

		// a = Ag * As + 128;

		__u32 AgAs = Ag * As + 128;

		// a /= 255;

		AgAs = (AgAs + (AgAs >> 8)) >> 8;

		if (AgAs == 255)
		{
			*d = s;
		}
		else if (AgAs != 0)
		{
			// fetch dest

			__u32 dd = *d;

			// separate components...

			__u32 Rd = (dd >> 11) & 31;
			__u32 Gd = (dd >> 5) & 63;
			__u32 Bd = dd & 31;

			// scale components...

			Rd = (Rd * 2106 + 123) >> 8;	// scale by (255 / 31)
			Gd = (Gd * 1036 + 132) >> 8;	// scale by (255 / 63)
			Bd = (Bd * 2106 + 123) >> 8;	// scale by (255 / 31)

			// d = (1 - a) * d + a * s + 128;

			Rd = (255 - AgAs) * Rd + AgAs * Rs + 128;
			Gd = (255 - AgAs) * Gd + AgAs * Gs + 128;
			Bd = (255 - AgAs) * Bd + AgAs * Bs + 128;

			// d /= 255;

			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// scale components...

			Rd = Rd * 31 + 128;				// scale by (31 / 255)
			Gd = Gd * 63 + 128;				// scale by (63 / 255)
			Bd = Bd * 31 + 128;				// scale by (31 / 255)

			// d /= 255;

			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// store dest

			*d = (__u16)((Rd << 11) | (Gd << 5) | Bd);
		}

		d++;
		g++;

		w--;
	}
}

static void Bop_a8_DrawString_Xor_Aop_rgb16(void)
{
	__u16 *	d = Aop; // destination
	__u8 *	g = Bop; // glyph alpha
//	__u16	s = 0;   // source color (already xor'ed with the xorcolor)
	int		w = 0;

	// separate components...

//	__u32 As = Cop >> 24;
	__u32 Rs = (Cop >> 16) & 255;
	__u32 Gs = (Cop >> 8) & 255;
	__u32 Bs = Cop & 255;

	w = Dlength;
	while (w > 0)
	{
		// fetch glyph alpha

		__u32 Ag = *g;

		// modulate source color by glyph alpha, s = (s * a + 128) / 255;

		__u32 RsAg = Rs * Ag + 128;			// s = s * a + 128;
		__u32 GsAg = Gs * Ag + 128; 
		__u32 BsAg = Bs * Ag + 128; 

		RsAg = (RsAg + (RsAg >> 8)) >> 8;	// s = s / 255;
		GsAg = (GsAg + (GsAg >> 8)) >> 8;
		BsAg = (BsAg + (BsAg >> 8)) >> 8;

		// scale components...

		RsAg = RsAg * 31 + 128;				// scale by (31 / 255)
		GsAg = GsAg * 63 + 128;				// scale by (63 / 255)
		BsAg = BsAg * 31 + 128;				// scale by (31 / 255)

		// d /= 255;

		RsAg = (RsAg + (RsAg >> 8)) >> 8;
		GsAg = (GsAg + (GsAg >> 8)) >> 8;
		BsAg = (BsAg + (BsAg >> 8)) >> 8;

		// no need to xor components separately - xor is a bitwise operation...

		*d ^= (__u16)((RsAg << 11) | (GsAg << 5) | BsAg); // alpha untouched...

		d++;
		g++;

		w--;
	}
}

// This function is used for single stage pipeline optimization - TURNED OFF to fix bug #5994
#ifdef OPTIMIZATION 
static void Bop_argb_BitBlt_Src_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u32 *	s = Bop; // source
	__u32	a = Kop; // alpha const

	if (a == 255)
	{
		dfb_memmove(d, s, Dlength * 4);
	}
	else if (a != 0)
	{
		int w = Dlength;
		while (w > 0)
		{
			// fetch source

			__u32 ss = *s;

			// fetch source alpha (modulated by source alpha)

			__u32 As = ss >> 24;

			// As = As * Ak + 128;

			As = As * a + 128;

			// As /= 255;

			As = (As + (As >> 8)) >> 8;

			// store dest

			*d = (ss & 0x00FFFFFF) | (As << 24);

			d++;
			s++;

			w--;
		}
	}
	else	/* a == 0 case */
	{
		int w = Dlength;
		while (w > 0)
		{
			*d = (*s & 0x00FFFFFF);

			d++;
			s++;

			w--;
		}
	}
}

#endif

#ifdef OPTIMIZATION
static void Bop_argb_BitBlt_SrcOver_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u32 *	s = Bop; // source
	__u32	a = Kop; // alpha const

	if (a == 255)
	{
		int w = Dlength;
		while (w > 0)
		{
			// fetch source

			__u32 ss = *s;

			// fetch source alpha

			__u32 As = ss >> 24;

			if (As == 255)
			{
				*d = ss;
			}
			else if (As != 0)
			{
				// separate components...

				__u32 Rs = (ss >> 16) & 255;
				__u32 Gs = (ss >> 8) & 255;
				__u32 Bs = ss & 255;

				// fetch dest

				__u32 dd = *d;

				// separate components...

				__u32 Ad = dd >> 24;
				__u32 Rd = (dd >> 16) & 255;
				__u32 Gd = (dd >> 8) & 255;
				__u32 Bd = dd & 255;

				// d = (1 - a) * d + a * s + 128;
	
                //Ad = (255 - As) * Ad + As * As + 128;			
				Ad = (255 - As) * Ad + As * 255 + 128;
				Rd = (255 - As) * Rd + As * Rs + 128;
				Gd = (255 - As) * Gd + As * Gs + 128;
				Bd = (255 - As) * Bd + As * Bs + 128;

				//Ad = (255 - As) * Ad + As * As + 128;
			
				// d /= 255;

				Ad = (Ad + (Ad >> 8)) >> 8;
				Rd = (Rd + (Rd >> 8)) >> 8;
				Gd = (Gd + (Gd >> 8)) >> 8;
				Bd = (Bd + (Bd >> 8)) >> 8;

				// store dest

				*d = (Ad << 24) | (Rd << 16) | (Gd << 8) | Bd;
			}

			d++;
			s++;

			w--;
		}
	}
	else if (a != 0)
	{
		int w = Dlength;
		while (w > 0)
		{
			// fetch source

			__u32 ss = *s;

			// fetch source alpha (modulated by source alpha)

			__u32 As = ss >> 24;

			// As = As * Ak + 128;

			As = As * a + 128;

			// As /= 255;

			As = (As + (As >> 8)) >> 8;

			if (As == 255)
			{
				*d = ss;
			}
			else if (As != 0)
			{
				// separate components...

				__u32 Rs = (ss >> 16) & 255;
				__u32 Gs = (ss >> 8) & 255;
				__u32 Bs = ss & 255;

				// fetch dest

				__u32 dd = *d;

				// separate components...

				__u32 Ad = dd >> 24;
				__u32 Rd = (dd >> 16) & 255;
				__u32 Gd = (dd >> 8) & 255;
				__u32 Bd = dd & 255;

				// d = (1 - a) * d + a * s + 128;

				Ad = (255 - As) * Ad + As * 255 + 128;
				Rd = (255 - As) * Rd + As * Rs + 128;
				Gd = (255 - As) * Gd + As * Gs + 128;
				Bd = (255 - As) * Bd + As * Bs + 128;

				// d /= 255;

				Ad = (Ad + (Ad >> 8)) >> 8;
				Rd = (Rd + (Rd >> 8)) >> 8;
				Gd = (Gd + (Gd >> 8)) >> 8;
				Bd = (Bd + (Bd >> 8)) >> 8;

				// store dest

				*d = (Ad << 24) | (Rd << 16) | (Gd << 8) | Bd;
			}

			d++;
			s++;

			w--;
		}
	}
}
#endif

static void Bop_a8_set_alphapixel_Aop_a8(void)
{
     int    w = Dlength;
     __u8  *S = Bop;
     __u8  *D = Aop;

#define SET_ALPHA_PIXEL_A8(d,a)\
     switch (a) {\
          case 0xff: d = 0xff;\
          case 0: break; \
          default: {\
               register __u16 s  = (a)+1;\
               register __u16 s1 = 256-s;\
               d = (d * s1 + s) >> 8;\
          }\
     }

     SET_ALPHA_PIXEL_DUFFS_DEVICE( D, S, w, A8 );

#undef SET_ALPHA_PIXEL_A8
}

#ifdef SUPPORT_RGB332
static void Bop_a8_set_alphapixel_Aop_rgb332(void)
{
     int    w = Dlength;
     __u8  *S = Bop;
     __u8  *D = Aop;

/* FIXME: implement correctly! */
#define SET_ALPHA_PIXEL_RGB332(d,a) \
     if (a & 0x80) \
          d = Cop;

     SET_ALPHA_PIXEL_DUFFS_DEVICE( D, S, w, RGB332 );
#undef SET_ALPHA_PIXEL_RGB332
}
#endif

static void Bop_a8_set_alphapixel_Aop_lut8(void)
{
     int    w = Dlength;
     __u8  *S = Bop;
     __u8  *D = Aop;

#define SET_ALPHA_PIXEL_LUT8(d,alpha) \
     switch (alpha) {\
          case 0xff: d = (__u8)Cop;\
          case 0: break; \
          default: {\
               register __u16 s = alpha+1;\
               DFBColor      dc = Alut->entries[d];\
               __u16         sa = alpha + dc.a;\
               dc.r = ((color.r - dc.r) * s + (dc.r << 8)) >> 8;\
               dc.g = ((color.g - dc.g) * s + (dc.g << 8)) >> 8;\
               dc.b = ((color.b - dc.b) * s + (dc.b << 8)) >> 8;\
               d = dfb_palette_search( Alut, dc.r, dc.g, dc.b,\
                                             (__u8)(sa & 0xff00 ? 0xff : sa) );\
          }\
     }

     while (w--) {
          SET_ALPHA_PIXEL_LUT8( *D, *S );
          D++, S++;
     }

#undef SET_ALPHA_PIXEL_LUT8
}

GFunc Bop_a8_set_alphapixel_Aop_PFI[DFB_NUM_PIXELFORMATS] = {
     Bop_a8_set_alphapixel_Aop_argb1555,
     Bop_a8_set_alphapixel_Aop_rgb16,
     Bop_a8_set_alphapixel_Aop_rgb24,
     Bop_a8_set_alphapixel_Aop_rgb32,
     Bop_a8_set_alphapixel_Aop_argb,
     Bop_a8_set_alphapixel_Aop_a8,
     NULL,
#ifdef SUPPORT_RGB332
     Bop_a8_set_alphapixel_Aop_rgb332,
#else
     NULL,
#endif
     NULL,
     NULL,
     NULL,
     Bop_a8_set_alphapixel_Aop_lut8
};


/********************************* Xacc_blend *********************************/

#ifdef USE_MMX
void Xacc_blend_srcalpha_MMX(void);
void Xacc_blend_invsrcalpha_MMX(void);
#endif

static void Xacc_blend_zero(void)
{
     int          i;
     Accumulator *X = Xacc;

     for (i=0; i<Dlength; i++) {
          if (!(X[i].a & 0xF000))
               X[i].a = X[i].r = X[i].g = X[i].b = 0;
     }
}

static void Xacc_blend_one(void)
{
}

static void Xacc_blend_srccolor(void)
{
     ONCE( "Xacc_blend_srccolor() unimplemented" );
}

static void Xacc_blend_invsrccolor(void)
{
     ONCE( "Xacc_blend_invsrccolor() unimplemented" );
}

static void Xacc_blend_srcalpha(void)
{
     int          w = Dlength;
     Accumulator *X = Xacc;

     if (Sacc) {
          Accumulator *S = Sacc;

          while (w--) {
               if (!(X->a & 0xF000))
			   {
				   register __u16 Sa = S->a;
				   //register __u16 i;

				   MUL8RND(Sa, X->r, X->r);
				   //i = (Sa * X->r) + 128;
				   //X->r = (i + (i >> 8)) >> 8;

				   MUL8RND(Sa, X->g, X->g);
				   //i = (Sa * X->g) + 128;
				   //X->g = (i + (i >> 8)) >> 8;

				   MUL8RND(Sa, X->b, X->b);
				   //i = (Sa * X->b) + 128;
				   //X->b = (i + (i >> 8)) >> 8;

				   MUL8RND(Sa, X->a, X->a);
				   //i = (Sa * X->a) + 128;
				   //X->a = (i + (i >> 8)) >> 8;
               }

               X++;
               S++;
          }
     }
     else {
		 register __u16 Sa = color.a;

		 while (w--) {
			 if (!(X->a & 0xF000))
			 {
				 //register __u16 i;

				 MUL8RND(Sa, X->r, X->r);
				 //i = (Sa * X->r) + 128;
				 //X->r = (i + (i >> 8)) >> 8;

				 MUL8RND(Sa, X->g, X->g);
				 //i = (Sa * X->g) + 128;
				 //X->g = (i + (i >> 8)) >> 8;

				 MUL8RND(Sa, X->b, X->b);
				 //i = (Sa * X->b) + 128;
				 //X->b = (i + (i >> 8)) >> 8;

				 MUL8RND(Sa, X->a, X->a);
				 //i = (Sa * X->a) + 128;
				 //X->a = (i + (i >> 8)) >> 8;
			 }
			 X++;
		 }
     }
}



static void Xacc_blend_invsrcalpha(void)
{
     int          w = Dlength;
     Accumulator *X = Xacc;

     if (Sacc) {
          Accumulator *S = Sacc;

          while (w--) {
               if (!(X->a & 0xF000))
			   {
                    register __u16 Sa = 0x100 - S->a;

				    if (S->a == 0xFF)
					{
					    Sa = 0;
					}

					MUL8RND(Sa, X->r, X->r);
                    //X->r = (Sa * X->r) >> 8;
					MUL8RND(Sa, X->g, X->g);
                    //X->g = (Sa * X->g) >> 8;
					MUL8RND(Sa, X->b, X->b);
                    //X->b = (Sa * X->b) >> 8;
					MUL8RND(Sa, X->a, X->a);
                    //X->a = (Sa * X->a) >> 8;
               }

               X++;
               S++;
          }
     }
     else {
          register __u16 Sa = 0x100 - color.a;
		  if (color.a == 0xFF)
		  {
			  Sa = 0;
		  }

          while (w--) {
			  if (!(X->a & 0xF000))
			  {

				  MUL8RND(Sa, X->r, X->r);
				  //X->r = (Sa * X->r) >> 8;
				  MUL8RND(Sa, X->g, X->g);
				  //X->g = (Sa * X->g) >> 8;
				  MUL8RND(Sa, X->b, X->b);
				  //X->b = (Sa * X->b) >> 8;
				  MUL8RND(Sa, X->a, X->a);
				  //X->a = (Sa * X->a) >> 8;
			  }

			  X++;
          }
     }
}

static void Xacc_blend_dstalpha(void)
{
     int          w = Dlength;
     Accumulator *X = Xacc;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(X->a & 0xF000))
		  {
			  register __u16 Da = D->a;
			  //register __u16 i;

			  MUL8RND(Da, X->r, X->r);
			  //i = (Da * X->r) + 128;
			  //X->r = (i + (i >> 8)) >> 8;

			  MUL8RND(Da, X->g, X->g);
			  //i = (Da * X->g) + 128;
			  //X->g = (i + (i >> 8)) >> 8;

			  MUL8RND(Da, X->b, X->b);
			  //i = (Da * X->b) + 128;
			  //X->b = (i + (i >> 8)) >> 8;

			  MUL8RND(Da, X->a, X->a);
			  //i = (Da * X->a) + 128;
			  //X->a = (i + (i >> 8)) >> 8;
          }
          X++;
          D++;
     }
}

static void Xacc_blend_xor(void)
{
	int          w = Dlength;
    Accumulator *X = Xacc;
    Accumulator *S = Sacc;
    while (w--) {
		if (!(X->a & 0xF000)) {
			X->r ^= S->r ^ color.r;
            X->g ^= S->g ^ color.g;
            X->b ^= S->b ^ color.b; 
        }
        X++;
        S++;
	}
}

static void Xacc_blend_invdstalpha(void)
{
     int          w = Dlength;
     Accumulator *X = Xacc;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(X->a & 0xF000))
		  {
			  register __u16 Da = 0x100 - D->a;

			  MUL8RND(Da, X->r, X->r);
			  //i = (Da * X->r) + 128;
			  //X->r = (i + (i >> 8)) >> 8;

			  MUL8RND(Da, X->g, X->g);
			  //i = (Da * X->g) + 128;
			  //X->g = (i + (i >> 8)) >> 8;

			  MUL8RND(Da, X->b, X->b);
			  //i = (Da * X->b) + 128;
			  //X->b = (i + (i >> 8)) >> 8;

			  MUL8RND(Da, X->a, X->a);
			  //i = (Da * X->a) + 128;
			  //X->a = (i + (i >> 8)) >> 8;
          }
          X++;
          D++;
     }
}

static void Xacc_blend_destcolor(void)
{
     ONCE( "Xacc_blend_destcolor() unimplemented" );
}

static void Xacc_blend_invdestcolor(void)
{
     ONCE( "Xacc_blend_invdestcolor() unimplemented" );
}

static void Xacc_blend_srcalphasat(void)
{
     ONCE( "Xacc_blend_srcalphasat() unimplemented" );
}

static GFunc Xacc_blend[] = {
     Xacc_blend_zero,         /* DSBF_ZERO         */
     Xacc_blend_one,          /* DSBF_ONE          */
     Xacc_blend_srccolor,     /* DSBF_SRCCOLOR     */
     Xacc_blend_invsrccolor,  /* DSBF_INVSRCCOLOR  */
     Xacc_blend_srcalpha,     /* DSBF_SRCALPHA     */
     Xacc_blend_invsrcalpha,  /* DSBF_INVSRCALPHA  */
     Xacc_blend_dstalpha,     /* DSBF_DESTALPHA    */
     Xacc_blend_invdstalpha,  /* DSBF_INVDESTALPHA */
     Xacc_blend_destcolor,    /* DSBF_DESTCOLOR    */
     Xacc_blend_invdestcolor, /* DSBF_INVDESTCOLOR */
     Xacc_blend_srcalphasat,  /* DSBF_SRCALPHASAT  */
	 Xacc_blend_xor,		  /* DSBF_XOR          */		
};

/********************************* Dacc_modulation ****************************/

#ifdef USE_MMX
void Dacc_modulate_argb_MMX(void);
#endif

static void Dacc_set_alpha(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000)) {
               D->a = color.a;
          }

          D++;
     }
}

static void Dacc_modulate_alpha(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000)) {
               D->a = (Cacc.a * D->a) >> 8;
          }

          D++;
     }
}

static void Dacc_modulate_rgb(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000))
		  {
  			  MUL8RND(Cacc.r, D->r, D->r);
              //D->r = (Cacc.r * D->r) >> 8;
			  MUL8RND(Cacc.g, D->g, D->g);
			  //D->g = (Cacc.g * D->g) >> 8;
			  MUL8RND(Cacc.b, D->b, D->b);
			  //D->b = (Cacc.b * D->b) >> 8;
          }
          D++;
     }
}

static void Dacc_modulate_rgb_set_alpha(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000))
		  {
               D->a = color.a;
			   MUL8RND(Cacc.r, D->r, D->r);
			   //D->r = (Cacc.r * D->r) >> 8;
			   MUL8RND(Cacc.g, D->g, D->g);
			   //D->g = (Cacc.g * D->g) >> 8;
			   MUL8RND(Cacc.b, D->b, D->b);
			   //D->b = (Cacc.b * D->b) >> 8;
          }
          D++;
     }
}

static void Dacc_modulate_argb(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000))
		  {
			  MUL8RND(Cacc.r, D->r, D->r);
			  //D->r = (Cacc.r * D->r) >> 8;
			  MUL8RND(Cacc.g, D->g, D->g);
			  //D->g = (Cacc.g * D->g) >> 8;
			  MUL8RND(Cacc.b, D->b, D->b);
			  //D->b = (Cacc.b * D->b) >> 8;
			  MUL8RND(Cacc.a, D->a, D->a);
              //D->a = (Cacc.a * D->a) >> 8;
          }

          D++;
     }
}

static GFunc Dacc_modulation[] = {
     NULL,
     NULL,
     Dacc_set_alpha,
     Dacc_modulate_alpha,
     Dacc_modulate_rgb,
     Dacc_modulate_rgb,
     Dacc_modulate_rgb_set_alpha,
     Dacc_modulate_argb
};

/********************************* misc accumulator operations ****************/

static void Sacc_mult_by_alpha_const(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;

     while (w--) {
		  S->a= (S->a * alpha_const)>>8;

          S++;
     }
}

static void Dacc_premultiply(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000))
		  {
			  register __u16 Da = D->a;

			  MUL8RND(Da, D->r, D->r);
			  //i = (Da * D->r) + 128;
			  //D->r = (i + (i >> 8)) >> 8;
			  MUL8RND(Da, D->g, D->g);
			  //i = (Da * D->g) + 128;
			  //D->g = (i + (i >> 8)) >> 8;
			  MUL8RND(Da, D->b, D->b);
			  //i = (Da * D->b) + 128;
			  //D->b = (i + (i >> 8)) >> 8;
          }

          D++;
     }
}

static void Dacc_demultiply(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
		 if ( (D->a != 0) && !(D->a & 0xF000) )
		  {
			  register __u16 Da = D->a;

			  D->r = (D->r << 8) / Da;
			  D->g = (D->g << 8) / Da;
			  D->b = (D->b << 8) / Da;
          }

          D++;
     }
}

static void Dacc_xor(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000)) {
               /* the destination alpha is left unchanged */
               D->r ^= color.r;
               D->g ^= color.g;
               D->b ^= color.b;
          }

          D++;
     }
}

#ifdef USE_MMX
void Cacc_add_to_Dacc_MMX(void);
void Sacc_add_to_Dacc_MMX(void);
#endif

static void Cacc_to_Dacc(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--)
          *D++ = Cacc;
}



static void Cacc_add_to_Dacc_C(void)
{
     int          w = Dlength;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000)) {
               D->a += Cacc.a;
               D->r += Cacc.r;
               D->g += Cacc.g;
               D->b += Cacc.b;
          }
          D++;
     }
}

GFunc Cacc_add_to_Dacc = Cacc_add_to_Dacc_C;

static void Sacc_add_to_Dacc_C(void)
{
     int          w = Dlength;
     Accumulator *S = Sacc;
     Accumulator *D = Dacc;

     while (w--) {
          if (!(D->a & 0xF000)) {
               D->a += S->a;
               D->r += S->r;
               D->g += S->g;
               D->b += S->b;
          }
          D++;
          S++;
     }
}

GFunc Sacc_add_to_Dacc = Sacc_add_to_Dacc_C;

static void Sop_is_Aop(void) { Sop = Aop;}
static void Sop_is_Bop(void) { Sop = Bop;}

static void Slut_is_Alut(void) { Slut = Alut;}
static void Slut_is_Blut(void) { Slut = Blut;}

static void Sacc_is_NULL(void) { Sacc = NULL;}
static void Sacc_is_Aacc(void) { Sacc = Aacc;}
static void Sacc_is_Bacc(void) { Sacc = Bacc;}

static void Dacc_is_Aacc(void) { Dacc = Aacc;}
static void Dacc_is_Bacc(void) { Dacc = Bacc;}

static void Xacc_is_Aacc(void) { Xacc = Aacc;}
static void Xacc_is_Bacc(void) { Xacc = Bacc;}

#ifdef OPTIMIZATION
static void FillRect_Clr_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u32	s = 0;   // solid color (forced to zero)

	int w = Dlength;
	while (w > 0)
	{
		*d = s;

		d++;

		w--;
	}
}
#endif

#ifdef OPTIMIZATION
static void FillRect_Src_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u32	s = Cop; // solid color

	int w = Dlength;
	while (w > 0)
	{
		*d = s;

		d++;

		w--;
	}
}
#endif

#ifdef OPTIMIZATION
static void FillRect_SrcOver_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u32	s = Cop; // solid color

	// separate components...

	__u32 As = s >> 24;
	__u32 Rs = (s >> 16) & 255;
	__u32 Gs = (s >> 8) & 255;
	__u32 Bs = s & 255;

	if (As == 255)
	{
		int w = Dlength;
		while (w > 0)
		{
			*d = s;

			d++;

			w--;
		}
	}
	else if (As != 0)
	{
		int w = Dlength;
		while (w > 0)
		{
			// fetch dest

			__u32 dd = *d;

			// separate components...

			__u32 Ad = dd >> 24;
			__u32 Rd = (dd >> 16) & 255;
			__u32 Gd = (dd >> 8) & 255;
			__u32 Bd = dd & 255;

			// d = (1 - a) * d + a * s + 128;

            //Ad = (255 - As) * Ad + As * As + 128;
			Ad = (255 - As) * Ad + As * 255 + 128;
			Rd = (255 - As) * Rd + As * Rs + 128;
			Gd = (255 - As) * Gd + As * Gs + 128;
			Bd = (255 - As) * Bd + As * Bs + 128;

			// d /= 255;

			Ad = (Ad + (Ad >> 8)) >> 8;
			Rd = (Rd + (Rd >> 8)) >> 8;
			Gd = (Gd + (Gd >> 8)) >> 8;
			Bd = (Bd + (Bd >> 8)) >> 8;

			// store dest

			*d = (Ad << 24) | (Rd << 16) | (Gd << 8) | Bd;

			d++;

			w--;
		}
	}
}
#endif

#ifdef OPTIMIZATION
static void FillRect_Xor_Aop_argb(void)
{
	__u32 *	d = Aop; // destination
	__u32	s = Cop & 0x00FFFFFF; // solid color (clear alpha)

	int w = Dlength;
	while (w > 0)
	{
		*d ^= s; // leave alpha untouched...

		d++;

		w--;
	}
}
#endif

/******************************************************************************/

void gGetDriverInfo( GraphicsDriverInfo *info )
{
     snprintf( info->name,
               DFB_GRAPHICS_DRIVER_INFO_NAME_LENGTH, "Software Driver" );

#ifdef USE_MMX
     if (dfb_mm_accel() & MM_MMX) {
          if (!dfb_config->mmx) {
               INITMSG( "MMX detected, but disabled by --no-mmx \n");
          }
          else {
               gInit_MMX();

               snprintf( info->name, DFB_GRAPHICS_DRIVER_INFO_NAME_LENGTH,
                         "MMX Software Driver" );

               INITMSG( "MMX detected and enabled\n");
          }
     }
     else {
          INITMSG( "No MMX detected\n" );
     }
#endif

     snprintf( info->vendor, DFB_GRAPHICS_DRIVER_INFO_VENDOR_LENGTH,
               "convergence integrated media GmbH" );

     info->version.major = 0;
     info->version.minor = 6;
}

void gGetDeviceInfo( GraphicsDeviceInfo *info )
{
     snprintf( info->name, DFB_GRAPHICS_DEVICE_INFO_NAME_LENGTH,
               "Software Rasterizer" );

     snprintf( info->vendor, DFB_GRAPHICS_DEVICE_INFO_VENDOR_LENGTH,
               use_mmx ? "MMX" : "Generic" );

     info->caps.accel    = DFXL_NONE;
     info->caps.flags    = 0;
     info->caps.drawing  = DSDRAW_NOFX;
     info->caps.blitting = DSBLIT_NOFX;
}

#define MODULATION_FLAGS (DSBLIT_BLEND_ALPHACHANNEL | \
                          DSBLIT_BLEND_COLORALPHA   | \
                          DSBLIT_COLORIZE           | \
                          DSBLIT_DST_PREMULTIPLY    | \
                          DSBLIT_SRC_PREMULTIPLY    | \
                          DSBLIT_DEMULTIPLY		    | \
                          DSBLIT_XOR)

int gAquire( CardState *state, DFBAccelerationMask accel )
{
     GFunc       *funcs       = gfuncs;
     CoreSurface *destination = state->destination;
     CoreSurface *source      = state->source;

     int dst_pfi, src_pfi = 0;

     DFBSurfaceLockFlags lock_flags;

     pthread_mutex_lock( &generic_lock );

     flagwneg = state->flagwneg;	// Get dest rect neg width/height bools
     flaghneg = state->flaghneg;

     /* Debug checks */
     if (!state->destination) {
          BUG("state check: no destination");
          pthread_mutex_unlock( &generic_lock );
          return 0;
     }
     if (!source  &&  DFB_BLITTING_FUNCTION( accel )) {
          BUG("state check: no source");
          pthread_mutex_unlock( &generic_lock );
          return 0;
     }
     
     dst_caps   = destination->caps;
     dst_height = destination->height;
     dst_format = destination->format;
     dst_bpp    = DFB_BYTES_PER_PIXEL( dst_format );
     dst_pfi    = DFB_PIXELFORMAT_INDEX( dst_format );

     if (DFB_BLITTING_FUNCTION( accel )) {
          src_caps   = source->caps;
          src_height = source->height;
          src_format = source->format;
          src_bpp    = DFB_BYTES_PER_PIXEL( src_format );
          src_pfi    = DFB_PIXELFORMAT_INDEX( src_format );

          lock_flags = state->blittingflags & ( DSBLIT_BLEND_ALPHACHANNEL |
                                                DSBLIT_BLEND_COLORALPHA   |
                                                DSBLIT_DST_COLORKEY ) ?
                       DSLF_READ | DSLF_WRITE : DSLF_WRITE;
     }
     else
          lock_flags = state->drawingflags & ( DSDRAW_BLEND |
                                               DSDRAW_DST_COLORKEY ) ?
                       DSLF_READ | DSLF_WRITE : DSLF_WRITE;

     color = state->color;
	 alpha_const = (state->alpha_const != 0) ? (state->alpha_const+1) : 0;

     switch (dst_format) {
          case DSPF_ARGB1555:
               Cop = PIXEL_ARGB1555( color.a, color.r, color.g, color.b );
               break;
          case DSPF_RGB16:
               Cop = PIXEL_RGB16( color.r, color.g, color.b );
               break;
          case DSPF_RGB24:
               Cop = PIXEL_RGB24( color.r, color.g, color.b );
               break;
          case DSPF_RGB32:
               Cop = PIXEL_RGB32( color.r, color.g, color.b );
               break;
          case DSPF_ARGB:
               Cop = PIXEL_ARGB( color.a, color.r, color.g, color.b );
               break;
          case DSPF_A8:
               Cop = color.a;
               break;
          case DSPF_YUY2:
               Cop   = (__u32)Y_FROM_RGB( color.r, color.g, color.b );
               CbCop = (__u8)CB_FROM_RGB( color.r, color.g, color.b );
               CrCop = (__u8)CR_FROM_RGB( color.r, color.g, color.b );
               Cop   = PIXEL_YUY2( Cop, CbCop, CrCop );
               break;
#ifdef SUPPORT_RGB332
          case DSPF_RGB332:
               Cop = PIXEL_RGB332( color.r, color.g, color.b );
               break;
#endif
          case DSPF_UYVY:
               Cop   = (__u32)Y_FROM_RGB( color.r, color.g, color.b );
               CbCop = (__u8)CB_FROM_RGB( color.r, color.g, color.b );
               CrCop = (__u8)CR_FROM_RGB( color.r, color.g, color.b );
               Cop   = PIXEL_UYVY( Cop, CbCop, CrCop );
               break;
          case DSPF_I420:
               Cop   = (__u32)Y_FROM_RGB( color.r, color.g, color.b );
               CbCop = (__u8)CB_FROM_RGB( color.r, color.g, color.b );
               CrCop = (__u8)CR_FROM_RGB( color.r, color.g, color.b );
               break;
          case DSPF_YV12:
               Cop   = (__u32)Y_FROM_RGB( color.r, color.g, color.b );
               CbCop = (__u8)CR_FROM_RGB( color.r, color.g, color.b );
               CrCop = (__u8)CB_FROM_RGB( color.r, color.g, color.b );
               break;
          case DSPF_LUT8:
               Cop  = state->color_index;
               Alut = destination->palette;
               break;
          default:
               ONCE("unsupported destination format");
               pthread_mutex_unlock( &generic_lock );
               return 0;
     }

     if (DFB_BLITTING_FUNCTION( accel )) {
          switch (src_format) {
               case DSPF_ARGB1555:
               case DSPF_RGB16:
               case DSPF_RGB24:
               case DSPF_RGB32:
               case DSPF_ARGB:
               case DSPF_A8:
               case DSPF_RGB332:
                    break;
               case DSPF_YUY2:
               case DSPF_UYVY:
               case DSPF_I420:
               case DSPF_YV12:
                    if (accel != DFXL_BLIT || src_format != dst_format ||
                        state->blittingflags != DSBLIT_NOFX)
                    {
                         ONCE("only copying blits supported for YUV in software");
                         pthread_mutex_unlock( &generic_lock );
                         return 0;
                    }
                    break;
               case DSPF_LUT8:
                    Blut = source->palette;
                    break;
               default:
                    ONCE("unsupported source format");
                    pthread_mutex_unlock( &generic_lock );
                    return 0;
          }
     }

     dfb_surfacemanager_lock( dfb_gfxcard_surface_manager() );

     if (DFB_BLITTING_FUNCTION( accel )) {
          if (dfb_surface_software_lock( source,
                                         DSLF_READ, &src_org, &src_pitch, 1 )) {
               dfb_surfacemanager_unlock( dfb_gfxcard_surface_manager() );
               pthread_mutex_unlock( &generic_lock );
               return 0;
          }

          src_field_offset = src_height/2 * src_pitch;
          
          state->source_locked = 1;
     }
     else
          state->source_locked = 0;

     if (dfb_surface_software_lock( state->destination,
                                    lock_flags, &dst_org, &dst_pitch, 0 )) {

          if (state->source_locked)
               dfb_surface_unlock( source, 1 );

          dfb_surfacemanager_unlock( dfb_gfxcard_surface_manager() );
          pthread_mutex_unlock( &generic_lock );
          return 0;
     }

     dst_field_offset = dst_height/2 * dst_pitch;
     
     dfb_surfacemanager_unlock( dfb_gfxcard_surface_manager() );

     
     switch (accel) {
          case DFXL_FILLRECTANGLE:
#ifdef OPTIMIZATION
               if (dst_format == DSPF_ARGB)
			   {
					bool over_ride = false;

					switch (state->porter_duff_rule)
					{
						case DSPD_CLEAR:
							*funcs++ = FillRect_Clr_Aop_argb;					// FillRect(), Clr
							over_ride = true;
							break;
						case DSPD_SRC:
							*funcs++ = FillRect_Src_Aop_argb;					// FillRect(), Src
							over_ride = true;
							break;
						case DSPD_SRC_OVER:
							*funcs++ = FillRect_SrcOver_Aop_argb;				// FillRect(), SrcOver
							over_ride = true;
							break;
						case DSPD_XOR:
							*funcs++ = FillRect_Xor_Aop_argb;					// FillRect(), Xor
							over_ride = true;
							break;
						default:
							over_ride = false;
							break;
					}

					if (over_ride)
					{
						// modulate the source color operand's alpha by the alpha modulation constant

						__u32 alpha = (Cop >> 24);
						__u32 alpha_const = state->alpha_const;

						alpha = alpha * alpha_const + 128;	// alpha *= alpha_const;
						alpha = (alpha + (alpha >> 8)) >> 8;// alpha /= 255;

						Cop = (Cop & 0x00FFFFFF) | (alpha << 24);

						break;	// we're done - break out of the switch (don't fallthru)
					}
			   }
#endif /* OPTIMIZATION */
          /* fallthru */
          case DFXL_DRAWRECTANGLE:
          case DFXL_FILLROUNDRECT:
          case DFXL_DRAWROUNDRECT:
          case DFXL_FILLOVAL:
          case DFXL_DRAWOVAL:
          case DFXL_FILLARC:
          case DFXL_DRAWARC:
          case DFXL_FILLPOLYGON:
          case DFXL_DRAWLINE:

			  /* modulate the color alpha with alpha constant */
			   /* state->color is left untouched */
			   color.a = (color.a * alpha_const)>>8;

               if (state->drawingflags & (DSDRAW_BLEND | DSDRAW_XOR)) {

                    /* not yet completed optimizing checks */
                    if (state->src_blend == DSBF_ZERO) {
                         if (state->dst_blend == DSBF_ZERO) {
                              Cop = 0;
                              if (state->drawingflags & DSDRAW_DST_COLORKEY) {
                                   Dkey = state->dst_colorkey;
                                   *funcs++ = Cop_toK_Aop_PFI[dst_pfi];
                              }
                              else
                                   *funcs++ = Cop_to_Aop_PFI[dst_pfi];
                              break;
                         }
                         else if (state->dst_blend == DSBF_ONE) {
                              break;
                         }
                    }

                    /* load from destination */
                    *funcs++ = Sop_is_Aop;
                    if (DFB_PIXELFORMAT_IS_INDEXED(dst_format))
                         *funcs++ = Slut_is_Alut;
                    *funcs++ = Dacc_is_Aacc;
                    *funcs++ = Sop_PFI_to_Dacc[dst_pfi];

                    /* premultiply destination */
                    if (state->drawingflags & DSDRAW_DST_PREMULTIPLY)
                         *funcs++ = Dacc_premultiply;

                    /* xor destination */
                    if (state->drawingflags & DSDRAW_XOR)
                         *funcs++ = Dacc_xor;

                    /* load source (color) */
					Cacc.a = color.a;
                    Cacc.r = color.r;
                    Cacc.g = color.g;
                    Cacc.b = color.b;

                    /* premultiply source (color) */
                    if (state->drawingflags & DSDRAW_SRC_PREMULTIPLY)
					{
						 __u16 ca = color.a;

						 MUL8RND(ca, Cacc.r, Cacc.r);
                         //Cacc.r = (Cacc.r * ca) >> 8;
						 MUL8RND(ca, Cacc.g, Cacc.g);
                         //Cacc.g = (Cacc.g * ca) >> 8;
						 MUL8RND(ca, Cacc.b, Cacc.b);
                         //Cacc.b = (Cacc.b * ca) >> 8;
                    }
					
                    if (state->drawingflags & DSDRAW_BLEND) {
                         /* source blending */
                         switch (state->src_blend) {
                              case DSBF_ZERO:
                              case DSBF_ONE:
                                   break;
                              case DSBF_SRCALPHA:
								  {
                                        __u16 ca = color.a;

										MUL8RND(ca, Cacc.a, Cacc.a);	
                                        //Cacc.a = (Cacc.a * ca) >> 8;
										MUL8RND(ca, Cacc.r, Cacc.r);	
                                        //Cacc.r = (Cacc.r * ca) >> 8;
										MUL8RND(ca, Cacc.g, Cacc.g);	
                                        //Cacc.g = (Cacc.g * ca) >> 8;
										MUL8RND(ca, Cacc.b, Cacc.b);	
                                        //Cacc.b = (Cacc.b * ca) >> 8;
                                        break;
                                   }
                              case DSBF_INVSRCALPHA:
								  {
                                        __u16 ca = 0x100 - color.a;

										MUL8RND(ca, Cacc.a, Cacc.a);	
                                        //Cacc.a = (Cacc.a * ca) >> 8;
										MUL8RND(ca, Cacc.r, Cacc.r);	
                                        //Cacc.r = (Cacc.r * ca) >> 8;
										MUL8RND(ca, Cacc.g, Cacc.g);	
                                        //Cacc.g = (Cacc.g * ca) >> 8;
										MUL8RND(ca, Cacc.b, Cacc.b);	
                                        //Cacc.b = (Cacc.b * ca) >> 8;
          
                                        break;
                                   }
                              case DSBF_DESTALPHA:
                              case DSBF_INVDESTALPHA:
                              case DSBF_DESTCOLOR:
                              case DSBF_INVDESTCOLOR:
                                   *funcs++ = Dacc_is_Bacc;
                                   *funcs++ = Cacc_to_Dacc;
          
                                   *funcs++ = Dacc_is_Aacc;
                                   *funcs++ = Xacc_is_Bacc;
                                   *funcs++ = Xacc_blend[state->src_blend - 1];
          
                                   break;
                              case DSBF_SRCCOLOR:
                              case DSBF_INVSRCCOLOR:
                              case DSBF_SRCALPHASAT:
                              case DSBF_XOR:
                                   ONCE("unimplemented src blend function");
								   break; 
                         }
          
                         /* destination blending */
                         *funcs++ = Sacc_is_NULL;
                         *funcs++ = Xacc_is_Aacc;
                         *funcs++ = Xacc_blend[state->dst_blend - 1];
          
                         /* add source to destination accumulator */
                         switch (state->src_blend) {
                              case DSBF_ZERO:
                                   break;
                              case DSBF_ONE:
                              case DSBF_SRCALPHA:
                              case DSBF_INVSRCALPHA:
                                   if (Cacc.a || Cacc.r || Cacc.g || Cacc.b)
                                        *funcs++ = Cacc_add_to_Dacc;
                                   break;
                              case DSBF_DESTALPHA:
                              case DSBF_INVDESTALPHA:
                              case DSBF_DESTCOLOR:
                              case DSBF_INVDESTCOLOR:
                                   *funcs++ = Sacc_is_Bacc;
                                   *funcs++ = Sacc_add_to_Dacc;
                                   break;
                              case DSBF_SRCCOLOR:
                              case DSBF_INVSRCCOLOR:
                              case DSBF_SRCALPHASAT:
                              case DSBF_XOR:
                                   ONCE("unimplemented src blend function");
                         }
                    }

                    /* demultiply result */
                    if (state->drawingflags & DSDRAW_DEMULTIPLY)
                         *funcs++ = Dacc_demultiply;
                    
                    /* write to destination */
                    *funcs++ = Sacc_is_Aacc;
                    if (state->drawingflags & DSDRAW_DST_COLORKEY) {
                         Dkey = state->dst_colorkey;
                         *funcs++ = Sacc_toK_Aop_PFI[dst_pfi];
                    }
                    else
                         *funcs++ = Sacc_to_Aop_PFI[dst_pfi];
               }
               else {
                    if (state->drawingflags & DSDRAW_DST_COLORKEY) {
                         Dkey = state->dst_colorkey;
                         *funcs++ = Cop_toK_Aop_PFI[dst_pfi];
                    }
                    else
                         *funcs++ = Cop_to_Aop_PFI[dst_pfi];
               }
               break;
          case DFXL_DRAWSTRING:
               if (src_format == DSPF_A8)
			   {
					bool over_ride = false;

					if (dst_format == DSPF_ARGB)
					{
						switch (state->porter_duff_rule)
						{
							case DSPD_CLEAR:
								*funcs++ = Bop_a8_DrawString_Clr_Aop_argb;			// DrawString(), Clr
								over_ride = true;
								break;
							case DSPD_SRC:
								*funcs++ = Bop_a8_DrawString_Src_Aop_argb;			// DrawString(), Src
								over_ride = true;
								break;
							case DSPD_SRC_OVER:
								*funcs++ = Bop_a8_DrawString_SrcOver_Aop_argb;		// DrawString(), SrcOver
								over_ride = true;
								break;
							case DSPD_XOR:
								*funcs++ = Bop_a8_DrawString_Xor_Aop_argb;			// DrawString(), Xor
								over_ride = true;
								break;
							default:
								over_ride = false;
								break;
						}
					}

					if (dst_format == DSPF_RGB16)
					{
						switch (state->porter_duff_rule)
						{
							case DSPD_CLEAR:
								*funcs++ = Bop_a8_DrawString_Clr_Aop_rgb16;			// DrawString(), Clr
								over_ride = true;
								break;
							case DSPD_SRC:
								*funcs++ = Bop_a8_DrawString_Src_Aop_rgb16;			// DrawString(), Src
								over_ride = true;
								break;
							case DSPD_SRC_OVER:
								*funcs++ = Bop_a8_DrawString_SrcOver_Aop_rgb16;		// DrawString(), SrcOver
								over_ride = true;
								break;
							case DSPD_XOR:
								*funcs++ = Bop_a8_DrawString_Xor_Aop_rgb16;			// DrawString(), Xor
								over_ride = true;
								break;
							default:
								over_ride = false;
								break;
						}

						// dfb pre-formats Cop to match the dst_format, but we always want it in ARGB format
						if (over_ride)
							Cop = PIXEL_ARGB( color.a, color.r, color.g, color.b );
					}

					if (over_ride)
					{
						// modulate the source color operand's alpha by the alpha modulation constant

						__u32 alpha = (Cop >> 24);
						__u32 alpha_const = state->alpha_const;

						alpha = alpha * alpha_const + 128;	// alpha *= alpha_const;
						alpha = (alpha + (alpha >> 8)) >> 8;// alpha /= 255;

						Cop = (Cop & 0x00FFFFFF) | (alpha << 24);

						break;	// we're done - break out of the switch (don't fallthru to DFXL_BLIT)
					}
			   }
          /* fallthru */
          case DFXL_BLIT:
#ifdef OPTIMIZATION 
               if (dst_format == DSPF_ARGB)
			   {
					bool over_ride = false;

					if (src_format == DSPF_ARGB)
					{
						switch (state->porter_duff_rule)
						{
							case DSPD_SRC:
								*funcs++ = Bop_argb_BitBlt_Src_Aop_argb;			// BitBlt(), Src
								over_ride = true;
								break;
							case DSPD_SRC_OVER:
								*funcs++ = Bop_argb_BitBlt_SrcOver_Aop_argb;		// BitBlt(), SrcOver
								over_ride = true;
								break;
							default:
								over_ride = false;
								break;
						}
					}

					if (over_ride)
					{
						// set just the alpha modulation constant operand (the color operand isn't used)

						Kop = state->alpha_const;

						break;	// we're done - break out of the switch (don't fallthru to DFXL_BLIT)
					}
			   }
#endif /* OPTIMIZATION */
               if ((src_format == DSPF_A8) &&
                   (state->blittingflags ==
                     (DSBLIT_BLEND_ALPHACHANNEL | DSBLIT_COLORIZE)) &&
                   (state->src_blend == DSBF_SRCALPHA) &&
                   (state->dst_blend == DSBF_INVSRCALPHA) &&
                   Bop_a8_set_alphapixel_Aop_PFI[dst_pfi])
               {
                    *funcs++ = Bop_a8_set_alphapixel_Aop_PFI[dst_pfi];
                    break;
               }
          /* fallthru */
          case DFXL_STRETCHBLIT: {
                    int modulation = state->blittingflags & MODULATION_FLAGS;

                    if (modulation) {
                         bool read_destination = false;
                         bool source_needs_destination = false;
                          
                         /* check if destination has to be read */
                         if (state->blittingflags & (DSBLIT_BLEND_ALPHACHANNEL |
                                                     DSBLIT_BLEND_COLORALPHA|DSBLIT_XOR))
                         {
                              switch (state->src_blend) {
                                   case DSBF_DESTALPHA:
                                   case DSBF_DESTCOLOR:
                                   case DSBF_INVDESTALPHA:
                                   case DSBF_INVDESTCOLOR:
                                        source_needs_destination = true;
                                   default:
                                        ;
                              }
                              
                              read_destination = source_needs_destination ||
                                                 (state->dst_blend != DSBF_ZERO);
                         }

                         /* read the destination if needed */
                         if (read_destination) {
                              *funcs++ = Sop_is_Aop;
                              if (DFB_PIXELFORMAT_IS_INDEXED(dst_format))
                                   *funcs++ = Slut_is_Alut;
                              *funcs++ = Dacc_is_Aacc;
                              *funcs++ = Sop_PFI_to_Dacc[dst_pfi];

                              if (state->blittingflags & DSBLIT_DST_PREMULTIPLY)
                                   *funcs++ = Dacc_premultiply;
                         }

                         /* read the source */
                         *funcs++ = Sop_is_Bop;
                         if (DFB_PIXELFORMAT_IS_INDEXED(src_format))
                              *funcs++ = Slut_is_Blut;
                         *funcs++ = Dacc_is_Bacc;
                         if (state->blittingflags & DSBLIT_SRC_COLORKEY) {
                              Skey = state->src_colorkey;
                              if ((accel == DFXL_BLIT) || (accel == DFXL_DRAWSTRING))
                                   *funcs++ = Sop_PFI_Kto_Dacc[src_pfi];
                              else
                                   *funcs++ = Sop_PFI_SKto_Dacc[src_pfi];
                         }
                         else {
                              if ((accel == DFXL_BLIT) || (accel == DFXL_DRAWSTRING))
                                   *funcs++ = Sop_PFI_to_Dacc[src_pfi];
                              else
                                   *funcs++ = Sop_PFI_Sto_Dacc[src_pfi];
                         }

                         /* modulate the source if requested */
                         if (Dacc_modulation[modulation & 0x7]) {
                              /* modulation source */
                              Cacc.a = color.a + 1;
                              Cacc.r = color.r + 1;
                              Cacc.g = color.g + 1;
                              Cacc.b = color.b + 1;

                              *funcs++ = Dacc_modulation[modulation & 0x7];
                         }

                         if (state->blittingflags & DSBLIT_SRC_PREMULTIPLY)
                              *funcs++ = Dacc_premultiply;
                         
                         /* do blend functions and combine both accumulators */
                         if (state->blittingflags & (DSBLIT_BLEND_ALPHACHANNEL |
                                                     DSBLIT_BLEND_COLORALPHA|DSBLIT_XOR))
                         {
                              /* Xacc will be blended and written to while
                                 Sacc and Dacc point to the SRC and DST
                                 as referenced by the blending functions */
                              *funcs++ = Sacc_is_Bacc;
                              *funcs++ = Dacc_is_Aacc;

                              if (source_needs_destination &&
                                  state->dst_blend != DSBF_ONE)
                              {
                                   /* blend the source */
                                   *funcs++ = Xacc_is_Bacc;
                                   *funcs++ = Xacc_blend[state->src_blend - 1];
                                   
                                   /* blend the destination */
                                   *funcs++ = Xacc_is_Aacc;
                                   *funcs++ = Xacc_blend[state->dst_blend - 1];
                              }
                              else {
                                   /* blend the destination if needed */
                                   if (read_destination) {
                                        *funcs++ = Xacc_is_Aacc;
										*funcs++ = Sacc_mult_by_alpha_const;
                                        *funcs++ = Xacc_blend[state->dst_blend - 1];
                                   }

                                   /* blend the source */
                                   *funcs++ = Xacc_is_Bacc;
                                   *funcs++ = Xacc_blend[state->src_blend - 1];
                              }

                              /* add the destination to the source */
                              if (read_destination) {
                                   *funcs++ = Sacc_is_Aacc;
                                   *funcs++ = Dacc_is_Bacc;
                                   *funcs++ = Sacc_add_to_Dacc;
                              }
                         }

                         if (state->blittingflags & DSBLIT_DEMULTIPLY) {
                              *funcs++ = Dacc_is_Bacc;
                              *funcs++ = Dacc_demultiply;
                         }
                         
                         /* write source to destination */
						 *funcs++ = Sacc_is_Bacc;
						 *funcs++ = Sacc_to_Aop_PFI[dst_pfi];
                    }
                    else if (src_format == dst_format/* &&
                             (!DFB_PIXELFORMAT_IS_INDEXED(src_format) ||
                              Alut == Blut)*/)
                    {
                         if ((accel == DFXL_BLIT) || (accel == DFXL_DRAWSTRING)) {
                              if (state->blittingflags & DSBLIT_SRC_COLORKEY) {
                                   Skey = state->src_colorkey;
                                   *funcs++ = Bop_PFI_Kto_Aop_PFI[dst_pfi];
                              }
                              else
                                   *funcs++ = Bop_PFI_to_Aop_PFI[dst_pfi];
                         }
                         else {
                              if (state->blittingflags & DSBLIT_SRC_COLORKEY) {
                                   Skey = state->src_colorkey;
                                   *funcs++ = Bop_PFI_SKto_Aop[dst_pfi];
                              }
                              else
                                   *funcs++ = Bop_PFI_Sto_Aop[dst_pfi];
                         }
                    }
                    else {
                         /* slow */
                         Sacc = Dacc = Aacc;

                         *funcs++ = Sop_is_Bop;
                         if (DFB_PIXELFORMAT_IS_INDEXED(src_format))
                              *funcs++ = Slut_is_Blut;

                         if ((accel == DFXL_BLIT) || (accel == DFXL_DRAWSTRING)) {
                              if (state->blittingflags & DSBLIT_SRC_COLORKEY ) {
                                   Skey = state->src_colorkey;
                                   *funcs++ = Sop_PFI_Kto_Dacc[src_pfi];
                              }
                              else
                                   *funcs++ = Sop_PFI_to_Dacc[src_pfi];
                         }
                         else { /* DFXL_STRETCHBLIT */

                              if (state->blittingflags & DSBLIT_SRC_COLORKEY ) {
                                   Skey = state->src_colorkey;
                                   *funcs++ = Sop_PFI_SKto_Dacc[src_pfi];
                              }
                              else
                                   *funcs++ = Sop_PFI_Sto_Dacc[src_pfi];

                         }
						 *funcs++ = Sacc_to_Aop_PFI[dst_pfi];
                    }
                    break;
               }
          default:
               ONCE("unimplemented drawing/blitting function");
               gRelease( state );
               return 0;
     }

     *funcs = NULL;

     return 1;
}

void gRelease( CardState *state )
{
     dfb_surface_unlock( state->destination, 0 );

     if (state->source_locked)
          dfb_surface_unlock( state->source, 1 );

     pthread_mutex_unlock( &generic_lock );
}

#define CHECK_PIPELINE()           \
     {                             \
          if (!*gfuncs)            \
               return;             \
     }                             \

#define RUN_PIPELINE()             \
     {                             \
          GFunc *funcs = gfuncs;   \
                                   \
          do {                     \
               (*funcs++)();       \
          } while (*funcs);        \
     }                             \


static int Aop_field = 0;

static void Aop_xy( void *org, int x, int y, int pitch )
{
     Aop = org;
	__u8* Aop8 = (__u8*)Aop;

     if (dst_caps & DSCAPS_SEPARATED) {
          Aop_field = y & 1;
          if (Aop_field)
               Aop8 += dst_field_offset;

          y /= 2;
     }

     Aop8 += y * pitch  +  x * dst_bpp;
	 Aop = Aop8;
}

static void Aop_next( int pitch )
{
	__u8* Aop8 = (__u8*)Aop;

     if (dst_caps & DSCAPS_SEPARATED) {
          Aop_field = !Aop_field;
          
          if (Aop_field)
               Aop8 += dst_field_offset;
          else
               Aop8 += pitch - dst_field_offset;
     }
     else
          Aop8 += pitch;
	Aop = Aop8;
}

static void Aop_prev( int pitch )
{
	__u8* Aop8 = (__u8*)Aop;

     if (dst_caps & DSCAPS_SEPARATED) {
          Aop_field = !Aop_field;
          
          if (Aop_field)
               Aop8 += dst_field_offset - pitch;
          else
               Aop8 -= dst_field_offset;
     }
     else
          Aop8 -= pitch;
	Aop = Aop8;
}


static int Bop_field = 0;

static void Bop_xy( void *org, int x, int y, int pitch )
{
     Bop = org;
	__u8* Bop8 = (__u8*)Bop;

     if (src_caps & DSCAPS_SEPARATED) {
          Bop_field = y & 1;
          if (Bop_field)
               Bop8 += src_field_offset;

          y /= 2;
     }

     Bop8 += y * pitch  +  x * src_bpp;
    Bop = Bop8;
}

static void Bop_next( int pitch )
{
	__u8* Bop8 = (__u8*)Bop;
     if (src_caps & DSCAPS_SEPARATED) {
          Bop_field = !Bop_field;
          
          if (Bop_field)
               Bop8 += src_field_offset;
          else
               Bop8 += pitch - src_field_offset;
     }
     else
     {
	  // if negative height, decrement so go from bottom to top.
	  if (flaghneg)
	      Bop8 -= pitch;
	  else
	      Bop8 += pitch;
     }
    Bop = Bop8;
}

static void Bop_prev( int pitch )
{
	__u8* Bop8 = (__u8*)Bop;
     if (src_caps & DSCAPS_SEPARATED) {
          Bop_field = !Bop_field;
          
          if (Bop_field)
               Bop8 += src_field_offset - pitch;
          else
               Bop8 -= src_field_offset;
     }
     else
          Bop8 -= pitch;
    Bop = Bop8; 
}


void gFillSegment( DFBRectangle *rect )
{
     CHECK_PIPELINE();
     Dlength = rect->w;
     Aop_xy( dst_org, rect->x, rect->y, dst_pitch );
     RUN_PIPELINE();
     Aop_next( dst_pitch );
}

void gFillRectangle( DFBRectangle *rect )
{
     int h;

     CHECK_PIPELINE();

     Dlength = rect->w;

     if (dst_format == DSPF_YUY2 || dst_format == DSPF_UYVY)
          Dlength /= 2;

     Aop_xy( dst_org, rect->x, rect->y, dst_pitch );

     h = rect->h;
     while (h--) {
          RUN_PIPELINE();

          Aop_next( dst_pitch );
     }

     if (dst_format == DSPF_I420 || dst_format == DSPF_YV12) {
          rect->x /= 2;
          rect->y /= 2;
          rect->w /= 2;
          rect->h /= 2;

          Dlength = rect->w;

          Cop = CbCop;
          Aop_xy( (__u8 *)dst_org + dst_height * dst_pitch,
                  rect->x, rect->y, dst_pitch/2 );
          h = rect->h;
          while (h--) {
               RUN_PIPELINE();

               Aop_next( dst_pitch/2 );
          }

          Cop = CrCop;
          Aop_xy( (__u8 *)dst_org + dst_height * dst_pitch + dst_height * dst_pitch/4,
                  rect->x, rect->y, dst_pitch/2 );
          h = rect->h;
          while (h--) {
               RUN_PIPELINE();

               Aop_next( dst_pitch/2 );
          }
     }
}

void gDrawLine( DFBRegion *line )
{
     int i,dx,dy,sdy,dxabs,dyabs,x,y,px,py;

     CHECK_PIPELINE();

     /* the horizontal distance of the line */
     dx = line->x2 - line->x1;
     dxabs = DFB_ABS(dx);

     /* the vertical distance of the line */
     dy = line->y2 - line->y1;
     dyabs = DFB_ABS(dy);

     if (!dx || !dy) {              /* draw horizontal/vertical line */
          DFBRectangle rect;
           
          rect.x = DFB_MIN (line->x1, line->x2);
          rect.y = DFB_MIN (line->y1, line->y2);
          rect.w = dxabs + 1;
          rect.h = dyabs + 1;

          gFillRectangle( &rect );
          return;
     }

     sdy = DFB_SIGN(dy) * DFB_SIGN(dx);
     x = dyabs >> 1;
     y = dxabs >> 1;

     if (dx > 0) {
          px  = line->x1;
          py  = line->y1;
     } else {
          px  = line->x2;
          py  = line->y2;
     }

     if (dxabs >= dyabs) { /* the line is more horizontal than vertical */

          for (i=0, Dlength=1; i<dxabs; i++, Dlength++) {
               y += dyabs;
               if (y >= dxabs) {
                    Aop_xy( dst_org, px, py, dst_pitch );
                    RUN_PIPELINE();
                    px += Dlength;
                    Dlength = 0;
                    y -= dxabs;
                    py += sdy;
               }
          }
          Aop_xy( dst_org, px, py, dst_pitch );
          RUN_PIPELINE();
     }
     else { /* the line is more vertical than horizontal */

          Dlength = 1;
          Aop_xy( dst_org, px, py, dst_pitch );
          RUN_PIPELINE();

          for (i=0; i<dyabs; i++) {
               x += dxabs;
               if (x >= dyabs) {
                    x -= dyabs;
                    px++;
               }
               py += sdy;

               Aop_xy( dst_org, px, py, dst_pitch );
               RUN_PIPELINE();
          }
     }
}

static void gDoBlit( int sx,     int sy,
                     int width,  int height,
                     int dx,     int dy,
                     int spitch, int dpitch,
                     void *sorg, void *dorg )
{
     if (dy > sy) {
          /* we must blit from bottom to top */
          Dlength = width;

          Aop_xy( dorg, dx, dy + height - 1, dpitch );
          Bop_xy( sorg, sx, sy + height - 1, spitch );
          
          while (height--) {
               RUN_PIPELINE();

               Aop_prev( dpitch );
               Bop_prev( spitch );
          }
     }
     else {
          /* we must blit from top to bottom */
          Dlength = width;

          Aop_xy( dorg, dx, dy, dpitch );
          Bop_xy( sorg, sx, sy, spitch );
          
          while (height--) {
               RUN_PIPELINE();

               Aop_next( dpitch );
               Bop_next( spitch );
          }
     }
}

void gBlit( DFBRectangle *rect, int dx, int dy )
{
     CHECK_PIPELINE();

     if (dx > rect->x)
          /* we must blit from right to left */
          Ostep = -1;
     else
          /* we must blit from left to right*/
          Ostep = 1;

     gDoBlit( rect->x, rect->y, rect->w, rect->h, dx, dy,
              src_pitch, dst_pitch, src_org, dst_org );

     /* do other planes */
     if (src_format == DSPF_I420 || src_format == DSPF_YV12) {
          __u8* sorg8;
          __u8* dorg8;
          void *sorg = (__u8 *)src_org + src_height * src_pitch;
          void *dorg = (__u8 *)dst_org + dst_height * dst_pitch;
          
          gDoBlit( rect->x/2, rect->y/2, rect->w/2, rect->h/2, dx/2, dy/2,
                   src_pitch/2, dst_pitch/2, sorg, dorg );
          
          sorg8 = sorg; dorg8 = dorg;
          sorg8 += src_height * src_pitch / 4;
          dorg8 += dst_height * dst_pitch / 4;
          sorg = sorg8; dorg = dorg8;
          
          gDoBlit( rect->x/2, rect->y/2, rect->w/2, rect->h/2, dx/2, dy/2,
                   src_pitch/2, dst_pitch/2, sorg, dorg );
     }
}

// Modified to handle negative widths and heights. Note that this should work
// fine for alpha blended situations, but will probably not function correctly
// with color keying when negative values are used.
void gStretchBlit( DFBRectangle *srect, DFBRectangle *drect )
{
     int f;
     int i = 0;

     CHECK_PIPELINE();

     Dlength = drect->w;
     SperD = (srect->w << 16) / drect->w;

     f = (srect->h << 16) / drect->h;

     Aop_xy( dst_org, drect->x, drect->y, dst_pitch );
     Bop_xy( src_org, srect->x, srect->y, src_pitch );

     // if negative height then point source lines to end of height and work
     // backwards.
     if (flaghneg)
     {
         __u8* Bop8 = (__u8*)Bop;
         Bop8 += (src_pitch * drect->h);	// Point to end of lines
         Bop = Bop8;
     }

     while (drect->h--) {
	  
          RUN_PIPELINE();

          Aop_next( dst_pitch );

          i += f;

          while (i > 0xFFFF) {
               i -= 0x10000;
               Bop_next( src_pitch );
          }
     }
}


#ifdef USE_MMX

/*
 * patches function pointers to MMX functions
 */
void gInit_MMX(void)
{
     use_mmx = 1;

/********************************* Sop_PFI_Sto_Dacc ***************************/
     Sop_PFI_Sto_Dacc[DFB_PIXELFORMAT_INDEX(DSPF_ARGB)] = Sop_argb_Sto_Dacc_MMX;
/********************************* Sop_PFI_to_Dacc ****************************/
     Sop_PFI_to_Dacc[DFB_PIXELFORMAT_INDEX(DSPF_RGB16)] = Sop_rgb16_to_Dacc_MMX;
     Sop_PFI_to_Dacc[DFB_PIXELFORMAT_INDEX(DSPF_RGB32)] = Sop_rgb32_to_Dacc_MMX;
     Sop_PFI_to_Dacc[DFB_PIXELFORMAT_INDEX(DSPF_ARGB )] = Sop_argb_to_Dacc_MMX;
/********************************* Sacc_to_Aop_PFI ****************************/
     Sacc_to_Aop_PFI[DFB_PIXELFORMAT_INDEX(DSPF_RGB16)] = Sacc_to_Aop_rgb16_MMX;
     Sacc_to_Aop_PFI[DFB_PIXELFORMAT_INDEX(DSPF_RGB32)] = Sacc_to_Aop_rgb32_MMX;
/********************************* Xacc_blend *********************************/
     Xacc_blend[DSBF_SRCALPHA-1] = Xacc_blend_srcalpha_MMX;
     Xacc_blend[DSBF_INVSRCALPHA-1] = Xacc_blend_invsrcalpha_MMX;
/********************************* Dacc_modulation ****************************/
     Dacc_modulation[DSBLIT_BLEND_ALPHACHANNEL |
                     DSBLIT_BLEND_COLORALPHA |
                     DSBLIT_COLORIZE] = Dacc_modulate_argb_MMX;
/********************************* misc accumulator operations ****************/
     Cacc_add_to_Dacc = Cacc_add_to_Dacc_MMX;
     Sacc_add_to_Dacc = Sacc_add_to_Dacc_MMX;
}

#endif

