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

package org.cablelabs.impl.havi.port.mpe;

import org.cablelabs.impl.havi.CapabilitiesSupport;
import org.cablelabs.impl.havi.HEventRepresentationDatabase;

import java.awt.Font;
import java.awt.event.KeyEvent;

import org.havi.ui.HFontCapabilities;
import org.havi.ui.event.HEventGroup;
import org.havi.ui.event.HEventRepresentation;
import org.havi.ui.event.HKeyCapabilities;
import org.havi.ui.event.HMouseCapabilities;
import org.havi.ui.event.HRcCapabilities;
import org.havi.ui.event.HRcEvent;
import org.ocap.ui.event.OCRcEvent;

/**
 * Implementation of the <code>CapabilitiesSupport</code> class for MPE port
 * intended to run on an OCAP implementation.
 * 
 * <p>
 * Note that this implementation depends on Java2 support for the font
 * capabilities methods.
 * <p>
 * Note that the remote control is currently marked as unsupported.
 * 
 * @see HFontCapabilities
 * @see HMouseCapabilities
 * @see HKeyCapabilities
 * @see HRcCapabilities
 * @see HEventRepresentation
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.5 $, $Date: 2002/06/03 21:31:03 $
 */
public class Capabilities extends CapabilitiesSupport
{
    public Capabilities()
    {
        // instantiate HEventRepresentationDatabase
        try
        {
            herDatabase = new HEventRepresentationDatabaseImpl();
        }
        catch (Exception e)
        {
            herDatabase = null;
        }

    }

    /*  ************************ Mouse *************************** */

    /**
     * The mouse is assumed to be not supported; always returns false.
     * 
     * @return <code>true</code> if mouse is supported on this platform;
     *         <code>false</code> if mouse is not supported
     * 
     * @see HMouseCapabilities#getInputDeviceSupported()
     */
    public boolean isMouseSupported()
    {
        return Toolkit.getBoolean(Property.MOUSE, false);
    }

    /*  ************************ Keyboard *************************** */

    /**
     * The keyboard is assumed to be not supported; always returns false.
     * 
     * @return <code>true</code> if keyboard is supported on this platform;
     *         <code>false</code> if keyboard is not supported
     * 
     * @see HKeyCapabilities#getInputDeviceSupported()
     */
    public boolean isKeyboardSupported()
    {
        return Toolkit.getBoolean(Property.KEYBOARD, false);
    }

    /**
     * The keyboard is always supported; all keys within the ranges
     * {@link KeyEvent#KEY_FIRST KeyEvent.KEY_FIRST}- {@link KeyEvent#KEY_LAST
     * KeyEvent.KEY_LAST} are assumed to be supported.
     * 
     * @param the
     *            virtual keycode to query (e.g., <code>KeyEvent.VK_SPACE</code>
     *            )
     * @return <code>true</code> if the given virtual key <code>keycode</code>
     *         is supported (i.e., can ever be generated); <code>false</code> if
     *         the <code>keycode</code> is not supported
     * 
     * @see HKeyCapabilities#isSupported(int vk)
     */
    public boolean isKeySupported(int keycode)
    {
        return isKeyboardSupported() && ((keycode >= KeyEvent.KEY_FIRST && keycode <= KeyEvent.KEY_LAST));
    }

    /*  ************************ Remote ************************* */

    /**
     * Remote control is currently supported; this always returns true.
     * 
     * @return <code>true</code> if remote control is supported on this
     *         platform; <code>false</code> if remote control is not supported
     * 
     * @see HRcCapabilities#getInputDeviceSupported()
     */
    public boolean isRemoteSupported()
    {
        return Toolkit.getBoolean(Property.REMOTE, true);
    }

    /**
     * @param key
     *            the virtual keycode to query (e.g.,
     *            {@link HRcEvent#VK_COLORED_KEY_0})
     * @return <code>true</code> if the given virtual key <code>keycode</code>
     *         is supported (i.e., can ever be generated); <code>false</code> if
     *         the <code>keycode</code> is not supported
     */
    public boolean isRcKeySupported(int keycode)
    {
        // Is remote currently unsupported
        if (false == isRemoteSupported()) return false;

        /*
         * Search the mandatory_keys array for a match. If additional keys are
         * supported then they should be checked also.
         */
        for (int x = 0; x < MANDATORY_KEYS.length; x++)
        {
            if (keycode == MANDATORY_KEYS[x]) return true;
        }
        return false;
    }

