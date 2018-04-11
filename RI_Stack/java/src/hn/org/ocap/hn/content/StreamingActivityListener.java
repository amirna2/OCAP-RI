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
package org.ocap.hn.content;

import java.util.EventListener;

/**
 * This interface represents a listener for notification of streaming
 * being started, changed or ended
 */
public interface StreamingActivityListener extends EventListener
{

    /**
     * Reason for activity ended.
     */

    /*
     * The service vanished from the network.
     */
    public final static int ACTIVITY_END_SERVICE_VANISHED       = 1;

    /*
     * Resources needed to present the service have been removed.
     */
    public final static int ACTIVITY_END_RESOURCE_REMOVED       = 3;

    /*
     * Access to the service or some component of it has been withdrawn by the system.
     */
    public final static int ACTIVITY_END_ACCESS_WITHDRAWN       = 4;

    /*
     * The application or user requested that the presentation be stopped.
     */
    public final static int ACTIVITY_END_USER_STOP                      = 5;


    /*
     * The presentation was stopped due to a network timeout
     */
    public final static int ACTIVITY_END_NETWORK_TIMEOUT        = 11;

    /*
     * The presentation was terminated due to an unknown reason or for multiple reasons
     */
    public final static int ACTIVITY_END_OTHER                  = 255;

    /**
     * contentTypes.
     */
    public final static int CONTENT_TYPE_ALL_RESOURCES = 0;
    public final static int CONTENT_TYPE_LIVE_RESOURCES = 1;
    public final static int CONTENT_TYPE_RECORDED_RESOURCES = 2;

    /**
     * Notifies the <code>StreamingActivityListener</code> when content begins
     * streaming the content to the home network in response to a request
     * for <code>ContentItem</code> streaming.
     *
     * @param channel The <code>ContentItem</code> requested.
     * @param activityID A unique value assigned by the implementation for this
     *      streaming activity.
     * @param URI the URI of the <res> that's been requested.
     * @param tuner The <code>NetworkInterface</code> representing the tuner
     *      the content is being streamed from if used. The value will be null
     *		if no tuner is used.
     */
    public void notifyStreamingStarted(ContentItem contentItem,
                                       int activityID,
                                       String URI,
                                       org.davic.net.tuning.NetworkInterface tuner);

    /**
     * Notifies the <code>StreamingActivityListener</code> when streaming parameter
     * on this activity such as tuning parameter changes
     *
     * @param channel The <code>ContentItem</code> associated with this activity
     * @param activityID A unique value assigned by the implementation for this
     *      streaming activity.
     * @param URI the URI of the <res> that's been requested.
     * @param tuner The <code>NetworkInterface</code> representing the tuner
     *      the content is being streamed from if used. The value will be null
     *		if no tuner is used.
     */
    public void notifyStreamingChange(ContentItem contentItem,
                                       int activityID,
                                       String URI,
                                       org.davic.net.tuning.NetworkInterface tuner);
    /**
     * Notifies the <code>StreamingActivityListener</code> when content
     * streaming to the home network in response to a request
     * for <code>ContentItem</code> streaming has ended.
     *
     * @param contentItem The <code>ContentItem</code> requested.
     * @param activityID A unique value assigned by the implementation for this
     *      streaming activity.
     * @param reasonOfEnd Unique value defined in this class for the end of this
     *      streaming activity
     */
    public void notifyStreamingEnded(ContentItem contentItem,
                                     int activityID,
                                     int reasonOfEnd);
}

