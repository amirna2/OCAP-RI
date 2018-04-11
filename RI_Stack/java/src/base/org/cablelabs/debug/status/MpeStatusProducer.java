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
package org.cablelabs.debug.status;

import java.util.Hashtable;
import java.util.Enumeration;

import org.cablelabs.impl.manager.ed.*;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.EventDispatchManager;

/**
 * Internal <code>StatusProducer</code> used to provide access to status
 * information provided by MPE managers. This class supports both passive and
 * active (listener) methods of status information access.
 */
public class MpeStatusProducer implements StatusProducer, EDListener
{
    protected MpeStatusProducer(StatusRegistrar registrar)
    {
        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        // Acquire identifiers for supported status types.
        initStatusTypes();

        // Register native MPE producer.
        registrar.registerStatusProducer(this, "mpeStatus");
    }

    /**
     * Acquire the native status information for the specified producer. The
     * type identifier specifies what type of status information is being
     * request, which has to be known ahead of time between producers and
     * consumers.
     * 
     * @param type
     *            status information type identifier.
     * @param format
     *            specifies the desired status format.
     * @param params
     *            is an optional parameter to the status API that allows for
     *            additional information to be passed to the native producer
     *            (e.g. an object supply additional information or return
     *            additional information)
     * 
     * @return Object containing status information.
     */
    public Object getStatus(String type, int format, Object params)
    {
        Integer i = (Integer) stringToIntMap.get(type);

        if (i == null) return null;

        // Call correct native method based on extra parameter type.
        if (params != null)
        {
            if (params instanceof Integer)
                return getMPEStatus(i.intValue(), format, ((Integer) params).intValue());
            else
                // Pass object parameter specific to status type.
                return getMPEStatus(i.intValue(), format, params);
        }
        else
            // Call default native method for now. If a native method
            // is needed to accept a different parameter type
            return getMPEStatus(i.intValue(), format, 0);
    }

    /**
     * Get all of the status types supported.
     * 
     * @return String[] of all supported status types.
     */
    public String[] getStatusTypes()
    {
        Enumeration e = stringToIntMap.keys();
        String[] types = new String[stringToIntMap.size()];
        for (int i = 0; e.hasMoreElements(); ++i)
            types[i] = new String((String) e.nextElement());
        return types;
    }

    /**
     * Acquire the set of status string identifiers and associated integer
     * identifiers for all supports status types. A hashmap is created from the
     * set of status types for easy mapping between the string identifiers and
     * their associated integer identifiers.
     */
    private void initStatusTypes()
    {
        // Acquire status types from MPE.
        Object[] typeArrays = getMPEStatusTypes();

        // Check for failure.
        if (typeArrays == null) return;

        // Get array of strings and array of integers.
        String[] strings = (String[]) typeArrays[0];
        int typeIds[] = (int[]) typeArrays[1];

        if (strings == null || typeIds == null) return;

        // Make sure they are the same size
        if (strings.length != typeIds.length) return;

        // Build hashmap for strings and integers (used to map
        // from the string to the integer identifier).
        for (int i = 0; i < strings.length; ++i)
        {
            Integer typeId = new Integer(typeIds[i]);
            stringToIntMap.put(strings[i], typeId); // String to integer map.
            intToStringMap.put(typeId, strings[i]); // Integer to string map.
        }
    }

