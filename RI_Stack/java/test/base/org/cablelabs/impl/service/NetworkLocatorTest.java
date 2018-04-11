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
package org.cablelabs.impl.service;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.net.InvalidLocatorException;

/**
 * Used to test org.cablelabs.service.NetworkLocator
 * 
 * @author bgreene
 */
public class NetworkLocatorTest extends TestCase
{
    /**
     * tests equals() based on URL for <code>NetworkLocator</code>.
     * 
     * @throws Exception
     */
    public void testEquals() throws Exception
    {
        NetworkLocator loc1 = createLocator("network://0x1.0x345");
        NetworkLocator loc2 = createLocator("network://0x2.0x346");

        assertEquals("A locator should be equal to itself", loc1, loc1);
        assertEquals("A locator should be equal to itself", loc2, loc2);
        assertEquals("Equivalent NetworkLocators should be equal", loc1, createLocator("network://0x1.0x345"));
        assertEquals("Equivalent NetworkLocators should be equal", loc2, createLocator("network://0x2.0x346"));
        assertFalse("Different NetworkLocators should NOT be equals", loc1.equals(loc2));
        assertFalse("Different NetworkLocators should NOT be equals", loc2.equals(loc1));
    }

    /**
     * tests hashCode() for <code>NetworkLocator</code>.
     * 
     * @throws Exception
     */
    public void testHashCode() throws Exception
    {
        NetworkLocator loc1 = createLocator("network://0x1.0x345");
        NetworkLocator loc2 = createLocator("network://0x2.0x346");

        assertEquals("A NetworkLocator's hashcode should be equal to itself", loc1.hashCode(), loc1.hashCode());
        assertEquals("A NetworkLocator's should be equal to itself", loc2.hashCode(), loc2.hashCode());
        assertEquals("Equivalent NetworkLocator's hashcode should be equal", loc1.hashCode(), createLocator(
                "network://0x1.0x345").hashCode());
        assertEquals("Equivalent NetworkLocator's hashcode should be equal", loc2.hashCode(), createLocator(
                "network://0x2.0x346").hashCode());
    }

    /**
     * Tests toString() for <code>NetworkLocator</code>.
     * 
     * @throws Exception
     */
    public void testToString() throws Exception
    {
        assertEquals("The string form should be the external form", createLocator("network://0x1.0x345").toString(),
                "network://0x1.0x345");
        assertEquals("The string form should be the external form", createLocator("network://0x2.0x346").toString(),
                "network://0x2.0x346");
    }

    /**
     * tests toExternalForm() for <code>NetworkLocator</code>.
     * 
     * @throws Exception
     */
    public void testToExternalForm() throws Exception
    {
        assertEquals("The string form should be the external form",
                createLocator("network://0x1.0x356").toExternalForm(), "network://0x1.0x356");
        assertEquals("The string form should be the external form",
                createLocator("network://0x2.0x346").toExternalForm(), "network://0x2.0x346");
    }

    /**
     * Tests that a <code>NetworkLocator</code> created from the String result
     * of toExternalForm() is equal to the <code>NetworkLocator</code> that was
     * used to render the external form.
     * 
     * @throws Exception
     */
    public void testRecreate() throws Exception
    {
        NetworkLocator nl1 = createLocator("network://0x1.0x123");
        String exf = nl1.toExternalForm();
        String toString = nl1.toString();

        NetworkLocator newOne = createLocator(exf);

        // they should be the same
        assertEquals("The 2 NetworkLocators should be equal", nl1, newOne);
        NetworkLocator newOne2 = createLocator(toString);
        assertEquals("The 2 NetworkLocators should be equal", nl1, newOne2);

        nl1 = new NetworkLocator(1, 67);
        exf = nl1.toExternalForm();
        toString = nl1.toString();
        newOne = createLocator(toString);

        // they should be the same
        assertEquals("The 2 NetworkLocators should be equal", nl1, newOne);
        newOne2 = createLocator(exf);
        assertEquals("The 2 NetworkLocators should be equal", nl1, newOne2);
    }

