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

/**
 * This class represents possible reasons to trigger an evaluation that leads to
 * the generation of an <code>{@link AlternativeMediaPresentationEvent}</code>
 * or a <code>{@link NormalMediaPresentationEvent}</code>. An application which
 * has a MonitorAppPermission("mediaAccess") can use predefined
 * <code>MediaPresentationEvaluationTrigger</code> or define its own
 * <code>MediaPresentationEvaluationTrigger</code> and indicate to the
 * implementation that presentation conditions have changed through the
 * <code>{@link MediaAccessConditionControl}</code>.
 * <p>
 * MANDATORY triggers: OCAP implementation SHALL be able to generate such
 * trigger independently of the monitor application. A monitor application
 * cannot generate such triggers exclusively. <br>
 * OPTIONAL triggers: such triggers MAY be generated by the OCAP implementation.
 * A monitor application can exclusively generate such triggers.
 * 
 * @see MediaAccessHandler
 */
public class MediaPresentationEvaluationTrigger
{
    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that the
     * broadcast PMT has changed. <br>
     * MANDATORY trigger
     */
    public final static MediaPresentationEvaluationTrigger PMT_CHANGED;

    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that access to
     * a resource has changed : lost or free resource. <br>
     * MANDATORY trigger
     */
    public final static MediaPresentationEvaluationTrigger RESOURCE_AVAILABILITY_CHANGED;

    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that a new
     * service has been selected. <br>
     * MANDATORY trigger
     */
    public final static MediaPresentationEvaluationTrigger NEW_SELECTED_SERVICE;

    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that new
     * service components have been selected via JMF control or via
     * ServiceContext. <br>
     * MANDATORY trigger
     */
    public final static MediaPresentationEvaluationTrigger NEW_SELECTED_SERVICE_COMPONENTS;

    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that the power
     * state has changed, e.g., switch to Software Standby. <br>
     * OPTIONAL trigger
     */
    public final static MediaPresentationEvaluationTrigger POWER_STATE_CHANGED;

    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that current
     * program event has changed. <br>
     * OPTIONAL trigger
     */
    public final static MediaPresentationEvaluationTrigger CURRENT_PROGRAM_EVENT_CHANGED;

    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that the user
     * preference for rating has been changed. <br>
     * OPTIONAL trigger
     */
    public final static MediaPresentationEvaluationTrigger USER_RATING_CHANGED;

    /**
     * <code>MediaPresentationEvaluationTrigger</code> indicating that program
     * event rating has changed. <br>
     * OPTIONAL trigger
     */
    public final static MediaPresentationEvaluationTrigger PROGRAM_EVENT_RATING_CHANGED;

    /**
     * Constructs a MediaPresentationEvaluationTrigger.
     */
    protected MediaPresentationEvaluationTrigger()
    {
        // This constructor is never used
        name = "Improperly constructed trigger";
        isOptional = false;
    }

    /**
     * Constructs a MediaPresentationEvaluationTrigger.
     */
    private MediaPresentationEvaluationTrigger(String name, boolean isOptional)
    {
        this.name = name;
        this.isOptional = isOptional;
    }

    private final String name;

    private final boolean isOptional;

    /**
     * Returns true if the trigger can be generated either by the OCAP
     * implementation or by the Monitor Application. Returns false if the
     * trigger is generated by the OCAP implementation.
     * 
     * @return true if the trigger can be generated either by the implementation
     *         or by the Monitor Application. <br>
     *         false if the trigger is generated by the OCAP implementation.
     */
    public boolean isOptional()
    {
        return isOptional;
    }

    public String toString()
    {
        return name;
    }

    static
    {
        PMT_CHANGED = new MediaPresentationEvaluationTrigger("PMT_CHANGED", false);
        RESOURCE_AVAILABILITY_CHANGED = new MediaPresentationEvaluationTrigger("RESOURCE_AVAILABILITY_CHANGED", false);
        NEW_SELECTED_SERVICE = new MediaPresentationEvaluationTrigger("NEW_SELECTED_SERVICE", false);
        NEW_SELECTED_SERVICE_COMPONENTS = new MediaPresentationEvaluationTrigger("NEW_SELECTED_SERVICE_COMPONENTS",
                false);
        POWER_STATE_CHANGED = new MediaPresentationEvaluationTrigger("POWER_STATE_CHANGED", true);
        CURRENT_PROGRAM_EVENT_CHANGED = new MediaPresentationEvaluationTrigger("CURRENT_PROGRAM_EVENT_CHANGED", true);
        USER_RATING_CHANGED = new MediaPresentationEvaluationTrigger("USER_RATING_CHANGED", true);
        PROGRAM_EVENT_RATING_CHANGED = new MediaPresentationEvaluationTrigger("PROGRAM_EVENT_RATING_CHANGED", true);
    }
}
