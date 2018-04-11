
package org.ocap.ui;

import java.awt.Dimension;
import java.util.Dictionary;
import javax.media.Player;
import javax.tv.service.selection.ServiceContext;
import org.davic.resources.ResourceServer;
import org.davic.resources.ResourceStatusListener;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HVideoDevice;
import org.havi.ui.HGraphicsDevice;
import org.ocap.ui.event.MultiScreenConfigurationListener;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.system.MonitorAppPermission;

/**
 * <p>The <code>MultiScreenManager</code> class is an abstract, singleton
 * management class implemented by an OCAP host platform that provides
 * multiscreen management services.</p>
 *
 * <p>For other semantic constraints and behavior that apply, see the
 * <i>OCAP Multiscreen Manager (MSM) Extension</i> specification.</p>
 * 
 * @author Glenn Adams
 * @since MSM I01
 *
 * @see org.davic.resources.ResourceServer
**/
public abstract class MultiScreenManager implements ResourceServer
{
    /**
     * Protected default constructor.
     *
     * @since MSM I01
     **/
    protected MultiScreenManager() {}

    /**
     * Obtain the set of accessible <code>HScreen</code> instances.
     *
     * <p>The set of <code>HScreen</code> instances returned SHALL be
     * determined as follows: when called by an OCAP application that is
     * granted <code>MonitorAppPermission("multiscreen.configuration")</code>,
     * then an <code>HScreen</code> instance SHALL be returned for each
     * display screen and each logical screen exposed through any
     * accessible <code>MultiScreenConfiguration</code> instance at the
     * time of this method's invocation; otherwise, an
     * <code>HScreen</code> instance SHALL be returned for each display
     * screen and each logical screen with which an accessible
     * <code>ServiceContext</code> is associated (either directly or
     * indirectly) for the purpose of presentation or potential
     * presentation.</p>
     *
     * <p>The first <code>HScreen</code> instance in the returned
     * <code>HScreen[]</code> array SHALL be by the same value returned
     * by the <code>getDefaultScreen()</code> method of this class.
     * Subsequent elements of the returned array do not follow a
     * prescribed order, and an application SHALL NOT rely upon the
     * order of these subsequent elements.</p>
     *
     * <p>The set of <code>HScreen</code> instances returned by this
     * method MAY change over the course of an application's lifecycle,
     * such as when a screen is added to or removed from an
     * accessible multiscreen configuration; however, the
     * <code>HScreen</code> instance reference that represents an
     * application's default <code>HScreen</code> SHALL remain constant
     * over the application's lifecycle. That is, from the perspective
     * of a given application instance, an MSM implementation
     * SHALL always return the same <code>HScreen</code> instance
     * reference as the first element of the returned array of
     * <code>HScreen</code> instances.</p>
     *
     * <p>Any <code>HScreen</code> instance returned by the this method
     * SHALL NOT be equivalent to the <i>empty <code>HScreen</code></i>
     * during the interval between the time when the method returns and
     * the time when a
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING</code> or
     * <code>MultiScreenContextEvent.MULTI_SCREEN_CONTEXT_SERVICE_CONTEXT_CHANGED</code>
     * event is generated and dispatched.</p>
     *
     * <p>Each <code>HScreen</code> instance returned by this method
     * MAY return different sets of <code>HScreenDevice</code>
     * instances from its <code>getHBackgroundDevices()</code>,
     * <code>getHVideoDevices()</code>, and
     * <code>getHGraphicsDevices()</code> methods over the course of an
     * application's lifecycle; however, as described below under the
     * definition of the <code>getDefaultScreen()</code> method, the
     * set of default <code>HBackgroundDevice</code>,
     * <code>HVideoDevice</code>, and <code>HGraphicsDevice</code>
     * instances returned via these methods from a default
     * <code>HScreen</code> instance SHALL remain constant over
     * an application's lifecycle, while the underlying device
     * resources and configurations of these default
     * <code>HScreenDevice</code> instances MAY change.</p>
     *
     * <p>Any background, video, or graphics screen device, i.e.,
     * <code>HBackgroundDevice</code>, <code>HVideoDevice</code>, or
     * <code>HGraphicsDevice</code>, of any <code>HScreen</code>
     * returned by this method SHALL NOT be equivalent to the <i>empty
     * <code>HScreenDevice</code></i> of its specific sub-type during
     * the interval between the time when this method returns and the
     * time when a
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING</code> or
     * <code>MultiScreenContextEvent.MULTI_SCREEN_CONTEXT_SERVICE_CONTEXT_CHANGED</code>
     * event is generated.</p>
     *
     * <p>The number of <code>HScreenDevice</code> instances returned
     * from the <code>getHBackgroundDevices()</code>,
     * <code>getHVideoDevices()</code>, and
     * <code>getHGraphicsDevices()</code> methods of an
     * <code>HScreen</code> instance returned by this method MAY change
     * over the lifecycle of that <code>HScreen</code> instance.  If
     * they do change, then a
     * <code>MultiScreenContextEvent.MULTI_SCREEN_CONTEXT_DEVICES_CHANGED</code>
     * SHALL be generated and dispatched to all registered
     * <code>MultiScreenContextListener</code> instances.</p>
     *
     * <p>If the number of <code>HScreenDevice</code> instances of a
     * particular type increases or remains the same (for a given
     * <code>HScreen</code> instance), then any reference to a
     * non-default <code>HScreenDevice</code> instance previously
     * returned to the application SHALL remain viable, and SHALL, at
     * the option of the MSM implementation, either (1) be reused to
     * represent the new underlying device resources of that type of
     * screen device or (2) be reset to the <i>empty
     * <code>HScreenDevice</code></i> of the appropriate sub-type
     * as defined above.  In the former case, the reused
     * <code>HScreenDevice</code> instance SHALL be present in the set
     * of screen devices returned from
     * <code>getHBackgroundDevices()</code>,
     * <code>getHVideoDevices()</code>, or
     * <code>getHGraphicsDevices()</code> method according to its screen
     * device type; in the latter case, the <code>HScreenDevice</code>
     * instance whose state is reset to the <i>empty
     * <code>HScreenDevice</code></i> state
     * SHALL NOT be present in the set of screen devices
     * returned by these methods.</p>
     *
     * <p>If the number of <code>HScreenDevice</code> instances of a
     * particular type decreases (for a given <code>HScreen</code>
     * instance), then any reference to a non-default
     * <code>HScreenDevice</code> instance previously returned to the
     * application SHALL remain viable, SHALL be reset to the <i>empty
     * screen device state</i>, and SHALL NOT be present in the set of
     * screen devices returned by the <code>HScreen</code> instance.</p>
     *
     * <p>The net effect of the above specified behavior is that an
     * application that accesses only its default <code>HScreen</code>
     * and default <code>HScreenDevice</code> instances can continue to
     * access and use those instances without any knowledge of the
     * existence of MSM functionality. In contrast, an application that
     * accesses non-default <code>HScreen</code> instances or
     * non-default <code>HScreenDevice</code> instances needs to monitor
     * changes to the current per-platform and per-display multiscreen
     * configurations as well as
     * changes to the set of screen devices associated with a
     * non-default screen.</p>
     *
     * <p>If a non-default <code>HScreen</code> instance that was
     * previously referenced by an application is reset to the empty
     * screen state as a result of a multiscreen configuration change,
     * the application can detect this fact by comparing the set of
     * <code>HScreen</code> instance references that were obtained prior
     * to an appropriate <code>MultiScreenConfigurationEvent</code> with those
     * obtainable after the event. After this event, those
     * <code>HScreen</code> instances that were reset to the empty state
     * will no longer be present in the array of <code>HScreen</code>
     * instances returned by this method.  Furthermore, those
     * previously obtained <code>HScreen</code> instances can be
     * queried as to whether they were reset by using the
     * <code>isEmptyScreen(HScreen)</code> method defined by this class.
     * Lastly, if the application continues to make use of such a reset
     * <code>HScreen</code> instance, then its behavior is well defined
     * and immutable.</p>
     *
     * <p>Similarly, if a non-default <code>HScreenDevice</code>
     * instance that was previously referenced by an application is
     * reset to the empty screen device state as a result of a
     * multiscreen configuration change, the application can detect this
     * fact by comparing the set of <code>HScreenDevice</code> instance
     * references that were obtained prior to an appropriate
     * <code>MultiScreenContextEvent</code> with those obtainable after
     * the event. After this event, those <code>HScreenDevice</code>
     * instances that were reset to the empty state will no longer be
     * accessible through set of accessible <code>HScreen</code>
     * instances returned by this method.  Furthermore, they can be
     * queried as to whether they were reset by using the
     * <code>isEmptyScreenDevice(HScreenDevice)</code> method defined by
     * this class. Lastly, if the application continues to make use of
     * such a reset <code>HScreenDevice</code> instance, then its
     * behavior is well defined and immutable.</p>
     *
     * <p>If an <code>HScreen</code> instance, <code><i>S</i></code>,
     * does not implement the
     * <code>MultiScreenConfigurableContext</code> interface (e.g.,
     * because it was not configurable when created and the MSM
     * implementation selectively implements this interface on specific
     * instances of <code>HScreen</code>) and if the new underlying
     * screen resources are configurable (and thus this interface would
     * be implemented on an <code>HScreen</code> instance that
     * represents this set of underlying screen resources), then the
     * MSM implementation SHALL NOT reuse <code><i>S</i></code> to
     * represent a new set of underlying screen resources upon a
     * multiscreen configuration change, but SHALL instead reset
     * <code><i>S</i></code> to the empty state.</p>
     * 
     * @return A non-empty array of <code>HScreen</code> instances as
     * described above.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     * @see MultiScreenConfigurableContext
     * @see org.ocap.ui.event.MultiScreenConfigurationEvent
     * @see org.ocap.ui.event.MultiScreenConfigurationListener
     * @see org.ocap.ui.event.MultiScreenContextEvent
     * @see org.ocap.ui.event.MultiScreenContextListener
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenDevice
     * @see org.havi.ui.HBackgroundDevice
     * @see org.havi.ui.HVideoDevice
     * @see org.havi.ui.HGraphicsDevice
     * @see org.ocap.system.MonitorAppPermission
     **/
    public HScreen[] getScreens()
    {
        return null;
    }

