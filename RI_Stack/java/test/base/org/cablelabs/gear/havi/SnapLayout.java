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

package org.cablelabs.gear.havi;

import org.havi.ui.HNavigable;
import java.awt.LayoutManager;
import java.awt.Container;
import java.awt.Component;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import org.cablelabs.gear.WrapperLayout;
import java.awt.event.KeyEvent;
import java.util.Hashtable;

/**
 * The SnapLayout class is intended to be used to simplify the setup of focus
 * traversals among HAVi {@link HNavigable} components. The SnapLayout layout
 * manager does not actually layout any components, it simply determines the
 * <i>best</i> focus traversal settings for each <code>HNavigable</code>
 * component. A SnapLayout employs the Decorator Design pattern; it is composed
 * of a component layout manager (or the <i>null</i> layout manager if absolute
 * positioning is desired).
 * <p>
 * For example, where one might see the following:
 * 
 * <pre>
 *   panel1.setLayout(new FlowLayout());
 *   ...
 *   panel2.setLayout(null);
 * </pre>
 * 
 * One can drop in the SnapLayout:
 * 
 * <pre>
 *   panel1.setLayout(new SnapLayout(new FlowLayout()));
 *   ...
 *   panel2.setLayout(new SnapLayout(null));
 * </pre>
 * 
 * Constraints can be associated with each component added to a container using
 * the <code>SnapLayout</code>. By using a {@link SnapLayoutConstraints} object
 * with a component one can instruct the <code>SnapLayout</code> to maintain
 * certain presets (e.g., don't assign a traversal to the <i>up</i> key) or
 * ignore a component altogether.
 * <p>
 * This implementation is based on figuring the distance from center of all
 * components. Traversals from the current component are chosen based on which
 * is closer in a given direction.
 * <p>
 * Note that, currently, favor is shown to elements in the order they were added
 * to the container. I.e., when deciding between two components for a traversal,
 * all else being equal the first one added to the container wins.
 * <p>
 * Future additions for tie-breaker heuristics could include:
 * <ul>
 * <li>Choose the one with the smallest <i>slope</i>.
 * <li>Favor options based on opposite dimension direction (e.g., when going
 * right, favor the <i>lower</i> of two choices).
 * </ul>
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.8 $ $Date: 2002/06/03 21:33:19 $
 * 
 * @see SnapLayoutConstraints
 * @see HNavigable
 */
public class SnapLayout extends WrapperLayout
{
    /**
     * Creates a <code>SnapLayout</code> which is not <i>wrapped</i> around an
     * existing layout manager. This is equivalent to using the
     * {@link #SnapLayout(LayoutManager) SnapLayout(null)} constructor.
     * 
     * @see #setLayout(LayoutManager)
     * @see #getLayout()
     */
    public SnapLayout()
    {
        this(null);
    }

    /**
     * Creates a <code>SnapLayout</code> using the given layout as a base.
     * 
     * @param layout
     *            the <code>LayoutManager</code> on which this
     *            <code>SnapLayout</code> is based.
     */
    public SnapLayout(LayoutManager layout)
    {
        super(layout);
    }

    /**
     * Creates a <code>SnapLayout</code> using the given layout as a base.
     * 
     * @param layout
     *            the <code>LayoutManager</code> on which this
     *            <code>SnapLayout</code> is based.
     * @param straight
     *            the importance of straight with respect to close
     * @param close
     *            the importance of close with respect to straight
     */
    public SnapLayout(LayoutManager layout, int straight, int close)
    {
        super(layout);
        setSlopeRatio(straight, close);
    }

    /**
     * Creates a <code>SnapLayout</code> using the given layout as a base.
     * 
     * @param layout
     *            the <code>LayoutManager</code> on which this
     *            <code>SnapLayout</code> is based.
     * @param straight
     *            the importance of straight with respect to close
     * @param close
     *            the importance of close with respect to straight
     * @param wrap
     *            whether wrapping is turned on or not
     */
    public SnapLayout(LayoutManager layout, int straight, int close, boolean wrap)
    {
        this(layout, straight, close);
        setWrap(wrap);
    }

    /**
     * Creates a <code>SnapLayout</code> using the given layout as a base. If
     * <code>closest</code> is <code>true</code> then objects which are closer
     * are favored. If it is <code>false</code> then objects in a straight line
     * are favored.
     * 
     * @param layout
     *            the <code>LayoutManager</code> on which this
     *            <code>SnapLayout</code> is based.
     * @param closest
     *            if <code>closest</code> is <code>true</code> then objects
     *            which are closer are favored. If it is <code>false</code> then
     *            objects in a straight line are favored.
     */
    public SnapLayout(LayoutManager layout, boolean closest)
    {
        this(layout, closest ? 0 : 1, closest ? 1 : 0);
    }

