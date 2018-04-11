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

package org.cablelabs.impl.manager.signalling;

import org.cablelabs.impl.signalling.*;
import java.util.*;
import org.cablelabs.impl.manager.signalling.AitParserTest.AITGenerator;
import org.cablelabs.impl.manager.signalling.XaitParserTest.XAITGenerator;

/**
 * This is a simple <i>command-line</i> program that allows one to generate an
 * XAIT.
 * 
 * @author Aaron Kamienski
 * @see XaitParserTest#XAITGenerator
 * @see XaitProps
 */
public class GenXait extends GenAit
{
    /**
     * Print usage() information.
     */
    protected void usage()
    {
        System.out.println("Usage: GenXait [options] <xait.properties>");
        System.out.println("  Will generate xait-i.bin files where i is the section number");
        System.out.println("Options:");
        System.out.println("  -o <dir>  : Generate output files to directory <dir>");
    }

    public static void main(String args[])
    {
        try
        {
            GenXait gen = new GenXait();
            gen.parseArgs(args);
            gen.go();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected GenXait()
    {
    }

    protected int parseArg(String args[], int i)
    {
        if ("-s".equals(args[i]))
        {
            usage();
            System.exit(0);
        }
        return i;
    }

    protected AitProps createAitProps()
    {
        return new XaitProps(Xait.NETWORK_SIGNALLING);
    }

    protected AITGenerator createAITGen()
    {
        return new XAITGenerator();
    }

    protected void fillGen(AITGenerator gen, AitProps props)
    {
        XAITGenerator xgen = (XAITGenerator) gen;
        XaitProps xprops = (XaitProps) props;

        // Add each service and it's apps to the gen
        AbstractServiceEntry[] svcs = ((Xait) xprops.getSignalling()).getServices();
        for (int i = 0; i < svcs.length; ++i)
        {
            xgen.add(svcs[i]);

            for (Enumeration e = svcs[i].apps.elements(); e.hasMoreElements();)
            {
                XAppEntry app = (XAppEntry) e.nextElement();
                AppEntry entry = app;

                xgen.add(entry);
            }
        }
    }

    protected String genName(int i)
    {
        return "xait-" + i + ".bin";
    }
}