    /**
     * Obtain the default <code>HScreen</code> instance.
     *
     * <p>The <code>HScreen</code> instance returned by this method
     * SHALL represent the currently active set of default, underlying
     * screen devices and their currently active HAVi screen
     * configurations. In addition, it MAY represent a set of currently
     * active non-default, underlying screen devices and their
     * configurations.</p>
     *
     * <p>The returned default <code>HScreen</code> instance is
     * intended to be the <b>default</b> from the
     * perspective of the calling application or the application on
     * whose behalf this method is invoked. If invoked by the platform
     * implementation in a context that does not imply a specific
     * application, then the returned value is not defined and SHALL be
     * implementation dependent, including, e.g., returning
     * <code>null</code>.</p>
     *
     * <p>Over the course of an application's lifecycle, the reference
     * returned from this method SHALL remain constant. Furthermore,
     * the set of default <code>HScreenDevice</code> instances returned
     * by the <code>getDefaultHBackgroundDevice()</code>,
     * <code>getDefaultHVideoDevice()</code>, and
     * <code>getDefaultHGraphicsDevice()</code> methods of this default
     * <code>HScreen</code> instance SHALL similarly remain constant
     * over the application's lifecycle. Notwithstanding this
     * constancy of reference, the underlying device resources and the
     * underlying configurations of these device resources MAY change
     * over the course of an application's lifecycle.</p>
     *
     * <p>Any non-default background, video, or graphics screen device,
     * i.e., <code>HBackgroundDevice</code>, <code>HVideoDevice</code>,
     * or <code>HGraphicsDevice</code>, of any <code>HScreen</code>
     * returned by this method SHALL NOT be equivalent to the <i>empty
     * <code>HScreenDevice</code></i> of its specific sub-type during
     * the interval between the time when this method returns and the
     * time when a
     * <code>MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING</code> or
     * <code>MultiScreenContextEvent.MULTI_SCREEN_CONTEXT_SERVICE_CONTEXT_CHANGED</code>
     * event is generated.</p>
     *
     * <p>If any <code>HScreenConfiguration</code> (or derived screen
     * device type specific) parameter of any of the default
     * <code>HScreenDevice</code> instances returned by the above cited
     * methods changes over the course of an application's lifecycle,
     * then an appropriate <code>HScreenConfigurationEvent</code> SHALL
     * be generated and dispatched to all registered
     * <code>HScreenConfigurationListener</code> instances. Similarly,
     * if any value that would be returned by any of the query
     * (<i>get*</i>) methods of the <code>MultiScreenContext</code>
     * interface implemented by this default <code>HScreen</code>
     * instance changes, then an appropriate
     * <code>MultiScreenContextEvent</code> SHALL be generated and
     * dispatched to all registered
     * <code>MultiScreenContextListener</code> instances.</p>
     *
     * <p>If any <code>HScreen</code> instance returned by this method
     * implements the <code>MultiScreenConfigurableContext</code>
     * interface, then every <code>HScreen</code> instance returned by
     * this method SHALL implement the <code>MultiScreenConfigurableContext</code>
     * interface irrespective of whether the underlying screen
     * resources represented by any returned <code>HScreen</code>
     * instance is configurable or not.</p>
     *
     * @return An <code>HScreen</code> instance as described above.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     * @see MultiScreenContext
     * @see MultiScreenConfigurableContext
     * @see org.ocap.ui.event.MultiScreenContextEvent
     * @see org.ocap.ui.event.MultiScreenContextListener
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenDevice
     * @see org.havi.ui.HBackgroundDevice
     * @see org.havi.ui.HVideoDevice
     * @see org.havi.ui.HGraphicsDevice
     * @see org.havi.ui.HScreenConfiguration
     * @see org.havi.ui.event.HScreenConfigurationEvent
     **/
    public HScreen getDefaultScreen()
    {
        return null;
    }

    /**
     * Find accessible screen(s) associated with specific service
     * context. A given service context may be associated with zero,
     * one, or multiple <code>HScreen</code> instances.
     *
     * <p>Find the set of accessible <code>HScreen</code> instances with
     * which a specified <code>ServiceContext</code> is associated. An
     * <code>HScreen</code> instance is accessible (by some application)
     * if the <code>getScreens()</code> method returns (or would return)
     * that instance (when invoked by the same application at the time
     * this method is invoked).</p>
     *
     * @param context a <code>ServiceContext</code> instance.
     *
     * @return An array of <code>HScreen</code> instances or
     * <code>null</code>. If the specified <code>ServiceContext</code> is
     * associated with some accessible <code>HScreen</code>, then that
     * <code>HScreen</code> SHALL be present in the returned array which
     * SHALL be non-null and non-empty; otherwise, <code>null</code>
     * is returned, indicating that the <code>ServiceContext</code> is
     * not associated with any <code>HScreen</code>.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see javax.tv.service.selection.ServiceContext
     **/
    public HScreen[] findScreens ( ServiceContext context )
    {
        return null;
    }

    /**
     * Obtain the screen associated with an output port.
     *
     * <p>Given a specific <code>VideoOutputPort</code> instance,
     * obtain the <code>HScreen</code> instance that is
     * associated with that port for the purpose of presentation.</p>
     *
     * @param port a <code>VideoOutputPort</code> instance.
     *
     * @return The <code>HScreen</code> instance associated with the
     * specified port or <code>null</code> if no association exists.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.ocap.hardware.VideoOutputPort
     **/
    public HScreen getOutputPortScreen ( VideoOutputPort port )
    {
        return null;
    }

    /**
     * Obtain the set of accessible screens that are compatible with an
     * output port.
     *
     * <p>Given a specific <code>VideoOutputPort</code> instance, obtain
     * the subset of <code>HScreen</code> instances (of the
     * larger set returned by <code>getScreens()</code>) that may be
     * associated with that port for the purpose of presentation.</p>
     *
     * @param port a <code>VideoOutputPort</code> instance.
     *
     * @return A (possibly empty) array of <code>HScreen</code>
     * instances that may be associated with the specified port.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.ocap.hardware.VideoOutputPort
     **/
    public HScreen[] getCompatibleScreens ( VideoOutputPort port )
    {
        return null;
    }

