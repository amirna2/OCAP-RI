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

package org.ocap.hardware.pod;

/**
 * This class represents an Application that resides in the OpenCable CableCARD
 * device. The CableCARD device has zero or more CableCARD Applications.
 * PODApplication instances corresponding to those CableCARD Applications are
 * retrieved by the {@link org.ocap.hardware.pod.POD#getApplications} method.
 * This class provides information of the CableCARD Application described in the
 * Application_info_req() APDU defined in [CCIF 2.0].
 **/
public interface PODApplication
{
    /**
     * The Conditional Access application type. This value is defined for the
     * application_type field in the Application_info_cnf() APDU. See OpenCable
     * CableCARD Interface specification.
     **/
    public static final int TYPE_CA = 0;

    /**
     * The "Copy Protection" application type. This value is defined for the
     * application_type field in the Application_info_cnf() APDU. See OpenCable
     * CableCARD Interface specification.
     **/
    public static final int TYPE_CP = 1;

    /**
     * The "IP Service" application type. This value is defined for the
     * application_type field in the Application_info_cnf() APDU. See OpenCable
     * CableCARD Interface specification.
     **/
    public static final int TYPE_IP = 2;

    /**
     * The "Network Interface - DVS/167" application type. This value is defined
     * for the application_type field in the Application_info_cnf() APDU.
     * See [CCIF 2.0].
     **/
    public static final int TYPE_DVS167 = 3;

    /**
     * The "Network Interface - DVS/178" application type. This value is defined
     * for the application_type field in the Application_info_cnf() APDU.
     * See [CCIF 2.0].
     **/
    public static final int TYPE_DVS178 = 4;

    /**
     * The "Diagnostic" application type. This value is defined for the
     * application_type field in the Application_info_cnf() APDU.
     * See [CCIF 2.0].
     **/
    public static final int TYPE_DIAGNOSTIC = 6;

    /**
     * The "Undesignated" application type. This value is defined for the
     * application_type field in the Application_info_cnf() APDU.
     * See [CCIF 2.0].
     **/
    public static final int TYPE_UNDESIGNATED = 7;

    /**
     * This method returns an application type value of the CableCARD
     * Application represented by this class. The application type is described
     * in the application_type field in the Application_info_cnf() APDU.
     *
     * @return an application type value of the CableCARD application
     *         represented by this class. Known values are defined as the field
     *         values prefixed with "TYPE_".
     **/
    public int getType();

    /**
     * This method returns an application version number of the CableCARD
     * Application represented by this class. The application version number is
     * described in the application_version_number field in the
     * Application_info_cnf() APDU.
     *
     * @return an application version number value of the CableCARD Application
     *         represented by this class.
     **/
    public int getVersionNumber();

    /**
     * This method returns an application name of the CableCARD Application
     * represented by this class. The application version number is described in
     * the application_name_byte field in the Application_info_cnf() APDU.
     *
     * @return an application name of the CableCARD Application represented by
     *         this class.
     **/
    public String getName();

    /**
     * This method returns a URL of the CableCARD Application represented by
     * this class. The URL is described in the application_url_byte field in the
     * Application_info_cnf() APDU.
     *
     * @return a URL of the CableCARD Application represented by this class.
     *
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("podApplication").
     **/
    public String getURL();
}
