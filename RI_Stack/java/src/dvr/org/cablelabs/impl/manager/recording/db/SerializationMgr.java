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

package org.cablelabs.impl.manager.recording.db;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.persistent.PersistentData;
import org.cablelabs.impl.persistent.PersistentDataSerializer;
import org.cablelabs.impl.recording.RecordingInfo;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.cablelabs.impl.recording.RecordingInfoNode;
import org.cablelabs.impl.recording.RecordingInfoTree;
import org.cablelabs.impl.util.MPEEnv;

/**
 * Implementation of <code>RecordingDBManager</code> based upon Java
 * serialization.
 * <p>
 * The local file system is used as the database, with individual records
 * occupying individual files in the file system. File records are named by
 * their {@link RecordingInfo#uniqueId} so that it is easy to locate the file
 * given the <code>RecordingImpl</code>.
 * <p>
 * In order to provide for more robust updates, we never overwrite a file
 * directly. Instead we write to a secondary file, then delete the original, and
 * finally rename the secondary file. When reading files in we look for the
 * secondary file first. If no secondary file is found or it is corrupt, we fall
 * back to the original filename.
 * <p>
 * Files will be names using a hexadecimal representation of the uniqueId. E.g.,
 * <code>"15"</code> or <code>"15.1"</code> for <code>uniqueId</code> of 21.
 * <p>
 * Some parameters are modifiable via system properties:
 * <table border>
 * <tr>
 * <th>Variable</th>
 * <th>Property(ies)</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>baseDir</td>
 * <td>org.cablelabs.ocap.dvr.serial.dir, OCAP.persistent.dvr + "/recdbser", or
 * "/syscwd/recdbser"</td>
 * <td>The location where files are stored</td>
 * </tr>
 * <tr>
 * <td>prefix</td>
 * <td>org.cablelabs.ocap.dvr.serial.prefix</td>
 * <td>Filename prefix to use, defaults to ""</td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 */
public class SerializationMgr extends PersistentDataSerializer implements RecordingDBManager
{
    private static final Logger log = Logger.getLogger(SerializationMgr.class);

    /**
     * Creates and returns an instance of <code>SerializationMgr</code>.
     * Satisfies a requirement of a <code>Manager</code> implementation.
     * 
     * @return an instance of this <code>Manager</code>.
     */
    public static Manager getInstance()
    {
        return new SerializationMgr();
    }

    /**
     * Construct only callable from self and subclasses.
     */
    protected SerializationMgr()
    {
        this(defineBaseDir(), MPEEnv.getEnv(PREFIX_PROP, ""), !"true".equals(MPEEnv.getEnv(ORPHAN_PROP)));
    }

    /**
     * This constructor is exposed for testing purposes only.
     */
    SerializationMgr(File baseDir, String prefix, boolean deleteCorruptEntries)
    {
        super(baseDir, prefix, deleteCorruptEntries);
    }

    /**
     * Returns the base directory that will be used for storing files. This
     * location is currently determined by examining the following system
     * property:
     * <ul>
     * <li> <code>OCAP.persistent.dvr</code> specifies path within which
     * <code>recdbser</code> sub-dir is created
     * </ul>
     * 
     * @return the base directory that will be used for storing files.
     */
    private static File defineBaseDir()
    {
        String value;

        // Figure the base directory
        value = MPEEnv.getEnv(BASEDIR_PROP, DEFAULT_DIR);
        return new File(value, DEFAULT_SUBDIR);
    }

    /**
     * Overrides
     * {@link org.cablelabs.impl.persistent.PersistentDataSerializer#initLastUniqueId(java.io.File)}
     * , returning the last unique ID as found in persistent storage.
     */
    protected long initLastUniqueId(File base)
    {
        long baseID;

        // Read value stored in persistent storage
        try
        {
            baseID = loadLastUniqueId(base);
        }
        // If not found or currupt, use default method (calculate based upon
        // existing ids)
        catch (IOException e)
        {
            baseID = super.initLastUniqueId(base);
        }

        return baseID;
    }

