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

package org.cablelabs.impl.media.player;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Vector;

import javax.media.Time;
import javax.tv.graphics.AlphaColor;
import javax.tv.media.AWTVideoSize;
import javax.tv.media.AWTVideoSizeControl;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.content.video.dvb.mpeg.drip.Handler;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.dvb.media.ActiveFormatDescriptionChangedEvent;
import org.dvb.media.AspectRatioChangedEvent;
import org.dvb.media.BackgroundVideoPresentationControl;
import org.dvb.media.DFCChangedEvent;
import org.dvb.media.StopByResourceLossEvent;
import org.dvb.media.VideoFormatEvent;
import org.dvb.media.VideoFormatListener;
import org.dvb.media.VideoPresentationControl;
import org.dvb.media.VideoTransformation;
import org.dvb.ui.DVBAlphaComposite;
import org.dvb.ui.DVBGraphics;
import org.dvb.ui.UnsupportedDrawingOperationException;
import org.havi.ui.HEventMulticaster;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScene;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoComponent;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;
import org.havi.ui.event.HScreenLocationModifiedEvent;
import org.havi.ui.event.HScreenLocationModifiedListener;
import org.ocap.media.ClosedCaptioningEvent;
import org.ocap.media.ClosedCaptioningListener;
import org.ocap.media.VideoComponentControl;
import org.ocap.media.VideoFormatControl;
import org.ocap.media.S3DSignalingChangedEvent;
import org.ocap.media.S3DConfiguration;

import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.mpe.ScalingBoundsDfc;
import org.cablelabs.impl.media.mpe.ScalingCaps;
import org.cablelabs.impl.media.presentation.VideoPresentation;
import org.cablelabs.impl.media.presentation.VideoPresentationContext;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.CallerContextEventMulticaster;

/**
 * {@link AbstractVideoPlayer} is a framework abstract base class for Players
 * that use the {@link org.cablelabs.impl.havi.port.mpe.HDVideoDevice} to
 * decode. Access to the HDVideoDevice is provided via the
 * {@link org.cablelabs.impl.media.player.VideoDevice} interace.
 * {@link AbstractVideoPlayer} extends {@link AbstractPlayer} and has two
 * subclasses: {@link AbstractServicePlayer} and {@link Handler}.
 * VideoController handles the video device resource management aspects of a
 * Player. Only a VideoController can be returned by the
 * {@link org.havi.ui.HVideoDevice#getVideoController()} method; HDVideoDevice
 * maintains a reference to the controlling Player via the VideoController base
 * class.
 * 
 * @author schoonma
 */
public abstract class AbstractVideoPlayer extends AbstractPlayer implements AVPlayer, VideoPresentationContext
{
    /**
     * logging
     */
    private static final Logger log = Logger.getLogger(AbstractVideoPlayer.class);

    /*
     * MediaAPI
     */

    private MediaAPI mediaAPI;

    public MediaAPI getMediaAPI()
    {
        if (mediaAPI == null)
        {
            mediaAPI = (MediaAPI) ManagerManager.getInstance(MediaAPIManager.class);
        }

        return mediaAPI;
    }

    /*
     * Construction
     */

    protected AbstractVideoPlayer(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage)
    {
        super(cc, lock, resourceUsage);
        addControls(avControls);
    }

    /*
     * 
     * VideoDevice
     */

    /**
     * This is the {@link org.cablelabs.impl.havi.port.mpe.HDVideoDevice} that
     * is being controlled by the {@link AVPlayer}.
     */
    protected VideoDevice videoDevice = null;

    /**
     * Indicates whether the HVideoDevice is currently reserved. It is possible
     * to not have the reservation but still be operational.
     */
    private boolean videoIsReserved = false;

    public VideoDevice getVideoDevice()
    {
        synchronized (getLock())
        {
            return videoDevice;
        }
    }

    /**
     * @return Returns <code>true</code> if the video output window should be
     *         shown. For a background presentation, this should always return
     *         true. For a component presentation, this depends on the
     *         visibility of the component containing the video.
     */
    protected boolean showVideo()
    {
        // Background player is always shown.
        // Component player is only shown if componentVideoShown flag is set to
        // true.
        return !isComponentPlayer || componentVideoShown;
    }

    public void setVideoDevice(VideoDevice vd)
    {
        synchronized (getLock())
        {
            videoDevice = vd;
        }
    }

