
package org.ocap.ui;

import java.rmi.Remote;
import java.util.Dictionary;
import javax.tv.service.selection.ServiceContext;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HScreenRectangle;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.system.MonitorAppPermission;

/**
 * <p>This interface provides a set of tools for accomplishing the
 * following:</p>
 *
 * <ol>
 *   <li>modifying the mapping of logical <code>HScreen</code>s
 *   to display <code>HScreen</code>s including the area (extent) on the display
 *   <code>HScreen</code> where a logical <code>HScreen</code> appears, its
 *   visibility, and its z-order (among other <code>HScreen</code>s);</li>
 *   <li>modifying the z-order of <code>HScreenDevice</code>s within
 *   an <code>HScreen</code>;</li>
 *   <li>modifying the set of <code>ServiceContext</code>s
 *   associated with an <code>HScreen</code>;</li>
 *   <li>modifying the association of display
 *   <code>HScreen</code>s and corresponding <code>VideoOutputPort</code>
 *   instances;</li>
 *   <li>modifying the set of <code>HScreenDevice</code>s whose
 *   generated audio constitute the set of audio sources of an
 *   <code>HScreen</code>;</li>
 *   <li>modifying the current audio focus assignment of a display
 *   <code>HScreen</code>;</li>
 *   <li>reserving and releasing reservation of underlying screen and
 *   screen device resources;</li>
 *   <li>obtaining a reference to current resource client that has
 *   reserved screen and its underlying resources;</li>
 *   <li>establishing the currently active per-display multiscreen
 *   configuration of a display <code>HScreen</code>;</li>
 * </ol>
 *
 * <p>If an <code>HScreen</code> instance may be exposed to an OCAP
 * application and if that <code>HScreen</code> is configurable with
 * respect to the functionality defined by this interface, then an
 * MSM implementation SHALL support the <code>MultiScreenConfigurableContext</code>
 * interface on every such <code>HScreen</code> instance.</p>
 *
 * <p>An MSM implementation MAY support this interface on an
 * <code>HScreen</code> instance that is not configurable with respect
 * to the functionality defined by this interface.</p>
 *
 * <p>A given implementation of this interface is not required to
 * support any or all defined configuration changes, but MAY, due to
 * hardware or other constraints, support only specific configuration
 * changes. If an implementation of this interface does not support a
 * specific configuration change, then an attempt to perform that
 * change SHALL cause an <code>IllegalStateException</code> to be
 * raised, as described under each method defined below.</p>
 *
 * @author Glenn Adams
 * @since MSM I01
 *
 * @see MultiScreenContext
 * @see org.davic.resources.ResourceProxy
 **/
