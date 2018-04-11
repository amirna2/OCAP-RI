/*
,------------------------------------------------------------------------------,
|                                                                              |
|                        Copyright 2010 Vidiom Systems Corp.                   |
|                              All rights reserved                             |
|                            Reproduced Under License                          |
|                                                                              |
|  This source code is the proprietary confidential property of Vidiom Systems |
|  Corp and is provided to recipient for documentation and educational         |
|  purposes only. Reproduction, publication, or distribution in any form to    |
|  any party other than the recipient is strictly prohibited.                  |
|                                                                              |
`------------------------------------------------------------------------------'
*/

package org.cablelabs.impl.manager.application;

import org.ocap.system.event.ErrorEvent;

/**
 * A task in which the given runnable code is executed while holding the lock on this
 * <code>SyncTask</code>.  Upon finishing the runnable code, Object.notify() will be
 * called on the <code>SyncTask</code> instance. This allows an interested party to
 * wait for the task execution to complete
 *	
 *  @author Greg Rutz
 */
public class SyncTask implements Runnable
{
    public SyncTask(Runnable run)
    {
        this.run = run;
    }
    
    /**
     * Returns any uncaught exception that was handled while executing the task.
     * 
     * @return the uncaught exception or null if no uncaught exception was handled
     */
    public Throwable getError()
    {
        return error;
    }

    /**
     * Runs the task code and then calls this.notify()
     */
    public synchronized void run()
    {
        try
        {
            run.run();
        }
        catch (Throwable e)
        {
            error = e;
            AppManager.logErrorEvent(ErrorEvent.SYS_REC_GENERAL_ERROR, e);
        }
        finally
        {
            notify();
        }
    }

    private Runnable run;
    private Throwable error = null;
}
