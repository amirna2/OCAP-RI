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

import java.io.File;
import java.util.Enumeration;

import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.MetadataNode;

import junit.framework.TestCase;

public class TrackingChangesOptionTest extends TestCase
{
    static final ContentDirectoryService CDS = MediaServer.getInstance().getCDS();
    static final ContentContainerImpl ROOT = CDS.getRootContainer();
    
    static final String UPNP_OBJECT_UPDATE_ID = "upnp:objectUpdateID";
    static final String UPNP_CONTAINER_UPDATE_ID = "upnp:containerUpdateID";
    static final String UPNP_TOTAL_DELETED_CHILD_COUNT = "upnp:totalDeletedChildCount";
    static final String ITEM_TITLE = "TrackingChangesOptionTest-item";
    static final String CONTAINER_TITLE = "TrackingChangesOptionTest-container";
    
    static final int BEFORE = 0;
    static final int AFTER = 1;
    
    public void testUpdate()
    {
        // Execute assertions in order
        update1();
        update2();
        update3();
        update4();
        update5();
        update6();
    }

    /**
     * Setup item, and check that upnp:objectUpdateID increments on metadata
     * modification, and that upnp:containerUpdateID increments for the parent
     * container.
     */
    public void update1()
    {
        final long itemObjectUpdateID[]       = new long[2];
        final long rootContainerUpdateID[]    = new long[2];
       
        CDS.createItem(ROOT, ITEM_TITLE, null, new File("Figaro.mp4"));

        ContentEntryImpl ce = getEntry(ROOT, ITEM_TITLE);
        MetadataNode mdn = ce.getRootMetadataNode();
        
        itemObjectUpdateID[BEFORE]      = Long.decode((String)mdn.getMetadata(UPNP_OBJECT_UPDATE_ID)).longValue();
        rootContainerUpdateID[BEFORE]   = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();
        
        mdn.addMetadata("flowers", "Spring Time");
        
        itemObjectUpdateID[AFTER]       = Long.decode((String)mdn.getMetadata(UPNP_OBJECT_UPDATE_ID)).longValue();
        rootContainerUpdateID[AFTER]    = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();
        
        assertTrue(itemObjectUpdateID[BEFORE] < itemObjectUpdateID[AFTER]);
        assertTrue(itemObjectUpdateID[AFTER] == CDS.getSystemUpdateID());        
        assertTrue(rootContainerUpdateID[BEFORE] < rootContainerUpdateID[AFTER]);
        assertTrue(rootContainerUpdateID[AFTER] == CDS.getSystemUpdateID());
        assertTrue(itemObjectUpdateID[AFTER] == CDS.getSystemUpdateID());
    }
    
    /**
     * Create a new container, and move the item from the previous test to this new
     * container.  Check that both container's upnp:containerUpdateID increment, as
     * well as the previous parent's upnp:totalDeletedChildCount.
     */
    public void update2()
    {
        final long rootContainerUpdateID[]    = new long[2];
        final long subContainerUpdateID[]     = new long[2];
        final long rootDeletedChildrenCount[] = new long[2];
        
        ContentEntryImpl ce = getEntry(ROOT, ITEM_TITLE);

        ContentContainerImpl newCC = (ContentContainerImpl)ROOT.getEntry((CDS.createContainer(ROOT, CONTAINER_TITLE, null, 
            ContentDirectoryService.CONTAINER_TYPE)).getID());
        subContainerUpdateID[BEFORE]        = Long.decode((String)newCC.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue(); 
        rootContainerUpdateID[BEFORE]       = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();
        rootDeletedChildrenCount[BEFORE]    = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_TOTAL_DELETED_CHILD_COUNT)).longValue();

        CDS.addEntry(newCC, ce);
        
