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
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.media.Clock;
import javax.media.Control;
import javax.media.Controller;
import javax.media.IncompatibleSourceException;
import javax.media.Time;
import javax.media.TimeBase;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.ServiceMediaHandler;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventChangeListener;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.media.JMFFactory;
import org.cablelabs.impl.media.JMFTests;
import org.cablelabs.impl.media.mpe.CannedDVRAPI;
import org.cablelabs.impl.media.protocol.recording.DataSource;
import org.cablelabs.impl.media.source.CannedTSBDataSource;
import org.cablelabs.impl.media.source.TSBDataSource;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.javatv.selection.DVRPresentation;
import org.cablelabs.impl.service.javatv.selection.DVRServiceContextExt;
import org.cablelabs.impl.service.javatv.selection.ServiceContextCallback;
import org.davic.media.MediaPresentedEvent;
import org.davic.net.tuning.NetworkInterface;
import org.dvb.media.VideoTransformation;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaPresentationEvent;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.media.EnteringLiveModeEvent;
import org.ocap.shared.media.TimeShiftControl;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * TSBPlayerTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class TSBPlayerTest extends TestCase
{

    private TSBServiceMediaHandler player;

    private CannedControllerListener listener;

    private MediaAccessHandlerRegistrar registrar;

    private TSBPlayerFactory factory;

    private CannedSIDatabase sidb;

    private PlayerHelper helper;

    /**
     *
     */
    public TSBPlayerTest()
    {
        super();
        // TODO (Josh) Implement
    }

    /**
     * @param name
     */
    public TSBPlayerTest(String name)
    {
        super(name);
        // TODO (Josh) Implement
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite(TSBPlayerTest.class);
        suite.setName("TSBPlayerTest");

        TSBPlayerFactory factory = new TSBPlayerFactory();

        InterfaceTestSuite[] testSuites = new InterfaceTestSuite[] {
                // Currently two failures in this test due to incomplete
                // implementation
                ServicePlayerInterfaceTest.isuite(factory),
                // ///////////////////////////////////////////////
                AVPlayerInterfaceTest.isuite(factory),
                PlayerInterfaceTest.isuite(factory),
                PlayerStopTimeTest.isuite(factory),
                // This test currently fails due to bug 4424
                AudioLanguageControlTest.isuite(factory),
                // ///////////////////////////////////////////////
                // This test is disabled until the control is finished
                // SubtitlingLanguageControlTest.isuite(factory),
                // ///////////////////////////////////////////////
                AWTVideoSizeControlTest.isuite(factory), BackgroundVideoPresentationControlTest.isuite(factory),
                ClosedCaptioningControlTest.isuite(factory), FreezeControlTest.isuite(factory),
                MediaTimeEventControlTest.isuite(factory), TimeShiftControlTest.isuite(factory),
                VideoComponentControlTest.isuite(factory), VideoFormatControlTest.isuite(factory),
                // Most of the tests in this fail because the control is not
                // finished
                VideoPresentationControlTest.isuite(factory)
        // ///////////////////////////////////////////////
        };

        for (int i = 0; i < testSuites.length; i++)
        {
            suite.addTest(testSuites[i]);
        }

        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();

        factory = new TSBPlayerFactory();
        factory.setUp();
        sidb = factory.getCannedSIDB();
        player = (TSBServiceMediaHandler) factory.createImplObject();
        listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);
        registrar = MediaAccessHandlerRegistrar.getInstance();

    }

    public void tearDown() throws Exception
    {
        player.close();
        player = null;
        sidb = null;
        factory.tearDown();
        factory = null;
        helper = null;

        registrar.registerMediaAccessHandler(null);
        registrar = null;
        super.tearDown();
    }

    // Test section

    public void testGetDuration()
    {
        assertEquals("Returned time does not match", 60000000000L, player.getDuration().getNanoseconds());
    }

    public void testStartPlayerEvents()
    {
        helper.startPlayer();
        assertEquals("Player should be started", Controller.Started, player.getState());
        assertEquals("Event count is incorrect", 8, listener.events.size());
        assertTrue("Expected EnteringLiveModeEvent, instead got " + listener.events.get(5).getClass(),
                listener.events.get(5) instanceof EnteringLiveModeEvent);
        assertTrue("Expected MediaPresentedEvent, instead got " + listener.events.get(6).getClass(),
                listener.events.get(6) instanceof MediaPresentedEvent);
        assertTrue("Expected MediaPresentationEvent, instead got " + listener.events.get(7).getClass(),
                listener.events.get(7) instanceof MediaPresentationEvent);
    }

    public void testOnlyAuthorizedComponentsDecodedLIVE() throws Exception
    {
        // CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        // registrar.registerMediaAccessHandler(handler);
        //
        // // TODO: commented-out for ECN 972 integration
        // // handler.addAcceptPID(sidb.jmfServiceComponent1V.getPID());
        //
        // helper.startPlayer();
        // //
        // // wait for an extra event that won't be delivered --
        // // this is to make sure that the decoding has been done
        // //
        // listener.waitForEvents(listener.events.size() + 1);
        //
        // CannedMediaAPI mediaAPI = (CannedMediaAPI)player.getMediaAPI();
        // int[] pids = mediaAPI.cannedGetDecodeRecordingPIDs();
        // assertNotNull("PIDs should not be null", pids);
        // assertEquals("PIDs array length is incorrect", 1, pids.length);
        // assertEquals("PID does not match",
        // sidb.jmfServiceComponent1V.getPID(), pids[0]);
        // fail("ECN 972 rewrite");
    }

    public void testOnlyAuthorizedComponentsDecodedDVR()
    {
        // CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        // registrar.registerMediaAccessHandler(handler);
        //
        // // TODO: commented-out for ECN 972 integration
        // // handler.addAcceptPID(sidb.jmfServiceComponent1V.getPID());
        //
        // helper.startPlayer();
        // //
        // // wait for an extra event that won't be delivered --
        // // this is to make sure that the decoding has been done
        // //
        // listener.waitForEvents(listener.events.size() + 1);
        //
        // //
        // // changing the rate will cause the player to switch from
        // // live mode to dvr mode
        // //
        // player.setRate(0.5f);
        //
        // CannedDVRAPI dvrAPI = (CannedDVRAPI)player.getDVRAPI();
        // int[] pids = dvrAPI.cannedGetDecodeTSBPIDs();
        // assertNotNull("PIDs should not be null", pids);
        // assertEquals("PIDs array length is incorrect", 1, pids.length);
        // assertEquals("PID does not match",
        // sidb.jmfServiceComponent1V.getPID(), pids[0]);
        // fail("ECN 972 rewrite");
    }

    public void testSetSource() throws Exception
    {
        // We already know that setting the source works with the right
        // DataSource
        // So let's set one that is incorrect
        try
        {
            player.setSource(new DataSource());
            fail("Exception should've been thrown when using incorrect DataSource");
        }
        catch (IncompatibleSourceException ex)
        {
        }
    }

    public void testGetDurationClosed()
    {
        player.close();
        assertTrue(player.isClosed());
        Time duration = player.getDuration();
        assertTrue(AbstractPlayer.CLOSED_TIME.equals(duration));
    }

    public void testGetTimeShiftControlNotNull()
    {
        Control c = player.getControl("org.ocap.shared.media.TimeShiftControl");
        assertTrue(c != null);
    }

    public void testGetBeginningOfBufferClosed()
    {
        TimeShiftControl control = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
        player.close();
        assertTrue(player.isClosed());
        Time beginning = control.getBeginningOfBuffer();
        assertTrue(AbstractPlayer.CLOSED_TIME.equals(beginning));
    }

    public void testGetEndOfBufferClosed()
    {
        TimeShiftControl control = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
        player.close();
        assertTrue(player.isClosed());
        Time end = control.getEndOfBuffer();
        assertTrue(AbstractPlayer.CLOSED_TIME.equals(end));
    }

    public void testDurationFromControlClosed()
    {
        TimeShiftControl control = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
        player.close();
        assertTrue(player.isClosed());
        Time duration = control.getDuration();
        assertTrue(AbstractPlayer.CLOSED_TIME.equals(duration));
    }

    public void testMaxDurationClosed()
    {
        TimeShiftControl control = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
        player.close();
        assertTrue(player.isClosed());
        Time duration = control.getMaxDuration();
        assertTrue(AbstractPlayer.CLOSED_TIME.equals(duration));
    }

    /*
     * public void testGetEndOfBuffer() {
     * //assertEquals("Returned time does not match", new
     * Time(60000000000L).getNanoseconds(), //
     * player.getEndOfBuffer().getNanoseconds()); }
     * 
     * public void xtestGetMaxDuration() { fail("Test not finished yet"); }
     */

    private static class TSBPlayerFactory extends JMFFactory
    {

        DVRAPIManager oldDVR;

        ServiceContextExt sce;

        Exception error = null;

        boolean done = false;

        public void setUp() throws Exception
        {
            super.setUp();

            oldDVR = (DVRAPIManager) ManagerManager.getInstance(DVRAPIManager.class);
            CannedDVRAPI cda = (CannedDVRAPI) CannedDVRAPI.getInstance();
            ManagerManagerTest.updateManager(DVRAPIManager.class, cda.getClass(), true, cda);
        }

        public void tearDown() throws Exception
        {
            ManagerManagerTest.updateManager(DVRAPIManager.class, oldDVR.getClass(), true, oldDVR);
            if (sce != null)
            {
                sce.destroy();
            }
            super.tearDown();
        }

        public Object createImplObject() throws Exception
        {
            final TSBServiceMediaHandler player = new CannedTSBPlayer(null);

            // Runnable r = new Runnable()
            // {
            // public void run()
            // {
            // synchronized(player)
            // {
            try
            {
                TSBDataSource ds = new TSBDataSource(new CannedTSBDataSource.CannedDVRServiceContext(), new Time(
                        Double.POSITIVE_INFINITY), 1);
                ServiceExt svc = getCannedSIDB().jmfService1;
                ds.setService(svc);
                player.setSource(ds);
                ExtendedNetworkInterface ni = (ExtendedNetworkInterface) JMFTests.nim.getNetworkInterfaces()[0];
                sce = new CannedDVRSCE(ni);
                ServiceDetailsExt sdx = Util.getServiceDetails(svc);
                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

                player.setInitialSelection(sdx, null);
            }
            catch (Exception ex)
            {
                error = ex;
            }
            done = true;
            // player.notifyAll();
            // }
            // }
            // };
            // new Thread(r).start();
            //
            // synchronized(player)
            // {
            // if(!done)
            // player.wait();
            // }
            // if(error != null)
            // throw error;

            return player;

        }

    }

    private static class CannedDVRSCE implements DVRServiceContextExt
    {

        private ExtendedNetworkInterface ni;

        public void setDestroyWhenIdle(boolean destroyWhenIdle)
        {
        }

        public void stopAbstractService()
        {
        }; // adc

        public void setAvailableServiceContextDelegates(List serviceContextDelegates)
        {
            // TODO: implement
        }

        public void forceEASTune(Service service)
        {
            // TODO: implement
        }

        public void unforceEASTune()
        {
            
        }

        public CannedDVRSCE(ExtendedNetworkInterface ni)
        {
            this.ni = ni;
        }

        public TimeShiftWindowClient getTimeShiftWindowClient()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void requestBuffering()
        {
            // no-op
        }

        public AppDomain getAppDomain()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public CallerContext getCallerContext()
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            return ccm.getCurrentContext();
        }

        public CallerContext getCreatingContext()
        {
            return getCallerContext();
        }

        public NetworkInterface getNetworkInterface()
        {
            return ni;
        }

        public void setDefaultVideoTransformation(VideoTransformation vt)
        {
            // TODO
        }

        public boolean isAppsEnabled()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isDestroyed()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isPersistentVideoMode()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isPresenting()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public void setApplicationsEnabled(boolean appsEnabled)
        {
            // TODO Auto-generated method stub

        }

        public void setInitialBackground(VideoTransformation trans)
        {
            // TODO Auto-generated method stub

        }

        public void setDefautVideoTransformation(org.dvb.media.VideoTransformation vt)
        {
            // TODO Auto-generated method stub

        }

        public void setInitialComponent(Container parent, Rectangle rect)
        {
            // TODO Auto-generated method stub

        }

        public void setPersistentVideoMode(boolean enable)
        {
            // TODO Auto-generated method stub

        }

        public void swapSettings(ServiceContext sc, boolean audioUse, boolean swapAppSettings)
                throws IllegalArgumentException
        {
            // TODO Auto-generated method stub

        }

        public void addListener(ServiceContextListener listener)
        {
            // TODO Auto-generated method stub

        }

        public void destroy() throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        public Service getService()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceContentHandler[] getServiceContentHandlers() throws SecurityException
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void removeListener(ServiceContextListener listener)
        {
            // TODO Auto-generated method stub

        }

        public void select(Service selection) throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        public void select(Locator[] components) throws InvalidLocatorException, InvalidServiceComponentException,
                SecurityException
        {
            // TODO Auto-generated method stub

        }

        public void stop() throws SecurityException
        {
            // TODO Auto-generated method stub

        }

        public boolean isBuffering()
        {
            // TODO Auto-generated method stub
            return true;
        }

        public DVRPresentation getDVRPresentation(Service service)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceMediaHandler addServiceContextCallback(ServiceContextCallback callback, int priority)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void removeServiceContextCallback(ServiceContextCallback callback)
        {
            // TODO Auto-generated method stub
        }
    }

    public static class CannedTimeShiftWindowClient implements TimeShiftWindowClient
    {

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBForSystemTime
         * (long, int)
         */
        public TimeShiftBuffer getTSBForSystemTime(long systemTime, int proximity)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public boolean sameAs(org.cablelabs.impl.manager.TimeShiftWindowClient tswc)
        {
            return false;
        }

        public TimeBase getTimeBase()
        {
            return null;
        }

        public long getTimeBaseStartTime()
        {
            return 0;
        }

        public void store(Object src)
        {
        }

        public boolean cacheLightweightTriggerEvent(Object src, LightweightTriggerEvent lwte)
        {
            return true;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBsForTimeSpan
         * (long, long)
         */
        public Enumeration getTSBsForSystemTimeSpan(long startTime, long duration)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void changeListener(TimeShiftWindowChangedListener tswcl, int tswclPriority)
        {
        }

        public void attachFor(int uses) throws IllegalStateException, IllegalArgumentException
        {
        }

        public void detachFor(int uses) throws IllegalArgumentException
        {
        }

        public void detachForAll()
        {
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#attachToTSB(org.
         * cablelabs.impl.manager.TimeShiftBuffer)
         */
        public TimeShiftBuffer attachToTSB(TimeShiftBuffer tsb) throws IllegalArgumentException
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#detachAllTSBs()
         */
        public void detachAllTSBs()
        {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#detachFromTSB(org
         * .cablelabs.impl.manager.TimeShiftBuffer)
         */
        public void detachFromTSB(TimeShiftBuffer tsb) throws IllegalArgumentException
        {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#elements()
         */
        public Enumeration elements()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getBufferingTSB()
         */
        public TimeShiftBuffer getBufferingTSB() throws IllegalStateException
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getFirstTSB()
         */
        public TimeShiftBuffer getFirstTSB()
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getLastTSB()
         */
        public TimeShiftBuffer getLastTSB()
        {
            // TODO Auto-generated method stub
            return null;
        }

        public TimeShiftBuffer getTSBForTimeBaseTime(long timeBaseTime, int proximity)
        {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.impl.manager.TimeShiftWindowClient#getSize()
         */
        public long getSize()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBFollowing(
         * org.cablelabs.impl.manager.TimeShiftBuffer)
         */
        public TimeShiftBuffer getTSBFollowing(TimeShiftBuffer tsb)
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBForMediaTime
         * (long, int)
         */
        public TimeShiftBuffer getTSBForMediaTime(long time, int proximity)
        {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#getTSBPreceeding
         * (org.cablelabs.impl.manager.TimeShiftBuffer)
         */
        public TimeShiftBuffer getTSBPreceeding(TimeShiftBuffer tsb)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public ServiceComponentExt[] getBufferingTSBComponents() throws IllegalStateException
        {
            return null;
        }

        public long getBufferingTSBMediaEndTime() throws IllegalStateException
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public long getBufferingTSBMediaStartTime() throws IllegalStateException
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public long getBufferingTSBMediaTimeOffset() throws IllegalStateException
        {
            return 0;
        }

        public long getBufferingTSBDuration() throws IllegalStateException
        {
            return 60000000000L;
        }

        public int getBufferingTSBHandle() throws IllegalStateException
        {
            return 0;
        }

        public long getBufferingTSBSize() throws IllegalStateException
        {
            return 0;
        }

        public long getDuration() throws IllegalStateException
        {
            return 60L;
        }

        public ExtendedNetworkInterface getNetworkInterface()
        {
            return null;
        }

        public int getState()
        {
            return 0;
        }

        public void release()
        {
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.impl.manager.TimeShiftWindowClient#addClient(org.cablelabs
         * .impl.manager.TimeShiftWindowChangedListener)
         */
        public TimeShiftWindowClient addClient(int reserveFor, TimeShiftWindowChangedListener tswcl, int tswclPriority)
        {
            // TODO Auto-generated method stub
            return null;
        }

        public void setBufferingPIDs(ServiceComponentExt[] compList) throws IllegalArgumentException
        {
        }

        public void setDesiredDuration(long duration)
        {
        }

        public void setMaximumDuration(long duration)
        {
        }

        public void setMinimumDuration(long duration) throws IllegalArgumentException
        {
        }

        public void syncBufferTimeToClock(Clock clock)
        {
        }

        public void addPlayer(AbstractDVRServicePlayer player)
        {
        }

        public void removePlayer(AbstractDVRServicePlayer player)
        {
        }

        public void addObserver(PlaybackClientObserver listener)
        {
        }

        public Vector getPlayers()
        {
            return new Vector();
        }

        public void removeObserver(PlaybackClientObserver listener)
        {
        }

        public LightweightTriggerEvent getEventByName(String name)
        {
            return null;
        }

        public String[] getEventNames()
        {
            return null;
        }

        public void store()
        {
        }

        public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte)
        {
            return false;
        }

        public void registerChangeNotification(LightweightTriggerEventChangeListener listener)
        {
        }

        public void unregisterChangeNotification(LightweightTriggerEventChangeListener listener)
        {
        }

        public int getUses()
        {
            return 0;
        }

        public ServiceExt getService()
        {
            return null;
        }

        public ResourceUsage getResourceUsage()
        {
            return null;
        }

        public boolean isAuthorized()
        {
            // TODO Auto-generated method stub
            return false;
        }

        public long getSystemStartTimeMs()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        public short getLTSID()
        {
            // TODO Auto-generated method stub
            return 0;
        }
    }
}
