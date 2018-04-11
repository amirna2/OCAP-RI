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

package org.cablelabs.impl.service.javatv.selection;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextDestroyedEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceContextPermission;
import javax.tv.xlet.XletContext;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.service.ServiceContextDelegateFactory;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceContextFactoryExt;
import org.cablelabs.impl.service.ServiceContextLifetimeListener;

/**
 * A base <code>ServiceContextFactory</code> implementation.
 * <p/>
 * The implementation must not synchronize on this object. Doing so allows
 * applications to block execution of the implementation.
 * 
 * @author Todd Earles
 */
public class ServiceContextFactoryImpl extends ServiceContextFactoryExt
{
    private static final Logger log = Logger.getLogger(ServiceContextFactoryImpl.class);

    private static final String DELEGATE_FACTORY_PARAM_PREFIX = "OCAP.serviceContextDelegateFactory";

    private static final String DELEGATE_FACTORY_DEFAULT_PARAM_PREFIX = "OCAP.serviceContextDefaultDelegateFactory";

    private final List serviceContextDelegateFactories;

    private static final String SERVICE_CONTEXT_CLASS_PARAM = "OCAP.serviceContextClass";

    /**
     * Construct a <code>ServiceContextFactoryImpl</code>.
     */
    public ServiceContextFactoryImpl()
    {
        // TODO(Todd): Create more that one service context if the platform
        // supports the simultaneous presentation of more than one service.

        // Get associated objects
        serviceContextDelegateFactories = getRegisteredServiceContextDelegateFactories();
        ccManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    }

    private List getRegisteredServiceContextDelegateFactories()
    {
        List delegateFactories = new ArrayList();
        delegateFactories.addAll(PropertiesManager.getInstance()
                .getInstancesByPrecedence(DELEGATE_FACTORY_PARAM_PREFIX));
        Object defaultDelegateFactory = PropertiesManager.getInstance().getInstanceByPrecedence(
                DELEGATE_FACTORY_DEFAULT_PARAM_PREFIX);
        if (defaultDelegateFactory != null)
        {
            delegateFactories.add(defaultDelegateFactory);
        }
        return delegateFactories;
    }

    // Description copied from ServiceContextFactory
    public void setCreateEnabled(boolean enabled)
    {
        //findbugs complains about "write to static method from instance" - however, I don't see any other way...
        //findbugs configured to ignore.
        createEnabled = enabled;
    }

