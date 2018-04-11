
package org.ocap.ui.event;

import java.util.EventObject;

/**
 * <p>A <code>MultiScreenEvent</code> is an abstract, base classs used to
 * organize event identification codes used by disparate types of events
 * related to multiple screen management functionality.</p>
 *
 * @author Glenn Adams
 * @since MSM I01
 **/
public abstract class MultiScreenEvent extends EventObject
{
    /**
     * First event identifier assigned to <code>MultiScreenConfigurationEvent</code>
     * event identifiers.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONFIGURATION_FIRST = 0;

    /**
     * First event identifier assigned to <code>MultiScreenContextEvent</code>
     * event identifiers.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_FIRST = 32;

    /**
     * Protected constructor for an <code>MultiScreenEvent</code>.
     *
     * @param source A reference to an event source as defined by a
     * concrete subclass of this class.
     *
     * @param id The event identifier of this event.
     *
     * @since MSM I01
     **/
    protected MultiScreenEvent ( Object source, int id )
    {
        super ( source );
    }

    /**
     * Obtain the event identifier associated with this event.
     *
     * @return The event identifier of this event, for which see
     * the sub-classes of this class:
     * <code>MultiScreenConfigurationEvent</code> and
     * <code>MultiScreenContextEvent</code>.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfigurationEvent
     * @see MultiScreenContextEvent
     **/
    public int getId()
    {
        return 0;
    }

}
