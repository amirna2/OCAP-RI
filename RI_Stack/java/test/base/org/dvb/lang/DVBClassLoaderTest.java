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

package org.dvb.lang;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.application.AppClassLoaderTest.DummyAuthCtx;
import org.cablelabs.impl.security.SecurityManagerImplTest.Loader;
import org.cablelabs.test.NanoHTTPD;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.InvalidFormatException;
import org.dvb.dsmcc.InvalidPathNameException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotEntitledException;
import org.dvb.dsmcc.ServerDeliveryException;
import org.dvb.dsmcc.ServiceXFRException;
import org.dvb.io.persistent.FileAccessPermissions;
import org.ocap.application.SecurityPolicyHandler;

/**
 * Tests the DVBClassLoader class.
 */
public class DVBClassLoaderTest extends TestCase
{
    static String baseDirectory;
    static
    {
        //
        // if the snfs directory exists, assume that
        // we should use that for the base directory to
        // load classes from
        //
        if ((new File("/snfs")).exists())
        {
            baseDirectory = "snfs";
        }
        else
        {
            baseDirectory = "syscwd";
        }
    }

    /**
     * Tests constructors.
     */
    public void testConstructors()
    {
        DVBClassLoader dvbcl;

        try
        {
            // Should this fail? Doesn't make sense, so yeah.
            dvbcl = new DummyDVBClassLoader(null);
            fail("Should not be able to create DVBClassLoader with null path");
        }
        catch (Exception e)
        { /* expected */
        }

        // While it doesn't make sense, I see no reason why it should fail...
        dvbcl = new DummyDVBClassLoader(new URL[0]);

        // Should succeed
        dvbcl = new DummyDVBClassLoader(urls);

        // Should succeed
        dvbcl = new DummyDVBClassLoader(urls, new DummyClassLoader());
    }

