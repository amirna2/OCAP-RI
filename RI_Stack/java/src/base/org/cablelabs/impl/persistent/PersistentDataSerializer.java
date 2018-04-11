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

package org.cablelabs.impl.persistent;

import org.cablelabs.impl.util.SystemEventUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.CheckedOutputStream;

import org.apache.log4j.Logger;

/**
 * <code>PersistentDataSerializer</code> is an abstract base class that provides
 * for robust serialization of data objects (extensions of
 * {@link PersistentData}.
 * <p>
 * There are no public methods on this class. It is expected that this class
 * will be subclassed as appropriate. If desirable, the subclass can expose the
 * explicit save/restore methods.
 * <p>
 * The local file system is used as the database, with individual persistent
 * objects occupying individual files in the file system. Persistent data files
 * are named by their {@link PersistentData#uniqueId} so that it is easy to
 * locate the file given the <code>PersistentData</code>.
 * <p>
 * In order to provide for more robust updates, we never overwrite a file
 * directly. Instead we write to a secondary file, then delete the original, and
 * finally rename the secondary file. When reading files in we look for the
 * secondary file first. If no secondary file is found or it is corrupt, we fall
 * back to the original filename.
 * <p>
 * Files will be names using a hexadecimal representation of the uniqueId. E.g.,
 * <code>"15"</code> or <code>"15.1"</code> for <code>uniqueId</code> of 21.
 * 
 * @author Aaron Kamienski
 */
public abstract class PersistentDataSerializer
{
    /**
     * Construct only callable from self and subclasses.
     * <p>
     * Corrupt entries are always deleted.
     * 
     * @param baseDir
     *            the root directory for all persistent files
     * @param prefix
     *            the prefix value used for all persistent files
     */
    protected PersistentDataSerializer(File baseDir, String prefix)
    {
        this(baseDir, prefix, true);
    }

