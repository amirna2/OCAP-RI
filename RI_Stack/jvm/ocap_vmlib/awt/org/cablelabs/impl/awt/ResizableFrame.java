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

package org.cablelabs.impl.awt;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * A subclass of <code>Frame</code> which provides a method for resizing to
 * match the current bounds of the {@link associated #getGraphicsConfiguration}
 * <code>GraphicsConfiguration</code>.
 * 
 * @author Aaron Kamienski
 */
public class ResizableFrame extends Frame
{
    /**
     * Constructs a new instance of <code>ResizableFrame</code> that is
     * initially invisible. The title of the <code>ResizableFrame</code> is
     * empty.
     * <p>
     * The frame is sized to fill the bounds of the default
     * <code>GraphicsDevice</code>'s default configuration.
     */
    public ResizableFrame()
    {
        super();
        initDevice();
    }

    /**
     * Create a <code>ResizableFrame</code> with the specified
     * <code>GraphicsConfiguration</code> of a screen device.
     * <p>
     * The frame is sized to fill the bounds of the default
     * <code>GraphicsDevice</code>'s default configuration.
     * 
     * @param gc
     *            the <code>GraphicsConfiguration</code> of the target screen
     *            device. If <code>gc</code> is <code>null</code>, the system
     *            default <code>GraphicsConfiguration</code> is assumed.
     * @exception IllegalArgumentException
     *                if <code>gc</code> is not from a screen device.
     */
    public ResizableFrame(GraphicsConfiguration gc)
    {
        super(gc);
        initDevice();
    }

    /**
     * Constructs a new, initially invisible <code>ResizableFrame</code> object
     * with the specified title and <code>GraphicsConfiguration</code>.
     * <p>
     * The frame is sized to fill the bounds of the default
     * <code>GraphicsDevice</code>'s default configuration.
     * 
     * @param title
     *            the title to be displayed in the frame's border. A
     *            <code>null</code> value is treated as an empty string, "".
     * @param gc
     *            the <code>GraphicsConfiguration</code> of the target screen
     *            device. If <code>gc</code> is <code>null</code>, the system
     *            default <code>GraphicsConfiguration</code> is assumed.
     * @exception IllegalArgumentException
     *                if <code>gc</code> is not from a screen device.
     */
    public ResizableFrame(String title, GraphicsConfiguration gc)
    {
        super(title, gc);
        initDevice();
    }

    /**
     * Constructs a new, initially invisible <code>ResizableFrame</code> object
     * with the specified title.
     * <p>
     * The frame is sized to fill the bounds of the default
     * <code>GraphicsDevice</code>'s default configuration.
     * 
     * @param title
     *            the title to be displayed in the frame's border. A
     *            <code>null</code> value is treated as an empty string, "".
     * @exception IllegalArgumentException
     *                if gc is not from a screen device.
     */
    public ResizableFrame(String title)
    {
        super(title);
        initDevice();
    }

    /**
     * Create a <code>ResizableFrame</code> with the specified
     * <code>GraphicsConfiguration</code> of a screen device.
     * <p>
     * The frame is sized to fill the bounds of the default
     * <code>GraphicsDevice</code>'s default configuration.
     * 
     * @param gd
     *            the <code>GraphicsDevice</code> of the target screen device
     * @exception IllegalArgumentException
     *                if <code>gc</code> a screen device
     */
    public ResizableFrame(GraphicsDevice gd)
    {
        super(gd.getDefaultConfiguration());
        graphicsDevice = gd;
    }

    /**
     * Constructs a new, initially invisible <code>ResizableFrame</code> object
     * with the specified title and <code>GraphicsConfiguration</code>.
     * <p>
     * The frame is sized to fill the bounds of the default
     * <code>GraphicsDevice</code>'s default configuration.
     * 
     * @param title
     *            the title to be displayed in the frame's border. A
     *            <code>null</code> value is treated as an empty string, "".
     * @param gc
     *            the <code>GraphicsDevice</code> of the target screen device
     * @exception IllegalArgumentException
     *                if <code>gd</code> is not a screen device
     */
    public ResizableFrame(String title, GraphicsDevice gd)
    {
        super(title, gd.getDefaultConfiguration());
        graphicsDevice = gd;
    }

    /**
     * Initializes {@link #graphicsDevice} with the associated
     * <code>GraphicsDevice</code>.
     */
    private void initDevice()
    {
        graphicsDevice = getGraphicsConfiguration().getDevice();
    }

    /**
     * Updates the bounds of this <code>ResizableFrame</code> to match the
     * current bounds of the {@link #getGraphicsConfiguration associated}
     * <code>GraphicsConfiguration</code> plus this frame's insets.
     * <p>
     * Equivalent to:
     * 
     * <pre>
     * Insets i = getInsets();
     * Rectangle r = graphicsDevice.getDefaultConfiguration().getBounds();
     * Component.setBounds(r.x - i.left, r.y - i.top, r.width + i.left + i.right, r.height + i.top + i.bottom);
     * </pre>
     * 
     * @see #getBounds
     * @see #getLocation
     * @see #getSize
     * @see #getInsets
     */
    public void updateBounds()
    {
        Insets i = getInsets();
        Rectangle r = graphicsDevice.getDefaultConfiguration().getBounds();
        // Rectangle r = getGraphicsConfiguration().getBounds();
        pUpdateBounds(r.x - i.left, r.y - i.top, r.width + i.left + i.right, r.height + i.top + i.bottom);
    }

    /*
     * public void setBounds() { // Essentially invoke
     * super.super.setBounds(...) pUpdateBounds(x, y, width, height); }
     */

    /**
     * Updates the bounds of this <code>Frame</code>. This method is native to
     * get around the fact that {@link Window#setBounds} silently ignores the
     * call and <i>x</i>, <i>y</i>, <i>width</i>, and <i>height</i> instance
     * variables are private to the <code>java.awt</code> package.
     */
    private native void pUpdateBounds(int x, int y, int width, int height);

    /**
     * The <code>GraphicsDevice</code> with which this <code>Frame</code> is
     * associated.
     */
    private GraphicsDevice graphicsDevice;

    /**
     * Initializes native method implementation.
     */
    private static native void pInitIDs();

    static
    {
        pInitIDs();
    }
}
