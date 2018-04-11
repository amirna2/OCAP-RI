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

package org.cablelabs.impl.manager.graphics;

import org.cablelabs.impl.awt.EventDispatchable;
import org.cablelabs.impl.awt.EventDispatcher;
import org.cablelabs.impl.awt.KeyboardFocusManagerFactory;
import org.cablelabs.impl.dvb.ui.DVBBufferedImagePeer;
import org.cablelabs.impl.havi.ExtendedGraphicsDevice;
import org.cablelabs.impl.havi.HaviToolkit;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.EventManager;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.GraphicsManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.progress.ProgressMgr;
import org.cablelabs.impl.manager.ProgressListener;
import org.cablelabs.impl.manager.ProgressManager;
import org.cablelabs.impl.util.JavaVersion;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.awt.Color;
import java.net.URL;
import java.awt.AWTEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;
import org.apache.log4j.Logger;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HScreen;
import org.havi.ui.HStaticIcon;
import org.ocap.ui.HSceneManager;

/**
 * Implementation of the <code>GraphicsManager</code> interface.
 * 
 * @author Aaron Kamienski
 */
public class GfxMgr implements GraphicsManager, ProgressListener
{

    // Log4J Logger
    private static final Logger log = Logger.getLogger(GfxMgr.class.getName());

    private final Container easPane;

    private final Container mmiPane;

    /**
     * Causes initialization of AWT and HAVi. Not publicly instantiable.
     */
    private GfxMgr()
    {
        // Initialize AWT
        Toolkit.getDefaultToolkit();

        // Create image factory that we will use
        imageFactory = findImageFactory();

        // Install the Event Dispatcher and Focus Handler
        if (installEventDispatcher())
        {
            installKeyboardFocusManagerFactory();
        }

        // Initialize HAVi
        HScreen.getDefaultHScreen();

        // replaced in Axiom code by installfocus handler
        // method is commented out below if needed later.

        // axiom addition
        ProgressManager pmgr = (ProgressManager) ProgressMgr.getInstance();
        pmgr.addListener(this);

        // Get the default window pane and create child containers
        // within it for EAS and MMI
        Container defaultWindowPane = HaviToolkit.getToolkit().getDefaultWindowPane();
        easPane = new MessageContainer();
        easPane.setLayout(null);
        easPane.setBounds(defaultWindowPane.getBounds());
        mmiPane = new FocusableMessageContainer();
        mmiPane.setLayout(null);
        mmiPane.setBounds(defaultWindowPane.getBounds());
        defaultWindowPane.add(easPane);
        defaultWindowPane.add(mmiPane);
        defaultWindowPane.validate();

        // Setup and show the splash screen
        showSplashScreen();
    }

    /**
     * Destroys this manager, causing it to release any and all resources.
     * Should only be called by the <code>ManagerManager</code>.
     */
    public void destroy()
    {
        imageFactory = null;
    }

    /**
     * Returns the singleton instance of <code>GraphicsManager</code>. Will be
     * called only once.
     * 
     * @return the singleton instance
     */
    public static synchronized Manager getInstance()
    {
        if (mgr == null)
        {
            mgr = new GfxMgr();
        }
        return mgr;
    }

    /**
	 *
	 */
    public void progressSignalled(int milestone)
    {
        if (null != splash)
        {
            splash.displayPip(milestone);
        }

        if (milestone == ProgressMgr.kInitialMonAppClassLoaded)
        {
            stopSplash();
        }

    }

    /**
     * Returns the singleton instance <i>factory</i> object used to create new
     * <code>DVBBufferedImagePeer</code> instances. Returns an instance of the
     * implementation appropriate for a given platform.
     * 
     * @return an instance of <code>DVBBufferedImagePeer.Factory</code> suitable
     *         for the current runtime platform
     */
    public DVBBufferedImagePeer.Factory getImageFactory()
    {
        if (imageFactory == null) throw new UnsupportedOperationException("Could not find peer implementation");
        return imageFactory;
    }

