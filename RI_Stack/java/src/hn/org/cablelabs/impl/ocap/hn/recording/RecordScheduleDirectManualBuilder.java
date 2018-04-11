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

package org.cablelabs.impl.ocap.hn.recording;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.MiniDomParser;
import org.cablelabs.impl.ocap.hn.util.xml.miniDom.Node;


/**
 * 
 * This class builds one or more RecordScheduleDirectManual instances an XML
 * document of type A_ARG_TYPE_RECORD_SCHEDULE_PARTS_USAGE or
 * A_ARG_TYPE_RECORD_SCHEDULE_USAGE. A_ARG_TYPE_RECORD_SCHEDULE_PARTS_USAGE will
 * only contain one instance. A_ARG_TYPE_RECORD_SCHEDULE_USAGE could contain
 * more than one instance.
 * 
 * @author Dan Woodard
 * 
 * @version $Revision$
 * 
 */
public class RecordScheduleDirectManualBuilder
{
    public static final int A_ARG_TYPE_RECORD_SCHEDULE_PARTS_USAGE = RecordScheduleDirectManual.RECORD_SCHEDULE_PARTS_USAGE;

    public static final int A_ARG_TYPE_RECORD_SCHEDULE_USAGE = RecordScheduleDirectManual.RECORD_SCHEDULE_USAGE;

    private static final RecordScheduleDirectManual[] EMPTY_RECORD_SCHEDULE_DIRECT_MANUAL_ARRAY = {};
    
    // Default is 0, means no validation error.
    public static final int NO_VALIDATION_ERROR_CODE = 0;
    
    // Error value when any of the property has a value that is not allowed.
    public static final int ERROR_PROPERTY_VALUE_NOT_ALLOWED = 1;
    
    // Error value when any of the mandatory property is missing
    public static final int ERROR_MISSING_MANDATORY_PROPERTY = 2;

    /**
     * Validates all the existence of mandatory properties and also checks for
     * existence of only allowed property values
     * 
     * @return int returns the error code based the type of validation failure
     */
    public static int validateRecordScheduleDirectManual(RecordScheduleDirectManual[] recordScheduleDirectManual)
    {
        for (int i = 0; i < recordScheduleDirectManual.length; i++)
        {
            RecordScheduleDirectManual rsdm = recordScheduleDirectManual[i];
            if (!rsdm.isValid())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("validateRecordScheduleDirectManual(), validation failed for one of the mandatory property missing.");
                }
                return ERROR_MISSING_MANDATORY_PROPERTY;
            }

            if (!rsdm.isAllowedPropertyValue())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("validateRecordScheduleDirectManual(), validation failed for property value that is not allowed.");
                }
                return ERROR_PROPERTY_VALUE_NOT_ALLOWED;
            }
        }
        return NO_VALIDATION_ERROR_CODE;
    }
    
    /**
     * Builds a RecordScheduleDirectManual instance from an input
     * A_ARG_TYPE_RecordScheduleParts or A_ARG_TYPE_RecordSchedule XML string.
     * 
     * @param elements
     *            The XML string to parse
     * @param usage
     * @return true if successfully built and valid, false if not.
     */
    public static RecordScheduleDirectManual[] build(String elements, int usage)
    {
        if (elements == null || elements.length() == 0)
        {
            throw new IllegalArgumentException("elements is null or empty");
        }

        if (usage != A_ARG_TYPE_RECORD_SCHEDULE_PARTS_USAGE && usage != A_ARG_TYPE_RECORD_SCHEDULE_USAGE)
        {
            throw new IllegalArgumentException("Bad usage parameter: usage=" + usage);
        }

        Node document = MiniDomParser.parse(elements);

        if (document != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("build(), MiniDomParser: parsed elements ok");
            }
        }
        else
        {
            if (log.isErrorEnabled())
            {
                log.error("build(), MiniDomParser: failed to parse " + elements);
            }
            return EMPTY_RECORD_SCHEDULE_DIRECT_MANUAL_ARRAY;
        }

        Vector builtItems = new Vector();

        if (usage == A_ARG_TYPE_RECORD_SCHEDULE_USAGE)
        {
            // TODO: iterate Dom document for multiple items and create a new
            // instance for each
            // for now just build the first one which may apply to all xml for
            // this project, TBD

            RecordScheduleDirectManual recordScheduleDirectManual = new RecordScheduleDirectManual(document, usage);

            builtItems.add(recordScheduleDirectManual); // save built instance
        }
        else
        // A_ARG_TYPE_RECORD_SCHEDULE_PARTS_USAGE will only have one instance
        {
            RecordScheduleDirectManual recordScheduleDirectManual = new RecordScheduleDirectManual(document, usage);

            builtItems.add(recordScheduleDirectManual); // save built instance
        }

        return toArray(builtItems);
    }

    private static final RecordScheduleDirectManual[] toArray(Vector v)
    {
        int n = v.size();

        return n == 0 ? EMPTY_RECORD_SCHEDULE_DIRECT_MANUAL_ARRAY
                : (RecordScheduleDirectManual[]) v.toArray(new RecordScheduleDirectManual[n]);
    }

    // Log4J logger.
    private static final Logger log = Logger.getLogger("RecordScheduleDirectManualBuilder");

    // static access only
    private RecordScheduleDirectManualBuilder()
    {
    }
}
