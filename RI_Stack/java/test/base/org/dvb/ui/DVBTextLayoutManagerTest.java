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

package org.dvb.ui;

import junit.framework.*;
import org.cablelabs.test.TestUtils;

/**
 * Tests DVBTextLayoutManagerTest.
 * 
 * @todo Should extend from an HTextLayoutManagerTest or use such an interface
 *       test.
 * @todo visual, graphical tests
 * 
 * @author Aaron Kamienski
 */
public class DVBTextLayoutManagerTest extends TestCase
{
    /**
     * Test render. This should be expanded into multiple test methods. Perhaps
     * with some doing visual testing.
     */
    public void XtestRender()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests the public fields.
     */
    public void testFields()
    {
        // Test expected values
        TestUtils.testFieldValues(DVBTextLayoutManager.class, hAlignNames, hAlignValues);
        TestUtils.testFieldValues(DVBTextLayoutManager.class, vAlignNames, vAlignValues);
        TestUtils.testFieldValues(DVBTextLayoutManager.class, orientNames, orientValues);
        TestUtils.testFieldValues(DVBTextLayoutManager.class, cornerNames, cornerValues);
    }

    /**
     * Tests no added public fields.
     */
    public void testNoAddedFields()
    {
        // Test no new fields
        String[] allFields = new String[hAlignNames.length + vAlignNames.length + orientNames.length
                + cornerNames.length];
        int i = 0;
        System.arraycopy(hAlignNames, 0, allFields, i, hAlignNames.length);
        System.arraycopy(vAlignNames, 0, allFields, i += hAlignNames.length, vAlignNames.length);
        System.arraycopy(orientNames, 0, allFields, i += vAlignNames.length, orientNames.length);
        System.arraycopy(cornerNames, 0, allFields, i += orientNames.length, cornerNames.length);

        TestUtils.testNoAddedFields(DVBTextLayoutManager.class, allFields);
    }

    /**
     * Checks all values as set by a constructor.
     */
    private void checkConstructor(String msg, DVBTextLayoutManager tlm, int halign, int valign, int orient, int corner,
            boolean wrap, int linespace, int letterspace, int htab)
    {
        assertEquals(msg + " unexpected hAlign", halign, tlm.getHorizontalAlign());
        assertEquals(msg + " unexpected vAlign", valign, tlm.getVerticalAlign());
        assertEquals(msg + " unexpected line orientation", orient, tlm.getLineOrientation());
        assertEquals(msg + " unexpected start corner", corner, tlm.getStartCorner());
        assertEquals(msg + " unexpected text wrapping", wrap, tlm.getTextWrapping());
        assertEquals(msg + " unexpected line space", linespace, tlm.getLineSpace());
        assertEquals(msg + " unexpected letter space", letterspace, tlm.getLetterSpace());
        assertEquals(msg + " unexpected horiz tab space", htab, tlm.getHorizontalTabSpacing());
    }

    /**
     * Tests the default constructor. Checks Default params:
     * 
     * <pre>
     *  (HORIZONTAL_START_ALIGN, 
     *   VERTICAL_START_ALIGN, 
     *   LINE_ORIENTATION_HORIZONTAL, 
     *   START_CORNER_UPPER_LEFT, 
     *   wrap = true, 
     *   linespace = (point size of the default font for HVisible) + 7, 
     *   letterspace = 0, 
     *   horizontalTabSpace = 56)
     * </pre>
     */
    public void testConstructor()
    {
        checkConstructor("Default", dvbtlm, DVBTextLayoutManager.HORIZONTAL_START_ALIGN,
                DVBTextLayoutManager.VERTICAL_START_ALIGN, DVBTextLayoutManager.LINE_ORIENTATION_HORIZONTAL,
                DVBTextLayoutManager.START_CORNER_UPPER_LEFT, true, // wrap
                // 26 + 7, // linespace = (point size of the default font for
                // HVisible) + 7
                -1, // linespace means to figure from default
                0, // letterspace
                56 // tabspace
        );
    }

