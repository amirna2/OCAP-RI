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

package org.cablelabs.impl.manager.sound;

import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.SoundManager;
import org.cablelabs.impl.manager.sound.SoundManagerListener;
import org.cablelabs.impl.sound.Sound;
import org.cablelabs.impl.util.SystemEventUtil;

import org.apache.log4j.Logger;

import java.util.Vector;

public class SoundMgr implements SoundManager, EDListener
{

    private static SoundMgr soundMgrInstance;

    private int maxArraySize;

    private Vector v;

    private SoundManagerListener currentSound;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(SoundMgr.class.getName());

    /**
     * Private Constuctor
     * 
     * Use <code>ManagerManager.getInstance(SoundManager.class)<code>
     * to get a reference to the singleton instance of the SoundManager.
     */
    private SoundMgr()
    {
        // get max size of block to use for sound data
        maxArraySize = nSoundGetMaxArraySize();

        // create a vector to hold listener/sndHandle pairs
        v = new Vector();
        if (v == null)
        {
            SystemEventUtil.logRecoverableError(new Exception("SoundMgr::SoundMgr(): Unable to create a Vector"));
        }
    }

    /**
     * Returns the singleton instance of the <code>SoundManager</code>.
     * 
     * Intended to be called by the
     * {@link org.cablelabs.impl.manager.ManagerManager ManagerManager} only.
     * 
     * Invoke <code>ManagerManager.getInstance(SoundManager.class)<code>
     * to get the singleton instance of the SoundManager.
     * Do not call this method directly.
     * 
     * @return the singleton instance of the <code>SoundManager</code>.
     * 
     * @see org.cablelabs.impl.manager.ManagerManager#getInstance(Class)
     */
    public synchronized static Manager getInstance()
    {
        if (soundMgrInstance == null)
        {
            soundMgrInstance = new SoundMgr();
        }

        return soundMgrInstance;
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
        // delete every native player in the SoundManagerInfo vector
        if (v != null)
        {
            // get number of elements
            int count = v.size();

            // if we have SoundManagerInfo objects
            if (count > 0)
            {
                // for each object, stop and delete the player
                for (int i = 0; i < count; i++)
                {
                    // get an object from the vector
                    SoundManagerInfo smd = (SoundManagerInfo) v.elementAt(i);

                    if (log.isDebugEnabled())
                    {
                        log.debug("destroy - elementAt(" + i + ") - sndHandle = " + Integer.toString(smd.sndHandle)
                                + " (" + Integer.toHexString(smd.sndHandle) + "), " + "listener = " + smd.listener);
                    }

                    // stop and delete the player
                    nSoundStop(smd.sndHandle);
                    nSoundDelete(smd.sndHandle);

                    // remove the object from the vector
                    v.removeElementAt(i);
                }
            }
        }
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
     * Creates a native player for the specified sound
     * 
     * @param listener
     *            Object to receive <code>SoundManager</code> events. This is
     *            also used to identify the object that requested the sound to
     *            be played.
     * 
     * @param data
     *            sound data to be played
     * 
     * @param size
     *            the number of bytes of sound data
     * 
     * @param mimeType
     *            MIME type of the sound
     * 
     * @return false if an error occurred, or the mimeType is not a supported
     *         audio format
     */
    public synchronized boolean create(SoundManagerListener listener, byte[] data, int size, String mimeType)
    {

        int mimeTypeID = getMimeTypeID(mimeType);

        int sndHandle = nSoundCreate(data, size, mimeTypeID);

        if (log.isDebugEnabled())
        {
            log.debug("create - nSoundCreate returned sndHandle = " + sndHandle + " (" + Integer.toHexString(sndHandle)
                    + ")");
        }

        if (sndHandle != 0)
        {
            // save the sndHandle associated with the listener
            boolean result = saveInfo(sndHandle, listener);
            if (result == false)
            {
                SystemEventUtil.logRecoverableError(new Exception(
                        "SoundMgr::create - Unable to save listener and sndHandle"));
                // if we can't save the data, then we can't play, so
                // delete the native player
                nSoundDelete(sndHandle);
                return false;
            }
            else
            {
                return true;
            }
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("create - nSoundCreate FAILED"));
            return false;
        }

    }

