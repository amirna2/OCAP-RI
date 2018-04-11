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

package org.ocap.system;

import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

import java.awt.Color;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppProxyTest.DummySecurityManager;

/**
 * Tests EASModuleRegistrar
 * 
 * @author Aaron Kamienski
 */
public class EASModuleRegistrarTest extends TestCase
{
    /**
     * Tests public fields.
     */
    public void testFields()
    {
        TestUtils.testNoPublicFields(EASModuleRegistrar.class);
        TestUtils.testNoAddedFields(EASModuleRegistrar.class, fieldNames);
        TestUtils.testFieldValues(EASModuleRegistrar.class, fieldNames, fieldValues);
    }

    /**
     * Tests no public constructor.
     */
    public void testNoPublicConstructor()
    {
        TestUtils.testNoPublicConstructors(EASModuleRegistrar.class);
    }

    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        assertNotNull("getInstance() should not return null", emr);
        assertSame("getInstance() should return same instance on repeated calls", emr, EASModuleRegistrar.getInstance());
    }

    /**
     * Tests getInstance() permission checks.
     */
    public void testGetInstance_security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            sm.p = null;
            EASModuleRegistrar.getInstance();
            assertNotNull("Expected checkPermission() to be called", sm.p);
            assertTrue("Expected MonitorAppPermission to be tested", sm.p instanceof MonitorAppPermission);
            assertEquals("Expected handler.eas to be tested", "handler.eas", sm.p.getName());
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests getEASCapabilities().
     */
    public void testGetEASCapabilities()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        for (int i = 0; i < capabilityTypes.length; ++i)
        {
            Object[] caps = emr.getEASCapability(fieldValues[i]);

            assertNotNull("Capabilities should be non-null array for " + fieldValues[i], caps);
            for (int j = 0; j < caps.length; ++j)
            {
                assertNotNull("Capability entries should be non-null for " + fieldValues[i] + ":" + j, caps[j]);
                assertTrue("Unexpected type returned for " + fieldValues[i] + ":" + j,
                        capabilityTypes[i].isAssignableFrom(caps[j].getClass()));
            }

            Object[] caps2 = emr.getEASCapability(fieldValues[i]);
            assertEquals("Expected same length array for successive calls", caps.length, caps2.length);
            for (int j = 0; j < caps2.length; ++j)
            {
                assertSame("Capability should be same on successive calls for " + fieldValues[i] + ":" + j, caps[j],
                        caps2[j]);
            }
        }

        // Test individual capabilties...?
    }

    /**
     * Tests getEASCapability() for detection of invalid.
     */
    public void testGetEASCapabilities_invalid()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        MinMax boundary = new MinMax(capabilityTypes.length);
        int min = boundary.min;
        int max = boundary.max;

        // min boundary
        try
        {
            emr.getEASCapability(min);
            fail("Expected IllegalArgumentException for " + min);
        }
        catch (IllegalArgumentException e)
        {
        }

        // max boundary
        try
        {
            emr.getEASCapability(max);
            fail("Expected IllegalArgumentException for " + max);
        }
        catch (IllegalArgumentException e)
        {
        }

        min = Integer.MIN_VALUE;
        max = Integer.MAX_VALUE;

        // MIN_VALUE
        try
        {
            emr.getEASCapability(min);
            fail("Expected IllegalArgumentException for " + min);
        }
        catch (IllegalArgumentException e)
        {
        }

        // MAX_VALUE
        try
        {
            emr.getEASCapability(max);
            fail("Expected IllegalArgumentException for " + max);
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Tests getEASCapability() for prevention of modification by copying data.
     */
    public void testGetEASCapabilities_safety()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        for (int i = 0; i < capabilityTypes.length; ++i)
        {
            Object[] caps = emr.getEASCapability(fieldValues[i]);

            for (int j = 0; j < caps.length; ++j)
                caps[j] = capabilityObjects[i];

            Object[] caps2 = emr.getEASCapability(fieldValues[i]);
            for (int j = 0; j < caps.length; ++j)
            {
                assertNotNull("Capability entries should be copied for safeguarding: " + fieldValues[i] + ":" + j,
                        caps[j]);
            }
        }
    }

    /**
     * Tests getEASAttribute().
     */
    public void testGetEASAttribute()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        for (int i = 0; i < fieldValues.length; ++i)
        {
            Object attrib = emr.getEASAttribute(fieldValues[i]);
            assertNotNull("Current attribute should be non-null: " + fieldValues[i], attrib);
            assertTrue("Attribute is of unexpected type: " + fieldValues[i],
                    attribTypes[i].isAssignableFrom(attrib.getClass()));

            // Successive call should produce same
            Object attrib2 = emr.getEASAttribute(fieldValues[i]);
            assertEquals("Expected equivalent attribute for successive calls: " + fieldValues[i], attrib, attrib2);
        }
    }

    /**
     * Tests setEASAttribute().
     */
    public void testSetEASAttribute()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        // foreach attribute (w/ capabilities)
        for (int i = 0; i < capabilityTypes.length; ++i)
        {
            Object caps[] = emr.getEASCapability(fieldValues[i]);
            Object currAttrib[] = { emr.getEASAttribute(fieldValues[i]) };
            int field[] = { fieldValues[i] };

            // Set attribute and test it
            for (int j = 0; j < caps.length; ++j)
            {
                Object value[] = { caps[j] };
                emr.setEASAttribute(field, value);

                assertEquals("Expected set value to be retrieved: " + fieldValues[i], caps[j],
                        emr.getEASAttribute(fieldValues[i]));
            }

            // Reset attribute to original, and test it
            emr.setEASAttribute(field, currAttrib);
            assertEquals("Expected set value to be retrieved: " + fieldValues[i], currAttrib[0],
                    emr.getEASAttribute(fieldValues[i]));
        }

        // Float attributes
        for (int i = capabilityTypes.length; i < fieldValues.length; ++i)
        {
            Float caps[] = { new Float(0.0F), new Float(0.3F), new Float(0.5F), new Float(0.8F), new Float(1.0F) };
            Object currAttrib[] = { emr.getEASAttribute(fieldValues[i]) };
            int field[] = { fieldValues[i] };

            // Set attribute and test it
            for (int j = 0; j < caps.length; ++j)
            {
                Object[] attrib = { caps[j] };
                emr.setEASAttribute(field, attrib);

                assertEquals("Expected set value to be retrieved: " + fieldValues[i], caps[j],
                        emr.getEASAttribute(fieldValues[i]));
            }

            // Reset attribute to original, and test it
            emr.setEASAttribute(field, currAttrib);
            assertEquals("Expected set value to be retrieved: " + fieldValues[i], currAttrib[0],
                    emr.getEASAttribute(fieldValues[i]));
        }
    }

    /**
     * Tests setEASAttribute() w/ multiple attribute/value pairs.
     */
    public void testSetEASAttribute_multi() throws Exception
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        int fields[] = fieldValues;
        Object caps[][] = new Object[fieldValues.length][];

        // First, figure out all of the possible capabilities
        for (int i = 0; i < capabilityTypes.length; ++i)
        {
            caps[i] = emr.getEASCapability(fieldValues[i]);
        }
        for (int i = capabilityTypes.length; i < fieldValues.length; ++i)
        {
            caps[i] = new Object[] { new Float(0.3F), new Float(0.5F), new Float(0.8F), new Float(1.0F) };
        }

        Object value[] = new Object[fieldValues.length];
        Object curr[] = new Object[fieldValues.length];

        // We can multiply the number of capabilities for each attribute to
        // find the number of combinations.
        // (Or plus one to include absence of attribute.)
        // This is the number of things we could test.

        int max = 1;
        for (int i = 0; i < caps.length; ++i)
            max *= caps[i].length;

        // Each value between 0..max can be considered an address
        // Each address uniquely identified a combination of attributes
        for (int address = 0; address < max; ++address)
        {
            // System.out.print("Addres="+address+": ");
            int addr = address;
            for (int i = 0; i < caps.length; ++i)
            {
                // Determine which capability to try
                int idx = addr % caps[i].length;
                value[i] = caps[i][idx];
                // Adjust address to consider next field
                addr /= caps[i].length;

                // Remember the current attribute
                curr[i] = emr.getEASAttribute(fields[i]);

                // System.out.print(fields[i]+":"+idx+"/"+caps[i].length+", ");
            }
            // System.out.println();

            // Call setEASAttribute(), finally.
            Object expected[] = value;
            try
            {
                emr.setEASAttribute(fields, value);
            }
            catch (IllegalArgumentException e)
            {
                // Don't expect IllegalArgumentException, but could get it.
                // E.g., if two colors are incompatible.
                // In which case we expect things to go unchanged
                expected = curr;
            }

            // Verify using getEASAttribute()
            for (int i = 0; i < caps.length; ++i)
            {
                assertEquals("Expected set value to be retrieved: " + fieldValues[i], expected[i],
                        emr.getEASAttribute(fieldValues[i]));
            }
        }
    }

    /**
     * Tests setEASAttribute() w/ invalid attribute/value pairs. Including:
     * <ul>
     * <li>array lengths that don't match
     * <li>null arrays
     * </ul>
     * 
     * If setting fails, then no attributes should be set.
     */
    public void testSetEASAttribute_invalid()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        // foreach attribute
        for (int i = 0; i < fieldValues.length; ++i)
        {
            Object currAttrib[] = { emr.getEASAttribute(fieldValues[i]) };
            int field[] = { fieldValues[i] };

            // Null arrays
            try
            {
                emr.setEASAttribute(field, null);
                fail("Expected IllegalArgumentException");
            }
            catch (IllegalArgumentException e)
            {
            }
            try
            {
                emr.setEASAttribute(null, currAttrib);
                fail("Expected IllegalArgumentException");
            }
            catch (IllegalArgumentException e)
            {
            }

            // Array lengths don't match
            try
            {
                emr.setEASAttribute(field, new Object[0]);
                fail("Expected IllegalArgumentException");
            }
            catch (IllegalArgumentException e)
            {
            }
            try
            {
                emr.setEASAttribute(new int[0], currAttrib);
                fail("Expected IllegalArgumentException");
            }
            catch (IllegalArgumentException e)
            {
            }
        }
    }

    /**
     * Tests set/getEASAttribute for detection of invalid field arguments.
     */
    public void testGetSetEASAttribute_invalid()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        MinMax boundary = new MinMax();
        int min = boundary.min;
        int max = boundary.max;

        // min boundary
        try
        {
            emr.getEASAttribute(min);
            fail("Expected IllegalArgumentException for " + min);
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            emr.setEASAttribute(new int[] { min }, new Object[] { new Object() });
            fail("Expected IllegalArgumentException for " + min);
        }
        catch (IllegalArgumentException e)
        {
        }

        // max boundary
        try
        {
            emr.getEASAttribute(max);
            fail("Expected IllegalArgumentException for " + max);
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            emr.setEASAttribute(new int[] { max }, new Object[] { new Object() });
            fail("Expected IllegalArgumentException for " + max);
        }
        catch (IllegalArgumentException e)
        {
        }

        min = Integer.MIN_VALUE;
        max = Integer.MAX_VALUE;

        // MIN_VALUE
        try
        {
            emr.getEASAttribute(min);
            fail("Expected IllegalArgumentException for " + min);
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            emr.setEASAttribute(new int[] { min }, new Object[] { new Object() });
            fail("Expected IllegalArgumentException for " + min);
        }
        catch (IllegalArgumentException e)
        {
        }

        // MAX_VALUE
        try
        {
            emr.getEASAttribute(max);
            fail("Expected IllegalArgumentException for " + max);
        }
        catch (IllegalArgumentException e)
        {
        }
        try
        {
            emr.setEASAttribute(new int[] { max }, new Object[] { new Object() });
            fail("Expected IllegalArgumentException for " + max);
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Tests setEASAttribute for detection of invalid arguments. Tests for
     * invalid type.
     */
    public void testSetEASAttribute_invalidType()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        // Test invalid type; single attribute
        for (int i = 0; i < attribTypes.length; ++i)
        {
            int field[] = { fieldValues[i] };
            for (int j = 0; j < invalidAttribs[i].length; ++j)
            {
                try
                {
                    Object[] value = { invalidAttribs[i][j] };
                    emr.setEASAttribute(field, value);
                    fail("Expected InvalidArgumentException for incorrect attrib type");
                }
                catch (IllegalArgumentException e)
                {
                }
            }
        }
    }

    /**
     * Tests setEASAttribute for detection of invalid arguments. Tests for
     * invalid type.
     */
    public void testSetEASAttribute_multi_invalidType()
    {
        // Test single invalid attribute; given multi attributes
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        int fields[] = fieldValues;
        Object caps[][] = new Object[fieldValues.length][];
        Object bad[][] = invalidAttribs;

        // First, figure out all of the possible capabilities
        for (int i = 0; i < capabilityTypes.length; ++i)
        {
            caps[i] = emr.getEASCapability(fieldValues[i]);
        }
        for (int i = capabilityTypes.length; i < fieldValues.length; ++i)
        {
            caps[i] = new Object[] { new Float(0.3F), new Float(0.5F), new Float(0.8F), new Float(1.0F) };
        }

        Object value[] = new Object[fieldValues.length];
        Object curr[] = new Object[fieldValues.length];

        // We can multiply the number of capabilities for each attribute to
        // find the number of combinations.
        // (Or plus one to include absence of attribute.)
        // This is the number of things we could test.

        int max = 1;
        for (int i = 0; i < caps.length; ++i)
            max *= caps[i].length;

        // Each value between 0..max can be considered an address
        // Each address uniquely identified a combination of attributes
        int badIdx = 0;
        int entry = 0;
        for (int address = 0; address < max; ++address)
        {
            int addr = address;
            for (int i = 0; i < caps.length; ++i)
            {
                // Determine which capability to try
                int idx = addr % caps[i].length;
                value[i] = caps[i][idx];
                // Adjust address to consider next field
                addr /= caps[i].length;

                // Remember the current attribute
                curr[i] = emr.getEASAttribute(fields[i]);
            }

            // Which entry to set incorrectly
            badIdx = (badIdx + 1) % caps.length;
            // Select invalid entry
            value[badIdx] = bad[badIdx][(entry++) % bad[badIdx].length];

            // Call setEASAttribute(), finally.
            try
            {
                emr.setEASAttribute(fields, value);
                fail("Expected IllegalArgumentException for " + badIdx + " @" + address);
            }
            catch (IllegalArgumentException e)
            {
            }

            // Verify using getEASAttribute()
            for (int i = 0; i < caps.length; ++i)
            {
                assertEquals("Expected attributes to be unchanged", curr[i], emr.getEASAttribute(fieldValues[i]));
            }
        }
    }

    private class MinMax
    {
        int min, max;

        public MinMax()
        {
            this(fieldValues.length);
        }

        public MinMax(int length)
        {
            min = Integer.MAX_VALUE;
            max = Integer.MIN_VALUE;

            // Find min-1 and max+1
            for (int i = 0; i < length; ++i)
            {
                min = Math.min(min, fieldValues[i]);
                max = Math.max(max, fieldValues[i]);
            }
            --min;
            ++max;
        }
    }

    /**
     * Figure out invalid attribute settings for the given set of capabilities
     * and attribute type.
     */
    private Object[] findBadAttribs(Class type, Object[] caps)
    {
        boolean color = false;
        if (Integer.class.isAssignableFrom(type) || (color = Color.class.isAssignableFrom(type)))
        {
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;

            for (int i = 0; i < caps.length; ++i)
            {
                int x = color ? ((Color) caps[i]).getRGB() : ((Integer) caps[i]).intValue();

                max = Math.max(x, max);
                min = Math.max(x, min);
            }
            ++max;
            --min;

            if (color)
                return new Object[] { new Color(min), new Color(max) };
            else
                return new Object[] { new Integer(min), new Integer(max) };
        }
        else if (String.class.isAssignableFrom(type))
        {
            String bad = "";
            for (int i = 0; i < caps.length; ++i)
            {
                bad += caps[i];
            }
            return new Object[] { bad, bad + "x", bad + "y" };
        }
        else if (Float.class.isAssignableFrom(type))
        {
            return new Float[] { new Float(-0.1F), new Float(1.1) };
        }
        fail("Internal test failure");
        // unreachable
        return null;
    }

    /**
     * Tests setEASAttribute for detection of invalid arguments. Tests for
     * unsupported capability.
     */
    public void testSetEASAttribute_invalidCap()
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        // Test unspecified capability; single attribute
        for (int i = 0; i < attribTypes.length; ++i)
        {
            Object bad[] = findBadAttribs(attribTypes[i],
                    (i < capabilityTypes.length) ? emr.getEASCapability(fieldValues[i]) : null);
            int[] field = { fieldValues[i] };
            Object currAttrib = emr.getEASAttribute(fieldValues[i]);

            // Set attribute and test it
            for (int j = 0; j < bad.length; ++j)
            {
                try
                {
                    emr.setEASAttribute(field, new Object[] { bad[j] });
                    fail("Expected InvalidArgumentException for out-of-range value");
                }
                catch (IllegalArgumentException e)
                {
                }
                assertEquals("Expected failed set to not affect value: " + fieldValues[i], currAttrib,
                        emr.getEASAttribute(fieldValues[i]));
            }
        }
    }

    /**
     * Tests setEASAttribute for detection of invalid arguments. Tests for
     * unsupported capability.
     */
    public void testSetEASAttribute_multi_invalidCap()
    {
        // Test single invalid attribute; given multi attributes
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        int fields[] = fieldValues;
        Object caps[][] = new Object[fieldValues.length][];
        Object bad[][] = new Object[fieldValues.length][];

        // First, figure out all of the possible capabilities
        for (int i = 0; i < capabilityTypes.length; ++i)
        {
            caps[i] = emr.getEASCapability(fieldValues[i]);
        }
        for (int i = capabilityTypes.length; i < fieldValues.length; ++i)
        {
            caps[i] = new Object[] { new Float(0.3F), new Float(0.5F), new Float(0.8F), new Float(1.0F) };
        }

        // Next, figure out some possible bad values
        for (int i = 0; i < fieldValues.length; ++i)
        {
            bad[i] = findBadAttribs(attribTypes[i], (i < capabilityTypes.length) ? emr.getEASCapability(fieldValues[i])
                    : null);
        }

        Object value[] = new Object[fieldValues.length];
        Object curr[] = new Object[fieldValues.length];

        // We can multiply the number of capabilities for each attribute to
        // find the number of combinations.
        // (Or plus one to include absence of attribute.)
        // This is the number of things we could test.

        int max = 1;
        for (int i = 0; i < caps.length; ++i)
            max *= caps[i].length;

        // Each value between 0..max can be considered an address
        // Each address uniquely identified a combination of attributes
        int badIdx = 0;
        int entry = 0;
        for (int address = 0; address < max; ++address)
        {
            int addr = address;
            for (int i = 0; i < caps.length; ++i)
            {
                // Determine which capability to try
                int idx = addr % caps[i].length;
                value[i] = caps[i][idx];
                // Adjust address to consider next field
                addr /= caps[i].length;

                // Remember the current attribute
                curr[i] = emr.getEASAttribute(fields[i]);
            }

            // Which entry to set incorrectly
            badIdx = (badIdx + 1) % caps.length;
            // Select invalid entry
            value[badIdx] = bad[badIdx][(entry++) % bad[badIdx].length];

            // Call setEASAttribute(), finally.
            try
            {
                emr.setEASAttribute(fields, value);
                fail("Expected IllegalArgumentException for " + badIdx + " @" + address);
            }
            catch (IllegalArgumentException e)
            {
            }

            // Verify using getEASAttribute()
            for (int i = 0; i < caps.length; ++i)
            {
                assertEquals("Expected attributes to be unchanged", curr[i], emr.getEASAttribute(fieldValues[i]));
            }
        }
    }

    /**
     * Tests get/setEASAttribute() for prevention of modification by copying
     * data. All types other than Color are "final", so we need not worry about
     * this. Assuming that setEASAttribute() won't accept a MyColor, and Color
     * doesn't provide any setters, then things should be fine.
     * <p>
     * As such, this test doesn't do anything!
     */
    public void testEASAttribute_safety()
    {
        // this method is left intentionally blank
    }

    public void testRegisterEASHandler_illegal() throws Exception
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();
        try
        {
            emr.registerEASHandler(null);
            fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        { /* expected */
        }
    }

    /**
     * Tests that unregisterEASHandler() doesn't leak the listener.
     */
    public void testRegisterEASHandler_UnsetLeak() throws Exception
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        EASHandler h = new Handler();
        Reference r = new WeakReference(h);

        emr.registerEASHandler(h);
        try
        {
            h = null;
            System.gc();
            System.gc();
            assertNotNull("Handler should still be remembered", r.get());

            emr.unregisterEASHandler();
            System.gc();
            System.gc();
            assertNull("Handler should be gc'ed after unset", r.get());
        }
        finally
        {
            emr.unregisterEASHandler();
        }
    }

    /**
     * Tests that registerEASHandler(non-null) doesn't leak the previous
     * Handler.
     */
    public void testRegisterEASHandler_ReplaceLeak() throws Exception
    {
        EASModuleRegistrar emr = EASModuleRegistrar.getInstance();

        EASHandler h1 = new Handler();
        EASHandler h2 = new Handler();
        Reference r1 = new WeakReference(h1);
        Reference r2 = new WeakReference(h2);

        emr.registerEASHandler(h1);
        try
        {
            h1 = null;
            System.gc();
            System.gc();
            assertNotNull("Handler should still be remembered", r1.get());

            emr.registerEASHandler(h2);
            h2 = null;
            System.gc();
            System.gc();
            assertNull("Handler should be gc'ed after replace", r1.get());
            assertNotNull("Replacement Handler should still be remembered", r2.get());
        }
        finally
        {
            emr.unregisterEASHandler();
        }
    }

    /**
     * Names of public static fields.
     */
    private static final String[] fieldNames = { "EAS_ATTRIBUTE_FONT_COLOR", "EAS_ATTRIBUTE_FONT_STYLE",
            "EAS_ATTRIBUTE_FONT_FACE", "EAS_ATTRIBUTE_FONT_SIZE", "EAS_ATTRIBUTE_BACK_COLOR",
            "EAS_ATTRIBUTE_FONT_OPACITY", "EAS_ATTRIBUTE_BACK_OPACITY", };

    /**
     * Expected alues of public static fields.
     */
    private static final int[] fieldValues = { 1, 2, 3, 4, 5, 6, 7, };

    /**
     * Supported attribute types.
     */
    private static final Class[] attribTypes = { Color.class, String.class, String.class, Integer.class, Color.class,
            Float.class, Float.class };

    private class Handler implements EASHandler
    {
        public boolean notifyPrivateDescriptor(byte[] descriptor)
        {
            return true;
        }

        public void stopAudio()
        {
        }
    }

    /**
     * Private extension of color to test invalid attributes.
     */
    static class MyColor extends Color
    {
        public MyColor(Color c)
        {
            super(c.getRGB());
        }
    }

    /**
     * Sets of invalid attribute values for each attribute.
     */
    private static final Object[][] invalidAttribs = {
            new Object[] { null, new MyColor(Color.red), "hello", new Integer(1), new Float(1), new Object() },
            new Object[] { null, Color.red, new Integer(1), new Float(1), new Object() },
            new Object[] { null, Color.red, new Integer(1), new Float(1), new Object() },
            new Object[] { null, Color.red, "hello", new Float(1), new Object() },
            new Object[] { null, new MyColor(Color.red), "hello", new Integer(1), new Float(1), new Object() },
            new Object[] { null, Color.red, "hello", new Integer(1), new Object() },
            new Object[] { null, Color.red, "hello", new Integer(1), new Object() }, };

    /**
     * Type of objects returned by getEASCapability.
     */
    private static final Class[] capabilityTypes = { Color.class, String.class, String.class, Integer.class,
            Color.class, };

    /**
     * Some objects of the type appropriate for EAS capabilities.
     */
    private static final Object[] capabilityObjects = { new Color(0x12, 0x34, 0x45), "flcl", "bebop",
            new Integer(0xcafebeef), new Color(0xfe, 0xdc, 0xba) };

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
        TestSuite suite = new TestSuite(EASModuleRegistrarTest.class);
        return suite;
    }

    public EASModuleRegistrarTest(String name)
    {
        super(name);
    }
}
