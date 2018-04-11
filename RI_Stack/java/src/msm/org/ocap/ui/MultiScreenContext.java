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

package org.ocap.ui;

import javax.tv.service.selection.ServiceContext;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HScreenRectangle;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.ui.event.MultiScreenConfigurationListener;
import org.ocap.ui.event.MultiScreenContextListener;

/**
 * <p>
 * This interface provides a set of tools for accomplishing the following:
 * </p>
 * 
 * <ol>
 * <li>distinguishing between <code>HScreen</code> instances that are directly
 * associated with a <code>VideoOutputPort</code> and those that are indirectly
 * associated with a <code>VideoOutputPort</code> through a logical mapping
 * process; i.e., discovering whether an <code>HScreen</code> is a display
 * screen or a logical screen;</li>
 * <li>discovering the unique platform identifier of an <code>HScreen</code>
 * instance's underlying resource;</li>
 * <li>discovering the category assigned to an <code>HScreen</code> within its
 * containing <code>MultiScreenConfiguration</code>;</li>
 * <li>discovering the mapping of logical <code>HScreen</code>s to display
 * <code>HScreen</code>s including the area (extent) on the display
 * <code>HScreen</code> where the logical <code>HScreen</code> appears, its
 * visibility, and its z-order;</li>
 * <li>discovering the z-order of <code>HScreenDevice</code>s within an
 * <code>HScreen</code>;</li>
 * <li>discovering the set of <code>ServiceContext</code>s associated with an
 * <code>HScreen</code>;</li>
 * <li>discovering the association of display <code>HScreen</code>s and
 * corresponding <code>VideoOutputPort</code> instances;</li>
 * <li>discovering the set of <code>HScreenDevice</code>s whose generated audio
 * constitute the set of audio sources of an <code>HScreen</code>;</li>
 * <li>discovering the current audio focus assignment of a display
 * <code>HScreen</code>;</li>
 * <li>obtaining notification of changes in state of this
 * <code>MultiScreenContext</code> or certain changes in the the
 * <code>HScreen</code> instance that implements this interface;</li>
 * <li>obtaining notification of changes to the per-display multiscreen
 * configuration of a display screen;</li>
 * <li>discovering the set of per-display multiscreen configurations that may be
 * used with a display screen;</li>
 * <li>obtaining the currently active per-display multiscreen configuration of a
 * display screen;</li>
 * </ol>
 * 
 * <p>
 * If an OCAP implementation does not support the <i>OCAP Multiscreen Manager
 * (MSM) Extension</i> and does not otherwise support this interface, then an
 * OCAP application MAY assume that the behavior of an <code>HScreen</code> is
 * equivalent to an <code>HScreen</code> instance that does implement this
 * interface, <code>MultiScreenContext</code>, whose methods would behave as
 * follows:
 * </p>
 * 
 * <ul>
 * <li><code>getScreenType()</code> returns <code>SCREEN_TYPE_DISPLAY</code>;</li>
 * <li><code>getID()</code> returns a platform dependent (possibly empty)
 * string;</li>
 * <li><code>getScreenCategory()</code> returns the string <code>"main"</code>;</li>
 * <li><code>getVisible()</code> returns <code>true</code>;</li>
 * <li><code>getZOrder()</code> returns <code>0</code>;</li>
 * <li><code>getZOrder(HScreenDevice <i>device</i>)</code> returns the array
 * index of <i>device</i> in an array of <code>HScreenDevice</code> instances
 * created by concatenating the ordered results of invoking on this
 * <code>HScreen</code> instance the methods
 * <code>HScreen.getHBackgroundDevices()</code>, then
 * <code>HScreen.getHVideoDevices()</code>, then
 * <code>HScreen.getHGraphicsDevices()</code>, or throws an
 * <code>IllegalArgumentException</code> in case <i>device</i> is not present in
 * this array;</li>
 * <li><code>getAudioSources()</code> returns an array of
 * <code>HScreenDevice</code> instances created by concatenating the ordered
 * results of invoking on this <code>HScreen</code> instance the methods
 * <code>HScreen.getHBackgroundDevices()</code>, then
 * <code>HScreen.getHVideoDevices()</code>, then
 * <code>HScreen.getHGraphicsDevices()</code>;</li>
 * <li><code>getAudioFocus()</code> returns this <code>HScreen</code> instance;</li>
 * <li><code>getOutputPorts()</code> returns an array containing all
 * <code>VideoOutputPort</code>s available on the platform;</li>
 * <li><code>getDisplayScreen()</code> returns this <code>HScreen</code>
 * instance;</li>
 * <li><code>getDisplayArea()</code> returns
 * <code>new HScreenRectangle(0,0,1,1)</code>;</li>
 * <li><code>getContexts()</code> returns an array containing all
 * <code>ServiceContext</code>s that are accessible by the current application;</li>
 * </ul>
 * 
 * <p>
 * An MSM implementation SHALL support the <code>MultiScreenContext</code>
 * interface on every <code>HScreen</code> instance.
 * </p>
 * 
 * @author Glenn Adams
 * @since MSM I01
 * 
 **/