public interface MultiScreenConfigurableContext extends MultiScreenContext, ResourceProxy
{
    /**
     * Configurable parameter identifying configurability of audio
     * source(s).
     * 
     * <p>Configuration of the audio source(s) of a screen is accomplished
     * by using the <code>addAudioSources(..)</code> and
     * <code>removeAudioSources(..)</code> methods defined by this
     * interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface supports configuration of its audio source(s), then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_AUDIO_SOURCE)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_AUDIO_SOURCE)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_AUDIO_SOURCE)</code>
     * SHALL return a value of type <code>HScreenDevice[]</code>, where
     * each entry in the value array is an accessible screen device of
     * this screen that can serve as an audio source for this
     * screen.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenDevice
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_AUDIO_SOURCE      = 0;

    /**
     * Configurable parameter identifying configurability of audio
     * focus.
     * 
     * <p>Configuration of the audio focus of a screen is accomplished
     * by using the <code>assignAudioFocus(..)</code> method defined by
     * this interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface supports configuration of its audio source(s), then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_AUDIO_FOCUS)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_AUDIO_FOCUS)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_AUDIO_FOCUS)</code>
     * SHALL return a value of type <code>HScreen[]</code>, where each
     * entry in the value array is an accessible screen that can serve
     * as an audio focus screen.</p>
     *
     * <p>If this screen is a display screen, then the returned entries
     * SHALL be restricted to those logical screens that are currently
     * mapped to this display screen that can be assigned audio focus.
     * Because a display screen can always be assigned audio focus
     * directly (as opposed to assigning audio focus to some logical
     * screen mapped to the display screen), the display screen is not
     * itself included in the returned entries.</p>
     *
     * <p>If this screen is a logical screen and if this logical screen
     * is mapped to a display screen and is capable of being assigned
     * audio focus, then the returned array SHALL contain only this
     * screen; otherwise, the returned array SHALL be empty.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_AUDIO_FOCUS       = 1;

    /**
     * Configurable parameter identifying configurability of screen
     * device z-order.
     * 
     * <p>Configuration of device z-order of a screen is accomplished
     * by using the <code>setZOrder(HScreenDevice[])</code> method
     * defined by this interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface supports configuration of the z-order of its screen
     * device(s), then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_DEVICE_Z_ORDER)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_DEVICE_Z_ORDER)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_DEVICE_Z_ORDER)</code>
     * SHALL return a value of type <code>HScreenDevice[][]</code>,
     * where each entry in the value array is an array of screen devices
     * whose order matches a supported z-ordering of those screen
     * devices, where the first entry of such an array of screen devices
     * is back-most in z-order (i.e., device z-order of zero).</p>
     *
     * <p><b>Example:</b></p>
     *
     * <p>The following assumptions apply:</p>
     * <ol>
     * <li>screen has one background device (B1), one video device (V1), and two graphics
     * devices (G1 and G2);</li>
     * <li>two orderings of graphics devices are supported:
     * (1) B1&lt;V1&lt;G1&lt;G2 and (2) B1&lt;V1&lt;G2&lt;G1;</li>
     * </ol>
     *
     * <pre>
     * MultiScreenConfigurableContext msxx = (MultiScreenConfigurableContext) HScreen.getDefaultHScreen();
     * Object[] values = msxx.getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_DEVICE_Z_ORDER);
     * ASSERT ( values != null );
     * ASSERT ( values instanceof HScreenDevice[][] );
     * ASSERT ( values.length == 2 );
     * HScreenDevice[] order1 = (HScreenDevice[]) values[0];
     * ASSERT ( order1 != null );
     * ASSERT ( order1.length == 4 );
     * ASSERT ( order1[0] instanceof HBackgroundDevice );
     * ASSERT ( order1[1] instanceof HVideoDevice );
     * ASSERT ( order1[2] instanceof HGraphicsDevice );
     * ASSERT ( order1[2].getIDstring().equals ( "G1" ) );
     * ASSERT ( order1[3] instanceof HGraphicsDevice );
     * ASSERT ( order1[2].getIDstring().equals ( "G2" ) );
     * HScreenDevice[] order2 = (HScreenDevice[]) values[1];
     * ASSERT ( order2 != null );
     * ASSERT ( order2.length == 4 );
     * ASSERT ( order2[0] instanceof HBackgroundDevice );
     * ASSERT ( order2[1] instanceof HVideoDevice );
     * ASSERT ( order2[2] instanceof HGraphicsDevice );
     * ASSERT ( order2[2].getIDstring().equals ( "G2" ) );
     * ASSERT ( order2[3] instanceof HGraphicsDevice );
     * ASSERT ( order2[2].getIDstring().equals ( "G1" ) );
     * </pre>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenDevice
     * @see org.havi.ui.HBackgroundDevice
     * @see org.havi.ui.HVideoDevice
     * @see org.havi.ui.HGraphicsDevice
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_DEVICE_Z_ORDER    = 2;

    /**
     * Configurable parameter identifying configurability of screen's
     * associated display area.
     *
     * <p>Configuration of the display area of a (logical) screen is accomplished
     * by using the <code>setDisplayArea(HScreenRectangle)</code> method
     * defined by this interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface is a display screen, then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_AREA)</code>
     * SHALL return <code>false</code>; otherwise, if this logical
     * screen supports configuration of its display area, then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_AREA)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_AREA)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_AREA)</code>
     * SHALL return a value of type <code>HScreenRectangle[]</code>,
     * where each entry in the value array is a supported display
     * area.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenRectangle
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_AREA      = 3;

    /**
     * Configurable parameter identifying configurability of screen's
     * associated display screen.
     *
     * <p>Configuration of the display screen of a (logical) screen is accomplished
     * by using the <code>setDisplayScreen(HScreen)</code> method
     * defined by this interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface is a display screen, then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_SCREEN)</code>
     * SHALL return <code>false</code>; otherwise, if this logical
     * screen supports configuration of its display screen, then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_SCREEN)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_SCREEN)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_SCREEN)</code>
     * SHALL return a value of type <code>HScreen[]</code>,
     * where each entry in the value array is an accessible display
     * screen to which this logical screen MAY be mapped.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_DISPLAY_SCREEN    = 4;

    /**
     * Configurable parameter identifying configurability of screen's
     * associated output port(s).
     * 
     * <p>Configuration of the output port(s) of a screen is accomplished
     * by using the <code>addOutputPorts(..)</code> and
     * <code>removeOutputPorts(..)</code> methods defined by this
     * interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface supports configuration of its video output port(s), then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_OUTPUT_PORT)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_OUTPUT_PORT)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_OUTPUT_PORT)</code>
     * SHALL return a value of type <code>VideoOutputPort[]</code>, where
     * each entry in the value array is an accessible video output port
     * to which this screen may be directly mapped.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.ocap.hardware.VideoOutputPort
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_OUTPUT_PORT       = 5;

    /**
     * Configurable parameter identifying configurability of screen's
     * associated service context(s).
     *
     * <p>Configuration of the service context(s) of a screen is
     * accomplished by using the <code>addServiceContexts(..)</code> and
     * <code>removeServiceContexts(..)</code> methods defined by
     * this interface, and by using the
     * <code>swapServiceContexts(..)</code> and
     * <code>moveServiceContexts(..)</code> methods defined by
     * <code>MultiScreenManager</code>.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface supports configuration of its output port(s), then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_SERVICE_CONTEXT)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_SERVICE_CONTEXT)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_SERVICE_CONTEXT)</code>
     * SHALL return a value of type <code>ServiceContext[]</code>, where
     * each entry in the value array is an accessible service context
     * that can be associated with this screen.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see javax.tv.service.selection.ServiceContext
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_SERVICE_CONTEXT   = 6;

    /**
     * Configurable parameter identifying configurability of screen's
     * visibility.
     *
     * <p>Configuration of the visibility of a screen is accomplished
     * by using the <code>setVisible(boolean)</code> method
     * defined by this interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface supports configuration of its visibility, then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_VISIBILITY)</code>
     * SHALL return <code>true</code>, but
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_VISIBILITY)</code>
     * SHALL return <code>false</code>, implying that if visibility is
     * configurable, then a continuous parameter space (i.e., both
     * <code>true</code> and <code>false</code>) applies.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_VISIBILITY        = 7;

    /**
     * Configurable parameter identifying configurability of screen's
     * z-order.
     *
     * <p>Configuration of the z-order of a screen is accomplished
     * by using the <code>setZOrder(int)</code> method
     * defined by this interface.</p>
     *
     * <p>If the <code>HScreen</code> instance referenced by this
     * interface supports configuration of its z-order (with respect to
     * other screens within its multiscreen configuration), then
     * <code>isConfigurableParameter(CONFIGURABLE_SCREEN_PARAMETER_Z_ORDER)</code>
     * SHALL return <code>true</code>.</p>
     *
     * <p>If
     * <code>hasDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_Z_ORDER)</code>
     * returns <code>true</code>, then
     * <code>getDiscreteParameterSpace(CONFIGURABLE_SCREEN_PARAMETER_Z_ORDER)</code>
     * SHALL return a value of type <code>Integer[]</code>,
     * where each value entry <code><i>v</i></code> in the value array is such that
     * <code><i>v</i>.intValue()</code> returns a supported z-order
     * index for this screen in the context of this screen's
     * multiscreen configuration.</p>
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public static final int CONFIGURABLE_SCREEN_PARAMETER_Z_ORDER           = 8;

    /**
     * Determine if configurable parameter is supported as configurable
     * (as opposed to fixed) by the platform implementation for some
     * screen.
     *
     * @param parameter a configurable screen parameter enumeration
     * value as defined above.
     *
     * @return If the platform implementation supports either
     * continuous or discrete variation of the specified
     * <code><i>parameter</i></code> on this screen, then returns <code>true</code>;
     * otherwise, returns <code>false</code>.
     *
     * @since MSM I01
     */
    public boolean isConfigurableParameter ( int parameter );

