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

package org.ocap.system;


/**
 * <p>
 * The SystemModule is used by an OCAP-J application
 * to send an APDU to the CableCARD device.
 * A SystemModule instance is provided by the
 * {@link SystemModuleHandler#ready} method after calling the
 * {@link SystemModuleRegistrar#registerSASHandler} method or the
 * {@link SystemModuleRegistrar#registerMMIHandler} method.
 * </p>
 *
 * @see SystemModuleRegistrar
 *
 * @author Patrick Ladd
 * @author Shigeaki Watanabe (modified by ECN 03.0531-4)
 */
public interface SystemModule
{
    /**
     * <p>
     * This method sends an APDU to the CableCARD device.
     * The APDU structure is defined in Table 16 in Section 8.3 of EIA-679-B
     * referred by [CCIF 2.0] and SCTE 28 2003.
     * The APDU structure consists of apdu_tag, length_field and data_byte.
     * </p>
     * <p>
     * For the Private Host Application of the SAS Resource, the session number
     * for sending the APDU is decided by the OCAP implementation automatically
     * when registered via the
     * {@link SystemModuleRegistrar#registerSASHandler} method. Sending APDU is
     * delegated to the SAS Resource.
     * </p>
     * <p>
     * For the MMI Resource and Application Information Resource, sending APDU
     * is delegated to the resident MMI and Application Information Resources.
     * The OCAP-J application can send APDUs of either MMI Resource or
     * Application Information Resource via a single SystemModule. The OCAP
     * implementation SHALL investigate the apdu_tag field in the APDU and send
     * the APDU to the CableCARD device using the session of the Resource
     * specified by the apdu_tag. The session established by the resident
     * MMI Resource and Application Information Resource is used to send the APDU.
     * If the apdu_tag indicates other Resource except MMI Resource or Application
     * Information Resource this method doesn't throw exception but the
     * {@link SystemModuleHandler#sendAPDUFailed} method is called to notify
     * APDU sending failed.
     * </p>
     * <p>
     * For both above, the delegated Resource encodes the specified APDU into an
     * SPDU complementing a length_field and sends it to the CableCARD device
     * according to the OpenCable CableCARD Interface Specification.
     * </p>
     * <p>
     * The OCAP implementation doesn't have to confirm the validity of the
     * specified dataByte parameter, but SHALL confirm the validity of the
     * specified apduTag value.
     * </p>
     * <p>
     * This method returns immediately and doesn't confirm success of sending
     * the APDU. Errors detected while sending the APDU are notified via the
     * {@link SystemModuleHandler#sendAPDUFailed} method.
     * </p>
     *
     * @param apduTag
     *            an apdu_tag value for the APDU to be sent to the CableCARD
     *            device.
     *
     * @param dataByte
     *            a data_byte binary for the APDU to be sent to the CableCARD
     *            device. This value shall contain only the data_byte part of an
     *            APDU structure defined in the OpenCable CableCARD Interface
     *            Specification. The APDU consists of the specified apduTag and
     *            dataByte and a length_field complemented by the OCAP
     *            implementation.
     *
     * @throws IllegalArgumentException
     *             if the specified apdu_tag value is invalid (i.e., the value
     *             is for another Resource type). Possible apdu_tag values and
     *             possible direction for each Resource are defined in the
     *             OpenCable CableCARD Interface Specification.
     *
     */
    public void sendAPDU(int apduTag, byte[] dataByte);
}
