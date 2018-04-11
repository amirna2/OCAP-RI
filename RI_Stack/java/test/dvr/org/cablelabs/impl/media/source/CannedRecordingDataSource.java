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
package org.cablelabs.impl.media.source;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

import javax.media.MediaLocator;
import javax.media.Time;
import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceType;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceMediaHandler;

import org.davic.net.tuning.NetworkInterface;
import org.dvb.application.AppID;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.RecordingExt;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver;
import org.cablelabs.impl.manager.recording.OcapRecordedServiceExt;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.media.protocol.recording.DataSource;
import org.cablelabs.impl.storage.MediaStorageVolumeExt;
import org.cablelabs.impl.util.TimeTable;

/**
 * CannedRecordingDataSource
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedRecordingDataSource extends DataSource
{

    private CannedRecordedService rs;

    private ExtendedNetworkInterface ni;

    /**
     * 
     */
    public CannedRecordingDataSource()
    {
        super();
        rs = new CannedRecordedService();
    }

    /**
     * @param source
     */
    public CannedRecordingDataSource(MediaLocator source)
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#getContentType()
     */
    public String getContentType()
    {
        // TODO (Josh) Implement
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#connect()
     */
    public void connect() throws IOException
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#disconnect()
     */
    public void disconnect()
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#start()
     */
    public void start() throws IOException
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.DataSource#stop()
     */
    public void stop() throws IOException
    {
        // TODO (Josh) Implement

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.source.RecordingDataSource#getRecordedService()
     */
    public OcapRecordedServiceExt getRecordedService()
    {
        return rs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.source.RecordingDataSource#recordingInProgress()
     */
    public boolean recordingInProgress()
    {
        // TODO (Josh) Implement
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.source.ServiceDataSource#getService()
     */
    public Service getService()
    {
        return rs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.media.source.ServiceDataSource#setNI(org.cablelabs
     * .impl.davic.net.tuning.ExtendedNetworkInterface)
     */
    public void setNI(ExtendedNetworkInterface ni)
    {
        this.ni = ni;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.media.source.ServiceDataSource#getNI()
     */
    public ExtendedNetworkInterface getNI()
    {
        // TODO (Josh) Implement
        return ni;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.Controls#getControls()
     */
    public Object[] getControls()
    {
        // TODO (Josh) Implement
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.protocol.Controls#getControl(java.lang.String)
     */
    public Object getControl(String controlType)
    {
        // TODO (Josh) Implement
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.media.Duration#getDuration()
     */
    public Time getDuration()
    {
        // TODO (Josh) Implement
        return null;
    }

    public class CannedRecordedService implements OcapRecordedServiceExt
    {

        private CannedRecordingRequest rr;

        private Time mt;

        public CannedRecordedService()
        {
            rr = new CannedRecordingRequest();
            mt = new Time(0L);
        }

        public String getNativeName()
        {
            return "CannedRecordedService";
        }

        public long getRecordedBitRate()
        {
            // TODO (Josh) Implement
            return 0;
        }

        public long getRecordedSize()
        {
            // TODO (Josh) Implement
            return 0;
        }

        public boolean isDecryptable()
        {
            // TODO (Josh) Implement
            return false;
        }

        public boolean isDecodable()
        {
            // TODO (Josh) Implement
            return false;
        }

        public RecordingRequest getRecordingRequest()
        {
            return rr;
        }

        public long getRecordedDuration()
        {
            return 60000; // 60 seconds
        }

        public MediaLocator getMediaLocator()
        {
            // TODO (Josh) Implement
            return null;
        }

        public void setMediaTime(Time mediaTime) throws AccessDeniedException
        {
            mt = mediaTime;
        }

        public Time getMediaTime()
        {
            return mt;
        }

        public Date getRecordingStartTime()
        {
            // TODO (Josh) Implement
            return null;
        }

        public void delete() throws AccessDeniedException
        {
            // TODO (Josh) Implement

        }

        public Time getFirstMediaTime()
        {
            return new Time(0L);
        }

        public SIRequest retrieveDetails(SIRequestor requestor)
        {
            // TODO (Josh) Implement
            return null;
        }

        public String getName()
        {
            // TODO (Josh) Implement
            return null;
        }

        public boolean hasMultipleInstances()
        {
            // TODO (Josh) Implement
            return false;
        }

        public ServiceType getServiceType()
        {
            // TODO (Josh) Implement
            return null;
        }

        public Locator getLocator()
        {
            // TODO (Josh) Implement
            return null;
        }

        public TimeTable getCCITimeTable()
        {
            return null;
        }

        public int getSegmentIndex()
        {
            return 0;
        }
    }

    public class CannedRecordingRequest implements RecordingRequest, RecordingExt
    {

        private int presCount = 0;

        public int getState()
        {
            // TODO (Josh) Implement
            return 0;
        }

        public boolean isRoot()
        {
            // TODO (Josh) Implement
            return false;
        }

        public RecordingRequest getRoot()
        {
            // TODO (Josh) Implement
            return null;
        }

        public RecordingRequest getParent()
        {
            // TODO (Josh) Implement
            return null;
        }

        public RecordingSpec getRecordingSpec()
        {
            // TODO (Josh) Implement
            return null;
        }

        public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
                AccessDeniedException
        {
            // TODO (Josh) Implement

        }

        public void delete() throws AccessDeniedException
        {
            // TODO (Josh) Implement

        }

        public void addAppData(String key, Serializable data) throws NoMoreDataEntriesException, AccessDeniedException
        {
            // TODO (Josh) Implement

        }

        public AppID getAppID()
        {
            // TODO (Josh) Implement
            return null;
        }

        public String[] getKeys()
        {
            // TODO (Josh) Implement
            return null;
        }

        public Serializable getAppData(String key)
        {
            // TODO (Josh) Implement
            return null;
        }

        public void removeAppData(String key) throws AccessDeniedException
        {
            // TODO (Josh) Implement

        }

        public void reschedule(RecordingSpec newRecordingSpec) throws AccessDeniedException
        {
            // TODO (Josh) Implement

        }

        public int getId()
        {
            // TODO (Josh) Implement
            return 0;
        }

        public NetworkInterface getNetworkInterface()
        {
            // TODO (Josh) Implement
            return null;
        }

        public int cannedGetPresentationCount()
        {
            return presCount;
        }

        public void addRecordingUpdateListener(RecordingUpdateListener listener)
        {

        }

        public void removeRecordingUpdateListener(RecordingUpdateListener listener)
        {

        }

        public MediaStorageVolumeExt getDestination()
        {
            return null;
        }

        public long getRecordedDuration()
        {
            return 0;
        }

        public void decPresentationCount(ServicePlayer player)
        {
            presCount--;
        }

        public void incPresentationCount(ServicePlayer player, ServiceContext sc)
        {
            presCount++;
        }

        public void addPlayer(AbstractDVRServicePlayer player)
        {
        }

        public void removePlayer(AbstractDVRServicePlayer player)
        {
        }

        public void addObserver(PlaybackClientObserver listener)
        {
        }

        public Vector getPlayers()
        {
            return new Vector();
        }

        public void removeObserver(PlaybackClientObserver listener)
        {
        }

        public void notifyServiceContextPresenting(ServiceContext sc, ServiceMediaHandler smh)
        {
        }

    }
}
