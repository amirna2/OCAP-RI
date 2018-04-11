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

import com.tvworks.plateval.framework.TestCase;
import com.tvworks.plateval.util.ResultLog;

public class TestCaseSet
{
   public static final int TEST_DumpResults=                  9999;
   public static final int TEST_EmptyLoop=                   10000;
   public static final int TEST_StaticClassLoading=          10001;
   public static final int TEST_VirtualMethodInvocation=     10002;
   public static final int TEST_NonVirtualMethodInvocation=  10003;
   public static final int TEST_InterfaceMethodInvocation=   10004;
   public static final int TEST_StaticMethodInvocation=      10005;
   public static final int TEST_TotalAndFreeMemory=          10006;
   public static final int TEST_AllocateLargestByteArray=    10007;
   public static final int TEST_RelocationDetection=         10008;
   public static final int TEST_ObjectInstantiationShallow=  10009;
   public static final int TEST_ObjectInstantiationDeep=     10010;
   public static final int TEST_LocalVariableWrite=          10011;
   public static final int TEST_LocalVariableRead=           10012;
   public static final int TEST_InstanceVariableWrite=       10013;
   public static final int TEST_InstanceVariableRead=        10014;
   public static final int TEST_StaticVariableWrite=         10015;
   public static final int TEST_StaticVariableRead=          10016;
   public static final int TEST_ArrayCreation=               10017;
   public static final int TEST_ArrayCopyHighLevel=          10018;
   public static final int TEST_ArrayCopyLowLevel=           10019;
   public static final int TEST_ThreadYield=                 10020;
   public static final int TEST_SyncBlockUncontended=        10021;
   public static final int TEST_SyncBlockContended=          10022;
   public static final int TEST_CaffeineMark=                10023;
   public static final int TEST_VQDecode=                    10024;
   public static final int TEST_FullscreenFill=              10025;
   public static final int TEST_FullscreenBlit=              10026;
   public static final int TEST_FontRender=                  10027;
   public static final int TEST_FullscreenFillClip=          10028;
   public static final int TEST_FullscreenBlitClip=          10029;
   public static final int TEST_FontRenderClip=              10030;
   
   static int mTestCaseListFull[]= 
   {
      TEST_StaticClassLoading,
      TEST_EmptyLoop,
      TEST_TotalAndFreeMemory,
      TEST_VirtualMethodInvocation,
      TEST_NonVirtualMethodInvocation,
      TEST_InterfaceMethodInvocation,
      TEST_StaticMethodInvocation,
      TEST_ObjectInstantiationShallow,
      TEST_ObjectInstantiationDeep,
      TEST_LocalVariableWrite,
      TEST_LocalVariableRead,
      TEST_InstanceVariableWrite,
      TEST_InstanceVariableRead,
      TEST_StaticVariableWrite,
      TEST_StaticVariableRead,
      TEST_ArrayCopyHighLevel,
      TEST_ArrayCopyLowLevel,
      TEST_ArrayCreation,
      TEST_ThreadYield,
      TEST_SyncBlockUncontended,
      TEST_SyncBlockContended,
      TEST_CaffeineMark,
      TEST_DumpResults,
      TEST_FontRender,
      TEST_FullscreenFill,
      TEST_FullscreenBlit,
      TEST_FontRenderClip,
      TEST_FullscreenFillClip,
      TEST_FullscreenBlitClip,
      TEST_DumpResults,
      TEST_VQDecode,
      TEST_DumpResults,
      TEST_TotalAndFreeMemory,
      TEST_AllocateLargestByteArray,
      TEST_RelocationDetection,
      TEST_TotalAndFreeMemory,
      TEST_AllocateLargestByteArray,
   };

   static int mTestCaseListSubset1[]= 
   {
      TEST_StaticClassLoading,
      TEST_EmptyLoop,
      TEST_FontRender,
      TEST_DumpResults,
      TEST_FontRenderClip,
      TEST_DumpResults,
      TEST_FullscreenFill,
      TEST_DumpResults,
      TEST_FullscreenBlit,
      TEST_DumpResults,
      TEST_FullscreenFillClip,
      TEST_DumpResults,
      TEST_FullscreenBlitClip,
      TEST_DumpResults,
      TEST_VQDecode,
   };
   
   private int mCurrentTestIndex;
   private int mTestCaseList[];
   
