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

package org.cablelabs.impl.media.access;

import junit.framework.TestCase; //import org.ocap.media.ParentalControlRatings;
import org.cablelabs.impl.manager.VChipManager;

/**
 * Unit test for {@link VChipManagerImpl}.
 * 
 * @author Scott Boss
 */
public class VChipManagerImplTest extends TestCase // implements
                                                   // ParentalControlRatings
{
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(VChipManagerImplTest.class);
    }

    /**
     * @param arg0
     */
    public VChipManagerImplTest(String arg0)
    {
        super(arg0);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        if (vChipMgr != null) vChipMgr.destroy();
        super.tearDown();
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#destroy()}.
     */
    public void testDestroy()
    {
        VChipManager vChipMgr1, vChipMgr2;
        vChipMgr1 = getVChipMgr();
        // destroy should null out the singleton instance so that next call
        // returns a new instance
        vChipMgr.destroy();
        vChipMgr2 = getVChipMgr();
        assertNotSame(vChipMgr1, vChipMgr2);
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#getInstance()}.
     */
    public void testGetInstance()
    {
        VChipManager vChipMgr1 = VChipManagerImpl.getInstance();
        assertNotNull(vChipMgr1);
        VChipManager vChipMgr2 = VChipManagerImpl.getInstance();
        assertNotNull(vChipMgr2);
        assertEquals(vChipMgr1, vChipMgr2);
    }

    private VChipManagerImpl vChipMgr = null;

    private VChipManagerImpl getVChipMgr()
    {
        return vChipMgr = (VChipManagerImpl) VChipManagerImpl.getInstance();
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.ParentalControlManagerImpl#getVChipBlockingSupport()}
     * .
     */
    public void testGetVChipBlockingSupport()
    {
        // VChip blocking reason is always BlockingReason.RATING_XDS
        BlockingReason[] VChipBlocking = getVChipMgr().getVChipBlockingSupport();
        assertEquals(1, VChipBlocking.length);
        assertEquals(BlockingReason.RATING_XDS, VChipBlocking[0]);
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#setSignaledBlocking(int)}
     * . Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#getSignaledBlocking()}
     * .
     */
    public void testSetGetSignaledBlocking()
    {
        getVChipMgr();
        assertEquals(TV_NONE | MPAA_NA, vChipMgr.getSignaledBlocking());
        vChipMgr.setSignaledBlocking(TV_14);
        assertEquals(TV_14 | MPAA_NA, vChipMgr.getSignaledBlocking());
        vChipMgr.setSignaledBlocking(TV_Y);
        assertEquals(TV_Y | MPAA_NA, vChipMgr.getSignaledBlocking());
        vChipMgr.setSignaledBlocking(MPAA_G);
        assertEquals(TV_Y | MPAA_G, vChipMgr.getSignaledBlocking());
        vChipMgr.setSignaledBlocking(MPAA_NC_17);
        assertEquals(TV_Y | MPAA_NC_17, vChipMgr.getSignaledBlocking());
        vChipMgr.setSignaledBlocking(TV_Y7 | MPAA_NA);
        assertEquals(TV_Y7 | MPAA_NA, vChipMgr.getSignaledBlocking());
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#setNotRatedSignaledBlocking(int, boolean)}
     * . IllegalArgumentException
     */
    public void testSetNotRatedSignaledBlocking_IllegalArgumentException()
    {
        // TODO: Add illegal argument tests if setNotRatedSignalledBlocking is
        // modified to check arguments
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#setNotRatedSignaledBlocking(int, boolean)}
     * .
     */
    public void testSetNotRatedSignaledBlocking()
    {
        // TODO: Add tests when testSetNotRatedSignaledBlocking is implemented
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#setSignaledBlockingOverride(boolean)}
     * .
     */
    public void testSetSignaledBlockingOverride()
    {
        getVChipMgr();
        /**
         * set override to true / signaledBlocking value is not changed but
         * VChip is programmed to not block Currently no way to validate VChip
         * is programmed to not block
         */
        vChipMgr.setSignaledBlockingOverride(true);
        assertEquals(TV_NONE | MPAA_NA, vChipMgr.getSignaledBlocking());
        vChipMgr.setSignaledBlockingOverride(false);

        /**
         * test that the signalled blocking value set before override is turned
         * on is restored after override is turned off
         */
        vChipMgr.setSignaledBlocking(TV_G);
        vChipMgr.setSignaledBlockingOverride(true);
        vChipMgr.setSignaledBlocking(TV_MA); // set signaledBlocking but should
                                             // not take effect due to override
        assertEquals(TV_MA | MPAA_NA, vChipMgr.getSignaledBlocking()); // Currently
                                                                       // no
                                                                       // wasy
                                                                       // to
                                                                       // validate
                                                                       // that
                                                                       // signaled
                                                                       // blocking
                                                                       // takes
                                                                       // effect
                                                                       // or not
        vChipMgr.setSignaledBlockingOverride(false);
        assertEquals(TV_G | MPAA_NA, vChipMgr.getSignaledBlocking()); // verify
                                                                      // original
                                                                      // signaledBlocking
                                                                      // is set
    }

    /**
     * Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#addRatingCallback(VChipRatingCallback, int)}
     * . Test method for
     * {@link org.cablelabs.impl.media.access.VChipManagerImpl#removeRatingCallback(VChipRatingCallback)}
     * .
     */
    public void testAddRemoveRatingCallback()
    {
        getVChipMgr();
        VChipRatingCallback ratingCB1 = new VChipRatingCallback()
        {
            public void notifyRatingChanged(int rating)
            {
            }
        };
        vChipMgr.addRatingCallback(ratingCB1, 0);
        vChipMgr.removeRatingCallback(ratingCB1);

        VChipRatingCallback ratingCB2 = new VChipRatingCallback()
        {
            public void notifyRatingChanged(int rating)
            {
            }
        };
        vChipMgr.addRatingCallback(ratingCB1, 0);
        vChipMgr.addRatingCallback(ratingCB2, 1);
        vChipMgr.removeRatingCallback(ratingCB1);
        vChipMgr.removeRatingCallback(ratingCB2);

        // TODO: add test code to verify callbacks are invoked in priority order
        // requires V-chip support to invoke callbacks and content with rating
        // changes
    }
}
