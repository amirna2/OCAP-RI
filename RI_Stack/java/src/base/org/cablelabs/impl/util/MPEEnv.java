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

package org.cablelabs.impl.util;

import java.io.File;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Properties;

import sun.security.action.GetIntegerAction;
import sun.security.action.GetPropertyAction;

import org.cablelabs.impl.manager.PropertiesManager;

/**
 * This class allows access to the MPE environment variable set. It also allows
 * for the user to override any MPE variables with locally stored values. Should
 * those overrides be deleted from this property set (see #removeEnvOverride()),
 * the original MPE environment values will be returned.
 * 
 * @author Greg Rutz
 * @author Alan Cohn
 */
public class MPEEnv
{

    /**
     * Get the value assigned to the environment variable <code>key</code>
     * 
     * @param key
     *            The environment variable name
     * @return The value associated with the environment variable
     */
    static public String getEnv(String key)
    {
        // Check our overrides list
        String oldValue;
        if ((oldValue = envVariables.getProperty(key)) != null) return oldValue;

        if ((oldValue = PropertiesManager.getInstance().getProperty(key, null)) != null) return oldValue;

        // Not in overrides, so check MPE
        return nGetEnv(key);
    }

    /**
     * Get the value assigned to the environment variable <code>key</code>. If
     * there is no environment variable associated with <code>key</code>, return
     * <code>defValue</code>
     * 
     * @param key
     *            The environment variable name
     * @param defValue
     *            The default value
     * @return The value associated with the environment variable
     */
    static public String getEnv(String key, String defValue)
    {
        String value = getEnv(key);
        if (value == null) return defValue;
        return value;
    }

    /**
     * Get the int value assigned to the environment variable <code>key</code>.
     * If there is no environment variable associated with <code>key</code>,
     * return <code>defValue</code>
     * 
     * @param key
     *            The environment variable name
     * @param defValue
     *            The default value
     * @return The value associated with the environment variable
     */
    static public int getEnv(String key, int defValue)
    {
        String value = getEnv(key);
        if (value == null) return defValue;
        return Integer.decode(value).intValue();
    }

    /**
     * Get the long value assigned to the environment variable <code>key</code>.
     * If there is no environment variable associated with <code>key</code>,
     * return <code>defValue</code>
     * 
     * @param key
     *            The environment variable name
     * @param defValue
     *            The default value
     * @return The value associated with the environment variable
     */
    static public long getEnv(String key, long defValue)
    {
        String value = getEnv(key);
        if (value == null) return defValue;
        return Long.decode(value).longValue();
    }

    /**
     * Set the value of the variable named <code>key</code> to the string in
     * <code>value</code>. If the variable has previously been defined, this
     * will override the previous value.
     * 
     * @param key
     *            The environment variable name
     * @param value
     *            The value to be associated with the variable name
     * @return the value that was previously assigned to <code>key</code>, else
     *         null if key was not previously defined
     */
    static public String setEnv(String key, String value)
    {
        String previousVal = envVariables.getProperty(key);
        envVariables.setProperty(key, value);

        // Return the value we are overriding, if any
        if (previousVal != null) return previousVal;

        return nGetEnv(key);
    }

    /**
     * Remove the override value for this key. If this key was previously
     * defined in an INI file, that value will become the value of this key
     * after making this call. If this key was not previously defined in an INI
     * file, the key will no longer exist after making this call.
     * 
     * @param key
     *            The environmant variable name
     */
    static public void removeEnvOverride(String key)
    {
        if (key != null) envVariables.remove(key);
    }

    /**
     * Provides a simple utility method for accessing System properties within a
     * <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @return {@link System.getProperty(String)} for the given <i>key</i>
     */
    public static String getSystemProperty(String key)
    {
        return (String) AccessController.doPrivileged(new GetPropertyAction(key));
    }

    /**
     * Provides a simple utility method for accessing System properties within a
     * <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @param defValue
     *            default value
     * @return {@link System.getProperty(String,String)} for the given
     *         <i>key</i>, <i>defValue</i>
     */
    public static String getSystemProperty(String key, String defValue)
    {
        return (String) AccessController.doPrivileged(new GetPropertyAction(key, defValue));
    }

    /**
     * Provides a simple utility method for accessing System properties within a
     * <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @param defValue
     *            default value
     * @return {@link Integer#getInteger(String)} for the given <i>key</i>,
     *         <i>defValue</i>
     */
    public static int getSystemProperty(String key, int defValue)
    {
        return ((Integer) AccessController.doPrivileged(new GetIntegerAction(key, defValue))).intValue();
    }

