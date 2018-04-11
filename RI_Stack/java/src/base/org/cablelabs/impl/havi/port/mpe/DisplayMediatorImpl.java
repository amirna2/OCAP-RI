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

package org.cablelabs.impl.havi.port.mpe;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.util.Vector;

import org.cablelabs.impl.ocap.manager.eas.EASState;
import org.dvb.application.AppID;
import org.havi.ui.HContainer;
import org.havi.ui.HScene;
import org.havi.ui.HTextValue;
import org.ocap.application.OcapAppAttributes;
import org.ocap.system.MonitorAppPermission;
import org.ocap.ui.HSceneBinding;
import org.ocap.ui.HSceneChangeRequestHandler;
import org.ocap.ui.HSceneManager;

import org.cablelabs.impl.havi.DefaultScene;
import org.cablelabs.impl.havi.DefaultSceneFactory;
import org.cablelabs.impl.havi.DisplayMediator;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * The {@link DisplayMediator} interface defines the mediator between the
 * {@link DefaultScene}/{@link DefaultSceneFactory} and the platform-specific
 * parent of the <code>HScene</code>.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @version $Revision: 1.10 $ $Date: 2002/06/03 21:31:03 $
 */
public class DisplayMediatorImpl extends HSceneManager implements DisplayMediator
{
    /** The frame that represents the screen */
    private Frame frame;

    /** The root container that represents the screen */
    // private Container root;
    /** The container that represents the graphics device */
    private HContainer deviceContainer;

    /**
     * Construct an HScene mediator
     * 
     * @param frame
     *            the frame that represents the screen
     * @param root
     *            the root container that represents the screen
     * @param container
     *            the container that represents the graphics device
     */
    DisplayMediatorImpl(Frame frame, Container root, HContainer container)
    {
        this.frame = frame;
        // this.root = root;
        deviceContainer = container;
    }

    // Defined in DisplayMediator
    public void add(HScene scene)
    {
        deviceContainer.add(scene);
    }

    // Defined in DisplayMediator
    public void remove(HScene scene)
    {
        deviceContainer.remove(scene);
    }

    // Defined in DisplayMediator
    public Container getContainer()
    {
        return deviceContainer;
    }

    // Defined in DisplayMediator
    public void popToFront(HScene scene)
    {
        boolean repaint = false;
        synchronized (deviceContainer.getTreeLock())
        {
            // Scene must already be present in the z-order and not already at the front
            if (deviceContainer.isAncestorOf(scene) && deviceContainer.getComponent(0) != scene)
            {
                Component focusOwner = scene.getFocusOwner();
                repaint = deviceContainer.popToFront(scene) && scene.isVisible();
                
                // Make sure we restore focus here.  When a component is moved to a new location
                // within the container, it is removed (which causes automatic loss of focus)
                // then re-added.
                if (focusOwner != null)
                {
                    int retries = 3;
                    while (scene.getFocusOwner() != null && retries-- > 0)
                    {
                        try
                        {
                            Thread.sleep(50);
                        }
                        catch (InterruptedException e) { }
                    }
                    focusOwner.requestFocus();
                }
            }
        }
        if (repaint) scene.repaint();
    }

    // Defined in DisplayMediator
    public void pushToRear(HScene scene)
    {
        boolean repaint = false;
        synchronized (deviceContainer.getTreeLock())
        {
            // Scene must already be present in the z-order and not already at the rear
            if (deviceContainer.isAncestorOf(scene) &&
                deviceContainer.getComponent(deviceContainer.getComponents().length - 1) != scene)
            {
                Component focusOwner = scene.getFocusOwner();
                
                repaint = deviceContainer.pushToBack(scene) && scene.isVisible();
                
                // Make sure we restore focus here.  When a component is moved to a new location
                // within the container, it is removed (which causes automatic loss of focus)
                // then re-added.
                if (focusOwner != null)
                {
                    int retries = 3;
                    while (scene.getFocusOwner() != null && retries-- > 0)
                    {
                        try
                        {
                            Thread.sleep(50);
                        }
                        catch (InterruptedException e) { }
                    }
                    focusOwner.requestFocus();
                }
            }
        }
        if (repaint) scene.repaint();
    }

    // Defined in DisplayMediator
    public void addKeyListener(HScene scene, KeyListener l)
    {
        frame.addKeyListener(l);
    }

