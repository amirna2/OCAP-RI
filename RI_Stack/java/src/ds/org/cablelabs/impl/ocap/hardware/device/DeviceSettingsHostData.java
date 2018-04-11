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

package org.cablelabs.impl.ocap.hardware.device;

import java.io.Serializable;
import java.util.Hashtable;

import org.cablelabs.impl.ocap.hardware.HostData;

public class DeviceSettingsHostData extends HostData implements Serializable
{
    /**
     * videoPortToOutputConfig
     * 
     * Maps a video port to its VideoOutputPort key is unique ID (string) value
     * is a VideoOutputConfiguration object
     * 
     * This means that all VideoOutputConfiguration classes must be Serializable
     */
    Hashtable videoPortToOutputConfig = new Hashtable();

    /**
     * audioPortToValues
     * 
     * maps a audio port to its values in the form of a AudioPortValues.
     * 
     * Key is a unique ID (string) value is a AudioPortValues object.
     */
    Hashtable audioPortToValues = new Hashtable();

    static class AudioPortValues implements Serializable
    {
        public int compression;

        public float gain;

        public int encoding;

        public float level;

        public boolean loopThru;

        public boolean muted;

        public int stereoMode;

        AudioPortValues(String compression, String gain, String encoding, String level, String loopThru, String muted,
                String stereoMode)
        {
            this.compression = Integer.parseInt(compression);
            this.gain = Float.parseFloat(gain);
            this.encoding = Integer.parseInt(encoding);
            this.level = Float.parseFloat(level);
            this.loopThru = Boolean.valueOf(loopThru) == Boolean.TRUE;
            this.muted = Boolean.valueOf(muted) == Boolean.TRUE;
            this.stereoMode = Integer.parseInt(stereoMode);
        }

        AudioPortValues(int compression, float gain, int encoding, float level, boolean loopThru, boolean muted,
                int stereoMode)
        {
            this.compression = compression;
            this.gain = gain;
            this.encoding = encoding;
            this.level = level;
            this.loopThru = loopThru;
            this.muted = muted;
            this.stereoMode = stereoMode;
        }

    }
}