    /**
     * Register a listener for the specified status type. Registered status
     * listeners will receive active status information in the same way that
     * status inforamtion would be delivered upon request using
     * <code>getStatus</code>. Only a single <code>StatusListener</code> may be
     * registered for a particular status type.
     * 
     * @param sl
     *            is the <code>StatusListener</code> to register.
     * @parma types is the information type to deliver.
     * @param formats
     *            is the format associated with each type.
     * @param params
     *            is an object containing any parameters specific to the
     *            associated inforamtion type (e.g. additional information
     *            indicating the nature of the status desired)
     * 
     * @return false if this producer does not support active delivery of status
     *         information to status listeners.
     */
    public boolean registerStatusListener(StatusListener sl, String type, int format, Object param)
            throws IllegalArgumentException
    {
        Integer typeId; // Integer identifier of String status type.

        if (sl == null || type == null) throw new IllegalArgumentException(" invalid parameter");

        synchronized (statusListeners)
        {
            int err;
            typeId = (Integer) stringToIntMap.get(type);

            // Validat type & make sure no listener currently registered.
            if (typeId == null || statusListeners.get(typeId) != null)
                throw new IllegalArgumentException(typeId == null ? "illegal type: " + type : "listener for " + type
                        + " already registered");

            // Make sure native status even system is initialized.
            initStatusEvents();

            // Call MPE status manager to register for status event.
            if ((err = registerStatusEvent(typeId.intValue(), format, param)) == 0)
            {
                // Create association between type identifier and registered
                // listener.
                statusListeners.put(typeId, new RegisteredStatusListener(sl, type, format, typeId.intValue()));
            }
            else
                throw new IllegalArgumentException("failed native status event registration for " + type + ", error = "
                        + err);
        }
        return true;
    }

    /**
     * Register a status listener for the specified set of status types. Each
     * status type can have an associated format specifier indicating what
     * format the caller would like to receive and an optional additional
     * parameter specific to the type of event. The additional parameter can be
     * used to specify additional requirements of the event or could be used as
     * an object reference for where to update status information.
     * 
     * @param sl
     *            is the listener to register.
     * @param types
     *            is a string array of types identifiers.
     * @param formats
     *            is a an array of format specifiers.
     * @param params
     *            is an array of object references (or null).
     * 
     * @return true always since status listeners are supported.
     */
    public boolean registerStatusListener(StatusListener sl, String[] types, int[] formats, Object[] params)
    {
        Integer typeId; // Integer identifier of String status type.

        if (sl == null || types == null || formats == null || types.length != formats.length)
            throw new IllegalArgumentException(" illegal parameter");

        synchronized (statusListeners)
        {
            // First validate the types as active listener types.
            for (int i = 0; i < types.length; ++i)
            {
                typeId = (Integer) stringToIntMap.get(types[i]);

                if (typeId == null || statusListeners.get(typeId) != null)
                    throw new IllegalArgumentException(typeId == null ? "illegal type: " + types[i] : "listener for "
                            + types[i] + " already registered");
            }

            // Make sure native status even system is initialized.
            initStatusEvents();

            // Now register the listeners.
            for (int i = 0; i < types.length; ++i)
            {
                int err;
                Integer tId = ((Integer) stringToIntMap.get(types[i]));

                // Call MPE status manager to register for status event.
                if ((err = registerStatusEvent(tId.intValue(), formats[i], (params != null ? params[i] : null))) == 0)
                {
                    // Create association between type identifier and registered
                    // listener.
                    statusListeners.put(tId, new RegisteredStatusListener(sl, types[i], formats[i], tId.intValue()));
                }
                else
                    throw new IllegalArgumentException("failed native status event registration for " + types[i]
                            + ", error = " + err);
            }
        }

        return true;
    }

    /**
     * Unregister a status listener. Does nothing since the MPE producer does
     * not currently support active status information delivery.
     */
    public void unregisterStatusListener(StatusListener sl)
    {
        synchronized (statusListeners)
        {
            Enumeration e = statusListeners.elements();
            while (e.hasMoreElements())
            {
                RegisteredStatusListener rsl = (RegisteredStatusListener) e.nextElement();
                if (rsl.sl == sl)
                {
                    unregisterStatusEvent(rsl.typeId);
                    statusListeners.remove(new Integer(rsl.typeId));
                }
            }
            // If no registered listeners terminate native ED event system.
            if (statusListeners.size() == 0) termStatusEvents();
        }
    }

    /**
     * Initialize native ED event system if not already initialized.
     */
    private void initStatusEvents()
    {
        if (nativeEventsInitialized == false)
        {
            initMPEStatusEvents();
            nativeEventsInitialized = true;
        }
    }

    private void termStatusEvents()
    {
        if (nativeEventsInitialized == true)
        {
            termMPEStatusEvents();
            nativeEventsInitialized = false;
        }
    }

