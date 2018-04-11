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

package org.ocap.hardware;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DownloadManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.GraphicsManager;
import org.cablelabs.impl.manager.HostManager;
import org.cablelabs.impl.manager.SignallingManager;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.ocap.hardware.ExtendedHost;
import org.cablelabs.impl.util.ExtendedSystemEventManager;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

import org.ocap.system.MonitorAppPermission;
import org.ocap.system.event.RebootEvent;
import org.ocap.system.event.SystemEventManager;

/**
 * This class represents the host terminal device and provides access to the
 * Host ID, raw image data, the power state of the host and a
 * java.util.Enumeration of references to VideoOutputPort instances. See also
 * org.ocap.OcapSystem to get the singleton instance.
 */
public class Host
{

    /**
     * A constructor of this class. An application must use the
     * {@link Host#getInstance} method to create an instance.
     */
    protected Host()
    {
        // Make sure the ED manager framework and GraphicsMAnager are active.
        ManagerManager.getInstance(EventDispatchManager.class);
        ManagerManager.getInstance(GraphicsManager.class);

        getHostPowerMode(new EDListener()
        {
            public synchronized void asyncEvent(int eventCode, int eventData1, int eventData2)
            {
                // parameter sanity check
                if ((eventCode == FULL_POWER) || (eventCode == LOW_POWER))
                {
                    notifyPowerChange(eventCode);
                }
            }
        });

        setDefaultPowerMode();
    }

    /**
     * This method returns a singleton system-wide instance of the Host class.
     * Since we now have the possibility of having the DeviceSettings extension,
     * ManangerManager is now used to get the correct HostManager
     *
     *
     *@return a singleton Host instance.
     */
    public static Host getInstance()
    {
        return HOSTSINGLETON;

    }

    /**
     * Get a human-readable string representing the ID of this Host. This should
     * be a string that could be read over the phone to an MSO that uniquely
     * identifies the Host.
     *
     * @return id String host id
     */
    public String getID()
    {
        // get the environment variable for the unique box identifier
        String id = MPEEnv.getEnv("MPE.SYS.ID");
        return id;
    }

    /**
     * @return the current power mode of the box (for example LOW_POWER).
     *
     * @see #FULL_POWER
     * @see #LOW_POWER
     */
    public int getPowerMode()
    {
        // Call native method to acquire power mode.
        return getHostPowerMode(null);
    }

