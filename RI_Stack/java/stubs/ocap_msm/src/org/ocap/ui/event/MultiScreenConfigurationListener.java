
package org.ocap.ui.event;

import java.util.EventListener;

/**
 * This listener is used to provide notifications regarding system and
 * application induced changes to the global state of the
 * <code>MultiScreenManager</code> instance or the state of some display
 * <code>HScreen</code> with respect to the per-platform or some
 * per-display multiscreen configuration, respectively, or to changes to
 * a specific <code>MultiScreenConfiguration</code> instance.
 *
 * @author Glenn Adams
 * @since MSM I01
 **/
public interface MultiScreenConfigurationListener extends EventListener
{
    /**
     * <p>When a <code>MultiScreenConfigurationEvent</code> is
     * generated, the implempentation SHALL invoke this method on all
     * registered listeners in order to report event information to each
     * listener as required by specific event semantics.</p>
     *
     * <p>In case the event is
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING</code>,
     * this method SHALL NOT be invoked unless the application that
     * registered this listener has been granted
     * <code>MonitorAppPermission("multiscreen.configuration")</code>.
     * Furthermore, an implementation of this method SHOULD severely
     * limit the amount of processing that may occur, since an absolute
     * time limit is placed on the invocation of this method for the set
     * of all applicable listeners. </p>
     *
     * @param evt A <code>MultiScreenConfigurationEvent</code> instance.
     *
     * @since MSM I01
     **/
    public void notify ( MultiScreenConfigurationEvent evt );
}