    // See GraphicsManager interface
    public HSceneManager getHSceneManager()
    {
        HGraphicsDevice gd = HScreen.getDefaultHScreen().getDefaultHGraphicsDevice();

        // The HSceneManager is also the DisplayMediator for the graphics device
        if (gd instanceof ExtendedGraphicsDevice)
        {
            ExtendedGraphicsDevice egd = (ExtendedGraphicsDevice) gd;
            return (HSceneManager) egd.getDisplayMediator();
        }

        // Should never get here
        SystemEventUtil.logRecoverableError(new Throwable(
                "The default graphics device is not an instance of ExtendedGraphicsDevice!  Returning null HSceneManager"));
        return null;
    }

    /**
     * Retrieves the <code>Container</code> that represents the plane to be used
     * to render the Emergency Alert messages. The exact implementation of this
     * <code>Container</code> is unspecified.
     * <p>
     * The size of the retrieved container should not be modified. It will
     * automatically be adjusted to cover the maximum area of the screen allowed
     * by the implementation. The caller can make use of a
     * <code>ComponentListener</code> in order to be notified of changes in
     * size.
     * <p>
     * It is expected that only one <i>caller</i> access this container at a
     * time; otherwise the results are undefined.
     * <p>
     * It is expected that the <i>caller</i> limit its interaction with the
     * retrieved <code>Container</code> to the following methods:
     * <ul>
     * <li> {@link java.awt.Component#setVisible}
     * <li> {@link java.awt.Container#add(java.awt.Component)}
     * <li> {@link java.awt.Container#remove(java.awt.Component)}
     * <li> {@link java.awt.Container#setLayout}
     * <li> {@link java.awt.Component#repaint()}
     * <li> {@link java.awt.Component#addComponentListener} (for being notified
     * of size changes)
     * <li> {@link java.awt.Component#removeComponentListener}
     * </ul>
     * 
     * @return the <code>Container</code> representing the Emergency Alert
     *         display plane
     */
    public Container getEmergencyAlertPlane()
    {
        return easPane;
    }

    /**
     * Retrieves the <code>Container</code> that represents the plane to be used
     * to render MMI dialog messages by the resident MMI handler. The exact
     * implementation of this <code>Container</code> is unspecified.
     * <p>
     * The size of the retrieved container should not be modified. It will
     * automatically be adjusted to cover the maximum area of the screen allowed
     * by the implementation. The caller can make use of a
     * <code>ComponentListener</code> in order to be notified of changes in
     * size.
     * <p>
     * It is expected that only one <i>caller</i> access this container at a
     * time; otherwise the results are undefined.
     * <p>
     * It is expected that the <i>caller</i> limit its interaction with the
     * retrieved <code>Container</code> to the following methods:
     * <ul>
     * <li> {@link Container#setVisible}
     * <li> {@link Container#add}
     * <li> {@link Container#remove}
     * <li> {@link Container#setLayout}
     * <li> {@link Container#repaint}
     * <li> {@link Container#addComponentListener} (for being notified of size
     * changes)
     * <li> {@link Container#removeComponentListener}
     * </ul>
     * 
     * @return the <code>Container</code> representing the Emergency Alert
     *         display plane
     */
    public Container getMMIDialogPlane()
    {
        return mmiPane;
    }

    /**
     * Implements
     * {@link org.cablelabs.impl.manager.GraphicsManager#setVisible(boolean)}.
     * using {@link HaviToolkit#setGraphicsVisible}.
     */
    public void setVisible(boolean visible)
    {
        // Currently we disable all graphics
        HaviToolkit.getToolkit().setGraphicsVisible(visible);
    }

