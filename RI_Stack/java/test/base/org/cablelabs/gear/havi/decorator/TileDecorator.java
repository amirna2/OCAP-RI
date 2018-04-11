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

package org.cablelabs.gear.havi.decorator;

import org.cablelabs.gear.util.TileRender;
import org.cablelabs.gear.util.PicturePortfolio;
import org.cablelabs.gear.data.GraphicData;

import org.havi.ui.HLook;
import org.havi.ui.HVisible;

/**
 * The <code>TileDecorator</code> is a <code>StateDecorator</code> that draws a
 * tiled <i>background</i> based on <i>tiles</i> represented by
 * {@link org.cablelabs.gear.data.GraphicData} objects.
 * 
 * <p>
 * 
 * Each <i>tile</i> is referenced by an index. Symbols representing the indices
 * correspond to the compass positions of the tiles. They are:
 * 
 * <table border>
 * <tr>
 * <td> <code>NORTHWEST</code>
 * <td>
 * <td> <code>NORTH</code>
 * <td>
 * <td> <code>NORHTEAST</code>
 * <td>
 * </tr>
 * <tr>
 * <td> <code>WEST</code>
 * <td>
 * <td> <code>CENTER</code>
 * <td>
 * <td> <code>EAST</code>
 * <td>
 * </tr>
 * <tr>
 * <td> <code>SOUTHWEST</code>
 * <td>
 * <td> <code>SOUTH</code>
 * <td>
 * <td> <code>SOUTHEAST</code>
 * <td>
 * </tr>
 * </table>
 * 
 * It is recommended that tile sizes be uniform. That is that the widths/heights
 * of tiles in a column/row be identical. While this is not required, it should
 * produce better looking results.
 * 
 * @see #showLook(Graphics, HVisible, int)
 * @see TileRender
 * 
 * @author Aaron Kamienski
 * @author Tom Henriksen
 * @author Jeff Bonin (havi 1.0.1 update)
 * @version $Id: TileDecorator.java,v 1.7 2002/06/03 21:32:31 aaronk Exp $
 */
public class TileDecorator extends StateDecorator
{
    /**
     * Index constant which references the "NORTH" tile in an array of tiles.
     */
    public static final int NORTH = TileRender.NORTH;

    /**
     * Index constant which references the "NORTHEAST" tile in an array of
     * tiles.
     */
    public static final int NORTHEAST = TileRender.NORTHEAST;

    /**
     * Index constant which references the "EAST" tile in an array of tiles.
     */
    public static final int EAST = TileRender.EAST;

    /**
     * Index constant which references the "SOUTHEAST" tile in an array of
     * tiles.
     */
    public static final int SOUTHEAST = TileRender.SOUTHEAST;

    /**
     * Index constant which references the "SOUTH" tile in an array of tiles.
     */
    public static final int SOUTH = TileRender.SOUTH;

    /**
     * Index constant which references the "SOUTHWEST" tile in an array of
     * tiles.
     */
    public static final int SOUTHWEST = TileRender.SOUTHWEST;

    /**
     * Index constant which references the "WEST" tile in an array of tiles.
     */
    public static final int WEST = TileRender.WEST;

    /**
     * Index constant which references the "NORTHWEST" tile in an array of
     * tiles.
     */
    public static final int NORTHWEST = TileRender.NORTHWEST;

    /**
     * Index constant which references the "CENTER" tile in an array of tiles.
     */
    public static final int CENTER = TileRender.CENTER;

    /**
     * Default constructor. This is (currently) a <i>leaf</i> decorator look. No
     * <i>tiles</i> have been assigned.
     * 
     * @see #setComponentLook(HLook)
     * @see #getComponentLook()
     */
    public TileDecorator()
    {
        this(null);
    }

    /**
     * Constructs a <code>TileDecorator</code> based on the given
     * <code>componentLook</code>. No <i>tiles</i> have been assigned.
     * 
     * @param componentLook
     *            The <code>HLook</code> to which this decorator is adding
     *            responsibilities; can be <code>null</code> if none is desired
     *            (i.e., this is a <i>leaf</i> look).
     */
    public TileDecorator(HLook componentLook)
    {
        super(componentLook);
    }

