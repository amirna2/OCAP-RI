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
 * The SystemModuleHandler is used by an OCAP-J application
 * for the following purposes:
 * 1) receive an APDU from the CableCARD device,
 * 2) detect an unsent APDU to the POD in case of an error,
 * and 3) notification of becoming registered and unregistered.
 *
 * @see SystemModuleRegistrar
 *
 * @author Patrick Ladd
 * @author Shigeaki Watanabe (modified by ECN 03.0531-4)
 */
public interface SystemModuleHandler
{
    /**
     * <p>
     * This is a call back method to notify an APDU received from the CableCARD
     * device.
     * The APDU structure is defined in Table 16 in Section 8.3 of EIA-679-B
     * referred by [CCIF 2.0] and SCTE 28 2003.
     * The APDU structure consists of apdu_tag, length_field and
     * data_byte.
     * </p>
     * <p>
     * For the Private Host Application on the SAS Resource, the
     * SystemModuleHandler is bound to a specific session number (and a specific
     * Private Host Application ID) when it is registered via the
     * {@link SystemModuleRegistrar#registerSASHandler} method. Only the
     * receiveAPDU() method that is bound to the session of the received APDU
     * shall be called only once by the OCAP implementation.
     * </p>
     * <p>
     * For the MMI Resource and the Application Information Resource, the OCAP-J
     * application can receive APDUs for both Resources by a single
     * SystemModuleHandler. The OCAP implementation shall call the receiveAPDU()
     * method of the SystemModuleHandler registered via the
     * {@link SystemModuleRegistrar#registerMMIHandler} method only once for
     * both the MMI and Application Information APDU.
     * </p>
     * <p>
     * The OCAP implementation extract the APDU from an SPDU from the CableCARD
     * device according to the OpenCable CableCARD Interface Specification, and
     * then call this method. Note that the OCAP implementation simply retrieves
     * the field values from the APDU and call this method. No validity check is
     * done by the OCAP implementation. Though SPDU and TPDU mechanism may
     * detect a destruction of the APDU structure while transmitting, the OCAP
     * shall call this method every time when it receives an APDU. In such case,
     * the parameters may be invalid so that the OCAP-J application can detect
     * an error.
     * </p>
     * <p>
     * Note that if the CableCARD device returns an APDU indicating an error
     * condition, this method is called instead of the sendAPDUFailed() method.
     * </p>
     * <p>
     * This method shall return immediately.
     * <p>
     *
     * @param apduTag
     *            an apdu_tag value in the APDU coming from the CableCARD
     *            device. I.e., first 3 bytes. If the corresponding bytes are
     *            missed, they are filled by zero. Note that the OCAP
     *            implementation calls this method according to the session
     *            number, so the apdu_tag value may be out of the valid range.
     *
     * @param lengthField
     *            a length_field value in the APDU coming from the CableCARD
     *            device. This is a decimal int value converted from a length
     *            field encoded in ASN.1 BER. If the corresponding bytes are
     *            missing, the value of this parameter is set to 0.
     *
     * @param dataByte
     *            an data_byte bytes in the APDU coming from the CableCARD
     *            device. If the corresponding bytes are missed since signaling
     *            trouble, only existing bytes are specified. If they are more
     *            than expected length, all existing bytes are specified. The
     *            APDU consists of the specified apdu_tag, dataByte and
     *            length_field. The APDU format is defined in the
     *            CableCardInterface 2.0 Specification [4].
     *
     */
    public void receiveAPDU(int apduTag, int lengthField, byte[] dataByte);

    /**
     * This is a call back method to notify an error has occurred while sending
     * an APDU via the {@link SystemModule#sendAPDU} method. This method shall
     * return immediately.
     *
     * @param apduTag
     *            an apdu_tag of the APDU that was failed to be sent. This is
     *            the apduTag value specified in the SystemModule.sendAPDU()
     *            method.
     *
     * @param dataByte
     *            an data_byte of the APDU that was failed to be sent. This is
     *            is dataByte value specified in the SystemModule.sendAPDU()
     *            method.
     *
     */
    public void sendAPDUFailed(int apduTag, byte[] dataByte);

    /**
     * This is a call back method to notify that the SystemModuleHandler is
     * being unregistered and give a chance to do a termination procedure. This
     * method returns after the termination procedure has finished.
     */
    public void notifyUnregister();

    /**
     * This is a call back method to notify that this SystemModuleHandler is
     * ready to receive an APDU, and returns a SystemModule to send an APDU to
     * the CableCARD device.
     *
     * @param systemModule
     *            a SystemModule instance corresponding to this
     *            SystemModuleHandler. The returned SystemModule sends an APDU
     *            using the same session that this SystemModuleHandler receives
     *            an APDU. Null is specified, if the OCAP implementation fails
     *            to establish a SAS connection or fails to create an
     *            SystemModule instance due to lack of resource.
     *
     */
    public void ready(SystemModule systemModule);
}
