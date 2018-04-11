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
 * Created on August 4, 2005
 */
package org.cablelabs.xlet.DvrSecurityTest;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;

/**
 * Xlet to exercise Recording Request list access. As the class name implies,
 * this Xlet should be run as a Monitor Application, including provisioning of
 * an appropriate Permission Request File (PRF)
 * 
 */
public class ListAccessMAXlet implements Xlet
{
    private static final String BANNER = "+---------------------------------------------------------+";

    private static final String WHOAMI = "ListAccessMAXlet ";

    // Instance variables in alphabetical order ...
    private OcapRecordingManager m_mgr;

    private XletContext m_xctx;

    private String m_configFname;

    public void startXlet()
    {
        init();

        TestUtil.dumpOverlappingEntries();
        // unfiltered entries ...
        TestUtil.dumpEntries(null);
        // filtered entries ...
        TestUtil.dumpEntries(new RecordingStateFilter(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE));
        // Root requests ...
        TestUtil.dumpRoot();
        // Parent requests ...
        TestUtil.dumpParent();
        System.out.println(BANNER);
        System.out.println(WHOAMI + "FINISHED - REVIEW LOG MESSAGES");
        System.out.println(BANNER);
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