public interface MultiScreenContext
{
    /**
     * If an <code>HScreen</code> is directly associated with a
     * <code>VideoOutputPort</code> and the extent of the <code>HScreen</code>
     * is mapped to the extent of the video raster produced from the
     * <code>VideoOutputPort</code>, then the type of the <code>HScreen</code>
     * is <code>SCREEN_TYPE_DISPLAY</code>, and is referred to as a display
     * <code>HScreen</code>.
     * 
     * @since MSM I01
     **/
    public static final int SCREEN_TYPE_DISPLAY = 0;

    /**
     * If an <code>HScreen</code> is not directly associated with a
     * <code>VideoOutputPort</code> or the extent of the <code>HScreen</code> is
     * mapped to a sub-region of the video raster produced from some
     * <code>VideoOutputPort</code>, then the type of the <code>HScreen</code>
     * is <code>SCREEN_TYPE_LOGICAL</code>, and is referred to as a logical
     * <code>HScreen</code>. A logical <code>HScreen</code> MAY be associated
     * with a display <code>HScreen</code>. If a logical <code>HScreen</code> is
     * not associated with a display <code>HScreen</code>, then a visible or
     * audible manifestation SHALL NOT be produced by any
     * <code>ServiceContext</code> associated with the logical
     * <code>HScreen</code>.
     * 
     * <p>
     * <em>Note:</em> A logical <code>HScreen</code> that is not associated with
     * a display <code>HScreen</code> may be decoding and using content for some
     * purpose other than presentation, e.g., it may be recording the content
     * from a <code>ServiceContext</code> for future presentation
     * </p>
     * 
     * @since MSM I01
     **/
    public static final int SCREEN_TYPE_LOGICAL = 1;

    /**
     * Obtain the type of this <code>HScreen</code>.
     * 
     * @return An integer value denoted by <code>SCREEN_TYPE_DISPLAY</code> or
     *         <code>SCREEN_TYPE_LOGICAL</code>.
     * 
     * @since MSM I01
     **/
    public int getScreenType();

    /**
     * Obtain a platform dependent unique identifier for the underlying
     * collection of screen resources denoted by this screen, where the scope
     * for uniqueness is no smaller than the set of screens associated with the
     * currently active per-platform multiscreen configuration and all active
     * per-display multiscreen configurations. It is implementation dependent
     * whether the scope for screen identifier uniqueness includes other,
     * non-active multiscreen configurations or not.
     * 
     * <p>
     * A screen identifier SHALL NOT be equal to any screen category returned by
     * <code>getScreenCategory()</code>.
     * </p>
     * 
     * <p>
     * If <code><i>S1</i></code> and <code><i>S2</i></code> are instances of
     * <code>HScreen</code> in the context of the implemented scope of
     * uniqueness and
     * <code>MultiScreenManager.sameResources(<i>S1</i>,<i>S2</i>)</code>
     * returns <code>false</code>, then
     * <code>((MultiScreenContext)<i>S1</i>).getID()</code> and
     * <code>((MultiScreenContext)<i>S2</i>).getID()</code> SHALL NOT return the
     * same (equivalent) string; conversely, if
     * <code>MultiScreenManager.sameResources(<i>S1</i>,<i>S2</i>)</code>
     * returns <code>true</code>, then
     * <code>((MultiScreenContext)<i>S1</i>).getID()</code> and
     * <code>((MultiScreenContext)<i>S2</i>).getID()</code> SHALL return the
     * same (equivalent) string.
     * </p>
     * 
     * @return A string value denoting the collection of underlying resources
     *         this <code>HScreen</code> instance represents in the implemented
     *         scope of uniqueness.
     * 
     * @since MSM I01
     * 
     * @see org.havi.ui.HScreen
     **/
    public String getID();