    /**
     * Initializes <i>imageFactory</i> attribute.
     * <p>
     * Searches for the proper implementation using the following algorithm:
     * <ol>
     * <li>Looks for AWT-specific impl provided by AWT port
     * (DVBBufferedImagePeer0).
     * <li>Looks for Java2-specific impl (DVBBufferedImagePeer2), if JAVA_2.
     * <li>Finally falls back on Java1 impl (DVBBufferedImagePeer1).
     * </ol>
     */
    private DVBBufferedImagePeer.Factory findImageFactory()
    {
        String pfx = "org.cablelabs.impl.dvb.ui.DVBBufferedImagePeer";
        String sfx = "$Factory";

        // First try AWT-specific impl provided by AWT port
        Object f = null;
        if ((f = newInstanceOf(pfx + "0" + sfx)) == null)
        {
            // Try Java2-specific version
            if (!JavaVersion.JAVA_2 || (f = newInstanceOf(pfx + "2" + sfx)) == null)
            // Finally fall-back to generic Java1 version
                f = newInstanceOf(pfx + "1" + sfx);
        }
        try
        {
            return (DVBBufferedImagePeer.Factory) f;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Attempts to load the class for the given <i>className</i> and create an
     * instance of it.
     * 
     * @param className
     *            the name of the implementation class to use
     * @return an instance if successful, <code>null</code> otherwise
     */
    private static Object newInstanceOf(String className)
    {
        try
        {
            Class clazz = Class.forName(className);
            return clazz.newInstance();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Installs the <code>EventDispatcher</code> provided by the
     * <code>EventManager</code> implementation.
     * <p>
     * This operation is performed only if the <code>java.awt.Toolkit</code>
     * implements the {@link org.cablelabs.impl.awt.EventDispatchable}
     * interface.
     * <p>
     * This will implicitly start the <code>EventManager</code> if it hasn't
     * been started already.
     * 
     * @return <code>true</code> if successful, <code>false</code> if the
     *         dispatcher wasn't installed
     */
    private boolean installEventDispatcher()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        EventDispatcher dispatcher;
        if ((tk instanceof EventDispatchable)
                && ((dispatcher = (EventDispatcher) ManagerManager.getInstance(EventManager.class)) != null))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Installing " + dispatcher + " in " + tk);
            }
            ((EventDispatchable) tk).setEventDispatcher(dispatcher);

            return true;
        }
        return false;
    }

    /**
     * Installs the <code>FocusHandler</code> provided by the
     * <code>FocusHManager</code> implementation.
     * <p>
     * This operation should be performed only if an
     * <code>EventDispatcher</code> is installed by
     * {@link #installEventDispatcher}.
     * <p>
     * This will implicitly start the <code>FocusManager</code> if it hasn't
     * been started already.
     */
    /*
     * Axiom private void installFocusHandler() { FocusManager fm =
     * (FocusManager) ManagerManager.getInstance(FocusManager.class);
     * FocusHandler fh = null; if ( fm != null && (fh =
     * fm.getFocusOwnerContext() != null ) { FocusHandler.setFocusHandler(fh);
     * if ( Logging.LOGGING ) log.debug("Installing "+ fh + " as FocusHandler");
     * } }
     */

    /*
     * Installs the <code>KeyboardFocusManager</code> provided by the
     * <code>FocusManager</code> implementation. <p> This operation should be
     * performed only if an <code>EventDispatcher</code> is installed by {@link
     * #installEventDispatcher}. <p> This will implicitly start the
     * <code>FocusManager</code> if it hasn't been started already.
     */
    private void installKeyboardFocusManagerFactory()
    {
        FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
        if (fm != null && fm instanceof KeyboardFocusManagerFactory)
        {
            KeyboardFocusManagerFactory.setKFMFactory((KeyboardFocusManagerFactory) fm);
        }
    }

    /**
     * Splash screen resource names searched for along the CLASSPATH.
     */
    private static final String[] SPLASH_RESOURCES = { "/splash.png", "/splash.jpg", "/splash.gif", "/splash.jpeg", };

    /**
     * Locate the splash screen resource by searching for an explicitly-named
     * file (if given) or along the CLASSPATH.
     * <p>
     * <ol>
     * <li> <code>MPEEnv.getEnv("OCAP.splash.file")</code> is consulted first; If
     * defined, it is used to locate the splash file.
     * <li>If not defined, then the following resources are searched for on the
     * CLASSPATH:
     * <ul>
     * <li> <code>"/splash.png"</code>
     * <li> <code>"/splash.jpg"</code>
     * <li> <code>"/splash.gif"</code>
     * <li> <code>"/splash.jpeg"</code>
     * </ul>
     * </ul>
     * 
     * @return a <code>URL</code> for the resource; <code>null</code> if not
     *         found
     */
    private URL findSplashResource()
    {
        try
        {
            // Read from specified file
            String var;
            if ((var = MPEEnv.getEnv("OCAP.splash.file")) != null)
            {
                File f = new File(var);
                if (f.exists())
                    return f.toURL();

                if (log.isWarnEnabled())
                {
                    log.warn("Splash file does not exist: " + var);
                }
                return null;
            }

            // Else, try classpath
            URL splashUrl = null;
            for (int i = 0; splashUrl == null && i < SPLASH_RESOURCES.length; ++i)
                splashUrl = getClass().getResource(SPLASH_RESOURCES[i]);
            return splashUrl;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Setup and display the startup splash screen.
     */
    private void showSplashScreen()
    {
        URL splashUrl = findSplashResource();
        if (splashUrl == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("No splash screen resource found");
            }
            return;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Displaying splash: " + splashUrl);
            }
        }

        // Load the image
        Image splashImage = Toolkit.getDefaultToolkit().createImage(splashUrl);

        // Get the parent container
        Container pane = getEmergencyAlertPlane();

        // Create the display component
        splash = new Splash(splashImage, pane);

        // Add splash screen for display -- will remove itself after timeout
        pane.add(splash);
        splash.repaint();
    }

    /**
     * Simple implementation of a splash screen.
     * <p>
     * <table border>
     * <tr>
     * <th>What?</th>
     * <th>Default</th>
     * <th>Override variable</th>
     * </tr>
     * <tr>
     * <td>Time to display splash</td>
     * <td>3000ms</td>
     * <td><code>"OCAP.splash.duration"</code></td>
     * </tr>
     * <tr>
     * <td>Max time to wait for image</td>
     * <td>3000ms</td>
     * <td><code>"OCAP.splash.wait"</code></td>
     * </tr>
     * </table>
     * 
     * @author Aaron Kamienski
     */
    private class Splash extends HStaticIcon implements TVTimerWentOffListener
    {

        /** from axiom **/
        private boolean painted = false;

        private boolean milestone[] = new boolean[ProgressMgr.kNumMilestones];

        /**
         * Creates a <code>Splash</code> component using the given <i>splash</i>
         * <code>Image</code>.
         * 
         * @param splash
         *            the image to display
         * @param parent
         *            the intended parent container; used to size this component
         */
        Splash(Image splash, Container parent)
        {
            super(splash);
            setSize(parent.getSize());

            // Let's load the image, if not already loaded
            tracker = new MediaTracker(this);
            tracker.addImage(splash, 0);
            try
            {
                // Pause loading to load image -- barely
                tracker.waitForID(0, MPEEnv.getEnv("OCAP.splash.wait", 3000L));
            }
            catch (InterruptedException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Exception waiting for splash", e);
                }
            }
        }

        /**
         * Invokes {@link #stop} after the timeout expires.
         */
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            stop();
        }

