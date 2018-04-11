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
 * The Test object provides a set of JUnit-like assert statements along with a
 * <code>TestResult</code> object for collecting test data. Each call to an
 * assert statement with increment the total test count, but only test failures
 * are stored in the test results.
 * 
 * @author Greg Rutz
 */
public class Test
{
    /**
     * Construct a new <code>Test</code> object with an empty test result set
     */
    public Test()
    {
        result = new TestResult();
    }

    /**
     * Get the test results for this test
     * 
     * @return the test results
     */
    public TestResult getTestResult()
    {
        return result;
    }

    /**
     * Asserts that a condition is true. If it isn't it adds an
     * AssertionFailedError with the given message to the test results.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertTrue(String message, boolean condition)
    {
        if (condition)
        {
            result.runTest();
            return true;
        }

        fail(message);
        return false;
    }

    /**
     * Asserts that a condition is true. If it isn't it throws an
     * AssertionFailedError.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertTrue(boolean condition)
    {
        return assertTrue(null, condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws an
     * AssertionFailedError with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertFalse(String message, boolean condition)
    {
        return assertTrue(message, !condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws an
     * AssertionFailedError.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertFalse(boolean condition)
    {
        return assertFalse(null, condition);
    }

    /**
     * Fails a test with the given message.
     */
    public void fail(String message)
    {
        result.runTest();
        try
        {
            fakeThrowAssertionFailedError(message);
        }
        catch (AssertionFailedError e)
        {
            result.addFailure(e);
        }
    }

    /**
     * Fails a test with no message.
     */
    public void fail()
    {
        fail(null);
    }

