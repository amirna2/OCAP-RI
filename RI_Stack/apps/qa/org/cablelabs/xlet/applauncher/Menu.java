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
package org.cablelabs.xlet.applauncher;

// Import standard Java classes.
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.Vector;

// Import OCAP classes.
import org.ocap.ui.event.OCRcEvent;

/**
 * This class implements a scrollable menu GUI for the application launcher.
 * 
 */
public class Menu extends Container implements KeyListener
{
    // The background image.
    private final String BACKGROUND = "/org/cablelabs/xlet/applauncher/images/menuBg.png";

    // The color of the highligts.
    private final Color HIGHLIGHT_COLOR = new Color(20, 20, 200);

    // Total number of menu slots or entries.
    private final int TOTAL_SLOTS = 7;

    // The location of the first menu entry slot.
    private final Point SLOT_START = new Point(50, 50);

    // The size of a menu entry slot.
    private final Dimension SLOT_SIZE = new Dimension(230, 27);

    // The name of the font used by the menu.
    private final String FONT_NAME = "tiresias";

    // The size of the font.
    private final int FONT_SIZE = 22;

    // The background menu image resource.
    private Image m_bkImage;

    // The array of menu entry slots.
    private Slot m_slots[] = new Slot[TOTAL_SLOTS];

    // The list of meny entries.
    private Vector m_itemList = new Vector();

    // Index into m_itemList
    private int m_highlightItemIndex = 0;

    // Index into m_slots[] .
    private int m_highlightSlotIndex = 0;

    // The menu listener handler.
    private MenuListener m_menuListener;

    /**
     * A constructor that initializes the listener for events that occur upon
     * menu selection.
     * 
     * @param listener
     *            The menu listener.
     */
    public Menu(MenuListener listener)
    {
        m_menuListener = listener;

        // Load background image.
        URL url = this.getClass().getResource(BACKGROUND);

        m_bkImage = java.awt.Toolkit.getDefaultToolkit().getImage(url);

        MediaTracker track = new MediaTracker(this);

        track.addImage(m_bkImage, 0);

        try
        {
            track.waitForID(0);
        }
        catch (InterruptedException e)
        {
            System.out.println("Unable to load image: " + BACKGROUND);
            e.printStackTrace();
        }

        this.setSize(m_bkImage.getWidth(this), m_bkImage.getHeight(this));

        // Set up the menu item slot.
        int y = SLOT_START.y;
        for (int i = 0; i < m_slots.length; ++i)
        {
            m_slots[i] = new Slot();
            m_slots[i].setLocation(SLOT_START.x, y);
            m_slots[i].setSize(SLOT_SIZE);
            m_slots[i].setFont(new Font(FONT_NAME, Font.PLAIN, FONT_SIZE));

            add(m_slots[i]);

            y += SLOT_SIZE.height;
        }

        // Add this as the key event listener.
        this.addKeyListener(this);
    }

    /**
	 * 
	 */
    public void cleanup()
    {
        removeKeyListener(this);
        m_menuListener = null;
        m_itemList.removeAllElements();
        m_itemList = null;
        for (int i = 0; i < m_slots.length; i++)
        {
            m_slots[i] = null;
        }
        m_slots = null;
    }

    /**
     * Add an item to the menu.
     * 
     * @param text
     *            The menu entry text to display.
     * @param c
     *            The color of the text.
     * @param obj
     *            The AppId.
     */
    public void addItem(String text, Color c, Object obj)
    {
        m_itemList.addElement(new Item(text, c, obj));
    }

    /**
     * Get the total number of items that is added via addItem() method.
     */
    public int getNumItems()
    {
        return m_itemList.size();
    }

    /**
     * Configure the menu.
     * 
     * Call reset() when it's ready to draw so that the graphical context can be
     * updated.
     */
    public void reset()
    {
        // Set the menu item into the slot.

        // Find the first item on the slot.
        int firstItem = m_highlightItemIndex - m_highlightSlotIndex;

        int max = (m_slots.length < m_itemList.size() ? m_slots.length : m_itemList.size());

        for (int i = 0; i < max; ++i)
        {
            Item item = (Item) m_itemList.elementAt(firstItem + i);

            m_slots[i].setText(item.m_text);
            m_slots[i].setTextColor(item.m_color);
            m_slots[i].setHighlight(false);
        }

        m_slots[m_highlightSlotIndex].setHighlight(true);

        // Grab the key focus.
        this.requestFocus();
    }

