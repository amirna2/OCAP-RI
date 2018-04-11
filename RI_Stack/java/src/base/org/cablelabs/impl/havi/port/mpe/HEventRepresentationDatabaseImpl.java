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

package org.cablelabs.impl.havi.port.mpe;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import org.havi.ui.event.HEventRepresentation;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.impl.havi.HEventRepresentationDatabase;
import org.cablelabs.impl.havi.HEventRepresentationImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This is the default implementation of
 * <code>HEventRepresentationDatabase</code> for the CableLabs OCAP stack.
 * <code>HEventRepresentation</code>s are read from a properties file specified
 * by the <em>OCAP.havi.eventRepresentationsFile<em>
 * property.  The property file is a resource of <code>HEventRepresentationDatabase</code>.
 * <p>
 * A valid event representation property file must have an entry (or entries) for
 * each mandatory OCAP key code as described in OCAP I16 Section 25.2.1.2.  An
 * example property file would look like this:
 * <p> <p>
 * <code>
 * VK_EXIT.color=BLUE
 * VK_EXIT.symbol=Images/exit.jpg
 * VK_EXIT.string=Exit
 * VK_POWER.color=RED
 * VK_POWER.symbol=Images/power.jpg
 * VK_POWER.string=Power
 * VK_REWIND.color=BLACK
 * VK_REWIND.symbol=Images/rwd.jpg
 * VK_REWIND.string=REW
 * VK_COLORED_KEY_3.supported=false
 * VK_NEXT_FAVORITE_CHANNEL.supported=false
 * .....
 * </code>
 * <hr>
 * <b>HEventRepresentation Supported</b>
 * <p> <p>
 * Not all remote control devices will contain all of the keys described in
 * OCAP I16 Section 25.2.1.2.  For those keys not supported by a particular
 * device (in the example file excerpt above, VK_COLORED_KEY_3 and
 * VK_NEXT_FAVORITE_CHANNEL) you can add an entry indicating lack of support.
 * For supported keys, it is not necessary to specify a <em>supported</em>
 * property entry in the file, simply specify some combination of
 * <em>string</em>, <em>color</em>, and <em>symbol</em>.
 * <p>
 * <p>
 * <b>HEventRepresentation String</b>
 * <p>
 * <p>
 * Event strings are required attributes for all supported events. Failure to
 * supply an event string will result in failure to create an event
 * representation for that event.
 * <p>
 * <p>
 * <b>HEventRepresentation Color</b>
 * <p>
 * <p>
 * Event colors are specified either by java.awt.Color constant or RGB formatted
 * hexadecimal integer prefaced with "0x". For example, both of these entries
 * describe the same event representation:
 * <p>
 * <p>
 * <code>
 * VK_POWER.color=red
 * VK_POWER.symbol=/Images/power.jpg
 * VK_POWER.string=Power
 * </code>
 * <p>
 * <code>
 * VK_POWER.color=0xFF0000
 * VK_POWER.symbol=/Images/power.jpg
 * VK_POWER.string=Power
 * </code>
 * <p>
 * Color attributes are only required for the VK_COLORED_KEY_? events. For all
 * other events, color is optional
 * <p>
 * <p>
 * <b>HEventRepresentation Symbol</b>
 * <p>
 * <p>
 * Event symbols are graphical representation of a particular remote control
 * key. Symbols are specified as java class resources of
 * <code>HEventRepresenationDatabase</code>. Symbol attributes are optional for
 * all event representations.
 * 
 * @author Greg Rutz
 */