    /**
     * Creates a <code>SnapLayout</code> using the given layout as a base. If
     * <code>closest</code> is <code>true</code> then objects which are closer
     * are favored. If it is <code>false</code> then objects in a straight line
     * are favored.
     * 
     * @param layout
     *            the <code>LayoutManager</code> on which this
     *            <code>SnapLayout</code> is based.
     * @param closest
     *            if <code>closest</code> is <code>true</code> then objects
     *            which are closer are favored. If it is <code>false</code> then
     *            objects in a straight line are favored.
     * @param wrap
     *            whether wrapping is turned on or not
     */
    public SnapLayout(LayoutManager layout, boolean closest, boolean wrap)
    {
        this(layout, closest);
        setWrap(wrap);
    }

    // Description copied from superclass/interface
    public void layoutContainer(Container parent)
    {
        super.layoutContainer(parent);
        setFocusTraversal(parent);
    }

    /**
     * Removes the specified component from the layout.
     * <p>
     * 
     * @param comp
     *            the component to be removed
     */
    public void removeLayoutComponent(Component comp)
    {
        if (constraints != null && comp instanceof HNavigable) constraints.remove(comp);

        super.removeLayoutComponent(comp);
    }

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     * <p>
     * If the constraints object is
     * <code>instanceof SnapLayoutConstraints</code> and the component is
     * <code>instanceof HNavigable</code> then the constraints are used by this
     * object. The wrapped constraints object is passed on to the component
     * layout manager (if there is one).
     * <p>
     * If the constraints object is not
     * <code>instanceof SnapLayoutConstraints</code>, then it is passed directly
     * to the component layout. As such, one need not wrap a constraint object
     * in a <code>SnapLayoutConstraint</code> if such constraints are not
     * necessary.
     * <p>
     * 
     * @see #setConstraints(Component, SnapLayoutConstraints)
     * 
     * @param comp
     *            the component to be added
     * @param constraints
     *            where/how the component is added to the layout.
     */
    public void addLayoutComponent(Component comp, Object constraints)
    {
        if (constraints instanceof SnapLayoutConstraints)
        {
            SnapLayoutConstraints slc = (SnapLayoutConstraints) constraints;

            // Save constraints for later
            setConstraints(comp, slc);

            // Pass on the wrapped constraints
            constraints = slc.constraints;
        }
        super.addLayoutComponent(comp, constraints);
    }

    /****************** SnapLayout *********************/

    /**
     * Sets the constraints for the specified component in this layout. Note
     * that the {@link SnapLayoutConstraints#constraints} are not passed on to
     * the component {@link #getLayout layout}.
     * 
     * @param comp
     *            the component to be modified.
     * @param constraints
     *            the constraints to be applied.
     * @see #addLayoutComponent(Component, Object)
     */
    public void setConstraints(Component comp, SnapLayoutConstraints c)
    {
        if (comp instanceof HNavigable)
        {
            synchronized (this)
            {
                if (constraints == null) constraints = new Hashtable();
            }

            if (c != null)
                constraints.put(comp, c.clone());
            else
                constraints.remove(comp);
        }
    }

    /**
     * Returns the constraints associated with the given component. If none were
     * assigned, the default constraints object is assumed (i.e., one created
     * with the default constructor).
     * 
     * @param the
     *            component for which constraints should be retrieved
     * @return the <code>SnapLayoutConstraints</code> for the given component;
     *         if none are available an empty constraints object is returned.
     */
    public SnapLayoutConstraints getConstraints(Component comp)
    {
        SnapLayoutConstraints c;
        if (constraints == null || (c = (SnapLayoutConstraints) constraints.get(comp)) == null)
        {
            c = emptyConstraints;
        }
        return c;
    }

    /**
     * Computes and sets up the proper focus traversals for each
     * {@link HNavigable} component contained in the parent
     * <code>Container</code>.
     * 
     * @param parent
     *            the enclosing <code>Container</code>
     */
    protected void setFocusTraversal(Container parent)
    {
        Component[] components = parent.getComponents();

        // Ignore non-navigable, non-visible components
        for (int i = 0; i < components.length; ++i)
        {
            if (!(components[i] instanceof HNavigable) || !components[i].isVisible()
                    || !components[i].isFocusTraversable() || getConstraints(components[i]).nontraversable)
            {
                components[i] = null;
            }
        }

        // We'll use the center point to measure distances
        Point center[] = new Point[components.length];
        for (int i = 0; i < center.length; ++i)
        {
            if (components[i] != null) center[i] = findCenter(components[i].getBounds());
        }

        // Set up focus traversals foreach component
        for (int i = 0; i < components.length; ++i)
        {
            if (components[i] != null) setFocusTraversal(i, components, parent, center);
        }
    }

