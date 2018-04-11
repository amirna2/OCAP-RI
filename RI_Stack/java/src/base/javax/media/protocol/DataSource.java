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

package javax.media.protocol;

import javax.media.*;
import javax.media.Duration;
import java.io.IOException;

/**
 * A <CODE>DataSource</CODE> is an abstraction for media protocol-handlers.
 * <CODE>DataSource</CODE> manages the life-cycle of the media source by
 * providing a simple connection protocol.
 * 
 * <h2>Source Controls</h2>
 * 
 * A <code>DataSource</code> might support an operation that is not part of the
 * <code>DataSource</code> class definition. For example a source could support
 * positioning its media to a particular time. Some operations are dependent on
 * the data stream that the source is managing, and support cannot be determined
 * until after the source has been connected.
 * <p>
 * 
 * To obtain all of the objects that provide control over a
 * <code>DataSource</code>, use <code>getControls</code> which returns an array
 * of <code>Object</code> To determine if a particular kind of control is
 * available and obtain the object that implements it, use
 * <code>getControl</code> which takes the name of the Class or Interface that
 * of the desired control.
 * 
 * 
 * @see Manager
 * @see DefaultPlayerFactory
 * @see Positionable
 * @see RateConfigureable
 * 
 * @version 1.17, 07/09/19
 */
abstract public class DataSource implements Controls, Duration
{

    MediaLocator sourceLocator;

    /**
     * A no-argument constructor required by pre 1.1 implementations so that
     * this class can be instantiated by calling <CODE>Class.newInstance</CODE>.
     * 
     */
    public DataSource()
    {
        sourceLocator = null;
    }

    /**
     * Construct a <CODE>DataSource</CODE> from a <CODE>MediaLocator</CODE>.
     * This method should be overloaded by subclasses; the default
     * implementation just keeps track of the <CODE>MediaLocator</CODE>.
     * 
     * 
     * @param source
     *            The <CODE>MediaLocator</CODE> that describes the
     *            <CODE>DataSource</CODE>.
     */
    public DataSource(MediaLocator source)
    {
        sourceLocator = null;
        setLocator(source);
    }

    /**
     * Set the connection <CODE>source</CODE> for this <CODE>DataSource</CODE>.
     * This method should only be called once; an error is thrown if the locator
     * has already been set.
     * 
     * @param source
     *            The <CODE>MediaLocator</CODE> that describes the media source.
     */
    public void setLocator(MediaLocator source)
    {
        if (sourceLocator == null)
        {
            sourceLocator = source;
        }
        else
        {
            // $jdr: Should we name the error here?
            throw new java.lang.Error("Locator already set on DataSource.");
        }
    }

    /**
     * Get the <CODE>MediaLocator</CODE> that describes this source. Returns
     * <CODE>null</CODE> if the locator hasn't been set. (Very unlikely.)
     * 
     * @return The <CODE>MediaLocator</CODE> for this source.
     */
    public MediaLocator getLocator()
    {
        return sourceLocator;
    }

    /**
     * Check to see if this connection has been initialized with a
     * <CODE>MediaLocator</CODE>. If the connection hasn't been initialized,
     * <CODE>initCheck</CODE> throws an <CODE>Error</CODE>. Most methods should
     * call <CODE>initCheck</CODE> on entry.
     * 
     * @throws java.lang.Error
     *             if the connection has not been initialized.
     */
    protected void initCheck()
    {
        if (sourceLocator == null)
        {
            // $jdr: This should a real media error.
            throw new java.lang.Error("Uninitialized DataSource error.");
        }
    }

    /**
     * Get a string that describes the content-type of the media that the source
     * is providing.
     * <p>
     * It is an error to call <CODE>getContentType</CODE> if the source is not
     * connected.
     * 
     * @return The name that describes the media content.
     */
    public abstract String getContentType();

    /**
     * Open a connection to the source described by the
     * <CODE>MediaLocator</CODE>.
     * <p>
     * 
     * The <CODE>connect</CODE> method initiates communication with the source.
     * 
     * @exception IOException
     *                Thrown if there are IO problems when <CODE>connect</CODE>
     *                is called.
     */
    public abstract void connect() throws IOException;

    /**
     * Close the connection to the source described by the locator.
     * <p>
     * The <CODE>disconnect</CODE> method frees resources used to maintain a
     * connection to the source. If no resources are in use,
     * <CODE>disconnect</CODE> is ignored. If <CODE>stop</CODE> hasn't already
     * been called, calling <CODE>disconnect</CODE> implies a stop.
     * 
     */
    public abstract void disconnect();

    /**
     * Initiate data-transfer. The <CODE>start</CODE> method must be called
     * before data is available. (You must call <CODE>connect</CODE> before
     * calling <CODE>start</CODE>.)
     * 
     * @exception IOException
     *                Thrown if there are IO problems with the source when
     *                <CODE>start</CODE> is called.
     */
    public abstract void start() throws IOException;

    /**
     * Stop the data-transfer. If the source has not been connected and started,
     * <CODE>stop</CODE> does nothing.
     */
    public abstract void stop() throws IOException;

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
