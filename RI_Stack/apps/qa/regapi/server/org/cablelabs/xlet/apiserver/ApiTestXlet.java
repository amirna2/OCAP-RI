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
package org.cablelabs.xlet.apiserver;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppID;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.application.DVBJProxy;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenRectangle;
import org.ocap.system.RegisteredApiManager;

/**
 * This Xlet installs a registered API and launches client Xlets that utilize
 * the API.
 * <p>
 * This is part of an Xlet Integration Test for the Registered API support.
 * 
 * @author Aaron Kamienski
 */
public class ApiTestXlet implements Xlet, AppStateChangeEventListener
{
    private XletContext xc;

    private boolean started = false;

    private boolean paused = true;

    private static final String APINAME = "org.cablelabs.api.apitest.TestApi";

    private static final String APIVER = "0";

    private static final String BADNAME = APINAME + ".bad";

    private String version;

    private String badVersion;

    private short storagePriority = 100;

    private AppID baseAppID = null;

    private Hashtable apps = new Hashtable();

    boolean onlySuccess = false;

    boolean onlyFail = false;

    public synchronized void initXlet(XletContext xc)
    {
        this.xc = xc;
    }

    public synchronized void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            parseArgs();
            registerApi();
        }
        started = true;

        if (paused)
        {
            if (!onlyFail) launchApp(0, "success", APINAME, version, true);
            if (!onlySuccess) launchApp(1, "no such api", BADNAME, version, false);
            if (!onlyFail) launchApp(2, "success(2)", APINAME, version, true);
        }
        paused = false;
    }

    private void parseArgs()
    {
        parseArgs((String[]) xc.getXletProperty("dvb.caller.parameters"));
        parseArgs((String[]) xc.getXletProperty(XletContext.ARGS));
    }

    private void parseArgs(String[] args)
    {
        if (args == null) return;
        for (int i = 0; i < args.length; ++i)
        {
            if (args[i].startsWith("baseid="))
            {
                // Defines the base AppID
                String value = args[i].substring(7);
                long longValue = Long.parseLong(value, 16);

                baseAppID = new AppID((int) (longValue >>> 16), (int) (longValue & 0xFFFF));
            }
            else if (args[i].startsWith("priority="))
            {
                storagePriority = Short.parseShort(args[i].substring(9));
            }
            else if ("onlysuccess".equals(args[i]))
            {
                onlySuccess = true;
                onlyFail = false;
            }
            else if ("onlyfail".equals(args[i]))
            {
                onlySuccess = false;
                onlyFail = true;
            }
        }
    }

    private void launchApp(int index, String name, String apiName, String apiVer, boolean expectResult)
            throws XletStateChangeException
    {
        if (baseAppID == null)
        {
            baseAppID = getAppID();
            baseAppID = new AppID(baseAppID.getOID(), baseAppID.getAID() + 1);
        }

        AppID id = new AppID(baseAppID.getOID(), baseAppID.getAID() + index);

        AppsDatabase db = AppsDatabase.getAppsDatabase();
        DVBJProxy app = (DVBJProxy) db.getAppProxy(id);

        rememberApp(app);

        Rectangle rect = makeRect(index);
        String[] args = { name, apiName, apiVer, expectResult + "", "x=" + rect.x, "y=" + rect.y,
                "width=" + rect.width, "height=" + rect.height };

        app.start(args);
    }

    private AppID getAppID()
    {
        String aidStr = (String) xc.getXletProperty("dvb.app.id");
        String oidStr = (String) xc.getXletProperty("dvb.org.id");

        if (aidStr == null || oidStr == null) return null;

        int aid = Integer.parseInt(aidStr, 16);
        long oid = Long.parseLong(oidStr, 16);

        return new AppID((int) oid, aid);
    }

    private static final HScreenRectangle rects[] = { new HScreenRectangle(0F, 0F, 1F, 1 / 3F),
            new HScreenRectangle(0F, 1 / 3F, 1F, 1 / 3F), new HScreenRectangle(0F, 2 / 3F, 1F, 1 / 3F), };

    private Rectangle makeRect(int index)
    {
        HGraphicsDevice gfx = HScreen.getDefaultHScreen().getDefaultHGraphicsDevice();
        Dimension resolution = gfx.getCurrentConfiguration().getPixelResolution();
        Rectangle bounds = new Rectangle(25, 25, resolution.width - 50, resolution.height - 50);

        HScreenRectangle relative = rects[index];

        return new Rectangle((int) (bounds.x + relative.x * bounds.width),
                (int) (bounds.y + relative.y * bounds.height), (int) (relative.width * bounds.width),
                (int) (relative.height * bounds.height));
    }

    private void rememberApp(DVBJProxy app)
    {
        apps.put(app, app);
        app.addAppStateChangeEventListener(this);
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.dvb.application.AppStateChangeEventListener#stateChange(org.dvb.application.AppStateChangeEvent)
     */
    public synchronized void stateChange(AppStateChangeEvent evt)
    {
        DVBJProxy app = (DVBJProxy) apps.get(evt.getSource());
        if (app != null)
        {
            System.out.println("AppEvent Received: " + evt);
            if (evt.hasFailed())
            {
                // Kill app and forget it
                apps.remove(app);
                app.removeAppStateChangeEventListener(this);
                app.stop(true);
            }
            else if (evt.getToState() == DVBJProxy.DESTROYED)
            {
                apps.remove(app);
                app.removeAppStateChangeEventListener(this);
            }
        }
    }

    private void stopApps()
    {
        for (Enumeration e = apps.keys(); e.hasMoreElements();)
        {
            DVBJProxy app = (DVBJProxy) e.nextElement();

            app.removeAppStateChangeEventListener(this);
            app.stop(true);
        }
        apps.clear();
    }

    private void registerApi() throws XletStateChangeException
    {
        RegisteredApiManager ram = RegisteredApiManager.getInstance();

        String version;
        String currVersion = ram.getVersion(APINAME);
        if (currVersion == null)
            version = APIVER;
        else
        {
            try
            {
                int number = Integer.parseInt(currVersion);
                version = "" + (number + 1);
            }
            catch (Exception e)
            {
                version = "0";
            }
        }
        badVersion = version + ".bad";

        try
        {
            ram.register(APINAME, version, new File("./scdf.xml"), (short) storagePriority);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException(e.getMessage());
        }
    }

    public synchronized void pauseXlet()
    {
        paused = true;
        stopApps();
    }

    public synchronized void destroyXlet(boolean forced) throws XletStateChangeException
    {
        stopApps();
        if (forced)
        {
            RegisteredApiManager ram = RegisteredApiManager.getInstance();

            try
            {
                ram.unregister(APINAME);
            }
            catch (Exception e)
            {
                System.err.println("Could not unregister API yet...");
                e.printStackTrace();
            }
        }
    }
}
