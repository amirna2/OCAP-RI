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

package org.cablelabs.test;

import junit.framework.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Some static utility methods that can be used by JUnit tests.
 */
public class TestUtils extends Assert
{
    /** Not publicly instantiable. */
    private TestUtils()
    {
    }

    /*  *********************** Suite ******************** */

    /**
     * Standard method for implementing suite() in a TestCase.
     * 
     * @return a TestSuite that contains all no-argument methods that begin with
     *         "test".
     */
    public static TestSuite suite(Class testClass)
    {
        return new TestSuite(testClass);
    }

    /**
     * Creates a test suite composed of tests starting with "xtest". Each test
     * is created with a special constructor which takes an extra Object
     * parameter that specifies the parameters to the test.
     */
    /*
     * public static TestSuite suite(Class testClass, Object parameters[]) {
     * 
     * }
     */

    /*  *********************** Ancestry ******************** */

    /**
     * Tests that subclass is a direct descendent (subclass!) of superclass.
     * I.e., subclass instanceof superclass == true.
     */
    public static void testExtends(Class subclass, Class superclass)
    {
        assertSame("Should extend " + superclass.getName(), subclass.getSuperclass(), superclass);
    }

    /**
     * Tests that subclass implements the given interface.
     */
    public static void testImplements(Class subclass, Class iface)
    {
        Class[] ifaces = subclass.getInterfaces();

        if (ifaces != null) for (int i = 0; i < ifaces.length; ++i)
            if (iface == ifaces[i]) return;
        fail("Should implement " + iface.getName());
    }

    /*  ******************* No Public Fields/Constructors ************** */

    /**
     * Tests that there are no public (non-final) fields exposed by the given
     * object/class.
     */
    public static void testNoPublicFields(Class cl)
    {
        Field f[] = cl.getFields();

        for (int i = 0; i < f.length; ++i)
        {
            int mod = f[i].getModifiers();
            if (Modifier.isPublic(mod) && !Modifier.isFinal(mod))
                fail("The field " + f[i].getName() + " is exposed (public)");
        }
    }

    /**
     * Tests that there are no public constructors exposed by the given class.
     */
    public static void testNoPublicConstructors(Class c)
    {
        Constructor cons[] = c.getConstructors();

        if (cons != null) for (int i = 0; i < cons.length; ++i)
            assertTrue("There should be no public constructors", !Modifier.isPublic(cons[i].getModifiers()));
    }

    /**
     * Tests that the given fields are:
     * <ol>
     * <li>Defined as part of the class
     * <li>public static final
     * <li>Of the appropriate type (if one is given)
     * <li>Not null (i.e., defined)
     * </ol>
     */
    public static void testPublicFields(Class cl, String fields[], Class type)
    {
        for (int i = 0; i < fields.length; ++i)
        {
            String field = fields[i];
            try
            {
                Field f = cl.getField(field);
                assertNotNull("The field '" + field + "' is not defined for " + "class '" + cl.getName() + "'", f);

                // Check for 'public static final'
                int modifiers = f.getModifiers();
                assertTrue("The field '" + field + "' is not public", Modifier.isPublic(modifiers));
                assertTrue("The field '" + field + "' is not static", Modifier.isStatic(modifiers));
                assertTrue("The field '" + field + "' is not final", Modifier.isFinal(modifiers));

                // Check for required type
                if (type != null)
                    assertSame("The field '" + field + "' is not of the correct type", type, f.getType());

                assertNotNull("The field '" + field + "' is undefined" + f.get(null));
            }
            catch (NoSuchFieldException noField)
            {
                fail("The field '" + field + "' is not defined for " + "class '" + cl.getName() + "'");
            }
            catch (IllegalAccessException illegal)
            {
                fail("The field '" + field + "' is not accessible in " + "class '" + cl.getName() + "'");
            }
        }
    }

    /**
     * Tests that the given fields are the only defined public fields.
     */
    public static void testNoAddedFields(Class cl, String fields[])
    {
        Hashtable acceptable = new Hashtable();
        if (fields != null) for (int i = 0; i < fields.length; ++i)
            acceptable.put(fields[i], fields[i]);

        Field[] defFields = cl.getDeclaredFields();

        for (int i = 0; i < defFields.length; ++i)
        {
            Field f = defFields[i];
            int mods = f.getModifiers();

            if (Modifier.isPublic(mods))
            {
                String name = f.getName();
                assertNotNull("Unexpected public field defined: " + name, acceptable.get(name));
            }
        }
    }

    /**
     * Tests that the given fields have unique definitions. If the
     * <code>bitwise</code> parameter is true, then the values should be tested
     * for the additional constraint of no overlapping bits.
     * <p>
     * Note that all fields are assumed to be static integer fields.
     */
    public static void testUniqueFields(Class cl, String fields[], boolean bitwise, int ofs, int run)
    {
        int or = 0;
        int xor = 0;
        Hashtable set = new Hashtable();
        String field = null;

        try
        {
            final int end = Math.min(fields.length - ofs, ofs + run);
            for (int i = ofs; i < end; ++i)
            {
                Field f = cl.getField(fields[i]);
                int val = f.getInt(null);
                Integer idx = new Integer(val);

                // Unique
                assertEquals("The field " + fields[i] + "=" + val + " is not unique;" + " previously defined by '"
                        + set.get(idx) + "'", false, set.get(idx) != null);
                set.put(idx, fields[i]);

                // Bitwise
                if (bitwise)
                {
                    or |= val;
                    xor ^= val;
                    assertEquals("The field " + fields[i] + "=" + Integer.toHexString(val)
                            + " has bits that overlap those defined by" + " other fields", or, xor);
                }
            }
        }
        catch (NoSuchFieldException noField)
        {
            fail("The field '" + field + "' is not defined in " + cl.getName());
        }
        catch (IllegalAccessException illegal)
        {
            fail("The field '" + field + "' could not be accessed in " + cl.getName());
        }
    }

    /**
     * @see #testUniqueFields(Class,String[],boolean,int,int)
     */
    public static void testUniqueFields(Class cl, String fields[], boolean bitwise)
    {
        testUniqueFields(cl, fields, bitwise, 0, fields.length);
    }

    /**
     * Tests the expected values for the the given fields.
     */
    public static void testFieldValues(Class cl, String fields[], int values[])
    {
        assertEquals("expected same length fields/values", fields.length, values.length);
        String field = null;
        try
        {
            for (int i = 0; i < fields.length; ++i)
            {
                field = fields[i];
                Field f = cl.getField(field);
                int val = f.getInt(null);

                assertEquals("Unexpected value for " + field, values[i], val);
            }
        }
        catch (NoSuchFieldException noField)
        {
            fail("The field '" + field + "' is not defined in " + cl.getName());
        }
        catch (IllegalAccessException illegal)
        {
            fail("The field '" + field + "' could not be accessed in " + cl.getName());
        }
    }
}
