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

package org.cablelabs.impl.ocap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Permission;
import java.util.Calendar;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.cablelabs.impl.debug.Debug;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.net.URLSupport;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

import org.cablelabs.impl.ocap.TimeZoneInitializer;

/**
 * The OcapMain class is the entry point to the CableLabs OCAP Java environment.
 * This provides for step 3, "Execution Engine Initialization", outlined in OCAP
 * 20.2.2. "Boot Process". It launches the Java Manager framework and initiates
 * the boot process.
 */
public class OcapMain
{
    // Right now, we use this lock to keep the main thread from returning.
    // When we implement a more proper shutdown, we can notify() this object
    // to let the main thread return and properly shutdown
    private static Object terminationLock = new Object();

    private static TimeZoneInitializer m_timeZoneIniter;

    static
    {
        // Load the Native side of the OCAP stack
        System.loadLibrary("mpe");

        // Initialize Logging subsystem
        initLogging();

        m_timeZoneIniter = new TimeZoneInitializer();
    }

    /**
     * Entry point to the OCAP Java application environment. Initializes the
     * Java environment using the following steps:
     * <ul>
     * <li>Verifies linkage with MPE native code.
     * <li>Starts the Java Manager framework.
     * <li>Starts any stack extensions.
     * <li>Starts resident applications.
     * </ul>
     * 
     * @param args
     *            currently unused
     */
    public static void main(String args[])
    {
        // Confirm linkage with MPE by calling native method
        LinkageConfirm();

        // Override various Java defaults (including properties and URL support)
        initJava();

        // Start up the Java Managers Framework
        startManagers();

        // Start up extensions
        startExtensions();

        // Start up apps
        startApps();

        // Don't let our main thread exit
        synchronized (terminationLock)
        {
            try
            {
                terminationLock.wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Ensures that OCAP native libraries are loaded. This should be called from
     * the <code>static</i> initializer
     * block of any OCAP (public API or private implementation) class
     * that provides <code>native</code> methods.
     * <p>
     * The implementation of this method ensures that the native libraries are
     * not loaded more than necessary.
     * <p>
     * This places ultimate responsibility for the loading of native libraries
     * on the classes that have native libraries. <i>This is most important for
     * testing purposes where <code>OcapMain</code> is not otherwise invoked
     * directly.</i>
     */
    public static void loadLibrary()
    {
        // Does nothing (directly).
        // The static initializer for this class performs the loading.
        // This means that System.loadLibrary will only ever be called once.
    }

    /**
     * Initializes the logging subsystem.
     */
    private static void initLogging()
    {
        InputStream moduleIS = null;
        try
        {
            Class self = OcapMain.class;

            //check final.properties for the log4j configuration file name and path
            String fileName = "/final.properties";
            moduleIS = self.getResourceAsStream(fileName);
            Properties props = new Properties();
            URL url = null;
            String configProp;
            if (moduleIS != null)
            {
                props.load(moduleIS);
                // Look for override properties (or standard at root, if no
                // override)
                configProp = props.getProperty("OCAP.log4j.properties", null);
                if (configProp != null)
                {
                    url = self.getResource(configProp);
                }
            }

            //check base.properties in case the log4j configuration file name and path is defined there
            if (url == null)
            {
                fileName = "/base.properties";
                moduleIS = self.getResourceAsStream(fileName);
                props = new Properties();
                props.load(moduleIS);
                configProp = props.getProperty("OCAP.log4j.properties", null);
                if (configProp != null)
                {
                    url = self.getResource(configProp);
                }
            }

            //if log4j configuration file name and path is not defined in either final.properties or base.properties, use a default
            if (url == null) url = self.getResource("/log4j.properties");
            // Configure if configuration file located
            if (url != null)
            {
                if (url.getFile().endsWith(".xml"))
                {
                    DOMConfigurator.configureAndWatch(url.getFile());
                }
                else
                {
                    PropertyConfigurator.configureAndWatch(url.getFile());
                }
            }
            else
            {
                System.out.println("Unable to find log4j configuration file");
            }
        }
        catch (IOException e)
        {
            // not much we can do here but print it
            e.printStackTrace();
        }
        finally
        {
            if (moduleIS != null)
            {
                try
                {
                    moduleIS.close();
                }
                catch (IOException ioe)
                {
                    // no-op
                }
            }
        }
    }

    /**
     * Initializes and/or overrides various Java stack defaults. This includes
     * overriding or setting of OCAP-specific system properties and
     * OCAP-specific URL support.
     * 
     * @see MPEEnv
     * @see URLSupport
     */
    private static void initJava()
    {
        /* Set the default Locale from our config file */
        String country = MPEEnv.getEnv("JVM.locale.country", "US");
        String language = MPEEnv.getEnv("JVM.locale.language", "en");
        Locale.setDefault(new Locale(language, country));

        /*
         * This will ensure that the installed locale list is properly
         * initialized
         */
        Calendar.getAvailableLocales();

        /*
         * get time zone and dst info from POD -- this will block until POD is
         * ready
         */
        m_timeZoneIniter.initTimeZone();

        // Override URL content type support
        URLSupport.setup();

        // Set a "no-restriction" security manager here so that any
        // access-checked
        // operations performed during stack initialization will succeed. Before
        // we start the applications, we will install the real OCAP security
        // manager
        System.setSecurityManager(new SecurityManager()
        {
            public void checkPermission(Permission p)
            {
            }

            public void checkPermission(Permission p, Object o)
            {
            }
        });
    }

    /**
     * Invokes the {@link ManagerManager#startAll} method to startup the Java
     * Managers Framework. This will start all <i>auto-run</i> managers, with
     * other managers being left to startup on-demand.
     */
    private static void startManagers()
    {
        try
        {
            ManagerManager.startAll();
        }
        catch (Throwable e)
        {
            if (e instanceof java.lang.reflect.InvocationTargetException)
            {
                e = ((java.lang.reflect.InvocationTargetException) e).getTargetException();
            }
            e.printStackTrace(Debug.out);
        }
    }

    /**
     * Causes all referenced stack extensions to be loaded. Extensions may be
     * specified via the <code>"OCAP.extensions.startup"</code> {@link MPEEnv
     * environment variable}. The variable should list the set of extension
     * classes that should be loaded at startup time. All extension
     * initialization should occur as part of static class initialization.
     */
    private static void startExtensions()
    {
        String var = MPEEnv.getEnv("OCAP.extensions.startup");
        if (var == null)
        {
            return;
        }

        for (StringTokenizer tok = new StringTokenizer(var, " \t,:;"); tok.hasMoreTokens();)
        {
            String name = tok.nextToken();
            try
            {
                // Ensure that the class is loaded
                Class.forName(name);
            }
            catch (Throwable e)
            {
                SystemEventUtil.logRecoverableError("Could not initialize extension: " + name, e);
            }
        }

    }

    /**
     * Follows the boot process outlined in OCAP 20.2.2 as it relates the
     * selecting of autoselect abstract services and launching of autostart
     * applications. This includes the launching of the initial monitor
     * application and waiting for it to signal it's readiness via
     * {@link org.ocap.OcapSystem#monitorConfiguredSignal}.
     */
    private static void startApps()
    {
        // First setup application-level security
        OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
        if (osm != null)
        {
            osm.securitySetup();
        }

        // Acquire ServicesDatabase...
        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        if (sm == null)
        {
            SystemEventUtil.logEvent("Unable to retrieve service manager - not initiating boot process");
            return;
        }
        ServicesDatabase svcDb = sm.getServicesDatabase();
        if (svcDb == null)
        {
            SystemEventUtil.logEvent("Unable to retrieve services database - not initiating boot process");
            return;
        }

        // Initiate the boot process
        svcDb.bootProcess();
    }

    /**
     * Simple native method used to confirm loading of native portion of OCAP
     * stack.
     */
    private static native void LinkageConfirm();
}
