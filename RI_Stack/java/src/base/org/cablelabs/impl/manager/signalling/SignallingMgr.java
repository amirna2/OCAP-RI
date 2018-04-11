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

package org.cablelabs.impl.manager.signalling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.SignallingManager;
import org.cablelabs.impl.manager.application.AttributesImpl;
import org.cablelabs.impl.manager.pod.PODListener;
import org.cablelabs.impl.pod.mpe.PODEvent;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AitImpl;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.SignallingEvent;
import org.cablelabs.impl.signalling.SignallingListener;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.davic.net.InvalidLocatorException;
import org.dvb.application.AppID;
import org.ocap.application.AppSignalHandler;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;
import org.ocap.system.MonitorAppPermission;

import sun.security.action.GetPropertyAction;

/**
 * This is the base class for <code>SignallingManager</code> implementations
 * based upon different methods of signalling acquisition. This provides a
 * common infrastructure for registering of listeners, parsing of the AIT and
 * XAIT, and the delivery of events.
 * 
 * @author Aaron Kamienski
 */
public abstract class SignallingMgr implements SignallingManager
{
    /**
     * Constructor only callable from subclasses.
     */
    protected SignallingMgr()
    {
        // Determine persistent storage file names
        String dir = MPEEnv.getEnv("OCAP.persistent.addressableXAIT");
        persistentPropertiesFile = dir + File.separator + "address_props";
        tempPersistentPropertiesFile = dir + File.separator + "_address_props_";

        // Load any persistent addressing properties
        final Hashtable[] properties = new Hashtable[] { new Hashtable() };
        Boolean success = (Boolean) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                BufferedReader reader = null;
                try
                {
                    reader = new BufferedReader(new FileReader(persistentPropertiesFile));

                    boolean error = false;

                    while (true)
                    {
                        RegisteredAddressingProperty rap = new RegisteredAddressingProperty();
                        rap.expirationDate = null;
                        rap.persist = false;

                        // Read in the first value for this entry. Null
                        // indicates
                        // proper end-of-file
                        String key = reader.readLine();
                        if (key == null)
                        {
                            break;
                        }

                        // Read in rest of values, break on premature
                        // end-of-file
                        String propVal = reader.readLine();
                        String date = reader.readLine();
                        if (propVal == null || date == null || propVal.equals(""))
                        {
                            error = true;
                            break;
                        }

                        rap.propertyValue = propVal;

                        // Validate Expiration Date
                        long dateMillis = Long.parseLong(date);
                        if (dateMillis == -1 || dateMillis > System.currentTimeMillis())
                        {
                            rap.persist = true;
                            if (dateMillis != -1)
                            {
                                rap.expirationDate = new Date(dateMillis);
                            }
                        }

                        if (rap.persist)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("loaded persistent addressing property - (key = " +
                                          key + ", value = " + propVal + ")");
                            }
                            properties[0].put(key, rap);
                        }
                    }

