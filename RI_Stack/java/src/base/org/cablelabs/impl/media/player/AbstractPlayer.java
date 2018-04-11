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

package org.cablelabs.impl.media.player;

import java.awt.Component;
import java.io.IOException;
import java.util.Arrays;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.Vector;

import javax.media.Clock;
import javax.media.ClockStartedError;
import javax.media.ClockStoppedException;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.DeallocateEvent;
import javax.media.GainChangeEvent;
import javax.media.GainChangeListener;
import javax.media.GainControl;
import javax.media.IncompatibleSourceException;
import javax.media.IncompatibleTimeBaseException;
import javax.media.InternalErrorEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.MediaTimeSetEvent;
import javax.media.NotPrefetchedError;
import javax.media.NotRealizedError;
import javax.media.PrefetchCompleteEvent;
import javax.media.RateChangeEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.StartEvent;
import javax.media.StopAtTimeEvent;
import javax.media.StopByRequestEvent;
import javax.media.StopEvent;
import javax.media.StopTimeChangeEvent;
import javax.media.Time;
import javax.media.TimeBase;
import javax.media.TransitionEvent;
import javax.media.protocol.DataSource;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.media.player.AlarmClock.Alarm.Callback;
import org.cablelabs.impl.media.presentation.Presentation;
import org.cablelabs.impl.media.presentation.PresentationContext;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.CallerContextEventMulticaster;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.Task;
import org.cablelabs.impl.util.TaskQueue;
import org.cablelabs.impl.util.TaskTimer;
import org.davic.media.ResourceReturnedEvent;
import org.davic.media.ResourceWithdrawnEvent;
import org.dvb.media.VideoTransformation;
import org.ocap.media.MediaTimer;

/**
 * This is the abstract base class of the {@link javax.media.Player}
 * implementation class hierarchy. It provides base functionality needed by all
 * Players. Subclasses add their own behavior by providing implementations of
 * the abstract methods defined in this class.
 * 
 * @author schoonma
 */
public abstract class AbstractPlayer implements Player, PresentationContext
{
    /** log4j logger */
    private static final Logger log = Logger.getLogger(AbstractPlayer.class.getName());

    /**
     * {@link CallerContextManager} instance, used to register
     * {@link CallbackData} for {@link ControllerListener}s.
     */
    static CallerContextManager ccMgr = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private static final int SYSTEM_CONTEXT_PRIORITY = 256;

    //when scheduling an alarm, if the current mediatime / alarmspec wall time delta is greater than this number, schedule
    //the timer with a time of delta * ALARM_TIMER_SCHEDULE_DELAY_FACTOR (will fire before target alarmspec time)
    private long ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD = 10000;

    // If timer fires and current mediatime / alarm spec wall time delta is less than this number, fire event, otherwise, reschedule the timer
    private long ALARM_TIMER_MILLIS_FIRING_THRESHOLD = 1000;

    //When scheduling an alarm, if current mediatime / alarmspec wall time delta is greater than
    // ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD, schedule the timer with a time of delta * this number
    //Range: > 0 to 1.0
    private float ALARM_TIMER_SCHEDULE_FACTOR = .8F;
    private final Object lock;
    private ResourceUsageImpl resourceUsage;
    //track if setMediaTime has been called
    private boolean mediaTimeSet;
    //no need to use 'this' in the identity hash lookup, just a unique identifier
    private final String id = "Id: 0x" + Integer.toHexString(System.identityHashCode(new Object())).toUpperCase() + ": ";

    /**
     * The {@link ClockImpl} that the {@link AbstractPlayer} uses to keep track
     * of media time.
     */
    private final ClockImpl clock;


    /*
     * Lifecycle
     */

