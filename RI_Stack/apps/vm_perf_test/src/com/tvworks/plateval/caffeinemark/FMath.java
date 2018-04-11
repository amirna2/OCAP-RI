 // COPYRIGHT_BEGIN
 //  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 //  
 //  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
 //  
 //  This software is available under multiple licenses: 
 //  
 //  (1) BSD 2-clause 
 //   Redistribution and use in source and binary forms, with or without modification, are
 //   permitted provided that the following conditions are met:
 //        ·Redistributions of source code must retain the above copyright notice, this list 
 //             of conditions and the following disclaimer.
 //        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
 //             and the following disclaimer in the documentation and/or other materials provided with the 
 //             distribution.
 //   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 //   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 //   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
 //   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 //   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 //   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 //   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 //   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 //   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 //   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 //   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 //  
 //  (2) GPL Version 2
 //   This program is free software; you can redistribute it and/or modify
 //   it under the terms of the GNU General Public License as published by
 //   the Free Software Foundation, version 2. This program is distributed
 //   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 //   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 //   PURPOSE. See the GNU General Public License for more details.
 //  
 //   You should have received a copy of the GNU General Public License along
 //   with this program.If not, see<http:www.gnu.org/licenses/>.
 //  
 //  (3)CableLabs License
 //   If you or the company you represent has a separate agreement with CableLabs
 //   concerning the use of this code, your rights and obligations with respect
 //   to this code shall be as set forth therein. No license is granted hereunder
 //   for any other purpose.
 //  
 //   Please contact CableLabs if you need additional information or 
 //   have any questions.
 //  
 //       CableLabs
 //       858 Coal Creek Cir
 //       Louisville, CO 80027-9750
 //       303 661-9100
 // COPYRIGHT_END
 

package com.tvworks.plateval.caffeinemark;

public class FMath
{
   public static final float PI=3.14159265358979323f;
   public static final float E=2.71828182846f;

   static final float sintable[]=
   {
      0.000000f, 0.017452f, 0.034899f, 0.052336f, 0.069756f, 0.087156f, 0.104528f, 0.121869f, 0.139173f, 0.156434f,
      0.173648f, 0.190809f, 0.207912f, 0.224951f, 0.241922f, 0.258819f, 0.275637f, 0.292372f, 0.309017f, 0.325568f,
      0.342020f, 0.358368f, 0.374607f, 0.390731f, 0.406737f, 0.422618f, 0.438371f, 0.453990f, 0.469472f, 0.484810f,
      0.500000f, 0.515038f, 0.529919f, 0.544639f, 0.559193f, 0.573576f, 0.587785f, 0.601815f, 0.615661f, 0.629320f,
      0.642788f, 0.656059f, 0.669131f, 0.681998f, 0.694658f, 0.707107f, 0.719340f, 0.731354f, 0.743145f, 0.754710f,
      0.766044f, 0.777146f, 0.788011f, 0.798636f, 0.809017f, 0.819152f, 0.829038f, 0.838671f, 0.848048f, 0.857167f,
      0.866025f, 0.874620f, 0.882948f, 0.891007f, 0.898794f, 0.906308f, 0.913545f, 0.920505f, 0.927184f, 0.933580f,
      0.939693f, 0.945519f, 0.951057f, 0.956305f, 0.961262f, 0.965926f, 0.970296f, 0.974370f, 0.978148f, 0.981627f,
      0.984808f, 0.987688f, 0.990268f, 0.992546f, 0.994522f, 0.996195f, 0.997564f, 0.998630f, 0.999391f, 0.999848f,
      1.000000f
   };

   static public float sin( float radians )
   {
      int angle, sign, index;

      angle= (int)((radians/PI)*180.0f);
      angle= angle%360;
      if ( angle < 0 )
      {
         angle= -angle;
         if ( angle > 180 )
            sign= 1;
         else
            sign= -1;
      }
      else
      {
         if ( angle > 180 )
            sign= -1;
         else
            sign= 1;
      }

      index= (angle % 180);
      if ( index > 90 )
      {
         index= 90-(index-90);
      }

      if ( sign > 0 )
         return sintable[index];
      else
         return -sintable[index];
   }

