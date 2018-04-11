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

package org.ocap.ui.event;

import java.util.EventListener;

/**
 * This listener is used to provide notifications regarding system and
 * application induced changes to a <code>MultiScreenContext</code>.
 * 
 * @author Glenn Adams
 * @since OCAP 1.0 I16 (ECN XXXX)
 **/
public interface MultiScreenContextListener extends EventListener
{
    /**
     * <p>
     * When an OCAP implementation makes any change to a
     * <code>MultiScreenContext</code> that causes generation of a
     * <code>MultiScreenContextEvent</code>, then the implementation SHALL
     * invoke this method on all registered listeners in order to report change
     * information to the listener.
     * </p>
     * 
     * <p>
     * If the application that registered this listener has not been granted
     * <code>MonitorAppPermission("multiscreen.context")</code> and the source
     * <code>MultiScreenContext</code> associated with the specified
     * <code>MultiScreenContextEvent</code> is associated with no
     * <code>ServiceContext</code> or a <code>ServiceContext</code> that is not
     * accessible to that application, then this method SHALL NOT be invoked on
     * this listener; otherwise it SHALL be invoked on this listener.
     * </p>
     * 
     * <p>
     * A <code>ServiceContext</code> is accessible to an application if it is
     * returned from the <code>ServiceContextFactory.getServiceContexts()</code>
     * method.
     * </p>
     * 
     * @param evt
     *            A <code>MultiScreenContextEvent</code> instance.
     * 
     * @since MSM I01
     **/
    public void notify(MultiScreenContextEvent evt);
}
