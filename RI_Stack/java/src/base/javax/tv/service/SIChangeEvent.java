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

package javax.tv.service;

import java.util.EventObject;

/**
 * <code>SIChangeEvent</code> objects are sent to <code>SIChangeListener</code>
 * instances to signal detected changes in the SI database.
 * <p>
 * 
 * Note that while the SI database may detect changes, notification of which
 * specific <code>SIElement</code> has changed is not guaranteed. The entity
 * reported by the method <code>getSIElement()</code> will be either:
 * <ul>
 * <li>The specific SI element that changed, or
 * <p>
 * <li>An SI element that contains, however indirectly, the specific SI element
 * that changed, or
 * <p>
 * <li><code>null</code>, if the specific changed element is unknown.
 * </ul>
 * 
 * The level of specificity provided by the change mechanism is entirely
 * dependent on the capabilities and current resources of the implementation.
 * 
 * <code>SIChangeEvent</code> instances also report the kind of change that
 * occurred to the SI element, via the method <code>getChangeType()</code>:
 * <ul>
 * 
 * <li><code>SIChangeType.ADD</code> indicates that the reported SI element is
 * new in the database.
 * <p>
 * 
 * <li><code>SIChangeType.REMOVE</code> indicates that the reported SI element
 * is defunct and no longer cached by the database. The results of subsequent
 * method invocations on the removed SIElement are undefined.
 * <p>
 * 
 * <li><code>SIChangeType.MODIFY</code> indicates that the data encapsulated by
 * the reported SI element has changed.
 * 
 * </ul>
 * 
 * In the event that the SIElement reported by this event is not the actual
 * element that changed in the broadcast (i.e. it is instead a containing
 * element or <code>null</code>), the <code>SIChangeType</code> will be
 * <code>SIChangeTypeMODIFY</code>. Individual SI element changes are reported
 * only once, i.e., a change to an SI element is not also reported as a change
 * to any containing (or "parent") SI elements.
 * 
 * @see #getSIElement
 * @see #getChangeType
 */
public abstract class SIChangeEvent extends EventObject
{

    SIChangeType type;

    SIElement element;

    /**
     * Constructs an <code>SIChangeEvent</code> object.
     * 
     * @param source
     *            The entity in which the change occurred.
     * 
     * @param type
     *            The type of change that occurred.
     * 
     * @param e
     *            The <code>SIElement</code> that changed, or <code>null</code>
     *            if this is unknown.
     */
    public SIChangeEvent(Object source, SIChangeType type, SIElement e)
    {
        super(source);
        this.type = type;
        this.element = e;
    }

    /**
     * Reports the <code>SIElement</code> that changed.
     * <p>
     * 
     * This method may return <code>null</code>, since it is not guaranteed that
     * the SI database can or will determine which element in a particular table
     * changed.
     * 
     * @return The <code>SIElement</code> that changed, or <code>null</code> if
     *         this is unknown.
     */
    public SIElement getSIElement()
    {
        return element;
    }

    /**
     * Indicates the type of change that occurred.
     * 
     * @return The type of change that occurred.
     */
    public SIChangeType getChangeType()
    {
        return type;
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