    /**
     * Obtain the screen category of this <code>HScreen</code> instance.
     * 
     * @return A String that is either (1) an element of the following
     *         enumeration of constants defined by
     *         <code>MultiScreenConfiguration</code>: {
     *         <code>SCREEN_CATEGORY_DISPLAY</code>,
     *         <code>SCREEN_CATEGORY_MAIN</code>,
     *         <code>SCREEN_CATEGORY_PIP</code>,
     *         <code>SCREEN_CATEGORY_POP</code>,
     *         <code>SCREEN_CATEGORY_OVERLAY</code>,
     *         <code>SCREEN_CATEGORY_GENERAL</code> }, or (2) a string value
     *         that denotes a platform-dependent screen category and that starts
     *         with the prefix <code>"x-"</code>.
     * 
     * @since MSM I01
     **/
    public String getScreenCategory();

    /**
     * Obtain screen visibility.
     * 
     * <p>
     * Determine whether this <code>HScreen</code> is marked as visible for
     * presentation on some display <code>HScreen</code>, where "visible" is
     * defined as producing a raster signal to a <code>VideoOutputPort</code>,
     * whether or not the <code>VideoOutputPort</code> is enabled or disabled. A
     * display <code>HScreen</code> SHALL remain marked as visible. A logical
     * <code>HScreen</code> MAY be visible or hidden (not visible).
     * </p>
     * 
     * @return A boolean value indicating whether this <code>HScreen</code> is
     *         marked visible or not on some display <code>HScreen</code>.
     * 
     * @since MSM I01
     **/
    public boolean getVisible();

    /**
     * Obtain screen z-order.
     * 
     * <p>
     * Determine the z-order of this <code>HScreen</code>. An display
     * <code>HScreen</code> SHALL always return a z-order of zero. A logical
     * <code>HScreen</code> MAY be assigned a z-order of 1 or greater, unless it
     * is not associated with a display <code>HScreen</code>, in which case its
     * z-order is -1.
     * </p>
     * 
     * <p>
     * A greater z-order denotes a more front-most (top-most) order among a set
     * of <code>HScreen</code> instances.
     * </p>
     * 
     * @return A value indicating the z-order of this <code>HScreen</code> or
     *         <code>-1</code>. If this <code>HScreen</code> is a logical
     *         <code>HScreen</code> that is not associated with a display
     *         <code>HScreen</code>, then <code>-1</code> SHALL be returned.
     * 
     * @since MSM I01
     **/
    public int getZOrder();

    /**
     * Obtain screen device z-order within this screen.
     * 
     * <p>
     * Determine the z-order of a specified <code>HScreenDevice</code> with an
     * <code>HScreen</code> where the following constraints apply:
     * </p>
     * 
     * <ul>
     * <li>if an <code>HBackgroundDevice</code> is present in this screen, then
     * the z-order of the rear-most (bottom-most) <code>HBackgroundDevice</code>
     * is zero;</li>
     * <li>if no <code>HBackgroundDevice</code> is present in this screen and if
     * an <code>HVideoDevice</code> is present in this screen, then the z-order
     * of the rear-most (bottom-most) <code>HVideoDevice</code> is zero;</li>
     * <li>if no <code>HBackgroundDevice</code> and no <code>HVideoDevice</code>
     * is present in this screen and if an <code>HGraphicsDevice</code> is
     * present in this screen, then the z-order of the rear-most (bottom-most)
     * <code>HGraphicsDevice</code> is zero;</li>
     * <li>the z-order of an <code>HVideoDevice</code> is greater than the
     * z-order of any <code>HBackgroundDevice</code> in this screen;</li>
     * <li>the z-order of an <code>HGraphicsDevice</code> is greater than the
     * z-order of any <code>HVideoDevice</code> in this screen;</li>
     * </ul>
     * 
     * <p>
     * A greater z-order denotes a more front-most (top-most) order among a set
     * of <code>HScreenDevice</code> instances within an <code>HScreen</code>
     * instance.
     * </p>
     * 
     * <p>
     * Each distinct set of underlying screen devices represented as an
     * <code>HScreen</code> instance constitutes a distinct z-ordering; i.e.,
     * given multiple <code>HScreen</code> instances representing distinct
     * underlying screens, the set of z-order values assigned to the underlying
     * screen device resources of these screens may reuse the same z-order
     * indices.
     * </p>
     * 
     * @param device
     *            an <code>HScreenDevice</code> that is associated with this
     *            screen.
     * 
     * @return A non-negative value indicating the z-order of the specified
     *         <code>HScreenDevice</code>.
     * 
     * @throws IllegalArgumentException
     *             if <em>device</em> is not an <code>HScreenDevice</code> of
     *             this screen.
     * 
     * @since MSM I01
     **/
    public int getZOrder(HScreenDevice device) throws IllegalArgumentException;

