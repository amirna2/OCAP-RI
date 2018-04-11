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

package org.cablelabs.impl.manager.pod;

import java.util.HashMap;
import java.util.Iterator;

import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.system.APDUReader;
import org.cablelabs.impl.manager.system.APDUReader.APDUReadException;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.session.MPEException;
import org.ocap.hardware.pod.POD;
import org.ocap.hardware.pod.PODApplication;

public class PodManagerImpl implements PODManager
{
    // Log4J Logger
    private static final Logger log = Logger.getLogger(PodManagerImpl.class.getName());

    public static final int MAX_APDU_SIZE = 4096;

    private PodImpl pod = null;
    
    private int m_manufacturerID = -1;
    private int m_version = -1;
    private byte[] m_macAddress = null;
    private byte[] m_serialNum = null;
    private PODApplication[] m_applications = null;

    /**
     * Private Constructor
     * 
     * Use <code>ManagerManager.getInstance(PodManagerImpl.class)<code>
     * to get a reference to the singleton instance of the PodManagerImpl.
     */
    private PodManagerImpl()
    {
        // create a new POD implementation instance
        pod = new PodImpl(this);
    }

    /**
     * Returns the singleton instance of the <code>PodManagerImpl</code>.
     * 
     * Intended to be called by the
     * {@link org.cablelabs.impl.manager.ManagerManager ManagerManager} only.
     * 
     * Invoke <code>ManagerManager.getInstance(PodManagerImpl.class)<code>
     * to get the singleton instance of the PodManagerImpl.
     * Do not call this method directly.
     * 
     * @return the singleton instance of the <code>PodManagerImpl</code>.
     * 
     * @see org.cablelabs.impl.manager.ManagerManager#getInstance(Class)
     */
    public synchronized static Manager getInstance()
    {
        // don't worry about this being a singleton, ManagerManager will manage
        // that issue
        // just return an instance of OcapTestImpl
        return new PodManagerImpl();
    }

    /**
     * Destroys this SoundMgr, causing it to release any and all resources.
     * 
     * This is NOT to be used to destroy a sound player. This method should only
     * be called by the <code>ManagerManager</code> or the finalize method of
     * this class.
     * 
     * Do not call this method directly.
     */
    public synchronized void destroy()
    {
        // forget about the POD implementation instance
        pod = null;
    }

    /**
     * Releases resources
     */
    public void finalize()
    {
        if (log.isDebugEnabled())
        {
            log.debug("finalize - entering");
        }

        destroy();
    }

    /**
     * This method will return an instance of POD.
     * 
     * @return an instance of org.ocap.hardware.pod.POD.
     */
    public POD getPOD()
    {
        return pod;
    }

    /**
     * Returns the CableCARD manufacturer ID as returned by the most
     * recent receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD manufacturer ID or -1 if the POD is not ready
     */
    public synchronized int getManufacturerID()
    {
        return m_manufacturerID;
    }
    
    /**
     * Returns the CableCARD version as returned by the most recent
     * receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD version or -1 if the POD is not ready
     */
    public synchronized int getVersion()
    {
        return m_version;
    }
    
    /**
     * Returns the 6 byte CableCARD MAC address as returned by the most recent
     * receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD MAC address.  If the POD is not ready
     * or if the card does not support at least version 2 of the Application
     * Information Resource, this method returns null
     */
    public synchronized byte[] getMACAddress()
    {
        return m_macAddress;
    }
    
    /**
     * Returns the multi-byte CableCARD serial number as returned by the most
     * recent receipt of the application_info_cnf() APDU
     * 
     * @return the CableCARD serial number.  If the POD is not ready
     * or if the card does not support at least version 2 of the Application
     * Information Resource, this method returns null
     */
    public synchronized byte[] getSerialNumber()
    {
        return m_serialNum;
    }

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
    public synchronized PODApplication[] getPODApplications()
    {
        return m_applications;
    }

