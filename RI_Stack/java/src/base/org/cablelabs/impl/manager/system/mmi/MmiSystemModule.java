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

package org.cablelabs.impl.manager.system.mmi;

import org.ocap.system.SystemModuleHandler;

/**
 * MmiSystemModule
 */
public class MmiSystemModule extends org.cablelabs.impl.manager.system.SystemModule
{
    /**
     * Constructor for the MmiSystemModule object
     * 
     */
    public MmiSystemModule(SystemModuleHandler handler)
    {
        // Call SystemModule constructor with valid tags for MMI.
        super(validTags);
        this.handler = handler;
    }

    /**
     * Implementation of the org.ocap.system.SystemModule sendAPDU() method
     * 
     * @param APDU
     */
    public void sendAPDU(int apduTag, byte[] apdu)
    {
        // Validate the specified tag.
        if (isValidTag(apduTag) == false) throw new IllegalArgumentException("Invalid APDU tag specified");

        // Validate connection status.
        connect();

        // Check for connection now and make native call to send APDU.
        if (connected && (true == podMMIAppInfoSendAPDU(apduTag, apdu)))
        {
            return; // Native send succeeded.
        }

        // Notify of send failure.
        handler.sendAPDUFailed(apduTag, apdu);
    }

    /**
     * connect()
     * 
     * Establishes the MMI connection to the POD device, which only needs to be
     * done once because the connection stays open indefinitely.
     */
    public synchronized void connect()
    {
        // If the MMI connection hasn't been made, make it now...
        if (connected == false)
        {
            // Attempt to establish MMI connection.
            connected = podMMIAppInfoConnect();
        }
    }

    /**
     * Native method for sending the MMI APDU to the POD
     */
    public native boolean podMMIAppInfoSendAPDU(int apduTag, byte[] apdu);

    /**
     * Native method to establish the MMI connection with the POD.
     */
    public native boolean podMMIAppInfoConnect();
    
    /**
     * Native method to retrieve the current Application Information resource version
     */
    public static native int getAppInfoResourceVersion();

    // List of valid MMI APDU and Application Info tags.
    private static int[] validTags = {
        MmiApduProtocol.APDU_TAG_OPEN_MMI_REQ,
        MmiApduProtocol.APDU_TAG_OPEN_MMI_CNF,
        MmiApduProtocol.APDU_TAG_CLOSE_MMI_REQ,
        MmiApduProtocol.APDU_TAG_CLOSE_MMI_CNF,
        MmiApduProtocol.APDU_TAG_APPLICATION_INFO_REQ,
        MmiApduProtocol.APDU_TAG_APPLICATION_INFO_CNF,
        MmiApduProtocol.APDU_TAG_SERVER_QUERY
    };

    // The associated SystemModuleHandler.
    private SystemModuleHandler handler = null;

    // MMI session ID, returned by podMMIConnect()
    private static boolean connected = false;

    static
    {
        // ensure that MPE/JNI native library is loaded
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
