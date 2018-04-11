package org.cablelabs.impl.ocap.hn.content;

import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;

public class LocalNetActionHandler implements NetActionHandler
{
    private NetActionEvent event = null;

    private Object sync = new Object();

    private long TIMEOUT = 60000L;

    /**
     * Waits for event in response to a method call that generates a UPnP action
     * aimed at a local NetModule. This method should only be called if either
     * this instance was just instantiated (thus event will be null) or if the
     * clearEvent() was recently called so that old events will not be returned.
     * 
     * @param tx
     * @param planID
     * @return NetActionEvent corresponding to the UPnP reply.
     */
    public NetActionEvent getLocalEvent()
    {
        synchronized (sync)
        {
            if (event == null)
            {
                try
                {
                    sync.wait(TIMEOUT);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
        return event;
    }

    /**
     * Set local event to null.
     * 
     */
    public void clearEvent()
    {
        event = null;
    }

    /**
     * Method required to implement <class>NetActionHandler</class>. Is called
     * when a <class>NetActionEvent</class> occurs.
     * 
     * @param e
     *            The <class>NetActionEvent</class>.
     */
    public void notify(NetActionEvent e)
    {
        synchronized (sync)
        {
            event = e;
            sync.notifyAll();
        }
    }
}
