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
 

package com.tvworks.plateval.memory;

import com.tvworks.plateval.TestCaseSet;
import com.tvworks.plateval.framework.TestCase;
import com.tvworks.plateval.util.ResultLog;

import com.tvworks.plateval.format.PlatformInformationRepository;

public class AllocateLargestByteArray implements TestCase
{
   public static void touch()
   {      
   }

   public void run()
   {
      long startTime, endTime, runTime;
      long totalMemory, freeMemory;
      int trySize, nextTrySize, maxAllocSize, maxPossibleSize;
      int l,h,gap;
      int loopCount;
      byte[] data;

      java.lang.System.gc();
      try
      {
         Thread.sleep(1000L);
      }
      catch( InterruptedException ie )
      {         
      }
      totalMemory= java.lang.Runtime.getRuntime().totalMemory();
      freeMemory= java.lang.Runtime.getRuntime().freeMemory();

      if ( freeMemory > Integer.MAX_VALUE )
      {
         maxPossibleSize= Integer.MAX_VALUE;
      }
      else
      {
         maxPossibleSize= (int)freeMemory;
      }

      loopCount= 0;
      maxAllocSize= 0;
      trySize= maxPossibleSize/2;
      
      startTime= System.currentTimeMillis();
      for( ;; )
      {
         ++loopCount;
         try
         {
            data= new byte[trySize];
      
            if ( data.length > maxAllocSize )
            {
               maxAllocSize= data.length;
            }
            l= data.length;
            h= maxPossibleSize;
         }
         catch( OutOfMemoryError ome )
         {
            l= maxAllocSize;
            h= trySize;
         }
         
         data= null;
         nextTrySize= (l+h)/2;
         gap= ((nextTrySize > trySize) ? (nextTrySize-trySize) : (trySize-nextTrySize));
         if ( (gap < 2) || (loopCount > 10000) )
         {
            break;
         }
         trySize= nextTrySize;
         
         Thread.yield();
      }      
      endTime= System.currentTimeMillis();
      
      runTime= endTime-startTime;
      
      ResultLog.getInstance().add( TestCaseSet.TEST_AllocateLargestByteArray, loopCount, maxAllocSize, runTime );      
   }
   
   public static void format( PlatformInformationRepository pir, StringBuffer sb, int data1, int data2, long data3 )
   {
      sb.append( "  Largest block size of "+data2+" bytes determined after "+data1+" allocations taking "+data3+" ms \r\n" );
   }
}
