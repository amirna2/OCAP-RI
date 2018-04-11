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

import java.text.CharacterIterator;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses an input string into a series of start tags, end tags, and blocks of
 * text, according to a subset of the HTML5 tokenization rules.
 * 
 * @author Spencer Schumann
 * 
 */
public class Parser
{
    private final String input;
    private final int length;
    private final TagHandler handler;
    private int pos = 0;
    private StringBuffer text = new StringBuffer();

    private static final char EOF = CharacterIterator.DONE;
    private static final HashMap charEntities = new HashMap();

    /**
     * Parses an html string, returning a Document object.
     * 
     * @param html
     *            input string
     * @return document
     */
    public static Document parse(String html)
    {
        Document.Builder document = Document.builder();
        parse(html, document);
        return document.build();
    }

    /**
     * Parse an html string, handling the tags and text with the specified
     * handler.
     * 
     * @param html
     *            string to parse
     * @param handler
     *            handler for tags and text
     */
    public static void parse(String html, TagHandler handler)
    {
        Parser parser = new Parser(html, handler);
        parser.read();
    }

    /**
     * Tests whether a given character is whitespace according to the HTML5
     * whitespace rules. In contrast to Character.isWhitespace, this method is
     * not dependent upon the current locale.
     * 
     * @param c
     *            character to test
     * @return true if it is whitespace, false otherwise
     */
    public static boolean isWhitespace(char c)
    {
        switch (c)
        {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
                return true;

            default:
                return false;
        }
    }

    /**
     * Private constructor to prevent direct instantiation and sub-classing.
     * 
     * @param input
     *            string to parse
     * @param handler
     *            handler for tags and text
     */
    private Parser(String input, TagHandler handler)
    {
        this.input = input;
        this.length = input.length();
        this.handler = handler;
    }

    /**
     * Read all of the characters in the input string.
     * 
     */
    private void read()
    {
        while (!atEnd())
        {
            if (current() == '<')
            {
                consume();
                readTag();
            }
            else
            {
                readText();
            }
        }

        emitText();
    }

    /**
     * Remove '\r' characters, converting them to newlines as specified by the
     * HTML5 preprocessing rules.
     * 
     * @param str
     *            string to convert
     * @return string with '\r' characters removed
     */
    private String convertCarriageReturns(String str)
    {
        int index = str.indexOf('\r');

        if (index < 0)
        {
            // No CR to convert
            return str;
        }

        StringBuffer buffer = new StringBuffer(str);

        while ((index = buffer.indexOf("\r", index)) >= 0)
        {
            int length = buffer.length();

            // Convert '\r' to '\n'
            buffer.setCharAt(index, '\n');

            if (index < (length - 1))
            {
                if (buffer.charAt(index + 1) == '\n')
                {
                    // "\r\n" - drop the '\n'
                    buffer.replace(index + 1, index + 2, "");
                }
            }
        }

        return buffer.toString();
    }

    /**
     * Send the current block of text to the handler's text callback method
     * 
     */
    private void emitText()
    {
        if (text.length() > 0)
        {
            String str = text.toString();
            str = convertCarriageReturns(str);
            handler.text(str);
            text = new StringBuffer();
        }
    }

/**
     * Read a block of text, which extends up to the next '<' character. 
     * Translate any character entities that are encountered.
     * 
     */
    private void readText()
    {
        while (!atEnd())
        {
            char c = current();
            if (c == '&')
            {
                readCharacterEntity(text);
            }
            else if (c == '<')
            {
                break;
            }
            else
            {
                if (isValidCharacter(c))
                {
                    text.append(c);
                }
                consume();
            }
        }
    }

    /**
     * Read a character entity from the input string and add the translated
     * character to the given string buffer.
     * 
     * @param buffer
     *            string buffer to receive translated character
     */
    private void readCharacterEntity(StringBuffer buffer)
    {
        int semiIndex = input.indexOf(';', pos);
        if (semiIndex > (pos + 1))
        {
            String entity = input.substring(pos + 1, semiIndex);

            // There's a possibly valid character entity reference here.
            if (entity.charAt(0) == '#')
            {
                // Numeric entity reference
                try
                {
                    char c = (char) Integer.parseInt(entity.substring(1));
                    if (isValidCharacter(c) && c != '\r')
                    {
                        buffer.append(c);
                        pos = semiIndex + 1;
                        return;
                    }
                }
                catch (NumberFormatException e)
                {
                }
            }
            else
            {
                // Check for named character entity
                Character c = (Character) charEntities.get(entity);
                if (null != c)
                {
                    buffer.append(c.charValue());
                    pos = semiIndex + 1;
                    return;
                }
            }
        }

        buffer.append('&');
        consume();
    }

