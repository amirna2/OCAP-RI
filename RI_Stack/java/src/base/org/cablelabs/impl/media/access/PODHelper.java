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

package org.cablelabs.impl.media.access;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.tv.service.SIException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.ocap.hardware.pod.POD;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PODManager;
import org.cablelabs.impl.service.SIManagerExt;

/**
 * This class provides helper methods for the
 * <code>org.cablelabs.impl.media.access</code> package for obtaining
 * information from the <code>POD</code>.
 * 
 * @author Jason Subbert
 * 
 */
public class PODHelper
{
    /**
     * Private constructor so that it cannot be instantiated.
     */
    private PODHelper()
    {
    }

    /**
     * Returns an array of <code>Service</code> objects that should be placed
     * under parental control.
     * 
     * @return the array of <code>Service</code> objects; this array will be
     *         empty if no services are blocked.
     */
    public static Service[] getBlockedServices()
    {
        // array of service objects returned from this method
        Service serviceArray[] = new Service[0];
        // number of channels under parental control
        int count;
        PODManager podman = (PODManager) ManagerManager.getInstance(PODManager.class);
        POD pod = podman.getPOD();

        if (pod.isReady())
        {
            byte array[] = pod.getHostParam(PARENTAL_CONTROL);
            ByteArrayInputStream bytes = new ByteArrayInputStream(array);

            // skip the first byte (factory reset)
            bytes.skip(1);
            if (bytes.available() > 0)
            {
                // get the number of channels
                count = ((bytes.read() & 0xff) << 8) | (bytes.read() & 0xff);
                if (count > 0)
                {
                    int byte1, byte2;
                    short major_channel, minor_channel;
                    List list = new ArrayList();
                    SIManagerExt mgr = (SIManagerExt) SIManager.createInstance();
                    Service service;
                    for (int i = 0; i < count; i++)
                    {
                        byte1 = bytes.read();
                        byte2 = bytes.read();
                        major_channel = (short) (((byte1 & 0x0f) << 6) | ((byte2 & 0xfc) >> 2));
                        minor_channel = (short) (((byte2 & 0x02) << 8) | (bytes.read() & 0xff));

                        try
                        {
                            service = mgr.getService(major_channel, minor_channel);
                            list.add(service);
                        }
                        catch (SIException e)
                        {
                            // cannot retrieve the service so it is not added to
                            // the list
                        }
                    }
                    serviceArray = (Service[]) list.toArray(new Service[0]);
                }
            }
        }
        return serviceArray;
    }

    /**
     * Returns the SCTE-65 defined rating region in which the host is located.
     * 
     * @return integer value representing the rating region, -1 if
     *         <code>POD.isReady()</code> returns <code>false</code>.
     */
    public static int getRatingRegion()
    {
        int returnValue = -1;
        PODManager podman = (PODManager) ManagerManager.getInstance(PODManager.class);
        POD pod = podman.getPOD();
        if (pod.isReady())
        {
            byte array[] = pod.getHostParam(RATING_REGION);
            returnValue = array[0] & 0xff;
        }
        return returnValue;
    }

    public final static int PARENTAL_CONTROL = 0x03;

    public final static int RATING_REGION = 0x09;
}
