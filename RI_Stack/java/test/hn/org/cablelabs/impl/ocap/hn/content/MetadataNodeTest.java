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

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.cablelabs.impl.ocap.hn.TestUtils;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.QualifiedName;
import org.dvb.application.AppID;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.storage.ExtendedFileAccessPermissions;

public class MetadataNodeTest extends TestCase
{
    private static final String NS = "http://a.a.org";
    private static final QualifiedName STRING   = new QualifiedName(NS, "stringObject");
    private static final QualifiedName BOOLEAN  = new QualifiedName(NS, "booleanObject");
    private static final QualifiedName INTEGER  = new QualifiedName(NS, "integerObject");
    private static final QualifiedName DATE     = new QualifiedName(NS, "dateObject");
    private static final QualifiedName LONG     = new QualifiedName(NS, "longObject");
    private static final QualifiedName DURATION = new QualifiedName(NS, "durationObject");
    private static final QualifiedName APPID    = new QualifiedName(NS, "appIDObject");
    private static final QualifiedName PERMS    = new QualifiedName(NS, "accessPermsObject");

    private static final QualifiedName STRING_ARRAY = new QualifiedName(NS, "stringArrayObject");

    static
    {
        registerValueWrapper(STRING,   false, StringWrapper.class);
        registerValueWrapper(BOOLEAN,  false, BooleanWrapper.class);
        registerValueWrapper(INTEGER,  false, IntegerWrapper.class);
        registerValueWrapper(DATE,     false, DateWrapper.class);
        registerValueWrapper(LONG,     false, LongWrapper.class);
        registerValueWrapper(DURATION, false, DateScheduledDurationWrapper.class);
        registerValueWrapper(APPID,    false, AppIDWrapper.class);
        registerValueWrapper(PERMS,    false, AccessPermissionsWrapper.class);

        registerValueWrapper(STRING_ARRAY, true, StringWrapper.class);
    }

    private static final QualifiedName SERIAL   = new QualifiedName(NS, "serializedObject");

    private static final Comparator GET_METADATA_ERROR_COMPARATOR = new GetMetadataErrorComparator();

    private static final String H1 = "Key";
    private static final String H2 = "Expected Class of Return Value";
    private static final String H3 = "Actual Class of Return Value";

    private static final String PAD = "  ";

    public void testAddMetadataNamespace()
    {
        MetadataNode node = MetadataNode.createMetadataNode();
        try
        {
            node.addMetadata("my:myspace", "my value");
            fail("Should have thrown IllegalArgumentException.");
        }
        catch (IllegalArgumentException ex)
        {
        }
    }

    public void testAddMetadataToContainer()
    {
        ContentContainer cc = TestUtils.getRootContainer();
        cc.createContentContainer("testCreateContainer-addMetadata", new ExtendedFileAccessPermissions(true, true,
                true, true, true, true, null, null));

        cc.getRootMetadataNode().addMetadata("dc:date", "2010-01-01");
        Calendar cal = new GregorianCalendar();
        cal.setTime(cc.getCreationDate());
        assertTrue(cal.get(Calendar.MONTH) == 0);
        assertTrue(cal.get(Calendar.DAY_OF_MONTH) == 1);
        assertTrue(cal.get(Calendar.YEAR) == 2010);
    }

