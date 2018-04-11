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

package org.cablelabs.xlet.UserPrefs;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.*;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.event.UserEvent;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.ui.event.OCRcEvent;
import org.dvb.application.*;

import org.dvb.user.UserPreferenceChangeListener;
import org.dvb.user.UserPreferenceChangeEvent;
import org.dvb.user.UserPreferenceManager;
import org.dvb.user.GeneralPreference;
import java.lang.SecurityException;
import java.lang.IllegalArgumentException;
import org.dvb.user.UnsupportedPreferenceException;

import org.dvb.user.UserPreferencePermission;

import java.io.IOException;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.ArgParser;

import org.cablelabs.test.autoxlet.*;

public class UserPrefs extends Container implements Xlet, KeyListener, UserPreferenceChangeListener, Driveable
{

    // The OCAP Xlet context.
    private javax.tv.xlet.XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    private String m_appName = "UserPrefs";

    private int m_appId;

    private static int m_signedAppId = 0x3FFF;

    private UserPreferenceManager userPrefMgr;

    // unsigned permissions
    private GeneralPreference userLangPref = null;

    private GeneralPreference parentalRatingPref = null;

    private GeneralPreference countryCodePref = null;

    private GeneralPreference fontSizePref = null;

    // signed permissions with PRF
    private GeneralPreference userEmailPref = null;

    private GeneralPreference userAddrPref = null;

    private GeneralPreference userNamePref = null;

    // keep track of the current expected preferences from STB
    private GeneralPreference expUserLangPref = null;

    private GeneralPreference expParentalRatingPref = null;

    private GeneralPreference expCountryCodePref = null;

    private GeneralPreference expFontSizePref = null;

    private GeneralPreference expUserEmailPref = null;

    private GeneralPreference expUserAddrPref = null;

    private GeneralPreference expUserNamePref = null;

    private boolean readPermission = false;

    private boolean writePermission = false;

    private boolean isSignedApp = false;

    // default built-in preferences,
    private String m_lang = "eng"; // http://en.wikipedia.org/wiki/List_of_ISO_639_codes

    // (3 letter code)
    private String m_country = "US"; // http://en.wikipedia.org/wiki/ISO_3166-1

    // (2 letter code)
    private String m_fontSize = "26"; // 26 is default

    private String m_parentalRating = "PG13";

    private String m_email = "jane.doe@cablecompany.net";

    private String m_address = "1234 Main St., Springfield XX 12345";

    private String m_name = "Jane Doe";

    // default hostapp.properties prefernces in case user does not
    // set them in hostapp.properties
    private String m_hostLang = "chi";

    private String m_hostCountry = "zh";

    private String m_hostParentalRating = "G";

    private String m_hostFontSize = "24";

    private String m_hostEmail = "john.doe@abcd.com";

    private String m_hostAddress = "9876 Abcd Cir., ABCD AB 98765";

    private String m_hostName = "John Doe";

    private String m_currentLang = "eng";

    private static String SECTION_DIVIDER = "==================================";

    // autoXlet
    private AutoXletClient m_axc = null;

    private static Logger m_log = null;

    private static Test m_test = null;

    private static Monitor m_eventMonitor = null;

    /**
     * Initializes the OCAP Xlet.
     * 
     * @param The
     *            context for this Xlet is passed in. A reference to the context
     *            is stored for further need. This is the place where any
     *            initialisation should be done, unless it takes a lot of time
     *            or resources.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialised.
     */
    public void initXlet(javax.tv.xlet.XletContext ctx) throws javax.tv.xlet.XletStateChangeException
    {
        System.out.println("[" + m_appName + "] : initXlet() - begin");

        // initialize AutoXlet
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
        }
        m_eventMonitor = new Monitor(); // used by event dispatcher

        // store off our xlet context
        m_ctx = ctx;

        // Setup the application graphical user interface.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 50, 530, 370, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        m_appId = Integer.parseInt((String) m_ctx.getXletProperty("dvb.app.id"), 16);
        int orgId = Integer.parseInt((String) m_ctx.getXletProperty("dvb.org.id"), 16);
        AppID appID = new AppID(orgId, m_appId);
        m_appName = AppsDatabase.getAppsDatabase().getAppAttributes(appID).getName();

