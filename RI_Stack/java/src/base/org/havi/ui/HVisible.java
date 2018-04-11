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

import java.awt.Dimension;
import java.awt.Image;
import java.util.Hashtable;

/**
 * The {@link org.havi.ui.HVisible} class is the base class for all
 * non-interactive components.
 *
 * <p>
 * If a layout manager is associated with the Container into which a
 * {@link org.havi.ui.HVisible} component is placed, the size and location of
 * the component will be controlled by the layout manager.
 *
 * <p>
 * {@link org.havi.ui.HVisible} provides the following features for the use of
 * subclasses:
 *
 * <ul>
 * <li>support for interaction states.
 * <li>a mechanism to associate the component with a pluggable
 * {@link org.havi.ui.HLook} class to which all drawing is delegated.
 * <li>support for state-related content which is drawn by the associated
 * {@link org.havi.ui.HLook}.
 * <li>support for scalable and alignable content.
 * <li>support for private look data.
 * <li>support for preferred sizes when used with a layout manager.
 * <li>control over the background painting behavior.
 * <li>a pluggable text layout management mechanism.
 * </ul>
 *
 * Some of these features are discussed in more detail below.
 *
 * <h3>Interaction State</h3>
 *
 * Interaction states for {@link org.havi.ui.HVisible} components are defined in
 * the {@link org.havi.ui.HState} interface. The only interaction states valid
 * for a plain (un-subclassed) {@link org.havi.ui.HVisible} are:
 * <p>
 * <ul>
 * <li>{@link org.havi.ui.HState#NORMAL_STATE} - indicating that the component
 * is in its normal interaction state.
 * <li>{@link org.havi.ui.HState#DISABLED_STATE} - indicating that the component
 * is disabled. While this has no effect on user interaction with a plain
 * {@link org.havi.ui.HVisible HVisible} (since there is no user interaction) it
 * will affect the visual appearance.
 * </ul>
 * <p>
 * Subclasses of {@link org.havi.ui.HVisible} may use other states. However,
 * {@link org.havi.ui.HVisible#setInteractionState} is the only means by which
 * state can be changed and will throw an IllegalArgumentException if the state
 * is not valid for a particular component type. Subclasses of
 * {@link org.havi.ui.HVisible} must not manipulate state in other ways.
 * <p>
 * The {@link org.havi.ui.HVisible#getInteractionState getInteractionState}
 * method is provided to allow any class to read the current state of a
 * component.
 *
 * <h3>State-based content</h3>
 *
 * Specific content may be set for any interaction state defined in
 * {@link org.havi.ui.HState}, irrespective of whether the subclass of
 * {@link org.havi.ui.HVisible} can ever be in that state.
 * <p>
 *
 * Note that content is set on the various <code>STATE</code> constants defined
 * in {@link org.havi.ui.HState}, and <em>not</em> on the <code>STATE_BIT</code>
 * constants. A <code>java.lang.IllegalArgumentException</code> will be thrown
 * by any method which takes a state as a parameter if a <code>STATE_BIT</code>
 * rather than a <code>STATE</code> is passed.
 *
 * <p>
 *
 * By default it is possible to set content for any of eight discrete states.
 * However, if no content has been set for a particular state, the associated
 * {@link org.havi.ui.HLook} should use content from the nearest matching state
 * as defined below:
 *
 * <p>
 * <table border>
 *
 * <tr>
 * <th>State</th>
 * <th>Content if missing</th>
 * <th>Example style</th>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#NORMAL_STATE}</td>
 * <td>none</td>
 * <td>no special style</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#FOCUSED_STATE}</td>
 * <td>{@link org.havi.ui.HState#NORMAL_STATE}</td>
 * <td>highlighted</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#ACTIONED_STATE}</td>
 * <td>{@link org.havi.ui.HState#FOCUSED_STATE}</td>
 * <td>pushed in</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#ACTIONED_FOCUSED_STATE}</td>
 * <td>{@link org.havi.ui.HState#FOCUSED_STATE}</td>
 * <td>highlighted & pushed in</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#DISABLED_STATE}</td>
 * <td>{@link org.havi.ui.HState#NORMAL_STATE}</td>
 * <td>grayed out</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#DISABLED_FOCUSED_STATE}</td>
 * <td>{@link org.havi.ui.HState#DISABLED_STATE}</td>
 * <td>grayed out & highlighted</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#DISABLED_ACTIONED_STATE}</td>
 * <td>{@link org.havi.ui.HState#ACTIONED_STATE}</td>
 * <td>grayed out & pushed in</td>
 * </tr>
 * <tr>
 * <td>{@link org.havi.ui.HState#DISABLED_ACTIONED_FOCUSED_STATE}</td>
 * <td>{@link org.havi.ui.HState#DISABLED_STATE}</td>
 * <td>grayed out & highlighted & pushed</td>
 * </tr>
 * </table>
 *
 * <p>
 * It is recommended that HLook implementations be capable of drawing components
 * in styles that allow the interaction state to be visually distinguished even
 * when no content is set, or the content is the same for several states.
 *
 * <p>
 * Content for any component may be changed &quot;on the fly&quot; using the
 * setContent methods. However, some components (e.g.
 * {@link org.havi.ui.HStaticAnimation} and {@link org.havi.ui.HAnimation}) may
 * be asynchronously referencing their content (i.e. through a separate
 * implementation-specific animation thread). Therefore the following
 * restrictions apply to the {@link org.havi.ui.HVisible#setAnimateContent}
 * method when the instance of {@link org.havi.ui.HVisible} on which it is
 * invoked implements the {@link org.havi.ui.HAnimateEffect} interface:
 *
 * <p>
 * <ul>
 * <li>The method must be synchronized with any implementation-specific
 * animation thread such that content cannot be changed while a different thread
 * is using it.
 * <li>If the animation was running the method should stop the animation in a
 * synchronized manner before changing content.
 * <li>The method should reset the animation to a starting position defined by
 * the current play mode. The repeat count of the animation should be reset to
 * 0.
 * <li>If the animation was running the method should start the animation.
 * </ul>
 *
 * Alternative platform-specific animation schemes which do not use
 * {@link org.havi.ui.HAnimateEffect} are outside the scope of this document.
 * However, a similar strategy should be employed to avoid synchronization
 * problems.
 *
 * <h3>Content Scaling and Alignment</h3>
 *
 * Where state-based content is used, it is an implementation option to support
 * scaling of the content to fit the HVisible. Some looks also support alignment
 * of content. See the class description of {@link org.havi.ui.HLook} for
 * details of which looks support scaling and alignment. See the fields
 * description of this class for constants which can be used to specify scaling
 * and alignment modes.
 *
 * <p>
 * The following methods are used to get and set alignment of content:
 *
 * <ul>
 * <li> {@link org.havi.ui.HVisible#setHorizontalAlignment}
 * <li> {@link org.havi.ui.HVisible#setVerticalAlignment}
 * <li> {@link org.havi.ui.HVisible#getHorizontalAlignment}
 * <li> {@link org.havi.ui.HVisible#getVerticalAlignment}
 * </ul>
 *
 * The following methods are used to get and set the scaling of content. Scaling
 * support is optional, however all implementations must support the
 * {@link org.havi.ui.HVisible#RESIZE_NONE} scaling mode. Platforms are
 * <em>not</em> required to support scaling of textual content by default.
 *
 * <ul>
 * <li> {@link org.havi.ui.HVisible#setResizeMode}
 * <li> {@link org.havi.ui.HVisible#getResizeMode}
 * </ul>
 *
 * <h3>Repaint Hints</h3>
 *
 * An associated HLook provides a mechanism of efficiently repainting an
 * {@link org.havi.ui.HVisible HVisible}, based on a hint which provides data
 * about the change which triggered the redrawing. The
 * {@link org.havi.ui.HLook#widgetChanged} method should be called in preference
 * to <code>repaint</code> whenever the HVisible requires a redraw to change its
 * appearance.
 *
 * <p>
 *
 * The {@link org.havi.ui.HLook#widgetChanged} method requires an array of one
 * or more {@link org.havi.ui.HChangeData} objects, which contain a hint, and
 * associated data. All keys have the type <code>int</code>, with values defined
 * as constants in this class. The following hints are defined:
 *
 * <p>
 * <table>
 * <border>
 * <tr>
 * <th>Hint (key)</th>
 * <th>Data (value)</th>
 * <th>Data Interpretation</th>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#TEXT_CONTENT_CHANGE}</td>
 * <td>Object[9]</td>
 * <td>[Integer changedState, String oldNORMAL_STATEtext, String
 * oldFOCUSED_STATEtext, String oldACTIONED_STATEtext, String
 * oldACTIONED_FOCUSED_STATEtext, String oldDISABLED_STATEtext, String
 * oldDISABLED_FOCUSED_STATEtext, String oldDISABLED_ACTIONED_STATEtext, String
 * oldDISABLED_ACTIONED_FOCUSED_STATEtext]</td>
 * </tr>
 *
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#GRAPHIC_CONTENT_CHANGE}</td>
 * <td>Object[9]</td>
 * <td>[Integer changedState, Image oldNORMAL_STATEimage, Image
 * oldFOCUSED_STATEimage, Image oldACTIONED_STATEimage, Image
 * oldACTIONED_FOCUSED_STATEimage, Image oldDISABLED_STATEimage, Image
 * oldDISABLED_FOCUSED_STATEimage, Image oldDISABLED_ACTIONED_STATEimage, Image
 * oldDISABLED_ACTIONED_FOCUSED_STATEimage]</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#ANIMATE_CONTENT_CHANGE}</td>
 * <td>Object[9]</td>
 * <td>[Integer changedState, Image[] oldNORMAL_STATEanimation, Image[]
 * oldFOCUSED_STATEanimation, Image[] oldACTIONED_STATEanimation, Image[]
 * oldACTIONED_FOCUSED_STATEanimation, Image[] oldDISABLED_STATEanimation,
 * Image[] oldDISABLED_FOCUSED_STATEanimation, Image[]
 * oldDISABLED_ACTIONED_STATEanimation, Image[]
 * oldDISABLED_ACTIONED_FOCUSED_STATEanimation]</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#CONTENT_CHANGE}</td>
 * <td>Object[9]</td>
 * <td>[Integer changedState, Object oldNORMAL_STATEcontent, Object
 * oldFOCUSED_STATEcontent, Object oldACTIONED_STATEcontent, Object
 * oldACTIONED_FOCUSED_STATEcontent, Object oldDISABLED_STATEcontent, Object
 * oldDISABLED_FOCUSED_STATEcontent, Object oldDISABLED_ACTIONED_STATEcontent,
 * Object oldDISABLED_ACTIONED_FOCUSED_STATEcontent]</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#STATE_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldState</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#CARET_POSITION_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldPosition</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#ECHO_CHAR_CHANGE}</td>
 * <td>Character</td>
 * <td>oldEcho</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#EDIT_MODE_CHANGE}</td>
 * <td>Boolean</td>
 * <td>oldEditMode</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#MIN_MAX_CHANGE}</td>
 * <td>Integer[2]</td>
 * <td>[oldMin, oldMax]</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#THUMB_OFFSETS_CHANGE}</td>
 * <td>Integer[2]</td>
 * <td>[oldMin, oldMax]</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#ORIENTATION_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldOrientation</td>
 * </tr>
 *
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#ITEM_VALUE_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldValue</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#ADJUSTMENT_VALUE_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldIndex</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#LIST_CONTENT_CHANGE}</td>
 * <td>HListElement[]</td>
 * <td>oldContent</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#LIST_ICONSIZE_CHANGE}</td>
 * <td>Dimension</td>
 * <td>oldSize</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#LIST_LABELSIZE_CHANGE}</td>
 * <td>Dimension</td>
 * <td>oldSize</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#LIST_MULTISELECTION_CHANGE}</td>
 * <td>Boolean</td>
 * <td>oldSelection</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#LIST_SCROLLPOSITION_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldScrollPosition</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#SIZE_CHANGE }</td>
 * <td>Integer</td>
 * <td>oldSize</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#BORDER_CHANGE}</td>
 * <td>Boolean</td>
 * <td>oldBorderMode</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#REPEAT_COUNT_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldRepeatCount</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#ANIMATION_POSITION_CHANGE}</td>
 * <td>Integer</td>
 * <td>oldValue</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#LIST_SELECTION_CHANGE}</td>
 * <td>HListElement[]</td>
 * <td>oldSelectedElements</td>
 * </tr>
 *
 * <tr>
 * <td>{@link org.havi.ui.HVisible#UNKNOWN_CHANGE}</td>
 * <td>Integer</td>
 * <td>UNKNOWN_CHANGE</td>
 * </tr>
 *
 *
 * </table>
 * <p>
 * Note that implementations of {@link org.havi.ui.HLook} may not actually
 * implement more efficient drawing code for a given hint. In particular, simply
 * repainting the entire {@link org.havi.ui.HVisible} is a valid implementation
 * option.
 *
 * <p>
 * The following code for the {@link org.havi.ui.HStaticRange#setRange} method
 * demonstrates how hint objects are used. Note that the values passed in the
 * hint are always the <em>old</em> values, since the {@link org.havi.ui.HLook}
 * can always retrieve the new values as needed from the
 * {@link org.havi.ui.HVisible}. Variables starting with &quot;my&quot; are
 * member variables of the class.
 *
 * <p>
 *
 * <pre>
 * public boolean setRange(int min, int max)
 * {
 *     if (min >= max) return false;
 *
 *     if (min != myMin || max != myMax)
 *     {
 *         // create hint object with the OLD values
 *         HChangeData oldRangeData = new HChangeData(HVisible.MIN_MAX_CHANGE, new Integer[] { new Integer(myMin),
 *                 new Integer(myMax) });
 *
 *         // update the values
 *         myMin = min;
 *         myMax = max;
 *
 *         Integer oldValue = new Integer(myValue);
 *
 *         // behavior checking elided
 *         if (myValue < myMin)
 *             myValue = myMin;
 *         else if (myValue > myMax)
 *             myValue = myMax;
 *         else
 *             oldValue = null; // myValue is within new range
 *
 *         HChangeData[] array;
 *         if (oldValue == null)
 *             array = new HChangeData[] { oldRangeData };
 *         else
 *             array = new HChangeDate[] { oldRangeData, new HChangeData(HVisible.ADJUSTMENT_VALUE_CHANGE, oldValue) };
 *
 *         // tell the look to repaint as needed
 *         if (myLook != null) myLook.widgetChanged(this, array);
 *     }
 *     return true;
 * }
 * </pre>
 *
 * <h3>Private HLook data</h3>
 *
 * Private data for the use of an associated {@link org.havi.ui.HLook} to
 * optimize the redraw of the component may be set on any
 * {@link org.havi.ui.HVisible}. Examples of such data are:
 *
 * <ul>
 * <li>cached bitmap representations of complex drawing operations
 * <li>scrolling metrics and positions
 * <li>cached layout data
 * </ul>
 *
 * Data is set using the {@link org.havi.ui.HVisible#setLookData} method, and
 * retrieved with the {@link org.havi.ui.HVisible#getLookData} method.
 *
 * <p>
 * To keep the on-screen representation of an {@link org.havi.ui.HVisible}
 * synchronized with its state, any class may call
 * {@link org.havi.ui.HVisible#setLookData} with a parameter of
 * <code>null</code> to invalidate any cached data. Instances of
 * {@link org.havi.ui.HLook} must be able to regenerate any data they store on
 * {@link org.havi.ui.HVisible} classes at any time. This mechanism is intended
 * for implementation optimization only and shall not be used for storing
 * content.
 *
 * <p>
 * Use of this mechanism is an implementation option. If this mechanism is not
 * used by an implementation, {@link org.havi.ui.HVisible#getLookData} shall
 * always return <code>null</code> and {@link org.havi.ui.HVisible#setLookData}
 * shall do nothing. Interoperable systems shall not assume that this mechanism
 * is implemented.
 *
 * <h3>Preferred Sizes</h3>
 *
 * The {@link org.havi.ui.HVisible#setDefaultSize} and
 * {@link org.havi.ui.HVisible#getDefaultSize} methods provide support for a
 * user-defined default preferred size to be passed to a layout manager. This
 * value is returned to the layout manager through the
 * {@link org.havi.ui.HLook#getPreferredSize} method and if set overrides any
 * look-specific value based on content calculated by the
 * {@link org.havi.ui.HLook}.
 *
 * <p>
 * Note that the interpretation of the size passed to
 * {@link org.havi.ui.HVisible#setDefaultSize} is <em>not</em> the overall size
 * of the component, but the area available to the look to render the component
 * into excluding any look-specific borders. Therefore when a layout manager is
 * in use the <em>actual</em> size of the component is likely to be larger than
 * this default size. See the descriptions of
 * {@link org.havi.ui.HLook#getMinimumSize},
 * {@link org.havi.ui.HLook#getPreferredSize} and
 * {@link org.havi.ui.HLook#getMaximumSize} in {@link org.havi.ui.HLook} for
 * details of the exact algorithm used.
 *
 * <p>
 * Note that constructors of {@link org.havi.ui.HVisible} and subclasses which
 * accept <code>width</code> and <code>height</code> parameters shall set the
 * default size to <code>[width,
  height]</code> as if these parameters were passed to
 * {@link org.havi.ui.HVisible#setDefaultSize}. If a layout manager is in use
 * the actual size of the component will probably be larger than this size after
 * layout due to the {@link org.havi.ui.HLook} adding borders. However, if no
 * layout manager is used the <code>width</code> and <code>height</code>
 * parameters are simply used to set the <em>actual</em> size of the component.
 *
 * <h3>Background Painting</h3>
 *
 * Normally the associated {@link org.havi.ui.HLook} does not paint the
 * background of the {@link org.havi.ui.HVisible}, allowing for non-rectangular
 * components and text overlaying bitmaps. However, {@link org.havi.ui.HVisible}
 * provides for components which require their background to be painted through
 * the setBackgroundMode method. Note that if the mode is set to
 * {@link org.havi.ui.HVisible#BACKGROUND_FILL} the return value of the
 * {@link org.havi.ui.HComponent#isOpaque} method <em>may</em> be true,
 * depending on whether the current background color of the
 * {@link org.havi.ui.HVisible} is opaque. If the background mode is set to
 * {@link org.havi.ui.HVisible#NO_BACKGROUND_FILL} the
 * {@link org.havi.ui.HComponent#isOpaque} method <em>must</em> return false.
 *
 * <h3>Event Handling</h3>
 *
 * While implementations of {@link org.havi.ui.HVisible} may enable certain
 * java.awt.AWTEvents, applications should assume that an
 * {@link org.havi.ui.HVisible} class does not generate or respond to any
 * java.awt.AWTEvents. If this behavior is required the standard AWT mechanisms
 * (i.e. processEvent and similar functions) may be used to handle events on
 * {@link org.havi.ui.HVisible} or subclasses. However, it is strongly
 * recommended that component implementors use the HAVi events defined in the
 * <code>org.havi.ui.event</code> package where possible.
 *
 * <p>
 * For example, a component wishing to respond to user action should normally
 * subclass {@link org.havi.ui.HComponent}, implement the
 * {@link org.havi.ui.HActionInputPreferred} interface and handle
 * {@link org.havi.ui.event.HActionEvent} events in the
 * {@link org.havi.ui.HActionInputPreferred#processHActionEvent} method, instead
 * of overriding <code>java.awt.Component#processEvent</code> or similar.
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
 * <td>x</td>
 * <td>x-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>y</td>
 * <td>y-coordinate of top left hand corner of this component in pixels,
 * relative to its parent container (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>width</td>
 * <td>width of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 * <tr>
 * <td>height</td>
 * <td>height of this component in pixels (subject to layout management).</td>
 * <td>---</td>
 * <td>java.awt.Component#setBounds</td>
 * <td>java.awt.Component#getBounds</td>
 * </tr>
 *
 *
 *
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
 * <td>Associated matte ({@link org.havi.ui.HMatte HMatte}).</td>
 * <td>none (i.e. getMatte() returns <code>null</code>)</td>
 * <td>{@link org.havi.ui.HComponent#setMatte setMatte}</td>
 * <td>{@link org.havi.ui.HComponent#getMatte getMatte}</td>
 * </tr>
 * <tr>
 * <td>The text layout manager responsible for text formatting.</td>
 * <td>An {@link org.havi.ui.HDefaultTextLayoutManager} object.</td>
 * <td> {@link org.havi.ui.HVisible#setTextLayoutManager}</td>
 * <td> {@link org.havi.ui.HVisible#getTextLayoutManager}</td>
 * </tr>
 *
 * <tr>
 * <td>The background painting mode</td>
 * <td>{@link org.havi.ui.HVisible#NO_BACKGROUND_FILL}</td>
 *
 * <td>{@link org.havi.ui.HVisible#setBackgroundMode}</td>
 * <td>{@link org.havi.ui.HVisible#getBackgroundMode}</td>
 * </tr>
 *
 * <tr>
 * <td>The default preferred size</td>
 * <td>not set (i.e. NO_DEFAULT_SIZE) unless specified by <code>width</code> and
 * <code>height</code> parameters</td>
 * <td>{@link org.havi.ui.HVisible#setDefaultSize}</td>
 * <td>{@link org.havi.ui.HVisible#getDefaultSize}</td>
 * </tr>
 *
 * <tr>
 * <td>The horizontal content alignment</td>
 * <td>{@link org.havi.ui.HVisible#HALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setHorizontalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getHorizontalAlignment}</td>
 * </tr>
 *
 * <tr>
 * <td>The vertical content alignment</td>
 * <td>{@link org.havi.ui.HVisible#VALIGN_CENTER}</td>
 * <td>{@link org.havi.ui.HVisible#setVerticalAlignment}</td>
 * <td>{@link org.havi.ui.HVisible#getVerticalAlignment}</td>
 * </tr>
 *
 * <tr>
 * <td>The content scaling mode</td>
 * <td>{@link org.havi.ui.HVisible#RESIZE_NONE}</td>
 * <td>{@link org.havi.ui.HVisible#setResizeMode}</td>
 * <td>{@link org.havi.ui.HVisible#getResizeMode}</td>
 * </tr>
 *
 * <tr>
 * <td>The border mode</td>
 * <td><code>true</code></td>
 * <td>{@link org.havi.ui.HVisible#setBordersEnabled}</td>
 * <td>{@link org.havi.ui.HVisible#getBordersEnabled}</td>
 * </tr>
 *
 *
 *
 *
 * </table>
 *
 * @author Tom Henriksen
 * @author Aaron Kamienski
 * @version 1.1
 */

