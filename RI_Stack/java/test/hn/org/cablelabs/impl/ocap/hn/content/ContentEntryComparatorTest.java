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

import java.util.Collections;

import org.cablelabs.impl.ocap.hn.content.navigation.ContentListImpl;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.ocap.hn.content.ContentEntry;

import junit.framework.TestCase;

public class ContentEntryComparatorTest extends TestCase
{
    public void testSingleCriteriaSorting()
    {
        MetadataNodeImpl mdn1 = new MetadataNodeImpl();
        MetadataNodeImpl mdn2 = new MetadataNodeImpl();
        MetadataNodeImpl mdn3 = new MetadataNodeImpl();
        
        mdn1.addMetadata(UPnPConstants.TITLE, "b");
        mdn2.addMetadata(UPnPConstants.TITLE, "z");
        mdn3.addMetadata(UPnPConstants.TITLE, "e");
        
        ContentListImpl cl = new ContentListImpl();
        
        cl.add(new ContentItemImpl(mdn1));
        cl.add(new ContentItemImpl(mdn2));
        cl.add(new ContentItemImpl(mdn3));
        
        Collections.sort(cl, new ContentEntryComparator("-" + UPnPConstants.TITLE));
        
        String[] descendingOrder = { "z", "e", "b" };
        for(int i = 0; i < descendingOrder.length; i++)
        {
            assertTrue(descendingOrder[i].equals(((ContentEntry)cl.get(i)).getRootMetadataNode().getMetadata(UPnPConstants.TITLE)));            
        }
        
        Collections.sort(cl, new ContentEntryComparator("+" + UPnPConstants.TITLE));
        
        String[] ascendingOrder = { "b", "e", "z" };
        for(int i = 0; i < ascendingOrder.length; i++)
        {
            assertTrue(ascendingOrder[i].equals(((ContentEntry)cl.get(i)).getRootMetadataNode().getMetadata(UPnPConstants.TITLE)));            
        }       
    }
    
    public void testMultipleCriteriaSorting()
    {
        MetadataNodeImpl mdn1 = new MetadataNodeImpl();
        MetadataNodeImpl mdn2 = new MetadataNodeImpl();
        MetadataNodeImpl mdn3 = new MetadataNodeImpl();
        
        mdn1.addMetadata(UPnPConstants.TITLE, "b");
        mdn2.addMetadata(UPnPConstants.TITLE, "b");
        mdn3.addMetadata(UPnPConstants.TITLE, "e");
        
        mdn1.addMetadata(UPnPConstants.UPNP_CLASS, "upnp:videoItem");
        mdn2.addMetadata(UPnPConstants.UPNP_CLASS, "upnp:audioItem");        
        mdn3.addMetadata(UPnPConstants.UPNP_CLASS, "upnp:container");
        
        ContentListImpl cl = new ContentListImpl();
        
        cl.add(new ContentItemImpl(mdn1));
        cl.add(new ContentItemImpl(mdn2));
        cl.add(new ContentItemImpl(mdn3));
        
        Collections.sort(cl, new ContentEntryComparator("-" + UPnPConstants.TITLE + ",-" + UPnPConstants.UPNP_CLASS));
        
        String[] descendingOrder = { "upnp:container", "upnp:videoItem", "upnp:audioItem" };
        for(int i = 0; i < descendingOrder.length; i++)
        {
            assertTrue(descendingOrder[i].equals(((ContentEntry)cl.get(i)).getRootMetadataNode().getMetadata(UPnPConstants.UPNP_CLASS)));            
        }
        Collections.sort(cl, new ContentEntryComparator("-" + UPnPConstants.TITLE + ",+" + UPnPConstants.UPNP_CLASS));
        
        String[] ascendingOrder = { "upnp:container", "upnp:audioItem", "upnp:videoItem" };
        for(int i = 0; i < ascendingOrder.length; i++)
        {
            assertTrue(ascendingOrder[i].equals(((ContentEntry)cl.get(i)).getRootMetadataNode().getMetadata(UPnPConstants.UPNP_CLASS)));            
        }
    }   
}