    /**
     * Obtain audio sources of this screen.
     * 
     * <p>
     * Obtain the set of <code>HScreenDevice</code> from which presented audio
     * is selected (and mixed) for the purpose of audio presentation from this
     * screen.
     * </p>
     * 
     * <p>
     * The default set of audio sources of a screen consists of all
     * <code>HScreenDevice</code> instances association with the screen.
     * </p>
     * 
     * <p>
     * The order of entries in the array returned by this method is not defined
     * by this specification and SHALL be considered implementation dependent.
     * </p>
     * 
     * @return A reference to an (possibly empty) array of
     *         <code>HScreenDevice</code> instances, where each such instance
     *         contributes to a mixed, audio presentation from this screen, or,
     *         if this screen does not support mixed audio, then at most one
     *         entry will be present in the returned array.
     * 
     * @since MSM I01
     **/
    public HScreenDevice[] getAudioSources();

    /**
     * Obtain the audio focus screen.
     * 
     * <p>
     * The audio focus screen of this <code>HScreen</code> is determined
     * according to the following ordered rules, where the first rule that
     * applies is used and others are ignored:
     * </p>
     * 
     * <ol>
     * <li>If this <code>HScreen</code> is a logical screen, then apply the
     * following ordered sub-rules:
     * <ol type="a">
     * <li>If this logical <code>HScreen</code> is mapped to a display screen,
     * then apply the following sub-rules:
     * <ol type="i">
     * <li>If this logical <code>HScreen</code> is currently assigned audio
     * focus in the context of its display screen, then this logical
     * <code>HScreen</code> is returned.</li>
     * <li>Otherwise (not currently assigned audio focus in its display screen),
     * <code>null</code> is returned.</li>
     * </ol>
     * </li>
     * <li>Otherwise (not mapped to a display screen), if this logical
     * <code>HScreen</code> is directly mapped to a video output port, then this
     * <code>HScreen</code> is returned.</li>
     * <li>Otherwise (not mapped to a display screen and not directly mapped to
     * a video output port), <code>null</code> is returned.</li>
     * </ol>
     * </li>
     * <li>Otherwise (this <code>HScreen</code> is a display screen), apply the
     * following sub-rules:
     * <ol type="a">
     * <li>If some logical screen that is mapped to this display screen is
     * assigned audio focus, then that logical <code>HScreen</code> is returned;
     * </li>
     * <li>Otherwise (no logical screen is mapped to this display screen or no
     * logical screen mapped to this display screen is assigned audio focus),
     * then return this display <code>HScreen</code>.</li>
     * </ol>
     * </li>
     * </ol>
     * 
     * <p>
     * The audio focus screen of a display screen is the screen whose currently
     * selected audio sources are assigned to be rendered on all (implied) audio
     * presentation devices of all video output ports to which the display
     * screen is mapped.
     * </p>
     * 
     * @return an <code>HScreen</code> instance or <code>null</code> as
     *         described above.
     * 
     * @since MSM I01
     * 
     * @see MultiScreenConfigurableContext#assignAudioFocus
     **/
    public HScreen getAudioFocus();

    /**
     * Obtain video ports to which screen is mapped.
     * 
     * <p>
     * Obtain the set of <code>VideoOutputPort</code>s associated with this
     * <code>HScreen</code>. If this <code>HScreen</code>'s type is
     * <code>SCREEN_TYPE_DISPLAY</code>, then the <code>VideoOutputPort</code>
     * instances associated with this display screen SHALL be returned. If this
     * <code>HScreen</code>'s type is <code>SCREEN_TYPE_LOGICAL</code> and this
     * <code>HScreen</code> is associated with a display <code>HScreen</code>,
     * then the <code>VideoOutputPort</code> instances associated with that
     * display <code>HScreen</code> SHALL be returned. If this
     * <code>HScreen</code>'s type is <code>SCREEN_TYPE_LOGICAL</code> and this
     * <code>HScreen</code> is not associated with a display
     * <code>HScreen</code>, then an empty array SHALL be returned.
     * </p>
     * 
     * @return A reference to an array of <code>VideoOutputPort</code>
     *         instances. If the returned array is empty, then this
     *         <code>HScreen</code> is not associated with any
     *         <code>VideoOutputPort</code>.
     * 
     * @since MSM I01
     **/
    public VideoOutputPort[] getOutputPorts();