    /**
     * Obtain the set of all current multiscreen configurations supported by
     * this platform, irrespective of their configuration type.
     *
     * <p>The set of multiscreen configuration instances returned by
     * this method SHALL include all per-platform multiscreen
     * configurations (composed solely of display screens) and all
     * per-display multiscreen configurations (composed of no more than one
     * display screen and any number of logical screens).</p>
     *
     * <p>The order of multiscreen configurations returned by this
     * method is not defined by this specification.</p>
     *
     * @return An array of <code>MultiScreenConfiguration</code>
     * instances.
     * 
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     * @see org.ocap.system.MonitorAppPermission
     **/
    public MultiScreenConfiguration[] getMultiScreenConfigurations()
        throws SecurityException
    {
        return null;
    }

    /**
     * Obtain multiscreen configurations of a specific configuration type.
     *
     * @param screenConfigurationType one of the following values: (1)
     * an element of the following enumeration of constants defined by
     * <code>MultiScreenConfiguration</code>: {
     * <code>SCREEN_CONFIGURATION_DISPLAY</code>,
     * <code>SCREEN_CONFIGURATION_NON_PIP</code>,
     * <code>SCREEN_CONFIGURATION_PIP</code>,
     * <code>SCREEN_CONFIGURATION_POP</code>,
     * <code>SCREEN_CONFIGURATION_GENERAL</code> }, or (2) some other
     * platform-dependent value not pre-defined as a multiscreen
     * configuration type.
     *
     * <p>The set of multiscreen configuration instances returned by
     * this method SHALL include all multiscreen configurations of the
     * specified type that appear in the array returned by
     * <code>getMultiScreenConfigurations()</code>.</p>
     *
     * <p>The order of multiscreen configurations returned by this
     * method is not defined by this specification.</p>
     *
     * @return An array of <code>MultiScreenConfiguration</code>
     * instances or <code>null</code>, depending on whether the specified
     * configuration type is supported or not.
     * 
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     * @see org.ocap.system.MonitorAppPermission
     **/
    public MultiScreenConfiguration[] getMultiScreenConfigurations ( String screenConfigurationType )
        throws SecurityException
    {
        return null;
    }

    /**
     * Obtain the multiscreen configuration of a specific screen.
     *
     * <p>A given <code>HScreen</code> instance SHALL be associated with
     * either zero or exactly one multiscreen configuration instance;
     * however, since a single underlying screen MAY be potentially
     * shared (multiply referenced) by multiple <code>HScreen</code>
     * instances, an underlying screen (and its constituent resources)
     * MAY be associated with more than one multiscreen configuration.</p>
     *
     * @param screen an <code>HScreen</code> instance
     *
     * @return The <code>MultiScreenConfiguration</code> instance of
     * which the specified <code>HScreen</code> is a constituent
     * screen or <code>null</code> if the specified
     * <code>HScreen</code> is orphaned (i.e., not owned by a
     * multiscreen configuration, e.g., as would be the case if it were
     * an empty screen).
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     **/
    public MultiScreenConfiguration getMultiScreenConfiguration ( HScreen screen )
    {
        return null;
    }

    /**
     * Obtain currently active per-platform display multiscreen configuration.
     *
     * @return The currently active per-platform display <code>MultiScreenConfiguration</code>
     * instance.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfiguration
     **/
    public MultiScreenConfiguration getMultiScreenConfiguration()
    {
        return null;
    }

    /**
     * Set currently active per-platform display multiscreen configuration.
     *
     * <p>If the specified <code><i>configuration</i></code> is the
     * current per-platform display multiscreen configuration, then,
     * unless <code>SecurityException</code> applies, return from this
     * method without producing any side effect.</p>
     *
     * <p>If the specified <code><i>configuration</i></code> is not the
     * current per-platform display multiscreen configuration and if
     * <code>SecurityException</code>,
     * <code>IllegalArgumentException</code>, and
     * <code>IllegalStateException</code> do not apply, then perform the
     * synchronous per-platform multiscreen configuration change
     * processing defined by the <i>OCAP Multiscreen Manager (MSM)
     * Extension</i> specification.</p>
     *
     * <p>If the <code><i>serviceContextAssociations</i></code> argument
     * is specified (i.e., not <code>null</code>), then any
     * <code>ServiceContext</code> instance that is accessible to the
     * invoking application SHALL be associated with either no screen or
     * the applicable screen(s) in the specified (new) per-platform
     * multiscreen configuration (or in its per-display multiscreen
     * configurations). If no association matches some accessible
     * <code>ServiceContext</code>, if some accessible
     * <code>ServiceContext</code> instance is not present in the
     * specified associations, or if it is present but no such
     * applicable screen exists in the new per-platform multiscreen
     * configuration (or in its per-display multiscreen configurations),
     * then the <code>ServiceContext</code> instance SHALL be associated
     * with the default service context association screen of the
     * specified multiscreen configuration, i.e., the screen returned by
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
     * instance to become the currently active per-platform display
     * multiscreen configuration.
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
     * <code><i>configuration</i></code> is not a per-platform multiscreen
     * configuration that would be returned by
     * <code>MultiScreenManager.getMultiScreenConfigurations(SCREEN_CONFIGURATION_DISPLAY)</code>.
     *
     * @throws IllegalStateException if the MSM implementation (1) does
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
        throws SecurityException, IllegalStateException, IllegalStateException
    {
    }

    /**
     * Request change to the currently active per-platform display
     * multiscreen configuration.
     *
     * <p>If the specified <code><i>configuration</i></code> is the current
     * configuration, then, unless <code>SecurityException</code>
     * applies, return from this method without producing any side
     * effect.</p>
     *
     * <p>If the specified <code><i>configuration</i></code> is not the
     * current per-platform display multiscreen configuration and if
     * <code>SecurityException</code>,
     * <code>IllegalArgumentException</code>, and
     * <code>IllegalStateException</code> do not apply, then initiate an
     * asynchronous change to the current per-platform multiscreen configuration,
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
     * @throws IllegalStateException (1) if the MSM implementation does
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
        throws SecurityException, IllegalStateException
    {
    }

    /**
     * Add a listener to be notified upon the occurence of multiscreen
     * configuration events. If a listener has previously been added and
     * not subsequently removed, then an attempt to add it again SHALL
     * NOT produce a side effect.
     *
     * <p>Configuration events that apply to this
     * <code>MultiScreenManager</code> singleton instance SHALL be
     * restricted to those that affect the complement of usable display
     * screens.</p>
     * 
     * <p>If an event defined by
     * <code>MultiScreenConfigurationEvent</code> is generated, then the
     * MSM implementation SHALL notify each registered screen
     * configuration listener accordingly.</p>
     *
     * @param listener a <code>MultiScreenConfigurationListener</code>
     * instance.
     *
     * @since MSM I01
     *
     * @see org.ocap.ui.event.MultiScreenConfigurationEvent
     * @see org.ocap.ui.event.MultiScreenConfigurationListener
     **/
    public void addMultiScreenConfigurationListener ( MultiScreenConfigurationListener listener )
    {
    }

    /**
     * Remove a listener previously added to be notified upon the
     * occurence of multiscreen configuration events. If the specified
     * listener is not currently registered as a listener, then an
     * attempt to remove it SHALL NOT produce a side effect.
     *
     * @param listener a <code>MultiScreenConfigurationListener</code>
     * instance.
     *
     * @since MSM I01
     *
     * @see org.ocap.ui.event.MultiScreenConfigurationListener
     **/
    public void removeMultiScreenConfigurationListener ( MultiScreenConfigurationListener listener )
    {
    }

    /**
     * Add resource status listener.
     * 
     * @param listener a <code>ResourceStatusListener</code> instance.
     *
     * @since MSM I01
     **/
    public void addResourceStatusListener ( ResourceStatusListener listener )
    {
    }

    /**
     * Remove resource status listener.
     * 
     * @param listener a <code>ResourceStatusListener</code> instance.
     *
     * @since MSM I01
     **/
    public void removeResourceStatusListener ( ResourceStatusListener listener )
    {
    }

