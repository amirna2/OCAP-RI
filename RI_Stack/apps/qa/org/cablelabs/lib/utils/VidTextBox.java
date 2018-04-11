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

package org.cablelabs.lib.utils;

import java.awt.Color;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBGraphics;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.MediaTracker;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.Container;
import java.net.URL;
import java.lang.String;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.tv.xlet.*;
import org.havi.ui.*;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

//
//--------------------------------------------------------------------------
//
//	VidTextBox class
//
//	Usage:
//		VidTextBox m_tb();				...Makes up defaults
//		VidTextBox m_tb(x,y,w,h,font_size);	...everything else is default
//		m_tb.write("This is a message");	...sends "This is a message\n"
//
//		There may be a mode that I'll have to set up to control printing
//		immediately or printing later on.
//
public class VidTextBox extends Container implements KeyListener
{
    private HMultilineEntry m_hml; // The multiline entry

    private int m_x; // Container text box.

    private int m_y;

    private int m_w;

    private int m_h;

    private Font m_font; // Font to display text in

    private FontMetrics m_fm; // For FontMetrics calculations

    private int m_fmh; // font height

    private int m_fmw; // font width

    private int m_size; // Max set font size

    private int m_nchars; // Max number of chars

    private String m_dbuf = new String(""); // Display buffer

    private String m_win = new String(""); // temp and current window text

    private int m_len; // String buffer length.

    private int m_nlines; // Number of lines per window into textbox.

    private int m_cperline; // Chars per line (approx).

    private int m_startpage; // Points to top of current page

    private int m_endpage; // Points to bottom of current page

    // scroll_up(n) - Scrolls up m_endpage by n lines.
    private void scroll_up(int ncnt)
    {
        int i;
        int temp; // temp

        for (i = ncnt; i != 0; --i)
        {
            temp = m_dbuf.lastIndexOf('\n', m_endpage - 1);
            if (temp <= 0)
            {
                break;
            }
            else
            {
                m_endpage = temp;
            }
        } // for
    } // scroll_up()

    // scroll_down(n) - Scroll down m_endpage by n lines
    private void scroll_down(int ncnt)
    {
        int i;
        int temp; // temp

        for (i = ncnt; i != 0; --i)
        {
            temp = m_dbuf.indexOf('\n', m_endpage + 1);
            if (temp <= 0)
            {
                break;
            }
            else
            {
                m_endpage = temp;
            }
        } // for(...
    } // scroll_down()

    // win_page(); - Set up windows worth using m_endpage.
    private void win_page()
    {
        int i;
        int temp;

        i = m_nlines;
        temp = m_endpage - 1;
        for (i = 0; i < m_nlines; ++i)
        {
            temp = m_dbuf.lastIndexOf('\n', temp) - 1;
            if (temp <= 0)
            {
                m_startpage = 0;
                break;
            }
            else
            {
                m_startpage = temp + 2;
            }
        }
    } // win_page()

    // Build HMultilineEntry object
    private void buildHml()
    {
        int cPerLine; // Total chars per line
        int LineTotal; // Total num lines of text

        m_font = new Font("tiresias", Font.PLAIN, m_size);

        this.setBounds(m_x, m_y, m_w, m_h); // Container
        m_hml = new HMultilineEntry(0, 0, m_w, m_h, 40); // Arbitrary 40 chars
        m_hml.setHorizontalAlignment(m_hml.HALIGN_LEFT); // Set alignment
        m_hml.setVerticalAlignment(m_hml.VALIGN_TOP);
        m_fm = m_hml.getFontMetrics(m_font); // Get size of font
        m_fmh = m_fm.getHeight();
        m_fmw = m_fm.charWidth('0');
        cPerLine = m_w / m_fmw; // Total chars in Multiline box
        LineTotal = m_h / m_fmh;
        m_nlines = LineTotal - 2; // Typically 2 less
        m_cperline = cPerLine; // approx
        if (m_nchars == 0) // default buffer size
        {
            m_nchars = m_nlines * m_cperline;
        }
        m_hml.setMaxChars(m_nchars); // Set correct maxchars.

        m_hml.setBackgroundMode(m_hml.BACKGROUND_FILL); // regular background
        m_hml.setBackground(new Color(128, 128, 128));
        m_hml.setForeground(new Color(16, 16, 16)); // nearly black - 0,0,0 is
                                                    // punch through color
        m_hml.setResizeMode(m_hml.RESIZE_NONE);
        m_hml.setName("Debug Output");
        m_hml.setFont(m_font);
        m_hml.setVisible(true);
        m_hml.setBordersEnabled(true); // Set borders

        m_len = m_dbuf.length(); // Set String length and current page end
        m_endpage = m_len - 1;

        this.add(m_hml); // Add multiline to container.

    } // buildHml

