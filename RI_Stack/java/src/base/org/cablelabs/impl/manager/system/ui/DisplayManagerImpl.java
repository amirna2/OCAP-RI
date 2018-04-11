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

package org.cablelabs.impl.manager.system.ui;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import org.cablelabs.impl.manager.system.html.MiniBrowser;
import org.cablelabs.impl.manager.system.html.MiniBrowserListener;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

public class DisplayManagerImpl extends DisplayManager implements MiniBrowserListener
{
    /**
     * Auto-generated
     */
    private static final long serialVersionUID = -1376083893226429206L;

    private static final int MARGIN = 10;
    private static final int OUTLINE_SIZE = 2;

    private static final Color FRAME_COLOR = new Color(10, 50, 100);
    private static final Color OUTLINE_COLOR = Color.GRAY;
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 0);
    
    private final ArrayList dialogs;
    
    private Rectangle dialogWindow;
    private Rectangle dialogContents;
    
    private Dialog activeDialog; 
    
    private boolean tabBarHighlighted;
    
    
    public DisplayManagerImpl()
    {
        dialogs = new ArrayList();
        activeDialog = null;
        
        tabBarHighlighted = false;
        
        addKeyListener(new KeyAdapter()
        {
            public void keyPressed(KeyEvent e)
            {
                if (activeDialog == null)
                {
                    return;
                }

                int code = e.getKeyCode();
                switch (code)
                {
                    case OCRcEvent.VK_LIVE:
                        /* Go to live TV - close all open dialogs */
                        while (!dialogs.isEmpty())
                        {
                            closeDialog((Dialog) dialogs.get(0));
                        }
                        break;
                        
                    case KeyEvent.VK_ENTER:
                        if(tabBarHighlighted)
                        {
                            /* Ignore when tab bar is selected */
                        }
                        else if(activeDialog.getMiniBrowser().anyLinkHighlighted())
                        {
                            activeDialog.getMiniBrowser().followCurrentLink();
                        }
                        else
                        {
                            closeDialog(activeDialog);
                        }
                        break;

                    case KeyEvent.VK_UP:
                        if(tabBarHighlighted)
                        {
                            selectTabDirection(-1);
                        }
                        else
                        {
                            boolean doScroll = true;
                            
                            if(!activeDialog.getMiniBrowser().isSelectionEnabled())
                            {
                                /* If we previously moved to the OK button, move back to the dialog area */
                                activeDialog.getMiniBrowser().enableSelection(true);
                                
                                if(activeDialog.getMiniBrowser().anyLinkHighlighted())
                                {
                                    /* If moving up just highlighted a link, then don't scroll */
                                    doScroll = false;
                                }
                            }
                            
                            if(doScroll)
                            {
                                activeDialog.getMiniBrowser().scrollVertically(MiniBrowser.SCROLL_UP_1_LINE);
                            }
                        }
                        break;

                    case KeyEvent.VK_DOWN:
                        if(tabBarHighlighted)
                        {
                            selectTabDirection(1);
                        }
                        else
                        {
                            boolean updated = activeDialog.getMiniBrowser().scrollVertically(MiniBrowser.SCROLL_DOWN_1_LINE);
                            
                            /* If not moved when scrolling down, select OK button */
                            if(!updated)
                            {
                                activeDialog.getMiniBrowser().enableSelection(false);
                            }
                        }
                        break;

                    case KeyEvent.VK_RIGHT:
                        if(tabBarHighlighted)
                        {
                            selectTabBar(false);
                        }
                        else
                        {
                            activeDialog.getMiniBrowser().moveSelectionHorizontally(true);                            
                        }
                        break;

                    case KeyEvent.VK_LEFT:
                        if(tabBarHighlighted)
                        {
                            /* Ignore when moving left */
                        }
                        else
                        {
                            boolean movedHighlight = activeDialog.getMiniBrowser().moveSelectionHorizontally(false);
                            
                            if(!movedHighlight && dialogs.size() > 1)
                            {
                                selectTabBar(true);
                            }
                        }
                        break;

                    case KeyEvent.VK_PAGE_UP:
                        activeDialog.getMiniBrowser().scrollVertically(MiniBrowser.SCROLL_UP_1_PAGE);
                        break;

                    case KeyEvent.VK_PAGE_DOWN:
                        activeDialog.getMiniBrowser().scrollVertically(MiniBrowser.SCROLL_DOWN_1_PAGE);
                        break;

                    case OCRcEvent.VK_EXIT:
                        closeDialog(activeDialog);
                        break;
                        
                    case OCRcEvent.VK_LAST:
                    case OCRcEvent.VK_BACK:
                    case OCRcEvent.VK_SKIP: // The "skip back" button
                    case HRcEvent.VK_REWIND:
                        activeDialog.getMiniBrowser().navigateBackwards();
                        break;

                    default:
                        if (KeyEvent.VK_1 <= code && code <= KeyEvent.VK_9)
                        {
                            int index = code - KeyEvent.VK_1;
                            if (index < dialogs.size())
                            {
                                setActiveDialog((Dialog) dialogs.get(index));
                            }
                        }                        
                        break;
                }
                
                repaint();
            }
        });
        
        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                updateBounds();
            }
        });
    }
    
    private void selectTabBar(boolean selected)
    {
        tabBarHighlighted = selected;
        activeDialog.getMiniBrowser().enableSelection(!selected);
    }

    public boolean displayDialog(Dialog dialog)
    {
        dialogs.add(dialog);
        setActiveDialog(dialog);

        dialog.getMiniBrowser().registerMiniBrowserListener(this);
        
        requestFocusInWindow();
        
        return true;
    }

    public void dismissDialog(Dialog dialog)
    {
        dialogs.remove(dialog);
        
        if(dialogs.isEmpty())
        {
            setActiveDialog(null);
        }
        else if(dialog == activeDialog)
        {
            /* TODO: Should we jump to dialog 0 here? */
            setActiveDialog((Dialog) dialogs.get(0));
        }

        if(tabBarHighlighted && dialogs.size() == 1)
        {
            selectTabBar(false);
        }
        
        updateBounds();
        repaint();
    }
    
    public void paint(Graphics g)
    {
        if(activeDialog == null)
        {
            return;
        }
        
        Rectangle innerFrame = new Rectangle(dialogWindow);
        
        innerFrame.grow(-OUTLINE_SIZE, -OUTLINE_SIZE);
        g.setColor(OUTLINE_COLOR);
        drawFrame(g, dialogWindow, innerFrame);
        
        g.setColor(FRAME_COLOR);
        drawFrame(g, innerFrame, dialogContents);

        drawOkButton(g);
        drawTabs(g);

        Rectangle dialogAreaFrame = new Rectangle(dialogContents);
        dialogAreaFrame.grow(OUTLINE_SIZE, OUTLINE_SIZE);
        g.setColor(OUTLINE_COLOR);
        drawFrame(g, dialogAreaFrame, dialogContents);
        
        /* Paint sub-components */
        super.paint(g);        
    }
    
    /**
     * Draw a rectangular frame - a rectangle with a rectangular cutout in the
     * middle
     * 
     * @param g
     *            graphics context to draw on
     * @param outer
     *            outer bounds of frame
     * @param inner
     *            rectangular cutout
     */
    private void drawFrame(Graphics g, Rectangle outer, Rectangle inner)
    {
        /* Draw top */
        g.fillRect(outer.x, outer.y, outer.width, inner.y - outer.y);
        
        /* Draw left side */
        g.fillRect(outer.x, inner.y, inner.x - outer.x, inner.height);
        
        /* Draw right side */
        g.fillRect(inner.x + inner.width, inner.y, outer.width - inner.width
                - (inner.x - outer.x), inner.height);
        
        /* Draw bottom */
        g.fillRect(outer.x, inner.y + inner.height, outer.width, outer.height
                - inner.height - (inner.y - outer.y));
    }

    /**
     * Draw the OK button
     * 
     * @param g  graphics context to draw on
     */
    private void drawOkButton(Graphics g)
    {
        if (activeDialog == null)
        {
            return;
        }

        String okText = "OK";

        Rectangle button = new Rectangle();
        button.height = getOkButtonHeight();
        button.width = (int) (dialogWindow.width * 0.2);        
        button.x = (dialogWindow.x + dialogWindow.width / 2) - button.width / 2;
        button.y = dialogWindow.y + dialogWindow.height - MARGIN - button.height;

        Color background = FRAME_COLOR.darker();

        // The OK button is selected if there is no selected link or selected
        // tab.
        if (tabBarHighlighted || (activeDialog.getMiniBrowser().anyLinkHighlighted() && activeDialog.getMiniBrowser().isSelectionEnabled()))
        {
            g.setColor(OUTLINE_COLOR);
        }
        else
        {
            button.grow(1, 1);
            g.setColor(HIGHLIGHT_COLOR);
            background = background.darker();
        }
        
        Rectangle inset = new Rectangle(button);
        inset.grow(-OUTLINE_SIZE, -OUTLINE_SIZE);
        drawFrame(g, button, inset);
        
        g.setColor(background);
        g.fillRect(inset.x, inset.y, inset.width, inset.height);

        g.setColor(Color.WHITE);
        drawCenteredText(g, okText, button);
    }
    
    /**
     * Draw tabs down the side of the dialog frame. Tabs are only displayed if
     * there are two or more active dialogs. The tabs are displayed on the side
     * of the dialog frame instead of the more common top position because
     * vertical space is more limited than horizontal space.
     * 
     * @param g
     *            graphics context to draw on
     */
    private void drawTabs(Graphics g)
    {
        /* Don't bother with tabs unless there are multiple dialogs */
        if (dialogs.size() <= 1)
        {
            return;
        }

        int y = dialogWindow.y + MARGIN;
        
        Rectangle outer = new Rectangle();
        Rectangle inner = new Rectangle();

        for (int i = 0; i < dialogs.size(); i++)
        {
            Color tabBackground = FRAME_COLOR;
            Color tabOutline = OUTLINE_COLOR;
            Color tabText = Color.WHITE;

            int innerWidthShift = 0;

            outer.y = y;
            inner.height = getTextHeight();
            inner.width = 3 * inner.height;
            y += inner.height + MARGIN - OUTLINE_SIZE / 2;

            if (dialogs.get(i) == activeDialog)
            {
                inner.grow(MARGIN / 2, MARGIN / 2);
            }
            else
            {
                outer.y += MARGIN / 2;
                tabBackground = tabBackground.darker();
                tabOutline = tabOutline.darker();
                tabText = tabText.darker();
                innerWidthShift = -OUTLINE_SIZE;
            }

            outer.width = inner.width + OUTLINE_SIZE * 2;
            outer.height = inner.height + OUTLINE_SIZE * 2;
            outer.x = dialogWindow.x - outer.width;

            inner.setBounds(outer);
            inner.grow(0, -OUTLINE_SIZE);
            inner.x = outer.x + OUTLINE_SIZE;
            inner.width += innerWidthShift;

            g.setColor(tabOutline);
            drawFrame(g, outer, inner);

            if (tabBarHighlighted && dialogs.get(i) == activeDialog)
            {
                g.setColor(HIGHLIGHT_COLOR);
                Rectangle selection = new Rectangle(inner);
                selection.grow(-OUTLINE_SIZE, -OUTLINE_SIZE);
                drawFrame(g, inner, selection);
                
                g.setColor(tabBackground.brighter());
                g.fillRect(selection.x, selection.y, selection.width, selection.height);
            }
            else
            {
                g.setColor(tabBackground);
                g.fillRect(inner.x, inner.y, inner.width, inner.height);
            }

            g.setColor(tabText);
            drawCenteredText(g, Integer.toString(i + 1), inner);
        }
    }    
    
    /**
     * Draw text centered within a rectangle
     * 
     * @param g         graphics context to draw on
     * @param text      text to draw
     * @param bounds    bounding box of text
     */
    private void drawCenteredText(Graphics g, String text, Rectangle bounds)
    {
        FontMetrics metrics = g.getFontMetrics();
        Rectangle textBounds = new Rectangle(metrics.stringWidth(text),
                                             metrics.getHeight());
        
        g.drawString(text,
                     bounds.x + bounds.width / 2 - textBounds.width / 2,
                     bounds.y + bounds.height / 2 - textBounds.height / 2 + metrics.getAscent());
    }
    
    private void setActiveDialog(Dialog dialog)
    {
        activeDialog = dialog;
        
        removeAll();

        if(activeDialog != null)
        {
            add(activeDialog.getMiniBrowser());
            setVisible(true);
        }
        else
        {
            setVisible(false);
        }
        
        updateBounds();
    }
    
    private void updateBounds()
    {
        dialogWindow = getBounds();
        dialogWindow.setLocation(0, 0);
        dialogWindow.grow((int) (dialogWindow.width * -0.2), (int) (dialogWindow.height * -0.075));

        dialogContents = new Rectangle(dialogWindow);
        dialogContents.height -= getOkButtonHeight() + MARGIN;
        dialogContents.grow(-MARGIN, -MARGIN);
        
        if(activeDialog != null)
        {
            activeDialog.getMiniBrowser().setBounds(dialogContents);
            activeDialog.getMiniBrowser().setScrollBarColor(FRAME_COLOR.brighter());
            
            activeDialog.getMiniBrowser().enableSelection(!tabBarHighlighted);
        }
    }
    
    /**
     * Get the height of the current font.
     * 
     * @return font height
     */
    private int getTextHeight()
    {
        return getFontMetrics(getFont()).getHeight();
    }

    /**
     * Get the height of the OK button.
     * 
     * @return button height
     */
    private int getOkButtonHeight()
    {
        /* Make the OK button slightly taller than the text height. */
        return getTextHeight() * 8 / 7;
    }

    private void closeDialog(Dialog dialog)
    {
        dialog.close();
        dismissDialog(dialog);
    }    
    
    private void selectTabDirection(int direction)
    {
        int size = dialogs.size();
        int currentIndex = dialogs.indexOf(activeDialog);
        
        int nextIndex = (currentIndex + direction + size) % size;
        
        setActiveDialog((Dialog) dialogs.get(nextIndex));
    }
    
    public void contentUpdated()
    {
        updateBounds();
        repaint();
    }
}
