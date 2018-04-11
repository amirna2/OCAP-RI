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

package org.cablelabs.impl.ocap.hardware;

import java.io.File;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.cablelabs.impl.persistent.PersistentDataSerializer;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

public class HostPersistence
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(HostPersistence.class.getName());

    private HostDataWriter hostWriter = null;

    private HostData hostData = null;

    protected Object synch = new Object();

    protected HostPersistence()
    {
        hostWriter = new HostDataWriter();
        // hostData is initialized during load
    }

    protected String getHostProperty(String key)
    {
        String value = (String) getHostData().get(key);
        return value;
    }

    /**
     * Provides a simple utility method for accessing host persisted values
     * within a <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @param defValue
     *            default value
     * @return {@link System.getProperty(String,String)} for the given
     *         <i>key</i>, <i>defValue</i>
     */
    public String getHostProperty(String key, String defValue) throws HostPersistenceException
    {
        String value = getHostProperty(key);

        if (value == null)
            return defValue;
        else
            return value;
    }

    /**
     * Provides a simple utility method for accessing host persisted values
     * within a <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @param defValue
     *            default value
     * @return {@link Integer#getInteger(String)} for the given <i>key</i>,
     *         <i>defValue</i>
     */

    public int getHostProperty(String key, final int defValue) throws HostPersistenceException

    {
        String value = getHostProperty(key);

        if (value == null)
            return defValue;
        else
            return Integer.parseInt(value);
    }

    /**
     * Provides a simple utility method for accessing host persisted values
     * within a <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @param defValue
     *            default value
     * @return {@link Long#getLong(String)} for the given <i>key</i>,
     *         <i>defValue</i>
     */
    public long getHostProperty(final String key, final long defValue) throws HostPersistenceException
    {
        String value = getHostProperty(key);

        if (value == null)
            return defValue;
        else
            return Long.parseLong(value);
    }

    /**
     * Provides a simple utility method for accessing host persisted values
     * within a <code>PrivilegedAction</code> block.
     * 
     * @param key
     *            property key
     * @param defValue
     *            default value
     * @return {@link Long#getLong(String)} for the given <i>key</i>,
     *         <i>defValue</i>
     */
    public float getHostProperty(final String key, final float defValue) throws HostPersistenceException
    {
        String value = getHostProperty(key);

        if (value == null)
            return defValue;
        else
            return Float.parseFloat(value);
    }

    public void persistHostProperty(final String key, final int value) throws HostPersistenceException
    {
        writeHostProperty(key, Integer.toString(value));
    }

    public void persistHostProperty(final String key, final boolean value) throws HostPersistenceException
    {
        writeHostProperty(key, Boolean.toString(value));
    }

    public void persistHostProperty(final String key, final long value) throws HostPersistenceException
    {
        writeHostProperty(key, Long.toString(value));
    }

    public void persistHostProperty(final String key, final float value) throws HostPersistenceException
    {
        writeHostProperty(key, Float.toString(value));
    }

    public void persistHostProperty(final String key, final String value) throws HostPersistenceException
    {
        writeHostProperty(key, value);
    }

    protected void hostDataPut(String key, String value)
    {
        getHostData().put(key, value);
    }

    protected void writeHostProperty(String key, String value)
    {
        synchronized (synch)
        {
            getHostData().put(key, value);
            hostWriter.saveData();
        }
    }

    protected void writeHostData()
    {
        synchronized (synch)
        {
            hostWriter.saveData();
        }
    }

    public void resetAllDefaults()
    {
        try
        {
            // delete original file otherwise partial reset may occur if power
            // goes off during resetAllDefaults, etc.
            hostWriter.delete();
            loadDefaults();
            hostWriter.saveData();
        }
        catch (HostPersistenceException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Initializes the host persistence values.
     * 
     * Does not persist the values to the file system.
     * 
     */
    protected void loadDefaults() throws HostPersistenceException
    {
        getHostData().clear();
    }

    /**
     * implements a configuration settings persistence utility based on the
     * PersistentDataSerializer framework. defined above as: protected static
     * final String DEFAULT_DIR = "/syscwd/persistent/"; protected static final
     * String BASEDIR_PROP = "OCAP.persistent.host"; // mpeenv.ini protected
     * static final String DEFAULT_SUBDIR = "host"; protected static final
     * String FILE_NAME = "hostData";
     * 
     * 
     * 
     * 
     */
    protected class HostDataWriter extends PersistentDataSerializer
    {
        HostDataWriter()
        {
            super(new File(MPEEnv.getEnv("OCAP.persistent.host")), "hostData");
        }

        /*
         * Save recording manager configuration to disk
         */
        void saveData()
        {
            // assert: m_configData initialized at recmgr init
            // Save off recording manager configuration data
            try
            {
                getHostData().setToPersisted(); // okay to do here, value won't
                                                // be saved if failure
                save(getHostData());
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Exception saving host data" + e);
                }

            }
        }

        void delete()
        {
            super.delete(getHostData());
        }

        /**
         * Read recording manager configuration data from persistent storage
         */
        void loadData()
        {
            if (log.isDebugEnabled())
            {
                log.debug("Loading Host persistence data");
            }

            Vector loaded = load();

            if (loaded == null || loaded.size() == 0)
            {
                hostData = newHostData();

                try
                {
                    loadDefaults();
                }
                catch (HostPersistenceException e)
                {
                    SystemEventUtil.logCatastrophicError("Unable to initialize new HostData", e);
                    throw new RuntimeException(e);
                }

                if (log.isDebugEnabled())
                {
                    log.debug("Default host Persistence data enabled");
                }
                return;
            }

            // We expect only one config entry
            if (loaded.size() != 1)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Unexpected host persistence count: " + loaded.size());
                }
            }

            // grab the first entry
            hostData = (HostData) loaded.firstElement();
            if (hostData == null)
            {
                // no configuration found. Set default values
                hostData = new HostData();
                if (log.isDebugEnabled())
                {
                    log.debug("Default host persistence enabled " + loaded.size());
                }
            }
        }
    }

    public void load()
    {
        hostWriter.loadData();
    }

    protected HostData newHostData()
    {
        return new HostData();
    }

    protected HostData getHostData()
    {
        return hostData;
    }

}
