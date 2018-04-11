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

package javax.tv.service;

/**
 * This class represents service type values such as "digital television",
 * "digital radio", "NVOD reference service", "NVOD time-shifted service",
 * "analog television", "analog radio", "data broadcast" and "application".
 * These basic service types can be extended by subclassing.
 * <p>
 * 
 * (These values are mappable to the ATSC service type in the VCT table and the
 * DVB service type in the Service Descriptor.)
 */
public class ServiceType
{

    private String name = null;

    /**
     * Creates a service type object.
     * 
     * @param name
     *            The string name of this type (e.g., "DIGITAL_TV").
     */
    protected ServiceType(String name)
    {
        this.name = name;
        if (name == null)
        {
            throw new NullPointerException("Name is null");
        }
    }

    /**
     * Provides the string name of the type. For the type objects defined in
     * this class, the string name will be identical to the class variable name.
     * 
     * @return The string name of the type.
     */
    public String toString()
    {
        return name;
    }

    /**
     * Digital TV service type.
     */
    public static final ServiceType DIGITAL_TV;

    /**
     * Digital radio service type.
     */
    public static final ServiceType DIGITAL_RADIO;

    /**
     * NVOD reference service type.
     */
    public static final ServiceType NVOD_REFERENCE;

    /**
     * NVOD time-shifted service type.
     */
    public static final ServiceType NVOD_TIME_SHIFTED;

    /**
     * Analog TV service type.
     */
    public static final ServiceType ANALOG_TV;

    /**
     * Analog radio service type.
     */
    public static final ServiceType ANALOG_RADIO;

    /**
     * Data broadcast service type identifying a data service.
     */
    public static final ServiceType DATA_BROADCAST;

    /**
     * Data application service type identifying an interactive application.
     */
    public static final ServiceType DATA_APPLICATION;

    /**
     * Unknown service type.
     */
    public static final ServiceType UNKNOWN;

    // Needed for compilation
    static
    {
        DIGITAL_TV = new ServiceType("DIGITAL_TV");
        DIGITAL_RADIO = new ServiceType("DIGITAL_RADIO");
        NVOD_REFERENCE = new ServiceType("NVOD_REFERENCE");
        NVOD_TIME_SHIFTED = new ServiceType("NVOD_TIME_SHIFTED");
        ANALOG_TV = new ServiceType("ANALOG_TV");
        ANALOG_RADIO = new ServiceType("ANALOG_RADIO");
        DATA_BROADCAST = new ServiceType("DATA_BROADCAST");
        DATA_APPLICATION = new ServiceType("DATA_APPLICATION");
        UNKNOWN = new ServiceType("UNKNOWN");
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
