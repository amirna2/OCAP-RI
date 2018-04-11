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
 * An interface that provides methods allowing an Xlet to discover information
 * about its environment. An XletContext is passed to an Xlet when the Xlet is
 * initialized. It provides an Xlet with a mechanism to retrieve properties, as
 * well as a way to signal internal state changes.
 * <p>
 * Critical resources (such as an Xlet's parent container and service context)
 * can be obtained by means of an <code>XletContext</code> instance. Therefore,
 * an Xlet's <code>XletContext</code> instance should only be accessible to
 * other code that is highly trusted.
 * 
 * @see javax.tv.xlet.Xlet
 * @see javax.tv.graphics.TVContainer
 * @see javax.tv.service.selection.ServiceContextFactory#getServiceContext
 */

public interface XletContext
{

    /**
     * The property key used to obtain initialization arguments for the Xlet.
     * The call <code>XletContext.getXletProperty(XletContext.ARGS)</code> will
     * return the arguments as an array of Strings. If there are no arguments,
     * then an array of length 0 will be returned.
     * 
     * @see #getXletProperty
     */
    public static final String ARGS = "javax.tv.xlet.args";

    /**
     * Used by an application to notify its manager that it has entered into the
     * <i>Destroyed</i> state. The application manager will not call the Xlet's
     * <code>destroy</code> method, and all resources held by the Xlet are
     * considered eligible for immediate reclamation. Before calling this
     * method, the Xlet must perform the same operations (clean up, releasing of
     * resources etc.) as it would in response to call to
     * {@link Xlet#destroyXlet}.
     * <p>
     * If this method is called during the execution of one of the {@link Xlet}
     * state transition methods, the Xlet will immediately transition into the
     * <i>Destroyed</i> state and the Xlet state transition method is considered
     * to have completed <em>unsuccesfully</em>.
     */
    public void notifyDestroyed();

    /**
     * Notifies the manager that the Xlet does not want to be active and has
     * entered the <i>Paused</i> state. Invoking this method will have no effect
     * if the Xlet is destroyed, or if it has not yet been started.
     * <p>
     * If an Xlet calls <code>notifyPaused()</code>, in the future it may
     * receive an <i>Xlet.startXlet()</i> call to request it to become active,
     * or an <i>Xlet.destroyXlet()</i> call to request it to destroy itself.
     * 
     */

    public void notifyPaused();

    /**
     * Provides an Xlet with a mechanism to retrieve named properties from the
     * XletContext.
     * 
     * @param key
     *            The name of the property.
     * 
     * @return A reference to an object representing the property.
     *         <code>null</code> is returned if no value is available for key.
     */
    public Object getXletProperty(String key);

    /**
     * Provides the Xlet with a mechanism to indicate that it is interested in
     * entering the <i>Active</i> state. Calls to this method can be used by an
     * application manager to determine which Xlets to move to <i>Active</i>
     * state. Any subsequent call to <code>Xlet.startXlet()</code> as a result
     * of this method will be made via a different thread than the one used to
     * call <code>resumeRequest()</code>.
     * 
     * @see Xlet#startXlet
     */

    public void resumeRequest();

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
