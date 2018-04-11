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

package org.dvb.user;

import java.util.Hashtable;

/**
 * This class defines a set of general preferences. These preferences are read
 * from the receiver and each application (downloaded or not) can access them
 * through the <code>UserPreferenceManager.read</code> method. The standardized
 * preferences are "User Language", "Parental Rating", "User Name",
 * "User Address", "User @", "Country Code", "Default Font Size".
 * <p>
 * When constructed, objects of this class are empty and have no values defined.
 * Values may be added using the add methods inherited from the Preference class
 * or by calling <code>UserPreferenceManager.read</code>.
 * <p>
 * The encodings of these standardized preferences are as follows.
 * <ul>
 * <li>User Language: 3 letter ISO 639 language codes;
 * <li>Parental Rating: string using the same encoding as returned by
 * <code>javax.tv.service.guide.ContentRatingAdvisory.getDisplayText</code>;
 * <li>User Name: Name of the user. This shall be in an order that is
 * appropriate for presentation directly to the user, e.g. in Western Europe,
 * listing the first name first and the family name last is recommended as being
 * culturally appropriate in many locales.
 * <li>User Address: postal address of the user, may contain multiple lines
 * separated by carriage return characters (as defined in table D-4).
 * <li>User @: e-mail address of the user in the SMTP form as defined in RFC821;
 * <li>Country Code: two letter ISO 3166-1 country code;
 * <li>Default Font Size: preferred font size for normal body text expressed in
 * points, decimal integer value encoded as a string (26 is the default;
 * differing size indicates a preference of different font size than usual)
 * </ul>
 * The preference names are treated as case-insensitive. The preference names
 * shall be considered equal at least when the method
 * java.lang.String.equalsIgnoreCase() returns true for the strings when the
 * locale "EN.UK" is used. Depending on the locale used in the implementation,
 * implementations are allowed to consider equal also other upper and lower case
 * character pairs in addition to those defined by the "EN.UK" locale.
 * <p>
 * The standardized preference names in the present document shall only use such
 * letters where the upper and lower case characters are recognized by the
 * "EN.UK" locale.
 */
public final class GeneralPreference extends Preference
{
    /**
     * Constructs a GeneralPreference object. A general preference maps a
     * preference name to a list of strings.
     *
     * @param name
     *            the general preference name.
     *
     * @exception IllegalArgumentException
     *                if the preference's name is not supported.
     */
    public GeneralPreference(String name) throws IllegalArgumentException
    {
        // Invoke the default constructor to create a preference object
        // with no values.
        super(name, (String) null);

        // Make sure it is an allowed preference name
        if (preferences.get(name.toLowerCase()) == null)
        {
            throw new IllegalArgumentException("Invalid preference name");
        }
    }

    /**
     * Validates that the value(s) for this preference are all formatted according
     * to their expected value ranges as indicated in the MHP and OCAP specs
     *
     * @throws UnsupportedPreferenceException if one or more of the values assigned
     * to this preference are not properly formatted
     */
    void validate()
        throws UnsupportedPreferenceException
    {
        PreferenceImpl pi = (PreferenceImpl)preferences.get(getName().toLowerCase());
        String[] values = getFavourites();
        if (values != null)
        {
            for (int i = 0; i < values.length; i++)
            {
                pi.validate(values[i]);
            }
        }
    }

    /**
     * Represents a User Preference defind by OCAP or MHP
     */
    private static class PreferenceImpl
    {
        /**
         * Validates the given value to see if it complies with values allowed for
         * this particular preference
         *
         * @param values the values to validate
         * @throws UnsupportedPreferenceException if the given value is not valid
         * for this particular preference
         */
        public void validate(String value) throws UnsupportedPreferenceException { }
    }

    private static final String PREF_USER_LANGUAGE = "User Language";
    private static final String PREF_PARENTAL_RATING = "Parental Rating";
    private static final String PREF_USER_NAME = "User Name";
    private static final String PREF_USER_ADDRESS = "User Address";
    private static final String PREF_USER_EMAIL = "User @";
    private static final String PREF_COUNTRY_CODE = "Country Code";
    private static final String PREF_DEFAULT_FONT_SIZE = "Default Font Size";
    private static final String PREF_ANALOG_AUDIO = "Analog Audio";
    private static final String PREF_CLOSED_CAPTION_ON = "Closed Caption On";
    private static final String PREF_ANALOG_CLOSED_CAPTION_SERVICE = "Analog Closed Caption Service";
    private static final String PREF_DIGITAL_CLOSED_CAPTION_SERVICE = "Digital Closed Caption Service";

