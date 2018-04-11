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

package org.cablelabs.impl.havi;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.havi.ui.HBackgroundImage;
import org.havi.ui.HEventMulticaster;
import org.havi.ui.event.HBackgroundImageEvent;
import org.havi.ui.event.HBackgroundImageListener;

/**
 * The <code>MpegBackgroundImage</code> class extends {@link HBackgroundImage}
 * to provide an implementation based upon MPEG-2 I-Frames.
 * <p>
 * <i>Note that this implementation synchronizes on itself because that
 * shouldn't cause a problem for other applications. The background image object
 * isn't shared. </i>
 * 
 * @author Aaron Kamienski
 */
public class MpegBackgroundImage extends HBackgroundImage implements Runnable
{
    /** The loaded mpeg i-frame. */
    private int nHandle = 0;

    /** The size of the loaded frame. */
    private Dimension size = new Dimension(-1, -1);

    /** The background image object to be reported to the listener */
    private HBackgroundImage hbi;

    /** The set of listeners. */
    private HBackgroundImageListener listeners;

    /**
     * Indicates whether we are currently loading or not. This is mainly used to
     * prevent multiple asy
     */
    private boolean loading;

    /**
     * Thread used for asynchronous loading.
     */
    private Thread async;

    /**
     * A lock used during loading. This is used to prevent dual simultaneous
     * loads from occuring (one asynchronous and one synchronous).
     */
    private Object loadLock = new Object();

    public MpegBackgroundImage(HBackgroundImage hbi, String filename)
    {
        super(filename);
        this.hbi = hbi;
    }

    public MpegBackgroundImage(HBackgroundImage hbi, URL imageURL)
    {
        super(imageURL);
        this.hbi = hbi;
    }

    public MpegBackgroundImage(HBackgroundImage hbi, byte[] pixels)
    {
        super(pixels);
        this.hbi = hbi;
        nHandle = 0;
    }

    /**
     * Load the data for this object. This method is asynchronous. The
     * completion of data loading is reported through the listener provided.
     * <p>
     * Multiple calls to <code>load</code> shall each add an extra listener, all
     * of which are informed when the loading is completed. If load is called
     * with the same listener more than once, the listener shall then receive
     * multiple copies of a single event.
     * 
     * @param l
     *            the listener to call when loading of data is completed.
     */
    public synchronized void load(HBackgroundImageListener l)
    {
        // if already loaded, then notify listener directly
        if (isLoaded())
            notifyLater(l);
        else
        {
            // add listener
            listeners = HEventMulticaster.add(listeners, l);

            // if not loading start loading
            if (!loading)
            {
                async = new Thread(this);
                async.start();
                loading = true;
            }
        }
    }

    /**
     * Determines the height of the image. This is returned in pixels as defined
     * by the format of the image concerned. If this information is not known
     * when this method is called then -1 is returned.
     * <p>
     * The image must have been successfully loaded to completion before this
     * information is guaranteed to be available. It is implementation specific
     * whether this information is available before the image is successfully
     * loaded to completion. An image whose loading failed for any reason shall
     * be considered as having this information unavailable.
     * 
     * @return the height of the image
     */
    public synchronized int getHeight()
    {
        return size.height;
    }

    /**
     * Determines the width of the image. This is returned in pixels as defined
     * by the format of the image concerned. If this information is not known
     * when this method is called then -1 is returned.
     * <p>
     * The image must have been successfully loaded to completion before this
     * information is guaranteed to be available. It is implementation specific
     * whether this information is available before the image is successfully
     * loaded to completion. An image whose loading failed for any reason shall
     * be considered as having this information unavailable.
     * 
     * @return the width of the image
     */
    public synchronized int getWidth()
    {
        return size.width;
    }

    /**
     * Flush all the resources used by this image. This includes any pixel data
     * being cached as well as all underlying system resources used to store
     * data or pixels for the image. After calling this method the image is in a
     * state similar to when it was first created without any load method having
     * been called. When this method is called, the image shall not be in use by
     * an application. Resources related to any
     * {@link org.havi.ui.HBackgroundDevice HBackgroundDevice} are not released.
     */
    public synchronized void flush()
    {
        size = null;

        int tmp = nHandle;
        nHandle = 0;
        nDispose(tmp);
    }

