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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class RenderLayout
{
    public static final int MARGIN = 5;
    
    private final Document document;
    private final int width;
    private final FontMetrics fontMetrics;
    
    private int contentHeight;

    public final List rows;
    public final List links;


    /**
     * Creates and lays out a new layout based on the given document, and
     * fitting the given width and font metrics for the default font.
     */
    public RenderLayout(Document document, int width, FontMetrics fontMetrics)
    {
        this.document = document;
        this.fontMetrics = fontMetrics;        
        this.width = width;
        
        this.rows = new ArrayList();
        this.links = new ArrayList();

        createTextLayout();
    }
    
    /**
     * Returns the document's background color.
     */
    public Color getBackgroundColor()
    {
        return document.getBackgroundColor();
    }
    
    /**
     * Returns the document's width.
     */
    public int getWidth() 
    {
        return width;
    }    

    /**
     * Returns the document's calculated height.
     */
    public int getContentHeight()
    {
        return contentHeight;
    }

    private void createTextLayout()
    {
        final int fontAscent = fontMetrics.getAscent();

        Iterator paragraphs = document.iterator();

        int yOffset = MARGIN;
        float paragraphMargin = 0.0f;
        boolean firstParagraph = true;

        while (paragraphs.hasNext())
        {
            /*
             * Each paragraph has a margin. Raw text will have a margin of 0.
             * Normal paragraphs will have a margin based on the font size at
             * the beginning of the paragraph (in other words, the style at the
             * point of paragraph creation). The spacing between two consecutive
             * paragraphs will be the max of their two margins. Empty
             * paragraphs may need some special handling: when calculating
             * space, if the next paragraph is empty, add its margin to the
             * max calc, but don't insert it as an empty line.
             */
            Paragraph paragraph = (Paragraph) paragraphs.next();
            StyledText text = createStyledTextList(document, paragraph, fontMetrics);
            Iterator rowIter = createRowIterator(text, width, MARGIN);

            if (!firstParagraph)
            {
                // Is the paragraph empty?
                if (rowIter.hasNext())
                {
                    paragraphMargin = Math.max(paragraphMargin,
                            paragraph.getMargin());
                    yOffset += paragraphMargin * fontAscent;
                    paragraphMargin = paragraph.getMargin();
                }
                else
                {
                    // Empty paragraph: possible margin size increase, but
                    // no actual space inserted now.
                    paragraphMargin = Math.max(paragraphMargin,
                            paragraph.getMargin());
                }
            }

            while (rowIter.hasNext())
            {
                TextRow row = (TextRow) rowIter.next();
                int height = row.getHeight();

                row.setYPos(yOffset);
                row.setAlignment(paragraph.getAlignment(), MARGIN);
                yOffset += height;
                rows.add(row);
                firstParagraph = false;
            }
        }

        setLineNumbers();
        
        if (rows.isEmpty())
        {
            contentHeight = 0;
        }
        else
        {
            TextRow lastRow = (TextRow) rows.get(rows.size() - 1);
            contentHeight = lastRow.getYPosition() + lastRow.getHeight() + MARGIN;
        }
    }
    
    /**
     * Create a linked list of styled text chunks.
     * 
     * @param currentDocument
     *            document to display
     * @param paragraph
     *            paragraph to display
     * @param baseFontMetrics
     *            metrics of the component's default font
     * @return styled text linked list
     */
    private StyledText createStyledTextList(Document currentDocument,
            Paragraph paragraph, FontMetrics baseFontMetrics)
    {
        Map baseFontAttributes = baseFontMetrics.getFont().getAttributes();
        float defaultPointSize = ((Float) baseFontAttributes
                .get(TextAttribute.SIZE)).floatValue();
        String family = (String) baseFontAttributes.get(TextAttribute.FAMILY);

        StyledText list = StyledText.LAST;
        StyledText previous = null;

        int pos = 0;
        int length = paragraph.length();
        String hyperlink = null;

        while (pos < length)
        {
            int startPos = pos;
            String str = null;

            // Find the next space, newline, or end of run of a text style.
            int limit = paragraph.getRunLimit(pos);
            while (pos < limit)
            {
                char c = paragraph.charAt(pos);
                if (c == ' ' || c == '\n')
                {
                    if (pos == startPos)
                    {
                        pos++;
                    }
                    break;
                }
                pos++;
            }

            str = paragraph.substring(startPos, pos);
            StyledText text = new StyledText(str);

            // Add this text node to the list.
            if (list == StyledText.LAST)
            {
                list = text;
            }

            if (previous != null)
            {
                previous.setNext(text);
            }

            previous = text;

            Map attributes = paragraph.getAttributes(startPos);
            setTextAttributes(text, attributes, currentDocument, family,
                    defaultPointSize);

            String newHyperlink = (String) attributes.get(TextStyle.HYPERLINK);
            if (newHyperlink != null)
            {
                Link link = null;
                if (!newHyperlink.equals(hyperlink))
                {
                    link = new Link(newHyperlink);
                    links.add(link);
                }
                else
                {
                    link = (Link) links.get(links.size() - 1);
                }

                link.add(text);
            }
            hyperlink = newHyperlink;
        }
        return list;
    }

    /**
     * Set text attributes of a chunk of text.
     * 
     * @param text
     *            chunk of text
     * @param attributes
     *            text attributes from document
     * @param document
     *            document to display
     * @param family
     *            font family
     * @param defaultPointSize
     *            default font size
     */
    private static void setTextAttributes(StyledText text, Map attributes,
            Document document, String family, float defaultPointSize)
    {

        Color color = (Color) attributes.get(TextStyle.COLOR);
        if (null == color)
        {
            if (attributes.containsKey(TextStyle.HYPERLINK))
            {
                color = document.getLinkColor();
            }
            else
            {
                color = document.getTextColor();
            }
        }
        text.setColor(color);

        if (attributes.containsKey(TextStyle.UNDERLINED))
        {
            text.setUnderlined(true);
        }

        float pointSize = defaultPointSize;
        Float sizeScaleFactor = (Float) attributes.get(TextStyle.FONTSIZE);
        if (null != sizeScaleFactor)
        {
            pointSize *= sizeScaleFactor.floatValue();
        }

        int style = Font.PLAIN;
        if (attributes.containsKey(TextStyle.BOLD))
        {
            style |= Font.BOLD;
        }

        if (attributes.containsKey(TextStyle.ITALIC))
        {
            style |= Font.ITALIC;
        }

        Font font = new Font(family, style, (int) pointSize);
        text.setFont(font);
    }    
    
    /**
     * Create an iterator over the rows of text.
     * 
     * @param text
     *            list of chunks of text
     * @param width
     *            width of display container
     * @return iterator of rows
     */
    private static Iterator createRowIterator(StyledText text, int width, int margin)
    {
        ArrayList rows = new ArrayList();

        while (text != StyledText.LAST)
        {
            TextRow row = new TextRow(width);
            rows.add(row);
            text = row.addText(text, margin);
        }

        return rows.iterator();
    }
    
    /**
     * Set the line numbers of the links and the total line count.
     */
    private void setLineNumbers()
    {
        Iterator linkIter = links.iterator();
        Link link = null;
        StyledText nextLinkWord = null;

        if (linkIter.hasNext())
        {
            link = (Link) linkIter.next();
            nextLinkWord = (StyledText) link.words.get(0);
        }

        Iterator rowIter = rows.iterator();
        int line = 0;
        while (rowIter.hasNext())
        {
            TextRow row = (TextRow) rowIter.next();

            while (nextLinkWord != null && row.words.contains(nextLinkWord))
            {
                link.setLineNumber(line);
                if (linkIter.hasNext())
                {
                    link = (Link) linkIter.next();
                    nextLinkWord = (StyledText) link.words.get(0);
                }
                else
                {
                    nextLinkWord = null;
                }
            }
            line++;
        }
    }
    
    /**
     * Represents a single row of text.
     */
    static class TextRow
    {
        private final ArrayList words = new ArrayList();

        private int width;

        private int totalWidth = 0;

        private int height = 0;

        private int ascent = 0;

        private int ypos = 0;

        /**
         * Create a text row with the given width.
         * 
         * @param width
         *            width of row
         */
        public TextRow(int width)
        {
            this.width = width;
        }

        /**
         * Add as much text as possible to this row from the list of text
         * chunks.
         * 
         * @param text
         *            text chunk list
         * @return the next text chunk list node that didn't fit on this row
         */
        private StyledText addText(StyledText text, int margin)
        {
            totalWidth = margin;
            int newTotalWidth = totalWidth;

            // Add all the text that can be added.
            while (text != StyledText.LAST)
            {
                // Find the end of the current run of spaces.
                StyledText spaceStart = text;
                while (text != StyledText.LAST && text.isSpace())
                {
                    height = Math.max(height, text.height);
                    ascent = Math.max(ascent, text.ascent);
                    newTotalWidth += text.width;
                    text = text.getNext();
                }

                // Find the end of the next full word.
                StyledText wordStart = text;
                while (text != StyledText.LAST && !text.isSpace()
                        && !text.isNewline())
                {
                    height = Math.max(height, text.height);
                    ascent = Math.max(ascent, text.ascent);
                    newTotalWidth += text.width;
                    text = text.getNext();
                }

                if (text.isNewline() && wordStart == text)
                {
                    // Only use the newline's measurements if it's the only
                    // thing on the row.
                    if (words.isEmpty())
                    {
                        height = Math.max(height, text.height);
                        ascent = Math.max(ascent, text.ascent);
                    }
                    return text.getNext();
                }

                if ((newTotalWidth + margin) > width && !words.isEmpty())
                {
                    // The word won't fit, so add a soft line break. Discard the
                    // spaces and end the row.
                    while (spaceStart != wordStart)
                    {
                        spaceStart.visible = false;
                        spaceStart = spaceStart.getNext();
                    }
                    return wordStart;
                }

                // Is there a word to insert?
                // TODO: spaceStart is an odd name here.
                while (spaceStart != text)
                {
                    words.add(spaceStart);
                    spaceStart.x = totalWidth;
                    totalWidth += spaceStart.width;
                    spaceStart.visible = true;
                    spaceStart = spaceStart.getNext();
                }
                totalWidth = newTotalWidth;
            }

            totalWidth += margin;
            return text;
        }

        /**
         * Returns the width of the row.
         * 
         * @return row width
         */
        public int getWidth()
        {
            return width;
        }
        
        /**
         * Return the height of this row.
         * 
         * @return row height
         */
        public int getHeight()
        {
            return height;
        }
        
        /**
         * Returns the y position of this row.
         * 
         * @return y position
         */
        public int getYPosition()
        {
            return ypos;
        }

        /**
         * Set the y position of this row within the component.
         * 
         * @param ypos
         *            y position
         */
        private void setYPos(int ypos)
        {
            this.ypos = ypos;

            Iterator i = words.iterator();
            while (i.hasNext())
            {
                StyledText text = (StyledText) i.next();
                text.y = ypos + ascent;
            }
        }

        /**
         * Set the row's alignment.
         * 
         * @param alignment
         *            one of Paragraph.LEFT, Paragraph.RIGHT, or
         *            Paragraph.CENTER
         */
        private void setAlignment(int alignment, int margin)
        {
            int offset = 0;
            if (alignment == Paragraph.LEFT)
            {
                offset = margin;
            }
            else
            {
                offset = width - totalWidth;
                if (alignment == Paragraph.CENTER)
                {
                    offset = Math.max(0, offset / 2);
                }
            }

            Iterator i = words.iterator();
            while (i.hasNext())
            {
                StyledText text = (StyledText) i.next();
                text.x = offset;
                offset += text.width;
            }
        }

        /**
         * Draw this row.
         * 
         * @param g graphics context to draw on.
         */
        public void draw(Graphics g)
        {
            Iterator i = words.iterator();
            while (i.hasNext())
            {
                StyledText text = (StyledText) i.next();
                text.draw(g);
            }
        }
    }

    /**
     * Styled text linked list. Each node in the list is a string of characters
     * that has the same formatting and that can't be broken across rows.
     * 
     * Using a linked list simplifies breaking the text into rows in
     * TextRow.addText.
     * 
     */
    private static class StyledText
    {
        private final String text;

        private Font font;
        private Color color;

        private int x;
        private int y;
        private int width;
        private int height;

        private int ascent;

        private boolean underlined;

        private StyledText next;

        private boolean visible = true;

        /**
         * Special StyledText node that marks the end of the list; using this
         * rather than null eliminates the need for null checks.
         */
        public static final StyledText LAST = new StyledText("");

        /**
         * Create a styled text node
         * 
         * @param text
         *            node's text
         */
        public StyledText(String text)
        {
            this.text = text;
            this.next = LAST;
        }

        /**
         * Set the text's font
         * 
         * @param font
         *            font for text
         */
        public void setFont(Font font)
        {
            // Question: is it OK to get FontMetrics this way, directly through
            // the default toolkit?

            FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(
                    font);
            this.font = font;
            width = metrics.stringWidth(text);
            height = metrics.getHeight();
            ascent = metrics.getAscent();
        }

        /**
         * Set the text color
         * 
         * @param color
         *            text color
         */
        public void setColor(Color color)
        {
            this.color = color;
        }

        /**
         * Set the underline style
         * 
         * @param underlined
         *            true for underlining, false for no underline
         */
        public void setUnderlined(boolean underlined)
        {
            this.underlined = underlined;
        }

        /**
         * Set the next node in the list
         * 
         * @param next
         *            next node
         */
        public void setNext(StyledText next)
        {
            if (null != next)
            {
                this.next = next;
            }
        }

        /**
         * Get the next node in the list
         * 
         * @return next node
         */
        public StyledText getNext()
        {
            return next;
        }

        /**
         * Check whether this text node is a space.
         * 
         * @return true if it's a space, false otherwise.
         */
        public boolean isSpace()
        {
            return text.equals(" ");
        }

        /**
         * Check whether this text node is a newline.
         * 
         * @return true if it's a newline, false otherwise.
         */
        public boolean isNewline()
        {
            return text.equals("\n");
        }

        /**
         * Draw this text node.
         * 
         * @param g
         *            graphics context to draw on
         * @param x
         *            x position of text
         * @param y
         *            y position of text
         */
        public void draw(Graphics g)
        {
            if (isNewline() || (isSpace() && !underlined))
            {
                return;
            }

            g.setColor(color);

            if (underlined)
            {
                int x2 = x + width - 1;
                g.drawLine(x, y + 1, x2, y + 1);
            }

            if (!isSpace())
            {
                g.setFont(font);
                g.drawString(text, x, y);
            }
        }
    }

    /**
     * Represents a hyperlink. Contains the link target and the set of
     * StyledText words that are part of the link.
     * 
     */
    static class Link
    {
        private final String link;

        private final List words = new ArrayList();

        private int lineNumber = -1;

        private List bounds = null;

        private Rectangle boundingRect = null;

        /**
         * Construct a link with the given link target
         * 
         * @param link
         *            link target
         */
        public Link(String link)
        {
            this.link = link;
        }

        /**
         * Add a word to this link object.
         * 
         * @param word
         *            word to add
         */
        public synchronized void add(StyledText word)
        {
            words.add(word);
            bounds = null;
            boundingRect = null;
        }

        /**
         * Set link's line number
         * 
         * @param lineNumber
         *            new line number
         */
        public void setLineNumber(int lineNumber)
        {
            this.lineNumber = lineNumber;
        }

        /**
         * Get link's line number
         * 
         * @return line number
         */
        public int getLineNumber()
        {
            return lineNumber;
        }

        /**
         * Get link target
         * 
         * @return link target
         */
        public String getURL()
        {
            return link;
        }

        /**
         * Get link bounds.
         * 
         * @return List containing a Rectangle for each word in the link.
         */
        public synchronized List getBounds()
        {
            if (bounds == null)
            {
                bounds = new ArrayList();
                Iterator i = words.iterator();
                while (i.hasNext())
                {
                    StyledText text = (StyledText) i.next();
                    if (text.visible)
                    {
                        bounds.add(new Rectangle(text.x, text.y - text.ascent,
                                text.width, text.height));
                    }
                }
            }
            return bounds;
        }

        /**
         * Get rectangle bounding all of the text in the link
         * 
         * @return bounding rectangle
         */
        public synchronized Rectangle getBoundingRect()
        {
            if (boundingRect == null)
            {
                Iterator i = getBounds().iterator();
                if (i.hasNext())
                {
                    boundingRect = new Rectangle((Rectangle) i.next());
                    while (i.hasNext())
                    {
                        boundingRect.union((Rectangle) i.next());
                    }
                }
            }
            return boundingRect;
        }
    }
}
