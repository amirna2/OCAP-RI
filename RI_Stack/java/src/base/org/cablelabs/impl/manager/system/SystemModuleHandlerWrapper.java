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

package org.cablelabs.impl.manager.system;

import org.cablelabs.impl.manager.system.SystemModuleRegistrarImpl;
import org.ocap.system.SystemModuleHandler;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;

/**
 * System Module Handler Wrapper
 * 
 * Used to wrap installed SysteModuleHandler objects. This wrapper object will
 * contain the caller's context and the target user handler.
 * 
 */
public class SystemModuleHandlerWrapper implements org.ocap.system.SystemModuleHandler, CallbackData
{

    /**
     * SysteModuleHandlerWrapper
     * 
     * Constructor. This constructor is used for all of the SAS handlers.
     * 
     * @param sessionId
     *            is the session identifiery for the POD communication
     *            interface.
     * @param ctx
     *            is the caller's associated caller context.
     * @param handler
     *            is the target SystemModuleHandler
     */
    public SystemModuleHandlerWrapper(SystemModuleHandler handler, CallerContext ctx, int sessionId,
            byte[] privateHostAppID)
    {
        this.sessionId = sessionId; // Save the session identifier.
        this.ctx = ctx; // Save the associated caller context.
        this.handler = handler; // Save the SAS handler.
        this.hostAppID = privateHostAppID; // Save private host app identifier.

        // Add this wrapper to the caller context for release on app
        // termination.
        ctx.addCallbackData(this, this);
    }

    /**
     * SysteModuleHandlerWrapper
     * 
     * Constructor. This constructor is only used for the single MMI handler.
     * 
     * @param handler
     *            is the target SystemModuleHandler
     * @param ctx
     *            is the caller's associated caller context.
     */
    public SystemModuleHandlerWrapper(SystemModuleHandler handler, CallerContext ctx)
    {
        this.sessionId = -1; // MMI handler doesn't have a session Id at the
                             // java level.
        this.ctx = ctx; // Save the associated caller context.
        this.handler = handler; // Save the MMI handler.
    }

    /**
     * receiverAPDU() implementation It is basically a NOOP unless a
     * non-resident MMI handler is registered
     * 
     * @param apduTag
     *            is the APDU tag value
     * @param lengthField
     *            is the length field encoded within the APDU
     * @param apdu
     *            is the APDU image byte array
     */
    public void receiveAPDU(final int apduTag, final int lengthField, final byte[] apdu)
    {
        // Call associated handler.
        handler.receiveAPDU(apduTag, lengthField, apdu);

        return;
    }

    /**
     * sendAPDUFailed() implementation It is basically a NOOP unless a
     * non-resident MMI handler is registered
     * 
     * @param failedAPDU
     */
    public void sendAPDUFailed(final int apduTag, final byte[] failedAPDU)
    {
        // Call associated handler.
        handler.sendAPDUFailed(apduTag, failedAPDU);

        return;
    }

    /**
     * notifyUnregister() implementation It is basically a NOOP unless a
     * non-resident MMI handler is registered
     */
    public void notifyUnregister()
    {
        // This is no longer being used... cleanup
        ctx.removeCallbackData(this);

        // Call associated handler.
        handler.notifyUnregister();

        return;
    }

    /**
     * ready() implementation
     * 
     * @param systemModule
     *            New SystemModule object created during registration
     */
    public void ready(final org.ocap.system.SystemModule systemModule)
    {
        // Call associated handler.
        handler.ready(systemModule);

        return;
    }

    /**
     * getSessionId()
     * 
     * Accessor method for the SAS session identifier. The one and only MMI
     * handler will have (-1) as the session identifier.
     * 
     * @ return the session identifier.
     */
    public int getSessionId()
    {
        return sessionId;
    }

    /**
     * getHandler()
     * 
     * Accessor method for the SystemModuleHandler instance.
     * 
     * @return the associated SystemModuleHandler.
     */
    public SystemModuleHandler getHandler()
    {
        return handler;
    }

    /**
     * getContext()
     * 
     * Accessor method for the handler's caller context. Used to deliver APDU's
     * and notifications within the handler's application context.
     * 
     * @return the associated CallerContext.
     */
    public CallerContext getContext()
    {
        return ctx;
    }

    public byte[] getHostAppID()
    {
        return hostAppID;
    }

    /**
     * The following three methods implement the "CallbackData" interface which
     * is used to monitor the handler's associated application. If the
     * application happens to terminate prior to it unregistering the its
     * handler, the destroy method will allow us to automatically unregister the
     * handler and return the associated resources.
     */
    public void destroy(CallerContext ctx)
    {
        SystemModuleRegistrarImpl smRegImpl = (SystemModuleRegistrarImpl) org.ocap.system.SystemModuleRegistrar.getInstance();

        // Get the system module registrar and remove this handler.
        smRegImpl.removeHandler(this);
        ctx.removeCallbackData(this);
    }

    public void pause(CallerContext callerContext)
    {
    }

    public void active(CallerContext callerContext)
    {
    }

    // The SAS session identifier (native handle).
    int sessionId = 0;

    // The target user's handler.
    private SystemModuleHandler handler;

    // The caller's context to use when calling the handler.
    private CallerContext ctx;

    private byte[] hostAppID = null;
}