    /**
     * Calculates and sets focus traversals for the given {@link HNavigable}
     * component.
     * 
     * @param current
     *            the component to set focus traversals for
     * @param components
     *            array of all of the components in the enclosing container,
     *            including <code>current</code>. Note that entries may be
     *            <code>null</code> because the given component is not
     *            <code>HNavigable</code>.
     * @param container
     *            the enclosing <code>Container</code>.
     * @param center
     *            the component center(s)
     */
    protected void setFocusTraversal(int index, Component[] components, Container container, Point[] center)
    {
        HNavigable current = (HNavigable) components[index];
        Dimension area = container.getSize();
        Point point = center[index];
        HNavigable right = null, left = null, up = null, down = null;
        int d_up, d_down, d_left, d_right;
        HNavigable wright = null, wleft = null, wup = null, wdown = null;
        int d_wup, d_wdown, d_wleft, d_wright;

        SnapLayoutConstraints slc = getConstraints((Component) current);
        // Don't bother if keeping all presets
        if (slc.up && slc.down && slc.right && slc.left) return;

        // Start with maximum distances
        d_up = d_down = d_left = d_right = Integer.MAX_VALUE;
        d_wup = d_wdown = d_wleft = d_wright = Integer.MAX_VALUE;

        for (int i = 0; i < components.length; ++i)
        {
            if (components[i] != null && components[i] != current)
            {
                HNavigable x = (HNavigable) components[i];
                Point xPoint = center[i];
                int dist, d, dh, dv;

                // Find actual distance between centers
                d = distCenter(point, xPoint);
                if (wrap)
                {
                    dh = distCenterWrapHorizontal(point, xPoint, area);
                    dv = distCenterWrapVertical(point, xPoint, area);
                }
                else
                {
                    dh = 0;
                    dv = 0;
                }

                /*
                 * Try and find the best for each of the for directions. The
                 * best is the shortest distance. If a shorter direct distance
                 * isn't found, and wrapping is enabled, try to find a shorter
                 * wrapped distance. A "wrapped" traversal isn't selected unless
                 * an appropriate non-wrapped traversal cannot be found.
                 */

                // Find best up
                if (!slc.up)
                {
                    dist = distUp(point, xPoint, d); // weighted dist up
                    if (dist < d_up)
                    {
                        d_up = dist;
                        up = x;
                    }
                    else if (wrap)
                    {
                        // Weighting down works the same as up w/wrap...
                        dist = distDown(point, xPoint, dv); // weighted wrap
                        if (dist < d_wup)
                        {
                            d_wup = dist;
                            wup = x;
                        }
                    }
                }

                // Find best down
                if (!slc.down)
                {
                    dist = distDown(point, xPoint, d); // weighted dist down
                    if (dist < d_down)
                    {
                        d_down = dist;
                        down = x;
                    }
                    else if (wrap)
                    {
                        // Weighting up works the same as down w/wrap...
                        dist = distUp(point, xPoint, dv); // weighted wrap
                        if (dist < d_wdown)
                        {
                            d_wdown = dist;
                            wdown = x;
                        }
                    }
                }

                // Find best right
                if (!slc.right)
                {
                    dist = distRight(point, xPoint, d); // weighted dist right
                    if (dist < d_right)
                    {
                        d_right = dist;
                        right = x;
                    }
                    else if (wrap)
                    {
                        // Weighting left works the same as right w/wrap...
                        dist = distLeft(point, xPoint, dh); // weighted wrap
                        if (dist < d_wright)
                        {
                            d_wright = dist;
                            wright = x;
                        }
                    }
                }

                // Find best left
                if (!slc.left)
                {
                    dist = distLeft(point, xPoint, d); // weighted dist left
                    if (dist < d_left)
                    {
                        d_left = dist;
                        left = x;
                    }
                    else if (wrap)
                    {
                        // Weighting right works the same as left w/wrap...
                        dist = distRight(point, xPoint, dh); // weighted wrap
                        if (dist < d_wleft)
                        {
                            d_wleft = dist;
                            wleft = x;
                        }
                    }
                }

            } // if (components[i] != null && components[i] != current)
        } // for()

        // Figure defaults if there are any
        if (wrap)
        {
            // Choose the wrap-arounds
            if (up == null) up = wup;
            if (down == null) down = wdown;
            if (left == null) left = wleft;
            if (right == null) right = wright;
        }
        else if (container instanceof HNavigable)
        {
            // Inherit from the parent
            if (up == null && !slc.up) up = ((HNavigable) container).getMove(UP);
            if (down == null && !slc.down) down = ((HNavigable) container).getMove(DOWN);
            if (left == null && !slc.left) left = ((HNavigable) container).getMove(LEFT);
            if (right == null && !slc.right) right = ((HNavigable) container).getMove(RIGHT);
        }
        // Keep current if the constraints say so
        if (slc.up) up = current.getMove(UP);
        if (slc.down) down = current.getMove(DOWN);
        if (slc.left) left = current.getMove(LEFT);
        if (slc.right) right = current.getMove(RIGHT);

        if (DEBUG)
        {
            System.out.println("Setting: " + current);
            System.out.println("  UP    " + d_up + ": " + up);
            System.out.println("  DOWN  " + d_down + ": " + down);
            System.out.println("  LEFT  " + d_left + ": " + left);
            System.out.println("  RIGHT " + d_right + ": " + right);
        }

        current.setFocusTraversal(up, down, left, right);
    }

