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

package org.cablelabs.impl.dvb.ui;

import java.awt.*;
import java.text.AttributedCharacterIterator;

import org.dvb.ui.*;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Implementation of <code>DVBGraphics</code> based upon a
 * <code>DVBGraphicsPeer</code> implementation. This requires the AWT
 * implementation's <code>Graphics</code> object to implement the
 * <code>DVBGraphicsPeer</code> interface.
 * 
 * @see DVBGraphicsPeer
 * @author Aaron Kamienski
 */
public class DVBGraphicsImpl extends DVBGraphics
{
    /**
     * The DVBGraphicsPeer given upon instantiation. This is used for
     * DVBGraphics-specific operations.
     */
    private DVBGraphicsPeer peer;

    /**
     * The DVBGraphicsPeer given upon instantiation, cast to a Graphics. This is
     * used for most graphics operations.
     */
    private Graphics g;

    /**
     * The current DVBAlphaComposite.
     */
    private DVBAlphaComposite comp;

    /**
     * Creates a new <code>DVBGraphicsImpl</code> that adapts the given
     * <code>DVBGraphicsPeer</code> object to the <code>DVBGraphics</code>
     * interface.
     */
    public DVBGraphicsImpl(DVBGraphicsPeer peer)
    {
        this(peer, DVBAlphaComposite.SrcOver);
    }

    /**
     * Creates a new <code>DVBGraphicsImpl2</code> that adapts the given
     * <code>DVBGraphicsPeer</code> object to the <code>DVBGraphics</code>
     * interface.
     */
    private DVBGraphicsImpl(DVBGraphicsPeer peer, int rule, float alpha)
    {
        this(peer, DVBAlphaComposite.getInstance(rule, alpha));
    }

