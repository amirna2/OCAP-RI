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
import javax.media.TransitionEvent;

import org.davic.mpeg.ElementaryStream;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.media.access.ComponentAuthorization;

/**
 * <code>MediaPresentationEvent</code> is a JMF event used as the parent class
 * of events indicating dynamic changes to the presentation of media components.
 * <p>
 * This event provides the trigger that leads to the generation of this event.
 */
public abstract class MediaPresentationEvent extends TransitionEvent
{
    /**
     * Constructor of MediaPresentationEvent
     * 
     * @see javax.media.TransitionEvent
     */
    protected MediaPresentationEvent(Controller from, int previous, int current, int target)
    {
        // This constructor is not used by the implementation because it does
        // not
        // include enough information to construct the event.
        super(from, previous, current, target);
        presentedStreams = new ElementaryStream[0];
        trigger = null;
        digital = false;
        sourceURL = null;
    }

    /**
     * Protected constructor for internal use only. Not for application use.
     */
    protected MediaPresentationEvent(Controller from, ComponentAuthorization authorization)
    {
        super(from, Controller.Started, Controller.Started, Controller.Started);
        this.presentedStreams = authorization.getAuthorizedStreams();
        this.trigger = authorization.getMediaPresentationEvaluationTrigger();
        this.digital = authorization.isDigital();
        this.sourceURL = authorization.getSourceURL();
    }

    /**
     * @return Returns true if the presented source is digital, false otherwise.
     */
    public boolean isSourceDigital()
    {
        return digital;
    }

    /**
     * @return Returns the URL of the source that is presented.
     */
    public OcapLocator getSourceURL()
    {
        return sourceURL;
    }

    /**
     * @return Returns the service components that are currently presented. If
     *         no service components is presented or the source is analog, null
     *         is returned.
     */
    public ElementaryStream[] getPresentedStreams()
    {
        return presentedStreams;
    }

    /**
     * @return Returns the trigger that leads to the generation of a
     *         MediaPresentationEvent.
     * @see MediaPresentationEvaluationTrigger
     */
    public MediaPresentationEvaluationTrigger getTrigger()
    {
        return trigger;
    }

    private final ElementaryStream[] presentedStreams;

    private final MediaPresentationEvaluationTrigger trigger;

    private final boolean digital;

    private final OcapLocator sourceURL;
}
