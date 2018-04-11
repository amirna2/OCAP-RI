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

/**
 * This class represents any RecordedService details which are time-based.
 */

package org.cablelabs.impl.recording;

import java.io.Serializable;
import java.util.Vector;

import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.util.TimeAssociatedElement;

public class TimeAssociatedDetailsInfo extends TimeAssociatedElement implements Serializable
{
    private static final long serialVersionUID = 4102017757242372009L;

    private Vector m_components;

    private int m_pcrPid;

    /**
     * Generate TimeAssociatedDetailsInfo with the given mediaTime, components,
     * and PCR PID. Note: components must be non-mutable.
     * 
     * @param mediaTime
     *            Media time associated with the details, in nanoseconds
     * @param components
     *            Vector of RecordedServiceComponents
     * @param pcrPid
     *            The PCR PID
     */
    public TimeAssociatedDetailsInfo(long mediaTimeNS, Vector components, int pcrPid)
    {
        super(mediaTimeNS);
        m_components = components;
        m_pcrPid = pcrPid;
    }

    /**
     * Get the time-associated RecordedService components
     * 
     * @return Non-mutable list of components valid for the associated media
     *         time
     */
    public Vector getComponents()
    {
        return m_components;
    }

    /**
     * Set the time-associated RecordedService components
     * 
     * @param m_components
     *            Non-mutable list of components valid for the associated media
     *            time
     */
    public void setComponents(Vector m_components)
    {
        this.m_components = m_components;
    }

    /**
     * Get the time-associated PCR PID
     * 
     * @return The time-associated PCR PID
     */
    public int getPcrPid()
    {
        return m_pcrPid;
    }

    /**
     * Set the time-associated PCR PID
     * 
     * @param pid
     *            The time-associated PCR PID
     */
    public void setPcrPid(int pcrPid)
    {
        m_pcrPid = pcrPid;
    }

    public long getTimeNanos()
    {
        return time;
    }

    public void setTimeNanos(final long timeNS)
    {
        time = timeNS;
    }

    public long getTimeMillis()
    {
        return time/1000000L;
    }

    public void setTimeMillis(final long timeMS)
    {
        time = timeMS*1000000L;
    }

    /**
     *  Provide a copy of this LightweightTriggerEvent.
     */
    public Object clone() throws CloneNotSupportedException
    {
        return new TimeAssociatedDetailsInfo(time, m_components, m_pcrPid);
    }
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("TimeAssociatedDetailsInfo 0x").append(Integer.toHexString(this.hashCode())).append(":{pcr ").append(
                m_pcrPid).append(", components ").append(m_components.toString()).append("}");

        return sb.toString();
    }
} // END class TimeAssociatedDetailsInfo