    /**
     * Returns the <code>HEventRepresentation</code> object that describes the
     * given event.
     * 
     * @param key
     *            the virtual keycode to acquire an
     *            <code>HEventRepresentation</code>
     * @return the <code>HEventRepresentation</code> object describing this
     *         remote control key; <code>null</code> is returned if the
     *         specified <code>key</code> does not have a valid remote control
     *         representation
     */
    public HEventRepresentation getRepresentation(int key)
    {
        // also return null if we don't have a valid HEvent db
        return (herDatabase == null) ? null : herDatabase.getEventRepresentation(key);
    }

    /**
     * Returns a new <code>HEventGroup</code> containing a subset of the keys
     * for which {#isRcKeySupported} and {#isKeySupported} returns
     * <code>true</code>.
     * 
     * @see org.cablelabs.impl.havi.CapabilitiesSupport#getDefaultKeys()
     * 
     */
    public HEventGroup getDefaultKeys()
    {
        // Currently returns the keys required by OCAP
        // But it might as well include all keys that are actually supported!
        HEventGroup group = new HEventGroup();
        for (int i = 0; i < MANDATORY_KEYS.length; ++i)
            group.addKey(MANDATORY_KEYS[i]);
        return group;
    }

    /**
     * The set of keys that OCAP mandates support for. OCAP I16 Section 25.2.1.2
     */
    private static final int[] MANDATORY_KEYS = { HRcEvent.VK_POWER, HRcEvent.VK_CHANNEL_DOWN, HRcEvent.VK_CHANNEL_UP,
            KeyEvent.VK_0, KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5, KeyEvent.VK_6,
            KeyEvent.VK_7, KeyEvent.VK_9, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
            KeyEvent.VK_ENTER, HRcEvent.VK_VOLUME_DOWN, HRcEvent.VK_VOLUME_UP, HRcEvent.VK_MUTE, KeyEvent.VK_PAUSE,
            HRcEvent.VK_PLAY, HRcEvent.VK_STOP, HRcEvent.VK_RECORD, HRcEvent.VK_FAST_FWD, HRcEvent.VK_REWIND,
            HRcEvent.VK_GUIDE, OCRcEvent.VK_RF_BYPASS, OCRcEvent.VK_MENU, HRcEvent.VK_INFO, OCRcEvent.VK_EXIT,
            OCRcEvent.VK_LAST, HRcEvent.VK_COLORED_KEY_0, HRcEvent.VK_COLORED_KEY_1, HRcEvent.VK_COLORED_KEY_2,
            HRcEvent.VK_COLORED_KEY_3, KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN, OCRcEvent.VK_NEXT_FAVORITE_CHANNEL,
            OCRcEvent.VK_ON_DEMAND, };

    /*  ************************ Fonts ************************* */

