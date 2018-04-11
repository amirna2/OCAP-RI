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

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.transport.TransportStream;

import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.service.AbstractServiceType;

import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceDetailsHandle;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.ServiceHandle;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.string.MultiString;

/**
 * An implementation of <code>AbstractService</code> based upon a given
 * <code>AbstractServiceEntry</code>.
 * 
 * @author Aaron Kamienski
 */
public class AbstractServiceImpl extends ServiceExt implements AbstractService
{
    /**
     * Creates an instance of <code>AbstractService</code> based upon the given
     * <code>AbstractServiceEntry</code>.
     * 
     * @param entry
     *            the basis for this <code>AbstractService</code>
     */
    public AbstractServiceImpl(AbstractServiceEntry entry)
    {
        try
        {
            this.entry = entry;
            this.locator = new OcapLocator(entry.id);
            serviceDetails = new AbstractServiceDetailsImpl();
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            // Constructing the OcapLocator in this way will never fail.
        }
    }

    /**
     * Will always call <code>notifyFailure</code> method of the
     * <code>javax.tv.service.SIRequestor</code> argument, passing a value of
     * <code>javax.tv.service.SIRequestFailure.DATA_UNAVAILABLE</code>.
     * Following which <code>retrievedDetails</code> will return
     * <code>null</code>.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified via a call to
     *            <code>notifyFailure</code>
     * 
     * @return a inoperative instance of <code>SIRequest</code>
     * 
     * @see AbstractService
     */
    public SIRequest retrieveDetails(final SIRequestor requestor)
    {
        final SIRequestDummy request = new SIRequestDummy();
        // get the caller context
        CallerContext ctx = getCurrentContext();
        ctx.runInContextAsync(new Runnable()
        {
            public void run()
            {
                requestor.notifySuccess(new SIRetrievable[] { serviceDetails });
            }
        });
        return request;
    }

    /**
     * Returns a short service name or acronym. For example, in ATSC systems the
     * service name is provided by the the PSIP VCT; in DVB systems, this
     * information is provided by the DVB Service Descriptor or the Multilingual
     * Service Name Descriptor. The service name may also be user-defined.
     * 
     * @return A string representing this service's short name. If the short
     *         name is unavailable, the string representation of the service
     *         number is returned.
     */
    public String getName()
    {
        return (entry.name == null) ? "" : entry.name;
    }

    /**
     * Always returns <code>false</code>.
     * 
     * @return <code>false</code>
     * @see AbstractService
     */
    public boolean hasMultipleInstances()
    {
        return false;
    }

    /**
     * Returns a locator of the form <code>"ocap://0x<i>XXXXXX</i>"</code> where
     * <i>XXXXXX</i> represents the <i>serviceId</i>
     * 
     * @return a <code>OcapLocator</code> for this abstract service
     */
    public Locator getLocator()
    {
        return locator;
    }

    /**
     * Tests two <code>Service</code> objects for equality. Returns
     * <code>true</code> if and only if:
     * <ul>
     * <li><code>obj</code>'s class is the same as the class of this
     * <code>Service</code>, and
     * <p>
     * <li><code>obj</code>'s <code>Locator</code> is equal to the
     * <code>Locator</code> of this <code>Service</code> (as reported by
     * <code>Service.getLocator()</code>, and
     * <p>
     * <li><code>obj</code> and this object encapsulate identical data.
     * </ul>
     * 
     * @param obj
     *            The object against which to test for equality.
     * 
     * @return <code>true</code> if the two <code>Service</code> objects are
     *         equal; <code>false</code> otherwise.
     */
    public boolean equals(Object obj)
    {
        return obj != null && getClass() == obj.getClass() && locator.equals(((Service) obj).getLocator())
        // OCAP says that they are only equal if the data encapsulated
                // by the objects are also equal. This includes the list of
                // applications.
                && getAppSet().equals(((AbstractServiceImpl) obj).getAppSet());
    }

    /**
     * Reports the hash code value of this <code>Service</code>. Two
     * <code>Service</code> objects that are equal will have identical hash
     * codes.
     * 
     * @return The hash code value of this <code>Service</code>.
     */
    public int hashCode()
    {
        return locator.hashCode();
    }

    // Description copied from ServiceNumber
    public int getServiceNumber()
    {
        // always return '-1' for an abstract-service service-number
        return -1;
    }

