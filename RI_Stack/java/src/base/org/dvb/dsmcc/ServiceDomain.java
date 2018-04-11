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

package org.dvb.dsmcc;

import java.io.FileNotFoundException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.davic.net.Locator;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.dvb.dsmcc.ObjectCarousel;
import org.cablelabs.impl.dvb.dsmcc.ObjectCarouselManager;
import org.cablelabs.impl.dvb.dsmcc.OcapNSAPAddress;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.SIManagerExt;

/**
 * A <code>ServiceDomain</code> represents a group of DSMCC objects. The objects
 * are sent either using the object carousel for a broadcast network or with the
 * DSM-CC User-to-User protocol for an interactive network.
 * <p>
 *
 * To access the objects of a <code>ServiceDomain</code>, it has to be attached
 * to the file system name space of the MHP terminal.
 *
 * To access the content of an object, the application has four ways:
 * <ul>
 * <li>It can instantiate the class that is used to read the object
 * (java.io.FileInputStream or java.io.RandomAccessFile for a File or
 * DSMCCStream for a stream) from its pathname. The loading of the object is
 * implicit but the application has no way to abort it. NB: Obviously, for the
 * Object Carousel, the write mode of java.io.RandomAccessFile is not allowed.
 * <li>It can instantiate a DSMCCObject and carry out a Synchronous loading. The
 * loading can be aborted by the abort method of the DSMCCObject class. When the
 * object is loaded, the application will instantiate the class used to read the
 * object.
 * <li>It can instantiate a DSMCCObject and carry out an Asynchronous loading.
 * So several loading can be started in parallel from the same thread.
 * <li>It is also possible to create directly a java.io.File for a DSMCC object.
 * </ul>
 * Instances of <code>ServiceDomain</code> exist in two states, attached and
 * detached. Newly created instances are always in the detached state. They
 * become attached when a call to the <code>attach</code> method succeeds. They
 * become detached following a call to the <code>detach</code> method.
 * <p>
 * When service domains in the attached state temporarily lose their network
 * connection (e.g. if the MHP terminal tunes away from the transport stream
 * where they are carried), the behaviour of DSMCC objects which are part of the
 * service domain is specified in the main body of the present document. If such a
 * network connection becomes available again then the service domain shall
 * resume normal behaviour.
 * <p>
 * A service domain which is temporarily lost its network connection may be
 * forced into the detached state by the implementation if the loss of the
 * network connection becomes irrecoverable. The precise details of when this
 * happens are implementation dependent. This is the only situation when shall
 * be forced into the detached state. Once a ServiceDomain is detached, it will
 * never be automatically attached.
 *
 * @author Brent Thompson
 * @author Todd Earles
 */