    /**
     * Determine if a supported configurable parameter has a discrete
     * or continuously variable value space.
     *
     * <p>In the present context, a "continuously" configurable
     * parameter means the platform supports or can approximate all
     * values of the parameter's value type, while "discrete" means only
     * certain, enumerable values of the parameter's value type may be
     * used as reported by
     * <code>getDiscreteParameterSpace(..)</code>.</p>
     *
     * @param parameter a configurable screen parameter enumeration
     * value as defined above.
     *
     * @return If the platform implementation supports a discrete,
     * (sub)set of values of of the value type space of the specified
     * <code><i>parameter</i></code> on this screen, then returns <code>true</code>;
     * otherwise, returns <code>false</code>, in which case all values
     * of the value type space are supported (or approximated).
     *
     * @throws IllegalArgumentException if
     * <code>isConfigurableParameter(<i>parameter</i>)</code> returns
     * <code>false</code>.
     *
     * @since MSM I01
     */
    public boolean hasDiscreteParameterSpace ( int parameter ) throws IllegalArgumentException;

    /**
     * Obtain the discrete, (sub)set of values of of the value type space
     * of a configurable parameter.
     *
     * <p>The actual runtime type of the array and the array's elements
     * returned by this method SHALL be as defined for the specified
     * configurable parameter as documented by each configurable
     * parameter's specification above.</p>
     *
     * <p>Unless indicated otherwise by the definition of a specific
     * configurable parameter, the order of entries in the array
     * returned by this method is not defined by this specification,
     * and SHALL be considered implementation dependent by an
     * interoperable application.</p>
     * 
     * <p>A set of supported discrete parameters MAY change for a given
     * screen and a given configurable parameter over the lifetime of a
     * screen based upon the dynamic state of the screen's underlying
     * resources; however, such a change, if it occurs, SHALL NOT occur
     * outside the time interval between the completion of dispatching
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING</code>
     * event and the completion of dispatching the corresponding
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGED</code>
     * event.</p>
     * 
     * @param parameter a configurable screen parameter enumeration
     * value as defined above.
     *
     * @return an array of object instances, each of
     * which denote a discrete value of the specified parameter that is
     * supported (or approximatable) by the platform.
     *
     * @throws IllegalArgumentException if
     * <code>isConfigurableParameter(<i>parameter</i>)</code> or
     * <code>hasDiscreteParameterSpace(<i>parameter</i>)</code>
     * returns
     * <code>false</code>.
     *
     * @since MSM I01
     */
    public Object[] getDiscreteParameterSpace ( int parameter ) throws IllegalArgumentException;

    /**
     * Set screen visibility.
     * 
     * <p>If this screen is a logical screen, then cause it to be marked
     * as visible (if previously hidden) or hidden (if previously
     * visible); otherwise, if this screen is a display screen, then
     * cause all logical screens mapped to this display screen to be
     * marked as visible (if previously hidden) or hidden (if previously
     * visible).</p>
     *
     * @param visible a boolean value indicating whether this logical
     * <code>HScreen</code> or the logical screens mapped to this
     * display <code>HScreen</code> should be made visible or hidden
     * (non-visible) on its associated display <code>HScreen</code>.
     *
     * @throws SecurityException if the calling thread has not been
     * granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if the visibility for this
     * <code>HScreen</code> cannot be changed, e.g., if the platform
     * uses a permanent visibility setting.
     *
     * @since MSM I01
     **/
    public void setVisible ( boolean visible ) throws SecurityException, IllegalStateException;

