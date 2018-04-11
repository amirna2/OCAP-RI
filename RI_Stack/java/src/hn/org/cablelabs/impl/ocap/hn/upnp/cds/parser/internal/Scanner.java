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

package org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal;

public class Scanner
{
    private static final boolean INCLUDE_TEST_CODE = true;

    private static final char[] EMPTY_CHAR_ARRAY = {};

    private static final int BAD = 0;

    public static final int END = 1;

    public static final int ASTERISK = 2;

    // public static final int WHITE_CHAR = 3;
    public static final int LEFT_PAREN = 4;

    public static final int RIGHT_PAREN = 5;

    public static final int BIN_OP = 6;

    public static final int STRING = 7;

    public static final int BOOL = 8;

    public static final int EXISTS = 9;

    public static final int AND = 10;

    public static final int OR = 11;

    public static final int PROPERTY = 12;

    private static final int NONE = 0;

    public static final int EQUAL = 1;

    public static final int NOT_EQUAL = 2;

    public static final int LESS_OR_EQUAL = 3;

    public static final int LESS = 4;

    public static final int GREATER_OR_EQUAL = 5;

    public static final int GREATER = 6;

    public static final int CONTAINS = 7;

    public static final int DERIVED_FROM = 8;

    public static final int DOES_NOT_CONTAIN = 9;

    public static final int FALSE = 1;

    public static final int TRUE = 2;

    private final char[] ca;

    private final int n;

    private int i;

    // TODO: add Token class?
    private int tokenIntValue;

    private String tokenStrValue;

    public Scanner(String s)
    {
        ca = s != null ? s.toCharArray() : EMPTY_CHAR_ARRAY;
        n = ca.length;
    }

    public int getTokenIntValue()
    {
        return tokenIntValue;
    }

    public String getTokenStrValue()
    {
        return tokenStrValue;
    }

    private static boolean isIdChar(char c)
    {
        // TODO: support other Unicode characters from XML std, section 2.3

        switch (c)
        {
            case ':':
            case '_':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
            case '-':
            case '.':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return true;
            default:
                return false;
        }
    }

    private static boolean isIdFirstChar(char c)
    {
        // TODO: support other Unicode characters from XML std, section 2.3

        switch (c)
        {
            case ':':
            case '_':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
                return true;
            default:
                return false;
        }
    }

    private static boolean isWhiteChar(char c)
    {
        // TODO: handle white space the way the screwy grammar calls for

        switch (c)
        {
            case ' ':
            case '\t':
            case '\n':
            case '\013':
            case '\f':
            case '\r':
                return true;
            default:
                return false;
        }
    }

    public static void main(String[] args)
    {
        if (INCLUDE_TEST_CODE)
        {
            test("foo = \"bar\" and id exists true");
        }
    }