    public void loseVideoDeviceControl()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "loseVideoDeviceControl()");
        }

        synchronized (getLock())
        {
            if (!isClosed())
            {
                stop(new StopByResourceLossEvent(this, Started, Realized, Realized, getSource().getLocator()));
            }
        }
    }

    private class VideoDeviceResourceClient implements ResourceClient
    {
        // Be greedy. Don't give it up without a fight!
        public boolean requestRelease(ResourceProxy proxy, Object requestData)
        {
            return false;
        }

        // We are about to lose it. Don't do anything until it is actually lost.
        public void release(ResourceProxy proxy)
        {
        }

        // VideoDevice reservation has been lost.
        public void notifyRelease(ResourceProxy proxy)
        {
            synchronized (getLock())
            {
                // Set flag indicating lost reservation and let base class know
                // that a resource has been lost so that it can send the
                // ResourceWithdrawnEvent if necessary.
                videoIsReserved = false;
                lostResource();
            }
        }
    }

    private VideoDeviceResourceClient videoDeviceClient = new VideoDeviceResourceClient();

    /**
     * This method may be defined by subclasses to indicate whether a background
     * video device should be preferred when acquiring the video device.
     * 
     * @return Returns true if the background device should be preferred;
     *         otherwise, false.
     */
    protected abstract boolean preferBackgroundDevice();

    /**
     * @return Returns a list of video devices, prioritized by whether a
     *         background device is preferred and filtered by whether this is a
     *         component player.
     */
    private Vector getPrioritizedVideoDevices()
    {
        // For a component player, return only devices that can be used as
        // component devices.
        if (isComponentPlayer)
        {
            return getComponentVideoDevices();
        }

        Vector devices = new Vector();
        if (preferBackgroundDevice())
        {
            devices.addAll(getBackgroundVideoDevices());
            devices.addAll(getComponentVideoDevices());
        }
        else
        {
            devices.addAll(getComponentVideoDevices());
            devices.addAll(getBackgroundVideoDevices());
        }
        return devices;
    }

    /**
     * This implementation of acquires the {@link VideoDevice} to be used by the
     * {@link AVPlayer}. If the video device could not be acquired, then this
     * method calls {@link AbstractPlayer#completePrefetching(Object)} with a
     * failure reason string.
     * <p/>
     * On completion of this method, the following fields are initialized:
     * <ul>
     * <li>{@link #videoDevice} &mdash; initialized to the {@link VideoDevice}
     * that is to be used for decoding</li>
     * <li>{@link #videoIsReserved} &mdash; set to true if the video device was
     * reserved; otherwise, false.</li>
     * </ul>
     *
     */
    protected Object doAcquirePrefetchResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doAcquirePrefetchResources");
        }

        setVideoDevice(null);
        videoIsReserved = false;

        // Get the list of candidate video devices appropriate for the player
        // type--i.e.,
        // component vs background, service context vs application created

        Vector allVds = getPrioritizedVideoDevices();
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "examining prioritized video devices: " + allVds);
        }

        while (videoDevice == null)
        {

            // From this set, separate out the ones that are either:
            // a. not controlled, or
            // b. controlled by a lower-priority application than the
            // application that own this player.

            Vector uncontrolledVds = new Vector(); // uncontrolled video devices
            Vector controllableVds = new Vector(); // controlled by
                                                   // lower-priority-app video
                                                   // devices

            for (Enumeration e = allVds.elements(); e.hasMoreElements();)
            {
                VideoDevice vd = (VideoDevice) e.nextElement();
                AbstractVideoPlayer owner = (AbstractVideoPlayer) vd.getController();
                // not controlled
                if (owner == null)
                {
                    uncontrolledVds.add(vd);
                }
                // controlled by a same- or lower-priority app (or we are presenting EAS)
                else if (getResourceUsage().isResourceUsageEAS() || getOwnerPriority() > owner.getOwnerPriority())
                {
                    controllableVds.add(vd);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "not examining video device controlled by app with higher priority - this app priority: "
                                + getOwnerPriority()
                                + ", other app priority:"
                                + owner.getOwnerPriority()
                                + " - "
                                + vd
                                + ", other app: " + owner);
                    }
                }
            }

            // If all devices are controlled by higher-priority applications
            // (indicated by
            // both the uncontrolled and controlled-by-lower-priority-app lists
            // being empty),
            // return an error message string (which will generate a
            // ResourceUnavailableEvent
            // on the player).

            if (uncontrolledVds.size() == 0 && controllableVds.size() == 0)
            {
                return "no controllable video device";
            }

            // If there are uncontrolled video devices, they comprise the target
            // list;
            // otherwise, the controlled-by-lower-priority-app devices comprise
            // the
            // target list.

            Vector targetVds = uncontrolledVds.size() > 0 ? uncontrolledVds : controllableVds;

            // Iterate through the target list, looking for one that can be
            // reserved and
            // controlled (and that is contributing).

            boolean retry = false;
            for (Enumeration e = targetVds.elements(); e.hasMoreElements();)
            {
                VideoDevice vd = (VideoDevice) e.nextElement();
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "Attempting to reserve and control device: " + vd);
                }

                int status = vd.reserveAndControlDevice(this, getResourceUsage(), videoDeviceClient);

                // If successful, assign videoDevice and videoIsReserved fields.
                if (status == VideoDevice.CONTROL_SUCCESS)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "successfully reserved and controlled device: " + vd);
                    }
                    setVideoDevice(vd);
                    videoIsReserved = true;
                    break;
                }
                // If insufficient priority, break out of loop because something
                // has changed.
                else if (status == VideoDevice.INSUFFICIENT_PRIORITY)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "insufficient priority - unable to reserve and control video device: " + vd);
                    }
                    retry = true;
                    break;
                }
                // If Player's CallerContext has been deleted, then exit with
                // failure.
                else if (status == VideoDevice.NO_CALLER_CONTEXT)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "orphaned player - unable to reserve and control video device: " + vd);
                    }
                    return "orphaned player cannot reserve video device";
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "no reservation or bad configuration - unable to reserve and control video device: " + vd);
                    }
                }
                // In other cases (no reservation or non-contributing
                // configuration), proceed to next one.
            }

            // If retry flag was set, restart the process.
            if (retry)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "retry was set to true - restarting the process");
                }
                continue;
            }

            // If we get here without a video device, it is because one could
            // not be controlled
            // and reserved above. So do the next best thing: Try to control the
            // video device
            // without getting a reservation.
            if (videoDevice == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "failed to reserve and control a video device - attempting to control without reservation");
                }
                // If no video device was reservable and usable, try to use them
                // without
                // reserving but still trying to ensure a contributing
                // configuration.
                // Abort if the priority is not sufficient.
                for (Enumeration e = targetVds.elements(); e.hasMoreElements();)
                {
                    VideoDevice vd = (VideoDevice) e.nextElement();
                    int status = vd.controlVideoDevice(this, getResourceUsage());

                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "controlVideoDevice result: " + status);
                    }
                    // If successful, assign videoDevice field and break out of
                    // loop.
                    // Otherwise, try the next one.
                    if (status == VideoDevice.CONTROL_SUCCESS)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "controlVideoDevice returned SUCCESS - setting video device to: " + vd);
                        }
                        setVideoDevice(vd);
                        break;
                    }
                }
                // If we get out of the above loop and no video device was
                // assigned,
                // start the process all over again.
            }
        }

        // If we get here, then a video device was found. Return null to
        // indicate success.
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "Using video device " + videoDevice);
        }
        return null;
    }

    protected void doReleasePrefetchedResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doReleasePrefetchedResources");
        }

        if (videoDevice != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "video device was not null - relinquishing video device");
            }
            videoDevice.relinquishVideoDevice(this);
            if (videoIsReserved)
            {
                videoDevice.releaseDevice(getOwnerCallerContext());
            }
            videoIsReserved = false;
            setVideoDevice(null);
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "video device relinquished");
            }
        }
    }

    public void notifyNotContributing()
    {
        synchronized (getLock())
        {
            // Don't do anything if stopped.
            if (isClosed() || isStopped())
            {
                return;
            }

            // Don't do anything if there is no video device.
            if (videoDevice == null)
            {
                return;
            }

            // Stop the player.
            stop(new StopByResourceLossEvent(this, Started, Realized, Realized, getSource().getLocator()));
        }
    }

    protected void doReleaseAllResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doReleaseAllResources");
        }
        if (videoComponent != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "releasing videoComponent");
            }
            videoComponent.release();
            videoComponent = null;
        }
    }

    /*
     * 
     * Component Video
     */

    /*
     * Capabilities
     */

    /**
     * This is the list of video devices that support scaling.
     */
    private static Vector componentVideoDevs = null;

    /**
     * This is the list of video devices that do <em>not</em> support scaling.
     */
    private static Vector backgroundVideoDevs = null;

    private void sortVideoDevices()
    {
        // Devices supporting component video will be added to this vector.
        componentVideoDevs = new Vector();
        backgroundVideoDevs = new Vector();

        // Iterate through all of the video devices, and sort them into scaling
        // vs static.
        HScreen hs = getDefaultScreen();
        HVideoDevice[] vds = hs.getHVideoDevices();
        if (vds != null)
        {
            // Iterate through video devices checking for component support.
            for (int i = 0; i < vds.length; ++i)
            {
                VideoDevice vd = (VideoDevice) vds[i];
                if (getMediaAPI().supportsComponentVideo(vd.getHandle()))
                {
                    componentVideoDevs.addElement(vd);
                }
                else
                {
                    backgroundVideoDevs.addElement(vd);
                }
            }
        }
    }

    /**
     * @return Returns an array of {@link VideoDevice} instances that support
     *         video scaling.
     */
    private Vector getComponentVideoDevices()
    {
        // Initialized on the first call.
        if (componentVideoDevs == null)
        {
            sortVideoDevices();
        }
        return componentVideoDevs;
    }

    /**
     * @return Returns the list of video devices that don't support scaling.
     */
    private Vector getBackgroundVideoDevices()
    {
        if (componentVideoDevs == null)
        {
            sortVideoDevices();
        }
        return backgroundVideoDevs;
    }

    /**
     * @return Returns true if component video is supported on at least one
     *         video device.
     */
    private boolean componentVideoIsSupported()
    {
        return !getComponentVideoDevices().isEmpty();
    }

    /*
     * VideoComponent
     */

    /*
     * VideoComponent
     */

    public void notifyMediaPresented()
    {
        super.notifyMediaPresented();

        // If this is a component-based Player, then repaint the component.
        if (isComponentPlayer)
        {
            getActiveVideoComponent().repaint();
        }
    }

    /**
     * Implementation-specific implementation of {@link HVideoComponent}.
     * Instances of {@link VideoComponent} will be returned by JMF
     * {@link Player}s via the
     * {@link VideoComponentControl#getVisualComponent()} method.
     * 
     * @author Aaron Kamienski
     * @author Michael Schoonover (converted to inner class & refactored for
     *         Decoder/Player merge)
     * @see javax.media.Player
     * @see HVideoComponent
     */
    class VideoComponent extends HVideoComponent
    {
        VideoComponent()
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "create VideoComponent");
            }
        }

        /*
         * HVideoComponent Overrides
         */

        public HVideoDevice getVideoDevice()
        {
            synchronized (getLock())
            {
                return (HVideoDevice) AbstractVideoPlayer.this.getVideoDevice();
            }
        }

        /*
         * Cleanup
         */

        /**
         * This method is called to force the cleanup of any resources held by
         * the video component. It removes any listeners that may have been
         * registered with the component and removes the component from its
         * container.
         */
        private void release()
        {
            // Remove location listeners that are registered for this component.
            teardownLocationListener();

            // Remove the Container from its parent, if there is one.
            Container parent = getParent();
            if (parent != null)
            {
                parent.remove(this);
            }

            // Clear out the listeners list.
            listeners = null;
        }

        /*
         * Event Notification
         */

        /**
         * The on-screen location of this component the last time listeners were
         * notified. This is used to determine if listeners should be notified.
         * <p/>
         * This field is volatile and synchronization when accessing it is
         * explicitly avoided.
         */
        private volatile Point lastNotifiedLocation;

        /**
         * The set of <code>HScreenLocationModifiedListener</code>s. These will
         * be invoked within this component's <code>CallerContext</code> only.
         */
        private HScreenLocationModifiedListener listeners;

        protected void setupListeners()
        {
            teardownLocationListener();
            setupLocationListener();
        }

        /**
         * Adds a listener to be notified when the absolute location (relative
         * to the <code>HGraphicsDevice</code>) changes.
         * 
         * @see org.havi.ui.HVideoComponent#addOnScreenLocationModifiedListener
         */
        public void addOnScreenLocationModifiedListener(HScreenLocationModifiedListener l)
        {
            synchronized (getLock())
            {
                if (listeners == null && l != null)
                {
                    setupLocationListener();
                }

                listeners = HEventMulticaster.add(listeners, l);
            }
        }

        /**
         * Removes a listener added with
         * {@link #addOnScreenLocationModifiedListener}.
         * 
         * @see org.havi.ui.HVideoComponent#removeOnScreenLocationModifiedListener
         */
        public void removeOnScreenLocationModifiedListener(HScreenLocationModifiedListener l)
        {
            synchronized (getLock())
            {
                Object old = listeners;

                listeners = HEventMulticaster.remove(listeners, l);

                if (old != null && listeners == null)
                {
                    teardownLocationListener();
                }
            }
        }

        /**
         * Notify listeners of change to the on-screen location of this
         * component.
         * 
         * @param loc
         *            the new absolute on-screen location of this component
         */
        private void notifyLocationModifiedListeners(Point loc)
        {
            HScreenLocationModifiedListener l = listeners;

            // Determine if the location is different
            if (l != null && !loc.equals(lastNotifiedLocation))
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "notify " + lastNotifiedLocation + "->" + loc);
                }

                // Save the new location
                lastNotifiedLocation = loc;

                // Notify listeners
                listeners.report(new HScreenLocationModifiedEvent(this));

            }
        }

        /**
         * Keeps track of all {@link Component} on which
         * {@link ComponentListener} has been installed.
         */
        private Vector listeningComponents;

        /**
         * Add {@link ComponentListener}s to this component and parent
         * components so that changes can be discovered.
         * 
         * @see #teardownLocationListener
         */
        protected void setupLocationListener()
        {
            // Listen to changes in location/size of self and any/all parent
            // components
            // This won't catch changes that result because of addition of
            // component
            // to another parent. (E.g., if it hadn't been added already.)
            listeningComponents = new Vector();
            for (Component c = this; c != null; c = c.getParent())
            {
                // Remove (so we don't add twice).
                c.removeComponentListener(componentListener);
                // Add listener.
                c.addComponentListener(componentListener);
                listeningComponents.addElement(c);

                // Don't invoke HScene.getParent()
                if (c instanceof HScene)
                {
                    break;
                }
            }

            // !!!!TODO!!!!!
            // Should we also add container listeners to parents? To determine
            // if this component or a parent is removed from the hierarchy?
        }

        /**
         * Remove previously added component listeners.
         * 
         * @see #setupLocationListener
         */
        private void teardownLocationListener()
        {
            // Stop listening for changes in location/size of self and parents
            Vector v = listeningComponents;
            listeningComponents = null;

            if (v != null)
            {
                for (Enumeration e = v.elements(); e.hasMoreElements();)
                {
                    Component c = (Component) e.nextElement();
                    c.removeComponentListener(componentListener);
                }
            }
        }

        /**
         * Handle cases where the component video has moved on screen or been
         * resized. Move happens not only when this component is moved, but when
         * a parent is moved.
         */
        class VideoComponentListener implements ComponentListener
        {
            public void componentMoved(ComponentEvent e)
            {
                synchronized (getLock())
                {
                    // If this or any ancestor moved, notify
                    // HScreenLocationModifiedListeners.
                    notifyLocationModifiedListeners(getLocationOnScreen());
                }
            }

            public void componentResized(ComponentEvent e)
            {
                synchronized (getLock())
                {
                    // Only update if the event is for *this* component. (or if
                    // its the parent)
                    if (isParent(e.getComponent(), videoComponent))
                    {
                        updateComponentVideoBounds();
                    }
                }
            }

            public void componentHidden(ComponentEvent e)
            {
                synchronized (getLock())
                {
                    // If component or its parent is hidden, hide the component
                    // video.
                    // But only do this if the component is the current
                    // component. Or if its the parent
                    if (isParent(e.getComponent(), videoComponent))
                    {
                        hideComponentVideo();
                    }
                }
            }

            public void componentShown(ComponentEvent e)
            {
                // No-op since this is taken care of by the paint() method.
            }

            private boolean isParent(Component com, Component child)
            {
                boolean retval = false;

                if (com == child)
                {
                    retval = true;
                }
                else if (com instanceof Container)
                {
                    Component[] coms = ((Container) com).getComponents();
                    for (int i = 0; i < coms.length && !retval; i++)
                    {
                        if (isParent(coms[i], child))
                        {
                            retval = true;
                        }
                    }
                }
                return retval;
            }
        }

        VideoComponentListener componentListener = new VideoComponentListener();

        /**
         * Override {@link Component#paint(java.awt.Graphics)} to show the
         * background video or fill using the {@link #getBackground() background
         * color}, depending on whether the component is active or not.
         * <p/>
         * <p/>
         * Logically, component video is rendered in the graphics plane.
         * However, in the actual physical layering, the scaled video plane is
         * behind the graphics plane. Therefore, to simulate rendering to the
         * graphics plane, special techniques are used. Invoking the
         * {@link Clock#syncStart(Time)} method will result in the
         * scaled video output, which is behind the graphics plane, being
         * positioned and sized to correspond with this component. The video
         * stream is made visible by rendering this component with a
         * {@link Color} that is transparent through to the video
         * plane.
         * 
         * @see AlphaColor
         */
        public void paint(Graphics g)
        {
            // Don't let anything change state.
            synchronized (getLock())
            {
                // Don't let anything change component tree/layout state.
                synchronized (getTreeLock())
                {
                    // If the player hasn't yet been converted to a component
                    // player, convert it now.
                    // Conversion happens the first time paint() is called for
                    // any VideoComponent
                    // associated with the Player.
                    if (isBackgroundPlayer())
                    {
                        // Add ComponentListeners for this and all parents in
                        // Component hierarchy.
                        setupListeners();
                        completeConversion(true);
                    }

                    // Get current size and background color.
                    Dimension size = getSize();
                    Color bg = getBackground();

                    // If this video component is active and video is currently
                    // decoding, then
                    // set up for transparent punch-thru filling, and scale the
                    // video.
                    if (this == getActiveVideoComponent() && isPresenting())
                    {
                        // Set the color to punch through to video (using SRC
                        // mode).
                        bg = new AlphaColor(bg.getRed(), bg.getGreen(), bg.getBlue(), 0);
                        if (g instanceof DVBGraphics)
                        {
                            try
                            {
                                ((DVBGraphics) g).setDVBComposite(DVBAlphaComposite.Src);
                            }
                            catch (org.dvb.ui.UnsupportedDrawingOperationException e)
                            {
                                //ignore
                            }
                        }

                        // Set component video bounds to the closest match to
                        // component's location/size,
                        // and make sure the component video is visible.
                        updateComponentVideoBounds();
                        showComponentVideo();
                    }

                    if (bg != null && bg.getAlpha() == 0 && isPresenting())
                    {
                        // Fill the component.
                        g.setColor(bg);
                        g.fillRect(0, 0, size.width, size.height);
                    }
                }
            }
        }
    }

    /**
     * This is a reference to the 'active' {@link VideoComponent}. Only the
     * 'active' {@link VideoComponent} controls the rendering of scaled
     * component video. On the active {@link VideoComponent}, the
     * {@link Component#paint(Graphics)} method 'punches through' the graphics
     * plane to the background video plane by painting the exposed area of the
     * component with the transparent color. On an inactive component,
     * {@link Component#paint(Graphics)} paints the exposed area in the
     * component's background color.
     * <p/>
     * When first constructed, a {@link VideoComponent} is active. A
     * {@link VideoComponent} is deactivated by the implementation by setting
     * videoComponent to null. Deactivation occurs in these situations:
     * <ul>
     * <li>The associated {@link AVPlayer} becomes closed.</li>
     * <li>A new {@link VideoComponent} is constructed to replace the currently
     * active one.</li>
     * </ul>
     */
    protected volatile VideoComponent videoComponent = null;

    /**
     * @return Returns the current "active" video component.
     * 
     * @see #videoComponent
     */
    protected VideoComponent getActiveVideoComponent()
    {
        return videoComponent;
    }

    // Always return null since component is obtained via VideoComponentControl.
    protected Component doGetVisualComponent()
    {
        return null;
    }

    /**
     * This boolean indicates whether the player has been converted to a
     * component video player. Initially it is false. Getting a video component
     * for the player is not sufficient to convert it into a component player.
     * Not until the component is painted does the player convert into a
     * component player. It can also convert into a component player on a swap
     * that is performed by the scaled video manager.
     */
    private boolean isComponentPlayer = false;

    /**
     * Indicates whether the player has been converted to a component player.
     * Conversion to component player doesn't happen until
     * {@link #completeConversion(boolean)} is called with a value of
     * <code>true</code>. This can happen as the result of either
     * <ul>
     * <li>the {@link VideoComponent} being painted, or</li>
     * <li>swapping background and component video via the SVM (scaled video
     * manager).</li>
     * </ul>
     */
    public boolean isComponentPlayer()
    {
        synchronized (getLock())
        {
            return isComponentPlayer;
        }
    }

    /**
     * @return The converse of {@link #isComponentPlayer()}.
     */
    protected boolean isBackgroundPlayer()
    {
        return !isComponentPlayer();
    }

    /**
     * Convert the player to either a background or component player.
     * 
     * @param isCompPlayer
     *            If true, convert the player to a component player; otherwise,
     *            change it to background player.
     */
    protected void completeConversion(boolean isCompPlayer)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "completeConversion(isCompPlayer=" + isCompPlayer + ")");
        }

        // Do nothing if closed.
        if (isClosed())
        {
            return;
        }

        // Disable background-only controls.
        awtVidSizeCtrl.setEnabled(!isCompPlayer);
        bgVidPresCtrl.setEnabled(!isCompPlayer);

        // Enable foreground-only controls.
        vidPresCtrl.setEnabled(isCompPlayer);

        // Set field indicating that it is a component player.
        isComponentPlayer = isCompPlayer;
    }

    /**
     * The last VideoTransformation size assigned.
     */
    protected VideoTransformation cachedVidTrans;

    /**
     * The last AWTVideoSize assigned.
     */
    protected AWTVideoSize cachedAWTSize;

    private void setSize(VideoTransformation vt)
    {
        this.cachedAWTSize = null;
        this.cachedVidTrans = vt;
    }

    private void setSize(AWTVideoSize sz)
    {
        this.cachedAWTSize = sz;
        this.cachedVidTrans = null;
    }

    /*
     * Component Video Manipulation
     */

    /**
     * Keeps track of the current component video bounds,
     */
    protected Rectangle componentVideoBounds = null;

    protected Rectangle componentClipRegion = null;

    /**
     * Indicates whether component video is currently being shown; it can be
     * hidden if the associated {@link VideoComponent} is hidden.
     */
    protected boolean componentVideoShown = false;

    /**
     * Update the bounds of the scaled video underlying the
     * {@link VideoComponent}'s visible area.
     */
    private void updateComponentVideoBounds()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "updateComponentVideoBounds()");
        }

        if (videoComponent == null || !isPresenting())
        {
            return;
        }

        // Compute the best fit for the current component size.
        Dimension size = videoComponent.getSize();
        Rectangle checkRect = new Rectangle(videoComponent.getLocationOnScreen(), size);
        Rectangle bestFitRect = getBestFit(videoComponent.getParent(), checkRect);

        // If the best-fit size is no different than current size, then just
        // return.
        if (componentVideoBounds != null && bestFitRect.equals(componentVideoBounds))
        {
            return;
        }

        // Set the scaled video bounds and cache the bounds if successful.
        Rectangle newComponentVideoBounds = new Rectangle(bestFitRect);
        if (setScaledVideoBounds(newComponentVideoBounds))
        {
            componentVideoBounds = newComponentVideoBounds;
        }
    }

    /**
     * Hide component video output by setting output rectangle to {0,0,0,0}.
     */
    private void hideComponentVideo()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "hideComponentVideo()");
        }

        if (componentVideoShown)
        {
            getVideoPresentation().hide();
            componentVideoShown = false;
        }
    }

    /**
     * Show component video at the current component video bounds. If not set,
     * throw assertion.
     */
    private void showComponentVideo()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "showComponentVideo()");
        }

        if (!componentVideoShown)
        {
            if (Asserting.ASSERTING)
            {
                Assert.preCondition(componentVideoBounds != null);
            }

            componentVideoShown = getVideoPresentation().show();
        }
    }

    /**
     * Helper method to set video bounds, based on component rectangle. If the
     * player is presenting, then it sets it on the presentation; otherwise, it
     * caches the size for when the player starts presenting again.
     * 
     * @param dest
     *            dest rectangle
     * @return Returns <code>true</code> if the component video size was
     *         successfully assigned; otherwise, returns <code>false</code>.
     */
    private boolean setScaledVideoBounds(Rectangle dest)
    {
        HScreenRectangle hsrc;
        HVideoConfiguration vcfg = getVideoDevice().getCurrentConfiguration();
        HGraphicsConfiguration gcfg = getDefaultGraphicsConfiguration();

        if (log.isDebugEnabled())
        {
            log.debug(getId() + "setScaledVideoBounds(" + dest + ")");
        }

        if (Asserting.ASSERTING)
        {
            Assert.preCondition(isPresenting());
            Assert.condition(dest != null);
        }

        // If no clipping region assigned, assign clipping region to full-screen
        // video.
        if (componentClipRegion == null)
        {
            componentClipRegion = new Rectangle(new Point(0, 0), vcfg.getPixelResolution());
        }

        // Next take steps to create the scaling bounds.
        // Note: vcfg is used for source and gcfg for dest.
        hsrc = Util.toHScreenRectangle(vcfg, componentClipRegion);
        HScreenRectangle hdest = Util.toHScreenRectangle(gcfg, dest);

        ScalingBounds sb = new ScalingBounds(hsrc, hdest);
        return getVideoPresentation().setBounds(sb);
    }

    /*
     * 
     * Controls
     */

    private AWTVideoSizeControlImpl awtVidSizeCtrl = new AWTVideoSizeControlImpl(true);

    private VideoPresentationControlImpl vidPresCtrl = new VideoPresentationControlImpl(false);

    private BackgroundVideoPresentationControlImpl bgVidPresCtrl = new BackgroundVideoPresentationControlImpl(true);

    private VideoFormatControlImpl vidFmtCtrl = new VideoFormatControlImpl(true);

    protected VideoComponentControlImpl vidCompCtrl = new VideoComponentControlImpl(componentVideoIsSupported());

    private ControlBase[] avControls = { awtVidSizeCtrl, vidPresCtrl, bgVidPresCtrl, vidCompCtrl, vidFmtCtrl, };

    /*
     * VideoComponentControl
     */

    protected class VideoComponentControlImpl extends ControlBase implements VideoComponentControl
    {
        VideoComponentControlImpl(boolean enabled)
        {
            super(enabled);
        }

        /**
         * This indicates whether {@link #getVisualComponent()} should return a
         * new component or just return the one that is cached on the player.
         */
        private boolean returnCachedComponent = true;

        public void returnCachedComponent(boolean b)
        {
            returnCachedComponent = b;
        }

        public HVideoComponent getVisualComponent()
        {
            synchronized (getLock())
            {
                if (returnCachedComponent)
                {
                    if (videoComponent == null) // there is no cached component,
                                                // so cache one
                    {
                        videoComponent = new VideoComponent();
                    }
                    // After first call, always return new component.
                    returnCachedComponent = false;
                    return videoComponent;
                }

                // Create new VideoComponent and assign as the active component.
                return videoComponent = new VideoComponent();
            }
        }

        public Rectangle getBestFit(Container parent, Rectangle desired) throws IllegalArgumentException
        {
            synchronized (getLock())
            {
                return AbstractVideoPlayer.this.getBestFit(parent, desired);
            }
        }
    }

    private Rectangle getBestFit(Container parent, Rectangle desired)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getBestFit(parent=" + parent + ", desired=" + desired + ")");
        }

        // If control is disabled, return input rectangle (may not be usable).
        if (isClosed())
        {
            return desired;
        }

        Dimension pdim = parent.getSize(); // Get parent component dimension.
        Point ploc = parent.getLocationOnScreen(); // Get its graphics
                                                   // coordinate location.

        // Make sure the location is completely within the parent bounds.
        if ((desired.x < 0 || desired.y < 0) || (desired.x + desired.width > pdim.width)
                || (desired.y + desired.height > pdim.height))
        {
            throw new IllegalArgumentException("desired location is not within the parent container");
        }

        // Now get the graphics coordinates for the desired output rectangle.
        Rectangle dst = new Rectangle(ploc.x + desired.x, ploc.y + desired.y, desired.width, desired.height);

        // Now use default source and desired destination for AWTVideoSize
        // instance to perform actual check with decoder.
        HGraphicsConfiguration vidCfg = getDefaultGraphicsConfiguration();
        AWTVideoSize sz = new AWTVideoSize(new Rectangle(new Point(0, 0), vidCfg.getPixelResolution()), dst);

        // HVideoConfiguration vidCfg = videoDevice.getCurrentConfiguration();
        // Rectangle src = new Rectangle(new Point(0,0),
        // vidCfg.getPixelResolution());
        AWTVideoSize bestFit = checkAWTSize(sz);
        dst = bestFit.getDestination();

        // Make sure the destination is valid relative to the parent.
        if ((dst.x < 0 || dst.y < 0) || (dst.x + dst.width > pdim.width) || (dst.y + dst.height > pdim.height))
        {
            throw new IllegalArgumentException("desired location is not within the parent container");
        }

        // Return valid result destination rectangle.
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getBestFit() returning " + dst);
        }
        return dst;
    }

    /*
     * AWTVideoSizeControl
     */

    private class AWTVideoSizeControlImpl extends ControlBase implements AWTVideoSizeControl
    {
        AWTVideoSizeControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public AWTVideoSize getSize()
        {
            synchronized (getLock())
            {

                return isEnabled() ? getAWTSize() : null;
            }
        }

        public AWTVideoSize getDefaultSize()
        {
            synchronized (getLock())
            {
                return isEnabled() ? getDefaultAWTSize(videoDevice) : null;
            }
        }

        /*
         * The source video size is returned in AWT screen coordinates. The
         * input video size (after ETR upsampling) is reported using AWT screen
         * coordinates.
         */
        public Dimension getSourceVideoSize()
        {
            synchronized (getLock())
            {
                if (!isPresenting())
                {
                    return null;
                }

                // The input video size in device dimension is converted to
                // graphics coordinates.
                HVideoConfiguration src = getVideoDevice().getCurrentConfiguration();
                HGraphicsConfiguration dst = getDefaultGraphicsConfiguration();
                Rectangle inRect;

                Dimension dim = null;
                if (src != null && dst != null)
                {
                    inRect = new Rectangle(getVideoPresentation().getInputSize());
                    Rectangle rect = Util.toRectangle(src, dst, inRect);
                    dim = new Dimension(rect.width, rect.height);
                }
                return dim;
            }
        }

        public boolean setSize(AWTVideoSize sz)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setSize(" + sz + ")");
            }

            synchronized (getLock())
            {
                // The TCK tests explicity check that an NPE is thrown in
                // this case
                if (sz == null)
                {
                    throw new NullPointerException("null AWTVideoSize");
                }

                // Fail if control is disabled.
                if (!isEnabled())
                {
                    return false;
                }

                // Fail is requested size is not supported.
                if (!checkAWTSize(sz).equals(sz))
                {
                    return false;
                }

                // If presenting, attempt to set the bounds.
                if (isPresenting())
                {
                    // Convert size to scaling bounds.
                    ScalingBounds sb = convertToScalingBounds(sz);
                    if (sb == null)
                    {
                        return false;
                    }

                    if (!getVideoPresentation().setBounds(sb))
                    {
                        return false;
                    }
                }

                // Cache the size.
                AbstractVideoPlayer.this.setSize(sz);
                return true;
            }
        }

        public AWTVideoSize checkSize(AWTVideoSize sz)
        {
            synchronized (getLock())
            {
                return isEnabled() ? checkAWTSize(sz) : null;
            }
        }
    }

    /*
     * VideoPresentationControl
     */

    private class VideoPresentationControlImpl extends ControlBase implements VideoPresentationControl
    {
        VideoPresentationControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public Dimension getInputVideoSize()
        {
            synchronized (getLock())
            {
                return isEnabled() ? AbstractVideoPlayer.this.getInputVideoSize() : null;
            }
        }

        public Dimension getVideoSize()
        {
            synchronized (getLock())
            {
                HScreenRectangle hr = getActiveVideoAreaOnScreen();
                HVideoConfiguration vcfg = getVideoDevice().getCurrentConfiguration();
                Rectangle r = Util.toRectangle(vcfg, hr);
                return isEnabled() ? r.getSize() : null;
            }
        }

        /*
         * getActiveVideoArea - This routine returns the video area minus any
         * letterboxing or pillarboxing that is known by the platform. Known
         * boxing is determined either by active Display Format Control (DFC)
         * processing or by Active Format Descriptor (AFD) signalled info. DFC
         * processing specifies boxing in the output (destination) and AFD
         * signalling specifies boxing in the input (source).
         * 
         * The DFC and AFD boxing areas are not included in the final area
         * returned to the client.
         */
        public HScreenRectangle getActiveVideoArea()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();
                if (!isPresenting() || vd == null)
                {
                    return null;
                }

                // convert the current dfc to a scaling bounds to factor any
                // DFC processing. This excludes any "bars" introduced as a
                // result of video filtering (bars that the platform knows
                // about)
                int dfc = getMediaAPI().getDFC(vd.getHandle());
                ScalingBounds sb = Util.toScalingBounds(dfc, getMediaAPI().getBounds(vd.getHandle()));

                // Compute initial activeVideoArea using the resulting scaling
                // bounds. The initial activeVideoArea will include the entire
                // video input (including any "bars" in the broadcast stream).
                // Some portion of this area can be offscreen.
                HScreenRectangle activeVideoArea = Util.toVideoArea(sb, new HScreenRectangle(0, 0, 1, 1));

                // Next factor in the AFD to determine if input bars need to be
                // removed from the area to compute active video area only.
                // This excludes any "bars" included in the broadcast stream
                // that
                // are signalled by an AFD (bars that the platform knows about).
                int afd = getMediaAPI().getActiveFormatDefinition(vd.getHandle());
                int aspectRatio = getMediaAPI().getAspectRatio(vd.getHandle());
                HScreenRectangle afdClipRegion = Util.toClipRegion(afd, aspectRatio);

                // convert the computer afd clip region to a video area
                HScreenRectangle afdVideoArea = Util.toVideoArea(sb, afdClipRegion);

                // combine the two areas (afd and dfc) by taking their
                // intersection
                HScreenRectangle result = Util.intersectRect(afdVideoArea, activeVideoArea);

                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getActiveVideoArea intersect of afd: " + afdVideoArea + ", activeVideoArea: "
                            + activeVideoArea + ", result: " + result);
                }

                return result;
            }
        }

        /*
         * getActiveVideoAreaOnScreen - This routine behaves identically to
         * getActiveVideoArea with one exception. The area is clipped to the
         * screen so that any video "offscreen" is not included.
         */
        public HScreenRectangle getActiveVideoAreaOnScreen()
        {
            synchronized (getLock())
            {
                // start with the full active video area
                HScreenRectangle activeVideoArea = getActiveVideoArea();

                // clip the full active video area to the screen
                HScreenRectangle activeVideoAreaOnScreen = null;
                if (activeVideoArea != null)
                {
                    HScreenRectangle boundingRectangle = new HScreenRectangle(0, 0, 1, 1);
                    activeVideoAreaOnScreen = Util.clipToRect(activeVideoArea, boundingRectangle);
                }

                return activeVideoAreaOnScreen;
            }
        }

        /*
         * getTotalVideoArea - This routine returns the video area including any
         * letterboxing or pillarboxing that sginalled by an AFD. Known boxing
         * introduced by Display Format Control (DFC) or video processing is
         * excluded. See excludeVideoFilteringBars routine.
         * 
         * The AFD signalled areas are included in the output by default and as
         * such, this routine simply skips any detection of AFD info.
         */
        public HScreenRectangle getTotalVideoArea()
        {
            synchronized (getLock())
            {
                // Get destination rectangle
                // Destination represents the tentative active video area
                // because
                // AFD is not factored.
                VideoDevice vd = getVideoDevice();
                if (!isPresenting() || vd == null)
                {
                    return null;
                }

                // convert the current dfc to a scaling bounds to factor any
                // DFC processing. This excludes any "bars" introduced as a
                // result of video filtering (bars that the platform knows
                // about)
                // TotalVideoArea excludes letterboxing and
                // pillarboxing introduced by video filtering but includes
                // letterboxing and pillarboxing that are signalled by AFD
                int dfc = getMediaAPI().getDFC(vd.getHandle());
                ScalingBounds sb = Util.toScalingBounds(dfc, getMediaAPI().getBounds(vd.getHandle()));

                // convert the scaling bounds to a screen area i
                HScreenRectangle totalVideoArea = Util.toVideoArea(sb, new HScreenRectangle(0, 0, 1, 1));

                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getTotalVideoArea() final result = " + totalVideoArea);
                }

                return totalVideoArea;
            }
        }

        /*
         * getTotalVideoAreaOnScreen - This routine behaves identically to
         * getTotalVideoArea with one exception. The area is clipped to the
         * screen so that any video "offscreen" is not included.
         */
        public HScreenRectangle getTotalVideoAreaOnScreen()
        {
            synchronized (getLock())
            {
                // start with the full total video area
                HScreenRectangle totalVideoArea = getTotalVideoArea();

                // clip the full total video area to the screen
                HScreenRectangle totalVideoAreaOnScreen = null;
                if (totalVideoArea != null)
                {
                    HScreenRectangle boundingRectangle = new HScreenRectangle(0, 0, 1, 1);
                    totalVideoAreaOnScreen = Util.clipToRect(totalVideoArea, boundingRectangle);
                }

                return totalVideoAreaOnScreen;
            }
        }

        public boolean supportsClipping()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();

                return (isEnabled() && vd != null) && getMediaAPI().supportsClipping(vd.getHandle());
            }
        }

        /**
         * The following description is additional to the desciption provided by
         * the specification. This method will not reposition the destination
         * rectangle on the screen. The destination rectangle is already
         * defined. The only thing that changes is source rectangle which
         * specifies the clipping region.
         * 
         * @see VideoPresentationControl#setClipRegion(Rectangle)
         */
        public Rectangle setClipRegion(Rectangle clipRect)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setClipRegion(" + clipRect + ")");
            }

            if (clipRect == null)
            {
                throw new NullPointerException();
            }

            synchronized (getLock())
            {
                // If there is no video device, then there is no way to
                // determine
                // whether the clipping rectangle would be supported.
                if (videoDevice == null)
                {
                    return null;
                }

                // Get the current video bounds (source & destination).
                MediaAPI media = getMediaAPI();
                int vd = videoDevice.getHandle();

                // if the platform does not support clipping, then we must
                // return
                // the full input video as the clipping region
                if (!media.supportsClipping(vd))
                {
                    Rectangle fullRect = new Rectangle(new Point(0, 0), getVideoSize());
                    if (log.isInfoEnabled())
                    {
                        log.info(getId() + "clipping not supported - returning: " + fullRect);
                    }
                    return fullRect;
                }

                ScalingBounds bounds = media.getBounds(vd);

                // Convert the clipRect to an HScreenRectangle and store into
                // the scaling bounds.
                HVideoConfiguration vcfg = videoDevice.getCurrentConfiguration();

                // set the clipRect, but keep the location and scaling the same
                // the easiest way to do this is to revert to a video transform and set the cliprect there
                VideoTransformation videoTransform = Util.toVideoTransformation(vcfg, bounds);
                videoTransform.setClipRegion(clipRect);
                bounds = Util.toScalingBounds(vcfg, videoTransform);

                // Get the closest match for the bounds, using the clipRect as
                // source.
                ScalingBounds closest = media.checkBounds(vd, bounds);

                // If the closest are the same as the requested bounds,
                // then return the input clipRect.
                if (closest.equals(bounds))
                {
                    componentClipRegion = clipRect;
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "requested clip rectangle is supported");
                    }
                }
                // If the closest bounds do NOT match the requested, then create
                // new Rectangle
                // from the src.
                else
                {
                    componentClipRegion = Util.toRectangle(vcfg, closest.src);
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "actual clipping rectangle assigned: " + componentClipRegion);
                    }
                }

                // Assign the closest bounds at the native layer.
                getMediaAPI().setBounds(vd, closest);

                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "setClipRegion - returning: " + componentClipRegion);
                }
                // Return the clipping region in video coordinates.
                return componentClipRegion;
            }
        }

        public Rectangle getClipRegion()
        {
            synchronized (getLock())
            {
                if (isEnabled() && videoDevice != null)
                {
                    if (componentClipRegion == null)
                    {
                        // Initialize the clip region to full screen.
                        HVideoConfiguration vcfg = videoDevice.getCurrentConfiguration();
                        componentClipRegion = new Rectangle(new Point(0, 0), vcfg.getPixelResolution());
                    }
                    return componentClipRegion;
                }
                return null;
            }
        }

        public float[] supportsArbitraryHorizontalScaling()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();

                return (isPresenting() && vd != null) ? getMediaAPI().getScalingCaps(vd.getHandle())
                        .getContinuousHorizRange() : null;
            }
        }

        public float[] supportsArbitraryVerticalScaling()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();

                return (isPresenting() && vd != null) ? getMediaAPI().getScalingCaps(vd.getHandle())
                        .getContinuousVertRange() : null;
            }
        }

        public float[] getHorizontalScalingFactors()
        {
            // synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();

                if (!isPresenting() || vd == null)
                {
                    return null;
                }

                ScalingCaps sc = getMediaAPI().getScalingCaps(vd.getHandle());

                float[] factors = sc.getDiscreteHFactors();

                    if (factors != null)
                    {
                        for (int i = 0; i < factors.length; i++)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(getId() + "AVP:getHorizontalScalingFactors - factors[" + i + "] = " + factors[i]);
                            }
                        }
                }

                return factors;
            }
        }

        public float[] getVerticalScalingFactors()
        {
            // synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();

                if (!isPresenting() || vd == null)
                {
                    return null;
                }

                ScalingCaps sc = getMediaAPI().getScalingCaps(vd.getHandle());

                float[] factors = sc.getDiscreteVFactors();

                    if (factors != null)
                    {
                        for (int i = 0; i < factors.length; i++)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(getId() + "AVP:getVerticalScalingFacttors - factors[" + i + "] = " + factors[i]);
                            }
                        }
                }

                return factors;
            }
        }

        public byte getPositioningCapability()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();

                if (isEnabled() && vd != null)
                {
                    return getMediaAPI().getPositioningCapability(vd.getHandle());
                }

                return POS_CAP_OTHER;
            }
        }

    } // VideoPresentationControlImpl

    /*
     * BackgroundVideoPresentationControl
     */

    private class BackgroundVideoPresentationControlImpl extends VideoPresentationControlImpl implements
            BackgroundVideoPresentationControl
    {
        BackgroundVideoPresentationControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public boolean setVideoTransformation(VideoTransformation vt)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setVideoTransformation(" + vt + ")");
            }

            synchronized (getLock())
            {
                if (isClosed())
                {
                    //player is closed - return false
                    return false;
                }
                // If we got here, then try to re-reserve the video device.
                // MHP 11.4.2.8.4: If a player does not have the video device
                // reserved,
                // it shall attempt to reserve it if the app that created the
                // player
                // calls setVideoTransformation().
                if (!videoIsReserved && getVideoDevice() != null && ccMgr != null
                        && getVideoDevice().getClient() != videoDeviceClient
                        && ccMgr.getCurrentContext() == getOwnerCallerContext())
                {
                    // make the reservation attempt
                    videoIsReserved = getVideoDevice().reserveDevice(getResourceUsage(), videoDeviceClient,
                            getOwnerCallerContext());

                    // MHP 11.4.2.8:
                    // Some of the decoder format controls pre-defined as part
                    // of
                    // org.dvb.media.VideoFormatControl may require control over
                    // the pixel aspect ratio of the final video output signal
                    // after
                    // video, graphics and backgrounds have been combined as
                    // shown in
                    // figure 17 on page 198. In order to enable this, a JMF
                    // player shall
                    // attempt to reserve the HVideoDevice instance on which its
                    // output is
                    // being displayed. Failure to reserve the HVideoDevice
                    // shall not be
                    // considered fatal to the JMF player but may result in an
                    // inferior
                    // TV video presentation.

                    // Since a minimal set of transformations impact pixel
                    // asspect ratio
                    // and there is no straight forward mechanism to detect such
                    // transformations.... just log the failed reservation
                    // attempt and continue
                    if (!videoIsReserved)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "setVideoTransformation failed to reserve the video device");
                        }
                    }
                }

                if (vt == null)
                {
                    throw new NullPointerException("null VideoTransformation");
                }

                // Fail if control is disabled.
                if (!isEnabled())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "setVideoTransformation: Control not enabled");
                    }
                    return false;
                }

                // Fail if size is not supported (i.e., requested transformation
                // doesn't match the closest one).
                VideoTransformation closest = getClosestMatch(vt);
                if (closest == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "setVideoTransformation closest match is null");
                    }
                    return false;
                }

                HVideoConfiguration vc = isPresenting() ? getVideoDevice().getCurrentConfiguration()
                        : getDefaultVideoDevice().getDefaultConfiguration();
                if (!Util.equals(vc, vt, closest))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "setVideoTransformation closest match not equal");
                    }
                    return false;
                }

                // Convert VT to scaling bounds. Fail if conversion fails.
                ScalingBounds sb = convertToScalingBounds(vt);
                if (sb == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "setVideoTransformation scaling bounds conversion failed");
                    }
                    return false;
                }
                // If presenting, set size at native layer. Also DFC, if
                // applicable.
                // If not presenting, skip this step.
                if (isPresenting())
                {
                    if (vt instanceof VideoTransformationDfc)
                    {
                        sb = new ScalingBoundsDfc(sb, ((VideoTransformationDfc) vt).getDfc());
                    }
                    if (!getVideoPresentation().setBounds(sb))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "setVideoTransformation setbounds failed");
                        }
                        return false;
                    }
                }

                // Cache the size.
                setSize(vt);

                return true;
            }
        }

        public VideoTransformation getVideoTransformation()
        {
            synchronized (getLock())
            {
                if (!isEnabled())
                {
                    return null;
                }

                if (!isPresenting())
                {
                    return new VideoTransformation();
                }

                ScalingBounds sb = getVideoPresentation().getBounds();
                if (Asserting.ASSERTING)
                {
                    Assert.condition(sb != null);
                }

                return convertToVideoTransformation(sb);
            }
        }

        public VideoTransformation getClosestMatch(VideoTransformation vt)
        {
            synchronized (getLock())
            {
                if (!isEnabled())
                {
                    return null;
                }

                if (vt == null)
                {
                    throw new NullPointerException("null VideoTransformation");
                }

                if (vt instanceof VideoTransformationDfc
                        && ((VideoTransformationDfc) vt).getDfc() != org.dvb.media.VideoFormatControl.DFC_PROCESSING_NONE)
                {
                    return vt;
                }

                if (isPresenting())
                {
                    // Convert transformation to scaling bounds specification.
                    // If unsuccessful (null returned), return default value.
                    ScalingBounds sb = convertToScalingBounds(vt);
                    if (sb == null)
                    {
                        return new VideoTransformation();
                    }

                    // Call native to check size.
                    ScalingBounds closest = checkBounds(sb);

                    // From JavaDoc for getClosestMatch
                    // Returns: the closest match to the input video
                    // transformation.
                    // If the input video transformation is supported, then the
                    // input
                    // video transformation will be returned (the same
                    // instance),
                    // otherwise a newly created instance will be returned
                    if (closest.equals(sb))
                    {
                        return vt;
                    }
                    else
                    {
                        return convertToVideoTransformation(closest);
                    }
                }
                else
                {
                    return new VideoTransformation();
                }
            }
        }
    } // BackgroundVideoPresentationControlImpl

    /*
     * 
     * Video Methods These methods are used to implement the various
     * video-related controls.
     */

    public void notifyActiveFormatChanged(int newAF)
    {
        vidFmtCtrl.notifyActiveFormatChanged(newAF);
    }

    public void notifyAspectRatioChanged(int newAR)
    {
        vidFmtCtrl.notifyAspectRatioChanged(newAR);
    }

    public void notifyDecoderFormatConversionChanged(int newDFC)
    {
        vidFmtCtrl.notifyDecoderFormatConversionChanged(newDFC);
    }

    public void notify3DFormatChanged(int s3dTransitionType)
    {
        vidFmtCtrl.notify3DFormatChanged(s3dTransitionType);
    }

    public boolean isPlatformMode()
    {
        synchronized (getLock())
        {
            VideoDevice vd = getVideoDevice();
            if (vd == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "isPlatform() - null video device");
                }
                return false;
            }

            return getMediaAPI().isPlatformDFC(getVideoDevice().getHandle());
        }
    }

    private static class VideoFormatControlEventDispatcher extends CallerContextEventMulticaster
    {
        public void dispatch(EventListener listeners, EventObject event)
        {
            if (listeners instanceof VideoFormatListener && event instanceof VideoFormatEvent)
            {
                ((VideoFormatListener) listeners).receiveVideoFormatEvent((VideoFormatEvent) event);
            }
            else if (Asserting.ASSERTING)
            {
                Assert.condition(false, "listener or event of incorrect type");
            }
        }
    }

    private class VideoFormatControlImpl extends ControlBase implements VideoFormatControl
    {
        VideoFormatControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public S3DConfiguration getS3DConfiguration()
        {
            getScanMode();

            VideoDevice vd = getVideoDevice();
            if (vd == null || isClosed() || !isEnabled())
            {
                return null;
            }

            return doGetS3DConfiguration(vd.getHandle());
        }

        public int getScanMode()
        {
            VideoDevice vd = getVideoDevice();
            if (vd == null || isClosed() || !isEnabled())
            {
                return VideoFormatControl.SCANMODE_UNKNOWN;
            }

            int scanMode = doGetInputVideoScanMode(vd.getHandle());

            return scanMode;
        }

        protected void release()
        {
            multicaster.cleanup();
        }

        /**
         * Return the aspect ratio of the video as it is transmitted. If the
         * aspect ratio is not known, ASPECT_RATIO_UNKNOWN is returned
         * 
         * @return the aspect ratio of the video
         */
        public int getAspectRatio()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();
                if (vd == null || isClosed() || !isEnabled() || !isPresenting())
                {
                    return ASPECT_RATIO_UNKNOWN;
                }

                return getMediaAPI().getAspectRatio(vd.getHandle());
            }
        }

        /**
         * Return the value of the active_format field of the MPEG Active Format
         * Description of the video if it is transmitted (one of the constants
         * AFD_* above). If this field is not available then AFD_NOT_PRESENT is
         * returned. The constant values for the constants representing the
         * Active Format Description should be identical to the values specified
         * in ETR154, annex B.
         * 
         * @return the value of the active_format field of the MPEG Active
         *         Format Description of the video if it is transmitted. If this
         *         field is not available, or the video is not MPEG, then
         *         AFD_NOT_PRESENT is returned.
         */
        public int getActiveFormatDefinition()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();
                if (vd == null || isClosed() || !isEnabled() || !isPresenting())
                {
                    return AFD_NOT_PRESENT;
                }

                return getMediaAPI().getActiveFormatDefinition(vd.getHandle());
            }
        }

        /**
         * Return a value representing what format conversion is being done by
         * the decoder in the platform (one of the constants DFC_* above). A
         * receiver may implement only a subset of the available options. This
         * decoder format conversion may be active or not depending upon the
         * mode of operation.
         * 
         * @return the decoder format conversion being performed or
         *         DFC_PROCESSING_UNKNOWN if this is not known
         */
        public int getDecoderFormatConversion()
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();
                if (vd == null || isClosed() || !isEnabled())
                {
                    return DFC_PROCESSING_UNKNOWN;
                }

                return getMediaAPI().getDFC(vd.getHandle());
            }
        }

        /**
         * This method returns a VideoTransformation object that corresponds
         * with the specified Decoder Format Conversion when applied to the
         * currently selected video. If the specified Decoder Format Conversion
         * is not supported for the currently selected video, then this method
         * returns null.
         * 
         * @param dfc
         *            the Decoder Format Conversion (one of the DFC_* constants
         *            specified in this interface)
         * 
         * @return the video transformation, or null if the specified Decoder
         *         Format Conversion is not supported for the currently selected
         *         video.
         */
        public VideoTransformation getVideoTransformation(int dfc)
        {
            synchronized (getLock())
            {
                VideoDevice vd = getVideoDevice();
                if (vd == null || isClosed() || !isEnabled())
                {
                    return null;
                }

                if (getMediaAPI().checkDFC(vd.getHandle(), dfc))
                {
                    ScalingBounds sb = Util.toScalingBounds(dfc, getMediaAPI().getBounds(vd.getHandle()));
                    return new VideoTransformationDfc(dfc, convertToVideoTransformation(sb));
                }
                else
                {
                    return null;
                }
            }
        }

        /**
         * Return the aspect ratio of the display device connected to this MHP
         * decoder (one of the constants DAR_* above)
         * 
         * @return the aspect ratio of the display device connected to the
         *         decoder
         */
        public int getDisplayAspectRatio()
        {
            synchronized (getLock())
            {
                if (isClosed() || !isEnabled())
                {
                    return -1;
                }

                return getDAR();
            }
        }

        /**
         * Test if control over the decoder format conversions is being managed
         * by the platform as defined by <code>DFC_PLATFORM</code>.
         * 
         * @return true if control over the decoder format conversions is being
         *         managed by the platform, false otherwise
         * 
         * @see #DFC_PLATFORM
         */
        public boolean isPlatform()
        {
            return isPlatformMode();
        }

        private void notifyActiveFormatChanged(int newAF)
        {
            ActiveFormatDescriptionChangedEvent afdEventObj = new ActiveFormatDescriptionChangedEvent(this, newAF);
            postEvent(afdEventObj);
        }

        private void notifyAspectRatioChanged(int newAR)
        {
            AspectRatioChangedEvent arEventObj = new AspectRatioChangedEvent(this, newAR);
            postEvent(arEventObj);
        }

        private void notifyDecoderFormatConversionChanged(int newDFC)
        {
            DFCChangedEvent dfcEventObj = new DFCChangedEvent(this, newDFC);
            postEvent(dfcEventObj);
        }

        private void notify3DFormatChanged(int s3dTransitionType)
        {
            S3DConfiguration config = getS3DConfiguration();
            S3DSignalingChangedEvent S3DEventObj = new S3DSignalingChangedEvent(AbstractVideoPlayer.this, s3dTransitionType, config);
            postEvent(S3DEventObj);
        }

        /*
         * Event Dispatching
         */

        /**
         * per-CallerContext EventMulticaster
         */
        private CallerContextEventMulticaster multicaster = new VideoFormatControlEventDispatcher();

        public final void addVideoFormatListener(VideoFormatListener listener)
        {
            synchronized (getLock())
            {
                if (isClosed())
                {
                    return;
                }

                multicaster.addListenerMulti(listener);
            }
        }

        public final void removeVideoFormatListener(VideoFormatListener listener)
        {
            synchronized (getLock())
            {
                if (isClosed())
                {
                    return;
                }

                multicaster.removeListener(listener);
            }
        }

        /**
         * Post the specified {@link ClosedCaptioningEvent} to all registered
         * {@link ClosedCaptioningListener}s.
         * 
         * @param event
         *            {@link ClosedCaptioningEvent} to be posted.
         */
        private void postEvent(VideoFormatEvent event)
        {
            multicaster.multicast(event);
        }
    }

    //override in subclasses as needed
    protected int doGetInputVideoScanMode(int handle)
    {
       return getMediaAPI().getInputVideoScanMode(handle);
    }

    protected S3DConfiguration doGetS3DConfiguration(int handle)
    {
        return getMediaAPI().getS3DConfiguration(handle);
    }

    private ScalingBounds checkBounds(ScalingBounds sb)
    {
        return getMediaAPI().checkBounds(getVideoDevice().getHandle(), sb);
    }

    private VideoPresentation getVideoPresentation()
    {
        return (VideoPresentation) presentation;
    }

    private int getDAR()
    {
        synchronized (getLock())
        {
            VideoDevice videv = (videoDevice == null) ? (VideoDevice) getDefaultVideoDevice() : videoDevice;
            HVideoConfiguration cfg = videv.getCurrentConfiguration();

            Dimension par = cfg.getPixelAspectRatio();
            Dimension pr = cfg.getPixelResolution();

            if (log.isDebugEnabled())
            {
                log.debug(getId() + "DAR = (par_w*pr_w) / (par_h*pr_h) = ( " + par.width + " * " + pr.width + " ) / ( "
                        + par.height + " * " + pr.height + ")");
            }
            float ar = (float) (par.width * pr.width) / (float) (par.height * pr.height);

            return (Math.abs(ar - 4f / 3f) < 0.001) ? org.dvb.media.VideoFormatControl.DAR_4_3
                    : org.dvb.media.VideoFormatControl.DAR_16_9;
        }
    }

    /**
     * This helper method returns the current video size as an
     * {@link AWTVideoSize} instance. If the player is presenting, it gets the
     * size from the associated {@link VideoPresentation}. If not presenting, it
     * returns the cached size. If the size isn't cached, it caches the default
     * size as the current size and returns it.
     * 
     * @return Returns the current video size.
     */
    private AWTVideoSize getAWTSize()
    {
        // If not video device, return default size.
        if (getVideoDevice() == null)
        {
            return getDefaultAWTSize(null);
        }

        // If no presentation...
        if (!isPresenting())
        {
            // Return cached bounds (if assigned).
            if (cachedAWTSize != null)
            {
                return cachedAWTSize;
            }
            // Else, return default size.
            else
            {
                return getDefaultAWTSize(null);
            }
        }

        // Presentation in progress, so get native size from presentation, and
        // convert to AWTVideoSize
        ScalingBounds sb = getVideoPresentation().getBounds();
        return convertToAWTVideoSize(sb);
    }

    /**
     * This helper method returns the closest supported {@link AWTVideoSize}. If
     * the player is presenting, it gets the closest supported size from the
     * associated video presentatioin; otherwise, it gets it by calling
     * {@link #getDefaultAWTSize(VideoDevice)}.
     * 
     * @param sz
     *            The size to check.
     * 
     * @return Returns the closest supported size.
     */
    private AWTVideoSize checkAWTSize(AWTVideoSize sz)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "checkAWTSize(" + sz + ")");
        }

        // If no video device, return default vidoe device size.
        if (videoDevice == null)
        {
            return getDefaultAWTSize(null);
        }

        // Check for negative width/height (TCK test).
        if (sz.getSource().width < 0 || sz.getSource().height < 0 || sz.getDestination().width < 0
                || sz.getDestination().height < 0)
        {
            return getDefaultAWTSize(videoDevice);
        }

        // Is presenting, so...
        // Convert requested size to native bounds, get closest match from
        // native, and return the closest match, converted to AWTVideoSize.
        ScalingBounds sb = convertToScalingBounds(sz);
        ScalingBounds closest = checkBounds(sb);
        AWTVideoSize size = convertToAWTVideoSize(closest);
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "checkAWTSize(" + sz + ") returning " + size);
        }
        return size;
    }

    /**
     * For a given video device, this method determines its default size, which
     * is determined from the video device's configuration.
     * 
     * @param vidDev
     *            - The video device to get the default size of. If this is
     *            <code>null</code>, then the default size is obtained for the
     *            default video device of the default screen.
     * 
     * @return Returns the {@link AWTVideoSize} representing the default size.
     */
    private AWTVideoSize getDefaultAWTSize(VideoDevice vidDev)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getDefaultAWTSize(" + vidDev + ")");
        }

        AWTVideoSize size; // return value

        // If no device or not presenting, then come up with a default size that
        // is equivalent to the whole video device. If the video device isn't
        // assigned,
        // then use the default video device.
        if (vidDev == null || !isPresenting())
        {
            // Get the video device; if one isn't assigned, use the default
            // video device.
            // Then get the current configuration from the device.
            VideoDevice vd = (vidDev == null) ? (VideoDevice) getDefaultVideoDevice() : vidDev;
            HVideoConfiguration vcfg = vd.getCurrentConfiguration();

            // Get Rectangle represenenting entire video device, and
            // convert it from video device coordinates to graphics coordinates.
            Rectangle vfull = new Rectangle(new Point(0, 0), vcfg.getPixelResolution());
            HGraphicsConfiguration gcfg = getDefaultGraphicsConfiguration();
            Rectangle gfull = Util.toRectangle(vcfg, gcfg, vfull);

            // Create an AWTVideoSize from the full rectangle and return it.
            size = new AWTVideoSize(gfull, gfull);
        }
        else
        {
            ScalingBounds defaultBounds;
            int dfc;

            // When the video scaling and positioning is in the DFC_PLATFORM
            // mode,
            // the return value of this method (getDefaultSize) shall change to
            // track changes
            // made by the policy underlying DFC_PLATFORM. MHP 1.0.3 section
            // 11.4.2.5.3
            if (vidFmtCtrl.isPlatform())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getDefaultAWTSize:  In platform mode");
                }

                defaultBounds = getMediaAPI().getBounds(vidDev.getHandle());
                dfc = getMediaAPI().getDFC(vidDev.getHandle());
            }

            // If not in DFC_PLATFORM mode, track changes for default as if the
            // box
            // is under platform control.
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getDefaultAWTSize:  NOT in platform mode");
                }

                defaultBounds = ScalingBounds.FULL;
                dfc = getMediaAPI().getPlatformDFC(vidDev.getHandle());
            }

            ScalingBounds sb = Util.toScalingBounds(dfc, defaultBounds);
            size = convertToAWTVideoSize(sb);
        }

        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getDefaultAWTSize(): " + size);
        }

        return size;
    }

    /**
     * @return Returns the size of the input video, if presenting; otherwise, it
     *         returns <code>null</code>.
     */
    private Dimension getInputVideoSize()
    {
        if (!isPresenting())
        {
            return null;
        }

        if ((getMediaAPI() != null) && (getVideoDevice() != null) && (getVideoDevice().getHandle() != 0))
        { 
            return getMediaAPI().getVideoInputSize(getVideoDevice().getHandle()); 
        }
        else
        { 
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "Getting dimensions from presentation, not media\n");
            }
            return getVideoPresentation().getInputSize();
        }
    }

    /**
     * Given an AWTVideoSize specification, convert it to a scaling bounds (i.e.
     * video device coords) specification based on the current video device
     * configuration.
     * <p/>
     * Note: This must only be called while synchronized on video device lock.
     * 
     * @param sz
     *            AWTVideoSize specification to convert.
     * 
     * @return null or ScalingBounds instance with specification.
     */
    private ScalingBounds convertToScalingBounds(AWTVideoSize sz)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "convertToScalingBounds(" + sz + ")");
        }

        // If no video device, return default scaling bounds.
        if (videoDevice == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "null video device");
            }
            return new ScalingBounds(VideoPresentation.DEFAULT_BOUNDS);
        }

        // Get the current video device configuration, which is needed to
        // convert bounds.
        // If no video configuration, return default bounds.
        HVideoConfiguration vcfg = videoDevice.getCurrentConfiguration();
        if (vcfg == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "null video configuration");
            }
            return new ScalingBounds(VideoPresentation.DEFAULT_BOUNDS);
        }

        // Get the graphics device configuration.
        HGraphicsConfiguration gcfg = getDefaultGraphicsConfiguration();

        return Util.toScalingBounds(vcfg, gcfg, sz);
    }

    /**
     * Given a VideoTransformation specification convert it to a scaling bounds
     * (i.e. video device coords) specification based on the current video
     * device configuration. This must only be called when synchronized on the
     * video lock.
     * 
     * @param vt
     *            VideoTransformation specification to convert.
     * 
     * @return null or ScalingBounds instance with specification.
     */
    protected ScalingBounds convertToScalingBounds(VideoTransformation vt)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "convertToScalingBounds(" + vt + ")");
        }

        // Get the current video device configuration's dimensions.
        if (videoDevice == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "null video device");
            }
            return null;
        }

        return Util.toScalingBounds(videoDevice.getCurrentConfiguration(), vt);
    }

    /**
     * Convert bounds from video device coords to graphics coords.
     * 
     * @param sb
     *            The {@link ScalingBounds} to convert.
     * 
     * @return {@link AWTVideoSize} that is equivalent to
     * @param sb
     *            .
     */
    protected AWTVideoSize convertToAWTVideoSize(ScalingBounds sb)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "convertToAWTVideoSize(" + sb + ")");
        }

        // Get the current graphics configuration.
        HGraphicsConfiguration gcfg = getDefaultGraphicsConfiguration();
        if (Asserting.ASSERTING)
        {
            Assert.condition(gcfg != null);
        }
        return Util.toAWTVideoSize(gcfg, sb);
    }

    /**
     * Given a pair of rectangles representing the current source and
     * destination scaling bounds based on the current video device coordinates,
     * convert them to a VideoTransformation representation. This must only be
     * called when synchronized on the video lock.
     * 
     * @param sb
     *            scaling bounds
     * @return VideoTransformation representation of the scaling values.
     */
    private VideoTransformation convertToVideoTransformation(ScalingBounds sb)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "convertToVideoTransformation(" + sb + ")");
        }

        if (videoDevice == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "null video device");
            }
            return new VideoTransformation();
        }

        // Get the current video device configuration.
        return Util.toVideoTransformation(videoDevice.getCurrentConfiguration(), sb);
    }

    protected ScalingBounds getScalingBounds()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getScalingBounds()");
        }
        HVideoConfiguration vcfg = null;

        synchronized (getLock())
        {
            if (videoDevice != null)
            {
                vcfg = videoDevice.getCurrentConfiguration();
            }
        }

        ScalingBounds sb = null;

        if (cachedVidTrans != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "scaling bounds from cached VideoTransformation");
            }
            sb = convertToScalingBounds(cachedVidTrans);
            if (cachedVidTrans instanceof VideoTransformationDfc)
            {
                sb = new ScalingBoundsDfc(sb, ((VideoTransformationDfc) cachedVidTrans).getDfc());
            }
        }
        else if (cachedAWTSize != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "scaling bounds from cached AWTVideoSize");
            }
            sb = convertToScalingBounds(cachedAWTSize);
        }
        else if (vcfg != null && componentVideoBounds != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "scaling bounds from cached component bounds");
            }
            if (componentClipRegion == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "creating default clipping region");
                }

                // Initialize the clip region to full screen.
                componentClipRegion = new Rectangle(new Point(0, 0), vcfg.getPixelResolution());
            }

            // used to create the scaling bounds.
            HScreenRectangle gsrc = Util.toHScreenRectangle(vcfg, componentClipRegion);
            HScreenRectangle gdst = Util.toHScreenRectangle(getDefaultGraphicsConfiguration(), componentVideoBounds);
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "  src clip region[" + componentClipRegion + "] --> " + gsrc);
            }
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "  dst bounds [" + componentVideoBounds + "] --> " + gdst);
            }

            sb = new ScalingBounds(gsrc, gdst);
        }

        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getScalingBounds returning " + sb);
        }
        return sb;
    }

    protected HScreen getDefaultScreen()
    {
        return HScreen.getDefaultHScreen();
    }

    private HVideoDevice getDefaultVideoDevice()
    {
        return getDefaultScreen().getDefaultHVideoDevice();
    }

    private HGraphicsDevice getDefaultGraphicsDevice()
    {
        return getDefaultScreen().getDefaultHGraphicsDevice();
    }

    protected HGraphicsConfiguration getDefaultGraphicsConfiguration()
    {
        return getDefaultGraphicsDevice().getCurrentConfiguration();
    }
}