    // Description copied from ServiceMinorNumber
    public int getMinorNumber()
    {
        // always return '-1' for an abstract-service service-minor-number
        return -1;
    }

    // Description copied from AbstractService
    public java.util.Enumeration getAppIDs()
    {
        return getAppSet().keys();
    }

    // Description copied from AbstractService
    public java.util.Enumeration getAppAttributes()
    {
        final ApplicationManager appMgr =
            (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
        return new Enumeration()
        {
            private Enumeration e = getAppSet().elements();

            public boolean hasMoreElements()
            {
                return e.hasMoreElements();
            }

            public Object nextElement()
            {
                return appMgr.createAppAttributes((AppEntry)e.nextElement(), AbstractServiceImpl.this);
            }
        };
    }

    // Override parent method
    public ServiceDetails getDetails()
    {
        return serviceDetails;
    }

    /**
     * Returns the serviceId for this <code>AbstractService</code>.
     * 
     * @return the serviceId for this <code>AbstractService</code>.
     */
    int getServiceId()
    {
        return entry.id;
    }

    /**
     * Overrides {@link Object#toString}.
     */
    public String toString()
    {
        return "AbstractService[0x" + Integer.toHexString(entry.id) + "]";
    }

    public void registerForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    public void unregisterForPSIAcquisition()
    {
        // TODO Auto-generated method stub
    }

    /**
     * Returns the set of <code>applications</code> as a <code>Hashtable</code>.
     * <p>
     * As a side effect the {@link #apps} variable is set if it is currently
     * <code>null</code>. No synchronization is in place here, but the worst
     * that could happen is that two threads go through the process individually
     * and one <code>Hashtable</code> becomes garbage immediately.
     * 
     * @return set of visible applications as a <code>Hashtable</code> which
     *         maps <code>AppID</code>s to <code>AppEntry</code> instances
     */
    private Hashtable getAppSet()
    {
        if (apps == null)
        {
            Hashtable newApps = new Hashtable();

            if (entry.apps != null)
            {
                for (Enumeration e = entry.apps.elements(); e.hasMoreElements();)
                {
                    AppEntry app = (AppEntry)e.nextElement();
                    newApps.put(app.id, app);
                }
            }

            apps = newApps;
        }
        return apps;
    }

    /**
     * ServiceDetails for this AbstractService
     */
    private ServiceDetails serviceDetails;

    /**
     * The locator for this service.
     */
    private OcapLocator locator;

    /**
     * The basis for this <code>AbstractService</code>.
     */
    private AbstractServiceEntry entry;

    /**
     * The set of applications visible in this abstract service. This is the set
     * of applications in the {@link AbstractServiceEntry#apps}, with
     * duplication <code>AppID</code>s removed. When duplicates are found, the
     * entry with the highest priority and highest launchOrder is kept.
     */
    private Hashtable apps;

    private CallerContext getCurrentContext()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        return ccm.getCurrentContext();
    }

    public Service createSnapshot(SICache siCache)
    {
        throw new UnsupportedOperationException();
    }

    public MultiString getNameAsMultiString()
    {
        return null;
    }

    public ServiceHandle getServiceHandle()
    {
        return null;
    }

    /**
     * Always returns <code>OCAP_ABSTRACT_SERVICE</code>.
     * 
     * @return {@link AbstractServiceType#OCAP_ABSTRACT_SERVICE}
     * @see AbstractService
     */
    public ServiceType getServiceType()
    {
        return AbstractServiceType.OCAP_ABSTRACT_SERVICE;
    }

    public Object getID()
    {
        return "AbstractServiceImpl" + entry.id;
    }

    public Object createLanguageSpecificVariant(String language)
    {
        return this;
    }

    public Object createLocatorSpecificVariant(Locator locator)
    {
        throw new UnsupportedOperationException();
    }

    public String getPreferredLanguage()
    {
        return null;
    }

    // a dummy request to return from the new async methods in this class.
    // cancel doesn't do anything - what would it do?
    private static class SIRequestDummy implements SIRequest
    {
        public boolean cancel()
        {
            // do nothing.
            return false;
        }
    }

    private class AbstractServiceDetailsImpl extends ServiceDetailsExt
    {