    /**
     * Set screen z-order.
     * 
     * <p>Cause this logical <code>HScreen</code> to change its z-order
     * among other logical <code>HScreen</code>s mapped to the same
     * display <code>HScreen</code>.</p>
     *
     * @param order a positive integer value indicating the new z-order to
     * assign to this logical <code>HScreen</code>.
     *
     * @throws SecurityException if the calling thread has not been
     * granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if this <code>HScreen</code>'s
     * type is not <code>SCREEN_TYPE_LOGICAL</code> or if the z-order for
     * this <code>HScreen</code> cannot be changed, e.g., if the platform uses a
     * permanent z-order setting.
     *
     * @since MSM I01
     **/
    public void setZOrder ( int order ) throws SecurityException, IllegalStateException;

    /**
     * Set the screen device z-order within this screen for a
     * set of screen devices.
     * 
     * <p>Atomically set the z-order of the specified set of
     * <code>HScreenDevice</code> within this screen where the following
     * constraints apply:</p>
     *
     * <ul>
     * <li>if an <code>HBackgroundDevice</code> is present in the
     * specified set of devices, then it (1) precedes any
     * <code>HVideoDevice</code> contained in the set of devices and (2)
     * precedes any <code>HGraphicsDevice</code> contained in the set of
     * devices;</li>
     * <li>if an <code>HVideoDevice</code> is present in the specified
     * set of devices, then it precedes any <code>HGraphicsDevice</code>
     * contained in the set of devices;</li>
     * </ul>
     *
     * <p>If no exception is thrown by this method, then the set of
     * specified <code>HScreenDevice</code> instances will be ordered
     * such that
     * <code>MultiScreenContext.getZOrder(HScreenDevice)</code> when
     * invoked on this screen with any of the specified devices will
     * return a z-order index that preserves the relative order of the
     * specified devices.</p>
     *
     * <p>If fewer than the entire set of <code>HScreenDevice</code>
     * instances associated with this screen is provided in the
     * <code><i>devices</i></code> argument, then the resulting relative order of
     * unspecified devices with respect to specified devices is not
     * defined, except that the constraints defined by
     * <code>MultiScreenContext.getZOrder(HScreenDevice)</code> SHALL
     * apply to the total ordering of devices in this screen after this
     * method returns.</p>
     *
     * @param devices an ordered array of <code>HScreenDevice</code>
     * instances that are associated with this screen.
     *
     * @throws SecurityException if the calling thread has not been
     * granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalArgumentException if <code><i>device</i></code> is not an
     * <code>HScreenDevice</code> of this screen.
     *
     * @throws IllegalStateException if (1) the z-order for the specified
     * <code>HScreenDevice</code> cannot be changed, e.g., if the
     * platform uses a permanent z-order setting for screen devices in
     * this screen, or (2) the order of specified devices does not
     * permit the assignment of z-order indices that satisfies the
     * above constraints.
     *
     * @since MSM I01
     **/
    public void setZOrder ( HScreenDevice[] devices ) throws SecurityException, IllegalArgumentException, IllegalStateException;

    /**
     * Add audio source(s) for this screen.
     * 
     * <p>Add one or more <code>HScreenDevice</code> instances to the
     * set of audio sources from which presented audio is selected (and
     * mixed) for the purpose of audio presentation from this
     * screen.</p>
     *
     * <p>If a specified audio source is already
     * designated as an audio source for this screen, but
     * <code><i>mixWithAudioFocus</i></code> differs from that specified when it was
     * added as an audio source, then the new <code><i>mixWithAudioFocus</i></code>
     * value applies.</p>
     *
     * @param devices a non-empty array of <code>HScreenDevice</code>
     * instances, where each such instance contributes to a mixed, audio
     * presentation from this screen.
     *
     * @param mixWithAudioFocus if <code>true</code>, then the specified
     * screen devices contribute (mix) audio to (with) any audio output
     * associated with any video output port with which this screen is
     * associated (directly or indirectly) regardless of whether or not
     * this screen is assigned audio focus; if <code>false</code>, then
     * they contribute audio only when this screen is assigned audio
     * focus.
     *
     * @throws SecurityException if the calling thread has not been
     * granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalArgumentException if <code><i>devices</i></code> is not a
     * non-empty array or some entry of <code><i>devices</i></code> is not an
     * <code>HScreenDevice</code> of this screen.
     *
     * @throws IllegalStateException if (1) the audio sources for this
     * <code>HScreen</code> cannot be changed, e.g., if the
     * platform uses a permanent audio source setting for screen devices in
     * this screen, or (2) if multiple audio sources are specified and
     * audio mixing is not supported.
     *
     * @since MSM I01
     **/
    public void addAudioSources ( HScreenDevice[] devices, boolean mixWithAudioFocus ) throws SecurityException, IllegalArgumentException, IllegalStateException;

    /**
     * Remove audio source(s) for this screen.
     * 
     * <p>Removes all or some non-empy set of specific
     * <code>HScreenDevice</code> instances from the set of audio
     * sources of this <code>HScreen</code>. If <code><i>devices</i></code> is
     * <code>null</code>, then all audio sources are removed.</p>
     *
     * @param devices either <code>null</code> or a non-empty set of
     * <code>HScreenDevice</code> instances.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalArgumentException if <code><i>devices</i></code> is not
     * <code>null</code> and some <code>HScreenDevice</code> entry is
     * not associated with this <code>HScreen</code> instance.
     *
     * @throws IllegalStateException if a specified
     * <code>HScreenDevice</code> used as an audio source for this
     * <code>HScreen</code> cannot be changed, e.g., if the platform
     * uses a permanent association of audio sources with the specified
     * <code>HScreenDevice</code>.
     *
     * @since MSM I01
     **/
    public void removeAudioSources ( HScreenDevice[] devices ) throws SecurityException, IllegalArgumentException, IllegalStateException;

