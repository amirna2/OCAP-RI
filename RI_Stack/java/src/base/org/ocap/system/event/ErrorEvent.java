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

package org.ocap.system.event;

import org.cablelabs.impl.util.SysInfoEvent;
import org.cablelabs.impl.util.SystemEventUtil.ThrowableInfo;

/**
 * This class represents an event returned by the system when an uncaught
 * exception or implementation error is encountered, or by an application that
 * wishes to log an error or an informational event. Error event type codes are
 * defined in this class. Applications may use the error type codes in this
 * class or proprietary class codes that are understood by the network.
 * <P>
 * The class takes a @link java.lang.Throwable object parameter in one
 * constructor, but the Throwable object cannot be returned from this class due
 * to implementation and security issues. The Throwable object attributes (i.e.,
 * message and stacktrace) can be retrieved from this class by calling
 * corresponding get methods, which in-turn call the Throwable object get
 * methods. However, the implementation MUST NOT allow the Throwable object get
 * methods to block indefinitely when called and MUST NOT wait longer than 30
 * seconds for them to return.
 * </P>
 * 
 * @author Aaron Kamienski
 */
public class ErrorEvent extends SystemEvent
{
    /* Error Event type codes */

    // **** Application information event codes ****
    /**
     * Application informational event that doesn't fit into any other given
     * category.
     */
    public final static int APP_INFO_GENERAL_EVENT = BEGIN_APP_INFO_RESERVED_EVENT_TYPES;

    // **** Application recoverable error codes ****
    /**
     * Application recoverable error that doesn't fit into any other given
     * category.
     */
    public final static int APP_REC_GENERAL_ERROR = BEGIN_APP_REC_ERROR_RESERVED_EVENT_TYPES;

    /**
     * Application recoverable error - a Java Throwable caught by an exception,
     * but that can be recovered from by the application, or a Throwable that
     * was created by an application due to detection of a recoverable event.
     */
    public final static int APP_REC_JAVA_THROWABLE = BEGIN_APP_REC_ERROR_RESERVED_EVENT_TYPES + 1;

    // **** Application catastrophic error codes ****
    /**
     * Application catastropic error that doesn't fit into any other given
     * category.
     */
    public final static int APP_CAT_GENERAL_ERROR = BEGIN_APP_CAT_ERROR_RESERVED_EVENT_TYPES;

    // **** System information event codes ****
    /**
     * System informational event that doesn't fit into any other given
     * category.
     */
    public final static int SYS_INFO_GENERAL_EVENT = BEGIN_SYS_INFO_RESERVED_EVENT_TYPES;

    // **** System recoverable error codes ****
    /**
     * System error that doesn't fit into any other given category.
     */
    public final static int SYS_REC_GENERAL_ERROR = BEGIN_SYS_REC_ERROR_RESERVED_EVENT_TYPES;

    // **** System catastrophic error codes ****
    /**
     * System catastrophic error that doesn't fit into any other given category.
     */
    public final static int SYS_CAT_GENERAL_ERROR = BEGIN_SYS_CAT_ERROR_RESERVED_EVENT_TYPES;

    /**
     * Java Throwable thrown by a call made by an application but not caught by
     * the application. This event is generated by the implemenation, but
     * indicates that an application cannot continue normal operations.
     */
    public final static int SYS_CAT_JAVA_THROWABLE = BEGIN_SYS_CAT_ERROR_RESERVED_EVENT_TYPES + 1;

    /**
     * Class constructor specifying the event type code and readable message.
     * 
     * @param typeCode
     *            - Unique error type code.
     * 
     * @param message
     *            - Readable error message.
     * 
     * @throws IllegalArgumentException
     *             when called by an application and the typeCode is not in one
     *             of the following ranges: {@link #BEGIN_APP_INFO_EVENT_TYPES}
     *             to {@link #END_APP_INFO_EVENT_TYPES}, or
     *             {@link #BEGIN_APP_REC_ERROR_EVENT_TYPES} to
     *             {@link #END_APP_REC_ERROR_EVENT_TYPES}, or
     *             {@link #BEGIN_APP_CAT_ERROR_EVENT_TYPES} to
     *             {@link #END_APP_CAT_ERROR_EVENT_TYPES}.
     */
    public ErrorEvent(int typeCode, String message) throws IllegalArgumentException
    {
        super(typeCode, message);

        this.message = super.getMessage();
        this.stackTrace = null;
        this.throwableClasses = null;
        this.throwableInfo = null;
    }

