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

package org.cablelabs.impl.storage;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.ocap.storage.DetachableStorageOption;
import org.ocap.storage.StorageProxy;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.util.SecurityUtil;

/**
 * The <code>DetachableStorageOption</code> implementation.
 * 
 * @author Todd Earles
 * @author Ryan Harris
 */
public class DetachableStorageOptionImpl implements DetachableStorageOption
{
    /**
     * Construct this object.
     */
    public DetachableStorageOptionImpl(StorageProxy proxy)
    {
        this.proxy = proxy;
    }

    // Description copied from DetachableStorageOption
    public boolean isDetachable()
    {
        // Get the native handle
        int handle = getNativeHandle(proxy);
        if (handle == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Native handle is null");
            }
            return false;
        }

        return nIsReadyToDetach(handle);
    }

    // Description copied from DetachableStorageOption
    public void makeDetachable() throws IOException
    {
        // security check
        checkPermission();

        // Get the native handle
        int handle = getNativeHandle(proxy);
        if (handle == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Native handle is null");
            }
            return;
        }

        if (nMakeDeviceDetachable(handle))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Device made detachable: " + proxy.toString());
            }
        }
        else
        {
            throw new IOException("Unable to detach storage device " + proxy.toString());
        }
    }

    // Description copied from DetachableStorageOption
    public void makeReady() throws IOException
    {
        // security check
        checkPermission();

        // Get the native handle
        int handle = getNativeHandle(proxy);
        if (handle == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Native handle is null");
            }
            return;
        }

        if (nMakeDeviceReady(handle))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Made ready device " + proxy.toString());
            }
        }
        else
        {
            throw new IOException("Unable to make ready device " + proxy.toString());
        }
    }

    /*
     * Get native handle from the storage proxy object
     */
    private int getNativeHandle(StorageProxy proxy)
    {
        return ((StorageProxyImpl) proxy).getNativeHandle();
    }

    private void checkPermission() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("storage"));
    }

    /**
     * Call to make device detachable
     * 
     * @param handle
     *            used by MPE layer to identify the device
     * @return true if device was successfully made detachable
     */
    private native boolean nMakeDeviceDetachable(int handle);

    /**
     * Call to set up a device for use. After this call has been made and
     * returns, the device should be in the READY state.
     * 
     * @param handle
     *            used by MPE layer to identify the device
     * @return true if device was properly initialized
     */
    private native boolean nMakeDeviceReady(int handle);

    /**
     * Call on a detachable device to see if it is ready to be detached The
     * device should be in the READY state if needing to be detached. If the
     * device is in the BUSY state, this call will return false.
     * 
     * @param handle
     *            used by MPE layer to identify the device
     * @return true if device can be detached
     */
    private native boolean nIsReadyToDetach(int handle);

    // The storage proxy to which this option belongs
    StorageProxy proxy = null;

    private static final Logger log = Logger.getLogger(StorageProxyImpl.class);

}
