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

package org.cablelabs.gear.util;

import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.image.ImageObserver;

import org.cablelabs.gear.data.GraphicData;

/**
 * The <code>TileRender</code> class is a utility class useful in rendering
 * <i>tiled</i> images. Given a set of <i>tiles</i> corresponding to the
 * directions on a compass (i.e., N, NE, E, SE, S, SW, W, NW, and center), the
 * {@link #paintTiles paintTiles} method will fill in the desired screen
 * rectangle.
 * 
 * <p>
 * 
 * Exactly one of each <i>corner</i> (NE, NW, SE, and SW) tile is painted. The
 * <i>border</i> tiles (N, E, S, W) are used to fill horizontally (N and S) and
 * vertically (E and W) as appropriate. The <i>center</i> tile is used to fill
 * in both horizontal and vertical directions. If any corner tiles are not
 * provided, the border tiles will fill in the gap. If an appropriate border
 * tile is not provided, then the center tile will fill in the gaps.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.6 $, $Date: 2002/06/03 21:31:07 $
 */
public class TileRender
{
    /**
     * Index constant which references the "NORTH" tile in an array of tiles.
     */
    public static final int NORTH = 0;

    /**
     * Index constant which references the "NORTHEAST" tile in an array of
     * tiles.
     */
    public static final int NORTHEAST = 1;

    /**
     * Index constant which references the "EAST" tile in an array of tiles.
     */
    public static final int EAST = 2;

    /**
     * Index constant which references the "SOUTHEAST" tile in an array of
     * tiles.
     */
    public static final int SOUTHEAST = 3;

    /**
     * Index constant which references the "SOUTH" tile in an array of tiles.
     */
    public static final int SOUTH = 4;

    /**
     * Index constant which references the "SOUTHWEST" tile in an array of
     * tiles.
     */
    public static final int SOUTHWEST = 5;

    /**
     * Index constant which references the "WEST" tile in an array of tiles.
     */
    public static final int WEST = 6;

    /**
     * Index constant which references the "NORTHWEST" tile in an array of
     * tiles.
     */
    public static final int NORTHWEST = 7;

    /**
     * Index constant which references the "CENTER" tile in an array of tiles.
     */
    public static final int CENTER = 8;

    private static final Dimension EMPTY = new Dimension(0, 0);

    /** Uninstantiable. */
    private TileRender()
    {
    }

