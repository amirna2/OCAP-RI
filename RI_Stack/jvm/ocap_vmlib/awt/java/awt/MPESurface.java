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

package java.awt;

/**
 * A simple wrapper object for an MPE <i>surface</i> handle. The main purpose of
 * the <code>MPESurface</code> object is to server as a referenced object that
 * will keep an MPE surface in memory and valid as long as it is referenced.
 * 
 * @see MPEImage
 * @see MPEGraphicsDevice
 * 
 * @author Aaron Kamienski
 */
class MPESurface
{
    /**
     * Native peer surface handle.
     */
    private int surface;

    /**
     * Color used for blanking of non-offscreens.
     */
    private static final java.awt.Color BLANK = new java.awt.Color(0, 0, 0, 0);

    /**
     * Constructs a new <code>MPESurface</code> as a wrapper for the given
     * native <i>surface</i> handle.
     * 
     * @param surface
     *            MPE native surface handle
     */
    MPESurface(int surface)
    {
        this.surface = surface;
    }

    /**
     * Constructs a new <code>MPESurface</code> using the given
     * <code>GraphicsConfiguration</code>. The surface is initialized to
     * <code>Color(0,0,0,0)</code>.
     * 
     * @param width
     *            the width of the surface to create
     * @param height
     *            the height of the surface to create
     * @param gc
     *            the <code>MPEGraphicsConfiguration</code> used to create the
     *            surface
     */
    MPESurface(int width, int height, MPEGraphicsConfiguration gc)
    {
        this(width, height, gc, BLANK);
    }

    /**
     * Constructs a new <code>MPESurface</code> using the given
     * <code>GraphicsConfiguration</code>; initialized to the given
     * <code>Color</code>.
     * 
     * @param width
     *            the width of the surface to create
     * @param height
     *            the height of the surface to create
     * @param gc
     *            the <code>MPEGraphicsConfiguration</code> used to create the
     *            surface
     * @param color
     *            the color to initialize the surface to
     */
    MPESurface(int width, int height, MPEGraphicsConfiguration gc, java.awt.Color color)
    {
        this.surface = gc.createCompatibleImageSurface(width, height, color);
    }

    /**
     * Returns the native surface peer handle.
     * 
     * @return the native surface peer
     * @throws NullPointerException
     *             if the surface is invalid
     */
    public int getPeer()
    {
        if (surface == 0) throw new NullPointerException("Invalid/disposed surface handle");
        return surface;
    }

    /**
     * Disposes of the native surface peer when this <code>MPESurface</code> is
     * no longer in use and garbage collected.
     */
    public void finalize()
    {
        dispose();
    }

    /**
     * Disposes of the native surface peer.
     * 
     * @see #finalize
     * @see #pDispose
     */
    private synchronized void dispose()
    {
        if (surface != 0) pDispose(surface);
        surface = 0;
    }

    /**
     * Implements <code>Object.toString</code>.
     * 
     * @return <code>String</code> representation of this surface
     */
    public String toString()
    {
        return super.toString() + "[surface=" + surface + "]";
    }

    /**
     * Native method implements disposal of native surface peer.
     * 
     * @param surface
     *            the surface to delete
     * @see #dispose
     */
    private static native int pDispose(int surface);
}