    // Description copied from ServiceContextFactory
    public ServiceContext createServiceContext() throws InsufficientResourcesException, SecurityException
    {
        // Check permission
        checkPermission(new ServiceContextPermission("create", "own"));

        // Create a service context for the current caller context.
        try
        {
            return createServiceContext(ccManager.getCurrentContext(), false);
        }
        // don't return null - catch, log & throw a runtime exception
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to construct serviceContext instance", e);
            }
            throw new RuntimeException("Unable to construct serviceContext instance: ", e);
        }
    }

    // Description copied from ServiceContextFactoryExt
    public ServiceContext createAutoSelectServiceContext()
    {
        try
        {
            return createServiceContext(ccManager.getSystemContext(), true);
        }
        // don't return null - catch, log & throw a runtime exception
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to construct serviceContext instance", e);
            }
            throw new RuntimeException("Unable to construct serviceContext instance: ", e);
        }
    }

    /**
     * Create a service context and handle its eventual destruction.
     */
    private ServiceContext createServiceContext(final CallerContext cc, boolean forAutoSelect)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException,
            InstantiationException
    {
        if (!createEnabled)
        {
            throw new SecurityException("ServiceContext creation is globally disabled");
        }

        if (log.isInfoEnabled())
        {
            log.info("createServiceContext");
        }
        // Create a new service context
        String serviceContextClassName = PropertiesManager.getInstance().getPropertyValueByPrecedence(
                SERVICE_CONTEXT_CLASS_PARAM);
        ServiceContextExt sc;
        Class serviceContextClass = Class.forName(serviceContextClassName);
        if (log.isDebugEnabled())
        {
            log.debug("creating serviceContext instance from class: " + serviceContextClass.getName());
        }
        Constructor serviceContextConstructor = serviceContextClass.getConstructor(new Class[] { boolean.class });
        sc = (ServiceContextExt) serviceContextConstructor.newInstance(new Object[] { new Boolean(forAutoSelect) });

        registerServiceContextDelegates(sc);
        // Remove it from the service context list(s) when it is destroyed.
        final ServiceContextExt finalSc = sc;
        sc.addListener(new ServiceContextListener()
        {
            public void receiveServiceContextEvent(ServiceContextEvent e)
            {
                if (e instanceof ServiceContextDestroyedEvent)
                {
                    remove(finalSc, cc);
                }
            }
        });

        // Add the new service context to the list(s) maintained by this
        // factory. Check to make sure the service context is not destroyed
        // because it may have been destroyed before we got the listener
        // installed.
        if (!sc.isDestroyed())
        {
            add(sc, cc);
        }

        return sc;
    }

    protected void registerServiceContextDelegates(ServiceContextExt sc)
    {
        // register the available service context delegates on the service
        // context
        List serviceContextDelegates = new ArrayList();
        for (Iterator iter = serviceContextDelegateFactories.iterator(); iter.hasNext();)
        {
            serviceContextDelegates.add(((ServiceContextDelegateFactory) iter.next()).createServiceContextDelegate());
        }
        if (log.isInfoEnabled())
        {
            log.info("registering serviceContextDelegates on serviceContext: " + sc + ", delegates: "
                    + serviceContextDelegates);
        }
        sc.setAvailableServiceContextDelegates(serviceContextDelegates);
    }

    /**
     * Add the specified service context to the list(s) of service context
     * objects known to this factory.
     * 
     * @param sc
     *            the new service context to add to the list
     * @param cc
     *            the caller context associated with <code>sc</code>
     */
    void add(ServiceContext sc, CallerContext cc)
    {
        // Add this new service context to the vector of service context objects
        // for
        // the specified caller context.
        CCData data = getCCData(cc);
        data.scImplVector.addElement(sc);

        // Add this new service context to the vector of all service context
        // objects
        allServiceContexts.addElement(sc);

        invokeListenersAdded(sc);
    }

    /**
     * Remove the specified service context from the list(s) of service context
     * objects known to this factory.
     * 
     * @param sc
     *            the new service context to add to the list
     * @param cc
     *            the caller context associated with <code>sc</code>
     */
    void remove(ServiceContext sc, CallerContext cc)
    {
        // Remove this service context from the vector of all service context
        // objects
        allServiceContexts.removeElement(sc);

        // Remove this service context from the vector of service context
        // objects for
        // the specified caller context.
        CCData data = getCCData(cc);
        data.scImplVector.removeElement(sc);

        invokeListenersRemoved(sc);
    }

    // Description copied from ServiceContextFactory
    public ServiceContext getServiceContext(XletContext ctx) throws SecurityException, ServiceContextException
    {
        // Handle invalid context
        if (ctx == null)
        {
            throw new NullPointerException("XletContext null");
        }

        // Must be a known XletContext instance, so ensure that the classloaders
        // for the passed XletContext and this factory are the same
        if (ctx.getClass().getClassLoader() != getClass().getClassLoader())
        {
            throw new ServiceContextException("Not a valid XletContext");
        }

        // Check permissions
        checkPermission(new ServiceContextPermission("access", "own"));

        // Get the service context
        CallerContext cc = ccManager.getCurrentContext();
        ServiceContext sc = (ServiceContext) cc.get(CallerContext.SERVICE_CONTEXT);
        if (sc == null)
        {
            throw new ServiceContextException();
        }
        else
        {
            return sc;
        }
    }

    // Description copied from ServiceContextFactory
    public ServiceContext[] getServiceContexts()
    {
        // Check permissions and return an zero-length array if no instances
        // are accessible. Flag whether we are returning all context objects
        // or just the ones we own.
        boolean justOurs = false;
        try
        {
            checkPermission(new ServiceContextPermission("access", "*"));
        }
        catch (SecurityException e)
        {
            try
            {
                checkPermission(new ServiceContextPermission("access", "own"));
                justOurs = true;
            }
            catch (SecurityException se)
            {
                return new ServiceContext[] {};
            }
        }

        // Get the caller context and its associated data
        CallerContext cc = ccManager.getCurrentContext();
        CCData data = getCCData(cc);

        // Create vector to hold all service context objects the caller has
        // access to.
        Vector vector = new Vector();

        // Always add the service context the caller is running in. This may be
        // a
        // service context that was created by a different caller context.
        ServiceContextExt ours = (ServiceContextExt) cc.get(CallerContext.SERVICE_CONTEXT);
        if ((ours != null) && !(ours.isDestroyed()))
        {
            vector.addElement(ours);
        }

        // If justOurs then get the vector of service context objects created
        // by the current caller context. Otherwise, get the vector of all
        // service context objects created by any caller context.
        Vector scList;
        if (justOurs)
        {
            scList = data.scImplVector;
        }
        else
        {
            scList = allServiceContexts;
        }

        // Add all the service context objects from the list specified above.
        // This
        // is done within a synchronized block so we ensure that the vector does
        // not change while we are looking at it.
        synchronized (scList)
        {
            for (int i = 0; i < scList.size(); i++)
            {
                ServiceContextExt sc = (ServiceContextExt) scList.elementAt(i);
                if ((ours != sc) && !(sc.isDestroyed()))
                {
                    vector.addElement(sc);
                }
            }
        }

        // Copy the vector into an array to return
        ServiceContext returnArray[] = new ServiceContext[vector.size()];
        vector.copyInto(returnArray);

        return returnArray;
    }

    // Description copied from ServiceContextFactoryExt
    public ServiceContext[] getAllServiceContexts()
    {
        // Copy the vector into an array and return it
        ServiceContext returnArray[] = new ServiceContext[allServiceContexts.size()];
        allServiceContexts.copyInto(returnArray);
        return returnArray;
    }

    private void checkPermission(ServiceContextPermission p) throws SecurityException
    {
        org.cablelabs.impl.util.SecurityUtil.checkPermission(p);
    }

    /**
     * Retrieve the caller context specific data. If the CCData object for the
     * specified caller context has not been created then create it.
     * 
     * @param cc
     *            the caller context
     * 
     * @return the caller context specific data for the specified caller context
     */
    private CCData getCCData(CallerContext cc)
    {
        CCData data = (CCData) cc.getCallbackData(this);
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, this);
        }
        return data;
    }

    /**
     * Caller context specific data
     */
    private class CCData implements CallbackData
    {
        public void destroy(CallerContext callerContext)
        {
            // Mark all service context objects created by this caller context
            // so they are destroyed when they become idle.
            for (Enumeration e = scImplVector.elements(); e.hasMoreElements();)
            {
                ServiceContextExt sc = (ServiceContextExt) e.nextElement();
                sc.setDestroyWhenIdle(true);
            }
        }

        public void pause(CallerContext callerContext)
        {
            // TODO(Todd): Determine if we need to do anything here
        }

        public void active(CallerContext callerContext)
        {
            // TODO(Todd): Determine if we need to do anything here
        }

        // Vector of service context objects
        public Vector scImplVector = new Vector();
    }

    // Variable defines whether service context creation is enabled or not
    private static boolean createEnabled = true;

    // Vector of all service context objects
    private static Vector allServiceContexts = new Vector();

    // Caller context manager
    protected CallerContextManager ccManager;

    private Vector m_listeners = new Vector();

    public void addServiceContextLifetimeListener(ServiceContextLifetimeListener l)
    {
        if (!m_listeners.contains(l))
        {
            m_listeners.add(l);
        }
    }

    public void removeServiceContextLifetimeListener(ServiceContextLifetimeListener l)
    {
        m_listeners.remove(l);
    }

    private void invokeListenersAdded(ServiceContext sc)
    {
        Enumeration l = m_listeners.elements();
        while (l.hasMoreElements())
        {
            ServiceContextLifetimeListener scll = (ServiceContextLifetimeListener) l.nextElement();
            try
            {
                scll.serviceContextCreated(sc);
            }
            catch (Exception e)
            { /* Ignore */
            }
        }
    }

    private void invokeListenersRemoved(ServiceContext sc)
    {
        Enumeration l = m_listeners.elements();
        while (l.hasMoreElements())
        {
            ServiceContextLifetimeListener scll = (ServiceContextLifetimeListener) l.nextElement();
            try
            {
                scll.serviceContextDeleted(sc);
            }
            catch (Exception e)
            { /* ignore */
            }
        }
    }
}
