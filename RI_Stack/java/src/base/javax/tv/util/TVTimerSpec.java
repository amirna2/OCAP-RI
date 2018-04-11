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

package javax.tv.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Vector;

import org.ocap.system.event.ErrorEvent;
import org.ocap.system.event.SystemEventManager;


/**
 * A class representing a timer specification. A timer specification declares
 * when a <code>TVTimerWentOffEvent</code> should be sent. These events are sent
 * to the listeners registered on the specification.</p>
 * 
 * <p>
 * A <code>TVTimerSpec</code> may be <b>absolute</b> or <b>delayed</b>. Absolute
 * specifications go off at the specified time. Delayed specifications go off
 * after waiting the specified amount of time.
 * </p>
 * 
 * <p>
 * Delayed specifications may be repeating or non-repeating. Repeating
 * specifications automatically reschedule themselves after going off.
 * </p>
 * 
 * <p>
 * Repeating specifications may be regular or non-regular. Regular
 * specifications attempt to go off at fixed intervals of time, irrespective of
 * system load or how long it takes to notify the listeners. Non-regular
 * specifications wait the specified amount of time after all listeners have
 * been called before going off again.
 * </p>
 * 
 * <p>
 * For example, you could create a repeating specification that goes off every
 * 100 milliseconds. Furthermore, imagine that it takes 5 milliseconds to notify
 * the listeners every time it goes off. If the specification is regular, the
 * listeners will be notified after 100 milliseconds, 200 milliseconds, 300
 * milliseconds, and so on. If the specification is non-regular, the listeners
 * will be notified after 100 milliseconds, 205 milliseconds, 310 milliseconds,
 * and so on.
 * </p>
 * 
 * @author: Alan Bishop
 */
public class TVTimerSpec
{

    /**
     * Creates a timer specification. It initially is absolute, non-repeating,
     * regular specification set to go off at time 0.
     */
    public TVTimerSpec()
    {
        absolute = true;
        repeat = false;
        regular = true;
        time = 0L;
        listeners = new Vector();
    }

    /**
     * Sets this specification to be absolute or delayed.
     * 
     * @param absolute
     *            Flag to indicate that this specification is either absolute or
     *            delayed. If <code>true</code>, the specification is absolute;
     *            otherwise, it is delayed.
     */
    public void setAbsolute(boolean absolute)
    {
        this.absolute = absolute;
    }

    /**
     * Checks if this specification is absolute.
     * 
     * @return <code>true</code> if this specification is absolute;
     *         <code>false</code> if it is delayed.
     */
    public boolean isAbsolute()
    {
        return absolute;
    }

    /**
     * Sets this specification to be repeating or non-repeating.
     * 
     * @param repeat
     *            Flag to indicate that this specification is either repeating
     *            or non-repeating. If <code>true</code>, the specification is
     *            repeating; otherwise, it is non-repeating.
     */
    public void setRepeat(boolean repeat)
    {
        this.repeat = repeat;
    }

    /**
     * Checks if this specification is repeating.
     * 
     * @return <code>true</code> if this specification is repeating;
     *         <code>false</code> if it is non-repeating.
     */
    public boolean isRepeat()
    {
        return repeat;
    }

    /**
     * Sets this specification to be regular or non-regular.
     * 
     * @param regular
     *            Flag to indicate that this specification is either regular or
     *            non-regular. If <code>true</code>, the specification is
     *            regular; otherwise, it is non-regular.
     */
    public void setRegular(boolean regular)
    {
        this.regular = regular;
    }

    /**
     * Checks if this specification is regular.
     * 
     * @return <code>true</code> if this specification is regular;
     *         <code>false</code> if it is non-regular.
     */
    public boolean isRegular()
    {
        return regular;
    }

    /**
     * Sets when this specification should go off. For absolute specifications,
     * this is a time in milliseconds since midnight, January 1, 1970 UTC. For
     * delayed specifications, this is a delay time in milliseconds.
     * 
     * @param time
     *            The time when this specification should go off.
     * @throws IllegalArgumentException
     *             If the specified time value is negative.
     */
    public void setTime(long time)
    {
        if (time < 0)
        {
            throw new IllegalArgumentException("TVTimerSpec Time cannot be negative");
        }

        this.time = time;
    }

    /**
     * Returns the absolute or delay time when this specification will go off.
     * 
     * @return The time when this specification will go off.
     */
    public long getTime()
    {
        return time;
    }

