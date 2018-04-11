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

/*
 * Created on Jul 11, 2005
 */
package org.cablelabs.xlet.apiclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.lang.reflect.Method;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HStaticText;
import org.ocap.system.RegisteredApiManager;

/**
 * @author Aaron Kamienski
 */
public class ApiTestXlet implements Xlet
{
    private XletContext xc;

    private boolean started = false;

    private HScene scene;

    private int x;

    private int y;

    private int width;

    private int height;

    private String name;

    private String apiName;

    private String apiVer;

    private boolean expectSuccess;

    public synchronized void initXlet(XletContext xc)
    {
        this.xc = xc;
    }

    public synchronized void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            parseArgs();
            testApi();
        }
        show();
        started = true;
    }

    private void testApi()
    {
        HSceneFactory factory = HSceneFactory.getInstance();
        HSceneTemplate template = new HSceneTemplate();
        template.setPreference(HSceneTemplate.SCENE_PIXEL_LOCATION, new Point(x, y), HSceneTemplate.PREFERRED);
        template.setPreference(HSceneTemplate.SCENE_PIXEL_DIMENSION, new Dimension(width, height),
                HSceneTemplate.PREFERRED);
        scene = factory.getBestScene(template);

        scene.setLayout(new BorderLayout());

        String reason = accessApi();
        boolean success = (reason == null) == expectSuccess;
        if (reason == null)
            reason = "SUCCESS";
        else
            reason = "FAILED:\n" + reason;

        RegisteredApiManager ram = RegisteredApiManager.getInstance();
        String[] apiNames = ram.getUsedNames();
        String apis = "";
        for (int i = 0; i < apiNames.length; ++i)
        {
            apis = "\n" + apiNames[i] + ":" + ram.getVersion(apiNames[i]);
        }

        HStaticText text = new HStaticText("Name=" + name + "\n" + reason + apis);
        text.setForeground(Color.lightGray);
        text.setBackground(success ? Color.green.darker() : Color.red.darker());
        text.setBackgroundMode(HStaticText.BACKGROUND_FILL);
        text.setHorizontalAlignment(HStaticText.HALIGN_CENTER);
        scene.add(text);

        scene.addNotify();
        scene.validate();
    }

    private String accessApi()
    {
        try
        {
            Class testClass = Class.forName(apiName);
            if (testClass == null) return "class not found";

            // Call testStaticAccess()
            Method staticAccess = testClass.getMethod("testStaticAccess", new Class[0]);
            if (!((Boolean) staticAccess.invoke(null, new Object[0])).booleanValue()) return "static method test";

            // Create a new instance
            Object instance = testClass.newInstance();
            if (instance == null) return "newInstance failed";

            // Call testAccess
            Method access = testClass.getMethod("testAccess", new Class[0]);
            if (!((Boolean) access.invoke(instance, new Object[0])).booleanValue()) return "instance method test";

            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return e.toString();
        }
    }

    private void parseArgs()
    {
        parseArgs((String[]) xc.getXletProperty("dvb.caller.parameters"));
        parseArgs((String[]) xc.getXletProperty(XletContext.ARGS));
    }

    private void parseArgs(String[] args)
    {
        if (args == null || args.length < 4) return;
        name = args[0];
        apiName = args[1];
        apiVer = args[2];
        expectSuccess = Boolean.valueOf(args[3]).booleanValue();
        for (int i = 4; i < args.length; ++i)
        {
            if (args[i].startsWith("height="))
            {
                height = Integer.parseInt(args[i].substring(7));
            }
            else if (args[i].startsWith("width="))
            {
                width = Integer.parseInt(args[i].substring(6));
            }
            else if (args[i].startsWith("x="))
            {
                x = Integer.parseInt(args[i].substring(2));
            }
            else if (args[i].startsWith("y="))
            {
                y = Integer.parseInt(args[i].substring(2));
            }
        }
    }

    public synchronized void pauseXlet()
    {
        hide();
    }

    private synchronized void show()
    {
        if (scene != null) scene.show();
    }

    private synchronized void hide()
    {
        if (scene != null) scene.setVisible(false);
    }

    private synchronized void destroy()
    {
        if (scene != null) scene.dispose();
        scene = null;
    }

    public synchronized void destroyXlet(boolean forced) throws XletStateChangeException
    {
        destroy();
    }
}
