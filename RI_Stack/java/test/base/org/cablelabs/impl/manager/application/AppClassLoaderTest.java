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

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.signalling.AppEntry;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;

/**
 * Tests the AppClassLoader implementation.
 * 
 * @author Aaron Kamienski
 */
public class AppClassLoaderTest extends TestCase
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
            baseDirectory = "/snfs";
        }
        else
        {
            baseDirectory = "/syscwd";
        }
    }

    /**
     * Tests constructor.
     */
    public void testCtor() throws Exception
    {
        AppEntry entry = new AppEntry();
        entry.baseDirectory = "/oc/xyz/blah/blah";
        entry.classPathExtension = new String[0];

        AuthContext ac = new DummyAuthCtx();

        new AppClassLoader(entry, new String[] {"/"}, ac, null);
        new AppClassLoader(entry, new String[] {"/"}, ac, new RegisteredApi[0], null);
    }

    /*
     * ==========================================================================
     * ===
     */

    /**
     * Base class for parameterized tests.
     */
    public abstract static class ClassPathTest extends InterfaceTestCase
    {
        public ClassPathTest(String name, ImplFactory f)
        {
            super(name, TestParam.class, f);
        }

        protected TestParam getTestParam()
        {
            return (TestParam) createImplObject();
        }

        protected AppClassLoader createClassLoader()
        {
            TestParam param = getTestParam();
            try
            {
                return new AppClassLoader(param.entry, new String[] {param.baseDir}, param.ac, param.apis, null);
            }
            catch (FileSysCommunicationException e)
            {
                return null;
            }
        }

        protected AppClassLoader appclassloader;

        protected void setUp() throws Exception
        {
            super.setUp();
            appclassloader = createClassLoader();
        }

        protected void tearDown() throws Exception
        {
            appclassloader = null;
            super.tearDown();
        }

        /**
         * A simple class used to hold test parameters and construct an
         * appclassloader from those parameters. Also implements to ImplFactory.
         */
        public static class TestParam implements ImplFactory
        {
            public AppEntry entry;

            public String baseDir;

            public RegisteredApi[] apis;

            public AuthContext ac;

            private static final String[] EMPTY = {};

            public TestParam(String baseDir, String[] extPath, AuthContext ac, RegisteredApi[] apis)
            {
                this(new AppEntry(), baseDir, ac, apis);
                entry.baseDirectory = baseDir;
                entry.classPathExtension = (extPath == null) ? EMPTY : extPath;
            }

            public TestParam(AppEntry entry, String baseDir, AuthContext ac, RegisteredApi[] apis)
            {
                this.entry = entry;
                this.baseDir = baseDir;
                this.apis = apis;
                this.ac = ac;
                if (ac == null) Thread.dumpStack();
                assertNotNull("Need a non-null AuthContext!", ac);
            }

            public Object createImplObject() throws Exception
            {
                return this;
            }

            public String getName()
            {
                return toString();
            }

            public String toString()
            {
                return "Param[" + classpath() + "]";
            }

            private String classpath()
            {
                StringBuffer sb = new StringBuffer(baseDir);
                for (int i = 0; i < entry.classPathExtension.length; ++i)
                {
                    sb.append(',').append(entry.classPathExtension[i]);
                }
                sb.append(",APIS=").append(apis);
                sb.append(",AC=").append(ac);
                return sb.toString();
            }
        }
    }

    public static class ClassPathSuccess extends ClassPathTest
    {
        public ClassPathSuccess(String name, ImplFactory f)
        {
            super(name, f);
        }

        public static Test suite()
        {

            InterfaceTestSuite suite = new InterfaceTestSuite(ClassPathSuccess.class);
            suite.setName("ClassPathSuccess");

            AuthContext auth = new DummyAuthCtx();

            RegisteredApi[] apis0 = { api(baseDirectory + "/apps/nosuchdir"), api(baseDirectory + "/apps/watchtv"),
                    api(baseDirectory + "/qa"), };

            // base
            suite.addFactory(new TestParam(baseDirectory + "/qa/xlet", null, auth, null));
            suite.addFactory(new TestParam(baseDirectory + "/qa/xlet", new String[] { "a", "b", "c" }, auth, null));
            suite.addFactory(new TestParam(baseDirectory + "/qa/xlet", new String[] { "a", "b", "c" }, auth, apis0));
            // relative
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { "xlet" }, auth, null));
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { "launcher", "xlet", "org/cablelabs" },
                    auth, null));
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { "launcher", "xlet", "org/cablelabs" },
                    auth, apis0));
            // absolute
            suite.addFactory(new TestParam("/snfs", new String[] { baseDirectory + "/qa/xlet" }, auth, null));
            suite.addFactory(new TestParam("/snfs", new String[] { baseDirectory + "/apps/config",
                    baseDirectory + "/qa/xlet", // here
                    baseDirectory + "/apps/launcher" }, auth, null));
            suite.addFactory(new TestParam("/snfs", new String[] { baseDirectory + "/qa/xlet" }, auth, apis0));
            // Mixed
            suite.addFactory(new TestParam("/snfs", new String[] { "apps/config", "unknown",
                    baseDirectory + "/qa/xlet", // here
                    "org/cablelabs" }, auth, null));
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { baseDirectory + "/apps/config/com",
                    baseDirectory + "/apps/launcher", "xlet", // here
            }, auth, null));
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { baseDirectory + "/apps/config/com",
                    baseDirectory + "/apps/launcher", "xlet", // here
            }, auth, apis0));

            // Registered Api
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { baseDirectory + "/apps/config/com",
                    baseDirectory + "/apps/launcher", }, auth, new RegisteredApi[] { api(baseDirectory + "/qa/xlet") // here
                    }));
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { baseDirectory + "/apps/config/com",
                    baseDirectory + "/apps/launcher", }, auth, new RegisteredApi[] { api("/sysced/apps/watchtv"),
                    api(baseDirectory + "/qa/xlet"), // here
                    api(baseDirectory + "/apps/config"), }));
            suite.addFactory(new TestParam(baseDirectory + "/qa", new String[] { baseDirectory + "/apps/config/com",
                    baseDirectory + "/apps/launcher", }, auth, new RegisteredApi[] {
                    api(baseDirectory + "/apps/watchtv"), api(baseDirectory + "/qa/xlet") // here
                    }));

            return suite;
        }

        /**
         * Tests successful loadClass.
         */
        public void testLoadClass() throws Exception
        {
            AppClassLoader cl = appclassloader;
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
         * Tests successful getResource().
         */
        public void testGetResource() throws Exception
        {
            AppClassLoader cl = appclassloader;
            java.net.URL url = cl.getResource("/org/cablelabs/xlet/stupid/StupidXlet.class");
            assertNotNull("Should be able to find class as resource", url);

            url = cl.getResource("org/cablelabs/ick.ick");
            assertNull("Null should be returned for nonfound resources", url);
        }

        /**
         * Tests attributes of URL returned by successful getResources().
         */
        public void testGetResource_URL() throws Exception
        {
            AppClassLoader cl = appclassloader;
            URL url = cl.getResource("/org/cablelabs/xlet/stupid/StupidXlet.class");
            assertNotNull("Should be able to find class as resource", url);

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
         * Tests successful getResourceAsStream().
         */
        public void testGetResourceAsStream() throws Exception
        {
            AppClassLoader cl = appclassloader;
            java.io.InputStream in = cl.getResourceAsStream("/org/cablelabs/xlet/stupid/StupidXlet.class");
            assertNotNull("Should be able to find class as resource", in);

            in = cl.getResourceAsStream("org/cablelabs/ick.ick");
            assertNull("Null should be returned for nonfound resources", in);
        }
    }

    /*
     * ==========================================================================
     * ===
     */

    /**
     * Inner subclass that houses parameterized tests that expect an "invalid"
     * classpath. By "invalid" it means that the searched for class or resource
     * should NOT be found on the path.
     */
    public static class ClassPathFail extends ClassPathTest
    {
        public ClassPathFail(String name, ImplFactory f)
        {
            super(name, f);
        }

        public static Test suite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(ClassPathFail.class);
            suite.setName("ClassPathFail");

            AuthContext auth = new DummyAuthCtx();

            // base
            suite.addFactory(new TestParam(baseDirectory + "/apps/launcher", null, auth, null));
            // relative
            suite.addFactory(new TestParam(baseDirectory + "/apps", new String[] { "launcher" }, auth, null));
            // absolute
            suite.addFactory(new TestParam(baseDirectory + "/apps/launcher", new String[] { baseDirectory
                    + "/apps/watchtv" }, auth, null));

            // TODO(AaronK): add versions of the above with RegisteredApis
            // included
            // TODO(AaronK): add versions that look for registered api classes

            return suite;
        }

        /**
         * Tests loadClass() expected to fail.
         */
        public void testLoadClass() throws Exception
        {
            AppClassLoader cl = appclassloader;
            try
            {
                cl.loadClass("org.cablelabs.xlet.stupid.StupidXlet");
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
            AppClassLoader cl = appclassloader;
            java.net.URL url = cl.getResource("org.cablelabs.xlet.stupid.StupidXlet.class");
            assertNull("Should not find class as resource", url);
        }
    }

    private static RegisteredApi api(final String path)
    {
        // Gah! getBaseDir is used... and it dynamically checks with
        // AppStorage...
        return new RegisteredApi(path, "1.0", null, new File("/who/cares/where"), (short) 100)
        {
            public File getApiPath()
            {
                return new File(path);
            }
        };
    }

    /*
     * ==========================================================================
     * ===
     */

    public AppClassLoaderTest(String name)
    {
        super(name);
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
        TestSuite suite = new TestSuite(AppClassLoaderTest.class);

        suite.addTest(ClassPathSuccess.suite());
        suite.addTest(ClassPathFail.suite());
        return suite;
    }

    /*
     * ==========================================================================
     * ===
     */

    /**
     * Dummy authentication context.
     * 
     * @author Aaron Kamienski
     */
    public static class DummyAuthCtx implements AuthContext
    {
        public int type = AuthInfo.AUTH_SIGNED_OCAP;

        public int getAppSignedStatus()
        {
            return type;
        }

        public AuthInfo getClassAuthInfo(String targName, FileSys fs)
        {
            return fs.exists(targName) ? (new DummyAuth(type, targName, fs)) : null;
        }
    }

    /**
     * Dummy file authentication.
     * 
     * @author Aaron Kamienski
     */
    static class DummyAuth implements AuthInfo
    {
        DummyAuth(int type, String file, FileSys fs)
        {
            this.type = type;

            try
            {
                this.bytes = fs.getFileData(file).getByteData();
            }
            catch (IOException e)
            {
                bytes = null;
            }
            catch (FileSysCommunicationException e)
            {
                bytes = null;
            }
        }

        public int getClassAuth()
        {
            return (bytes == null) ? AuthInfo.AUTH_UNKNOWN : type;
        }

        public byte[] getFile()
        {
            return bytes;
        }

        public boolean isSigned()
        {
            return bytes != null && type != AuthInfo.AUTH_FAIL && type != AuthInfo.AUTH_UNKNOWN
                    && type != AuthInfo.AUTH_UNSIGNED;
        }

        private byte[] bytes;

        private int type;
    }

    /**
     * XletApp shell class.
     */
    // TODO: this class isn't used by this test, but by others. Move it!
    static class DummyApp extends XletApp
    {
        private AppID id;

        public DummyApp()
        {
            this(new AppEntry() {{
                this.id = new AppID(TestAppIDs.APPCLASSLOADER, 1);
                this.version = 1;
            }}, new AppDomainImpl(null, null));
        }

        public DummyApp(AppEntry entry, AppDomainImpl domain)
        {
            super(entry, domain);
            this.id = entry.id;
        }
    }
}
