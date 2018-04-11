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
 

package com.tvworks.plateval.util;

public class IndexMapper
{
   private static int s1[]= 
   {
      25, 4, 11, 3, 17, 16, 8, 2, 7, 26, 6, 13, 5, 23, 22, 19, 9, 15, 1, 10, 12, 14, 0, 24, 18, 20, 21, 27
   };
   private static int s2[]=
   {
      17, 11, 22, 27, 0, 15, 8, 2, 1, 3, 21, 23, 24, 9, 25, 6, 13, 10, 14, 7, 16, 4, 5, 19, 20, 18, 12, 26      
   };
   private static int s3[]=
   {
      23, 16, 12, 20, 26, 13, 21, 10, 3, 22, 11, 8, 24, 19, 14, 0, 27, 25, 4, 2, 6, 17, 9, 5, 15, 1, 7, 18
   };
   private static int s4[]=
   {
      13, 9, 16, 3, 8, 10, 21, 0, 20, 12, 17, 22, 27, 1, 6, 24, 14, 18, 5, 19, 4, 7, 26, 2, 15, 11, 23, 25
   };
   private static int s5[]=
   {
      26, 10, 23, 20, 22, 13, 8, 18, 16, 17, 21, 11, 14, 12, 25, 9, 2, 19, 27, 5, 24, 1, 6, 3, 15, 7, 0, 4
   };
   private static int s6[]=
   {
      20, 22, 21, 23, 27, 15, 18, 8, 16, 10, 11, 1, 4, 7, 5, 19, 25, 2, 9, 17, 3, 6, 14, 26, 0, 24, 12, 13
   };
   private static int s7[]=
   {
      13, 16, 19, 7, 9, 23, 20, 24, 11, 2, 27, 10, 6, 8, 26, 17, 25, 0, 18, 5, 14, 22, 21, 4, 15, 12, 3, 1
   };
   private static int s8[]=
   {
      10, 13, 24, 8, 19, 21, 26, 3, 16, 2, 22, 1, 27, 5, 7, 23, 0, 20, 17, 25, 9, 15, 11, 6, 4, 12, 18, 14
   };
   private static int s9[]=
   {
      6, 27, 14, 16, 17, 1, 12, 9, 19, 23, 10, 24, 18, 5, 13, 8, 15, 4, 22, 7, 2, 20, 25, 3, 21, 0, 11, 26
   };
   private static int s10[]=
   {
      8, 12, 18, 9, 21, 23, 6, 20, 25, 26, 2, 15, 27, 7, 19, 17, 0, 14, 11, 10, 1, 13, 4, 3, 24, 16, 22, 5
   };
   private static int s11[]=
   {
      25, 23, 3, 1, 12, 20, 4, 9, 16, 2, 11, 17, 21, 26, 8, 5, 18, 19, 24, 13, 15, 22, 14, 0, 6, 7, 10, 27
   };
      
   private static Object sets[]= { s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11 };
   
   public static int getIndex( int set, int i )
   {
      int s[];
      
      s= (int[])sets[(set%sets.length)];
      
      return s[i];
   }
}
