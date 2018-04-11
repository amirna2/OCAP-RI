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

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.media.AudioLanguageControl;
import org.davic.media.LanguageControl;
import org.ocap.media.NormalMediaPresentationEvent;

/**
 * AudioLanguageControlTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class AudioLanguageControlTest extends AbstractLanguageControlTest
{
    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(AudioLanguageControlTest.class, factory);
    }

    public AudioLanguageControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public AudioLanguageControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    protected LanguageControl getControl()
    {
        return (AudioLanguageControl) player.getControl(AudioLanguageControl.class.getName());
    }

    public void testSelectDefaultLanguage() throws Exception
    {
        // mediaAccessHandler.setAcceptAllPIDS(true);
        // helper.prefetchPlayer();
        // helper.callSyncStartWithNoWait();
        // listener.waitForMediaPresentationEvent();
        // listener.reset();
        // mediaAccessHandler.setCheckMediaAccessAuthorizationCalled(false);
        //        
        // control.selectDefaultLanguage();
        // listener.waitForMediaPresentationEvent();
        // NormalMediaPresentationEvent event = (NormalMediaPresentationEvent)
        // listener.findEventOfClass(NormalMediaPresentationEvent.class);
        //        
        // assertTrue("Expected NormalMediaPresentationEvent", event != null);
        // FIXME - ecn 972
        // fail("ECN 972 rewrite");
        // assertTrue(mediaAccessHandler.isCheckMediaAccessAuthorizationCalled());
    }
}
