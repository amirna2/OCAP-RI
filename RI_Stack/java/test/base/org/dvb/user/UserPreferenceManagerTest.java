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

import org.cablelabs.test.ProxySecurityManager;

import java.io.File;
import java.security.Permission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.hardware.pod.POD;
import org.ocap.system.MonitorAppPermission;

import org.dvb.application.AppProxyTest.DummySecurityManager;

import org.cablelabs.impl.util.MPEEnv;

/**
 * Tests UserPreferenceManager
 * 
 * @author Todd Earles
 * @author Tom Henriksen
 * @author Greg Rutz
 */
public class UserPreferenceManagerTest extends TestCase
{
    String[] prevFavorites;

    String prevMostFavorite;

    protected void setUp() throws Exception
    {
        super.setUp();
        // Before we modify the system property that indicates the user pref
        // file name,
        // we want to instantiate our singleton instance with the real pref file
        // name.
        UserPreferenceManager.getInstance();

        GeneralPreference userLanguagePref = new GeneralPreference("User Language");
        UserPreferenceManager prefManager = UserPreferenceManager.getInstance();
        prefManager.read(userLanguagePref);
        //
        // save away the previous settings
        //
        prevFavorites = userLanguagePref.getFavourites();
        prevMostFavorite = userLanguagePref.getMostFavourite();

    }

    protected void tearDown() throws Exception
    {
        super.tearDown();

        //
        // restore the previous user language preference
        // 
        GeneralPreference userLanguagePref = new GeneralPreference("User Language");
        UserPreferenceManager prefManager = UserPreferenceManager.getInstance();
        userLanguagePref.add(prevFavorites);
        userLanguagePref.setMostFavourite(prevMostFavorite);
        prefManager.write(userLanguagePref);
    }

    public void testReadWriteDataFile() throws Exception
    {
        // Delete the user prefs data file
        (new File(MPEEnv.getEnv("OCAP.persistent.userprefs"), "userPrefsFile.dat")).delete();

        DummyUserPreferenceManager manager = new DummyUserPreferenceManager();

        // Write out the test file
        GeneralPreference langPref = new GeneralPreference("User Language");
        String[] langList = new String[] { "heb", "eng", "fre", "spa", "rus", "haw" };
        langPref.add(langList);
        manager.write(langPref);

        GeneralPreference parentalRatingPref = new GeneralPreference("Parental Rating");
        String rating = "TVMA N GV SC";
        parentalRatingPref.add(rating);
        manager.write(parentalRatingPref);

        GeneralPreference userNamePref = new GeneralPreference("User Name");
        String userName = "Greg Rutz";
        userNamePref.add(userName);
        manager.write(userNamePref);

        GeneralPreference userAddressPref = new GeneralPreference("User Address");
        String userAddress = "CableLabs, Louisville, CO";
        userAddressPref.add(userAddress);
        manager.write(userAddressPref);

        GeneralPreference userEmailPref = new GeneralPreference("User @");
        String userEmail = "info@cablelabs.org";
        userEmailPref.add(userEmail);
        manager.write(userEmailPref);

        GeneralPreference countryPref = new GeneralPreference("Country Code");
        String country = "USA";
        countryPref.add(country);
        manager.write(countryPref);

        GeneralPreference defaultFontPref = new GeneralPreference("Default Font Size");
        String fontSize = "32";
        defaultFontPref.add(fontSize);
        manager.write(defaultFontPref);

        // Now create a new UserPreferenceManager that will read the file that
        // we just
        // wrote. We will test to see if we wrote the correct items to the file
        DummyUserPreferenceManager manager2 = new DummyUserPreferenceManager();

        manager2.read(langPref);
        for (int i = 0; i < langPref.getFavourites().length; ++i)
        {
            assertEquals("Language preferences incorrect!", true, langPref.getFavourites()[i].equals(langList[i]));
        }

        manager2.read(parentalRatingPref);
        assertEquals("Parental rating preferences incorrect!", true, parentalRatingPref.getMostFavourite().equals(
                rating));

        manager2.read(userNamePref);
        assertEquals("User Name preferences incorrect!", true, userNamePref.getMostFavourite().equals(userName));

        manager2.read(userAddressPref);
        assertEquals("User Address preferences incorrect!", true, userAddressPref.getMostFavourite()
                .equals(userAddress));

        manager2.read(userEmailPref);
        assertEquals("User Email preferences incorrect!", true, userEmailPref.getMostFavourite().equals(userEmail));

        manager2.read(countryPref);
        assertEquals("Country Code preferences incorrect!", true, countryPref.getMostFavourite().equals(country));

        manager2.read(defaultFontPref);
        assertEquals("Default Font Size preferences incorrect!", true, defaultFontPref.getMostFavourite().equals(
                fontSize));
    }

