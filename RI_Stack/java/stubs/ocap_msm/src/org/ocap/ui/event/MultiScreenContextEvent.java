
package org.ocap.ui.event;

/**
 * A <code>MultiScreenContextEvent</code> is used to report a change to a
 * <code>MultiScreenContext</code> to interested listeners.
 *
 * <p>The following types of changes cause the generation of this
 * event:</p>
 *
 * <ul>
 *   <li>Change of associated <code>ServiceContext</code>;</li>
 *   <li>Change of associated display <code>HScreen</code>;</li>
 *   <li>Change of associated display <code>HScreen</code> area (extent)
 *   assignment;</li>
 *   <li>Change of associated set of <code>VideoOutputPort</code>s;</li>
 *   <li>Change of audio focus of a display <code>HScreen</code>;</li>
 *   <li>Change of screen visibility;</li>
 *   <li>Change of screen z-order.</li>
 *   <li>Change of set of underlying <code>HScreenDevice</code> that
 *   contribute audio sources to an <code>HScreen</code>;</li>
 *   <li>Change of set of underlying <code>HScreenDevice</code>
 *   instances, e.g., due to addition or removal of a
 *   <code>HScreenDevice</code> from an <code>HScreen</code>;</li>
 *   <li>Change of the z-order of the underlying <code>HScreenDevice</code>
 *   instances of an <code>HScreen</code>;</li>
 * </ul>
 *
 * @author Glenn Adams
 * @since MSM I01
 */
public class MultiScreenContextEvent extends MultiScreenEvent
{
    /**
     * The set of <code>HScreenDevice</code> instances associated with the underlying
     * <code>HScreen</code> of the source <code>MultiScreenContext</code> has
     * changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DEVICES_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 0;

    /**
     * The z-order of the set of <code>HScreenDevice</code> instances associated with the underlying
     * <code>HScreen</code> of the source <code>MultiScreenContext</code> has
     * changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DEVICES_Z_ORDER_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 1;

    /**
     * The <code>ServiceContext</code> associated with the underlying
     * <code>HScreen</code> of the source <code>MultiScreenContext</code> has
     * changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_SERVICE_CONTEXT_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 2;

    /**
     * The display <code>HScreen</code> associated with the underlying
     * <code>HScreen</code> of the source code>MultiScreenContext</code> has
     * <changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DISPLAY_SCREEN_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 3;

    /**
     * The area (extent) of the display <code>HScreen</code> to which the
     * underlying <code>HScreen</code> of the source <code>MultiScreenContext</code>
     * is assigned has changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_DISPLAY_AREA_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 4;

    /**
     * The set of video output ports associated with underlying
     * <code>HScreen</code> of the source <code>MultiScreenContext</code> has
     * changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_OUTPUT_PORT_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 5;

    /**
     * The visibility of the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_VISIBILITY_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 6;

    /**
     * The z-order of the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_Z_ORDER_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 7;

    /**
     * The audio sources of the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_AUDIO_SOURCES_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 8;

    /**
     * The audio focus screen of the underlying <code>HScreen</code> of the source
     * <code>MultiScreenContext</code> has changed. When the audio
     * focus screen of a display <code>HScreen</code> changes, then
     * this event SHALL be generated twice (after completing the
     * change): firstly to the <code>MultiScreenContext</code> of the
     * logical screen which has lost audio focus (if such logical
     * screen existed), and secondly to the
     * <code>MultiScreenContext</code> of the display screen. In both
     * of these cases, the source <code>MultiScreenContext</code> SHALL
     * be the display screen.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXT_AUDIO_FOCUS_CHANGED = MULTI_SCREEN_CONTEXT_FIRST + 9;

    /**
     * Last event identifier assigned to <code>MultiScreenConfigurationEvent</code>
     * event identifiers.
     *
     * @since MSM I01
     **/
    public static final int MULTI_SCREEN_CONTEXTS_LAST = MULTI_SCREEN_CONTEXT_FIRST + 9;

    /**
     * Construct a <code>MultiScreenContextEvent</code>.
     *
     * @param source A reference to a <code>MultiScreenContext</code>
     * interface.
     *
     * @param id The event identifier of this event, the value of which
     * SHALL be one of the following:
     * <code>MULTI_SCREEN_CONTEXT_DEVICES_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_DEVICES_Z_ORDER_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_SERVICE_CONTEXT_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_DISPLAY_SCREEN_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_DISPLAY_AREA_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_OUTPUT_PORT_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_VISIBILITY_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_Z_ORDER_CHANGED</code>,
     * <code>MULTI_SCREEN_CONTEXT_AUDIO_SOURCES_CHANGED</code>, or
     * <code>MULTI_SCREEN_CONTEXT_AUDIO_FOCUS_CHANGED</code>.
     *
     * @since MSM I01
     **/
    public MultiScreenContextEvent ( Object source, int id )
    {
        super ( source, id );
    }
}
