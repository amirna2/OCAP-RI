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

public class FullscreenFillClip implements TestCase
{
   public static void touch()
   {      
   }
   
   public void run()
   {
      Image img;
      Toolkit toolkit= Toolkit.getDefaultToolkit();
      Color color[];
      
      try
      {
         Thread.sleep(2000L);
      }
      catch( InterruptedException ie )
      {         
      }

      color= new Color[12];
      color[0]= Color.yellow;
      color[1]= Color.blue;
      color[2]= Color.cyan;
      color[3]= Color.darkGray;
      color[4]= Color.gray;
      color[5]= Color.green;
      color[6]= Color.lightGray;
      color[7]= Color.magenta;
      color[8]= Color.orange;
      color[9]= Color.pink;
      color[10]= Color.red;
      color[11]= Color.white;
      
      doRunClip( 0, color, 0, 0, 0, 0 );
      doRunClip( 2, color, 240, 180, 160, 120 );
      doRunClip( 4, color, 160, 120, 320, 240 );      
      doRunClip( 6, color, 80, 60, 480, 360 );      
      doRunClip( 8, color, 0, 0, 640, 480 );      
   }

   private void doRunClip( int id, Color color[], int x, int y, int width, int height )
   {
      long runTime, totalRunTime;
      int count, totalCount, loopCount;
      Component c;
      Graphics g;
      
      c= PlatEval.getComponent();
      g= c.getGraphics();

      com.tvworks.plateval.util.Graphics.blendOff(g);

      g.setClip( x, y, width, height );
      
      loopCount= 0;
      totalCount= 0;
      totalRunTime= 0;
      mTotalSyncTime= 0;
      count= 20;
      for(;;)
      {
         ++loopCount;         
         runTime= doRun(count, g, color);
         totalRunTime += runTime;
         totalCount += count;
         if ( totalRunTime > 20000 )
         {
            break;
         }
         if ( totalCount > 1000 )
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

      ResultLog.getInstance().add( TestCaseSet.TEST_FullscreenFillClip, id,   totalCount, mTotalSyncTime );
      ResultLog.getInstance().add( TestCaseSet.TEST_FullscreenFillClip, id+1, totalCount, totalRunTime );
   }
   
   private int mTotalSyncTime;
   public long doRun(int count, Graphics g, Color c[] )
   {
      long startTime, endTime, runTime;
      long syncStartTime, syncTime;
      int w, h, i, imax;
      Toolkit toolkit;
      Dimension dimScreen;

      toolkit= Toolkit.getDefaultToolkit();
      dimScreen= PlatEval.getScreenSize();
      w= dimScreen.width;
      h= dimScreen.height;
      imax= c.length;
      
      i= 0;
      syncTime= 0;
      startTime= System.currentTimeMillis();
      while( count-- > 0 )
      {
         g.setColor(c[i]);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         g.fillRect(0,0,w,h);
         syncStartTime= System.currentTimeMillis();
         toolkit.sync();
         syncTime += (System.currentTimeMillis()-syncStartTime);
         if ( ++i >= imax ) i= 0;
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
            sb.append( "  total fullscreen/fullclip syncs (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  sync cost: "+ operationCost+" ns\r\n" );
            break;
            
         case 1:
            sb.append( "  total fullscreen/fullclip fills (SRC) ="+totalBlits+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/totalBlits;            
            sb.append( "  fill cost: "+ operationCost+" ns\r\n" );
            break;

         case 2:
            sb.append( "  total fullscreen/threequarterclip syncs (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  sync cost: "+ operationCost+" ns\r\n" );
            break;
            
         case 3:
            sb.append( "  total fullscreen/threequarterclip image fills (SRC) ="+totalBlits+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/totalBlits;            
            sb.append( "  fill cost: "+ operationCost+" ns\r\n" );
            break;

         case 4:
            sb.append( "  total fullscreen/halfclip syncs (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  sync cost: "+ operationCost+" ns\r\n" );
            break;
            
         case 5:
            sb.append( "  total fullscreen/halfclip image fills (SRC) ="+totalBlits+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/totalBlits;            
            sb.append( "  fill cost: "+ operationCost+" ns\r\n" );
            break;

         case 6:
            sb.append( "  total fullscreen/quarterclip syncs (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  sync cost: "+ operationCost+" ns\r\n" );
            break;
            
         case 7:
            sb.append( "  total fullscreen/quarterclip fills (SRC) ="+totalBlits+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/totalBlits;            
            sb.append( "  fill cost: "+ operationCost+" ns\r\n" );
            break;

         case 8:
            sb.append( "  total fullscreen/noclip syncs (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  sync cost: "+ operationCost+" ns\r\n" );
            break;
            
         case 9:
            sb.append( "  total fullscreen/noclip fills (SRC) ="+totalBlits+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/totalBlits;            
            sb.append( "  fill cost: "+ operationCost+" ns\r\n" );
            break;
      }
   }
}