    /**
     * Class constructor specifying the event type code, and throwable
     * condition. The message is derived from the Throwable object. The
     * 
     * @param typeCode
     *            - The unique error type code.
     * @param throwable
     *            - A throwable object that was generated by the implementation
     *            or an application in response to an informational or error
     *            event, or by the implementation when a call made by an
     *            application throws an exception that isn't caught by the
     *            application.
     * 
     * @throws IllegalArgumentException
     *             when called by an application and the typeCode is not in one
     *             of the following ranges: {@link #BEGIN_APP_INFO_EVENT_TYPES}
     *             to {@link #END_APP_INFO_EVENT_TYPES}, or
     *             {@link #BEGIN_APP_REC_ERROR_EVENT_TYPES} to
     *             {@link #END_APP_REC_ERROR_EVENT_TYPES}, or
     *             {@link #BEGIN_APP_CAT_ERROR_EVENT_TYPES} to
     *             {@link #END_APP_CAT_ERROR_EVENT_TYPES}.
     */
    public ErrorEvent(int typeCode, Throwable throwable) throws IllegalArgumentException
    {
        // call our parent constructor, including a temp message string that
        // will
        // never get used as we'll never call super.getMessage()
        super(typeCode, "");

        // populate ThrowableInfo container, but don't decode anything until
        // it's asked for
        this.message = null;
        this.stackTrace = null;
        this.throwableClasses = null;
        this.throwableInfo = new ThrowableInfo(throwable);
    }

    /**
     * This constructor is provided for internal use by OCAP implementations;
     * applications SHOULD NOT call it.
     * 
     * @param typeCode
     *            - The unique error type code.
     * @param message
     *            - Readable message specific to the event generator.
     * @param stacktrace
     *            - Stacktrace taken from a Throwable object or null if no
     *            Throwable used.
     * @param throwableClasses
     *            - The class hierarchy list from a Throwable object or null if
     *            no Throwable used.
     * @param date
     *            - Event date in milli-seconds from midnight January 1, 1970
     *            GMT.
     * @param appId
     *            - The Id of the application logging the event.
     * 
     * @throws SecurityException
     *             if this constructor is called by any application.
     */
    public ErrorEvent(int typeCode, String message, String stacktrace, String[] throwableClasses, long date,
            org.dvb.application.AppID appId)
    {
        super(typeCode, message, date, appId);

        this.message = super.getMessage();
        this.stackTrace = stacktrace;
        this.throwableClasses = throwableClasses;
        this.throwableInfo = null;
    }

    /**
     * Gets the stack trace from the Throwable object if a Throwable object was
     * passed to the appropriate constructor.
     * 
     * @return The stack trace from the Throwable object, or null if no
     *         Throwable object is available, or if the message cannot be
     *         extracted from the Throwable object (perhaps
     *         Throwable.printStackTrace() threw an exception or blocked).
     */
    public String getStackTrace()
    {
        return (this.throwableInfo != null) ? this.throwableInfo.getStackTrace() : this.stackTrace;
    }

    /**
     * Gets the readable message String that was passed to a constructor
     * explicitly or within a Throwable object.
     * 
     * @return The readable message, if the message cannot be extracted from the
     *         Throwable object (perhaps Throwable.getMessage() threw an
     *         exception or blocked).
     */
    public String getMessage()
    {
        return (this.throwableInfo != null) ? this.throwableInfo.getMessage() : this.message;
    }

    /**
     * Gets the class hierarchy from the Throwable object that was passed to the
     * corresponding constructor. Each String in the array will be a fully
     * qualified class name. The first will be the full class name (with package
     * name) of the Throwable object passed to this class. Each subsequent
     * String shall contain the name of a super class up to but not including
     * java.lang.Object.
     * 
     * @return The stack trace from the Throwable object, or null if no
     *         Throwable object is available.
     */
    public String[] getThrowableClasses()
    {
        return (this.throwableInfo != null) ? this.throwableInfo.getClassHierarchy() : this.throwableClasses;
    }

    /**
     * Returns the name of this event. Used by the implementation of
     * {@link SystemEvent#privToString}.
     * 
     * @return the name of this event.
     * @see SystemEvent#privToString
     */
    String privNameToString()
    {
        Class cl = getClass();
        return (ErrorEvent.class == cl) // Special case for this class
        ? "ErrorEvent"
                : ((SysInfoEvent.class == cl) // Special case for impl-specific
                                              // subclass
                ? "SysInfoEvent"
                        : super.privNameToString()); // Handle app-specific
                                                     // subclasses w/ full-name
    }

    /**
     * Returns the message of this event. Used by the implementation of
     * {@link SystemEvent#privToString}.
     * 
     * @return the message of this event.
     * @see SystemEvent#privToString
     */
    String privMsgToString()
    {
        // string format: "<classname>: <msg>\n<stacktrace>"

        // get Throwable message
        String privStr = super.privMsgToString();

        // prepend w/ classname if available
        String[] hier = this.getThrowableClasses();
        if (hier != null)
        {
            privStr = hier[0] + ": " + privStr;
        }

        // append w/ stacktrace if available
        String strace = this.getStackTrace();
        if (strace != null)
        {
            privStr = privStr + "\n" + strace;
        }

        // return formatted string
        return privStr;
    }

    /**
     * Container for getting information out of the Throwable object.
     */
    private final ThrowableInfo throwableInfo;

    /**
     * The message string from the Throwable object or null if none is
     * available.
     */
    private final String message;

    /**
     * The stack trace from the Throwable object or null if none is available.
     */
    private final String stackTrace;

    /**
     * The class hierarchy of the throwable object that was passed to the
     * constructor.
     */
    private final String[] throwableClasses;
}
