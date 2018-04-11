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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a paragraph of HTML text within a document. Each character in a
 * paragraph has an associated map of style information, whose keys are the
 * constants defined in the TextStyle class. Links are represented as
 * TextStyle.HYPERLINK values.
 * 
 * @author Spencer Schumann
 * 
 */
public class Paragraph
{
    /**
     * Constant indicating left alignment for the paragraph.
     */
    public static final int LEFT = 0;

    /**
     * Constant indicating center alignment for the paragraph.
     */
    public static final int CENTER = 1;

    /**
     * Constant indicating right alignment for the paragraph.
     */
    public static final int RIGHT = 2;

    private final String text;
    private final AttributeSegment[] segments;
    private final float margin;
    private final int alignment;

    /**
     * Create a paragraph builder object.
     * 
     * @return paragraph builder
     */
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Get the paragraph's alignment.
     * 
     * @return LEFT, CENTER, or RIGHT
     */
    public int getAlignment()
    {
        return alignment;
    }

    /**
     * Get the paragraph's margin, which indicates the amount of vertical space
     * between this paragraph and surrounding paragraphs.
     * 
     * @return margin as a fraction of the default font size.
     */
    public float getMargin()
    {
        return margin;
    }

    /**
     * Get paragraph length.
     * 
     * @return paragraph length in characters
     */
    public int length()
    {
        return text.length();
    }

    /**
     * Get a character within the paragraph.
     * 
     * @param index
     *            index of character to retrieve
     * @return character
     */
    public char charAt(int index)
    {
        return text.charAt(index);
    }

    /**
     * Get a substring of the paragraph. Works identically to
     * {@link String#substring(int, int)}, including the exceptions it throws.
     * 
     * @param start
     *            starting index
     * @param end
     *            ending index
     * @return sub string
     */
    public String substring(int start, int end)
    {
        return text.substring(start, end);
    }

    /**
     * Get map of style attributes for the character at the given index.
     * 
     * @param index
     *            index of character
     * @return map of attributes
     */
    public Map getAttributes(int index)
    {
        int attrIndex = getAttributeSegmentIndex(index);
        return segments[attrIndex].attributes;
    }

    /**
     * Find a group of characters with identical style attributes.
     * 
     * @param index
     *            index of character within paragraph
     * @return index of first character past the given one that has a different
     *         set of attributes.
     */
    public int getRunLimit(int index)
    {
        int attrIndex = getAttributeSegmentIndex(index);
        if (attrIndex < (segments.length - 1))
        {
            return segments[attrIndex + 1].startPos;
        }
        else
        {
            return text.length();
        }
    }

    /**
     * Private constructor used by Builder to create an immutable paragraph
     * instance.
     * 
     * @param text
     *            paragraph text
     * @param segments
     *            groups of style attributes
     * @param margin
     *            paragraph margin
     * @param alignment
     *            paragraph alignment
     */
    private Paragraph(String text, AttributeSegment[] segments, float margin,
            int alignment)
    {
        this.text = text;
        this.segments = segments;
        this.margin = margin;
        this.alignment = alignment;
    }

    /**
     * Returns the index where the style run starts, which is the highest index
     * in the array of style segments with startPos <= pos.
     * 
     * @param pos
     *            a character index
     * @return index into segments array
     */
    private int getAttributeSegmentIndex(int pos)
    {
        // Adaptation of Arrays.binarySearch
        int low = 0;
        int high = segments.length - 1;
        while (low <= high)
        {
            int mid = (low + high) / 2;
            int midPos = segments[mid].startPos;

            if (pos > midPos)
            {
                low = mid + 1;
            }
            else if (pos < midPos)
            {
                high = mid - 1;
            }
            else
            {
                // exact match found.
                return mid;
            }
        }

        // Exact match not found; the index where the style run starts
        // is now in 'high'.
        return high;
    }

    /**
     * Builder class, used by Document.Builder, to create paragraphs
     * 
     */
    public static class Builder
    {
        private final StringBuffer textBuffer = new StringBuffer();
        private final ArrayList attributeSegments = new ArrayList();
        private boolean pendingSpace = false;
        private boolean discardLeadingSpace = true;
        private float margin = 0.0f;
        private int alignment = LEFT;