    /**
     * Assign audio focus to this screen.
     *
     * <p>At any given time, a display screen SHALL assign audio focus
     * to itself or or exactly one logical screen that maps to it (the
     * display screen). When audio focus is (newly) assigned to a
     * logical screen of a display screen and that logical screen does
     * not currently have audio focus assigned to it, then audio focus
     * SHALL be removed from any other logical screen that is mapped to
     * that display screen and assigned instead to the newly assigned
     * logical screen.</p>
     *
     * <p>If no logical screen is mapped to a given display screen or no
     * logical screen mapped to a given display screen is assigned audio
     * focus, then the display screen SHALL assign itself audio focus
     * (by default). Audio focus MAY be explicitly assigned to a
     * display screen by using this method on a display screen
     * instance.</p>
     * 
     * <p>The audio focus screen of a display screen is the screen whose
     * currently selected audio sources are assigned to be rendered on
     * all (implied) audio presentation devices of all video output
     * ports to which the display screen is mapped. If the screen to
     * which audio focus is assigned has no audio source, i.e., has an
     * empty set of audio sources, then audio (as produced by or
     * potentially produced by the OCAP platform) SHALL NOT be rendered
     * by any (implied) audio presentation device of all video output
     * ports to which the display screen is mapped.</p>
     * 
     * <p><em>Note:</em> Each distinct display screen may have a
     * distinct logical screen to which audio focus is assigned for the
     * purpose of rendering audio of the display screen (and its
     * collection of mapped logical screens).</p>
     * 
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if audio focus cannot be assigned
     * to this <code>HScreen</code> or the current audio focus cannot be changed.
     * 
     * @since MSM I01
     **/
    public void assignAudioFocus() throws SecurityException, IllegalStateException;

    /**
     * Add video output port(s) to which screen is mapped.
     * 
     * <p>If this <code>HScreen</code> is a logical screen rather than
     * a display screen, then it SHALL be considered to function as
     * equivalent to a display screen for the purpose of mapping to the
     * specified video output port(s); i.e., the logical screen is treated
     * as if it were a main display screen on its own accord.</p>
     *
     * @param ports a non-empty array of <code>VideoOutputPort</code>
     * instances.
     *
     * @param removeExisting if <code>true</code>, then remove association with
     * existing screen (if such association exists) before adding to new screen;
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if (1) the specified
     * <code>VideoOutputPort</code> is already associated with a
     * different <code>HScreen</code> and <code><i>removeExisting</i></code> is
     * not <code>true</code>, (2) this <code>HScreen</code> cannot be
     * mapped to some specified <code>VideoOutputPort</code> due to some
     * platform specific hardware constraint, or (3) the set of
     * <code>VideoOutputPort</code> instances to which this <code>HScreen</code>
     * is mapped cannot be changed, e.g., if the platform uses a permanent
     * association with a specific set of <code>VideoOutputPort</code>
     * instances.
     *
     * @since MSM I01
     **/
    public void addOutputPorts ( VideoOutputPort[] ports, boolean removeExisting ) throws SecurityException, IllegalStateException;

    /**
     * Remove video output port(s) to which screen is mapped.
     * 
     * <p>Removes all or some non-empty set of specific
     * <code>VideoOutputPort</code> instances from the set of video
     * output ports to which this <code>HScreen</code> is
     * mapped. If <code><i>ports</i></code> is <code>null</code>, then all video
     * output ports associations are removed.</p>
     *
     * @param ports either <code>null</code> or a non-empty array of
     * <code>VideoOutputPort</code> instances.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalArgumentException if <code><i>ports</i></code> is not
     * <code>null</code> and some <code>VideoOutputPort</code> entry is
     * not associated with this <code>HScreen</code> instance.
     *
     * @throws IllegalStateException if a specified <code>VideoOutputPort</code>
     * for this <code>HScreen</code> cannot be changed, e.g., if the
     * platform uses a permanent association with the specified
     * <code>VideoOutputPort</code>.
     *
     * @since MSM I01
     **/
    public void removeOutputPorts ( VideoOutputPort[] ports ) throws SecurityException, IllegalArgumentException, IllegalStateException;

    /**
     * Associate logical screen with display screen.
     * 
     * <p>Associates this logical <code>HScreen</code> with a display
     * <code>HScreen</code>.</p>
     *
     * @param screen a display <code>HScreen</code> instance or
     * <code>null</code>. If <code>null</code>, and if this method executes
     * without an exception, then upon return, this logical <code>HScreen</code>
     * is no longer associated with a display <code>HScreen</code>.
     *
     * @throws SecurityException if the calling thread has not been
     * granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if (1) this <code>HScreen</code>'s
     * type is not <code>SCREEN_TYPE_LOGICAL</code>, (2) the specified
     * <code><i>screen</i></code> is not <code>null</code> and its
     * screen type is not <code>SCREEN_TYPE_DISPLAY</code>, or (3)
     * the display <code>HScreen</code> associated with this logical
     * <code>HScreen</code> cannot be changed, e.g., if the platform
     * uses a permanent association with a specific display
     * <code>HScreen</code>.
     *
     * @since MSM I01
     **/
    public void setDisplayScreen ( HScreen screen ) throws SecurityException, IllegalStateException;