   public static String getName( int id )
   {
      String name;
      
      switch( id )
      {
         case TEST_StaticClassLoading:
            name= "StaticClassLoading";
            break;
         case TEST_EmptyLoop:
            name= "EmptyLoop";
            break;
         case TEST_VirtualMethodInvocation:
            name= "VirtualMethodInvocation";
            break;
         case TEST_NonVirtualMethodInvocation:
            name= "NonVirtualMethodInvocation";
            break;
         case TEST_InterfaceMethodInvocation:
            name= "InterfaceMethodInvocation";
            break;
         case TEST_StaticMethodInvocation:
            name= "StaticMethodInvocation";
            break;
         case TEST_TotalAndFreeMemory:
            name= "TotalAndFreeMemory";
            break;
         case TEST_AllocateLargestByteArray:
            name= "AllocateLargestByteArray";
            break;
         case TEST_RelocationDetection:
            name= "RelocationDetection";
            break;
         case TEST_ObjectInstantiationShallow:
            name= "ObjectInstantiationShallow";
            break;
         case TEST_ObjectInstantiationDeep:
            name= "ObjectInstantiationDeep";
            break;
         case TEST_LocalVariableWrite:
            name= "LocalVariableWrite";
            break;
         case TEST_LocalVariableRead:
            name= "LocalVariableRead";
            break;
         case TEST_InstanceVariableWrite:
            name= "InstanceVariableWrite";
            break;
         case TEST_InstanceVariableRead:
            name= "InstanceVariableRead";
            break;
         case TEST_StaticVariableWrite:
            name= "StaticVariableWrite";
            break;
         case TEST_StaticVariableRead:
            name= "StaticVariableRead";
            break;
         case TEST_ArrayCreation:
            name= "ArrayCreation";
            break;
         case TEST_ArrayCopyHighLevel:
            name= "ArrayCopyHighLevel";
            break;
         case TEST_ArrayCopyLowLevel:
            name= "ArrayCopyLowLevel";
            break;
         case TEST_ThreadYield:
            name= "ThreadYield";
            break;
         case TEST_SyncBlockUncontended:
            name= "SyncBlockUncontended";
            break;
         case TEST_SyncBlockContended:
            name= "SyncBlockContended";
            break;
         case TEST_CaffeineMark:
            name= "CaffeineMark";
            break;
         case TEST_VQDecode:
            name= "VQDecode";
            break;
         case TEST_FullscreenFill:
            name= "FullscreenFill";
            break;
         case TEST_FullscreenBlit:
            name= "FullscreenBlit";
            break;
         case TEST_FontRender:
            name= "FontRender";
            break;
         case TEST_FullscreenFillClip:
            name= "FullscreenFillClip";
            break;
         case TEST_FullscreenBlitClip:
            name= "FullscreenBlitClip";
            break;
         case TEST_FontRenderClip:
            name= "FontRenderClip";
            break;
         default:
            name= "Unknown("+Integer.toHexString(id)+")";
            break;
      }
      
      return name;
   }
   
   public TestCaseSet()
   {
      mCurrentTestIndex= 0;
      mTestCaseList= mTestCaseListFull;
   }

   public TestCaseSet( int subset )
   {
      mCurrentTestIndex= 0;
      switch( subset )
      {
         case 1:
            mTestCaseList= mTestCaseListSubset1;
            break;
         default:
            mTestCaseList= mTestCaseListFull;
            break;
      }
   }
   
   public int currentTestIndex()
   {
      return mCurrentTestIndex;
   }
   
   public int numberOfTests()
   {
      return mTestCaseList.length;   
   }
   
   public boolean hasNext()
   {
      return (mCurrentTestIndex < mTestCaseList.length );   
   }
   