    /**
     * Returns distance up from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distUp(Point current, Point x, int d)
    {
        if (current.y <= x.y) return Integer.MAX_VALUE;

        return weighVertical(current, x, d);
    }

    /**
     * Returns distance down from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distDown(Point current, Point x, int d)
    {
        if (x.y <= current.y) return Integer.MAX_VALUE;

        return weighVertical(current, x, d);
    }

    /**
     * Returns a vertically weighted distance between <code>current</code> and
     * <code>x</code>. Does not assume a direction other than vertical.
     * 
     * @param current
     *            the center of one component
     * @param x
     *            the center of the other component
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measured vertically
     */
    protected int weighVertical(Point current, Point x, int d)
    {
        // Slope is dX/dY
        int dX = Math.abs(current.x - x.x);
        int dY = Math.abs(current.y - x.y);

        return d + (d * dX * straight) / (dY * close);
    }

    /**
     * Returns distance right from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distRight(Point current, Point x, int d)
    {
        if (x.x <= current.x) return Integer.MAX_VALUE;

        return weighHorizontal(current, x, d);
    }

    /**
     * Returns distance left from <code>current</code> to <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measuring in the specified direction.
     */
    protected int distLeft(Point current, Point x, int d)
    {
        if (current.x <= x.x) return Integer.MAX_VALUE;

        return weighHorizontal(current, x, d);
    }

