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
package org.cablelabs.impl.ocap.hn;

import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.upnp.common.UPnPActionInvocation;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;

import org.apache.log4j.Logger;

/**
 * 
 * Abstract implementation of NetActionRequest. Receives and handles action
 * responses. Derived types are specific to action requests.
 * 
 * @author Dan Woodard
 * 
 * @version $Revision$
 * 
 * @see org.ocap.hn.NetActionRequest
 */
public class NetActionRequestImpl implements NetActionRequest
{
    private static final Logger log = Logger.getLogger(NetActionRequestImpl.class);

    protected NetActionHandler hnHandler;
    
    protected CallerContext callerContext;

    protected int error = -1;// default no error

    protected int actionStatus = NetActionEvent.ACTION_STATUS_NOT_AVAILABLE;

    // *TODO* - let's review this and see if it makes sense
    // Additional string which qualifies the action.  
    // This is needed in order to differentiate between browse action which
    // returns a ContentList and ContentServerNetModule.requestRootContainer() 
    // which returns a ContentContainer
    private String actionQualifier;
    
    /*
     * Default implementation for constructor.
     */
    public NetActionRequestImpl(NetActionHandler hnHandler)
    {
        this.hnHandler = hnHandler;
 
        // save the caller context to use for the handler.notify() call
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        callerContext = ccm.getCurrentContext();

    }

    /**
     * Constructs NetActionRequestImpl class with a action qualifier by calling
     * default constructor and setting an additional member attribute to indicate
     * special qualifier for associated UPnP Service action.
     * 
     * @param actionInvocation  associate this action invocation with this net action request
     * @param hnHandler         send notifications to this handler
     * @param actionQualifier   string which indicates additional processing required for this action.
     */
    protected NetActionRequestImpl(NetActionHandler hnHandler, String actionQualifier)
    {
        this(hnHandler);
        this.actionQualifier = actionQualifier;
    }

    /*
     * Default implementation returns false. Cancel() is not supported.
     * 
     * @see org.ocap.hn.NetActionRequest#cancel()
     */
    public boolean cancel()
    {
        return false;
    }

    /*
     * Returns current action status.
     * 
     * @see org.ocap.hn.NetActionRequest#getActionStatus()
     */
    public int getActionStatus()
    {
        return actionStatus;
    }
    
    /**
     * Updates the status of this action request with supplied value.
     * 
     * @param status    new status of request
     */
    public void setActionStatus(int status)
    {
        actionStatus = status;
    }

    /*
     * Returns error number when getActionStatus() returns
     * NetActionEvent.ACTION_FAILED.
     * 
     * @see org.ocap.hn.NetActionRequest#getError()
     */
    public int getError()
    {
        return error;
    }

    /**
     * Updates the error of this actioon request with supplied value.
     *  
     * @param errorCode new error code for this request
     */
    public void setError(int errorCode)
    {
        error = errorCode;
    }

    /*
     * Default implementation returns -1.0 indicating that the progress cannot
     * be determined.
     * 
     * @see org.ocap.hn.NetActionRequest#getProgress()
     */
    public float getProgress()
    {
        return (float) -1.0;
    }

    /**
     * Returns additional string which was supplied via constructor
     * which provides additional information about the action.  An
     * example is ContentServerNetModule.requestRootContainer().  It
     * uses a ContentDirectoryService.BROWSE but when returning results,
     * if browse was in response to above method, a ContentContainer instead
     * of a ContentList is to be returned.
     * 
     * @return  any associated action qualifier, may be null
     */
    public String getActionQualifier()
    {
        return actionQualifier;
    }
    
    public CallerContext getCallerContext()
    {
        return callerContext;
    }

    /**
     * Calls the NetActionHandler.notify() in the callers context.
     * 
     * @param event
     *            the NetActionEvent to callback with
     */
    public void notifyHandler(NetActionEvent event)
    {
        if (hnHandler != null)
        {
            final NetActionEvent finalEvent = event;

            callerContext.runInContextAsync(new Runnable()
            {
                public void run()
                {
                    hnHandler.notify(finalEvent);
                }
            });
        }
    }
}
