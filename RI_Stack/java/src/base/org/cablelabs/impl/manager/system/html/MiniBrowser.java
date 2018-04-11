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

package org.cablelabs.impl.manager.system.html;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cablelabs.impl.manager.system.html.RenderLayout.Link;
import org.cablelabs.impl.manager.system.html.RenderLayout.TextRow;


public class MiniBrowser extends Component implements UrlGetterListener
{
    /**
     * Auto-generated 
     */
    private static final long serialVersionUID = -6516967491929082330L;

    private static final int SCROLL_BAR_WIDTH = 16;
    private static final int NO_LINK = -1;
    
    /**
     * Constants used to vertically scroll the contents of the browser.
     */
    public static final int SCROLL_TO_TOP = 0;
    public static final int SCROLL_TO_BOTTOM = 1;
    public static final int SCROLL_UP_1_LINE = 2;
    public static final int SCROLL_DOWN_1_LINE = 3;
    public static final int SCROLL_UP_1_PAGE = 4;
    public static final int SCROLL_DOWN_1_PAGE = 5;

    
    private final UrlGetter urlGetter;
    private MiniBrowserListener miniBrowserListener;
    
    private RenderLayout renderCache;
    
    private Document renderContents;
    
    private Color scrollBarColor;
    
    private boolean scrollBarEnabled;
    private boolean scrollBarNecessary;
    private int scrollOffset;
    
    private boolean linkHighlightingEnabled;
    private int highlightedLink;

    private final ArrayList history;
    
