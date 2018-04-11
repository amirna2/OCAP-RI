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

package org.ocap.dvr;

import java.util.EventListener;
import javax.tv.service.selection.ServiceContext;

/**
 * This interface represents a listener that can be added to listen for
 * recording playback start. The implementation SHALL notify a listener once
 * when a recording playback starts. For purposes of this listener playback is
 * considered ongoing while the presenting <code>ServiceContext</code> is in the
 * presenting state regardless of trick mode. This listener is specific to
 * <code>ServiceContext</code> recording playback and does not notify for
 * discreet <code>Player</code> based recording playback.
 */
public interface RecordingPlaybackListener extends EventListener
{
    /**
     * Notifies the listener a recording playback has started. The
     * implementation SHALL create a new carousel Id for any artificial carousel
     * in each playback. The carouselIDs parameter SHALL reference broadcast
     * carousels when stored with a recorded service. An artificial carousel ID
     * shall not conflict with a carousel ID of a signaled carousel that was
     * also stored with the recorded service and presented by the context
     * parameter. An artificial carousel ID MAY conflict with other carousel
     * IDs.
     * 
     * @param context
     *            The <code>ServiceContext</code> presenting the recorded
     *            service.
     * @param artificialCarouselID
     *            Carousel ID for an artificial carousel that MAY have been
     *            created for the recording being played back. A value of -1
     *            indicates no artificial carousel was created.
     * @param carouselIDs
     *            Array of carousel IDs associated with broadcast carousels
     *            stored with the recording being played back. If no carousels
     *            are contained a zero length array is passed in.
     */
    public void notifyRecordingPlayback(ServiceContext context, int artificialCarouselID, int[] carouselIDs);
}
