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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Random HTML generator. Tests to ensure that ValidatingParser and Parser both
 * produce the same sequence of tags and text for any input string. Since each
 * parser is implemented with a completely different approach, this test is able
 * to find bugs in each parser.
 * 
 * @author Spencer Schumann
 * 
 */
public class Generator
{
    private Random random = new Random();
    private static boolean verbose = false;

    /**
     * Interface for random character generators.
     * 
     */
    private interface CharGen
    {
        /**
         * Generate a random string.
         * 
         * @return
         */
        public String generate();

        /**
         * Test whether the generator can generate the given character.
         * 
         * @param c
         *            character to test
         * @return true if generator includes c, false otherwise.
         */
        public boolean includes(char c);
    }

    /**
     * Generator that always returns the same character.
     * 
     */
    private class Single implements CharGen
    {
        private final char c;

        /**
         * Constructor for single character generator.
         * 
         * @param c
         *            character to generate
         */
        public Single(char c)
        {
            this.c = c;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#generate()
         */
        public String generate()
        {
            return Character.toString(c);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#includes
         * (char)
         */
        public boolean includes(char c)
        {
            return this.c == c;
        }
    }

    /**
     * Generator that always returns the same string.
     * 
     */
    private class StringGen implements CharGen
    {
        private final String str;

        /**
         * Constructor for string generator.
         * 
         * @param str
         *            string to generate
         */
        StringGen(String str)
        {
            this.str = str;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#generate()
         */
        public String generate()
        {
            return str;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#includes
         * (char)
         */
        public boolean includes(char c)
        {
            return str.indexOf(c) >= 0;
        }
    }

    /**
     * Generator with uniform, random selection from a range of characters
     * 
     */
    private class Range implements CharGen
    {
        private final char first;
        private final char last;
        private final int count;

        /**
         * Constructor for range generator.
         * 
         * @param first
         *            first character in range (inclusive)
         * @param last
         *            last character in range (inclusive)
         */
        public Range(char first, char last)
        {
            if (first > last)
            {
                char temp = first;
                first = last;
                last = temp;
            }
            this.first = first;
            this.last = last;
            this.count = last - first + 1;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#generate()
         */
        public String generate()
        {
            char c = (char) (this.first + random.nextInt(count));
            return Character.toString(c);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#includes
         * (char)
         */
        public boolean includes(char c)
        {
            return first <= c && c <= last;
        }
    }

    /**
     * Generator with uniform, random selection from a set of characters
     * 
     */
    private class CharSet implements CharGen
    {
        private final char[] set;

        /**
         * Constructor for set generator
         * 
         * @param list
         *            set of characters to select from
         */
        public CharSet(List list)
        {
            set = new char[list.size()];
            for (int i = 0; i < list.size(); i++)
            {
                set[i] = ((Character) list.get(i)).charValue();
            }
            Arrays.sort(set);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#generate()
         */
        public String generate()
        {
            return Character.toString(set[random.nextInt(set.length)]);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#includes
         * (char)
         */
        public boolean includes(char c)
        {
            return Arrays.binarySearch(set, c) >= 0;
        }
    }

    /**
     * Aggregate generator that selects from amongst its child generators. Each
     * child generator has an associated integral weight; when selecting a
     * character, the chance that each child generator will be used is the ratio
     * of the child's weight to the total weight of all children.
     * 
     */
    private class Group implements CharGen
    {
        private final ArrayList generators = new ArrayList();
        private final ArrayList weights = new ArrayList();

        private int weightTotal = 0;
        private CharGen defaultCharGen = null;

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#generate()
         */
        public String generate()
        {
            int choice = random.nextInt(weightTotal);
            for (int index = 0; index < generators.size(); index++)
            {
                int weight = ((Integer) weights.get(index)).intValue();
                if (weight > choice)
                {
                    return ((CharGen) generators.get(index)).generate();
                }

                choice -= weight;
            }

            return defaultCharGen.generate();
        }

        /**
         * Add default generator for characters that are not already covered by
         * this group's other generators. Note that calling add() after calling
         * this method will have unpredictable results.
         * 
         * @param weight
         *            relative weight for the default generator group.
         */
        public void addDefault(int weight)
        {
            weightTotal += weight;
            defaultCharGen = null;

            // Add chars from \t, \n, 32-126 to the list of default characters.
            // These are the standard ASCII range characters that are also in
            // the Annex A character list.
            ArrayList list = new ArrayList();

            if (!includes('\t'))
            {
                list.add(new Character('\t'));
            }

            if (!includes('\n'))
            {
                list.add(new Character('\n'));
            }

            if (!includes('\f'))
            {
                list.add(new Character('\f'));
            }

            // Note: more invalid characters could be added here.
            if (!includes('\u0080'))
            {
                list.add(new Character('\u0080'));
            }

            for (char c = 32; c < 127; c++)
            {
                if (!includes(c))
                {
                    list.add(new Character(c));
                }
            }

            ArrayList extended = new ArrayList();
            for (char c = 0xA0; c <= 0xFF; c++)
            {
                if (!includes(c))
                {
                    extended.add(new Character(c));
                }
            }

            Group group = new Group();
            defaultCharGen = group;
            group.add(new CharSet(list), 10);
            group.add(new CharSet(extended), 1);
        }

        /**
         * Add a Single generator.
         * 
         * @param c
         *            character to generate
         * @param weight
         *            weight of child generator
         */
        public void add(char c, int weight)
        {
            add(new Single(c), weight);
        }

        /**
         * Add a StringGen generator.
         * 
         * @param str
         *            string to generate
         * @param weight
         *            weight of child generator
         */
        public void add(String str, int weight)
        {
            add(new StringGen(str), weight);
        }

        /**
         * Add a child generator.
         * 
         * @param g
         *            child to add
         * @param weight
         *            weight of child generator
         */
        public void add(CharGen g, int weight)
        {
            generators.add(g);
            weights.add(new Integer(weight));
            weightTotal += weight;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.system.html.Generator.CharGen#includes
         * (char)
         */
        public boolean includes(char c)
        {
            for (int index = 0; index < generators.size(); index++)
            {
                CharGen g = (CharGen) generators.get(index);
                if (g.includes(c))
                {
                    return true;
                }
            }

            if (null != defaultCharGen)
            {
                return defaultCharGen.includes(c);
            }
            else
            {
                return false;
            }
        }
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////

    private final CharGen whitespace = new Group()
    {
        {
            add(' ', 50);
            add('\n', 20);
            add('\t', 5);
            add('\f', 1);
            add('\r', 5);
            add("\r\n", 5);
        }
    };

    private final CharGen lowerCase = new Range('a', 'z');
    private final CharGen upperCase = new Range('A', 'Z');

    private HashMap generators;

    /**
     * Create generators for each validating parser state.
     * 
     * @param parser
     *            parser for which generators will be created
     */
    private void createStateGenerators(ValidatingParser parser)
    {
        // Random HTML could be generated by just randomly choosing characters
        // with Random, but doing so would be very unlikely to cover all of the
        // parser states. By weighting the character generation based on the
        // current parser state, all states can be covered.

        generators = new HashMap();

        generators.put(parser.dataState, new Group()
        {
            {
                add('&', 1);
                add('<', 1);
                add(upperCase, 1);
                add(lowerCase, 5);
                add(' ', 1);
                addDefault(2);
            }
        });

        generators.put(parser.characterReferenceInDataState, new Group()
        {
            {
                Iterator iter = ValidatingParser.charEntities.keySet()
                        .iterator();
                while (iter.hasNext())
                {
                    String entity = (String) iter.next();
                    add(entity, 1); // Parse error (unless followed by a random
                                    // ';')
                    add(entity + ";", 5);
                }
                addDefault(300);
            }
        });

        generators.put(parser.tagOpenState, new Group()
        {
            {
                add('!', 1);
                add(lowerCase, 15);
                add(upperCase, 5);
                add('/', 50);
                add('?', 1); // parse error
                addDefault(1); // parse error
            }
        });

        generators.put(parser.endTagOpenState, new Group()
        {
            {
                add(upperCase, 5);
                add(lowerCase, 15);
                add('>', 1); // parse error
                addDefault(1); // parse error
            }
        });

        generators.put(parser.tagNameState, new Group()
        {
            {
                add('>', 10);
                add(upperCase, 10);
                add(lowerCase, 70);
                add(' ', 15);
                add(whitespace, 1);
                add('/', 2);
                addDefault(2);
            }
        });

        generators.put(parser.beforeAttributeNameState, new Group()
        {
            {
                add(whitespace, 5);
                add('/', 1);
                add('>', 20);
                add(upperCase, 5);
                add(lowerCase, 10);
                add('\"', 1); // parse error
                add('\'', 1); // parse error
                add('<', 1); // parse error
                add('=', 1); // parse error
                addDefault(3);
            }
        });

        generators.put(parser.attributeNameState, new Group()
        {
            {
                add(whitespace, 5);
                add('/', 1);
                add('=', 10);
                add('>', 3);
                add(upperCase, 10);
                add(lowerCase, 30);
                add('\"', 1); // parse error
                add('\'', 1); // parse error
                add('<', 1); // parse error
                addDefault(5);
            }
        });

        generators.put(parser.afterAttributeNameState, new Group()
        {
            {
                add(whitespace, 5);
                add('/', 1);
                add('=', 10);
                add('>', 3);
                add(upperCase, 10);
                add('\"', 1); // parse error
                add('\'', 1); // parse error
                add('<', 1); // parse error
                addDefault(5);
            }
        });

        generators.put(parser.beforeAttributeValueState, new Group()
        {
            {
                add(whitespace, 5);
                add('\"', 2);
                add('&', 1);
                add('\'', 2);
                add('>', 1); // parse error
                add('<', 1); // parse error
                add('=', 1); // parse error
                add('`', 1); // parse error
                addDefault(5);
            }
        });

        generators.put(parser.attributeValueDoubleQuotedState, new Group()
        {
            {
                add('\"', 2);
                add('&', 1);
                addDefault(20);
            }
        });

        generators.put(parser.attributeValueSingleQuotedState, new Group()
        {
            {
                add('\'', 2);
                add('&', 1);
                addDefault(20);
            }
        });

        generators.put(parser.attributeValueUnquotedState, new Group()
        {
            {
                add(whitespace, 5);
                add('&', 1);
                add('\"', 1); // parse error
                add('\'', 1); // parse error
                add('<', 1); // parse error
                add('=', 1); // parse error
                add('`', 1); // parse error
                addDefault(20);
            }
        });

        generators.put(parser.characterReferenceInAttributeValueState,
                generators.get(parser.characterReferenceInDataState));

        generators.put(parser.afterAttributeValueQuotedState, new Group()
        {
            {
                add(whitespace, 10);
                add('/', 1);
                add('>', 3);
                addDefault(1); // parse error
            }
        });

        generators.put(parser.selfClosingStartTagState, new Group()
        {
            {
                add('>', 10);
                addDefault(1); // parse error
            }
        });

        generators.put(parser.bogusCommentState, new Group()
        {
            {
                add('>', 1);
                addDefault(10);
            }
        });

        generators.put(parser.markupDeclarationOpenState, new Group()
        {
            {
                add("DOCTYPE", 1);
                add("--", 1);
                addDefault(1);
            }
        });

        generators.put(parser.commentStartState, new Group()
        {
            {
                add('-', 5);
                add('>', 1); // parse error
                addDefault(10);
            }
        });

        generators.put(parser.commentStartDashState, new Group()
        {
            {
                add('-', 2);
                add('>', 1); // parse error
                addDefault(4);
            }
        });

        generators.put(parser.commentState, new Group()
        {
            {
                add('-', 1);
                addDefault(4);
            }
        });

        generators.put(parser.commentEndDashState, new Group()
        {
            {
                add('-', 1);
                addDefault(1);
            }
        });

        generators.put(parser.commentEndState, new Group()
        {
            {
                add('>', 10);
                add(whitespace, 1); // parse error
                add('!', 1); // parse error
                add('-', 1); // parse error
                addDefault(1); // parse error
            }
        });

        generators.put(parser.commentEndBangState, new Group()
        {
            {
                add('-', 1);
                add('>', 2);
                addDefault(1);
            }
        });

        generators.put(parser.commentEndSpaceState, new Group()
        {
            {
                add(whitespace, 1);
                add('-', 1);
                add('>', 1);
                addDefault(1);
            }
        });
    }

    int charCount = 0;

    String generate(ValidatingParser parser)
    {
        ValidatingParser.State state = parser.getState();

        charCount++;

        if (state == parser.dataState)
        {
            // Generated HTML is at least 50 chars long
            if (random.nextInt(charCount) > 50)
            {
                return null;
            }
        }
        else
        {
            // For now, throw a premature EOF every 1 in 1000 characters.
            if (random.nextDouble() < 0.001)
            {
                return null;
            }
        }

        CharGen g = (CharGen) generators.get(state);
        if (null != g)
        {
            return g.generate();
        }
        else
        {
            throw new IllegalStateException("Unknown state: " + state);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Class to save text generated by the parser.
     * 
     */
    private static class Text
    {
        private final String text;

        /**
         * Constructor for text.
         * 
         * @param text
         *            text received from parser
         */
        public Text(String text)
        {
            this.text = text;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return "\"" + Util.escape(text) + "\"";
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            return text.hashCode();
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj)
        {
            if (!(obj instanceof Text))
                return false;
            Text other = (Text) obj;
            return text.equals(other.text);
        }
    }

    /**
     * Class to save start tags generated by the parser.
     * 
     */
    private static class StartTag
    {
        private final String name;
        private final Map attributes;

        /**
         * Constructor for start tags.
         * 
         * @param name
         *            name of tag
         * @param attributes
         *            tag attributes
         */
        public StartTag(String name, Map attributes)
        {
            this.name = name;
            this.attributes = attributes;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            StringBuffer buffer = new StringBuffer();
            buffer.append('<');
            buffer.append(name);
            if (!attributes.isEmpty())
            {
                ArrayList names = new ArrayList(attributes.keySet());
                Collections.sort(names);

                Iterator iter = names.iterator();
                while (iter.hasNext())
                {
                    buffer.append(' ');
                    String name = (String) iter.next();
                    String value = (String) attributes.get(name);

                    buffer.append(name);
                    if (value.length() > 0)
                    {
                        buffer.append("=\"");
                        buffer.append(Util.escape(value));
                        buffer.append('"');
                    }
                }
            }
            buffer.append('>');
            return buffer.toString();
        }

        /*
         * (non-Javadoc)
         * 
         * @note Auto-generated by Eclipse
         * 
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((attributes == null) ? 0 : attributes.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @note Auto-generated by Eclipse
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof StartTag))
                return false;
            StartTag other = (StartTag) obj;
            if (attributes == null)
            {
                if (other.attributes != null)
                    return false;
            }
            else if (!attributes.equals(other.attributes))
                return false;
            if (name == null)
            {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    /**
     * Class to save end tags generated by the parser.
     * 
     */
    private static class EndTag
    {
        private final String name;

        /**
         * Constructor for end tags
         * 
         * @param name
         *            end tag name
         */
        public EndTag(String name)
        {
            this.name = name;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return "</" + name + ">";
        }

        /*
         * (non-Javadoc)
         * 
         * @ note Auto-generated by Eclipse
         * 
         * @see java.lang.Object#hashCode()
         */
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         * 
         * @ note Auto-generated by Eclipse
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof EndTag))
                return false;
            EndTag other = (EndTag) obj;
            if (name == null)
            {
                if (other.name != null)
                    return false;
            }
            else if (!name.equals(other.name))
                return false;
            return true;
        }
    }

    /**
     * Generate an HTML string while parsing it, using the parser state to guide
     * generation.
     * 
     * @param tagHandler
     *            parser tag handler
     * @return generated HTML string
     */
    private String generate(ValidatingTagHandler tagHandler)
    {
        StringBuffer buffer = new StringBuffer();

        ValidatingParser parser = new ValidatingParser(tagHandler);
        createStateGenerators(parser);

        while (true)
        {
            String str = generate(parser);

            if (null == str)
            {
                if (verbose)
                {
                    System.out.println("\tEOF\t" + parser.getState());
                }
                parser.parse(ValidatingParser.EOF);
                break;
            }
            else
            {
                buffer.append(str);

                for (int i = 0; i < str.length(); i++)
                {
                    char c = str.charAt(i);
                    if (verbose)
                    {
                        System.out.println("\t'" + Util.escape(c) + "'\t"
                                + parser.getState());
                    }
                    parser.parse(c);
                }
            }
        }

        return buffer.toString();
    }

    /**
     * Read HTML from a file passed on the command line.
     * 
     * @param args
     *            command line arguments
     * @return HTML string, or null if reading fails
     */
    private static String getHTML(String[] args)
    {
        StringBuffer buffer = new StringBuffer();
        int count;
        byte[] input = new byte[1024];
        FileInputStream f;

        try
        {
            f = new FileInputStream(args[0]);
        }
        catch (FileNotFoundException e)
        {
            System.err.println(args[0] + " could not be opened.");
            return null;
        }
        try
        {
            while (-1 != (count = f.read(input)))
            {
                buffer.append(new String(input, 0, count));
            }
        }
        catch (IOException e)
        {
            System.err.println("Error while reading file.");
        }
        return buffer.toString();
    }

    /**
     * Write generated HTML to a temporary file.
     * 
     * @param html
     *            html to save
     */
    private static void writeHTML(String html)
    {
        FileOutputStream f;

        try
        {
            File dir = new File(".");
            File file = File.createTempFile("parse_failure", ".html", dir);
            f = new FileOutputStream(file);
            byte[] bytes = html.getBytes();
            f.write(bytes);
        }
        catch (IOException e)
        {
            System.err.println("Error writing file: " + e);
        }
    }

    /**
     * Run a single parsing test.
     * 
     * @param html
     *            html to parse; if null, randomly generate html.
     */
    private static void runTest(String html)
    {
        final ArrayList expectedTokens = new ArrayList();
        final ArrayList actualTokens = new ArrayList();
        boolean generated = false;

        ValidatingTagHandler handler = new ValidatingTagHandler()
        {
            public void text(String text)
            {
                Object token = new Text(text);
                if (verbose)
                {
                    System.out.println("Emitting " + token);
                }
                expectedTokens.add(token);
            }

            public void startTag(String name, Map attributes)
            {
                Object token = new StartTag(name, attributes);
                if (verbose)
                {
                    System.out.println("Emitting " + token);
                }
                expectedTokens.add(token);
            }

            public void endTag(String name)
            {
                Object token = new EndTag(name);
                if (verbose)
                {
                    System.out.println("Emitting " + token);
                }
                expectedTokens.add(token);
            }

            public void parseError(int code)
            {
                if (verbose)
                {
                    System.out.println("PARSE ERROR " + code);
                }
            }
        };

        if (html != null)
        {
            ValidatingParser parser = new ValidatingParser(handler);

            for (int i = 0; i < html.length(); i++)
            {
                char c = html.charAt(i);
                System.out.println("\t" + Util.escape(c) + "\t"
                        + parser.getState());
                parser.parse(c);
            }
            parser.parse(ValidatingParser.EOF);
        }
        else
        {
            generated = true;
            Generator generator = new Generator();
            html = generator.generate(handler);
        }

        TagHandler tagHandler = new TagHandler()
        {
            public void text(String text)
            {
                actualTokens.add(new Text(text));
            }

            public void startTag(String name, Map attributes)
            {
                actualTokens.add(new StartTag(name, attributes));
            }

            public void endTag(String name)
            {
                actualTokens.add(new EndTag(name));
            }
        };

        boolean exception = false;
        try
        {
            Parser.parse(html, tagHandler);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            exception = true;
        }

        if (verbose)
        {
            System.out
                    .println("\n====================================================");
            System.out.println("HTML: \"" + Util.escape(html) + "\"");
            System.out
                    .println("====================================================\n");
        }

        if (!exception && actualTokens.equals(expectedTokens))
        {
            if (verbose)
            {
                System.out.println("Generated HTML parsed as expected.");
            }

            if (!generated)
            {
                for (int i = 0; i < actualTokens.size(); i++)
                {
                    System.out.println(expectedTokens.get(i));
                }
            }
        }
        else
        {
            System.err.println("ERROR: incorrect parsing!");
            System.err.println("HTML: \"" + Util.escape(html) + "\"");

            int i;
            int length = Math.min(actualTokens.size(), expectedTokens.size());
            for (i = 0; i < length; i++)
            {
                Object expected = expectedTokens.get(i);
                Object actual = actualTokens.get(i);
                if (!expected.equals(actual))
                {
                    System.err.println("Token #" + (i + 1) + ":");
                    System.err.println("  Expected: " + expected);
                    System.err.println("  Actual:   " + actual);
                }
            }

            for (i = length; i < actualTokens.size(); i++)
            {
                System.err.println("Extra token: " + actualTokens.get(i));
            }

            for (i = length; i < expectedTokens.size(); i++)
            {
                System.err.println("Missing token: " + expectedTokens.get(i));
            }

            if (generated)
            {
                writeHTML(html);
            }

            System.exit(1);
        }
    }

    /**
     * Main entry point. With no arguments, randomly generates HTML and compares
     * the output of the two parsers. With an HTML file as an argument, parses
     * that file and prints details about the parsing process.
     * 
     * @param args
     *            command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length > 0)
        {
            String html = getHTML(args);
            runTest(html);
        }
        else
        {
            int count = 0;
            while (count < 1000000)
            {
                runTest(null);
                count++;
                if ((count % 100) == 0)
                {
                    System.out.println("Count: " + count);
                }
            }
        }
    }
}
