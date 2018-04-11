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

package org.cablelabs.impl.manager;

import org.cablelabs.impl.util.TaskQueue;
import org.cablelabs.impl.util.SystemEventUtil;

import java.lang.reflect.InvocationTargetException;

/**
 * Abstraction for an entity calling a Manager's APIs. This context will relate
 * to the current logical VM that the caller is running in.
 * 
 * @see CallbackData
 * @see CallerContextManager
 */
public interface CallerContext
{
    /**
     * Associates a <code>CallbackData</code> object with this
     * <code>CallerContext</code>. The provided <code>key</code> can be
     * subsequently used to access the data (via
     * {@link #getCallbackData(Object) getCallbackData} and remove the data (via
     * {@link #removeCallbackData(Object) removeCallbackData}.
     * 
     * @param data
     *            The CallbackData to store.
     * @param key
     *            The search key for the CallbackData.
     * @throws NullPointerException
     *             if <code>data</code> or <code>key</code> are
     *             <code>null</code>.
     */
    void addCallbackData(CallbackData data, Object key);

    /**
     * Disassociates a <code>CallbackData</code> object with this
     * <code>CallerContext</code>.
     * 
     * @param key
     *            Used to find the CallbackData object to remove.
     * @throws NullPointerException
     *             if <code>key</code> is <code>null</code>.
     */
    void removeCallbackData(Object key);

    /**
     * Retrieves the associated <code>CallbackData</code> object from this
     * <code>CallerContext</code>. The <code>CallbackData</code> object must
     * previously have been installed using
     * {@link #addCallbackData(CallbackData, Object) addCallbackData()}, and not
     * subsequently removed via {@link #removeCallbackData(Object)
     * removeCallbackData}.
     * 
     * @param key
     *            The object used to search for the CallbackData object.
     * @return The CallbackData object paired with the key, or <code>null</code>
     *         if not found.
     * @throws NullPointerException
     *             if <code>key</code> is <code>null</code>.
     */
    CallbackData getCallbackData(Object key);

    /**
     * Associates generic <code>Object</code> data with this
     * <code>CallerContext</code>. The provided <code>key</code> can be
     * subsequently used to access the data (via {@link #getData(Object)
     * getData} and remove the data (via <code>putData(null, key)</code>).
     * 
     * @param data
     *            The generic <code>Object</code> data to store
     * @param key
     *            The search key for the <code>Object</code> data
     * @throws NullPointerException
     *             if <code>key</code> is <code>null</code>.
     */
    // void putData(Object data, Object key);

    /**
     * Retrieves generic <code>Object</code> data associated with this
     * <code>CallerContext</code>. The <code>Object</code> data must previously
     * have been installed using {@link #putData}, and not subsequently removed
     * via <code>putData(null,key)</code>.
     * 
     * @param key
     *            The search key for the <code>Object</code> data
     * @throws NullPointerException
     *             if <code>key</code> is <code>null</code>.
     */
    // Object getData(Object key);

    /**
     * Executes a <code>Runnable</code> object within this
     * <code>CallerContext</code>'s thread context. This can be used by system
     * implementation code to perform arbitrary operations within the context of
     * the original caller. Specifically, this operation is suitable for event
     * delivery operations that must execute in a specific order.
     * <p>
     * All <code>Runnable</code> executed within a context using this method are
     * executed in FIFO order. That is they are serialized. They are executed
     * asynchronous to the caller as long as the caller wasn't executed this
     * same way. Compare to {@link #runInContextAsync} which executes within
     * this context without any synchronization with other operations. This is
     * similar to calling {@link #runInContextSync(Runnable)
     * runInContext(Runnable, false)}.
     * 
     * @param run
     *            The runnable object to execute.
     * @throws SecurityException
     *             if {@link #checkAlive()} would do the same.
     * @throws IllegalStateException
     *             if the current state of the application is not otherwise in
     *             an <i>alive</i> state.
     * 
     * @see #runInContextAsync
     * @see #runInContextSync
     */
    void runInContext(Runnable run) throws SecurityException, IllegalStateException;

