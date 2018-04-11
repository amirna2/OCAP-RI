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

package org.cablelabs.xlet.DvrExerciser;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;

import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;

/**
 * A component that can be used to display a scrolling list of messages on the
 * screen.
 * 
 * Usage notes: 1. Create an instance of this class. 2. Size the component
 * appropriately using setBounds(). 3. Add the component to a visible parent
 * Container. 4. Invoke addMessage() to add a message to be displayed.
 * 
 * Note: The component today only scrolls messages in one direction, so
 * unobserved messages will be permanently lost. There is currently no ability
 * for users to scroll the messages up/down.
 * 
 * @author andy
 * 
 */
public class Messenger extends org.havi.ui.HComponent
{
    /**
     * Added to silence the compiler
     */
    private static final long serialVersionUID = -4721013642095137550L;

    // default background color is gray
    private static final Color COLOR_BACKGROUND = new DVBColor(128, 128, 128, 255);// gray,
                                                                                   // opaque

    // default foreground color is white
    private static final Color COLOR_FOREGROUND = Color.white;

    // the maximum number of chars in message.
    private static final int MAX_MESSAGE_WIDTH = 100;

    // the maximum number of chars in message.
    private static final String MESSAGE_INDENT = "     ";

    // vector containing the messages
    private final Vector m_vectMessages;

    // number of messages visible on the component
    private int m_iMaxMessageCount = -1;

    // height in pixels of a line of text
    private int m_lineheight = 0;

    // private Runnable m_repainter = null;

    /**
     * Constructor sets the initial bounds and colors.
     */
    public Messenger()
    {
        super();
        setBackground(COLOR_BACKGROUND);
        setForeground(COLOR_FOREGROUND);

        setBounds(150, 150, 150, 150);
        m_vectMessages = new Vector();

        // m_repainter = new Runnable()
        // {
        // public void run()
        // {
        // repaint();
        // }
        // };
    }

    /**
     * Changing the bounds may result in a different number of messages being
     * visible. This routine resets the number of visible messages count, then
     * invokes the superclass' setBounds() routine.
     * 
     * @param x
     *            horizontal position of the left edge of the component.
     * @param y
     *            vertical positiono of the top edge of the component.
     * @param width
     *            component width.
     * @param height
     *            compoentn height.
     */
    public void setBounds(int x, int y, int width, int height)
    {
        m_iMaxMessageCount = -1;
        super.setBounds(x, y, width, height);
    }

    /**
     * Adds a message to be displayed.
     * 
     * @param message
     */
    public void addMessage(String message)
    {

        // String message;

        // limit maximum message width
        if (MAX_MESSAGE_WIDTH < message.length())
        {
            message = message.substring(0, MAX_MESSAGE_WIDTH - 1);
        }
        // else
        // {
        // message = message;
        // }

        message = MESSAGE_INDENT + message;

        // add the message to the message queue
        synchronized (m_vectMessages)
        {
            m_vectMessages.add(message);
        }

        // display the message in the context of the UI thread
        // java.awt.EventQueue.invokeLater(m_repainter);
        repaint();
    }

    /**
     * Paint the contents of this component.
     */
    public void paint(Graphics g)
    {
        Rectangle rectBounds;
        DVBGraphics dvbG = (DVBGraphics) g;

        // redraw the component using the background color
        dvbG.setColor(getBackground());
        rectBounds = getBounds();
        dvbG.fillRect(0, 0, rectBounds.width, rectBounds.height);

        // if the max message count needs to be calculated...
        if (-1 == m_iMaxMessageCount)
        {
            // ...get the font sizing
            FontMetrics m = g.getFontMetrics();

            // calculate the height of a line in pixels
            m_lineheight = m.getLeading() + m.getAscent();

            // calculate how many messages can be displayed in this component
            m_iMaxMessageCount = rectBounds.height / m_lineheight;
        }

        synchronized (m_vectMessages)
        {
            // calculate the starting index (offset) in the vector of messages
            int startIndex = m_vectMessages.size() - m_iMaxMessageCount;
            if (0 > startIndex)
            {
                startIndex = 0;
            }

            // set the drawing color to the foreground color
            dvbG.setColor(getForeground());

            // display the text messages
            for (int i = 0; i < m_iMaxMessageCount; i++)
            {
                if ((startIndex + i) >= m_vectMessages.size())
                {
                    break;
                }
                String message = (String) m_vectMessages.get(i + startIndex);
                // System.out.println("String..." + message);
                dvbG.drawString(message, 0, (i + 1) * m_lineheight);
            }
        }
        super.paint(g);
    }
}
