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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.media.DataStarvedEvent;
import javax.media.InternalErrorEvent;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.media.AWTVideoSize;
import javax.tv.media.MediaSelectEvent;
import javax.tv.media.MediaSelectFailedEvent;
import javax.tv.media.MediaSelectListener;
import javax.tv.media.MediaSelectPermission;
import javax.tv.media.MediaSelectSucceededEvent;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.InvalidServiceComponentException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.media.access.MediaAccessHandlerRegistrarImpl;
import org.cablelabs.impl.media.mpe.ScalingBounds;
import org.cablelabs.impl.media.presentation.Selection;
import org.cablelabs.impl.media.presentation.SelectionTrigger;
import org.cablelabs.impl.media.presentation.ServicePresentation;
import org.cablelabs.impl.media.presentation.ServicePresentationContext;
import org.cablelabs.impl.media.session.Session;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.CallbackList;
import org.cablelabs.impl.util.CallerContextEventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.davic.media.AudioLanguageControl;
import org.davic.media.FreezeControl;
import org.davic.media.LanguageControl;
import org.davic.media.LanguageNotAvailableException;
import org.davic.media.MediaFreezeException;
import org.davic.media.MediaPresentedEvent;
import org.davic.media.NotAuthorizedException;
import org.dvb.media.CAStopEvent;
import org.dvb.media.DVBMediaSelectControl;
import org.dvb.media.NoComponentSelectedEvent;
import org.dvb.media.PresentationChangedEvent;
import org.dvb.media.ServiceRemovedEvent;
import org.dvb.media.VideoTransformation;
import org.dvb.user.GeneralPreference;
import org.dvb.user.UserPreferenceChangeEvent;
import org.dvb.user.UserPreferenceChangeListener;
import org.dvb.user.UserPreferenceManager;
import org.dvb.user.UserPreferencePermission;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HVideoConfiguration;
import org.ocap.media.ClosedCaptioningControl;
import org.ocap.media.ClosedCaptioningEvent;
import org.ocap.media.ClosedCaptioningListener;
import org.ocap.media.MediaAccessConditionControl;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.media.MediaPresentationEvent;
import org.ocap.net.OcapLocator;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.system.MonitorAppPermission;

/**
 * This is the base class for all players that present a service.
 * 
 * @author schoonma
 */
