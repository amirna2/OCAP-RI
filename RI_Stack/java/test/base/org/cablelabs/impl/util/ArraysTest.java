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

package org.cablelabs.impl.util;

import java.util.List;

import javax.media.MediaLocator;

import junit.framework.TestCase;

public class ArraysTest extends TestCase
{
    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(ArraysTest.class);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testCopyNullArrays()
    {
        assertNull(Arrays.copy((int[]) null));
        assertNull(Arrays.copy((short[]) null));
        assertNull(Arrays.copy((Object[]) null, Object.class));
    }

    public void testCopyIntArray()
    {
        int[] a1 = { 9, 2, 7 };
        int[] a2 = Arrays.copy(a1);
        assertNotNull(a2);
        assertEquals(a1.length, a2.length);
        for (int i = 0; i < a1.length; ++i)
        {
            boolean found = false;
            for (int j = 0; !found && j < a2.length; ++j)
                if (a2[j] == a1[i]) found = true;
            assertTrue("could not find " + a1[i] + " in array a2", found);
        }
    }

    public void testCopyShortArray()
    {
        short[] a1 = { 9, 2, 7 };
        short[] a2 = Arrays.copy(a1);
        assertNotNull(a2);
        assertEquals(a1.length, a2.length);
        for (int i = 0; i < a1.length; ++i)
        {
            boolean found = false;
            for (int j = 0; !found && j < a2.length; ++j)
                if (a2[j] == a1[i]) found = true;
            assertTrue("could not find " + a1[i] + " in array a2", found);
        }
    }

    public void testCopyObjectsWithoutEqualsOverriddenArray()
    {
        MediaLocator[] a1 = { new MediaLocator("one"), new MediaLocator("two"), new MediaLocator("three"), };
        MediaLocator[] a2 = (MediaLocator[]) Arrays.copy(a1, MediaLocator.class);
        assertNotNull(a2);
        assertEquals(a1.length, a2.length);
        List a2List = java.util.Arrays.asList(a2);
        for (int i = 0; i < a1.length; ++i)
        {
            assertTrue("a2 does not contain " + a1[i], a2List.contains(a1[i]));
        }
    }

    public void testCopyObjectWithEqualsOverriddenArray()
    {
        String[] a1 = { new String("one"), new String("two"), new String("three"), };
        String[] a2 = (String[]) Arrays.copy(a1, String.class);
        assertNotNull(a2);
        assertEquals(a1.length, a2.length);
        List a2List = java.util.Arrays.asList(a2);
        for (int i = 0; i < a1.length; ++i)
        {
            assertTrue("a2 does not contain " + a1[i], a2List.contains(a1[i]));
        }
    }

    private static class Foo extends Object
    {
    }

    public void testCopyToSubtype()
    {
        Object[] objects = { new Foo(), new Foo(), new Foo() };
        Foo[] foos = (Foo[]) Arrays.copy(objects, Foo.class);
        assertNotNull(foos);
        assertEquals(objects.length, foos.length);
    }

    public void testCopyEmptyObjectArray()
    {
        Object[] a = (Object[]) Arrays.copy(new Object[] {}, Object.class);
        assertNotNull(a);
        assertEquals(0, a.length);
    }
}
