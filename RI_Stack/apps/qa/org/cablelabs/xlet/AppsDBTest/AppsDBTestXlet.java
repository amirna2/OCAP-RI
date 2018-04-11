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
package org.cablelabs.xlet.AppsDBTest;

import java.awt.*;
import java.awt.Component;
import java.util.*;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.dvb.application.*;

public class AppsDBTestXlet extends Component implements Xlet
{
    private static final Font m_font = new Font("SansSerif", Font.BOLD, 16);

    private HScene m_scene;

    private AppsDatabase m_database = null;

    public void initXlet(XletContext ctx)
    {
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setVisible(false);

        this.setBounds(0, 0, 640, 480);
        this.setBackground(Color.green);
        this.setForeground(Color.black);
        this.setFont(m_font);

        m_scene.add(this);
    }

    public void startXlet()
    {
        /** Get SIManager */
        try
        {
            m_database = AppsDatabase.getAppsDatabase();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        m_scene.show();
        m_scene.repaint();
        m_scene.requestFocus();

        log();
    }

    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    public void destroyXlet(boolean unconditional)
    {
        m_scene.dispose();
    }

    /*
     * Display contents of application database on TV screen.
     */
    public void paint(Graphics g)
    {
        g.setColor(Color.green);
        g.fillRect(0, 0, 640, 480);
        g.setFont(m_font);
        g.setColor(Color.black);

        int xpos = 60;
        int ypos = 80;
        int yspace = 20;
        int xspace = 130;

        g.drawString("AppName", xpos, ypos);
        xpos += xspace;
        g.drawString("AppID", xpos, ypos);
        xpos += xspace;
        g.drawString("isStartable", xpos, ypos);
        xpos += xspace;
        g.drawString("isServiceBound", xpos, ypos);
        xpos = 60;

        if (m_database != null)
        {
            Enumeration attributes = m_database.getAppAttributes(new CurrentServiceFilter());
            if (attributes != null)
            {
                while (attributes.hasMoreElements())
                {
                    AppAttributes info;
                    info = (AppAttributes) attributes.nextElement();

                    ypos += yspace;
                    g.drawString(info.getName(), xpos, ypos);
                    xpos += xspace;
                    g.drawString((info.getIdentifier()).toString(), xpos, ypos);
                    xpos += xspace;

                    if (true == info.isStartable())
                    {
                        g.drawString("true", xpos, ypos);
                    }
                    else
                    {
                        g.drawString("false", xpos, ypos);
                    }

                    xpos += xspace;

                    if (true == info.getIsServiceBound())
                    {
                        g.drawString("true", xpos, ypos);
                    }
                    else
                    {
                        g.drawString("false", xpos, ypos);
                    }

                    xpos = 60;
                }
            }
        }
    }

    /*
     * Print contents of application database to a console window.
     */
    public void log()
    {
        System.out.println("\n\n********************************************************************************");
        System.out.println("AppsDatabase Test Xlet:\n");

        if (m_database != null)
        {
            Enumeration attributes = m_database.getAppAttributes(new CurrentServiceFilter());

            if (attributes != null)
            {
                while (attributes.hasMoreElements())
                {
                    AppAttributes info;
                    info = (AppAttributes) attributes.nextElement();

                    System.out.println("AppName: " + info.getName());
                    System.out.println("AppID: " + (info.getIdentifier()).toString());

                    if (info.isStartable() == true)
                    {
                        System.out.println("isStartable = true");
                    }
                    else
                    {
                        System.out.println("isStartable = false");
                    }

                    if (true == info.getIsServiceBound())
                    {
                        System.out.println("isServiceBound = true\n");
                    }
                    else
                    {
                        System.out.println("isServiceBound = false\n");
                    }
                }
            }
        }
        else
        {
            System.out.println("Application Database Not Found");
        }
        System.out.println("********************************************************************************\n");
    }
}
