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

package org.cablelabs.lib.utils;

import java.io.Serializable;
import java.util.Date;
import java.lang.NullPointerException;

import javax.tv.service.selection.ServiceContext;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

import org.cablelabs.lib.utils.SimpleMonitor;

import javax.tv.service.selection.InvalidServiceComponentException;

public class Recorder
{

    ServiceContext m_svcCtx = null;

    Object m_handler = null;

    public Recorder()
    {
    }

    /*
     * Schedules a recording by locator recording spec
     * 
     * @param recordingName String name to be associated with recording that is
     * stored in app data
     * 
     * @param source Locator containing the source to record from
     * 
     * @param startTime Time in which to start recording
     * 
     * @param duration Length of recording in milliseconds
     * 
     * @param expiration Length of time from start of recording in which the
     * recording is deleted
     * 
     * @param retentionPriority
     * 
     * @param recordingPriority
     * 
     * @param access File access permissions to the recording request
     * 
     * @param organization organization the recording is tied to
     * 
     * @param destination MediaStorageVolume that the recording shall reside
     * 
     * @throws llegalArgumentException Refer to comments below in body @throws
     * InvalidServiceComponent Refer to comments below in body @throws
     * AccessDeniedException Refer to comments below in body @throws
     * SecurityException Refer to comments below in body @throws
     * NoMoreDataEntriesException Refer to comments below in body
     */
    public void scheduleRecording(String recordingName, OcapLocator locator, long startTime, long duration,
            long expiration, int retentionPriority, byte recordingPriority, ExtendedFileAccessPermissions efap,
            String org, MediaStorageVolume msv) throws IllegalArgumentException, InvalidServiceComponentException,
            AccessDeniedException, SecurityException, NoMoreDataEntriesException
    {

        OcapRecordingRequest rr = null;
        LocatorRecordingSpec lrs = null;
        OcapRecordingProperties orp = null;
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

        // May throw Illegal Argument Exception
        orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, expiration, retentionPriority,
                recordingPriority, efap, org, msv);

        OcapLocator[] source = new OcapLocator[1];
        source[0] = locator;

        // May throw InvalidServiceComponent or IllegalArgumentException
        lrs = new LocatorRecordingSpec(source, new Date(startTime), duration, orp);

