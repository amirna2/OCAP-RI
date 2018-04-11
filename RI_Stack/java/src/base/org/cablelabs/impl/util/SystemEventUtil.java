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

package org.cablelabs.impl.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Vector;

import org.dvb.application.AppID;
import org.ocap.system.event.ErrorEvent;
import org.ocap.system.event.SystemEvent;
import org.ocap.system.event.SystemEventManager;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

/**
 * <code>SystemEventUtil</code> is a utility class that aids in the logging of
 * {@link SystemEvent}s by the system. Specifically, this class provides
 * convenience methods for logging various types of errors as well as general
 * informational events with the {@link SystemEventManager}.
 * <p>
 * This simplifies the code involved with the generation and logging of such
 * events. Most notably, removing the need to include
 * <code>PrivilegedAction</code> code in the caller (which is necessary when
 * generating non-application events).
 * <p>
 * Note that the {@link #getStackTrace} and {@link #getThrowableClasses} methods
 * are meant to be used internally by this class as well as by the
 * implementation of {@link ErrorEvent}.
 * 
 * @author Aaron Kamienski
 */
public abstract class SystemEventUtil
{
    /**
     * Not publicly instantiable.
     */
    private SystemEventUtil()
    {
        // Not publiclly instantiable
    }

    /**
     * Log an uncaught exception thrown during the execution of application
     * code. The application error is logged against the application represented
     * by the current caller context.
     * <p>
     * This logs an instance of
     * <code>ErrorEvent({@link ErrorEvent#SYS_CAT_JAVA_THROWABLE SYS_CAT_JAVA_THROWABLE}, e)</code>.
     * <p>
     * Example:
     * 
     * <pre>
     * try
     * {
     *     cc.runInContextSync(new Runnable() {
     *         public void run()
     *         {
     *             ...
     *         }
     *     });
     * }
     * catch(InvocationTargetException e)
     * {
     *     ErrorUtil.logUncaughtException(e.getException());
     * }
     * </pre>
     * 
     * @param e
     *            uncaught application exception to be logged
     */
    public static void logUncaughtException(Throwable e)
    {
        log(newErrorEvent(ErrorEvent.SYS_CAT_JAVA_THROWABLE, e));
    }

    /**
     * Log an uncaught exception thrown during the execution of application
     * code. The application error is logged against the application represented
     * by the given <code>AppID</code>
     * 
     * @param e
     *            uncaught application exception to be logged
     * @param id
     *            <code>AppID</code> against which the exception is logged;
     *            <code>null</code> if there is none
     */
    /*
     * public static void logUncaughtException(Throwable e, AppID id) {
     * log(newErrorEvent(ErrorEvent.SYS_CAT_JAVA_THROWABLE, e, id)); }
     */

    /**
     * Log an uncaught exception thrown during the execution of application
     * code. The application error is logged against the application represented
     * by the given <code>AppID</code>
     * 
     * @param e
     *            uncaught application exception to be logged
     * @param cc
     *            <code>CallerContext</code> against which the exception is
     *            logged; <code>null</code> if there is none
     */
    public static void logUncaughtException(Throwable e, CallerContext cc)
    {
        log(newErrorEvent(ErrorEvent.SYS_CAT_JAVA_THROWABLE, e, (AppID) ((cc == null) ? null
                : cc.get(CallerContext.APP_ID))));
    }

    /**
     * Log an unexpected, but otherwise recoverable error (represented by the
     * given <code>Throwable</code>) within the stack.
     * <p>
     * This logs an instance of
     * <code>ErrorEvent({@link ErrorEvent#SYS_REC_GENERAL_ERROR SYS_REC_GENERAL_ERROR}, e)</code>.
     * 
     * @param e
     *            unexpected exception caught within the stack
     */
    public static void logRecoverableError(Throwable e)
    {
        log(newErrorEvent(ErrorEvent.SYS_REC_GENERAL_ERROR, e));
    }

