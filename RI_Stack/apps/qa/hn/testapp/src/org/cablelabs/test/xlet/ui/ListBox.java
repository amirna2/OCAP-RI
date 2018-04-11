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
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import org.cablelabs.test.ElementList;
import org.cablelabs.test.Logger;
import org.cablelabs.test.Test;
import org.cablelabs.test.TestElement;
import org.cablelabs.test.TestRunner;

/**
 * This is a simple scrolling listbox from which user can select choices.
 */
public class ListBox
{

    private int x;

    private int y;

    private int width;

    private int height;

    private Font font;

    private FontMetrics fm;

    private String[] fixedOptions;

    private String[] scrollingOptions;

    private int scrollingCount;

    private int fixedCount;

    private int alignment;

    private Color buttonColor;

    private Color itemColor;

    private int selectedIndex;

    private int maxDisplayableListItems;

    private int maxStringWidth = 0;

    private boolean recalcMaxLength = true;

    private ElementList m_rootElementList = null;

    private ElementList m_listInFocus = null;

    private TestRunner m_testRunner = null;

    private Logger m_logger = null;

    private Graphics m_graphics = null;

    private final String[] fixedMenuItemsRoot = { "Execute All Tests" };

    private final String[] fixedMenuItemsSub = { " Menu", "Execute All Tests" };

    /**
     * Specifies that the listmenu should be left aligned.
     * 
     */
    public static final int ALIGN_LEFT = 0;

    /**
     * Specifies that the listmenu should be center aligned.
     * 
     */
    public static final int ALIGN_CENTER = 1;

    /**
     * Specifies that the max displayable items.
     * 
     */
    public static final int MAX_ITEMS = 9;

    public static final int ROOT_MENU_FIXED_COUNT = 1;

    public static final int SUB_MENU_FIXED_COUNT = 2;

    /**
     * Constructor will create a Menu with the specified x, y coordinates (top
     * left corner) and width and height. If the width of the menu is smaller
     * than the length of the longest item in the menulist the width will be set
     * appropriately to the length of that item.
     * 
     * @param newX
     *            is the horizontal position of the top right corner
     * 
     * @param newY
     *            is the vertical position of the top right corner
     * 
     * @param newWidth
     *            is the width of the menu
     * 
     * @param newHeight
     *            is the height of the menu
     * 
     */
    public ListBox(final int newX, final int newY, final int newWidth, final int newHeight)
    {
        if (newX < 0 || newY < 0 || newWidth <= 0 || newHeight <= 0)
        {
            throw new IllegalArgumentException("Invalid values");
        }
        this.x = newX;
        this.y = newY;
        this.width = newWidth;
        this.height = newHeight;
        fixedCount = 0;
        scrollingCount = 0;
        selectedIndex = 0;
        alignment = ALIGN_CENTER;
        buttonColor = Color.green;
        itemColor = Color.white;
        font = new Font("Tiresias", Font.BOLD, 16);
        maxDisplayableListItems = MAX_ITEMS;
    }

    public void initialize(ElementList rootList, TestRunner runner, Graphics g)
    {
        this.m_rootElementList = rootList;
        this.m_testRunner = runner;
        this.m_logger = new Logger();
        this.m_logger.setPrefix("ListBox: ");
        this.m_graphics = g;
        this.handleNewListInFocus(m_rootElementList);
    }

    public void handleOption(int selectedOptionNumber)
    {

        // m_logger.log("selection == " + selectedOptionNumber);
        if (selectedOptionNumber == 1)
        {
            // m_logger.log("the selection was for the previous menu");
            // Display the root list. Check if focus is root.
            if (!m_listInFocus.isRoot())
            {
                // m_logger.log("reseting to previous menu: " +
                // ((ElementList)m_listInFocus.getParent()).getName());
                this.handleNewListInFocus((ElementList) m_listInFocus.getParent());
            }
            else
            {
                // m_logger.log("running catagory: " +
                // this.m_listInFocus.getName());
                // Run all test in list.
                this.m_testRunner.runCategory(this.m_listInFocus);
            }
        }
        else if (selectedOptionNumber == 2)
        {
            if (!m_listInFocus.isRoot())
            {
                // m_logger.log("running catagory: " +
                // this.m_listInFocus.getName());
                // Run all test in list.
                this.m_testRunner.runCategory(this.m_listInFocus);
            }
            else
            {
                // minus 1 for 0 base.
                int adjustedIndex = this.selectedIndex + selectedOptionNumber - this.fixedCount - 1;
                // m_logger.log("adjustedIndex == " + adjustedIndex);

                if (true == this.selectionIsDisplayed(adjustedIndex))
                {
                    TestElement element = (TestElement) this.m_listInFocus.getElements().elementAt(adjustedIndex);
                    if (element instanceof ElementList)
                    {
                        // m_logger.log("handling new sub list: " +
                        // element.getName());
                        this.handleNewListInFocus((ElementList) element);
                    }
                    else
                    {
                        m_logger.log("running test: " + element.getName());
                        this.handleSelectedTest((Test) element);
                    }
                }
                else
                {
                    m_logger.log("selection is not in scope - ignoring...");
                }
            }

        }
        else
        {
            // minus 1 for 0 base.
            int adjustedIndex = this.selectedIndex + selectedOptionNumber - this.fixedCount - 1;
            // m_logger.log("adjustedIndex == " + adjustedIndex);

            if (true == this.selectionIsDisplayed(adjustedIndex))
            {
                TestElement element = (TestElement) this.m_listInFocus.getElements().elementAt(adjustedIndex);
                if (element instanceof ElementList)
                {
                    // m_logger.log("handling new sub list: " +
                    // element.getName());
                    this.handleNewListInFocus((ElementList) element);
                }
                else
                {
                    // m_logger.log("running test: " + element.getName());
                    this.handleSelectedTest((Test) element);
                }
            }
            else
            {
                // m_logger.log("selection is not in scope - ignoring...");
            }
        }
    }

