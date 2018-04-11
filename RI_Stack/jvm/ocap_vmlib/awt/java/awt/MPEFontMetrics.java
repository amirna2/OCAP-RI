/*
 * @(#)MWFontMetrics.java	1.25 06/10/10
 *
 * Copyright  1990-2006 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version
 * 2 only, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included at /legal/license.txt).
 *
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, CA 95054 or visit www.sun.com if you need additional
 * information or have any questions.
 *
 */

package java.awt;

import java.util.Map;
import java.util.HashMap;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;

/**
 * A font metrics object for a font.
 *
 * @version 1.18, 05/14/02
 * @author Peter Stewart (PBP-RI MicroWindows version - MWFontMetrics)
 * @author Aaron Kamienski (MPEFontMetrics from MWFontMetrics)
 * @author David Hooley (incorporated Sun's GPL2 notice)
 *
 * Note: This is a renamed (and possibly modified) version of a phoneme file (MWFontMetrics.java)
 */
class MPEFontMetrics extends FontMetrics
{

    // serialVersionUID - 4683929
    static final long serialVersionUID = -4956160226949100590L;

    private static native void initIDs();

    static
    {
        initIDs();
    }

    /**
     * The standard ascent of the font. This is the logical height above the
     * baseline for the Alphanumeric characters and should be used for
     * determining line spacing. Note, however, that some characters in the font
     * may extend above this height.
     */
    int ascent;

    /**
     * The standard descent of the font. This is the logical height below the
     * baseline for the Alphanumeric characters and should be used for
     * determining line spacing. Note, however, that some characters in the font
     * may extend below this height.
     */
    int descent;

    /**
     * The standard leading for the font. This is the logical amount of space to
     * be reserved between the descent of one line of text and the ascent of the
     * next line. The height metric is calculated to include this extra space.
     */
    int leading;

    /**
     * The standard height of a line of text in this font. This is the distance
     * between the baseline of adjacent lines of text. It is the sum of the
     * ascent+descent+leading. There is no guarantee that lines of text spaced
     * at this distance will be disjoint; such lines may overlap if some
     * characters overshoot the standard ascent and descent metrics.
     */
    int height;

    /**
     * The maximum ascent for all characters in this font. No character will
     * extend further above the baseline than this metric.
     */
    int maxAscent;

    /**
     * The maximum descent for all characters in this font. No character will
     * descend further below the baseline than this metric.
     */
    int maxDescent;

    /**
     * The maximum possible height of a line of text in this font. Adjacent
     * lines of text spaced this distance apart will be guaranteed not to
     * overlap. Note, however, that many paragraphs that contain ordinary
     * alphanumeric text may look too widely spaced if this metric is used to
     * determine line spacing. The height field should be preferred unless the
     * text in a given line contains particularly tall characters.
     */
    int maxHeight;

    /**
     * The maximum advance width of any character in this font.
     */
    int maxAdvance;

    /** Native font handle */
    int nativeFont;

    /**
     * Cache of first 256 Unicode characters as these map to ASCII characters
     * and are often used.
     */
    private int[] widths;

    /**
     * Loads MPE Font and sets up the Font Metric fields
     *
     * @param ff
     *            the FontFactory to use or <code>null</code>.
     * @param fontName
     *            the name of the font to load from the given font factory (or
     *            the system font factory)
     * @param fontStyle
     *            the desired font style
     * @param fontHeight
     *            the desired font height
     */
    private native int pLoadFont(int ff, String fontName, int fontStyle, int fontHeight);

    /**
     * Frees up the resources used by the given nativeFont.
     */
    private static native void pDestroyFont(int nativeFont);

    /**
     * Queries the pixel width of the given character when rendered in the given
     * nativeFont.
     */
    private static native int pCharWidth(int nativeFont, char c);

    /**
     * Queries the pixel width of the given character array slice when rendered
     * in the given nativeFont.
     */
    private static native int pCharsWidth(int nativeFont, char chars[], int offset, int len);

    /**
     * Queries the pixel width of the given string when rendered in the given
     * nativeFont.
     */
    private static native int pStringWidth(int nativeFont, String string);

    /**
     * Retrieves the contents of the <code>private</code> metrics attribute of
     * the given <code>Font</code>.
     *
     * @return the contents of the <code>private</code> metrics attribute of the
     *         given <code>Font</code>
     */
    static native MPEFontMetrics pGetFontMetrics(Font font);

    /**
     * Sets the contents of the <code>private</code> metrics attribute of the
     * given <code>Font</code>.
     */
    static native void pSetFontMetrics(Font font, FontMetrics fm);

    /**
     * A map which maps a native font name and size to a font metrics object.
     * This is used as a cache to prevent loading the same fonts multiple times.
     */
    private static Map fontMetricsMap = new HashMap();

    /**
     * A reference queue that is used to cleanup entries from the
     * {@link #fontMetricsMap}.
     */
    private static ReferenceQueue rq = new ReferenceQueue();

    /**
     * Gets the MPEFontMetrics object for the supplied font. This method caches
     * font metrics to ensure native fonts are not loaded twice for the same
     * font. Assumes the system font factory is used.
     */
    static MPEFontMetrics getFontMetrics(Font font)
    {
        return getFontMetrics(font, 0);
    }

