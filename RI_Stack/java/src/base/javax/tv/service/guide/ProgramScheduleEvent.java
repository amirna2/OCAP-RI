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

package javax.tv.service.guide;

import javax.tv.service.*;

/**
 * A <code>ProgramScheduleEvent</code> notifies an
 * <code>ProgramScheduleListener</code> of changes to program events detected in
 * a <code>ProgramSchedule</code>. Specifically, this event signals the
 * addition, removal, or modification of a <code>ProgramEvent</code> in a
 * <code>ProgramSchedule</code>, or a change to the <code>ProgramEvent</code>
 * that is current.
 * <p>
 * 
 * The class <code>ProgramScheduleChangeType</code> defines the kinds of changes
 * reported by <code>ProgramScheduleEvent</code>. A
 * <code>ProgramScheduleChangeType</code> of <code>CURRENT_PROGRAM_EVENT</code>
 * indicates that the current <code>ProgramEvent</code> of a
 * <code>ProgramSchedule</code> has changed in identity.
 * 
 * @see ProgramScheduleListener
 * @see ProgramScheduleChangeType
 */
public class ProgramScheduleEvent extends SIChangeEvent
{

    /**
     * Constructs a <code>ProgramScheduleEvent</code>.
     * 
     * @param schedule
     *            The schedule in which the change occurred.
     * 
     * @param type
     *            The type of change that occurred.
     * 
     * @param e
     *            The <code>ProgramEvent</code> that changed.
     */
    public ProgramScheduleEvent(ProgramSchedule schedule, SIChangeType type, ProgramEvent e)
    {
        super(schedule, type, e);
    }

    /**
     * Reports the <code>ProgramSchedule</code> that generated the event. The
     * object returned will be identical to the object returned by the inherited
     * <code>EventObject.getSource()</code> method.
     * 
     * @return The <code>ProgramSchedule</code> that generated the event.
     * 
     * @see java.util.EventObject#getSource
     */
    public ProgramSchedule getProgramSchedule()
    {
        return (ProgramSchedule) getSource();
    }

    /**
     * Reports the <code>ProgramEvent</code> that changed. If the
     * <code>ProgramScheduleChangeType</code> is
     * <code>CURRENT_PROGRAM_EVENT</code>, the <code>ProgramEvent</code> that
     * became current will be returned. The object returned will be identical to
     * the object returned by inherited <code>SIChangeEvent.getSIElement</code>
     * method.
     * 
     * @return The <code>ProgramEvent</code> that changed.
     * 
     * @see javax.tv.service.SIChangeEvent#getSIElement
     */
    public ProgramEvent getProgramEvent()
    {
        return (ProgramEvent) getSIElement();
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
