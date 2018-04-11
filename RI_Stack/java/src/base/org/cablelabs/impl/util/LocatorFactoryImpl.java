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

package org.cablelabs.impl.util;

import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.locator.LocatorFactory;
import javax.tv.locator.MalformedLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;

import org.ocap.net.OcapLocator;

import org.cablelabs.impl.service.NetworkLocator;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportStreamExt;
import org.cablelabs.impl.spi.SPIService;

/**
 * This class implements a factory for the creation of Java TV locators. Per
 * OCAP these must be of type OcapLocator.
 * 
 * @see javax.tv.locator.Locator
 * 
 * @author Todd Earles
 * @author Brian Greene
 * @author Alan Cossitt (1102)
 */
public class LocatorFactoryImpl extends LocatorFactory
{
    // Description inherited from LocatorFactory
    public Locator createLocator(String locatorString) throws MalformedLocatorException
    {
        Locator locator = null;
        if (locatorString == null) throw new NullPointerException();
        // see the constructor for OcapLocator - this is the same test there.
        if (locatorString != null && locatorString.startsWith("ocap:"))
        {
            try
            {
                locator = new OcapLocator(locatorString);
            }
            catch (org.davic.net.InvalidLocatorException e)
            {
                throw new MalformedLocatorException();
            }
        }
        else if (locatorString.length() > 7 && locatorString.substring(0, 7).equals("network"))
        {
            try
            {
                locator = new NetworkLocator(locatorString);
            }
            catch (org.davic.net.InvalidLocatorException e)
            {
                throw new MalformedLocatorException();
            }
        }
        else
            throw new MalformedLocatorException();
        return locator;
    }

    public final OcapLocator transformServiceToLocator(ServiceExt service) 
    {            
        try
        {
            ServiceDetailsExt details = (ServiceDetailsExt) service.getDetails();

            OcapLocator loc = null;
            // If Service is SPI service the fpq form may not be known
            // Just return the service locator
            if((Service)service instanceof SPIService)
            {
                loc = LocatorUtil.convertJavaTVLocatorToOcapLocator(service.getLocator());
                return loc;
            }
            
            TransportStreamExt ts = (TransportStreamExt) details.getTransportStream();
            if (ts == null) 
                return null;

            if(details.isAnalog())
            {
                loc = new OcapLocator(ts.getFrequency(), ts.getModulationFormat());            	
            }
            else
            {
                loc = new OcapLocator(ts.getFrequency(), details.getProgramNumber(), ts.getModulationFormat());
                
            }
            
            return loc;
        }
        catch (Exception e)
        {
            SystemEventUtil.logEvent("Exception thrown, msg is: " + e.getMessage());
        }

        return null;
    }
    
