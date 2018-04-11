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
package org.cablelabs.xlet.VideoPresentationTest;

import org.dvb.media.VideoFormatControl;
import org.havi.ui.HScreenRectangle;

/**
 * TestData
 * 
 * @author Joshua Keplinger
 * 
 */
public class TestData
{
    public int afd, dfc, sourceId, ar;

    public int scrHeight, scrWidth;

    public int parHeight, parWidth;

    public HScreenRectangle ava, tva, avaos, tvaos;

    public String toString()
    {
        return "TestData [[sourceId=" + sourceId + ",afd=" + afdToString(afd) + ",ar=" + aspectRatioToString(ar)
                + ",dfc=" + dfcToString(dfc) + "], " + "[scrHeight=" + scrHeight + ",scrWidth=" + scrWidth + "], "
                + "[parHeight=" + parHeight + ",parWidth=" + parWidth + "], " + "ava[x=" + ava.x + ",y=" + ava.y
                + ",width=" + ava.width + ",height=" + ava.height + "], " + "tva[x=" + tva.x + ",y=" + tva.y
                + ",width=" + tva.width + ",height=" + tva.height + "], " + "avaos[x=" + avaos.x + ",y=" + avaos.y
                + ",width=" + avaos.width + ",height=" + avaos.height + "], " + "tvaos[x=" + tvaos.x + ",y=" + tvaos.y
                + ",width=" + tvaos.width + ",height=" + tvaos.height + "]";
    }

    private String aspectRatioToString(int ar)
    {
        switch (ar)
        {
            case VideoFormatControl.ASPECT_RATIO_16_9:
                return "16x9";
            case VideoFormatControl.ASPECT_RATIO_4_3:
                return "4x3";
            default:
                return "" + ar;
        }
    }

    private String afdToString(int afd)
    {
        switch (afd)
        {
            case VideoFormatControl.AFD_16_9:
                return "AFD_16_9";
            case VideoFormatControl.AFD_4_3:
                return "AFD_4_3";
            case VideoFormatControl.AFD_SAME:
                return "AFD_SAME";
            case VideoFormatControl.AFD_NOT_PRESENT:
                return "AFD_NOT_PRESENT";
            default:
                return "" + afd;
        }
    }

    private String dfcToString(int dfc)
    {
        switch (dfc)
        {
            case VideoFormatControl.DFC_PLATFORM:
                return "DFC_PLATFORM";
            case VideoFormatControl.DFC_PROCESSING_FULL:
                return "DFC_PROCESSING_FULL";
            case VideoFormatControl.DFC_PROCESSING_NONE:
                return "DFC_PROCESSING_NONE";
            case VideoFormatControl.DFC_PROCESSING_UNKNOWN:
                return "DFC_PROCESSING_UNKNOWN";
            case VideoFormatControl.DFC_PROCESSING_16_9_ZOOM:
                return "DFC_PROCESSING_16_9_ZOOM";
            case VideoFormatControl.DFC_PROCESSING_CCO:
                return "DFC_PROCESSING_CCO";
            case VideoFormatControl.DFC_PROCESSING_LB_16_9:
                return "DFC_PROCESSING_LB_16_9";
            case VideoFormatControl.DFC_PROCESSING_PAN_SCAN:
                return "DFC_PROCESSING_PAN_SCAN";
            default:
                return "" + dfc;
        }
    }
}
