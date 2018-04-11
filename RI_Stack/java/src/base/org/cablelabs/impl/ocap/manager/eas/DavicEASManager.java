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

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.manager.eas.message.EASMessage;
import org.davic.mpeg.TransportStream;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.ocap.mpeg.PODExtendedChannel;

import org.cablelabs.impl.manager.Manager;

/**
 * An singleton instance of this class provides an implementation of the
 * Emergency Alert System (EAS) module that uses the DAVIC Section Filtering API
 * for capturing emergency alert section tables arriving on in-band and
 * out-of-band channels.
 * 
 * @author Dave Beidle
 * @version $Revision$
 */
public class DavicEASManager extends EASManagerImpl
{
    private static final Logger log = Logger.getLogger(DavicEASManager.class);

    /**
     * An instance of this class monitors the currently tuned in-band transport
     * stream for MPEG-2 section tables containing cable emergency alert
     * messages.
     */
    private class EASInBandMonitor extends EASSectionTableMonitor implements NetworkInterfaceListener
    {
        private NetworkInterfaceManager m_niManager;

        /**
         * Constructs a new instance of the receiver with the given
         * <code>listener</code> assigned to receive notifications of EAS
         * section table acquisition.
         * 
         * @param listener
         *            the {@link EASSectionTableListener} instance to receive
         *            EAS section table notifications
         */
        public EASInBandMonitor(final EASSectionTableListener listener)
        {
            super(listener);
            this.m_niManager = NetworkInterfaceManager.getInstance();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.davic.net.tuning.NetworkInterfaceListener#receiveNIEvent(org.
         * davic.net.tuning.NetworkInterfaceEvent)
         */
        public void receiveNIEvent(NetworkInterfaceEvent anEvent)
        {
            if (anEvent instanceof NetworkInterfaceTuningOverEvent
                    && ((NetworkInterfaceTuningOverEvent) anEvent).getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED)
            {
                EASMessage.resetLastReceivedSequenceNumber();
                EASState.resetActiveAlertSet();
                start((NetworkInterface) anEvent.getSource());
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#dispose()
         */
        protected void dispose()
        {
            // Remove network interface listeners. TODO: Should we watch all
            // Network interfaces?
            NetworkInterface[] networkInterfaces = this.m_niManager.getNetworkInterfaces();
            networkInterfaces[0].removeNetworkInterfaceListener(this);

            // Stop section filters.
            super.stop();
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#
         * isOutOfBandAlert()
         */
        protected boolean isOutOfBandAlert()
        {
            return false; // always an in-band monitor
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#start()
         */
        protected boolean start()
        {
            // Ensure this instance is a network interface listener TODO: does
            // this inherently listen to all interfaces?
            NetworkInterface[] networkInterfaces = this.m_niManager.getNetworkInterfaces();
            networkInterfaces[0].removeNetworkInterfaceListener(this);
            networkInterfaces[0].addNetworkInterfaceListener(this);

            // Find at least one that is tuned TODO: Should we watch all Network
            // interfaces?
            for (int i = 0; i < networkInterfaces.length; ++i)
            {
                if (start(networkInterfaces[i]))
                {
                    break;
                }
            }

            return true; // TODO: Is it possible that none of them are tuned at
                         // this point?
        }

        /**
         * Starts EAS in-band section filtering on the currently tuned transport
         * stream using the given network interface.
         * 
         * @param ni
         *            a {@link NetworkInterface} present on the host
         * @return <code>true</code> if in-band section filtering was
         *         successfully started on the transport stream; otherwise
         *         <code>false</code>
         */
        protected boolean start(final NetworkInterface ni)
        {
            TransportStream stream = ni.getCurrentTransportStream();
            if (log.isInfoEnabled()) 
            {
                log.info("Start IB section filtering on NI:<" + ni + "> stream:<" + ni.getLocator() + ">");
            }
            return (stream != null) && super.start(stream);
        }
    }

    /**
     * An instance of this class monitors the POD Extended Channel for MPEG-2
     * section tables containing out-of-band cable emergency alert messages.
     */
    private class EASOutOfBandMonitor extends EASSectionTableMonitor
    {

        /**
         * Constructs a new instance of the receiver with the given
         * <code>listener</code> assigned to receive notifications of EAS
         * section table acquisition.
         * 
         * @param listener
         *            the {@link EASSectionTableListener} instance to receive
         *            EAS section table notifications
         */
        public EASOutOfBandMonitor(EASSectionTableListener listener)
        {
            super(listener);
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#dispose()
         */
        protected void dispose()
        {
            super.stop(); // stop section filters
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#
         * isOutOfBandAlert()
         */
        protected boolean isOutOfBandAlert()
        {
            return true; // always an out-of-band monitor
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.ocap.manager.eas.EASSectionTableMonitor#start()
         */
        protected boolean start()
        {
            if (log.isInfoEnabled()) 
            {
                log.info("Start OOB section filtering on POD extended channel");
            }
            return super.start(PODExtendedChannel.getInstance());
        }
    }

    // Class Methods

    /**
     * Returns a new instance of this class to satisfy the basic {@link Manager}
     * contract. <code>ManagerManager</code> ensures this instance is treated as
     * a singleton instance.
     * 
     * @return a new instance of the receiver
     */
    public static Manager getInstance()
    {
        return new DavicEASManager();
    }

    // Constructors

    /**
     * Constructs a new instance of the receiver.
     */
    protected DavicEASManager()
    {
        // intentionally left empty - no public constructors
        if (log.isInfoEnabled()) 
        {
            log.info("DavicEASManager instantiated");
        }
    }

    // Instance Methods

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASManagerImpl#createInBandMonitor()
     */
    protected EASSectionTableMonitor createInBandMonitor()
    {
        return new EASInBandMonitor(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.ocap.manager.eas.EASManagerImpl#createEASOutOfBandMonitor
     * ()
     */
    protected EASSectionTableMonitor createOutOfBandMonitor()
    {
        return new EASOutOfBandMonitor(this);
    }
}
