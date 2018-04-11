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
import java.util.Iterator;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIElement;
import javax.tv.service.SIException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.filesys.AuthFileSys;
import org.cablelabs.impl.manager.filesys.OCFileSys;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsCallback;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.davic.net.Locator;
import org.dvb.dsmcc.DSMCCObject;
import org.dvb.dsmcc.IllegalObjectTypeException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.NotLoadedException;
import org.ocap.net.OcapLocator;

/**
 * @author Eric Koldinger
 */
public class RealObjectCarousel implements ObjectCarousel, ServiceDetailsCallback
{
    // Log4J Logger
    private static final Logger log = Logger.getLogger(RealObjectCarousel.class.getName());

    /**
     * Number of ServiceDomains which have attached this carousel.
     */
    private int m_attachments = 0;

    /**
     * Name of where in the filesystem this carousel is mounted.
     */
    private String m_mountPoint = null;

    /**
     * Are we mounted? For starters, nope.
     */
    private boolean m_mounted = false;

    // TODO(Todd): This should really be a ServiceDetails. Handles passed to
    // native
    // should be ServiceDetails handles. This currently works with a Service
    // handle
    // because native SIDB uses the same handle for a service and its
    // corresponding
    // service details.
    private Service m_service;
    
    private ServiceComponentExt m_component;

    private int m_componentTag;

    private int m_carouselId;

    private Object m_sdUniqueId;

    private String m_url = null;

    // Class variables
    private static Vector carousels = new Vector();

    private static SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();

    private static ObjectCarouselManager ocManager = ObjectCarouselManager.getInstance();

    private RealObjectCarousel(Service service, ServiceComponentExt comp) throws SIException
    {
        // Grab the inputs
        m_service = service;
        m_component = comp;
        m_carouselId = comp.getCarouselID();
        m_componentTag = comp.getComponentTag();

        m_url = urlString(service, m_componentTag);
        if (log.isDebugEnabled())
        {
            log.debug("ObjCar: new ObjectCarousel(" + m_url + ")");
        }

        // Get the service details unique ID
        try
        {
            ServiceDetailsExt sdExt = ((ServiceDetailsExt) ((ServiceExt) m_service).getDetails());
            m_sdUniqueId = sdExt.getID();
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("ObjCar: Caught exception while looking up unique ID: " + e.getMessage());
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("ObjCar: new ObjectCarousel: CarouselID " + m_carouselId);
        }
    }

    // Returns an object carousel's service value
    public Service getService()
    {
        return m_service;
    }

    public int getCarouselId()
    {
        return m_carouselId;
    }

    public Locator getLocator()
    {
        try
        {
            return new OcapLocator(m_service.getLocator().toExternalForm() + ".@0x"
                    + Integer.toHexString(m_componentTag));
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            return null;
        }
    }

    /**
     * Find a carousel which corresponds to a given set of parameters. If it
     * already exists, return that carousel. If it doesn't exist, create a new
     * carousel and mount it.
     */
    static ObjectCarousel getObjectCarousel(Service service, ServiceComponentExt comp) throws MPEGDeliveryException
    {
        RealObjectCarousel oc = null;

        synchronized (carousels)
        {
            Iterator iter = carousels.iterator();
            boolean found = false;
            while (!found && iter.hasNext())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ObjCar: Comparing against existing carousel");
                }
                oc = (RealObjectCarousel) iter.next();
                if (oc.match(service, comp))
                {
                    // Found it. Mark it and get out
                    found = true;
                }
                else
                {
                    oc = null;
                }
            }

