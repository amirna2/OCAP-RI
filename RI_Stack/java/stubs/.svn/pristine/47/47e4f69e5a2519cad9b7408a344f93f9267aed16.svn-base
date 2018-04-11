
package org.ocap.ui.event;

import java.util.EventListener;

/**
 * This listener is used to provide notifications regarding system and
 * application induced changes to a <code>MultiScreenContext</code>.
 *
 * @author Glenn Adams
 * @since OCAP 1.0 I16 (ECN XXXX)
 **/
public interface MultiScreenContextListener extends EventListener
{
    /**
     * <p>When an OCAP implementation makes any change to a <code>MultiScreenContext</code>
     * that causes generation of a
     * <code>MultiScreenContextEvent</code>, then the
     * implementation SHALL invoke this method on all registered listeners in
     * order to report change information to the listener.</p>
     *
     * <p>If the application that registered this listener has not been
     * granted <code>MonitorAppPermission("multiscreen.context")</code>
     * and the source <code>MultiScreenContext</code> associated with
     * the specified <code>MultiScreenContextEvent</code> is associated
     * with no <code>ServiceContext</code> or a
     * <code>ServiceContext</code> that is not accessible to that
     * application, then this method SHALL
     * NOT be invoked on this listener; otherwise it SHALL be invoked on
     * this listener.</p>
     *
     * <p>A <code>ServiceContext</code> is accessible to an application if it is
     * returned from the <code>ServiceContextFactory.getServiceContexts()</code>
     * method.</p>
     *
     * @param evt A <code>MultiScreenContextEvent</code> instance.
     *
     * @since MSM I01
     **/
    public void notify ( MultiScreenContextEvent evt );
}
