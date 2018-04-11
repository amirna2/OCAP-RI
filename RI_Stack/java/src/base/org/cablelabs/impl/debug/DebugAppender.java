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

package org.cablelabs.impl.debug;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.helpers.LogLog;

/**
 * A Log4J Appender that outputs using the MPE tracing facility. The tracing
 * facility is exposed by the {@link org.cablelabs.impl.debug.Debug} class.
 * <p>
 * The implementation can be made to work with Log4J and Log4J/ME. It is
 * currently written to work with both, meaning it's written to the
 * least-common-denominator Log4J/ME.
 * 
 * @author Aaron Kamienski
 */
public class DebugAppender extends AppenderSkeleton
{
    public DebugAppender()
    {
        this(null);
    }

    public DebugAppender(Layout layout)
    {
        // this.layout = (layout == null) ? (new SimpleLayout()) : layout;
        this.layout = layout;
    }

    /**
     * Implements the <code>abstract</code> {@link AppenderSkeleton#append} to
     * log the given <code>event</code> to the native MPE logging facility using
     * {@link Debug#Msg}.
     * 
     * @param event
     */
    protected void append(LoggingEvent event)
    {
        if (this.closed)
        {
            LogLog.warn("Not allowed to write to a closed appender.");
            return;
        }

        Layout layout = getLayout();
        if (layout == null)
        {
            LogLog.error("No layout set for the appender named [" + name + "].");
            return;
        }

        Debug.Msg(layout.format(event), event.getLevel().toInt());

        if (layout.ignoresThrowable())
        {
            String[] s = event.getThrowableStrRep();
            if (s != null)
            {
                int len = s.length;
                for(int i = 0; i < len; ++i)
                {
                    Debug.Msg(s[i] + Layout.LINE_SEP,event.getLevel().toInt());
                }
            }
        }
    }

    public void close()
    {
        this.closed = true;
    }

    /**
     * Appender admits to having a layout, but can work without it (by using a
     * default SimpleLayout).
     */
    public boolean requiresLayout()
    {
        return true;
    }
}
