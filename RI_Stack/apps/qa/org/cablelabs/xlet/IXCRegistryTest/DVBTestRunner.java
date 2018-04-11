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

package org.cablelabs.xlet.IXCRegistryTest;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Vector;
import java.util.Iterator;

import org.dvb.application.AppID;

import org.dvb.io.ixc.IxcRegistry;

public class DVBTestRunner extends TestRunner implements Xlet
{
    private XletContext m_context;

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_context = ctx;

        // Create AppID for this Xlet and initialize the base class
        String aidStr = (String) ctx.getXletProperty("dvb.app.id");
        String oidStr = (String) ctx.getXletProperty("dvb.org.id");
        int aid = Integer.parseInt(aidStr, 16);
        long oid = Long.parseLong(oidStr, 16);
        AppID appID = new AppID((int) oid, aid);

        try
        {
            init(appID, ctx);
        }
        catch (FileNotFoundException e)
        {
            throw new XletStateChangeException("Could not find script file -- " + getScriptFilename(appID));
        }
        catch (IOException e)
        {
            throw new XletStateChangeException("Error reading script file!");
        }
    }

    public void startXlet() throws XletStateChangeException
    {
        start();
    }

    public void pauseXlet()
    {
        pause();
    }

    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        destroy(arg0);
    }

    public BindTest createBindTest()
    {
        return new DVBBindTest();
    }

    public BindTest createRebindTest()
    {
        return new DVBRebindTest();
    }

    public LookupTest createLookupTest()
    {
        return new DVBLookupTest();
    }

    public ListTest createListTest()
    {
        return new DVBListTest();
    }

    /**
     * This test class executes DVB IXC "bind" tests. This test will attempt to
     * bind the xlet's RemoteObject under a given name. The success or failure
     * of the bind operation will be tested against a given expected result
     */
    private class DVBBindTest extends BindTest
    {
        public DVBBindTest()
        {
            super("DVB Bind Test");
        }

        public void runTest()
        {
            try
            {
                IxcRegistry.bind(m_context, getBindName(), m_myRemote);

                if (getExpectedResult() == TEST_RESULT_PASS)
                    test.assertTrue(true);
                else if (getExpectedResult() == TEST_RESULT_FAIL)
                    test.fail(getTestInfo() + " -- " + toString() + "Bind was expected to fail, but passed!");
            }
            catch (Exception e)
            {
                if (getExpectedResult() == TEST_RESULT_PASS)
                    test.fail(getTestInfo() + " -- " + toString() + " Exception thrown while trying to bind object! "
                            + e.getMessage());
                else if (getExpectedResult() == TEST_RESULT_FAIL) test.assertTrue(true);
            }
        }
    }

    /**
     * This test class executes IXC "rebind" tests. This test will attempt to
     * rebind the xlet's RemoteObject under a given name. The success or failure
     * of the bind operation will be tested against a given expected result
     */
    private class DVBRebindTest extends BindTest
    {
        public DVBRebindTest()
        {
            super("DVB Rebind Test");
        }

        public void runTest()
        {
            try
            {
                IxcRegistry.rebind(m_context, getBindName(), m_myRemote);

                if (getExpectedResult() == TEST_RESULT_PASS)
                    test.assertTrue(true);
                else if (getExpectedResult() == TEST_RESULT_FAIL)
                    test.fail(getTestInfo() + " -- " + toString() + "Rebind was expected to fail, but passed!");
            }
            catch (Exception e)
            {
                if (getExpectedResult() == TEST_RESULT_PASS)
                    test.fail(getTestInfo() + " -- " + toString() + " Exception thrown while trying to rebind object! "
                            + e.getMessage());
                else if (getExpectedResult() == TEST_RESULT_FAIL) test.assertTrue(true);
            }
        }
    }

    /**
     * The IXC "lookup" test. This test will attempt to lookup a given remote
     * object name. If the expected result is <code>TEST_RESULT_PASS</code> and
     * the remote object is successfully acquired, the test will call the
     * getString() method of the remote object and compare it against the given
     * expected string
     */
    private class DVBLookupTest extends LookupTest
    {
        public DVBLookupTest()
        {
            super("DVB Lookup Test");
        }

        public void runTest()
        {
            try
            {
                TestRemote obj = (TestRemote) IxcRegistry.lookup(m_context, getLookupName());
                if (getExpectedResult() == TEST_RESULT_PASS)
                {
                    test.assertTrue(getTestInfo() + " -- " + toString()
                            + " String returned from remote object does not match expected!", obj.getTestString()
                            .equals(getExpectedString()));
                }
                else
                {
                    test.fail(getTestInfo() + " -- " + toString() + " Lookup was expected to fail, but passed!");
                }
            }
            catch (Exception e)
            {
                if (getExpectedResult() == TEST_RESULT_PASS)
                    test.fail(getTestInfo() + " -- " + toString() + " Exception thrown while trying to lookup object! "
                            + e.getMessage());
                else if (getExpectedResult() == TEST_RESULT_FAIL) test.assertTrue(true);
            }
        }

    }

    /**
     * Tests the list of remote object names against a set of expected names.
     * Unfortunately, it is not possible to predict the exact list of remote
     * objects that will be available to this xlet. For example, the AutoXlet
     * framework relies on IXC for remote control of test xlets. This test only
     * ensures that the list of expected names are actually present and that the
     * list of unexpected names is not present.
     */
    private class DVBListTest extends ListTest
    {
        public DVBListTest()
        {
            super("DVB Lookup Test");
        }

        public void runTest()
        {
            // Create vector of names based on our registry list so that
            // it can be easily searched
            String[] regNames = IxcRegistry.list(m_context);
            Vector names = new Vector(regNames.length);
            for (int i = 0; i < regNames.length; ++i)
                names.addElement(regNames[i]);

            // Look for expected names
            for (Iterator i = getExpectedNames().iterator(); i.hasNext();)
            {
                String name = (String) i.next();
                test.assertTrue(getTestInfo() + " -- " + toString() + " Expected registry list to return " + name,
                        names.contains(name));
            }

            // Look for unexpected names
            for (Iterator i = getNotExpectedNames().iterator(); i.hasNext();)
            {
                String name = (String) i.next();
                test.assertFalse(getTestInfo() + " -- " + toString() + " Expected registry list to NOT return " + name,
                        names.contains(name));
            }

        }

    }
}
