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

package org.cablelabs.impl.ocap.hardware;

import org.apache.log4j.Logger;
import org.ocap.hardware.Host;

/**
 * Implementation-specific instance of <code>Host</code>.
 * 
 * This was an inner class and has been moved from {@code HostManagerImpl}
 * 
 * @author Aaron Kamienski
 * @author Alan Cossitt -- DSExt changes.
 */
public class HostImpl extends Host implements ExtendedHost
{
    private static final Logger log = Logger.getLogger(HostImpl.class.getName());

    private static HostPersistence hostPersistence = null;

    /**
     * Initializes JNI.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();

    }

    private static void setNewHostPersistence(HostPersistence hp)
    {
        hostPersistence = hp;
        hostPersistence.load();
    }

    public HostImpl()
    {
        synchronized (HostImpl.class)
        {
            if (hostPersistence == null)
            {
                setNewHostPersistence(createNewHostPersistence());
            }
        }
    }

    /**
     * Implements {@link ExtendedHost#forceReboot()}.
     */
    public void forceReboot()
    {
        hostReboot();
    }

    /**
     * Implements {@link ExtendedHost#reboot(int)}.
     */
    public void reboot(int reason)
    {
        super.reboot(reason);
    }

    /**
     * Implements {@link ExtendedHost#setAudioMode(int)}.
     */
    public void setAudioMode(int mode) throws IllegalArgumentException
    {
        if (this.getAudioMode() == mode)
        {
            return;
        }
        
        if (mode == ExtendedHost.AUDIO_ON || mode == ExtendedHost.AUDIO_MUTED)
        {
            // Call native method to set audio mode.
            if (!setHostAudioMode(mode))
            {
                if (log.isErrorEnabled())
                {
                    log.error("setAudioMode() - problems setting audio mode");
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("mode is not AUDIO_ON or AUDIO_MUTED");
        }        
    }

    /**
     * Implements {@link ExtendedHost#getAudioMode()}.
     */
    public int getAudioMode()
    {
        // Call native method to acquire audio mode.
        return getHostAudioMode();       
    }

    public static HostPersistence getHostPersistence()
    {
        return hostPersistence;
    }

    /**
     * newHostPersistence
     * 
     * Designed to be overridden by any derived classes who have a different
     * persistence.
     * 
     * @return HostPersistence or derived class
     */
    protected HostPersistence createNewHostPersistence()
    {
        return new HostPersistence();
    }
    
    /**
     * Native method to acquire the current audio mode.
     * 
     * @return current audio mode, which must be one of the following constants:
     *         1 = Audio on 2 = Audio muted
     */
    private static native int getHostAudioMode();

    /**
     * Native method to set the current audio mode.
     * 
     * @return JNI_TRUE if no problems encountered, false otherwise
     */
    private static native boolean setHostAudioMode(int mode);    
}