    /**
     * Atomically swap service contexts between two <code>HScreen</code>
     * instances.
     *
     * <p>This method is a convenience method for supporting the common
     * function of swapping content presentation between screens.
     * Similar results obtained by this method MAY also be accomplished
     * by the more general mechanism of removing and adding service
     * contexts to specific accessible screens by using the
     * <code>MultiScreenConfigurableContext.addServiceContext(..)</code> and
     * <code>MultiScreenConfigurableContext.removeServiceContext(..)</code>
     * methods. Nevertheless, use of the more general method MAY result
     * in more presentation transition artifacts than use of this method
     * due to the atomic swap semantics of this method.</p>
     *
     * <p>The state of the decoder format conversion (DFC) component of
     * a video pipeline being used to process video associated with a
     * service context that is being swapped by this method SHALL NOT be
     * affected by performance of this method.</p>
     * 
     * @param screen1 an <code>HScreen</code> instance
     * whose service contexts are to be swapped with
     * those of <em>screen2</em>.
     *
     * @param screen2 an <code>HScreen</code> instance
     * whose service contexts are to be swapped with
     * those of <em>screen1</em>.
     *
     * @param exclusions if not <code>null</code>, then a non-empty
     * array of <code>ServiceContext</code> instances to be excluded
     * from the swap operation, i.e., whose screen associations are not
     * to be affected by the swap.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @throws IllegalStateException if any of the following hold: (1)
     * video is being presented as component video rather than
     * background video in either screen, or (2) the
     * <code>ServiceContext</code>s for the specified screens cannot be
     * changed, e.g., if the platform uses a permanent association with
     * a specific <code>ServiceContext</code> and a screen.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see javax.tv.service.selection.ServiceContext
     * @see org.ocap.system.MonitorAppPermission
     **/
    public void swapServiceContexts ( HScreen screen1, HScreen screen2, ServiceContext[] exclusions )
        throws SecurityException, IllegalStateException
    {
    }

    /**
     * Atomically move a set of specific service context from one
     * <code>HScreen</code> instance to another <code>HScreen</code>
     * instance.
     *
     * <p>This method is a convenience method for supporting the common
     * function of moving content presentations between screens for a
     * set of given service contexts.  Similar results obtained by this
     * method MAY also be accomplished by the more general mechanism of
     * removing and adding the service context to specific accessible
     * screens by using the
     * <code>MultiScreenConfigurableContext.addServiceContexts(..)</code>
     * and
     * <code>MultiScreenConfigurableContext.removeServiceContexts(..)</code>
     * methods. Nevertheless, use of the more general method MAY result
     * in more presentation transition artifacts than use of this method
     * due to the atomic move semantics of this method.</p>
     *
     * <p>The state of the decoder format conversion (DFC) component of
     * a video pipeline being used to process video associated with a
     * service context that is being moved by this method SHALL NOT be
     * affected by performance of this method.</p>
     * 
     * @param src an <code>HScreen</code> instance
     * from which the specified service contexts are to be moved.
     *
     * @param dst an <code>HScreen</code> instance
     * to which the specified service contexts are to be moved.
     *
     * @param contexts a non-empty array of <code>ServiceContext</code>
     * instances to be moved from <em>src</em> screen to <em>dst</em> screen.
     *
     * @throws SecurityException if the calling thread has
     * not been granted <code>MonitorAppPermission("multiscreen.configuration")</code>.
     *
     * @throws IllegalArgumentException if some specified
     * <code>ServiceContext</code> is not currently
     * associated with the source <code>HScreen</code> instance,
     * <em>src</em>.
     * 
     * @throws IllegalStateException if any of the following hold: (1)
     * video is being presented as component video rather than
     * background video in either screen; (2) some specified
     * <code>ServiceContext</code> for the specified screens cannot be
     * moved, e.g., if the platform uses a permanent association with
     * a specific <code>ServiceContext</code> and a screen; or (3) a
     * non-abstract <code>ServiceContext</code> is already associated with
     * with the destination screen and the platform supports only one
     * non-abstract <code>ServiceContext</code> per screen.
     *
     * @since MSM I01
     *
     * @see MultiScreenConfigurableContext
     * @see org.havi.ui.HScreen
     * @see javax.tv.service.selection.ServiceContext
     * @see org.ocap.system.MonitorAppPermission
     **/
    public void moveServiceContexts ( HScreen src, HScreen dst, ServiceContext[] contexts )
        throws SecurityException, IllegalArgumentException, IllegalStateException
    {
    }

    /**
     * Obtain the set of screen devices currently assigned for
     * use by a (JMF) media player.
     *
     * @param player a JMF <code>Player</code>
     * instance to query for its set of screen devices.
     *
     * @return An array of <code>HScreenDevice</code> instances, which
     * SHALL be empty if and only if there is no associated screen device.
     *
     * @since MSM I01
     *
     * @see javax.media.Player
     * @see org.havi.ui.HScreenDevice
     **/
    public HScreenDevice[] getPlayerScreenDevices ( Player player )
    {
        return new HScreenDevice[0];
    }

    /**
     * Add screen device(s) to a media player.
     *
     * @param player a JMF <code>Player</code>
     * instance with which to associate the specified screen
     * device(s).
     *
     * @param devices a non-empty array of <code>HScreenDevice</code>
     * instances on which to present some type of rendered content from the
     * specified media player.
     *
     * @throws SecurityException if the calling thread has
     * not been granted
     * <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalStateException if any of the following hold: (1)
     * the specified <em>player</em> is not in a stopped state; (2) the
     * specified screen <em>device</em> is not compatible with the
     * specified <em>player</em>; (3) some underlying screen device of
     * <em>devices</em> is not available for use by this application,
     * e.g., due to being exclusively reserved by another application;
     * or (4) some underlying screen device of <em>devices</em> is
     * already associated with a media player, and that device does not
     * support association with multiple media players.
     *
     * @since MSM I01
     *
     * @see javax.media.Player
     * @see org.havi.ui.HScreenDevice
     **/
    public void addPlayerScreenDevices ( Player player, HScreenDevice[] devices )
        throws SecurityException, IllegalStateException
    {
    }

    /**
     * Remove screen device(s) from a media player.
     *
     * <p>Removes all or a non-empty set of <code>HScreenDevice</code>
     * instances from the set of screen devices on which the specified
     * media player is presented (or otherwise associated for
     * presentation). If <code><i>devices</i></code> is <code>null</code>, then all
     * screen device associations are removed.</p>
     *
     * @param player a JMF <code>Player</code> instance from which to
     * remove association with the specified screen device.
     *
     * @param devices either <code>null</code> or a non-empty set of
     * <code>HScreenDevice</code> instances to be disassociated from the
     * specified media player.
     *
     * @throws SecurityException if the calling thread has
     * not been granted
     * <code>MonitorAppPermission("multiscreen.context")</code>.
     *
     * @throws IllegalArgumentException if <code><i>devices</i></code> is not
     * <code>null</code> and some entry of <code><i>devices</i></code> is not
     * associated with the specified <code>Player</code> instance.
     *
     * @throws IllegalStateException if the specified <em>player</em> is
     * not in a stopped state.
     *
     * @since MSM I01
     *
     * @see javax.media.Player
     * @see org.havi.ui.HScreenDevice
     **/
    public void removePlayerScreenDevices ( Player player, HScreenDevice[] devices )
        throws SecurityException, IllegalArgumentException, IllegalStateException
    {
    }

    /**
     * Obtain the singleton <i>empty <code>HScreen</code></i> instance.
     *
     * <p>Using this <i>empty <code>HScreen</code></i>, it is possible
     * to obtain a reference to the <i>empty
     * <code>HScreenDevice</code></code></i>, <i>empty
     * <code>HScreenConfiguration</code></code></i>, and <i>empty
     * <code>HScreenConfigTemplate</code></code></i> of each available
     * sub-type, e.g., <code>HBackgroundDevice</code>,
     * <code>HVideoDevice</code>, <code>HGraphicsDevice</code>, etc.</p>
     *
     * <p><em>Note:</em> The presence of this method is primarily aimed
     * at supporting the testing of MSM functionality, such as the
     * semantics of <code>isEmptyScreen()</code>,
     * <code>isEmptyScreenDevice()</code>,
     * <code>sameResources()</code>, etc.</p>
     *
     * @return the <i>empty <code>HScreen</code></i> singleton
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenDevice
     * @see org.havi.ui.HBackgroundDevice
     * @see org.havi.ui.HVideoDevice
     * @see org.havi.ui.HGraphicsDevice
     * @see org.havi.ui.HScreenConfiguration
     * @see org.havi.ui.HScreenConfigTemplate
     **/
    public HScreen getEmptyScreen()
    {
        return null;
    }