    /**
     * Executes a <code>Runnable</code> object within this
     * <code>CallerContext</code>'s thread context. This can be used by system
     * implementation code to perform arbitrary operations within the context of
     * the original caller that must wait for the operation to complete.
     * <p>
     * For example, it can be used to call a <i>caller</i>-installed handler
     * object prior to continuing on.
     * 
     * <pre>
     * final Handler handler = handler;
     * final boolean[] answer = { false };
     * ctx.runInContextSync(new Runnable()
     * {
     *     public void run()
     *     {
     *         answer[0] = handler.askQuestion();
     *     }
     * });
     * // ...continue with answer[0]
     * </pre>
     * 
     * @param run
     *            The runnable object to execute.
     * 
     * @throws SecurityException
     *             if {@link #checkAlive()} would do the same.
     * @throws IllegalStateException
     *             if the current state of the application is not otherwise in
     *             an <i>alive</i> state.
     * @throws InvocationTargetException
     *             if an exception was thrown by the <code>Runnable.run()</code>
     *             method.
     * 
     * @see #runInContext(Runnable)
     */
    void runInContextSync(Runnable run) throws SecurityException, IllegalStateException, InvocationTargetException;

    /**
     * Executes a <code>Runnable</code> object within this
     * <code>CallerContext</code>'s thread context. This can be used by system
     * implementation code to perform arbitrary operations that are intended to
     * be completely asynchronous to other operations. This is similar to
     * {@link #runInContext} except that <code>runInContext()</code> implies a
     * natural ordering to operations; this method implies no ordering.
     * <p>
     * This can be used to invoke <i>caller</i>-installed callback objects.
     * 
     * @param run
     *            the runnable objec to execute
     * @throws SecurityException
     *             if {@link #checkAlive()} would do the same.
     * @throws IllegalStateException
     *             if the current state of the application is not otherwise in
     *             an <i>alive</i> state.
     */
    void runInContextAsync(Runnable run) throws SecurityException, IllegalStateException;

    /**
     * Creates and returns a new <code>TaskQueue</code> that can be used to
     * execute <code>Runnable</code> tasks within this
     * <code>CallerContext</code>. All tasks posted to the given queue are
     * executed serially such that one must finish before the next one starts to
     * execute. Tasks posted to different queues execute independently of one
     * another (although order between queues is not specified or guaranteed).
     * <p>
     * If and when this <code>CallerContext</code> is <i>destroyed</i>, the
     * associated <code>TaskQueue</code>s will be implicitly
     * {@link TaskQueue#dispose()} if not already.
     * 
     * @return a new <code>TaskQueue</code> that can be used to execute tasks
     *         within this <code>CallerContext</code>
     * 
     * @throws IllegalStateException
     *             if the current state of this <code>CallerContext</code> is
     *             not currently in an <i>active</i> state
     * @throws UnsupportedOperationException
     *             if this <code>CallerContext</code> does not support the
     *             creation of queues
     */
    TaskQueue createTaskQueue();

    /**
     * Tests whether this <code>CallerContext</code> is <i>alive</i>.
     * 
     * @return <code>true</code> if this <code>CallerContext</code> has not been
     *         destroyed (i.e., no calls to
     *         {@link CallbackData#destroy(CallerContext)
     *         CallbackData.destroy()} have potentially been made).
     */
    boolean isAlive();

    /**
     * Checks whether this <code>CallerContext</code> is <i>alive</i>, and
     * throws a <code>SecurityException</code> if it is not.
     * 
     * @throws SecurityException
     *             if {@link #isAlive()} would return <code>false</code>.
     */
    void checkAlive() throws SecurityException;

    /**
     * Tests whether this <code>CallerContext</code> is <i>active</i>. An
     * <i>active</i> context is one that is both <i>alive</i> and not in a
     * <i>paused</i> state.
     * <p>
     * 
     * @return <code>true</code> if this <code>CallerContext</code> is
     *         <i>active</i> -- not <i>paused</i>
     * 
     * @see CallbackData#pause(CallerContext)
     * @see CallbackData#active(CallerContext)
     */
    boolean isActive();

    /**
     * Retrieve the attribute with the specified key.
     * 
     * @param key
     *            a unique identifier for the attribute to be returned.
     * @return the value of the attribute specified by <code>key</code>
     */
    Object get(Object key);

