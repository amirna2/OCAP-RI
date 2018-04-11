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

package org.cablelabs.impl.ocap.manager.eas;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceChangeEvent;
import org.dvb.user.UserPreferenceChangeListener;
import org.dvb.user.UserPreferenceManager;
import org.ocap.system.EASHandler;
import org.ocap.system.EASModuleRegistrar;

/**
 * This factory class generates instances of text-based alerts using the text
 * attributes established by an {@link EASHandler}.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
class EASAlertTextFactory implements UserPreferenceChangeListener
{
    // Constants

    public static final String[] DEFAULT_USER_LANGUAGE = new String[] { "eng" }; // ISO
                                                                                 // 639-2
                                                                                 // code
                                                                                 // for
                                                                                 // English

    private static final Color DEFAULT_BACK_COLOR = Color.RED.darker();

    private static final Float DEFAULT_BACK_OPACITY = new Float(EASAlertTextFactory.OPAQUE);

    private static final Color DEFAULT_FONT_COLOR = Color.WHITE;

    private static final String DEFAULT_FONT_FACE = "Tiresias";

    private static final Float DEFAULT_FONT_OPACITY = new Float(EASAlertTextFactory.OPAQUE);

    private static final Integer DEFAULT_FONT_SIZE = new Integer(26);

    private static final String DEFAULT_FONT_STYLE = "PLAIN";

    /*
     * The minimum color opacity of the alert text font. Values less than this
     * minimum may be too transparent for legibility. This value was chosen
     * based on developer experience as no "better practice" has been found.
     */
    private static final float MINIMUM_FONT_OPACITY = 0.7f;

    /*
     * The transparency of a color (alpha value) represented by a float value in
     * the range 0.0&nbsp;-&nbsp;1.0. An alpha value of 1.0 means that the color
     * is completely opaque and an alpha value of 0.0 means that the color is
     * completely transparent. See {@link java.awt.Color}.
     */
    private static final float OPAQUE = 1.0f;

    private static final float TRANSPARENT = 0.0f;

    // Class Fields

    private static final EASAlertTextFactory s_instance;

    private static final Map s_textAttributes;

    private static final Map s_textCapabilities;

    // Class Methods

    static
    {
        s_textAttributes = new Hashtable(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY); // set
                                                                                         // initial
                                                                                         // size
                                                                                         // to
                                                                                         // include
                                                                                         // opacity
        s_textCapabilities = new HashMap(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR); // set
                                                                                       // initial
                                                                                       // size
                                                                                       // to
                                                                                       // exclude
                                                                                       // opacity

        EASAlertTextFactory.initializeFontColor();
        EASAlertTextFactory.initializeFontStyle();
        EASAlertTextFactory.initializeFontFace();
        EASAlertTextFactory.initializeFontSize();
        EASAlertTextFactory.initializeBackgroundColor();

        EASAlertTextFactory.s_textAttributes.put(new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_OPACITY),
                EASAlertTextFactory.DEFAULT_FONT_OPACITY);
        EASAlertTextFactory.s_textAttributes.put(new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY),
                EASAlertTextFactory.DEFAULT_BACK_OPACITY);

        s_instance = new EASAlertTextFactory();
    }

    /**
     * Returns the singleton instance of the receiver.
     * 
     * @return the singleton instance of <code>EASAlertTextFactory</code>
     */
    public static EASAlertTextFactory getInstance()
    {
        return EASAlertTextFactory.s_instance;
    }

    /**
     * Initializes the list of possible background colors, and sets the default
     * background color attribute to a darker {@link Color#RED}. A darker hue of
     * the standard color definitions in {@link java.awt.Color} are used to fill
     * in this capability list.
     */
    private static void initializeBackgroundColor()
    {
        Integer key = new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR);
        EASAlertTextFactory.s_textAttributes.put(key, EASAlertTextFactory.DEFAULT_BACK_COLOR);
        EASAlertTextFactory.s_textCapabilities.put(key, Arrays.asList(new Color[] { Color.BLACK, Color.BLUE.darker(),
                Color.CYAN.darker(), Color.DARK_GRAY.darker(), Color.GRAY.darker(), Color.GREEN.darker(),
                Color.LIGHT_GRAY.darker(), Color.MAGENTA.darker(), Color.ORANGE.darker(), Color.PINK.darker(),
                Color.RED.darker(), Color.WHITE.darker(), Color.YELLOW.darker(), }));
    }

    /**
     * Initializes the list of possible font colors, and sets the default font
     * (foreground) color attribute to {@link Color#WHITE}. The standard color
     * definitions in {@link java.awt.Color} are used to fill in this capability
     * list.
     */
    private static void initializeFontColor()
    {
        Integer key = new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR);
        EASAlertTextFactory.s_textAttributes.put(key, EASAlertTextFactory.DEFAULT_FONT_COLOR);
        EASAlertTextFactory.s_textCapabilities.put(key, Arrays.asList(new Color[] { Color.BLACK, Color.BLUE,
                Color.CYAN, Color.DARK_GRAY, Color.GRAY, Color.GREEN, Color.LIGHT_GRAY, Color.MAGENTA, Color.ORANGE,
                Color.PINK, Color.RED, Color.WHITE, Color.YELLOW, }));
    }

    /**
     * Initializes the list of possible font faces from the
     * {@link GraphicsEnvironment#getAvailableFontFamilyNames()}, and sets the
     * default font face attribute to "Tiresias".
     * <p>
     * Note: support for the "Tiresias" font face is required by OCAP.
     */
    private static void initializeFontFace()
    {
        Integer key = new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE);
        GraphicsEnvironment graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        EASAlertTextFactory.s_textAttributes.put(key, EASAlertTextFactory.DEFAULT_FONT_FACE);
        EASAlertTextFactory.s_textCapabilities.put(key, Arrays.asList(graphicsEnv.getAvailableFontFamilyNames()));
    }

    /**
     * Initializes the list of possible font sizes, and sets the default font
     * size attribute to 26. The list of supported font sizes was carried over
     * from a prior implementation.
     */
    private static void initializeFontSize()
    {
        Integer key = new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE);
        EASAlertTextFactory.s_textAttributes.put(key, EASAlertTextFactory.DEFAULT_FONT_SIZE);
        EASAlertTextFactory.s_textCapabilities.put(key, Arrays.asList(new Integer[] { new Integer(14), new Integer(18),
                new Integer(24), new Integer(26), new Integer(31), new Integer(36), }));
    }

    /**
     * Initializes the list of possible font styles supported by
     * {@link java.awt.Font}, and sets the default font style attribute to
     * "PLAIN".
     */
    private static void initializeFontStyle()
    {
        Integer key = new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_STYLE);
        EASAlertTextFactory.s_textAttributes.put(key, EASAlertTextFactory.DEFAULT_FONT_STYLE);
        EASAlertTextFactory.s_textCapabilities.put(key, Arrays.asList(new String[] { "PLAIN", "BOLD", "ITALIC",
                "BOLD|ITALIC", }));
    }

    // Instance Fields

    private final UserPreferenceManager m_preferenceManager;

    private String[] m_preferredLanguages = EASAlertTextFactory.DEFAULT_USER_LANGUAGE;

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     */
    private EASAlertTextFactory()
    {
        this.m_preferenceManager = UserPreferenceManager.getInstance();

        // Instance initialization complete...
        // Monitor for user language preference changes if the user preference
        // manager exists; otherwise default to English
        if (this.m_preferenceManager != null)
        {
            updatePreferredLanguages();
            this.m_preferenceManager.addUserPreferenceChangeListener(this);
        }
    }

    // Instance Methods

    /**
     * Creates a new instance of {@link EASAlertTextAudio} for displaying a
     * scrolling emergency alert text across the screen and concurrently playing
     * back an audio track.
     * <p>
     * Note, the instance is initialized in three steps to split out the setting
     * of font/color attributes and preferred user languages in case support for
     * dynamic updates of the attributes is required. Such support would require
     * the retention of the newly-created alert for update notifications, and
     * subsequent disposal of that reference when the alert completes.
     * 
     * @param state
     *            a {@link EASState} reference for method callbacks
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return an {@link EASAlertTextAudio} object
     */
    public EASAlertTextAudio createTextAudio(final EASState state, final EASMessage message)
    {
        EASAlertTextAudio audioAlert = new EASAlertTextAudio(state, message);
        audioAlert.updateAttributes(getFont(), getFontColor(), getBackgroundColor());
        audioAlert.updatePreferredLanguages(this.m_preferredLanguages);
        return audioAlert;
    }

    /**
     * Creates a new instance of {@link EASAlertTextOnly} for displaying a
     * scrolling emergency alert text across the screen.
     * <p>
     * Note, the instance is initialized in three steps to split out the setting
     * of font/color attributes and preferred user languages in case support for
     * dynamic updates of the attributes is required. Such support would require
     * the retention of the newly-created alert for update notifications, and
     * subsequent disposal of that reference when the alert completes.
     * 
     * @param state
     *            a {@link EASState} reference for method callbacks
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return an {@link EASAlertTextOnly} object
     */
    public EASAlertTextOnly createTextOnly(final EASState state, final EASMessage message)
    {
        EASAlertTextOnly textAlert = new EASAlertTextOnly(state, message);
        textAlert.updateAttributes(getFont(), getFontColor(), getBackgroundColor());
        textAlert.updatePreferredLanguages(this.m_preferredLanguages);
        return textAlert;
    }

    /**
     * Returns the current value of the given EAS alert attribute that is
     * applied to an EAS alert text on a screen.
     * 
     * @param attribute
     *            one of the {@link EASModuleRegistrar} constants, having a
     *            prefix of <code>EAS_ATTRIBUTE_</code>, that specifies the EAS
     *            attribute of interest
     * @return an <code>Object</code> representing the current value for the
     *         given <code>attribute</code>
     * @throws IllegalArgumentException
     *             if the attribute is unknown
     * @see org.ocap.system.EASModuleRegistrar#getEASAttribute(int attribute)
     */
    public Object getEASAttribute(final int attribute)
    {
        Object value = EASAlertTextFactory.s_textAttributes.get(new Integer(attribute));

        if (null == value)
        {
            throw new IllegalArgumentException("Invalid EAS attribute:<" + attribute + ">");
        }

        return value;
    }

    /**
     * Returns the possible values for a given EAS alert attribute that can be
     * applied to EAS alert text on a screen.
     * 
     * @param attribute
     *            one of the {@link EASModuleRegistrar} constants, having a
     *            prefix of <code>EAS_ATTRIBUTE_</code>, that specifies the EAS
     *            attribute of interest
     * @return an <code>Object</code> array of possible values for the given
     *         <code>attribute</code>
     * @throws IllegalArgumentException
     *             if the attribute is unknown
     * @see org.ocap.system.EASModuleRegistrar#getEASCapability(int attribute)
     */
    public Object[] getEASCapability(final int attribute)
    {
        List capabilities = (List) EASAlertTextFactory.s_textCapabilities.get(new Integer(attribute));

        if (null == capabilities)
        {
            throw new IllegalArgumentException("Invalid EAS attribute:<" + attribute + ">");
        }

        return capabilities.toArray();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dvb.user.UserPreferenceChangeListener#receiveUserPreferenceChangeEvent
     * (org.dvb.user.UserPreferenceChangeEvent)
     */
    public void receiveUserPreferenceChangeEvent(UserPreferenceChangeEvent e)
    {
        if ("User Language".equalsIgnoreCase(e.getName()))
        {
            updatePreferredLanguages();
        }
    }

    /**
     * Sets the preferred attribute values to be applied to EAS alert text on a
     * screen.
     * <p>
     * If a specified attribute value is invalid (i.e., the value is not
     * included in the capabilities array returned by
     * {@link #getEASCapability(int)}), this method doesn't change any of the
     * current attribute values but throws an exception.
     * <p>
     * In the special cases of <code>EAS_ATTRIBUTE_FONT_OPACITY</code> and
     * <code>EAS_ATTRIBUTE_BACK_OPACITY</code>, the floating point value is
     * normalized to the valid alpha range of 0.0f through 1.0f instead of
     * throwing an exception. For example, -42.5f is normalized to 0.0f and 42.5
     * is normalized to 1.0f. 0.42f is unchanged. This behavior is in accordance
     * with the {@link EASModuleRegistrar#setEASAttribute(int[], Object[])} API
     * description.
     * 
     * @param attributes
     *            an array of {@link EASModuleRegistrar} constants, having a
     *            prefix of <code>EAS_ATTRIBUTE_</code>, that specifies the EAS
     *            attributes to change
     * @param values
     *            an array of preferred attribute values to be set for alert
     *            text. The i-th item of the <code>value</code> array
     *            corresponds to the i-th item of the <code>attribute</code>
     *            array.
     * @throws IllegalArgumentException
     *             if the attribute is out of range, or the value is not a
     *             possible value, or if the specified preference conflicts with
     *             FCC rules or SCTE 18. For example, an EAS message is
     *             invisible since same color is specified to a font and
     *             background.
     * @see org.ocap.system.EASModuleRegistrar#setEASAttribute(int[], Object[])
     */
    public void setEASAttribute(final int attributes[], final Object values[])
    {
        if (null == attributes || null == values || attributes.length != values.length)
        {
            throw new IllegalArgumentException("Arrays must be non-null and have equal length");
        }
        else if (attributes.length == 0)
        {
            return; // nothing to set
        }

        Map proposedSettings = new HashMap(EASAlertTextFactory.s_textAttributes); // copy
                                                                                  // existing
                                                                                  // settings
        for (int i = 0; i < attributes.length; ++i)
        {
            Integer key = new Integer(attributes[i]);
            try
            {
                switch (attributes[i])
                {
                    case EASModuleRegistrar.EAS_ATTRIBUTE_FONT_OPACITY:
                    case EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY:
                    {
                        float value = ((Float) values[i]).floatValue();

                        // Per EASModuleRegistrar the opacity will be normalized
                        // to 0..1 range instead of throwing an exception
                        value = Math.min(EASAlertTextFactory.OPAQUE, Math.max(EASAlertTextFactory.TRANSPARENT, value));
                        values[i] = new Float(value);
                        break;
                    }
                    default:
                    {
                        List capabilities = (List) EASAlertTextFactory.s_textCapabilities.get(key);
                        if (null == capabilities)
                        {
                            throw new IllegalArgumentException("Invalid EAS attribute:<" + key + ">");
                        }
                        else if (!capabilities.contains(values[i]))
                        {
                            throw new IllegalArgumentException("Invalid value:<" + values[i] + "> for attribute:<"
                                    + key + ">");
                        }
                    }
                }

                proposedSettings.put(key, values[i]);
            }
            catch (ClassCastException e)
            {
                throw new IllegalArgumentException("Can't cast value:<" + values[i] + "> to attribute type:<" + key
                        + ">");
            }
        }

        checkForColorConflict(proposedSettings);
        checkForOpacityConflict(proposedSettings);
        EASAlertTextFactory.s_textAttributes.putAll(proposedSettings);
    }

    /**
     * Checks for conflicts between the proposed font and background color
     * settings, and for conflicts with FCC rules or SCTE 18. For example, if
     * the font and background colors were the same, the alert text would become
     * invisible.
     * <p>
     * <a href="http://www.w3.org/WAI/ER/WD-AERT/#color-contrast">W3C
     * formulas</a> for color difference and color brightness are used to
     * determine if there's sufficient contrast between the font and background
     * colors when viewed by someone having color deficits or when viewed on a
     * black and white screen.
     * <p>
     * <b>Note:</b> <em>hard-coded (wired) numeric literals are used instead of
     * named constants to deter any attempt to alter these values and possibly
     * break the W3C formulas.</em>
     * 
     * @param proposedSettings
     *            a map of the proposed EAS text attribute settings
     * @throws IllegalArgumentException
     *             if the font and background color settings would make the
     *             alert text invisible or unreadable
     */
    private void checkForColorConflict(final Map proposedSettings)
    {
        Color fontColor = (Color) proposedSettings.get(new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR));
        Color backColor = (Color) proposedSettings.get(new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR));

        int colorDiff = Math.abs(fontColor.getRed() - backColor.getRed())
                + Math.abs(fontColor.getGreen() - backColor.getGreen())
                + Math.abs(fontColor.getBlue() - backColor.getBlue());
        if (colorDiff <= 500) // wired magic number so as to not break the W3C
                              // formula
        {
            throw new IllegalArgumentException("Insufficient difference between font and background colors");
        }

        int colorBright = (((fontColor.getRed() * 299) + (fontColor.getGreen() * 587) + (fontColor.getBlue() * 114)) / 1000)
                - (((backColor.getRed() * 299) + (backColor.getGreen() * 587) + (backColor.getBlue() * 114)) / 1000);
        if (Math.abs(colorBright) <= 125) // wired magic numbers so as to not
                                          // break the W3C formula
        {
            throw new IllegalArgumentException("Insufficient difference between font and background color brightnesses");
        }
    }

    /**
     * Checks for conflicts between the proposed font and background opacity
     * settings, and for conflicts with FCC rules or SCTE 18. For example, if
     * the font opacity was fully transparent, the alert text would be rendered
     * invisible.
     * <p>
     * At this time, the font opacity must be &gt;= 70% for this check to pass.
     * No check is make on the background opacity or its contrast with the font
     * opacity.
     * 
     * @param proposedSettings
     *            a map of the proposed EAS text attribute settings
     * @throws IllegalArgumentException
     *             if the font and background opacity settings would make the
     *             alert text invisible or unreadable
     */
    private void checkForOpacityConflict(final Map proposedSettings)
    {
        float fontOpacity = ((Float) proposedSettings.get(new Integer(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_OPACITY))).floatValue();
        // float backOpacity = ((Float) proposedSettings.get(new
        // Integer(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY))).floatValue();

        if (fontOpacity < EASAlertTextFactory.MINIMUM_FONT_OPACITY)
        {
            throw new IllegalArgumentException("Font too transparent for readability:<" + fontOpacity + ">");
        }

        // TODO: find a formula for opacity contrast between foreground and
        // background (if there is such a thing)
    }

    /**
     * Returns a new {@link Color} object representing the current background
     * color and opacity.
     * 
     * @return the current background color
     */
    private Color getBackgroundColor()
    {
        return getColor(EASModuleRegistrar.EAS_ATTRIBUTE_BACK_COLOR, EASModuleRegistrar.EAS_ATTRIBUTE_BACK_OPACITY);
    }

    /**
     * Returns a new {@link Color} object representing the current color and
     * opacity corresponding to the given EAS attributes.
     * 
     * @return the current color
     */
    private Color getColor(final int color, final int opacity)
    {
        Color base = (Color) getEASAttribute(color);
        int alpha = (int) (((Float) getEASAttribute(opacity)).floatValue() * 255f + 0.5f);

        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    /**
     * Returns a new {@link Font} object representing the current font face,
     * size, and style settings.
     * 
     * @return font the current font settings
     */
    private Font getFont()
    {
        String fontFace = (String) getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_FACE);
        int fontSize = ((Integer) getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_SIZE)).intValue();
        String fontStyle = (String) getEASAttribute(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_STYLE);
        int style = Font.PLAIN;

        if ("PLAIN".equals(fontStyle))
        {
            style = Font.PLAIN;
        }
        else if ("BOLD".equals(fontStyle))
        {
            style = Font.BOLD;
        }
        else if ("ITALIC".equals(fontStyle))
        {
            style = Font.ITALIC;
        }
        else if ("BOLD|ITALIC".equals(fontStyle))
        {
            style = Font.BOLD | Font.ITALIC;
        }

        return new Font(fontFace, style, fontSize);
    }

    /**
     * Returns a new {@link Color} object representing the current font
     * (foreground) color and opacity.
     * 
     * @return the current font color
     */
    private Color getFontColor()
    {
        return getColor(EASModuleRegistrar.EAS_ATTRIBUTE_FONT_COLOR, EASModuleRegistrar.EAS_ATTRIBUTE_FONT_OPACITY);
    }

    /**
     * Updates the user-preferred language settings from the User Preference
     * Manager. Defaults to ISO 639-2 English ("eng") if no value sets are
     * defined for user language preference.
     */
    private synchronized void updatePreferredLanguages()
    {
        GeneralPreference preference = new GeneralPreference("User Language");
        this.m_preferenceManager.read(preference);
        String[] languageCodes = preference.getFavourites();
        this.m_preferredLanguages = (languageCodes.length == 0) ? EASAlertTextFactory.DEFAULT_USER_LANGUAGE
                : languageCodes;
    }

}
