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

public class FloatAtom
    implements BenchmarkAtom
{

    public boolean initialize(int i)
    {
        if(i != 0)
            wMaxDegrees = i;
        xA = new float[3][3];
        vA = new float[3][20];
        vB = new float[3][20];
        int j = 0;
        do
        {
            vA[0][j] = j;
            vA[1][j] = -j;
            vA[2][j] = (float)j * 3.1415926535897931F;
        } while(++j < 20);
        return true;
    }

    public FloatAtom()
    {
        wMaxDegrees = 185;
    }

    public int execute()
    {
        int j;
        for(j = 0; j < wMaxDegrees; j += 5)
        {
            float d = ((float)(float)j * 3.1415926535897931F) / 180F;
            float d1 = FMath.sin(d);
            float d2 = FMath.cos(d);
            xA[0][0] = d2;
            xA[1][0] = d1;
            xA[2][0] = 0.0F;
            xA[0][1] = -d1;
            xA[1][1] = d2;
            xA[2][1] = 0.0F;
            xA[0][2] = 0.0F;
            xA[1][2] = 0.0F;
            xA[2][2] = 1.0F;
            int i = 0;
            do
            {
                vB[0][i] = 0.0F;
                vB[1][i] = 0.0F;
                vB[2][i] = 0.0F;
                int l = 0;
                do
                {
                    int k = 0;
                    do
                        vB[l][i] = vB[l][i] + xA[k][l] * vB[l][i];
                    while(++k < 3);
                } while(++l < 3);
            } while(++i < 20);
        }

        return j;
    }

    public String testName()
    {
        return new String("Float");
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
        return 4449;
    }

    public void setRemote()
    {
    }

    public float xA[][];
    public float xB[][];
    public float vA[][];
    public float vB[][];
    public int wMaxDegrees;
    public final int POINTCOUNT = 20;
}
