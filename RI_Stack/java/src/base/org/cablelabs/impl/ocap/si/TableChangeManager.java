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

package org.cablelabs.impl.ocap.si;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.service.SIChangeEvent;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.util.EventMulticaster;
import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;
import org.ocap.si.TableChangeListener;

/**
 * The <code>TableChangeManager</code> manages <code>TableChangeListeners</code>
 * associated with a particular in-band or out-of-band service. A service is
 * identified by a SourceID and/or a Frequency/Program#. This class handles the
 * addition and removal of listeners coupled with their associated
 * CallerContext. This class is extended to handle the incoming SI events and
 * send those events off to only the registered listeners for which they are
 * intended.
 * 
 * @author Greg Rutz
 */
abstract class TableChangeManager
{
    private static final Logger log = Logger.getLogger(TableChangeManager.class);

    /**
     * Subclasses should implement this method to create a Service object based
     * on data contained with the <code>SIChangeEvent</code>
     * 
     * @param event
     *            the incoming <code>SIChangeEvent</code>
     */
    abstract public void notifyChange(SIChangeEvent event);

    /**
     * Notifies all listeners of the given change event, if they are interested
     * in the given service
     * 
     * @param service
     *            the service for which listeners should be notified
     * @param event
     *            the change event to send to those listeners
     */
    protected void notifyListeners(final Service service, final SIChangeEvent event)
    {
        // Notify our implementation listeners in decreasing priority order
        final Vector priorityListeners = (Vector) prioritizedListeners.get(service);
        if (priorityListeners != null)
        {
            ccm.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    for (int i = 0; i < priorityListeners.size(); ++i)
                    {
                        PriorityListener pl = (PriorityListener) priorityListeners.elementAt(i);
                        pl.listener.notifyChange(event);
                    }
                }
            });
        }

        // Notify all application listeners
        CallerContext contexts = (CallerContext) registeredContexts.get(service);
        if (contexts != null)
        {
            contexts.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    CCData data = getCCData(ccm.getCurrentContext());
                    data.notifyListeners(service, event);
                }
            });
        }
    }

    /**
     * Add a TableChangeListener listening for changes to the specified service.
     * If the given listener is already registered with the current
     * CallerContext, no action is taken
     * 
     * @param sourceID
     *            the sourceID of the desired service, -1 if sourceID not
     *            specified
     * @param frequency
     *            the frequency of the desired service, -1 if frequency not
     *            specified
     * @param programNumber
     *            the program number of the desired service, -1 if program
     *            number is not specified
     * @param listener
     *            the listener that is to be registered
     */
    protected synchronized void addChangeListener(int sourceID, int frequency, int programNumber,
            TableChangeListener listener)
    {
        Service service = new Service(sourceID, frequency, programNumber);
        CallerContext context = ccm.getCurrentContext();
        CCData data = getCCData(context);

        // If we successfully added this listener to the CallerContext data,
        // then add this context to our list of registered contexts
        data.addListener(service, listener);

        CallerContext ccList = (CallerContext) registeredContexts.get(service);
        ccList = CallerContext.Multicaster.add(ccList, context);
        registeredContexts.put(service, ccList);
    }

    /**
     * Add a TableChangeListener listening for changes to the specified service.
     * If the given listener is already registered with any service, no action
     * is taken. The listeners registered with this call will be notified on the
     * system CallerContext in order of decreasing priority.
     * 
     * @param s
     *            desired service
     * @param listener
     *            the listener that is to be registered
     * @param priority
     *            the priority of this listener. Larger value == higher
     *            priority.
     */
    protected synchronized void addChangeListener(javax.tv.service.Service s, TableChangeListener listener, int priority)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addChangeListener - service: " + s + ", listener: " + listener + ", priority: " + priority);
        }
        // Ensure that this listener is not already registered
        for (Enumeration services = prioritizedListeners.elements(); services.hasMoreElements();)
        {
            Vector serviceListeners = (Vector) services.nextElement();
            for (int i = 0; i < serviceListeners.size(); ++i)
            {
                PriorityListener pl = (PriorityListener) serviceListeners.elementAt(i);
                if (pl.listener == listener) return; // Listener is already
                                                     // registered, so just
                                                     // return
            }
        }

        // Get the current listener list for this service
        ServiceExt sExt = (ServiceExt)s;
        Service service;
        try
        {
            ServiceDetailsExt details = (ServiceDetailsExt) sExt.getDetails();
            ServiceDetailsHandle serviceDetailsHandle = details.getServiceDetailsHandle();
            if (serviceDetailsHandle == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Null serviceDetailsHandle for details - unable to add listener - details  : " + details);
                }
                return;
            }
            service = new Service(serviceDetailsHandle.getHandle());
        }
        catch (SIRequestException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("SI request exception retrieving details", e);
            }
            return;
        }
        catch (InterruptedException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Interrupted retrieving details", e);
            }
            return;
        }
        Vector currentListeners = (Vector) prioritizedListeners.get(service);

        // Create our new priority listener
        PriorityListener pl = new PriorityListener();
        pl.priority = priority;
        pl.listener = listener;

        // No listeners registered yet to this service
        if (currentListeners == null)
        {
            currentListeners = new Vector();
            currentListeners.addElement(pl);
            prioritizedListeners.put(service, currentListeners);
        }
        else
        {
            // Add the new listener to the existing vector in decreasing
            // priority
            // order
            int i;
            for (i = 0; i < currentListeners.size(); ++i)
            {
                PriorityListener listeners = (PriorityListener) currentListeners.elementAt(i);
                if (pl.priority > listeners.priority) break;
            }
            currentListeners.insertElementAt(pl, i);
        }
    }
    
    /**
     * Add a TableChangeListener listening for changes to the specified service.
     * If the given listener is already registered with any service, no action
     * is taken. The listeners registered with this call will be notified on the
     * system CallerContext in order of decreasing priority.
     * 
     * @param sourceID
     *            the sourceID of the desired service, -1 if sourceID not
     *            specified
     * @param frequency
     *            the frequency of the desired service, -1 if frequency not
     *            specified
     * @param programNumber
     *            the program number of the desired service, -1 if program
     *            number is not specified
     * @param listener
     *            the listener that is to be registered
     * @param priority
     *            the priority of this listener. Larger value == higher
     *            priority.
     */
    protected synchronized void addChangeListenerWithPriority(int sourceID, int frequency, int programNumber,
            TableChangeListener listener, int priority)
    {
        // Ensure that this listener is not already registered
        for (Enumeration services = prioritizedListeners.elements(); services.hasMoreElements();)
        {
            Vector serviceListeners = (Vector) services.nextElement();
            for (int i = 0; i < serviceListeners.size(); ++i)
            {
                PriorityListener pl = (PriorityListener) serviceListeners.elementAt(i);
                if (pl.listener == listener) return; // Listener is already
                                                     // registered, so just
                                                     // return
            }
        }

        // Get the current listener list for this service
        Service service = new Service(sourceID, frequency, programNumber);
        Vector currentListeners = (Vector) prioritizedListeners.get(service);

        // Create our new priority listener
        PriorityListener pl = new PriorityListener();
        pl.priority = priority;
        pl.listener = listener;

        // No listeners registered yet to this service
        if (currentListeners == null)
        {
            currentListeners = new Vector();
            currentListeners.addElement(pl);
            prioritizedListeners.put(service, currentListeners);
        }
        else
        {
            // Add the new listener to the existing vector in descreasing
            // priority
            // order
            int i;
            for (i = 0; i < currentListeners.size(); ++i)
            {
                PriorityListener listeners = (PriorityListener) currentListeners.elementAt(i);
                if (pl.priority > listeners.priority) break;
            }
            currentListeners.insertElementAt(pl, i);
        }
    }

    /**
     * Remove the specified <code>TableChangeListener</code>
     * 
     * @param listener
     *            the listener to be removed
     */
    protected synchronized void removeChangeListener(TableChangeListener listener)
    {
        // Look for this listener in our prioritized list first
        for (Enumeration services = prioritizedListeners.keys(); services.hasMoreElements();)
        {
            Object service = services.nextElement();
            Vector serviceListeners = (Vector) prioritizedListeners.get(service);
            for (int i = 0; i < serviceListeners.size(); ++i)
            {
                PriorityListener pl = (PriorityListener) serviceListeners.elementAt(i);
                if (pl.listener == listener)
                {
                    serviceListeners.removeElementAt(i);
                    if (serviceListeners.isEmpty()) prioritizedListeners.remove(service);

                    return;
                }
            }
        }

        CCData data = getCCData(ccm.getCurrentContext());
        Service[] services = data.removeListener(listener);

        // The returned services list contains the list of Services for which
        // there are no more registered listeners. The CallerContexts associated
        // with these Services can be removed from the registeredContexts
        // hashtable.
        for (int i = 0; i < services.length; ++i)
        {
            Service service = services[i];
            CallerContext ccList = (CallerContext) registeredContexts.get(service);
            ccList = CallerContext.Multicaster.remove(ccList, ccm.getCurrentContext());
            if (ccList == null)
                registeredContexts.remove(service);
            else
                registeredContexts.put(service, ccList);
        }
    }

    /**
     * Per caller context data
     */
    protected class CCData implements CallbackData
    {
        // Notify each listener registered to the given service
        public void notifyListeners(Service service, SIChangeEvent event)
        {
            TableChangeListener listenerList = (TableChangeListener) listeners.get(service);
            if (listenerList != null)
            {
                // Listeners must be called back with an SIElement that has the
                // exact same locator with which it was registered, so build it
                // now
                OcapLocator locator = null;
                try
                {
                    if (service.sourceID != -1)
                        locator = new OcapLocator(service.sourceID);
                    else if(service.programNumber != -1)
                        locator = new OcapLocator(service.frequency, service.programNumber, -1);
                    else
                    {
                        //
                    }
                }
                catch (InvalidLocatorException e)
                {
                }

                // Set the locator
                if (event.getSIElement() instanceof ProgramAssociationTableImpl)
                {
                    ProgramAssociationTableImpl pat = (ProgramAssociationTableImpl) event.getSIElement();
                    pat.setLocator(locator);
                }
                else if (event.getSIElement() instanceof ProgramMapTableImpl)
                {
                    ProgramMapTableImpl pmt = (ProgramMapTableImpl) event.getSIElement();
                    pmt.setLocator(locator);
                }
                listenerList.notifyChange(event);
            }
        }

        // private Logger log = Logging.LOGGING ?
        // Logger.getLogger(TableChangeManager.class) : null;

        // Add a new listener to our multicaster list.
        public void addListener(Service service, TableChangeListener listener)
        {
            synchronized (TableChangeManager.this)
            {
                TableChangeListener listenerList = (TableChangeListener) listeners.get(service);
                listenerList = EventMulticaster.add(listenerList, listener);

                // Uncomment the following code to see the length of the
                // listenerList.
                // Also, uncomment the multicasterLength() method in
                // EventMulticaster.
                // if (Logging.LOGGING)
                // {
                // int length = (listenerList instanceof EventMulticaster)
                // ? ((EventMulticaster)listenerList).multicasterLength() : 1;
                // String svc = "{src="+Integer.toHexString(service.sourceID)
                // + ",freq=" + service.frequency
                // + ",prog=" + service.programNumber
                // + "}";
                // log.debug("addListener(svc="+svc+", listener="+listener+"): listeners length = "+length);
                // }

                listeners.put(service, listenerList);
            }
        }

        // Remove listener from each Service's listener list.
        // Return an array containing all
        // of the Services which no longer have listeners registered.
        public Service[] removeListener(TableChangeListener listener)
        {
            Vector v = new Vector();
            synchronized (TableChangeManager.this)
            {
                // Search through the list looking for a matching listener.
                // When one is found, remove it from the listenerList. If it
                // was the last listener registered for the Service, then remove
                // the Service entry from the listeners hashtable.
                for (Enumeration e = listeners.keys(); e.hasMoreElements();)
                {
                    Service service = (Service) e.nextElement();
                    TableChangeListener serviceListeners = (TableChangeListener) listeners.get(service);
                    serviceListeners = EventMulticaster.remove(serviceListeners, listener);
                    if (serviceListeners != null)
                        listeners.put(service, serviceListeners);
                    else
                    {
                        listeners.remove(service);
                        v.add(service);
                    }
                }
            }
            return (Service[]) v.toArray(new Service[v.size()]);
        }

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (TableChangeManager.this)
            {
                // Remove all references to this CallerContext from our list of
                // registered contexts.
                for (Enumeration e = registeredContexts.keys(); e.hasMoreElements();)
                {
                    Service service = (Service) e.nextElement();
                    CallerContext ccList = (CallerContext) registeredContexts.get(service);
                    ccList = CallerContext.Multicaster.remove(ccList, cc);
                    if (ccList == null)
                        registeredContexts.remove(service);
                    else
                        registeredContexts.put(service, ccList);
                }
            }
        }

        /**
         * The listeners to be notified of ProgramAssociationTable change events
         */
        private Hashtable listeners = new Hashtable();
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private CCData getCCData(CallerContext cc)
    {
        CCData data = null;
        synchronized (lock)
        {
            // Retrieve the data for the caller context
            data = (CCData) cc.getCallbackData(this);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to ccList.
            if (data == null)
            {
                data = new CCData();
                cc.addCallbackData(data, this);
            }
        }
        return data;
    }

    /**
     * This class represents a service as defined by a sourceID OR a frequency
     * and program number. A service with source ID == -1 and frequency == -1
     * and programNumber != -1 s an out-of-band service.
     * 
     * @author Greg Rutz
     */
    protected class Service
    {
        public int sourceID = -1;

        public int frequency = -1;

        public int programNumber = -1;
        
        public int serviceHandle = -1;

        public Service(int sourceID, int frequency, int programNumber)
        {
            this.sourceID = sourceID;
            this.frequency = frequency;
            this.programNumber = programNumber;
        }

        public Service(int sHandle)
        {
            this.serviceHandle = sHandle;
        }
        
        public boolean equals(Object other)
        {
            if (other instanceof Service)
            {
                Service s = (Service) other;
                if(s.serviceHandle == this.serviceHandle)                 
                    return true;
                if (s.sourceID == -1)
                {
                    if (s.frequency == frequency && s.programNumber == programNumber) 
                        return true;
                }
                if (s.sourceID == sourceID) 
                    return true;
            }
            return false;
        }

        public int hashCode()
        {
            final int PRIME = 1000003;
            int result = 0;

            if (sourceID != -1)
            {
                result = PRIME * result + sourceID;
            }    
            if(frequency != -1)
            {
                result = PRIME * result + frequency;
            }
            if(programNumber != -1)
            {
                result = PRIME * result + programNumber;
            }            
            if (serviceHandle != -1)
            {
                result = PRIME * result + serviceHandle;
            }
            return result;
        }
    }

    private class PriorityListener
    {
        public int priority;

        public TableChangeListener listener;
    }

    // Maintains a list of TableChangeListeners that have been registered by the
    // implementation on the SystemContext. These listeners have an associated
    // priority and will be notified in order of decreasing priority.
    // Hashtable(Service, Vector(PriorityListener))
    private Hashtable prioritizedListeners = new Hashtable();

    // Maintains a list of all CallerContexts holding reservations, keyed by
    // Service
    // Hashtable(Service, CallerContext)
    private Hashtable registeredContexts = new Hashtable();

    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private Object lock = new Object();
}
