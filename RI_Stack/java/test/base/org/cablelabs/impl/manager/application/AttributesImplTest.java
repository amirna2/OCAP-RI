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

/*
 * Created on Feb 28, 2007
 */
package org.cablelabs.impl.manager.application;

import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.appstorage.AppDescriptionInfo;
import org.cablelabs.impl.manager.application.AppDomainImplTest.TestAbstractService;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.test.iftc.InterfaceTestSuite;

import java.io.File;
import java.util.Hashtable;

import javax.tv.service.Service;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;

import org.davic.net.Locator;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.ocap.net.OcapLocator;
import org.ocap.application.OcapAppAttributes;
import org.ocap.application.OcapAppAttributesTest;
import org.ocap.application.OcapAppAttributesTest.OcapAttrFactory;

public class AttributesImplTest extends TestCase
{
    public void testNothing()
    {
        // Place holder to allow instantiation
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public AttributesImplTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    static class Factory implements OcapAttrFactory
    {
        private AppEntry entry;

        private AppEntry newEntry;

        private Service service;

        private boolean appStored;

        private boolean newAppStored;

        /*
         * Table of valid states: | entry | appStored | newEntry | newAppStored
         * |
         * --------------------------------------------------------------------
         * ------------------- | !null | false | null | false | | !null | true |
         * null | false | | !null | true | !null | false | | !null | true |
         * !null | true |
         */

        Factory(AppEntry entry, boolean appStored, AppEntry newEntry, boolean newAppStored, Service service)
        {
            if (newEntry == null) newAppStored = false; // override a wrong
                                                        // value by caller

            this.entry = entry;
            this.appStored = appStored; // is the app represented by entry
                                        // stored?
            this.newAppStored = newAppStored; // is the app represented by
                                              // newEntry stored?

            this.service = service;
        }

        public Object createImplObject()
        {
            // TODO(schoonma): Is it sufficient to pass null for first
            // parameter?
            // This was refactored for bug 5517, but not sure if this change is
            // correct
            // for this test.
            return new AttributesImpl(entry, (OcapLocator)service.getLocator());
        }

        public int getStoragePriority()
        {
            if (entry instanceof XAppEntry)
            {
                return ((XAppEntry)entry).storagePriority;
            }
            return 0;
        }

        public int getControlCode()
        {
            return entry.controlCode;
        }

        public Hashtable getNames()
        {
            return entry.names;
        }

        public int getPriority()
        {
            return entry.priority;
        }

        public boolean getServiceBound()
        {
            return entry.serviceBound;
        }

        public int getVisibility()
        {
            return entry.visibility;
        }

        public AppID getAppID()
        {
            return entry.id;
        }

        public Locator getService()
        {
            return (Locator) service.getLocator();
        }

        public String getLocationBase()
        {
            return entry.baseDirectory;
        }

        public String[] getClassPath()
        {
            return entry.classPathExtension;
        }

        public int getComponentTag()
        {
            if (entry.transportProtocols != null)
            {
                for (int i = 0; i < entry.transportProtocols.length; ++i)
                {
                    if (entry.transportProtocols[i] instanceof OcTransportProtocol)
                    {
                        // See MHP 11.7.2
                        if ((entry.controlCode == OcapAppAttributes.REMOTE) == ((OcTransportProtocol) entry.transportProtocols[i]).remoteConnection)
                            return ((OcTransportProtocol) entry.transportProtocols[i]).componentTag;
                    }
                }
            }
            return -1;
        }

        // is the old app stored?
        public boolean isStored()
        {
            return appStored;
        }

        public long getNewVersion()
        {
            return newEntry.version;
        }

        public long getVersion()
        {
            return entry.version;
        }

        public boolean isNewVersionSignaled()
        {
            return newAppStored;
        }

        public boolean hasNewVersion()
        {
            return ((newEntry == null) ? false : true);
        }

        public AppEntry getAppEntry()
        {
            return entry;
        }

        public AppEntry getNewAppEntry()
        {
            return newEntry;
        }
    }

    /**
     * Fake storage manager - used to ensure that everything is apparently
     * stored.
     * 
     * @author Aaron Kamienski
     */

    static class FakeAppStorage implements AppStorageManager
    {
        Factory currentFactory;

        private FakeAppStorage()
        {

        }

        public FakeAppStorage(Factory factory)
        {
            currentFactory = factory;
        }

        public void deleteApi(String name, String version)
        {
            // empty
        }

        public void deleteApp(AppID id, long version)
        {
            // empty
        }

        public ApiStorage retrieveApi(String name, String version)
        {
            return null;
        }

        public ApiStorage[] retrieveApis()
        {
            return null;
        }

        public AppStorage retrieveApp(AppID id, long version, String className)
        {

            boolean returnAppStorage = false;

            if (currentFactory != null && (id.equals(currentFactory.getAppID())))
            {
                if (currentFactory.getNewAppEntry() != null && version == currentFactory.getNewAppEntry().version
                        && currentFactory.newAppStored)
                {
                    returnAppStorage = true;
                }
                if (currentFactory.getAppEntry() != null && version == currentFactory.getAppEntry().version
                        && currentFactory.appStored)
                {
                    returnAppStorage = true;
                }
            }
            if (returnAppStorage)
            {
                return new AppStorage()
                {
                    public boolean lock()
                    {
                        return true;
                    }

                    public void unlock()
                    { /* empty */
                    }

                    public File getBaseDirectory()
                    {
                        return new File("/stored/app/");
                    }

                    public boolean isPreAuthenticated()
                    {
                        return false;
                    }

                    public int getStoragePriority()
                    {
                        return 0;
                    }

                    public AuthContext getAuthContext()
                    {
                        // TODO Auto-generated method stub
                        return null;
                    }
                };
            }
            else
                return null;
        }

        public String readAppBaseDir(AppID id, long version)
        {
            return null;
        }

        public boolean storeApi(String id, String version, int priority, AppDescriptionInfo info, File baseDir)
        {
            return false;
        }

        public boolean storeApp(AppID id, long version, int priority, AppDescriptionInfo info, TransportProtocol[] tp,
                String baseDir, boolean now)
        {
            return false;
        }

        public void updatePrivilegedCertificates(byte[] privCertDescriptor)
        {
            // empty
        }

        public void destroy()
        {
            // empty
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

    private static AppStorageManager oldASM;

    private static AppStorageManager asm;

    private static void replaceManagers(OcapAttrFactory factory)
    {
        oldASM = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);
        asm = new FakeAppStorage((Factory) factory);
        ManagerManagerTest.updateManager(AppStorageManager.class, asm.getClass(), true, asm);
    }

    private static void restoreManagers()
    {
        ManagerManagerTest.updateManager(AppStorageManager.class, oldASM.getClass(), true, oldASM);
        oldASM = null;
        asm = null;
    }

    /**
     * Extension of OcapAppAttributesTest, simply to add setup/teardown for
     * managers.
     * 
     * @author Aaron Kamienski
     */
    public static class DecoratedTest extends OcapAppAttributesTest
    {
        protected DecoratedTest(String name, Class testedClass, ImplFactory f)
        {
            super(name, testedClass, f);
            ocapFactory = (OcapAttrFactory) f;
            setUseClassInName(true);
        }

        public DecoratedTest(String name, ImplFactory f)
        {
            this(name, OcapAppAttributes.class, f);
        }

        public static InterfaceTestSuite isuite()
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(DecoratedTest.class);
            suite.setName(OcapAppAttributes.class.getName());
            return suite;
        }

        protected void setUp() throws Exception
        {
            replaceManagers(ocapFactory);
            super.setUp();
        }

        protected void tearDown() throws Exception
        {
            super.tearDown();
            restoreManagers();
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AttributesImplTest.class);

        // TODO: test many more variables with multiple appentry/factory
        // pairs...
        XAppEntry entry = new XAppEntry()
        {
            {
                baseDirectory = "/oc/0x4ab/";
                className = "org.cablelabs.xlet.ka.KillerApp$Xlet";
                classPathExtension = new String[0];
                controlCode = OcapAppAttributes.PRESENT;
                id = new AppID(0xbeef4dad, 0x4abc);
                names = new Hashtable();
                names.put("eng", "KillerApp");
                parameters = new String[0];
                priority = 100;
                registeredApi = new String[0];
                serviceId = 0x20001;
                storagePriority = 100;
                transportProtocols = new TransportProtocol[] { new OcTransportProtocol()
                {
                    {
                        componentTag = 10;
                        label = 1;
                        protocol = 1;
                        remoteConnection = true;
                        serviceId = 0x4a1;
                    }
                } };
                visibility = VISIBLE;
                version = 3;
                versions = new Hashtable();
                int theVersions[] = { 1, 0, 0 };
                versions.put(new Integer(0x101), theVersions); // 0x101 is for
                                                               // "ocap.profile"
                                                               // per 18.2.1.1
            }
        };
        Service service = new TestAbstractService(entry.serviceId, "MySvc", false);

        AppEntry newEntry = (AppEntry) entry.copy();
        newEntry.version = entry.version++;

        InterfaceTestSuite attrSuite = DecoratedTest.isuite();
        ImplFactory factory = null;

        factory = new Factory(entry, false, null, false, service);
        attrSuite.addFactory(factory);

        factory = new Factory(entry, true, null, false, service);
        attrSuite.addFactory(factory);

        factory = new Factory(entry, true, newEntry, false, service);
        attrSuite.addFactory(factory);

        factory = new Factory(entry, true, newEntry, true, service);
        attrSuite.addFactory(factory);

        suite.addTest(attrSuite);

        return suite;
    }
}
