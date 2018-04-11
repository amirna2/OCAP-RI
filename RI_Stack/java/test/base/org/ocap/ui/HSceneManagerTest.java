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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.davic.net.Locator;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppProxyTest.DummySecurityManager;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenDimension;
import org.havi.ui.HScreenPoint;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.TestSupport;
import org.ocap.application.OcapAppAttributes;
import org.ocap.system.MonitorAppPermission;

public class HSceneManagerTest extends TestCase
{
    /**
     * Tests no public constructors.
     */
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(HSceneManager.class);
    }

    public void testGetInstance()
    {
        HSceneManager hsm = HSceneManager.getInstance();
        assertNotNull("getInstance() should not return null", hsm);
        assertSame("Expect getInstance() to return same singleton", hsm, HSceneManager.getInstance());
    }

    public void testSetHSceneChangeRequestHandler()
    {
        HSceneManager hsm = HSceneManager.getInstance();

        try
        {
            hsm.setHSceneChangeRequestHandler(new Handler());
        }
        finally
        {
            hsm.setHSceneChangeRequestHandler(null);
        }
    }

    public void testSetHSceneChangeRequestHandler_security()
    {
        HSceneManager mgr = HSceneManager.getInstance();
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            mgr.setHSceneChangeRequestHandler(new Handler());
            assertNotNull("Expected checkPermission() to be called", sm.p);
            assertTrue("Expected MonitorAppPermission to be tested", sm.p instanceof MonitorAppPermission);
            assertEquals("Expected handler.resource to be tested", "handler.resource", sm.p.getName());

            sm.p = null;
            mgr.setHSceneChangeRequestHandler(null);
            assertNotNull("Expected checkPermission() to be called", sm.p);
            assertTrue("Expected MonitorAppPermission to be tested", sm.p instanceof MonitorAppPermission);
            assertEquals("Expected handler.resource to be tested", "handler.resource", sm.p.getName());
        }
        finally
        {
            ProxySecurityManager.pop();
            mgr.setHSceneChangeRequestHandler(null);
        }
    }

    public void testGetHSceneOrder_security()
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            HSceneManager.getHSceneOrder();
            assertNotNull("Expected checkPermission() to be called", sm.p);
            assertTrue("Expected MonitorAppPermission to be tested", sm.p instanceof MonitorAppPermission);
            assertEquals("Expected handler.resource to be tested", "handler.resource", sm.p.getName());
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests getHSceneOrder() and getAppSceneLocation().
     */
    public void testGetHSceneOrder_getAppHSceneLocation() throws Exception
    {
        Context[] ctx = { new Context(new AppID(1, 100)), new Context(new AppID(1, 200)),
                new Context(new AppID(1, 300)), };

        try
        {
            assertNotNull("Should never return null", HSceneManager.getHSceneOrder());
            assertEquals("Expected no scenes visible yet", 0, HSceneManager.getHSceneOrder().length);

            for (int i = 0; i < ctx.length; ++i)
                assertEquals("Expected app to not have location", -1, ctx[i].getLocation());

            // Create scenes and bring them to the front
            for (int i = 0; i < ctx.length; ++i)
            {
                HScene scene = ctx[i].getFullScreenScene();
                assertNotNull("Could not create scene for " + ctx[i].id);
                assertSame("Expected same scene to be returned", scene, ctx[i].getHScene());
                scene.setVisible(true);
                scene.show();

                assertEquals("Expect scene to be moved to the front", 0, ctx[i].getLocation());

                OcapAppAttributes[] order = HSceneManager.getHSceneOrder();

                assertNotNull("Expected non-null array to be returned", order);
                assertEquals("Unexpected array size returned", i + 1, order.length);
                for (int j = 0; j < order.length; ++j)
                {
                    int orderIdx = order.length - 1 - j;
                    assertNotNull("Expected a non-null OcapAppAttributes ", order[orderIdx]);
                    assertEquals("Unexpected app attributes at [" + orderIdx + "]", ctx[j].id,
                            order[orderIdx].getIdentifier());
                    assertEquals("Unexpected app scene location", orderIdx, ctx[j].getLocation());
                }
            }

            // Start reordering scenes
            for (int i = 0; i < ctx.length; ++i)
            {
                HScene scene = ctx[i].getHScene();
                scene.show();

                OcapAppAttributes[] order = HSceneManager.getHSceneOrder();

                assertNotNull("Expected non-null array to be returned", order);
                assertEquals("Unexpected array size returned", ctx.length, order.length);
                assertEquals("Unexpected app in front", ctx[i].id, order[0].getIdentifier());
            }
        }
        finally
        {
            for (int i = 0; i < ctx.length; ++i)
                ctx[i].cleanup();
        }
    }

    /**
     * Tests invocation of HSceneChangeRequestHandler.testShow().
     * 
     * @todo disabled per 5128
     */
    public void xxxtestHandlerInvocation_HSceneShow() throws Exception
    {
        HSceneManager hsm = HSceneManager.getInstance();

        Context[] ctx = { new Context(new AppID(2, 102)), new Context(new AppID(2, 100)),
                new Context(new AppID(2, 101)), new Context(new AppID(2, 103)), };

        Handler handler = new Handler();
        handler.move = true;
        handler.order = true;
        hsm.setHSceneChangeRequestHandler(handler);
        try
        {
            // First, get all scenes created and shown
            handler.show = true;
            for (int i = ctx.length; i-- > 0;)
            {
                // Get scene and request show
                HScene scene = ctx[i].getFullScreenScene();
                assertNotNull("Could not create scene for " + ctx[i].id);
                assertSame("Expected same scene to be returned", scene, ctx[i].getHScene());
                scene.setVisible(true);
                scene.show();

                // Ensure that scene is visible, and in front
                assertTrue("Scene should be visible", scene.isVisible());
                assertEquals("Scene should be moved to the front", 0, ctx[i].getLocation());
            }
            // All should be showing now

            handler.order = false; // prevent any reordering
            for (int i = 0; i < ctx.length; ++i)
            {
                // Get scene and hide
                HScene scene = ctx[i].getHScene();
                scene.setVisible(false);

                // Don't allow show
                handler.clear();
                handler.show = false;

                // Request show
                assertNotNull("Could not create scene for " + ctx[i].id);
                assertSame("Expected same scene to be returned", scene, ctx[i].getHScene());
                scene.setVisible(true);

                // Ensure that scene is not visible
                assertFalse("Scene should have been denied visibility", scene.isVisible());
                // TODO: should a non-visible scene have a z-order location? See
                // bug 4520.
                // I don't think that it should.
                // assertEquals("Scene should not have a location", -1,
                // ctx[i].getLocation());

                // Ensure testShow was called
                checkBinding("show() ", handler.showNew, ctx[i], scene);
                assertNotNull("Expected non-null binding[]", handler.showCurr);
                // TODO: should non-visible scene be in the list? See bug 4520
                assertEquals("Unexpected number of current scenes", ctx.length, handler.showCurr.length);
                for (int j = 0; j < ctx.length; ++j)
                {
                    // TODO: ctx[i] shouldn't be in the list (see bug 4520)
                    checkBinding("[" + j + "]", handler.showCurr[j], ctx[j], ctx[j].getHScene());
                }

                // Now allow show
                handler.clear();
                handler.show = true;

                scene.setVisible(true);

                // Ensure that scene is visible
                assertTrue("Scene should have been granted visibility", scene.isVisible());
                // TODO: should a non-visible scene have a z-order location? See
                // bug 4520.
                // I don't think that it should.
                // assertTrue("Scene should have a location", -1 !=
                // ctx[i].getLocation());

                // Ensure testShow was called
                checkBinding("show() ", handler.showNew, ctx[i], scene);
                assertNotNull("Expected non-null binding[]", handler.showCurr);
                // TODO: should non-visible scene be in the list? See bug 4520
                assertEquals("Unexpected number of current scenes", ctx.length, handler.showCurr.length);
                for (int j = 0; j < ctx.length; ++j)
                {
                    // TODO: ctx[i] shouldn't be in the list (see bug 4520)
                    checkBinding("[" + j + "]", handler.showCurr[j], ctx[j], ctx[j].getHScene());
                }
            }
        }
        finally
        {
            hsm.setHSceneChangeRequestHandler(null);

            for (int i = 0; i < ctx.length; ++i)
                ctx[i].cleanup();
        }
    }

    /**
     * Tests invocation of HSceneChangeRequestHandler.testMove().
     */
    public void testHandlerInvocation_HSceneMove() throws Exception
    {
        HSceneManager hsm = HSceneManager.getInstance();

        Context[] ctx = { new Context(new AppID(3, 102)), new Context(new AppID(3, 100)),
                new Context(new AppID(3, 101)), new Context(new AppID(3, 103)), };
        Rectangle[] bounds = { new Rectangle(10, 10, 620, 460), new Rectangle(30, 30, 100, 100),
                new Rectangle(240, 240, 240, 240), new Rectangle(0, 0, 320, 240), };

        Handler handler = new Handler();
        handler.show = true;
        handler.order = true;
        hsm.setHSceneChangeRequestHandler(handler);
        try
        {
            // First, get all scenes created and shown
            for (int i = 0; i < ctx.length; ++i)
            {
                // Get scene and request show
                HScene scene = ctx[i].getFullScreenScene();
                assertNotNull("Could not create scene for " + ctx[i].id);
                assertSame("Expected same scene to be returned", scene, ctx[i].getHScene());
                scene.setVisible(true);

                // Ensure that scene is visible, and in front
                assertTrue("Scene should be visible", scene.isVisible());
                assertEquals("Scene should be moved to the front", 0, ctx[i].getLocation());
            }

            for (int i = 0; i < ctx.length; ++i)
            {
                // Attempt to change bounds of HScene
                HScene scene = ctx[i].getHScene();
                handler.move = false;
                handler.clear();
                Rectangle origBounds = scene.getBounds();
                HScreenRectangle newRect = getRect(getGraphicsConfiguration(scene), bounds[i]);
                assertFalse("Internal error - destination bounds same as current bounds", origBounds.equals(bounds[i]));
                scene.setBounds(bounds[i]);

                // Ensure that bounds weren't changed
                assertEquals("Expected original bounds to be unchanged", origBounds, scene.getBounds());

                // Ensure that testMove was invoked
                checkBinding("move() ", handler.moveNew, ctx[i], newRect);
                assertNotNull("Expected non-null binding[]", handler.moveCurr);
                assertEquals("Unexpected number of current scenes", ctx.length, handler.moveCurr.length);
                for (int j = 0; j < ctx.length; ++j)
                {
                    int idx = handler.moveCurr.length - 1 - j;
                    checkBinding("[" + idx + "]", handler.moveCurr[idx], ctx[j], ctx[j].getHScene());
                }

                // Allow change bounds; change bounds
                handler.move = true;
                handler.clear();
                scene.setBounds(bounds[i]);

                // Ensure that bounds were changed
                assertEquals("Expected bounds to be changed", bounds[i], scene.getBounds());

                // Ensure that testMove was invoked
                checkBinding("move() ", handler.moveNew, ctx[i], newRect);
                assertNotNull("Expected non-null binding[]", handler.moveCurr);
                assertEquals("Unexpected number of current scenes", ctx.length, handler.moveCurr.length);
                for (int j = 0; j < ctx.length; ++j)
                {
                    int idx = handler.moveCurr.length - 1 - j;
                    checkBinding("[" + idx + "]", handler.moveCurr[idx], ctx[j], ctx[j].getHScene());
                }
            }
        }
        finally
        {
            hsm.setHSceneChangeRequestHandler(null);

            for (int i = 0; i < ctx.length; ++i)
                ctx[i].cleanup();
        }
    }

    /**
     * Tests invocation of HSceneChangeRequestHandler.testOrder().
     */
    public void testHandlerInvocation_HSceneOrder() throws Exception
    {
        HSceneManager hsm = HSceneManager.getInstance();

        Context[] ctx = { new Context(new AppID(4, 102)), new Context(new AppID(4, 100)),
                new Context(new AppID(4, 101)), new Context(new AppID(4, 103)), };

        Handler handler = new Handler();
        try
        {
            // First, get all scenes created and shown
            for (int i = 0; i < ctx.length; ++i)
            {
                // Get scene and request show
                HScene scene = ctx[i].getFullScreenScene();
                assertNotNull("Could not create scene for " + ctx[i].id);
                assertSame("Expected same scene to be returned", scene, ctx[i].getHScene());
                scene.setVisible(true);

                // Ensure that scene is visible, and in front
                assertTrue("Scene should be visible", scene.isVisible());
                assertEquals("Scene should be moved to the front", 0, ctx[i].getLocation());
            }

            // Set up current expected order
            Vector expectedOrder = new Vector();
            for (int i = ctx.length - 1; i >= 0; --i)
                expectedOrder.addElement(ctx[i]);

            // Now lets try to move each, with handler installed
            hsm.setHSceneChangeRequestHandler(handler);
            handler.show = true;
            handler.move = true;
            for (int i = 0; i < ctx.length; ++i)
            {
                HScene scene = ctx[i].getHScene();
                int currOrder = ctx[i].getLocation();

                // Double-check that not in front
                assertFalse("Scene should not be in front", 0 == currOrder);
                assertEquals("Scene should be in back", ctx.length - 1, currOrder);

                // Don't allow reorder
                handler.order = false;
                handler.clear();
                scene.show();

                // Ensure that ordering wasn't changed
                assertEquals("Expected ordering to go unchanged", currOrder, ctx[i].getLocation());

                // Ensure that testOrder was called
                assertEquals("Expected new order to be in front", 0, handler.orderNew);
                assertEquals("Expected curr order to be not in front", currOrder, handler.orderCurr);
                assertNotNull("Expected non-null curr scene order[]", handler.orderScenes);
                assertEquals("Unexpected number of current scenes", ctx.length, handler.orderScenes.length);
                for (int j = 0; j < ctx.length; ++j)
                {
                    Context cc = (Context) expectedOrder.elementAt(j);
                    checkBinding("[" + j + "]", handler.orderScenes[j], cc, cc.getHScene());
                }

                // Now allow ordering
                handler.clear();
                handler.order = true;

                scene.show();

                // Validate that ordering changed
                assertEquals("Expected to be popped to front", 0, ctx[i].getLocation());

                // Ensure that testOrder was called
                assertEquals("Expected new order to be in front", 0, handler.orderNew);
                assertEquals("Expected curr order to be not in front", currOrder, handler.orderCurr);
                assertNotNull("Expected non-null curr scene order[]", handler.orderScenes);
                assertEquals("Unexpected number of current scenes", ctx.length, handler.orderScenes.length);
                for (int j = 0; j < ctx.length; ++j)
                {
                    Context cc = (Context) expectedOrder.elementAt(j);
                    checkBinding("[" + j + "]", handler.orderScenes[j], cc, cc.getHScene());
                }

                // Re-order expected order (move from back to front)
                expectedOrder.removeElement(ctx[i]);
                expectedOrder.insertElementAt(ctx[i], 0);
            }
        }
        finally
        {
            hsm.setHSceneChangeRequestHandler(null);

            for (int i = 0; i < ctx.length; ++i)
                ctx[i].cleanup();
        }
    }

    /**
     * Test that the HScene handler is invoked in the proper CallerContext.
     */
    public void testHandlerInvocation_CC() throws Exception
    {
        Context[] ctx = { new Context(new AppID(5, 100)), new Context(new AppID(5, 101)),
                new Context(new AppID(5, 102)), };
        Context monapp = new Context(new AppID(5, 0x6001));

        try
        {
            HScene scene;

            // Setup handler
            Handler handler = new Handler();
            monapp.setHandler(handler);
            handler.show = true;
            handler.move = true;
            handler.order = true;

            // First get all scenes created
            for (int i = 0; i < ctx.length; ++i)
            {
                scene = ctx[i].getFullScreenScene();
                scene.setVisible(true);
                scene.show();
            }
            handler.clear();

            // show
            scene = ctx[0].getHScene();
            scene.setVisible(false);
            assertFalse("Scene should not be visible", scene.isVisible());
            scene.setVisible(true);
            assertTrue("Scene should be visible", scene.isVisible());
            assertNotNull("testShow() should've been invoked", handler.showCC);
            assertSame("Expected handler to be invoked within monapp cc", monapp, handler.showCC);
            handler.clear();

            // move
            scene = ctx[2].getHScene();
            Rectangle bounds = scene.getBounds();
            Rectangle newBounds = new Rectangle(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4);
            scene.setBounds(newBounds);
            assertEquals("Scene should've been moved", newBounds, scene.getBounds());
            assertNotNull("testMove() should've been invoked", handler.moveCC);
            assertSame("Expected handler to be invoked within monapp cc", monapp, handler.moveCC);
            handler.clear();

            // order
            scene = ctx[1].getHScene();
            assertTrue("Scene should already be visible", scene.isVisible());
            assertFalse("Scene should not yet be in front", ctx[1].getLocation() == 0);
            scene.show();
            assertEquals("Scene should be in front", 0, ctx[1].getLocation());
            assertNotNull("testOrder() should've been invoked", handler.orderCC);
            assertSame("Expected handler to be invoked within monapp cc", monapp, handler.showCC);

            // How about if replaced by another monapp
            Context monapp2 = new Context(new AppID(5, 0x6002));
            try
            {
                Handler handler2 = new Handler();
                monapp2.setHandler(handler2);
                handler.clear();
                handler2.clear();

                scene = ctx[1].getHScene();
                scene.setVisible(false);
                scene.setVisible(true);
                assertNotNull("Expected handler2.testShow() to be invoked", handler2.showCC);
                assertNull("Expected handler.testShow() not to be invoked", handler.showCC);
                assertSame("Expected handler2 to be invoked within monapp2 cc", monapp2, handler2.showCC);
            }
            finally
            {
                monapp2.setHandler(null);
                monapp2.cleanup();
            }
        }
        finally
        {
            monapp.setHandler(null);

            for (int i = 0; i < ctx.length; ++i)
                ctx[i].cleanup();
            monapp.cleanup();
        }
    }

    /**
     * Test that the handler is not leaked.
     */
    public void testSetHSceneChangeRequestHandler_ReplaceLeak()
    {
        HSceneManager hsm = HSceneManager.getInstance();

        Handler h1 = new Handler();
        Handler h2 = new Handler();
        try
        {
            // Set handler
            hsm.setHSceneChangeRequestHandler(h1);

            Reference r = new WeakReference(h1);
            h1 = null;
            System.gc();
            System.gc();
            assertNotNull("Handler should not be collected yet", r.get());

            hsm.setHSceneChangeRequestHandler(h2);
            System.gc();
            System.gc();
            assertNull("Replaced handler has been leaked", r.get());
        }
        finally
        {
            hsm.setHSceneChangeRequestHandler(null);
        }

    }

    /**
     * Test that the handler is not leaked.
     */
    public void testSetHSceneChangeRequestHandler_ClearLeak()
    {
        HSceneManager hsm = HSceneManager.getInstance();

        Handler handler = new Handler();
        try
        {
            // Set handler
            hsm.setHSceneChangeRequestHandler(handler);

            Reference r = new WeakReference(handler);
            handler = null;
            System.gc();
            System.gc();
            assertNotNull("Handler should not be collected yet", r.get());

            hsm.setHSceneChangeRequestHandler(null);
            System.gc();
            System.gc();
            assertNull("Cleared handler has been leaked", r.get());
        }
        finally
        {
            hsm.setHSceneChangeRequestHandler(null);
        }
    }

    /**
     * Test that the handler is not leaked.
     */
    public void testSetHSceneChangeRequestHandler_ReplaceLeak_CC() throws Exception
    {
        HSceneManager hsm = HSceneManager.getInstance();

        Context a1 = new Context(new AppID(6, 0x6001));
        Context a2 = new Context(new AppID(6, 0x6002));
        Handler h1 = new Handler();
        Handler h2 = new Handler();
        try
        {
            // Set handler
            a1.setHandler(h1);

            Reference r = new WeakReference(h1);
            h1 = null;
            System.gc();
            System.gc();
            assertNotNull("Handler should not be collected yet", r.get());

            a2.setHandler(h2);
            System.gc();
            System.gc();
            assertNull("Replaced handler has been leaked", r.get());
        }
        finally
        {
            a1.cleanup();
            a2.cleanup();
            hsm.setHSceneChangeRequestHandler(null);
        }

    }

    /**
     * Test that the handler is not leaked.
     */
    public void testSetHSceneChangeRequestHandler_ClearLeak_CC() throws Exception
    {
        HSceneManager hsm = HSceneManager.getInstance();

        Context a1 = new Context(new AppID(7, 0x6001));
        Context a2 = new Context(new AppID(7, 0x6002));
        Handler handler = new Handler();
        try
        {
            // Set handler
            a1.setHandler(handler);

            Reference r = new WeakReference(handler);
            handler = null;
            System.gc();
            System.gc();
            assertNotNull("Handler should not be collected yet", r.get());

            a2.setHandler(null);
            System.gc();
            System.gc();
            assertNull("Cleared handler has been leaked", r.get());
        }
        finally
        {
            a1.cleanup();
            a2.cleanup();
            hsm.setHSceneChangeRequestHandler(null);
        }
    }

    /**
     * Test that the handler is implicitly removed on app shutdown.
     */
    public void testSetHSceneChangeRequestHandler_AppDestroyed() throws Exception
    {
        Context app = new Context(new AppID(8, 100));
        Context monapp = new Context(new AppID(8, 0x6001));

        try
        {
            Handler handler = new Handler();
            monapp.setHandler(handler);

            HScene scene = app.getFullScreenScene();
            scene.setVisible(true);
            scene.show();

            assertNotNull("Expected handler to be invoked", handler.showCC);
            assertSame("Expected handler to be invoked in proper context", monapp, handler.showCC);
            assertFalse("Expected scene to not be visible", scene.isVisible());
            handler.clear();

            // Kill monapp
            monapp.cleanup();
            monapp = null;

            scene.setVisible(true);
            scene.show();

            assertNull("Handler should not be invoked after death of app", handler.showCC);
            assertTrue("Expected scene to be made visible", scene.isVisible());

            Reference r = new WeakReference(handler);
            handler = null;
            System.gc();
            System.gc();
            assertNull("Handler of dead app was leaked", r.get());
        }
        finally
        {
            app.cleanup();
            if (monapp != null) monapp.cleanup();
        }
    }

    private HGraphicsConfiguration getGraphicsConfiguration(HScene scene)
    {
        HSceneTemplate t = scene.getSceneTemplate();
        return (HGraphicsConfiguration) t.getPreferenceObject(HSceneTemplate.GRAPHICS_CONFIGURATION);
    }

    private HScreenRectangle getRect(HGraphicsConfiguration c, Rectangle r)
    {
        HScreenPoint p = getPoint(c, r.getLocation());
        HScreenDimension d = getDimension(c, r.getSize());

        return new HScreenRectangle(p.x, p.y, d.width, d.height);
    }

    private HScreenPoint getPoint(HGraphicsConfiguration c, Point p)
    {
        HScreenRectangle hsr = c.getScreenArea();
        Dimension res = c.getPixelResolution();

        float xn = (res.width > 0) ? (hsr.x + p.x * hsr.width / res.width) : hsr.x;
        float yn = (res.height > 0) ? (hsr.y + p.y * hsr.height / res.height) : hsr.y;

        return new HScreenPoint(xn, yn);
    }

    private HScreenDimension getDimension(HGraphicsConfiguration c, Dimension d)
    {
        HScreenRectangle hsr = c.getScreenArea();
        Dimension res = c.getPixelResolution();

        float wn = (res.width > 0) ? (d.width * hsr.width / res.width) : 0;
        float hn = (res.height > 0) ? (d.height * hsr.height / res.height) : 0;

        return new HScreenDimension(wn, hn);
    }

    /**
     * Returns the HScreenRectangle for the given scene.
     */
    private HScreenRectangle getRect(HScene scene)
    {
        // return scene.getPixelCoordinatesHScreenRectangle(new Rectangle(new
        // Point(0,0), scene.getSize()));
        return getRect(getGraphicsConfiguration(scene), scene.getBounds());
    }

    /**
     * Checks the given HSceneBinding against the given context and scene.
     */
    private void checkBinding(String msg, HSceneBinding bind, Context ctx, HScene scene)
    {
        checkBinding(msg, bind, ctx, getRect(scene));
    }

    /**
     * Checks the given HSceneBinding against the given contxt and
     * HScreenRectangle.
     */
    private void checkBinding(String msg, HSceneBinding bind, Context ctx, HScreenRectangle rect)
    {
        assertNotNull(msg + "Expected non-null binding", bind);
        assertEquals(msg + "Unexpected binding id", ctx.id, bind.getAppAttributes().getIdentifier());
        TestSupport.assertEquals("Unexpected binding rect", rect, bind.getRectangle());
    }

    private class Context extends org.dvb.event.EventManagerTest.Context
    {
        public Context(AppID id)
        {
            super(id);
        }

        public HScene getHScene()
        {
            final HScene[] scene = { null };
            doRun(new Runnable()
            {
                public void run()
                {
                    scene[0] = HSceneFactory.getInstance().getDefaultHScene();
                }
            });
            return scene[0];
        }

        public HScene getFullScreenScene()
        {
            final HScene[] scene = { null };
            doRun(new Runnable()
            {
                public void run()
                {
                    scene[0] = HSceneFactory.getInstance().getFullScreenScene(
                            HScreen.getDefaultHScreen().getDefaultHGraphicsDevice());
                }
            });
            return scene[0];
        }

        public int getLocation()
        {
            final int[] loc = { -1 };
            doRun(new Runnable()
            {
                public void run()
                {
                    loc[0] = HSceneManager.getInstance().getAppHSceneLocation();
                }
            });
            return loc[0];
        }

        public void setHandler(HSceneChangeRequestHandler h)
        {
            final HSceneChangeRequestHandler[] ha = { h };
            doRun(new Runnable()
            {
                public void run()
                {
                    HSceneManager hsm = HSceneManager.getInstance();
                    hsm.setHSceneChangeRequestHandler(ha[0]);
                    ha[0] = null;
                }
            });
        }

        public Object get(Object key)
        {
            // This is a kludge if I ever saw one...
            // It's counting on how DefaultScene is currently implemented...
            // Or expecting any other impl to be similar.
            if (key == AppsDatabase.class)
            {
                return new AppsDatabase()
                {
                    public AppAttributes getAppAttributes(AppID appId)
                    {
                        if (!id.equals(appId)) return null;
                        return new OcapAppAttributes()
                        {
                            public int getApplicationControlCode()
                            {
                                return 0;
                            }

                            public int getStoragePriority()
                            {
                                return 0;
                            }

                            public boolean hasNewVersion()
                            {
                                return false;
                            }

                            public boolean isNewVersionSignaled()
                            {
                                // if hasNewVersion returns false, then a new
                                // version can't have been stored,
                                // so return false.
                                return false;
                            }

                            public AppIcon getAppIcon()
                            {
                                return null;
                            }

                            public AppID getIdentifier()
                            {
                                return id;
                            }

                            public boolean getIsServiceBound()
                            {
                                return true;
                            }

                            public String getName()
                            {
                                return "";
                            }

                            public String getName(String iso639code)
                            {
                                return "";
                            }

                            public String[][] getNames()
                            {
                                return null;
                            }

                            public int getPriority()
                            {
                                return 100;
                            }

                            public String[] getProfiles()
                            {
                                return null;
                            }

                            public Object getProperty(String index)
                            {
                                return null;
                            }

                            public Locator getServiceLocator()
                            {
                                return null;
                            }

                            public int getType()
                            {
                                return 0;
                            }

                            public int[] getVersions(String profile)
                            {
                                return null;
                            }

                            public boolean isStartable()
                            {
                                return true;
                            }

                            public boolean isVisible()
                            {
                                return true;
                            }

                            public int getApplicationMode()
                            {
                                return NORMAL_MODE;
                            }
                        };
                    }
                };
            }
            else
                return super.get(key);
        }
    }

    private class Handler implements HSceneChangeRequestHandler
    {
        boolean show;

        HSceneBinding showNew;

        HSceneBinding[] showCurr;

        CallerContext showCC;

        public boolean testShow(HSceneBinding newScene, HSceneBinding oldScenes[])
        {
            showNew = newScene;
            showCurr = oldScenes;
            showCC = getContext();
            return show;
        }

        boolean move;

        HSceneBinding moveNew;

        HSceneBinding[] moveCurr;

        CallerContext moveCC;

        public boolean testMove(HSceneBinding moveScene, HSceneBinding currentScenes[])
        {
            moveNew = moveScene;
            moveCurr = currentScenes;
            moveCC = getContext();
            return move;
        }

        boolean order;

        HSceneBinding[] orderScenes;

        int orderCurr = -1, orderNew = -1;

        CallerContext orderCC;

        public boolean testOrder(HSceneBinding currentScenes[], int currentOrder, int newOrder)
        {
            orderScenes = currentScenes;
            orderCurr = currentOrder;
            orderNew = newOrder;
            orderCC = getContext();
            return order;
        }

        void clear()
        {
            showNew = null;
            showCurr = null;
            showCC = null;
            moveNew = null;
            moveCurr = null;
            moveCC = null;
            orderScenes = null;
            orderCurr = -1;
            orderNew = -1;
            orderCC = null;
        }

        private CallerContext getContext()
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            return ccm.getCurrentContext();
        }
    }

    private CallerContextManager save;

    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(save));
    }

    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    /*  ***** Boilerplate ***** */
    protected void setUp() throws Exception
    {
        super.setUp();
        replaceCCMgr();
    }

    protected void tearDown() throws Exception
    {
        restoreCCMgr();
        super.tearDown();
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(HSceneManagerTest.class);
        return suite;
    }

    public HSceneManagerTest(String name)
    {
        super(name);
    }

}
