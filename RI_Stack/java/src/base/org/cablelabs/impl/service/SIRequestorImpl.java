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

package org.cablelabs.impl.service;

import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIRetrievable;

import org.apache.log4j.Logger;

/**
 * A simple <code>SIRequestor</code> implementation.  Callers should use waitForCondition() instead of wait().
 * 
 * @author Todd Earles
 */
public class SIRequestorImpl implements SIRequestor
{
    private static final Logger log = Logger.getLogger(SIRequestorImpl.class);
    private boolean complete = false;

    // Description copied from SIRequestor
    public synchronized void notifyFailure(SIRequestFailureType reason)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyFailure - reason: " + reason);
        }
        this.reason = reason;
        complete = true;
        notify();
    }

    public synchronized void waitForCompletion()
    {
        while (!complete)
        {
            try
            {
                wait();
                if (!complete)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("waitforCompletion - woken up but not complete: " + this);
                    }
                }
            }
            catch (InterruptedException e)
            {
                //ignore
            }
        }
    }

    // Description copied from SIRequestor
    public synchronized void notifySuccess(SIRetrievable[] result)
    {
        this.result = result;
        complete = true;
        notify();
    }

    /**
     * Return the <code>SIRetrievable</code> objects which represent the results
     * of this asynchronous operation.
     * 
     * @return The results as an array of SIRetrievable objects
     * @throws SIRequestException
     *             If the asynchronous request failed
     */
    public SIRetrievable[] getResults() throws SIRequestException
    {
		// Added for findbugs issues fix - added synchronized block
    	synchronized(this)
    	{
        if (reason == null)
            return result;
        else
            throw new SIRequestException(reason);
    	}   
    }

    /** The results of the asynchronous retrieval */
    private SIRetrievable[] result = null;

    /** The reason for the failure or null if the retrieval succeeded */
    private SIRequestFailureType reason = null;
}