    /**
     * Determines if an instance of <code>HScreen</code> is equivalent,
     * in terms of constraint satisfaction, to the <i>empty
     * <code>HScreen</code></i>.
     *
     * <p>If <code><i>screen</i></code> does not implement
     * <code>MultiScreenConfigurableContext</code>, then those
     * constraints that pertain to
     * <code>MultiScreenConfigurableContext</code> SHALL be deemed to be
     * equivalent.</p>
     *
     * @param screen an <code>HScreen</code> instance.
     *
     * @return true if the specified <code><i>screen</i></code> is equivalent to the
     * <i>empty <code>HScreen</code></i>.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     **/
    public boolean isEmptyScreen ( HScreen screen )
    {
        return false;
    }

    /**
     * Determines if an instance of <code>HScreenDevice</code> is equivalent,
     * in terms of constraint satisfaction, to the <i>empty
     * <code>HScreenDevice</code></i> of the specific sub-type.
     *
     * @param device an <code>HScreenDevice</code> instance of one of
     * the following sub-types:  <code>HBackgroundDevice</code>,
     * <code>HVideoDevice</code>, or <code>HGraphicsDevice</code>.
     *
     * @return true if the specified <code><i>device</i></code> is equivalent to the
     * <i>empty <code>HScreenDevice</code></i> of the matching sub-type.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreenDevice
     **/
    public boolean isEmptyScreenDevice ( HScreenDevice device )
    {
        return false;
    }

    /**
     * Determines if two <code>HScreen</code> instances represent the
     * same underlying platform resources and underlying resource
     * state, i.e., are equivalent with respect to these underlying
     * resources.
     *
     * <p>For the purpose of determining equivalence, the following
     * conditions SHALL apply:</p>
     *
     * <ul>
     * <li>if exactly one of <code><i>screen1</i></code> and
     * <code><i>screen2</i></code> is equivalent to the <i>empty
     * <code>HScreen</code></i>, then the two screens are not equivalent
     * with respect to underlying resources;</li>
     * <li>if, for each screen device, <code><i>BD1</i></code>, returned
     * by <code><i>screen1</i>.getHBackgroundDevices()</code> there is
     * not exactly one screen device, <code><i>BD2</i></code>, returned
     * by <code><i>screen2</i>.getHBackgroundDevices()</code> such that
     * <code>sameResources(<i>BD1</i>,<i>BD2</i>)</code> returns
     * <code>true</code>, then the two screens are not equivalent with
     * respect to underlying resources;</li>
     * <li>if, for each screen device, <code><i>VD1</i></code>, returned
     * by <code><i>screen1</i>.getHVideoDevices()</code> there is
     * not exactly one screen device, <code><i>VD2</i></code>, returned
     * by <code><i>screen2</i>.getHVideoDevices()</code> such that
     * <code>sameResources(<i>VD1</i>,<i>VD2</i>)</code> returns
     * <code>true</code>, then the two screens are not equivalent with
     * respect to underlying resources;</li>
     * <li>if, for each screen device, <code><i>GD1</i></code>, returned
     * by <code><i>screen1</i>.getHGraphicsDevices()</code> there is
     * not exactly one screen device, <code><i>GD2</i></code>, returned
     * by <code><i>screen2</i>.getHGraphicsDevices()</code> such that
     * <code>sameResources(<i>GD1</i>,<i>GD2</i>)</code> returns
     * <code>true</code>, then the two screens are not equivalent with
     * respect to underlying resources;</li>
     * <li>if, given an equivalent set of template arguments,
     * <code><i>screen1</i>.getBestConfiguration(..)</code> and
     * <code><i>screen2</i>.getBestConfiguration(..)</code> would not
     * return (nominally unordered) sets of equivalent <code>HScreenConfiguration</code>
     * instances, then the two screens are not equivalent with respect
     * to underlying resources;</li>
     * <li>if, given an eqivalent set of screen configuration arguments,
     * <code><i>screen1</i>.getCoherentScreenConfigurations(..)</code> and
     * <code><i>screen2</i>.getCoherentScreenConfigurations(..)</code> would not
     * return sets of equivalent <code>HScreenConfiguration</code>
     * instances, then the two screens are not equivalent with respect
     * to underlying resources;</li>
     * <li>if, given an eqivalent set of screen configuration arguments,
     * <code><i>screen1</i>.setCoherentScreenConfigurations(..)</code> and
     * <code><i>screen2</i>.setCoherentScreenConfigurations(..)</code> would not
     * return the same value or would not modify the set of screen
     * devices associated with the specified screen configuration
     * arguments such that those screen devices remain equivalent,
     * then the two screens are not equivalent with respect
     * to underlying resources;</li>
     * <li>if none the above conditions apply, then
     * <code><i>screen1</i></code> and <code><i>screen2</i></code> are
     * deemed equivalent with respect to underlying resources;</li>
     * </ul>
     * 
     * @param screen1 an <code>HScreen</code> instance.
     *
     * @param screen2 an <code>HScreen</code> instance.
     *
     * @return true if the specified screens are equivalent as
     * described by the above conditions.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenDevice
     * @see org.havi.ui.HScreenConfiguration
     **/
    public boolean sameResources ( HScreen screen1, HScreen screen2 )
    {
        return false;
    }

