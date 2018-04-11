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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.ImageObserver;

/**
 * An implementation of the <code>GraphicData</code> interface which is based
 * upon a <code>PicturePortfolio</code>.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.2 $, $Date: 2002/06/03 21:31:09 $
 */
public class PortfolioGraphic implements GraphicData
{

    /**
     * Default constructor. No {@link PicturePortfolio} or portfolio id is
     * assigned.
     * 
     * @see #setPortfolioId(String)
     * @see #setPortfolio(PicturePortfolio)
     */
    public PortfolioGraphic()
    {
    }

    /**
     * <code>PicturePortfolio</code>/portfolio id constructor. The given
     * <code>PicturePortfolio</code> and portfolio picture id are associated
     * with a newly constructed <code>PortfolioGraphic</code>.
     */
    public PortfolioGraphic(PicturePortfolio pp, String id)
    {
        setPortfolio(pp);
        setPortfolioId(id);
    }

    // Description copied from GraphicData
    public Dimension getSize()
    {
        Dimension d = null;
        if (pp != null) d = pp.getSize(id);
        if (d == null) d = new Dimension(0, 0);
        return d;
    }

    // Description copied from GraphicData
    public int getWidth()
    {
        return getSize().width;
    }

    // Description copied from GraphicData
    public int getHeight()
    {
        return getSize().height;
    }

    // Description copied from GraphicData
    public void draw(Graphics g, int x, int y, ImageObserver io)
    {
        pp.draw(g, id, x, y, io);
    }

    // Description copied from GraphicData
    public void draw(Graphics g, int x, int y, int width, int height, ImageObserver io)
    {
        pp.draw(g, id, x, y, width, height, io);
    }

    /**
     * Sets the portfolio id for this <code>PortfolioGraphic</code>. This id is
     * used to reference a <i>picture</i> within an associated
     * {@link PicturePortfolio}.
     * 
     * @param id
     *            the portfolio id of the <i>picture</i> on which this
     *            <code>PortfolioGraphic</code> is based
     * @see #getPortfolioId()
     * @see #getPortfolio()
     * @see #setPortfolio(PicturePortfolio)
     */
    public void setPortfolioId(String id)
    {
        this.id = id;
    }

    /**
     * Returns the portfolio id for this <code>PortfolioGraphic</code>. This id
     * is used to reference a <i>picture</i> within an associated
     * {@link PicturePortfolio}.
     * 
     * @return the portfolio id of the <i>picture</i> on which this
     *         <code>PortfolioGraphic</code> is based
     * @see #setPortfolioId(String)
     * @see #getPortfolio()
     * @see #setPortfolio(PicturePortfolio)
     */
    public String getPortfolioId()
    {
        return id;
    }

    /**
     * Sets the portfolio associated with this <code>PortfolioGraphic</code>.
     * This portfolio is expected to hold the <i>picture</i> referenced by the
     * portfolio {@link #getPortfolioId() id}.
     * 
     * @param portfolio
     *            the portfolio containing the <i>picture</i> on which this
     *            <code>PortfolioGraphic</code> is based
     * @see #getPortfolioId()
     * @see #setPortfolioId(String)
     * @see #getPortfolio()
     */
    public void setPortfolio(PicturePortfolio pp)
    {
        this.pp = pp;
    }

    /**
     * Returns the portfolio associated with this <code>PortfolioGraphic</code>.
     * This portfolio is expected to hold the <i>picture</i> referenced by the
     * portfolio {@link #getPortfolioId() id}.
     * 
     * @return the portfolio containing the <i>picture</i> on which this
     *         <code>PortfolioGraphic</code> is based
     * @see #getPortfolioId()
     * @see #setPortfolioId(String)
     * @see #setPortfolio(PicturePortfolio)
     */
    public PicturePortfolio getPortfolio()
    {
        return pp;
    }

    /**
     * Overrides <code>Object.toString()</code> to provide descriptive
     * information about this object.
     * 
     * @return a <code>String</code> representation of this object
     */
    public String toString()
    {
        return super.toString() + " [" + pp + "," + id + "]";
    }

    /**
     * The <code>PicturePortfolio</code> on which this
     * <code>PortfolioGraphic</code> is based.
     */
    private PicturePortfolio pp;

    /**
     * The String that identifies this <i>picture</i> in the associated
     * <code>PicturePortfolio</code>.
     */
    private String id;
}
