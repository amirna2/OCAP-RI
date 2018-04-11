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

package org.cablelabs.gear.havi.decorator;

import java.awt.*;
import org.havi.ui.*;

/**
 * The {@link ProgressLook} class fills the control to display a percentage of
 * current value in relation to min/max value. The foreground color is used for
 * rendering.
 * 
 * @author Tom Henriksen
 * @author Jeff Bonin (havi 1.0.1 update)
 * @version $Id: ProgressLook.java,v 1.3 2002/06/03 21:32:30 aaronk Exp $
 */
public class ProgressLook extends org.havi.ui.HRangeLook
{

    /**
     * Creates a {@link ProgressLook} that will be used for rendering the
     * current value within a range.
     */
    public ProgressLook()
    {
    }

    /**
     * This function solves for x in the equation "x is y% of z".
     */
    private int solveForX(int Y, int Z)
    {
        return (int) ((Z * Y) / 100);
    }

    /**
     * This function solves for y in the equation "x is y% of z".
     */
    private int solveForY(int X, int Z)
    {
        return (Z == 0) ? 0 : (int) ((X * 100) / Z);
    }

    // Description copied from superclass/interface
    public void showLook(java.awt.Graphics g, HVisible visible, int state)
    {

        if (visible instanceof HStaticRange)
        {
            int FillSize = 0;
            HStaticRange range = (HStaticRange) visible;
            Dimension dim = range.getSize();

            Insets insets = getInsets(visible);
            int X = insets.left;
            int Y = insets.top;
            int W = dim.width - (insets.left + insets.right);
            int H = dim.height - (insets.bottom + insets.top);

            int PercentDone = solveForY(range.getValue() - range.getMinValue(), range.getMaxValue()
                    - range.getMinValue());

            g.setColor(visible.getForeground());

            switch (range.getOrientation())
            {
                case HStaticRange.ORIENT_LEFT_TO_RIGHT:
                case HStaticRange.ORIENT_RIGHT_TO_LEFT:
                    FillSize = solveForX(PercentDone, W);
                    if (FillSize > W) FillSize = W;
                    if (FillSize > 0) g.fillRect(X, Y, FillSize, H);
                    break;
                case HStaticRange.ORIENT_TOP_TO_BOTTOM:
                case HStaticRange.ORIENT_BOTTOM_TO_TOP:
                    FillSize = solveForX(PercentDone, H);
                    if (FillSize > H) FillSize = H;
                    if (FillSize > 0) g.fillRect(X, Y + (H - FillSize), W, FillSize);
                    break;
                default:
                    break;
            }
        }
    }
}