   static public float cos( float radians )
   {
      return( sin(radians+(PI/2.0f)) );
   }
   // From "Software Manual for the Elementary Functions" by Cody & Waite
   static float sqrt( float v )
   {
      int bits;
      int N;
      float f, fe, y;

      if ( v < 0 ) throw new ArithmeticException();

      if ( v == 0.0f ) return v;

      bits= Float.floatToIntBits(v);
      N= ((bits>>23)&0xFF);
      if ( N == 0 )
      {
         N= -126;
      }
      else
      {
         N -= 0x7F;
      }
      N += 1;
      bits= ((bits & 0x807FFFFF)|(0x7E<<23));
      f= Float.intBitsToFloat(bits);

      y= 0.42578f + 0.57422f * f;

      y= 0.5f*y + 0.5f*f/y;

      y= 0.5f*y + 0.5f*f/y;

      if ( (N & 1) == 0 )
      {
         fe= Float.intBitsToFloat( ((N>>1)+0x7F)<<23 );
         y *= fe;
      }
      else
      {
         fe= Float.intBitsToFloat( (((N+1)>>1)+0x7F)<<23 );
         y= 0.707106781f*y*fe;
      }

      return y;
   }

   static float abs( float x )
   {
      if ( x < 0.0f )
      {
         return -x;
      }
      else
      {
         return x;
      }
   }


   // From "Software Manual for the Elementary Functions" by Cody & Waite
   static float log( float x )
   {
      int bits;
      int exp, mantissa, N;
      float f;
      float znum, zden, z, w;
      float A, B, r, R;
      float XN, result;

      if ( x < 0.0f )
      {
         return Float.NaN;
      }

      bits= Float.floatToIntBits(x);
      exp= ((bits>>23)&0xFF);
      if ( exp == 0 )
      {
         exp= -126;
         mantissa= (bits & 0x7FFFFF);
      }
      else
      {
         exp -= 0x7F;
         mantissa= ((bits & 0x7FFFFF)|0x800000);
      }

      N= exp+1;
      f= Float.intBitsToFloat( (mantissa & 0x7FFFFF)|((0x7F-1)<<23) );

      if ( f > 0.70710678118654752440f )
      {
         znum= (f-0.5f)-0.5f;
         zden= (f*0.5f)+0.5f;
      }
      else
      {
         N= N-1;
         znum= f-0.5f;
         zden= (znum * 0.5f) + 0.5f;
      }

      z= znum/zden;      
      w= z*z;

      A= -0.5527074855e+0f;
      B= w -0.6632718214e+1f;

      r= w * A/B;
      R= z + z*r;

      XN= (float)N;

      result= ((-2.121944400546905827679e-4f*XN + R) + ((355.0f/512.0f)*XN));

      return result;
   }

   // From "Software Manual for the Elementary Functions" by Cody & Waite
   static float exp( float x )
   {
      float result, temp, XN;
      float X1, X2, g, z;
      float gP, Q, r;
      int N, bits, exp;

      // compare with slightly smaller than ln(Float.MAX_VALUE)
      if ( x > 88.722f )
      {
         return Float.POSITIVE_INFINITY;
      }
      // compare with slightly larger than ln(Float.MIN_VALUE)
      else if ( x < -87.336f)
      {
         return 0.0f;
      }
      else if ( abs(x) < 2.98023223877e-8f )
      {
         return 1.0f;
      }

      // temp= x/log(2)
      temp= x*1.4426950408889634074f;
      if ( temp < 0.0f )
      {
         N= (int)(temp-0.5f);
      }
      else
      {
         N= (int)(temp+0.5f);
      }
      XN= (float)N;

      X1= (float)((int)x);
      X2= x-X1;
      g= ((X1-XN*(355.0f/512.0f))+X2)+2.1219444005469058277e-4f*XN;

      z= g*g;

      gP= (z*0.00416028863f + 0.24999999950f)*g;
      Q= z*0.04998717878f + 0.5f;
      
      r= 0.5f + gP/(Q-gP);

      bits= Float.floatToIntBits(r);
      exp= ((bits>>23)&0xFF);
      if ( exp == 0 )
      {
         exp= -126;
      }
      else
      {
         exp -= 0x7F;
      }
      exp += (N+1);
      bits= ((bits & 0x7FFFFF)|((exp+0x7F)<<23));

      result= Float.intBitsToFloat( bits );                 

      return result;
   }
}