    /**
     * Log an unexpected, but otherwise recoverable error (represented by the
     * given <code>Throwable</code>) within the stack.
     * <p>
     * This logs an instance of
     * <code>ErrorEvent({@link ErrorEvent#SYS_REC_GENERAL_ERROR SYS_REC_GENERAL_ERROR}, e)</code>
     * , with the specified message.
     * 
     * @param message
     *            message describing the conditions of the error
     * @param e
     *            unexpected exception caught within the stack
     */
    public static void logRecoverableError(String message, Throwable e)
    {
        log(newErrorEvent(ErrorEvent.SYS_REC_GENERAL_ERROR, message, e));
    }

    /**
     * Log an unexpected exception thrown during the execution of the stack.
     * This method should be used when an unexpected, and possibly unrecoverable
     * exception is encountered.
     * <p>
     * This logs an instance of
     * <code>ErrorEvent({@link ErrorEvent#SYS_CAT_GENERAL_ERROR SYS_CAT_GENERAL_ERROR}, e)</code>.
     * 
     * @param e
     *            unexpected exception caught within the stack
     */
    public static void logCatastrophicError(Throwable e)
    {
        log(newErrorEvent(ErrorEvent.SYS_CAT_GENERAL_ERROR, e));
    }

    /**
     * Log an unexpected exception thrown during the execution of the stack.
     * This method should be used when an unexpected, and possibly unrecoverable
     * exception is encountered.
     * <p>
     * This logs an instance of
     * <code>ErrorEvent({@link ErrorEvent#SYS_CAT_GENERAL_ERROR SYS_CAT_GENERAL_ERROR}, e)</code>
     * , with the specified message.
     * 
     * @param message
     *            message describing the conditions of the error
     * @param e
     *            unexpected exception caught within the stack
     */
    public static void logCatastrophicError(String message, Throwable e)
    {
        log(newErrorEvent(ErrorEvent.SYS_CAT_GENERAL_ERROR, message, e));
    }

    /**
     * Log an informational event. This method should be used to log significant
     * milestones within the operation of the stack.
     * <p>
     * This logs an instance of
     * <code>{@link SysInfoEvent#SysInfoEvent(int,String) SysInfoEvent}({@link 
     * ErrorEvent#SYS_INFO_GENERAL_EVENT SYS_INFO_GENERAL_EVENT}, msg)</code>.
     * 
     * @param msg
     *            message describing the milestone
     */
    public static void logEvent(String msg)
    {
        log(newInfoEvent(ErrorEvent.SYS_INFO_GENERAL_EVENT, msg));
    }

    /**
     * Log an exception thrown during execution. This method should be used to
     * log an unexpected (catastrophic or recoverable) exception thrown during
     * execution of the stack or an application.
     * <p>
     * This logs an instance of <code>ErrorEvent(type, e)</code>.
     * <p>
     * Note that it is the responsibility of the caller to ensure that
     * <i>type</i> is a valid type.
     * 
     * @param type
     *            type of the event to log
     * @param e
     *            unexpected exception caught within the stack
     */
    public static void logException(int type, Throwable e)
    {
        log(newErrorEvent(type, e));
    }

    /**
     * Log an informational event. This method should be used to log significant
     * milestones within the operation of the stack.
     * <p>
     * This logs an instance of {@link SysInfoEvent#SysInfoEvent(int, String)
     * SysInfoEvent(type, msg)}.
     * <p>
     * Note that it is the responsibility of the caller to ensure that
     * <i>type</i> is a valid type.
     * 
     * @param type
     *            type of the event to log
     * @param msg
     *            message describing the milestone
     * 
     * @see #logEvent(String)
     */
    public static void logEvent(int type, String msg)
    {
        log(newInfoEvent(type, msg));
    }