        /**
         * Set paragraph alignment
         * 
         * @param One
         *            of LEFT, CENTER, or RIGHT
         */
        public void setAlignment(int alignment)
        {
            this.alignment = alignment;
        }

        /**
         * Set paragraph margin
         * 
         * @param margin
         *            paragraph margin, expressed as a fraction of the default
         *            font size
         */
        public void setMargin(float margin)
        {
            this.margin = margin;
        }

        /**
         * Add a line break to the paragraph.
         * 
         * @param size
         *            height of line break, expressed as a fraction of the
         *            default font size.
         */
        public void addLineBreak(float size)
        {
            Map attributes = Collections.singletonMap(TextStyle.FONTSIZE,
                    new Float(size));

            addAttributes(attributes);
            textBuffer.append('\n');
            pendingSpace = false;
            discardLeadingSpace = true;
        }

        /**
         * Add a string to the paragraph. Collapses runs of whitespace into a
         * single space.
         * 
         * @param text
         *            string to add
         * @param attributes
         *            map of style attributes for the string being added
         */
        public void addText(String text, Map attributes)
        {
            int pos = 0;
            int length = text.length();
            boolean attributesAdded = false;

            while (pos < length)
            {
                char c = text.charAt(pos++);
                if (Parser.isWhitespace(c))
                {
                    // Note: the first character in any run of whitespace
                    // determines the font style for that space.
                    if (!pendingSpace)
                    {
                        pendingSpace = true;

                        if (!attributesAdded)
                        {
                            addAttributes(attributes);
                            attributesAdded = true;
                        }
                    }
                }
                else
                {
                    // Add leading space, but not at the beginning of the
                    // paragraph or after a line break.
                    if (discardLeadingSpace)
                    {
                        discardLeadingSpace = false;
                        pendingSpace = false;
                    }
                    else if (pendingSpace)
                    {
                        textBuffer.append(' ');
                        pendingSpace = false;
                    }

                    if (!attributesAdded)
                    {
                        addAttributes(attributes);
                        attributesAdded = true;
                    }

                    textBuffer.append(c);
                }
            }
        }

        /**
         * Create a Paragraph based on the current state of this builder.
         * 
         * @param textColor
         *            default text color to be used for text with no
         *            TextStyle.COLOR attribute.
         * @return paragraph object
         */
        public Paragraph build(Color textColor)
        {
            Object[] segments;
            segments = new AttributeSegment[attributeSegments.size()];
            segments = attributeSegments.toArray(segments);

            return new Paragraph(textBuffer.toString(),
                    (AttributeSegment[]) segments, margin, alignment);
        }

        /**
         * Private constructor to prevent direct instantiation and sub-classing.
         * 
         */
        private Builder()
        {
            // Make sure there's at least one attribute map
            addAttributes(Collections.EMPTY_MAP);
        }

        /**
         * Add text attributes. If the new attributes are the same as the last
         * ones, the two runs of attributes are collapsed into one.
         * 
         * @param attributes
         *            style attributes to set
         */
        private void addAttributes(Map attributes)
        {
            // Are the new attributes equal to the last attributes?
            int size = attributeSegments.size();
            if (size > 0)
            {
                AttributeSegment lastSegment = (AttributeSegment) attributeSegments
                        .get(size - 1);
                if (lastSegment.attributes.equals(attributes))
                {
                    return;
                }
                else if (lastSegment.startPos == textBuffer.length())
                {
                    attributeSegments.remove(size - 1);
                }
            }

            AttributeSegment as = new AttributeSegment(new HashMap(attributes),
                    textBuffer.length());
            attributeSegments.add(as);
        }
    }

    /**
     * A set of style attributes that begin at a particular character index in
     * the paragraph.
     * 
     */
    private static class AttributeSegment
    {
        private final Map attributes;
        private final int startPos;

        /**
         * Construct new attribute segment.
         * 
         * @param attributes
         *            map of style attributes
         * @param startPos
         *            starting character index
         */
        public AttributeSegment(Map attributes, int startPos)
        {
            this.attributes = attributes;
            this.startPos = startPos;
        }
    }
}
