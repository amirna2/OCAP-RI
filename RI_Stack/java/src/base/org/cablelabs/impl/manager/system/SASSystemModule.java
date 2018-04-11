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

package org.cablelabs.impl.manager.system;

import org.ocap.system.SystemModuleHandler;

/**
 * SasSystemModule
 */
public class SASSystemModule extends org.cablelabs.impl.manager.system.SystemModule
{
    /**
     * Constructor for the SAS_SystemModule object. This constructor is used
     * when a new SAS connection is being made.
     * 
     * @param sessionId
     *            is the potentially pre-existing session identifier. If a new
     *            handler is replacing another, the session connection was
     *            already made. (-1) will be passed in if the session does not
     *            yet exist.
     * @param handler
     *            is the associated SysteModuleHandler.
     */
    public SASSystemModule(int sessionId, SystemModuleHandler handler)
    {
        super(validTags);
        this.sessionId = sessionId;
        this.handler = handler;
    }

    /**
     * <p>
     * This method sends an APDU to the CableCARD device.
     * </p>
     * <p>
     * For the Private Host Application of the SAS Resource, the session number
     * for sending the APDU is decided by the OCAP implementation automatically
     * when registration via the
     * {@link SystemModuleRegistrar#registerSASHandler} method. Sending APDU is
     * delegated to the SAS Resource.
     * </p>
     * <p>
     * For the MMI Resource and Application Information Resource, sending APDU
     * is delegated to the resident MMI and Application Information Resources.
     * The OCAP-J application can send APDUs of either MMI Resource or
     * Application Information Resource via a single SystemModule. The OCAP
     * implementation shall investigate the apdu_tag field in the APDU and send
     * the APDU to the CableCARD device using the session of the Resource
     * specified by the apdu_tag. The session established by the resident MMI
     * and Application Information Resource are used to send the APDU. If the
     * apdu_tag indicates other Resource except MMI Resource or Application
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
     * specified dataByte parameter, but shall cinfirm the validity of the
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
    public void sendAPDU(int apduTag, byte[] apdu)
    {
        // Validate the specified tag.
        if (isValidTag(apduTag) == false)
            throw new IllegalArgumentException("Invalid APDU tag specified");
        else
        {
            // TODO: check for connection request - ignore it and send a
            // sas_connect_cnf()
            // to the associated handler.

            // Make sure a data request isn't passing an APUD data buffer.
            if ((SAS_DATA_RQST == apduTag) && (null != apdu)) apdu = null;

            // Perform the native send.
            if (podSASSendAPDU(sessionId, apduTag, apdu) == false) handler.sendAPDUFailed(apduTag, apdu); // Call
                                                                                                          // handler
                                                                                                          // on
                                                                                                          // failure.
        }
    }

    /**
     * connect()
     * 
     * This method is called by the SystemModuleRegistrarImpl to establish an
     * SAS connection whenever an application registers an SAS handler. There is
     * no attempt made on the java side to maintain open connections since it's
     * really not clear all native platform will keep them open per the OCAP
     * spec. Hence, it's the native support layer's responsibility to maintain
     * the sessions for per private host application identifier.
     * 
     * @param privateHostAppID
     *            is the identifier of the session to establish.
     * 
     * @return the session handle for the SAS session to use.
     */
    public int connect(byte[] privateHostAppID)
    {
        return (sessionId = podSASConnect(privateHostAppID));
    }

    /**
     * Native method for sending the APDU to the POD
     */
    private native boolean podSASSendAPDU(int sessionId, int apduTag, byte[] apdu);

    /**
     * Native method for establishing an SAS session with the POD.
     */
    public native int podSASConnect(byte[] privateHostAppID);

    // APDU tag for SAS data request.
    private static final int SAS_DATA_RQST = 0x009F9A02;

    // List of valid MMI APDU tags.
    private static int[] validTags = { 0x009F9A00, 0x009F9A01, 0x009F9A02, 0x009F9A03, 0x009F9A04, 0x009F9A05,
            0x009F9A06, 0x009F9A07 };

    // The associated SystemModuleHandler.
    private SystemModuleHandler handler = null;

    // The native session handle.
    private int sessionId = 0;

    static
    {
        // insure that MPE/JNI native library is loaded
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
