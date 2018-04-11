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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.recording.AppDataContainer;
import org.cablelabs.impl.recording.RecordingInfoNode;
import org.cablelabs.impl.recording.RecordingInfoTree;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;

/**
 * Implements org.ocap.share.dvr.ParentRecordingRequest
 * 
 * @see <code>org.ocap.shared.dvr.ParentRecordingRequest</code>
 * @author jspruiel
 */
public class ParentNodeImpl extends RecordingRequestImpl implements ParentRecordingRequest
{
    /**
     * Constructor
     * 
     * @param source
     * @param rdbm
     */
    ParentNodeImpl(RecordingRequest root, RecordingRequest parent, PrivateRecordingSpec source,
            RecordingDBManager rdbm, RecordingManagerInterface recordingManager)
    {
        // store persistent parameters for parent
        // Work around for absence of updated persistent requirements.
        m_sync = recordingManager;
        m_recordingManager = recordingManager;
        m_rdbm = rdbm;
        m_infoTree = m_rdbm.newRecordTree();

        // if request is null, then it is root.
        // otherwise it is not root and request is stored.
        if (root == null)
        { // if null root param, then I am root
            // therefore have not parent.
            m_parent = null;
            m_root = this;
            m_isRoot = true;
        }
        else
        {
            // if root is someone else,
            // then this recording has a parent.
            m_root = root;
            m_isRoot = false;
            m_parent = (ParentNodeImpl) parent;
        }

        m_infoTree.setState(ParentRecordingRequest.UNRESOLVED_STATE);
        m_recordingSpec = source;
        m_recordingProperties = source.getProperties();

        // Set private data
        m_infoTree.setAppPrivateData(source.getPrivateData());
        
        // set recording properties
        m_infoTree.setExpirationDate(new Date(m_recordingProperties.getExpirationPeriod() * 1000L));
        if (source.getProperties() instanceof OcapRecordingProperties)
        {
            OcapRecordingProperties orp = (OcapRecordingProperties) m_recordingProperties;
            MediaStorageVolume destination = orp.getDestination();
            m_infoTree.setFap(orp.getAccessPermissions() == null ? getDefaultFAP() : orp.getAccessPermissions());
            m_infoTree.setBitRate(orp.getBitRate());
            m_infoTree.setOrganization(orp.getOrganization());
            m_infoTree.setPriority(orp.getPriorityFlag());
            m_infoTree.setRetentionPriority(orp.getRetentionPriority());
            m_infoTree.setDestination(destination);
        }
        else
        {
            m_infoTree.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
            m_infoTree.setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
            m_infoTree.setRetentionPriority(OcapRecordingProperties.DELETE_AT_EXPIRATION);
            m_infoTree.setFap(getDefaultFAP());
            m_infoTree.setOrganization(null);
            m_infoTree.setDestination(null);// TODO
        }

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        m_infoTree.setAppId((AppID) ccm.getCurrentContext().get(CallerContext.APP_ID));

        saveRecordingInfo(RecordingDBManager.ALL);
    }

