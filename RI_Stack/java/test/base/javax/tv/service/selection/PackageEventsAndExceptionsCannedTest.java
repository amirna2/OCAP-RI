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

/**
 * 
 */
package javax.tv.service.selection;

import javax.tv.locator.Locator;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.ocap.net.OcapLocator;

import org.cablelabs.test.SICannedConcreteTest;

/**
 * This is a simple test case that tests each of the exception and event classes
 * in this package. Since all of the events and exceptions are relatively
 * simple, they have been rolled up into one test case for better manageability.
 * 
 * @author Joshua Keplinger
 * 
 */
public class PackageEventsAndExceptionsCannedTest extends SICannedConcreteTest
{

    /**
     * @param name
     */
    public PackageEventsAndExceptionsCannedTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * This simple creates a test suite containing the tests in this class.
     * 
     * @return A TestSuite object containing the tests in this class.
     */
    public static TestSuite suite()
    {
        TestSuite ts = new TestSuite(PackageEventsAndExceptionsCannedTest.class);
        return ts;
    }

    // Test section

    /**
     * Tests the AlternativeContentEvent class
     */
    public void testAlternativeContentEvent() throws Exception
    {
        ServiceContext sc = ServiceContextFactory.getInstance().createServiceContext();
        try
        {
            AlternativeContentEvent ace = new AlternativeContentEvent(sc);
            assertEquals("Returned ServiceContext does not match", sc, ace.getSource());

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Tests the InsufficentResourcesException class
     */
    public void testInsufficientResourcesException()
    {
        InsufficientResourcesException irex = new InsufficientResourcesException();
        assertNull("Message should be null", irex.getMessage());
        irex = new InsufficientResourcesException("Some reason");
        assertEquals("Message does not match", "Some reason", irex.getMessage());
    }

    /**
     * Tests the InvalidServiceComponentException class
     */
    public void testInvalidServiceComponentException() throws Exception
    {
        Locator loc = new OcapLocator(100);
        InvalidServiceComponentException iscx = new InvalidServiceComponentException(loc);
        assertEquals("Locator does not match", loc, iscx.getInvalidServiceComponent());
        iscx = new InvalidServiceComponentException(loc, "some reason");
        assertEquals("Locator does not match", loc, iscx.getInvalidServiceComponent());
        assertEquals("Message does not match", "some reason", iscx.getMessage());
    }

    /**
     * Tests the ServiceContextEvent class
     */
    public void testServiceContextEvent() throws Exception
    {
        ServiceContext sc = ServiceContextFactory.getInstance().createServiceContext();
        try
        {
            ServiceContextEvent sce = new ServiceContextEvent(sc);
            assertEquals("Returned ServiceContext does not match", sc, sce.getServiceContext());

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Tests the ServiceContextException class
     */
    public void testServiceContextException()
    {
        ServiceContextException scex = new ServiceContextException();
        assertNull("Message should be null", scex.getMessage());
        scex = new ServiceContextException("Some reason");
        assertEquals("Message does not match", "Some reason", scex.getMessage());
    }

    /**
     * Tests the NormalContentEvent class
     */
    public void testNormalContentEvent() throws Exception
    {
        ServiceContext sc = ServiceContextFactory.getInstance().createServiceContext();
        try
        {
            NormalContentEvent nce = new NormalContentEvent(sc);
            assertEquals("Returned ServiceContext does not match", sc, nce.getSource());

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Tests the PresentationChangedEvent class
     */
    public void testPresentationChangedEvent() throws Exception
    {
        ServiceContext sc = ServiceContextFactory.getInstance().createServiceContext();
        try
        {
            PresentationChangedEvent pce = new PresentationChangedEvent(sc);
            assertEquals("Returned ServiceContext does not match", sc, pce.getSource());

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Tests the PresentationTerminatedEvent class
     */
    public void testPresentationTerminatedEvent() throws Exception
    {
        ServiceContext sc = ServiceContextFactory.getInstance().createServiceContext();
        try
        {
            PresentationTerminatedEvent pte = new PresentationTerminatedEvent(sc,
                    PresentationTerminatedEvent.TUNED_AWAY);
            assertEquals("Returned ServiceContext does not match", sc, pte.getSource());
            assertEquals("Returned reason code is incorrect", PresentationTerminatedEvent.TUNED_AWAY, pte.getReason());

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Tests the SelectionFailedEvent class
     */
    public void testSelectionFailedEvent() throws Exception
    {
        ServiceContext sc = ServiceContextFactory.getInstance().createServiceContext();
        try
        {
            SelectionFailedEvent sfe = new SelectionFailedEvent(sc, SelectionFailedEvent.INTERRUPTED);
            assertEquals("Returned ServiceContext does not match", sc, sfe.getSource());
            assertEquals("Returned reason code is incorrect", SelectionFailedEvent.INTERRUPTED, sfe.getReason());

        }
        finally
        {
            sc.destroy();
        }
    }

    /**
     * Tests the ServiceContextDestroyedEvent class
     */
    public void testServiceContextDestroyedEvent() throws Exception
    {
        ServiceContext sc = ServiceContextFactory.getInstance().createServiceContext();
        try
        {
            ServiceContextDestroyedEvent scde = new ServiceContextDestroyedEvent(sc);
            assertEquals("Returned ServiceContext does not match", sc, scde.getSource());

        }
        finally
        {
            sc.destroy();
        }
    }
}
