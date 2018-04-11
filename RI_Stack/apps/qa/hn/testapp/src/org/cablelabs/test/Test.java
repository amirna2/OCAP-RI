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

package org.cablelabs.test;

public abstract class Test implements TestElement
{
    public static final int TEST_STATUS_INTERRUPTED = 3;

    public static final int TEST_STATUS_NONE = 2;

    public static final int TEST_STATUS_PASS = 1;

    public static final int TEST_STATUS_FAIL = 0;

    public static final int MINIMUM_TIMEOUT = 5000;

    public static final int MAXIMUM_TIMEOUT = 6000000;

    private int m_status = TEST_STATUS_NONE;

    protected String m_name = "Base Test";

    protected String m_description = "This is the base test class";

    protected String m_failedDescription = null;

    protected long m_totalRunTime = 0;

    protected Logger testLogger = new Logger();

    private TestElement m_parentElement = null;

    /*
     * Timeout in milliseconds.
     */
    private long m_timeout = 5000;

    public Test(String name, int timeout)
    {
        if (timeout < MINIMUM_TIMEOUT || timeout > MAXIMUM_TIMEOUT)
        {
            // Use default timeout;
            timeout = 5000;
        }
        else
        {
            this.m_name = name;
        }
        this.m_timeout = timeout;
    }

    public Test()
    {

    }

    public int getStatus()
    {
        return this.m_status;
    }

    public long getTimeout()
    {
        return this.m_timeout;
    }

    public void setStatus(int status)
    {
        this.m_status = status;
    }

    public void setRunTime(long time)
    {
        this.m_totalRunTime = time;
    }

    public long getRunTime()
    {
        return this.m_totalRunTime;
    }

    public void setFailedDescription(String s)
    {
        this.m_failedDescription = s;
    }

    public String getFailedDescription()
    {
        return this.m_failedDescription;
    }

    public static String statusToString(int status)
    {
        switch (status)
        {
            case Test.TEST_STATUS_FAIL:
                return "TEST_STATUS_FAIL";
            case Test.TEST_STATUS_PASS:
                return "TEST_STATUS_PASS";
            case Test.TEST_STATUS_INTERRUPTED:
                return "TEST_STATUS_INTERRUPTED";
            case Test.TEST_STATUS_NONE:
            default:
                return "TEST_STATUS_NONE";
        }
    }

    public TestElement getParent()
    {
        return m_parentElement;
    }

    public void setParent(TestElement element)
    {
        m_parentElement = element;
    }

    public String getName()
    {
        return "Base Test";
    }

    public int getType()
    {
        return TestElement.TEST_ELEMENT_TYPE_TEST;
    }

    abstract public String getDescription();

    abstract public int prepare();

    abstract public int execute();

    abstract public int clean();
}