    /**
     * Construct an {@link AbstractPlayer}, owned by a {@link CallerContext}.
     * 
     * @param cc
     *            This is the {@link CallerContext} of the application for which
     *            the player is constructed. If the CallerContext is null, it is
     *            assumed that the player is being created by the current
     *            context. If a CallerContext is passed in, we assume that the
     *            system context is creating the player on behalf of the
     *            application represented by the context sent in.
     * @param lock  The Object whose monitor will be used to protect Player state against concurrent modification
     * @param resourceUsage The ResourceUsage used to acquire resources
     */
    protected AbstractPlayer(CallerContext cc, Object lock, ResourceUsageImpl resourceUsage)
    {
        //allow values to be overridden
        ALARM_TIMER_SCHEDULE_FACTOR = Float.parseFloat(PropertiesManager.getInstance().getProperty
                ("ALARM_TIMER_SCHEDULE_FACTOR", String.valueOf(ALARM_TIMER_SCHEDULE_FACTOR)));
        ALARM_TIMER_MILLIS_FIRING_THRESHOLD = Integer.parseInt(PropertiesManager.getInstance().getProperty
                ("ALARM_TIMER_MILLIS_FIRING_THRESHOLD", String.valueOf(ALARM_TIMER_MILLIS_FIRING_THRESHOLD)));
        ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD = Integer.parseInt(PropertiesManager.getInstance().getProperty
                ("ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD", String.valueOf(ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD)));

        if (cc == null)
        {
            // the player was created by an application context,
            ownerCC = ccMgr.getCurrentContext();

            // Register a CCData that will close the Player when Xlet is
            // destroyed
            // IFF the player was NOT created by a ServiceContext. Players
            // created
            // by the ServiceContext must continue to exist after the
            // application
            // that called select() dies.
            //
            // If the Xlet closes the Player normally, then this this CCData
            // will be
            // removed when all Player resources are released. This
            // CCData.destroy()
            // would only be called if the Xlet terminated without first closing
            // the Player. It avoids a leak of Player resources.
            getOwnerCallerContext().addCallbackData(new CallbackData()
            {
                public void destroy(CallerContext ctx)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "closing player created by CallerContext " + ctx);
                    }
                    close();
                }

                public void pause(CallerContext ctx)
                { /* no-op */
                }

                public void active(CallerContext ctx)
                { /* no-op */
                }
            }, ccDataKeyClosePlayer);
        }
        else
        {
            // If cc is not null, then it is a CallerContext of an Xlet
            // that created the Player indirectly by calling
            // ServiceContext.select().
            // Register CCData to null out this reference if the owning app
            // dies,
            // which will avoid leaking the reference to the killed app.
            ownerCC = cc;
            getOwnerCallerContext().addCallbackData(new CallbackData()
            {
                public void destroy(CallerContext ctx)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "cleaning up reference to owner " + ctx);
                    }
                    ownerCC = null;
                }

                public void pause(CallerContext ctx)
                { /* no-op */
                }

                public void active(CallerContext ctx)
                { /* no-op */
                }
            }, ccDataKeyClosePlayer);
        }
        this.lock = lock;
        this.resourceUsage = resourceUsage;
        clock = new ClockImpl(id);
    }

    protected String getId()
    {
        return id;
    }
    
    public ResourceUsageImpl getResourceUsage()
    {
        return resourceUsage;
    }
    
    public boolean isMediaTimeSet()
    {
        return mediaTimeSet;
    }
    
    /**
     * This object serves as a key for a {@link CallbackData} that is registered
     * for an Xlet if the Xlet created the Player directly (i.e., via
     * {@link Manager#createPlayer(MediaLocator)}). The
     * {@link CallbackData#destroy(CallerContext)} method will call
     * {@link #close()} to free up Player resources.
     */
    private Object ccDataKeyClosePlayer = new Object();

    /*
     * org.cablelabs.impl.media.player.Player
     */

    /**
     * This is the {@link CallerContext} of the Xlet for which the player was
     * created.
     */
    private volatile CallerContext ownerCC = null;

    public CallerContext getOwnerCallerContext()
    {
        return ownerCC;
    }

    public int getOwnerPriority()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "getOwnerPriority()");
        }
        // Copy the owner CallerContext so it doesn't change out from under us.
        CallerContext cc = ownerCC;
        // If the owner is null or dead, return priority of zero.
        if (cc == null || !cc.isAlive())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "null or dead ownerCC; returning priority 0");
            }
            return 0;
        }
        // Get the priority.
        Integer pri = (Integer) cc.get(CallerContext.APP_PRIORITY);
        // If priority cannot be obtained (null), it is the system context, so
        // return
        // system context priority (256).
        if (pri == null)
            return SYSTEM_CONTEXT_PRIORITY;
        else
            return pri.intValue();
    }

    /** This is the {@link DataSource} that is being used by the Player. */
    private DataSource dataSource = null;

    public Presentation getPresentation()
    {
        synchronized (getLock())
        {
            return presentation;
        }
    }

    public DataSource getSource()
    {
        synchronized (getLock())
        {
            return isClosed() ? null : dataSource;
        }
    }

    public void setSource(DataSource source) throws IOException, IncompatibleSourceException
    {
        if (Asserting.ASSERTING) Assert.condition(source != null);

        synchronized (getLock())
        {
            // If closed, do nothing.
            if (isClosed()) return;

            // Cache the DataSource.
            dataSource = source;
        }
    }

    /*
     * Duration interface
     */

    public final Time getDuration()
    {
        synchronized (getLock())
        {
            if (isClosed())
            {
                return CLOSED_TIME;
            }

            return getSource().getDuration();
        }
    }

    /*
     * State Management
     */

    /** Keeps track of the current Player state. Initially, in Unrealized state. */
    private volatile int state = Unrealized;

    /** Target state if Player is in state transition. */
    private volatile int targetState = Unrealized;

    public int getState()
    {
        synchronized (getLock())
        {
            return state;
        }
    }

    public int getTargetState()
    {
        synchronized (getLock())
        {
            return targetState;
        }
    }

    /**
     * @return A String showing the state, target state, and presentation status
     *         of the Player.
     */
    protected String getStateString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(" [");
        sb.append(stateToString(state));
        if (targetState != state)
        {
            sb.append("-->");
            sb.append(stateToString(targetState));
        }
        if (isClosed()) sb.append(", CLOSED");
        sb.append("]");
        return sb.toString();
    }

    /**
     * Assert that a state is a recognized state.
     * 
     * @param st
     *            - The state to check.
     */
    private void assertValidState(int st)
    {
        if (Asserting.ASSERTING)
            Assert.condition((st == Unrealized || st == Realizing || st == Realized || st == Prefetching
                    || st == Prefetched || st == Started), "invalid state: " + st);
    }

    /**
     * Changes the {@link Player} state to a new state. This must only be called
     * from within a synchronize(getLock()) block.
     * 
     * @param newState
     *            This is the new state (e.g., {@link Controller#Started}) to
     *            change the Player to.
     * @throws IllegalStateException
     *             if the Player is closed.
     */
    private void setState(int newState)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "changing state from " + stateToString(state) + " to " + stateToString(newState));
        }

        assertValidState(newState);
        assertOpen("setState");

        state = newState;
    }

    /**
     * Changes the {@link Player} target state. This must only be called from
     * within a synchronize(getLock()) block.
     * 
     * @param newState
     *            This is the new target state (e.g.,
     *            {@link Controller#Realizing}) to change the Player to.
     * @throws IllegalStateException
     *             if the Player is closed.
     */
    private void setTargetState(int newState)
    {
        // if (Logging.LOGGING)
        // log.debug("changing targetState from "+stateToString(targetState)+" to "+stateToString(newState));

        assertValidState(newState);
        assertOpen("setTargetState");

        targetState = newState;
    }

    /**
     * Throw {@link NotRealizedError} if the Player has not been Realized. This
     * must only be called from within a synchronize(getLock()) block.
     * 
     * @param method
     *            Name of method from which this was called.
     */
    private void assertAtLeastRealized(String method)
    {
        if (state < Realized)
        {
            String msg = makeStateErrorMessage(method, state);
            SystemEventUtil.logRecoverableError(new Exception(msg));
            throw new NotRealizedError(msg);
        }
    }

    /**
     * Throw {@link NotPrefetchedError} if the Player is not Prefetched. This
     * must only be called from within a synchronize(getLock()) block.
     * 
     * @param method
     *            Name of method from which this was called.
     */
    private void assertPrefetched(String method)
    {
        if (state != Prefetched)
        {
            String msg = makeStateErrorMessage(method, state);
            SystemEventUtil.logRecoverableError(new Exception(msg));
            throw new NotPrefetchedError(msg);
        }
    }

    /**
     * Throw {@link ClockStartedError} if the Player is started. This must only
     * be called from within a synchronize(getLock()) block.
     * 
     * @param method
     *            Name of method from which this was called.
     */
    private void assertStopped(String method)
    {
        if (isStarted())
        {
            String msg = makeStateErrorMessage(method, state);
            SystemEventUtil.logRecoverableError(new Exception(msg));
            throw new ClockStartedError(msg);
        }
    }

    /**
     * Throw {@link ClockStoppedException} if the Player is not started. This
     * must only be called from within a synchronize(getLock()) block.
     * 
     * @param method
     *            Name of method from which this was called.
     * @throws ClockStoppedException
     *             if clock is stopped
     */
    private void assertStarted(String method) throws ClockStoppedException
    {
        if (isStopped())
        {
            String msg = makeStateErrorMessage(method, state);
            SystemEventUtil.logRecoverableError(new Exception(msg));
            throw new ClockStoppedException(msg);
        }
    }

    /**
     * Throw an {@link IllegalStateException} if the Player is not open. This
     * must only be called from within a synchronize(getLock()) block.
     * 
     * @param method
     *            Name of method from which this was called.
     */
    private void assertOpen(String method)
    {
        if (isClosed())
        {
            String msg = makeStateErrorMessage(method, state);
            SystemEventUtil.logRecoverableError(new Exception(msg));
            throw new IllegalStateException(msg);
        }
    }

    private String makeStateErrorMessage(String method, int state)
    {
        return getId() + method + " method not allowed in " + stateToString(state) + " state";
    }

    /** This indicates whether the Player is currently closed. */
    private boolean isClosed = false;

    /**
     * This must only be called from within a synchronize(getLock()) block.
     * 
     * @return True if Player is closed; otherwise, false.
     */
    protected boolean isClosed()
    {
        return isClosed;
    }

    /** Set flag indicating that Player is closed. */
    private void setClosed()
    {
        if (log.isInfoEnabled())
        {
            log.info(getId() + "setClosed");
        }
        isClosed = true;
    }

    /**
     * This must only be called from within a synchronize(getLock()) block.
     * 
     * @return True if the Player is in a stopped state (e.g.,
     *         {@link Controller#Prefetched}.
     */
    protected boolean isStopped()
    {
        return state < Started;
    }

    /**
     * This must only be called from within a synchronize(getLock()) block.
     * 
     * @return True if the Player is Started.
     */
    protected boolean isStarted()
    {
        return state == Started;
    }

    /*
     * Asynchronous Method Support
     */

    /**
     * This method is provided to allow subclasses access to the synchronization
     * object. Methods in {@link AbstractPlayer}, although they could access the
     * {@link #lock} field directly, should access it through this method.
     * 
     * @return Returns an Object that should be used for thread synchronization.
     */
    public Object getLock()
    {
        return lock;
    }

    /**
     * Base class of all {@link Task} implementation classes. The main
     * functionality provided is the {@link #handleError(Throwable)} method,
     * which provides a uniform treatment of all {@link Error}s thrown during an
     * asynchronous operation&mdash;namely, to close the player with an internal
     * error.
     */
    abstract class PlayerTask extends Task
    {
        PlayerTask(String name)
        {
            super(name);
        }

        abstract void doPlayerTask();

        protected final void doTask()
        {
            synchronized (getLock())
            {
                // Don't execute tasks for closed players.
                if (isClosed()) return;

                doPlayerTask();
            }
        }

        /**
         * Handle {@link Error} being thrown by {@link #doTask()}.
         * 
         * @param e
         *            The {@link Error} thrown.
         */
        protected void handleError(Throwable e)
        {
            String msg = getId() + "startDecode() failed: " + e;
            SystemEventUtil.logRecoverableError(msg, e);
            postEvent(new InternalErrorEvent(AbstractPlayer.this, msg));
        }
    }

    /**
     * This is the {@link TaskQueue} that is used for carrying out asynchronous
     * work on the {@link Player}. It is obtained from the system
     * {@link CallerContext} since all asynchronous work will be carried out on
     * behalf of the system and not the owning application.
     * <p>
     * Note that this <code>TaskQueue</code> is also used for timer
     * notifications. Any changes to the how this <code>TaskQueue</code> is
     * created (i.e., with which {@link CallerContext}) should be made in
     * concert with changes to how the {@link #startTimer} is constructed.
     */
    private static final TaskQueue taskQueue = ccMgr.getSystemContext().createTaskQueue();

    public TaskQueue getTaskQueue()
    {
        return taskQueue;
    }

    /**
     * Post (enqueue) a {@link Runnable} to be executed asynchronously on thread
     * owned by a {@link TaskQueue} associated with the system
     * {@link CallerContext}. Runnables are guaranteed to be executed in the
     * order they are posted.
     * 
     * @param task
     *            - {@link PlayerTask} to post
     */
    protected void runAsync(PlayerTask task)
    {
        taskQueue.post(task);
    }

    /*
     * Event Dispatching
     */

    static class ControllerEventDispatcher extends CallerContextEventMulticaster
    {
        public void dispatch(EventListener listeners, EventObject event)
        {
            if (listeners instanceof ControllerListener && event instanceof ControllerEvent)
                ((ControllerListener) listeners).controllerUpdate((ControllerEvent) event);
            else if (Asserting.ASSERTING) Assert.condition(false, "listeners or event of wrong type");
        }
    }

    private CallerContextEventMulticaster multicaster = new ControllerEventDispatcher();

    public final void addControllerListener(ControllerListener l)
    {
        if (l == null) return;

        synchronized (getLock())
        {
            if (isClosed()) return;

            multicaster.addListenerMulti(l);
        }
    }

    public final void removeControllerListener(ControllerListener l)
    {
        if (l == null) return;

        synchronized (getLock())
        {
            if (isClosed()) return;

            multicaster.removeListener(l);
        }
    }

    /**
     * Post (deliver) a {@link ControllerEvent} to all registered
     * {@link ControllerListener}s. Execute the
     * {@link ControllerListener#controllerUpdate(ControllerEvent)} method using
     * a thread from the {@link CallerContext} for which the listener was added.
     * 
     * @param event
     *            -The {@link ControllerEvent} to post.
     */
    protected void postEvent(ControllerEvent event)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "postEvent(" + event + ")" + getStateString());
            }

            // If already closed, don't send any more events.
            if (isClosed())
            {
                if (log.isInfoEnabled())
                {
                    log.info(getId() + "player closed, not processing event");
                }
                return;
            }
            //callbackdata is removed in releaseAllResources - notify prior to calling releaseAllResources 
            multicaster.multicast(event);

            // If this was a ControllerClosedEvent, close the Player.
            if (event instanceof ControllerClosedEvent)
            {
                if (log.isInfoEnabled())
                {
                    log.info(getId() + "event is an instance of ControllerClosedEvent - releasing all resources and closing");
                }
                // Release all resources.
                releaseAllResources();

                // Close the Player and notify any listeners.
                setClosed();
            }
        }

        if (event instanceof ControllerClosedEvent)
        {
            multicaster.cleanup();
            multicaster = null;
        }
    }

    /*
     * Control Support
     */

    /**
     * List of {@link Control} supported by the {@link Player}. Subclass
     * constructors should populate this list by calling
     * {@link #addControls(ControlBase[])} or
     * {@link #addControls(ControlBase[])}. These methods don't check for
     * duplicates. It is expected that the code will be correct that adds
     * controls.
     */
    private Vector controls = new Vector();

    /**
     * Add a list of {@link ControlBase} instances to the list of supported
     * {@link Control}s for the player. This method should be called by the
     * player constructor.
     * 
     * @param ctrls
     *            An array of {@link ControlBase} instances to add.
     */
    protected void addControls(ControlBase[] ctrls)
    {
        controls.addAll(Arrays.asList(ctrls));
    }

    /**
     * This abstract inner class is the base for all {@link Control}
     * implementation classes that are associated with a
     * {@link org.cablelabs.impl.media.player.AbstractPlayer PlayerBase}. A
     * {@link Control} is either enabled or disabled. When disabled, all
     * operations should fail apprpriately. To enable/disable a {@link Control},
     * call {@link #setEnabled(boolean)}. When constructed with a no-argument
     * constructor ({@link #ControlBase()}), the initial state is disabled. To
     * explicitly specify the state at construction, use
     * {@link #ControlBase(boolean)}.
     */
    protected abstract class ControlBase implements Control
    {
        private boolean enabled;

        protected ControlBase()
        {
        }

        protected ControlBase(boolean isEnabled)
        {
            enabled = isEnabled;
        }

        protected boolean isEnabled()
        {
            return enabled;
        }

        protected void setEnabled(boolean b)
        {
            enabled = b;
        }

        /**
         * Release all resources being used by the {@link ControlBase}. Default
         * implementation does nothing.
         */
        protected void release()
        {
        }

        // This implementation does not implement visual control components.
        public Component getControlComponent()
        {
            return null;
        }

    }

    public final Control[] getControls()
    {
        synchronized (getLock())
        {
            if (isClosed()) return new Control[0];

            Vector v = new Vector();
            for (int i = 0; i < controls.size(); ++i)
            {
                ControlBase ctrl = (ControlBase) controls.elementAt(i);
                if (ctrl.isEnabled()) v.add(ctrl);
            }

            Control[] results = (Control[]) v.toArray(new Control[v.size()]);
            if (log.isTraceEnabled()) 
            {
                log.trace("getControls - returning: " + org.cablelabs.impl.util.Arrays.toString(results));
            }
            return results;
        }
    }

    public Control getControl(String forName)
    {
        if (forName == null) throw new NullPointerException("null Control name");

        // Lookup the Class object for 'forName'. If not found, return null.
        Class ctrlClass;
        try
        {
            ctrlClass = Class.forName(forName);
        }
        catch (ClassNotFoundException e)
        {
            if (log.isWarnEnabled()) 
            {
                log.warn("getControl - unable to resolve class for: " + forName);
            }
            return null;
        }

        // Iterate through the controls, looking for one that is an instanceof
        // the 'ctrlClass'.
        Control[] allControls = getControls();
        for (int i = 0; i < allControls.length; ++i)
        {
            Control ctrl = allControls[i];
            if (ctrlClass.isInstance(ctrl))
            {
                if (log.isDebugEnabled()) 
                {
                    log.debug("getControl - returning a control for: " + forName);
                }
                return ctrl;
            }
        }
        // If we got here, we didn't find it.
        if (log.isWarnEnabled()) 
        {
            log.warn("getControl - unable to find an available control for: " + forName);
        }
        return null;
    }

    /*
     * Player Methods
     */

    public Component getControlPanelComponent()
    {
        synchronized (getLock())
        {
            assertAtLeastRealized("getControlPanelComponent");
            // Stack doesn't support this component.
            return null;
        }
    }

    public GainControl getGainControl()
    {
        synchronized (getLock())
        {
            assertAtLeastRealized("getGainControl");
            // Stack doesn't implement a gain control.
            return null;
        }
    }

    public Component getVisualComponent()
    {
        synchronized (getLock())
        {
            if (isClosed()) return null;

            assertAtLeastRealized("getVisualComponent");
            return doGetVisualComponent();
        }
    }

    /**
     * This method should be defined by subclasses to return a VisualComponent
     * appropriate for the {@link Player} subtype.
     * 
     * @return an AWT {@link Component} that contains and manages the
     *         {@link Player}
     */
    protected abstract Component doGetVisualComponent();

    public void addController(Controller controller) throws IncompatibleTimeBaseException
    {
        // not allowed per OCAP spec
        throw new IncompatibleTimeBaseException("OCAP does not allow Controllers to be added to Players");
    }

    public void removeController(Controller oldController)
    {
        // no-op for OCAP
    }

    public Time getStartLatency()
    {
        synchronized (getLock())
        {
            assertAtLeastRealized("getStartLatency");
            // Since start latency is really only useful when Player/Controller
            // architecture
            // is supported, just return unknown latency since it doesn't
            // matter.
            return LATENCY_UNKNOWN;
        }
    }

    /*
     * Clock Methods
     */

    /** @return Returns the {@link ClockImpl} being used by the player. */
    public PlayerClock getClock()
    {
        return clock;
    }

    public long getMediaNanoseconds()
    {
        return clock.getMediaNanoseconds();
    }

    private Time getPresentationMediaTime()
    {
        Time retTime = clock.getMediaTime();
        if (presentation != null)
        {
            retTime = presentation.getMediaTime();
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "getPresentationMediaTime - returning: " + retTime);
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "getPresentationMediaTime - presentation is null - returning clock mediatime: " + retTime);
            }
        }
        return retTime;
    }

    /**
     * Retrieve the presentation mediatime if available, or player clock mediatime otherwise.
     *
     * @return media time
     */
    public Time getMediaTime()
    {
        synchronized (getLock())
        {
            Time retTime = null;
            if (isClosed())
            {
                retTime = CLOSED_TIME;
            }
            else
            {
                // Get the media time from presentation if it is not null
                // Presentation time is exact playing time it is more acurate
                // than clock

                if (presentation != null)
                {
                    retTime = presentation.getMediaTime();
                    if (log.isTraceEnabled())
                    {
                        log.trace(getId() + "getMediaTime - returning mediatime from presentation: " + retTime);
                    }
                }
                // if presentation time is null or unable to get time then get
                // the time from clock
                if (retTime == null)
                {
                    retTime = getClock().getMediaTime();
                    if (log.isTraceEnabled())
                    {
                        log.trace(getId() + "getMediaTime - presentation was null or returned a null mediatime - returning mediatime from player clock: "
                                + retTime);
                    }
                }
            }

            return retTime;
        }
    }

    public float getRate()
    {
        synchronized (getLock())
        {
            if (isClosed()) return 0;

            // Get the current rate from the clock.
            return getClock().getRate();
        }
    }

    public Time getStopTime()
    {
        synchronized (getLock())
        {
            if (isClosed()) return CLOSED_TIME;

            return getClock().getStopTime();
        }
    }

    public Time getSyncTime()
    {
        synchronized (getLock())
        {
            if (isClosed()) return CLOSED_TIME;

            return getClock().getSyncTime();
        }
    }

    public TimeBase getTimeBase()
    {
        synchronized (getLock())
        {
            if (isClosed()) return Manager.getSystemTimeBase();

            assertAtLeastRealized("getTimeBase");

            return getClock().getTimeBase();
        }
    }

    public Time mapToTimeBase(Time mt) throws ClockStoppedException
    {
        if (mt == null) throw new NullPointerException("null media time");

        if (mt.getNanoseconds() < 0)
            throw new IllegalArgumentException("invalid media time (" + mt.getNanoseconds() + ")");

        synchronized (getLock())
        {
            if (isClosed())
                return CLOSED_TIME;

            assertStarted("mapToTimeBase");

            Time returnValue = getClock().mapToTimeBase(mt);
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "mapToTimeBase " + mt + " returning: " + returnValue);
            }
            return returnValue;
        }
    }

    public void setMediaTime(Time mt)
    {
        setMediaTime(mt, true);
    }

    public void setMediaTime(Time mt, boolean sendEvent)
    {
        if (mt == null) throw new NullPointerException("null media time");
        if (mt.getNanoseconds() < 0)
            throw new IllegalArgumentException("invalid media time (" + mt.getNanoseconds() + ")");

        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "setMediaTime: " + mt + " - " + getStateString());
            }

            // Do nothing if closed.
            if (isClosed())
            {
                clock.setMediaTime(CLOSED_TIME);
                return;
            }

            // Must be Realized or greater state.
            assertAtLeastRealized("setMediaTime");
            if (getResourceUsage().isResourceUsageEAS())
            {
                throw new IllegalStateException("Unable to set media time - presenting EAS");
            }
            mediaTimeSet = true;
            // If the player is presenting, set the media time on the presentation; otherwise, set it directly on the clock.
            if (isPresenting())
            {
                //presentation.setMediatime is asynchronous - presentation implementation will update the player clock and trigger a mediatime event
                presentation.setMediaTime(mt);
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "is presenting - setMediaTime called on presentation: " + mt);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "not presenting - updating clock with requested mediatime: " + mt);
                }
                clockSetMediaTime(mt, sendEvent);
            }
        }
    }

    public void clockSetMediaTime(Time mt, boolean postMediaTimeEvent)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "clockSetMediaTime - current mediaTime: " + getClock().getMediaTime() + ", updated mediaTime: " + mt + ", post event: " + postMediaTimeEvent);
        }
        synchronized (getLock())
        {
            // If clock is started, stop it before (and restart it after)
            // setting the time.
            boolean clockStarted = getClock().isStarted();
            getClock().stop();
            // Update alarms PRIOR to updating clock media time

            LinkedList alarmCopy = new LinkedList(alarms);
            for (Iterator it = alarmCopy.iterator(); it.hasNext();)
            {
                AlarmImpl alarm = (AlarmImpl) it.next();
                alarm.notifyTimeChange(mt.getNanoseconds());
            }
            if (log.isInfoEnabled())
            {
                log.info(getId() + "clock mediatime changed from: " + getClock().getMediaTime() + " to " + mt + ", clock started: " + clockStarted);
            }
            getClock().setMediaTime(mt);
            if (clockStarted)
            {
                getClock().syncStart(new Time(0));
            }
            if (postMediaTimeEvent)
            {
                postEvent(new MediaTimeSetEvent(this, mt));
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "triggering mediatimesetevent: " + mt);
                }
            }
        }
    }

    public float setRate(float rate)
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "setRate(" + rate + ")" + getStateString());
            }

            // If closed, don't do anything.
            if (isClosed())
            {
                if (log.isInfoEnabled())
                {
                    log.info(getId() + "closed - unable to set rate - returning 0");
                }
                return 0;
            }

            // Must be Realized or greater state.
            assertAtLeastRealized("setRate");

            // If presenting, set media time via presentation; otherwise, set whatever value was passed in.
            float actualRate;
            if (isPresenting())
            {
                actualRate = presentation.setRate(rate);
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "presenting - calling presentation.setRate with value: " + rate + ", result: "
                            + actualRate);
                }
            }
            else
            {
                actualRate = rate;
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "not currently presenting - not calling presentation.setRate, using requested rate: "
                            + actualRate);
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "updating clock with rate: " + actualRate);
            }
            clockSetRate(actualRate, true);

            return actualRate;
        }
    }

    public void clockSetRate(float newRate, boolean postRateChangeEvent)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "clockSetRate: " + newRate);
        }
        synchronized (getLock())
        {
            if (isClosed())
                return;

            // If clock is started, stop it before (and restart it after)
            // setting the rate.
            boolean clockStarted = getClock().isStarted();
            getClock().stop();
            // Update all alarms PRIOR to setting clock rate
            LinkedList alarmCopy = new LinkedList(alarms);
            for (Iterator it = alarmCopy.iterator(); it.hasNext();)
            {
                AlarmImpl alarm = (AlarmImpl) it.next();
                alarm.notifyRateChange(newRate);
            }
            if (log.isInfoEnabled())
            {
                log.info(getId() + "clock rate changed from: " + getClock().getRate() + " to " + newRate);
            }

            getClock().setRate(newRate);
            if (clockStarted)
            {
                getClock().syncStart(new Time(0));
            }
            if (postRateChangeEvent)
            {
                // Send RateChangeEvent.
                postEvent(new RateChangeEvent(this, newRate));
            }
        }
    }

    class StopMediaTimeCallback implements Callback
    {
        public void fired(Alarm alarm)
        {
            synchronized (getLock())
            {
                // If this is firing on a 'defunct' (prior) alarm, ignore it.
                if (stopMediaTimeAlarmForward != alarm && stopMediaTimeAlarmReverse != alarm) return;

                // Alarm is valid, so stop the player.
                stop(new StopAtTimeEvent(AbstractPlayer.this, Started, Prefetched, Prefetched, getMediaTime()));

                // Notify threads that are waiting on state change.
                getLock().notifyAll();
            }
        }

        public void destroyed(Alarm alarm, AlarmException reason)
        {
            synchronized (getLock())
            {
                // If this is firing on a 'defunct' (prior) alarm, ignore it.
                if (stopMediaTimeAlarmForward != alarm && stopMediaTimeAlarmReverse != alarm) return;

                // TODO(mas): This should probably close the player.
                SystemEventUtil.logRecoverableError(getId() + "stop alarm destroyed", reason);
            }
        }
    }

    private final Callback stopMediaTimeCallback = new StopMediaTimeCallback();

    private Alarm stopMediaTimeAlarmForward = null;

    private Alarm stopMediaTimeAlarmReverse = null;

    /**
     * Activate the stopMediaTimeAlarm if one exists and the player is started
     */
    private void activateStopMediaTimeAlarm()
    {
        if (stopMediaTimeAlarmForward != null && stopMediaTimeAlarmReverse != null && isStarted())
        {
            try
            {
                stopMediaTimeAlarmForward.activate();
                stopMediaTimeAlarmReverse.activate();
            }
            catch (AlarmException x)
            {
                SystemEventUtil.logUncaughtException(x);
                throw new IllegalStateException(getId() + "could activate the stop time alarm");
            }
        }
    }

    public void setStopTime(Time stopMT)
    {
        if (stopMT == null) throw new NullPointerException("null stop time");
        if (stopMT.getNanoseconds() < 0)
            throw new IllegalArgumentException("invalid stop time (" + stopMT.getNanoseconds() + ")");

        synchronized (getLock())
        {
            if (isClosed()) return;

            assertAtLeastRealized("setStopTime");

            // If the new stop time is different than the current stop time,
            // assign it to the clock, send the event, and set the alarm.
            Time oldStopMT = getClock().getStopTime();
            getClock().setStopTime(stopMT);
            long stopNS = stopMT.getNanoseconds();
            if (oldStopMT != stopMT && oldStopMT.getNanoseconds() != stopNS)
            {
                // Post the event.
                postEvent(new StopTimeChangeEvent(this, stopMT));

                // Clear the current alarm, if there is one.
                if (stopMediaTimeAlarmForward != null)
                {
                    destroyAlarm(stopMediaTimeAlarmForward);
                    stopMediaTimeAlarmForward = null;
                }
                if (stopMediaTimeAlarmReverse != null)
                {
                    destroyAlarm(stopMediaTimeAlarmReverse);
                    stopMediaTimeAlarmReverse = null;
                }

                // Create a new alarm for the new stop time, if not the RESET
                // time and in the future.
                if (stopMT != Clock.RESET && getClock().getMediaNanoseconds() < stopNS)
                {
                    AlarmSpec specForward = new FixedAlarmSpec("stopMediaTimeForward", stopMT.getNanoseconds(),
                            AlarmSpec.Direction.FORWARD);
                    stopMediaTimeAlarmForward = createAlarm(specForward, stopMediaTimeCallback);
                    AlarmSpec specReverse = new FixedAlarmSpec("stopMediaTimeReverse", stopMT.getNanoseconds(),
                            AlarmSpec.Direction.REVERSE);
                    stopMediaTimeAlarmReverse = createAlarm(specReverse, stopMediaTimeCallback);

                    activateStopMediaTimeAlarm();
                }
            }

        }
    }

    public void setTimeBase(TimeBase master) throws IncompatibleTimeBaseException
    {
        synchronized (getLock())
        {
            if (isClosed())
                return;

            assertStopped("setTimeBase");
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "updating player timebase to: " + master);
            }

            getClock().setTimeBase(master);
        }
    }

    /*
     * Resource Methods
     */

    /** Keeps track of how many resources have been lost. */
    private int lostResourceCount = 0;

    protected boolean resourcesAreWithdrawn()
    {
        synchronized (getLock())
        {
            return lostResourceCount > 0;
        }
    }

    protected void lostResource()
    {
        synchronized (getLock())
        {
            // If this is the first resource lost, send the event.
            if (lostResourceCount == 0) postEvent(new ResourceWithdrawnEvent(this));
            // Increment the lost resource count.
            ++lostResourceCount;
        }
    }

    /**
     * Called whenever a resource is regained--mainly, by tuning back and by
     * re-reserving the video device as a result of
     * {@link org.dvb.media.BackgroundVideoPresentationControl#setVideoTransformation(VideoTransformation)}
     * .
     */
    protected void regainedResource()
    {
        synchronized (getLock())
        {
            // Decrement lost resource count.
            if (lostResourceCount > 0)
            {
                --lostResourceCount;
                // Send event if all resources have been returned.
                if (lostResourceCount == 0) postEvent(new ResourceReturnedEvent(this));
            }
        }
    }

    /*
     * State Transition Methods
     */

    /*
     * deallocate() implementation
     */

    private boolean abort = false;

    public void deallocate()
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "deallocate()" + getStateString());
            }

            // Do nothing if closed.
            if (isClosed())
                return;

            // Can only be called from a Stopped state.
            assertStopped("deallocate");

            // Save current state, which becomes the prior state, for
            // DeallocateEvent below.
            int priorState = state;

            switch (state)
            {
                case Unrealized:
                case Realized:
                    // Stay in same state.
                    break;

                case Realizing:
                    abortRealizing();
                    // Go back to Unrealized.
                    setState(Unrealized);
                    break;

                case Prefetching:
                    abortPrefetching();
                    // Go back to Realized.
                    setState(Realized);
                    break;

                case Prefetched:
                    // Transition back to Realized.
                    setState(Realized);
                    break;
            }

            // Set target state same as new state.
            setTargetState(state);
            // Release resources, based on new state.
            releaseResources(state);
            // Notify any threads that might be awaiting state change.
            getLock().notifyAll();
            // Send the event to notify listeners that deallocation occurred.
            postEvent(new DeallocateEvent(this, priorState, state, state, getMediaTime()));
        }
    }

    private void abortRealizing()
    {
        // Set flag to indicate realizing should be aborted.
        abort = true;
        // Interrupt realize task; if it was running, wait for it to finish.
        if (realizeTask.abort() == Task.RUNNING)
            waitForAbort();
        else
            abort = false; // reset abort flag because it won't get reset
    }

    private void abortPrefetching()
    {
        // Set flag to indicate prefetching should be aborted.
        abort = true;
        // Interrupt prefetch task; if it was running, wait for it to finish.
        if (prefetchTask.abort() == Task.RUNNING)
            waitForAbort();
        else
            abort = false; // reset abort flag because it won't get reset
    }

    private void waitForAbort()
    {
        while (abort)
            try
            {
                getLock().wait();
            }
            catch (InterruptedException x)
            {
                // ignore
            }
    }

    /**
     * This method should release resources held by the player to return it to
     * resources holdings it would have in the specified state. This should only
     * be called while the Player is stopped.
     * 
     * @param targetResourceState
     *            The target state to release resources for.
     */
    private void releaseResources(int targetResourceState)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "releaseResources(" + stateToString(targetResourceState) + ")");
        }

        if (targetResourceState < Prefetched)
        {
            mediaTimeSet = false;
            doReleasePrefetchedResources();
        }

        if (targetResourceState < Realized)
        {
            doReleaseRealizedResources();
        }
    }

    /**
     * Releases resources held by the {@link Player}. This must only be called
     * when synchronized on the {@link #lock}.
     */
    private void releaseAllResources()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "releaseAllResources");
        }
        // First, allow subclasses to release their resources..
        doReleaseAllResources();

        // Invalidate all controls.
        Control[] ctrls = getControls();
        for (int i = 0; i < ctrls.length; ++i)
        {
            ControlBase control = (ControlBase) ctrls[i];
            control.setEnabled(false);
            control.release();
        }

        // Clear pending timers.
        clearTimers();

        // Release all resources being held by the Player.
        releaseResources(Unrealized);

        // Disconnect the DataSource.
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "disconnecting from datasource");
        }
        if (dataSource != null)
        {
            dataSource.disconnect();
        }

        // Remove the CCData callback.
        CallerContext context = getOwnerCallerContext();
        if (context != null)
        {
            context.removeCallbackData(ccDataKeyClosePlayer);
        }

        // Null out the references to external objects.
        dataSource = null;
        resourceUsage = null;
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "releaseAllResources complete");
        }
    }

    protected abstract void doReleaseAllResources();

    /*
     * close() implementation
     */

    public void close()
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "close()" + getStateString());
            }

            // If Closed, do nothing.
            if (isClosed())
                return;

            switch (state)
            {
                case Started:
                    // Not allowed on Started Player, according to Section 8.2
                    // of JMF tutorial.
                    // However, the TCK tests consistenly call close without
                    // calling stop first, expecting it to succeed.
                    if (presentation != null) presentation.stop();
                    presentation = null;
                    break;

                case Unrealized:
                case Realized:
                case Prefetched:
                    // Not an in-progress state, so just release resources
                    // (handled after switch statement).
                    break;

                case Realizing:
                    abortRealizing();
                    break;

                case Prefetching:
                    abortPrefetching();
                    break;
            }

            // Send the event. dispatchEvent() will take care of releasing
            // resources and setting state to closed.
            postEvent(new ControllerClosedEvent(this));
        }
    }

    /*
     * start() implementation
     */

    public void start()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "start() " + getStateString());
            }

            // Do nothing if Closed.
            if (isClosed())
            {
                if (log.isInfoEnabled()) 
                {
                    log.info(getId() + "start() - player closed - not starting");
                }
                return;
            }

            // Get the current target state. If it is not Started, change it to
            // Started.
            if (targetState < Started)
            {
                setTargetState(Started);
            }

            switch (state)
            {
                case Started:
                    // If already Started, send the StartEvent again, using
                    // cached sync and media start times.
                    postEvent(new StartEvent(this, Started, Started, Started, mediaStartTime, syncStartTime));
                    return;

                case Unrealized:
                case Realized:
                case Realizing:
                    // Call prefetch() to transition Player to target Started
                    // state.
                    prefetch();
                    break;

                case Prefetching:
                    // Nothing to do since target state is set to Started. When
                    // it finishes Prefetching,
                    // it will automatically call syncStart().
                    break;

                case Prefetched:
                    // Already Prefetched, so just start it using current time
                    // base time.
                    syncStart(getTimeBase().getTime());
                    break;
            }
        }
    }

    /**
     * This is a specialization of {@link Task} that handles special behavior
     * for the resource acquisition states (realizing, prefetching).
     */
    private abstract class ResourceTask extends PlayerTask
    {
        ResourceTask(String name)
        {
            super(name);
        }

        protected final void doPlayerTask()
        {
            // Call abstract method to acquire resources.
            Object err = acquireResources();
            // Complete the resource acquisition state.
            completeState(err);
        }

        abstract Object acquireResources();

        abstract void completeState(Object error);
    }

    /*
     * realize() implementation
     */

    protected class RealizeTask extends ResourceTask
    {
        RealizeTask()
        {
            super("RealizeTask");
        }

        Object acquireResources()
        {
            return acquireRealizeResources();
        }

        void completeState(Object error)
        {
            completeRealizing(error);
        }
    }

    /**
     * The RealizeTask that is currently executing. It is null if it is not
     * executing.
     */
    private RealizeTask realizeTask = null;

    public void realize()
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "realize()" + getStateString());
            }

            // Don't do anything if closed.
            if (isClosed())
                return;

            // If target state is Unrealized, change it to Realized.
            // Otherwise, leave as is.
            if (targetState == Unrealized)
                setTargetState(Realized);

            switch (state)
            {
                case Realized:
                case Prefetching:
                case Prefetched:
                case Started:
                    // If already Realized, send the RealizeCompleteEvent.
                    postEvent(new RealizeCompleteEvent(this, state, state, targetState));
                    break;

                case Realizing:
                    // If already Realizing, nothing to do. Only one
                    // TransitionEvent is sent.
                    break;

                case Unrealized:
                    // Put it in the realizing state.
                    setState(Realizing);
                    // Send the transition event.
                    postEvent(new TransitionEvent(this, Unrealized, Realizing, targetState));
                    // Do the asynchronous work of the realize() method.
                    realizeTask = new RealizeTask();
                    runAsync(realizeTask);
                    // Notify any threads that may be awaiting state change.
                    getLock().notifyAll();
                    break;
            }
        }
    }

    private Object acquireRealizeResources()
    {
        return doAcquireRealizeResources();
    }

    /**
     * This method should be defined by subclasses to do the asynchronous work
     * of the {@link #realize()} method. This method is invoked by
     * {@link AbstractPlayer#acquireRealizeResources() ()}.
     * 
     * @return Returns null if successful; otherwise, and object indicating the
     *         error.
     */
    protected abstract Object doAcquireRealizeResources();

    /**
     * Undo the work (resource allocations) that was done by the
     * {@link #doAcquireRealizeResources()} method. This method
     * may be invoked by the {@link #deallocate()} and {@link #close()} methods.
     *
     */
    protected abstract void doReleaseRealizedResources();

    /**
     * This method is called to complete the Realizing state. If Realizing is
     * successful (
     * 
     * @param error
     *            is <code>null</code>), it transitions the Player to the
     *            Realized state; if not successful (
     * @param error
     *            is not null), it transitions back to the Unrealized state.
     *            <p>
     *            Realizing will be aborted if either {@link #deallocate()} or
     *            {@link #close()} is called while Realizing; this is idicated
     *            by the {@link #abort} field being set to true. In this case,
     *            no state change occurs in this mehod; it is the caller's
     *            responsibility to change state appropriately.
     * @param error
     *            This is an Object that indicates whether an error occurred
     *            during Realizing (in
     *            {@link #doAcquireRealizeResources()}). If
     *            <code>null</code>, no error occurred; otherwise, this is an
     *            Object that contains error information.
     */
    private void completeRealizing(Object error)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "completeRealizing(" + (error == null ? "success" : ("failure:" + error)) + ")"
                        + getStateString());
            }

            if (!isClosed())
            {
                // clear out the Task reference
                realizeTask = null;

                // close() or deallocate() called before we got here
                if (abort)
                {
                    abort = false;
                }
                // close() / deallocate() weren't called before we got here.
                else
                {
                    // successful case
                    if (error == null)
                    {
                        setState(Realized);
                        postEvent(new RealizeCompleteEvent(this, Realizing, Realized, targetState));
                        if (targetState >= Prefetched) prefetch();
                    }
                    // failure case
                    else
                    {
                        setState(Unrealized);
                        setTargetState(Unrealized);
                        postEvent(new ResourceUnavailableEvent(this, getId() + "realize failed: " + error));
                    }
                }
            }

            // Notify threads that are awaiting completion of Realizing state.
            getLock().notifyAll();
        }
    }

    /*
     * prefetch() implementation
     */

    protected class PrefetchTask extends ResourceTask
    {
        PrefetchTask()
        {
            super("PrefetchTask");
        }

        Object acquireResources()
        {
            return acquirePrefetchResources();
        }

        void completeState(Object error)
        {
            completePrefetching(error);
        }
    }

    /**
     * This is the PrefetchAsyncOp that is currently executing asynchronously.
     * It is null if none is executing.
     */
    private PrefetchTask prefetchTask = null;

    public void prefetch()
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "prefetch()" + getStateString());
            }

            // If closed, don't do anything.
            if (isClosed())
                return;

            // Adjust target state if it's lower than Prefetching.
            if (targetState < Prefetching)
                setTargetState(Prefetched);

            switch (state)
            {
                case Prefetched:
                case Started:
                    // When already Prefetched, re-send PrefetchCompleteEvent.
                    postEvent(new PrefetchCompleteEvent(this, state, state, targetState));
                    return;

                case Realizing:
                    // Don't need to do anything here since completeRealize()
                    // will invoke prefetch()
                    // because targetState has been set to Prefetched (or
                    // higher).
                    return;

                case Prefetching:
                    // Nothing to do if currently Prefetching; two prefetch
                    // calls results in one event.
                    return;

                case Unrealized:
                    // The controller is not realized yet, so implicitly invoke
                    // realize().
                    // When realize() completes, it will invoke prefetch()
                    // because of the targetState
                    // being greater than Realized.
                    realize();
                    return;

                case Realized:
                    // Change the state to Prefetching.
                    setState(Prefetching);
                    // Send the transtition event.
                    postEvent(new TransitionEvent(this, Realized, Prefetching, targetState));
                    // Kick off the asynchronous work.
                    prefetchTask = new PrefetchTask();
                    runAsync(prefetchTask);
                    // Notify threads awaiting state change.
                    getLock().notifyAll();
                    break;
            }
        }
    }

    private Object acquirePrefetchResources()
    {
        return doAcquirePrefetchResources();
    }

    /**
     * Subclass should define this method to do the work of the
     * {@link PrefetchTask}. This is invoked by
     * {@link ResourceTask#acquireResources()}.
     * 
     * @return Returns null if successful; otherwise, an Object indicating the
     *         error.
     */
    protected abstract Object doAcquirePrefetchResources();

    /**
     * This method undoes the work that was done by
     * {@link #doAcquirePrefetchResources()}. This involves
     * releasing any resources that were acquired during the Prefetching state.
     * This method may be invoked by {@link #deallocate()} or {@link #close()}.
     *
     */
    protected abstract void doReleasePrefetchedResources();

    /**
     * This method is called to complete the Prefetching state. If Prefetching
     * is successful (
     * 
     * @param error
     *            is <code>null</code>), it transitions the Player to the
     *            Prefetched state; if not successful (
     * @param error
     *            is not null), it transitions back to the Realized state.
     *            <p>
     *            Prefetching will be aborted if either {@link #deallocate()} or
     *            {@link #close()} is called while Prefetching; this is idicated
     *            by the {@link #abort} field being set to true. In this case,
     *            no state change occurs in this mehod; it is the caller's
     *            responsibility to change state appropriately.
     * @param error
     *            This is an Object that indicates whether an error occurred
     *            during Prefetching (in
     *            {@link #doAcquireRealizeResources()}). If
     *            <code>null</code>, no error occurred; otherwise, this is an
     *            Object that contains error information.
     */
    private void completePrefetching(Object error)
    {
        synchronized (getLock())
        {
            if (error == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "completePrefetching(success) - " + getStateString());
                }
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(getId() + "completePrefetching(failure- " + error + ") - " + getStateString());
                }
            }

            if (!isClosed())
            {
                // Clear out the Task reference.
                prefetchTask = null;

                // close() or deallocate() called before we got here
                if (abort)
                {
                    abort = false;
                }
                // close() / deallocate() weren't called before we got here.
                else
                {
                    if (error == null)
                    {
                        //construct the presentation prior to posting PrefetchComplete and before starting the clock, so initial requested mediatime is available
                        //may re-enter prefetching state, only create the presentation once until it is nulled out again due to state change
                        if (presentation == null)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(getId() + "calling createPresentation");
                            }
                            presentation = createPresentation();
                        }
                        
                        setState(Prefetched);
                        postEvent(new PrefetchCompleteEvent(this, Prefetching, Prefetched, targetState));
                        // Transition to Started state if it is the target.
                        if (targetState == Started) syncStart(getTimeBase().getTime());
                    }
                    else
                    {
                        setState(Realized);
                        setTargetState(Realized);
                        postEvent(new ResourceUnavailableEvent(this, getId() + "prefetch failed: " + error));
                    }
                }
            }

            // Notify threads that are awaiting a state change.
            getLock().notifyAll();
        }
    }

    /*
     * Presentation Management
     */

    /**
     * Reference to the {@link Presentation} that is being managed by the
     * player.
     */
    protected Presentation presentation;

    /**
     * This abstract method must be defined by player implementation classes to
     * return an appropriate {@link Presentation} subtype.
     * 
     * @return Return a reference to the {@link Presentation} interface..
     */
    protected abstract Presentation createPresentation();

    /**
     * This {@link PlayerTask} is responsible for starting a Presentation
     * asynchronously.
     */
    final class StartPresentationTask extends PlayerTask
    {
        StartPresentationTask()
        {
            super("StartPresentationTask");
        }

        /**
         * Attempt to create and start a {@link Presentation} for the player. If
         * successful, the newly created {@link Presentation} is assigned to the
         * {@link AbstractPlayer#presentation} field.
         */
        protected final void doPlayerTask()
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "StartPresentationTask - " + getStateString());
            }

            // Ignore if player target state is -not- Started or player is
            // closed(This could happen
            // if stop() or close() are called after start() but before the
            // presentation could begin.)
            if (isClosed() || targetState != Started)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(getId() + "unable to start presentation because player is closed or target state is no longer Started - target state: "
                            + targetState + ", closed: " + isClosed());
                }
                return;
            }
            //presentation was created during prefetch...start here
            if (Asserting.ASSERTING)
            {
                Assert.condition(presentation != null);
            }

            // Create a Presentation and start it. If it starts successfully,
            // assign it to the presentation field..otherwise, post a ControllerErrorEvent
            try
            {
                presentation.start();
                if (log.isInfoEnabled())
                {
                    log.info(getId() + "presentation started: " + presentation);
                }
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(getId() + "Unable to start presentation", e);
                }
                setState(Unrealized);
                setTargetState(Unrealized);
                postEvent(new ResourceUnavailableEvent(AbstractPlayer.this, getId() + "presentation creation failed: " + e.getMessage()));
            }
            // failure to start due to an error will post the appropriate event
        }
    }

    /*
     * syncStart()
     */

    private TVTimerWentOffListener listener = new TVTimerWentOffListener()
    {
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            synchronized (getLock())
            {
                if (startTimerSpec == e.getTimerSpec())
                {
                    runAsync(new StartPresentationTask());
                }

                // Clear out the spec, now that its work is done.
                startTimerSpec = null;
            }
        }
    };

    /**
     * This is a timer object used for sync start. This is created to use the
     * same {@link #taskQueue TaskQueue} as is used to execute other
     * asynchronous operations. The referenced {@link CallerContext} must be the
     * same context used to create the task queue.
     */
    private TaskTimer startTimer = new TaskTimer(taskQueue, ccMgr.getSystemContext());

    /** Timer psec that goes with {@link #startTimer}. */
    private TVTimerSpec startTimerSpec = null;

    /** Initial time-base time specified in call to {@link #syncStart(Time)}. */
    private Time syncStartTime = null;

    /** Initial media time when {@link #syncStart(Time)} was called. */
    private Time mediaStartTime = null;

    public void syncStart(Time tbt)
    {
        if (tbt == null) throw new NullPointerException("null start time");
        if (tbt.getNanoseconds() < 0)
            throw new IllegalArgumentException("invalid start time (" + tbt.getNanoseconds() + ")");

        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "syncStart()" + getStateString());
            }

            // Do nothing if Closed.
            if (isClosed())
                return;

            // Must not be Started and must be Prefetched.
            assertStopped("syncStart");
            assertPrefetched("syncStart");

            // Cache syncStart tbt and media time for parameters to StartEvent
            // (below and in start() method).
            syncStartTime = new Time(tbt.getNanoseconds());
            mediaStartTime = new Time(getMediaNanoseconds());

            // Start media clock.
            getClock().syncStart(tbt);

            // Set the state and send StartEvent.
            setTargetState(Started);

            // Compute milliseconds from 'now' until decoding should start. If
            // time has already passed (delta is less than zero), start decoding 
            // immediately 
            long deltaMS = (tbt.getNanoseconds() - getTimeBase().getNanoseconds()) / PlayerClock.NANOS_PER_MILLI;

            boolean startNow = (deltaMS < 1);
            if (!startNow)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "starting decode in " + deltaMS + " milliseconds");
                }

                // Create a startTimerSpec to start decoding asynchronously.
                startTimerSpec = startTimer.createTimerSpec();
                startTimerSpec.setAbsolute(false);
                startTimerSpec.setRepeat(false);
                startTimerSpec.setDelayTime(deltaMS);
                startTimerSpec.addTVTimerWentOffListener(listener);
                try
                {
                    startTimerSpec = startTimer.scheduleTimerSpec(startTimerSpec);
                }
                catch (TVTimerScheduleFailedException e)
                {
                    startTimerSpec = null;
                    SystemEventUtil.logRecoverableError(getId() + "Error starting timer in PlayerBase.syncStart", e);
                    startNow = true;
                    // Better to have it start now rather than never...
                }
            }

            if (startNow)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "starting decode now");
                }

                startTimerSpec = null;
                runAsync(new StartPresentationTask());
            }

            // If a stop time has been set, activate the alarm for it
            // so we are notified when to stop
            activateStopMediaTimeAlarm();

            // Notify threads that are awaiting state change.
            getLock().notifyAll();
        }
    }

    /*
     * stop() implementation
     */

    public void stop()
    {
        synchronized (getLock())
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "stop() " + getStateString());
            }

            // Don't do anything if closed.
            if (isClosed())
                return;

            switch (state)
            {
                case Unrealized:
                case Realized:
                case Prefetched:
                    // Set the target state to same as current state (so we
                    // don't advance any further).
                    setTargetState(state);
                    break;

                case Realizing:
                    // Don't advance past Realized state.
                    setTargetState(Realized);
                    break;

                case Prefetching:
                    // Transition to Prefetched.
                    setTargetState(Prefetched);
                    break;

                case Started:
                    // Transition to Prefetched.
                    setTargetState(Prefetched);
                    // Stop decoding, revert back to PrefetchedState, and send
                    // ClosedEvent.
                    stop(new StopByRequestEvent(this, Started, Prefetched, targetState, getClock().getMediaTime()));
                    // Return so we don't hit the dispatchEvent() call below,
                    // which is used for Stopped states.
                    return;
            }

            // If not Started, send the StopByRequestEvent for current state.
            postEvent(new StopByRequestEvent(this, state, state, targetState, getClock().getMediaTime()));
        }
    }

    /**
     * Helper method to stop decoding, do bookkeeping, and send a
     * {@link StopEvent}.
     * 
     * @param e
     *            The {@link StopEvent} to send.
     */
    protected final void stop(StopEvent e)
    {
        synchronized (getLock())
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "stop(StopEvent) - event: " + e + ", state: " + getStateString());
            }

            // Stop the clock.
            getClock().stop();

            // Deactivate all alarms.
            LinkedList alarmCopy = new LinkedList(alarms);
            for (Iterator it = alarmCopy.iterator(); it.hasNext();)
            {
                AlarmImpl alarm = (AlarmImpl) it.next();
                alarm.notifyStopped();
            }

            // Stop the Presentation if in progress;
            // close player if error occurs.
            if (presentation != null)
            {
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "calling presentation.stop and setting presentation to null");
                    }
                    presentation.stop();
                    presentation = null;
                }
                catch (Error x)
                {
                    postEvent(new InternalErrorEvent(this, getId() + x.toString()));
                    return;
                }
            }

            // Clear any timers that may be in effect.
            clearTimers();

            // Change state from Started to Prefetched.
            setState(e.getTargetState());
            setTargetState(e.getTargetState());
            if (e.getTargetState() < Prefetching) releaseResources(Realizing);

            // Clear the cached times.
            syncStartTime = null;
            mediaStartTime = null;

            // Send the stop event.
            postEvent(e);
        }
    }

    /**
     * This helper method deschedules the timers and clears the timer fields.
     */
    private void clearTimers()
    {
        synchronized (getLock())
        {
            if (startTimerSpec != null)
            {
                startTimer.deschedule(startTimerSpec);
                startTimerSpec = null;
            }
        }
    }

    /*
     * 
     * AlarmClock implementation
     */

    class AlarmImpl implements Alarm
    {
        /**
         * The {@link AlarmSpec} used to create the {@link AlarmImpl}.
         */
        final AlarmSpec alarmSpec;

        /**
         * The {@link Callback} that is invoked when the alarm fires.
         */
        final Callback callback;

        /**
         * Indicates whether the timer is currently active. An inactive timer
         * cannot deliver any alarm notification.
         */
        boolean isActive;

        /**
         * TVTimer and TVTimerSpec for use by this Alarm. Note that this timer
         * belongs to the {@link CallerContext} that code is executing in when
         * the {@link #AlarmImpl(AlarmSpec , Callback)} constructor is invoked.
         * This is expected to be in response to invocation of
         * {@link AbstractPlayer#createAlarm}, which is in response to
         * {@link AbstractPlayer#setStopTime}, {@link MediaTimer#setLastTime} or
         * {@link MediaTimer#setFirstTime}. In any case the context in which
         * those methods are called are expected to be the context within which
         * the timer goes off.
         */
        TVTimer alarmTimer = TVTimer.getTimer();

        TVTimerSpec timerSpec = null;

        /**
         * Called when the TVTimer goes off. Will be invoked within the context
         * within which this <code>AlarmImpl</code> was originally created.
         * 
         * @see #alarmTimer
         */
        TVTimerWentOffListener alarmListener = new TVTimerWentOffListener()
        {
            public void timerWentOff(TVTimerWentOffEvent e)
            {
                synchronized (getLock())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "alarm timer went off - timer spec: " + timerSpec + ", alarmspec: " + alarmSpec);
                    }
                    if (isActive)
                    {
                        long currentMediaTimeNanos = getPresentationMediaTime().getNanoseconds();
                        float currentRate = getRate();
                        long wallTimeTargetDeltaMillis = alarmSpec.getDelayWallTimeNanos(currentMediaTimeNanos, currentRate) / PlayerClock.NANOS_PER_MILLI;
                        if (wallTimeTargetDeltaMillis > ALARM_TIMER_MILLIS_FIRING_THRESHOLD)
                        {
                            try
                            {
                                if (log.isDebugEnabled())
                                {
                                    log.debug(getId() + "alarmspec wall-time millis delta " + wallTimeTargetDeltaMillis + " not within firing threshold millis: " + ALARM_TIMER_MILLIS_FIRING_THRESHOLD + " - rescheduling alarmspec: " + alarmSpec);
                                }
                                scheduleTimerTask(currentMediaTimeNanos, currentRate);
                            }
                            catch (AlarmException ae)
                            {
                                if (log.isWarnEnabled())
                                {
                                    log.warn(getId() + "Alarm exception scheduling timer task - media time nanos: " + currentMediaTimeNanos + ", rate: " + currentRate + ", alarmspec: " + alarmSpec);
                                }
                            }
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug(getId() + "alarmspec wall-time millis delta " + wallTimeTargetDeltaMillis + " within firing threshold millis: " + ALARM_TIMER_MILLIS_FIRING_THRESHOLD + " - firing: " + alarmSpec);
                            }
                            //less than (threshold / rate) millis of requested mediatime at current rate - fire
                            invokeFired();
                            timerSpec = null;
                        }
                    }
                }
            }
        };

        /**
         * Construct an instance from an {@link AlarmSpec} and a
         * {@link Callback}.
         * 
         * @param as
         *            The {@link AlarmSpec} from which to construct the alarm.
         * @param cb
         *            The {@link Callback} to call when the alarm fires.
         */
        AlarmImpl(AlarmSpec as, Callback cb)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "create Alarm for " + as);
            }

            alarmSpec = as;
            callback = cb;
        }

        public void activate() throws AlarmException
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "activate " + alarmSpec);
            }

            synchronized (getLock())
            {
                // If already active, do nothing.
                if (isActive)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "already active");
                    }
                    return;
                }

                // Set isActive flag.
                isActive = true;

                // Create a TimerTask for the alarm using presentation mediatime if available
                scheduleTimerTask(getPresentationMediaTime().getNanoseconds(), getRate());
            }
        }

        /**
         * Create and schedule a {@link TimerTask} for the {@link Alarm} based
         * on a given base media time and clock rate.
         * 
         * @param baseMediaTimeNanos
         *            The base media time (in nanoseconds).
         * @param rate
         *            The playback rate.
         * @throws AlarmException
         *             if timer could not be scheduled
         */
        void scheduleTimerTask(long baseMediaTimeNanos, float rate) throws AlarmException
        {
            synchronized(getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "scheduleTimerTask(mediaTime: " + new Time(baseMediaTimeNanos) + ", rate: " + rate + "): " + alarmSpec);
                }

                // If player is stopped, skip.
                if (isStopped())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "skip because player is stopped: " + alarmSpec);
                    }
                    return;
                }

                // If rate is paused, skip.
                if (!alarmSpec.canSchedule(rate, baseMediaTimeNanos))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "alarmSpec canSchedule returned false: " + alarmSpec + ", rate: " + rate + ", baseMediaTimeNanos: " + new Time(baseMediaTimeNanos));
                    }
                    return;
                }
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "alarmSpec canSchedule returned true: " + alarmSpec + ", rate: " + rate + ", baseMediaTimeNanos: " + new Time(baseMediaTimeNanos));
                }

                // Compute the wall-time delay in nanoseconds
                long delayWallTimeNanos = alarmSpec.getDelayWallTimeNanos(baseMediaTimeNanos, rate);
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "delayWallTimeNanos for alarmSpec " + alarmSpec + ": " + new Time(delayWallTimeNanos) + ", rate: " + rate + ", baseMediaTime: " + new Time(baseMediaTimeNanos));
                }
                // Convert to milliseconds
                long delayWallTimeMillis = delayWallTimeNanos / PlayerClock.NANOS_PER_MILLI;
                long specDelayMillis;
                //if delay > threshold, reschedule with the factor applied
                if (delayWallTimeMillis > ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD)
                {
                    specDelayMillis = (long) (delayWallTimeMillis * ALARM_TIMER_SCHEDULE_FACTOR);
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "wall-time delay millis: " + delayWallTimeMillis + " greater than reschedule factor millis: " + ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD +
                                " - using factor " + ALARM_TIMER_SCHEDULE_FACTOR + " - using delay millis: " + specDelayMillis);
                    }
                }
                else
                {
                    specDelayMillis =  delayWallTimeMillis;
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "wall-time delay millis: " + delayWallTimeMillis + " less than reschedule factor millis: " + ALARM_TIMER_MILLIS_SCHEDULE_WITH_FACTOR_THRESHOLD +
                        " - using delay millis: " + specDelayMillis);
                    }
                }

                // Construct the timer spec and schedule it.
                timerSpec = new TVTimerSpec();
                timerSpec.setAbsolute(false);
                timerSpec.setRepeat(false);

                timerSpec.setDelayTime(specDelayMillis);
                timerSpec.addTVTimerWentOffListener(alarmListener);

                try
                {
                    timerSpec = alarmTimer.scheduleTimerSpec(timerSpec);
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "timerSpec scheduled for alarmSpec: " + alarmSpec + ", timerspec: " + timerSpec);
                    }
                }
                catch (TVTimerScheduleFailedException e)
                {
                    timerSpec = null;
                    throw new AlarmException(e.toString());
                }
            }
        }

        public void deactivate()
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "deactivate: " + alarmSpec);
            }

            synchronized (getLock())
            {
                // If already inactive, do nothing.
                if (!isActive)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "alarmSpec already inactive: " + alarmSpec);
                    }
                    return;
                }

                // Clear the timer.
                clearTimer();
            }
        }

        void clearTimer()
        {
            synchronized (getLock())
            {
                // Cancel the timer task.
                cancelAlarmTimer();

                // Set the active flag.
                isActive = false;
            }
        }

        public AlarmSpec getAlarmSpec()
        {
            return alarmSpec;
        }

        void invokeFired()
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "firing: " + alarmSpec);
            }

            // Call the callback.
            Throwable error = null;
            try
            {
                callback.fired(this);
            }
            catch (Error e)
            {
                // Capture Error for Logging.LOGGING.
                error = e;
            }
            catch (RuntimeException x)
            {
                // Capture Exception for Logging.LOGGING.
                error = x;
            }
            // If an exception or error occurred, log it.
            if (error != null)
                SystemEventUtil.logRecoverableError(getId() + "error in Alarm.Callback.fired: " + alarmSpec, error);
        }

        void invokeDestroyed(AlarmException x)
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "invokeDestroyed: " + alarmSpec);
            }

            timerSpec = null;

            // Call the callback.
            Throwable error = null;
            try
            {
                callback.destroyed(this, x);
            }
            catch (Error e)
            {
                // Capture Error for Logging.LOGGING.
                error = e;
            }
            catch (RuntimeException e)
            {
                // Capture Exception for Logging.LOGGING.
                error = e;
            }
            // If an exception or error occurred, log it.
            if (error != null)
                SystemEventUtil.logRecoverableError(getId() + "error in Alarm.Callback.destroyed: " + alarmSpec, error);
        }

        /*
         * Methods called by the clock when rate or media time is changed. This
         * method should be called <em>before</em> the time or rate is changed
         * to ensure proper operation.
         */

        /**
         * Reevaluate the timer due to the media time being changed. If the time
         * is active, and the change would cause it to skip over the alarm's
         * media time, then send the event.
         * 
         * @param newMediaTimeNanos
         *            The new media time to check, in nanoseconds
         */
        void notifyTimeChange(long newMediaTimeNanos)
        {
            synchronized(getLock())
            {
                long currentMediaTimeNanos = clock.getMediaNanoseconds();
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "notifyTimeChange - currentMediaTime: " + new Time(currentMediaTimeNanos) + ", newMediaTime: " + new Time(newMediaTimeNanos) + ", spec: " + alarmSpec);
                }

                // Do nothing if inactive.
                if (!isActive)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "ignored because alarm is inactive: " + alarmSpec);
                    }
                    return;
                }

                //navigating across an alarm
                if (alarmSpec.shouldFire(currentMediaTimeNanos, newMediaTimeNanos))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "alarmSpec shouldFire returned true: " + alarmSpec + ", currentMediaTime: " + new Time(currentMediaTimeNanos) + ", newMediaTime: " + new Time(newMediaTimeNanos));
                    }

                    invokeFired();
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "alarmSpec shouldFire returned false: " + alarmSpec + ", currentMediaTime: " + new Time(currentMediaTimeNanos) + ", newMediaTime: " + new Time(newMediaTimeNanos));
                    }
                }
                if (timerSpec != null)
                {
                    cancelAlarmTimer();
                }

                // Now schedule a new task, based on the new media time and current rate.
                try
                {
                    scheduleTimerTask(newMediaTimeNanos, getRate());
                }
                catch (AlarmException x)
                {
                    invokeDestroyed(x);
                }
            }
        }

        void notifyRateChange(float newRate)
        {
            synchronized(getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "notifyRateChange(" + newRate + "): " + alarmSpec);
                }

                // Do nothing if inactive.
                if (!isActive)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "ignored because alarm is inactive: " + alarmSpec);
                    }
                    return;
                }

                // Reset current alarm.
                try
                {
                    cancelAlarmTimer();

                    // Reset the alarm - use presentation mediatime here
                    scheduleTimerTask(getPresentationMediaTime().getNanoseconds(), newRate);
                }
                catch (AlarmException x)
                {
                    invokeDestroyed(x);
                }
            }
        }

        void notifyStopped()
        {
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "notifyStopped: " + alarmSpec);
            }

            if (!isActive)
                return;

            // Cancel the task, if scheduled.
            cancelAlarmTimer();
        }

        void notifyStarted()
        {
            synchronized(getLock())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(getId() + "notifyStarted: " + alarmSpec);
                }

                if (!isActive)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "ignored because alarm is inactive: " + alarmSpec);
                    }
                    return;
                }

                cancelAlarmTimer();

                try
                {
                    scheduleTimerTask(getPresentationMediaTime().getNanoseconds(), getRate());
                }
                catch (AlarmException x)
                {
                    invokeDestroyed(x);
                }
            }
        }

        void cancelAlarmTimer()
        {
            synchronized (getLock())
            {
                if (timerSpec != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(getId() + "canceling timerspec associated with alarmSpec: " + alarmSpec + ", timerSpec: " + timerSpec);
                    }

                    alarmTimer.deschedule(timerSpec);
                    timerSpec = null;
                }
            }
        }
    }

    private LinkedList alarms = new LinkedList();

    public Alarm createAlarm(AlarmSpec spec, Callback callback)
    {
        synchronized (getLock())
        {
            Alarm alarm = new AlarmImpl(spec, callback);
            alarms.addLast(alarm);
            return alarm;
        }
    }

    public void destroyAlarm(Alarm alarm)
    {
        synchronized (getLock())
        {
            if (alarm == null) return;
            ((AlarmImpl) alarm).clearTimer();
            alarms.remove(alarm);
        }
    }

    /*
     * PresentationContext Notification Methods
     */

    public void notifyStopByError(String reason, Throwable throwable)
    {
        if (throwable == null)
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "notifyStopByError - reason: " + reason);
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info(getId() + "notifyStopByError - reason: " + reason, throwable);
            }
        }
        if (!isClosed())
        {
            setState(Unrealized);
            setTargetState(Unrealized);
        }
        postEvent(new ControllerErrorEvent(this, reason));
    }

    public void notifyMediaPresented()
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "media is presenting");
        }
    }

    /**
     * This method MUST be called to initiate a transition to the STARTED state
     */
    public void notifyStarted() {
        if (log.isInfoEnabled())
        {
            log.info(getId() + "notifyStarted - setting state to started - triggering StartEvent");
        }
        setState(Started);
        postEvent(new StartEvent(AbstractPlayer.this, Prefetched, Started, Started, syncStartTime, mediaStartTime));
        LinkedList alarmCopy = new LinkedList(alarms);
        for (Iterator it = alarmCopy.iterator(); it.hasNext();)
        {
            AlarmImpl alarm = (AlarmImpl) it.next();
            alarm.notifyStarted();
        }
    }

    protected boolean isPresenting()
    {
        return presentation != null && presentation.isPresenting();
    }

    public String toString()
    {
        //do not acquire locks in tostring calls
        String s = super.toString();
        return getId() + s.substring(s.lastIndexOf('.') + 1) + ", " + getStateString();
    }

    /*
     * 
     * Helper Methods
     */

    /**
     * Return a String representing the {@link Controller} state constant (such
     * as {@link Controller#Started}.
     * 
     * @param state
     *            An <code>int</code> {@link Controller} state.
     * @return Returns a String representing the state.
     */
    public static String stateToString(int state)
    {
        switch (state)
        {
            case Unrealized:
                return "Unrealized";
            case Realizing:
                return "Realizing";
            case Realized:
                return "Realized";
            case Prefetching:
                return "Prefetching";
            case Prefetched:
                return "Prefetched";
            case Started:
                return "Started";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    //common gaincontrol implementation, may or may not be used by subclasses
    protected class GainControlImpl extends ControlBase implements GainControl
    {
        //entries in base.properties containing the range of minimum and maximum decibel values supported by the platform
        private static final String MIN_DB_PROPERTY_NAME = "OCAP.audio.minDB";
        private static final String MAX_DB_PROPERTY_NAME = "OCAP.audio.maxDB";

        //mute and gain both have platform-agnostic defaults
        private boolean mute = false;
        private float gain = 0.0F;

        //min and max decibel gain are platform specific (retrieved via OCAP.audio properties)
        private float minGain;
        private float maxGain;

        //level default is calculated using 0.0 default decibel level and the min/max audio properties
        private float level;
        private double linearMin;
        private double linearMax;

        GainControlImpl()
        {
            super(true);
            String minDBString = PropertiesManager.getInstance().getProperty(MIN_DB_PROPERTY_NAME, null);
            String maxDBString = PropertiesManager.getInstance().getProperty(MAX_DB_PROPERTY_NAME, null);
            if (minDBString == null || maxDBString == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(getId() + "Minimum or maximum audio DB values not available - defaulting min and max DB to zero");
                }
                minGain = 0.0F;
                maxGain = 0.0F;
            }
            else
            {
                minGain = Float.parseFloat(minDBString);
                maxGain = Float.parseFloat(maxDBString);
            }
            linearMin = Math.pow(10, (minGain / 20));
            linearMax = Math.pow(10, (maxGain / 20));
            level = levelFromGain(gain);
        }

        public void setMute(boolean newMute)
        {
            if (isPresenting())
            {
                getPresentation().setMute(newMute);
            }
            mute = newMute;
            postEvent();
        }

        public boolean getMute()
        {
            return mute;
        }

        public float setDB(float newGain)
        {
            if (Math.abs(gain - newGain) > 0.001F)
            {
                if (isPresenting())
                {
                    gain = getPresentation().setGain(newGain);
                }
                else
                {
                    gain = newGain;
                }
                level = levelFromGain(gain);
                postEvent();
            }
            return gain;
        }

        public float getDB()
        {
            return gain;
        }

        public float setLevel(float newLevel)
        {
            if (Math.abs(level - newLevel) > 0.001F)
            {
                //convert to decibels and set the decibel value
                float newGain = gainFromLevel(newLevel);
                if (isPresenting())
                {
                    gain = getPresentation().setGain(newGain);
                    level = levelFromGain(gain);
                }
                else
                {
                    gain = newGain;
                    level = levelFromGain(gain);
                }
                postEvent();
            }
            return level;
        }

        public float getLevel()
        {
            return level;
        }

        public void addGainChangeListener(GainChangeListener listener)
        {
            gainChangeMulticaster.addListenerMulti(listener);
        }

        public void removeGainChangeListener(GainChangeListener listener)
        {
            gainChangeMulticaster.removeListener(listener);
        }

        private float gainFromLevel(float newLevel)
        {
            //fence in the level before calculating gain
            float fencedLevel = Math.min(Math.max(newLevel, 0.0F), 1.0F);
            return (float) (20.0F * ((Math.log(((fencedLevel * (linearMax - linearMin)) + linearMin))) / (Math.log(10))));
        }

        private float levelFromGain(float newGain)
        {
            float result = (float)((Math.pow(10, (newGain / 20.0F)) - linearMin) / (linearMax - linearMin));
            //fence in the result between zero and one
            return Math.min(Math.max(result, 0.0F), 1.0F);
        }

        private void postEvent()
        {
            gainChangeMulticaster.multicast(new GainChangeEvent(this, mute, gain, level));
        }
    }

    /**
     * per-CallerContext EventMulticaster supporting gainchangelisteners
     */
    protected CallerContextEventMulticaster gainChangeMulticaster = new GainChangeEventDispatcher();

    private static class GainChangeEventDispatcher extends CallerContextEventMulticaster
    {
        public void dispatch(EventListener listeners, EventObject event)
        {
            if (listeners instanceof GainChangeListener && event instanceof GainChangeEvent)
            {
                ((GainChangeListener) listeners).gainChange((GainChangeEvent) event);
            }
            else if (Asserting.ASSERTING)
            {
                Assert.condition(false, "listener or event of incorrect type");
            }
        }
    }
}