        private static final int kOriginX = 274;

        private static final int kOriginY = 300;

        private static final int kSide = 10;

        private static final int kSpacing = 15;

        private final Color bgColor = Color.gray;

        private final Color fgColor = Color.orange;

        /*
         * The following ..pip() methods are for displaying the progress bar
         * durin the boot process Based on the received signals at various
         * points, the progress bar is painted on the splash screen. Once we get
         * the first monitor app the splash screen along with the progress bar
         * is torn down.
         */

        /*
         * Turns on the ith milestone.
         */
        private void displayPip(int i)
        {
            milestone[i] = true;
            repaint();
        }

        /**
         * Paints the milestone. Default color of Gray and a color of Orange if
         * a milestone is reached.
         */
        private void paintPip(Graphics g, int i, boolean signalled)
        {
            g.setColor(bgColor);
            if (signalled)
            {
                g.setColor(fgColor);
            }
            int x = kOriginX + (i * kSpacing);
            g.fillRect(x, kOriginY, kSide, kSide);
            g.setColor(bgColor);
        }

        /**
         * Performs the paint operation. If the splash image was loaded prior to
         * the paint, then the {@link #setupTimeout timer} is setup (if not
         * already). After painting, if a previous <code>setupTimeout()</code>
         * failed, then will call {@link #stop} to dispose of the splash
         * display.
         */
        public void paint(Graphics g)
        {
            boolean loaded = tracker.checkID(0);
            Rectangle clip = g.getClipBounds();
            if (false == painted)
            {
                painted = true;
                super.paint(g);
            }
            else
            {
                super.paint(g);
            }

            // Draw progress milestones
            for (int i = 0; i < ProgressMgr.kNumMilestones; i++)
            {
                paintPip(g, i, milestone[i]);
            }

            if (removeOnPaint)
            {
                stop();
            }
            // don't consider it unless it paints entire image!
            if (loaded && clip.width == getWidth() && clip.height == getHeight())
            {
                setupTimeout();
            }
        }

