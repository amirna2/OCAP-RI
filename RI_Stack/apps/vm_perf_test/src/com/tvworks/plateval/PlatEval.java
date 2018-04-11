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
 

package com.tvworks.plateval;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.havi.ui.HScene;
import org.ocap.ui.event.OCRcEvent;

import com.tvworks.plateval.TestCaseSet;
import com.tvworks.plateval.framework.TestCase;
import com.tvworks.plateval.util.ResultLog;

public class PlatEval extends Component implements Runnable, Xlet
{
   static private Component mComp;
   static private Dimension mDimScreen;
   
   private XletContext mCtx;
   private Container mRootContainer;
   private Toolkit mToolkit;
   private Font mFont;
   private FontMetrics mFontMetrics;
   private int mXMargin;
   private int mYMargin;
   private String statusMessages[];
   private boolean mRunning;
   private boolean mNeedRefresh;
   private Thread mThread;
   private Object mLock;
   private TestCaseSet mTestCaseSet;
   private long mRunStartTime;
   private long mRunTotalTime;

   public static Component getComponent()
   {
      return mComp;
   }
   
   /*
    * Initialize data
    */
   public void initXlet( XletContext ctx ) 
   {
      mCtx= ctx;
      enableEvents( KeyEvent.KEY_EVENT_MASK |  FocusEvent.FOCUS_EVENT_MASK );
   }

   public void pauseXlet()
   {
      mRootContainer= null;
      if ( mThread != null )
      {
         mRunning= false;
         try
         {
            mThread.join();
         }
         catch( InterruptedException ie )
         {            
         }
      }
      
   }

   /*
    * Set up the container for the Xlet and get focus for it.
    */
   public void startXlet()
   {
      mDimScreen= new Dimension( 640, 480 );

      if ( mRootContainer == null )
      {
         mRootContainer = javax.tv.graphics.TVContainer.getRootContainer( mCtx );
         java.awt.Rectangle r= mRootContainer.getBounds();
         mDimScreen.width= r.width;
         mDimScreen.height= r.height;
         setSize( mDimScreen.width, mDimScreen.height );
         setVisible(false);
         mRootContainer.add(this,-1);
         com.tvworks.plateval.util.Graphics.show(mRootContainer);
         setVisible(true);
         requestFocus();
         
         mComp= this;
         
         mToolkit= Toolkit.getDefaultToolkit();
         
         mFont= new Font( "Tiresias", Font.PLAIN, 18 );
         mFontMetrics= getFontMetrics(mFont);
 
         mXMargin= mDimScreen.width/10;
         mYMargin= 3*mDimScreen.height/20;
         int lineHeight= mFontMetrics.getHeight();
         int lineCount= ((mDimScreen.height-mYMargin*2)/lineHeight);
         statusMessages= new String[lineCount-1];
         
         statusMsg("Press 1 to start all tests");
         statusMsg("Press 2 to start test subset A");
         
         mLock= new Object();
         mThread= new Thread( this );
         mThread.setPriority( Thread.MAX_PRIORITY );
         mThread.start();
      }
   }

   public void destroyXlet( boolean unconditional )
   {
   }
   
   public static Dimension getScreenSize()
   {
      return new Dimension( mDimScreen.width, mDimScreen.height );
   }
   
   public void paint(Graphics g) 
   {
      mNeedRefresh= true;
   }
   
   public synchronized void drawStatusScreen(Graphics g)
   {
      int x, y, ymargin, xmargin, lineHeight;
      String text;
      
      if ( mFont == null ) return;

      g.setColor( Color.black );
      g.fillRect( 0, 0, mDimScreen.width, mDimScreen.height );

      g.setFont( mFont );
      lineHeight= mFontMetrics.getHeight();  
      g.setColor( Color.white );

      xmargin= mXMargin;
      ymargin= mYMargin;
     
      text= "PlatEval";
      x= xmargin+((mDimScreen.width-2*xmargin)-mFontMetrics.stringWidth(text))/2;
      y= ymargin;
      g.drawString( text, x, y );

      if ( statusMessages != null )
      {
         x= xmargin;
         for( int i= 0; i < statusMessages.length; ++i )
         {
            y  += lineHeight;      
            text= statusMessages[i];
            if ( text != null )
            {
               g.drawString( text, x, y );
            }
         }
      }
   }
   
   public void statusMsg( String msg )
   {
      int i;
      
      for( i= 1; i < statusMessages.length; ++i )
      {
         statusMessages[i-1]= statusMessages[i];
      }
      statusMessages[i-1]= msg;

      mNeedRefresh= true;

      System.out.println( msg );
   }
   