    /**
     * Constructs a <code>TileDecorator</code> based on the given
     * <code>componentLook</code> and <code>GraphicData</code> tiles.
     * 
     * @param componentLook
     *            The <code>HLook</code> to which this decorator is adding
     *            responsibilities; can be <code>null</code> if none is desired
     *            (i.e., this is a <i>leaf</i> look).
     * @param bitMask
     *            the bitMask specifying the states to operate in
     * @param tiles
     *            the <code>GraphicData</code> tiles to use
     */
    public TileDecorator(HLook componentLook, int bitMask, GraphicData[] tiles)
    {
        super(componentLook, bitMask);
        setTiles(tiles);
    }

    /**
     * Creates a <code>TileDecorator</code>, initialized to operate in the given
     * set of states.
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param states
     *            an array of {@link org.havi.ui.HState HState-defined} states
     * @param tiles
     *            the <code>GraphicData</code> tiles to use
     * 
     * @throws IllegalArgumentException
     *             if an invalid state is specified
     * @throws NullPointerException
     *             if <code>states</code> is <code>null</code>
     */
    public TileDecorator(HLook look, int[] states, GraphicData[] tiles)
    {
        super(look, states);
        setTiles(tiles);
    }

    /**
     * Constructs a <code>TileDecorator</code> based on the given
     * <code>componentLook</code> and
     * {@link org.cablelabs.gear.util.PicturePortfolio PicturePortfolio-based}.
     * tiles.
     * <p>
     * Tiles are referenced by <code>Strings</code> corresponding to the regular
     * tile indices (e.g., <code>NORTH</code> becomes <code>"NORTH"</code>).
     * 
     * @param componentLook
     *            The <code>HLook</code> to which this decorator is adding
     *            responsibilities; can be <code>null</code> if none is desired
     *            (i.e., this is a <i>leaf</i> look).
     * @param bitMask
     *            the bitMask specifying the states to operate in
     * @param tiles
     *            the <code>PicturePortfolio</code> containing the tiles to use
     */
    public TileDecorator(HLook componentLook, int bitMask, PicturePortfolio tiles)
    {
        super(componentLook, bitMask);
        setTiles(tiles);
    }

    /**
     * Creates a <code>TileDecorator</code>, initialized to operate in the given
     * set of states.
     * <p>
     * Tiles are referenced by <code>Strings</code> corresponding to the regular
     * tile indices (e.g., <code>NORTH</code> becomes <code>"NORTH"</code>).
     * 
     * @param look
     *            the component <code>HLook</code>
     * @param states
     *            an array of {@link org.havi.ui.HState HState-defined} states
     * @param tiles
     *            the <code>PicturePortfolio</code> containing the tiles to use
     * 
     * @throws IllegalArgumentException
     *             if an invalid state is specified
     * @throws NullPointerException
     *             if <code>states</code> is <code>null</code>
     */
    public TileDecorator(HLook look, int[] states, PicturePortfolio tiles)
    {
        super(look, states);
        setTiles(tiles);
    }

