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
 * A <code>MultiScreenContextEvent</code> is used to report a change to a
 * <code>MultiScreenContext</code> to interested listeners.
 * 
 * <p>
 * The following types of changes cause the generation of this event:
 * </p>
 * 
 * <ul>
 * <li>Change of associated <code>ServiceContext</code>;</li>
 * <li>Change of associated display <code>HScreen</code>;</li>
 * <li>Change of associated display <code>HScreen</code> area (extent)
 * assignment;</li>
 * <li>Change of associated set of <code>VideoOutputPort</code>s;</li>
 * <li>Change of audio focus of a display <code>HScreen</code>;</li>
 * <li>Change of screen visibility;</li>
 * <li>Change of screen z-order.</li>
 * <li>Change of set of underlying <code>HScreenDevice</code> that contribute
 * audio sources to an <code>HScreen</code>;</li>
 * <li>Change of set of underlying <code>HScreenDevice</code> instances, e.g.,
 * due to addition or removal of a <code>HScreenDevice</code> from an
 * <code>HScreen</code>;</li>
 * <li>Change of the z-order of the underlying <code>HScreenDevice</code>
 * instances of an <code>HScreen</code>;</li>
 * </ul>
 * 
 * @author Glenn Adams
 * @since MSM I01
 */
public class MultiScreenContextEvent extends MultiScreenEvent
{
    /**
     * The set of <code>HScreenDevice</code> instances associated with the
     * underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DEVICES_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 0;

    /**
     * The z-order of the set of <code>HScreenDevice</code> instances associated
     * with the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DEVICES_Z_ORDER_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 1;

    /**
     * The <code>ServiceContext</code> associated with the underlying
     * <code>HScreen</code> of the source <code>MultiScreenContext</code> has
     * changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_SERVICE_CONTEXT_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 2;

    /**
     * The display <code>HScreen</code> associated with the underlying
     * <code>HScreen</code> of the source code>MultiScreenContext</code> has
     * <changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DISPLAY_SCREEN_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 3;

    /**
     * The area (extent) of the display <code>HScreen</code> to which the
     * underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> is assigned has changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DISPLAY_AREA_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 4;

    /**
     * The set of video output ports associated with underlying
     * <code>HScreen</code> of the source <code>MultiScreenContext</code> has
     * changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_OUTPUT_PORT_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 5;

    /**
     * The visibility of the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_VISIBILITY_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 6;

    /**
     * The z-order of the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_Z_ORDER_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 7;

    /**
     * The audio sources of the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_AUDIO_SOURCES_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 8;

    /**
     * The audio focus screen of the underlying <code>HScreen</code> of the
     * source <code>MultiScreenContext</code> has changed. When the audio focus
     * screen of a display <code>HScreen</code> changes, then this event SHALL
     * be generated twice (after completing the change): firstly to the
     * <code>MultiScreenContext</code> of the logical screen which has lost
     * audio focus (if such logical screen existed), and secondly to the
     * <code>MultiScreenContext</code> of the display screen. In both of these
     * cases, the source <code>MultiScreenContext</code> SHALL be the display
     * screen.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_AUDIO_FOCUS_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 9;

    /**
     * Last event identifier assigned to
     * <code>MultiScreenConfigurationEvent</code> event identifiers.
     * 
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXTS_LAST = MULTI_SCREEN_CONTEXT_FIRST + 9;

    /**
     * Construct a <code>MultiScreenContextEvent</code>.
     * 
     * @param source
     *            A reference to a <code>MultiScreenContext</code> interface.
     * 
     * @param id
     *            The event identifier of this event, the value of which SHALL
     *            be one of the following:
     *            <code>MULTI_SCREEN_CONTEXT_DEVICES_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_DEVICES_Z_ORDER_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_SERVICE_CONTEXT_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_DISPLAY_SCREEN_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_DISPLAY_AREA_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_OUTPUT_PORT_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_VISIBILITY_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_Z_ORDER_CHANGED</code>,
     *            <code>MULTI_SCREEN_CONTEXT_AUDIO_SOURCES_CHANGED</code>, or
     *            <code>MULTI_SCREEN_CONTEXT_AUDIO_FOCUS_CHANGED</code>.
     * 
     * @since MSM I01
     **/
    public MultiScreenContextEvent(Object source, int id)
    {
        super(source, id);
    }
}
