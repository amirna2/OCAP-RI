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

package org.cablelabs.impl.recording;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;

/**
 * Instances of <code>AppDataContainer</code> are used to hold application data
 * added to a {@link RecordingRequest#addAppData(String, java.io.Serializable)
 * recording}.
 * <p>
 * The <code>AppDataContainer</code> takes care of the serialization of the
 * application data so that the serialization of the application data is
 * separate from the serialization of the encapsulating
 * {@link RecordingInfoNode recording meta-data}. This is necessary because it
 * is necessary to serialize the application data as soon as possible to avoid
 * potential memory leaks and to de-serialize the application data as late as
 * possible to ensure that the data can be deserialized.
 *
 * @author Aaron Kamienski
 */
public class AppDataContainer implements Serializable
{
    /**
     * Creates an instance of AppDataContainer. The given <i>object</i> is
     * serialized immediately. As such, any calls to {@link #getObject()} will
     * return a new copy of the original object (and not the original object
     * itself).
     *
     * @param object
     *            the application data object to be serialized
     * @throws IOException
     *             if there were problems serializing the data object
     */
    public AppDataContainer(Serializable object) throws IOException
    {
        if (object == null) throw new NullPointerException("app data object cannot be null");

        data = toByteArray(object);
        string = object.toString();
    }

    /**
     * Creates an instance of AppDataContainer from another AppDataContainer.
     *
     * The object is initialized with the serialized form copied directly from
     * the <i>source</i> AppDataContainer. Any calls to {@link #getObject()}
     * will return a new copy of the object originally serialed in the
     * <i>source</i> AppDataContainer.
     *
     * @param source
     *            The AppDataContainer to be copied
     */
    public AppDataContainer(final AppDataContainer source)
    {
        if (source == null) throw new NullPointerException("source AppDataContainer cannot be null");

        //
        // Copy the data and string from the source
        //
        if (source.data != null)
        {
            data = new byte[source.data.length];
            System.arraycopy(source.data, 0, /* From */
            this.data, 0, /* To */
            source.data.length /* count */);
        }
        string = source.string;
    }

    /**
     * Returns a copy of the object provided in the
     * {@link #AppDataContainer(Serializable) constructor}. This is a new object
     * because the object has been serialized and de-serialized in the interim.
     *
     * @return a copy of the object provided during construction
     */
    public Serializable getObject() throws IOException, ClassNotFoundException
    {
        return getObject(null);
    }

    /**
     * Returns a copy of the object provided in the
     * {@link #AppDataContainer(Serializable) constructor}. This is a new object
     * because the object has been serialized and de-serialized in the interim.
     * <p>
     * In general the provided <code>ClassLoader</code> should be the
     * application class loader (see
     * {@link ApplicationManager#getAppClassLoader()}). This is basically
     * parameterized for testing.
     *
     * @param cl
     *            the <code>ClassLoader</code> to use in the reconstruction of
     *            the object; if <code>null</code> then default behavior is
     *            assumed
     * @return a copy of the object provided during construction
     */
    public Serializable getObject(ClassLoader cl) throws IOException, ClassNotFoundException
    {
        Serializable object = fromByteArray(data, cl);
        if (string == null) string = object.toString();
        return object;
    }

    /**
     * Returns the length of the serialized version of the object provided in
     * the {@link #AppDataContainer(Serializable) constructor}.
     *
     * @return length of the serialized version of the object provided during
     *         construction
     */
    public int getSize()
    {
        return data.length;
    }

    /**
     * Converts the given <code>Serializable</code> object to a
     * <code>byte[]</code> and returns it.
     *
     * @param object
     *            the object to serialize
     * @return the serialized <code>byte[]</code> for the given object
     */
    private static byte[] toByteArray(Serializable object) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        try
        {
            oos.writeObject(object);
            oos.flush();
            byte[] array = bos.toByteArray();
            return array;
        }
        finally
        {
            oos.close();
        }
    }

    /**
     * Converts the given serialized <code>byte[]</code> back to an instance of
     * the <code>Serializable</code> object provided during
     * {@link #AppDataContainer(Serializable) construction}.
     * <p>
     * In general the provided <code>ClassLoader</code> should be the
     * application class loader. This is basically parameterized for testing.
     *
     * @param data
     *            the <code>byte[]</code> to de-serialize
     * @param cl
     *            the <code>ClassLoader</code> to use in the reconstruction of
     *            the object; if <code>null</code> then default behavior is
     *            assumed
     * @return a de-serialized instance of the serialized
     *         <code>Serializable</code>
     */
    private static Serializable fromByteArray(byte[] data, final ClassLoader cl) throws IOException,
            ClassNotFoundException
    {
        // Create a custom ObjectInputStream that takes care of resolving
        // application classes against the application classloader.
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))
        {
            protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException,
                    StreamCorruptedException
            {
                return (cl == null) ? super.resolveClass(desc) : Class.forName(desc.getName(), false, cl);
            }
        };

        // Read and return the object
        try
        {
            return (Serializable) ois.readObject();
        }
        finally
        {
            ois.close();
        }
    }

    /**
     * Overrides {@link Object#toString()} to include information about the
     * original application data.
     */
    public String toString()
    {
        if (string != null)
            return super.toString() + "[" + this.string + "]";
        else
            return super.toString();
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof AppDataContainer)) return false;
        return java.util.Arrays.equals(data, ((AppDataContainer) obj).data);
    }

    public int hashCode()
    {
        Checksum csum = new Adler32();
        csum.update(data,0,data.length);
        return (int)csum.getValue();
    }

    /**
     * The serialized version of the application data object provided in the
     * {@link #AppDataContainer(Serializable) constructor}.
     */
    private byte[] data;

    /**
     * The previously recorded output of {@link Object#toString} for the object
     * provided upon construction. This is remembered purely for debugging
     * purposes and is not currently serialized.
     */
    private transient String string;

    /**
     * Serial Version UID is fixed, allowing the contents of this class to
     * change and built-in serialization to continue to work (assuming that
     * certain rules are followed).
     */
    private static final long serialVersionUID = 8709013095457043641L;
}