    public void testRead() throws Exception
    {
        UserPreferenceManager manager = UserPreferenceManager.getInstance();
        String[] preferenceList = new String[] { "heb", "eng", "fre", "spa", "rus", "haw" };

        // Note that we're also mixing case. We're not doing an explicit test
        // for that, but it should cause these tests to fail if it is a problem.
        GeneralPreference pref = new GeneralPreference("UsEr LaNgUaGe");
        pref.add(preferenceList);
        manager.write(pref);

        GeneralPreference p = new GeneralPreference("useR languAGE");
        manager.read(p);
        for (int x = 0; x < preferenceList.length; x++)
        {
            assertEquals("One of the preference values should be '" + preferenceList[x], true, containsString(
                    p.getFavourites(), preferenceList[x]));
        }
        assertEquals("The favourites count should be the same length as the list", preferenceList.length,
                p.getFavourites().length);
    }

    public void testUserPreferenceChangeListener() throws Exception
    {
        UserPreferenceManager manager = UserPreferenceManager.getInstance();
        DummyListener listener = new DummyListener();
        String prefName = "USER LANGUAGE";

        GeneralPreference pref = new GeneralPreference(prefName);
        pref.add("aus");

        manager.addUserPreferenceChangeListener(listener);
        synchronized (listener)
        {
            listener.reset();
            manager.write(pref);
            listener.wait(500);
        }

        assertEquals("The listener should have been called", true, listener.notifyCalled);
        assertEquals("The change event should have been for '" + prefName + "'", prefName, listener.preferenceName);

        manager.removeUserPreferenceChangeListener(listener);
        synchronized (listener)
        {
            listener.reset();
            manager.write(pref);
            listener.wait(500);
        }

        assertEquals("The listener should not have been called", false, listener.notifyCalled);
    }

    public void testPODInteraction() throws Exception
    {
        UserPreferenceManager manager = UserPreferenceManager.getInstance();
        POD pod = POD.getInstance();
        byte[] lang = null;

        // As an initial test, simply make sure that a read of the POD language
        // matches a read of the user preference
        GeneralPreference p1 = new GeneralPreference("User Language");
        manager.read(p1);
        lang = pod.getHostParam(0x08);
        assertTrue("Initial language from POD and UserPreference do not match", p1.getMostFavourite().equalsIgnoreCase(
                new String(lang)));

        // Test that updates to UPM are reflected in the POD
        GeneralPreference p2 = new GeneralPreference("User Language");
        String german = "ger";
        p2.setMostFavourite(german);
        manager.write(p2);
        lang = pod.getHostParam(0x08);
        assertTrue("Write to UserPreferenceManager not correctly updating the POD", german.equalsIgnoreCase(new String(
                lang)));

        // Test that updates to POD are reflected in the UPM
        GeneralPreference p3 = new GeneralPreference("User Language");
        String greek = "gre";
        pod.updateHostParam(0x08, greek.getBytes());
        manager.read(p3);
        assertTrue("Write to POD not correctly updating the UserPreferenceManager",
                greek.equalsIgnoreCase(p3.getMostFavourite()));
    }

    private static final String[] STD_PREFS = { "User Language", "Parental Rating", "Default Font Size",
            "Country Code", "User Name", "User Address", "User @" };

    private static final String[] UNPRIVILEGED_PREFS = { "User Language", "Parental Rating", "Default Font Size",
            "Country Code" };

    private static final String[] PRIVILEGED_PREFS = { "User Name", "User Address", "User @" };

