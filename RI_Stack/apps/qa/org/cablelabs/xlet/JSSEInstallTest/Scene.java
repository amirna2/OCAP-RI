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
package org.cablelabs.xlet.JSSEInstallTest;

//Import Personal Java packages.
import java.awt.*;
import java.util.*;

//Import OCAP packages.
import org.havi.ui.*;

/**
 * This sigleton class implements the display of an input string of words into a
 * centered, fixed size text window.
 * 
 */
public class Scene extends Component
{
    private static final Scene INSTANCE = new Scene();

    private HScene hscene = null;

    private Scene()
    {
    }// singleton

    public static Scene getInstance()
    {
        return INSTANCE;
    }

    private int MAX_LINES = 10;

    private String[] strings = new String[MAX_LINES];

    private Dimension TEXT_SIZE = new Dimension(380, 300);// text window
                                                          // dimension

    /**
     * Sets the viewable text. Replaces any previous text. Breaks the line up
     * into an array of strings. Each string fits within the width of the text
     * window.
     * 
     * @param line
     *            - must contain printable characters
     */
    public void setText(String line)
    {
        if (line != null)
        {
            for (int i = 0; i < strings.length; i++)
            {
                strings[i] = null;
            }

            FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(this.getFont());

            // if one line will fit within size width
            if (fm.stringWidth(line) <= TEXT_SIZE.width)
            {
                strings[0] = line;
            }
            // else the text needs to be wrapped into two lines
            else
            {
                // "\n" is delim for tokens
                StringTokenizer st = new StringTokenizer(line, "\n");
                strings[0] = "";
                // word wrap lines up to two lines
                int y = 0;// line1Baseline;
                int lineNum = 0;
                while (st.hasMoreTokens() && lineNum < MAX_LINES)
                {
                    String nextWord = st.nextToken();
                    // check that the line is within horizontal size
                    if (fm.stringWidth(nextWord) + fm.stringWidth(strings[lineNum]) <= TEXT_SIZE.width)
                    {
                        strings[lineNum] += nextWord + " ";// this will leave a
                                                           // space at the end
                                                           // of last word, it
                                                           // does no harm
                                                           // though
                    }
                    else
                    // paint previous line and start next line
                    {
                        lineNum++;
                        strings[lineNum] = nextWord + " ";// start next line
                    }
                }
            }
            repaint();
        }
    }

    /**
     * display or don't display, caller decides
     */
    public void setVisible(boolean bVis)
    {
        if (hscene != null)
        {
            System.out.println("Scene setVisible() " + bVis);

            if (bVis)
                hscene.show();
            else
                hscene.setVisible(bVis);
            this.requestFocus();

        }
    }

    /**
     * clean up and die
     */
    public void destroy()
    {
        hscene.setVisible(false);
        HSceneFactory.getInstance().dispose(hscene);
        hscene = null;
    }

    /**
     * paint the strings into the text window
     */
    public void paint(Graphics g)
    {
        // System.out.println("Scene paint()");
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(this.getFont());
        Rectangle r = this.getBounds();
        g.setColor(Color.blue);
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(Color.orange);
        int textX = (r.width - TEXT_SIZE.width) / 2;
        int textY = (r.height - TEXT_SIZE.height) / 2;
        g.fillRect(textX, textY, TEXT_SIZE.width, TEXT_SIZE.height);
        int y = textY + fm.getHeight();
        // g.setColor(Color.black);
        g.setColor(new Color(0, 0, 1));
        for (int i = 0; i < strings.length; i++)
        {
            if (strings[i] == null) break;
            g.drawString(strings[i], textX, y);
            y += fm.getHeight();
        }
    }

    /**
     * initializes the scene for display
     * 
     */
    public void initialize()
    {
        hscene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        hscene.setBounds(0, 0, 640, 480);// full screen hscene
        this.setBounds(0, 0, 640, 480);
        hscene.add(this);

    }
}
