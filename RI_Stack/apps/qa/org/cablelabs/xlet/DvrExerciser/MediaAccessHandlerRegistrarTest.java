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

package org.cablelabs.xlet.DvrExerciser;

import java.util.Enumeration;
import javax.media.Control;
import javax.media.Player;
import javax.tv.locator.Locator;
import javax.tv.locator.LocatorFactory;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.PresentationChangedEvent;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.dvb.service.selection.DvbServiceContext;
import org.davic.mpeg.ElementaryStream;
import org.ocap.media.MediaAccessAuthorization;
import org.ocap.media.MediaAccessConditionControl;
import org.ocap.media.MediaAccessHandler;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;
import org.ocap.media.MediaAccessHandlerRegistrar;

public class MediaAccessHandlerRegistrarTest implements MediaAccessHandler, MediaAccessAuthorization,
        ServiceContextListener
{
    DvrExerciser m_dvrExerciser;

    public MediaAccessHandlerRegistrarTest(DvrExerciser dvrExerciser)
    {
        m_dvrExerciser = dvrExerciser;
        m_dvrExerciser.logIt("MediaAccessHandlerRegistrarTest constructor()");
    }

    // public void runTest()
    // {
    // m_dvrExerciser.logIt("begin test");
    //		
    // //-S1 Verify getInstance() does not throw an exception.
    // MediaAccessHandlerRegistrar registrar = null;
    // MediaAccessHandlerRegistrar registrar2 = null;
    // try {
    // registrar = MediaAccessHandlerRegistrar.getInstance();
    // registrar2 = MediaAccessHandlerRegistrar.getInstance();
    // }
    // catch (Exception e)
    // {
    // String string = "getInstance()" + e.toString();
    // m_dvrExerciser.logIt(string);
    // return;
    // }
    //
    // //-S1 Verify getInstance() does not return null.
    // if (registrar == null)
    // {
    // m_dvrExerciser.logIt("getInstance() returned null.");
    // return;
    // }
    //
    // //-S1 Verify getInstance() returns the sole instance.
    // if (!registrar.equals(registrar2))
    // {
    // m_dvrExerciser.logIt("getInstance() did not return sole instance.");
    // return;
    // }
    // m_dvrExerciser.logIt("done");
    // }

    /*
     * Test application #1 for org.ocap.media-MediaAccessHandlerRegistrar-60
     * 
     * Assertion When MediaPresentationEvaluationTriggers are OPTIONAL, then
     * setExternalTriggers() sets those triggers.
     * 
     * Author enableTV/botte
     */
    // public class MediaAccessHandlerRegistrar60App1 implements
    // MediaAccessHandler, MediaAccessAuthorization, ServiceContextListener {
    // -S In an app with MonAppPermission("mediaAccess")

    // private TestContext m_tx = null;
    // private MediaPresentationEvent m_event = null;
    private boolean m_t1Fired = false;

    private MediaPresentationEvaluationTrigger m_t1 = null;

    private MediaPresentationEvaluationTrigger m_t2 = null;

    private Control m_control = null;

    public void runTest()
    {
        m_dvrExerciser.logIt("begin test");

        Thread.currentThread().setName("Primary thread");
        // tx.enterMethod();
        // m_tx = tx;

        // -S1 Implement a MediaAccessHandler(Player P,,,,) that retrieves the
        // -S1 MediaAccessConditionControl MACC from P.

        // -S1 Create two OPTIONAL MediaPresentationEvaluationTriggers, t1, t2.
        m_t1 = MediaPresentationEvaluationTrigger.PROGRAM_EVENT_RATING_CHANGED;
        m_t2 = MediaPresentationEvaluationTrigger.POWER_STATE_CHANGED;
        MediaPresentationEvaluationTrigger[] triggers = { m_t1, m_t2 };

        // -S1 Verify calling setExternalTriggers() with t1, t2 does not throw
        // exception.
        MediaAccessHandlerRegistrar registrar = MediaAccessHandlerRegistrar.getInstance();
        try
        {
            registrar.setExternalTriggers(triggers);
        }
        catch (Exception e)
        {
            m_dvrExerciser.logIt("setExternalTriggers " + e);
            return;
        }

        // -S1 Register that MediaAccessHandler.
        registrar.registerMediaAccessHandler(this);

        // -S1 Select broadcast service.
        DvbServiceContext context = null;
        try
        {
            context = (DvbServiceContext) ServiceContextFactory.getInstance().createServiceContext();
            context.addListener(this);

        }
        catch (Exception e)
        {
            m_dvrExerciser.logIt("Exception thown while creating ServiceContext." + e);
            return;
        }
        // String ocapURL = ATEValues.getOCAP_URL_DIGITAL(tx);
        String ocapURL = "ocap://0x44C";
        Locator locator = null;
        Service service = null;
        synchronized (this)
        {
            try
            {
                locator = LocatorFactory.getInstance().createLocator(ocapURL);
                service = SIManager.createInstance().getService(locator);
                context.select(service);
                m_dvrExerciser.logIt("Waiting for service selection");
                wait(30000);
            }
            catch (Exception e)
            {
                m_dvrExerciser.logIt("Exception thown while selecting service." + e);
                return;
            }
        }
        m_dvrExerciser.logIt("Selected service " + service.getName()); // 1

        // try
        // {
        // Thread.sleep(1000);
        // }
        // catch (InterruptedException e)
        // {
        // e.printStackTrace();
        // }

        // -S1 Cause t1 to fire by calling MACC.conditionHasChanged(t1).
        MediaAccessConditionControl control = (MediaAccessConditionControl) m_control;
        synchronized (this)
        {
            control.conditionHasChanged(m_t1);
            try
            {
                m_dvrExerciser.logIt("Waiting for media access authorization");
                wait(30000);
            }
            catch (Exception e)
            {
                m_dvrExerciser.logIt("wait()" + e);
                return;
            }
        }

        // -S1 Verify t1 was set by confirming
        // checkMediaAccessAuthorization(t1,,,,) was called.
        if (!m_t1Fired)
        {
            m_dvrExerciser.logIt("setExternalTriggers() did not set triggers.");
            return;
        }
        // tx.pathCheck(1);

        // context.stop();
        m_dvrExerciser.logIt("done");
    }

    // MediaAccessHandler
    public synchronized MediaAccessAuthorization checkMediaAccessAuthorization(Player p, OcapLocator sourceURL,
            boolean isSourceDigital, ElementaryStream[] esList, MediaPresentationEvaluationTrigger trigger)
    {
        m_dvrExerciser.logIt("MediaAccessHandler.checkMediaAccessAuth(,sourceURL,,,) is " + sourceURL);
        // m_dvrExerciser.logIt("Trigger = " + trigger);
        m_control = p.getControl("org.ocap.media.MediaAccessConditionControl");

        if (trigger.equals(m_t1))
        {
            m_dvrExerciser.logIt("Firing trigger for: " + trigger);
            m_t1Fired = true;
            notifyAll();
        }
        return this;
    }

    // MediaAccessAuthorization
    public int getDenialReasons(ElementaryStream es)
    {
        return 0;
    }

    public Enumeration getDeniedElementaryStreams()
    {
        return null;
    }

    public boolean isFullAuthorization()
    {
        return true;
    }

    // ServiceContextListener
    public synchronized void receiveServiceContextEvent(ServiceContextEvent e)
    {
        m_dvrExerciser.logIt("Called from thread: " + Thread.currentThread().getName());
        m_dvrExerciser.logIt("ServiceContextEvent: " + e);
        if (e instanceof PresentationChangedEvent)
        {
            notifyAll();
        }
    }
}

// }