    // Defined in DisplayMediator
    public void removeKeyListener(HScene scene, KeyListener l)
    {
        frame.removeKeyListener(l);
    }

    // Defined in DisplayMediator
    public boolean isOnDevice(Component component)
    {
        return deviceContainer.isAncestorOf(component);
    }

    // Defined in DisplayMediator
    public Component getFocusOwner(HScene scene)
    {
        Component c = frame.getFocusOwner();

        if (scene.isAncestorOf(c)) return c;

        return null;
    }

    // Defined in HSceneManager
    public int getAppHSceneLocation()
    {
        // Check the CallerContexts of all the scenes and return the
        // position of this caller. Scenes that are not visible do not
        // factor into z-ordering
        Component[] components = deviceContainer.getComponents();
        for (int i = 0, zorder = 0; i < components.length; ++i)
        {
            if (components[i].isVisible())
            {
                if (((DefaultScene) components[i]).isOwnedByCaller()) return zorder;
                zorder++;
            }
        }

        return -1;
    }

    // Defined in HSceneManager
    public void setHSceneChangeRequestHandler(HSceneChangeRequestHandler h)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.resource"));

        synchronized (lock)
        {
            Handler oldHandler = handler;
            handler = (h == null) ? (new Handler()) : (new ContextHandler(h));
            oldHandler.dispose();
        }
    }

    /**
     * Return the current handler.
     * 
     * @return the current handler
     */
    private Handler getHandler()
    {
        synchronized (lock)
        {
            return handler;
        }
    }

    /**
     * Cleanup the currently set handler.
     * 
     * @param h
     *            the handler to clear
     */
    private void clearHandler(Handler h)
    {
        synchronized (lock)
        {
            if (handler == h)
            {
                handler = new Handler();
                h.dispose();
            }
        }
    }

    // Defined in DisplayMediator
    public boolean checkNewPositionAndSize(HSceneBinding move)
    {
        return getHandler().checkNewPositionAndSize(move);
    }

    // Defined in DisplayMediator
    public boolean checkShow(HSceneBinding show)
    {
        if (!allowVisibility)
        {
            AppID superApp = getSuperApp();

            // Make sure this is not the superApp requesting to be shown
            if (superApp != null && superApp.equals(show.getAppAttributes().getIdentifier())) return true;

            return false;
        }

        return getHandler().checkShow(show);
    }

    // Defined in DisplayMediator
    public boolean checkPopToFront(HScene scene)
    {
        return getHandler().checkPopToFront(scene);
    }

    // Definition copied from superclass
    public boolean showVirtualKeyboard(HTextValue editComponent)
    {
        // unimplemented
        return false;
    }

    // Definition copied from superclass
    public void hideVirtualKeyboard()
    {
        // unimplemented
    }

    // Definition copied from superclass
    public void setVisible(boolean visible)
    {
        allowVisibility = visible;

        // If we are disallowing visibility, set each scene individually to
        // "not visible" (with the exception of the EAS super app)
        if (!visible)
        {
            AppID superApp = getSuperApp();

            // Get all the scenes currently on the display
            Component[] components = deviceContainer.getComponents();
            for (int i = 0; i < components.length; ++i)
            {
                HSceneBinding sb = ((DefaultScene) components[i]).getBinding();
                boolean isSuperApp = superApp != null && superApp.equals(sb.getAppAttributes().getIdentifier());

                // Only change the visibility of this scene if it is not the
                // super app
                // and it is currently visible
                if (!isSuperApp && components[i].isVisible()) components[i].setVisible(false);
            }
        }
        else
        {
            Component[] components = deviceContainer.getComponents();
            for (int i = 0; i < components.length; ++i)
            {
                components[i].setVisible(true);
            }
        }
    }

    // Definition copied from superclass
    public WindowEvent createWindowEvent(HScene scene, int eventId)
    {
        return new SceneEvent(scene, eventId);
    }

    // Defined in HSceneManager
    protected OcapAppAttributes[] getHSceneOrderImpl()
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.resource"));

        Component[] components = deviceContainer.getComponents();

        Vector appAttributes = new Vector();
        for (int i = 0; i < components.length; ++i)
        {
            if (components[i].isVisible())
                appAttributes.addElement(((DefaultScene) components[i]).getBinding().getAppAttributes());
        }

        OcapAppAttributes[] attr = new OcapAppAttributes[appAttributes.size()];
        appAttributes.toArray(attr);

        return attr;
    }

    /*
     * TODO: For 0.9.5ER3, we are just allowing this one EAS app to serve as a
     * "super app" that can override the global scene visibility flag. This is
     * not exactly what we want to be doing here. In the future, we may want to
     * either create a new MPEEnv var that can contain multiple superApp IDs or
     * require the use of an extension API to override the global visibility
     * flag
     */
    private AppID getSuperApp()
    {
        AppID superApp = EASState.getEASHostKeyListenerAppID();
        return (superApp.equals(EASState.INVALID_APP_ID)) ? null : superApp;
    }

    /**
     * Extends the <code>WindowEvent</code> class to add a constructor which
     * takes an <code>HScene</code>. The constructor still takes a non-null
     * <code>Window</code>, but this is not used passed the constructor (it is
     * simply to workaround the fact that the only <code>WindowEvent</code>
     * constructor takes a <code>Window</code> as opposed to a
     * <code>Component</code> as its source.
     */
    private class SceneEvent extends WindowEvent
    {
        /**
         * Constructs a new <code>WindowEvent</code> with the given
         * <code>id</code> and <code>scene</code> as the source. The
         * <code>Window</code> parameter is necessary for proper construction,
         * but is never referenced again.
         * 
         * @param scene
         *            the event source
         * @param id
         *            the event id; should be one of
         *            <ul>
         *            <li> <code>WindowEvent.WINDOW_ACTIVATED</code>
         *            <li> <code>WindowEvent.WINDOW_CLOSED</code>
         *            <li> <code>WindowEvent.WINDOW_CLOSING</code>
         *            <li> <code>WindowEvent.WINDOW_DEACTIVATED</code>
         *            <li> <code>WindowEvent.WINDOW_DEICONIFIED</code>
         *            <li> <code>WindowEvent.WINDOW_ICONIFIED</code>
         *            <li> <code>WindowEvent.WINDOW_OPENED</code>
         *            </ul>
         */
        public SceneEvent(HScene scene, int id)
        {
            super(frame, id);
            source = scene; // forget about the original window
        }
    }

    /**
     * Represents the default policy for handling <code>HScene</code> requests
     * when there isn't an installed {@link HSceneChangeRequestHandler}. Defines
     * the <i>handler</i> interface used within the implementation.
     * 
     * @see #checkNewPositionAndSize
     * @see DisplayMediatorImpl#checkNewPositionAndSize
     * @see #checkShow
     * @see DisplayMediatorImpl#checkShow
     * @see #checkPopToFront
     * @see DisplayMediatorImpl#checkPopToFront
     * 
     * @author Aaron Kamienski
     */
    private class Handler
    {
        /**
         * Returns whether or not the proposed scene position and size is
         * allowed by the monitor application
         * <p>
         * The default implementation always returns <code>true</code>.
         * 
         * @param move
         *            the scene that is requesting a change in position/size
         * @return true if the change in size and/or position is allowed or if
         *         there is no registered monitor application, false if the
         *         change is not allowed
         */
        boolean checkNewPositionAndSize(HSceneBinding move)
        {
            return true;
        }

        /**
         * Returns whether or not the given scene is allowed to be visible by
         * the monitor application
         * <p>
         * The default implementation always returns <code>true</code>.
         * 
         * @param show
         *            the scene that is requesting to be displayed
         * @return true if the scene is allowed to be displayed or if there is
         *         no registered monitor application, false if the change is not
         *         allowed
         */
        boolean checkShow(HSceneBinding show)
        {
            return true;
        }

        /**
         * Returns whether or not the given scene is allowed to move to the
         * front of the z-order list
         * <p>
         * The default implementation always returns <code>true</code>.
         * 
         * @param scene
         *            the scene that is requesting to be move to the front of
         *            the z-order list
         * @return true if the scene is allowed to move to the front of the
         *         z-order list or if there is not registered monitor
         *         application, false if the change is not allowed
         */
        boolean checkPopToFront(HScene scene)
        {
            return true;
        }

        /**
         * Perform any cleanup necessary when this handler is no longer used.
         * The default implementation does nothing.
         */
        void dispose()
        {
            // empty
        }
    }

    /**
     * Represents an installed {@link HSceneChangeRequestHandler}.
     * 
     * @author Aaron Kamienski
     */
    private class ContextHandler extends Handler implements CallbackData
    {
        ContextHandler(HSceneChangeRequestHandler h)
        {
            this.h = h;

            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            this.cc = ccm.getCurrentContext();

            cc.addCallbackData(this, ContextHandler.class);
        }

        /**
         * Implements
         * {@link Handler#checkNewPositionAndSize(org.ocap.ui.HSceneBinding)} by
         * invoking {@link HSceneChangeRequestHandler#testMove} within the
         * proper <code>CallerContext</code>.
         */
        boolean checkNewPositionAndSize(final HSceneBinding move)
        {
            // Create array of binding for all the visible scenes currently in
            // the z-order list
            Component[] components = deviceContainer.getComponents();
            Vector visibleScenes = new Vector();
            for (int i = 0; i < components.length; ++i)
            {
                if (components[i].isVisible()) visibleScenes.addElement(((DefaultScene) components[i]).getBinding());
            }

            final HSceneBinding[] currentScenes = new HSceneBinding[visibleScenes.size()];
            visibleScenes.toArray(currentScenes);

            // Can we perform the change?
            final boolean[] result = { true };
            CallerContext.Util.doRunInContextSync(cc, new Runnable()
            {
                public void run()
                {
                    result[0] = h.testMove(move, currentScenes);
                }
            });
            return result[0];
        }

        /**
         * Implements {@link Handler#checkPopToFront(org.havi.ui.HScene)} by
         * invoking {@link HSceneChangeRequestHandler#testOrder} within the
         * proper <code>CallerContext</code>.
         */
        boolean checkPopToFront(HScene scene)
        {
            int currentSceneLocation = -1;
            // Create array of binding for all the visible scenes currently
            // in the z-order list. Additionally, determine where in the z-order
            // the given scene is located
            Component[] components = deviceContainer.getComponents();
            Vector visibleScenes = new Vector();
            for (int i = 0, zorder = 0; i < components.length; ++i)
            {
                if (components[i].isVisible())
                {
                    if (components[i] == scene) currentSceneLocation = zorder;

                    zorder++;

                    visibleScenes.addElement(((DefaultScene) components[i]).getBinding());
                }
            }

            // If we didn't find the requested scene in the z-order, then it is
            // either
            // not in the container or not visible (shouldn't happen). In either
            // case
            // just allow the move.
            if (currentSceneLocation == -1) return true;

            final HSceneBinding[] currentScenes = new HSceneBinding[visibleScenes.size()];
            visibleScenes.toArray(currentScenes);

            // Can we perform the change?
            final boolean[] result = { true };
            final int curSceneLoc = currentSceneLocation;
            CallerContext.Util.doRunInContextSync(cc, new Runnable()
            {
                public void run()
                {
                    result[0] = h.testOrder(currentScenes, curSceneLoc, 0);
                }
            });
            return result[0];
        }

        /**
         * Implements {@link Handler#checkShow(org.ocap.ui.HSceneBinding)} by
         * invoking
         * {@link HSceneChangeRequestHandler#testShow(HSceneBinding, HSceneBinding[])}
         * within the associated <code>CallerContext</code>.
         */
        boolean checkShow(final HSceneBinding show)
        {
            // Create array of binding for all the scenes currently in the
            // z-order list
            Component[] components = deviceContainer.getComponents();
            Vector visibleScenes = new Vector();

            for (int i = 0; i < components.length; ++i)
            {
                if (components[i].isVisible()) visibleScenes.addElement(((DefaultScene) components[i]).getBinding());
            }

            final HSceneBinding[] currentScenes = new HSceneBinding[visibleScenes.size()];
            visibleScenes.toArray(currentScenes);

            // Can we perform the change?
            final boolean[] result = { true };
            CallerContext.Util.doRunInContextSync(cc, new Runnable()
            {
                public void run()
                {
                    result[0] = h.testShow(show, currentScenes);
                }
            });
            return result[0];
        }

        public void active(CallerContext callerContext)
        { /* empty */
        }

        public void pause(CallerContext callerContext)
        { /* empty */
        }

        public void destroy(CallerContext callerContext)
        {
            clearHandler(this);
        }

        /**
         * Removes this <code>CallbackData</code> object from the associated
         * <code>CallerContext</code> to avoid any data leaks.
         */
        void dispose()
        {
            cc.removeCallbackData(ContextHandler.class);
        }

        private final HSceneChangeRequestHandler h;

        private final CallerContext cc;
    }

    private boolean allowVisibility = true;

    private Handler handler = new Handler();

    private Object lock = new Object();
}
