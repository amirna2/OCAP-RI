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

public class LogicAtom
    implements BenchmarkAtom
{

    public boolean initialize(int i)
    {
        if(i != 0)
            wIterationCount = i;
        return true;
    }

    public int execute()
    {
        boolean flag7 = true;
        boolean flag8 = true;
        boolean flag9 = true;
        boolean flag10 = true;
        boolean flag11 = true;
        boolean flag12 = true;
        boolean flag6 = true;
        boolean flag = true;
        boolean flag1 = true;
        boolean flag2 = true;
        boolean flag3 = true;
        boolean flag4 = true;
        boolean flag5 = true;
        flag7 = true;
        flag8 = true;
        flag9 = true;
        flag10 = true;
        flag11 = true;
        flag12 = true;
        for(int i = 0; i < wIterationCount; i++)
            if((flag || flag1) && (flag2 || flag3) && (flag4 || flag5 || flag6))
            {
                flag7 = !flag7;
                flag8 = !flag8;
                flag9 = !flag9;
                flag10 = !flag10;
                flag11 = !flag11;
                flag12 = !flag12;
                flag = !flag;
                flag1 = !flag1;
                flag2 = !flag2;
                flag3 = !flag3;
                flag4 = !flag4;
                flag5 = !flag5;
                flag = !flag;
                flag1 = !flag1;
                flag2 = !flag2;
                flag3 = !flag3;
                flag4 = !flag4;
                flag5 = !flag5;
            }

        flag = true;
        flag1 = false;
        flag2 = true;
        flag3 = false;
        flag4 = true;
        flag5 = false;
        for(int j = 0; j < wIterationCount; j++)
            if((flag || flag1) && (flag2 || flag3) && (flag4 || flag5 || flag6))
            {
                flag = !flag;
                flag1 = !flag1;
                flag2 = !flag2;
                flag3 = !flag3;
                flag4 = !flag4;
                flag5 = !flag5;
                flag7 = !flag7;
                flag8 = !flag8;
                flag9 = !flag9;
                flag10 = !flag10;
                flag11 = !flag11;
                flag12 = !flag12;
                flag7 = !flag7;
                flag8 = !flag8;
                flag9 = !flag9;
                flag10 = !flag10;
                flag11 = !flag11;
                flag12 = !flag12;
            }

        return !flag7 ? 1 : 0;
    }

    public String testName()
    {
        return new String("Logic");
    }

    public LogicAtom()
    {
        wIterationCount = 1200;
    }

    public void setLocal()
    {
    }

    public int cleanUp()
    {
        return 0;
    }

    public int defaultMagnification()
    {
        return 3813;
    }

    public void setRemote()
    {
    }

    public int wIterationCount;
}
