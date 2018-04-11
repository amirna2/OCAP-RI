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

import org.cablelabs.gear.util.PicturePortfolio;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.image.ImageObserver;

/**
 * An implementation of the <code>AnimationData</code> interface based upon
 * <i>pictures</i> backed by a {@link PicturePortfolio}.
 * 
 * @author Aaron Kamienski
 * @version $Id: PortfolioAnimation.java,v 1.2 2002/06/03 21:31:09 aaronk Exp $
 */
public class PortfolioAnimation implements AnimationData
{
    /**
     * Default constructor. No {@link PicturePortfolio} or <i>picture</i> IDs
     * have been assigned.
     * 
     * @see #setPortfolio(PicturePortfolio)
     * @see #setPortfolioId(int,String)
     * @see #setPortfolioId(String[])
     */
    public PortfolioAnimation()
    {
    }

    /**
     * Creates a <code>PortfolioAnimation</code> based on the given portfolio
     * and picture identifiers.
     * 
     * @param pp
     *            portfolio containing the animation frames
     * @param id
     *            an array of <code>String</code> identifiers, identifying
     *            <i>pictures</i> in <code>pp</code>
     */
    public PortfolioAnimation(PicturePortfolio pp, String id[])
    {
        setPortfolio(pp);
        setPortfolioId(id);
    }

    // Description copied from AnimationData
    public void draw(int i, Graphics g, int x, int y, ImageObserver io) throws IndexOutOfBoundsException
    {
        String id = ids[i];
        if (id != null) pp.draw(g, id, x, y, io);
    }

    // Description copied from AnimationData
    public void draw(int i, Graphics g, int x, int y, int width, int height, ImageObserver io)
            throws IndexOutOfBoundsException
    {
        String id = ids[i];
        if (id != null) pp.draw(g, id, x, y, width, height, io);
    }

    // Description copied from AnimationData
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
            int w = 0, h = 0;