    /**
     * Set area of display screen to which logical screen is mapped.
     * 
     * <p>Associates this logical <code>HScreen</code> with an area (extent) of
     * its associated display <code>HScreen</code>.</p>
     *
     * @param rect an <code>HScreenRectangle</code> instance specifying the area
     * on the display <code>HScreen</code> associated with this logical
     * <code>HScreen</code> that SHALL correspond to the extent of this logical
     * <code>HScreen</code>.
     *
     * @throws SecurityException if the calling thread has not been
     * granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if (1) this <code>HScreen</code>'s
     * type is not <code>SCREEN_TYPE_LOGICAL</code> or (2) the area of the display
     * <code>HScreen</code> associated with this logical <code>HScreen</code>
     * cannot be changed, e.g., if the platform uses a permanent association
     * with a specific area of the associated display <code>HScreen</code>.
     *
     * @since MSM I01
     **/
    public void setDisplayArea ( HScreenRectangle rect ) throws SecurityException, IllegalStateException;

    /**
     * Test compatibility of service context with screen.
     * 
     * <p>Determine if application may assign <code>ServiceContext</code> to this
     * <code>HScreen</code> and if the specified <code>ServiceContext</code> is
     * compatible with presentation on this <code>HScreen</code>.</p>
     * 
     * @param context a valid <code>ServiceContext</code> instance.
     *
     * @return A boolean value indicating if the specified
     * <code>ServiceContext</code> instance can be assigned to this
     * <code>HScreen</code>. If it can be assigned, <code>true</code> is
     * returned; otherwise, <code>false</code> is returned.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalArgumentException if the specified
     * <code>ServiceContext</code> is not valid.
     *
     * @since MSM I01
     **/
    public boolean checkServiceContextCompatibility ( ServiceContext context ) throws SecurityException, IllegalArgumentException;

    /**
     * Add service context(s) to this screen.
     * 
     * <p>Add one or more <code>ServiceContext</code> instances to the
     * set of service contexts associated with this <code>HScreen</code>
     * in order to permit background, video, and graphics content from
     * these <code>ServiceContext</code> instances to be presented on the
     * <code>HScreen</code>'s respective screen devices.</p>
     *
     * <p>If a specified service context is already associated with the
     * underlying screen represented by this interface, then that
     * service context is not multiply associated with this screen, but
     * the existing association remains intact; that is, a given service
     * context SHALL be associated with a given (underlying) screen
     * either once or not at all.</p>
     *
     * <p>If more than one non-abstract service context is associated
     * with a given screen, and if multiple video sources from the
     * background based players of these multiple non-abstract service
     * contexts are associated with a given <code>HVideoDevice</code>
     * instance of the given screen, then the background based player
     * from these multiple service contexts whose video content is to be
     * displayed is determined according to the following ordered
     * rules:</p>
     *
     * <ol>
     * <li>If the owning application of one of the associated
     * non-abstract service contexts holds a reservation on a given
     * <code>HVideoDevice</code> of the screen, then background based
     * player content from that service context is designated for
     * presentation on that video device;</li>
     * <li>Otherwise, the background based player of the associated
     * non-abstract service context whose application is assigned the
     * highest priority is designated for presentation on that video
     * device;</li>
     * </ol>
     *
     * <p>If, after applying the above rules, multiple background based
     * players of a given application are designated for presentation on
     * a video device, then the player that was most recently started is
     * given priority for video presentation.</p>
     *
     * @param contexts a non-empty array of <code>ServiceContext</code>
     * instances.
     *
     * @param associateDefaultDevices if <code>true</code>, then
     * associate default screen devices of this screen with all
     * <code>ServiceMediaHandler</code> instances currently associated
     * with the specified <code>ServiceContext</code> instances;
     * otherwise, if <code>false</code>, then do not perform this
     * association with default screen devices.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if some specified <code>ServiceContext</code> for
     * this <code>HScreen</code> cannot be changed, e.g., if the
     * platform uses a permanent association with a specific
     * <code>ServiceContext</code>.
     *
     * @since MSM I01
     **/
    public void addServiceContexts ( ServiceContext[] contexts, boolean associateDefaultDevices ) throws SecurityException, IllegalStateException;

    /**
     * Remove service context(s) from screen.
     * 
     * <p>Remove all or some non-empty set of specific
     * <code>ServiceContext</code> instances from the set of service contexts
     * associated with this <code>HScreen</code>. If
     * <code><i>contexts</i></code> is
     * <code>null</code>, then all service contexts are removed.</p>
     *
     * @param contexts either <code>null</code> or a non-empty array of
     * <code>ServiceContext</code> instances.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalArgumentException if <code><i>contexts</i></code> is not
     * <code>null</code> and some
     * <code>ServiceContext</code> entry is not associated with this
     * <code>HScreen</code> instance.
     *
     * @throws IllegalStateException if a specified
     * <code>ServiceContext</code> cannot be changed, e.g., if the
     * platform uses a permanent association with a specified
     * <code>ServiceContext</code>.
     *
     * @since MSM I01
     **/
    public void removeServiceContexts ( ServiceContext[] contexts ) throws SecurityException, IllegalArgumentException, IllegalStateException;

