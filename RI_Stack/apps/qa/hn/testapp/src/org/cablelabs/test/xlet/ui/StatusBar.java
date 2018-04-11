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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class StatusBar
{

    private int y = 0; // top left corner of the status bar

    private int width;

    private int alignment;

    private Color bgColor;

    private Color fgColor;

    private Font font;

    private FontMetrics fm;

    private String message;

    /**
     * Specifies the staus bar height.
     */
    public static final int STATUS_BAR_HEIGHT = 50;

    /**
     * Specifies that the text in the status bar should be left aligned. This is
     * the default alignment.
     */
    public static final int ALIGN_LEFT = 0;

    /**
     * Specifies that the text in the status bar should be center aligned.
     */
    public static final int ALIGN_CENTER = 1;

    /**
     * Constructor will create a StatusBar. The location of the status bar is
     * always at the bottom of the screen and spans across the entire screen.
     * 
     */
    public StatusBar()
    {
        alignment = ALIGN_LEFT;
        bgColor = Color.darkGray;
        fgColor = Color.white;
        font = new Font("Tiresias", Font.BOLD, 14);
        y = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height - STATUS_BAR_HEIGHT;
        width = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
        message = "";
    }

    /**
     * Set the background color of the status bar.
     * 
     * @param newColor
     *            is the new background color to be used
     * 
     */
    public void setBGColor(final Color newColor)
    {
        if (newColor != null)
        {
            this.bgColor = newColor;
        }
    }

    /**
     * Get the current background color used.
     * 
     * @return current background color
     * 
     */
    public Color getBGColor()
    {
        return bgColor;
    }

    /**
     * Set the foreground color of the status bar.
     * 
     * @param newColor
     *            is the new foreground color to be used
     * 
     */
    public void setFGColor(final Color newColor)
    {
        if (newColor != null)
        {
            this.fgColor = newColor;
        }
    }

    /**
     * Get the current foreground color used.
     * 
     * @return current foreground color
     * 
     */
    public Color getFGColor()
    {
        return fgColor;
    }

    /**
     * Set the font for all list items.
     * 
     * @param newFont
     *            is the new font to be used
     * 
     */
    public void setFont(final Font newFont)
    {
        if (newFont != null)
        {
            this.font = newFont;
        }
    }

    /**
     * Get the current font used.
     * 
     * @return font is the current font
     * 
     */
    public Font getFont()
    {
        return font;
    }

    /**
     * Get the current text alignment.
     * 
     * @return current text alignment. One of <code>ALIGN_LEFT</code> or
     *         <code>ALIGN_CENTER</code>
     * 
     */
    public int getAlignment()
    {
        return alignment;
    }

    /**
     * Set the alignment of the text in the status bar. The list can be
     * ALIGN_LEFT or ALIGN_CENTER. The default alignment is set to ALIGN_LEFT.
     * 
     * @param align
     *            alignment specification. One of ALIGN_LEFT or ALIGN_CENTER.
     * 
     */
    public void setAlignment(final int align)
    {
        if (align == ALIGN_LEFT || align == ALIGN_CENTER)
        {
            alignment = align;
        }
    }

    /**
     * Set the message on the status bar. It will be displayed next time repaint
     * or update will be called.
     * 
     * @param msg
     *            message to be displayed
     * 
     */
    public void setMessage(final String msg)
    {
        if (msg == null)
        {
            message = "null";
        }
        else
        {
            message = msg;
        }
    }

    /**
     * Get current message from the status bar.
     * 
     * @return current text in the status bar
     * 
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * This is the method that will paint the status bar on the screen. It
     * should be invoked in the <code>appPaint()</code> method whenever the user
     * wishes to draw the menu.
     * 
     * @param g
     *            is the Graphics object used to draw the status bar
     * 
     */
    public void draw(final Graphics g)
    {
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        int strWidth;
        int myX = 50;
        int myY = y + 20;

        fm = g.getFontMetrics();
        strWidth = fm.stringWidth(message);
        if (alignment == ALIGN_CENTER)
        {
            myX = myX + (width - strWidth) / 2;
        }
        g.setColor(bgColor);
        g.fillRect(0, y, width, STATUS_BAR_HEIGHT);
        g.setColor(fgColor);
        g.setFont(font);
        g.drawString(message, myX, myY);

        g.setFont(oldFont);
        g.setColor(oldColor);
    }
}
