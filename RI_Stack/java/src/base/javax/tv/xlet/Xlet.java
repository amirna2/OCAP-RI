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

package javax.tv.xlet;

/**
 * This interface allows an application manager to create, initialize, start,
 * pause, and destroy an Xlet. An Xlet is an application or service designed to
 * be run and controlled by an application manager via this lifecycle interface.
 * The lifecycle states allow the application manager to manage the activities
 * of multiple Xlets within a runtime environment by selecting which Xlets are
 * active at a given time. The application manager maintains the state of the
 * Xlet and calls the lifecycle methods of the <code>Xlet</code> interface. The
 * Xlet implements these methods to update its internal activities and resource
 * usage as directed by the application manager. The <code>Xlet</code> interface
 * methods signal state changes in the application lifecycle, so they should be
 * implemented to return quickly. A state transition is not complete until the
 * state change method has returned.
 * <p>
 * The Xlet can initiate some state changes itself and informs the application
 * manager of those state changes by invoking methods on
 * <code>XletContext</code>.
 * <p>
 * In order to support interoperability between Xlets and application managers,
 * all Xlet classes must provide a public no-argument constructor.
 * <p>
 * The application manager directs the lifecycle of Xlets in light of their
 * access to any scarce resources that must be shared within the system, such as
 * as media players, tuners and section filters. In some systems, screen real
 * estate may also be considered such a shared resource. Xlets are responsible
 * for minimizing their use of shared resources while in any state other than
 * the <em>active</em> state. An Xlet that holds shared resources improperly is
 * eligible for forceful termination by the application manager.
 * 
 * @see XletContext
 */
public interface Xlet
{

    /**
     * Signals the Xlet to initialize itself and enter the <i>Paused</i> state.
     * The Xlet shall initialize itself in preparation for providing service. It
     * should not hold shared resources but should be prepared to provide
     * service in a reasonable amount of time.
     * <p>
     * After this method returns successfully, the Xlet is in the <i>Paused</i>
     * state and should be quiescent.
     * <p>
     * <b>Note:</b> This method is called only once.
     * <p>
     * 
     * @param ctx
     *            The <code>XletContext</code> for use by this Xlet, by which it
     *            may obtain initialization arguments and runtime properties.
     * 
     * @exception XletStateChangeException
     *                If the Xlet cannot be initialized.
     * 
     * @see javax.tv.xlet.XletContext#ARGS
     * @see javax.tv.xlet.XletContext#getXletProperty
     */

    public void initXlet(XletContext ctx) throws XletStateChangeException;

    /**
     * Signals the Xlet to start providing service and enter the <i>Active</i>
     * state. In the <i>Active</I> state the Xlet may hold shared resources. The
     * method will only be called when the Xlet is in the <i>paused</i> state.
     * <p>
     * 
     * @exception XletStateChangeException
     *                is thrown if the Xlet cannot start providing service.
     */

    /*
     * Two kinds of failures can prevent the service from starting, transient
     * and non-transient. For transient failures the
     * <code>XletStateChangeException</code> exception should be thrown. For
     * non-transient failures the <code>XletContext.notifyDestroyed</code>
     * method should be called.
     * 
     * @see XletContext#notifyDestroyed
     */
    public void startXlet() throws XletStateChangeException;

    /**
     * Signals the Xlet to stop providing service and enter the <i>Paused</i>
     * state. In the <i>Paused</i> state the Xlet must stop providing service,
     * and should release all shared resources and become quiescent. This method
     * will only be called called when the Xlet is in the <i>Active</i> state.
     * 
     */
    public void pauseXlet();

    /**
     * Signals the Xlet to terminate and enter the <i>Destroyed</i> state. In
     * the destroyed state the Xlet must release all resources and save any
     * persistent state. This method may be called from the <i>Loaded</i>,
     * <i>Paused</i> or <i>Active</i> states.
     * <p>
     * Xlets should perform any operations required before being terminated,
     * such as releasing resources or saving preferences or state.
     * <p>
     * 
     * <b>NOTE:</b> The Xlet can request that it not enter the <i>Destroyed</i>
     * state by throwing an <code>XletStateChangeException</code>. This is only
     * a valid response if the <code>unconditional</code> flag is set to
     * <code>false</code>. If it is <code>true</code> the Xlet is assumed to be
     * in the <i>Destroyed</i> state regardless of how this method terminates.
     * If it is not an unconditional request, the Xlet can signify that it
     * wishes to stay in its current state by throwing the Exception. This
     * request may be honored and the <code>destroyXlet()</code> method called
     * again at a later time.
     * 
     * 
     * @param unconditional
     *            If <code>unconditional</code> is true when this method is
     *            called, requests by the Xlet to not enter the destroyed state
     *            will be ignored.
     * 
     * @exception XletStateChangeException
     *                is thrown if the Xlet wishes to continue to execute (Not
     *                enter the <i>Destroyed</i> state). This exception is
     *                ignored if <code>unconditional</code> is equal to
     *                <code>true</code>.
     * 
     * 
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException;
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
