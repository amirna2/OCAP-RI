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

package org.cablelabs.impl.manager.recording;

import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingPermission;

import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.RecordingManager;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.recording.*;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;
import org.ocap.shared.dvr.*;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Implements functionality common to both LeafRecordingRequests
 * (OcapRecordingRequests) and ParentRecordingRequests
 * 
 * @author FSmith
 */
public abstract class RecordingRequestImpl implements RecordingRequest, Asserting
{
    /**
     * Any recording which has been deleted from the system will be assigned to
     * the DESTROYED_STATE for its externally visible state. And method called
     * on a recording in the DESTROYED_STATE will throw an IllegalStateException
     * as described by the RecordingManager.delete method
     */
    static final int DESTROYED_STATE = LeafRecordingRequest.FAILED_STATE + 0x1000;

    /**
     * Any newly initialized recording (unscheduled) will be in the INIT_STATE
     */
    static final int INIT_STATE = DESTROYED_STATE + 1;

    /**
     * Retrieve the persistent storage class for this recording request. This
     * may be a RecordingInfo for leaf recordings, or a RecordingInfoTree for
     * parent recordings
     * 
     * @return the RecordingInfoNode for this recording
     */
    abstract RecordingInfoNode getRecordingInfoNode();

    /**
     * Returns <code>true</code> if the application identified by AppID id has
     * read access to this recording request.
     * 
     * @param id
     *            AppID of the requesting application
     * 
     * @return <code>true</code> if the caller has read access to the recording
     *         request; otherwise returns <code>false</code>.
     */
    boolean hasReadAccess(AppID id)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        RecordingInfoNode info = getRecordingInfoNode();
        final OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
        return osm.hasReadAccess(info.getAppId(), info.getFap(), id, OcapSecurityManager.FILE_PERMS_RECORDING);
    }

    /*
     * Returns <code>true</code> if the application identified by AppID id has
     * write access to this recording request.
     * 
     * @param id AppID of the requesting application
     * 
     * @return <code>true</code> if the caller has write access to the recording
     * request; otherwise returns <code>false</code>.
     */
    boolean hasWriteAccess(AppID id)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        RecordingInfoNode info = getRecordingInfoNode();
        final OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
        return osm.hasWriteAccess(info.getAppId(), info.getFap(), id, OcapSecurityManager.FILE_PERMS_RECORDING);
    }

    /**
     * Checks that the caller has read ExtendedFileAccessPermissions.
     * 
     * @throws AccessDeniedException
     *             if the caller does not have read access.
     */
    void checkReadExtFileAccPerm() throws AccessDeniedException
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        if (SecurityUtil.isPrivilegedCaller())
        {
            return;
        }

        if (SecurityUtil.hasPermission(new RecordingPermission("*", "*")))
        { // Per OCAP DVR 7.2.1.4.2: RecordingPermission("*", "*") - create, read, modify, delete or cancel 
          //  any RecordingRequest or RecordedService, regardless of any restrictions specified through the 
          //  extended file access permission associated with the RecordingRequest.
            return;
        }
        
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if (null != ccm)
        {
            boolean isReadable = hasReadAccess((AppID) (ccm.getCurrentContext().get(CallerContext.APP_ID)));

            if (isReadable)
            {
                return;
            }
            else
            {
                // is not readable, so should be denied
                throw new AccessDeniedException();
            }
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("Failed to obtain CallerContextManager (" + ccm
                    + ") Object ref"));
            throw new AccessDeniedException(); // no detail message!!!
        }
    }

    /**
     * Checks that the caller has write ExtendedFileAccessPermissions.
     * 
     * @throws AccessDeniedException
     *             if the caller does not have read access.
     */
    public void checkWriteExtFileAccPerm() throws AccessDeniedException
    {
        // Internal method - caller should hold the lock
        // Assert: We're running in the caller context (not in System Context)
        
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        if (SecurityUtil.isPrivilegedCaller())
        {
            return;
        }
        
        if (SecurityUtil.hasPermission(new RecordingPermission("*", "*")))
        { // Per OCAP DVR 7.2.1.4.2: RecordingPermission("*", "*") - create, read, modify, delete or cancel 
          //  any RecordingRequest or RecordedService, regardless of any restrictions specified through the 
          //  extended file access permission associated with the RecordingRequest.
            return;
        }
        
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        if (null != ccm)
        {
            final AppID appid = (AppID) (ccm.getCurrentContext().get(CallerContext.APP_ID));
            boolean isReadable = hasReadAccess(appid);
            boolean isWritable = hasWriteAccess(appid);

            if (isReadable && isWritable)
            {
                return;
            }
            else
            {
                // is not both readable and writable, so access is denied
                throw new AccessDeniedException();
            }
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("Failed to obtain CallerContextManager (" + ccm
                    + ") Object ref"));
            throw new AccessDeniedException(); // no detail message!!!
        }
    }

    /**
     * Gets the application identifier of the application that owns this
     * recording request. The owner of a root recording request is the
     * application that called the RecordingManager.record(..) method. The owner
     * of a non-root recording request is the owner of the root for the
     * recording request.
     * 
     * @return Application identifier of the owning application.
     */
    public AppID getAppID()
    {
        synchronized (m_sync)
        {
            return getRecordingInfoNode().getAppId();
        }
    }

    /**
     * Get all Application specific data associated with this recording request.
     * 
     * @return All keys corresponding to the RecordingRequest; Null if there if
     *         no application data.
     */
    public String[] getKeys()
    {
        synchronized (m_sync)
        {
            RecordingInfoNode info = getRecordingInfoNode();
    
            // find owners appID (bug 4042 modification)
            AppID appID = info.getAppId();
    
            // Obtain the data table for this execution context.
            Hashtable dataTable = (Hashtable) info.getAppDataTable().get(appID.toString());
    
            if (dataTable == null)
            {
                return null;
            }
    
            int cnt = dataTable.size();
            if (cnt == 0)
            {
                return null;
            }
    
            String output[] = new String[cnt];
            cnt--;
            // Returns an enumeration of the keys in this hashtable.
            Enumeration enu = dataTable.keys();
            while (enu.hasMoreElements())
            {
                output[cnt--] = (String) enu.nextElement();
            }
            return output;
        } // END synchronized (m_sync)
    } // END getKeys()

    /**
     * Get application data corresponding to specified key.
     * 
     * @param key
     *            the key under which any data is to be returned
     * 
     * @return the application data corresponding to the specified key; Null if
     *         there if no data corresponding to the specified key.
     */
    public java.io.Serializable getAppData(String key)
    {
        synchronized (m_sync)
        {
            if (key == null)
            {
                throw new IllegalArgumentException("Detected null key parameter");
            }
    
            SecurityUtil.checkPermission(new RecordingPermission("read", "own"));
    
            // find owners appID (bug 4042 modification)
            AppID appID = getRecordingInfoNode().getAppId();
    
            Hashtable dataTable = (Hashtable) getRecordingInfoNode().getAppDataTable().get(appID.toString());
    
            // Look up the appData
            AppDataContainer appData;
            if (dataTable != null && (appData = (AppDataContainer) dataTable.get(key)) != null)
            {
                ApplicationManager am = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
                try
                {
                    // Unwrap the appData
                    return appData.getObject(am.getAppClassLoader(null));
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("AppData not accessible " + appID + " " + key, e);
                    }
            }
            }
            // Return null if there is no appData
            return null;
        } // END synchronized (m_sync)
    } // END getAppData()

    /**
     * Remove Application specific private data corresponding to the specified
     * key. This method exits silently if there was no data corresponding to the
     * key.
     * 
     * @param key
     *            the key under which data is to be removed
     * 
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     */
    public void removeAppData(String key) throws AccessDeniedException
    {
        synchronized (m_sync)
        {
            RecordingInfoNode info = getRecordingInfoNode();
            SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
            checkWriteExtFileAccPerm();
            if (key == null)
            {
                throw new IllegalArgumentException("Detected null key parameter");
            }
    
            if (info.getState() == RecordingRequestImpl.DESTROYED_STATE) throw new IllegalStateException();
    
            // find owners appID (bug 4042 modification)
            AppID appID = info.getAppId();
    
            Hashtable dataTable = (Hashtable) info.getAppDataTable().get(appID.toString());
            if (dataTable != null)
            {
                Integer currentSizeObject = (Integer) info.getUsedAppDataBytes().get(appID.toString());
                AppDataContainer oldData = (AppDataContainer) dataTable.get(key);
                if (currentSizeObject != null && oldData != null)
                {
                    int removedDataSize = oldData.getSize();
                    int newSum = currentSizeObject.intValue() - removedDataSize;
                    info.getUsedAppDataBytes().put(appID.toString(), new Integer(newSum));
                    dataTable.remove(key);
                }
            }
            saveRecordingInfo(RecordingDBManager.APP_DATA_TABLE);
        } // END synchronized (m_sync)
    } // END removeAppData()

    /**
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getId()
     */
    public int getId()
    {
        synchronized (m_sync)
        {
            return getRecordingInfoNode().getUniqueIDInt();
        }
    }

    /**
     * update the persistent metadata for this recording
     */
    void saveRecordingInfo(int updateFlag)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        try
        {
            m_rdbm.saveRecord(getRecordingInfoNode(), updateFlag);
        }
        catch (Exception e)
        {
            // TODO: How do we handle a failed persistant write?
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Add application specific private data. If the key is already in use, the
     * data corresponding to key is overwritten.
     * 
     * @param key
     *            the ID under which the data is to be added
     * @param data
     *            the data to be added
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     * 
     * @throws IllegalArgumentException
     *             if the size of the data is more than the size supported by
     *             the implementation. The implementation shall support at least
     *             256 bytes of data.
     * @throws NoMoreDataEntriesException
     *             if the recording request is unable to store any more
     *             Application data. The implementation shall support atleast 16
     *             data entries per recording request.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public void addAppData(String key, java.io.Serializable data) throws NoMoreDataEntriesException,
            AccessDeniedException
    {
        synchronized (m_sync)
        {
            RecordingInfoNode info = getRecordingInfoNode();
            if (key == null)
            {
                throw new IllegalArgumentException("Null key detected.");
            }
    
            if (data == null)
            {
                throw new IllegalArgumentException("Data param must implement Serializable");
            }
    
            SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
            checkWriteExtFileAccPerm();
    
            Hashtable dataTable;
    
            // Create wrapper container for AppData
            AppDataContainer appData = null;
            try
            {
                appData = new AppDataContainer(data);
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Unable to serialize appData", e);
                }
                // If object could not be serialized, it's likely the app's fault
                // TODO: what would be a better exception here?
                throw new IllegalArgumentException("Unable to serialize appData: " + e.getMessage());
            }
    
            int sizeOfNewData = appData.getSize();
    
            // find owners appID (bug 4042 modification)
            AppID callerAid = info.getAppId();
    
            // Obtain the data table for this caller's execution context from the
            // map <execution context, dataTable>.
            dataTable = (Hashtable) info.getAppDataTable().get(callerAid.toString());
            if (dataTable == null)
            {
                // First time use? Add Xlet context to map
                dataTable = new Hashtable();
                info.getAppDataTable().put(callerAid.toString(), dataTable);
                info.getUsedAppDataBytes().put(callerAid.toString(), new Integer(0));
            }
    
            Integer currentSizeObject = (Integer) info.getUsedAppDataBytes().get(callerAid.toString());
            int storedSum = currentSizeObject != null ? currentSizeObject.intValue() : 0;
            int newSum = sizeOfNewData + storedSum;
    
            AppDataContainer oldData = (AppDataContainer) dataTable.get(key);
    
            // Replace old data if it exist.
            if (oldData != null)
            {
                int sizeOfOldData = oldData.getSize();
                newSum = newSum - sizeOfOldData;
                if ((newSum - sizeOfOldData) > 16384)
                {
                    throw new IllegalArgumentException("Exceeds data size limit");
                }
                dataTable.remove(key);
            }
            else
            {
                if (newSum > 16384)
                {
                    throw new IllegalArgumentException("Exceeds data size limit");
                }
            }
    
            // Map the data and commit to disk.
            if (dataTable.size() < MAX_APPDATA_ENTRIES)
            {
                info.getUsedAppDataBytes().put(callerAid.toString(), new Integer(newSum));
                dataTable.put(key, appData);
                saveRecordingInfo(RecordingDBManager.APP_DATA_TABLE);
            }
            else
            {
                throw new NoMoreDataEntriesException();
            }
        } // END synchronized (m_sync)
    } // END addAppData()

    /**
     * Returns the state of the recording request.
     * 
     * @return State of the recording request.
     */
    public int getState()
    {
        synchronized (m_sync)
        {
            int state = getRecordingInfoNode().getState();
            if (state == RecordingRequestImpl.INIT_STATE)
            {
                state = LeafRecordingRequest.PENDING_NO_CONFLICT_STATE;
            }
            if (state == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }
            return state;
        }
    }

    /**
     * retrieve the internal state of this recording impl
     */
    public int getInternalState()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        return getRecordingInfoNode().getState();
    }

    /**
     * Creates a default extended file access permissions when none is specified
     * when creating a recording.
     * 
     * @return the default eFAP (as in ECN 1325)
     */
    ExtendedFileAccessPermissions getDefaultFAP()
    {
        ExtendedFileAccessPermissions eFAP = new ExtendedFileAccessPermissions(false, false, // no
                                                                                             // world
                                                                                             // permissions
                false, false, // no org permissions
                true, true, // only app permissions
                null, null); // no other orgs
        return eFAP;
    }

    static final int MAX_APPDATA_ENTRIES = 64;

    protected Object m_sync = null;

    // reference to the RecordingManager
    protected RecordingManagerInterface m_recordingManager = null;

    // reference to the recording database manager
    protected RecordingDBManager m_rdbm = null;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(RecordingRequestImpl.class.getName());
}
