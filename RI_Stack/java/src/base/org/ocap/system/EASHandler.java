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

package org.ocap.system;

/**
 * An OCAP-J application can register an EASHandler to the EASModuleRegistrar
 * via the {@link EASModuleRegistrar#registerEASHandler} method. The
 * {@link #notifyPrivateDescriptor} of this class is called to notify a location
 * of an alternative audio for EAS representation. The OCAP-J application can
 * play an audio specified by a private descriptor.
 * 
 * @see EASModuleRegistrar
 * 
 * @author Shigeaki Watanabe
 */
public interface EASHandler
{
    /**
     * <p>
     * This is a call back method to notify a private descriptor in the
     * cable_emergency_alert message defined in [SCTE 18]. If the
     * alert_priority=15 but no audio specified by [SCTE 18] is available, the
     * OCAP implementation shall call this method. The OCAP-J application can
     * get a location of an alternative audio specified in the private
     * descriptor and play it according to [SCTE 18]. If the OCAP-J application
     * doesn't support the private descriptor, the
     * EAShandler.notifyPrivateDescriptor() method shall return false and the
     * OCAP implementation can play detailed channel or proprietary audio. This
     * method shall return immediately. The audio shall be played in a unique
     * thread not to prevent an alert text representation.
     * <p>
     * 
     * @return true if the OCAP-J application can sound an audio of the location
     *         of the specified descriptor.
     * 
     * @param descriptor
     *            an array of bytes of a private descriptor in the
     *            cable_emergency_alert message defined in [SCTE 18].
     * 
     * 
     */
    public boolean notifyPrivateDescriptor(byte[] descriptor);

    /**
     * This is a call back method to notify that the alert duration has
     * finished. The OCAP-J application stops an audio specified by a private
     * descriptor. The OCAP-J application shall not unregister the EASHandler
     * until terminating an audio by this method.
     */
    public void stopAudio();
}
