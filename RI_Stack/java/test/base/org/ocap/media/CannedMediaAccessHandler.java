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
package org.ocap.media;

import java.util.Date;
import java.util.Enumeration;

import javax.media.Player;

import org.davic.mpeg.ElementaryStream;
import org.ocap.net.OcapLocator;

import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.cablelabs.impl.util.Arrays;

public class CannedMediaAccessHandler implements MediaAccessHandler
{
    private boolean checkMediaAccessAuthorizationCalled;

    private boolean notifySignaledBlockingCalled;

    private boolean denyAuthorization;

    private boolean allowAuthorization;

    private Object waitObject = new Object();

    private boolean cma_isSourceDigital;

    private OcapLocator[] cma_locators;

    private OcapLocator[] nsb_locators;

    public void reset()
    {
        checkMediaAccessAuthorizationCalled = false;
    }

    public MediaAccessAuthorization checkMediaAccessAuthorization(Player p, OcapLocator sourceURL,
            boolean isSourceDigital, ElementaryStream[] esList, MediaPresentationEvaluationTrigger evaluationTrigger)
    {

        cma_isSourceDigital = isSourceDigital;
        // setCMALocators(locators);

        MediaAccessAuthorization results;

        if (allowAuthorization)
        {
            results = new CannedMediaAccessAuthorization();
        }
        else
        {
            results = new CannedMediaAccessAuthorization(true, AlternativeMediaPresentationReason.RATING_PROBLEM);
        }

        synchronized (waitObject)
        {
            checkMediaAccessAuthorizationCalled = true;
            waitObject.notifyAll();
        }

        return results;
    }

    public void notifySignaledBlocking(OcapLocator[] locators)
    {
        setNSBLocators(locators);
        synchronized (waitObject)
        {
            notifySignaledBlockingCalled = true;
            waitObject.notifyAll();
        }
    }

    public void waitForCheckMediaAccessAuthorizationCall(long timeout)
    {
        try
        {
            synchronized (waitObject)
            {
                if (!checkMediaAccessAuthorizationCalled)
                {
                    waitObject.wait(timeout);
                }
            }
        }
        catch (InterruptedException exc)
        {
            // return
        }
    }

    public void waitForNotifySignaledBlockingCall(long timeout)
    {
        try
        {
            synchronized (waitObject)
            {
                if (!notifySignaledBlockingCalled)
                {
                    waitObject.wait(timeout);
                }
            }
        }
        catch (InterruptedException exc)
        {
            // return
        }
    }

    public boolean isCheckMediaAccessAuthorizationCalled()
    {
        return checkMediaAccessAuthorizationCalled;
    }

    public boolean isNotifySignaledBlockingCalled()
    {
        return notifySignaledBlockingCalled;
    }

    public void setCheckMediaAccessAuthorizationCalled(boolean checkMediaAccessAuthorizationCalled)
    {
        this.checkMediaAccessAuthorizationCalled = checkMediaAccessAuthorizationCalled;
    }

    public void setNotifySignaledBlockingCalled(boolean notifySignaledBlockingCalled)
    {
        this.notifySignaledBlockingCalled = notifySignaledBlockingCalled;
    }

    public void setDenyAuthorization(boolean value)
    {
        denyAuthorization = value;
    }

    public void setAllowAuthorization(boolean value)
    {
        allowAuthorization = value;
    }

    public boolean getCMAIsSourceDigital()
    {
        return cma_isSourceDigital;
    }

    public void setCMAIsSourceDigital(boolean b)
    {
        cma_isSourceDigital = b;
    }

    /*
     * public OcapLocator[] getCMALocators() { return cma_locators; } public
     * void setCMALocators(OcapLocator[] la) { cma_locators = (OcapLocator[])
     * Arrays.copy(la, OcapLocator.class); }
     */
    public OcapLocator[] getNSBLocators()
    {
        return nsb_locators;
    }

    public void setNSBLocators(OcapLocator[] la)
    {
        nsb_locators = (OcapLocator[]) Arrays.copy(la, OcapLocator.class);
    }

    private static class CannedMediaAccessAuthorization implements MediaAccessAuthorization
    {
        private boolean isDenied = false;

        private int denialReason = 0;

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

        public CannedMediaAccessAuthorization()
        {

        }

        public CannedMediaAccessAuthorization(boolean denied, int reason)
        {
            this.isDenied = denied;
            this.denialReason = reason;
        }
    }
}
