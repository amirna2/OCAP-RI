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

package org.cablelabs.impl.ocap.ui;

import java.util.Dictionary;

import javax.media.Player;
import javax.tv.service.selection.ServiceContext;

import org.apache.log4j.Logger;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenDevice;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.ui.MultiScreenConfiguration;
import org.ocap.ui.MultiScreenManager;
import org.ocap.ui.event.MultiScreenConfigurationListener;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.util.EventMulticaster;

import org.cablelabs.impl.manager.MsmManager;

/**
 * <p>
 * The <code>MultiScreenManagerImpl</code> class is an implementation, singleton
 * management class implemented by an OCAP host platform that provides
 * multiscreen management services.
 * </p>
 * 
 * <p>
 * For other semantic constraints and behavior that apply, see the <i>OCAP
 * Multiscreen Manager (MSM) Extension</i> specification.
 * </p>
 * 
 * @author Alan Cohn
 * 
 * @see org.davic.resources.ResourceServer
 **/
public class MultiScreenManagerImpl extends MultiScreenManager implements MsmManager
{
    /**
     * Public default constructor required by superclass to instantiate the
     * implementation subclass.
     */
    public MultiScreenManagerImpl()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.Manager#destroy()
     */
    public void destroy()
    {
    }

    // Description copied from MultiScreenManager
    public static MultiScreenManager getInstance()
    {
        if (null == singletonMsm)
        {
            singletonMsm = new MultiScreenManagerImpl();
        }
        return singletonMsm;
    }

    /**
     * @see org.cablelabs.impl.manager.MsmManager This method is called after
     *      getInstance() has been called.
     */
    public MultiScreenManager getMultiScreenManager()
    {
        return singletonMsm;
    }