    public void testSecurity() throws Exception
    {
        // Install test SecurityManager
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        UserPreferenceManager manager = UserPreferenceManager.getInstance();

        try
        {
            // No permission check for reading UNPRIVILEGED_PREFS
            // Permission required for WRITING UNPRIVILEGED_PREFS
            for (int i = 0; i < UNPRIVILEGED_PREFS.length; ++i)
            {
                GeneralPreference pref = new GeneralPreference(UNPRIVILEGED_PREFS[i]);
                Permission perm;

                // Test read
                sm.p = null;
                sm.all.removeAllElements();
                manager.read(pref);
                perm = findNonMonAppPermission(sm);
                assertSame("Permission should not be required for always-available preferences", null, perm);

                // Test write
                sm.all.removeAllElements();
                manager.write(pref);
                perm = findNonMonAppPermission(sm);
                checkPermission(perm, pref, "write");
            }

            // Permissions required for any access of PRIVILEGED_PREFS
            for (int i = 0; i < PRIVILEGED_PREFS.length; ++i)
            {
                GeneralPreference pref = new GeneralPreference(PRIVILEGED_PREFS[i]);
                Permission perm;

                // Test read
                sm.p = null;
                sm.all.removeAllElements();
                manager.read(pref);
                perm = findNonMonAppPermission(sm);
                checkPermission(perm, pref, "read");

                // Test write
                sm.p = null;
                sm.all.removeAllElements();
                manager.write(pref);
                perm = findNonMonAppPermission(sm);
                checkPermission(perm, pref, "write");
            }
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    // checkPermission() may be called multiple times when reading
    // or writing a pref if any POD-related operations are involved.
    // Since DummySecurityManager.p only references the first
    // Permission object passed in to checkPermission(), we look for
    // the first Permission that isn't an instance of
    // MonitorAppPermission("podApplication").
    private Permission findNonMonAppPermission(DummySecurityManager sm)
    {
        Permission permission = null;

        for (int i = 0; i < sm.all.size(); i++)
        {
            if (!(sm.all.elementAt(i) instanceof MonitorAppPermission))
            {
                permission = (Permission) sm.all.elementAt(i);
                break;
            }
        }
        return permission;
    }

    private void checkPermission(Permission p, GeneralPreference pref, String action)
    {
        assertNotNull("Permission check required for " + pref.getName(), p);
        assertTrue("Expected instanceof UserPreferencePermission", p instanceof UserPreferencePermission);
        assertEquals("Unexpected name", action, p.getName());
    }

    private boolean containsString(String[] array, String search)
    {
        for (int i = 0; i < array.length; i++)
        {
            if (array[i].equals(search)) return true;
        }
        return false;
    }

    public void testReadFacility() throws Exception
    {
        String[][] facilityList = { new String[] { "eng", "chi", "spa" }, new String[] { "fre", "rus" },
                new String[] { "haw", "eng", "spa", "heb" }, new String[] { "fre" }, new String[] { "rus", "haw" }, };
        String[] preferenceList = new String[] { "heb", "eng", "fre", "spa", "rus", "haw" };
        UserPreferenceManager manager = UserPreferenceManager.getInstance();

        for (int y = 0, count = 0; y < facilityList.length; y++, count = 0)
        {
            // Note that we're also mixing case. We're not doing an explicit
            // test
            // for that, but it should cause these tests to fail if it is a
            // problem.
            Facility facility = new Facility("user LanGuaGe", facilityList[y]);
            GeneralPreference pref = new GeneralPreference("user LANGUAGE");

            pref.add(preferenceList);
            manager.write(pref);

            GeneralPreference p = new GeneralPreference("User languAGE");
            manager.read(p, facility);

            for (int x = 0; x < preferenceList.length; x++)
            {
                if (containsString(facilityList[y], preferenceList[x]))
                {
                    count++;
                    assertEquals("One of the preference values should be '" + preferenceList[x] + "' for list " + y,
                            true, containsString(p.getFavourites(), preferenceList[x]));
                }
                else
                    assertEquals(
                            "One of the preference values should not be '" + preferenceList[x] + "' for list " + y,
                            false, containsString(p.getFavourites(), preferenceList[x]));
            }
            assertEquals("The allowed preference count should be " + count + " for list " + y, count,
                    p.getFavourites().length);
        }
    }

    // TODO (TomH) Finish Write Test
    public void testWrite() throws Exception
    {
        UserPreferenceManager manager = UserPreferenceManager.getInstance();

        GeneralPreference pref = new GeneralPreference("User Language");
        pref.add("eng");
        pref.add(new String[] { "spa", "fre" });
        manager.write(pref);
        GeneralPreference p = new GeneralPreference("user Language");
        manager.read(p);
        assertTrue("Preferred language should be 'eng' for English", p.getMostFavourite().equals("eng"));
        pref.setMostFavourite("fre");
        manager.write(pref);
        manager.read(p);
        assertTrue("Preferred language should be 'fre' for French", p.getMostFavourite().equals("fre"));
    }

    class DummyUserPreferenceManager extends UserPreferenceManager
    {
        public DummyUserPreferenceManager()
        {
            super();
        }
    }

    class DummyListener implements UserPreferenceChangeListener
    {
        public boolean notifyCalled = false;

        public String preferenceName = "";

        public void reset()
        {
            notifyCalled = false;
            preferenceName = "";
        }

        public synchronized void receiveUserPreferenceChangeEvent(UserPreferenceChangeEvent e)
        {
            notifyCalled = true;
            preferenceName = e.getName();
            notifyAll();
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
        TestSuite suite = new TestSuite(UserPreferenceManagerTest.class);
        return suite;
    }

    public UserPreferenceManagerTest(String name)
    {
        super(name);
    }
}
