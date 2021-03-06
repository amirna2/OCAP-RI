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

import java.awt.Graphics;
import java.awt.Rectangle;

import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;

public class RemoteServiceStatusBox extends org.havi.ui.HComponent
{
    private static final long serialVersionUID = 1924960809423460899L;

    // text color is white
    private final Color COLOR_TEXT = Color.black;

    protected static final String MSG_INIT = "Press PLAY to start remote playback";

    protected static final Color COLOR_INIT = new DVBColor(128, 128, 128, 155);

    protected static final Color COLOR_PAUSED = Color.yellow;

    protected static final String MSG_PLAY_0 = "Paused: 0.0";

    protected static final String MSG_PLAY_1 = "Playing: 1.0";

    protected static final String MSG_EOS = "EOS - Press STOP or REWIND";

    protected static final String MSG_BOS = "BOS - Press PLAY, FAST FWD or STOP";

    protected static final String MSG_PLAY_2_PREFIX = "Fast Fwd: ";

    protected static final String MSG_PLAY_3_PREFIX = "Rewind: ";
    
    protected static final String MSG_PLAY_4 = "Skip forward 10 seconds";
    
    protected static final String MSG_PLAY_5 = "Skip backwards 10 seconds";

    // protected static final String MSG_PLAY_4 = "Chunking: 1.0";
    protected static final String MSG_PENDING = "Play Starting";

    protected static final Color COLOR_PLAY = Color.green;

    protected static final String MSG_FAILED = "Playback Failed";

    protected static final String MSG_FAILED_RATE = "Rate Change Failed";

    protected static final String MSG_STOP = "Stopped";

    protected static final Color COLOR_STOP = Color.red;

    protected static final Color COLOR_PENDING = Color.blue;

    private String m_message = MSG_INIT;

    /**
     * Constructor sets the initial bounds and colors.
     */
    public RemoteServiceStatusBox(int x, int y, int width, int height)
    {
        super();
        setBackground(COLOR_INIT);
        setForeground(COLOR_INIT);
        setMessage(MSG_INIT);
        setBounds(x, y, width, height);
    }

    /**
     * Sets the string to be displayed on top of the spiral.
     * 
     * @param message
     *            the message to be displayed.
     */
    public void setMessage(String message)
    {
        m_message = message;
    }

    public void setColor(Color c)
    {
        setBackground(c);
        setForeground(c);
    }

    public void update(Color c, String message)
    {
        setMessage(message);
        setColor(c);
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

        // set the drawing color to the foreground color
        dvbG.setColor(getForeground());

        // determine the center of the component
        // int centerX = rectBounds.width/2;
        int centerX = 10;
        int centerY = rectBounds.height / 2 + 7;

        // display text
        dvbG.setColor(COLOR_TEXT);
        // dvbG.drawString(m_message,
        // centerX - (g.getFontMetrics().stringWidth(m_message))/2,
        // centerY - (g.getFontMetrics().getHeight())/2);

        dvbG.drawString(m_message, centerX, centerY);
        super.paint(dvbG);
    }
}
