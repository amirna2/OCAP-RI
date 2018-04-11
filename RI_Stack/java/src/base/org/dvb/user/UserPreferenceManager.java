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

package org.dvb.user;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.apache.log4j.Logger;
import org.ocap.hardware.pod.POD;

/**
 * The UserPreferenceManager class gives access to the user preference settings.
 * This class provides a set of methods that allow an application to read or
 * save user settings. It also provides a mechanism to notify applications when
 * a preference has been modified. The value of a user setting,retrieved with
 * the read method, is a copy of the value that is stored in the receiver. The
 * write method, if authorized, overwrites the stored value.
 * <p>
 * When end-user preferences are read into a <code>Preference</code> object from
 * the MHP terminal, the ordering of these values shall be as determined by the
 * end-user, from most preferred to least preferred to the extent that this is
 * known.
 * <p>
 * NOTE: MHP implementations are not required to validate the values in
 * Preference objects, even those which are saved using the write method.
 * Applications with write permissions need to be very careful that the values
 * written are valid. Applications reading preferences need to be aware of the
 * possibility that a previous application has set an invalid value.
 *
 * @author Todd Earles
 * @author Aaron Kamienski
 * @author Greg Rutz
 */
public class UserPreferenceManager
{
    /**
     * Construct the single user preference manager
     */
    protected UserPreferenceManager()
    {
        // Read in the saved preferences
        preferenceFile = new File(MPEEnv.getEnv("OCAP.persistent.userprefs"), "userPrefsFile.dat");
        if (log.isDebugEnabled())
        {
            log.debug("file path is " + preferenceFile.getPath());
        }
        readPreferenceFile();
    }

