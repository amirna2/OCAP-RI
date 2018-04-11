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

package org.cablelabs.impl.persistent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import org.cablelabs.impl.util.MPEEnv;

/**
 * Tests PersistentData.
 * 
 * @author Aaron Kamienski
 */
public abstract class PersistentDataAbstractTest extends TestCase
{

    public void testConstructor()
    {
        PersistentData data = new PersistentData(27)
        {
        };
        assertEquals("Unexpected uniqueId", 27, data.uniqueId);
    }

    /**
     * @todo reenable once 5553 is fixed
     */
    public void xxxtestSerialization() throws Exception
    {
        try
        {
            PersistentData data[] = new PersistentData[10];
            // Serialize...
            for (int i = 0; i < data.length; ++i)
            {
                data[i] = createInstance(i);

                File f = new File(baseDir, "test-" + i);
                FileOutputStream fos = new FileOutputStream(f);
                try
                {
                    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(fos));
                    out.writeObject(data[i]);
                    out.flush();
                }
                finally
                {
                    fos.close();
                }
            }

            // De-serialize
            for (int i = 0; i < 10; ++i)
            {
                PersistentData data1 = null;

                File f = new File(baseDir, "test-" + i);
                FileInputStream fis = new FileInputStream(f);
                try
                {
                    ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(fis));

                    data1 = (PersistentData) in.readObject();
                }
                finally
                {
                    fis.close();
                }

                assertTrue("De-serialized object isn't same as original", isEquals(data[i], data1));

                assertTrue("Problems deleting " + f, f.delete());
            }
        }
        finally
        {
            for (int i = 0; i < 10; ++i)
            {
                File f = new File(baseDir, "test-" + i);
                if (f.exists()) f.delete();
            }
        }
    }

    /**
     * Should be overridden by subclass tests.
     */
    protected abstract PersistentData createInstance(int id) throws Exception;

    /*
     * { return new PersistentData(id) {}; }
     */

    /**
     * Should be overridden by subclass tests.
     */
    protected abstract boolean isEquals(PersistentData d0, PersistentData d1);

    /*
     * { return d0.uniqueId == d1.uniqueId; }
     */

    /* ====================== Boilerplate ====================== */

    protected File baseDir;

    public void setUp() throws Exception
    {
        String path = MPEEnv.getSystemProperty("PersistentDataTest.dir");
        if (path == null)
        {
            path = MPEEnv.getSystemProperty("dvb.persistent.root");
            if (path == null)
            {
                path = "/snfs";
            }
        }

        File dir = new File(path);
        if (!dir.exists()) dir = new File("/syscwd");
        if (!dir.exists()) dir = new File(".");

        baseDir = dir;
    }

    public PersistentDataAbstractTest(String name)
    {
        super(name);
    }
}
