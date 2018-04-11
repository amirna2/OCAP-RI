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

/*
 *  This file incorporates work covered by the following copyright and  
 *  permission notice:  
 *  
 *  Copyright (c) 2005-2008, Cisco Systems, Inc.  
 * 
 *  Permission to use, copy, modify, and/or distribute this software  
 *  for any purpose with or without fee is hereby granted, provided  
 *  that the above copyright notice and this permission notice appear  
 *  in all copies.  
 * 
 *  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL  
 *  WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED  
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE  
 *  AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR  
 *  CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS  
 *  OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,  
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN  
 *  CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.  
 */

package org.cablelabs.debug;

import org.cablelabs.impl.ocap.OcapMain;

/**
 * Description: This module makes it possible to profile code in both debug and
 * release modes. In debug mode, the printf calls are via the serial port
 * (HOWEVER, you must uncomment the MPE_JNI line in mpeenv.ini in debug to see
 * the printfs). In release mode, the printf calls can be via UDP to the
 * headend, if you specify that in the mpeenv.ini file (again, you must make
 * sure that fastboot, which is at the bottom of the mpeenv.ini in release, is
 * uncommented). Timing is started by the <code>startTime</code> call -- you
 * supply the label of the process you want to time. The <code>stopyTime</code>
 * call completes a process started by <code>startTime</code>. The names of the
 * subprocesses you want to time are created by <code>addLabel</code> calls and
 * the indices used in <code>setWhere</code> calls are returned by the
 * <code>addLabel</code> calls. <code>setWhere</code>/<code>popWhere</code>
 * calls are used to time subprocesses. Subprocess timing can be recursive.
 * <p>
 * Since these calls can also be called from C, you can mix and match
 * subprocesses in C with subprocesses in Java.
 * <p>
 */
public class Profile
{
    /*
     * Provides the ability to profile code (can be mixed with profiles of C
     * code)
     */

    /**
     * use the boolean <code>isProfiling()</code> to avoid calling into
     * profiling code when profiling is disabled in MPE.
     * 
     * <pre>
     * Example:
     * case HRcEvent.VK_7:
     *   if (Profile.TUNING && Profile.isProfiling()) // stops if MPE disabled
     *     {
     * 	    System.out.println("Dumping Profile\n");
     * 	    Profile.dumpProfile(0);
     *     }
     * break;
     * </pre>
     */

    public static boolean isProfilingFlag = false; // set by init() from MPE

    // set ALL of these to FALSE to make profiling not compile into a released
    // application
    // add a new one if you want to add profiling of something else
    public static final boolean TUNING = true;

    // low level start/stop -- must be same as MPE_PROF_TUNING_LL
    // AND same as Profile.java in other Profile.java file
    public static final boolean TUNING_LL = true; // low level start/stop --
                                                  // must be same as
                                                  // MPE_PROF_TUNER_LL

    public static final boolean SECTION_FILTERING = false;

    public static final boolean CLASS_LOADER = true;

    /**
     * Creates a timer and starts it running
     * 
     * <pre>
     * example:
     * private int channelTuneJava = -1;
     *  if (Profile.TUNING && Profile.isProfiling())
     *     {
     * 	    Profile.startTiming("WatchPTV:select- " + channelNumber[channelNum] + "\n");
     * 	    Profile.setWhere(channelTuneJava);
     * 	    System.out.println("WatchPTVXlet:just Start Timing, setWhere for channelTuneJava = " + channelTuneJava + "\n");
     *     }
     * </pre>
     * 
     * @param s
     *            - String the name of the process timed (shows in the header of
     *            the timing report)
     */
    public static native void startTiming(String s);

    /**
     * Stops the current timer and stores the timing data into the array of
     * processes timed From here on this process will be displayed when
     * dumpProfile is called.
     * 
     * <pre>
     * example: if (Profile.TUNING &amp;&amp; Profile.isProfiling())
     * {
     *     Profile.popWhere();
     *     Profile.stopTiming();
     * }
     * </pre>
     * 
     * @param none
     */
    public static native void stopTiming();

