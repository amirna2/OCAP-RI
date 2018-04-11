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

package org.cablelabs.impl.ocap.hardware.frontpanel;

import junit.framework.*;
import org.ocap.hardware.frontpanel.ScrollSpec;
import org.cablelabs.impl.ocap.hardware.frontpanel.ScrollSpecImpl;

public class ScrollSpecTest extends TestCase
{
    public void testGetSet()
    {
        ScrollSpec scrollSpec1 = new ScrollSpecImpl(40, 20, 20, -1, 25, 4, 1);
        ScrollSpec scrollSpec2 = new ScrollSpecImpl(40, 20, 0, 10, 25, 4, 2);

        assertEquals("MaxHorizontalIterations not correct", scrollSpec1.getMaxHorizontalIterations(), 40);
        assertEquals("HorizontalIterations not correct", scrollSpec1.getHorizontalIterations(), 20);

        assertEquals("MaxVerticalIterations not correct", scrollSpec2.getMaxVerticalIterations(), 20);
        assertEquals("VerticalIterations not correct", scrollSpec2.getVerticalIterations(), 10);

        scrollSpec2.setHorizontalIterations(25);
        assertEquals("HorizontalIterations not correct", scrollSpec2.getHorizontalIterations(), 25);
        assertEquals("VerticalScrolling should have been disabled", scrollSpec2.getVerticalIterations(), 0);

        scrollSpec2.setVerticalIterations(15);
        assertEquals("VerticalIterations not correct", scrollSpec2.getVerticalIterations(), 15);
        assertEquals("HorizontalScrolling should have been disabled", scrollSpec2.getHorizontalIterations(), 0);

        scrollSpec2.setHoldDuration(40);
        assertEquals("HoldDuration not correct", scrollSpec2.getHoldDuration(), 40);
    }

    public void testIllegalValues()
    {
        ScrollSpec scrollSpec1 = new ScrollSpecImpl(40, 20, 20, -1, 25, 4, 1);
        ScrollSpec scrollSpec2 = new ScrollSpecImpl(40, 20, 0, 10, 25, 4, 2);

        try
        {
            scrollSpec1.setHorizontalIterations(-1);
            fail("scrollSpec1.setHorizontalIterations(-1) should have thrown exception");
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            scrollSpec1.setVerticalIterations(80);
            fail("scrollSpec1.setVerticalIterations(80) should have thrown exception");
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            scrollSpec1.setHoldDuration(-1);
            fail("scrollSpec1.setHoldDuration(-1) should have thrown exception");
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            scrollSpec2.setHoldDuration(60);
            fail("scrollSpec2.setHoldDuration(60) should have thrown exception");
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            scrollSpec1.setHoldDuration(30);
            fail("scrollSpec1.setHoldDuration(30) should have thrown exception");
        }
        catch (IllegalArgumentException e)
        {
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
        TestSuite suite = new TestSuite(ScrollSpecTest.class);
        return suite;
    }
}
