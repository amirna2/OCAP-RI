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

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;

import org.davic.media.LanguageControl;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaAccessHandler;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.net.OcapLocator;
import org.davic.mpeg.ElementaryStream;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.media.JMFBaseInterfaceTest;

class CannedMediaAccessHandler implements MediaAccessHandler
{

    public MediaAccessAuthorization checkMediaAccessAuthorization(Player p, OcapLocator sourceURL,
            boolean isSourceDigital, ElementaryStream[] esList, MediaPresentationEvaluationTrigger evaluationTrigger)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void notifySignaledBlocking(OcapLocator[] locators)
    {
        // TODO Auto-generated method stub

    }

    boolean checkAuthorizationCalled = false;

    void setCheckMediaAccessAuthorizationCalled(boolean called)
    {
        checkAuthorizationCalled = called;
    }

    boolean isCheckMediaAccessAuthorizationCalled()
    {
        return checkAuthorizationCalled;
    }

    boolean acceptAllPIDs = false;

    void setAcceptAllPIDS(boolean accept)
    {
        acceptAllPIDs = accept;
    }
}

public abstract class AbstractLanguageControlTest extends JMFBaseInterfaceTest
{

    protected Player player;

    protected CannedSIDatabase sidb;

    protected LanguageControl control;

    protected CannedControllerListener listener;

    protected PlayerHelper helper;

    protected CannedMediaAccessHandler mediaAccessHandler = new CannedMediaAccessHandler();

    public AbstractLanguageControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    // Test setup

    public void setUp() throws Exception
    {
        super.setUp();

        player = (Player) createImplObject();
        sidb = playerFactory.getCannedSIDB();
        listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);
        control = getControl();

        mediaAccessHandler = new CannedMediaAccessHandler();
        MediaAccessHandlerRegistrar.getInstance().registerMediaAccessHandler(mediaAccessHandler);

    }

    protected abstract LanguageControl getControl();

    public void tearDown() throws Exception
    {
        sidb = null;
        player.close();
        player.deallocate();

        control = null;
        player = null;
        mediaAccessHandler = null;
        helper = null;
        listener = null;

        MediaAccessHandlerRegistrar.getInstance().registerMediaAccessHandler(null);
        super.tearDown();

    }

    protected boolean startPlayerAndWaitForPresentation()
    {
        helper.prefetchPlayer();
        helper.callSyncStartWithNoWait();
        return listener.waitForMediaPresentationEvent();
    }

    // Test section

    public void testControlNotNull()
    {
        assertTrue(control != null);
    }

    public void testListAvailableLanguagesNoSelectedService()
    {
        String[] langs = control.listAvailableLanguages();
        assertTrue(langs != null);
        assertTrue(langs.length == 0);
    }

    public void testSelectLanguageNoSelectedService()
    {
        String lang = sidb.jmfServiceComponent1A1.getAssociatedLanguage();
        try
        {
            control.selectLanguage(lang);
            fail("An exception should have occurred");
        }
        catch (Exception exc)
        {
            // expected outcome
        }
    }

    public void testGetCurrentLanguageNoSelectedService()
    {
        String lang = control.getCurrentLanguage();
        assertTrue(lang != null);
        assertTrue(lang.length() == 0);
    }

    public void testSelectDefaultLanguageNoSelectedService() throws Exception
    {
        String lang = control.selectDefaultLanguage();
        assertTrue(lang != null);
        assertTrue(lang.length() == 0);
    }

    public void testListAvailableLanguages() throws Exception
    {
        assertTrue(startPlayerAndWaitForPresentation());
        String[] langs = control.listAvailableLanguages();
        assertTrue(langs != null);
        //
        // we are assuming here that the service that is currently
        // selected has more than one language available, this
        // is to test that the available languages are built from
        // all components in the service and not just the selected
        // components
        //
        assertTrue(langs.length > 1);

    }

    public void testGetCurrentLanguage()
    {
        assertTrue(startPlayerAndWaitForPresentation());
        String currentLanguage = control.getCurrentLanguage();
        assertTrue(currentLanguage != null);
        assertTrue(currentLanguage.length() == 3);
    }

    public void testGetCurrentLanguageNotChangedUntilDecoded() throws Exception
    {
        mediaAccessHandler.setAcceptAllPIDS(true);
        assertTrue(startPlayerAndWaitForPresentation());
        String currentLanguage = control.getCurrentLanguage();
        String nextLanguage = null;

        if (currentLanguage.equals(sidb.jmfServiceComponent1A1.getAssociatedLanguage()))
        {
            nextLanguage = sidb.jmfServiceComponent1A2.getAssociatedLanguage();
        }
        else
        {
            nextLanguage = sidb.jmfServiceComponent1A1.getAssociatedLanguage();
        }

        cma.cannedSetStallDecodeFirstFrameEvent(true);
        listener.reset();
        control.selectLanguage(nextLanguage);

        // this should not deliver an event, it should time out
        listener.waitForEvents(1);
        assertTrue("Received an event when we should be waiting " + "for decode first frame - " + listener.getEvent(0),
                listener.events.size() == 0);
        assertTrue("Language changed before the first frame was decoded",
                currentLanguage.equals(control.getCurrentLanguage()));

        synchronized (cma)
        {
            cma.notify();
        }
        listener.waitForMediaPresentationEvent();
        assertTrue("Language did not change after the first frame was decoded",
                nextLanguage.equals(control.getCurrentLanguage()));

    }

    public void testSelectLanguageBogusLanguage()
    {
        assertTrue(startPlayerAndWaitForPresentation());
        try
        {
            control.selectLanguage("xxx");
            fail("No exception selecting a bogus language");
        }
        catch (Exception exc)
        {
            // expected outcome
        }
    }

    public void testSelectLanguage1() throws Exception
    {
        // mediaAccessHandler.setAcceptAllPIDS(true);
        // assertTrue(startPlayerAndWaitForPresentation());
        // listener.reset();
        //        
        // String currentLanguage = control.getCurrentLanguage();
        // ServiceComponentExt serviceComponent = null;
        // if(currentLanguage.equals(sidb.jmfServiceComponent1A1.getAssociatedLanguage()))
        // {
        // serviceComponent = sidb.jmfServiceComponent1A2;
        // }
        // else
        // {
        // serviceComponent = sidb.jmfServiceComponent1A1;
        // }
        //
        // String nextLanguage = serviceComponent.getAssociatedLanguage();
        // mediaAccessHandler.setCheckMediaAccessAuthorizationCalled(false);
        // listener.reset();
        // control.selectLanguage(nextLanguage);
        // boolean recieved = listener.waitForMediaPresentationEvent();
        // assertTrue("Did not receive the media presentation event", recieved);
        // listener.reset();
        // assertTrue("Language was not changed",
        // control.getCurrentLanguage().equals(nextLanguage));
        // FIXME - ecn 972
        // fail("ECN 972 rewrite");
        // assertTrue(mediaAccessHandler.isCheckMediaAccessAuthorizationCalled());
    }
}
