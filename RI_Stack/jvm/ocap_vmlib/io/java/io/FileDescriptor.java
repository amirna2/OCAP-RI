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

package java.io;

import org.cablelabs.impl.io.DefaultFileSys;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.io.FileSysManager;

import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.io.StdOut;
import org.cablelabs.impl.io.WriteableFileSys;

/**
 * Instances of the file descriptor class serve as an opaque handle to the
 * underlying machine-specific structure representing an open file, an open
 * socket, or another source or sink of bytes. The main practical use for a file
 * descriptor is to create a <code>FileInputStream</code> or
 * <code>FileOutputStream</code> to contain it.
 * <p>
 * Applications should not create their own file descriptors.
 * 
 * @see java.io.FileInputStream
 * @see java.io.FileOutputStream
 * @since JDK1.0
 */
public final class FileDescriptor
{
    private int fd;

    /**
     * Read/Write/Append mode
     */
    private static final int READ = 1 << 0;

    private static final int WRITE = 1 << 1;

    private static final int APPEND = 1 << 2;

    private int accessMode = 0;

    /**
     * If we were copy-constructed with another file descriptor we recognize
     * that FD as our "parent". If we are told to "close" and we have a parent,
     * we simply tell our parent to close. If we have no parent, we decrement
     * our reference count and only truly close the underlying file if refCount
     * is zero.
     */
    private FileDescriptor parentFD = null;

    private int refCount = 0;

    private String path = null;

    /**
     * Constructs an (invalid) FileDescriptor object.
     */
    public FileDescriptor()
    {
        fd = -1;
    }

    private FileDescriptor(int fd)
    {
        this.fd = fd;
    }

    /**
     * Not supported in OCAP
     * 
     * Force all system buffers to synchronize with the underlying device. This
     * method returns after all modified data and attributes of this
     * FileDescriptor have been written to the relevant device(s). In
     * particular, if this FileDescriptor refers to a physical storage medium,
     * such as a file in a file system, sync will not return until all in-memory
     * modified copies of buffers associated with this FileDesecriptor have been
     * written to the physical medium.
     * 
     * sync is meant to be used by code that requires physical storage (such as
     * a file) to be in a known state For example, a class that provided a
     * simple transaction facility might use sync to ensure that all changes to
     * a file caused by a given transaction were recorded on a storage medium.
     * 
     * sync only affects buffers downstream of this FileDescriptor. If any
     * in-memory buffering is being done by the application (for example, by a
     * BufferedOutputStream object), those buffers must be flushed into the
     * FileDescriptor (for example, by invoking OutputStream.flush) before that
     * data will be affected by sync.
     * 
     * @exception SyncFailedException
     *                Thrown when the buffers cannot be flushed, or because the
     *                system cannot guarantee that all the buffers have been
     *                synchronized with physical media.
     * @since JDK1.1
     */
    public void sync() throws SyncFailedException
    {
        // Do nothing
    }

    /**
     * A handle to the standard input stream. Usually, this file descriptor is
     * not used directly, but rather via the input stream known as
     * <code>System.in</code>.
     * 
     * @see java.lang.System#in
     */
    public static final FileDescriptor in = new FileDescriptor(0);

    /**
     * A handle to the standard output stream. Usually, this file descriptor is
     * not used directly, but rather via the output stream known as
     * <code>System.out</code>.
     * 
     * @see java.lang.System#out
     */
    public static final FileDescriptor out = new FileDescriptor(1);

    /**
     * A handle to the standard error stream. Usually, this file descriptor is
     * not used directly, but rather via the output stream known as
     * <code>System.err</code>.
     * 
     * @see java.lang.System#err
     */
    public static final FileDescriptor err = new FileDescriptor(2);

    /**
     * Reference to an OpenFile object that is used to get file data
     */
    private OpenFile of = null;

    /**
     * WriteableFileSys reference in the case this file descriptor is associated
     * with a FileOutputStream or writeable RandomAccessFile
     */
    private WriteableFileSys wfs = null;

    /**
     * Used for handling stdout and stderr
     */
    private StdOut stdOut = null;

    /**
     * Tests if this file descriptor object is valid.
     * 
     * @return <code>true</code> if the file descriptor object represents a
     *         valid, open file, socket, or other active I/O connection;
     *         <code>false</code> otherwise.
     */
    public boolean valid()
    {
        return (fd != -1) || (of != null);
    }

