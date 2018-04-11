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
 

package com.tvworks.plateval.format;

import java.io.*;
import com.tvworks.plateval.caffeinemark.*;
import com.tvworks.plateval.fundamental.*;
import com.tvworks.plateval.graphics.*;
import com.tvworks.plateval.memory.*;
import com.tvworks.plateval.vq.*;
import com.tvworks.plateval.util.IndexMapper;
import com.tvworks.plateval.TestCaseSet;


public class PEFormatter implements PlatformInformationRepository
{
   public static void main( String args[] )
   {
      System.out.println("PEFormatter: v1.0: Formatter for PlatEval Xlet output");
      if ( args.length != 2 )
      {
         System.out.println( "USAGE: PEFormatter <infile> <outfile>" );
         System.out.println( "where:" );
         System.out.println( "  <infile> is the name of a file containing PlatEval xlet output" );
         System.out.println( "  <outfile> is the name of the file where the formatted output should be written" );
         return;
      }
      
      PEFormatter pef= new PEFormatter();
      
      pef.format( args[0], args[1] );
   }
   
   private long loopOverhead= -1;
   public void setLoopOverhead( long nanoseconds )
   {
      loopOverhead= nanoseconds;   
   }
   
   public long getLoopOverhead()
   {
      return loopOverhead;
   }
   
   private long shallowInstantiationOverhead= -1;
   public void setShallowInstantiationOverhead( long nanoseconds)
   {
      shallowInstantiationOverhead= nanoseconds;
   }
   
   public long getShallowInstantiationOverhead()
   {
      return shallowInstantiationOverhead;
   }
   
   private long localWriteCost= -1;
   public void setLocalWriteCost( long nanoseconds)
   {
      localWriteCost= nanoseconds;
   }
   
   public long getLocalWriteCost()
   {
      return localWriteCost;
   }

   private long threadYieldCost= -1;
   public void setThreadYieldCost( long nanoseconds )
   {
      threadYieldCost= nanoseconds;
   }
   
   public long getThreadYieldCost()
   {      
      return threadYieldCost;
   }
   
   
   private PEFormatter()
   {      
   }
   
   private void format( String inFileName, String outFileName )
   {
      FileInputStream fisIn= null;
      FileOutputStream fosOut= null;

      File inFile= new File( inFileName );
      try
      {
         fisIn= new FileInputStream( inFile );
      }
      catch( FileNotFoundException fnfe )
      {
         System.out.println("Error: unable to open input file ("+inFileName+")" );
      }

      if ( fisIn != null )
      {
         File outFile= new File( outFileName );
         try
         {
            fosOut= new FileOutputStream( outFile );
         }
         catch( FileNotFoundException fnfe )
         {
            System.out.println("Error: unable to open output file ("+outFileName+")" );
         }
      }

      if ( (fisIn != null) && (fosOut != null) )
      {
         byte[] line;
         
         for( ; ; )
         {
            line= getLine( fisIn );
            
            if ( line == null )
            {
               break;
            }

            String s= formatLine( line );            

            try
            {
               fosOut.write( s.getBytes() );
            }
            catch( IOException ioe )
            {               
            }
         }
      }
      
      if ( fisIn != null )
      {
         try
         {
            fisIn.close();
         }
         catch( IOException ioe )
         {            
         }
      }

      if ( fosOut != null )
      {
         try
         {
            fosOut.close();
         }
         catch( IOException ioe )
         {            
         }
      }
   }
   