    /**
     * starts timing a subprocess to the process initiated by
     * <code>startTiming</code> <code>setWhere</code> and <code>popWhere</code>
     * can only be invoked between a <code>startTiming</code>/
     * <code>stopTiming</code> pair. <code>setWhere</code>s and
     * <code>popWhere</code>s can be recursive. That is a <code>setWhere</code>
     * can be called and then another <code>setWhere</code>/
     * <code>popWhere</code> can be called in a subroutine (which can call
     * another subroutine with <code>setWhere</code>/<code>popWhere</code> up to
     * 128 levels deep). The time in any <code>setWhere</code> is NOT cumulative
     * -- it doesn't include the time in any <code>setWhere</code>/
     * <code>popWhere</code> underneath it. That is, the timing of any given
     * <code>setWhere</code> goes up to the next invoked <code>setWhere</code>,
     * doesn't time, and then restarts timing at the <code>popWhere</code>
     * associated with the invoked <code>setWhere</code>.
     * <p>
     * In the <code>dumpProfile</code>, the label given to the process timed by
     * the <code>setWhere</code>/<code>popWhere</code> will be that of the
     * <code>addLabel</code> associated with the where input parameter.
     * 
     * <pre>
     * example:
     * if (Profile.TUNING && Profile.isProfiling())
     *     {
     * 	    Profile.startTiming("WatchPTV:select- " + channelNumber[channelNum] + "\n");
     * 	    Profile.setWhere(channelTuneJava);
     * 	    System.out.println("WatchPTVXlet:just Start Timing, SetWhere for channelTuneJava = " + channelTuneJava + "\n");
     *      }
     * }
     * </pre>
     * 
     * @param where
     *            - int This is the value returned by <code>addLabel</code>
     *            associated with a given process name (string)
     */
    public static native void setWhere(int where);

    /**
     * Stops the timing of a subprocess initiated by a <code>setWhere</code>.
     * Each <code>setWhere</code> must be balanced by a <code>popWhere</code>.
     * For example, if you put a <code>setWhere</code> at the start of a
     * procedure (in order to time the procedure), you must put a
     * <code>popWhere</code> just prior to all the return statements from that
     * procedure (including the exit at the bottom of the procedure, of course).
     * 
     * <pre>
     * example: if (Profile.TUNING &amp;&amp; Profile.isProfiling())
     * {
     *     Profile.popWhere();
     *     Profile.stopTiming();
     * }
     * </pre>
     * 
     * @param none
     */
    public static native void popWhere();

    /**
     * When numPrints is zero, dump of all completed timings (
     * <code>startTime</code>/<code>stopTime</code> pair)up to the limit of
     * timings (currently 128). After the limit is reached, the last
     * limit-of-timing members are shown.
     * <p>
     * If numPrints is not zero, the last numPrints timings are shown. Set
     * numPrints to one to see the last timing logged.
     * <p>
     * this procedure prints via the serial port for debug and can be via a UDP
     * to the headend for production. The heading includes the string passed
     * into <code>startTime</code> and the labels describing what was timed come
     * from the <code>setWhere</code>s (which ultimately come from the
     * <code>addLabel</code>s). There is a graball label called "Process Time"
     * which includes all the time between the <code>startTime</code> and the
     * <code>setWhere</code>/<code>popWhere</code> and the <code>stopTime</code>
     * . If there are multiple <code>setWhere</code>/<code>popWhere</code> at
     * the highest level, then all time between these are added into the Process
     * Time.
     * <p>
     * Any process that cumulatively takes less than 1/1000th of a second AND
     * has no invocations is not printed. So, for example, if you did a
     * <code>startTime</code> immediately followed by a <code>setWhere</code>
     * and the associated <code>popWhere</code> is just prior to the
     * <code>stopTime</code>, then the Process Time will probably not print
     * since it will be less than 1/1000th of a second.
     * <p>
     * The Clock Time is the time that actually passes on the clock (what you
     * would get with a stopwatch). The CPU time is the "thread time", how much
     * time is spent in the PowerTV OS thread (the time spent on this
     * subprocess). If the <code>popWhere</code> is not on the same thread as
     * the <code>setWhere</code>, CPU time is set to zero. "Invoked" is how many
     * times a particular <code>setWhere</code>/<code>popWhere</code> pair were
     * called during the activity.
     * <p>
     * 
     * <pre>
     * Example:
     * case HRcEvent.VK_7:
     * if (Profile.TUNING && Profile.isProfiling())
     *     {
     * 		System.out.println("Dumping Profile\n");
     * 		Profile.dumpProfile(0); // displays all timings
     *      }
     * break;
     * the printout looks like this:
     * Timing for: WatchPTV:select- Channel 111
     * time run: 7/6/2005 14:36:33 
     * Total Clock Time:   3.391 secs, Total CPU Time:   0.000 secs, invoked: 1
     * Channel Tune Java, Clock Time:   3.239 secs, CPU Time:   0.000 secs, invoked: 1
     * Channel Tune in C, clock, Clock Time:   0.124 secs, CPU Time:   0.000 secs, invoked: 1
     * Channel Tune in C, thread, Clock Time:   0.027 secs, CPU Time:   0.016 secs, invoked: 2
     * ===========================
     * </pre>
     * <p>
     * notice that the label "Channel Tune Java" comes from the
     * <code>addLabel</code> while "Timing for: WatchPTV:select- Channel 111"
     * comes from the <code>startTiming</code> the "Channel Tune in C, clock"
     * and "Channel Tune in C, thread" come from <code>setWhere</code>s and
     * <code>popWhere</code>s in C.
     * 
     * @param numPrints
     *            - int number of timings to display (shows last numPrints
     *            timings)
     */
    public static native void dumpProfile(int numPrints);

