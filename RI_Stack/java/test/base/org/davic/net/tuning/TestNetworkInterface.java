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

package org.davic.net.tuning;

import org.davic.mpeg.TransportStream;
import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceImpl;
import org.cablelabs.impl.manager.ed.EDListener;

public class TestNetworkInterface extends NetworkInterfaceImpl
{
    public TestNetworkInterface(int i)
    {
        super(i);
    }

    protected boolean nativeTune(int tunerId, final EDListener listener, final int frequency, final int programNum,
            final int qam)
    {
        synchronized (lock)
        {
            // Send a failed event if the parameters match that of
            // failLocator. Otherwise, send a tune complete event.
            if (failLocator.getFrequency() == frequency && failLocator.getModulationFormat() == qam)
            {
                tunedTransportStream = null;
                // TODO(Todd): Should use EventCallback.TUNE_FAIL but cannot due
                // to obfuscation
                listener.asyncEvent(0x3, 0, 0);
            }
            else
            {
                try
                {
                    tunedTransportStream = StreamTable.getTransportStreams(successLocator)[0];
                }
                catch (Exception exc)
                {

                }
                // TODO(Todd): Should use EventCallback.TUNE_SYNCED but cannot
                // due to obfuscation
                listener.asyncEvent(0x2, 0, 0);
            }
        }
        return true;
    }

    public TransportStream getCurrentTransportStream()
    {
        synchronized (lock)
        {
            return tunedTransportStream;
        }
    }

    public static OcapLocator successLocator = null;

    public static OcapLocator canonicalSuccessLocator = null;

    public static OcapLocator failLocator = null;

    private TransportStream tunedTransportStream = null;

    private Object lock = new Object();

    static
    {
        try
        {
            //
            // frequency matches CannedSIDatabase.transportStream8
            //
            successLocator = new OcapLocator(5250, 0x2);
            //
            // frequency matches CannedSIDatabase.transportStream7
            //
            canonicalSuccessLocator = new OcapLocator(5000, 0x1);
            failLocator = new OcapLocator(5500, 0x3);
        }
        catch (InvalidLocatorException e)
        {
        }
    }
}
