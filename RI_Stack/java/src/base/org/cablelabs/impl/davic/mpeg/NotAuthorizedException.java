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
 * Created on Jul 13, 2006
 */
package org.cablelabs.impl.davic.mpeg;

import org.davic.mpeg.ElementaryStream;
import org.davic.mpeg.Service;
import org.davic.mpeg.TransportStream;

/**
 * Extension of {@link org.davic.mpeg.NotAuthorizedException} that provides
 * constructors that can set all instance variables.
 * 
 * @author Aaron Kamienski
 */
//findbugs complains about this pattern - shadowing superclass' name.
//Unfortunately, its a common pattern in the RI (so we ignore it).
public class NotAuthorizedException extends org.davic.mpeg.NotAuthorizedException
{
    /**
     * Creates an instance of <code>NotAuthorizedException</code>.
     * 
     * @param major
     *            the major reasons why the service or elementary stream was not
     *            authorized
     * @param minor
     *            the minor reasons why the service or elementary stream was not
     *            authorized
     * 
     * @see #setElementaryStreams
     * @see #setService
     */
    public NotAuthorizedException(int major, int minor)
    {
        super();
        this.reasons = new int[][] { new int[] { major, minor } };
    }

    /**
     * Creates an instance of <code>NotAuthorizedException</code>.
     * 
     * @param service
     *            the service that is not authorized
     * @param reasons
     *            the reasons why the service was not authorized
     */
    public NotAuthorizedException(Service service, int[] reasons)
    {
        super("Service not authorized: " + service);
        this.type = SERVICE;
        this.service = service;
        this.reasons = new int[][] { reasons };
    }

    /**
     * Creates an instance of <code>NotAuthorizedException</code>.
     * 
     * @param streams
     *            the elementary streams that were not authorized
     * @param reasons
     *            the reasons why the streams were not authorized
     */
    public NotAuthorizedException(ElementaryStream[] streams, int[][] reasons)
    {
        super("Streams not authorized: " + toString(streams));
        this.type = ELEMENTARY_STREAM;
        this.streams = streams;
        this.reasons = reasons;
    }

    // Description copied from super
    public ElementaryStream[] getElementaryStreams()
    {
        return streams;
    }

    // Description copied from super
    public int[] getReason(int index) throws IndexOutOfBoundsException
    {
        return reasons[index];
    }

    // Description copied from super
    public Service getService()
    {
        return service;
    }

    // Description copied from super
    public int getType()
    {
        return type;
    }

    /**
     * Overrides {@link Throwable#getMessage()} to allow for updating the
     * detailed message in response to {@link #setElementaryStreams}.
     */
    public String getMessage()
    {
        return (message != null) ? message : super.getMessage();
    }

    /**
     * Sets the elementary streams to be returned by
     * {@link #getElementaryStreams()} if they have not already been set by
     * {@link #NotAuthorizedException(ElementaryStream[], int[][])}. This method
     * has no effect if elementary streams or {@link #getService service} has
     * already been {@link #setService set}.
     * <p>
     * This also sets the {@link #getType type} to {@link #ELEMENTARY_STREAM}
     * and updates the {@link #getMessage message}.
     * 
     * @param streams
     *            the elementary streams that were not authorized
     * 
     * @see #NotAuthorizedException(int,int)
     */
    public void setElementaryStreams(ElementaryStream[] streams)
    {
        if (this.streams != null || this.service != null) return;

        this.streams = streams;
        this.type = ELEMENTARY_STREAM;
        this.message = "Streams not authorized: " + toString(streams);
    }

    /**
     * Sets the elementary streams to be returned by
     * {@link #getElementaryStreams} if they have not already been set by
     * {@link #NotAuthorizedException(ElementaryStream[], int[][])}. This method
     * has no effect if elementary streams or {@link #getService service} has
     * already been {@link #setService set}. The given
     * <code>TransportStream</code> is searched for an
     * <code>ElementaryStream</code> that matches the given <i>pid</i>.
     * <p>
     * This also sets the {@link #getType type} to {@link #ELEMENTARY_STREAM}
     * and updates the {@link #getMessage message}.
     * 
     * @param ts
     *            the transport stream to search for the desired elementary
     *            stream
     * @param pid
     *            the PID for the desired elementary stream
     * 
     * @see #NotAuthorizedException(int,int)
     */
    public void setElementaryStreams(TransportStream ts, int pid)
    {
        setElementaryStreams(new ElementaryStream[] { findElementaryStream(ts, pid) });
    }

    /**
     * Sets the service to be returned by {@link #getService()} if it has not
     * already been set by {@link #NotAuthorizedException(Service, int[])}. This
     * method has no effect if service or {@link #getElementaryStreams
     * elementary streams} has already been {@link #setElementaryStreams set}.
     * <p>
     * This also sets the {@link #getType type} to {@link #SERVICE} and updates
     * the {@link #getMessage message}.
     * 
     * @param service
     *            the service that is not authorized
     * 
     * @throws SecurityException
     *             if elementary streams or service has already been set
     * 
     * @see #NotAuthorizedException(int,int)
     */
    public void setService(Service service)
    {
        if (this.streams != null || this.service != null) return;
        this.service = service;
        this.type = SERVICE;
        this.message = "Service not authorized: " + service;
    }

    /**
     * Returns the <code>ElementaryStream</code> for the given
     * <code>TransportStream</code> and <i>pid</i>.
     * 
     * @param ts
     *            the transport stream
     * @param pid
     *            the PID specifying the elementary stream
     * @return the specified <code>ElementaryStream</code> or <code>null</code>
     */
    private static ElementaryStream findElementaryStream(TransportStream ts, int pid)
    {
        Service[] svcs = ts.retrieveServices();
        if (svcs != null)
        {
            for (int i = 0; i < svcs.length; ++i)
            {
                ElementaryStream es[] = svcs[i].retrieveElementaryStreams();
                if (es != null)
                {
                    for (int j = 0; j < es.length; ++j)
                    {
                        if (es[j].getPID() == pid) return es[j];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Creates a string representation for the given streams.
     * 
     * @param streams
     *            elementary streams
     * @return string representation for the given streams.
     */
    private static String toString(ElementaryStream[] streams)
    {
        if (streams == null)
            return "null";
        else
        {
            StringBuffer sb = new StringBuffer();

            sb.append('[');
            for (int i = 0; i < streams.length; ++i)
            {
                sb.append(streams[i]).append(',');
            }
            return sb.append("]").toString();
        }
    }

    /**
     * @see #getType()
     */
    private int type;

    /**
     * @see #getService()
     */
    private transient Service service;

    /**
     * @see #getElementaryStreams()
     */
    private transient ElementaryStream[] streams;

    /**
     * @see #getReason(int)
     */
    private int[][] reasons;

    /**
     * @see #getMessage()
     */
    private String message;
}
