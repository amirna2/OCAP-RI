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

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Date;

import javax.media.Controller;
import javax.tv.locator.Locator;
import javax.tv.service.Service;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.media.VideoTransformation; //import org.ocap.media.BlockedService;
import org.ocap.media.CannedMediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;

import org.cablelabs.impl.media.JMFBaseInterfaceTest;
import org.cablelabs.impl.media.source.ServiceDataSource;

/**
 * ServicePlayerInterfaceTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class ServicePlayerInterfaceTest extends JMFBaseInterfaceTest
{

    private AbstractServicePlayer player;

    // private CannedSIDatabase sidb;
    private CannedControllerListener listener;

    private PlayerHelper helper;

    private MediaAccessHandlerRegistrar registrar;

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     */
    public ServicePlayerInterfaceTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public ServicePlayerInterfaceTest(String name, ImplFactory factory)
    {
        this(name, AbstractServicePlayer.class, factory);
    }

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(ServicePlayerInterfaceTest.class, factory);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        player = (AbstractServicePlayer) createImplObject();
        // sidb = playerFactory.getCannedSIDB();
        listener = new CannedControllerListener(1);
        ((Player) player).addControllerListener(listener);
        helper = new PlayerHelper(player, listener);

        registrar = MediaAccessHandlerRegistrar.getInstance();
    }

    public void tearDown() throws Exception
    {
        registrar.registerMediaAccessHandler(null);
        ((Player) player).close();
        listener.waitForControllerClosedEvent();
        listener = null;
        helper = null;
        player = null;
        super.tearDown();
    }

    public void testAlternateMediaPresentationEventWithMediaAccessHandler() throws Exception
    {
        // CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        // registrar.registerMediaAccessHandler(handler);
        //        
        // helper.prefetchPlayer();
        // handler.addAcceptPID(sidb.jmfServiceComponent1V.getPID());
        //        
        // AbstractServicePlayer sPlayer = player;
        // helper.callSyncStartWithNoWait();
        //
        // //
        // // we might still need to wait for the MediaPresentedEvent,
        // // and different implementations will give us a different
        // // number of events
        // //
        // listener.waitForMediaPresentationEvent();
        // int size = listener.events.size();
        // assertTrue(size > 0);
        // ControllerEvent cevent = listener.getEvent(size - 1);
        // assertTrue("Did not receive the AlternativeMediaPresentationEvent - "
        // +
        // cevent.getClass().getName(),
        // cevent instanceof AlternativeMediaPresentationEvent);
        // AlternativeMediaPresentationEvent altEvent =
        // (AlternativeMediaPresentationEvent)cevent;
        //        
        // //
        // // verify that the vidio component was accepted and presented
        // // and that the audio component was rejected
        // //
        // assertNotNull("Presented streams was null",
        // altEvent.getPresentedStreams());
        // assertEquals("Presented streams length is incorrect", 1,
        // altEvent.getPresentedStreams().length);
        // assertEquals("Presented streams PIDs do not match",
        // sidb.jmfServiceComponent1V.getPID(),
        // altEvent.getPresentedStreams()[0].getPID());
        //        
        // assertNotNull("Not presented streams was null",
        // altEvent.getNotPresentedStreams());
        // assertEquals("Not presented streams length is incorrect", 1,
        // altEvent.getNotPresentedStreams().length);
        // assertEquals("Not presented streams PIDS do not match",
        // sidb.jmfServiceComponent1A1.getPID(),
        // altEvent.getNotPresentedStreams()[0].getPID());
        // assertNotNull("Trigger should not be null", altEvent.getTrigger());
        // assertTrue("Trigger did not match expected value",
        // altEvent.getTrigger().equals(handler.cannedGetEvaluationTrigger()));
        //        
        // assertTrue("Source URL is null", altEvent.getSourceURL() != null);
        // String expectedLocatorStr =
        // sidb.jmfService1.getLocator().toExternalForm();
        // OcapLocator expectedOcapLocator = new
        // OcapLocator(expectedLocatorStr);
        // assertTrue("Source URL did not match expected",
        // altEvent.getSourceURL().equals(expectedOcapLocator));
        //        
        //        
        //
        // //
        // // verify that the MediaAccessAuthorization was called with the
        // // right parameters and streams
        // //
        // assertTrue(handler.isCheckMediaAccessAuthorizationCalled());
        // assertTrue(sPlayer.equals(handler.cannedGetPlayer()));
        // assertTrue("Incorrect Media Presentation Trigger - " +
        // handler.cannedGetEvaluationTrigger(),
        // MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE.equals(handler.cannedGetEvaluationTrigger()));
        // //
        // // the es list should contain the audio and visual component from
        // // JMFService1
        // //
        // ElementaryStream[] esList = handler.cannedGetEsList();
        // assertTrue("Elementary Stream array is null", esList != null);
        // assertTrue("Elementary Stream array is the wrong size -- " +
        // esList.length, esList.length == 2);
        // int audioPID = sidb.jmfServiceComponent1A1.getPID();
        // int vidioPID = sidb.jmfServiceComponent1V.getPID();
        // int es0PID = esList[0].getPID();
        // int es1PID = esList[1].getPID();
        //        
        // assertTrue(audioPID == es0PID || audioPID == es1PID);
        // assertTrue(vidioPID == es0PID || vidioPID == es1PID);
        // fail("ECN 972 rewrite");
    }

    public void testNormalMediaPresentationEventWithMediaAccessHandler() throws Exception
    {
        // CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        // registrar.registerMediaAccessHandler(handler);
        //        
        // handler.setAcceptAllPIDS(true);
        // helper.prefetchPlayer();
        //        
        // AbstractServicePlayer sPlayer = player;
        // helper.callSyncStartWithNoWait();
        // //
        // // we might still need to wait for the MediaPresentedEvent,
        // // and different implementations will give us a different
        // // number of events
        // //
        // listener.waitForMediaPresentationEvent();
        // int size = listener.events.size();
        // ControllerEvent cevent = listener.getEvent(size - 1);
        // assertTrue(handler.isCheckMediaAccessAuthorizationCalled());
        // assertTrue("Did not receive the NormalMediaPresentationEvent",
        // cevent instanceof NormalMediaPresentationEvent);
        // assertTrue(sPlayer.equals(handler.cannedGetPlayer()));
        // assertTrue("Incorrect Media Presentation Trigger - " +
        // handler.cannedGetEvaluationTrigger(),
        // MediaPresentationEvaluationTrigger.NEW_SELECTED_SERVICE.equals(handler.cannedGetEvaluationTrigger()));
        // MediaPresentationEvent mpevent = (MediaPresentationEvent)cevent;
        // assertNotNull("Trigger should not be null", mpevent.getTrigger());
        // assertTrue(mpevent.getTrigger().equals(handler.cannedGetEvaluationTrigger()));
        //        
        // //
        // // the es list should contain the audio and visual component from
        // // JMFService1
        // //
        // ElementaryStream[] esList = handler.cannedGetEsList();
        // assertTrue("Elementary Stream array is null", esList != null);
        // assertTrue("Elementary Stream array is the wrong size -- " +
        // esList.length, esList.length == 2);
        // fail("ECN 972 rewrite");
    }

    public void testGetServiceContentLocators()
    {
        helper.startPlayer();

        assertEquals("Player not in realized state", Controller.Started, ((Player) player).getState());
        Locator[] locatorArr = player.getServiceContentLocators();
        assertNotNull("Returned array should not be null", locatorArr);
        //
        // the audio and video components should be selected
        //
        assertEquals("Returned array length is incorrect", 2, locatorArr.length);
    }

    public void testUserRatingChangePlayerNotStarted() throws Exception
    {
        // //
        // // since the player is not presenting when the preferences are
        // // changed, the media access handler should not be called
        // //
        // CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        // MediaAccessHandlerRegistrar.getInstance().registerMediaAccessHandler(handler);
        // handler.addAcceptPID(sidb.jmfServiceComponent1V.getPID());
        // handler.addAcceptPID(sidb.jmfServiceComponent1A1.getPID());
        //
        // GeneralPreference pref = new GeneralPreference("Parental Rating");
        // UserPreferenceManager.getInstance().write(pref);
        // handler.waitForCheckMediaAccessAuthorizationCall(2000);
        // assertFalse("checkMediaAccessAuthorizaton should not have been called",
        // handler.isCheckMediaAccessAuthorizationCalled());
        // fail("ECN 972 rewrite");
    }

    public void testUserRatingChange() throws Exception
    {
        // // Register the MediaAccessHandler
        // CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        // registrar.registerMediaAccessHandler(handler);
        // handler.addAcceptPID(sidb.jmfServiceComponent1V.getPID());
        // handler.addAcceptPID(sidb.jmfServiceComponent1A1.getPID());
        //
        // // Start the player
        // ((Player)player).start();
        // listener.waitForMediaPresentationEvent();
        // assertEquals("Player not in started state", Controller.Started,
        // ((Player)player).getState());
        // listener.reset();
        //        
        // // Change the user rating and ensure the MediaAccessHandler is called
        // handler.setCheckMediaAccessAuthorizationCalled(false);
        // GeneralPreference pref = new GeneralPreference("Parental Rating");
        // UserPreferenceManager.getInstance().write(pref);
        // handler.waitForCheckMediaAccessAuthorizationCall(5000);
        // assertTrue(handler.isCheckMediaAccessAuthorizationCalled());
        // assertTrue("Incorrect Media Presentation Trigger - " +
        // handler.cannedGetEvaluationTrigger(),
        // MediaPresentationEvaluationTrigger.USER_RATING_CHANGED.equals(handler.cannedGetEvaluationTrigger()));
        // fail("ECN 972 rewrite");
    }

    /*
     * public void testServiceSelectionMAHNotifyFalse() throws Exception { //
     * Register the MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler();
     * registrar.registerMediaAccessHandler(handler);
     * registrar.setNotifyCondition(false);
     * 
     * // Start the player ((Player) player).start();
     * listener.waitForMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset();
     * 
     * assertTrue(!handler.isCheckMediaAccessAuthorizationCalled()); }
     * 
     * public void testServiceSelectionMAHNotifyTrue() throws Exception { //
     * Register the MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler(); handler.setAllowAuthorization(true);
     * registrar.registerMediaAccessHandler(handler);
     * registrar.setNotifyCondition(true);
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForNormalMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset();
     * handler.waitForCheckMediaAccessAuthorizationCall(3000l);
     * 
     * assertTrue(received);
     * assertTrue(handler.isCheckMediaAccessAuthorizationCalled()); }
     */
    /*
     * public void testServiceBlockedNotifyFalse() { // Register the
     * MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler();
     * registrar.registerMediaAccessHandler(handler); long currentTime =
     * System.currentTimeMillis(); ServiceDataSource sDataSource =
     * (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); BlockedService[] blockedServiceArr = new
     * BlockedService[] { new BlockedService(new Date(currentTime), 10000l,
     * service, false, 0) }; try {
     * registrar.setServiceBlocking(blockedServiceArr);
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForAlternativeMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset(); assertTrue(received);
     * assertTrue(!handler.isCheckMediaAccessAuthorizationCalled()); } finally {
     * registrar.removeServiceBlocking(blockedServiceArr); } }
     */
    //
    // block a service and start playing it, once it is playing remove the
    // service blocking and see if normal presentation occurs
    //
    /*
     * public void testServiceBlockedUnblockWhilePresenting() { // Register the
     * MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler();
     * registrar.registerMediaAccessHandler(handler); long currentTime =
     * System.currentTimeMillis(); ServiceDataSource sDataSource =
     * (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); BlockedService[] blockedServiceArr = new
     * BlockedService[] { new BlockedService(new Date(currentTime), 10000l,
     * service, false, 0) }; try {
     * registrar.setServiceBlocking(blockedServiceArr);
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForAlternativeMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset(); assertTrue(received);
     * 
     * // // remove the blocking and verify that the normal presentation occurs
     * // registrar.removeServiceBlocking(blockedServiceArr); received =
     * listener.waitForNormalMediaPresentationEvent(); assertTrue(received); }
     * finally { registrar.removeServiceBlocking(blockedServiceArr); } }
     */
    //
    // register a blocked service that starts after presentation, and
    // another that ends well before. Verify that the presentation
    // isn't affected
    //
    /*
     * public void testServiceBlockedNotInTimePeriod() { // Register the
     * MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler();
     * registrar.registerMediaAccessHandler(handler);
     * registrar.setNotifyCondition(false); long currentTime =
     * System.currentTimeMillis(); ServiceDataSource sDataSource =
     * (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); BlockedService[] blockedServiceArr = new
     * BlockedService[] { new BlockedService(new Date(currentTime-20000),
     * 10000l, service, false, 0), new BlockedService(new
     * Date(currentTime+2000000), 10000l, service, false, 0) }; try {
     * registrar.setServiceBlocking(blockedServiceArr);
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForNormalMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset(); assertTrue(received);
     * assertTrue(!handler.isCheckMediaAccessAuthorizationCalled()); } finally {
     * registrar.removeServiceBlocking(blockedServiceArr); } }
     */
    //
    // register the service to be blocked at a later time, start the
    // player and see if the presentation changes when the time
    // threshold is crossed
    //
    /*
     * public void testServiceBlockedTimePeriodEnteredWhilePlaying() { //
     * Register the MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler();
     * registrar.registerMediaAccessHandler(handler);
     * registrar.setNotifyCondition(false); long currentTime =
     * System.currentTimeMillis(); ServiceDataSource sDataSource =
     * (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); long startTime = currentTime + 2000l;
     * BlockedService[] blockedServiceArr = new BlockedService[] { new
     * BlockedService(new Date(startTime), 10000l, service, false, 0), }; try {
     * registrar.setServiceBlocking(blockedServiceArr);
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForNormalMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); assertTrue(received);
     * assertTrue(!handler.isCheckMediaAccessAuthorizationCalled());
     * 
     * received = listener.waitForAlternativeMediaPresentationEvent(); long
     * endTime = System.currentTimeMillis(); // // make sure we've waited past
     * the time for the service to be // blocked //
     * assertTrue("Blockage was scheduled to start at " + startTime +
     * " but the test ended at " + endTime, endTime >= startTime);
     * assertTrue("Did not receive alternative content event by " + endTime +
     * " but scheduled blocking for " + startTime, received); } finally {
     * registrar.removeServiceBlocking(blockedServiceArr); } }
     */
    //
    // register the service to be blocked at a later time, start the
    // player and see if the presentation changes when the time
    // threshold is crossed
    //
    /*
     * public void testServiceBlockedTimePeriodLeftWhilePlaying() { // Register
     * the MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler();
     * registrar.registerMediaAccessHandler(handler);
     * registrar.setNotifyCondition(false); long currentTime =
     * System.currentTimeMillis(); ServiceDataSource sDataSource =
     * (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); long startTime = currentTime; BlockedService[]
     * blockedServiceArr = new BlockedService[] { new BlockedService(new
     * Date(startTime), 5000l, service, false, 0), }; try {
     * registrar.setServiceBlocking(blockedServiceArr);
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForAlternativeMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); assertTrue(received);
     * assertTrue(!handler.isCheckMediaAccessAuthorizationCalled());
     * 
     * received = listener.waitForNormalMediaPresentationEvent(); long endTime =
     * System.currentTimeMillis(); // // make sure we've waited past the time
     * for the service to be // blocked //
     * assertTrue("Blockage was scheduled to end at " + startTime +
     * " but the test ended at " + endTime, endTime >= startTime);
     * assertTrue("Did not receive normal content event by " + endTime +
     * " but scheduled blocking for " + startTime, received); } finally {
     * registrar.removeServiceBlocking(blockedServiceArr); } }
     */
    //
    // start a player, specify a service to be blocked while that
    // service is currently being presented
    //
    /*
     * public void testServiceBlockedSpecifiedWhilePresenting() { // Register
     * the MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler();
     * registrar.registerMediaAccessHandler(handler);
     * registrar.setNotifyCondition(false);
     * 
     * // // block the currently presenting service // long currentTime =
     * System.currentTimeMillis(); ServiceDataSource sDataSource =
     * (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); BlockedService[] blockedServiceArr = new
     * BlockedService[] { new BlockedService(new Date(currentTime), 30000l,
     * service, false, 0) };
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForNormalMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset(); assertTrue(received); try {
     * registrar.setServiceBlocking(blockedServiceArr); received =
     * listener.waitForAlternativeMediaPresentationEvent();
     * 
     * assertTrue(received);
     * assertTrue(!handler.isCheckMediaAccessAuthorizationCalled()); } finally {
     * registrar.removeServiceBlocking(blockedServiceArr); } }
     */

    /*
     * public void testServiceBlockedNotifyTrueMAHDenies() { // Register the
     * MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler(); handler.setDenyAuthorization(true);
     * registrar.registerMediaAccessHandler(handler);
     * 
     * long currentTime = System.currentTimeMillis(); ServiceDataSource
     * sDataSource = (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); BlockedService[] blockedServiceArr = new
     * BlockedService[] { new BlockedService(new Date(currentTime), 10000l,
     * service, true, 0) }; try {
     * registrar.setServiceBlocking(blockedServiceArr); // Start the player
     * ((Player) player).start(); boolean received =
     * listener.waitForAlternativeMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset(); assertTrue(received);
     * assertTrue(handler.isCheckMediaAccessAuthorizationCalled()); } finally {
     * registrar.removeServiceBlocking(blockedServiceArr); } }
     */
    /*
     * public void testServiceBlockedNotifyTrueMAHAllows() { // Register the
     * MediaAccessHandler CannedMediaAccessHandler handler = new
     * CannedMediaAccessHandler(); handler.setAllowAuthorization(true);
     * registrar.registerMediaAccessHandler(handler);
     * 
     * long currentTime = System.currentTimeMillis(); ServiceDataSource
     * sDataSource = (ServiceDataSource)player.getSource(); Service service =
     * sDataSource.getService(); BlockedService[] blockedServiceArr = new
     * BlockedService[] { new BlockedService(new Date(currentTime), 10000l,
     * service, true, 0) }; try {
     * registrar.setServiceBlocking(blockedServiceArr);
     * 
     * // Start the player ((Player) player).start(); boolean received =
     * listener.waitForNormalMediaPresentationEvent();
     * assertEquals("Player not in started state", Controller.Started, ((Player)
     * player).getState()); listener.reset(); assertTrue(received);
     * assertTrue(handler.isCheckMediaAccessAuthorizationCalled()); } finally {
     * registrar.removeServiceBlocking(blockedServiceArr); } }
     */
    public void testSetInitialVideoSize()
    {
        VideoTransformation vt = new VideoTransformation();
        Rectangle bounds = new Rectangle(50, 50);
        Container container = new Container();
        assertTrue(container.getComponentCount() == 0);

        player.setInitialVideoSize(vt, container, bounds);
        assertTrue(container.getComponentCount() == 1);
        assertTrue(player.isComponentPlayer());

    }

    public void testSetInitialVideoSizeIllegalArgs()
    {
        VideoTransformation vt = new VideoTransformation();
        Rectangle bounds = null;
        Container container = new Container();

        try
        {
            player.setInitialVideoSize(vt, container, bounds);
            fail("setInitialVideoSize with a null bounds didn't fail");
        }
        catch (IllegalArgumentException exc)
        {
            // expected outcome
        }
    }

    public void testSwapDecoders()
    {
        ServicePlayer player2 = (ServicePlayer) createImplObject();
        CannedControllerListener listener2 = new CannedControllerListener(2);
        player2.addControllerListener(listener2);
        PlayerHelper helper2 = new PlayerHelper(player2, listener2);
        // Obtain a VideoDevice for player2
        helper2.startPlayer();
        // Obtain a VideoDevice for player
        helper.startPlayer();
        Container container = new Container()
        {
            public Point getLocationOnScreen()
            {
                return getLocation();
            }
        };
        Rectangle bounds = new Rectangle(0, 0, 320, 240);
        player2.setInitialVideoSize(null, container, bounds);
        try
        {
            assertTrue(!player.isComponentPlayer());
            player.swapDecoders(player2, true);

            assertTrue(player.isComponentPlayer());
            assertTrue(!player2.isComponentPlayer());

            player.swapDecoders(player2, true);
            assertTrue(!player.isComponentPlayer());
            assertTrue(player2.isComponentPlayer());
        }
        finally
        {
            player2.close();
        }
    }

    public void testSwapDecoderNullDevice()
    {
        ServicePlayer player2 = (ServicePlayer) createImplObject();
        try
        {
            player2.setVideoDevice(null);
            assertTrue(!player.isComponentPlayer());
            player.swapDecoders(player2, true);
            player.close();
            assertTrue(!player.isComponentPlayer());
        }
        finally
        {
            player2.close();
        }
    }
}