    /**
     * Creates a new <code>DVBGraphicsImpl</code> that adapts the given
     * <code>DVBGraphicsPeer</code> object to the <code>DVBGraphics</code>
     * interface. This is used internally by the other constructors.
     */
    private DVBGraphicsImpl(DVBGraphicsPeer peer, DVBAlphaComposite comp)
    {
        if (peer == null) throw new NullPointerException("non-null Graphics required");

        this.peer = peer;
        this.g = (Graphics) peer;

        try
        {
            setDVBComposite(comp);
        }
        catch (UnsupportedDrawingOperationException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Returns all available Porter-Duff Rules for this specific Graphics
     * context.
     */
    public int[] getAvailableCompositeRules()
    {
        return peer.getAvailablePeerCompositeRules();
    }

    /**
     * Gets this graphics context's current color. This will return a DVBColor
     * cast to java.awt.Color.
     */
    public java.awt.Color getColor()
    {
        Color c = g.getColor();
        return (c instanceof org.dvb.ui.DVBColor) ? c : new org.dvb.ui.DVBColor(c);
    }

    /**
     * Returns the current <code>DVBAlphaComposite</code> in the
     * <code>DVBGraphics</code> context.
     */
    public org.dvb.ui.DVBAlphaComposite getDVBComposite()
    {
        return comp;
    }

    /**
     * Sets this graphics context's current color to the specified color.
     */
    public void setColor(java.awt.Color c)
    {
        g.setColor(c);
    }

    /**
     * Sets the <code>DVBAlphaComposite</code> for the <code>DVBGraphics</code>
     * context.
     */
    public void setDVBComposite(org.dvb.ui.DVBAlphaComposite comp)
            throws org.dvb.ui.UnsupportedDrawingOperationException
    {
        this.comp = comp;
        peer.setPeerComposite(comp.getRule(), comp.getAlpha());
    }

    /**
     * Returns the Sample Model (DVBBufferedImage.TYPE_BASE,
     * DVBBufferedImage.TYPE_ADVANCED) which is used in the on/off screen buffer
     * this graphics object draws into.
     * 
     * @return always returns DVBBufferedImage.TYPE_ADVANCED
     * 
     * @todo Should match the offscreen buffer type if there is one; should
     *       always be TYPE_BASE if it's on-screen
     */
    public int getType()
    {
        return DVBBufferedImage.TYPE_ADVANCED;
    }

    /**
     * Returns the best match for the specified Color as a DVBColor, in a
     * device-dependent manner, as constrained by the MHP graphics reference
     * model.
     * 
     * @param c
     *            the specified Color.
     * @return the best DVBColor match for the specified Color.
     */
    public DVBColor getBestColorMatch(java.awt.Color c)
    {
        return new DVBColor(c);
    }

    /**
     * Returns a <code>String</code> object representing this
     * <code>DVBGraphics</code> object's value.
     */
    public java.lang.String toString()
    {
        return super.toString();
    }

    public Graphics create()
    {
        return new DVBGraphicsImpl((DVBGraphicsPeer) g.create(), peer.getPeerCompositeRule(),
                peer.getPeerCompositeAlpha());
    }

    public void translate(int x, int y)
    {
        g.translate(x, y);
    }

    public void setPaintMode()
    {
        try
        {
            setDVBComposite(DVBAlphaComposite.SrcOver);
        }
        catch (UnsupportedDrawingOperationException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    public void setXORMode(Color c)
    {
        g.setXORMode(c);
    }

    public java.awt.Font getFont()
    {
        return g.getFont();
    }

    public void setFont(java.awt.Font font)
    {
        g.setFont(font);
    }

    public java.awt.FontMetrics getFontMetrics()
    {
        return g.getFontMetrics();
    }

    public java.awt.FontMetrics getFontMetrics(java.awt.Font f)
    {
        return g.getFontMetrics(f);
    }

    public Rectangle getClipBounds()
    {
        return g.getClipBounds();
    }

    public void clipRect(int x, int y, int width, int height)
    {
        g.clipRect(x, y, width, height);
    }

    public void setClip(int x, int y, int width, int height)
    {
        g.setClip(x, y, width, height);
    }

    public java.awt.Shape getClip()
    {
        return g.getClip();
    }

    public void setClip(java.awt.Shape clip)
    {
        g.setClip(clip);
    }

    public void copyArea(int x, int y, int width, int height, int dx, int dy)
    {
        g.copyArea(x, y, width, height, dx, dy);
    }

    public void drawLine(int x1, int y1, int x2, int y2)
    {
        g.drawLine(x1, y1, x2, y2);
    }

    public void fillRect(int x, int y, int width, int height)
    {
        g.fillRect(x, y, width, height);
    }

    public void clearRect(int x, int y, int width, int height)
    {
        g.clearRect(x, y, width, height);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight)
    {
        g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawOval(int x, int y, int width, int height)
    {
        g.drawOval(x, y, width, height);
    }

    public void fillOval(int x, int y, int width, int height)
    {
        g.fillOval(x, y, width, height);
    }

    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        g.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
    {
        g.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawPolyline(int xPoints[], int yPoints[], int nPoints)
    {
        g.drawPolyline(xPoints, yPoints, nPoints);
    }

    public void drawPolygon(int xPoints[], int yPoints[], int nPoints)
    {
        g.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void fillPolygon(int xPoints[], int yPoints[], int nPoints)
    {
        g.fillPolygon(xPoints, yPoints, nPoints);
    }

    public void drawString(String str, int x, int y)
    {
        g.drawString(str, x, y);
    }

    public void drawString(AttributedCharacterIterator iterator, int x, int y)
    {
        // TODO Implement this!
        // g.drawString(iterator, x, y);
    }

    public boolean drawImage(Image img, int x, int y, java.awt.image.ImageObserver observer)
    {
        return g.drawImage(toImage(img), x, y, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, java.awt.image.ImageObserver observer)
    {
        return g.drawImage(toImage(img), x, y, width, height, observer);
    }

    public boolean drawImage(Image img, int x, int y, Color bgcolor, java.awt.image.ImageObserver observer)
    {
        return g.drawImage(toImage(img), x, y, bgcolor, observer);
    }

    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
            java.awt.image.ImageObserver observer)
    {
        return g.drawImage(toImage(img), x, y, width, height, bgcolor, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            java.awt.image.ImageObserver observer)
    {
        return g.drawImage(toImage(img), dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
            Color bgcolor, java.awt.image.ImageObserver observer)
    {
        return g.drawImage(toImage(img), dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    public void dispose()
    {
        Graphics tmp = g;
        peer = null;
        g = null;
        if (tmp != null) tmp.dispose();
    }

    private Image toImage(Image img)
    {
        return (img instanceof org.dvb.ui.DVBBufferedImage) ? ((org.dvb.ui.DVBBufferedImage) img).getImage() : img;
    }

    /**
     * Implementation of <code>GraphicsFactory</code> class which adapts
     * <code>DVBGraphicsPeer</code> objects to the <code>DVBGraphics</code> api
     * using an instance of <code>DVBGraphicsImpl2</code>.
     */
    public static class GraphicsFactory implements org.cablelabs.impl.awt.GraphicsFactory
    {
        public Graphics wrap(Graphics peer)
        {
            return (peer instanceof DVBGraphicsPeer) ? (new DVBGraphicsImpl((DVBGraphicsPeer) peer)) : peer;
        }
    }

    // methods added for Graphics2D. These are supported in DVBGraphicsImpl2,
    // but not in DVBGraphicsImpl
    public void setStroke(Stroke s)
    {
        throw new UnsupportedOperationException("DVBGraphicsImpl does not support setStroke");
    }

    public Stroke getStroke()
    {
        throw new UnsupportedOperationException("DVBGraphicsImpl does not support getStroke");
    }

    public void setComposite(Composite comp)
    {
        throw new UnsupportedOperationException("DVBGraphicsImpl does not support setComposite");
    }

    public Composite getComposite()
    {
        throw new UnsupportedOperationException("DVBGraphicsImpl does not support getComposite");
    }

    public GraphicsConfiguration getDeviceConfiguration()
    {
        throw new UnsupportedOperationException("DVBGraphicsImpl does not support getDeviceConfiguration");
    }

}
