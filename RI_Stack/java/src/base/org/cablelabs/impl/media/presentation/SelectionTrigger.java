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

package org.cablelabs.impl.media.presentation;

import javax.media.Time;

import org.cablelabs.impl.media.session.Session;
import org.dvb.media.DVBMediaSelectControl;
import org.ocap.media.MediaPresentationEvaluationTrigger;

/**
 * This enumeration class defines reasons for starting a {@link Session}.
 */
public class SelectionTrigger extends MediaPresentationEvaluationTrigger
{
    /** Session started by {@link javax.media.Clock#syncStart(Time)}. */
    // MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE
    // public static final SelectionTrigger START = new
    // SelectionTrigger("START");
    /** Session started by {@link DVBMediaSelectControl} method. */
    // MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS
    // public static final SelectionTrigger SELECT = new
    // SelectionTrigger("SELECT");
    /**
     * Session started by
     * {@link MediaAccessConditionControl#conditionHasChanged(MediaPresentationEvaluationTrigger)
     * .
     */
    // Any of the MediaPresentationEvaluationTrigger's optional triggers
    // public static final SelectionTrigger ACCESS = new
    // SelectionTrigger("ACCESS");
    /** Session started because of failed {@link DVBMediaSelectControl} method. */
    public static final SelectionTrigger FALLBACK = new SelectionTrigger("FALLBACK");

    /** Session started because of switched digital re-tune. */
    public static final SelectionTrigger RETUNE = new SelectionTrigger("RETUNE");

    /** Session started because of resource availability change */
    public static final SelectionTrigger RESOURCES = new SelectionTrigger("RESOURCES");

    /** Session started because of conditional access update */
    public static final SelectionTrigger CONDITIONAL_ACCESS = new SelectionTrigger("CONDITIONAL_ACCESS");

    /** Session started because of service availability */
    public static final SelectionTrigger SERVICE = new SelectionTrigger("SERVICE");

    /** Session started because of a call to ServiceContext.select on an active presentation*/
    public static final SelectionTrigger SERVICE_CONTEXT_RESELECT = new SelectionTrigger("SERVICE_CONTEXT_RESELECT");

    /** Session change was due to an update in the format (to and from 2D or a 3D format)*/
    public static final SelectionTrigger FORMAT = new SelectionTrigger("FORMAT");

    /** Session started because of PMT change. */
    // MediaPresentationEvaluationTrigger.PMT_CHANGED
    // public static final SelectionTrigger PMT = new SelectionTrigger("PMT");

    private String name;

    protected SelectionTrigger(String triggerName)
    {
        this.name = triggerName;
    }

    public String toString()
    {
        return name;
    }
}