            if (ids != null && pp != null)
            {
                for (int i = 0; i < ids.length; ++i)
                {
                    if (ids[i] != null)
                    {
                        Dimension sz = pp.getSize(ids[i]);
                        if (sz != null)
                        {
                            w = Math.max(w, sz.width);
                            h = Math.max(h, sz.height);
                        }
                        else
                            System.out.println("ERROR");
                    }
                }
            }
            size = new Dimension(w, h);
        }

        return size;
    }

    // Description copied from AnimationData
    public int getWidth()
    {
        return getSizeInternal().width;
    }

    // Description copied from AnimationData
    public int getHeight()
    {
        return getSizeInternal().height;
    }

    // Description copied from AnimationData
    public int getLength()
    {
        return (ids == null) ? 0 : ids.length;
    }

    /**
     * Returns the portfolio ids for this <code>PortfolioAnimation</code>. These
     * ids are used to reference <i>picture</i>s within an associated
     * {@link PicturePortfolio}.
     * 
     * @return the array of portfolio ids for the <i>picture</i>s on which this
     *         <code>PortfolioAnimation</code> is based
     * 
     * @see #setPortfolioId(String[])
     * @see #setPortfolioId(int,String)
     * @see #getPortfolioId(int)
     * @see #getPortfolio()
     * @see #setPortfolio(PicturePortfolio)
     */
    public String[] getPortfolioId()
    {
        // Return a copy
        String[] tmp = ids;
        String[] retIds = null;
        if (tmp != null)
        {
            retIds = new String[tmp.length];
            System.arraycopy(tmp, 0, retIds, 0, tmp.length);
        }
        return retIds;
    }

    /**
     * Sets the portfolio ids for this <code>PortfolioAnimation</code>. These
     * ids are used to reference <i>picture</i>s within an associated
     * {@link PicturePortfolio}.
     * 
     * @param portfolioId
     *            the array of portfolio ids for the <i>picture</i>s on which
     *            this <code>PortfolioAnimation</code> is based
     * @see #getPortfolioId()
     * @see #setPortfolioId(int,String)
     * @see #getPortfolioId(int)
     * @see #getPortfolio()
     * @see #setPortfolio(PicturePortfolio)
     */
    public void setPortfolioId(String portfolioId[])
    {
        size = null; // clear cached size
        ids = portfolioId;
    }

    /**
     * Returns the portfolio id for this <code>PortfolioAnimation</code> at the
     * specified index. These ids are used to reference <i>picture</i>s within
     * an associated {@link PicturePortfolio}.
     * 
     * @return the portfolio id for the <i>picture</i> at the specified index on
     *         which this <code>PortfolioAnimation</code> is based
     * 
     * @throws IndexOutOfBoundsException
     *             if no such frame id exists
     * @throws NullPointerException
     *             if no ids have been added
     * 
     * @see #setPortfolioId(String[])
     * @see #setPortfolioId(int,String)
     * @see #getPortfolioId()
     * @see #getPortfolio()
     * @see #setPortfolio(PicturePortfolio)
     */
    public String getPortfolioId(int i) throws IndexOutOfBoundsException, NullPointerException
    {
        return ids[i];
    }

    /**
     * Sets the portfolio id for this <code>PortfolioAnimation</code> at the
     * specified index. These ids are used to reference <i>picture</i>s within
     * an associated {@link PicturePortfolio}.
     * 
     * @param id
     *            he portfolio id for the <i>picture</i> at the specified index
     *            on which this <code>PortfolioAnimation</code> is based
     * 
     * @see #getPortfolioId()
     * @see #setPortfolioId(String[])
     * @see #getPortfolioId(int)
     * @see #getPortfolio()
     * @see #setPortfolio(PicturePortfolio)
     * 
     * @throws IndexOutOfBoundsException
     *             if the index is outside the current array bounds (
     *             {@link #setPortfolioId(String[])} must be used to change the
     *             size of the array)
     */
    public void setPortfolioId(int i, String portfolioId)
    {
        size = null; // clear cached size
        ids[i] = portfolioId;
    }

    /**
     * Returns the portfolio associated with this
     * <code>PortfolioAnimation</code>. This portfolio is expected to hold the
     * <i>picture</i> referenced by the portfolio {@link #getPortfolioId() id}.
     * 
     * @return the portfolio containing the <i>picture</i> on which this
     *         <code>PortfolioAnimation</code> is based
     * @see #getPortfolioId()
     * @see #getPortfolioId(int)
     * @see #setPortfolioId(String[])
     * @see #setPortfolioId(int,String)
     * @see #setPortfolio(PicturePortfolio)
     */
    public PicturePortfolio getPortfolio()
    {
        return pp;
    }

    /**
     * Sets the portfolio associated with this <code>PortfolioAnimation</code>.
     * This portfolio is expected to hold the <i>picture</i> referenced by the
     * portfolio {@link #getPortfolioId() id}.
     * 
     * @param portfolio
     *            the portfolio containing the <i>picture</i> on which this
     *            <code>PortfolioAnimation</code> is based
     * 
     * @see #getPortfolioId()
     * @see #getPortfolioId(int)
     * @see #setPortfolioId(String[])
     * @see #setPortfolioId(int,String)
     * @see #getPortfolio()
     */
    public void setPortfolio(PicturePortfolio portfolio)
    {
        size = null; // clear cached size
        pp = portfolio;
    }

    /**
     * Overrides <code>Object.toString()</code> to provide descriptive
     * information about this object.
     * 
     * @return a <code>String</code> representation of this object
     */
    public String toString()
    {
        String str;
        if (ids == null || ids.length == 0)
            str = "<empty>";
        else
        {
            StringBuffer buf = new StringBuffer();
            String comma = "";

            for (int i = 0; i < ids.length; ++i)
            {
                buf.append(comma);
                buf.append(ids[i]);
                comma = ",";
            }
            str = buf.toString();
        }
        return super.toString() + " [" + pp + ",[" + str + "]]";
    }

    /**
     * The <code>PicturePortfolio</code> on which this
     * <code>PortfolioAnimation</code> is based.
     */
    private PicturePortfolio pp;

    /**
     * The Strings that identify the <i>pictures</i> in the associated
     * <code>PicturePortfolio</code> which make up the frames of the animation.
     */
    private String[] ids;

    /**
     * The current total <i>size</i> of the animation frames, if currently set.
     */
    private Dimension size;
}
