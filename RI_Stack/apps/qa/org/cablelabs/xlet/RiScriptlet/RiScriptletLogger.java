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

// Declare package.
package org.cablelabs.xlet.RiScriptlet;


import org.apache.log4j.Logger;

import java.lang.StackTraceElement;
import bsh.NameSpace;
import bsh.CallStack;


/**
 * The class RiScriptletLogger adds a prefix to the log msg which specifies the
 * script 
 * 
 * @author Steve Arendt
 */
public class RiScriptletLogger
{
    private Logger m_log;
    private String m_scriptName;

    public RiScriptletLogger(Class myClass, String scriptName)
    {
        m_scriptName = scriptName;
        m_log = Logger.getLogger(myClass);
    }

    private String getHdr() 
    {
        if (m_scriptName != null)
        {
            return m_scriptName + ": ";
        }
        else
        {
            return "NOTSCRIPT: ";
        }
    }

    public boolean isInfoEnabled() {return m_log.isInfoEnabled();}
    public boolean isErrorEnabled() {return m_log.isErrorEnabled();}
    public boolean isDebugEnabled() {return m_log.isDebugEnabled();}

    public void error (String logEntry) 
    {
        if (isErrorEnabled()) m_log.error(getHdr() + logEntry);
    }

    public void error (Throwable ex) 
    {
        if (isErrorEnabled()) m_log.error(getHdr(), ex);
    }

    public void error (String logEntry, Throwable ex) 
    {
        if (isErrorEnabled()) m_log.error(getHdr() + logEntry, ex);
    }

    public void info (String logEntry) 
    {
        if (isInfoEnabled()) m_log.info(getHdr() + logEntry);
    }

    public void info (Throwable ex) 
    {
        if (isInfoEnabled()) m_log.info(getHdr(), ex);
    }

    public void info (String logEntry, Throwable ex) 
    {
        if (isInfoEnabled()) m_log.info(getHdr() + logEntry, ex);
    }

    public void debug (String logEntry) 
    {
        if (isDebugEnabled()) m_log.debug(getHdr() + logEntry);
    }
    public void debug (Throwable ex) 
    {
        if (isDebugEnabled()) m_log.debug(getHdr(), ex);
    }
    public void debug (String logEntry, Throwable ex) 
    {
        if (isDebugEnabled()) m_log.debug(getHdr() + logEntry, ex);
    }

    public void callTrace (String logEntry, CallStack stack) 
    {
        if (isInfoEnabled()) 
        {
            String callStack = constructCallStackString(stack);
            m_log.info(callStack  + ": " + logEntry);
        }
    }

    public String constructCallStackString(CallStack stack)
    {
        NameSpace[] elems = stack.toArray();

        String callStackString = "";

        for (int i=0; i<elems.length; i++)
        {
            System.out.println("elems[" + i + "] = " + elems[i].toString());

            if (elems[i].getName().equals ("global"))
            {
                // prepend script name

                if (m_scriptName != null)
                {
                    callStackString = m_scriptName + ": " + callStackString;
                }
                else
                {
                    callStackString = "NOTSCRIPT : " + callStackString;
                }
            }
            else
            {
                // prepend method name

                if (callStackString.length() == 0)
                {
                    callStackString = elems[i].getName();
                }
                else
                {
                    callStackString = elems[i].getName() + ": " + callStackString;
                }
            }
        }

        return callStackString;
    }

}