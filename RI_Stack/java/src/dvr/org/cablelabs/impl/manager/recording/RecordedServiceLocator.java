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

import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

/**
 * Recorded service locator
 */
public class RecordedServiceLocator extends OcapLocator
{
    // TODO(Todd): This method has to return an OcapLocator because that is
    // what the MediaAccessHandler requires. However, the external form is
    // not a broadcast locator. Therefore, we extend OcapLocator here and
    // override most methods to do the best we can within the bounds of the
    // current OCAP specification. Pat will follow up with CableLabs to
    // resolve how this should really be done.

    // RecordedServiceLocator URL syntax: 
    //
    //  "recording://id=<recording_request_id>[".index=<segment_index>][+0x<hex_pid>]"
    //
    // segment_index only applies to non-segment RecordedServices. 
    // hex_pid only applies to RecordedService ServiceComponents]
    
    // NOTE: In the current state of affairs, non-broadcast (opaque) Locators should not 
    //       start with "ocap:" and should not return valid values from any of the field accessors
    //       defined on OCAPLocator. i.e. getServiceName() should only return a name if the
    //       Locator was constructed with the "n=" (named_service) or the field was set via
    //       OcapLocator.setServiceName(). e.g. getPIDs() should only return values if the
    //       OCAPLocator was constructing using the "+"/"&" qualifiers (PID_elements) or the
    //       field was set via OcapLocator.setPIDs().
    
    final String recServiceURLPrefix = "recording://id=";
    final String indexPrefix = "index=";
    final String pidPrefix = "0x";
    int recordingID = -1;
    int segmentIndex = -1;
    int pid = -1;
    String serviceName = null;

    /** Constructor */
    public RecordedServiceLocator(String url) throws InvalidLocatorException
    {
        // Allow the superclass to be constructed
        super("ocap://0x0");

        if (!url.startsWith(recServiceURLPrefix))
        {
            throw new InvalidLocatorException( "Locator " + url + " does not start with the "
                                               + "RecordedServiceLocator prefix (" 
                                               + recServiceURLPrefix + ')' );
        }
        // This is the real url
        this.url = url;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int getSourceID()
    {
        return -1;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public java.lang.String getServiceName()
    {
        return null;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int getFrequency()
    {
        return -1;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int getModulationFormat()
    {
        return -1;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int getProgramNumber()
    {
        return -1;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public short[] getStreamTypes()
    {
        return new short[0];
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int[] getIndexes()
    {
        return new int[0];
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int getEventId()
    {
        return -1;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public java.lang.String[] getComponentNames()
    {
        return new String[0];
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int[] getComponentTags()
    {
        return new int[0];
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public java.lang.String getPathSegments()
    {
        return null;
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public String[] getLanguageCodes()
    {
        return new String[0];
    }

    /**
     * NOT SUPPORTED ON RecordedServiceLocator
     */
    public int[] getPIDs()
    {
        return new int[0];
    }

    /**
     * Return the recording ID this RecordedServiceLocator refers to. Every RecordedServiceLocator
     * must have a recording ID reference.
     * 
     * @return recording ID
     */
    public int getRecordingID()
    {
        if (recordingID == -1)
        {
            // Pull the ID out of the Locator
            final int startOfID = recServiceURLPrefix.length();
            int firstDelim = url.indexOf('.');
            if (firstDelim < 0)
            {
                firstDelim = url.indexOf('+');
            }
            final int endOfID = (firstDelim < 0) ? url.length() : firstDelim;
            final String idString = url.substring(startOfID, endOfID);
            try
            {
                recordingID = Integer.parseInt(idString);
            }
            catch (Exception e)
            { // Leave it at -1
            }
        }
        return recordingID;
    }
    
    /**
     * Return the segment index this RecordedServiceLocator refers to 
     * 
     * @return the segment index (0..n) or -1 if the Locator doesn't contain a segment index
     */
    public int getSegmentIndex()
    {
        if (segmentIndex == -1)
        {
            // Pull the segment index out of the Locator
            final int firstDot = url.indexOf('.');
            final int startOfIndex = url.indexOf(indexPrefix, firstDot) + indexPrefix.length();
            if (startOfIndex > 0)
            {
                final int firstPlus = url.indexOf('+');
                final int endOfIndex = (firstPlus < 0) ? url.length() : firstPlus;
                final String indexString = url.substring(startOfIndex, endOfIndex);
                try
                {
                    segmentIndex = Integer.parseInt(indexString);
                }
                catch (Exception e)
                { // Leave it at -1
                }
            }
        }
        return segmentIndex;
    }
        
    /**
     * Return the PID this RecordedServiceLocator refers to 
     * 
     * @return the PID (0..2^15) or -1 if the Locator doesn't contain a PID reference
     */
    public int getPID()
    {
        if (pid == -1)
        {
            // Pull the pid out of the Locator
            final int firstPlus = url.indexOf('+');
            final int startOfPID = url.indexOf(pidPrefix, firstPlus) + pidPrefix.length();
            if (startOfPID > 0)
            {
                final int endOfPID = url.length();
                final String pidString = url.substring(startOfPID, endOfPID);
                try
                {
                    pid = Integer.parseInt(pidString, 16); // Value is hex
                }
                catch (Exception e)
                { // Leave it at -1
                }
            }
        }
        return pid;
    }

    // Description copied from org.davic.net.Locator
    public String toString()
    {
        return url;
    }

    // Description copied from org.davic.net.Locator
    public boolean hasMultipleTransformations()
    {
        // TODO(Todd): ECR 879 requires support for multiple ServiceDetails
        // for a single RecordedService. Does that mean we should return
        // true here? If yes, it probably makes sense to do so only when
        // there are multiple ServicesDetails available (e.g. only when one
        // or more presentations of this service is ongoing.
        return false;
    }

    // Description copied from org.davic.net.Locator
    public String toExternalForm()
    {
        return url;
    }

    // Description copied from org.davic.net.Locator
    public boolean equals(java.lang.Object obj)
    {
        // Make sure we have a good object
        if (obj == null || !(obj instanceof RecordedServiceLocator))
        {
            return false;
        }

        // Compare objects
        RecordedServiceLocator loc = (RecordedServiceLocator) obj;
        return (toExternalForm().equals(loc.toExternalForm()));
    }

    // Description copied from org.davic.net.Locator
    public int hashCode()
    {
        return toExternalForm().hashCode();
    }

    // The external form of this locator
    private final String url;
}