    /**
     * Caller: ParentNodeImpl(infoTree, rdbm)
     * 
     * CTOR creates children and inserts each child into the NavigationManager.
     * 
     * @param parent
     * @param infoTree
     * @param rdbm
     * @param sync
     */
    ParentNodeImpl(RecordingInfoTree infoTree, RecordingDBManager rdbm, RecordingManagerInterface recordingManager)
    {
        // set fields from persistent record
        // set root
        // set rec DB
        m_sync = recordingManager;
        m_recordingManager = recordingManager;
        m_infoTree = infoTree;
        m_rdbm = rdbm;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.ParentRecordingRequest#cancel()
     */
    public void cancel() throws IllegalStateException, AccessDeniedException
    {
        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug("parent = " + this);
            }

            if (m_infoTree.getState() == ParentRecordingRequest.CANCELLED_STATE
                    || m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            RecordingRequest request = null;

            m_cancel_in_progress = true;

            // Call cancel on each child.
            Vector ch = (Vector) m_myChildren.clone();
            for (Enumeration e = ch.elements(); e.hasMoreElements();)
            {
                request = (RecordingRequest) e.nextElement();
                if (request instanceof LeafRecordingRequest)
                {
                    try
                    {
                        LeafRecordingRequest rr = (LeafRecordingRequest) request;
                        int state = rr.getState();

                        // Skip if the child is not cancellable.
                        if (state == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE
                                || state == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                        {
                            rr.cancel();
                        }
                    }
                    catch (Exception ex)
                    {
                        // Should never occur because we checked that the
                        // request is cancellable.
                        SystemEventUtil.logRecoverableError(ex);
                    }
                }
                else
                {
                    try
                    {
                        ParentRecordingRequest rr = (ParentRecordingRequest) request;
                        int state = rr.getState();

                        // Skip if the child is not cancellable.
                        if (state != ParentRecordingRequest.CANCELLED_STATE)
                        {
                            rr.cancel();
                        }
                    }
                    catch (Exception ex)
                    {
                        // Should never occur because we checked that the
                        // request is cancellable.
                        SystemEventUtil.logRecoverableError(ex);
                    }
                }
            }

            m_cancel_in_progress = false;

            // If all children were not cancelled set state to CANCELLED_STATE.
            // If this method was called by the parent, do not notify the parent
            // that our state changed;
            // the reason is the parent doesn't need notification, it check if
            // all children were deleted
            // after execution returns.

            // Changes per ECR (WS32 DVR Content Deletion Policy)
            int oldState = m_infoTree.getState();
            m_infoTree.setState(ParentRecordingRequest.CANCELLED_STATE);
            NavigationManager.getInstance().updateRecording(this, ParentRecordingRequest.CANCELLED_STATE, oldState);
        }
    }

    public RecordingList getKnownChildren() throws IllegalStateException
    {
        synchronized (m_sync)
        {
            if (m_infoTree.getState() == ParentRecordingRequest.UNRESOLVED_STATE
                    || m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            return new RecordingListImpl(m_myChildren);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#isRoot()
     */
    public boolean isRoot()
    {
        synchronized (m_sync)
        {
            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            return m_isRoot;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getRoot()
     */
    public RecordingRequest getRoot()
    {
        synchronized (m_sync)
        {

            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }
            return m_root;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getParent()
     */
    public RecordingRequest getParent()
    {
        synchronized (m_sync)
        {
            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            return m_parent;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getRecordingSpec()
     */
    public RecordingSpec getRecordingSpec()
    {
        synchronized (m_sync)
        {
            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            // Per ECN 1325 - regardless of what was passed in, this
            // must be an OcapRecordingProperties, and only the
            // access and organization parameters are relevant,
            // so, pick random valid values for other fields.
            OcapRecordingProperties orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, 0, 127,
                    (byte) OcapRecordingProperties.RECORD_WITH_CONFLICTS, m_infoTree.getFap(),
                    m_infoTree.getOrganization(), null);

            PrivateRecordingSpec prs = new PrivateRecordingSpec(m_infoTree.getAppPrivateData(), orp);
            return prs;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.shared.dvr.RecordingRequest#setRecordingProperties(org.ocap.
     * shared.dvr.RecordingProperties)
     */
    public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
            AccessDeniedException
    {
        SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));

        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();
            
            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            m_infoTree.setExpirationDate(new Date(properties.getExpirationPeriod()));

            // At best update the OcapRecordingProperties
            if (properties instanceof OcapRecordingProperties)
            {
                OcapRecordingProperties orp = (OcapRecordingProperties) properties;

                // TODO: How do we persitently store a recording destination?
                MediaStorageVolume destination = orp.getDestination();
                m_infoTree.setBitRate(orp.getBitRate());
                m_infoTree.setOrganization(orp.getOrganization());
                m_infoTree.setPriority(orp.getPriorityFlag());
                m_infoTree.setFap(orp.getAccessPermissions() == null ? getDefaultFAP() : orp.getAccessPermissions());
                m_infoTree.setDestination(destination);

            }
            saveRecordingInfo(RecordingDBManager.ALL);
            m_recordingProperties = properties;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#delete()
     */
    public void delete() throws AccessDeniedException
    {
        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();

            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            RecordingRequest req = null;

            if (log.isInfoEnabled())
            {
                log.info("state: " + this.getState() + "deleting: " + this);
            }

            // Call delete on each child.
            Vector ch = (Vector) m_myChildren.clone();
            for (Enumeration e = ch.elements(); e.hasMoreElements();)
            {
                req = (RecordingRequest) e.nextElement();

                // Requests in the Destroyed state should not exist
                // in my list; any other state is valid for rr.delete().
                // Deletion is guaranteed.
                if (req instanceof LeafRecordingRequest)
                {
                    try
                    {
                        req.delete();
                    }
                    catch (Exception ex)
                    {
                        SystemEventUtil.logRecoverableError(ex);

                    }
                }
                else
                {
                    try
                    {
                        req.delete();
                    }
                    catch (Exception ex)
                    {
                        SystemEventUtil.logRecoverableError(ex);
                    }
                }
            }
            // If my parent is cancelled and I am in the cancelled state,
            // and later delete is called on me, I need to notify my
            // parent because he is waiting for me to become deleted.
            int oldState = m_infoTree.getState();

            // delete me from the disk.
            m_rdbm.deleteRecord(m_infoTree);

            if (isRoot())
            {
                // notify parent I was deleted in case I was called by the app.
                m_infoTree.setState(RecordingRequestImpl.DESTROYED_STATE);

                NavigationManager.getInstance().removeRecording(this, oldState, oldState);
            }
            else
            {
                // no parent to notify.
                m_infoTree.setState(RecordingRequestImpl.DESTROYED_STATE);
                m_parent.notifyRemoveChild(this, oldState, oldState);
            }
        }
    }

    /**
     * Called by a child of this parent instance when the child is deleted.
     */
    void notifyRemoveChild(RecordingRequest child, int nstate, int ostate)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Entered notifyRemoveChild");
        }

        // Release all references of child.
        forgetChild(child, nstate, ostate);

        // If true, wait until I am done calling cancel.
        // Cancel may or may not result in a delete.
        //
        // If false, then this is just a notification.
        //
        if (m_cancel_in_progress)
        {
            if (log.isDebugEnabled())
            {
                log.debug("notifyDelete cancel_in_prog = true");
            }
            return;
        }

        if (m_infoTree.getState() == ParentRecordingRequest.COMPLETELY_RESOLVED_STATE)
        {
            m_infoTree.setState(ParentRecordingRequest.PARTIALLY_RESOLVED_STATE);
        }

        // If I am in the CANCELLED_STATE I need to delete myself iff
        // all children are deleted.
        if (m_infoTree.getState() == ParentRecordingRequest.CANCELLED_STATE)
        {
            // I still have children so there is no state change.
            if (m_myChildren.size() != 0)
                return;

            // Now I can finally delete myself.
            // Delete the persistent record.
            m_rdbm.deleteRecord(this.m_infoTree, true);
            if (isRoot())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("removing me");
                }
                // notify nav myself
                NavigationManager.getInstance().removeRecording(this, m_infoTree.getState(), m_infoTree.getState());
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("calling notifyDelete on parent");
                }

                // Let my parents know I've been deleted.
                m_parent.notifyRemoveChild(this, m_infoTree.getState(), m_infoTree.getState());
            }
        }
    }

    /**
     * Called by a child of this parent instance when the child is deleted.
     */
    void notifyPurge(RecordingRequest child)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Entered notifyPurge");
        }

        // Release all references of child.
        m_myChildren.remove(child);

        if (child instanceof LeafRecordingRequest)
        {
            // Second, remove the child's m_info persistent structure
            // from my list of persistent children.
            m_infoTree.children.remove(((RecordingImpl) child).m_info);
        }
        else
        {
            // Second, remove the child's m_info persistent structure
            // from my list of persistent child objects.
            m_infoTree.children.remove(((ParentNodeImpl) child).m_infoTree);
            NavigationManager.getInstance().removeRecording(child, ParentRecordingRequest.COMPLETELY_RESOLVED_STATE,
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE);
        }

        // If I am in the CANCELLED_STATE I need to delete myself iff
        // all children are deleted.
        if ((m_infoTree.getState() == ParentRecordingRequest.COMPLETELY_RESOLVED_STATE) && m_myChildren.size() == 0)
        {
            // Delete the persistent record.
            m_rdbm.deleteRecord(this.m_infoTree, true);
            if (isRoot())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("removing me");
                }
                // notify nav myself
                NavigationManager.getInstance().removeRecording(this, ParentRecordingRequest.COMPLETELY_RESOLVED_STATE,
                        ParentRecordingRequest.COMPLETELY_RESOLVED_STATE);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("Calling notifyPurge on parent");
                }

