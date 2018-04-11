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

package org.cablelabs.impl.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

/**
 * This class manages the Java properties that configure the OCAP stack and any
 * enabled extensions.
 * 
 * The set of properties for each Java module (base, dvr, hn, etc.) fall into a
 * precedence order as desired by the developers. The list of available modules
 * (and their associated precedence order) is defined in the file
 * <i>props.properties</i> located in the same directory as the
 * <code>PropertiesManager</code> class file.
 * 
 * Each module is identified by its name string as defined in the
 * <i>props.properties</i> file. When the properties manager is initialized, it
 * will attempt to load a module-specific properties file for each entry from
 * the classpath. A module properties file is named according to the following
 * convention:
 * <p>
 * <i>[module_name].properties</i>
 * <p>
 * The <code>PropertiesManager</code> will only load a single properties file
 * for each module listed in <i>props.properties</i>. If more than one
 * module-specific properties file exists in the classpath, the first one found
 * will be used.
 */
public class PropertiesManager
{
    private static final Logger log = Logger.getLogger(PropertiesManager.class.getName());

    private static final PropertiesManager theInstance = new PropertiesManager();

    // List of java.util.Properties objects for each registered module
    // in order of increasing precedence
    private Vector moduleProperties = new Vector();

    /**
     * Returns the singleton instance of the <code>PropertiesManager</code>
     * 
     * @return the instance
     */
    public static PropertiesManager getInstance()
    {
        return theInstance;
    }

    /**
     * Initialize the properties manager
     */
    private PropertiesManager()
    {
        // Load the main module properties configuration file
        InputStream is = getClass().getResourceAsStream("props.properties");
        if (is == null)
        {
            ManagerManager.error("Could not locate module configuration file!");
            return;
        }
        Properties p = new Properties();
        try
        {
            p.load(is);
        }
        catch (IOException e)
        {
            ManagerManager.error("Could not load module configuration file!", e);
            return;
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                // ignore
            }
        }

        // Get the maxium number of modules to read
        int maxModules = Integer.parseInt(p.getProperty("OCAP.properties.maxModules", "10"));