    // Value constructor
    public VidTextBox(int tx, int ty, int tw, int th, int tfs, int tbs)
    {
        m_x = tx;
        m_y = ty;
        m_w = tw;
        m_h = th;
        m_size = tfs;
        m_nchars = tbs; // buffer size
        buildHml();
    }

    public void setBackgroundColor(DVBColor c)
    {
        m_hml.setBackground(c);
    }

    public void setForegroundColor(DVBColor c)
    {
        m_hml.setForeground(c);
    }

    // Default constructor
    public VidTextBox() // sets up defaults
    {
        m_x = 50;
        m_y = 100;
        m_w = 200;
        m_h = 100;
        m_size = 14; // Font Size
        m_nchars = 0; // Calculate default buffer size
        buildHml();
    }

    // Write - Writes a new line of text to the box.
    public void write(String instr)
    {
        m_win = m_dbuf.concat(instr); // Concatenate the input string.
        m_dbuf = m_win.concat("\n"); // Newline after each....like println

        output();
    } // write

    // Clears out the existing buffer
    public void reset()
    {
        m_dbuf = new String(""); // Display buffer
        m_win = new String(""); // temp and current window text
    }

    public void writeNoNewline(String instr)
    {
        m_dbuf = m_win = m_dbuf.concat(instr); // Concatenate the input string.
        output();
    }

    private void output()
    {
        int tnum = 0;
        int index = 0;
        tnum = m_dbuf.length(); // get new length
        while (tnum > m_nchars) // remove lines until it fits
        {
            index = m_dbuf.indexOf('\n', index + 1); // Get new line
            m_win = m_dbuf.substring(index); // remove line
            tnum = m_win.length(); // get new length
        } // continue until size is right
        if (index != 0) // check if any lines removed.
        {
            m_dbuf = m_win.substring(0); // if so, store the string
        }

        // Don't print if scrolling
        if (m_endpage != (m_len - 1)) // if not real time at end of page
        {
            m_len = m_dbuf.length(); // Just exit so window not affected
            return;
        }
        m_len = m_dbuf.length(); // Store length of string.
        m_endpage = m_len - 1; // Length
        win_page(); // rewind to find start of page
        try
        {
            m_win = m_dbuf.substring(m_startpage, m_endpage);
        }
        catch (Exception e)
        {
            System.out.println("***** Exception thrown on substring call! **********");
        }
        m_hml.setTextContent(m_win, HState.NORMAL_STATE); // ALL_STATES?
    }

    public void keyPressed(KeyEvent e) // Key pressed event handler
    {
        int index; // temp index
        int index1;
        int i; // Counter
        int key = e.getKeyCode();
        if (key == HRcEvent.VK_UP) // Scroll up half page
        {
            // OK must learn to scroll up.
            scroll_up(1); // scrolls up one line
            win_page(); // Get window worth
            m_win = m_dbuf.substring(m_startpage, m_endpage);
            m_hml.setTextContent(m_win, HState.NORMAL_STATE);
        }
        else if (key == HRcEvent.VK_DOWN) // Scroll down half page
        {
            scroll_down(1); // scroll down one line
            win_page(); // get window's worth
            m_win = m_dbuf.substring(m_startpage, m_endpage);
            m_hml.setTextContent(m_win, HState.NORMAL_STATE);
        }
        else if (key == OCRcEvent.VK_LIVE) // Resume real time printing below
        {
            m_endpage = m_len - 1; // Length
            win_page(); // rewind to find start of page
            m_win = m_dbuf.substring(m_startpage, m_endpage);
            m_hml.setTextContent(m_win, HState.NORMAL_STATE);
        }
    } // keyPressed(...

    public void keyTyped(KeyEvent e) // filler functions to avoid abstraction
    {
    }

    public void keyReleased(KeyEvent e)
    {
    }
} // Class VidTextBox
