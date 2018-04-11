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

package org.cablelabs.xlet.PermissionTest;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

import java.awt.AWTPermission;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SerializablePermission;
import java.net.SocketPermission;
import java.rmi.RemoteException;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.PropertyPermission;
import java.util.Vector;

import javax.tv.service.selection.SelectPermission;
import javax.tv.service.selection.ServiceContextPermission;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppID;
import org.dvb.application.AppsControlPermission;
import org.dvb.media.DripFeedPermission;
import org.dvb.net.rc.RCPermission;
import org.dvb.net.tuning.TunerPermission;
import org.dvb.user.UserPreferencePermission;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HStaticText;
import org.ocap.application.PermissionInformation;
import org.ocap.service.ServiceTypePermission;
import org.ocap.system.MonitorAppPermission;

// TODO: need to add a way to access the value of dvb.persistent.root.  
// most likely the location of persistent root should be added to a config 
// file because the ability to read the dvb.persistent.root property is 
// denied to Xlets in certain AppId ranges.

public class PermissionTestXlet extends HStaticText implements Driveable, Xlet
{
    public PermissionTestXlet()
    {
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        scene.setVisible(false);
        HSceneFactory.getInstance().dispose(scene);
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        this.ctx = ctx;

        // Initialize AutoXlet framework client and grab logger and test objects
        axc = new AutoXletClient(this, ctx);
        test = axc.getTest();

        // If we have successfully connected, initialize our logger from the
        // AutoXletClient, else use a default constructed XletLogger which will
        // send all logging output to standard out.
        if (axc.isConnected())
            log = axc.getLogger();
        else
            log = new XletLogger();
    }

    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    public void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            scene = HSceneFactory.getInstance().getFullScreenScene(
                    HScreen.getDefaultHScreen().getDefaultHGraphicsDevice());;
            scene.setLayout(new BorderLayout());

            setBackgroundMode(BACKGROUND_FILL);
            setForeground(new Color(234, 234, 234));
            setBackground(Color.darkGray); // temporary
            setFont(new Font("SansSerif", 0, 20));

            setup();
            runTests();
            showResults();

            scene.add(this);
            scene.validate();

