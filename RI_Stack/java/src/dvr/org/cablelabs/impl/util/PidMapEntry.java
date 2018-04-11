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

package org.cablelabs.impl.util;

import javax.tv.service.navigation.StreamType;

import org.cablelabs.impl.service.ServiceComponentExt;

/**
 * This class is used to encapsulate the elements in the PidMapTable.
 * 
 */
public class PidMapEntry
{
    // Use values outside the elementary stream range for these
    // internal types (stream type is 8 bits)
    public static final short NTSC_VIDEO = 0x100;

    public static final short NTSC_PRIMARY_AUDIO = 0x101;

    public static final short NTSC_SECONDARY_AUDIO = 0x102;

    public static final short ELEM_STREAMTYPE_UNKNOWN = 0;

    public static final int PID_UNKNOWN = -1;

    private short streamType = MediaStreamType.UNKNOWN;

    private short srcElementaryStreamType = ELEM_STREAMTYPE_UNKNOWN;

    private int srcPID = PID_UNKNOWN;

    private short recElementaryStreamType = ELEM_STREAMTYPE_UNKNOWN;

    private int recPID = PID_UNKNOWN;

    private ServiceComponentExt serviceComponentReference = null;

    public PidMapEntry()
    {
        // Use the instance-initialized values
    }

    public PidMapEntry(short mediaStrmType, short srcType, int sPid, short recType, int rPid, ServiceComponentExt ref)
    {
        streamType = mediaStrmType;
        srcElementaryStreamType = srcType;
        srcPID = sPid;
        recElementaryStreamType = recType;
        recPID = rPid;
        serviceComponentReference = ref;
    }

    public PidMapEntry(StreamType strmType, short srcType, int sPid, short recType, int rPid, ServiceComponentExt ref)
    {
        streamType = streamTypeToMediaStreamType(strmType);
        srcElementaryStreamType = srcType;
        srcPID = sPid;
        recElementaryStreamType = recType;
        recPID = rPid;
        serviceComponentReference = ref;
    }

    public short getStreamType()
    {
        return streamType;
    }

    public void setStreamType(short strmType)
    {
        streamType = strmType;
    }

    public short getSourceElementaryStreamType()
    {
        return srcElementaryStreamType;
    }

    public void setSourceElementaryStreamType(short srcType)
    {
        srcElementaryStreamType = srcType;
    }

    public int getSourcePID()
    {
        return srcPID;
    }

    public void setSourcePID(int sPid)
    {
        srcPID = sPid;
    }

    public short getRecordedElementaryStreamType()
    {
        return recElementaryStreamType;
    }

    public void setRecordedElementaryStreamType(short recType)
    {
        recElementaryStreamType = recType;
    }

    public int getRecordedPID()
    {
        return recPID;
    }

    public void setRecordedPID(int rPid)
    {
        recPID = rPid;
    }

    public ServiceComponentExt getServiceComponentReference()
    {
        return serviceComponentReference;
    }

    public void setServiceComponentReference(ServiceComponentExt ref)
    {
        serviceComponentReference = ref;
    }

    // Description copied from Object
    public boolean equals(Object obj)
    {
        // Make sure we have a good object
        if (obj == null || !(obj instanceof PidMapEntry))
        {
            return false;
        }
        // Compare fields
        PidMapEntry entry = (PidMapEntry) obj;
        return (getSourcePID() == entry.getSourcePID()
                && getSourceElementaryStreamType() == entry.getSourceElementaryStreamType() && getStreamType() == entry.getStreamType());
    }
    
    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + getSourcePID();
        hash = 31 * hash + getSourceElementaryStreamType();
        hash = 31 * hash + getStreamType();
        return hash;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return ("PidMapEntry:[streamType 0x" + Integer.toHexString(streamType) + ",srcElemType 0x"
                + Integer.toHexString(srcElementaryStreamType) + ",srcPID 0x" + Integer.toHexString(srcPID)
                + ",recElemType 0x" + Integer.toHexString(recElementaryStreamType) + ",recPID 0x"
                + Integer.toHexString(recPID) + ",svcComp " + serviceComponentReference + ']');
    }

    public static short streamTypeToMediaStreamType(StreamType strType)
    {
        if (strType == StreamType.VIDEO)
        {
            return MediaStreamType.VIDEO;
        }
        if (strType == StreamType.AUDIO)
        {
            return MediaStreamType.AUDIO;
        }
        if (strType == StreamType.DATA)
        {
            return MediaStreamType.DATA;
        }
        if (strType == StreamType.SUBTITLES)
        {
            return MediaStreamType.SUBTITLES;
        }
        if (strType == StreamType.SECTIONS)
        {
            return MediaStreamType.SECTIONS;
        }

        return MediaStreamType.UNKNOWN;
    } // END streamTypeToMediaStreamType()

    public static StreamType mediaStreamTypeToStreamType(short mediaStrType)
    {
        switch (mediaStrType)
        {
            case MediaStreamType.VIDEO:
                return StreamType.VIDEO;
            case MediaStreamType.AUDIO:
                return StreamType.AUDIO;
            case MediaStreamType.DATA:
                return StreamType.DATA;
            case MediaStreamType.SUBTITLES:
                return StreamType.SUBTITLES;
            case MediaStreamType.SECTIONS:
                return StreamType.SECTIONS;
            case MediaStreamType.UNKNOWN:
            case MediaStreamType.PCR:
            case MediaStreamType.PMT:
                return StreamType.UNKNOWN;
            default:
                throw new IllegalArgumentException("Bad media stream type value: " + mediaStrType);
        }
    } // END mediaStreamTypeToStreamType()
}