        // read hostapp.properties to get user preferences
        ArgParser args;
        try
        {
            args = new ArgParser((String[]) m_ctx.getXletProperty(XletContext.ARGS));
        }
        catch (IOException e)
        {
            throw new XletStateChangeException("Error creating ArgParser!");
        }

        try
        {
            m_hostLang = args.getStringArg("language");
        }
        catch (Exception e)
        {
            debugLog("use default value for language");
        }
        try
        {
            m_hostParentalRating = args.getStringArg("parentalCtrl");
        }
        catch (Exception e)
        {
            debugLog("use default value for parental control");
        }
        try
        {
            m_hostCountry = args.getStringArg("country");
        }
        catch (Exception e)
        {
            debugLog("use default value for country");
        }
        try
        {
            m_hostFontSize = args.getStringArg("font");
        }
        catch (Exception e)
        {
            debugLog("use default value for font size");
        }
        try
        {
            m_hostName = args.getStringArg("name");
        }
        catch (Exception e)
        {
            debugLog("use default value for name");
        }
        try
        {
            m_hostEmail = args.getStringArg("email");
        }
        catch (Exception e)
        {
            debugLog("use default value for email");
        }
        try
        {
            m_hostAddress = args.getStringArg("address");
        }
        catch (Exception e)
        {
            debugLog("use default value for address");
        }

        debugLog(" initXlet() - end");
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        debugLog(" startXlet() - begin");

        if (m_appId > m_signedAppId)
        {
            isSignedApp = true;
            debugLog("app " + m_appName + " (" + m_appId + ") is a signed app");
        }
        else
        {
            isSignedApp = false;
            debugLog("app " + m_appName + " (" + m_appId + ") is an unsigned app");
        }

        check_Permissions();

        try
        {
            userLangPref = new GeneralPreference("USER LANGUAGE");
            parentalRatingPref = new GeneralPreference("Parental Rating");
            countryCodePref = new GeneralPreference("country code");
            fontSizePref = new GeneralPreference("dEFAULT fONT sIZE");
            userEmailPref = new GeneralPreference("USER @");
            userAddrPref = new GeneralPreference("user aDdReSs");
            userNamePref = new GeneralPreference("User NAME");
        }
        catch (IllegalArgumentException iaex)
        {
            m_test.fail("UserPrefs::startXlet() - caught an IllegalArgumentException when trying to define preferences. "
                    + iaex.getMessage());
            debugLog(" startXlet() - caught an IllegalArgumentException");
            iaex.printStackTrace();
        }
        catch (Exception x)
        {
            m_test.fail("UserPrefs::startXlet() - caught an Exception when trying to define preferences. "
                    + x.getMessage());
            debugLog(" startXlet() - caught an exception");
            x.printStackTrace();
        }

        printTestList();

        // get an instance of the UserPreferenceManager
        // read in preferences from env/prefs/userPrefsFile.dat,
        // if the file exists
        userPrefMgr = UserPreferenceManager.getInstance();
        userPrefMgr.addUserPreferenceChangeListener(this);

        // if the file exists, the hashtable in userPrefMgr will contain
        // whatever values are in the file
        read_UserPreferences(false); // transfer data from hashfile to
        // our preference objects

        m_currentLang = userLangPref.getMostFavourite();

        expUserLangPref = userLangPref;
        expParentalRatingPref = parentalRatingPref;
        expCountryCodePref = countryCodePref;
        expFontSizePref = fontSizePref;
        expUserEmailPref = userEmailPref;
        expUserAddrPref = userAddrPref;
        expUserNamePref = userNamePref;

        display_UserPreferences();

        // Display the application.
        m_scene.show();
        m_scene.requestFocus();