    /**
     * This method informs a resource server that a particular object should be
     * informed of changes in the state of the resources managed by that server.
     * 
     * Refer to OC-SP-OCAP-MSM-I01-071012, Paragraph 7.3.4.2, Item 10.
     * 
     * @param listener
     *            the object to be informed of state changes
     */
    public void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("MSM addResourceStatusEventListener");
        }
        addResourceStatusEventListener(listener, ccm.getCurrentContext());
    }

    /**
     * This method informs a resource server that a particular object is no
     * longer interested in being informed about changes in state of resources
     * managed by that server. If the object had not registered it's interest
     * initially then this method has no effect.
     * 
     * @param listener
     *            the object which is no longer interested
     */
    public void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("MSM removeResourceStatusEventListener");
        }
        removeResourceStatusEventListener(listener, ccm.getCurrentContext());
    }

    /**
     * Add a <code>ResourceStatusListener</code> to this device for the given
     * calling context.
     * 
     * @param listener
     *            the <code>ResourceStatusListener</code> to be added to this
     *            device.
     * @param context
     *            the context of the application installing the listener
     */
    private void addResourceStatusEventListener(ResourceStatusListener listener, CallerContext context)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(context);

            // Update listener/multicaster
            data.rsListeners = EventMulticaster.add(data.rsListeners, listener);

            // Manage context/multicaster
            rsContexts = Multicaster.add(rsContexts, context);
        }
    }

    /**
     * Remove a <code>ResourceStatusListener</code> from this device for the
     * given calling context.
     * 
     * @param listener
     *            the <code>ResourceStatusListener</code> to be removed to this
     *            device.
     * @param ctx
     *            the context of the application removing the listener
     */
    private void removeResourceStatusEventListener(ResourceStatusListener listener, CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = (Data) ctx.getCallbackData(this);

            // Remove the given listener from the set of listeners
            if (data != null && data.rsListeners != null)
            {
                data.rsListeners = EventMulticaster.remove(data.rsListeners, listener);
            }
        }
    }

    /**
     * Notify <code>ResourceStatusListener</code>s of changes to the
     * HScreenDeviceReleasedEvent. <i>This method is not part of the defined
     * public API, but is present for the implementation only.</i>
     * 
     * @param e
     *            the event HScreenDeviceReleasedEvent.
     */
    void notifyResourceStatusListener(ResourceStatusEvent e)
    {
        // Do not lock during this call!
        // We simply want whatever we read at the time of access.

        final ResourceStatusEvent event = e;
        CallerContext contexts = rsContexts;
        if (contexts != null)
        {
            contexts.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();

                    // Listeners are maintained in-context
                    Data data = (Data) ctx.getCallbackData(MultiScreenManagerImpl.this);
                    if (null != data)
                    {
                        ResourceStatusListener l = data.rsListeners;
                        if (l != null)
                        {
                            l.statusChanged(event);
                        }
                    }

                }
            });
        }
    }

    // Description copied from MultiScreenManager
    public HScreen[] getScreens()
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public HScreen getDefaultScreen()
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public HScreen[] findScreens(ServiceContext context)
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public HScreen getOutputPortScreen(VideoOutputPort port)
    {
        // TODO complete method
        return null;
    }

    // Description copied from superclass
    public HScreen[] getCompatibleScreens(VideoOutputPort port)
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public MultiScreenConfiguration[] getMultiScreenConfigurations() throws SecurityException
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public MultiScreenConfiguration[] getMultiScreenConfigurations(String screenConfigurationType)
            throws SecurityException
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public MultiScreenConfiguration getMultiScreenConfiguration(HScreen screen)
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public MultiScreenConfiguration getMultiScreenConfiguration()
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public void setMultiScreenConfiguration(MultiScreenConfiguration configuration,
            Dictionary serviceContextAssociations) throws SecurityException, IllegalStateException,
            IllegalStateException
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void requestMultiScreenConfigurationChange(MultiScreenConfiguration configuration,
            Dictionary serviceContextAssociations) throws SecurityException, IllegalStateException
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void addMultiScreenConfigurationListener(MultiScreenConfigurationListener listener)
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void removeMultiScreenConfigurationListener(MultiScreenConfigurationListener listener)
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void addResourceStatusListener(ResourceStatusListener listener)
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void removeResourceStatusListener(ResourceStatusListener listener)
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void swapServiceContexts(HScreen screen1, HScreen screen2, ServiceContext[] exclusions)
            throws SecurityException, IllegalStateException
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void moveServiceContexts(HScreen src, HScreen dst, ServiceContext[] contexts) throws SecurityException,
            IllegalArgumentException, IllegalStateException
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public HScreenDevice[] getPlayerScreenDevices(Player player)
    {
        // TODO complete method
        return new HScreenDevice[0];
    }

    // Description copied from MultiScreenManager
    public void addPlayerScreenDevices(Player player, HScreenDevice[] devices) throws SecurityException,
            IllegalStateException
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public void removePlayerScreenDevices(Player player, HScreenDevice[] devices) throws SecurityException,
            IllegalArgumentException, IllegalStateException
    {
        // TODO complete method
    }

    // Description copied from MultiScreenManager
    public HScreen getEmptyScreen()
    {
        // TODO complete method
        return null;
    }

    // Description copied from MultiScreenManager
    public boolean isEmptyScreen(HScreen screen)
    {
        // TODO complete method
        return false;
    }

    // Description copied from MultiScreenManager
    public boolean isEmptyScreenDevice(HScreenDevice device)
    {
        // TODO complete method
        return false;
    }

    // Description copied from MultiScreenManager
    public boolean sameResources(HScreen screen1, HScreen screen2)
    {
        // TODO complete method
        return false;
    }

    // Description copied from MultiScreenManager
    public boolean sameResources(HScreenDevice device1, HScreenDevice device2)
    {
        // TODO complete method
        return false;
    }

    // Description copied from MultiScreenManager
    public boolean sameResources(ServiceContext sc1, ServiceContext sc2)
    {
        // TODO complete method
        return false;
    }

    /**
     * Access this device's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx)
    {
        synchronized (lock)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
            }
            return data;
        }
    }

    /**
     * Holds context-specific data. Specifically the set of
     * <code>ResourceStatusListener</code>s.
     */
    private class Data implements CallbackData
    {
        public ResourceStatusListener rsListeners = null;

        public void destroy(CallerContext ctx)
        {
        }

        public void active(CallerContext ctx)
        {
        }

        public void pause(CallerContext ctx)
        {
        }
    }

    /**
     * Multicaster for ResourceStatusListener.
     */
    private CallerContext rsContexts;

    /**
     * Reference to the CallerContextManager singleton.
     */
    private CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * Private lock.
     */
    private Object lock = new Object();

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(MultiScreenManager.class.getName());

    /*
     * only 1 Multiscreen Manager
     */
    private static MultiScreenManager singletonMsm;

}