    /**
     * Determines if two <code>HScreenDevice</code> instances represent the
     * same underlying platform resources and underlying resource
     * state, i.e., are equivalent with respect to these underlying
     * resources.
     *
     * <p>For the purpose of determining equivalence, the following
     * conditions SHALL apply:</p>
     *
     * <ul>

     * <li>if <code><i>device1</i></code> and
     * <code><i>device2</i></code> are not of the same sub-type,
     * <code>HBackgroundDevice</code>, <code>HVideoDevice</code>, or
     * <code>HGraphicsDevice</code>, then the two screen devices are not
     * equivalent with respect to underlying resources;</li>
     * 
     * <li>if exactly one of <code><i>device1</i></code> and
     * <code><i>device2</i></code> is equivalent to the <i>empty
     * <code>HScreenDevice</code></i> of the appropriate sub-type, then
     * the two screen devices are not equivalent with respect to
     * underlying resources;</li>

     * <li>if <code><i>device1</i>.getFlickerFilter()</code> and
     * <code><i>device2</i>.getFlickerFilter()</code> would not return the
     * same value, then the two screen devices are not equivalent with
     * respect to underlying resources;</li>
     * 
     * <li>if <code><i>device1</i>.getInterlaced()</code> and
     * <code><i>device2</i>.getInterlaced()</code> would not return the
     * same value, then the two screen devices are not equivalent with
     * respect to underlying resources;</li>
     * 
     * <li>if <code><i>device1</i>.getPixelAspectRatio()</code> and
     * <code><i>device2</i>.getPixelAspectRatio()</code> would not return
     * equivalent values, then the two screen devices are not equivalent with
     * respect to underlying resources;</li>
     * 
     * <li>if <code><i>device1</i>.getPixelResolution()</code> and
     * <code><i>device2</i>.getPixelResolution()</code> would not return
     * equivalent values, then the two screen devices are not equivalent with
     * respect to underlying resources;</li>
     * 
     * <li>if <code><i>device1</i>.getScreenArea()</code> and
     * <code><i>device2</i>.getScreenArea()</code> would not return
     * equivalent values, then the two screen devices are not equivalent with
     * respect to underlying resources;</li>
     * 
     * <li>if given equivalent <code>HScreenConfiguration</code>
     * instances, <code><i>SC1</i></code> and <code><i>SC2</i></code>,
     * as arguments, <code><i>device1</i>.getOffset(<i>SC1</i>)</code> and
     * <code><i>device2</i>.getOffset(<i>SC2</i>)</code> would not return
     * equivalent values, then the two screen devices are not equivalent with
     * respect to underlying resources;</li>
     * 
     * <li>if given equivalent <code>HScreenConfiguration</code>
     * instances, <code><i>SC1</i></code> and <code><i>SC2</i></code>,
     * and equivalent <code>Point</code> instances,
     * <code><i>P1</i></code> and <code><i>P2</i></code>,
     * as arguments, <code><i>device1</i>.convertTo(<i>SC1</i>,<i>P1</i>)</code> and
     * <code><i>device2</i>.convertTo(<i>SC2</i>,<i>P2</i>)</code> would not return
     * equivalent values, then the two screen devices are not equivalent with
     * respect to underlying resources;</li>
     * 
     * <li>if <code><i>device1</i>.getConfigurations()</code> and
     * <code><i>device2</i>.getConfigurations()</code> would not return
     * (nominally unordered) sets of equivalent
     * <code>HScreenConfiguration</code> instances, then the two screens
     * are not equivalent with respect to underlying resources;</li>
     *
     * <li>if <code><i>device1</i>.getCurrentConfiguration()</code> and
     * <code><i>device2</i>.getCurrentConfiguration()</code> would not return
     * equivalent <code>HScreenConfiguration</code> instances,
     * then the two screens are not equivalent with respect to
     * underlying resources;</li>
     *
     * <li>if <code><i>device1</i>.getDefaultConfiguration()</code> and
     * <code><i>device2</i>.getDefaultConfiguration()</code> would not return
     * equivalent <code>HScreenConfiguration</code> instances,
     * then the two screens are not equivalent with respect to
     * underlying resources;</li>
     *
     * <li>if, given an equivalent template arguments or sets of
     * template arguments,
     * <code><i>device1</i>.getBestConfiguration(..)</code> and
     * <code><i>device2</i>.getBestConfiguration(..)</code> would not
     * return sets of equivalent <code>HScreenConfiguration</code>
     * instances, then the two screens are not equivalent with respect
     * to underlying resources;</li>
     *
     * <li>if <code><i>device1</i></code> and
     * <code><i>device2</i></code> are instances of
     * <code>HBackgroundDevice</code> and if
     * <code><i>device1</i>.setBackgroundConfiguration(..)</code> and
     * <code><i>device2</i>.setBackgroundConfiguration(..)</code> would not
     * return the same value when equivalent
     * <code>HBackgroundConfiguration</code> instances are specified as
     * arguments, then the two screens are not equivalent with respect
     * to underlying resources;</li>
     *
     * <li>if <code><i>device1</i></code> and
     * <code><i>device2</i></code> are instances of
     * <code>HVideoDevice</code> and if (1)
     * <code><i>device1</i>.setVideoConfiguration(..)</code> and
     * <code><i>device2</i>.setVideoConfiguration(..)</code> would not
     * return the same value when equivalent
     * <code>HVideoConfiguration</code> instances are specified as
     * arguments, (2) <code><i>device1</i>.getVideoSource()</code> and
     * <code><i>device1</i>.getVideoSource()</code> would not return
     * nominally equivalent video sources, or (3)
     * <code><i>device1</i>.getVideoController()</code> and
     * <code><i>device1</i>.getVideoController()</code> would not return
     * nominally equivalent video controllers, then the two screens are
     * not equivalent with respect to underlying resources;</li>
     *
     * <li>if <code><i>device1</i></code> and
     * <code><i>device2</i></code> are instances of
     * <code>HGraphicsDevice</code> and if
     * <code><i>device1</i>.setGraphicsConfiguration(..)</code> and
     * <code><i>device2</i>.setGraphicsConfiguration(..)</code> would not
     * return the same value when equivalent
     * <code>HGraphicsConfiguration</code> instances are specified as
     * arguments, then the two screens are not equivalent with respect
     * to underlying resources;</li>
     *
     * <li>if none the above conditions apply, then
     * <code><i>device1</i></code> and <code><i>device2</i></code> are
     * deemed equivalent with respect to underlying resources;</li>
     * </ul>
     * 
     * @param device1 an <code>HScreenDevice</code> instance.
     *
     * @param device2 an <code>HScreenDevice</code> instance.
     *
     * @return true if the specified screens are equivalent as
     * described by the above conditions.
     *
     * @since MSM I01
     *
     * @see org.havi.ui.HScreen
     * @see org.havi.ui.HScreenDevice
     * @see org.havi.ui.HScreenConfiguration
     **/
    public boolean sameResources ( HScreenDevice device1, HScreenDevice device2 )
    {
        return false;
    }

    /**
     * Determines if two <code>ServiceContext</code> instances represent the
     * same underlying platform resources and underlying resource
     * state, i.e., are equivalent with respect to these underlying
     * resources.
     *
     * @param sc1 a <code>ServiceContext</code> instance.
     *
     * @param sc2 a <code>ServiceContext</code> instance.
     *
     * @return true if the specified service contexts are equivalent
     * (i.e., represent the same underlying resources and resource
     * state).
     *
     * @since MSM I01
     *
     * @see javax.tv.service.selection.ServiceContext
     **/
    public boolean sameResources ( ServiceContext sc1, ServiceContext sc2 )
    {
        return false;
    }

    /**
     * Gets the singleton instance of the <code>MultiScreenManager</code>.
     *
     * @since MSM I01
     *
     * @return The <code>MultiScreenManager</code> instance.
     **/
    public static MultiScreenManager getInstance()
    {
        return null;
    }

