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
package org.cablelabs.xlet.reclaim;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.ui.DVBBufferedImage;

/**
 * RezMemUser
 * 
 * @author Joshua Keplinger
 * 
 */
public class RezMemUser implements Xlet
{

    private String role;

    private String memType;

    private int sysMemCount;

    private Object[] waste;

    private XletContext xctx;

    private static final double ONE_MEG = Math.pow(2, 20);

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        role = null;
        waste = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        xctx = ctx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        String[] args = (String[]) (xctx.getXletProperty("dvb.caller.parameters"));
        if (args == null || args.length < 3)
            throw new XletStateChangeException("Number of arguments is incorrect, requires 3");
        role = args[0];
        if (!("low".equals(role)) && !("high".equals(role)))
            throw new XletStateChangeException("Expected 'low' or 'high' as first argument");
        memType = args[1];
        if (!("sys".equals(memType)) && !("vm".equals(memType)))
            throw new XletStateChangeException("Expected 'sys' or 'vm' as second argument");
        sysMemCount = Integer.parseInt(args[2]);

        // The low priority role is to use up all but roughly 1MB of the heap or
        // system memory
        if ("low".equals(role))
        {
            if ("vm".equals(memType))
                lowPriorityVMMem();
            else
                lowPrioritySysMem();
        }
        // The high priority role is to attempt to get 2MB of heap or system
        // memory
        else
        {
            if ("vm".equals(memType))
                highPriorityVMMem();
            else
                highPrioritySysMem();
        }
    }

    private void lowPriorityVMMem()
    {
        long free = Runtime.getRuntime().freeMemory();
        // System.out.println("Free heap mem before allocating: " + free +
        // " KB");
        double count = (free / ONE_MEG) - 1; // Number of 1 MB chunks
        // System.out.println("Testing with " + (int)count +
        // " 1 MB chunks of memory");
        waste = new Object[(int) count + 1];
        for (int i = 0; i < count; i++)
        {
            // System.out.println("Creating 1MB array #" + i);
            waste[i] = new byte[new Double(ONE_MEG).intValue()];
        }
    }

    private void lowPrioritySysMem()
    {
        waste = new Object[sysMemCount];
        for (int i = 0; i < sysMemCount; i++)
        {
            // System.out.println("Creating 1MB image #" + i);
            waste[i] = new DVBBufferedImage(1, 262144); // 1MB image
        }
    }

    private void highPriorityVMMem()
    {
        waste = new Object[1];
        waste[0] = new byte[new Double(ONE_MEG * 2).intValue()];
    }

    private void highPrioritySysMem()
    {
        waste = new Object[1];
        waste[0] = new DVBBufferedImage(2, 262144); // 2MB image
    }

}