    /**
     * Return an instance of the <code>UserPreferenceManager</code> for this
     * application. Repeated calls to this method by the same application shall
     * return the same instance.
     *
     * @return an instance of <code>UserPreferenceManager</code>
     */
    public static UserPreferenceManager getInstance()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getInstance()");
        }
        return manager;
    }

    /**
     * Allows an application to read a specified user preference.
     * When end-user preferences are read into a <code>Preference</code> object from the MHP
     * terminal, the ordering of these values shall be as determined by the end-user, from
     * most preferred to least preferred to the extent that this is known.
     *
     * @param p
     *            an object representing the preference to read.
     * @throws SecurityException
     *             if the calling application is denied access to this
     *             preference
     */
    public synchronized void read(Preference p) throws SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("read(Preference " + p.getName() + ")");
        }
        if (p == null)
        {
            throw new NullPointerException("null Preference");
        }

        // security check
        checkPermission(p.getName(), "read");

        // Lookup the preference by preference name. Use uppercase for lookup.
        Preference pref = (Preference) PREFS.get(p.getName().toUpperCase());

        // This preference may not yet exists or have been stored, so create a
        // default
        // empty prference that will eventually be returned
        if (pref == null)
        {
            pref = new GeneralPreference(p.getName());
            PREFS.put(p.getName().toUpperCase(), pref);
            pref = (Preference) PREFS.get(p.getName().toUpperCase());
        }

        // Use the default language from the POD if a preference has
        // not already been set
        if (p.getName().equalsIgnoreCase("User Language") &&
            pref.getMostFavourite() == null)
        {
            // If POD is not present/ready, don't bother reading
            POD pod = getPOD();
            if (pod.isReady())
            {
                // SCTE 28 defines the language feature ID as 0x08
                byte podLang[] = pod.getHostParam(8);

                // If we have received a valid lang from the POD, update the
                // user preference
                if (podLang.length == 3)
                {
                    // Modify the memory resident version of the user favourites
                    // by adding the
                    // POD language to the front of the list. If the language
                    // read out of the
                    // POD is already the favourite preference then this is a
                    // noop
                    String podLangString = new String(podLang);

                    pref.setMostFavourite(podLangString);

                    // I'm not sure if we should be doing anything if an
                    // exception is thrown
                    try
                    {
                        writeNoCheck(pref);
                        notifyUserPreferenceChangeListeners(new UserPreferenceChangeEvent(p.getName()));
                    }
                    catch (UnsupportedPreferenceException e) { }
                    catch (java.io.IOException e) { }
                }
            }
        }

        // Update the preference object passed in with the values stored for
        // this preference name.
        p.removeAll();
        p.add(pref.getFavourites());
    }

    /**
     * Allows an application to read a specified user preference taking into
     * account the facility defined by the application. After this method
     * returns, the values in the <code>Preference</code> object shall be the
     * values of that user preference with any unsupported values from the
     * <code>Facility</code> removed from that list. Note that the order of
     * values returned here need not be the same as that returned by
     * <code>read(Preference)</code>.
     * <p>
     * If the intersection between the two sets of values is empty then the
     * preference will have no value. If there is a mis-match between the name
     * of the preference used when constructing the facility and the name of the
     * preference used in this method then the preference will have no value.
     *
     * @param p
     *            an object representing the preference to read.
     * @param facility
     *            the preferred values the application for the preference
     * @throws SecurityException
     *             if the calling application is denied access to this
     *             preference
     */
    public synchronized void read(Preference p, Facility facility) throws SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("read(Preference " + p.getName() + ", Facility)");
        }

        if (p == null)
        {
            throw new NullPointerException("null Preference or Facility");
        }

        // Update the preference object passed in with the values stored for
        // this preference name.
        // Note: permissions are checked here
        read(p);

        // Filter out values not specified by the facility
        if (p.getName().equalsIgnoreCase(facility.getPreference()))
        {
            String[] fav = p.getFavourites();
            for (int i = 0; i < fav.length; ++i)
            {
                if (!facility.accept(fav[i])) p.remove(fav[i]);
            }
        }
        else
        {
            p.removeAll();
        }
    }

    /**
     * Saves the specified user preference. If this method succeeds then it will
     * change the value of this preference for all future MHP applications.
     *
     * @param p
     *            the preference to save.
     * @throws UnsupportedPreferenceException
     *             if the preference provided is not a standardized preference
     *             as defined for use with <code>GeneralPreference</code>.
     * @throws java.lang.SecurityException
     *             if the application does not have permission to call this
     *             method
     * @throws IOException
     *             if saving the preference fails for other I/O reasons
     */
    public synchronized void write(Preference p) throws UnsupportedPreferenceException, IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("UserPreferenceManager.write(" + p.getName() + ")");
        }
        if (p == null)
            throw new NullPointerException("null Preference");

        // Make sure it is a valid preference
        if (!(p instanceof GeneralPreference))
            throw new UnsupportedPreferenceException();

        // security check
        checkPermission(p.getName(), "write");

        // Validate the values for this preference
        try
        {
            ((GeneralPreference)p).validate();
        }
        catch (UnsupportedPreferenceException e)
        {
            SystemEventUtil.logRecoverableError("Invalid GeneralPreference value for preference \"" + p.getName() + "\".  Ignoring!", e);
            return;
        }

        // Perform actual write operation
        writeNoCheck(p);

        // Update the POD with the preference if applicable
        // Set the first preferred language as the default language in the POD
        if (p.getName().equalsIgnoreCase("User Language"))
        {
            // Only Update POD's language if POD is present/ready
            POD pod = getPOD();
            if (pod.isReady())
            {
                String favouriteLang = p.getMostFavourite();

                if (favouriteLang != null && favouriteLang.length() == 3)
                {
                    // Create the byte array required by the POD
                    byte podLang[] = new byte[3];
                    for (int i = 0; i < 3; ++i)
                    {
                        podLang[i] = (byte) favouriteLang.charAt(i);
                    }

                    // SCTE 28 defines the feature ID for language as 0x08
                    pod.updateHostParam(8, podLang);
                }
            }
        }

        notifyUserPreferenceChangeListeners(new UserPreferenceChangeEvent(p.getName()));
    }

    /**
     * Implementation of <code>write(Preference)</code> without parameter and
     * permission checks. This is called internally by {@link #write} and
     * elsewhere if necessary.
     *
     * @param p
     *            the preference to save.
     * @throws IOException
     *             if saving the preference fails for other I/O reasons
     */
    private void writeNoCheck(Preference p) throws UnsupportedPreferenceException, IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("UserPreferenceManager.writeNoCheck(" + p.getName() + ")");
        }
        // Store the preference keyed by preference name
        String prefName = p.getName();


        // This implementation supports a maximum of 255 unique preference types
        if (PREFS.size() == 255) throw new IOException("Maximum number of preferences reached (255)");

        // Due to the fact that case insensitive preference names are allowed,
        // we'll use uppercase to prevent duplicate preferences. NOTE: We must
        // make
        // a copy of the preference parameter so that changes to the object
        // (provided
        // by the application) are not reflected in our preference database.
        GeneralPreference prefToWrite = new GeneralPreference(p.getName());
        prefToWrite.add(p.getFavourites());
        PREFS.put(prefName.toUpperCase(), prefToWrite);
        // Write changes out to preference file
        writePreferenceFile();
    }

    /**
     * Read the user preference file from the persistent store data file The
     * format of that file is as follows:
     *
     * <pre>
     * numPrefs                     <em>Number of preferences</em>      <code>(1 byte)</code>
     * for i = 0 to numPrefs
     *      prefName_i              <em>UTF-8 Encoded String</em>
     *      numPrefValues           <em>Number of pref favourites</em>  <code>(1 byte)</code>
     *      for j = 0 to numPrefValues
     *          prefValue           <em>UTF-8 Encoded String</em>
     * </pre>
     */
    private void readPreferenceFile()
    {
        if (log.isDebugEnabled())
        {
            log.debug("readPreferenceFile()");
        }
        if (preferenceFile == null)
            return;

        // Open preference file for reading
        DataInputStream dis = null;
        CheckedInputStream cis = null;
        try
        {
            cis = new CheckedInputStream(new BufferedInputStream(openPrefFileForRead()), new CRC32());
            dis = new DataInputStream(cis);
        }
        // Pref file not written yet, so just return
        catch (FileNotFoundException e)
        {
            return;
        }

        // Read number of preferences. If for any reason, reading the file
        // fails, do
        // not update the internal hashtable representation
        Hashtable newTable = new Hashtable();

        try
        {
            int numPrefs = dis.readByte(); // Number of prefs in the file

            for (int prefCount = 0; prefCount < numPrefs; ++prefCount)
            {
                // Read pref name size and value
                GeneralPreference gp = new GeneralPreference(dis.readUTF());

                // Read the number of preference values associated with this
                // preference
                int numPrefValues = dis.readByte();

                // Read each preference value size and value and add to our new
                // hashtable
                for (int i = 0; i < numPrefValues; ++i)
                {
                    String prefValue = dis.readUTF();
                    gp.add(prefValue);
                }

                newTable.put(gp.getName().toUpperCase(), gp); // Add pref to our
                                                              // new hashtable
            }

            // Get computed checksum from bytes read up to this point
            long checksum = cis.getChecksum().getValue();

            // Compare with file checksum
            if (checksum != dis.readLong()) return;

            dis.close();
        }
        catch (IOException e)
        {
            return;
        }

        // If we made it to here, the file was read properly, so update our
        // internal
        // data structure
        // TODO: note Hashtable.putAll() isn't available pre-Java2
        PREFS.putAll(newTable);
    }

    /**
     * Write the user preferences file to the persistent store data file The
     * format of that file is as follows:
     *
     * <pre>
     * numPrefs                     <em>Number of preferences</em>      <code>(1 byte)</code>
     * for i = 0 to numPrefs
     *      prefName_i              <em>UTF-8 Encoded String</em>
     *      numPrefValues           <em>Number of pref favourites</em>  <code>(1 byte)</code>
     *      for j = 0 to numPrefValues
     *          prefValue           <em>UTF-8 Encoded String</em>
     * checksum                     <em>CRC32 file checksum</em>        <code>(8 bytes)</code>
     * </pre>
     *
     * @throws IOException
     *             If the preference file could not be written
     */
    private void writePreferenceFile() throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("writePreferenceFile()");
        }
        if (preferenceFile == null)
            return;

        // Open preference file for reading
        FileOutputStream fos = null;
        try
        {
            fos = openPrefFileForWrite();
        }
        catch (FileNotFoundException e)
        {
            return;
        } // Pref file not written yet, so just return

        synchronized (fos)
        {
            CheckedOutputStream cos = new CheckedOutputStream(new BufferedOutputStream(fos), new CRC32());
            DataOutputStream dos = new DataOutputStream(cos);

            // Write the total number of prefs
            dos.writeByte(PREFS.size());

            // Write each preference
            for (Enumeration e = PREFS.elements(); e.hasMoreElements();)
            {
                Preference p = (Preference) e.nextElement();

                // Write preference name size and value
                dos.writeUTF(p.getName());

                // Write the number of favourites and then each size and value
                String favs[] = p.getFavourites();
                dos.writeByte(favs.length);
                for (int i = 0; i < favs.length; ++i)
                {
                    dos.writeUTF(favs[i]);
                }
            }

            // Write the checksum value
            dos.writeLong(cos.getChecksum().getValue());

            dos.close();
        }
    }

    /**
     * Opens the file specified by the given <i>filename</i> for reading,
     * returning a <code>FileInputStream</code>. The file is opened within a
     * privileged action block so that system access permissions are used.
     *
     * @param filename
     *            the name of the file to open
     * @return a <code>FileInputStream</code> if the file could be opened
     * @throws FileNotFoundException
     */
    private FileInputStream openPrefFileForRead() throws FileNotFoundException
    {
        if (log.isDebugEnabled())
        {
            log.debug("openPrefFileForRead(): pref file is " + preferenceFile);
        }
        try
        {
            FileInputStream fis = (FileInputStream) AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws FileNotFoundException
                {
                    return new FileInputStream(preferenceFile);
                }
            });
            return fis;
        }
        catch (PrivilegedActionException pae)
        {
            Exception e = pae.getException();
            if (e instanceof FileNotFoundException)
                throw (FileNotFoundException) e;
            else
                throw new RuntimeException("Unexpected exception thrown: " + e);
        }
    }

    /**
     * Opens the file specified by the given <i>filename</i> for writing,
     * returning a <code>FileOutputStream</code>. The file is opened within a
     * privileged action block so that system access permissions are used.
     *
     * @param filename
     *            the name of the file to open
     * @return a <code>FileOutputStream</code> if the file could be opened
     * @throws IOException
     */
    private FileOutputStream openPrefFileForWrite() throws FileNotFoundException
    {
        if (log.isDebugEnabled())
        {
            log.debug("openPrefFileForWrite(): pref file is " + preferenceFile);
        }
        try
        {
            FileOutputStream fos = (FileOutputStream) AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws FileNotFoundException
                {
                    return new FileOutputStream(preferenceFile);
                }
            });
            return fos;
        }
        catch (PrivilegedActionException pae)
        {
            Exception e = pae.getException();
            if (e instanceof FileNotFoundException)
                throw (FileNotFoundException) e;
            else
                throw new RuntimeException("Unexpected exception thrown: " + e);
        }
    }

    /**
     * Adds a listener for changes in user preferences as held in the MHP terminal.
     * Specifically this includes changes made by MHP applications succeeding in
     * calling the write() method on this class. If the implementation of the MHP
     * terminal allows the end user to change preferences then these changes also
     * includes changes made to preferences by this mechanism. It does not include
     * changes made to a Preference instance within the scope of a single MHP application.
     *
     * @param l
     *            the listener to add.
     */
    public void addUserPreferenceChangeListener(UserPreferenceChangeListener l)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addUserPreferenceChangeListener(" + l.getClass() + ")");
        }
        addUserPreferenceChangeListener(l, ccm.getCurrentContext());
    }

    /**
     * Removes a listener for changes in user preferences.
     *
     * @param l
     *            the listener to remove.
     */
    public void removeUserPreferenceChangeListener(UserPreferenceChangeListener l)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeUserPreferenceChangeListener(" + l.getClass() + ")");
        }
        removeUserPreferenceChangeListener(l, ccm.getCurrentContext());
    }

    /**
     * Returns the <code>POD</code> singleton. This invokes
     * <code>POD.getInstance</code> within a privileged action block.
     *
     * @return {@link POD#getInstance}
     */
    private POD getPOD()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getPod()");
        }
        return (POD) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return POD.getInstance();
            }
        });
    }

    /**
     * Notify <code>UserPreferenceChangeListener</code>s of changes to a user
     * preference. <i>This method is not part of the defined public API, but is
     * present for the implementation only.</i>
     *
     * @param e
     *            the event to deliver
     */
    private void notifyUserPreferenceChangeListeners(UserPreferenceChangeEvent e)
    {
        if (log.isDebugEnabled())
        {
            log.debug("UserPreferenceManager.notifyUserPreferenceChangeListeners(" + e.getName() + ")");
        }
        final UserPreferenceChangeEvent event = e;
        CallerContext contexts = listenerContexts;

        if (contexts != null)
        {
            Runnable run = new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();

                    // Listeners are maintained in-context
                    Data data = (Data) ctx.getCallbackData(UserPreferenceManager.this);

                    UserPreferenceChangeListener l = data.upListeners;
                    if (l != null) l.receiveUserPreferenceChangeEvent(event);
                }
            };
            contexts.runInContext(run);
        }
    }

    /**
     * Add a <code>UserPreferenceChangeListener</code> to this manager for the
     * given calling context.
     *
     * @param listener
     *            the <code>UserPreferenceChangeListener</code> to be added to
     *            this manager.
     * @param ctx
     *            the context of the application installing the listener
     */
    private void addUserPreferenceChangeListener(UserPreferenceChangeListener listener, CallerContext ctx)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addUserPreferenceChangeListener(" + listener.getClass().toString() + ")");
        }
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(ctx);

            // Update listener/multicaster
            data.upListeners = EventMulticaster.add(data.upListeners, listener);

            // Manage context/multicaster
            listenerContexts = Multicaster.add(listenerContexts, ctx);

            if (log.isDebugEnabled())
            {
                log.debug("Added listeners to EventMulticaster and contexts to Multicaster");
            }
        }
    }

    /**
     * Remove a <code>UserPreferenceChangeListener</code> from this manager for
     * the given calling context.
     *
     * @param listener
     *            the <code>UserPreferenceChangeListener</code> to be removed to
     *            this manager.
     * @param ctx
     *            the context of the application removing the listener
     */
    private void removeUserPreferenceChangeListener(UserPreferenceChangeListener listener, CallerContext ctx)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeUserPreferenceChangeListener(" + listener.getClass().toString() + ")");
        }
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = (Data) ctx.getCallbackData(this);

            // Remove the given listener from the set of listeners
            if (data != null && data.upListeners != null)
            {
                data.upListeners = EventMulticaster.remove(data.upListeners, listener);
            }
        }
    }

    /**
     * Access this device's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     *
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx)
    {
        synchronized (lock)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
            }
            return data;
        }
    }

    /**
     * Cleans up after a CallerContext is destroyed, forgetting any listeners
     * previously associated with it.
     *
     * @param ctx
     *            the context to forget
     */
    private void cleanup(CallerContext ctx)
    {
        if (log.isDebugEnabled())
        {
            log.debug("cleanup() - all contexts are removed");
        }
        synchronized (lock)
        {
            // Remove ctx from the set of contexts with listeners
            listenerContexts = Multicaster.remove(listenerContexts, ctx);
        }
    }

    /**
     * Performs a security check on the caller to see if they have
     * <code>UserPreferencePermission</code>.
     * <p>
     * All applications (including unsigned) can read a subset of the
     * preferences. Explicit <code>UserPreferencePermission</code> is required
     * in order to write <i>any</i> preferences or read <i>all</i> preferences.
     *
     * @param prefName
     *            name of the preference
     * @param action
     *            whether the caller wants to read or write the preference
     * @throws SecurityException
     *
     * @see "MHP 12.6.2.15"
     * @see GeneralPreference#alwaysAccessible
     */
    private void checkPermission(String prefName, String action) throws SecurityException
    {
        if ("read".equals(action))
        {
            for (int i = 0; i < GeneralPreference.alwaysAccessible.length; i++)
            {
                if (GeneralPreference.alwaysAccessible[i].equalsIgnoreCase(prefName))
                {
                    // this preference is always accessible
                    return;
                }
            }
        }
        // does the caller have permission to read or write
        SecurityUtil.checkPermission(new UserPreferencePermission(action));
    }

    /**
     * A hash table of Preference objects keyed by preference name.
     */
    // Added final modifier for findbugs issues fix
    private static final Hashtable PREFS = new Hashtable();

    /**
     * CallerContext multicaster for executing UserPreferenceChangeListener.
     */
    private CallerContext listenerContexts = null;

    /**
     * General purpose lock. Private lock used to avoid using <code>this</code>
     * for synchronization.
     */
    private Object lock = new Object();

    /**
     * Preference persistent data file
     */
    protected File preferenceFile;

    /**
     * Singleton instance of the CallerContextManager.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * Logging mechanism
     */
    private static final Logger log = Logger.getLogger(UserPreferenceManager.class.getName());

    /**
     * Holds context-specific data. Specifically the set of
     * <code>UserPreferenceChangeListener</code>s.
     */
    private class Data implements CallbackData
    {
        public UserPreferenceChangeListener upListeners = null;

        public void destroy(CallerContext ctx)
        {
            cleanup(ctx);
        }

        public void active(CallerContext ctx)
        {
        }

        public void pause(CallerContext ctx)
        {
        }
    }

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }

    /**
     * The singleton instance of the user preference manager. Note this is last
     * to ensure that it occurs last during static initialization.
     */
    private static final UserPreferenceManager manager = new UserPreferenceManager();
}
