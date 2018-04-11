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

package org.cablelabs.impl.ocap.manager.eas;

import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.message.EASLocationCode;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;
import org.ocap.hardware.Host;
import org.ocap.hardware.PowerModeChangeListener;
import org.ocap.net.OcapLocator;
import org.ocap.system.EASHandler;
import org.ocap.system.EASListener;
import org.ocap.system.EASManager;
import org.ocap.system.EASModuleRegistrar;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.pod.PODListener;
import org.cablelabs.impl.pod.mpe.PODEvent;
import org.cablelabs.impl.service.ServiceContextFactoryExt;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback;
import org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This abstract class provides the implementation of the Emergency Alert System
 * (EAS) module in the OCAP stack.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public abstract class EASManagerImpl implements org.cablelabs.impl.manager.EASManager, EASSectionTableListener,
        BootProcessCallback, PODListener, PowerModeChangeListener
{
    /**
     * A singleton instance of this class serves as a proxy for
     * {@link org.ocap.system.EASManager}.
     * <p>
     * <b>NOTE:</b> due to <code>getInstance()</code> static signature
     * collisions between {@link org.ocap.system.EASManager} and
     * {@link org.cablelabs.impl.manager.EASManager}, and the nature of the
     * {@link ManagerManager} implementation, this nested class is used to
     * extend the former abstract class so the enclosing class can implement the
     * latter interface without conflict.
     */
    class EASManagerProxy extends org.ocap.system.EASManager
    {
        /**
         * Constructs a new instance of the receiver.
         */
        private EASManagerProxy()
        {
            // Intentionally left empty
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.system.EASManager#addListener(org.ocap.system.EASListener)
         */
        public void addListener(EASListener listener)
        {
            getCurrentState().addListener(listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASManager#getState()
         */
        public int getState()
        {
            return getCurrentState().getState();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.system.EASManager#removeListener(org.ocap.system.EASListener
         * )
         */
        public void removeListener(EASListener listener)
        {
            getCurrentState().removeListener(listener);
        }
    }

    /**
     * A singleton instance of this class serves as a proxy for
     * {@link org.ocap.system.EASModuleRegistrar}.
     */
    class EASModuleRegistrarProxy extends org.ocap.system.EASModuleRegistrar
    {
        /**
         * Constructs a new instance of the receiver.
         */
        private EASModuleRegistrarProxy()
        {
            // Intentionally left empty
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASModuleRegistrar#getEASAttribute(int
         * attribute)
         */
        public Object getEASAttribute(int attribute)
        {
            return getCurrentState().getEASAttribute(attribute);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASModuleRegistrar#getEASCapability(int
         * attribute)
         */
        public Object[] getEASCapability(int attribute)
        {
            return getCurrentState().getEASCapability(attribute);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASModuleRegistrar#registerEASHandler(EASHandler
         * handler)
         */
        public void registerEASHandler(EASHandler handler)
        {
            getCurrentState().registerEASHandler(handler);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASModuleRegistrar#setEASAttribute(int
         * attribute[], Object value[])
         */
        public void setEASAttribute(int attribute[], Object value[])
        {
            getCurrentState().setEASAttribute(attribute, value);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.ocap.system.EASModuleRegistrar#unregisterEASHandler()
         */
        public void unregisterEASHandler()
        {
            getCurrentState().unregisterEASHandler();
        }
    }

    // Class Constants

    // Class Constants

    private static final Logger log = Logger.getLogger(EASManagerImpl.class);

    private static final int CC_EA_LOCATION_FEATURE_ID = 0x0C;

    private static final String FILTER_EXPIRED_ALERTS_PARAM_NAME = "OCAP.eas.filter.expired.alerts";

    // see static initializer for following derived constant values
    private static final boolean FILTER_EXPIRED_ALERTS;

    private static final String EAS_IGNORE_POWER_MODE_CHANGES_PARAM_NAME = "OCAP.eas.ignore.power.mode.changes";

    private static final boolean EAS_IGNORE_POWER_MODE_CHANGES;

    static
    {
        FILTER_EXPIRED_ALERTS = Boolean.valueOf(MPEEnv.getEnv(EASManagerImpl.FILTER_EXPIRED_ALERTS_PARAM_NAME, "true"))
                .booleanValue();
        EAS_IGNORE_POWER_MODE_CHANGES = Boolean.valueOf(MPEEnv.getEnv(EASManagerImpl.EAS_IGNORE_POWER_MODE_CHANGES_PARAM_NAME, "false"))
                .booleanValue();
    }

    // Instance Fields

    private final EASManagerProxy m_easManager;

    private final EASModuleRegistrarProxy m_easRegistrar;

    private final Host m_host;

    private final PODManager m_podManager;

    private final ServiceContextFactoryExt m_serviceContextFactory;

    private final ServicesDatabase m_servicesDatabase;

    private boolean m_easEnabled;

    private EASSectionTableMonitor m_easMonitor;

    private EASState m_easState;

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     */
    protected EASManagerImpl()
    {
        if (log.isInfoEnabled())
        {
            log.info("EASManagerImpl instantiating...");
        }

        this.m_serviceContextFactory = (ServiceContextFactoryExt) ServiceContextFactory.getInstance();

        // Start of Critical Section -- EAS must create a ServiceContext for
        // AppDomainImpl to launch auto-start applications.
        try
        {
            ServiceContext serviceContext = this.m_serviceContextFactory.createServiceContext();
            // we no longer need this service context, construction of a
            // ServiceContext was required to initiate launch of auto-start
            // applications.
            serviceContext.destroy();
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError("Failed to create initial ServiceContext", e);
        }
        // End of Critical Section -- Do not delete above lines until
        // AppDomainImpl issue is fixed (Jira #OCORI-567).

        // Get references to other managers that'll be needed.
        this.m_host = Host.getInstance();
        this.m_podManager = (PODManager) ManagerManager.getInstance(PODManager.class);
        this.m_servicesDatabase = ((ServiceManager) ManagerManager.getInstance(ServiceManager.class)).getServicesDatabase();

        // Force static initializers to run in EASState base class first, before
        // the static initializers run in the subclasses.
        EASState.initialize(this);
        this.m_easState = EASStateNotInProgress.INSTANCE;
        this.m_easManager = new EASManagerProxy();
        this.m_easRegistrar = new EASModuleRegistrarProxy();

        // Instance initialization complete...
        // Monitor the boot process for when EAS should be enabled/disabled.
        this.m_servicesDatabase.addBootProcessCallback(this);

        // If EAS is to continue monitoring even in low power mode [keepalive],
        // then don't register with the Host.
        // If EAS is to stop monitoring when in low power mode [the default operation],
        // then register.
        if (EAS_IGNORE_POWER_MODE_CHANGES)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Not listening for power mode changes, EAS permanently ON");
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Starting power mode listener - default operation");
            }
            this.m_host.addPowerModeChangeListener(this);
        }

        // EAS is actually started by BootProcessCallback

        if (log.isInfoEnabled())
        {
            log.info("EASManagerImpl instantiated, waiting for boot to complete before starting");
        }
    }

    // Instance Methods

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.Manager#destroy()
     */
    public void destroy()
    {
        enableEAS(false);
        if (!EAS_IGNORE_POWER_MODE_CHANGES)
        {
            this.m_host.removePowerModeChangeListener(this);
        }
        this.m_servicesDatabase.removeBootProcessCallback(this);
    }

    /**
     * Returns the current operating state of EAS which is generally affected by
     * host device power mode changes.
     * 
     * @return <code>true</code> if EAS is enabled and should operate per [SCTE
     *         18]; <code>false</code> if EAS is disabled and no events should
     *         be displayed
     */
    public synchronized boolean easEnabled()
    {
        return this.m_easEnabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.EASManager#getEASManager() <p> NOTE: Due
     * to getInstance() static signature collisions between
     * org.ocap.system.EASManager and org.cablelabs.impl.manager.EASManager, and
     * the org.cablelabs.impl.manager.ManagerManager implementation constraints,
     * we use a nested class to extend the former and provide this method to
     * return the singleton instance of the nested class through the OCAP API.
     */
    public EASManager getEASManager()
    {
        return this.m_easManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.EASManager#getEASRegistrar()
     */
    public EASModuleRegistrar getEASRegistrar()
    {
        return this.m_easRegistrar;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.EASManager#isEASForceTune()
     */
    public boolean isEASForceTune()
    {
        return getCurrentState().isForceTune();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASSectionTableListener#notify(boolean
     * , org.davic.mpeg.sections.Section)
     */
    public void notify(final boolean oobAlert, final Section table)
    {
        if (easEnabled())
        {
            try
            {
                EASMessage message = EASMessage.create(oobAlert, table.getData());

                if (message == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Alert message discarded - duplicate");
                    }
                }
                else if (hasExpiredAlert(message))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Alert message discarded - incoming event has already expired");
                    }
                }
                else if (hasExcludedService(message))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Alert message discarded - current presenting service is in exception service list");
                    }
                }
                else if (hasInsufficientPriority(message))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Alert message discarded - alert priority too low to process:<"
                                        + message.getAlertPriority() + ">");
                    }
                }
                else if (hasExcludedLocation(message))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Alert message discarded - receiving device outside of alert coverage area");
                    }
                }
                else
                {
                    getCurrentState().receiveAlert(message);
                }
            }
            catch (IllegalArgumentException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Invalid alert message discarded - " + e.getMessage());
                }
            }
            catch (NoDataAvailableException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Invalid alert message discarded - no section data available");
                }
                SystemEventUtil.logRecoverableError("Invalid alert message - no section data available", e);
            }
            catch (NullPointerException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Invalid alert message discarded - no section table provided");
                }
                SystemEventUtil.logRecoverableError("Invalid alert message - no section table provided", e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.pod.PODListener#notify(org.cablelabs.impl.
     * manager.pod.PODEvent)
     */
    public void notify(PODEvent event)
    {
        if (log.isInfoEnabled())
        {
            log.info("received PODEvent - ID: 0x" + Integer.toHexString(event.getEvent()));
        }
        switch (event.getEvent())
        {
            case PODEvent.EventID.POD_EVENT_POD_READY:
                startMonitoring(); // startMonitoring will create the IB or OOB monitor,
                break;
            case PODEvent.EventID.POD_EVENT_RESET_PENDING:
                stopMonitoring(); // clean up the old monitor
                break;
            default:
                // intentionally left empty -- ignore other events
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.PowerModeChangeListener#powerModeChanged(int)
     */
    public void powerModeChanged(int newPowerMode)
    {
        if (log.isInfoEnabled())
        {
            log.info("Notified of power mode change to " + ((newPowerMode == Host.FULL_POWER) ? "Full" : "Low") + " power");
        }
        enableEAS(newPowerMode == Host.FULL_POWER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback#shutdown
     * (org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback)
     */
    public boolean monitorApplicationShutdown(ShutdownCallback callback)
    {
        // Disable as if low power (so that it will get reset after started).
        if (log.isInfoEnabled())
        {
            log.info("Notified of stack shutting down");
        }
        enableEAS(false);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void monitorApplicationStarted()
    {
        // Simply treat as a power mode change
        if (log.isInfoEnabled())
        {
            log.info("Notified of stack starting up");
        }
        // Always start if IGNORE_POWER_MODE_CHANGES is true, otherwise follow power mode
        enableEAS(EAS_IGNORE_POWER_MODE_CHANGES | EASManagerImpl.this.m_host.getPowerMode() == Host.FULL_POWER);
    }

    /**
     * {@inheritDoc}
     */
    public void initialUnboundAutostartApplicationsStarted()
    {
        // Nothing to do (only care about monitor app startup)
    }
    
    /**
     * Factory method used to create an instance of a
     * {@link EASSectionTableMonitor} to monitor in-band transport streams for
     * emergency alert messages. Must be overridden by subclasses.
     * 
     * @return an instance of <code>EASSectionTableMonitor</code> for in-band
     *         alert monitoring
     */
    protected abstract EASSectionTableMonitor createInBandMonitor();

    /**
     * Factory method used to create an instance of a
     * {@link EASSectionTableMonitor} to monitor the out-of-band POD Extended
     * Channel for emergency alert messages. Must be overridden by subclasses.
     * 
     * @return an instance of <code>EASSectionTableMonitor</code> for
     *         out-of-band alert monitoring
     */
    protected abstract EASSectionTableMonitor createOutOfBandMonitor();

    /**
     * Returns the current geographic location of the receiving device.
     * Subclasses should override this method if the EA location should not be
     * retrieved from the POD module.
     * 
     * @return the {@link EASLocationCode} object representing the geographic
     *         location of the receiving device, or <code>null</code> if the EA
     *         location from the POD should be used
     * @see EASManagerImpl#hasExcludedLocation(EASMessage)
     */
    protected EASLocationCode getGeographicLocation()
    {
        return null;
    }

    /**
     * Determines if a POD (e.g. CableCARD) is inserted in the receiving device.
     * The POD may not necessarily be in a ready state to process requests.
     * Subclasses could override this method if the POD should not be accessed.
     * 
     * @return <code>true</code> if POD management is enabled and the POD is
     *         inserted in the receiving device; otherwise <code>false</code>
     */
    protected boolean isPODPresent()
    {
        return (null == this.m_podManager) ? false : this.m_podManager.isPODPresent();
    }

    /**
     * Determines if a POD (e.g. CableCARD) is inserted in the receiving device
     * and is in a ready state to process requests. Subclasses could override
     * this method if the POD should not be accessed.
     * 
     * @return <code>true</code> if POD management is enabled, and the POD is
     *         inserted and in a ready state in the receiving device; otherwise
     *         <code>false</code>
     */
    protected boolean isPODReady()
    {
        return (null == this.m_podManager) ? false : this.m_podManager.isPODReady();
    }

    /**
     * Starts monitoring for EAS section tables.
     * <ul>
     * <li>If POD is READY, start OOB monitoring.</li>
     * <li>If POD is NOT PRESENT, start IB monitoring.</li>
     * <li>Ignore PRESENT but not READY (we'll receive an event later when we
     * become READY).</li>
     * </ul>
     */
    protected void startMonitoring()
    {
        if (log.isInfoEnabled())
        {
            log.info("startMonitoring");
        }
        if (isPODReady())
        {
            if (log.isInfoEnabled())
            {
                log.info("Starting OOB monitor");
            }
            this.m_easMonitor = createOutOfBandMonitor();

            if (!this.m_easMonitor.start())
            {
                if (log.isInfoEnabled())
                {
                    log.info("Unable to start OOB monitor");
                }
            }
        }
        // These should be mutually exclusive, but if there's no MPE support for
        // isPresent but there is support for for isReady,
        // we don't want to start IB monitoring
        else if (!isPODPresent())
        {
            if (log.isInfoEnabled())
            {
                log.info("Starting IB monitor");
            }
            this.m_easMonitor = createInBandMonitor();

            if (!this.m_easMonitor.start())
            {
                if (log.isInfoEnabled())
                {
                    log.info("Unable to start IB monitor");
                }
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("POD present but not ready - waiting to start monitoring when POD ready event is received");
            }
        }
    }

    /**
     * Stops monitoring for EAS section tables.
     */
    protected void stopMonitoring()
    {
        if (log.isInfoEnabled())
        {
            log.info("stopMonitoring");
        }
        EASSectionTableMonitor monitor = this.m_easMonitor;
        this.m_easMonitor = null;

        if (null != monitor)
        {
            monitor.dispose();
        }
    }

    /**
     * Changes the emergency alert message processing state to the given state.
     * 
     * @param state
     *            the {@link EASState} instance representing the new message
     *            processing state
     */
    synchronized void changeState(final EASState state)
    {
        if (log.isInfoEnabled())
        {
            log.info("Changing state from:<" + this.m_easState.getStateString() + "> to:<" + state.getStateString() + ">");
        }
        this.m_easState = state;
    }

    /**
     * Returns the current state of emergency alert processing.
     * 
     * @return the concrete instance of {@link EASState} representing the
     *         current state of EA processing
     */
    synchronized EASState getCurrentState()
    {
        return this.m_easState;
    }

    /**
     * Sets the current operating state of EAS.
     * 
     * @param enable
     *            is <code>true</code> to enable operations per [SCTE 18], or
     *            <code>false</code> to disable operations and prevent alerts
     *            from being displayed
     */
    private synchronized void enableEAS(final boolean enable)
    {
        if (this.m_easEnabled != enable)
        {
            this.m_easEnabled = enable;
            if (log.isInfoEnabled())
            {
                log.info("EAS processing now " + (enable ? "enabled!" : "disabled!"));
            }

            if (enable) // disabled -> enabled
            {
                if (null != this.m_podManager)
                {
                    this.m_podManager.addPODListener(this);
                }

                EASMessage.resetLastReceivedSequenceNumber();
                EASState.resetActiveAlertSet();
                changeState(EASStateNotInProgress.INSTANCE);
                startMonitoring(); // start monitoring for section tables
            }
            else
            // enabled -> disabled
            {
                if (null != this.m_podManager)
                {
                    this.m_podManager.removePODListener(this);
                }

                stopMonitoring(); // stop monitoring for section tables and
                                  // release associated resources
                getCurrentState().stopPresentingAlert(); // stop any alert
                                                         // presentations and
                                                         // release associated
                                                         // resources
            }
        }
    }

    /**
     * Determines if the alert message should be accepted based on the receiving
     * device's geographic location.
     * <p>
     * <b>NOTE:</b> {@link PODManager} caches and persists the POD's host
     * parameters which includes the EA location. That cache can be updated upon
     * certain POD events. To avoid cache synchronization issues between the
     * POD, the <code>PODManager</code> cache, and the EAS module, the
     * <code>PODManager</code> is always queried for the EA location in case it
     * changed due to a POD event. However, when a subclass overrides
     * {@link #getGeographicLocation()} to return a non-null location -- that
     * location is used instead.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return <code>true</code> if the receiving device's geographical location
     *         <i>is not</i> encompassed by the alert's location code list;
     *         otherwise <code>false</code>
     * @see EASManagerImpl#getGeographicLocation()
     */
    private boolean hasExcludedLocation(final EASMessage message)
    {
        EASLocationCode geographicLocation = getGeographicLocation();

        if (geographicLocation == null)
        {
            try
            {
                byte[] eaLocation = this.m_podManager.getPOD().getHostParam(EASManagerImpl.CC_EA_LOCATION_FEATURE_ID);
                geographicLocation = new EASLocationCode(eaLocation);
                geographicLocation.validate();
            }
            catch (IllegalArgumentException e) // if EA location feature not
                                               // supported or location invalid
            {
                geographicLocation = EASLocationCode.UNSPECIFIED;
            }
            catch (NullPointerException e) // if a PODManager is not defined
            {
                geographicLocation = EASLocationCode.UNSPECIFIED;
            }
        }

        // flip the result of the location lookup so true is returned if our
        // location is not encompassed by the list
        return !message.isLocationIncluded(geographicLocation);
    }

    /**
     * Determines if the alert message should be accepted based on the currently
     * presented service.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return <code>true</code> if the currently presenting service is in the
     *         alert's exception service list; otherwise <code>false</code>
     */
    private boolean hasExcludedService(final EASMessage message)
    {
        ServiceContext[] serviceContexts = this.m_serviceContextFactory.getAllServiceContexts();
        boolean serviceExcluded = false;

        for (int i = 0; !serviceExcluded && i < serviceContexts.length; ++i)
        {
            try
            {
                ServiceContext context = serviceContexts[i];
                if (null != context)
                {
                    Service service = context.getService();
                    if (null != service)
                    {
                        if (message.isOutOfBandAlert())
                        {
                            int sourceID = ((OcapLocator) service.getLocator()).getSourceID();
                            serviceExcluded = message.isServiceExcluded(sourceID);
                        }
                        else
                        {
                            int frequency = ((OcapLocator) service.getLocator()).getFrequency();
                            int programNumber = ((OcapLocator) service.getLocator()).getProgramNumber();
                            serviceExcluded = message.isServiceExcluded(frequency, programNumber);
                        }
                    }
                }
            }
            catch (IllegalStateException e) // if service destroyed
            {
                continue;
            }
        }

        return serviceExcluded;
    }

    /**
     * Determines if the alert message should be accepted based on its
     * expiration time (<code>event_start_time</code> plus
     * <code>event_duration</code>) compared to the current time (i.e. has the
     * alert "already expired"?).
     * <p>
     * Although SCTE 18 notes that messages are processed without regard to the
     * <code>event_start_time</code> value, it's been observed on some platforms
     * that cached messages have been directed through EAS processing again,
     * even though the alert had expired (see enableTV Bugzilla issue #3329).
     * This non-SCTE compliant behavior filters out these "already expired"
     * alerts before they are erroneously presented to the user. It addresses an
     * edge condition not covered by duplicate <code>EAS_event_ID</code>
     * processing.
     * <p>
     * Strict SCTE-18 behavior can be restored by defining the MPE environment
     * variable, <code>OCAP.eas.filter.expired.alerts</code>, and setting it to
     * "<code>false</code>".
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return <code>true</code> if the alert has already expired; otherwise
     *         <code>false</code>
     */
    private boolean hasExpiredAlert(final EASMessage message)
    {
        return (EASManagerImpl.FILTER_EXPIRED_ALERTS && message.isAlertExpired());
    }

    /**
     * Determines if the alert message should be accepted based on its priority.
     * This determination is based on [SCTE 18] 5, Table 4, <i>Alert
     * Priority</i>. Also reserved alert priority values are treated the same as
     * the next highest priority.
     * <p>
     * <b>NOTE:</b> currently all non-zero alert priorities are flagged for
     * unconditional processing.
     * 
     * @param message
     *            a parsed and validated instance of {@link EASMessage}
     * @return <code>true</code> if the alert priority <i>is not</i>
     *         sufficiently high enough to process; otherwise <code>false</code>
     */
    private boolean hasInsufficientPriority(final EASMessage message)
    {
        int alertPriority = message.getAlertPriority();
        boolean insufficientPriority;

        if (alertPriority == EASMessage.ALERT_PRIORITY_TEST)
        {
            insufficientPriority = true; // discard test messages
        }
        else if (alertPriority <= EASMessage.ALERT_PRIORITY_LOW)
        {
            insufficientPriority = false; // placeholder for checking
                                          // access-controlled service
        }
        else if (alertPriority <= EASMessage.ALERT_PRIORITY_MEDIUM)
        {
            insufficientPriority = false; // placeholder for checking
                                          // pay-per-view or VOD event
        }
        else
        // (alertPriority <= EASMessage.ALERT_PRIORITY_HIGH)
        { // (alertPriority <= EASMessage.ALERT_PRIORIY_MAXIMUM)
            insufficientPriority = false; // unconditionally process alert
        }

        return insufficientPriority;
    }
}
