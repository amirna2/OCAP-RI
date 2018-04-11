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

package javax.tv.media;

import javax.tv.locator.*;
import javax.media.Controller;

/**
 * <code>MediaSelectEvent</code> is the base class of events sent to
 * <code>MediaSelectListener</code> instances.
 * 
 * @see MediaSelectListener
 */
public abstract class MediaSelectEvent extends java.util.EventObject
{

    private Locator selection[];

    /**
     * Creates a new <code>MediaSelectEvent</code>.
     * 
     * @param controller
     *            The Controller that generated this event.
     * @param selection
     *            The <code>Locator</code> instances on which selection was
     *            attempted.
     */
    public MediaSelectEvent(Controller controller, Locator[] selection)
    {
        super(controller);
        this.selection = selection;
    }

    /**
     * Reports the Controller that generated this event.
     * 
     * @return The Controller that generated this event.
     */
    public Controller getController()
    {
        return (Controller) getSource();
    }

    /**
     * Reports the selection that caused this event. This corresponds to the
     * service component(s) specified as parameters of the
     * <code>MediaSelectControl</code> methods {@link MediaSelectControl#add
     * add(Locator)}, {@link MediaSelectControl#remove remove(Locator)},
     * {@link MediaSelectControl#select(Locator) select(Locator)} and
     * {@link MediaSelectControl#select(Locator[]) select(Locator[])}, and the
     * paramter <code>toComponent</code> of the method
     * {@link MediaSelectControl#replace(Locator, Locator)
     * replace(fromComponent, toComponent)}.
     * 
     * @return The selection that caused this event.
     * @see MediaSelectControl
     */
    public Locator[] getSelection()
    {
        return selection;
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
