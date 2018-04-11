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

package org.cablelabs.impl.media.protocol.file;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PullSourceStream;
import javax.media.protocol.URLDataSource;

import junit.framework.TestCase;

/**
 * Test the file DataSource.
 * 
 * @author schoonma
 */
public class FileDataSourceTest extends TestCase
{
    static String gifFileName = "/images/blue-ball.gif";

    static URL gifFileUrl = FileDataSourceTest.class.getResource(gifFileName);

    static MediaLocator gifFileLoc = new MediaLocator("file://" + gifFileName);

    static String gifMimeType = "image/gif";

    static String wavFileName = "/org/havi/ui/sounds/handclap.wav";

    static URL wavFileUrl = FileDataSourceTest.class.getResource(wavFileName);

    static MediaLocator wavFileLoc = new MediaLocator("file://" + wavFileName);

    static String wavMimeType = "audio/x-wav";

    public void testCreateDataSourceByURL()
    {
        try
        {
            Manager.createDataSource(gifFileUrl);
        }
        catch (Exception x)
        {
            fail("exception creating DataSource for URL '" + gifFileUrl + "': " + x);
        }
    }

    public void testCreateDataSourceByMediaLocator()
    {
        // This code is necessary because of different file systems on different
        // platforms.
        // Currently, the test only support SA (SNFS) and Windows (syscwd) root
        // folders.
        String filename = "/syscwd/qa/xlet/org/cablelabs/xlet/hsampler/images/blue-ball.gif";
        if (!new File(filename).exists())
        {
            filename = "/snfs/qa/xlet/org/cablelabs/xlet/hsampler/images/blue-ball.gif";
            if (!new File(filename).exists()) filename = null;
        }

        if (filename == null) fail("could not find file to test");

        // Specify the location w.r.t. syscwd.
        try
        {
            Manager.createDataSource(new MediaLocator("file://" + filename));
        }
        catch (Exception x)
        {
            fail("exception creating DataSource for MediaLocator '" + gifFileLoc + "': " + x);
        }
    }

    private URLDataSource getDataSource(URL url)
    {
        try
        {
            return (URLDataSource) Manager.createDataSource(url);
        }
        catch (Exception x)
        {
            fail("exception creating URLDataSource from " + url + ": " + x);
        }
        return null;
    }

    private URLDataSource connectDataSource(URL url)
    {
        URLDataSource ds = getDataSource(url);
        try
        {
            ds.connect();
        }
        catch (Exception x)
        {
            fail("exception connecting URLDataSource: " + x);
        }
        return ds;
    }

    public void testConnectURL()
    {
        URLDataSource ds = getDataSource(gifFileUrl);
        try
        {
            ds.connect();
        }
        catch (IOException x)
        {
            assertTrue("could not connect DataSource: " + x.getMessage(), false);
        }
        ds.disconnect();
    }

    public void testGetContentTypeGIF()
    {
        URLDataSource ds = connectDataSource(gifFileUrl);
        try
        {
            String contentType = ds.getContentType();
            assertEquals("getContentType() returned incorrect value: " + contentType, gifMimeType, contentType);
        }
        finally
        {
            ds.disconnect();
        }
    }

    // TODO(mas): should WAV be a supported MIME type?
    // public void testGetContentTypeWAV()
    // {
    // URLDataSource ds = connectDataSource(wavFileUrl);
    // try {
    // String contentType = ds.getContentType();
    // assertEquals("getContentType() returned incorrect value: "+contentType,
    // wavMimeType, contentType);
    // }
    // finally { ds.disconnect(); }
    // }

    public void testStart()
    {
        URLDataSource ds = connectDataSource(wavFileUrl);
        try
        {
            ds.start();
            ds.stop();
        }
        catch (IOException x)
        {
            assertTrue("could not start/stop DataSource", false);
        }
        finally
        {
            ds.disconnect();
        }
    }

    public void testGetStreams()
    {
        URLDataSource ds = connectDataSource(wavFileUrl);
        try
        {
            PullSourceStream[] streams = ds.getStreams();
            assertNotNull(streams);
            assertEquals(streams.length, 1);
            assertTrue(streams[0] instanceof PullSourceStream);
        }
        finally
        {
            ds.disconnect();
        }
    }

    public void testSourceStream()
    {
        URLDataSource ds = connectDataSource(wavFileUrl);
        try
        {
            PullSourceStream[] streams = ds.getStreams();
            PullSourceStream stream = streams[0];
            assertEquals(stream.getContentLength(), 11484);
            ContentDescriptor desc = stream.getContentDescriptor();
            assertNotNull(desc);
            assertFalse(stream.endOfStream());
        }
        finally
        {
            ds.disconnect();
        }
    }

    public void testReadStream()
    {
        URLDataSource ds = connectDataSource(wavFileUrl);
        try
        {
            PullSourceStream stream = ds.getStreams()[0];
            int length = (int) stream.getContentLength();
            int offset = 0;
            byte[] buffer = new byte[length];
            int count = stream.read(buffer, offset, length);
            assertTrue(count != -1 && count <= length);
        }
        catch (IOException x)
        {
            assertTrue("error reading from stream: " + x.getMessage(), false);
        }
        finally
        {
            ds.disconnect();
        }
    }
}
