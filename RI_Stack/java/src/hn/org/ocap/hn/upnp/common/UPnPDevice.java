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

/**
 * This interface is an abstract representation of a UPnP device.
 * It provides the data constituting a UPnP device that is
 * independent of the network interface on which the device is advertised.
 */
public interface UPnPDevice {

    /**
     * Gets the UPnP deviceType of this device. This value is
     * taken from the value of the {@code deviceType} element within the
     * device description.
     *
     * @return The type of this device.
     * If the {@code deviceType} is empty or not present,
     * returns the empty String.
     */
    String getDeviceType();

    /**
     * Gets the UPnP "friendly name" of this device. This value is
     * taken from the value of the {@code friendlyName} element within the
     * device description.
     *
     * @return The {@code friendlyName} of this device.
     * If the {@code friendlyName} is empty or not present, returns the
     * empty String.
     */
    String getFriendlyName();

    /**
     * Gets the UPnP manufacturer of this device. This value is
     * taken from the value of the {@code manufacturer} element within the
     * device description.
     *
     * @return The manufacturer of this device.
     * If the manufacturer is empty or not present, returns the
     * empty String.
     */
    String getManufacturer();

    /**
     * Gets the UPnP manufacturer URL of this device. This value is
     * taken from the value of the {@code manufacturerURL} element
     * within the device description.
     * If the manufacturerURL is empty or not present, returns
     * the empty String.
     *
     * @return The manufacturerURL of this device.
     */
    String getManufacturerURL();

    /**
     * Gets the UPnP model description of this device.
     * This value is taken from the value of the
     * {@code modelDescription} element within the device description.
     * If the modelDescription is empty or not present, returns
     * the empty String.
     *
     * @return The modelDescription of this device.
     */
    String getModelDescription();

    /**
     * Gets the UPnP model name of this device. This value is
     * taken from the value of the {@code modelName} element within the device
     * description.
     *
     * @return The {@code modelName} of this device.
     * If the {@code modelName} is empty or not present, returns the
     * empty String.
     */
    String getModelName();

    /**
     * Gets the UPnP model number of this device. This value is
     * taken from the value of the {@code modelNumber} element within the
     * device description.
     * If the modelNumber is empty or not present, returns the
     * empty String.
     *
     * @return The modelNumber of this device.
     */
    String getModelNumber();

    /**
     * Gets the UPnP model URL of this device. This value is
     * taken from the value of the {@code modelURL} element within the
     * device description.
     * If the modelURL is empty or not present, returns the empty
     * String.
     *
     * @return The modelURL of this device.
     */
    String getModelURL();

    /**
     * Gets the UPnP serial number of this device. This value is
     * taken from the value of the {@code serialNumber} element within the
     * device description.
     * If the serialNumber is empty or not present, returns the
     * empty String.
     *
     * @return The serialNumber of this device.
     */
    String getSerialNumber();

    /**
     * Gets the UPnP specVersion major and minor values of this
     * UPnP device, or of the root UPnP device containing this device.
     * This value is taken from the value of the major and minor sub
     * elements of the {@code specVersion} element within the device
     * description.
     * The format of the returned String is the &lt;major&gt;
     * value, followed by '.', followed by the &lt;minor&gt; value.
     *
     * @return The UPnP specVersion of this device.
     */
    String getSpecVersion();

    /**
     * Gets the UPnP Unique Device Name of this device. This value
     * is taken from the value of the {@code UDN} element
     * within the device description.
     *
     * @return The UDN of this device.
     * If the UDN is empty or not present, returns the empty
     * String.
     */
    String getUDN();

    /**
     * Gets the UPnP Universal Product Code of this device. This
     * value is taken from the value of the {@code UPC} element
     * within the device description.
     * If the UPC is empty or not present, returns the empty
     * String.
     *
     * @return The UPC of this device.
     */
    String getUPC();

    /**
     * Reports whether this UPnP device is a UPnP root device.
     *
     * @return true if this UPnP device represents a root device,
     * false if not.
     */
    boolean isRootDevice();

}
