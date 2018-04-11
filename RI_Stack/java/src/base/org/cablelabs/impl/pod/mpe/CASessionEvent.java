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

package org.cablelabs.impl.pod.mpe;

import org.cablelabs.impl.manager.pod.CASession;

public class CASessionEvent
{
    public interface EventID
    {
        /*
         * WARNING: These constants represent *MPE* event codes from mpe_pod.h. 
         * If the MPE event codes change, then these values will need to change
         * accordingly (the TIMEOUT event does NOT represent an MPE event code). 
         */

        static final int TIMEOUT = 0x93FF;
        /**
         * One or more streams in the decrypt request cannot be descrambled due to
         * inability to purchase (ca_pmt_reply/update with CA_enable of 0x71).
         *
         * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
         */
        static final int CANNOT_DESCRAMBLE_ENTITLEMENT = 0x9271;

        /**
         * One or more streams in the decrypt request cannot be descrambled due to
         * lack of resources (ca_pmt_reply/update with CA_enable of 0x73).
         *
         * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
         */
        static final int CANNOT_DESCRAMBLE_RESOURCES = 0x9273;

        /**
         * One or more streams in the decrypt request required MMI interaction
         * for a purchase dialog (ca_pmt_reply/update CA_enable of 0x02) and user
         * interaction is now in progress.
         *
         * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
         */
        static final int MMI_PURCHASE_DIALOG = 0x9202;

        /**
         * One or more streams in the decrypt request required MMI interaction
         * for a technical dialog (ca_pmt_reply/update CA_enable of 0x03) and user
         * interaction is now in progress.
         *
         * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
         */
        static final int MMI_TECHNICAL_DIALOG = 0x9203;

        /**
         * All streams in the decrypt request can be descrambled (ca_pmt_reply/update
         *  with CA_enable of 0x01).
         * OptionalEventData3 (msb to lsb): ((unused (24 bits) | LTSID (8 bits))
         */
        static final int FULLY_AUTHORIZED = 0x9201;

        /**
         * 
         * The MPE session is terminated. All resources have been released.
         * No more events will be received on the registered queue/ed handler.
         * Only induced as a response to mpe_podStopDecrypt().
         * 
         * THIS EVENT CAN BE IGNORED FOR CASessionEventListeners (it will not be sent)
         */
        static final int SESSION_SHUTDOWN = 0x9300;

        /**
         * CableCard removed. The session is terminated and will not recover.
         */
        static final int POD_REMOVED = 0x9301;

        /**
         * The CableCard resources for this session have been lost to a higher priority session.
         * The session is still active and is awaiting resources. An appropriate
         * mpe_PodDecryptSessionEvent is signaled when resources become available (see OCAP 16.2.1.7).
         */
        static final int RESOURCE_LOST = 0x9302;
    } // END EventID
    
    public static String stringForEvent(int eventID)
    {
        switch (eventID)
        {
            case CASessionEvent.EventID.CANNOT_DESCRAMBLE_ENTITLEMENT:
                return "CANNOT_DESCRAMBLE_ENTITLEMENT";
            case CASessionEvent.EventID.CANNOT_DESCRAMBLE_RESOURCES:
                return "CANNOT_DESCRAMBLE_RESOURCES";
            case CASessionEvent.EventID.MMI_PURCHASE_DIALOG:
                return "MMI_PURCHASE_DIALOG";
            case CASessionEvent.EventID.MMI_TECHNICAL_DIALOG:
                return "MMI_TECHNICAL_DIALOG";
            case CASessionEvent.EventID.FULLY_AUTHORIZED:
                return "FULLY_AUTHORIZED";
            case CASessionEvent.EventID.SESSION_SHUTDOWN:
                return "SESSION_SHUTDOWN";
            case CASessionEvent.EventID.POD_REMOVED:
                return "POD_REMOVED";
            case CASessionEvent.EventID.RESOURCE_LOST:
                return "RESOURCE_LOST";
            case CASessionEvent.EventID.TIMEOUT:
                return "TIMEOUT";
            default:
                return "unknown";
        } // END switch (eventID)
    } // END stringForEvent()
    
    private final int eventID;

    private final int data1;
    /* data2 is the EdHandle - no need to store it */
    private final int data3;

    private static final int INVALID_VAL = -1;
    private int m_program = INVALID_VAL;
    private short m_ltsid = CASession.LTSID_UNDEFINED;

    public CASessionEvent(int eventID, int data1, int data3)
    {
        this.eventID = eventID;
        this.data1 = data1;
        this.data3 = data3;
        
        switch (eventID)
        {
            case CASessionEvent.EventID.SESSION_SHUTDOWN:
            case CASessionEvent.EventID.POD_REMOVED:
            case CASessionEvent.EventID.RESOURCE_LOST:
            case CASessionEvent.EventID.TIMEOUT:
            {
                break;
            }
            case CASessionEvent.EventID.CANNOT_DESCRAMBLE_ENTITLEMENT:
            case CASessionEvent.EventID.CANNOT_DESCRAMBLE_RESOURCES:
            case CASessionEvent.EventID.MMI_PURCHASE_DIALOG:
            case CASessionEvent.EventID.MMI_TECHNICAL_DIALOG:
            case CASessionEvent.EventID.FULLY_AUTHORIZED:
            {
                this.m_ltsid = (short) (this.data3 & 0xFF);
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid event id: 0x" + Integer.toHexString(eventID) + ", data1: " + data1 + ", data3: " + data3);
        } // END switch (eventID)
    } // END CASessionEvent()

    /**
     * Get the event ID.
     */
    public int getEventID()
    {
        return eventID;
    }
    
    /**
     * Get the LTSID associated with the event.
     * Note: Only valid for certain event codes
     * 
     * @return The LTSID associated with the event or CASession.LTSID_UNDEFINED if this 
     *         event type doesn't include an LTSID
     */
    public short getLTSID()
    {
        return m_ltsid;
    }
    
    /**
     * Get the program number associated with the event.
     * Note: Only valid for certain event codes
     * 
     * @return The program number associated with the event
     * @throws IllegalArgumentException if there's no program number associated with the EventID
     */
    public int getProgramNumber()
    {
        if (m_program == INVALID_VAL)
        {
            throw new IllegalArgumentException("Event code " + eventID + " does not signal a program number");
        }
        
        return m_program;
    }        
    
    public String toString()
    {
        return "CASessionEvent {event " + stringForEvent(eventID) 
                + "(0x" +  Integer.toHexString(eventID)
                + "),data1 0x" + Integer.toHexString(data1)
                + ",data2 0x" + Integer.toHexString(data3) + '}';
    }
} // END CASessionEvent
