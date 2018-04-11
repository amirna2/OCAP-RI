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

package org.cablelabs.impl.manager.lightweighttrigger;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.dvb.dsmcc.DSMCCObject;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.cablelabs.impl.dvb.dsmcc.DSMCCStreamInterface;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarousel;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarouselManager;
import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysImpl;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.filesys.LoadedFileSys;
import org.cablelabs.impl.manager.filesys.LoadedStreamFileSys;

public class MediaTimeTagsFileSys extends FileSysImpl
{

    public OpenFile open(String path) throws FileNotFoundException
    {
        throw new FileNotFoundException(path);
    }

    public OpenFile openClass(String path) throws FileNotFoundException
    {
        throw new FileNotFoundException(path);
    }

    public FileSys load(String path, int mode) throws FileNotFoundException, IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Loading object: " + path);
        }

        ObjectCarouselManager ocm = ObjectCarouselManager.getInstance();
        ObjectCarousel oc = ocm.getCarouselByPath(path);
        DSMCCStreamInterface stream = oc.getStreamObject(path);

        // construct and return a LoadedFileSys
        // TODO: What value should go in for version?
        // BUG: Actually, should return this.
        return new LoadedStreamFileSys(stream, 0, this);
    }

    public AsyncLoadHandle asynchronousLoad(final String path, final int mode, final AsyncLoadCallback cb)
    {
        if (log.isDebugEnabled())
        {
            log.debug("asynchronousLoad() path=" + path + " mode=" + mode);
        }

        // Create a new thread and call sync load.
        Runnable run = new Runnable()
        {
            public void run()
            {
                Exception exc = null;
                FileSys lfs = null;

                if (log.isDebugEnabled())
                {
                    log.debug("asynchronousLoad() start async thread");
                }

                try
                {
                    lfs = load(path, mode);
                }
                catch (IOException e)
                {
                    exc = e;
                }
                if (log.isDebugEnabled())
                {
                    log.debug("async thread calling AsyncLoadCallback.done()");
                }

                cb.done(lfs, exc);
            }
        };
        new Thread(run).start();

        return new AsyncLoadHandle()
        {
            public boolean abort()
            {
                cb.abort();
                return true;
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#lastModified(java.lang.String)
     */
    public long lastModified(String path)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Getting last modified date for: " + path);
        }
        try
        {
            ObjectCarouselManager ocm = ObjectCarouselManager.getInstance();
            ObjectCarousel oc = ocm.getCarouselByPath(path);

            if (oc instanceof MediaTimeTagsCarousel)
            {
                MediaTimeTagsCarousel mttc = (MediaTimeTagsCarousel) oc;
                return mttc.getVersion();
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Carousel for " + path + " is not MTT Carousel.");
                }
            }
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Caught exception getting carousel for " + path, e);
            }
        }
        return 0;
    }

    private static final Logger log = Logger.getLogger(MediaTimeTagsFileSys.class.getName());

}
