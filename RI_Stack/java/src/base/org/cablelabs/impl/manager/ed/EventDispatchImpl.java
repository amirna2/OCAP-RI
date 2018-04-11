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

package org.cablelabs.impl.manager.ed;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.ThreadPriority;

/**
 * The <code>FileManager</code> implementation.
 * 
 * Provides the asynchronous event processing thread needed to listen and
 * retrieve asynchronous events from MPE (asynchronous loads, object version
 * changes, etc.) and distribute them to the registered listener within the
 * appropriate application context.
 * 
 */
public final class EventDispatchImpl implements EventDispatchManager
{

    // ******************** Public Methods ******************** //

    /**
     * Not publicly instantiable. Use
     * <code>EventDispatchManager.getInstance()</code> to instantiate this class
     * instead.
     */
    protected EventDispatchImpl()
    {
        // start up asynchronous event thread for normal events
        threadNormal = new EventDispatchThread(EDListener.QUEUE_NORMAL, "ED-Normal", ThreadPriority.SYSTEM);
        threadNormal.start();

        // start up asynchronous event thread for Class-1 special events
        threadSpecial1 = new EventDispatchThread(EDListener.QUEUE_SPECIAL1, "ED-Special-1", ThreadPriority.SYSTEM);
        threadSpecial1.start();

        // start up asynchronous event thread for tune-related events
        threadTuneEvents = new EventDispatchThread(EDListener.QUEUE_TUNE_EVENTS, "ED-TuneEvents", ThreadPriority.SYSTEM);
        threadTuneEvents.start();

        /*
         * TODO: Add event dispatch threads for "special" (time critical, etc.) events here */
    }

    /**
     * Returns the singleton instance of the EventDispatchImpl. Will be called
     * only once for each Manager class type.
     */
    public static synchronized Manager getInstance()
    {
        // return an instance of this class
        if (singleton == null)
            return (singleton = new EventDispatchImpl());
        else
            return singleton;
    }

    /**
     * Destroys the asynchronous event dispatch thread.
     */
    public void destroy()
    {
        // interrupt any live async threads
        // so that they will die off when they comes back up out of MPE
        if (threadNormal != null)
        {
            threadNormal.interrupt();
            threadNormal = null;
        }
        if (threadSpecial1 != null)
        {
            threadSpecial1.interrupt();
            threadSpecial1 = null;
        }
    }

    // ******************** Private Classes ******************** //

    private class EventDispatchThread extends Thread
    {
        private int queueType;

        /**
         * Constructor for EventDispatchThread.
         * 
         * @param queueType
         *            Specific EventDispatchListener queue type associated with
         *            this thread.
         * @param threadName
         *            is the name of the event thread.
         * @param threadPriority
         *            is the execution priority of the thread.
         */
        EventDispatchThread(int queueType, String threadName, int threadPriority)
        {
            super(threadName);
            this.queueType = queueType;
            super.setDaemon(true);
            super.setPriority(threadPriority);

            // Create native event queue on calling thread to avoid race
            // condition where usage of event occurs before queue creation.
            createNativeEventQueue(queueType);
        }

        /**
         * Asynchronous event thread created to go into the MPE event-dispatch
         * layer and process asynchronous events.
         */
        public void run()
        {
            // call the JNI layer to process the events
            // if we ever get back up here, then this thread should end
            while (!interrupted())
            {
                // We pass this array down so that native has a way of returning
                // multiple values to us without invoking JNI functions that
                // require
                // Java access
                int[] edListenerParamArray = new int[3];

                // Process the next event and retrieve the Java callback
                // information
                Object edListenerObject = processNextEvent(queueType, edListenerParamArray);

                // Returning a null object is common. It happens when the wait
                // on
                // an event queue times out. So, we'll just continue our loop
                if (edListenerObject == null)
                {
                    continue;
                }

                // The returned object should always be an instance of
                // EDListener
                if (!(edListenerObject instanceof EDListener))
                {
                    SystemEventUtil.logRecoverableError(new Exception(
                            "EventDispatchImpl: Returned object is NOT an EDListener"));
                    continue;
                }

                // Call the java callback
                EDListener listener = (EDListener) edListenerObject;
                try
                {
                    listener.asyncEvent(edListenerParamArray[0], edListenerParamArray[1], edListenerParamArray[2]);
                }
                catch (Throwable e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }
            }

            deleteNativeEventQueue(queueType);
        }
    }

    // ******************** Private Properties ******************** //

    /**
     * instance of the asynchronous thread for normal events
     */
    private EventDispatchThread threadNormal = null;

    /**
     * instance of the asynchronous thread for Special events
     */
    private EventDispatchThread threadSpecial1 = null;

    /**
     * instance of the asynchronous thread for tune-related events
     */
    private EventDispatchThread threadTuneEvents = null;

    /**
     * Singleton instance of the ED manager.
     */
    private static EventDispatchImpl singleton = null;

    // ******************** Native Methods ******************** //

    /**
     * Native method entry point to continually dispatch MPE events for the
     * indicated queue type up to designated Java objects
     */

    private static native Object processNextEvent(int queueType, int[] edListenerParamArray);

    private static native void createNativeEventQueue(int queueType);

    private static native void deleteNativeEventQueue(int queueType);

    // ******************** Static Initializers ******************** //

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }

}
