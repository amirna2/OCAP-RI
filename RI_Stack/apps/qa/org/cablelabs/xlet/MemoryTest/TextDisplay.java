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

// Declare package.
package org.cablelabs.xlet.MemoryTest;

// Import Personal Java packages.
import java.awt.*;
import java.awt.event.*;
import javax.tv.xlet.*;
import javax.tv.util.*;
import java.util.*;

// Import OCAP packages.
import org.havi.ui.*;
import org.dvb.ui.*;

/**
 * TextDisplay is a sort-of generic class for displaying text on the screen as
 * well as to the console. It facilitates the display of short text strings in a
 * tabular format. The table can also have one or more headings. One could use
 * this class, without the table formatting, by simply emitting a series of
 * headers.
 */
class TextDisplay
{
    private Graphics g;

    private Dimension size;

    private FontMetrics fm;

    private Rectangle clipRect;

    private int yRun;

    private int yGap;

    private int cols;

    private int x, y;

    private int row, col;

    private int colIndent;

    private int colWidth;

    private int headIndent;

    /**
     * Start the display of text. Needs the Graphics object as well as the
     * overall size of the display.
     * 
     * @param g
     *            Graphics object for the current display. The Font must already
     *            be set to the Graphics in order to use it successfully.
     * 
     * @param size
     *            The dimensions of the display area to use.
     */
    public TextDisplay(Graphics g, Dimension size)
    {
        this.g = g;
        this.size = size;
        fm = g.getFontMetrics();
        yRun = fm.getAscent();
        clipRect = g.getClipBounds();
        x = 0;
        y = yRun;
    }

    /**
     * Initialize parameters for a table to follow. This call is optional, but
     * table cells will not be shown until it has been called.
     * 
     * @param headIndent
     *            The number of pixels that the table header should be indented.
     * @param cols
     *            The number of columns for the table.
     * @param colIndent
     *            The number of pixels to indent rows for the table.
     * @param yGap
     *            The number of pixels to skip between rows.
     */
    public void setupTable(int headIndent, int cols, int colIndent, int yGap)
    {
        this.headIndent = headIndent;
        this.cols = cols;
        this.colIndent = colIndent;
        this.yGap = yGap;
        colWidth = (size.width - colIndent) / cols;
    }

    /**
     * Displays a string as a table heading (or footer for that matter). It
     * indents the heading by headIndent pixel as per setupTable().
     * 
     * @param heading
     *            String to display as a header/footer.
     */
    public void displayHeading(String heading)
    {
        x = headIndent;
        boolean drew = displayText(heading);

        if (drew) System.out.println(heading);

        y += yRun;
        col = 0;
    }

    /**
     * Display some text at the current x and y. Takes into account the current
     * clipping rectangle and doesn't draw the text if it would be completely
     * clipped.
     * 
     * @param str
     *            String to display.
     * 
     * @return boolean indication whether or not the string drew on the display.
     */
    private boolean displayText(String str)
    {
        Rectangle textRect = new Rectangle(x, y - fm.getAscent(), x + fm.stringWidth(str), y + fm.getDescent());
        Rectangle drawRect = textRect.intersection(clipRect);
        if (drawRect.isEmpty()) return false;

        g.setClip(drawRect);
        g.drawString(str, x, y);
        g.setClip(clipRect);

        return true;
    }

    /**
     * Display a String into the next cell of the current table. Table cells
     * fill from left to right and top to bottom. If the string is longer than
     * the cell, it is clipped to the cell's rectangle.
     * 
     * @param str
     *            String to display in the next cell.
     */
    public void displayCell(String str)
    {
        boolean drew;

        x = colIndent + (col * colWidth);
        drew = displayCellText(str);
        if (drew)
        {
            if (col == 0)
                System.out.print("    ");
            else
                System.out.print("  ");

            System.out.print(str);
        }

        ++col;
        if (col >= cols)
        {
            y += yRun;
            col = 0;

            if (drew) System.out.print('\n');
        }
    }

    /**
     * Display a String in a table cell. Takes the Graphics' clipping rectangle
     * into account and doesn't draw anything if it shouldn't appear.
     * 
     * @param str
     *            String to display.
     * 
     * @return boolean indication whether or not the string drew on the display.
     */

    private boolean displayCellText(String str)
    {
        Rectangle textRect = new Rectangle(x, y - fm.getAscent(), x + ((size.width - colIndent) / cols), y
                + fm.getDescent());
        Rectangle drawRect = textRect.intersection(clipRect);

        if (drawRect.isEmpty()) return false;

        g.setClip(drawRect);
        g.drawString(str, x, y);
        g.setClip(clipRect);

        return true;
    }

    /**
     * Move the "cursor" to the beginning of the next line. This is useful after
     * writing a series of cells to move the "cursor" to the start of the line
     * after the table.
     */
    public void newline()
    {
        if (col != 0)
        {
            y += yRun;
            col = 0;

            System.out.print('\n');
        }
    }
}
