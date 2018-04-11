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

package org.cablelabs.impl.debug;

import org.apache.log4j.Logger;

import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Implmentation of assertion-checking methods.
 * 
 * @author schoonma
 * 
 */
public class Assert implements Asserting
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(Assert.class);

    /**
     * This class is the {@link RuntimeException} that is thrown if the
     * assertion fails and exceptions are enabled.
     */
    private static class AssertionFailureException extends RuntimeException
    {
        private AssertionFailureException()
        {
            super();
        }

        private AssertionFailureException(String msg)
        {
            super(msg);
        }
    }

    //
    // The following static fields allow the behavior of the assertion methods
    // to be configurable. Default values are specified, but can be overridden
    // via OCAP.assert.* properties in MPEENV.INI.
    //

    /**
     * If <code>true</code>, use log4j to log a message if the assertion fails.
     * Default value is <code>false</code>. Override the default by setting the
     * <code>OCAP.assert.log</code> property in <code>final.properties</code>.
     */
    private static final boolean DO_LOG = "true".equals(MPEEnv.getEnv("OCAP.assert.log", "false"));

    /**
     * If <code>true</code>, log the assertion failure as a fatal system event
     * via {@link SystemEventUtil#logCatastrophicError(Throwable)}. Default
     * value is <code>true</code>. Override the default by setting the
     * <code>OCAP.assert.log</code> property in <code>final.properties</code>.
     */
    private static final boolean DO_EVENT = "true".equals(MPEEnv.getEnv("OCAP.assert.event", "true"));

    /**
     * If <code>true</code>, throw a {@link AssertionFailureException} if the
     * assertion fails. Default value is <code>false</code>. Override the default
     * by setting the <code>OCAP.assert.log</code> property in
     * <code>final.properties</code>.
     */
    private static final boolean DO_THROW = "true".equals(MPEEnv.getEnv("OCAP.assert.throw", "false"));

    /**
     * Assert the an expression is <code>true</code>. If it is not, perform
     * assert failure actions, as defined by the configuration fields
     * {@link #DO_LOG}, {@link #DO_EVENT}, and {@link #DO_THROW}.
     * 
     * @param expr
     *            The expression to be evaluated. If it is true, the assertion
     *            succeeds; otherwise, it fails.
     */
    public static final void condition(boolean expr)
    {
        if (!expr) assertionFailure(new AssertionFailureException());
    }

    /**
     * This is the same as {@link #assertion(boolean)}, except that an
     * additional descriptive message can be included with the assertion failure
     * message.
     * 
     * @param expr
     *            The boolean expression to be evaluated.
     * @param msg
     *            A string that is added to the assertion failure message.
     */
    public static final void condition(boolean expr, String msg)
    {
        if (!expr) assertionFailure(new AssertionFailureException(msg));
    }

    //
    // These special forms of condition are used for pre-conditions (things
    // that must be true at the start of a method or block) and post-conditions
    // (things
    // that must be true on exit from a method or block).
    //

    /**
     * Same as {@link #condition(boolean)}, except that the expression being
     * evaluated is a pre-condition&mdash;a condition that must be
     * <code>true</code> upon entry to a method or block of code.
     * 
     * @param expr
     *            The pre-condition to evaluate.
     */
    public static final void preCondition(boolean expr)
    {
        condition(expr, "PRECONDITION FAILURE");
    }

    /**
     * Same as {@link #condition(boolean, String)}, except that the expression
     * being evaluated is a pre-condition&mdash;a condition that must be
     * <code>true</code> upon entry to a method or block of code.
     * 
     * @param expr
     *            The pre-condition to evaluate.
     */
    public static final void preCondition(boolean expr, String msg)
    {
        condition(expr, "PRECONDITION FAILURE: " + msg);
    }

    /**
     * Same as {@link #condition(boolean)}, except that the expression being
     * evaluated is a post-condition&mdash;a condition that must be
     * <code>true</code> upon exit from a method or block of code.
     * 
     * @param expr
     *            The post-condition to evaluate.
     */
    public static final void postCondition(boolean expr)
    {
        condition(expr, "POSTCONDITION FAILURE");
    }

    /**
     * Same as {@link #condition(boolean, String)}, except that the expression
     * being evaluated is a post-condition&mdash;a condition that must be
     * <code>true</code> upon exit from a method or block of code.
     * 
     * @param expr
     *            The post-condition to evaluate.
     */
    public static final void postCondition(boolean expr, String msg)
    {
        condition(expr, "POSTCONDITION FAILURE: " + msg);
    }
    
    /**
     * Assert the supplied lock is held. If it is not, perform
     * assert failure actions, as defined by the configuration fields
     * {@link #DO_LOG}, {@link #DO_EVENT}, and {@link #DO_THROW}.
     * 
     * @param lock
     *            The lock to check.
     */
    public static final void lockHeld(final Object lock)
    {
        if (!ASSERTING) return;

        if (lock == null)
        {
            assertionFailure(new AssertionFailureException("Null lock"));
        }
        
        if (!Thread.holdsLock(lock))
        {
            assertionFailure(new AssertionFailureException("Lock " + lock.getClass().getName() + "@0x" + Integer.toHexString(lock.hashCode()) + " is not held" ));
        }
    }

    /**
     * Assert the supplied lock is not held. If it is, perform
     * assert failure actions, as defined by the configuration fields
     * {@link #DO_LOG}, {@link #DO_EVENT}, and {@link #DO_THROW}.
     * 
     * @param lock
     *            The lock to check.
     */
    public static final void lockNotHeld(final Object lock)
    {
        if (!ASSERTING) return;

        if (lock == null)
        {
            assertionFailure(new AssertionFailureException("Null lock"));
        }
        
        if (Thread.holdsLock(lock))
        {
            assertionFailure(new AssertionFailureException("Lock " + lock.getClass().getName() + "@0x" + Integer.toHexString(lock.hashCode()) + " is held" ));
        }
    }
    
    /**
     * This helper method is called by various 'condition' methods if an
     * assertion fails. It examines the configuration fields {@link #DO_LOG},
     * {@link #DO_EVENT}, and {@link #DO_THROW} to determine what to do.
     * 
     * @param x
     *            An {@link AssertionFailureException} that was constructed by
     *            the caller for the assertion failure.
     */
    private static final void assertionFailure(AssertionFailureException x)
    {
        if (!ASSERTING) return;

        if (DO_LOG)
        {
            if (log.isErrorEnabled())
            {
                log.error(x);
            }
        }

        if (DO_EVENT) SystemEventUtil.logCatastrophicError(x);

        if (DO_THROW) throw x;
    }
}