    /**
     * Used to paint the tiles represented by <code>GraphicData</code> objects.
     * <p>
     * The placement of the tiles is calculated according to the equations
     * summed up in the following table.
     * 
     * <table border>
     * <tr>
     * <th>tile</th>
     * <th>x</th>
     * <th>y</th>
     * <th>end-x</th>
     * <th>end-y</th>
     * </tr>
     * 
     * <tr>
     * <td><b>C</b></td>
     * <td> <code>minimum(NW.width, W.width, SW.width)</code>*</td>
     * <td> <code>minimum(NE.height, N.height, NW.height)</code>*</td>
     * <td> <code>width - minimum(NE.width, E.width, SE.width)</code>*</td>
     * <td> <code>height - minimum(SE.height, S.height, SW.height)</code>*</td>
     * </tr>
     * 
     * <tr>
     * <td><b>N</b></td>
     * <td> <code>NW.width</code></td>
     * <td> <code>0</code></td>
     * <td> <code>width - NE.width</code></td>
     * <td> <code>N.height</code></td>
     * </tr>
     * 
     * <tr>
     * <td><b>E</b></td>
     * <td> <code>width - E.width</code></td>
     * <td> <code>NE.height</code></td>
     * <td> <code>width</code></td>
     * <td> <code>height - SE.height</code></td>
     * </tr>
     * 
     * <tr>
     * <td><b>S</b></td>
     * <td> <code>SW.width</code></td>
     * <td> <code>height - S.height</code></td>
     * <td> <code>width - SE.width</code></td>
     * <td> <code>height</code></td>
     * </tr>
     * 
     * <tr>
     * <td><b>W</b></td>
     * <td> <code>0</code></td>
     * <td> <code>NW.height</code></td>
     * <td> <code>W.width</code></td>
     * <td> <code>height - SW.height</code></td>
     * </tr>
     * 
     * <tr>
     * <td><b>NE</b></td>
     * <td> <code>width - NE.width</code></td>
     * <td> <code>0</code></td>
     * <td> <code>width</code></td>
     * <td> <code>NE.height</code></td>
     * </tr>
     * 
     * <tr>
     * <td><b>SE</b></td>
     * <td> <code>width - SE.width</code></td>
     * <td> <code>height - SE.height</code></td>
     * <td> <code>width</code></td>
     * <td> <code>height</code></td>
     * </tr>
     * 
     * <tr>
     * <td><b>SW</b></td>
     * <td> <code>0</code></td>
     * <td> <code>height - SW.height</code></td>
     * <td> <code>SW.width</code></td>
     * <td> <code>height</code></td>
     * </tr>
     * 
     * <tr>
     * <td><b>NW</b></td>
     * <td> <code>0</code></td>
     * <td> <code>0</code></td>
     * <td> <code>NW.width</code></td>
     * <td> <code>NW.height</code></td>
     * </tr>
     * 
     * <tr>
     * <td colspan=5>*Calculations are actually more complex, to avoid
     * over-filling</td>
     * </tr>
     * </table>
     * 
     * @param g
     *            the graphics context to which all drawing is performed
     * @param tiles
     *            the <code>GraphicData[9]</code> of tiles to use
     * @param x
     *            the horizontal offset at which the tiles should be drawn
     * @param y
     *            the vertical offset at which the tiles should be drawn
     * @param width
     *            the width of the tiled rectangle
     * @param height
     *            the height of the tiled rectangle
     * @param io
     *            an <code>ImageObserver</code> used during drawing (generally
     *            this is the <code>Component</code> being drawn to)
     */
    public static void paintTiles(Graphics g, GraphicData[] tiles, int x, int y, int width, int height, ImageObserver io)
    {
        final Dimension C = getSize(tiles, CENTER), N = getSize(tiles, NORTH), E = getSize(tiles, EAST), S = getSize(
                tiles, SOUTH), W = getSize(tiles, WEST), NE = getSize(tiles, NORTHEAST), SE = getSize(tiles, SOUTHEAST), SW = getSize(
                tiles, SOUTHWEST), NW = getSize(tiles, NORTHWEST);

        int X, Y, endX, endY;

        Graphics g2 = g.create(x, y, width, height);
        try
        {

            // "Layout" and draw CENTER tiles (filling as necessary)
            if (C != EMPTY)
            {
                X = calcCenterOffset(W.width, NW.width, SW.width, NW.height, SW.height, height);
                Y = calcCenterOffset(N.height, NW.height, NE.height, NW.width, NE.width, width);
                endX = width - calcCenterOffset(E.width, NE.width, SE.width, NE.height, SE.height, height);
                endY = height - calcCenterOffset(S.height, SW.height, SE.height, SW.width, SE.width, width);

                final int saveY = Y;
                for (; X < endX; X += C.width)
                    for (Y = saveY; Y < endY; Y += C.height)
                        tiles[CENTER].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }

            // "Layout" and draw "SIDE" tiles (filling as necessary)
            if (N != EMPTY)
            {
                X = NW.width;
                Y = 0;
                endX = width - NE.width;

                for (; X < endX; X += N.width)
                    tiles[NORTH].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
            if (S != EMPTY)
            {
                X = SW.width;
                Y = height - S.height;
                endX = width - SE.width;

                for (; X < endX; X += S.width)
                    tiles[SOUTH].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
            if (E != EMPTY)
            {
                X = width - E.width;
                Y = NE.height;
                endY = height - SE.height;

                for (; Y < endY; Y += E.height)
                    tiles[EAST].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
            if (W != EMPTY)
            {
                X = 0;
                Y = NW.height;
                endY = height - SW.height;

                for (; Y < endY; Y += W.height)
                    tiles[WEST].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }

            // Draw "CORNER" tiles
            if (NE != EMPTY)
            {
                X = width - NE.width;
                Y = 0;

                tiles[NORTHEAST].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
            if (NW != EMPTY)
            {
                X = 0;
                Y = 0;

                tiles[NORTHWEST].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
            if (SE != EMPTY)
            {
                X = width - SE.width;
                Y = height - SE.height;

                tiles[SOUTHEAST].draw(g2, X, Y, io);
            }
            if (DEBUG) try
            {
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
            if (SW != EMPTY)
            {
                X = 0;
                Y = height - SW.height;

                tiles[SOUTHWEST].draw(g2, X, Y, io);
            }
            // Done! Finally!!
        }
        finally
        {
            // Reset Graphics clipping/translation
            g2.dispose();
        }
    }

    /**
     * Utility method used to create an <code>Image</code> using tiled
     * <i>pictures</i> represented by <code>GraphicData</code> objects.
     * 
     * @param tiles
     *            the <code>GraphicData[9]</code> of tiles to use
     * @param width
     *            the width of the tiled rectangle
     * @param height
     *            the height of the tiled rectangle
     * @param c
     *            a <code>Component</code> which is used to create a new image
     *            (using the <code>Component.createImage(width,height)</code>
     *            method
     * @return the <code>Image</code> created from tiling the given
     *         <code>GraphicData</code> objects
     */
    public static Image createTiledImage(GraphicData[] tiles, int width, int height, java.awt.Component c)
    {
        Image img = c.createImage(width, height);

        paintTiles(img.getGraphics(), tiles, 0, 0, width, height, c);
        return img;
    }

    /**
     * Used to calculate an offset for the bounds of the center tiles. This is
     * calculated in the following way:
     * <p>
     * <ol>
     * <li>if we have a border, then it will be used to fill the space left by
     * any missing corners (not the center)
     * <li>else, if we have corners and they are enough to not require center
     * fill, then no center fill is necessary
     * <li>else center filling is necessary
     * </ol>
     * 
     * @param border
     *            the width or height of the border component; this is height
     *            for N/S and width for E/W
     * @param corner1
     *            the width or height of a corner component; this is the same
     *            measurement as for the border
     * @param corner2
     *            the width or height of another corner component; this is the
     *            same measurement as for the border
     * @param corner1Opp
     *            the opposite measurement from <code>corner1</code>
     * @param corner2Opp
     *            the opposite measurement from <code>corner2</code>
     * @param threshold
     *            the total bounds width or height; this is height for border of
     *            E/W and width for border of N/S
     * @return a calculated offset (from the outer bounds) for one side of the
     *         center tiling
     */
    private final static int calcCenterOffset(int border, int corner1, int corner2, int corner1Opp, int corner2Opp,
            int threshold)
    {
        if (border != 0)
        {
            int ofs = border;
            if (corner1 != 0) ofs = Math.min(ofs, corner1);
            if (corner2 != 0) ofs = Math.min(ofs, corner2);
            return ofs;
        }
        else if (corner1Opp + corner2Opp >= threshold)
            return Math.min(corner1, corner2);
        else
            return 0;
    }

    /**
     * Calculate the minimum size that is appropriate for the given tiles (as
     * found in the given <code>GraphicData</code> array).
     * 
     * @param tiles
     *            the <code>GraphicData[9]</code> of tiles to use
     * @param all
     *            if <code>true</code> then the minimum should be the sum of
     *            <i>all</i> tiles, placed in their respective places at most
     *            once; if <code>false</code> then the minimum should be the
     *            smallest sum necessary for a tiling <i>style</i> (full,
     *            horizontal, vertical, or wallpaper -- depending upon what is
     *            present).
     */
    public static Dimension getMinimumSize(GraphicData[] tiles, boolean all)
    {
        Dimension C = getSize(tiles, CENTER), N = getSize(tiles, NORTH), E = getSize(tiles, EAST), S = getSize(tiles,
                SOUTH), W = getSize(tiles, WEST), NE = getSize(tiles, NORTHEAST), SE = getSize(tiles, SOUTHEAST), SW = getSize(
                tiles, SOUTHWEST), NW = getSize(tiles, NORTHWEST);

        // Sum ALL tiles (at most once)
        if (all)
            return new Dimension(Math.max(E.width, Math.max(NE.width, SE.width))
                    + Math.max(C.width, Math.max(N.width, S.width)) + Math.max(W.width, Math.max(NW.width, SW.width)),
                    Math.max(N.height, Math.max(NE.height, NW.height))
                            + Math.max(C.height, Math.max(E.height, W.height))
                            + Math.max(S.height, Math.max(SE.height, SW.height)));
        // FULL style (using corners only)
        // CORNER/CENTER style (using corners only)
        else if (NE != EMPTY && NW != EMPTY && SE != EMPTY && SW != EMPTY)
            return new Dimension(Math.max(NE.width, SE.width) + Math.max(NW.width, SW.width), Math.max(NE.height,
                    NW.height)
                    + Math.max(SE.height, SW.height));
        // HORIZONTAL style (using sides only)
        else if (E != EMPTY && W != EMPTY)
            return new Dimension(E.width + W.width, Math.max(E.height, W.height));
        // VERTICAL style (using sides only)
        else if (S != EMPTY && N != EMPTY)
            return new Dimension(Math.max(N.width, S.width), N.height + S.height);
        // WALLPAPER style
        else if (C != EMPTY) return C;
        return new Dimension(0, 0);
    }

    /**
     * Returns the size of the requested tile.
     * 
     * @return the size of the requested tile or <code>EMPTY</code> if there is
     *         no such tile
     */
    private static Dimension getSize(GraphicData[] tiles, int i)
    {
        return (tiles != null && tiles[i] != null) ? tiles[i].getSize() : EMPTY;
    }

    /**
     * This method can be used to wallpaper a single <code>Image</code> tile
     * across a rectangular area. Essentially, clipping to the requested bounds
     * and repetitively drawing the given image is taken care of.
     * <p>
     * This method is similar to {@link #paintTiles paintTiles} except that it
     * only uses a <i>single</i> <code>Image</code> as opposed to multiple
     * <i>tiles</i>.
     * 
     * @param i
     *            the <code>Image</code> to wallpaper across a given rectangle
     * @param g
     *            the graphics context to which all drawing is performed
     * @param bounds
     *            the rectangle within which wallpapering should be done
     * @param io
     *            an <code>ImageObserver</code> used during drawing (generally
     *            this is the <code>Component</code> being drawn to)
     * 
     * @see #paintTiles
     */
    public static void paintWallpaper(Image i, Graphics g, Rectangle bounds, ImageObserver io)
    {
        int w = i.getWidth(io);
        int h = i.getHeight(io);

        // Local copies
        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;

        Graphics g2 = g.create(x, y, width, height);
        try
        {
            // Tile the image
            for (int r = 0; r < width; r += w)
                for (int c = 0; c < height; c += h)
                    g2.drawImage(i, r, c, io);
        }
        finally
        {
            // Clean up
            g2.dispose();
        }
    }

    private static final boolean DEBUG = false;
}