    /**
     * Adds a label for a subprocess you want to time.
     * 
     * <pre>
     * if (Profile.TUNING &amp;&amp; Profile.isProfiling())
     * {
     *     System.out.println(&quot;WatchPTVXlet:about to add a label\n&quot;);
     *     channelTuneJava = Profile.addLabel(&quot;Channel Tune Java&quot;); // use in setWhere
     *     System.out.println(&quot;WatchPTVXlet:channelTuneJava = &quot; + channelTuneJava + &quot;\n&quot;);
     * }
     * </pre>
     * 
     * @param s
     *            - String Label associated with a given process (seen in
     *            <code>dumpProfile</code>)
     * @return int - index associated with a given process to be timed (used in
     *         <code>setWhere</code>)
     */
    public static native int addLabel(String s);

    /**
     * Adds a comment to an activity (<code>startTiming</code>) you want to
     * time. For example, in Object Carousel, "size of file is xxx" where you
     * want to modify xxx every time you do a read. Comments are strung together
     * with ";"
     * 
     * <pre>
     * if (Profile.TUNING &amp;&amp; Profile.isProfiling())
     * {
     *     System.out.println(&quot;WatchPTVXlet:about to add a comment\n&quot;);
     *     Profile.addComment(&quot;file name=&quot; + fileName);
     *     Profile.addComment(&quot;file size=&quot; + fileSize);
     * }
     * </pre>
     * 
     * @param s
     *            - String Comment associated with a given activity (seen in
     *            <code>dumpProfile</code>)
     */
    public static native void addComment(String s);

    /**
     * Gets the label index associated with a given string
     * 
     * <pre>
     * if (Profile.TUNING &amp;&amp; Profile.isProfiling())
     * {
     *     System.out.println(&quot;WatchPTVXlet:about to add a label\n&quot;);
     *     channelTuneJava = Profile.getIndex(&quot;Channel Tune Java&quot;); // use in setWhere
     *     System.out.println(&quot;WatchPTVXlet:channelTuneJava = &quot; + channelTuneJava + &quot;\n&quot;);
     * }
     * </pre>
     * 
     * @param s
     *            - String Label associated with a given process (seen in
     *            <code>dumpProfile</code>)
     * @return int - index associated with a given process to be timed (used in
     *         <code>setWhere</code>)
     */
    public static native int getIndex(String s);

    /**
     * returns a boolean telling whether timing is enabled.
     * 
     * @return boolean - false if profiling disabled, true if profiling enabled
     */
    private static native boolean init();

    /**
     * returns a boolean telling whether timing is enabled.
     * 
     * @return boolean - false if profiling disabled, true if profiling enabled
     */
    public static boolean isProfiling()
    {
        return isProfilingFlag;
    }

    static
    {
        // Load the Native side of this OCAP stack extension
        OcapMain.loadLibrary();
        isProfilingFlag = init();
    }
}