    /**
     * Process the given application_info_cnf() APDU and update our internal data
     * cache.  The given APDU data should contain only the APDU data -- NOT the
     * APDU header (tag, length)
     * 
     * @param apdu_data the apdu data
     * @param resourceVersion the Application Information resource version
     */
    public synchronized void processApplicationInfoCnfAPDU(byte[] apdu_data, int resourceVersion)
    {
        APDUReader reader = new APDUReader(apdu_data);
        
        try
        {
            m_manufacturerID = reader.getInt(2);
            m_version = reader.getInt(2);
            
            // Only available in version 2 or higher
            if (resourceVersion >= 2)
            {
                m_macAddress = reader.getBytes(6);
                int serialNumBytes = reader.getInt(1);
                m_serialNum = reader.getBytes(serialNumBytes);
            }
            
            // Parse applications
            int numApps = reader.getInt(1);
            m_applications = new PODApplication[numApps];
            for (int i = 0; i < numApps; i++)
            {
                int type = reader.getInt(1);        // 8-bit type
                int version = reader.getInt(2);     // 16-bit version
                
                int nameLength = reader.getInt(1);  // 8-bit name length
                String name = new String(reader.getBytes(nameLength));
                
                int urlLength = reader.getInt(1);   // 8-bit url length
                String url = new String(reader.getBytes(urlLength));
                
                m_applications[i] = new PODAppImpl(type, version, name, url);
            }
        }
        catch (APDUReadException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("processApplicationInfoCnfAPDU -- error parsing APDU - " + e.getMessage());
            }
        }
    }

    /**
     * <code>isReady</code>
     * 
     * Native method providing the current boot status of the CableCard device.
     * 
     * @return true if the CableCard device is currently bootstrapped.
     */
    public boolean isPODReady()
    {
        return nativeIsPODReady();
    }

    /**
     * Native method providing the inserted status of the CableCard device.
     * 
     * @return true if the CableCard device is currently inserted (but may not
     *         yet be ready).
     */
    public boolean isPODPresent()
    {
        return nativeIsPODPresent();
    }

    /**
     * <code>getAppInfo</code>
     * 
     * Native method for acquiring the list of generic features supported by the
     * CableCard per SCTE28 Table 8.12-B.
     * 
     * @return int[] containing the generic feature IDs supported.
     */
    public int[] getPODHostFeatures()
    {
        return nativeGetPODHostFeatures();
    }

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
    public byte[] getPODHostParam(int featureID)
    {
        return nativeGetPODHostParam(featureID);
    }

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
    public boolean updatePODHostParam(int featureID, byte[] value)
    {
        return nativeUpdatePODHostParam(featureID, value);
    }

    /**
     * <code>initPOD<TYPE></code>
     * 
     * Native method used for initializing POD-host communication
     */
    public void initPOD(Object eventListener)
    {
        nativeInitPOD(eventListener);
    }

    public void addPODListener(PODListener listener)
    {
        pod.addPODListener(listener);
    }

    public void removePODListener(PODListener listener)
    {
        pod.removePODListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public CASession startDecrypt(final CADecryptParams params) throws MPEException
    {
        if (log.isInfoEnabled())
        {
            log.info("startDecrypt(params=" + params + ")");
        }
        
        CASessionImpl newSession = new CASessionImpl(params);
        try
        {
            if (newSession.start() == false)
            {
                newSession = null;
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Caught exception during CASession.start()", e);
            }
            newSession = null;
        }

        return newSession;
    } // END startDecrypt()

    private HashMap m_cciServiceTable = new HashMap();
    
    private class ServiceCCITuple
    {
        private final ServiceDetails m_serviceDetails;
        private final byte m_cci;

        ServiceCCITuple(final ServiceDetails serviceDetails, final byte cci)
        {
            m_serviceDetails = serviceDetails;
            m_cci = cci;
        }
        public ServiceDetails getServiceDetails()
        {
            return m_serviceDetails;
        }
        
        public byte getCCI()
        {
            return m_cci;
        }
        
        public String toString()
        {
            return "ServiceCCITuple[" + m_serviceDetails 
                   + ',' + new CopyControlInfo(m_cci).toString() + ']';
        }
    };
    
    /**
     * {@inheritDoc}
     */
    public void setCCIForService( final Object key, final ServiceDetails serviceDetails, 
                                       final byte cci)
    {
        final ServiceCCITuple newTuple = new ServiceCCITuple(serviceDetails, cci);

        synchronized (m_cciServiceTable)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setCCIForService: Setting " + newTuple + " for " + key);
            }
            m_cciServiceTable.put(key, newTuple);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeCCIForService(final Object key)
    {
        synchronized (m_cciServiceTable)
        {
            if (log.isDebugEnabled())
            {
                log.debug("removeCCIForService: Removing mapping for " + key);
            }
            m_cciServiceTable.remove(key);
        }
    }

    /**
     * {@inheritDoc}
     */
    public CopyControlInfo getCCIForService(final ServiceDetails serviceDetails)
    {
        synchronized (m_cciServiceTable)
        {
            final Iterator cciTupleEnum = m_cciServiceTable.values().iterator();

            while (cciTupleEnum.hasNext())
            {
                final ServiceCCITuple curTuple = (ServiceCCITuple)(cciTupleEnum.next());

                if (serviceDetails.equals(curTuple.getServiceDetails()))
                {
                    final CopyControlInfo cci = new CopyControlInfo(curTuple.getCCI());
                    if (log.isDebugEnabled())
                    {
                        log.debug("getCCIForService: Returning " + cci);
                    }
                    return cci;
                }
            }
        }

        // No match
        if (log.isDebugEnabled())
        {
            log.debug("getCCIForService: No match for " + serviceDetails);
        }
        return null;
    } // END getCCIForService()
    
    // ****************************** Native Methods ******************************

    private static native boolean nativeIsPODReady();

    private static native boolean nativeIsPODPresent();

    private static native int[] nativeGetPODHostFeatures();

    private static native byte[] nativeGetPODHostParam(int featureID);

    private static native boolean nativeUpdatePODHostParam(int featureID, byte[] value);

    private static native void nativeInitPOD(Object eventListener);
    
    private static native int getAppInfoResourceVersion();

    /**
     * Start the decryption process and get back a session handle that
     * represents the native session.
     * 
     * @param params
     *            MediaDecryptParams
     * @param sessionHandle
     *            the session handle will be the first item in the sessionHandle array
     */
    static native int nativeStartDecrypt( final short handleType, 
                                          final int handle, 
                                          final int tunerId,
                                          final EDListener edl,
                                          final int pids[], 
                                          final short types[],
                                          final int priority,
                                          int[] sessionHandle );

    /**
     * Stop the decrypt session represented by the nativeSessionHandle. After
     * this call successfully returns the session is no longer valid.
     * 
     * @param nativeSessionHandle
     * @return <li> {@link MPEException#MPE_SUCCESS} if the stop was successful
     *         <li>An value other then {@link MPEException#MPE_SUCCESS} if there
     *         was a MPE level error
     */
    static native int nativeStopDecrypt(final int sessionHandle);

    static native int nativeGetDecryptStreamAuthorizations(int sessionHandle, CAElementaryStreamAuthorization[] elementaryStreamAuthorizations);
    
    /** Initialize the JNI code */
    private static native void nInit();

    // Initialize JNI layer.
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);
        nInit();
    }
}
