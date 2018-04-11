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

/*
 * PODExtendedChannel.java
 */

package org.ocap.mpeg;

/**
 *
 * @author  shigeaki watanabe
 */

/**
 * <p>
 * This class represents an extended channel that provides access to private
 * section data flows. The extended channel is defined in the Host-POD Interface
 * Standard (SCTE 28). When this class is specified as the stream parameter of
 * the org.davic.mpeg.sections.SectionFilterGroup.attach(TransportStream,
 * ResourceClient, Object) method, the SectionFilterGroup is connected to the
 * extended channel, i.e., the filters in the SectionFilterGroup filter the
 * private section data via OOB. The extended channel flow to be opened is
 * specified by PID, when the
 * org.davic.mpeg.sections.SectionFilter.startFiltering() method is called.
 * </p>
 * <p>
 * The methods defined in the super class (org.davic.mpeg.TransportStream) shall
 * behave as follows:
 * <ul>
 * <li>The getTransportStreamId() method returns -1.
 * <li>The retrieveService(int serviceId) method returns null.
 * <li>The retrieveServices() method returns null.
 * </ul>
 */
public abstract class PODExtendedChannel extends org.davic.mpeg.TransportStream
{
    /**
     * OCAP applications SHALL NOT use this method - it is provided for internal
     * use by the OCAP implementation. The result of calling this method from an
     * application is undefined, and valid implementations MAY throw any Error
     * or RuntimeException.
     */
    protected PODExtendedChannel()
    {
    }

    /**
     * Gets a PODExtendedChannel instance. The implementation MAY return the
     * same instance each time, or it MAY return different (but functionally
     * identical) instances.
     * 
     * @return A PODExtendedChannel instance.
     */
    public static PODExtendedChannel getInstance()
    {
        return singleton;
    }

    /**
     * Singleton instance of PODExtendedChannel.
     */
    private static final PODExtendedChannel singleton = new PODExtendedChannel()
    {
        /**
         * @return -1
         */
        public int getTransportStreamId()
        {
            return -1;
        }

        /**
         * @param serviceId
         *            ignored
         * @return <code>null</code>
         */
        public org.davic.mpeg.Service retrieveService(int serviceId)
        {
            return null;
        }

        /**
         * @return <code>null</code>
         */
        public org.davic.mpeg.Service[] retrieveServices()
        {
            return null;
        }
    };
}
