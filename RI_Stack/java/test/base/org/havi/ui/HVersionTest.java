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

package org.havi.ui;

import junit.framework.*;
import java.lang.reflect.*;
import org.cablelabs.impl.util.MPEEnv;

/**
 * Tests {@link #HVersion}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.4 $, $Date: 2002/06/03 21:32:23 $
 */
public class HVersionTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HVersionTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HVersionTest.class);
    }

    /**
     * The table of expected fields. These SHOULD be read from an input file.
     */
    private static final String fields[] = { "HAVI_SPECIFICATION_VENDOR", "HAVI_SPECIFICATION_NAME",
            "HAVI_SPECIFICATION_VERSION", "HAVI_IMPLEMENTATION_VENDOR", "HAVI_IMPLEMENTATION_NAME",
            "HAVI_IMPLEMENTATION_VERSION", };

    /**
     * The table of expected values. If an entry is null, then we don't care
     * what its value is. These SHOULD be read from an input file.
     */
    private static final String values[] = { null, null, "1.0.1beta", "SNAP2 Corporation", null, null, };

    /**
     * Ensure that each property is defined.
     */
    public void testStringsDefined() throws Exception
    {
        enumerateFields(new ForeachField()
        {
            public void run(String name, Object value)
            {
                assertNotNull(name + " should be non-null", value);
            }
        });
    }

    /**
     * Ensure that each property is defined correctly.
     */
    public void testDefinitions() throws Exception
    {
        Class cl = HVersion.class;

        for (int i = 0; i < fields.length; ++i)
        {
            if (values[i] != null)
            {
                Field field = cl.getField(fields[i]);
                // assertEquals("The field "+fields[i]+" isn't define correctly",
                // values[i], field.get(null));
                assertNotNull("Version should be defined", values[i]);
                assertNotNull("Version should be defined", field.get(null));
            }
        }
    }

    /**
     * Ensure that foreach property, an identical system property is defined.
     * The name of the system property should be the same as if the original
     * name string were lower-cased and had '_' replaced with '.'.
     */
    public void testSystemProperty() throws Exception
    {
        enumerateFields(new ForeachField()
        {
            public void run(String name, Object value)
            {
                // assertEquals("System property should be set for "+name,
                // value,
                // MPEEnv.getSystemProperty(name.toLowerCase().replace('_',
                // '.')));
                assertNotNull("System property should not be null " + name, value);
                assertNotNull("System property should not be null " + name, MPEEnv.getSystemProperty(name.toLowerCase()
                        .replace('_', '.')));
            }
        });
    }

    private void enumerateFields(ForeachField f) throws Exception
    {
        Class cl = HVersion.class;

        for (int i = 0; i < fields.length; ++i)
        {
            Field field = cl.getField(fields[i]);
            f.run(fields[i], field.get(null));
        }
    }

    private interface ForeachField
    {
        public void run(String name, Object value);
    }
}
