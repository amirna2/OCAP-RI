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

package org.cablelabs.impl.havi;

import java.awt.Font;
import java.awt.event.KeyEvent;

import org.havi.ui.HFontCapabilities;
import org.havi.ui.HScene;
import org.havi.ui.event.HEventGroup;
import org.havi.ui.event.HEventRepresentation;
import org.havi.ui.event.HKeyCapabilities;
import org.havi.ui.event.HMouseCapabilities;
import org.havi.ui.event.HRcCapabilities;
import org.havi.ui.event.HRcEvent;

/**
 * Implementations of the <code>CapabilitiesSupport</code> class provide support
 * for the <code>H*Capabilities</code> classes. For example, the implementation
 * of {@link HMouseCapabilities#getInputDeviceSupported()} might look something
 * like this:
 * 
 * <pre>
 * public static boolean getInputDeviceSupported()
 * {
 *     return HaviToolkit.getToolkit().getCapabilities().isMouseSupported();
 * }
 * </pre>
 * 
 * @see HFontCapabilities
 * @see HMouseCapabilities
 * @see HKeyCapabilities
 * @see HRcCapabilities
 * @see HEventRepresentation
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.4 $, $Date: 2002/06/03 21:32:54 $
 */
public abstract class CapabilitiesSupport
{
    /*  ************************ Mouse *************************** */

    /**
     * Implementation of
     * <code>HMouseCapabilities.getInputDeviceSupported()</code>.
     * 
     * @return <code>true</code> if mouse is supported on this platform;
     *         <code>false</code> if mouse is not supported
     * 
     * @see HMouseCapabilities#getInputDeviceSupported()
     */
    public abstract boolean isMouseSupported();

    /*  ************************ Keyboard *************************** */

    /**
     * Implementation of <code>HKeyCapabilities.getInputDeviceSupported()</code>
     * .
     * 
     * @return <code>true</code> if keyboard is supported on this platform;
     *         <code>false</code> if keyboard is not supported
     * 
     * @see HKeyCapabilities#getInputDeviceSupported()
     */
    public abstract boolean isKeyboardSupported();

    /**
     * Implementation of <code>HKeyCapabilities.isSupported(int vk)</code>.
     * 
     * @param the
     *            virtual keycode to query (e.g., {@link KeyEvent#VK_SPACE})
     * @return <code>true</code> if the given virtual key <code>keycode</code>
     *         is supported (i.e., can ever be generated); <code>false</code> if
     *         the <code>keycode</code> is not supported
     * 
     * @see HKeyCapabilities#isSupported(int vk)
     */
    public abstract boolean isKeySupported(int keycode);

    /*  ************************ Remote ************************* */

    /**
     * Implementation of <code>HRcCapabilities.getInputDeviceSupported()</code>.
     * 
     * @return <code>true</code> if remote control is supported on this
     *         platform; <code>false</code> if remote control is not supported
     * 
     * @see HRcCapabilities#getInputDeviceSupported()
     */
    public abstract boolean isRemoteSupported();

    /**
     * Implementation of <code>HRcCapabilities.isSupported(int vk)</code>.
     * 
     * @param key
     *            the virtual keycode to query (e.g.,
     *            {@link HRcEvent#VK_COLORED_KEY_0})
     * @return <code>true</code> if the given virtual key <code>keycode</code>
     *         is supported (i.e., can ever be generated); <code>false</code> if
     *         the <code>keycode</code> is not supported
     * 
     * @see HRcCapabilities#isSupported(int vk)
     */
    public abstract boolean isRcKeySupported(int key);

    /**
     * Implementation of <code>HRcCapabilities.getRepresentation(int key)</code>
     * .
     * 
     * @param key
     *            the virtual keycode to acquire an
     *            <code>HEventRepresentation</code>
     * @return the <code>HEventRepresentation</code> object describing this
     *         remote control key; <code>null</code> is returned if the
     *         specified <code>key</code> does not have a valid remote control
     *         representation
     * 
     * @see HRcCapabilities#getRepresentation(int key)
     * @see HEventRepresentation
     */
    public abstract HEventRepresentation getRepresentation(int key);

    /**
     * Returns the default set of keys that may be received by an
     * <code>HScene</code> Used to implement {@link HScene#getKeyEvents}. As
     * such, it returns a new <code>HEventGroup</code> containing a subset of
     * the keys for which {#isRcKeySupported} and {#isKeySupported} returns
     * <code>true</code>.
     * <p>
     * Should return a copy so that any changes to the <code>HEventGroup</code>
     * do not affect the object returned on subsequent calls.
     * 
     * @return a copy of the <code>HEventGroup</code> representing all keys that
     *         may be received by an <code>HScene</code> by default
     */
    public abstract HEventGroup getDefaultKeys();

    /*  ************************ Fonts ************************* */

    /**
     * Implementation of
     * <code>HFontCapabilities.getSupportedCharacterRanges(Font f)</code>
     * 
     * @param font
     *            The font to query for its support for Unicode ranges
     * @return An array of integer values, as defined in ISO/IEC 10646-1:1993(E)
     *         normative Annex A that this font supports, or null.
     * 
     * @see HFontCapabilities
     * @see HFontCapabilities#getSupportedCharacterRanges(Font f)
     */
    public abstract int[] getCharRanges(Font font);

    /**
     * Implementation of
     * <code>HFontCapabilities.isCharAvailable(Font f, char c)</code>.
     * 
     * @param font
     *            The font to query for its support for the specified character
     * @param c
     *            The character whose presence should be tested
     * @return <code>true</code> is the character is available within the font
     *         and can be rendered as defined in the ISO/IEC 10646- 1:1993(E)
     *         specification; <code>false</code> otherwise
     * 
     * @see HFontCapabilities
     * @see HFontCapabilities#isCharAvailable(Font font, char c)
     */
    public abstract boolean isCharAvailable(Font font, char c);

    /**
     * Tests if the specified font is accessible in a form usable with this
     * graphics configuration. For platforms which define a mechanism for
     * (possibly temporary) download of fonts, this method shall check fonts
     * available through that mechanism. This does not cause the font to be
     * downloaded but indicates whether a download will succeed. If information
     * needed to determine the existence of the font needs to be downloaded then
     * that information shall be downloaded as part of the execution of this
     * method. This method shall block while any such downloading happens. An
     * error in the downloading of any such information shall be reported as the
     * font not existing.
     * 
     * @param f
     *            the font to test.
     * @return <code>true</code> if the font is accessible, otherwise
     *         <code>false</code>.
     */
    public abstract boolean fontAccessible(java.awt.Font f);

    /**
     * Download a font which is only temporarily available on a platform. If
     * called for fonts which are permanently available on a platform, this
     * method has no effect. This method blocks while the font is downloaded. If
     * the font is not accessible then this method has no effect and the normal
     * Java font behavior is followed. The platform will use a platform
     * dependent approximation of this font if needed. If a font is already
     * downloaded then it shall not be downloaded again.
     * 
     * @param f
     *            the font to load.
     * @throws <CODE>java.io.IOException</CODE> if an error occurs while trying
     *         to download a font.
     */
    public abstract void downloadFont(java.awt.Font f) throws java.io.IOException;
}