    /**
     * Gets the MPEFontMetrics object for the supplied font. This method caches
     * font metrics to ensure native fonts are not loaded twice for the same
     * font. Uses the given fontFactory peer, which if <code>0</code> implies
     * the system font factory.
     */
    static synchronized MPEFontMetrics getFontMetrics(Font font, int fontFactory)
    {
        if (font == null) throw new NullPointerException("font is null");
        MPEFontMetrics fm = pGetFontMetrics(font);

        if (fm == null)
        {
            String name = font.getName();
            int style = font.getStyle();
            int size = font.getSize();

            if (fontFactory != 0)
            {
                /* We want to return null instead of throwing an error. */
                try
                {
                    fm = new MPEFontMetrics(font, name, style, size, fontFactory);
                }
                catch (AWTError e)
                {
                    fm = null;
                }
            }
            else
            {
                /*
                 * See if a font metrics of the same native name and size has
                 * already been loaded. If it has then we use that one.
                 */
                String key = name.toLowerCase() + "." + style + "." + size;

                FontReference ref = (FontReference) fontMetricsMap.get(key);
                if (ref == null || (fm = (MPEFontMetrics) ref.get()) == null)
                {
                    fm = new MPEFontMetrics(font, name, style, size, fontFactory);
                    ref = new FontReference(fm, rq);
                    fontMetricsMap.put(key, ref);
                }

                /*
                 * Let's do a little bit of house-cleaning while we're accessing
                 * the map. Remove all collected references from the map.
                 */
                while ((ref = (FontReference) rq.poll()) != null)
                {
                    ref.removeFrom(fontMetricsMap);
                }
            }
            pSetFontMetrics(font, fm);
        }

        return fm;
    }

    /**
     * Creates a font metrics for the supplied font. To get a font metrics for a
     * font use the static method getFontMetrics instead which does caching.
     *
     * @throws AWTError
     *             if the native font could not be accessed
     */
    private MPEFontMetrics(Font font, String nativeName, int style, int size, int fontFactory) throws AWTError
    {

        super(font);

        widths = new int[256];
        nativeFont = pLoadFont(fontFactory, nativeName, style, size);
        if (nativeFont == 0)
        {
            widths = null;
            throw new AWTError("Could not load native font");
        }
    }

    /**
     * Returns the list of known system fonts. Currently returns the standard
     * AWT names + the mandatory OCAP Tireseas system font.
     */
    static String[] getFontList()
    {
        return new String[] { "Serif", "SansSerif", "Dialog", "Monospaced", "DialogInput", "Tiresias" };
    }

    /**
     * Get leading.
     */
    public int getLeading()
    {
        return leading;
    }

    /**
     * Get ascent.
     */
    public int getAscent()
    {
        return ascent;
    }

    /**
     * Get descent.
     */
    public int getDescent()
    {
        return descent;
    }

    /**
     * Get height.
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Get maxAscent.
     */
    public int getMaxAscent()
    {
        return maxAscent;
    }

    /**
     * Get maxDescent.
     */
    public int getMaxDescent()
    {
        return maxDescent;
    }

    /**
     * Get maxAdvance.
     */
    public int getMaxAdvance()
    {
        return maxAdvance;
    }

    /**
     * Fast lookup of first 256 chars as these are always the same eg. ASCII
     * charset.
     */
    public int charWidth(char c)
    {
        if (c < 256) return widths[c];

        return pCharWidth(nativeFont, c);
    }

    /**
     * Return the width of the specified string in this Font.
     *
     * @param string
     *            the string to determine a width for
     *
     * @throws NullPointerException
     *             if <i>string</i> is <code>null</code>
     */
    public int stringWidth(String string) throws NullPointerException
    {
        if (string == null) throw new NullPointerException();
        return pStringWidth(nativeFont, string);
    }

    /**
     * Return the width of the specified char[] in this Font.
     *
     * @param chars
     *            the array of characters
     * @param offset
     *            the offset within the array of characters
     * @param length
     *            the number of characters in the array of characters, starting
     *            at the given offset
     *
     * @throws NullPointerException
     *             if <i>charsg</i> is <code>null</code>
     * @throws ArrayIndexOutOfBoundsException
     *             if <i>offset</i> and/or <i>length</i> are invalid
     */
    public int charsWidth(char chars[], int offset, int length) throws NullPointerException,
            ArrayIndexOutOfBoundsException
    {
        if (chars == null) throw new NullPointerException();
        if (offset < 0 || offset > chars.length || length > (chars.length - offset))
            throw new ArrayIndexOutOfBoundsException();
        if (length <= 0) return 0;
        return pCharsWidth(nativeFont, chars, offset, length);
    }

    /**
     * Get the widths of the first 256 characters in the font. This is stupid.
     */
    public int[] getWidths()
    {
        int[] newWidths = new int[256];

        System.arraycopy(widths, 0, newWidths, 0, 256);

        return newWidths;
    }

    /**
     * Destroy the native font.
     */
    protected void finalize() throws Throwable
    {
        pDestroyFont(nativeFont);

        super.finalize();
    }

    /**
     * Extends WeakReference to add a field to remember the key used to look-up
     * the <code>MPEFontMetrics</code> <code>Reference</code> object. This is
     * used to cleanup old references periodically.
     */
    private static class FontReference extends WeakReference
    {
        FontReference(MPEFontMetrics fm, Object key)
        {
            super(fm);
            this.key = key;
        }

        FontReference(MPEFontMetrics fm, Object key, ReferenceQueue q)
        {
            super(fm, q);
            this.key = key;
        }

        /**
         * Should be invoked to remove this <i>key</i>-to-<i>reference</i>
         * mapping from the given <code>Map</code>.
         *
         * @param map
         *            the map to remove the mapping from
         */
        void removeFrom(Map map)
        {
            if (map.get(key) == this) map.remove(key);
        }

        /**
         * The key used to locate this <code>Reference</code> in a
         * <code>Map</code>.
         */
        private Object key;
    }
}
