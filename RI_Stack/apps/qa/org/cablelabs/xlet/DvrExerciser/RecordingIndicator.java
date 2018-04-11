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

package org.cablelabs.xlet.DvrExerciser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.dvb.ui.DVBGraphics;
import org.havi.ui.HComponent;
import org.ocap.shared.dvr.LeafRecordingRequest;

/**
 * A utility class that displays information about the state of a 'current'
 * recording. This class will display the following: If no recording is
 * available to play, displays nothing. If a recording is in progress, displays
 * flashing red circle. If a recording is complete, displays a solid red circle.
 * If a recording terminates as incomplete, displays a solid yellow circle. If
 * an error occurred during recording, displays a solid gray circle.
 * 
 * @author andy
 * 
 */
public class RecordingIndicator extends HComponent implements Runnable
{
    private static final long serialVersionUID = 3936937912585159602L;

    private boolean m_bFlash = false;

    private Color m_color = Color.red;

    // update the flash indicator
    public void run()
    {
        for (;;)
        {
            if (true == m_bFlash)
            {
                m_bFlash = false;
            }
            else
            {
                m_bFlash = true;
            }
            repaint();

            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException ex)
            {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }
    }

    /**
     * Returns whether or not the indicator should be displayed. This method
     * also determines the color of the indicator.
     * 
     * @return <code>true</code> to display the indicator, <code>false</code> to
     *         not display the indicator.
     */
    private boolean getIndicatorState()
    {
        boolean bRetVal = false;

        if ( ! DvrExerciser.getInstance().isDvrEnabled() )
        {
            return bRetVal;
        }

        int recordingState = DvrTest.getInstance().getRecordingState();
        switch (recordingState)
        {
            case LeafRecordingRequest.FAILED_STATE:
                bRetVal = true;
                m_color = Color.GRAY;
                break;

            case LeafRecordingRequest.INCOMPLETE_STATE:
                bRetVal = true;
                m_color = Color.YELLOW;
                break;

            case LeafRecordingRequest.IN_PROGRESS_STATE:
                bRetVal = m_bFlash;
                m_color = Color.RED;
                break;

            case LeafRecordingRequest.COMPLETED_STATE:
                bRetVal = true;
                m_color = Color.RED;
                break;

            default:
                bRetVal = false;
                break;
        };

        return bRetVal;
    }

    /**
     * Paint this component on the screen
     */
    public void paint(Graphics g)
    {
        Rectangle rectBounds;

        DVBGraphics dvbG = (DVBGraphics) g;

        // redraw the component using the background color
        rectBounds = getBounds();

        if (true == getIndicatorState())
        {
            dvbG.setColor(m_color);
            g.fillOval(0, 0, rectBounds.width, rectBounds.height);

            super.paint(dvbG);
        }
    }
}
