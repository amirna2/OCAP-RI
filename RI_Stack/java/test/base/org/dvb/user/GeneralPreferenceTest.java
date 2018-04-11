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

package org.dvb.user;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests GeneralPreference
 * 
 * @author Greg Rutz
 */
public class GeneralPreferenceTest extends TestCase
{
    public void testConstructorsAndOthers() throws Exception
    {
        GeneralPreference pref = null;
        String[] favourites = null;

        // Valid General Preference name test
        boolean exception_thrown = false;
        try
        {
            pref = new GeneralPreference("Invalid Name");
        }
        catch (IllegalArgumentException e)
        {
            exception_thrown = true;
        }
        assertTrue("Exception not thrown with illegal name for General Preference", exception_thrown);

        // Test default constructed empty preference lists
        // Also test getName() method and hasValue() method
        pref = new GeneralPreference("User Language");
        assertTrue("GetMostFavourite should return null", pref.getMostFavourite() == null);
        assertTrue("GetFavourites should return empty array", pref.getFavourites().length == 0);
        assertTrue("hasValue() should return false", !pref.hasValue());
        assertTrue("getName does not return expeected name", pref.getName().equalsIgnoreCase("User Language"));
    }

    public void testAddStringArray() throws Exception
    {
        GeneralPreference pref = new GeneralPreference("User Language");
        String[] favourites = null;
        boolean different;

        // Test add(String[]) method with a new items
        String[] values = { "heb", "fre", "spa", "rus" };
        pref.add(values);
        favourites = pref.getFavourites();
        different = false;
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(values[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add(String[]) with new values", !different
                && favourites.length == values.length);

        // Test add(String[]) method with some item already in list
        String[] valuesToAdd = { "test1", "test2", "fre" };
        pref.add(valuesToAdd);
        favourites = pref.getFavourites();
        different = false;
        String[] new_values = { "heb", "spa", "rus", "test1", "test2", "fre" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add(String[]) with some existing values ",
                !different && favourites.length == new_values.length);

    }

    public void testAddInt() throws Exception
    {
        GeneralPreference pref = new GeneralPreference("User Language");
        String[] values = { "heb", "fre", "spa", "rus" };
        pref.add(values);

        String[] favourites = null;
        boolean different;

        // Test add(int,String) method with a new item
        pref.add(3, "ger");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values1 = { "heb", "fre", "spa", "ger", "rus" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values1[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add(3,'ger')", !different
                && favourites.length == new_values1.length);

        // Test add(int,String) method with item already in list
        pref.add(1, "spa");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values2 = { "heb", "spa", "fre", "ger", "rus" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values2[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add(1,'spa') ", !different
                && favourites.length == new_values2.length);

        // Test with position greater than size of list
        pref.add(8, "spa");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values3 = { "heb", "fre", "ger", "rus", "spa" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values3[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add(8,'spa') ", !different
                && favourites.length == new_values3.length);

        // Test with position less than 0
        pref.add(-1, "swa");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values4 = { "swa", "heb", "fre", "ger", "rus", "spa" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values4[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add(-1,'swa') ", !different
                && favourites.length == new_values4.length);

    }

    public void testAddString() throws Exception
    {
        GeneralPreference pref = new GeneralPreference("User Language");
        String[] values = { "heb", "fre", "spa", "rus" };
        pref.add(values);

        String[] favourites = null;
        boolean different;

        // Test add(String) method with a new item
        pref.add("ger");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values1 = { "heb", "fre", "spa", "rus", "ger" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values1[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add('ger')", !different
                && favourites.length == new_values1.length);

        // Test add(String) method with item already in list
        pref.add("spa");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values2 = { "heb", "fre", "rus", "ger", "spa" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values2[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after add('spa') ", !different
                && favourites.length == new_values2.length);

    }

    public void testRemove() throws Exception
    {
        GeneralPreference pref = new GeneralPreference("User Language");
        String[] values = { "heb", "fre", "spa", "rus" };
        pref.add(values);

        String[] favourites = null;
        boolean different;

        // Test remove(String) method
        pref.remove("heb");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values1 = { "fre", "spa", "rus" };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values1[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after remove('heb')", !different
                && favourites.length == new_values1.length);

        // Test removeAll method
        pref.removeAll();
        favourites = pref.getFavourites();
        assertTrue("New favourites list failed to produce correct list after remove('heb')", !pref.hasValue()
                && favourites.length == 0);
    }

    public void testSetMostFavouriteAndGetPosition() throws Exception
    {
        GeneralPreference pref = new GeneralPreference("User Language");
        String[] values = { "heb", "fre", "spa", "rus" };
        pref.add(values);

        String[] favourites = null;
        boolean different;

        // Test setMostFavourite method
        pref.setMostFavourite("rus");
        favourites = pref.getFavourites();
        different = false;
        String[] new_values1 = { "rus", "heb", "fre", "spa", };
        for (int i = 0; i < favourites.length; ++i)
        {
            if (!favourites[i].equals(new_values1[i]))
            {
                different = true;
                break;
            }
        }
        assertTrue("New favourites list failed to produce correct list after remove('heb')", !different
                && favourites.length == new_values1.length);
        assertTrue("getPosition() did return correct position (1)", pref.getPosition("heb") == 1);

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
        TestSuite suite = new TestSuite(GeneralPreferenceTest.class);
        return suite;
    }

    public GeneralPreferenceTest(String name)
    {
        super(name);
    }
}
