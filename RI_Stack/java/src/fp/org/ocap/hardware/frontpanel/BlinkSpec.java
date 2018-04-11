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

package org.ocap.hardware.frontpanel;

/**
 * This interface represents the front panel display blinking specification. If
 * BlinkSpec is for {@link TextDisplay}, all characters blink at the same time.
 */
public interface BlinkSpec
{
    /**
     * Gets the number of times per minute the display will blink.
     * 
     * @return Number of blink iterations per minute.
     */
    public int getIterations();

    /**
     * Gets the maximum number of times per minute all segments in an LED can
     * blink.
     * 
     * @return Maximum number of blink iterations per minute. Returns zero if
     *         blinking is not supported.
     */
    public int getMaxCycleRate();

    /**
     * Sets the number of times per minute data will blink across all of the
     * LEDs.
     * 
     * @param iterations
     *            Number of blink iterations per minute.
     * 
     * @throws IllegalArgumentException
     *             if the iteration is negative or cannot be supported by the
     *             front panel.
     */
    public void setIterations(int iterations);

    /**
     * Gets the percentage of time the text will be on during one blink
     * iteration.
     * 
     * @return Text display blink on percentage duration.
     */
    public int getOnDuration();

    /**
     * Sets the percentage of time the text display will remain on during one
     * blink iteration.
     * 
     * @param duration
     *            Text display blink on percentage duration. Setting this value
     *            to 100 sets the display no solid, no blinking. Setting this
     *            value to 0 effectively turns off the front panel display.
     *            Subtracting this value from 100 yields the percentage of off
     *            time during one blink iteration.
     * 
     * @throws IllegalArgumentException
     *             if the duration is negative or exceeds 100.
     */
    public void setOnDuration(int duration);

}