    /**
     * Plays the sound associated with the specified <code>listener</code>,
     * optionally, in 'loop' mode.
     * 
     * @param listener
     *            the sound to play
     * 
     * @param loop
     *            <code>boolean</code> that indicates that the sound should be
     *            played in loop mode
     * 
     * @return false if an error occurred
     */
    public synchronized boolean play(SoundManagerListener listener, boolean loop)
    {

        boolean bresult;
        boolean return_code = true;
        int sndHandle = 0;

        if (log.isDebugEnabled())
        {
            log.debug("play - listener = " + listener + ", loop = " + loop + ", currentSound = " + currentSound);
        }

        if (listener == null)
        {
            SystemEventUtil.logRecoverableError(new Exception("SoundMgr::play - ERROR - (listener == null)"));
            return false;
        }

        if (currentSound != null)
        {
            stop(currentSound);

            // if the new sound is the same as the currently playing sound,
            // then do NOT notify the owner.
            // if the new sound is NOT the currently playing sound,
            // then we must tell the owner that his sound has been stopped.
            if (listener != currentSound)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("play - sending " + "SND_EVENT_TERMINATED to listener");
                }
            }
        }

        // get the sndHandle associated with the new sound
        sndHandle = getHandle(listener);

        if (sndHandle != 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("stop - calling nSoundPlay");
            }
            bresult = nSoundPlay(sndHandle, loop);
            if (bresult == true)
            {
                currentSound = listener;
            }
            else
            {
                SystemEventUtil.logRecoverableError(new Exception("play - nSoundPlay FAILED"));
                return_code = false;
            }
        }
        else
        // (sndHandle == 0)
        {
            SystemEventUtil.logRecoverableError(new Exception("play - Unable to play, there is no "
                    + "sndHandle for the specified listener"));
            return_code = false;
        }

        if (log.isDebugEnabled())
        {
            log.debug("play - returning " + return_code);
        }

        return return_code;

    }

    /**
     * Stop playing the sound associated with the specified
     * <code>listener</code>.
     * 
     * @param listener
     *            the sound to stop playing
     */
    public synchronized void stop(SoundManagerListener listener)
    {

        // get the sndHandle associated with this listener
        int sndHandle = getHandle(listener);

        if (sndHandle != 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("stop - calling nSoundStop");
            }
            nSoundStop(sndHandle);
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("SoundMgr::stop - Unable to stop, there is no "
                    + "sndHandle for the specified listener"));

        }

        // if we just called sSoundStop on the current sound,
        // then we no longer have a current sound.
        if (listener == currentSound)
        {
            currentSound = null;
        }

    }

    /**
     * Release all resources associated with the specified <code>listener</code>
     * .
     * 
     * @param listener
     *            the sound to delete
     */
    public synchronized void delete(SoundManagerListener listener)
    {

        // if the specified listener is the currently playing sound,
        // then we must stop it.
        if (listener == currentSound)
        {
            stop(listener);
        }

        // get the sndHandle associated with this listener
        int sndHandle = getHandle(listener);

        if (sndHandle != 0)
        {
            // delete the native player
            if (log.isDebugEnabled())
            {
                log.debug("stop - calling nSoundDelete");
            }
            nSoundDelete(sndHandle);

            // delete the SoundManagerInfo for this sound
            deleteInfo(sndHandle);
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("SoundMgr::delete - Unable to delete, there is no "
                    + "sndHandle for the specified listener"));
        }

    }

    /**
     * Get the current media time position
     * 
     * @param listener
     *            the sound to get media time position
     * 
     * @return javax.media.Time representing the current media time position
     */
    public synchronized javax.media.Time getMediaTimePosition(SoundManagerListener listener)
    {
        // get the sndHandle associated with this listener
        int sndHandle = getHandle(listener);

        if (sndHandle != 0)
        {
            return (new javax.media.Time(nSoundGetMediaTimePosition(sndHandle)));
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("SoundMgr::getMediaTimePosition - Unable to "
                    + "getMediaTimePosition - There is no sndHandle for " + "the specified listener"));

            long nanoseconds = 0;
            return (new javax.media.Time(nanoseconds));
        }
    }

    /**
     * Set a new media time position.
     * 
     * @param listener
     *            the sound to set media time position
     * 
     * @param mediaTime
     *            representing the desired media time position
     */
    public synchronized void setMediaTimePosition(SoundManagerListener listener, javax.media.Time mediaTime)
    {
        // get the sndHandle associated with this listener
        int sndHandle = getHandle(listener);

        if (sndHandle != 0)
        {
            nSoundSetMediaTimePosition(sndHandle, mediaTime.getNanoseconds());
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("SoundMgr::getMediaTimePosition - Unable to "
                    + "getMediaTimePosition - There is no sndHandle for " + "the specified listener"));
        }
    }

    /**
     * Get the maximum size, in bytes, to use for the sound data byte array
     * 
     * @return maximum size, in bytes, to use for the sound data byte array
     */
    public int getMaxArraySize()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getMaxArraySize - maxArraySize = " + maxArraySize);
        }
        return maxArraySize;
    }

    /**
     * Determine if the specified MIME type is supported
     * 
     * @param mimeType
     *            MIME type of the sound
     * 
     * @return true if the sound can be played
     */
    public boolean isSupported(String mimeType)
    {
        int mimeTypeID;

        if (log.isDebugEnabled())
        {
            log.debug("isSupported - mimeType = " + mimeType);
        }

        mimeTypeID = getMimeTypeID(mimeType);

        return (nSoundIsSupported(mimeTypeID));
    }

    /**
     * Receive for events dispatched by EventDispatcher.
     * 
     * @param eventCode
     *            event code sent by mpeos_snd
     * @param eventData1
     *            Not Used.
     * @param eventData2
     *            mpe_sndHandle
     * 
     * @see org.cablelabs.impl.manager.ed.EDListener#asyncEvent
     */
    public synchronized void asyncEvent(int eventCode, int eventData1, int eventData2)
    {

        // if (Logging.LOGGING)
        // {
        // log.debug ( "asyncEvent - entering "
        // + "- eventCode  = " + eventCode
        // + ", eventData1 = " + eventData1
        // + ", eventData2 = " + eventData2 );
        // }

        /*
         * if (eventCode == SND_EVENT_COMPLETE) { if (Logging.LOGGING) {
         * log.debug ("asyncEvent - SND_EVENT_COMPLETE"); }
         * 
         * // notify the owner of the sound that it has finished playing // if
         * there is no one to notify, then do nothing.
         * 
         * // eventData2 is the sndHandle of the sound that has completed //
         * playing. Use that sndHandle to get the Listener to notify.
         * SoundManagerListener listener = getListener (eventData2); if
         * (listener != null) { listener.soundManagerEventHandler
         * (SND_EVENT_COMPLETE); }
         * 
         * // if the event was for the currently playing sound, // then remember
         * that there is no currently playing sound. if (listener ==
         * currentSound) { currentSound = null; }
         * 
         * } else if (eventCode == SND_EVENT_SESSION_CLOSED) { // no need to do
         * anything for this event } else { if (Logging.LOGGING) { log.debug
         * ("asyncEvent - Unknown event " + eventCode + " (" +
         * Integer.toHexString(eventCode) + ")" ); } }
         */
    }

    /**
     * Get the MIME type ID for the specified MIME type
     * 
     * @param mimeType
     *            String containing the MIME type
     * 
     * @return mimeTypeID
     */
    private int getMimeTypeID(String mimeType)
    {
        // There is not a separate MIME type for the different mpeg formats.
        // MPEG-1 Layer 1, 2, and 3, all have the same MIME type.

        /*
         * if (mimeType.equals("audio/mpeg")) { return SND_MIMETYPE_MPEG; } else
         * if (mimeType.equals("audio/ac3")) { return SND_MIMETYPE_AC3; } else
         * if (mimeType.equals("audio/x-aiff")) { return SND_MIMETYPE_AIFF; }
         * else { return SND_MIMETYPE_UNKNOWN; }
         */
        return 0;
    }

    /**********************************************************************/
    /*
     * Methods to save, delete, and access client info such as sndHandle,
     * SoundManagerListener, et. al.
     */

    /**
     * Saves the sndHandle and listener.
     * 
     * Creates a <code>Vector</code>, if one doesn't exist. Creates a
     * <code>SoundManagerInfo</code> object using the <code>sndHandle</code> and
     * <code>listener</code>. Saves the <code>SoundManagerInfo</code> object in
     * the <code>Vector</code>.
     * 
     * @param sndHandle
     *            to be saved
     * @param listener
     *            to be saved
     * 
     * @return true if successful
     */
    private boolean saveInfo(int sndHandle, SoundManagerListener listener)
    {

        // dumpSoundManagerInfo ("saveInfo - entering");

        // if we haven't yet created a vector, do it now.
        if (v == null)
        {
            v = new Vector();
            if (v == null)
            {
                // if we couldn't create a new vector, then we have a problem.
                SystemEventUtil.logRecoverableError(new Exception("SoundMgr::saveInfo - Unable to create a Vector"));
                return false;
            }
        }

        // create a new object
        SoundManagerInfo smd = new SoundManagerInfo(sndHandle, listener);

        // add the new object to the vector
        v.addElement(smd);

        // dumpSoundManagerInfo ("saveInfo - exiting");

        return true;
    }

    /**
     * Deletes the SoundManagerInfo object that contains the specified
     * sndHandle.
     * 
     * @param sndHandle
     *            the sndHandle of the object to be deleted
     */
    private void deleteInfo(int sndHandle)
    {
        SoundManagerInfo smd;

        if (v != null)
        {
            // get number of elements
            int count = v.size();

            // if we have SoundManagerInfo objects
            if (count > 0)
            {
                // find the object that contains the specified sndHandle
                for (int i = 0; i < count; i++)
                {
                    // get an object from the vector
                    smd = (SoundManagerInfo) v.elementAt(i);

                    // does this object contain the specified sndHandle?
                    if (smd.sndHandle == sndHandle)
                    {
                        // if (Logging.LOGGING)
                        // {
                        // log.debug ("deleteInfo - found it");
                        // }

                        // yes, remove the object from the vector
                        v.removeElementAt(i);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get the sndHandle associated with the specified listener.
     * 
     * A sndHandle of 0 will be returned if the specified listener is not found.
     * 
     * @param listener
     *            the sound for which you want the sndHandle
     * 
     * @return the sndHandle corresponding to the specified listener
     */
    private int getHandle(SoundManagerListener listener)
    {
        SoundManagerInfo smd;

        // dumpSoundManagerInfo ("getHandle - entering");

        if (v != null)
        {
            // get number of elements
            int count = v.size();

            // if we have SoundManagerInfo objects
            if (count > 0)
            {
                // find the object that contains the specified listener
                for (int i = 0; i < count; i++)
                {
                    // get an object from the vector
                    smd = (SoundManagerInfo) v.elementAt(i);

                    // does this object contain the specified listener?
                    if (smd.listener == listener)
                    {
                        // if (Logging.LOGGING)
                        // {
                        // log.debug ("getHandle - found it");
                        // }
                        // yes, we are done. return the sndHandle.
                        return (smd.sndHandle);
                    }
                }
            }
        }

        // if there is no vector, an empty vector, or we didn't find an
        // object with the specified listener, then return a sndHandle of 0
        SystemEventUtil.logRecoverableError(new Exception("SoundMgr::getHandle - could not find a sndHandle for the "
                + "specified listener"));

        return 0;
    }

    /**
     * Writes the contents of the <code>SoundManagerInfo</code> data objects to
     * the debug log.
     * 
     * Writes the specified <code>String</code> into the debug log along with
     * the contents of the <code>SoundManagerInfo</code> objects.
     * 
     * @param title
     *            the String to display with the data. private void
     *            dumpSoundManagerInfo (String title) { if (Logging.LOGGING) {
     *            if (v != null) { int count = v.size();
     * 
     *            if (count > 0) { for (int i=0; i<count; i++) {
     *            SoundManagerInfo smd = (SoundManagerInfo)v.elementAt(i);
     *            log.debug ("dumpSoundManagerInfo - " + title + " elementAt(" +
     *            i + ") - sndHandle = " + Integer.toString(smd.sndHandle) +
     *            " (" + Integer.toHexString(smd.sndHandle) + "), " +
     *            "listener = " + smd.listener ); } } } } }
     */

    /**********************************************************************/
    /*
     * native methods
     */

    /**
     * Returns an MPEOS Sound handle (mpe_sndHandle). If an error occurs, the
     * handle will be zero.
     * 
     * @param data
     *            sound data to be played.
     * 
     * @param size
     *            the number of bytes of sound data.
     * 
     * @param mimeTypeID
     *            ID that specifies the MIME type of the sound.
     * 
     * @return sndHandle Handle to use when calling MPEOS sound functions.
     */
    private native int nSoundCreate(byte[] data, int size, int mimeTypeID);

    /**
     * Play a sound specified by sndHandle.
     * 
     * @param sndHandle
     *            The sound to stop playing
     * 
     * @param loop
     *            boolean that indicates that the sound is to be played in loop
     *            mode.
     * 
     * @return true if successful
     */
    private native boolean nSoundPlay(int sndHandle, boolean loop);

    /**
     * Stop playing the sound specified by sndHandle.
     * 
     * @param sndHandle
     *            The sound to stop playing.
     */
    private native void nSoundStop(int sndHandle);

    /**
     * Release all resources associated with the specified sndHandle.
     * 
     * @param sndHandle
     *            The sound to stop playing.
     */
    private native void nSoundDelete(int sndHandle);

    /**
     * Get the current media time position, in nanoseconds.
     * 
     * @param sndHandle
     *            The sound to stop playing.
     * 
     * @return The current media time position, in nanoseconds.
     */
    private native long nSoundGetMediaTimePosition(int sndHandle);

    /**
     * Set a new media time position.
     * 
     * @param sndHandle
     *            The sound to stop playing.
     * 
     * @param nanoseconds
     *            representing the desired media time position.
     */
    private native void nSoundSetMediaTimePosition(int sndHandle, long nanoseconds);

    /**
     * Get the maximum size, in bytes, to use for the sound data byte[].
     * 
     * @return maximum size, in bytes, to use for the sound data byte[].
     */
    private native int nSoundGetMaxArraySize();

    /**
     * Determine if a sound can be played.
     * 
     * @see org.cablelabs.impl.manager.SoundManager
     * 
     * @param mimeTypeID
     *            ID of the mimeType to be played.
     */
    private native boolean nSoundIsSupported(int mimeTypeID);

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }

    public Sound createSound(String mimeType, byte[] data, int offset, int size)
    {
        // TODO (Josh) Implement
        return null;
    }

}

/**
 * Data object that is used to map <code>listener</code> to
 * <code>sndHandle</code>.
 */
class SoundManagerInfo
{
    int sndHandle;

    SoundManagerListener listener;

    // SoundManagerInfo()
    // {
    // }

    SoundManagerInfo(int newSndHandle, SoundManagerListener newListener)
    {
        sndHandle = newSndHandle;
        listener = newListener;
    }

}