    /**
     * Obtain display screen with which this screen is associated.
     * 
     * <p>
     * Obtain the display <code>HScreen</code> associated with this
     * <code>HScreen</code>. If this <code>HScreen</code>'s type is
     * <code>SCREEN_TYPE_DISPLAY</code>, then a reference to this
     * <code>HScreen</code> SHALL be returned. If this <code>HScreen</code>'s
     * type is <code>SCREEN_TYPE_LOGICAL</code> and this <code>HScreen</code> is
     * associated with a display <code>HScreen</code>, then that display
     * <code>HScreen</code> SHALL be returned. If this <code>HScreen</code>'s
     * type is <code>SCREEN_TYPE_LOGICAL</code> and this <code>HScreen</code> is
     * not associated with a display <code>HScreen</code>, then the value
     * <code>null</code> SHALL be returned.
     * </p>
     * 
     * @return A reference to a display <code>HScreen</code> instance or
     *         <code>null</code>. If <code>null</code>, then this
     *         <code>HScreen</code> is not associated with a display
     *         <code>HScreen</code>.
     * 
     * @since MSM I01
     **/
    public HScreen getDisplayScreen();

    /**
     * Obtain area of the display screen to which this screen is mapped.
     * 
     * <p>
     * Obtain the area (extent) of this <code>HScreen</code>. If this
     * <code>HScreen</code>'s type is <code>SCREEN_TYPE_DISPLAY</code>, then an
     * <code>HScreenRectangle</code> whose value is equal to
     * <code>HScreenRectangle(0,0,1,1)</code> SHALL be returned. If this
     * <code>HScreen</code>'s type is <code>SCREEN_TYPE_LOGICAL</code> and this
     * <code>HScreen</code> is associated with a display <code>HScreen</code>,
     * then the area (extent) occupied by this logical <code>HScreen</code> on
     * its associated display <code>HScreen</code> SHALL be returned. If this
     * <code>HScreen</code>'s type is <code>SCREEN_TYPE_LOGICAL</code> and this
     * <code>HScreen</code> is not associated with a display
     * <code>HScreen</code>, then the value <code>null</code> SHALL be returned.
     * </p>
     * 
     * @return A reference to an <code>HScreenRectangle</code> instance or
     *         <code>null</code>. If <code>null</code>, then this
     *         <code>HScreen</code> is not associated with a display
     *         <code>HScreen</code>.
     * 
     * @since MSM I01
     **/
    public HScreenRectangle getDisplayArea();

    /**
     * Obtain service contexts associated with this screen.
     * 
     * <p>
     * Obtain the set of <code>ServiceContext</code>s associated with this
     * <code>HScreen</code> to which the calling application is granted access.
     * </p>
     * 
     * @return A reference to an array of <code>ServiceContext</code> instances.
     *         If the returned array is empty, then this <code>HScreen</code> is
     *         not associated with any accessible <code>ServiceContext</code>.
     * 
     * @since MSM I01
     **/
    public ServiceContext[] getServiceContexts();

    /**
     * Add screen context listener.
     * 
     * <p>
     * Add a listener to be notified upon the occurence of screen context
     * events. If a listener has previously been added and not subsequently
     * removed, then an attempt to add it again SHALL NOT produce a side effect.
     * </p>
     * 
     * @param listener
     *            a <code>MultiScreenContextListener</code> instance.
     * 
     * @since MSM I01
     **/
    public void addScreenContextListener(MultiScreenContextListener listener);

    /**
     * Remove screen context listener.
     * 
     * <p>
     * Remove a listener previously addede to be notified upon the occurence of
     * screen context events. If the specified listener is not currently
     * registered as a listener, then an attempt to remove it SHALL NOT produce
     * a side effect.
     * </p>
     * 
     * @param listener
     *            a <code>MultiScreenContextListener</code> instance.
     * 
     * @since MSM I01
     **/
    public void removeScreenContextListener(MultiScreenContextListener listener);