    /**
     * Reconfiguration Constraints
     *
     * Assumptions:
     * 
     * 1. platform implements MSM as defined;
     * 2. more than one distinct multiscreen configuration exists and is
     *    accessible;
     * 3. caller has MonitorAppPermission("multiscreen.configuration");
     * 4. no MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING or
     *    MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGED event
     *    is generated during the execution of this method other than as a direct
     *    result of the invocation of setMultiScreenConfiguration() by this method;
     * 5. no MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_SCREEN_ADDED
     *    or MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_SCREEN_REMOVED
     *    event is generated during the execution of this method;
     * 6. application signals allow_default_device_reconfig as '1' in multiple
     *    screen usage descriptor;
     * 7. the platform preserves key default screen device configuration parameters,
     *    specifically screen area, pixel resolution, and pixel aspect ratio across
     *    configuration change; i.e., it does not take advantage of fact that
     *    application has signaled allow_default_device_reconfig as '1' in order to
     *    reconfigure default screen device parameters (note that this verification
     *    process could be expanded to cover case where platform does not preserve
     *    these parameters, i.e., an approprate HScreenConfigurationEvent or
     *    MultiScreenContextEvent could be listened for and used to note that
     *    platform does not preserve these parameters);
     **/
    static void verifyConstraints()
    {
        MultiScreenManager          MSM                 = MultiScreenManager.getInstance();

        // PRECONDITIONS

        // verify invariants
        verifyInvariantConstraints();

        // record default screen
        HScreen                     S1                  = MSM.getDefaultScreen();

        // record default background screen device and key configuration parameters
        HBackgroundDevice           B1                  = S1.getDefaultHBackgroundDevice();
        HScreenConfiguration        BC1                 = null;
        HScreenRectangle            B1SR                = null;
        Dimension                   B1PR                = null;
        Dimension                   B1PAR               = null;
        if ( B1 != null ) {
            BC1     = B1.getCurrentConfiguration();
            B1SR    = BC1.getScreenArea();
            B1PR    = BC1.getPixelResolution();
            B1PAR   = BC1.getPixelAspectRatio();
        }

        // record default video screen device and key configuration parameters
        HVideoDevice                V1                  = S1.getDefaultHVideoDevice();
        HScreenConfiguration        VC1                 = null;
        HScreenRectangle            V1SR                = null;
        Dimension                   V1PR                = null;
        Dimension                   V1PAR               = null;
        if ( V1 != null ) {
            VC1     = V1.getCurrentConfiguration();
            V1SR    = VC1.getScreenArea();
            V1PR    = VC1.getPixelResolution();
            V1PAR   = VC1.getPixelAspectRatio();
        }

        // record default graphics screen device and key configuration parameters
        HGraphicsDevice             G1                  = S1.getDefaultHGraphicsDevice();
        HScreenConfiguration        GC1                 = null;
        HScreenRectangle            G1SR                = null;
        Dimension                   G1PR                = null;
        Dimension                   G1PAR               = null;
        if ( G1 != null ) {
            GC1     = G1.getCurrentConfiguration();
            G1SR    = GC1.getScreenArea();
            G1PR    = GC1.getPixelResolution();
            G1PAR   = GC1.getPixelAspectRatio();
        }

        // record non-default screens
        HScreen[]                   SX1ND               = getNonDefaultScreens();

        // record non-default screen devices
        HScreenDevice[]             DX1ND               = getNonDefaultScreenDevices();
        
        // find different configuration from current configuration
        MultiScreenConfiguration[]  MSCX1               = MSM.getMultiScreenConfigurations();
        MultiScreenConfiguration    MSC1                = MSM.getMultiScreenConfiguration();
        MultiScreenConfiguration    MSC2                = null;
        for ( int i = 0; ( MSC2 == null ) && ( i < MSCX1.length ); i++ ) {
            if ( MSCX1[i] != MSC1 )
                MSC2 = MSCX1[i];
        }
        ASSERT ( MSC2 != null );
        ASSERT ( MSC2 != MSC1 );

        // CHANGE CONFIGURATION
        try {
            MSM.setMultiScreenConfiguration ( MSC2, null );
            waitForMultiScreenConfigurationChangingEvent();
            waitForMultiScreenConfigurationChangedEvent();
        } catch ( SecurityException x ) {
            ASSERT ( false );
        }

        // POSTCONDITIONS

        // verify invariants
        verifyInvariantConstraints();

        // 1. the current multiscreen configuration is the new configuration
        ASSERT ( MSM.getMultiScreenConfiguration() == MSC2 );

        // 2. the new default screen must be same instance as the old default screen
        HScreen                     S2                  = MSM.getDefaultScreen();
        ASSERT ( S2 == S1 );

        // 3. if it exists, the new default background screen device
        // must be the same instance as the old default background
        // screen device if it exists unless the application signals
        // allow_default_device_reconfig as '1' in which case it is
        // permitted that no default background device is available
        // after reconfiguration, in which case the former default
        // background device must be reset to the empty background
        // screen device state
        HBackgroundDevice           B2                  = S2.getDefaultHBackgroundDevice();
        if ( B1 != null ) {
            if ( B2 == null )
                ASSERT ( MSM.isEmptyScreenDevice ( B1 ) );
            else
                ASSERT ( B2 == B1 );
        }

        // 4. if it exists, the new default background screen device
        // must have same screen area, pixel resolution, and pixel
        // aspect ratio as it did with previous default background
        // screen device
        if ( B1 != null ) {
            if ( B2 != null ) {
                HScreenConfiguration    BC2                 = B1.getCurrentConfiguration();
                HScreenRectangle        B2SR                = BC2.getScreenArea();
                ASSERT ( B1SR != null );
                ASSERT ( B2SR != null );
                ASSERT ( B2SR.x == B1SR.x );
                ASSERT ( B2SR.y == B1SR.y );
                ASSERT ( B2SR.width == B1SR.width );
                ASSERT ( B2SR.height == B1SR.height );
                Dimension               B2PR                = BC2.getPixelResolution();
                ASSERT ( B1PR != null );
                ASSERT ( B2PR != null );
                ASSERT ( B2PR.width == B1PR.width );
                ASSERT ( B2PR.height == B1PR.height );
                Dimension               B2PAR               = BC2.getPixelAspectRatio();
                ASSERT ( B1PAR != null );
                ASSERT ( B2PAR != null );
                ASSERT ( B2PAR.width == B1PAR.width );
                ASSERT ( B2PAR.height == B1PAR.height );
            }
        }

        // 5. if it exists, the new default video screen device
        // must be the same instance as the old default video
        // screen device if it exists unless the application signals
        // allow_default_device_reconfig as '1' in which case it is
        // permitted that no default video device is available
        // after reconfiguration, in which case the former default
        // video device must be reset to the empty video
        // screen device state
        HVideoDevice                V2                  = S2.getDefaultHVideoDevice();
        if ( V1 != null ) {
            if ( V2 == null )
                ASSERT ( MSM.isEmptyScreenDevice ( V1 ) );
            else
                ASSERT ( V2 == V1 );
        }

        // 6. if it exists, the new default video screen device
        // must have same screen area, pixel resolution, and pixel
        // aspect ratio as it did with previous default video
        // screen device
        if ( V1 != null ) {
            if ( V2 != null ) {
                HScreenConfiguration    VC2                 = V1.getCurrentConfiguration();
                HScreenRectangle        V2SR                = VC2.getScreenArea();
                ASSERT ( V1SR != null );
                ASSERT ( V2SR != null );
                ASSERT ( V2SR.x == V1SR.x );
                ASSERT ( V2SR.y == V1SR.y );
                ASSERT ( V2SR.width == V1SR.width );
                ASSERT ( V2SR.height == V1SR.height );
                Dimension               V2PR                = VC2.getPixelResolution();
                ASSERT ( V1PR != null );
                ASSERT ( V2PR != null );
                ASSERT ( V2PR.width == V1PR.width );
                ASSERT ( V2PR.height == V1PR.height );
                Dimension               V2PAR               = VC2.getPixelAspectRatio();
                ASSERT ( V1PAR != null );
                ASSERT ( V2PAR != null );
                ASSERT ( V2PAR.width == V1PAR.width );
                ASSERT ( V2PAR.height == V1PAR.height );
            }
        }

        // 7. if it exists, the new default graphics screen device
        // must be the same instance as the old default graphics
        // screen device if it exists unless the application signals
        // allow_default_device_reconfig as '1' in which case it is
        // permitted that no default graphics device is available
        // after reconfiguration, in which case the former default
        // graphics device must be reset to the empty graphics
        // screen device state
        HGraphicsDevice             G2                  = S2.getDefaultHGraphicsDevice();
        if ( G1 != null ) {
            if ( G2 == null )
                ASSERT ( MSM.isEmptyScreenDevice ( G1 ) );
            else
                ASSERT ( G2 == G1 );
        }

        // 8. if it exists, the new default graphics screen device
        // must have same screen area, pixel resolution, and pixel
        // aspect ratio as it did with previous default graphics
        // screen device
        if ( G1 != null ) {
            if ( G2 != null ) {
                HScreenConfiguration    GC2                 = G1.getCurrentConfiguration();
                HScreenRectangle        G2SR                = GC2.getScreenArea();
                ASSERT ( G1SR != null );
                ASSERT ( G2SR != null );
                ASSERT ( G2SR.x == G1SR.x );
                ASSERT ( G2SR.y == G1SR.y );
                ASSERT ( G2SR.width == G1SR.width );
                ASSERT ( G2SR.height == G1SR.height );
                Dimension               G2PR                = GC2.getPixelResolution();
                ASSERT ( G1PR != null );
                ASSERT ( G2PR != null );
                ASSERT ( G2PR.width == G1PR.width );
                ASSERT ( G2PR.height == G1PR.height );
                Dimension               G2PAR               = GC2.getPixelAspectRatio();
                ASSERT ( G1PAR != null );
                ASSERT ( G2PAR != null );
                ASSERT ( G2PAR.width == G1PAR.width );
                ASSERT ( G2PAR.height == G1PAR.height );
            }
        }

        // 9. for every non-default screen obtained prior to
        // reconfiguration, if no longer a non-default screen, then it
        // must be equivalent to an empty screen; otherwise, it must not
        // be equivalent to an empty screen
        HScreen[]                   SX2ND               = getNonDefaultScreens();
        for ( int i = 0, j; i < SX1ND.length; i++ ) {
            HScreen                 S                   = SX1ND[i];
            for ( j = 0; j < SX2ND.length; i++ ) {
                if ( S == SX2ND[j] )
                    break;
            }
            if ( j == SX2ND.length )
                ASSERT ( MSM.isEmptyScreen ( S ) );
            else
                ASSERT ( ! MSM.isEmptyScreen ( S ) );
        }

        // 10. for every non-default screen device obtained prior to
        // reconfiguration, if no longer a non-default screen device,
        // then it must be equivalent to an empty screen device;
        // otherwise, it must not be equivalent to an empty screen
        // device
        HScreenDevice[]             DX2ND               = getNonDefaultScreenDevices();
        for ( int i = 0, j; i < DX1ND.length; i++ ) {
            HScreenDevice           D                   = DX1ND[i];
            for ( j = 0; j < DX2ND.length; i++ ) {
                if ( D == DX2ND[j] )
                    break;
            }
            if ( j == DX2ND.length )
                ASSERT ( MSM.isEmptyScreenDevice ( D ) );
            else
                ASSERT ( ! MSM.isEmptyScreenDevice ( D ) );
        }
    }

