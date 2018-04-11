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

package org.cablelabs.impl.ocap.hn.content;

import java.util.Comparator;

import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.ocap.hn.content.ContentEntry;

public class ContentEntryComparator implements Comparator
{
    private String sortCriteria = null;

    public ContentEntryComparator(String sortCriteria)
    {
        this.sortCriteria = sortCriteria;
    }

    public int compare(Object arg0, Object arg1)
    {
        if (sortCriteria == null 
                || sortCriteria.length() == 0 
                || !(arg0 instanceof ContentEntry) 
                || !(arg1 instanceof ContentEntry)
                || ((ContentEntry) arg0).getRootMetadataNode() == null
                || ((ContentEntry) arg1).getRootMetadataNode() == null)
        {
            return 0;
        }

        int result = 0;
        boolean ascending = true;
        
        String[] criteria = Utils.split(sortCriteria, ",");

        for(int i = 0; i < criteria.length; i++)
        {
            String key = criteria[i];

            if(key == null)
            {
                return 0;
            }

            // Only supporting + / - as sort modifiers
            if(key.startsWith("+"))
            {
                ascending = true;
            }
            else if(key.startsWith("-"))
            {
                ascending = false;
            }
            else
            {
                // Modifier required to sort
                return 0;
            }
            
            // Strip off sort modifier / add default namespace if required.
            key = DIDLLite.prefixed(key.substring(1, key.length()));
            
            Object obj1 = ((ContentEntry) arg0).getRootMetadataNode().getMetadata(key);
            Object obj2 = ((ContentEntry) arg1).getRootMetadataNode().getMetadata(key);

            if(obj1 instanceof Comparable && obj2 instanceof Comparable)
            {
                result = ((Comparable) obj1).compareTo(obj2);
                result = result * (ascending ? 1 : -1);
                if (result != 0 || sortCriteria.length() == 0)
                {
                    break;
                }
            }
        }

        return result;        
    }
}
