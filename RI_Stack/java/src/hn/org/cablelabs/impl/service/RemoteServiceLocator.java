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
package org.cablelabs.impl.service;

import javax.tv.locator.Locator;

import org.davic.net.InvalidLocatorException;
import org.ocap.hn.service.RemoteService;
import org.ocap.net.OcapLocator;

public class RemoteServiceLocator extends OcapLocator
{
    // TODO(Todd): This method has to return an OcapLocator because that is
    // what the MediaAccessHandler requires. However, the external form is
    // not a broadcast locator. Therefore, we extend OcapLocator here and
    // override most methods to do the best we can within the bounds of the
    // current OCAP specification. Pat will follow up with CableLabs to
    // resolve how this should really be done.
    //
    // RemoteServiceLocator URL syntax: 
    //
    //  "remoteservice://uuid=<device_uuid>.content_id=<content_id>+[pid]"
    //  "remoteservice://object_id=<objectId>+[pid]"
    //
    // NOTE: equals is implemented supporting comparison against
    // javax.tv.Locator
    private final String url;
    final String remoteServiceLocatorPrefix = "remoteservice://";
    final String remoteServiceUUIDPrefix = "remoteservice://uuid=";
    final String pidPrefix = "0x";
    final String contentIdPrefix = "content_id=";
    final String objectIdPrefix = "object_id=";
    String content_id = null;
    String device_id = null;
    int pid = -1;
    private RemoteService service = null;
    /**
     * Constructor
     */
    public RemoteServiceLocator(String url, RemoteService s) throws InvalidLocatorException
    {
        // Allow the superclass to be constructed
        super("ocap://0x0");

        if (!url.startsWith(remoteServiceLocatorPrefix))
        {
            throw new InvalidLocatorException( "Locator " + url + " does not start with the "
                                               + "RemoteServiceLocator prefix (" 
                                               + remoteServiceLocatorPrefix + ')' );
        }
        // This is the real url
        this.url = url;
        this.service = s;
    }
    
    public RemoteService getService()
    {
        return service;
    }
    
    public void setService(RemoteService s)
    {
        service = s;
    }
    
    // Description copied from OcapLocator

    public int getSourceID()
    {
        return -1;
    }

    // Description copied from OcapLocator

    public String getServiceName()
    {
        return null;
    }

    // Description copied from OcapLocator

    public int getFrequency()
    {
        return -1;
    }

    // Description copied from OcapLocator

    public int getModulationFormat()
    {
        return -1;
    }

    // Description copied from OcapLocator

    public int getProgramNumber()
    {
        return -1;
    }

    // Description copied from OcapLocator

    public short[] getStreamTypes()
    {
        return new short[0];
    }

    // Description copied from OcapLocator

    public int[] getIndexes()
    {
        return new int[0];
    }

    // Description copied from OcapLocator

    public int getEventId()
    {
        return -1;
    }

    // Description copied from OcapLocator

    public String[] getComponentNames()
    {
        return new String[0];
    }

    // Description copied from OcapLocator

    public int[] getComponentTags()
    {
        return new int[0];
    }

    // Description copied from OcapLocator

    public String getPathSegments()
    {
        return null;
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
    
    public String getDeviceId()
    {
        if (!url.startsWith(remoteServiceUUIDPrefix))
        {
            // This means its not the locator with uuid and content_id
            return null;
        }
        
        if (device_id == null)
        {            
            // Pull the device ID out of the Locator
            final int startOfID = remoteServiceUUIDPrefix.length();
            int firstDelim = url.indexOf('.');

            final int endOfID = (firstDelim < 0) ? url.length() : firstDelim;
            device_id = url.substring(startOfID, endOfID);           
        }
        return device_id;
    }
    
    public String getContentId()
    {
        if (!url.startsWith(remoteServiceUUIDPrefix))
        {
            // This means its not the locator with uuid and content_id
            return null;
        }
        
        if (content_id == null)
        {
            // Pull the ID out of the Locator
            final int startOfID = remoteServiceUUIDPrefix.length();
            int firstDelim = url.indexOf('.');
            if (firstDelim < 0)
            {
                firstDelim = url.indexOf('+');
            }
            final int endOfID = (firstDelim < 0) ? url.length() : firstDelim;
            content_id = url.substring(startOfID, endOfID);
        }
        return content_id;
    }
    
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

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        return super.equals(o);
    }

    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + toExternalForm().hashCode();
        return result;
    }
}
