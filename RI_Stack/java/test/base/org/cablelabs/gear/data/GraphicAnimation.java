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

package org.cablelabs.gear.data;

import java.awt.*;
import java.awt.image.*;

/**
 * A <code>GraphicAnimation</code> is a data wrapper that represents an animated
 * sequence of {@link GraphicData} frames.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski
 * @version $Id: GraphicAnimation.java,v 1.2 2002/06/03 21:31:08 aaronk Exp $
 */
public class GraphicAnimation implements AnimationData
{
    /**
     * Default constructor. No <code>GraphicData</code> animation frames are
     * assigned.
     * 
     * @see #setGraphicData(GraphicData[])
     * @see #setGraphicData(int,GraphicData)
     */
    public GraphicAnimation()
    {
    }

    /**
     * <code>GraphicData[]</code> constructor. The given array of
     * <code>GraphicData</code> frames is associated with the newly constructed
     * <code>GraphicAnimation</code>.
     * 
     * @param data
     *            the array of <code>GraphicData</code> frames on which this
     *            <code>GraphicAnimation</code> will be based
     */
    public GraphicAnimation(GraphicData[] data)
    {
        setGraphicData(data);
    }

    // Definition copied from AnimationData
    public Dimension getSize()
    {
        return new Dimension(getSizeInternal());
    }

    /**
     * Returns the size of this animation (as specified for <code>getSize</code>
     * ). This may be calculated on the fly as necessary or it may return a
     * cached <code>Dimension</code> object.
     * 
     * @return a newly created or privately cached <code>Dimension</code> giving
     *         the maximum size of this animation
     */
    private Dimension getSizeInternal()
    {
        // If necessary, calculate new maximum size
        if (size == null)
        {
            int maxW = 0, maxH = 0;

            if (data != null)
            {
                for (int i = 0; i < data.length; ++i)
                {
                    if (data[i] != null)
                    {
                        int w = data[i].getWidth();
                        int h = data[i].getHeight();

                        maxW = Math.max(maxW, w);
                        maxH = Math.max(maxH, h);
                    }
                }
            }
            size = new Dimension(maxW, maxH);
        }

        return size;
    }

    // Definition copied from AnimationData
    public int getWidth()
    {
        return getSizeInternal().width;
    }

    // Definition copied from AnimationData
    public int getHeight()
    {
        return getSizeInternal().height;
    }

    // Definition copied from AnimationData
    public void draw(int i, Graphics g, int x, int y, ImageObserver io) throws IndexOutOfBoundsException
    {
        if (data[i] != null) data[i].draw(g, x, y, io);
    }

    // Definition copied from AnimationData
    public void draw(int i, Graphics g, int x, int y, int width, int height, ImageObserver io)
            throws IndexOutOfBoundsException
    {
        if (data[i] != null) data[i].draw(g, x, y, width, height, io);
    }

    // Definition copied from AnimationData
    public int getLength()
    {
        return (data == null) ? 0 : data.length;
    }

    /**
     * Sets the array of <code>GraphicData</code> objects that are to be the
     * individual frames of this <code>AnimationData</code>.
     * 
     * @param data
     *            the <code>GraphicData</code> frames
     * @see #getGraphicData()
     * @see #setGraphicData(int,GraphicData)
     * @see #getGraphicData(int)
     */
    public void setGraphicData(GraphicData data[])
    {
        size = null; // clear cached size
        this.data = data;
    }

    /**
     * Returns the array of <code>GraphicData</code> objects that are the
     * individual frames of this <code>AnimationData</code>.
     * 
     * @return array of <code>GraphicData</code> frames
     * @see #setGraphicData(GraphicData[])
     * @see #setGraphicData(int,GraphicData)
     * @see #getGraphicData(int)
     */
    public GraphicData[] getGraphicData()
    {
        // Return a copy
        GraphicData[] retData = null;
        GraphicData[] tmp = data;
        if (tmp != null)
        {
            retData = new GraphicData[tmp.length];
            System.arraycopy(tmp, 0, retData, 0, tmp.length);
        }
        return retData;
    }

    /**
     * Sets an individual <code>GraphicData</code> frame of this
     * <code>AnimationData</code>.
     * 
     * @param i
     *            the index of the frame to set
     * @param data
     *            the <code>GraphicData</code> frame
     * @see #setGraphicData(GraphicData[])
     * @see #getGraphicData()
     * @see #getGraphicData(int)
     * 
     * @throws IndexOutOfBoundsException
     *             if the index is outside the current array bounds (
     *             {@link #setGraphicData(GraphicData[])} must be used to change
     *             the size of the array)
     */
    public void setGraphicData(int i, GraphicData graphic)
    {
        size = null; // clear cached size
        data[i] = graphic;
    }

    /**
     * Returns an individual <code>GraphicData</code> frame of this
     * <code>AnimationData</code>.
     * 
     * @param i
     *            the index of the frame to set
     * @return the indexed <code>GraphicData</code> frame
     * @see #setGraphicData(GraphicData[])
     * @see #getGraphicData()
     * @see #setGraphicData(int,GraphicData)
     */
    public GraphicData getGraphicData(int i)
    {
        return data[i];
    }

    // Definition copied from Object
    public String toString()
    {
        String str;
        if (data == null || data.length == 0)
            str = "<empty>";
        else
        {
            StringBuffer buf = new StringBuffer();
            String comma = "";

            for (int i = 0; i < data.length; ++i)
            {
                buf.append(comma);
                buf.append(data[i]);
                comma = ",";
            }
            str = buf.toString();
        }
        return super.toString() + " [" + str + "]";
    }

    /**
     * The array of <code>GraphicData</code> animation frames attached to this
     * object
     */
    private GraphicData[] data;

    /**
     * The current total <i>size</i> of the animation frames, if currently set.
     */
    private Dimension size;
}
