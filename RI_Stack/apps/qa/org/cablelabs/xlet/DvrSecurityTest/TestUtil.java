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
 * Created on August 5, 2005
 */
package org.cablelabs.xlet.DvrSecurityTest;

import java.util.Date;

import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;

public class TestUtil
{
    private static final String BANNER = "+---------------------------------------------------------+";

    private static final String WHOAMI = "TestUtil ";

    // behavior depends on attributes of calling application
    // (especially the PRF)
    static public void dumpEntries(RecordingListFilter filter)
    {
        if (filter instanceof RecordingStateFilter)
        {
            System.out.println(WHOAMI + "dumpEntries() RecordingStateFilter: "
                    + ((RecordingStateFilter) filter).getFilterValue());
        }
        else
        {
            System.out.println(WHOAMI + "dumpEntries() filter: " + filter);
        }

        OcapRecordingManager mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == mgr)
        {
            throw new NullPointerException(WHOAMI + "Failed to retrieve OcapRecordingManager Object ref");
        }

        RecordingList rlist = mgr.getEntries(filter);
        int numEntries = rlist.size();
        System.out.println(WHOAMI + BANNER);
        for (int i = 0; i < numEntries; ++i)
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) (rlist.getRecordingRequest(i));
            System.out.println(WHOAMI + "Entry: " + getReqInfo(orr));
        }
        System.out.println(WHOAMI + BANNER);
    }

    // behavior depends on attributes of calling application
    // (especially the PRF)
    static public void dumpOverlappingEntries()
    {
        OcapRecordingManager mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == mgr)
        {
            throw new NullPointerException(WHOAMI + "Failed to retrieve OcapRecordingManager Object ref");
        }

        RecordingList rlist = mgr.getEntries();
        int numEntries = rlist.size();
        System.out.println(WHOAMI + BANNER);
        System.out.println(WHOAMI + "Examining " + numEntries + " Entries for overlap ...");
        for (int i = 0; i < numEntries; ++i)
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) (rlist.getRecordingRequest(i));
            System.out.println(WHOAMI + "Entry: " + getReqInfo(orr));

            RecordingList rlistOvlp = orr.getOverlappingEntries();
            int numOvlpEntries = rlistOvlp.size();
            System.out.println(WHOAMI + "Found " + numOvlpEntries + " Overlapping Entries ...");
            for (int j = 0; j < numOvlpEntries; ++j)
            {
                OcapRecordingRequest orrOvlp = (OcapRecordingRequest) (rlistOvlp.getRecordingRequest(j));
                System.out.println(WHOAMI + "Overlapping Entry: " + getReqInfo(orrOvlp));
            }
        }
        System.out.println(WHOAMI + BANNER);
    }

    // behavior depends on attributes of calling application
    // (especially the PRF)
    static public void dumpParent()
    {
        OcapRecordingManager mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == mgr)
        {
            throw new NullPointerException(WHOAMI + "Failed to retrieve OcapRecordingManager Object ref");
        }

        RecordingList rlist = mgr.getEntries();
        int numEntries = rlist.size();
        System.out.println(WHOAMI + BANNER);
        for (int i = 0; i < numEntries; ++i)
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) (rlist.getRecordingRequest(i));
            System.out.println(WHOAMI + "Entry: " + getReqInfo(orr));

            OcapRecordingRequest parentReq = (OcapRecordingRequest) orr.getParent();
            System.out.println(WHOAMI + "Entry Parent: " + parentReq);
            if (null != parentReq)
            {
                System.out.println(WHOAMI + "Entry: " + getReqInfo(parentReq));
            }
        }
        System.out.println(WHOAMI + BANNER);
    }

    //
    // behavior depends on attributes of calling application
    // (especially the PRF)
    static public void dumpRoot()
    {
        OcapRecordingManager mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == mgr)
        {
            throw new NullPointerException(WHOAMI + "Failed to retrieve OcapRecordingManager Object ref");
        }

        RecordingList rlist = mgr.getEntries();
        int numEntries = rlist.size();
        System.out.println(WHOAMI + BANNER);
        for (int i = 0; i < numEntries; ++i)
        {
            OcapRecordingRequest orr = (OcapRecordingRequest) (rlist.getRecordingRequest(i));
            System.out.println(WHOAMI + "Entry: " + getReqInfo(orr));

            OcapRecordingRequest rootReq = (OcapRecordingRequest) orr.getRoot();
            System.out.println(WHOAMI + "Entry Root: " + rootReq);
            if (null != rootReq)
            {
                System.out.println(WHOAMI + "Entry: " + getReqInfo(rootReq));
            }
        }
        System.out.println(WHOAMI + BANNER);
    }

    static public String getReqInfo(OcapRecordingRequest orr)
    {
        try
        {
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) orr.getRecordingSpec();
            javax.tv.locator.Locator[] srcAry = lrs.getSource();
            Date sTime = lrs.getStartTime();
            long dMsec = lrs.getDuration();
            int state = orr.getState();
            return new String((srcAry[0] + " " + sTime + " " + dMsec + " " + stateToString(state)));
        }
        catch (IllegalStateException e)
        {
            System.err.println("Caught IllegalStateException when querying OcapRecordingRequest");
            return "UNKNOWN";
        }
    }

    static public String stateToString(int state)
    {
        String stateString = null;
        if (OcapRecordingRequest.TEST_STATE == state)
        {
            stateString = "TEST_STATE";
        }
        else if (ParentRecordingRequest.CANCELLED_STATE == state)
        {
            stateString = "CANCELLED_STATE";
        }
        else if (ParentRecordingRequest.COMPLETELY_RESOLVED_STATE == state)
        {
            stateString = "COMPLETELY_RESOLVED_STATE";
        }
        else if (ParentRecordingRequest.PARTIALLY_RESOLVED_STATE == state)
        {
            stateString = "PARTIALLY_RESOLVED_STATE";
        }
        else if (ParentRecordingRequest.UNRESOLVED_STATE == state)
        {
            stateString = "UNRESOLVED_STATE";
        }
        else if (LeafRecordingRequest.COMPLETED_STATE == state)
        {
            stateString = "COMPLETED_STATE";
        }
        else if (LeafRecordingRequest.DELETED_STATE == state)
        {
            stateString = "DELETED_STATE";
        }
        else if (LeafRecordingRequest.FAILED_STATE == state)
        {
            stateString = "FAILED_STATE";
        }
        else if (LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE == state)
        {
            stateString = "IN_PROGRESS_INSUFFICIENT_SPACE_STATE";
        }
        else if (LeafRecordingRequest.IN_PROGRESS_STATE == state)
        {
            stateString = "IN_PROGRESS_STATE";
        }
        else if (LeafRecordingRequest.INCOMPLETE_STATE == state)
        {
            stateString = "INCOMPLETE_STATE";
        }
        else if (LeafRecordingRequest.PENDING_NO_CONFLICT_STATE == state)
        {
            stateString = "PENDING_NO_CONFLICT_STATE";
        }
        else if (LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE == state)
        {
            stateString = "PENDING_WITH_CONFLICT_STATE";
        }
        else
        {
            stateString = "UNKNOWN_STATE";
        }
        return stateString;
    }
}