                // Let my parents no I've been purged.
                m_parent.notifyPurge(this);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#getKeys()
     */
    public String[] getKeys()
    {
        synchronized (m_sync)
        {

            Hashtable dataTable = null;
            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            // find owners appID (bug 4042 modification)
            AppID appID = m_infoTree.getAppId();

            // Obtain the data table for this execution context.
            dataTable = (Hashtable) m_infoTree.getAppDataTable().get(appID.toString());

            if (dataTable == null)
            {
                return null;
            }

            int cnt = dataTable.size();
            if (cnt == 0)
            {
                return null;
            }

            String[] output = new String[cnt];
            cnt--;
            // Returns an enumeration of the keys in this hashtable.
            Enumeration enu = dataTable.keys();
            while (enu.hasMoreElements())
            {
                output[cnt--] = (String) enu.nextElement();
            }
            return output;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.shared.dvr.RecordingRequest#removeAppData(java.lang.String)
     */
    public void removeAppData(String key) throws AccessDeniedException
    {
        SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();
            
            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            if (key == null)
            {
                throw new IllegalArgumentException("Detected null key parameter");
            }

            Hashtable dataTable = null;

            // find owners appID (bug 4042 modification)
            AppID appID = m_infoTree.getAppId();

            dataTable = (Hashtable) m_infoTree.getAppDataTable().get(appID.toString());
            if (dataTable != null)
            {
                Integer currentSizeObject = (Integer) m_infoTree.getUsedAppDataBytes().get(appID.toString());
                AppDataContainer oldData = (AppDataContainer) dataTable.get(key);
                if (oldData != null)
                {
                    int removedDataSize = oldData.getSize();
                    int newSum = currentSizeObject.intValue() - removedDataSize;
                    m_infoTree.getUsedAppDataBytes().put(appID.toString(), new Integer(newSum));
                    dataTable.remove(key);
                }
            }

            saveRecordingInfo(RecordingDBManager.ALL);
        }
    }

    /**
     * Modify the details of a recording request. The recording request shall be
     * re-evaluated based on the newly provided RecordingSpec. Rescheduling a
     * root recording request may result in state transitions for the root
     * recording request or its child recording requests. Rescheduling a root
     * recording request may also result in the scheduling of one or more new
     * child recording requests, or the deletion of one or more pending child
     * recording requests.
     * 
     * Note: If the recording request or one of its child recording request is
     * in IN_PROGRESS_STATE or IN_PROGRESS_INSUFFICIENT_SPACE_STATE, any changes
     * to the start time shall be ignored. In this case all other valid
     * parameters are applied. If the new value for a parameter is not valid
     * (e.g. the start-time and the duration is in the past), the implementation
     * shall ignore that parameter. In-progress recordings shall continue
     * uninterrupted, if the new recording spec does not request the recording
     * to be stopped.
     * 
     * @param newRecordingSpec
     *            - the new recording spec that shall be used to reschedule the
     *            root RecordingRequest.
     * 
     * @throws java.lang.IllegalArgumentException
     *             - if the new recording spec and the current recording spec
     *             for the recording request are different sub-classes of
     *             RecordingSpec.
     * 
     * @throws AccessDeniedException
     *             - if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * 
     * @throws java.lang.SecurityException
     *             - if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)*
     */
    public void reschedule(RecordingSpec newRecordingSpec) throws AccessDeniedException
    {
        synchronized (m_sync)
        {
            SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));

            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            // Returns gracefully if I am not root.
            if (!isRoot())
            {
                return;
            }

            // If the new recording spec and the current recording spec for
            // the recording request are different sub-classes
            // of RecordingSpec then throw an exception.
            if (!(newRecordingSpec instanceof PrivateRecordingSpec))
            {
                throw new IllegalArgumentException("Illegal RecordingSpec sub-class");
            }

            // get request resolution handler
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CallerContext cctx = ccm.getCurrentContext();
            AppID appID = (AppID) cctx.get(CallerContext.APP_ID);

            // If no rrrhandler I could just return, because unlike the resolve
            // method, this method
            // is defined to return void as if the application knows to call
            // resolve. However, this method
            // does not return the new child. I guess the app will detect the
            // new node the hard way.
            /*
             * if (appID == null) { return; }
             */

            String key = appID.toString();

            final RequestResolutionHandler rrh = m_recordingManager.getRequestResolutionHandler(key);

            // Create new parent in UNRESOLVED_STATE and make it a child
            // the first arguement. State changes in request arguement will
            // generate a RecordingChangedEvent.STATE_CHANGED.

            m_recordingProperties = newRecordingSpec.getProperties();
            m_infoTree.setExpirationDate(new Date(m_recordingProperties.getExpirationPeriod()));

            // At best update the OcapRecordingProperties
            if (m_recordingProperties instanceof OcapRecordingProperties)
            {
                OcapRecordingProperties orp = (OcapRecordingProperties) m_recordingProperties;

                // TODO: How do we persitently store a recording destination?
                MediaStorageVolume destination = orp.getDestination();
                m_infoTree.setBitRate(orp.getBitRate());
                m_infoTree.setOrganization(orp.getOrganization());
                m_infoTree.setPriority(orp.getPriorityFlag());
                m_infoTree.setFap(orp.getAccessPermissions() == null ? getDefaultFAP() : orp.getAccessPermissions());
                m_infoTree.setDestination(destination);
            }

            if (m_infoTree.getState() == ParentRecordingRequest.COMPLETELY_RESOLVED_STATE)
            {
                setResolutionState(ParentRecordingRequest.PARTIALLY_RESOLVED_STATE);
            }
            // what happens if you reschedule a root that is in the
            // CANCELLED_STATE?
            saveRecordingInfo(RecordingDBManager.ALL);

            // I cannot guarantee the caller that he will return
            // before his resolution handler is called or even before
            // he receives a RecordingChangedEvent. If it is desirable
            // for the application to return from this call before
            // its RequestResolutionHandler is entered, it can apply
            // synchronization.
            if (rrh != null)
            {
                final ParentNodeImpl parentRoot = this;
                cctx.runInContext(new Runnable()
                {
                    public void run()
                    {
                        rrh.requestResolution(parentRoot);
                    }
                });
            }
        } // END synchronized (m_sync)
    }

    /**
     * 
     * @param RequestResolutionState
     * @return
     */
    void setResolutionState(int resolutionState)
    {
        synchronized (m_sync)
        {
            if (m_infoTree.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw new IllegalStateException();
            }

            int oldState = m_infoTree.getState();
            m_infoTree.setState(resolutionState);
            NavigationManager.getInstance().updateRecording(this, resolutionState, oldState);
            if (m_parent != null)
            {
                m_parent.childResolutionStateChanged(this, resolutionState);
            }
        }
    }

    /**
     * Child notified me because his resolution state changed to partially, or
     * completely resolved.
     * 
     * 
     * 
     * 
     * @param child
     * @param resolutionState
     */
    private void childResolutionStateChanged(ParentNodeImpl child, int resolutionState)
    {
        // TODO:

        if (log.isInfoEnabled())
        {
            log.info("child=" + child + ", parent-state=" + resolutionState + ", children-size=" + m_myChildren.size());
        }

        // if I am not completely resolved yet.
        if ((m_infoTree.getState() == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE)
                || (m_infoTree.getState() == ParentRecordingRequest.UNRESOLVED_STATE))
        {
            // If level N-1, children are of type leaf else parent.
            Object rr = null;
            for (int i = 0; i < m_myChildren.size(); i++)
            {
                rr = m_myChildren.elementAt(i);
                if (log.isDebugEnabled())
                {
                    log.debug("rr Object = " + rr);
                }
                if (rr instanceof ParentRecordingRequest)
                {
                    ParentRecordingRequest prr = (ParentRecordingRequest) rr;

                    // I ignore children who are in CANCELLED_STATE because they
                    // are
                    // really pending delete.
                    if ((prr.getState() == ParentRecordingRequest.CANCELLED_STATE)) continue;

                    // Short circuit
                    // I can not resolve if I have children in the UNRESOLVED
                    // STATE
                    if ((prr.getState() == ParentRecordingRequest.UNRESOLVED_STATE)
                            || (prr.getState() == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE))
                    {
                        return;
                    }
                }
            }
        }

        m_infoTree.setState(ParentRecordingRequest.COMPLETELY_RESOLVED_STATE);
        // if all children completely resolved
        if (!isRoot())
        {
            m_parent.childResolutionStateChanged(this, ParentRecordingRequest.COMPLETELY_RESOLVED_STATE);
        }
    }

    /**
     * Caller: RecordingManagerImpl.completeRecordByServiceContext(...)
     * 
     * Add a child to this parent during the resolve process.
     * 
     * @param request
     *            refers to a parent or leaf object.
     */
    void insertChild(RecordingRequest request)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Entered. request  = " + request);
        }
        if (request instanceof RecordingImpl)
        {
            m_infoTree.children.addElement(((RecordingImpl) request).m_info);
        }
        else
        {
            m_infoTree.children.addElement(((ParentNodeImpl) request).m_infoTree);
        }

        m_myChildren.addElement(request);
    }

    void setParent(final ParentNodeImpl rr)
    {
        m_parent = rr;
    }

    void setRoot(final RecordingRequest rr)
    {
        m_root = rr;
        if (m_root == null)
        {
            m_isRoot = true;
        }
    }

    void addChild(RecordingRequest child)
    {
        m_myChildren.addElement(child);
    }

    void addChildren(final Vector children)
    {
        for (Enumeration e = children.elements(); e.hasMoreElements();)
        {
            m_myChildren.addElement(e.nextElement());
        }
    }

    void forgetChild(RecordingRequest child, int nstate, int ostate)
    {
        if (log.isDebugEnabled())
        {
            log.debug("child = " + child);
        }
        if (log.isDebugEnabled())
        {
            log.debug("nstate = " + nstate);
        }
        if (log.isDebugEnabled())
        {
            log.debug("ostate = " + ostate);
        }
        // First, remove the child RecordingRequest from
        // my list of RecordingRequest for children.
        m_myChildren.remove(child);

        if (child instanceof LeafRecordingRequest)
        {
            // Second, remove the child's m_info persistent structure
            // from my list of persistent children. Leaf's notify listeners.
            m_infoTree.children.remove(((RecordingImpl) child).m_info);
            // Note: Leaf is removed from NM by caller
        }
        else
        {
            // Second, remove the child's m_info persistent structure
            // from my list of persistent child objects.
            m_infoTree.children.remove(((ParentNodeImpl) child).m_infoTree);
            NavigationManager.getInstance().removeRecording(child, nstate, ostate);
        }
    }

    /**
     * Returns the base RecordingInfoNode that contains metadata about this
     * recording
     */
    RecordingInfoNode getRecordingInfoNode()
    {
        return m_infoTree;
    }

    /**
     * Parent implementation objects.
     */
    boolean m_cancel_in_progress = false;

    Vector m_myChildren = new Vector();

    static final int MAX_APPDATA_ENTRIES = 64;

    ParentNodeImpl m_parent = null;

    RecordingRequest m_root = null;

    boolean m_isRoot = false;

    RecordingInfoTree m_infoTree = null;

    PrivateRecordingSpec m_recordingSpec;

    RecordingProperties m_recordingProperties;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(ParentNodeImpl.class.getName());

}
