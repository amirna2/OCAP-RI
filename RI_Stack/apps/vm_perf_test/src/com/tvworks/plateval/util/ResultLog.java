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

import java.util.Vector;

public class ResultLog
{
   private static final ResultLog mLog= new ResultLog();
   private Vector results;
   
   public static void touch()
   {      
   }

   private ResultLog()
   {
      results= new Vector();
   }
   
   public static ResultLog getInstance()
   {
      return mLog;
   }
   
   public synchronized void add( int id, int data1, int data2, long resultTime )
   {
      ResultItem newItem= new ResultItem( System.currentTimeMillis(), id, data1, data2, resultTime );
      results.addElement(newItem);
   }
   
   public synchronized void dump()
   {
      int i, j, count, b;
      byte data[];
      StringBuffer sb;
      
      System.out.println("---------------------- PlatEval: Start of Results ----------------------" );
      sb= new StringBuffer();
      count= results.size();
      for( i= 0; i < count; ++i )
      {
         ResultItem ri= (ResultItem)results.elementAt(i);
         data= ri.toBytes(i);
         sb.setLength(0);
         sb.append( "0x" );
         for( j= 0; j < data.length; ++j )
         {
            b= (data[j] & 0xFF);
            if ( b < 0x10 )
            {
               sb.append('0');
            }
            sb.append( Integer.toHexString(b) );            
         }
         System.out.println( sb.toString() );
      }
      System.out.println("---------------------- PlatEval: End of Results ----------------------" );
   }
   
   public synchronized void flush()
   {
      results= new Vector();
   }
   
   private class ResultItem
   {
      long mTime;
      int mId;
      int mData1;
      int mData2;
      long mResultTime;
      
      public ResultItem( long time, int id, int data1, int data2, long resultTime )
      {
         mTime= time;
         mId= id;
         mData1= data1;
         mData2= data2;
         mResultTime= resultTime;
      }
      
      public byte[] toBytes(int recordNumber)
      {
         byte b[]= new byte[28];

         b[IndexMapper.getIndex(recordNumber,0)]=  (byte)((mTime>>56)&0xFF);
         b[IndexMapper.getIndex(recordNumber,1)]=  (byte)((mTime>>48)&0xFF);
         b[IndexMapper.getIndex(recordNumber,2)]=  (byte)((mTime>>40)&0xFF);
         b[IndexMapper.getIndex(recordNumber,3)]=  (byte)((mTime>>32)&0xFF);
         b[IndexMapper.getIndex(recordNumber,4)]=  (byte)((mTime>>24)&0xFF);
         b[IndexMapper.getIndex(recordNumber,5)]=  (byte)((mTime>>16)&0xFF);
         b[IndexMapper.getIndex(recordNumber,6)]=  (byte)((mTime>>8)&0xFF);
         b[IndexMapper.getIndex(recordNumber,7)]=  (byte)((mTime)&0xFF);

         b[IndexMapper.getIndex(recordNumber,8)]=  (byte)((mId>>24)&0xFF);
         b[IndexMapper.getIndex(recordNumber,9)]=  (byte)((mId>>16)&0xFF);
         b[IndexMapper.getIndex(recordNumber,10)]= (byte)((mId>>8)&0xFF);
         b[IndexMapper.getIndex(recordNumber,11)]= (byte)((mId)&0xFF);

         b[IndexMapper.getIndex(recordNumber,12)]= (byte)((mData1>>24)&0xFF);
         b[IndexMapper.getIndex(recordNumber,13)]= (byte)((mData1>>16)&0xFF);
         b[IndexMapper.getIndex(recordNumber,14)]= (byte)((mData1>>8)&0xFF);
         b[IndexMapper.getIndex(recordNumber,15)]= (byte)((mData1)&0xFF);

         b[IndexMapper.getIndex(recordNumber,16)]= (byte)((mData2>>24)&0xFF);
         b[IndexMapper.getIndex(recordNumber,17)]= (byte)((mData2>>16)&0xFF);
         b[IndexMapper.getIndex(recordNumber,18)]= (byte)((mData2>>8)&0xFF);
         b[IndexMapper.getIndex(recordNumber,19)]= (byte)((mData2)&0xFF);

         b[IndexMapper.getIndex(recordNumber,20)]= (byte)((mResultTime>>56)&0xFF);
         b[IndexMapper.getIndex(recordNumber,21)]= (byte)((mResultTime>>48)&0xFF);
         b[IndexMapper.getIndex(recordNumber,22)]= (byte)((mResultTime>>40)&0xFF);
         b[IndexMapper.getIndex(recordNumber,23)]= (byte)((mResultTime>>32)&0xFF);
         b[IndexMapper.getIndex(recordNumber,24)]= (byte)((mResultTime>>24)&0xFF);
         b[IndexMapper.getIndex(recordNumber,25)]= (byte)((mResultTime>>16)&0xFF);
         b[IndexMapper.getIndex(recordNumber,26)]= (byte)((mResultTime>>8)&0xFF);
         b[IndexMapper.getIndex(recordNumber,27)]= (byte)((mResultTime)&0xFF);
         
         return b;
      }
   }
}
