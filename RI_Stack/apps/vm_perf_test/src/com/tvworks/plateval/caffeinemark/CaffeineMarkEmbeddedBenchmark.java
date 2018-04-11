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

import java.util.Vector;

public class CaffeineMarkEmbeddedBenchmark extends Thread
{

    public CaffeineMarkEmbeddedBenchmark()
    {
    }

    public static void setAllLocal()
    {
        for(int i = 0; i < testSequence.size(); i++)
        {
            BenchmarkUnit benchmarkunit = (BenchmarkUnit)testSequence.elementAt(i);
            benchmarkunit.setLocal();
        }

    }

    public static int getHighScore()
    {
        int i = 0;
        for(int j = 0; j < testResults.size(); j++)
        {
            int k = getTestResult(j);
            if(k > i)
                i = k;
        }

        return i;
    }

    public static void setAllRemote()
    {
        for(int i = 0; i < testSequence.size(); i++)
        {
            BenchmarkUnit benchmarkunit = (BenchmarkUnit)testSequence.elementAt(i);
            benchmarkunit.setRemote();
        }

    }

    public static void setLocal(int i)
    {
        isLocal = true;
    }

    public static int getLastResult(int i)
    {
        if(i >= testResults.size())
        {
            return 0;
        } else
        {
            BenchmarkUnit benchmarkunit = (BenchmarkUnit)testSequence.elementAt(i);
            return benchmarkunit.lastResult;
        }
    }

    public static int getTestResult(int i)
    {
        if(i >= testResults.size())
        {
            return 0;
        } else
        {
            Integer integer = (Integer)testResults.elementAt(i);
            return integer.intValue();
        }
    }

    public static String getTestName(int i)
    {
        if(i >= testSequence.size())
        {
            return "Invalid Test Number";
        } else
        {
            BenchmarkUnit benchmarkunit = (BenchmarkUnit)testSequence.elementAt(i);
            return benchmarkunit.testName();
        }
    }

    public static void clearSequence()
    {
        isNormalSequence = false;
        testSequence.removeAllElements();
        testResults.removeAllElements();
    }

    public static void addTest(int i)
    {
        isNormalSequence = false;
        isComplete = false;
        switch(i)
        {
        case 0: // '\0'
            testSequence.addElement(new BenchmarkUnit(new SieveAtom()));
            testResults.addElement(new Integer(0));
            return;

        case 1: // '\001'
            testSequence.addElement(new BenchmarkUnit(new LoopAtom()));
            testResults.addElement(new Integer(0));
            return;

        case 2: // '\002'
            testSequence.addElement(new BenchmarkUnit(new LogicAtom()));
            testResults.addElement(new Integer(0));
            return;

        case 3: // '\003'
            testSequence.addElement(new BenchmarkUnit(new StringAtom()));
            testResults.addElement(new Integer(0));
            return;

        case 4: // '\004'
            testSequence.addElement(new BenchmarkUnit(new FloatAtom()));
            testResults.addElement(new Integer(0));
            return;

        case 5: // '\005'
            testSequence.addElement(new BenchmarkUnit(new MethodAtom()));
            testResults.addElement(new Integer(0));
            return;
        }
    }

    public void run()
    {
        int i;
        for(i = 0; i < testSequence.size(); i++)
        {
            if(monitor != null)
            {
                Thread.yield();
                if(!monitor.status(i, testSequence.size()))
                    return;
            }
            Thread.yield();
            BenchmarkUnit benchmarkunit = (BenchmarkUnit)testSequence.elementAt(i);
            if(isLocal)
                benchmarkunit.setLocal();
            testResults.setElementAt(new Integer(benchmarkunit.testScore()), i);
        }

        if(monitor != null)
            monitor.status(i, testSequence.size());
        if(isNormalSequence)
            isComplete = true;
    }

    public static int EmbeddedCaffeineMarkScore()
    {
        float d = 0.0F;
        float d1 = 0.0F;
        if(isComplete && (isNormalSequence || isEmbeddedSequence))
        {
            int i = 0;
            do
            {
                Integer integer = (Integer)testResults.elementAt(i);
                if(integer.intValue() != 0)
                {
                    d1++;
                    d += FMath.log( (float)integer.intValue() );
                } else
                {
                    return 0;
                }
            } while(++i <= 5);
            if(d1 > 0.5F)
                d /= d1;
            else
                return 0;
            return (int)FMath.exp(d);
        } else
        {
            return 0;
        }
    }

    public static void setNormalSequence()
    {
        testSequence.removeAllElements();
        testResults.removeAllElements();
        addTest(0);
        addTest(1);
        addTest(2);
        addTest(3);
        addTest(4);
        addTest(5);
        isNormalSequence = true;
    }

    public static final int SieveTest = 0;
    public static final int LoopTest = 1;
    public static final int LogicTest = 2;
    public static final int StringTest = 3;
    public static final int FloatTest = 4;
    public static final int MethodTest = 5;
    public static BenchmarkMonitor monitor;
    static Vector testSequence = new Vector();
    static Vector testResults = new Vector();
    static boolean isNormalSequence;
    static boolean isLocal;
    static boolean isComplete;
    static boolean isEmbeddedSequence;

}
