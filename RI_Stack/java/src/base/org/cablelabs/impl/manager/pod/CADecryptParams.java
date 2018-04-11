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

package org.cablelabs.impl.manager.pod;

import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.MediaDecodeParams;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.service.TransportStreamHandle;
import org.davic.mpeg.TransportStream;

/**
 * Immutable value container that defines parameters required for CA descrambling
 */

public class CADecryptParams
{
    /* Higher numbers imply higher priority */
    public static final int SERVICECONTEXT_PRIORITY = 60;
    public static final int JMF_PLAYER_PRIORITY = 50; 
    public static final int OBJECT_CAROUSEL_PRIORITY = 40;
    public static final int BUFFERING_PRIORITY = 30;
    public static final int SECTION_FILTERING_PRIORITY = 20;

    /* Handle type */
    public static final short SERVICE_DETAILS_HANDLE = 1;
    public static final short TRANSPORT_STREAM_HANDLE = 2; 
    
    /**
     * Construct the {@link CADecryptParams} instance from the specified
     * arguments.
     * 
     * @param edl
     *            - ED listener to receive events from the decrypt session
     * @param pids
     *            - <code>int</code> array of PIDs to decrypt.
     * @param priority
     *            - the priority of this decode. Lower numbers given higher
     *            priority
     */
    public CADecryptParams( final CASessionListener caListener, 
                               final ServiceDetailsExt serviceDetails, 
                               final int tunerId,
                               final int pids[], 
                               final short types[],
                               final int priority )
    {
        if (caListener == null) throw new IllegalArgumentException("null listener");
        if (pids == null) throw new IllegalArgumentException("null PID array");
        if (types == null) throw new IllegalArgumentException("null types array");

        this.caListener = caListener;
        this.handleType = CADecryptParams.SERVICE_DETAILS_HANDLE;
        this.serviceDetails = serviceDetails;
        this.transportStream = null;
        this.tunerId = tunerId;
        this.streamPids = Arrays.copy(pids);
        this.streamTypes = Arrays.copy(types);
        this.priority = priority;
    }

    /**
     * Construct the {@link CADecryptParams} instance from the specified
     * arguments.
     * 
     * @param edl
     *            - ED listener to receive events from the decrypt session
     * @param pids
     *            - <code>int</code> array of PIDs to decrypt.
     * @param priority
     *            - the priority of this decode. Lower numbers given higher
     *            priority
     */
    public CADecryptParams( final CASessionListener caListener, 
                               final TransportStream transportStream, 
                               final int tunerId,
                               final int pids[], 
                               final short types[],
                               final int priority )
    {
        if (caListener == null) throw new IllegalArgumentException("null listener");
        if (pids == null) throw new IllegalArgumentException("null PID array");
        if (types == null) throw new IllegalArgumentException("null types array");

        this.caListener = caListener;
        this.serviceDetails = null;
        this.handleType = CADecryptParams.TRANSPORT_STREAM_HANDLE;
        this.transportStream = transportStream;        	
        this.tunerId = tunerId;
        this.streamPids = Arrays.copy(pids);
        this.streamTypes = Arrays.copy(types);
        this.priority = priority;
    }
    
    public final int tunerId;
    
    public final CASessionListener caListener;
    
    public final short handleType;
    
    public final ServiceDetailsExt serviceDetails;
    
    public final TransportStream transportStream; 

    public final int streamPids[];

    public final short streamTypes[];

    public final int priority;

    public String toString()
    {
        return "MediaDecryptParams {service=" 
               + ((serviceDetails == null) ? "null" : serviceDetails.getLocator().toString())
               + ", transportStream="
               + ((transportStream == null) ? "null" : transportStream.toString())
               + ", tuner=" + tunerId
               + ", listener=" + caListener 
               + ", pids=" + Arrays.toString(streamPids)
               + ", priority=" + priority + "}";
    }
} // END class CADecryptParams