    /**
     * Test for valid characters as defined in Table A-2 of
     * OC-SP-CCIF2.0-I20-091211.
     * 
     * @param c
     *            character to test
     * @return true if valid, false otherwise
     */
    private static boolean isValidCharacter(char c)
    {
        return (32 <= c && c <= 126) || (160 <= c && c <= 255)
                || "\t\r\n\f".indexOf(c) >= 0;
    }

    /**
     * Test whether a given character is a letter. In contrast to
     * Character.isLetter, this method only allows upper case and lower case
     * letters A through Z.
     * 
     * @param c
     *            character to test
     * @return true if it is a letter, false otherwise
     */
    private static boolean isLetter(char c)
    {
        return ('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z');
    }

    /**
     * Convert a string to lower-case. In contrast with String.toLowerCase, this
     * method uses the HTML5 rules for converting case and is not locale
     * dependent.
     * 
     * @param str
     *            string to convert
     * @return string with upper case letters converted to lower case
     */
    public static String toLowerCase(String str)
    {
        // Only create the StringBuffer if necessary.
        // If there are no upper case characters, return the same string.
        int i;
        int length = str.length();
        StringBuffer buffer = null;

        for (i = 0; i < length; i++)
        {
            char c = str.charAt(i);
            if ('A' <= c && c <= 'Z')
            {
                buffer = new StringBuffer(length);
                buffer.append(str.substring(0, i));
                break;
            }
        }

        if (buffer != null)
        {
            for (; i < length; i++)
            {
                char c = str.charAt(i);
                if ('A' <= c && c <= 'Z')
                {
                    c += 0x20;
                }
                buffer.append(c);
            }
            return buffer.toString();
        }
        else
        {
            return str;
        }
    }

    /**
     * Reads a single tag from the input stream.
     * 
     * @return tag
     */
    private void readTag()
    {
        char c = current();
        switch (c)
        {
            case '/':
                consume();
                readEndTag();
                break;

            case '!':
                consume();
                readMarkupDeclaration();
                break;

            case '?':
                consume();
                readBogusComment();
                break;

            case EOF:
                text.append('<');
                break;

            default:
                if (isLetter(c))
                {
                    readStartTag();
                }
                else
                {
                    text.append('<');
                }
                break;
        }
    }

    /**
     * Reads a markup declaration, which is a tag that starts with "<!".
     * 
     */
    private void readMarkupDeclaration()
    {
        if (remainder().startsWith("--"))
        {
            consume(2);
            readComment();
        }
        else
        {
            // Note: <!DOCTYPE...> can be treated as a bogus comment.
            readBogusComment();
        }
    }

    /**
     * Read a comment tag as defined by the HTML5 processing rules.
     * 
     */
    private void readComment()
    {
        String rest = remainder();
        if (rest.startsWith(">"))
        {
            consume();
            return;
        }

        if (rest.startsWith("->"))
        {
            consume(2);
            return;
        }

        int index;
        while ((index = input.indexOf("--", pos)) >= 0)
        {
            // In case the comment doesn't end here, go back to second '-' of
            // the "--" that was found
            // (to handle the case of "---")
            int resumePos = index + 1;

            // Skip "--"
            pos = index + 2;

            if (current() == '!')
            {
                consume();
            }
            else
            {
                consumeWhitespace();
            }

            if (current() == '>')
            {
                // End of comment.
                consume();
                return;
            }
            else if (atEnd())
            {
                break;
            }
            else
            {
                pos = resumePos;
            }
        }

        // Unterminated comment.
        pos = length;
    }

    /**
     * Read a bogus comment as defined by the HTML5 processing rules.
     * 
     */
    private void readBogusComment()
    {
        // Skip to the next '>'.
        int index = input.indexOf('>', pos);
        if (index < 0)
        {
            pos = length;
        }
        else
        {
            pos = index + 1;
        }
    }

    /**
     * Read a start tag.
     * 
     */
    private void readStartTag()
    {
        String name = readTagName();
        Map attributes = readAttributes();

        if (current() == '>')
        {
            consume();
            emitText();
            handler.startTag(name, attributes);
        }
    }

    /**
     * Read an end tag.
     * 
     */
    private void readEndTag()
    {
        char c = current();
        if (isLetter(c))
        {
            String name = readTagName();

            // Strip off any attributes that were erroneously added to the end
            // tag
            readAttributes();

            if (!atEnd() && current() == '>')
            {
                consume();
                emitText();
                handler.endTag(name);
            }
        }
        else if ('>' == c)
        {
            // Parse error: encountered "</>". Nothing is emitted in this case.
            consume();
        }
        else if (c == EOF)
        {
            text.append("</");
        }
        else
        {
            readBogusComment();
        }
    }

