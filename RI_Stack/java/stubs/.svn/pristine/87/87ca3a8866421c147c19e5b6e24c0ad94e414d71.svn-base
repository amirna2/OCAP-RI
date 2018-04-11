
package org.ocap.ui.event;

import java.util.EventObject;

import org.davic.resources.ResourceStatusEvent;

/**
 * <p>A <code>MultiScreenResourceEvent</code> is used to report
 * changes regarding the resource status of multiscreen related
 * resources.</p>
 *
 * @author Glenn Adams
 * @since MSM I01
 *
 * @see org.davic.resources.ResourceStatusEvent
 **/
public class MultiScreenResourceEvent extends ResourceStatusEvent
{
    /**
     * The reservation on a screen has just been released, indicating
     * that the screen (or its constituent screen devices) MAY now be
     * reserved (i.e., they are now unreserved).
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_RESOURCE_SCREEN_RELEASED = 0;

    /**
     * The reservation on a screen has just been granted to an
     * application, indicating that the screen (including its constituent
     * screen devices) is no longer unreserved.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_RESOURCE_SCREEN_RESERVED = 0;

    /**
     * Constructor for an <code>MultiScreenResourceEvent</code>.
     *
     * @param source A reference to an <code>HScreen</code> instance
     * whose resource status has changed.
     *
     * @param id The event identifier of this event.
     *
     * @since MSM I01
     **/
    public MultiScreenResourceEvent ( Object source, int id )
    {
        super ( source );
    }

    /**
     * Obtain the source object that generated this event.
     *
     * @return A reference to an <code>HScreen</code> instance, or a
     * subclass thereof.
     *
     * @since MSM I01
     **/
    public Object getSource()
    {
        return null;
    }

    /**
     * Obtain the resource event identifier associated with this event.
     *
     * @return The event identifier of this event, where the identifier
     * is one of the following: {
     * <code>MULTI_SCREEN_RESOURCE_SCREEN_RELEASED</code>,
     * <code>MULTI_SCREEN_RESOURCE_SCREEN_RESERVED</code> }.
     *
     * @since MSM I01
     **/
    public int getId()
    {
        return 0;
    }

}
