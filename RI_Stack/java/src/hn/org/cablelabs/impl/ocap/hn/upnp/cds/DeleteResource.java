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

package org.cablelabs.impl.ocap.hn.upnp.cds;

import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;

/**
 * Delete Resource Action
 * 
 * @author Michael A. Jastad
 * @version $Revision$
 * 
 * @see
 */
public class DeleteResource extends UPnPActionInvocation
{
    // *TODO* - could this class be deleted?
    
    /** Action Resource URI key flag */
    private static final String RESOURCE_URI = "ResourceURI";

    private static final String URI_TAG = "?" + ContentDirectoryService.CONTENT_ID + "=";

    /**
     * Creates a new DeleteResource object.
     * 
     * @param action
     *            the action object received during the HTTP request
     */
    public DeleteResource(String[] argVals, UPnPAction action)
    {
        super(argVals, action);
    }

    /**
     * Returns the Resource Identifier to be deleted
     * 
     * @return A String representing the Resource Universal Identifier.
     */
    public String getResourceURI()
    {
        return getArgumentValue(DeleteResource.RESOURCE_URI);
    }

    /**
     * Returns the Resource Identifier to be deleted
     * 
     * @return An Integer representing the Resource ID.
     */
    public int getResourceID()
    {
        int resourceId = -1;

        String resUri = getResourceURI();
        String resId = "";

        int mark = resUri.indexOf(DeleteResource.URI_TAG);

        if (mark != -1)
        {
            resId = resUri.substring(mark + DeleteResource.URI_TAG.length());

            if (isDigit(resId))
            {
                resourceId = new Integer(resId).intValue();
            }
        }

        return resourceId;

    }

    /**
     * used to determine if a string value is a digit.
     * 
     * @param The
     *            string value to be verified.
     * 
     * @return True if the string value represents a digit.
     */
    private boolean isDigit(String arg)
    {
        boolean status = true;
        char[] charArray = arg.toCharArray();

        for (int i = 0; i < charArray.length; ++i)
        {
            if (Character.isDigit(charArray[i]))
            {
                continue;
            }
            else
            {
                status = false;
                break;
            }
        }

        return status;
    }
}
