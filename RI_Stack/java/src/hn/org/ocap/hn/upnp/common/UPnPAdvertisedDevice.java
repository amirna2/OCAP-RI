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

package org.ocap.hn.upnp.common;

import java.net.InetAddress;
import org.w3c.dom.Document;

/**
 * This interface represents a UPnP device as it is advertised on a particular
 * network. It provides the data constituting the device, portions of
 * which depend on the network interface on which it is advertised.
 * Corresponds to the information carried in the UPnP device description
 * document.
 */
public interface UPnPAdvertisedDevice extends UPnPDevice {

    /**
     * Returns the parent UPnP Device of this device, if any.
     *
     * @return This device's parent device. Returns null if this device
     * has no parent.
     */
    // UPnPAdvertisedDevice getParentAdvertisedDevice();

    /**
     * Gets the embedded devices for this UPnP Device.
     *
     * @return The embedded devices for this device.  If this device
     * has no embedded devices, returns a zero length array.
     * Returns only the next level of embedded devices, not
     * recursing through embedded devices for subsequent
     * levels of embedded devices.
     */
    UPnPAdvertisedDevice[] getEmbeddedAdvertisedDevices();

    /**
     * Gets the icons of this device. This returned array is
     * derived from the icon elements within the {@code iconList} element
     * of a device description.
     * If the iconList element in the device description is empty
     * or not present, returns a zero length array.
     *
     * @return The icons that the device declares.
     */
    UPnPAdvertisedDeviceIcon[] getAdvertisedIcons();

    /**
     * Gets the services supported by this device.  Does not return
     * services held in embedded devices.
     *
     * @return    The services supported by this device.  If the
     * serviceList element in the device description is
     * empty, this method returns a zero length array.
     */
    UPnPAdvertisedService[] getAdvertisedServices();

    /**
     * Returns the IP address from which this device was advertised.
     *
     * @return an InetAddress representing this device's IP address.
     */
    InetAddress getInetAddress();

    /**
     * Gets the UPnP presentation page URL of this device. This
     * value is taken from the value of the presentationURL
     * element within a device description.
     *
     * <p>If the presentationURL is empty or not present, returns
     * the empty String.
     *
     * @return The presentationURL of this device.
     */
    String getPresentationURL();

    /**
     * Reports the base URL for all relative URLs of this device.
     * This value is obtained from the {@code URLBase} element within the
     * device description document.  If this is an embedded device, the
     * {@code URLBase} element of the root device is returned.
     *
     * <p>If the {@code URLBase} property is not specified in the device
     * description document, this method returns the URL from which the
     * device description may be retrieved.
     *
     * @return The base URL for all relative URLs of this UPnP Device.
     */
    String getURLBase();

    /**
     * Gets the device description document in XML.  The form of the document
     * is defined by the UPnP Device Architecture specification.
     *
     * <p>For a root device, returns the document starting with the
     * &lt;?xml&gt; node. For an embedded device, returns the
     * sub-document starting
     * with the &lt;device&gt; node of the embedded device. Returns the
     * complete XML document from the level that is appropriate,
     * including any embedded devices.
     *
     * @return The device description document.
     */
    Document getXML();

}
