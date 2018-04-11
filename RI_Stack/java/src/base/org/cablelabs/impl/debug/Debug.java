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

import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.log4j.Level;
import org.apache.log4j.Priority;

/**
 * The <code>Debug</code> class provides Java access to the native side
 * debugging message output functionality. In the Simulator, this results in
 * messages displayed on the simulator's console window. On STB's, this will
 * result in a platform dependent output mechanism (typically, a serial io
 * output, or IP network message)
 * <p>
 * This class also includes a <code>PrintStream</code> ({@link #out}) that can
 * be used similar to <code>System.out</code>. This allows for simpler output,
 * including use of {@link PrintStream#println}.
 */
public class Debug
{
    /**
     * Results in a platform dependent native display of the string <i>s</i>.
     * 
     * @param s
     *            is the string to be displayed
     */
    synchronized public static void Msg(String s)
    {
        Msg(s, INFO);
    }

    /**
     * Results in a platform dependent native display of the string <i>s</i>.
     * Used by FDRAppender for FDRLogging purpose.
     * 
     * @param strMsg
     *            is the string to be displayed
     */
    synchronized public static void fdrMsg(String strMsg)
    {
        nFDRMsg(strMsg);
    }

    /**
     * Results in a platform dependent native display of the string <i>s</i>.
     * 
     * @param s
     *            is the string to be displayed
     */
    synchronized public static void Msg(String s, int level)
    {
        nMsg(s, level);
    }

    /**
     * Results in a platform dependent native SNMP log of the string
     * <i>message</i> at <i>timeStamp</i> to the given <i>oid</i>.
     * 
     * @param oid the SNMP OID of the table to add the message into
     * @param timeStamp the timestamp to associate with the message
     * @param message the string to be logged
     */
    public static void AddLogEntry(String oid, String timeStamp, String message)
    {
        nAddLogEntry(oid, timeStamp, message);
    }

    /**
     * Results in a platform dependent native display of the string <i>s</i>.
     * 
     * @param s
     *            is the string to be displayed
     * @param s
     *            is the logging level for this message string
     */
    private static native void nMsg(String s, int level);

    private static native void nProdMsg(String s);

    private static native void nFDRMsg(String strMsg);

    private static native void nAddLogEntry(String oid, String time, String msg);

    /**
     * <code>PrintStream</code> than can be used in-place of
     * <code>System.out</code>.
     */
    public static final PrintStream out = new PrintStream(new OutputStream()
    {
        public void close()
        {
        }

        public void flush()
        {
        }

        public void write(byte[] b)
        {
            Msg(new String(b));
        }

        public void write(byte[] b, int off, int len)
        {
            Msg(new String(b, off, len));
        }

        public void write(int b)
        {
            Msg(new String(new byte[] { (byte) b }));
        }
    });

    /**
     * Define logging level constants to line up w/ Log4J
     */
    public static final int FATAL = Priority.FATAL_INT;

    public static final int ERROR = Priority.ERROR_INT;

    public static final int WARN = Priority.WARN_INT;

    public static final int DEBUG = Priority.DEBUG_INT;

    public static final int INFO = Priority.INFO_INT;
    
    public static final int TRACE = Level.TRACE_INT;

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