    /**
     * Paints the tiles associated with this <code>TileDecorator</code> in the
     * area appropriate for the given <code>HVisible</code>. The individual
     * tiles are indexed by the symbolic indices:
     * 
     * <table border>
     * <tr>
     * <td> <code>NORTHWEST</code>
     * <td>
     * <td> <code>NORTH</code>
     * <td>
     * <td> <code>NORHTEAST</code>
     * <td>
     * </tr>
     * <tr>
     * <td> <code>WEST</code>
     * <td>
     * <td> <code>CENTER</code>
     * <td>
     * <td> <code>EAST</code>
     * <td>
     * </tr>
     * <tr>
     * <td> <code>SOUTHWEST</code>
     * <td>
     * <td> <code>SOUTH</code>
     * <td>
     * <td> <code>SOUTHEAST</code>
     * <td>
     * </tr>
     * </table>
     * 
     * The <i>corner</i> tiles are drawn exactly once. The <i>center</i> tile is
     * repeated as necessary along both the x and y axes. The <i>top</i>,
     * <i>bottom</i>, and <i>side</i> tiles are repeated along their
     * corresponding x or y axes as necessary.
     * 
     * <p>
     * 
     * The tile space is determined by the <i>inner</i> bounds of the given
     * <code>HVisible</code>. That is, the dimensions of the
     * <code>HVisible</code> with the border spacing subtracted.
     * 
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @return <code>null</code>
     * 
     * @see TileRender#paintTiles(Graphics,GraphicData[],int,int,int,int,ImageObserver)
     */
    public Object showLook(java.awt.Graphics g, HVisible visible)
    {
        // Determine inner bounds
        java.awt.Dimension size = visible.getSize();

        size.width -= getBorderWidth(visible);
        size.height -= getBorderHeight(visible);

        // Paint the tiles
        TileRender.paintTiles(g, tiles, getInsets(visible).left, getInsets(visible).top, size.width, size.height,
                visible);
        return null;
    }

    /**
     * Sets the set of tiles to be used for rendering.
     * 
     * @param tiles
     *            <code>GraphicData[9]</code> array of tiles to use
     * 
     * @throws NullPointerException
     *             if <code>tiles</code> is <code>null</code>
     * @throws IndexOutOfBoundsException
     *             if <code>tiles</code> is too short
     * @throws IllegalArgumentException
     *             if <code>tiles</code> is not of the correct length
     */
    public void setTiles(GraphicData[] tiles)
    {
        if (tiles.length > 9) throw new IllegalArgumentException("Too many tiles");

        // If input array is too short, should throw exception immediately.
        for (int i = 9; i-- > 0;)
            this.tiles[i] = tiles[i];
    }

    /**
     * Sets the tile to be used for rendering specified by the given index.
     * 
     * @param i
     *            index
     * @param tile
     *            <code>GraphicData</code> tile
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is greater than 9 or less than 0
     */
    public void setTiles(int i, GraphicData tile)
    {
        tiles[i] = tile;
    }

    /**
     * Returns the set of tiles to be used for rendering.
     * 
     * @return an 9-element <code>GraphicData</code> array with each entry
     *         corresponding to a tile
     */
    public GraphicData[] getTiles()
    {
        GraphicData[] array = new GraphicData[tiles.length];

        System.arraycopy(tiles, 0, array, 0, array.length);

        return array;
    }

    /**
     * Returns the tile to be used for rendering specified by the given index.
     * 
     * @param i
     *            index
     * @return the tile to be used for rendering specified by the given index
     * 
     * @throws IndexOutOfBoundsException
     *             if <code>i</code> is greater than 9 or less than 0
     */
    public GraphicData getTiles(int i)
    {
        return tiles[i];
    }

    /**
     * A convenience method for setting <code>GraphicData</code> tiles using a
     * {@link org.cablelabs.gear.util.PicturePortfolio}.
     * <code>GraphicData</code> tiles are referenced in the
     * <code>PicturePortfolio</code> using the <code>String</code> that
     * corresponds to the symbolic index (e.g., <code>"NORTH"</code> instead of
     * <code>NORTH</code>).
     * <p>
     * This method does the equivalent of:
     * 
     * <pre>
     * setTile(NORTH, pp.getPicture("NORTH");
     * ...
     * </pre>
     * 
     * @param pp
     *            the <code>PicturePortfolio</code> that should contain the
     *            tiles
     */
    public void setTiles(PicturePortfolio pp)
    {
        setTiles(NORTH, pp.getPicture("NORTH"));
        setTiles(NORTHEAST, pp.getPicture("NORTHEAST"));
        setTiles(EAST, pp.getPicture("EAST"));
        setTiles(SOUTHEAST, pp.getPicture("SOUTHEAST"));
        setTiles(SOUTH, pp.getPicture("SOUTH"));
        setTiles(SOUTHWEST, pp.getPicture("SOUTHWEST"));
        setTiles(WEST, pp.getPicture("WEST"));
        setTiles(NORTHWEST, pp.getPicture("NORTHWEST"));
        setTiles(CENTER, pp.getPicture("CENTER"));
    }