    /*
     * Many of the SunTCK tests check to ensure that the FileDescriptor is
     * invalid after closing an Input/OutputStream. So, we need this method to
     * formally close the FD
     */
    void close() throws IOException
    {
        if (of != null)
        {
            // If we have a parent just tell the parent to close, otherwise
            // decrement our ref count and potentially close the open file
            if (parentFD != null)
            {
                parentFD.close();
                parentFD = null;
                of = null;
            }
            else if (--refCount == 0)
            {
                of.close();
                of = null;
            }
        }
        fd = -1;
    }

    /**
     * Package private constructors meant to be called by all public Java
     * classes that can read/write files. These include java.io.FileInputStream,
     * java.io.FileOutputStream, java.io.RandomAccessFile, java.util.zip.ZipFile
     */

    /**
     * Create a FileDescriptor from the given file path and open the file for
     * reading/writing as indicated by the <code>mode</code> parameter.
     * 
     * @param path
     *            the path name of the file
     * @param mode
     *            specifies the read/write/append mode to use when opening. Can
     *            be one of ["r", "rw", "w", "wa"] where 'r' is read, 'w' is
     *            write, and 'a' is append
     */
    FileDescriptor(String filePath, String mode) throws IOException
    {
        path = FileSystem.getFileSystem().canonicalize(filePath);
        if (mode.indexOf('r') != -1) accessMode |= READ;
        if (mode.indexOf('w') != -1) accessMode |= WRITE;
        if (mode.indexOf('a') != -1) accessMode |= APPEND;

        // If this is a writeable file, check for the need to create its
        // metadata
        // file
        if ((accessMode & WRITE) != 0)
        {
            // If we are opening for write, the we only use the default (MPE)
            // filesys
            final DefaultFileSys fileSys = (DefaultFileSys) FileSysManager.getDefaultFileSys();
            boolean existsBefore = fileSys.exists(path);

            of = fileSys.open(path, mode);

            if (!existsBefore)
            {
                try
                {
                    FileSysManager.createMetadata(path, true);
                }
                catch (IOException e)
                {
                    // If we fail to create metadata, close the file and delete
                    // it
                    // and throw an exception
                    try
                    {
                        of.close();
                    }
                    catch (IOException f)
                    {
                    }
                    fileSys.delete(path);
                    of = null;
                    throw new IOException("Could not create file metadata");
                }
            }

            wfs = FileSysManager.getWriteableFileSys(path);
        }
        else
        {
            of = FileSysManager.getFileSys(path).open(path);
        }

        refCount = 1;
    }

    /**
     * Create a FileDescriptor from the given file and open the file for
     * reading/writing as indicated by the <code>mode</code> parameter.
     * 
     * @param path
     *            the file
     * @param mode
     *            specifies the read/write/append mode to use when opening. Can
     *            be one of "r", "rw", "w", "wa", "rwa", where 'r' is read, 'w'
     *            is write, and 'a' is append
     */
    FileDescriptor(File f, String mode) throws IOException
    {
        this(f.getPath(), mode);
    }

    /**
     * Create a FileDescriptor from the given file descriptor
     * 
     * @param path
     *            the file descriptor
     */
    FileDescriptor(FileDescriptor fileDesc)
    {
        // Handle stdout and stderr
        if (fileDesc == out || fileDesc == err)
        {
            stdOut = new StdOut();
        }
        else if (fileDesc != in) // use of stdin not allow in OCAP
        {
            parentFD = fileDesc;
            parentFD.refCount++;

            path = fileDesc.path;
            of = fileDesc.of;
            wfs = fileDesc.wfs;
            accessMode = fileDesc.accessMode;
        }
    }

    String getPath()
    {
        return path;
    }

    /**
     * File I/O operations
     */

    /**
     * Reads a single byte from the file
     * 
     * @return the byte read or <code>-1</code> if end-of-file is reached
     */
    int read() throws IOException
    {
        if (of == null) throw new IOException("Invalid file descriptor");

        return of.read();
    }