        rootContainerUpdateID[AFTER]    = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();
        rootDeletedChildrenCount[AFTER] = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_TOTAL_DELETED_CHILD_COUNT)).longValue();
        subContainerUpdateID[AFTER]     = Long.decode((String)newCC.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();
        
        assertTrue(rootContainerUpdateID[AFTER] > rootContainerUpdateID[BEFORE]);
        assertTrue(rootDeletedChildrenCount[AFTER] > rootDeletedChildrenCount[BEFORE]);
        assertTrue(subContainerUpdateID[AFTER] > subContainerUpdateID[BEFORE]);
    }
    
    /**
     * Add metadata to test item.  Make sure the parent container updates upnp:containerUpdateID(), 
     * but check that its parent does not increment upnp:containerUpdateID().
     */
    public void update3()
    {
        final long rootContainerUpdateID[]    = new long[2];
        final long subContainerUpdateID[]     = new long[2];
        
        ContentContainerImpl newCC = (ContentContainerImpl)getEntry(ROOT, CONTAINER_TITLE);
        ContentEntryImpl ce = getEntry(newCC, ITEM_TITLE);
        MetadataNode mdn = ce.getRootMetadataNode();
        
        subContainerUpdateID[BEFORE]  = Long.decode((String)newCC.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue(); 
        rootContainerUpdateID[BEFORE] = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();
        
        mdn.addMetadata("snow", "Winter Time");
        
        subContainerUpdateID[AFTER] = Long.decode((String)newCC.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();       
        rootContainerUpdateID[AFTER] = Long.decode((String)ROOT.getRootMetadataNode().getMetadata(UPNP_CONTAINER_UPDATE_ID)).longValue();
        
        assertTrue(subContainerUpdateID[BEFORE] < subContainerUpdateID[AFTER]);
        assertTrue(rootContainerUpdateID[BEFORE] == rootContainerUpdateID[AFTER]);        
    }
    
    /**
     * Update the res value make sure the systemUpdateID and the item's res@updateCount increment.
     */
    public void update4()
    {
        final long itemResUpdateCount[] = new long[2];
        final long systemUpdateID[]     = new long[2];
        
        ContentContainerImpl newCC = (ContentContainerImpl)getEntry(ROOT, CONTAINER_TITLE);
        ContentEntryImpl ce = getEntry(newCC, ITEM_TITLE);
        MetadataNode mdn = ce.getRootMetadataNode();
        
        systemUpdateID[BEFORE] = CDS.getSystemUpdateID();
        itemResUpdateCount[BEFORE] = Long.decode(((String[])mdn.getMetadata("didl-lite:res@updateCount"))[0]).longValue();
        
        mdn.addMetadata("didl-lite:res", "http://fiddle.com/id=1");
        
        systemUpdateID[AFTER] = CDS.getSystemUpdateID();        
        itemResUpdateCount[AFTER] = Long.decode(((String[])mdn.getMetadata("didl-lite:res@updateCount"))[0]).longValue();
        
        assertTrue(systemUpdateID[BEFORE] < systemUpdateID[AFTER]);
        assertTrue(itemResUpdateCount[BEFORE] == itemResUpdateCount[AFTER]);
    }
    
    /**
     * Update a res dependent property and make sure the systemUpdateID and the item's res@updateCount increment.
     */
    public void update5()
    {
        final long itemResUpdateCount[] = new long[2];
        final long systemUpdateID[]     = new long[2];
        
        ContentContainerImpl newCC = (ContentContainerImpl)getEntry(ROOT, CONTAINER_TITLE);
        ContentEntryImpl ce = getEntry(newCC, ITEM_TITLE);
        MetadataNode mdn = ce.getRootMetadataNode();
        
        systemUpdateID[BEFORE] = CDS.getSystemUpdateID();
        itemResUpdateCount[BEFORE] = Long.decode(((String[])mdn.getMetadata("didl-lite:res@updateCount"))[0]).longValue();
        
        mdn.addMetadata("didl-lite:res@protocolInfo", "http-get:*:*:mpg4");
        
        systemUpdateID[AFTER] = CDS.getSystemUpdateID();        
        itemResUpdateCount[AFTER] = Long.decode(((String[])mdn.getMetadata("didl-lite:res@updateCount"))[0]).longValue();
        
        assertTrue(systemUpdateID[BEFORE] < systemUpdateID[AFTER]);
        assertTrue(itemResUpdateCount[BEFORE] == itemResUpdateCount[AFTER]);
    }
    
    /**
     * Update a res dependent property to an existing value.  Make sure that none of the counters increment.
     */
    public void update6()
    {
        final long itemResUpdateCount[] = new long[2];
        final long itemObjectUpdateID[] = new long[2];
        final long systemUpdateID[]     = new long[2];
        
        ContentContainerImpl newCC = (ContentContainerImpl)getEntry(ROOT, CONTAINER_TITLE);
        ContentEntryImpl ce = getEntry(newCC, ITEM_TITLE);
        MetadataNode mdn = ce.getRootMetadataNode();
        
        systemUpdateID[BEFORE] = CDS.getSystemUpdateID();
        itemResUpdateCount[BEFORE] = Long.decode(((String[])mdn.getMetadata("didl-lite:res@updateCount"))[0]).longValue();
        itemObjectUpdateID[BEFORE] = Long.decode((String)mdn.getMetadata(UPNP_OBJECT_UPDATE_ID)).longValue();
        
        
        mdn.addMetadata("didl-lite:res@protocolInfo", "http-get:*:*:mpg4");
        
        systemUpdateID[AFTER] = CDS.getSystemUpdateID();        
        itemResUpdateCount[AFTER] = Long.decode(((String[])mdn.getMetadata("didl-lite:res@updateCount"))[0]).longValue();
        itemObjectUpdateID[AFTER] = Long.decode((String)mdn.getMetadata(UPNP_OBJECT_UPDATE_ID)).longValue();
        
        
        assertEquals(systemUpdateID[BEFORE], systemUpdateID[AFTER]);
        assertEquals(itemResUpdateCount[BEFORE], itemResUpdateCount[AFTER]);
        assertEquals(itemObjectUpdateID[BEFORE], itemObjectUpdateID[AFTER]);
    }    
    
    private ContentEntryImpl getEntry(ContentContainer cc, String title)
    {
        ContentEntryImpl ce = null;
        for(Enumeration e = cc.getEntries(); e.hasMoreElements();)
        {
            ce = (ContentEntryImpl)e.nextElement();
            if(title.equals((String)ce.getRootMetadataNode().getMetadata("dc:title")))
            {
                return ce;
            }
        }
        return null;
    }
}