    /**
     * Test constructors check with SecurityManager.
     */
    public void testConstructors_security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);

        try
        {
            sm.clear();

            // Create a classloader
            new DummyDVBClassLoader(urls);
            assertTrue("SecurityManager.checkCreateClassLoader should be called", sm.createClassLoader);

            sm.clear();

            // Create a classloader
            new DummyDVBClassLoader(urls, new DummyClassLoader());
            assertTrue("SecurityManager.checkCreateClassLoader should be called", sm.createClassLoader);
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests the expected ancestry of DVBClassLoader.
     */
    public void testAncestry()
    {
        TestUtils.testExtends(DVBClassLoader.class, java.security.SecureClassLoader.class);
    }

    /**
     * Tests newInstance(URL[]).
     */
    public void testNewInstance() throws Exception
    {
        DVBClassLoader dvbcl;

        try
        {
            // Should this fail? Doesn't make sense, so yeah.
            dvbcl = DVBClassLoader.newInstance(null);
            fail("Should not be able to create DVBClassLoader with null path");
        }
        catch (Exception e)
        { /* expected */
        }
        // While it doesn't make sense, I see no reason why it should fail...
        dvbcl = DVBClassLoader.newInstance(new URL[0]);
        assertNotNull("Shouldn't have a problem creating a DVBClassLoader with an empty path", dvbcl);

        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            // Should succeed
            dvbcl = DVBClassLoader.newInstance(urls);

            sm.clear();

            // Calling loadClass on this should result in a call to
            // SecurityManager
            dvbcl.loadClass("org.cablelabs.xlet.stupid.StupidXlet");
            assertTrue("Calling loadClass should result in call to security manager", sm.packageAccess.size() > 0);
            assertNotNull("Should call securityManager with the given package",
                    sm.packageAccess.get("org.cablelabs.xlet.stupid"));
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests newInstance(URL[], ClassLoader).
     */
    public void testNewInstance_parent() throws Exception
    {
        DummyClassLoader parent = new DummyClassLoader();
        DVBClassLoader dvbcl;

        try
        {
            // Should this fail? Doesn't make sense, so yeah.
            dvbcl = DVBClassLoader.newInstance(null, parent);
            fail("Should not be able to create DVBClassLoader with null path");
        }
        catch (Exception e)
        { /* expected */
        }
        // While it doesn't make sense, I see no reason why it should fail...
        dvbcl = DVBClassLoader.newInstance(new URL[0], parent);
        assertNotNull("Shouldn't have a problem creating a DVBClassLoader with an empty path", dvbcl);
        try
        {
            dvbcl = DVBClassLoader.newInstance(urls, null);
        }
        catch (Exception e)
        {
            fail("A null parent should be allowed");
        }

        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            // Should succeed
            dvbcl = DVBClassLoader.newInstance(urls, getClass().getClassLoader());

            sm.clear();

            // Calling loadClass on this should result in a call to
            // SecurityManager
            try
            {
                dvbcl.loadClass("org.cablelabs.xlet.stupid.StupidXlet");
            }
            catch (Exception e)
            { /* expected */
            }
            assertTrue("Calling loadClass should result in call to security manager", sm.packageAccess.size() > 0);
            assertNotNull("Should call securityManager with the given package",
                    sm.packageAccess.get("org.cablelabs.xlet.stupid"));
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests that lack of RuntimePermission("createClassLoader") doesn't prevent
     * an app from creating a DVBClassLoader via newInstance().
     * 
     * @throws Exception
     */
    public void testNewInstance_security() throws Exception
    {
        Loader loader = new Loader();
        PermissionCollection perms = new Permissions();
        loader.pd = new ProtectionDomain(new CodeSource(null, null), perms);
        loader.addClass(NewInstanceTester.class.getName());

        Class testClass = loader.loadClass(NewInstanceTester.class.getName());

        PermissionTester test = (PermissionTester) testClass.newInstance();

        DummySecurityManager sm = new DummySecurityManager()
        {
            public void checkCreateClassLoader()
            {
                super.checkCreateClassLoader();
                AccessController.checkPermission(new RuntimePermission("createClassLoader"));
            }
        };
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            test.run();
            assertTrue("SecurityManager.checkCreateClassLoader should be called", sm.createClassLoader);
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Test that caller's permissions context is used instead of the stack's
     * when loading classes or looking up resources.
     * 
     * @throws Exception
     */
    public void testLoadClass_security() throws Exception
    {
        Loader loader = new Loader();
        PermissionCollection perms = new Permissions();
        perms.add(new FilePermission("/syscwd/apps/launcher/org/cablelabs/xlet/launcher/AppLauncher$Xlet.class", "read"));
        loader.pd = new ProtectionDomain(new CodeSource(null, null), perms);
        loader.addClass(LoadClassTester.class.getName());

        Class testClass = loader.loadClass(LoadClassTester.class.getName());
        Constructor cons = testClass.getConstructor(new Class[] { String.class, String.class, boolean.class });
        PermissionTester testFail = (PermissionTester) cons.newInstance(new Object[] { "file:/syscwd/apps/launcher",
                "org.cablelabs.xlet.launcher.AppLauncher", Boolean.TRUE });
        PermissionTester testSuccess = (PermissionTester) cons.newInstance(new Object[] { "file:/syscwd/apps/launcher",
                "org.cablelabs.xlet.launcher.AppLauncher$Xlet", Boolean.FALSE });

        final boolean[] read = { false };
        DummySecurityManager sm = new DummySecurityManager()
        {
            public void checkRead(String file)
            {
                super.checkRead(file);
                read[0] = true;
                AccessController.checkPermission(new FilePermission(file, "read"));
            }

            public void checkRead(String file, Object context)
            {
                super.checkRead(file, context);
                read[0] = true;
                ((AccessControlContext) context).checkPermission(new FilePermission(file, "read"));
            }
        };

        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            read[0] = false;
            testSuccess.run();
            assertTrue("SecurityManager.checkRead should be called", read[0]);
            // LoadClassTester tests everything else...

            read[0] = false;
            testFail.run();
            assertTrue("SecurityManager.checkRead should be called", read[0]);
            // LoadClassTester tests everything else...

        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    public void testFindClass() throws Exception
    {
        OcapSecurityManager osm = new DummyOcapSecurity();
        OcapSecurityManager save = installOcapSecurity(osm);
        try
        {
            DVBClassLoader dvbcl;
            DummySecurityManager sm = new DummySecurityManager();
            ProxySecurityManager.install();
            ProxySecurityManager.push(sm);
            try
            {
                // Should succeed
                dvbcl = DVBClassLoader.newInstance(urls);

                sm.clear();

                Class cls = dvbcl.findClass("org.cablelabs.xlet.stupid.StupidXlet");
                assertNotNull("findClass returned null", cls);
                assertEquals("findClass returned wrong class", cls.getName(), "org.cablelabs.xlet.stupid.StupidXlet");
                assertSame("The foundClass should have our classLoader", dvbcl, cls.getClassLoader());

                ProtectionDomain domain = cls.getProtectionDomain();
                assertEquals("ProtectionDomains should be equal", osm.getProtectionDomain(), domain);
            }
            finally
            {
                ProxySecurityManager.pop();
            }
        }
        finally
        {
            installOcapSecurity(save);
        }
    }

    /**
     * Tests getParent().
     */
    public void testGetParent() throws Exception
    {
        ClassLoader cl = new DummyClassLoader();
        DVBClassLoader dvbcl = DVBClassLoader.newInstance(urls, cl);

        assertSame("The parent set on construction should be returned", cl, dvbcl.getParent());

        dvbcl = DVBClassLoader.newInstance(urls, null);
        assertNull("The parent set on construction should be returned", dvbcl.getParent());

        // How do we verify that the App class loader is the parent by default?
        // Just make sure that there is one
        dvbcl = DVBClassLoader.newInstance(urls);
        assertNotNull("A default parent classloader should be returned", dvbcl.getParent());
    }

    /**
     * Base class for parameterized tests.
     */
    public abstract static class ClassPathTest extends InterfaceTestCase
    {
        public ClassPathTest(String name, ImplFactory f)
        {
            super(name, URL[].class, f);
            factory = (Factory) f;
        }

        protected URL[] getURLPath()
        {
            return (URL[]) createImplObject();
        }

        protected DVBClassLoader createClassLoader()
        {
            return DVBClassLoader.newInstance(getURLPath());
        }

        protected DVBClassLoader dvbclassloader;

        protected DummyOcapSecurity osm;

        private OcapSecurityManager savedOSM;

        private AuthManager savedAM;

        protected Factory factory;

        protected void setUp() throws Exception
        {
            // System.out.println(getName());
            super.setUp();
            factory.setUp();
            osm = new DummyOcapSecurity();
            savedOSM = installOcapSecurity(osm);
            savedAM = installAuthManager(new DummyAuthMgr());
            dvbclassloader = createClassLoader();
        }

        protected void tearDown() throws Exception
        {
            dvbclassloader = null;
            installAuthManager(savedAM);
            installOcapSecurity(savedOSM);
            factory.tearDown();
            super.tearDown();
        }

        protected static class Factory implements ImplFactory
        {
            private URL[] path;

            private String name;

            public Factory(String name, URL[] path)
            {
                this.path = path;
                this.name = name;
            }

            public Factory(URL[] path)
            {
                this(null, path);
            }

            public Object createImplObject()
            {
                return path;
            }

            public String toString()
            {
                return (name == null) ? super.toString() : name;
            }

            public void setUp() throws Exception
            {
                // Does nothing
            }

            public void tearDown() throws Exception
            {
                // Does nothing
            }
        }

        protected static class HttpFactory extends Factory
        {
            private NanoHTTPD httpd;

            private int port;

            private File baseDir;

            public HttpFactory(String name, URL url, String baseDir)
            {
                super(name, new URL[] { url });

                this.baseDir = new File(baseDir);
                this.port = url.getPort();
            }

            public void setUp() throws Exception
            {
                super.setUp();

                if (httpd == null) httpd = new NanoHTTPD(port, baseDir);
            }

            public void tearDown() throws Exception
            {
                // httpd.shutdown();

                super.tearDown();
            }
        }
    }

    /**
     * Inner subclass that houses parameterized tests that expect a "valid"
     * classpath. By "valid" it means that the searched for class or resource
     * should be found on the path.
     */
    public static class ClassPathSuccess extends ClassPathTest
    {
        public static Test suite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(ClassPathSuccess.class);
            suite.setName("ClassPathSuccess");

            try
            {
                suite.addFactory(new Factory("Success1", new URL[] { new URL("file:/" + baseDirectory + "/xlet"),
                        new URL("file:/" + baseDirectory + "/qa/xlet"), }));
                suite.addFactory(new Factory("Success2", new URL[] { new URL("file:/" + baseDirectory + "/sys"),
                        new URL("http://www.someplace-that-doesnt-exist-I-hope.com/"),
                        new URL("file:/" + baseDirectory + "/qa/xlet"), new URL("file:/" + baseDirectory + "/qa"), }));
                suite.addFactory(new Factory("Success3", new URL[] {
                        new URL("http://www.someplace-that-doesnt-exist-I-hope.com/"),
                        new URL("file:/" + baseDirectory + "/qa/xlet/com"), new URL("file:/" + baseDirectory + "/qa"),
                        new URL("file:/" + baseDirectory + "/qa/xlet"), }));
                suite.addFactory(new HttpFactory("Success4", new URL("http://127.0.0.1:8181"), "/" + baseDirectory
                        + "/qa/xlet"));
                suite.addFactory(new HttpFactory("Success5", new URL("http://127.0.0.1:8182/xlet/"), "/"
                        + baseDirectory + "/qa"));
                // // Relative URL
                suite.addFactory(new Factory("Success6", new URL[] { new URL("file:qa/xlet"),
                        new URL("file:../snfs/qa/xlet"), }));
                suite.addFactory(new Factory("Success7", new URL[] { new URL("file:sys"), new URL("file:../snfs/sys"),
                        new URL("http://www.someplace-that-doesnt-exist-I-hope.com/"), new URL("file:qa/xlet"),
                        new URL("file:qa"), new URL("file:../snfs/qa/xlet"), new URL("file:../snfs/qa"), }));
                suite.addFactory(new Factory("Success8", new URL[] {
                        new URL("http://www.someplace-that-doesnt-exist-I-hope.com/"), new URL("file:qa/xlet/com"),
                        new URL("file:qa"), new URL("file:qa/xlet"), new URL("file:../snfs/qa/xlet/com"),
                        new URL("file:../snfs/qa"), new URL("file:../snfs/qa/xlet"), }));
            }
            catch (Exception e)
            {
                fail("Problems creating URLs: " + e);
            }

            return suite;
        }

        public ClassPathSuccess(String name, ImplFactory f)
        {
            super(name, f);
        }

        /**
         * Tests findClass().
         */
        public void testFindClass() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;
            Class theClass;

            // Verify that doesn't go to parent...
            try
            {
                theClass = cl.findClass("java.io.File");
                fail("Should not find a 'system' class not located on path");
            }
            catch (ClassNotFoundException e)
            { /* expected */
            }

            // Rest is stolen from testLoadClass...
            theClass = cl.findClass("org.cablelabs.xlet.stupid.StupidXlet");
            assertNotNull("findClass returned null", theClass);
            assertEquals("findClass returned wrong class", theClass.getName(), "org.cablelabs.xlet.stupid.StupidXlet");
            assertSame("The foundClass should have our classLoader", cl, theClass.getClassLoader());

            // Make sure non-existent class is not found
            try
            {
                theClass = cl.findClass("com.vidimo.xlet.XXX");
                fail("An exception was expected!");
            }
            catch (ClassNotFoundException e)
            { /* expected */
            }
        }

        /********* Following are defined by ClassLoader **************/

        /**
         * Tests getResource().
         */
        public void testGetResource() throws Exception
        {
            // Largely copied from AppClassLoader... could we refactor?
            DVBClassLoader cl = dvbclassloader;
            java.net.URL url = cl.getResource("org/cablelabs/xlet/stupid/StupidXlet.class");
            assertNotNull("Should be able to find class as resource with factory " + factory, url);

            url = cl.getResource("org/cablelabs/ick.ick");
            assertNull("Null should be returned for nonfound resources", url);
        }

        /**
         * Tests attributes of URL returned by successful getResources().
         */
        public void testGetResource_URL() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;
            java.net.URL url = cl.getResource("/org/cablelabs/xlet/stupid/StupidXlet.class");
            assertNotNull("Should be able to find class as resource with factory " + factory, url);

            // Bug 5121: ensure that URL can be manipulated/reconstituted
            URL url2 = new URL(url.toExternalForm());
            assertEquals("Could not reconstitute URL", url.toExternalForm(), url2.toExternalForm());

            assertEquals("Unexpected URL protocol", "file", url.getProtocol());
            assertNull("Expected URL host to be null", url.getHost());

            // For right now, just make sure we can connect and query...
            // Don't worry about what these return...
            URLConnection uc = url.openConnection();
            assertNotNull("Expected openConnection to succeed", uc);
            uc.getContentLength();
            uc.getContentType();
            uc.getContent();

        }

        /**
         * Tests getResourceAsStream().
         */
        public void testGetResourceAsStream() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;
            java.io.InputStream in = cl.getResourceAsStream("org/cablelabs/xlet/stupid/StupidXlet.class");
            assertNotNull("Should be able to find class as resource with factory " + factory, in);

            in = cl.getResourceAsStream("org/cablelabs/ick.ick");
            assertNull("Null should be returned for nonfound resources", in);
        }

        /**
         * Tests getResources(). (This is Java2-specific.)
         */
        public void testGetResources() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;

            // Should consult parent/system classloader
            {
                Enumeration e = cl.getResources("java/io/File.class");
                assertNotNull("Enumeration should never be null", e);
                assertTrue("The enumeration should contain elements for " + factory, e.hasMoreElements());
                Object obj = e.nextElement();
                assertNotNull("If there are elements, they should be returned", obj);
                assertTrue("Elements should be URL", obj instanceof URL);
            }

            // Verify that a resource is found
            {
                Enumeration e = cl.getResources("org/cablelabs/xlet/stupid/StupidXlet.class");
                assertNotNull("Enumeration should never be null", e);
                assertTrue("The enumeration should contain elements with factory " + factory, e.hasMoreElements());
                Object obj = e.nextElement();
                assertNotNull("If there are elements, they should be returned", obj);
                assertTrue("Elements should be URL", obj instanceof URL);

                assertTrue("Enumeration should be empty after one element", !e.hasMoreElements());
                try
                {
                    e.nextElement();
                    fail("Should throw an exception if there are no elements");
                }
                catch (java.util.NoSuchElementException ex)
                {
                }
            }

            // TODO: Should probably test that it can return MORE than one
            // resource!
        }

        /**
         * Tests findResources(). (This is Java2-specific.)
         */
        public void testFindResources() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;

            // Should not consult parent/system classloader
            {
                Enumeration e = cl.findResources("java/io/File.class");
                assertNotNull("Should still return an enumeration even if not found", e);
                assertTrue("The enumeration should contain NO elements", !e.hasMoreElements());
                try
                {
                    e.nextElement();
                    fail("Should throw an exception if there are no elements");
                }
                catch (java.util.NoSuchElementException ex)
                {
                }
            }

            // Verify that a resource is found
            {
                Enumeration e = cl.findResources("org/cablelabs/xlet/stupid/StupidXlet.class");
                assertNotNull("Enumeration should never be null", e);
                assertTrue("The enumeration should contain elements with factory " + factory, e.hasMoreElements());
                Object obj = e.nextElement();
                assertNotNull("If there are elements, they should be returned", obj);
                assertTrue("Elements should be URL", obj instanceof URL);

                assertTrue("Enumeration should be empty after one element", !e.hasMoreElements());
                try
                {
                    e.nextElement();
                    fail("Should throw an exception if there are no elements");
                }
                catch (java.util.NoSuchElementException ex)
                {
                }
            }

            // TODO: Should probably test that it can return MORE than one
            // resource!
        }

        /**
         * Tests loadClass().
         */
        public void testLoadClass() throws Exception
        {
            // Largely copied from AppClassLoader... could we refactor?
            DVBClassLoader cl = dvbclassloader;
            Class theClass = cl.loadClass("org.cablelabs.xlet.stupid.StupidXlet");
            assertNotNull("loadClass returned null", theClass);
            assertEquals("loadClass returned wrong class", theClass.getName(), "org.cablelabs.xlet.stupid.StupidXlet");
            assertSame("The foundClass should have our classLoader", cl, theClass.getClassLoader());

            // Make sure non-existent class is not found
            try
            {
                theClass = cl.loadClass("com.vidimo.xlet.XXX");
                fail("An exception was expected!");
            }
            catch (ClassNotFoundException e)
            { /* expected */
            }

            // Make sure we load system classes via system class loader
            theClass = cl.loadClass("java.io.File");
            assertNotSame("Should use parent/system class loader for other classes", theClass.getClassLoader(), cl);
        }

        /**
         * Tests the ProtectionDomain of loaded classes.
         */
        public void testProtectionDomain() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;
            try
            {
                Class theClass = cl.loadClass("org.cablelabs.xlet.stupid.StupidXlet");
                ProtectionDomain pd = theClass.getProtectionDomain();
                assertNotNull("The ProtectionDomain should not be null", pd);
                CodeSource cs = pd.getCodeSource();
                assertNotNull("The CodeSource should not be null", pd);
                URL url = cs.getLocation();
                assertNotNull("The CodeSource should have a non-null URL", url);
                // TODO: Should verify the codeSource URL
                // TODO: How can we verify Certificates?? At least verify that
                // the expected ones were loaded?
                Certificate[] certs = cs.getCertificates();
            }
            catch (Exception exc)
            {
                fail("Factory " + factory + " caught exception " + exc);
            }
        }
    }

    /**
     * Inner subclass that houses parameterized tests that expect an "invalid"
     * classpath. By "invalid" it means that the searched for class or resource
     * should NOT be found on the path.
     */
    public static class ClassPathFail extends ClassPathTest
    {
        public static Test suite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(ClassPathFail.class);
            suite.setName("ClassPathFail");
            try
            {

                suite.addFactory(new Factory("Fail1", new URL[] { new URL("file:/syscwd/sys"), }));
                suite.addFactory(new Factory("Fail2", new URL[] { new URL("file:/syscwd/qa/xlet/com"),
                        new URL("http://www.someplace-that-doesnt-exist-I-hope.com/"), new URL("file:/syscwd/qa"), }));
                suite.addFactory(new HttpFactory("Fail4", new URL("http://127.0.0.1:8183"), "/syscwd/qa/xlet/com"));
                suite.addFactory(new HttpFactory("Fail5", new URL("http://127.0.0.1:8184/qa/"), "/syscwd"));
                // Relative paths
                suite.addFactory(new Factory("Fail6", new URL[] { new URL("file:sys"), }));
                suite.addFactory(new Factory("Fail7", new URL[] { new URL("file:qa/xlet/com"),
                        new URL("http://www.someplace-that-doesnt-exist-I-hope.com/"), new URL("file:qa"), }));
            }
            catch (Exception e)
            {
                fail("Problems creating URLs: " + e);
            }

            return suite;
        }

        public ClassPathFail(String name, ImplFactory f)
        {
            super(name, f);
        }

        /**
         * Tests loadClass() expected to fail.
         */
        public void testLoadClass() throws Exception
        {
            // Largely copied from AppClassLoader... could we refactor?
            DVBClassLoader cl = dvbclassloader;
            try
            {
                Class theClass = cl.loadClass("org.cablelabs.xlet.stupid.StupidXlet");
                fail("An exception was expected!");
            }
            catch (ClassNotFoundException e)
            { /* expected */
            }
        }

        /**
         * Tests getResource() expected to fail.
         */
        public void testGetResource() throws Exception
        {
            // Largely copied from AppClassLoader... could we refactor?
            DVBClassLoader cl = dvbclassloader;
            java.net.URL url = cl.getResource("org.cablelabs.xlet.stupid.StupidXlet.class");
            assertNull("Should not find class as resource", url);
        }

        private void checkResources(Enumeration e)
        {
            assertNotNull("Should still return an enumeration even if not found", e);
            assertTrue("The enumeration should contain NO elements", !e.hasMoreElements());
            try
            {
                e.nextElement();
                fail("Should throw an exception if there are no elements");
            }
            catch (java.util.NoSuchElementException ex)
            {
            }
        }

        /**
         * Tests getResources(). (This is Java2-specific. )
         */
        public void testGetResources() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;
            Enumeration e = cl.getResources("org.cablelabs.apps.stupid.StupidXlet.class");
            checkResources(e);
        }

        /**
         * Tests findResources(). (This is Java2-specific. )
         */
        public void testFindResources() throws Exception
        {
            DVBClassLoader cl = dvbclassloader;
            Enumeration e = cl.findResources("org.cablelabs.apps.stupid.StupidXlet.class");
            checkResources(e);
        }
    }

    // For constructors... for JDK118/PJava12 where loadClass is abstract
    private static class DummyDVBClassLoader extends DVBClassLoader
    {
        public DummyDVBClassLoader(URL[] urls)
        {
            super(urls);
        }

        public DummyDVBClassLoader(URL[] urls, ClassLoader parent)
        {
            super(urls, parent);
        }

        protected Class loadClass(String name, boolean resolve)
        {
            throw new RuntimeException("Unimplemented");
        }
    }

    private static class DummySecurityManager extends NullSecurityManager
    {
        public boolean createClassLoader;

        public Hashtable packageAccess = new Hashtable();

        public DummySecurityManager()
        {
            PermissionCollection collection = new Permissions();
            collection.add(new AllPermission());
            try
            {
                domain = new ProtectionDomain(new java.security.CodeSource(new URL("file:/syscwd/qa/xlet/"), null),
                        collection);
            }
            catch (Exception e)
            { /* expected */
            }
        }

        public void checkCreateClassLoader()
        {
            createClassLoader = true;
        }

        public synchronized void checkPackageAccess(String pkg)
        {
            packageAccess.put(pkg, pkg);
        }

        synchronized void clear()
        {
            createClassLoader = false;
            packageAccess = new Hashtable();
        }

        public ProtectionDomain getProtectionDomain()
        {
            return domain;
        }

        ProtectionDomain domain = null;
    }

    static class DummyClassLoader extends ClassLoader
    {
        public boolean parentCalled;

        public DummyClassLoader()
        {
            super();
        }

        public Class loadClass(String name) throws ClassNotFoundException
        {
            parentCalled = true;
            throw new ClassNotFoundException(name);
        }

        protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            parentCalled = true;
            throw new ClassNotFoundException(name);
        }
    }

    static class DummyOcapSecurity implements OcapSecurityManager
    {
        public PermissionCollection pc;

        public ProtectionDomain pd;

        public ProtectionDomain getProtectionDomain()
        {
            if (pd == null)
            {
                PermissionCollection collection = new Permissions();
                collection.add(new AllPermission());
                try
                {
                    pd = new ProtectionDomain(new java.security.CodeSource(new URL("file:/oc/9999/"), null), collection);
                }
                catch (Exception e)
                {
                    fail("Could not create ProtectionDomain");
                }
            }
            return pd;
        }

        public PermissionCollection getUnsignedPermissions()
        {
            if (pc == null)
            {
                pc = new Permissions();
                pc.add(new FilePermission("/-", "read"));
            }
            return pc;
        }

        public boolean hasReadAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category)
        {
            return false;
        }

        public boolean hasWriteAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category)
        {
            return false;
        }

        public void securitySetup()
        {
            // Does nothing
        }

        public void setSecurityPolicyHandler(SecurityPolicyHandler handler)
        {
            // Does nothing
        }

        public void destroy()
        {
            // Does nothing
        }
    }

    /**
     * An instance of this interface will be loaded by a specialized ClassLoader
     * (an instance of Loader).
     * 
     * @author Aaron Kamienski
     */
    public static interface PermissionTester
    {
        public void run() throws Exception;
    }

    /**
     * This class will be loaded by a specialized ClassLoader (an instance of
     * Loader). This will be used so that we can give the class specific
     * permissions so that we can test newInstance().
     * 
     * @author Aaron Kamienski
     */
    public static class NewInstanceTester implements PermissionTester
    {
        public void run() throws Exception
        {
            DVBClassLoader cl = DVBClassLoader.newInstance(new URL[] { new URL("file:/syscwd/apps/launcher") });
            assertNotNull("Expected CL to be created", cl);
        }
    }

    public static class LoadClassTester implements PermissionTester
    {
        private String url;

        private String className;

        private boolean expectFail;

        public LoadClassTester()
        {
            this("file:/syscwd/apps/launcher", "org.cablelabs.xlet.launcher.AppLauncher$Xlet", true);
        }

        public LoadClassTester(String url, String className, boolean expectFail)
        {
            this.url = url;
            this.className = className;
            this.expectFail = expectFail;
        }

        public void run() throws Exception
        {
            DVBClassLoader cl = DVBClassLoader.newInstance(new URL[] { new URL(url) });
            assertNotNull("Expected CL to be created", cl);

            // Test loadClass...
            try
            {
                Class clazz = cl.loadClass(className);
                if (expectFail)
                    fail("Expected a SecurityException/ClassNotFoundException");
                else
                    assertNotNull("Expected Class to be returned", clazz);
            }
            catch (SecurityException e)
            {
                if (!expectFail) throw e;
                /* else */
                /* expected */
            }
            catch (ClassNotFoundException e)
            {
                if (!expectFail) throw e;
                /* else */
                /* expected */
            }

            // Try getResource as well...
            URL rez = cl.getResource(className.replace('.', '/') + ".class");
            if (expectFail)
                assertNull("Expected null to be returned", rez);
            else
                assertNotNull("Expected URL to be returned", rez);
        }
    }

    static class DummyAuthMgr implements AuthManager
    {
        private AuthContext ac = new DummyAuthCtx();

        public AuthContext createAuthCtx(String initialFile, int signers, int orgId)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public AuthContext getAuthCtx(CallerContext cc)
        {
            return ac; // basically the only reason that this is here...
        }

        public AuthInfo getClassAuthInfo(String targName, FileSys fs)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files) throws IOException
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file) throws IOException
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws IOException
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public String[] getHashfileNames(String dir, FileSys fs) throws IOException
        {
            return null; // will make http always fail...
        }

        public X509Certificate[][] getSigners(String targName, boolean knownRoot, FileSys fs, byte[] file)
                throws InvalidFormatException, InterruptedIOException, MPEGDeliveryException, ServerDeliveryException,
                InvalidPathNameException, NotEntitledException, ServiceXFRException, InsufficientResourcesException
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void invalidate(String targName)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void registerCRLMount(String path)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void setAuthCtx(CallerContext cc, AuthContext authCtx)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void setPrivilegedCerts(byte[] codes)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void unregisterCRLMount(String path)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void destroy()
        {
            throw new UnsupportedOperationException("unimplemented");
        }
    }

    public DVBClassLoaderTest(String name)
    {
        super(name);
    }

    protected AuthManager savedAM;

    protected URL[] urls;

    static final String[] urlStrings = { "file:/syscwd/qa/xlet/", "file:/snfs/qa/xlet/", "file:qa/xlet/",
            "file:../snfs/qa/xlet", "file:/", "file:", "http://www.cablelabs.org", };

    protected void setUp() throws Exception
    {
        // System.out.println(getName());
        super.setUp();
        urls = new URL[urlStrings.length];
        for (int i = 0; i < urlStrings.length; ++i)
            urls[i] = new URL(urlStrings[i]);

        savedAM = installAuthManager(new DummyAuthMgr());

        // Make sure that the FileSysMgr is installed
        ManagerManager.getInstance(FileManager.class);
    }

    protected void tearDown() throws Exception
    {
        urls = null;
        installAuthManager(savedAM);
        super.tearDown();
    }

    static AuthManager installAuthManager(AuthManager am)
    {
        // This is a workaround for bug 5072.
        // Ensure that the FM is up first (else we may end up being stuck with
        // our AuthManager forever)
        ManagerManager.getInstance(FileManager.class);

        AuthManager save = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        ManagerManagerTest.updateManager(AuthManager.class, am != null ? am.getClass() : null, false, am);
        return save;
    }

    static OcapSecurityManager installOcapSecurity(OcapSecurityManager osm)
    {
        OcapSecurityManager save = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
        ManagerManagerTest.updateManager(OcapSecurityManager.class, osm != null ? osm.getClass() : null, false, osm);
        return save;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(DVBClassLoaderTest.class);
        suite.addTest(ClassPathSuccess.suite());
        suite.addTest(ClassPathFail.suite());

        return suite;
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
}
