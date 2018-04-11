
package org.ocap.ui.event;

/**
 * A <code>MultiScreenConfigurationEvent</code> is used to report
 * changes to the global state of the <code>MultiScreenManager</code>
 * instance or the state of some display <code>HScreen</code> with
 * respect to the per-platform or some per-display multiscreen
 * configuration, respectively, or to changes to a specific
 * <code>MultiScreenConfiguration</code> instance.
 *
 * <p>The following types of changes SHALL cause the generation of this
 * event:</p>
 *
 * <ul>
 * <li>The currently active per-platform multiscreen configuration as determined by
 * the <code>MultiScreenManager</code> changes from one multiscreen
 * configuration to another multiscreen configuration;</li>
 * <li>The currently active per-display multiscreen configuration as determined by
 * some display <code>HScreen</code> changes from one multiscreen
 * configuration to another multiscreen configuration;</li>
 * <li>The set of screens associated with a
 * <code>MultiScreenConfiguration</code> changes (i.e., a screen is
 * added or removed from the multiscreen configuration);</li>
 * </ul>
 *
 * @author Glenn Adams
 * @since MSM I01
 *
 **/
public class MultiScreenConfigurationEvent extends MultiScreenEvent
{
    /**
     * A change to the currently active per-platform or some per-display
     * <code>MultiScreenConfiguration</code> as determined by the
     * <code>MultiScreenManager</code> or some display
     * <code>HScreen</code> has been initiated, in which case the value
     * returned by <code>getSource()</code> SHALL be the affected
     * <code>MultiScreenManager</code> or display <code>HScreen</code>,
     * and the value returned by <code>getRelated()</code> SHALL be the
     * subsequently active <code>MultiScreenConfiguration</code>.
     *
     * <p>A <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code> event
     * SHALL NOT be dispatched to an application that has not been
     * granted
     * <code>MonitorAppPermission("multiscreen.configuration")</code>.</p>
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_CHANGING = MULTI_SCREEN_CONFIGURATION_FIRST + 0;

    /**
     * The currently active per-platform or some per-display
     * <code>MultiScreenConfiguration</code> as determined by the
     * <code>MultiScreenManager</code> or some display
     * <code>HScreen</code> has changed, in which case the value
     * returned by <code>getSource()</code> SHALL be the affected
     * <code>MultiScreenManager</code> or display <code>HScreen</code>,
     * and the value returned by <code>getRelated()</code> SHALL be the
     * previously active <code>MultiScreenConfiguration</code>.
	 *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_CHANGED = MULTI_SCREEN_CONFIGURATION_FIRST + 1;

    /**
     * The set of screens associated with a
     * <code>MultiScreenConfiguration</code> has changed, with a new
     * screen having been added,
     * in which case the value returned by <code>getSource()</code> SHALL be
     * the affected <code>MultiScreenConfiguration</code>,
     * and the value returned by <code>getRelated()</code> SHALL be
     * the newly added <code>HScreen</code>.
     *
     * <p>Except during the interval between the last dispatching of an
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code> event and the
     * generation of a corresponding
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGED</code> event, a screen
     * SHALL NOT be added to and a
     * <code>MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED</code> event SHALL
     * NOT be generated for a multiscreen configuration that is the
     * current per-platform or some current per-display multiscreen
     * configuration.</p>
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED = MULTI_SCREEN_CONFIGURATION_FIRST + 2;

    /**
     * The set of screens associated with a
     * <code>MultiScreenConfiguration</code> has changed, with an
     * existing screen having been removed, in which case the value
     * returned by <code>getSource()</code> SHALL be the affected
     * <code>MultiScreenConfiguration</code>, and the value returned by
     * <code>getRelated()</code> SHALL be the newly removed
     * <code>HScreen</code>.
     *
     * <p>Except during the interval between the last dispatching of an
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code> event and the
     * generation of a corresponding
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGED</code> event, a screen
     * SHALL NOT be removed from and a
     * <code>MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED</code> event SHALL
     * NOT be generated for a multiscreen configuration that is the
     * current per-platform or some current per-display multiscreen
     * configuration.</p>
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED = MULTI_SCREEN_CONFIGURATION_FIRST + 3;

    /**
     * Last event identifier assigned to <code>MultiScreenConfigurationEvent</code>
     * event identifiers.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_LAST = MULTI_SCREEN_CONFIGURATION_FIRST + 3;

    /**
     * Construct an <code>MultiScreenConfigurationEvent</code>.
     *
     * @param source A reference to a <code>MultiScreenManager</code>
     * instance, a display <code>HScreen</code> instance, or a
     * <code>MultiScreenConfiguration</code> instance in accordance with
     * the specific event as specified above.
     *
     * @param id The event identifier of this event, the value of which
     * SHALL be one of the following:
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGING</code>,
     * <code>MULTI_SCREEN_CONFIGURATION_CHANGED</code>,
     * <code>MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED</code>, or
     * <code>MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED</code>.
     *
     * @param related A reference to a <code>MultiScreenConfiguration</code>
     * instance or an <code>HScreen</code> instance in
     * accordanced with the specific event as specified above.
     *
     * @since MSM I01
     **/
    public MultiScreenConfigurationEvent ( Object source, int id, Object related )
    {
        super ( source, id );
    }

    /**
     * Obtain a related object associated with this event.
     *
     * @return The related object instance of this event, the value of which
     * SHALL be one of the following as determined by the specific
     * event type:
     * a reference to a <code>MultiScreenConfiguration</code> instance
     * or
     * a reference to an <code>HScreen</code> instance.
     *
     * @since MSM I01
     **/
    public Object getRelated()
    {
        return null;
    }

}
