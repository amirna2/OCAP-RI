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

import java.io.*;
import java.net.*;
import javax.media.MediaLocator;
import javax.media.Duration;
import javax.media.Time;

/**
 * A default data-source created directly from a <code>URL</code> using
 * <code>URLConnection</code>.
 * 
 * @see java.net.URL
 * @see java.net.URLConnection
 * 
 * @see InputSourceStream
 * @version 1.20, 05/10/13.
 * 
 */
public class URLDataSource extends PullDataSource
{

    protected URLConnection conn;

    protected ContentDescriptor contentType;

    private PullSourceStream source;

    protected boolean connected;

    /**
     * Implemented by subclasses.
     */
    protected URLDataSource()
    {
    }

    /**
     * Construct a <CODE>URLDataSource</CODE> directly from a <code>URL</code>.
     */
    public URLDataSource(URL url) throws IOException
    {
        setLocator(new MediaLocator(url));
        connected = false;
    }

    /*
     * Get the single-element array for this source.
     * 
     * @return The single-element array of source stream.
     */
    public PullSourceStream[] getStreams()
    {
        // $jdr: Is this necessary? See getContentType().
        if (!connected)
        {
            throw new java.lang.Error("Unconnected source.");
        }
        return new PullSourceStream[] { source };
    }

    /**
     * Initialize the connection with the source.
     * 
     * @exception IOException
     *                Thrown if there are problems setting up the connection.
     */
    public void connect() throws IOException
    {

        // Make the connect.
        conn = getLocator().getURL().openConnection();
        conn.connect();
        connected = true;

        // Figure out the content type.
        String mimeType = conn.getContentType();
        if (mimeType == null)
        {
            mimeType = ContentDescriptor.CONTENT_UNKNOWN;
        }
        contentType = new ContentDescriptor(mimeType);

        // Create a source stream.
        source = new URLPullSourceStream();

    }

    /**
     * Return the content type name.
     * 
     * @return The content type name.
     */
    public String getContentType()
    {
        // $jdr: We could probably get away with
        // not doing anything here, and connecting on
        // creation, given that this protocol is pretty
        // "connection-less".
        if (!connected)
        {
            throw new java.lang.Error("Source is unconnected.");
        }
        return contentType.getContentType();
    }

    /**
     * Disconnect the source.
     */
    public void disconnect()
    {
        if (connected)
        {
            ((URLPullSourceStream) source).dispose();
            connected = false;
        }
    }

    public void start() throws IOException
    {
        // Nothing to do here either.
    }

    /**
     * Stops the
     * 
     */
    public void stop() throws IOException
    {
        // sure.
    }

    /**
     * Returns <code>Duration.DURATION_UNKNOWN</code>. The duration is not
     * available from an <code>InputStream</code>.
     * 
     * @return <code>Duration.DURATION_UNKNOWN</code>.
     */
    public Time getDuration()
    {
        return Duration.DURATION_UNKNOWN;
    }

    /**
     * Returns an empty array, because this source doesn't provide any controls.
     * 
     * @return empty <code>Object</code> array.
     */
    public Object[] getControls()
    {
        return new Object[0];
    }

    /**
     * Returns null, because this source doesn't provide any controls.
     */
    public Object getControl(String controlName)
    {
        return null;
    }

    private class URLPullSourceStream implements PullSourceStream
    {

        private InputStream stream;

        private boolean atEOS;

        public URLPullSourceStream() throws IOException
        {
            stream = conn.getInputStream();
        }

        /**
         * Dispose of the stream resource.
         * 
         * @throws IOException
         */
        public void dispose()
        {
            try
            {
                stream.close();
            }
            catch (IOException x)
            {
                // Nothing can be done if close fails, so just ignore the
                // exception.
            }
        }

        /*
         * PullSourceStream interface implementation
         */

        public int read(byte[] buffer, int offset, int length) throws IOException
        {
            // Read the bytes from the InputStream.
            int count = stream.read(buffer, offset, length);
            // Set flag if at end of stream.
            atEOS = (count == -1);
            // Return number of bytes read.
            return count;
        }

        public boolean willReadBlock()
        {
            // If already at end-of-stream, return true.
            if (atEOS) return true;

            // Not already at EOS, so return false if no bytes are available to
            // read;
            // otherwise, return true.
            try
            {
                return stream.available() == 0;
            }
            catch (IOException e)
            {
                return true;
            }
        }

        /*
         * SourceStream interface implementation
         */

        public boolean endOfStream()
        {
            return atEOS;
        }

        public ContentDescriptor getContentDescriptor()
        {
            return contentType;
        }

        public long getContentLength()
        {
            int length = conn.getContentLength();
            return (length == -1) ? LENGTH_UNKNOWN : length;
        }

        /*
         * Controls interface implementation
         */

        public Object getControl(String controlType)
        {
            return null;
        }

        public Object[] getControls()
        {
            return new Object[0];
        }

    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
