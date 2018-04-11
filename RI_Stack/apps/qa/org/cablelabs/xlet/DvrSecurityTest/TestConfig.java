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

/*
 * Created on August 3, 2005
 * TODO - provide additional methods for reading confi file lines
 */
package org.cablelabs.xlet.DvrSecurityTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Calendar;

import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;

public class TestConfig
{
    private static final String BANNER = "+---------------------------------------------------------+";

    private static final String WHOAMI = "TestConfig ";

    private static final String NULL_FAP = "null";

    private static final int PERMS_LEN = 6; // "rwrwrw"

    private static final int NUM_EFAP_TOKENS = 3;

    private static final int NUM_TIME_TOKENS = 6;

    // yyyy:MM:dd:HH:mm:ss
    // E.g.: "2005:08:03:11:45:00" (August 3, 2005; 11:45:00)
    static public Date getTime(String text)
    {
        Date theTime = null;
        StringTokenizer st = new StringTokenizer(text, ":");
        if (NUM_TIME_TOKENS == st.countTokens())
        {
            int year = Integer.parseInt(st.nextToken());
            int month = Integer.parseInt(st.nextToken()) - 1; // zero-ordinal
            int date = Integer.parseInt(st.nextToken());
            int hour = Integer.parseInt(st.nextToken());
            int min = Integer.parseInt(st.nextToken());
            int sec = Integer.parseInt(st.nextToken());
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, date, hour, min, sec);
            theTime = cal.getTime();
        }
        else
        {
            System.err.println("Badly-formed input time-token: \"" + text + "\"");
        }
        return (theTime);
    }

    // rwrwrw:readOrgIdList:writeOrgIdList
    // "rwrwrw:arbitraryIdentifier:readOrgIdList:writeOrgIdList"
    static public ExtendedFileAccessPermissions getEFAP(String text)
    {
        ExtendedFileAccessPermissions fap = null;
        StringTokenizer st = new StringTokenizer(text, ":");
        if (NUM_EFAP_TOKENS == st.countTokens())
        {

            // "rwrwrw"
            String perms = st.nextToken();

            if (perms.equals(NULL_FAP))
            {
                System.out.println(WHOAMI + "INTENTIONAL NULL FAP being returned");
                return null;
            }

            if (PERMS_LEN != perms.length())
            {
                System.err.println(WHOAMI + "Badly-formed input EFAP-token: \"" + text + "\"");
                return null;
            }

            boolean worldRead = false;
            if ('r' == perms.charAt(0))
            {
                worldRead = true;
            }
            boolean worldWrite = false;
            if ('w' == perms.charAt(1))
            {
                worldWrite = true;
            }
            boolean orgRead = false;
            if ('r' == perms.charAt(2))
            {
                orgRead = true;
            }
            boolean orgWrite = false;
            if ('w' == perms.charAt(3))
            {
                orgWrite = true;
            }
            boolean ownRead = false;
            if ('r' == perms.charAt(4))
            {
                ownRead = true;
            }
            boolean ownWrite = false;
            if ('w' == perms.charAt(5))
            {
                ownWrite = true;
            }

            // we are about to re-use the StringTokenizer ...
            String otherReadOrgIds_str = st.nextToken();
            String otherWriteOrgIds_str = st.nextToken();
            int[] otherReadOrgIds = null;
            int[] otherWriteOrgIds = null;

            st = new StringTokenizer(otherReadOrgIds_str, ",");
            int count = st.countTokens();
            if (0 < count)
            {
                int i = 0;
                String token = st.nextToken();
                if (!token.equals("null"))
                {
                    otherReadOrgIds = new int[count];
                    otherReadOrgIds[i++] = Integer.parseInt(token);
                    while (st.hasMoreElements())
                    {
                        otherReadOrgIds[i++] = Integer.parseInt(st.nextToken());
                    }
                }
            }

            st = new StringTokenizer(otherWriteOrgIds_str, ",");
            count = st.countTokens();
            if (0 < count)
            {
                int i = 0;
                String token = st.nextToken();
                if (!token.equals("null"))
                {
                    otherWriteOrgIds = new int[count];
                    otherWriteOrgIds[i++] = Integer.parseInt(token);
                    while (st.hasMoreElements())
                    {
                        otherWriteOrgIds[i++] = Integer.parseInt(st.nextToken());
                    }
                }
            }

            fap = new ExtendedFileAccessPermissions(worldRead, worldWrite, orgRead, orgWrite, ownRead, ownWrite,
                    otherReadOrgIds, otherWriteOrgIds);
        }
        else
        {
            System.err.println("Badly-formed input EFAP-token: \"" + text + "\"");
        }
        return (fap);
    }

    // behavior depends on attributes of calling application
    // (especially the PRF)
    static public void dumpOverlappingEntries()
    {
        OcapRecordingManager mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == mgr)
        {
            throw new NullPointerException("Failed to retrieve OcapRecordingManager Object ref");
        }

        RecordingList rlist = mgr.getEntries();
        int numEntries = rlist.size();
        System.out.println(BANNER);
        System.out.println(WHOAMI + "Examining " + numEntries + " Entries for overlap ...");
        for (int i = 0; i < numEntries; ++i)
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) (rlist.getRecordingRequest(i));
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) orr.getRecordingSpec();
            javax.tv.locator.Locator[] srcAry = lrs.getSource();
            Date sTime = lrs.getStartTime();
            long dMsec = lrs.getDuration();
            System.out.println(WHOAMI + "Entry: " + srcAry[0] + " " + sTime + " " + dMsec);

            RecordingList rlistOvlp = orr.getOverlappingEntries();
            int numOvlpEntries = rlistOvlp.size();
            System.out.println(WHOAMI + "Found " + numOvlpEntries + " Overlapping Entries ...");
            for (int j = 0; j < numOvlpEntries; ++j)
            {
                OcapRecordingRequest orrOvlp = (OcapRecordingRequest) (rlistOvlp.getRecordingRequest(j));
                LocatorRecordingSpec lrsOvlp = (LocatorRecordingSpec) orrOvlp.getRecordingSpec();
                javax.tv.locator.Locator[] srcAryOvlp = lrsOvlp.getSource();
                Date sTimeOvlp = lrsOvlp.getStartTime();
                long dMsecOvlp = lrsOvlp.getDuration();
                System.out.println(WHOAMI + "Overlapping Entry: " + srcAryOvlp[0] + " " + sTimeOvlp + " " + dMsecOvlp);
            }
        }
        System.out.println(BANNER);
    }

    static public OcapRecordingRequest findORR(String cfgFile, String reqId)
    {

        String cfgLine = getCfgLine(cfgFile, reqId);
        if (null == cfgLine)
        {
            System.err.println("Failed to retrieve " + cfgFile + " config line for reqID " + reqId);
            return null;
        }
        String srcId = getSrcIdFromCfgLine(cfgLine);
        Date startTime = getStartTimeFromCfgLine(cfgLine);
        long durMsec = getDurMsecFromCfgLine(cfgLine);

        OcapRecordingManager mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == mgr)
        {
            throw new NullPointerException("Failed to retrieve OcapRecordingManager Object ref");
        }
        RecordingList rlist = mgr.getEntries();
        int numEntries = rlist.size();
        System.out.println(WHOAMI + "getORR() numEntries: " + numEntries);
        if (0 == numEntries)
        {
            System.err.println(WHOAMI + "getORR() retrieved empty RecordingList ...");
            return null;
        }

        for (int i = 0; i < numEntries; ++i)
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) (rlist.getRecordingRequest(i));
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) orr.getRecordingSpec();
            javax.tv.locator.Locator[] srcAry = lrs.getSource();
            Date sTime = lrs.getStartTime();
            long dMsec = lrs.getDuration();

            // Peform the easy checks first ...
            if ((dMsec != durMsec) || (!(srcAry[0].toString().equals(srcId))))
            {
                continue;
            }

            // Date.equals() is sensitive to milliseconds (which is
            // "in the noise" for us) which is preventing us from
            // matching Dates.

            long time1 = startTime.getTime();
            long time2 = sTime.getTime();
            long delta = time1 - time2;
            if (0 > delta)
            {
                delta *= -1;
            }

            if (1000 > delta)
            {
                return (orr);
            }
        }
        return null;
    }

    /**
     * Using the specified recording request identifier, retrieve the
     * corresponding line from the specified "primary" ASCII configuration file.
     * This ASCII file defines the list of recording requests created by the
     * primary Xlet during initialization.
     * 
     * @param cfgFile
     *            primary ASCII configuration file filename
     * @param reqId
     *            arbitrary string recording request identifier
     */
    static public String getCfgLine(String cfgFile, String reqId)
    {
        BufferedReader rdr = null;
        try
        {
            String line = null;
            rdr = new BufferedReader(new FileReader(cfgFile));
            while (null != (line = rdr.readLine()))
            {
                // skip the leading '|'
                if (1 == line.indexOf(reqId, 1))
                {
                    rdr.close();
                    return line;
                }
            }
        }
        catch (Exception e)
        {
            throw new IllegalStateException(WHOAMI + "getCfgLine failed to retrieve config file line: "
                    + e.getMessage());
        }
        finally
        {
            if (null != rdr)
            {
                try
                {
                    rdr.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        return null;
    }

    // third token
    static public String getSrcIdFromCfgLine(String line)
    {
        StringTokenizer st = new StringTokenizer(line, "|");
        String ignore = st.nextToken();
        ignore = st.nextToken();
        return st.nextToken();
    }

    // fourth token
    static public Date getStartTimeFromCfgLine(String line)
    {
        Date startTime = null;
        StringTokenizer st = new StringTokenizer(line, "|");
        String ignore = st.nextToken();
        ignore = st.nextToken();
        ignore = st.nextToken();
        return getTime(st.nextToken());
    }

    // fifth token
    static public long getDurMsecFromCfgLine(String line)
    {
        long durMsec;
        StringTokenizer st = new StringTokenizer(line, "|");
        String ignore = st.nextToken();
        ignore = st.nextToken();
        ignore = st.nextToken();
        ignore = st.nextToken();
        return Long.parseLong(st.nextToken());
    }
}