    // listeners

    /**
     * Registers a listener with this timer specification.
     * 
     * @param l
     *            The listener to add.
     */
    public void addTVTimerWentOffListener(TVTimerWentOffListener l)
    {
        if (l == null)
        {
            throw new NullPointerException();
        }
        listeners.addElement(l);
    }

    /**
     * Removes a listener to this timer specification. Silently does nothing if
     * the listener was not listening on this specification.
     * 
     * @param l
     *            The listener to remove.
     */
    public void removeTVTimerWentOffListener(TVTimerWentOffListener l)
    {
        if (l == null)
        {
            throw new NullPointerException();
        }
        listeners.removeElement(l);
    }

    // convenience functions

    /**
     * Sets this specification to go off at the given absolute time. This is a
     * convenience function equivalent to <code>setAbsolute(true)</code>,
     * <code>setTime(when)</code>, <code>setRepeat(false)</code>.
     * 
     * @param when
     *            The absolute time for the specification to go off.
     * @throws IllegalArgumentException
     *             If the specified time value is negative.
     */
    public void setAbsoluteTime(long when)
    {
        if (when < 0)
        {
            throw new IllegalArgumentException("TVTimerSpec AbsoluteTime cannot be negative");
        }

        absolute = true;
        time = when;
        repeat = false;
    }

    /**
     * Sets this specification to go off after the given delay time. This is a
     * convenience function equivalent to <code>setAbsolute(false)</code>,
     * <code>setTime(delay)</code>, <code>setRepeat(false)</code>.
     * 
     * @param delay
     *            The relative time for the specification to go off.
     * @throws IllegalArgumentException
     *             If the specified time value is negative.
     */
    public void setDelayTime(long delay)
    {
        if (delay < 0)
        {
            throw new IllegalArgumentException("TVTimerSpec DelayTime cannot be negative");
        }

        absolute = false;
        time = delay;
        repeat = false;
    }

    // for the benefit of timer implementations

    /**
     * Calls all listeners registered on this timer specification. When this
     * method returns, all listeners will have been notified.
     * <p>
     * This function is primarily for the benefit of those writing
     * implementations of TVTimers.
     * 
     * @param source
     *            The TVTimer that decided that this specification should go
     *            off.
     */
    public void notifyListeners(TVTimer source)
    {
        if (source == null) return;

        TVTimerWentOffListener array[] = null;
        synchronized (listeners)
        {
            array = new TVTimerWentOffListener[listeners.size()];
            listeners.copyInto(array);
        }
        for (int i = 0; i < array.length; ++i)
        {
            TVTimerWentOffEvent evt = new TVTimerWentOffEvent(source, this);
            try
            {
                array[i].timerWentOff(evt);
            }
            catch (Throwable throwable)
            {
                SystemEventManager mgr = SystemEventManager.getInstance();
                mgr.log(newErrorEvent(throwable));
            }
        }
    }

    /**
     * Create an instance of an <code>ErrorEvent</code> for purposes of logging
     * the given uncaught <code>Throwable</code>. A {@link ErrorEvent#getType
     * type} of {@link ErrorEvent#SYS_CAT_JAVA_THROWABLE} is implicitly
     * specified.
     * 
     * @param throwable
     *            the uncaught exception/error to be logged
     * @return a new instance of <code>ErrorEvent</code>
     */
    private ErrorEvent newErrorEvent(final Throwable throwable)
    {
        return (ErrorEvent) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new ErrorEvent(ErrorEvent.SYS_CAT_JAVA_THROWABLE, throwable);
            }
        });
    }

    public String toString()
    {
        StringBuffer outString = new StringBuffer("TVTS 0x" + Integer.toHexString(this.hashCode()) + ':' + " [time="
                + getTime() + ", absolute=" + isAbsolute() + ", repeat=" + isRepeat() + ", regular=" + isRegular());

        for (Enumeration e = listeners.elements(); e.hasMoreElements();)
        {
            TVTimerWentOffListener listener = (TVTimerWentOffListener) e.nextElement();

            outString.append(", listener=");

            outString.append(listener.getClass().getName());
        }
        outString.append(']');

        return outString.toString();
    }

    private boolean absolute;

    private boolean repeat;

    private boolean regular;

    private long time;

    private Vector listeners;
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
