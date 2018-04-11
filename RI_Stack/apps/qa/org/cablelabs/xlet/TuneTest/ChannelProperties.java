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
package org.cablelabs.xlet.TuneTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

import org.davic.net.InvalidLocatorException;
import org.ocap.net.OcapLocator;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.XletLogger;

public class ChannelProperties
{
    /**
     * Basic Channel contructor
     */
    public ChannelProperties(String propFile) throws FileNotFoundException, IOException
    {
        // Log to STDOUT
        _log = new XletLogger();

        _log.log("ChannelProperties: propFile: " + propFile);
        FileInputStream fis = new FileInputStream(propFile);
        _prop = new ArgParser(fis);
        fis.close();
    }

    public ChannelProperties(String propFile, Logger log) throws FileNotFoundException, IOException
    {
        _log = log;

        _log.log("ChannelProperties: propFile: " + propFile);
        FileInputStream fis = new FileInputStream(propFile);
        _prop = new ArgParser(fis);
        fis.close();
    }

    public Vector buildChannelMap()
    {
        addChannels("digital");
        addChannels("analog");
        addChannels("gen");
        return _channelVector;
    }

    public void addChannels(String type)
    {
        int i = 0;
        OcapLocator ocaploc = null;

        while (true)
        {
            if (type.equals("gen"))
                ocaploc = getChannelByFPQ(i, type);
            else
                ocaploc = getChannelBySourceId(i, type);

            if (ocaploc == null) return;

            _channelVector.addElement(ocaploc);
            i++;
        }
    }

    /*
     * Build a general type channel(tune by frequency,program number, qam) from
     * the properties.
     */
    public OcapLocator getChannelByFPQ(int index, String type)
    {
        int frequency = 0;
        int programNum = 0;
        int qam = 0;
        // String name = null;
        // String description = null;
        OcapLocator ocaploc = null;
        try
        {
            frequency = _prop.getIntArg(type + CHAN_FREQUENCY + index);
            programNum = _prop.getIntArg(type + CHAN_PROGRAM_NUMBER + index);
            qam = _prop.getIntArg(type + CHAN_QAM + index);
            // name = _prop.getStringArg(type + CHAN_NAME + index);
            // description = _prop.getStringArg(type + CHAN_DESC + index);
            _log.log(type + " Channel- freq: " + frequency + " qam :" + qam + " pid :" + programNum);
            ocaploc = new OcapLocator(frequency, programNum, qam);
        }
        catch (InvalidLocatorException e)
        {
            _log.log("Could not create locator: " + e);
            return null;
        }
        catch (Exception e)
        {
            _log.log("Done getting channels. Type = " + type);
            return null;
        }

        return ocaploc;
    }

    public OcapLocator getChannelBySourceId(int index, String type)
    {
        int sourceId = 0;
        // String name = null;
        // String description = null;
        OcapLocator ocaploc = null;
        try
        {
            sourceId = _prop.getInt16Arg(type + CHAN_SOURCE_ID + index);
            // name = _prop.getStringArg(type+CHAN_NAME + index);
            // description = _prop.getStringArg(type+CHAN_DESC + index);
            _log.log(type + " OcapLocator sourceId=" + sourceId);
            ocaploc = new OcapLocator(sourceId);
        }
        catch (InvalidLocatorException e)
        {
            _log.log("Could not create locator: " + e);
            return null;
        }
        catch (Exception e)
        {
            _log.log("Done getting channels type=" + type);
            return null;
        }

        return ocaploc;
    }

    private Vector _channelVector = new Vector();

    private ArgParser _prop = null;

    private static final String CHAN_FREQUENCY = "_channel_freq_";

    private static final String CHAN_PROGRAM_NUMBER = "_channel_program_number_";

    private static final String CHAN_QAM = "_channel_qam_";

    private static final String CHAN_NAME = "_channel_name_";

    private static final String CHAN_DESC = "_channel_description_";

    private static final String CHAN_SOURCE_ID = "_channel_sourceId_";

    private Logger _log = null;
}
