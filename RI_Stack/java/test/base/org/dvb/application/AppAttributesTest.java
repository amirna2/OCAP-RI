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

package org.dvb.application;

import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.test.iftc.InterfaceTestSuite;

import java.util.Enumeration;
import java.util.Hashtable;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;

import org.davic.net.Locator;
import org.dvb.user.Facility;
import org.dvb.user.GeneralPreference;
import org.dvb.user.Preference;
import org.dvb.user.UserPreferenceManager;
import org.ocap.application.OcapAppAttributes;

/*
 * How can we test this interface?
 * 1) Install an app and check values.
 * 2) Make it an interface test and use it to test implementations.
 *    But how do we verify the information????
 *
 * It is currently set up as an interface test...
 * And if we go with (1) it might make sense to maintain an
 * inner class for an interface test... to test the implementation
 * returned by the AppsDatabase for the installed app.
 */

/**
 * Tests the AppAttributes interface.
 */
public class AppAttributesTest extends InterfaceTestCase
{
    /**
     * Tests getType().
     */
    public void testGetType() throws Exception
    {
        assertEquals("Only DVB_J apps should be supported", AppAttributes.DVB_J_application, appattributes.getType());
    }

    /**
     * Tests getName().
     */
    public void testGetName() throws Exception
    {
        if (attrFactory.getNames().size() == 0)
        {
            assertNull("Expected null name", appattributes.getName());
            return;
        }

        // Based upon preferences...
        UserPreferenceManager upm = UserPreferenceManager.getInstance();
        Preference pref = new GeneralPreference("User Language");

        String[][] names = appattributes.getNames();
        String[] langs = new String[names.length];
        for (int i = 0; i < names.length; ++i)
            langs[i] = names[i][0];

        Facility f = new Facility("User Language", langs);
        upm.read(pref, f);

        String lang = pref.getMostFavourite();
        if (lang != null)
        {
            assertNotNull("Expected non-null name", appattributes.getName());
            assertEquals("Unexpected default name", appattributes.getName(lang), appattributes.getName());
        }
        else if (names.length != 0)
        {
            assertNotNull("Expected non-null name", appattributes.getName());
        }
    }

    /**
     * Tests getName().
     */
    public void testGetName_Iso() throws Exception
    {
        Hashtable names = attrFactory.getNames();

        for (Enumeration langs = names.keys(); langs.hasMoreElements();)
        {
            String lang = (String) langs.nextElement();
            assertEquals("Unexpected name for lang: " + lang, names.get(lang), appattributes.getName(lang));
        }
    }

    /**
     * Tests GetNames ().
     */
    public void testGetNames() throws Exception
    {
        Hashtable names = (Hashtable) attrFactory.getNames().clone();

        String[][] namesArray = appattributes.getNames();

        assertEquals("Unexpected number of names", names.size(), namesArray.length);

        for (int i = 0; i < namesArray.length; ++i)
        {
            assertNotNull("Null name entry", namesArray[i]);
            assertEquals("Unexpected length of name entry", 2, namesArray[i].length);

            assertEquals("Unexpected name for lang: " + namesArray[i][0], names.get(namesArray[i][0]), namesArray[i][1]);
            names.remove(namesArray[i][0]);
        }
        assertEquals("Some names were missing", 0, names.size());
    }

    /**
     * Tests getProfiles().
     */
    public void testGetProfiles() throws Exception
    {
        fail("Unimplemented test");
    }

    /**
     * Tests getVersions().
     */
    public void XtestGetVersions() throws Exception
    {
        fail("Unimplemented test");
        // TODO testGetVersions
    }

    /**
     * Tests GetIsServiceBound ().
     */
    public void testGetIsServiceBound() throws Exception
    {
        assertEquals("Unexpected value for serviceBound", attrFactory.getServiceBound(),
                appattributes.getIsServiceBound());
    }

