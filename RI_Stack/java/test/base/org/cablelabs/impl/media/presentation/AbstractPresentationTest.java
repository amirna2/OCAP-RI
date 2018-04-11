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
package org.cablelabs.impl.media.presentation;

import org.cablelabs.impl.media.JMFTests;

import junit.framework.TestCase;

/**
 * AbstractPresentationTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class AbstractPresentationTest extends TestCase
{

    private CannedAbstractPresentation pres;

    private CannedPresentationContext context;

    public AbstractPresentationTest()
    {
        super("AbstractPresentationTest");
    }

    public AbstractPresentationTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(AbstractPresentationTest.class);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        JMFTests.setUpJMF();

        context = new CannedPresentationContext();
        pres = new CannedAbstractPresentation(context);
    }

    public void tearDown() throws Exception
    {
        pres = null;
        context = null;

        JMFTests.tearDownJMF();

        super.tearDown();
    }

    // Test Section

    public void testStart() throws Exception
    {
        // Regular start
        pres.start();
        assertTrue("Presentation should be started", pres.isPresenting());

        // Cause a failure
        try
        {
            pres.start();
            fail("Expected AssertionFailureException when starting twice");
        }
        catch (Exception expected)
        {
        }
    }

    public void testStop() throws Exception
    {
        // First we'll call stop when it's already stopped
        pres.stop();

        // Now let's start it up and then call stop
        pres.start();
        assertTrue("Presentation should be started", pres.isPresenting());
        pres.stop();
        assertFalse("Presentation should be stopped", pres.isPresenting());

        // Now throw an exception (for code coverage sake)
        pres.throwStopException = true;
        pres.start();
        assertTrue("Presentation should be started", pres.isPresenting());
        pres.stop();
        assertFalse("Presentation should be stopped", pres.isPresenting());
    }

    public void testSetAndGetRate() throws Exception
    {
        assertEquals("Rate doesn't match", 1.0f, pres.getRate(), 0.001f);
        float newRate = 3.0f;
        assertEquals("Rate doesn't match", newRate, pres.setRate(newRate), 0.001f);
        assertEquals("Rate doesn't match", newRate, pres.getRate(), 0.001f);

        // Now we'll set with it started
        pres.start();
        newRate = 2.0f;
        assertEquals("Rate doesn't match", newRate, pres.setRate(newRate), 0.001f);
        assertEquals("Rate doesn't match", newRate, pres.getRate(), 0.001f);
    }

    public void testConstructor() throws Exception
    {
        // Success
        pres = new CannedAbstractPresentation(context);

        try
        {
            pres = new CannedAbstractPresentation(null);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException expected)
        {
        }
    }

}
