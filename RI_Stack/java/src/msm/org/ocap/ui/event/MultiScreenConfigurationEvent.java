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

package org.ocap.ui.event;

/**
 * A <code>MultiScreenConfigurationEvent</code> is used to report changes to the
 * global state of the <code>MultiScreenManager</code> instance or the state of
 * some display <code>HScreen</code> with respect to the per-platform or some
 * per-display multiscreen configuration, respectively, or to changes to a
 * specific <code>MultiScreenConfiguration</code> instance.
 * 
 * <p>
 * The following types of changes SHALL cause the generation of this event:
 * </p>
 * 
 * <ul>
 * <li>The currently active per-platform multiscreen configuration as determined
 * by the <code>MultiScreenManager</code> changes from one multiscreen
 * configuration to another multiscreen configuration;</li>
 * <li>The currently active per-display multiscreen configuration as determined
 * by some display <code>HScreen</code> changes from one multiscreen
 * configuration to another multiscreen configuration;</li>
 * <li>The set of screens associated with a
 * <code>MultiScreenConfiguration</code> changes (i.e., a screen is added or
 * removed from the multiscreen configuration);</li>
 * </ul>
 * 
 * @author Glenn Adams
 * @since MSM I01
 * 
 **/
public class MultiScreenConfigurationEvent extends MultiScreenEvent
{
    /**
     * A change to the currently active per-platform or some per-display
     * <code>MultiScreenConfiguration</code> as determined by the
     * <code>MultiScreenManager</code> or some display <code>HScreen</code> has
     * been initiated, in which case the value returned by
     * <code>getSource()</code> SHALL be the affected
     * <code>MultiScreenManager</code> or display <code>HScreen</code>, and the
     * value returned by <code>getRelated()</code> SHALL be the subsequently
     * active <code>MultiScreenConfiguration</code>.
     * 
     * <p>
     * A <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code> event SHALL NOT be
     * dispatched to an application that has not been granted
     * <code>MonitorAppPermission("multiscreen.configuration")</code>.
     * </p>
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_CHANGING = MULTI_SCREEN_CONFIGURATION_FIRST + 0;

    /**
     * The currently active per-platform or some per-display
     * <code>MultiScreenConfiguration</code> as determined by the
     * <code>MultiScreenManager</code> or some display <code>HScreen</code> has
     * changed, in which case the value returned by <code>getSource()</code>
     * SHALL be the affected <code>MultiScreenManager</code> or display
     * <code>HScreen</code>, and the value returned by <code>getRelated()</code>
     * SHALL be the previously active <code>MultiScreenConfiguration</code>.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_CHANGED = MULTI_SCREEN_CONFIGURATION_FIRST + 1;

    /**
     * The set of screens associated with a
     * <code>MultiScreenConfiguration</code> has changed, with a new screen
     * having been added, in which case the value returned by
     * <code>getSource()</code> SHALL be the affected
     * <code>MultiScreenConfiguration</code>, and the value returned by
     * <code>getRelated()</code> SHALL be the newly added <code>HScreen</code>.
     * 
     * <p>
     * Except during the interval between the last dispatching of an
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code> event and the generation
     * of a corresponding <code>MULTI_SCREEN_CONFIGURATION_CHANGED</code> event,
     * a screen SHALL NOT be added to and a
     * <code>MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED</code> event SHALL NOT be
     * generated for a multiscreen configuration that is the current
     * per-platform or some current per-display multiscreen configuration.
     * </p>
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED = MULTI_SCREEN_CONFIGURATION_FIRST + 2;

    /**
     * The set of screens associated with a
     * <code>MultiScreenConfiguration</code> has changed, with an existing
     * screen having been removed, in which case the value returned by
     * <code>getSource()</code> SHALL be the affected
     * <code>MultiScreenConfiguration</code>, and the value returned by
     * <code>getRelated()</code> SHALL be the newly removed <code>HScreen</code>
     * .
     * 
     * <p>
     * Except during the interval between the last dispatching of an
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code> event and the generation
     * of a corresponding <code>MULTI_SCREEN_CONFIGURATION_CHANGED</code> event,
     * a screen SHALL NOT be removed from and a
     * <code>MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED</code> event SHALL NOT be
     * generated for a multiscreen configuration that is the current
     * per-platform or some current per-display multiscreen configuration.
     * </p>
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED = MULTI_SCREEN_CONFIGURATION_FIRST + 3;

    /**
     * Last event identifier assigned to
     * <code>MultiScreenConfigurationEvent</code> event identifiers.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_LAST = MULTI_SCREEN_CONFIGURATION_FIRST + 3;

    /**
     * Construct an <code>MultiScreenConfigurationEvent</code>.
     * 
     * @param source
     *            A reference to a <code>MultiScreenManager</code> instance, a
     *            display <code>HScreen</code> instance, or a
     *            <code>MultiScreenConfiguration</code> instance in accordance
     *            with the specific event as specified above.
     * 
     * @param id
     *            The event identifier of this event, the value of which SHALL
     *            be one of the following:
     *            <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code>,
     *            <code>MULTI_SCREEN_CONFIGURATION_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED</code>, or
     *            <code>MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED</code>.
     * 
     * @param related
     *            A reference to a <code>MultiScreenConfiguration</code>
     *            instance or an <code>HScreen</code> instance in accordanced
     *            with the specific event as specified above.
     * 
     * @since MSM I01
     **/
    public MultiScreenConfigurationEvent(Object source, int id, Object related)
    {
        super(source, id);
    }

    /**
     * Obtain a related object associated with this event.
     * 
     * @return The related object instance of this event, the value of which
     *         SHALL be one of the following as determined by the specific event
     *         type: a reference to a <code>MultiScreenConfiguration</code>
     *         instance or a reference to an <code>HScreen</code> instance.
     * 
     * @since MSM I01
     **/
    public Object getRelated()
    {
        return null;
    }

}
