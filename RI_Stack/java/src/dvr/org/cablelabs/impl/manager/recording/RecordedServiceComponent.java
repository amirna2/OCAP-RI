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

package org.cablelabs.impl.manager.recording;

import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.SIException;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.ServiceComponent;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.navigation.StreamType;

import org.cablelabs.impl.manager.recording.RecordedServiceImpl.RecordedServiceDetails;
import org.cablelabs.impl.recording.RecordedServiceComponentInfo;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceComponentHandle;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.util.string.MultiString;
import org.davic.net.InvalidLocatorException;
import org.ocap.shared.dvr.RecordedService;

/**
 * An implementation of <code>ServiceComponent</code> for recorded
 * ServiceComponents. This class utilizes a
 * <code>RecordedServiceComponentInfo</code> object to save its data.
 * 
 * @see javax.tv.service.navigation.ServiceComponent
 * @see org.cablelabs.impl.recording.RecordedServiceComponentInfo
 * 
 * @author Brian Greene
 * @author Todd Earles
 */
public class RecordedServiceComponent extends ServiceComponentExt
{
    // The persisted information that backs this component.
    private RecordedServiceComponentInfo info;

    private RecordedService service;

    private ServiceDetails serviceDetails;

    private final Object siObjectID;

    private RecordedServiceLocator locator;

    /**
     * Constructor
     * 
     * @param details
     *            RecordedServiceDetails for this RecordedServiceComponent
     * 
     * @param info
     *            The RecordedServiceComponentInfo for this recorded service
     *            component.
     */
    public RecordedServiceComponent(RecordedServiceDetails details, RecordedServiceComponentInfo info)
    {
        this.info = info;
        this.service = (RecordedService) details.getService();
        this.serviceDetails = details;
        this.locator = null;

        siObjectID = (String) ((ServiceDetailsExt) serviceDetails).getID() + ":" + getPID();
    }

    // Description copied from UniqueIdentifier
    public Object getID()
    {
        return siObjectID;
    }

    // Description copied from ServiceExt
    public ServiceComponent createSnapshot(SICache siCache)
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from LanguageVariant
    public String getPreferredLanguage()
    {
        return info.getAssociatedLanguage();
    }

    // Description copied from LanguageVariant
    public Object createLanguageSpecificVariant(String language)
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from ServiceDetailsVariant
    public Object createServiceDetailsSpecificVariant(ServiceDetails serviceDetails)
    {
        throw new UnsupportedOperationException();
    }

    // Description copied from ServiceComponentExt
    public ServiceComponentHandle getServiceComponentHandle()
    {
        // This object is not available via the SI database
        return null;
    }

    // Description copied from ServiceComponentExt
    public ServiceDetails getServiceDetails()
    {
        return serviceDetails;
    }

    // Description copied from ServiceComponentExt
    public int getPID()
    {
        return info.getPID();
    }

    // Description copied from ServiceComponentExt
    public int getComponentTag() throws SIException
    {
        throw new SIException("No component tag for this component");
    }

    // Description copied from ServiceComponentExt
    public int getAssociationTag() throws SIException
    {
        throw new SIException("No association tag for this component");
    }

    // Description copied from ServiceComponentExt
    public int getCarouselID() throws SIException
    {
        throw new SIException("No carousel for this component");
    }

    // Description copied from ServiceComponentExt
    public short getElementaryStreamType()
    {
        // TODO(Todd): Where is the elementary stream type for this component?
        return info.getElemStreamType();
    }

    // Description copied from ServiceComponent
    public String getName()
    {
        return info.getName();
    }

    // Description copied from ServiceComponentExt
    public MultiString getNameAsMultiString()
    {
        // TODO(Todd): Use the real multi-string when it is available.
        return new MultiString(new String[] { "" }, new String[] { getName() });
    }

    // Description copied from ServiceComponent
    public String getAssociatedLanguage()
    {
        return info.getAssociatedLanguage();
    }

    // Description copied from ServiceComponent
    public StreamType getStreamType()
    {
        return info.getStreamType();
    }

    // Description copied from ServiceComponent
    public Service getService()
    {
        return this.service;
    }

    // Description copied from SIElement
    public Locator getLocator()
    {
        if (this.locator == null)
        { // We lazily initialize the locator and retain it
            final String locatorString = this.serviceDetails.getLocator().toExternalForm() 
                                            + ".+0x" + Integer.toHexString(getPID());
            try
            {
                this.locator = new RecordedServiceLocator(locatorString);
            }
            catch (InvalidLocatorException ile)
            {
                throw new IllegalArgumentException( "could not create RecordedServiceLocator for " 
                                                    + locatorString + ": " + ile.getMessage() );
            }
        }
        return this.locator;
    }

    // Description copied from SIElement
    public ServiceInformationType getServiceInformationType()
    {
        return info.getServiceInformationType();
    }

    // Description copied from SIRetrievable
    public Date getUpdateTime()
    {
        return info.getUpdateTime();
    }

    // Description copied from SIElement
    public boolean equals(Object o)
    {
        if (o == null || !(this.getClass().isInstance(o))) return false;
        RecordedServiceComponent b = (RecordedServiceComponent) o;

        // Test the related info objects that the components rely on and the
        // related services.
        return equalObjects(this.info, b.info) && equalObjects(this.service, b.service);
    }
    
    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + (this.info == null ? 0 : this.info.hashCode());
        hash = 31 * hash + (this.service == null ? 0 : this.service.hashCode());
        return hash;
    }

    /**
     * Compare 2 objects for equality.
     */
    private boolean equalObjects(Object a, Object b)
    {
        if ((a == null && (b == null))) return true;
        if ((a == null) != (b == null)) return false;
        if (a != null && a.equals(b)) return true;
        return false;
    }

    public String toString()
    {
        return "RecordedServiceComponent{" + "info=" + info + ", service=" + service + ", serviceDetails="
                + serviceDetails + "} " + super.toString();
    }
}
