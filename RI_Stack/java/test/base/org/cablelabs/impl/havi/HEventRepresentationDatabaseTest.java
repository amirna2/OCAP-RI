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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.ocap.ui.event.OCRcEvent;

import org.havi.ui.event.HRcEvent;
import org.havi.ui.event.HEventRepresentation;
import org.havi.ui.event.HRcCapabilities;

import java.awt.Color;
import java.awt.event.KeyEvent;

/**
 * Used to test HEventRepresentationDatabase
 * 
 * @author Greg Rutz
 */
public class HEventRepresentationDatabaseTest extends TestCase
{
    public void testFoo()
    {
        // empty test so this test can still be included in the base
        // level suites
    }

    /**
     * The following tests are designed to be used with the
     * HEventRepresentations.txt test properties file
     * 
     * @todo disabled per 4595
     */
    public void xxxtestHER() throws Exception
    {
        HEventRepresentation her;

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_POWER);
        assertNotNull("VK_POWER HEventRepresentation should not be null", her);
        assertTrue("VK_POWER should be supported", her.isSupported());
        assertEquals("VK_POWER string is not correct", "Power", her.getString());
        assertEquals("VK_POWER color is not correct", Color.red, her.getColor());
        assertNull("VK_POWER symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_CHANNEL_DOWN);
        assertNotNull("VK_CHANNEL_DOWN HEventRepresentation should not be null", her);
        assertTrue("VK_CHANNEL_DOWN should be supported", her.isSupported());
        assertEquals("VK_CHANNEL_DOWN string is not correct", "Channel-", her.getString());
        assertEquals("VK_CHANNEL_DOWN color is not correct", Color.black, her.getColor());
        assertNull("VK_CHANNEL_DOWN symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_CHANNEL_UP);
        assertNotNull("VK_CHANNEL_UP HEventRepresentation should not be null", her);
        assertTrue("VK_CHANNEL_UP should be supported", her.isSupported());
        assertEquals("VK_CHANNEL_UP string is not correct", "Channel+", her.getString());
        assertEquals("VK_CHANNEL_UP color is not correct", Color.black, her.getColor());
        assertNull("VK_CHANNEL_UP symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_0);
        assertNotNull("VK_0 HEventRepresentation should not be null", her);
        assertTrue("VK_0 should be supported", her.isSupported());
        assertEquals("VK_0 string is not correct", "0", her.getString());
        assertEquals("VK_0 color is not correct", Color.white, her.getColor());
        assertNull("VK_0 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_1);
        assertNotNull("VK_1 HEventRepresentation should not be null", her);
        assertTrue("VK_1 should be supported", her.isSupported());
        assertEquals("VK_1 string is not correct", "1", her.getString());
        assertEquals("VK_1 color is not correct", Color.white, her.getColor());
        assertNull("VK_1 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_2);
        assertNotNull("VK_2 HEventRepresentation should not be null", her);
        assertTrue("VK_2 should be supported", her.isSupported());
        assertEquals("VK_2 string is not correct", "2", her.getString());
        assertEquals("VK_2 color is not correct", Color.white, her.getColor());
        assertNull("VK_2 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_3);
        assertNotNull("VK_3 HEventRepresentation should not be null", her);
        assertTrue("VK_3 should be supported", her.isSupported());
        assertEquals("VK_3 string is not correct", "3", her.getString());
        assertEquals("VK_3 color is not correct", Color.white, her.getColor());
        assertNull("VK_3 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_4);
        assertNotNull("VK_4 HEventRepresentation should not be null", her);
        assertTrue("VK_4 should be supported", her.isSupported());
        assertEquals("VK_4 string is not correct", "4", her.getString());
        assertEquals("VK_4 color is not correct", Color.white, her.getColor());
        assertNull("VK_4 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_5);
        assertNotNull("VK_5 HEventRepresentation should not be null", her);
        assertTrue("VK_5 should be supported", her.isSupported());
        assertEquals("VK_5 string is not correct", "5", her.getString());
        assertEquals("VK_5 color is not correct", Color.white, her.getColor());
        assertNull("VK_5 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_6);
        assertNotNull("VK_6 HEventRepresentation should not be null", her);
        assertTrue("VK_6 should be supported", her.isSupported());
        assertEquals("VK_6 string is not correct", "6", her.getString());
        assertEquals("VK_6 color is not correct", Color.white, her.getColor());
        assertNull("VK_6 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_7);
        assertNotNull("VK_7 HEventRepresentation should not be null", her);
        assertTrue("VK_7 should be supported", her.isSupported());
        assertEquals("VK_7 string is not correct", "7", her.getString());
        assertEquals("VK_7 color is not correct", Color.white, her.getColor());
        assertNull("VK_7 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_8);
        assertNotNull("VK_8 HEventRepresentation should not be null", her);
        assertTrue("VK_8 should be supported", her.isSupported());
        assertEquals("VK_8 string is not correct", "8", her.getString());
        assertEquals("VK_8 color is not correct", Color.white, her.getColor());
        assertNull("VK_8 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_9);
        assertNotNull("VK_9 HEventRepresentation should not be null", her);
        assertTrue("VK_9 should be supported", her.isSupported());
        assertEquals("VK_9 string is not correct", "9", her.getString());
        assertEquals("VK_9 color is not correct", Color.white, her.getColor());
        assertNull("VK_9 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_DOWN);
        assertNotNull("VK_DOWN HEventRepresentation should not be null", her);
        assertTrue("VK_DOWN should be supported", her.isSupported());
        assertEquals("VK_DOWN string is not correct", "Down", her.getString());
        assertEquals("VK_DOWN color is not correct", Color.blue, her.getColor());
        assertNull("VK_DOWN symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_UP);
        assertNotNull("VK_UP HEventRepresentation should not be null", her);
        assertTrue("VK_UP should be supported", her.isSupported());
        assertEquals("VK_UP string is not correct", "Up", her.getString());
        assertEquals("VK_UP color is not correct", Color.blue, her.getColor());
        assertNull("VK_UP symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_LEFT);
        assertNotNull("VK_LEFT HEventRepresentation should not be null", her);
        assertTrue("VK_LEFT should be supported", her.isSupported());
        assertEquals("VK_LEFT string is not correct", "Left", her.getString());
        assertEquals("VK_LEFT color is not correct", Color.blue, her.getColor());
        assertNull("VK_LEFT symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_RIGHT);
        assertNotNull("VK_RIGHT HEventRepresentation should not be null", her);
        assertTrue("VK_RIGHT should be supported", her.isSupported());
        assertEquals("VK_RIGHT string is not correct", "Right", her.getString());
        assertEquals("VK_RIGHT color is not correct", Color.blue, her.getColor());
        assertNull("VK_RIGHT symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_VOLUME_DOWN);
        assertNotNull("VK_VOLUME_DOWN HEventRepresentation should not be null", her);
        assertTrue("VK_VOLUME_DOWN should be supported", her.isSupported());
        assertEquals("VK_VOLUME_DOWN string is not correct", "Vol-", her.getString());
        assertEquals("VK_VOLUME_DOWN color is not correct", Color.black, her.getColor());
        assertNotNull("VK_VOLUME_DOWN symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_VOLUME_UP);
        assertNotNull("VK_VOLUME_UP HEventRepresentation should not be null", her);
        assertTrue("VK_VOLUME_UP should be supported", her.isSupported());
        assertEquals("VK_VOLUME_UP string is not correct", "Vol+", her.getString());
        assertEquals("VK_VOLUME_UP color is not correct", Color.black, her.getColor());
        assertNotNull("VK_VOLUME_UP symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_MUTE);
        assertNotNull("VK_MUTE HEventRepresentation should not be null", her);
        assertTrue("VK_MUTE should be supported", her.isSupported());
        assertEquals("VK_MUTE string is not correct", "Mute", her.getString());
        assertEquals("VK_MUTE color is not correct", Color.black, her.getColor());
        assertNull("VK_MUTE symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_PAUSE);
        assertNotNull("VK_PAUSE HEventRepresentation should not be null", her);
        assertTrue("VK_PAUSE should be supported", her.isSupported());
        assertEquals("VK_PAUSE string is not correct", "Pause", her.getString());
        assertEquals("VK_PAUSE color is not correct", Color.green, her.getColor());
        assertNotNull("VK_PAUSE symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_PLAY);
        assertNotNull("VK_PLAY HEventRepresentation should not be null", her);
        assertTrue("VK_PLAY should be supported", her.isSupported());
        assertEquals("VK_PLAY string is not correct", "Play", her.getString());
        assertEquals("VK_PLAY color is not correct", Color.green, her.getColor());
        assertNotNull("VK_PLAY symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_STOP);
        assertNotNull("VK_STOP HEventRepresentation should not be null", her);
        assertTrue("VK_STOP should be supported", her.isSupported());
        assertEquals("VK_STOP string is not correct", "Stop", her.getString());
        assertEquals("VK_STOP color is not correct", Color.green, her.getColor());
        assertNull("VK_STOP symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_RECORD);
        assertNotNull("VK_RECORD HEventRepresentation should not be null", her);
        assertTrue("VK_RECORD should be supported", her.isSupported());
        assertEquals("VK_RECORD string is not correct", "Record", her.getString());
        assertEquals("VK_RECORD color is not correct", Color.green, her.getColor());
        assertNull("VK_RECORD symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_FAST_FWD);
        assertNotNull("VK_FAST_FWD HEventRepresentation should not be null", her);
        assertTrue("VK_FAST_FWD should be supported", her.isSupported());
        assertEquals("VK_FAST_FWD string is not correct", "FFWD", her.getString());
        assertEquals("VK_FAST_FWD color is not correct", Color.green, her.getColor());
        assertNotNull("VK_FAST_FWD symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_REWIND);
        assertNotNull("VK_REWIND HEventRepresentation should not be null", her);
        assertTrue("VK_REWIND should be supported", her.isSupported());
        assertEquals("VK_REWIND string is not correct", "REW", her.getString());
        assertEquals("VK_REWIND color is not correct", Color.green, her.getColor());
        assertNotNull("VK_REWIND symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_GUIDE);
        assertNotNull("VK_GUIDE HEventRepresentation should not be null", her);
        assertTrue("VK_GUIDE should be supported", her.isSupported());
        assertEquals("VK_GUIDE string is not correct", "Guide", her.getString());
        assertEquals("VK_GUIDE color is not correct", Color.white, her.getColor());
        assertNotNull("VK_GUIDE symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(OCRcEvent.VK_RF_BYPASS);
        assertNotNull("VK_RF_BYPASS HEventRepresentation should not be null", her);
        assertFalse("VK_RF_BYPASS should not be supported", her.isSupported());

        her = HRcCapabilities.getRepresentation(OCRcEvent.VK_MENU);
        assertNotNull("VK_MENU HEventRepresentation should not be null", her);
        assertTrue("VK_MENU should be supported", her.isSupported());
        assertEquals("VK_MENU string is not correct", "Menu", her.getString());
        assertEquals("VK_MENU color is not correct", Color.white, her.getColor());
        assertNull("VK_MENU symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_INFO);
        assertNotNull("VK_INFO HEventRepresentation should not be null", her);
        assertTrue("VK_INFO should be supported", her.isSupported());
        assertEquals("VK_INFO string is not correct", "Info", her.getString());
        assertEquals("VK_INFO color is not correct", Color.white, her.getColor());
        assertNotNull("VK_INFO symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(OCRcEvent.VK_EXIT);
        assertNotNull("VK_EXIT HEventRepresentation should not be null", her);
        assertTrue("VK_EXIT should be supported", her.isSupported());
        assertEquals("VK_EXIT string is not correct", "Exit", her.getString());
        assertEquals("VK_EXIT color is not correct", Color.black, her.getColor());
        assertNull("VK_EXIT symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(OCRcEvent.VK_LAST);
        assertNotNull("VK_LAST HEventRepresentation should not be null", her);
        assertTrue("VK_LAST should be supported", her.isSupported());
        assertEquals("VK_LAST string is not correct", "Last", her.getString());
        assertEquals("VK_LAST color is not correct", Color.white, her.getColor());
        assertNull("VK_LAST symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_COLORED_KEY_0);
        assertNotNull("VK_COLORED_KEY_0 HEventRepresentation should not be null", her);
        assertTrue("VK_COLORED_KEY_0 should be supported", her.isSupported());
        assertEquals("VK_COLORED_KEY_0 string is not correct", "Color0", her.getString());
        assertEquals("VK_COLORED_KEY_0 color is not correct", Color.blue, her.getColor());
        assertNull("VK_COLORED_KEY_0 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_COLORED_KEY_1);
        assertNotNull("VK_COLORED_KEY_1 HEventRepresentation should not be null", her);
        assertTrue("VK_COLORED_KEY_1 should be supported", her.isSupported());
        assertEquals("VK_COLORED_KEY_1 string is not correct", "Color1", her.getString());
        assertEquals("VK_COLORED_KEY_1 color is not correct", Color.red, her.getColor());
        assertNull("VK_COLORED_KEY_1 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_COLORED_KEY_2);
        assertNotNull("VK_COLORED_KEY_2 HEventRepresentation should not be null", her);
        assertTrue("VK_COLORED_KEY_2 should be supported", her.isSupported());
        assertEquals("VK_COLORED_KEY_2 string is not correct", "Color2", her.getString());
        assertEquals("VK_COLORED_KEY_2 color is not correct", Color.green, her.getColor());
        assertNull("VK_COLORED_KEY_2 symbol should be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(HRcEvent.VK_COLORED_KEY_3);
        assertNotNull("VK_COLORED_KEY_3 HEventRepresentation should not be null", her);
        assertFalse("VK_COLORED_KEY_3 should not be supported", her.isSupported());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_PAGE_UP);
        assertNotNull("VK_PAGE_UP HEventRepresentation should not be null", her);
        assertTrue("VK_PAGE_UP should be supported", her.isSupported());
        assertEquals("VK_PAGE_UP string is not correct", "Page Up", her.getString());
        assertEquals("VK_PAGE_UP color is not correct", Color.white, her.getColor());
        assertNotNull("VK_PAGE_UP symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(KeyEvent.VK_PAGE_DOWN);
        assertNotNull("VK_PAGE_DOWN HEventRepresentation should not be null", her);
        assertTrue("VK_PAGE_DOWN should be supported", her.isSupported());
        assertEquals("VK_PAGE_DOWN string is not correct", "Page Down", her.getString());
        assertEquals("VK_PAGE_DOWN color is not correct", Color.white, her.getColor());
        assertNotNull("VK_PAGE_DOWN symbol should not be null", her.getSymbol());

        her = HRcCapabilities.getRepresentation(OCRcEvent.VK_NEXT_FAVORITE_CHANNEL);
        assertNotNull("VK_NEXT_FAVORITE_CHANNEL HEventRepresentation should not be null", her);
        assertFalse("VK_NEXT_FAVORITE_CHANNEL should not be supported", her.isSupported());

        her = HRcCapabilities.getRepresentation(OCRcEvent.VK_ON_DEMAND);
        assertNotNull("VK_ON_DEMAND HEventRepresentation should not be null", her);
        assertFalse("VK_ON_DEMAND should not be supported", her.isSupported());
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
        TestSuite suite = new TestSuite(HEventRepresentationDatabaseTest.class);
        return suite;
    }

    public HEventRepresentationDatabaseTest(String name)
    {
        super(name);
    }
}