    private boolean selectionIsDisplayed(int selectedOption)
    {
        if ((selectedOption < this.selectedIndex)
                || (selectedOption > (this.selectedIndex + this.maxDisplayableListItems)))
        {
            return false;
        }
        return true;
    }

    private void handleSelectedTest(Test t)
    {
        this.m_testRunner.setCurrentTest(t);
        this.m_testRunner.executeTest(t);
    }

    private void handleNewListInFocus(ElementList list)
    {
        if (null != list)
        {
            this.m_listInFocus = list;
            if (list.isRoot())
            {
                this.setFixed(true);
            }
            else
            {
                this.setFixed(false);
            }
        }
        else
        {
            this.setFixed(true);
        }
        this.setScrollingOptionsAndCount();
        this.draw(this.m_graphics);
    }

    private void setScrollingOptionsAndCount()
    {
        this.scrollingCount = this.m_listInFocus.getElements().size();
        this.scrollingOptions = new String[this.scrollingCount];
        // Load the scrollingOptions from the elments
        for (int ii = 0; ii < this.m_listInFocus.getElements().size(); ii++)
        {
            this.scrollingOptions[ii] = ((TestElement) this.m_listInFocus.getElements().elementAt(ii)).getName();
        }
    }

    public void setFixed(boolean isRoot)
    {
        // this.m_logger.log("setFixed isRoot = " + isRoot);
        if (true == isRoot)
        {
            fixedOptions = this.fixedMenuItemsRoot;
        }
        else
        {
            fixedOptions = this.fixedMenuItemsSub;
        }
        fixedCount = fixedOptions.length;
        recalcMaxLength = true;
    }

