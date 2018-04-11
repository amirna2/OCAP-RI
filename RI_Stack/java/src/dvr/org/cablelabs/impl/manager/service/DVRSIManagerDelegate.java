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

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.SegmentedRecordedService;

import org.cablelabs.impl.manager.recording.OcapRecordedServiceExt;
import org.cablelabs.impl.manager.recording.RecordedServiceImpl;
import org.cablelabs.impl.manager.recording.RecordedServiceLocator;

/**
 * DVR implementation of <code>SIManager</code>.
 * 
 * @author Todd Earles
 */
public class DVRSIManagerDelegate implements SIManagerDelegate
{
    private static final Logger log = Logger.getLogger(DVRSIManagerDelegate.class.getName());

    // Description copied from SIManager
    public Service getService(Locator locator) throws InvalidLocatorException, SecurityException
    {
        // Handle recorded service locators here. Handle all other locators in
        // the super-class.
        if (log.isDebugEnabled())
        {
            log.debug("DVRSIManagerDelegate.getService(" + locator + ") class " + locator.getClass());
        }
        
        if (locator instanceof RecordedServiceLocator)
        {
            RecordedService service = getRecordedService((RecordedServiceLocator)locator);
            if (service == null)
            {
                throw new InvalidLocatorException(locator, "Recorded service not found");
            }
            return service;
        }
        
        return null;
    }

    /**
     * Get the RecordedService which has a locator with the specified external
     * form.
     * 
     * @param locator
     *            The recorded service url
     * @return The recorded service or null if not found
     */
    private RecordedService getRecordedService(final RecordedServiceLocator locator)
    {
        // TODO(Todd): What kind of security check should be done here?

        if (log.isDebugEnabled())
        {
            log.debug("getRecordedService for " + locator);
        }

        final RecordingManager rm = RecordingManager.getInstance();
        final int recordingID = locator.getRecordingID();
        final int segmentIndex = locator.getSegmentIndex();

        RecordingRequest rr = rm.getRecordingRequest(recordingID);

        if (!(rr instanceof LeafRecordingRequest))
        {
            return null;
        }
        
        RecordedService rs;
        try
        {
            rs = ((LeafRecordingRequest)rr).getService();
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Caught an exception trying to access recorded service from " 
                          + rr + " via locator " + locator, e);
            }
            return null;
        }
        
        if ((rs == null) || (segmentIndex == -1) || !(rs instanceof SegmentedRecordedService))
        { // This Locator doesn't refer to a RecordingRequest that has a RecordedService
          //  (it hasn't started or the RS has been deleted) or the Locator doesn't refer
          //  to a segment in a SegmentedRecordedService. Either way, we're done here. 
            return rs;
        }
        
        // Assert: we have a RS Locator with an index reference and a non-null 
        //         SegmentedRecordedService. Try to find the segment.
        final RecordedService segments[] = ((SegmentedRecordedService)rs).getSegments();
        for (int i=0; i<segments.length; i++)
        {
            OcapRecordedServiceExt orse = (RecordedServiceImpl)segments[i];
            
            if (orse.getSegmentIndex() == segmentIndex)
            {
                return orse;
            }
        }

        // The recorded service was not found
        return null;
    }
}