public class HVisible extends HComponent implements HState
{
    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setHorizontalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be left
     * aligned.
     */
    public static final int HALIGN_LEFT = 0;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setHorizontalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be
     * centered horizontally.
     */
    public static final int HALIGN_CENTER = 1;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setHorizontalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be right
     * aligned.
     */
    public static final int HALIGN_RIGHT = 2;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setHorizontalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be fully
     * justified (horizontally).
     */
    public static final int HALIGN_JUSTIFY = 3;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setVerticalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be
     * vertically aligned to the top of the component.
     */
    public static final int VALIGN_TOP = 0;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setVerticalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be
     * centered vertically.
     */
    public static final int VALIGN_CENTER = 4;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setVerticalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be
     * vertically aligned to the bottom of the component.
     */
    public static final int VALIGN_BOTTOM = 8;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setVerticalAlignment} method of
     * {@link org.havi.ui.HVisible} which indicates that content should be fully
     * justified (vertically).
     */
    public static final int VALIGN_JUSTIFY = 12;

    /**
     * A constant for use with the {@link org.havi.ui.HVisible#setResizeMode}
     * method of {@link org.havi.ui.HVisible} which indicates that content
     * should not be scaled to fit the component.
     */
    public final static int RESIZE_NONE = 0;

    /**
     * A constant for use with the {@link org.havi.ui.HVisible#setResizeMode}
     * method of {@link org.havi.ui.HVisible} which indicates that content
     * should be scaled to fit the component while preserving the aspect ratio
     * of the content. Areas of the component that are not filled by the content
     * will be look dependent.
     */
    public static final int RESIZE_PRESERVE_ASPECT = 1;

    /**
     * A constant for use with the {@link org.havi.ui.HVisible#setResizeMode}
     * method of {@link org.havi.ui.HVisible} which indicates that content
     * should be scaled to fit the component. Aspect ratios of the content need
     * not be preserved.
     */
    public static final int RESIZE_ARBITRARY = 2;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setBackgroundMode} method of
     * {@link org.havi.ui.HVisible} which indicates that an associated
     * {@link org.havi.ui.HLook} should not fill the bounding rectangle of the
     * {@link org.havi.ui.HVisible} with its current background color before
     * drawing any content. Therefore any previous content will NOT necessarily
     * be erased during the repainting of the {@link org.havi.ui.HVisible}.
     */
    public static final int NO_BACKGROUND_FILL = 0;

    /**
     * A constant for use with the
     * {@link org.havi.ui.HVisible#setBackgroundMode} method of
     * {@link org.havi.ui.HVisible} which indicates that an associated
     * {@link org.havi.ui.HLook} should fill the bounding rectangle of the
     * {@link org.havi.ui.HVisible} with its current background color before
     * drawing any content. Any previous content will be erased during the
     * repainting of the {@link org.havi.ui.HVisible}.
     */
    public static final int BACKGROUND_FILL = 1;

    /**
     * A constant which indicates the first change value for use with the
     * hinting mechanism.
     */
    public static final int FIRST_CHANGE = 0;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the text content has
     * changed. The value for this hint is an array,
     * <code>java.lang.Object[9]</code>, which contains the state for which the
     * content changed (a <code>java.lang.Integer</code>) and the old content (a
     * <code>java.lang.String</code>) for all 8 states.
     */
    public static final int TEXT_CONTENT_CHANGE = 0;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the graphical
     * content has changed. The value for this hint is an array,
     * <code>java.lang.Object[9]</code>, which contains the state for which the
     * content changed (a <code>java.lang.Integer</code>) and the old content (a
     * <code>java.awt.Image</code>) for all 8 states.
     */
    public static final int GRAPHIC_CONTENT_CHANGE = 1;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the animated content
     * has changed. The value for this hint is an array,
     * <code>java.lang.Object[9]</code>, which contains the state for which the
     * content changed (a <code>java.lang.Integer</code>) and the old content (a
     * <code>java.awt.Image[]</code>) for all 8 states.
     */
    public static final int ANIMATE_CONTENT_CHANGE = 2;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the miscellaneous
     * content has changed. The value for this hint is an array,
     * <code>java.lang.Object[9]</code>, which contains the state for which the
     * content changed (a <code>java.lang.Integer</code>) and the old content (a
     * <code>java.lang.Object</code>) for all 8 states.
     */
    public static final int CONTENT_CHANGE = 3;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the interaction
     * state has changed. The value for this hint is a
     * <code>java.lang.Integer</code> which has the value of the old state.
     */
    public static final int STATE_CHANGE = 4;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the caret position
     * has changed. The value for this hint is a <code>java.lang.Integer</code>
     * which has the value of the old caret position.
     */
    public static final int CARET_POSITION_CHANGE = 5;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the echo character
     * has changed. The value for this hint is a
     * <code>java.lang.Character</code> which has the value of the old echo
     * character.
     */
    public static final int ECHO_CHAR_CHANGE = 6;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the editing mode,
     * adjustment mode or selection mode of the widget has changed. The value
     * for this hint is a <code>java.lang.Boolean</code> which has the value of
     * the old mode.
     */
    public static final int EDIT_MODE_CHANGE = 7;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the range of an
     * {@link org.havi.ui.HAdjustmentValue} component has changed. The value for
     * this hint is an array, <code>java.lang.Object[2]</code>, which contains
     * the old minimum and maximum extents of the range as
     * <code>java.lang.Integer</code>.
     */
    public static final int MIN_MAX_CHANGE = 8;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the thumb offsets of
     * an {@link org.havi.ui.HAdjustmentValue} component have changed. The value
     * for this hint is an array, <code>java.lang.Object[2]</code>, which
     * contains the old minimum and maximum thumb offsets as
     * <code>java.lang.Integer</code>.
     */
    public static final int THUMB_OFFSETS_CHANGE = 9;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the value of an
     * {@link org.havi.ui.HAdjustmentValue} component has changed. The value for
     * this hint is a <code>java.lang.Integer</code> which contains the old
     * index.
     */
    public static final int ADJUSTMENT_VALUE_CHANGE = 13;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the orientation of
     * an {@link org.havi.ui.HOrientable} component has changed. The value for
     * this hint is a <code>java.lang.Integer</code> which has the value of the
     * old orientation.
     */
    public static final int ORIENTATION_CHANGE = 10;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the value of an
     * {@link org.havi.ui.HTextValue} component has changed. The value for this
     * hint is a <code>java.lang.Integer</code> which contains the old value.
     *
     * @deprecated This constant shall not be used for the hinting mechanism.
     *             Looks shall treat it as {@link #UNKNOWN_CHANGE}. See also
     *             {@link #CARET_POSITION_CHANGE} and
     *             {@link #TEXT_CONTENT_CHANGE}.
     */
    public static final int TEXT_VALUE_CHANGE = 11;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the value of an
     * {@link org.havi.ui.HItemValue} component has changed. The value for this
     * hint is a <code>java.lang.Integer</code> which contains the old value.
     */
    public static final int ITEM_VALUE_CHANGE = 12;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the content of an
     * {@link org.havi.ui.HListGroup} component has changed. The value for this
     * hint is a <code>java.lang.Integer</code> which contains the old content.
     */
    public static final int LIST_CONTENT_CHANGE = 14;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the iconsize of an
     * {@link org.havi.ui.HListGroup} component has changed. The value for this
     * hint is a <code>java.lang.Integer</code> which contains the old size.
     */
    public static final int LIST_ICONSIZE_CHANGE = 15;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the labelsize of an
     * {@link org.havi.ui.HListGroup} component has changed. The value for this
     * hint is a <code>java.lang.Integer</code> which contains the old size.
     */
    public static final int LIST_LABELSIZE_CHANGE = 16;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the multiselection
     * setting of an {@link org.havi.ui.HListGroup} component has changed. The
     * value for this hint is a <code>java.lang.Integer</code> which contains
     * the old setting.
     */
    public static final int LIST_MULTISELECTION_CHANGE = 17;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the scrollposition
     * of an {@link org.havi.ui.HListGroup} component has changed. The value for
     * this hint is a <code>java.lang.Integer</code> which contains the old
     * position.
     */
    public static final int LIST_SCROLLPOSITION_CHANGE = 18;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the size of an
     * {@link org.havi.ui.HVisible} component has changed. The value for this
     * hint is a <code>java.lang.Dimension</code> which contains the old size.
     */
    public static final int SIZE_CHANGE = 19;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the border mode has
     * changed. The value for this hint is a <code>java.lang.Boolean</code>
     * which has the value of the old border mode.
     */
    public static final int BORDER_CHANGE = 20;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that
     * {@link org.havi.ui.HAnimateEffect#setRepeatCount} was called. Note that
     * this hint is also used, when the actual value has not changed. This is to
     * notify a reset of an internal counter. The value for this hint is a
     * <code>java.lang.Integer</code> which has the value of the old repeat
     * count.
     */
    public static final int REPEAT_COUNT_CHANGE = 21;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the value of an
     * {@link org.havi.ui.HStaticAnimation} component has changed. The value for
     * this hint is a <code>java.lang.Integer</code> which contains the old
     * value.
     */
    public static final int ANIMATION_POSITION_CHANGE = 22;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that the selection in
     * {@link org.havi.ui.HListGroup} was changed. The value for this hint is an
     * {@link org.havi.ui.HListElement}[] or <code>null</code> for the old
     * selection.
     */
    public static final int LIST_SELECTION_CHANGE = 23;

    /**
     * A constant for use with the hinting mechanism (see the
     * {@link org.havi.ui.HLook#widgetChanged} method in
     * {@link org.havi.ui.HLook}). This hint indicates that some unspecified
     * change has occurred. The value for this hint is a
     * <code>java.lang.Integer</code> which also has the value UNKNOWN_CHANGE.
     */
    public static final int UNKNOWN_CHANGE = 24;

    /**
     * A constant which indicates the last defined value for use with the
     * hinting mechanism.
     */
    public static final int LAST_CHANGE = UNKNOWN_CHANGE;

    /**
     * A constant for use with the {@link org.havi.ui.HVisible#setDefaultSize}
     * and {@link org.havi.ui.HVisible#getDefaultSize} methods of
     * {@link org.havi.ui.HVisible} which indicates that no default width is
     * desired for the {@link org.havi.ui.HVisible}.
     */
    public static final int NO_DEFAULT_WIDTH = -1;

    /**
     * A constant for use with the {@link org.havi.ui.HVisible#setDefaultSize}
     * and {@link org.havi.ui.HVisible#getDefaultSize} methods of
     * {@link org.havi.ui.HVisible} which indicates that no default height is
     * desired for the {@link org.havi.ui.HVisible}.
     */
    public static final int NO_DEFAULT_HEIGHT = -1;

    /**
     * A constant for use with the {@link org.havi.ui.HVisible#setDefaultSize}
     * and {@link org.havi.ui.HVisible#getDefaultSize} methods of
     * {@link org.havi.ui.HVisible} which indicates that no default size is
     * desired for the {@link org.havi.ui.HVisible}.
     * <p>
     * The contents of the Dimension object cannot be relied upon. Comparisons
     * must always be done using object identity, i.e. using the &quot;==&quot;
     * operator.
     */
    public static final java.awt.Dimension NO_DEFAULT_SIZE = new java.awt.Dimension(NO_DEFAULT_WIDTH, NO_DEFAULT_HEIGHT);

    /**
     * Creates an {@link org.havi.ui.HVisible} component with no
     * {@link org.havi.ui.HLook}. See the class description for details of
     * constructor parameters and default values.
     */
    public HVisible()
    {
        iniz();
    }

    /**
     * Creates an {@link org.havi.ui.HVisible} component with the given
     * {@link org.havi.ui.HLook}. See the class description for details of
     * constructor parameters and default values.
     * <p>
     * Applications shall not use <code>HLook</code>s with this constructor
     * unless those <code>HLook</code>s are specified as working with HVisible.
     * If an <code>HLook</code> is used which is specified as only working with
     * specific sub-classes of <code>HVisible</code> then the failure mode is
     * implementation dependent.
     *
     * @param hlook
     *            The {@link org.havi.ui.HLook} associated with the
     *            {@link org.havi.ui.HVisible} component.
     */
    public HVisible(HLook hlook)
    {
        iniz();
        inizLook(hlook);
    }

    /**
     * Creates an {@link org.havi.ui.HVisible} component with the given
     * {@link org.havi.ui.HLook} and the specified location and size. See the
     * class description for details of constructor parameters and default
     * values.
     *
     * @param hlook
     *            The {@link org.havi.ui.HLook} associated with the
     *            {@link org.havi.ui.HVisible} component.
     * @param x
     *            the x-coordinate of the {@link org.havi.ui.HVisible} component
     *            within its Container.
     * @param y
     *            the y-coordinate of the {@link org.havi.ui.HVisible} component
     *            within its Container.
     * @param width
     *            the width of the {@link org.havi.ui.HVisible} component in
     *            pixels.
     * @param height
     *            the height of the {@link org.havi.ui.HVisible} component in
     *            pixels.
     */
    public HVisible(HLook hlook, int x, int y, int width, int height)
    {
        super(x, y, width, height);
        iniz();
        setDefaultSize(new Dimension(width, height));
        inizLook(hlook);
    }

    /**
     * By default an {@link org.havi.ui.HVisible} component is not
     * focus-traversable.
     *
     * @return false
     * @see java.awt.Component#isFocusTraversable
     */
    public boolean isFocusTraversable()
    {
        return this instanceof HNavigable;
    }

    /**
     * Draws the current state of the component, by calling the
     * {@link org.havi.ui.HLook#showLook} method of the associated
     * {@link org.havi.ui.HLook}. If no {@link org.havi.ui.HLook} is associated
     * with the component, (i.e. the {@link org.havi.ui.HVisible} was created
     * with a null {@link org.havi.ui.HLook} or the look has been set to null
     * using {@link org.havi.ui.HVisible#setLook}) then the paint method should
     * do nothing. This mechanism may be used for components that wish to extend
     * {@link org.havi.ui.HVisible}, and override the paint method, without
     * supporting the {@link org.havi.ui.HLook} interface.
     *
     * @param g
     *            the graphics context to use for painting.
     */
    public void paint(java.awt.Graphics g)
    {
        HLook look = getLook();
        if (look != null) look.showLook(g, this, getInteractionState());
    }

    /**
     * The update() method in {@link org.havi.ui.HVisible} overrides that in
     * Component and does not clear the background of the component, it simply
     * modifies the current Color of the Graphics object to match that of the
     * components background Color, and calls the paint() method.
     *
     * @param g
     *            the graphics context to use for updating.
     */
    public void update(java.awt.Graphics g)
    {
        g.setColor(getBackground());
        paint(g);
    }

    /**
     * Sets a single piece of text content for this component, per state.
     * Different (single pieces of) content can be associated with the different
     * states of a component.
     * <p>
     * If the {@link org.havi.ui.HVisible} has an associated
     * {@link org.havi.ui.HLook}, then it should repaint itself.
     *
     * @param string
     *            The content. If the content is null, then any currently
     *            assigned content shall be removed for the specified state.
     * @param state
     *            The state of the component for which this content should be
     *            displayed. Note that content is set on the
     *            <code>XXX_STATE</code> constants defined in
     *            {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     */
    public void setTextContent(String string, int state)
    {
        validState(state, true);

        // Let's allocate the array since now we want to actually use it.
        synchronized (this)
        {
            if (textContent == null) textContent = new String[STATE_COUNT];
        }

        // Save current content info
        HLook look = getLook();
        HChangeData info = (look == null) ? null : saveContent(state, TEXT_CONTENT_CHANGE);

        // Make changes
        switch (state)
        {
            default:
                textContent[mapStateIndex(state)] = string;
                break;
            case ALL_STATES:
                for (int i = FIRST_INDEX; i <= LAST_INDEX; ++i)
                    textContent[i] = string;
                break;
        }

        // Notify look about changes
        notifyLook(info);
    }

    /**
     * Sets a single piece of graphical content for this component, per state.
     * Different (single pieces of) content can be associated with the different
     * states of a component.
     * <p>
     * Note that the content is not copied, merely its object reference.
     * <p>
     * If the {@link org.havi.ui.HVisible} has an associated
     * {@link org.havi.ui.HLook}, then it should repaint itself.
     *
     * @param image
     *            The content. If the content is null, then any currently
     *            assigned content shall be removed for the specified state.
     * @param state
     *            The state of the component for which this content should be
     *            displayed. Note that content is set on the
     *            <code>XXX_STATE</code> constants defined in
     *            {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     */
    public void setGraphicContent(Image image, int state)
    {
        validState(state, true);

        // Let's allocate the array since now we want to actually use it.
        synchronized (this)
        {
            if (graphicContent == null) graphicContent = new Image[STATE_COUNT];
        }

        // Save current content info
        HLook look = getLook();
        HChangeData info = (look == null) ? null : saveContent(state, GRAPHIC_CONTENT_CHANGE);

        // Make changes
        switch (state)
        {
            default:
                graphicContent[mapStateIndex(state)] = image;
                break;
            case ALL_STATES:
                for (int i = FIRST_INDEX; i <= LAST_INDEX; ++i)
                    graphicContent[i] = image;
                break;
        }

        // Notify look about changes
        notifyLook(info);
    }

    /**
     * Sets an array of graphical content (primarily used for animation), per
     * state. Different (single arrays of) content can be associated with the
     * different states of a component.
     * <p>
     * Note that the content is not copied, merely its object reference.
     * <p>
     * If the {@link org.havi.ui.HVisible} has an associated
     * {@link org.havi.ui.HLook}, then it should repaint itself.
     *
     * @param imageArray
     *            An array of images that make up the animation. If the array is
     *            null, then any currently assigned content shall be removed for
     *            the specified state.
     * @param state
     *            The state of the component for which this content should be
     *            displayed. Note that content is set on the
     *            <code>XXX_STATE</code> constants defined in
     *            {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     */
    public void setAnimateContent(Image[] imageArray, int state)
    {
        validState(state, true);

        HAnimateEffect anim = null;
        boolean restart = false;
        if (this instanceof HAnimateEffect)
        {
            anim = (HAnimateEffect) this;

            // So we can restart the animation
            restart = anim.isAnimated();

            // Make sure the animation is stopped
            anim.stop();

            // Reset the position
            anim.setPosition(0);
        }

        // Let's allocate the array since now we want to actually use it.
        synchronized (this)
        {
            if (animateContent == null) animateContent = new Image[STATE_COUNT][];
        }

        // Save current content info
        HLook look = getLook();
        HChangeData info = (look == null) ? null : saveContent(state, ANIMATE_CONTENT_CHANGE);

        // Make changes
        switch (state)
        {
            default:
                animateContent[mapStateIndex(state)] = imageArray;
                break;
            case ALL_STATES:
                for (int i = FIRST_INDEX; i <= LAST_INDEX; ++i)
                    animateContent[i] = imageArray;
                break;
        }

        if (anim != null)
        {
            // Restart the animation if it was running
            // The current repeat count will automatically be reset to 0
            if (restart) anim.start();
        }

        // Notify look about changes
        notifyLook(info);
    }

    /**
     * Sets a single piece of content for this component, per state. Different
     * (single pieces of) content can be associated with the different states of
     * a component.
     * <p>
     * Note that the content is not copied, merely its object reference.
     * <p>
     * If the {@link org.havi.ui.HVisible} has an associated
     * {@link org.havi.ui.HLook}, then it should repaint itself.
     *
     * @param object
     *            The content. If the content is null, then any currently
     *            assigned content shall be removed for the specified state.
     * @param state
     *            The state of the component for which this content should be
     *            displayed. Note that content is set on the
     *            <code>XXX_STATE</code> constants defined in
     *            {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     */
    public void setContent(Object object, int state)
    {
        validState(state, true);

        // Let's allocate the array since now we want to actually use it.
        synchronized (this)
        {
            if (content == null) content = new Object[STATE_COUNT];
        }

        // Save current content info
        HLook look = getLook();
        HChangeData info = (look == null) ? null : saveContent(state, CONTENT_CHANGE);

        // Make changes
        switch (state)
        {
            default:
                content[mapStateIndex(state)] = object;
                break;
            case ALL_STATES:
                for (int i = FIRST_INDEX; i <= LAST_INDEX; ++i)
                    content[i] = object;
                break;
        }

        // Notify look about changes
        notifyLook(info);
    }

    /**
     * Gets the text content for this component.
     *
     * @param state
     *            The state for which content is to be retrieved. Note that
     *            content is set on the <code>XXX_STATE</code> constants defined
     *            in {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     * @return The text content associated with the specified state. If no text
     *         content has been set for the specified state, then
     *         <code>null</code> is returned.
     */
    public String getTextContent(int state)
    {
        validState(state, false);

        return (textContent == null) ? null : textContent[mapStateIndex(state)];
    }

    /**
     * Gets the graphic content for this component.
     *
     * @param state
     *            The state for which content is to be retrieved. Note that
     *            content is set on the <code>XXX_STATE</code> constants defined
     *            in {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     * @return The graphical content associated with the specified state. If no
     *         graphical content has been set for the specified state, then
     *         <code>null</code> is returned.
     */
    public Image getGraphicContent(int state)
    {
        validState(state, false);

        return (graphicContent == null) ? null : graphicContent[mapStateIndex(state)];
    }

    /**
     * Gets the animate content for this component.
     *
     * @param state
     *            The state for which content is to be retrieved. Note that
     *            content is set on the <code>XXX_STATE</code> constants defined
     *            in {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     * @return The animate content associated with the specified state. If no
     *         animate content has been set for the specified state, then
     *         <code>null</code> is returned.
     */
    public Image[] getAnimateContent(int state)
    {
        validState(state, false);

        return (animateContent == null) ? null : animateContent[mapStateIndex(state)];
    }

    /**
     * Gets the content for this component.
     *
     * @param state
     *            The state for which content is to be retrieved. Note that
     *            content is set on the <code>XXX_STATE</code> constants defined
     *            in {@link org.havi.ui.HState}, and <em>not</em> on the
     *            <code>XXX_STATE_BIT</code> constants. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     * @return The content associated with the specified state. If no content
     *         has been set for the specified state, then <code>null</code> is
     *         returned.
     */
    public Object getContent(int state)
    {
        validState(state, false);

        return (content == null) ? null : content[mapStateIndex(state)];
    }

    /**
     * Sets the {@link org.havi.ui.HLook} for this component.
     *
     * @param hlook
     *            The {@link org.havi.ui.HLook} that is to be used for this
     *            component. Note that this parameter may be null, in which case
     *            the component will not draw itself until a look is set.
     * @exception HInvalidLookException
     *                If the Look is not compatible with this type of component,
     *                for example a graphic look being set on a text component,
     *                an HInvalidLookException is thrown. Note that HVisible
     *                itself will never throw this exception, but it may be
     *                thrown by a subclass which has overridden this method.
     */
    public void setLook(HLook hlook) throws HInvalidLookException
    {
        lookData.clear(); // reset look data
        look = hlook;
    }

    /**
     * Gets the {@link org.havi.ui.HLook} for this component.
     *
     * @return the {@link org.havi.ui.HLook} that is being used by this
     *         component - if no {@link org.havi.ui.HLook} has been set, then
     *         returns null.
     */
    public HLook getLook()
    {
        return look;
    }

    /**
     * Gets the preferred size of the {@link org.havi.ui.HVisible}. The
     * getPreferredSize method of the {@link org.havi.ui.HLook} that is
     * associated with this {@link org.havi.ui.HVisible} will be called to
     * calculate the dimensions.
     *
     * @return A dimension object indicating this {@link org.havi.ui.HVisible}'s
     *         preferred size - if no {@link org.havi.ui.HLook} has been
     *         associated with the {@link org.havi.ui.HVisible}, then the
     *         current {@link org.havi.ui.HVisible} dimensions as determined
     *         with <code>getSize</code> will be returned.
     * @see org.havi.ui.HLook#getPreferredSize
     */
    public Dimension getPreferredSize()
    {
        HLook look = getLook();
        return (look == null) ? getSize() : look.getPreferredSize(this);
    }

    /**
     * Gets the maximum size of the {@link org.havi.ui.HVisible}. The
     * getMaximumSize method of the {@link org.havi.ui.HLook} that is associated
     * with this {@link org.havi.ui.HVisible} will be called to calculate the
     * dimensions.
     *
     * @return A dimension object indicating this {@link org.havi.ui.HVisible}'s
     *         maximum size - if no {@link org.havi.ui.HLook} has been
     *         associated with the {@link org.havi.ui.HVisible}, then the
     *         current {@link org.havi.ui.HVisible} dimensions as determined
     *         with <code>getSize</code> will be returned.
     * @see org.havi.ui.HLook#getMaximumSize
     */
    public Dimension getMaximumSize()
    {
        HLook look = getLook();
        return (look == null) ? getSize() : look.getMaximumSize(this);
    }

    /**
     * Gets the minimum size of the {@link org.havi.ui.HVisible}. The
     * getMinimumSize method of the {@link org.havi.ui.HLook} that is associated
     * with this {@link org.havi.ui.HVisible} will be called to calculate the
     * dimensions.
     *
     * @return A dimension object indicating this {@link org.havi.ui.HVisible}'s
     *         minimum size - if no {@link org.havi.ui.HLook} has been
     *         associated with the {@link org.havi.ui.HVisible}, then the
     *         current {@link org.havi.ui.HVisible} dimensions as determined
     *         with <code>getSize</code> will be returned.
     * @see org.havi.ui.HLook#getMinimumSize
     */
    public Dimension getMinimumSize()
    {
        HLook look = getLook();
        return (look == null) ? getSize() : look.getMinimumSize(this);
    }

    /**
     * Set the interaction state for this component. This method is provided for
     * the use by subclasses of {@link org.havi.ui.HVisible} to change the
     * interaction state of the {@link org.havi.ui.HVisible}. Subclasses
     * <em>MUST NOT</em> manipulate the state by any other mechanism.
     * <p>
     * Attempts to set states which are not valid for the subclass will cause an
     * <code>java.lang.IllegalArgumentException</code> to be thrown. See the
     * class descriptions of each component for the definitions of which states
     * are valid.
     *
     * @param state
     *            the interaction state for this component. A
     *            <code>java.lang.IllegalArgumentException</code> will be thrown
     *            if a <code>STATE_BIT</code> rather than a <code>STATE</code>
     *            is passed.
     * @see org.havi.ui.HState
     */
    protected void setInteractionState(int state)
    {
        boolean paint = false;
        switch (state)
        {
            case DISABLED_ACTIONED_FOCUSED_STATE:
            case DISABLED_ACTIONED_STATE:
                if (!(this instanceof HSwitchable))
                {
                    break;
                }
            case ACTIONED_FOCUSED_STATE:
            case ACTIONED_STATE:
                // Actionable or Switchable...
                if (!(this instanceof HActionable)) break;
                if ((state & ACTIONED_STATE_BIT) != 0 && !(this instanceof HSwitchable)) paint = true;
            case FOCUSED_STATE:
            case DISABLED_FOCUSED_STATE:
                // Focusable...
                if (!(this instanceof HNavigable)) break;
            case DISABLED_STATE:
            case NORMAL_STATE:
                // Anybody is allowed in here...
                int oldstate = this.state;
                this.state = state;

                // Call HComponent.setEnabled().
                // So isEnabled() returns a valid state.
                super.setEnabled((state & DISABLED_STATE_BIT) == 0);

                // Force a repaint NOW for HActionable ACTIONs
                java.awt.Graphics g;
                if (paint && (g = getGraphics()) != null)
                {
                    paint(g);
                    g.dispose();
                    HaviToolkit.getToolkit().flush();
                }
                else
                    notifyLook(STATE_CHANGE, new Integer(oldstate));

                return;
        }
        // Fall through on invalid states
        throw new IllegalArgumentException("Invalid state: " + state);
    }

    /**
     * Return the interaction state the component is currently in.
     *
     * @return the interaction state the component is currently in.
     * @see org.havi.ui.HState
     */
    public int getInteractionState()
    {
        return state;
    }

    /**
     * Sets the text layout manager that should be used to layout the text for
     * this component.
     *
     * @param manager
     *            the {@link org.havi.ui.HTextLayoutManager} to be used by this
     *            component.
     */
    public void setTextLayoutManager(HTextLayoutManager manager)
    {
        textLayout = manager;
        unspecifiedChange();
    }

    /**
     * Gets the text layout manager that is being used to layout this text.
     *
     * @return The {@link org.havi.ui.HTextLayoutManager} that is being used by
     *         this component.
     */
    public HTextLayoutManager getTextLayoutManager()
    {
        return textLayout;
    }

    /**
     * Get the background mode of this {@link org.havi.ui.HVisible}. The return
     * value specifies how the look should draw the background (i.e. a rectangle
     * filling the bounds of the {@link org.havi.ui.HVisible}).
     *
     * @return one of {@link org.havi.ui.HVisible#NO_BACKGROUND_FILL} or
     *         {@link org.havi.ui.HVisible#BACKGROUND_FILL}.
     */
    public int getBackgroundMode()
    {
        return backgroundMode;
    }

    /**
     * Set the background drawing mode. The value specifies how the look should
     * draw the background (i.e. a rectangle filling the bounds of the
     * {@link org.havi.ui.HVisible}).
     *
     * @param mode
     *            one of {@link org.havi.ui.HVisible#NO_BACKGROUND_FILL} or
     *            {@link org.havi.ui.HVisible#BACKGROUND_FILL}
     */
    public void setBackgroundMode(int mode)
    {
        switch (mode)
        {
            case NO_BACKGROUND_FILL:
            case BACKGROUND_FILL:
                backgroundMode = mode;
                unspecifiedChange();
                break;
            default:
                throw new IllegalArgumentException("Illegal BackgroundMode: " + mode);
        }
    }

    /**
     * Returns true if the entire {@link org.havi.ui.HVisible} area, as given by
     * the java.awt.Component#getBounds method, is fully opaque, i.e. its
     * {@link org.havi.ui.HLook} guarantees that all pixels are painted in an
     * opaque Color.
     * <p>
     * This method will call the {@link org.havi.ui.HLook#isOpaque} method of an
     * associated {@link org.havi.ui.HLook} if one is set. If no HLook is
     * associated this method returns false.
     * <p>
     * The default return value is implementation specific and depends on the
     * background painting mode of the given {@link org.havi.ui.HVisible}. The
     * consequences of an invalid overridden value are implementation specific.
     *
     * @return true if all the pixels with the java.awt.Component#getBounds
     *         method are fully opaque, i.e. its associated
     *         {@link org.havi.ui.HLook} guarantees that all pixels are painted
     *         in an opaque Color.
     */
    public boolean isOpaque()
    {
        HLook look;
        return ((look = getLook()) != null) && look.isOpaque(this);
    }

    /**
     * Set the preferred default size for this component when a layout manager
     * is in use.
     * <p>
     * Note that the size set with this method is not a <em>guaranteed</em>
     * size; if set it will be passed to the layout manager through the
     * {@link org.havi.ui.HLook#getPreferredSize} method. The default size of a
     * component is to be interpreted as the area in which the component can be
     * rendered, <em>excluding</em> look-specific borders.
     * <p>
     * Valid arguments include {@link org.havi.ui.HVisible#NO_DEFAULT_SIZE}, and
     * Dimensions containing {@link org.havi.ui.HVisible#NO_DEFAULT_WIDTH} or
     * {@link org.havi.ui.HVisible#NO_DEFAULT_HEIGHT}.
     *
     * @param defaultSize
     *            specifies the default preferred size. If this parameter is
     *            null a <code>java.lang.NullPointerException</code> will be
     *            thrown.
     *            <p>
     *            If this parameter specifies a size smaller than an
     *            implementation-defined minimum size, the preferred size of
     *            this component shall be set to that implementation-defined
     *            minimum size.
     */
    public void setDefaultSize(java.awt.Dimension defaultSize)
    {
        // If no size is given...
        if (defaultSize == null) throw new NullPointerException("The defaultSize cannot be null");

        // If size is invalid...
        if (defaultSize != NO_DEFAULT_SIZE
                && ((defaultSize.width != NO_DEFAULT_WIDTH && defaultSize.width < MIN_DEFAULT_WIDTH) || (defaultSize.height != NO_DEFAULT_HEIGHT && defaultSize.height < MIN_DEFAULT_HEIGHT)))
            throw new IllegalArgumentException("The defaultSize is too small: " + defaultSize);

        // Otherwise, set the size...
        this.defaultSize = (defaultSize == NO_DEFAULT_SIZE) ? defaultSize : new Dimension(defaultSize);
    }

    /**
     * Returns the default preferred size to be used for this component when a
     * layout manager is in use. If no default size has been set using the
     * {@link org.havi.ui.HVisible#setDefaultSize} method
     * {@link org.havi.ui.HVisible#NO_DEFAULT_SIZE} is returned.
     * <p>
     * If the parent Container into which the HVisible is placed has no layout
     * manager the default preferred size has no effect.
     * <p>
     * The default size of a component is to be interpreted as the area in which
     * the component can be rendered, <em>excluding</em> look-specific borders.
     *
     * @return the default preferred size to be used for this component when a
     *         layout manager is in use.
     */
    public java.awt.Dimension getDefaultSize()
    {
        Dimension defaultSize = this.defaultSize;
        return (defaultSize == NO_DEFAULT_SIZE || defaultSize == null) ? defaultSize : new Dimension(defaultSize);
    }

    /**
     * Retrieve a look-specific private data object. Instances of
     * {@link org.havi.ui.HLook} may use this method to retrieve private data
     * (e.g. layout hints, cached images etc.) from the HVisible. Use of this
     * mechanism is an implementation option. If this mechanism is not used by
     * an implementation, or no data has been set for the specified key this
     * method returns <code>null</code>.
     *
     * @param key
     *            an object which uniquely identifies the type of look for which
     *            the private data is to be retrieved. Keys need not be unique
     *            across different instances of the same look class.
     * @return a private data object as previously set using
     *         {@link org.havi.ui.HVisible#setLookData}, or <code>null</code>.
     * @see org.havi.ui.HLook
     * @see org.havi.ui.HVisible#setLookData
     */
    public java.lang.Object getLookData(java.lang.Object key)
    {
        return lookData.get(key);
    }

    /**
     * Set a look-specific private data object. Instances of
     * {@link org.havi.ui.HLook} may use this method to set private data (e.g.
     * layout hints, cached images etc.) on the HVisible. Use of this mechanism
     * is an implementation option. If this mechanism is not used by an
     * implementation, this method will have no effect and calls to
     * {@link org.havi.ui.HVisible#getLookData} shall return <code>null</code>.
     * If for this specified key a data object has been set,the old data object
     * shall be replaced with the new one.
     *
     * @param key
     *            an object which uniquely identifies the type of look for which
     *            the private data is to be retrieved. Keys need not be unique
     *            across different instances of the same look class.
     * @param data
     *            a private data object, or null to remove any current object
     *            set on this HVisible.
     * @see org.havi.ui.HLook
     * @see org.havi.ui.HVisible#getLookData
     */
    public void setLookData(java.lang.Object key, java.lang.Object data)
    {
        if (data != null)
            lookData.put(key, data);
        else
            lookData.remove(key);
    }

    /**
     * Set the horizontal alignment of any state-based content rendered by an
     * associated {@link org.havi.ui.HLook}. If content is not used in the
     * rendering of this HVisible calls to this method shall change the current
     * alignment mode, but this will not affect the rendered representation.
     *
     * @param halign
     *            the new horizontal alignment mode, one of
     *            {@link org.havi.ui.HVisible#HALIGN_LEFT},
     *            {@link org.havi.ui.HVisible#HALIGN_CENTER},
     *            {@link org.havi.ui.HVisible#HALIGN_RIGHT} or
     *            {@link org.havi.ui.HVisible#HALIGN_JUSTIFY}.
     */
    public void setHorizontalAlignment(int halign)
    {
        switch (halign)
        {
            case HALIGN_LEFT:
            case HALIGN_CENTER:
            case HALIGN_RIGHT:
            case HALIGN_JUSTIFY:
                this.halign = halign;
                unspecifiedChange();
                break;
            default:
                throw new IllegalArgumentException("Illegal Horiz Alignment: " + halign);
        }
    }

    /**
     * Set the vertical alignment of any state-based content rendered by an
     * associated {@link org.havi.ui.HLook}. If content is not used in the
     * rendering of this HVisible calls to this method shall change the current
     * alignment mode, but this will not affect the rendered representation.
     *
     * @param valign
     *            the new vertical alignment mode, one of
     *            {@link org.havi.ui.HVisible#VALIGN_TOP},
     *            {@link org.havi.ui.HVisible#VALIGN_CENTER},
     *            {@link org.havi.ui.HVisible#VALIGN_BOTTOM} or
     *            {@link org.havi.ui.HVisible#VALIGN_JUSTIFY}.
     */
    public void setVerticalAlignment(int valign)
    {
        switch (valign)
        {
            case VALIGN_TOP:
            case VALIGN_CENTER:
            case VALIGN_BOTTOM:
            case VALIGN_JUSTIFY:
                this.valign = valign;
                unspecifiedChange();
                break;
            default:
                throw new IllegalArgumentException("Illegal Vert Alignment: " + valign);
        }
    }

    /**
     * Get the horizontal alignment of any state-based content rendered by an
     * associated {@link org.havi.ui.HLook}. If content is not used in the
     * rendering of this HVisible the value returned shall be valid, but has no
     * affect on the rendered representation.
     *
     * @return the current horizontal alignment mode, one of
     *         {@link org.havi.ui.HVisible#HALIGN_LEFT},
     *         {@link org.havi.ui.HVisible#HALIGN_CENTER},
     *         {@link org.havi.ui.HVisible#HALIGN_RIGHT} or
     *         {@link org.havi.ui.HVisible#HALIGN_JUSTIFY}.
     */
    public int getHorizontalAlignment()
    {
        return halign;
    }

    /**
     * Get the vertical alignment of any state-based content rendered by an
     * associated {@link org.havi.ui.HLook}. If content is not used in the
     * rendering of this HVisible the value returned shall be valid, but has no
     * affect on the rendered representation.
     *
     * @return the current vertical alignment mode, one of
     *         {@link org.havi.ui.HVisible#VALIGN_TOP},
     *         {@link org.havi.ui.HVisible#VALIGN_CENTER},
     *         {@link org.havi.ui.HVisible#VALIGN_BOTTOM} or
     *         {@link org.havi.ui.HVisible#VALIGN_JUSTIFY}.
     */
    public int getVerticalAlignment()
    {
        return valign;
    }

    /**
     * Set the resize mode of this <code>HVisible</code>. If the associated
     * <code>HLook</code> does not render content or if scaling is not
     * supported, changing the mode may have no visible effect.
     *
     * @param resize
     *            the new scaling mode, one of
     *            {@link org.havi.ui.HVisible#RESIZE_NONE},
     *            {@link org.havi.ui.HVisible#RESIZE_PRESERVE_ASPECT} or
     *            {@link org.havi.ui.HVisible#RESIZE_ARBITRARY}
     */
    public void setResizeMode(int resize)
    {
        switch (resize)
        {
            case RESIZE_NONE:
            case RESIZE_PRESERVE_ASPECT:
            case RESIZE_ARBITRARY:
                resizeMode = resize;
                unspecifiedChange();
                break;
            default:
                throw new IllegalArgumentException("Illegal Resize Mode: " + halign);
        }
    }

    /**
     * Get the scaling mode for scaling any state-based content rendered by an
     * associated {@link org.havi.ui.HLook}. If content is not used in the
     * rendering of this HVisible the value returned shall be valid, but has no
     * affect on the rendered representation.
     *
     * @return the current scaling mode, one of
     *         {@link org.havi.ui.HVisible#RESIZE_NONE},
     *         {@link org.havi.ui.HVisible#RESIZE_PRESERVE_ASPECT} or
     *         {@link org.havi.ui.HVisible#RESIZE_ARBITRARY}
     */
    public int getResizeMode()
    {
        return resizeMode;
    }

    /**
     * Modifies the {@link org.havi.ui.HState} of this
     * {@link org.havi.ui.HVisible} by calling setInteractionState, depending on
     * the value of b. This method should invoke the superclass method.
     *
     * @param b
     *            If true, this {@link org.havi.ui.HVisible} is enabled;
     *            otherwise this {@link org.havi.ui.HVisible} is disabled.
     */
    public void setEnabled(boolean b)
    {
        // Called from setInteractionState()
        // super.setEnabled(b);

        int newState = getInteractionState();
        newState = b ? (newState & ~DISABLED_STATE_BIT) : (newState | DISABLED_STATE_BIT);

        try
        {
            setInteractionState(newState);
        }
        catch (IllegalArgumentException e)
        {
            // ignored
        }

    }

    /**
     * The method en- or disables rendering of platform-specific borders. If
     * <code>enable</code> is <code>true</code>, the associated
     * {@link org.havi.ui.HLook} may render borders, if supported. If the
     * specified parameter is <code>false</code>, no borders will be rendered
     * and the whole area is available to the content Additionally, all values
     * of the insets objects returned by {@link org.havi.ui.HLook#getInsets}
     * will be zero.
     * <p>
     * The setting shall only be used by {@link org.havi.ui.HAnimateLook},
     * {@link org.havi.ui.HGraphicLook} and {@link org.havi.ui.HTextLook}. Only
     * for these looks shall {@link org.havi.ui.HLook#widgetChanged} with
     * {@link org.havi.ui.HVisible#BORDER_CHANGE} be called. All other looks
     * shall ignore this value and the method shall not be called. The behavior
     * of third party looks is not defined. The default value is
     * <code>true</code>.
     *
     * @param enable
     *            the new border mode to enable or disable border rendering
     * @see #getBordersEnabled
     */
    public void setBordersEnabled(boolean enable)
    {
        if (hasBorders != enable)
        {
            hasBorders = enable;

            // Send a widgetChanged event!
            // Only to HGraphicLook, HTextLook, HAnimateLook */
            HLook look;
            if ((look = getLook()) != null
                    && (look instanceof HGraphicLook || look instanceof HTextLook || look instanceof HAnimateLook))
            {
                notifyLook(BORDER_CHANGE, new Boolean(!enable));
            }
        }
    }

    /**
     * Returns the current border mode. Applications should rather call
     * {@link org.havi.ui.HLook#getInsets} to find out, if any borders are
     * rendered.
     *
     * @return the current border mode
     * @see #setBordersEnabled
     */
    public boolean getBordersEnabled()
    {
        return hasBorders;
    }

    /**
     * Common constructor initialization.
     */
    private void iniz()
    {
        addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentResized(java.awt.event.ComponentEvent e)
            {
                notifyLook(SIZE_CHANGE, oldSize);
            }
        });
    }

    /**
     * Initialize the look to be used.
     */
    private void inizLook(HLook look)
    {
        try
        {
            setLook(look);
        }
        catch (HInvalidLookException ignored)
        {
        }
    }

    /**
     * Checks whether the given state is a valid state. If not, an
     * IllegalArgumentException is thrown.
     */
    private void validState(int state, boolean all) throws IllegalArgumentException
    {
        switch (state)
        {
            case NORMAL_STATE:
            case FOCUSED_STATE:
            case ACTIONED_STATE:
            case ACTIONED_FOCUSED_STATE:
            case DISABLED_STATE:
            case DISABLED_ACTIONED_STATE:
            case DISABLED_ACTIONED_FOCUSED_STATE:
            case DISABLED_FOCUSED_STATE:
                break;
            case ALL_STATES:
                if (all) break;
            default:
                throw new IllegalArgumentException("Invalid state: " + state);
        }
    }

    /**
     * Maps the given {@link HState} value to an index for use in accessing data
     * from an array.
     *
     * @param state
     *            the given state value
     * @return an index into a state array
     */
    private int mapStateIndex(int state)
    {
        return state & ALL_STATES;
    }

    /**
     * Saves the current state of this <code>HVisible</code>'s content to an
     * <code>HChangeData</code> object.
     *
     * @param state
     *            the state of content that is being changed
     * @param hint
     *            the content change hint to use
     */
    private HChangeData saveContent(int state, int hint)
    {
        Object[] array = new Object[9];

        array[0] = new Integer(state);
        for (int i = 0; i < STATE_COUNT;)
        {
            state = i | NORMAL_STATE;
            switch (hint)
            {
                case TEXT_CONTENT_CHANGE:
                    array[++i] = getTextContent(state);
                    break;
                case GRAPHIC_CONTENT_CHANGE:
                    array[++i] = getGraphicContent(state);
                    break;
                case ANIMATE_CONTENT_CHANGE:
                    array[++i] = getAnimateContent(state);
                    break;
                case CONTENT_CHANGE:
                    array[++i] = getContent(state);
                    break;
            }
        }
        return new HChangeData(hint, array);
    }

    /**
     * Notifies the current <code>HLook</code>, if there is one, of the
     * specified change.
     *
     * Does not allocate an <code>HChangeData</code> or
     * <code>HChangeData[]</code> if there is no <code>HLook</code> associated
     * with this component.
     *
     * @param hint
     *            the change of which the look should be made aware
     * @param data
     *            the data associated with the change
     *
     * @see HLook#widgetChanged
     */
    void notifyLook(int hint, Object data)
    {
        HLook look = getLook();
        if (look != null) look.widgetChanged(this, new HChangeData[] { new HChangeData(hint, data) });
    }

    /**
     * Notifies the current <code>HLook</code>, if there is one, of the
     * specified change.
     *
     * Does not allocate an <code>HChangeData[]</code> if there is no
     * <code>HLook</code> associated with this component.
     *
     * @param info
     *            the change of which the look should be made aware
     *
     * @see HLook#widgetChanged
     */
    void notifyLook(HChangeData info)
    {
        HLook look = getLook();
        if (look != null) look.widgetChanged(this, new HChangeData[] { info });
    }

    /**
     * Notifies the current <code>HLook</code>, if there is one, of the
     * specified changes.
     *
     * @param info
     *            the changes of which the look should be made aware
     *
     * @see HLook#widgetChanged
     */
    void notifyLook(HChangeData[] info)
    {
        HLook look = getLook();
        if (look != null) look.widgetChanged(this, info);
    }

    /**
     * Notifies the current <code>HLook</code> about an {@link #UNKNOWN_CHANGE
     * unspecified} change to this component. If there is no {@link #getLook()
     * current} look, then this method has no affect.
     * <p>
     * Only executed if the <code>static final</code> variable
     * <code>NOTIFY_UNSPECIFIED</code> is <code>true</code>.
     * <p>
     * This method is only <i>package</i> private so that it can be called by
     * <code>org.havi.ui</code> subclasses, if necessary.
     *
     * @see #setTextLayoutManager(HTextLayoutManager)
     * @see #setBackgroundMode(int)
     * @see #setHorizontalAlignment(int)
     * @see #setVerticalAlignment(int)
     * @see #setResizeMode(int)
     */
    void unspecifiedChange()
    {
        if (NOTIFY_UNSPECIFIED)
        {
            notifyLook(UNKNOWN_CHANGE, new Integer(UNKNOWN_CHANGE));
        }
    }

    /**
     * Represents an explicitly-set <code>null</code> default look.
     */
    private static final Object NULL_LOOK = new Object();

    /**
     * Provides for a common implementation of <code>setDefaultLook()</code>.
     * <p>
     * This method isn't directly used by <code>HVisible</code>, but is instead
     * here as a convenience for sub-classes.
     *
     * @param prop
     *            key for looking up the default look in the context of the
     *            current app
     * @param look
     *            the new default look
     */
    static void setDefaultLookImpl(String prop, HLook look)
    {
        toolkit.setGlobalData(prop, look == null ? NULL_LOOK : look);
    }

    /**
     * Provides for a common implementation of <code>getDefaultLook()</code>.
     * The first time that this is called in the context of an application, this
     * will return a new instance of a look.
     * <p>
     * This method isn't directly used by <code>HVisible</code>, but is instead
     * here as a convenience for sub-classes.
     *
     * @param prop
     *            key for looking up the default look in the context of the
     *            current app
     * @param defaultPrototype
     *            if not overridden, this is the type of object to return upon
     *            first call by an application
     * @return the current default (or null) for the calling application
     */
    static HLook getDefaultLookImpl(String prop, Class defaultPrototype)
    {
        Object value = toolkit.getGlobalData(prop);
        HLook look;

        // NULL_LOOK used when explicitly set to null
        if (value == NULL_LOOK)
            look = null;
        // non-null is previously known/set value
        else if (value != null)
            look = (HLook) value;
        // null indicates this is the first time for this app
        else
        {
            // null indicates it has never been set!
            try
            {
                // Create the default look based on system property
                String name = HaviToolkit.getToolkit().getProperty(prop);
                Class lookClass = Class.forName(name);

                if (!defaultPrototype.isAssignableFrom(lookClass))
                    throw new ClassCastException(lookClass + " not an " + defaultPrototype);

                look = (HLook) lookClass.newInstance();
                value = look;
            }
            catch (Exception e)
            {
                // If we could not define it based on the property...
                try
                {
                    look = (HLook) defaultPrototype.newInstance();
                    value = look;
                }
                catch (Exception e2)
                {
                    // Stack is in bad shape -- doesn't use logging because may
                    // not be available
                    e2.printStackTrace();

                    look = null;
                }
            }

            toolkit.setGlobalData(prop, value);
        }

        return look;
    }

    // These size-related methods from java.awt.Component are
    // overridden to provide SIZE_CHANGE support. They simply
    // save off the current size and let the original methods
    // call a ComponentListener which notifies the look with
    // a widgetChanged call.
    public void setSize(Dimension newSize)
    {
        oldSize = getSize();
        super.setSize(newSize);
    }

    public void setSize(int width, int height)
    {
        oldSize = getSize();
        super.setSize(width, height);
    }

    public void setBounds(java.awt.Rectangle newBounds)
    {
        oldSize = getSize();
        super.setBounds(newBounds);
    }

    public void setBounds(int x, int y, int w, int h)
    {
        oldSize = getSize();
        super.setBounds(x, y, w, h);
    }

    /**
     * A <code>ChangeList</code> is a list of one or more
     * <code>HChangeData</code>. This class can be used in place of the standard
     * <code>HChangeData</code> when it's likely (or at least possible) that
     * multiple calls to {@link HLook#widgetChanged} would be made.
     *
     * <pre>
     * ChangeList changes = new ChangeList(0,null); // not filled in yet
     * ...
     * changes.append(doSomethingReturnChangeData());
     * ...
     * getLook().widgetChanged(this, changes.toArray());
     * </pre>
     *
     * This class is package-private because it is intended for use by standard
     * HAVi implementation classes only. At some point, it may make sense for it
     * to be moved elsewhere, but that isn't necessary right now.
     */
    class ChangeList extends HChangeData
    {
        /** Standard constructor. */
        public ChangeList(int hint, Object data)
        {
            super(hint, data);
        }

        /**
         * Appends a <code>ChangeList</code> to the end of this
         * <code>ChangeList</code>. If the given list is <code>null</code>, then
         * no changes are made and the current head of the list is returned.
         *
         * @param list
         *            the <code>ChangeList</code> to append
         * @return the head of the <code>ChangeList</code>
         */
        public ChangeList append(ChangeList list)
        {
            if (list != null)
            {
                // Add list to the end of this list
                last.next = list;
                // Update our last reference
                last = list.last;
                // Update the list size
                size += list.size;
            }
            return this;
        }

        /**
         * Returns the size of this list.
         */
        public int getSize()
        {
            return size;
        }

        /**
         * Converts this list to an array of <code>HChangeData</code> objects.
         *
         * @return an <code>HChangeData[]</code> of length <code> >= 1</code>.
         */
        public HChangeData[] toArray()
        {
            HChangeData[] array = new HChangeData[size];

            int i = 0;
            for (ChangeList list = this; list != null; list = list.next)
            {
                array[i++] = list;
            }
            return array;
        }

        /** The size of this list (at least 1) */
        private int size = 1;

        /** The next element in this list. */
        private ChangeList next;

        /** The last element in this list. */
        private ChangeList last = this;
    }

    /**
     * Array which holds any state dependant animation content.
     */
    private Image[][] animateContent;

    /**
     * Array which holds state dependant misc. content.
     */
    private Object[] content;

    /**
     * Array which holds any state dependant graphical content.
     */
    private Image[] graphicContent;

    /**
     * Array which holds any state dependant textual content.
     */
    private String[] textContent;

    /**
     * The rendering strategy associated with this HVisible.
     */
    private HLook look;

    /**
     * The current interaction state of this HVisible.
     */
    private int state = NORMAL_STATE;

    /**
     * The text layout strategy associated with this HVisible.
     */
    private HTextLayoutManager textLayout = HaviToolkit.getToolkit().getTextLayoutManager();

    /**
     * The current background mode.
     */
    private int backgroundMode = NO_BACKGROUND_FILL;

    /**
     * The current resize mode.
     */
    private int resizeMode = RESIZE_NONE;

    /**
     * The current horizontal alignment.
     */
    private int halign = HALIGN_CENTER;

    /**
     * The current vertical alignment.
     */
    private int valign = VALIGN_CENTER;

    /**
     * The look-specifi private data set.
     */
    private Hashtable lookData = new Hashtable();

    /**
     * The default size.
     */
    private Dimension defaultSize = NO_DEFAULT_SIZE;

    /**
     * Whether this component should have borders drawn or not.
     */
    private boolean hasBorders = true;

    /**
     * The number of states.
     */
    private static final int STATE_COUNT = 8;

    /**
     * The last state index.
     */
    private static final int LAST_INDEX = LAST_STATE & ALL_STATES;

    /**
     * The first state index.
     */
    private static final int FIRST_INDEX = FIRST_STATE & ALL_STATES;

    /**
     * The minimum default width.
     */
    private static final int MIN_DEFAULT_WIDTH = 0;

    /**
     * The minimum default height.
     */
    private static final int MIN_DEFAULT_HEIGHT = 0;

    /**
     * Whether the current look should be notified about "unspecified" changes.
     */
    private static boolean NOTIFY_UNSPECIFIED = false;

    /**
     * The oldSize passed on to HLook.widgetChanged in response to a change in
     * size.
     */
    private Dimension oldSize;
}
