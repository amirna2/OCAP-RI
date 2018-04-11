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

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests AppDataContainer.
 * 
 * @author Aaron Kamienski
 */
public class AppDataContainerTest extends TestCase
{

    public void testConstructor_null() throws Exception
    {
        try
        {
            new AppDataContainer(null);
            fail("Expected NullPointerException with null parameter");
        }
        catch (NullPointerException e)
        {
            // ignored
        }
    }

    public void testGetObject() throws Exception
    {
        Serializable object = new Rectangle(27, 34, 99, 1);
        AppDataContainer adc = new AppDataContainer(object);

        Serializable obj2 = adc.getObject();
        assertNotNull("Expected getObject() to return non-null (1)", obj2);
        assertNotSame("Expected getObject() to return different object (1)", object, obj2);
        assertEquals("Expected getObject() to return equivalent object (1)", object, obj2);

        object = obj2;
        obj2 = adc.getObject();
        assertNotNull("Expected getObject() to return non-null (2)", obj2);
        assertNotSame("Expected getObject() to return different object (2)", object, obj2);
        assertEquals("Expected getObject() to return equivalent object (2)", object, obj2);

        adc = new AppDataContainer(object);
        obj2 = adc.getObject();
        assertNotNull("Expected getObject() to return non-null (3)", obj2);
        assertNotSame("Expected getObject() to return different object (3)", object, obj2);
        assertEquals("Expected getObject() to return equivalent object (3)", object, obj2);

        adc = new AppDataContainer(obj2);
        obj2 = adc.getObject();
        assertNotNull("Expected getObject() to return non-null (4)", obj2);
        assertNotSame("Expected getObject() to return different object (4)", object, obj2);
        assertEquals("Expected getObject() to return equivalent object (4)", object, obj2);
    }

    public void testGetObject_classloader() throws Exception
    {
        TestClassLoader cl = new TestClassLoader();
        String dataClassName = DummyClass.class.getName();
        cl.addClass(dataClassName);
        Class dataClass = Class.forName(dataClassName, false, cl);
        Serializable dataObject = (Serializable) dataClass.newInstance();
        AppDataContainer adc = new AppDataContainer(dataObject);

        Serializable obj2 = adc.getObject(cl);
        assertNotNull("Expected getObject() to return non-null (1)", obj2);
        assertNotSame("Expected getObject() to return different object (1)", dataObject, obj2);
        assertEquals("Expected getObject() to return equivalent object (1)", dataObject, obj2);

        dataObject = obj2;
        obj2 = adc.getObject(cl);
        assertNotNull("Expected getObject() to return non-null (2)", obj2);
        assertNotSame("Expected getObject() to return different object (2)", dataObject, obj2);
        assertEquals("Expected getObject() to return equivalent object (2)", dataObject, obj2);

        adc = new AppDataContainer(dataObject);
        obj2 = adc.getObject(cl);
        assertNotNull("Expected getObject() to return non-null (3)", obj2);
        assertNotSame("Expected getObject() to return different object (3)", dataObject, obj2);
        assertEquals("Expected getObject() to return equivalent object (3)", dataObject, obj2);

        adc = new AppDataContainer(obj2);
        obj2 = adc.getObject(cl);
        assertNotNull("Expected getObject() to return non-null (4)", obj2);
        assertNotSame("Expected getObject() to return different object (4)", dataObject, obj2);
        assertEquals("Expected getObject() to return equivalent object (4)", dataObject, obj2);
    }

    public void testGetSize() throws Exception
    {
        Serializable object = new Integer(0x76543210);
        AppDataContainer adc = new AppDataContainer(object);

        int length = adc.getSize();
        assertTrue("Expected length to be at least 4 bytes", length >= 4);
        assertEquals("Expected same length to be returned repeatedly", length, adc.getSize());
    }

    public void testToString() throws Exception
    {
        // Just ensure it doesn't crash for now

        AppDataContainer adc = new AppDataContainer(new byte[0]);
        assertNotNull("Expected toString() to return a valid string", adc.toString());
    }

