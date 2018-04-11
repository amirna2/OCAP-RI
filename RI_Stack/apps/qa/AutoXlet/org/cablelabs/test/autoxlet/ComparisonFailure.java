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

package org.cablelabs.test.autoxlet;

/**
 * An assertion failure that is thrown as a result of errors when comparing an
 * expected and an actual value
 */
public class ComparisonFailure extends AssertionFailedError
{

    /**
     * Constructs a comparison failure.
     * 
     * @param message
     *            the identifying message or null
     * @param expected
     *            the expected string value
     * @param actual
     *            the actual string value
     */
    public ComparisonFailure(String message, String expected, String actual)
    {
        super(message);
        fExpected = expected;
        fActual = actual;
    }

    /**
     * Returns "..." in place of common prefix and "..." in place of common
     * suffix between expected and actual.
     * 
     * @see java.lang.Throwable#getMessage()
     */
    public String getMessage()
    {
        if (fExpected == null || fActual == null) return Test.format(super.getMessage(), fExpected, fActual);

        int end = Math.min(fExpected.length(), fActual.length());

        int i;
        for (i = 0; i < end; i++)
        {
            if (fExpected.charAt(i) != fActual.charAt(i)) break;
        }

        int j = fExpected.length() - 1;
        int k = fActual.length() - 1;
        for (; k >= i && j >= i; k--, j--)
        {
            if (fExpected.charAt(j) != fActual.charAt(k)) break;
        }

        String actual, expected;

        // equal strings
        if (j < i && k < i)
        {
            expected = fExpected;
            actual = fActual;
        }
        else
        {
            expected = fExpected.substring(i, j + 1);
            actual = fActual.substring(i, k + 1);
            if (i <= end && i > 0)
            {
                expected = "..." + expected;
                actual = "..." + actual;
            }

            if (j < fExpected.length() - 1) expected = expected + "...";
            if (k < fActual.length() - 1) actual = actual + "...";
        }
        return Test.format(super.getMessage(), expected, actual);
    }

    private String fExpected;

    private String fActual;
}