            started = true;
        }

        scene.show();
        requestFocus();
    }

    private void showResults()
    {
        Color color;
        String text;
        if (results.isEmpty())
        {
            // all the tests passed - background color should be green
            color = Color.green.darker();
            text = PASSED_STRING;
        }
        else
        {
            // some of the tests failed - background should be red
            color = Color.red.darker();
            StringBuffer sb = new StringBuffer();
            // iterate over the list of failures and draw them on the screen
            for (Enumeration e = results.elements(); e.hasMoreElements();)
            {
                sb.append((String) e.nextElement()).append('\n');
            }
            text = sb.toString();
        }
        setBackground(color);
        setTextContent(text, ALL_STATES);
    }

    public void runTests()
    {
        int aid = getAppID().getAID();

        if (aid >= 0 && aid <= 0x3fff)
        {
            testUnsigned();
        }
        else if (aid >= 0x4000 && aid <= 0x5fff)
        {
            if (getPRFStream() != null)
            {
                testSignedWithPRF();
            }
            else
            {
                testSignedWithoutPRF();
            }
        }
        else if (aid >= 0x6000 && aid <= 0x7fff)
        {
            if (getPRFStream() != null)
            {
                testDuallySignedWithPRF();
            }
            else
            {
                testDuallySignedWithoutPRF();
            }
        }
        else
            log.log("Invalid AppID!");

    }

    protected void testPermissionCollectionGranted(PermissionCollection permissions)
    {
        SecurityManager sm = System.getSecurityManager();

        for (Enumeration enumer = permissions.elements(); enumer.hasMoreElements();)
        {
            Permission p = (Permission) enumer.nextElement();
            try
            {
                sm.checkPermission(p);
                test.assertTrue(true); // Test passed
            }
            catch (SecurityException e)
            {
                String message = "FAILURE: app doesn't have permission " + p;
                test.fail(message); // Test failed
                results.addElement(message);
                log.log(message);
            }
        }
    }

    protected void testPermissionCollectionDenied(PermissionCollection permissions)
    {
        SecurityManager sm = System.getSecurityManager();

        for (Enumeration enumer = permissions.elements(); enumer.hasMoreElements();)
        {
            Permission p = (Permission) enumer.nextElement();
            try
            {
                sm.checkPermission(p);
            }
            catch (SecurityException e)
            {
                test.assertTrue(true); // Test passed
                continue;
            }

            String message = "FAILURE: app has permission " + p;
            test.fail(message); // Test failed
            results.addElement(message);
            log.log(message);
        }
    }

    public void testUnsigned()
    {
        testPermissionCollectionGranted(PermissionInformation.getUnsignedAppPermissions());
        testPermissionCollectionDenied(alwaysDenied);
        testPermissionCollectionDenied(unsignedDenied);
        testPermissionCollectionDenied(prfPermissions);
    }

    public void testSignedWithoutPRF()
    {
        // The following line was added here because it only pertains
        // to a signed app without a PRF.
        signedDenied.add(new UserPreferencePermission("write"));

        testPermissionCollectionGranted(PermissionInformation.getUnsignedAppPermissions());
        testPermissionCollectionGranted(signed);
        testPermissionCollectionDenied(alwaysDenied);
        testPermissionCollectionDenied(signedDenied);
        testPermissionCollectionDenied(prfPermissions);
    }

    public void testSignedWithPRF()
    {
        testPermissionCollectionGranted(PermissionInformation.getUnsignedAppPermissions());
        testPermissionCollectionGranted(signed);
        testPermissionCollectionDenied(alwaysDenied);
        testPermissionCollectionDenied(signedDenied);
        processPRFFile();
        testPermissionCollectionGranted(prfRequestedPermissions);
        testPermissionCollectionDenied(prfDeniedPermissions);
    }

    public void testDuallySignedWithoutPRF()
    {
        testPermissionCollectionGranted(PermissionInformation.getUnsignedAppPermissions());
        testPermissionCollectionGranted(signed);
        testPermissionCollectionDenied(duallySigned);
        testPermissionCollectionDenied(alwaysDenied);
        testPermissionCollectionDenied(duallySignedDenied);
        testPermissionCollectionDenied(prfPermissions);
    }

    public void testDuallySignedWithPRF()
    {
        testPermissionCollectionGranted(PermissionInformation.getUnsignedAppPermissions());
        testPermissionCollectionGranted(signed);
        testPermissionCollectionGranted(duallySigned);
        testPermissionCollectionDenied(alwaysDenied);
        // permissions granted and denied by permission request file
        processPRFFile();
        testPermissionCollectionGranted(prfRequestedPermissions);
        testPermissionCollectionDenied(prfDeniedPermissions);
    }

    /**
     * Get the Permissions Request Files as an InputStream (if present).
     * 
     * @return <code>InputStream</code> instance with the
     */
    public InputStream getPRFStream()
    {
        // Generate OCAP PRF resource name
        String name = getClass().getName();
        int idx = name.lastIndexOf(".");
        if (idx != -1) name = name.substring(idx + 1);
        String prf = "ocap." + name + ".perm";

        InputStream stream = getClass().getResourceAsStream(prf);
        if (stream == null)
        {
            // No OCAP PRF! Might be a DVB PRF
            prf = "dvb." + name + ".perm";
            stream = getClass().getResourceAsStream(prf);
        }
        return stream;
    }

    public String getPRFData()
    {
        int length;
        char array[] = null;
        InputStream stream = getPRFStream();

        try
        {
            length = stream.available();
            array = new char[length];
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            reader.read(array, 0, length);
        }
        catch (IOException e)
        {
            return null;
        }

        return new String(array);
    }

    protected void processPRFFile()
    {
        String data = getPRFData();
        prfRequestedPermissions = new Permissions();
        prfDeniedPermissions = new Permissions();

        /*
         * <!ELEMENT permissionrequestfile (file?, capermission?,
         * applifecyclecontrol?, returnchannel?, tuning?, servicesel?,
         * userpreferences?, network?, dripfeed?, persistentfilecredential*,
         * ocap:monitorapplication*, ocap:servicetypepermission*)>
         */

        /*
         * <!ELEMENT file EMPTY> <!ATTLIST file value (true|false) "true" >
         */
        if (data.indexOf("<file") != -1)
        {
            int start = data.indexOf("<file");

            int end = data.indexOf("</file>", start + 1);
            if (end == -1)
            {
                end = data.indexOf("/>", start);
            }

            if (end != -1)
            {
                String substr = data.substring(start, end);
                int falseIndex = substr.indexOf("false");
                String persistentRoot = System.getProperty("dvb.persistent.root");
                AppID id = getAppID();

                if (!persistentRoot.endsWith("" + File.separatorChar)) persistentRoot = persistentRoot + "/";

                if (falseIndex != -1)
                {
                    log.log("Adding FilePermission to denied");

                    // Verify read access to the persistent root directory and
                    // all files in it is denied
                    prfDeniedPermissions.add(new FilePermission(persistentRoot, "read"));
                    prfDeniedPermissions.add(new FilePermission(persistentRoot + "*", "read"));

                    // Verify read/write access to OID directory and all files
                    // in it are denied
                    persistentRoot = persistentRoot + Integer.toHexString(id.getOID()) + "/";
                    prfDeniedPermissions.add(new FilePermission(persistentRoot, "read, write"));
                    prfDeniedPermissions.add(new FilePermission(persistentRoot + "*", "read,write,delete"));

                    // Verify read/write access to AID directory and all files
                    // in it are denied
                    persistentRoot = persistentRoot + Integer.toHexString(id.getAID()) + "/";
                    prfDeniedPermissions.add(new FilePermission(persistentRoot, "read, write"));
                    prfDeniedPermissions.add(new FilePermission(persistentRoot + "-", "read,write,delete"));
                }
                else
                {
                    log.log("Adding FilePermission to requested");
                    // Verify read access to the persistent root directory and
                    // all files in it is allowed
                    prfRequestedPermissions.add(new FilePermission(persistentRoot, "read"));
                    prfRequestedPermissions.add(new FilePermission(persistentRoot + "*", "read"));

                    // Verify read/write access to OID directory and all files
                    // in it are allowed
                    persistentRoot = persistentRoot + Integer.toHexString(id.getOID()) + "/";
                    prfRequestedPermissions.add(new FilePermission(persistentRoot, "read, write"));
                    prfRequestedPermissions.add(new FilePermission(persistentRoot + "*", "read,write,delete"));

                    // Verify read/write access to AID directory and all files
                    // in it are allowed
                    persistentRoot = persistentRoot + Integer.toHexString(id.getAID()) + "/";
                    prfRequestedPermissions.add(new FilePermission(persistentRoot, "read, write"));
                    prfRequestedPermissions.add(new FilePermission(persistentRoot + "-", "read,write,delete"));
                }
            }
        }

        /*
         * <!ELEMENT applifecyclecontrol EMPTY> <!ATTLIST applifecyclecontrol
         * value (true|false) "true" >
         */
        if (data.indexOf("applifecyclecontrol") != -1)
        {
            int start = data.indexOf("applifecyclecontrol");

            int end = data.indexOf("applifecyclecontrol", start + 1);
            if (end == -1)
            {
                end = data.indexOf("/>", start);
            }

            if (end != -1)
            {
                String substr = data.substring(start, end);
                int valueStart = substr.indexOf("value");
                String value = "true";

                if (valueStart != -1)
                {
                    int quote = substr.indexOf("\"", valueStart);
                    int quoteEnd = substr.indexOf("\"", quote + 1);
                    value = substr.substring(quote + 1, quoteEnd);
                }

                if ("true".equals(value))
                {
                    log.log("Adding AppsControlPermission to requested");
                    prfRequestedPermissions.add(new AppsControlPermission("*", "*"));
                }
                else
                {
                    log.log("Adding AppsControlPermission to denied");
                    prfDeniedPermissions.add(new AppsControlPermission("*", "*"));
                }
            }
        }

        /*
         * <!ELEMENT returnchannel (defaultisp?,phonenumber*)> <!ELEMENT
         * defaultisp EMPTY> <!ELEMENT phonenumber (#PCDATA)>
         */
        if (data.indexOf("returnchannel") != -1)
        {
            int start = data.indexOf("returnchannel");

            int end = data.indexOf("returnchannel", start + 1);
            if (end == -1)
            {
                end = data.indexOf("/>", start);
            }

            if (end != -1)
            {
                String substr = data.substring(start, end);
                int defaultIsp = substr.indexOf("defaultisp");

                int phoneNumber = substr.indexOf("<phonenumber>");
                String number = null;

                while (phoneNumber != -1)
                {
                    int numStart = substr.indexOf(">", phoneNumber);
                    int numEnd = substr.indexOf("<", numStart + 1);
                    number = substr.substring(numStart + 1, numEnd);

                    if (number != null)
                    {
                        log.log("Adding RCPermission(target:" + number + ") to requested");
                        prfRequestedPermissions.add(new RCPermission("target:" + number));
                    }

                    substr = substr.substring(numEnd, substr.length() - 1);
                    phoneNumber = substr.indexOf("<phonenumber>");
                }

                if (defaultIsp != -1)
                {
                    log.log("Adding RCPermission to requested");
                    prfRequestedPermissions.add(new RCPermission("target:default"));
                }

            }
        }

        /*
         * <!ELEMENT tuning EMPTY> <!ATTLIST tuning value (true|false) "true" >
         */
        if (data.indexOf("tuning") != -1)
        {
            int start = data.indexOf("tuning");

            int end = data.indexOf("tuning", start + 1);
            if (end == -1)
            {
                end = data.indexOf("/>", start);
            }

            if (end != -1)
            {
                String substr = data.substring(start, end);
                int valueStart = substr.indexOf("value");
                String value = "true";

                if (valueStart != -1)
                {
                    int quote = substr.indexOf("\"", valueStart);
                    int quoteEnd = substr.indexOf("\"", quote + 1);
                    value = substr.substring(quote + 1, quoteEnd);
                }

                if ("true".equals(value))
                {
                    log.log("adding TunerPermission to the requested list");
                    prfRequestedPermissions.add(new TunerPermission(""));
                }
                else
                {
                    log.log("adding TunerPermission to the denied list");
                    prfDeniedPermissions.add(new TunerPermission(""));
                }
            }
        }

        /*
         * <!ELEMENT servicesel EMPTY> <!ATTLIST servicesel value (true|false)
         * "true" >
         */
        if (data.indexOf("servicesel") != -1)
        {
            int start = data.indexOf("servicesel");

            int end = data.indexOf("servicesel", start + 1);
            if (end == -1)
            {
                end = data.indexOf("/>", start);
            }

            if (end != -1)
            {
                String substr = data.substring(start, end);
                int valueStart = substr.indexOf("value");
                String value = "true";

                if (valueStart != -1)
                {
                    int readValueStart = substr.indexOf("\"", valueStart);
                    int readValueEnd = substr.indexOf("\"", readValueStart + 1);
                    value = substr.substring(readValueStart + 1, readValueEnd);
                }

                if ("true".equals(value))
                {
                    log.log("adding SelectPermission(\"*\",\"own\" to requested list");
                    prfRequestedPermissions.add(new SelectPermission("*", "own"));
                }
                else
                {
                    log.log("adding SelectPermission(\"*\",\"own\" to denied list");
                    prfDeniedPermissions.add(new SelectPermission("*", "own"));
                }
            }
        }

        /*
         * <!ELEMENT userpreferences EMPTY> <!ATTLIST userpreferences write
         * (true|false) "false" read (true|false) "true" >
         */
        if (data.indexOf("userpreferences") != -1)
        {
            int start = data.indexOf("userpreferences");

            int end = data.indexOf("userpreferences", start + 1);
            if (end == -1)
            {
                end = data.indexOf("/>", start);
            }

            if (end != -1)
            {
                String substr = data.substring(start, end);
                int writeStart = substr.indexOf("write");
                int readStart = substr.indexOf("read");
                String readValue = "true";
                String writeValue = "false";

                if (readStart != -1)
                {
                    int readValueStart = substr.indexOf("\"", readStart);
                    int readValueEnd = substr.indexOf("\"", readValueStart + 1);
                    if (readValueEnd > readValueStart)
                    {
                        readValue = substr.substring(readValueStart + 1, readValueEnd);
                    }

                    if ("true".equals(readValue))
                    {
                        log.log("adding UserPreferencePermisson(\"read\") to requested list");
                        prfRequestedPermissions.add(new UserPreferencePermission("read"));
                    }
                    else
                    {
                        log.log("adding UserPreferencePermisson(\"read\") to denied list");
                        prfDeniedPermissions.add(new UserPreferencePermission("read"));
                    }
                }

                if (writeStart != -1)
                {
                    int writeValueStart = substr.indexOf("\"", writeStart);
                    int writeValueEnd = substr.indexOf("\"", writeValueStart + 1);
                    if (writeValueEnd > writeValueStart)
                    {
                        writeValue = substr.substring(writeValueStart + 1, writeValueEnd);
                    }

                    if ("true".equals(writeValue))
                    {
                        log.log("adding UserPreferencePermisson(\"write\") to requested list");
                        prfRequestedPermissions.add(new UserPreferencePermission("write"));
                    }
                    else
                    {
                        log.log("adding UserPreferencePermisson(\"write\") to denied list");
                        prfDeniedPermissions.add(new UserPreferencePermission("write"));
                    }
                }
            }
        }

        /*
         * <!ELEMENT network (host)+> <!ELEMENT host (#PCDATA)> <!ATTLIST host
         * action CDATA #REQUIRED >
         */
        if (data.indexOf("<network") != -1)
        {
            int start = data.indexOf("<network");
            int end = data.indexOf("network", start + 2);

            if (end != -1)
            {
                String substr = data.substring(start, end);
                int hostStart = substr.indexOf("host");
                int hostEnd = substr.indexOf("host", hostStart + 1);
                String hostString = substr.substring(hostStart, hostEnd);
                int action = hostString.indexOf("action");

                if (action != -1)
                {
                    String hostname, actionString;
                    int hostNameStart, hostNameEnd;
                    int quote = hostString.indexOf("\"", action);
                    int quoteEnd = hostString.indexOf("\"", quote + 1);
                    SocketPermission perm;

                    actionString = hostString.substring(quote + 1, quoteEnd);

                    hostNameStart = hostString.indexOf(">", quoteEnd);
                    hostNameEnd = hostString.indexOf("</", hostNameStart + 1);
                    hostname = hostString.substring(hostNameStart + 1, hostNameEnd);
                    hostname.trim();
                    perm = new SocketPermission(hostname, actionString);
                    log.log("adding requested permission " + perm);
                    prfRequestedPermissions.add(perm);
                }
            }
        }

        /*
         * <!ELEMENT dripfeed EMPTY> <!ATTLIST dripfeed value (true|false)
         * "true" >
         */
        if (data.indexOf("dripfeed") != -1)
        {
            String value = "true";
            int valueStart;
            int start = data.indexOf("dripfeed");
            int end = data.indexOf("dripfeed", start + 1);

            if (end == -1)
            {
                end = data.indexOf("/>", start);
            }

            if (end != -1)
            {
                String substr = data.substring(start, end);

                valueStart = substr.indexOf("value");
                if (valueStart != -1)
                {
                    int quote = substr.indexOf("\"", valueStart);
                    int quoteEnd = substr.indexOf("\"", quote + 1);
                    value = substr.substring(quote + 1, quoteEnd);
                }

                if ("true".equals(value))
                {
                    log.log("adding DripFeedPermission() to requested list");
                    prfRequestedPermissions.add(new DripFeedPermission(""));
                }
                else
                {
                    log.log("adding DripFeedPermission() to denied list");
                    prfDeniedPermissions.add(new DripFeedPermission(""));
                }
            }
        }

        /*
         * <!ELEMENT persistentfilecredential (grantoridentifier,
         * expirationdate, filename+, signature, certchainfileid)> <!ELEMENT
         * grantoridentifier EMPTY> <!ATTLIST grantoridentifier id CDATA
         * #REQUIRED > <!ELEMENT expirationdate EMPTY> <!ATTLIST expirationdate
         * date CDATA #REQUIRED > <!ELEMENT filename (#PCDATA)> <!ATTLIST
         * filename write (true|false) "true" read (true|false) "true" >
         * <!ELEMENT signature (#PCDATA)> <!ELEMENT certchainfileid (#PCDATA)>
         */
        if (data.indexOf("persistentfilecredential") != -1)
        {
            // TODO: need to implement this
        }

        /*
         * <!ELEMENT ocap:monitorapplication EMPTY> <!ENTITY %
         * OCAPMonitorAppPermType.class "(registrar | service | servicemanager |
         * security | reboot | systemevent | handler.appFilter |
         * handler.resource | handler.closedCaptioning | filterUserEvents |
         * handler.eas | setVideoPort | podApplication | signal.configured |
         * properties | storage | registeredapi | vbifiltering |)" > <!ATTLIST
         * ocap:monitorapplication name %OCAPMonitorAppPermType.class; #REQUIRED
         * value (true | false) "false"
         */
        if (data.indexOf("ocap:monitorapplication") != -1)
        {
            int start = data.indexOf("ocap:monitorapplication");

            while (start != -1)
            {
                // find the closing of the entry
                int end = data.indexOf("/>", start + 1);
                if (end != -1)
                {
                    String substring = data.substring(start, end);
                    String name = null;
                    String value = "false";
                    int nameStart = substring.indexOf("name");
                    int valueStart = substring.indexOf("value");
                    if (nameStart != -1)
                    {
                        PermissionCollection perms;
                        int quote = substring.indexOf("\"", nameStart);

                        if (quote != -1)
                        {
                            int valueEnd = substring.indexOf("\"", quote + 1);
                            name = substring.substring(quote + 1, valueEnd);
                        }

                        if (valueStart != -1)
                        {
                            quote = substring.indexOf("\"", valueStart);
                            if (quote != -1)
                            {
                                int valueEnd = substring.indexOf("\"", quote + 1);
                                value = substring.substring(quote + 1, valueEnd);
                            }
                        }

                        // only add to requested permissions if this a
                        // dually-signed app
                        // otherwise add this permission to the denied list
                        if ("true".equals(value) && getAppID().getAID() >= 0x6000)
                        {
                            perms = prfRequestedPermissions;
                        }
                        else
                        {
                            perms = prfDeniedPermissions;
                        }

                        if ("service".equals(name))
                        {
                            // OCAP 10.2.2.2.3.3
                            log.log("Adding permissions associated with monitorAppPermission(\"service\") to "
                                    + (perms == prfRequestedPermissions ? "requested" : "denied"));
                            perms.add(new ServiceContextPermission("access", "*"));
                            perms.add(new ServiceContextPermission("getServiceContentHandlers", "own"));
                            perms.add(new ServiceContextPermission("create", "own"));
                            perms.add(new ServiceContextPermission("destroy", "own"));
                            perms.add(new ServiceContextPermission("stop", "*"));
                            perms.add(new SelectPermission("*", "own"));
                            perms.add(new ServiceTypePermission("*", "own"));

                        }
                        else if ("servicemanager".equals(name))
                        {
                            // OCAP 10.2.2.2.3.3
                            log.log("Adding permissions associated with monitorAppPermission(\"serviceManager\") to "
                                    + (perms == prfRequestedPermissions ? "requested" : "denied"));
                            perms.add(new ServiceContextPermission("access", "*"));
                            perms.add(new ServiceContextPermission("getServiceContentHandlers", "own"));
                            perms.add(new ServiceContextPermission("create", "own"));
                            perms.add(new ServiceContextPermission("destroy", "own"));
                            perms.add(new ServiceContextPermission("stop", "*"));
                            perms.add(new SelectPermission("*", "own"));
                            perms.add(new ServiceTypePermission("*", "*")); // TODO:
                                                                            // unless
                                                                            // overridden
                                                                            // by
                                                                            // ServiceTypePermission...
                            // OCAP 10.2.2.3
                            perms.add(new AppsControlPermission(null, null));
                        }
                        // else if ("properties".equals(name))
                        // {
                        // perms.add(new
                        // PropertyPermission("ocap.hardware.vendor_id",
                        // "read"));
                        // perms.add(new
                        // PropertyPermission("ocap.hardware.version_id",
                        // "read"));
                        // perms.add(new
                        // PropertyPermission("ocap.hardware.createdate",
                        // "read"));
                        // perms.add(new PropertyPermission("ocap.serialnum",
                        // "read"));
                        // perms.add(new PropertyPermission("ocap.memory.video",
                        // "read"));
                        // perms.add(new PropertyPermission("ocap.memory.total",
                        // "read"));
                        // perms.add(new
                        // PropertyPermission("ocap.system.highdef", "read"));
                        // }
                        else
                        {
                            log.log("Adding permissions associated with monitorAppPermission(" + name + ") to "
                                    + (perms == prfRequestedPermissions ? "requested" : "denied"));
                            perms.add(new MonitorAppPermission(name));
                        }
                    }
                }
                // does ocap:monitorapplication occur again?
                start = data.indexOf("ocap:monitorapplication", end + 1);
            }
        }

        /*
         * <!ELEMENT ocap:servicetypepermission EMPTY> <!ATTLIST
         * ocap:servicetypepermission type (broadcast | abstract.mso |
         * abstract.manufacturer) "broadcast" actions (own | all) "all" value
         * (true | false) "false" >
         */
        if (data.indexOf("ocap:servicetypepermission") != -1)
        {
            int start = data.indexOf("ocap:servicetypepermission");
            while (start != -1)
            {
                // find the closing of the entry
                int end = data.indexOf("/>", start + 1);
                if (end != -1)
                {
                    String substring = data.substring(start, end);
                    String type = "broadcast";
                    String actions = "*";
                    String value = "false";
                    int typeStart = substring.indexOf("type");
                    int actionsStart = substring.indexOf("action");
                    int valueStart = substring.indexOf("value");

                    if (typeStart != -1)
                    {
                        int quote = substring.indexOf("\"", typeStart);
                        if (quote != -1)
                        {
                            int valueEnd = substring.indexOf("\"", quote + 1);
                            type = substring.substring(quote + 1, valueEnd);
                        }
                    }

                    if (actionsStart != -1)
                    {
                        int quote = substring.indexOf("\"", actionsStart);
                        if (quote != -1)
                        {
                            int valueEnd = substring.indexOf("\"", quote + 1);
                            actions = substring.substring(quote + 1, valueEnd);
                        }
                    }

                    if (valueStart != -1)
                    {
                        int quote = substring.indexOf("\"", valueStart);
                        if (quote != -1)
                        {
                            int valueEnd = substring.indexOf("\"", quote + 1);
                            value = substring.substring(quote + 1, valueEnd);
                        }
                    }
                    if ("all".equals(actions))
                    {
                        actions = "*";
                    }

                    if ("true".equals(value))
                    {
                        log.log("Adding ServiceTypePermission(" + type + ", " + actions + ") to requested list");
                        prfRequestedPermissions.add(new ServiceTypePermission(type, actions));
                    }
                    else
                    {
                        log.log("Adding ServiceTypePermission(" + type + ", " + actions + ") to denied list");
                        prfDeniedPermissions.add(new ServiceTypePermission(type, actions));
                    }
                }
                // does ocap:servicetypepermission occur again?
                start = data.indexOf("ocap:servicetypepermission", end + 1);
            }
        }
    }

    /**
     * Creates an AppID instance that contains the AID and OID values for this
     * application.
     * 
     * @return an AppID object for this application
     */
    protected AppID getAppID()
    {
        String aidStr = (String) ctx.getXletProperty("dvb.app.id");
        String oidStr = (String) ctx.getXletProperty("dvb.org.id");
        if (aidStr == null || oidStr == null) return null;

        int aid = Integer.parseInt(aidStr, 16);
        long oid = Long.parseLong(oidStr, 16);
        return new AppID((int) oid, aid);
    }

    protected void setup()
    {
        int aid = getAppID().getAID();

        // these are needed for all AIDs
        setupUnsignedDenied();
        setupAlwaysDenied();
        setupPrfPermissions();

        // permission collections needed for signed applications
        if (aid >= 0x4000)
        {
            setupSigned();
            setupSignedDenied();
        }
        // permission collections needed for dually-signed applications
        if (aid >= 0x6000)
        {
            setupDuallySigned();
            setupDuallySignedDenied();
        }

        addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if (scene.isVisible()) repaint();
            }
        });
    }

    /**
     * Creates a collection of Permission objects that are denied to unsigned
     * applications
     * 
     */
    protected void setupUnsignedDenied()
    {
        unsignedDenied = new Permissions();

        unsignedDenied.add(new MonitorAppPermission("registrar"));
        unsignedDenied.add(new MonitorAppPermission("service"));
        unsignedDenied.add(new MonitorAppPermission("servicemanager"));
        unsignedDenied.add(new MonitorAppPermission("security"));
        unsignedDenied.add(new MonitorAppPermission("reboot"));
        unsignedDenied.add(new MonitorAppPermission("systemevent"));
        unsignedDenied.add(new MonitorAppPermission("handler.appFilter"));
        unsignedDenied.add(new MonitorAppPermission("handler.resource"));
        unsignedDenied.add(new MonitorAppPermission("handler.closedCaptioning"));
        unsignedDenied.add(new MonitorAppPermission("filterUserEvents"));
        unsignedDenied.add(new MonitorAppPermission("handler.eas"));
        unsignedDenied.add(new MonitorAppPermission("setVideoPort"));
        unsignedDenied.add(new MonitorAppPermission("podApplication"));
        unsignedDenied.add(new MonitorAppPermission("signal.configured"));
        unsignedDenied.add(new MonitorAppPermission("storage"));
        unsignedDenied.add(new MonitorAppPermission("properties"));
        unsignedDenied.add(new MonitorAppPermission("registeredapi"));
        unsignedDenied.add(new MonitorAppPermission("vbifiltering"));
        // unsignedDenied.add(new
        // FilePermission(System.getProperty("dvb.persistent.root"), "read"));
        // //TODO: test for persistent storage
        unsignedDenied.add(new PropertyPermission("dvb.persistent.root", "read"));
        unsignedDenied.add(new PropertyPermission("*", "read"));
        unsignedDenied.add(new UserPreferencePermission("write"));
        unsignedDenied.add(new ServiceContextPermission("create", "own"));
        unsignedDenied.add(new ServiceContextPermission("destroy", "own"));
    }

    /**
     * Creates a collection of Permission objects that are granted to signed
     * applicatons
     * 
     */
    protected void setupSigned()
    {
        signed = new Permissions();

        signed.add(new PropertyPermission("dvb.persistent.root", "read"));
        signed.add(new SelectPermission("*", "own")); // MHP 11.10.2.7
        signed.add(new ServiceContextPermission("*", "own")); // MHP 11.10.2.7
        signed.add(new FilePermission("/oc/-", "read")); // MHP 11.10.2.2
        // write permission for carousel mount points should be granted even 
        // though it actual operation wil fail at the native level
        signed.add(new FilePermission("/oc/-", "write"));  

    }

    /**
     * Creates a collection of Permission objects that are denied to signed
     * applicatons
     * 
     */
    protected void setupSignedDenied()
    {
        signedDenied = new Permissions();

        signedDenied.add(new MonitorAppPermission("registrar"));
        signedDenied.add(new MonitorAppPermission("service"));
        signedDenied.add(new MonitorAppPermission("servicemanager"));
        signedDenied.add(new MonitorAppPermission("security"));
        signedDenied.add(new MonitorAppPermission("reboot"));
        signedDenied.add(new MonitorAppPermission("systemevent"));
        signedDenied.add(new MonitorAppPermission("handler.appFilter"));
        signedDenied.add(new MonitorAppPermission("handler.resource"));
        signedDenied.add(new MonitorAppPermission("handler.closedCaptioning"));
        signedDenied.add(new MonitorAppPermission("filterUserEvents"));
        signedDenied.add(new MonitorAppPermission("handler.eas"));
        signedDenied.add(new MonitorAppPermission("setVideoPort"));
        signedDenied.add(new MonitorAppPermission("podApplication"));
        signedDenied.add(new MonitorAppPermission("signal.configured"));
        signedDenied.add(new MonitorAppPermission("storage"));
        signedDenied.add(new MonitorAppPermission("properties"));
        signedDenied.add(new MonitorAppPermission("registeredapi"));
        signedDenied.add(new MonitorAppPermission("vbifiltering"));
        signedDenied.add(new PropertyPermission("*", "read"));
    }

    /**
     * Creates a collection of Permission objects that are granted to
     * dually-signed applications
     * 
     */
    protected void setupDuallySigned()
    {
        duallySigned = new Permissions();

        // the following permission is only granted if PRF contains
        // monitorAppPermission("properties")

        // duallySigned.add(new PropertyPermission("*", "read"));
    }

    /**
     * Creates a collection of Permission objects that are denied to
     * dually-signed applications
     * 
     */
    protected void setupDuallySignedDenied()
    {
        duallySignedDenied = new Permissions();

        duallySignedDenied.add(new MonitorAppPermission("registrar"));
        duallySignedDenied.add(new MonitorAppPermission("service"));
        duallySignedDenied.add(new MonitorAppPermission("servicemanager"));
        duallySignedDenied.add(new MonitorAppPermission("security"));
        duallySignedDenied.add(new MonitorAppPermission("reboot"));
        duallySignedDenied.add(new MonitorAppPermission("systemevent"));
        duallySignedDenied.add(new MonitorAppPermission("handler.appFilter"));
        duallySignedDenied.add(new MonitorAppPermission("handler.resource"));
        duallySignedDenied.add(new MonitorAppPermission("handler.closedCaptioning"));
        duallySignedDenied.add(new MonitorAppPermission("filterUserEvents"));
        duallySignedDenied.add(new MonitorAppPermission("handler.eas"));
        duallySignedDenied.add(new MonitorAppPermission("setVideoPort"));
        duallySignedDenied.add(new MonitorAppPermission("podApplication"));
        duallySignedDenied.add(new MonitorAppPermission("signal.configured"));
        duallySignedDenied.add(new MonitorAppPermission("storage"));
        duallySignedDenied.add(new MonitorAppPermission("properties"));
        duallySignedDenied.add(new MonitorAppPermission("registeredapi"));
        duallySignedDenied.add(new MonitorAppPermission("vbifiltering"));
    }

    /**
     * Creates a collection of Permission objects that are denied to all
     * applications
     * 
     */
    protected void setupAlwaysDenied()
    {
        alwaysDenied = new Permissions();

        alwaysDenied.add(new AllPermission());
        alwaysDenied.add(new RuntimePermission("createClassLoader"));
        alwaysDenied.add(new RuntimePermission("getClassLoader"));
        alwaysDenied.add(new RuntimePermission("setContextClassLoader"));
        alwaysDenied.add(new RuntimePermission("setSecurityManager"));
        alwaysDenied.add(new RuntimePermission("createSecurityManager"));
        alwaysDenied.add(new RuntimePermission("exitVM"));
        alwaysDenied.add(new RuntimePermission("setFactory"));
        alwaysDenied.add(new RuntimePermission("setIO"));
        alwaysDenied.add(new RuntimePermission("modifyThread"));
        alwaysDenied.add(new RuntimePermission("stopThread"));
        alwaysDenied.add(new RuntimePermission("modifyThreadGroup"));
        alwaysDenied.add(new RuntimePermission("getProtectionDomain"));
        alwaysDenied.add(new RuntimePermission("readFileDescriptor"));
        alwaysDenied.add(new RuntimePermission("writeFileDescriptor"));
        alwaysDenied.add(new RuntimePermission("loadLibrary.testlib"));
        alwaysDenied.add(new RuntimePermission("accessInPackage.org.ocap.application"));
        alwaysDenied.add(new RuntimePermission("defineClassInPackage.org.ocap.application"));
        alwaysDenied.add(new RuntimePermission("accessDelcaredMembers"));
        alwaysDenied.add(new RuntimePermission("queuePrintJob"));
        alwaysDenied.add(new PropertyPermission("*", "write"));
        // even though actual write operation will fail at the native level,
        // write permission for carousel mount points should still be granted
        //alwaysDenied.add(new FilePermission("/oc/-", "write"));
        alwaysDenied.add(new FilePermission("", "write"));
        alwaysDenied.add(new AWTPermission("*"));
        alwaysDenied.add(new SerializablePermission("enableSubclassImplementation"));
        alwaysDenied.add(new SerializablePermission("enableSubstitution"));

    }

    /**
     * Creates a collection of Permission objects that can be granted to an
     * application with a permissions request file. This is also used to verify
     * that an app without a PRF doesn't already have these permissions.
     * 
     */
    protected void setupPrfPermissions()
    {
        prfPermissions = new Permissions();

        // String persistentRoot = System.getProperty("dvb.persistent.root"); //
        // TODO: need to test dvb.persistent.root

        AppID id = getAppID();

        // prfPermissions.add(new FilePermission(persistentRoot + "/" +
        // id.getOID() + "/*", "read,write"));
        // prfPermissions.add(new FilePermission(persistentRoot + "/" +
        // id.getOID() + "/" + id.getAID() + "/-", "read,write"));
        prfPermissions.add(new AppsControlPermission("*", "*"));
        prfPermissions.add(new RCPermission("target:default"));
        prfPermissions.add(new TunerPermission(""));
        prfPermissions.add(new UserPreferencePermission("read"));
        prfPermissions.add(new UserPreferencePermission("write"));
        prfPermissions.add(new DripFeedPermission(""));
        prfPermissions.add(new ServiceContextPermission("access", "*"));
        prfPermissions.add(new ServiceContextPermission("stop", "*"));
        if (id.getAID() < 0x4000)
        {
            // permission already granted for signed apps
            // still should be denied to unsigned apps
            prfPermissions.add(new SelectPermission("*", "own"));
        }
        prfPermissions.add(new ServiceTypePermission("*", "own"));
        prfPermissions.add(new ServiceContextPermission("*", "*"));
        prfPermissions.add(new SelectPermission("*", "*"));
        prfPermissions.add(new ServiceTypePermission("*", "*"));
        prfPermissions.add(new AppsControlPermission("*", "*"));
        prfPermissions.add(new ServiceTypePermission("*", "*"));
        prfPermissions.add(new MonitorAppPermission("registrar"));
        prfPermissions.add(new MonitorAppPermission("service"));
        prfPermissions.add(new MonitorAppPermission("servicemanager"));
        prfPermissions.add(new MonitorAppPermission("security"));
        prfPermissions.add(new MonitorAppPermission("reboot"));
        prfPermissions.add(new MonitorAppPermission("systemevent"));
        prfPermissions.add(new MonitorAppPermission("handler.appFilter"));
        prfPermissions.add(new MonitorAppPermission("handler.resource"));
        prfPermissions.add(new MonitorAppPermission("handler.closedCaptioning"));
        prfPermissions.add(new MonitorAppPermission("filterUserEvents"));
        prfPermissions.add(new MonitorAppPermission("handler.eas"));
        prfPermissions.add(new MonitorAppPermission("setVideoPort"));
        prfPermissions.add(new MonitorAppPermission("podApplication"));
        prfPermissions.add(new MonitorAppPermission("signal.configured"));
        prfPermissions.add(new MonitorAppPermission("storage"));
        prfPermissions.add(new MonitorAppPermission("properties"));
        prfPermissions.add(new MonitorAppPermission("registeredapi"));
        prfPermissions.add(new MonitorAppPermission("vbifiltering"));
    }

    /* Permissions that are always denied */
    protected PermissionCollection alwaysDenied;

    /* Permissions denied to an unsigned app */
    protected PermissionCollection unsignedDenied;

    /* Permissions granted to signed app */
    protected PermissionCollection signed;

    /* Permissions denied to a signed app */
    protected PermissionCollection signedDenied;

    /* Permissions granted to a dually signed app */
    protected PermissionCollection duallySigned;

    /* Permissions denied to a dually signed app */
    protected PermissionCollection duallySignedDenied;

    /* Permissions that can be requested by a signed app */
    protected PermissionCollection prfPermissions;

    /* Permissions requested by permission request file */
    protected PermissionCollection prfRequestedPermissions;

    /* Permissions denied by permissions request file */
    protected PermissionCollection prfDeniedPermissions;

    // XletContext passed in on Xlet construction
    private XletContext ctx;

    // HScene used for drawing
    protected HScene scene;

    // true after startXlet() has been called the first time
    private boolean started;

    // Vector of String objects that report test results
    protected Vector results = new Vector();

    // Objects used to integrate with AutoXlet testing framework
    private AutoXletClient axc;

    private Logger log;

    private Test test;

    // String drawn on the screen if the test passed
    private final String PASSED_STRING = "Passed!";

    public void dispatchEvent(KeyEvent arg0, boolean arg1, int arg2) throws RemoteException
    {
        log.log("AutoXlet EventReceived, but no events are needed by this Xlet");
    }
}