    /**
     * Test the required getMetadata return value classes. This test is drawn from requirements in the
     * UPnPConstants, RecordingContentItem, and NetRecordingEntry javadocs (aka the HNEXT spec).
     * <p>
     * NOTE: Constants marked 'UNSPEC' in the below do not have explicit return value class requirements
     *       in the HNEXT spec. This is meant to imply that String is required, according to section 6.3.6.2
     *       of the HNP spec.
     * <p>
     * NOTE: This test flagrantly ignores a fundamental question, namely, is the 'value' argument to the
     *       addMetadata methods subject to the same requirements?
     */
    public void testGetMetadataRequiredReturnTypes()
    {
        List errors = new ArrayList();

        // UPnPConstants

        testGetMetadataRequiredReturnType(errors, UPnPConstants.ACTOR,                "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ACTOR_AT_ROLE,        "Seymour",    String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ALBUM,                "Buffy",      String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ALBUM_ART,             new String[]
                                                                                     {"http://xx",
                                                                                      "http://yy"}, String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ARTIST,               "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ARTIST_AT_ROLE,       "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ARTIST_DISCOGRAPHY,   "http://xx",  String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.AUTHOR,                new String[]
                                                                                     {"Aristotle",
                                                                                      "Plato"},     String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.AUTHOR_AT_ROLE,       new String[]
                                                                                     {"Primary",
                                                                                      "Secondary"}, String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.CHANNEL_NAME,         "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.CHANNEL_NUMBER,       "4",          Integer.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.COMMENTS,              new String[]
                                                                                     {"Boo!",
                                                                                      "Hiss!"},     String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.CONTRIBUTOR,           new String[]
                                                                                     {"Getty",
                                                                                      "Jetty"},     String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.CREATION_DATE,        "2010-01-01", Date.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.CREATOR,              "God",        String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.DESCRIPTION,          "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.DIRECTOR,              new String[]
                                                                                     {"Scorsese",
                                                                                      "Allen"},     String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.DVD_REGION_CODE,      "42",         Integer.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.GENRE,                new String[]
                                                                                     {"Jazz",
                                                                                      "Techno"},    String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ICON_REF,             "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.ID,                   "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.LANGUAGE,             new String[]
                                                                                     {"en-US",
                                                                                      "en-GB"},     String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.LONG_DESCRIPTION,     "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.LYRICS_REF,            new String[]
                                                                                     {"http://xx",
                                                                                      "http://yy"}, String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.MEDIA_ID,             "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.PARENT_ID,            "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.PLAYLIST,              new String[]
                                                                                     {"MySongs",
                                                                                      "YourSongs"}, String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.PRODUCER,              new String[]
                                                                                     {"Zero",
                                                                                      "Frank"},     String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.PROP_STORAGE_FREE,    "42",         Long.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.PROP_STORAGE_TOTAL,   "42",         Long.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.PUBLISHER,             new String[]
                                                                                     {"Apple",
                                                                                      "MCI"},       String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.RADIO_BAND,           "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.RADIO_CALL_SIGN,      "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.RADIO_STATION_ID,     "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.RATING,               "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.REGION,               "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.RELATION,              new String[]
                                                                                     {"http://xx",
                                                                                      "http://yy"}, String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.RIGHTS,                new String[]
                                                                                     {"Slim",
                                                                                      "None"},      String[].class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.SCHEDULED_END_TIME,   "2010-01-01", Date.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.SCHEDULED_START_TIME, "2010-01-01", Date.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.STORAGE_MEDIUM,       "UNSPEC",     String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.TITLE,                "Title",      String.class);
        testGetMetadataRequiredReturnType(errors, UPnPConstants.TRACK_NUMBER,         "42",         Integer.class);

        // RecordingContentItem

        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_ACCESS_PERMISSIONS,  "w=,1,1,o=0x0,1,1,a=0x0,1,1,",  ExtendedFileAccessPermissions.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_APP_ID,              "567890",     AppID.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_CONTENT_URI,         "http://xx",  String.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_DESTINATION,         "42",         String.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_DURATION,            "42",         Integer.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_EXPIRATION_PERIOD,   "42",         Long.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_MEDIA_FIRST_TIME,    "42",         Long.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_MSO_CONTENT,         "true",       Boolean.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_NET_RECORDING_ENTRY, "nre42",      String.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_ORGANIZATION,        "IBM",        String.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_PRESENTATION_POINT,  "42",         Long.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_PRIORITY_FLAG,       "42",         Integer.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_RECORDING_STATE,     "42",         Integer.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_RETENTION_PRIORITY,  "42",         Integer.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_SOURCE_ID,           "42",         String.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_SOURCE_ID_TYPE,      "42",         String.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_SPACE_REQUIRED,      "42",         Long.class);
        testGetMetadataRequiredReturnType(errors, RecordingContentItem.PROP_START_TIME,          "2010-01-01", Date.class);

        // RecordingContentItem (backward compatible)

