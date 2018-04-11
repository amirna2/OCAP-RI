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
 * VBIFilterEvent.java
 *
 * Created on September 18, 2004, 1:33 PM
 */

package org.ocap.media;

/**
 * <p>
 * This class represents a VBI filter event. When a specific event happens, the
 * {@link VBIFilterListener#filterUpdate} method is called with an event that
 * has a proper event code to indicate the event.
 * </p>
 *
 * @author Shigeaki Watanabe (Panasonic)
 * @author Amir Nathoo
 */
public class VBIFilterEvent
{
    /**
     * Indicates that the first VBI data unit is available. This event code is
     * issued only once after calling {@link VBIFilter#startFiltering} method
     * even if multiple lines/fields is specified to the filter. Filtering
     * continues.
     */
    public static final int EVENT_CODE_FIRST_VBI_DATA_AVAILABLE = 1;

    /**
     * Indicates current filtering is terminated forcibly for any reason except
     * other EVENT_CODE_ constants. E.g., a {@link VBIFilter#stopFiltering} is
     * called.
     */
    public static final int EVENT_CODE_FORCIBLE_TERMINATED = 2;

    /**
     * Indicates the current video for VBI data unit filtering has changed. Note
     * that current filtering doesn't stop even if this event happens. An
     * application may stop filtering and then restart to retrieve valid data
     * units.
     */
    public static final int EVENT_CODE_VIDEO_SOURCE_CHANGED = 3;

    /**
     * Indicates descrambling is unavailable for current video. Note that
     * current filtering doesn't stop even if this event happens. Continues
     * filtering until timeout.
     */
    public static final int EVENT_CODE_FAILED_TO_DESCRAMBLE = 4;

    /**
     * Indicates a timeout (specified by {@link VBIFilter#setTimeOut}) occurred,
     * i.e., this event code indicates no data unit is available.
     */
    public static final int EVENT_CODE_TIMEOUT = 5;

    /**
     * Indicates an internal buffer is full. Filtering stops automatically.
     */
    public static final int EVENT_CODE_BUFFER_FULL = 6;

    /**
     * Indicates that a specified time-period elapsed after receiving the first
     * byte of a data unit.
     */
    public static final int EVENT_CODE_TIME_NOTIFICATION = 7;

    /**
     * Indicates that the specified number of new data units are filtered and
     * stored in a buffer cyclically.
     */
    public static final int EVENT_CODE_UNITS_NOTIFICATION = 8;

    /**
     * Constructor of this class.
     */
    public VBIFilterEvent()
    {
    }

    /**
     * This method returns an instance of a class implementing VBIFilter that is
     * the source of the event.
     *
     * @return instance of a class implementing VBIFilter that is the source of
     *         the event
     */
    public Object getSource()
    {
        return null;
    }

    /**
     * This method returns application specific data that was specified by
     * VBIFilter.startFiltering() methods.
     *
     * @return an application specific data that was specified by
     *         VBIFilter.startFiltering() methods.
     */
    public Object getAppData()
    {
        return null;
    }

    /**
     * This method returns the specific event code that caused this event.
     *
     * @return an event code. One of the constants that has EVENT_CODE_ prefix.
     */
    public int getEventCode()
    {
        return 0;
    }
}