        public ServiceDetails createSnapshot(SICache siCache)
        {
            throw new UnsupportedOperationException();
        }

        public MultiString getLongNameAsMultiString()
        {
            return null;
        }

        public int getPcrPID()
        {
            return 0x1FFF;
        }

        public SIRequest retrievePcrPID(SIRequestor requestor)
        {
            return null;
        }
        
        public int getProgramNumber()
        {
            return -1;
        }

        public ServiceDetailsHandle getServiceDetailsHandle()
        {
            return null;
        }

        public int getSourceID()
        {
            return entry.id;
        }

        public int getAppID()
        {
            // Fix me!!
            return -1;
        }

        public TransportStream getTransportStream()
        {
            return null;
        }

        public SIRequest retrieveCarouselComponent(SIRequestor requestor)
        {
            return dummyRetrieve(requestor);
        }

        public SIRequest retrieveCarouselComponent(SIRequestor requestor, int carouselID)
        {
            return dummyRetrieve(requestor);
        }

        public SIRequest retrieveComponentByAssociationTag(SIRequestor requestor, int associationTag)
        {
            return dummyRetrieve(requestor);
        }

        public SIRequest retrieveDefaultMediaComponents(SIRequestor requestor)
        {
            return dummyRetrieve(requestor);
        }

        public Object getID()
        {
            return "AbstractServiceDetailsImpl" + entry.id;
        }

        public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
        {
            // no-op function since there are no ServiceComponents for abstract
            // services.
        }

        public DeliverySystemType getDeliverySystemType()
        {
            return DeliverySystemType.CABLE;
        }

        public String getLongName()
        {
            return AbstractServiceImpl.this.getName();
        }

        // see annex t 2.2.16.3
        public ProgramSchedule getProgramSchedule()
        {
            return null;
        }

        public Service getService()
        {
            return AbstractServiceImpl.this;
        }

        /**
         * Always returns <code>OCAP_ABSTRACT_SERVICE</code>.
         * 
         * @return {@link AbstractServiceType#OCAP_ABSTRACT_SERVICE}
         * @see AbstractService
         */
        public ServiceType getServiceType()
        {
            return AbstractServiceImpl.this.getServiceType();
        }

        public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
        {
            // no-op function since there are no ServiceComponents for abstract
            // services.
        }

        // annex T specifies that for abstract services there is never any data
        // for this call
        public SIRequest retrieveComponents(final SIRequestor requestor)
        {
            return dummyRetrieve(requestor);
        }

        // annex T specifies that for abstract services there is never any data
        // for this call
        public SIRequest retrieveServiceDescription(final SIRequestor requestor)
        {
            return dummyRetrieve(requestor);
        }

        private SIRequest dummyRetrieve(final SIRequestor requestor)
        {
            final SIRequestDummy request = new SIRequestDummy();
            // get the caller context
            CallerContext ctx = getCurrentContext();
            ctx.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // per the spec return data unavailable for abstract
                    // services.
                    requestor.notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
                }
            });
            return request;
        }

        public Locator getLocator()
        {
            return AbstractServiceImpl.this.getLocator();
        }

        // FIXME: what is the right type here? passed in at construction
        // to make the differentiation between XAIT and programatic SI?
        public ServiceInformationType getServiceInformationType()
        {
            return ServiceInformationType.UNKNOWN;
        }

        public Date getUpdateTime()
        {
            return new Date(entry.time);
        }

        public int[] getCASystemIDs()
        {
            return new int[0];
        }

        public boolean isFree()
        {
            return true;
        }

        public int getServiceNumber()
        {
            return -1;
        }

        public int getMinorNumber()
        {
            return -1;
        }

        public Object createLanguageSpecificVariant(String language)
        {
            return this;
        }

        public String getPreferredLanguage()
        {
            return null;
        }

        public boolean equals(Object other)
        {
            if (other instanceof AbstractServiceDetailsImpl)
            {
                AbstractServiceDetailsImpl otherDetails = (AbstractServiceDetailsImpl)other;
                return (getUpdateTime().equals(otherDetails.getUpdateTime()) &&
                        getLocator().equals(otherDetails.getLocator()));
            }
            return false;
        }
       
        public int hashCode()
        {
            return getUpdateTime().hashCode() ^ getLocator().hashCode();
        }
        
    }

}