        /**
         * Sets up a timer for 3sec. When the timer expires {@link #stop} will
         * be called to dispose of the splash display. If any problems are
         * encountered, then {@link #removeOnPaint} will be set such that the
         * next {@link #paint} will remove this component. The {@link #timeout}
         * variable is used to determine if we've attempted a timeout before or
         * not.
         */
        private void setupTimeout()
        {
            if (timeout)
            {
                return;
            }
            timeout = true;

            try
            {
                TVTimer timer = TVTimer.getTimer();
                TVTimerSpec spec = new TVTimerSpec();
                spec.setDelayTime(MPEEnv.getEnv("OCAP.splash.duration", 3000L));
                spec.addTVTimerWentOffListener(this);
                spec = timer.scheduleTimerSpec(spec);
                if (log.isDebugEnabled())
                {
                    log.debug("Splash will timeout in " + spec.getTime());
                }
            }
            catch (Throwable e)
            {
                SystemEventUtil.logRecoverableError(new Exception("Could not set splash timer " + e));
                removeOnPaint = true;
            }
        }

        /**
         * Disposes of the splash display.
         */
        private void stop()
        {
            this.setVisible(false);
            Container parent = getParent();
            if (parent != null)
            {
                parent.remove(this);
            }
        }

        /** Used to track the loading of the splash image. */
        private MediaTracker tracker;

        /** Indicates whether timeout has been setup or not. */
        private boolean timeout;

