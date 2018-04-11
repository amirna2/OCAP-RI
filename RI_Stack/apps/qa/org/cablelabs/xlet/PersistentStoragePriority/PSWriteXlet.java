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

// Declare package.
package org.cablelabs.xlet.PersistentStoragePriority;

// Import Personal Java packages.
import javax.tv.xlet.*;

import org.dvb.application.AppID;
import org.dvb.io.persistent.FileAttributes;

import org.cablelabs.lib.utils.ArgParser;

import java.io.*;

/**
 * The class deadxlet4 performs a complex test on the persistent storage
 * mechanism by creating files from various xlets and exiting some of the xlets
 * while keeping others running. A final xlet runs and uses up persistent
 * storage space until files get purged and the space gets recycled. The
 * objective is to identify the files that would get purged and watch as they
 * get purged one by one.
 * 
 * Arguments: config_file=config.properties Config File (In case needed)
 * 
 * @version 25 July 2006
 * @author Vidiom Systems Corp.
 */
public class PSWriteXlet implements Xlet
{
    XletContext m_ctx = null; // This Xlet's context

    String rootname = null;

    int numbytes = 0;

    static final String CONFIG_FILE = "config_file";

    static final String LOW_PRIORITY = "LOW";

    static final String MED_PRIORITY = "MEDIUM";

    static final String HIGH_PRIORITY = "HIGH";

    static final String NO_PRIORITY = "NONE";

    static final String DATA_SIZE = "Kbytes";

    static final String APP_NAME = "AppName";

    static final int MAX_FILES = 3;

    String appName;

    /**
     * Initializes the OCAP Xlet. Note that even though we could theoretically
     * do a lot here, we don't. We wait until startXlet before running the test.
     * All we do here is mount the file system and get test arguments set up. We
     * also set up AutoClient stuff.
     * 
     * @param XletContext
     *            The context for this Xlet is passed in.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialised.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_ctx = ctx;
        AppID id = getID();
        String aid = Integer.toHexString(id.getAID());

        try
        {
            // Get path name of config file.
            ArgParser xlet_args = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            String str_config_file_name = xlet_args.getStringArg(CONFIG_FILE);
            FileInputStream fis_read = new FileInputStream(str_config_file_name);
            ArgParser config_args = new ArgParser(fis_read);

            // Retrieve data about the root location
            rootname = System.getProperty("dvb.persistent.root");

            // Parse out number of bytes to be written for each file
            numbytes = config_args.getIntArg(DATA_SIZE);

            // Parse out name
            appName = config_args.getStringArg(APP_NAME + aid);
            System.out.println("Inside" + appName + " initXlet");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet() throws XletStateChangeException
    {
        System.out.println(appName + " running");
        AppID id = getID();
        String oid = Integer.toHexString(id.getOID());
        String aid = Integer.toHexString(id.getAID());
        String fname = null;
        // Fill local 1K buffer
        byte[] m_buf = new byte[1024];
        for (int i = 0; i < 1024; ++i)
        {
            m_buf[i] = (byte) 0xd1;
        }

        try
        {
            for (int j = 0; j < MAX_FILES; j++)
            {
                String priority = null;
                switch (j)
                {
                    case 0:
                        priority = LOW_PRIORITY;
                        break;
                    case 1:
                        priority = MED_PRIORITY;
                        break;
                    case 2:
                        priority = HIGH_PRIORITY;
                        break;
                }
                // First, create the full path name
                fname = rootname + "/" + oid + "/" + aid + "/" + appName + "_" + priority + ".dat";
                File fdes = new File(fname); // Create file
                FileOutputStream fos = new FileOutputStream(fdes); // Create
                                                                   // file
                                                                   // output
                                                                   // stream
                for (int i = 0; i < numbytes; ++i) // Write the values
                {
                    fos.write(m_buf, 0, 1024); // 1K at a time
                }
                fos.close(); // Close file
                System.out.println(fname + " written with " + numbytes + " Kbytes");
                FileAttributes fat = FileAttributes.getFileAttributes(fdes); // Get
                                                                             // Attributes
                switch (j)
                {
                    case 0: // File Attributes
                        fat.setPriority(FileAttributes.PRIORITY_LOW); // Set
                                                                      // priority
                        break;
                    case 1: // File Attributes
                        fat.setPriority(FileAttributes.PRIORITY_MEDIUM); // Set
                                                                         // priority
                        break;
                    case 2: // File Attributes
                        fat.setPriority(FileAttributes.PRIORITY_HIGH); // Set
                                                                       // priority
                        break;
                }
                FileAttributes.setFileAttributes(fat, fdes); // Set attributes
                System.out.println("Setting file permissions to " + priority);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new XletStateChangeException("Cannot create file " + fname);
        }
    }

    /*
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        System.out.println("pauseXlet: " + appName);
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     */
    public void destroyXlet(boolean x) throws XletStateChangeException
    {
        System.out.println("destroyXlet: " + appName);
    }

    /*
     * Returns current ID of this xlet
     */
    public AppID getID()
    {
        int aid, oid;
        String str;

        str = (String) m_ctx.getXletProperty("dvb.org.id");
        oid = (int) Long.parseLong(str, 16);
        str = (String) m_ctx.getXletProperty("dvb.app.id");
        aid = Integer.parseInt(str, 16);

        return new AppID(oid, aid);
    } // getID() function

}