            // Didn't find a carousel.
            // Create one.
            if (!found)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ObjCar: Creating a new carousel");
                }

                try
                {
                    oc = new RealObjectCarousel(service, comp);
                }
                catch (SIException e)
                {
                    throw new MPEGDeliveryException(e.getMessage());
                }

                carousels.add(oc);
            }
        }

        // Leave the synchronized block.
        try
        {
            oc.attach();
            oc.mount();
            ocManager.addCarousel(oc, new AuthFileSys(new OCFileSys()));
        }
        catch (Exception e)
        {
            oc.detach();
            throw new MPEGDeliveryException(e.getMessage());
        }
        return oc;
    }

    /**
     * Remove a carousel from the global queue.
     * 
     * @param oc
     *            The carousel to remove.
     */
    static private void removeCarousel(RealObjectCarousel oc)
    {
        synchronized (carousels)
        {
            // Check one more time that the number of attachments is 0.
            // This could occur if we did a detach at pretty much the same
            // time that we doing a getObjectCarousel on the same carousel.
            if (oc.m_attachments != 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ObjCar: Not removing carousel.  Attachments is non-zero");
                }
                return;
            }
            carousels.remove(oc);
            ocManager.removeCarousel(oc);
        }
    }

    /**
     * Increment the attachments count, indicating that another ServiceDomain
     * has attached to this carousel.
     */
    private synchronized void attach()
    {
        m_attachments++;
        if (log.isDebugEnabled())
        {
            log.debug("ObjCar: attaching to carousel(" + m_carouselId + "): Attachments now: " + m_attachments);
        }
        if (m_attachments == 1)
        {
            // First attachment, listen for service details remaps.
            TransportExt.addServiceDetailsCallback(this, 0);
        }
    }

    /**
     * Detach a ServiceDomain from this carousel. If the number of attached
     * domains goes to 0, the carousel will be unmounted.
     */
    public synchronized void detach()
    {
        int handle;
        m_attachments--;
        if (log.isDebugEnabled())
        {
            log.debug("ObjCar: Detaching from carousel (" + m_url + "): Remaining attachments: " + m_attachments);
        }
        if (m_attachments == 0)
        {
            removeCarousel(this);

            handle = ((ServiceExt) m_service).getServiceHandle().getHandle();

            if (m_mounted)
            {
                try
                {
                    nativeGoUnmount(m_url, handle, m_carouselId);
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("ObjCar: Caught exception while unmounting: " + e.getMessage());
                    }
                } finally
                {
                    m_mounted = false;
                    TransportExt.removeServiceDetailsCallback(this);
                }
            }
        }
    }

    /**
     * Mount a carousel per the information entered above.
     */
    private synchronized void mount() throws MPEGDeliveryException
    {
        int handle;
        // Make sure we're not already mounted.
        if (m_mounted)
        {
            return;
        }

        handle = ((ServiceExt) m_service).getServiceHandle().getHandle();

        nativeGoMount(m_url, handle, m_carouselId);
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("ObjCar: mount(" + m_url + ", " + m_carouselId + ") complete");
            }
            m_mountPoint = nativeGetPath(m_url, handle, m_carouselId);
            if (log.isDebugEnabled())
            {
                log.debug("ObjCar: mount(" + m_url + ", " + m_carouselId + ") mountpoint " + m_mountPoint);
            }
        }
        catch (Exception e)
        {
            throw new MPEGDeliveryException(e.getMessage());
        }
        m_mounted = true;
    }

    /**
     * Return the mountpoint for this carousel.
     * 
     * @return The mountpoint.
     */
    public synchronized String getMountPoint() throws NotLoadedException, FileNotFoundException
    {
        // Make sure we've mounted it.
        // Don't bother to synchronize on the mounted variable,
        // as we'll recheck this in the mount routine and synchronize there.
        if (!m_mounted)
        {
            throw new NotLoadedException("Carousel not mounted");
        }
        // Just return the mountpoint.
        return m_mountPoint;
    }

    /**
     * Determine if the current connection is available.
     */
    public boolean isNetworkConnectionAvailable()
    {
        boolean connAvail = false;
        if (m_mounted)
        {
            try
            {
                int handle = ((ServiceExt) m_service).getServiceHandle().getHandle();
                connAvail = nativeCheckConnection(m_url, handle, m_carouselId);
            }
            catch (Exception e)
            {
                connAvail = false;
            }
        }
        return connAvail;
    }

    /**
     * Add a listener for connection changes.
     * 
     * @param listener
     */
    public void addConnectionListener(ObjectCarouselConnectionListener listener)
    {
        // TODO: Implement this method.
    }

    /**
     * Remove a previously registered listener.
     * 
     * @param listener
     */
    public void removeConnectionListener(ObjectCarouselConnectionListener listener)
    {
        // TODO: Implement this method
    }

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
    public DSMCCStreamInterface getStreamObject(String path) throws IOException, IllegalObjectTypeException
    {
        int fileType = getFileType(path);
        if (fileType == TYPE_STREAM)
        {
            return new DSMCCStreamImpl(path);
        }
        else if (fileType == TYPE_STREAMEVENT)
        {
            return new DSMCCStreamEventImpl(path);
        }
        else
        {
            throw new IllegalObjectTypeException(path + " is not a stream");
        }
    }

    /**
     * Determine if this carousel matches the one being searched for. Rough
     * prototype only at this point.
     */
    private boolean match(Service service, ServiceComponentExt comp)
    {
        if (service.equals(m_service) && comp.equals(m_component))
        {
            return true;
        }
        // Got here, doesn't match.
        return false;
    }

    /**
     * Does the carousel match the specified locator?
     * 
     * @param l
     *            The locator to check against this carousel.
     * @return True if this carousel can be specified with this locator, false
     *         otherwise.
     */
    public boolean match(Locator l)
    {
        try
        {
            // Get the service component for the carousel to be mounted
            ServiceComponentExt component = getCarouselComponent(l);
            Service service = siManager.getService(l);
            return match(service, component);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Caught exception " + e.getClass().getName() + " while comparing locator: "
                        + l.toExternalForm() + " Must not match");
            }
            return false;
        }
    }

    private String urlString(Service service, int componentTag)
    {
        return service.getLocator().toExternalForm() + "@0x" + Integer.toHexString(componentTag);
    }

    // Native method specifications....
    //
    private static native void nativeGoMount(String url, int handle, int carouselId) throws MPEGDeliveryException;

    private static native void nativeReMount(String path, int handle, int carouselId) throws MPEGDeliveryException;

    // TODO(Todd): This method should not take a handle. The handle is unknown
    // if the
    // service is not currently mapped to a transport stream.
    private static native void nativeGoUnmount(String url, int handle, int carouselId) throws NotLoadedException;

    private static native String nativeGetPath(String url, int handle, int carouselId) throws NotLoadedException,
            FileNotFoundException;

    private static native boolean nativeCheckConnection(String url, int handle, int carouselId)
            throws NotLoadedException, FileNotFoundException;

    static
    {
        OcapMain.loadLibrary();
    }

    // Service mapped
    public void notifyMapped(ServiceDetails sd)
    {
        // Do nothing
    }

    // Service re-mapped
    public void notifyRemapped(ServiceDetails sd)
    {
        if (m_sdUniqueId.equals(((ServiceDetailsExt) sd).getID()) && m_mounted)
        {
            // Get the new handle and tell native to re-mount the OC
            int handle = ((ServiceDetailsExt) sd).getServiceDetailsHandle().getHandle();
            String l_mountPoint;
			// Added for findbugs issues fix - caching m_mountPoint variable
            synchronized(this)
            {
            	l_mountPoint = m_mountPoint;	
            }
            try
            {
                nativeReMount(l_mountPoint, handle, m_carouselId);
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ObjCar: Caught exception while re-mounting: " + e.getMessage());
                }
            }
        }
    }

    // Service unmapped
    public void notifyUnmapped(ServiceDetails sd)
    {
        if (m_sdUniqueId.equals(((ServiceDetailsExt) sd).getID()) && m_mounted)
        {
            // Unmount the OC
            try
            {
                nativeGoUnmount(m_url, 0, m_carouselId);
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("ObjCar: Caught exception while unmounting: " + e.getMessage());
                }
            } finally
            {
                m_mounted = false;
            }
        }
    }

    /**
     * Get the service component for the carousel with the specified locator.
     * 
     * @param locator
     *            the locator which references the carousel
     * 
     * @return the service component that carries the specified carousel
     */
    static ServiceComponentExt getCarouselComponent(Locator locator) throws InvalidLocatorException, SIException
    {
        SIElement[] elements;
        ServiceComponentExt component;

        if (log.isDebugEnabled())
        {
            log.debug("getCarouselComponent locator: " + locator);
        }

        try
        {
            SIManagerExt manager = (SIManagerExt) SIManager.createInstance();
            elements = manager.getSIElement(locator);

            if (elements[0] instanceof ServiceComponentExt) return (ServiceComponentExt) elements[0];

            if (elements[0] instanceof ServiceDetailsExt)
            {
                ServiceDetailsExt details = (ServiceDetailsExt) elements[0];
                component = (ServiceComponentExt) details.getCarouselComponent();
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getCarouselComponent - elements[0] is NOT an "
                            + "instance of ServiceDetailsExt or ServiceComponentExt - " + "throwing new SIException");
                }
                throw new SIException("Invalid Service or ServiceComponent");
            }
        }
        catch (SIRequestException e)
        {
            throw new SIException("SI request failed: " + e.getReason());
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getCarouselComponent - request failed - throwing new SIException", e);
            }
            throw new SIException("SI request failed");
        }

        return component;
    }

    /**
     * Get the service component for the carousel with the specified ID.
     * 
     * @param service
     *            the service carrying the carousel
     * @param carouselId
     *            the id of the carousel
     * 
     * @return the service component that carries the specified carousel
     */
    static ServiceComponentExt getCarouselComponent(Service service, int carouselId) throws SIException
    {
        ServiceDetailsExt details;
        ServiceComponentExt component;

        if (log.isDebugEnabled())
        {
            log.debug("getCarouselComponent - Service: " + service + " carouselId: " + carouselId);
        }

        try
        {
            details = (ServiceDetailsExt) ((ServiceExt) service).getDetails();
            component = (ServiceComponentExt) details.getCarouselComponent(carouselId);
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getCarouselComponent - request failed - throwing new SIException");
            }
            throw new SIException();
        }

        return component;
    }

    public byte[] getNSAPAddress()
    {
        // Create the NSAP address
        byte[] nsapAddr = new byte[] { 0x00, // AFI = 0x00
                0x00, // Type = 0x00
                0x00, 0x00, 0x00, 0x00, // carousel_id
                0x01, // specifierType = 0x01
                0x00, 0x10, 0x00, // specifierData = 0x001000
                0x00, 0x00, // transport_stream_id
                0x00, 0x00, // original_network_id = 0
                0x00, 0x00, // service_id (source_id or program_number)
                0x3f, // multiplex_type (2 bit) + reserved = 0x3f
                -1, -1, -1 // reserved = 0xFFFFFF
        };

        // Pull required information from the currently mounted component
        ServiceDetailsExt details = (ServiceDetailsExt) m_component.getServiceDetails();

        int frequency = ((TransportStreamExt) details.getTransportStream()).getFrequency();
        int serviceId;
        int carouselId = 0;

        if (frequency == -1)
            serviceId = details.getProgramNumber();
        else
            // Inband
            serviceId = details.getSourceID();

        try
        {
            carouselId = m_component.getCarouselID();
        }
        catch (SIException e)
        {
            // TODO: (Jason) what should be done here?
            // eat the exception and just use 0 as the carousel id?
            if (log.isDebugEnabled())
            {
                log.debug("getNSAPAddress - ignoring SIException - " + e.getMessage());
            }
        }

        // Set the carousel_id
        nsapAddr[2] = (byte) ((carouselId >> 24) & 0xff);
        nsapAddr[3] = (byte) ((carouselId >> 16) & 0xff);
        nsapAddr[4] = (byte) ((carouselId >> 8) & 0xff);
        nsapAddr[5] = (byte) ((carouselId) & 0xff);

        // Set the service_id
        nsapAddr[14] = (byte) ((serviceId >> 8) & 0xff);
        nsapAddr[15] = (byte) ((serviceId) & 0xff);

        // Set the multiplex type
        if (frequency != -1)
            nsapAddr[16] = (byte) 0x7f;
        // else if (OC_DSG == multiplexType)
        // {
        // nsapAddr[16] = (byte) 0xbf;
        // nsapAddr[18]= (byte)((DSGappID>>8) & 0xFF );
        // nsapAddr[19]= (byte)((DSGappID) & 0xFF);
        // }
        else
            nsapAddr[16] = (byte) 0xff;

        return nsapAddr;
    }

    private static native int nativeEnableObjectChangeEvents(String path, VersionEdListener obj, int queueType)
            throws IOException;

    private static native void nativeDisableObjectChangeEvents(int objChangeHandle);

    private static native boolean nativeGetFileInfoIsKnown(String path) throws SecurityException, java.io.IOException;

    private static native boolean nativePrefetchFile(String path);

    private static native String nativeResolveServiceXfr(String path, byte targetNSAP[]) throws java.io.IOException;

    private static native int nativeGetFileInfoType(String path) throws SecurityException, java.io.IOException;

    /**
     * Event Dispatch listener for asynchronous version-change events
     */
    class VersionEdListener implements EDListener
    {
        private DSMCCObject dsmccObj;

        private ObjectChangeCallback callBack;

        public int ocHandle;

        VersionEdListener(DSMCCObject obj, ObjectChangeCallback cb)
        {
            dsmccObj = obj;
            callBack = cb;
        }

        /**
         * Process the async event to handle a version change callback. Note
         * that the version number goes in the eventCode
         */
        public void asyncEvent(int eventCode, int eventData1, int eventData2)
        {
            if (log.isDebugEnabled())
            {
                log.debug("RealObjectCarousel.VersionEdListener.asyncEvent(" + eventCode + ")");
            }
            if (eventCode != EVENTCODE_OBJECTCHANGE_DONE)
            {
                // If the event code does not equal the termination event, send
                // the event.
                callBack.objectChanged(eventCode);
            }
            else
            {
                // otherwise, we're done. Release the callback object.
                callBack = null;
            }
        }
    }

    // event codes passed between MPE & DSMCCObject
    private static final int EVENTCODE_UNKNOWN = 0;

    // ObjectChangeEvent codes
    // IMPORTANT:
    // Make these bigger than 8 bits, because we kinda use them down below too.
    // Fortutunately, versions are only 8 bits long, so if these are bigger than
    // that, we won't ever signal incorrectly.
    private static final int EVENTCODE_OBJECTCHANGE = 0x10000;

    private static final int EVENTCODE_OBJECTCHANGE_DONE = 0x10001;

    // ED queue type for this object
    private static final int QUEUE_TYPE = EDListener.QUEUE_NORMAL;

    public void disableChangeEvents(Object handle)
    {
        if (handle instanceof VersionEdListener)
        {
            VersionEdListener ed = (VersionEdListener) handle;
            nativeDisableObjectChangeEvents(ed.ocHandle);
        }
    }

    public Object enableObjectChangeEvents(String path, DSMCCObject obj, ObjectChangeCallback callback)
            throws IOException
    {
        VersionEdListener ed = new VersionEdListener(obj, callback);
        ed.ocHandle = nativeEnableObjectChangeEvents(path, ed, QUEUE_TYPE);
        return ed;
    }

    public boolean getFileInfoIsKnown(String path) throws SecurityException, IOException
    {
        if (m_mounted)
        {
            return nativeGetFileInfoIsKnown(path);
        }
        else
        {
            throw new IOException(path + " is not mounted");
        }

    }

    public int getFileType(String realPath) throws IOException
    {
        if (m_mounted)
        {
            return nativeGetFileInfoType(realPath);
        }
        else
        {
            throw new IOException(realPath + " is not mounted");
        }

    }

    public void prefetchFile(String path)
    {
        if (m_mounted)
        {
            nativePrefetchFile(path);
        }
    }

    public String resolveServiceXfer(String path, byte[] targetNSAP) throws IOException
    {
        if (m_mounted)
        {
            return nativeResolveServiceXfr(path, targetNSAP);
        }
        else
        {
            throw new IOException("Carousel Not Mounted");
        }
    }
}
