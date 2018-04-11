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

package org.cablelabs.xlet.SimpleTest;

import org.cablelabs.lib.utils.ArgParser;
import java.awt.*;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

/**
 * SimpleTestXlet takes two arguments: BGCOLOR and MESSAGE. It paints the
 * background a given color, and displays a simple message at the top of the
 * screen.
 */

public class SimpleTestXlet extends Container implements javax.tv.xlet.Xlet
{
    private static final String BGCOLOR = "BGCOLOR";

    private static final String MESSAGE = "MESSAGE";

    private String m_bg_color;

    private String m_msg;

    private HScene m_scene;

    private static final String DIVIDER = "***********************************************************************\n";

    public void initXlet(XletContext ctx)
    {
        try
        {
            // Get hostapp parameters.
            ArgParser parser = new ArgParser((String[]) ctx.getXletProperty(ctx.ARGS));
            m_bg_color = parser.getStringArg(BGCOLOR);
            m_msg = parser.getStringArg(MESSAGE);

            // Set my Container's bounds
            setBounds(0, 0, 640, 480);

            // Create HScene
            m_scene = HSceneFactory.getInstance().getDefaultHScene();
            m_scene.add(this);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.out.println(DIVIDER + "SimpleTestXlet::initXlet().\nMessage: " + m_msg + "\nColor: " + m_bg_color + "\n"
                + DIVIDER);
    }

    public void startXlet()
    {
        System.out.println(DIVIDER + "SimpleTestXlet::startXlet()\n" + DIVIDER);
        m_scene.setVisible(true);
        m_scene.show();
        m_scene.repaint();
    }

    public void pauseXlet()
    {
        System.out.println(DIVIDER + "SimpleTestXlet::pauseXlet()\n" + DIVIDER);
        m_scene.setVisible(false);
    }

    public void destroyXlet(boolean unconditional)
    {
        System.out.println(DIVIDER + "SimpleTestXlet::destroyXlet()\n" + DIVIDER);
        m_scene.setVisible(false);
        m_scene.dispose();
    }

    public void paint(Graphics graphics)
    {
        try
        {
            // Cover screen with background color.
            graphics.setColor(getColor(m_bg_color, Color.white));
            graphics.fillRect(0, 0, 640, 480);

            // Draw box at the top of the screen.
            int xpos = 40;
            int ypos = 40;

            graphics.setColor(Color.orange);
            graphics.fillRect(xpos, ypos, 560, 50);

            // Print message in the box.
            xpos = 60;
            ypos = 70;

            graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
            graphics.setColor(Color.black);
            graphics.drawString(m_msg, xpos, ypos);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Color getColor(String color, Color defaultColor)
    {
        if ("black".equals(color.toLowerCase()))
        {
            return Color.black;
        }

        else if ("blue".equals(color.toLowerCase()))
        {
            return Color.blue;
        }

        else if ("cyan".equals(color.toLowerCase()))
        {
            return Color.cyan;
        }

        else if ("darkgray".equals(color.toLowerCase()))
        {
            return Color.darkGray;
        }

        else if ("gray".equals(color.toLowerCase()))
        {
            return Color.gray;
        }

        else if ("grey".equals(color.toLowerCase()))
        {
            return Color.gray;
        }

        else if ("green".equals(color.toLowerCase()))
        {
            return Color.green;
        }

        else if ("lightgray".equals(color.toLowerCase()))
        {
            return Color.lightGray;
        }

        else if ("magenta".equals(color.toLowerCase()))
        {
            return Color.magenta;
        }

        else if ("orange".equals(color.toLowerCase()))
        {
            return Color.orange;
        }

        else if ("pink".equals(color.toLowerCase()))
        {
            return Color.pink;
        }

        else if ("red".equals(color.toLowerCase()))
        {
            return Color.red;
        }

        else if ("white".equals(color.toLowerCase()))
        {
            return Color.white;
        }

        else if ("yellow".equals(color.toLowerCase()))
        {
            return Color.yellow;
        }

        else
        {
            return defaultColor;
        }
    }
}
