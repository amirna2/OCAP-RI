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

package org.cablelabs.impl.havi.port.mpe;

import org.dvb.media.VideoFormatControl;

import org.cablelabs.impl.util.MPEEnv;

/**
 * Class designed to define a unique duple identifier for configurations. If
 * platformDfc does not equal {@link VideoFormatControl.DFC_PROCESSING_UNKNOWN},
 * then both the native handle and the platformDfc must match. Otherwise, just
 * the native handle is used. This keeps compatability between DSExt and base
 * code, as well as keeping base functionality when DSExt is defined for
 * configurations which don't use DFC (all non-video configurations).
 * 
 * @author Alan Cossitt
 * 
 */
public class UniqueConfigId
{
    public int nativeHandle;

    public int platformDfc;

    /**
     * Is DSExt (Device Settings Extension) being used.
     */
    public static final boolean dsExtUsed = (MPEEnv.getEnv("ocap.api.option.ds") != null);

    private UniqueConfigId()
    {
    };

    public UniqueConfigId(int handle, int dfc)
    {
        if (!dsExtUsed && dfc != VideoFormatControl.DFC_PROCESSING_UNKNOWN) // if
                                                                            // device
                                                                            // settings
                                                                            // not
                                                                            // used,
                                                                            // then
                                                                            // DFC
                                                                            // should
                                                                            // always
                                                                            // be
                                                                            // "unknown".
        {
            throw new IllegalArgumentException("DFC defined for non-DSEXT configuration");
        }
        nativeHandle = handle;
        platformDfc = dfc;
    }

    public UniqueConfigId(int handle)
    {
        this(handle, VideoFormatControl.DFC_PROCESSING_UNKNOWN);
    }

    public boolean equals(Object obj)
    {
        if (super.equals(obj)) return true;

        if (obj instanceof UniqueConfigId)
        {
            UniqueConfigId id = (UniqueConfigId) obj;
            return equals(id.nativeHandle, id.platformDfc);
        }
        return false;
    }

    public boolean equals(int nHandle, int dfc)
    {
        if (dsExtUsed) // if DSEXT, handle and DFC must both match
        {
            if (this.nativeHandle == nHandle)
            {
                // if DFC is undefined (i.e., not used) for this config, don't
                // use it to match.
                if (this.platformDfc == VideoFormatControl.DFC_PROCESSING_UNKNOWN)
                {
                    return true;
                }
                else if (this.platformDfc == dfc)
                {
                    return true;
                }
            }
        }
        else if (this.nativeHandle == nHandle)
        {
            return true;
        }
        return false;
    }

    public String toString()
    {
        return "UniqueConfigId:  nativeHandle=" + nativeHandle + ", platformDfc=" + platformDfc + ", hash="
                + this.hashCode();
    }

    /**
     * overridden since Hashtable uses both hashCode() and equals() for
     * comparision. I want <handle, -1> to match with <handle, 0> or <handle,
     * 8>, so I've changed equals and hashCode to make this happen.
     */
    public int hashCode()
    {
        return this.nativeHandle;
    }
}