	/**
     * Set currently active multiscreen configuration for this display
     * screen (i.e., choose among the set of subsets of logical screens
     * that may be associated with this display screen at a given time).
     *
     * <p>If the specified <code><i>configuration</i></code> is the current
     * configuration for this display screen, then, unless
     * <code>SecurityException</code> applies, return from this method
     * without producing any side effect.</p>
     *
     * <p>If the specified <code><i>configuration</i></code> is not the current
     * configuration for this display screen and if
     * <code>SecurityException</code>,
     * <code>IllegalStateException</code>, and
     * <code>IllegalStateException</code> do not apply, then perform the
     * synchronous display multiscreen configuration change processing
     * defined in the <i>OCAP Multiscreen Manager (MSM) Extension</i>
     * specification.</p>
     *
     * <p>If a <code><i>serviceContextAssociations</i></code> argument is specified
     * (i.e., not <code>null</code>), then any
     * <code>ServiceContext</code> instance that is accessible to the
     * invoking application SHALL be associated with either no screen or
     * the applicable screen(s) in the specified (new) multiscreen
     * configuration. If no association matches some accessible
     * <code>ServiceContext</code>, if some
     * accessible <code>ServiceContext</code> instance is not present in
     * the specified associations, or if it is present but no such
     * applicable screen exists in the new multiscreen
     * configuration, then the <code>ServiceContext</code> instance
     * SHALL be associated with the default service context association
     * screen of the specified multiscreen configuration, i.e., the
     * screen returned by
     * <code><i>configuration</i>.getDefaultServiceContextScreen()</code>.</p>
     *
     * <p>For the purpose of matching accessible
     * <code>ServiceContext</code> instances whose references appear as
     * keys in a specified
     * <code><i>serviceContextAssociations</i></code> dictionary, the
     * virtual method <code>equals(Object)</code> on these keys SHALL be
     * used, in which case it is assumed that this method behaves
     * identically to the default implementation of
     * <code>Object.equals(Object)</code>.</p>
     *
     * <p><em>Note:</em> In the context of a given application instance,
     * the MSM host implementation should maintain a one-to-one relationship
     * between <code>ServiceContext</code> instances exposed to that
     * application and collections of underyling service context
     * resources. If the MSM host implementation fails to maintain this
     * relationship, then when consulting a
     * <code><i>serviceContextAssociations</i></code> dictionary, the
     * MSM implemenation may consider two distinct collections of
     * underlying service context resources to be the same service
     * context, e.g., if at different times, a single
     * <code>ServiceContext</code> instance references distinct underlying
     * collections of resources, or may consider a single collection of
     * underlying service context resources to be two distinct service
     * contexts, e.g., if at a given time, two distinct
     * <code>ServiceContext</code> instances reference the same
     * underlying collection of resources.</p>
     * 
	 * <p>The state of the decoder format conversion (DFC) component of
	 * a video pipeline being used to process video associated with a
 	 * service context that is implicitly swapped or moved between screens by
	 * this method SHALL NOT be affected by performance of this
	 * method.</p>
	 * 
     * @param configuration a <code>MultiScreenConfiguration</code>
     * instance to become the currently active per-display multiscreen
     * configuration for this display screen.
     * 
     * @param serviceContextAssociations if not <code>null</code>, then
     * a <code>Dictionary</code> instance whose keys are
     * <code>ServiceContext</code> instances and whose values are
     * <code>String</code> instances, where the string values are
     * defined as follows:
     * (1) if the string value is <code>"-"</code>, then no screen
     * applies (in which case a matching service context is not
     * associated with any screen after the configuration change),
     * (2) otherwise, if the string value is <code>"*"</code>,
     * then all screens of the new screen configuration apply,
     * (3) otherwise, if the string value is a screen identifier as returned by 
     * <code>MultiScreenContext.getID()</code>, then that screen
     * applies,
     * (4) otherwise, if the string value is a screen category as returned by 
     * <code>MultiScreenContext.getScreenCategory()</code>, then any
     * screen of the new configuration with that category applies,
     * (5) otherwise, if the string value is a semicolon separated list
     * of screen identifiers, then each screen of the new configuration
     * with a matching identifier applies,
     * (6) otherwise, if the string value is a semicolon separated list
     * of screen categories, then each screen of the new configuration
     * with a matching category applies,
     * (7) otherwise, if the string value is a semicolon separated list
     * of screen identifiers or screen categories, then each screen of the new configuration
     * with a matching identifier or category applies,
     * (8) otherwise, the default service context association screen of
     * the new configfuration applies.
     * 
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @throws IllegalArgumentException if
     * <code><i>configuration</i></code> is not one of this (display)
     * screen's multiscreen configurations as would be returned by
     * <code>MultiScreenContext.getMultiScreenConfigurations()</code>.
     *
     * @throws IllegalStateException if this screen is not a display
     * screen or if the MSM implementation (1) does
     * not permit activation of the specified multiscreen configuration,
     * (2) if this method was previously called and the change
     * processing steps are not yet complete, or (3)
     * if activation is not otherwise permitted at the time of method
     * invocation.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     * @see org.ocap.system.MonitorAppPermission
     **/
	public void setMultiScreenConfiguration ( MultiScreenConfiguration configuration, Dictionary serviceContextAssociations )
		throws SecurityException, IllegalArgumentException, IllegalStateException;

