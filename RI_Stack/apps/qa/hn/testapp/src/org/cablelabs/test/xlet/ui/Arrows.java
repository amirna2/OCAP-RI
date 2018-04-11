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

package org.cablelabs.test.xlet.ui;

import java.awt.Graphics;

/**
 * Helper class for drawing filled arrows.
 */
public final class Arrows
{

    /**
     * Don't create instances of this helper class.
     */
    private Arrows()
    {
    }

    /**
     * Draws a filled up arrow.
     * 
     * @param g
     *            The graphics context.
     * @param x
     *            The x-coordinate of this arrow.
     * @param y
     *            The y-coordinate of this arrow.
     * @param width
     *            The width of this arrow.
     * @param height
     *            The height of this arrow.
     */
    public static void upArrow(final Graphics g, final int x, final int y, final int width, final int height)
    {
        int[] tempX = new int[3];
        int[] tempY = new int[3];
        tempX[0] = x;
        tempX[1] = x + (width >> 1);
        tempX[2] = x + width;
        tempY[0] = y + height;
        tempY[1] = y;
        tempY[2] = y + height;
        g.fillPolygon(tempX, tempY, 3);
    }

    /**
     * Draws a filled down arrow.
     * 
     * @param g
     *            The graphics context.
     * @param x
     *            The x-coordinate of this arrow.
     * @param y
     *            The y-coordinate of this arrow.
     * @param width
     *            The width of this arrow.
     * @param height
     *            The height of this arrow.
     */
    public static void downArrow(final Graphics g, final int x, final int y, final int width, final int height)
    {
        int[] tempX = new int[3];
        int[] tempY = new int[3];
        tempX[0] = x;
        tempX[1] = x + (width >> 1);
        tempX[2] = x + width;
        tempY[0] = y;
        tempY[1] = y + height;
        tempY[2] = y;
        g.fillPolygon(tempX, tempY, 3);
    }

    /**
     * Draws a filled left arrow.
     * 
     * @param g
     *            The graphics context.
     * @param x
     *            The x-coordinate of this arrow.
     * @param y
     *            The y-coordinate of this arrow.
     * @param width
     *            The width of this arrow.
     * @param height
     *            The height of this arrow.
     */
    public static void leftArrow(final Graphics g, final int x, final int y, final int width, final int height)
    {
        int[] tempX = new int[3];
        int[] tempY = new int[3];
        tempX[0] = x + width;
        tempX[1] = x;
        tempX[2] = x + width;
        tempY[0] = y - height;
        tempY[1] = y - (height >> 1);
        tempY[2] = y;
        g.fillPolygon(tempX, tempY, 3);
    }

    /**
     * Draws a filled right arrow.
     * 
     * @param g
     *            The graphics context.
     * @param x
     *            The x-coordinate of this arrow.
     * @param y
     *            The y-coordinate of this arrow.
     * @param width
     *            The width of this arrow.
     * @param height
     *            The height of this arrow.
     */
    public static void rightArrow(final Graphics g, final int x, final int y, final int width, final int height)
    {
        int[] tempX = new int[3];
        int[] tempY = new int[3];
        tempX[0] = x;
        tempX[1] = x + width;
        tempX[2] = x;
        tempY[0] = y - height;
        tempY[1] = y - (height >> 1);
        tempY[2] = y;
        g.fillPolygon(tempX, tempY, 3);
    }
}
