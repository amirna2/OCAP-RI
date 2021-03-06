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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.SecurityUtil;

import java.text.DateFormat;

import org.dvb.application.AppID;

/**
 * This class is the basis for system event messages. Applications cannot create
 * this class directly, they must create one of the defined subclasses instead.
 * <p>
 * The event type code is defined with ranges reserved for specific types of
 * events. Ranges defined for the implementation using "SYS" cannot be used by
 * applications.
 * <p>
 * Ranges defined as "reserved" will be allocated by OCAP (or other future
 * standards). Applications and OCAP implementations SHOULD NOT use these values
 * until their meaning is standardised.
 * <p>
 * Values in a SYS range that are not reserved may be used by OCAP implementors
 * - their meaning is implementation-dependent.
 * <p>
 * Values in an APP range that are not reserved may be used by OCAP applications
 * - their meaning is application-dependent. The {@link #getAppID()} method may
 * be useful to disambiguate these codes.
 * <p>
 * Informational events can be used for any information desired. Recoverable
 * error events do not prevent an application or the implementation from
 * continued execution. Catastrophic events generated by or on behalf of an
 * application indicate that the application cannot continue execution and will
 * be a cause for self-destruction. Reboot events generated by the
 * implementation indicate that the implementation cannot continue execution and
 * a system generated reboot is imminent.
 * 
 * @author Aaron Kamienski
 */
public class SystemEvent
{

    /**
     * Start of range for system generated informational events.
     */
    public static final int BEGIN_SYS_INFO_EVENT_TYPES = 0x00000000;

    /**
     * Start of range reserved for system generated informational events defined
     * by OCAP. This reserved range ends with {@link #END_SYS_INFO_EVENT_TYPES}.
     */
    public static final int BEGIN_SYS_INFO_RESERVED_EVENT_TYPES = 0x04000000;

    /**
     * End of range for system generated informational events.
     */
    public static final int END_SYS_INFO_EVENT_TYPES = 0x07FFFFFF;

    /**
     * Start of range for application generated informational events.
     */
    public static final int BEGIN_APP_INFO_EVENT_TYPES = 0x08000000;

    /**
     * Start of range reserved for application generated informational events
     * defined by OCAP. This reserved range ends with
     * {@link #END_APP_INFO_EVENT_TYPES}.
     */
    public static final int BEGIN_APP_INFO_RESERVED_EVENT_TYPES = 0x0C000000;

    /**
     * End of range for application generated informational events.
     */
    public static final int END_APP_INFO_EVENT_TYPES = 0x1FFFFFFF;

    /**
     * Start of range for system generated recoverable error events.
     * <p>
     * These events may refer to a specific application, which will be
     * identified by the {@link #getAppID()} method, or may be internal system
     * errors, in which case <code>getAppID()</code> will return null.
     */
    public static final int BEGIN_SYS_REC_ERROR_EVENT_TYPES = 0x20000000;

    /**
     * Start of range reserved for system generated recoverable error events
     * defined by OCAP. This reserved range ends with
     * {@link #END_SYS_REC_ERROR_EVENT_TYPES}.
     */
    public static final int BEGIN_SYS_REC_ERROR_RESERVED_EVENT_TYPES = 0x24000000;

    /**
     * End of range for system generated recoverable error events.
     */
    public static final int END_SYS_REC_ERROR_EVENT_TYPES = 0x27FFFFFF;

    /**
     * Start of range for application generated recoverable error events.
     * <p>
     * This type of error is intended to indicate that something went wrong
     * (e.g. a data file could not be loaded or a system call failed), but the
     * application is designed to handle the error gracefully so does not need
     * to terminate.
     */
    public static final int BEGIN_APP_REC_ERROR_EVENT_TYPES = 0x28000000;

    /**
     * Start of range reserved for application generated recoverable error
     * events defined by OCAP. This reserved range ends with
     * {@link #END_APP_REC_ERROR_EVENT_TYPES}.
     */
    public static final int BEGIN_APP_REC_ERROR_RESERVED_EVENT_TYPES = 0x2C000000;

    /**
     * End of range for application generated recoverable error events.
     */
    public static final int END_APP_REC_ERROR_EVENT_TYPES = 0x2FFFFFFF;

    /**
     * Start of range for system generated catastrophic events.
     * <p>
     * These the events are generated by the system when it detects a
     * catastrophic failure that will cause (or has caused) the application
     * identified by the {@link #getAppID()} method to be terminated.
     * <p>
     * These events may also be internal system errors, in which case
     * <code>getAppID()</code> will return null.
     */
    public static final int BEGIN_SYS_CAT_ERROR_EVENT_TYPES = 0x30000000;

    /**
     * Start of range reserved for system generated catastrophic error events
     * defined by OCAP. This reserved range ends with
     * {@link #END_SYS_CAT_ERROR_EVENT_TYPES}.
     */
    public static final int BEGIN_SYS_CAT_ERROR_RESERVED_EVENT_TYPES = 0x34000000;

