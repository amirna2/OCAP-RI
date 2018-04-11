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
package org.cablelabs.impl.media.presentation;

import javax.media.Time;
import javax.media.protocol.DataSource;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.player.AlarmClock;
import org.cablelabs.impl.media.player.PlayerClock;
import org.cablelabs.impl.media.player.AlarmClock.Alarm;
import org.cablelabs.impl.media.player.AlarmClock.Alarm.Callback;
import org.cablelabs.impl.util.TaskQueue;

/**
 * CannedPresentationContext
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedPresentationContext implements PresentationContext
{

    public Object lock;

    public CannedPlayerClock clock;

    public CannedPresentationContext()
    {
        lock = new Object();
        clock = new CannedPlayerClock();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.PresentationContext#clockSetMediaTime
     * (javax.media.Time)
     */
    public void clockSetMediaTime(Time mt, boolean postMediaTimeEvent)
    {
        clock.setMediaTime(mt);
    }
    
    public boolean isMediaTimeSet()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.PresentationContext#clockSetRate
     * (float)
     */
    public void clockSetRate(float rate, boolean postRateChangeEvent)
    {
        clock.setRate(rate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.presentation.PresentationContext#getClock()
     */
    public PlayerClock getClock()
    {
        return clock;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.presentation.PresentationContext#getLock()
     */
    public Object getLock()
    {
        return lock;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.PresentationContext#
     * getOwnerCallerContext()
     */
    public CallerContext getOwnerCallerContext()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.PresentationContext#getSource()
     */
    public DataSource getSource()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.PresentationContext#getTaskQueue()
     */
    public TaskQueue getTaskQueue()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.media.presentation.PresentationContext#
     * notifyMediaPresented()
     */
    public void notifyMediaPresented()
    {
        // TODO Auto-generated method stub

    }
    
    public void notifyStarted() {
        
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.presentation.PresentationContext#notifyStopByError
     * (java.lang.String)
     */
    public void notifyStopByError(String reason, Throwable throwable)
    {
        // TODO Auto-generated method stub

    }

    public Alarm createAlarm(AlarmClock.AlarmSpec spec, Callback callback)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void destroyAlarm(Alarm alarm)
    {
        // TODO Auto-generated method stub

    }

    public boolean getMute()
    {
        return false;
    }

    public float getGain()
    {
        return 0.0F;
    }
}