	/**
     * Request that the currently active multiscreen configuration for
     * this display screen be changed.
     *
     * <p>If the specified <code><i>configuration</i></code> is the current
     * configuration for this display screen, then, unless
     * <code>SecurityException</code> applies, return from this method
     * without producing any side effect.</p>
     *
     * <p>If the specified <code><i>configuration</i></code> is not the current
     * display configuration and if <code>SecurityException</code>,
     * <code>IllegalArgumentException</code>, and
	 * <code>IllegalStateException</code> do not apply, then initiate an
     * asynchronous change to the current multiscreen configuration,
     * where the semantics of <code>setMultiScreenConfiguration()</code>
     * apply except that this method SHALL return immediately after
     * <code>MultiScreenConfiguration.MULTI_SCREEN_CONFIGURATION_CHANGING</code>
     * is generated (but before it is dispatched).</p>
     *
     * @param configuration a <code>MultiScreenConfiguration</code>
     * instance to become the currently active screen configuration.
     * 
     * @param serviceContextAssociations either <code>null</code> or
     * a <code>Dictionary</code> instance whose keys are
     * <code>ServiceContext</code> instances and whose values are
     * <code>String</code> instances, with semantics as defined by
     * <code>setMultiScreenConfiguration(..)</code> above.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @throws IllegalArgumentException if
     * <code><i>configuration</i></code> is not one of this (display)
     * screen's multiscreen configurations as would be returned by
     * <code>MultiScreenContext.getMultiScreenConfigurations()</code>.
     *
     * @throws IllegalStateException if this screen is not a display
     * screen or (1) if the MSM implementation does
     * not permit activation of the specified multiscreen configuration,
     * (2) if this method was previously called and the change
     * processing steps are not yet complete, or (3) if activation is
     * not otherwise permitted at the time of method invocation.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     * @see org.ocap.system.MonitorAppPermission
     */
	public void requestMultiScreenConfigurationChange ( MultiScreenConfiguration configuration, Dictionary serviceContextAssociations )
		throws SecurityException, IllegalArgumentException, IllegalStateException;

    /**
     * Obtain the resource client that currently holds the reservation
     * on the underlying screen and screen resources associated with
     * this <code>HScreen</code> instance.
     * 
     * @return a <code>ResourceClient</code> instance or
     * <code>null</code> if the underlying screen and screen resources
     * associated with this <code>HScreen</code> are not reserved.
     *
     * @since MSM I01
     **/
	public ResourceClient getClient();

    /**
     * Atomically reserve underlying resources of this screen.
     * 
     * <p>Reserving a screen SHALL be considered equivalent to
     * reserving all <code>HScreenDevice</code> instances associated
     * with the screen, except that when the screen is reserved using
     * this method, individual <code>HScreenDevice</code> instances
     * SHALL not be released without first releasing all
     * <code>HScreenDevice</code> instances and all underlying screen
     * resources of this screen.</p>
     *
     * <p>If when reserving a screen, some <code>HScreenDevice</code> of
     * the screen is already reserved by another application, then that
     * reservation must be successfully released (by the MSM
     * implementation) prior to granting reservation to the screen; and,
     * if it is not or cannot be released, then an attempt to reserve
     * the screen SHALL fail (i.e., this method returns
     * <code>false</code>).</p>
     *
     * <p>If when reserving a screen, some <code>HScreenDevice</code>
     * of the screen is already reserved by the reserving application,
     * then that reservation SHALL be implicitly subsumed by the
     * successful reservation of the entire screen.</p>
     *
     * <p>If an attempt to reserve a screen using this method succeeds,
     * i.e., returns <code>true</code>, then the
     * <code>getClient()</code> method of all
     * <code>HScreenDevice</code> instances of the screen SHALL return
     * the same value as the <code>getClient()</code> method defined by
     * this interface (as implemented by the concrete implementation of
     * <code>HScreen</code>).</p>
     *
     * <p>If this screen was previously unreserved and invocation of
     * this method results in it becoming reserved, then, prior to
     * returning from this method, a
     * <code>MultiScreenResourceEvent.MULTI_SCREEN_RESOURCE_SCREEN_RESERVED</code>
     * event SHALL be generated.</p>
     * 
     * <p>If the calling application already holds a reservation on this
     * screen and if the specified <code><i>client</i></code> and
     * <code><i>requestData</i></code>
     * arguments are identical to those specified when the calling
     * application was previously granted the reservation, then this
     * method SHALL have no side effect and return <code>true</code>.
     * However, if the calling application already holds a reservation
     * on this screen but either the specified
     * <code><i>client</i></code> or
     * <code><i>requestData</i></code> argument differs from those specified when the
     * calling application was previously granted the reservation, then
     * (1) the specified <code><i>client</i></code> and
     * <code><i>requestData</i></code> SHALL
     * replace those previously specified, (2) the calling application
     * SHALL retain the reservation, and (3) this method SHALL return
     * <code>true</code>.</p>
     * 
     * @param client a <code>ResourceClient</code> instance.
     *
     * @param requestData either <code>null</code> or an Object
     * instance that implements the <code>java.rmi.Remote</code> interface.
     *
     * @return <code>true</code> if underlying resources of screen were
     * successfully reserved, otherwise <code>false</code>.
     *
     * @since MSM I01
     *
     * @see java.rmi.Remote
     **/
    public boolean reserveScreen ( ResourceClient client, Object requestData );

    /**
     * Atomically release underlying resources of screen.
     *
     * <p>If the calling application does not hold a reservation on
     * this screen, then this method SHALL have no side effect.</p>
     * 
     * <p>If this screen was previously reserved and invocation of this
     * method results in it being no longer reserved (i.e., becoming
     * unreserved), then, prior to returning from this method, a
     * <code>MultiScreenResourceEvent.MULTI_SCREEN_RESOURCE_SCREEN_RELEASED</code>
     * event SHALL be generated.</p>
     *
     * @since MSM I01
     **/
    public void releaseScreen();

}