    static private void verifyInvariantConstraints()
    {
        int found;
                
        // 1. there must be a multiscreen manager
        MultiScreenManager          MSM                 = MultiScreenManager.getInstance();
        ASSERT ( MSM != null );

        // 2. there must be a current multiscreen configuration
        MultiScreenConfiguration    MSC                 = MSM.getMultiScreenConfiguration();
        ASSERT ( MSC != null );

        // 3. there must be a non-empty set of accessible multiscreen
        // configurations
        MultiScreenConfiguration[]  MSCX                = MSM.getMultiScreenConfigurations();
        ASSERT ( MSCX != null );
        ASSERT ( MSCX.length > 0 );

        // 4. the current multiscreen configuration must be an
        // accessible configuration
        found = 0;
        for ( int i = 0; i < MSCX.length; i++ ) {
            ASSERT ( MSCX[i] != null );
            if ( MSCX[i] == MSC ) { // instance identity required
                found++;
            }
        }
        ASSERT ( found == 1 );

        // 5. there must be a non-empty set of screens in current
        // multiscreen configuration
        HScreen[]                   MSCSX               = MSC.getScreens();
        ASSERT ( MSCSX != null );
        ASSERT ( MSCSX.length > 0 );

        // 6. the screens in the current multiscreen configuration must
        // not be empty
        for ( int i = 0; i < MSCSX.length; i++ ) {
            ASSERT ( ! MSM.isEmptyScreen ( MSCSX[i] ) );
        }

        // 7. any two distinct screen entries in the current multiscreen
        // configuration must not represent the same resources
        for ( int i = 0; i < MSCSX.length; i++ ) {
            for ( int j = 0; j < MSCSX.length; j++ ) {
                if ( i != j ) {
                    ASSERT ( MSCSX[i] != null );
                    ASSERT ( MSCSX[j] != null );
                    ASSERT ( ! MSM.sameResources ( MSCSX[i], MSCSX[j] ) );
                }
            }
        }

        // 8. there must be a current default screen
        HScreen                     MSMS0               = MSM.getDefaultScreen();
        ASSERT ( MSMS0 != null );

        // 9. the current default screen must not be equivalent to the
        // empty screen
        ASSERT ( ! MSM.isEmptyScreen ( MSMS0 ) );

        // 10. exactly one screen entry in the current multiscreen
        // configuration must represent the same resources as the default screen
        found = 0;
        for ( int i = 0; i < MSCSX.length; i++ ) {
            if ( MSM.sameResources ( MSCSX[i], MSMS0 ) )
                found++;
        }
        ASSERT ( found == 1 );

        // 11. there must be a non-empty set of accessible screens
        HScreen[]                   MSMSX               = MSM.getScreens();
        ASSERT ( MSMSX != null );
        ASSERT ( MSMSX.length > 0 );

        // 12. the current default screen must be a distinct member of
        // the set of accessible screens

        found = 0;
        for ( int i = 0; i < MSMSX.length; i++ ) {
            ASSERT ( MSMSX[i] != null );
            if ( MSMSX[i] == MSMS0 ) {  // instance identity required
                found++;
            }
        }
        ASSERT ( found == 1 );

        // 13. any background screen device of the current default
        // screen must not be equivalent to the empty background screen device
        HBackgroundDevice[]         BX                  = MSMS0.getHBackgroundDevices();
        if ( BX != null ) {
            for ( int i = 0; i < BX.length; i++ ) {
                ASSERT ( BX[i] != null );
                ASSERT ( ! MSM.isEmptyScreenDevice ( BX[i] ) );
            }
        }

        // 14. any video screen device of the current default screen
        // must not be equivalent to the empty video screen device
        HVideoDevice[]              VX                  = MSMS0.getHVideoDevices();
        if ( VX != null ) {
            for ( int i = 0; i < VX.length; i++ ) {
                ASSERT ( VX[i] != null );
                ASSERT ( ! MSM.isEmptyScreenDevice ( VX[i] ) );
            }
        }

        // 15. any graphics screen device of the current default screen
        // must not be equivalent to the empty graphics screen device
        HGraphicsDevice[]           GX                  = MSMS0.getHGraphicsDevices();
        if ( GX != null ) {
            for ( int i = 0; i < GX.length; i++ ) {
                ASSERT ( GX[i] != null );
                ASSERT ( ! MSM.isEmptyScreenDevice ( GX[i] ) );
            }
        }

    }

    static private HScreen[] getNonDefaultScreens()
    {
        MultiScreenManager          MSM                 = MultiScreenManager.getInstance();
        HScreen[]                   SX1                 = MSM.getScreens();
        HScreen                     S1                  = MSM.getDefaultScreen();
        int                         NSX1ND              = 0;
        for ( int i = 0; i < SX1.length; i++ ) {
            if ( ! MSM.sameResources ( SX1[i], S1 ) )
                NSX1ND++;
        }
        HScreen[]                   SX1ND               = new HScreen[NSX1ND];
        for ( int i = 0, n = 0; i < SX1.length; i++ ) {
            if ( ! MSM.sameResources ( SX1[i], S1 ) )
                SX1ND[n++] = SX1[i];
        }
        return SX1ND;
    }

    static private HScreenDevice[] getNonDefaultScreenDevices()
    {
        MultiScreenManager          MSM                 = MultiScreenManager.getInstance();
        HScreen[]                   SX1                 = MSM.getScreens();
        int                         NDX1ND              = 0;
        for ( int i = 0; i < SX1.length; i++ ) {
            HScreenDevice[]         BX1                 = SX1[i].getHBackgroundDevices();
            HScreenDevice           B1                  = SX1[i].getDefaultHBackgroundDevice();
            for ( int j = 0; i < BX1.length; j++ ) {
                if ( ! MSM.sameResources ( BX1[i], B1 ) )
                    NDX1ND++;
            }
            HScreenDevice[]         VX1                 = SX1[i].getHVideoDevices();
            HScreenDevice           V1                  = SX1[i].getDefaultHVideoDevice();
            for ( int j = 0; i < VX1.length; j++ ) {
                if ( ! MSM.sameResources ( VX1[i], V1 ) )
                    NDX1ND++;
            }
            HScreenDevice[]         GX1                 = SX1[i].getHGraphicsDevices();
            HScreenDevice           G1                  = SX1[i].getDefaultHGraphicsDevice();
            for ( int j = 0; i < GX1.length; j++ ) {
                if ( ! MSM.sameResources ( GX1[i], G1 ) )
                    NDX1ND++;
            }
        }
        HScreenDevice[]             DX1ND               = new HScreenDevice[NDX1ND];
        for ( int i = 0, n = 0; i < SX1.length; i++ ) {
            HScreenDevice[]         BX1                 = SX1[i].getHBackgroundDevices();
            HScreenDevice           B1                  = SX1[i].getDefaultHBackgroundDevice();
            for ( int j = 0; i < BX1.length; j++ ) {
                if ( ! MSM.sameResources ( BX1[i], B1 ) )
                    DX1ND[n++] = BX1[i];
            }
            HScreenDevice[]         VX1                 = SX1[i].getHVideoDevices();
            HScreenDevice           V1                  = SX1[i].getDefaultHVideoDevice();
            for ( int j = 0; i < VX1.length; j++ ) {
                if ( ! MSM.sameResources ( VX1[i], V1 ) )
                    DX1ND[n++] = VX1[i];
            }
            HScreenDevice[]         GX1                 = SX1[i].getHGraphicsDevices();
            HScreenDevice           G1                  = SX1[i].getDefaultHGraphicsDevice();
            for ( int j = 0; i < GX1.length; j++ ) {
                if ( ! MSM.sameResources ( GX1[i], G1 ) )
                    DX1ND[n++] = GX1[i];
            }
        }
        return DX1ND;
    }
    
    static private void waitForMultiScreenConfigurationChangingEvent()
    {
        // wait for MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGING
    }
    
    static private void waitForMultiScreenConfigurationChangedEvent()
    {
        // wait for MultiScreenConfigurationEvent.MULTI_SCREEN_CONFIGURATION_CHANGED
    }

    static private void ASSERT ( boolean condition )
    {
        // check and report assertion failure
    }
    
}
