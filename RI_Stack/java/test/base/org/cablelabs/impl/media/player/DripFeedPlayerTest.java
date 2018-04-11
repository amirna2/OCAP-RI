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

import javax.media.Player;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.media.DripFeedDataSource;
import org.havi.ui.HScreen;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.CannedHScreen;
import org.cablelabs.impl.media.JMFFactory;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;

/**
 * DripFeedPlayerTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class DripFeedPlayerTest extends TestCase
{

    private CannedMediaAPI cannedMediaAPI;

    private DripFeedPlayer player;

    private CannedControllerListener listener;

    private PlayerHelper helper;

    private DripFeedPlayerFactory playerFactory;

    /**
	 * 
	 */
    public DripFeedPlayerTest()
    {
        super("DripFeedPlayerTest");
    }

    /**
     * @param arg0
     */
    public DripFeedPlayerTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(DripFeedPlayerTest.class);
        ImplFactory factory = new DripFeedPlayerFactory();

        InterfaceTestSuite[] testSuites = new InterfaceTestSuite[] { PlayerInterfaceTest.isuite(factory),
                AWTVideoSizeControlTest.isuite(factory), BackgroundVideoPresentationControlTest.isuite(factory), };

        for (int i = 0; i < testSuites.length; i++)
        {
            suite.addTest(testSuites[i]);
        }

        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        playerFactory = new DripFeedPlayerFactory();
        playerFactory.setUp();

        player = (DripFeedPlayer) playerFactory.createImplObject();
        listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);
        cannedMediaAPI = (CannedMediaAPI) ManagerManager.getInstance(MediaAPIManager.class);
    }

    public void tearDown() throws Exception
    {
        player.close();
        helper = null;
        player.removeControllerListener(listener);
        listener = null;
        player = null;
        playerFactory.tearDown();
        playerFactory = null;
        super.tearDown();
    }

    // Test Section

    public void testFeedandGetFrame() throws Exception
    {
        byte[] feedData = new byte[10];
        player.feed(feedData);
        assertNull("Returned data should be null", player.getFrame());

        //
        // the feedData should not be pushed through because the player isn't
        // started
        //
        helper.prefetchPlayer();
        player.feed(feedData);
        assertEquals("Data size is incorrect", feedData.length, player.getFrame().length);
        assertTrue("Drip Feed frame should not have been decoded yet",
                cannedMediaAPI.cannedGetLastDripFeedFrame() == null);

        //
        // once started, the feed data should be pushed through
        //
        helper.startPlayer();
        assertTrue("Did not see expected drip feed frame decoded",
                cannedMediaAPI.cannedGetLastDripFeedFrame().length == feedData.length);

        //
        // feed new data
        //
        feedData = new byte[20];
        player.feed(feedData);
        assertTrue("Did not see expected drip feed frame decoded",
                cannedMediaAPI.cannedGetLastDripFeedFrame().length == feedData.length);
    }

    // Support Section

    private static class DripFeedPlayerFactory extends JMFFactory
    {

        public Object createImplObject() throws Exception
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            DripFeedPlayer player = new CannedDripFeedPlayer(ccm.getCurrentContext());
            DripFeedDataSource ds = new DripFeedDataSource();
            player.setSource(ds);
            CannedControllerListener listener = new CannedControllerListener(0);
            player.addControllerListener(listener);

            return player;
        }

        public void movePlayerToPresenting(Player player)
        {
            byte[] feedData = new byte[10];
            ((DripFeedPlayer) player).feed(feedData);
        }
    }

    private static class CannedDripFeedPlayer extends DripFeedPlayer
    {
        CannedDripFeedPlayer(CallerContext cc)
        {
            super(cc);
        }

        protected HScreen getDefaultScreen()
        {
            return CannedHScreen.getInstance();
        }
    }

}
