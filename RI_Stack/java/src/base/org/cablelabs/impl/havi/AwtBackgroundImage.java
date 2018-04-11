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

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Hashtable;

import org.havi.ui.HBackgroundImage;
import org.havi.ui.event.HBackgroundImageEvent;
import org.havi.ui.event.HBackgroundImageListener;

/**
 * The {@link AwtBackgroundImage} class extends {@link HBackgroundImage} to
 * provide an implementation based on <code>java.awt.Image</code>.
 * 
 * @author Todd Earles
 * @version $Revision: 1.5 $ $Date: 2002/11/07 21:13:40 $
 */
public class AwtBackgroundImage extends HBackgroundImage
{
    /** The AWT image */
    private Image awtImage;

    /** A flag to indicate that the AWT image is loaded */
    private boolean loaded = false;

    /** The background image object to be reported to the listener */
    private HBackgroundImage hbi;

    /**
     * The hash table used to map platform netural {@link HBackgroundImage}
     * objects the their corresponding platform dependent implentation objects.
     */
    private static final Hashtable hashtable = new Hashtable();

    /**
     * Constructs an {@link AwtBackgroundImage} object using the specified
     * filename.
     */
    public AwtBackgroundImage(HBackgroundImage hbi, String filename)
    {
        super(filename);
        this.hbi = hbi;

        // Add the mapping from the users HBackgroundImage to the platform
        // specific implementation object.
        hashtable.put(hbi, this);
    }

    /**
     * Constructs an {@link AwtBackgroundImage} object using the specified URL.
     */
    public AwtBackgroundImage(HBackgroundImage hbi, URL imageURL)
    {
        super(imageURL);
        this.hbi = hbi;

        // Add the mapping from the users HBackgroundImage to the platform
        // specific implementation object.
        hashtable.put(hbi, this);
    }

    /**
     * Constructs an {@link AwtBackgroundImage} object using the specified set
     * of pixels.
     */
    public AwtBackgroundImage(HBackgroundImage hbi, byte[] pixels)
    {
        super(pixels);
        this.hbi = hbi;

        // Add the mapping from the users HBackgroundImage to the platform
        // specific implementation object.
        hashtable.put(hbi, this);
    }

    // Definition copied from HBackgroundImage
    public void load(HBackgroundImageListener l)
    {
        // Load the AWT image
        loadAwtImage();

        // Use a separate thread to notify the listener once the image has been
        // loaded.
        if (l != null)
        {
            BackgroundImageLoader loader = new BackgroundImageLoader(l);
            loader.start();
        }
    }

    /**
     * Load the AWT image. This is a synchronized method to ensure that only the
     * initial call triggers the actual loading of the image. Subsequent calls
     * to this method (which may be from another thread) just return.
     */
    synchronized private void loadAwtImage()
    {
        // Only load the image if it is not already loaded
        if (loaded == false)
        {
            Toolkit tk = Toolkit.getDefaultToolkit();
            if (filename != null)
                awtImage = tk.getImage(filename);
            else if (imageURL != null)
                awtImage = tk.getImage(imageURL);
            else if (pixels != null) awtImage = tk.createImage(pixels);
            loaded = true;
        }
    }

    // Definition copied from HBackgroundImage
    public int getHeight()
    {
        return awtImage.getHeight(null);
    }

    // Definition copied from HBackgroundImage
    public int getWidth()
    {
        return awtImage.getWidth(null);
    }

    // Definition copied from HBackgroundImage
    public void flush()
    {
        awtImage.flush();
    }

    /**
     * Get the AWT image contained in this HBackgroundImage object.
     */
    public Image getAwtImage()
    {
        return awtImage;
    }

    /**
     * Lookup the implementation object associated with an HBackgroundImage
     * object. This is called by the StillImageBackgroundConfiguration.display
     * method since it only has an HBackgroundImage and must find the platform
     * specific implementation object.
     */
    public static AwtBackgroundImage getImpl(HBackgroundImage hbi)
    {
        return (AwtBackgroundImage) (hashtable.get(hbi));
    }

    /**
     * Thread that handles loading the image
     */
    private class BackgroundImageLoader extends Thread
    {
        // The listener to call after the image is loaded
        HBackgroundImageListener listener;

        // Thread constructor
        public BackgroundImageLoader(HBackgroundImageListener l)
        {
            super("BackgroundImageLoader");
            listener = l;
        }

        // Thread run method
        public void run()
        {
            // Load the image
            try
            {
                MediaTracker tracker = new MediaTracker(new Component()
                {
                });
                tracker.addImage(awtImage, 1);
                tracker.waitForID(1);
                if (tracker.isErrorID(1)) throw new RuntimeException("Cannot load background image");
            }
            catch (Exception e)
            {
                throw new RuntimeException("Cannot load background image");
            }

            // Notify listener (verified to be non-null by thread initiator)
            HBackgroundImageEvent evt = new HBackgroundImageEvent(hbi, HBackgroundImageEvent.BACKGROUNDIMAGE_LOADED);
            listener.imageLoaded(evt);
        }
    }
}
