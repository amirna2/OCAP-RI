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

/*
 * Created on Nov 8, 2006
 */
package org.cablelabs.impl.manager;

import java.io.File;

import org.cablelabs.impl.signalling.XAppEntry;

/**
 * The <code>AppDownloadManager</code> represents the process by which unbound
 * applications may be downloaded in accordance with OCAP 20.2.3.1.2 (ECN 913).
 * 
 * @author Aaron Kamienski
 */
public interface AppDownloadManager extends Manager
{
    /**
     * Initiates an asynchronous request to <i>download</i> the given
     * application. Downloading an application implies:
     * <ul>
     * <li>Tuning to the in-band transport.
     * <li>Downloading all files described by the ADF (if present, or all files
     * under the <code>base_directory</code>) to the host device's <i>cache</i>
     * <li>Authenticating the files, if specified.
     * <li>And finally notifying the given {@link Callback} of the result.
     * </ul>
     * 
     * If the app need not be downloaded (no OC transports) or it is determined
     * early on that it cannot be downloaded, then <code>null</code> should be
     * returned and the request ignored.
     * 
     * @param entry
     *            describes the application to be downloaded
     * @param authenticate
     *            if <code>true</code> then downloaded app must be
     *            pre-authenticated (and download should fail if this cannot be
     *            accomplished); if <code>false</code> then authentication may
     *            be performed lazily
     * @param stealTuner
     *            true if this download should forcibly steal the tuner from
     *            an application in case there are no free tuners
     * @param callback
     *            the <code>Callback</code> to notify upon download completion
     *            (includes either success or failure)
     * 
     * @return a <code>DownloadRequest</code>, via which a requested download
     *         may be cancelled; <code>null</code> if app need not be downloaded
     *         (or cannot)
     */
    DownloadRequest download(XAppEntry entry, boolean authenticate,
                             boolean stealTuner, Callback callback);

    /**
     * Interface via which an application may cancel an asynchronous
     * {@link AppDownloadManager#download download request}.
     * 
     * @author Aaron Kamienski
     */
    public static interface DownloadRequest
    {
        /**
         * Cancels an outstanding request.
         * 
         * @return <code>true</code> if the request has been cancelled;
         *         <code>false</code> if the request was already cancelled or
         *         completed (either {@link Callback#downloadSuccess
         *         successfully} or {@link Callback#downloadFailure
         *         unsuccessfully}.
         */
        boolean cancel();
    }

    /**
     * Defines the callback interface for asynchronous application download
     * requests. Callers of {@link AppDownloadManager#download} should provide
     * an implementation of this interface which will be invoked upon either
     * {@link #downloadSuccess successful} or {@link #downloadFailure failed}
     * completion of the request.
     * 
     * @author Aaron Kamienski
     */
    public static interface Callback
    {
        /**
         * Invoked to notify the download requester of a <i>failed</i> download.
         * 
         * @param reason
         *            the reason for the failure; one of
         *            {@link #GENERAL_FAILURE}, {@link #TUNING_FAILURE},
         *            {@link #DSMCC_FAILURE}, {@link #IO_FAILURE},
         *            {@link #AUTH_FAILURE}
         * @param msg
         *            further information regarding the failure
         */
        void downloadFailure(int reason, String msg);

        /**
         * Invoked to notify the download requestor of a <i>successful</i>
         * download.
         * 
         * @param app
         *            provides the requester with access to the downloaded app
         */
        void downloadSuccess(DownloadedApp app);

        int GENERAL_FAILURE = 0;

        int TUNING_FAILURE = 1;

        int DSMCC_FAILURE = 2;

        int IO_FAILURE = 3;

        int AUTH_FAILURE = 4;
    }

    /**
     * Defines the interface by which a download requester gains access to the
     * downloaded application.
     * 
     * @author Aaron Kamienski
     */
    public static interface DownloadedApp
    {
        /**
         * This method should be invoked when the consumer of the downloaded app
         * files no longer needs to the downloaded app.
         */
        void dispose();

        /**
         * Returns the base directory for the downloaded files in the local file
         * system.
         * 
         * @return the base directory for the downloaded files
         */
        File getBaseDirectory();
    }
}