    /**
     * This utility class provides wrapper methods for common CallerContext
     * functionality considered to be desirable in our stack implementation
     * 
     * @author Greg Rutz
     */
    static public class Util
    {
        /**
         * Convenience method for {@link #runInContextSync(Runnable)} that
         * handles <code>InvocationTargetException</code> for you. If this
         * exception is caught, it will be logged to the
         * <code>SystemEventUtil</code>.
         * 
         * @param cc
         *            The <code>CallerContext</code> object on which to execute
         *            the given <code>Runnable</code>
         * @param run
         *            The <code>Runnable</code> object to execute.
         * @throws SecurityException
         *             if {@link #checkAlive()} would do the same.
         * @throws IllegalStateException
         *             if the current state of the application is not otherwise
         *             in an <i>alive</i> state.
         * @return false if <code>InvocationTargetException</code> has been
         *         caught and logged, true otherwise
         */
        public static boolean doRunInContextSync(CallerContext cc, Runnable run) throws SecurityException,
                IllegalStateException
        {
            try
            {
                cc.runInContextSync(run);
                return true;
            }
            catch (InvocationTargetException e)
            {
                SystemEventUtil.logUncaughtException(e.getTargetException(), cc);
            }
            return false;
        }
    }

    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve the {@link javax.tv.service.selection.ServiceContext} attribute
     * for this <code>CallerContext</code>.
     */
    public static final Object SERVICE_CONTEXT = new Object();

    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve the unique id of the ServiceContext attribute of this
     * <code>CallerContext</code>
     */
    public static final Object SERVICE_CONTEXT_ID = new Object();

    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve the <i>base directory</i> or <code>user.dir</code> for this
     * context as a <code>String</code>. This value should be used to parse
     * relative file paths, including ".".
     */
    public static final Object USER_DIR = new Object();
    
    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve the full path to the directory that would be returned as the
     * Java system property <i>java.io.tmpdir</i> for this application
     */
    public static final Object JAVAIO_TMP_DIR = new Object();

    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve the {@link org.dvb.application.AppID} attribute for this
     * <code>CallerContext</code>.
     */
    public static final Object APP_ID = new Object();

    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve the run-time application priority attribute (as an
     * <code>Integer</code> object) for this <code>CallerContext</code>.
     */
    public static final Object APP_PRIORITY = new Object();

    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve a <code>ThreadGroup</code> specific to the calling application.
     * This is meant to be used by {@link SecurityManager#getThreadGroup()}
     * implementation.
     */
    public static final Object THREAD_GROUP = new Object();
    
    /**
     * Can be specified as the <i>key</i> parameter to {@link #get} in order to
     * retrieve a {@link org.davic.net.Locator Locator} for the calling
     * application's home service (if any is known).
     */
    public static final Object SERVICE_DETAILS = new Object();

    /**
     * The <code>CallerContext.Multicaster</code> is an implementation of
     * <code>CallerContext</code> meant to assist in the efficient and
     * thread-safe management of <code>CallerContext</code>-base callbacks.
     * <p>
     * A <code>Multicaster</code>'s functions and usage are similar to event
     * multicasters (e.g., <code>java.awt.AWTEventMulticaster</code> or
     * {@link org.havi.ui.HEventMulticaster}). The static
     * {@link #add(CallerContext, CallerContext)} and
     * {@link #remove(CallerContext, CallerContext)} methods are used to manage
     * a thread-safe multicaster. The {@link #runInContext} method is used to
     * execute a given <code>Runnable</code> on all contexts.
     * <p>
     * One major deviation from the common multicaster idiom is that each
     * <code>CallerContext</code> added to the multicaster is referenced only
     * once. This requires that the implementation is different from that of
     * most multicasters.
     * <p>
     * No instance methods beyond <code>runInContext()</code> should be used.
     * They will throw exceptions.
     * <p>
     * Note that a <code>Multicaster</code> context will never be returned by
     * the <code>CallerContextManager</code>.
     * <p>
     * The intended use for <code>Multicaster</code> is to be used to handle the
     * callback of application listeners on application-independent objects
     * (e.g., <code>org.dvb.appliation.AppProxy</code>). This can be done by
     * maintaining an appropriate event multicaster within the
     * <code>CallerContext</code> object that created it using the data-access
     * methods of <code>CallerContext</code> as well as maintaining a
     * <code>CallerContext</code> multicaster with which to make callbacks. Here
     * is an incomplete example:
     * 
     * <pre>
     * private CallerContext listenerContext;
     * private Listener listener;
     * private static CallerContextManager ccm;
     * 
     * public synchronized void addListener(Listener l)
     * {
     *     CallerContext ctx = ccm.getCurrentContext();
     * 
     *     // Listeners are maintained within Context
     *     SimpleData data = (SimpleData)ctx.getCallbackData(this);
     *     if (data == null)
     *         data = new SimpleData(null) {
     *             public void destroy(CallerContext ctx) {
     *                 forgetContext(ctx);
     *             }
     *         };
     *     data.setData(EventMulticaster.add(data.getData(), l));
     *     ctx.addCallbackData(data, this);
     * 
     *     // Manage the current set of contexts
     *     listenerContext = {@link #add Multicaster.add}(listenerContext, ctx);
     * }
     * 
     * // used to forget context and all listeners
     * private synchronized void forgetContext(CallerContext ctx)
     * {
     *     listenerContext = {@link #remove(CallerContext, CallerContext) Multicaster.remove}(listenerContext, ctx);
     * }
     * 
     * // used to notify listeners in all contexts about an event
     * private void notifyListeners(final Event e)
     * {
     *      listenerContext.{@link #runInContext runInContext}(new Runnable() {
     *          public void run() {
     *              SimpleData d = (SimpleData)ccm.getCurrentContext().getCallbackData();
     *              Listener l;
     *              if (d != null && (l = d.getData()) != null)
     *                  l.notify(e);
     *          }
     *      });
     * }
     * </pre>
     * 
     * @author Aaron Kamienski
     */
    public static class Multicaster implements CallerContext
    {
        private final CallerContext a;

        private final CallerContext b;

        /**
         * Creates a multicaster instance which chains callback-a with
         * callback-b. The parameters a and b passed to the constructor shall be
         * used to populate the fields a and b of the instance.
         * 
         * @param a
         *            listener-a
         * @param b
         *            listener-b
         */
        private Multicaster(CallerContext a, CallerContext b)
        {
            this.a = a;
            this.b = b;
        }

        /**
         * Removes a context from this multicaster and returns the result.
         * 
         * @param old
         *            the listener to be removed
         */
        private CallerContext remove(CallerContext old)
        {
            if (old == a) return b;
            if (old == b) return a;

            CallerContext a2 = removeInternal(a, old);
            CallerContext b2 = removeInternal(b, old);
            if (a2 == a && b2 == b)
                return this;
            
            return addInternal(a2, b2);
        }

        /**
         * Returns the resulting multicaster from adding context-a and context-b
         * together. If context-a is null, it returns context-b; If context-b is
         * null, it returns context-a If neither are null, then it creates and
         * returns a new <code>Multicaster</code> instance which chains a with
         * b.
         * 
         * @param a
         *            event context-a
         * @param b
         *            event context-b
         */
        private static CallerContext addInternal(CallerContext a, CallerContext b)
        {
            // If a is empty just return b
            // If b already contains a, just return b
            if (a == null || contains(b, a)) return b;
            // If b is empty just return a
            // If a already contains b, just return a
            if (b == null || contains(a, b)) return a;

            return new Multicaster(a, b);
        }

        /**
         * Determines if <i>multi</i> is considered to contain <i>single</i>.
         * This will return <code>true</code> if <code>multi == single</code> or
         * <code>multi instanceof Multicaster</code> and any of the following
         * are true:
         * <ul>
         * <li> <code>multi.a == single</code>
         * <li> <code>multi.b == single</code>
         * <li> <code>contains(multi.a, single)</code>
         * <li> <code>contains(multi.b, single)</code>
         * </ul>
         * Otherwise, <code>false</code> is returned.
         * 
         * @param multi
         *            the CallerContext/Multicaster to search
         * @param single
         *            the CallerContext to search for
         * @return <code>true</code> if <i>multi</i> is considered to
         *         <i>contain</i> <i>single</i>, <code>false</code> otherwise
         */
        private static boolean contains(CallerContext multi, CallerContext single)
        {
            if (multi == single)
                return true;
            else if (multi != null && multi instanceof Multicaster)
            {
                Multicaster m = (Multicaster) multi;
                return m.a == single || m.b == single || contains(m.a, single) || contains(m.b, single);
            }
            return false;
        }

        /**
         * Returns the resulting multicast context after removing the old
         * context from context-l. If context-l equals the old context OR
         * context-l is null, returns null. Else if context-l is an instance of
         * <code>Multicaster</code>, then it removes the old context from it.
         * Else, returns context l.
         * 
         * @param l
         *            the context being removed from
         * @param old
         *            the context being removed
         */
        private static CallerContext removeInternal(CallerContext l, CallerContext old)
        {
            if (l == old || l == null)
                return null;
            else if (l instanceof Multicaster)
                return ((Multicaster) l).remove(old);
            else
                return l;
        }

        /**
         * Adds <i>context-a</i> with <i>context-b</i> and returns the resulting
         * <code>Multicaster</code>. <i>Note that each
         * <code>CallerContext</code> is added only once</i>.
         * 
         * @param a
         *            context-a
         * @param b
         *            context-b
         */
        public static CallerContext add(CallerContext a, CallerContext b)
        {
            return addInternal(a, b);
        }

        /**
         * Removes the old <code>CallerContext</code> <i>old</i> from
         * <i>context</i> and returns the resulting multicaster.
         * 
         * @param context
         *            multicaster
         * @param old
         *            the <code>CallerContext</code> being removed
         */
        public static CallerContext remove(CallerContext context, CallerContext old)
        {
            return removeInternal(context, old);
        }

        /**
         * Executes the given <code>Runnable</code> object within the context of
         * <i>context-a</i> and <i>context-b</i> by invoking the
         * {@link CallerContext#runInContext(Runnable)} method.
         * <p>
         * Note that exceptions are caught and ignored for each call to
         * <code>runInContext()</code>.
         * 
         * @param run
         *            the runnable object
         */
        public void runInContext(Runnable run) throws SecurityException, IllegalStateException
        {
            run(a, run);
            run(b, run);
        }

        /**
         * Executes the given <code>Runnable</code> object within the context of
         * <i>context-a</i> and <i>context-b</i> by invoking the
         * {@link CallerContext#runInContextSync(Runnable)} method.
         * 
         * @param run
         *            the runnable object
         */
        public void runInContextSync(Runnable run) throws InvocationTargetException
        {
            runSync(a, run);
            runSync(b, run);
        }

        /**
         * Executes the given <code>Runnable</code> object within the context of
         * <i>context-a</i> and <i>context-b</i> by invoking the
         * {@link CallerContext#runInContextAsync(Runnable)} method.
         * 
         * @param run
         *            the runnable object
         */
        public void runInContextAsync(Runnable run)
        {
            runAsync(a, run);
            runAsync(b, run);
        }

        /**
         * Runs the given <code>Runnable</code> in the given
         * <code>CallerContext</code>, if defined. Catches and ignores
         * <code>SecurityException</code> and <code>IllegalStateException</code>
         * .
         */
        private void run(CallerContext cc, Runnable run)
        {
            try
            {
                if (cc != null) cc.runInContext(run);
            }
            catch (SecurityException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
            catch (IllegalStateException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }

        /**
         * Runs the given <code>Runnable</code> in the given
         * <code>CallerContext</code>, if defined. Catches and ignores
         * <code>SecurityException</code> and <code>IllegalStateException</code>
         * .
         */
        private void runSync(CallerContext cc, Runnable run) throws InvocationTargetException
        {
            try
            {
                if (cc != null) cc.runInContextSync(run);
            }
            catch (SecurityException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
            catch (IllegalStateException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }

        /**
         * Runs the given <code>Runnable</code> in the given
         * <code>CallerContext</code>, if defined. Catches and ignores
         * <code>SecurityException</code> and <code>IllegalStateException</code>
         * .
         */
        private void runAsync(CallerContext cc, Runnable run)
        {
            try
            {
                if (cc != null) cc.runInContextAsync(run);
            }
            catch (SecurityException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
            catch (IllegalStateException e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }

        /** Unimplemented. */
        public void addCallbackData(CallbackData data, Object key)
        {
            throw new UnsupportedOperationException("Invalid context");
        }

        /** Unimplemented. */
        public void removeCallbackData(Object key)
        {
            throw new UnsupportedOperationException("Invalid context");
        }

        /** Unimplemented. */
        public CallbackData getCallbackData(Object key)
        {
            throw new UnsupportedOperationException("Invalid context");
        }

        /** Unimplemented. */
        public boolean isAlive()
        {
            throw new UnsupportedOperationException("Invalid context");
        }

        /** Unimplemented. */
        public void checkAlive() throws SecurityException
        {
            throw new UnsupportedOperationException("Invalid context");
        }

        /** Unimplemented. */
        public boolean isActive()
        {
            throw new UnsupportedOperationException("Invalid context");
        }

        /** Unimplemented. */
        public Object get(Object key)
        {
            throw new UnsupportedOperationException("Invalid context");
        }

        /** Unimplemented. */
        public TaskQueue createTaskQueue()
        {
            throw new UnsupportedOperationException("Invalid context");
        }
    }
}
