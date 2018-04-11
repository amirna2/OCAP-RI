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

package org.cablelabs.impl.havi;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.BitSet;

import org.apache.log4j.Logger;

import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.havi.ui.HActionInputPreferred;
import org.havi.ui.HActionable;
import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenDimension;
import org.havi.ui.HScreenPoint;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.event.HActionEvent;
import org.havi.ui.event.HEventGroup;
import org.ocap.application.OcapAppAttributes;
import org.ocap.ui.HSceneBinding;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.FocusManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.FocusManager.DispatchFilter;
import org.cablelabs.impl.manager.FocusManager.FocusContext;
import org.cablelabs.impl.manager.FocusManager.RootContainer;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * An {@link HScene} is a container representing the displayable area on-screen
 * within which the application can display itself and thus interact with the
 * user.
 * 
 * <p>
 * {@link HScene} may be regarded as a simple connection to the window
 * management policy within the device, acting as a "screen resource reservation
 * mechanism" denoting the area within which an application may present
 * components.
 * 
 * <h3>Rendering Behavior</h3>
 * 
 * By default, {@link HScene} does not paint itself on-screen, only its added
 * "child" components and hence its only immediate graphical effect is to "clip"
 * its child components. However, it is possible to request that the entire
 * {@link HScene} be painted in the current background color before any drawing
 * takes place, and/or that a background image be drawn before the children are
 * rendered.
 * 
 * <h3>Class Behavior</h3>
 * 
 * For all interoperable applications, the {@link HScene} is considered the
 * top-level component of the application. No parent component to an
 * {@link HScene} should be accessible to applications. Interoperable
 * applications should not use the getParent method in {@link HScene}, since
 * results are implementation dependent and valid implementations may generate a
 * run-time error.
 * 
 * <p>
 * Although {@link HScene} is a subclass of <code>java.awt.Container</code>,
 * implementations are allowed to insert extra classes in the inheritance tree
 * between {@link HScene} and <code>Container</code>. It is allowed that this
 * may result in {@link HScene} inheriting additional methods beyond those
 * specified here. This allows platforms with only one native
 * <code>java.awt.Frame</code> to use {@link HScene} as specified, whereas
 * platforms with support for multiple <code>java.awt.Frame</code> or
 * <code>java.awt.Window</code> classes can use an {@link HScene} class derived
 * from the appropriate class.
 * 
 * <p>
 * 
 * {@link HScene HScenes} follow the design pattern of the
 * <code>java.awt.Window</code> class. They are not a scarce resource on the
 * platform. On platforms which only support one {@link HScene} being visible at
 * one time the current {@link HScene} both loses the input focus and is hidden
 * (e.g. iconified) when another application successfully requests the input
 * focus. Two <code>java.awt.event.WindowEvent</code> events, with ids
 * <code>WINDOW_DEACTIVATED</code> and <code>WINDOW_ICONIFIED</code>, shall be
 * generated and sent to the {@link HScene} which has lost the focus and the
 * <CODE>isShowing</CODE> method for that HScene shall return false.
 * 
 * <p>
 * In terms of delegation, the {@link HScene} shall behave like a
 * <code>Window</code> with a native peer implementation, in that it will not
 * appear to delegate any functionality to any parent object. Components which
 * do not specify default characteristics inherit default values transitively
 * from their parent objects. Therefore, the implementation of {@link HScene}
 * must have valid defaults defined for all characteristics, e.g.
 * <code>Font</code>, foreground <code>Color</code>, background
 * <code>Color</code>, <code>ColorModel</code>, <code>Cursor</code> and
 * <code>Locale</code>.
 * 
 * <h3>Additional Z-order support</h3>
 * 
 * {@link HScene} extends the <code>java.awt.Container</code> class by providing
 * additional Z-ordering capabilities, which are required since components in
 * the HAVi user-interface are explicitly allowed to overlap each other. The
 * Z-ordering capabilities are defined by the
 * {@link org.havi.ui.HComponentOrdering HComponentOrdering} interface.
 * 
 * <p>
 * Note that these Z-ordering capabilities (<code>addBefore, addAfter,
 * pop, popInFrontOf, popToFront, push, pushBehind and
 * pushToBack</code>) must be implemented by (implicitly) reordering the child
 * Components within the {@link HScene}, so that the standard AWT convention
 * that the Z-order is defined as the order in which Components are added to a
 * given Container is maintained. See the description for
 * {@link org.havi.ui.HComponentOrdering HComponentOrdering} for more details.
 * 
 * <h3>Shortcut Keys</h3>
 * 
 * It is an implementation option for HAVi systems to implement shortcut keys by
 * forwarding <code>java.awt.KeyEvent</code> events to the parent {@link HScene}
 * of an application. Under these circumstances it is the responsibility of the
 * application designer to ensure that the relevant
 * <code>java.awt.KeyEvent</code> events used for "shortcut keys" are not
 * consumed by any custom components.
 * 
 * <p>
 * Implementations of the standard HAVi UI components which process
 * <code>java.awt.event.KeyEvent</code> events shall not consume any KeyEvent,
 * thus allowing their use as a shortcut key on implementation which rely on
 * KeyEvents being available in this way.
 * 
 * <h3>Event Handling</h3>
 * 
 * The mechanism by which input events are passed to the {@link HScene} and its
 * component hierarchy is not specified.
 * 
 * <p>
 * Note that whether the {@link HScene} is visible or not (as determined by the
 * {@link #isVisible() isVisible} method) does not guarantee that it has the
 * input focus and is receiving events.
 * 
 * <p>
 * When the application initially gains the input focus, this is indicated by
 * the system sending a <code>java.awt.event.WindowEvent</code> of type
 * <code>WINDOW_ACTIVATED</code> to the {@link HScene}. The {@link HScene}
 * should request that a child component gain the focus, if one is available.
 * However, the mechanism by which this occurs is intentionally not specified
 * here.
 * 
 * <p>
 * When the entire application loses the user's focus, the system shall notify
 * the {@link HScene} that it is no longer receiving events by sending a
 * java.awt.event.WindowEvent of type WINDOW_DEACTIVATED to the {@link HScene}.
 * 
 * 
 * <h3>Acquiring and Displaying HScenes</h3>
 * 
 * <p>
 * There is no public constructor for {@link HScene}, it is constructed by an
 * {@link HSceneFactory}. Only one {@link HScene} per
 * {@link org.havi.ui.HGraphicsDevice HGraphicsDevice} can be acquired at any
 * one time for each application.
 * 
 * <p>
 * The application may request that it be made visible by calling the the
 * {@link #show() show} method. This method should ensure that the
 * {@link HScene} is completely visible to the user, e.g. by expanding an icon,
 * or changing the stacking order between competing overlapping applications.
 * 
 * <p>
 * Making the {@link HScene} visible shall not automatically cause it to receive
 * or even request input focus. Input focus can be requested by the application
 * by calling the <CODE>requestFocus</CODE> method at any time.
 * 
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
 * <td>Visibility of the {@link HScene}</td>
 * <td>false</td>
 * <td>setVisible</td>
 * <td>isVisible</td>
 * </tr>
 * <tr>
 * <td>Activity of associated shortcuts</td>
 * <td>Shortcuts are active</td>
 * <td>{@link #enableShortcuts(boolean) enableShortcuts}</td>
 * <td>{@link #isEnableShortcuts() isEnableShortcuts}</td>
 * </tr>
 * <tr>
 * <td>Associated layout manager</td>
 * <td>null</td>
 * <td>java.awt.Container#setLayout</td>
 * <td>java.awt.Container#getLayout</td>
 * </tr>
 * <tr>
 * <td>Background image mode</td>
 * <td>IMAGE_NONE</td>
 * <td>{@link #setRenderMode(int) setRenderMode}</td>
 * <td>{@link #getRenderMode() getRenderMode}</td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 * @author Alex Resh (original HScene implementation)
 * @author Todd Earles
 * @author Jay Tracy (1.01b support)
 * @version $Revision: 1.25 $, $Date: 2002/11/07 21:13:40 $
 */
public class DefaultScene extends HScene implements RootContainer, FocusContext
{
    private static final Logger log = Logger.getLogger(DefaultScene.class.getName());

    private static final boolean DOUBLE_BUFFERED = "true".equalsIgnoreCase(MPEEnv.getEnv("MPE.GFX.BUFFERED"));

    /**
     * Package-private constructor. Creates a new <code>DefaultScene</code>
     * using the given <code>HSceneTemplate</code>, which will eventually be
     * placed into the appropriate device.
     * 
     * @param mediator
     *            the <code>org.cablelabs.impl.havi.DisplayMediator</code> used
     *            to mediate between this HScene, and the device to which it's
     *            added
     * @param hst
     *            the <code>HSceneTemplate</code> used to define this
     *            <code>HScene</code>.
     * @param f
     *            the <code>DefaultSceneFactory</code> used to create this scene
     * @param locX
     *            the initial X position of the scene
     * @param locY
     *            the initial Y position of the scene
     * @param width
     *            the initial width of the scene
     * @param height
     *            the initial height of the scene
     */
    DefaultScene(DisplayMediator mediator, HSceneTemplate hst, DefaultSceneFactory f, int locX, int locY, int width,
            int height)
    {
        this.mediator = mediator;
        this.factory = f;

        tracker = new HandleShortcutListener();
        if (ENABLE_SHORTCUTS) // by default
            addKeyListener(tracker);

        setVisible(false);

        graphicsConfig = (HGraphicsConfiguration) hst.getPreferenceObject(HSceneTemplate.GRAPHICS_CONFIGURATION);

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        context = ccm.getCurrentContext();

        // Create callback data
        getData(context);

        // Set the initial bounds
        super.setBounds(locX, locY, width, height);

        inized = true;
    }
    
    /**
     * Overrides {@link java.awt.Component#paramString()} for the purposes of
     * modifying behavior of {@link Object#toString()}.
     */
    protected String paramString()
    {
        return context + "," + super.paramString();
    }

    /**
     * Remove all disabled shortcuts from the shortcuts list. A shortcut is
     * disabled if its component is removed from the component tree under the
     * HScene.
     */
    private void removeDisabledShortcuts()
    {
        int[] keys = shortcuts.getKeys();
        for (int i = 0; i < keys.length; ++i)
        {
            int keyCode = keys[i];
            Component c = (Component) shortcuts.get(keyCode);
            if (!isAncestorOf(c)) removeShortcut(keyCode);
        }
    }

    // Definition copied from HScene
    public void enableShortcuts(boolean enable)
    {
        enableShortcuts = enable;
        if (enable)
        {
            addKeyListener(tracker);
        }
        else
        {
            removeKeyListener(tracker);
        }
    }

    // Definition copied from HScene
    public boolean addShortcut(int keyCode, HActionable comp)
    {
        checkState();
        if (comp == null)
            shortcuts.put(keyCode, null);
        else if (keyCode == KeyEvent.VK_UNDEFINED || !(comp instanceof Component) || !isAncestorOf((Component) comp))
            return false;
        else
        {
            int otherKey = getShortcutKeycode(comp);
            if (otherKey != KeyEvent.VK_UNDEFINED) removeShortcut(otherKey);
            shortcuts.put(keyCode, comp);
        }
        return true;
    }

    // Definition copied from HScene
    public int[] getAllShortcutKeycodes()
    {
        checkState();

        removeDisabledShortcuts();

        return shortcuts.getKeys();
    }

    // Definition copied from HScene
    public int getShortcutKeycode(HActionable comp)
    {
        checkState();

        removeDisabledShortcuts();

        int[] keys = shortcuts.getKeys();
        for (int i = 0; i < keys.length; i++)
        {
            HActionable c = (HActionable) shortcuts.get(keys[i]);
            if (c == comp) return keys[i];
        }
        return KeyEvent.VK_UNDEFINED;
    }

    // Definition copied from HScene
    public boolean isEnableShortcuts()
    {
        checkState();

        return enableShortcuts;
    }

    // Definition copied from HScene
    public void removeShortcut(int keyCode)
    {
        checkState();

        shortcuts.put(keyCode, null);
    }

    // Definition copied from HScene
    public HActionable getShortcutComponent(int keyCode)
    {
        checkState();

        removeDisabledShortcuts();

        return (HActionable) shortcuts.get(keyCode);
    }

    // Definition copied from HScene
    public HScreenRectangle getPixelCoordinatesHScreenRectangle(java.awt.Rectangle r)
    {
        checkState();

        // get the pixel bounds of the scene
        Dimension sceneSize = getSize();

        // get the HScreenRectangle area of the scene
        HScreenRectangle sceneRect = getSceneScreenRect(getBounds());

        return (DefaultSceneFactory.convertArea(r, sceneSize, sceneRect));
    }
    
    // Definition copied from HScene
    public void paint(Graphics g)
    {
        if ((context != null) && context.isAlive())
        {
            final Graphics graphics = g;

            try
            {
                context.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        DefaultScene.super.paint(graphics);
                    }
                });
            }
            catch (InvocationTargetException e)
            {
                SystemEventUtil.logUncaughtException(e.getTargetException(), context);
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }
    }

    /**
     * Returns whether or not this caller is the same one that created this
     * scene
     * 
     * @return true if the <code>CallerContext</code> making this call is the
     *         same one that created this scene
     */
    public boolean isOwnedByCaller()
    {
        // Get current caller context
        CallerContextManager cm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext currentContext = cm.getCurrentContext();

        return context == currentContext;
    }

    protected HScreenRectangle getSceneScreenRect(Rectangle r)
    {
        checkState();

        HGraphicsConfigTemplate hgct = graphicsConfig.getConfigTemplate();

        Dimension graphicsPixelDim = (Dimension) hgct.getPreferenceObject(HScreenConfigTemplate.PIXEL_RESOLUTION);
        HScreenRectangle graphicsScreenRect = (HScreenRectangle) hgct.getPreferenceObject(HScreenConfigTemplate.SCREEN_RECTANGLE);

        return DefaultSceneFactory.convertArea(r, graphicsPixelDim, graphicsScreenRect);
    }

    // Definition copied from HScene
    public HSceneTemplate getSceneTemplate()
    {
        checkState();

        // Should return a copy of the scene template
        HSceneTemplate hst = new HSceneTemplate();

        // Set preferences
        hst.setPreference(HSceneTemplate.GRAPHICS_CONFIGURATION, graphicsConfig, HSceneTemplate.REQUIRED);
        hst.setPreference(HSceneTemplate.SCENE_PIXEL_LOCATION, getLocation(), HSceneTemplate.REQUIRED);
        hst.setPreference(HSceneTemplate.SCENE_PIXEL_DIMENSION, getSize(), HSceneTemplate.REQUIRED);

        HScreenRectangle screenRect = getSceneScreenRect(getBounds());
        hst.setPreference(HSceneTemplate.SCENE_SCREEN_LOCATION, new HScreenPoint(screenRect.x, screenRect.y),
                HSceneTemplate.REQUIRED);
        hst.setPreference(HSceneTemplate.SCENE_SCREEN_DIMENSION, new HScreenDimension(screenRect.width,
                screenRect.height), HSceneTemplate.REQUIRED);

        return hst;
    }

    // Definition copied from HScene
    public void setActive(boolean active)
    {
        this.active = active;
        if (isActivable(false))
            fm.requestActivate(this, false);
        else
            fm.notifyDeactivated(this);
    }

    // Definition copied from HScene
    public void setVisible(boolean show)
    {
        super.setVisible(show);

        if (!show) mediator.pushToRear(this);

        if (isActivable(false))
            fm.requestActivate(this, false);
        else if (!show) fm.notifyDeactivated(this);
    }

    // Definition copied from HScene
    public void show()
    {
        super.show();
    }

    // Definition copied from HScene
    protected void sceneToFront()
    {
        mediator.popToFront(this);
    }

    // Definition copied from HScene
    protected boolean checkShow()
    {
        return mediator.checkShow(getBinding());
    }

    // Definition copied from HScene
    protected boolean checkPopToFront()
    {
        return mediator.checkPopToFront(this);
    }

    // Definition copied from HScene
    protected boolean checkMove(final int x, final int y, final int width, final int height)
    {
        HSceneBinding binding = new HSceneBinding()
        {
            public HScreenRectangle getRectangle()
            {
                return DefaultScene.this.getSceneScreenRect(new Rectangle(x, y, width, height));
            }

            public OcapAppAttributes getAppAttributes()
            {
                return DefaultScene.this.getAppAttributes();
            }
        };

        // SA: the checkNewPositionAndSize used to be called only when the
        // HScene was visible. In that
        // case, the position would still be checked whenever setVisible(true)
        // or show() was called.
        // This lead to bug OCORI-675 where several CTP tests did not set the
        // HScene visible, but still expected
        // checkNewPositionAndSize to be called. The OCAP spec (1.1) does not
        // restrict this call to only
        // visible HScenes, so I changed the code to call
        // checkNewPositionAndSize whether an HScene is
        // visible or not.

        return mediator.checkNewPositionAndSize(binding);
    }

    /**
     * Handles shortcut-keys.
     * 
     * @param e
     *            the KeyEvent
     * 
     * @see #isEnableShortcuts()
     * @see #addShortcut(int, HActionable)
     * @see #removeShortcut(int)
     * @see #getAllShortcutKeycodes()
     * @see #getShortcutKeycode(HActionable)
     */
    protected void handleShortcut(KeyEvent e)
    {
        if (mediator.isOnDevice(this) && isEnableShortcuts())
        {
            int key = e.getKeyCode();
            HActionable a = getShortcutComponent(key);

            if (a != null &&
            /* a instanceof HActionInputPreferred && */// redundant
                    a instanceof Component && // only sub-components allowed
                    isAncestorOf((Component) a))
            {
                HActionEvent action = new HActionEvent(a, ActionEvent.ACTION_PERFORMED, a.getActionCommand());

                ((HActionInputPreferred) a).processHActionEvent(action);
            }
        }
    }

    // Definition copied from HScene
    public java.awt.Component getFocusOwner()
    {
        return activated ? focusOwner : null;
    }

    // Definition copied from HScene
    public void dispose()
    {
        super.dispose();
        
        if (enableShortcuts)
        {
            // Ensure that listener is not leaked
            removeKeyListener(tracker);
        }

        mediator.remove(this);
        factory.disposeImpl(this);
    }

    // Definition copied from HScene
    public void setKeyEvents(HEventGroup keyCodes)
    {
        super.setKeyEvents(keyCodes);

        if (keyCodes == null)
            keySet = null;
        else
        {
            int[] keys = keyCodes.getKeyEvents();
            BitSet bitSet = new BitSet();
            for (int i = 0; i < keys.length; ++i)
                bitSet.set(keys[i]);
            keySet = bitSet;
        }
    }

    /**
     * Checks whether the current state of this <code>HScene</code> is legal or
     * not.
     * 
     * @throws IllegalStateException
     *             is thrown if the state of this <code>HScene</code> is
     *             determined to be invalid and illegal.
     */
    protected void checkState() throws IllegalStateException
    {
        // Allow calls to be made while being created...
        if (!inized) return;

        // If we've been removed from the main frame... throw a fit!
        if (!mediator.isOnDevice(this)) throw new java.lang.IllegalStateException();
    }

    /**
     * Disallow getParent() call.
     */
    public Container getParent()
    {
        if (NO_PARENT) throw new RuntimeException("HScene has no parent");
        return super.getParent();
    }

    /**
     * Allows others to get access to the <code>DisplayMediator</code> for this
     * <code>DefaultScene</code>. This is <b>not</b> a part of the public API.
     */
    public DisplayMediator getDisplayMediator()
    {
        return (mediator);
    }

    /**
     * Returns the HSceneBinding that describes this scene
     * 
     * @return the HSceneBinding
     */
    public HSceneBinding getBinding()
    {
        return new HSceneBinding()
        {
            public HScreenRectangle getRectangle()
            {
                return getSceneScreenRect(getBounds());
            }

            public OcapAppAttributes getAppAttributes()
            {
                return DefaultScene.this.getAppAttributes();
            }
        };
    }

    /**
     * Override <code>super.processEvent()</code> to dispatch
     * <code>WindowEvent</code>s.
     * <p>
     * This method has been made public so that it can be called from outside
     * this object. This is necessary because
     * <code>Component.dispatchEvent()</code> does not handle
     * <code>WindowEvent</code>s and is final (i.e., unoverridable).
     * 
     * @param e
     *            the AWTEvent to process
     */
    public void processEvent(AWTEvent e)
    {
        if (log.isTraceEnabled())
        {
            log.trace("processEvent: " + e);
        }

        if (e instanceof WindowEvent)
            processWindowEvent((WindowEvent) e);
        else
        {
            if (log.isTraceEnabled())
            {
                log.trace("processEvent (not WindowEvent): " + e);
            }

            super.processEvent(e);
        }
    }

    /**
     * Implements
     * {@link org.cablelabs.impl.manager.FocusManager.RootContainer#handleRequestFocus}
     * .
     */
    public void handleRequestFocus(Component c, boolean temporary)
    {
        focusRequested = true;
        if (dispatcher.setFocusRequest(c, temporary))
        {
            fm.requestActivate(this, true);
        }
    }

    /**
     * Implements {@link FocusManager.FocusContext#getPriority()}.
     */
    public int getPriority()
    {
        return PRIORITY_NORMAL;
    }
   
    /**
     * Implements
     * {@link FocusManager.FocusContext#dispatchEvent(java.awt.AWTEvent, DispatchFilter, boolean)}
     * .
     * <p>
     * The given event is dispatched to the current focus owner, if this scene
     * is activated, is interested in the event, and is accepted by the
     * <code>DispatchFilter</code>.
     */
    public void dispatchEvent(final AWTEvent e, DispatchFilter filter, final boolean interestFilter)
    {
        if (context != null && context.isAlive() &&
            activated && (!interestFilter || isInterested(e)) &&
            (filter == null || filter.accept(context)))
        {
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    dispatcher.dispatchEvent(e);
                }
            });
        }
    }

    /**
     * Implements {@link FocusManager.FocusContext#notifyActivated()}.
     */
    public void notifyActivated()
    {
        if (context != null && context.isAlive())
        {
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    boolean wasActivated = activated;
                    activated = true;
    
                    // Send WINDOW_ACTIVATED event
                    if (!wasActivated)
                        processEvent(mediator.createWindowEvent(DefaultScene.this, WindowEvent.WINDOW_ACTIVATED));
    
                    // Update the focus owner
                    dispatcher.focusGained();
                }
            });
        }
    }

    /**
     * Implements {@link FocusManager.FocusContext#notifyDeactivated()}.
     */
    public void notifyDeactivated()
    {
        activated = false;

        if (context != null && context.isAlive())
        {
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    // Send WINDOW_DEACTIVATED event
                    processEvent(mediator.createWindowEvent(DefaultScene.this, WindowEvent.WINDOW_DEACTIVATED));

                    // Update focus owner
                    dispatcher.focusLost(true);
                }
            });
        }
    }

    /**
     * Implements {@link FocusContext#clearFocus}
     */
    public void clearFocus()
    {
        if (context != null && context.isAlive())
        {
            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    // Update focus owner
                    dispatcher.focusLost(false);
                }
            });
        }
    }

    /**
     * Returns the app attributes associated with this scene's CallerContext
     * 
     * @return the OcapAppAttributes association with this scene
     */
    private OcapAppAttributes getAppAttributes()
    {
        // Get the AppID and AppsDatabase associated with this scene's context
        AppID appID = (AppID) context.get(CallerContext.APP_ID);
        AppsDatabase db = (AppsDatabase) context.get(org.dvb.application.AppsDatabase.class);

        // Get the AppAttributes associated with the AppID. These will actually
        // be an instance of OcapAppAttributes
        return (OcapAppAttributes) db.getAppAttributes(appID);
    }

    /**
     * Returns whether this scene should be considered activable or not. To be
     * activable all of the following must be true:
     * <ul>
     * <li> {@link HScene#isVisible visible}
     * <li> {@link HScene#setActive active}
     * <li>focus has been {@link Component#requestFocus requested} at least once
     * <li>owning application is {@link org.dvb.application.AppProxy#getState()
     * Active}
     * </ul>
     * 
     * @param ignoreAppState
     *            if <code>true</code> then ignore the current application
     *            state; if <code>false</code> then include the current
     *            application state
     * 
     * @return <code>true</code> if this scene should be considered activable;
     *         <code>false</code> otherwise
     */
    private boolean isActivable(boolean ignoreAppState)
    {
        return focusRequested && isVisible() && active && (ignoreAppState || context.isActive());
    }

    /**
     * Determines if this scene is interested in the given <code>AWTEvent</code>
     * . This scene is <i>interested</i> in the event if all of the following
     * are true:
     * <ul>
     * <li> <code>e instanceof KeyEvent</code> is <code>true</code>
     * <li>the {@link HEventGroup} set via
     * {@link HScene#setKeyEvents(HEventGroup)} includes the given key event
     * </ul>
     * 
     * @param e
     *            the event being dispatched to this <code>RootContainter</code>
     * @return <code>true</code> if this scene is interested in the event
     */
    private boolean isInterested(AWTEvent e)
    {
        if (!(e instanceof KeyEvent)) return false;

        BitSet keys = keySet;
        return (keys == null) ? true : keys.get(((KeyEvent) e).getKeyCode());
    }

    public boolean isDoubleBuffered()
    {
        return DOUBLE_BUFFERED;
    }

    /**
     * Holds context-specific data.
     */
    private class Data implements CallbackData
    {
        public void destroy(CallerContext ctx)
        {
            fm.notifyDeactivated(DefaultScene.this);
            dispose();
        }

        public void active(CallerContext ctx)
        {
            if (isActivable(false)) fm.requestActivate(DefaultScene.this, false);
        }

        public void pause(CallerContext ctx)
        {
            fm.notifyDeactivated(DefaultScene.this);
        }
    }

    /**
     * Access this device's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx)
    {
        synchronized (lock)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
            }
            return data;
        }
    }

    /**
     * The HScene mediator.
     */
    protected DisplayMediator mediator;

    /**
     * The creating instance of HSceneFactory.
     */
    protected DefaultSceneFactory factory;

    /**
     * The {@link HGraphicsConfiguration} used to create this HScene
     */
    protected HGraphicsConfiguration graphicsConfig;

    /**
     * The mapping of virtual keycodes to shortcuts.
     */
    protected KeySet shortcuts = new KeySet();

    /**
     * If <code>true</code>, then keyboard shortcuts are enabled.
     */
    protected boolean enableShortcuts = ENABLE_SHORTCUTS;

    /**
     * The <code>KeyListener</code> used to enable shortcuts.
     */
    protected KeyListener tracker;

    /**
     * Cached instance of the FocusManager used to request focus.
     */
    private FocusManager fm = (FocusManager) ManagerManager.getInstance(FocusManager.class);

    /** The event dispatcher. */
    private Dispatcher dispatcher = new Dispatcher();

    /** If true then focus has been requested at least once. */
    private boolean focusRequested;

    /** The currently focused component, if activated. */
    private Component focusOwner = null;

    /** If true then currently activated (i.e., focused). */
    private boolean activated;

    /**
     * The set of events that this scene is interested in. If <code>null</code>,
     * then all keys are supported.
     */
    private BitSet keySet = null;

    /** Initialized flag. */
    protected boolean inized = false;

    /**
     * Lock object
     */
    private Object lock = new Object();

    /**
     * reference to the CallerContext
     */
    private CallerContext context = null;

    /**
     * Flag specifies whether HScene.getParent() is allowed or not. If not, it
     * really should be handled by a SecurityManager. That way we can see WHO is
     * making the call (this class or something else).
     */
    private static final boolean NO_PARENT = false;

    /**
     * Shortcuts are implemented (turned on by the implementation).
     */
    private static final boolean ENABLE_SHORTCUTS = false;

    private class Dispatcher
    {
        public boolean setFocusRequest(Component c, boolean temporary)
        {
            // If scene is dead, forget it
            DefaultScene w = DefaultScene.this;
            if (!mediator.isOnDevice(w)) return false;

            // Set focus requestor and temporary state
            focus = c;
            isTemporary = temporary;

            // Determine whether activation should be requested
            // We ignore the current app state, allowing requestFocus() to be
            // called from startXlet()
            // Unfortunately, that also allows it to be called from paused
            // state.
            return isActivable(true);
        }

        /**
         * Sets the current focusOwner and sends FOCUS_GAINED and FOCUS_LOST
         * events.
         */
        public void focusGained()
        {
            if (context != null && context.isAlive())
            {
                // Set focusOwner = focus
                Component oldOwner = focusOwner;
                Component newOwner = focus;
                focusOwner = newOwner;
    
                // Send FOCUS_LOST to old focusOwner
                if (oldOwner != null && oldOwner != newOwner)
                    oldOwner.dispatchEvent(new FocusEvent(oldOwner, FocusEvent.FOCUS_LOST, isTemporary));
                // Send FOCUS_GAINED
                if (newOwner != null)
                    newOwner.dispatchEvent(new FocusEvent(newOwner, FocusEvent.FOCUS_GAINED, isTemporary));
            }
        }

        /**
         * Sends a FOCUS_LOST event to the current focusOwner. Clear the focus
         * if this is not a temporary loss
         */
        public void focusLost(boolean temporary)
        {
            if (context != null && context.isAlive())
            {
                Component oldOwner = focusOwner;
                if (oldOwner != null)
                {
                    oldOwner.dispatchEvent(new FocusEvent(oldOwner, FocusEvent.FOCUS_LOST, true));
    
                    if (!temporary)
                    {
                        focusOwner = null;
                        focus = null;
                    }
                }
            }
        }

        /**
         * Dispatches the given event to the current focusOwner, if any. If this
         * event is not a KeyEvent, then it is not dispatched.
         * 
         * @param e
         *            the event to dispatch
         */
        public void dispatchEvent(AWTEvent e)
        {
            if (log.isTraceEnabled())
            {
                log.trace("dispatchEvent: " + e);
            }

            if (context != null && context.isAlive())
            {
                Component owner = focusOwner;
    
                if (!(e instanceof KeyEvent)) 
                {
                    if (log.isTraceEnabled())
                    {
                        log.trace("dispatchEvent: event not KeyEvent");
                    }

                    return;
                }
    
                KeyEvent ke = (KeyEvent) e;
                if (owner != null)
                {
                    if (log.isTraceEnabled())
                    {
                        log.trace("dispatchEvent: dispatching event to " + owner);
                    }

                    Object oldSource = ke.getSource();
                    ke.setSource(owner);
                    owner.dispatchEvent(ke);
                    ke.setSource(oldSource);
                    ke.consume();
                }
                else
                {
                    if (log.isTraceEnabled())
                    {
                        log.trace("dispatchEvent: Focus owner is null");
                    }
                }
            }
            else
            {
                if (context == null)
                {
                    if (log.isTraceEnabled())
                    {
                        log.trace("dispatchEvent: NULL CONTEXT");
                    }
                }
                else
                {
                    if (log.isTraceEnabled())
                    {
                        log.trace("dispatchEvent: CONTEXT NOT ALIVE");
                    }
                }
            }
        }

        /**
         * The current focus requester.
         */
        private Component focus;

        private boolean isTemporary;
    }

    private class HandleShortcutListener extends KeyAdapter
    {
        public void keyPressed(KeyEvent e)
        {
            handleShortcut(e);
        }
    };
}