    /**
     * Loads the image from the file or URL asynchronously.
     * 
     */
    public void run()
    {
        loadNow();
    }

    /**
     * Loads the MPEG I-frame immediately. All pending listeners will be
     * notified of the outcome.
     * 
     * <em>Implmentation notes</em>
     * <p>
     * Note that it is possible for two loads to be ongoing. If load() is called
     * and then loadNow() is called directly (by the implementation so that it
     * can actually display an I-frame) or vice versa. This really isn't such a
     * big deal. Whomever finishes first will notify the listeners, but likely
     * have their references to data overwritten.
     * <p>
     * We can easily avoid the loadNow() followed by load() case by having
     * loadNow() set (within the same synchronized block used to test) the
     * loading variable. This would prevent load() from firing up the
     * asynchronous load and just wait for loadNow() to finish.
     * <p>
     * However, if an async load is in progress and loadNow is called... We
     * would have to block waiting on the async load... This could be done by
     * adding a listener, or perhaps synchronizing on a loading object... That
     * seems to be the ticket... introduce a new lock that is the loading
     * object. When the lock is held, somebody is currently loading. A new
     * loadNow() call will have to wait, and once it gets the lock it will find
     * it has nothing to do!
     * 
     * @return the HBackgroundImageEvent identifier indicating outcome of
     *         loading operation (will be
     *         {@link HBackgroundImageEvent#BACKGROUNDIMAGE_LOADED} if
     *         successful)
     */
    public int loadNow()
    {
        int id = HBackgroundImageEvent.BACKGROUNDIMAGE_LOADED;
        // This try block is used to break out of the code in case of an error.
        // Prior to a potential error "id" is set to the event id specifying the
        // error.
        // Also, the thrown exception is used to release the loadLock prior to
        // notifying listeners.
        try
        {
            // This prevents multiple loads from occurring at the same
            // time.
            // When one finishes, the other will acquire the lock and
            // reap the rewards.
            // If the first failed, the second will go ahead and try again.
            // Which seems okay, except what if the second one succeeds???
            // !!!!All of the listeners were called (and forgotten) by the
            // first!!!
            synchronized (loadLock)
            {
                // If not already loaded, then load
                if (!isLoaded())
                {
                    // Initial error would be file not found
                    id = HBackgroundImageEvent.BACKGROUNDIMAGE_FILE_NOT_FOUND;

                    // Get an input stream and the size of the data
                    InputStream is = null;
                    int size = 0;
                    if (filename != null)
                    {
                        File f = new File(filename);
                        size = (int) f.length();
                        is = new FileInputStream(filename);
                    }
                    else
                    {
                        URLConnection connect = imageURL.openConnection();
                        size = connect.getContentLength();
                        is = connect.getInputStream();
                    }
                    // If there isn't anything to read, treat as file not found
                    if (size == 0) throw new FileNotFoundException("File was empty");

                    // Any further errors would be considered io errors
                    id = HBackgroundImageEvent.BACKGROUNDIMAGE_IOERROR;

                    // Read input stream (hopefully in one read)
                    byte buffer[] = new byte[size];
                    int pos = 0;
                    int n;
                    while (size > 0 && (n = is.read(buffer, pos, size)) != -1)
                    {
                        pos += n;
                        size -= n;
                    }
                    if (size != 0) throw new IOException("Couldn't read entire file");

                    // following error is invalid i-frame
                    id = HBackgroundImageEvent.BACKGROUNDIMAGE_INVALID;
                    // Added for findbugs issues fix - Moved the nCreate method to the synchronized block
                    synchronized (this)
                    {
                    // Now create the native BGImage handle
                    // And check to see if it's a valid I-Frame
                    // And get the actual size
                    int handle = nCreate(buffer, this.size);
                    if (handle == 0) throw new IllegalArgumentException("Not a valid I-Frame");

                    // All is well, save new information for later access
                        this.loading = false;
                        this.async = null; // free up the async thread for GC
                        this.nHandle = handle; // remember the native handle
                        id = HBackgroundImageEvent.BACKGROUNDIMAGE_LOADED;
                    }
                } // if (!isLoaded())
            } // synchronized(loadLock)
        }
        catch (Exception e)
        {
            // id contains the error given an exception
            // the exception releases the loadLock
        }

        // Let the listeners know
        return notifyListeners(id);
    }

