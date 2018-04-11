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
package org.cablelabs.impl.media.mpe;

import java.util.Arrays;

import javax.media.Time;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.util.PidMapTable;
import org.ocap.media.S3DConfiguration;

/**
 * CannedDVRAPI
 * 
 * @author Joshua Keplinger
 * 
 */
public class CannedDVRAPI implements DVRAPIManager
{
    private int decodeEvent = MediaAPI.Event.CONTENT_PRESENTING;

    private int[] decodeRecordingPIDs;

    private int[] decodeTSBPIDs;

    private int handle;

    private float rate;

    private long mt;

    private boolean checkVideoDevice;

    private EDListener lastDecodeRecordingListener;

    private String lastDecodedRecording;

    public CannedDVRAPI()
    {
        handle = 1;
        rate = 0.0f;
        mt = 0L;
    }

    public void destroy()
    {

    }

    public static DVRAPIManager getInstance()
    {
        return new CannedDVRAPI();
    }

    public String toString(int event)
    {
        switch (event)
        {
            case DVRAPI.Event.END_OF_FILE:
                return "DVR_END_OF_FILE";
            case DVRAPI.Event.START_OF_FILE:
                return "DVR_START_OF_FILE";
            case DVRAPI.Event.SESSION_CLOSED:
                return "DVR_SESSION_CLOSED";
            default:
                return MediaAPIImpl.eventToString(event);
        }
    }

    public Playback decodeTSB(EDListener listener, int vd, int tsb, PidMapTable pidMapTable, byte cci, long alarmMediaTime, long start, float rate,
            boolean blocked, boolean mute, float gain)
    {
        if (checkVideoDevice && vd != 1 && vd != 2) throw new MPEMediaError(1, "Invalid decoder");
        setRate(1, rate);
        queuePresentingEvent(listener);
        return new Playback(1, this.rate, gain);
    }

    public Playback decodeRecording(EDListener listener, int vd, String recording, PidMapTable pidMapTable, byte cci, long alarmMediaTime, long start,
                                    float rate, boolean blocked, boolean mute, float gain) throws RecordingDeletedException
    {
        if (checkVideoDevice && vd != 1 && vd != 2) throw new MPEMediaError(1, "Invalid decoder");
        setRate(1, rate);
        mt = start;
        queuePresentingEvent(listener);
        lastDecodeRecordingListener = listener;
        lastDecodedRecording = recording;

        return new Playback(1, this.rate, gain);
    }

    public void stopDVRDecode(int dvr, boolean holdFrame)
    {
        if (handle != dvr) throw new MPEMediaError(4, "Invalid dvr handle");
        rate = 0.0f;
    }

    public float getRate(int dvr)
    {
        if (handle != dvr) throw new MPEMediaError(4, "Invalid dvr handle");
        return rate;
    }

    public float setRate(int dvr, float rate)
    {
        if (handle != dvr) throw new MPEMediaError(4, "Invalid dvr handle");

        if (Arrays.binarySearch(new float[] { -2.0f, -1.0f, -0.5f, -0.25f, 0.0f, 0.25f, 0.5f, 1.0f, 2.0f }, rate) >= 0)
            return this.rate = rate;
        else
            return this.rate;
    }

    public void setMediaTime(int dvr, long time)
    {
        if (handle != dvr) throw new MPEMediaError(4, "Invalid dvr handle");

        mt = time;
    }

    public Time getMediaTime(int dvr)
    {
        if (handle != dvr) throw new MPEMediaError(4, "Invalid dvr handle");

        return new Time(mt);
    }

    private boolean blockingState = false;

    public void blockPresentation(int dvr, boolean block)
    {
        if (handle != dvr) throw new MPEMediaError(4, "Invalid dvr handle");

        blockingState = block;
    }

    public boolean cannedGetBlocking()
    {
        return blockingState;
    }

    public String cannedGetLastDecodedRecording()
    {
        return lastDecodedRecording;
    }

    public void cannedClearLastDecodedRecording()
    {
        lastDecodedRecording = null;
    }

    public EDListener cannedGetLastDecodeRecordingListener()
    {
        return lastDecodeRecordingListener;
    }

    private void queuePresentingEvent(EDListener l)
    {
        //
        // This is currently not done by the real api and is mocked out by
        // the player (see DVRServicePlayerBase.startTimeshiftSession)
        // 
        final EDListener listener = l;
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                listener.asyncEvent(decodeEvent, 0, 0);
            }
        });
    }

    public int[] cannedGetDecodeRecordingPIDs()
    {
        return decodeRecordingPIDs;
    }

    public int[] cannedGetDecodeTSBPIDs()
    {
        return decodeTSBPIDs;
    }

    public void cannedSetCheckVideoDevice(boolean b)
    {
        checkVideoDevice = b;
    }

    public Time getRecordingMediaTimeForFrame(String name, Time mt, int direction)
    {
        return new Time(0);
    }

    public Time getTsbMediaTimeForFrame(int tsbHandle, Time originalTime, int direction)
    {
        return new Time(0);
    }

    public boolean stepFrame(int dvr, int direction)
    {
        return true;
    }

    public void playbackChangePids(int dvr, PidMapTable pidMapTable)
    {

    }

    public void setMute(int sessionHandle, boolean mute)
    {
        //no-op
    }

    public float setGain(int sessionHandle, float gain)
    {
        return 0.0F;
    }

    public S3DConfiguration getS3DConfiguration(int vd)
    {
        return null;
    }

    public int getInputVideoScanMode(int vd)
    {
        return 0;
    }

    public void setCCI(int sessionHandle, byte cci)
    {
        //no-op
    }

    public void setAlarm(int sessionHandle, long alarmMediaTime)
    {
        //no-op
    }
}
