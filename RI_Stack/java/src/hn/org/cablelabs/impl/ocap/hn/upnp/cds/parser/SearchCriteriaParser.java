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

package org.cablelabs.impl.ocap.hn.upnp.cds.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.Expression;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.ExpressionWithTerms;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.Factor;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.FactorWithBinOp;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.FactorWithExists;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.FactorWithParens;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.Scanner;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.SearchCriteriaConstrained;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.SearchCriteriaUnconstrained;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.Term;
import org.cablelabs.impl.ocap.hn.upnp.cds.parser.internal.TermWithFactors;

public class SearchCriteriaParser
{
    private final Set validProperties;
    private final Scanner scanner;

    private int token;

    private int tokenIntValue;

    private String tokenStrValue;

    public SearchCriteriaParser(Set validProperties, String s)
    {
        this.validProperties = validProperties;
        scanner = new Scanner(s);
        scan();
    }

    public SearchCriteria parse() throws BadSearchCriteriaSyntax
    {
        return parseSearchCriteria();
    }

    private Expression parseExpression() throws BadSearchCriteriaSyntax
    {
        List terms = new ArrayList();

        terms.add(parseTerm());

        while (token == Scanner.OR)
        {
            scan();
            terms.add(parseTerm());
        }

        return new ExpressionWithTerms(terms);
    }

    private Factor parseFactor() throws BadSearchCriteriaSyntax
    {
        Factor result;

        switch (token)
        {
            case Scanner.PROPERTY:
                String property = scanner.getTokenStrValue();
                scan();
                if (! validProperties.contains(property) && ! validProperties.contains("*"))
                {
                    throw new BadSearchCriteriaSyntax("'" + property + "' is not a search capability.");
                }
                switch (token)
                {
                    case Scanner.BIN_OP:
                        int binOp = tokenIntValue;
                        scan();
                        require(Scanner.STRING);
                        String string = scanner.getTokenStrValue();
                        scan();
                        result = new FactorWithBinOp(property, binOp, string);
                        break;

                    case Scanner.EXISTS:
                        scan();
                        require(Scanner.BOOL);
                        int bool = scanner.getTokenIntValue();
                        scan();
                        result = new FactorWithExists(property, bool);
                        break;

                    default:
                        throw new BadSearchCriteriaSyntax("Expecting bin op or exists, got "
                                + Scanner.prettyNameToken(token));
                }
                break;

            case Scanner.LEFT_PAREN:
                scan();
                result = new FactorWithParens(parseExpression());
                require(Scanner.RIGHT_PAREN);
                scan();
                break;

            default:
                throw new BadSearchCriteriaSyntax("Expecting property or left paren, got "
                        + Scanner.prettyNameToken(token));
        }

        return result;
    }

    private SearchCriteria parseSearchCriteria() throws BadSearchCriteriaSyntax
    {
        SearchCriteria result;

        if (token == Scanner.ASTERISK)
        {
            scan();
            result = new SearchCriteriaUnconstrained();
        }
        else
        {
            result = new SearchCriteriaConstrained(parseExpression());
        }

        require(Scanner.END);

        return result;
    }

    private Term parseTerm() throws BadSearchCriteriaSyntax
    {
        List factors = new ArrayList();

        factors.add(parseFactor());

        while (token == Scanner.AND)
        {
            scan();
            factors.add(parseFactor());
        }

        return new TermWithFactors(factors);
    }

    private void require(int token) throws BadSearchCriteriaSyntax
    {
        if (this.token != token)
        {
            throw new BadSearchCriteriaSyntax("Expecting " + Scanner.prettyNameToken(token));
        }
    }

    private void scan()
    {
        token = scanner.nextToken();
        tokenIntValue = scanner.getTokenIntValue();
        tokenStrValue = scanner.getTokenStrValue();
    }
}