    /**
     * This will set the location of the top left corner of the menu to a new x
     * and y values.
     * 
     * @param newX
     *            is the new x value
     * 
     * @param newY
     *            is the new y value
     * 
     */
    public void setLocation(final int newX, final int newY)
    {
        if (newX < 0 || newY < 0)
        {
            throw new IllegalArgumentException("Invalid values for coordinates");
        }
        this.x = newX;
        this.y = newY;
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
        if (newFont == null)
        {
            throw new IllegalArgumentException("Font should not be null");
        }
        this.font = newFont;
        recalcMaxLength = true;
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
     * Set the width of the menu.
     * 
     * @param newWidth
     *            size of the new menu
     * 
     */
    public void setWidth(final int newWidth)
    {
        if (newWidth < 0)
        {
            throw new IllegalArgumentException("Invalid value for width");
        }
        this.width = newWidth;
        recalcMaxLength = true;
    }

    /**
     * Get the current width used.
     * 
     * @return int current size of the menu
     * 
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Set the height of the menu.
     * 
     * @param newHeight
     *            size of the new menu
     * 
     */
    public void setHeight(final int newHeight)
    {
        if (newHeight < 0)
        {
            throw new IllegalArgumentException("Invalid value for height");
        }
        this.height = newHeight;
    }

    /**
     * Get the current height used.
     * 
     * @return int current size of the menu
     * 
     */
    public int getHeight()
    {
        return height;
    }

    public ElementList getListInFocus()
    {
        return this.m_listInFocus;
    }

    /**
     * Sets the colors of the listbox items.
     * 
     * @param btnColor
     *            is the color to use for the button, specifying null will leave
     *            the current color
     * 
     * @param itmColor
     *            is the color to use for the list item, specifying null will
     *            leave the current color
     * 
     */
    public void setColors(final Color btnColor, final Color itmColor)
    {
        if (btnColor != null)
        {
            this.buttonColor = btnColor;
        }
        if (itmColor != null)
        {
            this.itemColor = itmColor;
        }
    }

    /**
     * Scrolls the list one page up. If there is not enough items to perform
     * scrolling nothing will happen
     * 
     */
    public void pageUp()
    {
        if (selectedIndex - maxDisplayableListItems > 0)
        {
            selectedIndex -= maxDisplayableListItems;
        }
        else
        {
            selectedIndex = 0;
        }
    }

    /**
     * Scrolls the list one page down. If there is not enough items to perform
     * scrolling nothing will happen.
     * 
     */
    public void pageDown()
    {
        if ((selectedIndex + maxDisplayableListItems) < scrollingCount)
        {
            selectedIndex += maxDisplayableListItems;
        }
        else if ((scrollingCount - selectedIndex) > maxDisplayableListItems)
        {
            selectedIndex = scrollingCount - maxDisplayableListItems;
        }
    }

    /**
     * Scrolls the list one item up. If there is not enough items to perform
     * scrolling nothing will happen.
     * 
     */
    public void scrollUp()
    {
        if (selectedIndex > 0)
        {
            --selectedIndex;
        }
    }

    /**
     * Scrolls the list one item down. If there is not enough items to perform
     * scrolling nothing will happen.
     * 
     */
    public void scrollDown()
    {
        if (selectedIndex + maxDisplayableListItems < scrollingCount)
        {
            ++selectedIndex;
        }
    }

    /**
     * Set the alignment of the list box within the rectangle that it's painted
     * in. The list can be ALIGN_LEFT or ALIGN_CENTER. The default alignment is
     * set to ALIGN_CENTER.
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
        else
        {
            throw new IllegalArgumentException("Invalid alignment constant");
        }
    }

    /**
     * Get the current alignment of the list.
     * 
     * @return current alignment of the list. One of <code>ALIGN_LEFT</code> or
     *         <code>ALIGN_CENTER</code>
     * 
     */
    public int getAlignment()
    {
        return alignment;
    }

    /**
     * Get the number of items in the list.
     * 
     * @return number of items in the list
     * 
     */
    public int getListSize()
    {
        return scrollingCount;
    }

    /**
     * Get the number of items in the fixed list only.
     * 
     * @return number of items in the fixed list only
     * 
     */
    public int getFixedListSize()
    {
        return fixedCount;
    }

    /**
     * Get the current selected index in the scrolling list or in the fixed list
     * if the numberSelected is in the range of fixed options.
     * 
     * @param numberSelected
     *            Selected number.
     * @return selected index in the scrolling/fixed list or -1 if
     *         numberSelected is invalid.
     * 
     */
    public int getSelectedIndex(final int numberSelected)
    {
        if ((numberSelected <= 0) || (numberSelected + selectedIndex > fixedCount + scrollingCount))
        {
            return -1;
        }

        if (numberSelected <= fixedCount)
        {
            return numberSelected - 1;
        }
        else
        {
            return (numberSelected - fixedCount) + selectedIndex - 1;
        }
    }

    /**
     * Get the current selected item.
     * 
     * @param numberSelected
     *            Selected number.
     * @return selected item from the list or null if numberSelected is invalid.
     * 
     */
    public String getSelectedItem(final int numberSelected)
    {
        int index = getSelectedIndex(numberSelected);
        if (-1 == index)
        {
            return null;
        }
        if (fixedCount == 0 || numberSelected > fixedCount)
        {
            return scrollingOptions[index];
        }
        else
        {
            return fixedOptions[index];
        }
    }

    /**
     * Get item from the given index.
     * 
     * @param index
     *            Index of them item.
     * @return item at given index or null if index is invalid.
     * 
     */
    public String getItem(final int index)
    {
        if (index > fixedCount + scrollingCount - 1 || index < 0)
        {
            return null;
        }
        if (fixedCount == 0 || index > fixedCount)
        {
            return scrollingOptions[index - fixedCount];
        }
        else
        {
            return fixedOptions[index];
        }
    }

    /**
     * This is the method that will paint the list on the screen. It should be
     * invoked in the <code>appPaint()</code> method whenever the user wishes to
     * draw the menu.
     * 
     * @param g
     *            is the Graphics object used to draw the list box
     * 
     */
    public void draw(final Graphics g)
    {

        // m_logger.log("Draw Enter.");
        int sh = 0;
        int numWidth = 0;
        int scrollbarDist = 0;
        int myX = this.x;
        int myY = this.y;
        int topY;
        int i;
        int currentStringWidth;

        if (recalcMaxLength)
        {
            g.setFont(this.font);
            fm = g.getFontMetrics();
            sh = fm.getHeight() + 3;
            numWidth = fm.stringWidth("1");

            // m_logger.log("draw fixedCount == " + fixedCount);
            // this should handle the case where strings are too long
            if (!this.m_listInFocus.isRoot())
            {
                this.fixedMenuItemsSub[0] = this.m_listInFocus.getParent().getName() + " Menu";
            }
            for (i = 0; i < fixedCount; ++i)
            {
                currentStringWidth = fm.stringWidth(fixedOptions[i]);
                if ((currentStringWidth > maxStringWidth) && (currentStringWidth < width))
                {
                    maxStringWidth = currentStringWidth;
                }
            }

            // m_logger.log("draw scrollingCount == " + scrollingCount);
            for (i = 0; i < scrollingCount; ++i)
            {
                currentStringWidth = fm.stringWidth(((TestElement) this.m_listInFocus.getElements().elementAt(i)).getName());
                if ((currentStringWidth > maxStringWidth) && (currentStringWidth < width))
                {
                    maxStringWidth = currentStringWidth;
                }
            }
        }

        // Draw the scrollbar 1.5 units away from the menu.
        scrollbarDist = numWidth + (numWidth >> 1);

        if (alignment == ListBox.ALIGN_CENTER)
        {
            // center text
            myX = this.x + (width - maxStringWidth - scrollbarDist) >> 1;
            int tot = fixedCount + scrollingCount;
            if (tot >= 9)
            {
                myY = this.y + (height - (MAX_ITEMS * sh)) >> 1;
            }
            else
            {
                myY = this.y + (height - (tot * sh)) >> 1;
            }
        }
        else
        {
            // Left-Justify
            myX = this.x + scrollbarDist + 5;
            myY = this.y + 10;
        }
        // Save top position for scrollbar.
        topY = myY;

        // draw fixed options
        if (fixedOptions != null)
        {
            // m_logger.log("drawing fixed options: ");
            String hyphen = "";
            // Start at 1 b/c this is used to print the menu item count.
            for (int ii = 1; ii <= fixedCount; ++ii)
            {
                if (!fixedOptions[ii - 1].equals(""))
                {
                    g.setColor(buttonColor);
                    g.drawString(String.valueOf(ii), myX, myY);
                    g.setColor(itemColor);
                    hyphen = ii < 10 ? " - " : "   - ";
                    g.drawString(hyphen + fixedOptions[ii - 1], myX + numWidth, myY);
                    g.drawString(hyphen + fixedOptions[ii - 1], myX + numWidth, myY);
                    // m_logger.log(fixedOptions[ii - 1]);
                    myY += sh;
                }
            }
        }

        // draw scrolling list options
        if (scrollingOptions != null)
        {
            int currentMaxIndex;
            maxDisplayableListItems = ((this.y + this.height) - sh - myY) / fm.getHeight();
            // m_logger.log("maxDisplayableListItems == " +
            // maxDisplayableListItems);
            if (maxDisplayableListItems > (MAX_ITEMS - fixedCount))
            {
                maxDisplayableListItems = (MAX_ITEMS - fixedCount);
            }

            currentMaxIndex = (selectedIndex + maxDisplayableListItems - 1);

            if (currentMaxIndex > (scrollingCount - 1))
            {
                currentMaxIndex = scrollingCount - 1;
            }

            // m_logger.log("selectedIndex == " + selectedIndex);
            // m_logger.log("currentMaxIndex == " + currentMaxIndex);
            for (int index = selectedIndex; index <= currentMaxIndex; ++index)
            {
                String tmp1;
                String tmp2;
                g.setColor(buttonColor);
                tmp1 = "" + (index - selectedIndex + fixedCount + 1);
                g.drawString(tmp1, myX, myY);
                numWidth = fm.stringWidth(tmp1);
                tmp2 = " - " + scrollingOptions[index];
                g.setColor(itemColor);
                g.drawString(tmp2, myX + numWidth, myY);
                // m_logger.log("drawing scrolling options: " +tmp2);
                myY += sh;
            }

            // Display up arrow if items can scroll up
            if (selectedIndex != 0)
            {
                Arrows.upArrow(g, myX - scrollbarDist, topY - (sh >> 1), numWidth, sh >> 1);
            }
            // Display down arrow if items can scroll down
            if ((scrollingCount - 1) > currentMaxIndex)
            {
                Arrows.downArrow(g, myX - scrollbarDist, myY - sh - (sh >> 1), numWidth, sh >> 1);
            }
        }
        else
        {
            // m_logger.log("scrollingOptions == null");
        }
    }

    /**
     * This method will paint the list on the screen using the Toolkit.sync()
     * method. This means that whenever it will be called it will paint the menu
     * without calling repaint().
     * 
     * @param g
     *            is the Graphics object used to draw the list box
     * 
     */
    public void update(final Graphics g)
    {
        draw(g);
        Toolkit.getDefaultToolkit().sync();
    }
}