    /**
     * Read a tag name.
     * 
     * @return the tag's name
     */
    private String readTagName()
    {
        StringBuffer buffer = new StringBuffer();
        while (!atEnd())
        {
            char c = current();
            if (isWhitespace(c) || '/' == c || '>' == c)
            {
                break;
            }
            else
            {
                if (isValidCharacter(c))
                {
                    buffer.append(c);
                }
                consume();
            }
        }
        return toLowerCase(buffer.toString());
    }

    /**
     * Read tag attributes.
     * 
     * @return map of attribute key-value pairs.
     */
    private Map readAttributes()
    {
        // The total number of attributes is expected to be
        // much less than the default HashMap size of 16.
        // TODO: I'd prefer to use Collections.EMPTY_MAP by default...
        Map attributes = new HashMap(4);

        while (true)
        {
            consumeWhitespace();
            if (atEnd())
            {
                return attributes;
            }

            switch (current())
            {
                case '/':
                    // Possible XML-style self-closing tag. This syntax is
                    // accepted, but does not have any effect.
                    consume();

                    if (!atEnd() && current() == '>')
                    {
                        return attributes;
                    }
                    break;

                case '>':
                    // End of tag.
                    return attributes;

                default:
                    readAttribute(attributes);
                    break;
            }
        }
    }

    /**
     * Read a single attribute and add it to the attribute map. If the attribute
     * already exists in the map, the existing value is retained.
     * 
     * @param attributes
     *            map of attributes being read.
     */
    private void readAttribute(Map attributes)
    {
        String name = readAttributeName();
        String value = "";

        consumeWhitespace();
        if (current() == '=')
        {
            consume();
            consumeWhitespace();
            value = readAttributeValue();
        }

        // The first appearance of a given attribute takes
        // precedence.
        if (!attributes.containsKey(name))
        {
            attributes.put(name, value);
        }
    }

    /**
     * Read an attribute name.
     * 
     * @return the attribute name
     */
    private String readAttributeName()
    {
        StringBuffer buffer = new StringBuffer();

        int startPos = pos;
        loop: while (!atEnd())
        {
            char c = current();
            switch (c)
            {
                case '/':
                case '>':
                    break loop;

                default:
                    // '=' is allowed as the first character of an attribute
                    // name, but it generates a parse error.
                    if (c == '=' && pos != startPos)
                    {
                        break loop;
                    }
                    else if (isWhitespace(c))
                    {
                        break loop;
                    }
                    else if (isValidCharacter(c))
                    {
                        buffer.append(c);
                    }
                    consume();
                    break;
            }
        }
        return toLowerCase(buffer.toString());
    }

    /**
     * Read an attribute value.
     * 
     * @return attribute value
     */
    private String readAttributeValue()
    {
        char c = current();
        switch (c)
        {
            case '>':
                return "";

            case '\'':
            case '\"':
                consume();
                return readQuotedValue(c);

            case EOF:
                return "";

            default:
                return readUnquotedValue();
        }
    }

    /**
     * Read a quoted attribute value.
     * 
     * @param quote
     *            quotation mark that terminates this quotation.
     * @return attribute value
     */
    private String readQuotedValue(char quote)
    {
        StringBuffer buffer = new StringBuffer();

        while (!atEnd())
        {
            char c = current();
            if (c == quote)
            {
                consume();
                break;
            }
            else if (c == '&')
            {
                readCharacterEntity(buffer);
            }
            else
            {
                if (isValidCharacter(c))
                {
                    buffer.append(c);
                }
                consume();
            }
        }

        String str = buffer.toString();
        return convertCarriageReturns(str);
    }

    /**
     * Read an unquoted attribute value.
     * 
     * @return attribute value
     */
    private String readUnquotedValue()
    {
        StringBuffer buffer = new StringBuffer();

        loop: while (!atEnd())
        {
            char c = current();
            switch (c)
            {
                case '&':
                    readCharacterEntity(buffer);
                    break;

                case '>':
                    break loop;

                default:
                    if (isWhitespace(c))
                    {
                        break loop;
                    }
                    else
                    {
                        if (isValidCharacter(c))
                        {
                            buffer.append(c);
                        }
                        consume();
                    }
                    break;
            }
        }

        return buffer.toString();
    }

    /**
     * Consume a single character. The current input position is advanced by
     * one.
     * 
     */
    private void consume()
    {
        pos++;
    }

    /**
     * Consume count input characters. The current input position is advanced by
     * count.
     * 
     * @param count
     */
    private void consume(int count)
    {
        pos += count;
    }

    /**
     * Get the current input character. Returns EOF if no more characters are
     * available.
     * 
     * @return current character
     */
    private char current()
    {
        return atEnd() ? EOF : input.charAt(pos);
    }

    /**
     * Return the substring of the input from the current position to the end of
     * the string.
     * 
     * @return substring of input
     */
    private String remainder()
    {
        return atEnd() ? "" : input.substring(pos);
    }

    /**
     * Determine whether the entire input string has been read.
     * 
     * @return true if at end, false otherwise
     */
    private boolean atEnd()
    {
        return pos >= length;
    }

