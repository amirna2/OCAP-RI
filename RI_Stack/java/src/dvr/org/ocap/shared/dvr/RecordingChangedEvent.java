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

package org.ocap.shared.dvr;

import java.util.EventObject;

/**
 * Event used to notify listeners of changes in the list of recording requests
 * maintained by the RecordingManager. This event is not generated for changes
 * other than those for which constants are defined in this class.
 */
public class RecordingChangedEvent extends EventObject
{

    /**
     * A new <code>RecordingRequest</code> was added.
     */
    public static final int ENTRY_ADDED = 1;

    /**
     * A <code>RecordingRequest</code> was deleted.
     */
    public static final int ENTRY_DELETED = 2;

    /**
     * The state of a <code>RecordingRequest</code> changed
     */
    public static final int ENTRY_STATE_CHANGED = 3;

    /*
     * (non-Javadoc) New state variable
     */
    private int m_newState = 0;

    /*
     * (non-Javadoc) Old state variable.
     * 
     * @author jspruiel
     */
    private int m_oldState = 0;

    /*
     * 
     */
    private int m_reasonForChange;

    /**
     * Constructs the event. Events constructed with this constructor shall have
     * a type of ENTRY_STATE_CHANGED.
     * 
     * @param source
     *            The <code>RecordingRequest</code> that caused the event.
     * @param newState
     *            the state the <code>RecordingRequest</code> is now in.
     * @param oldState
     *            the state the <code>RecordingRequest</code> was in before the
     *            state change.
     */
    public RecordingChangedEvent(RecordingRequest source, int newState, int oldState)
    {
        super(source);
        m_newState = newState;
        m_oldState = oldState;
        m_reasonForChange = ENTRY_STATE_CHANGED;
    }

    /**
     * Constructs the event.
     * 
     * @param source
     *            The <code>RecordingRequest</code> that caused the event.
     * @param newState
     *            the state the <code>RecordingRequest</code> is now in.
     * @param oldState
     *            the state the <code>RecordingRequest</code> was in before the
     *            state change.
     * @param type
     *            the type of change which caused this event to be generated
     */
    public RecordingChangedEvent(RecordingRequest source, int newState, int oldState, int type)
    {
        super(source);
        m_newState = newState;
        m_oldState = oldState;
        m_reasonForChange = type;
    }

    /**
     * Returns the <code>RecordingRequest</code> that caused the event.
     * 
     * @return The <code>RecordingRequest</code> that caused the event.
     */
    public RecordingRequest getRecordingRequest()
    {
        return (RecordingRequest) super.getSource();
    }

    /**
     * Returns the change to the <code>RecordingRequest</code>.
     * 
     * @return the type of the change which caused the event
     */
    public int getChange()
    {
        return m_reasonForChange;
    }

    /**
     * Returns the new state for the <code>RecordingRequest</code>.
     * 
     * @return The new state.
     */
    public int getState()
    {
        return m_newState;
    }

    /**
     * Returns the old state for the <code>RecordingRequest</code>.
     * 
     * @return The old state.
     */
    public int getOldState()
    {
        return m_oldState;
    }
    
    private final String reasonString[] = { "INVALID", "ENTRY_ADDED", "ENTRY_DELETED", "ENTRY_STATE_CHANGED"};

    public String toString()
    {
        return "RecordingChangedEvent 0x" + Integer.toHexString(this.hashCode()).toString() 
               + ":(" + reasonString[m_reasonForChange] + ",oldState " + m_oldState 
               + ",newState " + m_newState + ')';
    }
}
