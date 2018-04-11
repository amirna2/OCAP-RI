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

import java.util.Vector;

import javax.media.ControllerErrorEvent;
import javax.media.Player;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.media.MediaSelectEvent;
import javax.tv.media.MediaSelectFailedEvent;
import javax.tv.media.MediaSelectListener;
import javax.tv.media.MediaSelectSucceededEvent;
import javax.tv.service.selection.InvalidServiceComponentException;

import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.media.JMFBaseInterfaceTest;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.dvb.media.DVBMediaSelectControl;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * DVBMediaSelectControlTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class DVBMediaSelectControlTest extends JMFBaseInterfaceTest
{

    private DVBMediaSelectControl control;

    private CannedSIDatabase sidb;

    private Player player;

    private CannedMediaSelectListener listener;

    private CannedControllerListener cclistener;

    protected Locator[] JMFService1DefaultComponents;

    protected Locator[] JMFService2DefaultComponents;

    private int oldEvent = MediaAPI.Event.CONTENT_PRESENTING;

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(DVBMediaSelectControlTest.class, factory);
    }

    public DVBMediaSelectControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public DVBMediaSelectControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    // Test setup

    public void setUp() throws Exception
    {
        super.setUp();
        sidb = playerFactory.getCannedSIDB();

        JMFService1DefaultComponents = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A1.getLocator() };
        JMFService2DefaultComponents = new Locator[] { sidb.jmfServiceComponent2V.getLocator(),
                sidb.jmfServiceComponent2A1.getLocator() };

        player = (Player) createImplObject();
        control = (DVBMediaSelectControl) player.getControl(DVBMediaSelectControl.class.getName());
        listener = new CannedMediaSelectListener();
        control.addMediaSelectListener(listener);

        cclistener = new CannedControllerListener(1);
        player.addControllerListener(cclistener);
        player.realize();
        cclistener.waitForRealizeCompleteEvent();
        player.prefetch();
        cclistener.waitForPrefetchCompleteEvent();
        player.start();
        assertTrue("Didn't receive MediaPresentationEvent", cclistener.waitForMediaPresentationEvent());
        cclistener.reset();
    }

    public void tearDown() throws Exception
    {
        cma.cannedSetDecodeBroadcastEvent(oldEvent, oldEvent);
        control.removeMediaSelectListener(listener);
        player.close();
        listener = null;
        control = null;
        player = null;
        cclistener = null;

        super.tearDown();
    }

    // Test section

    // /////////////////////////////////
    // selectServiceMediaComponents(Locator)
    // /////////////////////////////////

    /*
     * Player is not service-bound and successfully selects a different Service
     */
    public void testSelectServiceMediaComponentsSuccessNotServiceBound() throws Exception
    {
        control.selectServiceMediaComponents(sidb.jmfLocator2);
        checkEvent(MediaSelectSucceededEvent.class);
        checkComponents(JMFService2DefaultComponents, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService2DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to select another Service due to
     * an unknown failure.
     */
    public void testSelectServiceMediaComponentsFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.CONTENT_PRESENTING);

        control.selectServiceMediaComponents(sidb.jmfLocator2);
        checkEvent(MediaSelectFailedEvent.class);
        checkComponents(JMFService2DefaultComponents, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService1DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to select another Service due to
     * an unknown failure twice, closing the player.
     */
    public void testSelectServiceMediaComponentsFailureUnknownFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.FAILURE_UNKNOWN);

        control.selectServiceMediaComponents(sidb.jmfLocator2);
        checkEvent(MediaSelectFailedEvent.class);
        checkComponents(JMFService2DefaultComponents, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkFailure(-1, ControllerErrorEvent.class);
    }

    /*
     * Player is not service-bound, but fails to select another Service due to
     * CA denied, then an unknown failure, finally closing the player.
     */
    public void testSelectServiceMediaComponentsFailureCADeniedFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.FAILURE_UNKNOWN);

        control.selectServiceMediaComponents(sidb.jmfLocator2);
        checkEvent(MediaSelectFailedEvent.class);
        checkComponents(JMFService2DefaultComponents, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkFailure(-1, ControllerErrorEvent.class);
    }

    /*
     * Player is service-bound, and successfully selects the (same) Service
     */
    public void testSelectServiceMediaComponentsSuccessServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        control.selectServiceMediaComponents(sidb.jmfLocator1);
        checkEvent(MediaSelectSucceededEvent.class);
        checkComponents(JMFService1DefaultComponents, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService1DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Failure to select due to invalid locator (i.e. a network locator)
     */
    public void testSelectServiceMediaComponentsInvalidLocatorException() throws Exception
    {
        try
        {
            control.selectServiceMediaComponents(sidb.network6.getLocator());
            fail("Expected InvalidLocatorException");
        }
        catch (InvalidLocatorException ex)
        {
            // expected
        }
    }

    /*
     * Failure to select due to invalid service components.
     */
    public void testSelectServiceMediaComponentsInvalidServiceComponentException() throws Exception
    {
        try
        {
            control.selectServiceMediaComponents(sidb.service21.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    // /////////////////////////////////
    // select(Locator)
    // /////////////////////////////////

    /*
     * Player is not service-bound, and successfully selects a single
     * ServiceComponent from the same Service
     */
    public void testSelectSingleLocatorSuccessNotServiceBound() throws Exception
    {
        control.select(sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to select a single component due
     * to an unknown failure.
     */
    public void testSelectSingleLocatorFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.CONTENT_PRESENTING);

        control.select(sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService1DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to select a single component due
     * to an unknown failure, another unknown failure, finally Player closes.
     */
    public void testSelectSingleLocatorFailureUnknownFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.FAILURE_UNKNOWN);

        control.select(sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkFailure(-1, ControllerErrorEvent.class);
    }

    /*
     * Player is not service-bound, but fails to select a single component due
     * to an unknown failure, then CA denial, then Player stops.
     */
    public void testSelectSingleLocatorFailureUnknownFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.select(sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to select a single component due
     * to a CA denial failure.
     */
    public void testSelectSingleLocatorFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.CONTENT_PRESENTING);
        //    	
        // control.select(sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkComponents(JMFService1DefaultComponents,
        // control.getCurrentSelection());
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to select a single component due
     * to a CA denial failure, an unknown failure, then Player closes.
     */
    public void testSelectSingleLocatorFailureCADeniedFailureUnknownNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_UNKNOWN);
        //    	
        // control.select(sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(-1, ControllerErrorEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to select a single component due
     * to a CA denial failure, another CA denial, then Player stops.
     */
    public void testSelectSingleLocatorFailureCADeniedFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.select(sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is service-bound, and successfully selects a single
     * ServiceComponent from the same Service
     */
    public void testSelectSingleLocatorSuccessServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        control.select(sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is service-bound, but fails to select a ServiceComponent from a
     * different Service
     */
    public void testSelectSingleLocatorFailureServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        try
        {
            control.selectServiceMediaComponents(sidb.jmfServiceComponent2A2.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    /*
     * Failure to select due to invalid locator (i.e. a network locator)
     */
    public void testSelectSingleLocatorInvalidLocatorException() throws Exception
    {
        try
        {
            control.select(sidb.network6.getLocator());
            fail("Expected InvalidLocatorException");
        }
        catch (InvalidLocatorException ex)
        {
            // expected
        }
    }

    // /////////////////////////////////
    // select(Locator[])
    // /////////////////////////////////

    /*
     * Player is not service-bound, and successfully selects multiple
     * ServiceComponents from a different Service
     */
    public void testSelectMultipleLocatorsSuccessNotServiceBound() throws Exception
    {
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent2V.getLocator(),
                sidb.jmfServiceComponent2A1.getLocator(), sidb.jmfServiceComponent2S1.getLocator() };
        control.select(expected);
        checkEvent(MediaSelectSucceededEvent.class);
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to select multiple components due
     * to an unknown failure.
     */
    public void testSelectMultipleLocatorsFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.CONTENT_PRESENTING);
        Locator[] locs = new Locator[] { sidb.jmfServiceComponent2V.getLocator(),
                sidb.jmfServiceComponent2A1.getLocator(), sidb.jmfServiceComponent2S1.getLocator() };
        control.select(locs);
        checkEvent(MediaSelectFailedEvent.class);
        checkComponents(locs, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService1DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to select multiple components due
     * to an unknown failure, another unknown failure, then Player closes.
     */
    public void testSelectMultipleLocatorsFailureUnknownFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.FAILURE_UNKNOWN);
        Locator[] locs = new Locator[] { sidb.jmfServiceComponent2V.getLocator(),
                sidb.jmfServiceComponent2A1.getLocator(), sidb.jmfServiceComponent2S1.getLocator() };
        control.select(locs);
        checkEvent(MediaSelectFailedEvent.class);
        checkComponents(locs, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkFailure(-1, ControllerErrorEvent.class);
    }

    /*
     * Player is not service-bound, but fails to select multiple components due
     * to a CA denial failure.
     */
    public void testSelectMultipleLocatorsFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.CONTENT_PRESENTING);
        // Locator[] locs = new Locator[]
        // {
        // sidb.jmfServiceComponent2V.getLocator(),
        // sidb.jmfServiceComponent2A1.getLocator(),
        // sidb.jmfServiceComponent2S1.getLocator()
        // };
        // control.select(locs);
        // checkEvent(MediaSelectFailedEvent.class);
        // checkComponents(locs,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkComponents(JMFService1DefaultComponents,
        // control.getCurrentSelection());
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to select multiple components due
     * to a CA denial failure, unknown failure, then Player closes.
     */
    public void testSelectMultipleLocatorsFailureCADeniedFailureUnknownNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_UNKNOWN);
        // Locator[] locs = new Locator[]
        // {
        // sidb.jmfServiceComponent2V.getLocator(),
        // sidb.jmfServiceComponent2A1.getLocator(),
        // sidb.jmfServiceComponent2S1.getLocator()
        // };
        // control.select(locs);
        // checkEvent(MediaSelectFailedEvent.class);
        // checkComponents(locs,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(-1, ControllerErrorEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to select multiple components due
     * to a CA denial failure, another CA denial, then Player stops.
     */
    public void testSelectMultipleLocatorsFailureCADeniedFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        // Locator[] locs = new Locator[]
        // {
        // sidb.jmfServiceComponent2V.getLocator(),
        // sidb.jmfServiceComponent2A1.getLocator(),
        // sidb.jmfServiceComponent2S1.getLocator()
        // };
        // control.select(locs);
        // checkEvent(MediaSelectFailedEvent.class);
        // checkComponents(locs,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is service-bound, and successfully selects multiple
     * ServiceComponents from the same Service
     */
    public void testSelectMultipleLocatorsSuccessServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A2.getLocator(), sidb.jmfServiceComponent1S1.getLocator() };
        control.select(expected);
        checkEvent(MediaSelectSucceededEvent.class);
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is service-bound, but fails to multiple ServiceComponents from a
     * different Service
     */
    public void testSelectMultipleLocatorsFailureServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);

        Locator[] locs = new Locator[] { sidb.jmfServiceComponent2V.getLocator(),
                sidb.jmfServiceComponent2A1.getLocator(), sidb.jmfServiceComponent2S1.getLocator() };
        try
        {
            control.select(locs);
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    /*
     * Failure to select due to invalid locator (i.e. a network locator)
     */
    public void testSelectMultipleLocatorsInvalidLocatorException() throws Exception
    {
        try
        {
            control.select(new Locator[] { sidb.network6.getLocator() });
            fail("Expected InvalidLocatorException");
        }
        catch (InvalidLocatorException ex)
        {
            // expected
        }
    }

    // /////////////////////////////////
    // add()
    // /////////////////////////////////

    /*
     * Player is not service-bound, and successfully adds a component from the
     * same service
     */
    public void testAddSuccessSameServiceNotServiceBound() throws Exception
    {
        control.add(sidb.jmfServiceComponent1S1.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1S1.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, and successfully adds a component from a
     * different service
     */
    public void testAddFailureDifferentServiceNotServiceBound() throws Exception
    {
        try
        {
            control.add(sidb.jmfServiceComponent2S1.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    /*
     * Fail to add a second audio component when one is already selected.
     */
    public void testAddFailureInvalidServiceComponentExceptionDuplicateComponent() throws Exception
    {
        try
        {
            control.add(sidb.jmfServiceComponent1A2.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    /*
     * Fail to add a non-presentable data component
     */
    public void testAddFailureInvalidServiceComponentExceptionNonPresentableComponent() throws Exception
    {
        try
        {
            control.add(sidb.jmfServiceComponent1D2.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    /*
     * Player is not service-bound, but fails to add the component due to an
     * unknown failure.
     */
    public void testAddFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.CONTENT_PRESENTING);

        control.add(sidb.jmfServiceComponent1S1.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1S1.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService1DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to add the component due to an
     * unknown failure, another unknown failure, then Player closes.
     */
    public void testAddFailureUnknownFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.FAILURE_UNKNOWN);

        control.add(sidb.jmfServiceComponent1S1.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1S1.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkFailure(-1, ControllerErrorEvent.class);
    }

    /*
     * Player is not service-bound, but fails to add the component due to an
     * unknown failure, then CA denial, then Player stops.
     */
    public void testAddFailureUnknownFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.add(sidb.jmfServiceComponent1S1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1S1.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to add the component due to a CA
     * denied failure
     */
    public void testAddFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.CONTENT_PRESENTING);
        //    	
        // control.add(sidb.jmfServiceComponent1S1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1S1.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkComponents(JMFService1DefaultComponents,
        // control.getCurrentSelection());
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to add the component due to a CA
     * denied failure, then unknown failure, then Player closes.
     */
    public void testAddFailureCADeniedFailureUnknownNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_UNKNOWN);
        //    	
        // control.add(sidb.jmfServiceComponent1S1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1S1.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(-1, ControllerErrorEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to add the component due to a CA
     * denied failure, another CA denial, then Player stops.
     */
    public void testAddFailureCADeniedFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.add(sidb.jmfServiceComponent1S1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1S1.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is service bound, and successfully adds another component.
     */
    public void testAddSuccessSameServiceServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        control.add(sidb.jmfServiceComponent1S1.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1S1.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is service-bound, but fails to select a component from another
     * service
     */
    public void testAddFailureDifferentServiceServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        try
        {
            control.add(sidb.jmfServiceComponent2S1.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    // /////////////////////////////////
    // remove()
    // /////////////////////////////////

    /*
     * Player is not service-bound, and successfully removes a component from
     * the same service
     */
    public void testRemoveSuccessNotServiceBound() throws Exception
    {
        control.remove(sidb.jmfServiceComponent1A1.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Fail to remove a component when it is not selected.
     */
    public void testRemoveFailureInvalidLocatorExceptionNotSelectedComponent() throws Exception
    {
        try
        {
            control.remove(sidb.jmfServiceComponent1S1.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidLocatorException ex)
        {
            // expected
        }
    }

    /*
     * Player is not service-bound, but fails to remove the component due to an
     * unknown failure.
     */
    public void testRemoveFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.CONTENT_PRESENTING);

        control.remove(sidb.jmfServiceComponent1A1.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService1DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to remove the component due to an
     * unknown failure, then another unknown failure, then Player closes.
     */
    public void testRemoveFailureUnknownFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.FAILURE_UNKNOWN);

        control.remove(sidb.jmfServiceComponent1A1.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkFailure(-1, ControllerErrorEvent.class);
    }

    /*
     * Player is not service-bound, but fails to remove the component due to an
     * unknown failure, then a CA denial, then Player stops.
     */
    public void testRemoveFailureUnknownFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.remove(sidb.jmfServiceComponent1A1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to remove the component due to a
     * CA denied failure
     */
    public void testRemoveFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.CONTENT_PRESENTING);
        //    	
        // control.remove(sidb.jmfServiceComponent1A1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkComponents(JMFService1DefaultComponents,
        // control.getCurrentSelection());
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to remove the component due to a
     * CA denied failure, then unknown failure, then Player closes.
     */
    public void testRemoveFailureCADeniedFailureUnknownNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_UNKNOWN);
        //    	
        // control.remove(sidb.jmfServiceComponent1A1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(-1, ControllerErrorEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to remove the component due to a
     * CA denied failure, another CA denial, then Player stops.
     */
    public void testRemoveFailureCADeniedFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.remove(sidb.jmfServiceComponent1A1.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is service bound, and successfully adds another component.
     */
    public void testRemoveSuccessSameServiceServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        control.remove(sidb.jmfServiceComponent1A1.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    // /////////////////////////////////
    // replace()
    // /////////////////////////////////

    /*
     * Player is not service-bound, and successfully replaces a component from
     * the same service (same type)
     */
    public void testReplaceSuccessSameTypeNotServiceBound() throws Exception
    {
        control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, and successfully replaces a component from
     * the same service (different type)
     */
    public void testReplaceSuccessDifferentTypeNotServiceBound() throws Exception
    {
        control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1S1.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1S1.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Fail to replace a component when it is not selected.
     */
    public void testReplaceFailureInvalidLocatorExceptionNotSelectedComponent() throws Exception
    {
        try
        {
            control.replace(sidb.jmfServiceComponent1S1.getLocator(), sidb.jmfServiceComponent1S2.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidLocatorException ex)
        {
            // expected
        }
    }

    /*
     * Fail to replace a component with a non-presentable one
     */
    public void testReplaceFailureInvalidServiceComponentExceptionNonPresentableComponent() throws Exception
    {
        try
        {
            control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1D2.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    /*
     * Player is not service-bound, but fails to replace the component due to an
     * unknown failure.
     */
    public void testReplaceFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.CONTENT_PRESENTING);

        control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(JMFService1DefaultComponents, control.getCurrentSelection());
    }

    /*
     * Player is not service-bound, but fails to replace the component due to an
     * unknown failure, another unknown failure, then Player closes.
     */
    public void testReplaceFailureUnknownFailureUnknownNotServiceBound() throws Exception
    {
        oldEvent = cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN, MediaAPI.Event.FAILURE_UNKNOWN);

        control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectFailedEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkFailure(-1, ControllerErrorEvent.class);
    }

    /*
     * Player is not service-bound, but fails to replace the component due to an
     * unknown failure, then CA denial, then Player stops.
     */
    public void testReplaceFailureUnknownFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_UNKNOWN,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.replace(sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to remove the component due to a
     * CA denied failure
     */
    public void testReplaceFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.CONTENT_PRESENTING);
        //    	
        // control.replace(sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkComponents(JMFService1DefaultComponents,
        // control.getCurrentSelection());
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to remove the component due to a
     * CA denied failure, then unknown failure, then Player closes.
     */
    public void testReplaceFailureCADeniedFailureUnknownNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_UNKNOWN);
        //    	
        // control.replace(sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(-1, ControllerErrorEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is not service-bound, but fails to remove the component due to a
     * CA denied failure, another CA denial, then Player stops.
     */
    public void testReplaceFailureCADeniedFailureCADeniedNotServiceBound() throws Exception
    {
        // oldEvent =
        // cma.cannedSetDecodeBroadcastEvent(MediaAPI.Event.FAILURE_CA_DENIED,
        // MediaAPI.Event.FAILURE_CA_DENIED);
        //    	
        // control.replace(sidb.jmfServiceComponent1A1.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator());
        // checkEvent(MediaSelectFailedEvent.class);
        // Locator[] expected = new Locator[]
        // {
        // sidb.jmfServiceComponent1V.getLocator(),
        // sidb.jmfServiceComponent1A2.getLocator()
        // };
        // checkComponents(expected,
        // ((MediaSelectEvent)listener.events.get(0)).getSelection());
        // checkFailure(2, CAStopEvent.class);
        // fail("ECN 972 rewrite");
    }

    /*
     * Player is service-bound, and successfully adds another component.
     */
    public void testReplaceSuccessSameServiceServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());
    }

    /*
     * Player is service-bound, but fails to replace a component with one from a
     * different service
     */
    public void testReplaceFailureDifferentServiceServiceBound() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        try
        {
            control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent2A1.getLocator());
            fail("Expected InvalidServiceComponentException");
        }
        catch (InvalidServiceComponentException ex)
        {
            // expected
        }
    }

    /*
     * Player is service-bound, and successfully adds another component. Then
     * the player is stopped and restarted. The components displayed should be
     * the components after the replacement, not the originals
     */
    public void testReplaceSuccessSameServiceServiceBoundTheStopAndRestart() throws Exception
    {
        ((CannedBroadcastPlayer) player).cannedSetServiceBound(true);
        //
        // replace the audio component and verify that it succeeded
        //
        control.replace(sidb.jmfServiceComponent1A1.getLocator(), sidb.jmfServiceComponent1A2.getLocator());
        checkEvent(MediaSelectSucceededEvent.class);
        Locator[] expected = new Locator[] { sidb.jmfServiceComponent1V.getLocator(),
                sidb.jmfServiceComponent1A2.getLocator() };
        checkComponents(expected, ((MediaSelectEvent) listener.events.get(0)).getSelection());
        checkComponents(expected, control.getCurrentSelection());

        //
        // restart the player
        //
        player.stop();
        cclistener.waitForStopEvent();
        player.start();
        cclistener.waitForMediaPresentedEvent();

        //
        // check the current components
        // 
        checkComponents(expected, control.getCurrentSelection());
    }

    // /////////////////////////////////
    // Helper method section
    // /////////////////////////////////

    private void checkComponents(Locator[] expected, Locator[] actual)
    {
        assertEquals("Number of components is incorrect", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++)
        {
            boolean match = false;
            for (int j = 0; j < actual.length; j++)
            {
                if (expected[i].equals(actual[j]) || expected[i] == actual[j])
                {
                    match = true;
                    break;
                }
            }
            if (!match) fail("Locator " + expected[i] + " was not selected");
        }
    }

    private void checkEvent(Class expectedEvent)
    {
        listener.waitForEvents(1);
        assertEquals("Number of events is incorrect", 1, listener.events.size());
        MediaSelectEvent event = (MediaSelectEvent) listener.events.get(0);
        assertTrue("Event received is incorrect, expected " + expectedEvent.getName(),
                expectedEvent.isAssignableFrom(event.getClass()));
    }

    private void checkFailure(int compCount, Class event)
    {
        cclistener.waitForEvents(1);
        assertEquals("Events received count is incorrect", 1, cclistener.events.size());
        assertTrue("Didn't receive " + event + ", instead received " + cclistener.getEvent(0).getClass(),
                event.isAssignableFrom(cclistener.getEvent(0).getClass()));
        if (compCount != -1)
            assertEquals("Returned array length is incorrect", compCount, control.getCurrentSelection().length);
    }

    private class CannedMediaSelectListener implements MediaSelectListener
    {
        private final static int WAIT_TIME = 8000;

        public Vector events = new Vector();

        public void selectionComplete(MediaSelectEvent event)
        {
            events.add(event);
        }

        public void waitForEvents(int count)
        {
            long startTime = System.currentTimeMillis();

            //
            // wait until we get the correct number of events or until
            // we get tired of waiting
            // 
            while (events.size() < count && System.currentTimeMillis() < startTime + WAIT_TIME)
            {
                Thread.yield();
                synchronized (events)
                {
                    try
                    {
                        events.wait(WAIT_TIME / 20);
                    }
                    catch (InterruptedException exc)
                    {
                        //
                        // if we got interrupted, break out of the loop and
                        // return
                        //
                        exc.printStackTrace();
                        return;
                    }// end catch
                }// end synch
            }// end while
        }// end waitForEvents()

    }

}