    /** The block ranges defined by HFontCapabilities. */
    private static final int rangeIDs[] = { HFontCapabilities.BASIC_LATIN, HFontCapabilities.LATIN_1_SUPPLEMENT,
            HFontCapabilities.LATIN_EXTENDED_A, HFontCapabilities.LATIN_EXTENDED_B, HFontCapabilities.IPA_EXTENSIONS,
            HFontCapabilities.SPACING_MODIFIER_LETTERS, HFontCapabilities.COMBINING_DIACRITICAL_MARKS,
            HFontCapabilities.BASIC_GREEK, HFontCapabilities.GREEK_SYMBOLS_AND_COPTIC, HFontCapabilities.CYRILLIC,
            HFontCapabilities.ARMENIAN, HFontCapabilities.BASIC_HEBREW, HFontCapabilities.HEBREW_EXTENDED,
            HFontCapabilities.BASIC_ARABIC, HFontCapabilities.ARABIC_EXTENDED, HFontCapabilities.DEVANAGARI,
            HFontCapabilities.BENGALI, HFontCapabilities.GURMUKHI, HFontCapabilities.GUJARATI, HFontCapabilities.ORIYA,
            HFontCapabilities.TAMIL, HFontCapabilities.TELUGU, HFontCapabilities.KANNADA, HFontCapabilities.MALAYALAM,
            HFontCapabilities.THAI, HFontCapabilities.LAO, HFontCapabilities.BASIC_GEORGIAN,
            HFontCapabilities.GEORGIAN_EXTENDED, HFontCapabilities.HANGUL_JAMO,
            HFontCapabilities.LATIN_EXTENDED_ADDITIONAL, HFontCapabilities.GREEK_EXTENDED,
            HFontCapabilities.GENERAL_PUNCTUATION, HFontCapabilities.SUPERSCRIPTS_AND_SUBSCRIPTS,
            HFontCapabilities.CURRENCY_SYMBOLS, HFontCapabilities.COMBINING_DIACTRICAL_MARKS_FOR_SYMBOLS,
            HFontCapabilities.LETTERLIKE_SYMBOLS, HFontCapabilities.NUMBER_FORMS, HFontCapabilities.ARROWS,
            HFontCapabilities.MATHEMATICAL_OPERATORS, HFontCapabilities.MISCELLANEOUS_TECHNICAL,
            HFontCapabilities.CONTROL_PICTURES, HFontCapabilities.OPTICAL_CHARACTER_RECOGNITION,
            HFontCapabilities.ENCLOSED_ALPHANUMERICS, HFontCapabilities.BOX_DRAWING, HFontCapabilities.BLOCK_ELEMENTS,
            HFontCapabilities.GEOMETRICAL_SHAPES, HFontCapabilities.MISCELLANEOUS_SYMBOLS, HFontCapabilities.DINGBATS,
            HFontCapabilities.CJK_SYMBOLS_AND_PUNCTUATION, HFontCapabilities.HIRAGANA, HFontCapabilities.KATAKANA,
            HFontCapabilities.BOPOMOFO, HFontCapabilities.HANGUL_COMPATIBILITY_JAMO,
            HFontCapabilities.CJK_MISCELLANEOUS, HFontCapabilities.ENCLOSED_CJK_LETTERS_AND_MONTHS,
            HFontCapabilities.CJK_COMPATIBILITY, HFontCapabilities.HANGUL, HFontCapabilities.HANGUL_SUPPLEMENTARY_A,
            HFontCapabilities.HANGUL_SUPPLEMENTARY_B, HFontCapabilities.CJK_UNIFIED_IDEOGRAPHS,
            HFontCapabilities.PRIVATE_USE_AREA, HFontCapabilities.CJK_COMPATIBILITY_IDEOGRAPHS,
            HFontCapabilities.ALPHABETIC_PRESENTATION_FORMS_A, HFontCapabilities.ARABIC_PRESENTATION_FORMS_A,
            HFontCapabilities.COMBINING_HALF_MARKS, HFontCapabilities.CJK_COMPATIBILITY_FORMS,
            HFontCapabilities.SMALL_FORM_VARIANTS, HFontCapabilities.ARABIC_PRESENTATION_FORMS_B,
            HFontCapabilities.HALFWIDTH_AND_FULLWIDTH_FORMS, HFontCapabilities.SPECIALS, };