    /**
     * Used to test that the conversion from the hex string used to create the
     * locator produces the correct int value.
     * 
     * @throws Exception
     */
    public void testConstructorConversion() throws Exception
    {
        NetworkLocator nl1 = createLocator("network://0xfba.0x123");
        assertEquals("The hex string for transport_id was not properly converted.", 4026, nl1.getTransportID());
        assertEquals("The hex string for network_id was not properly converted.", 291, nl1.getNetworkID());

        NetworkLocator nl2 = createLocator("network://0x123.0xfba");
        assertEquals("The hex string for transport_id was not properly converted.", 291, nl2.getTransportID());
        assertEquals("The hex string for network_id was not properly converted.", 4026, nl2.getNetworkID());
    }

    /**
     * Used to test various "good" & "bad" URLs to the
     * <code>NetworkLocator</code> constructor to ensure that the Locator is
     * created successfully or the correct InvalidLocatorException is thrown.
     * 
     * @throws Exception
     */
    public void testCreate() throws Exception
    {
        // create minimum-length network locator
        try
        {
            createLocator("network://0x1.0x1");
            // success case - keep on going
        }
        catch (InvalidLocatorException e)
        {
            fail("The locator creation should have succeeded w/o throwing an InvalidLocatorException.");
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        // create maximum-length network locator
        try
        {
            createLocator("network://0x1234.0x1234");
            // success case - keep on going
        }
        catch (InvalidLocatorException e)
        {
            fail("The locator creation should have succeeded w/o throwing an InvalidLocatorException.");
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network://0x1.0x1z3)");
            fail("The locator creation should have failed - invalid characters in hex string.");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network://0x1.0x123234)");
            fail("The locator creation should have failed - number too large.");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.  Caught: "
                    + ex.getClass().getName());
        }

        try
        {
            createLocator("network://0xfffff.0x12)");
            fail("The locator creation should have failed - number too large.");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.  Caught: "
                    + ex.getClass().getName());
        }

        try
        {
            createLocator("network//0x12)");
            fail("The locator creation should have failed - no \":\" separator");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network//0x2.0x12)");
            fail("The locator creation should have failed - no \":\" separator");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network:/0x12)");
            fail("The locator creation should have failed - missing a /");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network:/0x.f.0x12)");
            fail("The locator creation should have failed - missing a /");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network://12");
            fail("The locator creation should have failed - no leading 0x to the ID");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network://1212121212");
            fail("The locator creation should have failed - no leading 0x to the ID");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network://0x121212");
            fail("The locator creation should have failed - no leading 0x to the ID");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network://0x5.12");
            fail("The locator creation should have failed - no leading 0x to the ID");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            createLocator("network://05.0x12");
            fail("The locator creation should have failed - no leading 0x to the ID");
        }
        catch (InvalidLocatorException e)
        {
            // success case - keep on going
        }
        catch (Exception ex)
        {
            fail("Only InvalidLocatorException should be thrown from the NetworkLocator constructor.");
        }

        try
        {
            new NetworkLocator(-1, 100);
            fail("The locator creation should have failed - invalid transportID");
        }
        catch (InvalidLocatorException ex)
        {
            // success case - keep on going
        }
        try
        {
            new NetworkLocator(100, -100);
            fail("The locator creation should have failed - invalid transportID");
        }
        catch (InvalidLocatorException ex)
        {
            // Expected
        }
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
        TestSuite suite = new TestSuite(NetworkLocatorTest.class);
        return suite;
    }

    public NetworkLocatorTest(String name)
    {
        super(name);
    }

    /**
     * Should be overridden by subclass test.
     */
    protected NetworkLocator createLocator(String url) throws Exception
    {
        return new NetworkLocator(url);
    }
}
