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

import java.awt.*;
import org.havi.ui.*;

/**
 * The <code>MatteCompositor</code> interface defines a strategy for
 * implementing matte compositions. This includes support for compositing a flat
 * matte or image matte directly with a destination or with an an intermediate
 * off-screen buffer.
 * 
 * @author Todd Earles
 * @version $Revision: 1.1 $ $Date: 2001/04/11 21:59:43 $
 */
public interface MatteCompositor
{
    /**
     * Get an off-screen image.
     * 
     * @param width
     *            the minimum width required for the image
     * @param height
     *            the minimum height required for the image
     * @return the off-screen image
     */
    public Image createOffScreenImage(int width, int height);

    /**
     * Create a graphics object for the specified off-screen image.
     * 
     * @param offScreenImage
     *            the off-screen image
     * @return the off-screen graphics
     */
    public Graphics createOffScreenGraphics(Image offScreenImage);

    /**
     * Composite an off-screen image with the destination using SRC mode. No
     * matte is used in this composition.
     * 
     * @param offScreenImage
     *            the off-screen image
     * @param g
     *            the graphics for the destination
     */
    public void composite(Graphics g, Image offScreenImage);

    /**
     * Composite an off-screen image with the destination using the specified
     * matte. If an off-screen image is not specified, then the matte is
     * composited directly with the destination
     * 
     * @param g
     *            the destination graphics context
     * @param gOffScreen
     *            the source graphics context (null if none)
     * @param offScreenImage
     *            the off-screen image (null if none)
     * @param matte
     *            matte to apply when compositing the source image with the
     *            destination
     */
    public void composite(Graphics g, Graphics gOffScreen, Image offScreenImage, HMatte matte);
}