   private int lineNumber= 0;
   private String formatLine( byte line[] )
   {
      byte[] binline= null;
      long recordTime;
      int recordId, data1, data2;
      long data3;      
      StringBuffer sb= new StringBuffer();      

      if ( (line[0] == '0') && (line[1] == 'x') )
      {            
         binline= new byte[line.length/2-1];
         int b, j;
         for( j= 2; j < line.length; j += 2 )
         {
            b= getByteFromASCIIHex(work,IndexMapper.getIndex(lineNumber,j/2-1)*2+2);
            binline[j/2-1]= (byte)(b&0xFF);
         }
         ++lineNumber;
         
         recordTime= getLong( binline, 0 );
         recordId= getInt( binline, 8 );
         data1= getInt( binline, 12 );
         data2= getInt( binline, 16 );
         data3= getLong( binline, 20);
         
         java.util.Date startDate= new java.util.Date(recordTime);
         String testName= com.tvworks.plateval.TestCaseSet.getName(recordId);
         
         sb.append( "Test " );
         sb.append( testName );
         sb.append( " started at " );
         sb.append( startDate.toString() );
         sb.append( ":\r\n" );

         switch( recordId )
         {
            case TestCaseSet.TEST_StaticClassLoading:
               StaticClassLoading.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_EmptyLoop:
               EmptyLoop.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_TotalAndFreeMemory:
               TotalAndFreeMemory.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_VirtualMethodInvocation:
               VirtualMethodInvocation.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_NonVirtualMethodInvocation:
               NonVirtualMethodInvocation.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_InterfaceMethodInvocation:
               InterfaceMethodInvocation.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_StaticMethodInvocation:
               StaticMethodInvocation.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_ObjectInstantiationShallow:
               ObjectInstantiationShallow.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_ObjectInstantiationDeep:
               ObjectInstantiationDeep.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_LocalVariableWrite:
               LocalVariableWrite.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_LocalVariableRead:
               LocalVariableRead.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_InstanceVariableWrite:
               InstanceVariableWrite.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_InstanceVariableRead:
               InstanceVariableRead.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_StaticVariableWrite:
               StaticVariableWrite.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_StaticVariableRead:
               StaticVariableRead.format( this, sb, data1, data2, data3 );
               break;
                           
            case TestCaseSet.TEST_AllocateLargestByteArray:
               AllocateLargestByteArray.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_RelocationDetection:
               RelocationDetection.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_ArrayCreation:
               ArrayCreation.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_ArrayCopyHighLevel:
               ArrayCopyHighLevel.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_ArrayCopyLowLevel:
               ArrayCopyLowLevel.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_ThreadYield:
               ThreadYield.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_SyncBlockUncontended:
               SyncBlockUncontended.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_SyncBlockContended:
               SyncBlockContended.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_CaffeineMark:
               CaffeineMark.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_VQDecode:
               VQDecode.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_FullscreenFill:
               FullscreenFill.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_FullscreenBlit:
               FullscreenBlit.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_FontRender:
               FontRender.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_FullscreenFillClip:
               FullscreenFillClip.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_FullscreenBlitClip:
               FullscreenBlitClip.format( this, sb, data1, data2, data3 );
               break;
               
            case TestCaseSet.TEST_FontRenderClip:
               FontRenderClip.format( this, sb, data1, data2, data3 );
               break;
               
            default:
               sb.append( "  " );
               sb.append( data1 );
               sb.append( ", " );
               sb.append( data2 );
               sb.append( ", " );
               sb.append( data3 );
               sb.append( "\r" );
               sb.append( "\n" );
               break;
         }
         sb.append( "\r" );
         sb.append( "\n" );
      }
      else
      {
         sb.append( new String(line) );
         sb.append( "\r" );
         sb.append( "\n" );
      }
            
      return sb.toString();
   }
   
   private byte[] work= new byte[128];
   private int unreadC;   
   private boolean haveUnreadC= false;
   byte[] getLine( InputStream is )
   {
      int i, c;
      byte[] line= null;
      
      i= 0;
      for( ; ; )
      {
         if ( haveUnreadC )
         {
            c= unreadC;
            haveUnreadC= false;
         }
         else
         {
            try
            {
                  c= is.read();
            }
            catch( IOException ioe )
            {
               c= -1;
            }
         }
         
         if ( (c == 0x0a) || (c == 0x0d) || (c == -1) )
         {
            try
            {
                  c= is.read();
            }
            catch( IOException ioe )
            {
               c= -1;
            }
            
            if ( (c != 0x0a) && (c != 0x0d) && (c != -1) )
            {
               haveUnreadC= true;
               unreadC= c;
            }
            break;            
         }
         
         if ( i >= work.length )
         {
            byte workNew[]= new byte[work.length*2+1];
            System.arraycopy( work, 0, workNew, 0, work.length );
            work= workNew;
         }
         
         work[i]= (byte)(c&0xFF);
         
         ++i;
      }
    
      /* JRW
      if ( i > 2 )
      {
         if ( (work[0] == '0') && (work[1] == 'x') )
         {            
            line= new byte[i/2-1];
            int b, j;
            for( j= 2; j < i; j += 2 )
            {
               b= getByteFromASCIIHex(work,IndexMapper.getIndex(lineNumber,j/2-1)*2+2);
               line[j/2-1]= (byte)(b&0xFF);
            }
            ++lineNumber;
         }
      }
      */
      if ( i > 0 )
      {
         line= new byte[i];
         System.arraycopy( work, 0, line, 0, i );
      }
            
      return line;      
   }
   
   private long getLong( byte data[], int offset )
   {
      long r= 0;
      int i,imax,b;
      
      imax= offset+8;
      for( i= offset; i < imax; ++i )
      {
         b= (data[i]&0xFF);
         r= r*256 + b;
      }
      
      return r;
   }

   private int getInt( byte data[], int offset )
   {
      int r= 0;
      int i,imax,b;
      
      imax= offset+4;
      for( i= offset; i < imax; ++i )
      {
         b= (data[i]&0xFF);
         r= r*256 + b;
      }
      
      return r;
   }
   
   private int getByteFromASCIIHex( byte data[], int offset )
   {
      int r= 0;
      int i,imax,b;
      
      imax= offset+2;
      for( i= offset; i < imax; ++i )
      {
         b= (data[i]&0xFF);
         if ( (b >= '0') && (b <= '9') )
         {
            b -= '0';
         }
         else if ( (b >= 'a') && (b <= 'f') )
         {
            b -= ('a'-10);
         }
         else if ( (b >= 'A') && (b <= 'F') )
         {
            b -= ('A'-10);
         }
         r= r*16 + b;
      }
      
      return r;
   }
}