    /**
     * Returns <code>true</code> if the image has been loaded into memory.
     * 
     * @return <code>true</code> if the image has been loaded into;
     *         <code>false</code> otherwise
     */
    private synchronized boolean isLoaded()
    {
        return nHandle != 0;
    }

    /**
     * Clears the current set of listeners and returns the current set of
     * listeners.
     * 
     * @return the current set of listeners (before nullifying the set)
     */
    private synchronized HBackgroundImageListener clearListeners()
    {
        HBackgroundImageListener l = listeners;
        listeners = null;
        return l;
    }

    /**
     * Notifies the given listener of successful image loading at a later time.
     * This is called when <code>load()</code> is called and it is known that
     * the image has already been loaded.
     * 
     * @param l
     *            the listener to notify of successful loading
     */
    private void notifyLater(final HBackgroundImageListener l)
    {
        if (l == null) return;

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext ctx = ccm.getCurrentContext();

        ctx.runInContext(new Runnable()
        {
            public void run()
            {
                notifyListeners(l, HBackgroundImageEvent.BACKGROUNDIMAGE_LOADED);
            }
        });
    }

    /**
     * Notifies listeners of the outcome of image loading. As part of
     * notification, all current listeners are forgotten.
     * 
     * @param id
     *            the event id
     * @return the HBackgroundImageEvent identifier indicating outcome of
     *         loading operation (will be
     *         {@link HBackgroundImageEvent#BACKGROUNDIMAGE_LOADED} if
     *         successful)
     */
    private int notifyListeners(int id)
    {
        return notifyListeners(clearListeners(), id);
    }

    /**
     * Notifies listeners of the outcome of image loading.
     * 
     * @param listeners
     *            the listeners to notify
     * @param id
     *            the event id
     * @return the HBackgroundImageEvent identifier indicating outcome of
     *         loading operation (will be
     *         {@link HBackgroundImageEvent#BACKGROUNDIMAGE_LOADED} if
     *         successful)
     */
    private int notifyListeners(HBackgroundImageListener listeners, int id)
    {
        if (listeners != null)
        {
            HBackgroundImageEvent e = new HBackgroundImageEvent(hbi, id);

            if (id == HBackgroundImageEvent.BACKGROUNDIMAGE_LOADED)
                listeners.imageLoaded(e);
            else
                listeners.imageLoadFailed(e);
        }
        return id;
    }

    /**
     * Displays the image to the given native device.
     * 
     * @param nDevice
     *            the native device
     * @param r
     *            screen placement relative to the device
     * 
     * @throws IllegalArgumentException
     */
    public void displayImage(int nDevice, Rectangle r) throws IllegalArgumentException
    {
        switch (nDisplay(nHandle, nDevice, r))
        {
            case 0:
                return;
            case 1:
            case 2:
                throw new IllegalArgumentException("Image could not be displayed");
        }
    }

    /**
     * Initializes the JNI layer. Called from static initializer.
     */
    private static native void nInit();

    /**
     * Creates the native handle and returns the actual size, after verifying
     * that it is a valid I-Frame.
     * 
     * @param data
     *            the supposed i-frame
     * @param dim
     *            the <code>Dimension</code> object to fill with the width and
     *            height of the image
     * @return the native handle or <code>0</code> if invalid
     */
    private static native int nCreate(byte[] data, Dimension dim);

    /**
     * Deletes the native handle and frees up any resources.
     * 
     * @param nHandle
     *            the native handle
     */
    private static native void nDispose(int nHandle);

    /**
     * Displays the native image to the given native device.
     * 
     * @param nImage
     *            the native image
     * @param nDevice
     *            the native device
     * @param r
     *            on-screen rectangle
     * @return 0 for success, 1 for unsupported operation, 2 for illegal image
     */
    private static native int nDisplay(int nImage, int nDevice, Rectangle r);

    /**
     * Static initializer invokes
     * {@link org.cablelabs.impl.ocap.OcapMain#loadLibrary} and {@link #nInit}.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }
}
