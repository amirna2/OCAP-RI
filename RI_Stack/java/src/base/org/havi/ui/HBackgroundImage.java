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

/*
 * Copyright 2000-2003 by HAVi, Inc. Java is a trademark of Sun
 * Microsystems, Inc. All rights reserved.  
 */

package org.havi.ui;

import java.net.*;
import org.havi.ui.event.*;
import org.cablelabs.impl.havi.*;

/**
 * This class represents a background image. Images of this class can be used as
 * full screen backgrounds outside the java.awt framework.
 * 
 * <hr>
 * The parameters to the constructors are as follows, in cases where parameters
 * are not used, then the constructor should use the default values.
 * <p>
 * <h3>Default parameter values exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Parameter</th>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=5>None.</td>
 * </tr>
 * </table>
 * <h3>Default parameter values not exposed in the constructors</h3>
 * <table border>
 * <tr>
 * <th>Description</th>
 * <th>Default value</th>
 * <th>Set method</th>
 * <th>Get method</th>
 * </tr>
 * <tr>
 * <td colspan=4>None.</td>
 * </tr>
 * </table>
 * 
 * @author Alex Resh
 * @author Todd Earles
 * @version 1.1
 */
public class HBackgroundImage
{
    /**
     * Create an HBackgroundImage object. Loading of the data for the object
     * shall not happen at this time.
     * 
     * @param filename
     *            the name of the file to use as the source of data in a
     *            platform-specific URL format.
     */
    public HBackgroundImage(String filename)
    {
        this.filename = filename;

        if (filename == null) throw new IllegalArgumentException("filename cannot be null");

    }

    /**
     * Create an HBackgroundImage object from an array of bytes encoded in the
     * same encoding format as when reading this type of image data from a file.
     * <p>
     * If this constructor succeeds then the object will automatically be in the
     * loaded state and calling the {@link org.havi.ui.HBackgroundImage#load}
     * method shall immediately generate an
     * {@link org.havi.ui.event.HBackgroundImageEvent} reporting success.
     * <p>
     * If the byte array does not contain a valid image then this constructor
     * may throw a <code>java.lang.IllegalArgumentException</code>.
     * <p>
     * Calling the {@link org.havi.ui.HBackgroundImage#flush} method on an
     * object built with this constructor shall have no effect.
     * 
     * @param pixels
     *            the data for the HBackgroundImage object encoded in the
     *            specified format for image files of this type.
     */
    public HBackgroundImage(byte[] pixels)
    {
        this.pixels = pixels;

        if (pixels == null) throw new IllegalArgumentException("pixels cannot be null");
    }

    /**
     * Create an HBackgroundImage object. Loading of the data for the object
     * shall not happen at this time.
     * 
     * @param contents
     *            a URL referring to the data to load.
     */
    public HBackgroundImage(java.net.URL contents)
    {
        imageURL = contents;

        if (contents == null) throw new IllegalArgumentException("contents cannot be null");

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
     * 
     * @see org.havi.ui.event.HBackgroundImageEvent
     */
    public void load(HBackgroundImageListener l)
    {
        // If not already allocated, then allocate a new platform and/or media
        // specific background image object.
        getImpl();

        // Defer to the platform specific implementation
        if (impl != null) impl.load(l);
    }

    /**
     * Used within the implementation to get access to the <i>impl</i> object.
     * 
     * @return an implementation of <code>HBackgroundImage</code>
     */
    synchronized HBackgroundImage getImpl()
    {
        // If not already allocated, then allocate a new platform and/or media
        // specific background image object.
        if (impl == null)
        {
            if (filename != null)
                impl = HaviToolkit.getToolkit().createBackgroundImage(this, filename);
            else if (imageURL != null)
                impl = HaviToolkit.getToolkit().createBackgroundImage(this, imageURL);
            else if (pixels != null)
                impl = HaviToolkit.getToolkit().createBackgroundImage(this, pixels);
            else
                throw new RuntimeException("Implementation error - null filename/url/pixels");
        }
        return impl;
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
    public int getHeight()
    {
        // Defer to the platform specific implementation
        if (impl != null)
            return impl.getHeight();
        else
            return -1;
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
    public int getWidth()
    {
        // Defer to the platform specific implementation
        if (impl != null)
            return impl.getWidth();
        else
            return -1;
    }

    /**
     * Flush all the resources used by this image. This includes any pixel data
     * being cached as well as all underlying system resources used to store
     * data or pixels for the image. After calling this method the image is in a
     * state similar to when it was first created without any load method having
     * been called. When this method is called, the image shall not be in use by
     * an application. Resources related to any
     * {@link org.havi.ui.HBackgroundDevice} are not released.
     */
    public void flush()
    {
        // Defer to the platform specific implementation
        if (impl != null)
        {
            impl.flush();
            impl = null;
        }
    }

    /** Platform and/or media specific {@link HBackgroundImage} */
    private HBackgroundImage impl;

    /** The image filename or null if not constructed with a filename */
    protected String filename;

    /** The image URL or null if not constructed with a URL */
    protected URL imageURL;

    /** The image pixels or null if not constructed with pixels. */
    protected byte[] pixels;
}