    /**
     * Returns whether <i>all</i> available tiles are considered when performing
     * perferred/minimum size calculations.
     * 
     * @return if <code>true</code> then all available tiles are to be used in
     *         the sizing calculations; else only non-<i>fill</i> tiles are to
     *         be used.
     * @see #setConsiderAllTiles(boolean)
     * @see #getPreferredSize(HVisible)
     * @see #getMinimumSize(HVisible)
     */
    public boolean isConsiderAllTiles()
    {
        return useAllTiles;
    }

    /**
     * Specifies whether <i>all</i> available tiles should be used in the
     * preferred/minimum size calculations.
     * 
     * @param bool
     *            if <code>true</code> then all available tiles should be used
     *            in the sizing calculations; else only non-<i>fill</i> tiles
     *            are to be used.
     * @see #isConsiderAllTiles()
     */
    public void setConsiderAllTiles(boolean bool)
    {
        useAllTiles = bool;
    }

    /**
     * Returns a dimension equal to the minimum size possible (as there really
     * is no preferred size, other than greater-than-or-equal-to the minimum).
     * The maximum of this calculation and the dimensions returned by
     * <code>getComponentLook().getPreferredSize()</code> is returned.
     * 
     * @return the minimum size possible
     */
    public java.awt.Dimension getPreferredSize(HVisible visible)
    {
        java.awt.Dimension d = calcMinimumSize(visible);
        java.awt.Dimension superd = super.getPreferredSize(visible);
        d.width = Math.max(d.width, superd.width);
        d.height = Math.max(d.height, superd.height);

        return d;
    }

    /**
     * Returns a maximum size of <code>(Short.MAX_VALUE, Short.MAX_VALUE)</code>
     * .
     * 
     * @return <code>(Short.MAX_VALUE, Short.MAX_VALUE)</code>.
     */
    public java.awt.Dimension getMaximumSize(HVisible visible)
    {
        return new java.awt.Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /**
     * Returns a dimension equal to the minimum size allowed for this
     * <code>HVisible</code> assuming the current tiling parameters. The maximum
     * of this calculation and the dimensions returned by
     * <code>getComponentLook().getMinimumSize()</code> is returned.
     * 
     * @return maximum of the minimum size calculation for the given
     *         <code>visible</code> and the dimensions returned by
     *         <code>getComponentLook().getMinimumSize()</code>
     * @see TileRender#getMinimumSize(GraphicData[],boolean)
     */
    public java.awt.Dimension getMinimumSize(HVisible visible)
    {
        java.awt.Dimension d = calcMinimumSize(visible);
        java.awt.Dimension superd = super.getMinimumSize(visible);
        d.width = Math.max(d.width, superd.width);
        d.height = Math.max(d.height, superd.height);

        return d;
    }

    /**
     * Returns a dimension specifying the minimum size for this component given
     * current tiling for <i>all</i> states. This is the smallest dimension to
     * show each state's tiling at its smallest size.
     * 
     * @return the minimum size possible
     * @see TileRender#getMinimumSize(GraphicData[],boolean)
     */
    protected java.awt.Dimension calcMinimumSize(HVisible visible)
    {
        return TileRender.getMinimumSize(tiles, isConsiderAllTiles());
    }

    /**
     * If <code>true</code> then all available tiles should be used in the
     * sizing calculations. If <code>false</code> then only non-<i>fill</i>
     * tiles are used.
     * 
     * @see #isConsiderAllTiles()
     * @see #setConsiderAllTiles(boolean)
     */
    private boolean useAllTiles;

    /**
     * The current set of tiles.
     */
    private final GraphicData[] tiles = new GraphicData[9];
}
