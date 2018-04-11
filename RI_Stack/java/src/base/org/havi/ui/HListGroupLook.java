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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.havi.ImageRender;
import org.cablelabs.impl.havi.SizingHelper;
import org.cablelabs.impl.havi.TextSupport;
import org.cablelabs.impl.havi.SizingHelper.Strategy;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

/**
 * The {@link org.havi.ui.HListGroupLook HListGroupLook} class is used by the
 * {@link org.havi.ui.HListGroup HListGroup} component to display both the
 * {@link org.havi.ui.HListGroup HListGroup} itself (potentially including a
 * scrollbar component) and graphical or textual list content held on the
 * {@link org.havi.ui.HListGroup HListGroup}. This look will be provided by the
 * platform and the exact way in which it is rendered will be platform
 * dependent.
 * 
 * <p>
 * The {@link org.havi.ui.HListGroupLook HListGroupLook} class draws the
 * HListGroup and any look-specific borders around the component, and then
 * renders the content set on the {@link org.havi.ui.HListGroup HListGroup}. It
 * uses the {@link org.havi.ui.HListGroup#getListContent getListContent} method
 * to determine the content to render. The content of the HListGroup does not
 * depend on the interaction state.
 * 
 * <p>
 * {@link org.havi.ui.HListGroupLook HListGroupLook} should use the following
 * properties of {@link org.havi.ui.HListGroup HListGroup} to lay out and render
 * the {@link org.havi.ui.HListElement HListElement} content:
 * 
 * <p>
 * <table border>
 * <tr>
 * <th>Item</th>
 * <th>Method</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td>Orientation</td>
 * <td>{@link org.havi.ui.HListGroup#getOrientation getOrientation}</td>
 * <td>direction to lay out elements</td>
 * </tr>
 * <tr>
 * <td>Content</td>
 * <td>{@link org.havi.ui.HListGroup#getListContent getListContent}</td>
 * <td>elements to display</td>
 * </tr>
 * <tr>
 * <td>Scroll position</td>
 * <td>{@link org.havi.ui.HListGroup#getScrollPosition getScrollPosition}</td>
 * <td>first element to draw</td>
 * </tr>
 * <tr>
 * <td>Selection</td>
 * <td>{@link org.havi.ui.HListGroup#isItemSelected isItemSelected}</td>
 * <td>mark an element as selected</td>
 * </tr>
 * <tr>
 * <td>Current item</td>
 * <td>{@link org.havi.ui.HListGroup#getCurrentItem getCurrentItem}</td>
 * <td>highlight an element</td>
 * </tr>
 * </table>
 * <p>
 * 
 * {@link org.havi.ui.HListGroupLook HListGroupLook} should draw a scrollbar as
 * necessary when there are more {@link org.havi.ui.HListElement HListElements}
 * than can be displayed. It is an implementation option to leave border space
 * between each element. The insets used for the element borders can be
 * retrieved using {@link org.havi.ui.HListGroupLook#getElementInsets
 * getElementInsets}
 * 
 * <p>
 * Implementations of {@link org.havi.ui.HListGroupLook HListGroupLook} should
 * use the appropriate methods on {@link org.havi.ui.HListGroup HListGroup} to
 * determine which scaling and alignment modes to use when rendering content.
 * See the class description for {@link org.havi.ui.HLook HLook} for more
 * details.
 * <p>
 * {@link org.havi.ui.HListGroupLook HListGroupLook} may support scalable
 * graphical content. As a minimum, all implementations must support the
 * {@link org.havi.ui.HVisible#RESIZE_NONE RESIZE_NONE} scaling mode for
 * graphical content, and all alignment modes for text content. However, Note
 * that {@link org.havi.ui.HListGroupLook HListGroupLook} behaves slightly
 * differently from other HAVi {@link org.havi.ui.HLook HLook} classes, as
 * follows.
 * <p>
 * <ul>
 * <li>Where supported, scaling applies to the icon (graphical content) of each
 * {@link org.havi.ui.HListElement HListElement}, based on the area allocated to
 * that {@link org.havi.ui.HListElement HListElement} rather than the entire
 * area of the {@link org.havi.ui.HListGroup HListGroup}.
 * <li>Alignment mode applies to the content of the
 * {@link org.havi.ui.HListElement HListElement} within the area allocated to
 * that {@link org.havi.ui.HListElement HListElement} rather than the entire
 * area of the {@link org.havi.ui.HListGroup HListGroup}.
 * </ul>
 * <p>
 * Note that the results of applying the
 * {@link org.havi.ui.HVisible#VALIGN_JUSTIFY VALIGN_JUSTIFY} and
 * {@link org.havi.ui.HVisible#HALIGN_JUSTIFY HALIGN_JUSTIFY} alignment modes to
 * graphical content are defined to identical to
 * {@link org.havi.ui.HVisible#VALIGN_CENTER VALIGN_CENTER} and
 * {@link org.havi.ui.HVisible#HALIGN_CENTER HALIGN_CENTER} modes respectively,
 * as justification is meaningless in this context.
 * 
 * <p>
 * This is the default look that is used by {@link org.havi.ui.HListGroup
 * HListGroup}.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * 
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td>Element insets</td>
 * <td>null</td>
 * <td>{@link org.havi.ui.HListGroupLook#getElementInsets getElementInsets}</td>
 * <td>---</td>
 * </tr>
 * </table>
 * 
 * @see org.havi.ui.HListGroup
 * @see org.havi.ui.HListElement
 * @see org.havi.ui.HVisible
 * @see org.havi.ui.HLook
 * @see org.havi.ui.HDefaultTextLayoutManager
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HListGroupLook implements HExtendedLook, HAdjustableLook
{
    /**
     * Creates a {@link org.havi.ui.HListGroupLook HListGroupLook} object. See
     * the class description for details of constructor parameters and default
     * values.
     */
    public HListGroupLook()
    {
        // Empty
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#showLook showLook} method is the
     * entry point for repainting the entire {@link org.havi.ui.HVisible}
     * component. This method delegates the responsibility of drawing the
     * component background, borders and any <code>HVisible</code> related data
     * or content to the <code>fillBackground</code>, <code>renderVisible</code>
     * and <code>renderBorders</code> methods respectively, subject to the
     * clipping rectangle of the Graphics object passed to it. This method shall
     * call the methods <code>fillBackground</code>, <code>renderVisible</code>,
     * and <code>renderBorders</code> in that order and shall not do any
     * rendering itself.
     * <p>
     * The {@link org.havi.ui.HExtendedLook#showLook showLook} method should not
     * modify the clipRect (clipping rectangle) of the <code>Graphics</code>
     * object that is passed to it in a way which includes any area not part of
     * that original clipRect. If any modifications are made, the original
     * clipRect shall be restored.
     * <p>
     * Any resources <b>explicitly</b> associated with an
     * {@link org.havi.ui.HExtendedLook} should be loaded by the
     * {@link org.havi.ui.HExtendedLook} during its creation, etc. Note that the
     * &quot;standard&quot; looks don't load content by default.
     * <p>
     * This method is called from the {@link org.havi.ui.HVisible#paint} method
     * of {@link org.havi.ui.HVisible} and must never be called from elsewhere.
     * Components wishing to redraw themselves should call their repaint method
     * in the usual way.
     * 
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible,
     *            allowing the look to render the appropriate content for that
     *            state. Note that some components (e.g. HStaticRange, HRange,
     *            HRangeValue) do not use state-based content.
     */
    public void showLook(java.awt.Graphics g, HVisible visible, int state)
    {
        // Draw the background
        fillBackground(g, visible, state);

        // Render content
        renderVisible(g, visible, state);

        // Draw border decorations
        renderBorders(g, visible, state);

        return;
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#fillBackground} method paints the
     * component with its current background <code>Color</code> according to the
     * {@link org.havi.ui.HVisible#setBackgroundMode} method of
     * {@link org.havi.ui.HVisible}.
     * <p>
     * This method is only called from <code>showLook</code> within this
     * <code>HExtendedLook</code> implementation. It's not the intention to call
     * this method directly from outside of the <code>HExtendedLook</code>.
     * <p>
     * Regardless of the background mode, it is an implementation option for
     * this method to render added decorations which may affect the look's
     * transparency. This method shall not be used to render any HVisible
     * related data or content associated with the HVisible. It is purely for
     * background and decoration only.
     * <p>
     * The <code>fillBackground</code> method should not modify the clipRect
     * (clipping rectangle) of the <code>Graphics</code> object that is passed
     * to it in a way which includes any area not part of that original
     * clipRect. If any modifications are made, the original clipRect shall be
     * restored.
     * <p>
     * 
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible
     * @see HLook#isOpaque
     * @since MHP 1.0.3/1.1.1
     */
    public void fillBackground(java.awt.Graphics g, HVisible visible, int state)
    {
        if (visible.getBackgroundMode() == HVisible.BACKGROUND_FILL)
        {
            Dimension size = visible.getSize();
            Color bg = visible.getBackground();
            g.setColor(bg);
            g.fillRect(0, 0, size.width, size.height);
        }
        return;
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#renderBorders} method paints any
     * implementation specific borders according to the
     * {@link org.havi.ui.HVisible#setBordersEnabled} method of
     * {@link org.havi.ui.HVisible}. If borders are drawn, the border decoration
     * shall be rendered within the border area as returned by
     * <code>getInsets</code>. The behavior of this method, when a subclass
     * overrides the method <code>getInsets</code> is undefined, except that it
     * will never draw outside the border area as returned by
     * <code>getInsets</code>.
     * <p>
     * This method is only called from <code>showLook</code> within this
     * <code>HExtendedLook</code> implementation. It's not the intention to call
     * this method directly from outside of the <code>HExtendedLook</code>.
     * <p>
     * The {@link org.havi.ui.HExtendedLook#renderBorders} method should not
     * modify the clipRect (clipping rectangle) of the <code>Graphics</code>
     * object that is passed to it in a way which includes any area not part of
     * that original clipRect. If any modifications are made, the original
     * clipRect shall be restored.
     * <p>
     * 
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible
     * @since MHP 1.0.3/1.1.1
     */
    public void renderBorders(java.awt.Graphics g, HVisible visible, int state)
    {
        HaviToolkit.getToolkit().drawBorder(g, visible, state, getInsets(visible));
        return;
    }

    /**
     * The {@link org.havi.ui.HExtendedLook#renderVisible} method paints any
     * content or other data associated with the look's HVisible. This method
     * shall not be used to render a background nor any other decoration. Its
     * purpose is purely to render content or other value data stored on the
     * HVisible. This may be set via HVisible methods such as
     * <code>setTextContent</code> and <code>setGraphicContent</code>. Rendering
     * shall take place within the bounds returned by the <code>getInsets</code>
     * method.
     * <p>
     * This method is only called from <code>showLook</code> within this
     * <code>HExtendedLook</code> implementation. It's not the intention to call
     * this method directly from outside of the <code>HExtendedLook</code>.
     * <p>
     * For looks which draw content (e.g. {@link org.havi.ui.HTextLook},
     * {@link org.havi.ui.HGraphicLook} and {@link org.havi.ui.HAnimateLook}),
     * if no content is associated with the component, this method does nothing.
     * <p>
     * The {@link org.havi.ui.HExtendedLook#renderVisible} method should not
     * modify the clipRect (clipping rectangle) of the <code>Graphics</code>
     * object that is passed to it in a way which includes any area not part of
     * that original clipRect. If any modifications are made, the original
     * clipRect shall be restored.
     * <p>
     * The labels of the associated <code>HListElement</code>s shall be rendered
     * by using the current text layout manager of the <code>HListGroup</code>.
     * For each visible label is the <code>render()</code> method of
     * <code>HTextLayoutManager</code> called. The position and size per label
     * are specified as insets relatively to the bounds of
     * <code>HListGroup</code>. Note that the bounds are independent of any
     * borders of the <code>HListGroup</code>, but the insets have to include
     * the borders per element, if any. The look shall use the method
     * <code>getLabelSize()</code> of <code>HListGroup</code> to determine the
     * size for each label. If the returned dimension object has
     * <code>DEFAULT_LABEL_WIDTH</code> for the width and/or
     * <code>DEFAULT_LABEL_HEIGHT</code> for the height as values, then this
     * method shall use implementation specific value(s) as default(s) for the
     * missing dimension(s) instead. If <code>getTextLayoutManager()</code>
     * returns <code>null</code>, then labels shall not be rendered.
     * <p>
     * If supported, scaling of icons shall reflect the resize mode of the
     * visible within the area of the respective list element. The look shall
     * use the method <code>getIconSize()</code> of <code>HListGroup</code> to
     * determine the size for each icon. If the returned dimension object has
     * <code>DEFAULT_ICON_WIDTH</code> for the width and/or
     * <code>DEFAULT_ICON_HEIGHT</code> for the height as values, then this
     * method shall use implementation specific value(s) as default(s) for the
     * missing dimension(s) instead.
     * <p>
     * Except for the alignment of labels and sizes of labels and icons, it is
     * explicitly not defined, how this look arranges icons and labels within
     * the elements' areas. Additionally, it is an implementation option to
     * render labels and icons in other sizes than specified, if the available
     * size per element is smaller or larger than label and icon size. It is
     * also not defined, how the look presents the current item and selected
     * items, or the current selection mode. The elements shall be layed out as
     * specified by <code>getOrientation()</code> of the associated
     * <code>HListGroup</code>.
     * <p>
     * When the associated <code>HListGroup</code> contains more elements than
     * presentable, the look shall make the user aware of that condition, e.g.
     * by displaying an additional scrollbar reflecting the current scroll
     * position. Again, the visible means by which this information is conveyed
     * is not defined and implementation dependent. It is an implementation
     * option for <code>HListGroupLook</code> to draw elements before the scroll
     * position, in order to fill the available space.
     * <p>
     * The behavior of this method, when a subclass overrides the methods
     * <code>getInsets()</code> or <code>getElementInsets()</code>, is not
     * defined. Application developers shall not assume that the corresponding
     * borders will appear as specified.
     * <p>
     * The {@link org.havi.ui.HExtendedLook#renderVisible} method is responsible
     * for painting any implementation specific borders for each HListElement as
     * well as drawing of an additional scrollbar if required.
     * 
     * @param g
     *            the graphics context.
     * @param visible
     *            the visible.
     * @param state
     *            the state parameter indicates the state of the visible
     * @since MHP 1.0.3/1.1.1
     */
    public void renderVisible(java.awt.Graphics g, HVisible visible, int state)
    {
        Dimension size = visible.getSize();

        // Draw the content
        final HListGroup group;
        final HListElement[] elements;
        if (visible instanceof HListGroup && (elements = (group = (HListGroup) visible).getListContent()) != null)
        {
            final Rectangle[] layout = layout(elements, group);
            if (layout != null)
            {
                final HTextLayoutManager tlm = group.getTextLayoutManager();
                final boolean VERT = isVertical(group.getOrientation());
                final int scroll = group.getScrollPosition();
                int adjustX = VERT ? 0 : (layout[0].x - layout[scroll].x);
                int adjustY = VERT ? (layout[0].y - layout[scroll].y) : 0;
                Insets listInsets = getListAreaInsets(group);

                if (!isForward(group.getOrientation()))
                {
                    adjustX = VERT ? 0 : (size.width - layout[scroll].x - layout[scroll].width - listInsets.right);
                    adjustY = VERT ? (size.height - layout[scroll].y - layout[scroll].height - listInsets.bottom) : 0;
                }

                Insets labelInsets = new Insets(0, 0, 0, 0);
                Rectangle visAreaBounds;

                // Calculate the bounds of the area for list element
                // graphics (ie. not frame and not scroll
                // arrows). This will be used to set clipping and to
                // count the number of list elements that are fully
                // visible (ie. not clipped in any way).
                {
                    int visAreaWd = layout[elements.length].width - listInsets.right - listInsets.left;
                    int visAreaHt = layout[elements.length].height - listInsets.top - listInsets.bottom;
                    int visAreaX = listInsets.left;
                    int visAreaY = listInsets.top;

                    visAreaBounds = new Rectangle(visAreaX, visAreaY, visAreaWd, visAreaHt);
                }

                // Prevent drawing outside the visible (ie. non-frame,
                // non-scroll arrow) area
                Rectangle oldClipRect = g.getClipBounds();
                Rectangle intersect = visAreaBounds.intersection(oldClipRect);
                g.setClip(intersect);

                int numVisibleElements = 0;

                // System.out.println("visAreaBounds= " + visAreaBounds);
                // System.out.println("clipBounds= " + oldClipRect);
                // System.out.println("intersect= " + intersect);

                // Render each element
                for (int i = scroll; i < elements.length; ++i)
                {
                    // Make selection visible
                    boolean selected = group.isItemSelected(i);
                    Rectangle elementBounds = new Rectangle(layout[i].x + adjustX, layout[i].y + adjustY,
                            layout[i].width, layout[i].height);

                    // We need this number to determine if the end of
                    // list scroll arrow needs to be drawn.
                    if (visAreaBounds.contains(elementBounds)) numVisibleElements++;

                    if (selected)
                    {
                        // Temporarily swap foreground/background colors.
                        // This is necessary to fool the layout manager.
                        swapColors(group);
                        // Always paint the background.
                        g.setColor(group.getBackground());
                        g.fillRect(layout[i].x + adjustX, layout[i].y + adjustY, layout[i].width, layout[i].height);
                    }

                    // Render the Icon first
                    Image icon = elements[i].getIcon();
                    Rectangle iconBounds = new Rectangle(((Bounds) layout[i]).icon);
                    iconBounds.x += adjustX;
                    iconBounds.y += adjustY;
                    ImageRender.render(g, icon, iconBounds, group);

                    g.setColor(visible.getForeground());

                    // Render the label next
                    Rectangle labelBounds = ((Bounds) layout[i]).label;
                    String label = elements[i].getLabel();
                    if (label != null && (labelBounds.width > 0) && (labelBounds.height > 0))
                    {
                        // Calculate insets for label
                        labelInsets.left = labelBounds.x + adjustX;
                        labelInsets.top = labelBounds.y + adjustY;
                        labelInsets.bottom = size.height - (labelBounds.y + adjustY) - labelBounds.height;
                        labelInsets.right = size.width - (labelBounds.x + adjustX) - labelBounds.width;

                        tlm.render(label, g, group, labelInsets);
                    }

                    // Draw border around element if current
                    if (group.getSelectionMode() && i == group.getCurrentIndex())
                    {
                        g.drawRect(layout[i].x + adjustX, layout[i].y + adjustY, layout[i].width - 1,
                                layout[i].height - 1);
                        g.drawRect(layout[i].x + 1 + adjustX, layout[i].y + 1 + adjustY, layout[i].width - 3,
                                layout[i].height - 3);
                    }

                    // Reset foreground/background colors.
                    // (Should this be in a finally block?)
                    if (selected) swapColors(group);
                }

                // Restore default clipping before drawing arrows (so they don't
                // get clipped)
                g.setClip(oldClipRect);

                // System.out.println("renderVisible: numVisibleElements= " +
                // numVisibleElements + " numElements= " + elements.length +
                // " scroll= " + scroll);
                // System.out.println("renderVisible: numNotVisBeforeBeginning= "
                // + scroll);
                // System.out.println("renderVisible: numNotVisAfterEnd= " +
                // (elements.length - (scroll + numVisibleElements)));

                boolean drawStartOfListArrow = scroll > 0;
                boolean drawEndOfListArrow = elements.length - (scroll + numVisibleElements) > 0;

                if (drawStartOfListArrow || drawEndOfListArrow)
                {
                    Point pos;
                    Polygon arrow;
                    Dimension scrollArrowSize = ScrollArrow.getScrollArrowSize(group);

                    g.setColor(group.getForeground());

                    if (drawStartOfListArrow)
                    {
                        pos = ScrollArrow.getScrollArrowStartOfListPos(group, scrollArrowSize);
                        arrow = ScrollArrow.makeStartOfListArrow(group, pos, scrollArrowSize);
                        g.fillPolygon(arrow);
                    }

                    if (drawEndOfListArrow)
                    {
                        pos = ScrollArrow.getScrollArrowEndOfListPos(group, scrollArrowSize);
                        arrow = ScrollArrow.makeEndOfListArrow(group, pos, scrollArrowSize);
                        g.fillPolygon(arrow);
                    }
                }

                // if (DEBUG)
                // debugOutlines(elements, layout, scroll, g, adjustX, adjustY);
            }
        }

        return;
    }

    /**
     * Swaps the foreground and background colors of the given component.
     * 
     * @param c
     *            component whose foreground and background colors should be
     *            swapped
     */
    private static void swapColors(java.awt.Component c)
    {
        Color tmp = c.getBackground();
        c.setBackground(c.getForeground());
        c.setForeground(tmp);
    }

    /*
     * private static void debugOutlines(HListElement[] elements, Rectangle[]
     * layout, int scroll, java.awt.Graphics g, int adjustX, int adjustY) {
     * for(int i = scroll; i < elements.length; ++i) { Rectangle location =
     * (Bounds)layout[i];
     * 
     * // Outline element g.setColor(Color.black);
     * g.drawRect(location.x+adjustX, location.y+adjustY, location.width-1,
     * location.height-1); g.setColor(Color.white);
     * g.drawRect(location.x+1+adjustX, location.y+1+adjustY, location.width-3,
     * location.height-3);
     * 
     * // Outline icon location = ((Bounds)layout[i]).icon;
     * g.setColor(Color.blue); g.drawRect(location.x+adjustX,
     * location.y+adjustY, location.width-1, location.height-1);
     * g.setColor(Color.white); g.drawRect(location.x+1+adjustX,
     * location.y+1+adjustY, location.width-3, location.height-3);
     * 
     * // Outline label location = ((Bounds)layout[i]).label;
     * g.setColor(Color.red); g.drawRect(location.x+adjustX, location.y+adjustY,
     * location.width-1, location.height-1); g.setColor(Color.white);
     * g.drawRect(location.x+1+adjustX, location.y+1+adjustY, location.width-3,
     * location.height-3); } }
     */

    /**
     * Called by the {@link org.havi.ui.HVisible HVisible} whenever its content,
     * state, or any other data changes. See the class description of
     * {@link org.havi.ui.HVisible HVisible} for more information about the
     * <code>changes</code> parameter.
     * <p>
     * The implementation of this method should work out which graphical areas
     * of the {@link org.havi.ui.HVisible HVisible} have changed and make any
     * relevant calls to trigger the repainting of those areas.
     * <p>
     * A minimum implementation of this method could simply call
     * 
     * <pre>
     * visible.repaint()
     * </pre>
     * 
     * @param visible
     *            the {@link org.havi.ui.HVisible HVisible} which has changed
     * @param changes
     *            an array containing hint data and associated hint objects. If
     *            this argument is <code>null</code> a full repaint will be
     *            triggered.
     */
    public void widgetChanged(HVisible visible, HChangeData[] changes)
    {
        if (false) // Really lame-o implementation
        {
            if (visible.isVisible()) visible.repaint();
        }
        else
        // More complex implementation follows...
        {
            for (int i = 0; i < changes.length; ++i)
            {
                switch (changes[i].hint)
                {
                    case HVisible.ORIENTATION_CHANGE:
                    case HVisible.ITEM_VALUE_CHANGE:
                    case HVisible.LIST_CONTENT_CHANGE:
                    case HVisible.LIST_ICONSIZE_CHANGE:
                    case HVisible.LIST_LABELSIZE_CHANGE:
                        visible.setLookData(this, null);
                        // fall through
                    case HVisible.EDIT_MODE_CHANGE:
                    case HVisible.LIST_MULTISELECTION_CHANGE:
                    case HVisible.LIST_SCROLLPOSITION_CHANGE:
                    case HVisible.UNKNOWN_CHANGE:
                    default:
                        // Should probably get even MORE precise...
                        if (visible.isVisible()) visible.repaint();
                        break;
                }
            }
        }
    }

    /**
     * Returns the size to present one element of the specified
     * <code>HVisible</code> plus any additional dimensions that the
     * <code>HListGroupLook</code> requires for border decoration etc.
     * <p>
     * The extra space required for border decoration can be determined from the
     * <code>getInsets()</code> and <code>getElementInsets()</code> methods. The
     * behavior is not defined for the case, when a subclass overrides these
     * methods. Application developers shall not assume any influence on the
     * returned dimensions.
     * <p>
     * The size per element shall be determined by calls to
     * <code>getIconSize()</code> and <code>getLabelSize()</code> of
     * <code>HListGroup</code>. If any of the dimensions requests a default as
     * specified by <code>DEFAULT_ICON_WIDTH</code>,
     * <code>DEFAULT_ICON_HEIGHT</code>, <code>DEFAULT_LABEL_WIDTH</code> and
     * <code>DEFAULT_LABEL_HEIGHT</code>, then an implementation specific
     * default is used for the corresponding value(s).
     * 
     * @param visible
     *            <code>HVisible</code> to which this <code>HLook</code> is
     *            attached.
     * @return A dimension object indicating this <code>HLook</code>'s minimum
     *         size.
     * @see HListGroup#setIconSize
     * @see HListGroup#setLabelSize
     * @see HVisible#getMinimumSize
     */
    public Dimension getMinimumSize(HVisible visible)
    {
        // For layout:
        // minimum size is the size to present one element or an
        // implementation specific minimum (32 x 32 for example) if no
        // elements are present.

        if (SIZE_STANDARD)
            return SizingHelper.getMinimumSize(visible, this, strategy);
        else
        {
            HListGroup lg = (HListGroup) visible;
            Dimension size;
            if (lg.getNumItems() == 0)
                size = getElementSize(null, lg); // implementation-specific min
            else
            {
                HListElement[] items = lg.getListContent();
                int maxW = 0, maxH = 0;
                for (int i = 0; i < items.length; ++i)
                {
                    Dimension sz = getElementSize(items[i], lg);
                    maxW = Math.max(maxW, sz.width);
                    maxH = Math.max(maxH, sz.height);
                }
                size = new Dimension(maxW, maxH);
            }

            return addInsets(size, getListAreaInsets(lg));
        }
    }

    /**
     * Gets the preferred size of the <code>HVisible</code> component when drawn
     * with this <code>HListGroupLook</code>.
     * <p>
     * If a default size for width and height was set with
     * <code>HVisible.setDefaultSize()</code>, then the dimensions are rounded
     * down to the nearest element (minimum of one) according to the orientation
     * of the associated <code>HListGroup</code>, and any dimensions for border
     * decorations etc. are added.
     * <p>
     * If no default size was set or only for one dimension (i.e. height is
     * <code>NO_DEFAULT_HEIGHT</code> or width is <code>NO_DEFAULT_WIDTH</code>
     * ), then the unset dimension(s) shall be sufficiently large to present
     * five elements according to the <code>HListGroup</code>'s orientation. Any
     * dimensions for border decoration etc. are added.
     * <p>
     * The extra space required for border decoration can be determined from the
     * <code>getInsets()</code> and <code>getElementInsets()</code> methods. The
     * behavior is not defined for the case, when a subclass overrides these
     * methods. Application developers shall not assume any influence on the
     * returned dimensions.
     * <p>
     * The size per element shall be determined by calls to
     * <code>getIconSize()</code> and <code>getLabelSize()</code> of
     * <code>HListGroup</code>. If any of the values requests a default as
     * specified by <code>DEFAULT_ICON_WIDTH</code>,
     * <code>DEFAULT_ICON_HEIGHT</code>, <code>DEFAULT_LABEL_WIDTH</code> and
     * <code>DEFAULT_LABEL_HEIGHT</code>, then an implementation specific
     * default is used for the corresponding value(s).
     * 
     * @param visible
     *            <code>HVisible</code> to which this <code>HLook</code> is
     *            attached.
     * @return A dimension object indicating the preferred size of the
     *         <code>HVisible</code> when drawn with this
     *         <code>HListGroupLook</code>.
     * @see HListGroup#setIconSize
     * @see HListGroup#setLabelSize
     * @see HVisible#getPreferredSize
     * @see HVisible#setDefaultSize
     */
    public Dimension getPreferredSize(HVisible visible)
    {
        // For layout:
        //
        // the preferred size is that set by setDefaultSize rounded
        // down to the nearest element (minimum of one) or the size
        // required to present 5 elements if a default size is not set.

        if (SIZE_STANDARD)
            return SizingHelper.getPreferredSize(visible, this, strategy);
        else
        {
            // Calculate size for one element
            HListGroup lg = (HListGroup) visible;
            Insets listInsets = getListAreaInsets(lg);
            Dimension oneSize = getMinimumSize(lg);
            Dimension size = lg.getDefaultSize();

            int w = oneSize.width - listInsets.left - listInsets.right;
            int h = oneSize.height - listInsets.top - listInsets.bottom;

            if (size == null || size == HVisible.NO_DEFAULT_SIZE)
            {
                if (isVertical(lg.getOrientation()))
                    h *= 5;
                else
                    w *= 5;
            }
            else
            {
                // round down (to a minimum of 1)
                if (!isVertical(lg.getOrientation()) && size.width > w)
                    w = (size.width / w) * w;
                else if (isVertical(lg.getOrientation()) && size.height > h) h = (size.height / h) * h;
            }

            return new Dimension(w + listInsets.left + listInsets.right, h + listInsets.top + listInsets.bottom);
        }
    }

    /**
     * Returns the size to present all elements of the specified
     * <code>HVisible</code> plus any additional dimensions that the
     * <code>HListGroupLook</code> requires for border decoration etc. If no
     * elements are present, a dimension object is returned with width and
     * height set to <code>java.lang.Short.MAX_VALUE</code>.
     * <p>
     * The extra space required for border decoration can be determined from the
     * <code>getInsets()</code> and <code>getElementInsets()</code> methods. The
     * behavior is not defined for the case, when a subclass overrides these
     * methods. Application developers shall not assume any influence on the
     * returned dimensions.
     * <p>
     * The size per element shall be determined by calls to
     * <code>getIconSize()</code> and <code>getLabelSize()</code> of
     * <code>HListGroup</code>. If any of the values requests a default as
     * specified by <code>DEFAULT_ICON_WIDTH</code>,
     * <code>DEFAULT_ICON_HEIGHT</code>, <code>DEFAULT_LABEL_WIDTH</code> and
     * <code>DEFAULT_LABEL_HEIGHT</code>, then an implementation specific
     * default is used for the corresponding value(s).
     * 
     * @param visible
     *            <code>HVisible</code> to which this <code>HLook</code> is
     *            attached.
     * @return A dimension object indicating this <code>HListGroupLook</code>'s
     *         maximum size.
     * @see HListGroup#setIconSize
     * @see HListGroup#setLabelSize
     * @see HVisible#getMaximumSize
     */
    public Dimension getMaximumSize(HVisible visible)
    {
        // For layout:
        //
        // the maximum size is that required to present all elements.

        if (SIZE_STANDARD)
            return SizingHelper.getMaximumSize(visible, this, strategy);
        else
        {
            HListGroup lg = (HListGroup) visible;
            HListElement[] items = lg.getListContent();

            int w = 0, h = 0;
            if (items != null && items.length != 0)
            {
                final boolean VERT = isVertical(lg.getOrientation());
                for (int i = 0; i < items.length; ++i)
                {
                    Dimension sz = getElementSize(items[i], lg);
                    if (VERT)
                    {
                        w = Math.max(w, sz.width);
                        h += sz.height;
                    }
                    else
                    {
                        w += sz.width;
                        h = Math.max(h, sz.height);
                    }
                }
            }

            Insets listInsets = getListAreaInsets(lg);
            return new Dimension(w + listInsets.left + listInsets.right, h + listInsets.top + listInsets.bottom);
        }
    }

    /**
     * Returns true if the entire painted area of the
     * {@link org.havi.ui.HVisible HVisible} when using this look is fully
     * opaque, i.e. the {@link org.havi.ui.HLook#showLook showLook} method
     * guarantees that all pixels are painted in an opaque Color.
     * <p>
     * The default value is implementation specific and depends on the
     * background painting mode of the given {@link org.havi.ui.HVisible
     * HVisible}. The consequences of an invalid overridden value are
     * implementation specific.
     * 
     * @param visible
     *            the visible to test
     * @return true if all the pixels with the java.awt.Component#getBounds
     *         method of an {@link org.havi.ui.HVisible HVisible} using this
     *         look are fully opaque, i.e. the
     *         {@link org.havi.ui.HLook#showLook showLook} method guarantees
     *         that all pixels are painted in an opaque Color, otherwise false.
     */
    public boolean isOpaque(HVisible visible)
    {
        return (visible.getBackgroundMode() != HVisible.NO_BACKGROUND_FILL)
                && HaviToolkit.getToolkit().isOpaque(visible.getBackground());
    }

    /**
     * Determines the insets of this {@link org.havi.ui.HLook HLook}, which
     * indicate the size of the border. This area is reserved for the
     * {@link org.havi.ui.HLook HLook} to use for drawing borders around the
     * associated {@link org.havi.ui.HVisible HVisible}.
     * 
     * @param hvisible
     *            {@link org.havi.ui.HVisible HVisible} to which this
     *            {@link org.havi.ui.HLook HLook} is attached.
     * @return the insets of this {@link org.havi.ui.HLook HLook}.
     */
    public java.awt.Insets getInsets(HVisible hvisible)
    {
        return (Insets) insets.clone();
    }

    /**
     * Returns a value which indicates the pointer click position in the
     * on-screen representation of the orientable component.
     * <p>
     * The behavior of this method in {@link org.havi.ui.HListGroupLook
     * HListGroupLook} differs from the behavior of
     * {@link org.havi.ui.HAdjustableLook#hitTest HAdjustableLook.hitTest()} in
     * that if the position belongs to an {@link org.havi.ui.HListElement
     * HListElement} of the associated {@link org.havi.ui.HListGroup HListGroup}
     * , then this method will return the index of that element. The
     * <code>HListGroup</code> shall interpret this index as current item. If
     * the value is <code>ADJUST_THUMB</code>, then the caller shall use
     * <code>getValue()</code> to retrieve the actual scroll position
     * corresponding to the specified pointer position. The look will not change
     * the appearance of that element (eg. by highlighting it). Such a change
     * may only occur due to a call to {@link #widgetChanged}.
     * <p>
     * Note that it is a valid implementation option to always return
     * {@link org.havi.ui.HAdjustableLook#ADJUST_NONE ADJUST_NONE}.
     * 
     * @param component
     *            - the HOrientable component for which the hit position should
     *            be calculated
     * @param pt
     *            - the pointer click point relative to the upper-left corner of
     *            the specified component.
     * @return one of {@link org.havi.ui.HAdjustableLook#ADJUST_NONE
     *         ADJUST_NONE},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_BUTTON_LESS
     *         ADJUST_BUTTON_LESS},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_PAGE_LESS
     *         ADJUST_PAGE_LESS},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_THUMB ADJUST_THUMB},
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_PAGE_MORE
     *         ADJUST_PAGE_MORE} or
     *         {@link org.havi.ui.HAdjustableLook#ADJUST_BUTTON_MORE
     *         ADJUST_BUTTON_MORE}, or a non-negative element index.
     */
    public int hitTest(HOrientable component, java.awt.Point pt)
    {
        /* Return index of element if one is selected. */
        if (HIT_TEST_INDEX)
        {
            final HListGroup lg;
            final HListElement[] elements;
            Dimension size;

            if (component instanceof HListGroup && (lg = (HListGroup) component).isVisible() && pt.x >= 0 && pt.y >= 0
                    && (size = lg.getSize()).width > pt.x && size.height > pt.y
                    && (elements = lg.getListContent()) != null)
            {
                final Rectangle[] layout = layout(elements, lg);
                if (layout != null)
                {
                    final boolean VERT = isVertical(lg.getOrientation());
                    final int scroll = lg.getScrollPosition();
                    int adjustX = VERT ? 0 : (layout[0].x - layout[scroll].x);
                    int adjustY = VERT ? (layout[0].y - layout[scroll].y) : 0;

                    if (!isForward(lg.getOrientation()))
                    {
                        Insets i = getListAreaInsets(lg);
                        adjustX = VERT ? 0 : (size.width - layout[scroll].x - layout[scroll].width - i.right);
                        adjustY = VERT ? (size.height - layout[scroll].y - layout[scroll].height - i.bottom) : 0;
                    }

                    // Adjust our incoming point accordingly.
                    pt.x -= adjustX;
                    pt.y -= adjustY;

                    // Look through them all
                    for (int i = scroll; i < elements.length; ++i)
                    {
                        if (layout[i].contains(pt)) return i;
                    }
                }
            }
        }
        /* Return constant if scroll area is selected. */
        /* Currently we have no scroll bar. */

        return ADJUST_NONE;
    }

    /**
     * Returns the value of the component which corresponds to the pointer
     * position specified by pt. If the position does not map to a value (eg.
     * the mouse is outside the active area of the component), this method
     * returns <code>null</code>. A non-<code>null</code> value represents the
     * scroll position of the associated <code>HListGroup</code>. The value
     * shall never be less than zero.
     * <p>
     * The look shall not reflect the value returned by this method visibly. If
     * the component uses the returned value, it will inform the look by calling
     * {@link #widgetChanged widgetChanged()}.
     * 
     * @param component
     *            an {@link org.havi.ui.HOrientable HOrientable} implemented by
     *            an {@link org.havi.ui.HVisible HVisible}
     * @param pt
     *            the position of the mouse-cursor relative to the upper-left
     *            corner of the associated component
     * @return the non-negative scroll position associated with the specified
     *         pointer position or <code>null</code>
     * @see #hitTest
     */
    public java.lang.Integer getValue(HOrientable component, java.awt.Point pt)
    {
        // TODO: implement getValue() if mouse/scrollbar support is ever needed
        // The "value" is the scrollPosition (for HListGroup).
        // Note that lacking an implicit scrollBar, this isn't necessary.
        // Plus, this is only really necessary given mouse support, and
        // OCAP does not require mouse support.
        // So, returning null (which is valid) is the logical choice
        return null;
    }

    /**
     * Retrieve the element insets for this instance of
     * {@link org.havi.ui.HListGroupLook HListGroupLook}. The element insets
     * control the amount of empty space left between the elements and the
     * border of the HListGroup component.
     * 
     * @return the element insets, or <code>null</code> if insets are not used
     *         by this implementation of {@link org.havi.ui.HListGroupLook
     *         HListGroupLook}.
     */
    public java.awt.Insets getElementInsets()
    {
        return (Insets) elementInsets.clone();
    }

    /**
     * Retrieve the number of visible elements for the specified component.
     * <p>
     * This method should determine the number of list elements that would be
     * completely visible should the specified component be drawn using this
     * look.
     * 
     * @param visible
     *            the {@link org.havi.ui.HVisible HVisible} to obtain the number
     *            of visible elements for.
     * @return the number of visible elements.
     */
    public int getNumVisible(HVisible visible)
    {
        final HListGroup lg = (HListGroup) visible;
        final HListElement[] elements = lg.getListContent();

        if (elements == null)
            return 0;
        else
        {
            final Rectangle[] layout = layout(elements, lg);
            final Dimension size = lg.getSize();
            final Insets listInsets = getListAreaInsets(lg);
            final boolean VERT = isVertical(lg.getOrientation());
            final int scroll = lg.getScrollPosition();

            int n = 0;

            if (isForward(lg.getOrientation()))
            {
                final int adjustX = VERT ? 0 : (layout[0].x - layout[scroll].x);
                final int adjustY = VERT ? (layout[0].y - layout[scroll].y) : 0;
                final int maxX = size.width - listInsets.right - adjustX;
                final int maxY = size.height - listInsets.bottom - adjustY;

                for (int i = scroll; i < elements.length; ++i)
                {
                    Rectangle r = layout[i];
                    if (r.x + r.width > maxX) // || r.x < minX?
                        break;
                    if (r.y + r.height > maxY) // || r.y < minY?
                        break;

                    ++n;
                }
            }
            else
            {
                final int adjustX = VERT ? 0
                        : (size.width - layout[scroll].x - layout[scroll].width - listInsets.right);
                final int adjustY = VERT ? (size.height - layout[scroll].y - layout[scroll].height - listInsets.bottom)
                        : 0;
                final int minX = listInsets.right - adjustX;
                final int minY = listInsets.top - adjustY;

                for (int i = scroll; i < elements.length; ++i)
                {
                    Rectangle r = layout[i];
                    if (r.x < minX) break;
                    if (r.y < minY) break;

                    ++n;
                }
            }

            return n;
        }
    }

    /**
     * Calculates the <i>layout</i> of the given components. Returns this layout
     * as an array of <code>Rectangle</code> objects where each entry
     * corresponds to the bounds of the element in the <code>items</code> array.
     * 
     * @param items
     *            the array of elements (as returned by
     *            {@link HListGroup#getListContent() lg.getListContent()}
     * @param lg
     *            the associated <code>HListGroup</code>
     * @return the layout of the given <code>items</code>
     * 
     * @throws NullPointerException
     *             if the given <code>items</code> or <code>lg</code> is
     *             <code>null</code>
     */
    private Rectangle[] layout(HListElement[] items, HListGroup lg) throws NullPointerException
    {
        final int numItems = items.length;

        // Check for existing data
        final Dimension size = lg.getSize();
        Object data = lg.getLookData(this);
        if (!RELAYOUT
                && // if true, force relayout regardless of data
                data != null && (data instanceof Rectangle[]) && ((Rectangle[]) data).length == numItems + 1
                && ((Rectangle[]) data)[numItems].getSize().equals(size)) return (Rectangle[]) data;
        // Make sure data is cleared
        lg.setLookData(this, null);

        // Create new layout data
        Rectangle[] layout = null;
        if (numItems > 0)
        {
            layout = new Rectangle[items.length + 1];
            // extra element at end it to check for size changes
            layout[items.length] = lg.getBounds();
            // Can be nulled out by methods we call to keep us from saving
            Reference newData = new Reference(layout);

            final int START; // first element to visit
            final int END; // last element to visit
            final int ADV; // direction to advance to next element

            int orient = lg.getOrientation();
            if (isForward(orient)) // "forward"
            {
                // [0, items.length)
                START = 0;
                END = items.length;
                ADV = 1;
            }
            else
            // "backward"
            {
                // (items.length, -1)
                START = items.length - 1;
                END = -1;
                ADV = -1;
            }

            // These are the inner bounds of the component
            Insets listInsets = getListAreaInsets(lg);
            final int maxW = size.width - listInsets.left - listInsets.right;
            final int maxH = size.height - listInsets.top - listInsets.bottom;

            // We start at this location
            int x = listInsets.right;
            int y = listInsets.top;

            if (isVertical(orient))
            {
                for (int i = START; i != END; i += ADV)
                {
                    layout[i] = layoutElementV(items[i], lg, newData, x, y, maxW, maxH);
                    y += layout[i].height;
                }
            }
            else
            {
                for (int i = START; i != END; i += ADV)
                {
                    layout[i] = layoutElementH(items[i], lg, newData, x, y, maxW, maxH);
                    x += layout[i].width;
                }
            }
            lg.setLookData(this, newData.ref);
        }

        // System.out.println("\n\nBEGIN LAYOUT\n\n");
        // for (int i = 0; i < layout.length; i++)
        // System.out.println(layout[i]);

        // System.out.println("\n\nEND LAYOUT\n\n");

        return layout;
    }

    /**
     * Calculates the size of an <code>HListElement</code> given the calculated
     * (orientation-independent) icon and label sizes. Element insets are not
     * included.
     * 
     * @param iconSize
     *            the calculated size of the icon
     * @param labelSize
     *            the calculated size of the label
     * @return the size of the element
     */
    private static Dimension getElementSizeV(Dimension iconSize, Dimension labelSize)
    {
        // How should these be laid out?
        // For right now, do: Icon left of Label, centering each
        int spacing = (iconSize.width > 0 && labelSize.width > 0) ? V_SPACING : 0;
        return new Dimension(iconSize.width + labelSize.width + spacing, Math.max(iconSize.height, labelSize.height));
    }

    /**
     * Calculates the size of an <code>HListElement</code> given the calculated
     * (orientation-independent) icon and label sizes. Element insets are not
     * included.
     * 
     * @param iconSize
     *            the calculated size of the icon
     * @param labelSize
     *            the calculated size of the label
     * @return the size of the element
     */
    private static Dimension getElementSizeH(Dimension iconSize, Dimension labelSize)
    {
        // How should these be laid out?
        // For right now, do: Icon over Label, centering each
        int spacing = (iconSize.height > 0 && labelSize.height > 0) ? H_SPACING : 0;
        return new Dimension(Math.max(iconSize.width, labelSize.width), iconSize.height + labelSize.height + spacing);
    }

    /**
     * Calculates the bounds of the given <code>HListElement</code> assuming
     * that the list will be oriented vertically. Sizing is based on the
     * element's icon and label, as well as the state of the associated
     * <code>HListGroup</code>.
     * <p>
     * The bounds of the icon and label are also determined and returned as part
     * of the <code>Bounds</code> object.
     * <p>
     * If the <code>ref</code> reference to layout data is non-null, it may have
     * its data set to null to indicate that layout data should not be cached at
     * this time.
     * 
     * @param e
     *            the element to size
     * @param lg
     *            the associated <code>HListGroup</code>
     * @param ref
     *            a reference to the layout data
     * @param x
     *            the assigned x-coordinate location of the element
     * @param y
     *            the assigned y-coordinate location of the element
     * @param maxW
     *            the maximum width of the element
     * @param maxH
     *            the maximum height of the element
     * @return the size of the element
     */
    private static Bounds layoutElementV(HListElement e, HListGroup lg, Reference ref, int x, int y, int maxW, int maxH)
    {
        Dimension iconSize = getIconSize(e.getIcon(), lg, ref);
        Dimension labelSize = getLabelSize(e.getLabel(), lg);

        // How should these be laid out?
        // For right now, do: Icon left of Label, centering each

        // if label && labelSize != default -> fill label
        // else if icon && iconSize != default -> fill icon

        Insets insets = ((HListGroupLook) lg.getLook()).getElementInsets();
        int height = getElementSizeV(iconSize, labelSize).height;

        Bounds bounds = new Bounds(x, y, maxW, height + insets.top + insets.bottom);

        bounds.icon = new Rectangle(insets.left + x, insets.top + y, iconSize.width, height);
        int tmp = bounds.icon.x + iconSize.width + ((iconSize.width > 0) ? V_SPACING : 0);
        bounds.label = new Rectangle(tmp, insets.top + y, maxW - tmp - insets.right + x, height);
        // Note that "x" includes the HListGroup insets.
        // As a result, "icon.x" does as well.
        // We don't want to consider this when calculating the width
        // of the label! Which is why "x" is added back to the width
        // (to subtract it from "tmp").

        return bounds;
    }

    /**
     * Calculates the bounds of the given <code>HListElement</code> assuming
     * that the list will be oriented horizontally. Sizing is based on the
     * element's icon and label, as well as the state of the associated
     * <code>HListGroup</code>.
     * <p>
     * The bounds of the icon and label are also determined and returned as part
     * of the <code>Bounds</code> object.
     * <p>
     * If the <code>ref</code> reference to layout data is non-null, it may have
     * its data set to null to indicate that layout data should not be cached at
     * this time.
     * 
     * @param e
     *            the element to size
     * @param lg
     *            the associated <code>HListGroup</code>
     * @param ref
     *            a reference to the layout data
     * @param x
     *            the assigned x-coordinate location of the element
     * @param y
     *            the assigned y-coordinate location of the element
     * @param maxW
     *            the maximum width of the element
     * @param maxH
     *            the maximum height of the element
     * @return the size of the element
     */
    private static Bounds layoutElementH(HListElement e, HListGroup lg, Reference ref, int x, int y, int maxW, int maxH)
    {
        Dimension iconSize = getIconSize(e.getIcon(), lg, ref);
        Dimension labelSize = getLabelSize(e.getLabel(), lg);

        // How should these be laid out?
        // For right now, do: Icon over Label, centering each

        // if label && labelSize != default -> fill label
        // else if icon && iconSize != default -> fill icon

        Insets insets = ((HListGroupLook) lg.getLook()).getElementInsets();
        int width = getElementSizeH(iconSize, labelSize).width;

        Bounds bounds = new Bounds(x, y, width + insets.left + insets.right, maxH);

        bounds.icon = new Rectangle(insets.left + x, insets.top + y, width, iconSize.height);
        int tmp = bounds.icon.y + iconSize.height + ((iconSize.height > 0) ? H_SPACING : 0);
        bounds.label = new Rectangle(insets.left + x, tmp, width, maxH - tmp - insets.bottom + y);
        // Note that "y" includes the HListGroup insets.
        // As a result, "icon.y" does as well.
        // We don't want to consider this when calculating the height
        // of the label! Which is why "y" is added back to the height
        // (to subtract it from "tmp").

        return bounds;
    }

    /**
     * Determines the size of the icon given the default size specified by the
     * {@link HListGroup#getIconSize() HListGroup}.
     * <p>
     * If the <code>ref</code> reference to layout data is non-null, it may have
     * its data set to null to indicate that layout data should not be cached at
     * this time.
     * 
     * @param icon
     *            the icon to size
     * @param lg
     *            the associated <code>HListGroup</code>
     * @param ref
     *            a reference to the layout data
     * @return the size of the icon based on the requested size; if no default
     *         size is specified, one will be calculated
     */
    private static Dimension getIconSize(Image icon, HListGroup lg, Reference ref)
    {
        // Default icon size should be big enough to show icon
        Dimension iconSize = lg.getIconSize();
        if (iconSize.width == HListGroup.DEFAULT_ICON_WIDTH)
        {
            int tmp;
            if (icon == null)
                iconSize.width = 0;
            else if ((tmp = icon.getWidth(lg)) >= 0)
                iconSize.width = tmp;
            else
            {
                iconSize.width = 0;
                // Null out reference to keep from caching data
                if (ref != null) ref.ref = null;
            }
        }
        if (iconSize.height == HListGroup.DEFAULT_ICON_HEIGHT)
        {
            int tmp;
            if (icon == null)
                iconSize.height = 0;
            else if ((tmp = icon.getHeight(lg)) >= 0)
                iconSize.height = tmp;
            else
            {
                iconSize.height = 0;
                // Null out reference to keep from caching data
                if (ref != null) ref.ref = null;
            }
        }
        return iconSize;
    }

    /**
     * Determines the size of the label given the default size specified by the
     * {@link HListGroup#getLabelSize() HListGroup}.
     * 
     * @param label
     *            the label to size
     * @param lg
     *            the associated <code>HListGroup</code>
     * @return the size of the label based on the requested size if no default
     *         size is specified, one will be calculated
     */
    private static Dimension getLabelSize(String label, HListGroup lg)
    {
        // Default label size should be big enough to show text
        Dimension labelSize = lg.getLabelSize();
        if (labelSize.width == HListGroup.DEFAULT_LABEL_WIDTH || labelSize.height == HListGroup.DEFAULT_LABEL_HEIGHT)
        {
            Font font = lg.getFont();
            if (font == null) font = HaviToolkit.getToolkit().getDefaultFont();
            FontMetrics metrics = lg.getFontMetrics(font);
            String[] text = null;
            if (label != null)
            {
                text = TextSupport.getLines(label);

                if (labelSize.width == HListGroup.DEFAULT_LABEL_WIDTH)
                    labelSize.width = TextSupport.getMaxWidth(text, metrics);
                if (labelSize.height == HListGroup.DEFAULT_LABEL_HEIGHT)
                    labelSize.height = text.length * TextSupport.getFontHeight(metrics);
            }
            else
            {
                if (labelSize.width == HListGroup.DEFAULT_LABEL_WIDTH) labelSize.width = 0;
                if (labelSize.height == HListGroup.DEFAULT_LABEL_HEIGHT) labelSize.height = 0;
            }
        }
        return labelSize;
    }

    /**
     * Returns the element size.
     * 
     * @see #getElementSizeH(Dimension,Dimension)
     * @see #getElementSizeV(Dimension,Dimension)
     */
    private static Dimension getElementSize(HListElement e, HListGroup lg)
    {
        Dimension iconSize = getIconSize(e != null ? e.getIcon() : null, lg, null);
        Dimension labelSize = getLabelSize(e != null ? e.getLabel() : null, lg);
        Dimension size;

        switch (lg.getOrientation())
        {
            case HOrientable.ORIENT_TOP_TO_BOTTOM:
            case HOrientable.ORIENT_BOTTOM_TO_TOP:
                size = getElementSizeV(iconSize, labelSize);
                break;
            case HOrientable.ORIENT_RIGHT_TO_LEFT:
            case HOrientable.ORIENT_LEFT_TO_RIGHT:
                size = getElementSizeH(iconSize, labelSize);
                break;
            default:
                throw new RuntimeException("Internal error");
        }
        size = addInsets(size, ((HListGroupLook) lg.getLook()).getElementInsets());

        return size;
    }

    /**
     * Adds insets to the given <code>Dimension</code>. Note, that the input
     * dimension is adjusted directly.
     * 
     * @param dim
     *            the input dimension
     * @param insets
     *            the insets to added to dimension
     * @return <code>dim</code> with insets added
     */
    private static Dimension addInsets(Dimension dim, Insets insets)
    {
        dim.width += insets.left + insets.right;
        dim.height += insets.top + insets.bottom;
        return dim;
    }

    /**
     * Whether to perform sizing based on the description in HListGroup or
     * provided in HLook/HListGroupLook. If true, then use the standard sizing
     * method.
     */
    private static final boolean SIZE_STANDARD = false;

    /**
     * Implements the <code>SizingHelper.Strategy</code> interface to provide
     * the <code>SizingHelper</code> methods with information about the content
     * to be sized by this look.
     */
    private static final Strategy strategy = !SIZE_STANDARD ? null : ((Strategy) (new Strategy()
    {
        /**
         * Returns whether the given <code>hvisible</code> has any
         * {@link HVisible#getGraphicContent(int)} or not.
         * 
         * @return <code>HListGroup.getNumItems() &gt; 0</code>
         */
        public boolean hasContent(HVisible hvisible)
        {
            return ((HListGroup) hvisible).getNumItems() > 0;
        }

        /**
         * Calculates the largest dimensions of all content.
         * 
         * @param hvisible
         *            the <code>HVisible</code> to query for content
         * @return the largest dimensions of all content.
         */
        public Dimension getMaxContentSize(HVisible hvisible)
        {
            // return total size of content
            HListGroup lg = (HListGroup) hvisible;
            HListElement[] items = lg.getListContent();

            int w = 0, h = 0;
            if (items != null && items.length != 0)
            {
                final boolean VERT = isVertical(lg.getOrientation());
                for (int i = 0; i < items.length; ++i)
                {
                    Dimension sz = getElementSize(items[i], lg);
                    if (VERT)
                    {
                        w = Math.max(w, sz.width);
                        h += sz.height;
                    }
                    else
                    {
                        w += sz.width;
                        h = Math.max(h, sz.height);
                    }
                }
            }

            Insets arrowInsets = ScrollArrow.getArrowInsets(lg);

            h += arrowInsets.top + arrowInsets.bottom;
            w += arrowInsets.left + arrowInsets.right;

            return new Dimension(w, h);
        }

        /**
         * Calculates the smallest dimensions of all content.
         * 
         * @param hvisible
         *            the <code>HVisible</code> to query for content
         * @return the smallest dimensions of all content.
         */
        public Dimension getMinContentSize(HVisible hvisible)
        {
            HListGroup lg = (HListGroup) hvisible;
            Dimension size;
            if (lg.getNumItems() == 0)
                size = getLabelSize("none", lg); // implementation-specific min
            else
            {
                HListElement[] items = lg.getListContent();
                int maxW = 0, maxH = 0;
                for (int i = 0; i < items.length; ++i)
                {
                    Dimension sz = getElementSize(items[i], lg);
                    maxW = Math.max(maxW, sz.width);
                    maxH = Math.max(maxH, sz.height);
                }

                Insets arrowInsets = ScrollArrow.getArrowInsets(lg);

                maxH += arrowInsets.top + arrowInsets.bottom;
                maxW += arrowInsets.left + arrowInsets.right;

                size = new Dimension(maxW, maxH);
            }
            return size;
        }

        /**
         * Returns whether the <code>HLook</code> in question supports sizing of
         * content or not.
         * 
         * @return <code>false</code>
         */
        public boolean supportsScaling()
        {
            return true;
        }
    }));

    /**
     * Returns whether the given <code>orient</code> is "vertical" or
     * "horizontal". Vertical is either top-to-bottom or bottom-to-top;
     * horizontal is either left-to-right or right-to-left.
     * 
     * @return <code>true</code> if <code>orient</code> is considered
     *         <i>vertical</i>; <code>false</code> otherwise
     */
    private static boolean isVertical(int orient)
    {
        return orient >= HOrientable.ORIENT_TOP_TO_BOTTOM;
    }

    /**
     * Returns whether the given <code>orient</code> is "forwards" or
     * "backwards". Forwards is either top-to-bottom or left-to-right; backwards
     * is bottom-to-top or right-to-left.
     * 
     * @return <code>true</code> if <code>orient</code> is considered
     *         <i>forward</i>; <code>false</code> otherwise
     */
    private static boolean isForward(int orient)
    {
        return (orient & 1) == 0;
    }

    /**
     * A reference to a <code>Rectangle[]</code>.
     */
    private static class Reference
    {
        public Rectangle[] ref;

        public Reference(Rectangle[] ref)
        {
            this.ref = ref;
        }
    }

    /**
     * Extends a Rectangle to add inner rectangle references to subdivide the
     * interior of this rectangle. Used to carry the layout of an HListElement
     * with its label and icon.
     */
    private static class Bounds extends Rectangle
    {
        public Rectangle label;

        public Rectangle icon;

        public Bounds()
        { /* empty */
        }

        public Bounds(int x, int y, int width, int height)
        {
            super(x, y, width, height);
        }
    }

    /**
     * Calculate the insets of the list area, relative to the bounds of the list
     * group, and taking into account the orientation of the list.
     * 
     * @param lg
     *            the associated <code>HListGroup</code>
     * @return <code>Insets</code> of the list area
     */
    private Insets getListAreaInsets(HListGroup lg)
    {
        Insets frameInsets = getInsets(lg);
        Insets arrowInsets = ScrollArrow.getArrowInsets(lg);

        int top = frameInsets.top + arrowInsets.top;
        int right = frameInsets.right + arrowInsets.right;
        int bottom = frameInsets.bottom + arrowInsets.bottom;
        int left = frameInsets.left + arrowInsets.left;

        return new Insets(top, left, bottom, right);
    }

    /**
     * All scroll arrow functionality resides in this class.
     */
    private static class ScrollArrow
    {
        private ScrollArrow()
        {
            /* do not instantiate */
        }

        /**
         * Calculate the insets of the scroll arrow areas at the start and end
         * of the list, relative to the bounds of the content area (i.e. the
         * inside of the list group frame), and taking into account the
         * orientation of the list.
         * 
         * @param lg
         *            the associated <code>HListGroup</code>
         * @return <code>Insets</code> of the scroll arrow areas
         */
        private static Insets getArrowInsets(HListGroup lg)
        {
            int top = 0, left = 0, bottom = 0, right = 0;

            if (isVertical(lg.getOrientation()))
                top = bottom = SCROLL_ARROW_LENGTH + SCROLL_ARROW_SPACE;

            else
                right = left = SCROLL_ARROW_LENGTH + SCROLL_ARROW_SPACE;

            // System.out.println("getArrowInsets: top= " + top + " left= " +
            // left + " bottom= " + bottom + " right= " + right);

            return new Insets(top, left, bottom, right);
        }

        /**
         * Calculate the height & width of the the scroll arrow bounding box as
         * determined by the orientation of the associated
         * <code>HListGroup</code>.
         * 
         * @param lg
         *            the associated <code>HListGroup</code>
         * @return <code>Dimension</code> specifying the calculated height and
         *         widht.
         */
        private static Dimension getScrollArrowSize(HListGroup lg)
        {
            int w, h;
            Insets insets = ((HListGroupLook) lg.getLook()).getInsets(lg);

            if (isVertical(lg.getOrientation()))
            {
                int listElemWd = lg.getWidth() - insets.left - insets.right;

                w = Math.min(SCROLL_ARROW_WIDTH, listElemWd);
                h = SCROLL_ARROW_LENGTH;
            }

            else
            {
                int listElemHt = lg.getHeight() - insets.top - insets.bottom;

                w = SCROLL_ARROW_LENGTH;
                h = Math.min(SCROLL_ARROW_WIDTH, listElemHt);
            }

            return new Dimension(w, h);
        }

        private static Point makeTopRightPos(HListGroup lg, Dimension size)
        {
            Insets insets = ((HListGroupLook) lg.getLook()).getInsets(lg);
            int x, y;

            x = insets.right;
            y = insets.top;

            // Center the arrow
            if (isVertical(lg.getOrientation()))
                x += (lg.getWidth() - insets.left - insets.right - size.width) / 2;

            else
                y += (lg.getHeight() - insets.top - insets.bottom - size.height) / 2;

            return new Point(x, y);
        }

        private static Point makeTopLeftOrBottomRightPos(HListGroup lg, Dimension size)
        {
            Insets insets = ((HListGroupLook) lg.getLook()).getInsets(lg);
            int x, y;

            if (isVertical(lg.getOrientation())) // bottom-right
            {
                int listElementWd = lg.getWidth() - insets.left - insets.right;

                x = insets.right + (listElementWd - size.width) / 2;
                y = lg.getHeight() - insets.bottom - SCROLL_ARROW_LENGTH;
            }

            else
            // top-left
            {
                int listElementHt = lg.getHeight() - insets.top - insets.bottom;

                x = lg.getWidth() - insets.left - SCROLL_ARROW_LENGTH;
                y = insets.top + (listElementHt - size.height) / 2;
            }

            return new Point(x, y);
        }

        private static Point getScrollArrowStartOfListPos(HListGroup lg, Dimension size)
        {
            if (isForward(lg.getOrientation()))
                return makeTopRightPos(lg, size);

            else
                return makeTopLeftOrBottomRightPos(lg, size);
        }

        private static Point getScrollArrowEndOfListPos(HListGroup lg, Dimension size)
        {
            if (isForward(lg.getOrientation()))
                return makeTopLeftOrBottomRightPos(lg, size);

            else
                return makeTopRightPos(lg, size);
        }

        /**
         * Adds the vertices for an up pointing triangle contained by the
         * bounding box specified by pos & size to the specified
         * <code>Polygon</code>.
         * 
         * @param poly
         *            The <code>Polygon</code> to hold the triangle verticies.
         *            This function assumes that poly is empty.
         * @param pos
         *            the location of the arrow's bounding box.
         * @param size
         *            the height & width of the arrow's bounding box.
         */
        private static void addUpArrowVertices(Polygon poly, Point pos, Dimension size)
        {
            int w = size.width - 1;
            int h = size.height - 1;

            poly.addPoint(pos.x, pos.y + h);
            poly.addPoint(pos.x + w, pos.y + h);
            poly.addPoint(pos.x + w / 2, pos.y);
        }

        /**
         * Adds the vertices for a down pointing triangle contained by the
         * bounding box specified by pos & size to the specified
         * <code>Polygon</code>.
         * 
         * @param poly
         *            The <code>Polygon</code> to hold the triangle verticies.
         *            This function assumes that poly is empty.
         * @param pos
         *            the location of the arrow's bounding box.
         * @param size
         *            the height & width of the arrow's bounding box.
         */
        private static void addDownArrowVertices(Polygon poly, Point pos, Dimension size)
        {
            int w = size.width - 1;
            int h = size.height - 1;

            poly.addPoint(pos.x, pos.y);
            poly.addPoint(pos.x + w, pos.y);
            poly.addPoint(pos.x + w / 2, pos.y + h);
        }

        /**
         * Adds the vertices for a left pointing triangle contained by the
         * bounding box specified by pos & size to the specified
         * <code>Polygon</code>.
         * 
         * @param poly
         *            The <code>Polygon</code> to hold the triangle verticies.
         *            This function assumes that poly is empty.
         * @param pos
         *            the location of the arrow's bounding box.
         * @param size
         *            the height & width of the arrow's bounding box.
         */
        private static void addLeftArrowVertices(Polygon poly, Point pos, Dimension size)
        {
            int w = size.width - 1;
            int h = size.height - 1;

            poly.addPoint(pos.x + w, pos.y);
            poly.addPoint(pos.x + w, pos.y + h);
            poly.addPoint(pos.x, pos.y + h / 2);
        }

        /**
         * Adds the vertices for a right pointing triangle contained by the
         * bounding box specified by pos & size to the specified
         * <code>Polygon</code>.
         * 
         * @param poly
         *            The <code>Polygon</code> to hold the triangle verticies.
         *            This function assumes that poly is empty.
         * @param pos
         *            the location of the arrow's bounding box.
         * @param size
         *            the height & width of the arrow's bounding box.
         */
        private static void addRightArrowVertices(Polygon poly, Point pos, Dimension size)
        {
            int w = size.width - 1;
            int h = size.height - 1;

            poly.addPoint(pos.x, pos.y);
            poly.addPoint(pos.x, pos.y + h);
            poly.addPoint(pos.x + w, pos.y + h / 2);
        }

        /**
         * Create a triangular <code>Polygon</code> contained in the rectangular
         * area specified by pos and size, and pointing in direction appropriate
         * for the beginning of the list.
         * 
         * @param lg
         *            the associated <code>HListGroup</code>
         * @param pos
         *            the location of the arrow's bounding box.
         * @param size
         *            the height & width of the arrow's bounding box.
         * @return a triangular <code>Polygon</code>
         */
        private static Polygon makeStartOfListArrow(HListGroup lg, Point pos, Dimension size)
        {
            Polygon poly = new Polygon();

            switch (lg.getOrientation())
            {
                case HOrientable.ORIENT_BOTTOM_TO_TOP:
                    addDownArrowVertices(poly, pos, size);
                    break;

                case HOrientable.ORIENT_LEFT_TO_RIGHT:
                    addLeftArrowVertices(poly, pos, size);
                    break;

                case HOrientable.ORIENT_RIGHT_TO_LEFT:
                    addRightArrowVertices(poly, pos, size);
                    break;

                case HOrientable.ORIENT_TOP_TO_BOTTOM:
                    addUpArrowVertices(poly, pos, size);
                    break;
            }

            return poly;
        }

        /**
         * Create a triangular <code>Polygon</code> contained in the rectangular
         * area specified by pos and size, and pointing in direction appropriate
         * for the end of the list.
         * 
         * @param lg
         *            the associated <code>HListGroup</code>
         * @param pos
         *            the location of the arrow's bounding box.
         * @param size
         *            the height & width of the arrow's bounding box.
         * @return a triangular <code>Polygon</code>
         */
        private static Polygon makeEndOfListArrow(HListGroup lg, Point pos, Dimension size)
        {
            Polygon poly = new Polygon();

            switch (lg.getOrientation())
            {
                case HOrientable.ORIENT_BOTTOM_TO_TOP:
                    addUpArrowVertices(poly, pos, size);
                    break;

                case HOrientable.ORIENT_LEFT_TO_RIGHT:
                    addRightArrowVertices(poly, pos, size);
                    break;

                case HOrientable.ORIENT_RIGHT_TO_LEFT:
                    addLeftArrowVertices(poly, pos, size);
                    break;

                case HOrientable.ORIENT_TOP_TO_BOTTOM:
                    addDownArrowVertices(poly, pos, size);
                    break;
            }

            return poly;
        }

        /**
         * The orientation independent width of a scroll arrow.
         */
        private static final int SCROLL_ARROW_WIDTH = 26;

        /**
         * The orientation independent length of a scroll arrow.
         */
        private static final int SCROLL_ARROW_LENGTH = 10;

        /**
         * The space between a scroll arrow and the adjoining list element.
         */
        private static final int SCROLL_ARROW_SPACE = 3;
    }

    /**
     * The vertical spacing between an icon and label.
     */
    private static final int V_SPACING = 4;

    /**
     * The horizontal spacing between an icon and label.
     */
    private static final int H_SPACING = 4;

    /**
     * The insets associated with this look.
     */
    private Insets insets = HaviToolkit.getToolkit().getDefaultHLookInsets();

    /**
     * The insets associated with each element.
     */
    private Insets elementInsets = HaviToolkit.getToolkit().getDefaultHListGroupLookElementInsets();

    /** A debugging flag that forces us to recalculate the layout. */
    private static final boolean RELAYOUT = false;

    /** Implement hitTest() support for returning index values. */
    private static final boolean HIT_TEST_INDEX = true;
}