        // Load properties files for each module
        for (int i = 0; i < maxModules; i++)
        {
            String moduleName;
            InputStream moduleIS;
            if ((moduleName = p.getProperty("OCAP.properties.module." + i)) != null)
            {
                // Load module-specific properties file from classpath root
                String propFileName = "/" + moduleName + ".properties";
                moduleIS = getClass().getResourceAsStream(propFileName);
                if (moduleIS == null)
                {
                    continue;
                }

                Properties moduleProps = new Properties();
                try
                {
                    // Load the properties from file and add to the beginning of
                    // our list so that the list ends up being in order of
                    // decreasing precedence.
                    moduleProps.load(moduleIS);
                    moduleProperties.add(0, moduleProps);
                    if (log.isInfoEnabled())
                    {
                        log.info("loading module properties for: " + moduleName);
                    }
                }
                catch (IOException e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Could not load module properties (" + propFileName + ")", e);
                    }
                } finally
                {
                    try
                    {
                        moduleIS.close();
                    }
                    catch (IOException e)
                    {
                        // ignore
                    }
                }
            }
        }
    }

    /**
     * Returns the value of the given property from the module with highest
     * precedence.
     * <p>
     * As an example, this method can be used by the <code>ManagerManager</code>
     * during instantiation of managers based on which extensions are enabled.
     * For example, for instantiation of a <code>StorageManager</code>. The
     * <i>base</i> module properties file would define this property:
     * <p>
     * <i>OCAP.mgrmgr.StorageManager=org.cablelabs.impl.manager.storage.
     * StorageManager<i>
     * <p>
     * While the <i>dvr</i> extension module properties file would define the
     * same property this way:
     * <p>
     * <i>OCAP.mgrmgr.StorageManager=org.cablelabs.impl.manager.storage.
     * DVRStorageManager</i>
     * <p>
     * If the stack was compiled with the DVR extension enabled and its module
     * had a higher precedence than the base module, the DVR property value
     * would be the value returned
     * 
     * @param name
     *            the full property name
     * @return the property value from the highest precedence module or null if
     *         property name was not found in any module's property list
     */
    public String getPropertyValueByPrecedence(String name)
    {
        // Iterate over the list of module properties (highest precedence first)
        // looking for the first instance of the given property name
        for (ListIterator i = moduleProperties.listIterator(); i.hasNext();)
        {
            String value;
            Properties p = (Properties) i.next();

            if ((value = p.getProperty(name)) != null)
            {
                return value;
            }
        }

        return null;
    }

    /**
     * A convenience method for retrieving a property value. Exact same behavior
     * as {@link #getPropertyValueByPrecedence(String)}.
     * 
     * @param name
     *            the property name
     * @param defaultValue
     *            if the property name is not found, this value is returned
     * @return the property value
     */
    public String getProperty(String name, String defaultValue)
    {
        String val = getPropertyValueByPrecedence(name);

        return (val == null) ? defaultValue : val;
    }

    /**
     * Returns a set of properties (keys and values) whose key names start with
     * the given property name prefix. When identical property names are found
     * across modules, only the highest precedence property is returned. When
     * identical property names are found within a single property, the property
     * actually returned is implementation-dependent
     * 
     * @param propNamePrefix
     *            the property name prefix used to match against properties in
     *            all modules
     * @return all properties whose names begin with the given property name
     *         prefix or an empty list if no matching properties were found in
     *         any module
     */
    public Properties getPropertiesByPrecedence(String propNamePrefix)
    {
        Properties matchingProps = new Properties();

        // Iterate over the list of module properties and add all values
        // found starting with the given property name prefix. Iterate in
        // reverse order -- if we are not allowing duplicates, we want the
        // higher precedence properties to override the lower
        for (int i = moduleProperties.size() - 1; i >= 0; i--)
        {
            Properties p = (Properties) moduleProperties.elementAt(i);

            // Iterate over each key in the property set looking for
            // property names starting with our prefix
            for (Enumeration e = p.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                if (key.startsWith(propNamePrefix)) matchingProps.setProperty(key, p.getProperty(key));
            }
        }

        return matchingProps;
    }

    /**
     * Helper function which constructs a single object from a property name
     * prefix (the prefix discovered in the extension properties files must
     * resolve to a fully qualified class name).
     * 
     * This function calls getPropertyValueByPrecedence to determine the highest
     * precedence class name and uses reflection to call the no-arg constructor
     * for the value.
     * 
     * If a property value cannot be instantiated, a message is logged and null
     * is returned.
     * 
     * @param name
     *            the property name used to retrieve fully-qualified class name
     * @return the object representing the highest precedence property value
     *         found
     */
    public Object getInstanceByPrecedence(String name)
    {
        String className = getPropertyValueByPrecedence(name);
        if (className != null)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getInstanceByPrecedence for: " + name + ": " + className);
                }
                return Class.forName(className).newInstance();
            }
            catch (InstantiationException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct: " + className, e);
                }
            }
            catch (IllegalAccessException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct: " + className, e);
                }
            }
            catch (ClassNotFoundException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct: " + className, e);
                }
            }
        }
        else
        {
            if (log.isWarnEnabled())
            {
                log.warn("No property found for: " + name);
            }
        }

        return null;
    }

    /**
     * Helper function which constructs an ordered list of instances from a
     * property name prefix (each prefix discovered in the extension properties
     * files must resolve to a fully qualified class name).
     * 
     * This function calls getAllPropertyValues to build the property list and
     * uses reflection to call the no-arg constructor for each property value.
     * 
     * If a property value cannot be instantiated, a message is logged, and the
     * resulting list willl contain all instances that were successfully
     * instantiated.
     * 
     * @param propNamePrefix
     *            the prefix used to retrieve fully-qualified class names
     * @return the ordered list of instances that were successfully
     *         instantiated, which may be an empty list, but will not be null
     */
    public List getInstancesByPrecedence(String propNamePrefix)
    {
        List classes = new ArrayList();
        List classNames = getAllPropertyValues(propNamePrefix);
        if (log.isDebugEnabled())
        {
            log.debug("getInstancesByPrecedence for: " + propNamePrefix + ": " + classNames);
        }

        for (Iterator iter = classNames.iterator(); iter.hasNext();)
        {
            String className = iter.next().toString();
            try
            {
                classes.add(Class.forName(className).newInstance());
            }
            catch (InstantiationException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct: " + className, e);
                }
            }
            catch (IllegalAccessException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct: " + className, e);
                }
            }
            catch (ClassNotFoundException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to construct: " + className, e);
                }
            }
        }
        return classes;
    }

    /**
     * Returns a list of all property values across all modules for the given
     * property name prefix. The property values are returned in order --
     * highest precedence first. If multiple properties starting with the given
     * prefix are found in a single module, their order is
     * implementation-dependent.
     * <p>
     * As an example, this method can be used by the stack to locate all
     * available <i>delegates</i> of a particular type. For example, the
     * <i>base</i> module would define the standard
     * <code>ServiceContextDelegateFactory</code> implementation like this:
     * <p>
     * <i>OCAP.serviceContextDelegateFactory=
     * AbstractServiceContextDelegateFactory</i>
     * <p>
     * And the DVR module would define a delegate factory to handle presentation
     * of recorded services like this:
     * <p>
     * <i>OCAP.serviceContextDelegateFactory.1=
     * DVRBroadcastServiceContextDelegateFactory</i>
     * <i>OCAP.serviceContextDelegateFactory
     * .2=RecordedServiceContextDelegateFactory</i>
     * <p>
     * The presentation engine can query all
     * <code>ServiceContextDelegateFactory</code> implementations to determine
     * which one can handle the service that needs to be presented.
     * 
     * @param propNamePrefix
     *            the property name prefix
     * @return a list of all property values whose names begin with the given
     *         property name prefix or a 0-length list if no matching properties
     *         were found in any module
     */
    public List getAllPropertyValues(String propNamePrefix)
    {
        List allPropValues = new ArrayList();

        // Iterate over the list of module properties and add all values
        // found starting with the given property name prefix.
        for (ListIterator i = moduleProperties.listIterator(); i.hasNext();)
        {
            Properties p = (Properties) i.next();

            // Iterate over each key in the property set looking for
            // property names starting with our prefix
            for (Enumeration e = p.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                if (key.startsWith(propNamePrefix)) allPropValues.add(p.getProperty(key));
            }
        }
        return new ArrayList(allPropValues);
    }
}