        testGetMetadataRequiredReturnType(errors, "ocap:scheduledStartDateTime",                 "2010-01-01", Date.class);

        // NetRecordingEntry

        testGetMetadataRequiredReturnType(errors, NetRecordingEntry.PROP_CDS_REFERENCE,          "cdsRef",  String.class);
        testGetMetadataRequiredReturnType(errors, NetRecordingEntry.PROP_RCI_LIST,               "rciList", String.class);
        testGetMetadataRequiredReturnType(errors, NetRecordingEntry.PROP_SCHEDULED_CDS_ENTRY_ID, "sCDSeID", String.class);

        if (! errors.isEmpty())
        {
            Collections.sort(errors, GET_METADATA_ERROR_COMPARATOR);

            int w1 = H1.length();
            int w2 = H2.length();
            int w3 = H3.length();

            for (Iterator i = errors.iterator(); i.hasNext(); )
            {
                GetMetadataError error = (GetMetadataError) i.next();

                String key = error.key();
                String classNameActual = error.classNameActual();
                String classNameExpected = error.classNameExpected();

                if (w1 < key.length())
                {
                    w1 = key.length();
                }

                if (w2 < classNameExpected.length())
                {
                    w2 = classNameExpected.length();
                }

                if (w3 < classNameActual.length())
                {
                    w3 = classNameActual.length();
                }
            }

            StringBuffer sb = new StringBuffer();

            sb.append("\n")
              .append("\n")
              .append(PAD)
              .append(leftJustified(w1, H1))
              .append(PAD)
              .append(leftJustified(w2, H2))
              .append(PAD)
              .append(leftJustified(w3, H3))
              .append("\n")
            ;

            for (Iterator i = errors.iterator(); i.hasNext(); )
            {
                GetMetadataError error = (GetMetadataError) i.next();

                String key = error.key();
                String classNameActual = error.classNameActual();
                String classNameExpected = error.classNameExpected();

                sb.append("\n")
                  .append(PAD)
                  .append(leftJustified(w1, key))
                  .append(PAD)
                  .append(leftJustified(w2, classNameExpected))
                  .append(PAD)
                  .append(leftJustified(w3, classNameActual))
                ;
            }

            sb.append("\n");

            fail(sb.toString());
        }
    }

    public void testMultivaluedMetadataInteractions()
    {
        Object returnValue;

        MetadataNode mn = MetadataNode.createMetadataNode();

        // add a single-valued dependent property; is the independent property added?
        mn.addMetadata("foo@bar", "hen one");
        returnValue = get(mn, "foo");
        assertEquals("", returnValue);

        // change the dependent property to multivalued; does the independent property change?
        mn.addMetadata("foo@bar", new String[] {"duck one", "duck two"});
        returnValue = get(mn, "foo");
        assertTrue(Arrays.equals(new String[] {"", null}, (String[]) returnValue));

        // change the independent property to single-valued; does the dependent property change?
        mn.addMetadata("foo", "xyzzy");
        returnValue = get(mn, "foo@bar");
        assertEquals("duck one", returnValue);

        // delete the independent property; is the dependent property deleted?
        mn.addMetadata("foo", null);
        returnValue = get(mn, "foo@bar");
        assertNull(returnValue);

        // change one dependent property; does another dependent property change?
        mn.addMetadata("goo", new Integer(15));
        mn.addMetadata("goo@ber_1", "goober_1");
        mn.addMetadata("goo@ber_2", "goober_2");

        returnValue = get(mn, "goo");
        assertEquals(new Integer(15), returnValue);
        returnValue = get(mn, "goo@ber_1");
        assertEquals("goober_1", returnValue);
        returnValue = get(mn, "goo@ber_2");
        assertEquals("goober_2", returnValue);

        mn.addMetadata("goo@ber_1", new String[] {"goober 1a", "goober 1b"});

        returnValue = get(mn, "goo");
        assertTrue(Arrays.equals(new Integer[] {new Integer(15), null}, (Integer[]) returnValue));
        returnValue = get(mn, "goo@ber_1");
        assertTrue(Arrays.equals(new String[] {"goober 1a", "goober 1b"}, (String[]) returnValue));
        returnValue = get(mn, "goo@ber_2");
        assertTrue(Arrays.equals(new String[] {"goober_2", null}, (String[]) returnValue));

        // pathological case: assigning array to single-valued property, with nulls
        mn.addMetadata("dc:date", new Date());
        mn.addMetadata("dc:date@contrived", "");
        mn.addMetadata("dc:date", new Date[] {null, null});
        mn.addMetadata("dc:date@contrived", new String[] {null, null});
        if (get(mn, "dc:date") == null)
        {
            assertNull(get(mn, "dc:date@contrived"));
        }
        else
        {
            assertNotNull(get(mn, "dc:date@contrived"));
        }

        // pathological case: assigning array to single-valued property, with nulls
        mn.addMetadata("dc:date", new String("2010-01-01"));
        mn.addMetadata("dc:date@contrived", "");
        mn.addMetadata("dc:date", new String[] {null, null});
        mn.addMetadata("dc:date@contrived", new String[] {null, null});
        if (get(mn, "dc:date") == null)
        {
            assertNull(get(mn, "dc:date@contrived"));
        }
        else
        {
            assertNotNull(get(mn, "dc:date@contrived"));
        }

        // pathological case: assigning array to multivalued property, with nulls
        mn.addMetadata("hoo", new Integer(3));
        mn.addMetadata("hoo@boy", "");
        mn.addMetadata("hoo", new Integer[] {null, null});
        mn.addMetadata("hoo@boy", new String[] {null, null});
        if (get(mn, "hoo") == null)
        {
            assertNull(get(mn, "hoo@boy"));
        }
        else
        {
            assertNotNull(get(mn, "hoo@boy"));
        }
    }

    private static Object get(MetadataNode mn, String key)
    {
//        return mn.getMetadata(key);
        return ((MetadataNodeImpl) mn).getSpecValueRegardless(key);
    }
    
    public void testAttributeEscaping()
    {
        String attribute = "didl-lite:foo@bar";
        String attributeWithXMLEntity = "CONTENTFORMAT=\"video/mpeg\"";
        
        MetadataNodeImpl mdn = new MetadataNodeImpl("0", "-1", "0", null);
        mdn.addMetadata("upnp:class", "object.item");
        
        mdn.addMetadata(attribute,  attributeWithXMLEntity);
        
        assertTrue(attributeWithXMLEntity.equals(mdn.getMetadata(attribute)));
        
        // Create a new metadata node from the DIDLLite document generated from the original
        System.out.println(DIDLLite.getView(new ContentItemImpl(mdn)));

        MetadataNodeImpl mdn2 = new MetadataNodeImpl(DIDLLite.getView(new ContentItemImpl(mdn)));
        
        assertTrue(attributeWithXMLEntity.equals(mdn2.getMetadata(attribute)));
    }

    public void testObjectMarshalling()
    {
        MetadataNodeImpl mdn = new MetadataNodeImpl("0", "-1", "0", null);
        mdn.addNameSpace("a", NS);

        String str = "Jack & Jill Test'n <String>";
        Boolean bool =     Boolean.TRUE;
        Integer intValue = new Integer(123000); // Used for duration too, to seconds
        Date date =        new GregorianCalendar(2010, 01, 20, 13, 24, 15).getTime();  // To seconds
        Long longValue =   new Long(123444);
        AppID appID =      new AppID(5,5);
        ExtendedFileAccessPermissions efap = new ExtendedFileAccessPermissions(true, true, true, true, true, true, null, null);

        MetadataNode embeddedNode =     MetadataNode.createMetadataNode();
        embeddedNode.addMetadata("key1", "value1");
        embeddedNode.addMetadata("key2", "value2");

        Map map =      new HashMap();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", new Integer(5));

        String[] arrayObject = {"foo", "bar"};

        mdn.addMetadata(UPnPConstants.UPNP_CLASS, "object.item");

        mdn.addMetadata(STRING,   str);
        mdn.addMetadata(BOOLEAN,  bool);
        mdn.addMetadata(INTEGER,  intValue);
        mdn.addMetadata(DATE,     date);
        mdn.addMetadata(LONG,     longValue);
        mdn.addMetadata(DURATION, intValue);
        mdn.addMetadata(APPID,    appID);
        mdn.addMetadata(PERMS,    efap);
        mdn.addMetadata("metadata1", embeddedNode);
        mdn.addMetadata(SERIAL, map);
        mdn.addMetadata(STRING_ARRAY, arrayObject);

        MetadataNode foo = (MetadataNode)mdn.getMetadata("metadata1");
        assertTrue(foo.equals(embeddedNode));

        // Create a new metadata node from the DIDLLite document generated from the original
        System.out.println(DIDLLite.getView(new ContentItemImpl(mdn)));

        MetadataNodeImpl mdn2 = new MetadataNodeImpl(DIDLLite.getView(new ContentItemImpl(mdn)));

        // Test results after reconstruction from DIDLLite
        assertTrue(str.equals(mdn2.getMetadata(STRING)));
        assertTrue(bool.equals(mdn2.getMetadata(BOOLEAN)));
        assertTrue(intValue.equals(mdn2.getMetadata(INTEGER)));
        assertTrue(date.equals(mdn2.getMetadata(DATE)));
        assertTrue(longValue.equals(mdn2.getMetadata(LONG)));
        assertTrue(intValue.equals(mdn2.getMetadata(DURATION)));
        assertTrue(appID.equals(mdn2.getMetadata(APPID)));

        ExtendedFileAccessPermissions efap2 = (ExtendedFileAccessPermissions)mdn2.getMetadata(PERMS);
        assertTrue(efap.hasReadWorldAccessRight() == efap2.hasReadWorldAccessRight());
        assertTrue(efap.hasReadOrganisationAccessRight() == efap2.hasReadOrganisationAccessRight());
        assertTrue(efap.hasReadApplicationAccessRight() == efap2.hasReadApplicationAccessRight());
        assertTrue(efap.hasWriteWorldAccessRight() == efap2.hasWriteWorldAccessRight());
        assertTrue(efap.hasWriteOrganisationAccessRight() == efap2.hasWriteOrganisationAccessRight());
        assertTrue(efap.hasWriteApplicationAccessRight() == efap2.hasWriteApplicationAccessRight());

        MetadataNode embeddedNode2 = (MetadataNode)mdn2.getMetadata("metadata1");
        assertTrue("value1".equals(embeddedNode2.getMetadata("key1")));
        assertTrue("value2".equals(embeddedNode2.getMetadata("key2")));

        Map map2 = (Map)mdn2.getMetadata(SERIAL);
        assertTrue("value1".equals(map2.get("key1")));
        assertTrue("value2".equals(map2.get("key2")));
        assertTrue(new Integer(5).equals(map2.get("key3")));

        assertTrue(Arrays.equals(arrayObject, (String[])mdn2.getMetadata(STRING_ARRAY)));
    }
    
    public void testStandardNamespaces()
    {
        HashMap map = new HashMap();
        map.put("av",        "urn:schemas-upnp-org:av:av");
        map.put("avs",       "urn:schemas-upnp-org:av:avs");
        map.put("avdt",      "urn:schemas-upnp-org:av:avdt");
        map.put("avt-event", "urn:schemas-upnp-org:metadata-1-0/AVT/");
        map.put("didl-lite", "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/");
        map.put("cds-event", "urn:schemas-upnp-org:av:cds-event");
        map.put("rcs-event", "urn:schemas-upnp-org:metadata-1-0/RCS/");
        map.put("srs",       "urn:schemas-upnp-org:av:srs");
        map.put("srs-event", "urn:schemas-upnp-org:av:srs-event");
        map.put("upnp",      "urn:schemas-upnp-org:metadata-1-0/upnp/");
        map.put("dc",        "http://purl.org/dc/elements/1.1/");
        map.put("xsd",       "http://www.w3.org/2001/XMLSchema");
        map.put("xsi",       "http://www.w3.org/2001/XMLSchema-instance");
        map.put("xml",       "http://www.w3.org/XML/1998/namespace");
        
        MetadataNodeImpl node = new MetadataNodeImpl("0", "-1", "0", null);
        node.addMetadata("upnp:class", "object.item");
        
        for(Iterator i = map.keySet().iterator(); i.hasNext();)
        {
            String prefix = (String)i.next();
            String key = prefix + ":" + "key";
            try 
            {
                node.addMetadata(key, "value");
                assertTrue(true);
            }
            catch(IllegalArgumentException ex)
            {
                System.out.println("Did not recognise prefix = " + prefix);
                System.out.println(ex);
                assertTrue(false);
            }
        }
        
        System.out.println(DIDLLite.getView(new ContentItemImpl(node)));
    }

    public void testSerializableStuff()
    {
        MetadataNode mn = MetadataNode.createMetadataNode();

        try
        {
            mn.addMetadata("foo", new ArrayList(Arrays.asList(new Object[0])));
        }
        catch (IllegalArgumentException e)
        {
            fail("Should not have thrown IllegalArgumentException.");
        }

        try
        {
            mn.addMetadata("foo", new ArrayList(Arrays.asList(new Object[1])));
        }
        catch (IllegalArgumentException e)
        {
            fail("Should not have thrown IllegalArgumentException.");
        }

        try
        {
            mn.addMetadata("foo", new ArrayList(Arrays.asList(new Object[] {new Object()})));
            fail("Should have thrown IllegalArgumentException.");
        }
        catch (IllegalArgumentException e)
        {
        }
    }
    
    public void testDifferentDependentNamespace()
    {
        MetadataNodeImpl mdn = new MetadataNodeImpl("0", "-1", "0", null);
        
        mdn.addMetadata("upnp:class", "object.item");

        mdn.addMetadata("didl-lite:res@dlna:cleartext", "testvalue");
        mdn.addMetadata("didl-lite:res", "http://host:port/media");
        
        // Create a new metadata node from the DIDLLite document generated from the original
        System.out.println(DIDLLite.getView(new ContentItemImpl(mdn)));

        MetadataNodeImpl mdn2 = new MetadataNodeImpl(DIDLLite.getView(new ContentItemImpl(mdn)));
        assertTrue("testvalue".equals(((String[])mdn2.getMetadata("didl-lite:res@dlna:cleartext"))[0]));
    }
    
    private String leftJustified(int width, String s)
    {
        int n = width - s.length();

        if (n == 0)
        {
            return s;
        }

        assert n > 0;

        StringBuffer sb = new StringBuffer(s);

        for (int i = 0; i < n; ++ i)
        {
            sb.append(' ');
        }

        return sb.toString();
    }

    private void testGetMetadataRequiredReturnType(List errors, String key, Object value, Class c)
    {
        MetadataNode node = MetadataNode.createMetadataNode();

        node.addMetadata(key, value);
        value = node.getMetadata(key);

        if (! c.isInstance(value))
        {
            errors.add(new GetMetadataError(key, value, c));
        }
    }

    private static void registerValueWrapper(QualifiedName qName, boolean multi, Class vwClass)
    {
        try
        {
            Method rvwMethod = MetadataNodeImpl.class.getDeclaredMethod("registerProperty", new Class[] {QualifiedName.class, boolean.class, Class.class});

            rvwMethod.setAccessible(true);
            rvwMethod.invoke(null, new Object[] {qName, Boolean.valueOf(multi), vwClass});
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }

    private static final class GetMetadataError
    {
        private final String key;
        private final String classNameActual;
        private final String classNameExpected;

        public GetMetadataError(String key, Object value, Class c)
        {
            this.key = key;
            this.classNameActual = value == null ? "<null>" : niceClassName(value.getClass());
            this.classNameExpected = niceClassName(c);
        }

        public String classNameActual()
        {
            return classNameActual;
        }

        public String classNameExpected()
        {
            return classNameExpected;
        }

        public String key()
        {
            return key;
        }

        private static String niceClassName(Class c)
        {
            String s = c.getName();

            return c.isArray() ? s.substring(2, s.length() - 1) + "[]" : s;
        }
    }

    private static final class GetMetadataErrorComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            GetMetadataError gme1 = (GetMetadataError) o1;
            GetMetadataError gme2 = (GetMetadataError) o2;

            return gme1.key().compareTo(gme2.key());
        }
    }
}
