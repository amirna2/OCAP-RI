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

package org.ocap.hn.upnp.server;

import org.ocap.hn.upnp.common.UPnPAdvertisedDeviceIcon;
import org.ocap.hn.upnp.common.UPnPDeviceIcon;


/**
 * This class represents a UPnP Device Icon with associated 
 * binary data for a {@code UPnPManagedDevice}.
 */
public class UPnPManagedDeviceIcon implements UPnPDeviceIcon
{
    private final String m_mimetype;
    private final int m_width;
    private final int m_height;
    private final int m_colordepth;    
    private final byte[] m_data;
    
    /**
     * Construct the instance. 
     *  
     * @param mimetype The mimetype of this icon in the form 
     *                 image/xxxx.
     * @param width The width of this icon in pixels. 
     * @param height The height of this icon in pixels. 
     * @param colordepth The color depth of this icon in bits. 
     * @param data A byte array containing the binary icon data.
     *             The contents of the array are copied into the resulting
     *             {@code UPnPManagedDeviceIcon} object.
     *             No validation is performed on the array, but it
     *             should contain data consistent with the other parameters
     *             to the constructor.
     */
    public UPnPManagedDeviceIcon(String mimetype, int width, int height,
                                 int colordepth, byte[] data)
    {
        m_mimetype = mimetype;
        m_width = width;
        m_height = height;
        m_colordepth = colordepth;

        if(data != null)
        {
            m_data = new byte[data.length];
            System.arraycopy(data, 0, m_data, 0, data.length);
        }
        else
        {
            m_data = null;
        }
    }

    /**
     * Gets the binary data that represents this icon. 
     * 
     * @return An array containing a copy of the binary icon data.
     */
    public byte[] getData()
    {
        if (m_data == null)
        {
            return null;
        }
        return (byte[]) m_data.clone();
    }

    /**
     * Gets the network representations of this
     * <code>UPnPManagedDeviceIcon</code>.
     * Since the UPnP device description {@code iconList} element contains
     * information specific to the network interface on which it is advertised,
     * there can be multiple {@code UPnPAdvertisedDeviceIcon} objects
     * associated with a single {@code UPnPManagedDeviceIcon}.
     *
     * @return The network representations of this
     * {@code UPnPManagedDeviceIcon}.  Returns a zero-length array if
     * the corresponding UPnP device has not been advertised on a network
     * interface.
     */
    public UPnPAdvertisedDeviceIcon[] getAdvertisedDeviceIcons()
    {
        // We don't have enough information here, need subclass to implement
        // correctly.
        return new UPnPAdvertisedDeviceIcon[0];
    }

    ////////  Methods inherited from UPnPDeviceIcon  ////////

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * If the corresponding UPnP device has been advertised,
     * this method returns the same value as
     * {@code getAdvertisedDeviceIcons()[0].getColorDepth()}.
     */
    public int getColorDepth()
    {
        return m_colordepth;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * If the corresponding UPnP device has been advertised,
     * this method returns the same value as
     * {@code getAdvertisedDeviceIcons()[0].getHeight()}.
     */
    public int getHeight()
    {
        return m_height;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * If the corresponding UPnP device has been advertised,
     * this method returns the same value as
     * {@code getAdvertisedDeviceIcons()[0].getMimeType()}.
     */
    public String getMimeType()
    {
        return m_mimetype;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * If the corresponding UPnP device has been advertised,
     * this method returns the same value as
     * {@code getAdvertisedDeviceIcons()[0].getWidth()}.
     */
    public int getWidth()
    {
        return m_width;
    }
}
