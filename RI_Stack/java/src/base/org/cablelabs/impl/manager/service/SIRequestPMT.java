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

import javax.tv.locator.Locator;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramMapTable;

import org.cablelabs.impl.ocap.si.ProgramMapTableExt;
import org.cablelabs.impl.service.ProgramMapTableHandle;
import org.cablelabs.impl.service.SIDatabaseException;
import org.cablelabs.impl.service.SINotAvailableYetException;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;

public class SIRequestPMT extends SIRequestImpl
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(SIRequestPMT.class);

    /** Identity of this object */
    protected String id = "@" + Integer.toHexString(System.identityHashCode(this));

    public SIRequestPMT(SICacheImpl siCache, Locator locator, SIRequestor requestor)
    {
        super(siCache, null, requestor);

        // If the locator is not a valid PAT request locator, just cancel this
        // request. This will allow us to return a request object and the
        // attemptDelivery function will succeed as a no-op.
        if (!validateLocator(locator)) canceled = true;

        ocapLocator = (OcapLocator) locator;
    }

    public SIRequestPMT(SICacheImpl siCache, Service s, SIRequestor requestor)
    {
        super(siCache, null, requestor);

        service = s;
    }
    
    public synchronized boolean attemptDelivery()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Attempting delivery");
        }

        // The request has already been canceled so consider the request
        // to be finished.
        if (canceled)
            return true;

        int sourceID=-1;
        int frequency=-1;
        int programNumber=0;
        int mode=-1; 
        
        if(ocapLocator != null)
        {
            sourceID = ocapLocator.getSourceID();
            frequency = ocapLocator.getFrequency();
            programNumber = ocapLocator.getProgramNumber();
            mode = ocapLocator.getModulationFormat();
            if (mode == -1)
            {
                // An unspecified/unknown modulation mode of -1
                // implies a QAM 256 modulation per OCAP spec
                // See Javadoc for org.ocap.net.OcapLocator.getModulationFormat()
                mode = 0x10;
            }            
        }


        // Attempt delivery
        try
        {
            // Get the handle to the specified transport
            ProgramMapTableHandle pmtHandle = null;

            if(service != null)
            {
                ServiceExt sExt = (ServiceExt)service;
                int handle = ((ServiceDetailsExt)sExt.getDetails()).getServiceDetailsHandle().getHandle();
                if (log.isDebugEnabled())
                {
                    log.debug(id + " Attempting delivery service: " + service + " handle: " + handle);
                }
                pmtHandle = siDatabase.getProgramMapTableByService(handle);
            }
            else if (sourceID != -1)
            {
                pmtHandle = siDatabase.getProgramMapTableBySourceID(sourceID);
            }
            else if(programNumber != -1)
            {
                pmtHandle = siDatabase.getProgramMapTableByProgramNumber(frequency, mode, programNumber);
            }

            // Get the PMT object from the cache if it is present. Otherwise,
            // create it from the database and add it to the cache.
            ProgramMapTable[] pmt = new ProgramMapTable[1];
            pmt[0] = siCache.getCachedProgramMapTable(pmtHandle);
            if (pmt[0] == null)
            {
                pmt[0] = siDatabase.createProgramMapTable(pmtHandle);
                siCache.putCachedProgramMapTable(pmtHandle, pmt[0]);
            }
            
            if(!(this.siDatabase instanceof SISnapshotDatabase))
            {
                // Make sure we are tuned to the transport stream associated with
                // the SI
                checkTuned(pmt[0]);
            }

            if(ocapLocator != null)
            {
                ((ProgramMapTableExt) pmt[0]).setLocator(ocapLocator);                
            }
            // Notify the requestor
            notifySuccess(pmt);
            return true;
        }
        catch (SINotAvailableYetException e)
        {
            // Try again later
            return false;
        }
        catch (SIDatabaseException e)
        {
            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            return true;
        }
        catch (Exception e)
        {
            notifyFailure(SIRequestFailureType.DATA_UNAVAILABLE);
            return true;
        }
    }

    public synchronized boolean cancel()
    {
        if (log.isDebugEnabled())
        {
            log.debug(id + " Request canceled");
        }

        // Return if already canceled
        if (canceled)
            return false;

        // Cancel the request
        boolean result = siCache.cancelServiceDetailsRequest(this);
        if (result == true)
            notifyFailure(SIRequestFailureType.CANCELED);
        canceled = true;
        return result;
    }

    public String toString()
    {
        return super.toString() + "[Locator = " + ocapLocator + "]";
    }

    private boolean validateLocator(Locator locator)
    {
        // Locator can not be null
        if (locator == null) return false;

        // Check for valid OcapLocator
        OcapLocator ol = null;
        if (locator instanceof OcapLocator)
        {
            ol = (OcapLocator) locator;
        }
        else
        {
            try
            {
                ol = new OcapLocator(locator.toExternalForm());
            }
            catch (Exception e)
            {
            }
        }
        if (ol == null) return false;

        // Locator must describe either a source ID or frequency/program#
        if (ol.getSourceID() == -1)
        {
            // Frequency may be -1 if this is an OOB PMT request, but program
            // number
            // can never be -1
            if (ol.getProgramNumber() == -1) return false;
        }

        return true;
    }

    OcapLocator ocapLocator = null;
    Service service = null;
}