    /**
     * Skip past all consecutive whitespace characters starting at the current
     * input position.
     * 
     */
    private void consumeWhitespace()
    {
        while (!atEnd())
        {
            char r = current();
            if (isWhitespace(r))
            {
                consume();
            }
            else
            {
                break;
            }
        }
    }

    /**
     * Convenience method for setting up the map of named character entities.
     * 
     * @param name
     *            name of character
     * @param charCode
     *            character code
     */
    private static void addCharEntity(String name, int charCode)
    {
        charEntities.put(name, new Character((char) charCode));
    }

    static
    {
        // This list of character entities is from Table A-2 of
        // OC-SP-CCIF2.0-I20-091211.
        addCharEntity("quot", 34);
        addCharEntity("amp", 38);
        addCharEntity("lt", 60);
        addCharEntity("gt", 62);
        addCharEntity("nbsp", 160);
        addCharEntity("iexcl", 161);
        addCharEntity("cent", 162);
        addCharEntity("pound", 163);
        addCharEntity("curren", 164);
        addCharEntity("yen", 165);
        addCharEntity("brvbar", 166);
        addCharEntity("sect", 167);
        addCharEntity("uml", 168);
        addCharEntity("copy", 169);
        addCharEntity("ordf", 170);
        addCharEntity("laquo", 171);
        addCharEntity("not", 172);
        addCharEntity("shy", 173);
        addCharEntity("reg", 174);
        addCharEntity("macr", 175);
        addCharEntity("deg", 176);
        addCharEntity("plusmn", 177);
        addCharEntity("sup2", 178);
        addCharEntity("sup3", 179);
        addCharEntity("acute", 180);
        addCharEntity("micro", 181);
        addCharEntity("para", 182);
        addCharEntity("middot", 183);
        addCharEntity("cedil", 184);
        addCharEntity("sup1", 185);
        addCharEntity("ordm", 186);
        addCharEntity("raquo", 187);
        addCharEntity("frac14", 188);
        addCharEntity("frac12", 189);
        addCharEntity("frac34", 190);
        addCharEntity("iquest", 191);
        addCharEntity("Agrave", 192);
        addCharEntity("Aacute", 193);
        addCharEntity("Acirc", 194);
        addCharEntity("Atilde", 195);
        addCharEntity("Auml", 196);
        addCharEntity("Aring", 197);
        addCharEntity("AElig", 198);
        addCharEntity("Ccedil", 199);
        addCharEntity("Egrave", 200);
        addCharEntity("Eacute", 201);
        addCharEntity("Ecirc", 202);
        addCharEntity("Euml", 203);
        addCharEntity("Igrave", 204);
        addCharEntity("Iacute", 205);
        addCharEntity("Icirc", 206);
        addCharEntity("Iuml", 207);
        addCharEntity("ETH", 208);
        addCharEntity("Ntilde", 209);
        addCharEntity("Ograve", 210);
        addCharEntity("Oacute", 211);
        addCharEntity("Ocirc", 212);
        addCharEntity("Otilde", 213);
        addCharEntity("Ouml", 214);
        addCharEntity("times", 215);
        addCharEntity("Oslash", 216);
        addCharEntity("Ugrave", 217);
        addCharEntity("Uacute", 218);
        addCharEntity("Ucirc", 219);
        addCharEntity("Uuml", 220);
        addCharEntity("Yacute", 221);
        addCharEntity("THORN", 222);
        addCharEntity("szlig", 223);
        addCharEntity("agrave", 224);
        addCharEntity("aacute", 225);
        addCharEntity("acirc", 226);
        addCharEntity("atilde", 227);
        addCharEntity("auml", 228);
        addCharEntity("aring", 229);
        addCharEntity("aelig", 230);
        addCharEntity("ccedil", 231);
        addCharEntity("egrave", 232);
        addCharEntity("eacute", 233);
        addCharEntity("ecirc", 234);
        addCharEntity("euml", 235);
        addCharEntity("igrave", 236);
        addCharEntity("iacute", 237);
        addCharEntity("icirc", 238);
        addCharEntity("iuml", 239);
        addCharEntity("eth", 240);
        addCharEntity("ntilde", 241);
        addCharEntity("ograve", 242);
        addCharEntity("oacute", 243);
        addCharEntity("ocirc", 244);
        addCharEntity("otilde", 245);
        addCharEntity("ouml", 246);
        addCharEntity("divide", 247);
        addCharEntity("oslash", 248);
        addCharEntity("ugrave", 249);
        addCharEntity("uacute", 250);
        addCharEntity("ucirc", 251);
        addCharEntity("uuml", 252);
        addCharEntity("yacute", 253);
        addCharEntity("thorn", 254);
        addCharEntity("yuml", 255);
    }
}