    /**
     * Tests IsStartable ().
     */
    public void testIsStartable() throws Exception
    {
        boolean startable;
        switch (attrFactory.getControlCode())
        {
            case OcapAppAttributes.PRESENT:
            case OcapAppAttributes.AUTOSTART:
                startable = true;
                break;
            default:
                startable = false;
                break;
        }

        assertEquals("Unexpected value for startable", startable, appattributes.isStartable());
    }

    /**
     * Tests isVisible().
     */
    public void testIsVisible() throws Exception
    {
        boolean visible;
        switch (attrFactory.getVisibility())
        {
            case AppEntry.NON_VISIBLE:
            default:
                visible = false;
                break;
            case AppEntry.VISIBLE:
            case AppEntry.LISTING_ONLY:
                visible = true;
                break;
        }
        assertEquals("Unexpected value for isVisible", visible, appattributes.isVisible());
    }

    /**
     * Tests GetIdentifier ().
     */
    public void testGetIdentifier() throws Exception
    {
        AppID expected = attrFactory.getAppID();
        AppID actual = appattributes.getIdentifier();

        assertNotNull("Expected non-null AppID", actual);
        assertEquals("Unexpected OID", expected.getOID(), actual.getOID());
        assertEquals("Unexpected AID", expected.getAID(), actual.getAID());
    }

    /**
     * Tests GetAppIcon ().
     */
    public void XtestGetAppIcon() throws Exception
    {
        fail("Unimplemented test");
        // TODO testGetAppIcon
    }

    /**
     * Tests getPriority().
     */
    public void testGetPriority() throws Exception
    {
        assertEquals("unexpected app priority", attrFactory.getPriority(), appattributes.getPriority());
    }

    /**
     * Tests getServiceLocator().
     */
    public void testGetServiceLocator() throws Exception
    {
        assertEquals("unexpected service locator", attrFactory.getService(), appattributes.getServiceLocator());
    }

    /**
     * Tests GetProperty ().
     */
    public void testGetProperty() throws Exception
    {
        // Verify the following properties
        // "dvb.j.location.base",
        // "dvb.j.location.cpath.extension",
        // "dvb.transport.oc.component.tag",

        checkProperty("dvb.j.location.base", attrFactory.getLocationBase());

        String[] path = attrFactory.getClassPath();
        String[] actualPath = (String[]) appattributes.getProperty("dvb.j.location.cpath.extension");
        if (path == null)
            assertTrue("Expected null or empty classpath", actualPath == null || actualPath.length == 0);
        else
        {
            assertEquals("Unexpected cpath length", path.length, actualPath.length);
            for (int i = 0; i < path.length; ++i)
                assertEquals("Unexpected cpath entry: " + i, path[i], actualPath[i]);
        }

        int tag = attrFactory.getComponentTag();
        checkProperty("dvb.transport.oc.component.tag", new Integer(tag));
    }

    protected void checkProperty(String name, Object value)
    {
        assertEquals("Unexpected property value: " + name, value, appattributes.getProperty(name));
    }

    public static interface AppAttrFactory extends ImplFactory
    {
        // TODO: add interfaces to access expected values

        AppID getAppID();

        Hashtable getNames();

        boolean getServiceBound();

        int getVisibility();

        int getControlCode();

        int getPriority();

        Locator getService();

        String getLocationBase();

        String[] getClassPath();

        int getComponentTag(); // -1 means undefined
    }

    protected AppAttrFactory attrFactory;

    protected AppAttributesTest(String name, Class testedClass, ImplFactory f)
    {
        super(name, testedClass, f);
        attrFactory = (AppAttrFactory) f;
        setUseClassInName(true);
    }

    public AppAttributesTest(String name, ImplFactory f)
    {
        this(name, AppAttributes.class, f);
    }

    protected AppAttributes createAppAttributes()
    {
        return (AppAttributes) createImplObject();
    }

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(AppAttributesTest.class);
        suite.setName(AppAttributes.class.getName());
        return suite;
    }

    protected AppAttributes appattributes;

    protected void setUp() throws Exception
    {
        super.setUp();
        appattributes = createAppAttributes();
    }

    protected void tearDown() throws Exception
    {
        appattributes = null;
        super.tearDown();
    }
}
