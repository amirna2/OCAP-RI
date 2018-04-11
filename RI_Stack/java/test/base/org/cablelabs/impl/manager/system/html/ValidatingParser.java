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

/**
 * HTML parser for validating documents. Generates parse errors as described in
 * the HTML5 specification.
 * 
 * @author Spencer Schumann
 * 
 */
public class ValidatingParser
{
    // ///////////////////////////////////////////////////////////////////////
    // Parsing states as defined by the HTML 5 draft specification. The
    // goal of the code in this section is to match as closely as possible
    // the descriptions of the states in the specification so that it
    // obviously implements the specification correctly. No attempt is
    // made to make this code fast or small.

    // The section numbers for each state reference the HTML5 draft
    // specification at http://dev.w3.org/html5/spec/tokenization.html.

    /**
     * Data state, section 8.2.4.1
     */
    public State dataState = new State("Data")
    {
        public void consume(char c)
        {
            if (c == '&')
            {
                setState(characterReferenceInDataState);
            }
            else if (c == '<')
            {
                setState(tagOpenState);
            }
            else if (c == EOF)
            {
                // Note: no actual EOF token needs to be emitted for this
                // implementation.
            }
            else
            {
                emitChar(c);
            }
        }
    };

    /**
     * Character reference in data state, section 8.2.4.2
     */
    public State characterReferenceInDataState = new State(
            "CharacterReferenceInData")
    {
        public void consume(char c)
        {
            createCharacterReferenceConsumer();
            reconsume(c);
        }

        public void characterReference(char c)
        {
            if (c == EOF)
            {
                emitChar('&');
            }
            else
            {
                emitChar(c);
            }
            setState(dataState);
        }
    };

    /**
     * Tag open state, section 8.2.4.8
     */
    public State tagOpenState = new State("TagOpen")
    {
        public void consume(char c)
        {
            if ('!' == c)
            {
                setState(markupDeclarationOpenState);
            }
            else if ('/' == c)
            {
                setState(endTagOpenState);
            }
            else if (isUpperCase(c))
            {
                createNewStartTag();
                tagName.append((char) (c + 0x20));
                setState(tagNameState);
            }
            else if (isLowerCase(c))
            {
                createNewStartTag();
                tagName.append(c);
                setState(tagNameState);
            }
            else if ('?' == c)
            {
                // TODO: add real parse error codes here and on every other
                // occurrance of handler.parseError(). (CR 44152)
                handler.parseError(0);
                setState(bogusCommentState);
            }
            else
            {
                handler.parseError(0);
                emitChar('<');
                setState(dataState);
                reconsume(c);
            }
        }
    };

    /**
     * End tag open state, section 8.2.4.9
     */
    public State endTagOpenState = new State("EndTagOpen")
    {
        public void consume(char c)
        {
            if (isUpperCase(c))
            {
                createNewEndTag();
                tagName.append((char) (c + 0x20));
                setState(tagNameState);
            }
            else if (isLowerCase(c))
            {
                createNewEndTag();
                tagName.append(c);
                setState(tagNameState);
            }
            else if (c == '>')
            {
                handler.parseError(0);
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                emitChar('<');
                emitChar('/');
                setState(dataState);
                reconsume(c);
            }
            else
            {
                handler.parseError(0);
                setState(bogusCommentState);
            }
        }
    };

    /**
     * Tag name state, section 8.2.4.10
     */
    public State tagNameState = new State("TagName")
    {
        public void consume(char c)
        {
            if (isWhitespace(c))
            {
                setState(beforeAttributeNameState);
            }
            else if (c == '/')
            {
                setState(selfClosingStartTagState);
            }
            else if (c == '>')
            {
                emitTag();
                setState(dataState);
            }
            else if (isUpperCase(c))
            {
                tagName.append((char) (c + 0x20));
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                tagName.append(c);
            }
        }
    };