    /**
     * Returns a horizontally weighted distance between <code>current</code> and
     * <code>x</code>. Does not assume a direction other than horizontal.
     * 
     * @param current
     *            the center of one component
     * @param x
     *            the center of the other component
     * @param d
     *            the pre-computed distance between <code>current</code> and
     *            <code>x</code>
     * 
     * @return the weighted distance between <code>current</code> and
     *         <code>x</code> when measured horizontally
     */
    protected int weighHorizontal(Point current, Point x, int d)
    {
        // Slope is dY/dX
        int dX = Math.abs(current.x - x.x);
        int dY = Math.abs(current.y - x.y);

        return d + (d * dY * straight) / (dX * close);
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code>.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code>
     */
    protected int distCenter(Point current, Point x)
    {
        int dX = current.x - x.x;
        int dY = current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code> assuming horizontal wrapping.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the parent container dimensions
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code> assuming horizontal
     *         wrapping
     */
    protected int distCenterWrapHorizontal(Point current, Point x, Dimension d)
    {
        int dX = d.width - current.x - x.x;
        int dY = current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the distance from the center of <code>current</code> to the
     * center of <code>x</code> assuming horizontal wrapping.
     * 
     * @param current
     *            the center of the component we are measuring <i>from</i>
     * @param x
     *            the center of the component we are measuring <i>to</i>
     * @param d
     *            the parent container dimensions
     * @return the straight-line absolute value distance between points
     *         <code>current</code> and <code>x</code> assuming horizontal
     *         wrapping
     */
    protected int distCenterWrapVertical(Point current, Point x, Dimension d)
    {
        int dX = current.x - x.x;
        int dY = d.height - current.y - x.y;

        return (int) Math.sqrt((double) (dX * dX + dY * dY));
    }

    /**
     * Returns the center <code>Point</code> of <code>x</code>.
     * 
     * @param x
     *            the area for which the geometric center should be found
     * @return the <code>Point</code> which specifies the geometric center of
     *         the given area
     */
    protected Point findCenter(Rectangle x)
    {
        return new Point(x.x + x.width / 2, x.y + x.height / 2);
    }

    /**
     * Ratio by which straight vs close should be favored. This ratio is figured
     * in with the weighing of the distance. If the slope should not be
     * considered, straight should be 0. If the slope should be highly
     * considered, straight should be > close.
     * <p>
     * Please don't set close to 0.
     * <p>
     * The default is 1:1.
     */
    protected int straight = 1, close = 1;

    /**
     * Sets the ratio that determines how heavily the slope is taken into
     * account. The slope is multiplied by this ratio. If
     * <code>straight > close</code> then straight line traversals are favored
     * over close traversals. The reverse also holds. The extremes are given
     * when one is 0 and the other non-zero. For example, if
     * <code>close=1</code> and <code>straight=0</code>, then the closest
     * component is always chosen, regardless of the angle.
     * <p>
     * The default is 1:1.
     * 
     * @param straight
     *            the straight portion of the straight:close ratio
     * @param close
     *            the close portion of the straight:close ratio
     */
    public void setSlopeRatio(int straight, int close)
    {
        if (close == straight) close = straight = 1;
        if (close == 0)
        {
            close = 1;
            straight = 1000;
        }
        this.straight = straight;
        this.close = close;
    }

    /**
     * Returns the value of the <i>straight</i> weighting (with respect to the
     * <i>close</i> weighting).
     * 
     * @return straight weighting
     * @see #setSlopeRatio(int,int)
     */
    public int getStraightWeight()
    {
        return straight;
    }

    /**
     * Sets the value of the <i>straight</i> weighting (with respect to the
     * <i>close</i> weighting).
     * 
     * @param straight
     *            straight weighting
     * @see #setSlopeRatio(int,int)
     */
    public void setStraightWeight(int straight)
    {
        setSlopeRatio(straight, close);
    }

    /**
     * Returns the value of the <i>close</i> weighting (with respect to the
     * <i>straight</i> weighting).
     * 
     * @return close weighting
     * @see #setSlopeRatio(int,int)
     */
    public int getCloseWeight()
    {
        return close;
    }

    /**
     * Sets the value of the <i>close</i> weighting (with respect to the
     * <i>straight</i> weighting).
     * 
     * @param straight
     *            close weighting
     * @see #setSlopeRatio(int,int)
     */
    public void setCloseWeight(int close)
    {
        setSlopeRatio(straight, close);
    }

    /**
     * Controls whether this layout will attempt to add wrap-around traversals
     * when no other good traversal exists. I.e., instead of falling back on the
     * parent container traversals, select the component farthest away in the
     * opposite direction.
     */
    private boolean wrap = false;

    /**
     * Controls whether this layout will attempt to add wrap-around traversals
     * when no other good traversal exists. I.e., instead of falling back on the
     * parent container traversals, select the component farthest away in the
     * opposite direction.
     * <p>
     * Note that when wrapping is enabled, traversals are <b>not</b> inherited
     * from the parent container. Also, note that a wrap-around traversal is
     * only selected if no suitable <i>non</i>-wrap-around traversal can be
     * found.
     * 
     * @param wrap
     *            if <code>true</code> then wrap-around traversals are enabled;
     *            <code>false</code> disables wrap-around traversals and is the
     *            default.
     */
    public void setWrap(boolean wrap)
    {
        this.wrap = wrap;
    }

    /**
     * @see #setWrap(boolean)
     */
    public boolean getWrap()
    {
        return wrap;
    }

    /** Convenience constant for <code>KeyEvent.VK_UP</code>. */
    private static final int UP = KeyEvent.VK_UP;

    /** Convenience constant for <code>KeyEvent.VK_DOWN</code>. */
    private static final int DOWN = KeyEvent.VK_DOWN;

    /** Convenience constant for <code>KeyEvent.VK_LEFT</code>. */
    private static final int LEFT = KeyEvent.VK_LEFT;

    /** Convenience constant for <code>KeyEvent.VK_RIGHT</code>. */
    private static final int RIGHT = KeyEvent.VK_RIGHT;

    /**
     * Convenience constant constraints object with all values set to
     * <code>false</code>.
     */
    private static final SnapLayoutConstraints emptyConstraints = new SnapLayoutConstraints();

    /**
     * Mapping of {@link HNavigable}s to {@link SnapLayoutConstraint}s.
     */
    private Hashtable constraints;

    private static final boolean DEBUG = false;
}
