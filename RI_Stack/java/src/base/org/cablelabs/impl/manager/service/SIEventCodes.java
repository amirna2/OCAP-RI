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

package org.cablelabs.impl.manager.service;

/**
 * This class stores MPE SI event code values so that they can be interpreted by
 * our Java EDListeners. DO NOT modify any of the event code values! These
 * values were intentionally not made "final" so that their values could be set
 * by JNI at initialization-time.
 * 
 * @author Greg Rutz
 */
public class SIEventCodes
{
    //findbugs complains that codes should be final. However, we need to initialize them via JNI, so no...
    //findbugs configured to ignore the "error".
    public static int SI_EVENT_UNKNOWN;

    public static int SI_EVENT_OOB_VCT_ACQUIRED;

    public static int SI_EVENT_OOB_NIT_ACQUIRED;

    public static int SI_EVENT_OOB_PAT_ACQUIRED;

    public static int SI_EVENT_OOB_PMT_ACQUIRED;

    public static int SI_EVENT_IB_PAT_ACQUIRED;

    public static int SI_EVENT_IB_PMT_ACQUIRED;

    public static int SI_EVENT_TRANSPORT_STREAM_UPDATE;

    public static int SI_EVENT_NETWORK_UPDATE;

    public static int SI_EVENT_SERVICE_DETAILS_UPDATE;

    public static int SI_EVENT_SERVICE_COMPONENT_UPDATE;

    public static int SI_EVENT_IB_PAT_UPDATE;

    public static int SI_EVENT_IB_PMT_UPDATE;

    public static int SI_EVENT_OOB_PAT_UPDATE;

    public static int SI_EVENT_OOB_PMT_UPDATE;

    public static int SI_EVENT_SI_ACQUIRING;

    public static int SI_EVENT_SI_NOT_AVAILABLE_YET;

    public static int SI_EVENT_SI_FULLY_ACQUIRED;
    
    public static int SI_EVENT_SI_DISABLED;

    public static int SI_EVENT_TUNED_AWAY;

    public static int SI_EVENT_NIT_SVCT_ACQUIRED;

    public static int SI_CHANGE_TYPE_ADD;

    public static int SI_CHANGE_TYPE_MODIFY;

    public static int SI_CHANGE_TYPE_REMOVE;

    /*
     * This method initializes the event code values from JNI
     */
    native static void nInitEventCodes();

    /**
     * Performs static initialization for this class
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInitEventCodes();
    }
}