    /**
     * End of range for system generated catastrophic error events.
     */
    public static final int END_SYS_CAT_ERROR_EVENT_TYPES = 0x37FFFFFF;

    /**
     * Start of range reserved for application generated catastrophic error
     * events defined by OCAP. This reserved range ends with
     * {@link #END_APP_CAT_ERROR_EVENT_TYPES}.
     */
    public static final int BEGIN_APP_CAT_ERROR_EVENT_TYPES = 0x38000000;

    /**
     * Start of range reserved for application generated catastrophic error
     * events defined by OCAP. This reserved range ends with
     * {@link #END_APP_CAT_ERROR_EVENT_TYPES}.
     */
    public static final int BEGIN_APP_CAT_ERROR_RESERVED_EVENT_TYPES = 0x3C000000;

    /**
     * End of range for application generated catastrophic error events.
     */
    public static final int END_APP_CAT_ERROR_EVENT_TYPES = 0x3FFFFFFF;

    /**
     * Start of range for system generated reboot events.
     */
    public static final int BEGIN_SYS_REBOOT_EVENT_TYPES = 0x40000000;

    /**
     * Start of range reserved for system generated reboot events defined by
     * OCAP. This reserved range ends with {@link #END_SYS_REBOOT_EVENT_TYPES}.
     */
    public static final int BEGIN_SYS_REBOOT_RESERVED_EVENT_TYPES = 0x44000000;

    /**
     * End of range for system generated reboot events.
     */
    public static final int END_SYS_REBOOT_EVENT_TYPES = 0x47FFFFFF;

    /**
     * Start of range for system generated resource depletion events.
     */
    public static final int BEGIN_SYS_RES_DEP_EVENT_TYPES = 0x50000000;

    /**
     * Start of range reserved for system generated resource depletion events
     * defined by OCAP. This reserved range ends with
     * {@link #END_SYS_RES_DEP_EVENT_TYPES}.
     */
    public static final int BEGIN_SYS_RES_DEP_RESERVED_EVENT_TYPES = 0x54000000;

    /**
     * End of range for system generated resource depletion events.
     */
    public static final int END_SYS_RES_DEP_EVENT_TYPES = 0x57FFFFFF;

    /**
     * Start of range reserved for system generated deferred download events in
     * response to a CVT signaling a deferred download. This reserved range ends
     * with (@link #END_SYS_DNLD_EVENT_TYPES).
     */
    public static final int BEGIN_SYS_DNLD_EVENT_TYPES = 0X58000000;

    /**
     * End of range for system deferred download events.
     */
    public static final int END_SYS_DNLD_EVENT_TYPES = 0X58FFFFFF;
    
    /**
     * Start of range reserved for system generated CableCARD reset events.
     * This reserved range ends with {@link #END_SYS_CABLECARD_RESET_EVENT_TYPES}. 
     */
    public static final int BEGIN_SYS_CABLECARD_RESET_EVENT_TYPES = 0X59000000;
    
    /**
     * End of range for system CableCARD reset events.
     */
    public static final int END_SYS_CABLECARD_RESET_EVENT_TYPES = 0X59FFFFFF;


    /**
     * System event constructor. Assigns a date, and AppID. The readable message
     * is set to null.
     * 
     * @param typeCode
     *            - Unique event type.
     * 
     * @throws IllegalArgumentException
     *             if the typeCode is not in a defined application range when
     *             the event is created by an application.
     */
    protected SystemEvent(int typeCode) throws IllegalArgumentException
    {
        this(typeCode, null);

        checkTypeCode(typeCode);
    }

    /**
     * System event constructor with message. Assigns a date, and AppID.
     * 
     * @param typeCode
     *            - Unique event type.
     * @param message
     *            - Readable message specific to the event generator.
     * 
     * @throws IllegalArgumentException
     *             if the typeCode is not in a defined application range when
     *             the event is created by an application.
     */
    protected SystemEvent(int typeCode, String message) throws IllegalArgumentException
    {
        this(getCurrentAppID(), typeCode, message, System.currentTimeMillis());

        checkTypeCode(typeCode);
    }

    /**
     * This constructor is provided for internal use by OCAP implementations;
     * applications SHOULD NOT call it.
     * 
     * @param typeCode
     *            - The unique error type code.
     * @param message
     *            - Readable message specific to the event generator.
     * @param date
     *            - Event date in milli-seconds from midnight January 1, 1970
     *            GMT.
     * @param appId
     *            - The Id of the application logging the event.
     * 
     * @throws SecurityException
     *             if this constructor is called by any application.
     */
    protected SystemEvent(int typeCode, String message, long date, org.dvb.application.AppID appId)
    {
        this(appId, typeCode, message, date);

        // Ensure that only implementation calls this method
        SecurityUtil.checkPrivilegedCaller();
    }

