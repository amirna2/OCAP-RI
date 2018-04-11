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

package org.cablelabs.impl.dvb.dsmcc;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.tv.service.Service;

import org.davic.net.Locator;
import org.dvb.dsmcc.DSMCCObject;
import org.dvb.dsmcc.IllegalObjectTypeException;
import org.dvb.dsmcc.NotLoadedException;

/**
 * @author Eric Koldinger
 */
/**
 * @author koldinger
 * 
 */
public interface ObjectCarousel
{
    // file type definitions (to match mpe_file.h)
    // THESE MUST MATCH MPE_FILE.H OR HORRIBLE THINGS WILL HAPPEN
    // and your plants will turn brown and die.
    public static final int TYPE_UNKNOWN = 0x0000;

    public static final int TYPE_FILE = 0x0001;

    public static final int TYPE_DIR = 0x0002;

    public static final int TYPE_STREAM = 0x0003;

    public static final int TYPE_STREAMEVENT = 0x0004;

    public static final int TYPE_OTHER = 0xffff;

    /**
     * Retrieve the service containing this carousel.
     * 
     * @return
     */
    public Service getService();

    /**
     * Get the locator used to mount this carousel.
     * 
     * @return The locator.
     */
    public Locator getLocator();

    /**
     * Get an NSAP Address for this carousel.
     * 
     * @return A 20 byte array containing the NSAP address. Null if NSAP doesn't
     *         make sense for this carousel.
     */
    public byte[] getNSAPAddress();

    /**
     * Detach a ServiceDomain from this carousel. If the number of attached
     * domains goes to 0, the carousel will be unmounted.
     */
    public void detach();

    /**
     * Return the mountpoint for this carousel.
     * 
     * @return The mountpoint.
     */
    public String getMountPoint() throws NotLoadedException, FileNotFoundException;

    /**
     * Does the carousel match the specified locator?
     * 
     * @param l
     *            The locator to check against this carousel.
     * @return True if this carousel can be specified with this locator, false
     *         otherwise.
     */
    public boolean match(Locator l);

    /**
     * Retrieve a stream or stream event object from within an object carousel.
     * 
     * @param path
     *            The path to the object, including the mount point.
     * @return The appropriate stream object
     * @throws IOException
     *             If any read activity fails.
     * @throws IllegalObjectTypeException
     *             If path is not a stream or stream event.
     */
    public DSMCCStreamInterface getStreamObject(String path) throws IOException, IllegalObjectTypeException;

    /**
     * Determine if the current connection is available.
     */
    public boolean isNetworkConnectionAvailable();

    /**
     * Add a listener for connection changes.
     * 
     * @param listener
     */
    public void addConnectionListener(ObjectCarouselConnectionListener listener);

    /**
     * Remove a previously registered listener.
     * 
     * @param listener
     */
    public void removeConnectionListener(ObjectCarouselConnectionListener listener);

    /**
     * @param path
     * @return
     * @throws IOException
     */
    public boolean getFileInfoIsKnown(String path) throws IOException;

    /**
     * @param path
     */
    public void prefetchFile(String path);

    /**
     * @param path
     * @param targetNSAP
     * @return
     * @throws IOException
     */
    public String resolveServiceXfer(String path, byte[] targetNSAP) throws IOException;

    /**
     * @param realPath
     * @return
     * @throws IOException
     */
    public int getFileType(String realPath) throws IOException;

    /**
     * @param path
     * @param obj
     * @param callback
     * @return
     * @throws IOException
     */
    public Object enableObjectChangeEvents(String path, DSMCCObject obj, ObjectChangeCallback callback)
            throws IOException;

    /**
     * @param objectChangeHandle
     */
    public void disableChangeEvents(Object objectChangeHandle);
}