    /**
     * EDListener callback function.
     * 
     * @param eventCode
     *            is the status event + event type identifier
     * @param eventData1
     *            is the status object reference
     * @param eventData2
     *            is the format identifier
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        // Extract the specific status event identifier.
        Integer typeId = new Integer(eventCode);
        RegisteredStatusListener rsl;

        synchronized (statusListeners)
        {
            // Attempt to locate registered listener for status event.
            rsl = (RegisteredStatusListener) statusListeners.get(typeId);

            // Validate status listener index (i.e. status identifier).
            if (rsl == null) return;
        }

        // Deliver the status object to the listener.
        rsl.deliverStatus(typeId.intValue(), eventData2, eventData1);
    }

    /**
     * Private class used to keep track of registered status listeners and
     * delivery status events to a particular status consumer. The status events
     * are delivered in the original CallerContext of the registered status
     * consumer.
     */
    private class RegisteredStatusListener implements CallbackData
    {
        RegisteredStatusListener(StatusListener sl, String type, int format, int typeId)
        {
            this.sl = sl;
            this.type = new String(type);
            this.format = format;
            this.typeId = typeId;
            ctx = ccm.getCurrentContext();
            ctx.addCallbackData(this, this);
        }

        /**
         * Deliver status event in the original caller context.
         * 
         * @param typeID
         *            is the integer identifier of the String status type
         * @param format
         *            is the format of the status object
         * @param status
         *            is the status object integer reference
         */
        void deliverStatus(final int typeId, final int format, final int status)
        {
            System.out.println("[deliverStatus] - typeId = " + typeId + ", format = " + format + ", status = " + status);

            if (ctx != null && this.typeId == typeId && this.format == format)
            {
                ctx.runInContext(new Runnable()
                {
                    public void run()
                    {
                        switch (format)
                        {
                            case FORMAT_INT:
                                // Deliver simply status event.
                                sl.deliverStatus(type, format, status);
                                break;
                            default:
                            {
                                // Call native to allow for conversion of int
                                // reference to an object ref.
                                Object statObj = getObject(typeId, format, status);

                                // Deliver status to registered listener.
                                sl.deliverStatus(type, format, statObj);
                                break;
                            }
                        }
                    }
                });
            }
        }

        /**
         * The following three methods implement the "CallbackData" interface
         * which is used to monitor the handler's associated application. If the
         * application happens to terminate prior to it unregistering the its
         * handler, the destroy method will allow us to automatically unregister
         * the handler and return the associated resources.
         */
        public void destroy(CallerContext callerContext)
        {
            ctx.removeCallbackData(this); // Remove RegisteredListener from
                                          // caller context.
            unregisterStatusListener(sl); // Unregister status listener.
            ctx = null; // Remove references.
            sl = null;
            type = null;
        }

        public void pause(CallerContext callerContext)
        {
        }

        public void resume(CallerContext callerContext)
        {
        }

        public void active(CallerContext callerContext)
        {
        }

        StatusListener sl; // Caller's StatusListener.

        String type; // Status event type.

        int format; // Status format.

        int typeId; // Status type identifier for mapping to string type.

        CallerContext ctx = null;
    }

    /*
     * Native methods for registering/unregistering for status events, acquiring
     * the supported status types and acquiring status information.
     */
    native Object getMPEStatus(int typeId, int format, int param);

    native Object getMPEStatus(int typeId, int format, Object param);

    native int registerStatusEvent(int typeId, int format, Object param);

    native void unregisterStatusEvent(int typeId);

    native Object getObject(int typeId, int format, int reference);

    native void initMPEStatusEvents();

    native void termMPEStatusEvents();

    native Object[] getMPEStatusTypes();

    // Hashtable used to track user status listeners.
    private Hashtable statusListeners = new Hashtable();

    // Hashtable used to map from the string type to native integer type
    // identifier.
    private Hashtable stringToIntMap = new Hashtable();

    // Hashtable used to map from the native integer type identifier to the
    // string type.
    private Hashtable intToStringMap = new Hashtable();

    // Flag indicating whether the native event system is initialized.
    private boolean nativeEventsInitialized = false;

    /**
     * Singleton instance of the CallerContextManager.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

}
