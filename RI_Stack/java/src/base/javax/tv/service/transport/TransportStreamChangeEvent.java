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

package javax.tv.service.transport;

import javax.tv.service.*;

/**
 * A <code>TransportStreamChangeEvent</code> notifies an
 * <code>TransportStreamChangeListener</code> of changes detected in a
 * <code>TransportStreamCollection</code>. Specifically, this event signals the
 * addition, removal, or modification of a <code>TransportStream</code>.
 * 
 * @see TransportStreamCollection
 * @see TransportStream
 */
public class TransportStreamChangeEvent extends TransportSIChangeEvent
{

    /**
     * Constructs a <code>TransportStreamChangeEvent</code>.
     * 
     * @param collection
     *            The transport stream collection in which the change occurred.
     * 
     * @param type
     *            The type of change that occurred.
     * 
     * @param ts
     *            The <code>TransportStream</code> that changed.
     */
    public TransportStreamChangeEvent(TransportStreamCollection collection, SIChangeType type, TransportStream ts)
    {
        super(collection, type, ts);
    }

    /**
     * Reports the <code>TransportStreamCollection</code> that generated the
     * event. It will be identical to the object returned by the
     * <code>getTransport()</code> method.
     * 
     * @return The <code>TransportStreamCollection</code> that generated the
     *         event.
     */
    public TransportStreamCollection getTransportStreamCollection()
    {
        return (TransportStreamCollection) getTransport();
    }

    /**
     * Reports the <code>TransportStream</code> that changed. It will be
     * identical to the object returned by the inherited
     * <code>SIChangeEvent.getSIElement</code> method.
     * 
     * @return The <code>TransportStream</code> that changed.
     */
    public TransportStream getTransportStream()
    {
        return (TransportStream) getSIElement();
    }

    // Description copied from EventObject
    public String toString()
    {
        return this.getClass().getName() + "[source=" + source.toString() + ", type=" + getChangeType()
                + ", transport stream=" + getTransportStream() + "]";
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