        /** Indicates that <i>this</i> should be removed following paint. */
        private boolean removeOnPaint;
    }

    /**
     * The singleton instance.
     */
    private static GraphicsManager mgr;

    /**
     * The singleton DVBBufferedImagePeer.Factory.
     */
    private DVBBufferedImagePeer.Factory imageFactory;

    private Splash splash;

    public void stopSplash()
    {
        if (null != splash)
        {
            splash.stop();
        }
        splash = null;
    }

    /**
     * Container for system messages. Automatically resizes upon configuration
     * changes to fill the entire screen.
     * 
     * @see HDGraphicsDevice#GfxContainer
     */
    private static class MessageContainer extends Container
    {
        MessageContainer()
        {
            setFocusable(false);
        }

        /**
         * Resize the component to fill the given bounds and also update sub
         * containers.
         * 
         * @param bounds
         *            new bounds
         */
        void updateBounds(Rectangle bounds)
        {
            // TODO: make sure that size updates work correctly!
            setBounds(bounds);
        }
    }

    /**
     * Container for system messages that can receive input focus. Automatically
     * resizes upon configuration changes to fill the entire screen.
     * 
     * @see HDGraphicsDevice#GfxContainer
     */
    private static class FocusableMessageContainer extends MessageContainer implements FocusManager.FocusContext,
            FocusManager.RootContainer
    {
        final private CallerContext context;

        private Component requestedFocus = null;

        private Component currentFocus = null;

        private boolean temporary = false;

        FocusableMessageContainer()
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            context = ccm.getCurrentContext();
        }

        public void notifyActivated()
        {
            final Component focusLost;
            final Component focusGained;
            final boolean temporaryChange;

            // Determine whether a focus change is occurring.
            synchronized (this)
            {
                if ((null != currentFocus) && (currentFocus != requestedFocus))
                {
                    focusLost = currentFocus;
                }
                else
                {
                    focusLost = null;
                }

                if (null != requestedFocus)
                {
                    currentFocus = requestedFocus;
                    focusGained = requestedFocus;
                }
                else
                {
                    focusGained = null;
                }

                temporaryChange = temporary;
            }

            // Was there a focus change?
            if ((null == focusLost) && (null == focusGained))
            {
                return;
            }

            // Send focus change events within caller's context
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    if (null != focusLost)
                    {
                        focusLost.dispatchEvent(new FocusEvent(focusLost, FocusEvent.FOCUS_LOST, temporaryChange));
                    }
                    if (null != focusGained)
                    {
                        focusGained.dispatchEvent(new FocusEvent(focusGained, FocusEvent.FOCUS_GAINED, temporary));
                    }
                }
            });
        }

        public void notifyDeactivated()
        {
            final Component oldFocus;

            synchronized (this)
            {
                if (null != currentFocus)
                {
                    oldFocus = currentFocus;
                    currentFocus = null;
                }
                else
                {
                    oldFocus = null;
                }
            }

            if (null != oldFocus)
            {
                CallerContext.Util.doRunInContextSync(context, new Runnable()
                {
                    public void run()
                    {
                        oldFocus.dispatchEvent(new FocusEvent(oldFocus, FocusEvent.FOCUS_LOST, false));
                    }
                });
            }
        }

        public int getPriority()
        {
            return PRIORITY_HIGH;
        }

        public void dispatchEvent(final AWTEvent e, FocusManager.DispatchFilter filter, boolean interestFilter)
        {
            final Component target;

            synchronized (this)
            {
                if ((null != currentFocus) && (e instanceof KeyEvent))
                {
                    target = currentFocus;
                }
                else
                {
                    target = null;
                }
            }

            if (null != target)
            {
                CallerContext.Util.doRunInContextSync(context, new Runnable()
                {
                    public void run()
                    {
                        KeyEvent ke = (KeyEvent) e;
                        Object oldSource = ke.getSource();
                        ke.setSource(target);
                        target.dispatchEvent(e);
                        ke.setSource(oldSource);
                        ke.consume();
                    }
                });
            }
        }

        public void clearFocus()
        {
            notifyDeactivated();

            synchronized (this)
            {
                requestedFocus = null;
            }
        }

        public Component getFocusOwner()
        {
            synchronized (this)
            {
                return currentFocus;
            }
        }

        public void handleRequestFocus(Component c, boolean temporary)
        {
            FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

            synchronized (this)
            {
                requestedFocus = c;
                this.temporary = temporary;
            }

            fm.requestActivate(this, true);
        }

        public void setVisible(boolean show)
        {
            super.setVisible(show);

            if (!show)
            {
                FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);
                fm.notifyDeactivated(this);
            }
        }
    }
}
