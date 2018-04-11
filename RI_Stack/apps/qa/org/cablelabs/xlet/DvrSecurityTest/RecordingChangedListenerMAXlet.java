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
 * Created on August 8, 2005
 */
package org.cablelabs.xlet.DvrSecurityTest;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;

/**
 * Xlet that listens for Recording Changed Events. This Xlet should be a Monitor
 * App Xlet: both RecordingPermission("read",...) and Extended File Access
 * Permissions are involved.
 */
public class RecordingChangedListenerMAXlet implements Xlet, RecordingChangedListener
{
    private static final String BANNER = "+---------------------------------------------------------+";

    private static final String WHOAMI = "RecordingChangedListenerXlet ";

    // Instance variables in alphabetical order ...
    private OcapRecordingManager m_mgr;

    private XletContext m_xctx;

    private String m_configFname;

    public void startXlet()
    {
        init();
        m_mgr.addRecordingChangedListener(this);
    }

    public void pauseXlet()
    {
    }

    public void destroyXlet(boolean x)
    {
    }

    public void initXlet(XletContext ctx)
    {
        m_xctx = ctx;
    }

    public void recordingChanged(RecordingChangedEvent e)
    {
        System.out.println(BANNER);
        System.out.println(WHOAMI + "recordingChanged: " + e);
        int change = e.getChange();
        if (RecordingChangedEvent.ENTRY_ADDED == change)
        {
            System.out.println(WHOAMI + "recordingChanged: ENTRY_ADDED");
        }
        else if (RecordingChangedEvent.ENTRY_DELETED == change)
        {
            System.out.println(WHOAMI + "recordingChanged: ENTRY_DELETED");
        }
        else if (RecordingChangedEvent.ENTRY_STATE_CHANGED == change)
        {
            int oldState = e.getOldState();
            int newState = e.getState();
            System.out.println(WHOAMI + "recordingChanged: ENTRY_STATE_CHANGED from "
                    + TestUtil.stateToString(oldState) + " to " + TestUtil.stateToString(newState));
        }
        else
        {
            System.out.println(WHOAMI + "recordingChanged UNKNOWN CHANGE: " + change);
        }
        OcapRecordingRequest orr = (OcapRecordingRequest) e.getRecordingRequest();
        System.out.println(WHOAMI + "OcapRecordingRequest: " + TestUtil.getReqInfo(orr));
        System.out.println(BANNER);
    }

    private void init()
    {
        m_mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == m_mgr)
        {
            throw new NullPointerException("Failed to retrieve OcapRecordingManager Object ref");
        }
        else
        {
            System.out.println("Retrieved OcapRecordingManager Object ref: " + m_mgr);
        }
    }
}