    /*
     * Highlight the next item in the menu.
     * 
     * This handler is called when the VK_DOWN event occurs.
     */
    private void highlightNext()
    {
        // Get the next item index.
        int nextIndex = m_highlightItemIndex + 1;

        if (nextIndex < m_itemList.size())
        {
            // Set the next item index.
            m_highlightItemIndex = nextIndex;

            // Get the next slot index.
            nextIndex = m_highlightSlotIndex + 1;

            if (nextIndex < m_slots.length && nextIndex < m_itemList.size()) m_highlightSlotIndex = nextIndex;
        }

        // Reset the menu context.
        reset();

        // Update the display.
        repaint();
    }

    /*
     * Highlight the previous item in the menu.
     * 
     * This handler is called when the VK_UP event occurs.
     */
    private void highlightPrev()
    {
        // Get the previous item index.
        int nextIndex = m_highlightItemIndex - 1;

        if (nextIndex >= 0)
        {
            // Set the previous item index.
            m_highlightItemIndex = nextIndex;

            // Get the previous slot index.
            nextIndex = m_highlightSlotIndex - 1;

            if (nextIndex >= 0) m_highlightSlotIndex = nextIndex;

        }

        // Reset the menu context.
        reset();

        // Update the display.
        repaint();
    }

    /**
     * Update the Menu display.
     * 
     * @param g
     *            The graphics context.
     */
    public void paint(Graphics g)
    {
        g.drawImage(m_bkImage, 0, 0, this);

        super.paint(g);
    }

    /*
     * This class manages the GUI that represents a single slot or menu entry.
     * It draws the text and highlight box when the entry is highlighted.
     */
    private class Slot extends Component
    {
        // Flag indicating highlight state.
        private boolean m_isHighlighted = false;

        // The color of the text.
        private Color m_textColor;

        // The text to display in this slot.
        private String m_text;

        /*
         * The default constructor.
         */
        public Slot()
        {
            // Do nothing extra.
        }

        /*
         * Set the text string.
         * 
         * @param text The string to display.
         */
        public void setText(String text)
        {
            m_text = text;
        }

        /*
         * Set the text color.
         * 
         * @param c The color of the text.
         */
        public void setTextColor(Color c)
        {
            m_textColor = c;
        }

        /*
         * Highlight the slot entry.
         * 
         * @param highlight If <b>true</b> then the slot is in a highlighted
         * state. If <b>false</b> then the slot is in an unhighlighted state
         */
        public void setHighlight(boolean highlight)
        {
            m_isHighlighted = highlight;
        }

        /**
         * Update the display of the slot entry.
         * 
         * @param g
         *            The graphics context.
         */
        public void paint(Graphics g)
        {
            if (m_isHighlighted)
            {
                g.setColor(HIGHLIGHT_COLOR);
                g.fillRect(0, 0, getBounds().width, getBounds().height);
            }

            int ascent = this.getFontMetrics(getFont()).getAscent();

            if (m_textColor != null) g.setColor(m_textColor);

            g.drawString(m_text, 5, ascent);
        }
    }

    /*
     * The menu item data that holds text, color and flag information.
     */
    private class Item
    {
        // The menu item text string.
        public String m_text;

        // The menu item color.
        public Color m_color;

        // The menu item flag state.
        public Object m_obj;

        /*
         * A constructor that initializes the menu items text string, color and
         * flag state.
         * 
         * @param text The menu item text string.
         * 
         * @param c The menu item color.
         * 
         * @param obj The menu item flag information.
         */
        public Item(String text, Color c, Object obj)
        {
            m_text = text;
            m_color = c;
            m_obj = obj;
        }
    }

    /**
     * A handler for when a key is typed from a keyboard.
     * 
     * @param ev
     *            The key event which caused this handler to be invoked.
     * 
     * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
     */
    public void keyTyped(KeyEvent ev)
    {
        // Do nothing for now.
    }

    /**
     * A handler for when a key is pressed.
     * 
     * @param ev
     *            The key event which caused this handler to be invoked.
     * 
     * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
     */
    public void keyPressed(KeyEvent ev)
    {
        if (!isVisible())
        {
            return;
        }
        switch (ev.getKeyCode())
        {
            case OCRcEvent.VK_UP:
                highlightPrev();
                break;

            case OCRcEvent.VK_DOWN:
                highlightNext();
                break;

            case OCRcEvent.VK_ENTER:
                Item item = (Item) m_itemList.elementAt(m_highlightItemIndex);
                m_menuListener.menuSelected(item.m_obj);
                break;

            case OCRcEvent.VK_STOP:
                item = (Item) m_itemList.elementAt(m_highlightItemIndex);
                m_menuListener.menuStopped(item.m_obj);
                break;

            default:
                break;
        }
    }

    /**
     * A handler for when a key is released.
     * 
     * @param ev
     *            The key event which caused this handler to be invoked.
     * 
     * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
     */
    public void keyReleased(KeyEvent e)
    {
        // Do nothig for now.
    }

}