    /**
     * Provides a simple utility method for accessing System properties within a
     * <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @param defValue
     *            default value
     * @return {@link Long#getLong(String)} for the given <i>key</i>,
     *         <i>defValue</i>
     */
    public static long getSystemProperty(final String key, final long defValue)
    {
        class GetLongAction implements PrivilegedAction
        {
            public Object run()
            {
                return Long.getLong(key, defValue);
            }
        }
        return ((Long) AccessController.doPrivileged(new GetLongAction())).longValue();
    }

    /**
     * Initializes the OCAP properties. Expect port-specific properties to be
     * initialized via the INI file. The INI file can also be used to override
     * fixed and runtime properties.
     */
    static void initProperties()
    {
        // Load fixed properties (don't override if already set)
        InputStream is = MPEEnv.class.getResourceAsStream("/ocap.properties");
        if (is == null) is = MPEEnv.class.getResourceAsStream("ocap.properties");
        if (is != null)
        {
            Properties fixed = new Properties();
            try
            {
                fixed.load(is);
                for (Enumeration e = fixed.propertyNames(); e.hasMoreElements();)
                {
                    String key = (String) e.nextElement();
                    // Allow for runtime override
                    if (System.getProperty(key) == null) System.setProperty(key, fixed.getProperty(key));
                }
            }
            catch (java.io.IOException e)
            {
            }
        }

        // Load runtime properties
        String envVal;
        if ((envVal = MPEEnv.getEnv("ocap.hardware.vendor_id")) != null)
            System.setProperty("ocap.hardware.vendor_id", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.hardware.version_id")) != null)
            System.setProperty("ocap.hardware.version_id", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.hardware.createdate")) != null)
            System.setProperty("ocap.hardware.createdate", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.hardware.serialnum")) != null)
            System.setProperty("ocap.hardware.serialnum", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.memory.video")) != null) System.setProperty("ocap.memory.video", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.memory.total")) != null) System.setProperty("ocap.memory.total", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.system.highdef")) != null) System.setProperty("ocap.system.highdef", envVal);

        // ECR OCAP1.0.2-N-08.1216-3
        // additional platform specific system properties
        String apsspStr[] = { "ocap.hardware.version", "ocap.hardware.model_id", "ocap.software.model_id",
                "ocap.software.vendor_id", "ocap.software.version" };
        for (int x = 0; x < apsspStr.length; x++)
        {
            if ((envVal = MPEEnv.getEnv(apsspStr[x])) != null) System.setProperty(apsspStr[x], envVal);
        }

        // will be defined in DVR extension properties file if available
        if ((envVal = PropertiesManager.getInstance().getProperty("gem.recording.version.major", null)) != null)
        {
            System.setProperty("gem.recording.version.major", envVal);
        }
        if ((envVal = PropertiesManager.getInstance().getProperty("gem.recording.version.minor", null)) != null)
        {
            System.setProperty("gem.recording.version.minor", envVal);
        }
        if ((envVal = PropertiesManager.getInstance().getProperty("gem.recording.version.micro", null)) != null)
        {
            System.setProperty("gem.recording.version.micro", envVal);
        }

        // TODO: iterate over for all "ocap.api.option" properties and do
        // System.setProperty in the loop?
        // DVR Extension
        envVal = MPEEnv.getEnv("OCAP.guides.hnclient");
        if (envVal == null || !envVal.equalsIgnoreCase("true"))
        {
            if ((envVal = MPEEnv.getEnv("ocap.api.option.dvr")) != null) System.setProperty("ocap.api.option.dvr", envVal);
            if ((envVal = MPEEnv.getEnv("ocap.api.option.limited_storage_dvr")) != null) System.setProperty("ocap.api.option.limited_storage_dvr", envVal);
            if ((envVal = MPEEnv.getEnv("ocap.api.option.dvr.update")) != null)
                System.setProperty("ocap.api.option.dvr.update", envVal);
        }

        // Front Panel Extension
        if ((envVal = MPEEnv.getEnv("ocap.api.option.fp")) != null) System.setProperty("ocap.api.option.fp", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.api.option.fp.update")) != null)
            System.setProperty("ocap.api.option.fp.update", envVal);

        // MultiScreen Manager Support -- not supported
        // if ((envVal = MPEEnv.getEnv("ocap.api.option.msm")) != null)
        // System.setProperty("ocap.api.option.msm", envVal);

        // Device Settings Extension
        if ((envVal = MPEEnv.getEnv("ocap.api.option.ds")) != null) System.setProperty("ocap.api.option.ds", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.api.option.ds.update")) != null)
            System.setProperty("ocap.api.option.ds.update", envVal);

        // Home Networking Extension
        if ((envVal = MPEEnv.getEnv("ocap.api.option.hn")) != null) System.setProperty("ocap.api.option.hn", envVal);
        if ((envVal = MPEEnv.getEnv("ocap.api.option.hn.update")) != null)
            System.setProperty("ocap.api.option.hn.update", envVal);

        // RI proprietary properties
        String ocapBuild = MPEEnv.getEnv("MPE.SYS.OCAPVERSION");

        // Use RI Build number as HAVi version
        if (System.getProperty("havi.implementation.version") == null && ocapBuild != null)
            System.setProperty("havi.implementation.version", ocapBuild);

        // Host ID (OCAP1.0 Section 13.3.12.3
        if ((envVal = MPEEnv.getEnv("MPE.SYS.ID")) != null) System.setProperty("ocap.hardware.host_id", envVal);

        // Persistent storage properties. Also create the directories if they
        // don't already
        // exist
        if ((envVal = MPEEnv.getEnv("OCAP.persistent.root")) != null)
        {
            // Make sure the trailing slash is present
            if (envVal.charAt(envVal.length() - 1) != '/') envVal += '/';

            File dir = null;

            // App Storage
            if (MPEEnv.getEnv("OCAP.persistent.appstorage") == null)
                MPEEnv.setEnv("OCAP.persistent.appstorage", envVal + "app");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.appstorage"));
            if (!dir.exists()) dir.mkdirs();

            // User Preferences
            if (MPEEnv.getEnv("OCAP.persistent.userprefs") == null)
                MPEEnv.setEnv("OCAP.persistent.userprefs", envVal + "prefs");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.userprefs"));
            if (!dir.exists()) dir.mkdirs();

            // XAIT Persistent 
            if (MPEEnv.getEnv("OCAP.persistent.xaitstorage") == null)
                MPEEnv.setEnv("OCAP.persistent.xaitstorage", envVal + "xait");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.xaitstorage"));
            if (!dir.exists()) dir.mkdirs();

            // Certificate Revocation List
            if (MPEEnv.getEnv("OCAP.persistent.crlstor") == null)
                MPEEnv.setEnv("OCAP.persistent.crlstor", envVal + "crlstor");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.crlstor"));
            if (!dir.exists()) dir.mkdirs();

            // POD Generic Features
            if (MPEEnv.getEnv("OCAP.persistent.podgf") == null)
                MPEEnv.setEnv("OCAP.persistent.podgf", envVal + "podgf");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.podgf"));
            if (!dir.exists()) dir.mkdirs();

            // Addressable XAIT properties
            if (MPEEnv.getEnv("OCAP.persistent.addressableXAIT") == null)
                MPEEnv.setEnv("OCAP.persistent.addressableXAIT", envVal + "addrXAIT");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.addressableXAIT"));
            if (!dir.exists()) dir.mkdirs();

            // Host properties
            if (MPEEnv.getEnv("OCAP.persistent.host") == null)
                MPEEnv.setEnv("OCAP.persistent.host", envVal + "host");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.host"));
            if (!dir.exists()) dir.mkdirs();

            // Root certificates
            if (MPEEnv.getEnv("OCAP.persistent.certs") == null)
                MPEEnv.setEnv("OCAP.persistent.certs", envVal + "certs");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.certs"));
            if (!dir.exists()) dir.mkdirs();

            // DVR Storage -- TODO: This needs to be moved to some DVR-specific
            // area of the stack
            if (MPEEnv.getEnv("OCAP.persistent.dvr") == null)
                MPEEnv.setEnv("OCAP.persistent.dvr", envVal + "dvr");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.dvr"));
            if (!dir.exists()) dir.mkdirs();

            // dvb.persistent.root OCAP property
            String dvbRoot;
            if ((dvbRoot = MPEEnv.getEnv("OCAP.persistent.dvbroot")) == null)
                System.setProperty("dvb.persistent.root", envVal + "usr");
            else
                System.setProperty("dvb.persistent.root", dvbRoot);
            // dvb.persistent.root is created by the PersistentStorageAttributes
            // class
            
            // SI cache 
            if (MPEEnv.getEnv("OCAP.persistent.sicache") == null)
                MPEEnv.setEnv("OCAP.persistent.sicache", envVal + "si");
            dir = new File(MPEEnv.getEnv("OCAP.persistent.sicache"));
            if (!dir.exists()) dir.mkdirs();
        }

        // Set the java.io.tmpdir property
        String tmpDir = MPEEnv.getEnv("OCAP.javaio.tmpdir", "[javaiotmpdir]");
        System.setProperty("java.io.tmpdir", tmpDir);
    }

    private static Properties envVariables = new Properties();

    static native String nGetEnv(String key);

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        initProperties();
    }
}
