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

package javax.tv.locator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.NetworkLocator;

/**
 * Tests LocatorFactory
 * 
 * @author Todd Earles
 * @author Brian Greene
 */
public class LocatorFactoryTest extends TestCase
{
    /**
     * Test createLocator()
     */
    public void testCreateOcapLocator() throws Exception
    {
        LocatorFactory lf = LocatorFactory.getInstance();
        Locator locator = lf.createLocator("ocap://0x44c");
        assertTrue("Expected locator to be of type OcapLocator", locator instanceof OcapLocator);
        assertTrue("Expected toExternalForm() to match original string", locator.toExternalForm()
                .equals("ocap://0x44c"));
        try
        {
            locator = lf.createLocator("");
            fail("Expected MalformedLocatorException");
        }
        catch (MalformedLocatorException e)
        {
        }
    }

    /**
     * Tests that createLocator creates NetworkLocators correctly.
     */
    public void testCreateNetworkLocator() throws Exception
    {
        LocatorFactory lf = LocatorFactory.getInstance();
        Locator loc = lf.createLocator("network://0x45.0x3");
        assertNotNull("Expected the locator to exist", loc);
        assertTrue("Expected locator to be of type NetworkLocator", loc instanceof NetworkLocator);
        assertTrue("expected the external form of the locator to matck original string",
                "network://0x45.0x3".equals(loc.toExternalForm()));
    }

    /**
     * Tests that createLocator creates TransportStreamLocator correctly.
     * 
     */
    public void testCreateTransportStreamLocator() throws Exception
    {

        String tsLocString = "ocap://0x1388.0xa";
        LocatorFactory lf = LocatorFactory.getInstance();
        Locator loc = lf.createLocator(tsLocString);
        assertNotNull("Expected the locator to exist", loc);
        assertTrue("Expected locator to be of type TransportStreamLocator", loc instanceof OcapLocator);
        assertTrue("expected the external form of the locator to matck original string",
                tsLocString.equals(loc.toExternalForm()));
    }

    /**
     * Used to test that only the expected exception is thrown from the
     * LocatorFactory when it is passed bad data.
     */
    public void testExceptions()
    {
        LocatorFactory lf = LocatorFactory.getInstance();
        Locator loc = null;
        // create bad locators...
        try
        {
            loc = lf.createLocator("networK://0x45.0x3");
            fail("exception should have been thrown.");
        }
        catch (MalformedLocatorException e)
        {/* should be thrown so we do nothing */
        }
        catch (Exception e)
        {
            fail("Only MalformedLocator may be thrown.");
        }
        try
        {
            loc = lf.createLocator("transportSTream://0x45.0x3");
            fail("exception should have been thrown.");
        }
        catch (MalformedLocatorException e)
        {/* should be thrown so we do nothing */
        }
        catch (Exception e)
        {
            fail("Only MalformedLocator may be thrown.");
        }
        try
        {
            loc = lf.createLocator("oCap://0x45");
            fail("exception should have been thrown.");
        }
        catch (MalformedLocatorException e)
        {/* should be thrown so we do nothing */
        }
        catch (Exception e)
        {
            fail("Only MalformedLocator may be thrown.");
        }
        try
        {
            loc = lf.createLocator("network://45.0x3");
            fail("exception should have been thrown.");
        }
        catch (MalformedLocatorException e)
        {/* should be thrown so we do nothing */
        }
        catch (Exception e)
        {
            fail("Only MalformedLocator may be thrown.");
        }
        try
        {
            loc = lf.createLocator("transportStream://0x45.0");
            fail("exception should have been thrown.");
        }
        catch (MalformedLocatorException e)
        {/* should be thrown so we do nothing */
        }
        catch (Exception e)
        {
            fail("Only MalformedLocator may be thrown.");
        }
        try
        {
            loc = lf.createLocator(null);
            fail("Null Pointer should have been thrown.");
        }
        catch (NullPointerException e)
        {/* do nothing as this is what we wanted */
        }
        catch (Exception e)
        {
            fail("NullPointerException should have been thrown.");
        }
    }

    /**
     * Test transformLocator()
     */
    public void testTransformLocator() throws Exception
    {
        LocatorFactory lf = LocatorFactory.getInstance();
        Locator locator = lf.createLocator("ocap://0x44c");
        Locator tl[] = lf.transformLocator(locator);
        assertTrue("Expected array size to be 1", tl.length == 1);
        assertTrue("Expected locator to be of type OcapLocator", tl[0] instanceof OcapLocator);
        assertTrue("Expected toExternalForm() to match original string", tl[0].toExternalForm().equals("ocap://0x44c"));
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(LocatorFactoryTest.class);
        return suite;
    }

    public LocatorFactoryTest(String name)
    {
        super(name);
    }
}
