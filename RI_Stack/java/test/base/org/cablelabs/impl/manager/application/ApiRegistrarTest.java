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

package org.cablelabs.impl.manager.application;

import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.AppStorageManager.ApiStorage;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.signalling.XAppEntry;

//import org.cablelabs.test.NanoHTTPD;
import org.cablelabs.impl.manager.AppStorageManagerTest.HttpD;
import org.cablelabs.impl.manager.AuthManager.AuthContext;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.ocap.system.RegisteredApiManager;
import org.ocap.system.RegisteredApiManagerTest;

/**
 * Tests the RegisteredApiManager.
 * 
 * @author Aaron Kamienski
 */
public class ApiRegistrarTest extends RegisteredApiManagerTest
{

    /** Http Driver. */
    private static/* NanoHTTPD */HttpD m_nh = null;

    /** Port to use for http server. */
    public static int PORT = 81;

    /**
     * Tests lookup().
     */
    public void testLookup() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        String[] names = { getName() + 1, getName() + 2, getName() + 3 };

        // Create dummy context
        DummyContext cc = new DummyContext();

        try
        {
            for (int i = 0; i < names.length; ++i)
            {
                ram.register(names[i], "1." + i, new File(APIDIR + "test-scdf.xml"), (short) (100 + i));

                String[] copy = new String[i + 1];
                System.arraycopy(names, 0, copy, 0, copy.length);

                RegisteredApi[] apis = ar.lookup(copy, cc);
                assertNotNull("Should not return a null array", apis);
                try
                {
                    assertEquals("Unexpected array length", i + 1, apis.length);

                    // Make sure each name is present...
                    RegisteredApi[] apis2 = new RegisteredApi[apis.length];
                    System.arraycopy(apis, 0, apis2, 0, apis2.length);
                    for (int j = 0; j <= i; ++j)
                    {
                        boolean found = false;
                        for (int idx = 0; idx < apis2.length; ++idx)
                        {
                            if (apis2[idx] != null && names[j].equals(apis2[idx].name))
                            {
                                checkApi(names[j], "1." + j, (short) (100 + j), apis2[idx]);

                                apis2[idx] = null;
                                found = true;
                                break;
                            }
                        }
                        assertTrue("lookup failed to return " + names[j], found);
                    }
                }
                finally
                {
                    // Remove from use
                    for (int j = 0; j < apis.length; ++j)
                    {
                        apis[j].removeFromUse(cc);
                    }
                }
            }
        }
        finally
        {
            for (int i = 0; i < names.length; ++i)
            {
                unregister(names[i]);
            }
            cc.dispose();
        }
    }

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests lookup() with a URL (ECN 1009).
     */
    /*
     * public void testUrlLookup() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * String[] names = { getName()+1, getName()+2, getName()+3 };
     * 
     * // Create dummy context DummyContext cc = new DummyContext();
     * 
     * try { for(int i = 0; i < names.length; ++i) { ram.register(names[i],
     * "1."+i, new URL("http", "127.0.0.1", PORT, "test-scdf.xml"),
     * (short)(100+i));
     * 
     * String[] copy = new String[i+1]; System.arraycopy(names, 0, copy, 0,
     * copy.length);
     * 
     * RegisteredApi[] apis = ar.lookup(copy, cc);
     * assertNotNull("Should not return a null array", apis); try {
     * assertEquals("Unexpected array length", i+1, apis.length);
     * 
     * // Make sure each name is present... RegisteredApi[] apis2 = new
     * RegisteredApi[apis.length]; System.arraycopy(apis, 0, apis2, 0,
     * apis2.length); for(int j = 0; j <= i; ++j) { boolean found = false;
     * for(int idx = 0; idx < apis2.length; ++idx) { if ( apis2[idx] != null &&
     * names[j].equals(apis2[idx].name) ) { checkApi(names[j], "1."+j,
     * (short)(100+j), apis2[idx]);
     * 
     * apis2[idx] = null; found = true; break; } }
     * assertTrue("lookup failed to return "+names[j], found); } } finally { //
     * Remove from use for(int j = 0; j < apis.length; ++j) {
     * apis[j].removeFromUse(cc); } } } } finally { for(int i = 0; i <
     * names.length; ++i) { unregister(names[i]); } cc.dispose(); }
     * 
     * 
     * }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests lookup() with no apis.
     */
    public void testLookup_none() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        DummyContext cc = new DummyContext();
        try
        {
            RegisteredApi[] apis = ar.lookup(new String[] { getName() }, cc);
            assertNotNull("Expected non-null array", apis);
            assertEquals("Expected no found apis", 0, apis.length);
        }
        finally
        {
            cc.dispose();
        }
    }

    /**
     * Tests lookup() for non-existent api. Including some existent and some
     * not.
     */
    public void testLookup_nonexistent() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        String[] names = { getName() + 1, getName() + 2, getName() + 3 };

        // Create dummy context
        DummyContext cc = new DummyContext();

        try
        {
            for (int i = 0; i < names.length; ++i)
            {
                ram.register(names[i], "1." + i, new File(APIDIR + "test-scdf.xml"), (short) (100 + i));

                // Lookup ALL names
                RegisteredApi[] apis = ar.lookup(names, cc);
                assertNotNull("Should not return a null array", apis);
                try
                {
                    assertEquals("Unexpected array length", i + 1, apis.length);

                    // Make sure each name is present...
                    RegisteredApi[] apis2 = new RegisteredApi[apis.length];
                    System.arraycopy(apis, 0, apis2, 0, apis2.length);
                    for (int j = 0; j <= i; ++j)
                    {
                        boolean found = false;
                        for (int idx = 0; idx < apis2.length; ++idx)
                        {
                            if (apis2[idx] != null && names[j].equals(apis2[idx].name))
                            {
                                checkApi(names[j], "1." + j, (short) (100 + j), apis2[idx]);

                                apis2[idx] = null;
                                found = true;
                                break;
                            }
                        }
                        assertTrue("lookup failed to return " + names[j], found);
                    }
                }
                finally
                {
                    // Remove from use
                    for (int j = 0; j < apis.length; ++j)
                    {
                        apis[j].removeFromUse(cc);
                    }
                }
            }
        }
        finally
        {
            for (int i = 0; i < names.length; ++i)
            {
                unregister(names[i]);
            }
            cc.dispose();
        }
    }

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests lookup() for non-existent api. Including some existent and some
     * not. Tests URL-based register() descrived in ECN-1009.
     */
    /*
     * public void testLookupNonexistentViaUrlRegister() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * String[] names = { getName()+1, getName()+2, getName()+3 };
     * 
     * // Create dummy context DummyContext cc = new DummyContext();
     * 
     * try { for(int i = 0; i < names.length; ++i) { ram.register(names[i],
     * "1."+i, new URL("http", "127.0.0.1", PORT, "test-scdf.xml"),
     * (short)(100+i));
     * 
     * // Lookup ALL names RegisteredApi[] apis = ar.lookup(names, cc);
     * assertNotNull("Should not return a null array", apis); try {
     * assertEquals("Unexpected array length", i+1, apis.length);
     * 
     * // Make sure each name is present... RegisteredApi[] apis2 = new
     * RegisteredApi[apis.length]; System.arraycopy(apis, 0, apis2, 0,
     * apis2.length); for(int j = 0; j <= i; ++j) { boolean found = false;
     * for(int idx = 0; idx < apis2.length; ++idx) { if ( apis2[idx] != null &&
     * names[j].equals(apis2[idx].name) ) { checkApi(names[j], "1."+j,
     * (short)(100+j), apis2[idx]);
     * 
     * apis2[idx] = null; found = true; break; } }
     * assertTrue("lookup failed to return "+names[j], found); } } finally { //
     * Remove from use for(int j = 0; j < apis.length; ++j) {
     * apis[j].removeFromUse(cc); } } } } finally { for(int i = 0; i <
     * names.length; ++i) { unregister(names[i]); } cc.dispose(); } }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests register(), IllegalStateException
     */
    public void testRegister_inUse() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        String name = getName() + 1;
        String ver = "0.1";
        String ver2 = "0.1a";

        int length = checkApi(name, ver, -1, false);

        // Register api
        ram.register(name, ver, new File(APIDIR + "test-scdf.xml"), (short) 100);

        DummyContext cc = new DummyContext();
        try
        {
            length = checkApi(name, ver, length + 1, true);

            // Mark as in-use using lookup
            RegisteredApi api[] = ar.lookup(new String[] { name }, cc);
            assertEquals("Unexpected number of apis found", 1, api.length);

            // Attempt to register a new version
            try
            {
                ram.register(name, ver2, new File(APIDIR + "test-scdf.xml"), (short) 200);
                fail("Expected an IllegalStateException");
            }
            catch (IllegalStateException e)
            { /* expected */
            }
            finally
            {
                api[0].removeFromUse(cc);
            }
        }
        finally
        {
            cc.dispose();
            ram.unregister(name);
        }
    }

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests register(), IllegalStateException Tests URL-based register()
     * descrived in ECN-1009.
     */
    /*
     * public void testRegisterInUseViaURL() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * String name = getName()+1; String ver = "0.1"; String ver2 = "0.1a";
     * 
     * int length = checkApi(name, ver, -1, false);
     * 
     * // Register api ram.register(name, ver, new URL("http", "127.0.0.1",
     * PORT, "test-scdf.xml"), (short)100);
     * 
     * DummyContext cc = new DummyContext(); try { length = checkApi(name, ver,
     * length+1, true);
     * 
     * // Mark as in-use using lookup RegisteredApi api[] = ar.lookup(new
     * String[] {name}, cc); assertEquals("Unexpected number of apis found", 1,
     * api.length);
     * 
     * // Attempt to register a new version try { ram.register(name, ver2, new
     * URL("http", "127.0.0.1", PORT, "test-scdf.xml"), (short)200);
     * fail("Expected an IllegalStateException"); } catch(IllegalStateException
     * e) { expected } finally { api[0].removeFromUse(cc); } } finally {
     * cc.dispose(); ram.unregister(name); } }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests register(), IllegalStateException, and verifies that it doesn't
     * matter what method (File or URL) the caller used to register API.
     * 
     * Tests URL-based register() descrived in ECN-1009.
     */
    /*
     * public void testRegisterInUseViaURLandFile() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * String name = getName()+1; String ver = "0.1"; String ver2 = "0.1a";
     * 
     * int length = checkApi(name, ver, -1, false);
     * 
     * // Register api ram.register(name, ver, new URL("http", "127.0.0.1",
     * PORT, "test-scdf.xml"), (short)100);
     * 
     * DummyContext cc = new DummyContext(); try { length = checkApi(name, ver,
     * length+1, true);
     * 
     * // Mark as in-use using lookup RegisteredApi api[] = ar.lookup(new
     * String[] {name}, cc); assertEquals("Unexpected number of apis found", 1,
     * api.length);
     * 
     * // Attempt to register a new version try { ram.register(name, ver2, new
     * File(APIDIR + "test-scdf.xml"), (short)200);
     * fail("Expected an IllegalStateException"); } catch(IllegalStateException
     * e) { expected } finally { api[0].removeFromUse(cc); } } finally {
     * cc.dispose(); ram.unregister(name); } }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests register(), existing API not-in-use.
     * 
     * @see RegisteredApiManagerTest#testRegister_replace()
     */
    public void testRegister_notInUse() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        String name = getName() + 1;
        String ver = "0.2";
        String ver2 = "0.2a";

        int length = checkApi(name, ver, -1, false);

        // Register api
        ram.register(name, ver, new File(APIDIR + "test-scdf.xml"), (short) 100);

        DummyContext cc = new DummyContext();
        try
        {
            length = checkApi(name, ver, length + 1, true);

            // Verify that original files were stored.
            AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
            ApiStorage api = asm.retrieveApi(name, ver);
            assertNotNull("Expected API to be stored", api);
            assertEquals("Unexpected version stored", ver, api.getVersion());

            ram.register(name, ver2, new File(APIDIR + "test-scdf.xml"), (short) 99); // lesser
                                                                                      // priority
                                                                                      // shouldn't
                                                                                      // matter
                                                                                      // --
                                                                                      // as
                                                                                      // should
                                                                                      // delete
                                                                                      // anyhow

            // Should succeed!
            checkApi(name, ver2, length, true);

            // Verify that new files were stored
            api = asm.retrieveApi(name, ver2);
            assertNotNull("Expected API to be stored", api);
            assertEquals("Unexpected version stored", ver2, api.getVersion());

            // Verify that original files were deleted!!!
            api = asm.retrieveApi(name, ver);
            assertNull("Expected original API to be deleted", api);
        }
        finally
        {
            cc.dispose();
            ram.unregister(name);
        }
    }

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests register(), existing API not-in-use.
     * 
     * Tests URL-based register() descrived in ECN-1009.
     * 
     * @see RegisteredApiManagerTest#testRegister_replace()
     */
    /*
     * public void testRegisterNotInUseViaUrl() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * String name = getName()+1; String ver = "0.2"; String ver2 = "0.2a";
     * 
     * int length = checkApi(name, ver, -1, false);
     * 
     * // Register api ram.register(name, ver, new URL("http", "127.0.0.1",
     * PORT, "test-scdf.xml"), (short)100);
     * 
     * DummyContext cc = new DummyContext(); try { length = checkApi(name, ver,
     * length+1, true);
     * 
     * // Verify that original files were stored. AppStorageManager asm =
     * (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
     * ApiStorage api = asm.retrieveApi(name, ver);
     * assertNotNull("Expected API to be stored", api);
     * assertEquals("Unexpected version stored", ver, api.getVersion());
     * 
     * ram.register(name, ver2, new URL("http", "127.0.0.1", PORT,
     * "test-scdf.xml"), (short)99); // lesser priority shouldn't matter -- as
     * should delete anyhow
     * 
     * // Should succeed! checkApi(name, ver2, length, true);
     * 
     * // Verify that new files were stored api = asm.retrieveApi(name, ver2);
     * assertNotNull("Expected API to be stored", api);
     * assertEquals("Unexpected version stored", ver2, api.getVersion());
     * 
     * // Verify that original files were deleted!!! api = asm.retrieveApi(name,
     * ver); assertNull("Expected original API to be deleted", api); } finally {
     * cc.dispose(); ram.unregister(name); } }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests unregister(), IllegalStateException
     */
    public void testUnRegister_inUse() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        String name = getName() + 1;
        String ver = "0.1";

        int length = checkApi(name, ver, -1, false);

        // Register api
        ram.register(name, ver, new File(APIDIR + "test-scdf.xml"), (short) 100);

        DummyContext cc = new DummyContext();
        try
        {
            length = checkApi(name, ver, length + 1, true);

            // Mark as in-use using lookup
            RegisteredApi api[] = ar.lookup(new String[] { name }, cc);
            assertEquals("Unexpected number of apis found", 1, api.length);

            // Attempt to unregister
            try
            {
                ram.unregister(name);
                fail("Expected an IllegalStateException");
            }
            catch (IllegalStateException e)
            { /* expected */
            }
            finally
            {
                api[0].removeFromUse(cc);
            }
        }
        finally
        {
            cc.dispose();
            ram.unregister(name);
        }
    }

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests unregister(), IllegalStateException
     */
    /*
     * public void testUnRegisterInUseViaURL() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * String name = getName()+1; String ver = "0.1";
     * 
     * int length = checkApi(name, ver, -1, false);
     * 
     * // Register api ram.register(name, ver, new URL("http", "127.0.0.1",
     * PORT, "test-scdf.xml"), (short)100);
     * 
     * DummyContext cc = new DummyContext(); try { length = checkApi(name, ver,
     * length+1, true);
     * 
     * // Mark as in-use using lookup RegisteredApi api[] = ar.lookup(new
     * String[] { name }, cc); assertEquals("Unexpected number of apis found",
     * 1, api.length);
     * 
     * // Attempt to unregister try { ram.unregister(name);
     * fail("Expected an IllegalStateException"); } catch(IllegalStateException
     * e) { expected } finally { api[0].removeFromUse(cc); } } finally {
     * cc.dispose(); ram.unregister(name); } }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests getUsedNames().
     */
    public void testGetUsedNames() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        String name = getName() + 1;
        String name2 = getName() + 2;
        String name3 = getName() + 3;

        DummyContext cc1 = new DummyContext();
        DummyContext cc2 = new DummyContext();
        replaceCCMgr();
        try
        {
            // Register two apis
            ram.register(name, "1", new File(APIDIR + "test-scdf.xml"), (short) 100);
            ram.register(name2, "1", new File(APIDIR + "test-scdf.xml"), (short) 100);
            ram.register(name3, "1", new File(APIDIR + "test-scdf.xml"), (short) 100);

            String[] names1 = { name, name3 };
            String[] names2 = { name2, name3 };

            // Lookup APIs for different CC's
            RegisteredApi[] api1 = ar.lookup(names1, cc1);
            RegisteredApi[] api2 = ar.lookup(names2, cc2);

            try
            {
                // Within two different CC's, call getUsedNames

                // Verify names
                checkNames(names1, getUsedNames(cc1));
                checkNames(names2, getUsedNames(cc2));
                // Again, to make sure that don't change
                checkNames(names1, getUsedNames(cc1));
                checkNames(names2, getUsedNames(cc2));
            }
            finally
            {
                // Forget lookups
                for (int i = 0; i < api1.length; ++i)
                    api1[i].removeFromUse(cc1);
                for (int i = 0; i < api2.length; ++i)
                    api2[i].removeFromUse(cc2);
            }
        }
        finally
        {
            unregister(name);
            unregister(name2);
            unregister(name3);

            restoreCCMgr();

            cc1.dispose();
            cc2.dispose();
        }
    }

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Tests getUsedNames(). Tests URL-based register() descrived in ECN-1009.
     */
    /*
     * public void testGetUsedNamesViaURL() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * String name = getName()+1; String name2 = getName()+2; String name3 =
     * getName()+3;
     * 
     * DummyContext cc1 = new DummyContext(); DummyContext cc2 = new
     * DummyContext(); replaceCCMgr(); try { // Register two apis
     * ram.register(name, "1", new URL("http", "127.0.0.1", PORT,
     * "test-scdf.xml"), (short)100); ram.register(name2, "1", new URL("http",
     * "127.0.0.1", PORT, "test-scdf.xml"), (short)100); ram.register(name3,
     * "1", new URL("http", "127.0.0.1", PORT, "test-scdf.xml"), (short)100);
     * 
     * String[] names1 = { name, name3 }; String[] names2 = { name2, name3 };
     * 
     * // Lookup APIs for different CC's RegisteredApi[] api1 =
     * ar.lookup(names1, cc1); RegisteredApi[] api2 = ar.lookup(names2, cc2);
     * 
     * try { // Within two different CC's, call getUsedNames
     * 
     * // Verify names checkNames(names1, getUsedNames(cc1)); checkNames(names2,
     * getUsedNames(cc2)); // Again, to make sure that don't change
     * checkNames(names1, getUsedNames(cc1)); checkNames(names2,
     * getUsedNames(cc2)); } finally { // Forget lookups for(int i = 0; i <
     * api1.length; ++i) api1[i].removeFromUse(cc1); for(int i = 0; i <
     * api2.length; ++i) api2[i].removeFromUse(cc2); } } finally {
     * unregister(name); unregister(name2); unregister(name3);
     * 
     * restoreCCMgr();
     * 
     * cc1.dispose(); cc2.dispose(); } }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Ensure that register() with a File relative to the CWD, in the CWD,
     * doesn't result in NPE.
     */
    public void testRegister_relativeCwd() throws Exception
    {
        ApiRegistrar ar = (ApiRegistrar) ram;

        // Move CWD...
        String newCwd = APIDIR;
        String oldCwd = System.getProperty("user.dir");
        System.setProperty("user.dir", newCwd);
        try
        {
            assertEquals("Internal error - could not set user.dir", newCwd, System.getProperty("user.dir"));

            ar.register("NoSuchApi", "1", new File("test-scdf.xml"), (short) 100);
        }
        catch (NullPointerException e)
        {
            fail("Should not get a NullPointerException");
        }
        finally
        {
            if (oldCwd != null)
                System.setProperty("user.dir", oldCwd);
            else
                System.getProperties().remove("user.dir");
        }
    }

    // Start - This method is commented due to removal of ECN 1009 changes from
    // spec
    /**
     * Ensure that register() with a File relative to the CWD, in the CWD,
     * doesn't result in NPE. Tests URL-based register() descrived in ECN-1009.
     */
    /*
     * public void testRegisterRelativeCwdViaUrl() throws Exception {
     * 
     * //Initialize http server; done here because not global to all tests. if
     * (m_nh == null) { //Establish http system. m_nh = new HttpD(PORT, new
     * File(APIDIR.substring(0, APIDIR.length() -1)), true); }
     * 
     * ApiRegistrar ar = (ApiRegistrar)ram;
     * 
     * // Move CWD... String newCwd = APIDIR; String oldCwd =
     * System.getProperty("user.dir"); System.setProperty("user.dir", newCwd);
     * try { assertEquals("Internal error - could not set user.dir", newCwd,
     * System.getProperty("user.dir"));
     * 
     * ar.register("NoSuchApi", "1", new URL("http", "127.0.0.1", PORT,
     * "test-scdf.xml"), (short)100); } catch (NullPointerException e) {
     * fail("Should not get a NullPointerException"); } finally { if ( oldCwd !=
     * null ) System.setProperty("user.dir", oldCwd); else
     * System.getProperties().remove("user.dir"); } }
     */
    // End - This method is commented due to removal of ECN 1009 changes from
    // spec

    private void checkNames(String[] orig, String[] actual)
    {
        assertNotNull("Array should not be null", actual);
        assertEquals("Unexpected array length", orig.length, actual.length);
        for (int i = 0; i < orig.length; ++i)
        {
            boolean found = true;
            for (int j = 0; j < actual.length; ++j)
            {
                if (actual[j] != null && orig[i].equals(actual[j]))
                {
                    actual[j] = null; // mark as found
                    found = true;
                    break;
                }
            }
            assertTrue("Did not find " + orig[i], found);
        }
    }

    private String[] getUsedNames(CallerContext cc) throws Exception
    {
        final String[][] pnames = { null };
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                pnames[0] = ram.getUsedNames();
            }
        });
        return pnames[0];
    }

    private void checkApi(String name, String version, short priority, RegisteredApi api)
    {
        assertEquals("Unexpected name", name, api.name);
        assertEquals("Unexpected version for " + name, version, api.version);
        assertEquals("Unexpected priority for " + name, priority, api.storagePriority);
    }

    protected RegisteredApiManager getInstance()
    {
        return new ApiRegistrar();
    }

    private CallerContextManager ccSave;

    private AppStorageManager asSave;

    private void replaceCCMgr()
    {
        ccSave = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(ccSave));
    }

    private void replaceAppStorageMgr()
    {
        asSave = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
        ManagerManagerTest.updateManager(AppStorageManager.class, DummyStorage.class, false, new DummyStorage());
    }

    private void restoreCCMgr()
    {
        if (ccSave != null)
            ManagerManagerTest.updateManager(CallerContextManager.class, ccSave.getClass(), true, ccSave);
    }

    private void restoreAppStorageMgr()
    {
        if (asSave != null) ManagerManagerTest.updateManager(AppStorageManager.class, asSave.getClass(), true, asSave);
    }

    /**
     * Test implementation of <code>AppStorageManager</code>.
     * 
     * @author Aaron Kamienski
     */
    public static class DummyStorage implements AppStorageManager
    {
        public final Hashtable stored = new Hashtable();

        public final Hashtable storedApi = new Hashtable();

        public final Hashtable locked = new Hashtable();

        public static class App
        {
            public final AppID id;

            public final long version;

            public App(AppID id, long version)
            {
                this.id = id;
                this.version = version;
            }

            public boolean equals(Object obj)
            {
                if (!(obj instanceof App)) return false;
                App other = (App) obj;
                return version == other.version && id.equals(other.id);
            }

            public int hashCode()
            {
                return id.hashCode() ^ ((int)(version & 0xFFFFFFFF));
            }
        }

        public static class Api
        {
            public final String name;

            public final String version;

            public Api(String name, String version)
            {
                this.name = name;
                this.version = version;
            }

            public boolean equals(Object obj)
            {
                if (!(obj instanceof Api)) return false;
                Api other = (Api) obj;
                return name.equals(other.name) && version.equals(other.version);
            }

            public int hashCode()
            {
                return name.hashCode() ^ version.hashCode();
            }
        }

        private class AppStorageImpl implements AppStorage
        {
            protected Object key;

            private File baseDir;

            AppStorageImpl(Object key, File baseDir)
            {
                this.key = key;
                this.baseDir = baseDir;
            }

            public File getBaseDirectory()
            {
                return baseDir;
            }

            public boolean isPreAuthenticated()
            {
                return false;
            }

            public boolean lock()
            {
                synchronized (locked)
                {
                    Integer i = (Integer) locked.get(key);
                    int count = (i == null) ? 1 : (i.intValue() + 1);
                    locked.put(key, new Integer(count));
                }
                return true;
            }

            public void unlock()
            {
                synchronized (locked)
                {
                    Integer i = (Integer) locked.get(key);
                    if (i != null)
                    {
                        int count = i.intValue() - 1;
                        if (count <= 0)
                            locked.remove(key);
                        else
                            locked.put(key, new Integer(count));
                    }
                }
            }

            public int getStoragePriority()
            {
                // TODO Auto-generated method stub
                return 0;
            }

            public AuthContext getAuthContext()
            {
                // TODO Auto-generated method stub
                return null;
            }
        }

        private class ApiStorageImpl extends AppStorageImpl implements ApiStorage
        {
            public ApiStorageImpl(Api key, File baseDir)
            {
                super(key, baseDir);
            }

            public String getName()
            {
                return ((Api) key).name;
            }

            public String getVersion()
            {
                return ((Api) key).version;
            }
        }

        public void deleteApp(AppID id, long version)
        {
            stored.remove(new App(id, version));
        }

        public void deleteApi(String name, String version)
        {
            storedApi.remove(new Api(name, version));
        }

        public String readAppBaseDir(AppID id, long version)
        {
            return ((AppStorage) stored.get(new App(id, version))).getBaseDirectory().toString();
        }

        public AppStorage retrieveApp(AppID id, long version, String className)
        {
            return (AppStorage) stored.get(new App(id, version));
        }

        public ApiStorage retrieveApi(String name, String version)
        {
            return (ApiStorage) storedApi.get(new Api(name, version));
        }

        public ApiStorage[] retrieveApis()
        {
            Vector v = new Vector();
            for (Enumeration e = storedApi.elements(); e.hasMoreElements();)
            {
                v.addElement(e.nextElement());
            }
            ApiStorage[] a = new ApiStorage[v.size()];
            v.copyInto(a);

            return a;
        }

        public boolean storeApp(AppID id, long version, int priority, AppDescriptionInfo info, TransportProtocol[] tp,
                String baseDir, boolean now)
        {
            App key = new App(id, version);
            // Simply pretend like we stored it in same place it came from for
            // now
            AppStorage storage = new AppStorageImpl(key, new File(baseDir));
            stored.put(key, storage);
            return true;
        }

        public boolean storeApi(String id, String version, int priority, AppDescriptionInfo info, File baseDir)
        {
            Api key = new Api(id, version);
            // Simply pretend like we stored it in same place it came from for
            // now
            AppStorage storage = new ApiStorageImpl(key, baseDir);
            storedApi.put(key, storage);
            return true;
        }

        public void updatePrivilegedCertificates(byte[] privCertDescriptor)
        {
            // empty
        }

        public void destroy()
        { /* empty */
        }

        public boolean isPartiallyStored(AppID id, long version)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean backgroundStoreApp(XAppEntry entry, TransportProtocol tp)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean storeApp(XAppEntry entry, String[] fsMounts, boolean adfFromHashfiles)
            throws FileSysCommunicationException
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean updateStoragePriority(AppStorage app, int priority)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public String getAppStorageDirectory()
        {
            // TODO Auto-generated method stub
            return null;
        }
    }

    /* ==================== Boilerplate =============== */

    public void setUp() throws Exception
    {
        super.setUp();
        replaceAppStorageMgr();
    }

    public void tearDown() throws Exception
    {
        restoreAppStorageMgr();
        super.tearDown();
    }

    public ApiRegistrarTest(String test)
    {
        super(test);
    }

    public static Test suite()
    {
        return new TestSuite(ApiRegistrarTest.class);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(ApiRegistrarTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new ApiRegistrarTest(tests[i]));
            return suite;
        }
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