    /**
     * Reads bytes from the file starting at the current file pointer
     * 
     * @param b
     *            the location where the read bytes should be stored
     * @param off
     *            the offset in the array at which read bytes should be stored
     * @param len
     *            the number of bytes to read
     * @return the number of bytes read or <code>-1</code> if end-of-file is
     *         reached
     * @throws IOException
     *             if an error occurred while reading
     */
    int readBytes(byte b[], int off, int len) throws IOException
    {
        if (of == null) throw new IOException("Invalid file descriptor");

        return of.read(b, off, len);
    }

    /**
     * Attempts to skip over <code>n</code> bytes of input, discarding the
     * skipped bytes.
     * 
     * @param n
     *            the number of bytes to be skipped.
     * @return the number of bytes actually skipped. May return <code>0</code>
     *         or a negative value if no bytes are skipped
     * @throws IOException
     *             if an I/O error occurs
     */
    long skip(long n) throws IOException
    {
        if (of == null) throw new IOException("Invalid file descriptor");

        return of.skip(n);
    }

    /**
     * Returns the number of bytes available for reading from this file
     * 
     * @return the number of available bytes
     * @throws IOException
     *             if an I/O error occurs
     */
    int available() throws IOException
    {
        if (of == null) throw new IOException("Invalid file descriptor");

        return of.available();
    }

    /**
     * Returns the file pointer position
     * 
     * @return the file pointer position
     * @throws IOException
     *             if an I/O error occurs
     */
    long getFilePointer() throws IOException
    {
        if (of == null) throw new IOException("Invalid file descriptor");

        return of.getFilePointer();
    }

    /**
     * Attempts to move the file pointer to the given offset from the beginning
     * of the file. The offset may be set beyond the end of the file. Setting
     * the offset beyond the end of the file does not change the file length.
     * The file length will change only by writing after the offset has been set
     * beyond the end of the file.
     * 
     * @param pos
     *            the offset position, measured in bytes from the beginning of
     *            the file, at which to set the file poitner
     * @throws IOException
     *             if <code>pos</code> is negative or if an I/O error occurs
     */
    void seek(long pos) throws IOException
    {
        if (of == null) throw new IOException("Invalid file descriptor");

        if (pos < 0) throw new IOException("pos can not be negative");

        of.seek(pos);
    }

    /**
     * Returns the length of this file
     * 
     * @return the length of the file in bytes
     * @throws IOException
     *             if an I/O error occurs
     */
    long length() throws IOException
    {
        if (of == null) throw new IOException("Invalid file descriptor");

        return of.length();
    }

    private byte[] arr = new byte[1];

    /**
     * Writes the given byte to the file at the current file pointer
     * 
     * @param b
     *            the byte to write
     * @throws IOException
     *             if an I/O error occurs
     */
    void write(int b) throws IOException
    {
        arr[0] = (byte) b;

        // handle stdout
        if (stdOut != null)
        {
            stdOut.write(arr, 0, 1);
        }
        else
        {
            if (wfs == null) throw new IOException("Read-only file system");

            if (of == null) throw new IOException("Invalid file descriptor");

            wfs.write(of.getNativeFileHandle(), arr, 0, 1);
        }
    }

    /**
     * Writes the given bytes to the file at the current file pointer.
     * 
     * @param b
     *            the bytes to write
     * @param off
     *            the offset in the byte array from which to retrieve bytes that
     *            will be written to the file
     * @param len
     *            the number of bytes to write
     * @throws IOException
     *             if an I/O error occurs
     */
    void writeBytes(byte b[], int off, int len) throws IOException
    {
        // handle stdout
        if (stdOut != null)
        {
            stdOut.write(b, off, len);
        }
        else
        {
            if (wfs == null) throw new IOException("Read-only file system");

            if (of == null) throw new IOException("Invalid file descriptor");

            wfs.write(of.getNativeFileHandle(), b, off, len);
        }
    }

    /**
     * Sets the length of the file. May truncate file contents.
     * 
     * @param newLength
     *            the new length of the file in bytes
     * @throws IOException
     *             if an I/O error occurs
     */
    void setLength(long newLength) throws IOException
    {
        if (wfs == null) throw new IOException("Read-only file system");

        if (of == null) throw new IOException("Invalid file descriptor");

        wfs.setLength(of.getNativeFileHandle(), newLength);
    }
}