    /**
     * Construct only callable from self and subclasses.
     * 
     * @param baseDir
     *            the root directory for all persistent files
     * @param prefix
     *            the prefix value used for all persistent files
     * @param deleteCorruptEntries
     *            if <code>true</code> then all corrupt entries will be deleted;
     *            if <code>false</code> then they will be left around but
     *            ignored
     */
	// Added for findbugs issues fix - start
    protected PersistentDataSerializer(final File baseDir, String prefix, boolean deleteCorruptEntries)
    {
        this.baseDir = baseDir;
        this.prefix = prefix;
        this.deleteCorruptEntries = deleteCorruptEntries;

        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                /**
                 * Creates the {@link #baseDir} base
                 * directory} and initializes {@link #lastUniqueId}.
                 * <p>
                 */
                // Create baseDir
                baseDir.mkdirs();
                if (!baseDir.exists())
                    throw new RuntimeException("Internal error - could not create DB directory " + baseDir);
                else if (!baseDir.isDirectory())
                    throw new RuntimeException("Internal error - could not access DB directory " + baseDir);
        
                lastUniqueId = initLastUniqueId(baseDir);

                if (log.isInfoEnabled())
                {
                    log.info("Initialized: " + this);
                }
		            
                return null;
            }
        });
    }
	// Added for findbugs issues fix - end

    /**
     * Determine the initialization value for the <code>lastUniqueId</code>.
     * This implementation of the method examines the existing persistent files
     * stored in the file-system and returns the maximum id represented by
     * those.
     * <p>
     * This method may be overridden by a subclass to use a different scheme.
     * For example, a subclass may store the lastUniqueId in a persistent file
     * itself and simply return it.
     * <p>
     * <strong>Note</strong> that this method is called during construction
     * time. It is not recommended that it rely on any instance variables that
     * would be initialized by a constructor (beyond those initialized by this
     * class' constructor).
     * 
     * @param base
     *            the base directory where persistent files are stored
     * @return the last used uniqueId
     */
    protected long initLastUniqueId(File base)
    {
        long id = 0;

        // Initialize lastUniqueId from files stored in file-system
        Filter filter = new Filter();
        String[] list = base.list(filter);

        if (list != null && list.length != 0) id = filter.max;

        return id;
    }

    /**
     * Overrides {@link Object#toString()} to display internal information on
     * this object.
     * 
     * @return string representation of this object
     */
    public String toString()
    {
        return super.toString() + "[" + "baseDir=" + baseDir + ",prefix=" + prefix + ",lastId=" + lastUniqueId + "]";
    }

    /**
     * Returns the next <i>uniqueId</i> that should be used in the creation of a
     * new {@link PersistentData} object to be stored. Each invocation of this
     * method results in a new <i>uniqueId</i> being returned.
     * <p>
     * It is recommended that subclasses implement a
     * <code>newPersistentData()</code> factory method that returns a subclass
     * of <code>PersistentData</code>, passing the return value of this method
     * to the constructor. For example:
     * 
     * <pre>
     * public PersistentDataSubclass newData()
     * {
     *     return new PersistentDataSubclass(nextUniqueId());
     * }
     * </pre>
     * 
     * @return the next <i>uniqueId</i>
     */
    protected synchronized long nextUniqueId()
    {
        return ((Long) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new Long(nextUniqueIdPriv());
            }
        })).longValue();
    }

    /**
     * Saves the given data object to a file using Java's built-in
     * serialization. The given <i>data</i> object is stored to a file based on
     * the {@link PersistentData#uniqueId}.
     * 
     * @param data
     *            the data object to save to persistent storage
     */
    protected synchronized void save(final PersistentData data) throws IOException
    {
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws IOException
                {
                    savePriv(data);
                    return null;
                }
            });
        }
        catch (PrivilegedActionException pae)
        {
            Exception e = pae.getException();
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else if (e instanceof IOException)
                throw (IOException) e;
            else
                throw new RuntimeException(pae.getMessage());
        }
    }

    /**
     * Private implementation method for saving a persistent data object. Should
     * be invoked withing a {@link PrivilegedAction}.
     * 
     * @param data
     *            the data object to save to persistent storage
     * @see #save(PersistentData)
     */
    private void savePriv(PersistentData data) throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("savePriv: " + data);
        }

        String name = prefix + Long.toHexString(data.uniqueId);
        File file1 = new File(baseDir, name + ".1");
        File file = new File(baseDir, name);

        // Cleanup if necessary
        if (file1.exists())
            cleanupPriv(file1, file);

        // Write to file.1
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file1)));
        try
        {
            CheckedOutputStream cos = new CheckedOutputStream(dos, new Adler32());
            ObjectOutputStream out = new ObjectOutputStream(cos);

            // Write object
            out.writeObject(data);
            out.flush();

            // Write checksum
            long csum = cos.getChecksum().getValue();
            dos.writeLong(csum);
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(new Exception(
                    "PersistentDataSerializer - savePriv - serialization failed " + e.getMessage(), e));
        } finally
        {
            // Close file.1
            dos.close();
        }

        // Delete file (old)
        if (!file.delete() && file.exists())
        {
            throw new IOException("Could not delete overwrite " + file);
        }

        // Rename file.1 to file
        if (!file1.renameTo(file))
        {
            SystemEventUtil.logRecoverableError(new Exception("PersistentDataSerializer - savePriv - Could not rename "
                    + file1 + " to " + file));
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("savePriv: saved to " + file);
            }
        }
    }

    /**
     * Simply deletes the file from persistent storage. Will attempt to delete
     * both XXXXXXXX and XXXXXXXX.1 from storage, where XXXXXXXX is the object's
     * {@link PersistentData#uniqueId} expressed in hexadecimal form.
     * 
     * @param data
     *            persistent data object to delete
     */
    protected synchronized void delete(final PersistentData data)
    {
        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                String name = prefix + Long.toHexString(data.uniqueId);

                deletePriv(name + ".1");
                deletePriv(name);
                return null;
            }
        });
    }

    /**
     * Deletes the file with the given name. Used to implement
     * {@link #delete(PersistentData)}.
     * 
     * @param name
     *            the file to delete
     */
    private void deletePriv(String name)
    {
        deletePriv(new File(baseDir, name));
    }

    /**
     * Deletes the given file.
     * 
     * @param file
     *            the file to delete
     */
    private void deletePriv(File file)
    {
        if (file.delete())
        {
            if (log.isDebugEnabled())
            {
                log.debug("Deleted " + file);
            }
        }
        else if (file.exists())
        {
            SystemEventUtil.logRecoverableError(new Exception(
                    "PersistentDataSerializer - deletePriv - Could not delete " + file));
        }

    }

    /**
     * Reads all data objects from persistent storage and updates the internal
     * DB of <i>uniqueId<i>s.
     * <p>
     * Persistent data files are searched for in the base directory, named as
     * described in the {@link PersistentDataSerializer class} documentation.
     * 
     * @return vector of <code>PersistentData</code>
     * @see #loadPriv()
     */
    protected Vector load()
    {
        return (Vector) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return loadPriv();
            }
        });
    }

    /**
     * Private method for loading persistent data objectss into a
     * <code>Vector</code>. Should be invoked within {@link PrivilegedAction}.
     * 
     * @see #load
     */
    private Vector loadPriv()
    {
        Vector v = new Vector();

        Filter filter = new Filter();
        String[] list = baseDir.list(filter);

        // Generate list of unique ids
        Hashtable unique = new Hashtable();
        for (int i = 0; i < list.length; ++i)
        {
            if (log.isDebugEnabled())
            {
                log.debug("load: List[" + i + "] " + list[i]);
            }

            try
            {
                Long id = new Long(extractId(list[i]));
                unique.put(id, id);
            }
            catch (Exception e)
            {
                // Unexpected
                SystemEventUtil.logRecoverableError(new Exception(
                        "PersistentDataSerializer - loadPriv - Could not extractId", e));
                continue;
            }
        }

        for (Enumeration e = unique.keys(); e.hasMoreElements();)
        {
            Long id = (Long) e.nextElement();

            PersistentData data = loadPriv(id.longValue());
            if (data != null) v.addElement(data);
        }

        return v;
    }

    /**
     * Attempts to load the single persistent data object indicated by the given
     * id.
     * <p>
     * Used to implement {@link #load}.
     * 
     * @param id
     *            unique identifier for the expected data object
     * @return the loaded persistent data object, or <code>null</code> if it
     *         couldn't be loaded
     * 
     * @see #loadPriv(java.io.File)
     */
    private PersistentData loadPriv(long id)
    {
        String name = prefix + Long.toHexString(id);
        File f;
        PersistentData data = loadPriv(f = new File(baseDir, name + ".1"));
        if (data == null)
        {
            // TODO: For short term fix (as requested by DebraH), do not delete
            // recording metadata
            // This "corrupt" object may just be legacy DVR metadata
            if (deleteCorruptEntries) deletePriv(f); // delete corrupt record,
                                                     // if present

            data = loadPriv(f = new File(baseDir, name));
        }
        if (data == null)
        {
            SystemEventUtil.logRecoverableError(new Exception(
                    "PersistentDataSerializer - loadPriv - Apparent corruption -- expected data object not found " + f));
            // TODO: For short term fix (as requested by DebraH), do not delete
            // recording metadata
            // This "corrupt" object may just be legacy DVR metadata
            if (deleteCorruptEntries) deletePriv(f); // delete corrupt record,
                                                     // if present

        }
        return data;
    }

    /**
     * Attempts to load the persistent data object indicated by the given
     * <code>File</code>.
     * <p>
     * Used to implement {@link #load}.
     * 
     * @param file
     *            file expected to contain the persistent data object
     * @return the loaded persistent data object, or <code>null</code> if it
     *         couldn't be loaded
     * 
     * @see #loadPriv(java.io.File)
     */
    private PersistentData loadPriv(File file)
    {
        if (!file.exists()) return null;

        PersistentData data = null;
        DataInputStream dis = null;

        // Open file for read
        try
        {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        }
        catch (IOException e)
        {
            // Return null if couldn't open file
            return null;
        }

        try
        {
            CheckedInputStream cis = new CheckedInputStream(dis, new Adler32());
            ObjectInputStream in = new ObjectInputStream(cis);

            // Read persistent data object from file
            data = (PersistentData) in.readObject();

            // Read and verify checksum
            long csum = dis.readLong();
            if (csum != cis.getChecksum().getValue())
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Checksum failure, corruption: " + cis.getChecksum().getValue() + " != " + csum);
                }

                // return null on failure
                data = null;
            }
        }
        catch (Throwable e)
        {
            SystemEventUtil.logRecoverableError(new Exception(
                    "PersistentDataSerializer - loadPriv - Could not read PersistentData " + file, e));
            data = null;
        } finally
        {
            try
            {
                dis.close();
            }
            catch (IOException e)
            {
                SystemEventUtil.logRecoverableError(new Exception(
                        "PersistentDataSerializer - loadPriv - Could not close " + file, e));
            }
        }

        return data;
    }

    /**
     * Perform any necessary cleanup before saving a persistent data object.
     * Specifically, we don't expect there to be a secondary (i.e., with a ".1"
     * postfix) persistent data object file in storage.
     * 
     * @param file1
     *            don't expect this file to exist
     * @param file
     */
    private void cleanupPriv(File file1, File file)
    {
        // Does file exist
        if (!file.exists())
        {
            // Apparently failed to rename file1
            if (log.isDebugEnabled())
            {
                log.debug("cleanup needed - " + file1 + " exists, but not " + file);
            }

            // Let's try again
            if (!file1.renameTo(file))
            {
                SystemEventUtil.logRecoverableError(new Exception(
                        "PersistentDataSerializer - cleanup - Could not rename " + file1 + " to " + file));
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("cleanup - Renamed " + file1 + " to " + file);
                }
            }

        }
        else
        {
            // Either was unable to finish writing file1
            // Or was unable to delete file and rename file1
            if (log.isDebugEnabled())
            {
                log.debug("cleanup needed - " + file1 + " and " + file + " exist");
            }

            // Let's just consider file1 invalid and delete it
            if (!file1.delete())
            {
                SystemEventUtil.logRecoverableError(new Exception(
                        "PersistentDataSerializer - cleanup - Could not delete " + file1));
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("cleanup - Deleted " + file1);
                }
        }
    }
    }

    /**
     * Maps filename to unique id.
     * 
     * @param filename
     *            to extract unique id from
     * @return the unique id
     * @throws IllegalArgumentException
     *             if name couldn't not be parsed
     * @throws NumberFormatException
     *             if name couldn't be interpreted as id
     */
    private long extractId(String filename) throws NumberFormatException, IllegalArgumentException
    {
        if (!filename.startsWith(prefix)) throw new IllegalArgumentException(filename);

        String name = filename.substring(prefix.length());
        if (name.endsWith(".1")) name = name.substring(0, name.length() - 2);

        if (name.length() == 0) throw new IllegalArgumentException(filename);

        // Extract the id
        return Long.parseLong(name, 16);
    }

    /**
     * Retrieves the next unique id.
     * 
     * @return the next unique id
     */
    private synchronized long nextUniqueIdPriv()
    {
        File f1, f2;
        do
        {
            lastUniqueId = incrUniqueId(lastUniqueId);

            String name = prefix + Long.toHexString(lastUniqueId);
            f1 = new File(baseDir, name);
            f2 = new File(baseDir, name + ".1");

        }
        while (f1.exists() || f2.exists());

        // We naturally expect that there won't be 2^64 files already!
        // So we shouldn't run out ever

        return lastUniqueId;
    }

    /**
     * Increments the given id to the next possible id. This implementation
     * simply increments using <code>++</code>. The resulting value is still
     * tested against existing values to ensure that there is no overlap (in
     * particular in case of a recycling of values due to overflow).
     * <p>
     * This may be overridden by a subclass if the range of possible ids is
     * smaller than that implied by a 64-bit <code>long</code>.
     * 
     * @param id
     *            the current uniqueId value
     * @return the next possible uniqueId value (e.g., <code>++id</code>)
     */
    protected long incrUniqueId(long id)
    {
        return ++id;
    }

    /**
     * Can be used to determine if the given filenames match those expected.
     * 
     * @author Aaron Kamienski
     * @see java.io.File#list(java.io.FilenameFilter)
     */
    private class Filter implements FilenameFilter
    {
        public long max = Long.MIN_VALUE;

        public long min = Long.MAX_VALUE;

        /**
         * Accept prefix + "xxx" [ ".1"] where "xxx" is a hexadecimal number
         * produced by Long.toHexString().
         * 
         * @return <code>true</code> if filename subscribes to the given
         *         conventions
         */
        public boolean accept(File dir, String file)
        {
            if (!dir.equals(baseDir) || !file.startsWith(prefix))
            {
                return false;
            }

            try
            {
                long id = extractId(file);
                max = Math.max(max, id);
                min = Math.min(min, id);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    /**
     * The filesystem directory where persistent data objects are stored.
     */
    private File baseDir;

    /**
     * If <code>true</code> then corrupt entries will be deleted. This defaults
     * to <code>true</code> but may be adjusted by a subclass as necessary.
     */
    protected boolean deleteCorruptEntries = true;

    /**
     * The prefix used for persistent data object filenames.
     */
    private String prefix;

    /**
     * The last uniqueId used.
     */
    private long lastUniqueId = 0;

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(PersistentDataSerializer.class);
}
