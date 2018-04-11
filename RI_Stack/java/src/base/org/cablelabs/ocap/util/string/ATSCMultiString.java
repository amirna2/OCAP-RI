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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

package org.cablelabs.ocap.util.string;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import com.ibm.icu.text.UnicodeDecompressor;

/**
 * An instance of this class represents an immutable
 * <code>multiple_string_structure()</code> as defined in ATSC A/65C ¤6.10,
 * <i>Multiple String Structure</i>. The bit stream syntax for the structure is:
 * <table>
 * <tr valign=bottom>
 * <th>Syntax</th>
 * <th>No. of Bits</th>
 * <th>Format</th>
 * </tr>
 * <tr valign=top>
 * <td><code>multiple_string_structure() {</code></td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;number_strings</code></td>
 * <td align=center>8</code>
 * <td align=center>uimsbf</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;for (i=0; i< number_strings; i++) {</code></td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;ISO_639_language_code</code></td>
 * <td align=center>24</code>
 * <td align=center>uimsbf</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;number_segments</code></td>
 * <td align=center>8</code>
 * <td align=center>uimsbf</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;for (j=0; j< number_segments; j++) {</code>
 * </td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;compression_type</code></td>
 * <td align=center>8</code>
 * <td align=center>uimsbf</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mode</code></td>
 * <td align=center>8</code>
 * <td align=center>uimsbf</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;number_bytes</code></td>
 * <td align=center>8</code>
 * <td align=center>uimsbf</code>
 * </tr>
 * <tr valign=top>
 * <td>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;for (k=0; k< number_bytes; k++) {</code>
 * </td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * <tr valign=top>
 * <td>
 * <code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;compressed_string_byte [k]</code>
 * </td>
 * <td align=center>8</code>
 * <td align=center>bslbf</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}</code></td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;&nbsp;&nbsp;}</code></td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * <tr valign=top>
 * <td><code>&nbsp;&nbsp;}</code></td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * <tr valign=top>
 * <td><code>}</code></td>
 * <td align=center>&nbsp;</code>
 * <td align=center>&nbsp;</code>
 * </tr>
 * </table>
 * <dl>
 * <dt><code>number_strings</code></dt>
 * <dd>The number of strings in the following data. Could have a value of zero.</dd>
 * <dt><code>ISO_639_language_code</code></dt>
 * <dd>The 3-byte (24 bits) ISO 639-2 language code specifying the language used
 * for the i<sup>th</sup> string. Each character is encoded into 8 bits
 * according to ISO 8859-1 (ISO Latin-1).</dd>
 * <dt><code>number_segments</code></dt>
 * <dd>The number of segments making up the i<sup>th</sup> string in the
 * following data. A specific mode is assigned for each segment.</dd>
 * <dt><code>compression_type</code></dt>
 * <dd>The compression type for the j<sup>th</sup> segment. Compression types
 * 0x01 (Huffman Program Title) and 0x02 (Huffman Program Description) are
 * restricted for use with text mode 0x00 (Unicode Code Range 0x0000 - 0x00FF).
 * Compression type 0x00 (no compression) is used for all other modes.</dd>
 * <dt><code>mode</code></dt>
 * <dd>The mode to be used to interpret the segment contents as encoded
 * character (textual) data (see
 * {@link #decodeString(ByteArrayInputStream, StringBuffer)}. In case a mode is
 * not supported, the string entry that includes that mode within the multiple
 * string structure shall be ignored.</dd>
 * <dt><code>number_bytes</code></dt>
 * <dd>The number of bytes comprising the textual data for the j<sup>th</sup>
 * segment.</dd>
 * <dt><code>compressed_string_byte</code></dt>
 * <dd>The encoded, and possibly compressed, textual data for the j<sup>th</sup>
 * segment.</dd>
 * </dl>
 * Note: decoded text strings may have zero-length. Although more typical
 * classes provide a bidirectional API for encoding and decoding field values
 * onto/from a byte array or stream, there is currently no requirement for a
 * set-top box to encode a multi-string structure onto either output.</p>
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class ATSCMultiString implements Serializable
{
    // Class Constants

    /**
     * The default ISO 639-2 language code for constructing a single-language
     * instance of this class, and for retrieving a default string when a
     * preferred language is not specified or the preferred language is not
     * included in the map.
     */
    public static final String DEFAULT_LANGUAGE_CODE = "eng".toLowerCase();

    public static final int MSS_COMPRESSION_TYPE_NONE = 0x00;

    public static final int MSS_COMPRESSION_TYPE_HUFFMAN_TITLE = 0x01;

    public static final int MSS_COMPRESSION_TYPE_HUFFMAN_DESC = 0x02;

    public static final int MSS_MODE_UNICODE_RANGE_0x0000_0x00FF = 0x00;

    public static final int MSS_MODE_UNICODE_STANDARD_COMPRESSION = 0x3E;

    public static final int MSS_MODE_UNICODE_UTF16 = 0x3F;

    /**
     * If <code>true</code>, exclude surrogate pairs when assembling a UTF-16
     * string. Otherwise, include surrogate pairs in the assembled UTF-16
     * string.
     */
    private static final boolean SKIP_SURROGATE_PAIRS = false;

    /**
     * The Eclipse-generated serialized version unique ID for this class.
     */
    private static final long serialVersionUID = -8623837003498121891L;

    // Instance Fields

    /**
     * @serial The hash map containing the ISO 639-2 three-character language
     *         code to string mapping. The map may be empty. A hash map is used
     *         since no synchronization is required (once created, it doesn't
     *         change). Hash maps are also Serializable so no serialization code
     *         is needed.
     */
    private HashMap m_LangCodeStringMap;

    // Constructors

    /**
     * Constructs a new instance of the receiver representing the empty set of
     * language code/string mappings. Also required for serialization.
     */
    public ATSCMultiString()
    {
        this.m_LangCodeStringMap = new HashMap(0, 1.0f);
    }

    /**
     * Constructs a new instance of the receiver from the given byte array. If a
     * decoding error occurs within one of the strings, that string is skipped
     * per ATSC A/65C ¤6.10. The instance can have a size of 0 (no strings).
     * 
     * @param bytes
     *            the byte array containing the ATSC multiple string structure
     *            to be parsed
     * @throws IllegalArgumentException
     *             if <code>bytes</code> is null
     * @throws ArrayIndexOutOfBoundsException
     *             if the end of the byte array is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             multi-string structure were incorrect and caused a buffer
     *             overrun.
     */
    public ATSCMultiString(final byte[] bytes)
    {
        if (null == bytes)
        {
            throw new IllegalArgumentException("Byte array must not be null");
        }

        try
        {
            decodeStrings(new ByteArrayInputStream(bytes));
        }
        catch (EOFException e)
        {
            throw new ArrayIndexOutOfBoundsException(e.getMessage());
        }
    }

    /**
     * Constructs a new instance of the receiver from the given byte stream. If
     * a decoding error occurs within one of the strings, that string is skipped
     * per ATSC A/65C ¤6.10. The instance can have a size of 0 (no strings).
     * 
     * @param stream
     *            the byte stream containing the ATSC multiple string structure
     *            to be parsed
     * @throws IllegalArgumentException
     *             if <code>stream</code> is null
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             multi-string structure were incorrect and caused a buffer
     *             overrun.
     */
    public ATSCMultiString(final ByteArrayInputStream stream) throws EOFException
    {
        if (null == stream)
        {
            throw new IllegalArgumentException("Byte array input stream must not be null");
        }

        decodeStrings(stream);
    }

    /**
     * Constructs a new single-entry instance of the receiver from given string
     * and using the default language code. This is a convenience constructor
     * used when there's only one possible string that's always represented in
     * the default language.
     * 
     * @param string
     *            the string value corresponding to the default language code
     * @throws IllegalArgumentException
     *             if <code>string</code> is null
     */
    public ATSCMultiString(final String string)
    {
        this(new String[] { ATSCMultiString.DEFAULT_LANGUAGE_CODE }, new String[] { string });
    }

    /**
     * Constructs a new instance of the receiver from the given language codes
     * and corresponding strings. The instance can have a size of 0 (no
     * strings). A language code may have a corresponding zero-length string.
     * 
     * @param languageCodes
     *            the array of three-character ISO 639-2 language codes
     *            corresponding to the array of <code>strings</code>. If the
     *            language code appears more than once in the array, the last
     *            occurrence is retained.
     * @param strings
     *            the array of string values having a 1:1 correspondence to the
     *            array of language codes
     * @throws IllegalArgumentException
     *             if <code>languages</code> or <code>strings</code> is null, or
     *             they have different lengths, or an array element is null, or
     *             the language code is not three characters in length
     */
    public ATSCMultiString(final String[] languageCodes, final String[] strings)
    {
        if (null == languageCodes || null == strings || languageCodes.length != strings.length)
        {
            throw new IllegalArgumentException("Language code and string arrays must be non-null and have equal length");
        }

        int numberStrings = languageCodes.length;
        this.m_LangCodeStringMap = new HashMap(numberStrings, 1.0f);

        for (int i = 0; i < numberStrings; ++i)
        {
            if (null == languageCodes[i] || 3 != languageCodes[i].length()) // ISO
                                                                            // 639-2
                                                                            // codes
                                                                            // are
                                                                            // 3
                                                                            // characters
            {
                throw new IllegalArgumentException("Language code[" + i + "] must be a three character ISO 639-2 code");
            }
            else if (null == strings[i]) // corresponding string value must not
                                         // be null
            {
                throw new IllegalArgumentException("String value[" + i + "] must not be null");
            }
            else
            {
                this.m_LangCodeStringMap.put(languageCodes[i].toLowerCase(), strings[i]);
            }
        }
    }

    // Instance Methods

    /**
     * Returns the string value corresponding to the default ISO 639-2 language
     * code.
     * 
     * @return the string value corresponding to the default language code, or
     *         <code>null</code> if the default language code is not found or
     *         the instance contains no strings.
     */
    public String getDefaultValue()
    {
        return getValue(ATSCMultiString.DEFAULT_LANGUAGE_CODE);
    }

    /**
     * Returns an array of the ISO 639-2 three-character language codes
     * contained in this instance.
     * 
     * @return a <code>String</code> array of language codes. The array may be
     *         empty if there are no strings in the set.
     */
    public String[] getLanguageCodes()
    {
        return (String[]) this.m_LangCodeStringMap.keySet().toArray(new String[this.size()]);
    }

    /**
     * Returns the string value corresponding to the given ISO 639-2 language
     * code. If the language code is not found, or is null or has a zero-length,
     * the string value corresponding to the default language code is returned.
     * 
     * @param languageCode
     *            the specific language code used to select the string to return
     * @return the string value corresponding to the given language code, or
     *         <code>null</code> if the given language code is not found or the
     *         default language is not found, or the instance contains no
     *         strings.
     */
    public String getValue(final String languageCode)
    {
        return getValue(new String[] { languageCode });
    }

    /**
     * Returns the string value corresponding to the most preferred language
     * code listed in the given array. If none of the languages in the array are
     * found, the string value corresponding to the default language code is
     * returned. The default language code is also used if the given array is
     * null or has a zero-length.
     * 
     * @param languageCodes
     *            the array of preferred languages from which to select the
     *            string to return. Each element must be a three-character ISO
     *            639-2 language code. The array is assumed to be ordered in
     *            descending order of preference. Null elements are skipped.
     * @return the string value corresponding to the most preferred language
     *         code, or <code>null</code> if a matching language is not found or
     *         the default language is not found, or the instance contains no
     *         strings.
     */
    public String getValue(final String[] languageCodes)
    {
        String string = null;

        if (null != languageCodes)
        {
            for (int i = 0; null == string && i < languageCodes.length; ++i)
            {
                if (null != languageCodes[i] && 3 == languageCodes[i].length())
                {
                    string = (String) this.m_LangCodeStringMap.get(languageCodes[i].toLowerCase());
                }
            }
        }

        if (null == string)
        {
            string = (String) this.m_LangCodeStringMap.get(ATSCMultiString.DEFAULT_LANGUAGE_CODE);
        }

        return string;
    }

    /**
     * Returns an array of the language-specific string values contained in this
     * instance.
     * 
     * @return a <code>String</code> array of language-specific string values.
     *         The array may be empty if there are no strings in the set.
     */
    public String[] getValues()
    {
        return (String[]) this.m_LangCodeStringMap.values().toArray(new String[this.size()]);
    }

    /**
     * Returns the number of strings in the receiver.
     * 
     * @return the number of strings
     */
    public int size()
    {
        return this.m_LangCodeStringMap.size();
    }

    /**
     * Returns a string representation of the receiver.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("ATSCMultiString");
        buf.append(": map=").append(this.m_LangCodeStringMap);
        return buf.toString();
    }

    /**
     * Decodes a string from one or more segments contained within an ATSC A/65C
     * multiple string structure. The remaining segments of the string are
     * skipped over if any segment contains an invalid <code>mode</code> or an
     * invalid <code>compression_type</code> for the <code>mode</code>.
     * Supported modes include:
     * <dl>
     * <dt>0x00</dt>
     * <dd>Unicode Code Range 0x0000 Ð 0x00FF, typically single byte ASCII
     * characters that may be compressed.</dd>
     * <dt>0x01-0x06, 0x09-0x0E, 0x10, 0x20-0x27, 0x30-0x33</dt>
     * <dd>A simple form of run-length encoding for a sequence of 16-bit Unicode
     * code values where the segmentÕs bytes represent the least significant
     * 8-bits of a sequence of 16-bit Unicode code values and the most
     * significant 8-bits of these code values is implied by the mode value
     * itself.</dd>
     * <dt>0x3E</dt>
     * <dd>Standard Compression Scheme for Unicode (SCSU).</dd>
     * <dt>0x3F</dt>
     * <dd>UTF-16 representation of Unicode character data. Surrogate pairs are
     * supported.</dd>
     * </dl>
     * 
     * @param stream
     *            the byte stream containing the multiple-segment string to
     *            decode
     * @return <code>true</code> if the string was successfully decoded;
     *         otherwise <code>false</code>
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length fields in the
     *             multiple-string structure were incorrect and caused a buffer
     *             overrun.
     */
    private boolean decodeString(final ByteArrayInputStream stream, final StringBuffer buffer) throws EOFException
    {
        boolean skipRemainingSegments = false;

        int numberSegments = getByte(stream);

        for (int j = 0; j < numberSegments; ++j)
        {
            int compressionType = getByte(stream);
            int mode = getByte(stream);
            int numberBytes = getByte(stream);

            if (skipRemainingSegments)
            { // had encountered a segment decode error
                stream.skip(numberBytes);
                continue;
            }
            else if (mode != ATSCMultiString.MSS_MODE_UNICODE_RANGE_0x0000_0x00FF
                    && compressionType != ATSCMultiString.MSS_COMPRESSION_TYPE_NONE)
            { // compression types 0x01 and 0x02 restricted to text mode 0x00
              // only per ATSC A/65C ¤6.10
                stream.skip(numberBytes);
                skipRemainingSegments = true;
                continue;
            }
            else if (mode == ATSCMultiString.MSS_MODE_UNICODE_RANGE_0x0000_0x00FF) // mode
                                                                                   // =
                                                                                   // 0x00
            {
                if (compressionType == ATSCMultiString.MSS_COMPRESSION_TYPE_NONE) // 1-byte
                                                                                  // ASCII
                                                                                  // characters
                {
                    buffer.append(new String(getBytes(stream, numberBytes)));
                }
                else if ((compressionType == ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_TITLE)
                        || (compressionType == ATSCMultiString.MSS_COMPRESSION_TYPE_HUFFMAN_DESC))
                {
                    buffer.append(ATSCHuffmanDecoder.decodeHuffmanString(stream, compressionType));
                }
                else
                { // illegal compression type
                    stream.skip(numberBytes);
                    skipRemainingSegments = true;
                    continue;
                }
            }
            else if ((mode >= 0x01 && mode <= 0x06) || (mode >= 0x09 && mode <= 0x0E) || (mode >= 0x20 && mode <= 0x27)
                    || (mode >= 0x30 && mode <= 0x33) || (mode == 0x10))
            { // the text is not compressed and the upper 8 bits of the
              // character value is the mode value
                int upperByte = mode << 8;
                for (int k = 0; k < numberBytes; ++k)
                {
                    buffer.append((char) (upperByte | getByte(stream)));
                }
            }
            else if (mode == ATSCMultiString.MSS_MODE_UNICODE_STANDARD_COMPRESSION) // mode
                                                                                    // ==
                                                                                    // 0x3E
            { // Standard Compression Scheme for Unicode (SCSU)
                buffer.append(UnicodeDecompressor.decompress(getBytes(stream, numberBytes)));
            }
            else if (mode == ATSCMultiString.MSS_MODE_UNICODE_UTF16) // mode ==
                                                                     // 0x3F
            { // check for byte-order mark
                boolean bigEndian = false;
                int number = (getByte(stream) << 8) | getByte(stream);

                if (number == 0xfeff)
                {
                    bigEndian = true;
                }
                else if (number == 0xfffe)
                {
                    bigEndian = false;
                }
                else
                { // missing byte-order mark
                    stream.skip(numberBytes - 2);
                    skipRemainingSegments = true;
                    continue;
                }

                // assemble UTF-16 string
                for (numberBytes -= 2; numberBytes > 1; numberBytes -= 2)
                {
                    int oneByte = getByte(stream);
                    int twoByte = getByte(stream);
                    number = (bigEndian) ? (oneByte << 8) | twoByte : (twoByte << 8) | oneByte;

                    if (ATSCMultiString.SKIP_SURROGATE_PAIRS && number >= 0xd800)
                    {
                        numberBytes -= 2;
                        stream.skip(2); // skip the surrogate pair
                        continue;
                    }

                    buffer.append((char) number);
                }

                // handle pad byte if it exists (numberBytes will be 0 or 1 at
                // this point)
                stream.skip(numberBytes);
            }
            else
            { // illegal mode
                stream.skip(numberBytes);
                skipRemainingSegments = true;
            }
        }

        return !skipRemainingSegments;
    }

    /**
     * Decodes one or more strings contained within an ATSC A/65C multiple
     * string structure.
     * 
     * @param stream
     *            the byte stream containing the ATSC multiple string structure
     *            to be parsed
     * @throws IllegalStateException
     *             if the ISO-8859-1 is not supported by the JVM (however it's
     *             required in all compliant JVM ports)
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             multi-string structure were incorrect and caused a buffer
     *             overrun.
     */
    private void decodeStrings(final ByteArrayInputStream stream) throws EOFException, IllegalStateException
    {
        int numberStrings = getByte(stream);
        this.m_LangCodeStringMap = new HashMap(numberStrings, 1.0f);

        try
        {
            for (int i = 0; i < numberStrings; ++i)
            {
                String languageCode = new String(getBytes(stream, 3), "ISO-8859-1");
                StringBuffer buffer = new StringBuffer();
                if (decodeString(stream, buffer))
                {
                    this.m_LangCodeStringMap.put(languageCode.toLowerCase(), buffer.toString());
                }
            }
        }
        catch (UnsupportedEncodingException e) // should not occur in a
                                               // compliant JVM port
        {
            throw new IllegalStateException("ISO-8859-1 is not a supported charset name");
        }
    }

    /**
     * Retrieves an unsigned 8-bit value from the byte stream.
     * 
     * @param stream
     *            the byte stream containing the byte to extract
     * @return the next 8-bit value as an <code>int</code> to maintain the
     *         most-significant bit of the byte as a value and not a sign bit
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             multi-string structure were incorrect and caused a buffer
     *             overrun.
     */
    private final int getByte(ByteArrayInputStream stream) throws EOFException
    {
        int value = stream.read();

        if (value < 0)
        {
            throw new EOFException("Premature end of stream");
        }

        return value;
    }

    /**
     * Retrieves a byte array of the given length from the byte stream.
     * 
     * @param stream
     *            the byte stream containing the byte array to extract
     * @param length
     *            the number of bytes to decode from the byte stream
     * @return a byte array of the given length, or consisting of what's left in
     *         the byte stream
     * @throws EOFException
     *             if the end of the byte stream is prematurely reached. This
     *             would occur if one of the length or count fields in the
     *             multi-string structure were incorrect and caused a buffer
     *             overrun.
     */
    private final byte[] getBytes(ByteArrayInputStream stream, final int length) throws EOFException
    {
        byte[] bytes = new byte[Math.min(length, stream.available())];

        if (stream.read(bytes, 0, bytes.length) < 0)
        {
            throw new EOFException("Premature end of stream");
        }

        return bytes;
    }
}
