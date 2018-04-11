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

package org.cablelabs.impl.manager.system;

import org.ocap.system.SystemModule;
import org.ocap.system.SystemModuleHandler;
import org.ocap.system.SystemModuleRegistrar;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.SystemManager;
import org.cablelabs.test.TestUtils;
import junit.framework.*;

/**
 * Tests SystemModuleRegistrarImpl & SystemModuleMgr
 */
public class SystemModuleTest extends TestCase
{
    /**
     * Ensure that both classes don't have public constructors
     */
    public void testConstructor() throws Exception
    {
        if (DEBUG) System.out.println("testConstructor()");

        TestUtils.testNoPublicConstructors(SystemModuleMgr.class);
        TestUtils.testNoPublicConstructors(SystemModuleRegistrarImpl.class);
    }

    /**
     * Tests getInstance() for both the module and proxy classes
     */
    public void testGetInstance() throws Exception
    {
        if (DEBUG) System.out.println("testGetInstance()");

        assertTrue("SystemModuleRegistrarImpl must be a SystemModuleRegistrar",
                registrar instanceof SystemModuleRegistrar);

        assertTrue("SystemModuleMgr must be a Manager", mgr instanceof org.cablelabs.impl.manager.Manager);
    }

    /**
     * Tests SAS Handler
     * 
     * This verifies that a SystemModuleHandler can register with an unused
     * privateHostAppID, and the registrar will build a new SystemModule and
     * return via the handler's ready() method in the same context in which the
     * SystemModuleHandler was registered.
     */
    public void testSASHandler() throws Exception
    {
        if (DEBUG) System.out.println("testSASHandler()");

        // test setup

        mySystemModuleHandler sasHandler1 = new mySystemModuleHandler();
        mySystemModuleHandler sasHandler2 = new mySystemModuleHandler();

        CallerContext currCtx = ccm.getCurrentContext();

        assertFalse("sasHandler1 should not be ready", sasHandler1.isReady());
        assertFalse("sasHandler1 should not be unregistered", sasHandler1.isUnregistered());
        assertFalse("sasHandler2 should not be ready", sasHandler2.isReady());
        assertFalse("sasHandler2 should not be unregistered", sasHandler2.isUnregistered());

        // register 1st SAS Handler w/ unused privateHostAppID (should succeed)

        registrar.registerSASHandler(sasHandler1, hostAppID);

        synchronized (sasHandler1.getWaitObject())
        {
            if (sasHandler1.isReady() == false)
            {
                try
                {
                    sasHandler1.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertTrue("sasHandler should be ready", sasHandler1.isReady());
        assertFalse("sasHandler should not be unregistered", sasHandler1.isUnregistered());
        assertFalse("sasHandler2 should not be ready", sasHandler2.isReady());
        assertFalse("sasHandler2 should not be unregistered", sasHandler2.isUnregistered());

        assertEquals("sasHandler1 should be should be should be called w/ correct CallerContext", currCtx,
                sasHandler1.getCurrCtx());

        // re-register 1st SAS Handler (should fail)

        boolean succeed1 = false;
        try
        {
            registrar.registerSASHandler(sasHandler1, hostAppID);
        }
        catch (IllegalArgumentException e)
        {
            succeed1 = true;
        }
        assertTrue("re-registration of sasHandler1 should throw IllegalArgumentException", succeed1);

        assertTrue("sasHandler should be ready", sasHandler1.isReady());
        assertFalse("sasHandler should not be unregistered", sasHandler1.isUnregistered());
        assertFalse("sasHandler2 should not be ready", sasHandler2.isReady());
        assertFalse("sasHandler2 should not be unregistered", sasHandler2.isUnregistered());

        // register 2nd SAS Handler w/ unused privateHostAppID (should succeed)

        registrar.registerSASHandler(sasHandler2, hostAppID);

        synchronized (sasHandler2.getWaitObject())
        {
            if (sasHandler2.isReady() == false)
            {
                try
                {
                    sasHandler2.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertFalse("sasHandler should not be ready", sasHandler1.isReady());
        assertTrue("sasHandler should be unregistered", sasHandler1.isUnregistered());
        assertTrue("sasHandler2 should be ready", sasHandler2.isReady());
        assertFalse("sasHandler2 should not be unregistered", sasHandler2.isUnregistered());

        assertEquals("sasHandler1 should be should be should be called w/ correct CallerContext", currCtx,
                sasHandler1.getCurrCtx());
        assertEquals("sasHandler2 should be should be should be called w/ correct CallerContext", currCtx,
                sasHandler2.getCurrCtx());

        // unregister SAS Handler by privateHostAppId (should succeed)

        registrar.unregisterSASHandler(hostAppID);

        synchronized (sasHandler2.getWaitObject())
        {
            if (sasHandler2.isReady() == true)
            {
                try
                {
                    sasHandler2.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertFalse("sasHandler should not be ready", sasHandler1.isReady());
        assertTrue("sasHandler should be unregistered", sasHandler1.isUnregistered());
        assertFalse("sasHandler2 should not be ready", sasHandler2.isReady());
        assertTrue("sasHandler2 should be unregistered", sasHandler2.isUnregistered());

        assertEquals("sasHandler1 should be should be called w/ correct CallerContext", currCtx,
                sasHandler1.getCurrCtx());
        assertEquals("sasHandler2 should be should be called w/ correct CallerContext", currCtx,
                sasHandler2.getCurrCtx());

        // unregister an unregistered SAS Handler by privateHostAppId (should
        // fail)

        boolean succeed2 = false;
        try
        {
            registrar.unregisterSASHandler(hostAppID);
        }
        catch (IllegalArgumentException e)
        {
            succeed2 = true;
        }
        assertTrue("unregistration of unregistered handler should have thrown IllegalArgumentException", succeed2);

        assertFalse("sasHandler should not be ready", sasHandler1.isReady());
        assertTrue("sasHandler should be unregistered", sasHandler1.isUnregistered());
        assertFalse("sasHandler2 should not be ready", sasHandler2.isReady());
        assertTrue("sasHandler2 should be unregistered", sasHandler2.isUnregistered());

        // register 2nd SAS Handler w/ unused privateHostAppID (should succeed)

        registrar.registerSASHandler(sasHandler2, hostAppID);

        synchronized (sasHandler2.getWaitObject())
        {
            if (sasHandler2.isReady() == false)
            {
                try
                {
                    sasHandler2.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertFalse("sasHandler should not be ready", sasHandler1.isReady());
        assertTrue("sasHandler should be unregistered", sasHandler1.isUnregistered());
        assertTrue("sasHandler2 should be ready", sasHandler2.isReady());
        assertFalse("sasHandler2 should not be unregistered", sasHandler2.isUnregistered());

        assertEquals("sasHandler1 should be should be called w/ correct CallerContext", currCtx,
                sasHandler1.getCurrCtx());
        assertEquals("sasHandler2 should be should be called w/ correct CallerContext", currCtx,
                sasHandler2.getCurrCtx());

        // unregister SAS Handler by handler (should succeed)

        registrar.unregisterSASHandler(sasHandler2);

        synchronized (sasHandler2.getWaitObject())
        {
            if (sasHandler2.isReady() == true)
            {
                try
                {
                    sasHandler2.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertFalse("sasHandler should not be ready", sasHandler1.isReady());
        assertTrue("sasHandler should be unregistered", sasHandler1.isUnregistered());
        assertFalse("sasHandler2 should not be ready", sasHandler2.isReady());
        assertTrue("sasHandler2 should be unregistered", sasHandler2.isUnregistered());

        assertEquals("sasHandler2 should be should be called w/ correct CallerContext", currCtx,
                sasHandler2.getCurrCtx());

        // unregister an unregistered SAS Handler by handler (should fail)

        boolean succeed3 = false;
        try
        {
            registrar.unregisterSASHandler(sasHandler2);
        }
        catch (IllegalArgumentException e)
        {
            succeed3 = true;
        }
        assertTrue("unregistration of unregistered handler should have thrown IllegalArgumentException", succeed3);

        assertFalse("sasHandler should not be ready", sasHandler1.isReady());
        assertTrue("sasHandler should be unregistered", sasHandler1.isUnregistered());
        assertFalse("sasHandler2 should not be ready", sasHandler2.isReady());
        assertTrue("sasHandler2 should be unregistered", sasHandler2.isUnregistered());

        // end of test
        return;
    }

    /**
     * Tests MMI Handler
     * 
     * Verifies that the registration of the Resident (first) MMI Handler works,
     * creates a new MMI module, and calls the handler's ready() method with the
     * new module. Also verifies that the registration of a second
     * (non-resident) MMI Handler usurps the Resident MMI Handler, and that an
     * unregistration of that non-resident MMI Handler causes the Resident MMI
     * Handler to be restored. Also verifies that only a single non-resident MMI
     * Handler can be registered at a time.
     */
    public void testMMIHandler() throws Exception
    {
        if (DEBUG) System.out.println("testMMIHandler()");

        // test setup

        mySystemModuleHandler residentMMIHandler = new mySystemModuleHandler();
        mySystemModuleHandler nonresidentMMIHandler = new mySystemModuleHandler();
        mySystemModuleHandler nonresidentMMIHandler2 = new mySystemModuleHandler();

        CallerContext currCtx = ccm.getCurrentContext();

        // register Resident MMI Handler (should succeed)

        assertFalse("residentMMIHandler should not be ready", residentMMIHandler.isReady());
        assertFalse("residentMMIHandler should not be registered", residentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler2.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler2.isUnregistered());

        registrar.registerMMIHandler(residentMMIHandler);

        synchronized (residentMMIHandler.getWaitObject())
        {
            if (residentMMIHandler.isReady() == false)
            {
                try
                {
                    residentMMIHandler.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertTrue("residentMMIHandler should be ready", residentMMIHandler.isReady());
        assertFalse("residentMMIHandler should not be registered", residentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler2.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler2.isUnregistered());

        assertEquals("residentMMIHandler should be called w/ correct CallerContext", currCtx,
                residentMMIHandler.getCurrCtx());

        // register non-Resident MMI Handler (should succeed and usurp Resident
        // MMI Handler)

        registrar.registerMMIHandler(nonresidentMMIHandler);

        synchronized (nonresidentMMIHandler.getWaitObject())
        {
            if (nonresidentMMIHandler.isReady() == false)
            {
                try
                {
                    nonresidentMMIHandler.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertFalse("residentMMIHandler should not be ready", residentMMIHandler.isReady());
        assertTrue("residentMMIHandler should be registered", residentMMIHandler.isUnregistered());
        assertTrue("non-residentMMIHandler should be ready", nonresidentMMIHandler.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler2.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler2.isUnregistered());

        assertEquals("residentMMIHandler should be called w/ correct CallerContext", currCtx,
                residentMMIHandler.getCurrCtx());
        assertEquals("non-residentMMIHandler should be called w/ correct CallerContext", currCtx,
                nonresidentMMIHandler.getCurrCtx());

        // re-register non-Resident MMI Handler (nothing should happen)

        registrar.registerMMIHandler(nonresidentMMIHandler);

        synchronized (nonresidentMMIHandler.getWaitObject())
        {
            if (nonresidentMMIHandler.isReady() == false)
            {
                try
                {
                    nonresidentMMIHandler.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertFalse("residentMMIHandler should not be ready", residentMMIHandler.isReady());
        assertTrue("residentMMIHandler should be registered", residentMMIHandler.isUnregistered());
        assertTrue("non-residentMMIHandler should be ready", nonresidentMMIHandler.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler2.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler2.isUnregistered());

        // try to register 2nd non-Resident MMI Handler (exception should get
        // thrown)

        boolean succeed1 = false;
        try
        {
            registrar.registerMMIHandler(nonresidentMMIHandler2);
        }
        catch (IllegalArgumentException e)
        {
            succeed1 = true;
        }
        assertTrue("2nd non-resident MMI Handler throws IllegalArgumentException", succeed1);

        assertFalse("residentMMIHandler should not be ready", residentMMIHandler.isReady());
        assertTrue("residentMMIHandler should be registered", residentMMIHandler.isUnregistered());
        assertTrue("non-residentMMIHandler should be ready", nonresidentMMIHandler.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler2.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler2.isUnregistered());

        // unregister non-Resident MMI Handler (resident MMI Handler should get
        // restored)

        registrar.unregisterMMIHandler();

        synchronized (residentMMIHandler.getWaitObject())
        {
            if (residentMMIHandler.isReady() == false)
            {
                try
                {
                    residentMMIHandler.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
        synchronized (nonresidentMMIHandler.getWaitObject())
        {
            if (nonresidentMMIHandler.isReady() == true)
            {
                try
                {
                    nonresidentMMIHandler.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertTrue("residentMMIHandler should be ready", residentMMIHandler.isReady());
        assertFalse("residentMMIHandler should not be registered", residentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler.isReady());
        assertTrue("non-residentMMIHandler should be registered", nonresidentMMIHandler.isUnregistered());
        assertFalse("non-residentMMIHandler should not be ready", nonresidentMMIHandler2.isReady());
        assertFalse("non-residentMMIHandler should not be registered", nonresidentMMIHandler2.isUnregistered());

        assertEquals("residentMMIHandler should be called w/ correct CallerContext", currCtx,
                residentMMIHandler.getCurrCtx());
        assertEquals("non-residentMMIHandler should be called w/ correct CallerContext", currCtx,
                nonresidentMMIHandler.getCurrCtx());

        // end of test
        return;
    }

    /**
     * Tests registerSASHandler and the send/receive capability.
     * 
     * This verifies that a SystemModuleHandler can register with an unused
     * privateHostAppID, and the registrar will build a new SystemModule and
     * return via the handler's ready() method in the same context in which the
     * SystemModuleHandler was registered. After registration verification is
     * made that APDU's can be sent and received witht the POD application.
     */
    public void testSASCaHandler() throws Exception
    {
        if (DEBUG) System.out.println("testSASCaHandler()");

        // test setup

        mySystemModuleHandler sasHandler1 = new mySystemModuleHandler();

        CallerContext currCtx = ccm.getCurrentContext();

        assertFalse("sasHandler1 should not be ready", sasHandler1.isReady());
        assertFalse("sasHandler1 should not be unregistered", sasHandler1.isUnregistered());

        // register 1st SAS Handler w/ unused privateHostAppID (should succeed)

        registrar.registerSASHandler(sasHandler1, caAppId);

        synchronized (sasHandler1.getWaitObject())
        {
            if (sasHandler1.isReady() == false)
            {
                try
                {
                    sasHandler1.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertTrue("sasHandler should be ready", sasHandler1.isReady());
        assertFalse("sasHandler should not be unregistered", sasHandler1.isUnregistered());

        assertEquals("sasHandler1 should be should be called w/ correct CallerContext", currCtx,
                sasHandler1.getCurrCtx());

        // Then send a data request APDU to the Ca application and wait for
        // reply

        sasHandler1.sendAPDU(SAS_DATA_RQST, null);

        synchronized (sasHandler1.getWaitObject())
        {
            if (sasHandler1.getRcvLengthField() > 0)
            {
                try
                {
                    sasHandler1.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        // Should see this one as a response to our SAS_DATA_RQST.
        // Format should follow OC-SP-CC-IF-I16-040402 Table 7.11-G
        if (sasHandler1.getRcvApduTag() != SAS_DATA_AV)
        {
            fail("didn't receive expected SAS_DATA_AV response to SAS_DATA_RQST");
        }

        assertTrue("expected reply to have length > 0", sasHandler1.getRcvLengthField() > 0);
        assertTrue("expected reply data bytes to have length > 0", sasHandler1.getRcvDataBytes().length > 0);

        // unregister SAS Handler by handler (should succeed)

        registrar.unregisterSASHandler(sasHandler1);

        synchronized (sasHandler1.getWaitObject())
        {
            if (sasHandler1.isReady() == true)
            {
                try
                {
                    sasHandler1.getWaitObject().wait(waitObjectTimeout);
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        assertFalse("sasHandler should not be ready", sasHandler1.isReady());
        assertTrue("sasHandler should be unregistered", sasHandler1.isUnregistered());

        assertEquals("sasHandler1 should be should be called w/ correct CallerContext", currCtx,
                sasHandler1.getCurrCtx());

        // end of test
        return;
    }

    /**
     * The JUnit setup method
     */
    protected void setUp() throws Exception
    {
        super.setUp();

    }

    /**
     * The teardown method for JUnit
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * The main program for the SystemModuleTest class
     * 
     * @param args
     *            The command line arguments
     */
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

    /**
     * A unit test suite for JUnit
     * 
     * @return The test suite
     */
    public static Test suite()
    {
        // This test requires that some of the tests be run
        // in a particular order, so we must add the tests manually
        TestSuite suite = new TestSuite();
        suite.addTest(new SystemModuleTest("testConstructor"));
        suite.addTest(new SystemModuleTest("testGetInstance"));
        suite.addTest(new SystemModuleTest("testMMIHandler"));
        suite.addTest(new SystemModuleTest("testSASHandler"));

        return suite;
    }

    /**
     *Constructor for the SystemModuleTest object
     * 
     * @param name
     *            Test Name
     */
    public SystemModuleTest(String name)
    {
        super(name);
    }

    private final SystemModuleMgr mgr = (SystemModuleMgr) ManagerManager.getInstance(SystemManager.class);

    private final SystemModuleRegistrarImpl registrar = (SystemModuleRegistrarImpl) org.ocap.system.SystemModuleRegistrar.getInstance();

    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private final static boolean DEBUG = false;

    /* The hostAppID used for SAS tests */
    protected final static byte[] hostAppID = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01 };

    // From powertv.h: Id for the CA application.
    protected final byte[] caAppId = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01 };

    // Number of ms that the tests will wait for Handler callbacks to occur
    protected static int waitObjectTimeout = 10000;

    // SAS APDU definitions:
    // private static final int SAS_CONNCECT_RQST = 0x009F9A00;
    // private static final int SAS_CONNCECT_CNF = 0x009F9A01;
    private static final int SAS_DATA_RQST = 0x009F9A02;

    private static final int SAS_DATA_AV = 0x009F9A03;
    // private static final int SAS_DATA_AV_CNF = 0x009F9A04;
    // private static final int SAS_SERVER_QUERY = 0x009F9A05;
    // private static final int SAS_SERVER_REPLY = 0x009F9A06;

    // PowerTV CA Handler specific definitions:
    // TODO: Hard coding the application specific data here is a temporary
    // solution. This should
    // be abstracted into some properies data file so that the message specific
    // testing per
    // application per platform can be conducted by generalized junit test code.
    // private static final byte kOcm_Sas_Host2PodReq = 0x01;
    // private static final byte kOcm_Sas_Host2PodRsp = 0x02;
    // private static final byte kOcm_Sas_Pod2HostReq = 0x03;
    // private static final byte kOcm_Sas_Pod2HostRsp = 0x04;
    // private static final byte kOcm_Sas_TestId = 0x00;

}

class mySystemModuleHandler implements SystemModuleHandler
{
    private boolean ready = false;

    private boolean unregistered = false;

    private Object waitObject = new Object();

    private CallerContextManager ccm;

    private CallerContext currCtx = null;

    private SystemModule sysMod = null;

    private int rcvApduTag = 0;

    private int rcvLengthField = 0;

    private byte[] rcvDataBytes = null;

    public mySystemModuleHandler()
    {
        this.ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    }

    public boolean isReady()
    {
        return this.ready;
    }

    public boolean isUnregistered()
    {
        return this.unregistered;
    }

    public CallerContext getCurrCtx()
    {
        return this.currCtx;
    }

    public Object getWaitObject()
    {
        return this.waitObject;
    }

    public void sendAPDU(int apduTag, byte[] dataBytes)
    {
        this.sysMod.sendAPDU(apduTag, dataBytes);
    }

    public int getRcvApduTag()
    {
        return this.rcvApduTag;
    }

    public int getRcvLengthField()
    {
        return this.rcvLengthField;
    }

    public byte[] getRcvDataBytes()
    {
        return this.rcvDataBytes;
    }

    public void ready(SystemModule systemModule)
    {
        synchronized (waitObject)
        {
            this.ready = true;
            this.unregistered = false;
            this.sysMod = systemModule;
            this.currCtx = ccm.getCurrentContext();
            waitObject.notifyAll();
        }
    }

    public void notifyUnregister()
    {
        synchronized (waitObject)
        {
            this.unregistered = true;
            this.ready = false;
            this.sysMod = null;
            this.currCtx = ccm.getCurrentContext();
            waitObject.notifyAll();
        }
    }

    public void receiveAPDU(int apduTag, int lengthField, byte[] dataByte)
    {
        synchronized (waitObject)
        {
            this.rcvApduTag = apduTag;
            this.rcvLengthField = lengthField;
            this.rcvDataBytes = dataByte;
            this.currCtx = ccm.getCurrentContext();
            waitObject.notifyAll();
        }
    }

    public void sendAPDUFailed(int apduTag, byte[] dataByte)
    {
        synchronized (waitObject)
        {
            this.currCtx = ccm.getCurrentContext();
            waitObject.notifyAll();
        }
    }
}