public abstract class AbstractServicePlayer extends AbstractVideoPlayer implements ServicePlayer,
        ServicePresentationContext
{
    /**
     * log4j Logger
     */
    private static final Logger log = Logger.getLogger(AbstractServicePlayer.class);

    private DVBMediaSelectControlImpl mediaSelectControl = new DVBMediaSelectControlImpl(true);

    private ClosedCaptioningControlImpl closedCaptioningControl = new ClosedCaptioningControlImpl(true);

    private final PresentationModeControlImpl presentationModeControl = new PresentationModeControlImpl(true);

    private PresentationModeNotifier presentationModeNotifier = new PresentationModeNotifier();

    private UserPreferenceChangeListener userPreferenceChangeListener = new UserPreferenceChangeListenerImpl();

    /**
     * This is the {@link Selection} used when the {@link ServicePresentation}
     * is started.  It should only be used by subclasses in the createPresentation method.
     *
     * If the 'current selection' is needed, code should call getServicePresentation().waitForCurrentSelection()
     */
    protected Selection initialSelection;

    private ExtendedNetworkInterface networkInterface;

    protected AbstractServicePlayer(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage)
    {
        super(cc, lock, resourceUsage);
        addControls(controls);
    }

    public void switchToAlternativeContent(Class alternativeContentReasonClass, int alternativeContentReasonCode)
    {
        if (presentation instanceof ServicePresentation)
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "switching to alternative content - reason: " + alternativeContentReasonCode);
            }
            ((ServicePresentation) presentation).switchToAlternativeContent(ServicePresentation.ALTERNATIVE_CONTENT_MODE_STOP_DECODE, alternativeContentReasonClass, alternativeContentReasonCode);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "unable to switch to alternative content with reason: " + alternativeContentReasonCode
                        + ", presentation: " + presentation);
            }
        }
    }

    /**
     * User preference change listener.
     */
    private class UserPreferenceChangeListenerImpl implements UserPreferenceChangeListener
    {
        public void receiveUserPreferenceChangeEvent(UserPreferenceChangeEvent e)
        {
            synchronized (getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "received UserPreferenceChangeEvent: " + e.getName());
                }

                if (isClosed() || !isPresenting())
                {
                    return;
                }

                String userPreferenceName = e.getName().toLowerCase();
                if ("Closed Caption On".equalsIgnoreCase(userPreferenceName))
                {
                    closedCaptioningControl.setCCOnPrefs();
                }
                else if ("Analog Closed Caption Service".equalsIgnoreCase(userPreferenceName))
                {
                    closedCaptioningControl.setCCAnalogServicePrefs();
                }
                else if ("Digital Closed Caption Service".equalsIgnoreCase(userPreferenceName))
                {
                    closedCaptioningControl.setCCDigitalServicePrefs();
                }
                else if ("parental rating".equalsIgnoreCase(userPreferenceName))
                {
                    // Handle parental rating change
                    // Reselect if this trigger is being handled by the stack
                    MediaAccessHandlerRegistrar mahri = MediaAccessHandlerRegistrar.getInstance();
                    if (!((MediaAccessHandlerRegistrarImpl) mahri).isTriggerExternal(MediaPresentationEvaluationTrigger.USER_RATING_CHANGED))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "UserPreferenceChangeListener trigger NOT external, calling reselect...");
                        }
                        getServicePresentation().reselect(MediaPresentationEvaluationTrigger.USER_RATING_CHANGED);
                    }
                }
                else if ("user language".equalsIgnoreCase(userPreferenceName))
                {
                    // Marking this reference to be final as we can't access a
                    // non final variable from within a method of the anonymous
                    // inner class.
                    final GeneralPreference userLanguage = new GeneralPreference(userPreferenceName);

                    // Get the instance of UserPreferenceManager and read the
                    // "user language" preference
                    UserPreferenceManager usrPrefMgr = UserPreferenceManager.getInstance();
                    usrPrefMgr.read(userLanguage);

                    AccessController.doPrivileged(new PrivilegedAction()
                    {
                        public Object run()
                        {
                            String favoriteLanguage = userLanguage.getMostFavourite();
                            try
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug(getId() + "user language preference changed - selecting language:"
                                            + favoriteLanguage);
                                }
                                // Update the components of the current
                                // selection
                                // with new SAP audio steam if any by calling
                                // the
                                // selectLanguage method of the
                                // AudioLanguageControl
                                ((AudioLanguageControl) getControl(AudioLanguageControl.class.getName())).selectLanguage(favoriteLanguage);
                            }
                            catch (LanguageNotAvailableException lnae)
                            {
                                // Nothing wrong in getting this exception. This
                                // could
                                // happen when the currently tuned channel is
                                // not as SAP
                                // enabled channel. When we tune to a channel
                                // with a SAP,
                                // tuning process will appropriately pick the
                                // preferred
                                // language during selection process.
                                if (log.isDebugEnabled())
                                {
                                    log.debug(getId() + "requested audio language is not available - ignoring request to select language: "
                                                    + favoriteLanguage, lnae);
                                }
                            }
                            catch (NotAuthorizedException nae)
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug(getId() + "ignoring not authorized while attempting to select language: "
                                            + favoriteLanguage, nae);
                                }
                                // TODO: we can ignore this exception as MAH
                                // will
                                // re-trigger selection once the authorization
                                // is
                                // achieved.
                            }
                            return null;
                        }
                    });
                }
            }
        }
    }

    /**
     * Reports true if the player is a ServiceContext specific player created by
     * implicitly via ServiceContext.select method, or false if the player was
     * created explicitly via by the application via createPlayer.
     * 
     * @return true if it is a ServiceContext specific player.
     */
    public boolean isServiceContextPlayer()
    {
        return (getResourceUsage() instanceof ServiceContextResourceUsage);
    }

    /*
     * AVPlayerBase Overrides
     */

    /**
     * If this was created by a
     * {@link javax.tv.service.selection.ServiceContext}, then prefer the
     * background device.
     */
    public boolean preferBackgroundDevice()
    {
        return isServiceContextPlayer();
    }

    protected Object doAcquireRealizeResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doAcquireRealizeResources");
        }
        synchronized(getLock())
        {
            // default implementation is a no-op
            // Listen to user preference changes. Switch to the system context to
            // install
            // the listener so that events are delivered on the system context.
            ccMgr.getSystemContext().runInContext(new Runnable()
            {
                public void run()
                {
                    UserPreferenceManager.getInstance().addUserPreferenceChangeListener(userPreferenceChangeListener);
                }
            });
        }
        return null;
    }

    protected void doReleaseRealizedResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "doReleaseRealizedResources");
        }
        synchronized(getLock())
        {
            if (userPreferenceChangeListener != null)
            {
                //remove the listener
                UserPreferenceManager.getInstance().removeUserPreferenceChangeListener(userPreferenceChangeListener);
            }

            if (presentation != null)
            {
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "calling presentation.stop and setting presentation to null");
                    }
                    presentation.stop();
                    presentation = null;
                }
                catch (Error x)
                {
                    postEvent(new InternalErrorEvent(this, x.toString()));
                }
            }
            videoComponentContainer = null;
        }
    }

    /*
     * ServicePlayer
     */

    public Locator[] getServiceContentLocators()
    {
        synchronized (getLock())
        {
            Selection selection = getServicePresentation().waitForCurrentSelection();
            if (selection != null)
            {
                return selection.getComponentLocators();
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(getId() + "getServiceContentLocators called but currentSelection was null - returning empty array");
                }
            }
            return new Locator[] {};
        }
    }

    public ExtendedNetworkInterface getNetworkInterface()
    {
        return networkInterface;
    }

    public void setNetworkInterface(ExtendedNetworkInterface networkInterface)
    {
        this.networkInterface = networkInterface;
    }

    public BroadcastAuthorization getBroadcastAuthorization()
    {
        // by default, we don't provide a broadcast authorization component
        return null;
    }

    public void updateServiceContextSelection(Locator[] componentLocators) throws InvalidLocatorException, InvalidServiceComponentException
    {
        if (log.isInfoEnabled())
        {
            log.info(getId() + "updateServiceContextSelection - locators: " + Arrays.toString(componentLocators));
        }
        Selection selection = getServicePresentation().waitForCurrentSelection();
        handleSelect(selection.getServiceDetails(), componentLocators, false);
    }

    /**
     * This method -must- be called by all ServicePlayer implementations wishing to present a service via ServiceContext OR
     * standalone JMF (Manager.createPlayer)
     *  
     * @param serviceDetails
     *            The {@link ServiceDetails} from which media components are
     *            presented - may only be null in the case of AlternativeContent
     *
     * @param componentLocators
     *            Any array of {@link Locator}s of service compoenents to
     *            present. If this is <code>null</code>, then the default
     *            components will be selected.
     *
     */
    public void setInitialSelection(ServiceDetails serviceDetails, Locator[] componentLocators)
    {
        if (Asserting.ASSERTING)
        {
            Assert.preCondition(!isPresenting());
            Assert.preCondition(initialSelection == null);
        }

        // Cache the initial selection for later use.
        initialSelection = new Selection(MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE,
                (ServiceDetailsExt) serviceDetails, componentLocators);
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "setInitialSelection: " + initialSelection);
        }
    }

    /**
     * If this is a component player, this is the {@link Container} that holds
     * the {@link VideoComponent}.
     */
    private Container videoComponentContainer;

    /**
     * Internal method for converting a player to a background player as part of
     * a service context swap operation. Removes the component settings of this
     * player, converting it to a background player.
     */
    private void convertToBackgroundPlayer()
    {
        if (Asserting.ASSERTING)
        {
            Assert.preCondition(isComponentPlayer());
        }

        // Remove the video component from the container
        videoComponentContainer.remove(videoComponent);
        videoComponent.setVisible(false);

        // Release references to container and component.
        videoComponentContainer = null;
        videoComponent = null;

        // If a clipping region has been set, then cache the video
        // transformation
        // using the clipping region, converted from AWT coords to video device
        // coords.
        if (componentVideoBounds != null && componentClipRegion != null)
        {
            // Get graphics and video device configurations to use to convert
            // between coord systems.
            HVideoConfiguration vcfg = getVideoDevice().getCurrentConfiguration();
            HGraphicsConfiguration gcfg = getDefaultGraphicsConfiguration();

            // Convert upper-left corner of clip region from graphics to video
            // coordinates.
            Point vp0 = gcfg.convertTo(vcfg, componentClipRegion.getLocation());
            // Convert the lower-right corner of clip region from graphics to
            // video coordinates.
            Point vp1 = gcfg.convertTo(vcfg, new Point(componentClipRegion.x + componentClipRegion.width,
                    componentClipRegion.y + componentClipRegion.height));
            // Construct a clipping rectangle from the upper-left and
            // lower-right points.
            Rectangle clipping = new Rectangle(vp0, new Dimension(vp1.x - vp0.x, vp1.y - vp0.y));

            // Create default video transformation but with the converted
            // clipping region.
            cachedVidTrans = new VideoTransformation();
            cachedVidTrans.setClipRegion(clipping);
        }

        // Clear out component video bounds.
        componentVideoBounds = null;
        componentClipRegion = null;

        // Set flag on video component control, forcing it to return create a
        // new VideoComponent on the next call to getVisualComponent().
        vidCompCtrl.returnCachedComponent(false);

        // Finish bookkeeping to convert to background player.
        completeConversion(false);
    }

    /**
     * Internal method for converting a player to a component player as part of
     * a service context swap operation. It creates a new {@link VideoComponent}
     * for the player and assigns it to the container. The newly created
     * component copies its initial bounds and visibility settings from the
     * previous component player's component.
     * 
     * @param from
     *            source player
     */
    private void convertToComponentPlayer(AbstractServicePlayer from)
    {
        if (Asserting.ASSERTING)
        {
            Assert.preCondition(isBackgroundPlayer());
        }

        // Get the clipRegion
        if (cachedAWTSize != null)
        {
            componentClipRegion = cachedAWTSize.getSource();
        }
        else
        {
            if (cachedVidTrans != null)
            {
                ScalingBounds sb = convertToScalingBounds(cachedVidTrans);
                AWTVideoSize sz = convertToAWTVideoSize(sb);
                componentClipRegion = sz.getSource();
            }
        }

        // Clear out background player-specific fields.
        cachedAWTSize = null;
        cachedVidTrans = null;

        // Cache the container and bounds of other player.
        videoComponentContainer = from.videoComponentContainer;
        componentVideoBounds = from.componentVideoBounds;
        componentVideoShown = from.componentVideoShown;

        // Create new VideoComponent and add it to the container.
        videoComponent = new VideoComponent();
        videoComponent.setBounds(componentVideoBounds);
        videoComponent.setVisible(componentVideoShown);
        videoComponent.setupLocationListener();
        videoComponentContainer.add(videoComponent);

        // Set flag on video component control, forcing it to return the
        // videoComponent
        // cached by the player on the next call to getVisualComponent().
        vidCompCtrl.returnCachedComponent(true);

        // Finish bookkeeping to convert to foreground player.
        completeConversion(true);
    }

    public void setInitialVideoSize(VideoTransformation trans, Container container, Rectangle bounds)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "setInitialVideoSize(trans=" + trans + ", container=" + container + ", bounds=" + bounds + ")");
        }

        if (container != null && bounds == null)
        {
            throw new IllegalArgumentException("Container specified but not bounds.");
        }

        synchronized (getLock())
        {
            // If initial transformation specified, cache it.
            if (trans != null)
            {
                cachedVidTrans = trans;
            }
            // If a container was specified and this is a component-capable
            // player...
            if ((videoComponentContainer = container) != null && vidCompCtrl.isEnabled())
            {
                // Convert to component player and cache component video bounds.
                completeConversion(true);
                componentVideoBounds = bounds;
                componentVideoShown = true;

                // Construct a VideoComponent (stored in the 'videoComponent'
                // field) and add it to the container.
                videoComponent = new VideoComponent();
                videoComponentContainer.add(videoComponent);

                // Set the component's bounds and establish listeners for
                // component location change.
                videoComponent.setBounds(bounds);
                videoComponent.setupListeners();
            }
        }
    }

    public void swapDecoders(ServicePlayer sp, boolean useOtherAudio)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "swapDecoders(otherPlayer=" + sp + ", useOtherAudio=" + useOtherAudio + ")");
        }

        // Lock access to this video device.
        synchronized (getLock())
        {
            AbstractServicePlayer otherPlayer = (AbstractServicePlayer) sp;

            // Lock access to other video device.
            synchronized (otherPlayer.getLock())
            {
                VideoDevice vd = getVideoDevice();
                VideoDevice otherVd = otherPlayer.getVideoDevice();

                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "swapDecoders(): swapping video device " + vd + " with " + otherVd);
                }

                // If either is null, then one must be closed, so just return
                // silently.
                if (vd == null || otherVd == null)
                {
                    return;
                }

                // Swap what the native decoders are decoding.
                try
                {
                    getMediaAPI().swapDecoders(vd.getHandle(), otherVd.getHandle(), useOtherAudio);
                }
                catch (Error e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(getId() + "swapDecoders(): media swap failed", e);
                    }
                    return;
                }

                // Swap presentation info. This must be done before the video
                // device
                // swap and conversion between background/component to ensure
                // that settings
                // are correct.
                getServicePresentation().swap(otherPlayer.getServicePresentation());

                // Swap background/component player settings.
                if (isComponentPlayer())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "swapDecoders(): convert " + this + " to BG player and " + sp + " to COMP player");
                    }
                    otherPlayer.convertToComponentPlayer(this);
                    convertToBackgroundPlayer();
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "swapDecoders(): convert " + this + " to COMP player and " + sp + " to BG player");
                    }
                    convertToComponentPlayer(otherPlayer);
                    otherPlayer.convertToBackgroundPlayer();
                }

                // Lastly, swap the video devices and controllers of the video
                // devices.
                vd.swapControllers(otherPlayer);
            }
        }
    }

    private void handleSelect(ServiceDetailsExt sdx, Locator[] locators, boolean fromMediaSelectControl) throws InvalidLocatorException,
            InvalidServiceComponentException
    {
        // Do nothing if closed.
        if (isClosed())
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "ignoring handleSelect when closed");
            }
            return;
        }

        if (getResourceUsage().isResourceUsageEAS())
        {
            throw new IllegalStateException("Unable to use MediaSelectControl - presenting EAS");
        }
        Selection currentSelection = getServicePresentation().waitForCurrentSelection();
        Util.validateServiceComponents(currentSelection.getSIManager(), locators, isServiceBound(), sdx);
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "handleSelect - details: " + sdx + ", locators: " + Arrays.toString(locators) + ", called by mediaSelectControl: " + fromMediaSelectControl);
        }
        // Make the selection request on the session with the appropriate trigger (NormalContentEvent will be posted for SERVICE_CONTENT_RESELECT triggers)
        MediaPresentationEvaluationTrigger trigger = (fromMediaSelectControl ?
                MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE_COMPONENTS : SelectionTrigger.SERVICE_CONTEXT_RESELECT);
        Selection selection = new Selection(trigger, sdx, locators);
        getServicePresentation().select(selection);
    }

    /**
     * {@link CallbackList} of registered {@link SessionChangeCallback}s.
     */
    private CallbackList sessionChangeCallbacks = new CallbackList(SessionChangeCallback.class);

    public int addSessionChangeCallback(SessionChangeCallback cb)
    {
        if (cb == null)
        {
            return Session.INVALID;
        }

        synchronized (getLock())
        {
            sessionChangeCallbacks.addCallback(cb, 1);
            return isPresenting() ? getServicePresentation().getSessionHandle() : Session.INVALID;
        }
    }

    public void removeSessionChangeCallback(SessionChangeCallback cb)
    {
        if (cb == null)
        {
            return;
        }

        sessionChangeCallbacks.removeCallback(cb);
    }

    // The CallbackList framework uses reflection to invoke callbacks.
    // For performance reasons, cache the Method objects for each callback
    // during static initialization.

    private static Method notifySessionCompleteMethod;

    private static Method notifyStartingSessionMethod;

    private static Method notifyStoppingSessionMethod;
    {
        try
        {
            notifySessionCompleteMethod = SessionChangeCallback.class.getDeclaredMethod("notifySessionComplete",
                    new Class[] { int.class, boolean.class });
            notifyStartingSessionMethod = SessionChangeCallback.class.getDeclaredMethod("notifyStartingSession",
                    new Class[] { int.class });
            notifyStoppingSessionMethod = SessionChangeCallback.class.getDeclaredMethod("notifyStoppingSession",
                    new Class[] { int.class });
        }
        catch (Throwable x)
        {
            SystemEventUtil.logCatastrophicError(x);
        }
    }

    public void notifySessionComplete(int sessionHandle, boolean succeeded)
    {
        try
        {
            sessionChangeCallbacks.invokeCallbacks(notifySessionCompleteMethod, new Object[] {
                    new Integer(sessionHandle), succeeded ? Boolean.TRUE : Boolean.FALSE });
        }
        catch (Throwable x)
        {
            SystemEventUtil.logRecoverableError(x);
        }
    }

    public void notifyStartingSession(int sessionHandle)
    {
        try
        {
            sessionChangeCallbacks.invokeCallbacks(notifyStartingSessionMethod, new Object[] { new Integer(
                    sessionHandle) });
        }
        catch (Throwable x)
        {
            SystemEventUtil.logRecoverableError(x);
        }
    }

    public void notifyStoppingSession(int sessionHandle)
    {
        try
        {
            sessionChangeCallbacks.invokeCallbacks(notifyStoppingSessionMethod, new Object[] { new Integer(
                    sessionHandle) });
        }
        catch (Throwable x)
        {
            SystemEventUtil.logRecoverableError(x);
        }
    }

    /*
     * Controls
     */

    private ControlBase[] controls = {mediaSelectControl, new FreezeControlImpl(true), closedCaptioningControl,
            new MediaAccessConditionControlImpl(true),
            new AudioLanguageControlImpl(true), presentationModeControl };

    /**
     * @return This method should return <code>true</code> if the
     *         {@link ServicePlayer} is restricted to presenting <em>only</em>
     *         components within the initially selected {@link Service}. If the
     *         {@link ServicePlayer} can present components external to the
     *         {@link Service}&mdash;e.g., a
     *         {@link org.cablelabs.impl.media.content.ocap.broadcast.Handler
     *         ocap.broadcast.Handler}&mdash;this should return
     *         <code>false</code>.
     */
    protected abstract boolean isServiceBound();

    private static class DVBMediaSelectControlEventDispatcher extends CallerContextEventMulticaster
    {
        public void dispatch(EventListener listeners, EventObject event)
        {
            if (listeners instanceof MediaSelectListener && event instanceof MediaSelectEvent)
            {
                ((MediaSelectListener) listeners).selectionComplete((MediaSelectEvent) event);
            }
            else
            {
                if (Asserting.ASSERTING)
                {
                    Assert.condition(false, "listeners or event of wrong type");
                }
            }
        }
    }

    private class DVBMediaSelectControlImpl extends ControlBase implements DVBMediaSelectControl
    {

        DVBMediaSelectControlImpl(boolean enabled)
        {
            super(enabled);
        }

        /*
         * Helpers
         */

        protected void release()
        {
            multicaster.cleanup();
        }

        /**
         * This is called by other MediaSelectControl methods to queue up the
         * selection on the presentation.
         *
         * @param sdx
         *            - the service details to be presented
         * @param locators
         *            - javaTV locators for the selection (may be null if
         *            reselecting service details via
         *            DVBMediaSelectControl.selectServiceMediaComponents)
         *
         */
        private void select(ServiceDetailsExt sdx, Locator[] locators) throws InvalidLocatorException,
                InvalidServiceComponentException
        {
            handleSelect(sdx, locators, true);
        }

        private void checkNullLocator(Locator locator) throws NullPointerException
        {
            if (locator == null)
            {
                throw new NullPointerException("null Locator");
            }
        }

        private void checkNullLocators(Locator[] locators) throws NullPointerException
        {
            if (locators == null)
            {
                throw new NullPointerException("null Locator array");
            }
            for (int i = 0; i < locators.length; ++i)
            {
                checkNullLocator(locators[i]);
            }
        }

        /**
         * Check that the specified service details is the same as the currently
         * presenting service details.
         * 
         * @param toService
         *            the service details to check.
         * @param isServiceBound
         *            indicates whether the service <em>must</em> be the same.
         * 
         * @throws InvalidServiceComponentException
         *             if the service is not hte same.
         */
        private void checkServiceRestriction(ServiceDetailsExt toService, boolean isServiceBound)
                throws InvalidServiceComponentException
        {
            Selection selection = getServicePresentation().waitForCurrentSelection();
            if (isServiceBound && !toService.equals(selection.getServiceDetails()))
            {
                throw new InvalidServiceComponentException(toService.getLocator(),
                        "different service selection not allowed");
            }
        }

        private boolean checkStarted(Locator[] locators)
        {
            boolean started = isStarted();
            if (!started)
            {
                postEvent(new MediaSelectFailedEvent(AbstractServicePlayer.this, locators));
            }
            return started;
        }

        /*
         * DVBMediaSelectControl methods
         */

        public void selectServiceMediaComponents(Locator serviceLocator) throws InvalidLocatorException,
                InvalidServiceComponentException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "select serviceMediaComponents - locator: " + serviceLocator);
            }

            checkNullLocator(serviceLocator);

            synchronized (getLock())
            {
                if (isClosed() || !checkStarted(new Locator[] { serviceLocator }))
                {
                    return;
                }

                // Get the Service represented by the locator and check for
                // restriction.
                Service service = Util.getService(serviceLocator);
                ServiceDetailsExt sdx = Util.getServiceDetails(service);
                checkServiceRestriction(sdx, isServiceBound());

                // Queue the request.
                select(sdx, null);
            }
        }

        /*
         * MediaSelectControl methods
         */

        public void select(Locator[] componentLocators) throws InvalidLocatorException,
                InvalidServiceComponentException, SecurityException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "select locators: " + Arrays.toString(componentLocators));
            }

            checkNullLocators(componentLocators);

            synchronized (getLock())
            {
                if (isClosed() || !checkStarted(componentLocators))
                {
                    return;
                }

                for (int i = 0; i < componentLocators.length; i++)
                {
                    SecurityUtil.checkPermission(new MediaSelectPermission(componentLocators[i]));
                }
                // Queue the request.
                Selection selection = getServicePresentation().waitForCurrentSelection();
                select(selection.getServiceDetails(), componentLocators);
            }
        }

        public void select(Locator componentLocator) throws InvalidLocatorException, InvalidServiceComponentException,
                SecurityException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "select locator: " + componentLocator);
            }

            checkNullLocator(componentLocator);

            synchronized (getLock())
            {
                if (isClosed() || !checkStarted(new Locator[] { componentLocator }))
                {
                    return;
                }

                SecurityUtil.checkPermission(new MediaSelectPermission(componentLocator));

                // Queue the request.
                Selection selection = getServicePresentation().waitForCurrentSelection();
                select(selection.getServiceDetails(), new Locator[] { componentLocator });
            }
        }

        private ServiceComponentExt[] getCurrentServiceComponents()
        {
            return getServicePresentation().waitForCurrentSelection().getCurrentComponents();
        }

        public void add(Locator componentLocator) throws InvalidLocatorException, InvalidServiceComponentException,
                InsufficientResourcesException, SecurityException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "add locator: " + componentLocator);
            }

            checkNullLocator(componentLocator);

            synchronized (getLock())
            {
                if (isClosed() || !checkStarted(new Locator[] { componentLocator }))
                {
                    return;
                }

                SecurityUtil.checkPermission(new MediaSelectPermission(componentLocator));

                // Lookup the component for the locator and make sure it's part
                // of the current service.
                Selection selection = getServicePresentation().waitForCurrentSelection();
                ServiceComponentExt addComponent = Util.getServiceComponent(selection.getSIManager(),
                        componentLocator);

                // Get currently selected components and build the new
                // components list.
                ServiceComponentExt[] allComponents = getCurrentServiceComponents();
                ServiceComponentExt[] newComponents = new ServiceComponentExt[allComponents.length + 1];
                System.arraycopy(allComponents, 0, newComponents, 0, allComponents.length);
                newComponents[allComponents.length] = addComponent;

                // Obtain locators from the service components.
                Locator[] locators = Util.getLocatorsForComponents(newComponents);

                // Queue the request.
                select(Util.getServiceDetails(newComponents[0]), locators);
            }
        }

        /**
         * Check that a component, represented by a locator, can be removed.
         * 
         * @param allComponents
         *            the components from which to remove.
         * @param componentLocator
         *            the locator of the component to remove, which must be one
         *            of the components in
         * @param allComponents
         *            .
         * 
         * @return the index into
         * @param allComponents
         *            of the component to remove.
         * 
         * @throws InvalidLocatorException
         *             if the componentLocator is invalid.
         */
        int checkRemove(ServiceComponentExt[] allComponents, Locator componentLocator) throws InvalidLocatorException
        {
            // use current selection's SIManager to retrieve components
            Selection selection = getServicePresentation().waitForCurrentSelection();
            ServiceComponentExt rmvComponent = Util.getServiceComponent(selection.getSIManager(),
                    componentLocator);
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "checkRemove - all components: " + Arrays.toString(allComponents) + ", component to remove: "
                        + rmvComponent);
            }
            int i;
            for (i = 0; i < allComponents.length; ++i)
            {
                if (allComponents[i].equals(rmvComponent))
                {
                    break;
                }
            }
            //went through array - did not find the requested component
            if (i == allComponents.length)
            {
                throw new InvalidLocatorException(componentLocator, "not part of current presentation");
            }
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "checkRemove - found locator: " + componentLocator + " and serviceComponent index: " + i);
            }
            return i;
        }

        public void remove(Locator componentLocator) throws InvalidLocatorException, InvalidServiceComponentException,
                SecurityException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "remove locator: " + componentLocator);
            }
            checkNullLocator(componentLocator);

            synchronized (getLock())
            {
                if (isClosed() || !checkStarted(new Locator[] { componentLocator }))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "remove called when closed or not started - ignoring");
                    }
                    return;
                }

                SecurityUtil.checkPermission(new MediaSelectPermission(componentLocator));

                // Lookup the component for the locator and make sure it's part
                // of the current presentation.
                ServiceComponentExt[] allComponents = getCurrentServiceComponents();
                int rmvIdx = checkRemove(allComponents, componentLocator);
                //index was found..make sure we have a length > 0
                if ((allComponents.length - 1) < 1)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(getId() + "last component removed - calling notifyNoComponentSelected");
                    }
                    notifyNoComponentSelected();
                    return;
                }
                // Create new set of components without the removed component.
                ServiceComponentExt[] newComponents = new ServiceComponentExt[allComponents.length - 1];
                int newArrayIndex = 0;
                for (int i=0;i<allComponents.length;i++)
                {
                    //skip index to be removed
                    if (rmvIdx == i)
                    {
                        continue;
                    }
                    newComponents[newArrayIndex++] = allComponents[i];
                }

                // Get the locators.
                Locator[] locators = Util.getLocatorsForComponents(newComponents);

                // Queue the request.
                select(Util.getServiceDetails(newComponents[0]), locators);
            }
        }

        public void replace(Locator rmvComponentLocator, Locator addComponentLocator) throws InvalidLocatorException,
                InvalidServiceComponentException, InsufficientResourcesException, SecurityException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "replace locator - current locator: " + rmvComponentLocator + ", new locator: "
                        + addComponentLocator);
            }
            checkNullLocator(rmvComponentLocator);
            checkNullLocator(addComponentLocator);

            synchronized (getLock())
            {
                if (isClosed() || !checkStarted(new Locator[] { rmvComponentLocator, addComponentLocator }))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "closed or not started - ignoring request to replace locator");
                    }
                    return;
                }

                SecurityUtil.checkPermission(new MediaSelectPermission(rmvComponentLocator));
                SecurityUtil.checkPermission(new MediaSelectPermission(addComponentLocator));

                // Check adding/removal of components.
                Selection selection = getServicePresentation().waitForCurrentSelection();
                ServiceComponentExt addComp = Util.getServiceComponent(selection.getSIManager(),
                        addComponentLocator);
                ServiceComponentExt[] allComponents = getCurrentServiceComponents();
                int rmvIdx = checkRemove(allComponents, rmvComponentLocator);
                // Build the new list of components.
                ServiceComponentExt[] newComponents = (ServiceComponentExt[]) Arrays.copy(allComponents,
                        ServiceComponentExt.class);
                newComponents[rmvIdx] = addComp;

                // Get the locators.
                Locator[] locators = Util.getLocatorsForComponents(newComponents);

                // Queue the request.
                select(Util.getServiceDetails(newComponents[0]), locators);
            }
        }

        public Locator[] getCurrentSelection()
        {
            return getServiceContentLocators();
        }

        /*
         * Event Dispatching
         */

        private DVBMediaSelectControlEventDispatcher multicaster = new DVBMediaSelectControlEventDispatcher();

        public final void addMediaSelectListener(MediaSelectListener listener)
        {
            if (listener == null)
            {
                throw new NullPointerException("null MediaSelectListener");
            }

            synchronized (getLock())
            {
                if (isClosed())
                {
                    return;
                }
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "addMediaSelectListener: " + listener);
                }
                multicaster.addListenerMulti(listener);
            }
        }

        public final void removeMediaSelectListener(MediaSelectListener listener)
        {
            if (listener == null)
            {
                throw new NullPointerException("null MediaSelectListener");
            }

            synchronized (getLock())
            {
                if (isClosed())
                {
                    return;
                }
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "removeMediaSelectListener: " + listener);
                }
                multicaster.removeListener(listener);
            }
        }

        /**
         * Post the specified {@link MediaSelectEvent} to all registered
         * {@link MediaSelectListener}s.
         * 
         * @param event
         *            The {@link MediaSelectEvent} to be posted.
         */
        private void postEvent(MediaSelectEvent event)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "mediaSelectControl postEvent: " + event);
            }
            // Send the event.
            multicaster.multicast(event);
        }
    }

    /*
     * MAS - commented-out since it is not part of implementation (for now)
     * private class SubtitlingLanguageControlImpl extends
     * BaseLanguageControlImpl implements SubtitlingLanguageControl {
     * SubtitlingLanguageControlImpl(boolean enabled) { super(enabled); }
     * 
     * protected StreamType getComponentStreamType() { return
     * StreamType.SUBTITLES; }
     * 
     * public boolean isSubtitlingOn() { ServiceComponentExt subtitleComponent =
     * getCurrentLanguageServiceComponent(); return subtitleComponent != null; }
     * 
     * public boolean setSubtitling(boolean isActivated) {
     * synchronized(getLock()) { if (isClosed() || !isPresenting()) return
     * false;
     * 
     * boolean previousValue = isSubtitlingOn(); ServiceComponentExt
     * subtitleComponent = getCurrentLanguageServiceComponent();
     * 
     * if (isActivated) { ServiceComponentExt targetComponent = null;
     * 
     * try { Selection selection =
     * getServicePresentation().getCurrentSelection();
     * 
     * if (selection != null) { ServiceAdapter serviceAdapter = new
     * ServiceAdapter(selection.service); ServiceComponentExt[] components =
     * serviceAdapter.getAllServiceComponents(); // // at most, there will be
     * one language for each component // for (int i = 0; i < components.length;
     * i++) { if (components[i].getStreamType() == getComponentStreamType()) {
     * targetComponent = components[i]; break; } } }
     * 
     * if (subtitleComponent != null && targetComponent != null) {
     * mediaSelectCtrl.replace(subtitleComponent.getLocator(),
     * targetComponent.getLocator()); } else if (targetComponent != null) {
     * mediaSelectCtrl.add(targetComponent.getLocator()); } } catch
     * (InsufficientResourcesException exc) {
     * log.info("Exception adding subtitle component", exc); } catch
     * (InvalidLocatorException exc) {
     * log.info("Exception adding subtitle component", exc); } catch
     * (InvalidServiceComponentException exc) {
     * log.info("Exception adding subtitle component", exc); } catch
     * (SecurityException exc) { log.info("Exception adding subtitle component",
     * exc); }
     * 
     * } else { // // get the current service component for subtitling and //
     * remove it from the player // if (subtitleComponent != null) { try {
     * mediaSelectCtrl.remove(subtitleComponent.getLocator()); } catch
     * (InvalidLocatorException exc) {
     * log.info("Exception adding subtitle component", exc); } catch
     * (InvalidServiceComponentException exc) {
     * log.info("Exception adding subtitle component", exc); } catch
     * (SecurityException exc) { log.info("Exception adding subtitle component",
     * exc); }
     * 
     * } } return previousValue; } } }
     */

    private class PresentationModeControlImpl extends ControlBase implements PresentationModeControl
    {
        PresentationModeControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public void addPresentationModeListener(PresentationModeListener listener)
        {
            presentationModeNotifier.addPresentationModeListener(listener);
        }

        public void removePresentationModeListener(PresentationModeListener listener)
        {
            presentationModeNotifier.removePresentationModeListener(listener);
        }

        public Component getControlComponent()
        {
            return null;
        }
    }

    private class AudioLanguageControlImpl extends BaseLanguageControlImpl implements AudioLanguageControl
    {
        AudioLanguageControlImpl(boolean enabled)
        {
            super(enabled);
        }

        protected StreamType getComponentStreamType()
        {
            return StreamType.AUDIO;
        }
    }

    private abstract class BaseLanguageControlImpl extends ControlBase implements LanguageControl
    {
        BaseLanguageControlImpl(boolean enabled)
        {
            super(enabled);
        }

        protected abstract StreamType getComponentStreamType();

        public String[] listAvailableLanguages()
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "listAvailableLanguages");
            }
            synchronized (getLock())
            {
                if (!isStarted())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "Player is closed or not presenting, returning String[0]");
                    }
                    return new String[0];
                }

                // Get the set of language codes from the audio components.
                Selection selection = getServicePresentation().waitForCurrentSelection();
                List foundLanguages = new ArrayList();

                if (selection != null)
                {
                    ServiceComponentExt[] components = Util.getAllMediaComponents(selection.getServiceDetails());
                    // At most, there will be one language for each component.
                    for (int i = 0; i < components.length; i++)
                    {
                        String thisLang = components[i].getAssociatedLanguage();
                        if (components[i].getStreamType() == getComponentStreamType() && thisLang != null
                                && !thisLang.trim().equals("") && !foundLanguages.contains(thisLang))
                        {
                            foundLanguages.add(thisLang);
                        }
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "No current selection, returning String[0]");
                    }
                }
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "listAvailableLanguages returning: " + foundLanguages);
                }
                return (String[]) Arrays.copy(foundLanguages.toArray(), String.class);
            }
        }

        public void selectLanguage(String lang) throws LanguageNotAvailableException, NotAuthorizedException
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "selectLanguage: " + lang);
            }
            synchronized (getLock())
            {
                if (!isStarted())
                {
                    throw new LanguageNotAvailableException("not started");
                }

                // Find the audio component with the desired language and
                // replace
                // the current audio component with the desired one.
                ServiceComponentExt currentAudioComponent = getCurrentLanguageServiceComponent();
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "current audio component: " + currentAudioComponent);
                }
                ServiceComponentExt targetAudioComponent = getLanguageServiceComponent(lang);
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "found target audio component for language: " + lang + ": " + targetAudioComponent);
                }

                if (targetAudioComponent == null)
                {
                    throw new LanguageNotAvailableException("target audio component is null: " + lang);
                }

                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "replacing current audio component with target - target audio component: "
                                + targetAudioComponent);
                    }
                    if (currentAudioComponent != null)
                    {
                        mediaSelectControl.replace(currentAudioComponent.getLocator(), targetAudioComponent.getLocator());
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "no current audio component - adding audio component: " + targetAudioComponent);
                        }
                        mediaSelectControl.add(targetAudioComponent.getLocator());
                    }
                }
                catch (InvalidLocatorException exc)
                {
                    throw new LanguageNotAvailableException("Invalid Locator while changing language to " + lang
                            + " - " + exc.getMessage());
                }
                catch (InvalidServiceComponentException exc)
                {
                    throw new LanguageNotAvailableException("Invalid Service Component while changing language to "
                            + lang + " - " + exc.getMessage());
                }
                catch (InsufficientResourcesException exc)
                {
                    throw new LanguageNotAvailableException("Insufficient resources while changing language to: "
                            + lang + " - " + exc.getMessage());
                }
            }
        }

        public String getCurrentLanguage()
        {
            synchronized (getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getCurrentLanguage");
                }
                if (!isStarted())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "Player is closed or not presenting, returning empty String");
                    }
                    return "";
                }

                String currentLanguage;
                ServiceComponentExt audioComponent = getCurrentLanguageServiceComponent();
                if (audioComponent != null)
                {
                    currentLanguage = audioComponent.getAssociatedLanguage();
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "No audioComponent available, returning empty String");
                    }
                    currentLanguage = "";
                }
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "getCurrentLanguage - returning: " + currentLanguage);
                }
                return currentLanguage;
            }
        }

        public String selectDefaultLanguage() throws NotAuthorizedException
        {
            synchronized (getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "selectDefaultLanguage");
                }
                if (!isStarted())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "not started - returning empty string");
                    }
                    return "";
                }

                // Gget the associated language from the default audio component
                // of the currently selected service.
                String defaultLanguage = null;
                Selection selection = getServicePresentation().waitForCurrentSelection();
                if (selection != null)
                {
                    ServiceComponentExt[] defaultComponents = Util.getDefaultMediaComponents(selection.getServiceDetails());
                    for (int i = 0; i < defaultComponents.length; i++)
                    {
                        if (defaultComponents[i].getStreamType() == getComponentStreamType())
                        {
                            defaultLanguage = defaultComponents[i].getAssociatedLanguage();
                            break;
                        }
                    }

                }

                if (defaultLanguage != null)
                {
                    try
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(getId() + "default language found - selecting: " + defaultLanguage);
                        }
                        selectLanguage(defaultLanguage);
                    }
                    catch (LanguageNotAvailableException exc)
                    {
                        throw new NotAuthorizedException(exc.getMessage());
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "no default language found - returning empty string");
                    }
                    defaultLanguage = "";
                }
                return defaultLanguage;
            }
        }

        /**
         * Get the audio component with the specified language
         * 
         * @param language
         *            requested language
         * @return audio component associated with the specified language
         */
        protected ServiceComponentExt getLanguageServiceComponent(String language)
        {
            ServiceComponentExt targetAudioComponent = null;
            Selection selection = getServicePresentation().waitForCurrentSelection();
            if (selection != null)
            {
                ServiceComponentExt[] components = Util.getAllMediaComponents(selection.getServiceDetails());
                for (int i = 0; i < components.length; ++i)
                {
                    ServiceComponentExt component = components[i];
                    StreamType type = component.getStreamType();
                    if (type == getComponentStreamType())
                    {
                        if (component.getAssociatedLanguage().equals(language))
                        {
                            targetAudioComponent = component;
                            break;
                        }
                    }
                }
            }
            return targetAudioComponent;
        }

        /**
         * Accessor
         * 
         * @return the currently selected components and extract the audio
         *         component
         */
        protected ServiceComponentExt getCurrentLanguageServiceComponent()
        {
            ServiceComponentExt audioComponent = null;
            Selection selection = getServicePresentation().waitForCurrentSelection();
            if (selection != null)
            {
                ServiceComponentExt[] components = selection.getCurrentComponents();
                for (int i = 0; i < components.length; ++i)
                {
                    ServiceComponentExt component = components[i];
                    StreamType type = component.getStreamType();
                    if (type == getComponentStreamType())
                    {
                        audioComponent = component;
                        break;
                    }
                }
            }
            return audioComponent;
        }
    }

    private class FreezeControlImpl extends ControlBase implements FreezeControl
    {
        FreezeControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public void freeze() throws MediaFreezeException
        {
            synchronized (getLock())
            {
                commonFreeze(true);
            }
        }

        public void resume() throws MediaFreezeException
        {
            synchronized (getLock())
            {
                commonFreeze(false);
            }
        }

        /**
         * Common Freeze/Resume implemenation.
         * 
         * @param freeze
         *            <code>True</code> means freeze; <code>false</code> means
         *            resume.
         * 
         * @throws MediaFreezeException
         *             if not presenting
         */
        private void commonFreeze(boolean freeze) throws MediaFreezeException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "commmonFreeze(" + freeze + ")");
            }

            // Spec says Player must be presenting.
            if (!isPresenting())
            {
                throw new MediaFreezeException("not presenting");
            }

            // Call native to freeze/resume.
            if (freeze)
            {
                getServicePresentation().freeze();
            }
            else
            {
                getServicePresentation().resume();
            }
        }
    }

    private static class ClosedCaptioningControlEventDispatcher extends CallerContextEventMulticaster
    {
        public void dispatch(EventListener listeners, EventObject event)
        {
            if (listeners instanceof ClosedCaptioningListener && event instanceof ClosedCaptioningEvent)
            {
                ((ClosedCaptioningListener) listeners).ccStatusChanged((ClosedCaptioningEvent) event);
            }
            else
            {
                if (Asserting.ASSERTING)
                {
                    Assert.condition(false, "listeners or event of wrong type");
                }
            }
        }
    }

    private class ClosedCaptioningControlImpl extends ControlBase implements ClosedCaptioningControl
    {
        private int state;

        private int[] currentServices;

        private int[] supportedSvc;

        private int eventId;

        private UserPreferenceManager userPreferenceManager;

        ClosedCaptioningControlImpl(boolean enabled)
        {
            super(enabled);
            state = getMediaAPI().getCCState();
            supportedSvc = getMediaAPI().getCCSupportedServiceNumbers();
            currentServices = new int[] { CC_NO_SERVICE, CC_NO_SERVICE };

            userPreferenceManager = UserPreferenceManager.getInstance();
            // Instance initialization complete...
            // Monitor for cc preference changes if the user preference manager
            // exists;
            if (userPreferenceManager != null)
            {
                try
                {
                    // examining cc prefs requires permissions
                    SecurityUtil.checkPermission(new UserPreferencePermission("read"));

                    setCCAnalogServicePrefs();
                    setCCDigitalServicePrefs();
                    setCCOnPrefs();
                }
                catch (SecurityException ex)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(getId() + "no UserPreferencePermission(read) - unable to examine CC prefs");
                    }
                }
            }
        }

        protected void release()
        {
            multicaster.cleanup();
        }

        public void setClosedCaptioningState(int newState)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setClosedCaptioningState(" + newState + ")");
            }

            checkPermission();

            if (state == newState)
            {
                return;
            }

            // change CC state
            getMediaAPI().setCCState(newState);
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setClosedCaptioningState(): STATE CHANGED");
            }
            state = newState;

            switch (newState)
            {
                case CC_TURN_ON:
                    eventId = org.ocap.media.ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_ON;
                    break;
                case CC_TURN_OFF:
                    eventId = org.ocap.media.ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_OFF;
                    break;
                case CC_TURN_ON_MUTE:
                    eventId = org.ocap.media.ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_ON_MUTE;
                    break;
                default:
                    break;
            }
            ClosedCaptioningEvent ccEvent = new ClosedCaptioningEvent(this, eventId);
            postEvent(ccEvent); // notify all listeners
        }

        public int getClosedCaptioningState()
        {
            checkPermission();

            return state;
        }

        public void setClosedCaptioningServiceNumber(int analogServiceNumber, int digitalServiceNumber)
                throws SecurityException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "setClosedCaptioningServiceNumber(" + analogServiceNumber + "," + digitalServiceNumber + ")");
            }

            checkPermission();

            if (!checkSupportedService(analogServiceNumber))
            {
                throw new IllegalArgumentException("Invalid Service Number");
            }
            if (!checkSupportedService(digitalServiceNumber))
            {
                throw new IllegalArgumentException("Invalid Service Number");
            }

            getMediaAPI().setCCServiceNumbers(analogServiceNumber, digitalServiceNumber);
            currentServices[0] = analogServiceNumber;
            currentServices[1] = digitalServiceNumber;

            eventId = org.ocap.media.ClosedCaptioningEvent.EVENTID_CLOSED_CAPTIONING_SELECT_NEW_SERVICE;
            ClosedCaptioningEvent ccEvent = new ClosedCaptioningEvent(this, eventId);
            postEvent(ccEvent);
        }

        public int[] getClosedCaptioningServiceNumber() throws SecurityException
        {
            checkPermission();

            return currentServices;
        }

        public int[] getSupportedClosedCaptioningServiceNumber()
        {
            checkPermission();

            return supportedSvc;
        }

        /*
         * 
         * Event Dispatching
         */

        /**
         * per-CallerContext EventMulticaster
         */
        private CallerContextEventMulticaster multicaster = new ClosedCaptioningControlEventDispatcher();

        public final void addClosedCaptioningListener(ClosedCaptioningListener listener)
        {
            checkPermission();

            if (isClosed())
            {
                return;
            }

            multicaster.addListenerOnce(listener);
        }

        public final void removeClosedCaptioningListener(ClosedCaptioningListener listener)
        {
            checkPermission();

            if (isClosed())
            {
                return;
            }

            multicaster.removeListener(listener);
        }

        /**
         * Post the specified {@link ClosedCaptioningEvent} to all registered
         * {@link ClosedCaptioningListener}s.
         * 
         * @param event
         *            {@link ClosedCaptioningEvent} to be posted.
         */
        private void postEvent(ClosedCaptioningEvent event)
        {
            multicaster.multicast(event);
        }

        private boolean checkSupportedService(int svc)
        {
            boolean res = false;

            // always allow 'CC_NO_SERVICE' to indicate
            // "no decoding is necessary"
            if (svc == CC_NO_SERVICE)
            {
                res = true;
            }
            else
            {
                // look thru the supported services array for a match
                for (int i = 0; i < supportedSvc.length; i++)
                {
                    if (supportedSvc[i] == svc)
                    {
                        res = true;
                        break;
                    }
                }
            }

            return res;
        }

        private void checkPermission() throws SecurityException
        {
            SecurityUtil.checkPermission(new MonitorAppPermission("handler.closedCaptioning"));
        }

        // For ecn 1316
        private synchronized void setCCOnPrefs()
        {
            GeneralPreference preference = new GeneralPreference("Closed Caption On");
            userPreferenceManager.read(preference);
            String ccPref = null;
            if (preference.hasValue()) ccPref = preference.getMostFavourite();

            if (ccPref != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "Got CC On Preference change. Setting to:" + ccPref);
                }

                if (ccPref.equals("On"))
                    setClosedCaptioningState(ClosedCaptioningControl.CC_TURN_ON);
                else if (ccPref.equals("Off")) setClosedCaptioningState(ClosedCaptioningControl.CC_TURN_OFF);
                // else? this ecn doesn?t cover CC_TURN_ON_MUTE case?
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "CC On Pref was null, not modifying pref");
                }
            }
        }

        // For ecn 1316
        private synchronized void setCCAnalogServicePrefs()
        {
            // get the digital number so we can plug it in if analog changes
            int digitalServiceNumber = (getClosedCaptioningServiceNumber())[1];
            GeneralPreference preference = new GeneralPreference("Analog Closed Caption Service");
            userPreferenceManager.read(preference);
            String ccPref = null;
            if (preference.hasValue()) ccPref = preference.getMostFavourite();

            if (ccPref != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "Got CC Analog Service Preference change. Setting to:" + ccPref);
                }

                if (ccPref.equals("CC1"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_CC1, digitalServiceNumber);
                else if (ccPref.equals("CC2"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_CC2, digitalServiceNumber);
                else if (ccPref.equals("CC3"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_CC3, digitalServiceNumber);
                else if (ccPref.equals("CC4"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_CC4, digitalServiceNumber);
                else if (ccPref.equals("T1"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_T1, digitalServiceNumber);
                else if (ccPref.equals("T2"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_T2, digitalServiceNumber);
                else if (ccPref.equals("T3"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_T3, digitalServiceNumber);
                else if (ccPref.equals("T4"))
                    setClosedCaptioningServiceNumber(CC_ANALOG_SERVICE_T4, digitalServiceNumber);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "CC Analog Service Pref was null, not modifying");
                }
            }
        }

        private synchronized void setCCDigitalServicePrefs()
        {
            // get the analog number so we can plug it in if digital changes
            int analogServiceNumber = (getClosedCaptioningServiceNumber())[0];
            GeneralPreference preference = new GeneralPreference("Digital Closed Caption Service");
            userPreferenceManager.read(preference);
            String prefString = null;
            if (preference.hasValue()) prefString = preference.getMostFavourite();

            if (prefString != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "Got CC Digital Service Pref change. Setting to:" + prefString);
                }
                int ccPref = Integer.parseInt(prefString);
                if (1 <= ccPref && ccPref <= 63) setClosedCaptioningServiceNumber(analogServiceNumber, ccPref);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "CC Digital Service Pref was null, so not modifying");
                }
            }
        }
    }

    private class MediaAccessConditionControlImpl extends ControlBase implements MediaAccessConditionControl
    {
        MediaAccessConditionControlImpl(boolean enabled)
        {
            super(enabled);
        }

        public void conditionHasChanged(MediaPresentationEvaluationTrigger trigger)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "conditionHasChanged - trigger: " + trigger);
            }
            checkPermission();

            synchronized (getLock())
            {
                if (!isEnabled() || isClosed() || !isPresenting())
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(getId() + "closed, not presenting or not enabled - not triggering reselect - enabled: "
                                + isEnabled() + ", closed: " + isClosed() + ", presenting: " + isPresenting());
                    }
                    return;
                }

                getServicePresentation().reselect(trigger);
            }
        }

        private void checkPermission() throws SecurityException
        {
            SecurityUtil.checkPermission(new MonitorAppPermission("mediaAccess"));
        }

    }

    public void notifyMediaPresented()
    {
        super.notifyMediaPresented();
        postEvent(new MediaPresentedEvent(this));
    }

    public void notifyCAStop()
    {
        stop(new CAStopEvent(this, Started, Started, Prefetched, getSource().getLocator()));
    }

    public void notifyNoData()
    {
        stop(new DataStarvedEvent(this, Started, Started, Prefetched, getMediaTime()));
    }

    public void notifyNoSource(String msg, Throwable throwable)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "notifyNoSource(): " + msg, throwable);
        }
        stop(new ServiceRemovedEvent(this, Started, Started, Realized, getSource().getLocator()));
    }

    public void notifyNoComponentSelected()
    {
        stop(new NoComponentSelectedEvent(this, Started, Started, Prefetched, getSource().getLocator()));
    }

    public void notifyAlternativeContent(Class alternativeContentClass, int alternativeContentReasonCode)
    {
        presentationModeNotifier.notifyAlternativeContent(alternativeContentClass, alternativeContentReasonCode);
    }

    public void notifyNormalContent()
    {
        presentationModeNotifier.notifyNormalContent();
    }

    public void notifyNoReasonAlternativeMediaPresentation(ElementaryStreamExt[] streams, OcapLocator locator, 
                                                           MediaPresentationEvaluationTrigger trigger, boolean digital)
    {
        if (log.isInfoEnabled())
        {
            log.info(getId() + "notifyNoReasonAlternativeMediaPresentation - streams: " + Arrays.toString(streams));
        }
        int[] deniedStreamsPids = new int[streams.length];
        int[] deniedStreamsReasons = new int[streams.length];
        for (int i=0;i<streams.length;i++)
        {
            deniedStreamsPids[i] = streams[i].getPID();
            deniedStreamsReasons[i] = 0;
        }

        ComponentAuthorization authorization = new ComponentAuthorization(streams, null, locator, trigger, digital,
                deniedStreamsPids, deniedStreamsReasons);

        MediaPresentationEvent event = authorization.getNoReasonAlternativeMediaPresentationEvent(this);
        if (log.isInfoEnabled())
        {
            log.info(getId() + "notifyNoReasonAlternativeMediaPresentation: " + event);
        }
        postEvent(event);
    }

    public void notifyMediaSelectSucceeded(Locator[] locators)
    {
        mediaSelectControl.postEvent(new MediaSelectSucceededEvent(this, locators));
    }

    public void notifyMediaSelectFailed(Locator[] locators)
    {
        mediaSelectControl.postEvent(new MediaSelectFailedEvent(this, locators));
    }

    public void notifyPresentationChanged(int reason)
    {
        postEvent(new PresentationChangedEvent(this, getSource().getLocator(), reason));
    }

    public void notifyMediaAuthorization(ComponentAuthorization authorization)
    {
        MediaPresentationEvent event = authorization.getMediaPresentationEvent(this);
        if (log.isInfoEnabled())
        {
            log.info(getId() + "notifyMediaAuthorization: " + event);
        }
        postEvent(event);
    }

    private ServicePresentation getServicePresentation()
    {
        return (ServicePresentation) getPresentation();
    }

    private class PresentationModeNotifier
    {
        private final Set presentationModeListeners = new HashSet();

        private final Object lock = new Object();

        public void addPresentationModeListener(PresentationModeListener listener)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "addPresentationModeListener: " + listener);
            }
            synchronized (lock)
            {
                presentationModeListeners.add(listener);
            }
        }

        public void removePresentationModeListener(PresentationModeListener listener)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "removePresentationModeListener: " + listener);
            }
            synchronized (lock)
            {
                presentationModeListeners.remove(listener);
            }
        }

        public void notifyNormalContent()
        {
            Collection copy = new HashSet();
            synchronized (lock)
            {
                copy.addAll(presentationModeListeners);
            }
            for (Iterator iter = copy.iterator(); iter.hasNext();)
            {
                ((PresentationModeListener) iter.next()).normalContent();
            }
        }

        public void notifyAlternativeContent(Class alternativeContentClass, int alternativeContentReasonCode)
        {
            Collection copy = new HashSet();
            synchronized (lock)
            {
                copy.addAll(presentationModeListeners);
            }
            for (Iterator iter = copy.iterator(); iter.hasNext();)
            {
                ((PresentationModeListener) iter.next()).alternativeContent(alternativeContentClass, alternativeContentReasonCode);
            }
        }
    }
}
