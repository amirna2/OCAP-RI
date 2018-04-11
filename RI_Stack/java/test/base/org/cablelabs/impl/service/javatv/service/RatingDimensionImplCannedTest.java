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
package org.cablelabs.impl.service.javatv.service;

import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.cablelabs.impl.manager.service.CannedSIDatabase.RatingDimensionHandleImpl;
import org.cablelabs.impl.util.string.MultiString;
import org.cablelabs.test.SICannedConcreteTest;

/**
 * <p>
 * CannedRatingDimensionExtTest is an interface test for testing the
 * RatingDimensionExt interface using canned data and behavior from the canned
 * testing environment. Since RatingDimensionExt consists of only a handful of
 * getter methods, there will not be a need for extensive description of these
 * tests. Let it be known, however, that these methods are thoroughly tested for
 * proper functionality.
 * </p>
 * 
 * @author Joshua Keplinger
 */
public class RatingDimensionImplCannedTest extends SICannedConcreteTest
{

    /**
     * Main method, allows this test to be run stand-alone.
     * 
     * @param args
     *            Arguments to be passed to the main method (ignored)
     */
    public static void main(String[] args)
    {
        try
        {
            TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    // Setup Section \\

    /**
     * No-arg constructor for creating our test case.
     * 
     */
    public RatingDimensionImplCannedTest()
    {
        super("RatingDimensionImplCannedTest");
    }

    /**
     * Creates a test suite to for use in the testing environment.
     * 
     * @return An InterfaceTestSuite that contains the tests in this test case.
     */
    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(RatingDimensionImplCannedTest.class);
        suite.setName(RatingDimensionImpl.class.getName());
        return suite;
    }

    /**
     * Sets up our test
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        handle = new RatingDimensionHandleImpl(10);
        name = new MultiString(new String[] { "eng" }, new String[] { "MPAA" });
        levels = 3;
        description = new MultiString[][] {
                { new MultiString(new String[] { "eng" }, new String[] { "R" }),
                        new MultiString(new String[] { "eng" }, new String[] { "Restricted" }), },
                { new MultiString(new String[] { "eng" }, new String[] { "PG" }),
                        new MultiString(new String[] { "eng" }, new String[] { "Parental Guidance" }), },
                { new MultiString(new String[] { "eng" }, new String[] { "G" }),
                        new MultiString(new String[] { "eng" }, new String[] { "General Admission" }), } };
        dimension = new RatingDimensionImpl(sic, handle, name, levels, description, null);
    }

    /**
     * Clean up after the test
     */
    protected void tearDown() throws Exception
    {
        dimension = null;
        handle = null;
        name = null;
        description = null;

        super.tearDown();
    }

    // Test Section \\

    /**
     * Tests the constructor and the associated getter methods for proper
     * setting and retrieval of data.
     */
    public void testConstructor() throws Exception
    {
        assertEquals("Handle does not match.", handle, dimension.getRatingDimensionHandle());
        assertEquals("Name does not match", name.getValue(null), dimension.getDimensionName());
        assertEquals("Number of levels does not match", levels, dimension.getNumberOfLevels());
        assertEquals("Short description does not match", description[0][0].getValue(null),
                dimension.getRatingLevelDescription((short) 0)[0]);
        assertEquals("Long description does not match", description[0][1].getValue(null),
                dimension.getRatingLevelDescription((short) 0)[1]);
    }

    // Data Section \\

    /**
     * Holds the instance of the RatingDimensionExt object we are testing.
     */
    private RatingDimensionImpl dimension;

    private RatingDimensionHandleImpl handle;

    private MultiString name;

    private short levels;

    private MultiString[][] description;

}
