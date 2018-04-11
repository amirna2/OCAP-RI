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
import java.awt.event.*;
import org.havi.ui.HSceneFactory;

import org.apache.log4j.Logger;

/**
 * An implementation of <code>Frame</code> which performs all painting using
 * double-buffering techniques. This <code>Frame</code> may be created by the
 * {@link HSceneFactory} on some platforms.
 * 
 * @see Component#isDoubleBuffered()
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.8 $, $Date: 2001/06/01 15:56:40 $
 */
public class DBFrame extends Frame
{

    // Log4J Logger
    private static final Logger log = Logger.getLogger(DBFrame.class.getName());

    /**
     * Default constructor.
     */
    public DBFrame()
    {
        super();
        addComponentListener(new ResizeTracker());

        if (log.isDebugEnabled())
        {
            log.debug("Using DBFrame...");
        }
    }

    /**
     * Constructor which specifies the name of the <code>Frame</code>.
     * 
     * @param str
     *            the name of the <code>Frame</code>
     */
    public DBFrame(String str)
    {
        super(str);
        addComponentListener(new ResizeTracker());
    }

    /**
     * Overrides <code>update()</code> to simply call <code>paint()</code>
     * without clearing the screen.
     * 
     * @param g
     *            the current graphics context
     */
    public void update(Graphics g)
    {
        paint(g);
    }

    /**
     * Overrides <code>paint()</code> to paint this component and all
     * sub-components using an offscreen buffer. Prior to painting
     * sub-components, the background color is painted, effectively erasing any
     * previously drawn graphics. This is necessary because
     * <code>update()</code> no longer clears the screen.
     * 
     * @param g
     *            the current graphics context
     */
    public void paint(Graphics g)
    {
        Dimension size = getSize();

        Image img;
        Graphics offscreen = null;

        try
        {
            if ((img = getOffscreenImage()) == null || (offscreen = img.getGraphics()) == null)
            {
                g.setColor(getBackground());
                g.clearRect(0, 0, size.width, size.height);
                super.paint(g);
            }
            else
            {
                Rectangle clipRect = g.getClipBounds();
                if (clipRect == null) 
                { 
                    clipRect = new Rectangle(0, 0, getWidth(), getHeight()); 
                }
                offscreen.setClip(clipRect); 
                

                offscreen.setColor(getBackground());
                offscreen.clearRect(0, 0, size.width, size.height);         

                super.paint(offscreen);
                prepOnscreen(g);
                g.drawImage(img, 0, 0, null);
            }
        }
        finally
        {
            if (offscreen != null) offscreen.dispose();
        }
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Override to manipulate onscreen graphics prior to rendering.
     * 
     * @param offscreen
     *            the onscreen Graphics to modify
     */
    protected void prepOnscreen(Graphics onscreen)
    {
        // Do nothing here
    }

    /**
     * Returns <code>true</code> if this component is painted to an offscreen
     * image ("buffer") that's copied to the screen later.
     * 
     * @return <code>true</code>
     */
    public boolean isDoubleBuffered()
    {
        return true;
    }

    /**
     * Returns the current offscreen <code>Image</code> used for
     * double-buffering.
     * 
     * @return the current offscreen <code>Image</code> used for
     *         double-buffering.
     * @see #paint(Graphics)
     */
    private Image getOffscreenImage()
    {
        if (offscreenImage == null)
        {
            Dimension size = getSize();
            offscreenImage = createImage(size.width, size.height);
        }
        return offscreenImage;
    }

    /** the current offscreen image used for double-buffering. */
    private Image offscreenImage;

    /**
     * ComponentAdapter used to allocate a new offscreen image if necessary.
     */
    private class ResizeTracker extends ComponentAdapter
    {
        /**
         * Called when this <code>DBFrame</code> is resized. If the
         * <code>DBFrame</code> is made larger, then a new offscreen
         * <code>Image</code> is allocated.
         */
        public void componentResized(ComponentEvent e)
        {
            Dimension size = getSize();

            if (offscreenImage != null
                    && (size.width > offscreenImage.getWidth(null) || size.height > offscreenImage.getHeight(null)))
            {
                offscreenImage.flush();
                offscreenImage = null;
            }
        }
    }

}
