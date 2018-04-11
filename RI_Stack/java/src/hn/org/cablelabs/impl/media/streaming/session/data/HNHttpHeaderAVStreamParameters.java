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

package org.cablelabs.impl.media.streaming.session.data;

import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.StreamType;

import org.apache.log4j.Logger;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.util.Arrays;

public class HNHttpHeaderAVStreamParameters
{
    private static Logger log = Logger.getLogger(HNHttpHeaderAVStreamParameters.class);

    public static final int UNKNOWN_PID_VALUE = 0xFFFF;
    
    private int m_videoPID = UNKNOWN_PID_VALUE;

    private int m_videoType = UNKNOWN_PID_VALUE;

    private int m_audioPID = UNKNOWN_PID_VALUE;

    private int m_audioType = UNKNOWN_PID_VALUE;

    public HNHttpHeaderAVStreamParameters(int videoPID, int videoType, int audioPID, int audioType)
            throws IllegalArgumentException
    {
        // TODO: Check pid range - find out the invalid values and compare
        // against them??
        if (((videoType < 0) || (videoPID > 0x1FFF)) || ((audioPID > 0x1FFF) || (audioType < 0)))
        {
            throw new IllegalArgumentException("Invalid PID or Type: videoPID: " + videoPID + " videoType: "
                    + videoType + " audioPID: " + audioPID + " audioType: " + audioType);
        }

        this.m_videoPID = videoPID;
        this.m_videoType = videoType;
        this.m_audioPID = audioPID;
        this.m_audioType = audioType;

        if (log.isDebugEnabled())
        {
            log.debug("constructor with pids/types - video pid set to: " + m_videoPID + ", video pid type set to: " + m_videoType + ", audio pid set to: " + m_audioPID + ", audio pid type set to: " + m_audioType);
        }
    }

    /**
     * Initialize HN AV stream parameters with components array.
     *
     * @param components components to select.
     */
    public HNHttpHeaderAVStreamParameters(ServiceComponent[] components) {
        if (log.isDebugEnabled())
        {
            log.debug("input components: " + Arrays.toString(components));
        }
        for (int i = 0; i < components.length; ++i) {
            ServiceComponentExt component = (ServiceComponentExt) components[i];

            if (StreamType.VIDEO.equals(component.getStreamType())) {
                this.m_videoPID = component.getPID();
                this.m_videoType = component.getElementaryStreamType();
            }
            if (StreamType.AUDIO.equals(component.getStreamType())) {
                this.m_audioPID = component.getPID();
                this.m_audioType = component.getElementaryStreamType();
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("constructor WITH components - video pid set to: " + m_videoPID + ", video pid type set to: " + m_videoType + ", audio pid set to: " + m_audioPID + ", audio pid type set to: " + m_audioType);
        }
    }

    public HNHttpHeaderAVStreamParameters()
    {
        if (log.isDebugEnabled())
        {
            log.debug("constructor WITHOUT components - video pid: " + m_videoPID + ", video pid type: " + m_videoType + ", audio pid: " + m_audioPID + ", audio pid type: " + m_audioType);
        }
    }
/*
    public int getAudioPID()
    {
        return this.m_audioPID;
    }

    public void setAudioPID(int audioPID)
    {
        this.m_audioPID = audioPID;
    }

    public int getVideoPID()
    {
        return this.m_videoPID;
    }

    public void setVideoPID(int videoPID)
    {
        this.m_videoPID = videoPID;
    }

    public int getAudioType()
    {
        return this.m_audioType;
    }

    public void setAudioType(int audioType)
    {
        this.m_audioType = audioType;
    }

    public int getVideoType()
    {
        return this.m_videoType;
    }

    public void setVideoType(int videoType)
    {
        this.m_videoType = videoType;
    }
*/
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        HNHttpHeaderAVStreamParameters that = (HNHttpHeaderAVStreamParameters) o;

        if (m_audioPID != that.m_audioPID)
        {
            return false;
        }
        if (m_audioType != that.m_audioType)
        {
            return false;
        }
        if (m_videoPID != that.m_videoPID)
        {
            return false;
        }
        if (m_videoType != that.m_videoType)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result = m_videoPID;
        result = 31 * result + m_videoType;
        result = 31 * result + m_audioPID;
        result = 31 * result + m_audioType;
        return result;
    }

    public String toString()
    {
        return "HNHttpHeaderAVStreamParameters - videoPID: " + m_videoPID + ", videoType: " + m_videoType +
                ", audioPID: " + m_audioPID + ", m_audioType: " + m_audioType;
    }
}
