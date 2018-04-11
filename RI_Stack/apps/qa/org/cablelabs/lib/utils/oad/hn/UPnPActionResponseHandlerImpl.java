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

package org.cablelabs.lib.utils.oad.hn;

import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPGeneralErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;

//
// UPnPActionResponseHandlerUtil
//
// This inner class implements UPnPActionResponseHandler to act as a listener for 
// a asynchronous UPnP action responses to the OCAPAppDriver's invoke methods.
class UPnPActionResponseHandlerImpl implements UPnPActionResponseHandler
{
    private UPnPResponse upnpResponse = null;
    private boolean receivedUpnpResponse = false;
    private Object signal = new Object();
    private static final long TIMEOUT = 15000;
    private String responseDescription = "";
    private String[] argNames = {};
    private String[] argValues = {};
    
    /**
     * {@inheritDoc}
     */
    public void notifyUPnPActionResponse(UPnPResponse response) 
    {
        upnpResponse = response;
        receivedUpnpResponse = true;
        try
        {
            synchronized (this.signal)
            {
                this.signal.notifyAll();
            }
        }
        catch (Exception e)
        {
        }
    }
    
    /**
     * Waits for async response from an action invocation.  
     * 
     * @return true if got the response, false if not
     */
    protected boolean waitRequestResponse()
    {
        responseDescription = "Response still pending.";
        if (!this.receivedUpnpResponse)
        {
            try
            {
                synchronized (this.signal)
                {
                    this.signal.wait(TIMEOUT);
                }
            }
            catch (InterruptedException e)
            {
            }
        }
        if (!this.receivedUpnpResponse)
        {
            responseDescription = "Failed to get UpnpResponse within " + TIMEOUT + " milliseconds";
            return false;
        }
        if (upnpResponse instanceof UPnPGeneralErrorResponse) 
        {
            responseDescription = "Error code: " + ((UPnPGeneralErrorResponse)upnpResponse).getErrorCode();
            return false; 
        }
        if (upnpResponse instanceof UPnPErrorResponse) 
        {
            responseDescription = "Error code: " + ((UPnPErrorResponse)upnpResponse).getErrorCode() + " - "
                + ((UPnPErrorResponse)upnpResponse).getErrorDescription();
            return false; 
        }
        if (upnpResponse instanceof UPnPActionResponse) 
        {
            argNames = ((UPnPActionResponse)upnpResponse).getArgumentNames();
            argValues = ((UPnPActionResponse)upnpResponse).getArgumentValues();
        }
        responseDescription = "HTTP response code: " + upnpResponse.getHTTPResponseCode();
        
        this.receivedUpnpResponse = false;// reset for next request
        return true;
    }
    
    /**
     * Gets the argument names from UPnP response message.
     * 
     * @return a String[] object containing argument names in the <class>UPnPActionResponse</class>.
     */
    protected String[] getOutArgNames()
    {
        return argNames;
    }
    
    /**
     * Gets the argument values from UPnP response message.
     * 
     * @return a String[] object containing argument values in the <class>UPnPActionResponse</class>.
     */
    protected String[] getOutArgValues()
    {
        return argValues;
    }
    
    /**
     * Gets the response to the corresponding UPnP request.
     * 
     * @return the <class>UPnPResponse</class> description that will include the HTTP response code or 
     *         the UPnP Error code of the response. 
     */
    protected String getResponseDescription()
    {
        return responseDescription;
    }
}

