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

package org.ocap.media;

import javax.media.Controller;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.util.Arrays;
import org.davic.mpeg.ElementaryStream;

/**
 * <code>AlternativeMediaPresentationEvent</code> is a JMF event generated to
 * indicate that an "alternative" content is presented during the media
 * presentation of a service.
 * <p>
 * Alternative content is defined as content that is not actually part of the
 * service.
 * <p>
 * <code>AlternativeMediaPresentationEvent</code> notification is generated :
 * <li>When alternative media content presentation begins;
 * <li>During the presentation of a service, if any of the service components
 * presented are replaced by alternative content;
 * <li>During the presentation of a service, if an alternative media content was
 * presented and an evaluation leads to a new alternative media content
 * presentation.
 */
public abstract class AlternativeMediaPresentationEvent extends MediaPresentationEvent implements
        NotPresentedMediaInterface
{
    private static final Logger log = Logger.getLogger(AlternativeMediaPresentationEvent.class);

    /**
     * Constructor of MediaPresentationEvent
     * 
     * @see MediaPresentationEvent
     */
    protected AlternativeMediaPresentationEvent(Controller from, int previous, int current, int target)
    {
        // This constructor is not used by the implementation because it does
        // not
        // include enough information to construct the event.
        super(from, previous, current, target);
        notPresentedStreams = new ElementaryStream[0];
        denialReasons = new int[0];
        if (log.isInfoEnabled())
        {
            log.info("constructed no-reason AlternativeMediaPresentationEvent - not presented streams: " + Arrays.toString(notPresentedStreams));
        }
    }

    /**
     * Protected constructor for internal use only. Not for application use.
     */
    protected AlternativeMediaPresentationEvent(Controller from, ComponentAuthorization authorization)
    {
        super(from, authorization);
        this.notPresentedStreams = authorization.getDeniedStreams();

        denialReasons = new int[notPresentedStreams.length];
        for (int i = 0; i < notPresentedStreams.length; i++)
        {
            denialReasons[i] = authorization.getDenialReasons(notPresentedStreams[i]);
        }
        if (log.isInfoEnabled())
        {
            log.info("constructed AlternativeMediaPresentationEvent - not presented streams: " + Arrays.toString(notPresentedStreams) + ", denial reasons: " + Arrays.toString(denialReasons));
        }
    }

    /**
     * @return Returns the subset of explicitely (by Application request) or
     *         implicitely (by the Player itself) service components that were
     *         selected and which presentation was not possible.
     */
    public ElementaryStream[] getNotPresentedStreams()
    {
        return notPresentedStreams;
    }

    /**
     * @return Returns a bit mask of reasons that lead to the non presentation
     *         of the given service component. The reasons are defined in
     *         <code>{@link AlternativeMediaPresentationReason}</code>
     *         )interface.
     * @param es
     *            a not presented service component.
     */
    public int getReason(ElementaryStream es)
    {
        int reason = 0;
        for (int i = 0; i < notPresentedStreams.length; i++)
        {
            if (es.equals(notPresentedStreams[i]))
            {
                reason = denialReasons[i];
                break;
            }
        }
        return reason;
    }

    private final ElementaryStream[] notPresentedStreams;

    private final int[] denialReasons;
}
