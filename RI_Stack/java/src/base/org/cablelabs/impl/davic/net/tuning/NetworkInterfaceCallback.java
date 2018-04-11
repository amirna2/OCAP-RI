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

package org.cablelabs.impl.davic.net.tuning;

/**
 * This interface defines callback methods that are invoked by the
 * {@link org.davic.net.tuning.NetworkInterface} when tuning occur. These
 * callbacks are synchronous, as opposed to the
 * {@link org.davic.net.tuning.NetworkInterfaceListener} method, which is called
 * asynchronously using the event listener model.
 * <p>
 * Calls to mutator methods of the {@link ExtendedNetworkInterface} from any of
 * the notify methods defined in this interface throw an
 * {@link IllegalStateException}.
 * 
 * @author schoonma
 * @author Todd Earles (added re-tune support)
 */
public interface NetworkInterfaceCallback
{
    /**
     * Called when a tune is initiated on a {@link ExtendedNetworkInterface}.
     * This is called before any state change in the network interface.
     * 
     * @param ni
     *            The {@link ExtendedNetworkInterface} that is about to be
     *            tuned.
     * @param tuneInstance
     *            An object indicating the most recent tune. This will be the
     *            same as the return value from ExtendedNetworkInterface.tune(),
     *            but can also be used as a indication of the tune starting at
     *            this point.
     */
    void notifyTunePending(ExtendedNetworkInterface ni, Object tuneInstance);

    /**
     * Called when a tune completes for any reason, successful or not. This is
     * called after the tune completes and the state of the network interface
     * reflects the completed tune but before
     * {@link org.davic.net.tuning.NetworkInterfaceTuningOverEvent} is posted.
     * 
     * @param ni
     *            The {@link ExtendedNetworkInterface} for which tuning
     *            completed.
     * @param tuneInstance
     *            An object indicating the most recent tune.
     * @param success
     *            True if the tune completed successfully; otherwise, false.
     * @param isSynced
     *            True if the NI is synced to an MPEG stream and ready to
     *            section filter and/or present A/V.
     */
    void notifyTuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean isSynced);

    /**
     * Called when a re-tune is initiated on a {@link ExtendedNetworkInterface}.
     * This is called before any state change in the network interface.
     * 
     * @param ni
     *            The {@link ExtendedNetworkInterface} that is about to be
     *            re-tuned.
     * @param tuneInstance
     *            An object indicating the most recent tune. This will be the
     *            same as the return value from ExtendedNetworkInterface.tune(),
     *            but can also be used as a indication of the tune starting at
     *            this point.
     */
    void notifyRetunePending(ExtendedNetworkInterface ni, Object tuneInstance);

    /**
     * Called when a re-tune completes for any reason, successful or not. This
     * is called after the re-tune completes and the state of the network
     * interface reflects the completed re-tune.
     * 
     * @param ni
     *            The {@link ExtendedNetworkInterface} for which the re-tune
     *            completed.
     * @param tuneInstance
     *            An object indicating the most recent tune.
     * @param success
     *            True if the tune completed successfully; otherwise, false.
     * @param isSynced
     *            True if the NI is synced to an MPEG stream and ready to
     *            section filter and/or present A/V
     */
    void notifyRetuneComplete(ExtendedNetworkInterface ni, Object tuneInstance, boolean success, boolean isSynced);

    /**
     * Called when the {@link ExtendedNetworkInterface} is explicitly un-tuned.
     * 
     * @param ni
     *            The {@link ExtendedNetworkInterface} that has become un-tuned.
     * @param tuneInstance
     *            An object indicating the most recent tune.
     */
    void notifyUntuned(ExtendedNetworkInterface ni, Object tuneInstance);

    /**
     * Called when the {@link ExtendedNetworkInterface} acquires sync.
     * 
     * @param ni
     *            The {@link ExtendedNetworkInterface} which synced.
     * @param tuneInstance
     *            An object indicating the most recent tune.
     */
    void notifySyncAcquired(ExtendedNetworkInterface ni, Object tuneInstance);

    /**
     * 
     * @param ni
     * @param tuneInstance
     *            An object indicating the most recent tune.
     */
    void notifySyncLost(ExtendedNetworkInterface ni, Object tuneInstance);
}
