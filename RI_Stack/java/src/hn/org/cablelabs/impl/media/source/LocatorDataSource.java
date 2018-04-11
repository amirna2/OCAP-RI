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

package org.cablelabs.impl.media.source;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;

public class LocatorDataSource extends DataSource
{
    protected ContentDescriptor contentType;

    protected boolean connected;

    protected String m_fullContentType;

    private static final Logger log = Logger.getLogger(LocatorDataSource.class);
    private Map headers = new HashMap();

    /**
     * Implemented by subclasses.
     */
    protected LocatorDataSource()
    {
    }

    /**
     * Construct a <CODE>URLDataSource</CODE> directly from a <code>URL</code>.
     */
    public LocatorDataSource(URL url) throws IOException
    {
        super(new MediaLocator(url));
    }

    /**
     * This method opens a URL connection using its associated URL and then gets
     * the content type. It closes the input stream associated with the
     * connection once the content type is retrieved. It uses a HEAD request to
     * query for the content type rather than a GET since the file transfer is
     * not handled here. A HEAD request is appropriate for just getting
     * information about the content.
     */
    public void connect() throws IOException
    {
        // Make the connect.
        URLConnection conn = null;
        try
        {
            // Open the connection
            conn = getLocator().getURL().openConnection();

            // Cast this connection to an HTTP connection
            HttpURLConnection httpConn = (HttpURLConnection) conn;

            // *TODO* - uncomment these two lines to test dangling connections
            // Set the HTTP request type to be HEAD since just retrieving
            // content type
            httpConn.setUseCaches(false);
            httpConn.setRequestMethod("HEAD");
            //pass the getContentFeatures and transferMode headers in case this is a dlna-compatible server
            httpConn.addRequestProperty("getcontentFeatures.dlna.org", "1");
            httpConn.addRequestProperty("transferMode.dlna.org", "Streaming");
            conn.connect();
            connected = true;
            headers = httpConn.getHeaderFields();
            // Figure out the content type.
            m_fullContentType = conn.getContentType();
            String mimeType = HNStreamProtocolInfo.parseContentFormat(conn.getContentType());
            //response code may be 400/content type may be text/HTML in that case...but we don't have a player for that type
            //letting it fall through and fail
            if (mimeType == null)
            {
                mimeType = ContentDescriptor.CONTENT_UNKNOWN;
            }
            if (log.isDebugEnabled()) 
            {
                log.debug("content type for locator: " + getLocator().toExternalForm() + ": " + mimeType);
            }
            contentType = new ContentDescriptor(mimeType);
        }
        finally
        {
            if (conn != null)
            {
                conn.getInputStream().close();
            }
        }
    }

    public void disconnect()
    {
        connected = false;
    }

    /**
     * Provide http headers - a map of header String name keys to a List of values
     * @return
     */
    public Map getHeaders()
    {
        return new HashMap(headers);
    }

    public String getContentType()
    {
        if (!connected)
        {
            throw new java.lang.Error("Source is unconnected.");
        }
        return contentType.getContentType();
    }

    public String getFullContentType()
    {
        return m_fullContentType;
    }

    public void start() throws IOException
    {
    }

    public void stop() throws IOException
    {
    }

    public Object getControl(String controlType)
    {
        return null;
    }

    public Object[] getControls()
    {
        return new Object[0];
    }

    public Time getDuration()
    {
        return null;
    }

}