    /**
     * Private constructor, initializes fields. Used by other constructors to
     * initialize fields.
     * 
     * @param appId
     *            - The Id of the application logging the event.
     * @param typeCode
     *            - The unique error type code.
     * @param message
     *            - Readable message specific to the event generator.
     * @param date
     *            - Event date in milli-seconds from midnight January 1, 1970
     *            GMT.
     */
    private SystemEvent(org.dvb.application.AppID appId, int typeCode, String message, long date)
    {
        this.typeCode = typeCode;
        this.message = message;
        this.date = date;
        this.appId = appId;
    }

    /**
     * Determines the AppID of the current application.
     * 
     * @return the AppID of the current application
     */
    private static org.dvb.application.AppID getCurrentAppID()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        if (ccm == null) return null;

        CallerContext ctx = ccm.getCurrentContext();

        return (org.dvb.application.AppID) ctx.get(CallerContext.APP_ID);
    }

    /**
     * Method to be overridden by subclasses, called from constructor to test
     * for valid typeCode. Need not be called directly by subclass constructors
     * because it will be called by the superclass constructor.
     * 
     * @param typeCode
     *            The unique error type code
     * 
     * @throws IllegalArgumentException
     *             if the typeCode is not in a defined application range when
     *             the event is created by an application.
     */
    private static void checkTypeCode(int typeCode) throws IllegalArgumentException
    {
        if (!SecurityUtil.isPrivilegedCaller()
                && !((typeCode >= BEGIN_APP_INFO_EVENT_TYPES && typeCode <= END_APP_INFO_EVENT_TYPES)
                        || (typeCode >= BEGIN_APP_REC_ERROR_EVENT_TYPES && typeCode <= END_APP_REC_ERROR_EVENT_TYPES) || (typeCode >= BEGIN_APP_CAT_ERROR_EVENT_TYPES && typeCode <= END_APP_CAT_ERROR_EVENT_TYPES)))
        {
            throw new IllegalArgumentException("typeCode not in a defined application range");
        }
    }

    /**
     * Gets the globally unique identifier of the application logging the event.
     * 
     * @return The identifier of the application, or null for events that do not
     *         have an associated application (such as system initiated
     *         reboots).
     */
    public org.dvb.application.AppID getAppID()
    {
        return appId;
    }

    /**
     * Gets the event type code. Identifies a code specific to the event system.
     * 
     * @return type code of the event.
     */
    public int getTypeCode()
    {
        return typeCode;
    }

    /**
     * Gets the event date in milli-seconds from midnight January 1, 1970, GMT.
     * The return value from this method can be passed to the
     * java.util.Date(long) constructor.
     * 
     * @return The date the event was submitted to the system.
     */
    public long getDate()
    {
        return date;
    }

    /**
     * Gets the readable message.
     * 
     * @return message string of the event.
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Generates a private string representation of the given event. This is
     * used because the {@link SystemEvent} class and subclasses are not
     * specified to overrided {@link Object#toString()} with something useful.
     * <p>
     * Returns:
     * 
     * <pre>
     * privNameToString() + &quot;[&quot; + privDataToString() + &quot;] &quot; + privMsgToString
     * </pre>
     * 
     * @return a string representation of this event
     * @see #privNameToString
     * @see #privDataToString
     * @see #privMsgToString
     */
    String privToString()
    {
        return privNameToString() + "[" + privDataToString() + "] " + privMsgToString();
    }

    /**
     * Returns the name of this event. Used by the implementation of
     * {@link #privToString}.
     * 
     * @return the name of this event.
     * @see #privToString
     */
    String privNameToString()
    {
        return getClass().getName();
    }

    /**
     * Returns the data of this event. Used by the implementation of
     * {@link #privToString}.
     * 
     * @return the data of this event.
     * @see #privToString
     */
    String privDataToString()
    {
        AppID id = getAppID();
        // Added for findbugs issues fix - start
        // Moved this from class level variable to local variable
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        // Added for findbugs issues fix - end
        return Integer.toHexString(getTypeCode()) + "," + ((id == null) ? "" : (id + ","))
                + format.format(new java.util.Date(getDate()));
    }

    /**
     * Returns the message of this event. Used by the implementation of
     * {@link #privToString}.
     * 
     * @return the message of this event.
     * @see #privToString
     */
    String privMsgToString()
    {
        return getMessage();
    }

    /**
     * Message string of the event.
     */
    private final String message;

    /**
     * The date the event was submitted to the system.
     */
    private final long date;

    /**
     * The type code of the event.
     */
    private final int typeCode;

    /**
     * The AppID of the application that logged the event.
     */
    private final org.dvb.application.AppID appId;

    /**
     * Used in the formatting of the current date.
     * 
     * @see #privDataToString
     */
    // Added for findbugs issues fix - start
    /*
     * This commented from class level variable to a method's local variable
     * since it is used in one method.
     */
   // private static final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    // Added for findbugs issues fix - end
}