    public void testSerialization() throws Exception
    {
        Serializable[] objects = { "Hello, world", new Integer(3), new Hashtable(), new byte[] { 9 },
                new Object[] { new Integer(3), new Short((short) 4) }, };

        for (int i = 0; i < objects.length; ++i)
        {
            AppDataContainer adc = new AppDataContainer(objects[i]);

            // Write ADC
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(adc);
            oos.flush();

            // Read back ADC
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            AppDataContainer adc2 = (AppDataContainer) ois.readObject();

            // Verify that contains the same data
            Serializable object = adc2.getObject();

            // Note should only expect the following if overrides equals()
            // We handle byte[] and Object[] specially.
            if (objects[i] instanceof byte[])
                assertTrue("Expect these two array to be equivalent", Arrays.equals((byte[]) objects[i],
                        (byte[]) object));
            else if (objects[i] instanceof Object[])
                assertTrue("Expect these two array to be equivalent", Arrays.equals((Object[]) objects[i],
                        (Object[]) object));
            else
                assertEquals("Expect these two objects to be equivalent", objects[i], object);
        }
    }

    /**
     * Used to modify the given classname such that the last '.' or '$' field
     * has it's first character modified. This is used to change a classname
     * just enough so that when loaded into a special class loader it isn't
     * found anywhere else.
     * 
     * @param orig
     *            original class name
     * @return modified class name
     */
    private String modifyName(String orig)
    {
        int index = orig.lastIndexOf('$');
        if (index < 0) index = orig.lastIndexOf('.');

        if (index < 0)
            index = 0;
        else
            ++index;

        // Modify char at the index...
        char charAt = orig.charAt(index);
        ++charAt;

        return orig.substring(0, index) + charAt + orig.substring(index + 1);
    }