        debugLog(" startXlet() - end");
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean forced) throws javax.tv.xlet.XletStateChangeException
    {
        m_scene.setVisible(false);

        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    // /////////////////////////////////////////////////////////////////////////
    // User Preference methods
    // /////////////////////////////////////////////////////////////////////////
    private void check_Permissions()
    {
        debugLog(" check_Permissions() - begin");
        if (isSignedApp)
        {
            try
            {
                SecurityManager security = System.getSecurityManager();
                if (security != null)
                {
                    security.checkPermission(new UserPreferencePermission("read"));
                    readPermission = true;
                }
            }
            catch (SecurityException se)
            {
                debugLog("read permission denied");
            }

            try
            {
                SecurityManager security = System.getSecurityManager();
                if (security != null)
                {
                    security.checkPermission(new UserPreferencePermission("write"));
                    writePermission = true;
                }
            }
            catch (SecurityException se)
            {
                debugLog("write permission denied");
            }

            debugLog("signed app " + m_appName + " (" + m_appId + ") " + (readPermission ? "has" : "doesn't have")
                    + " read permission");
            debugLog("signed app " + m_appName + " (" + m_appId + ") " + (readPermission ? "has" : "doesn't have")
                    + " write permission");
        }
        debugLog(" check_Permissions() - end");
    }

    private void reinit_UserPreferences()
    {
        debugLog(" reinit() - begin");

        userLangPref.removeAll();
        parentalRatingPref.removeAll();
        countryCodePref.removeAll();
        fontSizePref.removeAll();
        if (isSignedApp)
        {
            userNamePref.removeAll();
            userAddrPref.removeAll();
            userEmailPref.removeAll();
        }

        expUserLangPref = new GeneralPreference("USER LANGUAGE");
        expParentalRatingPref = new GeneralPreference("Parental Rating");
        expCountryCodePref = new GeneralPreference("country code");
        expFontSizePref = new GeneralPreference("dEFAULT fONT sIZE");
        expUserEmailPref = new GeneralPreference("USER @");
        expUserAddrPref = new GeneralPreference("user aDdReSs");
        expUserNamePref = new GeneralPreference("User NAME");

        print(SECTION_DIVIDER);
        print("All User Preferences have been cleared");

        testAllPrefs();

        debugLog(" reinit() - begin");
    }

    private void display_UserPreferences()
    {
        debugLog("UserPrefs::display_UserPreferences() - begin");

        print(SECTION_DIVIDER);

        if (userLangPref != null)
        {
            if (userLangPref.hasValue())
            {
                String[] favs = userLangPref.getFavourites();
                print("lang is " + userLangPref.getName() + "\t most fav: " + userLangPref.getMostFavourite());
                for (int i = 0; i < favs.length; i++)
                {
                    print("\t fav " + i + "=" + favs[i]);
                }
            }
            else
            {
                print("lang is undefined");
            }
        }

        if (parentalRatingPref != null)
        {
            if (parentalRatingPref.hasValue())
            {
                String[] favs = parentalRatingPref.getFavourites();
                print("ParentalRating is " + parentalRatingPref.getName() + "\t most fav: "
                        + parentalRatingPref.getMostFavourite());
                for (int i = 0; i < favs.length; i++)
                {
                    print("\t fav " + i + "=" + favs[i]);
                }
            }
            else
            {
                print("parental rating is undefined");
            }
        }

        if (countryCodePref != null)
        {
            if (countryCodePref.hasValue())
            {
                String[] favs = countryCodePref.getFavourites();
                print("country code is " + countryCodePref.toString() + "\t most fav: "
                        + countryCodePref.getMostFavourite());
                for (int i = 0; i < favs.length; i++)
                {
                    print("\t fav " + i + "=" + favs[i]);
                }
            }
            else
            {
                print("country code is undefined");
            }
        }

        if (fontSizePref != null)
        {
            if (fontSizePref.hasValue())
            {
                String[] favs = fontSizePref.getFavourites();
                print("default font size=" + fontSizePref.toString() + "\t most fav: "
                        + fontSizePref.getMostFavourite());
                for (int i = 0; i < favs.length; i++)
                {
                    print("\t fav " + i + "=" + favs[i]);
                }
            }
            else
            {
                print("default font size is undefined");
            }
        }

        if (isSignedApp)
        {
            if (userNamePref != null)
            {
                if (userNamePref.hasValue())
                {
                    String[] favs = userNamePref.getFavourites();
                    print("user name is " + userNamePref.toString() + " fav: " + userNamePref.getMostFavourite());
                    for (int i = 0; i < favs.length; i++)
                    {
                        print("\t fav " + i + "=" + favs[i]);
                    }
                }
                else
                {
                    print("user name is undefined");
                }
            }

            if (userAddrPref != null)
            {
                if (userAddrPref.hasValue())
                {
                    String[] favs = userAddrPref.getFavourites();
                    print("user address is " + userAddrPref.toString() + " fav: " + userAddrPref.getMostFavourite());
                    for (int i = 0; i < favs.length; i++)
                    {
                        print("\t fav " + i + "=" + favs[i]);
                    }
                }
                else
                {
                    print("user address is undefined");
                }
            }

            if (userEmailPref != null)
            {
                if (userEmailPref.hasValue())
                {
                    String[] favs = userEmailPref.getFavourites();
                    print("user email is " + userEmailPref.toString() + " fav: " + userEmailPref.getMostFavourite());
                    for (int i = 0; i < favs.length; i++)
                    {
                        print("\t fav " + i + "=" + favs[i]);
                    }
                }
                else
                {
                    print("user email is undefined");
                }
            }
        }

        debugLog("UserPrefs::display_UserPreferences() - end");
    }

    /**
     * this is only called if we have read permission
     * 
     */
    private void read_UserPreferences(boolean runTest)
    {
        debugLog(" read_UserPreferences() - begin");

        try
        {
            userPrefMgr.read(userLangPref);
            userPrefMgr.read(countryCodePref);
            userPrefMgr.read(parentalRatingPref);
            userPrefMgr.read(fontSizePref);
            // can't read these three unless signed AND have
            // read permission in PRF;
            // if unsigned or (signed and don't have read permission)
            // will get a SecurityException
            if (isSignedApp && readPermission)
            {
                userPrefMgr.read(userNamePref);
                userPrefMgr.read(userAddrPref);
                userPrefMgr.read(userEmailPref);
            }
        }
        catch (SecurityException ex)
        {
            m_test.fail("UserPrefs::read_UserPreferences - caught a security exception trying to read a UserPreference: "
                    + ex.getMessage());
            debugLog("caught a security exception trying to read a UserPreference");
            ex.printStackTrace();
        }

        print(SECTION_DIVIDER);
        print("Completed read from UserPreferenceManger");

        if (expUserLangPref != null && !expUserLangPref.hasValue())
        {
            expUserLangPref.add(-1, m_currentLang);
        }

        if (runTest)
        {
            testAllPrefs();
        }

        debugLog(" read_UserPreferences() - end");
    }

    private void write_UserPreferences()
    {
        debugLog(" write_UserPreferences() - begin");

        if (writePermission)
        {
            try
            {
                userPrefMgr.write(userLangPref);
                userPrefMgr.write(parentalRatingPref);
                userPrefMgr.write(countryCodePref);
                userPrefMgr.write(fontSizePref);
                userPrefMgr.write(userNamePref);
                userPrefMgr.write(userAddrPref);
                userPrefMgr.write(userEmailPref);
            }
            catch (SecurityException ex)
            {
                m_test.fail("UserPrefs.write_UserPreferences - caught a security exception trying to write a UserPreference: "
                        + ex.getMessage());
                debugLog("caught a security exception trying to write a UserPreference");
                ex.printStackTrace();
            }
            catch (UnsupportedPreferenceException upex)
            {
                m_test.fail("UserPrefs::write_UserPreferences - caught an unsupported preference exception trying to write a UserPreference: "
                        + upex.getMessage());
                debugLog("caught an unsupported preference exception trying to write a UserPreference");
                upex.printStackTrace();
            }
            catch (IOException ioex)
            {
                m_test.fail("UserPrefs::write_UserPreferences - caught an IOException trying to write a UserPreference: "
                        + ioex.getMessage());
                debugLog("caught an IOException trying to write a UserPreference");
                ioex.printStackTrace();
            }
            catch (Exception x)
            {
                m_test.fail("UserPrefs::write_UserPreferences - caught an exception trying to write a UserPreference: "
                        + x.getMessage());
                debugLog("caught an exception trying to write a UserPreference");
                x.printStackTrace();
            }
            print(SECTION_DIVIDER);
            print("Completed write to UserPreferenceManger");
        }
        else
        {
            print(SECTION_DIVIDER);
            print("Skipping write to UserPreferenceManager - write permission denied");
        }

        testAllPrefs();

        debugLog(" write_UserPreferences() - end");
    }

    /**
     * ----------------------------------------------------------------------
     * set_HostAppUserPreferences
     * 
     * use args passed in by hostapp.properties for user preferences
     */
    private void set_HostAppUserPreferences()
    {

        debugLog("set_HostAppUserPreferences() - begin");

        userLangPref.add(m_hostLang);
        userLangPref.setMostFavourite(m_hostLang);
        expUserLangPref.add(-1, m_hostLang);

        m_currentLang = userLangPref.getMostFavourite();

        parentalRatingPref.add(m_hostParentalRating);
        parentalRatingPref.setMostFavourite(m_hostParentalRating);
        expParentalRatingPref.add(-1, m_hostParentalRating);

        countryCodePref.add(m_hostCountry);
        countryCodePref.setMostFavourite(m_hostCountry);
        expCountryCodePref.add(-1, m_hostCountry);

        fontSizePref.add(m_hostFontSize);
        fontSizePref.setMostFavourite(m_hostFontSize);
        expFontSizePref.add(-1, m_hostFontSize);

        if (isSignedApp)
        {
            userNamePref.add(m_hostName);
            userNamePref.setMostFavourite(m_hostName);
            expUserNamePref.add(-1, m_hostName);

            userAddrPref.add(m_hostAddress);
            userAddrPref.setMostFavourite(m_hostAddress);
            expUserAddrPref.add(-1, m_hostAddress);

            userEmailPref.add(m_hostEmail);
            userEmailPref.setMostFavourite(m_hostEmail);
            expUserEmailPref.add(-1, m_hostEmail);
        }

        print(SECTION_DIVIDER);
        print("Completed setting user preferences based on hostapp.properties");

        testAllPrefs();

        debugLog("set_HostAppUserPreferences() - end");
    }

    /**
     * ----------------------------------------------------------------------
     * set_UserPreferences
     * 
     * use args passed in by hostapp.properties for user preferences
     */
    private void set_UserPreferences()
    {

        debugLog("set_UserPreferences() - begin");

        userLangPref.add(m_lang);
        userLangPref.setMostFavourite(m_lang);
        expUserLangPref.add(-1, m_lang);

        m_currentLang = userLangPref.getMostFavourite();

        parentalRatingPref.add(m_parentalRating);
        parentalRatingPref.setMostFavourite(m_parentalRating);
        expParentalRatingPref.add(-1, m_parentalRating);

        countryCodePref.add(m_country);
        countryCodePref.setMostFavourite(m_country);
        expCountryCodePref.add(-1, m_country);

        fontSizePref.add(m_fontSize);
        fontSizePref.setMostFavourite(m_fontSize);
        expFontSizePref.add(-1, m_fontSize);

        if (isSignedApp)
        {
            userNamePref.add(m_name);
            userNamePref.setMostFavourite(m_name);
            expUserNamePref.add(-1, m_name);

            userAddrPref.add(m_address);
            userAddrPref.setMostFavourite(m_address);
            expUserAddrPref.add(-1, m_address);

            userEmailPref.add(m_email);
            userEmailPref.setMostFavourite(m_email);
            expUserEmailPref.add(-1, m_email);
        }

        print(SECTION_DIVIDER);
        print("Completed setting hard-coded user preferences");

        testAllPrefs();

        debugLog("set_UserPreferences() - end");
    }

    //
    // UserPreferenceChangeListener implementation
    //
    public void receiveUserPreferenceChangeEvent(UserPreferenceChangeEvent e)
    {
        debugLog(" receiveUserPreferenceChangeEvent - event is " + e.getName());
    }

    // 
    // display info on xlet usage
    // 
    void printTestList()
    {
        print(SECTION_DIVIDER);

        if (isSignedApp)
        {
            print("App " + m_appName + " (" + m_appId + ") is signed.");

            if (readPermission)
            {
                print("\t read permission approved");
            }
            else
            {
                print("\t read permission denied");
            }

            if (writePermission)
            {
                print("\t write permission approved\n");
            }
            else
            {
                print("\t write permission denied\n");
            }
        }
        else
        {
            print("App " + m_appName + " (" + m_appId + ") is unsigned. It has no read");
            print("\t access to User Address, User Name, User @.\n");
        }

        print("Press 1 to read mgr values from hashtable");
        print("Press 2 to clear preference object values");
        print("Press 3 to write pref objects to user pref file");
        print("Press 4 to add hardcoded values to preference objects");
        print("Press 5 to add hostapp values to preference objects");
        print("Press 6 to display user preference objects");
    }

    // 
    // Key Handling methods
    // 
    public void keyPressed(KeyEvent e)
    {

        switch (e.getKeyCode())
        {
            case OCRcEvent.VK_1: // read the user preference file?
                read_UserPreferences(true);
                break;
            case OCRcEvent.VK_2: // clear user preferences
                reinit_UserPreferences();
                break;
            case OCRcEvent.VK_3: // write user preference file
                if (writePermission)
                {
                    write_UserPreferences();
                }
                else
                {
                    print("Write Permission Denied, nothing written.");
                }
                break;
            case OCRcEvent.VK_4: // set hardcoded user preference
                set_UserPreferences();
                break;
            case OCRcEvent.VK_5: // set hostapp user preference
                set_HostAppUserPreferences();
                break;
            case OCRcEvent.VK_6: // display user preference
                display_UserPreferences();
                break;
            case OCRcEvent.VK_INFO: // display user preference
                printTestList();
                break;
            default:
                // m_ctx.notifyDestroyed();
                break;
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }

    //
    // For autoxlet automation framework (Driveable interface)
    // 
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(timeout);
            synchronized (m_eventMonitor)
            {
                keyPressed(e);

                int key = e.getKeyCode();
                if (key == OCRcEvent.VK_1 || key == OCRcEvent.VK_2 || key == OCRcEvent.VK_3 || key == OCRcEvent.VK_4
                        || key == OCRcEvent.VK_5)
                {
                    m_eventMonitor.waitForReady();
                }
            }
        }
        else
        {
            keyPressed(e);
        }
    }

    //
    // logging function - allow messages to post to teraterm and autoxlet logs
    //
    private void debugLog(String msg)
    {
        m_log.log("[" + m_appName + "] :" + msg);
    }

    //
    // printing function - allow messages to post in screen and log
    //
    private void print(String msg)
    {
        m_log.log("\t" + msg);
        m_vbox.write("    " + msg);
    }

    private void testAllPrefs()
    {

        // UserLang Pref
        testSinglePref("User Language Preference", expUserLangPref, userLangPref);

        // ParentalRating Pref
        testSinglePref("Parental Rating Preference", expParentalRatingPref, parentalRatingPref);

        // CountryCode Pref
        testSinglePref("Country Code Preference", expCountryCodePref, countryCodePref);

        // FontSize Pref
        testSinglePref("Default Font Size Preference", expFontSizePref, fontSizePref);

        // UserEmail Pref
        testSinglePref("User Email Preference", expUserEmailPref, userEmailPref);

        // UserAddr Pref
        testSinglePref("User Address Preference", expUserAddrPref, userAddrPref);

        // UserName Pref
        testSinglePref("User Name Preference", expUserNamePref, userNamePref);

        try
        {
            m_eventMonitor.notifyAll();
        }
        catch (Exception e)
        {
        }
    }

    private void testSinglePref(String prefName, GeneralPreference expPref, GeneralPreference actualPref)
    {
        String testMsg = "Test Failed - " + prefName + " ";
        String[] expFavs;
        String[] favs;

        m_test.assertTrue(testMsg + "contains at least one value? " + actualPref.hasValue() + ", expecting "
                + expPref.hasValue(), expPref.hasValue() == actualPref.hasValue());
        debugLog(prefName + " - contains at least one value? " + actualPref.hasValue() + ", expecting "
                + expPref.hasValue());

        m_test.assertTrue(testMsg + "'s name is " + actualPref.getName() + ", expecting " + expPref.getName(),
                expPref.getName().equals(actualPref.getName()));
        debugLog(prefName + "'s name is " + actualPref.getName() + ", expecting " + expPref.getName());

        expFavs = expPref.getFavourites();
        favs = actualPref.getFavourites();
        m_test.assertTrue(testMsg + " contains " + favs.length + " favourites, expecting " + expFavs.length,
                expFavs.length == favs.length);
        debugLog(prefName + " -  contains " + favs.length + " favourites, expecting " + expFavs.length);
        if (expFavs.length == favs.length)
        {
            for (int i = 0; i < favs.length; i++)
            {
                m_test.assertTrue(testMsg + " favorite " + i + " is " + favs[i] + ", expecting " + expFavs[i],
                        expFavs[i].equals(favs[i]));
                debugLog(prefName + " -  favorite " + i + " is " + favs[i] + ", expecting " + expFavs[i]);
            }
        }
    }
}