    public int nextToken()
    {
        while (i < n && isWhiteChar(ca[i]))
        {
            ++i;
        }

        tokenIntValue = NONE;
        tokenStrValue = null;

        assert i <= n;

        if (i == n)
        {
            // TODO: prevent multiple END returns?

            return END;
        }

        int result;

        char c = ca[i];
        ++i;

        try
        {
            // TODO: more lexical error checking of 'property'?

            if (isIdFirstChar(c))
            {
                StringBuffer sbId = new StringBuffer();
                sbId.append(c);

                while (i < n && isIdChar(ca[i]))
                {
                    char d = ca[i];
                    ++i;

                    sbId.append(d);
                }

                if (i < n && ca[i] == '@')
                {
                    char d = ca[i];
                    ++i;

                    sbId.append(d);

                    if (!(i < n && isIdFirstChar(ca[i])))
                    {
                        throw new BadTokenException();
                    }

                    d = ca[i];
                    ++i;

                    sbId.append(d);

                    while (i < n && isIdChar(ca[i]))
                    {
                        d = ca[i];
                        ++i;

                        sbId.append(d);
                    }
                }

                // TODO: do lookup?
                String id = new String(sbId);

                if ("and".equals(id))
                {
                    result = AND;
                }
                else if ("contains".equals(id))
                {
                    result = BIN_OP;
                    tokenIntValue = CONTAINS;
                }
                else if ("derivedfrom".equals(id))
                {
                    result = BIN_OP;
                    tokenIntValue = DERIVED_FROM;
                }
                else if ("doesNotContain".equals(id))
                {
                    result = BIN_OP;
                    tokenIntValue = DOES_NOT_CONTAIN;
                }
                else if ("exists".equals(id))
                {
                    result = EXISTS;
                }
                else if ("false".equals(id))
                {
                    result = BOOL;
                    tokenIntValue = FALSE;
                }
                else if ("or".equals(id))
                {
                    result = OR;
                }
                else if ("true".equals(id))
                {
                    result = BOOL;
                    tokenIntValue = TRUE;
                }
                else
                {
                    result = PROPERTY;
                    tokenStrValue = id;
                }
            }
            else
            {
                char d;

                switch (c)
                {
                    case '@':
                        StringBuffer sbId = new StringBuffer();
                        sbId.append(c);

                        if (!(i < n && isIdFirstChar(ca[i])))
                        {
                            throw new BadTokenException();
                        }

                        d = ca[i];
                        ++i;

                        sbId.append(d);

                        while (i < n && isIdChar(ca[i]))
                        {
                            d = ca[i];
                            ++i;

                            sbId.append(d);
                        }

                        result = PROPERTY;
                        tokenStrValue = new String(sbId);
                        break;

                    case '*':
                        result = ASTERISK;
                        break;

                    /*
                     * case ' ': case '\t': case '\n': case '\013': case '\f':
                     * case '\r': result = WHITE_CHAR; break;
                     */

                    case '(':
                        result = LEFT_PAREN;
                        break;

                    case ')':
                        result = RIGHT_PAREN;
                        break;

                    case '=':
                        result = BIN_OP;
                        tokenIntValue = EQUAL;
                        break;

                    case '!':
                        if (!(i < n && ca[i] == '='))
                        {
                            throw new BadTokenException();
                        }

                        ++i;
                        result = BIN_OP;
                        tokenIntValue = NOT_EQUAL;
                        break;

                    case '<':
                        result = BIN_OP;
                        if (i < n && ca[i] == '=')
                        {
                            ++i;
                            tokenIntValue = LESS_OR_EQUAL;
                        }
                        else
                        {
                            tokenIntValue = LESS;
                        }
                        break;

                    case '>':
                        result = BIN_OP;
                        if (i < n && ca[i] == '=')
                        {
                            ++i;
                            tokenIntValue = GREATER_OR_EQUAL;
                        }
                        else
                        {
                            tokenIntValue = GREATER;
                        }
                        break;

                    case '"':
                        StringBuffer sbString = new StringBuffer();

                        while (i < n && ca[i] != '"')
                        {
                            d = ca[i];
                            ++i;

                            if (d == '\\')
                            {
                                assert i <= n;

                                if (i == n)
                                {
                                    throw new BadTokenException();
                                }

                                d = ca[i];
                                ++i;

                                if (!(d == '"' || d == '\\'))
                                {
                                    throw new BadTokenException();
                                }
                            }

                            sbString.append(d);
                        }

                        assert i <= n;

                        if (i == n)
                        {
                            throw new BadTokenException();
                        }

                        ++i;
                        result = STRING;
                        tokenStrValue = new String(sbString);
                        break;

                    default:
                        throw new BadTokenException();
                }
            }
        }
        catch (BadTokenException e)
        {
            result = BAD;
        }

        return result;
    }