    /**
     * Overrides
     * {@link org.cablelabs.impl.persistent.PersistentDataSerializer#nextUniqueId()}
     * so that the next unique ID is stored to disk after it is acquired by
     * calling <code>super.nextUniqueId()</code>.
     * 
     * @return the next unique ID
     */
    protected synchronized long nextUniqueId()
    {
        long id = super.nextUniqueId();

        try
        {
            saveLastUniqueId(uniqueIdFile, id);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Could not save lastUniqueId", e);
            }
        }

        return id;
    }

    /**
     * Overrides
     * {@link PersistentDataSerializer#incrUniqueId(long)}
     * , limiting the range of <i>id</i>s to 32 bits.
     * 
     * @return next possible unique ID
     */
    protected long incrUniqueId(long id)
    {
        return (int) id + 1;
    }

    // Description copied from Manager
    public void destroy()
    {
        // Doesn't need to do anything
    }

    // Description copied from RecordingDBManager
    public RecordingInfo2 newRecord()
    {
        return new SegmentedLeaf(nextUniqueId());
    }

    public RecordingInfo2 newRecord(RecordingInfo ri, long uniqueID)
    {
        return new SegmentedLeaf(uniqueID, ri);
    }

    // Description copied from RecordingDBManager
    public RecordingInfoTree newRecordTree()
    {
        return new Tree(nextUniqueId());
    }

    /**
     * Saves the given record to a file using Java's built-in serialization.
     */
    public synchronized void saveRecord(final RecordingInfoNode info) throws IOException
    {
        save(info);
    }

    /**
     * Currently implemented using {@link #saveRecord(RecordingInfoNode)}.
     */
    public synchronized void saveRecord(final RecordingInfoNode info, int updateFlag) throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("saveRecord: " + Integer.toHexString(updateFlag) + " " + info);
        }

        saveRecord(info);
    }

    // TODO: Add this if we need it. Not seen as necessary at this point.
    // Description coped from RecordingDBManager
    /*
     * public synchronized void saveRecord(RecordingInfoTree tree, boolean
     * recurse) throws IOException { saveRecord(tree); if ( recurse &&
     * tree.children != null ) { for (Enumeration e = tree.children.elements();
     * e.hasMoreElements();) { RecordingInfoNode node =
     * (RecordingInfoNode)e.nextElement(); if ( node instanceof
     * RecordingInfoTree ) saveRecord(tree, recurse); else saveRecord(node); }
     * 
     * }
     * 
     * } }
     */

    /**
     * Simply deletes the file from persistent storage. Will attempt to delete
     * both XXXXXXXX and XXXXXXXX.1 from storage, where XXXXXXXX is the record's
     * {@link RecordingInfo#uniqueId} expressed in hexadecimal form.
     * 
     * @param info
     *            record to delete
     */
    public synchronized void deleteRecord(final RecordingInfoNode info)
    {
        delete(info);
    }

    // Description copied from RecordingDBManager
    public synchronized void deleteRecord(RecordingInfoTree tree, boolean recurse)
    {
        delete(tree);
        if (recurse && tree.children != null)
        {
            for (Enumeration e = tree.children.elements(); e.hasMoreElements();)
            {
                RecordingInfoNode node = (RecordingInfoNode) e.nextElement();
                if (node instanceof RecordingInfoTree)
                    deleteRecord((RecordingInfoTree) node, recurse);
                else
                    delete(node);
            }
        }
    }

    /**
     * Reads all records from persistent storage and updates the internal DB of
     * <i>uniqueId<i>s.
     * <p>
     * Records are searched for in the base directory, named as described in the
     * {@link SerializationMgr class} documentation.
     * 
     * @return vector of <code>RecordingInfo</code>
     * @see #load()
     */
    public Vector loadRecords()
    {
        // TODO: I'm sure this hiearchy reconstruction could be simplified...

        Vector loaded = load();
        boolean trees = false;

        // Ensure we only deal with types that we can handle
        for (int i = 0; i < loaded.size();)
        {
            PersistentData data = (PersistentData) loaded.elementAt(i);

            if (!(data instanceof SavedByMe))
                loaded.removeElementAt(i);
            else
            {
                ++i;
                trees = trees || (data instanceof Tree);
            }
        }

        // If no trees, just return the loaded array
        if (!trees)
            return loaded;
        else
        {
            // Save off all entries... in uniqueId->node map
            Hashtable map = new Hashtable();
            for (Enumeration e = loaded.elements(); e.hasMoreElements();)
            {
                RecordingInfoNode node = (RecordingInfoNode) e.nextElement();

                map.put(new Long(node.uniqueId), node);
            }

            // Iterate over all entries and build up tree
            for (Enumeration e = loaded.elements(); e.hasMoreElements();)
            {
                RecordingInfoNode node = (RecordingInfoNode) e.nextElement();
                if ((node instanceof Tree))
                {
                    if (map.get(new Long(node.uniqueId)) != null) updateChildren(map, (Tree) node);
                    ((Tree) node).childIds = null;
                }
            }

            // What is left in the hashtable are the roots
            Vector roots = new Vector();
            for (Enumeration e = map.elements(); e.hasMoreElements();)
            {
                roots.addElement(e.nextElement());
            }

            return roots;
        }
    }

    /**
     * Updates the given tree's {@link Tree#children} list.
     * 
     * @param map
     *            mapping of id to node; only contains entries that don't
     *            already have a parent
     * @param tree
     *            the tree to update
     */
    private void updateChildren(Hashtable map, Tree tree)
    {
        if (tree.children == null) tree.children = new Vector();
        if (tree.childIds == null) return;
        for (int i = 0; i < tree.childIds.length; ++i)
        {
            RecordingInfoNode node = (RecordingInfoNode) map.remove(new Long(tree.childIds[i]));
            // Does that child exist?
            if (node != null)
            {
                tree.children.addElement(node);
                if (node instanceof Tree)
                {
                    updateChildren(map, (Tree) node);
                }
            }
        }
    }

    /**
     * Loads the last unique ID value from persistent storage.
     * 
     * @return the id returned by the most recent invocation of
     *         {@link #nextUniqueId()}
     * 
     * @throws IOException
     *             if the last unique id could not be loaded
     */
    private synchronized long loadLastUniqueId(File base) throws IOException
    {
        uniqueIdFile = new File(base, UNIQUEID);

        DataInputStream dis = new DataInputStream(new FileInputStream(uniqueIdFile));

        long id = dis.readLong();
        long csum = dis.readLong();

        dis.close();

        if (id != ~csum) throw new IOException("Data corruption exception");

        return id;
    }

    /**
     * Stores the last unique ID value to persistent storage.
     * 
     * @param id
     *            the id to store for later retrieval by
     *            {@link #loadLastUniqueId(File)}
     * @throws IOException
     */
    private synchronized void saveLastUniqueId(File base, final long id) throws IOException
    {
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws IOException
                {
                    DataOutputStream dos = new DataOutputStream(new FileOutputStream(uniqueIdFile));
                    dos.writeLong(id);
                    dos.writeLong(~id); // checksum
                    dos.close();
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
     * Simple tagging interface.
     * 
     * @author Aaron Kamienski
     */
    private static interface SavedByMe
    {
        // empty
    }

    /**
     * Extension that implements the tagging interface.
     * 
     * @author Aaron Kamienski
     */
    static class Leaf extends RecordingInfo implements Serializable, SavedByMe
    {
        Leaf(long uniqueId)
        {
            super(uniqueId);
        }

        Leaf(long uniqueId, final RecordingInfo rinfo)
        {
            super(uniqueId, rinfo);
        }
    }

    /**
     * Extension that implements the tagging interface.
     * 
     * @author Craig Pratt
     */
    static class SegmentedLeaf extends RecordingInfo2 implements Serializable, SavedByMe
    {
        SegmentedLeaf(long uniqueId)
        {
            super(uniqueId);
        }

        SegmentedLeaf(long uniqueId, final RecordingInfo rinfo)
        {
            super(uniqueId, rinfo);
        }
    }

    /**
     * Extension that saves/restores the child uniqueIds.
     * 
     * @author Aaron Kamienski
     */
    static class Tree extends RecordingInfoTree implements Serializable, SavedByMe
    {
        /**
         * After reading, this contains the uniqueIds of the previously stored
         * children.
         */
        transient long[] childIds;

        Tree(long uniqueId)
        {
            super(uniqueId);

            // Always have a vector in-place
            children = new Vector();
        }

        /**
         * Provide for serialization of non-serializable objects contained
         * within. This takes care of writing <i>transient</i> fields to the
         * given <code>ObjectOutputStream</code>.
         * <p>
         * This method is implemented for serialization only, not for general
         * usage.
         * 
         * @param out
         * 
         * @see #childIds
         */
        private void writeObject(ObjectOutputStream out) throws IOException
        {
            // Write default fields
            out.defaultWriteObject();

            // Write out non-serializable/transient fields

            int n = (children == null) ? -1 : children.size();
            out.writeInt(n);
            if (n > 0)
            {
                for (int i = 0; i < n; ++i)
                {
                    out.writeLong(((RecordingInfoNode) children.elementAt(i)).uniqueId);
                }
            }
        }

        /**
         * Provide for de-serialization of non-serializable objects contained
         * within. This takes care of reading <i>transient</i> fields from the
         * given <code>ObjectInputStream</code>.
         * <p>
         * This method is implemented for serialization only, not for general
         * usage.
         * 
         * @param in
         * 
         * @see #childIds
         */
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
        {
            // Read default fields
            in.defaultReadObject();

            // Read in non-seriallizable/transient fields
            int n = in.readInt();
            if (n >= 0)
            {
                childIds = new long[n];
                for (int i = 0; i < n; ++i)
                    childIds[i] = in.readLong();
            }
        }
    }

    /**
     * The uniqueIdFile as initialized by the most recent call to
     * {@link #loadLastUniqueId}. This is the <code>File</code> that was read by
     * {@link #loadLastUniqueId} (if it existed) and then written to by
     * subsequent calls to {@link #saveLastUniqueId}.
     */
    private File uniqueIdFile;

    static
    {
        // For the 0.9 release only -- this code moves the DVR metadata from
        // its old location (/itfs/recdbser <SA>, /syscwd/recdbser <Sim>, or
        // /hda/recdbser <Mot>) to the new location
        File newDir = defineBaseDir();
        File oldDir = null;
        boolean result = true;

        File motDir = new File("/hda/recdbser");
        File simDir = new File("/syscwd/recdbser");
        File saDir = new File("/itfs/recdbser");
        if (motDir.exists() && motDir.isDirectory())
            oldDir = motDir;
        else if (simDir.exists() && simDir.isDirectory())
            oldDir = simDir;
        else if (saDir.exists() && saDir.isDirectory()) oldDir = saDir;

        if (oldDir != null)
        {
            newDir.mkdirs();
            File dirEntries[] = oldDir.listFiles();

            for (int i = 0; i < dirEntries.length; ++i)
            {
                File newFile = new File(newDir, dirEntries[i].getName());
                if (!(result = dirEntries[i].renameTo(newFile))) break;
            }
        }

        if (!result)
        {
            System.out.println("********************* ERROR!  FAILED TO RENAME OLD DVR SERIALIZATION DIRECTORY TO NEW 0.9 RELEASE LOCATIONS *******************");
        }
    }

    protected static final String DEFAULT_DIR = "/syscwd"; // not a very good
                                                           // default...

    protected static final String DEFAULT_SUBDIR = "recdbser";

    protected static final String UNIQUEID = "lastid.dat";

    protected static final String BASEDIR_PROP = "OCAP.persistent.dvr";

    protected static final String PREFIX_PROP = "OCAP.dvr.serial.prefix";

    protected static final String ORPHAN_PROP = "OCAP.dvr.recording.leaveOrphans";
}
