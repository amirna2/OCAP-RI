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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIException;

import org.apache.log4j.Logger;
import org.davic.net.Locator;
import org.dvb.dsmcc.DSMCCException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotLoadedException;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;

public class ObjectCarouselManager
{
    private Vector carousels = new Vector();

    private Vector builders = new Vector();

    private FileManager fileMgr = (FileManager) ManagerManager.getInstance(FileManager.class);

    // Log4J Logger
    private static final Logger log = Logger.getLogger(ObjectCarouselManager.class.getName());

    private static ObjectCarouselManager s_instance = null;

    /**
     * Get the singleton instance of the ObjectCarouselManager.
     * 
     * @return The ObjectCarouselManager.
     */
    public static synchronized ObjectCarouselManager getInstance()
    {
        if (s_instance == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("ObjectCarouselManager: Constructing singleton");
            }
            s_instance = new ObjectCarouselManager();
        }
        return s_instance;
    }

    /**
     * Construct the OC manager and add all OC builders specified in the
     * properties files
     */
    private ObjectCarouselManager()
    {
        // Add all OC builders specified in properties files. Properties are
        // returned
        // highest precedence first, so the default builder is guaranteed to be
        // added last
        builders.addAll(PropertiesManager.getInstance().getInstancesByPrecedence("OCAP.dsmcc.ObjectCarouselBuilder"));
    }

    /**
     * Mount a carousel based on a locator alone. The locator must specify the
     * component containing the carousel, or a service which contains only a
     * single carousel.
     * 
     * @param loc
     *            Locator of the service containing the carousel.
     * @return The carousel.
     * @throws InvalidLocatorException
     *             If no carousel type can mount this carousel.
     * @throws SIException
     * @throws MPEGDeliveryException
     * @throws org.davic.net.InvalidLocatorException
     * @throws SecurityException
     */
    public ObjectCarousel getObjectCarousel(Locator loc) throws InvalidLocatorException, SIException,
            MPEGDeliveryException, SecurityException
    {
        for (int i = 0; i < builders.size(); i++)
        {
            ObjectCarouselBuilder ocb = (ObjectCarouselBuilder) builders.get(i);
            ObjectCarousel oc = ocb.getObjectCarousel(loc);
            if (oc != null)
            {
                return oc;
            }
        }
        throw new InvalidLocatorException(loc, "No carousel registered to handle locator");
    }

    /**
     * Mount a specified carousel within a service.
     * 
     * @param loc
     *            Locator of the service.
     * @param carouselId
     *            The carousel ID within the service.
     * @return The carousel.
     * @throws SecurityException
     * @throws InvalidLocatorException
     * @throws SIException
     * @throws org.davic.net.InvalidLocatorException
     * @throws MPEGDeliveryException
     */
    public ObjectCarousel getObjectCarousel(Locator loc, int carouselId) throws DSMCCException,
            InvalidLocatorException, SIException, MPEGDeliveryException
    {
        for (int i = 0; i < builders.size(); i++)
        {
            ObjectCarouselBuilder ocb = (ObjectCarouselBuilder) builders.get(i);
            if (log.isDebugEnabled())
            {
                log.debug("Invoking " + ocb.getClass().getName() + " to mount carousel +" + loc.toExternalForm() + ", "
                        + carouselId);
            }
            ObjectCarousel oc = ocb.getObjectCarousel(loc, carouselId);
            if (oc != null)
            {
                return oc;
            }
        }
        throw new InvalidLocatorException(loc, "No carousel registered to handle locator");
    }

    /**
     * Find an object carousel who's mount point corresponds with the start of
     * the path passed in. The path passed in can be longer than the
     * mount-point, allowing an entire path name to be passed in.
     * 
     * @param path
     *            The path to search for.
     * @return The appropriate ObjectCarousel.
     * @throws MPEGDeliveryException
     */
    public ObjectCarousel getCarouselByPath(String path) throws MPEGDeliveryException
    {
        // Find a carousel which has a mount point that matches the path passed
        // in.
        // Typically, the path would be the path of a stream event or
        // DSMCCStreamEvent
        // object. This function would search through the carousels until the
        // carouosel
        // mount point is found to be at the start of the passed in string. Note
        // that we
        // need to match the mount point to the next '/' character so that we
        // avoid confusion
        // created by names such as /oc/1 versus /oc/10.
        ObjectCarousel oc;
        String str;

        synchronized (carousels)
        {
            Iterator iter = carousels.iterator();
            while (iter.hasNext())
            {
                oc = (ObjectCarousel) iter.next();
                try
                {
                    str = oc.getMountPoint();
                }
                catch (Exception e)
                {
                    throw new MPEGDeliveryException(e.getMessage());
                }
                if (path.startsWith(str)) // str should start off the path
                {
                    if (path.length() == str.length() || path.charAt(str.length()) == '/') // str
                                                                                           // should
                                                                                           // immediately
                                                                                           // be
                                                                                           // followed
                                                                                           // by
                                                                                           // '/'
                    {
                        // OK, Mount point is in the path. This is the carousel
                        // we are looking
                        // for.
                        return oc;
                    }
                }
            }
        }
        throw new MPEGDeliveryException("Carousel not found. Path: " + path); // carousel
                                                                              // not
                                                                              // there
    }

    /**
     * Add an object carousel to the list of mounted carousels. The
     * ObjectCarouselBuilder needs to call this when it's completed mounting the
     * carousel.
     * 
     * @param oc
     *            The new ObjectCarousel.
     */
    public void addCarousel(ObjectCarousel oc, FileSys fileSys)
    {

            try
            {
            if (log.isDebugEnabled())
            {
                log.debug("Adding " + oc.getClass().getName() + " carousel at " + oc.getMountPoint());
            }
        }
            catch (Exception e)
            { /* Shouldn't get here */
            }
        synchronized (carousels)
        {
            try
            {
                fileMgr.registerFileSys(oc.getMountPoint(), fileSys);
                carousels.add(oc);
            }
            catch (NotLoadedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (FileNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove an object carousel from teh list of mounted carousels. The
     * ObjectCarousel needs to remove itself when it unmounts.
     * 
     * @param oc
     *            The carousel to be removed.
     */
    public void removeCarousel(ObjectCarousel oc)
    {
        synchronized (carousels)
        {
            try
            {
                fileMgr.unregisterFileSys(oc.getMountPoint());
            }
            catch (NotLoadedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (FileNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            carousels.remove(oc);
        }
    }
}