    /**
     * Tests the parameterized constructor.
     */
    public void testConstructor_params()
    {
        final boolean wrapValues[] = { true, false };
        final int lineValues[] = { -1, 33, 45 };
        final int letterValues[] = { 0, 3 };
        final int tabValues[] = { 56, 140 };

        // Horizontal Align values
        doValuesTest(hAlignValues, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getHorizontalAlign();
            }

            public void set(int value)
            {
                dvbtlm = new DVBTextLayoutManager(value, vAlignValues[0], goodOrientValues[0], goodCornerValues[0],
                        wrapValues[0], lineValues[0], letterValues[0], tabValues[0]);
            }
        });
        // Vertical Align values
        doValuesTest(vAlignValues, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getVerticalAlign();
            }

            public void set(int value)
            {
                dvbtlm = new DVBTextLayoutManager(hAlignValues[0], value, goodOrientValues[0], goodCornerValues[0],
                        wrapValues[0], lineValues[0], letterValues[0], tabValues[0]);
            }
        });
        // Orientation values
        doValuesTest(goodOrientValues, badOrientValues, true, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getLineOrientation();
            }

            public void set(int value)
            {
                dvbtlm = new DVBTextLayoutManager(hAlignValues[0], vAlignValues[0], value, goodCornerValues[0],
                        wrapValues[0], lineValues[0], letterValues[0], tabValues[0]);
            }
        });
        // Start corner values
        doValuesTest(goodCornerValues, badCornerValues, true, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getStartCorner();
            }

            public void set(int value)
            {
                dvbtlm = new DVBTextLayoutManager(hAlignValues[0], vAlignValues[0], goodOrientValues[0], value,
                        wrapValues[0], lineValues[0], letterValues[0], tabValues[0]);
            }
        });

        // WrapValues
        doValuesTest(new int[] { 0, 1 }, null, false, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getTextWrapping() ? 1 : 0;
            }

            public void set(int value)
            {
                dvbtlm = new DVBTextLayoutManager(hAlignValues[0], vAlignValues[0], goodOrientValues[0],
                        goodCornerValues[0], value != 0, lineValues[0], letterValues[0], tabValues[0]);
            }
        });
        // LineSpacing Values
        doValuesTest(lineValues, null, false, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getLineSpace();
            }

            public void set(int value)
            {
                dvbtlm = new DVBTextLayoutManager(hAlignValues[0], vAlignValues[0], goodOrientValues[0],
                        goodCornerValues[0], wrapValues[0], value, letterValues[0], tabValues[0]);
            }
        });
        // LetterSpacing Values
        doValuesTest(letterValues, new int[] { Short.MAX_VALUE + 1, Short.MIN_VALUE - 1 }, // out
                                                                                           // of
                                                                                           // 16-bit
                                                                                           // range
                true, new ValueAccess()
                {
                    public int get()
                    {
                        return dvbtlm.getLetterSpace();
                    }

                    public void set(int value)
                    {
                        dvbtlm = new DVBTextLayoutManager(hAlignValues[0], vAlignValues[0], goodOrientValues[0],
                                goodCornerValues[0], wrapValues[0], lineValues[0], value, tabValues[0]);
                    }
                });
        // TabSpacing Values
        doValuesTest(tabValues, null, false, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getHorizontalTabSpacing();
            }

            public void set(int value)
            {
                dvbtlm = new DVBTextLayoutManager(hAlignValues[0], vAlignValues[0], goodOrientValues[0],
                        goodCornerValues[0], wrapValues[0], lineValues[0], letterValues[0], value);
            }
        });
    }

    /**
     * Tests set|getHorizontalAlign(). Defaults tested by constructor tests.
     */
    public void testHorizontalAlign()
    {
        doValuesTest(hAlignValues, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getHorizontalAlign();
            }

            public void set(int value)
            {
                dvbtlm.setHorizontalAlign(value);
            }
        });
    }

    /**
     * Tests set|getVerticalAlign(). Defaults tested by constructor tests.
     */
    public void testVerticalAlign()
    {
        doValuesTest(vAlignValues, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getVerticalAlign();
            }

            public void set(int value)
            {
                dvbtlm.setVerticalAlign(value);
            }
        });
    }

    /**
     * Tests set|getLineOrientation(). Defaults tested by constructor tests.
     */
    public void testLineOrientation()
    {
        doValuesTest(goodOrientValues, badOrientValues, true, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getLineOrientation();
            }

            public void set(int value)
            {
                dvbtlm.setLineOrientation(value);
            }
        });
    }

    /**
     * Tests set|getStartCorner(). Defaults tested by constructor tests.
     */
    public void testStartCorner()
    {
        doValuesTest(goodCornerValues, badCornerValues, true, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getStartCorner();
            }

            public void set(int value)
            {
                dvbtlm.setStartCorner(value);
            }
        });
    }

    /**
     * Tests set|getTextWrapping(). Defaults tested by constructor tests.
     */
    public void testTextWrapping()
    {
        doValuesTest(new int[] { 0, 1 }, null, false, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getTextWrapping() ? 1 : 0;
            }

            public void set(int value)
            {
                dvbtlm.setTextWrapping(value != 0);
            }
        });
    }

    /**
     * Tests set|getLineSpace(). Defaults tested by constructor tests.
     */
    public void testLineSpace()
    {
        doValuesTest(new int[] { -1, 28, 33, 45, 72 }, null, false, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getLineSpace();
            }

            public void set(int value)
            {
                dvbtlm.setLineSpace(value);
            }
        });
    }

    /**
     * Tests set|getLetterSpace(). Defaults tested by constructor tests.
     */
    public void testLetterSpace()
    {
        doValuesTest(new int[] { 0, 2, 50, 100 }, new int[] { Short.MAX_VALUE + 1, Short.MIN_VALUE - 1 }, // outside
                                                                                                          // 16-bit
                                                                                                          // value
                true, new ValueAccess()
                {
                    public int get()
                    {
                        return dvbtlm.getLetterSpace();
                    }

                    public void set(int value)
                    {
                        dvbtlm.setLetterSpace(value);
                    }
                });
    }

    /**
     * Tests set|getHorizontalTabSpacing(). Defaults tested by constructor
     * tests.
     */
    public void testHorizontalTabSpacing()
    {
        doValuesTest(new int[] { 0, 8, 56, 144, 200 }, null, false, new ValueAccess()
        {
            public int get()
            {
                return dvbtlm.getHorizontalTabSpacing();
            }

            public void set(int value)
            {
                dvbtlm.setHorizontalTabSpacing(value);
            }
        });
    }

    private static final String[] hAlignNames = { "HORIZONTAL_START_ALIGN", "HORIZONTAL_END_ALIGN",
            "HORIZONTAL_CENTER", };

    private static final String[] vAlignNames = { "VERTICAL_START_ALIGN", "VERTICAL_END_ALIGN", "VERTICAL_CENTER", };

    private static final String[] orientNames = { "LINE_ORIENTATION_HORIZONTAL", "LINE_ORIENTATION_VERTICAL", };

    private static final String[] cornerNames = { "START_CORNER_LOWER_LEFT", "START_CORNER_LOWER_RIGHT",
            "START_CORNER_UPPER_LEFT", "START_CORNER_UPPER_RIGHT", };

    private static final int[] hAlignValues = { DVBTextLayoutManager.HORIZONTAL_START_ALIGN,
            DVBTextLayoutManager.HORIZONTAL_END_ALIGN, DVBTextLayoutManager.HORIZONTAL_CENTER, };

    private static final int[] vAlignValues = { DVBTextLayoutManager.VERTICAL_START_ALIGN,
            DVBTextLayoutManager.VERTICAL_END_ALIGN, DVBTextLayoutManager.VERTICAL_CENTER, };

    private static final int[] orientValues = { DVBTextLayoutManager.LINE_ORIENTATION_HORIZONTAL,
            DVBTextLayoutManager.LINE_ORIENTATION_VERTICAL, };

    private static final int[] goodOrientValues = { DVBTextLayoutManager.LINE_ORIENTATION_HORIZONTAL,
            DVBTextLayoutManager.LINE_ORIENTATION_VERTICAL, };

    private static final int[] badOrientValues = { DVBTextLayoutManager.START_CORNER_LOWER_LEFT,
            DVBTextLayoutManager.START_CORNER_LOWER_RIGHT, DVBTextLayoutManager.START_CORNER_UPPER_LEFT,
            DVBTextLayoutManager.START_CORNER_UPPER_RIGHT, -1 };

    private static final int[] cornerValues = {
    /* Unsupported */
    DVBTextLayoutManager.START_CORNER_LOWER_LEFT, DVBTextLayoutManager.START_CORNER_LOWER_RIGHT,
            DVBTextLayoutManager.START_CORNER_UPPER_LEFT, DVBTextLayoutManager.START_CORNER_UPPER_RIGHT, };

    private static final int[] goodCornerValues = {
    /* Unsupported */
    DVBTextLayoutManager.START_CORNER_LOWER_LEFT, DVBTextLayoutManager.START_CORNER_LOWER_RIGHT,
            DVBTextLayoutManager.START_CORNER_UPPER_LEFT, DVBTextLayoutManager.START_CORNER_UPPER_RIGHT, };

    private static final int[] badCornerValues = { DVBTextLayoutManager.LINE_ORIENTATION_HORIZONTAL,
            DVBTextLayoutManager.LINE_ORIENTATION_VERTICAL, -1, };

    /**
     * @see #doValuesTest
     */
    private interface ValueAccess
    {
        int get();

        void set(int value);
    }

    /**
     * Used to test a range of values for an API.
     */
    private void doValuesTest(int[] values, ValueAccess access)
    {
        doValuesTest(values, null, true, access);
    }

    /**
     * Used to test a range of values for an API.
     */
    private void doValuesTest(int[] values, int[] badValues, boolean edge, ValueAccess access)
    {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        // Test valid values
        if (values != null)
        {
            for (int i = 0; i < values.length; ++i)
            {
                min = Math.min(min, values[i]);
                max = Math.max(max, values[i]);

                access.set(values[i]);

                assertEquals("Retrieved value should be the set value", values[i], access.get());
                assertEquals("Retrieved value should be the set value (2)", values[i], access.get());
            }
        }

        if (badValues != null)
        {
            for (int i = 0; i < badValues.length; ++i)
            {
                min = Math.min(min, badValues[i]);
                max = Math.max(max, badValues[i]);

                try
                {
                    access.set(badValues[i]);
                    fail("Expected IllegalArgumentException for unsupported value " + badValues[i]);
                }
                catch (IllegalArgumentException e)
                {
                }
            }
        }

        if (edge)
        {
            // Test invalid values
            int good = access.get();
            try
            {
                access.set(min - 1);
                fail("Expected IllegalArgumentException for invalid value (-1)");
            }
            catch (IllegalArgumentException e)
            {
            }
            assertEquals("Expected value to remain unchanged", good, access.get());

            good = access.get();
            try
            {
                access.set(max + 1);
                fail("Expected IllegalArgumentException for invalid value (+1)");
            }
            catch (IllegalArgumentException e)
            {
            }
            assertEquals("Expected value to remain unchanged", good, access.get());
        }
    }

    protected DVBTextLayoutManager dvbtlm;

    protected void setUp() throws Exception
    {
        super.setUp();

        dvbtlm = new DVBTextLayoutManager();
    }

    protected void tearDown() throws Exception
    {
        dvbtlm = null;

        super.tearDown();
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
        TestSuite suite = new TestSuite(DVBTextLayoutManagerTest.class);
        return suite;
    }

    public DVBTextLayoutManagerTest(String name)
    {
        super(name);
    }
}