    public static String prettyNameBinOp(int binOp)
    {
        switch (binOp)
        {
            case EQUAL:
                return "is equal to";
            case NOT_EQUAL:
                return "is not equal to";
            case LESS_OR_EQUAL:
                return "is less than or equal to";
            case LESS:
                return "is less than";
            case GREATER_OR_EQUAL:
                return "is greater than or equal to";
            case GREATER:
                return "is greater than";
            case CONTAINS:
                return "contains";
            case DERIVED_FROM:
                return "is derived from";
            case DOES_NOT_CONTAIN:
                return "does not contain";
            default:
                throw new IllegalStateException();
        }
    }

    public static String prettyNameExistsBool(int bool)
    {
        switch (bool)
        {
            case TRUE:
                return "exists";
            case FALSE:
                return "does not exist";
            default:
                throw new IllegalStateException();
        }
    }

    public static String prettyNameToken(int token)
    {
        switch (token)
        {
            case BAD:
                return "unrecognizable token";

            case END:
                return "end of string";

            case ASTERISK:
                return "asterisk";

                /*
                 * case WHITE_CHAR: return "white char";
                 */

            case LEFT_PAREN:
                return "left paren";

            case RIGHT_PAREN:
                return "right paren";

            case BIN_OP:
                return "bin op";

            case STRING:
                return "string";

            case BOOL:
                return "bool";

            case EXISTS:
                return "exists";

            case AND:
                return "and";

            case OR:
                return "or";

            case PROPERTY:
                return "property";

            default:
                throw new IllegalStateException();
        }
    }

    private static void test(String s)
    {
        if (INCLUDE_TEST_CODE)
        {
            System.out.println(s);

            Scanner scanner = new Scanner(s);

            for (int t = scanner.nextToken(); t != END; t = scanner.nextToken())
            {
                switch (t)
                {
                    case BAD:
                        System.out.print("#bad#");
                        break;

                    case END:
                        System.out.print("#end#");
                        break;

                    case ASTERISK:
                        System.out.print("#asterisk#");
                        break;

                    /*
                     * case WHITE_CHAR: System.out.print("#white#"); break;
                     */

                    case LEFT_PAREN:
                        System.out.print("#leftParen#");
                        break;

                    case RIGHT_PAREN:
                        System.out.print("#rightParen#");
                        break;

                    case BIN_OP:
                        switch (scanner.getTokenIntValue())
                        {
                            case EQUAL:
                                System.out.print("#equal#");
                                break;

                            case NOT_EQUAL:
                                System.out.print("#notEqual#");
                                break;

                            case LESS_OR_EQUAL:
                                System.out.print("#lessOrEqual#");
                                break;

                            case LESS:
                                System.out.print("#less#");
                                break;

                            case GREATER_OR_EQUAL:
                                System.out.print("#greaterOrEqual#");
                                break;

                            case GREATER:
                                System.out.print("#greater#");
                                break;

                            case CONTAINS:
                                System.out.print("#contains#");
                                break;

                            case DERIVED_FROM:
                                System.out.print("#derivedFrom#");
                                break;

                            case DOES_NOT_CONTAIN:
                                System.out.print("#doesNotContain#");
                                break;

                            default:
                                throw new IllegalStateException();
                        }
                        break;

                    case STRING:
                        System.out.print("#str(" + scanner.getTokenStrValue() + ")#");
                        break;

                    case BOOL:
                        switch (scanner.getTokenIntValue())
                        {
                            case FALSE:
                                System.out.print("#false#");
                                break;

                            case TRUE:
                                System.out.print("#true#");
                                break;

                            default:
                                throw new IllegalStateException();
                        }
                        break;

                    case EXISTS:
                        System.out.print("#exists#");
                        break;

                    case AND:
                        System.out.print("#and#");
                        break;

                    case OR:
                        System.out.print("#or#");
                        break;

                    case PROPERTY:
                        System.out.print("#property(" + scanner.getTokenStrValue() + ")#");
                        break;

                    default:
                        throw new IllegalStateException();
                }
                System.out.print(' ');
            }

            System.out.println();
            System.out.println();
        }
    }

    private static final class BadTokenException extends Exception
    {
    }
}
