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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;

import com.tvworks.plateval.PlatEval;
import com.tvworks.plateval.TestCaseSet;
import com.tvworks.plateval.framework.TestCase;
import com.tvworks.plateval.util.ResultLog;

import com.tvworks.plateval.format.PlatformInformationRepository;

public class FontRenderClip implements TestCase
{
   public static void touch()
   {      
   }
   
   public void run()
   {
      try
      {
         Thread.sleep(2000L);
      }
      catch( InterruptedException ie )
      {         
      }

      doRunClip( 0, 0, 0, 0, 0 );
      doRunClip( 1, 240, 180, 160, 120 );
      doRunClip( 2, 160, 120, 320, 240 );      
      doRunClip( 3, 80, 60, 480, 360 );      
      doRunClip( 4, 0, 0, 640, 480 );      
   }

   public void doRunClip( int id, int x, int y, int width, int height )
   {
      long runTime, totalRunTime;
      int count, totalCount, loopCount;
      int pointMin, pointMax, point;
      Component c;
      Graphics g;
      Rectangle clip;
      
      c= PlatEval.getComponent();
      g= c.getGraphics();
      pointMin= 12;
      pointMax= 32;
      mTotalFontCreationTime= 0;
      mNextDistinctGlyphHeight= 0;
         
      com.tvworks.plateval.util.Graphics.blendOff(g);

      clip= new Rectangle( x, y, width, height );
      
      loopCount= 0;
      totalCount= 0;
      totalRunTime= 0;
      count= 10;
      point= pointMin;
      for(;;)
      {
         ++loopCount;
         runTime= doRun(count, g, clip, c, point);
         totalRunTime += runTime;
         totalCount += mTotalChars;
         point += 2;
         if ( point > pointMax )
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
      
      ResultLog.getInstance().add( TestCaseSet.TEST_FontRenderClip, id, totalCount, totalRunTime );
   }
   
   
   private static char mTestChar[]=
   {
      0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,   
      0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f,   
      0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f,   
      0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x5b, 0x5c, 0x5d, 0x5e, 0x5f,   
      0x60, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6a, 0x6b, 0x6c, 0x6d, 0x6e, 0x6f,   
      0x70, 0x71, 0x72, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a, 0x7b, 0x7c, 0x7d, 0x7e, 0x7f,   
      0xa0, 0xa1, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xab, 0xac, 0xad, 0xae, 0xaf,   
      0xb0, 0xb1, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xbb, 0xbc, 0xbd, 0xbe, 0xbf,   
      0xc0, 0xc1, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xcb, 0xcc, 0xcd, 0xce, 0xcf,   
   };

   private long mTotalFontCreationTime;
   private int mTotalChars;
   private int mNextDistinctGlyphHeight;
   private int mGlyphHeights[]= new int[40];
   public long doRun(int count, Graphics g, Rectangle clip, Component c, int point )
   {
      long startTime, endTime, runTime;
      Toolkit toolkit;
      Font font;
      FontMetrics fm;
      Dimension dimScreen;
      int lineHeight, cIndex, cIndexMax, charCount;
      int i, x, y, w, h;

      toolkit= Toolkit.getDefaultToolkit();
      dimScreen= PlatEval.getScreenSize();

      startTime= System.currentTimeMillis();
      font= new Font( "Tiresias", Font.PLAIN, point );
      fm= c.getFontMetrics(font);
      lineHeight= fm.getHeight();
      g.setFont(font);
      endTime= System.currentTimeMillis();
      mTotalFontCreationTime += (endTime-startTime);

      cIndex= 0;
      cIndexMax= mTestChar.length;
      w= dimScreen.width;
      h= dimScreen.height;
      charCount= 0;
      
      for( i= 0; i < mNextDistinctGlyphHeight; ++i )
      {
        if ( lineHeight == mGlyphHeights[i] )
        {
           break;
        }
      }
      if ( i == mNextDistinctGlyphHeight )
      {
         mGlyphHeights[i]= lineHeight;
         ++mNextDistinctGlyphHeight;
      }
      
      g.setClip( clip.x, clip.y, clip.width, clip.height );

      runTime= 0;
      while( count-- > 0 )
      {
         g.setColor( Color.black );
         g.fillRect(0,0,w,h);
         g.setColor( Color.white );
         startTime= System.currentTimeMillis();
         y= lineHeight;
         while( y < h )
         {
            x= 0;
            while( x < w )
            {
               g.drawChars( mTestChar, cIndex, 1, x, y );
               x += fm.charWidth(mTestChar[cIndex]);
               ++cIndex;
               if ( cIndex >= cIndexMax ) cIndex= 0;
               ++charCount;
            }
            y += lineHeight;
         }
         endTime= System.currentTimeMillis();

         toolkit.sync();
         runTime += endTime-startTime;
      }
      mTotalChars= charCount;
      
      return runTime;
   }

   public static void format( PlatformInformationRepository pir, StringBuffer sb, int data1, int data2, long data3 )
   {
      long operationCost;
      
      switch( data1 )
      {
         case 0:
            sb.append( "  total glyphs (fullclip) (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  glyph draw cost: "+ operationCost+" ns\r\n" );
            break;
         case 1:
            sb.append( "  total glyphs (threequarterclip) (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  glyph draw cost: "+ operationCost+" ns\r\n" );
            break;
         case 2:
            sb.append( "  total glyphs (halfclip) (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  glyph draw cost: "+ operationCost+" ns\r\n" );
            break;
         case 3:
            sb.append( "  total glyphs (quarterclip) (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  glyph draw cost: "+ operationCost+" ns\r\n" );
            break;
         case 4:
            sb.append( "  total glyphs (noclip) (SRC) ="+data2+" total time="+data3+" ms\r\n" );            
            operationCost= (data3*1000000)/data2;            
            sb.append( "  glyph draw cost: "+ operationCost+" ns\r\n" );
            break;
      }
   }
}
