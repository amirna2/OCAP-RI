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

package org.cablelabs.impl.manager;

import javax.tv.service.navigation.ServiceDetails;

import org.ocap.hardware.pod.POD;
import org.ocap.hardware.pod.PODApplication;

import org.cablelabs.impl.manager.pod.CADecryptParams;
import org.cablelabs.impl.manager.pod.CASession;
import org.cablelabs.impl.manager.pod.CASessionListener;
import org.cablelabs.impl.manager.pod.PODListener;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.session.MPEException;

/**
 * A <code>Manager</code> that is responsible for initializing the POD subsystem
 * as well as various other POD-related responsibilities.
 * <p>
 * The <code>PODManager</code> implementation currently does very little.
 * However, methods may be added in the future.
 */
public interface PODManager extends Manager
{
    /**
     * This method will return an instance of POD.
     * 
     * @return an instance of org.ocap.hardware.pod.POD.
     */
    POD getPOD();
    
    /**
     * Returns the CableCARD manufacturer ID as returned by the most
     * recent receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD manufacturer ID or -1 if the POD is not ready
     */
    int getManufacturerID();
    
    /**
     * Returns the CableCARD version as returned by the most recent
     * receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD version or -1 if the POD is not ready
     */
    int getVersion();
    
    /**
     * Returns the 6 byte CableCARD MAC address as returned by the most recent
     * receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD MAC address.  If the POD is not ready
     * or if the card does not support at least version 2 of the Application
     * Information Resource, this method returns null
     */
    byte[] getMACAddress();
    
    /**
     * Returns the multi-byte CableCARD serial number as returned by the most
     * recent receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD serial number.  If the POD is not ready
     * or if the card does not support at least version 2 of the Application
     * Information Resource, this method returns null
     */
    byte[] getSerialNumber();

    /**
     * <code>getPODApplications</code>
     * 
     * Acquires the applications supported by the CableCard device.  As indicated
     * by the most recently received application_info_cnf APDU.  NOTE: The currently
     * installed MMI handler is responsible for sending the application_info_req() APDU
     * 
     * @return the POD applications or null if the application_info_cnf() APDU has
     *         not yet been received
     */
    PODApplication[] getPODApplications();
    
    /**
     * Process the given application_info_cnf() APDU and update our internal data
     * cache.  The given APDU data should contain only the APDU data -- NOT the
     * APDU header (tag, length)
     * 
     * @param apdu_data the apdu data
     * @param resourceVersion the Application Information resource version
     */
    void processApplicationInfoCnfAPDU(byte[] apdu_data, int resourceVersion);

    /**
     * <code>isReady</code>
     * 
     * Native method providing the current boot status of the CableCard device.
     * 
     * @return true if the CableCard device is currently bootstrapped.
     */
    boolean isPODReady();

    /**
     * Native method providing the inserted status of the CableCard device.
     * 
     * @return true if the CableCard device is currently inserted (but may not
     *         yet be ready).
     */
    boolean isPODPresent();

    /**
     * <code>getAppInfo</code>
     * 
     * Native method for acquiring the list of generic features supported by the
     * CableCard per SCTE28 Table 8.12-B.
     * 
     * @return int[] containing the generic feature IDs supported.
     */
    int[] getPODHostFeatures();

    /**
     * <code>getPODHostParam<TYPE></code>
     * 
     * Native methods used for acquiring the feature parameter values. There are
     * three different native methods available for each possible value type:
     * integer, byte array and integer array.
     * 
     * Note the boolean types use the integer interface with a value of 0 or 1
     * indicating true or false. This convention helps reduce the number of
     * native methods required for acquiring the feature values.
     * 
     * @param featureID
     *            is the generic feature ID per SCTE28 Table 8.12-B
     */
    byte[] getPODHostParam(int featureID);

    /**
     * <code>updatePODHostParam<TYPE></code>
     * 
     * Native method used for updating the feature parameter values. There are
     * three different native methods available for each possible value type:
     * integer, byte array and integer array.
     * 
     * Note the boolean types use the integer interface with a value of 0 or 1
     * indicating true or false. And the non-value types (e.g. reset_p_c_pin)
     * also use the integer interface with a value of 0. This convention helps
     * reduce the number of native methods required for support of the feature
     * value updates.
     * 
     * @param featureID
     *            is the generic feature ID per SCTE28 Table 8.12-B
     * @param value
     *            is a byte array containing the new value of the associated
     *            feature.
     */
    boolean updatePODHostParam(int featureID, byte[] value);

    /**
     * <code>initPOD<TYPE></code>
     * 
     * Native method used for initializing POD-host communication
     */
    void initPOD(Object eventListener);

    /**
     * Add a PODListener
     */
    void addPODListener(PODListener listener);

    /**
     * Remove a PODListener
     */
    void removePODListener(PODListener listener);

    /**
     * Start a decryption session with the specified parameters. If no decryption
     * session is required for the given Service, this will return null. 
     * 
     * Note that this method may block until the initial CA status is 
     * determined - which can be established by calling CASession.getLastEvent().
     * 
     * @param params
     *            MediaDecryptParams for the conditional access session
     * @return {@link CASession} object.
     * @throws MPEException
     *             If MPE reports and error
     */
    public CASession startDecrypt(final CADecryptParams params) 
        throws MPEException;
    
    /**
     * Set the current CCI value for the given Service. This should be called every time
     *  a CCI indication is received from the platform on any session that originates 
     *  encrypted data from a tuner/POD. 
     *  
     *  Note: This facility is present to support org.ocap.hardware.CopyControl
     *  
     * @param key Identifies the originator of the CCI Service/CCI pairing
     * @param serviceDetails The Service object the CCI update apples to
     * @param cci The CCI value currently applicable
     */
    public void setCCIForService(final Object key, final ServiceDetails serviceDetails, final byte cci);
    
    /**
     * Remove the CCI value previously set via setCCIForService(). This should be called 
     *  when a session originating CCI values is terminated. 
     *  
     * Note: This facility is present to support org.ocap.hardware.CopyControl
     *  
     * @param key Identifies the originator of the CCI Service/CCI pairing
     */
    public void removeCCIForService(final Object key);
    
    /**
     * Return the CCI value active for the given Service or null if the Service is not
     * currently being accessed.
     * 
     * @param serviceDetails The Service to query CCI bits for
     * 
     * @return the CCI value active for the given Service or null if the Service is not
     * currently being accessed.
     */
    public CopyControlInfo getCCIForService(final ServiceDetails serviceDetails);
}
