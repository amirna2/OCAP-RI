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

import org.dvb.ui.DVBGraphics;

public class Spiral extends org.havi.ui.HComponent implements Runnable
{
    /**
	 * 
	 */
    private static final long serialVersionUID = 1924960809423460899L;

    private boolean m_rotate = false;

    // default background color is green
    private final Color COLOR_BACKGROUND = new Color(0, 128, 0);

    // default foreground color is black
    private final Color COLOR_FOREGROUND = Color.red;

    // text color is white
    private final Color COLOR_TEXT = Color.black;

    // determines how much the spiral 'fills' the component -
    // for the general case, this should probably be calculated
    // ratiometrically w.r.t. the size of the component
    private static final float SCALE_FACTOR = 0.25f;

    private double PI = 3.141592654;

    // keeps track of value causing spiral to 'spin'
    private double m_spin = 0;

    private String m_message = "This is a message";

    /**
     * Constructor sets the initial bounds and colors.
     */
    public Spiral()
    {
        super();
        setBackground(COLOR_BACKGROUND);
        setForeground(COLOR_FOREGROUND);

        setBounds(150, 150, 150, 150);
    }

    /**
     * Starts the spiral rotating.
     * 
     * 
     */
    public void startSpiral()
    {
        // ...kick off a background thread do do the rotation
        Thread rotator = new Thread(this);
        rotator.start();
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

    /**
     * Mutator for the <code>rotate</code> state.
     * 
     * @param rotate
     *            set to <code>true</code> to allow the spiral to rotate,
     *            <code>false</code> otherwise.
     */
    public void setRotate(boolean rotate)
    {
        m_rotate = rotate;
    }

    /**
     * run() method for spinning the spiral. This method is run in the context
     * of its own thread.
     */
    public void run()
    {
        // runnable class to be invoked in the context of the UI thread

        for (;;)
        {
            try
            {
                // re-awaken every 50 msec
                Thread.sleep(50);
                if (true == m_rotate)
                {
                    m_spin += .1;

                    // to keep m_spin from eventually overflowing
                    if ((2 * PI) <= m_spin)
                    {
                        m_spin = 0;
                    }

                    repaint();
                }
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
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
        // dvbG.fillRoundRect(0, 0, rectBounds.width, rectBounds.height, 50,
        // 50);
        dvbG.fillRect(0, 0, rectBounds.width, rectBounds.height);

        // set the drawing color to the foreground color
        dvbG.setColor(getForeground());

        // determine the center of the component
        int centerX = rectBounds.width / 2;
        int centerY = rectBounds.height / 2;

        // draw spiral
        for (int i = 0; i < 250 * PI; i++)
        {
            float sin = (float) Math.cos(m_spin + .03 * i);
            float cos = (float) Math.sin(m_spin + .03 * i);
            float exp_cos = i * SCALE_FACTOR * cos;
            float exp_sin = i * SCALE_FACTOR * sin;
            int x = Math.round(exp_cos);
            int y = Math.round(exp_sin);

            dvbG.fillOval((int) x + centerX, (int) y + centerY, 5, 5);
        }

        // display text
        dvbG.setColor(COLOR_TEXT);
        dvbG.drawString(m_message, centerX - (g.getFontMetrics().stringWidth(m_message)) / 2, centerY
                - (g.getFontMetrics().getHeight()) / 2);

        super.paint(dvbG);
    }
}
