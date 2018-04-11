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
 

package com.tvworks.plateval.graphics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;

import com.tvworks.plateval.PlatEval;
import com.tvworks.plateval.TestCaseSet;
import com.tvworks.plateval.framework.TestCase;
import com.tvworks.plateval.util.ResultLog;

import com.tvworks.plateval.format.PlatformInformationRepository;

public class FullscreenBlit implements TestCase
{
   public static void touch()
   {      
   }
   
   public void run()
   {
      long runTime, totalRunTime;
      int count, totalCount, loopCount;
      Component c;
      Graphics g;
      Image img1, img2;
      Toolkit toolkit= Toolkit.getDefaultToolkit();
      
      try
      {
         Thread.sleep(2000L);
      }
      catch( InterruptedException ie )
      {         
      }
      
      // ---------------------------------------------
      // TVNav / OCAP 
      
      img1= toolkit.createImage( "images/fs1.png" );
      img2= toolkit.createImage( "images/fs2.png" );
      
      // ---------------------------------------------
      
      // ---------------------------------------------
      // Zodiac
      /*
      try
      {
         img1= toolkit.createImage( new java.net.URL("resource:///images/fs1.png") );               
         img2= toolkit.createImage( new java.net.URL("resource:///images/fs2.png") );               
      }
      catch( java.io.IOException ioe )
      {
         System.out.println("Error: exception creating image from URL" );
         ioe.printStackTrace();
         return;
      }
      */
      // ---------------------------------------------
      
      com.tvworks.plateval.util.Image.loadImageSynchronous(img1);
      com.tvworks.plateval.util.Image.loadImageSynchronous(img2);
            
      c= PlatEval.getComponent();
      g= c.getGraphics();

      com.tvworks.plateval.util.Graphics.blendOff(g);
      
      loopCount= 0;
      totalCount= 0;
      totalRunTime= 0;
      mTotalSyncTime= 0;
      count= 20;
      for(;;)
      {
         ++loopCount;         
         runTime= doRun(count, g, img2, img1);
         totalRunTime += runTime;
         totalCount += count;
         if ( totalRunTime > 20000 )
         {
            break;
         }

         try
         {
            Thread.sleep(100L);
         }
         catch( InterruptedException ie )
         {            
         }
      }
      
      ResultLog.getInstance().add( TestCaseSet.TEST_FullscreenBlit, 0, totalCount, mTotalSyncTime );
      ResultLog.getInstance().add( TestCaseSet.TEST_FullscreenBlit, 1, totalCount, totalRunTime );

      com.tvworks.plateval.util.Graphics.blendOn(g);
      
      loopCount= 0;
      totalCount= 0;
      totalRunTime= 0;
      mTotalSyncTime= 0;
      count= 20;
      for(;;)
      {
         ++loopCount;         
         runTime= doRun(count, g, img1, img2);
         totalRunTime += runTime;
         totalCount += count;
         if ( totalRunTime > 20000 )
         {
            break;
         }

         try
         {
            Thread.sleep(100L);
         }
         catch( InterruptedException ie )
         {            
         }
      }
      
      ResultLog.getInstance().add( TestCaseSet.TEST_FullscreenBlit, 2, totalCount, mTotalSyncTime );
      ResultLog.getInstance().add( TestCaseSet.TEST_FullscreenBlit, 3, totalCount, totalRunTime );
   }

   private int mTotalSyncTime;
   public long doRun(int count, Graphics g, Image img1, Image img2 )
   {
      long startTime, endTime, runTime;
      long syncStartTime, syncTime;
      Toolkit toolkit;

      toolkit= Toolkit.getDefaultToolkit();
      
      syncTime= 0;
      startTime= System.currentTimeMillis();
      while( count-- > 0 )
      {
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         g.drawImage(img1, 0, 0, null );
         g.drawImage(img2, 0, 0, null );
         syncStartTime= System.currentTimeMillis();
         toolkit.sync();
         syncTime += (System.currentTimeMillis()-syncStartTime);
      }
      endTime= System.currentTimeMillis();
      runTime= endTime-startTime;
      mTotalSyncTime += syncTime;
      
      return runTime;
   }
   
   public static void format( PlatformInformationRepository pir, StringBuffer sb, int data1, int data2, long data3 )
   {
      long operationCost;
      int totalBlits;
      
      totalBlits= data2*100;
      switch( data1 )
      {
         case 0:
            sb.append( "  total fullscreen syncs (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  fullscreen sync cost: "+ operationCost+" ns\r\n" );
            break;
            
         case 1:
            sb.append( "  total fullscreen image blits (SRC) ="+totalBlits+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/totalBlits;            
            sb.append( "  fullscreen blit cost: "+ operationCost+" ns\r\n" );
            break;

         case 2:
            sb.append( "  total fullscreen syncs (SRC_OVER) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  fullscreen sync cost: "+ operationCost+" ns\r\n" );
            break;
            
         case 3:
            sb.append( "  total fullscreen image blits (SRC_OVER) ="+totalBlits+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/totalBlits;            
            sb.append( "  fullscreen blit cost: "+ operationCost+" ns\r\n" );
            break;
      }
   }
}