   protected void processKeyEvent(KeyEvent e)
   {
      switch( e.getID() )
      {
         case KeyEvent.KEY_PRESSED:
            processKeyPressed( e );
            break;

         case KeyEvent.KEY_TYPED:
            break;

         case KeyEvent.KEY_RELEASED:
            break;
            
         default:
            break;
      }
   }

   private void processKeyPressed( KeyEvent e )
   {
      switch( e.getKeyCode() )
      {
         case OCRcEvent.VK_1:
            runTests(0);
            break;
         case OCRcEvent.VK_2:
            runTests(1);
            break;
      }
   }
   
   private void runTests( int set )
   {
      synchronized( mLock )
      {
         if ( mTestCaseSet == null )
         {
            mRunStartTime= System.currentTimeMillis();
       
            switch( set )
            {
               case 1:
                  statusMsg("starting tests (subset"+set+")...");                  
                  mTestCaseSet= new TestCaseSet(set);
                  break;
               default:
                  statusMsg("starting tests (full)...");
                  mTestCaseSet= new TestCaseSet();
                  break;
            }
         }         
      }
   }
   
   public void run()
   {
      TestCase tc= null;
      boolean finishedTests= false;
      
      mRunning= true;
      
      try
      {
         while( mRunning )
         {
            synchronized( mLock )
            {
               if ( mTestCaseSet != null  )
               {
                  tc= mTestCaseSet.getNext();
                  if ( tc == null )
                  {
                     finishedTests= true;
                     mTestCaseSet= null;
                  }
                  else
                  {
                     statusMsg( "starting test "+mTestCaseSet.currentTestIndex()+" of "+mTestCaseSet.numberOfTests() );
                  }
               }
            }
            
            if ( mNeedRefresh )
            {
               mNeedRefresh= false;
   
               drawStatusScreen(getGraphics());
   
               mToolkit.sync();           
            }
            
            if ( tc != null )
            {
               try
               {
                  tc.run();
               }
               catch( Throwable t )
               {               
                  System.out.println("Error running test "+mTestCaseSet.currentTestIndex()+": "+t.getMessage() );
                  t.printStackTrace();
               }
            }
            
            if ( finishedTests )
            {
               mRunTotalTime= System.currentTimeMillis()-mRunStartTime;
               
               finishedTests= false;
               
               Thread t= new Thread(
                                      new Runnable()
                                      {
                                         public void run()
                                         {
                                            statusMsg("done tests, run time= "+mRunTotalTime+" ms" );
                                            statusMsg("dumping results...");
                                            
                                            ResultLog.getInstance().dump();
                                            ResultLog.getInstance().flush();
   
                                            statusMsg("Press 1 to start all tests");
                                            statusMsg("Press 2 to start test subset A");
                                         }
                                      }
                                   );
               t.start();
            }
            
            try
            {
               Thread.sleep( 30L );
            }
            catch( InterruptedException ie )
            {            
            }
         }
      }
      catch( Throwable t )
      {
         System.out.println("Error in main test running thread: "+t.getMessage());
         t.printStackTrace();
      }
   }
   
   
   /* Uncomment the following block to allow running PlatEval on J2SE.  
    * a) uncomment out Test inner class
    * b) add JRE to project
    * c) be sure to run dounsigned.bat to repair CaffeineMark classes
    */
   /*   
   public static void main( String args[] )
   {
      Test test= new Test();
      test.setSize( 640, 480 );
   }

   private static class Test extends java.awt.Frame implements Runnable
   {
      private Toolkit mToolkit;
      private Font mFont;
      private FontMetrics mFontMetrics;
      private int mXMargin;
      private int mYMargin;
      private String statusMessages[];
      private boolean mRunning;
      private boolean mNeedRefresh;
      private Thread mThread;
      private Object mLock;
      private TestCaseSet mTestCaseSet;
      private long mRunStartTime;
      private long mRunTotalTime;

      public Test()
      {
         enableEvents( KeyEvent.KEY_EVENT_MASK |  FocusEvent.FOCUS_EVENT_MASK | java.awt.AWTEvent.WINDOW_EVENT_MASK );
         setVisible(true);
         requestFocus();
         
         mComp= this;
         
         mToolkit= Toolkit.getDefaultToolkit();
         
         mFont= new Font( "Tiresias", Font.PLAIN, 18 );
         mFontMetrics= getFontMetrics(mFont);
 
         mDimScreen= new Dimension( 640, 480 );
         mXMargin= mDimScreen.width/10;
         mYMargin= 3*mDimScreen.height/20;
         int lineHeight= mFontMetrics.getHeight();
         int lineCount= ((mDimScreen.height-mYMargin*2)/lineHeight);
         statusMessages= new String[lineCount-1];
         
         statusMsg("Press 1 to start tests");
         
         mLock= new Object();
         mThread= new Thread( this );
         mThread.setPriority( Thread.MAX_PRIORITY );
         mThread.start();
      }

      protected void processWindowEvent( java.awt.event.WindowEvent we )
      {
         int id= we.getID();

         switch( id)
         {
            case java.awt.event.WindowEvent.WINDOW_CLOSING:
               System.exit(0);
               break;
         }
      }

      protected void processKeyEvent(KeyEvent e)
      {
         switch( e.getID() )
         {
            case KeyEvent.KEY_PRESSED:
               processKeyPressed( e );
               break;

            case KeyEvent.KEY_TYPED:
               break;

            case KeyEvent.KEY_RELEASED:
               break;
               
            default:
               break;
         }
      }

      private void processKeyPressed( KeyEvent e )
      {
         switch( e.getKeyCode() )
         {
            case OCRcEvent.VK_1:
               runTests();
               break;
         }
      }
      
      private void runTests()
      {
         synchronized( mLock )
         {
            if ( mTestCaseSet == null )
            {
               mRunStartTime= System.currentTimeMillis();
               
               statusMsg("starting tests...");
               
               mTestCaseSet= new TestCaseSet();
            }         
         }
      }
      
      public void run()
      {
         TestCase tc= null;
         boolean finishedTests= false;
         
         mRunning= true;
         
         try
         {
            while( mRunning )
            {
               synchronized( mLock )
               {
                  if ( mTestCaseSet != null  )
                  {
                     tc= mTestCaseSet.getNext();
                     if ( tc == null )
                     {
                        finishedTests= true;
                        mTestCaseSet= null;
                     }
                     else
                     {
                        statusMsg( "starting test "+mTestCaseSet.currentTestIndex()+" of "+mTestCaseSet.numberOfTests() );
                     }
                  }
               }
               
               if ( mNeedRefresh )
               {
                  mNeedRefresh= false;
      
                  drawStatusScreen(getGraphics());
      
                  mToolkit.sync();           
               }
               
               if ( tc != null )
               {
                  try
                  {
                     tc.run();
                  }
                  catch( Throwable t )
                  {               
                     System.out.println("Error running test "+mTestCaseSet.currentTestIndex()+": "+t.getMessage() );
                     t.printStackTrace();
                  }
               }
               
               if ( finishedTests )
               {
                  mRunTotalTime= System.currentTimeMillis()-mRunStartTime;
                  
                  finishedTests= false;
                  
                  Thread t= new Thread(
                                         new Runnable()
                                         {
                                            public void run()
                                            {
                                               statusMsg("done tests, run time= "+mRunTotalTime+" ms" );
                                               statusMsg("dumping results...");
                                               
                                               ResultLog.getInstance().dump();
                                               ResultLog.getInstance().flush();
      
                                               statusMsg("Press 1 to start tests");                                                  
                                            }
                                         }
                                      );
                  t.start();
               }
               
               try
               {
                  Thread.sleep( 30L );
               }
               catch( InterruptedException ie )
               {            
               }
            }
         }
         catch( Throwable t )
         {
            System.out.println("Error in main test running thread: "+t.getMessage());
            t.printStackTrace();
         }
      }
      public void paint(Graphics g) 
      {
         mNeedRefresh= true;
      }
      
      public synchronized void drawStatusScreen(Graphics g)
      {
         int x, y, ymargin, xmargin, lineHeight;
         String text;
         
         if ( mFont == null ) return;

         g.setColor( Color.black );
         g.fillRect( 0, 0, mDimScreen.width, mDimScreen.height );

         g.setFont( mFont );
         lineHeight= mFontMetrics.getHeight();  
         g.setColor( Color.white );

         xmargin= mXMargin;
         ymargin= mYMargin;
        
         text= "PlatEval";
         x= xmargin+((mDimScreen.width-2*xmargin)-mFontMetrics.stringWidth(text))/2;
         y= ymargin;
         g.drawString( text, x, y );

         if ( statusMessages != null )
         {
            x= xmargin;
            for( int i= 0; i < statusMessages.length; ++i )
            {
               y  += lineHeight;      
               text= statusMessages[i];
               if ( text != null )
               {
                  g.drawString( text, x, y );
               }
            }
         }
      }
      
      public void statusMsg( String msg )
      {
         int i;
         
         for( i= 1; i < statusMessages.length; ++i )
         {
            statusMessages[i-1]= statusMessages[i];
         }
         statusMessages[i-1]= msg;

         mNeedRefresh= true;

         System.out.println( msg );
      }
   }
   */
}