public class ServiceDomain
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(ServiceDomain.class);

    /** Identity of this Object */
    private final String id = "@" + Integer.toHexString(System.identityHashCode(this));

    /**
     * Current state of this service domain. The current state will be one of
     * the following values at all times.
     * <ul>
     * <li>STATE_NOT_ATTACHED indicates that this service domain is not
     * currently attached to an object carousel.
     * <li>STATE_ATTACHING indicates that this service domain is in the process
     * of attaching to an object carousel.
     * <li>STATE_ATTACHED indicates that service domain is attached to an object
     * carousel.
     * </ul>
     */
    private int state = STATE_NOT_ATTACHED;

    private static final int STATE_NOT_ATTACHED = 1;

    private static final int STATE_ATTACHING = 2;

    private static final int STATE_ATTACHED = 3;

    private static final ObjectCarouselManager s_objMan = ObjectCarouselManager.getInstance();

    /**
     * The attach flag holds a single field that indicates whether the attach
     * has been canceled.
     */
    private class AttachFlag
    {
        boolean canceled = false;
    }

    /**
     * This field holds the attach flag for the current attach that is in
     * progress. When the service domain moves to the ATTACHING state a new
     * attach flag is created (with the value false) and assigned to this field.
     * Since attach() is synchronous only one attach can be in progress at any
     * point in time.
     * <p>
     * If the attach() completes without being interrupted this field will
     * contain the attach flag originally assigned and it will still be false.
     * In this case the attach() completes normally and this field is cleared.
     * <p>
     * If another thread performs a detach while the service domain is in the
     * ATTACHING state, then the current attach flag is set to true and this
     * field is set to null. When the original attach() completes it will notice
     * that the attach flag is false and will fail with InterruptedIOException.
     * <p>
     * This approach allows us to associate a seperate attach flag with each
     * pending attach(). This is necessary since it may take some time for each
     * failed attach (each on its own thread) to notice that it has failed.
     * However, from the service domains point of view there is never more than
     * one attach pending. As soon as the current attach is canceled it can be
     * forgotten by the service domain.
     */
    private AttachFlag currentAttachFlag = null;

    /** The service component of the currently mounted carousel */
    // private ServiceComponent mountedComponent = null;

    /** The locator used to mount the currently mounted object carousel. */
    // private OcapLocator mountedLocator = null;

    private ObjectCarousel objectCarousel = null;

    /**
     * An object private to and unique for each occurance of a ServiceDomain
     * object. This object is used for synchronizing state changes in the
     * service domain.
     */
    private Object implObject = new Object();

    /**
     * Per caller context data
     */
    static class CCData implements CallbackData
    {
        /**
         * The domains list is used to keep track of all ServiceDomain objects
         * currently in the attached state for this caller context.
         */
        public volatile Vector domains = new Vector();

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            // Discard the caller context data for this caller context.
            cc.removeCallbackData(ServiceDomain.class);
            // Remove each ServiceDomain object from the domains list, and
            // delete it.
            int size = domains.size();

            for (int i = 0; i < size; i++)
            {
                try
                {
                    // Grab the first element in the queue
                    ServiceDomain sd = (ServiceDomain) domains.elementAt(i);
                    // And detach it
                    sd.detach();
                    // And get rid of it
                }
                catch (Exception e)
                {
                    // Ignore any exceptions
                    if (log.isDebugEnabled())
                    {
                        log.debug("destroy() ignoring Exception " + e);
                    }
                }
            }
            // Toss the whole thing
            domains = null;
        }
    }

    /**
     * Creates a ServiceDomain object.
     */
    public ServiceDomain()
    {
    }

    /**
     * This function is used to attach a ServiceDomain from an object carousel.
     * It loads the module which contains the service gateway object and mounts
     * the <code>ServiceDomain</code> volume in the file system hierarchy. This
     * call will block until the service gateway is loaded. It can be aborted by
     * another thread with the method detach. In this case an
     * <code>InterruptedIOException</code> is thrown.
     *
     * <p>
     * Calling this method on a <code>ServiceDomain</code> object already in the
     * attached state shall imply a detach of the <code>ServiceDomain</code>
     * object before the attach operation unless the <code>ServiceDomain</code>
     * is already attached to the correct location. Hence if the attach
     * operation fails, the appropriate exception for the failure mode shall be
     * thrown and the <code>ServiceDomain</code> is left in a detached state and
     * not attached to the former object carousel / service domain. If the
     * <code>ServiceDomain</code> is already attached to the correct location
     * then the method call shall have no effect.
     * <p>
     *
     * @param aDVBService
     *            The coordinates of the DVB service which contains the object
     *            carousel. This locator has to point to a DVB service.
     * @param aCarouselId
     *            The identifier of the carousel.
     * @exception InterruptedIOException
     *                The attachment has been aborted.
     * @exception MPEGDeliveryException
     *                An MPEG error occurred (such as time-out).
     * @exception ServiceXFRException
     *                The service gateway cannot be loaded in the current
     *                service domain. This exception shall not be thrown in this
     *                version of the specification.
     */
    public void attach(Locator l, int aCarouselId) throws ServiceXFRException, InterruptedIOException,
            MPEGDeliveryException
    {

        if (log.isDebugEnabled())
        {
            log.debug(id + " attach(locator: " + l + " carouselId: " + aCarouselId + ") entering...");
        }

        try
        {
            AttachFlag flag = startAttach();
            ObjectCarousel newOC = null;
            // Surround this in a try/catch so we can make sure that, no matter
            // what we do, we do
            // mop up the completeAttach();
            try
            {
                newOC = s_objMan.getObjectCarousel(l, aCarouselId);
            } finally
            {
                completeAttach(newOC, flag);
            }
        }
        catch (DSMCCException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - caught DSMCCException - " + e.getMessage());
            }
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - throwing new MPEGDeliveryException (DSMCCException)");
            }
            throw new MPEGDeliveryException("DSMCCException: " + e.getMessage());
        }
        catch (InvalidLocatorException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - caught InvalidLocatorException - " + e.getMessage());
            }
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - throwing new MPEGDeliveryException (Invalid locator)");
            }
            throw new MPEGDeliveryException("Invalid locator: " + e.getMessage());
        }
        catch (SIException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - caught SIException - " + e.getMessage());
            }
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - throwing new MPEGDeliveryException (Cannot access SI)");
            }
            throw new MPEGDeliveryException("Cannot access SI: " + e.getMessage());
        }
    }

    private void completeAttach(ObjectCarousel newOC, AttachFlag attachFlag) throws InterruptedIOException
    {
        synchronized (implObject)
        {
            // If the canceled flag is set then the attach was canceled. In this
            // case we need to call the native layer to unmount the carousel
            // then throw InterruptedIOException
            if (attachFlag.canceled)
            {
                if (newOC != null)
                {
                    newOC.detach();
                }
                throw new InterruptedIOException();
            }
            // If something was already mounted, detach from it.
            if (objectCarousel != null)
            {
                if (objectCarousel != newOC)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Detaching previously mounted carousel");
                    }
                    objectCarousel.detach();
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Carousel remounted.  Releasing original.");
                    }
                    objectCarousel.detach();
                }
            }
            currentAttachFlag = null;
            objectCarousel = newOC;
            if (objectCarousel != null)
            {
                // If we get here then everything worked. Update the state and
                // all
                // state variables to transition to the ATTACHED state.
                setState(STATE_ATTACHED);

                // Remove mount for list of CRL scan locations.
                ((AuthManager) ManagerManager.getInstance(AuthManager.class)).registerCRLMount(objectCarousel.getLocator()
                        .toExternalForm());
            }
            else
            {
                setState(STATE_NOT_ATTACHED);
            }
        }
    }

    private AttachFlag startAttach()
    {
        AttachFlag attachFlag;
        // Transition to the ATTACHING state
        synchronized (implObject)
        {
            // If we are already attached to (or attaching to) a different
            // object carousel then detach it first. The pending attach is not
            // immediately canceled. Instead, a flag is set and when the
            // attach eventually completes the cancelation is noticed and the
            // InterruptedIOException is thrown.
            if (state == STATE_ATTACHING)
            {
                setState(STATE_NOT_ATTACHED);
                if (currentAttachFlag != null)
                {
                    currentAttachFlag.canceled = true;
                    currentAttachFlag = null;
                }
            }

            // Transition to the ATTACHING state
            setState(STATE_ATTACHING);
            attachFlag = new AttachFlag();
            currentAttachFlag = attachFlag;
        }
        return attachFlag;
    }

    /**
     * This function is used to attach a <code>ServiceDomain</code> from an
     * object carousel. It loads the module which contains the service gateway
     * object and mounts the <code>ServiceDomain</code> volume in the file
     * system hierarchy. This call will block until the service gateway is
     * loaded. It can be aborted by another thread with the method detach. In
     * this case an <code>InterruptedIOException</code> is thrown.
     * <p>
     * Calling this method on a <code>ServiceDomain</code> object already in the
     * attached state shall imply a detach of the <code>ServiceDomain</code>
     * object before the attach operation unless the <code>ServiceDomain</code>
     * is already attached to the correct location. Hence if the attach
     * operation fails, the appropriate exception for the failure mode shall be
     * thrown and the <code>ServiceDomain</code> is left in a detached state and
     * not attached to the former object carousel / service domain. If the
     * <code>ServiceDomain</code> is already attached to the correct location
     * then the method call shall have no effect.
     * <p>
     *
     * @param l
     *            The locator pointing to the elementary stream carrying the DSI
     *            of the object carousel, or to a DVB service that carries one
     *            and only one object carousel.
     *
     * @exception DSMCCException
     *                An error has occurred during the attachment. For example,
     *                the locator does not point to a component carrying a DSI
     *                of an Object Carousel or to a service containing a single
     *                carousel
     * @exception InterruptedIOException
     *                The attachment has been aborted.
     * @exception MPEGDeliveryException
     *                attaching to this domain would require tuning.
     *
     */
    public void attach(Locator l) throws DSMCCException, InterruptedIOException, MPEGDeliveryException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " attach(locator: " + l + ") entering...");
        }

        try
        {
            AttachFlag flag = startAttach();
            // Attach the carousel
            ObjectCarousel newOC = null;
            // Surround this in a try/catch so we can make sure that, no matter
            // what we do, we do
            // mop up the completeAttach();
            try
            {
                newOC = s_objMan.getObjectCarousel(l);
            } finally
            {
                completeAttach(newOC, flag);
            }
        }
        catch (InvalidLocatorException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - caught InvalidLocatorException - " + e);
            }
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - throwing new DSMCCException (Invalid locator)");
            }
            throw new DSMCCException("Invalid locator " + l);
        }
        catch (SIException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - caught SIException - " + e.getMessage());
            }
            if (log.isDebugEnabled())
            {
                log.debug(id + " attach - throwing new MPEGDeliveryException (Problem accessing SI)");
            }
            throw new DSMCCException("Problem accessing SI: " + e.getMessage());
        }

        return;
    }

    /**
     * This function is used to attach a <code>ServiceDomain</code> from either
     * an object carousel or from an interactive network. This call will block
     * until the attachment is done.
     * <p>
     * Calling this method on a <code>ServiceDomain</code> object already in the
     * attached state shall imply a detach of the <code>ServiceDomain</code>
     * object before the attach operation unless the <code>ServiceDomain</code>
     * is already attached to the correct location. Hence if the attach
     * operation fails, the appropriate exception for the failure mode shall be
     * thrown and the <code>ServiceDomain</code> is left in a detached state and
     * not attached to the former object carousel / service domain. If the
     * <code>ServiceDomain</code> is already attached to the correct location
     * then the method call shall have no effect.
     * <p>
     *
     * @param NSAPAddress
     *            The NSAP Address of a ServiceDomain as defined in in ISO/IEC
     *            13818-6
     * @exception InterruptedIOException
     *                The attachment has been aborted.
     * @exception InvalidAddressException
     *                The NSAP Address is invalid.
     * @exception DSMCCException
     *                An error has occurred during the attachment.
     * @exception MPEGDeliveryException
     *                attaching to this domain would require tuning.
     */
    public void attach(byte[] NSAPAddress) throws DSMCCException, InterruptedIOException, InvalidAddressException,
            MPEGDeliveryException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " attach(NSAPAddress: " + NSAPAddress + ") - entering");
        }

        OcapNSAPAddress nsap = new OcapNSAPAddress(NSAPAddress);
        Locator serviceLocator = nsap.getServiceLocator();
        int carouselId = nsap.getCarouselID();

        attach(serviceLocator, carouselId);
    }

    /**
     * A call to this method is a hint that the applications gives to the MHP to
     * unmount the volume and delete the objects of the service domain. When
     * another application is using objects of the same service domain the
     * method has no effects. When there are no other application using objects
     * of the service domain, a call to this method is a hint that the MHP can
     * free all the resources allocated to this service domain.
     * <p>
     * After this, the <code>ServiceDomain</code> will be in a non-attached
     * state and will behave as if it had just been constructed. Subsequent
     * calls to <code>detach</code> shall throw <code>NotLoadedException</code>.
     *
     * @exception NotLoadedException
     *                is thrown if the ServiceDomain is not attached or if there
     *                is no call to <code>attach</code> in progress.
     */
    public void detach() throws NotLoadedException
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " detach() - entering");
        }

        // Transition to the NOT_ATTACHED state
        synchronized (implObject)
        {
            // If we are already in the NOT_ATTACHED state then throw
            // NotLoadedException
            if (state == STATE_NOT_ATTACHED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " detach - (state == STATE_NOT_ATTACHED) - throwing NotLoadedException");
                }
                throw new NotLoadedException();
            }

            // Cancel the current attach if there is one. We do this by marking
            // the current attach flag as canceled. When the attach completes
            // it will see the flag and handle it.
            if ((state == STATE_ATTACHING) && (currentAttachFlag != null))
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " detach - ((state == STATE_ATTACHING) && (currentAttachFlag != null))");
                }
                currentAttachFlag.canceled = true;
                currentAttachFlag = null;
            }

            // Detach the underlying object carousel.
            if (objectCarousel != null)
            {
                Locator loc = objectCarousel.getLocator();
                objectCarousel.detach();
                ((AuthManager) ManagerManager.getInstance(AuthManager.class)).unregisterCRLMount(loc.toExternalForm());
            }

            // Update state variables to go to the NOT_ATTACHED state
            setState(STATE_NOT_ATTACHED);

        }
    }

    /**
     * This method returns the NSAP address of the <code>ServiceDomain</code>.
     *
     * @return the NSAP address of the <code>ServiceDomain</code>.
     * @exception NotLoadedException
     *                is thrown if the <code>ServiceDomain</code> is not
     *                attached.
     */
    public byte[] getNSAPAddress() throws NotLoadedException
    {
        synchronized (implObject)
        {
            // If we are in the NOT_ATTACHED state then throw NotLoadedException
            if (state == STATE_NOT_ATTACHED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " getNSAPAddress - (state == STATE_NOT_ATTACHED) " + "- throwing NotLoadedException");
                }
                throw new NotLoadedException();
            }
            return objectCarousel.getNSAPAddress();
        }
    }

    /**
     * Obtain a java.net.URL corresponding to a 'dvb:' locator. If the service
     * domain corresponding to the locator is attached and the file referenced
     * in the locator exists then an instance of <code>java.net.URL</code> is
     * returned which can be used to reference this file.
     *
     * @param l
     *            a locator object encapsulating a 'dvb:' locator which includes
     *            a 'dvb_abs_path' element.
     * @return a <code>java.net.URL</code> which can be used to access the file
     *         referenced by the 'dvb:' locator
     * @exception InvalidLocatorException
     *                if the locator is not a valid 'dvb:' locator or does not
     *                includes all elements including 'dvb_abs_path' element
     * @exception NotLoadedException
     *                is thrown if the locator is valid and includes enough
     *                information but it references a service domain which is
     *                not attached.
     * @exception FileNotFoundException
     *                if the service domain is attached but the file referenced
     *                by the locator does not exist
     */
    public static URL getURL(org.davic.net.Locator l) throws NotLoadedException, org.davic.net.InvalidLocatorException,
            FileNotFoundException
    {
        // The locator must be an OcapLocator
        OcapLocator loc;

        if (l instanceof OcapLocator)
        {
            loc = (OcapLocator) l;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getURL - locator is NOT an instance of OcapLocator"
                        + " - throwing new InvalidLocatorException (Invalid locator)");
            }
            throw new org.davic.net.InvalidLocatorException("Invalid locator " + l);
        }

        // Get the path segments for the file to be found
        String pathSegments = loc.getPathSegments();
        if (pathSegments == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getURL - No path segments specified for " + l + "throwing InvalidLocatorException");
            }
            throw new org.davic.net.InvalidLocatorException("No path segments specified for " + l);
        }

        ServiceDomain serviceDomain = null;

        // Get the list of service domains known to this caller context
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();
        CCData data = getCCData(cc);
        Vector domains = data.domains;

        // Iterate over this list of service domains looking for the one
        // matching the specified locator.
        synchronized (domains)
        {
            Iterator iterator = domains.iterator();
            while (iterator.hasNext())
            {
                // Get the next service domain object and set a flag if
                // it is the one we are looking for. Only mounted carousels
                // are checked.
                ServiceDomain sd = (ServiceDomain) iterator.next();
                synchronized (sd.implObject)
                {
                    if (sd.objectCarousel.match(l))
                    {
                        serviceDomain = sd;
                        break;
                    }
                }
            }
        }

        // Throw an exception if the service domain has not been attached by
        // this caller context.
        if (serviceDomain == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getURL - (serviceDomain == null) - throwing NotLoadedException");
            }
            throw new NotLoadedException("Service domain is not attached");
        }

        DSMCCObject dsmccObj = serviceDomain.getMountPoint();
        if (dsmccObj == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getURL - (dsmccObj == null) - throwing NotLoadedException");
            }
            throw new NotLoadedException("Service domain has not completed mounting yet");
        }

        // Check to make sure the file exists.
        DSMCCObject file = new DSMCCObject(dsmccObj, pathSegments);
        if (!file.exists())
        {
            throw new FileNotFoundException(file.getPath());
        }

        // Create the URL that should be used to access the file specified by
        // the locator.
        try
        {
            URL newUrl = new URL("file://" + file.getAbsolutePath());
            return newUrl;
        }
        catch (MalformedURLException e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("getUrl - caught MalformedURLException - "
                        + "throwing new org.davic.net.InvalidLocatorException " + "(No path segments specified)");
            }

            throw new org.davic.net.InvalidLocatorException("No path segments specified: " + e.getMessage());
        }
    }

    /**
     * Returns a <code>DSMCCObject</code> object describing the top level
     * directory of this <code>ServiceDomain</code>. If the ServiceDomain object
     * is not attached then null is returned.
     *
     * @return an instance of org.dvb.dsmcc.DSMCCObject if attached or null
     *         otherwise
     *
     * @since MHP 1.0.1
     */
    public DSMCCObject getMountPoint()
    {
        DSMCCObject obj = null;

        synchronized (implObject)
        {
            // Make sure we're attached before we try to create a DSMCCObject
            if (state == STATE_ATTACHED)
            {
                try
                {
                    String path = objectCarousel.getMountPoint();

                    // Unchecked exceptions may be thrown here
                    obj = new DSMCCObject(path);
                }
                catch (Exception e)
                {
                    // ignore errors, just end up returning null below
                    if (log.isDebugEnabled())
                    {
                        log.debug(id + " getMountPoint - caught Exception  - " + e.getMessage());
                    }
            }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(id + " getMountPoint - (state != STATE_ATTACHED)");
                }
        }
        }

        // Return the created object or null if it wasn't created
        return obj;
    }

    /**
     * Return whether the network connection for this service domain is
     * available. This return value is independent of whether the service domain
     * is attached or not. If a service domain is distributed across multiple
     * network connections (e.g. using the optional support for DSMCC over IIOP)
     * then this will reflect the availability of the network connection
     * carrying the object mounted to the mount point.
     *
     * @return true if the network connection for this service domain is
     *         available otherwise false
     * @since MHP 1.0.1
     */
    public boolean isNetworkConnectionAvailable()
    {
        if (objectCarousel != null)
        {
            return objectCarousel.isNetworkConnectionAvailable();
        }

        if (log.isDebugEnabled())
        {
            log.debug(id + " isNetworkConnectionAvailable - " + "(mountedLocator == null) - returning false");
        }

        return false;
    }

    /**
     * Return whether this service domain is in the attached or detached state.
     *
     * @return true if this service domain is in the attached state, otherwise
     *         false
     * @since MHP 1.0.1
     */
    public boolean isAttached()
    {
        return (state == STATE_ATTACHED);
    }

    /**
     * Return the locator for this service domain. If this ServiceDomain
     * instance was last attached by specifying a locator then an equivalent
     * locator shall be returned except if the original locator contained extra
     * information that is not necessary to identify the service domain in which
     * case this extra information is removed. If the attach was done with the
     * <code>attach(locator, int)</code> signature, the locator is complemented
     * with the component_tag value that the platform has identified during
     * attaching the ServiceDomain. If this ServiceDomain instance was last
     * attached by specifying an NSAP address then the locator shall be
     * generated from that address. If this ServiceDomain has never been
     * attached then null shall be returned.
     * <p>
     * The syntax of the NSAP address is defined in section titled
     * "LiteOptionsProfileBody" in annex B of the MHP specification. It contains
     * the same fields as the locator syntax specified in the System integration
     * aspects clause. The locator is constructed by taking the fields out of
     * the NSAP address and encoding them in the locator syntax together with
     * the component_tag value that the platform has identified during attaching
     * the ServiceDomain.
     *
     * @since MHP 1.0.1
     *
     * @return a locator for this service domain
     */
    public org.davic.net.Locator getLocator()
    {
        ObjectCarousel car = objectCarousel;
        if (car != null)
        {
            return (car.getLocator());
        }
        else
        {
            return null;
        }
    }

    /**
     * Set the state for this service domain
     *
     * @param newState
     *            the new state
     */
    private void setState(int newState)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " State changing from " + stateName(state) + " to " + stateName(newState));
        }

        // Get the domain list for the current caller context
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        CallerContext cc = ccm.getCurrentContext();
        CCData data = getCCData(cc);
        Vector domains = data.domains;

        // If transitioning to the ATTACHED state, then add this service domain
        // to the list of attached service domains for the current caller
        // context.
        if (state != STATE_ATTACHED && newState == STATE_ATTACHED)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " Adding " + this + " to list of attached domains for " + cc);
            }
            domains.add(this);
        }

        // If transitioning away from the ATTACHED state, then remove this
        // service domain from the list of attached service domains for the
        // current caller context.
        if (state == STATE_ATTACHED && newState != STATE_ATTACHED)
        {
            if (log.isDebugEnabled())
            {
                log.debug(id + " Removing " + this + " from list of attached domains for " + cc);
            }
            domains.remove(this);
        }

        // Set the current state as requested
        state = newState;
    }

    private String stateName(int state)
    {
        switch (state)
        {
            case STATE_NOT_ATTACHED:
                return "NOT_ATTACHED";
            case STATE_ATTACHING:
                return "ATTACHING";
            case STATE_ATTACHED:
                return "ATTACHED";
            default:
                return "UNKNOW";
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     *
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private static CCData getCCData(CallerContext cc)
    {
        // Retrieve the data for the caller context
        CCData data = (CCData) cc.getCallbackData(ServiceDomain.class);

        // If a data block has not yet been assigned to this caller context
        // then allocate one.
        if (data == null)
        {
            data = new CCData();
            cc.addCallbackData(data, ServiceDomain.class);
        }
        return data;
    }
}
