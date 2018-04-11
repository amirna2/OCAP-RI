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

package org.cablelabs.xlet.MemoryLimitsTest;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

/*
 * Test to verify RAM memory limits per ECN OCAP1.0.2-O-07.1163-4.
 * For the OCAP RI stack, the symbol VMOPT.4 in the mpeenv.ini file
 * should be set to VMOPT.4=-Xmx64m in order to pass this test.
 */
public class MemoryLimitsTestXlet implements Xlet
{
    XletContext m_ctx;

    boolean m_started = false;

    Runtime rt;

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("MemoryLimitsTextXlet in initXlet");
        m_ctx = ctx; // save context
        rt = Runtime.getRuntime();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        System.out.println("MemoryLimitsTextXlet in startXlet");
        if (m_started) return;
        m_started = true;

        /*
         * This subsection contains requirements that are extensions to [DVB-GEM
         * 1.0.2] Section: G.6. OCAP implementations SHALL provide at least 64MB
         * of memory to support OCAP-J applications. At least this amount of
         * memory SHALL be available across all applications, regardless of
         * number. The following minimum memory requirements are defined for
         * OCAP terminals. All are to be measured in the startXlet() method of
         * one or more OCAP-J test applications. The implementation SHALL:
         * Successfully load any arbitrary 1MB (1,048,576 bytes) of Java class
         * files into the memory space of the Java virtual machine. Execution of
         * code called as part of initializing fields in classes is excluded
         * from consideration as part of "load"ing here. RAM usage by the
         * bytecode verifier is included in consideration as part of "load"ing
         * here. The classes comprising the OCAP-J test application(s) are
         * included within this 1MB of Java class files, and the test
         * application(s) will cause additional classes to be loaded sufficient
         * to meet the 1MB requirement if necessary.
         * 
         * The implementation SHALL supply enough memory to do the above and
         * individually each of the following. These individual tests are not
         * required to run concurrently, and it is expected that the memory for
         * each test is recovered before the next test is run.
         * 
         * 1. Successfully create up to 10 Java byte arrays of arbitrary length
         * with a combined total of up to 60,817,408 (58M) entries. 2.
         * Successfully create 46 instances of org.dvb.ui.DvbBufferedImage of
         * type DvbBufferedImage.TYPE_ADVANCED, and 640x480 pixels in size. 3.
         * Successfully load from one or more files into memory 3456 seconds of
         * audio encoded at 128 kbit/s (where kbit/s is as used in [ETSI TR 101
         * 154]). It SHALL be measured using files that do not include any
         * optional extension fields. 4. Successfully allocate up to 10 arrays
         * of java.lang.Object of arbitrary length with a combined total of up
         * to 3,538,944 entries, and fill each array element with a distinct
         * instance of java.lang.Object.
         * 
         * The memory requirements detailed in this section are not exhaustive.
         * For example, the specific requirement concerning an array of type
         * byte in no way implies that OCAP terminals are exempt from
         * requirements found elsewhere in the OCAP specification (including
         * normatively referenced specifications) for supporting arrays of other
         * types.
         */

        boolean pass = do_byte_array_test();
        rt.gc(); // heap cleanup
        if (!pass) return;

        System.out.println("MemoryLimitsTextXlet done");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
    }

    // Create up to 10 Java byte arrays of arbitrary length
    // with a combined total of up to 60,817,408 (58M) entries.
    // Return true if test passes else false.
    private boolean do_byte_array_test()
    {
        byte barray[][];
        barray = new byte[10][];
        try
        {
            for (int x = 0; x < barray.length; x++)
                barray[x] = new byte[60817408 / 10];
        }
        catch (OutOfMemoryError e)
        {
            System.out.println("MemoryLimitsTestXlet OutOfMemoryError Error array test");
            return false;
        }

        catch (Exception e)
        {
            System.out.println("MemoryLimitsTestXlet Exception Error array test");
            return false;
        }
        return true;
    }

}
