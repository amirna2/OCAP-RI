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

package org.cablelabs.impl.manager.service;

import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.ocap.si.ProgramAssociationTableExt;
import org.cablelabs.impl.ocap.si.ProgramMapTableExt;
import org.cablelabs.impl.service.OcapLocatorImpl;
import org.cablelabs.impl.service.SIDatabase;
import org.cablelabs.impl.service.SINotAvailableException;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;

/**
 * A base class for the implementation of all <code>SIRequest</code> types.
 * 
 * @author Todd Earles
 */
public abstract class SIRequestImpl implements SIRequest
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestImpl.class);

    /** Identity of this object */
    protected String id = "@" + Integer.toHexString(System.identityHashCode(this));

    private static final CallerContextManager ccManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    protected static final ServiceManager sMgr = 
        (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
    
    /**
     * Construct an <code>SIRequest</code>
     * 
     * @param siCache
     *            The <code>SICache</code> to which this request belongs or null
     *            if none.
     * @param language
     *            The language preference for the request or null if no
     *            preference.
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     */
    public SIRequestImpl(SICacheImpl siCache, String language, SIRequestor requestor)
    {
        this.siCache = siCache;
        this.siDatabase = (siCache == null) ? null : siCache.getSIDatabase();
        this.language = language;
        this.requestor = requestor;

        // Save the caller context so we can use it later to perform the
        // asynchronous notification.
        requestingCallerContext = ccManager.getCurrentContext();

        // Save the creation time so that we can age and timeout this
        // request if it cannot be satisfied.
        creationTime = System.currentTimeMillis();
        
        // SI request timeout is set to 15 sec by default
        // It is configurable via ocap.properties
        asyncTimeout = sMgr.getRequestAsyncTimeout();
        expirationTime = creationTime + asyncTimeout;
    }

    /**
     * Return the time this request was created.
     * 
     * @return The creation time in milliseconds since midnight, January 1, 1970
     *         UTC.
     */
    public long getCreationTime()
    {
        return creationTime;
    }

    /**
     * Return the time this request will expire.
     * 
     * @return the expiration time
     * expiration time is set to creation time + SI request timeout value
     */
    public long getExpirationTime()
    {
        return expirationTime;
    }
    
    /**
     * Set the time this request will expire.
     * 
     */
    public void setExpirationTime(long expireTime)
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + "SIRequestImpl setExpirationTime: " + expireTime);
        }
        expirationTime = expireTime;
    }
    
    /**
     * Attempt delivery of the requested service information. This method must
     * not block waiting for the requested data.
     * 
     * @return Returns true if the request has completed (with success or
     *         failure). Returns false if the request is still pending because
     *         it cannot be satisfied at this time.
     */
    public abstract boolean attemptDelivery();

    /**
     * Notifies the SIRequestor of successful asynchronous SI retrieval.
     * 
     * @param result
     *            The previously requested data.
     */
    protected synchronized void notifySuccess(final SIRetrievable[] result)
    {
        // If notification has already been sent then do nothing
        if (canceled) return;

        //if (LOGGING) log.debug(id + " Retrieval succeeded with " + Arrays.toString(result));
        try
        {
            requestingCallerContext.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    requestor.notifySuccess(result);
                }
            });
        }
        catch (IllegalStateException e)
        {
            // The application exited already - just ignore this
        }

        // Mark as canceled since this request has now been fulfilled and can
        // no longer be canceled.
        canceled = true;
    }

    /**
     * Notifies the SIRequestor of unsuccessful asynchronous SI retrieval.
     * 
     * @param reason
     *            The reason why the asynchronous request failed.
     */
    protected synchronized void notifyFailure(final SIRequestFailureType reason)
    {
        // If notification has already been sent then do nothing
        if (canceled)
            return;

        if (log.isDebugEnabled())
        {
            log.debug(id + " Retrieval failed due to " + reason);
        }
        try
        {
            requestingCallerContext.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Notify the requestor
                    requestor.notifyFailure(reason);
                }
            });
        }
        catch (IllegalStateException e)
        {
            // The application exited already - just ignore this
        }

        // Mark as canceled since this request has now been fulfilled and can
        // no longer be canceled.
        canceled = true;
    }

    /**
     * Determine whether or not the transport stream represented by the given
     * <code>SIRetrievable</code> is currently tuned by any of the tuners on the
     * host
     * 
     * @param loc
     *            The <code>SIRetrievable</code>(s) of interest
     * @throws SINotAvailableException
     *             If none of the tuners on the host are currently tuned to the
     *             transport stream associated with the given SI data.
     */
    protected void checkTuned(SIRetrievable siElement) throws SINotAvailableException
    {
        TransportStreamExt tsExt = null;
        if (siElement instanceof ServiceComponentExt)
        {
            ServiceComponentExt serviceCompExt = (ServiceComponentExt) siElement;
            tsExt = (TransportStreamExt) ((ServiceDetailsExt) (serviceCompExt.getServiceDetails())).getTransportStream();
        }
        else if (siElement instanceof ProgramAssociationTableExt)
        {
            ProgramAssociationTableExt pat = (ProgramAssociationTableExt) siElement;
            tsExt = (TransportStreamExt) pat.getTransportStream();
        }
        else if (siElement instanceof ProgramMapTableExt)
        {
            ProgramMapTableExt pmt = (ProgramMapTableExt) siElement;
            tsExt = (TransportStreamExt) pmt.getTransportStream();
        }

        if (log.isDebugEnabled())
        {
            log.debug(id + " checkTuned... tsExt.getFrequency(): " + tsExt.getFrequency());
        }
        
        // Out-of-band transport stream is always considered to be tuned
        if (tsExt == null || tsExt.getFrequency() == 0xFFFFFFFF || tsExt.getFrequency() == OcapLocatorImpl.HN_FREQUENCY)
            return;

        // Make a DAVIC transport stream object associated with each tuner on
        // the box, then check to see if any tuner is actually tuned to that
        // transport stream
        NetworkInterface nis[] = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
        for (int i = 0; i < nis.length; ++i)
        {
            // Create a DAVIC transport stream
            ExtendedNetworkInterface ni = (ExtendedNetworkInterface) NetworkInterfaceManager.getInstance()
                    .getNetworkInterface(tsExt.getDavicTransportStream(nis[i]));

            // If the tuner is tuned to this transport stream, we can return.
            if (ni != null && tsExt.getFrequency() == ni.getTransportStreamFrequency()) return;
        }

        // If none of the tuners on the box are tuned to the transport stream,
        // we
        // will reach the end of the loop and fall to here
        throw new SINotAvailableException("Not tuned to the transport stream");
    }

    // Description copied from Object
    public String toString()
    {
        return getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(this));
    }

    /** The SI cache to which this request belongs */
    final SICacheImpl siCache;

    /** The SI database to use in attempts to satisfy this request */
    final SIDatabase siDatabase;

    /** The preferred language for the request */
    final String language;

    /** The requestor to be notified */
    protected final SIRequestor requestor;

    /** The caller context of the requestor */
    private final CallerContext requestingCallerContext;

    /** The creation time for this request */
    private final long creationTime;

    /** The expiration time for this request */
    protected long expirationTime;
    
    private int asyncTimeout;
    
    /** True if this request has been canceled (or already fulfilled) */
    protected boolean canceled = false;
}