   public TestCase getNext()
   {
      TestCase testcase= null;
      
      if ( (mCurrentTestIndex >= 0) && (mCurrentTestIndex < mTestCaseList.length) )
      {
         int id= mTestCaseList[mCurrentTestIndex++];
         
         if ( id == TEST_DumpResults )
         {
            ResultLog.getInstance().dump();            
            if ( (mCurrentTestIndex >= 0) && (mCurrentTestIndex < mTestCaseList.length) )
            {
               id= mTestCaseList[mCurrentTestIndex++];
            }
            else
            {
               return null;
            }
         }
         
         switch ( id )
         {
            case TEST_StaticClassLoading:
               testcase= new com.tvworks.plateval.fundamental.StaticClassLoading();
               break;
            case TEST_EmptyLoop:
               testcase= new com.tvworks.plateval.fundamental.EmptyLoop();
               break;
            case TEST_VirtualMethodInvocation:
               testcase= new com.tvworks.plateval.fundamental.VirtualMethodInvocation();
               break;
            case TEST_NonVirtualMethodInvocation:
               testcase= new com.tvworks.plateval.fundamental.NonVirtualMethodInvocation();
               break;
            case TEST_InterfaceMethodInvocation:
               testcase= new com.tvworks.plateval.fundamental.InterfaceMethodInvocation();
               break;
            case TEST_StaticMethodInvocation:
               testcase= new com.tvworks.plateval.fundamental.StaticMethodInvocation();
               break;
            case TEST_TotalAndFreeMemory:
               testcase= new com.tvworks.plateval.memory.TotalAndFreeMemory();
               break;
            case TEST_AllocateLargestByteArray:
               testcase= new com.tvworks.plateval.memory.AllocateLargestByteArray();
               break;
            case TEST_RelocationDetection:
               testcase= new com.tvworks.plateval.memory.RelocationDetection();
               break;
            case TEST_ObjectInstantiationShallow:
               testcase= new com.tvworks.plateval.fundamental.ObjectInstantiationShallow();
               break;
            case TEST_ObjectInstantiationDeep:
               testcase= new com.tvworks.plateval.fundamental.ObjectInstantiationDeep();
               break;
            case TEST_LocalVariableWrite:
               testcase= new com.tvworks.plateval.fundamental.LocalVariableWrite();
               break;
            case TEST_LocalVariableRead:
               testcase= new com.tvworks.plateval.fundamental.LocalVariableRead();
               break;
            case TEST_InstanceVariableWrite:
               testcase= new com.tvworks.plateval.fundamental.InstanceVariableWrite();
               break;
            case TEST_InstanceVariableRead:
               testcase= new com.tvworks.plateval.fundamental.InstanceVariableRead();
               break;
            case TEST_StaticVariableWrite:
               testcase= new com.tvworks.plateval.fundamental.StaticVariableWrite();
               break;
            case TEST_StaticVariableRead:
               testcase= new com.tvworks.plateval.fundamental.StaticVariableRead();
               break;
            case TEST_ArrayCreation:
               testcase= new com.tvworks.plateval.fundamental.ArrayCreation();
               break;
            case TEST_ArrayCopyHighLevel:
               testcase= new com.tvworks.plateval.fundamental.ArrayCopyHighLevel();
               break;
            case TEST_ArrayCopyLowLevel:
               testcase= new com.tvworks.plateval.fundamental.ArrayCopyLowLevel();
               break;
            case TEST_ThreadYield:
               testcase= new com.tvworks.plateval.fundamental.ThreadYield();
               break;
            case TEST_SyncBlockUncontended:
               testcase= new com.tvworks.plateval.fundamental.SyncBlockUncontended();
               break;
            case TEST_SyncBlockContended:
               testcase= new com.tvworks.plateval.fundamental.SyncBlockContended();
               break;
            case TEST_CaffeineMark:
               testcase= new com.tvworks.plateval.caffeinemark.CaffeineMark();
               break;
            case TEST_VQDecode:
               testcase= new com.tvworks.plateval.vq.VQDecode();
               break;
            case TEST_FullscreenFill:
               testcase= new com.tvworks.plateval.graphics.FullscreenFill();
               break;
            case TEST_FullscreenBlit:
               testcase= new com.tvworks.plateval.graphics.FullscreenBlit();
               break;
            case TEST_FontRender:
               testcase= new com.tvworks.plateval.graphics.FontRender();
               break;
            case TEST_FullscreenFillClip:
               testcase= new com.tvworks.plateval.graphics.FullscreenFillClip();
               break;
            case TEST_FullscreenBlitClip:
               testcase= new com.tvworks.plateval.graphics.FullscreenBlitClip();
               break;
            case TEST_FontRenderClip:
               testcase= new com.tvworks.plateval.graphics.FontRenderClip();
               break;
         }
      }
      
      return testcase;
   }
}