    /**
     * Constructs a new MiniBrowser.  The given UrlGetter will be
     * used to retrieve HTML content from URLs.
     */
    public MiniBrowser(UrlGetter urlGetter)
    {
        this.urlGetter = urlGetter;
        this.miniBrowserListener = null;
        
        renderCache = null;
        renderContents = null;
        
        scrollBarColor = Color.gray;
        
        scrollBarEnabled = true;
        linkHighlightingEnabled = true;

        history = new ArrayList();
        
        resetBrowser();
        
        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent e)
            {
                updateRenderCache();
            }
        });
        
        addPropertyChangeListener(new PropertyChangeListener()
        {            
            public void propertyChange(PropertyChangeEvent event)
            {
                if("font".equals(event.getPropertyName()))
                {
                    updateRenderCache();
                }
            }
        });
        
        updateRenderCache();
    }
    
    public void paint(Graphics g)
    {
        paintBackground(g);
        
        if(renderCache != null)
        {
            Graphics child = g.create();
            child.translate(0, -scrollOffset);

            paintForeground(child);

            child.dispose();
            
            if(scrollBarEnabled && scrollBarNecessary)
            {
                paintScrollBar(g);
            }
        }
    }

    private void paintBackground(Graphics g)
    {
        Color color = getBackgroundColor();
        g.setColor(color);
        
        Rectangle clip = g.getClipBounds();
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
    }

    private void paintForeground(Graphics g)
    {
        Rectangle clip = g.getClipBounds();

        clip.grow(-RenderLayout.MARGIN, -RenderLayout.MARGIN);
        g.setClip(clip);

        /* Draw link highlight */
        if (linkHighlightingEnabled && highlightedLink != NO_LINK)
        {
            Link link = (Link) renderCache.links.get(highlightedLink);
            
            List bounds = link.getBounds();
            for (int j = 0; j < bounds.size(); j++)
            {
                Rectangle r = (Rectangle) bounds.get(j);

                /* TODO: this fails if the text or background color is close to yellow. */
                g.setColor(Color.YELLOW);
                g.fillRect(r.x, r.y, r.width, r.height);
            }            
        }
        
        Rectangle rowBounds = new Rectangle();
        if (renderCache.rows != null)
        {
            Iterator i = renderCache.rows.iterator();
            while (i.hasNext())
            {
                TextRow row = (TextRow) i.next();
                rowBounds.setBounds(0, row.getYPosition(), row.getWidth(), row.getHeight());
                if (clip.intersects(rowBounds))
                {
                    row.draw(g);
                }
            }
        }
    }

    private void paintScrollBar(Graphics g)
    {
        int x = getWidth() - SCROLL_BAR_WIDTH - RenderLayout.MARGIN;

        int height = getHeight() - (RenderLayout.MARGIN * 2);
        
        /* Draw full bar */
        g.setColor(scrollBarColor);
        g.fillRect(x, RenderLayout.MARGIN, SCROLL_BAR_WIDTH, height);
        

        /* Draw scroll box */
        int pos = (int) Math.round((double) scrollOffset * height / renderCache.getContentHeight());
        int size = (int) Math.round((double) height * getHeight() / renderCache.getContentHeight());
        
        g.setColor(scrollBarColor.brighter());
        g.fillRect(x, RenderLayout.MARGIN + pos, SCROLL_BAR_WIDTH, size);    

        /* Draw border */
        g.setColor(scrollBarColor.darker());
        g.drawRect(x, RenderLayout.MARGIN, SCROLL_BAR_WIDTH, height);    
    }

    /**
     * Navigates to the given URL.  All previous history will be lost.
     */
    public void navigate(String url)
    {
        history.clear();        

        history.add(url);
        urlGetter.getUrl(url, this);        
    }

    /**
     * Navigates to the previous item in the history.  If no items
     * exist in the history, this does nothing.
     */
    public void navigateBackwards()
    {
        if(hasHistory())
        {
            /* Most recent entry is always current page, so remove that first */
            history.remove(history.size() - 1);
            urlGetter.getUrl((String) history.get(history.size() - 1), this);
        }
    }

    /**
     * Navigates to the link currently selected in the UI.  If no link
     * is selected, this does nothing.
     */
    public void followCurrentLink()
    {
        if(highlightedLink != NO_LINK)
        {
            Link link = (Link) renderCache.links.get(highlightedLink);
            String url = link.getURL();

            history.add(url);
            urlGetter.getUrl(url, this);        
        }
    }

    /**
     * Scrolls vertically by the amount given.  The amount is one of
     * the constants defined in this class.
     */
    public boolean scrollVertically(int scrollType)
    {
        boolean changed = false;

        Font font = getFont();        
        FontMetrics metrics = (font == null) ? null : getFontMetrics(font);
        int fontSize = (metrics == null) ? 0 : metrics.getHeight();
        
        int scrollAmount = 0;
        boolean repositionHighlightAtExtents = false;
        
        switch(scrollType)
        {
            case SCROLL_TO_TOP:
                scrollAmount = -scrollOffset;
                repositionHighlightAtExtents = true;
                break;
                
            case SCROLL_TO_BOTTOM:
                scrollAmount = Math.max(0, renderCache.getContentHeight() - getHeight());
                repositionHighlightAtExtents = true;
                break;
                
            case SCROLL_UP_1_LINE:
                if(!highlightLinkVertically(-1))
                {
                    scrollAmount = -fontSize;
                }
                else
                {
                    changed = true;
                }
                break;
                
            case SCROLL_DOWN_1_LINE:
                if(!highlightLinkVertically(1))
                {
                    scrollAmount = fontSize;
                }
                else
                {
                    changed = true;
                }
                break;
                
            case SCROLL_UP_1_PAGE:
                scrollAmount = -getHeight();
                repositionHighlightAtExtents = true;
                break;
                
            case SCROLL_DOWN_1_PAGE:
                scrollAmount = getHeight();
                repositionHighlightAtExtents = true;
                break;
                
            default:
                throw new IllegalArgumentException("Invalid scrollType");
        }
        
        if(scrollAmount != 0)
        {
            boolean scrolled = scrollBy(scrollAmount);
            changed = changed || scrolled;
            
            if(!scrolled && repositionHighlightAtExtents && renderCache.links.size() != 0)
            {
                /* In the case of page up/down, select first/last link on page */
                if(scrollAmount < 0)
                {
                    highlightedLink = 0;
                }
                else
                {
                    highlightedLink = renderCache.links.size() - 1;
                }
                
                changed = true;
            }
        }
        
        return changed;
    }

    /**
     * Moves the selection horizontally, in the case where there
     * is more than one link on a single line.  Passing true will
     * move the selection to the right, passing false will move
     * it to the left.
     */
    public boolean moveSelectionHorizontally(boolean right)
    {
        final int direction = right ? 1 : -1;
        
        boolean focusChanged = false;

        if (highlightedLink != NO_LINK)
        {
            /* Focus is on a link */
            
            int lineNumber = ((Link) renderCache.links.get(highlightedLink)).getLineNumber();

            int adjacentIndex = highlightedLink + direction;
            if (adjacentIndex >= 0 && adjacentIndex < renderCache.links.size())
            {
                int adjacentLineNumber = ((Link) renderCache.links.get(adjacentIndex)).getLineNumber();
                
                if (adjacentLineNumber == lineNumber)
                {
                    /* Focus moving to another link on the same line */
                    highlightedLink = adjacentIndex;
                    focusChanged = true;
                }
            }
        }
        
        return focusChanged;
    }
    
    /**
     * Sets the color used to draw the scroll bar.  This is the
     * base color, and color.brigher() and color.darker() will
     * be used to draw the elements of the scroll bar.  
     */
    public void setScrollBarColor(Color color)
    {
        scrollBarColor = color;
    }

    /**
     * Enables or disables the display of the scroll bar.  If the scroll
     * bar is enabled, it may not show up if the content is smaller than 
     * the content area.  If the scroll bar is disabled, it will never
     * appear.  However, calling scrollBy() will still scroll the page.
     */
    public void enableScrollBar(boolean enabled)
    {
        scrollBarEnabled = enabled;
    }

    /**
     * Enables or disables the highlight display of the currently 
     * selected link.  If enabled, the currently selected link will be
     * highlighted.  If disabled, the highlight will not be drawn.
     */
    public void enableSelection(boolean enabled)
    {
        linkHighlightingEnabled = enabled;
    }

    /**
     * Returns true if the currently selected link will be
     * highlighted.
     */
    public boolean isSelectionEnabled()
    {
        return linkHighlightingEnabled;
    }

    /**
     * Returns true if the current page has any embedded links.
     */
    public boolean hasLinks()
    {
        boolean hasLinks = false;
        
        if(renderCache != null)
        {
            hasLinks = !renderCache.links.isEmpty();
        }

        return hasLinks;
    }

    /**
     * Returns true if there is at least one URL in the
     * history buffer for navigateBackwards() to use.
     */
    public boolean hasHistory()
    {
        /* First element always contains current link, so must be more than 1 */
        return history.size() > 1;        
    }

    /**
     * Returns true if the contents of the browser is larger than the
     * display area, and can therefore be scrolled.  Returns false if
     * the entire contents can fit in the display area without scrolling.
     * 
     * This is independent of whether the scroll bar is actually
     * enalbed by enableScrollBar().
     */
    public boolean isScrollable()
    {
        return scrollBarNecessary;        
    }

    /**
     * Returns true if there is any link in the browser which currently
     * has the highlight, even if the highlight is not visible due to
     * calling enableSelection().
     */
    public boolean anyLinkHighlighted()
    {
        return highlightedLink != NO_LINK;
    }
    
    /**
     * Returns the background color of the embedded HTML page.
     */
    public Color getBackgroundColor()
    {
        Color color = Color.black;
        
        if (renderCache != null)
        {
            color = renderCache.getBackgroundColor();
        }
        
        return color;
    }
    
    /**
     * Implementation of the UrlGetterListener interface.
     * 
     * @see UrlGetterListener
     */
    public void fileDownloadComplete(String contents)
    {
        renderContents = Parser.parse(contents);
        
        resetBrowser();
        updateRenderCache();
    }

    /**
     * Implementation of the UrlGetterListener interface.
     * 
     * @see UrlGetterListener
     */
    public void fileDownloadFailed(int reason)
    {
        String content = "<center><br>An error occurred while communicating<br>" + 
                         "with the CableCARD.<br><br>" +
                         "Error code: " + reason;

        renderContents = Parser.parse(content);

        resetBrowser();
        updateRenderCache();
    }
    
    /**
     * Sets the given listener as the listener which will receive MiniBrowser events.
     * Only one listener can be registered at once, and any new listener will
     * override the previous one.  Calling this with null will clear the listener.
     */
    public void registerMiniBrowserListener(MiniBrowserListener listener)
    {
        miniBrowserListener = listener;
    }
    
    private void resetBrowser()
    {
        scrollBarNecessary = false;
        scrollOffset = 0;
        
        highlightedLink = NO_LINK;        
    }
    
    private void updateRenderCache()
    {
        if(renderContents != null)
        {
            Font currentFont = getFont();
            
            if(currentFont != null)
            {
                /* Reduce font size slightly */
                currentFont = new Font(currentFont.getFamily(), currentFont.getStyle(), currentFont.getSize() * 9 / 10);
                
                renderCache = new RenderLayout(renderContents, getWidth(), getFontMetrics(currentFont));
                
                if(renderCache.getContentHeight() > getHeight())
                {
                    /* If scroll bar is required, recalculate for smaller width */
                    renderCache = new RenderLayout(renderContents,
                                                   getWidth() - SCROLL_BAR_WIDTH - (RenderLayout.MARGIN * 2),
                                                   getFontMetrics(currentFont));
                    scrollBarNecessary = true;
                }
                else
                {
                    scrollBarNecessary = false;
                }
            }
            else
            {
                renderCache = null;
            }
        }
        else
        {
            renderCache = null;
        }
        
        triggerUpdate();
    }

    private void triggerUpdate()
    {
        if(miniBrowserListener != null)
        {
            miniBrowserListener.contentUpdated();
        }
        
        repaint();
    }
    
    private boolean scrollBy(int amount)
    {
        boolean scrollMoved = false;
        
        if(renderCache != null)
        {
            int offset = scrollOffset;
    
            offset += amount;
            offset = Math.min(offset, renderCache.getContentHeight() - getHeight());
            offset = Math.max(offset, 0);
    
            if (offset != scrollOffset)
            {
                scrollOffset = offset;
                highlightLinkVertically(amount > 0 ? 1 : -1);
    
                triggerUpdate();
                scrollMoved = true;
            }
        }
        
        return scrollMoved;
    }

    private boolean highlightLinkVertically(int direction)
    {
        boolean result = false;

        if (renderCache != null)
        {
            /* Deselect the old link if it isn't visible. */
            if (!isLinkVisible(highlightedLink))
            {
                highlightedLink = NO_LINK;
            }
    
            int oldLink = highlightedLink;
            int count = renderCache.links.size();
    
            if (oldLink == NO_LINK)
            {
                /*
                 *  No visible link was selected; select the first or last visible link.
                 */
                int searchStart = (direction > 0) ? -1 : count;
                for (int i = searchStart + direction; i >= 0 && i < count; i += direction)
                {
                    if (isLinkVisible(i))
                    {
                        highlightedLink = i;
                        result = true;
                        break;
                    }
                }
            }
            else
            {
                /*
                 *  A link was already selected; move up or down by line number to
                 *  find a new link to select.
                 */
                int lineNumber = ((Link) renderCache.links.get(oldLink)).getLineNumber();
    
                /*
                 * Skip links on the same line, and find the next/prev line number
                 * with a link
                 */
                int i = oldLink + direction;
                int newLine = -1;
                while (i >= 0 && i < count)
                {
                    newLine = ((Link) renderCache.links.get(i)).getLineNumber();
                    if (newLine != lineNumber)
                    {
                        break;
                    }
                    i += direction;
                }
    
                if (isLinkVisible(i))
                {
                    /*
                     *  Find the link on the new line that is horizontally closest to
                     *  the old link
                     */
                    Rectangle bounds = ((Link) renderCache.links.get(oldLink)).getBoundingRect();
                    
                    int bestIndex = i;
                    int bestDistance = getHorizontalDistance(bounds,
                        ((Link) renderCache.links.get(i)).getBoundingRect());
                    
                    while (i >= 0 && i < count)
                    {
                        Link link = (Link) renderCache.links.get(i);
                        if (link.getLineNumber() != newLine)
                        {
                            break;
                        }
    
                        int d = getHorizontalDistance(bounds, ((Link) renderCache.links.get(i)).getBoundingRect());
                        if (d < bestDistance)
                        {
                            bestIndex = i;
                            bestDistance = d;
                        }
                        i += direction;
                    }
                    
                    highlightedLink = bestIndex;
                    result = true;
                }
            }
    
            triggerUpdate();            
        }
        
        return result;
    }
    
    private boolean isLinkVisible(int index)
    {
        boolean visible = false;
        
        if (renderCache != null)
        {
            if (index == NO_LINK || index < 0 || index >= renderCache.links.size())
            {
                return false;
            }
            
            Link link = (Link) renderCache.links.get(index);
    
            Rectangle visibleArea = getBounds();
            visibleArea.setLocation(0, scrollOffset);
    
            List bounds = link.getBounds();
            Iterator iter = bounds.iterator();
            while (iter.hasNext())
            {
                Rectangle r = (Rectangle) iter.next();
                if (visibleArea.contains(r))
                {
                    visible = true;
                }
            }
        }
        
        return visible;
    }
    
    /**
     * Gets the horizontal distance between two rectangles. If the rectangles
     * overlap horizontally, the distance is negative and indicates the amount
     * of overlap.
     * 
     * @param a
     *            first rectangle
     * @param b
     *            second rectangle
     * @return distance between the rectangles
     */
    private int getHorizontalDistance(Rectangle a, Rectangle b)
    {
        Rectangle isect = a.intersection(b);
        
        int isectAmount = isect.width;
        
        if (isect.width <= 0)
        {
            int ax1 = a.x;
            int ax2 = a.x + a.width;
            int bx1 = b.x;
            int bx2 = b.x + b.width;
            if (ax1 < bx1)
            {
                isectAmount = bx1 - ax2;
            }
            else
            {
                isectAmount = ax1 - bx2;
            }
        }
        
        return isectAmount;
    }    
}
