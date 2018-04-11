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

package org.cablelabs.impl.havi.port.mpe;

/**
 * The set of <code>HaviToolkit</code> properties that are used to specify
 * various settings for this port.
 * <p>
 * <i>It may make sense to even put the default values here so that they are in
 * one place.</i>
 * <p>
 * Please note that if the definitions in this class are changed, the entire
 * port should be recompiled (not just this class).
 * 
 * @author Aaron Kamienski
 * @version $Id: Property.java,v 1.3 2002/11/07 21:13:41 aaronk Exp $
 */
public interface Property
{
    /**
     * Prefix for all port-related properties.
     */
    String PREFIX = "cablelabs.havi.port.mpe.";

    /**
     * Dimension property specifying default Screen size.
     */
    String SCREEN_SIZE = PREFIX + "Screen.size";

    /**
     * Boolean property specifying whether screen should be double-buffered.
     */
    String DOUBLE_BUFFERED = PREFIX + "Screen.db";

    /**
     * Color property specifying the default background color.
     */
    String BACKGROUND = PREFIX + "Graphics.background";

    /**
     * Color property specifying the default foreground color.
     */
    String FOREGROUND = PREFIX + "Graphics.foreground";

    /**
     * Boolean property specifying whether DVB graphics emulation should be
     * enabled or not. Defaults to <code>false</code>.
     */
    String DVB_EMULATION = PREFIX + "Graphics.dvbemul";

    /**
     * Color property specifying the "punchthrough color" that is used as the
     * background color for the main window frame. When painted, video is
     * displayed.
     */
    String PUNCHTHROUGH_COLOR = PREFIX + "Graphics.punchthrough";

    /**
     * String property specifying what text layout manager should be used by
     * default. If not defined or class cannot be found, then
     * HDefaultTextLayoutManager will be used by default.
     */
    String TLM = PREFIX + "Graphics.tlm";

    /**
     * Color property specifying the default BackgroundDevice color.
     */
    String BG_COLOR = PREFIX + "Background.color";

    /**
     * String property specifying the StillImageBackground draw method. Valid
     * values are:
     * <ul>
     * <li> <code>"CENTERED"</code>
     * <li> <code>"SCALED"</code>
     * <li> <code>"TILED"</code>
     * </ul>
     * The default is <code>"CENTERED"</code>.
     */
    String BG_DRAWMETHOD = PREFIX + "Background.drawMethod";

    /**
     * Boolean property specifying whether a mouse device is supported. Defaults
     * to <code>true</code>.
     */
    String MOUSE = PREFIX + "Capabilities.mouse";

    /**
     * Boolean property specifying whether a remote device is supported.
     * Defaults to <code>false</code>.
     */
    String REMOTE = PREFIX + "Capabilities.remote";

    /**
     * Boolean property specifying whether a keyboard device is supported.
     * Defaults to <code>true</code>.
     */
    String KEYBOARD = PREFIX + "Capabilities.keyboard";

    /**
     * Boolean property specifying whether a virtual keyboard is to be used.
     * Defaults to <code>false</code>.
     */
    String VIRTUAL_KEYBOARD = PREFIX + "VirtualKeyboard.use";

    /**
     * Boolean property specifying whether to <i>eagerly</i> coalesce
     * Paint/Update events or not. Defaults to <code>false</code>, whereby the
     * default implementation is used. If <code>true</code>, then events will be
     * coalesced eagerly; i.e., all update rectangles will be unioned to create
     * a single event.
     */
    String EAGER_COALESCING = PREFIX + "EventCoalesce.eager";

    /**
     * Integer property specifying the default inset value for all HAVi HLooks
     * Defaults to (3,3,3,3)
     */
    String HLOOK_INSET = PREFIX + "HLook.DefaultInset";

    /**
     * Integer property specifying the default inset value for all HAVi HLooks
     * Defaults to (6,6,6,6)
     */
    String HLISTGROUPLOOK_ELEMENT_INSET = PREFIX + "HListGroupLook.DefaultElementInset";
}
