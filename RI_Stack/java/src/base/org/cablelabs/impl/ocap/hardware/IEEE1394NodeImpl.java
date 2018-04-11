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

package org.cablelabs.impl.ocap.hardware;

import java.lang.IllegalArgumentException;

import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hardware.IEEE1394Node;
import org.ocap.system.MonitorAppPermission;

import org.cablelabs.impl.ocap.si.ByteArrayWrapper;
import org.cablelabs.impl.ocap.si.ByteParser;

/**
 * Implements <code>IEEE1394Node</code> using native implementation.
 * 
 * @author Bin Lu
 */
public class IEEE1394NodeImpl implements IEEE1394Node
{

    private IEEE1394NodeImpl(byte[] euid, String modelName, String vendorName, short[] subunitType)
    {
        synchronized (this)
        {
            this.eui64 = euid;
            this.model_name = modelName.trim();
            this.vendor_name = vendorName.trim();
            this.subunit_type = subunitType;
        }
    }

    // This will be a problem when the plugin/unplug happens during the queries
    public static synchronized IEEE1394Node[] getIEEE1394NodeList(int handle)
    {
        byte[] data = nGetIEEE1394NodeList(handle);

        if (data == null)
        {
            return null;
        }

        ByteArrayWrapper wrapper = new ByteArrayWrapper(data, 0);

        int numNode = ByteParser.getInt(wrapper);

        byte[] eui;
        String vendorName;
        String modelName;
        short[] subunitType = new short[1];
        IEEE1394Node[] nodes;

        nodes = new IEEE1394Node[numNode];

        for (int i = 0; i < numNode; i++)
        {
            eui = ByteParser.getByteArray(wrapper, 8);
            vendorName = ByteParser.getString(wrapper, 128);
            modelName = ByteParser.getString(wrapper, 128);
            subunitType[0] = (short) ByteParser.getInt(wrapper);

            nodes[i] = new IEEE1394NodeImpl(eui, modelName, vendorName, subunitType);
        }

        return nodes;
    }

    public static synchronized void selectIEEE1394Sink(int handle, byte[] eui64, short subunitType)
            throws IllegalArgumentException
    {

        if (eui64 == null || eui64.length != 8)
        {
            throw new IllegalArgumentException("The eui64 is not valid!");
        }

        IEEE1394Node[] nodes = IEEE1394NodeImpl.getIEEE1394NodeList(handle);

        if (nodes == null || nodes.length == 0)
        {
            throw new IllegalArgumentException("No IEEE1394 nodes configured/available.");
        }

        boolean foundNode = false;
        for (int i = 0; i < nodes.length; i++)
        {
            boolean isThisNode = true;
            for (int j = 0; j < eui64.length; j++)
            {
                if (eui64[j] != nodes[i].getEUI64()[j])
                {
                    isThisNode = false;
                    break;
                }
            }

            if (isThisNode)
            {
                if (nSelectIEEE1394Sink(handle, eui64, subunitType) != 0)
                {
                    throw new IllegalArgumentException("Invalid eui64/subunit_type parameters!!");
                }
                foundNode = true;
                break;
            }
        }

        if (!foundNode)
        {
            throw new IllegalArgumentException("Invalid node!!");
        }

    }

    /**
     * 
     * Returns the value of EUI-64 of the 1394 node. EUI-64 is defined in IEEE
     * Std 1394-1995.
     * 
     * 
     * @return an unsigned big endian 64-bits value of EUI-64 of the 1394 node.
     * 
     * @throws SecurityException
     *             if the caller has not been granted
     *             MonitorAppPermission("setVideoPort").
     * 
     */
    public byte[] getEUI64() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("setVideoPort"));

        return eui64;
    }

    /**
     * 
     * Returns the value of MODEL NAME TEXTUAL DESCRIPTOR of the 1394 node.
     * MODEL NAME TEXTUAL DESCRIPTOR is defined in EIA-775-A.
     * 
     * @return the value of MODEL NAME TEXTUAL DESCRIPTOR of the 1394 node. If
     *         the 1394 node does not have the MODEL NAME TEXTUAL DESCRIPTOR,
     *         null is returned.
     * 
     * @throws SecurityException
     *             if the caller has not been granted
     *             MonitorAppPermission("setVideoPort").
     * 
     */
    public java.lang.String getModelName() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("setVideoPort"));

        return model_name;
    }

    /**
     * 
     * Returns the value of VENDOR NAME TEXTUAL DESCRIPTOR of the 1394 node.
     * VENDOR NAME TEXTUAL DESCRIPTOR is defined in EIA-775-A.
     * 
     * @return the value of VENDOR NAME TEXTUAL DESCRIPTOR of the 1394 node If
     *         the 1394 node does not have the VENDOR NAME TEXTUAL DESCRIPTOR,
     *         null is returned.
     * 
     * @throws SecurityException
     *             if the caller has not been granted
     *             MonitorAppPermission("setVideoPort").
     * 
     */
    public java.lang.String getVendorName() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("setVideoPort"));

        return vendor_name;
    }

    /**
     * 
     * Returns the list of subunitTypes supported by the 1394 node.
     * 
     * @return the list of subunitTypes supported by the 1394 node. The subunit
     *         type is defined in EIA-775-A.
     * 
     * @throws SecurityException
     *             if the caller has not been granted
     *             MonitorAppPermission("setVideoPort")
     * 
     */
    public short[] getSubunitType() throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("setVideoPort"));

        return subunit_type;
    }

    private byte[] eui64;

    private String model_name;

    private String vendor_name;

    private short[] subunit_type;

    private static native byte[] nGetIEEE1394NodeList(int handle);

    private static native int nSelectIEEE1394Sink(int handle, byte[] eui64, short subunitType);

}
