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

package org.cablelabs.impl.media.player;

import java.awt.Rectangle;

import org.ocap.media.VideoFormatControl;
import org.apache.log4j.Logger;
import org.dvb.media.VideoTransformation;
import org.havi.ui.HScreenPoint;

import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;

public class VideoTransformationDfc extends VideoTransformation implements Asserting
{

    /** Log4J */
    private static final Logger log = Logger.getLogger(Util.class);

    public VideoTransformationDfc(int dfcParam, VideoTransformation vt)
    {
        super(vt.getClipRegion(), vt.getScalingFactors()[0], vt.getScalingFactors()[1], vt.getVideoPosition());

        // check for a valid dfc value passed in
        if ((dfcParam >= VideoFormatControl.DFC_PROCESSING_NONE && dfcParam <= VideoFormatControl.DFC_PROCESSING_16_9_ZOOM))
        {
            dfc = dfcParam;
        }
        else
        {
            if (ASSERTING) Assert.condition(false, "VideoTransformationDFC - Unexpected DFC: " + dfc);

            dfc = VideoFormatControl.DFC_PROCESSING_NONE;
        }
    }

    public int getDfc()
    {
        return dfc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.media.VideoTransformation#isPanScan()
     */
    public boolean isPanAndScan()
    {
        return (dfc == VideoFormatControl.DFC_PROCESSING_PAN_SCAN) ? true : false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.media.VideoTransformation#setClipRegion(java.awt.Rectangle)
     */
    public void setClipRegion(Rectangle clipRect)
    {
        dfc = VideoFormatControl.DFC_PROCESSING_NONE;
        super.setClipRegion(clipRect);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dvb.media.VideoTransformation#setScalingFactors(float, float)
     */
    public void setScalingFactors(float horizontalScalingFactor, float verticalScalingFactor)
    {
        dfc = VideoFormatControl.DFC_PROCESSING_NONE;
        super.setScalingFactors(horizontalScalingFactor, verticalScalingFactor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.dvb.media.VideoTransformation#setVideoPosition(org.havi.ui.HScreenPoint
     * )
     */
    public void setVideoPosition(HScreenPoint location)
    {
        dfc = VideoFormatControl.DFC_PROCESSING_NONE;
        super.setVideoPosition(location);
    }

    private int dfc;
}
