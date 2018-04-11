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

package org.cablelabs.impl.manager.lightweighttrigger;

import java.io.Serializable;

import org.cablelabs.impl.util.GenericTimeAssociatedElement;
import org.cablelabs.impl.util.TimeAssociatedElement;

/**
 * Class used to store/retrieve artificial event information into/from the
 * TimeTable.
 */
public class LightweightTriggerEvent extends TimeAssociatedElement implements Serializable
{
    private static final long serialVersionUID = 59852263672393499L;

    final public byte[] data;

    final public String eventName;

    final public int id;

    public LightweightTriggerEvent(long time, int id, String eventName)
    {
        super(time);
        this.eventName = eventName;
        this.data = null;
        this.id = id;
    }

    public LightweightTriggerEvent(long time, String eventName, int id, byte[] data)
    {
        super(time);
        this.eventName = eventName;
        this.data = data;
        this.id = id;
    }

    public LightweightTriggerEvent(long time, LightweightTriggerEvent lwte)
    {
        this(time, lwte.eventName, lwte.id, lwte.data);
        // this.eventName = lwte.eventName;
        // this.id = id = lwte.id;
        // this.data = lwte.data;
        // System.arraycopy(lwte.data, 0, this.data, 0, lwte.data.length);
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer("LightweightTriggerEvent 0x");
        sb.append(Integer.toHexString(this.hashCode()));
        sb.append(":{ ").append(eventName).append("; ").append("id=").append(id).append("; ").append("time=").append(
                time);
        sb.append("}");
        return sb.toString();
    }

    public boolean hasSameIdentity(LightweightTriggerEvent lwte)
    {
        if (lwte.id == this.id || lwte.eventName.equals(this.eventName)) return true;
        return false;
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
        byte newData[] = new byte[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);
        return new LightweightTriggerEvent(time, eventName,id,newData);
    }
} // END LightweightTriggerEvent class