    /**
     * Add a listener to be notified upon the occurence of multiscreen
     * configuration events that apply to this screen in the case it is a
     * display screen. If a listener has previously been added and not
     * subsequently removed, then an attempt to add it again SHALL NOT produce a
     * side effect.
     * 
     * <p>
     * Configuration events that apply to a display screen SHALL be restricted
     * to those that affect the complement of logical screens associated with
     * the display screen.
     * </p>
     * 
     * <p>
     * If an event defined by <code>MultiScreenConfigurationEvent</code> is
     * generated, then the MSM implementation SHALL notify each registered
     * screen configuration listener accordingly.
     * </p>
     * 
     * @param listener
     *            a <code>MultiScreenConfigurationListener</code> instance.
     * 
     * @throws IllegalStateException
     *             if the type of this screen is not
     *             <code>SCREEN_TYPE_DISPLAY</code>.
     * 
     * @since MSM I01
     * 
     * @see org.ocap.ui.event.MultiScreenConfigurationEvent
     * @see org.ocap.ui.event.MultiScreenConfigurationListener
     **/
    public void addMultiScreenConfigurationListener(MultiScreenConfigurationListener listener)
            throws IllegalStateException;

    /**
     * Remove a listener previously added to be notified upon the occurence of
     * multiscreen configuration events. If the specified listener is not
     * currently registered as a listener, then an attempt to remove it SHALL
     * NOT produce a side effect.
     * 
     * @param listener
     *            a <code>MultiScreenConfigurationListener</code> instance.
     * 
     * @since MSM I01
     * 
     * @see org.ocap.ui.event.MultiScreenConfigurationListener
     **/
    public void removeMultiScreenConfigurationListener(MultiScreenConfigurationListener listener);

    /**
     * Obtain set of all per-display multiscreen configurations currently
     * associated with this display screen where the configuration type of any
     * such multiscreen configuration SHALL NOT be
     * <code>SCREEN_CONFIGURATION_DISPLAY</code>).
     * 
     * @return A non-empty array of <code>MultiScreenConfiguration</code>
     *         instances.
     * 
     * @throws SecurityException
     *             if the calling thread has not been granted
     *             <code>MonitorAppPermission("multiscreen.configuration")</code>
     *             .
     * 
     * @since MSM I01
     * 
     * @see MultiScreenConfiguration
     * @see org.ocap.system.MonitorAppPermission
     **/
    public MultiScreenConfiguration[] getMultiScreenConfigurations() throws SecurityException;

    /**
     * Obtain per-display multiscreen configurations of a specific type
     * associated with this display screen.
     * 
     * @param screenConfigurationType
     *            (1) an element of the following enumeration of constants
     *            defined by <code>MultiScreenConfiguration</code>: {
     *            <code>SCREEN_CONFIGURATION_NON_PIP</code>,
     *            <code>SCREEN_CONFIGURATION_PIP</code>,
     *            <code>SCREEN_CONFIGURATION_POP</code>,
     *            <code>SCREEN_CONFIGURATION_GENERAL</code> , or (2) some other
     *            platform-dependent value not pre-defined as a multiscreen
     *            configuration type.
     * 
     * @return An array of <code>MultiScreenConfiguration</code> instances or
     *         <code>null</code>, depending on whether specified configuration
     *         type is supported or not.
     * 
     * @throws SecurityException
     *             if the calling thread has not been granted
     *             <code>MonitorAppPermission("multiscreen.configuration")</code>
     *             .
     * 
     * @throws IllegalArgumentException
     *             if <code><i>screenConfigurationType</i></code> is
     *             <code>SCREEN_CONFIGURATION_DISPLAY</code>, is not defined, or
     *             is otherwise unknown to the platform implemention.
     * 
     * @since MSM I01
     * 
     * @see MultiScreenConfiguration
     * @see org.ocap.system.MonitorAppPermission
     **/
    public MultiScreenConfiguration[] getMultiScreenConfigurations(String screenConfigurationType)
            throws SecurityException, IllegalArgumentException;

    /**
     * Obtain the currently active per-display multiscreen configuration for
     * this display screen.
     * 
     * @return The currently active per-display
     *         <code>MultiScreenConfiguration</code> instance that applies to
     *         this display screen.
     * 
     * @throws IllegalStateException
     *             if this <code>HScreen</code> is not a display screen.
     * 
     * @since MSM I01
     * 
     * @see MultiScreenConfiguration
     **/
    public MultiScreenConfiguration getMultiScreenConfiguration() throws IllegalStateException;

}