    /**
     * Before attribute name state, section 8.2.4.34
     */
    public State beforeAttributeNameState = new State("BeforeAttributeName")
    {
        public void consume(char c)
        {
            if (isWhitespace(c))
            {
                // stay in state.
            }
            else if (c == '/')
            {
                setState(selfClosingStartTagState);
            }
            else if (c == '>')
            {
                emitTag();
                setState(dataState);
            }
            else if (isUpperCase(c))
            {
                startNewAttribute();
                attributeName.append((char) (c + 0x20));
                setState(attributeNameState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                if (c == '\"' || c == '\'' || c == '<' || c == '=')
                {
                    handler.parseError(0);
                }
                startNewAttribute();
                attributeName.append(c);
                setState(attributeNameState);
            }
        }
    };

    /**
     * Attribute name state, section 8.2.4.35
     */
    public State attributeNameState = new State("AttributeName")
    {
        public void consume(char c)
        {
            boolean emit = false;

            if (isWhitespace(c))
            {
                setState(afterAttributeNameState);
            }
            else if (c == '/')
            {
                setState(selfClosingStartTagState);
            }
            else if (c == '=')
            {
                setState(beforeAttributeValueState);
            }
            else if (c == '>')
            {
                emit = true;
                setState(dataState);
            }
            else if (isUpperCase(c))
            {
                attributeName.append((char) (c + 0x20));
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                if (c == '\"' || c == '\'' || c == '<')
                {
                    handler.parseError(0);
                }
                attributeName.append(c);
            }

            if (getState() != attributeNameState)
            {
                // Leaving state - check attribute name and drop it if already
                // present
                String name = removeInvalidChars(attributeName.toString());
                if (attributes.containsKey(name))
                {
                    handler.parseError(0);
                    attributeName = null;
                    // TODO: test the case where an EOF terminated
                    // the name (with one parse error) and that name was a
                    // duplicated (a second parse error) (CR 44152)
                }
            }

            if (emit)
            {
                emitTag();
            }
        }
    };

    /**
     * After attribute name state, section 8.2.4.36
     */
    public State afterAttributeNameState = new State("AfterAttributeName")
    {
        public void consume(char c)
        {
            if (isWhitespace(c))
            {
                // stay in state
            }
            else if (c == '/')
            {
                setState(selfClosingStartTagState);
            }
            else if (c == '=')
            {
                setState(beforeAttributeValueState);
            }
            else if (c == '>')
            {
                emitTag();
                setState(dataState);
            }
            else if (isUpperCase(c))
            {
                startNewAttribute();
                attributeName.append((char) (c + 0x20));
                setState(attributeNameState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                if (c == '\"' || c == '\'' || c == '<')
                {
                    handler.parseError(0);
                }
                startNewAttribute();
                attributeName.append(c);
                setState(attributeNameState);
            }
        }
    };

    /**
     * Before attribute value state, section 8.2.4.37
     */
    public State beforeAttributeValueState = new State("BeforeAttributeValue")
    {
        public void consume(char c)
        {
            if (isWhitespace(c))
            {
                // Stay in state
            }
            else if (c == '\"')
            {
                setState(attributeValueDoubleQuotedState);
            }
            else if (c == '&')
            {
                setState(attributeValueUnquotedState);
                reconsume(c);
            }
            else if (c == '\'')
            {
                setState(attributeValueSingleQuotedState);
            }
            else if (c == '>')
            {
                handler.parseError(0);
                emitTag();
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                if (c == '<' || c == '=' || c == '`')
                {
                    handler.parseError(0);
                }
                attributeValue.append(c);
                setState(attributeValueUnquotedState);
            }
        }
    };

    /**
     * Attribute value (double-quoted) state, section 8.2.4.38
     */
    public State attributeValueDoubleQuotedState = new State(
            "AttributeValueDoubleQuoted")
    {
        public void consume(char c)
        {
            if (c == '\"')
            {
                setState(afterAttributeValueQuotedState);
            }
            else if (c == '&')
            {
                setState(characterReferenceInAttributeValueState);
                createCharacterReferenceConsumer('\"');
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                attributeValue.append(c);
            }
        }
    };

    /**
     * Attribute value (single-quoted) state, section 8.2.4.39
     */
    public State attributeValueSingleQuotedState = new State(
            "AttributeValueSingleQuoted")
    {
        public void consume(char c)
        {
            if (c == '\'')
            {
                setState(afterAttributeValueQuotedState);
            }
            else if (c == '&')
            {
                setState(characterReferenceInAttributeValueState);
                createCharacterReferenceConsumer('\'');
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                attributeValue.append(c);
            }
        }
    };

    /**
     * Attribute value (unquoted) state, section 8.2.4.40
     */
    public State attributeValueUnquotedState = new State(
            "AttributeValueUnquoted")
    {
        public void consume(char c)
        {
            if (isWhitespace(c))
            {
                setState(beforeAttributeNameState);
            }
            else if (c == '&')
            {
                setState(characterReferenceInAttributeValueState);
                createCharacterReferenceConsumer('>');
            }
            else if (c == '>')
            {
                emitTag();
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                if (c == '\"' || c == '\'' || c == '<' || c == '=' || c == '`')
                {
                    handler.parseError(0);
                }
                attributeValue.append(c);
            }
        }
    };

    /**
     * Character reference in attribute value state, section 8.2.4.41
     */
    public State characterReferenceInAttributeValueState = new State(
            "CharacterReferenceInAttributeValue")
    {
        public void consume(char c)
        {
            throw new IllegalStateException(
                    "No characters should be consumed directly within this state");
        }

        public void characterReference(char c)
        {
            if (c == EOF)
            {
                attributeValue.append('&');
            }
            else
            {
                attributeValue.append(c);
            }
            setState(getPreviousState());
        }
    };

    /**
     * After attribute value (quoted) state, section 8.2.4.42
     */
    public State afterAttributeValueQuotedState = new State(
            "AfterAttributeValueQuoted")
    {
        public void consume(char c)
        {
            if (isWhitespace(c))
            {
                setState(beforeAttributeNameState);
            }
            else if (c == '/')
            {
                setState(selfClosingStartTagState);
            }
            else if (c == '>')
            {
                emitTag();
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                handler.parseError(0);
                setState(beforeAttributeNameState);
                reconsume(c);
            }
        }
    };

    /**
     * Self-closing start tag state, section 8.2.4.43
     */
    public State selfClosingStartTagState = new State("SelfClosingStartTag")
    {
        public void consume(char c)
        {
            if (c == '>')
            {
                selfClosingTag = true;
                emitTag();
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                handler.parseError(0);
                setState(beforeAttributeNameState);
                reconsume(c);
            }
        }
    };

    /**
     * Bogus comment state, section 8.2.4.44
     */
    public State bogusCommentState = new State("BogusComment")
    {
        public void consume(char c)
        {
            if (c == '>')
            {
                setState(dataState);
            }
            else if (c == EOF)
            {
                setState(dataState);
                reconsume(c);
            }
            else
            {
                // Discard the character. There's no need to emit comment tokens
                // in this implementation.
            }
        }
    };

    /**
     * Markup declaration open state, section 8.2.4.45
     */
    public State markupDeclarationOpenState = new State("MarkupDeclarationOpen")
    {
        StringBuffer buffer;

        public void enterState()
        {
            buffer = new StringBuffer();
        }

        public void consume(char c)
        {
            buffer.append(c);

            if (buffer.toString().equals("--"))
            {
                setState(commentStartState);
            }
            else if (buffer.toString().equalsIgnoreCase("DOCTYPE"))
            {
                // Treat DOCTYPE as a bogus comment with no parse error.
                setState(bogusCommentState);
            }
            else if (buffer.length() > "DOCTYPE".length() || c == EOF)
            {
                handler.parseError(0);
                setState(bogusCommentState);
                for (int i = 0; i < buffer.length(); i++)
                {
                    reconsume(buffer.charAt(i));
                }
            }
        }
    };

    /**
     * Comment start state, section 8.2.4.46
     */
    public State commentStartState = new State("CommentStart")
    {
        public void consume(char c)
        {
            if (c == '-')
            {
                setState(commentStartDashState);
            }
            else if (c == '>')
            {
                handler.parseError(0);
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                setState(commentState);
            }
        }
    };

    /**
     * Comment start dash state, section 8.2.4.47
     */
    public State commentStartDashState = new State("CommentStartDash")
    {
        public void consume(char c)
        {
            if (c == '-')
            {
                setState(commentEndState);
            }
            else if (c == '>')
            {
                handler.parseError(0);
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                setState(commentState);
            }
        }
    };

    /**
     * Comment state, section 8.2.4.48
     */
    public State commentState = new State("Comment")
    {
        public void consume(char c)
        {
            if (c == '-')
            {
                setState(commentEndDashState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                // Stay in state.
            }
        }
    };

    /**
     * Comment end dash state, section 8.2.4.49
     */
    public State commentEndDashState = new State("CommentEndDash")
    {
        public void consume(char c)
        {
            if (c == '-')
            {
                setState(commentEndState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                setState(commentState);
            }
        }
    };

    /**
     * Comment end state, section 8.2.4.50
     */
    public State commentEndState = new State("CommentEnd")
    {
        public void consume(char c)
        {
            if (c == '>')
            {
                setState(dataState);
            }
            else if (isWhitespace(c))
            {
                handler.parseError(0);
                setState(commentEndSpaceState);
            }
            else if (c == '!')
            {
                handler.parseError(0);
                setState(commentEndBangState);
            }
            else if (c == '-')
            {
                handler.parseError(0);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                handler.parseError(0);
                setState(commentState);
            }
        }
    };

    /**
     * Comment end bang state, section 8.2.4.51
     */
    public State commentEndBangState = new State("CommentEndBang")
    {
        public void consume(char c)
        {
            if (c == '-')
            {
                setState(commentEndDashState);
            }
            else if (c == '>')
            {
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                setState(commentState);
            }
        }
    };

    /**
     * Comment end space state, section 8.2.4.52
     */
    public State commentEndSpaceState = new State("CommentEndSpace")
    {
        public void consume(char c)
        {
            if (isWhitespace(c))
            {
                // Stay in state
            }
            else if (c == '-')
            {
                setState(commentEndDashState);
            }
            else if (c == '>')
            {
                setState(dataState);
            }
            else if (c == EOF)
            {
                handler.parseError(0);
                setState(dataState);
                reconsume(c);
            }
            else
            {
                setState(commentState);
            }
        }
    };

    // ////////////////////////////////////////////////////////////////////////
    // Character references: The HTML5 spec does not define explicit states for
    // parsing character references. See section 8.2.4.70, Tokenizing character
    // references.

    /**
     * Create a character reference consumer state with no additional allowed
     * character. See HTML5 rules for processing character entities.
     * 
     */
    private void createCharacterReferenceConsumer()
    {
        createCharacterReferenceConsumer(EOF);
    }

    /**
     * Create a character reference consumer state with the given additional
     * allowed character. See HTML5 rules for processing character entities.
     * 
     * @param allowed
     */
    private void createCharacterReferenceConsumer(final char allowed)
    {
        final StringBuffer buffer = new StringBuffer();

        final Runnable charRefFailure = new Runnable()
        {
            public void run()
            {
                // Not a character reference.
                specialState = null;
                state.characterReference(EOF);

                // Reconsume all the characters that were consumed.
                for (int i = 0; i < buffer.length(); i++)
                {
                    reconsume(buffer.charAt(i));
                }
            }
        };

        final State charRef = new State("CharacterReference")
        {
            public void consume(char c)
            {
                if (c == ';')
                {
                    Character ref = (Character) charEntities.get(buffer
                            .toString());
                    if (null == ref)
                    {
                        handler.parseError(0);
                        buffer.append(c);
                        charRefFailure.run();
                    }
                    else
                    {
                        specialState = null;
                        state.characterReference(ref.charValue());
                    }
                }
                else if (c == EOF)
                {
                    handler.parseError(0);
                    buffer.append(c);
                    charRefFailure.run();
                }
                else
                {
                    buffer.append(c);
                }
            }
        };

        final State potentialCharRef = new State("PotentialCharacterReference")
        {
            public void consume(char c)
            {
                if (isWhitespace(c) || c == '<' || c == '&' || c == EOF
                        || c == additionalAllowedCharacter)
                {
                    buffer.append(c);
                    charRefFailure.run();
                }
                else
                {
                    specialState = charRef;
                    reconsume(c);
                }
            }
        };

        specialState = potentialCharRef;
    }

    // End of HTML 5 parsing states.
    // ///////////////////////////////////////////////////////////////////////

    /**
     * End of file character.
     */
    public static final char EOF = CharacterIterator.DONE;

    private final ValidatingTagHandler handler;
    private State state = null;
    private State previousState = null;
    private StringBuffer text = new StringBuffer();

    private StringBuffer tagName = new StringBuffer();
    private StringBuffer attributeName = new StringBuffer();
    private StringBuffer attributeValue = new StringBuffer();
    private HashMap attributes = new HashMap();
    private boolean closingTag = false;
    private boolean selfClosingTag = false;
    private char additionalAllowedCharacter = EOF;
    private State specialState = null;
    private char lastChar = '\0';
    static HashMap charEntities = new HashMap();

    /**
     * Constructor for parser. Parsing results will be returned through the
     * callback methods of handler.
     * 
     * @param handler
     *            handler to receive parsing results.
     */
    public ValidatingParser(ValidatingTagHandler handler)
    {
        this.handler = handler;
        setState(dataState);
    }

    /**
     * Feed a single character into the parser.
     * 
     * @param c
     *            character to parse
     */
    public void parse(char c)
    {
        // Note: this change to '\r' handling needs to be added to the Baseline
        // HTML Profile specification.
        if (lastChar == '\r')
        {
            if (c == '\n')
            {
                // CR followed by LF - drop the CR
            }
            else
            {
                // CR followed by something else - convert last CR to LF
                parsePreprocessed('\n');
            }
        }

        if (c != '\r')
        {
            parsePreprocessed(c);
        }

        lastChar = c;

        // NOTE: for validation purposes, the line and column numbers could be
        // saved here.
    }

    /**
     * Feed a single character into the parser after it has been preprocessed to
     * remove '\r' characters.
     * 
     * @param c
     *            character to parse
     */
    private void parsePreprocessed(char c)
    {
        if (specialState != null)
        {
            specialState.consume(c);
        }
        else
        {
            state.consume(c);
        }

        if (c == EOF)
        {
            // Emit any text that hasn't been emitted yet
            emitText();
        }
    }

    /**
     * Parse the contents of a string.
     * 
     * @param str
     *            string to parse
     */
    public void parse(String str)
    {
        for (int i = 0; i < str.length(); i++)
        {
            parse(str.charAt(i));
        }
        parse(EOF);
    }

    /**
     * Get the current state of the parser.
     * 
     * @return parser state
     */
    public State getState()
    {
        return state;
    }

    /**
     * Base class for all parser state objects
     * 
     */
    public abstract class State
    {
        private String name;

        /**
         * Constructor for parser state.
         * 
         * @param name
         *            name of state
         */
        protected State(String name)
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
            return name;
        }

        /**
         * Handle a translated character reference.
         * 
         * @param c
         *            translated character
         */
        public void characterReference(char c)
        {
            throw new IllegalStateException("Unexpected character reference");
        }

        /**
         * Called when transitioning to a new state to allow the state to
         * perform any needed initialization.
         * 
         */
        public void enterState()
        {
        }

        /**
         * Consume a character.
         * 
         * @param c
         *            character to consume.
         */
        public abstract void consume(char c);
    }

    /**
     * Reconsume a character, feeding it back into the parser again.
     * 
     * @param c
     *            character to reconsume
     */
    private void reconsume(char c)
    {
        parsePreprocessed(c);
    }

    /**
     * Checks whether a character is upper case, according to the HTML5 rules.
     * 
     * @param c
     *            character to test
     * @return true if upper case, false otherwise
     */
    private static boolean isUpperCase(char c)
    {
        return 'A' <= c && c <= 'Z';
    }

    /**
     * Checks whether a character is lower case, according to the HTML5 rules.
     * 
     * @param c
     *            character to test
     * @return true if lower case, false otherwise
     */
    private static boolean isLowerCase(char c)
    {
        return 'a' <= c && c <= 'z';
    }

    /**
     * Checks whether a character is a valid Baseline HTML Profile character.
     * 
     * @param c
     *            character to test
     * @return true if valid, false otherwise
     */
    private static boolean isValid(char c)
    {
        // Test for valid characters as defined in Table A-2 of
        // OC-SP-CCIF2.0-I20-091211.
        // Note: '\f' is not listed as a valid character in Table A-2, and it
        // should be added.
        return (32 <= c && c <= 126) || (160 <= c && c <= 255) || c == '\t'
                || c == '\n' || c == '\f';
    }

    /**
     * Return a new string with all characters removed for which isValid()
     * returns false.
     * 
     * @param str
     *            string to filter
     * @return string with invalid characters removed.
     */
    private static String removeInvalidChars(String str)
    {
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i < str.length(); i++)
        {
            char c = str.charAt(i);
            if (isValid(c))
            {
                buffer.append(c);
            }
        }

        return buffer.toString();
    }

    /**
     * Checks whether a character is white space, according to the HTML5 rules.
     * 
     * @param c
     *            character to test
     * @return true if white space, false otherwise
     */
    private static boolean isWhitespace(char c)
    {
        switch (c)
        {
            case '\t':
            case '\n':
            case '\f':
            case ' ':
                return true;

            default:
                return false;
        }
    }

    /**
     * Set the parser state.
     * 
     * @param next
     *            state to set
     */
    private void setState(State next)
    {
        previousState = state;
        state = next;
        state.enterState();
    }

    /**
     * Get the parser's previous state
     * 
     * @return previous state
     */
    private State getPreviousState()
    {
        return previousState;
    }

    /**
     * Emit a character of text data. Characters are buffered and emitted as one
     * contiguous string.
     * 
     * @param c
     *            character to emit
     */
    private void emitChar(char c)
    {
        text.append(c);
    }

    /**
     * Emit buffered text.
     * 
     */
    private void emitText()
    {
        String str = removeInvalidChars(text.toString());
        if (str.length() > 0)
        {
            handler.text(str);
        }
        text = new StringBuffer();
    }

    /**
     * Emit a start or end tag.
     * 
     */
    private void emitTag()
    {
        // There might be un-emitted text that needs to be emitted before the
        // tag.
        emitText();

        // Add current attribute if one was being constructed
        startNewAttribute();

        if (closingTag)
        {
            if (!attributes.isEmpty())
            {
                handler.parseError(0);
            }
            if (selfClosingTag)
            {
                handler.parseError(0);
            }
            String name = removeInvalidChars(tagName.toString());
            handler.endTag(name);
        }
        else
        {
            // TODO: only certain tags can be self-closing tags - generate parse
            // error when appropriate. (CR 44152)
            String name = removeInvalidChars(tagName.toString());
            handler.startTag(name, attributes);
        }
    }

    /**
     * Create a new start tag.
     * 
     */
    private void createNewStartTag()
    {
        tagName = new StringBuffer();
        attributes = new HashMap();
        attributeName = new StringBuffer();
        attributeValue = new StringBuffer();
        closingTag = false;
        selfClosingTag = false;
    }

    /**
     * Create a new end tag.
     * 
     */
    private void createNewEndTag()
    {
        createNewStartTag();
        closingTag = true;
    }

    /**
     * Begin a new attribute within the current tag.
     * 
     */
    private void startNewAttribute()
    {
        if (null != attributeName && attributeName.length() > 0)
        {
            String name = removeInvalidChars(attributeName.toString());
            String value = removeInvalidChars(attributeValue.toString());
            attributes.put(name, value);
        }
        attributeName = new StringBuffer();
        attributeValue = new StringBuffer();
    }

    /**
     * Convenience method for initializing the character entity map.
     * 
     * @param name
     *            name of character entity
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

        // Add numeric expressions
        for (char c = 0; c < 256; c++)
        {
            if (isValid(c))
            {
                addCharEntity("#" + Integer.toString(c), c);
            }
        }
    }
}
