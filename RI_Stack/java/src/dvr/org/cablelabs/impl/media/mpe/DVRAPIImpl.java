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

import javax.media.Time;
import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.presentation.S3DConfigurationImpl;
import org.cablelabs.impl.util.PidMapTable;
import org.ocap.media.S3DConfiguration;

/**
 * This represents the MPE APIs that are DVR-specific. It extends
 * {@link MediaAPIImpl}.
 * 
 * @author schoonma
 */
public class DVRAPIImpl implements DVRAPIManager
{
    private static final Logger log = Logger.getLogger(DVRAPIImpl.class);

    /**
     * Directions for the frame control api.
     */
    private static final int DVR_DIRECTION_FORWARD = 0;

    private static final int DVR_DIRECTION_REVERSE = 1;

    private static DVRAPIImpl instance;

    protected DVRAPIImpl()
    {
        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);
    }

    public static synchronized DVRAPIManager getInstance()
    {
        if (instance == null) instance = new DVRAPIImpl();

        return instance;
    }

    private static synchronized void clearInstance()
    {
        instance = null;
    }

    public void destroy()
    {
        clearInstance();
    }

    public Playback decodeTSB(final EDListener listener, final int vd, final int tsb, PidMapTable pidMapTable, byte cci, long alarmMediaTime,
            final long start, final float rate, final boolean blocked, boolean mute, float gain)
    {
        if (log.isInfoEnabled())
        {
            log.info("decodeTSB(): " + " vd=" + MediaAPIImpl.handleToString(vd) + ", tsb="
                    + MediaAPIImpl.handleToString(tsb) + ", pidMapTable=" + pidMapTable + ", start=" + start
                    + ", rate=" + rate + ", blocked=" + blocked + ", alarm mediatime: " + alarmMediaTime + ", cci: " + cci);
        }

        // Check arguments.
        if (listener == null)
            throw new IllegalArgumentException("null listener");
        if (pidMapTable == null)
            throw new IllegalArgumentException("null pidMapTable");

        float[] rateArray = new float[]{rate};
        float[] gainArray = new float[]{gain};

        // Start native decoding.
        int[] dvra = { -1 };
        int err = nativeDecodeTSB(listener, vd, tsb, pidMapTable, cci, alarmMediaTime, dvra, start, rateArray, blocked, mute, gainArray);
        if (err != 0)
            throw new MPEMediaError(err, "decodeTSB");
        int dvr = dvra[0];

        Playback playback = new Playback(dvr, rateArray[0], gainArray[0]);
        if (log.isDebugEnabled())
        {
            log.debug("playback=" + playback);
        }
        return playback;
    }

    public Playback decodeRecording(EDListener listener, int vd, String recording, PidMapTable pidMapTable, byte cci,
                                    long alarmMediaTime, long start, float rate, boolean blocked, boolean mute, float gain) throws RecordingDeletedException
    {
        if (log.isInfoEnabled())
        {
            log.info("decodeRecording - start nanos=" + start + ", rate=" + rate  + " vd=" + MediaAPIImpl.handleToString(vd) + ", recording=" + recording
                    + ", pidMapTable=" + pidMapTable + ", alarm: " + alarmMediaTime + ", cci: " + cci);
        }

        // Check arguments.
        if (listener == null)
            throw new IllegalArgumentException("null listener");
        if (pidMapTable == null)
            throw new IllegalArgumentException("null pidMapTable");

        // Get the recording name; if null is returned, it has been deleted,
        // so just return null to indicate that playback could not be started.
        if (recording == null)
            throw new RecordingDeletedException("<deleted before playback started>");

        int[] dvrHandle = { -1 };

        float[] rateArray = new float[]{rate};
        float[] gainArray = new float[]{gain};

        int err = nativeDecodeRecording(listener, vd, recording, pidMapTable, cci, alarmMediaTime, dvrHandle, start, rateArray, blocked, mute, gainArray);
        //all native errors can be treated the same
        if (err != 0)
        {
            throw new MPEMediaError(err, "decodeRecording");
        }
        int dvr = dvrHandle[0];
        Playback playback = new Playback(dvr, rateArray[0], gainArray[0]);
        if (log.isDebugEnabled())
        {
            log.debug("playback=" + playback);
        }
        return playback;
    }

    public void blockPresentation(int dvr, boolean block)
    {
        if (log.isDebugEnabled())
        {
            log.debug("blockPresentation(dvr=" + MediaAPIImpl.handleToString(dvr) + ", block=" + block + ")");
        }

        int err = nativeBlockPresentation(dvr, block);
        if (err != 0)
            throw new MPEMediaError(err, "blockPresentation");
    }

    public void stopDVRDecode(int dvr, boolean holdFrame)
    {
        if (log.isInfoEnabled())
        {
            log.info("stopDVRDecode(dvr=" + MediaAPIImpl.handleToString(dvr) + " hold frame: " + holdFrame + ")");
        }

        nativeStopDVRDecode(dvr, holdFrame);
    }

    public float getRate(int dvr)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getRate(dvr=" + MediaAPIImpl.handleToString(dvr) + ")");
        }

        float[] rate = new float[1];
        int err = nativeGetRate(dvr, rate);
        if (err != 0)
            throw new MPEMediaError(err, "getRate");
        return rate[0];
    }

    public float setRate(int dvr, float rate)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setRate(dvr=" + MediaAPIImpl.handleToString(dvr) + ", rate=" + rate + ")");
        }

        float[] rateArray = new float[1];
        rateArray[0] = rate;
        int err = nativeSetRate(dvr, rateArray);
        if (err != 0)
            throw new MPEMediaError(err, "setRate");
        return rateArray[0];
    }

    public void setMediaTime(int dvr, long time)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setMediaTime(dvr=" + MediaAPIImpl.handleToString(dvr) + ", time=" + time + "ns)");
        }

        int err = nativeSetMediaTime(dvr, time);
        if (err != 0)
            throw new MPEMediaError(err, "setMediaTime");
    }

    public Time getMediaTime(int dvr)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getMediaTime(dvr=" + MediaAPIImpl.handleToString(dvr) + ")");
        }

        long[] time = new long[1];
        int err = nativeGetMediaTime(dvr, time);
        if (err != 0)
            throw new MPEMediaError(err, "getMediaTime");
        return new Time(time[0]);
    }

    public Time getRecordingMediaTimeForFrame(String name, Time mt, int direction)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getRecordingMediaTimeForFrame(name=" + name + " direction=" + direction + " Time= " + mt + ")");
        }

        long[] time = new long[1];
        int err = nativeGetRecordingMediaTimeForFrame(name, mt.getNanoseconds(), time, direction);
        if (err != 0)
            throw new MPEMediaError(err, "getRecordingMediaTimeForFrame");
        return new Time(time[0]);
    }

    public Time getTsbMediaTimeForFrame(int tsbHandle, Time mt, int direction)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getTsbMediaTimeForFrame(tsbHandle=" + tsbHandle + " direction=" + direction + " Time= " + mt
                    + ")");
        }

        long[] time = new long[1];
        int err = nativeGetTsbMediaTimeForFrame(tsbHandle, mt.getNanoseconds(), time, direction);
        if (err != 0)
            throw new MPEMediaError(err, "getTsbMediaTimeForFrame");
        return new Time(time[0]);
    }

    public boolean stepFrame(int dvr, int direction)
    {
        if (log.isDebugEnabled())
        {
            log.debug("stepFrame(dvr=" + MediaAPIImpl.handleToString(dvr) + " direction=" + direction + ")");
        }

        int err = nativeStepFrame(dvr, direction);
        if (err != 0)
            throw new MPEMediaError(err, "getFrameTime");
        return true;
    }

    public void playbackChangePids(int dvr, PidMapTable pidMapTable)
    {
        int err = nativePlaybackChangePids(dvr, pidMapTable);
        if (err != 0)
        {
            throw new MPEMediaError(err, "changePlaybackPids");
        }
    }

    public static int translateDirection(boolean direction)
    {
        return (direction ? DVR_DIRECTION_FORWARD : DVR_DIRECTION_REVERSE);
    }

    public void setMute(int sessionHandle, boolean mute)
    {
        int err = jniSetMute(sessionHandle, mute);
        if (err != 0)
        {
            throw new MPEMediaError(err, "setMute: " + mute);
        }
    }

    public float setGain(int sessionHandle, float gain)
    {
        float[] arg = new float[]{gain};
        int err = jniSetGain(sessionHandle, arg);
        if (err != 0)
        {
            throw new MPEMediaError(err, "setGain: " + gain);
        }
        return arg[0];
    }

    /*
     * Returns the input video scan mode via the 
     * call mpe_mediaGetInputVideoScanMode
     *
     * @see org.cablelabs.impl.media.mpe.MediaAPI#getInputVideoScanMode(int)
     */
    public int getInputVideoScanMode(int vd)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getInputVideoScanMode(" + vd + ")");
        }

        int scanModeArray[] = new int[1];
        int err = nativeGetVideoScanMode(vd, scanModeArray);
        if (err != 0)
        {
            throw new MPEMediaError(err, "getInputVideoScanMode");
        }

        // by convention with JNI, scanMode is element 0
        return scanModeArray[0];
    }

    public S3DConfiguration getS3DConfiguration(int vd)
    {
        if (log.isTraceEnabled())
        {
            log.trace("getS3DConfiguration(" + vd + ")");
        }

        int[] specifierArray = new int[3];
        int payloadArraySz = 1000;

        byte[] payloadArray;

        while (true)
        {
            payloadArray = new byte[payloadArraySz];

            int err = nativeGetS3DConfiguration(vd, specifierArray, payloadArray, payloadArraySz);

            // if MPE_ENOMEM, payload size is too small, so increase it and redo call
            if (err == 2 /* MPE_ENOMEM */)
            {
                payloadArraySz = 2 * payloadArraySz;
                continue;
            }
            else if (err != 0)
            {
                throw new MPEMediaError(err, "getS3DConfiguration");
            }

            break;
        }

        int formatType = specifierArray[0];
        int payloadType = specifierArray[1];
        int payloadSz = specifierArray[2];

        byte[] payload = new byte[payloadSz];
        System.arraycopy(payloadArray, 0, payload, 0, payloadSz);

        if (log.isTraceEnabled())
        {
            log.trace("formatType = " + formatType);
        }
        if (log.isTraceEnabled())
        {
            log.trace("payloadType = " + payloadType);
        }
        if (log.isTraceEnabled())
        {
            log.trace("payloadSz = " + payloadSz);
        }
        if (log.isTraceEnabled())
        {
            log.trace("PAYLOAD:");
        }
        if (log.isTraceEnabled())
        {
            int numLines = payloadSz/16;
            int leftover = payloadSz%16;
            for (int i=0; i<numLines; i++)
        {
                if (log.isTraceEnabled())
        {
                log.trace(payload[i+0] + ", " + payload[i+1] + ", " + payload[i+2] + ", " + payload[i+3] + ", " +
                    payload[i+4] + ", " + payload[i+5] + ", " + payload[i+6] + ", " + payload[i+7] + ", " + 
                    payload[i+8] + ", " + payload[i+9] + ", " + payload[i+10] + ", " + payload[i+11] + ", " + 
                    payload[i+12] + ", " + payload[i+13] + ", " + payload[i+14] + ", " + payload[i+15]);
        }
            }

            if (leftover != 0)
            {
                String tmp = payload[numLines*16] + "";
                for (int i=numLines*16+1; i<payloadSz; i++)
                {
                    tmp += ", " + payload[i];
                }
                if (log.isTraceEnabled())
                {
                    log.trace(tmp);
                }
        }
        }
        
        if (formatType == 0)
        {
            return null;
        }
        else
        {
            return new S3DConfigurationImpl(payloadType, formatType, payload);
        }
    }

    public void setCCI(int sessionHandle, byte cci)
    {
        int err = jniSetCCI(sessionHandle, cci);
        if (err != 0)
        {
            throw new MPEMediaError(err, "setCCI: " + cci);
        }
    }

    public void setAlarm(int sessionHandle, long alarmMediaTime)
    {
        int err = jniSetAlarm(sessionHandle, alarmMediaTime);
        if (err != 0)
        {
            throw new MPEMediaError(err, "setAlarm: " + alarmMediaTime);
        }
    }


    /*
     * 
     * Native Wrapper Methods These methods imply forward to the corresponding
     * JNI methods. By overriding these methods, subclasses can bypass the calls
     * to the JNI/native layer. This is useful for testing.
     */

    protected int nativeDecodeTSB(EDListener listener, int decoder, int tsb, PidMapTable pidMapTable, byte cci, long alarmMediaTime, int[] dvr,
            long start, float[] rate, boolean blocked, boolean mute, float[] gain)
    {
        return jniDecodeTSB(listener, decoder, tsb, pidMapTable, cci, alarmMediaTime, dvr, start, rate, blocked, mute, gain);
    }

    protected int nativeDecodeRecording(EDListener listener, int decoder, String recording, PidMapTable pidMapTable, byte cci,
                                        long alarmMediaTime, int[] handle, long start, float[] rate, boolean blocked, boolean mute, float[] gain)
    {
        return jniDecodeRecording(listener, decoder, recording, pidMapTable, cci, alarmMediaTime, handle, start, rate, blocked, mute, gain);
    }

    protected int nativeStopDVRDecode(int dvr, boolean holdFrame)
    {
        return jniStop(dvr, holdFrame);
    }

    protected int nativeBlockPresentation(int dvr, boolean block)
    {
        return jniBlockPresentation(dvr, block);
    }

    protected int nativeSetRate(int dvr, float[] rate)
    {
        return jniSetRate(dvr, rate);
    }

    protected int nativeGetRate(int dvr, float[] rate)
    {
        return jniGetRate(dvr, rate);
    }

    protected int nativeSetMediaTime(int dvr, long time)
    {
        return jniSetMediaTime(dvr, time);
    }

    protected int nativeGetMediaTime(int dvr, long[] time)
    {
        return jniGetMediaTime(dvr, time);
    }

    protected int nativeGetRecordingMediaTimeForFrame(String name, long mediaTime, long[] time, int direction)
    {
        return jniGetRecordingMediaTimeForFrame(name, mediaTime, time, direction);
    }

    protected int nativeGetTsbMediaTimeForFrame(int tsbHandle, long mediaTime, long[] time, int direction)
    {
        return jniGetTsbMediaTimeForFrame(tsbHandle, mediaTime, time, direction);
    }

    protected int nativeStepFrame(int dvr, int direction)
    {
        return jniStepFrame(dvr, direction);
    }

    protected int nativePlaybackChangePids(int dvr, PidMapTable pidMapTable)
    {
        return jniPlaybackChangePids(dvr, pidMapTable);
    }

    protected int nativeGetVideoScanMode(int vd, int[] specifierArray)
    {
        return jniGetVideoScanMode(vd, specifierArray);
    }

    protected int nativeGetS3DConfiguration(int vd, int[] specifierArray, byte[] payloadArray, int payloadArraySz)
    {
        return jniGetS3DConfiguration(vd, specifierArray, payloadArray, payloadArraySz);
    }

    /*
     * Native Methods All of the native methods below (except jniInit()) call an
     * MPE method (as indicated by their documentation) and return the resulting
     * MPE return value.
     */

    /**
     * Decode specified components from a TSB.
     * 
     * @param listener
     *            the {@link EDListener} that will receive asynchronous events
     *            for the decode session
     * @param decoder
     *            the native HDVideoDevice handle to decode to
     * @param tsb
     *            native handle of TSB from which to decode
     * @param pidMapTable
     *            the pidMapTable
     * @param cci
     *            the CCI byte to apply
     * @param alarmMediaTime
     *            the alarm mediatime or -1
     * @param dvr
     *            int array containing one element that will be initialized to a
     *            DVR playback handle on return (if successful)
     * @param start
     *            start time of playback, specified in media time nanoseconds
     * @param rate
     *            requested playback rate (updated with actual rate)
     * @param blocked
     *            blocked flag
     * @param mute
     *            mute flag
     * @param gain
     *            requested gain (updated with actual gain)
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniDecodeTSB(EDListener listener, int decoder, int tsb, PidMapTable pidMapTable, byte cci, long alarmMediaTime,
            int[] dvr, long start, float[] rate, boolean blocked, boolean mute, float[] gain);

    /**
     * Start native decoding of recording.
     * 
     * @param listener
     *            the {@link EDListener} that will receive asynchronous events
     *            for the decode session
     * @param decoder
     *            native handle of video device to use
     * @param recording
     *            native recording name to decode
     * @param pidMapTable
     *            the pidMapTable
     * @param cci
     *            the CCI byte to apply
     * @param alarmMediaTime
     *            the alarm mediatime or -1
     * @param dvr
     *            int array containing one element that will be initialized to a
     *            DVR playback handle on return (if successful)
     * @param start
     *            start time of playback, specified in media time nanoseconds
     * @param rate
     *            requested playback rate (updated with actual rate)
     * @param blocked
     *            blocked flag
     * @param mute
     *            mute flag
     * @param gain
     *            requested gain (updated with actual gain)
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniDecodeRecording(EDListener listener, int decoder, String recording,
            PidMapTable pidMapTable, byte cci, long alarmMediaTime, int[] dvr, long start, float[] rate, boolean blocked, boolean mute, float[] gain);

    /**
     * Native method to stop the DVR playback.
     * 
     * @param dvr handle of MPE DVR playback structure 
     * @param holdFrame   display the last frame if true
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniStop(int dvr, boolean holdFrame);

    /**
     * Native method to set the playback rate for a time-shift buffer. The
     * implementation is the same for all subclasses, so implementation resides
     * here.
     * 
     * @param dvr
     *            the native handle to a DVR playback structure
     * @param rate
     *            a float array whose first element is the desired rate (on
     *            input) and the actual rate (on return)
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniSetRate(int dvr, float[] rate);

    /**
     * Native method to retrieve the current playback rate.
     * 
     * @param dvr
     *            handle of MPE DVR playback structure
     * @param rate
     *            a float array of one element that is assigned the playback
     *            rate on return
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniGetRate(int dvr, float[] rate);

    /**
     * Native method to set the time for the dvr playback.
     * 
     * @param dvr
     *            handle of MPE DVR playback structure
     * @param time
     *            the media time to assign
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniSetMediaTime(int dvr, long time);

    /**
     * Native method to retrieve the time for the dvr playback.
     * 
     * @param dvr
     *            handle of MPE DVR playback structure
     * @param time
     *            long array containing one element that is set on return to the
     *            current media time
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniGetMediaTime(int dvr, long[] time);

    /**
     * 
     * @param dvr
     * @param time
     * @return
     */
    private static native int jniGetRecordingMediaTimeForFrame(String name, long mediaTime, long[] time, int direction);

    private static native int jniGetTsbMediaTimeForFrame(int tsbHandle, long mediaTime, long[] time, int direction);

    /**
     * 
     * @param dvr
     * @param time
     * @param direction
     * @return
     */
    private static native int jniStepFrame(int dvr, int direction);

    /**
     * Native method to block or unblock video on a drv playback.
     * 
     * @param dvr
     *            handle of MPE DVR playback structure
     * @param block
     *            <code>true</code> to block presentation; <code>false</code> to
     *            unblock.
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniBlockPresentation(int dvr, boolean block);

    /**
     * Native method to change TSB playback pids
     * 
     * @param dvr
     *            handle of MPE DVR playback structure
     * @param pidMapTable
     *            pidMapTable containing requested streams
     * @return Returns 0 if successful; otherwise, a native (MPE) error code.
     */
    private static native int jniPlaybackChangePids(int dvr, PidMapTable pidMapTable);

    private static native int jniSetMute(int sessionHandle, boolean mute);

    private static native int jniSetGain(int sessionHandle, float[] arg);

    private static native int jniGetS3DConfiguration(int decoder, int[] specifierArray, byte[] payloadArray, int payloadArraySz);

    private static native int jniGetVideoScanMode(int decoder, int[] specifierArray);
    
    private static native int jniSetCCI(int sessionHandle, byte cci);

    private static native int jniSetAlarm(int sessionHandle, long alarmMediaTime);
}