    /**
     * Asserts that two objects are equal. If they are not an
     * AssertionFailedError is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, Object expected, Object actual)
    {
        if (expected == null && actual == null)
        {
            result.runTest();
            return true;
        }
        if (expected != null && expected.equals(actual))
        {
            result.runTest();
            return true;
        }
        failNotEquals(message, expected, actual);
        return false;
    }

    /**
     * Asserts that two objects are equal. If they are not an
     * AssertionFailedError is added.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(Object expected, Object actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two Strings are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, String expected, String actual)
    {
        result.runTest();

        if (expected == null && actual == null) return true;
        if (expected != null && expected.equals(actual)) return true;

        try
        {
            fakeThrowComparisonFailure(message, expected, actual);
        }
        catch (ComparisonFailure e)
        {
            result.addFailure(e);
        }
        return false;
    }

    /**
     * Asserts that two Strings are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String expected, String actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two doubles are equal concerning a delta. If they are not an
     * AssertionFailedError is added with the given message. If the expected
     * value is infinity then the delta value is ignored.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, double expected, double actual, double delta)
    {
        // handle infinity specially since subtracting to infinite values gives
        // NaN and the
        // the following test fails
        if (Double.isInfinite(expected))
        {
            if (expected != actual)
            {
                failNotEquals(message, new Double(expected), new Double(actual));
                return false;
            }
        }
        else if (!(Math.abs(expected - actual) <= delta)) // Because comparison
                                                          // with NaN always
                                                          // returns false
        {
            failNotEquals(message, new Double(expected), new Double(actual));
            return false;
        }
        return true;
    }

    /**
     * Asserts that two doubles are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(double expected, double actual, double delta)
    {
        return assertEquals(null, expected, actual, delta);
    }

    /**
     * Asserts that two floats are equal concerning a delta. If they are not an
     * AssertionFailedError is added with the given message. If the expected
     * value is infinity then the delta value is ignored.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, float expected, float actual, float delta)
    {
        // handle infinity specially since subtracting to infinite values gives
        // NaN and the
        // the following test fails
        if (Float.isInfinite(expected))
        {
            if (expected != actual)
            {
                failNotEquals(message, new Float(expected), new Float(actual));
                return false;
            }
        }
        else if (!(Math.abs(expected - actual) <= delta))
        {
            failNotEquals(message, new Float(expected), new Float(actual));
            return false;
        }
        return true;
    }

    /**
     * Asserts that two floats are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(float expected, float actual, float delta)
    {
        return assertEquals(null, expected, actual, delta);
    }

    /**
     * Asserts that two longs are equal. If they are not an AssertionFailedError
     * is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, long expected, long actual)
    {
        return assertEquals(message, new Long(expected), new Long(actual));
    }

    /**
     * Asserts that two longs are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(long expected, long actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two booleans are equal. If they are not an
     * AssertionFailedError is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, boolean expected, boolean actual)
    {
        return assertEquals(message, new Boolean(expected), new Boolean(actual));
    }

    /**
     * Asserts that two booleans are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(boolean expected, boolean actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two bytes are equal. If they are not an AssertionFailedError
     * is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, byte expected, byte actual)
    {
        return assertEquals(message, new Byte(expected), new Byte(actual));
    }

    /**
     * Asserts that two bytes are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(byte expected, byte actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two chars are equal. If they are not an AssertionFailedError
     * is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, char expected, char actual)
    {
        return assertEquals(message, new Character(expected), new Character(actual));
    }

    /**
     * Asserts that two chars are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(char expected, char actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two shorts are equal. If they are not an
     * AssertionFailedError is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, short expected, short actual)
    {
        return assertEquals(message, new Short(expected), new Short(actual));
    }

    /**
     * Asserts that two shorts are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(short expected, short actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two ints are equal. If they are not an AssertionFailedError
     * is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(String message, int expected, int actual)
    {
        return assertEquals(message, new Integer(expected), new Integer(actual));
    }

    /**
     * Asserts that two ints are equal.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertEquals(int expected, int actual)
    {
        return assertEquals(null, expected, actual);
    }

    /**
     * Asserts that an object isn't null.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertNotNull(Object object)
    {
        return assertNotNull(null, object);
    }

    /**
     * Asserts that an object isn't null. If it is an AssertionFailedError is
     * added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertNotNull(String message, Object object)
    {
        return assertTrue(message, object != null);
    }

    /**
     * Asserts that an object is null.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertNull(Object object)
    {
        return assertNull(null, object);
    }

    /**
     * Asserts that an object is null. If it is not an AssertionFailedError is
     * added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertNull(String message, Object object)
    {
        return assertTrue(message, object == null);
    }

    /**
     * Asserts that two objects refer to the same object. If they are not an
     * AssertionFailedError is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertSame(String message, Object expected, Object actual)
    {
        if (expected == actual)
        {
            result.runTest();
            return true;
        }
        failNotSame(message, expected, actual);
        return false;
    }

    /**
     * Asserts that two objects refer to the same object. If they are not the
     * same an AssertionFailedError is added.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertSame(Object expected, Object actual)
    {
        return assertSame(null, expected, actual);
    }

    /**
     * Asserts that two objects refer to the same object. If they are not an
     * AssertionFailedError is added with the given message.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertNotSame(String message, Object expected, Object actual)
    {
        if (expected != actual)
        {
            result.runTest();
            return true;
        }
        failSame(message);
        return false;
    }

    /**
     * Asserts that two objects refer to the same object. If they are not the
     * same an AssertionFailedError is added.
     * 
     * @return true if test passed, false if test failed
     */
    public boolean assertNotSame(Object expected, Object actual)
    {
        return assertNotSame(null, expected, actual);
    }

    /**
     * 
	 */
    private void failSame(String message)
    {
        String formatted = "";
        if (message != null) formatted = message + " ";
        fail(formatted + "expected not same");
    }

    private void failNotSame(String message, Object expected, Object actual)
    {
        String formatted = "";
        if (message != null) formatted = message + " ";
        fail(formatted + "expected same:<" + expected + "> was not:<" + actual + ">");
    }

    private void failNotEquals(String message, Object expected, Object actual)
    {
        fail(format(message, expected, actual));
    }

    public static String format(String message, Object expected, Object actual)
    {
        String formatted = "";
        if (message != null) formatted = message + " ";
        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }

    private void fakeThrowAssertionFailedError(String message) throws AssertionFailedError
    {
        throw new AssertionFailedError(message);
    }

    private void fakeThrowComparisonFailure(String message, String expected, String actual) throws ComparisonFailure
    {
        throw new ComparisonFailure(message, expected, actual);
    }

    private TestResult result;
}
