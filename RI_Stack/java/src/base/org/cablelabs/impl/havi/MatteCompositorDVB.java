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

import org.havi.ui.*;

import java.awt.Component;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Color;

import org.dvb.ui.DVBGraphics;
import org.dvb.ui.DVBColor;
import org.dvb.ui.DVBBufferedImage;
import org.dvb.ui.DVBAlphaComposite;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The <code>MatteCompositorDVB</code> class implements the
 * <code>MatteCompositor</code> interface using the DVB MHP extended graphics
 * API.
 * 
 * @author Todd Earles (original MatteCompositorJava2D)
 * @author Aaron Kamienski
 * @version $Id: MatteCompositorDVB.java,v 1.2 2002/06/03 21:32:59 aaronk Exp $
 */
public class MatteCompositorDVB implements MatteCompositor
{
    // Definition copied from the MatteCompositor interface
    public MatteCompositorDVB(Component component)
    {
        this.component = component;
    }

    // Definition copied from the MatteCompositor interface
    public Image createOffScreenImage(int width, int height)
    {
        return new DVBBufferedImage(width, height);
    }

    // Definition copied from the MatteCompositor interface
    public Graphics createOffScreenGraphics(Image offScreenImage)
    {
        // Our emulated version will currently return null...
        return offScreenImage.getGraphics();
    }

    // Definition copied from the MatteCompositor interface
    public void composite(Graphics g, Image offScreenImage)
    {
        g.drawImage(offScreenImage, 0, 0, null);
    }

    // Definition copied from the MatteCompositor interface
    public void composite(Graphics g, Graphics gOffScreen, Image offScreenImage, HMatte matte)
    {
        // Use the DVBGraphics version throughout this method
        DVBGraphics g2 = (DVBGraphics) g;
        DVBGraphics g2OffScreen = (DVBGraphics) gOffScreen;

        // Extract the matte data based upon the type of matte
        boolean matteTypeFlat = true;
        float alpha = 0;
        Point offset = null;
        Image matteImage = null;
        if (matte instanceof HFlatMatte)
        {
            HFlatMatte m = (HFlatMatte) matte;
            alpha = m.getMatteData();
        }
        else if (matte instanceof HFlatEffectMatte)
        {
            HFlatEffectMatte m = (HFlatEffectMatte) matte;
            float[] alp = m.getMatteData();
            if (alp != null) alpha = alp[m.getPosition()];
        }
        else if (matte instanceof HImageMatte)
        {
            matteTypeFlat = false;
            HImageMatte m = (HImageMatte) matte;
            matteImage = m.getMatteData();
            offset = m.getOffset();
        }
        else if (matte instanceof HImageEffectMatte)
        {
            matteTypeFlat = false;
            HImageEffectMatte m = (HImageEffectMatte) matte;
            int position = m.getPosition();
            Image data[] = m.getMatteData();
            if (data != null) matteImage = data[position];
            offset = m.getOffset(position);
        }

        // Processing depends on whether it is a flat matte or image matte
        if (matteTypeFlat == true)
        {
            // Compositing depends on whether we have an off-screen image or
            // not.
            if (g2OffScreen == null)
            {
                // No off-screen image. Just composite the flat matte with the
                // destination.
                DVBAlphaComposite savedComposite = g2.getDVBComposite();
                Color savedColor = g2.getColor();
                setComposite(g2, DVBAlphaComposite.DstIn);
                g2.setColor(new DVBColor(0F, 0F, 0F, alpha));
                Rectangle rect = component.getBounds();
                g2.fillRect(0, 0, rect.width, rect.height);
                setComposite(g2, savedComposite);
                g2.setColor(savedColor);
            }
            else
            {
                // There is an off-screen image. Composite the off-screen image
                // with the destination using the specified flat matte.
                DVBAlphaComposite ac = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER, alpha);
                DVBAlphaComposite savedComposite = g2.getDVBComposite();
                setComposite(g2, ac);
                g2.drawImage(offScreenImage, 0, 0, null);
                setComposite(g2, savedComposite);
            }
        }
        else
        {
            // Compositing depends on whether we have an off-screen image or
            // not.
            if (g2OffScreen == null)
            {
                // No off-screen image. Just composite the image matte with the
                // destination.
                if (matteImage != null)
                {
                    // Composite the image matte with the destination
                    DVBAlphaComposite savedComposite = g2.getDVBComposite();
                    setComposite(g2, DVBAlphaComposite.DstIn);
                    g2.drawImage(matteImage, offset.x, offset.y, null);
                    setComposite(g2, savedComposite);
                }
            }
            else
            {
                // There is an off-screen image. Composite the off-screen image
                // with the destination using the specified image matte.
                if (matteImage != null)
                {
                    // Composite the image matte with the off-screen image
                    DVBAlphaComposite savedComposite = g2OffScreen.getDVBComposite();
                    setComposite(g2OffScreen, DVBAlphaComposite.DstIn);
                    g2OffScreen.drawImage(matteImage, offset.x, offset.y, null);
                    setComposite(g2OffScreen, savedComposite);
                }
                // Composite the off-screen image with the destination
                g2.drawImage(offScreenImage, 0, 0, null);
            }
        }
    }

    /**
     * Sets the composite for the given DVBGraphics to the given
     * DVBAlphaComposite, ignoring any UnsupportedDrawingOperationExceptions.
     */
    private void setComposite(DVBGraphics g, DVBAlphaComposite comp)
    {
        try
        {
            g.setDVBComposite(comp);
        }
        catch (org.dvb.ui.UnsupportedDrawingOperationException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /** Component for which this instance was created. */
    Component component = null;
}
