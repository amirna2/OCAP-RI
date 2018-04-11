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

import java.io.Serializable;
import java.util.Vector;

import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceMediaHandler;

import org.davic.net.tuning.NetworkInterface;
import org.dvb.application.AppID;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;

import org.cablelabs.impl.davic.net.tuning.CannedNetworkInterface;
import org.cablelabs.impl.manager.RecordingExt;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.media.player.ServicePlayer;
import org.cablelabs.impl.storage.MediaStorageVolumeExt;

public class CannedRecordingRequest implements RecordingRequest, RecordingExt
{
    int state;

    private RecordingSpec spec;

    public int getState()
    {
        return state;
    }

    public boolean isRoot()
    {
        return false;
    }

    public RecordingRequest getRoot()
    {
        return null;
    }

    public RecordingRequest getParent()
    {
        return null;
    }

    public RecordingSpec getRecordingSpec()
    {
        return spec;
    }

    public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
            AccessDeniedException
    {

    }

    public void delete() throws AccessDeniedException
    {

    }

    public void addAppData(String key, Serializable data) throws NoMoreDataEntriesException, AccessDeniedException
    {

    }

    public AppID getAppID()
    {
        return null;
    }

    public String[] getKeys()
    {
        return null;
    }

    public Serializable getAppData(String key)
    {
        return null;
    }

    public void removeAppData(String key) throws AccessDeniedException
    {

    }

    public void reschedule(RecordingSpec newRecordingSpec) throws AccessDeniedException
    {

    }

    public int getId()
    {
        return 0;
    }

    public NetworkInterface getNetworkInterface()
    {
        return new CannedNetworkInterface(0);
    }

    public long getRecordedDuration()
    {
        return 60000L;
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

    public void cannedSetState(int state)
    {
        this.state = state;
    }

    public void cannedSetRecordingSpec(RecordingSpec spec)
    {
        this.spec = spec;
    }

    public void decPresentationCount(ServicePlayer player)
    {
        // TODO Auto-generated method stub

    }

    public void incPresentationCount(ServicePlayer player, ServiceContext sc)
    {
        // TODO Auto-generated method stub

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

    public void removeObserver(PlaybackClientObserver listener)
    {
    }

    public Vector getPlayers()
    {
        return new Vector();
    }

    public void notifyServiceContextPresenting(ServiceContext sc, ServiceMediaHandler smh)
    {

    }
}