                    // Only update our properties if the file was properly
                    // formed
                    if (!error)
                    {
                        return new Boolean(true);
                    }
                }
                catch (IOException e)
                {
                    // No persistent properties or corrupt properties file --
                    // ignore
                }
                finally
                {
                    if (reader != null) try
                    {
                        reader.close();
                    }
                    catch (IOException e) { }
                }
                return new Boolean(false);
            }
        });

        if (success.booleanValue())
        {
            registeredAddressingProperties = properties[0];
        }

        //register a listener that will stop and start XAIT monitoring when events are received
        //this listener is never removed
        PODListener podListener = new PODListener()
        {
            //XAITMonitor is lazily constructed in addXAITListener method, so this listener will no-op until an xait listener is registered
            public void notify(PODEvent event)
            {
                if (log.isInfoEnabled())
                {
                    log.info("received PODEvent - ID: 0x" + Integer.toHexString(event.getEvent()));
                }
                synchronized (this)
                {
                    switch (event.getEvent())
                    {
                        case PODEvent.EventID.POD_EVENT_POD_READY:
                            if (xaitMonitor != null)
                            {
                                xaitMonitor.startMonitoring();
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("xaitMonitor is null, not starting monitoring");
                                }
                            }
                            break;
                        case PODEvent.EventID.POD_EVENT_RESET_PENDING:
                            if (xaitMonitor != null)
                            {
                                xaitMonitor.stopMonitoring();
                            }
                            else
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug("xaitMonitor is null, not stopping monitoring");
                                }
                            }
                            break;
                        default:
                            // intentionally left empty -- ignore other events
                    }
                }
            }
        };

        ((PODManager) ManagerManager.getInstance(PODManager.class)).addPODListener(podListener);
    }

    /**
     * Stops and forgets any currently monitored application signalling.
     */
    public synchronized void destroy()
    {
        if (xaitMonitor != null)
        {
            xaitMonitor.stopMonitoring();
            xaitMonitor = null;
        }

        for (Enumeration e = monitors.elements(); e.hasMoreElements();)
        {
            SignallingMonitor monitor = (SignallingMonitor) e.nextElement();
            monitor.stopMonitoring();
        }
        monitors.clear();
    }

    /**
     * Provides for the implementation of the
     * {@link org.ocap.application.AppManagerProxy#setAppSignalHandler} method.
     * <P>
     * This will set (or clear) the singleton instance of
     * <code>AppSignalHandler</code> that is consulted before updating the
     * services database with information from an XAIT. If set, then the
     * <code>AppSignalHandler</code> will be called with
     * <code>OcapAppAttributes</code> corresponding to the entries in the new
     * XAIT.
     * <p>
     * Note that appropriate security permissions should be tested prior to
     * invoking this method.
     * 
     * @param handler
     *            An instance of a class implementing the AppSignalHandler
     *            interface that decides whether application information is
     *            updated using the new version of the XAIT or not If null is
     *            set, the AppSignalHandler be removed.
     */
    public void setAppSignalHandler(AppSignalHandler handler)
    {
        synchronized (this)
        {
            if (signalHandler != null)
            {
                signalHandler.dispose();
            }

            signalHandler = (handler != null) ? (new AppSignalContext(handler)) : null;
        }
    }

    /**
     * Adds a <code>SignallingListener</code> interested in AIT for the given
     * service.
     * <ol>
     * <li>Creates a <code>SignallingMonitor</code> for the given service, if
     * necessary.
     * <li>Adds the listener to the <code>SignallingMonitor</code>
     * <li>Calls {@link SignallingMonitor#startMonitoring}, if this is the first
     * listener.
     * </ol>
     */
    public synchronized void addAitListener(OcapLocator service, SignallingListener l)
    {
        OcapLocator key = makeSimple(service);
        SignallingMonitor monitor = (SignallingMonitor) monitors.get(key);

        if (monitor == null)
        {
            monitor = createAitMonitor(key);
            monitors.put(key, monitor);
        }

        if (monitor.addListener(l))
        {
            monitor.startMonitoring();
        }
    }

    /**
     * Adds a <code>SignallingListener</code> interested in XAIT.
     * <ol>
     * <li>Creates a <code>SignallingMonitor</code>, if necessary.
     * <li>Adds the listener to the <code>SignallingMonitor</code>
     * <li>Calls {@link SignallingMonitor#startMonitoring}, if this is the first
     * listener.
     * </ol>
     */
    public synchronized void addXaitListener(SignallingListener l)
    {
        if (xaitMonitor == null)
        {
            xaitMonitor = createXaitMonitor();
        }

        if (xaitMonitor.addListener(l))
        {
            xaitMonitor.startMonitoring();
        }
    }

    /**
     * Removes the <code>SignallingListener</code> interested in AIT for the
     * given service.
     * <ol>
     * <li>Acquires the previously created <code>SignallingMonitor</code>
     * <li>Removes the listener.
     * <li>Calls {@link SignallingMonitor#stopMonitoring}, if this is the last
     * listener
     * </ol>
     */
    public synchronized void removeAitListener(OcapLocator service, SignallingListener l)
    {
        OcapLocator key = makeSimple(service);
        SignallingMonitor monitor = (SignallingMonitor) monitors.get(key);

        if (monitor != null && monitor.removeListener(l))
        {
            monitor.stopMonitoring();
            monitors.remove(key);
        }
    }

    /**
     * Remvoes the <code>SignallingListener</code> interested in XAIT.
     * <ol>
     * <li>Acquires the previously created <code>SignallingMonitor</code>
     * <li>Removes the listener.
     * <li>Calls {@link SignallingMonitor#stopMonitoring}, if this is the last
     * listener
     * </ol>
     */
    public synchronized void removeXaitListener(SignallingListener l)
    {
        if ((null != xaitMonitor) && (xaitMonitor.removeListener(l)))
        {
            xaitMonitor.stopMonitoring();
            xaitMonitor = null;
        }
    }

    /**
     * Instructs the <code>SignallingManager</code> to re-signal the given AIT
     * or XAIT. If the given table has not been recently signalled or if this
     * signalling has been replaced by newer signalling, this call is ignored.
     * 
     * @param ait
     *            The XAIT or AIT to resignal
     */
    public synchronized void resignal(Ait ait)
    {
        // Scan in-band monitors looking for this signalling
        for (Enumeration e = monitors.elements(); e.hasMoreElements();)
        {
            SignallingMonitor monitor = (SignallingMonitor) e.nextElement();
            synchronized (monitor)
            {
                if (monitor.lastSignalled == ait)
                {
                    monitor.lastSignalled = null;
                    monitor.resignal();
                }
            }
        }

        // Out-of-band monitor
        if (xaitMonitor != null)
        {
            synchronized (xaitMonitor)
            {
                if (xaitMonitor.lastSignalled == ait)
                {
                    xaitMonitor.lastSignalled = null;
                    xaitMonitor.resignal();
                }
            }
        }
    }

    /**
     * Instructs the <code>SignallingManager</code> to re-signal the AIT or XAIT
     * that contains the given <code>AppID</code>. If the given
     * <code>AppID</code> has not been signaled, this call is ignored
     * 
     * @param appID
     *            The application ID whose signalling we want to re-signal
     */
    public void resignal(AppID appID)
    {
        // Scan in-band monitors looking for this signalling
        for (Enumeration e = monitors.elements(); e.hasMoreElements();)
        {
            SignallingMonitor monitor = (SignallingMonitor) e.nextElement();
            synchronized (monitor)
            {
                Ait ait = monitor.lastSignalled;
                if (ait != null)
                {
                    for (int i = 0; i < ait.getApps().length; i++)
                    {
                        if (ait.getApps()[i].id.equals(appID))
                        {
                            monitor.lastSignalled = null;
                            monitor.resignal();
                        }
                    }
                }
            }
        }

        // Out-of-band monitor
        if (xaitMonitor != null)
        {
            synchronized (xaitMonitor)
            {
                Xait ait = (Xait)xaitMonitor.lastSignalled;
                if (ait != null)
                {
                    AbstractServiceEntry[] services = ait.getServices();
                    for (int i = 0; i < services.length; i++)
                    {
                        AbstractServiceEntry service = services[i];
                        for (Iterator j = service.apps.iterator(); j.hasNext();)
                        {
                            XAppEntry xae = (XAppEntry)j.next();
                            if (xae.id.equals(appID))
                            {
                                xaitMonitor.lastSignalled = null;
                                xaitMonitor.resignal();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Registers unbound applications.
     * <ol>
     * <li>If no XAIT listener(s) is (are) resolved then does nothing.
     * <li>Parses the Xait and invokes listener(s)
     * </ol>
     */
    public void registerUnboundApp(java.io.InputStream xaitStream)
        throws IllegalArgumentException, SecurityException, java.io.IOException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("registrar"));

        // Parse the XAIT contained in the given InputStream
        final XaitParser xaitParser = new XaitParser(Xait.REGISTER_UNBOUND_APP);
        xaitParser.parse(xaitStream);

        // Dispatch event to the listener(s)
        if (xaitMonitor != null)
        {
            CallerContextManager ccm = (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);
            ccm.getCurrentContext().runInContextAsync(new Runnable() {
                public void run()
                {
                    xaitMonitor.handleSignalling(xaitParser.getSignalling(), true, true);
                }
            });
        }
    }

    /**
     * Registers addressing properties used for comparison when an
     * addressing_descriptor from an AIT or XAIT is evaluated. The
     * implementation SHALL maintain a set of properties registered by any
     * application. The implementation SHALL adhere to the following rules in
     * order when registering each property passed in the properties parameter:
     * <li>When a property contains a value that is not an instance of
     * java.lang.String the property is ignored.</li> <li>When a property key is
     * 0 length the property is ignored.</li> <li>When a property key is a
     * duplicate of a Java system property the property is ignored.</li> <li>
     * When a property key is a duplicate of a property previously registered by
     * this method it is ignored.</li> <li>When a property key is a duplicate of
     * an addressable attribute retrieved from the security system the property
     * is ignored.</li> <li>When a property key is not registered and the value
     * is not a 0 length String the property is added. If a property is not
     * registered and the property value is a 0 length String the property is
     * ignored.</li>
     * 
     * @param properties
     *            The set of properties to be registered.
     * @param persist
     *            If true the properties parameters are stored in persistent
     *            storage, otherwise they are not stored and SHALL be removed
     *            immediately if previously stored.
     * @param expirationDate
     *            Date the implementation SHALL remove the properties from
     *            persistent storage. Only applies if the persist parameter is
     *            set to true. If the date is in the past then no expiration
     *            date is set.
     * 
     * @throws SecurityException
     *             if the calling application is not granted
     *             MonitorAppPermission("properties").
     */
    public void registerAddressingProperties(Properties properties, boolean persist, Date expirationDate)
    {
        // Must have MonitorAppPermission("properties")
        SecurityUtil.checkPermission(new MonitorAppPermission("properties"));

        if (log.isDebugEnabled())
        {
            log.debug("Register addressing properties: persist = " + persist +
                      ", expiration = " + expirationDate);
        }
        
        // Check persistence and ensure expiration date is valid
        boolean shouldPersist = false;
        Date exprDate = null;
        if (persist)
        {
            if (expirationDate == null ||
                expirationDate.getTime() > System.currentTimeMillis()) // Valid expiration date
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Register addressing properties: expiration date is valid");
                }
        
                shouldPersist = true;
                if (expirationDate != null) exprDate = (Date) expirationDate.clone();
            }
        }

        synchronized (propertiesLock)
        {
            // Validate each property and potentially store it
            for (Enumeration e = properties.keys(); e.hasMoreElements();)
            {
                // Key must be a non-zero length String
                Object key = e.nextElement();
                if (!(key instanceof String) || ((String) key).length() == 0)
                {
                    continue;
                }

                String keyString = (String) key;
                
                if (log.isDebugEnabled())
                {
                    log.debug("Register addressing properties: key = " + keyString);
                }

                // Key must not be a Java system property, a previously
                // registered
                // property, or a security property returned by the CableCARD
                if (AccessController.doPrivileged(new GetPropertyAction(keyString)) != null ||
                    registeredAddressingProperties.get(keyString) != null ||
                    securityProperties.getProperty(keyString) != null)
                {
                    continue;
                }

                // Value must be a non-zero length string
                Object value = properties.get(key);
                if (!(value instanceof String) || ((String) value).length() == 0)
                {
                    continue;
                }

                RegisteredAddressingProperty rap = new RegisteredAddressingProperty();
                rap.propertyValue = (String) value;
                rap.expirationDate = exprDate;
                rap.persist = shouldPersist;

                if (log.isDebugEnabled())
                {
                    log.debug("Register addressing properties: value = " + rap.propertyValue);
                }

                // All checks passed. Update our database
                registeredAddressingProperties.put(keyString, rap);
            }
        }

        updateAddressingPropertyPersistence();

        // OCAP-1.0 11.2.2.5.5
        reEvaluateAllSignalling();
    }

    /**
     * Gets the addressing properties previously registered by the
     * <code>registerAddressingProperties</code> method. The set of properties
     * returned by this method may be out of date as soon as this method
     * returns.
     * 
     * @return The set of registered addressing properties. If no addressing
     *         properties have been registered an empty Properties object is
     *         returned.
     */
    public Properties getAddressingProperties()
    {
        Properties retVal = new Properties();

        synchronized (propertiesLock)
        {
            for (Enumeration e = registeredAddressingProperties.keys(); e.hasMoreElements();)
            {
                String key = (String) e.nextElement();
                RegisteredAddressingProperty value = (RegisteredAddressingProperty) registeredAddressingProperties.get(key);
                retVal.setProperty(key, value.propertyValue);
            }
        }
        return retVal;
    }

    /**
     * Removes addressing properties set by the
     * <code>registerAddressingProperties</code> method. Each String in the
     * properties parameter SHALL be compared to registered property keys and if
     * a match is found the property SHALL be removed. If the properties
     * parameter is null, all registered properties SHALL be removed from both
     * volatile storage and non-volatile storage if persistently stored.
     * 
     * @param properties
     *            The properties to remove.
     * 
     * @throws SecurityException
     *             if the calling application is not granted
     *             MonitorAppPermission("properties").
     */
    public void removeAddressingProperties(String[] properties)
    {
        // Must have MonitorAppPermission("properties")
        SecurityUtil.checkPermission(new MonitorAppPermission("properties"));

        synchronized (propertiesLock)
        {
            // If properties list is null, clear our registered properties
            if (properties == null)
            {
                registeredAddressingProperties.clear();
            }
            else
            {
                // Remove each property
                for (int i = 0; i < properties.length; ++i)
                {
                    String key = properties[i];

                    if (key == null)
                    {
                        continue;
                    }
                    
                    if (log.isDebugEnabled())
                    {
                        log.debug("Register addressing properties: key = " + key);
                    }

                    if (registeredAddressingProperties.containsKey(key))
                    {
                        registeredAddressingProperties.remove(key);
                    }
                }
            }

            updateAddressingPropertyPersistence();
        }
    }

    /**
     * Gets the security system Host addressable attributes queried by the
     * implementation. The implementation SHALL format addressable attributes
     * sent by the security system into name/value pairs in the returned
     * <code>Properties</code>. The set of properties returned by this method
     * may be out of date as soon as this method returns.
     * 
     * @return The set of addressable attributes set by the security system.
     */
    public Properties getSecurityAddressableAttributes()
    {
        synchronized (propertiesLock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Security Addressable Properties:");
                for (Enumeration e = securityProperties.keys(); e.hasMoreElements();)
            {
                    String key = (String)e.nextElement();
                    log.debug("    " + key + ", " + securityProperties.getProperty(key));
            }
            }
            return new Properties(securityProperties);
        }
    }

    /**
     * Walks through all registered monitors and refresh our list of in-use
     * security properties from the latest signalling. This action may start or
     * stop the polling thread depending on whether the security properties list
     * changed from empty to not or vice versa
     */
    private synchronized void refreshSecurityProperties()
    {
        // Update our set of security properties with any potential
        // additions or removals from this new signalling
        synchronized (propertiesLock)
        {
            // Create a new list of all security properties that we can
            // compare against the original list
            Properties newSecurityProperties = new Properties();

            // Scan in-band monitors
            for (Enumeration e = monitors.elements(); e.hasMoreElements();)
            {
                SignallingMonitor monitor = (SignallingMonitor) e.nextElement();
				// Added for findbugs issues fix
				// Added synchronization on proper objects
                synchronized(monitor)
                {
                    updateSecurityProperties(monitor.lastSignalled, newSecurityProperties);
                }
            }
            // Out-of-band monitor
            if (xaitMonitor != null)
            {
                synchronized(xaitMonitor)
                {
                    updateSecurityProperties(xaitMonitor.lastSignalled, newSecurityProperties);    
                }
            }

            // Start or stop our security property polling thread
            if (securityProperties.isEmpty() && !newSecurityProperties.isEmpty())
            {
                pollingThread = new SecurityPollingThread();
                pollingThread.start();
            }
            else if (!securityProperties.isEmpty() && newSecurityProperties.isEmpty())
            {
                pollingThread.kill();
            }

            // Update our current security properties set
            securityProperties.clear();
            securityProperties.putAll(newSecurityProperties);
        }
    }

    /**
     * Update the given properties list with all security properties from the
     * most recently signaled table. The property value will be retrieved from
     * the current signalling manager properties set. If the property is not
     * found in the current set, the value will be retrieved directly from the
     * POD
     * 
     * @param ait
     *            the new AIT or XAIT signalling
     * @param newSecurityProps
     *            the properties set to populate
     */
    private void updateSecurityProperties(Ait ait, Properties newSecurityProps)
    {
        AitImpl aitImpl = (AitImpl)ait;
        if (aitImpl != null)
        {
            // For each security property in this table, either copy its
            // most recently retrieved value from the old security props
            // list or go retrieve the value from the POD
            for (Iterator i = aitImpl.getSecurityProps().iterator(); i.hasNext();)
            {
                String propName = (String) i.next();
                String propValue = securityProperties.getProperty(propName);
                if (propValue == null)
                {
                    propValue = getPODHostAddressableProperty(propName);
                }
                newSecurityProps.setProperty(propName, propValue);
            }
        }
    }

    /**
     * Instructs the signalling manager to re-evaluate all of our pre-existing
     * signalling to determine if property changes have triggered addressable
     * application changes
     * 
     * @param listener
     *            the listener(s) to notify of the event; should never be
     *            <code>null</code>
     * @param ait
     *            the <code>Ait</code> (or <code>Xait</code>) to deliver
     */
    protected void reEvaluateAllSignalling()
    {
        // Scan in-band monitors
        for (Enumeration e = monitors.elements(); e.hasMoreElements();)
        {
            SignallingMonitor monitor = (SignallingMonitor) e.nextElement();
			// Added for findbugs issues fix
			// Added synchronization on proper objects
            synchronized(monitor)
            {
                monitor.handleSignalling(monitor.lastSignalled, true, false);
            }
        }

        // Out-of-band monitor
        if (xaitMonitor != null)
        {
            synchronized(xaitMonitor)
            {
                xaitMonitor.handleSignalling(xaitMonitor.lastSignalled, true, false);
            }
        }
    }

    /**
     * Re-evaluates the persistence state of all registered properties. Updates
     * are made to the persistent storage file if necessary.
     * 
     * Each persistent property takes 3 lines in the file:
     * 
     * <i>property_key</i> <i>property_value</i> <i>expiration_date</i>
     * 
     * where <i>expiration_date</i> is the string form of Date.getTime() long
     * integer. If no expiration date, -1.
     */
    private void updateAddressingPropertyPersistence()
    {
        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                PrintWriter writer = null;
                try
                {
                    writer = new PrintWriter(new FileOutputStream(tempPersistentPropertiesFile));

                    // Check each property to see if we should persist it to the
                    // file
                    for (Enumeration e = registeredAddressingProperties.keys(); e.hasMoreElements();)
                    {
                        String key = (String) e.nextElement();
                        RegisteredAddressingProperty rap = (RegisteredAddressingProperty) registeredAddressingProperties.get(key);

                        // Only persist those properties designated as
                        // persistent
                        if (rap.persist)
                        {
                            if (rap.expirationDate != null
                                    && rap.expirationDate.getTime() <= System.currentTimeMillis())
                            {
                                // Expiration Date is passed, so remove
                                // persistence
                                rap.persist = false;
                                rap.expirationDate = null;
                                continue;
                            }

                            // Add property key and value to our file
                            writer.println(key);
                            writer.println(rap.propertyValue);

                            // Check expiration date
                            if (rap.expirationDate == null)
                                writer.println("-1");
                            else
                                writer.println("" + rap.expirationDate.getTime());
                        }
                    }
                    writer.close();

                    // Delete old file and rename our temp
                    File oldFile = new File(persistentPropertiesFile);
                    oldFile.delete();

                    // Rename our new file
                    File newFile = new File(tempPersistentPropertiesFile);
                    newFile.renameTo(oldFile);
                }
                catch (IOException e1)
                {
                    // Log this error
                } finally
                {
                    if (writer != null)
                    {
                        writer.close();
                    }
                }
                return null;
            }
        });
    }

    /**
     * Simplifies the given locator so that it contains the minimum information
     * in order to refer to a service. If the original locator is
     * sourceID-based, then it returns a locator that only contains the
     * sourceID. If the original locator is frequency:program-based, then it
     * returns a locator that only contains the frequency:program:qam.
     * <p>
     * It is assumed that the locator is already canonicalized (i.e., it has a
     * sourceID or frequency:program).
     * 
     * @param loc
     *            the sourceID or frequency:program locator
     * @return the simplified version of the locator
     */
    private OcapLocator makeSimple(OcapLocator loc)
    {
        int sourceId = loc.getSourceID();

        try
        {
            if (sourceId != -1) return new OcapLocator(sourceId);

            return new OcapLocator(loc.getFrequency(), loc.getProgramNumber(), loc.getModulationFormat());
        }
        catch (Exception e)
        {
            return loc;
        }
    }

    /**
     * Factory method used to create an instance of a
     * <code>SignallingMonitor</code> for monitoring out-of-band XAIT
     * signalling. There will be only one XAIT <code>SignallingMonitor</code> in
     * use at a time.
     * 
     * @return <code>SignallingMonitor</code> to use for monitoring XAIT
     */
    protected abstract SignallingMonitor createXaitMonitor();

    /**
     * Factory method used to create an instance of
     * <code>SignallingMonitor</code> for monitoring in-band AIT signalling.
     * There will be only one AIT <code>SignallingMonitor</code> in use for a
     * given <i>serviceId</i> at a time; however, there may be as many
     * <code>SignallingMonitor</code>s in use as there are
     * <code>NetworkInterface</code>s.
     * 
     * @param service
     *            locator for service
     * @return <code>SignallingMonitor</code> to use for monitoring AIT for the
     *         given service
     */
    protected abstract SignallingMonitor createAitMonitor(OcapLocator service);

    /**
     * The current XaitMonitor.
     */
    protected SignallingMonitor xaitMonitor = null;

    /**
     * The set of service-specific monitors.
     */
    private Hashtable monitors = new Hashtable();

    /**
     * An instance of <code>SignallingMonitor</code> is created for every
     * necessary instance of AIT/XAIT monitoring. That is, an single instance is
     * created to monitor the out-of-band XAIT as well as one for each in-band
     * AIT that is actively being monitored.
     * 
     * @author Aaron Kamienski
     */
    protected abstract class SignallingMonitor
    {
        /**
         * Setup and initiate monitoring of application signalling given the
         * appropriate source. If at any point monitoring is stopped for some
         * reason other than a call to {@link #stopMonitoring} (e.g., not enough
         * resources, a tune-away occured), then the
         * <code>SignallingMonitor</code> must set itself up to resume the
         * monitoring as soon as possible.
         */
        public abstract void startMonitoring();

        /**
         * Terminate and teardown monitoring of application signalling for the
         * appropriate source. This includes the releasing of any resources
         * acquired as part of {@link #startMonitoring}.
         */
        public abstract void stopMonitoring();

        /**
         * Ask this monitor to remove any version checking and just acquire the
         * next available signalling
         */
        public abstract void resignal();

        /**
         * Adds the given listener.
         * 
         * @return <code>true</code> if this is the first listener added
         */
        public synchronized boolean addListener(SignallingListener l)
        {
            boolean start = (listeners == null);
            listeners = EventMulticaster.add(listeners, l);
            return start;
        }

        /**
         * Removes the given listener.
         * 
         * @return <code>true</code> if this is the last listener removed
         */
        public synchronized boolean removeListener(SignallingListener l)
        {
            listeners = EventMulticaster.remove(listeners, l);
            return listeners == null;
        }

        /**
         * Instructs monitor to handle the given new signalling and notify any
         * listeners
         * 
         * @param ait
         *            the <code>Ait</code> (or <code>Xait</code>) to deliver
         * @param filter
         *            true if we should filter the signaled apps based on
         *            registered addressable attributes, false if no filtering
         *            should take place
         * @param registered
         *            true if this signaling is a result of a call to registerUnboundApp
         *            and therefore, listeners should be notified in the
         *            calling context, false if the listeners should be notified
         *            in the system context.
         * @return false if problems were encountered during app download; this
         *         is an indicator to the signalling implementation that it
         *         should re-send the same (X)AIT again next time. Returns true
         *         if no errors occurred
         */
        protected synchronized void handleSignalling(Ait ait, boolean filter, boolean registered)
        {
            if (listeners == null) return;
            
            // Only remember the last live signaling, not registerUnboundApps signaling
            if (!registered)
                lastSignalled = ait;

            boolean notify = true;

            // Filter the addressable apps for this signaled AIT based on our
            // updated properties
            if (filter)
            {
                // Ensure that our security properties list is up to date
                refreshSecurityProperties();

                // We only want to notify our listeners if the list of valid apps
                // has actually changed due to filtering
                AitImpl aitImpl = (AitImpl)ait;
                notify = aitImpl.filterApps(getSecurityAddressableAttributes(), getAddressingProperties());
            }

            // If this is an XAIT, check with the registered app signal handler
            // to see if we can release this XAIT to the stack
            if (ait instanceof Xait)
            {
                AppSignalContext handler = null;
                synchronized (SignallingMgr.this)
                {
                    handler = signalHandler;
                }
                
                // If there is an XAIT handler, only update the database with this
                // XAIT if the handler says it is OK, otherwise resignal this XAIT
                Xait xait = (Xait)ait;
                if (xait.getSource() == Xait.NETWORK_SIGNALLING &&
                    handler != null && !handler.notifyXAITUpdate((Xait) ait))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("XAIT was rejected by appSignalHandler! resignaling...");
                    }
                    resignal();
                    notify = false;
                }
            }

            if (notify)
            {
                final SignallingEvent event = new SignallingEvent(SignallingMonitor.this, ait);

                if (!registered)
                {
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    ccm.getSystemContext().runInContextAsync(new Runnable()
                    {
                        public void run()
                        {
                            synchronized(SignallingMonitor.this)
                            {
                                //listeners may have been nulled out
                                if (listeners != null)
                                {
                                    listeners.signallingReceived(event);
                                }
                            }
                        }
                    });
                }
                else
                {
                    listeners.signallingReceived(event);
                }
            }
        }

        /**
         * The most recently signaled table by this monitor
         */
        protected Ait lastSignalled = null;

        /**
         * The installed listener(s).
         */
        protected SignallingListener listeners = null;
    }

    // This lock is held by a thread while updating either of our properties
    // sets (security, registered) or while filtering addressable applications
    // based on those properties.
    private Object propertiesLock = new Object();

    // Stores RegisteredAddressingProperty structures keyed by property name
    private Hashtable registeredAddressingProperties = new Properties();

    // Used to store registered addressing properties
    private class RegisteredAddressingProperty
    {
        public boolean persist;

        public Date expirationDate;

        public String propertyValue;
    }

    private String persistentPropertiesFile;

    private String tempPersistentPropertiesFile;

    // Complete list of security properties referenced by all currently
    // signaled X/AITs and their values.
    private Properties securityProperties = new Properties();

    // This thread implements the polling logic described in OCAP-1.0 11.2.2.5.5
    // for security attributes
    private SecurityPollingThread pollingThread;

    private class SecurityPollingThread extends Thread
    {
        private volatile boolean run = true;

        public SecurityPollingThread()
        {
            super("Signaling Polling Thread");
        }

        // Stop the thread
        public void kill()
        {
            run = false;
            interrupt();
        }

        public void run()
        {
            while (run)
            {
                try
                {
                    // Sleep first since this thread will be started right after
                    // querying the security system for attribute values
                    Thread.sleep(60000);
                }
                catch (InterruptedException e)
                {
                    break;
                }

                boolean valueChanged = false;
                synchronized (propertiesLock)
                {
                    // Look for changes in our current set of security
                    // properties
                    for (Enumeration e = securityProperties.keys(); e.hasMoreElements();)
                    {
                        String propName = (String) e.nextElement();
                        String propValue = securityProperties.getProperty(propName);
                        String newPropValue = getPODHostAddressableProperty(propName);

                        if (!propValue.equals(newPropValue)) valueChanged = true;
                    }
                }

                if (valueChanged) reEvaluateAllSignalling();
            }
        }
    }

    /**
     * Forgets the current <code>AppSignalHandler</code> if it is represented by
     * the given <code>AppSignalContext</code>.
     * 
     * @param handler
     *            the <code>AppSignalContext</code> to forget
     */
    private synchronized void clearSignalHandler(AppSignalContext handler)
    {
        if (signalHandler == handler) signalHandler = null;
    }

    /**
     * This class encapsulates a <code>CallerContext</code> and an
     * <code>AppSignalHandler</code>. Implements <code>CallbackData</code> so
     * that it can remove itself when the installing application goes away.
     * 
     * @author Aaron Kamienski
     */
    private class AppSignalContext implements org.cablelabs.impl.manager.CallbackData
    {
        /**
         * Calls <code>handler.notifyXAITUpdate() from within the context of
         * the caller who installed the <i>handler</i>.
         * It is also the responsibility of this method to generate the
         * <code>OcapAppAttributes</code> to be passed to the
         * <code>AppSignalHandler</code> from the given <code>Xait</code>
         * object.
         * 
         * @param xait
         *            the new XAIT
         * @return <code>true</code> if the XAIT is accepted
         */
        boolean notifyXAITUpdate(Xait xait)
        {
            final OcapAppAttributes[] apps = getApps(xait);
            final boolean[] ret = { true };

            CallerContext.Util.doRunInContextSync(ctx, new Runnable()
            {
                public void run()
                {
                    ret[0] = handler.notifyXAITUpdate(apps);
                }
            });
            return ret[0];
        }

        /**
         * Creates an array of <code>OcapAppAttributes</code> from the given
         * <i>xait</i>.
         * 
         * @param xait
         *            the new XAIT
         * @return an array of <code>OcapAppAttributes</code> from the given
         *         <i>xait</i>.
         */
        private OcapAppAttributes[] getApps(Xait xait)
        {
            Vector attributes = new Vector();
            
            // Iterate through our abstract services
            AbstractServiceEntry[] services = xait.getServices();
            for (int i = 0; i < services.length; i++)
            {
                AbstractServiceEntry ase = services[i];
                OcapLocator loc;
                try
                {
                    loc = new OcapLocator(ase.id);
                }
                catch (InvalidLocatorException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("AppSignalContext -- Could not construct service locator. Service id = " + ase.id);
                    }
                    continue;
                }
                
                // Construct attributes objects for each app in this service
                for (Iterator j = ase.apps.iterator(); j.hasNext();)
                {
                    attributes.add(new AttributesImpl((AppEntry)j.next(),loc));
                }
            }
            
            OcapAppAttributes[] retVal = new OcapAppAttributes[attributes.size()];
            attributes.copyInto(retVal);
            return retVal;
        }

        public void pause(CallerContext cc)
        { /* empty */
        }

        public void active(CallerContext cc)
        { /* empty */
        }

        /**
         * Ensures that this handler isn't used anymore.
         */
        public void destroy(CallerContext cc)
        {
            dispose();
            clearSignalHandler(this);
        }

        /**
         * Simple constructor.
         */
        AppSignalContext(AppSignalHandler handler)
        {
            this.handler = handler;

            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            this.ctx = ccm.getCurrentContext();
            this.ctx.addCallbackData(this, this);
        }

        /**
         * Disposes of this <code>AppSignalHandler</code> by causing the
         * associated <code>CallerContext</code> to forget about it.
         */
        void dispose()
        {
            ctx.removeCallbackData(this);
        }

        private CallerContext ctx;

        private AppSignalHandler handler;
    }

    /**
     * The currently installed <code>AppSignalHandler</code> and its associated
     * <code>CallerContext</code> represented as an
     * <code>AppSignalContext</code>. Defaults to <code>null</code> indicating
     * no installed handler.
     */
    private AppSignalContext signalHandler = null;

    private native String getPODHostAddressableProperty(String propertyName);

    private static final Logger log = Logger.getLogger(SignallingMgr.class.getName());
}
