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

package org.cablelabs.impl.signalling;

/**
 * An implementation of this interface represents an eXtended Application
 * Information Table describing unbound applications. This application
 * signalling may have come from one of three potential sources:
 * <ul>
 * <li>network-based signalling (i.e., the out-of-band
 * <code>PODExtendedChannel</code>
 * <li>invocation of
 * {@link org.ocap.application.AppManagerProxy#registerUnboundApp}
 * <li>invocation of
 * {@link org.ocap.application.AppManagerProxy#unregisterUnboundApp}
 * </ul>
 * 
 * @see "OCAP-1.0: 11 Application Signalling"
 * @author Aaron Kamienski
 */
public interface Xait extends Ait
{
    /**
     * A {@link #getSource source} of <code>NETWORK_SIGNALLING</code> indicates
     * that this signalling was acquired via the out-of-band
     * <code>PODExtendedChannel</code>.
     * <p>
     * The data in this <code>Xait</code>should be considered complete. As such,
     * services that are no longer referenced should be removed.
     * <p>
     * An <code>Xait</code>from this source may add or update abstract services.
     * However, any modifications to applications originally signalled from a
     * source of {@link #REGISTER_UNBOUND_APP} should be ignored.
     */
    public static final int NETWORK_SIGNALLING = 0;

    /**
     * A {@link #getSource source} of <code>REGISTER_BOUND_APP</code> indicates
     * that this signalling was submitted by a privileged application using the
     * {@link org.ocap.application.AppManagerProxy#registerUnboundApp} method.
     * <p>
     * An <code>Xait</code> from this source may add or update abstract
     * services. However, any modifications to applications originally signalled
     * from a source of {@link #NETWORK_SIGNALLING} should be ignored.
     */
    public static final int REGISTER_UNBOUND_APP = 1;

    /**
     * A {@link #getSource source} of <code>HOST_DEVICE</code> indicates that
     * this signalling specifies <i>host-device manufacturer</i> applications.
     * In other words, <i>resident</i> applications.
     * <p>
     * An <code>Xait</code> from this source may only refer to abstract services
     * with a <i>serviceId</i> in the range of <code>0x010000 - 0x01FFFF</code>.
     */
    public static final int HOST_DEVICE = -1;

    /**
     * Retrieves the abstract services indicated by this <code>Xait</code> as
     * represented by <code>AppSignalling</code> objects. Each roughly
     * corresponds to an <i>abstract_service_descriptor</i> contained in the
     * outer common loop of the XAIT.
     * <p>
     * The applications signalled as part of each service can be retrieved
     * directly from the service objects.
     * 
     * @return abstract services specified in the XAIT
     */
    public AbstractServiceEntry[] getServices();

    /**
     * Indicates the source of this XAIT. The source can be one of
     * {@link #NETWORK_SIGNALLING} or {@link #REGISTER_UNBOUND_APP}.
     * 
     * @return one of {@link #NETWORK_SIGNALLING}, {@link #REGISTER_UNBOUND_APP}
     *         , or {@link #HOST_DEVICE}.
     */
    public int getSource();

    /**
     * Returns, as an array of bytes, the contents of the
     * <i>privileged_certificate_descriptor</i>. There is exactly one such entry
     * in the XAIT. It is up to the consumer of this data to extract the SHA-1
     * hashes (each is 20 bytes long) from this <code>byte[]</code> if
     * necessary.
     * 
     * @return the contents of the <i>privileged_certificate_descriptor</i> as a
     *         <code>byte[]</code>
     */
    public byte[] getPrivilegedCertificateBytes();
}
