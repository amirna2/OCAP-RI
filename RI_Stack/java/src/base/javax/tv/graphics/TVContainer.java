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

package javax.tv.graphics;

/**
 * A class that allows an Xlet to get the root container for its AWT components.
 * 
 * @version 1.13, 10/24/05
 */

public class TVContainer
{

    private TVContainer()
    {
    } // disallow construction

    /**
     * Get the parent container for an Xlet to put its AWT components in, if the
     * Xlet has a graphical representation. Xlets without a graphical
     * representation should never call this method. If the Xlet is the only
     * Xlet that is currently active to invoke this method, it will return an
     * instance of <code>java.awt.Container</code> that is initially invisible,
     * with an undefined size and position. If another Xlet that is currently
     * active has requested a root container (via this API, or some other
     * platform-specific API), this method may return null.
     * <p>
     * If this method is called multiple times for the same XletContext, the
     * same container will be returned each time.
     * <p>
     * The methods for setting the size and position of the xlet's root
     * container shall attempt to change the shape of the container, but they
     * may fail silently or make platform specific approximations. For example,
     * a request to change the shape of the root container might result in its
     * overlapping with another root container, and overlapping root containers
     * might not be supported by the hardware. An application that needs to
     * discover if a request to change the size or position has succeeded should
     * query the component for the result.
     * 
     * @param ctx
     *            The XletContext of the Xlet requesting the container.
     * 
     * @return An AWT <code>Container</code>, or <code>null</code> if no
     *         container is available.
     */
    public static java.awt.Container getRootContainer(javax.tv.xlet.XletContext ctx)
    {
        if (ctx == null) throw new NullPointerException();

        return (java.awt.Container) ctx.getXletProperty("javax.tv.xlet.container");
    }
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