    // Description inherited from LocatorFactory
    public Locator[] transformLocator(Locator source) throws InvalidLocatorException
    {
        OcapLocator locator;
        try
        {
            locator = new OcapLocator(source.toExternalForm());
        }
        catch (org.davic.net.InvalidLocatorException e)
        {
            throw new InvalidLocatorException(source, source.toExternalForm());
        }

        /*
         * This method implements ECN 1102:
         * 
         * This subsection complies with [DVB-GEM 1.0.2] Section 11.11.11. When
         * the javax.tv.locator.LocatorFactory.transformLocator method is called
         * and the parameter is a source_id based locator, and if the frequency
         * and program_number mappings are known for the source_id the
         * implementation SHALL return a locator with those terms.
         * 
         * If the frequency or program_number to source_id mappings are not
         * known the implementation SHALL return the parameter locator.
         */

        // isService() returns false for locators already frequency, program
        // number, and modulation, based.
        // but some locators that do return a non-negative source ID are
        // actually OOB so there needs
        // to be a check.
        if (locator.getSourceID() != -1 && !LocatorUtil.isAbstract(source))
        {

            ServiceDetails[] details;
            try
            {

                SIManagerExt siManager = (SIManagerExt) SIManager.createInstance();

                // Get the ServiceDetails for the locator; This blocks on an
                // underlying asychronous call
                details = siManager.getServiceDetails(locator);
            }
            catch (Exception e)
            {
                SystemEventUtil.logEvent("Exception thrown, msg is: " + e.getMessage());
                return getDefaultTransform(source);
            }

            /*
             * okay, lets get any known data so we don't lose anything. The
             * returned locator must contain all the component information
             * contained in the original locator
             * 
             * These are the valid forms. Note none of them have source ID so
             * there is no form with a source id that already passes ECN 1102.:
             * 
             * public OcapLocator (int frequency, int programNumber, int
             * modulationFormat)
             * 
             * public OcapLocator(int frequency, int programNumber, int
             * modulationFormat, short[] streamType, String[]
             * ISO639LanguageCode, int eventID, String pathSegments)
             * 
             * public OcapLocator(int frequency, int programNumber, int
             * modulationFormat, short[] streamType, int[] index, int eventID,
             * String pathSegments)
             * 
             * public OcapLocator(int frequency, int programNumber, int
             * modulationFormat, int[] PID, int eventID, String pathSegments)
             * 
             * public OcapLocator(int frequency, int programNumber, int
             * modulationFormat, int eventID, int[] componentTags, String
             * pathSegments)
             * 
             * So possible values: int frequency, int programNumber, int
             * modulationFormat short[] streamType, String[] ISO639LanguageCode,
             * int eventID, String pathSegments, int[] index, int[] PID, int[]
             * componentTags
             */

            short[] streamTypes = locator.getStreamTypes(); // zero length if
                                                            // omitted
            String[] langCodes = locator.getLanguageCodes(); // zero length if
                                                             // omitted
            int eventId = locator.getEventId(); // -1 if omitted
            String pathSeg = locator.getPathSegments(); // null if omitted
                                                        // (constructors accept
                                                        // null for this value)
            int[] indexes = locator.getIndexes(); // zero length if omitted
            int[] pids = locator.getPIDs(); // zero lenght if omitted
            int[] compTags = locator.getComponentTags(); // zero length if
                                                         // omitted

            Vector freqBasedLocators = new Vector();

            for (int i = 0; i < details.length; i++)
            {
                try
                {
                    // cast ServiceDetails to ServiceDetailsExt, from which you
                    // can get the program number.
                    ServiceDetailsExt detail = (ServiceDetailsExt) details[i];

                    // From the ServiceDetails, get the TransportStream object.
                    // Cast that to TransportStreamExt, from which you can get
                    // the frequency and modulation.
                    TransportStreamExt ts = (TransportStreamExt) detail.getTransportStream();
                    if (ts == null) continue;

                    int freq = ts.getFrequency();
                    int progNum = detail.getProgramNumber();
                    int modu = ts.getModulationFormat();

                    OcapLocator freqBasedLocator;

                    if (langCodes != null && langCodes.length > 0)
                    {
                        // only constructor that accepts langCodes
                        freqBasedLocator = new OcapLocator(freq, progNum, modu, streamTypes, langCodes, eventId,
                                pathSeg);

                    }
                    else if (indexes != null && indexes.length > 0)
                    {
                        // only constructor that accepts indexes
                        freqBasedLocator = new OcapLocator(freq, progNum, modu, streamTypes, indexes, eventId, pathSeg);

                    }
                    else if (pids != null && pids.length > 0)
                    {
                        // only constructor that accepts pids
                        freqBasedLocator = new OcapLocator(freq, progNum, modu, pids, eventId, pathSeg);
                    }
                    else if (compTags != null && compTags.length > 0)
                    {
                        // only constructor that accepts compTags
                        freqBasedLocator = new OcapLocator(freq, progNum, modu, eventId, compTags, pathSeg);

                    }
                    else
                    // covers eventId, streamTypes, and pathSegs.
                    {
                        freqBasedLocator = new OcapLocator(freq, progNum, modu, streamTypes, langCodes, eventId,
                                pathSeg);

                    }

                    freqBasedLocators.add(freqBasedLocator);
                }
                catch (Exception e)
                {
                    // catch everything including casting exceptions and just
                    // move on (nothing to see here ;-))
                    SystemEventUtil.logEvent("Exception thrown, msg is: " + e.getMessage());
                    continue;
                }
            }

            if (freqBasedLocators.size() > 0)
            {
                return (Locator[]) freqBasedLocators.toArray(new Locator[freqBasedLocators.size()]);
            }

        }
        return getDefaultTransform(source);
    }

    private Locator[] getDefaultTransform(Locator source)
    {
        Locator locators[] = new Locator[1];
        locators[0] = source;
        return locators;
    }

}
