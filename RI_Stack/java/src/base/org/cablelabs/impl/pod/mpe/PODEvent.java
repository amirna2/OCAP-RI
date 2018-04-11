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

/**
 * POD-specific events that reflect MPE events
 */
public class PODEvent
{
    public interface EventID
    {
        /* events */
        public static final int POD_EVENT_GENERIC_FEATURE_UPDATE = 0x9001;

        public static final int POD_EVENT_APP_INFO_UPDATE = 0x9002;

        public static final int POD_EVENT_POD_INSERTED = 0x9003;

        public static final int POD_EVENT_POD_READY = 0x9004;

        public static final int POD_EVENT_POD_REMOVED = 0x9005;

        /**
         * The contract is that Java MUST call (through JNI) mpe_podRecvAPDU on
         * EVERY <code>POD_EVENT_RECV_APDU</code> event that it receives. This
         * is necessary to pump APDUs through MPE and MPEOS.
         * 
         * The other part of the contract is that a mpe_podRecvAPDU will not
         * block if a <code>POD_EVENT_RECV_APDU</code> event has been received.
         */
        public static final int POD_EVENT_RECV_APDU = 0x9006;

        /**
         * MPE is echoing back a failed APDU Send message through the Receive
         * functionality.
         */
        public static final int POD_EVENT_SEND_APDU_FAILURE = 0x9007;

		/*
		 * resource has been freed and is available to an interested party
		 */        
        public static final int POD_EVENT_RESOURCE_AVAILABLE = 0x9008; 
        
        /*
         * A POD reset will start shortly
         */
        public static final int POD_EVENT_RESET_PENDING = 0x900A;
        
        /* end events */
    }

    private final int event;

    private final int data1;

    /* data2 is the EdHandle */
    private final int data3;

    public PODEvent(int event, int data1, int data3)
    {
        this.event = event;
        this.data1 = data1;
        this.data3 = data3;
    }

    public PODEvent(int event)
    {
        this(event, -1, -1);
    }

    public int getEvent()
    {
        return event;
    }

    public int getData1()
    {
        return data1;
    }

    public int getData3()
    {
        return data3;
    }
}
