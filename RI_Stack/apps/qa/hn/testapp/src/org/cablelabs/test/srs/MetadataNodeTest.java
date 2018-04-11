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

package org.cablelabs.test.srs;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.cablelabs.test.Test;
import org.ocap.hn.content.MetadataIdentifiers;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingSpec;

/**
 * Tests the MetadataNode implementation
 * 
 * @author Dan Woodard
 * 
 */
public class MetadataNodeTest extends Test
{

    private static final String OCAP_APP_NAME_SPACE = "ocapApp";

    private static final String testName = "MetadataNodeTest";

    private boolean testResult = false;

    private boolean displayResult = true;

    private String testResultString = null;

    public MetadataNodeTest()
    {
        this.testLogger.setPrefix(testName);
    }

    public int clean()
    {
        return Test.TEST_STATUS_PASS;
    }

    public int execute()
    {

        this.testLogger.log(" Start");

        if (runTest1())
        {
            this.testResult = true;

            this.testLogger.log(" NetRecordingSpec test PASSED");
        }
        else
        {
            this.testResult = false;

            this.testLogger.log(" NetRecordingSpec test FAILED");
        }

        if (this.testResult) return Test.TEST_STATUS_PASS;

        return Test.TEST_STATUS_FAIL;
    }

    public int prepare()
    {
        return Test.TEST_STATUS_PASS;
    }

    public String getName()
    {
        return "srs.MetadataNodeTest";
    }

    public String getDescription()
    {
        return "this is the srs.MetadataNodeTest test";
    }

    private static String addOcapAppNameSpace(String key)
    {
        return OCAP_APP_NAME_SPACE + ":" + key;
    }

    /**
     * Tests MetadataNode
     * 
     * @return true for pass, false for fail
     */
    private boolean runTest1()
    {
        this.testLogger.log(" create NetRecordingSpec()");
        NetRecordingSpec spec = new NetRecordingSpec();// this will create an
                                                       // empty MetadataNode
        MetadataNode node = spec.getMetadata();

        if (node == null)
        {
            this.testLogger.log(" FAILED TO GET MetadataNode");
            return false;
        }
        this.testLogger.log(" got MetadataNode from NetRecordingSpec");

        this.testLogger.log(" create NetRecordingSpec(MetadataNode)");
        NetRecordingSpec spec2 = new NetRecordingSpec(node);// this will create
                                                            // an empty
                                                            // MetadataNode

        if (spec2.getMetadata() == node)
        {
            this.testLogger.log(" in NetRecordingSpec is the same instance as passed into constructor");
        }

        String key1 = "key1";
        String ocapAppkey2 = "ocapApp:key2";
        String ocapkey3 = "ocap:key3";
        String upnpkey4 = "upnp:key4";
        String key5 = "key5";

        Object badobj = new Object();
        String obj1 = "obj1";
        String obj2 = "obj2";
        String obj3 = "obj3";
        String obj4 = "obj4";

        MetadataNode node2 = node.createMetadataNode("inputkey");

        try
        {
            node.addMetadata(key1, badobj);
            this.testLogger.log(" ERROR: did not verify value checking with caught IllegalArgumentException;");
            return false;
        }
        catch (IllegalArgumentException e)
        {
            this.testLogger.log(" verified value checking with caught IllegalArgumentException;");
        }

        try
        {

            node2.addMetadata(ocapkey3, obj3);
            this.testLogger.log(" added key3");
            node2.addMetadata(upnpkey4, obj4);
            this.testLogger.log(" added key4");
            node.addMetadata(key1, obj1);
            this.testLogger.log(" added key1");
            node.addMetadata(ocapAppkey2, obj2);
            this.testLogger.log(" added key2");
            node.addMetadata(key5, node2);
            this.testLogger.log(" added key5");
            this.testLogger.log(" verified addMetadata;");
        }
        catch (IllegalArgumentException e1)
        {
            this.testLogger.log(" ERROR unexpected IllegalArgumentException " + e1);
            return false;
        }

        String[] keys = node2.getKeys();

        Set s = new HashSet();

        for (int i = 0; i < keys.length; i++)
        {
            this.testLogger.log(" adding node2 keys, key: " + keys[i]);
            s.add(keys[i]);
            if (s.contains(keys[i]))
                this.testLogger.log(" s.contains() the key");
            else
                this.testLogger.log(" fail s.contains()");
        }

        if (s.contains(ocapkey3) && s.contains(upnpkey4))
        {
            this.testLogger.log(" node2 contains the correct keys");
        }
        else
        {
            this.testLogger.log(" ERROR node2 does not contain the correct keys");
            return false;
        }

        keys = node.getKeys();

        s.clear();

        for (int i = 0; i < keys.length; i++)
        {
            this.testLogger.log(" node contains key: " + (String) keys[i]);
            s.add(keys[i]);
        }

        if (s.contains(addOcapAppNameSpace(key1)) && s.contains(ocapAppkey2) && s.contains(addOcapAppNameSpace(key5))
                && s.contains(ocapkey3) && s.contains(upnpkey4))
        {
            this.testLogger.log(" node contains the correct keys");
        }
        else
        {
            this.testLogger.log(" ERROR node does not contain the correct keys");
            return false;
        }

        if (node.getMetadata(addOcapAppNameSpace(key1)) == obj1 && node.getMetadata(ocapAppkey2) == obj2)
        {
            this.testLogger.log(" node contains the correct values");

        }
        else
        {
            this.testLogger.log(" ERROR node does not contain the correct values");
            return false;
        }

        Enumeration enumerator = node.getMetadata();

        if (!enumerator.hasMoreElements())
        {
            this.testLogger.log(" ERROR node does not contain any values");
            return false;
        }

        s.clear();

        while (enumerator.hasMoreElements())
        {
            this.testLogger.log(" adding to set from enumeration");
            s.add(enumerator.nextElement());
        }

        if (s.contains(obj1) && s.contains(obj2) && s.contains(obj3) && s.contains(obj4))
        {
            this.testLogger.log(" node enumerates the correct values");
        }
        else
        {
            this.testLogger.log(" ERROR node does not enumerate the correct values");
            return false;
        }

        return true;

    }
}
