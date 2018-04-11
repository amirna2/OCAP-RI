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

package org.cablelabs.xlet.RiExerciser.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.dvb.ui.DVBGraphics;
import org.havi.ui.HComponent;

/*
 * Displays a horizontal bar graph component.
 * 
 * Usage notes:
 * 1. Create an instance of this class and add it to a container.
 * 2. Set the component bounds as desired - this will define the dimensions
 * of the bar graph.
 * 3. Call <code>setCompletionRatio()</code> to update the graph with the 
 * current level of completion.
 */
public class Bargraph extends HComponent
{
    // outline color
    private static final Color s_outlineColor = Color.ORANGE;

    // background color
    private static final Color s_backgroundColor = Color.BLACK;

    // indicator color
    private static final Color s_barColor = Color.RED;

    // provided to shut up the compiler
    private static final long serialVersionUID = 3936937912585159603L;

    // the completion ratio, normalized to 1.0
    private float m_completionRatio;

    /**
     * Sets the completion ratio of the indicator. The completion ratio will be
     * clamped to range from 0.0 to 1.0. Note that calling this method will
     * cause this component to be repainted.
     * 
     * @param completionRatio
     *            current completionRatio.
     */
    public void setCompletionRatio(float completionRatio)
    {
        if (0 > completionRatio)
        {
            m_completionRatio = 0.0f;
        }
        else if (1.0 < completionRatio)
        {
            m_completionRatio = 1.0f;
        }
        else
        {
            m_completionRatio = completionRatio;
        }
        repaint();
    }

    /**
     * Paint this component on the screen
     */
    public void paint(Graphics g)
    {
        Rectangle rectBounds;

        DVBGraphics dvbG = (DVBGraphics) g;

        rectBounds = getBounds();

        // draw the outline of the bar graph
        dvbG.setColor(s_outlineColor);
        dvbG.drawRect(1, 0, rectBounds.width - 2, rectBounds.height - 1);

        // draw the background
        dvbG.setColor(s_backgroundColor);
        dvbG.fillRect(2, 1, rectBounds.width - 3, rectBounds.height - 2);

        // draw the bar indicator
        dvbG.setColor(s_barColor);
        dvbG.fillRect(2, 1, (int) ((rectBounds.width - 3) * m_completionRatio), rectBounds.height - 2);
    }
}
