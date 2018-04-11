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

/**
 *
 */
package org.cablelabs.impl.manager.recording;

import java.util.Vector;

import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.davic.resources.ResourceStatusEvent;
import org.dvb.application.AppID;
import org.ocap.dvr.BufferingRequest;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceManagerImpl;
import org.cablelabs.impl.davic.resources.ResourceOfferListener;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DisableBufferingListener;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.NetManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.RecordingManager;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.ocap.dvr.TimeShiftBufferResourceUsageImpl;
import org.cablelabs.impl.security.PersistentStoragePermission;
import org.cablelabs.impl.service.javatv.selection.DVRServiceContextImpl;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Implements <code>BufferingRequest</code>
 * 
 * @author jspruiel
 * 
 */
public class BufferingRequestImpl extends BufferingRequest implements ResourceOfferListener,
        TimeShiftWindowChangedListener, DisableBufferingListener
{
    private static RecordingManagerInterface recordingManager;

    /**
     * Constructor.
     * 
     * @param brm
     *            Reference to the BufferingRequestManager.
     * @param sync
     *            The specified lock to use.
     * @param service
     *            The service to buffer.
     * @param minDuration
     *            The minimum duration to buffer.
     * @param maxDuration
     *            The maximum duration to buffer.
     * @param efap
     *            The caller's file access permissions.
     */
    public BufferingRequestImpl(BufferingRequestManager brm, Object sync, CallerContext ctx, AppID appID,
            Service service, long minDuration, long maxDuration, ExtendedFileAccessPermissions efap)
    {
        m_buffReqManager = brm;
        m_sync = sync;
        m_service = service;
        m_minDuration = minDuration;
        m_maxDuration = maxDuration;
        
        m_logPrefix = "BR 0x" + Integer.toHexString(this.hashCode()) + ": ";

        m_owner = appID;
        m_br = this;
        m_TSBRUsage = new TimeShiftBufferResourceUsageImpl(ctx, service, this);

        // Make sure the efap is setup correctly with world access is set.
        if (null == efap)
        {
            efap = new ExtendedFileAccessPermissions(true, false, false, false, false, false, new int[0], new int[0]);
        }
        else if (false == efap.hasReadWorldAccessRight())
        {
            efap.setPermissions(true, efap.hasWriteWorldAccessRight(), efap.hasReadOrganisationAccessRight(),
                    efap.hasWriteOrganisationAccessRight(), efap.hasReadApplicationAccessRight(),
                    efap.hasWriteApplicationAccessRight(), efap.getReadAccessOrganizationIds(),
                    efap.getWriteAccessOrganizationIds());
        }

        m_efap = efap;
        // initial state.
        changeState(m_inActiveState);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#getService()
     */
    public Service getService()
    {
        return m_service;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#setService(javax.tv.service.Service)
     */
    public void setService(Service service) throws IllegalArgumentException
    {
        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "setService");
            }

            // Per JavaDoc - If this parameter is null, no write permissions are
            // given to this request.
            // if the calling application does not have one of the
            // write ExtendedFileAccessPermissions set by the
            // createInstance or setExtendedFileAccessPermissions methods.
            if (m_efap == null || hasWriteExtFileAccPerm() == false)
            {
                throw new java.lang.SecurityException();
            }

            m_buffReqManager.validateService(service);

            m_service = service;

            if (m_state != null)
            {
                m_state.setService(service, this);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#getMinimumDuration()
     */
    public long getMinimumDuration()
    {
        return m_minDuration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#setMinimumDuration(long)
     */
    public void setMinimumDuration(long minDuration)
    {
        synchronized (m_sync)
        {
            // Per JavaDoc - If this parameter is null, no write permissions are
            // given to this request.
            // if the calling application does not have one of the
            // write ExtendedFileAccessPermissions set by the
            // createInstance or setExtendedFileAccessPermissions methods.
            if (m_efap == null || hasWriteExtFileAccPerm() == false)
            {
                throw new java.lang.SecurityException();
            }

            // Throws: java.lang.IllegalArgumentException -
            // If the parameter is greater than the current value and Host
            // device does not have enough space to meet the request,
            // or if the parameter is greater than the maximum duration set by
            // the createInstance or setMaximumDuration methods,
            // or if the parameter is less than the duration returned by
            // OcapRecordingManager.getSmallestTimeShiftDuration().

            if (((minDuration > m_minDuration) && false)
                    || // This case is handled below see
                       // m_tswc.setMinimumDuration(minDuration)
                    (minDuration > m_maxDuration)
                    || (minDuration < getRecordingManager().getSmallestTimeShiftDuration()))
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "setMinimumDuration failed. minDur = " + minDuration);
                }
                throw new IllegalArgumentException("minDuration");
            }

            // Store the new duration. The next call
            // may fail, but will be retried until cancel
            // is called. However, the new duration is stored.
            m_minDuration = minDuration;

            // Try to set the time shift buffer to a
            // larger value. The method throws an IllegaArguementException
            // if disk space is insufficient for the request.
            if (m_tswc != null)
            {
                m_tswc.setMinimumDuration(minDuration);
            }

            // If the TimeShiftWindowClient shutdown because it could
            // not seamlessly resize its buffer, detach and wait for
            // state change event TSWSTATE_TUNED.

            // If the TimeShiftWindowClient is already in BUFFSHUTDOWN,
            // and event for this state will not be received.

            // TODO: I am not convinced that I do not need to
            // check the state.
            if (m_tswc != null)
            {
                long state = m_tswc.getState();
                if (state == TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN)
                {
                    changeState(m_wfTunedStateImpl);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#getMaxDuration()
     */
    public long getMaxDuration()
    {
        return m_maxDuration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#setMaxDuration(long)
     */
    public void setMaxDuration(long duration)
    {

        synchronized (m_sync)
        {

            // Per JavaDoc - If this parameter is null, no write permissions are
            // given to this request.

            // if the calling application does not have one of the
            // write ExtendedFileAccessPermissions set by the
            // createInstance or setExtendedFileAccessPermissions methods.
            if (m_efap == null || hasWriteExtFileAccPerm() == false)
            {
                throw new java.lang.SecurityException();
            }

            if ((duration < 0) || (duration < m_minDuration)
                    || (duration < getRecordingManager().getSmallestTimeShiftDuration()))
            {
                throw new java.lang.IllegalArgumentException("maxDuration");
            }

            m_maxDuration = duration;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#getExtendedFileAccessPermissions()
     */
    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions()
    {
        return m_efap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.dvr.BufferingRequest#setExtendedFileAccessPermissions(org.ocap
     * .storage.ExtendedFileAccessPermissions)
     */
    public void setExtendedFileAccessPermissions(ExtendedFileAccessPermissions efap)
    {
        if (efap == null)
        {
            throw new IllegalArgumentException("setExtendedFileAccessPermission() Argument efap is null");
        }
        synchronized (m_sync)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            AppID creator = (AppID) (ccm.getCurrentContext().get(CallerContext.APP_ID));
            if (m_owner.equals(creator) == false)
            {
                throw new SecurityException();
            }
            // make sure world access is set.
            if (false == efap.hasReadWorldAccessRight())
            {
                efap.setPermissions(true, efap.hasWriteWorldAccessRight(), efap.hasReadOrganisationAccessRight(),
                        efap.hasWriteOrganisationAccessRight(), efap.hasReadApplicationAccessRight(),
                        efap.hasWriteApplicationAccessRight(), efap.getReadAccessOrganizationIds(),
                        efap.getWriteAccessOrganizationIds());

            }
            m_efap = efap;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.dvr.BufferingRequest#getAppID()
     */
    public AppID getAppID()
    {
        return m_owner;
    }

    /**
     * Cancels the buffering request.
     * 
     * @param list
     *            The list from which the object is removed.
     */
    void cancelBufferingRequest(Vector list)
    {
        synchronized (m_sync)
        {
            if (m_state != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "cancelBufferingRequest");
                }
                m_state.cancelBufferingRequest(list);
            }
        }
    }

    /**
     * Initiates actual buffering.
     * 
     */
    void startBuffering()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "startBuffering");
        }

        // java.lang.SecurityException - if the calling application does not
        // have the "file" element set to true in its permission request file.
        SecurityUtil.checkPermission(new PersistentStoragePermission());

        synchronized (m_sync)
        {
            // Verify that stack wide buffering is not disabled
            if (getRecordingManager().isBufferingEnabled() == false)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "startBuffering: buffering disabled.");
                }
                m_br.changeState(m_disabledState);
                return;
            }
            try
            {
                // If the TimeShiftWindowManager fails to return
                // a TimeShiftClient should I keep the old one or
                // act as if this called never occurred? I am choosing
                // the later, hence the need for tswcTemp and swapping.

                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "startBuffering: Attempting to acquire a " 
                                         + m_minDuration + '/' + m_maxDuration 
                                         + " TSW..." );
                }
                
                // acquire a new TimeShiftClient for new service, it may return
                // null.
                m_tswc = m_tsm.getTSWByDuration(m_service, m_minDuration, m_maxDuration,
                        TimeShiftManager.TSWUSE_BUFFERING, m_TSBRUsage, this, 
                        TimeShiftManager.LISTENER_PRIORITY_LOW );

                if (m_tswc == null)
                {
                    return;
                }
                
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "startBuffering: Got " + m_tswc);
                }

                // check state. If TimeShiftWindowClient state does not change
                // it will send an event.
                int state = m_tswc.getState();

                switch (state)
                {
                    case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                    case TimeShiftManager.TSWSTATE_BUFFERING:
                    {
                        try
                        {
                            m_tswc.attachFor(TimeShiftManager.TSWUSE_BUFFERING);
                            changeState(m_activeStateImpl);
                        }
                        catch (Exception e)
                        {
                            // m_br.changeState(m_retryStateImpl);
                            if (log.isInfoEnabled())
                            {
                                log.info(m_logPrefix + "startBuffering: READY_TO_BUFFER or BUFFERING - attachFor failed", e);
                            }
                            m_tswc.release();
                            m_tswc = null;
                        }
                        break;
                    }
                    case TimeShiftManager.TSWSTATE_TUNE_PENDING:
                    case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
                    case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
                    case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
                    case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                    {
                        // Don't really expect later state, but if it were
                        // to occur we still go this state.
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "startBuffering: " + TimeShiftManager.stateString[state] + " " + this);
                        }
    
                        if (log.isInfoEnabled())
                        {
                            log.info(m_logPrefix + "startBuffering: switching to m_wfTunedStateImpl.");
                        }
                        changeState(m_wfTunedStateImpl);
                        break;
                    }
                    default:
                    {
                        // Otherwise if the call to attachFor fails, forget the time
                        // shift client.
                        m_tswc.release();
                        m_tswc = null;
    
                        // go into the re-honor mode.
                        NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
                        NetworkInterfaceManagerImpl nimpl = (NetworkInterfaceManagerImpl) nm.getNetworkInterfaceManager();
    
                        // TODO: Change magic number to something more appropriate
                        // and check proper
                        // priority.
                        nimpl.addResourceOfferListener(this, 6);
                        changeState(m_retryStateImpl);
                        break;
                    }
                } // END switch (state)
            }
            catch (NoFreeInterfaceException e)
            {
                // request for new TimeShiftWindowClient failed.
                // go into the re-honor mode. The m_tswc field is already null.
                NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
                NetworkInterfaceManagerImpl nimpl = (NetworkInterfaceManagerImpl) nm.getNetworkInterfaceManager();

                // TODO: Change magic number to something more appropriate and
                // check proper
                // priority.
                nimpl.addResourceOfferListener(this, 6);

                changeState(m_retryStateImpl);

                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "startBuffering failed - no free interface", e);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.TimeShiftWindowChangedListener#tswChanged(
     * org.cablelabs.impl.manager.TimeShiftWindowChangedEvent)
     */
    public void tswStateChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent tswce)
    {
        synchronized (m_sync)
        {
            // If the event was inflight when the tswc was replaced and was
            // already dispatched, it may arrive late. We don't want this
            // BufferingRequest to respond to an event from the old tswc.
            if (tswc.equals(m_tswc) == false)
            {
                return;
            }
            m_state.tswChanged(tswc, tswce);
        }

    }

    public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo cci)
    {
        // Buffering requests are not affected by CCI (time restriction is prevented at playback)
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.davic.resources.ResourceOfferListener#offerResource
     * (java.lang.Object)
     */
    public boolean offerResource(Object resource)
    {
        synchronized (m_sync)
        {
            return m_state.offerResource(resource);
        }
    }

    // Description copied from DisableBufferingListener
    public void notifyBufferingDisabledStateChange(boolean enabled)
    {
        synchronized (m_sync)
        {
            m_state.bufferingDisableStateChange(enabled);
        }
    }

    /**
     * Gets the TimeShiftWindowClient.
     * 
     * @return A TimeShiftWindowClient
     */
    public TimeShiftWindowClient getTSWClient()
    {
        return m_tswc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.davic.resources.ResourceStatusListener#statusChanged(org.davic.resources
     * .ResourceStatusEvent)
     */
    public void statusChanged(ResourceStatusEvent rse)
    {
        /* no op */
    }

    /**
     * Called to set the new state.
     * 
     * @param brState
     *            new State
     */
    public void changeState(BufferingRequestState brState)
    {
        synchronized (m_sync)
        {

            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "changeState: " + brState);
            }
            m_state = brState;
        }
    }

    /**
     * Checks that the caller has write ExtendedFileAccessPermissions.
     * 
     */
    boolean hasWriteExtFileAccPerm() throws SecurityException
    {
        if (SecurityUtil.isPrivilegedCaller())
        {
            return true;
        }

        OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if ((null == osm) || (null == ccm))
        {
            SystemEventUtil.logRecoverableError(new Exception("Failed to obtain OcapSecurityManager (" + osm
                    + ") or CallerContextManager (" + ccm + ") Object ref"));
            throw new SecurityException(); // no detail message!!!
        }

        if (osm.hasWriteAccess(m_owner, m_efap, (AppID) (ccm.getCurrentContext().get(CallerContext.APP_ID)),
                OcapSecurityManager.FILE_PERMS_RECORDING))
        {
            return true;
        }

        return false;
    }

    private RecordingManagerInterface getRecordingManager()
    {
        synchronized (BufferingRequestImpl.class)
        {
            if (recordingManager == null)
            {
                recordingManager = (RecordingManagerInterface) ((org.cablelabs.impl.manager.RecordingManager) ManagerManager.getInstance(org.cablelabs.impl.manager.RecordingManager.class)).getRecordingManager();
            }

            return recordingManager;
        }
    }

    /**
     * Base class based on the Gang-of-Four state pattern.
     * 
     * @author jspruiel
     */
    abstract class BufferingRequestState
    {
        public void setService(Service service, BufferingRequestImpl br)
        {
        }

        /**
         * @param list
         *            The list of active <code>BufferingRequest</code> objects.
         */
        void cancelBufferingRequest(Vector list)
        {
            // if the calling application does not have write permission
            // for the request as determined by the
            // ExtendedFileAccessPermissions
            // returned by the getExtendedFileAccessPermissions method in
            // the parameter, or if the calling application does not
            // have MonitorAppPermission("handler.recording").
            if (hasWriteExtFileAccPerm() == false)
            {
                throw new java.lang.SecurityException();
            }

            SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));

            // Remove from list
            list.remove(m_br);
            m_br.changeState(m_inActiveState);

            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CallerContext ctx = ccm.getCurrentContext();

            if (ctx != ccm.getSystemContext())
            {
                // I don't like the idea of holding a reference
                // to the actual caller context.
                m_buffReqManager.removeAppFromActiveList(ctx);
            }
        }

        /**
         * Called when a network interface becomes available.
         * 
         * @param resource
         *            The resource that is being offered.
         * @return true if the resource is needed.
         */
        public boolean offerResource(Object resource)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "offerResource:event = " + resource);
            }

            return false;
        }

        /**
         * @param tswc 
         * @param tswce
         *            An event indicating a <code>TimeShiftWindow</code> state
         *            change.
         */
        public void tswChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent tswce)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "tswChanged:event = " + tswce);
            }
        }

        /**
         * Handle notification that the stack wide buffering state has changed
         */
        public void bufferingDisableStateChange(boolean enabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "bufferingDisable = " + enabled);
            }
        }
    }

    /**
     * Fields relevant to the <code>TimeShiftClient</code> are null.
     * 
     * @author jspruiel
     * 
     */
    class InactiveState extends BufferingRequestState
    {
        final String m_logPrefix = this.m_logPrefix + "InactiveState: ";

        /**
         * Cancels the buffering request.
         * 
         * @param list
         *            The list from which the object is removed.
         */
        void cancelBufferingRequest(Vector list)
        {
        }

        /**
         * Handle notification that the stack wide buffering state has changed
         */
        public void bufferingDisableStateChange(boolean enabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "bufferingDisable = " + enabled);
            }
        }

        public boolean offerResource(Object resource)
        {
            return false;
        }
    }

    /**
     * This state represents an outstanding buffering request when system wide
     * buffering has been disabled (by a call to
     * OcapRecordingManager.disableBuffering(). See ECR 929 Rev 3
     */
    class DisabledState extends BufferingRequestState
    {
        final String m_logPrefix = this.m_logPrefix + "DisabledState: ";

        /**
         * Handle notification that the stack wide buffering state has changed
         */
        public void bufferingDisableStateChange(boolean enabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "bufferingDisable = " + enabled);
            }
            if (enabled == true)
            {
                startBuffering();
            }
        }
    }

    /**
     * The state when the underlying TimeShiftClient is buffering.
     * 
     * @author jspruiel
     * 
     */
    class ActiveState extends BufferingRequestState
    {
        final String m_logPrefix = this.m_logPrefix + "ActiveState: ";

        public void setService(Service service, BufferingRequestImpl br)
        {
            try
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "setService: " + service);
                }
                m_tswc.release();
                m_tswc = null;
            }
            catch (Exception ge)
            {
                // we shouldn't get an exception.
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "setService failed", ge);
                }
            }

            // gets the new timeshift window and attempts
            // buffering with new service.
            startBuffering();
        }

        /**
         * Handle notification that the stack wide buffering state has changed
         */
        public void bufferingDisableStateChange(boolean enabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "bufferingDisableStateChange - enabled: " + enabled);
            }

            if (enabled == false)
            {
                try
                {
                    m_tswc.release();
                    m_tswc = null;
                }
                catch (Exception ge)
                {
                    // we shouldn't get an exception.
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "bufferingDisableStateChange failed", ge);
                    }
                }

                m_br.changeState(m_disabledState);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState#cancelBufferingRequest(java.util.Vector)
         */
        void cancelBufferingRequest(Vector list)
        {
            try
            {
                // Remove from list, change to inActiveState
                super.cancelBufferingRequest(list);

                m_tswc.release();
                m_tswc = null;
            }
            catch (Exception ge)
            {
                // we shouldn't get an exception.
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "cancelBufferingRequest failed", ge);
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState#offerResource(java.lang.Object)
         */
        public boolean offerResource(Object resource)
        {
            // I am active, therefore I must already have a resource.
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState
         * #tswChanged(org.cablelabs.impl.manager.TimeShiftWindowChangedEvent)
         */
        public void tswChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent tswce)
        {
            long state = tswce.getNewState();
            int reason = tswce.getReason();

            if (state == TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN)
            {
                // detach from the time shift window and then
                // wait for state change event TSWSTATE_READY_TO_BUFFER.
                if (reason == TimeShiftManager.TSWREASON_PIDCHANGE)
                {
                    // Nothing to do here - just logging
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "received TimeShiftManager.TSWSTATE_BUFFSHUTDOWN with reason " + reason);
                    }
                }
                try
                {
                    m_tswc.detachFor(TimeShiftManager.TSWUSE_BUFFERING);
                }
                catch (Exception ex)
                {
                    // we shouldn't get an exception.
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "tswChanged BUFFSHUTDOWN - detachFor BUFFERING failed", ex);
                    }
                }

                m_br.changeState(m_wfTunedStateImpl);
            }
            // This event means the network interface is no longer
            // available. When in this state we deploy a strategy
            // to restart the buffering activity using a new
            // TimeShiftWindowClient.

            if ((tswce.getNewState() == TimeShiftManager.TSWSTATE_INTSHUTDOWN)
                    || (tswce.getNewState() == TimeShiftManager.TSWSTATE_IDLE))
            {
                // prior state could have been BUFFERING, so call detach.
                // calling detach if not attached should not be an issue.
                // detach from the TimeShiftWindow
                // transition to the retry state which will attempt to honor
                // buffering request.
                try
                {
                    m_tswc.release();
                    m_tswc = null;
                }
                catch (Exception ex)
                {
                    // we shouldn't get an exception.
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "tswChanged INTSHUTDOWN or NOTTUNED - release failed", ex);
                    }
                    m_tswc.release();
                    m_tswc = null;
                }
                // For service remap we need to wait for another tune
                if (reason == TimeShiftManager.TSWREASON_SERVICEREMAP)
                {
                    // Nothing to do here - just logging
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "reason = TimeShiftManager.TSWREASON_SERVICEREMAP");
                    }
                    m_br.changeState(m_wfTunedStateImpl);
                }
                else
                {

                    // request for new TimeShiftWindowClient failed.
                    // go into the re-honor mode.
                    NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
                    NetworkInterfaceManagerImpl nimpl = (NetworkInterfaceManagerImpl) nm.getNetworkInterfaceManager();

                    // TODO: Change magic number to something more appropriate
                    // and check proper
                    // priority.
                    nimpl.addResourceOfferListener(m_br, 6);

                    m_br.changeState(m_retryStateImpl);
                }
            }
        }
    }

    /**
     * Represent a <code>BufferingRequest</code> in re-honor mode.
     * 
     * @author jspruiel
     * 
     */
    class RetryState extends BufferingRequestState
    {
        final String m_logPrefix = this.m_logPrefix + "RetryState: ";
        
        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState#setService(javax.tv.service.Service)
         */
        public void setService(Service service, BufferingRequestImpl br)
        {
            // Service already validated
            // We are not attached so no need to detach.
            // Get a new time shift window.
            startBuffering();
        }

        /**
         * Handle notification that the stack wide buffering state has changed
         */
        public void bufferingDisableStateChange(boolean enabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "bufferingDisable = " + enabled);
            }

            if (enabled == false)
            {
                // We are not attached, so nothing to clean up
                m_br.changeState(m_disabledState);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState#offerResource(java.lang.Object)
         */
        public boolean offerResource(Object resource)
        {
            synchronized (m_sync)
            {
                if (resource instanceof NetworkInterface)
                {
                    // request for new TimeShiftWindowClient failed.
                    // go into the re-honor mode.
                    NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
                    NetworkInterfaceManagerImpl nimpl = (NetworkInterfaceManagerImpl) nm.getNetworkInterfaceManager();

                    // TODO: Change magic number to something more appropriate
                    // and check proper
                    // priority.
                    nimpl.removeResourceOfferListener(m_br);

                    // start buffering and accept resource.
                    m_br.startBuffering();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * The state when the <code>BufferingRequest</code> is waiting for tuning to
     * complete.
     * 
     * @author jspruiel
     */
    class WFTunedState extends BufferingRequestState
    {
        final String m_logPrefix = this.m_logPrefix + "WFTunedState: ";

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState#setService(javax.tv.service.Service)
         */
        public void setService(Service service, BufferingRequestImpl br)
        {
            // forget current m_tswc before calling startBuffering.
            m_tswc.release();
            m_tswc = null;

            startBuffering();
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState#cancelBufferingRequest(java.util.Vector)
         */
        void cancelBufferingRequest(Vector list)
        {
            // Remove from list, change to inActiveState
            super.cancelBufferingRequest(list);

            try
            {
                m_tswc.release();
                m_tswc = null;
            }
            catch (Exception ge)
            {
                // we shouldn't get an exception.
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "cancelBufferingRequest - release failed", ge);
                }
            }
        }

        /**
         * Handle notification that the stack wide buffering state has changed
         */
        public void bufferingDisableStateChange(boolean enabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "bufferingDisable = " + enabled);
            }

            if (enabled == false)
            {
                try
                {
                    m_tswc.release();
                    m_tswc = null;
                }
                catch (Exception ge)
                {
                    // we shouldn't get an exception.
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "bufferingDisableStateChange - release failed", ge);
                    }
                }

                m_br.changeState(m_disabledState);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.manager.recording.BufferingRequestImpl.
         * BufferingRequestState#offerResource(java.lang.Object)
         */
        public boolean offerResource(Object resource)
        {
            return false;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.recording.BufferingRequestState#tswChanged
         * (org.cablelabs.impl.manager.recording.BufferingRequestImpl,
         * org.cablelabs.impl.manager.TimeShiftWindowChangedEvent)
         */
        public void tswChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent tswce)
        {
            // This event means the network interface is no longer
            // available. When in this state we deploy a strategy
            // to restart the buffering activity.
            if (tswce.getNewState() == TimeShiftManager.TSWSTATE_INTSHUTDOWN)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "tswChanged:TSWSTATE_INTSHUTDOWN");
                }
                // already detached
                // request for new TimeShiftWindowClient failed.
                // go into the re-honor mode.
                NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
                NetworkInterfaceManagerImpl nimpl = (NetworkInterfaceManagerImpl) nm.getNetworkInterfaceManager();

                // TODO: Change magic number to something more appropriate and
                // check proper
                // priority.
                nimpl.addResourceOfferListener(m_br, 6);
                // MUST remove listener and reference
                // to time shift window client.
                m_tswc.release();
                m_tswc = null;

                m_br.changeState(m_retryStateImpl);
            }
            else if (tswce.getNewState() == TimeShiftManager.TSWSTATE_READY_TO_BUFFER)
            {
                // attempt attach on tune
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "tswChanged:TSWSTATE_READY_TO_BUFFER");
                    }

                    tswc.attachFor(TimeShiftManager.TSWUSE_BUFFERING);
                    m_br.changeState(m_activeStateImpl);
                }
                catch (Exception e)
                {
                    // we shouldn't get an exception but if we do,
                    // release time shift window client resources
                    // and reference.
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "tswChanged READY_TO_BUFFER attachfor failed", e);
                    }
                    m_tswc.release();
                    m_tswc = null;
                    m_br.changeState(m_retryStateImpl);
                }
            }
            else if (tswce.getNewState() == TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN)
            {
                // Detach
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "tswChanged:TSWSTATE_BUFFSHUTDOWN");
                }
                // MUST remove listener and reference
                // to time shift window client.
                m_tswc.release();
                m_tswc = null;

                m_br.changeState(m_retryStateImpl);
            }
        }
    }

    // Following are the BufferingRequestImpl fields

    /**
     * A reference to buffering request and accessed by the state machine.
     */
    private BufferingRequestImpl m_br;

    /**
     * A reference to the InActiveState object.
     */
    private InactiveState m_inActiveState = new InactiveState();

    /**
     * A reference to the InActiveState object.
     */
    private DisabledState m_disabledState = new DisabledState();

    /**
     * A reference to the WFTunedState object.
     */
    private WFTunedState m_wfTunedStateImpl = new WFTunedState();

    /**
     * A reference to the RetryState object.
     */
    private RetryState m_retryStateImpl = new RetryState();

    /**
     * A reference to the ActiveState object.
     */
    private ActiveState m_activeStateImpl = new ActiveState();

    /**
     * A reference to the <code>TimeShiftManager</code> object.
     */
    private TimeShiftManager m_tsm = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);

    /**
     * A reference to the callers' file access permission object.
     * 
     */
    private ExtendedFileAccessPermissions m_efap;

    /**
     * The service to be buffered.
     */
    private Service m_service;

    /**
     * The underlying <code>TimeShiftWindowClient</code>.
     */
    private TimeShiftWindowClient m_tswc;

    /**
     * The callers' <code>AppID</code>.
     */
    private AppID m_owner;

    /**
     * The resource usage.
     */
    private TimeShiftBufferResourceUsageImpl m_TSBRUsage = null;

    /**
     * The concrete state implementation.
     */
    private BufferingRequestState m_state;

    /**
     * The monitor object.
     */
    private Object m_sync;

    /**
     * The minimum duration for this buffer request.
     */
    private long m_minDuration;

    /**
     * The maximum duration for this buffer request.
     */
    private long m_maxDuration;

    /**
     * The <code>BufferingRequestManager</code>
     */
    private BufferingRequestManager m_buffReqManager;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(BufferingRequestImpl.class.getName());

    private final String m_logPrefix;
}