public class HEventRepresentationDatabaseImpl implements HEventRepresentationDatabase
{
    /**
     * Construct a <code>HEventRepresentationDatabase</code> implementation that
     * reads event representation information from a properties file.
     */
    public HEventRepresentationDatabaseImpl()
    {
        // Determine our property file name
        final String propertyFile = MPEEnv.getEnv(EVENT_PROPERTY_FILE, DEFAULT_PROPERTY_FILE);

        // Parse the properties file
        final Properties props = new Properties();
        try
        {
            Exception ex = (Exception) AccessController.doPrivileged(new PrivilegedAction()
            {

                public Object run()
                {
                    try
                    {
                        props.load(super.getClass().getResourceAsStream(propertyFile));
                    }
                    catch (Exception e)
                    {
                        return e;
                    }
                    return null;
                }

            });
            if (ex != null) throw ex;
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError("Unable to load HEventRepresentationPropertyFile (" + propertyFile
                    + ")", e);
            return;
        }

        // Build event representations from the property data
        Hashtable parsedEvents = new Hashtable();
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();)
        {
            String propName = (String) e.nextElement();

            // Grab the event class field name up to the '.' char
            String fieldName = propName.substring(0, propName.indexOf('.'));

            // Find the keycode corresponding to the given field name
            int keyCode;
            try
            {
                keyCode = OCRcEvent.class.getField(fieldName).getInt(null);
            }
            catch (Exception ex)
            {
                SystemEventUtil.logRecoverableError("Error identifying event field name (" + fieldName + ")", ex);
                continue;
            }

            // Add the appropriate field to our event representation
            EventRepresentation er = (EventRepresentation) parsedEvents.get(new Integer(keyCode));
            if (er == null) er = new EventRepresentation();
            String eventAttribute = propName.substring(propName.indexOf('.') + 1);
            String propValue = props.getProperty(propName);

            // Does this attribute represent event color?
            if (eventAttribute.equals(EVENT_ATTRIBUTE_COLOR))
            {
                // 24-bit RGB color value provided as a hex integer
                if (propValue.substring(0, 2).equals("0x"))
                {
                    er.color = new Color(Integer.parseInt(propValue.substring(2), 16));
                    parsedEvents.put(new Integer(keyCode), er);
                }
                // Static color definition in java.awt.Color
                else
                {
                    try
                    {
                        Field f = Color.class.getField(propValue);
                        er.color = (Color) f.get(null);
                        parsedEvents.put(new Integer(keyCode), er);
                    }
                    catch (Exception ex)
                    {
                        SystemEventUtil.logRecoverableError("Unknown event color for " + fieldName, ex);
                    }
                }
            }
            // Does this attribute represent event name?
            else if (eventAttribute.equals(EVENT_ATTRIBUTE_NAME))
            {
                er.string = propValue;
                parsedEvents.put(new Integer(keyCode), er);
            }
            // Does this attribute represent event symbol?
            else if (eventAttribute.equals(EVENT_ATTRIBUTE_SYMBOL))
            {
                Toolkit tk = Toolkit.getDefaultToolkit();
                java.net.URL url = super.getClass().getResource(propValue);
                Image img = (url == null) ? null : tk.createImage(url);
                if (img != null)
                {
                    er.symbol = img;
                    parsedEvents.put(new Integer(keyCode), er);
                }
            }
            // Is this event unsupported?
            else if (eventAttribute.equals(EVENT_ATTRIBUTE_SUPPORTED))
            {
                // If unsupported just add an "unsupported" HEventRepresentation
                // object to the database
                if (propValue.equalsIgnoreCase("false"))
                    database.put(new Integer(keyCode), new HEventRepresentationImpl());
            }
        }

        // Finally, add all of our valid, parsed events to the database
        for (Enumeration e = parsedEvents.keys(); e.hasMoreElements();)
        {
            Integer eventCode = (Integer) e.nextElement();
            EventRepresentation er = (EventRepresentation) parsedEvents.get(eventCode);

            // All supported eventCodes must have a string representation.
            if (er.string == null)
            {
                database.put(eventCode, new HEventRepresentationImpl());
            }
            // Additionally, the VK_COLORED_KEY_* keys must have a color
            // representation.
            else if ((eventCode.intValue() == OCRcEvent.VK_COLORED_KEY_0
                    || eventCode.intValue() == OCRcEvent.VK_COLORED_KEY_1
                    || eventCode.intValue() == OCRcEvent.VK_COLORED_KEY_2
                    || eventCode.intValue() == OCRcEvent.VK_COLORED_KEY_3
                    || eventCode.intValue() == OCRcEvent.VK_COLORED_KEY_4 || eventCode.intValue() == OCRcEvent.VK_COLORED_KEY_5)
                    && er.color == null)
            {
                database.put(eventCode, new HEventRepresentationImpl());
            }
            else
            {
                database.put(eventCode, new HEventRepresentationImpl(er.string, er.color, er.symbol));
            }
        }
    }

    // Description copied from interface
    public HEventRepresentation getEventRepresentation(int eventCode)
    {
        return (HEventRepresentation) database.get(new Integer(eventCode));
    }

    /**
     * Instances of this class are used to incrementally store information
     * specific to a particular event representation while we are parsing the
     * properties file
     * 
     * @author Greg Rutz
     */
    private class EventRepresentation
    {
        public String string = null;

        public Image symbol = null;

        public Color color = null;
    }

    private Hashtable database = new Hashtable();

    private final String EVENT_PROPERTY_FILE = "OCAP.havi.eventRepresentationsFile";

    private final String DEFAULT_PROPERTY_FILE = "/org/cablelabs/impl/havi/HEventRepresentations.properties";

    private final String EVENT_ATTRIBUTE_COLOR = "color";

    private final String EVENT_ATTRIBUTE_NAME = "string";

    private final String EVENT_ATTRIBUTE_SYMBOL = "symbol";

    private final String EVENT_ATTRIBUTE_SUPPORTED = "supported";
}