    /**
     * The block ranges defined by HFontCapabilities.
     */
    private static final char range[] = { '\u0020', '\u007E', // BASIC_LATIN
            '\u00A0', '\u00FF', // LATIN_1_SUPPLEMENT
            '\u0100', '\u017F', // LATIN_EXTENDED_A
            '\u0180', '\u024F', // LATIN_EXTENDED_B
            '\u0250', '\u02AF', // IPA_EXTENSIONS
            '\u02B0', '\u02FF', // SPACING_MODIFIER_LETTERS
            '\u0300', '\u036F', // COMBINING_DIACRITICAL_MARKS
            '\u0370', '\u03CF', // BASIC_GREEK
            '\u03D0', '\u03FF', // GREEK_SYMBOLS_AND_COPTIC
            '\u0400', '\u04FF', // CYRILLIC
            '\u0530', '\u058F', // ARMENIAN
            '\u05D0', '\u05EA', // BASIC_HEBREW
            '\u0590', '\u05CF', // HEBREW_EXTENDED
            '\u0600', '\u0652', // BASIC_ARABIC
            '\u0653', '\u06FF', // ARABIC_EXTENDED
            '\u0900', '\u097F', // DEVANAGARI
            '\u0980', '\u09FF', // BENGALI
            '\u0A00', '\u0A7F', // GURMUKHI
            '\u0A80', '\u0AFF', // GUJARATI
            '\u0B00', '\u0B7F', // ORIYA
            '\u0B80', '\u0BFF', // TAMIL
            '\u0C00', '\u0C7F', // TELUGU
            '\u0C80', '\u0CFF', // KANNADA
            '\u0D00', '\u0D7F', // MALAYALAM
            '\u0E00', '\u0E7F', // THAI
            '\u0E80', '\u0EFF', // LAO
            '\u10D0', '\u10FF', // BASIC_GEORGIAN
            '\u10A0', '\u10CF', // GEORGIAN_EXTENDED
            '\u1100', '\u11FF', // HANGUL_JAMO
            '\u1E00', '\u1EFF', // LATIN_EXTENDED_ADDITIONAL
            '\u1F00', '\u1FFF', // GREEK_EXTENDED
            '\u2000', '\u206F', // GENERAL_PUNCTUATION
            '\u2070', '\u209F', // SUPERSCRIPTS_AND_SUBSCRIPTS
            '\u20A0', '\u20CF', // CURRENCY_SYMBOLS
            '\u20D0', '\u20FF', // COMBINING_DIACTRICAL_MARKS_FOR_SYMBOLS
            '\u2100', '\u214F', // LETTERLIKE_SYMBOLS
            '\u2150', '\u218F', // NUMBER_FORMS
            '\u2190', '\u21FF', // ARROWS
            '\u2200', '\u22FF', // MATHEMATICAL_OPERATORS
            '\u2300', '\u23FF', // MISCELLANEOUS_TECHNICAL
            '\u2400', '\u243F', // CONTROL_PICTURES
            '\u2440', '\u245F', // OPTICAL_CHARACTER_RECOGNITION
            '\u2460', '\u24FF', // ENCLOSED_ALPHANUMERICS
            '\u2500', '\u257F', // BOX_DRAWING
            '\u2580', '\u259F', // BLOCK_ELEMENTS
            '\u25A0', '\u25FF', // GEOMETRICAL_SHAPES
            '\u2600', '\u26FF', // MISCELLANEOUS_SYMBOLS
            '\u2700', '\u27BF', // DINGBATS
            '\u3000', '\u303F', // CJK_SYMBOLS_AND_PUNCTUATION
            '\u3040', '\u309F', // HIRAGANA
            '\u30A0', '\u30FF', // KATAKANA
            '\u3100', '\u312F', // BOPOMOFO
            '\u3130', '\u318F', // HANGUL_COMPATIBILITY_JAMO
            '\u3190', '\u319F', // CJK_MISCELLANEOUS
            '\u3200', '\u32FF', // ENCLOSED_CJK_LETTERS_AND_MONTHS
            '\u3300', '\u33FF', // CJK_COMPATIBILITY
            '\u3400', '\u3D2D', // HANGUL
            '\u3D2E', '\u44B7', // HANGUL_SUPPLEMENTARY_A
            '\u44B8', '\u4DFF', // HANGUL_SUPPLEMENTARY_B
            '\u4E00', '\u9FFF', // CJK_UNIFIED_IDEOGRAPHS
            '\uE000', '\uF8FF', // PRIVATE_USE_AREA
            '\uF900', '\uFAFF', // CJK_COMPATIBILITY_IDEOGRAPHS
            '\uFB00', '\uFB4F', // ALPHABETIC_PRESENTATION_FORMS_A
            '\uFB50', '\uFDFF', // ARABIC_PRESENTATION_FORMS_A
            '\uFE20', '\uFE2F', // COMBINING_HALF_MARKS
            '\uFE30', '\uFE4F', // CJK_COMPATIBILITY_FORMS
            '\uFE50', '\uFE6F', // SMALL_FORM_VARIANTS
            '\uFE70', '\uFEFE', // ARABIC_PRESENTATION_FORMS_B
            '\uFF00', '\uFFEF', // HALFWIDTH_AND_FULLWIDTH_FORMS
            '\uFFF0', '\uFFFD', // SPECIALS
    };

    // Definition copied from superclass
    public int[] getCharRanges(Font font)
    {
        int array[] = new int[range.length]; // allocate the max length
        int n = 0;

        // Look at each range and see if the 1st char is available
        // If it is, that range gets added to the array
        for (int i = 0, j = 0; i + 1 < range.length; i += 2, ++j)
        {
            // Just try 1st char in each block
            if (isCharAvailable(font, range[i])) array[n++] = rangeIDs[j];
        }

        // Copy the array to a shorter copy
        int copy[] = new int[n];
        System.arraycopy(array, 0, copy, 0, copy.length);
        return copy;
    }

    // Definition copied from superclass
    public boolean isCharAvailable(Font font, char c)
    {
        // return font.canDisplay(c);
        return true;
    }

    // Definition copied from superclass
    public boolean fontAccessible(java.awt.Font f)
    {
        return true;
    }

    // Definition copied from superclass
    public void downloadFont(java.awt.Font f)
    {
        // Do nothing
    }

    HEventRepresentationDatabase herDatabase;
}