        recordAndAddAppData(recordingName, duration, orp, rm, lrs);
    }

    /*
     * Plays back a recording by either passed in service conext or created
     * service context
     * 
     * @param recordingName String name associated with a recording that is
     * stored in app data
     * 
     * @param svcCtx Service Context to be used to display the recorded video on
     * If null is passed in, a service context will be created
     * 
     * @throws SecurityException
     * 
     * @throws IndexOutOfBoundsException
     * 
     * @throws NullPointerException
     * 
     * @throws AccessDeniedException
     */
    public void playbackRecording(String recordingName, ServiceContext svcCtx) throws IndexOutOfBoundsException,
            SecurityException, NullPointerException, AccessDeniedException
    {
        RecordingRequest rr = null;
        // Get the Recorinding List
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        // Security Exception could be thrown here
        RecordingList rl = rm.getEntries();

        // Walk the list
        for (int x = 0; x < rl.size(); x++)
        {
            // Match the name to the recording
            // IndexOutOfBoundsException can be thrown here
            rr = rl.getRecordingRequest(x);

            // Delete if match
            if (recordingName.equals((String) rr.getAppData(recordingName)))
            {
                System.out.println("************************************************************");
                System.out.println("* Selecting " + recordingName + " as " + rr.toString() + "**");
                System.out.println("************************************************************");
                break;
            }
        }

        // Playback the recording on the given Service Context
        if (svcCtx == null)
        {
            throw new NullPointerException("SeviceContext parameter is null");
        }
        else
        {
            // May throw IllegalStateException or AccessDeniedException
            RecordedService rs = ((LeafRecordingRequest) rr).getService();
            // May throw IllegalStateException or SecurityException
            svcCtx.select(rs);
        }
    }

    /*
     * Checks the state of the ongoing recording
     * 
     * @param recordingName String name associated with a recording that is
     * stored in app data
     * 
     * return
     */
    public void checkState(String recordingName)
    {
        // Get the Recorinding List
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        RecordingList rl = rm.getEntries();

        // Walk the list
        for (int x = 0; x < rl.size(); x++)
        {
            // Match the name to the recording
            RecordingRequest rr = rl.getRecordingRequest(x);

            // Print state if match
            if (recordingName.equals((String) rr.getAppData(recordingName)))
            {

            }
        }
    }

    /*
     * Deletes a recording from the database
     * 
     * @param recordingName Straing name associated with a recording that is
     * stored in app data
     * 
     * @throws IndexOutofBoundsException
     * 
     * @throws SecurityException
     * 
     * @throws AccessDeniedException
     */
    public void deleteRecording(String recordingName) throws IndexOutOfBoundsException, SecurityException,
            AccessDeniedException
    {
        // Get the Recording List
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        // Security Exception could be thrown here
        RecordingList rl = rm.getEntries();

        // Walk the list
        for (int x = 0; x < rl.size(); x++)
        {
            // Match the name to the recording
            // IndexOutOfBoundsException can be thrown here
            RecordingRequest rr = rl.getRecordingRequest(x);

            // Delete if match
            if (recordingName.equals((String) rr.getAppData(recordingName)))
            {
                System.out.println("*************************************************************");
                System.out.println("* Deleting " + recordingName + " as " + rr.toString() + "**");
                System.out.println("*************************************************************");

                // SecurityException or AccessDeniedException can be thrown here
                rr.delete();
                return;
            }
        }
    }

    /*
     * Registers a handler to receive recorder events
     * 
     * @param handler Handles various recording and playback events from the
     * recorder
     */
    public void registerHandler(Object handler)
    {

    }

    /*
     * Registers a handler to recieve recorder events
     * 
     * @param handler Handles various recording and playback events from the
     * recorder
     */
    public void unregisterHandler(Object handler)
    {

    }

    /*
     * Retrieves the MediaStorageVlume from a given recording
     * 
     * @ param recordingName
     * 
     * @ throw SecurityException
     * 
     * @ throw IndexOutOfBoundsException
     */
    public MediaStorageVolume getMediaStorageVolume(String recordingName) throws SecurityException,
            IndexOutOfBoundsException
    {
        MediaStorageVolume msv = null;
        // Get the Recording List
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        // Security Exception could be thrown here
        RecordingList rl = rm.getEntries();

        // Walk the list
        for (int x = 0; x < rl.size(); x++)
        {
            // Match the name to the recording
            // IndexOutOfBoundsException can be thrown here
            RecordingRequest rr = rl.getRecordingRequest(x);

            // if match return the MediaStorageVolume associated with the Spec
            if (recordingName.equals((String) rr.getAppData(recordingName)))
            {
                OcapRecordingProperties orp = (OcapRecordingProperties) rr.getRecordingSpec().getProperties();
                msv = orp.getDestination();
                System.out.println("*************************************************************");
                System.out.println("* Returning MSV " + msv.toString() + "**");
                System.out.println("*************************************************************");
                break;
            }
        }
        return msv;
    }

    public OcapRecordedService getRecordedService(String recordingName) throws SecurityException,
            IndexOutOfBoundsException, IllegalStateException, AccessDeniedException
    {
        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        // Security Exception could be thrown here
        RecordingList rl = rm.getEntries();

        // Walk the list
        for (int x = 0; x < rl.size(); x++)
        {
            // Match the name to the recording
            // IndexOutOfBoundsException can be thrown here
            OcapRecordingRequest rr = (OcapRecordingRequest) rl.getRecordingRequest(x);
            // if match return the MediaStorageVolume associated with the Spec
            if (recordingName.equals((String) rr.getAppData(recordingName)))
            {
                return (OcapRecordedService) rr.getService();
            }
        }
        return null;
    }

    /**
     * Start a recording ASAP. Wait is true, the function will not return until
     * the recording completes. A timeout is used so that if the recording fails
     * and there is no event sent to
     * 
     * 
     * @param recordingName
     * @param locator
     * @param duration
     * @param expiration
     * @param retentionPriority
     * @param recordingPriority
     * @param efap
     * @param org
     * @param msv
     * @param synchronous
     *            true means wait for the recording to complete or fail before
     *            returning.
     * @throws IllegalArgumentException
     * @throws InvalidServiceComponentException
     * @throws AccessDeniedException
     * @throws SecurityException
     * @throws NoMoreDataEntriesException
     */
    public RecordingRequest nowRecording(final String recordingName, final OcapLocator locator, final long duration,
            final long expiration, final int retentionPriority, final byte recordingPriority,
            final ExtendedFileAccessPermissions efap, final String org, final MediaStorageVolume msv,
            final boolean synchronous) throws IllegalArgumentException, InvalidServiceComponentException,
            AccessDeniedException, SecurityException, NoMoreDataEntriesException
    {

        RecordingRequest rr = null;

        final OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

        // May throw Illegal Argument Exception
        final OcapRecordingProperties orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE,
                expiration, retentionPriority, recordingPriority, efap, org, msv);

        OcapLocator[] source = new OcapLocator[1];
        source[0] = locator;
        Date now = new Date();
        Date startTime = new Date(now.getTime() + 1000); // start this a little
                                                         // in the future.

        // May throw InvalidServiceComponent or IllegalArgumentException
        final LocatorRecordingSpec lrs = new LocatorRecordingSpec(source, startTime, duration, orp);

        if (synchronous == true)
        {
            final SimpleMonitor monitor = new SimpleMonitor();

            RecordingChangedListener rcl = new RecordingChangedListener()
            {
                public void recordingChanged(RecordingChangedEvent e)
                {
                    if (e.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                    {
                    }
                    else if (e.getState() == LeafRecordingRequest.IN_PROGRESS_STATE)
                    {
                    }
                    else if (e.getState() == LeafRecordingRequest.COMPLETED_STATE)
                    {
                        System.out.println("Recorder.nowRecording:  Recording succeeded");
                        monitor.setAndNotify(true); // true == recording
                                                    // succeeded
                    }
                    else if (e.getState() == LeafRecordingRequest.FAILED_STATE)
                    {
                        System.out.println("Recorder.nowRecording:  Recording failed");
                        monitor.setAndNotify(false); // false == recording
                                                     // failed.
                    }
                }
            };

            rm.addRecordingChangedListener(rcl);

            rr = recordAndAddAppData(recordingName, duration, orp, rm, lrs);

            try
            {
                monitor.setAndWait(false, duration + 5000); // default is
                                                            // recording failed
            }
            catch (InterruptedException e1)
            {
                e1.printStackTrace();
            }
            rm.removeRecordingChangedListener(rcl);
            if (monitor.getState() == false)
            {
                rr = null;
            }
        }
        else
        {
            rr = recordAndAddAppData(recordingName, duration, orp, rm, lrs);
        }
        return rr;
    }

    private RecordingRequest recordAndAddAppData(String recordingName, long duration, OcapRecordingProperties orp,
            OcapRecordingManager rm, RecordingSpec rs) throws InvalidServiceComponentException, AccessDeniedException,
            NoMoreDataEntriesException
    {
        OcapRecordingRequest rr;

        // May throw IllegalArgumentException, AccessDeniedException, and
        // SecurityException
        rr = (OcapRecordingRequest) rm.record(rs);

        if (rr != null)
        {
            // create an entry with the recording name to be matched with the
            // key
            // May throw SecurityException, IllegalArgumentException,
            // NoMoreDataEntriesException, or AccessDeniedException
            rr.addAppData(recordingName, (Serializable) recordingName);

            System.out.println("*****************************************************************");
            System.out.println("****" + recordingName + " scheduled as " + rr.toString() + "*****");
            System.out.println("*****************************************************************");
        }
        return rr;
    }

    /*
     * If the media storage volume associated w/ a recording is null, find the
     * default internal storage device
     * 
     * @return MediaStorageVolume returns default MediaStorageVolume
     */
    public MediaStorageVolume getDefaultStorageVolume()
    {
        MediaStorageVolume msv = null;
        LogicalStorageVolume lsv[] = null;
        StorageProxy[] proxies = StorageManager.getInstance().getStorageProxies();
        if (proxies.length != 0)
        {
            lsv = proxies[0].getVolumes();
        }
        else
        {
            System.out.println(" *********No proxies avaliable*********");
            return null;
        }

        System.out.println("*************************************************");
        System.out.println(" *****Found " + lsv.length + " volumes.******");
        System.out.println("*************************************************");
        for (int i = 0; i < lsv.length; i++)
        {
            if (lsv[i] instanceof MediaStorageVolume)
            {
                msv = (MediaStorageVolume) lsv[i];
                System.out.println("*************************************************");
                System.out.println("******Found MSV: " + msv + "*********");
                System.out.println("*************************************************");
            }
        }

        if (msv == null)
        {
            System.out.println("*************************************************");
            System.out.println("*******MediaStorageVolume not found!********");
            System.out.println("*************************************************");
        }
        return msv;
    }
}
