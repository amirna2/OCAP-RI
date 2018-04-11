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

/*
 * Created on Jun 21, 2006
 */
package org.cablelabs.test.iftc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.parser.ITestCreator;

/**
 * This is a simple extension of the GroboUtils
 * {@link net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite
 * InterfaceTestSuite} that allows one to specify a filter of desired tests.
 * This filter is included on {@link #InterfaceTestSuite(String[]) constructors}
 * as a list of test method names. If no such list of names is provided (i.e.,
 * it is <code>null</code> or one of the other constructors is used), then this
 * class functions just like the super class. If a list is provided, then all
 * test methods that don't match an entry is the given filter are replaced with
 * a no-op test.
 * <p>
 * One suggested why of using this would be to have an <code>isuite()</code>
 * method on an {@link InterfaceTestCase} class that took a list of test names.
 * E.g.,
 * 
 * <pre>
 * public static InterfaceTestSuite isuite()
 * {
 *     return isuite(null);
 * }
 * 
 * public static InterfaceTestSuite isuite(String[] tests)
 * {
 *     return new InterfaceTestSuite(AppStorageManagerTest.class, tests);
 * }
 * </pre>
 * <p>
 * This class does its <i>magic</i> by overriding
 * {@link #createTestCreator(Vector)}.
 * 
 * @author Aaron Kamienski
 */
//findbugs complains about this pattern - shadowing superclass' name.
//Unfortunately, its a common pattern in the RI (so we ignore it).
public class InterfaceTestSuite extends net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite
{
    /**
     * Creates an instance of <code>InterfaceTestSuite</code>. This is
     * equivalent to
     * {@link net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite#InterfaceTestSuite()
     * super()}.
     */
    public InterfaceTestSuite()
    {
        super();
    }

    /**
     * Creates an instance of <code>InterfaceTestSuite</code>. This is
     * equivalent to
     * {@link net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite#InterfaceTestSuite(Class)
     * super(Class)}.
     * 
     * @param theClass
     *            the class under inspection
     */
    public InterfaceTestSuite(Class theClass)
    {
        super(theClass);
    }

    /**
     * Creates an instance of <code>InterfaceTestSuite</code>. This is
     * equivalent to
     * {@link net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite#InterfaceTestSuite(Class,ImplFactory)
     * super(Class,ImplFactory)}.
     * 
     * @param theClass
     *            the class under inspection
     * @param f
     *            a factory to add to this suite.
     */
    public InterfaceTestSuite(Class theClass, ImplFactory f)
    {
        super(theClass, f);
    }

    /**
     * Creates an instance of <code>InterfaceTestSuite</code>. Similar to
     * {@link #InterfaceTestSuite()}, by specifies the set of names to accept.
     * 
     * @param testNames
     *            the list of tests to run
     */
    public InterfaceTestSuite(String[] testNames)
    {
        super();
        iniz(testNames);
    }

    /**
     * Creates an instance of <code>InterfaceTestSuite</code>. Similar to
     * {@link #InterfaceTestSuite(Class)}, by specifies the set of names to
     * accept.
     * 
     * @param theClass
     *            the class under inspection
     * @param testNames
     *            the list of tests to run
     */
    public InterfaceTestSuite(Class theClass, String[] testNames)
    {
        super(theClass);
        iniz(testNames);
    }

    /**
     * Creates an instance of <code>InterfaceTestSuite</code>. Similar to
     * {@link #InterfaceTestSuite(Class,ImplFactory)}, by specifies the set of
     * names to accept.
     * 
     * @param theClass
     *            the class under inspection
     * @param f
     *            a factory to add to this suite.
     * @param testNames
     *            the list of tests to run
     */
    public InterfaceTestSuite(Class theClass, ImplFactory f, String[] testNames)
    {
        super(theClass, f);
        iniz(testNames);
    }

    /**
     * Called by constructors to initialize the filtering support.
     * 
     * @param testNames
     *            the names to keep
     */
    private void iniz(String[] testNames)
    {
        if (testNames == null) return;

        lookup = new Hashtable();
        for (int i = 0; i < testNames.length; ++i)
        {
            lookup.put(testNames[i], testNames[i]);
        }
    }

    /**
     * Overrides
     * {@link net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite#createTestCreator(java.util.Vector)}
     * , returning a special instance of <code>ITestCreator</code> whose
     * implementation of {@link ITestCreator#createTest} filters out unwanted
     * tests.
     * 
     * @return filtering instance of <code>ITestCreator</code>
     */
    protected ITestCreator createTestCreator(Vector vf)
    {
        return new CreatorDecorator(super.createTestCreator(vf));
    }

    /**
     * An instance of <code>ITestCreator</code> that decorates another instance,
     * adding the ability to filter out unwanted tests (replacing them with
     * no-op tests).
     * 
     * @author Aaron Kamienski
     */
    private class CreatorDecorator implements ITestCreator
    {
        /**
         * Creates an instance of <code>CreatorDecorator</code> that wraps the
         * given <code>ITestCreator</code> instance.
         * 
         * @param itc
         *            the original creator
         */
        CreatorDecorator(ITestCreator itc)
        {
            this.itc = itc;
        }

        /**
         * Simply calls {@link ITestCreator#canCreate(java.lang.Class)} on the
         * wrapped creator.
         */
        public boolean canCreate(Class theClass)
        {
            return itc.canCreate(theClass);
        }

        /**
         * Calls
         * {@link ITestCreator#createTest(java.lang.Class, java.lang.reflect.Method)}
         * on the wrapped creator only if the test method name is accepted. If
         * the name isn't accepted, then a no-op {@link TestCase} is returned.
         * 
         * @return the desired test or a no-op <code>Test</code> that tests
         *         nothing
         */
        public Test createTest(Class theClass, Method method) throws InstantiationException, NoSuchMethodException,
                InvocationTargetException, IllegalAccessException, ClassCastException
        {
            if (lookup == null || lookup.get(method.getName()) != null)
            {
                return itc.createTest(theClass, method);
            }
            else
            {
                return new TestCase("skipped:" + method)
                {
                    protected void runTest()
                    {
                        // Does nothing
                    }
                };
            }
        }

        /** The original, wrapped creator. */
        private ITestCreator itc;
    }

    /** The list of accepted names; null if no names are filtered. */
    private Hashtable lookup;
}