    /**
     * Transition the power mode of the system to the given mode.
     * <p>
     *
     * If the power mode is already in the target mode, this method SHALL do
     * nothing.
     *
     * Setting host power mode to low-power SHALL NOT disrupt any ongoing
     * recording.
     *
     * In devices where a separate power mode is maintained for standby
     * recordings, setting the power mode to low-power SHALL transition to
     * standby-recording power mode when a recording is in progress.
     * <p>
     *
     * A change of power mode SHALL be communicated to installed
     * {@link PowerModeChangeListener}s.
     *
     * @param mode
     *            The new power mode for the system.
     *
     * @throws IllegalArgumentException
     *             if <i>mode</i> is not one of {@link Host#FULL_POWER} or
     *             {@link Host#LOW_POWER}
     * @throws SecurityException
     *             if the caller does not have
     *             <code>MonitorAppPermission("powerMode")</code>
     */
    public void setPowerMode(int mode) // ENC 1297
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("powerMode"));
        if (this.getPowerMode() == mode)
        {
            return;
        }

        setPowerModeNoPerm(mode);
    }

    protected void setPowerModeNoPerm(int mode) throws IllegalArgumentException // ECN
                                                                                // 1297
    {
        /*
         * Don't do any permission checking or anything else specific to the
         * base here. This code is used by extensions which have their own
         * permissions, etc.
         */
        if (mode == Host.FULL_POWER || mode == Host.LOW_POWER)
        {
            if (!setHostPowerMode(mode))
            {
                RuntimeException re = new RuntimeException("setHostPowerMode failed");
                SystemEventUtil.logCatastrophicError(re);
                throw re;
            }
        }
        else
        {
            throw new IllegalArgumentException("mode is not FULL_POWER or LOW_POWER");
        }

    }

    protected void setDefaultPowerMode() // ECN 1297
    {
        // ECN 1297 default power mode is low power. Do not do security check
        // since this is part
        // of startup, not an application call and therefore is a trusted actor
        // (and the permission
        // may not always be defined).
        setPowerModeNoPerm(Host.LOW_POWER);
    }

    /**
     * Gets the MAC address used by the Host for reverse channel unicast
     * communications.  This value SHALL match the value the Host would
     * use in DSG mode for a DHCP request. The format of the String returned
     * SHALL be six pairs of characters where each pair represents a
     * hexadecimal byte value of the address and where each pair is separated
     * by a colon.  For example "0D:0E:0F:10:11:12".  The first byte
     * representation in the String starting at location 0 SHALL be the most
     * significant byte in the address.
     *
     * @return MAC address of the Host.
     */
    public String getReverseChannelMAC()
    {
        // get the environment variable for the RF MAC Address
        String macId = MPEEnv.getEnv("MPE.SYS.RFMACADDR");
        return macId;
    }

    /**
     * Power mode constant for normal "on" mode.
     */
    public static final int FULL_POWER = 1;

    /**
     * Power mode constant for "standby" mode.
     */
    public static final int LOW_POWER = 2;

    /**
     * Adds the PowerModeChangeListener to be called (
     * {@link PowerModeChangeListener#powerModeChanged} when the power mode of
     * the box changes (for example when the user presses the Power button).
     *
     * @param l
     *            is an instance implementing PowerModeChangeListener whose
     *            powerModeChanged method will be called when the power mode of
     *            the Host Device changes.
     */
    public void addPowerModeChangeListener(PowerModeChangeListener l)
    {
        // Add the listener to the list of listeners for this caller context.
        synchronized (lock)
        {
            // Get the data for the current caller context.
            CCData data = getCCData(ccManager.getCurrentContext());

            data.listeners = EventMulticaster.add(data.listeners, l);
        }
    }

    /**
     * Removes the previously-added PowerModeChangeListener.
     *
     * @param l
     *            is the PowerModeChangeListener to disable. Does nothing if l
     *            was never added, has been removed, or is null.
     */
    public void removePowerModeChangeListener(PowerModeChangeListener l)
    {
        // Remove the listener from the list of listeners for this caller
        // context.
        synchronized (lock)
        {
            // Retreive the caller's associated context data.
            CCData data = getCCData(ccManager.getCurrentContext());

            data.listeners = EventMulticaster.remove(data.listeners, l);
        }
    }

    /**
     * This method returns a java.util.Enumeration of
     * VideoOutputPort instances representing all video output ports
     * physically present on the device.  The returned Enumeration
     * SHALL reflect a 1 to 1 mapping between VideoOutputPort
     * instances and physical video output ports.  For example, 2 HDMI
     * output ports driven by the same controller would report two distinct
     * VideoOutputPort instances of type AV_OUTPUT_PORT_TYPE_HDMI.
     * This method SHALL report all VideoOutputPort instances regardless of the
     * enabled or disabled status of the port.
     *
     * @return the java.util.Enumeration of VideoOutputPort instances.
     */
    public java.util.Enumeration getVideoOutputPorts()
    {
        return org.cablelabs.impl.ocap.hardware.VideoOutputPortImpl.getVideoOutputPorts();
    }

    /**
     * <p>
     * This method initiates a reboot of the Host device. The method caller
     * shall have the MonitorAppPermission("reboot").
     * </p>
     * <p>
     * Note that the
     * {@link org.ocap.system.event.SystemEventListener#notifyEvent} method
     * SHALL be called before the initiated reboot is performed by the Host
     * device. The monitor application MAY clean up resources in the
     * SystemEventListener.notifyEvent method call.
     * After the SystemEventListener.notifyEvent method call returns, the
     * Host device SHALL continue the reboot following the boot process described in
     * the <i>Boot Process</i> Section of this specification.
     * </p>
     *
     * @throws SecurityException
     *             if the caller does not have the
     *             MonitorAppPermission("reboot").
     */
    public void reboot()
    {
        // Check for necessary permissions.
        SecurityUtil.checkPermission(new MonitorAppPermission("reboot"));

        reboot(RebootEvent.REBOOT_BY_TRUSTED_APP);
    }

    /**
     * Used to implement {@link #reboot()} and {@link ExtendedHost#reboot(int)}
     */
    protected void reboot(final int reason)
    {
        // Log the reboot event.
        // Using a PrivilegedAction to allow use of non-app event typeCode.
        AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                ExtendedSystemEventManager sem = (ExtendedSystemEventManager) SystemEventManager.getInstance();
                if (null != sem) sem.log(new RebootEvent(reason, ""), REBOOT_TIMEOUT);
                return null;
            }
        });

        // Call native method to complete reboot operation.
        hostReboot();
    }

    /**
     * <p>
     * This method initiates a download of the operating software in the Host
     * as specified by [CCIF2.0].
     *
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("codeDownload").
     *
     */
    public void codeDownload()
    {
        // check for proper security permissions
        SecurityUtil.checkPermission(new MonitorAppPermission("codeDownload"));

        // call the DownloadManager to instigate the code download
        DownloadManager dl = (DownloadManager) ManagerManager.getInstance(DownloadManager.class);
        dl.startDownload();
    }

    /**
     * Query whether there is an AC Outlet on the STB.
     *
     * NOTE: AC Outlet refers to an external power plug on the STB. That is, a
     * device such as a VCR can plug into the STB for power.
     *
     * @return true if there is an AC Outlet, else false.
     *
     **/
    public boolean isACOutletPresent()
    {
        String acOutletPresent = MPEEnv.getEnv("MPE.SYS.ACOUTLET");
        return ((acOutletPresent != null) && (acOutletPresent.toUpperCase().compareTo("TRUE")) == 0) ? true : false;
    }

    /**
     * Query whether power to the AC Outlet, if present, is currently On (true)
     * or Off (false)
     *
     * NOTE: AC Outlet refers to an external power plug on the STB. That is, a
     * device such as a VCR can plug into the STB for power.
     *
     * @return The current AC Outlet status (false = Off, true = On).
     *
     * @throws java.lang.IllegalStateException
     *             if this method is called when there is no AC Outlet.
     *
     **/
    public boolean getACOutlet()
    {
        if (!isACOutletPresent())
        {
            throw new IllegalStateException("AC outlet is not present");
        }

        return getHostACOutlet();
    }

    /**
     * Switch power to AC Outlet, if present, On (true) or Off (false)
     *
     * NOTE: AC Outlet refers to an external power plug on the STB. That is, a
     * device such as a VCR can plug into the STB for power.
     *
     * @param enable
     *            The power setting for the AC Outlet.
     *
     * @throws java.lang.IllegalStateException
     *             if this method is called when there is no AC Outlet.
     *
     **/
    public void setACOutlet(boolean enable)
    {
        if (!isACOutletPresent())
        {
            throw new IllegalStateException("AC outlet is not present");
        }

        setHostACOutlet(enable);

        return;
    }

    /**
     * Returns capability of RF bypass control on the host.
     *
     * @return true if the host can control RF bypass on/off, else false.
     *
     **/
    public boolean getRFBypassCapability()
    {
        String rfBypassCap = MPEEnv.getEnv("MPE.SYS.RFBYPASS");
        return ((rfBypassCap != null) && (rfBypassCap.toUpperCase().compareTo("TRUE") == 0)) ? true : false;
    }

    /**
     * Queries whether RF Bypass is currently enabled. If RF Bypass is enabled,
     * the incoming RF signal is directly routed to the RF output port when the
     * host is in a stand by mode, thereby totally bypassing the host.
     *
     * @return true if RF Bypass is currently enabled, else false. If the host
     *         doesn't support RF bypass, false returns.
     *
     **/
    public boolean getRFBypass()
    {
        // initially assume RF Bypass not supported
        boolean rfBypassEnabled = false;

        if (getRFBypassCapability())
        {
            // RF Bypass is supported, so query current state from native
            rfBypassEnabled = getHostRFBypass();
        }

        return rfBypassEnabled;
    }

    /**
     * Enables or disables RF Bypass. If RF Bypass is enabled, the incoming RF
     * signal is directly routed to the RF output port when the host is in a
     * stand by mode, thereby totally bypassing the host.
     *
     * @param enable
     *            If true, RF Bypass will be enabled. Otherwise it will be
     *            disabled.
     *
     * @throws java.lang.IllegalStateException
     *             if the host doesn't support RF bypass.
     *
     **/
    public void setRFBypass(boolean enable)
    {
        if (!getRFBypassCapability())
        {
            throw new IllegalStateException("RF Bypass not supported by this Host");
        }

        setHostRFBypass(enable);

        return;
    }

    /**
     * Removes the XAIT saved to persistent storage. If no XAIT is present in
     * persistent storage this method does nothing successfully. This method
     * SHALL NOT affect a cached XAIT and any running applications.
     *
     * @throws SecurityException
     *             if the calling application is not granted
     *             MonitorAppPermission("storage").
     */
    public void removeXAIT()
    {
        // Check for necessary permissions.
        SecurityUtil.checkPermission(new MonitorAppPermission("storage"));

        SignallingManager sm = (SignallingManager) ManagerManager.getInstance(SignallingManager.class);
        sm.deletePersistentXait();
    }

    // ******************** Private Implementation *****************

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
        synchronized (lock)
        {
            // Retrieve the data for the caller context
            CCData data = (CCData) cc.getCallbackData(lock);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to ccList.
            if (data == null)
            {
                data = new CCData();
                cc.addCallbackData(data, lock);
                ccList = CallerContext.Multicaster.add(ccList, cc);
            }
            return data;
        }
    }

    /**
     * Notify the listeners of power mode change. A vector of listeners is used
     * to notify the registered clients.
     *
     * @param mode
     *            the new power mode
     */
    private void notifyPowerChange(final int mode)
    {
        final CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if (ccList != null)
        {
            ccList.runInContext(new Runnable()
            {
                public void run()
                {
                    CCData data = getCCData(ccm.getCurrentContext());
                    PowerModeChangeListener pl;
                    if (data != null && ((pl = data.listeners) != null))
                    {
                        pl.powerModeChanged(mode);
                    }
                }
            });
        }
    }

    /**
     * Per caller context data
     */
    private class CCData implements CallbackData
    {
        /**
         * The listeners is used to keep track of all objects that have
         * registered to be notified of powerModeChanged events.
         */
        public volatile PowerModeChangeListener listeners;

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        { /* empty */
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        { /* empty */
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (lock)
            {
                // Remove the current application from the list of caller
                // contexts.
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                cc.removeCallbackData(lock);
                listeners = null;
            }
        }
    }

    // ******************** Private Variables *****************

    private CallerContextManager ccManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * Multicast list of caller context objects for tracking listeners per
     * caller context. At any point in time this list will be the complete list
     * of caller context objects that have an assigned CCData.
     */
    private volatile CallerContext ccList = null;

    /**
     * General purpose lock. Private lock used to avoid using <code>this</code>
     * for synchronization.
     */
    private Object lock = new Object();

    /**
     * singleton instance of the <code>Host</code> class.
     */
    // Added for findbugs issues fix - start
    private static Host HOSTSINGLETON = ((HostManager) ManagerManager.getInstance(HostManager.class)).getHostInstance();

    // Added for findbugs issues fix - end
    // ******************** Native Methods ********************

    /**
     * Native method to reboot this host device.
     */
    protected static native void hostReboot();

    /**
     * Native method to acquire the current power mode.
     *
     * @return current power mode, which must be one of the following constants:
     *         1 = Full power 2 = Low power
     */
    private static native int getHostPowerMode(EDListener edListener);

    /**
     * Native method to set the current power mode.
     *
     * @return current power mode, which must be one of the following constants:
     *         1 = Full power 2 = Low power
     */
    private static native boolean setHostPowerMode(int mode);

    /**
     * Native method to set the AC outlet state.
     */
    private static native void setHostACOutlet(boolean enable) throws IllegalStateException;

    /**
     * Native method to get the current state of the AC outlet.
     *
     * @return <code>true</code> if the AC outlet is on, <code>false</code>
     *         otherwise
     */
    private static native boolean getHostACOutlet() throws IllegalStateException;

    /**
     * Checks whether RF bypass is enabled or disabled
     *
     * @return <code>true</code> if RF bypass is enabled, <code>false</code> if
     *         RF bypass is disabled.
     */
    private static native boolean getHostRFBypass();

    /**
     * Enables or disables RF bypass.
     *
     * @param enable
     *            <code>true</code> to enable RF bypass, <code>false</code> to
     *            disable RF bypass
     */
    private static native void setHostRFBypass(boolean enable);

    /**
     * Timeout, specified in milliseconds, to wait for {@link RebootEvent}
     * dispatch to complete before continuing on with the reboot process. Should
     * be defined as anything between 5000 and 60000 milliseconds.
     *
     * @see "OCAP-1.0 21.2.1.19.1 Reboot Handling"
     */
    private static final long REBOOT_TIMEOUT = MPEEnv.getEnv("OCAP.reboot.timeout", 60000L);

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