    /**
     * Verifies that serialization of app-specific class can be a problem.
     */
    public void testProblemSerialization() throws Exception
    {
        // Create a class loader that loads a special version of a class
        final TestClassLoader cl = new TestClassLoader();
        String origClassName = DummyClass.class.getName();
        String dataClassName = modifyName(origClassName);
        cl.addClass(origClassName, dataClassName);

        // Create an instance of that class
        Class dataClass = Class.forName(dataClassName, false, cl);
        Object dataObject = dataClass.newInstance();

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(dataObject);
        oos.flush();

        // De-serialize
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));

        try
        {
            ois.readObject();
            fail("Expected ClassNotFoundException");
        }
        catch (ClassNotFoundException e)
        { /* expected */
        }

        // Now, de-serialize with ClassLoader
        ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))
        {
            protected Class resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException,
                    StreamCorruptedException
            {
                return Class.forName(desc.getName(), false, cl);
            }
        };

        Object dataObject2 = ois.readObject();

        assertSame("Expected dataObjects to have same class", dataClass, dataObject2.getClass());
    }

    /**
     * Tests what happens if different ClassLoaders are used.
     */
    public void testSerialization_classLoader() throws Exception
    {
        // Create a class loader that loads a special version of a class
        TestClassLoader cl = new TestClassLoader();
        String origClassName = DummyClass.class.getName();
        String dataClassName = origClassName;
        cl.addClass(origClassName, dataClassName);

        // Create an instance of that class
        Class dataClass = Class.forName(dataClassName, false, cl);
        dataClass.newInstance(); // extra call for good measure...
        Object dataObject = dataClass.newInstance();

        // Wrap with ADC
        AppDataContainer adc = new AppDataContainer((Serializable) dataObject);

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(adc);
        oos.flush();

        // De-serialize
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        AppDataContainer adc2 = (AppDataContainer) ois.readObject();

        // getObject() should not succeed given CL
        Serializable object = adc2.getObject();

        // At this point, dataClass and object.getClass() should be different...
        assertNotSame("Expected different Classes given different ClassLoaders", dataClass, object.getClass());

        // getObject() should succeed given CL
        object = adc2.getObject(cl);

        // At this point, should be same CL
        assertSame("Expected different Classes given different ClassLoaders", dataClass, object.getClass());
    }

    /**
     * Tests what happens if ClassLoader is *required*.
     */
    public void testSerialization_classLoaderRequired() throws Exception
    {
        // Create a class loader that loads a special version of a class
        TestClassLoader cl = new TestClassLoader();
        String origClassName = DummyClass.class.getName();
        String dataClassName = modifyName(origClassName);
        cl.addClass(origClassName, dataClassName);

        // Create an instance of that class
        Class dataClass = Class.forName(dataClassName, false, cl);
        Object dataObject = dataClass.newInstance();

        // Wrap with ADC
        AppDataContainer adc = new AppDataContainer((Serializable) dataObject);

        // Serialize
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(adc);
        oos.flush();

        // De-serialize
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        AppDataContainer adc2 = (AppDataContainer) ois.readObject();

        // getObject() should not succeed given CL
        try
        {
            adc2.getObject();
            fail("Expected ClassNotFoundException");
        }
        catch (ClassNotFoundException e)
        { /* expected */
        }

        // getObject() should succeed given CL
        Serializable object = adc2.getObject(cl);

        // At this point, should be same CL
        assertSame("Expected different Classes given different ClassLoaders", dataClass, object.getClass());
    }

    private class TestClassLoader extends ClassLoader
    {
        public Hashtable classNames = new Hashtable();

        private Hashtable classes = new Hashtable();

        public void addClass(String className)
        {
            addClass(className, className);
        }

        public void addClass(String className, String alternate)
        {
            assertEquals("New name must be same as old name", alternate.length(), className.length());
            classNames.put(alternate, className);
        }

        /**
         * Overrides ClassLoader.loadClass() to not go to the parent for certain
         * classes, possibly loading a secondary copy.
         * 
         * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
         */
        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            String origName = (String) classNames.get(name);
            if (origName == null)
            {
                return super.loadClass(name, resolve);
            }
            else
            {
                Class found = (Class) classes.get(name);
                if (found != null) return found;
                found = loadMyClass(name, origName);
                classes.put(name, found);
                if (resolve) resolveClass(found);
                return found;
            }
        }

        private int updateBytes(byte[] array, int index, byte[] oldBytes, byte[] newBytes)
        {
            if (index + oldBytes.length > array.length) return index + oldBytes.length;

            int j = 0;
            int i = index;
            while (j < oldBytes.length)
            {
                if (array[i++] != oldBytes[j++]) break;
            }
            if (j != oldBytes.length) return index + 1;

            // At this point we have found oldBytes...
            System.arraycopy(newBytes, 0, array, index, newBytes.length);
            return index + newBytes.length;
        }

        private void updateBytes(byte[] array, String oldStr, String newStr)
        {
            if (oldStr == newStr || oldStr.equals(newStr)) return;

            byte[] oldBytes = oldStr.getBytes();
            byte[] newBytes = newStr.getBytes();

            for (int i = 0; i < array.length;)
            {
                i = updateBytes(array, i, oldBytes, newBytes);
            }
        }

        private String lastPart(String str, char delim)
        {
            int index = str.lastIndexOf(delim);
            if (index < 0)
                return str;
            else
                return str.substring(index + 1);
        }

        private Class loadMyClass(String newName, String name) throws ClassNotFoundException
        {
            byte[] classBytes = null;

            try
            {
                String rezName = name.replace('.', '/') + ".class";
                InputStream is = getResourceAsStream(rezName);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int bytes = 0;

                while ((bytes = is.read(buf)) > 0)
                {
                    bos.write(buf, 0, bytes);
                }
                classBytes = bos.toByteArray();
                updateBytes(classBytes, lastPart(lastPart(name, '.'), '$'), lastPart(lastPart(newName, '.'), '$'));
            }
            catch (Exception e)
            {
                throw new ClassNotFoundException(e.getMessage());
            }

            return defineClass(newName, classBytes, 0, classBytes.length);
        }
    }

    public static class DummyClass implements Serializable
    {
        private static int staticValue = 0;

        private final int value = staticValue++;

        public int hashCode()
        {
            return value;
        }

        public boolean equals(Object obj)
        {
            if (getClass() != obj.getClass()) return false;
            return value == ((DummyClass) obj).value;
        }
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AppDataContainerTest.class);
        return suite;
    }

    public AppDataContainerTest(String name)
    {
        super(name);
    }
}