    private static Hashtable preferences = new Hashtable();

    // Initialize our list of supported preferences along with a method that can validate
    // potential values to be assigned to this preference
    static
    {
        // Must be a 3-character ISO 639 language code
        preferences.put(PREF_USER_LANGUAGE.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                // A zero length language is used as a valid setting otherwise must be 3 characters
                if ((value.length() != 3) && (value.length() != 0))
                {
                    throw new UnsupportedPreferenceException();
                }
                for (int i = 0; i < value.length(); i++)
                {
                    if (!Character.isLetter(value.charAt(i)))
                        throw new UnsupportedPreferenceException();
                }
            }
        });

        // Shouldn't validate this, since supported ratings could change
        preferences.put(PREF_PARENTAL_RATING.toLowerCase(), new PreferenceImpl());

        // Any user name is valid
        preferences.put(PREF_USER_NAME.toLowerCase(), new PreferenceImpl());

        // Any user address is valid
        preferences.put(PREF_USER_ADDRESS.toLowerCase(), new PreferenceImpl());

        // Make sure the email address contains the '@' character
        preferences.put(PREF_USER_EMAIL.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                if (value.indexOf('@') == -1)
                {
                    throw new UnsupportedPreferenceException();
                }
            }
        });

        // Must be a 2-character ISO 3166-1 country code
        preferences.put(PREF_COUNTRY_CODE.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                if (value.length() != 2)
                {
                    throw new UnsupportedPreferenceException();
                }
                for (int i = 0; i < value.length(); i++)
                {
                    if (!Character.isLetter(value.charAt(i)))
                    {
                        throw new UnsupportedPreferenceException();
                    }
                }
            }
        });

        // Must be an integer
        preferences.put(PREF_DEFAULT_FONT_SIZE.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                try
                {
                    Integer.parseInt(value);
                }
                catch (NumberFormatException e)
                {
                    throw new UnsupportedPreferenceException();
                }
            }
        });

        // Must be either "Primary" or "Secondary"
        preferences.put(PREF_ANALOG_AUDIO.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                if (!value.equalsIgnoreCase("Primary") && !value.equalsIgnoreCase("Secondary"))
                {
                    throw new UnsupportedPreferenceException();
                }
            }
        });

        // Must be either "On" or "Off"
        preferences.put(PREF_CLOSED_CAPTION_ON.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                if (!value.equalsIgnoreCase("On") && !value.equalsIgnoreCase("Off"))
                    throw new UnsupportedPreferenceException();
            }
        });

        // Must be one of "CC1", "CC2", "CC3", "CC4", "T1", "T2", "T3", "T4"
        preferences.put(PREF_ANALOG_CLOSED_CAPTION_SERVICE.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                if (!value.equalsIgnoreCase("CC1") && !value.equalsIgnoreCase("CC2") &&
                    !value.equalsIgnoreCase("CC3") && !value.equalsIgnoreCase("CC4") &&
                    !value.equalsIgnoreCase("T1") && !value.equalsIgnoreCase("T2") &&
                    !value.equalsIgnoreCase("T3") && !value.equalsIgnoreCase("T4"))
                {
                    throw new UnsupportedPreferenceException();
                }
            }
        });

        // Must be an integer between 1 and 63
        preferences.put(PREF_DIGITAL_CLOSED_CAPTION_SERVICE.toLowerCase(), new PreferenceImpl() {
            public void validate(String value) throws UnsupportedPreferenceException
            {
                try
                {
                    int x = Integer.parseInt(value);
                    if (x < 1 || x > 63)
                    {
                        throw new UnsupportedPreferenceException();
                    }
                }
                catch (NumberFormatException e)
                {
                    throw new UnsupportedPreferenceException();
                }
            }
        });
    }

    /**
     * These preferences are always accessible. This is used by the
     * <code>UserPreferenceManager</code> when checking whether a given
     * preference can be read.
     */
    static final String alwaysAccessible[] = new String[] {
        PREF_USER_LANGUAGE,
        PREF_PARENTAL_RATING,
        PREF_DEFAULT_FONT_SIZE,
        PREF_COUNTRY_CODE
    };
}