    /**
     * Creates a new <code>ErrorEvent</code> for the given <i>type</i> and
     * <i>throwable</i>. The <code>ErrorEvent</code> is created within a
     * privileged block, allowing the system to create non-application
     * <code>ErrorEvent</code>s.
     * 
     * @param type
     *            error event type
     * @param e
     *            throwable
     * @return a new instance of <code>ErrorEvent</code>
     */
    private static ErrorEvent newErrorEvent(final int type, final Throwable e)
    {
        return (ErrorEvent) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new ErrorEvent(type, e);
            }
        });
    }

    /**
     * Creates a new <code>ErrorEvent</code> for the given <i>type</i>,
     * <i>message</i>, and <i>throwable</i>. The <code>ErrorEvent</code> is
     * created within a privileged block, allowing the system to create
     * non-application <code>ErrorEvent</code>s.
     * 
     * @param type
     *            error event type
     * @param message
     *            additional message
     * @param e
     *            throwable
     * @return a new instance of <code>ErrorEvent</code>
     */
    private static ErrorEvent newErrorEvent(final int type, final String message, final Throwable e)
    {
        return (ErrorEvent) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                ThrowableInfo ti = new SystemEventUtil.ThrowableInfo(e);
                return new ErrorEvent(type, message + " - "
                        + (ti.getClassHierarchy() != null ? ti.getClassHierarchy()[0] + ": " : "") + ti.getMessage(),
                        ti.getStackTrace(), ti.getClassHierarchy(), System.currentTimeMillis(), getCurrentAppID());
            }
        });
    }

    /**
     * Creates a new <code>ErrorEvent</code> for the given <i>type</i>,
     * <i>throwable</i>, and <code>AppID</code>. The <code>ErrorEvent</code> is
     * created within a privileged block, allowing the system to create
     * non-application <code>ErrorEvent</code>s.
     * 
     * @param type
     *            error event type
     * @param e
     *            throwable
     * @param id
     *            application id or <code>null</code> if there is none
     * @return a new instance of <code>ErrorEvent</code>
     */
    private static ErrorEvent newErrorEvent(final int type, final Throwable e, final AppID id)
    {
        return (ErrorEvent) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                ThrowableInfo ti = new SystemEventUtil.ThrowableInfo(e);
                return new ErrorEvent(type, ti.getMessage(), ti.getStackTrace(), ti.getClassHierarchy(),
                        System.currentTimeMillis(), id);
            }
        });
    }

    /**
     * Creates a new <code>ErrorEvent</code> for the given <i>type</i> and
     * <i>msg</i>. The <code>ErrorEvent</code> is created within a privileged
     * block, allowing the system to create non-application
     * <code>ErrorEvent</code>s.
     * 
     * @param type
     *            error event type
     * @param msg
     *            error event message
     * @return a new instance of <code>ErrorEvent</code>
     */
    private static ErrorEvent newInfoEvent(final int type, final String msg)
    {
        return (ErrorEvent) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new SysInfoEvent(type, msg);
            }
        });
    }

    /**
     * Determines the AppID of the current application.
     * 
     * @return the AppID of the current application
     */
    private static org.dvb.application.AppID getCurrentAppID()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext ctx = ccm.getCurrentContext();

        return (org.dvb.application.AppID) ctx.get(CallerContext.APP_ID);
    }

    /**
     * Logs the given <code>ErrorEvent</code> with the
     * <code>SystemEventManager</code>. This is simply a convenience method for:
     * 
     * <pre>
     * SystemEventManager mgr = SystemEventManager.getInstance();
     * mgr.log(e);
     * </pre>
     * 
     * @param e
     *            the <code>ErrorEvent</code> to log
     */
    private static void log(ErrorEvent e)
    {
        mgr.log(e);
    }

    /** Static reference to <code>SystemEventManager</code> singleton. */
    private static SystemEventManager mgr = SystemEventManager.getInstance();

    public static class ThrowableInfo
    {
        private Throwable throwable;

        private boolean triedMessage = false;

        private boolean hasMessage = false;

        private String message = null;

        private boolean triedStackTrace = false;

        private String stackTrace = null;

        private boolean triedClassHierarchy = false;

        private String[] classHierarchy = null;

        /**
         * Constructor for the ThrowableInfo utility class.
         * <p>
         * Note that this method is generally meant to be used internally by
         * this class or the implementation of {@link ErrorEvent}.
         * 
         * @param throwable
         *            - A throwable object that was generated by the
         *            implementation or an application in response to an
         *            informational or error event, or by the implementation
         *            when a call made by an application throws an exception
         *            that isn't caught by the application.
         */
        public ThrowableInfo(Throwable throwable)
        {
            this.throwable = throwable;

        }

        /**
         * Generates the message from the Throwable.
         * <p>
         * Note that this method is generally meant to be used internally by
         * this class or the implementation of {@link ErrorEvent}.
         * 
         * @return the message the given throwable as a String, or null if the
         *         Throwable throws an error.
         */
        public String getMessage()
        {
            if (this.triedMessage == false)
            {
                this.triedMessage = true;

                // TODO: protect this user-defined call against never returning
                // eg: call this asynchronously and start a timeout
                // note: only do this for non-stack instigated Throwables
                // ie: classloader is !(null or system-classloader)

                // protect against badly acting (user-defined) Throwable
                // implementations
                try
                {
                    this.message = throwable.getMessage();
                    this.hasMessage = true;
                }
                catch (Throwable t)
                {
                    this.message = throwable.getClass().getName() + ".getMessage() threw the error " + t.toString();
                    this.hasMessage = false;
                }
            }

            return this.message;
        }

        /**
         * Generates the stack trace from the Throwable as a String.
         * <p>
         * Note that this method is generally meant to be used internally by
         * this class or the implementation of {@link ErrorEvent}.
         * 
         * @return the stack trace for the given throwable as a String, or null
         *         if the Throwable did not return a stack trace or did not
         *         return a valid message.
         */
        public String getStackTrace()
        {
            if (this.triedStackTrace == false)
            {
                this.triedStackTrace = true;

                // insure that this Throwable has a valid message
                if (this.hasValidMessage() == true)
                {
                    // protect against badly acting (user-defined) Throwable
                    // implementations
                    try
                    {
                        // TODO: protect this user-defined call against never
                        // returning
                        // eg: call this asynchronously and start a timeout
                        // note: only do this for non-stack instigated
                        // Throwables
                        // ie: classloader is !(null or system-classloader)

                        // Get stackTrace
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        PrintStream out = new PrintStream(bos);
                        throwable.printStackTrace(out);
                        out.flush();

                        this.stackTrace = bos.toString();
                    }
                    catch (Throwable t)
                    {
                        // keep default stackTrace upon errors (ie: null)
                    }
                }
            }

            return this.stackTrace;
        }

        /**
         * Generates the class hierarchy from the Throwable as a string.
         * <p>
         * Note that this method is generally meant to be used internally by
         * this class or the implementation of {@link ErrorEvent}.
         * 
         * @return the class hierarchy for the given throwable as a string, or
         *         null if the Throwable did not return a class or did not
         *         return a valid message.
         */
        public String[] getClassHierarchy()
        {
            if (this.triedClassHierarchy == false)
            {
                this.triedClassHierarchy = true;

                // insure that this Throwable has a valid message
                if (this.hasValidMessage() == true)
                {
                    try
                    {
                        // Get hierarchy
                        Vector classes = new Vector();
                        Class clazz;
                        for (clazz = throwable.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass())
                        {
                            classes.addElement(clazz.getName());
                        }
                        String[] throwableClasses = new String[classes.size()];
                        classes.copyInto(throwableClasses);

                        this.classHierarchy = throwableClasses;
                    }
                    catch (Throwable t)
                    {
                        // keep default class hierarchy upon errors (ie: null)
                    }
                }
            }

            return this.classHierarchy;
        }

        private boolean hasValidMessage()
        {
            if (this.triedMessage == false)
            {
                this.getMessage();
            }
            return this.hasMessage;
        }
    }

}
