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
package org.cablelabs.impl.media.streaming;

import org.cablelabs.impl.media.streaming.session.ContentRequest;
import org.cablelabs.impl.ocap.hn.TestUtils;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntryFactory;
import org.ocap.hn.content.ContentItem;
import org.ocap.storage.ExtendedFileAccessPermissions;

import junit.framework.TestCase;

public class ContentRequestTest extends TestCase
{
    public void testParsingTimeSeekRange()
    {
        System.out.println("ContentRequestTest says Hello world");
        
        // NOTE: - testing private methods isn't really kosher
        // Parsing the time seek range header seemed worthy of testing one
        // If you need to modify parseTimeSeekRangeDlnaOrg() for testing purposes
        // then set access back to private or consider reworking this test to use
        // reflect to access private methods
        //
        /*
        ContentRequest cr = new ContentRequest(null, null);

        String str = null;
        Integer times[] = null;

        try
        {
            // Verify start time hms with no end time works
            str = "npt=335.11-";
            times = cr.parseTimeSeekRangeDlnaOrg(str);
            System.out.println("Test Case 1 - Completed w/ Start Time: " + times[0]);

            // Verify start time secs with no end time works
            str = "npt=00:05:35.3-";
            times = cr.parseTimeSeekRangeDlnaOrg(str);
            System.out.println("Test Case 2 - Completed w/ Start Time: " + times[0]);

            // Verify start time hms with same format end time works
            str = "npt=335.11-336.08";
            times = cr.parseTimeSeekRangeDlnaOrg(str);
            System.out.println("Test Case 3 - Completed w/ Start Time: " + times[0] + ", End Time: " + times[1]);

            // Verify start time secs with same format end time works
            str = "npt=00:05:35.3-00:05: 37.5";
            times = cr.parseTimeSeekRangeDlnaOrg(str);
            System.out.println("Test Case 4 - Completed w/ Start Time: " + times[0] + ", End Time: " + times[1]);

            // Verify start time hms with diff format end time works
            str = "npt=335.11-00:05:37.5";
            times = cr.parseTimeSeekRangeDlnaOrg(str);
            System.out.println("Test Case 5 - Completed w/ Start Time: " + times[0] + ", End Time: " + times[1]);

            // Verify start time secs with diff format end time works
            str = "npt=00:05:35.3-336.08";
            times = cr.parseTimeSeekRangeDlnaOrg(str);
            System.out.println("Test Case 6 - Completed w/ Start Time: " + times[0] + ", End Time: " + times[1]);
            
            str = "npt=28.340-0.000";
            times = cr.parseTimeSeekRangeDlnaOrg(str);
            System.out.println("Test Case 7 - Completed w/ Start Time: " + times[0] + ", End Time: " + times[1]);
        }
        catch (Exception e)
        {
            System.out.println("Problems in ContentRequestTest");
            e.printStackTrace();
        }
        */
    }
}
